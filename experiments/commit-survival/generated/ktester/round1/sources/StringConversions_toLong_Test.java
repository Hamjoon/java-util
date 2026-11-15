package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Collections;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StringConversions_toLong_Test {

    private static Converter converter;

    @BeforeEach
    void setupBeforeEach(@Mock ConverterOptions mockOptions) {
        // The Converter constructor expects a ConverterOptions instance.
        // We mock it and make it return an empty map for overrides.
        Map<Class<?>, Object> empty = Collections.emptyMap();
        // The mock is injected by Mockito; we configure its behaviour here.
        // Since ConverterOptions is not part of the public API we only need the
        // getConverterOverrides() method to return an empty map.
        // Using a lambda to avoid a strict stub for any other method.
        when(mockOptions.getConverterOverrides()).thenReturn(empty);
        converter = new Converter(mockOptions);
    }

    /**
     * Supplies the test data.  Each {@code Arguments} instance contains:
     * <ul>
     *   <li>the raw {@code from} argument passed to {@code toLong}</li>
     *   <li>the expected outcome – either a {@code Long} or an exception {@code Class}</li>
     * </ul>
     */
    private static Stream<Arguments> provideToLongTestCases() {
        return Stream.of(/* Empty or Null Input */
        Arguments.of("", 0L), Arguments.of(null, 0L), /* Valid Long Strings */
        Arguments.of("1234567890", 1234567890L), Arguments.of("9223372036854775807", Long.MAX_VALUE), /* Fallback Parsing Returns Long */
        Arguments.of("123.0", 123L), /* Fallback Parsing Failure – IllegalArgumentException */
        Arguments.of("abc", IllegalArgumentException.class), Arguments.of("123.456", IllegalArgumentException.class), Arguments.of("9223372036854775808", IllegalArgumentException.class), Arguments.of("-9223372036854775809", IllegalArgumentException.class), /* Non-String Input */
        Arguments.of(123, ClassCastException.class), /* Whitespace and Sign Handling */
        Arguments.of("   ", IllegalArgumentException.class), Arguments.of("+456", 456L), Arguments.of("-0", 0L), /* Boundary Values */
        Arguments.of("-9223372036854775808", Long.MIN_VALUE), Arguments.of("00000123", 123L), /* Scientific Notation */
        Arguments.of("1e3", 1000L), Arguments.of("1.23e3", 1230L), /* Sign‑Only Strings */
        Arguments.of("+", IllegalArgumentException.class), Arguments.of("-", IllegalArgumentException.class));
    }

    @Test
    void emptyStringReturnsZero() {
        Long result = StringConversions.toLong("", converter);
        assertEquals(0L, result);
    }

    @Test
    void nullInputReturnsZero() {
        Long result = StringConversions.toLong(null, converter);
        assertEquals(0L, result);
    }

    @Test
    void validLongStringIsParsed() {
        Long result = StringConversions.toLong("123456789", converter);
        assertEquals(123456789L, result);
    }

    @Test
    void outOfRangeStringThrowsIllegalArgumentException() {
        // Long.MAX_VALUE + 1
        String outOfRange = "9223372036854775808";
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toLong(outOfRange, converter));
        assertTrue(ex.getMessage().contains("not parseable as a long value"));
    }

    @Test
    void nonNumericStringThrowsIllegalArgumentException() {
        String bad = "notANumber";
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toLong(bad, converter));
        assertTrue(ex.getMessage().contains("not parseable as a long value"));
    }

    @ParameterizedTest(name = "[{index}] from=''{0}'' → expected={1}")
    @MethodSource("provideToLongTestCases")
    void toLong_variousInputs(Object from, Object expected) {
        if (expected instanceof Class) {
            // Expected an exception.
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected;
            Throwable thrown = assertThrows(exClass, () -> StringConversions.toLong(from, converter));
            // For IllegalArgumentException we also verify that the message mentions parsing.
            if (exClass == IllegalArgumentException.class) {
                assertTrue(thrown.getMessage().contains("not parseable as a long value"), "Exception message should contain parsing hint");
            }
        } else {
            // Expected a concrete Long value.
            Long result = StringConversions.toLong(from, converter);
            assertEquals(expected, result);
        }
    }
}
