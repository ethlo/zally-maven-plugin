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
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.zalando.zally.core.CheckDetails;
import org.zalando.zally.core.DefaultContext;
import org.zalando.zally.core.Result;
import org.zalando.zally.core.RuleDetails;
import org.zalando.zally.core.RulesManager;
import org.zalando.zally.rule.api.Check;
import org.zalando.zally.rule.api.Context;
import org.zalando.zally.rule.api.Violation;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.swagger.v3.oas.models.OpenAPI;

public class ZallyRunner
{
    private final RulesManager rulesManager;
    private final Config ruleConfigs;

    public ZallyRunner(final Config ruleConfigs)
    {
        this.ruleConfigs = ruleConfigs;
        this.rulesManager = RulesManager.Companion.fromClassLoader(ruleConfigs.withFallback(ConfigFactory.load("rules-config.conf")));
    }

    public List<Result> validate(String url) throws IOException
    {
        final OpenAPI openApi = new OpenApiParser().parse(url);
        final Context context = new DefaultContext("", openApi, null);

        final List<Result> resultList = new LinkedList<>();
        for (RuleDetails ruleDetails : rulesManager.getRules())
        {
            final Object base = ruleDetails.getInstance();
            final Class<?> ruleClass = base.getClass();
            final String simpleName = ruleClass.getSimpleName();

            final Object instance = ruleConfigs.hasPath(simpleName) ? createInstance(base.getClass(), ruleConfigs) : base;

            for (Method method : ruleClass.getDeclaredMethods())
            {
                final Check check = method.getAnnotation(Check.class);
                if (check != null && method.getParameterTypes().length == 1 && method.getParameterTypes()[0] == Context.class)
                {
                    final CheckDetails checkDetails = ruleDetails.toCheckDetails(check, method);
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
                                handleViolation(resultList, checkDetails, violation);
                            }
                        }
                        else if (result instanceof Violation)
                        {
                            handleViolation(resultList, checkDetails, (Violation) result);
                        }
                    }
                }
            }
        }

        resultList.sort(Comparator.comparing(Result::getViolationType));
        return resultList;
    }

    private Object createInstance(Class<?> ruleClass, Config ruleConfig)
    {
        try
        {
            final Constructor<?> constructor = ruleClass.getConstructor(Config.class);
            return constructor.newInstance(ruleConfig);
        }
        catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e)
        {
            throw new RuntimeException("Cannot instantiate rule " + ruleClass, e);
        }
    }

    private void handleViolation(final List<Result> resultList, final CheckDetails details, Violation violation)
    {
        final Result result = new Result(
                details.getRule().id(),
                details.getRuleSet().url(details.getRule()),
                details.getRule().title(),
                violation.getDescription(),
                details.getCheck().severity(),
                violation.getPointer(),
                null/*locator.locate(violation.getPointer())*/
        );
        resultList.add(result);
    }

    public List<RuleDetails> getRules()
    {
        return rulesManager.getRules();
    }
}
