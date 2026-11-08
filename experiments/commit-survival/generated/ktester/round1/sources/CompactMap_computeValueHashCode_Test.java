package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class CompactMap_computeValueHashCode_Test {

    private TestableCompactMap map;

    /**
     * Simple subclass that makes the protected method public for the tests.
     */
    private static class TestableCompactMap extends CompactMap<Object, Object> {

        int computeValueHashCodePublic(Object value) {
            return super.computeValueHashCode(value);
        }
    }

    /**
     * Custom object whose {@code hashCode()} returns a predefined constant.
     */
    private static class CustomHashObject {

        private final int hash;

        CustomHashObject(int hash) {
            this.hash = hash;
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    /**
     * Object whose {@code hashCode()} throws an {@link IllegalStateException}.
     */
    private static class ThrowingHashObject {

        @Override
        public int hashCode() {
            throw new IllegalStateException("Intentional exception from hashCode");
        }
    }

    /**
     * Object whose {@code hashCode()} throws an {@link AssertionError} (an {@link Error}).
     */
    private static class ErrorThrowingHashObject {

        @Override
        public int hashCode() {
            throw new AssertionError("Intentional error from hashCode");
        }
    }

    /**
     * Object whose {@code hashCode()} throws a {@link NullPointerException}.
     */
    private static class NullPointerThrowingHashObject {

        @Override
        public int hashCode() {
            throw new NullPointerException("Intentional NPE from hashCode");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        map = new TestableCompactMap();
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up per‑test.
        map = null;
    }

    /**
     * Parameter source for the main {@code computeValueHashCode} group.
     */
    private static Stream<Arguments> provideComputeValueHashCodeCases() {
        return Stream.of(// value == this (already tested above, kept for completeness)
        arguments("this", 17), // null value
        arguments(null, 0), // Integer 42 – Integer.hashCode() == 42
        arguments(42, 42), // String "sample string"
        arguments("sample string", "sample string".hashCode()), // Custom object with hashCode() == 12345
        arguments(new CustomHashObject(12345), 12345), // Different CompactMap instance – we compare with its own hashCode()
        arguments(new CompactMap<>(), new CompactMap<>().hashCode()));
    }

    /**
     * Parameter source for the additional cases group.
     */
    private static Stream<Arguments> provideComputeValueHashCodeAdditionalCases() {
        return Stream.of(// 1231
        arguments(Boolean.TRUE, Boolean.TRUE.hashCode()), // 1074341368
        arguments(3.14d, Double.hashCode(3.14d)), // 65 (hash of name "A")
        arguments(Sample.A, Sample.A.hashCode()), arguments(new CustomHashObject(17), 17), arguments(new CustomHashObject(Integer.MIN_VALUE), Integer.MIN_VALUE));
    }

    /**
     * Parameter source for exception‑propagation cases.
     */
    private static Stream<Arguments> provideComputeValueHashCodeExceptionCases() {
        return Stream.of(arguments(new ThrowingHashObject(), IllegalStateException.class), arguments(new ErrorThrowingHashObject(), AssertionError.class), arguments(new NullPointerThrowingHashObject(), NullPointerException.class));
    }

    @Test
    void returns17WhenValueIsTheMapItself() {
        // The special case: when the value argument is the same instance as the map,
        // the method must return the constant 17.
        int result = map.computeValueHashCodePublic(map);
        assertEquals(17, result, "computeValueHashCode should return 17 when the value is the map instance itself");
    }

    @Test
    void returnsZeroWhenValueIsNull() {
        // Null values should yield a hash code of 0.
        int result = map.computeValueHashCodePublic(null);
        assertEquals(0, result, "computeValueHashCode should return 0 for a null value");
    }

    @Test
    void returnsValueHashCodeWhenValueIsNonNullAndNotTheMap() {
        // For a regular non‑null object, the method must delegate to the object's hashCode().
        Object value = new Object() {

            @Override
            public int hashCode() {
                return 123456;
            }
        };
        int result = map.computeValueHashCodePublic(value);
        assertEquals(123456, result, "computeValueHashCode should return the object's hashCode when value is a regular object");
    }

    @Test
    void returnsStandardHashCodeForCommonObjectTypes() {
        // Verify behaviour with built‑in types (String, Integer) to ensure no special handling is introduced.
        String str = "testString";
        Integer integer = 42;
        assertEquals(str.hashCode(), map.computeValueHashCodePublic(str), "String hashCode should be returned unchanged");
        assertEquals(integer.hashCode(), map.computeValueHashCodePublic(integer), "Integer hashCode should be returned unchanged");
    }

    @ParameterizedTest(name = "[{index}] value={0} → expected={1}")
    @MethodSource("provideComputeValueHashCodeCases")
    void computeValueHashCode_mainGroup(Object value, int expected) {
        // For the special sentinel case we must pass the map instance itself.
        Object argument = "this".equals(String.valueOf(value)) ? map : value;
        int result = map.computeValueHashCodePublic(argument);
        assertEquals(expected, result, () -> "Unexpected hash code for value: " + (argument == null ? "null" : argument.getClass().getSimpleName()));
    }

    @ParameterizedTest(name = "[{index}] additional value={0} → expected={1}")
    @MethodSource("provideComputeValueHashCodeAdditionalCases")
    void computeValueHashCode_additionalGroup(Object value, int expected) {
        int result = map.computeValueHashCodePublic(value);
        assertEquals(expected, result, () -> "Unexpected hash code for additional value: " + value);
    }

    @ParameterizedTest(name = "[{index}] exception case value={0} expects {1}")
    @MethodSource("provideComputeValueHashCodeExceptionCases")
    void computeValueHashCode_exceptions(Object value, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> map.computeValueHashCodePublic(value), () -> "Expected exception " + expectedException.getSimpleName() + " was not thrown");
    }
}
