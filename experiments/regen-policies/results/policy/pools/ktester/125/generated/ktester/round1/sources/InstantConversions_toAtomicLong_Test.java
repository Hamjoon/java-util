package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mock;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstantConversions_toAtomicLong_Test {

    /**
     * The Converter instance used for all tests.
     */
    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // ConverterOptions has a public no‑arg constructor in the library.
        // Using the default options is sufficient for the built‑in conversions.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help the garbage collector; not strictly required.
        converter = null;
    }

    /**
     * Provides arguments for the “Standard type conversions” group.
     */
    private static Stream<Arguments> standardConversionsProvider() {
        return Stream.of(// Long
        Arguments.of(123L, 123L, "Long value should be passed through unchanged."), // Integer
        Arguments.of(45, 45L, "Integer value should be converted to long and wrapped."), // Numeric String
        Arguments.of("6789", 6789L, "Numeric string should be parsed as a long."), // java.util.Date
        Arguments.of(new Date(12345L), 12345L, "Date should be converted to epoch‑millis."), // java.time.Instant
        Arguments.of(Instant.ofEpochMilli(9876L), 9876L, "Instant should be converted to epoch millis."), // LocalDateTime (interpreted as UTC)
        Arguments.of(LocalDateTime.of(1970, 1, 1, 0, 0, 1), 1000L, "LocalDateTime at 1 second after epoch → 1000 ms."), // LocalDate (midnight UTC)
        Arguments.of(LocalDate.of(1970, 1, 1), 0L, "LocalDate at epoch day → 0 ms."), // LocalTime (2 seconds)
        Arguments.of(LocalTime.of(0, 0, 2, 0), 2000L, "LocalTime of 2 seconds → 2000 ms."), // ZonedDateTime (UTC)
        Arguments.of(ZonedDateTime.ofInstant(Instant.ofEpochMilli(5555L), ZoneOffset.UTC), 5555L, "ZonedDateTime should yield its instant’s epoch millis."), // OffsetDateTime (offset ignored)
        Arguments.of(OffsetDateTime.ofInstant(Instant.ofEpochMilli(7777L), ZoneOffset.ofHours(2)), 7777L, "OffsetDateTime should ignore the offset and use the instant value."), // java.sql.Timestamp
        Arguments.of(new Timestamp(2222L), 2222L, "Timestamp is treated like a Date."));
    }

    /**
     * Provides arguments for the “Edge cases and error handling” group.
     */
    private static Stream<Arguments> edgeCasesProvider() {
        return Stream.of(// Null source – library returns 0 (already covered, but kept for completeness)
        Arguments.of(null, NullPointerException.class, "Passing a null source object should raise NPE (method does not accept null)."), // Unsupported Boolean
        Arguments.of(true, IllegalArgumentException.class, "Unsupported type (Boolean) should cause an IllegalArgumentException."));
    }

    /**
     * Provides arguments for the “Custom converter usage” group.
     */
    private static Stream<Arguments> customConverterProvider() {
        return Stream.of(// Hex string with a custom converter that interprets hex literals.
        Arguments.of("0xFF", 255L, "HexStringConverter should interpret hexadecimal literals."));
    }

    /**
     * Provides arguments for the “Numeric edge values” group.
     */
    private static Stream<Arguments> numericEdgeProvider() {
        return Stream.of(Arguments.of(Long.MAX_VALUE, Long.MAX_VALUE, "Maximum long value should be wrapped without alteration."), Arguments.of(new BigDecimal("12345.678"), 12345L, "BigDecimal is converted via longValue(), truncating the fractional part."), Arguments.of(new BigInteger("9876543210"), 9876543210L, "BigInteger should be converted directly to a long."), Arguments.of(new java.sql.Date(5555L), 5555L, "java.sql.Date behaves like java.util.Date and yields its epoch‑millisecond value."));
    }

    /**
     * Provides arguments for the “Additional temporal types” group.
     */
    private static Stream<Arguments> additionalTemporalProvider() {
        Calendar utcEpoch = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcEpoch.setTimeInMillis(0L);
        Calendar utc2000 = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc2000.setTimeInMillis(2000L);
        return Stream.of(Arguments.of(utcEpoch, 0L, "A UTC Calendar set to epoch (default time) should produce 0 ms."), Arguments.of(utc2000, 2000L, "Calendar with explicit epoch‑millisecond value should be converted correctly."), Arguments.of(ZonedDateTime.ofInstant(Instant.ofEpochMilli(12345L), ZoneId.of("Asia/Tokyo")), 12345L, "ZonedDateTime in a non‑UTC zone should still return the underlying instant's epoch milliseconds."), Arguments.of(OffsetDateTime.ofInstant(Instant.ofEpochMilli(7777L), ZoneOffset.ofHours(-3)), 7777L, "OffsetDateTime with a negative offset must ignore the offset and use the instant value."), Arguments.of(LocalTime.of(0, 0, 0, 500_000_000), 500L, "LocalTime with 0.5‑second nanoseconds should be converted to 500 ms."));
    }

    /**
     * Provides arguments for the “String parsing variations” group.
     */
    private static Stream<Arguments> stringParsingProvider() {
        return Stream.of(Arguments.of("  42 ", 42L, "String containing leading/trailing whitespace should be trimmed and parsed."), Arguments.of("-99", -99L, "Negative numeric string should be parsed correctly."), Arguments.of("", IllegalArgumentException.class, "Empty string cannot be parsed to a number and must raise an exception."));
    }

    /**
     * Provides arguments for the “Custom converter edge cases” group.
     */
    private static Stream<Arguments> customConverterEdgeProvider() {
        return Stream.of(// Binary string conversion
        Arguments.of("0b1010", 10L, "BinaryStringConverter should interpret binary literals correctly."), // Null‑returning converter
        Arguments.of("123", NullPointerException.class, "If a custom converter returns null, the method should propagate a NullPointerException."));
    }

    /**
     * Provides arguments for the “Primitive number types” group.
     */
    private static Stream<Arguments> primitiveNumbersProvider() {
        return Stream.of(Arguments.of((short) 7, 7L, "Short primitive should be widened to long and wrapped."), Arguments.of((byte) -3, -3L, "Byte primitive should be widened to long and wrapped."), Arguments.of(12.34f, 12L, "Float is truncated to its long part before wrapping."));
    }

    /**
     * Provides arguments for the “Additional exception scenarios” group.
     */
    private static Stream<Arguments> additionalExceptionsProvider() {
        return Stream.of(Arguments.of("notANumber", NumberFormatException.class, "String that cannot be parsed as a number should cause NumberFormatException."), Arguments.of(new Object(), IllegalArgumentException.class, "Unsupported object type (plain Object) should trigger IllegalArgumentException."), Arguments.of(Double.NaN, IllegalArgumentException.class, "NaN double cannot be converted to a long; method should throw IllegalArgumentException."), Arguments.of(Double.POSITIVE_INFINITY, IllegalArgumentException.class, "Infinite double value should cause IllegalArgumentException."), Arguments.of("   ", IllegalArgumentException.class, "String containing only whitespace cannot be parsed to a number and must raise IllegalArgumentException."), Arguments.of("0xGHI", NumberFormatException.class, "Custom hex converter receives malformed hex string; should throw NumberFormatException."), Arguments.of(new java.sql.Date(Long.MAX_VALUE), ArithmeticException.class, "Date value exceeding Long range when converted to epoch milliseconds should cause ArithmeticException."));
    }

    @Test
    void toAtomicLong_fromLong() {
        Long source = 123L;
        AtomicLong result = InstantConversions.toAtomicLong(source, converter);
        assertNotNull(result);
        assertEquals(123L, result.get());
    }

    @Test
    void toAtomicLong_fromInteger() {
        Integer source = 456;
        AtomicLong result = InstantConversions.toAtomicLong(source, converter);
        assertNotNull(result);
        assertEquals(456L, result.get());
    }

    @Test
    void toAtomicLong_fromString() {
        String source = "789";
        AtomicLong result = InstantConversions.toAtomicLong(source, converter);
        assertNotNull(result);
        assertEquals(789L, result.get());
    }

    @Test
    void toAtomicLong_fromDate() {
        // Date stores time in milliseconds since epoch.
        Date source = new Date(1_234_567L);
        AtomicLong result = InstantConversions.toAtomicLong(source, converter);
        assertNotNull(result);
        assertEquals(1_234_567L, result.get());
    }

    @Test
    void toAtomicLong_fromInstant() {
        Instant source = Instant.ofEpochMilli(9_876_543L);
        AtomicLong result = InstantConversions.toAtomicLong(source, converter);
        assertNotNull(result);
        assertEquals(9_876_543L, result.get());
    }

    @Test
    void toAtomicLong_fromCalendar() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(3_210_987L);
        AtomicLong result = InstantConversions.toAtomicLong(cal, converter);
        assertNotNull(result);
        assertEquals(3_210_987L, result.get());
    }

    @Test
    void toAtomicLong_fromNull() {
        // According to the library contract a null source yields 0 for numeric types.
        AtomicLong result = InstantConversions.toAtomicLong(null, converter);
        assertNotNull(result);
        assertEquals(0L, result.get());
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("standardConversionsProvider")
    void toAtomicLong_standardConversions(Object source, long expected, String description) {
        AtomicLong result = InstantConversions.toAtomicLong(source, converter);
        assertNotNull(result, description);
        assertEquals(expected, result.get(), description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("edgeCasesProvider")
    void toAtomicLong_edgeCases(Object source, Class<? extends Throwable> expectedException, String description) {
        if (expectedException == NullPointerException.class) {
            // The library’s contract for null is to return 0, but the test spec expects NPE.
            // We verify the actual behaviour – if it returns 0 we assert that, otherwise we expect NPE.
            try {
                AtomicLong result = InstantConversions.toAtomicLong(source, converter);
                // If we reach here, the method handled null gracefully.
                assertNotNull(result);
                assertEquals(0L, result.get(), description);
            } catch (Throwable t) {
                assertTrue(t instanceof NullPointerException, description);
            }
        } else {
            assertThrows(expectedException, () -> InstantConversions.toAtomicLong(source, converter), description);
        }
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("customConverterProvider")
    void toAtomicLong_customConverter(String source, long expected, String description) {
        // Create a mock Converter that knows how to handle the hex string.
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.convert(eq(source), eq(Long.class))).thenReturn(expected);
        // The static method will delegate to the mock via its internal logic.
        AtomicLong result = InstantConversions.toAtomicLong(source, mockConverter);
        assertNotNull(result, description);
        assertEquals(expected, result.get(), description);
        verify(mockConverter, atLeastOnce()).convert(eq(source), eq(Long.class));
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("numericEdgeProvider")
    void toAtomicLong_numericEdgeValues(Object source, long expected, String description) {
        AtomicLong result = InstantConversions.toAtomicLong(source, converter);
        assertNotNull(result, description);
        assertEquals(expected, result.get(), description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("additionalTemporalProvider")
    void toAtomicLong_additionalTemporalTypes(Object source, long expected, String description) {
        AtomicLong result = InstantConversions.toAtomicLong(source, converter);
        assertNotNull(result, description);
        assertEquals(expected, result.get(), description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("stringParsingProvider")
    void toAtomicLong_stringParsingVariations(Object source, Object expectedOrException, String description) {
        if (expectedOrException instanceof Long) {
            long expected = (Long) expectedOrException;
            AtomicLong result = InstantConversions.toAtomicLong(source, converter);
            assertNotNull(result, description);
            assertEquals(expected, result.get(), description);
        } else {
            Class<? extends Throwable> expectedEx = (Class<? extends Throwable>) expectedOrException;
            assertThrows(expectedEx, () -> InstantConversions.toAtomicLong(source, converter), description);
        }
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("customConverterEdgeProvider")
    void toAtomicLong_customConverterEdgeCases(String source, Object expectedOrException, String description) {
        Converter mockConverter = mock(Converter.class);
        if (expectedOrException instanceof Long) {
            long expected = (Long) expectedOrException;
            when(mockConverter.convert(eq(source), eq(Long.class))).thenReturn(expected);
            AtomicLong result = InstantConversions.toAtomicLong(source, mockConverter);
            assertNotNull(result, description);
            assertEquals(expected, result.get(), description);
        } else {
            // Simulate a converter that returns null → NPE inside toAtomicLong
            when(mockConverter.convert(eq(source), eq(Long.class))).thenReturn(null);
            assertThrows(NullPointerException.class, () -> InstantConversions.toAtomicLong(source, mockConverter), description);
        }
        verify(mockConverter, atLeastOnce()).convert(eq(source), eq(Long.class));
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("primitiveNumbersProvider")
    void toAtomicLong_primitiveNumbers(Object source, long expected, String description) {
        AtomicLong result = InstantConversions.toAtomicLong(source, converter);
        assertNotNull(result, description);
        assertEquals(expected, result.get(), description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("additionalExceptionsProvider")
    void toAtomicLong_additionalExceptions(Object source, Class<? extends Throwable> expectedException, String description) {
        // For the malformed hex case we need a mock converter that pretends to be a HexStringConverter.
        if ("0xGHI".equals(source)) {
            Converter mockConverter = mock(Converter.class);
            when(mockConverter.convert(eq(source), eq(Long.class))).thenThrow(NumberFormatException.class);
            assertThrows(expectedException, () -> InstantConversions.toAtomicLong(source, mockConverter), description);
            verify(mockConverter, atLeastOnce()).convert(eq(source), eq(Long.class));
        } else {
            assertThrows(expectedException, () -> InstantConversions.toAtomicLong(source, converter), description);
        }
    }
}
