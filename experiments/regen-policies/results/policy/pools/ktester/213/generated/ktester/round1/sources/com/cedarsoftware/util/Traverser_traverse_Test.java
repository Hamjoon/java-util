package com.cedarsoftware.util;

import java.util.function.Consumer;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Set;
import java.util.IdentityHashMap;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Test;
import java.util.TimeZone;
import java.util.Date;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Collections;
import java.util.Map;
import java.util.HashSet;
import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.LinkedHashMap;
// <-- added static import for JUnit assertions
import static org.junit.jupiter.api.Assertions.*;

class Traverser_traverse_Test {

    /* --------------------------------------------------------------------- */
    static class Beta {

        int age;

        Map<Object, Object> friends;

        Charlie charlie;
    }

    static class Charlie {

        double salary;

        Collection<Object> timezones;

        Object[] dates;

        Alpha alpha;

        TimeZone zone = TimeZone.getDefault();
    }

    static class Alpha {

        String name;

        Collection<Object> contacts;

        Beta beta;
    }

    /**
     * Information about a node that has been visited during traversal.
     */
    public static final class NodeVisit {

        private final Object node;

        private final Map<Field, Object> fields;

        NodeVisit(Object node, Map<Field, Object> fields) {
            this.node = node;
            this.fields = Collections.unmodifiableMap(fields);
        }

        /**
         * @return the object instance that was visited
         */
        public Object getNode() {
            return node;
        }

        /**
         * @return an immutable map of the fields (including inherited) and their values
         */
        public Map<Field, Object> getFields() {
            return fields;
        }
    }

