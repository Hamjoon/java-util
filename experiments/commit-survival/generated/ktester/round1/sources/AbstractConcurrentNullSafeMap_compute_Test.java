package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;

// kept from the template; not required for these tests
@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_compute_Test {

    private TestMap map;

    /**
     * Simple concrete implementation used only for testing the abstract class.
     */
    private static final class TestMap extends AbstractConcurrentNullSafeMap<String, String> {

        TestMap(ConcurrentHashMap<Object, Object> internal) {
            super(internal);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use a real ConcurrentHashMap as the backing store so that the
        // mask/unmask logic inside AbstractConcurrentNullSafeMap can be exercised.
        map = new TestMap(new ConcurrentHashMap<>());
    }

    @AfterEach
    void teardownAfterEach() {
        map.clear();
        map = null;
    }

    private static Stream<Arguments> computeScenarioProvider() {
        return Stream.of(// key = null, remapping returns non‑null
        Arguments.of(null, (BiFunction<String, String, String>) (k, v) -> v == null ? "default" : v + "-upd", "default"), // existing key with non‑null value, remapping appends
        Arguments.of("existing", (BiFunction<String, String, String>) (k, v) -> v + "-changed", "value-changed"), // existing key with null value, remapping creates first value
        Arguments.of("nullValueKey", (BiFunction<String, String, String>) (k, v) -> v == null ? "first" : v, "first"));
    }

    @Test
    void computeAddsNewEntryWhenAbsent() {
        // given
        String key = "alpha";
        BiFunction<String, String, String> remapping = (k, v) -> "first";
        // when
        String result = map.compute(key, remapping);
        // then
        assertEquals("first", result, "compute should return the value produced by the remapping function");
        assertEquals("first", map.get(key), "the new value must be stored in the map");
    }

    @Test
    void computeReplacesExistingValueUsingOldValue() {
        // given
        String key = "beta";
        map.put(key, "old");
        BiFunction<String, String, String> remapping = (k, v) -> v + "-new";
        // when
        String result = map.compute(key, remapping);
        // then
        assertEquals("old-new", result, "compute must pass the current value to the remapping function");
        assertEquals("old-new", map.get(key), "the updated value must be stored");
    }

    @Test
    void computeRemappingReturnsNullRemovesEntry() {
        // given
        String key = "gamma";
        map.put(key, "to-be-removed");
        BiFunction<String, String, String> remapping = (k, v) -> null;
        // when
        String result = map.compute(key, remapping);
        // then
        assertNull(result, "compute should return null when the remapping function returns null");
        assertFalse(map.containsKey(key), "the entry must be removed from the map");
    }

    @Test
    void computeHandlesNullKey() {
        // given
        BiFunction<String, String, String> remapping = (k, v) -> "nullKeyValue";
        // when
        String result = map.compute(null, remapping);
        // then
        assertEquals("nullKeyValue", result, "compute must work with a null key");
        assertEquals("nullKeyValue", map.get(null), "the value associated with the null key must be stored");
    }

    @Test
    void computeHandlesNullKeyAndRemappingReturnsNull() {
        // given
        map.put(null, "present");
        BiFunction<String, String, String> remapping = (k, v) -> null;
        // when
        String result = map.compute(null, remapping);
        // then
        assertNull(result, "compute should return null when remapping returns null for a null key");
        assertFalse(map.containsKey(null), "the null-key entry must be removed");
    }

    /**
     * When the key is {@code null} the internal masking logic must treat it as a
     * regular entry. The old value is {@code null}, the remapping function returns
     * a non‑null default, and the result is stored and returned.
     */
    @Test
    void computeNullKeyRemappingReturnsNonNull() {
        BiFunction<String, String, String> remapping = (k, v) -> v == null ? "default" : v + "-updated";
        String result = map.compute(null, remapping);
        assertEquals("default", result);
        assertEquals("default", map.get(null));
    }

    /**
     * The map already contains a key that maps to a masked {@code null} value.
     * The remapping function receives {@code null} as the old value and returns a
     * new non‑null value, which must be stored.
     */
    @Test
    void computeKeyPresentWithMaskedNullValueRemappingToNonNull() {
        String key = "nullValueKey";
        // stores a masked null internally
        map.put(key, null);
        BiFunction<String, String, String> remapping = (k, v) -> v == null ? "new" : v;
        String result = map.compute(key, remapping);
        assertEquals("new", result);
        assertEquals("new", map.get(key));
    }

    /**
     * No‑op update: the remapping function returns exactly the current value.
     * The map size must stay the same and the returned value must equal the original.
     */
    @Test
    void computeNoOpUpdateSameValue() {
        String key = "existingKey";
        String original = "originalValue";
        map.put(key, original);
        int sizeBefore = map.size();
        // identity
        BiFunction<String, String, String> remapping = (k, v) -> v;
        String result = map.compute(key, remapping);
        assertEquals(original, result);
        assertEquals(original, map.get(key));
        assertEquals(sizeBefore, map.size(), "Map size must not change on a no‑op update");
    }

    /**
     * Absent key where the remapping function returns {@code null}.
     * According to {@code ConcurrentMap.compute} contract the map must remain unchanged.
     */
    @Test
    void computeAbsentKeyRemappingReturnsNull() {
        String key = "absentKey";
        assertFalse(map.containsKey(key));
        BiFunction<String, String, String> remapping = (k, v) -> null;
        String result = map.compute(key, remapping);
        assertNull(result);
        assertFalse(map.containsKey(key), "Key must not be added when remapping returns null");
    }

    /**
     * The remapping function throws an unchecked exception.
     * The exception must propagate and the map must stay unchanged.
     */
    @Test
    void computeExceptionPropagationFromRemappingFunction() {
        String key = "keyWithError";
        String original = "original";
        map.put(key, original);
        BiFunction<String, String, String> remapping = (k, v) -> {
            throw new IllegalStateException("boom");
        };
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> map.compute(key, remapping), "Exception from remapping function must be propagated");
        assertEquals("boom", ex.getMessage());
        // Verify the original entry is still present and unchanged
        assertEquals(original, map.get(key));
    }

    /**
     * Passing a {@code null} remapping function must result in a
     * {@link NullPointerException} before any internal logic is executed.
     */
    @Test
    void computeNullRemappingFunctionThrowsNPE() {
        String key = "anyKey";
        assertThrows(NullPointerException.class, () -> map.compute(key, null), "Null remappingFunction must cause NPE");
    }

    /**
     * When the underlying {@code internalMap} does not support the {@code compute}
     * operation (e.g., an unmodifiable map), the exception must be propagated.
     */
    @Test
    void computeUnderlyingMapUnsupportedOperation() {
        // Mock a ConcurrentMap that throws UnsupportedOperationException on compute()
        @SuppressWarnings("unchecked")
        ConcurrentMap<Object, Object> mockInternal = mock(ConcurrentMap.class);
        when(mockInternal.compute(any(), any())).thenThrow(new UnsupportedOperationException("compute not supported"));
        TestMap unsafeMap = new TestMap(mockInternal);
        BiFunction<String, String, String> remapping = (k, v) -> "value";
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> unsafeMap.compute("someKey", remapping), "UnsupportedOperationException from the backing map must propagate");
        assertEquals("compute not supported", ex.getMessage());
        // Verify that the mock's compute method was indeed invoked
        verify(mockInternal, times(1)).compute(any(), any());
    }

    @ParameterizedTest
    @MethodSource("computeScenarioProvider")
    void computeParameterizedScenarios(String key, BiFunction<String, String, String> remapping, String expected) {
        // Prepare initial state when needed
        if ("existing".equals(key)) {
            map.put(key, "value");
        } else if ("nullValueKey".equals(key)) {
            // masked null
            map.put(key, null);
        }
        String result = map.compute(key, remapping);
        assertEquals(expected, result);
        assertEquals(expected, map.get(key));
    }
}
