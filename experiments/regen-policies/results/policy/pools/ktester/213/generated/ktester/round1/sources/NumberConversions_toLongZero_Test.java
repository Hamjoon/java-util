package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toLongZero_Test {

    /**
     * The Converter instance passed to the method under test.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Simple holder for a single test case's arguments and description.
     */
    private static class ArgumentsHolder {

        final Object from;

        final Converter converter;

        final String description;

        ArgumentsHolder(Object from, Converter converter, String description) {
            this.from = from;
            this.converter = converter;
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    /**
     * Dummy {@link Converter} implementation that does nothing.
     */
    private static class DummyConverter implements Converter {

        @Override
        public Object convert(Object src, Class<?> target) {
            // No conversion – never used by toLongZero.
            return src;
        }
    }

    /**
     * Converter that throws if its {@code convert} method is ever called.
     */
    private static class ThrowingConverter implements Converter {

        @Override
        public Object convert(Object src, Class<?> target) {
            throw new RuntimeException("Should not be called");
        }
    }

    /**
     * Supplies all test cases described in the JSON document.
     */
    private static Stream<ArgumentsHolder> provideTestCases() {
        return Stream.of(// null / null
        new ArgumentsHolder(null, null, "When both arguments are null the method should still return the constant LONG_ZERO (0L)."), // Integer source, mock converter
        new ArgumentsHolder(123, MockConverter.INSTANCE, "Even if the source object is a non‑null Integer, the method ignores it and returns LONG_ZERO."), // String source, dummy converter
        new ArgumentsHolder("some string", new DummyConverter(), "When the source object is a String the method still returns the constant 0L."), // Date source, null converter
        new ArgumentsHolder(new Date(), null, "Date instance as source does not affect the result; LONG_ZERO is returned."), // Generic Object source, mock converter
        new ArgumentsHolder(new Object(), MockConverter.INSTANCE, "A generic Object as source should still lead to the constant LONG_ZERO."), // Boolean source, null converter
        new ArgumentsHolder(true, null, "Boolean true as source object; method should ignore it and return LONG_ZERO."), // Long zero source, mock converter
        new ArgumentsHolder(0L, MockConverter.INSTANCE, "Long instance with value zero; still returns the constant LONG_ZERO."), // Double zero source, dummy converter
        new ArgumentsHolder(0.0d, new DummyConverter(), "Double zero as source; conversion is not performed, constant 0L is returned."), // BigDecimal source, mock converter
        new ArgumentsHolder(new BigDecimal("123.45"), MockConverter.INSTANCE, "BigDecimal source; method still returns LONG_ZERO regardless of numeric value."), // BigInteger source, dummy converter
        new ArgumentsHolder(new BigInteger("987654321"), new DummyConverter(), "BigInteger source; method ignores it and returns the constant zero."), // AtomicLong source, null converter
        new ArgumentsHolder(new AtomicLong(42L), null, "AtomicLong source; method does not dereference the atomic value and returns LONG_ZERO."), // Calendar source, mock converter
        new ArgumentsHolder(Calendar.getInstance(), MockConverter.INSTANCE, "Calendar instance as source; conversion is bypassed and constant zero is returned."), // LocalDate source, dummy converter
        new ArgumentsHolder(LocalDate.now(), new DummyConverter(), "LocalDate source; method still returns LONG_ZERO."), // LocalDateTime source, null converter
        new ArgumentsHolder(LocalDateTime.now(), null, "LocalDateTime source; method ignores it and returns the constant LONG_ZERO."), // Instant source, mock converter
        new ArgumentsHolder(Instant.now(), MockConverter.INSTANCE, "Instant source; method returns LONG_ZERO irrespective of temporal value."), // int array source, dummy converter
        new ArgumentsHolder(new int[] { 1, 2, 3 }, new DummyConverter(), "Array source; method does not attempt to convert array elements and returns LONG_ZERO."), // List source, mock converter
        new ArgumentsHolder(List.of(1, 2, 3), MockConverter.INSTANCE, "Collection source; method still returns the constant zero."), // String source with a converter that would throw if called
        new ArgumentsHolder("any string", new ThrowingConverter(), "String source with a converter that would throw if invoked; method never calls the converter and returns LONG_ZERO."));
    }

    /**
     * Safe string representation for debugging – handles nulls and arrays.
     */
    private static String safeToString(Object obj) {
        if (obj == null)
            return "null";
        if (obj.getClass().isArray())
            return Arrays.deepToString(new Object[] { obj });
        return obj.toString();
    }

    @Test
    void toLongZero_returnsZeroWhenSourceIsNull() {
        Long result = NumberConversions.toLongZero(null, mockConverter);
        assertNotNull(result, "Result should never be null");
        assertEquals(0L, result.longValue(), "Expected Long zero for null source");
    }

    @Test
    void toLongZero_returnsZeroWhenSourceIsAString() {
        Long result = NumberConversions.toLongZero("12345", mockConverter);
        assertNotNull(result);
        assertEquals(0L, result.longValue(), "Expected Long zero regardless of source content");
    }

    @Test
    void toLongZero_returnsZeroWhenSourceIsANumber() {
        Long result = NumberConversions.toLongZero(987L, mockConverter);
        assertNotNull(result);
        assertEquals(0L, result.longValue(), "Expected Long zero regardless of source type");
    }

    @Test
    void toLongZero_returnsZeroWhenSourceIsAnObject() {
        Object arbitrary = new Object();
        Long result = NumberConversions.toLongZero(arbitrary, mockConverter);
        assertNotNull(result);
        assertEquals(0L, result.longValue(), "Expected Long zero for any object source");
    }

    @Test
    void toLongZero_ignoresConverterBehaviour() {
        // Even if the mock converter were to be called, the method does not use it.
        // Verify that the returned value is still zero.
        Long result = NumberConversions.toLongZero("any", mockConverter);
        assertEquals(0L, result);
    }

    /**
     * Parameterized test that verifies {@code NumberConversions.toLongZero} always returns
     * {@link CommonValues#LONG_ZERO} regardless of the supplied arguments.
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("provideTestCases")
    void toLongZero_alwaysReturnsConstantZero(ArgumentsHolder holder) {
        // Arrange
        Object from = holder.from;
        Converter converter = holder.converter;
        // Act
        Long result = NumberConversions.toLongZero(from, converter);
        // Assert
        assertNotNull(result, "Result must never be null");
        assertEquals(0L, result.longValue(), () -> "Expected LONG_ZERO (0L) for input: from=" + safeToString(from) + ", converter=" + (converter == null ? "null" : converter.getClass().getSimpleName()));
    }
}
