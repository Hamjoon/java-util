package com.cedarsoftware.util;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

// @ExtendWith(MockitoExtension.class)
class CompactMap_get_Test {

    private CompactMap<String, Integer> map;

    /**
     * Test‑specific CompactMap with a very small {@code compactSize()} to
     * trigger the different storage strategies quickly.
     */
    private static class TestCompactMap<K, V> extends CompactMap<K, V> {

        @Override
        protected int compactSize() {
            // 0‑1 entries → special, 2 entries → Object[], >2 → Map
            return 2;
        }

        @Override
        protected Map<K, V> getNewMap() {
            return new HashMap<>();
        }
    }

    /**
     * Case‑insensitive variant – {@code isCaseInsensitive()} returns true.
     */
    private static class CaseInsensitiveCompactMap<K, V> extends TestCompactMap<K, V> {

        @Override
        protected boolean isCaseInsensitive() {
            return true;
        }
    }

    /**
     * Map implementation that always throws a {@link RuntimeException}
     * on {@code get}.
     */
    private static class RuntimeExceptionMap<K, V> implements Map<K, V> {

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public boolean containsKey(Object key) {
            return false;
        }

        @Override
        public boolean containsValue(Object value) {
            return false;
        }

        @Override
        public V get(Object key) {
            throw new RuntimeException("forced");
        }

        @Override
        public V put(K key, V value) {
            return null;
        }

        @Override
        public V remove(Object key) {
            return null;
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> m) {
        }

        @Override
        public void clear() {
        }

        @Override
        public Set<K> keySet() {
            return Collections.emptySet();
        }

        @Override
        public Collection<V> values() {
            return Collections.emptyList();
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            return Collections.emptySet();
        }
    }

    /**
     * Simple custom key with proper {@code equals}/{@code hashCode}.
     */
    private static final class CustomKey {

        private final int id;

        CustomKey(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof CustomKey) && ((CustomKey) o).id == this.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public String toString() {
            return "CustomKey{id=" + id + '}';
        }
    }

    private static class MapOperation {

        final String action;

        final Object key;

        // null for remove
        final Object value;

        MapOperation(String action, Object key, Object value) {
            this.action = action;
            this.key = key;
            this.value = value;
        }

        @Override
        public String toString() {
            return "MapOperation{" + action + ", key=" + key + ", value=" + value + '}';
        }
    }

    /**
     * Simple holder for parameter arguments – mirrors JUnit's {@code Arguments}.
     */
    private static final class Arguments {

        private final Object[] args;

        private Arguments(Object... args) {
            this.args = args;
        }

        static Arguments of(Object... args) {
            return new Arguments(args);
        }

        Object[] get() {
            return args;
        }
    }

//     @BeforeEach
    void setupBeforeEach() {
        map = new TestCompactMap<>();
    }

//     @AfterEach
    void teardownAfterEach() {
        // Ensure each test starts with a clean map.
        map.clear();
    }

    private static Stream<Arguments> arrayStorageProvider() {
        return // null key case
        Stream.// null key case
        of(// null key case
        Arguments.of("alpha", "1"), // null key case
        Arguments.of("beta", "2"), Arguments.of("gamma", null), Arguments.of(null, "555"));
    }

    private static Stream<Arguments> mapStorageProvider() {
        return // key 42 does not exist → null (but we store only strings)
        Stream.// key 42 does not exist → null (but we store only strings)
        of(Arguments.of(42, "10"), Arguments.of(99, null), Arguments.of(null, "999"));
    }

    private static Stream<Arguments> emptyMapProvider() {
        return Stream.of(Arguments.of("anything"), Arguments.of((Object) null));
    }

    private static Stream<Arguments> singleEntryProvider() {
        return Stream.of(Arguments.of("soloKey", "777"), Arguments.of("otherKey", null), Arguments.of(null, "888"));
    }

    private static Stream<Arguments> transitionProvider() {
        return Stream.of(Arguments.of(List.of(new MapOperation("put", "k1", "v1"), new MapOperation("put", "k2", "v2")), "k1", "v1"), Arguments.of(List.of(new MapOperation("put", "k1", "v1"), new MapOperation("put", "k2", "v2")), "k2", "v2"), Arguments.of(List.of(new MapOperation("put", "k1", "v1"), new MapOperation("put", "k2", "v2"), new MapOperation("put", "k3", "v3")), "k3", "v3"), Arguments.of(// added null value for remove
        List.// added null value for remove
        of(// added null value for remove
        new MapOperation("put", "solo", "single"), new MapOperation("remove", "solo", null)), "solo", null));
    }

