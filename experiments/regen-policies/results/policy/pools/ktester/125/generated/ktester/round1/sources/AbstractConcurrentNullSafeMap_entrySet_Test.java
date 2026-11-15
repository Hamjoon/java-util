package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import static org.junit.jupiter.api.Assertions.*;
import java.util.AbstractMap.SimpleEntry;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_entrySet_Test {

    private TestMap<String, String> map;

    /**
     * Simple concrete implementation that uses a plain {@link ConcurrentHashMap} as the backing store.
     */
    private static final class TestMap<K, V> extends AbstractConcurrentNullSafeMap<K, V> {

        TestMap() {
            super(new ConcurrentHashMap<>());
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        map = new TestMap<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the map is cleared after each test to avoid cross‑test contamination.
        map.clear();
    }

    @Test
    void entrySetIteratesOverAllEntriesIncludingNulls() {
        // Arrange – put a normal entry, a null key and a null value.
        map.put("a", "valueA");
        map.put(null, "nullKey");
        map.put("b", null);
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        // Act – collect entries into a temporary map for easy assertions.
        Map<String, String> collected = new HashMap<>();
        for (Map.Entry<String, String> e : entrySet) {
            collected.put(e.getKey(), e.getValue());
        }
        // Assert – all three entries are present and nulls are correctly unmasked.
        assertEquals(3, collected.size());
        assertEquals("valueA", collected.get("a"));
        assertEquals("nullKey", collected.get(null));
        assertNull(collected.get("b"));
    }

    @Test
    void iteratorRemoveDeletesEntryFromUnderlyingMap() {
        // Arrange
        map.put("x", "1");
        map.put("y", "2");
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        // Act – remove the first entry via the iterator.
        Iterator<Map.Entry<String, String>> it = entrySet.iterator();
        assertTrue(it.hasNext());
        Map.Entry<String, String> first = it.next();
        // should remove from the backing ConcurrentMap
        it.remove();
        // Assert – the map no longer contains the removed entry but still contains the other.
        assertEquals(1, map.size());
        assertFalse(map.containsKey(first.getKey()));
        assertTrue(map.containsKey("y"));
    }

    @Test
    void entrySetContainsDetectsExistingEntry() {
        // Arrange
        map.put("k1", "v1");
        Map.Entry<String, String> expected = new AbstractMap.SimpleEntry<>("k1", "v1");
        // Act
        boolean contains = map.entrySet().contains(expected);
        // Assert
        assertTrue(contains);
    }

    @Test
    void entrySetContainsRejectsNonMatchingEntry() {
        // Arrange
        map.put("k1", "v1");
        Map.Entry<String, String> different = new AbstractMap.SimpleEntry<>("k1", "different");
        // Act
        boolean contains = map.entrySet().contains(different);
        // Assert
        assertFalse(contains);
    }

    @Test
    void entrySetRemoveDeletesMatchingEntry() {
        // Arrange
        map.put("k2", "v2");
        Map.Entry<String, String> toRemove = new AbstractMap.SimpleEntry<>("k2", "v2");
        // Act
        boolean removed = map.entrySet().remove(toRemove);
        // Assert
        assertTrue(removed);
        assertFalse(map.containsKey("k2"));
        assertEquals(0, map.size());
    }

    @Test
    void entrySetRemoveReturnsFalseWhenEntryNotPresent() {
        // Arrange
        map.put("k3", "v3");
        Map.Entry<String, String> notPresent = new AbstractMap.SimpleEntry<>("k3", "different");
        // Act
        boolean removed = map.entrySet().remove(notPresent);
        // Assert
        assertFalse(removed);
        assertTrue(map.containsKey("k3"));
    }

    @Test
    void entrySetClearEmptiesMap() {
        // Arrange
        map.put("a", "1");
        map.put("b", "2");
        assertEquals(2, map.size());
        // Act
        map.entrySet().clear();
        // Assert
        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
    }

    @Test
    void entrySetSizeReflectsUnderlyingMapSize() {
        // Arrange
        map.put("one", "1");
        map.put(null, "nullKey");
        map.put("two", null);
        // Act
        int size = map.entrySet().size();
        // Assert
        assertEquals(3, size);
    }

    @Test
    void entrySetEntrySetValueUpdatesUnderlyingMap() {
        // Arrange
        map.put("key", "old");
        Map.Entry<String, String> entry = map.entrySet().iterator().next();
        // Act
        String previous = entry.setValue("new");
        // Assert
        assertEquals("old", previous);
        assertEquals("new", map.get("key"));
    }

    @Test
    void sizeWhenMapIsEmpty() {
        // No entries added.
        assertEquals(0, map.entrySet().size(), "Empty map should report size 0");
    }

    @Test
    void containsWithNonMapEntryArgument() {
        // Arrange
        map.put("k", "v");
        // Act & Assert
        assertFalse(map.entrySet().contains("just a string"), "contains(Object) with non‑Map.Entry argument must return false");
    }

    @Test
    void containsMatchingEntryWithNullKey() {
        map.put(null, "value1");
        Map.Entry<String, String> entry = new SimpleEntry<>(null, "value1");
        assertTrue(map.entrySet().contains(entry), "contains should recognize entry with null key");
    }

    @Test
    void containsMatchingEntryWithNullValue() {
        map.put("key2", null);
        Map.Entry<String, String> entry = new SimpleEntry<>("key2", null);
        assertTrue(map.entrySet().contains(entry), "contains should recognize entry with null value");
    }

    @Test
    void containsMatchingEntryExactPair() {
        map.put("key3", "value3");
        Map.Entry<String, String> entry = new SimpleEntry<>("key3", "value3");
        assertTrue(map.entrySet().contains(entry), "contains should recognize exact key/value pair");
    }

    @Test
    void containsNonMatchingEntryKeyAbsent() {
        Map.Entry<String, String> entry = new SimpleEntry<>("keyX", "valueX");
        assertFalse(map.entrySet().contains(entry), "contains must return false when key is absent");
    }

    @Test
    void containsNonMatchingEntryValueDiffers() {
        map.put("key2", "value2");
        Map.Entry<String, String> entry = new SimpleEntry<>("key2", "differentValue");
        assertFalse(map.entrySet().contains(entry), "contains must return false when key exists but value differs");
    }

    @Test
    void removeWithNonMapEntryArgument() {
        map.put("a", "b");
        assertFalse(map.entrySet().remove(12345), "remove(Object) with non‑Map.Entry argument must return false");
        assertEquals(1, map.size(), "Map size must remain unchanged");
    }

    @Test
    void removeMatchingEntry() {
        map.put("key2", "value2");
        Map.Entry<String, String> entry = new SimpleEntry<>("key2", "value2");
        assertTrue(map.entrySet().remove(entry), "remove should delete matching entry");
        assertFalse(map.containsKey("key2"));
        assertEquals(0, map.size());
    }

    @Test
    void removeNonMatchingEntryValueDiffers() {
        map.put("key2", "value2");
        Map.Entry<String, String> entry = new SimpleEntry<>("key2", "wrongValue");
        assertFalse(map.entrySet().remove(entry), "remove must return false when value differs");
        assertTrue(map.containsKey("key2"));
        assertEquals(1, map.size());
    }

    @Test
    void iteratorTraversalAndSetValue() {
        map.put("key1", "value1");
        Iterator<Map.Entry<String, String>> it = map.entrySet().iterator();
        assertTrue(it.hasNext());
        Map.Entry<String, String> entry = it.next();
        assertEquals("key1", entry.getKey());
        assertEquals("value1", entry.getValue());
        String old = entry.setValue("newVal");
        assertEquals("value1", old);
        assertEquals("newVal", map.get("key1"));
    }

    @Test
    void entryEqualsHashCodeAndToString() {
        map.put("key1", "value1");
        // Obtain two distinct Entry objects that represent the same mapping
        Map.Entry<String, String> e1 = map.entrySet().iterator().next();
        Map.Entry<String, String> e2 = map.entrySet().iterator().next();
        // equals & hashCode
        assertEquals(e1, e2, "Two Entry objects with same key/value must be equal");
        assertEquals(e1.hashCode(), e2.hashCode(), "Equal Entry objects must have identical hashCode");
        // equals with non‑Entry object
        assertNotEquals(e1, "some string", "Entry.equals must return false for non‑Entry objects");
        // toString format
        assertEquals("key1=value1", e1.toString(), "Entry.toString() must produce \"key=value\" using unmasked values");
    }

    @Test
    void clearEmptiesUnderlyingMap() {
        map.put("x", "1");
        map.put("y", "2");
        map.entrySet().clear();
        assertEquals(0, map.size());
        assertTrue(map.entrySet().isEmpty(), "After clear, entrySet should be empty");
    }

    @Test
    void iteratorRemoveOperationDecreasesSize() {
        map.put("a", "1");
        map.put("b", "2");
        int originalSize = map.entrySet().size();
        Iterator<Map.Entry<String, String>> it = map.entrySet().iterator();
        assertTrue(it.hasNext());
        it.next();
        it.remove();
        assertEquals(originalSize - 1, map.entrySet().size(), "Size after iterator.remove() must be originalSize‑1");
        assertEquals(originalSize - 1, map.size(), "Map size must also reflect the removal");
    }

    @Test
    void iteratorRemoveWithoutNextThrowsIllegalStateException() {
        map.put("a", "1");
        Iterator<Map.Entry<String, String>> it = map.entrySet().iterator();
        assertThrows(IllegalStateException.class, it::remove, "Calling remove() before next() must throw IllegalStateException");
    }

    @Test
    void containsThrowsClassCastExceptionOnWrongKeyType() {
        map.put("valid", "value");
        Map.Entry<Integer, String> entry = new SimpleEntry<>(123, "value");
        assertThrows(ClassCastException.class, () -> map.entrySet().contains(entry), "contains must throw ClassCastException when entry key type is incompatible");
    }

    @Test
    void removeThrowsClassCastExceptionOnWrongKeyType() {
        map.put("valid", "value");
        Map.Entry<Integer, String> entry = new SimpleEntry<>(123, "value");
        assertThrows(ClassCastException.class, () -> map.entrySet().remove(entry), "remove must throw ClassCastException when entry key type is incompatible");
    }

    @Test
    void removeEntryWithNullKeyAndNullValue() {
        map.put(null, null);
        Map.Entry<String, String> entry = new SimpleEntry<>(null, null);
        assertTrue(map.entrySet().remove(entry), "Removing null→null entry must return true");
        assertEquals(0, map.size(), "Map should be empty after removal");
    }

    @Test
    void containsEntryWithBothNullKeyAndNullValue() {
        map.put(null, null);
        Map.Entry<String, String> entry = new SimpleEntry<>(null, null);
        assertTrue(map.entrySet().contains(entry), "contains must recognize entry with both key and value null");
    }

    @Test
    void entrySetSizeAfterIteratorRemoval() {
        map.put("k1", "v1");
        map.put("k2", "v2");
        int before = map.entrySet().size();
        Iterator<Map.Entry<String, String>> it = map.entrySet().iterator();
        // advance to first entry
        it.next();
        it.remove();
        assertEquals(before - 1, map.entrySet().size(), "entrySet().size() must reflect removal performed via iterator");
    }

    @Test
    void iteratorSetValueWithNullValue() {
        map.put("key1", "oldValue");
        Map.Entry<String, String> entry = map.entrySet().iterator().next();
        String previous = entry.setValue(null);
        assertEquals("oldValue", previous, "setValue(null) should return previous non‑null value");
        assertNull(map.get("key1"), "Map must store null after setValue(null)");
    }

    @Test
    void containsAfterRemovalEntryNoLongerPresent() {
        map.put("key2", "value2");
        Map.Entry<String, String> entry = new SimpleEntry<>("key2", "value2");
        assertTrue(map.entrySet().remove(entry), "Initial removal must succeed");
        assertFalse(map.entrySet().contains(entry), "After removal, contains must return false for the same entry");
    }

    @Test
    void iteratorNextBeyondEndThrowsNoSuchElementException() {
        map.put("only", "one");
        Iterator<Map.Entry<String, String>> it = map.entrySet().iterator();
        assertTrue(it.hasNext());
        // consume the only element
        it.next();
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next, "Calling next() when no elements remain must throw NoSuchElementException");
    }

    @Test
    void iteratorDoubleRemoveThrowsIllegalStateException() {
        map.put("a", "1");
        Iterator<Map.Entry<String, String>> it = map.entrySet().iterator();
        assertTrue(it.hasNext());
        it.next();
        // first remove is legal
        it.remove();
        assertThrows(IllegalStateException.class, it::remove, "Calling remove() twice without intervening next() must throw IllegalStateException");
    }

    @Test
    void containsThrowsClassCastExceptionOnWrongValueType() {
        map.put("key1", "value1");
        Map.Entry<String, Integer> entry = new SimpleEntry<>("key1", 12345);
        assertThrows(ClassCastException.class, () -> map.entrySet().contains(entry), "contains must throw ClassCastException when entry value type is incompatible");
    }

    @Test
    void removeThrowsClassCastExceptionOnWrongValueType() {
        map.put("key2", "value2");
        Map.Entry<String, Object> entry = new SimpleEntry<>("key2", new Object());
        assertThrows(ClassCastException.class, () -> map.entrySet().remove(entry), "remove must throw ClassCastException when entry value type is incompatible");
    }
}
