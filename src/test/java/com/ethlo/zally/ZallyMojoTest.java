package com.ethlo.zally;
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

import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.Ignore;
import org.junit.Test;
import org.zalando.zally.rule.api.Severity;

import com.ethlo.zally.rules.WhiteListedPluralizeNamesForArraysRule;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ZallyMojoTest
{
    private final String url = "/modified_petstore/petstore.yaml";

    @Test
    public void smokeTest() throws IllegalAccessException, IOException
    {
        final String url = "/modified_petstore/petstore.yaml";
        final ZallyMojo mojo = new ZallyMojo();
        FieldUtils.writeField(mojo, "failOn", Arrays.asList(Severity.MUST, Severity.SHOULD), true);
        FieldUtils.writeField(mojo, "source", url, true);
        FieldUtils.writeField(mojo, "skipRules", new TreeSet<>(Arrays.asList("104", "174")), true);
        FieldUtils.writeField(mojo, "resultFile", Files.createTempFile("zally-maven-plugin", ".yaml").toString(), true);
        try
        {
            mojo.execute();
        }
        catch (MojoFailureException expected)
        {

        }
    }

    @Test
    public void testConfigureRule() throws IllegalAccessException, IOException
    {
        final ZallyMojo mojo = new ZallyMojo();
        FieldUtils.writeField(mojo, "failOn", Arrays.asList(Severity.MUST, Severity.SHOULD), true);
        FieldUtils.writeField(mojo, "source", url, true);
        FieldUtils.writeField(mojo, "skipRules", new TreeSet<>(Arrays.asList("PluralizeNamesForArraysRule", "CommonFieldTypesRule", "SecureAllEndpointsWithScopesRule")), true);
        FieldUtils.writeField(mojo, "resultFile", Files.createTempFile("zally-maven-plugin", ".yaml").toString(), true);

        final TreeMap<String, Object> pluralRuleConfig = new TreeMap<>();
        pluralRuleConfig.put("whitelist", Arrays.asList("content", "delta"));

        final Map<String, Object> ruleConfigs = new LinkedHashMap<>();
        final ObjectMapper mapper = new ObjectMapper();
        ruleConfigs.put(WhiteListedPluralizeNamesForArraysRule.class.getSimpleName(), mapper.writeValueAsString(pluralRuleConfig));
        FieldUtils.writeField(mojo, "ruleConfigs", ruleConfigs, true);
        FieldUtils.writeField(mojo, "skipRules", new TreeSet<>(Collections.singletonList("CommonFieldTypesRule")), true);

        try
        {
            mojo.execute();
        }
        catch (MojoFailureException expected)
        {

        }
    }

    @Ignore
    @Test
    public void testRefs() throws IllegalAccessException, MojoFailureException
    {
        final ZallyMojo mojo = new ZallyMojo();
        FieldUtils.writeField(mojo, "failOn", Arrays.asList(Severity.MUST, Severity.SHOULD), true);
        FieldUtils.writeField(mojo, "source", url, true);
        FieldUtils.writeField(mojo, "ignore", Arrays.asList("104", "174"), true);
        mojo.execute();
    }
}