    private static Stream<Arguments> caseInsensitiveProvider() {
        return Stream.of(Arguments.of("CaseInsensitiveCompactMap", "alpha", "upper"), Arguments.of("CaseInsensitiveCompactMap", null, "nullKey"));
    }

    private static Stream<Arguments> nonStringKeyProvider() {
        return Stream.of(Arguments.of(123, "intValue", "intValue"), Arguments.of(123L, "longValue", "longValue"), Arguments.of(true, "boolValue", "boolValue"));
    }

    private static Stream<Arguments> customKeyProvider() {
        return Stream.of(Arguments.of(new CustomKey(1), "custom1"), Arguments.of(new CustomKey(2), null));
    }

    private static Stream<Arguments> removalProvider() {
        return Stream.of(Arguments.of("second", "222"), Arguments.of("first", null));
    }

    /* --------------------------------------------------------------------- */
    private static void setInternalVal(CompactMap<?, ?> map, Object value) throws Exception {
        Field f = CompactMap.class.getDeclaredField("val");
        f.setAccessible(true);
        f.set(map, value);
    }

    /* Helper assertion methods (so we do not need JUnit). */
    private static void assertEquals(Object expected, Object actual, String msg) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(msg + " – expected: " + expected + ", actual: " + actual);
        }
    }

    private static void assertNull(Object actual, String msg) {
        if (actual != null) {
            throw new AssertionError(msg + " – expected null but was: " + actual);
        }
    }

    private static void assertThrows(Class<? extends Throwable> type, Runnable r, String msg) {
        try {
            r.run();
            throw new AssertionError(msg + " – expected exception of type " + type.getName());
        } catch (Throwable t) {
            if (!type.isInstance(t)) {
                throw new AssertionError(msg + " – threw wrong exception type: " + t, t);
            }
        }
    }

    /**
     * Runs all tests.  Execute with {@code java -ea CompactMap_get_Test} to
     * enable assertions.
     */
//     public static void main(String[] args) throws Exception {
//         getFromEmptyMap_returnsNull();
//         getFromSingleEntry_returnsValue();
//         getFromObjectArrayRepresentation_returnsCorrectValues();
//         getFromDelegatedMapRepresentation_returnsCorrectValues();
//         get_ArrayStorage();
//         get_MapStorage();
//         get_EmptyMap();
//         get_SingleEntry();
//         get_TransitionBetweenModes();
//         get_CaseInsensitive();
//         get_NonStringKeys();
//         get_CustomKey();
//         get_AfterRemoval();
//         get_MalformedArray_OddLength_throwsIndexOutOfBounds();
//         get_MalformedArray_SingleKey_throwsIndexOutOfBounds();
//         get_ArrayWrongValueType_throwsClassCast();
//         get_ArrayWrongValueTypeWithObjectKey_throwsClassCast();
//         get_DelegatedMapThrowsRuntimeException_propagates();
//         get_FaultySingleEntry_throwsNullPointerException();
//         System.out.println("All CompactMap.get tests passed.");
//     }

    /**
     * Empty map must return {@code null} for any key.
     */
//     @Test
//     void getFromEmptyMap_returnsNull() {
//         CompactMap<String, Integer> map = new TestCompactMap<>();
//         assertNull(map.get("anyKey"), "Getting from an empty CompactMap should return null");
//     }

    /**
     * Single entry (size == 1) must return the stored value for its key.
     */
//     @Test
//     void getFromSingleEntry_returnsValue() {
//         CompactMap<String, Integer> map = new TestCompactMap<>();
//         map.put("one", 1);
//         assertEquals(1, map.get("one"), "Value for the only key should be returned");
//         assertNull(map.get("missing"), "Missing key should return null even when map has one entry");
//     }

    /**
     * With two entries the internal representation is an Object[]; both keys must be found.
     */
