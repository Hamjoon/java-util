package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.util.concurrent.ConcurrentMap;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_putIfAbsent_Test {

    @Mock
    private ConcurrentMap<Object, Object> // mocked backing map
    mockInternal;

    // instance under test
    private TestMap testMap;

    /**
     * Minimal concrete subclass used with a mocked backing map.
     */
    private static final class TestMap extends AbstractConcurrentNullSafeMap<String, String> {

        TestMap(ConcurrentMap<Object, Object> internal) {
            super(internal);
        }
    }

    /**
     * Concrete subclass that uses a real ConcurrentHashMap (for size tests).
     */
    private static final class RealMap extends AbstractConcurrentNullSafeMap<String, String> {

        RealMap() {
            super(new ConcurrentHashMap<>());
        }
    }

    /**
     * Subclass that deliberately returns {@code null} from {@code maskNullKey}.
     */
    private static final class BadMaskKeyMap extends AbstractConcurrentNullSafeMap<String, String> {

        BadMaskKeyMap(ConcurrentMap<Object, Object> internal) {
            super(internal);
        }

        @Override
        protected Object maskNullKey(String key) {
            // break the contract deliberately
            return null;
        }
    }

    /**
     * Subclass that deliberately returns {@code null} from {@code maskNullValue}.
     */
    private static final class BadMaskValueMap extends AbstractConcurrentNullSafeMap<String, String> {

        BadMaskValueMap(ConcurrentMap<Object, Object> internal) {
            super(internal);
        }

        @Override
        protected Object maskNullValue(String value) {
            // break the contract deliberately
            return null;
        }
    }

    /**
     * Simple key wrapper that uses logical equality based on an integer id.
     */
    private static final class KeyWrapper {

        private final int id;

        KeyWrapper(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof KeyWrapper) && ((KeyWrapper) o).id == this.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public String toString() {
            return "KeyWrapper{id=" + id + '}';
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Mockito creates the mockInternal automatically because of @Mock
        testMap = new TestMap(mockInternal);
    }

    private static Stream<Arguments> basicScenarios() {
        return Stream.of(// group: putIfAbsent - basic scenarios
        Arguments.of("alpha", "first", null, "Key 'alpha' is not present; should insert and return null"), Arguments.of("alpha", "second", "first", "Key 'alpha' already present with 'first'; should return existing value"), Arguments.of(null, "nullKeyValue", null, "Null key absent; entry added and null returned"), Arguments.of("nullValueKey", null, null, "Non‑null key with null value; entry added and null returned"), Arguments.of(null, null, null, "Both key and value null; entry added and null returned"), Arguments.of(null, "anotherValue", "nullKeyValue", "Null key already present with value 'nullKeyValue'; should return it"), Arguments.of("nullValueKey", null, "first", "Key 'nullValueKey' already present with 'first'; null value attempt returns existing"));
    }

    private static Stream<Arguments> existingNullValueScenarios() {
        return Stream.of(Arguments.of("beta", "newValue", null, "Key 'beta' present with null value; putIfAbsent must return null"), Arguments.of("beta", null, null, "Key 'beta' present with null value; putting null again returns null"), Arguments.of("gamma", "val", "val", "Key 'gamma' maps to \"val\"; identical value returns existing"), Arguments.of("gamma", new String("val"), "val", "Key 'gamma' maps to \"val\"; equal but different instance returns existing"));
    }

    @Test
    void putIfAbsent_delegatesToInternalMap_andReturnsPreviousValue() {
        // GIVEN
        String key = "key1";
        String newValue = "value1";
        String previousValue = "oldValue";
        // internal map should return the previous value unchanged (already un‑masked)
        when(mockInternal.putIfAbsent(key, newValue)).thenReturn(previousValue);
        // WHEN
        String result = testMap.putIfAbsent(key, newValue);
        // THEN
        assertEquals(previousValue, result, "putIfAbsent should return the previous value");
        // verify that the internal map received exactly the same (non‑null) arguments
        verify(mockInternal, times(1)).putIfAbsent(key, newValue);
        verifyNoMoreInteractions(mockInternal);
    }

    @Test
    void putIfAbsent_handlesNullKeyAndValue_usingSentinels() {
        // GIVEN
        // stand‑in for the internal NullSentinel
        Object sentinel = new Object();
        // The abstract class masks nulls, so the internal map will receive non‑null objects.
        // We cannot refer to the real sentinel class, but we can assert that the arguments
        // passed to the mock are not the original nulls.
        when(mockInternal.putIfAbsent(any(), any())).thenReturn(sentinel);
        // WHEN
        String result = testMap.putIfAbsent(null, null);
        // THEN
        // The method should unmask the sentinel and therefore return null.
        assertNull(result, "When the previous value is the null‑sentinel, the returned value must be null");
        // Capture the arguments passed to the mock to ensure they are not null.
        ArgumentCaptor<Object> keyCaptor = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Object> valueCaptor = ArgumentCaptor.forClass(Object.class);
        verify(mockInternal, times(1)).putIfAbsent(keyCaptor.capture(), valueCaptor.capture());
        assertNotNull(keyCaptor.getValue(), "Masked key must not be null (sentinel should be used)");
        assertNotNull(valueCaptor.getValue(), "Masked value must not be null (sentinel should be used)");
        verifyNoMoreInteractions(mockInternal);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("basicScenarios")
    void putIfAbsent_basicScenarios(Object key, String value, Object expected, String description) {
        // Arrange mock behaviour:
        // For absent entries the delegate returns null; for present entries we return the stored value.
        Object maskedReturn = expected instanceof String ? expected : null;
        // When the expected result is null but the previous value could be a sentinel,
        // we let the mock return a sentinel object and rely on unmasking to produce null.
        Object delegateReturn = (expected == null && (key == null || value == null)) ? // sentinel for null previous value
        new Object() : maskedReturn;
        when(mockInternal.putIfAbsent(any(), any())).thenReturn(delegateReturn);
        // Act
        String result = testMap.putIfAbsent((String) key, value);
        // Assert
        if (expected == null) {
            assertNull(result, description);
        } else {
            assertEquals(expected, result, description);
        }
        // Verify that the internal map never receives raw nulls (they must be masked)
        ArgumentCaptor<Object> keyCap = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Object> valCap = ArgumentCaptor.forClass(Object.class);
        verify(mockInternal, times(1)).putIfAbsent(keyCap.capture(), valCap.capture());
        assertNotNull(keyCap.getValue(), "Masked key must be non‑null");
        assertNotNull(valCap.getValue(), "Masked value must be non‑null");
        verifyNoMoreInteractions(mockInternal);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("existingNullValueScenarios")
    void putIfAbsent_existingNullValues(Object key, String value, Object expected, String description) {
        // For a stored null value the delegate map will hold a sentinel object.
        Object sentinel = new Object();
        Object delegateReturn = (expected == null) ? sentinel : expected;
        when(mockInternal.putIfAbsent(any(), any())).thenReturn(delegateReturn);
        String result = testMap.putIfAbsent((String) key, value);
        if (expected == null) {
            assertNull(result, description);
        } else {
            assertEquals(expected, result, description);
        }
        // Ensure masking of nulls (arguments must be non‑null)
        ArgumentCaptor<Object> keyCap = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Object> valCap = ArgumentCaptor.forClass(Object.class);
        verify(mockInternal, times(1)).putIfAbsent(keyCap.capture(), valCap.capture());
        assertNotNull(keyCap.getValue(), "Masked key must be non‑null");
        assertNotNull(valCap.getValue(), "Masked value must be non‑null");
        verifyNoMoreInteractions(mockInternal);
    }

    @Test
    void putIfAbsent_customKeyWrapper_equalityAndSize() {
        RealMap realMap = new RealMap();
        // 1️⃣ Insert first entry with a custom key
        KeyWrapper k1 = new KeyWrapper(1);
        assertNull(realMap.putIfAbsent(k1, "one"), "First insertion with custom key should return null");
        assertEquals(1, realMap.size(), "Map size should be 1 after first insertion");
        // 2️⃣ Attempt insertion with a different instance but same logical key
        KeyWrapper k2 = new KeyWrapper(1);
        assertEquals("one", realMap.putIfAbsent(k2, "different"), "Second insertion with equal key must return original value");
        assertEquals(1, realMap.size(), "Map size must stay unchanged after duplicate key");
        // 3️⃣ Size‑related tests with a plain String key
        assertNull(realMap.putIfAbsent("delta", "first"), "First insertion of 'delta' should return null");
        assertEquals(2, realMap.size(), "Map size should increase after new key");
        assertEquals("first", realMap.putIfAbsent("delta", "second"), "Second insertion of same key must return existing value");
        assertEquals(2, realMap.size(), "Map size must stay unchanged after duplicate key");
    }

    @Test
    void putIfAbsent_internalMapIsNull_throwsNPE() {
        TestMap nullMap = new TestMap(null);
        assertThrows(NullPointerException.class, () -> nullMap.putIfAbsent("anyKey", "anyValue"), "Calling putIfAbsent with a null internalMap must throw NPE");
    }

    @Test
    void putIfAbsent_internalMapThrowsUnsupportedOperationException_propagates() {
        when(mockInternal.putIfAbsent(any(), any())).thenThrow(new UnsupportedOperationException("unsupported"));
        assertThrows(UnsupportedOperationException.class, () -> testMap.putIfAbsent("key", "value"), "UnsupportedOperationException must propagate unchanged");
    }

    @Test
    void putIfAbsent_internalMapThrowsClassCastException_propagates() {
        when(mockInternal.putIfAbsent(any(), any())).thenThrow(new ClassCastException("bad key type"));
        assertThrows(ClassCastException.class, () -> testMap.putIfAbsent("key", "value"), "ClassCastException must propagate unchanged");
    }

    @Test
    void putIfAbsent_maskNullKeyReturnsNull_throwsNPE() {
        // Use a real ConcurrentHashMap as the delegate (it rejects null keys)
        BadMaskKeyMap map = new BadMaskKeyMap(new ConcurrentHashMap<>());
        assertThrows(NullPointerException.class, () -> map.putIfAbsent("anyKey", "anyValue"), "If maskNullKey returns null, the underlying map receives a null key and throws NPE");
    }

    @Test
    void putIfAbsent_maskNullValueReturnsNull_throwsNPE() {
        BadMaskValueMap map = new BadMaskValueMap(new ConcurrentHashMap<>());
        assertThrows(NullPointerException.class, () -> map.putIfAbsent("anyKey", "anyValue"), "If maskNullValue returns null, the underlying map receives a null value and throws NPE");
    }
}
