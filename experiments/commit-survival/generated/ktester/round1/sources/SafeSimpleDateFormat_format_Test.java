package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.text.*;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SafeSimpleDateFormat_format_Test {

    /**
     * Instance under test – created with a known pattern.
     */
    private SafeSimpleDateFormat safeSdf;

    /**
     * A deterministic date used by all tests.
     */
    private Date fixedDate;

    /**
     * Custom StringBuffer that throws on any append operation.
     */
    private static class ThrowingStringBuffer extends StringBuffer {

        @Override
        public StringBuffer append(String str) {
            throw new RuntimeException("append failed");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Pattern: ISO‑like date without time.
        safeSdf = new SafeSimpleDateFormat("yyyy-MM-dd");
        // Create a fixed date: 2023‑03‑15.
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2023, Calendar.MARCH, 15);
        fixedDate = cal.getTime();
    }

    /**
     * Helper to build a GregorianCalendar Date.
     */
    private static Date gregorian(int year, int month, int day) {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(year, month, day);
        return cal.getTime();
    }

    /**
     * Helper to build a GregorianCalendar Date with time.
     */
    private static Date gregorian(int year, int month, int day, int hour, int minute, int second) {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(year, month, day, hour, minute, second);
        return cal.getTime();
    }

    static Stream<Arguments> standardFormattingProvider() {
        return Stream.of(Arguments.of("Epoch date with YEAR_FIELD", new Date(0L), new StringBuffer(), new FieldPosition(DateFormat.YEAR_FIELD), "1970-01-01"), Arguments.of("2023-03-15 with existing buffer and HOUR_OF_DAY0_FIELD", gregorian(2023, Calendar.MARCH, 15, 13, 45, 30), new StringBuffer("Start:"), new FieldPosition(DateFormat.HOUR_OF_DAY0_FIELD), "Start:2023-03-15"));
    }

    @Test
    void format_returnsFormattedString() {
        // GIVEN
        StringBuffer sb = new StringBuffer();
        FieldPosition fp = new FieldPosition(0);
        // WHEN
        StringBuffer result = safeSdf.format(fixedDate, sb, fp);
        // THEN
        assertSame(sb, result, "The method should return the same StringBuffer that was passed in");
        assertEquals("2023-03-15", sb.toString(), "The date should be formatted according to the pattern");
    }

    @Test
    void format_appendsToExistingBuffer() {
        // GIVEN
        StringBuffer sb = new StringBuffer("Prefix-");
        FieldPosition fp = new FieldPosition(0);
        // WHEN
        safeSdf.format(fixedDate, sb, fp);
        // THEN
        assertEquals("Prefix-2023-03-15", sb.toString(), "The formatted date should be appended to the existing buffer contents");
    }

    @Test
    void format_doesNotAlterFieldPosition() {
        // GIVEN
        FieldPosition fp = new FieldPosition(0);
        int originalBeginIndex = fp.getBeginIndex();
        int originalEndIndex = fp.getEndIndex();
        // WHEN
        safeSdf.format(fixedDate, new StringBuffer(), fp);
        // THEN
        assertEquals(originalBeginIndex, fp.getBeginIndex(), "FieldPosition begin index should remain unchanged");
        assertEquals(originalEndIndex, fp.getEndIndex(), "FieldPosition end index should remain unchanged");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("standardFormattingProvider")
    void format_standardFormatting(String description, Date date, StringBuffer sb, FieldPosition fp, String expected) {
        // Use the default safeSdf (pattern "yyyy-MM-dd")
        safeSdf.format(date, sb, fp);
        assertEquals(expected, sb.toString());
    }

    @Test
    void format_nullFieldPosition_allowed() {
        Date date = gregorian(2022, Calendar.DECEMBER, 31);
        StringBuffer sb = new StringBuffer();
        // Passing null should be forwarded unchanged.
        safeSdf.format(date, sb, null);
        assertEquals("2022-12-31", sb.toString());
    }

    @Test
    void format_nullStringBuffer_throwsNPE() {
        Date now = new Date();
        FieldPosition fp = new FieldPosition(DateFormat.YEAR_FIELD);
        assertThrows(NullPointerException.class, () -> safeSdf.format(now, null, fp), "Underlying SimpleDateFormat.format should throw NPE for null StringBuffer");
    }

    @Test
    void format_nullDate_throwsNPE() {
        StringBuffer sb = new StringBuffer();
        FieldPosition fp = new FieldPosition(DateFormat.MONTH_FIELD);
        assertThrows(NullPointerException.class, () -> safeSdf.format(null, sb, fp), "Underlying SimpleDateFormat.format should throw NPE for null Date");
    }

    @Test
    void format_differentPattern_firstCall_createsNewSimpleDateFormat() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = gregorian(2021, Calendar.JULY, 4, 9, 30, 0);
        StringBuffer sb = new StringBuffer();
        sdf.format(date, sb, new FieldPosition(DateFormat.HOUR_OF_DAY0_FIELD));
        assertEquals("04/07/2021 09:30:00", sb.toString());
    }

    @Test
    void format_differentPattern_secondCall_hitsCache() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = gregorian(2021, Calendar.JULY, 4, 9, 30, 0);
        StringBuffer sb1 = new StringBuffer();
        StringBuffer sb2 = new StringBuffer();
        // First call – creates and caches the SimpleDateFormat
        sdf.format(date, sb1, new FieldPosition(DateFormat.HOUR_OF_DAY0_FIELD));
        // Second call – should reuse the cached instance
        sdf.format(date, sb2, new FieldPosition(DateFormat.HOUR_OF_DAY0_FIELD));
        assertEquals("04/07/2021 09:30:00", sb1.toString());
        assertEquals("04/07/2021 09:30:00", sb2.toString());
    }

    @Test
    void format_fieldPositionYearField_updatesIndexes() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("yyyy-MM-dd");
        Date date = gregorian(2022, Calendar.MAY, 10);
        FieldPosition fp = new FieldPosition(DateFormat.YEAR_FIELD);
        StringBuffer sb = new StringBuffer();
        sdf.format(date, sb, fp);
        assertEquals(0, fp.getBeginIndex(), "Year should start at index 0");
        assertEquals(4, fp.getEndIndex(), "Year should end at index 4");
        assertEquals("2022-05-10", sb.toString());
    }

    @Test
    void format_negativeEpochDate() {
        // one day before epoch
        Date date = new Date(-86_400_000L);
        StringBuffer sb = new StringBuffer();
        safeSdf.format(date, sb, new FieldPosition(DateFormat.DAY_OF_MONTH_FIELD));
        assertEquals("1969-12-31", sb.toString());
    }

    @Test
    void format_withTimeZoneOffset() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("yyyy-MM-dd Z");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+02:00"));
        Date epoch = new Date(0L);
        StringBuffer sb = new StringBuffer();
        sdf.format(epoch, sb, null);
        assertEquals("1970-01-01 +0200", sb.toString(), "Formatted string should contain the +0200 offset");
    }

    @Test
    void format_reuseFieldPosition_multipleCalls() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("yyyy-MM-dd");
        FieldPosition fp = new FieldPosition(DateFormat.YEAR_FIELD);
        StringBuffer sb = new StringBuffer();
        // First call – with a different date (2020-01-01)
        sdf.format(gregorian(2020, Calendar.JANUARY, 1), sb, fp);
        // Reset buffer for clarity
        sb.setLength(0);
        // Second call – with the date under test (2021-12-31)
        sdf.format(gregorian(2021, Calendar.DECEMBER, 31), sb, fp);
        assertEquals(0, fp.getBeginIndex(), "Begin index should still be 0 after second call");
        assertEquals(4, fp.getEndIndex(), "End index should still be 4 after second call");
        assertEquals("2021-12-31", sb.toString());
    }

    @Test
    void format_patternWithLiteralText() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("'Year:' yyyy");
        Date date = gregorian(2023, Calendar.JANUARY, 1);
        StringBuffer sb = new StringBuffer();
        sdf.format(date, sb, null);
        assertEquals("Year: 2023", sb.toString());
    }

    @Test
    void format_nullPatternInConstructor_throwsNPE() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(null);
        Date now = new Date();
        StringBuffer sb = new StringBuffer();
        assertThrows(NullPointerException.class, () -> sdf.format(now, sb, new FieldPosition(DateFormat.YEAR_FIELD)), "Creating SimpleDateFormat with null pattern should throw NPE");
    }

    @Test
    void format_invalidPattern_throwsIllegalArgumentException() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("yyyy-MM-dd-@@");
        Date now = new Date();
        StringBuffer sb = new StringBuffer();
        assertThrows(IllegalArgumentException.class, () -> sdf.format(now, sb, new FieldPosition(DateFormat.MONTH_FIELD)), "Invalid pattern should cause IllegalArgumentException");
    }

    @Test
    void format_stringBufferThrowsRuntimeException_propagates() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("yyyy-MM-dd");
        Date epoch = new Date(0L);
        ThrowingStringBuffer sb = new ThrowingStringBuffer();
        FieldPosition fp = new FieldPosition(DateFormat.YEAR_FIELD);
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> sdf.format(epoch, sb, fp));
        assertEquals("append failed", thrown.getMessage());
    }
}