//     @Test
//     void getFromObjectArrayRepresentation_returnsCorrectValues() {
//         CompactMap<String, Integer> map = new TestCompactMap<>();
//         map.put("first", 10);
//         map.put("second", 20);
//         assertEquals(10, map.get("first"), "First key should resolve to its value");
//         assertEquals(20, map.get("second"), "Second key should resolve to its value");
//         assertNull(map.get("absent"), "Absent key should return null");
//     }

    /**
     * When the number of entries exceeds {@code compactSize()}, CompactMap delegates to a regular Map.
     */
//     @Test
//     void getFromDelegatedMapRepresentation_returnsCorrectValues() {
//         CompactMap<String, Integer> map = new TestCompactMap<>();
//         map.put("a", 100);
//         map.put("b", 200);
//         map.put("c", 300);
//         assertEquals(100, map.get("a"));
//         assertEquals(200, map.get("b"));
//         assertEquals(300, map.get("c"));
//         assertNull(map.get("nonexistent"));
//     }

    /* ---------- Array storage (val is Object[]) ---------- */
//     @ParameterizedTest(name = "{index}: get({0}) → {1}")
//     @MethodSource("arrayStorageProvider")
//     void get_ArrayStorage(String key, String expected) {
//         Stream<Arguments> source = Stream.of(Arguments.of("alpha", "1"), Arguments.of("beta", "2"), Arguments.of("gamma", null), Arguments.of(null, "555"));
//         for (Arguments a : (Iterable<Arguments>) source::iterator) {
//             String key = (String) a.get()[0];
//             String expected = (String) a.get()[1];
//             CompactMap<String, Integer> map = new TestCompactMap<>();
//             map.put("alpha", 1);
//             map.put("beta", 2);
//             // add null key/value for the dedicated case
//             map.put(null, 555);
//             Object result = map.get(key);
//             if (expected == null) {
//                 assertNull(result, "key=" + key);
//             } else {
//                 int expectedInt = Integer.parseInt(expected.replaceAll("[^0-9-]", ""));
//                 assertEquals(expectedInt, result, "key=" + key);
//             }
//         }
//     }

    /* ---------- Map storage (val is a Map) ---------- */
//     @ParameterizedTest(name = "{index}: get({0}) → {1}")
//     @MethodSource("mapStorageProvider")
//     void get_MapStorage(Object key, String expected) {
//         Stream<Arguments> source = Stream.of(Arguments.of(42, "10"), Arguments.of(99, null), Arguments.of(null, "999"));
//         for (Arguments a : (Iterable<Arguments>) source::iterator) {
//             Object key = a.get()[0];
//             String expected = (String) a.get()[1];
//             CompactMap<Object, Integer> map = new TestCompactMap<>();
//             map.put("x", 10);
//             map.put("y", 20);
//             map.put("z", 30);
//             map.put(null, 999);
//             Object result = map.get(key);
//             if (expected == null) {
//                 assertNull(result, "key=" + key);
//             } else {
//                 int expectedInt = Integer.parseInt(expected.replaceAll("[^0-9-]", ""));
//                 assertEquals(expectedInt, result, "key=" + key);
//             }
//         }
//     }

    /* ---------- Empty map ---------- */
//     @ParameterizedTest(name = "{index}: empty get({0}) → null")
//     @MethodSource("emptyMapProvider")
//     void get_EmptyMap(Object key) {
//         Stream<Arguments> source = Stream.of(Arguments.of("anything"), Arguments.of((Object) null));
//         for (Arguments a : (Iterable<Arguments>) source::iterator) {
//             Object key = a.get()[0];
//             CompactMap<String, Integer> map = new TestCompactMap<>();
//             assertNull(map.get(key), "Empty map get(" + key + ")");
//         }
//     }

    /* ---------- Single entry storage ---------- */
