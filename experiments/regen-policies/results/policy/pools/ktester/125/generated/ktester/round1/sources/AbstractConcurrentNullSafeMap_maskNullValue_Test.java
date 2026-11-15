package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.Collections;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_maskNullValue_Test {

    private TestMap map;

    /**
     * Simple concrete subclass exposing the protected method for testing.
     */
    private static class TestMap extends AbstractConcurrentNullSafeMap<Object, Object> {

        TestMap(ConcurrentMap<Object, Object> internalMap) {
            super(internalMap);
        }

        /**
         * Delegates to the protected {@code maskNullValue} method.
         */
        Object maskNullValuePublic(Object value) {
            return super.maskNullValue(value);
        }
    }

    /**
     * A user‑defined POJO used in one of the test cases.
     */
    private static final class MyPojo {

        private final String name;

        MyPojo(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof MyPojo))
                return false;
            MyPojo myPojo = (MyPojo) o;
            return name.equals(myPojo.name);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use a real ConcurrentHashMap as the backing map – it is thread‑safe and harmless for these tests.
        ConcurrentMap<Object, Object> backing = new ConcurrentHashMap<>();
        map = new TestMap(backing);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to avoid cross‑test contamination.
        map.clear();
    }

    /**
     * Supplies the test data for {@link #maskNullValue_VariousScenarios(Object, Object, String)}.
     *
     * @return a stream of arguments: input value, expected output, description
     */
    private static Stream<Arguments> maskNullValueProvider() {
        return Stream.of(// sentinel object itself – should be returned unchanged
        Arguments.of(AbstractConcurrentNullSafeMap.NullSentinel.NULL_VALUE, AbstractConcurrentNullSafeMap.NullSentinel.NULL_VALUE, "sentinel object itself (non‑null) must be returned unchanged"), // null handling – must be converted to the sentinel
        Arguments.of(null, AbstractConcurrentNullSafeMap.NullSentinel.NULL_VALUE, "null value should be masked with NullSentinel.NULL_VALUE"), // primitive wrapper (Integer)
        Arguments.of(42, 42, "non‑null Integer should be returned unchanged"), // String value
        Arguments.of("test-string", "test-string", "non‑null String must be returned as‑is"), // custom POJO instance
        Arguments.of(new MyPojo("alpha"), new MyPojo("alpha"), "custom object reference must be returned unchanged"), // empty collection
        Arguments.of(Collections.emptyList(), Collections.emptyList(), "empty List instance must be returned unchanged"));
    }

    @Test
    void maskNullValue_ShouldReturnNullValueSentinel_WhenValueIsNull() {
        // GIVEN a null value
        String value = null;
        // WHEN the protected method is invoked via the public delegate
        Object masked = map.maskNullValuePublic(value);
        // THEN the sentinel representing a null value is returned
        assertEquals(AbstractConcurrentNullSafeMap.NullSentinel.NULL_VALUE, masked, "maskNullValue should return NullSentinel.NULL_VALUE for null input");
    }

    @Test
    void maskNullValue_ShouldReturnOriginalValue_WhenValueIsNonNull() {
        // GIVEN a non‑null value
        String value = "nonNull";
        // WHEN the protected method is invoked via the public delegate
        Object masked = map.maskNullValuePublic(value);
        // THEN the same instance is returned (no masking occurs)
        assertSame(value, masked, "maskNullValue should return the original value when it is not null");
    }

    /**
     * Parameterized test that verifies {@code maskNullValue} behaviour for a variety of inputs,
     * including the sentinel itself, {@code null}, primitive wrappers, strings, custom objects,
     * and empty collections.
     *
     * @param input       the value supplied to {@code maskNullValue}
     * @param expected    the value expected to be returned
     * @param description a short description of the scenario (used as the test name)
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("maskNullValueProvider")
    void maskNullValue_VariousScenarios(Object input, Object expected, String description) {
        // WHEN the protected method is invoked via the public delegate
        Object masked = map.maskNullValuePublic(input);
        // THEN the returned object must be exactly the expected one (identity preservation)
        assertSame(expected, masked, () -> "maskNullValue failed for scenario: " + description);
    }
}
