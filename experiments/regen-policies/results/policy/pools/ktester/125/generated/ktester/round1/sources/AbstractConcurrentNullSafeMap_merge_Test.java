package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_merge_Test {

    /**
     * Instance under test – recreated for every test method.
     */
    private AbstractConcurrentNullSafeMap<String, String> map;

    /**
     * Mocked remapping function supplied to {@code merge}.
     */
    @Mock
    private BiFunction<String, String, String> remappingFunction;

    /**
     * Simple concrete subclass that delegates to a {@link ConcurrentHashMap}.
     */
    private static final class TestMap extends AbstractConcurrentNullSafeMap<String, String> {

        TestMap() {
            super(new ConcurrentHashMap<>());
        }
    }

    /**
     * Concrete subclass that accepts {@link Object} keys (used for key‑type variance test).
     */
    @SuppressWarnings("unchecked")
    private static final class ObjectKeyMap extends AbstractConcurrentNullSafeMap<Object, String> {

        ObjectKeyMap() {
            super(new ConcurrentHashMap<>());
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh map for each test to avoid cross‑test contamination.
        map = new TestMap();
        // The @Mock field (remappingFunction) is automatically initialised by MockitoExtension.
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to release any retained entries and help the GC.
        map.clear();
    }

    @Test
    @DisplayName("merge throws NPE when remappingFunction is null")
    void mergeNullRemappingFunctionShouldThrowNPE() {
        assertThrows(NullPointerException.class, () -> map.merge("anyKey", "someValue", null), "Expected NPE because remappingFunction is null");
    }

    @Test
    @DisplayName("merge throws NPE when value argument is null")
    void mergeNullValueShouldThrowNPE() {
        assertThrows(NullPointerException.class, () -> map.merge("anyKey", null, (v1, v2) -> v1), "Expected NPE because value argument is null");
    }

    @Test
    @DisplayName("merge inserts value when key is absent")
    void mergeAbsentKeyInsertsValue() {
        String result = map.merge("alpha", "first", (oldV, newV) -> oldV + "+" + newV);
        assertEquals("first", result);
        assertEquals("first", map.get("alpha"));
    }

    @Test
    @DisplayName("merge combines values when key already present")
    void mergeExistingKeyCombinesValues() {
        // pre‑populate
        map.put("alpha", "first");
        String result = map.merge("alpha", "second", (oldV, newV) -> oldV + "+" + newV);
        assertEquals("first+second", result);
        assertEquals("first+second", map.get("alpha"));
    }

    @Test
    @DisplayName("merge removes entry when remappingFunction returns null")
    void mergeRemappingReturnsNullRemovesEntry() {
        map.put("beta", "initial");
        String result = map.merge("beta", "second", (oldV, newV) -> null);
        assertNull(result);
        assertFalse(map.containsKey("beta"));
    }

    @Test
    @DisplayName("merge accepts a null key")
    void mergeWithNullKey() {
        String result = map.merge(null, "nullKeyValue", (oldV, newV) -> oldV);
        assertEquals("nullKeyValue", result);
        assertEquals("nullKeyValue", map.get(null));
    }

    @Test
    @DisplayName("first insertion with normal key/value works")
    void mergeFirstNormalInsertion() {
        String result = map.merge("gamma", "value1", (oldV, newV) -> oldV);
        assertEquals("value1", result);
        assertEquals("value1", map.get("gamma"));
    }

    @Test
    @DisplayName("remapping returning null removes existing entry (non‑null key)")
    void mergeRemapNullRemovesExisting() {
        map.put("gamma", "value2");
        String result = map.merge("gamma", "value2", (oldV, newV) -> null);
        assertNull(result);
        assertFalse(map.containsKey("gamma"));
    }

    @Test
    @DisplayName("merge throws NPE when supplied value is null even if key is valid")
    void mergeNullValueStillThrowsNPE() {
        assertThrows(NullPointerException.class, () -> map.merge("delta", null, (oldV, newV) -> "fallback"));
    }

    @Test
    @DisplayName("remappingFunction is NOT called when key absent")
    void mergeAbsentKeyDoesNotInvokeRemappingFunction() {
        // mock already injected
        String result = map.merge("newKey", "init", remappingFunction);
        assertEquals("init", result);
        verifyNoInteractions(remappingFunction);
    }

    @Test
    @DisplayName("remappingFunction is called exactly once for existing key")
    void mergeExistingKeyInvokesRemappingFunctionOnce() {
        map.put("existingKey", "first");
        when(remappingFunction.apply(any(), any())).thenAnswer(inv -> inv.getArgument(0) + "+" + inv.getArgument(1));
        String result = map.merge("existingKey", "second", remappingFunction);
        assertEquals("first+second", result);
        verify(remappingFunction, times(1)).apply("first", "second");
    }

    @Test
    @DisplayName("merge propagates exception thrown by remappingFunction")
    void mergeRemappingFunctionThrowsException() {
        BiFunction<String, String, String> throwing = (oldV, newV) -> {
            throw new IllegalStateException("boom");
        };
        map.put("boom", "value");
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> map.merge("boom", "value", throwing));
        assertEquals("boom", ex.getMessage());
    }

    @Test
    @DisplayName("merge works with non‑String (Integer) keys")
    void mergeWithIntegerKey() {
        ObjectKeyMap intMap = new ObjectKeyMap();
        String result = intMap.merge(123, "intKeyValue", (oldV, newV) -> oldV);
        assertEquals("intKeyValue", result);
        assertEquals("intKeyValue", intMap.get(123));
    }

    @Test
    @DisplayName("size increases after inserting a previously absent key")
    void sizeIncreasesAfterInsert() {
        assertEquals(0, map.size());
        map.merge("sizeKey", "v1", (oldV, newV) -> oldV);
        assertEquals(1, map.size());
        assertEquals("v1", map.get("sizeKey"));
    }

    @Test
    @DisplayName("size decreases after removal via remapping returning null")
    void sizeDecreasesAfterRemoval() {
        map.put("sizeKey", "v1");
        assertEquals(1, map.size());
        String result = map.merge("sizeKey", "v2", (oldV, newV) -> null);
        assertNull(result);
        assertEquals(0, map.size());
    }

    @Test
    @DisplayName("remapping returns existing value unchanged")
    void remapReturnsSameValue() {
        map.put("sameKey", "sameValue");
        String result = map.merge("sameKey", "sameValue", (oldV, newV) -> oldV);
        assertEquals("sameValue", result);
        assertEquals("sameValue", map.get("sameKey"));
    }

    @Test
    @DisplayName("remapping returns the supplied new value")
    void remapReturnsNewValue() {
        map.put("diffKey", "old");
        String result = map.merge("diffKey", "new", (oldV, newV) -> newV);
        assertEquals("new", result);
        assertEquals("new", map.get("diffKey"));
    }

    @Test
    @DisplayName("first merge with null key stores the value")
    void firstMergeWithNullKey() {
        String result = map.merge(null, "firstNull", (oldV, newV) -> oldV);
        assertEquals("firstNull", result);
        assertEquals("firstNull", map.get(null));
    }

    @Test
    @DisplayName("subsequent merge with same null key concatenates values")
    void subsequentMergeWithNullKey() {
        // simulate previous insertion
        map.put(null, "firstNull");
        String result = map.merge(null, "secondNull", (oldV, newV) -> oldV + "+" + newV);
        assertEquals("firstNull+secondNull", result);
        assertEquals("firstNull+secondNull", map.get(null));
    }

    @Test
    @DisplayName("remappingFunction receives unmasked (original) values")
    void remapReceivesUnmaskedValues() {
        map.put("maskKey", "masked");
        BiFunction<String, String, String> function = (oldV, newV) -> {
            assertEquals("masked", oldV, "oldV should be the original unmasked value");
            return "unchanged";
        };
        String result = map.merge("maskKey", "masked", function);
        assertEquals("unchanged", result);
        assertEquals("unchanged", map.get("maskKey"));
    }
}
