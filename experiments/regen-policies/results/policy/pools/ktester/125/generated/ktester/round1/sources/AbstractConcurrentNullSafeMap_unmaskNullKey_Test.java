package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.DayOfWeek;
import java.util.function.Function;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_unmaskNullKey_Test {

    private TestMap map;

    /**
     * Simple concrete implementation used only for exposing the protected method.
     */
    static class TestMap extends AbstractConcurrentNullSafeMap<String, String> {

        TestMap() {
            super(new ConcurrentHashMap<>());
        }

        /**
         * Public wrapper that returns {@link Object} so that the generic cast inside the
         *  implementation does not cause a {@link ClassCastException} when the supplied
         *  key is not a {@code String}.
         */
        Object unmask(Object key) {
            return unmaskNullKey(key);
        }
    }

    /**
     * A tiny user‑defined key class used in one of the parameterised test cases.
     */
    static final class CustomKey {

        final String id;

        CustomKey(String id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return "CustomKey{id='" + id + "'}";
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        map = new TestMap();
    }

    @AfterEach
    void teardownAfterEach() {
        map = null;
    }

    /**
     * Supplies the arguments for {@link #unmaskNullKey_variousInputs(Object, Object)}.
     * Each {@link Arguments} entry mirrors a test case from the JSON document.
     */
    private static Stream<Arguments> provideUnmaskNullKeyCases() {
        // Regular non‑null string key
        String sampleKey = "sampleKey";
        // Integer key – should be returned unchanged (no ClassCastException)
        Integer intKey = 42;
        // The other enum constant from NullSentinel (not the key sentinel)
        AbstractConcurrentNullSafeMap.NullSentinel nullValueSentinel = AbstractConcurrentNullSafeMap.NullSentinel.NULL_VALUE;
        // DayOfWeek enum constant from a different enum type
        DayOfWeek monday = DayOfWeek.MONDAY;
        // Custom user‑defined object
        CustomKey customKey = new CustomKey("id123");
        // Array object
        int[] intArray = new int[] { 1, 2, 3 };
        // Lambda expression (treated as a functional interface instance)
        Function<Void, Void> lambda = v -> null;
        // Generic Object instance
        Object genericObject = new Object();
        // Null reference (not the sentinel)
        Object nullKey = null;
        return Stream.of(// sentinel already covered by dedicated test – included here for completeness
        Arguments.of(AbstractConcurrentNullSafeMap.NullSentinel.NULL_KEY, null), // regular string key
        Arguments.of(sampleKey, sampleKey), // null reference (non‑sentinel)
        Arguments.of(nullKey, null), // integer key
        Arguments.of(intKey, intKey), // other enum constant from NullSentinel
        Arguments.of(nullValueSentinel, nullValueSentinel), // enum constant from a different enum type
        Arguments.of(monday, monday), // custom user‑defined object
        Arguments.of(customKey, customKey), // array object – we assert reference equality
        Arguments.of(intArray, intArray), // lambda instance
        Arguments.of(lambda, lambda), // generic Object instance
        Arguments.of(genericObject, genericObject));
    }

    @Test
    void unmaskNullKey_returnsNull_whenSentinelKeyProvided() {
        // GIVEN a sentinel representing a null key
        Object sentinel = AbstractConcurrentNullSafeMap.NullSentinel.NULL_KEY;
        // WHEN the sentinel is unmasked
        String result = map.unmask(sentinel);
        // THEN the result should be null
        assertNull(result, "The unmasked value of the NULL_KEY sentinel must be null");
    }

    @Test
    void unmaskNullKey_returnsOriginalKey_whenNonSentinelProvided() {
        // GIVEN a regular (non‑null) key
        String originalKey = "regularKey";
        // WHEN the key is unmasked
        String result = map.unmask(originalKey);
        // THEN the same key instance should be returned (cast to the generic type)
        assertSame(originalKey, result, "A non‑sentinel key must be returned unchanged");
    }

    /**
     * Parameterised test that iterates over a comprehensive set of inputs described in the
     * supplied JSON test cases. For every entry the method must return the exact same instance
     * (or {@code null} when appropriate).
     */
    @ParameterizedTest(name = "[{index}] key = {0} → expected = {1}")
    @MethodSource("provideUnmaskNullKeyCases")
    void unmaskNullKey_variousInputs(Object key, Object expected) {
        // WHEN the key is passed to the protected method via the public wrapper
        Object result = map.unmask(key);
        // THEN the returned value must match the expectation
        if (expected == null) {
            assertNull(result, "Expected null for key: " + key);
        } else if (expected.getClass().isArray()) {
            // For arrays we want to ensure the same reference is returned
            assertSame(expected, result, "Array reference must be unchanged");
        } else {
            assertEquals(expected, result, "Returned value must be equal to the original key");
            // For objects that do not override equals we also verify reference identity
            if (!expected.getClass().isEnum() && !(expected instanceof String) && !(expected instanceof Number)) {
                assertSame(expected, result, "Reference identity must be preserved for " + expected);
            }
        }
    }
}
