package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_entrySet_Test {

    @Mock
    private ConcurrentMap<Object, Object> mockInternalMap;

    private TestMap testMap;

    /**
     * Concrete class used only for testing – it simply forwards the constructor.
     */
    private static class TestMap extends AbstractConcurrentNullSafeMap<String, Integer> {

        TestMap(ConcurrentMap<Object, Object> internalMap) {
            super(internalMap);
        }

        /**
         * expose the protected mask methods so we can build expected arguments for the mock
         */
        Object maskKey(String k) {
            return maskNullKey(k);
        }

        Object maskValue(Integer v) {
            return maskNullValue(v);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh instance for each test.
        testMap = new TestMap(mockInternalMap);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the mock to avoid cross‑test interference.
        reset(mockInternalMap);
    }

    private Map.Entry<String, Integer> entryFromSet(String key, Integer value) {
        // Helper that creates a TestMap with a single entry and returns that entry.
        ConcurrentMap<Object, Object> realMap = new ConcurrentHashMap<>();
        realMap.put(key, value);
        TestMap realTestMap = new TestMap(realMap);
        return realTestMap.entrySet().iterator().next();
    }

    @Test
    void entrySet_iterator_returnsUnmaskedEntries() {
        // GIVEN – a backing entry set containing a single entry ("k1","v1")
        Map.Entry<Object, Object> internalEntry = new AbstractMap.SimpleEntry<>("k1", "v1");
        Set<Map.Entry<Object, Object>> internalEntrySet = new HashSet<>(Collections.singleton(internalEntry));
        when(mockInternalMap.entrySet()).thenReturn(internalEntrySet);
        // WHEN – we obtain the iterator from the public entrySet view
        Iterator<Map.Entry<String, String>> it = testMap.entrySet().iterator();
        // THEN – iterator yields a correctly unmasked entry
        assertTrue(it.hasNext(), "Iterator should have one element");
        Map.Entry<String, String> e = it.next();
        assertEquals("k1", e.getKey());
        assertEquals("v1", e.getValue());
        // iterator's remove should delegate to the internal iterator's remove
        // no exception means delegation succeeded
        it.remove();
        verify(mockInternalMap.entrySet().iterator()).remove();
    }

    @Test
    void entrySet_sizeReflectsInternalSize() {
        // GIVEN
        Set<Map.Entry<Object, Object>> internalEntrySet = mock(Set.class);
        when(internalEntrySet.size()).thenReturn(42);
        when(mockInternalMap.entrySet()).thenReturn(internalEntrySet);
        // WHEN
        int size = testMap.entrySet().size();
        // THEN
        assertEquals(42, size);
        verify(internalEntrySet).size();
    }

    @Test
    void entrySet_contains_usesMaskingAndInternalMapLookup() {
        // GIVEN – internal map returns the masked value for the masked key
        Object maskedKey = testMap.maskKey("k2");
        Object maskedValue = testMap.maskValue("v2");
        when(mockInternalMap.get(maskedKey)).thenReturn(maskedValue);
        // WHEN – we ask the entrySet whether it contains the entry
        boolean contains = testMap.entrySet().contains(new AbstractMap.SimpleEntry<>("k2", "v2"));
        // THEN – it should be true because the internal map matches the masked pair
        assertTrue(contains);
        verify(mockInternalMap).get(maskedKey);
    }

    @Test
    void entrySet_remove_delegatesToInternalMapRemove() {
        // GIVEN – the internal map will successfully remove the masked pair
        Object maskedKey = testMap.maskKey("k3");
        Object maskedValue = testMap.maskValue("v3");
        when(mockInternalMap.remove(maskedKey, maskedValue)).thenReturn(true);
        // WHEN
        boolean removed = testMap.entrySet().remove(new AbstractMap.SimpleEntry<>("k3", "v3"));
        // THEN
        assertTrue(removed);
        verify(mockInternalMap).remove(maskedKey, maskedValue);
    }

    @Test
    void entrySet_clear_callsInternalMapClear() {
        // WHEN
        testMap.entrySet().clear();
        // THEN
        verify(mockInternalMap).clear();
    }

    @Test
    void entrySet_size_emptyMap() {
        // GIVEN an empty internal entry set
        Set<Map.Entry<Object, Object>> internalSet = mock(Set.class);
        when(internalSet.size()).thenReturn(0);
        when(mockInternalMap.entrySet()).thenReturn(internalSet);
        // WHEN
        int size = testMap.entrySet().size();
        // THEN
        assertEquals(0, size);
        verify(internalSet).size();
    }

    @Test
    void entrySet_size_twoElements() {
        // GIVEN an internal entry set reporting size 2
        Set<Map.Entry<Object, Object>> internalSet = mock(Set.class);
        when(internalSet.size()).thenReturn(2);
        when(mockInternalMap.entrySet()).thenReturn(internalSet);
        // WHEN
        int size = testMap.entrySet().size();
        // THEN
        assertEquals(2, size);
        verify(internalSet).size();
    }

    @Test
    void entrySet_iterator_next_unmaskedEntries() {
        // GIVEN a backing entry set with two entries (including nulls)
        Map.Entry<Object, Object> e1 = new AbstractMap.SimpleEntry<>("a", 1);
        Map.Entry<Object, Object> e2 = new AbstractMap.SimpleEntry<>(null, null);
        Set<Map.Entry<Object, Object>> internalSet = new LinkedHashSet<>(Arrays.asList(e1, e2));
        when(mockInternalMap.entrySet()).thenReturn(internalSet);
        // WHEN we iterate over the public view
        Iterator<Map.Entry<String, Integer>> it = testMap.entrySet().iterator();
        // THEN we receive the correctly unmasked entries (order preserved)
        assertTrue(it.hasNext());
        Map.Entry<String, Integer> first = it.next();
        assertEquals("a", first.getKey());
        assertEquals(Integer.valueOf(1), first.getValue());
        assertTrue(it.hasNext());
        Map.Entry<String, Integer> second = it.next();
        assertNull(second.getKey());
        assertNull(second.getValue());
        assertFalse(it.hasNext());
    }

    @Test
    void entrySet_iterator_hasNext_behavior() {
        // GIVEN a single element set
        Map.Entry<Object, Object> e = new AbstractMap.SimpleEntry<>("k", 10);
        Set<Map.Entry<Object, Object>> internalSet = Collections.singleton(e);
        when(mockInternalMap.entrySet()).thenReturn(internalSet);
        Iterator<Map.Entry<String, Integer>> it = testMap.entrySet().iterator();
        // first call – element present
        assertTrue(it.hasNext());
        // consume
        it.next();
        // second call – no more elements
        assertFalse(it.hasNext());
    }

    @Test
    void entrySet_iterator_next_noElements_throwsNoSuchElementException() {
        // GIVEN an empty internal set
        Set<Map.Entry<Object, Object>> internalSet = Collections.emptySet();
        when(mockInternalMap.entrySet()).thenReturn(internalSet);
        Iterator<Map.Entry<String, Integer>> it = testMap.entrySet().iterator();
        // THEN next() throws NoSuchElementException
        assertThrows(NoSuchElementException.class, it::next);
    }

    @Test
    void entrySet_iterator_remove_delegatesAndUpdatesSize() {
        // Use a real ConcurrentHashMap to verify removal semantics
        ConcurrentMap<Object, Object> realMap = new ConcurrentHashMap<>();
        realMap.put("k1", 1);
        realMap.put("k2", 2);
        TestMap realTestMap = new TestMap(realMap);
        Iterator<Map.Entry<String, Integer>> it = realTestMap.entrySet().iterator();
        // remove first element
        assertTrue(it.hasNext());
        it.next();
        it.remove();
        // size should be reduced to 1
        assertEquals(1, realTestMap.entrySet().size());
        assertFalse(realMap.containsKey("k1"));
    }

    @Test
    void entrySet_iterator_remove_withoutNext_throwsIllegalStateException() {
        // GIVEN a non‑empty set
        Map.Entry<Object, Object> e = new AbstractMap.SimpleEntry<>("x", 5);
        Set<Map.Entry<Object, Object>> internalSet = Collections.singleton(e);
        when(mockInternalMap.entrySet()).thenReturn(internalSet);
        Iterator<Map.Entry<String, Integer>> it = testMap.entrySet().iterator();
        // THEN calling remove before next() throws IllegalStateException
        assertThrows(IllegalStateException.class, it::remove);
    }

    @Test
    void entrySet_iterator_remove_twice_throwsIllegalStateException() {
        // Use a real map to get a concrete iterator that enforces the contract
        ConcurrentMap<Object, Object> realMap = new ConcurrentHashMap<>();
        realMap.put("a", 1);
        TestMap realTestMap = new TestMap(realMap);
        Iterator<Map.Entry<String, Integer>> it = realTestMap.entrySet().iterator();
        it.next();
        // first remove OK
        it.remove();
        // second remove without another next() should fail
        assertThrows(IllegalStateException.class, it::remove);
    }

    @Test
    void entrySet_iterator_concurrentModification_throwsCME() {
        // GIVEN a real ConcurrentHashMap (which detects concurrent modifications)
        ConcurrentMap<Object, Object> realMap = new ConcurrentHashMap<>();
        realMap.put("k", 1);
        TestMap realTestMap = new TestMap(realMap);
        Iterator<Map.Entry<String, Integer>> it = realTestMap.entrySet().iterator();
        // MODIFY the map after iterator creation
        realTestMap.put("new", 2);
        // THEN any subsequent iterator operation should throw CME
        assertThrows(ConcurrentModificationException.class, it::hasNext);
    }

    @Test
    void entrySet_contains_nonEntry_returnsFalse() {
        assertFalse(testMap.entrySet().contains("just a string"));
    }

    @Test
    void entrySet_contains_matchingEntry_returnsTrue() {
        Object maskedKey = testMap.maskKey("a");
        Object maskedValue = testMap.maskValue(1);
        when(mockInternalMap.get(maskedKey)).thenReturn(maskedValue);
        assertTrue(testMap.entrySet().contains(new AbstractMap.SimpleEntry<>("a", 1)));
        verify(mockInternalMap).get(maskedKey);
    }

    @Test
    void entrySet_contains_matchingNullEntry_returnsTrue() {
        Object maskedKey = testMap.maskKey(null);
        Object maskedValue = testMap.maskValue(null);
        when(mockInternalMap.get(maskedKey)).thenReturn(maskedValue);
        assertTrue(testMap.entrySet().contains(new AbstractMap.SimpleEntry<>(null, null)));
        verify(mockInternalMap).get(maskedKey);
    }

    @Test
    void entrySet_contains_nonMatchingValue_returnsFalse() {
        Object maskedKey = testMap.maskKey("a");
        // internal map holds a different value
        when(mockInternalMap.get(maskedKey)).thenReturn(testMap.maskValue(99));
        assertFalse(testMap.entrySet().contains(new AbstractMap.SimpleEntry<>("a", 1)));
    }

    @Test
    void entrySet_contains_missingKey_returnsFalse() {
        Object maskedKey = testMap.maskKey("missing");
        // no entry
        when(mockInternalMap.get(maskedKey)).thenReturn(null);
        assertFalse(testMap.entrySet().contains(new AbstractMap.SimpleEntry<>("missing", 1)));
    }

    @Test
    void entrySet_contains_nullKeyNonNullValue_returnsTrue() {
        Object maskedKey = testMap.maskKey(null);
        Object maskedValue = testMap.maskValue(5);
        when(mockInternalMap.get(maskedKey)).thenReturn(maskedValue);
        assertTrue(testMap.entrySet().contains(new AbstractMap.SimpleEntry<>(null, 5)));
    }

    @Test
    void entrySet_contains_wrongKeyType_throwsClassCastException() {
        // The entry's key is an Integer, but the map expects String keys.
        assertThrows(ClassCastException.class, () -> testMap.entrySet().contains(new AbstractMap.SimpleEntry<>(123, 1)));
    }

    @Test
    void entrySet_contains_wrongValueType_throwsClassCastException() {
        // The entry's value is a String, but the map expects Integer values.
        assertThrows(ClassCastException.class, () -> testMap.entrySet().contains(new AbstractMap.SimpleEntry<>("a", "one")));
    }

    @Test
    void entrySet_remove_nonEntry_returnsFalse() {
        assertFalse(testMap.entrySet().remove(42));
    }

    @Test
    void entrySet_remove_matchingEntry_returnsTrue() {
        Object maskedKey = testMap.maskKey("a");
        Object maskedValue = testMap.maskValue(1);
        when(mockInternalMap.remove(maskedKey, maskedValue)).thenReturn(true);
        assertTrue(testMap.entrySet().remove(new AbstractMap.SimpleEntry<>("a", 1)));
        verify(mockInternalMap).remove(maskedKey, maskedValue);
    }

    @Test
    void entrySet_remove_nullEntry_returnsTrue() {
        Object maskedKey = testMap.maskKey(null);
        Object maskedValue = testMap.maskValue(null);
        when(mockInternalMap.remove(maskedKey, maskedValue)).thenReturn(true);
        assertTrue(testMap.entrySet().remove(new AbstractMap.SimpleEntry<>(null, null)));
        verify(mockInternalMap).remove(maskedKey, maskedValue);
    }

    @Test
    void entrySet_remove_wrongKeyType_throwsClassCastException() {
        assertThrows(ClassCastException.class, () -> testMap.entrySet().remove(new AbstractMap.SimpleEntry<>(true, 1)));
    }

    @Test
    void entrySet_remove_wrongValueType_throwsClassCastException() {
        assertThrows(ClassCastException.class, () -> testMap.entrySet().remove(new AbstractMap.SimpleEntry<>("a", new Date())));
    }

    @Test
    void entrySet_clear_delegatesToInternalMap() {
        testMap.entrySet().clear();
        verify(mockInternalMap).clear();
    }

    @Test
    void entry_equals_nonEntry_returnsFalse() {
        Map.Entry<String, Integer> e = entryFromSet("a", 1);
        assertFalse(e.equals("not an entry"));
    }

    @Test
    void entry_equals_matchingNulls_returnsTrue() {
        Map.Entry<String, Integer> e = entryFromSet(null, null);
        assertTrue(e.equals(new AbstractMap.SimpleEntry<>(null, null)));
    }

    @Test
    void entry_equals_sameKeyDifferentValue_returnsFalse() {
        Map.Entry<String, Integer> e = entryFromSet("a", 1);
        assertFalse(e.equals(new AbstractMap.SimpleEntry<>("a", 2)));
    }

    @Test
    void entry_equals_differentKeySameValue_returnsFalse() {
        Map.Entry<String, Integer> e = entryFromSet("a", 1);
        assertFalse(e.equals(new AbstractMap.SimpleEntry<>("b", 1)));
    }

    @Test
    void entry_hashCode_nonNulls() {
        Map.Entry<String, Integer> e = entryFromSet("a", 1);
        int expected = Objects.hashCode("a") ^ Objects.hashCode(1);
        assertEquals(expected, e.hashCode());
    }

    @Test
    void entry_hashCode_nulls() {
        Map.Entry<String, Integer> e = entryFromSet(null, null);
        // 0 ^ 0
        int expected = 0;
        assertEquals(expected, e.hashCode());
    }

    @Test
    void entry_toString_nonNulls() {
        Map.Entry<String, Integer> e = entryFromSet("a", 1);
        assertEquals("a=1", e.toString());
    }

    @Test
    void entry_toString_nulls() {
        Map.Entry<String, Integer> e = entryFromSet(null, null);
        assertEquals("null=null", e.toString());
    }

    @Test
    void entry_setValue_updatesUnderlyingMap() {
        // Mock an internal entry to control setValue behavior
        Map.Entry<Object, Object> internalEntry = mock(Map.Entry.class);
        when(internalEntry.getKey()).thenReturn("a");
        when(internalEntry.getValue()).thenReturn(1);
        // Simulate old value being masked 1, new value masked 42
        when(internalEntry.setValue(testMap.maskValue(42))).thenReturn(testMap.maskValue(1));
        // Build a Set view that will wrap this mocked internal entry
        Set<Map.Entry<Object, Object>> internalSet = Collections.singleton(internalEntry);
        when(mockInternalMap.entrySet()).thenReturn(internalSet);
        // Obtain the public entry
        Map.Entry<String, Integer> entry = testMap.entrySet().iterator().next();
        // Perform setValue
        Integer old = entry.setValue(42);
        // Verify old value is unmasked and map now contains new value
        assertEquals(Integer.valueOf(1), old);
        verify(internalEntry).setValue(testMap.maskValue(42));
    }

    @Test
    void entry_setValue_nullKeyAndValue() {
        // Mock internal entry with null key/value
        Map.Entry<Object, Object> internalEntry = mock(Map.Entry.class);
        when(internalEntry.getKey()).thenReturn(null);
        when(internalEntry.getValue()).thenReturn(null);
        when(internalEntry.setValue(testMap.maskValue(99))).thenReturn(testMap.maskValue(null));
        Set<Map.Entry<Object, Object>> internalSet = Collections.singleton(internalEntry);
        when(mockInternalMap.entrySet()).thenReturn(internalSet);
        Map.Entry<String, Integer> entry = testMap.entrySet().iterator().next();
        Integer old = entry.setValue(99);
        assertNull(old);
        verify(internalEntry).setValue(testMap.maskValue(99));
    }
}
