package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.TimeZone;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringConversions_toTimeZone_Test {

    @Mock
    Converter // not used by the method but required for the signature
    converter;

    static Stream<Arguments> nullAndEmptyProvider() {
        return Stream.of(Arguments.of(null), Arguments.of(""), Arguments.of("   "));
    }

    static Stream<Arguments> validIdProvider() {
        return Stream.of(Arguments.of("UTC", "UTC"), Arguments.of("America/New_York", "America/New_York"), Arguments.of("  GMT  ", "GMT"));
    }

    static Stream<Arguments> invalidIdProvider() {
        return Stream.of(Arguments.of("Invalid/Zone"), Arguments.of("XYZ"));
    }

    static Stream<Arguments> nonStringProvider() {
        return Stream.of(// Integer
        Arguments.of(123), // generic Object
        Arguments.of(new Object()));
    }

    static Stream<Arguments> offsetIdProvider() {
        return Stream.of(Arguments.of("GMT+02:00", "GMT+02:00"), Arguments.of("GMT-05:00", "GMT-05:00"), Arguments.of("  GMT+09:30  ", "GMT+09:30"));
    }

    static Stream<Arguments> threeLetterProvider() {
        return Stream.of(Arguments.of("EST"), Arguments.of("PST"));
    }

    @Test
    void toTimeZone_nullInput_returnsNull() {
        TimeZone tz = StringConversions.toTimeZone(null, converter);
        assertNull(tz, "Null source should result in null TimeZone");
    }

    @Test
    void toTimeZone_blankString_returnsNull() {
        TimeZone tz = StringConversions.toTimeZone("   ", converter);
        assertNull(tz, "Blank (or whitespace‑only) string should result in null TimeZone");
    }

    @Test
    void toTimeZone_validId_returnsCorrespondingTimeZone() {
        TimeZone tz = StringConversions.toTimeZone("America/New_York", converter);
        assertNotNull(tz, "Valid zone ID must not return null");
        assertEquals("America/New_York", tz.getID(), "Returned TimeZone should have the requested ID");
    }

    @Test
    void toTimeZone_validIdWithWhitespace_isTrimmed() {
        TimeZone tz = StringConversions.toTimeZone("  UTC  ", converter);
        assertNotNull(tz, "Whitespace‑padded valid ID must not return null");
        assertEquals("UTC", tz.getID(), "Input should be trimmed before conversion");
    }

    @Test
    void toTimeZone_invalidId_returnsGMT() {
        // According to java.util.TimeZone#getTimeZone, an unknown ID falls back to GMT
        TimeZone tz = StringConversions.toTimeZone("Invalid/Zone", converter);
        assertNotNull(tz, "Even an invalid ID should return a non‑null TimeZone");
        assertEquals("GMT", tz.getID(), "Invalid IDs are mapped to GMT by TimeZone.getTimeZone");
    }

    // ---------- Null and empty handling ----------
    @ParameterizedTest(name = "[{index}] from=''{0}'' → expected null")
    @MethodSource("nullAndEmptyProvider")
    void toTimeZone_nullAndEmpty_handling(Object from) {
        TimeZone tz = StringConversions.toTimeZone(from, converter);
        assertNull(tz, "Input should be trimmed to null and result in null TimeZone");
    }

    // ---------- Valid timezone IDs ----------
    @ParameterizedTest(name = "[{index}] from=''{0}'' → expected ID=''{1}''")
    @MethodSource("validIdProvider")
    void toTimeZone_validIds(String from, String expectedId) {
        TimeZone tz = StringConversions.toTimeZone(from, converter);
        assertNotNull(tz, "Valid ID must not produce null");
        assertEquals(expectedId, tz.getID(), "TimeZone ID should match expected");
    }

    // ---------- Invalid timezone IDs ----------
    @ParameterizedTest(name = "[{index}] from=''{0}'' → fallback to GMT")
    @MethodSource("invalidIdProvider")
    void toTimeZone_invalidIds(String from) {
        TimeZone tz = StringConversions.toTimeZone(from, converter);
        assertNotNull(tz, "Even an invalid ID should return a non‑null TimeZone");
        assertEquals("GMT", tz.getID(), "Invalid IDs must fall back to GMT");
    }

    // ---------- Non‑string input ----------
    @ParameterizedTest(name = "[{index}] from={0} (type {1}) → ClassCastException")
    @MethodSource("nonStringProvider")
    void toTimeZone_nonStringInput(Object from) {
        assertThrows(ClassCastException.class, () -> StringConversions.toTimeZone(from, converter), "Passing a non‑String should cause ClassCastException");
    }

    // ---------- Converter argument ignored ----------
    @Test
    void toTimeZone_converterArgumentIgnored() {
        // The mock converter is already injected; we just verify the result is unchanged.
        TimeZone tz = StringConversions.toTimeZone("UTC", converter);
        assertNotNull(tz);
        assertEquals("UTC", tz.getID());
    }

    // ---------- Offset timezone IDs ----------
    @ParameterizedTest(name = "[{index}] from=''{0}'' → expected ID=''{1}''")
    @MethodSource("offsetIdProvider")
    void toTimeZone_offsetIds(String from, String expectedId) {
        TimeZone tz = StringConversions.toTimeZone(from, converter);
        assertNotNull(tz);
        assertEquals(expectedId, tz.getID());
    }

    // ---------- Three‑letter timezone IDs ----------
    @ParameterizedTest(name = "[{index}] from=''{0}'' → expected ID=''{0}''")
    @MethodSource("threeLetterProvider")
    void toTimeZone_threeLetterIds(String from) {
        TimeZone tz = StringConversions.toTimeZone(from, converter);
        assertNotNull(tz);
        assertEquals(from, tz.getID());
    }

    // ---------- Case sensitivity ----------
    @Test
    void toTimeZone_caseSensitivity_lowercaseFallsBackToGMT() {
        TimeZone tz = StringConversions.toTimeZone("utc", converter);
        assertNotNull(tz);
        assertEquals("GMT", tz.getID(), "Lower‑case IDs are not recognised and map to GMT");
    }

    // ---------- Long unknown ID fallback ----------
    @Test
    void toTimeZone_longUnknownId_fallbackToGMT() {
        String longId = "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX";
        TimeZone tz = StringConversions.toTimeZone(longId, converter);
        assertNotNull(tz);
        assertEquals("GMT", tz.getID(), "Very long unknown IDs should map to GMT");
    }

    // ---------- Unicode whitespace trimming ----------
    @Test
    void toTimeZone_unicodeWhitespace_notTrimmed_fallbackToGMT() {
        // EM SPACE (U+2003) is not considered whitespace by StringUtilities.trimToNull
        String input = "\u2003GMT\u2003";
        TimeZone tz = StringConversions.toTimeZone(input, converter);
        assertNotNull(tz);
        assertEquals("GMT", tz.getID(), "Untrimmed Unicode whitespace leads to unknown ID → GMT");
    }

    // ---------- Newline and tab whitespace ----------
    @Test
    void toTimeZone_newlineAndTabWhitespace_isTrimmed() {
        String input = "\t\n GMT \n\t";
        TimeZone tz = StringConversions.toTimeZone(input, converter);
        assertNotNull(tz);
        assertEquals("GMT", tz.getID(), "Leading/trailing newline/tab characters should be trimmed");
    }
}
