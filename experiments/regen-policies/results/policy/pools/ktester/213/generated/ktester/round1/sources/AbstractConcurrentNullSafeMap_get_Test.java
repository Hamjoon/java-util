package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.ConcurrentMap;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_get_Test {

    /**
     * Mocked backing map used by the concrete test instance.
     */
    @Mock
    private ConcurrentMap<Object, Object> mockInternalMap;

    /**
     * Concrete instance of the abstract class under test.
     */
    private AbstractConcurrentNullSafeMap<String, String> mapUnderTest;

    /**
     * Simple holder for arguments used in null‑key / non‑null‑key tests.
     */
    private static class ArgumentsHolder {

        final Object key;

        final Object internalValue;

        final boolean expectNull;

        final String description;

        ArgumentsHolder(Object key, Object internalValue, boolean expectNull, String description) {
            this.key = key;
            this.internalValue = internalValue;
            this.expectNull = expectNull;
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    /**
     * Holder for key‑type‑compatibility scenarios.
     */
    private static class KeyCompatibilityHolder {

        final Class<?> keyClass;

        final String expectedValue;

        final Object key;

        KeyCompatibilityHolder(Class<?> keyClass, String expectedValue, Object key) {
            this.keyClass = keyClass;
            this.expectedValue = expectedValue;
            this.key = key;
        }

        @Override
        public String toString() {
            return "keyClass=" + keyClass.getSimpleName() + ", key=" + key;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create an anonymous concrete subclass that simply forwards to the mocked internal map.
        mapUnderTest = new AbstractConcurrentNullSafeMap<String, String>(mockInternalMap) {
        };
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the mock to avoid cross‑test interference.
        reset(mockInternalMap);
    }

    /**
     * Data provider for NullKeyHandling group.
     */
    private static Stream<ArgumentsHolder> nullKeyHandlingProvider() {
        return Stream.of(new ArgumentsHolder(null, "value1", false, "When the supplied key is null, the internal map contains a mapping for the masked null key and returns a non‑null value."), new ArgumentsHolder(null, null, true, "When the supplied key is null and the internal map has no entry for the masked‑null key, get(null) must return null."));
    }

    /**
     * Data provider for NonNullKeyHandling group.
     */
    private static Stream<ArgumentsHolder> nonNullKeyHandlingProvider() {
        return Stream.of(new ArgumentsHolder("presentKey", "realValue", false, "Key exists with a non‑null value."), new ArgumentsHolder("absentKey", null, true, "Key does not exist; internalMap returns null."), new ArgumentsHolder("nullValueKey", NullSentinel.NULL_VALUE, false, "Key exists but its associated value is null (sentinel)."));
    }

    /**
     * Data provider for KeyTypeCompatibility group.
     */
    private static Stream<KeyCompatibilityHolder> keyCompatibilityProvider() {
        return Stream.of(new KeyCompatibilityHolder(Number.class, "theAnswer", 42), new KeyCompatibilityHolder(CharSequence.class, "seqValue", "charSeq"), new KeyCompatibilityHolder(CharSequence.class, "builderValue", new StringBuilder("builder")));
    }

    @Test
    void get_ReturnsValue_ForNonNullKeyAndNonNullValue() {
        // GIVEN
        String key = "alpha";
        String expected = "value1";
        when(mockInternalMap.get(key)).thenReturn(expected);
        // WHEN
        String actual = mapUnderTest.get(key);
        // THEN
        assertEquals(expected, actual, "The map should return the value stored for a regular (non‑null) key.");
        verify(mockInternalMap, times(1)).get(key);
    }

    @Test
    void get_ReturnsNull_WhenKeyNotPresent() {
        // GIVEN
        String missingKey = "doesNotExist";
        when(mockInternalMap.get(missingKey)).thenReturn(null);
        // WHEN
        String actual = mapUnderTest.get(missingKey);
        // THEN
        assertNull(actual, "A missing key should result in a null return value.");
        verify(mockInternalMap, times(1)).get(missingKey);
    }

    @Test
    void get_HandlesNullKey_WithNonNullValue() {
        // GIVEN
        // sentinel used to represent a null key
        Object nullKeyMask = NullSentinel.NULL_KEY;
        String expected = "valueForNullKey";
        when(mockInternalMap.get(nullKeyMask)).thenReturn(expected);
        // WHEN
        String actual = mapUnderTest.get(null);
        // THEN
        assertEquals(expected, actual, "The map should correctly return the value associated with a null key.");
        verify(mockInternalMap, times(1)).get(nullKeyMask);
    }

    @Test
    void get_HandlesNonNullKey_WithNullValue() {
        // GIVEN
        String key = "beta";
        // sentinel used to represent a null value
        Object nullValueMask = NullSentinel.NULL_VALUE;
        when(mockInternalMap.get(key)).thenReturn(nullValueMask);
        // WHEN
        String actual = mapUnderTest.get(key);
        // THEN
        assertNull(actual, "A stored null value should be unmasked back to a Java null.");
        verify(mockInternalMap, times(1)).get(key);
    }

    @Test
    void get_HandlesNullKey_WithNullValue() {
        // GIVEN
        Object nullKeyMask = NullSentinel.NULL_KEY;
        Object nullValueMask = NullSentinel.NULL_VALUE;
        when(mockInternalMap.get(nullKeyMask)).thenReturn(nullValueMask);
        // WHEN
        String actual = mapUnderTest.get(null);
        // THEN
        assertNull(actual, "A null key mapped to a null value should be returned as null.");
        verify(mockInternalMap, times(1)).get(nullKeyMask);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("nullKeyHandlingProvider")
    void get_NullKeyHandling(ArgumentsHolder holder) {
        Object maskedKey = NullSentinel.NULL_KEY;
        when(mockInternalMap.get(maskedKey)).thenReturn(holder.internalValue);
        String actual = mapUnderTest.get(holder.key);
        if (holder.expectNull) {
            assertNull(actual);
        } else {
            assertEquals(holder.internalValue, actual);
        }
        verify(mockInternalMap, times(1)).get(maskedKey);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("nonNullKeyHandlingProvider")
    void get_NonNullKeyHandling(ArgumentsHolder holder) {
        Object internalVal = holder.internalValue;
        when(mockInternalMap.get(holder.key)).thenReturn(internalVal);
        String actual = mapUnderTest.get(holder.key);
        // If the internal value is the NULL_VALUE sentinel we expect a Java null.
        if (internalVal == NullSentinel.NULL_VALUE) {
            assertNull(actual);
        } else if (holder.expectNull) {
            assertNull(actual);
        } else {
            assertEquals(internalVal, actual);
        }
        verify(mockInternalMap, times(1)).get(holder.key);
    }

    @Test
    void get_InvalidKeyType_ThrowsClassCastException() {
        // mapUnderTest is declared as <String, String>. Supplying an Integer should fail at the cast.
        Integer badKey = 12345;
        assertThrows(ClassCastException.class, () -> mapUnderTest.get(badKey));
    }

    @Test
    void get_EmptyMap_ReturnsNull() {
        // The mock returns null for any key.
        when(mockInternalMap.get(any())).thenReturn(null);
        String actual = mapUnderTest.get("anyKey");
        assertNull(actual);
        verify(mockInternalMap, times(1)).get("anyKey");
    }

    @Test
    void get_NullKeyWithNullValue_ReturnsNull() {
        Object maskedKey = NullSentinel.NULL_KEY;
        Object nullValueMask = NullSentinel.NULL_VALUE;
        when(mockInternalMap.get(maskedKey)).thenReturn(nullValueMask);
        String actual = mapUnderTest.get(null);
        assertNull(actual);
        verify(mockInternalMap, times(1)).get(maskedKey);
    }

    @ParameterizedTest(name = "[{index}] key type {0}")
    @MethodSource("keyCompatibilityProvider")
    void get_KeyTypeCompatibility(KeyCompatibilityHolder holder) {
        // Create a map instance with the required generic key type.
        AbstractConcurrentNullSafeMap<?, String> map = new AbstractConcurrentNullSafeMap<Object, String>(mockInternalMap) {
        };
        // The abstract class masks only null keys, so non‑null keys are passed through unchanged.
        when(mockInternalMap.get(holder.key)).thenReturn(holder.expectedValue);
        @SuppressWarnings("unchecked")
        String actual = ((AbstractConcurrentNullSafeMap<Object, String>) map).get(holder.key);
        assertEquals(holder.expectedValue, actual);
        verify(mockInternalMap, times(1)).get(holder.key);
    }

    @Test
    void get_NoSideEffectOnMapSize() {
        // Arrange: map size is 5 before the call.
        when(mockInternalMap.size()).thenReturn(5);
        when(mockInternalMap.get("presentKey")).thenReturn("presentValue");
        int sizeBefore = mapUnderTest.size();
        String value = mapUnderTest.get("presentKey");
        int sizeAfter = mapUnderTest.size();
        assertEquals(5, sizeBefore, "Size before get should be 5");
        assertEquals(5, sizeAfter, "Size after get should remain unchanged");
        assertEquals("presentValue", value);
        // once before, once after
        verify(mockInternalMap, times(2)).size();
        verify(mockInternalMap, times(1)).get("presentKey");
    }

    @Test
    void get_NullInternalMap_ThrowsNullPointerException() {
        // Create a concrete subclass with a null internal map.
        AbstractConcurrentNullSafeMap<String, String> brokenMap = new AbstractConcurrentNullSafeMap<String, String>(null) {
        };
        assertThrows(NullPointerException.class, () -> brokenMap.get("anyKey"));
    }

    @Test
    void get_ValueTypeMismatch_ThrowsClassCastException() {
        // Map declared as <String, Integer>, but internal map returns a String.
        AbstractConcurrentNullSafeMap<String, Integer> map = new AbstractConcurrentNullSafeMap<String, Integer>(mockInternalMap) {
        };
        when(mockInternalMap.get("mismatchKey")).thenReturn("notAnInteger");
        assertThrows(ClassCastException.class, () -> map.get("mismatchKey"));
        verify(mockInternalMap, times(1)).get("mismatchKey");
    }
}
