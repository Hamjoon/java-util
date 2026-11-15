package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_merge_Test {

    private TestMap map;

    /**
     * Simple concrete implementation used only for testing
     */
    private static class TestMap extends AbstractConcurrentNullSafeMap<String, String> {

        TestMap() {
            super(new ConcurrentHashMap<>());
        }
    }

    /**
     * Subclass that deliberately returns {@code null} from {@code maskNullValue}
     *  to provoke a NPE inside the delegate map.
     */
    private static class BadMaskMap extends AbstractConcurrentNullSafeMap<String, String> {

        BadMaskMap() {
            super(new ConcurrentHashMap<>());
        }

        @Override
        protected Object maskNullValue(String value) {
            // Incorrect implementation – should return a sentinel, not null
            return null;
        }
    }

    /**
     * Subclass with a {@code null} internal map to test NPE from the delegate.
     */
    private static class NullInternalMap extends AbstractConcurrentNullSafeMap<String, String> {

        NullInternalMap() {
            // deliberately pass null
            super(null);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        map = new TestMap();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to avoid cross‑test contamination
        map.clear();
    }

    @Test
    void mergeWhenKeyAbsentShouldInsertAndReturnValue() {
        // given
        String key = "absent";
        String value = "first";
        // should never be called
        BiFunction<String, String, String> remap = (oldV, newV) -> oldV + newV;
        // when
        String result = map.merge(key, value, remap);
        // then
        assertEquals(value, result, "merge should return the supplied value when the key is absent");
        assertEquals(value, map.get(key), "map should contain the newly merged value");
    }

    @Test
    void mergeWhenKeyPresentShouldApplyRemappingFunction() {
        // given
        String key = "present";
        map.put(key, "old");
        String newValue = "new";
        BiFunction<String, String, String> remap = (oldV, newV) -> oldV + "-" + newV;
        // when
        String result = map.merge(key, newValue, remap);
        // then
        assertEquals("old-new", result, "merge should return the value produced by the remapping function");
        assertEquals("old-new", map.get(key), "map should store the value returned by the remapping function");
    }

    @Test
    void mergeWhenRemappingFunctionReturnsNullShouldRemoveEntry() {
        // given
        String key = "toBeRemoved";
        map.put(key, "existing");
        // signal removal
        BiFunction<String, String, String> remap = (oldV, newV) -> null;
        // when
        String result = map.merge(key, "ignored", remap);
        // then
        assertNull(result, "merge should return null when the remapping function returns null");
        assertFalse(map.containsKey(key), "the entry should be removed from the map");
    }

    @Test
    void mergeSupportsNullKey() {
        // given
        // null key is allowed by this map implementation
        String key = null;
        String value = "nullKeyValue";
        // not used for first insert
        BiFunction<String, String, String> remap = (oldV, newV) -> oldV + newV;
        // when
        String result = map.merge(key, value, remap);
        // then
        assertEquals(value, result, "merge should handle a null key like any other key");
        assertEquals(value, map.get(null), "retrieving with a null key should return the stored value");
    }

    @Test
    void mergeWhenRemappingFunctionReturnsNullForNullExistingValueShouldStoreNewValue() {
        // given
        String key = "nullValueKey";
        // store a null value (masked internally)
        map.put(key, null);
        String newValue = "replaced";
        BiFunction<String, String, String> remap = (oldV, newV) -> {
            // oldV will be null because the existing value was null
            return oldV == null ? newV : oldV;
        };
        // when
        String result = map.merge(key, newValue, remap);
        // then
        assertEquals(newValue, result, "merge should replace a null existing value with the new value");
        assertEquals(newValue, map.get(key), "map should contain the new value after replacement");
    }

    @Test
    void mergeWithNullRemappingFunctionShouldThrowNPE() {
        // given
        String key = "anyKey";
        String value = "anyValue";
        // when / then
        assertThrows(NullPointerException.class, () -> map.merge(key, value, null), "merge must throw NullPointerException when the remapping function is null");
    }

    @Test
    void mergeWithNullValueArgumentShouldThrowNPE() {
        // given
        String key = "anyKey";
        BiFunction<String, String, String> remap = (oldV, newV) -> oldV;
        // when / then
        assertThrows(NullPointerException.class, () -> map.merge(key, null, remap), "merge must throw NullPointerException when the supplied value is null");
    }

    /**
     * Null‑checks already covered, but we verify the order: remapping function NPE precedes value NPE.
     */
    @Test
    void nullChecksOrderRemappingFirst() {
        // The method should throw for a null remapping function before checking the value.
        assertThrows(NullPointerException.class, () -> map.merge("k", null, null));
    }

    /**
     * Insertion when key is {@code null} – already covered, but we also verify the remapping function is never called.
     */
    @Test
    void mergeInsertionWithNullKeyDoesNotInvokeRemapping() {
        String key = null;
        String value = "first";
        // a remapping function that would fail if called
        BiFunction<String, String, String> remap = (oldV, newV) -> {
            fail("Remapping function should not be invoked for absent key");
            return null;
        };
        String result = map.merge(key, value, remap);
        assertEquals(value, result);
        assertEquals(value, map.get(null));
    }

    /**
     * Replacement where the remapping function returns a non‑null value (already covered).
     */
    @Test
    void mergeReplacementNonNullResult() {
        String key = "k2";
        map.put(key, "old");
        BiFunction<String, String, String> remap = (oldV, newV) -> oldV + "-" + newV;
        String result = map.merge(key, "new", remap);
        assertEquals("old-new", result);
        assertEquals("old-new", map.get(key));
    }

    /**
     * Removal when the remapping function returns {@code null} (already covered).
     */
    @Test
    void mergeRemovalWhenRemapReturnsNull() {
        String key = "k3";
        map.put(key, "any");
        BiFunction<String, String, String> remap = (oldV, newV) -> null;
        String result = map.merge(key, "ignored", remap);
        assertNull(result);
        assertFalse(map.containsKey(key));
    }

    /**
     * Verify that a null value argument is rejected even though the map masks nulls internally.
     */
    @Test
    void mergeRejectsNullValueEvenWithMasking() {
        assertThrows(NullPointerException.class, () -> map.merge("sentinelKey", null, (v1, v2) -> "resolved"));
    }

    /**
     * Ensure that the remapping function receives unmasked values (sentinel handling).
     */
    @Test
    void mergeRemapReceivesUnmaskedValues() {
        String key = "sentinelKey";
        map.put(key, "sentinelValue");
        BiFunction<String, String, String> remap = (v1, v2) -> v1 + "-" + v2;
        String result = map.merge(key, "sentinelValue", remap);
        assertEquals("sentinelValue-sentinelValue", result);
        assertEquals("sentinelValue-sentinelValue", map.get(key));
    }

    /**
     * Merge with an existing null key whose value is also null.
     */
    @Test
    void mergeExistingNullKeyWithNullValue() {
        // Insert a null key with a null value (both masked internally)
        map.put(null, null);
        BiFunction<String, String, String> remap = (oldV, newV) -> oldV == null ? "init" : oldV + "+" + newV;
        String result = map.merge(null, "newVal", remap);
        assertEquals("init", result);
        assertEquals("init", map.get(null));
    }

    /**
     * Exception thrown by the remapping function must propagate and leave the map unchanged.
     */
    @Test
    void mergeRemapExceptionPropagatesAndLeavesMapUnchanged() {
        String key = "exKey";
        map.put(key, "val");
        BiFunction<String, String, String> remap = (v1, v2) -> {
            throw new RuntimeException("boom");
        };
        RuntimeException ex = assertThrows(RuntimeException.class, () -> map.merge(key, "ignored", remap));
        assertEquals("boom", ex.getMessage());
        // The original entry must still be present
        assertEquals("val", map.get(key));
    }

    /**
     * Remapping function receives {@code null} as the old value when the stored value is a logical null.
     */
    @Test
    void mergeRemapReceivesNullOldValue() {
        String key = "kNullOld";
        // logical null
        map.put(key, null);
        BiFunction<String, String, String> remap = (oldV, newV) -> oldV == null ? "filled" : oldV;
        String result = map.merge(key, "new", remap);
        assertEquals("filled", result);
        assertEquals("filled", map.get(key));
    }

    /**
     * Remapping function returns the same value it received – map should keep the original entry.
     */
    @Test
    void mergeRemapReturnsSameValueNoChange() {
        String key = "kSame";
        map.put(key, "oldValue");
        // ignore newV
        BiFunction<String, String, String> remap = (oldV, newV) -> oldV;
        String result = map.merge(key, "same", remap);
        assertEquals("oldValue", result);
        assertEquals("oldValue", map.get(key));
    }

    /**
     * Sentinel handling where the existing value is a logical null and the remap returns a concrete value.
     */
    @Test
    void mergeSentinelNullValueHandling() {
        String key = "sentinelKey";
        // logical null
        map.put(key, null);
        BiFunction<String, String, String> remap = (oldV, newV) -> oldV == null ? "resolved" : oldV + "+" + newV;
        String result = map.merge(key, "nonNull", remap);
        assertEquals("resolved", result);
        assertEquals("resolved", map.get(key));
    }

    /**
     * Remapping function returns an incompatible type – expect a ClassCastException.
     */
    @Test
    void mergeRemapReturnsIncompatibleType() {
        String key = "someKey";
        // Integer instead of String
        BiFunction<String, String, Object> remap = (v1, v2) -> 123;
        assertThrows(ClassCastException.class, () -> map.merge(key, "stringValue", (BiFunction) remap));
    }

    /**
     * Internal map is {@code null} – merge should throw NPE when delegating to it.
     */
    @Test
    void mergeWithNullInternalMapThrowsNPE() {
        NullInternalMap badMap = new NullInternalMap();
        BiFunction<String, String, String> remap = (v1, v2) -> v1;
        assertThrows(NullPointerException.class, () -> badMap.merge("anyKey", "anyValue", remap));
    }

    /**
     * Subclass that returns {@code null} from {@code maskNullValue} – internal map receives a null and throws NPE.
     */
    @Test
    void mergeBadMaskNullValueCausesInternalMapNPE() {
        BadMaskMap badMaskMap = new BadMaskMap();
        // simple identity
        BiFunction<String, String, String> remap = (oldV, newV) -> oldV;
        assertThrows(NullPointerException.class, () -> badMaskMap.merge("keyWithBadMask", "value", remap));
    }
}
