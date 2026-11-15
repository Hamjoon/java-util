package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;

@ExtendWith(MockitoExtension.class)
class MapConversions_toString_Test {

    /**
     * Mocked Converter that will be used for all test cases.
     */
    @Mock
    private Converter mockConverter;

    /**
     * A very small {@link Converter} implementation used for the majority of test cases.
     * It simply returns the supplied object unchanged.  When the object is a {@link Map},
     * its {@code toString()} representation is returned – this matches the expected
     * strings defined in the test data.
     */
    private static class SimpleConverter implements Converter {

        @SuppressWarnings("unchecked")
        @Override
        public <T> T convert(Object from, Class<T> toClass) {
            if (from == null) {
                return null;
            }
            // If the target type is String and the source is a Map, return the map's toString()
            if (toClass == String.class && from instanceof Map) {
                return (T) from.toString();
            }
            // For any other case, just cast/return the object (e.g., String, Number, etc.)
            return (T) from;
        }
    }

    /**
     * Converter that always throws the supplied {@link RuntimeException} when {@code convert}
     * is called.  Used to verify exception propagation.
     */
    private static class ThrowingConverter implements Converter {

        private final RuntimeException toThrow;

        ThrowingConverter(RuntimeException toThrow) {
            this.toThrow = toThrow;
        }

        @Override
        public <T> T convert(Object from, Class<T> toClass) {
            throw toThrow;
        }
    }

    /**
     * Initialise Mockito annotations before each test.
     */
    @BeforeEach
    void setupBeforeEach() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * Data provider for the "Null handling" group.
     */
    static List<Arguments> nullHandlingProvider() {
        return List.of(// from = null, converter = SimpleConverter -> expect null
        Arguments.of(null, new SimpleConverter(), null, "null source returns null"), // from = new Object(), converter = null -> expect NPE
        Arguments.of(new Object(), null, NullPointerException.class, "null converter throws NPE"));
    }

    /**
     * Data provider for the "Simple scalar types" group.
     */
    static List<Arguments> simpleScalarProvider() {
        return List.of(Arguments.of("plain string", new SimpleConverter(), "plain string", "String is returned unchanged"), Arguments.of(12345, new SimpleConverter(), "12345", "Integer converted to its textual representation"), Arguments.of(true, new SimpleConverter(), "true", "Boolean converted to its textual representation"));
    }

    /**
     * Data provider for the "Map conversion" group.
     */
    static List<Arguments> mapConversionProvider() {
        Map<String, Object> source = Map.of("key1", "value1", "key2", 2);
        return List.of(Arguments.of(source, new SimpleConverter(), "{key1=value1, key2=2}", "Map's toString representation"));
    }

    /**
     * Data provider for the "Locale handling" group.
     */
    static List<Arguments> localeProvider() {
        return List.of(Arguments.of(Locale.US, new SimpleConverter(), "{language=en, country=US, script=, variant=}", "Locale.US representation"), Arguments.of(new Locale("fr", "CA", "POSIX"), new SimpleConverter(), "{language=fr, country=CA, script=, variant=POSIX}", "Locale with variant"));
    }

    /**
     * Data provider for the "Date / Time handling" group.
     */
    static List<Arguments> dateTimeProvider() {
        return List.of(Arguments.of(new Date(0L), new SimpleConverter(), "{epochMillis=0}", "java.util.Date -> epochMillis"), Arguments.of(LocalDate.of(2023, 3, 15), new SimpleConverter(), "{year=2023, month=3, day=15}", "LocalDate representation"), Arguments.of(LocalTime.of(14, 30, 45, 123456789), new SimpleConverter(), "{hour=14, minute=30, second=45, nanos=123456789}", "LocalTime representation"), Arguments.of(ZonedDateTime.parse("2023-03-15T14:30:45+02:00[Europe/Paris]"), new SimpleConverter(), "{date={year=2023, month=3, day=15}, time={hour=14, minute=30, second=45, nanos=0}, zone=Europe/Paris}", "ZonedDateTime broken into date, time, zone"));
    }

