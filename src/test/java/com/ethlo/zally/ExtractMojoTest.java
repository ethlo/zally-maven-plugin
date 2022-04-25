package com.ethlo.zally;

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

import java.util.Collections;

import org.junit.Test;

import io.swagger.v3.core.model.ApiDescription;
import io.swagger.v3.oas.models.Operation;

public class ExtractMojoTest
{
    private final OperationData operationData = new OperationData(new Operation().operationId("my-operation"), new ApiDescription("/foo/bar/{baz}", "get"), Collections.singletonMap("x-foo", true));

    @Test
    public void matchOperation()
    {
        assertThat(ExtractMojo.match("operationId=my-operation", operationData)).isTrue();
    }

    @Test
    public void testOperationRegex()
    {
        assertThat(ExtractMojo.match("operationId=my-ope.*", operationData)).isTrue();
    }

    @Test
    public void testOperationRegexNegative()
    {
        assertThat(ExtractMojo.match("operationId=!^my-ope.*", operationData)).isFalse();
    }

    @Test
    public void testPathRegex()
    {
        assertThat(ExtractMojo.match("path=/foo/.*", operationData)).isTrue();
        assertThat(ExtractMojo.match("path=/foo/baz/.*", operationData)).isFalse();
    }

    @Test
    public void testMatchExtensionKey()
    {
        assertThat(ExtractMojo.match("extensions=x-foo", operationData)).isTrue();
    }
}
