package com.ethlo.zally.rules;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.Test;
import org.zalando.zally.core.DefaultContext;
import org.zalando.zally.rule.api.Context;
import org.zalando.zally.rule.api.Violation;

import com.ethlo.zally.OpenApiParser;
import com.typesafe.config.ConfigFactory;
import io.swagger.v3.oas.models.OpenAPI;

public class WhiteListedPluralizeNamesForArraysRuleTest
{
    @Test
    public void checkArrayPropertyNamesArePlural()
    {
        final String url = "modified_petstore/petstore.yaml";
        final OpenAPI openApi = new OpenApiParser().parse(url);
        final Context context = new DefaultContext("", openApi, null);
        final List<Violation> violations = new WhiteListedPluralizeNamesForArraysRule(ConfigFactory.empty()).checkArrayPropertyNamesArePlural(context);
        assertThat(violations).isEmpty();
    }
}