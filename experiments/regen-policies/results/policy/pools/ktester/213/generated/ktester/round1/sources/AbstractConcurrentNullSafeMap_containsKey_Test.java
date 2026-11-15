package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import java.lang.reflect.Method;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_containsKey_Test {

    private TestMap<String, Integer> map;

    /**
     * Simple concrete implementation used only for testing.
     */
    private static class TestMap<K, V> extends AbstractConcurrentNullSafeMap<K, V> {

        TestMap(ConcurrentMap<Object, Object> backing) {
            super(backing);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use a fresh backing map for each test to avoid cross‑test interference.
        ConcurrentMap<Object, Object> backing = new ConcurrentHashMap<>();
        map = new TestMap<>(backing);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to help GC and keep the test isolated.
        map.clear();
    }

    /**
     * Invoke the protected {@code maskNullKey} method via reflection.
     */
    private static Object maskNullKey(AbstractConcurrentNullSafeMap<?, ?> map, Object key) {
        try {
            Method m = AbstractConcurrentNullSafeMap.class.getDeclaredMethod("maskNullKey", Object.class);
            m.setAccessible(true);
            return m.invoke(map, key);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Set the {@code internalMap} field to a given value via reflection.
     */
    private static void setInternalMap(AbstractConcurrentNullSafeMap<?, ?> map, ConcurrentMap<Object, Object> newMap) {
        try {
            var f = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
            f.setAccessible(true);
            f.set(map, newMap);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void containsKey_returnsTrueWhenKeyPresent() {
        // given
        map.put("one", 1);
        map.put("two", 2);
        // when
        boolean result = map.containsKey("one");
        // then
        assertTrue(result, "containsKey should return true for an existing key");
    }

    @Test
    void containsKey_returnsFalseWhenKeyAbsent() {
        // given
        map.put("alpha", 10);
        // when
        boolean result = map.containsKey("beta");
        // then
        assertFalse(result, "containsKey should return false for a missing key");
    }

    @Test
    void containsKey_handlesNullKeyCorrectly() {
        // given
        // null key is allowed by the null‑safe map
        map.put(null, 42);
        // when
        boolean containsNull = map.containsKey(null);
        boolean containsNonNull = map.containsKey("any");
        // then
        assertTrue(containsNull, "containsKey should return true for a stored null key");
        assertFalse(containsNonNull, "containsKey should return false for a key that was not stored");
    }

    @Test
    void containsKey_doesNotModifyMap() {
        // given
        map.put("key", 5);
        int sizeBefore = map.size();
        // when
        map.containsKey("key");
        // then
        assertEquals(sizeBefore, map.size(), "containsKey must not alter the size of the map");
    }

    @ParameterizedTest(name = "[{index}] key={0} → expected={1}")
    @CsvSource({ // null key present
    "null, true", // null key absent (handled in separate test)
    "null, false", "existingKey, true", "missingKey, false" })
    void containsKey_basicBehaviour(String keyString, boolean expected) {
        // Prepare the map according to the expected outcome
        if (expected) {
            // For true we need the key to be present.
            if ("null".equals(keyString)) {
                map.put(null, 42);
            } else if ("existingKey".equals(keyString)) {
                map.put("existingKey", 1);
            }
        } else {
            // Ensure the key is absent.
            if ("null".equals(keyString)) {
                // leave map without a null key
            } else if ("missingKey".equals(keyString)) {
                map.put("someOtherKey", 99);
            }
        }
        Object key = "null".equals(keyString) ? null : keyString;
        boolean result = map.containsKey(key);
        assertEquals(expected, result, () -> "containsKey should return " + expected + " for key: " + key);
    }

    @Test
    void containsKey_nullKeyAbsent() {
        // No entry for null key
        assertFalse(map.containsKey(null), "containsKey must return false when null key is not stored");
    }

    @Test
    void containsKey_wrongKeyType_throwsClassCastException() {
        // map is <String,Integer>, passing an Integer should cause a ClassCastException
        assertThrows(ClassCastException.class, () -> map.containsKey(12345), "containsKey should throw ClassCastException for a key of incompatible type");
    }

    @Test
    void containsKey_nullKey_masksAndForwardsSentinel_true() {
        // Mock internal map
        @SuppressWarnings("unchecked")
        ConcurrentMap<Object, Object> mockInternal = mock(ConcurrentMap.class);
        TestMap<String, Integer> testMap = new TestMap<>(mockInternal);
        // Obtain the sentinel that maskNullKey(null) would produce
        Object sentinel = maskNullKey(testMap, null);
        // Configure mock to return true only for the sentinel
        when(mockInternal.containsKey(sentinel)).thenReturn(true);
        // Act
        boolean result = testMap.containsKey(null);
        // Verify delegation
        assertTrue(result, "containsKey should return true when internal map reports true for the sentinel");
        verify(mockInternal, times(1)).containsKey(sentinel);
        verifyNoMoreInteractions(mockInternal);
    }

    @Test
    void containsKey_nullKey_masksAndForwardsSentinel_false() {
        @SuppressWarnings("unchecked")
        ConcurrentMap<Object, Object> mockInternal = mock(ConcurrentMap.class);
        TestMap<String, Integer> testMap = new TestMap<>(mockInternal);
        Object sentinel = maskNullKey(testMap, null);
        when(mockInternal.containsKey(sentinel)).thenReturn(false);
        boolean result = testMap.containsKey(null);
        assertFalse(result, "containsKey should return false when internal map reports false for the sentinel");
        verify(mockInternal, times(1)).containsKey(sentinel);
    }

    @Test
    void containsKey_internalMapThrowsRuntimeException_propagates() {
        @SuppressWarnings("unchecked")
        ConcurrentMap<Object, Object> mockInternal = mock(ConcurrentMap.class);
        TestMap<String, Integer> testMap = new TestMap<>(mockInternal);
        when(mockInternal.containsKey(any())).thenThrow(new RuntimeException("boom"));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> testMap.containsKey("anyKey"), "RuntimeException should propagate unchanged");
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void containsKey_nullKey_internalMapThrowsRuntimeException_propagates() {
        @SuppressWarnings("unchecked")
        ConcurrentMap<Object, Object> mockInternal = mock(ConcurrentMap.class);
        TestMap<String, Integer> testMap = new TestMap<>(mockInternal);
        Object sentinel = maskNullKey(testMap, null);
        when(mockInternal.containsKey(sentinel)).thenThrow(new RuntimeException("nullBoom"));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> testMap.containsKey(null), "RuntimeException from internal map should propagate even for null key");
        assertEquals("nullBoom", ex.getMessage());
    }

    @Test
    void containsKey_mixedState_nullKeyFound() {
        // Populate map with sentinel (null key) and other entries
        map.put(null, 0);
        map.put("a", 1);
        map.put("b", 2);
        assertTrue(map.containsKey(null), "containsKey should find the stored null key amidst other entries");
    }

    @Test
    void containsKey_mixedState_existingKeyFound() {
        map.put(null, 0);
        map.put("existingKey", 100);
        map.put("other", 200);
        assertTrue(map.containsKey("existingKey"), "containsKey must locate a regular key even when a null key sentinel is present");
    }

    @Test
    void containsKey_mixedState_missingKeyNotFound() {
        map.put(null, 0);
        map.put("present", 1);
        assertFalse(map.containsKey("missingKey"), "containsKey should return false for a key that is not present, regardless of other entries");
    }

    @Test
    void containsKey_internalMapIsNull_throwsNullPointerException_nullKey() throws Exception {
        // Create map with a real backing map, then nullify the internalMap field.
        setInternalMap(map, null);
        assertThrows(NullPointerException.class, () -> map.containsKey(null), "When internalMap is null, containsKey must throw NullPointerException");
    }

    @Test
    void containsKey_internalMapIsNull_throwsNullPointerException_nonNullKey() throws Exception {
        setInternalMap(map, null);
        assertThrows(NullPointerException.class, () -> map.containsKey("someKey"), "When internalMap is null, containsKey must throw NullPointerException for any key");
    }

    @Test
    void containsKey_internalMapThrowsIllegalArgumentException_propagates() {
        @SuppressWarnings("unchecked")
        ConcurrentMap<Object, Object> mockInternal = mock(ConcurrentMap.class);
        TestMap<String, Integer> testMap = new TestMap<>(mockInternal);
        when(mockInternal.containsKey(any())).thenThrow(new IllegalArgumentException("bad key"));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> testMap.containsKey("invalidKey"), "IllegalArgumentException should propagate unchanged");
        assertEquals("bad key", ex.getMessage());
    }

    @Test
    void containsKey_nullKey_internalMapThrowsIllegalArgumentException_propagates() {
        @SuppressWarnings("unchecked")
        ConcurrentMap<Object, Object> mockInternal = mock(ConcurrentMap.class);
        TestMap<String, Integer> testMap = new TestMap<>(mockInternal);
        Object sentinel = maskNullKey(testMap, null);
        when(mockInternal.containsKey(sentinel)).thenThrow(new IllegalArgumentException("null bad"));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> testMap.containsKey(null), "IllegalArgumentException from internal map should propagate for null key");
        assertEquals("null bad", ex.getMessage());
    }
}
