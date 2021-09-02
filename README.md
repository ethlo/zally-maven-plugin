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
        <ignore>
            146,174,134
        </ignore>
        <ruleConfig>
            <!-- NOTE: The rule elements' content can be written in JSON or YAML -->
            <PluralizeResourceNamesRule>
                whitelist:
                - current
                - self
            </PluralizeResourceNamesRule>
        </ruleConfig>
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
[info] Validating file 'modified_petstore/petstore.yaml'
[info] Will fail build on errors of severity: MUST, SHOULD
[info] 
[info] Ignored rules 
[info] --------------
[info] 104 - MUST - Secure Endpoints
[info] 174 - MUST - Use common field names
[info] 
[info] Error descriptions
[info] ------------------
[info] 104 - MUST - Secure Endpoints - https://zalando.github.io/restful-api-guidelines/#104
[info] 105 - MUST - Secure All Endpoints With Scopes - https://zalando.github.io/restful-api-guidelines/#105
[info] 110 - MUST - Response As JSON Object - https://zalando.github.io/restful-api-guidelines/#110
[info] 115 - MUST - Do Not Use URI Versioning - https://zalando.github.io/restful-api-guidelines/#115
[info] 118 - MUST - Property Names Must be ASCII snake_case - https://zalando.github.io/restful-api-guidelines/#118
[info] 132 - SHOULD - Use uppercase separate words with hyphens for HTTP headers - https://zalando.github.io/restful-api-guidelines/#132
[info] 151 - SHOULD - Specify Success and Error Responses - https://zalando.github.io/restful-api-guidelines/#151
[info] 174 - MUST - Use common field names - https://zalando.github.io/restful-api-guidelines/#174
[info] 176 - MUST - Use Problem JSON - https://zalando.github.io/restful-api-guidelines/#176
[info] 183 - MUST - Use Only the Specified Proprietary Zalando Headers - https://zalando.github.io/restful-api-guidelines/#183
[info] 215 - MUST - Provide API Identifier - https://zalando.github.io/restful-api-guidelines/#215
[info] 218 - MUST - Contain API Meta Information - https://zalando.github.io/restful-api-guidelines/#218
[info] 219 - MUST - Provide API Audience - https://zalando.github.io/restful-api-guidelines/#219
[info] 224 - MUST - Follow Naming Convention for Hostnames - https://zalando.github.io/restful-api-guidelines/#224
[warn] 
[warn] Severity MUST (18)
[warn] ------------------
[warn] 219 - API Audience must be provided - /info/x-audience
[warn] 215 - API Identifier should be provided - /info/x-api-id
[warn] 218 - Description has to be provided - /info/description
[warn] 218 - Contact name has to be provided - /info/contact/name
[warn] 218 - Contact URL has to be provided - /info/contact/url
[warn] 218 - Contact e-mail has to be provided - /info/contact/email
[warn] 224 - API audience null is not supported. - /info
[warn] 115 - URL contains version number - /servers/0
[warn] 183 - use only standardized or specified response headers - /paths/~1pets/get/responses/200/headers/x-next
[warn] 105 - Endpoint is not secured by scope(s) - /paths/~1orders~1{orderId}/put
[warn] 105 - Endpoint is not secured by scope(s) - /paths/~1pets/get
[warn] 105 - Endpoint is not secured by scope(s) - /paths/~1pets/post
[warn] 105 - Endpoint is not secured by scope(s) - /paths/~1pets~1{petId}/get
[warn] 118 - Property name has to be snake_case - /components/schemas/Error/properties/messageA
[warn] 110 - Always return JSON objects as top-level data structures to support extensibility - /components/schemas/Pets
[warn] 176 - Operations should return problem JSON when any problem occurs during processing whether caused by client or server. Media type have to be 'application/problem+json' - /paths/~1pets/get/responses/default/content/application~1json
[warn] 176 - Operations should return problem JSON when any problem occurs during processing whether caused by client or server. Media type have to be 'application/problem+json' - /paths/~1pets/post/responses/default/content/application~1json
[warn] 176 - Operations should return problem JSON when any problem occurs during processing whether caused by client or server. Media type have to be 'application/problem+json' - /paths/~1pets~1{petId}/get/responses/default/content/application~1json
[warn] 
[warn] Severity SHOULD (6)
[warn] -------------------
[warn] 224 - API audience null is not supported. - /info
[warn] 151 - operation should contain the default response - /paths/~1orders~1{orderId}/put
[warn] 151 - problem+json should be used as default response - /paths/~1pets/get/responses/default/content/application~1json
[warn] 151 - problem+json should be used as default response - /paths/~1pets/post/responses/default/content/application~1json
[warn] 151 - problem+json should be used as default response - /paths/~1pets~1{petId}/get/responses/default/content/application~1json
[warn] 132 - Header has to be Hyphenated-Pascal-Case - /paths/~1pets/get/responses/200/headers/x-next
[warn] 
[warn] Severity MAY (1)
[warn] ----------------
[warn] 224 - API audience null is not supported. - /info
[info] 
[info] Writing result file to /tmp/zally-maven-plugin3863169074762372296.yaml
[info] 
```
