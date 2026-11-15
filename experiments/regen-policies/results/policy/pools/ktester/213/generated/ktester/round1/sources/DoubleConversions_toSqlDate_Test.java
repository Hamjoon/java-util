package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Date;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DoubleConversions_toSqlDate_Test {

    @Mock
    private Converter mockConverter;

    @Mock
    private ConverterOptions mockOptions;

    private AutoCloseable mocks;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise Mockito annotations for this test instance.
        mocks = MockitoAnnotations.openMocks(this);
        // Configure the mock chain: converter -> options -> zoneId.
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("UTC"));
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Release Mockito resources.
        mocks.close();
    }

    static Stream<Arguments> normalConversionProvider() {
        return Stream.of(// 1622505600.0 → 2021‑06‑01 UTC
        Arguments.of(1622505600.0, ZoneId.of("UTC"), "2021-06-01", "Converts a positive whole‑second epoch value to a java.sql.Date using UTC zone."), // 1622505600.75 in New York (UTC‑4) → 2021‑05‑31
        Arguments.of(1622505600.75, ZoneId.of("America/New_York"), "2021-05-31", "Fractional seconds are truncated; instant falls on 2021‑05‑31 20:00 in New York."), // -86400.0 UTC → 1969‑12‑31
        Arguments.of(-86400.0, ZoneId.of("UTC"), "1969-12-31", "Negative epoch seconds represent dates before 1970‑01‑01."));
    }

    static Stream<Arguments> invalidInputProvider() {
        return Stream.of(Arguments.of("not a double", ClassCastException.class, "Passing a String should cause a ClassCastException."), Arguments.of(null, NullPointerException.class, "Passing null should cause a NullPointerException when unboxing."));
    }

    static Stream<Arguments> fractionalTruncationProvider() {
        return Stream.of(Arguments.of(0.75, "1970-01-01", "Positive fractional seconds are truncated toward zero; 0.75 → epoch second 0."), Arguments.of(-0.5, "1970-01-01", "Negative fractional seconds are also truncated toward zero; -0.5 → 0."), Arguments.of(-86400.9, "1969-12-31", "Fractional part after a negative whole‑second value is ignored; -86400.9 → -86400."));
    }

    static Stream<Arguments> timeZoneBoundaryProvider() {
        return Stream.of(Arguments.of(0.0, ZoneId.of("Pacific/Kiritimati"), "1970-01-01", "UTC epoch 0 at +14 offset still falls on 1970‑01‑01."), Arguments.of(0.0, ZoneId.of("Etc/GMT+12"), "1969-12-31", "UTC epoch 0 at -12 offset moves the local date back one day."), Arguments.of(86400.0, ZoneId.of("Pacific/Apia"), "1970-01-02", "One day after epoch in a +13 zone still lands on 1970‑01‑02."));
    }

    static Stream<Arguments> zeroEpochZoneProvider() {
        return Stream.of(Arguments.of(ZoneId.of("UTC"), "1970-01-01", "Baseline test confirming epoch 0 maps to 1970‑01‑01 in UTC."), Arguments.of(ZoneId.of("Europe/London"), "1970-01-01", "London has no offset at the epoch moment, so the date remains 1970‑01‑01."));
    }

    @Test
    void toSqlDate_zeroEpochSeconds_returns1970_01_01() {
        // given
        // 1970-01-01T00:00:00Z
        Double epochSeconds = 0.0;
        // when
        Date result = DoubleConversions.toSqlDate(epochSeconds, mockConverter);
        // then
        assertEquals(Date.valueOf("1970-01-01"), result);
    }

    @Test
    void toSqlDate_positiveEpochSeconds_returnsCorrespondingDate() {
        // given
        // 2021-01-01T00:00:00Z = 1609459200 seconds since epoch
        Double epochSeconds = 1609459200.0;
        // when
        Date result = DoubleConversions.toSqlDate(epochSeconds, mockConverter);
        // then
        assertEquals(Date.valueOf("2021-01-01"), result);
    }

    @Test
    void toSqlDate_fractionalSeconds_areTruncated() {
        // given
        // 2021-01-01T00:00:00Z + 0.999 seconds → still 2021-01-01 after truncation
        Double epochSeconds = 1609459200.999;
        // when
        Date result = DoubleConversions.toSqlDate(epochSeconds, mockConverter);
        // then
        assertEquals(Date.valueOf("2021-01-01"), result);
    }

    @Test
    void toSqlDate_nonDoubleInput_throwsClassCastException() {
        // given
        Object notADouble = "12345";
        // when / then
        assertThrows(ClassCastException.class, () -> DoubleConversions.toSqlDate(notADouble, mockConverter));
    }

    /**
     * Normal conversions and fractional handling
     */
    @ParameterizedTest(name = "{index} ⇒ {2}")
    @MethodSource("normalConversionProvider")
    void toSqlDate_normalConversions(Object from, ZoneId zone, String expectedDateString, String description) {
        // arrange
        when(mockOptions.getZoneId()).thenReturn(zone);
        // act
        Date result = DoubleConversions.toSqlDate(from, mockConverter);
        // assert
        assertEquals(Date.valueOf(expectedDateString), result, description);
    }

    /**
     * Edge cases – large values
     */
    @Test
    void toSqlDate_maxSupportedDate_returns9999_12_31() {
        // 9999‑12‑31 23:59:59 UTC
        Double epochSeconds = 253402300799.0;
        Date result = DoubleConversions.toSqlDate(epochSeconds, mockConverter);
        assertEquals(Date.valueOf("9999-12-31"), result);
    }

    /**
     * Error handling – invalid input type
     */
    @ParameterizedTest(name = "{index} ⇒ {2}")
    @MethodSource("invalidInputProvider")
    void toSqlDate_invalidInputThrows(Object from, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> DoubleConversions.toSqlDate(from, mockConverter), description);
    }

    /**
     * Error handling – null or malformed converter
     */
    @Test
    void toSqlDate_nullConverter_throwsNullPointerException() {
        Double epochSeconds = 0.0;
        assertThrows(NullPointerException.class, () -> DoubleConversions.toSqlDate(epochSeconds, null));
    }

    @Test
    void toSqlDate_converterWithNullOptions_throwsNullPointerException() {
        Double epochSeconds = 0.0;
        when(mockConverter.getOptions()).thenReturn(null);
        assertThrows(NullPointerException.class, () -> DoubleConversions.toSqlDate(epochSeconds, mockConverter));
    }

    @Test
    void toSqlDate_optionsWithNullZoneId_throwsNullPointerException() {
        Double epochSeconds = 0.0;
        when(mockOptions.getZoneId()).thenReturn(null);
        assertThrows(NullPointerException.class, () -> DoubleConversions.toSqlDate(epochSeconds, mockConverter));
    }

    /**
     * Fractional and negative truncation
     */
    @ParameterizedTest(name = "{index} ⇒ {2}")
    @MethodSource("fractionalTruncationProvider")
    void toSqlDate_fractionalTruncation(Object from, String expectedDate, String description) {
        Date result = DoubleConversions.toSqlDate(from, mockConverter);
        assertEquals(Date.valueOf(expectedDate), result, description);
    }

    /**
     * Time‑zone boundary effects
     */
    @ParameterizedTest(name = "{index} ⇒ {2}")
    @MethodSource("timeZoneBoundaryProvider")
    void toSqlDate_timeZoneBoundary(Object from, ZoneId zone, String expectedDate, String description) {
        when(mockOptions.getZoneId()).thenReturn(zone);
        Date result = DoubleConversions.toSqlDate(from, mockConverter);
        assertEquals(Date.valueOf(expectedDate), result, description);
    }

    /**
     * Extreme and special double values
     */
    @Test
    void toSqlDate_nanReturnsEpochDate() {
        Double epochSeconds = Double.NaN;
        Date result = DoubleConversions.toSqlDate(epochSeconds, mockConverter);
        assertEquals(Date.valueOf("1970-01-01"), result);
    }

    @Test
    void toSqlDate_positiveInfinityThrowsDateTimeException() {
        Double epochSeconds = Double.POSITIVE_INFINITY;
        assertThrows(DateTimeException.class, () -> DoubleConversions.toSqlDate(epochSeconds, mockConverter));
    }

    @Test
    void toSqlDate_negativeInfinityThrowsDateTimeException() {
        Double epochSeconds = Double.NEGATIVE_INFINITY;
        assertThrows(DateTimeException.class, () -> DoubleConversions.toSqlDate(epochSeconds, mockConverter));
    }

    @Test
    void toSqlDate_oneSecondBeyondMaxDateThrowsIllegalArgumentException() {
        // 10000‑01‑01 00:00:00 UTC
        Double epochSeconds = 253402300800.0;
        assertThrows(IllegalArgumentException.class, () -> DoubleConversions.toSqlDate(epochSeconds, mockConverter));
    }

    @Test
    void toSqlDate_doubleLargerThanLongMaxThrowsDateTimeException() {
        // > Long.MAX_VALUE
        Double epochSeconds = 9.223372036854776E18;
        assertThrows(DateTimeException.class, () -> DoubleConversions.toSqlDate(epochSeconds, mockConverter));
    }

    /**
     * Zero epoch with various zones (baseline)
     */
    @ParameterizedTest(name = "{index} ⇒ {2}")
    @MethodSource("zeroEpochZoneProvider")
    void toSqlDate_zeroEpochVariousZones(ZoneId zone, String expectedDate, String description) {
        when(mockOptions.getZoneId()).thenReturn(zone);
        Date result = DoubleConversions.toSqlDate(0.0, mockConverter);
        assertEquals(Date.valueOf(expectedDate), result, description);
    }

    /**
     * Additional exception scenarios
     */
    @Test
    void toSqlDate_objectNotDoubleThrowsClassCastException() {
        Object from = new Object();
        assertThrows(ClassCastException.class, () -> DoubleConversions.toSqlDate(from, mockConverter));
    }

    @Test
    void toSqlDate_negativeZeroReturnsEpochDate() {
        // Double.NEGATIVE_ZERO
        Double epochSeconds = -0.0;
        Date result = DoubleConversions.toSqlDate(epochSeconds, mockConverter);
        assertEquals(Date.valueOf("1970-01-01"), result);
    }

    @Test
    void toSqlDate_doubleMaxValueThrowsDateTimeException() {
        Double epochSeconds = Double.MAX_VALUE;
        assertThrows(DateTimeException.class, () -> DoubleConversions.toSqlDate(epochSeconds, mockConverter));
    }

    @Test
    void toSqlDate_negativeOneWithNullConverterThrowsNullPointerException() {
        Double epochSeconds = -1.0;
        assertThrows(NullPointerException.class, () -> DoubleConversions.toSqlDate(epochSeconds, null));
    }

    @Test
    void toSqlDate_optionsReturningNullZoneIdThrowsNullPointerException() {
        Double epochSeconds = 0.0;
        when(mockOptions.getZoneId()).thenReturn(null);
        assertThrows(NullPointerException.class, () -> DoubleConversions.toSqlDate(epochSeconds, mockConverter));
    }
}
