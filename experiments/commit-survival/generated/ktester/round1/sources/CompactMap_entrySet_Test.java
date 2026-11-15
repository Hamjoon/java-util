package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

// Mockito not used but kept from template
@ExtendWith(MockitoExtension.class)
class CompactMap_entrySet_Test {

    private CompactMap<String, String> map;

    /* -------------------------------------------------------------
     *  entrySet_iterator_type – copy vs compact iterator
     * ------------------------------------------------------------- */
    private static class CopyIterMap<K, V> extends CompactMap<K, V> {

        @Override
        protected boolean useCopyIterator() {
            return true;
        }
    }

    private static class CompactIterMap<K, V> extends CompactMap<K, V> {

        @Override
        protected boolean useCopyIterator() {
            return false;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        map = new CompactMap<>();
        map = new CompactMap<>();
    }

    @Test
    void entrySetSizeAndIteration() {
        map.put("one", "1");
        map.put("two", "2");
        map.put("three", "3");
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        // size should reflect map size
        assertEquals(3, entrySet.size());
        // iterator should return all entries
        Iterator<Map.Entry<String, String>> it = entrySet.iterator();
        int count = 0;
        while (it.hasNext()) {
            Map.Entry<String, String> e = it.next();
            assertTrue(map.containsKey(e.getKey()));
            assertEquals(map.get(e.getKey()), e.getValue());
            count++;
        }
        assertEquals(3, count);
    }

    @Test
    void entrySetContains() {
        map.put("k", "v");
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        // Positive case
        Map.Entry<String, String> present = new AbstractMap.SimpleEntry<>("k", "v");
        assertTrue(entrySet.contains(present));
        // Wrong value
        Map.Entry<String, String> wrongValue = new AbstractMap.SimpleEntry<>("k", "x");
        assertFalse(entrySet.contains(wrongValue));
        // Wrong key
        Map.Entry<String, String> wrongKey = new AbstractMap.SimpleEntry<>("z", "v");
        assertFalse(entrySet.contains(wrongKey));
        // Not a Map.Entry at all
        assertFalse(entrySet.contains("not an entry"));
    }

    @Test
    void entrySetRemove() {
        map.put("a", "alpha");
        map.put("b", "beta");
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        Map.Entry<String, String> toRemove = new AbstractMap.SimpleEntry<>("a", "alpha");
        assertTrue(entrySet.remove(toRemove));
        assertFalse(map.containsKey("a"));
        assertEquals(1, entrySet.size());
        // Removing a non‑present entry returns false and does not affect size
        Map.Entry<String, String> absent = new AbstractMap.SimpleEntry<>("c", "gamma");
        assertFalse(entrySet.remove(absent));
        assertEquals(1, entrySet.size());
    }

    @Test
    void entrySetRemoveAll() {
        map.put("x", "10");
        map.put("y", "20");
        map.put("z", "30");
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        Collection<Object> toRemove = new ArrayList<>();
        toRemove.add(new AbstractMap.SimpleEntry<>("x", "10"));
        toRemove.add(new AbstractMap.SimpleEntry<>("y", "20"));
        // should be ignored
        toRemove.add("not an entry");
        assertTrue(entrySet.removeAll(toRemove));
        assertFalse(map.containsKey("x"));
        assertFalse(map.containsKey("y"));
        assertTrue(map.containsKey("z"));
        assertEquals(1, entrySet.size());
    }

    @Test
    void entrySetRetainAll() {
        map.put("a", "1");
        map.put("b", "2");
        map.put("c", "3");
        map.put("d", "4");
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        Collection<Object> toRetain = new ArrayList<>();
        toRetain.add(new AbstractMap.SimpleEntry<>("b", "2"));
        toRetain.add(new AbstractMap.SimpleEntry<>("d", "4"));
        // not present
        toRetain.add(new AbstractMap.SimpleEntry<>("x", "99"));
        assertTrue(entrySet.retainAll(toRetain));
        assertFalse(map.containsKey("a"));
        assertFalse(map.containsKey("c"));
        assertTrue(map.containsKey("b"));
        assertTrue(map.containsKey("d"));
        assertEquals(2, entrySet.size());
    }

    @Test
    void entrySetClear() {
        map.put("p", "p1");
        map.put("q", "q1");
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        assertFalse(map.isEmpty());
        entrySet.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, entrySet.size());
    }

