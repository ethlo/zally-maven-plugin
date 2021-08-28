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

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.Test;
import org.zalando.zally.rule.api.Severity;

public class ZallyMojoTest
{
    @Test
    public void smokeTest() throws IllegalAccessException, IOException
    {
        final String url = "modified_petstore/petstore.yaml";
        final ZallyMojo mojo = new ZallyMojo();
        FieldUtils.writeField(mojo, "failOn", Arrays.asList(Severity.MUST, Severity.SHOULD), true);
        FieldUtils.writeField(mojo, "source", url, true);
        FieldUtils.writeField(mojo, "ignore", Arrays.asList("104", "174"), true);
        FieldUtils.writeField(mojo, "resultFile", Files.createTempFile("zally-maven-plugin", ".yaml").toString(), true);
        try
        {
            mojo.execute();
        }
        catch (MojoFailureException expected)
        {

        }
    }
}
