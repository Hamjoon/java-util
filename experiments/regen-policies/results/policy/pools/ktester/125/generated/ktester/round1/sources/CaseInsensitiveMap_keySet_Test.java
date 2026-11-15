package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicInteger;

class CaseInsensitiveMap_keySet_Test {

    private CaseInsensitiveMap<String, String> map;

    private Set<String> keySet;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a map with a mixture of keys (different cases) and values
        map = new CaseInsensitiveMap<>();
        map.put("One", "1");
        map.put("Two", "2");
        map.put("Three", "3");
        // upper‑case key
        map.put("FOUR", "4");
        // mixed case key
        map.put("fIvE", "5");
        keySet = map.keySet();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map after each test to avoid cross‑test contamination
        map.clear();
    }

    @Test
    void keySetSizeMatchesMapSize() {
        assertEquals(map.size(), keySet.size(), "keySet().size() should reflect the underlying map size");
    }

    @Test
    void keySetContainsIsCaseInsensitive() {
        assertTrue(keySet.contains("one"));
        assertTrue(keySet.contains("ONE"));
        assertTrue(keySet.contains("tWo"));
        assertTrue(keySet.contains("four"));
        assertTrue(keySet.contains("FIVE"));
        assertFalse(keySet.contains("six"));
    }

    @Test
    void iteratorReturnsOriginalKeyCasing() {
        List<String> iterated = new ArrayList<>();
        for (String k : keySet) {
            iterated.add(k);
        }
        // The original case should be preserved
        assertTrue(iterated.contains("One"));
        assertTrue(iterated.contains("Two"));
        assertTrue(iterated.contains("Three"));
        assertTrue(iterated.contains("FOUR"));
        assertTrue(iterated.contains("fIvE"));
    }

    @Test
    void removeViaKeySetUpdatesUnderlyingMap() {
        // different case
        boolean removed = keySet.remove("ONE");
        assertTrue(removed, "remove should succeed for a case‑insensitive match");
        assertFalse(map.containsKey("One"));
        assertEquals(4, map.size());
        // Removing a non‑present key should return false and not affect the map
        assertFalse(keySet.remove("nonexistent"));
        assertEquals(4, map.size());
    }

    @Test
    void toArrayReturnsOriginalKeys() {
        String[] array = keySet.toArray(new String[0]);
        // Verify length
        assertEquals(5, array.length);
        // Verify that each original key appears exactly once
        List<String> asList = Arrays.asList(array);
        assertTrue(asList.containsAll(Arrays.asList("One", "Two", "Three", "FOUR", "fIvE")));
    }

    @Test
    void hashCodeIsSumOfUnderlyingKeyHashCodes() {
        int expected = 0;
        for (String k : map.keySet()) {
            // CaseInsensitiveString hashCode is already case‑insensitive
            expected += k.hashCode();
        }
        assertEquals(expected, keySet.hashCode(), "keySet().hashCode() should be the sum of the underlying key hash codes");
    }

    @Test
    void retainAllKeepsOnlySpecifiedKeysCaseInsensitively() {
        // Retain only "one" and "FIVE" (different cases than stored)
        Collection<String> toRetain = Arrays.asList("ONE", "five");
        boolean changed = keySet.retainAll(toRetain);
        assertTrue(changed, "retainAll should report a change when entries are removed");
        assertEquals(2, map.size());
        assertTrue(map.containsKey("One"));
        assertTrue(map.containsKey("fIvE"));
        assertFalse(map.containsKey("Two"));
        assertFalse(map.containsKey("Three"));
        assertFalse(map.containsKey("FOUR"));
    }

    // -----------------------------------------------------------------
    @Test
    void iteratorReturnsOriginalStringAndNonStringKey() {
        CaseInsensitiveMap<Object, Integer> mixedMap = new CaseInsensitiveMap<>();
        // stored internally as CaseInsensitiveString
        mixedMap.put("TeSt", 1);
        // non‑String key
        mixedMap.put(42, 2);
        Iterator<Object> it = mixedMap.keySet().iterator();
        Object first = it.next();
        assertEquals("TeSt", first, "Iterator should return the original String key");
        Object second = it.next();
        assertEquals(42, second, "Iterator should return non‑String key unchanged");
    }

    @Test
    void iteratorHasNextBecomesFalseAfterConsumingAllElements() {
        Iterator<String> it = keySet.iterator();
        while (it.hasNext()) {
            // consume
            it.next();
        }
        assertFalse(it.hasNext(), "hasNext() must be false after all elements are consumed");
        assertFalse(it.hasNext(), "Subsequent calls to hasNext() should continue to return false");
    }

    // -----------------------------------------------------------------
    @Test
    void hashCodeHandlesNullKeyAndEmptyMap() {
        CaseInsensitiveMap<String, String> mapWithNull = new CaseInsensitiveMap<>();
        mapWithNull.put(null, "nullValue");
        mapWithNull.put("Abc", "x");
        mapWithNull.put("DEF", "y");
        int expected = "Abc".toLowerCase().hashCode() + "DEF".toLowerCase().hashCode();
        assertEquals(expected, mapWithNull.keySet().hashCode(), "Null key should contribute 0 to the hash sum");
        // empty map
        CaseInsensitiveMap<String, String> empty = new CaseInsensitiveMap<>();
        assertEquals(0, empty.keySet().hashCode(), "Empty map should have hash code 0");
    }

    // -----------------------------------------------------------------
    @Test
    void sizeReturnsNumberOfEntries() {
        CaseInsensitiveMap<String, String> m = new CaseInsensitiveMap<>();
        m.put("A", "1");
        m.put("B", "2");
        m.put("C", "3");
        assertEquals(3, m.keySet().size());
    }

    @Test
    void containsWorksCaseInsensitivelyAndForMissingKeys() {
        CaseInsensitiveMap<String, String> m = new CaseInsensitiveMap<>();
        m.put("Hello", "world");
        Set<String> ks = m.keySet();
        assertTrue(ks.contains("hello"));
        assertTrue(ks.contains("HELLO"));
        assertFalse(ks.contains("Goodbye"));
    }

    // -----------------------------------------------------------------
    @Test
    void removeReturnsTrueWhenElementExisted() {
        CaseInsensitiveMap<String, Integer> m = new CaseInsensitiveMap<>();
        m.put("RemoveMe", 1);
        assertTrue(m.keySet().remove("removeme"));
        assertEquals(0, m.size());
    }

    @Test
    void removeReturnsFalseWhenElementDidNotExist() {
        CaseInsensitiveMap<String, Integer> m = new CaseInsensitiveMap<>();
        assertFalse(m.keySet().remove("nonexistent"));
        assertEquals(0, m.size());
    }

    // -----------------------------------------------------------------
    @Test
    void toArrayFillsSuppliedLargeArrayAndAddsNullSentinel() {
        CaseInsensitiveMap<String, String> m = new CaseInsensitiveMap<>();
        m.put("A", "a");
        m.put("B", "b");
        String[] supplied = new String[5];
        String[] result = m.keySet().toArray(supplied);
        assertSame(supplied, result, "When array is large enough, the same instance should be returned");
        List<String> content = Arrays.asList(result);
        assertTrue(content.containsAll(Arrays.asList("A", "B")));
        assertNull(result[2], "Element after the last entry must be null sentinel");
        assertNull(result[3]);
        assertNull(result[4]);
    }

    @Test
    void toArrayCreatesNewArrayWhenSuppliedIsTooSmall() {
        CaseInsensitiveMap<Integer, String> m = new CaseInsensitiveMap<>();
        m.put(1, "one");
        m.put(2, "two");
        m.put(3, "three");
        Integer[] supplied = new Integer[2];
        Integer[] result = m.keySet().toArray(supplied);
        assertNotSame(supplied, result, "A new array should be allocated when supplied array is too small");
        assertEquals(3, result.length);
        List<Integer> list = Arrays.asList(result);
        assertTrue(list.containsAll(Arrays.asList(1, 2, 3)));
    }

    @Test
    void toArrayThrowsArrayStoreExceptionForIncompatibleArray() {
        CaseInsensitiveMap<String, String> m = new CaseInsensitiveMap<>();
        m.put("Only", "value");
        assertThrows(ArrayStoreException.class, () -> m.keySet().toArray(new Integer[1]), "Incompatible array type must cause ArrayStoreException");
    }

    @Test
    void toArrayThrowsNullPointerExceptionWhenArrayIsNull() {
        CaseInsensitiveMap<String, String> m = new CaseInsensitiveMap<>();
        m.put("key", "value");
        assertThrows(NullPointerException.class, () -> m.keySet().toArray(null), "Passing null to toArray should throw NullPointerException");
    }

    @Test
    void toArrayExactSizeNoTrailingNull() {
        CaseInsensitiveMap<String, String> m = new CaseInsensitiveMap<>();
        m.put("X", "x");
        m.put("Y", "y");
        String[] supplied = new String[2];
        String[] result = m.keySet().toArray(supplied);
        assertSame(supplied, result);
        assertEquals(2, result.length);
        List<String> list = Arrays.asList(result);
        assertTrue(list.containsAll(Arrays.asList("X", "Y")));
        assertFalse(Arrays.asList(result).contains(null), "No null sentinel when array length equals size");
    }

    @Test
    void toArrayEmptySuppliedCreatesNewArray() {
        CaseInsensitiveMap<Integer, String> m = new CaseInsensitiveMap<>();
        m.put(1, "one");
        m.put(2, "two");
        m.put(3, "three");
        Integer[] supplied = new Integer[0];
        Integer[] result = m.keySet().toArray(supplied);
        assertNotSame(supplied, result);
        assertEquals(3, result.length);
        List<Integer> list = Arrays.asList(result);
        assertTrue(list.containsAll(Arrays.asList(1, 2, 3)));
    }

    @Test
    void toArrayWithSupertypeArrayPreservesOriginalStrings() {
        CaseInsensitiveMap<String, String> m = new CaseInsensitiveMap<>();
        m.put("foo", "v1");
        m.put("bar", "v2");
        Object[] supplied = new Object[5];
        Object[] result = m.keySet().toArray(supplied);
        assertSame(supplied, result);
        List<Object> list = Arrays.asList(result);
        assertTrue(list.containsAll(Arrays.asList("foo", "bar")));
        assertNull(result[2], "Null sentinel should be placed after the last element");
        assertNull(result[3]);
        assertNull(result[4]);
    }

    // -----------------------------------------------------------------
    @Test
    void retainAllRemovesNonMatchingElementsAndReturnsTrue() {
        CaseInsensitiveMap<String, String> m = new CaseInsensitiveMap<>();
        m.put("one", "1");
        m.put("two", "2");
        m.put("three", "3");
        boolean changed = m.keySet().retainAll(Arrays.asList("ONE", "THREE"));
        assertTrue(changed);
        assertEquals(2, m.size());
        assertTrue(m.containsKey("one"));
        assertTrue(m.containsKey("three"));
        assertFalse(m.containsKey("two"));
    }

    @Test
    void retainAllUnchangedWhenAllElementsMatchReturnsFalse() {
        CaseInsensitiveMap<String, String> m = new CaseInsensitiveMap<>();
        m.put("a", "1");
        m.put("b", "2");
        boolean changed = m.keySet().retainAll(Arrays.asList("A", "B"));
        assertFalse(changed);
        assertEquals(2, m.size());
    }

    @Test
    void retainAllThrowsClassCastExceptionWhenElementCannotBeCastedToKeyType() {
        CaseInsensitiveMap<Integer, String> intMap = new CaseInsensitiveMap<>();
        intMap.put(1, "one");
        assertThrows(ClassCastException.class, () -> intMap.keySet().retainAll(Arrays.asList("string")), "RetainAll with incompatible element type must throw ClassCastException");
    }

    @Test
    void retainAllWithEmptyCollectionClearsSetAndReturnsTrueIfNonEmpty() {
        CaseInsensitiveMap<String, String> m = new CaseInsensitiveMap<>();
        m.put("a", "1");
        m.put("b", "2");
        m.put("c", "3");
        boolean changed = m.keySet().retainAll(Collections.emptyList());
        assertTrue(changed);
        assertEquals(0, m.size());
        assertTrue(m.keySet().isEmpty());
    }

    @Test
    void retainAllWithEmptyCollectionOnEmptyMapReturnsFalse() {
        CaseInsensitiveMap<String, String> empty = new CaseInsensitiveMap<>();
        boolean changed = empty.keySet().retainAll(Collections.emptyList());
        assertFalse(changed);
        assertTrue(empty.keySet().isEmpty());
    }

    @Test
    void retainAllWithNullElementKeepsOnlyNullKey() {
        CaseInsensitiveMap<String, String> m = new CaseInsensitiveMap<>();
        m.put(null, "nullVal");
        m.put("present", "p");
        m.put("alsoPresent", "ap");
        boolean changed = m.keySet().retainAll(Arrays.asList((Object) null));
        assertTrue(changed);
        assertEquals(1, m.size());
        assertTrue(m.containsKey(null));
    }

    @Test
    void retainAllNullArgumentThrowsNullPointerException() {
        CaseInsensitiveMap<String, String> m = new CaseInsensitiveMap<>();
        m.put("x", "1");
        assertThrows(NullPointerException.class, () -> m.keySet().retainAll(null), "Calling retainAll(null) must throw NullPointerException");
    }

    // -----------------------------------------------------------------
    @Test
    void hashCodeIsIdenticalRegardlessOfKeyCase() {
        CaseInsensitiveMap<String, Integer> map1 = new CaseInsensitiveMap<>();
        map1.put("Case", 1);
        CaseInsensitiveMap<String, Integer> map2 = new CaseInsensitiveMap<>();
        map2.put("cAsE", 2);
        // case‑insensitive hash
        int expected = "case".hashCode();
        assertEquals(expected, map1.keySet().hashCode(), "Hash code for map1 should equal case‑insensitive hash");
        assertEquals(expected, map2.keySet().hashCode(), "Hash code for map2 should equal case‑insensitive hash");
    }

    // -----------------------------------------------------------------
    @Test
    void iteratorOnEmptyMapHasNoElementsAndNextThrowsNoSuchElementException() {
        CaseInsensitiveMap<String, String> empty = new CaseInsensitiveMap<>();
        Iterator<String> it = empty.keySet().iterator();
        assertFalse(it.hasNext(), "Iterator on empty map must report no next element");
        assertThrows(NoSuchElementException.class, it::next, "Calling next() on empty iterator must throw NoSuchElementException");
    }

    @Test
    void iteratorRemoveBeforeNextThrowsIllegalStateException() {
        Iterator<String> it = keySet.iterator();
        assertThrows(IllegalStateException.class, it::remove, "Calling remove() before next() must throw IllegalStateException");
    }

    @Test
    void iteratorRemoveTwiceWithoutNextThrowsIllegalStateException() {
        Iterator<String> it = keySet.iterator();
        // first element
        it.next();
        // first removal succeeds
        it.remove();
        assertThrows(IllegalStateException.class, it::remove, "Second consecutive remove() without intervening next() must throw IllegalStateException");
    }

    @Test
    void iteratorRemoveDeletesCurrentElementFromBackingMap() {
        CaseInsensitiveMap<String, Integer> m = new CaseInsensitiveMap<>();
        m.put("Alpha", 1);
        m.put("Beta", 2);
        Iterator<String> it = m.keySet().iterator();
        assertEquals("Alpha", it.next());
        it.remove();
        assertEquals(1, m.size());
        assertFalse(m.keySet().contains("Alpha"));
        assertTrue(m.keySet().contains("Beta"));
    }

    // -----------------------------------------------------------------
    @Test
    void containsNullReturnsTrueWhenNullKeyPresent() {
        CaseInsensitiveMap<String, String> m = new CaseInsensitiveMap<>();
        m.put(null, "nullValue");
        assertTrue(m.keySet().contains(null));
    }

    @Test
    void containsNullReturnsFalseWhenNoNullKey() {
        assertFalse(keySet.contains(null));
    }

    @Test
    void containsNonStringKeyWorksWithoutCaseConversion() {
        CaseInsensitiveMap<Object, String> mixed = new CaseInsensitiveMap<>();
        mixed.put(100, "hundred");
        assertTrue(mixed.keySet().contains(100));
        assertFalse(mixed.keySet().contains("100"));
    }

    // -----------------------------------------------------------------
    @Test
    void removeNullDeletesNullKeyAndReturnsTrue() {
        CaseInsensitiveMap<String, String> m = new CaseInsensitiveMap<>();
        m.put(null, "nullValue");
        assertTrue(m.keySet().remove(null));
        assertEquals(0, m.size());
        assertFalse(m.keySet().contains(null));
    }

    @Test
    void removeNullReturnsFalseWhenNullKeyAbsent() {
        assertFalse(keySet.remove(null));
        assertEquals(5, map.size());
    }
}
