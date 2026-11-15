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
class AbstractConcurrentNullSafeMap_putIfAbsent_Test {

    @Mock
    private ConcurrentMap<Object, Object> mockInternalMap;

    private TestConcurrentNullSafeMap mapUnderTest;

    /**
     * Concrete subclass used only for testing the abstract class.
     */
    private static final class TestConcurrentNullSafeMap extends AbstractConcurrentNullSafeMap<String, String> {

        TestConcurrentNullSafeMap(ConcurrentMap<Object, Object> internalMap) {
            super(internalMap);
        }
    }

    /**
     * Subclass that deliberately throws NPE from {@code maskNullKey} when the key is {@code null}.
     */
    private static final class MaskNullKeyThrowsMap extends AbstractConcurrentNullSafeMap<String, String> {

        MaskNullKeyThrowsMap(ConcurrentMap<Object, Object> internalMap) {
            super(internalMap);
        }

        @Override
        protected Object maskNullKey(String key) {
            if (key == null) {
                throw new NullPointerException("maskNullKey does not allow null keys");
            }
            return super.maskNullKey(key);
        }
    }

    /**
     * Subclass that deliberately throws NPE from {@code maskNullValue} when the value is {@code null}.
     */
    private static final class MaskNullValueThrowsMap extends AbstractConcurrentNullSafeMap<String, String> {

        MaskNullValueThrowsMap(ConcurrentMap<Object, Object> internalMap) {
            super(internalMap);
        }

        @Override
        protected Object maskNullValue(String value) {
            if (value == null) {
                throw new NullPointerException("maskNullValue does not allow null values");
            }
            return super.maskNullValue(value);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Mockito creates the mock for us (annotated with @Mock).
        mapUnderTest = new TestConcurrentNullSafeMap(mockInternalMap);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mock interactions to keep each test isolated.
        reset(mockInternalMap);
    }

    private static Stream<Arguments> absentKeyProvider() {
        return Stream.of(Arguments.of("alpha", "42"), Arguments.of("beta", "initial"));
    }

    @Test
    void putIfAbsent_WithNonNullKeyAndValue_WhenAbsent_ReturnsNull() {
        // GIVEN
        String key = "myKey";
        String value = "myValue";
        when(mockInternalMap.putIfAbsent(key, value)).thenReturn(null);
        // WHEN
        String result = mapUnderTest.putIfAbsent(key, value);
        // THEN
        assertNull(result, "Result should be null when the key was absent");
        verify(mockInternalMap, times(1)).putIfAbsent(key, value);
    }

    @Test
    void putIfAbsent_WithNonNullKeyAndValue_WhenPresent_ReturnsPreviousValue() {
        // GIVEN
        String key = "existingKey";
        String newValue = "newValue";
        String previousValue = "oldValue";
        when(mockInternalMap.putIfAbsent(key, newValue)).thenReturn(previousValue);
        // WHEN
        String result = mapUnderTest.putIfAbsent(key, newValue);
        // THEN
        assertEquals(previousValue, result, "Result should be the previously associated value");
        verify(mockInternalMap, times(1)).putIfAbsent(key, newValue);
    }

    @Test
    void putIfAbsent_WithNullKeyAndValue_UsesMaskedSentinels() {
        // GIVEN
        // The concrete implementation of maskNullKey/value returns sentinel objects that are
        // guaranteed to be non‑null. We don't need the actual sentinel class – we only need to
        // confirm that the internal map receives non‑null arguments.
        when(mockInternalMap.putIfAbsent(any(), any())).thenReturn(null);
        // WHEN
        String result = mapUnderTest.putIfAbsent(null, null);
        // THEN
        assertNull(result, "Result should be null when both key and value are null and absent");
        // Capture the arguments passed to the internal map.
        ArgumentCaptor<Object> keyCaptor = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Object> valueCaptor = ArgumentCaptor.forClass(Object.class);
        verify(mockInternalMap, times(1)).putIfAbsent(keyCaptor.capture(), valueCaptor.capture());
        // The masked arguments must not be null (they are sentinel objects).
        assertNotNull(keyCaptor.getValue(), "Masked key should not be null");
        assertNotNull(valueCaptor.getValue(), "Masked value should not be null");
    }