    /**
     * Traverses the object graph with complete node visiting capabilities.
     *
     * @param root          the root object to start traversal
     * @param visitor       visitor that receives detailed node information (must not be {@code null})
     * @param classesToSkip classes to skip during traversal (can be {@code null})
     */
    public static void traverse(Object root, Consumer<NodeVisit> visitor, Set<Class<?>> classesToSkip) {
        Convention.throwIfNull(visitor, "visitor cannot be null");
        if (root == null) {
            return;
        }
        if (classesToSkip == null) {
            classesToSkip = Collections.emptySet();
        }
        // Tracks objects that have already been visited – identity based to handle cycles correctly
        IdentityHashMap<Object, Boolean> visited = new IdentityHashMap<>();
        // Simple depth‑first stack
        Deque<Object> stack = new ArrayDeque<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            Object current = stack.pop();
            if (current == null) {
                continue;
            }
            // Skip if we have already processed this exact instance
            if (visited.containsKey(current)) {
                continue;
            }
            visited.put(current, Boolean.TRUE);
            Class<?> curClass = current.getClass();
            // If the class is in the skip set we do NOT notify the visitor and we do NOT walk its contents
            if (classesToSkip.contains(curClass)) {
                continue;
            }
            // Gather field information for the visitor
            Map<Field, Object> fieldMap = new LinkedHashMap<>();
            for (Class<?> c = curClass; c != null; c = c.getSuperclass()) {
                for (Field f : c.getDeclaredFields()) {
                    f.setAccessible(true);
                    Object value;
                    try {
                        value = f.get(current);
                    } catch (IllegalAccessException e) {
                        // Should not happen because we called setAccessible(true)
                        continue;
                    }
                    fieldMap.put(f, value);
                }
            }
            // Notify the visitor about the current node
            visitor.accept(new NodeVisit(current, fieldMap));
            // Enqueue children (fields, collection elements, map entries, array elements)
            // 1) Field values
            for (Object value : fieldMap.values()) {
                enqueueIfRelevant(value, visited, classesToSkip, stack);
            }
            // 2) Iterable contents
            if (current instanceof Iterable<?>) {
                for (Object element : (Iterable<?>) current) {
                    enqueueIfRelevant(element, visited, classesToSkip, stack);
                }
            }
            // 3) Map contents (keys and values)
            if (current instanceof Map<?, ?>) {
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) current).entrySet()) {
                    enqueueIfRelevant(entry.getKey(), visited, classesToSkip, stack);
                    enqueueIfRelevant(entry.getValue(), visited, classesToSkip, stack);
                }
            }
            // 4) Array contents
            if (curClass.isArray()) {
                int len = Array.getLength(current);
                for (int i = 0; i < len; i++) {
                    Object element = Array.get(current, i);
                    enqueueIfRelevant(element, visited, classesToSkip, stack);
                }
            }
        }
    }

    /**
     * Helper that decides whether a value should be traversed further.
     */
    private static void enqueueIfRelevant(Object value, IdentityHashMap<Object, Boolean> visited, Set<Class<?>> classesToSkip, Deque<Object> stack) {
        if (value == null) {
            return;
        }
        // Primitive wrappers, String, Enum, Class, and other immutable “leaf” types are not traversed
        Class<?> vc = value.getClass();
        if (isLeafType(vc)) {
            // Still honour the skip set – if the leaf type is explicitly requested to be skipped,
            // we simply do not enqueue it (the visitor will never see it because we never called
            // visitor for it in the first place).
            if (classesToSkip.contains(vc)) {
                return;
            }
            // Leaf types are not traversed further
            return;
        }
        // If the class is in the skip set we do not enqueue it at all
        if (classesToSkip.contains(vc)) {
            return;
        }
        // Avoid re‑adding objects that have already been visited
        if (!visited.containsKey(value)) {
            stack.push(value);
        }
    }

    /**
     * Determines whether a type should be considered a leaf in the object graph.
     * Leaf types are not traversed into (their fields are not inspected).
     */
    private static boolean isLeafType(Class<?> cls) {
        return cls.isPrimitive() || cls == String.class || Number.class.isAssignableFrom(cls) || Boolean.class == cls || Character.class == cls || cls.isEnum() || cls == Class.class || // cover common JDK temporal types
        cls == java.time.temporal.Temporal.class;
    }

    /**
     * Verify that a {@code null} visitor results in an {@link IllegalArgumentException}.
     */
    @Test
    void testTraverse_nullVisitor_throwsException() {
        Alpha root = new Alpha();
        // 1️⃣ visitor = null, skip set non‑null (empty)
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> Traverser.traverse(root, (Consumer<Traverser.NodeVisit>) null, Collections.emptySet()));
        assertEquals("visitor cannot be null", ex1.getMessage());
        // 2️⃣ visitor = null, skip set = null (the method should still fail early)
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> Traverser.traverse(root, (Consumer<Traverser.NodeVisit>) null, (Set<Class<?>>) null));
        assertEquals("visitor cannot be null", ex2.getMessage());
    }

    /**
     * Verify that the traverser visits each distinct node in the object graph and that
     * the {@code classesToSkip} set correctly prevents visitation of skipped types.
     */
    @Test
    void testTraverse_visitsAllNodesAndRespectsSkipSet() {
        // Build a small cyclic graph (similar to the legacy test)
        Alpha alpha = new Alpha();
        Beta beta = new Beta();
        Charlie charlie = new Charlie();
        alpha.name = "alpha";
        alpha.beta = beta;
        alpha.contacts = new ArrayList<>();
        alpha.contacts.add(beta);
        alpha.contacts.add(charlie);
        alpha.contacts.add("Harry");
        beta.age = 45;
        beta.charlie = charlie;
        beta.friends = new LinkedHashMap<>();
        beta.friends.put("Tom", "Tom Jones");
        beta.friends.put(alpha, "Alpha beta");
        beta.friends.put("beta", beta);
        charlie.salary = 150_000.01;
        charlie.alpha = alpha;
        charlie.timezones = new LinkedList<>();
        charlie.timezones.add(TimeZone.getTimeZone("EST"));
        charlie.timezones.add(TimeZone.getTimeZone("GMT"));
        charlie.dates = new Date[] { new Date() };
        // Counters for each type we care about
        final int[] alphaCount = { 0 };
        final int[] betaCount = { 0 };
        final int[] charlieCount = { 0 };
        final int[] timeZoneCount = { 0 };
        Consumer<Traverser.NodeVisit> visitor = visit -> {
            Object node = visit.getNode();
            if (node instanceof Alpha) {
                alphaCount[0]++;
            } else if (node instanceof Beta) {
                betaCount[0]++;
            } else if (node instanceof Charlie) {
                charlieCount[0]++;
            } else if (node instanceof TimeZone) {
                timeZoneCount[0]++;
            }
        };
        // ---- Traversal without skipping any class ----
        Traverser.traverse(alpha, visitor, Collections.emptySet());
        assertEquals(1, alphaCount[0], "Alpha should be visited exactly once");
        assertEquals(1, betaCount[0], "Beta should be visited exactly once");
        assertEquals(1, charlieCount[0], "Charlie should be visited exactly once");
        assertTrue(timeZoneCount[0] >= 1, "At least one TimeZone should be visited");
        // Reset counters
        alphaCount[0] = betaCount[0] = charlieCount[0] = timeZoneCount[0] = 0;
        // ---- Traversal with TimeZone class in the skip set ----
        Set<Class<?>> skip = new HashSet<>();
        skip.add(TimeZone.class);
        Traverser.traverse(alpha, visitor, skip);
        assertEquals(1, alphaCount[0], "Alpha should still be visited");
        assertEquals(1, betaCount[0], "Beta should still be visited");
        assertEquals(1, charlieCount[0], "Charlie should still be visited");
        assertEquals(0, timeZoneCount[0], "TimeZone instances should be skipped");
    }

    /**
     * Verify that traversal works when {@code classesToSkip} is {@code null}.
     * The visitor should be invoked for every distinct node in the graph.
     */
    @Test
    void testTraverse_nullClassesToSkip_visitsAllNodes() {
        // ---- build a small cyclic object graph ----
        Alpha alpha = new Alpha();
        Beta beta = new Beta();
        Charlie charlie = new Charlie();
        alpha.name = "alpha";
        alpha.beta = beta;
        alpha.contacts = new ArrayList<>();
        alpha.contacts.add(beta);
        alpha.contacts.add(charlie);
        // a plain String (should be visited)
        alpha.contacts.add("Harry");
        beta.age = 45;
        beta.charlie = charlie;
        beta.friends = new LinkedHashMap<>();
        // String key/value
        beta.friends.put("Tom", "Tom Jones");
        // object key (alpha) – creates a cycle
        beta.friends.put(alpha, "Alpha beta");
        // object value (beta) – creates a cycle
        beta.friends.put("beta", beta);
        charlie.salary = 150_000.01;
        // back‑reference to Alpha (cycle)
        charlie.alpha = alpha;
        charlie.timezones = new LinkedList<>();
        charlie.timezones.add(TimeZone.getTimeZone("EST"));
        charlie.timezones.add(TimeZone.getTimeZone("GMT"));
        charlie.dates = new Date[] { new Date() };
        // ---- counters for each type we care about ----
        final int[] alphaCount = { 0 };
        final int[] betaCount = { 0 };
        final int[] charlieCount = { 0 };
        final int[] stringCount = { 0 };
        final int[] timeZoneCount = { 0 };
        Consumer<Traverser.NodeVisit> visitor = visit -> {
            Object node = visit.getNode();
            if (node instanceof Alpha) {
                alphaCount[0]++;
            } else if (node instanceof Beta) {
                betaCount[0]++;
            } else if (node instanceof Charlie) {
                charlieCount[0]++;
            } else if (node instanceof String) {
                stringCount[0]++;
            } else if (node instanceof TimeZone) {
                timeZoneCount[0]++;
            }
        };
        // ---- invoke traversal with a null skip set ----
        Traverser.traverse(alpha, visitor, (Set<Class<?>>) null);
        // ---- assertions -------------------------------------------------
        assertEquals(1, alphaCount[0], "Alpha should be visited exactly once");
        assertEquals(1, betaCount[0], "Beta should be visited exactly once");
        assertEquals(1, charlieCount[0], "Charlie should be visited exactly once");
        assertEquals(1, stringCount[0], "The String \"Harry\" should be visited exactly once");
        assertTrue(timeZoneCount[0] >= 1, "At least one TimeZone instance should be visited");
    }
}
