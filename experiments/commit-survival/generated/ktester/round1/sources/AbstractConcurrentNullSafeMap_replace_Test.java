package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_replace_Test {

    @Mock
    private ConcurrentMap<Object, Object> internalMapMock;

    /**
     * Concrete instance of the abstract class under test
     */
    private AbstractConcurrentNullSafeMap<String, String> mapUnderTest;

    @BeforeEach
    void setupBeforeEach() {
        // By default we use the mocked internal map; individual tests can replace this instance.
        mapUnderTest = new AbstractConcurrentNullSafeMap<String, String>(internalMapMock) {
        };
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mock interactions after each test to avoid cross‑test contamination
        reset(internalMapMock);
    }

    /**
     * Helper to build a map instance that uses the mock for each test
     */
    private AbstractConcurrentNullSafeMap<String, String> withMock() {
        return new AbstractConcurrentNullSafeMap<String, String>(internalMapMock) {
        };
    }

    private static Stream<Arguments> matchingOldValueProvider() {
        return Stream.of(arguments("alpha", "old", "new", true), // second call assumes previous replace succeeded
        arguments("alpha", "new", "newer", true));
    }

    private static Stream<Arguments> oldValueMismatchProvider() {
        return Stream.of(arguments("beta", "old", "new", false));
    }

    private static Stream<Arguments> absentKeyProvider() {
        return Stream.of(arguments("gamma", "any", "new", false));
    }

    private static Stream<Arguments> nullKeyProvider() {
        return Stream.of(arguments(null, "oldNullKey", "newNullKey", true), arguments(null, "wrong", "new", false));
    }

    private static Stream<Arguments> nullOldValueProvider() {
        return Stream.of(arguments("delta", null, "nowNotNull", true), // description says false – old null does not match non‑null stored value
        arguments("delta", null, null, false));
    }

    @Test
    void replace_WithAllNonNullValues_DelegatesToInternalMapAndReturnsResult() {
        // GIVEN
        String key = "k1";
        String oldVal = "old";
        String newVal = "new";
        when(internalMapMock.replace(key, oldVal, newVal)).thenReturn(true);
        // WHEN
        boolean result = mapUnderTest.replace(key, oldVal, newVal);
        // THEN
        assertTrue(result, "replace should return true as supplied by internal map");
        verify(internalMapMock, times(1)).replace(key, oldVal, newVal);
    }

    @Test
    void replace_WithNullKey_MasksKeyBeforeDelegation() {
        // GIVEN
        String oldVal = "old";
        String newVal = "new";
        // The internal map will receive masked (non‑null) objects; we just return false for simplicity
        when(internalMapMock.replace(any(), eq(oldVal), eq(newVal))).thenReturn(false);
        // WHEN
        boolean result = mapUnderTest.replace(null, oldVal, newVal);
        // THEN
        assertFalse(result, "replace should propagate the boolean result from internal map");
        ArgumentCaptor<Object> keyCaptor = ArgumentCaptor.forClass(Object.class);
        verify(internalMapMock, times(1)).replace(keyCaptor.capture(), eq(oldVal), eq(newVal));
        Object capturedKey = keyCaptor.getValue();
        assertNotNull(capturedKey, "The masked key must not be null");
        // The captured key should not be the original null reference
        assertNotEquals(null, capturedKey, "Masked key should differ from the original null");
    }

    @Test
    void replace_WithNullOldAndNewValues_MasksValuesBeforeDelegation() {
        // GIVEN
        String key = "k2";
        // Stub the internal map to return true when called with any non‑null masked values
        when(internalMapMock.replace(eq(key), any(), any())).thenReturn(true);
        // WHEN
        boolean result = mapUnderTest.replace(key, null, null);
        // THEN
        assertTrue(result, "replace should return the value supplied by internal map");
        ArgumentCaptor<Object> oldCaptor = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Object> newCaptor = ArgumentCaptor.forClass(Object.class);
        verify(internalMapMock, times(1)).replace(eq(key), oldCaptor.capture(), newCaptor.capture());
        Object capturedOld = oldCaptor.getValue();
        Object capturedNew = newCaptor.getValue();
        assertNotNull(capturedOld, "Masked old value must not be null");
        assertNotNull(capturedNew, "Masked new value must not be null");
        // Ensure that the masked objects are not the original null references
        assertNotEquals(null, capturedOld, "Masked old value should differ from the original null");
        assertNotEquals(null, capturedNew, "Masked new value should differ from the original null");
    }

    @Test
    void replace_WithAllNulls_MasksAllParameters() {
        // GIVEN
        // internal map will be called with three masked non‑null objects; we simply return false
        when(internalMapMock.replace(any(), any(), any())).thenReturn(false);
        // WHEN
        boolean result = mapUnderTest.replace(null, null, null);
        // THEN
        assertFalse(result, "replace should forward the boolean result from internal map");
        ArgumentCaptor<Object> keyCaptor = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Object> oldCaptor = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Object> newCaptor = ArgumentCaptor.forClass(Object.class);
        verify(internalMapMock, times(1)).replace(keyCaptor.capture(), oldCaptor.capture(), newCaptor.capture());
        assertNotNull(keyCaptor.getValue(), "Masked key must not be null");
        assertNotNull(oldCaptor.getValue(), "Masked old value must not be null");
        assertNotNull(newCaptor.getValue(), "Masked new value must not be null");
    }

    @ParameterizedTest(name = "[{index}] replace({0}, {1}, {2}) → {3}")
    @MethodSource("matchingOldValueProvider")
    void replace_SucceedsWhenOldValueMatches(String key, String oldVal, String newVal, boolean expected) {
        when(internalMapMock.replace(key, oldVal, newVal)).thenReturn(expected);
        boolean result = withMock().replace(key, oldVal, newVal);
        assertEquals(expected, result);
        verify(internalMapMock, times(1)).replace(key, oldVal, newVal);
    }

    @ParameterizedTest(name = "[{index}] replace({0}, {1}, {2}) → {3}")
    @MethodSource("oldValueMismatchProvider")
    void replace_FailsWhenOldValueDoesNotMatch(String key, String oldVal, String newVal, boolean expected) {
        when(internalMapMock.replace(key, oldVal, newVal)).thenReturn(expected);
        boolean result = withMock().replace(key, oldVal, newVal);
        assertEquals(expected, result);
        verify(internalMapMock, times(1)).replace(key, oldVal, newVal);
    }

    @ParameterizedTest(name = "[{index}] replace({0}, {1}, {2}) → {3}")
    @MethodSource("absentKeyProvider")
    void replace_FailsWhenKeyAbsent(String key, String oldVal, String newVal, boolean expected) {
        when(internalMapMock.replace(key, oldVal, newVal)).thenReturn(expected);
        boolean result = withMock().replace(key, oldVal, newVal);
        assertEquals(expected, result);
        verify(internalMapMock, times(1)).replace(key, oldVal, newVal);
    }

    @ParameterizedTest(name = "[{index}] replace(null, {1}, {2}) → {3}")
    @MethodSource("nullKeyProvider")
    void replace_NullKeyHandling(String key, String oldVal, String newVal, boolean expected) {
        // we cannot know the exact masked key object, so use any()
        when(internalMapMock.replace(any(), eq(oldVal), eq(newVal))).thenReturn(expected);
        boolean result = withMock().replace(key, oldVal, newVal);
        assertEquals(expected, result);
        verify(internalMapMock, times(1)).replace(any(), eq(oldVal), eq(newVal));
    }

    @ParameterizedTest(name = "[{index}] replace({0}, null, {2}) → {3}")
    @MethodSource("nullOldValueProvider")
    void replace_NullOldValueHandling(String key, String oldVal, String newVal, boolean expected) {
        when(internalMapMock.replace(eq(key), any(), eq(newVal))).thenReturn(expected);
        boolean result = withMock().replace(key, oldVal, newVal);
        assertEquals(expected, result);
        verify(internalMapMock, times(1)).replace(eq(key), any(), eq(newVal));
    }

    @Test
    void replace_NullNewValueHandling() {
        String key = "epsilon";
        String oldVal = "present";
        String newVal = null;
        when(internalMapMock.replace(key, oldVal, any())).thenReturn(true);
        boolean result = withMock().replace(key, oldVal, newVal);
        assertTrue(result);
        verify(internalMapMock, times(1)).replace(key, oldVal, any());
    }

    @Test
    void replace_BothOldAndNewNull() {
        String key = "zeta";
        when(internalMapMock.replace(key, any(), any())).thenReturn(true);
        boolean result = withMock().replace(key, null, null);
        assertTrue(result);
        verify(internalMapMock, times(1)).replace(eq(key), any(), any());
    }

    @Test
    void replace_VerifySentinelObjectsAreUsed() throws Exception {
        // Use a real ConcurrentHashMap so we can inspect its content
        ConcurrentMap<Object, Object> realMap = new ConcurrentHashMap<>();
        AbstractConcurrentNullSafeMap<String, String> realMapUnderTest = new AbstractConcurrentNullSafeMap<String, String>(realMap) {
        };
        // Obtain the sentinel objects via reflection
        Method maskKey = AbstractConcurrentNullSafeMap.class.getDeclaredMethod("maskNullKey", Object.class);
        Method maskVal = AbstractConcurrentNullSafeMap.class.getDeclaredMethod("maskNullValue", Object.class);
        maskKey.setAccessible(true);
        maskVal.setAccessible(true);
        Object nullKeySentinel = maskKey.invoke(realMapUnderTest, null);
        Object nullValueSentinel = maskVal.invoke(realMapUnderTest, null);
        Object nonNullValueSentinel = maskVal.invoke(realMapUnderTest, "nonNull");
        // Pre‑populate the map with (null → null) using the sentinel objects
        realMap.put(nullKeySentinel, nullValueSentinel);
        // Perform replace(null, null, "nonNull")
        boolean result = realMapUnderTest.replace(null, null, "nonNull");
        assertTrue(result, "replace should succeed when sentinel entry exists");
        // Verify that the internal map now contains the new sentinel value
        assertEquals(nonNullValueSentinel, realMap.get(nullKeySentinel), "The value stored for the masked null key must be the sentinel for \"nonNull\"");
    }

    @Test
    void replace_NullKeyMatchingOldValue() {
        when(internalMapMock.replace(any(), eq("oldNullKey"), eq("newNullKey"))).thenReturn(true);
        boolean result = withMock().replace(null, "oldNullKey", "newNullKey");
        assertTrue(result);
        verify(internalMapMock, times(1)).replace(any(), eq("oldNullKey"), eq("newNullKey"));
    }

    @Test
    void replace_FailsWhenOldNullButStoredNonNull() {
        when(internalMapMock.replace(eq("delta"), any(), eq("nowNotNull"))).thenReturn(false);
        boolean result = withMock().replace("delta", null, "nowNotNull");
        assertFalse(result);
        verify(internalMapMock, times(1)).replace(eq("delta"), any(), eq("nowNotNull"));
    }

    @Test
    void replace_NonNullKeyBothOldAndNewNull() {
        when(internalMapMock.replace(eq("theta"), any(), any())).thenReturn(true);
        boolean result = withMock().replace("theta", null, null);
        assertTrue(result);
        verify(internalMapMock, times(1)).replace(eq("theta"), any(), any());
    }

    @Test
    void replace_FailsWhenOldMismatchAndNewValueNull() {
        when(internalMapMock.replace(eq("epsilon"), eq("different"), any())).thenReturn(false);
        boolean result = withMock().replace("epsilon", "different", null);
        assertFalse(result);
        verify(internalMapMock, times(1)).replace(eq("epsilon"), eq("different"), any());
    }

    @Test
    void replace_IncompatibleKeyTypeReturnsFalse() {
        // The internal map mock will receive an Integer key; we simply return false.
        when(internalMapMock.replace(eq(12345), eq("oldNumberKey"), eq("newNumberKey"))).thenReturn(false);
        boolean result = withMock().replace((String) (Object) 12345, "oldNumberKey", "newNumberKey");
        assertFalse(result);
        verify(internalMapMock, times(1)).replace(eq(12345), eq("oldNumberKey"), eq("newNumberKey"));
    }

    @Test
    void replace_MismatchedOldValueTypeReturnsFalse() {
        when(internalMapMock.replace(eq("lambda"), eq(true), eq("nowString"))).thenReturn(false);
        boolean result = withMock().replace("lambda", true, "nowString");
        assertFalse(result);
        verify(internalMapMock, times(1)).replace(eq("lambda"), eq(true), eq("nowString"));
    }

    @Test
    void replace_AllNullsOnEmptyMapReturnsFalse() {
        when(internalMapMock.replace(any(), any(), any())).thenReturn(false);
        boolean result = withMock().replace(null, null, null);
        assertFalse(result);
        verify(internalMapMock, times(1)).replace(any(), any(), any());
    }

    @Test
    void replace_SucceedsWhenSentinelObjectsAlreadyPresent() throws Exception {
        ConcurrentMap<Object, Object> realMap = new ConcurrentHashMap<>();
        AbstractConcurrentNullSafeMap<String, String> map = new AbstractConcurrentNullSafeMap<String, String>(realMap) {
        };
        // Access sentinel objects
        Method maskKey = AbstractConcurrentNullSafeMap.class.getDeclaredMethod("maskNullKey", Object.class);
        Method maskVal = AbstractConcurrentNullSafeMap.class.getDeclaredMethod("maskNullValue", Object.class);
        maskKey.setAccessible(true);
        maskVal.setAccessible(true);
        Object nullKeySentinel = maskKey.invoke(map, null);
        Object nullValueSentinel = maskVal.invoke(map, null);
        Object finalValueSentinel = maskVal.invoke(map, "finalValue");
        // Insert sentinel → sentinel
        realMap.put(nullKeySentinel, nullValueSentinel);
        // Replace should succeed
        boolean result = map.replace(null, null, "finalValue");
        assertTrue(result);
        assertEquals(finalValueSentinel, realMap.get(nullKeySentinel), "After replace the sentinel value must be updated to the new one");
    }

    @Test
    void replace_ThrowsNPEWhenInternalMapIsNull() {
        AbstractConcurrentNullSafeMap<String, String> mapWithNullInternal = new AbstractConcurrentNullSafeMap<String, String>(null) {
        };
        assertThrows(NullPointerException.class, () -> mapWithNullInternal.replace("anyKey", "old", "new"), "Calling replace with a null internalMap must throw NPE");
    }

    @Test
    void replace_PropagatesUnsupportedOperationException() {
        // Mock that throws the exception when replace is called
        ConcurrentMap<Object, Object> badMap = mock(ConcurrentMap.class);
        when(badMap.replace(any(), any(), any())).thenThrow(new UnsupportedOperationException("replace not supported"));
        AbstractConcurrentNullSafeMap<String, String> map = new AbstractConcurrentNullSafeMap<String, String>(badMap) {
        };
        assertThrows(UnsupportedOperationException.class, () -> map.replace("key", "old", "new"), "UnsupportedOperationException must be propagated");
    }

    @Test
    void replace_PropagatesRuntimeExceptionFromInternalMap() {
        when(internalMapMock.replace(any(), any(), any())).thenThrow(new IllegalStateException("corrupted"));
        assertThrows(IllegalStateException.class, () -> withMock().replace("trigger", "old", "new"), "IllegalStateException should bubble up unchanged");
    }

    @Test
    void replace_ThrowsClassCastExceptionDueToBadMaskNullKey() {
        // Subclass that returns an incompatible sentinel (Integer) for a null key
        AbstractConcurrentNullSafeMap<String, String> badMaskMap = new AbstractConcurrentNullSafeMap<String, String>(internalMapMock) {

            @Override
            protected Object maskNullKey(String key) {
                // Integer instead of expected sentinel type
                return 42;
            }
        };
        // The mock will throw ClassCastException when it receives the wrong key type
        when(internalMapMock.replace(eq(42), any(), any())).thenThrow(new ClassCastException("bad key type"));
        assertThrows(ClassCastException.class, () -> badMaskMap.replace(null, "old", "new"), "ClassCastException from internalMap.replace must be propagated");
    }

    @Test
    void replace_ThrowsClassCastExceptionDueToBadMaskNullValue() {
        // Subclass that returns an incompatible sentinel (List) for a null value
        AbstractConcurrentNullSafeMap<String, String> badMaskMap = new AbstractConcurrentNullSafeMap<String, String>(internalMapMock) {

            @Override
            protected Object maskNullValue(String value) {
                // List instead of expected sentinel
                return java.util.Collections.emptyList();
            }
        };
        // The mock will throw ClassCastException when it receives the wrong value type
        when(internalMapMock.replace(any(), eq(java.util.Collections.emptyList()), any())).thenThrow(new ClassCastException("bad value type"));
        assertThrows(ClassCastException.class, () -> badMaskMap.replace("someKey", null, null), "ClassCastException from internalMap.replace must be propagated");
    }
}
