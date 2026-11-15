package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.text.SimpleDateFormat;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.text.ParsePosition;
import java.util.Calendar;
import java.util.Date;

class SafeSimpleDateFormat_setLenient_Test {

    private static final String FORMAT = "yyyy/MM/dd";

    private SafeSimpleDateFormat safeSdf;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh SafeSimpleDateFormat instance for each test
        safeSdf = new SafeSimpleDateFormat(FORMAT);
        // Ensure the underlying SimpleDateFormat starts in a known state (lenient = true)
        SimpleDateFormat underlying = SafeSimpleDateFormat.getDateFormat(FORMAT);
        underlying.setLenient(true);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the lenient flag to its default to avoid cross‑test interference
        SimpleDateFormat underlying = SafeSimpleDateFormat.getDateFormat(FORMAT);
        underlying.setLenient(true);
    }

    @Test
    void setLenient_false_updatesUnderlyingSimpleDateFormat() {
        // given
        // safeSdf is already instantiated in @BeforeEach
        // when
        safeSdf.setLenient(false);
        // then
        SimpleDateFormat underlying = SafeSimpleDateFormat.getDateFormat(FORMAT);
        assertFalse(underlying.isLenient(), "Underlying SimpleDateFormat should have lenient set to false");
    }

    @Test
    void setLenient_true_updatesUnderlyingSimpleDateFormat() {
        // given
        // safeSdf is already instantiated in @BeforeEach
        // Ensure it starts as false to see the change
        safeSdf.setLenient(false);
        SimpleDateFormat underlyingBefore = SafeSimpleDateFormat.getDateFormat(FORMAT);
        assertFalse(underlyingBefore.isLenient(), "Precondition failed: lenient should be false");
        // when
        safeSdf.setLenient(true);
        // then
        SimpleDateFormat underlyingAfter = SafeSimpleDateFormat.getDateFormat(FORMAT);
        assertTrue(underlyingAfter.isLenient(), "Underlying SimpleDateFormat should have lenient set to true");
    }

    @ParameterizedTest(name = "setLenient({0}) should affect parsing of an invalid ISO date")
    @CsvSource({ "true,  true,  2023-03-02", "false, false, null" })
    void setLenient_forwardingToUnderlying(boolean lenient, boolean expectSuccess, String expectedDateString) {
        // Arrange
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("yyyy-MM-dd");
        sdf.setLenient(lenient);
        ParsePosition pos = new ParsePosition(0);
        // Act
        Date result = sdf.parse("2023-02-30", pos);
        // Assert
        if (expectSuccess) {
            assertNotNull(result, "Parsing should succeed in lenient mode");
            assertEquals(8, pos.getIndex(), "ParsePosition should advance to end of input");
            // Verify the rolled‑over date is 2023‑03‑02
            Calendar cal = Calendar.getInstance();
            cal.setTime(result);
            assertEquals(2023, cal.get(Calendar.YEAR));
            assertEquals(Calendar.MARCH, cal.get(Calendar.MONTH));
            assertEquals(2, cal.get(Calendar.DAY_OF_MONTH));
        } else {
            assertNull(result, "Parsing should fail in strict mode");
            assertEquals(0, pos.getIndex(), "ParsePosition should remain at 0 on failure");
        }
    }

    @ParameterizedTest(name = "Pattern dd/MM/yyyy with lenient={0}")
    @CsvSource({ "true,  true,  2023-03-03", "false, false, null" })
    void setLenient_differentPattern(String dummy, boolean lenient, boolean expectSuccess, String expectedDateString) {
        // The dummy column is required because JUnit does not allow a leading boolean without a name.
        // Arrange
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("dd/MM/yyyy");
        sdf.setLenient(lenient);
        ParsePosition pos = new ParsePosition(0);
        // Act
        Date result = sdf.parse("31/02/2023", pos);
        // Assert
        if (expectSuccess) {
            assertNotNull(result, "Lenient parsing should succeed for out‑of‑range day");
            Calendar cal = Calendar.getInstance();
            cal.setTime(result);
            assertEquals(2023, cal.get(Calendar.YEAR));
            assertEquals(Calendar.MARCH, cal.get(Calendar.MONTH));
            assertEquals(3, cal.get(Calendar.DAY_OF_MONTH));
        } else {
            assertNull(result, "Strict parsing should reject the invalid date");
            assertEquals(0, pos.getIndex(), "ParsePosition should stay at 0 on failure");
        }
    }

    @Test
    void setLenient_sequentialFlagChanges() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("yyyy-MM-dd");
        // First call – enable lenient mode
        sdf.setLenient(true);
        ParsePosition pos1 = new ParsePosition(0);
        Date d1 = sdf.parse("2023-02-30", pos1);
        assertNotNull(d1, "First parse (lenient) should succeed");
        assertEquals(8, pos1.getIndex(), "ParsePosition should advance after successful parse");
        // Second call – switch to strict mode
        sdf.setLenient(false);
        ParsePosition pos2 = new ParsePosition(0);
        Date d2 = sdf.parse("2023-02-30", pos2);
        assertNull(d2, "Second parse (strict) should fail");
        assertEquals(0, pos2.getIndex(), "ParsePosition should remain at 0 after failure");
    }

    @Test
    void setLenient_independenceAcrossInstances() {
        // Instance A – ISO pattern
        SafeSimpleDateFormat sdfA = new SafeSimpleDateFormat("yyyy-MM-dd");
        // Instance B – month‑year pattern
        SafeSimpleDateFormat sdfB = new SafeSimpleDateFormat("MM-yyyy");
        // Change lenient flag only on instance A
        sdfA.setLenient(true);
        // also set B to false to prove independence later
        sdfB.setLenient(false);
        // Parsing with A (lenient) – should succeed despite invalid day
        ParsePosition posA = new ParsePosition(0);
        Date dateA = sdfA.parse("2023-02-30", posA);
        assertNotNull(dateA, "Instance A should parse leniently");
        assertEquals(8, posA.getIndex(), "ParsePosition for A should advance");
        // Parsing with B – pattern does not accept "13/2023" regardless of leniency
        ParsePosition posB = new ParsePosition(0);
        Date dateB = sdfB.parse("13/2023", posB);
        assertNull(dateB, "Instance B should fail to parse mismatched string");
        assertEquals(0, posB.getIndex(), "ParsePosition for B should stay at 0");
        // Verify that the underlying formatter for pattern "MM-yyyy" is still lenient = false
        SimpleDateFormat underlyingB = SafeSimpleDateFormat.getDateFormat("MM-yyyy");
        assertFalse(underlyingB.isLenient(), "Underlying formatter for B should retain its own flag");
    }

    @Test
    void setLenient_emptyPattern_throwsException() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("");
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> sdf.setLenient(true), "Expected IllegalArgumentException for empty pattern");
        assertTrue(thrown.getMessage().toLowerCase().contains("empty"), "Exception message should mention 'empty' pattern");
    }

    @Test
    void setLenient_nullPattern_throwsNPE() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(null);
        NullPointerException thrown = assertThrows(NullPointerException.class, () -> sdf.setLenient(true), "Expected NullPointerException when format is null");
        assertTrue(thrown.getMessage().toLowerCase().contains("null") || thrown.getMessage().toLowerCase().contains("format"), "Exception message should mention null format");
    }

    @Test
    void setLenient_nullPattern_false_throwsNPE() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(null);
        NullPointerException thrown = assertThrows(NullPointerException.class, () -> sdf.setLenient(false), "Expected NullPointerException when format is null");
        assertTrue(thrown.getMessage().toLowerCase().contains("null") || thrown.getMessage().toLowerCase().contains("format"), "Exception message should mention null format");
    }
}
