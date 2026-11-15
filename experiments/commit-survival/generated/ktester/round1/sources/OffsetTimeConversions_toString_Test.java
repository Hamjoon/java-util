package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class OffsetTimeConversions_toString_Test {

    /**
     * A mock Converter – the method under test does not use it, but it must be supplied.
     */
    @Mock
    Converter mockConverter;

    /**
     * Example OffsetTime used in the tests.
     */
    private OffsetTime sampleOffsetTime;

    @BeforeEach
    void setupBeforeEach() {
        // Create a deterministic OffsetTime (2023‑01‑01T12:34:56.789+02:00)
        sampleOffsetTime = OffsetTime.of(12, 34, 56, 789_000_000, ZoneOffset.ofHours(2));
    }

    /**
     * Provides a stream of arguments for {@link #toString_parameterised(Object, Converter, Object)}.
     *
     * Each {@code Arguments} consists of:
     * <ul>
     *   <li>{@code from} – the source object passed to {@code toString}</li>
     *   <li>{@code converter} – the {@link Converter} argument (null or a dummy implementation)</li>
     *   <li>{@code expected} – either the expected ISO‑8601 string or the {@link Class} of the
     *        exception that should be thrown.</li>
     * </ul>
     */
    private static Stream<Arguments> toStringProvider() {
        // A dummy Converter that simply returns the source object – used for the single
        // test case that supplies a non‑null converter.
        Converter dummyConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> dest) {
                return src;
            }
        };
        return Stream.of(// Standard UTC case
        Arguments.of(OffsetTime.of(12, 30, 45, 0, ZoneOffset.UTC), null, "12:30:45Z"), // Edge case: maximum nanosecond precision with a positive offset
        Arguments.of(OffsetTime.of(23, 59, 59, 999_999_999, ZoneOffset.ofHoursMinutes(5, 30)), null, "23:59:59.999999999+05:30"), // Negative offset without minutes
        Arguments.of(OffsetTime.of(0, 0, 0, 0, ZoneOffset.ofHours(-7)), null, "00:00:00-07:00"), // Null input – should throw NPE
        Arguments.of(null, null, NullPointerException.class), // Invalid type (String) – should throw ClassCastException
        Arguments.of("2021-01-01T12:00:00Z", null, ClassCastException.class), // UTC with non‑zero nanoseconds
        Arguments.of(OffsetTime.of(12, 0, 0, 123_456_789, ZoneOffset.UTC), null, "12:00:00.123456789Z"), // Positive offset with hour and minute components
        Arguments.of(OffsetTime.of(8, 15, 30, 0, ZoneOffset.ofHoursMinutes(2, 45)), null, "08:15:30+02:45"), // Negative offset with hour and minute components
        Arguments.of(OffsetTime.of(5, 5, 5, 0, ZoneOffset.ofHoursMinutes(-3, -15)), null, "05:05:05-03:15"), // Fractional second that ends with trailing zeros (should be trimmed)
        Arguments.of(OffsetTime.of(23, 59, 59, 500_000_000, ZoneOffset.UTC), null, "23:59:59.5Z"), // Non‑null Converter instance – method must ignore it
        Arguments.of(OffsetTime.of(0, 0, 0, 0, ZoneOffset.UTC), dummyConverter, "00:00:00Z"), // Explicit zero offset created via ZoneOffset.ofHours(0) – should render as 'Z'
        Arguments.of(OffsetTime.of(0, 0, 0, 0, ZoneOffset.ofHours(0)), null, "00:00:00Z"));
    }

    @Test
    void toString_returnsIsoOffsetTimeString() {
        // GIVEN
        // the object to convert
        Object from = sampleOffsetTime;
        // mockConverter is already injected by Mockito; it is not used by the method.
        // WHEN
        String result = OffsetTimeConversions.toString(from, mockConverter);
        // THEN
        // Expected format is the ISO_OFFSET_TIME representation, e.g. "12:34:56.789+02:00"
        String expected = sampleOffsetTime.format(java.time.format.DateTimeFormatter.ISO_OFFSET_TIME);
        assertEquals(expected, result, "The toString method should format the OffsetTime using ISO_OFFSET_TIME");
    }

    @Test
    void toString_handlesNullInputGracefully() {
        // GIVEN
        Object from = null;
        // WHEN / THEN
        // The method casts the input to OffsetTime, so a null should cause a NullPointerException.
        // This test documents that behaviour.
        assertThrows(NullPointerException.class, () -> OffsetTimeConversions.toString(from, mockConverter));
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("toStringProvider")
    void toString_parameterised(Object from, Converter converter, Object expected) {
        // The method under test does not use the Converter argument, but we still pass it
        // to verify that its presence (or absence) does not affect the outcome.
        if (expected instanceof Class) {
            // Expected an exception – verify that the correct type is thrown.
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected;
            assertThrows(exClass, () -> OffsetTimeConversions.toString(from, converter), () -> "Expected exception " + exClass.getSimpleName() + " for input: " + from);
        } else {
            // Expected a successful conversion to a string.
            String result = OffsetTimeConversions.toString(from, converter);
            assertEquals(expected, result, () -> "Incorrect ISO_OFFSET_TIME formatting for input: " + from);
        }
    }
}
