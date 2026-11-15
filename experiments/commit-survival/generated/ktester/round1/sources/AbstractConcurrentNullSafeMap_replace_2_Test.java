package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.util.concurrent.ConcurrentMap;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_replace_2_Test {

    /**
     * Mocked internal map that the AbstractConcurrentNullSafeMap delegates to.
     */
    @Mock
    private ConcurrentMap<Object, Object> internalMap;

    /**
     * Concrete instance of the abstract class under test.
     */
    private TestMap<String, String> map;

    /**
     * Simple concrete subclass that only forwards the constructor.
     */
    private static class TestMap<K, V> extends AbstractConcurrentNullSafeMap<K, V> {

        TestMap(ConcurrentMap<Object, Object> internalMap) {
            super(internalMap);
        }

        /**
         * Expose the protected mask methods for verification in tests.
         */
        Object maskKey(K key) {
            return maskNullKey(key);
        }

        Object maskVal(V value) {
            return maskNullValue(value);
        }

        /**
         * Expose the protected unmask method (used to obtain the sentinel for null values).
         */
        Object maskNull() {
            return maskNullValue(null);
        }
    }

    /**
     * Subclass that forces {@code maskNullKey} to return {@code null}.
     */
    private static class NullKeyMaskMap<K, V> extends TestMap<K, V> {

        NullKeyMaskMap(ConcurrentMap<Object, Object> internalMap) {
            super(internalMap);
        }

        @Override
        protected Object maskNullKey(K key) {
            // deliberately break the contract
            return null;
        }
    }

    /**
     * Subclass that forces {@code maskNullValue} to return {@code null}.
     */
    private static class NullValueMaskMap<K, V> extends TestMap<K, V> {

        NullValueMaskMap(ConcurrentMap<Object, Object> internalMap) {
            super(internalMap);
        }

        @Override
        protected Object maskNullValue(V value) {
            // deliberately break the contract
            return null;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Mockito creates the mock for us via the @Mock annotation.
        map = new TestMap<>(internalMap);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mock interactions to avoid cross‑test interference.
        reset(internalMap);
    }

    private static Stream<Arguments> basicFunctionalityProvider() {
        return Stream.of(// present key – should return previous value
        Arguments.of("existingKey", "newValue", "oldValue"), // missing key – internal map returns null
        Arguments.of("missingKey", "anyValue", null));
    }

    private static Stream<Arguments> nullKeyHandlingProvider() {
        return Stream.of(// existing null key – previous value is non‑null
        Arguments.of("newValueForNullKey", "oldValueForNullKey"), // missing null key – internal map returns null
        Arguments.of("anyValue", null));
    }

    private static Stream<Arguments> nullValueHandlingProvider() {
        return Stream.of(// existing key with non‑null previous value
        Arguments.of("existingKey", "oldValue"), // missing key – should return null
        Arguments.of("missingKey", null));
    }

    @Test
    void replace_NonNullKeyAndValue_ReturnsPreviousValue() {
        // GIVEN
        String key = "myKey";
        String value = "newValue";
        String previous = "oldValue";
        // internalMap is expected to receive the masked key/value.
        when(internalMap.replace(map.maskKey(key), map.maskVal(value))).thenReturn(previous);
        // WHEN
        String result = map.replace(key, value);
        // THEN
        assertEquals(previous, result, "replace should return the previous value returned by internalMap");
        verify(internalMap, times(1)).replace(map.maskKey(key), map.maskVal(value));
    }

    @Test
    void replace_NullKey_MasksKeyBeforeDelegating() {
        // GIVEN
        String value = "value";
        String previous = "old";
        // sentinel used for a null key
        Object maskedNullKey = map.maskKey(null);
        when(internalMap.replace(maskedNullKey, map.maskVal(value))).thenReturn(previous);
        // WHEN
        String result = map.replace(null, value);
        // THEN
        assertEquals(previous, result);
        ArgumentCaptor<Object> keyCaptor = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Object> valCaptor = ArgumentCaptor.forClass(Object.class);
        verify(internalMap).replace(keyCaptor.capture(), valCaptor.capture());
        assertSame(maskedNullKey, keyCaptor.getValue(), "The null key must be masked before being passed to internalMap");
        assertEquals(map.maskVal(value), valCaptor.getValue(), "The non‑null value should be passed unchanged (or masked if implementation does so).");
    }

    @Test
    void replace_NullValue_ReturnsNullWhenPreviousWasNull() {
        // GIVEN
        String key = "key";
        // sentinel representing a stored null value
        Object maskedNullValue = map.maskNull();
        Object maskedKey = map.maskKey(key);
        // internalMap reports that the previous value was a null (sentinel)
        when(internalMap.replace(maskedKey, maskedNullValue)).thenReturn(maskedNullValue);
        // WHEN
        String result = map.replace(key, null);
        // THEN
        assertNull(result, "replace should unmask a sentinel null value to a real null");
        verify(internalMap).replace(maskedKey, maskedNullValue);
    }

    @Test
    void replace_NoPreviousMapping_ReturnsNull() {
        // GIVEN
        String key = "absent";
        String value = "something";
        when(internalMap.replace(map.maskKey(key), map.maskVal(value))).thenReturn(null);
        // WHEN
        String result = map.replace(key, value);
        // THEN
        assertNull(result, "When internalMap.replace returns null, replace should also return null");
        verify(internalMap).replace(map.maskKey(key), map.maskVal(value));
    }

    /**
     * replace(K, V) - basic functionality
     */
    @ParameterizedTest(name = "[{index}] key={0}, value={1} => expected={2}")
    @MethodSource("basicFunctionalityProvider")
    void replace_BasicFunctionality(String key, String value, String expected) {
        // GIVEN
        Object maskedKey = map.maskKey(key);
        Object maskedVal = map.maskVal(value);
        when(internalMap.replace(maskedKey, maskedVal)).thenReturn(expected);
        // WHEN
        String result = map.replace(key, value);
        // THEN
        assertEquals(expected, result);
        verify(internalMap, times(1)).replace(maskedKey, maskedVal);
    }

    /**
     * replace(K, V) - null key handling
     */
    @ParameterizedTest(name = "[{index}] key=null, value={1} => expected={2}")
    @MethodSource("nullKeyHandlingProvider")
    void replace_NullKeyHandling(String value, String expected) {
        // GIVEN
        Object maskedNullKey = map.maskKey(null);
        Object maskedVal = map.maskVal(value);
        when(internalMap.replace(maskedNullKey, maskedVal)).thenReturn(expected);
        // WHEN
        String result = map.replace(null, value);
        // THEN
        assertEquals(expected, result);
        verify(internalMap, times(1)).replace(maskedNullKey, maskedVal);
    }

    /**
     * replace(K, V) - null value handling
     */
    @ParameterizedTest(name = "[{index}] key={0}, value=null => expected={2}")
    @MethodSource("nullValueHandlingProvider")
    void replace_NullValueHandling(String key, String expected) {
        // GIVEN
        Object maskedKey = map.maskKey(key);
        // sentinel for null
        Object maskedNull = map.maskVal(null);
        when(internalMap.replace(maskedKey, maskedNull)).thenReturn(expected);
        // WHEN
        String result = map.replace(key, null);
        // THEN
        assertEquals(expected, result);
        verify(internalMap, times(1)).replace(maskedKey, maskedNull);
    }

    /**
     * replace(K, V) - previous value was null (masked)
     */
    @Test
    void replace_PreviousValueWasNull_ReturnsNull() {
        // GIVEN
        String key = "keyWithNullValue";
        String newValue = "newNonNullValue";
        Object maskedKey = map.maskKey(key);
        Object maskedNewVal = map.maskVal(newValue);
        // internalMap reports that the previous value was the sentinel for null
        when(internalMap.replace(maskedKey, maskedNewVal)).thenReturn(map.maskNull());
        // WHEN
        String result = map.replace(key, newValue);
        // THEN
        assertNull(result, "When the previous value is a masked null, replace should return null");
        verify(internalMap).replace(maskedKey, maskedNewVal);
    }

    /**
     * replace(K, V) - both key and value null (previous entry present)
     */
    @Test
    void replace_BothNullKeyAndValue_EntryPresent_ReturnsNull() {
        // GIVEN
        Object maskedNullKey = map.maskKey(null);
        Object maskedNullVal = map.maskVal(null);
        // internalMap had a null key with a null value (sentinel stored)
        when(internalMap.replace(maskedNullKey, maskedNullVal)).thenReturn(map.maskNull());
        // WHEN
        String result = map.replace(null, null);
        // THEN
        assertNull(result, "Replacing a null key/value where an entry already exists should return null");
        verify(internalMap).replace(maskedNullKey, maskedNullVal);
    }

    /**
     * replace(K, V) - both key and value null (no entry)
     */
    @Test
    void replace_BothNullKeyAndValue_NoEntry_ReturnsNull() {
        // GIVEN
        Object maskedNullKey = map.maskKey(null);
        Object maskedNullVal = map.maskVal(null);
        // internalMap returns null because the entry does not exist
        when(internalMap.replace(maskedNullKey, maskedNullVal)).thenReturn(null);
        // WHEN
        String result = map.replace(null, null);
        // THEN
        assertNull(result, "Replacing a null key/value when no entry exists should return null");
        verify(internalMap).replace(maskedNullKey, maskedNullVal);
    }

    /**
     * replace(K, V) - replace with same value (no change)
     */
    @Test
    void replace_SameValue_NoChange_ReturnsPrevious() {
        // GIVEN
        String key = "existingKey";
        String sameValue = "oldValue";
        Object maskedKey = map.maskKey(key);
        Object maskedVal = map.maskVal(sameValue);
        when(internalMap.replace(maskedKey, maskedVal)).thenReturn(sameValue);
        // WHEN
        String result = map.replace(key, sameValue);
        // THEN
        assertEquals(sameValue, result, "Replacing with the same value should still return the previous value");
        verify(internalMap).replace(maskedKey, maskedVal);
    }

    /**
     * replace(K, V) - internal map is null -> NullPointerException
     */
    @Test
    void replace_InternalMapNull_ThrowsNPE() {
        // GIVEN a map instance whose internalMap field is null
        TestMap<String, String> nullInternalMap = new TestMap<>(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> nullInternalMap.replace("anyKey", "anyValue"), "Calling replace when internalMap is null should throw NPE");
    }

    /**
     * replace(K, V) - internal map replace throws UnsupportedOperationException
     */
    @Test
    void replace_InternalMapReplaceUnsupported_PropagatesException() {
        // GIVEN
        when(internalMap.replace(any(), any())).thenThrow(new UnsupportedOperationException("replace not supported"));
        // WHEN / THEN
        assertThrows(UnsupportedOperationException.class, () -> map.replace("someKey", "someValue"), "UnsupportedOperationException from internalMap.replace should be propagated");
    }

    /**
     * replace(K, V) - maskNullKey overridden to return null -> NPE from internalMap
     */
    @Test
    void replace_MaskNullKeyReturnsNull_ThrowsNPE() {
        // GIVEN a map that returns null for maskNullKey
        NullKeyMaskMap<String, String> brokenMap = new NullKeyMaskMap<>(internalMap);
        // internalMap will receive a null key; we let it throw NPE
        when(internalMap.replace(isNull(), any())).thenThrow(new NullPointerException("key is null"));
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> brokenMap.replace(null, "value"), "If maskNullKey returns null, internalMap.replace should receive null and throw NPE");
    }

    /**
     * replace(K, V) - maskNullValue overridden to return null -> NPE from internalMap
     */
    @Test
    void replace_MaskNullValueReturnsNull_ThrowsNPE() {
        // GIVEN a map that returns null for maskNullValue
        NullValueMaskMap<String, String> brokenMap = new NullValueMaskMap<>(internalMap);
        // internalMap will receive a null value; we let it throw NPE
        when(internalMap.replace(any(), isNull())).thenThrow(new NullPointerException("value is null"));
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> brokenMap.replace("key", null), "If maskNullValue returns null, internalMap.replace should receive null and throw NPE");
    }
}
