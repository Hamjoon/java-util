package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Array;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveMap_entrySet_Test {

    private CaseInsensitiveMap<String, String> map;

    @BeforeEach
    void setupBeforeEach() {
        map = new CaseInsensitiveMap<>();
        map.put("One", "Alpha");
        map.put("Two", "Beta");
        map.put("Three", "Gamma");
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the static cache is reset for other tests
        CaseInsensitiveMap.setMaxCacheLengthString(100);
    }

    @Test
    void entrySetSizeReflectsMapSize() {
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        assertEquals(3, entrySet.size(), "Entry set size should match map size");
    }

    @Test
    void entrySetIteratorProvidesCaseInsensitiveKeys() {
        Iterator<Map.Entry<String, String>> it = map.entrySet().iterator();
        Set<String> seenKeys = new HashSet<>();
        while (it.hasNext()) {
            Map.Entry<String, String> e = it.next();
            // Keys must be returned in their original case
            assertTrue(e.getKey().equals("One") || e.getKey().equals("Two") || e.getKey().equals("Three"));
            seenKeys.add(e.getKey());
        }
        assertEquals(Set.of("One", "Two", "Three"), seenKeys);
    }

    @Test
    void entrySetContainsWorksCaseInsensitively() {
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        Map.Entry<String, String> entry = new AbstractMap.SimpleEntry<>("one", "Alpha");
        assertTrue(entrySet.contains(entry), "Entry set should contain entry with case‑insensitive key");
        // Different value should not be considered contained
        Map.Entry<String, String> wrongValue = new AbstractMap.SimpleEntry<>("One", "Wrong");
        assertFalse(entrySet.contains(wrongValue));
    }

    @Test
    void entrySetToArrayReturnsCaseInsensitiveEntries() {
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        Object[] array = entrySet.toArray();
        assertEquals(3, array.length);
        Class<?> cieClass = Class.forName("com.cedarsoftware.util.CaseInsensitiveEntry");
        for (Object o : array) {
            assertTrue(o instanceof Map.Entry);
            assertTrue(cieClass.isInstance(o), "Elements should be CaseInsensitiveEntry instances");
        }
    }

    @Test
    void entrySetToTypedArrayRespectsProvidedArray() {
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        Map.Entry<String, String>[] supplied = new Map.Entry[2];
        Map.Entry<String, String>[] result = entrySet.toArray(supplied);
        assertEquals(3, result.length);
        assertTrue(result[0] instanceof Map.Entry);
        assertTrue(result[1] instanceof Map.Entry);
        // may be null if array was larger
        assertTrue(result[2] == null || result[2] instanceof Map.Entry);
    }

    @Test
    void entrySetRemoveDeletesFromUnderlyingMap() {
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        Map.Entry<String, String> toRemove = new AbstractMap.SimpleEntry<>("two", "Beta");
        assertTrue(entrySet.remove(toRemove), "remove should return true for existing entry");
        assertNull(map.get("Two"));
        assertEquals(2, map.size());
    }

    @Test
    void entrySetRemoveAllDeletesMultipleEntries() {
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        List<Map.Entry<String, String>> toRemove = List.of(new AbstractMap.SimpleEntry<>("one", "Alpha"), new AbstractMap.SimpleEntry<>("three", "Gamma"));
        assertTrue(entrySet.removeAll(toRemove));
        assertEquals(1, map.size());
        assertEquals("Beta", map.get("Two"));
    }

    @Test
    void entrySetRetainAllKeepsOnlySpecifiedEntries() {
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        // Keep only entry with key "Two"
        List<Map.Entry<String, String>> retain = List.of(new AbstractMap.SimpleEntry<>("Two", "Beta"));
        assertTrue(entrySet.retainAll(retain));
        assertEquals(1, map.size());
        assertEquals("Beta", map.get("Two"));
        assertNull(map.get("One"));
        assertNull(map.get("Three"));
    }

    @Test
    void entrySetClearEmptiesMap() {
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        entrySet.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, entrySet.size());
    }

    @Test
    void entrySetSizeAndIsEmptyOnEmptyMap() {
        CaseInsensitiveMap<String, String> empty = new CaseInsensitiveMap<>();
        Set<Map.Entry<String, String>> entrySet = empty.entrySet();
        assertEquals(0, entrySet.size());
        assertTrue(entrySet.isEmpty());
    }

    @Test
    void entrySetSizeAndIsEmptyOnPopulatedMap() {
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        assertEquals(3, entrySet.size());
        assertFalse(entrySet.isEmpty());
    }

    @Test
    void iteratorRemoveWorksAndUpdatesUnderlyingMap() {
        Iterator<Map.Entry<String, String>> it = map.entrySet().iterator();
        assertTrue(it.hasNext());
        Map.Entry<String, String> first = it.next();
        assertEquals("One", first.getKey());
        // remove the entry we just visited
        it.remove();
        assertNull(map.get("One"));
        assertEquals(2, map.size());
    }

    @Test
    void iteratorRemoveBeforeNextThrowsIllegalStateException() {
        Iterator<Map.Entry<String, String>> it = map.entrySet().iterator();
        assertThrows(IllegalStateException.class, it::remove);
    }

    @Test
    void iteratorRemoveTwiceThrowsIllegalStateException() {
        Iterator<Map.Entry<String, String>> it = map.entrySet().iterator();
        it.next();
        // first removal OK
        it.remove();
        assertThrows(IllegalStateException.class, it::remove);
    }

    @Test
    void iteratorNextWhenEmptyThrowsNoSuchElementException() {
        map.clear();
        Iterator<Map.Entry<String, String>> it = map.entrySet().iterator();
        assertThrows(NoSuchElementException.class, it::next);
    }

    @Test
    void iteratorDetectsConcurrentModification() {
        Iterator<Map.Entry<String, String>> it = map.entrySet().iterator();
        // Direct modification of the map should cause CME on next()
        map.put("Four", "Delta");
        assertThrows(ConcurrentModificationException.class, it::next);
    }

    @Test
    void entrySetContainsNonEntryReturnsFalse() {
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        assertFalse(entrySet.contains("not an entry"));
    }

    @Test
    void entrySetContainsNullReturnsFalse() {
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        assertFalse(entrySet.contains(null));
    }

    @Test
    void entrySetContainsCaseInsensitiveKeyMatches() {
        map.put("Key", "value");
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        Map.Entry<String, String> entry = new AbstractMap.SimpleEntry<>("key", "value");
        assertTrue(entrySet.contains(entry));
    }

    @Test
    void entrySetContainsNullValueMatches() {
        map.put("Key", null);
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        Map.Entry<String, String> entry = new AbstractMap.SimpleEntry<>("key", null);
        assertTrue(entrySet.contains(entry));
    }

    @Test
    void entrySetContainsValueMismatchReturnsFalse() {
        map.put("Key", "value1");
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        Map.Entry<String, String> entry = new AbstractMap.SimpleEntry<>("key", "value2");
        assertFalse(entrySet.contains(entry));
    }

    @Test
    void entrySetContainsMissingKeyReturnsFalse() {
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        Map.Entry<String, String> entry = new AbstractMap.SimpleEntry<>("Missing", "any");
        assertFalse(entrySet.contains(entry));
    }

    @Test
    void entrySetContainsNullValueButNonNullQueryValueReturnsFalse() {
        map.put("Key", null);
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        Map.Entry<String, String> entry = new AbstractMap.SimpleEntry<>("key", "nonNull");
        assertFalse(entrySet.contains(entry));
    }

    @Test
    void entrySetRemoveExistingEntryReturnsTrue() {
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        Map.Entry<String, String> toRemove = new AbstractMap.SimpleEntry<>("two", "Beta");
        assertTrue(entrySet.remove(toRemove));
        assertNull(map.get("Two"));
        assertEquals(2, map.size());
    }

    @Test
    void entrySetRemoveNonEntryReturnsFalse() {
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        assertFalse(entrySet.remove("not an entry"));
    }

    @Test
    void entrySetRemoveKeyCaseInsensitive() {
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        Map.Entry<String, String> toRemove = new AbstractMap.SimpleEntry<>("kEy", "Alpha");
        assertTrue(entrySet.remove(toRemove));
        assertNull(map.get("One"));
    }

    @Test
    void entrySetRemoveWithDifferentValueStillRemoves() {
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        Map.Entry<String, String> toRemove = new AbstractMap.SimpleEntry<>("One", "different");
        assertTrue(entrySet.remove(toRemove));
        assertNull(map.get("One"));
    }

    @Test
    void entrySetRemoveNonExistingKeyReturnsFalse() {
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        Map.Entry<String, String> toRemove = new AbstractMap.SimpleEntry<>("Missing", "value");
        assertFalse(entrySet.remove(toRemove));
        assertEquals(3, map.size());
    }

    @Test
    void entrySetRemoveAllRemovesMatchingEntries() {
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        List<Map.Entry<String, String>> toRemove = List.of(new AbstractMap.SimpleEntry<>("one", "Alpha"), new AbstractMap.SimpleEntry<>("three", "Gamma"));
        assertTrue(entrySet.removeAll(toRemove));
        assertEquals(1, map.size());
        assertEquals("Beta", map.get("Two"));
    }

    @Test
    void entrySetRemoveAllIgnoresNonEntryObjects() {
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        List<Object> mixed = List.of(null, "string", new AbstractMap.SimpleEntry<>("One", "Alpha"));
        assertTrue(entrySet.removeAll(mixed));
        assertEquals(2, map.size());
        assertNull(map.get("One"));
    }

    @Test
    void entrySetRemoveAllWithMismatchedKeyTypeIsIgnored() {
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        List<Object> bad = List.of(// Integer key, wrong type
        new AbstractMap.SimpleEntry<>(1, "One"));
        assertFalse(entrySet.removeAll(bad));
        assertEquals(3, map.size());
    }

    @Test
    void entrySetRemoveAllWithDuplicateEntriesRemovesOnlyOnce() {
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        Map.Entry<String, String> dup = new AbstractMap.SimpleEntry<>("One", "Alpha");
        List<Map.Entry<String, String>> dupList = List.of(dup, dup);
        assertTrue(entrySet.removeAll(dupList));
        assertEquals(2, map.size());
        assertNull(map.get("One"));
    }

    @Test
    void entrySetRemoveAllNullCollectionThrowsNPE() {
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        assertThrows(NullPointerException.class, () -> entrySet.removeAll(null));
    }

    @Test
    void entrySetRetainAllEmptyCollectionClearsMap() {
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        assertTrue(entrySet.retainAll(Collections.emptyList()));
        assertTrue(map.isEmpty());
    }

    @Test
    void entrySetRetainAllNoChangeReturnsFalse() {
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        List<Map.Entry<String, String>> retain = List.of(new AbstractMap.SimpleEntry<>("One", "Alpha"), new AbstractMap.SimpleEntry<>("Two", "Beta"), new AbstractMap.SimpleEntry<>("Three", "Gamma"));
        assertFalse(entrySet.retainAll(retain));
        assertEquals(3, map.size());
    }

    @Test
    void entrySetRetainAllCaseInsensitiveKeyMatches() {
        map.clear();
        map.put("Key1", "A");
        map.put("Key2", "B");
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        List<Map.Entry<String, String>> retain = List.of(new AbstractMap.SimpleEntry<>("key1", "A"));
        assertTrue(entrySet.retainAll(retain));
        assertEquals(1, map.size());
        assertEquals("A", map.get("Key1"));
        assertNull(map.get("Key2"));
    }

    @Test
    void entrySetRetainAllNullCollectionThrowsNPE() {
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        assertThrows(NullPointerException.class, () -> entrySet.retainAll(null));
    }

    @Test
    void entrySetToArrayEmptyMapReturnsEmptyArray() {
        map.clear();
        Object[] array = map.entrySet().toArray();
        assertEquals(0, array.length);
    }

    @Test
    @SuppressWarnings("unchecked")
    void entrySetToTypedArrayExactSizeUsesSuppliedArray() {
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        Map.Entry<String, String>[] supplied = (Map.Entry<String, String>[]) new Map.Entry[3];
        Map.Entry<String, String>[] result = entrySet.toArray(supplied);
        assertSame(supplied, result);
        assertEquals(3, result.length);
        assertNotNull(result[0]);
    }

    @Test
    @SuppressWarnings("unchecked")
    void entrySetToTypedArrayShorterArrayCreatesNewArray() {
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        Map.Entry<String, String>[] supplied = (Map.Entry<String, String>[]) new Map.Entry[2];
        Map.Entry<String, String>[] result = entrySet.toArray(supplied);
        assertNotSame(supplied, result);
        assertEquals(3, result.length);
    }

    @Test
    @SuppressWarnings("unchecked")
    void entrySetToTypedArrayLongerArrayHasTrailingNull() {
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        Map.Entry<String, String>[] supplied = (Map.Entry<String, String>[]) new Map.Entry[5];
        Map.Entry<String, String>[] result = entrySet.toArray(supplied);
        assertSame(supplied, result);
        assertEquals(5, result.length);
        // index == size should be null
        assertNull(result[3]);
    }

    @Test
    void entrySetToArrayNullArgumentThrowsNPE() {
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        assertThrows(NullPointerException.class, () -> entrySet.toArray(null));
    }

    @Test
    void entrySetToTypedArrayIncompatibleComponentThrowsArrayStoreException() {
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        String[] wrong = new String[3];
        assertThrows(ArrayStoreException.class, () -> entrySet.toArray(wrong));
    }

    @Test
    void entrySetIsLiveViewReflectsSubsequentModifications() {
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        map.put("Four", "Delta");
        assertTrue(entrySet.stream().anyMatch(e -> e.getKey().equals("Four")));
        assertEquals(4, entrySet.size());
    }
}
