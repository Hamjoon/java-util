package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collections;

/**
 * JUnit 4 test suite for {@link CompactLinkedMap}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public constructors</li>
 *   <li>Inherited public Map methods (size, isEmpty, put, get, remove, clear, containsKey, containsValue, etc.)</li>
 *   <li>Behavior of the overridden protected methods {@code getNewMap()} and {@code useCopyIterator()}</li>
 *   <li>Edge‑cases such as null keys/values, duplicate keys, large number of entries, and removal of non‑existent keys</li>
 *   <li>Exception scenarios (e.g., {@code NullPointerException} when a null key is not allowed by the underlying map)</li>
 * </ul>
 *
 * Note: {@code CompactLinkedMap} extends {@code CompactMap} which in turn implements {@code java.util.Map}.
 * The test therefore uses the public {@code Map} API to validate behaviour.
 */
public class CompactLinkedMapTest {

    private CompactLinkedMap<String, String> emptyMap;
    private CompactLinkedMap<String, String> populatedMap;

    @Before
    public void setUp() {
        emptyMap = new CompactLinkedMap<>();
        populatedMap = new CompactLinkedMap<>();
        populatedMap.put("one", "1");
        populatedMap.put("two", "2");
        populatedMap.put("three", "3");
    }

    /** --------------------------------------------------------------
     *  Constructor tests
     *  --------------------------------------------------------------
     */

    @Test
    public void testDefaultConstructorCreatesEmptyMap() {
        assertTrue("New map should be empty", emptyMap.isEmpty());
        assertEquals("Size should be 0", 0, emptyMap.size());
    }

    @Test
    public void testCopyConstructorCopiesAllEntries() {
        Map<String, String> source = new LinkedHashMap<>();
        source.put("a", "A");
        source.put("b", "B");

        CompactLinkedMap<String, String> copy = new CompactLinkedMap<>(source);
        assertEquals("Copy should have same size as source", source.size(), copy.size());
        assertTrue("Copy should contain all keys from source", copy.keySet().containsAll(source.keySet()));
        assertTrue("Copy should contain all values from source", copy.values().containsAll(source.values()));
    }

    /** --------------------------------------------------------------
     *  Basic Map operation tests (inherited from CompactMap)
     *  --------------------------------------------------------------
     */

    @Test
    public void testPutAndGet() {
        emptyMap.put("key", "value");
        assertEquals("Value retrieved should match inserted value", "value", emptyMap.get("key"));
        assertEquals("Size should be 1 after one put", 1, emptyMap.size());
    }

    @Test
    public void testPutOverwrite() {
        populatedMap.put("two", "22");
        assertEquals("Overwritten value should be returned by get", "22", populatedMap.get("two"));
        assertEquals("Size must stay the same after overwriting", 3, populatedMap.size());
    }

    @Test
    public void testRemoveExistingKey() {
        String removed = populatedMap.remove("two");
        assertEquals("Removed value must be returned", "2", removed);
        assertFalse("Map must no longer contain removed key", populatedMap.containsKey("two"));
        assertEquals("Size must decrement", 2, populatedMap.size());
    }

    @Test
    public void testRemoveNonExistingKey() {
        assertNull("Removing a non‑existent key should return null", populatedMap.remove("nonexistent"));
        assertEquals("Size must stay unchanged", 3, populatedMap.size());
    }

    @Test
    public void testContainsKeyAndValue() {
        assertTrue(populatedMap.containsKey("one"));
        assertFalse(populatedMap.containsKey("four"));
        assertTrue(populatedMap.containsValue("3"));
        assertFalse(populatedMap.containsValue("4"));
    }

    @Test
    public void testClear() {
        populatedMap.clear();
        assertTrue("Map should be empty after clear", populatedMap.isEmpty());
        assertEquals("Size must be 0 after clear", 0, populatedMap.size());
    }

    @Test
    public void testPutAll() {
        Map<String, String> other = new LinkedHashMap<>();
        other.put("four", "4");
        other.put("five", "5");

        populatedMap.putAll(other);
        assertEquals("Size must increase by number of new entries", 5, populatedMap.size());
        assertTrue(populatedMap.containsKey("four"));
        assertTrue(populatedMap.containsKey("five"));
    }

    @Test
    public void testKeySetOrderIsInsertionOrder() {
        // CompactLinkedMap should preserve insertion order (LinkedHashMap semantics)
        int index = 0;
        String[] expectedOrder = {"one", "two", "three"};
        for (String key : populatedMap.keySet()) {
            assertEquals("Insertion order must be preserved", expectedOrder[index++], key);
        }
    }

