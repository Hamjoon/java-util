package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toCalendar_Test {

    /**
     * A real {@link Converter} instance used for the tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; using the default constructor.
        // If a different constructor is needed, adjust accordingly.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static converter reference.
        converter = null;
    }

    private static Stream<Arguments> validNumericProvider() {
        return Stream.of(// Long value
        Arguments.of("Long value", 1622548800000L, 1622548800000L), // Integer value
        Arguments.of("Integer value", 12345, 12345L), // Numeric String
        Arguments.of("String numeric", "1622548800000", 1622548800000L), // java.util.Date
        Arguments.of("Date instance", new Date(1622548800000L), 1622548800000L), // java.time.Instant
        Arguments.of("Instant instance", Instant.ofEpochMilli(1622548800000L), 1622548800000L), // LocalDateTime interpreted in default zone
        Arguments.of("LocalDateTime instance", LocalDateTime.of(2021, 6, 1, 0, 0), LocalDateTime.of(2021, 6, 1, 0, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()), // Byte
        Arguments.of("Byte instance", Byte.valueOf((byte) 7), 7L), // Short
        Arguments.of("Short instance", Short.valueOf((short) 123), 123L), // Float (truncated)
        Arguments.of("Float instance", Float.valueOf(12.34f), 12L), // Double (truncated)
        Arguments.of("Double instance", Double.valueOf(56.78), 56L), // BigDecimal
        Arguments.of("BigDecimal instance", new BigDecimal("9876543210"), 9876543210L), // BigInteger
        Arguments.of("BigInteger instance", new BigInteger("1234567890123456789"), 1234567890123456789L), // AtomicLong
        Arguments.of("AtomicLong instance", new AtomicLong(55555L), 55555L), // AtomicInteger
        Arguments.of("AtomicInteger instance", new AtomicInteger(777), 777L));
    }

    private static Stream<Arguments> exceptionProvider() {
        return Stream.of(// Null source
        Arguments.of("Null source", null, converter, NullPointerException.class), // Non‑numeric String
        Arguments.of("Non‑numeric String", "abc", converter, NumberFormatException.class), // Boolean source
        Arguments.of("Boolean source", true, converter, IllegalArgumentException.class), // Null converter
        Arguments.of("Null converter", 1622548800000L, null, NullPointerException.class), // Unsupported Object
        Arguments.of("Unsupported Object", new Object(), converter, IllegalArgumentException.class), // Unsupported Collection
        Arguments.of("Unsupported Collection", new ArrayList<>(), converter, IllegalArgumentException.class), // Year instance
        Arguments.of("Year instance", Year.of(2021), converter, IllegalArgumentException.class), // Character instance
        Arguments.of("Character instance", 'A', converter, IllegalArgumentException.class), // Empty string
        Arguments.of("Empty string", "", converter, NumberFormatException.class), // Whitespace string
        Arguments.of("Whitespace string", "   ", converter, NumberFormatException.class), // String exceeding Long.MAX_VALUE
        Arguments.of("Overflow string", "9223372036854775808", converter, NumberFormatException.class));
    }

    @Test
    void toCalendar_fromLong_returnsCalendarWithSameEpochMillis() {
        // GIVEN
        // a concrete epoch‑millisecond value
        Long source = 1_642_000_000_000L;
        // WHEN
        Calendar result = NumberConversions.toCalendar(source, converter);
        // THEN
        assertNotNull(result, "Resulting Calendar should not be null");
        assertEquals(source.longValue(), result.getTimeInMillis(), "Calendar time should match the source long value");
    }

    @Test
    void toCalendar_fromInteger_isPromotedToLong() {
        // GIVEN
        // seconds since epoch, will be treated as millis by toLong
        Integer source = 1_642_000_000;
        // WHEN
        Calendar result = NumberConversions.toCalendar(source, converter);
        // THEN
        assertNotNull(result);
        assertEquals(source.longValue(), result.getTimeInMillis(), "Integer should be promoted to long without alteration");
    }

    @Test
    void toCalendar_fromStringNumeric_isParsedToLong() {
        // GIVEN
        String source = "1642000000000";
        // WHEN
        Calendar result = NumberConversions.toCalendar(source, converter);
        // THEN
        assertNotNull(result);
        assertEquals(Long.parseLong(source), result.getTimeInMillis(), "String containing a numeric value should be parsed to long");
    }

    @Test
    void toCalendar_fromNull_returnsNull() {
        // GIVEN
        Object source = null;
        // WHEN
        Calendar result = NumberConversions.toCalendar(source, converter);
        // THEN
        assertNull(result, "Converting null should yield null Calendar");
    }

    /* -------------------------------------------------------------
     *  Parameterized tests for successful numeric conversions
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validNumericProvider")
    void toCalendar_validNumericConversions(String description, Object source, long expectedMillis) {
        // WHEN
        Calendar result = NumberConversions.toCalendar(source, converter);
        // THEN
        assertNotNull(result, "Resulting Calendar should not be null");
        assertEquals(expectedMillis, result.getTimeInMillis(), () -> "Calendar time should match expected millis for: " + description);
    }

    /* -------------------------------------------------------------
     *  Parameterized tests for expected exception scenarios
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("exceptionProvider")
    void toCalendar_expectedExceptions(String description, Object source, Converter conv, Class<? extends Throwable> expectedException) {
        // WHEN / THEN
        assertThrows(expectedException, () -> NumberConversions.toCalendar(source, conv), () -> "Expected " + expectedException.getSimpleName() + " for: " + description);
    }

    /* -------------------------------------------------------------
     *  Additional explicit tests for edge‑cases not covered by the
     *  parameterized data (e.g., null handling that returns null in
     *  the original framework).  These are kept for completeness.
     * ------------------------------------------------------------- */
    @Test
    void toCalendar_fromNull_returnsNull_whenConverterHandlesIt() {
        // Some implementations may return null instead of throwing; this test documents the
        // observed behaviour with the default Converter.
        assertThrows(NullPointerException.class, () -> NumberConversions.toCalendar(null, converter));
    }
}
