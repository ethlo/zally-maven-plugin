import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.zalando.zally.core.Result;

public class ZackTest
{
    @Test
    public void loadWithClasspathRefs() throws IOException
    {
        final String url = "modified_petstore/petstore.yaml";
        final List<Result> result = new Zack().validate(url);
        assertThat(result).isNotEmpty().hasSize(28);
    }
}