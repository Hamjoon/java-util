package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.concurrent.ConcurrentModificationException;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveMap_keySet_Test {

    private CaseInsensitiveMap<String, Integer> map;

    private Set<String> keySet;

    @BeforeEach
    void setupBeforeEach() {
        map = new CaseInsensitiveMap<>();
        map.put("One", 1);
        map.put("Two", 2);
        // mixed case key
        map.put("three", 3);
        keySet = map.keySet();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map after each test to avoid cross‑test contamination
        map.clear();
    }

    @Test
    void keySetContainsIsCaseInsensitive() {
        assertTrue(keySet.contains("ONE"));
        assertTrue(keySet.contains("two"));
        assertTrue(keySet.contains("ThReE"));
        assertFalse(keySet.contains("four"));
    }

    @Test
    void keySetSizeReflectsUnderlyingMap() {
        assertEquals(3, keySet.size());
        map.put("Four", 4);
        assertEquals(4, keySet.size());
    }

    @Test
    void keySetClearEmptiesMap() {
        assertFalse(map.isEmpty());
        keySet.clear();
        assertTrue(map.isEmpty());
        assertTrue(keySet.isEmpty());
    }

    @Test
    void keySetRemoveByKeyIsCaseInsensitive() {
        boolean changed = keySet.remove("ONE");
        assertTrue(changed);
        assertEquals(2, keySet.size());
        assertFalse(map.containsKey("One"));
        // Removing a non‑existing key should return false and not change size
        assertFalse(keySet.remove("nonexistent"));
        assertEquals(2, keySet.size());
    }

    @Test
    void keySetRemoveAllRemovesMatchingKeysCaseInsensitively() {
        Collection<String> toRemove = Arrays.asList("two", "FOUR");
        boolean changed = keySet.removeAll(toRemove);
        assertTrue(changed);
        // "Four" was not present, only "Two" removed
        assertEquals(2, keySet.size());
        assertFalse(keySet.contains("Two"));
        assertTrue(keySet.contains("One"));
        assertTrue(keySet.contains("three"));
    }

    @Test
    void keySetRetainAllKeepsOnlySpecifiedKeysCaseInsensitively() {
        Collection<String> toKeep = Arrays.asList("three", "ONE");
        boolean changed = keySet.retainAll(toKeep);
        assertTrue(changed);
        assertEquals(2, keySet.size());
        assertTrue(keySet.contains("One"));
        assertTrue(keySet.contains("three"));
        assertFalse(keySet.contains("Two"));
    }

    @Test
    void keySetToArrayReturnsOriginalCaseKeys() {
        Object[] array = keySet.toArray();
        List<Object> list = Arrays.asList(array);
        assertTrue(list.containsAll(Arrays.asList("One", "Two", "three")));
        // Ensure the case is preserved
        assertTrue(list.stream().anyMatch(o -> "One".equals(o)));
        assertTrue(list.stream().anyMatch(o -> "Two".equals(o)));
        assertTrue(list.stream().anyMatch(o -> "three".equals(o)));
    }

    @Test
    void keySetHashCodeMatchesSumOfKeyHashCodes() {
        int expected = 0;
        for (String k : map.keySet()) {
            expected += k.hashCode();
        }
        assertEquals(expected, keySet.hashCode());
    }

    @Test
    void iteratorProvidesOriginalCaseKeysAndSupportsRemove() {
        Iterator<String> it = keySet.iterator();
        List<String> iterated = new ArrayList<>();
        while (it.hasNext()) {
            String k = it.next();
            iterated.add(k);
            if ("Two".equals(k)) {
                // remove via iterator
                it.remove();
            }
        }
        assertEquals(Arrays.asList("One", "Two", "three"), iterated);
        assertFalse(map.containsKey("Two"));
        assertEquals(2, map.size());
    }

    @Test
    void iteratorNextConvertsCaseInsensitiveStringToPlainString() {
        // Insert a key that will be wrapped internally as CaseInsensitiveString
        map.put("MiXeD", 5);
        Set<String> ks = map.keySet();
        Iterator<String> it = ks.iterator();
        boolean foundMixed = false;
        while (it.hasNext()) {
            String k = it.next();
            if ("MiXeD".equals(k)) {
                foundMixed = true;
                // The iterator should have returned the original String, not the internal wrapper
                assertEquals("MiXeD", k);
            }
        }
        assertTrue(foundMixed);
    }

    // ----- keySet_contains (null handling) -----
    @Test
    void keySetContainsNull_whenNullKeyPresent_returnsTrue() {
        map.clear();
        map.put(null, 0);
        Set<String> ks = map.keySet();
        assertTrue(ks.contains(null));
    }

    @Test
    void keySetContainsNull_whenNoNullKey_returnsFalse() {
        // ensure map is empty (no null key)
        map.clear();
        Set<String> ks = map.keySet();
        assertFalse(ks.contains(null));
    }

    // ----- keySet_remove (null handling) -----
    @Test
    void keySetRemoveNull_whenNullKeyPresent_removesAndReturnsTrue() {
        map.clear();
        map.put(null, 42);
        Set<String> ks = map.keySet();
        boolean changed = ks.remove(null);
        assertTrue(changed);
        assertFalse(map.containsKey(null));
        assertEquals(0, ks.size());
    }

    @Test
    void keySetRemoveNull_whenNoNullKey_returnsFalse() {
        // no null key
        map.clear();
        Set<String> ks = map.keySet();
        boolean changed = ks.remove(null);
        assertFalse(changed);
        assertEquals(0, ks.size());
    }

    // ----- keySet_removeAll with null and duplicates -----
    @Test
    void keySetRemoveAllWithNullAndDuplicates_removesCorrectly() {
        map.clear();
        map.put("A", 1);
        map.put("B", 2);
        map.put(null, 3);
        map.put("C", 4);
        Set<String> ks = map.keySet();
        Collection<Object> toRemove = Arrays.asList(null, "a", null, "A");
        boolean changed = ks.removeAll(toRemove);
        assertTrue(changed);
        assertEquals(2, ks.size());
        assertTrue(ks.containsAll(Arrays.asList("B", "C")));
        assertFalse(ks.contains("A"));
        assertFalse(ks.contains(null));
    }

    @Test
    void keySetRemoveAllWithEmptyCollection_returnsFalseAndNoChange() {
        map.clear();
        map.put("X", 1);
        map.put("Y", 2);
        Set<String> ks = map.keySet();
        boolean changed = ks.removeAll(Collections.emptyList());
        assertFalse(changed);
        assertEquals(2, ks.size());
    }

    // ----- keySet_retainAll with null -----
    @Test
    void keySetRetainAllWithNull_keepsNullAndMatchingKey() {
        map.clear();
        map.put(null, 0);
        map.put("Alpha", 1);
        map.put("Beta", 2);
        Set<String> ks = map.keySet();
        Collection<Object> toKeep = Arrays.asList(null, "alpha");
        boolean changed = ks.retainAll(toKeep);
        assertTrue(changed);
        assertEquals(2, ks.size());
        assertTrue(ks.contains(null));
        assertTrue(ks.contains("Alpha"));
        assertFalse(ks.contains("Beta"));
    }

    @Test
    void keySetRetainAllWithAllKeysPresent_returnsFalseNoChange() {
        map.clear();
        map.put("One", 1);
        map.put("Two", 2);
        Set<String> ks = map.keySet();
        Collection<Object> toKeep = Arrays.asList("one", "two", null);
        boolean changed = ks.retainAll(toKeep);
        assertFalse(changed);
        assertEquals(2, ks.size());
    }

    // ----- keySet_hashCode_empty -----
    @Test
    void keySetHashCodeWhenEmpty_returnsZero() {
        map.clear();
        Set<String> ks = map.keySet();
        assertEquals(0, ks.hashCode());
    }

    // ----- iterator exceptions -----
    @Test
    void iteratorNextOnEmptyMap_throwsNoSuchElementException() {
        map.clear();
        Set<String> ks = map.keySet();
        Iterator<String> it = ks.iterator();
        assertThrows(NoSuchElementException.class, it::next);
    }

    @Test
    void iteratorRemoveBeforeNext_throwsIllegalStateException() {
        Set<String> ks = map.keySet();
        Iterator<String> it = ks.iterator();
        assertThrows(IllegalStateException.class, it::remove);
    }

    @Test
    void iteratorDoubleRemove_throwsIllegalStateException() {
        Set<String> ks = map.keySet();
        Iterator<String> it = ks.iterator();
        assertTrue(it.hasNext());
        // first element
        it.next();
        // first removal – OK
        it.remove();
        assertThrows(IllegalStateException.class, it::remove);
    }

    @Test
    void iteratorConcurrentModification_throwsConcurrentModificationException() {
        Set<String> ks = map.keySet();
        Iterator<String> it = ks.iterator();
        // modify map directly after iterator creation
        map.put("newKey", 99);
        assertThrows(ConcurrentModificationException.class, it::next);
    }

    // ----- iterator remove multiple -----
    @Test
    void iteratorRemoveMultipleLeavesMapEmpty() {
        map.clear();
        map.put("A", 1);
        map.put("B", 2);
        map.put("C", 3);
        Set<String> ks = map.keySet();
        Iterator<String> it = ks.iterator();
        while (it.hasNext()) {
            it.next();
            it.remove();
        }
        assertTrue(map.isEmpty());
        assertEquals(0, ks.size());
        assertFalse(it.hasNext());
    }

    // ----- toArray type and length -----
    @Test
    void keySetToArrayReturnsCorrectLengthAndStringElements() {
        map.clear();
        map.put("First", 1);
        map.put("Second", 2);
        map.put("Third", 3);
        Set<String> ks = map.keySet();
        Object[] arr = ks.toArray();
        assertEquals(3, arr.length);
        // Ensure every element is a plain String (not the internal wrapper)
        for (Object o : arr) {
            assertTrue(o instanceof String);
        }
        List<String> asList = Arrays.asList((String[]) ks.toArray(new String[0]));
        assertTrue(asList.containsAll(Arrays.asList("First", "Second", "Third")));
    }

    // ----- retainAll class‑cast -----
    @Test
    void keySetRetainAllWithIncompatibleType_throwsClassCastException() {
        map.clear();
        map.put("X", 1);
        map.put("Y", 2);
        Set<String> ks = map.keySet();
        Collection<Object> badCollection = Arrays.asList(42, "X");
        assertThrows(ClassCastException.class, () -> ks.retainAll(badCollection));
    }

    // ----- removeAll class‑cast -----
    @Test
    void keySetRemoveAllWithIncompatibleType_throwsClassCastException() {
        map.clear();
        map.put("P", 1);
        map.put("Q", 2);
        Set<String> ks = map.keySet();
        Collection<Object> badCollection = Arrays.asList(3.14, "P");
        assertThrows(ClassCastException.class, () -> ks.removeAll(badCollection));
    }
}
