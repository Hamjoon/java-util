package com.cedarsoftware.util;

import java.util.*;

// import org.junit.*;
// import static org.junit.Assert.*;
// @ExtendWith(MockitoExtension.class)
class DeepEquals_deepEquals_2_Test {

    /**
     * Shared resources that could be reused by many tests.
     */
    private static Map<String, Object> sharedOptions;

    /**
     * Per‑test mutable state.
     */
    private Map<String, Object> options;

    /**
     * Simple POJO with a custom {@code equals()} that only checks the {@code name}.
     */
    private static final class Person {

        private final String name;

        private final int age;

        Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof Person))
                return false;
            Person other = (Person) o;
            return Objects.equals(this.name, other.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }
    }

    /**
     * Class whose {@code equals()} always returns {@code false}.
     */
    private static final class CustomEquals {

        private final int id;

        CustomEquals(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            // force DeepEquals to look at fields
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        public int getId() {
            return id;
        }
    }

    /**
     * Simple node that can form cyclic graphs.
     */
    private static final class Node {

        private String name;

        private Node next;

        Node(String name) {
            this.name = name;
        }

        static Node cycleA() {
            Node a = new Node("A");
            a.next = a;
            return a;
        }

        static Node cycleB() {
            Node b = new Node("B");
            Node c = new Node("C");
            b.next = c;
            c.next = b;
            return b;
        }

        public String getName() {
            return name;
        }

        public Node getNext() {
            return next;
        }
    }

    private static final class NullIdentityCase {

        final String description;

        final Object a;

        final Object b;

        final Map<String, ?> opts;

        final boolean expected;

        NullIdentityCase(String description, Object a, Object b, Map<String, ?> opts, boolean expected) {
            this.description = description;
            this.a = a;
            this.b = b;
            this.opts = opts;
            this.expected = expected;
        }
    }

    private static final class PrimitiveWrapperCase {

        final String description;

        final Object a;

        final Object b;

        final Map<String, ?> opts;

        final boolean expected;

        PrimitiveWrapperCase(String description, Object a, Object b, Map<String, ?> opts, boolean expected) {
            this.description = description;
            this.a = a;
            this.b = b;
            this.opts = opts;
            this.expected = expected;
        }
    }

    //     @BeforeAll
    static void setupBeforeAll() {
        sharedOptions = new HashMap<>();
    }

    //     @BeforeEach
    void setupBeforeEach() {
        options = new HashMap<>(sharedOptions);
    }

    //     @AfterEach
    void teardownAfterEach() {
        if (options != null) {
            options.clear();
        }
    }

    //     @AfterAll
    static void teardownAfterAll() {
        sharedOptions = null;
    }

    private static List<NullIdentityCase> nullIdentityCases() {
        Object same = new Object();
        List<NullIdentityCase> list = new ArrayList<>();
        list.add(new NullIdentityCase("Both arguments are null – method should consider them equal.", null, null, Collections.emptyMap(), true));
        list.add(new NullIdentityCase("One argument is null and the other is a non‑null object – should return false.", null, new Object(), Collections.emptyMap(), false));
        list.add(new NullIdentityCase("Both arguments refer to the same instance – deep‑equals should short‑circuit to true.", same, same, Collections.emptyMap(), true));
        return list;
    }

    private static List<PrimitiveWrapperCase> primitiveWrapperCases() {
        List<PrimitiveWrapperCase> list = new ArrayList<>();
        list.add(new PrimitiveWrapperCase("Two Integer objects with the same value should be considered equal.", Integer.valueOf(5), Integer.valueOf(5), Collections.emptyMap(), true));
        list.add(new PrimitiveWrapperCase("Two Integer objects with different values should not be equal.", Integer.valueOf(5), Integer.valueOf(6), Collections.emptyMap(), false));
        list.add(new PrimitiveWrapperCase("Different numeric wrapper types are not considered equal when no conversion option is supplied.", Integer.valueOf(5), Long.valueOf(5L), Collections.emptyMap(), false));
        return list;
    }

    /**
     * Returns the list of null‑identity test cases.
     */
    public static List<NullIdentityCase> getNullIdentityCases() {
        return Collections.unmodifiableList(nullIdentityCases());
    }

    /**
     * Returns the list of primitive‑wrapper test cases.
     */
    public static List<PrimitiveWrapperCase> getPrimitiveWrapperCases() {
        return Collections.unmodifiableList(primitiveWrapperCases());
    }

    /**
     * Provides access to a fresh options map for a test.
     */
    public Map<String, Object> newOptionsMap() {
        return new HashMap<>(sharedOptions);
    }
}
