package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.text.*;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SafeSimpleDateFormat_parse_Test {

    /**
     * The format used for all tests in this class.
     */
    private static final String TEST_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * Instance under test.
     */
    private SafeSimpleDateFormat safeSdf;

    /**
     * A reference SimpleDateFormat used to build expected results.
     */
    private SimpleDateFormat referenceSdf;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh SafeSimpleDateFormat for each test to avoid cross‑test contamination.
        safeSdf = new SafeSimpleDateFormat(TEST_FORMAT);
        referenceSdf = new SimpleDateFormat(TEST_FORMAT);
        // Ensure both formatters use the same calendar configuration (the constructor of
        // SafeSimpleDateFormat already does this, but we mirror it for the reference formatter).
        Calendar cal = Calendar.getInstance();
        cal.clear();
        referenceSdf.setCalendar(cal);
        referenceSdf.setLenient(cal.isLenient());
        referenceSdf.setTimeZone(cal.getTimeZone());
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help the garbage collector; not strictly necessary.
        safeSdf = null;
        referenceSdf = null;
    }

    private static Stream<Arguments> validParsingProvider() {
        return Stream.of(Arguments.of("Exact match yyyy‑MM‑dd", "yyyy-MM-dd", "2023-08-15", "2023-08-15T00:00:00Z"), Arguments.of("Pattern dd/MM/yyyy HH:mm", "dd/MM/yyyy HH:mm", "15/08/2023 14:30", "2023-08-15T14:30:00Z"));
    }

    private static Stream<Arguments> invalidParsingProvider() {
        return Stream.of(Arguments.of("Slash separator instead of dash", "yyyy-MM-dd", "2023/08/15", 0), Arguments.of("Month out of range (13)", "yyyy-MM-dd", "2023-13-01", 5));
    }

    /**
     * Verifies that {@code parse} correctly parses a valid date string and
     * updates the {@link ParsePosition} to the index after the parsed text.
     */
    @Test
    void parse_ValidString_UpdatesParsePositionAndReturnsDate() {
        // given
        String input = "2023-08-15 13:45:30 extra text";
        ParsePosition pos = new ParsePosition(0);
        // Expected date using a plain SimpleDateFormat (which we trust).
        Date expected = referenceSdf.parse("2023-08-15 13:45:30", new ParsePosition(0));
        // when
        Date actual = safeSdf.parse(input, pos);
        // then
        assertNotNull(actual, "Parsed date should not be null for a valid input");
        assertEquals(expected, actual, "Parsed date should match the expected value");
        assertEquals(19, pos.getIndex(), "ParsePosition index should point just after the parsed substring");
        assertTrue(pos.getErrorIndex() == -1, "Error index should remain -1 for successful parse");
    }

    /**
     * Verifies that {@code parse} returns {@code null} and sets the error index
     * when the input cannot be parsed.
     */
    @Test
    void parse_InvalidString_ReturnsNullAndSetsErrorIndex() {
        // given
        String input = "not-a-date";
        ParsePosition pos = new ParsePosition(0);
        // when
        Date result = safeSdf.parse(input, pos);
        // then
        assertNull(result, "Result should be null for an unparsable string");
        assertEquals(-1, pos.getIndex(), "ParsePosition index should remain unchanged on failure");
        assertEquals(0, pos.getErrorIndex(), "Error index should be set to the index where parsing failed");
    }

    /**
     * Verifies that parsing starting from a non‑zero offset works correctly.
     */
    @Test
    void parse_WithNonZeroStartPosition_ParsesCorrectly() {
        // given
        String prefix = "IGNORE ";
        String datePart = "2022-01-01 00:00:00";
        String input = prefix + datePart + " END";
        // start after the ignored prefix
        ParsePosition pos = new ParsePosition(prefix.length());
        Date expected = referenceSdf.parse(datePart, new ParsePosition(0));
        // when
        Date actual = safeSdf.parse(input, pos);
        // then
        assertNotNull(actual, "Parsed date should not be null when starting at a valid offset");
        assertEquals(expected, actual, "Parsed date should match the expected value");
        assertEquals(prefix.length() + datePart.length(), pos.getIndex(), "ParsePosition should advance to the end of the parsed date substring");
    }

    /**
     * Ensures that the method is thread‑safe by invoking it from multiple threads
     * concurrently. The test does not assert internal state; it merely checks that
     * no exceptions are thrown and that each thread obtains the correct result.
     */
    @Test
    void parse_ConcurrentAccess_NoExceptionsAndConsistentResults() throws Exception {
        // given
        String input = "2025-12-31 23:59:59";
        Date expected = referenceSdf.parse(input, new ParsePosition(0));
        Runnable task = () -> {
            ParsePosition pos = new ParsePosition(0);
            Date actual = safeSdf.parse(input, pos);
            assertNotNull(actual);
            assertEquals(expected, actual);
            assertEquals(input.length(), pos.getIndex());
        };
        // when
        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(task);
            threads[i].start();
        }
        for (Thread t : threads) {
            t.join();
        }
        // then
        // If any thread threw an AssertionError, the test would fail automatically.
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validParsingProvider")
    void parse_ValidParsing_MatchingFormat(String description, String pattern, String source, String expectedIso) throws Exception {
        // Arrange
        safeSdf = new SafeSimpleDateFormat(pattern);
        referenceSdf = new SimpleDateFormat(pattern);
        // Align calendars / time‑zone
        Calendar cal = Calendar.getInstance();
        cal.clear();
        referenceSdf.setCalendar(cal);
        referenceSdf.setLenient(true);
        referenceSdf.setTimeZone(cal.getTimeZone());
        ParsePosition pos = new ParsePosition(0);
        Date expected = referenceSdf.parse(source, new ParsePosition(0));
        // Act
        Date actual = safeSdf.parse(source, pos);
        // Assert
        assertNotNull(actual, description);
        assertEquals(expected, actual, description);
        assertEquals(source.length(), pos.getIndex(), description);
        assertEquals(-1, pos.getErrorIndex(), description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidParsingProvider")
    void parse_InvalidParsing_MismatchedFormat(String description, String pattern, String source, int expectedErrorIndex) {
        safeSdf = new SafeSimpleDateFormat(pattern);
        ParsePosition pos = new ParsePosition(0);
        Date result = safeSdf.parse(source, pos);
        assertNull(result, description);
        assertEquals(-1, pos.getIndex(), description);
        assertEquals(expectedErrorIndex, pos.getErrorIndex(), description);
    }

    @Test
    void parse_NullSource_ThrowsNullPointerException() {
        ParsePosition pos = new ParsePosition(0);
        assertThrows(NullPointerException.class, () -> safeSdf.parse(null, pos), "Parsing a null source must throw NPE");
    }

    @Test
    void parse_NullParsePosition_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> safeSdf.parse("2023-08-15", null), "Parsing with a null ParsePosition must throw NPE");
    }

    @Test
    void parse_NonZeroStartIndex_ParsesCorrectly() throws Exception {
        String pattern = "yyyy-MM-dd";
        safeSdf = new SafeSimpleDateFormat(pattern);
        referenceSdf = new SimpleDateFormat(pattern);
        Calendar cal = Calendar.getInstance();
        cal.clear();
        referenceSdf.setCalendar(cal);
        referenceSdf.setLenient(true);
        referenceSdf.setTimeZone(cal.getTimeZone());
        String source = "prefix2023-08-15suffix";
        // after "prefix"
        ParsePosition pos = new ParsePosition(6);
        Date expected = referenceSdf.parse("2023-08-15", new ParsePosition(0));
        Date actual = safeSdf.parse(source, pos);
        assertNotNull(actual);
        assertEquals(expected, actual);
        // 10 chars for "yyyy-MM-dd"
        assertEquals(6 + 10, pos.getIndex());
    }

    @Test
    void parse_ISO8601WithUTCTimezone() throws Exception {
        // X parses 'Z' as UTC
        String pattern = "yyyy-MM-dd'T'HH:mm:ssX";
        safeSdf = new SafeSimpleDateFormat(pattern);
        referenceSdf = new SimpleDateFormat(pattern);
        referenceSdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String source = "2023-08-15T12:00:00Z";
        ParsePosition pos = new ParsePosition(0);
        Date expected = referenceSdf.parse(source, new ParsePosition(0));
        Date actual = safeSdf.parse(source, pos);
        assertNotNull(actual);
        assertEquals(expected, actual);
        assertEquals(source.length(), pos.getIndex());
    }

    @Test
    void parse_LenientAcceptsOutOfRangeMonth() throws Exception {
        String pattern = "yyyy-MM-dd";
        safeSdf = new SafeSimpleDateFormat(pattern);
        safeSdf.setLenient(true);
        referenceSdf = new SimpleDateFormat(pattern);
        referenceSdf.setLenient(true);
        String source = "2023-13-01";
        ParsePosition pos = new ParsePosition(0);
        Date expected = referenceSdf.parse(source, new ParsePosition(0));
        Date actual = safeSdf.parse(source, pos);
        assertNotNull(actual);
        assertEquals(expected, actual);
        assertEquals(source.length(), pos.getIndex());
    }

    @Test
    void parse_NonLenientRejectsOutOfRangeMonth() {
        String pattern = "yyyy-MM-dd";
        safeSdf = new SafeSimpleDateFormat(pattern);
        safeSdf.setLenient(false);
        referenceSdf = new SimpleDateFormat(pattern);
        referenceSdf.setLenient(false);
        String source = "2023-13-01";
        ParsePosition pos = new ParsePosition(0);
        Date result = safeSdf.parse(source, pos);
        assertNull(result);
        assertEquals(-1, pos.getIndex());
        // SimpleDateFormat reports the error at the first invalid character of the month (index 5)
        assertEquals(5, pos.getErrorIndex());
    }

    @Test
    void parse_WithExplicitUTCTimezone() throws Exception {
        String pattern = "yyyy-MM-dd HH:mm";
        safeSdf = new SafeSimpleDateFormat(pattern);
        safeSdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        referenceSdf = new SimpleDateFormat(pattern);
        referenceSdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String source = "2023-08-15 12:00";
        ParsePosition pos = new ParsePosition(0);
        Date expected = referenceSdf.parse(source, new ParsePosition(0));
        Date actual = safeSdf.parse(source, pos);
        assertNotNull(actual);
        assertEquals(expected, actual);
        assertEquals(source.length(), pos.getIndex());
    }

    @Test
    void parse_TwoDigitYear_WithCustomStart() throws Exception {
        String pattern = "yy-MM-dd";
        safeSdf = new SafeSimpleDateFormat(pattern);
        // Set start date to 2000‑01‑01 so "23" becomes 2023
        Calendar start = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault());
        start.clear();
        start.set(2000, Calendar.JANUARY, 1);
        safeSdf.set2DigitYearStart(start.getTime());
        referenceSdf = new SimpleDateFormat(pattern);
        referenceSdf.set2DigitYearStart(start.getTime());
        String source = "23-08-15";
        ParsePosition pos = new ParsePosition(0);
        Date expected = referenceSdf.parse(source, new ParsePosition(0));
        Date actual = safeSdf.parse(source, pos);
        assertNotNull(actual);
        assertEquals(expected, actual);
        assertEquals(source.length(), pos.getIndex());
    }

    @Test
    void parse_FrenchMonthName() throws Exception {
        String pattern = "MMM dd, yyyy";
        safeSdf = new SafeSimpleDateFormat(pattern);
        // French DateFormatSymbols
        DateFormatSymbols french = new DateFormatSymbols(Locale.FRENCH);
        safeSdf.setDateFormatSymbols(french);
        referenceSdf = new SimpleDateFormat(pattern, Locale.FRENCH);
        referenceSdf.setDateFormatSymbols(french);
        String source = "août 15, 2023";
        ParsePosition pos = new ParsePosition(0);
        Date expected = referenceSdf.parse(source, new ParsePosition(0));
        Date actual = safeSdf.parse(source, pos);
        assertNotNull(actual);
        assertEquals(expected, actual);
        assertEquals(source.length(), pos.getIndex());
    }

    @Test
    void parse_LeapYear_Feb29Valid() throws Exception {
        String pattern = "yyyy-MM-dd";
        safeSdf = new SafeSimpleDateFormat(pattern);
        referenceSdf = new SimpleDateFormat(pattern);
        String source = "2020-02-29";
        ParsePosition pos = new ParsePosition(0);
        Date expected = referenceSdf.parse(source, new ParsePosition(0));
        Date actual = safeSdf.parse(source, pos);
        assertNotNull(actual);
        assertEquals(expected, actual);
        assertEquals(source.length(), pos.getIndex());
    }

    @Test
    void parse_NonLeapYear_Feb29Invalid() {
        String pattern = "yyyy-MM-dd";
        safeSdf = new SafeSimpleDateFormat(pattern);
        referenceSdf = new SimpleDateFormat(pattern);
        String source = "2019-02-29";
        ParsePosition pos = new ParsePosition(0);
        Date result = safeSdf.parse(source, pos);
        assertNull(result);
        assertEquals(-1, pos.getIndex());
        // SimpleDateFormat stops after day field (index 8)
        assertEquals(8, pos.getErrorIndex());
    }

    @Test
    void parse_EmptySourceString() {
        String pattern = "yyyy-MM-dd";
        safeSdf = new SafeSimpleDateFormat(pattern);
        ParsePosition pos = new ParsePosition(0);
        Date result = safeSdf.parse("", pos);
        assertNull(result);
        assertEquals(-1, pos.getIndex());
        assertEquals(0, pos.getErrorIndex());
    }

    @Test
    void parse_NegativeStartIndex() {
        String pattern = "yyyy-MM-dd";
        safeSdf = new SafeSimpleDateFormat(pattern);
        ParsePosition pos = new ParsePosition(-5);
        Date result = safeSdf.parse("2023-08-15", pos);
        assertNull(result);
        assertEquals(-1, pos.getIndex());
        assertEquals(-5, pos.getErrorIndex());
    }

    @Test
    void parse_MaximumYear() throws Exception {
        String pattern = "yyyyMMdd";
        safeSdf = new SafeSimpleDateFormat(pattern);
        referenceSdf = new SimpleDateFormat(pattern);
        referenceSdf.setLenient(false);
        String source = "99991231";
        ParsePosition pos = new ParsePosition(0);
        Date expected = referenceSdf.parse(source, new ParsePosition(0));
        Date actual = safeSdf.parse(source, pos);
        assertNotNull(actual);
        assertEquals(expected, actual);
        assertEquals(source.length(), pos.getIndex());
    }

    @Test
    void parse_TrailingCharactersIgnored() throws Exception {
        String pattern = "yyyy-MM-dd";
        safeSdf = new SafeSimpleDateFormat(pattern);
        referenceSdf = new SimpleDateFormat(pattern);
        String source = "2023-08-15XYZ";
        ParsePosition pos = new ParsePosition(0);
        Date expected = referenceSdf.parse("2023-08-15", new ParsePosition(0));
        Date actual = safeSdf.parse(source, pos);
        assertNotNull(actual);
        assertEquals(expected, actual);
        // only the date part is consumed
        assertEquals(10, pos.getIndex());
    }

    @Test
    void constructor_WithIllegalPattern_ThrowsIllegalArgumentException() {
        Executable ctor = () -> new SafeSimpleDateFormat("yyyy-??-dd");
        assertThrows(IllegalArgumentException.class, ctor, "An illegal pattern should cause IllegalArgumentException at construction");
    }

    @Test
    void constructor_WithNullPattern_ThrowsNullPointerException() {
        Executable ctor = () -> new SafeSimpleDateFormat(null);
        assertThrows(NullPointerException.class, ctor, "Passing null as the pattern must throw NullPointerException");
    }
}
