import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.zalando.zally.core.CheckDetails;
import org.zalando.zally.core.DefaultContext;
import org.zalando.zally.core.JsonPointerLocator;
import org.zalando.zally.core.Result;
import org.zalando.zally.core.RuleDetails;
import org.zalando.zally.core.RulesManager;
import org.zalando.zally.rule.api.Check;
import org.zalando.zally.rule.api.Context;
import org.zalando.zally.rule.api.Violation;

import com.typesafe.config.ConfigFactory;
import io.swagger.v3.oas.models.OpenAPI;

public class Zack
{
    private final RulesManager rulesManager;

    public Zack()
    {
        this.rulesManager = RulesManager.Companion.fromClassLoader(ConfigFactory.load("rules-config.conf"));
    }

    public List<Result> validate(String url) throws IOException
    {
        final OpenAPI openApi = new OpenApiParser().parse(url);
        final Context context = new DefaultContext("", openApi, null);

        final List<Result> resultList = new LinkedList<>();
        for (RuleDetails ruleDetails : rulesManager.getRules())
        {
            final Object instance = ruleDetails.getInstance();
            for (Method method : instance.getClass().getDeclaredMethods())
            {
                final Check check = method.getAnnotation(Check.class);
                if (check != null && method.getParameterTypes().length == 1 && method.getParameterTypes()[0] == Context.class)
                {
                    final CheckDetails checkDetails = ruleDetails.toCheckDetails(check, method);
                    final Object result;
                    try
                    {
                        result = method.invoke(instance, context);
                    }
                    catch (IllegalAccessException | InvocationTargetException e)
                    {
                        throw new RuntimeException(e);
                    }
                    if (result != null)
                    {
                        if (result instanceof Iterable)
                        {
                            //noinspection unchecked
                            for (Violation violation : (Iterable<? extends Violation>) result)
                            {
                                handleViolation(resultList, checkDetails, violation);
                            }
                        }
                        else if (result instanceof Violation)
                        {
                            handleViolation(resultList, checkDetails, (Violation) result);
                        }
                    }
                }
            }
        }

        resultList.sort(Comparator.comparing(Result::getViolationType));
        return resultList;
    }

    private void handleViolation(final List<Result> resultList, final CheckDetails details, Violation violation)
    {
        final Result result = new Result(
                details.getRule().id(),
                details.getRuleSet().url(details.getRule()),
                details.getRule().title(),
                violation.getDescription(),
                details.getCheck().severity(),
                violation.getPointer(),
                null/*locator.locate(violation.getPointer())*/
        );
        resultList.add(result);
    }

    public List<RuleDetails> getRules()
    {
        return rulesManager.getRules();
    }
}
