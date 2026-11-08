package com.cedarsoftware.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.lang.reflect.Field;
// import org.junit.Test;
// import org.junit.runner.RunWith;
import org.junit.jupiter.api.Test;
// import org.junit.runners.JUnit4;
import org.junit.jupiter.api.extension.ExtendWith;
// import static org.junit.Assert.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
// @ExtendWith(MockitoExtension.class)
import static org.junit.jupiter.api.Assertions.*;

// keep the extension if Mockito is on the classpath
// @ExtendWith(MockitoExtension.class)
class CompactMap_get_Test {

    /**
     * Helper subclass that lets the test control the {@code compactSize},
     * the key used for the single‑value optimisation and the concrete backing
     * map type.  It also allows overriding case‑sensitivity.
     */
    private static class TestCompactMap<K, V> extends CompactMap<K, V> {

        private final int compactSize;

        private final K singleKey;

        private final Map<K, V> backingMap;

        private final boolean caseInsensitive;

        TestCompactMap(int compactSize, K singleKey, Map<K, V> backingMap) {
            this(compactSize, singleKey, backingMap, false);
        }

        TestCompactMap(int compactSize, K singleKey, Map<K, V> backingMap, boolean caseInsensitive) {
            this.compactSize = compactSize;
            this.singleKey = singleKey;
            this.backingMap = backingMap;
            this.caseInsensitive = caseInsensitive;
        }

        @Override
        protected int compactSize() {
            return compactSize;
        }

        @Override
        protected K getSingleValueKey() {
            return singleKey;
        }

        @Override
        protected Map<K, V> getNewMap() {
            return backingMap;
        }

        @Override
        protected boolean isCaseInsensitive() {
            return caseInsensitive;
        }
    }

    /* -------------------------------------------------------------
     *  Empty map – {@code get} must always return {@code null}.
     * ------------------------------------------------------------- */
    @Test
    void getFromEmptyMap_returnsNull() {
        CompactMap<String, Integer> map = new TestCompactMap<>(3, "id", new LinkedHashMap<>());
        assertTrue(map.isEmpty(), "Map should be empty initially");
        assertNull(map.get("anyKey"), "Getting any key from an empty map must return null");
    }

    /* -------------------------------------------------------------
     *  Single‑entry state – the internal representation is the special
     *  single‑value optimisation.
     * ------------------------------------------------------------- */
    @Test
    void getFromSingleEntryMap_returnsValueOrNull() {
        CompactMap<String, String> map = new TestCompactMap<>(3, "value", new LinkedHashMap<>());
        assertNull(map.put("value", "ONE"), "First put must return null");
        assertEquals("ONE", map.get("value"), "Should retrieve the stored value for the single key");
        assertNull(map.get("other"), "Non‑existent key must return null");
    }

    /* -------------------------------------------------------------
     *  Compact array state – when the size is between 2 and {@code compactSize}
     *  the map stores keys/values in an {@code Object[]} array.
     * ------------------------------------------------------------- */
    @Test
    void getFromCompactArray_returnsCorrectValues() {
        CompactMap<String, Integer> map = new TestCompactMap<>(5, "id", new LinkedHashMap<>());
        map.put("a", 10);
        map.put("b", 20);
        assertEquals(Integer.valueOf(10), map.get("a"));
        assertEquals(Integer.valueOf(20), map.get("b"));
        assertNull(map.get("c"), "Missing key must return null");
    }

    /* -------------------------------------------------------------
     *  Backing map state – once the number of entries exceeds {@code compactSize},
     *  the implementation switches to a regular {@link Map}.
     * ------------------------------------------------------------- */
    @Test
    void getFromBackingMap_returnsCorrectValues() {
        CompactMap<String, String> map = new TestCompactMap<>(2, "id", new LinkedHashMap<>());
        map.put("x", "X");
        map.put("y", "Y");
        map.put("z", "Z");
        assertEquals("X", map.get("x"));
        assertEquals("Y", map.get("y"));
        assertEquals("Z", map.get("z"));
        assertNull(map.get("missing"), "Missing key must return null");
    }

    /* -------------------------------------------------------------
     *  Verify that {@code get(null)} behaves like a normal {@link Map}.
     * ------------------------------------------------------------- */
    @Test
    void getWithNullKey_behaviourMatchesMapContract() {
        CompactMap<String, String> map = new TestCompactMap<>(3, "id", new LinkedHashMap<>());
        map.put(null, "NULL_VALUE");
        assertEquals("NULL_VALUE", map.get(null));
        map.remove(null);
        assertNull(map.get(null), "After removal, null key should yield null");
    }

    /* -------------------------------------------------------------
     *  Single‑entry map where the key is {@code null}.
     * ------------------------------------------------------------- */
    @Test
    void getFromSingleEntryMap_nullKey() {
        CompactMap<String, String> map = new TestCompactMap<>(3, "nullKey", new LinkedHashMap<>());
        assertNull(map.put(null, "NULL_VALUE"));
        assertEquals("NULL_VALUE", map.get(null));
        assertNull(map.get("any"), "Any non‑null key must return null");
    }

