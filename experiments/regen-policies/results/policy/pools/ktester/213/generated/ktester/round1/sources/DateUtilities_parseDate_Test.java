package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Date;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.InvocationTargetException;
import java.time.*;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DateUtilities_parseDate_Test {

    @BeforeAll
    static void setupBeforeAll() {
        // Ensure the abbreviation map contains the entries used by the tests.
        // The map is mutable, so we can safely add entries here.
        Map<String, String> abbrev = DateUtilities.ABBREVIATION_TO_TIMEZONE;
        // The library may already contain these, but adding them makes the test
        // robust and independent of the implementation details.
        abbrev.putIfAbsent("PST", "America/Los_Angeles");
        abbrev.putIfAbsent("EST", "America/New_York");
    }

    static Stream<String> emptyOrNullProvider() {
        return Stream.of(null, "", "   ");
    }

    static Stream<Arguments> validIso8601Provider() {
        return Stream.of(// Full UTC timestamp
        Arguments.of("2023-01-15T10:20:30Z", "2023-01-15T10:20:30.000Z"), // Positive offset
        Arguments.of("2023-01-15T10:20:30+02:00", "2023-01-15T08:20:30.000Z"), // Date only – midnight in system default zone (converted to UTC)
        Arguments.of("2023-01-15", ZonedDateTime.of(LocalDate.of(2023, 1, 15), LocalTime.MIDNIGHT, ZoneId.systemDefault()).toInstant().toString()), // Fractional seconds (millis)
        Arguments.of("2023-01-15T10:20:30.123Z", "2023-01-15T10:20:30.123Z"), // Negative offset
        Arguments.of("2023-01-15T10:20:30-05:00", "2023-01-15T15:20:30.000Z"), // Offset without colon
        Arguments.of("2023-01-15T10:20:30+0200", "2023-01-15T08:20:30.000Z"));
    }

    static Stream<Arguments> invalidDateProvider() {
        return Stream.of(Arguments.of("not-a-date", IllegalArgumentException.class), Arguments.of("2023-02-30T10:00:00Z", DateTimeException.class), // No zone information while ensureDateTimeAlone=true (internal overload throws)
        Arguments.of("2023-01-15T10:20:30", IllegalArgumentException.class));
    }

    static Stream<Arguments> tzAbbreviationProvider() {
        return Stream.of(// PST abbreviation (mapped to America/Los_Angeles, which is -08:00 in winter)
        Arguments.of("2023-01-15T10:20:30 PST", "2023-01-15T18:20:30.000Z"), // Full zone ID
        Arguments.of("2023-01-15T10:20:30[America/New_York]", "2023-01-15T15:20:30.000Z"));
    }

    static Stream<Arguments> whitespaceProvider() {
        return Stream.of(Arguments.of("   2023-01-15T10:20:30Z   ", "2023-01-15T10:20:30.000Z"));
    }

    static Stream<Arguments> edgeCaseProvider() {
        return Stream.of(Arguments.of("1970-01-01T00:00:00Z", "1970-01-01T00:00:00.000Z"), Arguments.of("9999-12-31T23:59:59Z", "9999-12-31T23:59:59.000Z"));
    }

    @Test
    void parseDate_nullOrEmpty_returnsNull() {
        assertNull(DateUtilities.parseDate(null), "null input should return null");
        assertNull(DateUtilities.parseDate(""), "empty string should return null");
        assertNull(DateUtilities.parseDate("   "), "blank string should return null");
    }

    @Test
    void parseDate_iso8601WithZ_returnsCorrectInstant() {
        // 2023‑08‑30T22:00:00Z  ==  2023‑08‑30 22:00 UTC
        Date date = DateUtilities.parseDate("2023-08-30T22:00Z");
        assertNotNull(date);
        Instant instant = date.toInstant();
        assertThat(instant).isEqualTo(Instant.parse("2023-08-30T22:00:00Z"));
    }

    @Test
    void parseDate_iso8601WithPositiveOffset_equivalentToUtc() {
        // Same instant expressed with +02:00 offset
        Date utcDate = DateUtilities.parseDate("2023-08-30T22:00Z");
        // 22:00Z == 24:00+02:00
        Date offsetDate = DateUtilities.parseDate("2023-08-30T24:00+02:00");
        assertNotNull(utcDate);
        assertNotNull(offsetDate);
        assertEquals(utcDate, offsetDate, "Dates with equivalent instants must be equal");
    }

    @Test
    void parseDate_iso8601WithNegativeOffset_equivalentToUtc() {
        // Same instant expressed with -05:00 offset
        Date utcDate = DateUtilities.parseDate("2023-08-30T22:00Z");
        // 22:00Z == 17:00-05:00
        Date offsetDate = DateUtilities.parseDate("2023-08-30T17:00-05:00");
        assertNotNull(utcDate);
        assertNotNull(offsetDate);
        assertEquals(utcDate, offsetDate);
    }

    @Test
    void parseDate_fractionalSeconds_areHandledCorrectly() {
        Date date = DateUtilities.parseDate("2023-08-30T22:00:00.123456Z");
        assertNotNull(date);
        Instant instant = date.toInstant();
        // The parser should keep the fractional part up to nanosecond precision.
        assertThat(instant).isEqualTo(Instant.parse("2023-08-30T22:00:00.123456Z"));
    }

    @Test
    void parseDate_unixEpochMillisString_isInterpretedAsInstant() {
        // 1640995200000 ms = 2022‑01‑01T00:00:00Z
        Date date = DateUtilities.parseDate("1640995200000");
        assertNotNull(date);
        Instant instant = date.toInstant();
        assertThat(instant).isEqualTo(Instant.parse("2022-01-01T00:00:00Z"));
    }

    @Test
    void parseDate_explicitZoneId_keepsZoneWhenUsingOverload() {
        // The overload that returns ZonedDateTime is used to verify zone handling.
        String input = "2024-02-05T22:31:17.409[Asia/Tokyo]";
        ZonedDateTime zdt = DateUtilities.parseDate(input, ZoneId.of("UTC"), true);
        assertNotNull(zdt);
        assertThat(zdt.getZone()).isEqualTo(ZoneId.of("Asia/Tokyo"));
        assertThat(zdt.getHour()).isEqualTo(22);
        assertThat(zdt.getMinute()).isEqualTo(31);
        assertThat(zdt.getSecond()).isEqualTo(17);
        assertThat(zdt.getOffset()).isEqualTo(ZoneOffset.ofHours(9));
        // Verify the instant matches the expected UTC instant.
        Instant expected = Instant.parse("2024-02-05T13:31:17.409Z");
        assertThat(zdt.toInstant()).isEqualTo(expected);
    }

    @Test
    void privateConstructor_isNotInstantiable() throws Exception {
        Constructor<DateUtilities> ctor = DateUtilities.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(ctor.getModifiers()), "Constructor should be private");
        ctor.setAccessible(true);
        assertThrows(InvocationTargetException.class, ctor::newInstance, "Instantiating the utility class must throw an exception");
    }

    /**
     * 1. Empty or null inputs – already covered, but we keep a parameterized version for completeness **
     */
    @ParameterizedTest(name = "[{index}] emptyOrNull: \"{0}\" → null")
    @MethodSource("emptyOrNullProvider")
    void parseDate_emptyOrNull_inputs_returnNull(String input) {
        assertNull(DateUtilities.parseDate(input));
    }

    /**
     * 2. Valid ISO‑8601 date‑time strings **
     */
    @ParameterizedTest(name = "[{index}] iso8601: {0} → {1}")
    @MethodSource("validIso8601Provider")
    void parseDate_validIso8601_returnsExpectedInstant(String input, String expectedIso) {
        Date date = DateUtilities.parseDate(input);
        assertNotNull(date, "Parsed date should not be null");
        Instant actual = date.toInstant();
        Instant expected = Instant.parse(expectedIso);
        assertThat(actual).isEqualTo(expected);
    }

    /**
     * 3. Invalid date strings – expect exceptions **
     */
    @ParameterizedTest(name = "[{index}] invalid: {0} → {1}")
    @MethodSource("invalidDateProvider")
    void parseDate_invalidInputs_throwException(String input, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> DateUtilities.parseDate(input));
    }

    /**
     * 4. Timezone abbreviations and zone IDs **
     */
    @ParameterizedTest(name = "[{index}] tzAbbrevOrId: {0} → {1}")
    @MethodSource("tzAbbreviationProvider")
    void parseDate_timezoneAbbreviationOrId(String input, String expectedIso) {
        Date date = DateUtilities.parseDate(input);
        assertNotNull(date);
        Instant actual = date.toInstant();
        Instant expected = Instant.parse(expectedIso);
        assertThat(actual).isEqualTo(expected);
    }

    /**
     * 5. Whitespace handling **
     */
    @ParameterizedTest(name = "[{index}] whitespace: \"{0}\" → {1}")
    @MethodSource("whitespaceProvider")
    void parseDate_whitespacePaddedInput(String input, String expectedIso) {
        Date date = DateUtilities.parseDate(input);
        assertNotNull(date);
        Instant actual = date.toInstant();
        Instant expected = Instant.parse(expectedIso);
        assertThat(actual).isEqualTo(expected);
    }

    /**
     * 6. Edge‑case dates – epoch and far‑future **
     */
    @ParameterizedTest(name = "[{index}] edgeCase: {0} → {1}")
    @MethodSource("edgeCaseProvider")
    void parseDate_edgeCaseDates(String input, String expectedIso) {
        Date date = DateUtilities.parseDate(input);
        assertNotNull(date);
        Instant actual = date.toInstant();
        Instant expected = Instant.parse(expectedIso);
        assertThat(actual).isEqualTo(expected);
    }
}