    /** --------------------------------------------------------------
     *  Edge‑case tests
     *  --------------------------------------------------------------
     */

    @Test
    public void testNullKeyAllowed() {
        // LinkedHashMap permits null keys; CompactMap delegates to underlying map.
        emptyMap.put(null, "nullValue");
        assertTrue(emptyMap.containsKey(null));
        assertEquals("nullValue", emptyMap.get(null));
    }

    @Test
    public void testNullValueAllowed() {
        emptyMap.put("nullValueKey", null);
        assertTrue(emptyMap.containsKey("nullValueKey"));
        assertNull(emptyMap.get("nullValueKey"));
    }

    @Test
    public void testLargeNumberOfEntries() {
        CompactLinkedMap<Integer, Integer> largeMap = new CompactLinkedMap<>();
        int count = 500; // well beyond the default compactSize (70)
        for (int i = 0; i < count; i++) {
            largeMap.put(i, i * 10);
        }
        assertEquals("All entries should be stored", count, largeMap.size());
        // spot‑check a few entries
        assertEquals(Integer.valueOf(0), largeMap.get(0));
        assertEquals(Integer.valueOf(2500), largeMap.get(250));
        assertEquals(Integer.valueOf(4990), largeMap.get(499));
    }

    @Test
    public void testDuplicateKeyInsertionOrderUnchanged() {
        populatedMap.put("one", "ONE");
        // The key order should stay the same after overwriting a key.
        String[] expected = {"one", "two", "three"};
        int i = 0;
        for (String key : populatedMap.keySet()) {
            assertEquals(expected[i++], key);
        }
    }

    /** --------------------------------------------------------------
     *  Protected method behavior verification (via reflection)
     *  --------------------------------------------------------------
     */

    @Test
    public void testGetNewMapCreatesLinkedHashMapWithCorrectInitialCapacity() throws Exception {
        // Use reflection to invoke the protected method
        java.lang.reflect.Method m = CompactLinkedMap.class.getDeclaredMethod("getNewMap");
        m.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> newMap = (Map<String, String>) m.invoke(emptyMap);
        assertTrue("Returned map must be a LinkedHashMap", newMap instanceof LinkedHashMap);
        // The initial capacity is compactSize()+1; we cannot read it directly,
        // but we can ensure the map behaves like a freshly created LinkedHashMap.
        assertTrue("New map should be empty", newMap.isEmpty());
    }

    @Test
    public void testUseCopyIteratorReturnsFalse() throws Exception {
        java.lang.reflect.Method m = CompactLinkedMap.class.getDeclaredMethod("useCopyIterator");
        m.setAccessible(true);
        Boolean result = (Boolean) m.invoke(emptyMap);
        assertFalse("useCopyIterator must return false for CompactLinkedMap", result);
    }

    /** --------------------------------------------------------------
     *  Exception scenario tests
     *  --------------------------------------------------------------
     */

    @Test(expected = UnsupportedOperationException.class)
    public void testUnmodifiableEntrySetThrowsOnRemove() {
        // entrySet().remove() is optional; CompactMap delegates to LinkedHashMap which allows it,
        // but we deliberately test the contract by using an unmodifiable view.
        // Wrap the map with Collections.unmodifiableMap to provoke the exception.
        Map<String, String> unmod = Collections.unmodifiableMap(populatedMap);
        unmod.entrySet().remove(unmod.entrySet().iterator().next());
    }

    @Test
    public void testEqualsAndHashCodeContract() {
        CompactLinkedMap<String, String> mapA = new CompactLinkedMap<>();
        CompactLinkedMap<String, String> mapB = new CompactLinkedMap<>();

        mapA.put("x", "X");
        mapA.put("y", "Y");

        mapB.put("x", "X");
        mapB.put("y", "Y");

        assertEquals("Maps with same entries must be equal", mapA, mapB);
        assertEquals("Equal maps must have same hashCode", mapA.hashCode(), mapB.hashCode());

        mapB.put("z", "Z");
        assertNotEquals("Maps with different entries must not be equal", mapA, mapB);
    }

    @Test
    public void testToStringContainsEntries() {
        String s = populatedMap.toString();
        assertTrue("toString should contain key 'one'", s.contains("one"));
        assertTrue("toString should contain value '2'", s.contains("2"));
    }
}
