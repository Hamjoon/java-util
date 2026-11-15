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
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_putIfAbsent_Test {

    /**
     * Mocked delegate map used by the concrete test implementation.
     */
    @Mock
    private ConcurrentMap<Object, Object> internalMap;

    /**
     * Concrete instance of the abstract class under test.
     */
    private TestMap<String, String> mapUnderTest;

    /**
     * Simple concrete subclass that bypasses the null‑masking logic.
     */
    private static class TestMap<K, V> extends AbstractConcurrentNullSafeMap<K, V> {

        TestMap(ConcurrentMap<Object, Object> internal) {
            super(internal);
        }

        @Override
        protected Object maskNullKey(K key) {
            // Identity – no special sentinel handling for the test
            return key;
        }

        @Override
        protected Object maskNullValue(V value) {
            // Identity – no special sentinel handling for the test
            return value;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected K unmaskNullKey(Object key) {
            return (K) key;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected V unmaskNullValue(Object value) {
            return (V) value;
        }
    }

    /**
     * Subclass that deliberately passes {@code null} as the internal map.
     */
    private static class NullInternalMap extends AbstractConcurrentNullSafeMap<String, String> {

        NullInternalMap() {
            super(null);
        }

        @Override
        protected Object maskNullKey(String key) {
            return key;
        }

        @Override
        protected Object maskNullValue(String value) {
            return value;
        }

        @Override
        protected String unmaskNullKey(Object key) {
            return (String) key;
        }

        @Override
        protected String unmaskNullValue(Object value) {
            return (String) value;
        }
    }

    /**
     * Subclass whose {@code maskNullKey} returns {@code null}.
     */
    private static class MaskKeyReturnsNull extends AbstractConcurrentNullSafeMap<String, String> {

        MaskKeyReturnsNull(ConcurrentMap<Object, Object> internal) {
            super(internal);
        }

        @Override
        protected Object maskNullKey(String key) {
            return null;
        }

        @Override
        protected Object maskNullValue(String value) {
            return value;
        }

        @Override
        protected String unmaskNullKey(Object key) {
            return (String) key;
        }

        @Override
        protected String unmaskNullValue(Object value) {
            return (String) value;
        }
    }

    /**
     * Subclass whose {@code maskNullValue} returns {@code null}.
     */
    private static class MaskValueReturnsNull extends AbstractConcurrentNullSafeMap<String, String> {

        MaskValueReturnsNull(ConcurrentMap<Object, Object> internal) {
            super(internal);
        }

        @Override
        protected Object maskNullKey(String key) {
            return key;
        }

        @Override
        protected Object maskNullValue(String value) {
            return null;
        }

        @Override
        protected String unmaskNullKey(Object key) {
            return (String) key;
        }

        @Override
        protected String unmaskNullValue(Object value) {
            return (String) value;
        }
    }

    private static class NullHandlingCase {

        final String key;

        final String value;

        final String expected;

        final String description;

        NullHandlingCase(String key, String value, String expected, String description) {
            this.key = key;
            this.value = value;
            this.expected = expected;
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh concrete map for each test, injecting the mocked internal map.
        mapUnderTest = new TestMap<>(internalMap);
    }

    @AfterEach
    void teardownAfterEach() {
        // Verify that no unexpected interactions occurred on the mock.
        verifyNoMoreInteractions(internalMap);
    }

    private static Stream<NullHandlingCase> nullHandlingProvider() {
        return Stream.of(new NullHandlingCase(null, "nullKeyValue", null, "Key is null, value non‑null – expect null previous"), new NullHandlingCase("someKey", null, null, "Key non‑null, value null – expect null previous"), new NullHandlingCase(null, null, null, "Both key and value null – expect null previous"));
    }

    @Test
    void putIfAbsent_keyAbsent_returnsNull_andDelegatesCorrectly() {
        // GIVEN
        String key = "myKey";
        String value = "myValue";
        when(internalMap.putIfAbsent(key, value)).thenReturn(null);
        // WHEN
        String result = mapUnderTest.putIfAbsent(key, value);
        // THEN
        assertNull(result, "Expected null when the key was absent");
        verify(internalMap).putIfAbsent(key, value);
    }

    @Test
    void putIfAbsent_keyPresent_returnsExistingValue_andDelegatesCorrectly() {
        // GIVEN
        String key = "existingKey";
        String newValue = "newValue";
        String oldValue = "oldValue";
        when(internalMap.putIfAbsent(key, newValue)).thenReturn(oldValue);
        // WHEN
        String result = mapUnderTest.putIfAbsent(key, newValue);
        // THEN
        assertEquals(oldValue, result, "Expected the previously associated value to be returned");
        verify(internalMap).putIfAbsent(key, newValue);
    }

    @Test
    void putIfAbsent_nullKeyAndValue_delegatesAndReturnsResult() {
        // GIVEN
        String key = null;
        String value = null;
        String previous = "previous";
        when(internalMap.putIfAbsent(null, null)).thenReturn(previous);
        // WHEN
        String result = mapUnderTest.putIfAbsent(key, value);
        // THEN
        assertEquals(previous, result, "Result should be the value returned by the internal map");
        verify(internalMap).putIfAbsent(null, null);
    }

    @Test
    void putIfAbsent_nullKeyAndValue_whenNoPrevious_returnsNull() {
        // GIVEN
        when(internalMap.putIfAbsent(null, null)).thenReturn(null);
        // WHEN
        String result = mapUnderTest.putIfAbsent(null, null);
        // THEN
        assertNull(result, "Result should be null when the internal map returns null");
        verify(internalMap).putIfAbsent(null, null);
    }

    @Test
    void putIfAbsent_nullKey_thenSecondCallReturnsExistingValue() {
        // GIVEN first call stores a null key with "first"
        when(internalMap.putIfAbsent(null, "first")).thenReturn(null);
        // second call with the same null key should return the previously stored value
        when(internalMap.putIfAbsent(null, "second")).thenReturn("first");
        // WHEN first insertion
        String firstResult = mapUnderTest.putIfAbsent(null, "first");
        // WHEN second insertion attempt
        String secondResult = mapUnderTest.putIfAbsent(null, "second");
        // THEN
        assertNull(firstResult, "First insertion should return null (no previous mapping)");
        assertEquals("first", secondResult, "Second call must return the existing value");
        verify(internalMap).putIfAbsent(null, "first");
        verify(internalMap).putIfAbsent(null, "second");
    }

    @Test
    void putIfAbsent_nonNullKey_thenNullValue_thenSecondCallReturnsExistingNullValue() {
        // GIVEN first call stores a non‑null key with a null value
        when(internalMap.putIfAbsent("someKey", null)).thenReturn(null);
        // second call with same key but non‑null value should return the previously stored null
        // internal map returns previous (null)
        when(internalMap.putIfAbsent("someKey", "newValue")).thenReturn(null);
        // WHEN first insertion
        String firstResult = mapUnderTest.putIfAbsent("someKey", null);
        // WHEN second insertion attempt
        String secondResult = mapUnderTest.putIfAbsent("someKey", "newValue");
        // THEN
        assertNull(firstResult, "First insertion should return null (no previous mapping)");
        assertNull(secondResult, "Second call must return the existing null value");
        verify(internalMap).putIfAbsent("someKey", null);
        verify(internalMap).putIfAbsent("someKey", "newValue");
    }

    @Test
    void putIfAbsent_internalMapIsNull_throwsNullPointerException() {
        // GIVEN a map whose internalMap field is null
        AbstractConcurrentNullSafeMap<String, String> map = new NullInternalMap();
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> map.putIfAbsent("alpha", "first"), "Delegating to a null internalMap must throw NPE");
    }

    @Test
    void putIfAbsent_internalMapThrowsClassCastException_propagates() {
        // GIVEN a mock that throws ClassCastException on any putIfAbsent call
        ConcurrentMap<Object, Object> throwingMap = mock(ConcurrentMap.class);
        when(throwingMap.putIfAbsent(any(), any())).thenThrow(ClassCastException.class);
        AbstractConcurrentNullSafeMap<String, String> map = new TestMap<>(throwingMap);
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> map.putIfAbsent("alpha", "first"), "ClassCastException from the internal map should propagate");
        verify(throwingMap).putIfAbsent("alpha", "first");
    }

    @Test
    void putIfAbsent_internalMapThrowsIllegalStateException_propagates() {
        // GIVEN a read‑only map that throws IllegalStateException on modification
        ConcurrentMap<Object, Object> readOnly = (ConcurrentMap<Object, Object>) Collections.unmodifiableMap(new ConcurrentHashMap<>());
        AbstractConcurrentNullSafeMap<String, String> map = new TestMap<>(readOnly);
        // WHEN / THEN
        assertThrows(IllegalStateException.class, () -> map.putIfAbsent("alpha", "first"), "Attempting to modify a read‑only map should throw IllegalStateException");
    }

    @Test
    void putIfAbsent_maskNullKeyReturnsNull_throwsNullPointerException() {
        // GIVEN a subclass whose maskNullKey returns null, and a mock that throws NPE when key is null
        when(internalMap.putIfAbsent(isNull(), any())).thenThrow(NullPointerException.class);
        AbstractConcurrentNullSafeMap<String, String> map = new MaskKeyReturnsNull(internalMap);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> map.putIfAbsent("anyKey", "first"), "maskNullKey returning null should cause NPE from internalMap");
        verify(internalMap).putIfAbsent(isNull(), eq("first"));
    }

    @Test
    void putIfAbsent_maskNullValueReturnsNull_throwsNullPointerException() {
        // GIVEN a subclass whose maskNullValue returns null, and a mock that throws NPE when value is null
        when(internalMap.putIfAbsent(any(), isNull())).thenThrow(NullPointerException.class);
        AbstractConcurrentNullSafeMap<String, String> map = new MaskValueReturnsNull(internalMap);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> map.putIfAbsent("alpha", null), "maskNullValue returning null should cause NPE from internalMap");
        verify(internalMap).putIfAbsent(eq("alpha"), isNull());
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("nullHandlingProvider")
    void putIfAbsent_nullScenarios(NullHandlingCase tc) {
        // GIVEN
        when(internalMap.putIfAbsent(tc.key, tc.value)).thenReturn(tc.expected);
        // WHEN
        String result = mapUnderTest.putIfAbsent(tc.key, tc.value);
        // THEN
        assertEquals(tc.expected, result, tc.description);
        verify(internalMap).putIfAbsent(tc.key, tc.value);
    }
}
