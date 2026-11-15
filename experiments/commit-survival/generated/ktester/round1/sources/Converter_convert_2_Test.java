package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import com.cedarsoftware.util.convert.Convert;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;

@ExtendWith(MockitoExtension.class)
class Converter_convert_2_Test {

    /**
     * Data holder for simple conversion cases.
     */
    private static class SimpleCase {

        final Object from;

        final Class<?> to;

        // may be a value or an exception class
        final Object expected;

        final String description;

        SimpleCase(Object from, Class<?> to, Object expected, String description) {
            this.from = from;
            this.to = to;
            this.expected = expected;
            this.description = description;
        }
    }

    /**
     * ------------------------------------------------------------------
     */
    static List<SimpleCase> basicTypeCases() {
        return List.of(new SimpleCase("123", Integer.class, Integer.valueOf(123), "Convert a numeric String to Integer – verifies the String→Integer branch."), new SimpleCase(123, String.class, "123", "Convert an Integer to String – verifies the Integer→String branch."), new SimpleCase("true", Boolean.class, Boolean.TRUE, "Convert a String representing a boolean to Boolean – exercises the String→Boolean branch."), new SimpleCase(false, String.class, "false", "Convert a primitive boolean to String – tests primitive‑to‑wrapper conversion handling."));
    }

