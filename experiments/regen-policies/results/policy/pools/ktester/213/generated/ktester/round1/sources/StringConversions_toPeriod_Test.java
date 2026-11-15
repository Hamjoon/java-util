package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.Period;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringConversions_toPeriod_Test {

    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The toPeriod method does not actually use the Converter, but we provide a real instance
        // to satisfy the method signature.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release static resources if any (none for this test).
        converter = null;
    }

    private static Stream<Arguments> validPeriodProvider() {
        return Stream.of(// standard multi‑component period
        Arguments.of("P2Y3M5D", Period.of(2, 3, 5)), // zero length period
        Arguments.of("P0D", Period.ZERO), // year‑only
        Arguments.of("P5Y", Period.ofYears(5)), // month‑only
        Arguments.of("P6M", Period.ofMonths(6)), // day‑only
        Arguments.of("P7D", Period.ofDays(7)), // explicit zero components
        Arguments.of("P0Y0M5D", Period.of(0, 0, 5)), // negative period (ISO‑8601 allows a leading minus)
        Arguments.of("-P1Y2M3D", Period.of(-1, -2, -3)), // very large year value (within int range of Period)
        Arguments.of("P999999999Y", Period.ofYears(999999999)));
    }

    private static Stream<Arguments> illegalArgumentProvider() {
        return Stream.of(// malformed period string
        Arguments.of("invalid"), // null input – leads to NullPointerException inside Period.parse, wrapped
        Arguments.of(null), // lower‑case designator (Period.parse is case‑sensitive)
        Arguments.of("p2y3m5d"), // time component present – Period does not support time units
        Arguments.of("PT5H"), // leading/trailing whitespace – not accepted by Period.parse
        Arguments.of(" P1Y "), // explicit plus sign – not part of the accepted format
        Arguments.of("P+1Y"), // empty string
        Arguments.of(""), // only the designator without numbers
        Arguments.of("P"), // year value exceeds int range (overflow)
        Arguments.of("P2147483648Y"), // negative number placed after the designator (invalid format)
        Arguments.of("P-1Y"), // trailing numeric characters without a designator
        Arguments.of("P1Y2M3D4"));
    }

    @Test
    void toPeriod_ValidIso8601String_ReturnsExpectedPeriod() {
        // given
        // 2 years, 3 months, 5 days
        String isoPeriod = "P2Y3M5D";
        Period expected = Period.of(2, 3, 5);
        // when
        Period actual = StringConversions.toPeriod(isoPeriod, converter);
        // then
        assertEquals(expected, actual, "The parsed Period should match the expected value.");
    }

    @Test
    void toPeriod_InvalidString_ThrowsIllegalArgumentException() {
        // given
        String badPeriod = "not-a-period";
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toPeriod(badPeriod, converter));
        assertTrue(ex.getMessage().contains("Unable to parse"), "Exception message should indicate a parsing problem.");
    }

    @Test
    void toPeriod_NullInput_ThrowsIllegalArgumentException() {
        // given
        Object nullInput = null;
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toPeriod(nullInput, converter));
        assertTrue(ex.getMessage().contains("Unable to parse"), "Null input should be wrapped in an IllegalArgumentException.");
    }

    /* --------------------------------------------------------------
     *  Valid period strings – expected to return a Period instance.
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] toPeriod(''{0}'') => {1}")
    @MethodSource("validPeriodProvider")
    void toPeriod_ValidInput_ReturnsExpectedPeriod(Object input, Period expected) {
        // when
        Period actual = StringConversions.toPeriod(input, null);
        // then
        assertEquals(expected, actual, () -> "Parsed Period does not match expected for input: " + input);
    }

    /* --------------------------------------------------------------
     *  Invalid inputs – expected to throw IllegalArgumentException.
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] toPeriod(''{0}'') throws IllegalArgumentException")
    @MethodSource("illegalArgumentProvider")
    void toPeriod_InvalidInput_ThrowsIllegalArgumentException(Object input) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toPeriod(input, null));
        assertTrue(ex.getMessage().contains("Unable to parse"), () -> "Exception message should contain 'Unable to parse' for input: " + input);
    }

    /* --------------------------------------------------------------
     *  Inputs that cause a ClassCastException before parsing.
     * -------------------------------------------------------------- */
    @Test
    void toPeriod_NonStringObject_ThrowsClassCastException() {
        // a CharSequence that is not a String – the direct cast fails
        Object nonString = new StringBuilder("P2Y3M5D");
        assertThrows(ClassCastException.class, () -> StringConversions.toPeriod(nonString, null), "Casting a non‑String object should raise ClassCastException.");
    }

    /* --------------------------------------------------------------
     *  Additional explicit test for null Converter (method ignores it).
     * -------------------------------------------------------------- */
    @Test
    void toPeriod_ValidInput_WithNullConverter_ReturnsPeriod() {
        String iso = "P1Y2M3D";
        Period expected = Period.of(1, 2, 3);
        Period actual = StringConversions.toPeriod(iso, null);
        assertEquals(expected, actual, "Method should work even when Converter argument is null.");
    }
}
