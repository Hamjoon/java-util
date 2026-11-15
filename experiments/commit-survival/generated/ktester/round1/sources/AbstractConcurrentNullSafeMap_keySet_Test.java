package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_keySet_Test {

    @Mock
    private ConcurrentMap<Object, Object> // mocked backing map
    internalMap;

    // instance under test
    private TestMap testMap;

    // for manual MockitoAnnotations init (optional)
    private AutoCloseable mocks;

    /**
     * Concrete subclass that bypasses the null‑masking logic for simplicity.
     */
    private static final class TestMap extends AbstractConcurrentNullSafeMap<String, Integer> {

        TestMap(ConcurrentMap<Object, Object> internalMap) {
            super(internalMap);
        }

        @Override
        protected Object maskNullKey(String key) {
            // identity – no sentinel handling
            return key;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected String unmaskNullKey(Object key) {
            // identity conversion
            return (String) key;
        }

        @Override
        protected Object maskNullValue(Integer value) {
            return value;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected Integer unmaskNullValue(Object value) {
            return (Integer) value;
        }
    }

    /**
     * Subclass that deliberately sets the internal map to {@code null}
     *  to provoke a NullPointerException when the keySet view accesses it.
     */
    private static final class NullInternalMap extends AbstractConcurrentNullSafeMap<String, Integer> {

        NullInternalMap() {
            super(null);
        }

        @Override
        protected Object maskNullKey(String key) {
            return key;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected String unmaskNullKey(Object key) {
            return (String) key;
        }

        @Override
        protected Object maskNullValue(Integer value) {
            return value;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected Integer unmaskNullValue(Object value) {
            return (Integer) value;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise mocks and the concrete map implementation.
        mocks = MockitoAnnotations.openMocks(this);
        testMap = new TestMap(internalMap);
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Release Mockito resources.
        mocks.close();
    }

    @Test
    void keySetSizeAndContainsReflectInternalMap() {
        // GIVEN – a backing map whose keySet contains two entries.
        Set<Object> internalKeySet = new HashSet<>(Arrays.asList("alpha", "beta"));
        when(internalMap.keySet()).thenReturn(internalKeySet);
        when(internalMap.containsKey("alpha")).thenReturn(true);
        when(internalMap.containsKey("beta")).thenReturn(true);
        when(internalMap.containsKey("gamma")).thenReturn(false);
        // WHEN
        Set<String> ks = testMap.keySet();
        // THEN – size mirrors the internal set.
        assertEquals(2, ks.size(), "keySet size should match internal map size");
        // AND – contains delegates to internalMap.containsKey after masking.
        assertTrue(ks.contains("alpha"));
        assertTrue(ks.contains("beta"));
        assertFalse(ks.contains("gamma"));
    }

    @Test
    void iteratorReturnsUnmaskedKeys() {
        // GIVEN – internal map keySet iterator yields sentinel objects.
        Set<Object> internalKeySet = new LinkedHashSet<>(Arrays.asList("one", "two", "three"));
        when(internalMap.keySet()).thenReturn(internalKeySet);
        // WHEN
        Iterator<String> it = testMap.keySet().iterator();
        // THEN – iterator traverses in the same order and unmasking is identity.
        assertTrue(it.hasNext());
        assertEquals("one", it.next());
        assertTrue(it.hasNext());
        assertEquals("two", it.next());
        assertTrue(it.hasNext());
        assertEquals("three", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void removeDelegatesToInternalMapAndReturnsCorrectBoolean() {
        // GIVEN – internalMap.remove returns a non‑null value for a known key.
        when(internalMap.remove("target")).thenReturn(new Object());
        // WHEN
        boolean removed = testMap.keySet().remove("target");
        // THEN
        assertTrue(removed, "remove should return true when internalMap.remove yields non‑null");
        verify(internalMap, times(1)).remove("target");
    }

    @Test
    void removeReturnsFalseWhenKeyNotPresent() {
        // GIVEN – internalMap.remove returns null for an absent key.
        when(internalMap.remove("absent")).thenReturn(null);
        // WHEN
        boolean removed = testMap.keySet().remove("absent");
        // THEN
        assertFalse(removed, "remove should return false when internalMap.remove yields null");
        verify(internalMap, times(1)).remove("absent");
    }

    @Test
    void clearInvokesInternalMapClear() {
        // GIVEN – a fresh keySet view.
        Set<String> ks = testMap.keySet();
        // WHEN
        ks.clear();
        // THEN – internal map's clear method is called exactly once.
        verify(internalMap, times(1)).clear();
    }

    @Test
    void iteratorBehaviorWithRemoveUpdatesUnderlyingMap() {
        // Use a real ConcurrentHashMap so that iterator.remove() really mutates the map.
        ConcurrentMap<Object, Object> realMap = new ConcurrentHashMap<>();
        realMap.put("a", 1);
        realMap.put("b", 2);
        realMap.put("c", 3);
        TestMap realTestMap = new TestMap(realMap);
        Iterator<String> it = realTestMap.keySet().iterator();
        // Advance to first element and remove it via iterator.
        assertTrue(it.hasNext());
        assertEquals("a", it.next());
        // should delete key "a" from the underlying map
        it.remove();
        // Verify that the map no longer contains the removed key and size shrank.
        assertFalse(realMap.containsKey("a"));
        assertEquals(2, realTestMap.keySet().size());
        assertFalse(realTestMap.keySet().contains("a"));
    }

    @Test
    void iteratorOnEmptyMapBehavesCorrectly() {
        // Empty backing map.
        Set<Object> emptySet = Collections.emptySet();
        when(internalMap.keySet()).thenReturn(emptySet);
        Iterator<String> it = testMap.keySet().iterator();
        assertFalse(it.hasNext(), "hasNext() must be false for an empty iterator");
        assertThrows(NoSuchElementException.class, it::next, "next() on an empty iterator should throw NoSuchElementException");
    }

    @Test
    void sizeReflectsMutationsWhenUsingMockedInternalKeySet() {
        // We'll back the mock with a real Set so that put/remove affect the size.
        Set<Object> backingKeySet = new HashSet<>();
        when(internalMap.keySet()).thenReturn(backingKeySet);
        // Simulate put: internalMap.put should add the key to the backing set.
        when(internalMap.put(any(), any())).thenAnswer(invocation -> {
            Object k = invocation.getArgument(0);
            backingKeySet.add(k);
            // previous value not relevant for this test
            return null;
        });
        // Simulate remove: internalMap.remove should remove the key from the set.
        when(internalMap.remove(any())).thenAnswer(invocation -> {
            Object k = invocation.getArgument(0);
            return backingKeySet.remove(k) ? new Object() : null;
        });
        // Initially empty.
        assertEquals(0, testMap.keySet().size());
        // Add three keys (including a null key – maskNullKey returns null).
        testMap.put("x", 10);
        testMap.put(null, 20);
        testMap.put("y", 30);
        assertEquals(3, testMap.keySet().size());
        // Remove one key via the map API.
        testMap.remove("x");
        assertEquals(2, testMap.keySet().size());
        // Clear the map.
        testMap.clear();
        assertEquals(0, testMap.keySet().size());
    }

    @Test
    void containsHandlesNullKeyCorrectly() {
        Set<Object> internalKeySet = new HashSet<>(Arrays.asList((Object) null, "present"));
        when(internalMap.keySet()).thenReturn(internalKeySet);
        when(internalMap.containsKey(null)).thenReturn(true);
        when(internalMap.containsKey("present")).thenReturn(true);
        when(internalMap.containsKey("absent")).thenReturn(false);
        Set<String> ks = testMap.keySet();
        assertTrue(ks.contains(null), "contains(null) should be true when the map holds a null key");
        assertTrue(ks.contains("present"));
        assertFalse(ks.contains("absent"));
    }

    @Test
    void removeNullKeySemantics() {
        // Case 1 – null key present.
        // non‑null indicates removal
        when(internalMap.remove(null)).thenReturn(new Object());
        assertTrue(testMap.keySet().remove(null), "remove(null) should return true when key existed");
        // Case 2 – null key absent.
        when(internalMap.remove(null)).thenReturn(null);
        assertFalse(testMap.keySet().remove(null), "remove(null) should return false when key absent");
    }

    @Test
    void removeNonNullKeySemantics() {
        // Existing key.
        when(internalMap.remove("delta")).thenReturn(new Object());
        assertTrue(testMap.keySet().remove("delta"));
        // Non‑existent key.
        when(internalMap.remove("epsilon")).thenReturn(null);
        assertFalse(testMap.keySet().remove("epsilon"));
    }

    @Test
    void clearOnAlreadyEmptyMapDoesNotThrow() {
        // internalMap.clear() on a mock never throws; just verify call count.
        Set<String> ks = testMap.keySet();
        // first clear (map may be empty)
        ks.clear();
        // second clear on already empty map
        ks.clear();
        verify(internalMap, times(2)).clear();
    }

    @Test
    void iteratorRemoveUpdatesKeySetView() {
        // Real map to observe side‑effects.
        ConcurrentMap<Object, Object> realMap = new ConcurrentHashMap<>();
        realMap.put("a", 1);
        realMap.put("b", 2);
        realMap.put("c", 3);
        TestMap realTestMap = new TestMap(realMap);
        Iterator<String> it = realTestMap.keySet().iterator();
        // Remove "b" via iterator.
        while (it.hasNext()) {
            String key = it.next();
            if ("b".equals(key)) {
                it.remove();
                break;
            }
        }
        assertEquals(2, realTestMap.keySet().size());
        assertFalse(realTestMap.keySet().contains("b"));
        assertNull(realMap.get("b"));
    }

    @Test
    void iteratorRemoveWithoutNextThrowsIllegalStateException() {
        Set<Object> internalKeySet = new HashSet<>(Arrays.asList("x"));
        when(internalMap.keySet()).thenReturn(internalKeySet);
        Iterator<String> it = testMap.keySet().iterator();
        assertThrows(IllegalStateException.class, it::remove, "Calling iterator.remove() before next() must throw IllegalStateException");
    }

    @Test
    void containsThrowsClassCastExceptionForIncompatibleType() {
        Set<Object> internalKeySet = new HashSet<>(Arrays.asList("stringKey"));
        when(internalMap.keySet()).thenReturn(internalKeySet);
        when(internalMap.containsKey(any())).thenReturn(true);
        Set<String> ks = testMap.keySet();
        assertThrows(ClassCastException.class, () -> ks.contains(123), "contains() should throw ClassCastException when argument type is incompatible");
    }

    @Test
    void containsThrowsNullPointerExceptionWhenInternalMapIsNull() {
        NullInternalMap mapWithNullInternal = new NullInternalMap();
        Set<String> ks = mapWithNullInternal.keySet();
        assertThrows(NullPointerException.class, () -> ks.contains("any"), "Calling contains() when internalMap is null should throw NPE");
    }
}