    /**
     * ------------------------------------------------------------------
     */
    static List<SimpleCase> dateTimeCases() {
        return List.of(new SimpleCase(new Date(0L), LocalDateTime.class, LocalDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneId.systemDefault()), "Convert java.util.Date (epoch) to LocalDateTime – covers Date→LocalDateTime branch."), new SimpleCase(Timestamp.valueOf("2023-01-01 12:34:56"), LocalDate.class, LocalDate.of(2023, 1, 1), "Convert java.sql.Timestamp to LocalDate – verifies Timestamp→LocalDate conversion."), new SimpleCase("2022-12-31", LocalDate.class, LocalDate.of(2022, 12, 31), "Convert ISO‑8601 date String to LocalDate – tests String→LocalDate parsing branch."));
    }

    /**
     * ------------------------------------------------------------------
     */
    static List<SimpleCase> atomicNumericCases() {
        return List.of(new SimpleCase("42", AtomicInteger.class, new AtomicInteger(42), "Convert a numeric String to AtomicInteger – exercises conversion to atomic types."), new SimpleCase(new AtomicLong(99L), Long.class, Long.valueOf(99L), "Convert AtomicLong to Long – verifies unwrapping of atomic numeric types."), new SimpleCase(true, AtomicBoolean.class, new AtomicBoolean(true), "Convert primitive boolean to AtomicBoolean – tests boolean→atomic conversion."));
    }

    @Test
    void convertNullToPrimitiveBoolean_returnsFalse() {
        // given
        Object source = null;
        // when
        boolean result = Converter.convert(source, boolean.class);
        // then
        assertFalse(result, "Converting null to primitive boolean should yield false");
    }

    @Test
    void convertNullToWrapperBoolean_returnsNull() {
        // given
        Object source = null;
        // when
        Boolean result = Converter.convert(source, Boolean.class);
        // then
        assertNull(result, "Converting null to Boolean wrapper should yield null");
    }

    @Test
    void convertStringToInteger_parsesCorrectly() {
        // given
        String source = "12345";
        // when
        Integer result = Converter.convert(source, Integer.class);
        // then
        assertEquals(12345, result);
    }

    @Test
    void convertIntegerToString_returnsStringRepresentation() {
        // given
        Integer source = 9876;
        // when
        String result = Converter.convert(source, String.class);
        // then
        assertEquals("9876", result);
    }

    @Test
    void convertStringToLocalDate_parsesIsoDate() {
        // given
        String source = "2023-08-15";
        // when
        LocalDate result = Converter.convert(source, LocalDate.class);
        // then
        assertEquals(LocalDate.of(2023, 8, 15), result);
    }

    @Test
    void convertLocalDateToString_formatsIsoDate() {
        // given
        LocalDate source = LocalDate.of(2022, 12, 31);
        // when
        String result = Converter.convert(source, String.class);
        // then
        assertEquals("2022-12-31", result);
    }

    @Test
    void convertStringToLocalDateTime_parsesIsoDateTime() {
        // given
        String source = "2023-08-15T13:45:30";
        // when
        LocalDateTime result = Converter.convert(source, LocalDateTime.class);
        // then
        assertEquals(LocalDateTime.of(2023, 8, 15, 13, 45, 30), result);
    }

    @Test
    void convertLocalDateTimeToString_formatsIsoDateTime() {
        // given
        LocalDateTime source = LocalDateTime.of(2021, 5, 10, 9, 30, 15);
        // when
        String result = Converter.convert(source, String.class);
        // then
        assertEquals("2021-05-10T09:30:15", result);
    }

    @Test
    void convertStringToBigDecimal_parsesCorrectly() {
        // given
        String source = "12345.6789";
        // when
        BigDecimal result = Converter.convert(source, BigDecimal.class);
        // then
        assertEquals(new BigDecimal("12345.6789"), result);
    }

    @Test
    void convertBigDecimalToString_returnsPlainString() {
        // given
        BigDecimal source = new BigDecimal("9876.54321");
        // when
        String result = Converter.convert(source, String.class);
        // then
        assertEquals("9876.54321", result);
    }

    @Test
    void convertStringToBigInteger_parsesCorrectly() {
        // given
        String source = "12345678901234567890";
        // when
        BigInteger result = Converter.convert(source, BigInteger.class);
        // then
        assertEquals(new BigInteger("12345678901234567890"), result);
    }

    @Test
    void convertBigIntegerToString_returnsStringRepresentation() {
        // given
        BigInteger source = new BigInteger("98765432109876543210");
        // when
        String result = Converter.convert(source, String.class);
        // then
        assertEquals("98765432109876543210", result);
    }

    @Test
    void convertDateToCalendar_preservesInstant() {
        // given
        // a fixed epoch millis
        Date source = new Date(1_600_000_000_000L);
        // when
        Calendar result = Converter.convert(source, Calendar.class);
        // then
        assertNotNull(result);
        assertEquals(source.getTime(), result.getTimeInMillis());
    }

    @Test
    void convertCalendarToDate_preservesInstant() {
        // given
        Calendar source = Calendar.getInstance();
        source.setTimeInMillis(1_600_000_000_000L);
        // when
        Date result = Converter.convert(source, Date.class);
        // then
        assertNotNull(result);
        assertEquals(source.getTimeInMillis(), result.getTime());
    }

    @Test
    void convertNullToPrimitiveInt_returnsZero() {
        // given
        Object source = null;
        // when
        int result = Converter.convert(source, int.class);
        // then
        assertEquals(0, result);
    }

    @Test
    void convertNullToWrapperInt_returnsNull() {
        // given
        Object source = null;
        // when
        Integer result = Converter.convert(source, Integer.class);
        // then
        assertNull(result);
    }

    @Test
    void convertStringToChar_returnsFirstCharacter() {
        // given
        String source = "Zebra";
        // when
        char result = Converter.convert(source, char.class);
        // then
        assertEquals('Z', result);
    }

    @Test
    void convertCharToString_returnsSingleCharString() {
        // given
        Character source = 'Q';
        // when
        String result = Converter.convert(source, String.class);
        // then
        assertEquals("Q", result);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("basicTypeCases")
    void basicTypeConversions(SimpleCase tc) {
        Object result = Converter.convert(tc.from, tc.to);
        assertEquals(tc.expected, result, tc.description);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("dateTimeCases")
    void dateTimeConversions(SimpleCase tc) {
        Object result = Converter.convert(tc.from, tc.to);
        assertEquals(tc.expected, result, tc.description);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("atomicNumericCases")
    void atomicNumericConversions(SimpleCase tc) {
        Object result = Converter.convert(tc.from, tc.to);
        // Atomic classes do not override equals – compare the contained value
        if (result instanceof AtomicInteger ai) {
            assertEquals(((AtomicInteger) tc.expected).get(), ai.get());
        } else if (result instanceof AtomicLong al) {
            assertEquals(((AtomicLong) tc.expected).get(), al.get());
        } else if (result instanceof AtomicBoolean ab) {
            assertEquals(((AtomicBoolean) tc.expected).get(), ab.get());
        } else {
            fail("Unexpected atomic type: " + result);
        }
    }

    /**
     * ------------------------------------------------------------------
     */
    @Test
    void convertNullToString_returnsNull() {
        Object result = Converter.convert(null, String.class);
        assertNull(result, "When source is null, conversion should return null regardless of target type.");
    }

    @Test
    void convertIdentityString_returnsSameInstance() {
        String source = "already a string";
        String result = Converter.convert(source, String.class);
        assertSame(source, result, "If source is already an instance of the target type, the same object should be returned.");
    }

    /**
     * ------------------------------------------------------------------
     */
    @Test
    void convertObjectToInteger_unsupportedThrows() {
        Object source = new Object();
        assertThrows(IllegalArgumentException.class, () -> Converter.convert(source, Integer.class), "Attempt to convert a generic Object to Integer should throw because no conversion is registered.");
    }

    @Test
    void convertInvalidNumberStringToDouble_throws() {
        String source = "not-a-number";
        assertThrows(IllegalArgumentException.class, () -> Converter.convert(source, Double.class), "String that cannot be parsed as a number to Double – verifies error handling for parsing failures.");
    }

    /**
     * ------------------------------------------------------------------
     */
    @Test
    void customConversion_StringToUUID_works() {
        // Register a custom conversion from String → UUID
        Converter.addConversion(String.class, UUID.class, (Convert<UUID>) source -> UUID.fromString((String) source));
        String uuidString = "123e4567-e89b-12d3-a456-426614174000";
        UUID result = Converter.convert(uuidString, UUID.class);
        assertEquals(UUID.fromString(uuidString), result, "After registering a custom conversion from String to UUID, the convert method should use the custom function.");
    }

    /**
     * ------------------------------------------------------------------
     */
    @Test
    void convertEmptyStringToInteger_throws() {
        assertThrows(IllegalArgumentException.class, () -> Converter.convert("", Integer.class), "Empty string cannot be parsed to Integer – verifies proper error handling for zero‑length numeric strings.");
    }

    @Test
    void convertMaxIntStringToInteger_success() {
        String source = "2147483647";
        Integer result = Converter.convert(source, Integer.class);
        assertEquals(Integer.valueOf(2147483647), result, "String representing Integer.MAX_VALUE should convert correctly – tests upper bound of 32‑bit signed int.");
    }

    @Test
    void convertOverflowIntString_throws() {
        assertThrows(IllegalArgumentException.class, () -> Converter.convert("2147483648", Integer.class), "String value exceeding Integer.MAX_VALUE should cause a conversion failure – checks overflow detection.");
    }

    @Test
    void convertDecimalStringToDouble_success() {
        String source = "123.456";
        Double result = Converter.convert(source, Double.class);
        assertEquals(Double.valueOf(123.456), result, "Decimal string to Double – validates floating‑point parsing.");
    }

    @Test
    void convertDecimalStringToFloat_success() {
        String source = "123.456";
        Float result = Converter.convert(source, Float.class);
        assertEquals(Float.valueOf(123.456f), result, "Decimal string to Float – validates floating‑point parsing with loss of precision.");
    }

    @Test
    void convertHexStringToInteger_throws() {
        assertThrows(IllegalArgumentException.class, () -> Converter.convert("0xFF", Integer.class), "Hexadecimal string is not accepted by the default numeric parser – ensures non‑decimal formats are rejected.");
    }

    /**
     * ------------------------------------------------------------------
     */
    @Test
    void convertWhitespaceBooleanString_true() {
        String source = " true ";
        Boolean result = Converter.convert(source, Boolean.class);
        assertTrue(result, "String with surrounding whitespace should be trimmed and parsed as Boolean true.");
    }

    @Test
    void convertUpperCaseFalseString_false() {
        String source = "FALSE";
        Boolean result = Converter.convert(source, Boolean.class);
        assertFalse(result, "Case‑insensitive parsing of Boolean false.");
    }

    @Test
    void convertNonStandardBooleanString_throws() {
        assertThrows(IllegalArgumentException.class, () -> Converter.convert("yes", Boolean.class), "Non‑standard Boolean literals should trigger a conversion error.");
    }

    @Test
    void convertSingleCharStringToCharacter_success() {
        String source = "A";
        Character result = Converter.convert(source, Character.class);
        assertEquals(Character.valueOf('A'), result, "Single‑character string to Character – validates simple char conversion.");
    }

    @Test
    void convertMultiCharStringToCharacter_throws() {
        assertThrows(IllegalArgumentException.class, () -> Converter.convert("AB", Character.class), "String longer than one character cannot be converted to Character – checks length validation.");
    }

    /**
     * ------------------------------------------------------------------
     */
    @Test
    void convertLocalDateTimeToDate_preservesInstant() {
        LocalDateTime source = LocalDateTime.of(2020, 5, 15, 10, 30, 45);
        Date result = Converter.convert(source, Date.class);
        Instant expected = source.atZone(ZoneId.systemDefault()).toInstant();
        assertEquals(Date.from(expected), result, "LocalDateTime → Date conversion – verifies that the instant is preserved across the API boundary.");
    }

    @Test
    void convertDateToZonedDateTime_preservesZone() {
        Date source = new Date(0L);
        ZonedDateTime result = Converter.convert(source, ZonedDateTime.class);
        ZonedDateTime expected = ZonedDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneId.systemDefault());
        assertEquals(expected, result, "Date (epoch) → ZonedDateTime – tests conversion that adds zone information.");
    }

    @Test
    void convertGregorianCalendarToLocalDate_epoch() {
        Calendar source = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        source.setTimeInMillis(0L);
        LocalDate result = Converter.convert(source, LocalDate.class);
        assertEquals(LocalDate.of(1970, 1, 1), result, "Calendar set to epoch (default constructor) → LocalDate – ensures date component extraction works.");
    }

    @Test
    void convertTimestampToLocalDateTime_preservesNanos() {
        Timestamp source = Timestamp.valueOf("2023-03-10 12:34:56.123456789");
        LocalDateTime result = Converter.convert(source, LocalDateTime.class);
        assertEquals(LocalDateTime.of(2023, 3, 10, 12, 34, 56, 123456789), result, "Timestamp with nanosecond precision → LocalDateTime – validates that sub‑second precision is retained.");
    }

    /**
     * ------------------------------------------------------------------
     */
    @Test
    void convertAtomicIntegerToPrimitiveInt_success() {
        AtomicInteger source = new AtomicInteger(5);
        int result = Converter.convert(source, int.class);
        assertEquals(5, result, "AtomicInteger → primitive int – checks unwrapping of atomic numeric types.");
    }

    @Test
    void convertAtomicBooleanToBoolean_success() {
        AtomicBoolean source = new AtomicBoolean(true);
        Boolean result = Converter.convert(source, Boolean.class);
        assertTrue(result, "AtomicBoolean → Boolean – verifies boolean atomic conversion.");
    }

    @Test
    void convertAtomicLongToLong_success() {
        AtomicLong source = new AtomicLong(99L);
        Long result = Converter.convert(source, Long.class);
        assertEquals(Long.valueOf(99L), result, "AtomicLong → Long – ensures proper unboxing.");
    }

    /**
     * ------------------------------------------------------------------
     */
    @Test
    void convertIntegerIdentity_returnsSameInstance() {
        Integer source = Integer.valueOf(10);
        Integer result = Converter.convert(source, Integer.class);
        assertSame(source, result, "Source already an Integer and target is Integer – should return the original object.");
    }

    @Test
    void convertPrimitiveIntToInteger_createsEqualValue() {
        // autoboxed to Integer by the convert method
        int source = 20;
        Integer result = Converter.convert(source, Integer.class);
        assertEquals(Integer.valueOf(20), result, "Primitive int (autoboxed) to Integer – conversion should produce an equal Integer value.");
    }

    /**
     * ------------------------------------------------------------------
     */
    @Test
    void convertNullToPrimitiveInt_returnsZero_accordingToSpec() {
        int result = Converter.convert(null, int.class);
        assertEquals(0, result, "Converting null to primitive int should yield logical zero per library contract.");
    }

    @Test
    void convertNullToPrimitiveBoolean_returnsFalse_accordingToSpec() {
        boolean result = Converter.convert(null, boolean.class);
        assertFalse(result, "Converting null to primitive boolean should yield false per library contract.");
    }

    /**
     * ------------------------------------------------------------------
     */
    @Test
    void convertStringToList_unsupportedThrows() {
        assertThrows(IllegalArgumentException.class, () -> Converter.convert("123", List.class), "There is no built‑in conversion from String to List – the converter must signal unsupported conversion.");
    }

    @Test
    void convertObjectToLocalDate_unsupportedThrows() {
        assertThrows(IllegalArgumentException.class, () -> Converter.convert(new Object(), LocalDate.class), "Generic Object cannot be converted to a date type – verifies proper error reporting for completely unknown source types.");
    }

    /**
     * ------------------------------------------------------------------
     */
    @Test
    void convertWithNullTargetClass_throwsNPE() {
        assertThrows(NullPointerException.class, () -> Converter.convert("123", null), "Calling convert with a null target Class should raise NPE before any conversion logic is applied.");
    }
}
