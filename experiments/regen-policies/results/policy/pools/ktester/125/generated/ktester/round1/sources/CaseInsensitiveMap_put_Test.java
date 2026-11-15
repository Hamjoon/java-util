package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveMap_put_Test {

    /**
     * Simple POJO used for custom‑object‑key tests.
     */
    private static final class Foo {

        private final int id;

        Foo(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof Foo) && ((Foo) o).id == this.id;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(id);
        }

        @Override
        public String toString() {
            return "Foo{id=" + id + '}';
        }
    }

    /**
     * The default cache length for {@link CaseInsensitiveMap} is 100.
     * We reset it before the whole suite runs to guarantee a clean state.
     */
    @BeforeAll
    static void setupBeforeAll() {
        // Ensure a deterministic cache length for all tests.
        CaseInsensitiveMap.setMaxCacheLengthString(100);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the cache length is restored after each test in case a test changes it.
        CaseInsensitiveMap.setMaxCacheLengthString(100);
    }

    private static List<Arguments> stringKeyBranchProvider() {
        return List.of(// first insertion
        Arguments.of("Hello", 1, null), // update with different case
        Arguments.of("HELLO", 2, 1), // another update
        Arguments.of("HeLLo", 3, 2));
    }

    private static List<Arguments> nonStringKeyBranchProvider() {
        return List.of(// first insertion
        Arguments.of(42, "Answer", null), // update same integer key
        Arguments.of(42, "Updated", "Answer"));
    }

    private static List<Arguments> nullKeyBranchProvider() {
        return List.of(Arguments.of(null, "nullValue", null), Arguments.of(null, "newNull", "nullValue"));
    }

    /* --------------------------------------------------------------------- *
     *  Basic case‑insensitive behavior for String keys
     * --------------------------------------------------------------------- */
    @Test
    void put_StringKey_IsCaseInsensitive() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        // First insertion
        String previous = map.put("KeyOne", "First");
        assertNull(previous, "First insertion should return null");
        assertEquals("First", map.get("keyone"));
        assertEquals(1, map.size());
        // Second insertion with a different case – should replace the first value
        previous = map.put("kEyOnE", "Second");
        assertEquals("First", previous, "Second insertion should return the previous value");
        assertEquals("Second", map.get("KEYONE"));
        assertEquals(1, map.size(), "Map size must stay 1 because the key is considered equal");
    }

    /* --------------------------------------------------------------------- *
     *  Non‑String keys must be handled exactly like a normal map
     * --------------------------------------------------------------------- */
    @Test
    void put_NonStringKey_BehavesLikeRegularMap() {
        CaseInsensitiveMap<Object, Integer> map = new CaseInsensitiveMap<>();
        Integer previous = map.put(42, 100);
        assertNull(previous);
        assertEquals(100, map.get(42));
        // Different key of same type – should not collide with the integer key
        previous = map.put(43, 200);
        assertNull(previous);
        assertEquals(200, map.get(43));
        // Verify that a String key does not interfere with integer keys
        map.put("Answer", 42);
        assertEquals(42, map.get("answer"));
        assertEquals(100, map.get(42));
        assertEquals(200, map.get(43));
        assertEquals(3, map.size());
    }

    /* --------------------------------------------------------------------- *
     *  Null keys are allowed (they are not instances of String)
     * --------------------------------------------------------------------- */
    @Test
    void put_NullKey_IsStoredAndRetrievable() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        String previous = map.put(null, "nullValue");
        assertNull(previous);
        assertEquals("nullValue", map.get(null));
        // Adding another entry must not affect the null entry
        map.put("someKey", "someValue");
        assertEquals("nullValue", map.get(null));
        assertEquals("someValue", map.get("SOMEKEY"));
        assertEquals(2, map.size());
    }

    /* --------------------------------------------------------------------- *
     *  Construction from an existing map – ensure that the backing map
     *  respects case‑insensitivity for String keys.
     * --------------------------------------------------------------------- */
    @Test
    void put_WithSourceMap_CaseInsensitiveBehaviourPreserved() {
        Map<String, String> source = new LinkedHashMap<>();
        source.put("Alpha", "A");
        source.put("Beta", "B");
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>(source);
        // Existing entries must be reachable case‑insensitively
        assertEquals("A", map.get("alpha"));
        assertEquals("B", map.get("BETA"));
        // New puts must also be case‑insensitive
        map.put("ALPHA", "NewA");
        assertEquals("NewA", map.get("alpha"));
        assertEquals(2, map.size(), "Size must stay 2 because the key is considered equal");
    }

    /* --------------------------------------------------------------------- *
     *  Verify that put returns the previous value for both String and
     *  non‑String keys.
     * --------------------------------------------------------------------- */
    @Test
    void put_ReturnsPreviousValue_ForBothKeyTypes() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        // String key
        assertNull(map.put("Key", "First"));
        assertEquals("First", map.put("kEy", "Second"));
        assertEquals("Second", map.get("KEY"));
        // Non‑String key
        CaseInsensitiveMap<Object, Integer> map2 = new CaseInsensitiveMap<>();
        assertNull(map2.put(1, 10));
        assertEquals(Integer.valueOf(10), map2.put(1, 20));
        assertEquals(Integer.valueOf(20), map2.get(1));
    }

    /**
     * Replace the private {@code map} field of a {@link CaseInsensitiveMap}
     * with a user‑supplied backing map. This is required for tests that need a
     * specific implementation (e.g. {@link ConcurrentHashMap}) which the
     * public API does not expose directly.
     */
    @SuppressWarnings("unchecked")
    private static <K, V> void setBackingMap(CaseInsensitiveMap<K, V> ciMap, Map<K, V> backing) {
        try {
            Field f = CaseInsensitiveMap.class.getDeclaredField("map");
            f.setAccessible(true);
            f.set(ciMap, backing);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    // ---------- Empty string key ----------
    @Test
    void put_EmptyStringKey_BehavesCaseInsensitive() {
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
        Integer prev = map.put("", 10);
        assertNull(prev);
        assertEquals(10, map.get(""));
        assertEquals(1, map.size());
        prev = map.put("", 20);
        assertEquals(10, prev);
        assertEquals(20, map.get(""));
        assertEquals(1, map.size());
    }

    // ---------- Null value handling ----------
    @Test
    void put_NullValue_WithStringKey() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        // first insertion with null value
        assertNull(map.put("Test", null));
        assertNull(map.get("test"));
        // update with non‑null value, previous value was null
        assertNull(map.put("test", "value"));
        assertEquals("value", map.get("TEST"));
    }

    @Test
    void put_NullValue_WithNonStringKey() {
        CaseInsensitiveMap<Boolean, String> map = new CaseInsensitiveMap<>();
        // Boolean key, null value
        assertNull(map.put(true, null));
        assertNull(map.get(true));
        // update with non‑null value, previous was null
        assertNull(map.put(true, "notNull"));
        assertEquals("notNull", map.get(true));
    }

    // ---------- CharSequence non‑String key ----------
    @Test
    void put_StringBuilderKey_TreatedAsNonString() {
        CaseInsensitiveMap<Object, String> map = new CaseInsensitiveMap<>();
        StringBuilder sb1 = new StringBuilder("abc");
        assertNull(map.put(sb1, "val1"));
        assertEquals("val1", map.get(sb1));
        // Same instance – should replace value
        assertEquals("val1", map.put(sb1, "val2"));
        assertEquals("val2", map.get(sb1));
        // Different instance with same content – distinct key
        StringBuilder sb2 = new StringBuilder("abc");
        assertNull(map.put(sb2, "val3"));
        assertEquals("val3", map.get(sb2));
        assertEquals("val2", map.get(sb1));
        assertEquals(3, map.size());
    }

    // ---------- Same case String key repeat ----------
    @Test
    void put_SameCaseStringKey_ReplacesValue() {
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
        assertNull(map.put("SameKey", 100));
        assertEquals(100, map.put("SameKey", 200));
        assertEquals(200, map.get("samekey"));
        assertEquals(1, map.size());
    }

    // ---------- Non‑String custom object key ----------
    @Test
    void put_CustomObjectKey_EqualityBasedOnEqualsAndHashCode() {
        CaseInsensitiveMap<Foo, String> map = new CaseInsensitiveMap<>();
        Foo foo1 = new Foo(1);
        assertNull(map.put(foo1, "first"));
        assertEquals("first", map.get(new Foo(1)));
        // another instance with same logical identity
        Foo foo2 = new Foo(1);
        assertEquals("first", map.put(foo2, "second"));
        assertEquals("second", map.get(foo1));
        assertEquals(1, map.size());
    }

    // ---------- Backing map specific exception tests ----------
    @Test
    void put_NullKey_WithConcurrentHashMapBacking_ThrowsNPE() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        setBackingMap(map, new ConcurrentHashMap<>());
        assertThrows(NullPointerException.class, () -> map.put(null, "someValue"));
    }

    @Test
    void put_NullValue_WithConcurrentHashMapBacking_ThrowsNPE() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        setBackingMap(map, new ConcurrentHashMap<>());
        assertThrows(NullPointerException.class, () -> map.put("anyKey", null));
    }

    @Test
    void put_NonComparableKey_WithTreeMapBacking_ThrowsClassCastException() {
        CaseInsensitiveMap<Object, String> map = new CaseInsensitiveMap<>();
        setBackingMap(map, new TreeMap<>());
        // not Comparable
        Object key = new Object();
        assertThrows(ClassCastException.class, () -> map.put(key, "val"));
    }

    @Test
    void put_IncompatibleKeyType_WithTreeMapExpectingStrings_ThrowsClassCastException() {
        // Prepare a TreeMap that already contains a String key
        TreeMap<String, String> tree = new TreeMap<>();
        tree.put("alpha", "a");
        CaseInsensitiveMap<Object, String> map = new CaseInsensitiveMap<>(tree);
        // The backing map is now a TreeMap; inserting an Integer should clash
        assertThrows(ClassCastException.class, () -> map.put(42, "val"));
    }

    @Test
    void put_WithUnmodifiableMapBacking_ThrowsUnsupportedOperationException() {
        Map<String, String> unmod = Collections.unmodifiableMap(new HashMap<>());
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>(unmod);
        assertThrows(UnsupportedOperationException.class, () -> map.put("key", "value"));
    }

    @Test
    void put_WithEmptyMapBacking_ThrowsUnsupportedOperationException() {
        Map<String, String> empty = Collections.emptyMap();
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>(empty);
        assertThrows(UnsupportedOperationException.class, () -> map.put("k", "v"));
    }

    @Test
    void put_NullKey_WithConcurrentSkipListMapBacking_ThrowsNPE() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        setBackingMap(map, new ConcurrentSkipListMap<>());
        assertThrows(NullPointerException.class, () -> map.put(null, "val"));
    }

    @Test
    void put_NullValue_WithConcurrentSkipListMapBacking_ThrowsNPE() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        setBackingMap(map, new ConcurrentSkipListMap<>());
        assertThrows(NullPointerException.class, () -> map.put("k", null));
    }

    /**
     * Executes the three steps from the "String key branch" JSON group.
     * Demonstrates that case variations all map to the same logical entry.
     */
    @ParameterizedTest
    @MethodSource("stringKeyBranchProvider")
    void put_StringKeyBranch_VariousCases(String key, Integer value, Object expectedPrev) {
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
        // First call – map is empty, expectedPrev must be null
        Object prev = map.put(key, value);
        assertEquals(expectedPrev, prev);
    }

    // ---------- Parameterized test for Non‑String key branch ----------
    @ParameterizedTest
    @MethodSource("nonStringKeyBranchProvider")
    void put_NonStringKeyBranch_Various(Object key, String value, Object expectedPrev) {
        CaseInsensitiveMap<Object, String> map = new CaseInsensitiveMap<>();
        Object prev = map.put(key, value);
        assertEquals(expectedPrev, prev);
    }

    // ---------- Parameterized test for Null key branch ----------
    @ParameterizedTest
    @MethodSource("nullKeyBranchProvider")
    void put_NullKeyBranch_Various(Object key, String value, Object expectedPrev) {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        Object prev = map.put(key, value);
        assertEquals(expectedPrev, prev);
    }
}
