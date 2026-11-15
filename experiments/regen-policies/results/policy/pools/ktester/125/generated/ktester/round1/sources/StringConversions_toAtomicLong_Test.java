package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.any;

class StringConversions_toAtomicLong_Test {

    private Converter converter;

    /* -------------------------------------------------------------
     *  Helper class to bundle test arguments cleanly.
     * ------------------------------------------------------------- */
    private static class ArgumentsHolder {

        final Object source;

        final AtomicLong expected;

        final String description;

        ArgumentsHolder(Object source, AtomicLong expected, String description) {
            this.source = source;
            this.expected = expected;
            this.description = description;
        }

        // Used by @MethodSource – JUnit expects a single argument, so we expose getters.
        Object getSource() {
            return source;
        }

        AtomicLong getExpected() {
            return expected;
        }

        String getDescription() {
            return description;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use default options – the Converter works out‑of‑the‑box for the
        // primitive and wrapper conversions we need in the tests.
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up per‑test
        converter = null;
    }

    // ---------- Numeric inputs – direct Number handling ----------
    static Stream<ArgumentsHolder> numericNumberProvider() {
        return Stream.of(new ArgumentsHolder(123L, new AtomicLong(123L), "Long object"), new ArgumentsHolder(42, new AtomicLong(42L), "Integer object"), new ArgumentsHolder(3.14f, new AtomicLong(3L), "Float truncates"));
    }

    // ---------- String inputs – numeric strings ----------
    static Stream<ArgumentsHolder> numericStringProvider() {
        return Stream.of(new ArgumentsHolder("9876543210", new AtomicLong(9876543210L), "Simple numeric string"), new ArgumentsHolder("   -15  ", new AtomicLong(-15L), "Whitespace and negative sign"));
    }

    // ---------- String inputs – non‑numeric strings ----------
    static Stream<Object> nonNumericStringProvider() {
        return Stream.of("notANumber", "123abc");
    }

    // ---------- Boolean inputs ----------
    static Stream<ArgumentsHolder> booleanProvider() {
        return Stream.of(new ArgumentsHolder(true, new AtomicLong(1L), "Boolean true → 1"), new ArgumentsHolder(false, new AtomicLong(0L), "Boolean false → 0"));
    }

    // ---------- Other numeric wrapper types ----------
    static Stream<ArgumentsHolder> otherWrappersProvider() {
        return Stream.of(new ArgumentsHolder(Byte.valueOf((byte) 5), new AtomicLong(5L), "Byte wrapper"), new ArgumentsHolder(Short.valueOf((short) 12), new AtomicLong(12L), "Short wrapper"), new ArgumentsHolder(new BigInteger("123456789012345"), new AtomicLong(123456789012345L), "BigInteger"), new ArgumentsHolder(new BigDecimal("98765.4321"), new AtomicLong(98765L), "BigDecimal truncates"));
    }

    // ---------- Date and time inputs ----------
    static Stream<ArgumentsHolder> dateTimeProvider() {
        return Stream.of(new ArgumentsHolder(new Date(1622548800000L), new AtomicLong(1622548800000L), "java.util.Date"), new ArgumentsHolder(createUtcCalendar(1622548800000L), new AtomicLong(1622548800000L), "Calendar UTC"), new ArgumentsHolder(Instant.ofEpochMilli(123456789L), new AtomicLong(123456789L), "Instant"));
    }

    private static Calendar createUtcCalendar(long millis) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(millis);
        return cal;
    }

    // ---------- String edge cases – sign and padding ----------
    static Stream<ArgumentsHolder> stringEdgeProvider() {
        return Stream.of(new ArgumentsHolder("+42", new AtomicLong(42L), "Explicit plus sign"), new ArgumentsHolder("00000123", new AtomicLong(123L), "Leading zeros"), new ArgumentsHolder("9223372036854775807", new AtomicLong(Long.MAX_VALUE), "Long.MAX_VALUE"), new ArgumentsHolder("9223372036854775808", null, "Overflow → NumberFormatException"));
    }

    @Test
    void toAtomicLongFromStringNumber() {
        Object source = "12345";
        AtomicLong result = StringConversions.toAtomicLong(source, converter);
        assertNotNull(result);
        assertEquals(12345L, result.get());
    }

    @Test
    void toAtomicLongFromLong() {
        Object source = 98765L;
        AtomicLong result = StringConversions.toAtomicLong(source, converter);
        assertNotNull(result);
        assertEquals(98765L, result.get());
    }

    @Test
    void toAtomicLongFromInteger() {
        Object source = 42;
        AtomicLong result = StringConversions.toAtomicLong(source, converter);
        assertNotNull(result);
        assertEquals(42L, result.get());
    }

    @Test
    void toAtomicLongFromNull() {
        Object source = null;
        AtomicLong result = StringConversions.toAtomicLong(source, converter);
        assertNotNull(result);
        // According to Converter contract, null converts to 0 for numeric primitives
        assertEquals(0L, result.get());
    }

