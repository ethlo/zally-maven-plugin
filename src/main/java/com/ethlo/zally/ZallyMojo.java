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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.zalando.zally.core.CheckDetails;
import org.zalando.zally.core.Result;
import org.zalando.zally.core.RuleDetails;
import org.zalando.zally.rule.api.Severity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

@Mojo(threadSafe = true, name = "validate", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class ZallyMojo extends AbstractMojo
{
    private final ObjectMapper mapper;

    @Parameter(required = true, defaultValue = "${project.basedir}/src/main/resources/api.yaml", property = "zally.source")
    private String source;

    @Parameter(property = "zally.failOn")
    private List<Severity> failOn;

    @Parameter(property = "zally.resultFile")
    private String resultFile;

    @Parameter(property = "zally.skip", defaultValue = "false")
    private boolean skip;

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    @Parameter(property = "zally.ruleConfigs")
    private Map<String, String> ruleConfigs;

    @Parameter(property = "zally.skipRules")
    private Set<String> skipRules;

    public ZallyMojo()
    {
        mapper = new ObjectMapper(new YAMLFactory());
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    }

    @Override
    public void execute() throws MojoFailureException
    {
        if (skip)
        {
            getLog().info("Skipping execution as requested");
            return;
        }

        final Config config = parseConfigMap(ruleConfigs).withFallback(ConfigFactory.load("reference"));

        final ZallyRunner zallyRunner = new ZallyRunner(config, getLog());

        final boolean existsOnClassPath = getClass().getClassLoader().getResourceAsStream(source) != null;
        final boolean existsOnFilesystem = Files.exists(Paths.get(source));
        if (!existsOnClassPath && !existsOnFilesystem)
        {
            throw new MojoFailureException("The specified source file could not be found: " + source);
        }

        printInfo("Validating file '" + source + "'");

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

        printErrorDescriptionsWithLink(zallyRunner.getRules());

        printSkippedRulesInfo(zallyRunner.getRules());
        final Map<CheckDetails, List<Result>> results = validate(zallyRunner, skipRules, source);

        // Map results to severity
        final Map<Severity, Map<CheckDetails, List<Result>>> resultsBySeverity = new LinkedHashMap<>();
        results.forEach((details, resultList) ->
        {
            for (final Result result : resultList)
            {
                resultsBySeverity.compute(result.getViolationType(), (severity, resultsByDetail) ->
                {
                    if (resultsByDetail == null)
                    {
                        resultsByDetail = new LinkedHashMap<>();
                    }
                    resultsByDetail.compute(details, (cd, rs) ->
                    {
                        if (rs == null)
                        {
                            rs = new LinkedList<>();
                        }
                        rs.add(result);
                        return rs;
                    });

                    return resultsByDetail;
                });
            }
        });

        printErrors(resultsBySeverity);

        writeResults(results);

        // Check if we should halt the build due to validation errors
        for (Severity severity : failOn)
        {
            final int size = Optional.ofNullable(resultsBySeverity.get(severity))
                    .map(Map::size)
                    .orElse(0);
            if (size > 0)
            {
                throw new MojoFailureException("Failing build due to errors with severity " + severity);
            }
        }
    }

    private void printInfo(String message)
    {
        getLog().info("");
        getLog().info(message);
    }

    private void printErrors(Map<Severity, Map<CheckDetails, List<Result>>> results)
    {
        final List<String> violations = new LinkedList<>();
        results.forEach((severity, res) ->
                res.forEach((checkDetails, resultList) ->
                        resultList.forEach(result ->
                                violations.add(checkDetails.getRule().id()
                                        + " - " + severity
                                        + " - " + checkDetails.getInstance().getClass().getSimpleName()
                                        + " - " + result.getDescription()
                                        + " - " + result.getPointer()
                                        + " - " + result.getLines()))));

        printHeader("Rule violations (" + violations.size() + ")");
        violations.forEach(v -> getLog().warn(v));
        getLog().warn("");
    }

    private void printHeader(String message)
    {
        getLog().info("");
        getLog().info(message);
        getLog().info(StringUtils.repeat("-", message.length()));
    }

    private Config parseConfigMap(Map<String, String> ruleConfig)
    {
        final Map<String, String> m = ruleConfig != null ? ruleConfig : new TreeMap<>();
        final Map<String, Map<?, ?>> configurations = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : m.entrySet())
        {
            final Map<?, ?> config = loadConfig(e.getKey(), e.getValue());
            Optional.ofNullable(config).ifPresent(c -> configurations.put(e.getKey(), c));
        }
        return ConfigFactory.parseMap(configurations);
    }

    private void printErrorDescriptionsWithLink(List<RuleDetails> rules)
    {
        final List<String> errorDescriptionsWithLink = rules
                .stream()
                .map(rule ->
                        rule.getRule().id() + " - "
                                + rule.getInstance().getClass().getSimpleName() + " - "
                                + rule.getRule().severity().name() + " - "
                                + rule.getRule().title() + " - "
                                + rule.getRuleSet().getUrl()).sorted()
                .collect(Collectors.toList());

        printHeader("Rules (" + rules.size() + ")");
        errorDescriptionsWithLink.forEach(i -> getLog().info(i));
    }

    private void printSkippedRulesInfo(List<RuleDetails> rules)
    {
        final Set<String> skipped = new LinkedHashSet<>();
        skipRules.forEach(ruleName ->
        {
            if (rules.stream().anyMatch(r ->
            {
                final String ruleClassName = r.getInstance().getClass().getSimpleName();
                final boolean ruleNameMatch = ruleClassName.equals(ruleName);
                final boolean isSkipped = skipRules.contains(ruleClassName);
                return ruleNameMatch && isSkipped;
            }))
            {
                skipped.add(ruleName);
            }
            else
            {
                getLog().warn("Requested to skip rule '" + ruleName + "', but no such rule is known.");
            }
        });

        final List<String> skippedDescription =
                rules
                        .stream()
                        .filter(r -> skipped.contains(r.getInstance().getClass().getSimpleName()))
                        .sorted(Comparator.comparing(a -> a.getRule().id()))
                        .map(d -> d.getRule().id() + " - " + d.getInstance().getClass().getSimpleName() + " - " + d.getRule().severity() + " - " + d.getRule().title())
                        .collect(Collectors.toList());

        if (!skippedDescription.isEmpty())
        {
            printHeader("Skipped rules (" + skippedDescription.size() + ")");
            skippedDescription.forEach(i -> getLog().info(i));
        }
    }

    private Map<String, Object> loadConfig(final String ruleName, final String ruleConfig)
    {
        try
        {
            return mapper.readValue(ruleConfig, Map.class);
        }
        catch (JsonProcessingException e)
        {
            throw new UncheckedIOException("Unable to parse configuration for rule name " + ruleName, e);
        }
    }

    private void writeResults(Map<CheckDetails, List<Result>> results)
    {
        if (resultFile != null && !resultFile.trim().equals(""))
        {
            try
            {
                printInfo("Writing result file to " + resultFile);
                getLog().info("");
                final Path target = Paths.get(resultFile);
                Files.createDirectories(target.getParent());
                Files.writeString(target, mapper.writeValueAsString(results.values().stream().filter(r -> !r.isEmpty()).collect(Collectors.toList())));
            }
            catch (IOException e)
            {
                throw new UncheckedIOException(e);
            }
        }
    }

    private Map<CheckDetails, List<Result>> validate(ZallyRunner zallyRunner, final Set<String> skipped, String url)
    {
        try
        {
            return zallyRunner.validate(url, skipped);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e.getMessage(), e);
        }
    }
}
