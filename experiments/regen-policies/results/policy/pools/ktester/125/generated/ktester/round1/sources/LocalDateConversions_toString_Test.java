package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.*;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class LocalDateConversions_toString_Test {

    @Mock
    private Converter // Converter is not used by the method, but required by signature
    mockConverter;

    /**
     * Provides a stream of arguments for the parameterised test.
     *
     * Each {@link Arguments} instance contains:
     * <ul>
     *   <li>the {@code from} value (Object)</li>
     *   <li>the {@code converter} value (Converter – may be {@code null})</li>
     *   <li>the expected outcome – either a {@link String} result or an exception class</li>
     *   <li>a flag indicating whether an exception is expected</li>
     * </ul>
     */
    static Stream<Arguments> toStringCases() {
        // A Converter implementation that would throw if ever invoked.
        Converter throwingConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                throw new RuntimeException("Converter should not be invoked");
            }
        };
        return Stream.of(// Standard case
        arguments(LocalDate.of(2023, 1, 15), null, "2023-01-15", false), // Leap‑year case
        arguments(LocalDate.of(2000, 2, 29), null, "2000-02-29", false), // Minimum supported LocalDate
        arguments(LocalDate.MIN, null, "-999999999-01-01", false), // Maximum supported LocalDate
        arguments(LocalDate.MAX, null, "+999999999-12-31", false), // Null input – should raise NPE
        arguments(null, null, NullPointerException.class, true), // Invalid type – String instead of LocalDate
        arguments("2023-01-15", null, ClassCastException.class, true), // Wrong but related type – LocalDateTime
        arguments(LocalDateTime.of(2023, 1, 15, 10, 30), null, ClassCastException.class, true), // Non‑null Converter (throws if used) – method must ignore it
        arguments(LocalDate.of(2023, 12, 31), throwingConverter, "2023-12-31", false), // Year zero
        arguments(LocalDate.of(0, 1, 1), null, "0000-01-01", false), // Negative year (1 BC)
        arguments(LocalDate.of(-1, 12, 31), null, "-0001-12-31", false), // Small positive year (<1000) – zero‑padded
        arguments(LocalDate.of(5, 1, 1), null, "0005-01-01", false), // Single‑digit month and day – zero‑padded
        arguments(LocalDate.of(2023, 3, 5), null, "2023-03-05", false), // Double‑digit month and day – unchanged
        arguments(LocalDate.of(2023, 10, 10), null, "2023-10-10", false), // Round‑trip test (parse then format)
        arguments(LocalDate.parse("2022-08-15"), null, "2022-08-15", false), // High year within range
        arguments(LocalDate.of(9999, 12, 31), null, "9999-12-31", false), // Converter that would throw if used – same as earlier but with a different date
        arguments(LocalDate.of(2023, 1, 15), throwingConverter, "2023-01-15", false));
    }

    @Test
    void toString_ShouldReturnIsoLocalDateString_ForValidLocalDate() {
        // GIVEN
        LocalDate date = LocalDate.of(2023, 5, 17);
        String expected = "2023-05-17";
        // WHEN
        String actual = LocalDateConversions.toString(date, mockConverter);
        // THEN
        assertEquals(expected, actual);
        // Verify that the converter was not interacted with (method is independent of it)
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toString_ShouldThrowNullPointerException_WhenSourceIsNull() {
        // GIVEN
        Object nullSource = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> LocalDateConversions.toString(nullSource, mockConverter));
        // No interaction with the converter is expected
        verifyNoInteractions(mockConverter);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("toStringCases")
    void toString_VariousScenarios(Object from, Converter converter, Object expected, boolean expectException) {
        // When the test supplies a mock, we want to ensure the method never touches it.
        // The mock is injected via the @Mock field; for all other converters we simply
        // pass them through.
        Converter effectiveConverter = (converter != null) ? converter : mockConverter;
        if (expectException) {
            Class<? extends Throwable> exceptionClass = (Class<? extends Throwable>) expected;
            assertThrows(exceptionClass, () -> LocalDateConversions.toString(from, effectiveConverter), () -> "Expected exception " + exceptionClass.getSimpleName());
        } else {
            String expectedString = (String) expected;
            String actual = LocalDateConversions.toString(from, effectiveConverter);
            assertEquals(expectedString, actual, () -> "Formatted string does not match for input: " + from);
        }
        // The method must never interact with the Converter, regardless of its concrete type.
        verifyNoInteractions(mockConverter);
    }
}
