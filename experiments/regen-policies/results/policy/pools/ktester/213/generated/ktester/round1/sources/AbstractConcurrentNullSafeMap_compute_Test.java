package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_compute_Test {

    private TestConcurrentNullSafeMap map;

    /**
     * Concrete implementation used only for testing – it simply forwards the supplied
     *  internal map to the abstract superclass.
     */
    private static class TestConcurrentNullSafeMap extends AbstractConcurrentNullSafeMap<String, String> {

        TestConcurrentNullSafeMap(ConcurrentMap<Object, Object> internalMap) {
            super(internalMap);
        }
    }

    /**
     * Subclass that deliberately passes a null backing map to test defensive‑programming
     *  behaviour when {@code internalMap} is null.
     */
    private static class NullBackingMap extends AbstractConcurrentNullSafeMap<String, String> {

        NullBackingMap() {
            super(null);
        }
    }

    /* --------------------------------------------------------------
     *  Helper holder class for parameterised arguments
     * -------------------------------------------------------------- */
    private static class ArgumentsHolder {

        final String key;

        final BiFunction<String, String, String> remapping;

        final String expected;

        final String description;

        ArgumentsHolder(String key, BiFunction<String, String, String> remapping, String expected, String description) {
            this.key = key;
            this.remapping = remapping;
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
        // Use a real ConcurrentHashMap as the backing store so that the null‑masking logic
        // exercised by AbstractConcurrentNullSafeMap can be verified end‑to‑end.
        ConcurrentMap<Object, Object> backing = new ConcurrentHashMap<>();
        map = new TestConcurrentNullSafeMap(backing);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to guarantee isolation between tests.
        map.clear();
    }

    /* --------------------------------------------------------------
     *  Parameterised tests for the JSON‑driven scenarios
     * -------------------------------------------------------------- */
    private static Stream<ArgumentsHolder> computeNullKeyAndValueProvider() {
        return Stream.of(// case: null key, function returns first
        new ArgumentsHolder(null, (BiFunction<String, String, String>) (k, v) -> v == null ? "first" : v + "-updated", "first", "When key is null, oldValue is null and function returns a non‑null value."), // case: key "A", function returns null (removal)
        new ArgumentsHolder("A", (BiFunction<String, String, String>) (k, v) -> null, null, "Remapping function returns null, causing removal."), // case: first call for fresh key "B"
        new ArgumentsHolder("B", (BiFunction<String, String, String>) (k, v) -> v == null ? "init" : v, "init", "First call for a fresh key, oldValue is null."), // case: second call for key "B" after previous init
        new ArgumentsHolder("B", (BiFunction<String, String, String>) (k, v) -> v + "-again", "init-again", "Key already present with value \"init\"; function updates it."));
    }

    @Test
    void computeAddsNewEntryWhenAbsent() {
        // GIVEN
        String key = "newKey";
        BiFunction<String, String, String> remapping = (k, v) -> "generated";
        // WHEN
        String result = map.compute(key, remapping);
        // THEN
        assertEquals("generated", result, "compute should return the newly generated value");
        assertEquals("generated", map.get(key), "value must be stored under the given key");
    }

    @Test
    void computeUpdatesExistingEntry() {
        // GIVEN
        String key = "existingKey";
        map.put(key, "oldValue");
        BiFunction<String, String, String> remapping = (k, v) -> v + "-updated";
        // WHEN
        String result = map.compute(key, remapping);
        // THEN
        assertEquals("oldValue-updated", result, "compute should apply the function to the old value");
        assertEquals("oldValue-updated", map.get(key), "the map must contain the updated value");
    }

    @Test
    void computeRemovesEntryWhenFunctionReturnsNull() {
        // GIVEN
        String key = "toBeRemoved";
        map.put(key, "someValue");
        // signal removal
        BiFunction<String, String, String> remapping = (k, v) -> null;
        // WHEN
        String result = map.compute(key, remapping);
        // THEN
        assertNull(result, "compute should return null when the remapping function returns null");
        assertFalse(map.containsKey(key), "the entry must be removed from the map");
    }

    @Test
    void computeHandlesNullKey() {
        // GIVEN
        BiFunction<String, String, String> remapping = (k, v) -> "valueForNullKey";
        // WHEN
        String result = map.compute(null, remapping);
        // THEN
        assertEquals("valueForNullKey", result);
        assertEquals("valueForNullKey", map.get(null));
    }

    @Test
    void computeHandlesNullValue() {
        // GIVEN
        String key = "nullValueKey";
        // explicit null value
        map.put(key, null);
        BiFunction<String, String, String> remapping = (k, v) -> v == null ? "filled" : v;
        // WHEN
        String result = map.compute(key, remapping);
        // THEN
        assertEquals("filled", result);
        assertEquals("filled", map.get(key));
    }

    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("computeNullKeyAndValueProvider")
    void compute_nullKeyAndValueScenarios(ArgumentsHolder holder) {
        // If the key already has a value (second call for "B"), pre‑populate it.
        if ("B".equals(holder.key) && "init-again".equals(holder.expected) && map.get("B") == null) {
            map.put("B", "init");
        }
        String result = map.compute(holder.key, holder.remapping);
        assertEquals(holder.expected, result);
        if (holder.expected == null) {
            assertFalse(map.containsKey(holder.key));
        } else {
            assertEquals(holder.expected, map.get(holder.key));
        }
    }

    /* --------------------------------------------------------------
     *  Individual tests for the remaining JSON groups
     * -------------------------------------------------------------- */
    @Test
    void compute_nullKeyWithNullResult() {
        // key = null, remapping returns null
        BiFunction<String, String, String> remapping = (k, v) -> null;
        String result = map.compute(null, remapping);
        assertNull(result);
        // According to contract, the entry should be absent after a null result.
        assertFalse(map.containsKey(null));
    }

    @Test
    void compute_nullRemappingFunctionThrowsNPE() {
        // JDK contract: null remappingFunction must cause NPE
        assertThrows(NullPointerException.class, () -> map.compute("anyKey", null));
        // Ensure map unchanged
        assertTrue(map.isEmpty());
    }

    @Test
    void compute_typeMismatchInStoredValue_causesClassCastException() {
        // Directly inject a mismatched value into the internal map (Integer instead of String)
        // raw insertion bypasses masking
        map.internalMap.put("mismatchKey", 42);
        BiFunction<String, String, String> remapping = (k, v) -> "newValue";
        assertThrows(ClassCastException.class, () -> map.compute("mismatchKey", remapping));
        // Verify that the original mismatched entry remains unchanged
        Object raw = map.internalMap.get("mismatchKey");
        assertEquals(42, raw);
    }

    @Test
    void compute_internalMapIsNull_throwsNullPointerException() {
        NullBackingMap nullMap = new NullBackingMap();
        BiFunction<String, String, String> remapping = (k, v) -> "value";
        assertThrows(NullPointerException.class, () -> nullMap.compute("anyKey", remapping));
    }

    @Test
    void compute_remappingFunctionThrowsException_isPropagatedAndMapUnchanged() {
        String key = "exceptionKey";
        map.put(key, "existing");
        BiFunction<String, String, String> badFunction = (k, v) -> {
            throw new IllegalArgumentException("bad");
        };
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> map.compute(key, badFunction));
        assertEquals("bad", ex.getMessage());
        // Ensure map still contains the original value
        assertEquals("existing", map.get(key));
    }
}
