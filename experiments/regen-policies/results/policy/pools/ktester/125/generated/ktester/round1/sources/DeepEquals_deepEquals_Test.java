package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.util.concurrent.atomic.*;
import java.util.stream.*;
// <-- added import for DayOfWeek
import java.time.DayOfWeek;

@ExtendWith(MockitoExtension.class)
class DeepEquals_deepEquals_Test {

    /**
     * Shared mutable map used by the option‑based tests.
     */
    private Map<String, Object> options;

    /**
     * Simple POJO with a custom {@code equals()} that only looks at {@code name}.
     * This mirrors the behaviour used in the original library's test suite.
     */
    static class Person {

        final String name;

        final int age;

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
            Person p = (Person) o;
            // deliberately ignore age
            return Objects.equals(name, p.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }

    /**
     * Minimal node class that can reference another node, used to build a
     * circular object graph.
     */
    static class Node {

        final String id;

        Node parent;

        Node child;

        Node(String id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof Node))
                return false;
            Node n = (Node) o;
            return Objects.equals(id, n.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }

    /**
     * Simple point class without an overridden {@code equals()} – field‑by‑field
     * comparison should be used.
     */
    static class Point {

        int x, y;

        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    /**
     * Class that implements identity‑based equality (i.e. {@code ==} only).
     */
    static class Identity {

        final int id;

        Identity(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            // identity based
            return this == o;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(this);
        }
    }

    /**
     * Self‑referential node used for the “Self‑referential objects (cyclic but
     * identical)” test case.
     */
    static class SelfNode {

        final int id;

        SelfNode next;

        SelfNode(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            return this == o;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }

    /**
     * Collection whose iterator throws a {@link RuntimeException} on the first
     * call to {@code next()} – used to verify that DeepEquals propagates the
     * exception.
     */
    static class FaultyCollection implements Collection<Integer> {

        private final List<Integer> data = List.of(1, 2, 3);

        @Override
        public Iterator<Integer> iterator() {
            return new Iterator<>() {

                private final Iterator<Integer> it = data.iterator();

                private boolean first = true;

                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public Integer next() {
                    if (first) {
                        first = false;
                        throw new RuntimeException("Iterator failure");
                    }
                    return it.next();
                }
            };
        }

        // Boiler‑plate methods – not used by the test
        @Override
        public int size() {
            return data.size();
        }

        @Override
        public boolean isEmpty() {
            return data.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return data.contains(o);
        }

        @Override
        public Object[] toArray() {
            return data.toArray();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return data.toArray(a);
        }

        @Override
        public boolean add(Integer integer) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return data.containsAll(c);
        }

        @Override
        public boolean addAll(Collection<? extends Integer> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        options = new HashMap<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure each test starts with a clean options map.
        options.clear();
    }

    static Stream<Arguments> nullHandlingProvider() {
        return Stream.of(Arguments.of("Both null", null, null, true), Arguments.of("First null, second non‑null", null, 42, false), Arguments.of("Second null, first non‑null", "non‑null", null, false));
    }

    static Stream<Arguments> primitiveWrapperProvider() {
        return Stream.of(Arguments.of("Integer vs Integer same value", Integer.valueOf(5), Integer.valueOf(5), true), Arguments.of("Integer vs Double (different wrapper types)", Integer.valueOf(5), Double.valueOf(5.0), false));
    }

    static Stream<Arguments> stringNumberOptionProvider() {
        return Stream.of(Arguments.of("Matching numeric string", "456", 456, true), Arguments.of("Non‑numeric string", "notANumber", 0, false));
    }

    static Stream<Arguments> arrayEqualityProvider() {
        return Stream.of(Arguments.of("int[] equal", new int[] { 1, 2, 3 }, new int[] { 1, 2, 3 }, true), Arguments.of("int[] different element", new int[] { 1, 2, 3 }, new int[] { 1, 2, 4 }, false), Arguments.of("int[][] nested equal", new int[][] { { 1, 2 }, { 3, 4 } }, new int[][] { { 1, 2 }, { 3, 4 } }, true));
    }

    static Stream<Arguments> collectionEqualityProvider() {
        return Stream.of(Arguments.of("List same order", List.of(1, 2), List.of(1, 2), true), Arguments.of("List different order", List.of(1, 2), List.of(2, 1), false), Arguments.of("Set irrespective of order", Set.of(1, 2), new HashSet<>(List.of(2, 1)), true));
    }