//     @ParameterizedTest(name = "{index}: single entry get({0}) → {1}")
//     @MethodSource("singleEntryProvider")
//     void get_SingleEntry(Object key, String expected) {
//         Stream<Arguments> source = Stream.of(Arguments.of("soloKey", "777"), Arguments.of("otherKey", null), Arguments.of(null, "888"));
//         for (Arguments a : (Iterable<Arguments>) source::iterator) {
//             Object key = a.get()[0];
//             String expected = (String) a.get()[1];
//             CompactMap<String, Integer> map = new TestCompactMap<>();
//             map.put("soloKey", 777);
//             // ensure only one entry (repeat puts are idempotent)
//             map.put("soloKey", 777);
//             map.put("soloKey", 777);
//             // now test the null‑key case
//             map.clear();
//             map.put(null, 888);
//             Object result = map.get(key);
//             if (expected == null) {
//                 assertNull(result, "key=" + key);
//             } else {
//                 int expectedInt = Integer.parseInt(expected.replaceAll("[^0-9-]", ""));
//                 assertEquals(expectedInt, result, "key=" + key);
//             }
//         }
//     }

    /* ---------- Transition between storage modes ---------- */
//     @ParameterizedTest(name = "{index}: ops={0}, get({1}) → {2}")
//     @MethodSource("transitionProvider")
//     void get_TransitionBetweenModes(List<MapOperation> ops, Object key, String expected) {
//         Stream<Arguments> source = Stream.of(Arguments.of(List.of(new MapOperation("put", "k1", "v1"), new MapOperation("put", "k2", "v2")), "k1", "v1"), Arguments.of(List.of(new MapOperation("put", "k1", "v1"), new MapOperation("put", "k2", "v2")), "k2", "v2"), Arguments.of(List.of(new MapOperation("put", "k1", "v1"), new MapOperation("put", "k2", "v2"), new MapOperation("put", "k3", "v3")), "k3", "v3"), Arguments.of(List.of(new MapOperation("put", "solo", "single"), new MapOperation("remove", "solo", null)), "solo", null));
//         for (Arguments a : (Iterable<Arguments>) source::iterator) {
//             @SuppressWarnings("unchecked")
//             List<MapOperation> ops = (List<MapOperation>) a.get()[0];
//             String key = (String) a.get()[1];
//             String expected = (String) a.get()[2];
//             CompactMap<String, String> map = new TestCompactMap<>();
//             for (MapOperation op : ops) {
//                 if ("put".equals(op.action)) {
//                     map.put((String) op.key, (String) op.value);
//                 } else if ("remove".equals(op.action)) {
//                     map.remove((String) op.key);
//                 }
//             }
//             Object result = map.get(key);
//             if (expected == null) {
//                 assertNull(result, "key=" + key);
//             } else {
//                 assertEquals(expected, result, "key=" + key);
//             }
//         }
//     }

    /* ---------- Case‑insensitive mode ---------- */
//     @ParameterizedTest(name = "{index}: CI get({0}) → {1}")
//     @MethodSource("caseInsensitiveProvider")
//     void get_CaseInsensitive(String mapClass, String key, String expected) throws Exception {
//         CompactMap<String, String> ciMap = new CaseInsensitiveCompactMap<>();
//         ciMap.put("Alpha", "upper");
//         ciMap.put(null, "nullKey");
//         assertEquals("upper", ciMap.get("alpha"));
//         assertEquals("nullKey", ciMap.get(null));
//     }

    /* ---------- Non‑String key types ---------- */
//     @ParameterizedTest(name = "{index}: key={0} ({1}) → {2}")
//     @MethodSource("nonStringKeyProvider")
//     void get_NonStringKeys(Object key, Object value, String expected) {
//         Stream<Arguments> source = Stream.of(Arguments.of(123, "intValue", "intValue"), Arguments.of(123L, "longValue", "longValue"), Arguments.of(true, "boolValue", "boolValue"));
//         for (Arguments a : (Iterable<Arguments>) source::iterator) {
//             Object key = a.get()[0];
//             Object value = a.get()[1];
//             String expected = (String) a.get()[2];
//             CompactMap<Object, String> anyMap = new TestCompactMap<>();
//             anyMap.put(key, (String) value);
//             Object result = anyMap.get(key);
//             assertEquals(expected, result);
//         }
//     }

    /* ---------- Custom key object ---------- */
