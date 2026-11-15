package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

class SafeSimpleDateFormat_toString_Test {

    /**
     * Provides a stream of test arguments: format string, expected result,
     * and a description used only for readability in the test name.
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> toStringTestData() {
        return Stream.of(// basic behavior group
        org.junit.jupiter.params.provider.Arguments.of("yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm:ss", "Typical date‑time pattern"), org.junit.jupiter.params.provider.Arguments.of("", "", "Empty format string"), org.junit.jupiter.params.provider.Arguments.of("yyyy/MM/dd'T'HH:mm:ss.SSSZ", "yyyy/MM/dd'T'HH:mm:ss.SSSZ", "Pattern with quotes and timezone literal"), org.junit.jupiter.params.provider.Arguments.of("   ", "   ", "Whitespace‑only format"), // additional scenarios group
        org.junit.jupiter.params.provider.Arguments.of("yyyy-MM-dd 'at' HH:mm:ss", "yyyy-MM-dd 'at' HH:mm:ss", "Quoted literal word \"at\""), org.junit.jupiter.params.provider.Arguments.of("yyyy-MM-dd ☃", "yyyy-MM-dd ☃", "Unicode snowman character"), org.junit.jupiter.params.provider.Arguments.of("yyyy-MM-dd\n\tHH:mm", "yyyy-MM-dd\n\tHH:mm", "Newline and tab control characters"), org.junit.jupiter.params.provider.Arguments.of("yyyyMMddHHmmssSSSyyyyMMddHHmmssSSSyyyyMMddHHmmssSSS", "yyyyMMddHHmmssSSSyyyyMMddHHmmssSSSyyyyMMddHHmmssSSS", "Very long pattern (>30 chars)"), org.junit.jupiter.params.provider.Arguments.of("yyyy-MM-dd 🚀", "yyyy-MM-dd 🚀", "Emoji (surrogate pair) in pattern"), org.junit.jupiter.params.provider.Arguments.of("dd/MM/yyyy", "dd/MM/yyyy", "Common European date pattern"), org.junit.jupiter.params.provider.Arguments.of("yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm:ss", "Identity test – same instance as constructor argument"));
    }

    @Test
    void toStringReturnsExactFormatString() {
        // given
        String format = "yyyy/MM/dd HH:mm:ss";
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(format);
        // when
        String result = sdf.toString();
        // then
        assertEquals(format, result, "SafeSimpleDateFormat.toString() should return the format string supplied to the constructor");
    }

    @Test
    void toStringHandlesEmptyFormat() {
        // given
        String format = "";
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(format);
        // when
        String result = sdf.toString();
        // then
        assertEquals(format, result, "SafeSimpleDateFormat.toString() should correctly handle an empty format string");
    }

    @Test
    void toStringThrowsNPEWhenFormatIsNull() {
        // given
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(null);
        // when / then
        assertThrows(NullPointerException.class, sdf::toString, "Calling toString() on a SafeSimpleDateFormat created with a null format should throw NPE");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("toStringTestData")
    void toStringVariousPatterns(String format, String expected, String description) {
        // given
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(format);
        // when
        String result = sdf.toString();
        // then
        assertEquals(expected, result, () -> "toString() should return the exact pattern for: " + description);
        // Additional check for the identity case (last test in the data set)
        if (description.contains("Identity")) {
            // The internal field is final and holds the reference passed to the constructor,
            // therefore the returned String should be the same object.
            assertSame(format, result, "Returned String should be the same instance as the constructor argument");
        }
    }

    @Test
    void toStringThrowsNPEWhenConstructedWithNull() {
        // given
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(null);
        // when / then
        assertThrows(NullPointerException.class, sdf::toString, "Expected NullPointerException when _format is null");
    }
}