    @Test
    void toAtomicLongFromInvalidStringThrowsException() {
        Object source = "not-a-number";
        assertThrows(NumberFormatException.class, () -> {
            StringConversions.toAtomicLong(source, converter);
        });
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("numericNumberProvider")
    void toAtomicLong_NumericNumbers(Object source, AtomicLong expected, String description) {
        AtomicLong result = StringConversions.toAtomicLong(source, converter);
        assertNotNull(result);
        assertEquals(expected.get(), result.get(), description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("numericStringProvider")
    void toAtomicLong_NumericStrings(Object source, AtomicLong expected, String description) {
        AtomicLong result = StringConversions.toAtomicLong(source, converter);
        assertNotNull(result);
        assertEquals(expected.get(), result.get(), description);
    }

    @ParameterizedTest(name = "[{index}] Non‑numeric string ''{0}'' throws")
    @MethodSource("nonNumericStringProvider")
    void toAtomicLong_NonNumericStringsThrows(String source) {
        assertThrows(NumberFormatException.class, () -> StringConversions.toAtomicLong(source, converter), "Expected NumberFormatException for source: " + source);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("booleanProvider")
    void toAtomicLong_Boolean(Object source, AtomicLong expected, String description) {
        AtomicLong result = StringConversions.toAtomicLong(source, converter);
        assertNotNull(result);
        assertEquals(expected.get(), result.get(), description);
    }

    // ---------- Null input handling ----------
    @Test
    void toAtomicLong_NullInput_ThrowsNullPointerException() {
        // According to the JSON spec, toLong does not accept null and should NPE.
        assertThrows(NullPointerException.class, () -> StringConversions.toAtomicLong(null, converter));
    }

    // ---------- Custom Converter usage – fallback path ----------
    @Test
    void toAtomicLong_CustomConverterReturnsValue() {
        Converter mock = Mockito.mock(Converter.class);
        Mockito.when(mock.convert(any(), Mockito.eq(Long.class))).thenReturn(777L);
        Object source = "custom";
        AtomicLong result = StringConversions.toAtomicLong(source, mock);
        assertNotNull(result);
        assertEquals(777L, result.get());
    }

    // ---------- Range‑checked String conversion (private overload) ----------
    @Test
    void toAtomicLong_RangeCheckedStringWithinBounds() {
        // The private overload is exercised indirectly via toLong(String).
        // "50" should succeed.
        AtomicLong result = StringConversions.toAtomicLong("50", converter);
        assertNotNull(result);
        assertEquals(50L, result.get());
    }

    @Test
    void toAtomicLong_RangeCheckedStringBelowLowBound_ThrowsNumberFormatException() {
        // Assuming the implementation enforces a low bound of 0 (as hinted).
        assertThrows(NumberFormatException.class, () -> StringConversions.toAtomicLong("-1000", converter));
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("otherWrappersProvider")
    void toAtomicLong_OtherWrappers(Object source, AtomicLong expected, String description) {
        AtomicLong result = StringConversions.toAtomicLong(source, converter);
        assertNotNull(result);
        assertEquals(expected.get(), result.get(), description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("dateTimeProvider")
    void toAtomicLong_DateTimeTypes(Object source, AtomicLong expected, String description) {
        AtomicLong result = StringConversions.toAtomicLong(source, converter);
        assertNotNull(result);
        assertEquals(expected.get(), result.get(), description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("stringEdgeProvider")
    void toAtomicLong_StringEdgeCases(String source, AtomicLong expected, String description) {
        if (expected == null) {
            assertThrows(NumberFormatException.class, () -> StringConversions.toAtomicLong(source, converter), "Expected overflow to throw NumberFormatException");
        } else {
            AtomicLong result = StringConversions.toAtomicLong(source, converter);
            assertNotNull(result);
            assertEquals(expected.get(), result.get(), description);
        }
    }

    // ---------- Converter edge cases ----------
    @Test
    void toAtomicLong_ConverterReturnsNull_ThrowsNullPointerException() {
        Converter mock = Mockito.mock(Converter.class);
        Mockito.when(mock.convert(any(), Mockito.eq(Long.class))).thenReturn(null);
        assertThrows(NullPointerException.class, () -> StringConversions.toAtomicLong("anyString", mock));
    }

    @Test
    void toAtomicLong_ConverterThrowsIllegalArgumentException_Propagates() {
        Converter mock = Mockito.mock(Converter.class);
        Mockito.when(mock.convert(any(), Mockito.eq(Long.class))).thenThrow(new IllegalArgumentException("converter failure"));
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toAtomicLong("trigger", mock));
    }

    // ---------- AtomicLong as source ----------
    @Test
    void toAtomicLong_AtomicLongSource() {
        AtomicLong source = new AtomicLong(77L);
        AtomicLong result = StringConversions.toAtomicLong(source, converter);
        assertNotNull(result);
        assertEquals(77L, result.get());
    }

    // ---------- CharSequence types other than String ----------
    @Test
    void toAtomicLong_CharBufferSource() {
        CharBuffer cb = CharBuffer.wrap("256");
        AtomicLong result = StringConversions.toAtomicLong(cb, converter);
        assertNotNull(result);
        assertEquals(256L, result.get());
    }

    // ---------- Unsupported source type without converter ----------
    @Test
    void toAtomicLong_UnsupportedSource_ThrowsIllegalArgumentException() {
        Object source = new Object();
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toAtomicLong(source, converter));
    }
}
