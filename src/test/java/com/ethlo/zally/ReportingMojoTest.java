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

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.Test;

public class ReportingMojoTest
{
    private final String url = "modified_petstore/petstore.yaml";

    @Test
    public void smokeTest() throws IllegalAccessException, MojoFailureException
    {
        final ReportingMojo mojo = new ReportingMojo();
        FieldUtils.writeField(mojo, "source", url, true);
        mojo.execute();
    }
}
