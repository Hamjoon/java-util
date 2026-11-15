package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.sql.Timestamp;
import java.time.LocalDate;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalDateConversions_toTimestamp_Test {

    /**
     * A real {@code Converter} instance – the conversion logic does not depend on it.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter requires a ConverterOptions instance; the default options are sufficient.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference.
        converter = null;
    }

    /**
     * Helper that replicates the conversion formula used in the target method.
     */
    private static long expectedMillisFrom(LocalDate date) {
        // epochDay is the number of days since 1970‑01‑01.
        // One day = 86_400 seconds = 86_400_000 milliseconds.
        return date.toEpochDay() * 86_400_000L;
    }

    private static Stream<Arguments> validLocalDateProvider() {
        ???;
    }

    private static Stream<Arguments> invalidInputProvider() {
        return Stream.of(Arguments.of(null, null, NullPointerException.class), Arguments.of("2023-01-01", null, ClassCastException.class));
    }

    @Test
    void convertsEpochStartToZeroTimestamp() {
        // 1970‑01‑01 is the epoch start; its epoch day is 0.
        LocalDate epoch = LocalDate.of(1970, 1, 1);
        Timestamp ts = LocalDateConversions.toTimestamp(epoch, converter);
        assertNotNull(ts, "Timestamp should not be null");
        assertEquals(0L, ts.getTime(), "Timestamp for epoch start must be 0");
    }

    @Test
    void convertsTypicalDateCorrectly() {
        LocalDate date = LocalDate.of(2023, 1, 1);
        Timestamp ts = LocalDateConversions.toTimestamp(date, converter);
        long expected = expectedMillisFrom(date);
        assertEquals(expected, ts.getTime(), () -> "Timestamp for " + date + " should be " + expected);
    }

    @Test
    void convertsLeapDayCorrectly() {
        LocalDate leapDay = LocalDate.of(2020, 2, 29);
        Timestamp ts = LocalDateConversions.toTimestamp(leapDay, converter);
        long expected = expectedMillisFrom(leapDay);
        assertEquals(expected, ts.getTime(), () -> "Timestamp for leap day " + leapDay + " should be " + expected);
    }

    @Test
    void throwsNullPointerExceptionWhenSourceIsNull() {
        // The implementation casts the source to LocalDate without a null check,
        // therefore a NPE is expected.
        assertThrows(NullPointerException.class, () -> LocalDateConversions.toTimestamp(null, converter));
    }

    @Test
    void throwsClassCastExceptionWhenSourceIsWrongType() {
        // Passing a type other than LocalDate triggers the cast failure.
        assertThrows(ClassCastException.class, () -> LocalDateConversions.toTimestamp("2023-01-01", converter));
    }

    @ParameterizedTest(name = "[{index}] {0} with converter={2}")
    @MethodSource("validLocalDateProvider")
    void toTimestamp_validLocalDate(LocalDate input, Converter conv, long expectedMillis) {
        Timestamp ts = LocalDateConversions.toTimestamp(input, conv);
        assertNotNull(ts, "Timestamp must not be null");
        assertEquals(expectedMillis, ts.getTime(), () -> "Timestamp for " + input + " should have millis " + expectedMillis);
    }

    @ParameterizedTest(name = "[{index}] from={0}")
    @MethodSource("invalidInputProvider")
    void toTimestamp_invalidInput(Object from, Converter conv, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> LocalDateConversions.toTimestamp(from, conv), () -> "Expected " + expectedException.getSimpleName() + " for input: " + from);
    }
}
