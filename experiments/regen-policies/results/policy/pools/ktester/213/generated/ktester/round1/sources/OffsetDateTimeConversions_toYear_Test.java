package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class OffsetDateTimeConversions_toYear_Test {

    @Mock
    private Converter mockConverter;

    @Mock
    private ConverterOptions mockOptions;

    /**
     * Very small {@link Converter} implementation used only for these tests.
     * It stores a {@link ConverterOptions} instance that simply returns the
     * supplied {@link ZoneId}.  This mirrors the behaviour of the real
     * {@code SimpleConverter} used in the original library.
     */
    private static final class SimpleConverter implements Converter {

        private final ConverterOptions options;

        SimpleConverter(ZoneId zoneId) {
            this.options = new SimpleOptions(zoneId);
        }

        @Override
        public ConverterOptions getOptions() {
            return options;
        }
    }

    /**
     * Minimal {@link ConverterOptions} implementation exposing only {@link #getZoneId()}.
     */
    private static final class SimpleOptions implements ConverterOptions {

        private final ZoneId zoneId;

        SimpleOptions(ZoneId zoneId) {
            this.zoneId = zoneId;
        }

        @Override
        public ZoneId getZoneId() {
            return zoneId;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Configure the mock Converter to return a mock ConverterOptions that supplies a known ZoneId.
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("UTC"));
    }

    private static Stream<Arguments> successfulConversionProvider() {
        return Stream.of(// ----- Normal conversions -------------------------------------------------
        Arguments.of("Standard conversion where the OffsetDateTime and the target ZoneId are the same (2022).", OffsetDateTime.parse("2022-06-15T10:15:30+02:00"), // Europe/Paris is UTC+2 in summer → same instant
        new SimpleConverter(ZoneId.of("Europe/Paris")), Year.of(2022)), Arguments.of("Conversion crosses a year boundary after applying UTC (2022-12-31T23:00:00-05:00 → 2023).", OffsetDateTime.parse("2022-12-31T23:00:00-05:00"), new SimpleConverter(ZoneId.of("UTC")), Year.of(2023)), Arguments.of("Leap‑year date stays within the same year after conversion to Asia/Tokyo (2000).", OffsetDateTime.parse("2000-02-29T12:00:00Z"), new SimpleConverter(ZoneId.of("Asia/Tokyo")), Year.of(2000)), // ----- Boundary conditions ------------------------------------------------
        Arguments.of("OffsetDateTime just before midnight in a -02:00 offset; converting to UTC moves to next year (2022 → 2023).", OffsetDateTime.parse("2021-12-31T23:59:59-02:00"), new SimpleConverter(ZoneId.of("UTC")), Year.of(2022)), Arguments.of("Maximum positive offset (+14) pushes the instant back into the previous calendar day (2022 → 2021).", OffsetDateTime.parse("2022-01-01T00:00:00+14:00"), new SimpleConverter(ZoneId.of("UTC")), Year.of(2021)), Arguments.of("Maximum negative offset (-14) advances the instant into the next calendar day (2022 → 2023).", OffsetDateTime.parse("2022-12-31T23:59:59-14:00"), new SimpleConverter(ZoneId.of("UTC")), Year.of(2023)));
    }

    private static Stream<Arguments> exceptionScenarioProvider() {
        return Stream.of(Arguments.of("Null 'from' argument should cause NullPointerException.", null, new SimpleConverter(ZoneId.of("UTC")), NullPointerException.class), Arguments.of("'from' is a String, not an OffsetDateTime, causing ClassCastException.", "2022-01-01", new SimpleConverter(ZoneId.of("UTC")), ClassCastException.class), Arguments.of("Null 'converter' argument leads to NullPointerException when the method calls converter.getOptions().", OffsetDateTime.parse("2022-01-01T00:00:00Z"), null, NullPointerException.class), Arguments.of("Converter with a null Options object causes NullPointerException when getOptions().getZoneId() is accessed.", OffsetDateTime.parse("2022-01-01T00:00:00Z"), new // override to return null options
        SimpleConverter(// override to return null options
        null) {

            @Override
            public ConverterOptions getOptions() {
                return null;
            }
        }, NullPointerException.class), Arguments.of("Converter whose Options return a null ZoneId results in NullPointerException inside atZoneSameInstant.", OffsetDateTime.parse("2022-01-01T00:00:00Z"), // options with null ZoneId
        new SimpleConverter(null), NullPointerException.class));
    }

    @Test
    void toYear_returnsCorrectYear_forOffsetDateTimeInSameZone() {
        // GIVEN
        OffsetDateTime offsetDateTime = OffsetDateTime.of(// 2023‑05‑10
        2023, // 2023‑05‑10
        5, // 2023‑05‑10
        10, // 12:00:00.000
        12, // 12:00:00.000
        0, // 12:00:00.000
        0, // 12:00:00.000
        0, // UTC offset
        ZoneOffset.UTC);
        // WHEN
        Year result = OffsetDateTimeConversions.toYear(offsetDateTime, mockConverter);
        // THEN
        assertEquals(Year.of(2023), result);
    }

    @Test
    void toYear_respectsConverterZoneId_whenDifferentOffset() {
        // GIVEN
        // OffsetDateTime at +02:00, 01:00 local time → 2023‑05‑09 23:00 UTC (date changes)
        OffsetDateTime offsetDateTime = OffsetDateTime.of(2023, 5, 10, 1, 0, 0, 0, // +02:00
        ZoneOffset.ofHours(2));
        // WHEN
        Year result = OffsetDateTimeConversions.toYear(offsetDateTime, mockConverter);
        // THEN
        // After conversion to UTC the date becomes 2023‑05‑09, so the year is still 2023.
        assertEquals(Year.of(2023), result);
    }

    /* --------------------------------------------------------------------- *
     *  Parameterized tests for successful conversions.
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("successfulConversionProvider")
    void toYear_returnsExpectedYear(String description, OffsetDateTime from, Converter converter, Year expected) {
        // WHEN
        Year result = OffsetDateTimeConversions.toYear(from, converter);
        // THEN
        assertEquals(expected, result, description);
    }

    /* --------------------------------------------------------------------- *
     *  Parameterized tests for exception scenarios.
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("exceptionScenarioProvider")
    void toYear_throwsExpectedException(String description, Object from, Converter converter, Class<? extends Throwable> expectedException) {
        // WHEN / THEN
        assertThrows(expectedException, () -> OffsetDateTimeConversions.toYear(from, converter), description);
    }
}
