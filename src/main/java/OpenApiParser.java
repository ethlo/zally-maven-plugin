import io.swagger.parser.util.ParseOptions;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.util.ResolverFully;

public class OpenApiParser
{
    public OpenAPI parse(String url)
    {
        final ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolve(true);
        final OpenAPI parseResult = new OpenAPIV3Parser().read(url);
        new ResolverFully(true).resolveFully(parseResult);
        return parseResult;
    }
}
