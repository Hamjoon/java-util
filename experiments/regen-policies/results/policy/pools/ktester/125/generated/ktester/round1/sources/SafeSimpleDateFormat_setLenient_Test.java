package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.text.SimpleDateFormat;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.text.ParsePosition;
import java.util.Date;

@ExtendWith(MockitoExtension.class)
class SafeSimpleDateFormat_setLenient_Test {

    /**
     * The format string used for the tests – each test uses a distinct value to avoid
     *  interference through the internal ThreadLocal cache.
     */
    private static final String FORMAT_1 = "yyyy/MM/dd";

    private static final String FORMAT_2 = "dd-MM-yyyy";

    // Additional format strings used by specific test groups
    private static final String AMBIGUOUS_MM_DD_YYYY = "MM/dd/yyyy";

    private static final String YEAR_MONTH_DAY = "yyyy-MM-dd";

    @Test
    void setLenient_false_isPropagatedToUnderlyingSimpleDateFormat() {
        // given
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat(FORMAT_1);
        // when
        safe.setLenient(false);
        // then
        SimpleDateFormat underlying = SafeSimpleDateFormat.getDateFormat(FORMAT_1);
        assertNotNull(underlying, "Underlying SimpleDateFormat should not be null");
        assertFalse(underlying.isLenient(), "Underlying SimpleDateFormat should have lenient set to false");
    }

    @Test
    void setLenient_true_isPropagatedToUnderlyingSimpleDateFormat() {
        // given
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat(FORMAT_2);
        // when
        safe.setLenient(true);
        // then
        SimpleDateFormat underlying = SafeSimpleDateFormat.getDateFormat(FORMAT_2);
        assertNotNull(underlying, "Underlying SimpleDateFormat should not be null");
        assertTrue(underlying.isLenient(), "Underlying SimpleDateFormat should have lenient set to true");
    }

    /* ----------------------------------------------------------------------
     * Basic functionality – parsing with ambiguous date in lenient / strict mode
     * ---------------------------------------------------------------------- */
    @ParameterizedTest(name = "lenient={0} → parsing \"02/30/2023\"")
    @CsvSource({ "true, true, 2023-03-02", "false, false, null" })
    void basicParsingBehavior(boolean lenient, boolean expectSuccess, String expectedDateString) throws Exception {
        // given
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat(AMBIGUOUS_MM_DD_YYYY);
        safe.setLenient(lenient);
        ParsePosition pos = new ParsePosition(0);
        // when
        Date result = safe.parse("02/30/2023", pos);
        // then
        if (expectSuccess) {
            assertNotNull(result, "Parsing should succeed in lenient mode");
            // Verify the normalized date (Feb 30 → Mar 2)
            SimpleDateFormat verifier = new SimpleDateFormat("yyyy-MM-dd");
            assertEquals(expectedDateString, verifier.format(result));
            assertEquals(10, pos.getIndex(), "ParsePosition should be advanced to end of input");
        } else {
            assertNull(result, "Parsing should fail in strict mode");
            assertEquals(0, pos.getIndex(), "ParsePosition index should remain 0 on failure");
        }
    }

    /* ---------------------------------------------------------
     * Toggle behaviour – change lenient flag at runtime
     * --------------------------------------------------------- */
    @Test
    void toggleLenientFlag_runtimeEffect() throws Exception {
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat(AMBIGUOUS_MM_DD_YYYY);
        // start strict
        safe.setLenient(false);
        ParsePosition pos1 = new ParsePosition(0);
        Date d1 = safe.parse("02/30/2023", pos1);
        assertNull(d1, "Strict mode should reject ambiguous date");
        assertEquals(0, pos1.getIndex());
        // switch to lenient
        safe.setLenient(true);
        ParsePosition pos2 = new ParsePosition(0);
        Date d2 = safe.parse("02/30/2023", pos2);
        assertNotNull(d2, "Lenient mode should accept ambiguous date after toggle");
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
        assertEquals("2023-03-02", fmt.format(d2));
        assertEquals(10, pos2.getIndex());
    }