    /* -------------------------------------------------------------
     *  Compact array containing a {@code null} key.
     * ------------------------------------------------------------- */
    @Test
    void getFromCompactArray_nullKey() {
        CompactMap<String, String> map = new TestCompactMap<>(5, "id", new LinkedHashMap<>());
        map.put(null, "NULL_VAL");
        map.put("x", "X_VAL");
        assertEquals("NULL_VAL", map.get(null));
        assertEquals("X_VAL", map.get("x"));
        assertNull(map.get("missing"), "Missing key must return null");
    }

    /* -------------------------------------------------------------
     *  Backing map containing a {@code null} key.
     * ------------------------------------------------------------- */
    @Test
    void getFromBackingMap_nullKey() {
        CompactMap<String, String> map = new TestCompactMap<>(2, "id", new LinkedHashMap<>());
        map.put(null, "NULL_IN_MAP");
        map.put("a", "A");
        assertEquals("NULL_IN_MAP", map.get(null));
        assertEquals("A", map.get("a"));
        assertNull(map.get("b"), "Missing key must return null");
    }

    /* -------------------------------------------------------------
     *  Case‑insensitive map – key matching should ignore case when the
     *  option is enabled.
     * ------------------------------------------------------------- */
    @Test
    void getFromCaseInsensitiveMap_matchesIgnoringCase() {
        CompactMap<String, String> map = new TestCompactMap<>(5, "id", new LinkedHashMap<>(), true);
        map.put("test", "caseInsensitiveValue");
        assertEquals("caseInsensitiveValue", map.get("TeSt"));
        assertNull(map.get("OTHER"), "Non‑existent key must still return null");
    }

    /* -------------------------------------------------------------
     *  Invalid internal representation – odd‑length array should cause
     *  an exception.
     * ------------------------------------------------------------- */
    @Test
    void getWithOddLengthArray_throwsArrayIndexOutOfBoundsException() throws Exception {
        CompactMap<String, String> map = new TestCompactMap<>(5, "id", new LinkedHashMap<>());
        Field valField = CompactMap.class.getDeclaredField("val");
        valField.setAccessible(true);
        // Simulate a corrupted internal array: ["k1","v1","k2"] (odd length)
        valField.set(map, new Object[] { "k1", "v1", "k2" });
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> map.get("anyKey"));
    }

    /* -------------------------------------------------------------
     *  Parameterized tests – empty‑map scenarios.
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "Empty map get({0}) → null")
    @CsvSource({ "'anyKey'", "null" })
    void emptyMapParameterized(String key) {
        CompactMap<String, String> map = new TestCompactMap<>(3, "id", new LinkedHashMap<>());
        assertNull(map.get(key));
    }

    /* -------------------------------------------------------------
     *  Parameterized tests – single‑entry scenarios.
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "Single entry get({0}) → {1}")
    @CsvSource({ "'alpha', A", "'beta', null", "null, NULL_VALUE" })
    void singleEntryParameterized(String key, String expected) {
        CompactMap<String, String> map = new TestCompactMap<>(3, "alpha", new LinkedHashMap<>());
        // single entry
        map.put("alpha", "A");
        if ("NULL_VALUE".equals(expected)) {
            // third case – replace with a null‑key entry
            map.clear();
            map.put(null, "NULL_VALUE");
        }
        Object actual = map.get(key);
        if ("null".equals(expected)) {
            assertNull(actual);
        } else {
            assertEquals(expected, actual);
        }
    }

    /* -------------------------------------------------------------
     *  Parameterized tests – compact‑array scenarios.
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "Compact array get({0}) → {1}")
    @CsvSource({ "'one', 1", "'two', 2", "'missing', null", "null, NULL_VAL" })
    void compactArrayParameterized(String key, String expected) {
        CompactMap<String, String> map = new TestCompactMap<>(5, "id", new LinkedHashMap<>());
        map.put("one", "1");
        map.put("two", "2");
        map.put(null, "NULL_VAL");
        Object actual = map.get(key);
        if ("null".equals(expected)) {
            assertNull(actual);
        } else {
            assertEquals(expected, actual);
        }
    }

    /* -------------------------------------------------------------
     *  Parameterized tests – backing‑map scenarios.
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "Backing map get({0}) → {1}")
    @CsvSource({ "'x', X_VAL", "'y', null", "null, NULL_IN_MAP" })
    void backingMapParameterized(String key, String expected) {
        CompactMap<String, String> map = new TestCompactMap<>(2, "id", new LinkedHashMap<>());
        map.put("x", "X_VAL");
        map.put(null, "NULL_IN_MAP");
        // pushes into backing‑map state
        map.put("z", "Z_VAL");
        Object actual = map.get(key);
        if ("null".equals(expected)) {
            assertNull(actual);
        } else {
            assertEquals(expected, actual);
        }
    }
}