    static Stream<Arguments> mapEqualityProvider() {
        Map<String, Integer> m1 = new LinkedHashMap<>();
        m1.put("a", 1);
        m1.put("b", 2);
        Map<String, Integer> m2 = new LinkedHashMap<>();
        m2.put("b", 2);
        m2.put("a", 1);
        Map<String, Integer> m3 = new LinkedHashMap<>();
        m3.put("a", 1);
        m3.put("b", 3);
        return Stream.of(Arguments.of("Maps with same entries (different order)", m1, m2, true), Arguments.of("Maps differing in a value", m1, m3, false));
    }

    @Test
    void testSameObjectEquals() {
        Date now = new Date();
        assertTrue(DeepEquals.deepEquals(now, now));
    }

    @Test
    void testEqualsWithNulls() {
        Date d = new Date();
        assertFalse(DeepEquals.deepEquals(null, d));
        assertFalse(DeepEquals.deepEquals(d, null));
        assertTrue(DeepEquals.deepEquals(null, null));
    }

    @Test
    void testPrimitiveArrayEquality() {
        int[] a = { 1, 2, 3 };
        int[] b = { 1, 2, 3 };
        int[] c = { 1, 2, 4 };
        assertTrue(DeepEquals.deepEquals(a, b));
        assertFalse(DeepEquals.deepEquals(a, c));
    }

    @Test
    void testObjectArrayEquality() {
        String[] a = { "alpha", "beta", null };
        String[] b = { "alpha", "beta", null };
        String[] c = { "alpha", "beta", "gamma" };
        assertTrue(DeepEquals.deepEquals(a, b));
        assertFalse(DeepEquals.deepEquals(a, c));
    }

    @Test
    void testCollectionEquality() {
        List<String> l1 = Arrays.asList("zulu", "alpha", "bravo");
        List<String> l2 = Arrays.asList("zulu", "alpha", "bravo");
        List<String> l3 = Arrays.asList("zulu", "alpha", "charlie");
        assertTrue(DeepEquals.deepEquals(l1, l2));
        assertFalse(DeepEquals.deepEquals(l1, l3));
    }

    @Test
    void testMapEquality() {
        Map<String, Integer> m1 = new LinkedHashMap<>();
        m1.put("one", 1);
        m1.put("two", 2);
        Map<String, Integer> m2 = new LinkedHashMap<>();
        m2.put("one", 1);
        m2.put("two", 2);
        Map<String, Integer> m3 = new LinkedHashMap<>();
        m3.put("one", 1);
        m3.put("two", 3);
        assertTrue(DeepEquals.deepEquals(m1, m2));
        assertFalse(DeepEquals.deepEquals(m1, m3));
    }

    @Test
    void testIgnoreCustomEqualsOption() {
        Person p1 = new Person("Jim", 27);
        Person p2 = new Person("Jim", 34);
        // By default Person overrides equals() to compare only the name.
        assertTrue(p1.equals(p2));
        assertTrue(DeepEquals.deepEquals(p1, p2));
        // Tell DeepEquals to ignore Person's custom equals().
        Set<Class<?>> ignore = new HashSet<>();
        ignore.add(Person.class);
        options.put(DeepEquals.IGNORE_CUSTOM_EQUALS, ignore);
        // Now DeepEquals must compare fields and detect the age mismatch.
        assertFalse(DeepEquals.deepEquals(p1, p2, options));
        assertNotNull(options.get(DeepEquals.DIFF));
    }

    @Test
    void testAllowStringToMatchNumberOption() {
        String s = "42";
        Integer i = 42;
        // Without the option the values are not equal.
        assertFalse(DeepEquals.deepEquals(s, i));
        // Enable the option – now they are considered equal.
        options.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, Boolean.TRUE);
        assertTrue(DeepEquals.deepEquals(s, i, options));
    }

