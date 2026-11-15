package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
// added import for java.sql.Date
import java.sql.Date;

@ExtendWith(MockitoExtension.class)
class MapConversions_toYear_Test {

    /**
     * Shared {@link Converter} used by all test cases.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter class requires a ConverterOptions instance.
        // Using the default constructor of ConverterOptions provides the standard configuration.
//         converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Explicitly release the reference; not strictly required but keeps the pattern clear.
        converter = null;
    }

    private static Stream<Arguments> successfulConversionProvider() {
        return Stream.of(// Direct Year instance
        Arguments.of(Year.of(2023), Year.of(2023)), // Integer, Long and String representations
        Arguments.of(2024, Year.of(2024)), Arguments.of("2025", Year.of(2025)), // Maps with different recognised keys
        Arguments.of(mapOf("year", 2026), Year.of(2026)), Arguments.of(mapOf("value", "2027"), Year.of(2027)), Arguments.of(mapOf("_v", 2028L), Year.of(2028)), // Date / Calendar based sources
        // java.util.Date epoch
        Arguments.of(new Date(0L), Year.of(1970)), Arguments.of(calendarOf(2030, Calendar.JANUARY, 1), Year.of(2030)), // Additional successful conversions
        Arguments.of(YearMonth.of(2029, 5), Year.of(2029)), Arguments.of(LocalDate.of(2031, 12, 31), Year.of(2031)), Arguments.of(LocalDateTime.of(2032, 1, 1, 0, 0), Year.of(2032)), Arguments.of(ZonedDateTime.of(2033, 6, 15, 12, 0, 0, 0, ZoneId.of("UTC")), Year.of(2033)), Arguments.of(OffsetDateTime.of(2034, 7, 20, 10, 30, 0, 0, ZoneOffset.UTC), Year.of(2034)), Arguments.of(Instant.ofEpochSecond(0), Year.of(1970)), Arguments.of(new Timestamp(0L), Year.of(1970)), Arguments.of(java.sql.Date.valueOf("2040-01-01"), Year.of(2040)), // Maps with string / long values
        Arguments.of(mapOf("year", "2050"), Year.of(2050)), Arguments.of(mapOf("value", 2051L), Year.of(2051)), Arguments.of(mapOf("_v", "2052"), Year.of(2052)), // String with surrounding whitespace
        Arguments.of(" 2053 ", Year.of(2053)), // Extreme negative year expressed as a string
        Arguments.of("-2147483648", Year.of(-2147483648)), // Boundary values
        Arguments.of(Year.of(-1), Year.of(-1)), Arguments.of(Year.of(0), Year.of(0)), Arguments.of(Year.of(999_999_999), Year.of(999_999_999)), Arguments.of("-100", Year.of(-100)));
    }

    private static Stream<Arguments> mapPrecedenceProvider() {
        return Stream.of(Arguments.of(mapOf("year", 2100, "value", 2099, "_v", 2098), Year.of(2100)), Arguments.of(mapOf("value", 2101, "_v", 2102), Year.of(2101)), Arguments.of(mapOf("_v", 2103), Year.of(2103)));
    }

    private static Stream<Arguments> errorCaseProvider() {
        return Stream.of(// Null source – the library throws IllegalArgumentException (or NPE). We assert IllegalArgumentException.
        Arguments.of(null, IllegalArgumentException.class), // Unparseable string
        Arguments.of("not-a-year", DateTimeParseException.class), // Map without any recognised key
        Arguments.of(mapOf("unknown", 2029), IllegalArgumentException.class), // Unsupported type (Boolean)
        Arguments.of(Boolean.TRUE, IllegalArgumentException.class), // Out‑of‑range year values – Year.of throws DateTimeException
        Arguments.of(1_000_000_000, DateTimeException.class), Arguments.of(-1_000_000_000, DateTimeException.class), // Empty / whitespace‑only strings
        Arguments.of("", DateTimeParseException.class), Arguments.of("   ", DateTimeParseException.class), // Map with supported key but null value
        Arguments.of(mapOf("year", null), IllegalArgumentException.class), // Map with supported key but Boolean value
        Arguments.of(mapOf("year", true), IllegalArgumentException.class), // Map with supported key but array value
        Arguments.of(mapOf("value", new int[] { 2025 }), IllegalArgumentException.class), // Plain Object – unsupported source type
        Arguments.of(new Object(), IllegalArgumentException.class), // Primitive array – unsupported source type
        Arguments.of(new int[] { 2025 }, IllegalArgumentException.class));
    }

    /**
     * Creates a {@link Calendar} set to the given year/month/day (time set to midnight).
     */
    private static Calendar calendarOf(int year, int month, int day) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.clear();
        cal.set(year, month, day);
        return cal;
    }

    /**
     * Safe string representation for debugging (handles nulls).
     */
    private static String safeToString(Object obj) {
        return obj == null ? "null" : obj.toString();
    }

    @Test
    void toYear_fromMapWithYearKey_IntegerValue() {
        Map<String, Object> map = new HashMap<>();
        map.put("year", 2020);
        Year result = MapConversions.toYear(map, converter);
        assertEquals(Year.of(2020), result, "Map with key \"year\" and Integer value should be converted to the corresponding Year");
    }

    @Test
    void toYear_fromMapWithValueKey_StringValue() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", "2021");
        Year result = MapConversions.toYear(map, converter);
        assertEquals(Year.of(2021), result, "Map with key \"value\" and String value should be converted to the corresponding Year");
    }

    @Test
    void toYear_fromMapWithVKey_LongValue() {
        Map<String, Object> map = new HashMap<>();
        map.put("_v", 2022L);
        Year result = MapConversions.toYear(map, converter);
        assertEquals(Year.of(2022), result, "Map with key \"_v\" and Long value should be converted to the corresponding Year");
    }

    @Test
    void toYear_whenSourceIsAlreadyYear_returnsSameInstance() {
        Year source = Year.of(1999);
        Year result = MapConversions.toYear(source, converter);
        // The conversion should be a no‑op – the same Year instance (or an equal one) is returned.
        assertEquals(source, result, "When the source object is already a Year, the method should return an equal Year");
    }

    @Test
    void toYear_whenSourceIsNull_returnsNull() {
        Year result = MapConversions.toYear(null, converter);
        assertNull(result, "A null source must result in a null Year");
    }

    @ParameterizedTest(name = "[{index}] from={0} → expected={1}")
    @MethodSource("successfulConversionProvider")
    void toYear_successfulConversions(Object from, Year expected) {
        Year result = MapConversions.toYear(from, converter);
        assertEquals(expected, result, () -> "Conversion of " + safeToString(from) + " should produce " + expected);
    }

    @ParameterizedTest(name = "[{index}] map={0} → expected={1}")
    @MethodSource("mapPrecedenceProvider")
    void toYear_mapKeyPrecedence(Map<String, Object> map, Year expected) {
        Year result = MapConversions.toYear(map, converter);
        assertEquals(expected, result, () -> "Map " + map + " should resolve to " + expected + " according to key precedence");
    }

    @ParameterizedTest(name = "[{index}] from={0} → throws={1}")
    @MethodSource("errorCaseProvider")
    void toYear_errorCases(Object from, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> MapConversions.toYear(from, converter), () -> "Conversion of " + safeToString(from) + " should throw " + expectedException.getSimpleName());
    }

    @Test
    void toYear_converterIsNull_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> MapConversions.toYear(2025, null), "Passing a null Converter should result in NullPointerException");
    }

    /**
     * Convenience factory for a {@link Map} with var‑args key/value pairs.
     */
    @SafeVarargs
    private static Map<String, Object> mapOf(Object... keyValues) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return map;
    }
}