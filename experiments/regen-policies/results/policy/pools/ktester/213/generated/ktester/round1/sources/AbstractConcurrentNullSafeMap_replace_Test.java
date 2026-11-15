package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.ConcurrentMap;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_replace_Test {

    /**
     * Mocked backing map used to capture interactions.
     */
    @Mock
    private ConcurrentMap<Object, Object> internalMapMock;

    /**
     * Concrete instance of the abstract class under test.
     */
    private AbstractConcurrentNullSafeMap<String, String> testMap;

    /**
     * Concrete instance of the abstract class under test (delegation variant).
     */
    private AbstractConcurrentNullSafeMap<String, String> delegatingMap;

    /**
     * Simple concrete subclass that only forwards the constructor.
     */
    private static class TestMap<K, V> extends AbstractConcurrentNullSafeMap<K, V> {

        TestMap(ConcurrentMap<Object, Object> internal) {
            super(internal);
        }
    }

    /**
     * Concrete subclass that uses a real ConcurrentHashMap for functional tests.
     */
    private static class RealMap<K, V> extends AbstractConcurrentNullSafeMap<K, V> {

        RealMap() {
            super(new ConcurrentHashMap<>());
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise the delegating map with the mocked internal map.
        delegatingMap = new TestMap<>(internalMapMock);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the mock to avoid cross‑test interference.
        reset(internalMapMock);
    }

    /**
     * Helper that captures the arguments passed to {@code internalMap.replace}
     * and returns the value that the mock is configured to return.
     */
    private boolean invokeReplace(String key, String oldValue, String newValue) {
        // Configure the mock to return true for any replace call.
        when(internalMapMock.replace(any(), any(), any())).thenReturn(true);
        return testMap.replace(key, oldValue, newValue);
    }

    /**
     * Provides arguments for functional replace tests.
     *
     * Each {@link Arguments} contains:
     * - key (String or null)
     * - oldValue (String or null)
     * - newValue (String or null)
     * - pre‑existing value in the map before replace (String or null)
     * - expected boolean result from replace
     * - description (for readability only)
     */
    private static Stream<Arguments> functionalReplaceProvider() {
        return Stream.of(// matching old value (all non‑null)
        Arguments.of("alpha", "old", "new", "old", true, "replace with matching old value (all non‑null)"), // non‑matching old value (all non‑null)
        Arguments.of("beta", "wrong", "newValue", "correct", false, "replace with non‑matching old value (all non‑null)"), // null key handling – match
        Arguments.of(null, "nullKeyOld", "nullKeyNew", "nullKeyOld", true, "replace when key is null and old matches"), // null key handling – no match
        Arguments.of(null, "different", "ignored", "actualValue", false, "replace when key is null and old does not match"), // null oldValue – match (value stored is null)
        Arguments.of("gamma", null, "newAfterNull", null, true, "replace when oldValue is null and stored value is null"), // null oldValue – no match (stored non‑null)
        Arguments.of("delta", null, "newValue", "present", false, "replace when oldValue is null but stored value is non‑null"), // null newValue – successful replace
        Arguments.of("epsilon", "toBeNull", null, "toBeNull", true, "replace when newValue is null and old matches"), // null newValue – fail because old does not match
        Arguments.of("zeta", "wrong", null, "right", false, "replace when newValue is null but old does not match"), // null key + null old + non‑null new – match
        Arguments.of(null, null, "nonNullAfterNull", null, true, "replace null key & old null, new non‑null"), // null key + null old + null new – match (no change)
        Arguments.of(null, null, null, null, true, "replace null key & old null, new null (no change)"), // null key + non‑null old + null new – fail
        Arguments.of(null, "someValue", null, null, false, "replace null key with non‑null old that does not match"), // absent key
        Arguments.of("missingKey", "anyOld", "anyNew", null, false, "replace with absent key"), // matching old value and identical new value
        Arguments.of("theta", "constant", "constant", "constant", true, "replace with matching old value and identical new value"), // null key with matching non‑null old & new
        Arguments.of(null, "oldNullKeyValue", "newNullKeyValue", "oldNullKeyValue", true, "replace null key with matching non‑null old and new"), // null key with non‑matching old
        Arguments.of(null, "wrongOld", "newValue", "actualValue", false, "replace null key with non‑matching old"), // non‑null key where current value is null, new non‑null
        Arguments.of("iota", null, "nowNotNull", null, true, "replace non‑null key where current value is null, to non‑null new"), // non‑null key where current value is null, new null
        Arguments.of("kappa", null, null, null, true, "replace non‑null key where current value is null, new null"), // non‑null key where current value non‑null, old null (no match) -> new null
        Arguments.of("lambda", null, null, "present", false, "replace non‑null key where stored non‑null, old null (no match)"), // non‑null key where current value non‑null, old null (no match) -> new non‑null
        Arguments.of("mu", null, "newAfterFailedNullOld", "existing", false, "replace non‑null key where stored non‑null, old null (no match) with new non‑null"));
    }

    @Test
    void replace_AllNonNull_ShouldDelegateUnchanged() {
        // Arrange
        String key = "k1";
        String oldVal = "old";
        String newVal = "new";
        // Act
        boolean result = delegatingMap.replace(key, oldVal, newVal);
        // Assert
        assertTrue(result, "The return value should be the one from the internal map.");
        verify(internalMapMock, times(1)).replace(key, oldVal, newVal);
    }

    @Test
    void replace_NullKey_ShouldMaskKeyOnly() {
        // Arrange
        String oldVal = "old";
        String newVal = "new";
        // Act
        delegatingMap.replace(null, oldVal, newVal);
        // Assert
        ArgumentCaptor<Object> keyCaptor = ArgumentCaptor.forClass(Object.class);
        verify(internalMapMock, times(1)).replace(keyCaptor.capture(), eq(oldVal), eq(newVal));
        Object maskedKey = keyCaptor.getValue();
        assertNotNull(maskedKey, "Null key must be masked to a non‑null sentinel.");
        assertNotSame(null, maskedKey, "The masked key must not be the literal null.");
    }

    @Test
    void replace_NullOldValue_ShouldMaskOldValueOnly() {
        // Arrange
        String key = "k2";
        String newVal = "new";
        // Act
        delegatingMap.replace(key, null, newVal);
        // Assert
        ArgumentCaptor<Object> oldValCaptor = ArgumentCaptor.forClass(Object.class);
        verify(internalMapMock, times(1)).replace(eq(key), oldValCaptor.capture(), eq(newVal));
        Object maskedOld = oldValCaptor.getValue();
        assertNotNull(maskedOld, "Null oldValue must be masked to a non‑null sentinel.");
    }

    @Test
    void replace_NullNewValue_ShouldMaskNewValueOnly() {
        // Arrange
        String key = "k3";
        String oldVal = "old";
        // Act
        delegatingMap.replace(key, oldVal, null);
        // Assert
        ArgumentCaptor<Object> newValCaptor = ArgumentCaptor.forClass(Object.class);
        verify(internalMapMock, times(1)).replace(eq(key), eq(oldVal), newValCaptor.capture());
        Object maskedNew = newValCaptor.getValue();
        assertNotNull(maskedNew, "Null newValue must be masked to a non‑null sentinel.");
    }

    @Test
    void replace_AllNull_ShouldMaskAllArguments() {
        // Act
        delegatingMap.replace(null, null, null);
        // Assert
        ArgumentCaptor<Object> keyCaptor = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Object> oldCaptor = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Object> newCaptor = ArgumentCaptor.forClass(Object.class);
        verify(internalMapMock, times(1)).replace(keyCaptor.capture(), oldCaptor.capture(), newCaptor.capture());
        assertAll(() -> assertNotNull(keyCaptor.getValue(), "Key sentinel must not be null"), () -> assertNotNull(oldCaptor.getValue(), "Old value sentinel must not be null"), () -> assertNotNull(newCaptor.getValue(), "New value sentinel must not be null"));
    }

    @Test
    void replace_ReturnValuePropagation() {
        // Arrange
        when(internalMapMock.replace(any(), any(), any())).thenReturn(false);
        // Act
        boolean result = delegatingMap.replace("k", "old", "new");
        // Assert
        assertFalse(result, "The method must return the exact boolean from the internal map.");
        verify(internalMapMock, times(1)).replace("k", "old", "new");
    }

    @ParameterizedTest(name = "{5}")
    @MethodSource("functionalReplaceProvider")
    void replace_FunctionalBehaviour(String key, String oldValue, String newValue, String preExisting, boolean expectedResult, String _description) {
        // Arrange: use a real map
        RealMap<String, String> map = new RealMap<>();
        // Populate the map with the pre‑existing value if any.
        if (preExisting != null || (preExisting == null && (key != null || oldValue == null))) {
            // The map's put method handles null masking internally.
            map.put(key, preExisting);
        }
        // Act
        boolean result = map.replace(key, oldValue, newValue);
        // Assert return value
        assertEquals(expectedResult, result, () -> "Unexpected replace result for case: " + _description);
        // Verify final state only when replace succeeded (as per ConcurrentMap contract)
        if (expectedResult) {
            assertEquals(newValue, map.get(key), () -> "Map value after successful replace does not match expected new value.");
        } else {
            // When replace fails the value must stay as preExisting
            assertEquals(preExisting, map.get(key), () -> "Map value changed despite replace failure.");
        }
    }

    @Test
    void replace_WhenInternalMapIsNull_ShouldThrowNPE() {
        // Create a subclass that deliberately passes null to the super‑constructor.
        AbstractConcurrentNullSafeMap<String, String> mapWithNullInternal = new AbstractConcurrentNullSafeMap<String, String>(null) {
        };
        // Act & Assert
        assertThrows(NullPointerException.class, () -> mapWithNullInternal.replace("anyKey", "old", "new"), "Calling replace on a map with a null internalMap must throw NPE.");
    }

    @Test
    void replace_WhenUnderlyingMapDoesNotSupportReplace_ShouldPropagateUnsupportedOperationException() {
        // Mock that throws UnsupportedOperationException on replace.
        @SuppressWarnings("unchecked")
        ConcurrentMap<Object, Object> unsupportedMap = mock(ConcurrentMap.class);
        when(unsupportedMap.replace(any(), any(), any())).thenThrow(new UnsupportedOperationException("replace not supported"));
        AbstractConcurrentNullSafeMap<String, String> map = new TestMap<>(unsupportedMap);
        // Act & Assert
        assertThrows(UnsupportedOperationException.class, () -> map.replace("keyUnsupported", "oldVal", "newVal"), "UnsupportedOperationException should be propagated from the underlying map.");
    }

    @Test
    void replace_WhenUnderlyingMapThrowsClassCastException_ShouldPropagate() {
        // Mock that throws ClassCastException when a non‑String key is used.
        @SuppressWarnings("unchecked")
        ConcurrentMap<Object, Object> classCastMap = mock(ConcurrentMap.class);
        when(classCastMap.replace(eq(12345), any(), any())).thenThrow(new ClassCastException("Key must be String"));
        AbstractConcurrentNullSafeMap<Object, String> map = new TestMap<>(classCastMap);
        // Act & Assert
        assertThrows(ClassCastException.class, () -> map.replace(12345, "oldString", "newString"), "ClassCastException from the underlying map must be propagated.");
    }
}
