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
import java.io.UncheckedIOException;
import java.nio.file.Files;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

@Mojo(name = "validate", defaultPhase = LifecyclePhase.COMPILE)
public class ZackMojo extends AbstractMojo
{
    @Parameter(required = true, defaultValue = "classpath:/api.yaml", property = "source")
    private String source;

    @Parameter(property = "ignore")
    private List<String> ignore;

    @Parameter(property = "failOn")
    private List<Severity> failOn;

    @Parameter(property = "resultFile")
    private String resultFile;

    @Parameter(property = "skip", defaultValue = "false")
    private boolean skip;

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    @Override
    public void execute() throws MojoFailureException
    {
        if (skip)
        {
            getLog().info("Skipping execution as requested");
            return;
        }

        final Zack zack = new Zack();
        final List<RuleDetails> rules = zack.getRules();

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
            getLog().warn("Will never fail build due to errors. Adjust 'failOn' " +
                    "property to fail on requested severities (" + Arrays.toString(Severity.values()) + ")");
        }

        printIgnoredRulesInfo(rules);

        final List<Result> results = validate(zack, source);

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

        for (Severity severity : failOn)
        {
            final int size = Optional.ofNullable(resultsByViolationType.get(severity)).map(Collection::size).orElse(0);
            if (size > 0)
            {
                throw new MojoFailureException("Failing build due to " + size + " errors with severity " + severity);
            }
        }
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
                Files.writeString(Paths.get(resultFile), mapper.writeValueAsString(results));
            }
            catch (IOException e)
            {
                throw new UncheckedIOException(e);
            }
        }
    }

    private List<Result> validate(Zack zack, String url)
    {
        try
        {
            return zack.validate(url);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e.getMessage(), e);
        }
    }
}
