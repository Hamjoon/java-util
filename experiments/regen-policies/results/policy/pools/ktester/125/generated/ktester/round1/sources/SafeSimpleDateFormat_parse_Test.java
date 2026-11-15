package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.text.*;
import java.time.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SafeSimpleDateFormat_parse_Test {

    // -----------------------------------------------------------------------
    private static final String FORMAT = "yyyy-MM-dd";

    private static final String SOURCE = "2023-01-01";

    // class under test
    private SafeSimpleDateFormat safeSimpleDateFormat;

    // mock returned by getDateFormat
    private SimpleDateFormat delegateMock;

    // mock for the static method
    private MockedStatic<SafeSimpleDateFormat> staticMock;

    private ParsePosition parsePosition;

    private Date expectedDate;

    // -----------------------------------------------------------------------
    private static final String DEFAULT_FORMAT = "yyyy-MM-dd";

    @BeforeEach
    void setupBeforeEach() {
        // Create a mock SimpleDateFormat that will be returned by the static factory
        delegateMock = mock(SimpleDateFormat.class);
        // Prepare a deterministic Date that the mock will return
        // 2022‑01‑01 00:00:00 UTC
        expectedDate = new Date(1_640_995_200_000L);
        // Configure the mock to return the expected Date for any source/position
        when(delegateMock.parse(anyString(), any(ParsePosition.class))).thenReturn(expectedDate);
        // Mock the static method SafeSimpleDateFormat.getDateFormat(String)
        staticMock = Mockito.mockStatic(SafeSimpleDateFormat.class);
        staticMock.when(() -> SafeSimpleDateFormat.getDateFormat(FORMAT)).thenReturn(delegateMock);
        // Create the instance under test – its constructor will call the mocked static method
        safeSimpleDateFormat = new SafeSimpleDateFormat(FORMAT);
        // Fresh ParsePosition for each test
        parsePosition = new ParsePosition(0);
        // for the default format. Individual tests can override this behaviour.
        staticMock = Mockito.mockStatic(SafeSimpleDateFormat.class);
        staticMock.when(() -> SafeSimpleDateFormat.getDateFormat(DEFAULT_FORMAT)).thenAnswer(invocation -> new SimpleDateFormat(DEFAULT_FORMAT));
        safeSimpleDateFormat = new SafeSimpleDateFormat(DEFAULT_FORMAT);
        parsePosition = new ParsePosition(0);
    }

    @AfterEach
    void teardownAfterEach() {
        // Release the static mock so it does not affect other tests
        if (staticMock != null) {
            staticMock.close();
        }
        if (staticMock != null) {
            staticMock.close();
        }
    }

    /**
     * Creates a {@link Date} at midnight UTC for the given ISO‑date string (yyyy‑MM‑dd).
     */
    private static Date utcMidnight(String isoDate) {
        LocalDate ld = LocalDate.parse(isoDate);
        return Date.from(ld.atStartOfDay(ZoneOffset.UTC).toInstant());
    }

    /**
     * Provides arguments for the “Valid parsing scenarios” group.
     */
    private static Stream<Arguments> validParsingProvider() {
        return Stream.of(// source, format, expectedDate, expectedIndex, description
        Arguments.of("2023-08-15", DEFAULT_FORMAT, utcMidnight("2023-08-15"), 10, "Exact match – index should be at the end of the string."), Arguments.of("20230815extra", DEFAULT_FORMAT, utcMidnight("2023-08-15"), 8, "Valid prefix followed by extra characters – parser stops after the date."), Arguments.of("15/08/2023", "dd/MM/yyyy", utcMidnight("2023-08-15"), 10, "Different pattern – parsing succeeds with a custom format."));
    }

    // -----------------------------------------------------------------------
    @Test
    void parse_DelegatesToSimpleDateFormatAndReturnsDate() {
        // WHEN
        Date actual = safeSimpleDateFormat.parse(SOURCE, parsePosition);
        // THEN
        assertEquals(expectedDate, actual, "The date returned by parse should be the one provided by the delegate");
        // Verify that the delegate SimpleDateFormat was called with the exact arguments
        verify(delegateMock, times(1)).parse(eq(SOURCE), eq(parsePosition));
        // Verify that the static factory was queried with the correct format string
        staticMock.verify(() -> SafeSimpleDateFormat.getDateFormat(FORMAT), times(1));
    }

    @Test
    void parse_PassesParsePositionThroughDelegate() {
        // GIVEN – a ParsePosition that starts at index 5
        parsePosition.setIndex(5);
        // WHEN
        safeSimpleDateFormat.parse(SOURCE, parsePosition);
        // THEN – the same ParsePosition instance must be passed to the delegate
        verify(delegateMock).parse(eq(SOURCE), same(parsePosition));
    }

    @Test
    void parse_ReturnsNullWhenDelegateReturnsNull() {
        // GIVEN – configure the delegate to return null
        when(delegateMock.parse(anyString(), any(ParsePosition.class))).thenReturn(null);
        // WHEN
        Date actual = safeSimpleDateFormat.parse(SOURCE, parsePosition);
        // THEN
        assertNull(actual, "When the underlying SimpleDateFormat returns null, parse should also return null");
    }

    // -----------------------------------------------------------------------
    @ParameterizedTest(name = "{4}")
    @MethodSource("validParsingProvider")
    void parse_ValidScenarios(String source, String format, Date expectedDate, int expectedIndex, String _desc) {
        // Re‑configure the static mock for the specific format used in this case
        staticMock.when(() -> SafeSimpleDateFormat.getDateFormat(format)).thenAnswer(invocation -> new SimpleDateFormat(format));
        // Re‑instantiate the class under test with the required format
        safeSimpleDateFormat = new SafeSimpleDateFormat(format);
        parsePosition = new ParsePosition(0);
        // WHEN
        Date actual = safeSimpleDateFormat.parse(source, parsePosition);
        // THEN
        assertNotNull(actual, "Parse should succeed and return a non‑null Date");
        assertEquals(expectedDate.getTime(), actual.getTime(), "Returned Date should represent the expected calendar day");
        assertEquals(expectedIndex, parsePosition.getIndex(), "ParsePosition index should be advanced to the character after the parsed date");
    }

    // -----------------------------------------------------------------------
    @Test
    void parse_NullSource_ThrowsNullPointerException() {
        // Use a real SimpleDateFormat for the default pattern – it throws NPE on null source
        staticMock.when(() -> SafeSimpleDateFormat.getDateFormat(DEFAULT_FORMAT)).thenAnswer(invocation -> new SimpleDateFormat(DEFAULT_FORMAT));
        safeSimpleDateFormat = new SafeSimpleDateFormat(DEFAULT_FORMAT);
        parsePosition = new ParsePosition(0);
        assertThrows(NullPointerException.class, () -> safeSimpleDateFormat.parse(null, parsePosition), "Passing a null source should result in NullPointerException");
    }

    @Test
    void parse_NullParsePosition_ThrowsNullPointerException() {
        staticMock.when(() -> SafeSimpleDateFormat.getDateFormat(DEFAULT_FORMAT)).thenAnswer(invocation -> new SimpleDateFormat(DEFAULT_FORMAT));
        safeSimpleDateFormat = new SafeSimpleDateFormat(DEFAULT_FORMAT);
        assertThrows(NullPointerException.class, () -> safeSimpleDateFormat.parse("2023-08-15", null), "Passing a null ParsePosition should result in NullPointerException");
    }

    // -----------------------------------------------------------------------
    @Test
    void parse_StartsAtOffset_ParsesSuccessfully() {
        staticMock.when(() -> SafeSimpleDateFormat.getDateFormat(DEFAULT_FORMAT)).thenAnswer(invocation -> new SimpleDateFormat(DEFAULT_FORMAT));
        safeSimpleDateFormat = new SafeSimpleDateFormat(DEFAULT_FORMAT);
        // "abc2023-08-15XYZ" -> date starts at index 3
        parsePosition = new ParsePosition(3);
        String source = "abc2023-08-15XYZ";
        Date actual = safeSimpleDateFormat.parse(source, parsePosition);
        assertNotNull(actual);
        assertEquals(utcMidnight("2023-08-15").getTime(), actual.getTime(), "Date should be parsed correctly when starting at a non‑zero offset");
        assertEquals(13, parsePosition.getIndex(), "ParsePosition should be advanced to the character after the parsed date");
    }

    @Test
    void parse_StartsInsidePattern_FailsAndLeavesErrorIndex() {
        staticMock.when(() -> SafeSimpleDateFormat.getDateFormat(DEFAULT_FORMAT)).thenAnswer(invocation -> new SimpleDateFormat(DEFAULT_FORMAT));
        safeSimpleDateFormat = new SafeSimpleDateFormat(DEFAULT_FORMAT);
        // inside "2023-08-15"
        parsePosition = new ParsePosition(5);
        String source = "2023-08-15extra";
        Date actual = safeSimpleDateFormat.parse(source, parsePosition);
        assertNull(actual, "Parsing should fail when the start index cuts the pattern");
        assertEquals(5, parsePosition.getErrorIndex(), "Error index should be set to the original start position");
    }

    // -----------------------------------------------------------------------
    @Test
    void parse_LenientMode_RollsOverInvalidDay() {
        staticMock.when(() -> SafeSimpleDateFormat.getDateFormat(DEFAULT_FORMAT)).thenAnswer(invocation -> {
            SimpleDateFormat sdf = new SimpleDateFormat(DEFAULT_FORMAT);
            // default, but set explicitly for clarity
            sdf.setLenient(true);
            return sdf;
        });
        safeSimpleDateFormat = new SafeSimpleDateFormat(DEFAULT_FORMAT);
        parsePosition = new ParsePosition(0);
        // Feb 30 rolls over to Mar 2 in lenient mode
        String source = "2023-02-30";
        Date actual = safeSimpleDateFormat.parse(source, parsePosition);
        // Expected date: 2023‑03‑02
        Date expected = utcMidnight("2023-03-02");
        assertNotNull(actual);
        assertEquals(expected.getTime(), actual.getTime(), "Lenient parsing should roll over to the next valid date");
        assertEquals(10, parsePosition.getIndex(), "ParsePosition should be advanced to the end of the parsed text");
    }

    @Test
    void parse_StrictMode_InvalidDateReturnsNull() {
        staticMock.when(() -> SafeSimpleDateFormat.getDateFormat(DEFAULT_FORMAT)).thenAnswer(invocation -> {
            SimpleDateFormat sdf = new SimpleDateFormat(DEFAULT_FORMAT);
            sdf.setLenient(false);
            return sdf;
        });
        safeSimpleDateFormat = new SafeSimpleDateFormat(DEFAULT_FORMAT);
        // Explicitly set strict mode via the API (even though the factory already did)
        safeSimpleDateFormat.setLenient(false);
        parsePosition = new ParsePosition(0);
        String source = "2023-02-30";
        Date actual = safeSimpleDateFormat.parse(source, parsePosition);
        assertNull(actual, "Strict parsing should return null for an out‑of‑range day");
        assertEquals(0, parsePosition.getErrorIndex(), "Error index should be set to the start position when parsing fails");
    }

    // -----------------------------------------------------------------------
    @Test
    void parse_LeapYearDate_ParsesSuccessfully() {
        staticMock.when(() -> SafeSimpleDateFormat.getDateFormat(DEFAULT_FORMAT)).thenAnswer(invocation -> new SimpleDateFormat(DEFAULT_FORMAT));
        safeSimpleDateFormat = new SafeSimpleDateFormat(DEFAULT_FORMAT);
        parsePosition = new ParsePosition(0);
        String source = "2020-02-29";
        Date actual = safeSimpleDateFormat.parse(source, parsePosition);
        assertNotNull(actual);
        assertEquals(utcMidnight("2020-02-29").getTime(), actual.getTime(), "Valid leap‑year date should be parsed correctly");
        assertEquals(10, parsePosition.getIndex(), "ParsePosition should point after the parsed date");
    }

    @Test
    void parse_NonLeapYearFeb29_ReturnsNullAndSetsErrorIndex() {
        staticMock.when(() -> SafeSimpleDateFormat.getDateFormat(DEFAULT_FORMAT)).thenAnswer(invocation -> new SimpleDateFormat(DEFAULT_FORMAT));
        safeSimpleDateFormat = new SafeSimpleDateFormat(DEFAULT_FORMAT);
        parsePosition = new ParsePosition(0);
        String source = "2019-02-29";
        Date actual = safeSimpleDateFormat.parse(source, parsePosition);
        assertNull(actual, "Non‑leap‑year Feb‑29 should not be parsed");
        assertEquals(0, parsePosition.getErrorIndex(), "Error index should be set to the start position on failure");
    }

    // -----------------------------------------------------------------------
    @Test
    void parse_EmptyString_ReturnsNullAndSetsErrorIndex() {
        staticMock.when(() -> SafeSimpleDateFormat.getDateFormat(DEFAULT_FORMAT)).thenAnswer(invocation -> new SimpleDateFormat(DEFAULT_FORMAT));
        safeSimpleDateFormat = new SafeSimpleDateFormat(DEFAULT_FORMAT);
        parsePosition = new ParsePosition(0);
        String source = "";
        Date actual = safeSimpleDateFormat.parse(source, parsePosition);
        assertNull(actual);
        assertEquals(0, parsePosition.getErrorIndex(), "Empty source should cause an error at index 0");
    }

    @Test
    void parse_LeadingWhitespaceAtZeroIndex_Fails() {
        staticMock.when(() -> SafeSimpleDateFormat.getDateFormat(DEFAULT_FORMAT)).thenAnswer(invocation -> new SimpleDateFormat(DEFAULT_FORMAT));
        safeSimpleDateFormat = new SafeSimpleDateFormat(DEFAULT_FORMAT);
        parsePosition = new ParsePosition(0);
        String source = "   2023-08-15";
        Date actual = safeSimpleDateFormat.parse(source, parsePosition);
        assertNull(actual);
        assertEquals(0, parsePosition.getErrorIndex(), "Leading whitespace prevents matching at index 0");
    }

    @Test
    void parse_LeadingWhitespaceWithAdjustedParsePosition_ParsesSuccessfully() {
        staticMock.when(() -> SafeSimpleDateFormat.getDateFormat(DEFAULT_FORMAT)).thenAnswer(invocation -> new SimpleDateFormat(DEFAULT_FORMAT));
        safeSimpleDateFormat = new SafeSimpleDateFormat(DEFAULT_FORMAT);
        // start after the three spaces
        parsePosition = new ParsePosition(3);
        String source = "   2023-08-15";
        Date actual = safeSimpleDateFormat.parse(source, parsePosition);
        assertNotNull(actual);
        assertEquals(utcMidnight("2023-08-15").getTime(), actual.getTime(), "Parsing should succeed when the position skips the whitespace");
        assertEquals(13, parsePosition.getIndex(), "Index should be advanced to the character after the parsed date");
    }

    // -----------------------------------------------------------------------
    @Test
    void parse_MultipleDates_ParsesOnlyFirstAndAdvancesPosition() {
        staticMock.when(() -> SafeSimpleDateFormat.getDateFormat(DEFAULT_FORMAT)).thenAnswer(invocation -> new SimpleDateFormat(DEFAULT_FORMAT));
        safeSimpleDateFormat = new SafeSimpleDateFormat(DEFAULT_FORMAT);
        parsePosition = new ParsePosition(0);
        String source = "2023-08-15 2023-09-01";
        Date actual = safeSimpleDateFormat.parse(source, parsePosition);
        assertNotNull(actual);
        assertEquals(utcMidnight("2023-08-15").getTime(), actual.getTime(), "Only the first date should be parsed");
        assertEquals(10, parsePosition.getIndex(), "ParsePosition should point right after the first date");
    }

    // -----------------------------------------------------------------------
    @Test
    void parse_NullFormatString_ThrowsNullPointerException() {
        // Configure the static mock to throw NPE when a null format is requested
        staticMock.when(() -> SafeSimpleDateFormat.getDateFormat(null)).thenThrow(new NullPointerException("format is null"));
        safeSimpleDateFormat = new SafeSimpleDateFormat(null);
        parsePosition = new ParsePosition(0);
        assertThrows(NullPointerException.class, () -> safeSimpleDateFormat.parse("2023-08-15", parsePosition), "Instantiating with a null format should cause NullPointerException on parse");
    }

    @Test
    void parse_InvalidPattern_ThrowsIllegalArgumentException() {
        String badPattern = "yyyy-??-dd";
        staticMock.when(() -> SafeSimpleDateFormat.getDateFormat(badPattern)).thenThrow(new IllegalArgumentException("Invalid pattern"));
        safeSimpleDateFormat = new SafeSimpleDateFormat(badPattern);
        parsePosition = new ParsePosition(0);
        assertThrows(IllegalArgumentException.class, () -> safeSimpleDateFormat.parse("2023-08-15", parsePosition), "An illegal pattern should propagate IllegalArgumentException from the factory");
    }
}
