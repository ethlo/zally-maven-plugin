import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.Test;
import org.zalando.zally.rule.api.Severity;

public class ZackTest
{
    @Test
    public void smokeTest() throws IllegalAccessException, IOException, MojoFailureException
    {
        final String url = "modified_petstore/petstore.yaml";
        final ZackMojo mojo = new ZackMojo();
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