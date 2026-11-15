package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.util.concurrent.ConcurrentMap;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_replace_2_Test {

    /**
     * Mocked backing map used by the concrete test implementation.
     */
    @Mock
    private ConcurrentMap<Object, Object> internalMap;

    /**
     * Concrete instance of the abstract class under test.
     */
    private AbstractConcurrentNullSafeMap<String, String> map;

    /**
     * Simple concrete subclass that simply forwards to the protected constructor.
     */
    private static class TestMap extends AbstractConcurrentNullSafeMap<String, String> {

        TestMap(ConcurrentMap<Object, Object> internal) {
            super(internal);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Mockito creates the mock for us (annotated with @Mock).
        // Initialise the concrete map implementation with the mocked internal map.
        map = new TestMap(internalMap);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mock interactions so each test starts with a clean slate.
        clearInvocations(internalMap);
    }

    /**
     * Provides the basic behavior cases (existing key vs missing key).
     */
    private static Stream<ReplaceCase> basicBehaviorProvider() {
        return Stream.of(new ReplaceCase("alpha", "newValue", "oldValue", "Key 'alpha' exists in the map with value 'oldValue'. replace should update the value to 'newValue' and return the previous value 'oldValue'."), new ReplaceCase("beta", "anyValue", null, "Key 'beta' does not exist. replace should not add a new entry and must return null."));
    }

    /**
     * Null‑key handling cases.
     */
    private static Stream<ReplaceCase> nullKeyProvider() {
        return Stream.of(new ReplaceCase(null, "newVal", "oldVal", "The map already contains a null key mapped to \"oldVal\". Calling replace(null, \"newVal\") must return the previous value \"oldVal\" and update the entry to \"newVal\"."), new ReplaceCase(null, "any", null, "The map does not contain a null key. replace(null, \"any\") should return null and must not create a new entry."));
    }

    /**
     * Null‑value handling cases.
     */
    private static Stream<ReplaceCase> nullValueProvider() {
        return Stream.of(new ReplaceCase("gamma", null, "oldVal", "Key \"gamma\" exists with value \"oldVal\". Replacing it with null should return the previous value \"oldVal\" and store a null value for the key."), new ReplaceCase("delta", null, null, "Key \"delta\" is absent. replace(\"delta\", null) must return null and must not add the key to the map."));
    }

    /**
     * Calls the protected {@code maskNullKey} method via reflection.
     * For the purpose of these tests the implementation simply returns a sentinel
     * object for {@code null} keys; otherwise it returns the key unchanged.
     */
    private static Object maskNullKey(Object key) {
        // sentinel placeholder – exact value not important for verification
        return key == null ? new Object() : key;
    }

    /**
     * Calls the protected {@code maskNullValue} method via reflection.
     * Mirrors the behaviour of the real implementation: {@code null} values are replaced
     * with a sentinel, otherwise the value is returned unchanged.
     */
    private static Object maskNullValue(Object value) {
        // sentinel placeholder
        return value == null ? new Object() : value;
    }

    @Test
    void replace_NonNullKeyAndValue_WhenPreviousMappingExists_ReturnsPreviousValue() {
        // GIVEN
        String key = "key";
        String newValue = "newValue";
        String previousValue = "oldValue";
        // internalMap.replace should return the *masked* previous value.
        // For non‑null values the mask method returns the value unchanged,
        // so we can simply return the plain previous value.
        when(internalMap.replace(any(), any())).thenReturn(previousValue);
        // WHEN
        String result = map.replace(key, newValue);
        // THEN
        assertEquals(previousValue, result, "replace should return the previous value when it exists");
        // Verify that the internal map received the correctly masked arguments.
        verify(internalMap, times(1)).replace(key, newValue);
    }

    @Test
    void replace_NullKeyAndNonNullValue_WhenPreviousMappingExists_ReturnsPreviousValue() {
        // GIVEN
        String newValue = "value";
        String previousValue = "oldValue";
        when(internalMap.replace(any(), any())).thenReturn(previousValue);
        // WHEN
        String result = map.replace(null, newValue);
        // THEN
        assertEquals(previousValue, result);
        // The key is masked inside the method, therefore we cannot assert the exact argument,
        // but we can verify that replace was called exactly once.
        verify(internalMap, times(1)).replace(any(), eq(newValue));
    }

    @Test
    void replace_NonNullKeyAndNullValue_WhenPreviousMappingExists_ReturnsPreviousValue() {
        // GIVEN
        String key = "key";
        String previousValue = "oldValue";
        when(internalMap.replace(any(), any())).thenReturn(previousValue);
        // WHEN
        String result = map.replace(key, null);
        // THEN
        assertEquals(previousValue, result);
        verify(internalMap, times(1)).replace(eq(key), any());
    }

    @Test
    void replace_WhenNoPreviousMapping_ReturnsNull() {
        // GIVEN
        String key = "key";
        String newValue = "newValue";
        when(internalMap.replace(any(), any())).thenReturn(null);
        // WHEN
        String result = map.replace(key, newValue);
        // THEN
        assertNull(result, "replace should return null when there was no previous mapping");
        verify(internalMap, times(1)).replace(eq(key), eq(newValue));
    }

    @ParameterizedTest(name = "[basic] {index}: {0}")
    @MethodSource("basicBehaviorProvider")
    void replace_BasicBehavior(ReplaceCase tc) {
        // GIVEN
        // mask does not change non‑null values
        Object maskedPrev = tc.expected;
        when(internalMap.replace(eq(maskNullKey(tc.key)), eq(maskNullValue(tc.value)))).thenReturn(maskedPrev);
        // WHEN
        String result = map.replace(tc.key, tc.value);
        // THEN
        if (tc.expected == null) {
            assertNull(result, tc.description);
        } else {
            assertEquals(tc.expected, result, tc.description);
        }
        verify(internalMap, times(1)).replace(eq(maskNullKey(tc.key)), eq(maskNullValue(tc.value)));
    }

    @ParameterizedTest(name = "[null‑key] {index}: {0}")
    @MethodSource("nullKeyProvider")
    void replace_NullKeyHandling(ReplaceCase tc) {
        // GIVEN
        when(internalMap.replace(eq(maskNullKey(tc.key)), eq(maskNullValue(tc.value)))).thenReturn(tc.expected);
        // WHEN
        String result = map.replace(tc.key, tc.value);
        // THEN
        if (tc.expected == null) {
            assertNull(result, tc.description);
        } else {
            assertEquals(tc.expected, result, tc.description);
        }
        verify(internalMap, times(1)).replace(eq(maskNullKey(tc.key)), eq(maskNullValue(tc.value)));
    }

    @ParameterizedTest(name = "[null‑value] {index}: {0}")
    @MethodSource("nullValueProvider")
    void replace_NullValueHandling(ReplaceCase tc) {
        // GIVEN
        when(internalMap.replace(eq(maskNullKey(tc.key)), eq(maskNullValue(tc.value)))).thenReturn(tc.expected);
        // WHEN
        String result = map.replace(tc.key, tc.value);
        // THEN
        if (tc.expected == null) {
            assertNull(result, tc.description);
        } else {
            assertEquals(tc.expected, result, tc.description);
        }
        verify(internalMap, times(1)).replace(eq(maskNullKey(tc.key)), eq(maskNullValue(tc.value)));
    }

    /**
     * Same existing value case.
     */
    @Test
    void replace_SameExistingValue_ReturnsPreviousValue() {
        // GIVEN
        String key = "epsilon";
        String sameValue = "same";
        when(internalMap.replace(eq(maskNullKey(key)), eq(maskNullValue(sameValue)))).thenReturn(sameValue);
        // WHEN
        String result = map.replace(key, sameValue);
        // THEN
        assertEquals(sameValue, result, "replace should return the previous value even when the new value is identical");
        verify(internalMap, times(1)).replace(eq(maskNullKey(key)), eq(maskNullValue(sameValue)));
    }

    /**
     * Size invariance when replace misses.
     */
    @Test
    void replace_MissDoesNotChangeMapSize() {
        // Use a real ConcurrentHashMap to inspect size.
        ConcurrentMap<Object, Object> realBacking = new ConcurrentHashMap<>();
        // Pre‑populate with a single entry to have a known size.
        realBacking.put("existing", "value");
        AbstractConcurrentNullSafeMap<String, String> realMap = new TestMap(realBacking);
        int sizeBefore = realMap.size();
        // WHEN: replace a missing key.
        String result = realMap.replace("zeta", "new");
        // THEN
        assertNull(result, "replace should return null for a missing key");
        assertEquals(sizeBefore, realMap.size(), "Map size must remain unchanged after a replace miss");
    }

    /**
     * Internal map is null – constructor receives null.
     */
    @Test
    void replace_InternalMapIsNull_ThrowsNullPointerException() {
        // GIVEN a map instance with a null internal map.
        AbstractConcurrentNullSafeMap<String, String> nullBackingMap = new TestMap(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> nullBackingMap.replace("anyKey", "anyValue"), "Calling replace when internalMap is null must throw NullPointerException");
    }

    /**
     * Underlying map does not support replace – throws UnsupportedOperationException.
     */
    @Test
    void replace_UnderlyingMapUnsupported_PropagatesException() {
        // GIVEN
        when(internalMap.replace(any(), any())).thenThrow(new UnsupportedOperationException("replace not supported"));
        // WHEN / THEN
        assertThrows(UnsupportedOperationException.class, () -> map.replace("key1", "value1"), "UnsupportedOperationException from internal map should propagate");
    }

    /**
     * Key type mismatch – underlying map throws ClassCastException.
     */
    @Test
    void replace_KeyTypeMismatch_ThrowsClassCastException() {
        // GIVEN
        doThrow(new ClassCastException("Invalid key type")).when(internalMap).replace(any(), any());
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> map.replace((String) (Object) Integer.valueOf(123), "stringValue"), "ClassCastException should be propagated when key type is invalid");
    }

    /**
     * Value type mismatch – underlying map throws ClassCastException.
     */
    @Test
    void replace_ValueTypeMismatch_ThrowsClassCastException() {
        // GIVEN
        doThrow(new ClassCastException("Invalid value type")).when(internalMap).replace(any(), any());
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> map.replace("validKey", (String) (Object) Integer.valueOf(456)), "ClassCastException should be propagated when value type is invalid");
    }
}
