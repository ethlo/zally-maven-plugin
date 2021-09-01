package com.ethlo.zally;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.Arrays;
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

    private Map.Entry<Method, Operation> getOperation(PathItem value)
    {
        return getOperationByMethod(Method.GET, value.getGet())
                .orElseGet(() -> getOperationByMethod(Method.POST, value.getPost())
                        .orElseGet(() -> getOperationByMethod(Method.PUT, value.getPut())
                                .orElseGet(() -> getOperationByMethod(Method.PATCH, value.getPatch())
                                        .orElseGet(() -> getOperationByMethod(Method.DELETE, value.getDelete())
                                                .orElseGet(() -> getOperationByMethod(Method.HEAD, value.getHead())
                                                        .orElseGet(() -> getOperationByMethod(Method.OPTIONS, value.getOptions())
                                                                .orElseThrow(() -> new IllegalStateException("No method found for pathItem: " + value))))))));
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
        final Node root = new Node("/");
        for (final Map.Entry<String, PathItem> pathEntry : paths.entrySet())
        {
            final Map.Entry<Method, Operation> methodOperationEntry = getOperation(pathEntry.getValue());
            root.addPath(pathEntry.getKey() + " [" + methodOperationEntry.getKey() + " - " + methodOperationEntry.getValue().getOperationId() + "]");
        }
        final ByteArrayOutputStream bout = new ByteArrayOutputStream();
        root.children.forEach(r -> this.printTree(r, bout));
        return bout.toString(StandardCharsets.UTF_8);
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

        public void addPath(final String pathUri)
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
}
