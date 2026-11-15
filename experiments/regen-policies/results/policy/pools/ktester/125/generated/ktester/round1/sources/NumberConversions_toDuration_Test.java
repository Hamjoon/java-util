package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toDuration_Test {

    private static Converter converter;

    /**
     * The default {@link Converter} used when the JSON specifies {@code "null"} for the converter.
     */
    private static Converter defaultConverter;

    /**
     * Simple POJO used by a few test cases that require a custom object containing a {@code long} value.
     */
    private static final class CustomObject {

        private final long value;

        CustomObject(long value) {
            this.value = value;
        }

        long getValue() {
            return value;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; default options are sufficient for these tests.
        converter = new Converter(new ConverterOptions());
    }

    /**
     * Creates a Mockito mock of {@link Converter} that returns {@code result}
     * when {@code convert(source, …)} is invoked.  All other arguments return
     * {@code null} (the default Mockito behaviour), which is sufficient for
     * the tests that expect an exception.
     */
    private static Converter mockConverterReturning(Object source, Object result) {
        Converter mock = mock(Converter.class);
        when(mock.convert(eq(source), any())).thenReturn(result);
        return mock;
    }

    /**
     * Supplies all successful conversion scenarios extracted from the JSON.
     * Each {@link Arguments} entry contains:
     * <ul>
     *   <li>the source object</li>
     *   <li>a {@link Converter} (or {@code null} to use the default)</li>
     *   <li>the expected ISO‑8601 duration string</li>
     * </ul>
     */
    private static Stream<Arguments> provideSuccessfulConversions() {
        // ---- Primitive / wrapper numeric types -------------------------------------------------
        return Stream.of(// Valid numeric conversions
        Arguments.of(123L, null, "PT0.123S"), Arguments.of(456, null, "PT0.456S"), Arguments.of("789", null, "PT0.789S"), Arguments.of(new BigInteger("1000"), null, "PT1S"), Arguments.of(new BigDecimal("1500.9"), null, "PT1.500S"), Arguments.of(AtomicLong.valueOf(2000L), null, "PT2S"), // Additional numeric conversions
        Arguments.of((byte) 7, null, "PT0.007S"), Arguments.of((short) 15, null, "PT0.015S"), Arguments.of(9.99f, null, "PT0.009S"), Arguments.of(123.456, null, "PT0.123S"), Arguments.of(AtomicInteger.valueOf(88), null, "PT0.088S"), Arguments.of(createGregorianCalendar(8000L), null, "PT8S"), Arguments.of(new Timestamp(9000L), null, "PT9S"), Arguments.of(new java.sql.Date(10000L), null, "PT10S"), Arguments.of(ZonedDateTime.ofInstant(Instant.ofEpochMilli(11000L), ZoneId.systemDefault()), null, "PT11S"), Arguments.of(OffsetDateTime.ofInstant(Instant.ofEpochMilli(12000L), ZoneOffset.UTC), null, "PT12S"), Arguments.of(-500L, null, "PT-0.5S"), Arguments.of(new BigDecimal("-2500.75"), null, "PT-2.5S"), // Date/Time conversions that rely on the default converter
        Arguments.of(new Date(2500L), null, "PT2.5S"), Arguments.of(Instant.ofEpochMilli(3000L), null, "PT3S"), // Conversions that require a custom converter (mocked)
        Arguments.of(LocalDateTime.of(1970, 1, 1, 0, 0, 4, 0), mockConverterReturning(LocalDateTime.of(1970, 1, 1, 0, 0, 4, 0), 4000L), "PT4S"), Arguments.of(new CustomObject(1234L), mockConverterReturning(new CustomObject(1234L), 1234L), "PT1.234S"), Arguments.of(LocalDate.of(1970, 1, 2), mockConverterReturning(LocalDate.of(1970, 1, 2), 86_400_000L), "PT86400S"));
    }

    /**
     * Helper to create a {@link GregorianCalendar} pre‑initialised with the given epoch millis.
     */
    private static Calendar createGregorianCalendar(long epochMillis) {
        Calendar cal = new GregorianCalendar();
        cal.setTimeInMillis(epochMillis);
        return cal;
    }

    /**
     * Supplies all error‑handling scenarios from the JSON.
     */
    private static Stream<Arguments> provideExceptionalConversions() {
        CustomObject custom5555 = new CustomObject(5555L);
        CustomObject custom7777 = new CustomObject(7777L);
        CustomObject custom999 = new CustomObject(999L);
        CustomObject custom111 = new CustomObject(111L);
        // Converters that deliberately return unsupported values
        Converter nullReturningConverter = mock(Converter.class);
        when(nullReturningConverter.convert(eq(custom5555), any())).thenReturn(null);
        Converter booleanReturningConverter = mock(Converter.class);
        when(booleanReturningConverter.convert(eq(custom7777), any())).thenReturn(Boolean.TRUE);
        Converter listReturningConverter = mock(Converter.class);
        when(listReturningConverter.convert(eq(custom999), any())).thenReturn(List.of(1, 2));
        Converter charReturningConverter = mock(Converter.class);
        when(charReturningConverter.convert(eq(custom111), any())).thenReturn('c');
        return Stream.of(// Null source with null converter (default converter is also null)
        Arguments.of(null, null, NullPointerException.class), // Unsupported primitive type
        Arguments.of(true, null, IllegalArgumentException.class), // String that cannot be parsed as a number
        Arguments.of("notANumber", null, IllegalArgumentException.class), // Year (unsupported)
        Arguments.of(Year.of(2020), null, IllegalArgumentException.class), // Collection types
        Arguments.of(new ArrayList<>(), null, IllegalArgumentException.class), Arguments.of(new HashMap<>(), null, IllegalArgumentException.class), // Already-a-Duration instance (unsupported for numeric conversion)
        Arguments.of(Duration.ofSeconds(5), null, IllegalArgumentException.class), // Empty / whitespace strings
        Arguments.of("", null, IllegalArgumentException.class), Arguments.of("   ", null, IllegalArgumentException.class), // Numeric overflow (value > Long.MAX_VALUE)
        Arguments.of("9223372036854775808", null, IllegalArgumentException.class), // Floating‑point special values
        Arguments.of(Double.NaN, null, IllegalArgumentException.class), Arguments.of(Float.NaN, null, IllegalArgumentException.class), Arguments.of(Double.POSITIVE_INFINITY, null, IllegalArgumentException.class), Arguments.of(Float.NEGATIVE_INFINITY, null, IllegalArgumentException.class), // BigInteger overflow
        Arguments.of(new BigInteger("9223372036854775808"), null, IllegalArgumentException.class), // Converters that return null / wrong type
        Arguments.of(custom5555, nullReturningConverter, NullPointerException.class), Arguments.of(custom7777, booleanReturningConverter, IllegalArgumentException.class), Arguments.of(custom999, listReturningConverter, IllegalArgumentException.class), Arguments.of(custom111, charReturningConverter, IllegalArgumentException.class));
    }

    @Test
    void toDuration_fromLong_returnsCorrectDuration() {
        Long source = 12_345L;
        Duration expected = Duration.ofMillis(12_345L);
        Duration actual = NumberConversions.toDuration(source, converter);
        assertEquals(expected, actual, "Duration should be created from the long value directly");
    }

    @Test
    void toDuration_fromInteger_returnsCorrectDuration() {
        Integer source = 6_789;
        Duration expected = Duration.ofMillis(6_789L);
        Duration actual = NumberConversions.toDuration(source, converter);
        assertEquals(expected, actual, "Duration should be created from the integer value");
    }

    @Test
    void toDuration_fromStringNumeric_returnsCorrectDuration() {
        String source = "12345";
        Duration expected = Duration.ofMillis(12_345L);
        Duration actual = NumberConversions.toDuration(source, converter);
        assertEquals(expected, actual, "String containing a numeric value should be parsed to long");
    }

    @Test
    void toDuration_fromDate_returnsCorrectDuration() {
        // Date#getTime() returns the epoch milliseconds.
        // 1 second after epoch
        Date source = new Date(1_000L);
        Duration expected = Duration.ofMillis(1_000L);
        Duration actual = NumberConversions.toDuration(source, converter);
        assertEquals(expected, actual, "Date should be converted using its epoch millisecond value");
    }

    @Test
    void toDuration_fromNull_returnsZeroDuration() {
        // Converter returns 0 for null when converting to a numeric primitive.
        Duration expected = Duration.ofMillis(0L);
        Duration actual = NumberConversions.toDuration(null, converter);
        assertEquals(expected, actual, "Null input should result in a zero‑millisecond Duration");
    }

    @Test
    void toDuration_fromNegativeLong_returnsCorrectDuration() {
        Long source = -5_000L;
        Duration expected = Duration.ofMillis(-5_000L);
        Duration actual = NumberConversions.toDuration(source, converter);
        assertEquals(expected, actual, "Negative long values should be represented correctly in Duration");
    }

    /**
     * Parameterised test that verifies successful conversion of a wide variety
     * of source objects into the expected {@link Duration}.
     *
     * The {@code expectedIso} string is an ISO‑8601 duration (e.g. {@code PT0.123S})
     * which is parsed via {@link Duration#parse(CharSequence)}.
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("provideSuccessfulConversions")
    void testToDuration_success(Object from, Converter converter, String expectedIso) {
        // Resolve the possibly‑null converter to the default instance.
        Converter usedConverter = (converter == null) ? defaultConverter : converter;
        Duration expected = Duration.parse(expectedIso);
        Duration actual = NumberConversions.toDuration(from, usedConverter);
        assertEquals(expected, actual, () -> "Duration mismatch for input [" + from + "]");
    }

    /**
     * Parameterised test that confirms the method throws the expected exception
     * for unsupported or malformed inputs.
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("provideExceptionalConversions")
    void testToDuration_exceptions(Object from, Converter converter, Class<? extends Throwable> expectedException) {
        Converter usedConverter = (converter == null) ? defaultConverter : converter;
        assertThrows(expectedException, () -> NumberConversions.toDuration(from, usedConverter), () -> "Expected exception " + expectedException.getSimpleName() + " for input [" + from + "]");
    }
}
