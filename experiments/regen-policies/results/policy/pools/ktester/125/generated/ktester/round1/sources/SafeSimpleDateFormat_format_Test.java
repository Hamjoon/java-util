package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.text.DateFormat;

class SafeSimpleDateFormat_format_Test {

    private static final String PATTERN = "yyyy-MM-dd HH:mm:ss";

    private SafeSimpleDateFormat safeSdf;

    private Date epoch;

    private FieldPosition fieldPosition;

    @BeforeEach
    void setupBeforeEach() {
        // The date representing the Unix epoch (1970‑01‑01T00:00:00Z)
        epoch = new Date(0L);
        // SafeSimpleDateFormat instance under test
        safeSdf = new SafeSimpleDateFormat(PATTERN);
        // FieldPosition is required by the format method; 0 is a harmless value
        fieldPosition = new FieldPosition(0);
    }

    /**
     * Verifies that {@code SafeSimpleDateFormat.format(Date, StringBuffer, FieldPosition)}
     * produces the same textual representation as a regular {@link SimpleDateFormat}
     * configured with the same pattern.
     */
    @Test
    void format_ShouldMatchSimpleDateFormatOutput() {
        // GIVEN – a fresh StringBuffer to receive the formatted text
        StringBuffer buffer = new StringBuffer();
        // WHEN – the SafeSimpleDateFormat formats the epoch date
        StringBuffer returned = safeSdf.format(epoch, buffer, fieldPosition);
        // THEN – the returned buffer is the same instance that was supplied
        assertSame(buffer, returned, "The format method should return the supplied StringBuffer");
        // AND – the content matches a plain SimpleDateFormat with the same pattern
        SimpleDateFormat plain = new SimpleDateFormat(PATTERN);
        String expected = plain.format(epoch);
        assertEquals(expected, buffer.toString(), "SafeSimpleDateFormat should produce the same output as SimpleDateFormat");
    }

    /**
     * Verifies that formatting appends to an existing buffer rather than overwriting it.
     */
    @Test
    void format_ShouldAppendToExistingBuffer() {
        // GIVEN – a buffer that already contains some text
        StringBuffer buffer = new StringBuffer("prefix-");
        // WHEN – format is invoked
        safeSdf.format(epoch, buffer, fieldPosition);
        // THEN – the original content is preserved and the formatted date is appended
        SimpleDateFormat plain = new SimpleDateFormat(PATTERN);
        String expected = "prefix-" + plain.format(epoch);
        assertEquals(expected, buffer.toString(), "The formatted date should be appended to the existing buffer content");
    }

    /**
     * Verifies that a {@code null} date argument results in a {@link NullPointerException},
     * mirroring the behaviour of {@link SimpleDateFormat}.
     */
    @Test
    void format_NullDate_ShouldThrowNPE() {
        StringBuffer buffer = new StringBuffer();
        assertThrows(NullPointerException.class, () -> safeSdf.format(null, buffer, fieldPosition), "Formatting a null Date should throw NullPointerException");
    }

    @Test
    void format_NullStringBuffer_ShouldThrowNPE() {
        // Null StringBuffer is passed directly – SimpleDateFormat.format will NPE
        assertThrows(NullPointerException.class, () -> safeSdf.format(epoch, null, fieldPosition), "Formatting with a null StringBuffer should throw NullPointerException");
    }

    @Test
    void format_NullFieldPosition_ShouldFormatSuccessfully() {
        StringBuffer buffer = new StringBuffer();
        // SimpleDateFormat permits a null FieldPosition
        assertDoesNotThrow(() -> safeSdf.format(epoch, buffer, null), "Formatting with a null FieldPosition must not throw");
        // Verify the textual result matches a plain SimpleDateFormat
        SimpleDateFormat plain = new SimpleDateFormat(PATTERN);
        assertEquals(plain.format(epoch), buffer.toString(), "Output must be identical to SimpleDateFormat when FieldPosition is null");
    }

    @Test
    void format_YearFieldPosition_ShouldPopulateBeginIndex() {
        FieldPosition yearPos = new FieldPosition(DateFormat.YEAR_FIELD);
        StringBuffer buffer = new StringBuffer();
        safeSdf.format(epoch, buffer, yearPos);
        // For the default pattern the year starts at index 0 and spans 4 characters
        assertEquals(0, yearPos.getBeginIndex(), "Year field begin index should be 0");
        assertEquals(4, yearPos.getEndIndex(), "Year field end index should be 4");
    }

    @Test
    void format_InvalidFieldIndex_ShouldLeaveFieldPositionUnchanged() {
        FieldPosition invalidPos = new FieldPosition(-1);
        StringBuffer buffer = new StringBuffer();
        safeSdf.format(epoch, buffer, invalidPos);
        // SimpleDateFormat ignores invalid field indexes – they remain unchanged
        assertEquals(-1, invalidPos.getBeginIndex(), "Begin index should stay -1 for an invalid field index");
        assertEquals(-1, invalidPos.getEndIndex(), "End index should stay -1 for an invalid field index");
    }

    @Test
    void format_WithInvalidPattern_ShouldPropagateIllegalArgumentException() {
        // Pattern that SimpleDateFormat cannot parse
        SafeSimpleDateFormat badPatternSdf = new SafeSimpleDateFormat("yyyy-??-dd");
        StringBuffer buffer = new StringBuffer();
        assertThrows(IllegalArgumentException.class, () -> badPatternSdf.format(new Date(), buffer, new FieldPosition(0)), "An illegal pattern must cause IllegalArgumentException to propagate");
    }

    @Test
    void format_WithNullPattern_ShouldPropagateNullPointerException() throws Exception {
        // Create a normal instance then use reflection to inject a null pattern.
        SafeSimpleDateFormat nullPatternSdf = new SafeSimpleDateFormat(PATTERN);
        Field fmtField = SafeSimpleDateFormat.class.getDeclaredField("_format");
        fmtField.setAccessible(true);
        // force _format = null
        fmtField.set(nullPatternSdf, null);
        StringBuffer buffer = new StringBuffer();
        assertThrows(NullPointerException.class, () -> nullPatternSdf.format(new Date(), buffer, new FieldPosition(0)), "A null pattern should cause NullPointerException to propagate");
    }
}
