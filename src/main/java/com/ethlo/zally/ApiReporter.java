package com.ethlo.zally;

/*-
 * #%L
 * zally-maven-plugin
 * %%
 * Copyright (C) 2021 Morten Haraldsen (ethlo)
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

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.collect.Iterators;
import io.swagger.models.Method;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;

public class ApiReporter
{
    private final OpenAPI openAPI;

    public ApiReporter(final OpenAPI openAPI)
    {
        this.openAPI = openAPI;
    }

    private Map<Method, Operation> getOperations(PathItem value)
    {
        final Map<Method, Operation> operations = new LinkedHashMap<>();
        operations.put(Method.GET, value.getGet());
        operations.put(Method.PUT, value.getPut());
        operations.put(Method.DELETE, value.getDelete());
        operations.put(Method.HEAD, value.getHead());
        operations.put(Method.OPTIONS, value.getOptions());
        operations.put(Method.POST, value.getPost());
        operations.put(Method.PATCH, value.getPatch());
        operations.values().removeIf(Objects::isNull);
        return operations;
    }

    private Optional<Map.Entry<Method, Operation>> getOperationByMethod(Method method, Operation operation)
    {
        if (operation != null)
        {
            return Optional.of(new AbstractMap.SimpleEntry<>(method, operation));
        }
        return Optional.empty();
    }

    public String render()
    {
        final Paths paths = this.openAPI.getPaths();
        final Node root = new Node("");
        for (final Map.Entry<String, PathItem> pathEntry : paths.entrySet())
        {
            final Map<Method, Operation> methodOperations = getOperations(pathEntry.getValue());
            for (final Map.Entry<Method, Operation> methodOperationEntry : methodOperations.entrySet())
            {
                root.addPath(pathEntry.getKey(), methodOperationEntry.getKey(), methodOperationEntry.getValue().getOperationId());
            }
        }
        final ByteArrayOutputStream bout = new ByteArrayOutputStream();
        printTree(root, bout);
        return bout.toString(StandardCharsets.UTF_8);
    }

    private void print(String prefix, Node node, boolean isTail, boolean isRoot, PrintWriter out)
    {
        final String strConnector = isRoot ? "" : isTail ? "└── " : "├── ";
        final String strNode = node.item;
        out.println(prefix + strConnector + strNode);
        final Set<Node> children = node.getChildren();

        if (!children.isEmpty())
        {
            children.stream().limit(children.size() - 1)
                    .forEach(child ->
                            print(prefix + (isTail ? "    " : "│   "), child, false, false, out));

            print(prefix + (isTail ? "    " : "│   "), Iterators.getLast(children.iterator()), true, false, out);
        }
        out.flush();
    }

    public void printTree(Node ref, OutputStream out)
    {
        final PrintWriter pw = new PrintWriter(out);

        final Set<Node> roots = ref.getChildren();
        if (roots == null)
        {
            pw.println(ref + " does not exist");
            pw.flush();
            return;
        }
        else if (roots.isEmpty())
        {
            //pw.println(ref + " has no children");
            pw.flush();
            return;
        }
        print("", ref, true, true, pw);
    }

    static class Node
    {
        private final String item;
        private final Set<Node> children = new LinkedHashSet<>();

        public Node(final String item)
        {
            this.item = Objects.requireNonNull(item, "item cannot be null");
        }

        public String getItem()
        {
            return item;
        }

        public Set<Node> getChildren()
        {
            return children;
        }

        public void addPath(final String pathUri, final Method method, final String operationId)
        {
            final AtomicReference<Node> nodeRef = new AtomicReference<>(this);
            Arrays.stream(pathUri.split("/"))
                    .filter(p -> !p.isEmpty())
                    .forEach(pathPart ->
                    {
                        final Node newNode = nodeRef.get().getOrCreateNode(pathPart);
                        nodeRef.get().children.add(newNode);
                        nodeRef.set(newNode);
                    });
            nodeRef.get().children.add(new Node(method.name() + " - " + operationId));
        }

        private Node getOrCreateNode(final String item)
        {
            for (Node child : children)
            {
                if (child.getItem().equals(item))
                {
                    return child;
                }
            }
            return new Node(item);
        }

        @Override
        public boolean equals(final Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Node node = (Node) o;
            return Objects.equals(item, node.item);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(item);
        }

        @Override
        public String toString()
        {
            return "Node{" +
                    "item='" + item + '\'' +
                    ", children=" + children.size() +
                    '}';
        }
    }
}
