package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.Iterator;

class CompactCIHashMap_getNewMap_Test {

    private CompactCIHashMap<String, Integer> ciHashMap;

    /**
     * Subclass that allows us to control the value returned by {@code compactSize()}.
     */
    private static class TestCompactCIHashMap extends CompactCIHashMap<String, Integer> {

        private final int overriddenSize;

        TestCompactCIHashMap(int overriddenSize) {
            this.overriddenSize = overriddenSize;
        }

        @Override
        protected int compactSize() {
            return overriddenSize;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh instance before each test.
        ciHashMap = new CompactCIHashMap<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and avoid accidental reuse.
        ciHashMap = null;
    }

    @Test
    void getNewMap_returnsCaseInsensitiveMapInstance() {
        Map<String, Integer> newMap = ciHashMap.getNewMap();
        // The concrete implementation should be CaseInsensitiveMap.
        assertTrue(newMap instanceof CaseInsensitiveMap, "getNewMap() should return an instance of CaseInsensitiveMap");
    }

    @Test
    void getNewMap_returnsEmptyMap() {
        Map<String, Integer> newMap = ciHashMap.getNewMap();
        assertTrue(newMap.isEmpty(), "The newly created map should be empty");
        assertEquals(0, newMap.size(), "Size of the new map must be zero");
    }

    @Test
    void getNewMap_isCaseInsensitive() {
        Map<String, Integer> newMap = ciHashMap.getNewMap();
        // Insert using mixed‑case key.
        newMap.put("TeStKeY", 42);
        // Retrieval should succeed regardless of key case.
        assertEquals(42, newMap.get("testkey"));
        assertEquals(42, newMap.get("TESTKEY"));
        assertEquals(42, newMap.get("TeStKeY"));
    }

    @Test
    void getNewMap_isCaseInsensitiveForStringKeys() {
        Map<String, Integer> newMap = ciHashMap.getNewMap();
        newMap.put("TeStKeY", 42);
        // Retrieval with different capitalisations must succeed.
        assertEquals(42, newMap.get("testkey"));
        assertEquals(42, newMap.get("TESTKEY"));
        assertEquals(42, newMap.get("TeStKeY"));
    }

    @Test
    void getNewMap_nonStringKeyIsNotCaseInsensitive() {
        // Use a raw map to allow non‑String keys (the generic type is String, but the
        // underlying CaseInsensitiveMap permits any Object as key).
        @SuppressWarnings("unchecked")
        Map rawMap = ciHashMap.getNewMap();
        rawMap.put(42, "answer");
        assertEquals("answer", rawMap.get(42));
        assertNull(rawMap.get("42"), "String \"42\" must not match the Integer key 42");
    }

    @Test
    void getNewMap_nullKeyIsRejected() {
        Map<String, Integer> newMap = ciHashMap.getNewMap();
        // The concrete CaseInsensitiveMap throws NullPointerException for null keys.
        assertThrows(NullPointerException.class, () -> newMap.put(null, 1), "Putting a null key should throw NullPointerException");
    }

    @Test
    void getNewMap_returnsDistinctInstances() {
        Map<String, Integer> map1 = ciHashMap.getNewMap();
        Map<String, Integer> map2 = ciHashMap.getNewMap();
        map1.put("A", 1);
        assertEquals(1, map1.size());
        assertTrue(map2.isEmpty(), "Second map must remain empty after mutating the first");
    }

    @Test
    void getNewMap_respectsClassPolicies() {
        Map<String, Integer> newMap = ciHashMap.getNewMap();
        // Verify case‑insensitivity (already covered, but kept for policy emphasis).
        newMap.put("Key", 10);
        assertEquals(10, newMap.get("key"));
        // Verify iterator behavior matches copy‑iterator policy.
        // CompactCIHashMap uses a copy iterator (useCopyIterator() == false),
        // meaning the iterator should operate on a snapshot.
        // The simplest observable effect is that modifications after iterator creation
        // do not affect the iterator's traversal.
        Iterator<Map.Entry<String, Integer>> iterator = newMap.entrySet().iterator();
        // modify after iterator creation
        newMap.put("Another", 20);
        // The iterator should only see the entry that existed at creation time.
        assertTrue(iterator.hasNext(), "Iterator should have at least one element");
        Map.Entry<String, Integer> entry = iterator.next();
        assertEquals("Key", entry.getKey());
        assertEquals(10, entry.getValue());
        assertFalse(iterator.hasNext(), "Iterator should not see the later added entry");
    }

    @Test
    void getNewMap_negativeCompactSizeThrows() {
        TestCompactCIHashMap map = new TestCompactCIHashMap(-5);
        // The HashMap constructor inside getNewMap() will receive -4 ( -5 + 1 )
        // and therefore throw IllegalArgumentException.
        assertThrows(IllegalArgumentException.class, map::getNewMap, "Negative initial capacity must cause IllegalArgumentException");
    }

    @Test
    void getNewMap_overflowCompactSizeThrows() {
        TestCompactCIHashMap map = new TestCompactCIHashMap(Integer.MAX_VALUE);
        // compactSize() + 1 overflows to Integer.MIN_VALUE, which is illegal.
        assertThrows(IllegalArgumentException.class, map::getNewMap, "Overflowed initial capacity must cause IllegalArgumentException");
    }

    @ParameterizedTest
    @ValueSource(strings = { "alpha", "BETA", "Gamma" })
    void getNewMap_iteratorDoesNotSeeLaterAdds(String key) {
        Map<String, Integer> map = ciHashMap.getNewMap();
        map.put(key, 1);
        Iterator<String> it = map.keySet().iterator();
        // Add a new entry after iterator creation.
        map.put("newKey", 2);
        // Iterator should only iterate over the original key.
        assertTrue(it.hasNext());
        assertEquals(key, it.next());
        assertFalse(it.hasNext());
    }
}
