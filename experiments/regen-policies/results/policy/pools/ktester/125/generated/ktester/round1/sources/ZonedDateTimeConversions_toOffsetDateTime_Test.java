package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class ZonedDateTimeConversions_toOffsetDateTime_Test {

    @Mock
    private Converter // not used by the method under test
    mockConverter;

    /* --------------------------------------------------------------
     *  Helper class to provide a non‑null mock Converter without using
     *  Mockito (used only for the edge‑case test where a mock is passed
     *  explicitly).  The implementation is irrelevant because the method
     *  under test never calls any Converter method.
     * -------------------------------------------------------------- */
    private static class MockedConverter implements Converter {
        // No‑op implementation – all methods can remain default / empty
    }

    private static Stream<Arguments> successfulConversionProvider() {
        return Stream.of(// positive offset (+02:00)
        Arguments.of(ZonedDateTime.parse("2023-03-15T12:30:45+02:00[Europe/Paris]"), "2023-03-15T12:30:45+02:00", "Positive offset (+02:00) – zone ID is dropped"), // negative offset (‑05:00)
        Arguments.of(ZonedDateTime.parse("2022-12-01T08:15:00-05:00[America/New_York]"), "2022-12-01T08:15:00-05:00", "Negative offset (‑05:00) – instant preserved"), // UTC (zero offset)
        Arguments.of(ZonedDateTime.parse("2021-07-20T00:00:00Z[UTC]"), "2021-07-20T00:00:00Z", "UTC zone – zero offset"), // nanosecond precision (+09:00)
        Arguments.of(ZonedDateTime.parse("2024-02-29T23:59:59.123456789+09:00[Asia/Tokyo]"), "2024-02-29T23:59:59.123456789+09:00", "Maximum nanosecond precision retained"));
    }

    private static Stream<Arguments> edgeCaseProvider() {
        return Stream.of(// null Converter (method should still succeed)
        Arguments.of(ZonedDateTime.parse("2023-03-15T12:30:45+02:00[Europe/Paris]"), "2023-03-15T12:30:45+02:00", null, "Null converter supplied – conversion must still succeed"), // mock Converter (explicitly supplied)
        Arguments.of(ZonedDateTime.parse("2023-03-15T12:30:45+02:00[Europe/Paris]"), "2023-03-15T12:30:45+02:00", new MockedConverter(), "Non‑null mock converter – should be ignored"), // epoch instant (UTC)
        Arguments.of(ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.of("UTC")), "1970-01-01T00:00:00Z", null, "Epoch instant conversion (UTC)"), // leap‑year with max nanoseconds
        Arguments.of(ZonedDateTime.of(2000, 2, 29, 23, 59, 59, 999_999_999, ZoneId.of("UTC")), "2000-02-29T23:59:59.999999999Z", null, "Leap‑year date with maximum nanosecond fraction"), // DST forward transition (Europe/Paris 2023‑03‑26 02:30 becomes +02:00)
        Arguments.of(ZonedDateTime.of(2023, 3, 26, 2, 30, 0, 0, ZoneId.of("Europe/Paris")), "2023-03-26T02:30:00+02:00", null, "DST forward transition – post‑transition offset (+02:00)"), // DST backward transition (Europe/Paris 2023‑10‑29 02:30 resolves to +01:00)
        Arguments.of(ZonedDateTime.of(2023, 10, 29, 2, 30, 0, 0, ZoneId.of("Europe/Paris")), "2023-10-29T02:30:00+01:00", null, "DST backward transition – earlier offset (+01:00)"), // Maximum positive UTC offset (+14:00)
        Arguments.of(ZonedDateTime.of(2023, 12, 31, 23, 59, 59, 0, ZoneId.of("Pacific/Kiritimati")), "2023-12-31T23:59:59+14:00", null, "Extreme positive offset (+14:00)"), // Maximum negative UTC offset (-12:00) using Etc/GMT+12 (note sign reversal)
        Arguments.of(ZonedDateTime.of(2023, 1, 1, 0, 0, 0, 0, ZoneId.of("Etc/GMT+12")), "2023-01-01T00:00:00-12:00", null, "Extreme negative offset (-12:00) via Etc/GMT+12"), // Parsing via ISO_ZONED_DATE_TIME formatter
        Arguments.of(ZonedDateTime.parse("2023-03-15T12:30:45+02:00[Europe/Paris]", DateTimeFormatter.ISO_ZONED_DATE_TIME), "2023-03-15T12:30:45+02:00", null, "Parsing using ISO_ZONED_DATE_TIME formatter"));
    }

    private static Stream<Arguments> typeMismatchProvider() {
        return Stream.of(Arguments.of("2023-03-15T12:30:45+02:00[Europe/Paris]", "String instead of ZonedDateTime"), Arguments.of(LocalDateTime.now(), "LocalDateTime (no zone information)"), Arguments.of(Instant.now(), "Instant (cannot be cast to ZonedDateTime)"), Arguments.of(new java.util.Date(), "java.util.Date instance"), Arguments.of(OffsetDateTime.now(), "OffsetDateTime (related but not a ZonedDateTime)"));
    }

    @Test
    void toOffsetDateTime_WithValidZonedDateTime_ReturnsCorrectOffset() {
        // given
        ZonedDateTime zdt = ZonedDateTime.of(2023, 5, 10, 12, 30, 45, 0, // UTC‑04:00 (or UTC‑05:00 depending on DST)
        ZoneId.of("America/New_York"));
        OffsetDateTime expected = zdt.toOffsetDateTime();
        // when
        OffsetDateTime actual = ZonedDateTimeConversions.toOffsetDateTime(zdt, mockConverter);
        // then
        assertEquals(expected, actual, "The OffsetDateTime returned should match the one produced by ZonedDateTime#toOffsetDateTime()");
        assertEquals(zdt.getOffset(), actual.getOffset(), "The offset component must be identical to the source ZonedDateTime's offset");
    }

    @Test
    void toOffsetDateTime_WithInvalidType_ThrowsClassCastException() {
        // given
        Object notAZonedDateTime = "I am not a ZonedDateTime";
        // when / then
        assertThrows(ClassCastException.class, () -> ZonedDateTimeConversions.toOffsetDateTime(notAZonedDateTime, mockConverter), "Passing an object that is not a ZonedDateTime should result in a ClassCastException");
    }

    @Test
    void toOffsetDateTime_WithNullSource_ThrowsNullPointerException() {
        // given
        Object nullSource = null;
        // when / then
        assertThrows(NullPointerException.class, () -> ZonedDateTimeConversions.toOffsetDateTime(nullSource, mockConverter), "A null source should cause a NullPointerException when the method attempts to invoke toOffsetDateTime()");
    }

    /* --------------------------------------------------------------
     *  Successful conversion test cases (positive offsets, negative offsets,
     *  UTC, nanosecond precision, etc.).
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("successfulConversionProvider")
    void toOffsetDateTime_SuccessfulConversions(ZonedDateTime source, String expectedIso, String description) {
        // given – source ZonedDateTime already created by the provider
        OffsetDateTime expected = OffsetDateTime.parse(expectedIso);
        // when
        OffsetDateTime actual = ZonedDateTimeConversions.toOffsetDateTime(source, mockConverter);
        // then
        assertEquals(expected, actual, () -> "Conversion failed for case: " + description);
        assertEquals(source.getOffset(), actual.getOffset(), () -> "Offset mismatch for case: " + description);
    }

    /* --------------------------------------------------------------
     *  Edge‑case conversions (null converter, epoch, leap year,
     *  DST transitions, extreme offsets, parsing via formatter, etc.).
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("edgeCaseProvider")
    void toOffsetDateTime_EdgeCases(ZonedDateTime source, String expectedIso, Converter converterToUse, String description) {
        // when
        OffsetDateTime actual = ZonedDateTimeConversions.toOffsetDateTime(source, converterToUse);
        // then
        OffsetDateTime expected = OffsetDateTime.parse(expectedIso);
        assertEquals(expected, actual, () -> "Edge case failed: " + description);
        assertEquals(source.getOffset(), actual.getOffset(), () -> "Offset mismatch in edge case: " + description);
    }

    /* --------------------------------------------------------------
     *  Null source handling – should throw NullPointerException.
     * -------------------------------------------------------------- */
    @Test
    void toOffsetDateTime_NullSource_ThrowsNullPointerException() {
        // given
        Object nullSource = null;
        // when / then
        assertThrows(NullPointerException.class, () -> ZonedDateTimeConversions.toOffsetDateTime(nullSource, mockConverter), "A null source must trigger NullPointerException");
    }

    /* --------------------------------------------------------------
     *  Type‑mismatch cases – expecting ClassCastException.
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("typeMismatchProvider")
    void toOffsetDateTime_TypeMismatchThrowsClassCastException(Object invalidSource, String description) {
        // when / then
        assertThrows(ClassCastException.class, () -> ZonedDateTimeConversions.toOffsetDateTime(invalidSource, mockConverter), () -> "Expected ClassCastException for: " + description);
    }
}
