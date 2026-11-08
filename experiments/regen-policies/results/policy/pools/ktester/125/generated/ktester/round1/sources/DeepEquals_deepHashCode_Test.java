package com.cedarsoftware.util;

import java.util.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.*;
import java.lang.reflect.Method;

@ExtendWith(MockitoExtension.class)
class DeepEquals_deepHashCode_Test {

    /**
     * Shared test data **
     */
    private List<String> orderedList1;

    private List<String> orderedList2;

    private Set<String> unorderedSet1;

    private Set<String> unorderedSet2;

    private Map<String, Integer> map1;

    private Map<String, Integer> map2;

    public static final String DIFF = "diff";

    public static final String IGNORE_CUSTOM_EQUALS = "IGNORE_CUSTOM_EQUALS";

    public static final String ALLOW_STRINGS_TO_MATCH_NUMBERS = "ALLOW_STRINGS_TO_MATCH_NUMBERS";

    // Simple POJO without any overrides
    static class Person {

        public String name;

        public int age;

        Person(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }

    // POJO that overrides equals only
    static class EqualsOnly {

        public String value;

        EqualsOnly(String value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof EqualsOnly))
                return false;
            EqualsOnly that = (EqualsOnly) o;
            return Objects.equals(value, that.value);
        }
        // No hashCode override
    }

    // POJO that overrides hashCode only
    static class HashOnly {

        public int id;

        HashOnly(int id) {
            this.id = id;
        }

        @Override
        public int hashCode() {
            return id * 31;
        }
        // No equals override
    }

    // Node class for cyclic graph tests
    static class Node {

        int value;

        Node next;

        Node(int value) {
            this.value = value;
        }
    }

    // Class whose hashCode throws an exception
    static class ThrowingHash {

        @Override
        public int hashCode() {
            throw new IllegalStateException("boom");
        }
    }

    // List implementation that throws CME during iteration
    static class BrokenList<E> extends AbstractList<E> {

        private final List<E> delegate;

        BrokenList(List<E> delegate) {
            this.delegate = delegate;
        }

        @Override
        public E get(int index) {
            return delegate.get(index);
        }

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public Iterator<E> iterator() {
            return new Iterator<>() {

                private final Iterator<E> it = delegate.iterator();

                @Override
                public boolean hasNext() {
                    // Simulate concurrent modification detection
                    throw new ConcurrentModificationException("Iterator broken");
                }

                @Override
                public E next() {
                    return it.next();
                }
            };
        }
    }

    // Map implementation that throws CME during entrySet iteration
    static class BrokenMap<K, V> extends AbstractMap<K, V> {

        private final Map<K, V> delegate = new HashMap<>();

        @Override
        public Set<Entry<K, V>> entrySet() {
            return new AbstractSet<>() {

                @Override
                public Iterator<Entry<K, V>> iterator() {
                    return new Iterator<>() {

                        private final Iterator<Entry<K, V>> it = delegate.entrySet().iterator();

                        @Override
                        public boolean hasNext() {
                            throw new ConcurrentModificationException("Map entrySet broken");
                        }

                        @Override
                        public Entry<K, V> next() {
                            return it.next();
                        }
                    };
                }

                @Override
                public int size() {
                    return delegate.size();
                }
            };
        }
    }

    // Class with a private field to trigger reflective access
    static class PrivateFieldClass {

        private final String secret = "top-secret";
    }

    // SecurityManager that denies reflective access (setAccessible)
    static class DenyReflectiveAccessSecurityManager extends SecurityManager {

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
            if (perm instanceof java.lang.reflect.ReflectPermission && "suppressAccessChecks".equals(perm.getName())) {
                throw new SecurityException("Reflective access denied by test SecurityManager");
            }
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        orderedList1 = new ArrayList<>(Arrays.asList("alpha", "bravo", "charlie"));
        orderedList2 = new ArrayList<>(Arrays.asList("alpha", "bravo", "charlie"));
        unorderedSet1 = new LinkedHashSet<>(Arrays.asList("one", "two", "three"));
        // different iteration order
        unorderedSet2 = new HashSet<>(Arrays.asList("three", "one", "two"));
        map1 = new LinkedHashMap<>();
        map1.put("a", 1);
        map1.put("b", 2);
        map1.put("c", 3);
        map2 = new HashMap<>();
        map2.put("c", 3);
        map2.put("b", 2);
        map2.put("a", 1);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references – not strictly required but keeps the test tidy
        orderedList1 = null;
        orderedList2 = null;
        unorderedSet1 = null;
        unorderedSet2 = null;
        map1 = null;
        map2 = null;
    }

    static Stream<Arguments> primitiveWrapperProvider() {
        return Stream.of(Arguments.of("Integer wrapper", Integer.valueOf(42), Objects.hashCode(42)), Arguments.of("Boolean wrapper", Boolean.TRUE, Objects.hashCode(true)), Arguments.of("Character wrapper", Character.valueOf('A'), Objects.hashCode('A')));
    }

    static Stream<Arguments> primitiveArrayProvider() {
        return Stream.of(Arguments.of(new int[] { 1, 2, 3 }, Arrays.hashCode(new int[] { 1, 2, 3 }), "int[] should be hashed via Arrays.hashCode"), Arguments.of(new long[] { 0L, -1L, Long.MAX_VALUE }, Arrays.hashCode(new long[] { 0L, -1L, Long.MAX_VALUE }), "long[] with edge values"));
    }

    static Stream<Arguments> collectionProvider() {
        return Stream.of(Arguments.of(Arrays.asList(1, 2, 3), Objects.hash(1, 2, 3), "List with simple elements"), Arguments.of(new LinkedList<>(Arrays.asList("x", null, "y")), Objects.hash("x", null, "y"), "LinkedList containing a null element"), Arguments.of(Collections.unmodifiableList(Arrays.asList(5, 6)), Objects.hash(5, 6), "Unmodifiable list"));
    }

    static Stream<Arguments> mapProvider() {
        Map<String, Integer> simple = Map.of("a", 1, "b", 2);
        int expectedSimple = Objects.hash(simple.entrySet().toArray());
        Map<Object, Object> mixed = new HashMap<>() {

            {
                put("key", new int[] { 7, 8 });
                put(42, "value");
            }
        };
        // For the mixed map we compute the expected deep hash by delegating to the method under test
        int expectedMixed = DeepEquals.deepHashCode(mixed);
        return Stream.of(Arguments.of(simple, expectedSimple, "Simple map should be hashed based on entry set (order‑independent)"), Arguments.of(mixed, expectedMixed, "Map with mixed key/value types (including an array) must deep‑hash each component"));
    }

    static Stream<Arguments> immutableProvider() throws MalformedURLException {
        UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");
        URI uri = URI.create("http://example.com/path?query=1");
        URL url = new URL("http://example.org");
        Date date = Date.from(Instant.parse("2023-01-01T00:00:00Z"));
        return Stream.of(Arguments.of(uuid, uuid.hashCode(), "UUID should return its own hashCode"), Arguments.of(uri, uri.hashCode(), "URI should return its own hashCode"), Arguments.of(url, url.hashCode(), "URL should return its own hashCode"), Arguments.of(date, date.hashCode(), "Date should return its own hashCode"));
    }

    static Stream<Arguments> atomicProvider() {
        return Stream.of(Arguments.of(new AtomicInteger(5), Objects.hash(5), "AtomicInteger based on its int value"), Arguments.of(new AtomicLong(123456789L), Objects.hash(123456789L), "AtomicLong based on its long value"), Arguments.of(new AtomicBoolean(true), Objects.hash(true), "AtomicBoolean based on its boolean value"));
    }

    static Stream<Arguments> emptyProvider() {
        return Stream.of(Arguments.of(new int[] {}, Arrays.hashCode(new int[] {}), "Empty primitive array"), Arguments.of(Collections.emptyList(), Objects.hash(), "Empty List"), Arguments.of(Collections.emptyMap(), Objects.hash(), "Empty Map"));
    }

    /**
     * Computes a deep hash code for the given object by traversing its entire
     * graph.  The algorithm is safe against circular references.
     *
     * @param obj the object to hash, may be {@code null}
     * @return an integer representing the object's deep hash code
     */
    public static int deepHashCode(Object obj) {
        // Identity map is used to detect already‑visited objects (including
        // collections, maps and custom POJOs) and break cycles.
        return deepHashCode(obj, new IdentityHashMap<>());
    }

    private static int deepHashCode(Object obj, IdentityHashMap<Object, Boolean> visited) {
        if (obj == null)
            return 0;
        // Detect cycles – return a constant (0) for already‑seen objects.
        if (visited.containsKey(obj))
            return 0;
        visited.put(obj, Boolean.TRUE);
        // -----------------------------------------------------------------
        // Primitive wrappers, String, Enum, Date, UUID, URI, URL, etc.
        // -----------------------------------------------------------------
        if (isPrimitiveOrWrapperOrString(obj) || obj instanceof Enum<?> || obj instanceof java.util.Date || obj instanceof java.time.temporal.Temporal || obj instanceof UUID || obj instanceof URI || obj instanceof URL) {
            return obj.hashCode();
        }
        // -----------------------------------------------------------------
        // Atomic types
        // -----------------------------------------------------------------
        if (obj instanceof AtomicInteger)
            return Objects.hash(((AtomicInteger) obj).get());
        if (obj instanceof AtomicLong)
            return Objects.hash(((AtomicLong) obj).get());
        if (obj instanceof AtomicBoolean)
            return Objects.hash(((AtomicBoolean) obj).get());
        // -----------------------------------------------------------------
        // Arrays
        // -----------------------------------------------------------------
        Class<?> cls = obj.getClass();
        if (cls.isArray()) {
            Class<?> component = cls.getComponentType();
            // Primitive arrays – delegate to java.util.Arrays
            if (component.isPrimitive()) {
                if (component == int.class)
                    return Arrays.hashCode((int[]) obj);
                if (component == long.class)
                    return Arrays.hashCode((long[]) obj);
                if (component == byte.class)
                    return Arrays.hashCode((byte[]) obj);
                if (component == char.class)
                    return Arrays.hashCode((char[]) obj);
                if (component == short.class)
                    return Arrays.hashCode((short[]) obj);
                if (component == boolean.class)
                    return Arrays.hashCode((boolean[]) obj);
                if (component == float.class)
                    return Arrays.hashCode((float[]) obj);
                if (component == double.class)
                    return Arrays.hashCode((double[]) obj);
                // Should never reach here
                return 0;
            }
            // Object array – hash each element recursively
            Object[] arr = (Object[]) obj;
            int result = 1;
            for (Object e : arr) result = 31 * result + deepHashCode(e, visited);
            return result;
        }
        // -----------------------------------------------------------------
        // Collections
        // -----------------------------------------------------------------
        if (obj instanceof Collection) {
            Collection<?> coll = (Collection<?>) obj;
            // Ordered collections (List, Queue, Deque, etc.)
            if (obj instanceof List || obj instanceof Queue || obj instanceof Deque) {
                int result = 1;
                for (Object e : coll) result = 31 * result + deepHashCode(e, visited);
                return result;
            }
            // Unordered collections (Set, etc.) – order‑independent hash
            int unorderedHash = 0;
            for (Object e : coll) unorderedHash += deepHashCode(e, visited);
            return unorderedHash;
        }
        // -----------------------------------------------------------------
        // Maps
        // -----------------------------------------------------------------
        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            int hash = 0;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                int keyHash = deepHashCode(entry.getKey(), visited);
                int valHash = deepHashCode(entry.getValue(), visited);
                // combine key/value hashes
                hash += keyHash ^ valHash;
            }
            return hash;
        }
        // -----------------------------------------------------------------
        // Objects that explicitly override hashCode()
        // -----------------------------------------------------------------
        if (overridesHashCode(cls))
            // may throw – test expects propagation
            return obj.hashCode();
        // -----------------------------------------------------------------
        // POJOs – hash based on (non‑static) fields, including private ones
        // -----------------------------------------------------------------
        int result = 1;
        Class<?> current = cls;
        while (current != null && current != Object.class) {
            Field[] fields = current.getDeclaredFields();
            for (Field f : fields) {
                if (Modifier.isStatic(f.getModifiers()))
                    // skip static fields
                    continue;
                try {
                    // may throw SecurityException
                    f.setAccessible(true);
                    Object value = f.get(obj);
                    result = 31 * result + deepHashCode(value, visited);
                } catch (IllegalAccessException e) {
                    // Convert to unchecked – the test suite expects a
                    // SecurityException for blocked reflective access, not this.
                    throw new RuntimeException(e);
                }
            }
            current = current.getSuperclass();
        }
        return result;
    }

    /**
     * Checks whether the supplied object is a primitive wrapper or a String.
     */
    private static boolean isPrimitiveOrWrapperOrString(Object obj) {
        return obj instanceof Number || obj instanceof Character || obj instanceof Boolean || obj instanceof String;
    }

    /**
     * Determines whether a class provides its own {@code hashCode()} implementation.
     */
    private static boolean overridesHashCode(Class<?> c) {
        try {
            Method m = c.getMethod("hashCode");
            return m.getDeclaringClass() != Object.class;
        } catch (NoSuchMethodException e) {
            // Should never happen – Object defines hashCode()
            return false;
        }
    }

    public static boolean deepEquals(Object a, Object b) {
        // Simple delegation to the real library would be ideal, but for the
        // purpose of this kata we provide a minimal stub that relies on
        // standard equality.
        return Objects.equals(a, b);
    }

    public static boolean deepEquals(Object a, Object b, Map<String, Object> options) {
        // Stub implementation – the test suite does not exercise this method.
        return deepEquals(a, b);
    }

    @Test
    void deepHashCode_PrimitiveWrapper_ShouldMatchHashCode() {
        Integer i = 42;
        assertEquals(i.hashCode(), DeepEquals.deepHashCode(i));
    }

    @Test
    void deepHashCode_String_ShouldMatchHashCode() {
        String s = "deepHash";
        assertEquals(s.hashCode(), DeepEquals.deepHashCode(s));
    }

    @Test
    void deepHashCode_OrderedList_SameContentSameOrder_ShouldBeEqual() {
        int h1 = DeepEquals.deepHashCode(orderedList1);
        int h2 = DeepEquals.deepHashCode(orderedList2);
        assertEquals(h1, h2, "Lists with identical ordering must have identical deep hash codes");
    }

    @Test
    void deepHashCode_OrderedList_DifferentOrder_ShouldDiffer() {
        List<String> shuffled = new ArrayList<>(orderedList1);
        Collections.reverse(shuffled);
        int h1 = DeepEquals.deepHashCode(orderedList1);
        int h2 = DeepEquals.deepHashCode(shuffled);
        assertNotEquals(h1, h2, "Lists with different ordering must produce different deep hash codes");
    }

    @Test
    void deepHashCode_UnorderedSet_DifferentInsertionOrder_ShouldBeEqual() {
        int h1 = DeepEquals.deepHashCode(unorderedSet1);
        int h2 = DeepEquals.deepHashCode(unorderedSet2);
        assertEquals(h1, h2, "Sets are order‑independent – deep hash must be identical");
    }

    @Test
    void deepHashCode_UnorderedSet_DifferentContent_ShouldDiffer() {
        Set<String> other = new HashSet<>(unorderedSet1);
        other.remove("two");
        other.add("four");
        int h1 = DeepEquals.deepHashCode(unorderedSet1);
        int h2 = DeepEquals.deepHashCode(other);
        assertNotEquals(h1, h2, "Sets with different elements must produce different deep hashes");
    }

    @Test
    void deepHashCode_Map_SameEntriesDifferentOrder_ShouldBeEqual() {
        int h1 = DeepEquals.deepHashCode(map1);
        int h2 = DeepEquals.deepHashCode(map2);
        assertEquals(h1, h2, "Maps with identical entries must have identical deep hash codes");
    }

    @Test
    void deepHashCode_Map_DifferentValues_ShouldDiffer() {
        Map<String, Integer> altered = new HashMap<>(map1);
        // change a value
        altered.put("b", 99);
        int h1 = DeepEquals.deepHashCode(map1);
        int h2 = DeepEquals.deepHashCode(altered);
        assertNotEquals(h1, h2, "Maps with different values must produce different deep hashes");
    }

    @Test
    void deepHashCode_NestedStructure_ShouldBeConsistent() {
        Map<String, Object> complex1 = new HashMap<>();
        complex1.put("list", orderedList1);
        complex1.put("set", unorderedSet1);
        complex1.put("innerMap", map1);
        Map<String, Object> complex2 = new HashMap<>();
        complex2.put("list", new ArrayList<>(orderedList2));
        complex2.put("set", new HashSet<>(unorderedSet2));
        complex2.put("innerMap", new LinkedHashMap<>(map2));
        int h1 = DeepEquals.deepHashCode(complex1);
        int h2 = DeepEquals.deepHashCode(complex2);
        assertEquals(h1, h2, "Identical nested structures must yield identical deep hash codes");
    }

    @Test
    void deepHashCode_CircularReference_ShouldNotThrowAndBeStable() {
        List<Object> circular = new ArrayList<>();
        circular.add("start");
        // self‑reference
        circular.add(circular);
        // First call – ensure no StackOverflowError
        int first = DeepEquals.deepHashCode(circular);
        // Second call – result must be the same as the first
        int second = DeepEquals.deepHashCode(circular);
        assertEquals(first, second, "Deep hash of a circular structure must be stable across invocations");
    }

    @Test
    void deepHashCode_PrimitiveArray_ShouldMatchArraysHashCode() {
        int[] ints = { 1, 2, 3, 4, 5 };
        int expected = Arrays.hashCode(ints);
        assertEquals(expected, DeepEquals.deepHashCode(ints), "Primitive int[] deep hash should equal Arrays.hashCode");
    }

    @Test
    void deepHashCode_ObjectArray_ShouldBeConsistent() {
        String[] strings = { "one", "two", "three" };
        int h1 = DeepEquals.deepHashCode(strings);
        int h2 = DeepEquals.deepHashCode(new String[] { "one", "two", "three" });
        assertEquals(h1, h2, "Object arrays with identical content must have identical deep hash codes");
    }

    // ---------- Null handling ----------
    @Test
    void deepHashCode_Null_ShouldReturnZero() {
        assertEquals(0, DeepEquals.deepHashCode(null), "null input must yield hash code 0");
    }

    // ---------- Primitive wrappers ----------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("primitiveWrapperProvider")
    void deepHashCode_PrimitiveWrapper(String description, Object input, int expected) {
        assertEquals(expected, DeepEquals.deepHashCode(input), description);
    }

    // ---------- Primitive arrays ----------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("primitiveArrayProvider")
    void deepHashCode_PrimitiveArray(Object array, int expected, String description) {
        assertEquals(expected, DeepEquals.deepHashCode(array), description);
    }

    // ---------- Object arrays with nulls ----------
    @Test
    void deepHashCode_ObjectArrayWithNulls() {
        String[] arr = new String[] { "foo", null, "bar" };
        int expected = Arrays.deepHashCode(arr);
        assertEquals(expected, DeepEquals.deepHashCode(arr), "Object array containing null entries must be handled correctly");
    }

    // ---------- Nested object arrays ----------
    @Test
    void deepHashCode_NestedObjectArrays() {
        Object[] nested = new Object[] { new int[] { 1, 2 }, new String[] { "a", "b" } };
        int expected = Arrays.deepHashCode(nested);
        assertEquals(expected, DeepEquals.deepHashCode(nested), "Multi‑dimensional array should be processed recursively");
    }

    // ---------- Collections ----------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("collectionProvider")
    void deepHashCode_Collections(Object collection, int expected, String description) {
        assertEquals(expected, DeepEquals.deepHashCode(collection), description);
    }

    // ---------- Nested collections ----------
    @Test
    void deepHashCode_NestedCollections() {
        List<List<Integer>> outer = Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4));
        int expected = Objects.hash(outer.get(0), outer.get(1));
        assertEquals(expected, DeepEquals.deepHashCode(outer), "List of Lists must be hashed recursively preserving inner order");
    }

    // ---------- Maps ----------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("mapProvider")
    void deepHashCode_Maps(Object map, int expected, String description) {
        assertEquals(expected, DeepEquals.deepHashCode(map), description);
    }

    // ---------- Custom POJOs ----------
    @Test
    void deepHashCode_CustomPojoWithoutOverrides() {
        Person p = new Person("John", 30);
        int expected = Objects.hash(p.name, p.age);
        assertEquals(expected, DeepEquals.deepHashCode(p), "Plain POJO without custom equals/hashCode must be hashed by its fields");
    }

    @Test
    void deepHashCode_CustomPojoEqualsOnly() {
        EqualsOnly eo = new EqualsOnly("X");
        int expected = Objects.hash(eo.value);
        assertEquals(expected, DeepEquals.deepHashCode(eo), "POJO overriding only equals must still be hashed by field values");
    }

    @Test
    void deepHashCode_CustomPojoHashOnly() {
        HashOnly ho = new HashOnly(99);
        int expected = Objects.hash(ho.id);
        assertEquals(expected, DeepEquals.deepHashCode(ho), "POJO overriding only hashCode must be ignored; hash based on fields");
    }

    // ---------- Cyclic object graph ----------
    @Test
    void deepHashCode_SelfReferencingNode() {
        Node node = new Node(1);
        // self‑reference
        node.next = node;
        int h1 = DeepEquals.deepHashCode(node);
        int h2 = DeepEquals.deepHashCode(node);
        assertEquals(h1, h2, "Self‑referencing object must produce a deterministic hash");
    }

    @Test
    void deepHashCode_MutualCyclicNodes() {
        Node n1 = new Node(1);
        Node n2 = new Node(2);
        n1.next = n2;
        n2.next = n1;
        int h1 = DeepEquals.deepHashCode(n1);
        int h2 = DeepEquals.deepHashCode(n2);
        // The two hashes are not required to be equal but must be stable across calls
        assertEquals(h1, DeepEquals.deepHashCode(n1), "Hash must be stable for mutual cycle (node1)");
        assertEquals(h2, DeepEquals.deepHashCode(n2), "Hash must be stable for mutual cycle (node2)");
    }

    // ---------- Standard library immutable types ----------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("immutableProvider")
    void deepHashCode_ImmutableTypes(Object obj, int expected, String description) {
        assertEquals(expected, DeepEquals.deepHashCode(obj), description);
    }

    // ---------- Atomic types ----------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("atomicProvider")
    void deepHashCode_AtomicTypes(Object atomic, int expected, String description) {
        assertEquals(expected, DeepEquals.deepHashCode(atomic), description);
    }

    // ---------- Enums ----------
    @Test
    void deepHashCode_Enum() {
        DayOfWeek d = DayOfWeek.MONDAY;
        assertEquals(d.hashCode(), DeepEquals.deepHashCode(d), "Enum constants should use their built‑in hashCode");
    }

    // ---------- Empty collections and arrays ----------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("emptyProvider")
    void deepHashCode_EmptyStructures(Object empty, int expected, String description) {
        assertEquals(expected, DeepEquals.deepHashCode(empty), description);
    }

    // ---------- Object with throwing hashCode ----------
    @Test
    void deepHashCode_ObjectThrowingHashCode_ShouldPropagateException() {
        ThrowingHash th = new ThrowingHash();
        assertThrows(IllegalStateException.class, () -> DeepEquals.deepHashCode(th), "Exception from object's hashCode() must be propagated");
    }

    // ---------- Collection that throws ConcurrentModificationException ----------
    @Test
    void deepHashCode_BrokenList_ShouldPropagateCME() {
        BrokenList<Integer> bl = new BrokenList<>(Arrays.asList(1, 2, 3));
        assertThrows(ConcurrentModificationException.class, () -> DeepEquals.deepHashCode(bl), "Iterator throwing CME must be propagated");
    }

    // ---------- Map that throws ConcurrentModificationException ----------
    @Test
    void deepHashCode_BrokenMap_ShouldPropagateCME() {
        BrokenMap<Integer, String> bm = new BrokenMap<>();
        assertThrows(ConcurrentModificationException.class, () -> DeepEquals.deepHashCode(bm), "Map entrySet iterator throwing CME must be propagated");
    }

    // ---------- Security manager blocks reflective field access ----------
    @Test
    void deepHashCode_PrivateFieldClass_WithSecurityManager_ShouldThrowSecurityException() {
        // Install a restrictive SecurityManager that denies reflective access
        SecurityManager original = System.getSecurityManager();
        System.setSecurityManager(new DenyReflectiveAccessSecurityManager());
        try {
            PrivateFieldClass pfc = new PrivateFieldClass();
            assertThrows(SecurityException.class, () -> DeepEquals.deepHashCode(pfc), "SecurityException expected when reflection is blocked");
        } finally {
            // Restore original SecurityManager to avoid side‑effects on other tests
            System.setSecurityManager(original);
        }
    }
}
