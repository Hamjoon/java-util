package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

@ExtendWith(MockitoExtension.class)
class CompactMap_clear_Test {

    private CompactMap<String, Integer> map;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh instance for each test
        map = new CompactMap<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the map reference is cleared after each test
        map = null;
    }

    @Test
    @DisplayName("clear() on an already empty map leaves it empty")
    void clearOnEmptyMap() {
        assertTrue(map.isEmpty(), "Map should be empty initially");
        assertEquals(0, map.size(), "Initial size must be 0");
        map.clear();
        assertTrue(map.isEmpty(), "Map should remain empty after clear()");
        assertEquals(0, map.size(), "Size must stay 0 after clear()");
        assertNull(map.get("anyKey"), "Getting any key must return null");
        assertTrue(map.keySet().isEmpty(), "keySet must be empty");
        assertTrue(map.values().isEmpty(), "values collection must be empty");
    }

    @Test
    @DisplayName("clear() removes a single entry")
    void clearAfterSingleEntry() {
        map.put("one", 1);
        assertFalse(map.isEmpty(), "Map should contain one entry");
        assertEquals(1, map.size(), "Size must be 1 before clear()");
        assertEquals(1, map.get("one"));
        map.clear();
        assertTrue(map.isEmpty(), "Map must be empty after clear()");
        assertEquals(0, map.size(), "Size must be 0 after clear()");
        assertNull(map.get("one"), "Previously stored key must return null");
        assertTrue(map.keySet().isEmpty(), "keySet must be empty after clear()");
        assertTrue(map.values().isEmpty(), "values collection must be empty after clear()");
    }

    @Test
    @DisplayName("clear() removes multiple entries")
    void clearAfterMultipleEntries() {
        map.put("a", 10);
        map.put("b", 20);
        map.put("c", 30);
        assertEquals(3, map.size(), "Map should have three entries before clear()");
        assertFalse(map.isEmpty());
        map.clear();
        assertTrue(map.isEmpty(), "Map must be empty after clear()");
        assertEquals(0, map.size(), "Size must be 0 after clear()");
        assertNull(map.get("a"));
        assertNull(map.get("b"));
        assertNull(map.get("c"));
        assertTrue(map.keySet().isEmpty(), "keySet must be empty after clear()");
        assertTrue(map.values().isEmpty(), "values collection must be empty after clear()");
    }

    @Test
    @DisplayName("keySet().clear() delegates to CompactMap.clear()")
    void keySetClearDelegatesToMapClear() {
        map.put("x", 100);
        map.put("y", 200);
        assertEquals(2, map.size());
        // The clear() of the keySet view should clear the underlying map
        map.keySet().clear();
        assertTrue(map.isEmpty(), "Map must be empty after keySet().clear()");
        assertEquals(0, map.size());
        assertTrue(map.keySet().isEmpty(), "keySet must be empty after its own clear()");
    }

    @Test
    @DisplayName("values().clear() delegates to CompactMap.clear()")
    void valuesClearDelegatesToMapClear() {
        map.put("alpha", 1);
        map.put("beta", 2);
        assertEquals(2, map.size());
        // The clear() of the values view should clear the underlying map
        map.values().clear();
        assertTrue(map.isEmpty(), "Map must be empty after values().clear()");
        assertEquals(0, map.size());
        assertTrue(map.values().isEmpty(), "values collection must be empty after its own clear()");
    }

    @Test
    @DisplayName("clear() after many entries (beyond compactSize) still empties the map")
    void clearAfterLargeNumberOfEntries() {
        // Insert a number of entries that will force CompactMap to switch to a backing Map.
        // The exact threshold is implementation‑specific; we use a safe upper bound.
        int many = 200;
        for (int i = 0; i < many; i++) {
            map.put("k" + i, i);
        }
        assertEquals(many, map.size(), "Map should contain " + many + " entries before clear()");
        map.clear();
        assertTrue(map.isEmpty(), "Map must be empty after clear() with many entries");
        assertEquals(0, map.size(), "Size must be 0 after clear()");
        assertTrue(map.keySet().isEmpty(), "keySet must be empty after clear()");
        assertTrue(map.values().isEmpty(), "values collection must be empty after clear()");
    }

    @Test
    @DisplayName("clear on empty map – containsKey/containsValue behave correctly")
    void clearOnEmptyMapContainsChecks() {
        // The map is freshly created, therefore empty.
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        // Verify that containsKey / containsValue return false for any argument,
        // including null.
        assertFalse(map.containsKey(null), "containsKey(null) must be false on empty map");
        assertFalse(map.containsValue(null), "containsValue(null) must be false on empty map");
        assertFalse(map.containsKey("any"), "containsKey on arbitrary key must be false");
        assertFalse(map.containsValue(123), "containsValue on arbitrary value must be false");
        // should be a no‑op but keep the contract
        map.clear();
        // After clear the same expectations hold.
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertFalse(map.containsKey(null));
        assertFalse(map.containsValue(null));
    }

    @Test
    @DisplayName("clear after adding several entries – map behaves like a fresh instance")
    void clearAfterAddingSeveralEntries() {
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);
        map.put("four", 4);
        assertEquals(4, map.size());
        assertFalse(map.isEmpty());
        map.clear();
        // The map must look brand‑new.
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertFalse(map.containsKey("one"));
        assertFalse(map.containsValue(1));
        assertNull(map.get("any"));
        assertTrue(map.entrySet().isEmpty(), "entrySet must be empty after clear()");
        assertTrue(map.keySet().isEmpty(), "keySet must be empty after clear()");
        assertTrue(map.values().isEmpty(), "values must be empty after clear()");
    }

    @Test
    @DisplayName("clear after inserting a null key/value pair")
    void clearAfterNullKeyAndValue() {
        map.put(null, null);
        assertEquals(1, map.size());
        assertFalse(map.isEmpty());
        assertTrue(map.containsKey(null));
        assertTrue(map.containsValue(null));
        assertNull(map.get(null));
        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertFalse(map.containsKey(null));
        assertFalse(map.containsValue(null));
        assertNull(map.get(null));
    }

    @Test
    @DisplayName("entrySet iterator throws ConcurrentModificationException after clear")
    void entrySetIteratorConcurrentModification() {
        map.put("a", 1);
        map.put("b", 2);
        Iterator<CompactMap.Entry<String, Integer>> it = map.entrySet().iterator();
        // Structural modification
        map.clear();
        // The next call must detect the modification.
        assertThrows(ConcurrentModificationException.class, it::next, "Iterator should throw CME after underlying map is cleared");
    }

    @Test
    @DisplayName("keySet iterator throws ConcurrentModificationException after clear")
    void keySetIteratorConcurrentModification() {
        map.put("x", 10);
        map.put("y", 20);
        Iterator<String> it = map.keySet().iterator();
        map.clear();
        assertThrows(ConcurrentModificationException.class, it::next, "KeySet iterator must throw CME after clear()");
    }

    @Test
    @DisplayName("values iterator throws ConcurrentModificationException after clear")
    void valuesIteratorConcurrentModification() {
        map.put("p", 5);
        map.put("q", 6);
        Iterator<Integer> it = map.values().iterator();
        map.clear();
        assertThrows(ConcurrentModificationException.class, it::next, "Values iterator must throw CME after clear()");
    }

    @Test
    @DisplayName("entrySet iterator throws NoSuchElementException when empty after clear")
    void entrySetIteratorNoSuchElementAfterClear() {
        map.put("k", 100);
        Iterator<CompactMap.Entry<String, Integer>> it = map.entrySet().iterator();
        map.clear();
        // hasNext should be false; next() must throw NoSuchElementException
        assertFalse(it.hasNext(), "Iterator should report no next element after clear()");
        assertThrows(NoSuchElementException.class, it::next, "Iterator must throw NoSuchElementException when next() is called on an empty iterator");
    }

    @Test
    @DisplayName("keySet iterator throws NoSuchElementException when empty after clear")
    void keySetIteratorNoSuchElementAfterClear() {
        map.put("alpha", 1);
        Iterator<String> it = map.keySet().iterator();
        map.clear();
        assertFalse(it.hasNext(), "KeySet iterator should have no elements after clear()");
        assertThrows(NoSuchElementException.class, it::next, "KeySet iterator must throw NoSuchElementException after clear()");
    }

    @Test
    @DisplayName("values iterator throws NoSuchElementException when empty after clear")
    void valuesIteratorNoSuchElementAfterClear() {
        map.put("beta", 2);
        Iterator<Integer> it = map.values().iterator();
        map.clear();
        assertFalse(it.hasNext(), "Values iterator should have no elements after clear()");
        assertThrows(NoSuchElementException.class, it::next, "Values iterator must throw NoSuchElementException after clear()");
    }

    @Test
    @DisplayName("entrySet iterator remove() throws IllegalStateException after clear")
    void entrySetIteratorRemoveIllegalStateAfterClear() {
        map.put("z", 999);
        Iterator<CompactMap.Entry<String, Integer>> it = map.entrySet().iterator();
        map.clear();
        // remove() without a preceding successful next()
        assertThrows(IllegalStateException.class, it::remove, "Iterator.remove() must throw IllegalStateException when called without next()");
    }

    @Test
    @DisplayName("keySet iterator remove() throws IllegalStateException after clear")
    void keySetIteratorRemoveIllegalStateAfterClear() {
        map.put("k1", 1);
        Iterator<String> it = map.keySet().iterator();
        map.clear();
        assertThrows(IllegalStateException.class, it::remove, "KeySet iterator.remove() must throw IllegalStateException after clear()");
    }
}
