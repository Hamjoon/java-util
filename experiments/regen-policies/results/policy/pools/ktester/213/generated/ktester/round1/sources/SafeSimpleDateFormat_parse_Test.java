package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SafeSimpleDateFormat_parse_Test {

    private SafeSimpleDateFormat safeSimpleDateFormat;

    private static final String PATTERN = "yyyy-MM-dd";

    private static final String DEFAULT_PATTERN = "yyyy-MM-dd";

    private static final class ValidScenario {

        final String pattern;

        final String source;

        final int startIndex;

        // length of successfully parsed substring
        final int expectedIndexAdvance;

        final boolean expectFailure;

        ValidScenario(String pattern, String source, int startIndex, int expectedIndexAdvance, boolean expectFailure) {
            this.pattern = pattern;
            this.source = source;
            this.startIndex = startIndex;
            this.expectedIndexAdvance = expectedIndexAdvance;
            this.expectFailure = expectFailure;
        }

        @Override
        public String toString() {
            return "ValidScenario{" + "pattern='" + pattern + '\'' + ", source='" + source + '\'' + ", startIndex=" + startIndex + ", expectedIndexAdvance=" + expectedIndexAdvance + ", expectFailure=" + expectFailure + '}';
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh instance for each test to avoid cross‑test contamination
        safeSimpleDateFormat = new SafeSimpleDateFormat(PATTERN);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and make the intent explicit
        safeSimpleDateFormat = null;
    }

    /* -------------------------------------------------------------
     *  Parameterised tests for the “Valid parsing scenarios” group
     * ------------------------------------------------------------- */
    private static Stream<ValidScenario> validScenariosProvider() {
        return Stream.of(// 1. Standard ISO‑like date, pattern yyyy‑MM‑dd, start at 0
        new ValidScenario("yyyy-MM-dd", "2023-08-15", 0, // expected index after parse (length of "2023-08-15")
        10, false), // 2. European style date, pattern dd/MM/yyyy, start at 0
        new ValidScenario("dd/MM/yyyy", "31/12/1999", 0, // "31/12/1999".length()
        10, false), // 3. Extra text after a correct date, pattern yyyy‑MM‑dd, start at 0
        new ValidScenario("yyyy-MM-dd", "2023-08-15 extra", 0, // parser stops before the space
        10, false), // 4. Start parsing at a position that points to a space (failure)
        new ValidScenario("yyyy-MM-dd", "2023-08-15 extra", 11, // indicates failure (null result)
        -1, true));
    }

    @Test
    void parse_ValidDate_ReturnsCorrectDateAndUpdatesParsePosition() throws Exception {
        // given
        String source = "2023-08-15";
        ParsePosition pos = new ParsePosition(0);
        SimpleDateFormat expectedFormatter = new SimpleDateFormat(DEFAULT_PATTERN);
        expectedFormatter.setLenient(false);
        Date expected = expectedFormatter.parse(source);
        // when
        Date actual = safeSimpleDateFormat.parse(source, pos);
        // then
        assertNotNull(actual, "Parsed date should not be null for a valid input");
        assertEquals(expected.getTime(), actual.getTime(), "Parsed date should match expected value");
        assertEquals(source.length(), pos.getIndex(), "ParsePosition index should be advanced to end of input");
        assertEquals(-1, pos.getErrorIndex(), "Error index should remain -1 for successful parse");
    }

    @Test
    void parse_InvalidDate_ReturnsNullAndSetsErrorIndex() {
        // given
        String source = "invalid-date";
        ParsePosition pos = new ParsePosition(0);
        // when
        Date result = safeSimpleDateFormat.parse(source, pos);
        // then
        assertNull(result, "Parsing an invalid date string should return null");
        assertEquals(0, pos.getErrorIndex(), "Error index should be set to the position where parsing failed");
        // index should stay at the start because nothing was consumed
        assertEquals(0, pos.getIndex(), "ParsePosition index should remain unchanged on failure");
    }

    @Test
    void parse_WithOffsetStart_ParsesFromGivenPosition() throws Exception {
        // given
        String source = "xxxx2023-08-15";
        // start parsing at index 4 (right before the date)
        ParsePosition pos = new ParsePosition(4);
        SimpleDateFormat expectedFormatter = new SimpleDateFormat(DEFAULT_PATTERN);
        expectedFormatter.setLenient(false);
        Date expected = expectedFormatter.parse("2023-08-15");
        // when
        Date actual = safeSimpleDateFormat.parse(source, pos);
        // then
        assertNotNull(actual, "Parsed date should not be null when starting from a valid offset");
        assertEquals(expected.getTime(), actual.getTime(), "Parsed date should match expected value");
        // index should now point just after the parsed date (4 + length of date string)
        assertEquals(4 + DEFAULT_PATTERN.length(), pos.getIndex(), "ParsePosition index should be advanced past the parsed substring");
        assertEquals(-1, pos.getErrorIndex(), "Error index should remain -1 for successful parse");
    }

    @ParameterizedTest(name = "[{index}] pattern={0}, source={1}, start={2}")
    @MethodSource("validScenariosProvider")
    void parse_ValidScenarios_Parameterized(ValidScenario scenario) throws Exception {
        // Arrange – create a formatter with the scenario‑specific pattern
        SafeSimpleDateFormat formatter = new SafeSimpleDateFormat(scenario.pattern);
        ParsePosition pos = new ParsePosition(scenario.startIndex);
        // Act
        Date result = formatter.parse(scenario.source, pos);
        // Assert
        if (scenario.expectFailure) {
            assertNull(result, "Parsing should fail and return null");
            assertEquals(scenario.startIndex, pos.getErrorIndex(), "Error index must be set to the position where parsing started");
            assertEquals(scenario.startIndex, pos.getIndex(), "ParsePosition index must remain unchanged on failure");
        } else {
            assertNotNull(result, "Parsing should succeed");
            // Verify the date using a plain SimpleDateFormat (lenient = false)
            SimpleDateFormat reference = new SimpleDateFormat(scenario.pattern);
            reference.setLenient(false);
            Date expected = reference.parse(scenario.source.substring(scenario.startIndex, scenario.startIndex + scenario.expectedIndexAdvance));
            assertEquals(expected.getTime(), result.getTime(), "Parsed date must match the reference formatter");
            assertEquals(scenario.startIndex + scenario.expectedIndexAdvance, pos.getIndex(), "ParsePosition index must be advanced correctly");
            assertEquals(-1, pos.getErrorIndex(), "Error index should stay -1 on successful parse");
        }
    }

    /* -------------------------------------------------------------
     *  Null and error handling tests
     * ------------------------------------------------------------- */
    @Test
    void parse_NullSource_ThrowsNullPointerException() {
        // given
        ParsePosition pos = new ParsePosition(0);
        // when / then
        assertThrows(NullPointerException.class, () -> safeSimpleDateFormat.parse(null, pos), "Passing a null source should throw NullPointerException");
        // The ParsePosition should remain untouched
        assertEquals(0, pos.getIndex(), "ParsePosition index must stay at 0");
        assertEquals(-1, pos.getErrorIndex(), "Error index must stay at -1");
    }

    @Test
    void parse_NullParsePosition_ThrowsNullPointerException() {
        // given
        String source = "2023-08-15";
        // when / then
        assertThrows(NullPointerException.class, () -> safeSimpleDateFormat.parse(source, null), "Passing a null ParsePosition should throw NullPointerException");
    }

    /* -------------------------------------------------------------
     *  Invalid format string handling
     * ------------------------------------------------------------- */
    @Test
    void parse_InvalidPattern_ConstructorThrowsIllegalArgumentException() {
        // The pattern contains illegal pattern letters ("XX")
        String badPattern = "yyyy-XX-dd";
        // Construction of SafeSimpleDateFormat itself does not validate the pattern,
        // but the first call to parse() triggers SimpleDateFormat creation which throws.
        SafeSimpleDateFormat badFormatter = new SafeSimpleDateFormat(badPattern);
        ParsePosition pos = new ParsePosition(0);
        assertThrows(IllegalArgumentException.class, () -> badFormatter.parse("2023-08-15", pos), "An illegal pattern should cause SimpleDateFormat to throw IllegalArgumentException");
    }
}
