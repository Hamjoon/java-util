package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;
import java.sql.Date;
import java.time.ZoneId;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.DateTimeException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toSqlDate_Test {

    @Mock
    private Converter mockConverter;

    @Mock
    private ConverterOptions mockOptions;

    private AutoCloseable mocks;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise @Mock fields
        mocks = MockitoAnnotations.openMocks(this);
        // Default behaviour: the converter returns the mocked options
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        mocks = MockitoAnnotations.openMocks(this);
        // By default the converter returns a non‑null options object.
        when(mockConverter.getOptions()).thenReturn(mockOptions);
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Release Mockito resources
        mocks.close();
        mocks.close();
    }

    /**
     * Configures the mock {@link ConverterOptions} to return the supplied zone.
     *
     * @param zoneId the IANA zone id string, or {@code null} to simulate a null ZoneId.
     */
    private void configureZone(String zoneId) {
        if (zoneId == null) {
            when(mockOptions.getZoneId()).thenReturn(null);
        } else {
            when(mockOptions.getZoneId()).thenReturn(ZoneId.of(zoneId));
        }
    }

    private static Stream<Arguments> successfulConversions() {
        return Stream.of(// epoch 0 UTC
        Arguments.of(0L, "UTC", Date.valueOf("1970-01-01"), "Epoch millisecond 0 in UTC zone should produce 1970‑01‑01"), // epoch 0 New York (UTC‑5) → previous calendar day
        Arguments.of(0L, "America/New_York", Date.valueOf("1969-12-31"), "Epoch 0 in America/New_York falls on the previous day"), // 2021‑06‑01 00:00 UTC
        Arguments.of(1622505600000L, "UTC", Date.valueOf("2021-06-01"), "Positive epoch milliseconds for 2021‑06‑01 UTC"), // 2021‑06‑01 09:00 Asia/Tokyo (same local date)
        Arguments.of(1622552400000L, "Asia/Tokyo", Date.valueOf("2021-06-01"), "Epoch that maps to 2021‑06‑01 09:00 in Tokyo still yields 2021‑06‑01"), // BigDecimal input
        Arguments.of(new BigDecimal("1622505600000"), "UTC", Date.valueOf("2021-06-01"), "BigDecimal should be handled via longValue()"), // One day before epoch UTC
        Arguments.of(-86_400_000L, "UTC", Date.valueOf("1969-12-31"), "One day before the epoch in UTC should yield 1969‑12‑31"), // Millisecond just before epoch UTC
        Arguments.of(-1L, "UTC", Date.valueOf("1969-12-31"), "A millisecond just before the epoch still falls on the previous day"), // Leap‑day 2020‑02‑29 UTC
        Arguments.of(1582934400000L, "UTC", Date.valueOf("2020-02-29"), "Leap‑day epoch milliseconds should map to 2020‑02‑29"), // Epoch 0 in UTC‑12 (Etc/GMT+12) → previous calendar day
        Arguments.of(0L, "Etc/GMT+12", Date.valueOf("1969-12-31"), "Epoch 0 in a UTC‑12 zone is the previous day"), // One day after epoch in UTC‑12 still yields 1970‑01‑01
        Arguments.of(86_400_000L, "Etc/GMT+12", Date.valueOf("1970-01-01"), "One day after epoch in UTC‑12 still falls on 1970‑01‑01"), // Integer representation of one day
        Arguments.of(86_400_000, "UTC", Date.valueOf("1970-01-02"), "Integer one‑day value should convert correctly"), // Short zero
        Arguments.of((short) 0, "UTC", Date.valueOf("1970-01-01"), "Short zero behaves like Long zero"), // Byte zero
        Arguments.of((byte) 0, "UTC", Date.valueOf("1970-01-01"), "Byte zero behaves like Long zero"), // BigInteger input
        Arguments.of(new BigInteger("1622505600000"), "UTC", Date.valueOf("2021-06-01"), "BigInteger is a Number; its longValue() yields the correct epoch"), // AtomicLong input
        Arguments.of(new AtomicLong(1622505600000L), "UTC", Date.valueOf("2021-06-01"), "AtomicLong implements Number; conversion should produce the same date as Long"), // AtomicInteger input
        Arguments.of(new AtomicInteger(86_400_000), "UTC", Date.valueOf("1970-01-02"), "AtomicInteger implements Number; its longValue() yields one day after epoch"), // Double input (truncates)
        Arguments.of(new Double(1622505600000.0), "UTC", Date.valueOf("2021-06-01"), "Double is a Number; longValue() truncates correctly"), // Float input (truncates)
        Arguments.of(new Float(86_400_000.0f), "UTC", Date.valueOf("1970-01-02"), "Float is a Number; conversion works for values that fit in a long"));
    }

    private static Stream<Arguments> invalidInputs() {
        return Stream.of(// Non‑Number (String)
        Arguments.of("123", "UTC", ClassCastException.class, "Passing a non‑Number (String) should cause ClassCastException"), // Null source object
        Arguments.of(null, "UTC", NullPointerException.class, "Null source object leads to NullPointerException"), // Null converter
        Arguments.of(0L, "UTC", NullPointerException.class, "Null Converter results in NullPointerException"), // Converter with null options
        Arguments.of(0L, "UTC", NullPointerException.class, "Converter.getOptions() returns null → NPE"), // Options with null ZoneId
        Arguments.of(0L, null, NullPointerException.class, "Options.getZoneId() returns null → NPE"));
    }

    private static Stream<Arguments> edgeCaseErrors() {
        return Stream.of(// BigDecimal overflow positive
        Arguments.of(new BigDecimal("9223372036854775808"), "UTC", ArithmeticException.class, "BigDecimal > Long.MAX_VALUE should cause ArithmeticException"), // BigDecimal overflow negative
        Arguments.of(new BigDecimal("-9223372036854775809"), "UTC", ArithmeticException.class, "BigDecimal < Long.MIN_VALUE should cause ArithmeticException"), // Options object exists but its getZoneId() returns null
        Arguments.of(0L, null, NullPointerException.class, "Options exist but getZoneId() returns null → NPE"));
    }

    private static Stream<Arguments> instantOutOfRange() {
        return Stream.of(Arguments.of(Long.MAX_VALUE, "UTC", DateTimeException.class, "Instant.ofEpochMilli(Long.MAX_VALUE) exceeds supported range"), Arguments.of(Long.MIN_VALUE, "UTC", DateTimeException.class, "Instant.ofEpochMilli(Long.MIN_VALUE) is below supported range"));
    }

    @Test
    void toSqlDate_EpochZero_Utc() {
        // GIVEN
        // 1970-01-01T00:00:00Z
        Long epochMilli = 0L;
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("UTC"));
        // WHEN
        Date sqlDate = NumberConversions.toSqlDate(epochMilli, mockConverter);
        // THEN
        assertEquals(Date.valueOf("1970-01-01"), sqlDate, "Epoch 0 in UTC should produce 1970-01-01");
    }

    @Test
    void toSqlDate_OneDayLater_Utc() {
        // GIVEN
        // 1970-01-02T00:00:00Z (one day later)
        Long epochMilli = 86_400_000L;
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("UTC"));
        // WHEN
        Date sqlDate = NumberConversions.toSqlDate(epochMilli, mockConverter);
        // THEN
        assertEquals(Date.valueOf("1970-01-02"), sqlDate, "Epoch of one day in UTC should produce 1970-01-02");
    }

    @Test
    void toSqlDate_EpochZero_WithDifferentZone() {
        // GIVEN
        // 1970-01-01T00:00:00Z
        Long epochMilli = 0L;
        // Use a zone that is ahead of UTC, e.g., Asia/Tokyo (+09:00)
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("Asia/Tokyo"));
        // WHEN
        Date sqlDate = NumberConversions.toSqlDate(epochMilli, mockConverter);
        // THEN
        // 1970-01-01T00:00:00Z is 1970-01-01T09:00:00+09:00, still the same calendar date
        assertEquals(Date.valueOf("1970-01-01"), sqlDate, "Epoch 0 with Asia/Tokyo zone should still be 1970-01-01");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("successfulConversions")
    void toSqlDate_successful(Object from, String zoneId, Date expected, String description) {
        // GIVEN
        configureZone(zoneId);
        // WHEN
        Date result = NumberConversions.toSqlDate(from, mockConverter);
        // THEN
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("invalidInputs")
    void toSqlDate_invalidInputs(Object from, String zoneId, Class<? extends Throwable> expectedException, String description) {
        // GIVEN
        if (expectedException == NullPointerException.class && from == null) {
            // from is null → no need to configure zone
        } else if (expectedException == NullPointerException.class && zoneId != null) {
            // Normal case: configure zone
            configureZone(zoneId);
        } else if (expectedException == NullPointerException.class && zoneId == null) {
            // Options return null ZoneId
            when(mockConverter.getOptions()).thenReturn(mockOptions);
            when(mockOptions.getZoneId()).thenReturn(null);
        }
        // Special handling for the "null converter" case
        Converter converterToUse = mockConverter;
        if (description.contains("Null Converter")) {
            converterToUse = null;
        } else if (description.contains("null options")) {
            when(mockConverter.getOptions()).thenReturn(null);
        }
        // WHEN / THEN
        assertThrows(expectedException, () -> NumberConversions.toSqlDate(from, converterToUse), description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("edgeCaseErrors")
    void toSqlDate_edgeCases(Object from, String zoneId, Class<? extends Throwable> expectedException, String description) {
        // GIVEN
        if (zoneId != null) {
            configureZone(zoneId);
        } else {
            // Simulate Options returning null ZoneId
            when(mockConverter.getOptions()).thenReturn(mockOptions);
            when(mockOptions.getZoneId()).thenReturn(null);
        }
        // WHEN / THEN
        assertThrows(expectedException, () -> NumberConversions.toSqlDate(from, mockConverter), description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("instantOutOfRange")
    void toSqlDate_instantOutOfRange(Long from, String zoneId, Class<? extends Throwable> expectedException, String description) {
        // GIVEN
        configureZone(zoneId);
        // WHEN / THEN
        assertThrows(expectedException, () -> NumberConversions.toSqlDate(from, mockConverter), description);
    }
}
