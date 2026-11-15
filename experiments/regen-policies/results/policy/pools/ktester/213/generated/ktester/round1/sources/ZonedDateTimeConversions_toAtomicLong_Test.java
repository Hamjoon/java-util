package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ZonedDateTimeConversions_toAtomicLong_Test {

    /**
     * Shared {@link Converter} instance used by all test methods.
     */
    private static Converter sharedConverter;

    /**
     * Instance of {@link Converter} created before each test (in case a test mutates state).
     */
    private Converter converter;

    /**
     * Simple POJO used by the custom‑converter tests.
     */
    static class MyCustomNumber {

        private final long value;

        MyCustomNumber(long value) {
            this.value = value;
        }

        long getValue() {
            return value;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; using the default constructor.
        sharedConverter = new Converter(new ConverterOptions());
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh Converter for each test to avoid cross‑test side effects.
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // No external resources to clean up; method present for completeness.
        converter = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        sharedConverter = null;
    }

    private static Stream<Arguments> numericTypesProvider() {
        return Stream.of(// Long
        Arguments.of(123L, 123L), // Integer (auto‑boxed)
        Arguments.of(456, 456L), // Double (truncated)
        Arguments.of(78.9d, 78L), // BigDecimal (truncated)
        Arguments.of(new BigDecimal("12345.67"), 12345L), // BigInteger
        Arguments.of(new BigInteger("9876543210"), 9876543210L));
    }

    private static Stream<Arguments> stringInputsProvider() {
        return Stream.of(Arguments.of("42", 42L), Arguments.of("-9876543210", -9876543210L), // decimal string, truncated
        Arguments.of("123.999", 123L));
    }

    private static Stream<Arguments> dateTimeProvider() {
        ZonedDateTime zdt = ZonedDateTime.of(2020, 5, 10, 12, 30, 0, 0, ZoneId.of("UTC"));
        return Stream.of(Arguments.of(new Date(1622548800000L), 1622548800000L), Arguments.of(Instant.ofEpochSecond(1609459200L), 1609459200000L), Arguments.of(LocalDateTime.of(2020, 1, 1, 12, 30, 15), 1577881815000L), // epoch milli for the given ZonedDateTime
        Arguments.of(zdt, 1589106600000L), Arguments.of(LocalDate.of(1970, 1, 1), 0L));
    }

    private static Stream<Arguments> boundaryValuesProvider() {
        return Stream.of(Arguments.of(Long.MAX_VALUE, Long.MAX_VALUE), Arguments.of(Long.MIN_VALUE, Long.MIN_VALUE), // Values beyond the long range are capped by the library.
        Arguments.of(new BigDecimal("9.223372036854775807E18"), Long.MAX_VALUE), Arguments.of(new BigDecimal("-9.223372036854775808E18"), Long.MIN_VALUE));
    }

    @Test
    void toAtomicLong_fromLong_returnsSameValue() {
        Long source = 123L;
        AtomicLong result = ZonedDateTimeConversions.toAtomicLong(source, converter);
        assertNotNull(result);
        assertEquals(123L, result.get());
    }

    @Test
    void toAtomicLong_fromStringNumber_returnsParsedValue() {
        String source = "456";
        AtomicLong result = ZonedDateTimeConversions.toAtomicLong(source, converter);
        assertNotNull(result);
        assertEquals(456L, result.get());
    }

    @Test
    void toAtomicLong_fromZonedDateTime_returnsEpochMilli() {
        ZonedDateTime source = ZonedDateTime.of(2020, 5, 10, 12, 30, 0, 0, ZoneId.of("UTC"));
        // Expected value is whatever the Converter would produce when converting the same object to long.
        long expected = converter.convert(source, long.class);
        AtomicLong result = ZonedDateTimeConversions.toAtomicLong(source, converter);
        assertNotNull(result);
        assertEquals(expected, result.get());
    }

    @Test
    void toAtomicLong_fromNull_returnsZero() {
        Object source = null;
        AtomicLong result = ZonedDateTimeConversions.toAtomicLong(source, converter);
        assertNotNull(result);
        assertEquals(0L, result.get());
    }

    @Test
    void toAtomicLong_fromMapRepresentation_returnsMappedValue() {
        // The Converter supports map‑based conversion for date/time types.
        // For a ZonedDateTime the map typically contains a "time" key (epoch milli) and optionally "zone".
        // arbitrary epoch milliseconds
        long epochMilli = 1_600_000_000_000L;
        var map = java.util.Map.of("time", epochMilli, "zone", "UTC");
        long expected = converter.convert(map, long.class);
        AtomicLong result = ZonedDateTimeConversions.toAtomicLong(map, converter);
        assertNotNull(result);
        assertEquals(expected, result.get());
    }

    @ParameterizedTest(name = "Numeric conversion: {0} → {1}")
    @MethodSource("numericTypesProvider")
    void toAtomicLong_numericTypes(Object source, long expected) {
        AtomicLong result = ZonedDateTimeConversions.toAtomicLong(source, converter);
        assertNotNull(result);
        assertEquals(expected, result.get());
    }

    @ParameterizedTest(name = "String conversion: \"{0}\" → {1}")
    @MethodSource("stringInputsProvider")
    void toAtomicLong_stringInputs(String source, long expected) {
        AtomicLong result = ZonedDateTimeConversions.toAtomicLong(source, converter);
        assertNotNull(result);
        assertEquals(expected, result.get());
    }

    @Test
    void toAtomicLong_stringNonNumeric_throwsNumberFormatException() {
        String source = "notANumber";
        assertThrows(NumberFormatException.class, () -> ZonedDateTimeConversions.toAtomicLong(source, converter));
    }

    @Test
    void toAtomicLong_nullSource_nullConverter_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ZonedDateTimeConversions.toAtomicLong(null, null));
    }

    @Test
    void toAtomicLong_nullSource_customConverter_returnsZero() {
        // custom converter that treats null as 0L
        Converter mock = mock(Converter.class);
        when(mock.convert(eq(null), eq(Long.class))).thenReturn(0L);
        AtomicLong result = ZonedDateTimeConversions.toAtomicLong(null, mock);
        assertNotNull(result);
        assertEquals(0L, result.get());
    }

    @ParameterizedTest(name = "Date/Time conversion: {0} → {1}")
    @MethodSource("dateTimeProvider")
    void toAtomicLong_dateTimeInputs(Object source, long expected) {
        // Use the real converter to compute the expected value – ensures consistency with library logic.
        long expectedFromConverter = converter.convert(source, long.class);
        assertEquals(expected, expectedFromConverter, "Sanity check of expected value");
        AtomicLong result = ZonedDateTimeConversions.toAtomicLong(source, converter);
        assertNotNull(result);
        assertEquals(expectedFromConverter, result.get());
    }

    @ParameterizedTest(name = "Boundary conversion: {0} → {1}")
    @MethodSource("boundaryValuesProvider")
    void toAtomicLong_boundaryValues(Object source, long expected) {
        AtomicLong result = ZonedDateTimeConversions.toAtomicLong(source, converter);
        assertNotNull(result);
        assertEquals(expected, result.get());
    }

    @Test
    void toAtomicLong_unsupportedObject_throwsIllegalArgumentException() {
        Object source = new Object();
        assertThrows(IllegalArgumentException.class, () -> ZonedDateTimeConversions.toAtomicLong(source, converter));
    }

    @Test
    void toAtomicLong_unsupportedArray_throwsIllegalArgumentException() {
        int[] source = { 1, 2, 3 };
        assertThrows(IllegalArgumentException.class, () -> ZonedDateTimeConversions.toAtomicLong(source, converter));
    }

    @Test
    void toAtomicLong_customConverter_handlesMyCustomNumber() {
        MyCustomNumber source = new MyCustomNumber(555);
        Converter mock = mock(Converter.class);
        when(mock.convert(eq(source), eq(Long.class))).thenReturn(555L);
        AtomicLong result = ZonedDateTimeConversions.toAtomicLong(source, mock);
        assertNotNull(result);
        assertEquals(555L, result.get());
    }

    @Test
    void toAtomicLong_lenientStringConverter_stripsNonDigits() {
        String source = "123XYZ";
        Converter mock = mock(Converter.class);
        // The lenient converter pretends to strip non‑digit characters before parsing.
        when(mock.convert(eq(source), eq(Long.class))).thenReturn(123L);
        AtomicLong result = ZonedDateTimeConversions.toAtomicLong(source, mock);
        assertNotNull(result);
        assertEquals(123L, result.get());
    }

    @Test
    void toAtomicLong_doubleNaN_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> ZonedDateTimeConversions.toAtomicLong(Double.NaN, converter));
    }

    @Test
    void toAtomicLong_doublePositiveInfinity_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> ZonedDateTimeConversions.toAtomicLong(Double.POSITIVE_INFINITY, converter));
    }

    @Test
    void toAtomicLong_booleanValue_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> ZonedDateTimeConversions.toAtomicLong(true, converter));
    }

    @Test
    void toAtomicLong_uuid_throwsIllegalArgumentException() {
        UUID source = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> ZonedDateTimeConversions.toAtomicLong(source, converter));
    }

    @Test
    void toAtomicLong_genericMap_throwsIllegalArgumentException() {
        Map<String, Object> source = new HashMap<>();
        source.put("foo", "bar");
        assertThrows(IllegalArgumentException.class, () -> ZonedDateTimeConversions.toAtomicLong(source, converter));
    }

    @Test
    void toAtomicLong_yearObject_throwsIllegalArgumentException() {
        Year source = Year.of(2025);
        assertThrows(IllegalArgumentException.class, () -> ZonedDateTimeConversions.toAtomicLong(source, converter));
    }

    @Test
    void toAtomicLong_mapWithWrongKey_throwsIllegalArgumentException() {
        Map<String, Object> source = Collections.singletonMap("someKey", ZonedDateTime.now());
        assertThrows(IllegalArgumentException.class, () -> ZonedDateTimeConversions.toAtomicLong(source, converter));
    }

    @Test
    void toAtomicLong_mapWithCorrectKeyButBadValue_throwsIllegalArgumentException() {
        Map<String, Object> source = Collections.singletonMap(MapConversions.ZONED_DATE_TIME, "not a date");
        assertThrows(IllegalArgumentException.class, () -> ZonedDateTimeConversions.toAtomicLong(source, converter));
    }

    @Test
    void toAtomicLong_converterThrowsRuntimeException_propagates() {
        Converter mock = mock(Converter.class);
        when(mock.convert(any(), eq(Long.class))).thenThrow(new RuntimeException("boom"));
        assertThrows(RuntimeException.class, () -> ZonedDateTimeConversions.toAtomicLong("123", mock));
    }

    @Test
    void toAtomicLong_converterReturnsNullForNullSource_throwsNullPointerException() {
        Converter mock = mock(Converter.class);
        when(mock.convert(eq(null), eq(Long.class))).thenReturn(null);
        assertThrows(NullPointerException.class, () -> ZonedDateTimeConversions.toAtomicLong(null, mock));
    }
}
