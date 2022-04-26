package com.ethlo.zally;

/*-
 * #%L
 * zally-maven-plugin
 * %%
 * Copyright (C) 2021 - 2022 Morten Haraldsen (ethlo)
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

import java.util.Objects;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OperationFilter
{
    private Pattern matcher;
    private String pointer;
    private String expression;

    public OperationFilter()
    {

    }

    public OperationFilter(@JsonProperty("pointer") final String pointer, @JsonProperty("expression") final String expression)
    {
        this.pointer = pointer;
        setExpression(expression);
    }

    public String getPointer()
    {
        return pointer;
    }

    public String getExpression()
    {
        return expression;
    }

    public void setPointer(final String pointer)
    {
        this.pointer = pointer;
    }

    public void setExpression(final String expression)
    {
        this.expression = expression;
        this.matcher = Pattern.compile(expression);
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final OperationFilter that = (OperationFilter) o;
        return Objects.equals(pointer, that.pointer) && Objects.equals(expression, that.expression);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(pointer, expression);
    }

    @Override
    public String toString()
    {
        return "OperationFilter{" +
                "pointer='" + pointer + '\'' +
                ", expression='" + expression + '\'' +
                '}';
    }

    public String asString()
    {
        return pointer + "=" + expression;
    }

    public boolean isMatch(String value)
    {
        return matcher.matcher(value).matches();
    }
}
