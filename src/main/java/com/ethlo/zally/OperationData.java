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

import io.swagger.v3.core.model.ApiDescription;
import io.swagger.v3.oas.models.Operation;

import java.util.List;
import java.util.Map;

public class OperationData
{
    private final Map<String, Object> extensions;
    private final String method;
    private final String path;
    private final Boolean deprecated;
    private final String operationId;
    private final List<String> tags;

    public OperationData(final Operation operation, final ApiDescription api, final Map<String, Object> extensions)
    {
        this.method = api.getMethod();
        this.path = api.getPath();
        this.deprecated = operation.getDeprecated();
        this.operationId = operation.getOperationId();
        this.tags = operation.getTags();
        this.extensions = extensions;
    }

    public Map<String, Object> getExtensions()
    {
        return extensions;
    }

    public String getMethod()
    {
        return method;
    }

    public String getPath()
    {
        return path;
    }

    public Boolean getDeprecated()
    {
        return deprecated;
    }

    public String getOperationId()
    {
        return operationId;
    }

    public List<String> getTags()
    {
        return tags;
    }
}
