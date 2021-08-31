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
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.zalando.zally.core.Result;
import org.zalando.zally.core.RuleDetails;
import org.zalando.zally.rule.api.Severity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import edu.emory.mathcs.backport.java.util.Collections;

@Mojo(threadSafe = true, name = "validate", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class ZallyMojo extends AbstractMojo
{
    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    @Parameter(required = true, defaultValue = "${project.basedir}/src/main/resources/api.yaml", property = "zally.source")
    private String source;

    @Parameter(property = "zally.ignore")
    private List<String> ignore;

    @Parameter(property = "zally.failOn")
    private List<Severity> failOn;

    @Parameter(property = "zally.resultFile")
    private String resultFile;

    @Parameter(property = "zally.skip", defaultValue = "false")
    private boolean skip;

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    @Parameter
    private Map<String, String> ruleConfig;

    @Override
    public void execute() throws MojoFailureException
    {
        if (skip)
        {
            getLog().info("Skipping execution as requested");
            return;
        }

        final Config config = parseConfigMap(ruleConfig);

        final ZallyRunner zallyRunner = new ZallyRunner(config);
        final List<RuleDetails> rules = zallyRunner.getRules();

        final boolean existsOnClassPath = getClass().getClassLoader().getResourceAsStream(source) != null;
        final boolean existsOnFilesystem = Files.exists(Paths.get(source));
        if (!existsOnClassPath && !existsOnFilesystem)
        {
            throw new MojoFailureException("The specified source file could not be found: " + source);
        }

        getLog().info("Validating file '" + source + "'");

        if (!failOn.isEmpty())
        {
            getLog().info("Will fail build on errors of severity: " + failOn
                    .stream()
                    .map(Enum::name)
                    .collect(Collectors.joining(", ")));
        }
        else
        {
            getLog().warn("No errors will fail the build, reporting only. Adjust 'failOn' " +
                    "property to fail on requested severities:" + Arrays.toString(Severity.values()));
        }

        printIgnoredRulesInfo(rules);

        final List<Result> results = validate(zallyRunner, source);

        final Map<Severity, List<Result>> resultsByViolationType = results
                .stream()
                .filter(r -> !ignore.contains(r.getId()))
                .collect(Collectors.groupingBy(Result::getViolationType, Collectors.mapping(result -> result, Collectors.toList())));

        final Map<String, List<Result>> errorsOccurred = results
                .stream()
                .collect(Collectors.groupingBy(Result::getId, Collectors.mapping(result -> result, Collectors.toList())));

        printErrorDescriptionsWithLink(errorsOccurred);

        printErrors(resultsByViolationType);

        writeResults(results);

        // Check if we should halt the build due to validation errors
        for (Severity severity : failOn)
        {
            final int size = Optional.ofNullable(resultsByViolationType.get(severity)).map(Collection::size).orElse(0);
            if (size > 0)
            {
                throw new MojoFailureException("Failing build due to " + size + " errors with severity " + severity);
            }
        }
    }

    private Config parseConfigMap(Map<String, String> ruleConfig)
    {
        final Map<String, String> m = ruleConfig != null ? ruleConfig : Collections.emptyMap();
        final Map<String, Map> configurations = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : m.entrySet())
        {
            try
            {
                configurations.put(e.getKey(), mapper.readValue(e.getValue(), Map.class));
            }
            catch (JsonProcessingException jsonProcessingException)
            {
                throw new UncheckedIOException("Unable to parse configuration for rule name " + e.getKey(), jsonProcessingException);
            }
        }
        return ConfigFactory.parseMap(configurations);
    }

    private void printErrors(Map<Severity, List<Result>> resultsByViolationType)
    {
        for (Severity severity : Severity.values())
        {
            Optional.ofNullable(resultsByViolationType.get(severity))
                    .ifPresent(s ->
                    {
                        getLog().warn("");
                        final String errorHeader = "Severity " + severity.name() + " (" + s.size() + ")";
                        getLog().warn(errorHeader);
                        getLog().warn(StringUtils.repeat("-", errorHeader.length()));
                        for (final Result result : s)
                        {
                            getLog().warn(toViolationLine(result));
                        }
                    });
        }
    }

    private void printErrorDescriptionsWithLink(Map<String, List<Result>> errorsOccurred)
    {
        final List<String> errorDescriptionsWithLink = errorsOccurred
                .keySet()
                .stream()
                .map(id ->
                {
                    final Result first = errorsOccurred.get(id).get(0);
                    return first.getId() + " - " + first.getViolationType() + " - " + first.getTitle() + " - " + first.getUrl();
                }).sorted()
                .collect(Collectors.toList());

        getLog().info("");
        getLog().info("Error descriptions");
        getLog().info("------------------");
        errorDescriptionsWithLink.forEach(i -> getLog().info(i));
    }

    private void printIgnoredRulesInfo(List<RuleDetails> rules)
    {
        final Map<String, RuleDetails> ignored = new LinkedHashMap<>();
        ignore.forEach(i -> ignored.put(i, rules.stream().filter(r -> r.getRule().id().equals(i)).findFirst().orElse(null)));
        final List<String> ignoredDescription =
                ignored
                        .values()
                        .stream()
                        .filter(Objects::nonNull)
                        .sorted(Comparator.comparing(a -> a.getRule().id()))
                        .map(d -> d.getRule().id() + " - " + d.getRule().severity() + " - " + d.getRule().title())
                        .collect(Collectors.toList());
        getLog().info("");
        getLog().info("Ignored rules ");
        getLog().info("--------------");

        ignoredDescription.forEach(i -> getLog().info(i));
    }

    private String toViolationLine(Result result)
    {
        return result.getId() + " - " + result.getDescription() + " - " + result.getPointer();
    }

    private void writeResults(List<Result> results)
    {
        if (resultFile != null && !resultFile.trim().equals(""))
        {
            try
            {
                getLog().info("");
                getLog().info("Writing result file to " + resultFile);
                getLog().info("");
                final Path target = Paths.get(resultFile);
                Files.createDirectories(target.getParent());
                Files.writeString(target, mapper.writeValueAsString(results));
            }
            catch (IOException e)
            {
                throw new UncheckedIOException(e);
            }
        }
    }

    private List<Result> validate(ZallyRunner zallyRunner, String url)
    {
        try
        {
            return zallyRunner.validate(url);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e.getMessage(), e);
        }
    }
}