    /* ---------------------------------------------------------
     * Different pattern – month overflow handling
     * --------------------------------------------------------- */
    @ParameterizedTest(name = "lenient={0} → parsing \"2023-13-01\"")
    @CsvSource({ "true, true, 2024-01-01", "false, false, null" })
    void differentPatternParsing(boolean lenient, boolean expectSuccess, String expected) throws Exception {
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat(YEAR_MONTH_DAY);
        safe.setLenient(lenient);
        ParsePosition pos = new ParsePosition(0);
        Date result = safe.parse("2023-13-01", pos);
        if (expectSuccess) {
            assertNotNull(result);
            SimpleDateFormat verifier = new SimpleDateFormat("yyyy-MM-dd");
            assertEquals(expected, verifier.format(result));
            assertEquals(10, pos.getIndex());
        } else {
            assertNull(result);
            assertEquals(0, pos.getIndex());
        }
    }

    /* ---------------------------------------------------------
     * Formatting unaffected by lenient flag
     * --------------------------------------------------------- */
    @ParameterizedTest(name = "lenient={0} → formatting date")
    @CsvSource({ "true", "false" })
    void formattingUnaffectedByLenient(boolean lenient) throws Exception {
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat(AMBIGUOUS_MM_DD_YYYY);
        safe.setLenient(lenient);
        Date date = new SimpleDateFormat("yyyy-MM-dd").parse("2023-03-15");
        String formatted = safe.format(date);
        assertEquals("03/15/2023", formatted, "Formatting output must stay constant regardless of lenient flag");
    }

    /* ---------------------------------------------------------
     * Cache reuse consistency – two instances share the same SimpleDateFormat
     * --------------------------------------------------------- */
    @Test
    void cacheReuseConsistency() throws Exception {
        // First instance sets lenient true
        SafeSimpleDateFormat first = new SafeSimpleDateFormat(AMBIGUOUS_MM_DD_YYYY);
        first.setLenient(true);
        ParsePosition pos1 = new ParsePosition(0);
        Date d1 = first.parse("02/30/2023", pos1);
        assertNotNull(d1, "First instance in lenient mode should parse successfully");
        // Second instance, same pattern, should see the same underlying SimpleDateFormat
        SafeSimpleDateFormat second = new SafeSimpleDateFormat(AMBIGUOUS_MM_DD_YYYY);
        ParsePosition pos2 = new ParsePosition(0);
        Date d2 = second.parse("02/30/2023", pos2);
        assertNotNull(d2, "Second instance should inherit lenient flag from cache");
        SimpleDateFormat verifier = new SimpleDateFormat("yyyy-MM-dd");
        assertEquals("2023-03-02", verifier.format(d2));
        // Now change flag via first instance to false
        first.setLenient(false);
        ParsePosition pos3 = new ParsePosition(0);
        Date d3 = second.parse("02/30/2023", pos3);
        assertNull(d3, "After toggling to strict, second instance must reject ambiguous date");
        assertEquals(0, pos3.getIndex());
    }

    /* ---------------------------------------------------------
     * Null format string – expect NullPointerException
     * --------------------------------------------------------- */
    @Test
    void nullFormatString_throwsNPE() {
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat(null);
        assertThrows(NullPointerException.class, () -> safe.setLenient(true), "Calling setLenient on a formatter constructed with null pattern should throw NPE");
    }

    /* ---------------------------------------------------------
     * Invalid pattern – expect IllegalArgumentException
     * --------------------------------------------------------- */
    @Test
    void invalidPattern_throwsIAE() {
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat("yyyy-##-dd");
        assertThrows(IllegalArgumentException.class, () -> safe.setLenient(false), "Calling setLenient on a formatter with an illegal pattern should throw IAE");
    }
}
