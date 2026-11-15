package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verifyNoInteractions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DoubleConversions_toDate_Test {

    @Mock
    private Converter // not used by the method, but required by the signature
    mockConverter;

    @AfterEach
    void teardownAfterEach() {
        // Ensure the method under test does not interact with the Converter.
        verifyNoInteractions(mockConverter);
    }

    private static Stream<Arguments> toDateProvider() {
        return Stream.of(// Standard positive whole‑second value
        Arguments.of("Standard positive whole‑second value – converts epoch seconds to correct Date (2021‑06‑01T00:00:00Z)", 1622548800.0, 1622548800000L, null), // Fractional seconds
        Arguments.of("Fractional seconds – verifies that milliseconds are correctly derived from the double fraction.", 1622548800.123, 1622548800123L, null), // Zero seconds
        Arguments.of("Zero seconds – should produce the epoch start Date (1970‑01‑01T00:00:00Z).", 0.0, 0L, null), // Negative seconds (one day before epoch)
        Arguments.of("Negative seconds – one day before the epoch, confirming handling of negative timestamps.", -86400.0, -86400000L, null), // Very large double that overflows long after multiplication
        Arguments.of("Very large double that, when multiplied by 1000, exceeds Long.MAX_VALUE; casting saturates to Long.MAX_VALUE.", 9_223_372_036_854_776.0, Long.MAX_VALUE, null), // Double.NaN – casting to long yields 0
        Arguments.of("Double.NaN – casting NaN to long yields 0, resulting in the epoch Date.", Double.NaN, 0L, null), // Double.POSITIVE_INFINITY – casting to long yields Long.MAX_VALUE
        Arguments.of("Double.POSITIVE_INFINITY – casting to long yields Long.MAX_VALUE.", Double.POSITIVE_INFINITY, Long.MAX_VALUE, null), // Null `from` argument – should throw NullPointerException
        Arguments.of("Null `from` argument – the cast to Double triggers a NullPointerException.", null, NullPointerException.class, null), // Non‑Double input (Integer) – should throw ClassCastException
        Arguments.of("`from` is not a Double (Integer provided) – should raise ClassCastException.", 123, ClassCastException.class, null), // Non‑null Converter argument – method ignores the converter
        Arguments.of("Non‑null Converter argument – method ignores the converter, outcome identical to case 1.", 1622548800.0, // we will replace null with mock in the test body
        1622548800000L, /* use the mock */
        null));
    }

    private static Stream<Arguments> toDateAdditionalProvider() {
        return Stream.of(// Negative fractional seconds – truncates toward zero
        Arguments.of("Negative fractional seconds – verifies truncation toward zero when converting -0.75 s to milliseconds (-750 ms).", -0.75, -750L, null), // Very small positive fraction (< 1 ms) – becomes 0
        Arguments.of("Very small positive fraction (< 1 ms) – casting to long should drop the fractional part, yielding 0 ms.", 0.0009, 0L, null), // Just‑above‑1‑ms fraction – truncates to 1 ms
        Arguments.of("Just‑above‑1‑ms fraction – 0.0012 s → 1.2 ms, truncates to 1 ms.", 0.0012, 1L, null), // Double.NEGATIVE_INFINITY – casting to long yields Long.MIN_VALUE
        Arguments.of("Double.NEGATIVE_INFINITY – casting to long yields Long.MIN_VALUE.", Double.NEGATIVE_INFINITY, Long.MIN_VALUE, null), // Double.MAX_VALUE – overflow after multiplication, saturates to Long.MAX_VALUE
        Arguments.of("Double.MAX_VALUE – after multiplication overflow, cast to long saturates to Long.MAX_VALUE.", Double.MAX_VALUE, Long.MAX_VALUE, null), // Double.MIN_VALUE – smallest positive non‑zero double, still < 1 ms → 0
        Arguments.of("Double.MIN_VALUE (smallest positive non‑zero double) – multiplied by 1000 still < 1, cast to long yields 0.", Double.MIN_VALUE, 0L, null), // Large negative double beyond Long.MIN_VALUE/1000 – becomes Long.MIN_VALUE
        Arguments.of("Large negative double beyond Long.MIN_VALUE/1000 – casting results in Long.MIN_VALUE.", -9_223_372_036_854_776.0, Long.MIN_VALUE, null), // Negative zero – treated as 0 when cast to long
        Arguments.of("Negative zero – double -0.0 is treated as 0 when cast to long, producing epoch Date.", -0.0, 0L, null), // Float input – should throw ClassCastException
        Arguments.of("`from` is a Float, not a Double – the explicit cast to Double should throw ClassCastException.", Float.valueOf(1.23f), ClassCastException.class, null), // String input – should throw ClassCastException
        Arguments.of("`from` is a String representing a number – still not a Double, so a ClassCastException is expected.", "123.45", ClassCastException.class, null));
    }

    @Test
    void testPositiveDoubleConversion() {
        // 1.5 seconds
        Double input = 1.5;
        Date result = DoubleConversions.toDate(input, mockConverter);
        assertEquals(1500L, result.getTime(), "1.5 seconds should become 1500 milliseconds");
    }

    @Test
    void testZeroDoubleConversion() {
        Double input = 0.0;
        Date result = DoubleConversions.toDate(input, mockConverter);
        assertEquals(0L, result.getTime(), "Zero seconds should become epoch (0 ms)");
    }

    @Test
    void testNegativeDoubleConversion() {
        // -2 seconds
        Double input = -2.0;
        Date result = DoubleConversions.toDate(input, mockConverter);
        assertEquals(-2000L, result.getTime(), "-2 seconds should become -2000 milliseconds");
    }

    @Test
    void testLargeDoubleConversion() {
        // 1,000,000 seconds
        Double input = 1_000_000.0;
        Date result = DoubleConversions.toDate(input, mockConverter);
        assertEquals(1_000_000_000L, result.getTime(), "1,000,000 seconds should become 1,000,000,000 milliseconds");
    }

    @Test
    void testNonDoubleInputThrowsClassCastException() {
        // Not a Double
        Integer input = 42;
        assertThrows(ClassCastException.class, () -> DoubleConversions.toDate(input, mockConverter), "Passing a non‑Double should result in ClassCastException");
    }

    @Test
    void testNullFromThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> DoubleConversions.toDate(null, mockConverter), "Passing null as the source should result in NullPointerException");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("toDateProvider")
    void testToDateGroup(String description, Object from, Object expected, Converter converter) {
        // The method under test should never touch the converter – the verification
        // is performed in @AfterEach via {@code verifyNoInteractions}.
        if (expected instanceof Class) {
            // Expected an exception.
            @SuppressWarnings("unchecked")
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected;
            assertThrows(exClass, () -> DoubleConversions.toDate(from, converter), description);
        } else {
            // Expected a normal conversion.
            Date result = DoubleConversions.toDate(from, converter);
            long expectedMillis = (Long) expected;
            assertEquals(expectedMillis, result.getTime(), description);
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("toDateAdditionalProvider")
    void testToDateAdditionalGroup(String description, Object from, Object expected, Converter converter) {
        if (expected instanceof Class) {
            @SuppressWarnings("unchecked")
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected;
            assertThrows(exClass, () -> DoubleConversions.toDate(from, converter), description);
        } else {
            Date result = DoubleConversions.toDate(from, converter);
            long expectedMillis = (Long) expected;
            assertEquals(expectedMillis, result.getTime(), description);
        }
    }
}
