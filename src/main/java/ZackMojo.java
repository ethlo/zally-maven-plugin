import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
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

    @Parameter(property = "result-file")
    private String result;

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    @Override
    public void execute() throws MojoExecutionException
    {
        final Zack zack = new Zack();
        final List<RuleDetails> rules = zack.getRules();

        getLog().info("Processing file '" + source + "'");

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
        if (result != null && !result.trim().equals(""))
        {
            try
            {
                getLog().info("Writing result file to " + result);
                Files.writeString(Paths.get(result), mapper.writeValueAsString(results));
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
