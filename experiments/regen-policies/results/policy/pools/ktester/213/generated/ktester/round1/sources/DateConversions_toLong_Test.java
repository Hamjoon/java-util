package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Date;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DateConversions_toLong_Test {

    /**
     * Shared Converter instance – the method does not use it but the signature requires it.
     */
    private static Converter sharedConverter;

    /**
     * A custom {@link Date} subclass whose {@code getTime()} method deliberately
     * throws an {@link IllegalStateException}. This helps verify that {@code toLong}
     * propagates runtime exceptions from the underlying {@code Date} implementation.
     */
    private static final class BadDate extends Date {

        @Override
        public long getTime() {
            throw new IllegalStateException("BadDate.getTime() failure");
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; use defaults.
        ConverterOptions options = new ConverterOptions();
        sharedConverter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No external resources to clean up.
        sharedConverter = null;
    }

    private static Stream<Arguments> validDateProvider() {
        return Stream.of(// Unix epoch (0 ms)
        Arguments.of("Convert a Date representing the Unix epoch (0 milliseconds) to a long.", new Date(0L), 0L), // Typical positive timestamp
        Arguments.of("Convert a typical Date with a positive millisecond value to a long.", new Date(1622548800123L), 1622548800123L), // java.sql.Timestamp extends java.util.Date
        Arguments.of("Convert a java.sql.Timestamp to a long; should return same millisecond value.", new Timestamp(1622548800123L), 1622548800123L), // Negative epoch (pre‑1970)
        Arguments.of("Convert a Date with a negative epoch time to a long.", new Date(-123456789L), -123456789L));
    }

    private static Stream<Arguments> invalidInputProvider() {
        return Stream.of(// Null source – NPE when calling getTime()
        Arguments.of("Passing a null reference as the source object results in NullPointerException.", null, NullPointerException.class), // Wrong type – String
        Arguments.of("Passing a String (not a Date) causes ClassCastException.", "2021-01-01", ClassCastException.class), // Wrong type – generic Object
        Arguments.of("Passing an Object that does not inherit from Date triggers ClassCastException.", new Object(), ClassCastException.class));
    }

    @Test
    void testToLongReturnsCorrectEpochMillis() {
        // GIVEN
        long expected = System.currentTimeMillis();
        Date date = new Date(expected);
        // WHEN
        long actual = DateConversions.toLong(date, sharedConverter);
        // THEN
        assertEquals(expected, actual, "toLong should return the exact epoch milliseconds contained in the Date");
    }

    @Test
    void testToLongThrowsClassCastExceptionWhenSourceIsNotDate() {
        // GIVEN
        Object notADate = "2023-01-01";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> DateConversions.toLong(notADate, sharedConverter), "toLong should throw ClassCastException when the source object is not a Date");
    }

    @Test
    void testToLongThrowsNullPointerExceptionWhenSourceIsNull() {
        // GIVEN
        Object nullSource = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> DateConversions.toLong(nullSource, sharedConverter), "toLong should throw NullPointerException when the source object is null");
    }

    /* -------------------------------------------------------------
     *  Valid Date conversions
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validDateProvider")
    void testValidDateConversions(String description, Object from, Long expected) {
        // GIVEN: 'from' is either a java.util.Date or a subclass (e.g., Timestamp)
        // WHEN
        // converter is ignored
        long actual = DateConversions.toLong(from, null);
        // THEN
        assertEquals(expected.longValue(), actual, () -> "Expected " + expected + " but got " + actual + " for case: " + description);
    }

    /* -------------------------------------------------------------
     *  Invalid inputs / exceptional cases
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidInputProvider")
    void testInvalidInputs(String description, Object from, Class<? extends Throwable> expectedException) {
        // GIVEN: a non‑Date or null source
        // WHEN / THEN
        assertThrows(expectedException, () -> DateConversions.toLong(from, null), () -> "Expected exception " + expectedException.getSimpleName() + " for case: " + description);
    }

    @Test
    void testBadDateSubclassExceptionPropagation() {
        // GIVEN
        BadDate badDate = new BadDate();
        // WHEN / THEN
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> DateConversions.toLong(badDate, null), "BadDate#getTime() should propagate its IllegalStateException");
        assertEquals("BadDate.getTime() failure", ex.getMessage(), "Exception message should match the one thrown by BadDate");
    }
}
