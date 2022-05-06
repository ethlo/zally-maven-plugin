package com.ethlo.zally;/*-
 * #%L
 * zally-maven-plugin
 * %%
 * Copyright (C) 2021 Morten Haraldsen (ethlo)
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.plugin.logging.Log;
import org.jetbrains.annotations.NotNull;
import org.zalando.zally.core.CheckDetails;
import org.zalando.zally.core.DefaultContext;
import org.zalando.zally.core.JsonPointerLocator;
import org.zalando.zally.core.Result;
import org.zalando.zally.core.RuleDetails;
import org.zalando.zally.rule.api.Check;
import org.zalando.zally.rule.api.Context;
import org.zalando.zally.rule.api.Rule;
import org.zalando.zally.rule.api.RuleSet;
import org.zalando.zally.rule.api.Violation;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;
import io.swagger.v3.oas.models.OpenAPI;

public class ZallyRunner
{
    private final List<RuleDetails> rules;

    private final Log logger;

    public ZallyRunner(final Config ruleConfigs, final Log logger)
    {
        this.rules = new LinkedList<>();
        this.logger = logger;
        final List<Class<?>> ruleClasses = loadRuleClasses();
        for (Class<?> ruleClass : ruleClasses)
        {
            final String simpleName = ruleClass.getSimpleName();
            logger.debug("Loading rule " + simpleName);
            final Object instance = createRuleInstance(ruleClass, ruleConfigs);
            final Rule ruleAnnotation = ruleClass.getAnnotation(Rule.class);
            this.rules.add(new RuleDetails((RuleSet) createInstance(ruleAnnotation.ruleSet()), ruleAnnotation, instance));
        }
    }

    public Map<CheckDetails, List<Result>> validate(String url, final Set<String> skipped) throws IOException
    {
        final OpenAPI openApi = new OpenApiParser().parseInlined(url);
        final Context context = new DefaultContext("", openApi, null);

        final Map<CheckDetails, List<Result>> returnValue = new LinkedHashMap<>();
        for (RuleDetails ruleDetails : rules)
        {
            if (!skipped.contains(ruleDetails.getInstance().getClass().getSimpleName()))
            {
                final Object instance = ruleDetails.getInstance();
                for (Method method : instance.getClass().getDeclaredMethods())
                {
                    final Check checkAnnotation = method.getAnnotation(Check.class);

                    if (checkAnnotation != null && method.getParameterTypes().length == 1 && method.getParameterTypes()[0] == Context.class)
                    {
                        final List<Result> violationList = new LinkedList<>();
                        final CheckDetails checkDetails = performCheck(context, violationList, instance, ruleDetails.getRule(), ruleDetails.getRuleSet(), method, checkAnnotation, url);
                        returnValue.put(checkDetails, violationList);
                    }
                }
            }
        }

        return returnValue;
    }

    @NotNull
    private CheckDetails performCheck(Context context, List<Result> violationList, Object instance, Rule ruleAnnotation, RuleSet ruleSet, Method method, Check checkAnnotation, String url)
    {
        final CheckDetails checkDetails = new CheckDetails(ruleSet, ruleAnnotation, instance, checkAnnotation, method);

        final Object result;
        try
        {
            result = method.invoke(instance, context);
        }
        catch (IllegalAccessException | InvocationTargetException e)
        {
            throw new RuntimeException(e);
        }
        if (result != null)
        {
            if (result instanceof Iterable)
            {
                //noinspection unchecked
                for (Violation violation : (Iterable<? extends Violation>) result)
                {
                    // Ignore violations if there are x-zally-ignore markers.
                    if (context.isIgnored(violation.getPointer(), checkDetails.getRule().id())
                            || context.isIgnored(violation.getPointer(), "*"))
                    {
                        logger.info(String.format("Ignore violation, rule = %s, at %s", checkDetails.getRule().id(), violation.getPointer()));
                        continue;
                    }
                    violationList.add(handleViolation(url, checkDetails, violation));
                }
            }
            else if (result instanceof Violation)
            {
                violationList.add(handleViolation(url, checkDetails, (Violation) result));
            }
        }
        return checkDetails;
    }

    private List<Class<?>> loadRuleClasses()
    {
        try (ScanResult result = new ClassGraph().enableClassInfo().enableAnnotationInfo().scan())
        {
            final ClassInfoList classInfos = result.getClassesWithAnnotation(Rule.class.getName());
            return classInfos.stream().map(ClassInfo::loadClass).collect(Collectors.toList());
        }
    }

    private Object createRuleInstance(Class<?> ruleClass, Config ruleConfig)
    {
        try
        {
            for (Constructor<?> constructor : ruleClass.getConstructors())
            {
                final Class<?>[] paramTypes = constructor.getParameterTypes();
                if (paramTypes.length == 1 && paramTypes[0].equals(Config.class))
                {
                    return constructor.newInstance(ruleConfig.withFallback(ConfigFactory.parseMap(Collections.emptyMap())));
                }
            }
            return ruleClass.getConstructor().newInstance();
        }
        catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e)
        {
            throw new RuntimeException("Cannot instantiate rule " + ruleClass, e);
        }
    }

    private Object createInstance(Class<?> type)
    {
        try
        {
            final Constructor<?> constructor = type.getConstructor();
            return constructor.newInstance();
        }
        catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e)
        {
            throw new RuntimeException("Cannot instantiate class " + type, e);
        }
    }

    private Result handleViolation(String url, final CheckDetails details, Violation violation)
    {
        JsonPointerLocator locator = new JsonPointerLocator("");
        try {
            // throw new IOException();
            locator = new JsonPointerLocator(Files.readString(Paths.get(url)));
        } catch (IOException e) {
            logger.warn("Could not read File");;
            e.printStackTrace();
        }

        return new Result(
                details.getRule().id(),
                details.getRuleSet().url(details.getRule()),
                details.getRule().title(),
                violation.getDescription(),
                details.getCheck().severity(),
                violation.getPointer(),
                locator.locate(violation.getPointer())
        );
    }

    public List<RuleDetails> getRules()
    {
        return rules;
    }
}
