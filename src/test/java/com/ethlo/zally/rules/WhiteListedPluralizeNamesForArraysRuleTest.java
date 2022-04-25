package com.ethlo.zally.rules;

/*-
 * #%L
 * zally-maven-plugin
 * %%
 * Copyright (C) 2021 - 2022 Morten Haraldsen (ethlo)
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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.Test;
import org.zalando.zally.core.DefaultContext;
import org.zalando.zally.rule.api.Context;
import org.zalando.zally.rule.api.Violation;

import com.ethlo.zally.OpenApiParser;
import com.typesafe.config.ConfigFactory;
import io.swagger.v3.oas.models.OpenAPI;

public class WhiteListedPluralizeNamesForArraysRuleTest
{
    @Test
    public void checkArrayPropertyNamesArePlural()
    {
        final String url = "modified_petstore/petstore.yaml";
        final OpenAPI openApi = new OpenApiParser().parse(url);
        final Context context = new DefaultContext("", openApi, null);
        final List<Violation> violations = new WhiteListedPluralizeNamesForArraysRule(ConfigFactory.empty()).checkArrayPropertyNamesArePlural(context);
        assertThat(violations).isEmpty();
    }
}