    /**
     * Simulates two threads calling {@code putIfAbsent} on the same key.
     * The mock is configured to return {@code null} on the first call (key absent)
     * and the previously stored value on the second call.
     */
    @Test
    void putIfAbsent_ConcurrentBehavior_Simulated() {
        // GIVEN
        String key = "c";
        String firstValue = "5";
        String secondValue = "6";
        // The mock will return null for the first invocation and "5" for the second.
        when(mockInternalMap.putIfAbsent(any(), any())).thenReturn(// first thread inserts
        null).thenReturn(// second thread sees existing value
        "5");
        // WHEN – first simulated thread
        String firstResult = mapUnderTest.putIfAbsent(key, firstValue);
        // WHEN – second simulated thread
        String secondResult = mapUnderTest.putIfAbsent(key, secondValue);
        // THEN
        assertNull(firstResult, "First insertion should return null (key was absent)");
        assertEquals("5", secondResult, "Second insertion should return the existing value");
        verify(mockInternalMap, times(2)).putIfAbsent(any(), any());
    }

    /**
     * Parameterized test for absent keys returning {@code null}.
     * Covers the JSON group “putIfAbsent - absent key returns null”.
     */
    @ParameterizedTest(name = "[{index}] key={0}, value={1}")
    @MethodSource("absentKeyProvider")
    void putIfAbsent_AbsentKey_ReturnsNull(String key, String value) {
        // GIVEN
        when(mockInternalMap.putIfAbsent(any(), any())).thenReturn(null);
        // WHEN
        String result = mapUnderTest.putIfAbsent(key, value);
        // THEN
        assertNull(result, "Expected null when the key is absent");
        verify(mockInternalMap, times(1)).putIfAbsent(any(), any());
    }

    /**
     * Verifies that constructing the map with a {@code null} internal map leads to a
     * {@link NullPointerException} when {@code putIfAbsent} is invoked.
     */
    @Test
    void putIfAbsent_InternalMapIsNull_ThrowsNPE() {
        // GIVEN
        TestConcurrentNullSafeMap mapWithNullInternal = new TestConcurrentNullSafeMap(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> mapWithNullInternal.putIfAbsent("anyKey", "123"), "Expected NPE when internalMap is null");
    }

    /**
     * Ensures that an {@link UnsupportedOperationException} thrown by the underlying map
     * propagates out of {@code putIfAbsent}.
     */
    @Test
    void putIfAbsent_InternalMapThrowsUnsupportedOperationException_Propagates() {
        // GIVEN
        when(mockInternalMap.putIfAbsent(any(), any())).thenThrow(new UnsupportedOperationException("operation not supported"));
        // WHEN / THEN
        assertThrows(UnsupportedOperationException.class, () -> mapUnderTest.putIfAbsent("test", "value"), "Exception from internal map should propagate");
    }

    /**
     * Simulates a subclass whose {@code maskNullKey} implementation rejects {@code null}
     * keys by throwing {@link NullPointerException}. The exception should propagate.
     */
    @Test
    void putIfAbsent_MaskNullKeyThrowsNPE_Propagates() {
        // GIVEN
        MaskNullKeyThrowsMap map = new MaskNullKeyThrowsMap(mockInternalMap);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> map.putIfAbsent(null, "someValue"), "maskNullKey NPE should propagate");
    }

    /**
     * Simulates a subclass whose {@code maskNullValue} implementation rejects {@code null}
     * values by throwing {@link NullPointerException}. The exception should propagate.
     */
    @Test
    void putIfAbsent_MaskNullValueThrowsNPE_Propagates() {
        // GIVEN
        MaskNullValueThrowsMap map = new MaskNullValueThrowsMap(mockInternalMap);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> map.putIfAbsent("someKey", null), "maskNullValue NPE should propagate");
    }
}
