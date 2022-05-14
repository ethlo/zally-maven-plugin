# Zally-maven-plugin

[![Maven Central](https://img.shields.io/maven-central/v/com.ethlo.zally/zally-maven-plugin.svg)](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.ethlo.zally%22%20AND%20a%3A%22zally-maven-plugin%22)
[![Hex.pm](https://img.shields.io/hexpm/l/plug.svg)](LICENSE)
[![Build Status](https://app.travis-ci.com/ethlo/zally-maven-plugin.svg?branch=main)](https://app.travis-ci.com/ethlo/zally-maven-plugin)
[![Coverage Status](https://coveralls.io/repos/github/ethlo/zally-maven-plugin/badge.svg?branch=main)](https://coveralls.io/github/ethlo/zally-maven-plugin?branch=main)

Unofficial maven plugin using [Zally](https://github.com/zalando/zally) for OpenAPI 3.x specification validation.

## Benefits

* Simple to use and tweak rules to fit your requirements
* No Zally installation/server setup required
* Supports API definitions with external references out of the box

## Usage

```xml

<plugin>
    <groupId>com.ethlo.zally</groupId>
    <artifactId>zally-maven-plugin</artifactId>
    <version>VERSION</version>
    <configuration>
        <!--Configure severities that fail the build. Default is MUST, SHOULD -->
        <failOn>MUST</failOn>
        <!-- The input file to validate -->
        <source>src/main/resources/openapi/api.yaml</source>
        <!--Ignore certain rules. Default is none -->
        <skipRules>
            <skipRule>CommonFieldTypesRule</skipRule>
            <skipRule>SecureAllEndpointsWithScopesRule</skipRule>
            <skipRule>NoVersionInUriRule</skipRule>
        </skipRules>
        <ruleConfig>
            <!-- NOTE: The rule elements' content can be written in JSON or YAML -->
            <PluralizeResourceNamesRule>
                whitelist:
                - current
                - self
            </PluralizeResourceNamesRule>
        </ruleConfig>
        <!-- Alternatively rules can be configured with configuration file -->
        <rulesConfigLocation>reference.conf</rulesConfigLocation>
        <!-- Write the result of the validation to file. Optional-->
        <resultFile>target/api_validation_result.yaml</resultFile>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>report</goal>
                <goal>validate</goal>
            </goals>
        </execution>
    </executions>
    <dependencies>
        <dependency>
            <!-- The pluggable rule-set you want to run -->
            <groupId>org.zalando</groupId>
            <artifactId>zally-ruleset-zalando</artifactId>
            <version>2.1.0</version>
        </dependency>
    </dependencies>
</plugin>
```

## Example output

```shell
[info] Analyzing file 'modified_petstore/petstore.yaml'
[info] API hierarchy:
[info] 
[info]     ├── orders
[info]     │   └── {orderId}
[info]     │       └── PUT - updateOrder
[info]     └── pets
[info]         ├── GET - listPets
[info]         ├── POST - createPets
[info]         └── {petId}
[info]             └── GET - showPetById
[info] 
[info] 
[info] Validating file 'modified_petstore/petstore.yaml'
[info] Will fail build on errors of severity: MUST, SHOULD
[info] 
[info] Rules (36)
[info] ----------
[info] 101 - UseOpenApiRule - MUST - Provide API Specification using OpenAPI - https://zalando.github.io/restful-api-guidelines/
[info] 104 - SecureAllEndpointsRule - MUST - Secure Endpoints - https://zalando.github.io/restful-api-guidelines/
[info] 105 - SecureAllEndpointsWithScopesRule - MUST - Secure All Endpoints With Scopes - https://zalando.github.io/restful-api-guidelines/
[info] 107 - ExtensibleEnumRule - SHOULD - Prefer Compatible Extensions - https://zalando.github.io/restful-api-guidelines/
[info] 110 - SuccessResponseAsJsonObjectRule - MUST - Response As JSON Object - https://zalando.github.io/restful-api-guidelines/
[info] 115 - NoVersionInUriRule - MUST - Do Not Use URI Versioning - https://zalando.github.io/restful-api-guidelines/
[info] 116 - VersionInInfoSectionRule - MUST - Use Semantic Versioning - https://zalando.github.io/restful-api-guidelines/
[info] 118 - SnakeCaseInPropNameRule - MUST - Property Names Must be ASCII snake_case - https://zalando.github.io/restful-api-guidelines/
[info] 120 - PluralizeNamesForArraysRule - SHOULD - Array names should be pluralized - https://zalando.github.io/restful-api-guidelines/
[info] 120 - WhiteListedPluralizeNamesForArraysRule - SHOULD - Array names should be pluralized - https://zalando.github.io/restful-api-guidelines/
[info] 125 - EnumValueTypeRule - SHOULD - Represent enumerations as strings - https://zalando.github.io/restful-api-guidelines/
[info] 129 - KebabCaseInPathSegmentsRule - MUST - Lowercase words with hyphens - https://zalando.github.io/restful-api-guidelines/
[info] 130 - SnakeCaseForQueryParamsRule - MUST - Use snake_case (never camelCase) for Query Parameters - https://zalando.github.io/restful-api-guidelines/
[info] 132 - PascalCaseHttpHeadersRule - SHOULD - Use uppercase separate words with hyphens for HTTP headers - https://zalando.github.io/restful-api-guidelines/
[info] 134 - PluralizeResourceNamesRule - MUST - Pluralize Resource Names - https://zalando.github.io/restful-api-guidelines/
[info] 136 - AvoidTrailingSlashesRule - MUST - Avoid Trailing Slashes - https://zalando.github.io/restful-api-guidelines/
[info] 143 - IdentifyResourcesViaPathSegments - MUST - Resources must be identified via path segments - https://zalando.github.io/restful-api-guidelines/
[info] 145 - NestedPathsMayBeRootPathsRule - MAY - Consider Using (Non-) Nested URLs - https://zalando.github.io/restful-api-guidelines/
[info] 146 - LimitNumberOfResourcesRule - SHOULD - Limit number of resource types - https://zalando.github.io/restful-api-guidelines/
[info] 147 - LimitNumberOfSubResourcesRule - SHOULD - Limit number of Sub-resources level - https://zalando.github.io/restful-api-guidelines/
[info] 150 - UseStandardHttpStatusCodesRule - MUST - Use Standard HTTP Status Codes - https://zalando.github.io/restful-api-guidelines/
[info] 151 - JsonProblemAsDefaultResponseRule - MUST - Specify Success and Error Responses - https://zalando.github.io/restful-api-guidelines/
[info] 153 - Use429HeaderForRateLimitRule - MUST - Use 429 With Header For Rate Limits - https://zalando.github.io/restful-api-guidelines/
[info] 154 - QueryParameterCollectionFormatRule - SHOULD - Use and Specify Explicitly the Form-Style Query Format for Collection Parameters - https://zalando.github.io/restful-api-guidelines/
[info] 166 - AvoidLinkHeadersRule - MUST - Avoid Link in Header Rule - https://zalando.github.io/restful-api-guidelines/
[info] 171 - FormatForNumbersRule - MUST - Define Format for Type Number and Integer - https://zalando.github.io/restful-api-guidelines/
[info] 172 - MediaTypesRule - SHOULD - Prefer standard media type names - https://zalando.github.io/restful-api-guidelines/
[info] 174 - CommonFieldTypesRule - MUST - Use common field names - https://zalando.github.io/restful-api-guidelines/
[info] 176 - UseProblemJsonRule - MUST - Use Problem JSON - https://zalando.github.io/restful-api-guidelines/
[info] 183 - ProprietaryHeadersRule - MUST - Use Only the Specified Proprietary Zalando Headers - https://zalando.github.io/restful-api-guidelines/
[info] 215 - ApiIdentifierRule - MUST - Provide API Identifier - https://zalando.github.io/restful-api-guidelines/
[info] 218 - ApiMetaInformationRule - MUST - Contain API Meta Information - https://zalando.github.io/restful-api-guidelines/
[info] 219 - ApiAudienceRule - MUST - Provide API Audience - https://zalando.github.io/restful-api-guidelines/
[info] 224 - FunctionalNamingForHostnamesRule - MUST - Follow Naming Convention for Hostnames - https://zalando.github.io/restful-api-guidelines/
[info] 235 - DateTimePropertiesSuffixRule - SHOULD - Name date/time properties using the "_at" suffix - https://zalando.github.io/restful-api-guidelines/
[info] 240 - UpperCaseEnums - SHOULD - Declare enum values using UPPER_SNAKE_CASE format - https://zalando.github.io/restful-api-guidelines/
[info] 
[info] Skipped rules (1)
[info] -----------------
[info] 174 - CommonFieldTypesRule - MUST - Use common field names
[info] 
[info] Rule violations (26)
[info] --------------------
[warn] 219 - MUST - ApiAudienceRule - API Audience must be provided - /info/x-audience
[warn] 215 - MUST - ApiIdentifierRule - API Identifier should be provided - /info/x-api-id
[warn] 218 - MUST - ApiMetaInformationRule - Contact URL has to be provided - /info/contact/url
[warn] 218 - MUST - ApiMetaInformationRule - Contact e-mail has to be provided - /info/contact/email
[warn] 218 - MUST - ApiMetaInformationRule - Description has to be provided - /info/description
[warn] 218 - MUST - ApiMetaInformationRule - Contact name has to be provided - /info/contact/name
[warn] 224 - MUST - FunctionalNamingForHostnamesRule - API audience null is not supported. - /info
[warn] 115 - MUST - NoVersionInUriRule - URL contains version number - /servers/0
[warn] 183 - MUST - ProprietaryHeadersRule - use only standardized or specified response headers - /paths/~1pets/get/responses/200/headers/x-next
[warn] 104 - MUST - SecureAllEndpointsRule - API must be secured by OAuth2 or Bearer Authentication - /components/securitySchemes
[warn] 105 - MUST - SecureAllEndpointsWithScopesRule - Endpoint is not secured by scope(s) - /paths/~1orders~1{orderId}/put
[warn] 105 - MUST - SecureAllEndpointsWithScopesRule - Endpoint is not secured by scope(s) - /paths/~1pets/get
[warn] 105 - MUST - SecureAllEndpointsWithScopesRule - Endpoint is not secured by scope(s) - /paths/~1pets/post
[warn] 105 - MUST - SecureAllEndpointsWithScopesRule - Endpoint is not secured by scope(s) - /paths/~1pets~1{petId}/get
[warn] 118 - MUST - SnakeCaseInPropNameRule - Property name has to be snake_case - /components/schemas/Error/properties/messageA
[warn] 110 - MUST - SuccessResponseAsJsonObjectRule - Always return JSON objects as top-level data structures to support extensibility - /components/schemas/Pets
[warn] 176 - MUST - UseProblemJsonRule - Operations should return problem JSON when any problem occurs during processing whether caused by client or server. Media type have to be 'application/problem+json' - /paths/~1pets/get/responses/default/content/application~1json
[warn] 176 - MUST - UseProblemJsonRule - Operations should return problem JSON when any problem occurs during processing whether caused by client or server. Media type have to be 'application/problem+json' - /paths/~1pets/post/responses/default/content/application~1json
[warn] 176 - MUST - UseProblemJsonRule - Operations should return problem JSON when any problem occurs during processing whether caused by client or server. Media type have to be 'application/problem+json' - /paths/~1pets~1{petId}/get/responses/default/content/application~1json
[warn] 224 - SHOULD - FunctionalNamingForHostnamesRule - API audience null is not supported. - /info
[warn] 151 - SHOULD - JsonProblemAsDefaultResponseRule - operation should contain the default response - /paths/~1orders~1{orderId}/put
[warn] 151 - SHOULD - JsonProblemAsDefaultResponseRule - problem+json should be used as default response - /paths/~1pets/get/responses/default/content/application~1json
[warn] 151 - SHOULD - JsonProblemAsDefaultResponseRule - problem+json should be used as default response - /paths/~1pets/post/responses/default/content/application~1json
[warn] 151 - SHOULD - JsonProblemAsDefaultResponseRule - problem+json should be used as default response - /paths/~1pets~1{petId}/get/responses/default/content/application~1json
[warn] 132 - SHOULD - PascalCaseHttpHeadersRule - Header has to be Hyphenated-Pascal-Case - /paths/~1pets/get/responses/200/headers/x-next
[warn] 224 - MAY - FunctionalNamingForHostnamesRule - API audience null is not supported. - /info
[info] 
[info] Writing result file to /tmp/zally-maven-plugin9175458596507981917.yaml
[info] 
```