    @Test
    void iteratorRemoveWorks() {
        map.put("k1", "v1");
        map.put("k2", "v2");
        map.put("k3", "v3");
        Iterator<Map.Entry<String, String>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> e = it.next();
            if ("k2".equals(e.getKey())) {
                // remove via iterator
                it.remove();
            }
        }
        assertFalse(map.containsKey("k2"));
        assertEquals(2, map.size());
    }

    /* -------------------------------------------------------------
     *  entrySet_size_and_clear – additional checks
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("entrySet().size() on a newly created map is 0")
    void entrySetSizeEmpty() {
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        assertEquals(0, entrySet.size(), "Empty map should have entrySet size 0");
    }

    @Test
    @DisplayName("entrySet().size() reflects map size after puts")
    void entrySetSizeAfterPuts() {
        map.put("a", "1");
        map.put("b", "2");
        map.put("c", "3");
        assertEquals(3, map.entrySet().size(), "Size should be 3 after three puts");
    }

    @Test
    @DisplayName("entrySet().clear() empties the underlying CompactMap")
    void entrySetClearEmptiesMap() {
        map.put("x", "x1");
        map.put("y", "y1");
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        entrySet.clear();
        assertTrue(map.isEmpty(), "Map should be empty after entrySet.clear()");
        assertEquals(0, entrySet.size(), "entrySet size must be 0 after clear");
    }

    /* -------------------------------------------------------------
     *  entrySet_contains – null value handling and non‑entry objects
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("contains() returns true for entry with null value")
    void entrySetContainsNullValue() {
        map.put("c", null);
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        Map.Entry<String, String> entry = new AbstractMap.SimpleEntry<>("c", null);
        assertTrue(entrySet.contains(entry), "Entry with null value should be reported as present");
    }

    @Test
    @DisplayName("contains() returns false for non‑Map.Entry argument")
    void entrySetContainsNonEntry() {
        map.put("a", "1");
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        assertFalse(entrySet.contains("not an entry"), "contains() must return false for non‑Entry objects");
    }

    @Test
    @DisplayName("contains() returns false for entry with missing key")
    void entrySetContainsMissingKey() {
        map.put("a", "1");
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        Map.Entry<String, String> missing = new AbstractMap.SimpleEntry<>("missing", "1");
        assertFalse(entrySet.contains(missing), "Entry with a key not present should return false");
    }

    /* -------------------------------------------------------------
     *  entrySet_remove – non‑Entry argument
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("remove() returns false when argument is not a Map.Entry")
    void entrySetRemoveNonEntry() {
        map.put("a", "alpha");
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        assertFalse(entrySet.remove("just a string"), "remove() must return false for non‑Entry argument");
        assertEquals(1, map.size(), "Map size must stay unchanged");
    }

    /* -------------------------------------------------------------
     *  entrySet_removeAll – empty collection and collection with non‑Entry
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("removeAll() with empty collection returns false and does nothing")
    void entrySetRemoveAllEmptyCollection() {
        map.put("a", "1");
        map.put("b", "2");
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        Collection<Object> empty = Collections.emptyList();
        assertFalse(entrySet.removeAll(empty), "removeAll on empty collection should return false");
        assertEquals(2, map.size(), "Map size must stay unchanged");
    }

    @Test
    @DisplayName("removeAll() ignores non‑Entry objects and returns false if nothing removed")
    void entrySetRemoveAllNonEntryObjects() {
        map.put("a", "1");
        map.put("b", "2");
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        Collection<Object> mixed = new ArrayList<>();
        mixed.add("not an entry");
        mixed.add(123);
        assertFalse(entrySet.removeAll(mixed), "removeAll with only non‑Entry objects should return false");
        assertEquals(2, map.size(), "Map size must stay unchanged");
    }

    /* -------------------------------------------------------------
     *  entrySet_retainAll – various edge cases
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("retainAll() with identical collection returns false and leaves map unchanged")
    void entrySetRetainAllIdentical() {
        map.put("a", "1");
        map.put("b", "2");
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        Collection<Object> identical = new ArrayList<>();
        identical.add(new AbstractMap.SimpleEntry<>("a", "1"));
        identical.add(new AbstractMap.SimpleEntry<>("b", "2"));
        assertFalse(entrySet.retainAll(identical), "retainAll with identical collection should return false");
        assertEquals(2, map.size(), "Map size must stay unchanged");
    }

    @Test
    @DisplayName("retainAll() removes entry with mismatched value")
    void entrySetRetainAllMismatchedValue() {
        map.put("a", "1");
        map.put("b", "2");
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        Collection<Object> collection = new ArrayList<>();
        // same key, different value
        collection.add(new AbstractMap.SimpleEntry<>("a", "99"));
        // correct entry
        collection.add(new AbstractMap.SimpleEntry<>("b", "2"));
        assertTrue(entrySet.retainAll(collection), "retainAll should return true when a removal occurs");
        assertFalse(map.containsKey("a"), "Key 'a' should have been removed due to value mismatch");
        assertTrue(map.containsKey("b"));
        assertEquals(1, map.size());
    }

    @Test
    @DisplayName("retainAll() with empty collection clears the map")
    void entrySetRetainAllEmptyCollection() {
        map.put("x", "1");
        map.put("y", "2");
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        assertTrue(entrySet.retainAll(Collections.emptyList()), "retainAll on empty collection should return true");
        assertTrue(map.isEmpty(), "Map must be empty after retainAll with empty collection");
    }

    @Test
    @DisplayName("iterator() returns CopyEntryIterator when useCopyIterator() is true")
    void iteratorIsCopyEntryIterator() throws ClassNotFoundException {
        CompactMap<String, String> copyMap = new CopyIterMap<>();
        copyMap.put("a", "1");
        Iterator<Map.Entry<String, String>> it = copyMap.entrySet().iterator();
        Class<?> copyIterClass = Class.forName("com.cedarsoftware.util.CompactMap$CopyEntryIterator");
        assertTrue(copyIterClass.isInstance(it), "Iterator should be instance of CompactMap.CopyEntryIterator");
    }

    @Test
    @DisplayName("iterator() returns CompactEntryIterator when useCopyIterator() is false")
    void iteratorIsCompactEntryIterator() throws ClassNotFoundException {
        CompactMap<String, String> compactMap = new CompactIterMap<>();
        compactMap.put("a", "1");
        Iterator<Map.Entry<String, String>> it = compactMap.entrySet().iterator();
        Class<?> compactIterClass = Class.forName("com.cedarsoftware.util.CompactMap$CompactEntryIterator");
        assertTrue(compactIterClass.isInstance(it), "Iterator should be instance of CompactMap.CompactEntryIterator");
    }

    /* -------------------------------------------------------------
     *  entrySet_removeAll / retainAll – null collection handling
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("removeAll(null) throws NullPointerException")
    void removeAllNullCollection() {
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        assertThrows(NullPointerException.class, () -> entrySet.removeAll(null), "removeAll(null) must throw NullPointerException");
    }

    @Test
    @DisplayName("retainAll(null) throws NullPointerException")
    void retainAllNullCollection() {
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        assertThrows(NullPointerException.class, () -> entrySet.retainAll(null), "retainAll(null) must throw NullPointerException");
    }

    /* -------------------------------------------------------------
     *  iterator edge cases – NoSuchElementException, IllegalStateException,
     *  ConcurrentModificationException
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("iterator.next() on empty map throws NoSuchElementException")
    void iteratorNextOnEmptyThrows() {
        Iterator<Map.Entry<String, String>> it = map.entrySet().iterator();
        assertThrows(NoSuchElementException.class, it::next, "Calling next() on empty iterator must throw NoSuchElementException");
    }

    @Test
    @DisplayName("iterator.remove() before next() throws IllegalStateException")
    void iteratorRemoveBeforeNextThrows() {
        map.put("k", "v");
        Iterator<Map.Entry<String, String>> it = map.entrySet().iterator();
        assertThrows(IllegalStateException.class, it::remove, "Calling remove() before next() must throw IllegalStateException");
    }

    @Test
    @DisplayName("iterator detects concurrent modification")
    void iteratorConcurrentModification() {
        map.put("a", "1");
        map.put("b", "2");
        Iterator<Map.Entry<String, String>> it = map.entrySet().iterator();
        // advance once so iterator is in a valid state
        assertTrue(it.hasNext());
        it.next();
        // modify the map directly
        map.put("c", "3");
        // subsequent iterator operation must detect the change
        assertThrows(ConcurrentModificationException.class, it::hasNext, "Iterator should throw ConcurrentModificationException after concurrent modification");
    }
}
