package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocalDateConversions_toString_Test {

    /**
     * A mock Converter – the implementation of {@code toString} does not use it,
     *  but the method signature requires a non‑null argument.
     */
    @Mock
    Converter mockConverter;

    /**
     * Provides the test data for the main {@code toString} group.
     * Each {@link Arguments} consists of:
     * <ul>
     *   <li>the {@code from} object (may be {@link LocalDate}, {@code null}, or a wrong type)</li>
     *   <li>a {@link Converter} instance (may be {@code null} or a mock)</li>
     *   <li>the expected outcome – either a {@link String} with the ISO representation
     *       or a {@link Class} of the expected exception.</li>
     * </ul>
     */
    static Stream<Arguments> toStringProvider() {
        // a mock that will be used where the test specifies a non‑null Converter
        Converter nonNullMock = mock(Converter.class);
        return Stream.of(// standard conversion
        arguments(LocalDate.of(2023, 5, 17), null, "2023-05-17"), // non‑null converter (not used by the method)
        arguments(LocalDate.of(1970, 1, 1), nonNullMock, "1970-01-01"), // edge values
        arguments(LocalDate.MIN, null, "-999999999-01-01"), arguments(LocalDate.MAX, null, "+999999999-12-31"), // leap‑year date
        arguments(LocalDate.of(2020, 2, 29), null, "2020-02-29"), // null source – expect NPE
        arguments(null, null, NullPointerException.class), // wrong type – expect CCE
        arguments("2023-05-17", null, ClassCastException.class));
    }

    /**
     * Provides the test data for the additional edge‑case group.
     */
    static Stream<Arguments> toStringAdditionalProvider() {
        // regular non‑null mock
        Converter regularMock = mock(Converter.class);
        // a mock that throws on any method call – the method under test never calls it,
        // so it should not affect the result.
        Converter throwingMock = mock(Converter.class, invocation -> {
            throw new RuntimeException("Converter method should not be invoked");
        });
        return Stream.of(arguments(LocalDate.of(1, 1, 1), null, "0001-01-01"), arguments(LocalDate.of(0, 1, 1), null, "0000-01-01"), arguments(LocalDate.of(-5, 3, 15), null, "-0005-03-15"), arguments(LocalDate.of(9999, 12, 31), null, "9999-12-31"), arguments(LocalDate.of(2023, 4, 5), regularMock, "2023-04-05"), arguments(LocalDate.of(2023, 11, 9), throwingMock, "2023-11-09"));
    }

    @Test
    void toString_WithValidLocalDate_ReturnsIsoString() {
        // GIVEN
        LocalDate date = LocalDate.of(2023, 3, 15);
        String expected = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        // WHEN
        String actual = LocalDateConversions.toString(date, mockConverter);
        // THEN
        assertEquals(expected, actual, "LocalDateConversions.toString should format the date using ISO_LOCAL_DATE");
    }

    @Test
    void toString_WithNullArgument_ThrowsNullPointerException() {
        // GIVEN
        Object nullObj = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> LocalDateConversions.toString(nullObj, mockConverter), "Calling toString with a null source should throw NullPointerException");
    }

    @Test
    void toString_WithNonLocalDateArgument_ThrowsClassCastException() {
        // GIVEN
        Object notALocalDate = "2023-03-15";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> LocalDateConversions.toString(notALocalDate, mockConverter), "Calling toString with a non‑LocalDate argument should throw ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] toString({0}) => {2}")
    @MethodSource("toStringProvider")
    void toString_VariousInputs(Object from, Converter converter, Object expected) {
        if (expected instanceof Class) {
            // Expected an exception
            assertThrows((Class<? extends Throwable>) expected, () -> LocalDateConversions.toString(from, converter), () -> "Expected exception " + ((Class<?>) expected).getSimpleName());
        } else {
            // Expected a normal ISO‑8601 string
            String actual = LocalDateConversions.toString(from, converter);
            assertEquals(expected, actual, () -> "ISO string mismatch for input: " + from);
        }
    }

    @ParameterizedTest(name = "[{index}] toString({0}) => {2}")
    @MethodSource("toStringAdditionalProvider")
    void toString_AdditionalEdgeCases(Object from, Converter converter, String expected) {
        // The method never interacts with the converter, so we simply verify the output.
        String actual = LocalDateConversions.toString(from, converter);
        assertEquals(expected, actual, () -> "Unexpected ISO string for input: " + from);
    }
}
