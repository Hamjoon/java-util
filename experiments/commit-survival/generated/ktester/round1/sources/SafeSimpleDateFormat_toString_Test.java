package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;

class SafeSimpleDateFormat_toString_Test {

    private SafeSimpleDateFormat safeSimpleDateFormat;

    private String format;

    @BeforeEach
    void setupBeforeEach() {
        // The concrete format is set in each test method to keep tests independent.
        // Here we initialise the field to avoid a NullPointerException if a test
        // forgets to assign it.
        format = "yyyy/MM/dd";
        safeSimpleDateFormat = new SafeSimpleDateFormat(format);
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly release references – helps the GC in long test suites.
        safeSimpleDateFormat = null;
        format = null;
    }

    @Test
    @DisplayName("toString() returns the exact format string supplied to the constructor")
    void toStringReturnsSuppliedFormat() {
        // given – already set up in @BeforeEach
        // when
        String result = safeSimpleDateFormat.toString();
        // then
        assertEquals(format, result, "SafeSimpleDateFormat.toString() should return the format string passed to the constructor");
    }

    @Test
    @DisplayName("toString() works with an empty format string")
    void toStringWithEmptyFormat() {
        // given
        format = "";
        safeSimpleDateFormat = new SafeSimpleDateFormat(format);
        // when
        String result = safeSimpleDateFormat.toString();
        // then
        assertEquals("", result, "SafeSimpleDateFormat.toString() should correctly handle an empty format string");
    }

    @Test
    @DisplayName("toString() works with a whitespace‑only format string")
    void toStringWithWhitespaceFormat() {
        // given
        format = "   ";
        safeSimpleDateFormat = new SafeSimpleDateFormat(format);
        // when
        String result = safeSimpleDateFormat.toString();
        // then
        assertEquals("   ", result, "SafeSimpleDateFormat.toString() should preserve whitespace in the format string");
    }

    @Test
    @DisplayName("Multiple instances maintain their own format strings")
    void multipleInstancesMaintainSeparateFormats() {
        // given
        SafeSimpleDateFormat sdf1 = new SafeSimpleDateFormat("yyyy-MM-dd");
        SafeSimpleDateFormat sdf2 = new SafeSimpleDateFormat("dd/MM/yyyy");
        // when
        String result1 = sdf1.toString();
        String result2 = sdf2.toString();
        // then
        assertAll(() -> assertEquals("yyyy-MM-dd", result1, "First instance should retain its format"), () -> assertEquals("dd/MM/yyyy", result2, "Second instance should retain its format"));
    }

    @Test
    @DisplayName("toString() throws NullPointerException when internal _format is null (defensive test)")
    void toStringThrowsWhenInternalFormatIsNull() throws Exception {
        // given – construct a normal instance then corrupt its private field via reflection
        SafeSimpleDateFormat corrupted = new SafeSimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Field formatField = SafeSimpleDateFormat.class.getDeclaredField("_format");
        formatField.setAccessible(true);
        // force internal state to null
        formatField.set(corrupted, null);
        // when / then – calling toString() should result in NPE
        assertThrows(NullPointerException.class, corrupted::toString, "SafeSimpleDateFormat.toString() should throw NullPointerException when internal _format is null");
    }
}
