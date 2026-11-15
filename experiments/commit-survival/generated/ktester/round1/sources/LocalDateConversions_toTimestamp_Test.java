package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class LocalDateConversions_toTimestamp_Test {

    /**
     * The Converter dependency is not used by {@code LocalDateConversions.toTimestamp},
     * but the method signature requires it, so we provide a mock.
     */
    @Mock
    private Converter mockConverter;

    /* -------------------------------------------------------------
     *  Valid conversion scenarios – parameterized test
     * ------------------------------------------------------------- */
    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// epoch start – should be exactly 0 ms
        arguments(LocalDate.of(1970, 1, 1), 0L), // a recent date – forward conversion
        arguments(LocalDate.of(2023, 3, 15), LocalDate.of(2023, 3, 15).toEpochDay() * 86_400L * 1_000L), // pre‑epoch date – negative milliseconds
        arguments(LocalDate.of(1960, 1, 1), LocalDate.of(1960, 1, 1).toEpochDay() * 86_400L * 1_000L), // leap‑day date – ensures leap‑year handling
        arguments(LocalDate.of(2000, 2, 29), LocalDate.of(2000, 2, 29).toEpochDay() * 86_400L * 1_000L), // one day after epoch – 86 400 000 ms
        arguments(LocalDate.of(1970, 1, 2), 86_400_000L), // maximum date that still fits into a signed 64‑bit millisecond value
        // (the calculation overflows to Long.MAX_VALUE by design of the method)
        arguments(LocalDate.of(292278994, 8, 17), Long.MAX_VALUE), // date one day beyond the maximum – overflow wraps to Long.MIN_VALUE
        arguments(LocalDate.of(292278994, 8, 18), Long.MIN_VALUE));
    }

    @Test
    void toTimestamp_epochStart_returnsZeroTimestamp() {
        // GIVEN
        // epoch day = 0
        LocalDate epochStart = LocalDate.of(1970, 1, 1);
        // WHEN
        Timestamp ts = LocalDateConversions.toTimestamp(epochStart, mockConverter);
        // THEN
        assertNotNull(ts, "Timestamp should not be null");
        assertEquals(0L, ts.getTime(), "Timestamp for 1970‑01‑01 should be 0 milliseconds");
    }

    @Test
    void toTimestamp_arbitraryDate_returnsCorrectTimestamp() {
        // GIVEN
        LocalDate date = LocalDate.of(2023, 8, 15);
        // days → seconds → milliseconds
        long expectedMillis = date.toEpochDay() * 86_400L * 1_000L;
        // WHEN
        Timestamp ts = LocalDateConversions.toTimestamp(date, mockConverter);
        // THEN
        assertNotNull(ts, "Timestamp should not be null");
        assertEquals(expectedMillis, ts.getTime(), "Timestamp does not match expected milliseconds for " + date);
    }

    @Test
    void toTimestamp_nullInput_throwsNullPointerException() {
        // GIVEN
        Object nullObj = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> LocalDateConversions.toTimestamp(nullObj, mockConverter), "Passing null should result in NullPointerException");
    }

    @ParameterizedTest(name = "[{index}] {0} → expected ms = {1}")
    @MethodSource("validConversionProvider")
    void toTimestamp_validConversions_returnsExpectedTimestamp(LocalDate from, long expectedMillis) {
        // WHEN
        Timestamp ts = LocalDateConversions.toTimestamp(from, mockConverter);
        // THEN
        assertNotNull(ts, "Timestamp must not be null");
        assertEquals(expectedMillis, ts.getTime(), () -> "Timestamp milliseconds for " + from + " did not match expected value");
    }

    /* -------------------------------------------------------------
     *  Invalid input – type mismatch
     * ------------------------------------------------------------- */
    @Test
    void toTimestamp_stringInput_throwsClassCastException() {
        // GIVEN
        Object wrongType = "2023-03-15";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> LocalDateConversions.toTimestamp(wrongType, mockConverter), "Passing a non‑LocalDate object should throw ClassCastException");
    }
}
