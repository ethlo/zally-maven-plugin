# Zally-maven-plugin

[![Maven Central](https://img.shields.io/maven-central/v/com.ethlo.zally/zally-maven-plugin.svg)](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.ethlo.zally%22%20AND%20a%3A%22zally-maven-plugin%22)
[![Hex.pm](https://img.shields.io/hexpm/l/plug.svg)](LICENSE)
[![Build Status](https://travis-ci.org/ethlo/zally-maven-plugin.svg?branch=main)](https://travis-ci.org/ethlo/zally-maven-plugin)
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
        <!-- Write the result of the validation to file. Optional-->
        <resultFile>target/api_validation_result.yaml</resultFile>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>validate</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```