//     @ParameterizedTest(name = "{index}: custom key get({0}) → {1}")
//     @MethodSource("customKeyProvider")
//     void get_CustomKey(Object key, String expected) {
//         Stream<Arguments> source = Stream.of(Arguments.of(new CustomKey(1), "custom1"), Arguments.of(new CustomKey(2), null));
//         for (Arguments a : (Iterable<Arguments>) source::iterator) {
//             Object key = a.get()[0];
//             String expected = (String) a.get()[1];
//             CompactMap<CustomKey, String> customMap = new TestCompactMap<>();
//             customMap.put(new CustomKey(1), "custom1");
//             Object result = customMap.get(key);
//             if (expected == null) {
//                 assertNull(result);
//             } else {
//                 assertEquals(expected, result);
//             }
//         }
//     }

    /* ---------- Key present after removal of another key ---------- */
//     @ParameterizedTest(name = "{index}: after removal get({0}) → {1}")
//     @MethodSource("removalProvider")
//     void get_AfterRemoval(String key, String expected) {
//         Stream<Arguments> source = Stream.of(Arguments.of("second", "222"), Arguments.of("first", null));
//         for (Arguments a : (Iterable<Arguments>) source::iterator) {
//             String key = (String) a.get()[0];
//             String expected = (String) a.get()[1];
//             CompactMap<String, Integer> map = new TestCompactMap<>();
//             map.put("first", 111);
//             map.put("second", 222);
//             map.remove("first");
//             Object result = map.get(key);
//             if (expected == null) {
//                 assertNull(result);
//             } else {
//                 assertEquals(expected, result);
//             }
//         }
//     }

    /**
     * Malformed Object[] (odd length) should cause {@link IndexOutOfBoundsException}.
     */
//     @Test
//     void get_MalformedArray_OddLength_throwsIndexOutOfBounds() throws Exception {
//         CompactMap<String, String> map = new TestCompactMap<>();
//         setInternalVal(map, new Object[] { "alpha", "value1", "beta" });
//         assertThrows(IndexOutOfBoundsException.class, () -> map.get("alpha"), "Odd‑length array should cause IndexOutOfBoundsException");
//     }

    /**
     * Malformed Object[] with only a key (no value) throws {@link IndexOutOfBoundsException}.
     */
//     @Test
//     void get_MalformedArray_SingleKey_throwsIndexOutOfBounds() throws Exception {
//         CompactMap<String, String> map = new TestCompactMap<>();
//         setInternalVal(map, new Object[] { "onlyKey" });
//         assertThrows(IndexOutOfBoundsException.class, () -> map.get("onlyKey"), "Single‑key array should cause IndexOutOfBoundsException");
//     }

    /**
     * Value of wrong type in Object[] leads to {@link ClassCastException}.
     */
//     @Test
//     void get_ArrayWrongValueType_throwsClassCast() throws Exception {
//         CompactMap<String, String> map = new TestCompactMap<>();
//         setInternalVal(map, new Object[] { "key", Integer.valueOf(42) });
//         assertThrows(ClassCastException.class, () -> map.get("key"), "Wrong value type should cause ClassCastException");
//     }

    /**
     * Value of wrong type where key comparison succeeds but value cast fails.
     */
//     @Test
//     void get_ArrayWrongValueTypeWithObjectKey_throwsClassCast() throws Exception {
//         CompactMap<Object, String> map = new TestCompactMap<>();
//         Object dummyKey = new Object();
//         setInternalVal(map, new Object[] { dummyKey, "value" });
//         assertThrows(ClassCastException.class, () -> map.get(dummyKey), "Wrong value type with object key should cause ClassCastException");
//     }

    /**
     * Delegated map that throws a runtime exception propagates it.
     */
//     @Test
//     void get_DelegatedMapThrowsRuntimeException_propagates() throws Exception {
//         CompactMap<String, String> map = new TestCompactMap<>();
//         setInternalVal(map, new RuntimeExceptionMap<>());
//         RuntimeException ex = assertThrows(RuntimeException.class, () -> map.get("anyKey"), "RuntimeException from delegated map must propagate");
//         assertEquals("forced", ex.getMessage());
//     }

    /**
     * Faulty single‑entry representation that causes {@link NullPointerException}.
     */
//     @Test
//     void get_FaultySingleEntry_throwsNullPointerException() throws Exception {
//         CompactMap<String, String> map = new TestCompactMap<>();
//         // not a proper internal representation
//         setInternalVal(map, new Object());
//         assertThrows(NullPointerException.class, () -> map.get("test"), "Faulty single‑entry representation should throw NPE");
//     }
}