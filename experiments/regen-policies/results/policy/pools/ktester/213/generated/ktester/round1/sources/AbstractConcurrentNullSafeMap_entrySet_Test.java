package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_entrySet_Test {

    private TestMap map;

    /**
     * Concrete implementation used only for testing.
     */
    private static final class TestMap extends AbstractConcurrentNullSafeMap<String, String> {

        TestMap(ConcurrentMap<Object, Object> internal) {
            super(internal);
        }

        // expose the protected mask/unmask methods only for test verification (optional)
        Object maskKey(String k) {
            return maskNullKey(k);
        }

        Object maskValue(String v) {
            return maskNullValue(v);
        }

        String unmaskKey(Object k) {
            return unmaskNullKey(k);
        }

        String unmaskValue(Object v) {
            return unmaskNullValue(v);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use a fresh ConcurrentHashMap for each test to guarantee isolation.
        ConcurrentMap<Object, Object> internal = new ConcurrentHashMap<>();
        map = new TestMap(internal);
        // Populate with a mixture of normal, null‑key and null‑value entries.
        map.put("a", "1");
        map.put(null, "nullKey");
        map.put("b", null);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the map is cleared so that later tests start clean.
        map.clear();
    }

    /* -------------------------------------------------------------
     * Helper methods
     * ------------------------------------------------------------- */
    private void resetMap(Map<String, String> entries) {
        map.clear();
        entries.forEach(map::put);
    }

    private static Stream<Arguments> containsProvider() {
        return Stream.of(// non‑Map.Entry argument
        Arguments.of("someString", false), // equal entry
        Arguments.of(new AbstractMap.SimpleEntry<>("a", "1"), true), // same key, different value
        Arguments.of(new AbstractMap.SimpleEntry<>("a", "different"), false), // non‑existent key
        Arguments.of(new AbstractMap.SimpleEntry<>("c", "1"), false), // null‑key / null‑value entry (requires map to contain null→null)
        Arguments.of(new AbstractMap.SimpleEntry<>(null, null), true));
    }

    private static Stream<Arguments> removeProvider() {
        return Stream.of(// non‑Map.Entry argument
        Arguments.of(12345, false), // remove entry with null value
        Arguments.of(new AbstractMap.SimpleEntry<>("b", null), true), // same key, wrong value
        Arguments.of(new AbstractMap.SimpleEntry<>("a", "wrong"), false), // non‑existent key
        Arguments.of(new AbstractMap.SimpleEntry<>("nonexistent", "1"), false));
    }

    @Test
    void entrySetSizeReflectsUnderlyingMap() {
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        assertEquals(3, entrySet.size(), "entrySet size should match number of entries");
    }

    @Test
    void iteratorReturnsUnmaskedEntries() {
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        Iterator<Map.Entry<String, String>> it = entrySet.iterator();
        // Collect entries for easier assertions.
        while (it.hasNext()) {
            Map.Entry<String, String> e = it.next();
            if (e.getKey() == null) {
                assertEquals("nullKey", e.getValue(), "null key should map to its stored value");
            } else if ("b".equals(e.getKey())) {
                assertNull(e.getValue(), "key 'b' should have a null value");
            } else if ("a".equals(e.getKey())) {
                assertEquals("1", e.getValue(), "key 'a' should map to '1'");
            } else {
                fail("Unexpected entry: " + e);
            }
        }
    }

    @Test
    void entrySetContainsWorksWithNulls() {
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        // Build entries matching the map's current state.
        Map.Entry<String, String> entryA = Map.entry("a", "1");
        Map.Entry<String, String> entryNullKey = Map.entry(null, "nullKey");
        Map.Entry<String, String> entryB = Map.entry("b", null);
        assertTrue(entrySet.contains(entryA), "entrySet should contain ('a','1')");
        assertTrue(entrySet.contains(entryNullKey), "entrySet should contain (null,'nullKey')");
        assertTrue(entrySet.contains(entryB), "entrySet should contain ('b',null)");
        assertFalse(entrySet.contains(Map.entry("c", "3")), "entrySet should not contain unrelated entry");
    }

    @Test
    void entrySetRemoveDeletesFromUnderlyingMap() {
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        Map.Entry<String, String> toRemove = Map.entry("a", "1");
        assertTrue(entrySet.remove(toRemove), "remove should succeed for existing entry");
        assertFalse(map.containsKey("a"), "underlying map should no longer contain key 'a'");
        assertEquals(2, entrySet.size(), "size should decrease after removal");
    }

    @Test
    void entrySetClearEmptiesUnderlyingMap() {
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        entrySet.clear();
        assertTrue(map.isEmpty(), "clear on entrySet should empty the map");
        assertEquals(0, entrySet.size(), "entrySet size should be zero after clear");
    }

    @Test
    void entrySetEntrySetValueUpdatesUnderlyingMap() {
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        Map.Entry<String, String> target = null;
        for (Map.Entry<String, String> e : entrySet) {
            if ("a".equals(e.getKey())) {
                target = e;
                break;
            }
        }
        assertNotNull(target, "Entry with key 'a' must be present");
        String old = target.setValue("newValue");
        assertEquals("1", old, "setValue should return the previous value");
        assertEquals("newValue", map.get("a"), "underlying map must reflect the new value");
    }

    @Test
    void entrySetIteratorRemoveWorks() {
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        Iterator<Map.Entry<String, String>> it = entrySet.iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> e = it.next();
            if ("b".equals(e.getKey())) {
                // remove entry with null value
                it.remove();
                break;
            }
        }
        assertFalse(map.containsKey("b"), "Key 'b' should have been removed via iterator");
        assertEquals(2, entrySet.size(), "Size should reflect removal");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: contains({0}) => {1}")
    @MethodSource("containsProvider")
    void testContains(Object input, boolean expected) {
        // Adjust map for the null‑null case
        if (input instanceof Map.Entry<?, ?> entry && entry.getKey() == null && entry.getValue() == null) {
            resetMap(Map.of(null, null, "a", "1", "b", null));
        }
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        assertEquals(expected, entrySet.contains(input));
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: remove({0}) => {1}")
    @MethodSource("removeProvider")
    void testRemove(Object input, boolean expected) {
        // Ensure map is in its default state for most cases
        if (input instanceof Map.Entry<?, ?> entry && entry.getKey() instanceof Integer) {
            // class‑cast test handled separately
        }
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        boolean result = entrySet.remove(input);
        assertEquals(expected, result);
    }

    // -----------------------------------------------------------------
    @Test
    void testIteratorContents() {
        // Prepare map with the exact entries expected by the JSON spec
        resetMap(Map.of("a", "1", null, null, "b", null));
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        Iterator<Map.Entry<String, String>> it = entrySet.iterator();
        List<Map.Entry<String, String>> collected = new ArrayList<>();
        while (it.hasNext()) {
            collected.add(it.next());
        }
        // Build the expected list (order independent)
        List<Map.Entry<String, String>> expected = List.of(Map.entry("a", "1"), Map.entry(null, null), Map.entry("b", null));
        assertTrue(collected.containsAll(expected) && expected.containsAll(collected), "Iterator must return exactly the expected entries");
    }

    // -----------------------------------------------------------------
    @Test
    void testEntryEqualsAndHashCode() {
        // Obtain an entry from the map to test its equals/hashCode implementation
        // any entry
        Map.Entry<String, String> mapEntry = map.entrySet().iterator().next();
        Map.Entry<String, String> same = new AbstractMap.SimpleEntry<>(mapEntry.getKey(), mapEntry.getValue());
        // Equality
        assertTrue(mapEntry.equals(same), "Entries with identical key/value must be equal");
        assertTrue(same.equals(mapEntry), "Symmetry of equals");
        // Different key
        Map.Entry<String, String> diffKey = new AbstractMap.SimpleEntry<>("different", mapEntry.getValue());
        assertFalse(mapEntry.equals(diffKey));
        // Different value
        Map.Entry<String, String> diffVal = new AbstractMap.SimpleEntry<>(mapEntry.getKey(), "different");
        assertFalse(mapEntry.equals(diffVal));
        // Non‑Entry comparison
        assertFalse(mapEntry.equals("just a string"));
        // Null values equality
        Map.Entry<String, String> nullVal1 = new AbstractMap.SimpleEntry<>("k", null);
        Map.Entry<String, String> nullVal2 = new AbstractMap.SimpleEntry<>("k", null);
        assertTrue(nullVal1.equals(nullVal2));
        assertEquals(nullVal1.hashCode(), nullVal2.hashCode(), "Equal entries must have identical hashCodes");
        // toString format
        Map.Entry<String, String> sample = new AbstractMap.SimpleEntry<>("alpha", "beta");
        assertEquals("alpha=beta", sample.toString());
    }

    // -----------------------------------------------------------------
    @Test
    void testSetValueReturnsOldAndUpdatesMap() {
        // entry "a" -> "1"
        Map.Entry<String, String> e = map.entrySet().stream().filter(entry -> "a".equals(entry.getKey())).findFirst().orElseThrow();
        String old = e.setValue("newValue");
        assertEquals("1", old);
        assertEquals("newValue", map.get("a"));
    }

    @Test
    void testSetValueOnNullKeyEntry() {
        // Ensure map has null->null mapping
        resetMap(Map.of(null, null, "a", "1"));
        Map.Entry<String, String> nullEntry = map.entrySet().stream().filter(entry -> entry.getKey() == null).findFirst().orElseThrow();
        String old = nullEntry.setValue("x");
        assertNull(old);
        assertEquals("x", map.get(null));
    }

    // -----------------------------------------------------------------
    @Test
    void testSizeReportsCorrectCount() {
        assertEquals(3, map.entrySet().size(), "Pre‑populated map should report size 3");
    }

    @Test
    void testClearEmptiesMapAndEntrySet() {
        map.entrySet().clear();
        assertEquals(0, map.entrySet().size());
        assertTrue(map.isEmpty());
    }

    // -----------------------------------------------------------------
    @Test
    void testIteratorRemoveSingleEntry() {
        // Map with two entries
        resetMap(Map.of("a", "1", "b", "2"));
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        Iterator<Map.Entry<String, String>> it = entrySet.iterator();
        // Remove first entry
        assertTrue(it.hasNext());
        // first entry
        it.next();
        it.remove();
        assertEquals(1, entrySet.size());
        assertFalse(map.containsKey("a"));
        assertTrue(map.containsKey("b"));
    }

    @Test
    void testIteratorRemoveWithoutNextThrows() {
        Iterator<Map.Entry<String, String>> it = map.entrySet().iterator();
        assertThrows(IllegalStateException.class, it::remove, "Calling remove() before next() must throw IllegalStateException");
    }

    // -----------------------------------------------------------------
    @Test
    void testEntrySetReflectsSubsequentPuts() {
        TestMap emptyMap = new TestMap(new ConcurrentHashMap<>());
        emptyMap.put("k", "v");
        Set<Map.Entry<String, String>> entrySet = emptyMap.entrySet();
        assertTrue(entrySet.contains(Map.entry("k", "v")), "Live view must reflect entries added after entrySet creation");
    }

    @Test
    void testEntrySetRemoveUpdatesUnderlyingMap() {
        // Map already contains ("a","1")
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        assertTrue(entrySet.remove(Map.entry("a", "1")));
        assertFalse(map.containsKey("a"));
    }

    // -----------------------------------------------------------------
    @Test
    void testContainsWithCustomEntryImplementation() {
        // custom entry that equals a map entry ("a","1")
        Map.Entry<String, String> custom = new Map.Entry<>() {

            @Override
            public String getKey() {
                return "a";
            }

            @Override
            public String getValue() {
                return "1";
            }

            @Override
            public String setValue(String v) {
                return null;
            }

            @Override
            public boolean equals(Object o) {
                return o instanceof Map.Entry && Objects.equals(getKey(), ((Map.Entry<?, ?>) o).getKey()) && Objects.equals(getValue(), ((Map.Entry<?, ?>) o).getValue());
            }

            @Override
            public int hashCode() {
                return Objects.hashCode(getKey()) ^ Objects.hashCode(getValue());
            }
        };
        assertTrue(map.entrySet().contains(custom));
    }

    @Test
    void testContainsCustomEntryWithDifferentValue() {
        Map.Entry<String, String> custom = new Map.Entry<>() {

            @Override
            public String getKey() {
                return "a";
            }

            @Override
            public String getValue() {
                return "different";
            }

            @Override
            public String setValue(String v) {
                return null;
            }

            @Override
            public boolean equals(Object o) {
                return o instanceof Map.Entry && Objects.equals(getKey(), ((Map.Entry<?, ?>) o).getKey()) && Objects.equals(getValue(), ((Map.Entry<?, ?>) o).getValue());
            }

            @Override
            public int hashCode() {
                return Objects.hashCode(getKey()) ^ Objects.hashCode(getValue());
            }
        };
        assertFalse(map.entrySet().contains(custom));
    }

    // -----------------------------------------------------------------
    @Test
    void testIteratorNextOnEmptyMapThrows() {
        TestMap empty = new TestMap(new ConcurrentHashMap<>());
        Iterator<Map.Entry<String, String>> it = empty.entrySet().iterator();
        assertThrows(NoSuchElementException.class, it::next, "Calling next() on an empty iterator must throw NoSuchElementException");
    }

    // -----------------------------------------------------------------
    @Test
    void testIteratorRemoveUnsupportedExceptionPropagation() {
        // ConcurrentHashMap's iterator does not support remove()
        Iterator<Map.Entry<String, String>> it = map.entrySet().iterator();
        assertTrue(it.hasNext());
        // advance to first element
        it.next();
        assertThrows(UnsupportedOperationException.class, it::remove, "Iterator.remove() on ConcurrentHashMap must throw UnsupportedOperationException");
    }

    // -----------------------------------------------------------------
    @Test
    void testContainsClassCastException() {
        Map.Entry<Integer, String> wrongKeyEntry = new AbstractMap.SimpleEntry<>(123, "value");
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        assertThrows(ClassCastException.class, () -> entrySet.contains(wrongKeyEntry), "contains() with wrong key type must throw ClassCastException");
    }

    // -----------------------------------------------------------------
    @Test
    void testRemoveClassCastException() {
        Map.Entry<String, Integer> wrongValueEntry = new AbstractMap.SimpleEntry<>("validKey", 456);
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        assertThrows(ClassCastException.class, () -> entrySet.remove(wrongValueEntry), "remove() with wrong value type must throw ClassCastException");
    }
}
