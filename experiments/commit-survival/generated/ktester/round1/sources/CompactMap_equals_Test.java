package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CompactMap_equals_Test {

    /**
     * Subclass of {@link CompactMap} that deliberately rejects {@code null} keys
     * by throwing a {@link NullPointerException} from {@code containsKey}.
     */
    private static class NoNullKeyCompactMap<K, V> extends CompactMap<K, V> {

        @Override
        public boolean containsKey(Object key) {
            if (key == null) {
                throw new NullPointerException("null keys are not supported by this CompactMap variant");
            }
            return super.containsKey(key);
        }
    }

    @Test
    void equalsSameInstance_returnsTrue() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("a", 1);
        assertTrue(map.equals(map), "A map must be equal to itself");
    }

    @Test
    void equalsWithNonMap_returnsFalse() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("a", 1);
        assertFalse(map.equals("not a map"), "CompactMap should return false when compared with a non‑Map");
    }

    @Test
    void equalsDifferentSize_returnsFalse() {
        CompactMap<String, Integer> map1 = new CompactMap<>();
        map1.put("a", 1);
        map1.put("b", 2);
        Map<String, Integer> map2 = new HashMap<>();
        // only one entry
        map2.put("a", 1);
        assertFalse(map1.equals(map2), "Maps with different sizes must not be equal");
    }

    @Test
    void emptyMapEqualsEmptyHashMap() {
        CompactMap<String, Integer> emptyCompact = new CompactMap<>();
        Map<String, Integer> emptyHash = new HashMap<>();
        assertTrue(emptyCompact.equals(emptyHash), "Two empty maps must be equal");
        assertTrue(emptyHash.equals(emptyCompact), "Equality must be symmetric");
    }

    @Test
    void emptyMapNotEqualToNonEmpty() {
        CompactMap<String, Integer> emptyCompact = new CompactMap<>();
        Map<String, Integer> nonEmpty = new HashMap<>();
        nonEmpty.put("x", 42);
        assertFalse(emptyCompact.equals(nonEmpty), "Empty CompactMap must not equal a non‑empty map");
    }

    @Test
    void singleEntryMapEqualsEquivalentMap() {
        CompactMap<String, Integer> compact = new CompactMap<>();
        compact.put("key", 100);
        Map<String, Integer> other = new HashMap<>();
        other.put("key", 100);
        assertTrue(compact.equals(other), "Maps with a single identical entry must be equal");
    }

    @Test
    void singleEntryMapNotEqualWhenValueDiffers() {
        CompactMap<String, Integer> compact = new CompactMap<>();
        compact.put("key", 100);
        Map<String, Integer> other = new HashMap<>();
        other.put("key", 200);
        assertFalse(compact.equals(other), "Maps with same key but different values must not be equal");
    }

    @Test
    void twoEntryMapEqualsEquivalentMap() {
        CompactMap<String, Integer> compact = new CompactMap<>();
        compact.put("a", 1);
        compact.put("b", 2);
        Map<String, Integer> other = new HashMap<>();
        other.put("b", 2);
        // insertion order different on purpose
        other.put("a", 1);
        assertTrue(compact.equals(other), "CompactMap with two entries must equal a map with same entries irrespective of order");
    }

    @Test
    void twoEntryMapNotEqualWhenMissingKey() {
        CompactMap<String, Integer> compact = new CompactMap<>();
        compact.put("a", 1);
        compact.put("b", 2);
        Map<String, Integer> other = new HashMap<>();
        // missing "b"
        other.put("a", 1);
        assertFalse(compact.equals(other), "Maps with missing keys must not be equal");
    }

    @Test
    void largeMapEqualsEquivalentMap() {
        // CompactMap's default compactSize() is 80, so we insert 85 entries
        CompactMap<Integer, Integer> compact = new CompactMap<>();
        Map<Integer, Integer> reference = new HashMap<>();
        for (int i = 0; i < 85; i++) {
            compact.put(i, i * 10);
            reference.put(i, i * 10);
        }
        assertTrue(compact.equals(reference), "CompactMap that has switched to an internal Map must equal an equivalent map");
    }

    @Test
    void largeMapNotEqualWhenValueDiffers() {
        CompactMap<Integer, Integer> compact = new CompactMap<>();
        Map<Integer, Integer> reference = new HashMap<>();
        for (int i = 0; i < 85; i++) {
            compact.put(i, i * 10);
            reference.put(i, i * 10);
        }
        // Introduce a difference
        reference.put(42, 999);
        assertFalse(compact.equals(reference), "Large maps with a differing value must not be equal");
    }

    @Test
    void mapWithNullKeyAndValueEqualsEquivalentMap() {
        CompactMap<String, String> compact = new CompactMap<>();
        compact.put(null, null);
        compact.put("x", null);
        // last put replaces previous null key entry
        compact.put(null, "y");
        Map<String, String> other = new HashMap<>();
        other.put(null, "y");
        other.put("x", null);
        assertTrue(compact.equals(other), "Maps containing null keys/values must compare correctly");
    }

    @Test
    void mapWithNullValueNotEqualWhenOtherHasNonNull() {
        CompactMap<String, String> compact = new CompactMap<>();
        compact.put("k", null);
        Map<String, String> other = new HashMap<>();
        other.put("k", "notNull");
        assertFalse(compact.equals(other), "Null vs non‑null values must be considered unequal");
    }

    @Test
    void equalsThrowsWhenOtherMapHasNullKey_andCompactMapDoesNotSupportNull() {
        // Arrange: a CompactMap variant that cannot handle null keys
        CompactMap<String, String> compact = new NoNullKeyCompactMap<>();
        compact.put("present", "value");
        // The other map contains a null key – this should trigger the NPE
        Map<String, String> other = new HashMap<>();
        other.put(null, "value");
        // Act & Assert: the NPE from containsKey(null) must propagate
        assertThrows(NullPointerException.class, () -> compact.equals(other), "When the argument map contains a null key and CompactMap does not support null keys, equals should throw NullPointerException");
    }
}
