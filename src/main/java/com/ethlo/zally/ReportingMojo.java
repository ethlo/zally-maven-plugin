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

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

@Mojo(threadSafe = true, name = "report", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class ReportingMojo extends AbstractMojo
{
    @Parameter(required = true, defaultValue = "${project.basedir}/src/main/resources/api.yaml", property = "zally.source")
    private String source;

    @Parameter(property = "zally.skip", defaultValue = "false")
    private boolean skip;

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    @Override
    public void execute() throws MojoFailureException
    {
        if (skip)
        {
            getLog().info("Skipping execution as requested");
            return;
        }

        final boolean existsOnClassPath = getClass().getClassLoader().getResourceAsStream(source) != null;
        final boolean existsOnFilesystem = Files.exists(Paths.get(source));
        if (!existsOnClassPath && !existsOnFilesystem)
        {
            throw new MojoFailureException("The specified source file could not be found: " + source);
        }

        getLog().info("Analyzing file '" + source + "'");

        getLog().info("API hierarchy:");
        final String hierarchy = new ApiReporter(new OpenApiParser().parse(source)).render();
        Arrays.stream(hierarchy.split("\n")).forEach(line -> getLog().info(line));
        getLog().info("");
    }
}
