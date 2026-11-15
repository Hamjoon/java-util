package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_unmaskNullValue_Test {

    private TestMap map;

    private StringMap stringMap;

    private ObjectMap objectMap;

    /**
     * Concrete subclass used only for testing the protected behaviour.
     */
    private static class TestMap extends AbstractConcurrentNullSafeMap<String, String> {

        TestMap(ConcurrentMap<Object, Object> internal) {
            super(internal);
        }

        /**
         * Exposes the protected {@code unmaskNullValue} method for direct testing.
         */
        String callUnmaskNullValue(Object value) {
            return super.unmaskNullValue(value);
        }
    }

    /**
     * Concrete subclass with {@code String} value type – used for type‑safety tests.
     */
    private static class StringMap extends AbstractConcurrentNullSafeMap<String, String> {

        StringMap(ConcurrentMap<Object, Object> internal) {
            super(internal);
        }

        /**
         * Exposes the protected {@code unmaskNullValue} method for testing.
         */
        String callUnmaskNullValue(Object value) {
            return super.unmaskNullValue(value);
        }
    }

    /**
     * Concrete subclass with {@code Object} value type – used for non‑sentinel enum handling.
     */
    private static class ObjectMap extends AbstractConcurrentNullSafeMap<String, Object> {

        ObjectMap(ConcurrentMap<Object, Object> internal) {
            super(internal);
        }

        /**
         * Exposes the protected {@code unmaskNullValue} method for testing.
         */
        Object callUnmaskNullValue(Object value) {
            return super.unmaskNullValue(value);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use a real ConcurrentHashMap as the backing store for both maps.
        ConcurrentMap<Object, Object> backing1 = new ConcurrentHashMap<>();
        ConcurrentMap<Object, Object> backing2 = new ConcurrentHashMap<>();
        stringMap = new StringMap(backing1);
        objectMap = new ObjectMap(backing2);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear both maps to avoid cross‑test contamination.
        stringMap.clear();
        objectMap.clear();
    }

    private static Stream<Arguments> provideValuesForStringMap() {
        return Stream.of(// Sentinel → null
        Arguments.of(NullSentinel.NULL_VALUE, null), // Regular string → same string
        Arguments.of("actualString", "actualString"), // Genuine null → null
        Arguments.of(null, null));
    }

    @Test
    void unmaskNullValue_returnsOriginalValueWhenNotSentinel() {
        // GIVEN
        String original = "actualValue";
        // WHEN
        String result = stringMap.callUnmaskNullValue(original);
        // THEN
        assertSame(original, result, "When the stored value is not the NULL_VALUE sentinel, unmaskNullValue should return the original object.");
    }

    @Test
    void unmaskNullValue_returnsNullWhenSentinel() {
        // GIVEN
        // Access the sentinel via the protected enum; the subclass can see it.
        Object sentinel = NullSentinel.NULL_VALUE;
        // WHEN
        String result = map.callUnmaskNullValue(sentinel);
        // THEN
        assertNull(result, "When the stored value equals NullSentinel.NULL_VALUE, unmaskNullValue must return null.");
    }

    /**
     * Verifies that {@code unmaskNullValue} returns the correct result for:
     * <ul>
     *   <li>the {@code NULL_VALUE} sentinel (should become {@code null})</li>
     *   <li>a regular non‑null string (should be returned unchanged)</li>
     *   <li>a genuine {@code null} reference (should stay {@code null})</li>
     * </ul>
     */
    @ParameterizedTest(name = "[{index}] value={0} → expected={1}")
    @MethodSource("provideValuesForStringMap")
    void unmaskNullValue_stringMap_branches(Object input, String expected) {
        // WHEN
        String result = stringMap.callUnmaskNullValue(input);
        // THEN
        assertEquals(expected, result, () -> "Unexpected result for input: " + input);
    }

    /**
     * Supplying an incompatible type (e.g., {@code Integer}) to a map whose value type is {@code String}
     * must trigger a {@link ClassCastException} because the unchecked cast inside {@code unmaskNullValue}
     * fails.
     */
    @Test
    void unmaskNullValue_stringMap_throwsClassCastExceptionOnWrongType() {
        // GIVEN
        Integer wrongType = 123;
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> stringMap.callUnmaskNullValue(wrongType), "An Integer supplied to a String‑typed map should cause ClassCastException.");
    }

    /**
     * When the map's value type is {@code Object}, the enum constant {@code NULL_KEY}
     * is *not* the sentinel for null values, so it should be returned unchanged.
     */
    @Test
    void unmaskNullValue_objectMap_nonSentinelEnum_returnsSameInstance() {
        // GIVEN
        NullSentinel enumValue = NullSentinel.NULL_KEY;
        // WHEN
        Object result = objectMap.callUnmaskNullValue(enumValue);
        // THEN
        assertSame(enumValue, result, "NULL_KEY is not the null‑value sentinel and should be returned unchanged.");
    }

    /**
     * When the map's value type is {@code String}, the same enum constant must be cast to {@code String},
     * which fails and results in a {@link ClassCastException}. This confirms that only {@code NULL_VALUE}
     * receives special handling.
     */
    @Test
    void unmaskNullValue_stringMap_nonSentinelEnum_throwsClassCastException() {
        // GIVEN
        NullSentinel enumValue = NullSentinel.NULL_KEY;
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> stringMap.callUnmaskNullValue(enumValue), "Casting NULL_KEY to String should fail with ClassCastException.");
    }
}
