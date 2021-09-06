package com.ethlo.zally.rules;

/*-
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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.zalando.zally.rule.api.Check;
import org.zalando.zally.rule.api.Context;
import org.zalando.zally.rule.api.Rule;
import org.zalando.zally.rule.api.Severity;
import org.zalando.zally.rule.api.Violation;

import com.ethlo.zally.rules.common.PlingStemmer;
import com.typesafe.config.Config;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;

@Rule(
        ruleSet = ConfigurableZalandoRuleSet.class,
        id = "120",
        severity = Severity.SHOULD,
        title = "Array names should be pluralized"
)
public class WhiteListedPluralizeNamesForArraysRule
{
    private final List<String> whiteList;

    public WhiteListedPluralizeNamesForArraysRule(Config config)
    {
        this.whiteList = config.hasPath(getClass().getSimpleName()) ? config.getConfig(getClass().getSimpleName()).getStringList("whitelist") : Collections.emptyList();
    }

    public static Map<String, Schema> getAllSchemas(OpenAPI openAPI)
    {
        if (openAPI != null && openAPI.getComponents() != null && openAPI.getComponents().getSchemas() != null)
        {
            return openAPI.getComponents().getSchemas();
        }
        return Collections.emptyMap();
    }

    @Check(severity = Severity.SHOULD)
    public List<Violation> checkArrayPropertyNamesArePlural(final Context context)
    {
        return getAllSchemas(context.getApi()).entrySet().stream()
                .filter(it -> "array".equals(it.getValue().getType()))
                //.peek(it -> System.out.println(it.getKey() + " - " + it.getValue()))
                .filter(it -> whiteList.contains(it.getKey()))
                .filter(it -> !PlingStemmer.isPlural(it.getKey()))
                .map(it -> context.violation("Array property name appears to be singular: " + it.getKey(), it.getValue()))
                .collect(Collectors.toList());
    }
}
