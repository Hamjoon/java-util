package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toInstant_Test {

    /**
     * Shared {@link Converter} used by all tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The library provides an IdentityConverter that simply returns the supplied value.
        // It is suitable for all the conversion scenarios in this test suite.
        converter = IdentityConverter.INSTANCE;
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference.
        converter = null;
    }

    private static Stream<Arguments> validConversionProvider() {
        // a deterministic epoch‑millis value used in many cases
        long epoch = 1_620_000_000_000L;
        Instant instant = Instant.ofEpochMilli(epoch);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(epoch);
        return Stream.of(// Long
        Arguments.of(epoch, Instant.ofEpochMilli(epoch), "Convert a Long representing epoch milliseconds directly to an Instant."), // Integer (auto‑widened)
        Arguments.of(1_620_000, Instant.ofEpochMilli(1_620_000L), "Convert an Integer (auto‑widened to long) to an Instant."), // Numeric String
        Arguments.of("1620000000000", instant, "Convert a numeric String to an Instant."), // java.util.Date
        Arguments.of(new Date(epoch), instant, "Convert a java.util.Date to an Instant using its internal epoch millis."), // java.time.Instant (already an Instant)
        Arguments.of(instant, instant, "Convert a java.time.Instant (already an Instant) – the method should return the same instant."), // java.sql.Timestamp (subclass of java.util.Date)
        Arguments.of(new Timestamp(epoch), instant, "Convert a java.sql.Timestamp (subclass of java.util.Date) to an Instant."), // BigDecimal
        Arguments.of(new BigDecimal("1620000000000"), instant, "Convert a BigDecimal representing epoch milliseconds to an Instant."), // Calendar instance
        Arguments.of(calendar, instant, "Convert a Calendar instance to an Instant using its time‑in‑millis value."), // Byte
        Arguments.of((byte) 5, Instant.ofEpochMilli(5L), "Convert a Byte value (5) to an Instant; should be treated as epoch milliseconds."), // Short (negative)
        Arguments.of((short) -12, Instant.ofEpochMilli(-12L), "Convert a Short value (-12) to an Instant representing a time before the epoch."), // BigInteger
        Arguments.of(new BigInteger("1620000000000"), instant, "Convert a BigInteger containing a valid epoch‑millis value to an Instant."), // AtomicLong
        Arguments.of(new AtomicLong(epoch), instant, "Convert an AtomicLong holding epoch milliseconds to an Instant."), // Double with fractional part (truncated)
        Arguments.of(1620000000000.9d, instant, "Convert a Double with a fractional part; the fractional part should be truncated."), // Float with fractional part (truncated)
        Arguments.of(1620000.7f, Instant.ofEpochMilli(1_620_000L), "Convert a Float with a fractional part; the fractional part should be truncated."), // Negative numeric String
        Arguments.of("-1000", Instant.ofEpochMilli(-1000L), "Convert a negative numeric String to an Instant representing a moment before the epoch."), // Zero string
        Arguments.of("0", Instant.ofEpochMilli(0L), "Convert the String \"0\" to the epoch Instant."));
    }

    private static Stream<Arguments> exceptionConversionProvider() {
        long epoch = 1_620_000_000_000L;
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(epoch);
        return Stream.of(// Null source – toLong treats null as 0, but the JSON expects NPE.
        // According to the library, null is converted to 0, however we honour the test spec.
        Arguments.of(null, IdentityConverter.INSTANCE, NullPointerException.class, "Passing a null source object should result in a NullPointerException (toLong cannot handle null)."), // Unsupported Boolean type
        Arguments.of(true, IdentityConverter.INSTANCE, IllegalArgumentException.class, "Passing an unsupported type (Boolean) should cause an IllegalArgumentException from toLong."), // Non‑numeric String
        Arguments.of("not-a-number", IdentityConverter.INSTANCE, NumberFormatException.class, "A non‑numeric String cannot be parsed to a long and should raise NumberFormatException."), // Null converter with supported source
        Arguments.of(epoch, null, NullPointerException.class, "Converter is null while source is a supported Long; method should fail with NPE when it attempts to use the converter."), // Null converter with Calendar source
        Arguments.of(cal, null, NullPointerException.class, "Converter is null for a Calendar source; toLong will try to delegate conversion and throw NPE."), // Empty string
        Arguments.of("", IdentityConverter.INSTANCE, NumberFormatException.class, "An empty string cannot be parsed as a number; expect NumberFormatException."), // Unsupported temporal types
        Arguments.of(LocalDate.now(), IdentityConverter.INSTANCE, IllegalArgumentException.class, "LocalDate is not a supported conversion source for toLong; should raise IllegalArgumentException."), Arguments.of(ZonedDateTime.now(), IdentityConverter.INSTANCE, IllegalArgumentException.class, "ZonedDateTime is not convertible to a long epoch millis by toLong; expect IllegalArgumentException."), // Special floating‑point values
        Arguments.of(Double.NaN, IdentityConverter.INSTANCE, IllegalArgumentException.class, "NaN does not represent a valid numeric epoch; conversion should be rejected."), Arguments.of(Float.POSITIVE_INFINITY, IdentityConverter.INSTANCE, IllegalArgumentException.class, "Infinity cannot be mapped to a long epoch millis; expect IllegalArgumentException."), // Unsupported atomic numeric type (AtomicInteger)
        Arguments.of(new AtomicInteger(123), IdentityConverter.INSTANCE, IllegalArgumentException.class, "AtomicInteger is not handled by toLong; should trigger IllegalArgumentException."), // Overflow – BigDecimal > Long.MAX_VALUE
        Arguments.of(new BigDecimal(Long.MAX_VALUE).add(BigDecimal.ONE), IdentityConverter.INSTANCE, ArithmeticException.class, "BigDecimal value exceeds Long.MAX_VALUE; toLong should throw ArithmeticException via longValueExact."), // Overflow – BigInteger > Long.MAX_VALUE
        Arguments.of(new BigInteger(Long.MAX_VALUE).add(BigInteger.ONE), IdentityConverter.INSTANCE, ArithmeticException.class, "BigInteger value exceeds Long.MAX_VALUE; conversion to long should raise ArithmeticException."), // BigDecimal with fractional part (cannot be converted exactly)
        Arguments.of(new BigDecimal("12345.67"), IdentityConverter.INSTANCE, ArithmeticException.class, "BigDecimal with a fractional part cannot be converted exactly to long; expect ArithmeticException."));
    }

    @Test
    void toInstant_fromLong_returnsCorrectInstant() {
        // a known epoch‑millisecond value
        Long epochMilli = 1_642_000_000_000L;
        Instant expected = Instant.ofEpochMilli(epochMilli);
        Instant actual = NumberConversions.toInstant(epochMilli, converter);
        assertEquals(expected, actual, "Instant created from Long should match expected value");
    }

    @Test
    void toInstant_fromStringNumber_returnsCorrectInstant() {
        // same value as above, but as a String
        String epochStr = "1642000000000";
        Instant expected = Instant.ofEpochMilli(Long.parseLong(epochStr));
        Instant actual = NumberConversions.toInstant(epochStr, converter);
        assertEquals(expected, actual, "Instant created from numeric String should match expected value");
    }

    @Test
    void toInstant_fromDate_returnsCorrectInstant() {
        Date date = new Date(1_642_000_000_000L);
        Instant expected = date.toInstant();
        Instant actual = NumberConversions.toInstant(date, converter);
        assertEquals(expected, actual, "Instant created from Date should match Date's instant");
    }

    @Test
    void toInstant_fromCalendar_returnsCorrectInstant() {
        Calendar cal = new GregorianCalendar();
        cal.setTimeInMillis(1_642_000_000_000L);
        Instant expected = Instant.ofEpochMilli(cal.getTimeInMillis());
        Instant actual = NumberConversions.toInstant(cal, converter);
        assertEquals(expected, actual, "Instant created from Calendar should match calendar's epoch millis");
    }

    @Test
    void toInstant_fromNull_returnsEpochStart() {
        // According to Converter's null handling, a null numeric conversion yields 0.
        Instant expected = Instant.ofEpochMilli(0);
        Instant actual = NumberConversions.toInstant(null, converter);
        assertEquals(expected, actual, "Null source should be converted to Instant at epoch start (0 ms)");
    }

    /* --------------------------------------------------------------
     *  Parameterized tests – valid conversions (numeric & date types)
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("validConversionProvider")
    void toInstant_validConversions(Object from, Instant expected, String description) {
        Instant actual = NumberConversions.toInstant(from, converter);
        assertEquals(expected, actual, description);
    }

    /* --------------------------------------------------------------
     *  Parameterized tests – invalid conversions / exception cases
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("exceptionConversionProvider")
    void toInstant_invalidConversions(Object from, Converter conv, Class<? extends Throwable> expectedException, String description) {
        // The method under test is static; we simply invoke it and assert the exception.
        assertThrows(expectedException, () -> NumberConversions.toInstant(from, conv), description);
    }
}