    /**
     * Data provider for the "Temporal offset types" group.
     */
    static List<Arguments> temporalOffsetProvider() {
        return List.of(Arguments.of(OffsetTime.of(14, 30, 0, 0, ZoneOffset.ofHoursMinutes(5, 30)), new SimpleConverter(), "{hour=14, minute=30, second=0, nanos=0, offsetHour=5, offsetMinute=30}", "OffsetTime includes offset fields"), Arguments.of(OffsetDateTime.parse("2023-03-15T14:30:45+05:30"), new SimpleConverter(), "{date={year=2023, month=3, day=15}, time={hour=14, minute=30, second=45, nanos=0}, offsetHour=5, offsetMinute=30}", "OffsetDateTime with date, time and offset"));
    }

    /**
     * Data provider for the "Period / Duration handling" group.
     */
    static List<Arguments> periodDurationProvider() {
        return List.of(Arguments.of(Period.of(2, 5, 10), new SimpleConverter(), "{years=2, months=5, days=10}", "Period representation"), Arguments.of(Duration.ofSeconds(3661, 500_000_000), new SimpleConverter(), "{seconds=3661, nanos=500000000}", "Duration representation"));
    }

    /**
     * Data provider for the "UUID / URI / URL handling" group.
     */
    static List<Arguments> uuidUriUrlProvider() {
        return List.of(Arguments.of(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"), new SimpleConverter(), "{mostSigBits=1314564453825188563, leastSigBits=-8192775951544840294}", "UUID broken into bits"), Arguments.of(URI.create("https://example.com/path?query=1"), new SimpleConverter(), "{URI=https://example.com/path?query=1}", "URI stored under key \"URI\""), Arguments.of(createUrl("https://example.com"), new SimpleConverter(), "{URL=https://example.com}", "URL stored under key \"URL\""));
    }

    private static URL createUrl(String spec) {
        try {
            return new URL(spec);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Data provider for the "Throwable handling" group.
     */
    static List<Arguments> throwableProvider() {
        IllegalArgumentException cause = new NullPointerException("null cause");
        IllegalArgumentException ex = new IllegalArgumentException("bad argument", cause);
        return List.of(Arguments.of(ex, new SimpleConverter(), "{class=java.lang.IllegalArgumentException, message=bad argument, cause=java.lang.NullPointerException, causeMessage=null cause}", "Throwable converted to map with class, message, cause, causeMessage"));
    }

    /**
     * Data provider for the "Unsupported type fallback" group.
     */
    static List<Arguments> unsupportedFallbackProvider() {
        Object obj = new Object();
        return List.of(Arguments.of(obj, new SimpleConverter(), obj.toString(), "Fallback to Object.toString()"));
    }

    /**
     * Data provider for the "Atomic types" group.
     */
    static List<Arguments> atomicProvider() {
        return List.of(Arguments.of(new AtomicBoolean(true), new SimpleConverter(), "{_v=true}", "AtomicBoolean representation"), Arguments.of(new AtomicInteger(42), new SimpleConverter(), "{_v=42}", "AtomicInteger representation"), Arguments.of(new AtomicLong(123456789L), new SimpleConverter(), "{_v=123456789}", "AtomicLong representation"));
    }

    /**
     * Data provider for the "Big numeric types" group.
     */
    static List<Arguments> bigNumericProvider() {
        return List.of(Arguments.of(new BigDecimal("12345.6789"), new SimpleConverter(), "{value=12345.6789}", "BigDecimal representation"), Arguments.of(new BigInteger("9876543210123456789"), new SimpleConverter(), "{value=9876543210123456789}", "BigInteger representation"));
    }

    /**
     * Data provider for the "Primitive wrappers and character" group.
     */
    static List<Arguments> primitiveWrapperProvider() {
        return List.of(Arguments.of(Byte.valueOf((byte) 7), new SimpleConverter(), "{value=7}", "Byte wrapper representation"), Arguments.of(Character.valueOf('Z'), new SimpleConverter(), "{value=Z}", "Character representation"), Arguments.of(Short.valueOf((short) 12), new SimpleConverter(), "{value=12}", "Short wrapper representation"));
    }

    /**
     * Data provider for the "Class handling" group.
     */
    static List<Arguments> classHandlingProvider() {
        return List.of(Arguments.of(String.class, new SimpleConverter(), "{class=java.lang.String}", "Class object representation"));
    }

    /**
     * Data provider for the "Temporal types – LocalDateTime, Instant, MonthDay" group.
     */
    static List<Arguments> temporalTypesProvider() {
        return List.of(Arguments.of(LocalDateTime.of(2023, 3, 15, 14, 30, 45, 123456789), new SimpleConverter(), "{date={year=2023, month=3, day=15}, time={hour=14, minute=30, second=45, nanos=123456789}}", "LocalDateTime broken into date & time maps"), Arguments.of(Instant.ofEpochSecond(10, 500_000_000), new SimpleConverter(), "{epochMillis=10500}", "Instant represented by epochMillis"), Arguments.of(MonthDay.of(12, 25), new SimpleConverter(), "{month=12, day=25}", "MonthDay representation"));
    }

    /**
     * Data provider for the "Year and YearMonth" group.
     */
    static List<Arguments> yearYearMonthProvider() {
        return List.of(Arguments.of(Year.of(2025), new SimpleConverter(), "{year=2025}", "Year representation"), Arguments.of(YearMonth.of(2025, 7), new SimpleConverter(), "{year=2025, month=7}", "YearMonth representation"));
    }

    /**
     * Data provider for the "Zone types – ZoneId and ZoneOffset" group.
     */
    static List<Arguments> zoneTypesProvider() {
        return List.of(Arguments.of(ZoneId.of("Europe/Paris"), new SimpleConverter(), "{zone=Europe/Paris}", "ZoneId representation"), Arguments.of(ZoneOffset.ofHoursMinutes(5, 30), new SimpleConverter(), "{offsetHour=5, offsetMinute=30}", "ZoneOffset representation"));
    }

    /**
     * Data provider for the "StringBuilder and StringBuffer" group.
     */
    static List<Arguments> stringBuilderBufferProvider() {
        return List.of(Arguments.of(new StringBuilder("builder"), new SimpleConverter(), "builder", "StringBuilder converted via toString()"), Arguments.of(new StringBuffer("buffer"), new SimpleConverter(), "buffer", "StringBuffer converted via toString()"));
    }

    /**
     * Data provider for the "Calendar and TimeZone handling" group.
     */
    static List<Arguments> calendarTimeZoneProvider() {
        Calendar utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        // epoch
        utcCal.setTimeInMillis(0L);
        TimeZone nyTz = TimeZone.getTimeZone("America/New_York");
        return List.of(Arguments.of(utcCal, new SimpleConverter(), "{date={year=1970, month=1, day=1}, time={hour=0, minute=0, second=0, nanos=0}, zone=UTC}", "Calendar at epoch UTC"), Arguments.of(nyTz, new SimpleConverter(), "{zone=America/New_York}", "TimeZone representation"));
    }

    /**
     * Data provider for the "Timestamp handling" group.
     */
    static List<Arguments> timestampProvider() {
        return List.of(Arguments.of(new Timestamp(1_600_000_000_000L), new SimpleConverter(), "{epochMillis=1600000000000}", "Timestamp represented by epochMillis"));
    }

    /**
     * Data provider for the "Converter exception propagation" group.
     */
    static List<Arguments> converterExceptionProvider() {
        return List.of(Arguments.of("sample", new ThrowingConverter(new NullPointerException("converter NPE")), NullPointerException.class, "Converter throws NPE – should propagate"), Arguments.of(123, new ThrowingConverter(new IllegalArgumentException("bad conversion")), IllegalArgumentException.class, "Converter throws IAE – should propagate"));
    }

    /**
     * Verify that a map containing a {@code "value"} entry is converted to a {@link String}
     * by delegating to the {@link Converter}.
     */
    @Test
    void toString_mapsValueKey_convertsViaConverter() {
        // GIVEN
        Map<String, Object> source = new HashMap<>();
        source.put("value", "original");
        // The converter is expected to receive the map (or the extracted value) and the target type.
        when(mockConverter.convert(any(), eq(String.class))).thenReturn("converted");
        // WHEN
        String result = MapConversions.toString(source, mockConverter);
        // THEN
        assertEquals("converted", result, "The result should be whatever the Converter returns");
        // Verify that the converter was indeed called – the exact argument (map vs value) is not part of the contract,
        // so we accept any Object as the first argument.
        verify(mockConverter, times(1)).convert(any(), eq(String.class));
    }

    /**
     * Verify that a plain {@link String} source is passed straight through the {@link Converter}
     * and the returned value is used.
     */
    @Test
    void toString_stringSource_convertsViaConverter() {
        // GIVEN
        String source = "already a string";
        when(mockConverter.convert(eq(source), eq(String.class))).thenReturn("convertedString");
        // WHEN
        String result = MapConversions.toString(source, mockConverter);
        // THEN
        assertEquals("convertedString", result);
        verify(mockConverter, times(1)).convert(eq(source), eq(String.class));
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("nullHandlingProvider")
    void testNullHandling(Object from, Converter converter, Class<? extends Throwable> expectedException, String description) {
        if (expectedException != null) {
            assertThrows(expectedException, () -> MapConversions.toString(from, converter), description);
        } else {
            assertNull(MapConversions.toString(from, converter), description);
        }
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("simpleScalarProvider")
    void testSimpleScalarTypes(Object from, Converter converter, String expected, String description) {
        assertEquals(expected, MapConversions.toString(from, converter), description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("mapConversionProvider")
    void testMapConversion(Object from, Converter converter, String expected, String description) {
        assertEquals(expected, MapConversions.toString(from, converter), description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("localeProvider")
    void testLocaleHandling(Object from, Converter converter, String expected, String description) {
        assertEquals(expected, MapConversions.toString(from, converter), description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("dateTimeProvider")
    void testDateTimeHandling(Object from, Converter converter, String expected, String description) {
        assertEquals(expected, MapConversions.toString(from, converter), description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("temporalOffsetProvider")
    void testTemporalOffsetTypes(Object from, Converter converter, String expected, String description) {
        assertEquals(expected, MapConversions.toString(from, converter), description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("periodDurationProvider")
    void testPeriodDurationHandling(Object from, Converter converter, String expected, String description) {
        assertEquals(expected, MapConversions.toString(from, converter), description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("uuidUriUrlProvider")
    void testUuidUriUrlHandling(Object from, Converter converter, String expected, String description) {
        assertEquals(expected, MapConversions.toString(from, converter), description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("throwableProvider")
    void testThrowableHandling(Object from, Converter converter, String expected, String description) {
        assertEquals(expected, MapConversions.toString(from, converter), description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("unsupportedFallbackProvider")
    void testUnsupportedFallback(Object from, Converter converter, String expected, String description) {
        assertEquals(expected, MapConversions.toString(from, converter), description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("atomicProvider")
    void testAtomicTypes(Object from, Converter converter, String expected, String description) {
        assertEquals(expected, MapConversions.toString(from, converter), description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("bigNumericProvider")
    void testBigNumericTypes(Object from, Converter converter, String expected, String description) {
        assertEquals(expected, MapConversions.toString(from, converter), description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("primitiveWrapperProvider")
    void testPrimitiveWrappersAndCharacter(Object from, Converter converter, String expected, String description) {
        assertEquals(expected, MapConversions.toString(from, converter), description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("classHandlingProvider")
    void testClassHandling(Object from, Converter converter, String expected, String description) {
        assertEquals(expected, MapConversions.toString(from, converter), description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("temporalTypesProvider")
    void testTemporalTypes(Object from, Converter converter, String expected, String description) {
        assertEquals(expected, MapConversions.toString(from, converter), description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("yearYearMonthProvider")
    void testYearAndYearMonth(Object from, Converter converter, String expected, String description) {
        assertEquals(expected, MapConversions.toString(from, converter), description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("zoneTypesProvider")
    void testZoneTypes(Object from, Converter converter, String expected, String description) {
        assertEquals(expected, MapConversions.toString(from, converter), description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("stringBuilderBufferProvider")
    void testStringBuilderAndBuffer(Object from, Converter converter, String expected, String description) {
        assertEquals(expected, MapConversions.toString(from, converter), description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("calendarTimeZoneProvider")
    void testCalendarAndTimeZone(Object from, Converter converter, String expected, String description) {
        assertEquals(expected, MapConversions.toString(from, converter), description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("timestampProvider")
    void testTimestampHandling(Object from, Converter converter, String expected, String description) {
        assertEquals(expected, MapConversions.toString(from, converter), description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("converterExceptionProvider")
    void testConverterExceptionPropagation(Object from, Converter converter, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> MapConversions.toString(from, converter), description);
    }
}
