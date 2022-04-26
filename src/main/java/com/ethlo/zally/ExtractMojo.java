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

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import io.swagger.v3.core.filter.AbstractSpecFilter;
import io.swagger.v3.core.filter.SpecFilter;
import io.swagger.v3.core.model.ApiDescription;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;

@Mojo(threadSafe = true, name = "extract", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class ExtractMojo extends AbstractMojo
{
    @Parameter(required = true, defaultValue = "${project.basedir}/src/main/resources/api.yaml", property = "zally.source")
    private String source;

    @Parameter(property = "outputFile")
    private File outputFile;

    @Parameter(property = "configFile")
    private File configFile;

    @Parameter(property = "skip", defaultValue = "false")
    private boolean skip;

    @Parameter(property = "filters")
    private List<OperationFilter> filters;

    @Parameter(property = "description")
    private String description;

    @Parameter(property = "name")
    private String name;

    @Parameter(property = "title")
    private String title;

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "${mojoExecution}", readonly = true, required = true)
    private MojoExecution mojoExecution;

    private static final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory()
            .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
            .enable(YAMLGenerator.Feature.SPLIT_LINES)
            .enable(YAMLGenerator.Feature.ALWAYS_QUOTE_NUMBERS_AS_STRINGS))
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void execute() throws MojoFailureException
    {
        final Optional<OpenAPI> loaded = load(getLog(), skip, source, false);

        if (name == null)
        {
            name = mojoExecution.getExecutionId();
        }

        if (outputFile == null)
        {
            outputFile = Paths.get(project.getBuild().getOutputDirectory()).resolve(name + ".yaml").toFile();
        }

        if (configFile != null)
        {
            try
            {
                final ExtractionDefinition extractionDefinition = yamlMapper.readValue(configFile, ExtractionDefinition.class);
                if (this.title == null)
                {
                    this.title = extractionDefinition.getTitle();
                }

                if (this.description == null)
                {
                    this.description = extractionDefinition.getDescription();
                }

                this.filters = extractionDefinition.getFilters();
            }
            catch (IOException exc)
            {
                throw new UncheckedIOException(exc);
            }
        }

        loaded.ifPresent(openAPI ->
        {
            getLog().info(String.format("Processing extraction %s", name));
            final AtomicInteger totalEvaluated = new AtomicInteger(0);
            final AtomicInteger totalMatched = new AtomicInteger(0);
            final OpenAPI filtered = new SpecFilter().filter(openAPI, new AbstractSpecFilter()
            {
                @Override
                public Optional<Operation> filterOperation(final Operation operation, final ApiDescription api, final Map<String, List<String>> params, final Map<String, String> cookies, final Map<String, List<String>> headers)
                {
                    final String path = api.getPath();
                    final PathItem pathItem = openAPI.getPaths().get(path);
                    final Map<String, Object> extensionMap = new LinkedHashMap<>(Optional.ofNullable(pathItem.getExtensions()).orElse(Collections.emptyMap()));
                    extensionMap.putAll(operation.getExtensions());
                    final OperationData operationData = new OperationData(operation, api, extensionMap);
                    if (filters.stream().anyMatch(filter ->
                    {
                        final boolean matched = match(filter, operationData);
                        if (matched)
                        {
                            totalMatched.incrementAndGet();
                            getLog().info(String.format("Including '%s' in '%s' because '%s'", operation.getOperationId(), name, filter.asString()));
                        }
                        totalEvaluated.incrementAndGet();
                        return matched;
                    }))
                    {
                        return Optional.of(operation);
                    }
                    return Optional.empty();
                }

                @Override
                public boolean isRemovingUnreferencedDefinitions()
                {
                    return true;
                }
            }, null, null, null);


            Optional.ofNullable(title).ifPresent(t -> filtered.getInfo().setTitle(t));
            Optional.ofNullable(description).ifPresent(t -> filtered.getInfo().setDescription(t));
            getLog().info(String.format("Included %s/%s API operations in extract", totalMatched.get(), totalEvaluated.get()));

            try
            {
                getLog().info("Writing extracted APIs to " + outputFile);
                Files.createDirectories(outputFile.toPath().getParent());
                Yaml.mapper().writeValue(outputFile, filtered);
            }
            catch (IOException e)
            {
                throw new UncheckedIOException(e);
            }
        });
    }

    public static boolean match(final OperationFilter filter, final OperationData data)
    {
        final String path = filter.getPointer().startsWith("/") ? filter.getPointer() : "/" + filter.getPointer();
        final String regexp = filter.getExpression();
        try
        {
            final JsonNode jsonNode = mapper.readTree(mapper.writeValueAsString(data));
            final JsonNode value = jsonNode.at(JsonPointer.compile(path));
            if (value.isObject())
            {
                final Iterator<String> fieldNames = value.fieldNames();
                while (fieldNames.hasNext())
                {
                    if (filter.isMatch(fieldNames.next()))
                    {
                        return true;
                    }
                }
            }
            else if (value.isArray())
            {
                final Iterator<JsonNode> elements = value.elements();
                while (elements.hasNext())
                {
                    if (filter.isMatch(elements.next().textValue()))
                    {
                        return true;
                    }
                }
            }
            else if (value.isTextual())
            {
                return filter.isMatch(value.textValue());
            }

            return false;
        }
        catch (IOException exc)
        {
            throw new UncheckedIOException(exc);
        }
    }

    public static Optional<OpenAPI> load(final Log log, final boolean skip, final String source, final boolean inlined) throws MojoFailureException
    {
        if (skip)
        {
            log.info("Skipping execution as requested");
            return Optional.empty();
        }

        final boolean existsOnClassPath = ExtractMojo.class.getClassLoader().getResourceAsStream(source) != null;
        final boolean existsOnFilesystem = Files.exists(Paths.get(source));
        if (!existsOnClassPath && !existsOnFilesystem)
        {
            throw new MojoFailureException("The specified source file could not be found: " + source);
        }

        log.info("Reading file '" + source + "'");
        return Optional.of(inlined ? new OpenApiParser().parseInlined(source) : new OpenApiParser().parse(source));
    }
}
