package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toCalendar_Test {

    /**
     * Shared {@link Converter} instance used by all tests.
     */
    private static Converter converter;

    /**
     * Shared {@link Converter} instance used by the majority of tests.
     */
    private static Converter defaultConverter;

    /**
     * A Converter that always returns the supplied fixed long value.
     */
    private static final class FixedLongConverter extends Converter {

        private final long fixed;

        FixedLongConverter(long fixed) {
            super(new ConverterOptions());
            this.fixed = fixed;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T convert(Object source, Class<T> target) {
            if (target == Long.class || target == long.class) {
                return (T) Long.valueOf(fixed);
            }
            return super.convert(source, target);
        }
    }

    /**
     * A Converter that returns {@code null} for any conversion request.
     */
    private static final class NullReturningConverter extends Converter {

        NullReturningConverter() {
            super(new ConverterOptions());
        }

        @Override
        public <T> T convert(Object source, Class<T> target) {
            return null;
        }
    }

    /**
     * A Converter that throws the supplied exception (or a generic RuntimeException).
     */
    private static final class ExceptionThrowingConverter extends Converter {

        private final RuntimeException toThrow;

        ExceptionThrowingConverter() {
            this(new RuntimeException("forced exception"));
        }

        ExceptionThrowingConverter(RuntimeException toThrow) {
            super(new ConverterOptions());
            this.toThrow = toThrow;
        }

        @Override
        public <T> T convert(Object source, Class<T> target) {
            throw toThrow;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter requires a ConverterOptions instance – the default constructor is sufficient
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the shared Converter reference
        converter = null;
    }

    /**
     * Arguments for the “standard conversion” group (null converter).
     */
    private static Stream<Arguments> standardConversionProvider() {
        return Stream.of(// Long literal
        Arguments.of(123_456_789L, 123_456_789L, "Long literal"), // Numeric String
        Arguments.of("1618033988749", 1_618_033_988_749L, "String numeric"), // Date at epoch
        Arguments.of(new Date(0L), 0L, "Date epoch"), // BigInteger
        Arguments.of(new BigInteger("9876543210"), 9_876_543_210L, "BigInteger"), // BigDecimal (truncated)
        Arguments.of(new BigDecimal("12345.678"), 12_345L, "BigDecimal truncation"), // AtomicLong
        Arguments.of(new AtomicLong(5_555_555L), 5_555_555L, "AtomicLong"), // Timestamp
        Arguments.of(new Timestamp(1_620_000_000_000L), 1_620_000_000_000L, "Timestamp"), // Instant
        Arguments.of(Instant.ofEpochMilli(2_000_000_000L), 2_000_000_000L, "Instant"));
    }

    /**
     * Primitive wrappers and atomic types (null converter).
     */
    private static Stream<Arguments> wrapperAndAtomicProvider() {
        return Stream.of(Arguments.of(Integer.valueOf(42), 42L, "Integer wrapper"), Arguments.of(Short.valueOf((short) 7), 7L, "Short wrapper"), Arguments.of(Byte.valueOf((byte) 3), 3L, "Byte wrapper"), Arguments.of(Double.valueOf(12_345.678), 12_345L, "Double truncation"), Arguments.of(new AtomicInteger(777), 777L, "AtomicInteger"), Arguments.of(new AtomicLong(5_555_555L), 5_555_555L, "AtomicLong (again)"));
    }

    /**
     * java.time types (null converter).
     */
    private static Stream<Arguments> javaTimeProvider() {
        return Stream.of(Arguments.of(LocalDate.of(1970, 1, 2), 86_400_000L, "LocalDate 1970-01-02 → +1 day"), Arguments.of(LocalDateTime.of(1970, 1, 1, 0, 0, 1), 1_000L, "LocalDateTime 1970-01-01T00:00:01 → +1 sec"), Arguments.of(ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), 0L, "ZonedDateTime UTC epoch → 0"), Arguments.of(OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), 0L, "OffsetDateTime UTC epoch → 0"), Arguments.of(Year.of(1970), 0L, "Year 1970 → start of epoch"));
    }

    /**
     * Verify that a {@link Long} value is correctly turned into a {@link Calendar}
     * whose epoch‑millis match the supplied long.
     */
    @Test
    void toCalendar_fromLong_returnsCorrectCalendar() {
        // given
        // 2021‑05‑01T00:00:00Z
        long epochMillis = 1_620_548_800_000L;
        // when
        Calendar cal = NumberConversions.toCalendar(epochMillis, converter);
        // then
        assertNotNull(cal, "Calendar should not be null");
        assertEquals(epochMillis, cal.getTimeInMillis(), "Calendar time should match the supplied long value");
    }

    /**
     * Verify that a numeric {@link String} is parsed to a long and then to a Calendar.
     */
    @Test
    void toCalendar_fromStringNumber_returnsCorrectCalendar() {
        // given
        // 2021‑05‑09T07:46:40Z
        String epochString = "1620548800000";
        long expected = Long.parseLong(epochString);
        // when
        Calendar cal = NumberConversions.toCalendar(epochString, converter);
        // then
        assertNotNull(cal);
        assertEquals(expected, cal.getTimeInMillis(), "Calendar time should match the parsed long value from the string");
    }

    /**
     * Verify that a {@link Date} instance is converted to a Calendar with the same epoch time.
     */
    @Test
    void toCalendar_fromDate_returnsCorrectCalendar() {
        // given
        // 2020‑09‑13T12:26:40Z
        Date date = new Date(1_600_000_000_000L);
        // when
        Calendar cal = NumberConversions.toCalendar(date, converter);
        // then
        assertNotNull(cal);
        assertEquals(date.getTime(), cal.getTimeInMillis(), "Calendar time should match the Date's epoch milliseconds");
    }

    /**
     * Verify that a {@code null} source yields a Calendar set to the epoch (0 ms).
     * The {@link NumberConversions#toLong(Object, Converter)} contract returns 0 for null,
     * which in turn creates a Calendar representing the epoch.
     */
    @Test
    void toCalendar_fromNull_returnsEpochCalendar() {
        // given
        Object nullValue = null;
        // when
        Calendar cal = NumberConversions.toCalendar(nullValue, converter);
        // then
        assertNotNull(cal);
        assertEquals(0L, cal.getTimeInMillis(), "Calendar created from null should represent the epoch (0 milliseconds)");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("standardConversionProvider")
    void toCalendar_standardConversions(Object from, long expectedMillis, String description) {
        Calendar cal = NumberConversions.toCalendar(from, defaultConverter);
        assertNotNull(cal, description);
        assertEquals(expectedMillis, cal.getTimeInMillis(), "Unexpected epoch millis for: " + description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("wrapperAndAtomicProvider")
    void toCalendar_wrapperAndAtomicConversions(Object from, long expectedMillis, String description) {
        Calendar cal = NumberConversions.toCalendar(from, defaultConverter);
        assertNotNull(cal, description);
        assertEquals(expectedMillis, cal.getTimeInMillis(), "Unexpected epoch millis for: " + description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("javaTimeProvider")
    void toCalendar_javaTimeConversions(Object from, long expectedMillis, String description) {
        Calendar cal = NumberConversions.toCalendar(from, defaultConverter);
        assertNotNull(cal, description);
        assertEquals(expectedMillis, cal.getTimeInMillis(), "Unexpected epoch millis for: " + description);
    }

    @Test
    void toCalendar_customFixedLongConverter_ignoresSource() {
        Converter fixed = new FixedLongConverter(999L);
        Calendar cal = NumberConversions.toCalendar("any value", fixed);
        assertNotNull(cal);
        assertEquals(999L, cal.getTimeInMillis(), "FixedLongConverter should force epoch millis to 999");
    }

    @Test
    void toCalendar_customFixedLongConverter_nullSource() {
        Converter fixed = new FixedLongConverter(555L);
        Calendar cal = NumberConversions.toCalendar(null, fixed);
        assertNotNull(cal);
        assertEquals(555L, cal.getTimeInMillis(), "FixedLongConverter should supply 555 even when source is null");
    }

    @Test
    void toCalendar_customExceptionThrowingConverter_propagatesRuntimeException() {
        Converter throwing = new ExceptionThrowingConverter();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> NumberConversions.toCalendar("trigger", throwing));
        assertEquals("forced exception", ex.getMessage(), "ExceptionThrowingConverter should propagate its RuntimeException");
    }

    @Test
    void toCalendar_customExceptionThrowingConverter_checkedWrapped() {
        RuntimeException runtime = new RuntimeException("wrapped");
        Converter throwing = new ExceptionThrowingConverter(runtime);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> NumberConversions.toCalendar("value", throwing));
        assertSame(runtime, ex, "The exact RuntimeException should be propagated");
    }

    @Test
    void toCalendar_customNullReturningConverter_resultsInNPE() {
        Converter nullReturning = new NullReturningConverter();
        assertThrows(NullPointerException.class, () -> NumberConversions.toCalendar("any", nullReturning), "Unboxing a null Long should cause NullPointerException");
    }

    @Test
    void toCalendar_malformedString_throwsNumberFormatException() {
        assertThrows(NumberFormatException.class, () -> NumberConversions.toCalendar("not a number", defaultConverter), "Non‑numeric string should cause NumberFormatException");
    }

    @Test
    void toCalendar_unsupportedObject_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> NumberConversions.toCalendar(new Object(), defaultConverter), "Unsupported source type should cause IllegalArgumentException");
    }

    @Test
    void toCalendar_booleanValue_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> NumberConversions.toCalendar(Boolean.TRUE, defaultConverter), "Boolean source should cause IllegalArgumentException");
    }

    @Test
    void toCalendar_longMaxValue_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> NumberConversions.toCalendar(Long.MAX_VALUE, defaultConverter), "Epoch millis beyond Calendar range should throw IllegalArgumentException");
    }

    @Test
    void toCalendar_longMinValue_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> NumberConversions.toCalendar(Long.MIN_VALUE, defaultConverter), "Epoch millis below Calendar range should throw IllegalArgumentException");
    }
}
