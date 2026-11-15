package com.cedarsoftware.util.convert;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.math.BigInteger;
import java.util.Date;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.Calendar;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class DateConversions_toBigInteger_Test {

    /* --------------------------------------------------------------------- *
     *  Helper that mimics the real InstantConversions behaviour for the
     *  purpose of these unit tests.
     * --------------------------------------------------------------------- */
    private static MockedStatic<InstantConversions> mockInstantConversions() {
        MockedStatic<InstantConversions> mocked = mockStatic(InstantConversions.class);
        mocked.when(() -> InstantConversions.toBigInteger(any(Instant.class), any())).thenAnswer(invocation -> {
            Instant i = invocation.getArgument(0);
            // Truncate any sub‑second part – this mirrors the expected behaviour.
            return BigInteger.valueOf(i.getEpochSecond());
        });
        return mocked;
    }

    /* --------------------------------------------------------------------- *
     *  Parameter provider for the “Valid conversions – primitive and common
     *  date types” group.
     * --------------------------------------------------------------------- */
    private static Stream<Arguments> validPrimitiveAndCommonProvider() {
        return Stream.of(// Date at epoch → 0
        Arguments.of(new Date(0L), null, BigInteger.ZERO), // Instant → same seconds
        Arguments.of(Instant.ofEpochSecond(12345L), null, BigInteger.valueOf(12345L)), // LocalDateTime at epoch (system default zone) → 0
        Arguments.of(LocalDateTime.of(1970, 1, 1, 0, 0, 0), null, BigInteger.ZERO), // ISO‑8601 UTC string at epoch → 0
        Arguments.of("1970-01-01T00:00:00Z", null, BigInteger.ZERO), // java.sql.Timestamp at epoch → 0
        Arguments.of(Timestamp.valueOf("1970-01-01 00:00:00"), null, BigInteger.ZERO), // Calendar set to epoch UTC → 0
        Arguments.of(createUtcCalendar(0L), null, BigInteger.ZERO), // Long representing epoch seconds → same value
        Arguments.of(Long.valueOf(9876543210L), null, BigInteger.valueOf(9876543210L)));
    }

    private static Calendar createUtcCalendar(long epochMilli) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(epochMilli);
        return cal;
    }

    /* --------------------------------------------------------------------- *
     *  Additional valid conversions – non‑epoch dates
     * --------------------------------------------------------------------- */
    private static Stream<Arguments> additionalValidProvider() {
        return Stream.of(// java.util.Date → 1234567890 seconds
        Arguments.of(new Date(1_234_567_890_000L), null, BigInteger.valueOf(1_234_567_890L)), // LocalDate one day after epoch → 86400 seconds
        Arguments.of(LocalDate.of(1970, 1, 2), null, BigInteger.valueOf(86_400L)), // ZonedDateTime at epoch UTC → 0
        Arguments.of(ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")), null, BigInteger.ZERO), // OffsetDateTime at epoch UTC → 0
        Arguments.of(OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), null, BigInteger.ZERO), // java.sql.Date 2020‑01‑02 → epoch seconds 1577923200
        Arguments.of(java.sql.Date.valueOf("2020-01-02"), null, BigInteger.valueOf(1_577_923_200L)), // BigInteger input should be returned unchanged
        Arguments.of(BigInteger.valueOf(5_555L), null, BigInteger.valueOf(5_555L)), // AtomicLong → same value
        Arguments.of(new AtomicLong(7_777L), null, BigInteger.valueOf(7_777L)), // Instant with nanos → truncated seconds
        Arguments.of(Instant.ofEpochSecond(10, 500_000_000L), null, BigInteger.valueOf(10L)), // Negative epoch seconds (Long) → same negative value
        Arguments.of(Long.valueOf(-86_400L), null, BigInteger.valueOf(-86_400L)));
    }

    @Test
    void toBigInteger_convertsDateViaInstant() {
        // GIVEN
        // Create a java.util.Date representing a known instant.
        // 12 345 seconds after epoch
        Date date = new Date(12_345L * 1_000L);
        Converter converter = mock(Converter.class);
        BigInteger expected = new BigInteger("999");
        // Mock the static InstantConversions.toBigInteger method.
        try (MockedStatic<InstantConversions> mockedInstantConversions = mockStatic(InstantConversions.class)) {
            mockedInstantConversions.when(() -> InstantConversions.toBigInteger(any(Instant.class), eq(converter))).thenReturn(expected);
            // WHEN
            BigInteger actual = DateConversions.toBigInteger(date, converter);
            // THEN
            assertEquals(expected, actual, "The returned BigInteger should be the one supplied by the mocked InstantConversions");
            mockedInstantConversions.verify(() -> InstantConversions.toBigInteger(any(Instant.class), eq(converter)), times(1));
        }
    }

    @ParameterizedTest(name = "[{index}] from={0}, expected={2}")
    @MethodSource("validPrimitiveAndCommonProvider")
    @DisplayName("Valid conversions – primitive and common date types")
    void testValidPrimitiveAndCommon(Object from, Converter converter, BigInteger expected) {
        try (MockedStatic<InstantConversions> mocked = mockInstantConversions()) {
            BigInteger actual = DateConversions.toBigInteger(from, converter);
            assertEquals(expected, actual);
            // Verify that the static conversion was invoked exactly once.
            mocked.verify(() -> InstantConversions.toBigInteger(any(Instant.class), eq(converter)), times(1));
        }
    }

    /* --------------------------------------------------------------------- *
     *  Null handling
     * --------------------------------------------------------------------- */
    @Test
    @DisplayName("Null handling – both arguments null")
    void testNullHandling() {
        assertNull(DateConversions.toBigInteger(null, null), "When the source object is null the method should return null");
    }

    /* --------------------------------------------------------------------- *
     *  Invalid / unsupported type handling
     * --------------------------------------------------------------------- */
    @Test
    @DisplayName("Invalid type – generic Object")
    void testInvalidObject() {
        Object bad = new Object();
        assertThrows(IllegalArgumentException.class, () -> DateConversions.toBigInteger(bad, null), "Unsupported type should raise IllegalArgumentException");
    }

    @Test
    @DisplayName("Invalid type – malformed date string")
    void testMalformedString() {
        String bad = "not-a-date";
        assertThrows(DateTimeParseException.class, () -> DateConversions.toBigInteger(bad, null), "Malformed ISO‑8601 string should cause DateTimeParseException");
    }

    /* --------------------------------------------------------------------- *
     *  Custom Converter usage – string "custom" mapped to Instant.ofEpochSecond(42)
     * --------------------------------------------------------------------- */
    @Test
    @DisplayName("Custom converter – maps string \"custom\" to epoch 42")
    void testCustomConverterString() {
        // Prepare a stub converter that knows how to handle the special string.
        Converter custom = mock(Converter.class);
        when(custom.convert(eq("custom"), eq(Instant.class))).thenReturn(Instant.ofEpochSecond(42L));
        try (MockedStatic<InstantConversions> mocked = mockInstantConversions()) {
            BigInteger result = DateConversions.toBigInteger("custom", custom);
            assertEquals(BigInteger.valueOf(42L), result);
            // Verify that the custom converter was indeed consulted.
            verify(custom, times(1)).convert(eq("custom"), eq(Instant.class));
        }
    }

    @ParameterizedTest(name = "[{index}] from={0}, expected={2}")
    @MethodSource("additionalValidProvider")
    @DisplayName("Additional valid conversions – non‑epoch dates")
    void testAdditionalValid(Object from, Converter converter, BigInteger expected) {
        try (MockedStatic<InstantConversions> mocked = mockInstantConversions()) {
            BigInteger actual = DateConversions.toBigInteger(from, converter);
            assertEquals(expected, actual);
            mocked.verify(() -> InstantConversions.toBigInteger(any(Instant.class), eq(converter)), times(1));
        }
    }

    /* --------------------------------------------------------------------- *
     *  Unsupported temporal sub‑types
     * --------------------------------------------------------------------- */
    @Test
    @DisplayName("Unsupported temporal sub‑type – LocalTime")
    void testUnsupportedLocalTime() {
        LocalTime lt = LocalTime.of(12, 34, 56);
        assertThrows(IllegalArgumentException.class, () -> DateConversions.toBigInteger(lt, null), "LocalTime lacks a date component and should raise IllegalArgumentException");
    }

    @Test
    @DisplayName("Unsupported temporal sub‑type – YearMonth")
    void testUnsupportedYearMonth() {
        YearMonth ym = YearMonth.of(2021, 5);
        assertThrows(IllegalArgumentException.class, () -> DateConversions.toBigInteger(ym, null), "YearMonth is not supported and should raise IllegalArgumentException");
    }

    /* --------------------------------------------------------------------- *
     *  Custom converter for numeric types – Integer 12345 → Instant 9999
     * --------------------------------------------------------------------- */
    @Test
    @DisplayName("Custom converter – maps Integer 12345 to Instant epoch 9999")
    void testCustomConverterNumeric() {
        Converter custom = mock(Converter.class);
        when(custom.convert(eq(12345), eq(Instant.class))).thenReturn(Instant.ofEpochSecond(9_999L));
        try (MockedStatic<InstantConversions> mocked = mockInstantConversions()) {
            BigInteger result = DateConversions.toBigInteger(12345, custom);
            assertEquals(BigInteger.valueOf(9_999L), result);
            verify(custom, times(1)).convert(eq(12345), eq(Instant.class));
        }
    }

    /* --------------------------------------------------------------------- *
     *  Date out of Instant range – expect DateTimeException
     * --------------------------------------------------------------------- */
    @Test
    @DisplayName("Date out of Instant.MAX range")
    void testDateBeyondInstantMax() {
        Date tooBig = new Date(Long.MAX_VALUE);
        assertThrows(DateTimeException.class, () -> DateConversions.toBigInteger(tooBig, null), "Date beyond Instant.MAX should throw DateTimeException");
    }

    @Test
    @DisplayName("Date out of Instant.MIN range")
    void testDateBeforeInstantMin() {
        Date tooSmall = new Date(Long.MIN_VALUE);
        assertThrows(DateTimeException.class, () -> DateConversions.toBigInteger(tooSmall, null), "Date before Instant.MIN should throw DateTimeException");
    }
}