    @Test
    void testCircularReferenceEquality() {
        Node a = new Node("root");
        Node b = new Node("root");
        // Create a circular reference: a -> child -> a
        a.child = new Node("child");
        a.child.parent = a;
        // Same structure for b, but with a different instance for the child.
        b.child = new Node("child");
        b.child.parent = b;
        assertTrue(DeepEquals.deepEquals(a, b));
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nullHandlingProvider")
    void testNullHandling(String description, Object a, Object b, boolean expected) {
        assertEquals(expected, DeepEquals.deepEquals(a, b));
    }

    // -----------------------------------------------------------------
    @Test
    void testReferenceEquality() {
        Object obj = new Object();
        assertTrue(DeepEquals.deepEquals(obj, obj));
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("primitiveWrapperProvider")
    void testPrimitiveWrapperEquality(String description, Object a, Object b, boolean expected) {
        assertEquals(expected, DeepEquals.deepEquals(a, b));
    }

    // -----------------------------------------------------------------
    @Test
    void testStringToNumberDefault() {
        assertFalse(DeepEquals.deepEquals("123", 123));
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("stringNumberOptionProvider")
    void testStringNumberOption(String description, String s, Number n, boolean expected) {
        options.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, Boolean.TRUE);
        assertEquals(expected, DeepEquals.deepEquals(s, n, options));
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("arrayEqualityProvider")
    void testArrayEquality(String description, Object a, Object b, boolean expected) {
        assertEquals(expected, DeepEquals.deepEquals(a, b));
    }

    // -----------------------------------------------------------------
    @Test
    void testCustomEqualsDefault() {
        Person p1 = new Person("John", 30);
        Person p2 = new Person("John", 45);
        // default behaviour uses Person.equals() → true
        assertTrue(DeepEquals.deepEquals(p1, p2));
    }

    // -----------------------------------------------------------------
    @Test
    void testCustomEqualsIgnoredViaOption() {
        Person p1 = new Person("John", 30);
        Person p2 = new Person("John", 45);
        // empty set = ignore all custom equals
        options.put(DeepEquals.IGNORE_CUSTOM_EQUALS, Collections.emptySet());
        assertFalse(DeepEquals.deepEquals(p1, p2, options));
    }

    // -----------------------------------------------------------------
    @Test
    void testCyclicStructureEquality() {
        Node a = new Node("root");
        a.child = new Node("child");
        a.child.parent = a;
        Node b = new Node("root");
        b.child = new Node("child");
        b.child.parent = b;
        assertTrue(DeepEquals.deepEquals(a, b));
    }

    // -----------------------------------------------------------------
    @Test
    void testFallbackToObjectsEquals() {
        assertTrue(DeepEquals.deepEquals("simpleString", "simpleString"));
        assertFalse(DeepEquals.deepEquals("abc", "def"));
    }

    // -----------------------------------------------------------------
    @Test
    void testEmptyCollectionsAndMaps() {
        assertTrue(DeepEquals.deepEquals(new ArrayList<>(), new LinkedList<>()));
        assertTrue(DeepEquals.deepEquals(new HashSet<>(), new HashSet<>()));
        assertTrue(DeepEquals.deepEquals(new HashMap<>(), new LinkedHashMap<>()));
    }

    // -----------------------------------------------------------------
    @Test
    void testDifferentCollectionImplementations() {
        List<Integer> a = new ArrayList<>(List.of(1, 2, 3));
        List<Integer> b = new LinkedList<>(List.of(1, 2, 3));
        assertTrue(DeepEquals.deepEquals(a, b));
    }

    // -----------------------------------------------------------------
    @Test
    void testArrayVsListMismatch() {
        int[] array = { 1, 2, 3 };
        List<Integer> list = List.of(1, 2, 3);
        assertFalse(DeepEquals.deepEquals(array, list));
    }

    // -----------------------------------------------------------------
    @Test
    void testNullElementsInsideCollections() {
        List<Integer> a = Arrays.asList(null, 1, null);
        List<Integer> b = Arrays.asList(null, 1, null);
        List<Integer> c = Arrays.asList(null, 1, 2);
        List<Integer> d = Arrays.asList(1, null, 2);
        assertTrue(DeepEquals.deepEquals(a, b));
        assertFalse(DeepEquals.deepEquals(c, d));
    }

    // -----------------------------------------------------------------
    @Test
    void testCustomObjectWithoutEquals() {
        Point p1 = new Point(5, 10);
        Point p2 = new Point(5, 10);
        Point p3 = new Point(5, 11);
        assertTrue(DeepEquals.deepEquals(p1, p2));
        assertFalse(DeepEquals.deepEquals(p1, p3));
    }

    // -----------------------------------------------------------------
    @Test
    void testIdentityBasedEquals() {
        Identity i1 = new Identity(1);
        Identity i2 = new Identity(1);
        assertFalse(DeepEquals.deepEquals(i1, i2));
        assertTrue(DeepEquals.deepEquals(i1, i1));
    }

    // -----------------------------------------------------------------
    @Test
    void testOptionsParameterNullHandling() {
        // Passing a null options map should behave like the no‑option overload.
        assertFalse(DeepEquals.deepEquals("789", 789, null));
    }

    // -----------------------------------------------------------------
    @Test
    void testCombinedOptions() {
        Person p1 = new Person("John", 30);
        Person p2 = new Person("John", 45);
        options.put(DeepEquals.IGNORE_CUSTOM_EQUALS, Collections.emptySet());
        options.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, Boolean.TRUE);
        // age differs → false even though string‑to‑number option is irrelevant here
        assertFalse(DeepEquals.deepEquals(p1, p2, options));
    }

    // -----------------------------------------------------------------
    @Test
    void testDateEquality() {
        Date d1 = new Date(1625097600000L);
        Date d2 = new Date(1625097600000L);
        Date d3 = new Date(1625097601000L);
        assertTrue(DeepEquals.deepEquals(d1, d2));
        assertFalse(DeepEquals.deepEquals(d1, d3));
    }

    // -----------------------------------------------------------------
    @Test
    void testUuidEquality() {
        UUID u1 = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        UUID u2 = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        assertTrue(DeepEquals.deepEquals(u1, u2));
    }

    // -----------------------------------------------------------------
    @Test
    void testAtomicEquality() {
        AtomicInteger ai1 = new AtomicInteger(7);
        AtomicInteger ai2 = new AtomicInteger(7);
        AtomicLong al1 = new AtomicLong(100);
        AtomicLong al2 = new AtomicLong(101);
        assertTrue(DeepEquals.deepEquals(ai1, ai2));
        assertFalse(DeepEquals.deepEquals(al1, al2));
    }

    // -----------------------------------------------------------------
    @Test
    void testBigDecimalScaleSensitivity() {
        BigDecimal bd1 = new BigDecimal("1.0");
        BigDecimal bd2 = new BigDecimal("1.00");
        assertFalse(DeepEquals.deepEquals(bd1, bd2));
    }

    // -----------------------------------------------------------------
    @Test
    void testEnumEquality() {
        // Use java.time.DayOfWeek enum instead of a missing custom enum.
        assertTrue(DeepEquals.deepEquals(DayOfWeek.MONDAY, DayOfWeek.MONDAY));
        assertFalse(DeepEquals.deepEquals(DayOfWeek.MONDAY, DayOfWeek.FRIDAY));
    }

    // -----------------------------------------------------------------
    @Test
    void testMapWithNullKeyAndValue() {
        Map<String, String> a = new HashMap<>();
        a.put(null, null);
        a.put("key1", "value1");
        Map<String, String> b = new HashMap<>();
        b.put(null, null);
        b.put("key1", "value1");
        assertTrue(DeepEquals.deepEquals(a, b));
    }

    // -----------------------------------------------------------------
    @Test
    void testSetWithDuplicateElements() {
        Set<Integer> a = new HashSet<>(Arrays.asList(1, 2, 2, 3));
        Set<Integer> b = new HashSet<>(Arrays.asList(3, 2, 1));
        assertTrue(DeepEquals.deepEquals(a, b));
    }

    // -----------------------------------------------------------------
    @Test
    void testSelfReferentialObjects() {
        SelfNode a = new SelfNode(1);
        // self reference
        a.next = a;
        SelfNode b = new SelfNode(1);
        b.next = b;
        assertTrue(DeepEquals.deepEquals(a, b));
    }

    // -----------------------------------------------------------------
    @Test
    void testOptionsTypeMismatchIgnoreCustomEquals() {
        // wrong type
        options.put(DeepEquals.IGNORE_CUSTOM_EQUALS, "true");
        assertThrows(ClassCastException.class, () -> DeepEquals.deepEquals("test", "test", options));
    }

    @Test
    void testOptionsTypeMismatchAllowStrings() {
        // wrong type
        options.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, 1);
        assertThrows(ClassCastException.class, () -> DeepEquals.deepEquals("123", 123, options));
    }

    // -----------------------------------------------------------------
    @Test
    void testOptionsMapContainsNullKey() {
        options.put(null, Boolean.TRUE);
        options.put(DeepEquals.IGNORE_CUSTOM_EQUALS, Collections.emptySet());
        assertThrows(NullPointerException.class, () -> DeepEquals.deepEquals("x", "x", options));
    }

    // -----------------------------------------------------------------
    @Test
    void testFaultyCollectionIterator() {
        FaultyCollection a = new FaultyCollection();
        FaultyCollection b = new FaultyCollection();
        assertThrows(RuntimeException.class, () -> DeepEquals.deepEquals(a, b));
    }
}
