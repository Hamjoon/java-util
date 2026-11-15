package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_merge_Test {

    private ConcurrentMap<Object, Object> internalMap;

    private TestMap map;

    /**
     * Concrete implementation used only for testing the abstract class.
     */
    private static class TestMap extends AbstractConcurrentNullSafeMap<String, Integer> {

        TestMap(ConcurrentMap<Object, Object> internalMap) {
            super(internalMap);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        internalMap = new ConcurrentHashMap<>();
        map = new TestMap(internalMap);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to avoid cross‑test contamination.
        internalMap.clear();
    }

    /**
     * -------------------------------------------------------------------
     */
    private static Stream<MergeCase> nullCheckCases() {
        return Stream.of(new MergeCase("remappingFunction is null – should throw NPE", "anyKey", "someValue", null, NullPointerException.class), new MergeCase("value is null – should throw NPE", "anyKey", null, (o, n) -> o, NullPointerException.class));
    }

    /**
     * -------------------------------------------------------------------
     */
    private static Stream<MergeCase> absentKeyCases() {
        BiFunction<String, String, String> shouldNotBeCalled = (oldV, newV) -> {
            throw new AssertionError("Remapping function should not be invoked for absent key");
        };
        return Stream.of(new MergeCase("Absent key – store and return supplied value", "newKey", "initialValue", shouldNotBeCalled, "initialValue"));
    }

    /**
     * -------------------------------------------------------------------
     */
    private static Stream<MergeCase> existingKeyCases() {
        return Stream.of(new MergeCase("Existing key – concatenate old and new values", "existingKey", "newPart", (oldV, newV) -> oldV + newV, "oldPartnewPart"));
    }

    /**
     * -------------------------------------------------------------------
     */
    private static Stream<MergeCase> remapReturnsNullCases() {
        return Stream.of(new MergeCase("Remapping returns null – entry removed", "toBeRemoved", "ignored", (oldV, newV) -> null, null));
    }

    /**
     * -------------------------------------------------------------------
     */
    private static Stream<MergeCase> remapThrowsExceptionCases() {
        return Stream.of(new MergeCase("Remapping throws IllegalStateException – original mapping unchanged", "exceptionKey", "newValue", (oldV, newV) -> {
            throw new IllegalStateException("boom");
        }, IllegalStateException.class));
    }

    /**
     * -------------------------------------------------------------------
     */
    private static Stream<MergeCase> nullKeyAbsentCases() {
        BiFunction<String, String, String> shouldNotBeCalled = (oldV, newV) -> {
            throw new AssertionError("Remapping should not be called for absent null key");
        };
        return Stream.of(new MergeCase("Null key absent – store and return value", null, "nullKeyValue", shouldNotBeCalled, "nullKeyValue"));
    }

    /**
     * -------------------------------------------------------------------
     */
    private static Stream<MergeCase> nullKeyExistingCases() {
        return Stream.of(new MergeCase("Null key existing – concatenate existing and new parts", null, "addedPart", (oldV, newV) -> oldV + newV, "originalPartaddedPart"));
    }

    @Test
    void mergeWhenKeyAbsent_ShouldInsertAndReturnValue() {
        // given
        String key = "alpha";
        Integer value = 10;
        BiFunction<Integer, Integer, Integer> remapping = (oldV, newV) -> oldV + newV;
        // when
        Integer result = map.merge(key, value, remapping);
        // then
        assertEquals(value, result, "Result should be the supplied value when the key is absent");
        assertTrue(internalMap.containsKey(key), "Internal map must contain the new key");
        assertEquals(value, map.get(key), "Stored value must match the supplied value");
    }

    @Test
    void mergeWhenKeyPresent_ShouldApplyRemappingFunction() {
        // given
        String key = "beta";
        // existing value
        map.put(key, "5");
        String newValue = "7";
        // "5" + "7" = "57"
        BiFunction<String, String, String> remapping = (oldV, newV) -> oldV + newV;
        // when
        String result = map.merge(key, newValue, remapping);
        // then
        assertEquals("57", result);
        assertEquals("57", map.get(key), "The value stored after merge must be the result of the remapping function");
    }

    @Test
    void mergeRemappingReturnsNull_ShouldRemoveEntryAndReturnNull() {
        // given
        String key = "gamma";
        map.put(key, 3);
        // signal removal
        BiFunction<Integer, Integer, Integer> remapping = (oldV, newV) -> null;
        // when
        Integer result = map.merge(key, 4, remapping);
        // then
        assertNull(result, "Result should be null when the remapping function returns null");
        assertFalse(internalMap.containsKey(key), "The entry must be removed from the internal map");
        assertNull(map.get(key), "Public view must also report the key as absent");
    }

    @Test
    void mergeWithNullRemappingFunction_ShouldThrowNPE() {
        // given
        String key = "delta";
        String value = "1";
        // when / then
        assertThrows(NullPointerException.class, () -> map.merge(key, value, null), "merge must reject a null remapping function");
    }

    @Test
    void mergeWithNullValue_ShouldThrowNPE() {
        // given
        String key = "epsilon";
        BiFunction<Integer, Integer, Integer> remapping = (oldV, newV) -> oldV;
        // when / then
        assertThrows(NullPointerException.class, () -> map.merge(key, null, remapping), "merge must reject a null value argument");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("nullCheckCases")
    void merge_NullChecks(MergeCase c) {
        if (c.expected == NullPointerException.class) {
            assertThrows(NullPointerException.class, () -> map.merge(c.key, c.value, c.remapping), c.description);
        }
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("absentKeyCases")
    void merge_AbsentKey(MergeCase c) {
        String result = map.merge(c.key, c.value, c.remapping);
        assertEquals(c.expected, result);
        assertEquals(c.expected, map.get(c.key));
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("existingKeyCases")
    void merge_ExistingKey(MergeCase c) {
        // pre‑populate with the "oldPart" value
        map.put(c.key, "oldPart");
        String result = map.merge(c.key, c.value, c.remapping);
        assertEquals(c.expected, result);
        assertEquals(c.expected, map.get(c.key));
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("remapReturnsNullCases")
    void merge_RemappingReturnsNull(MergeCase c) {
        map.put(c.key, "present");
        String result = map.merge(c.key, c.value, c.remapping);
        assertNull(result);
        assertFalse(internalMap.containsKey(c.key));
        assertNull(map.get(c.key));
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("remapThrowsExceptionCases")
    void merge_RemappingThrowsException(MergeCase c) {
        // set up original mapping
        map.put(c.key, "original");
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> map.merge(c.key, c.value, c.remapping), c.description);
        assertEquals("boom", ex.getMessage());
        // verify original value is still present
        assertEquals("original", map.get(c.key));
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("nullKeyAbsentCases")
    void merge_NullKeyAbsent(MergeCase c) {
        String result = map.merge(c.key, c.value, c.remapping);
        assertEquals(c.expected, result);
        assertEquals(c.expected, map.get(null));
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("nullKeyExistingCases")
    void merge_NullKeyExisting(MergeCase c) {
        // pre‑populate mapping for null key
        map.put(null, "originalPart");
        String result = map.merge(c.key, c.value, c.remapping);
        assertEquals(c.expected, result);
        assertEquals(c.expected, map.get(null));
    }

    /**
     * -------------------------------------------------------------------
     */
    @Test
    void merge_InternalMapIsNull_ShouldThrowNPEAfterArgumentChecks() throws Exception {
        // given: a map with a null internalMap (set via reflection)
        Field field = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        field.setAccessible(true);
        field.set(map, null);
        // The null checks for value and remapping function happen before we touch internalMap,
        // so we use non‑null arguments to get past them.
        String key = "anyKey";
        String value = "someValue";
        BiFunction<String, String, String> remapping = (oldV, newV) -> oldV;
        // when / then: NPE from internalMap.merge
        assertThrows(NullPointerException.class, () -> map.merge(key, value, remapping), "When internalMap is null, merge should throw NPE after argument validation");
    }
}
