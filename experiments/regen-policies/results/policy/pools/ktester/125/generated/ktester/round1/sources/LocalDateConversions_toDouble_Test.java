package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalDateConversions_toDouble_Test {

    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Use default options – no special configuration required for the tests
        converter = new Converter(new ConverterOptions());
    }

    private static Stream<Arguments> supportedTypesProvider() {
        long epochMilli = 1_622_548_800_000L;
        double epochSeconds = epochMilli / 1000.0;
        ???;
    }

    private static Stream<Arguments> additionalZonesProvider() {
        double nyExpected = 1_622_577_600.0;
        double sqlDateExpected = 1_622_505_600.0;
        long epochMilli = 1_622_548_800_000L;
        double calendarExpected = epochMilli / 1000.0;
        return Stream.of(Arguments.of("Convert ZonedDateTime with America/New_York offset (-04:00)", ZonedDateTime.of(2021, 6, 1, 12, 0, 0, 0, ZoneId.of("America/New_York")), nyExpected), Arguments.of("Convert OffsetDateTime with offset -04:00", OffsetDateTime.of(2021, 6, 1, 12, 0, 0, 0, ZoneOffset.ofHours(-4)), nyExpected), Arguments.of("Convert java.sql.Date (midnight UTC)", java.sql.Date.valueOf("2021-06-01"), sqlDateExpected), Arguments.of("Convert Calendar built in Pacific Time via epoch millis", new Calendar.Builder().setInstant(epochMilli).setTimeZone(TimeZone.getTimeZone("America/Los_Angeles")).build(), calendarExpected));
    }

    private static Stream<Arguments> numericTypesProvider() {
        long epochMilli = 1_622_548_800_000L;
        double epochSeconds = epochMilli / 1000.0;
        return Stream.of(Arguments.of("Convert AtomicLong holding epoch‑millis", new AtomicLong(epochMilli), epochSeconds), Arguments.of("Convert BigDecimal representing epoch‑millis", new BigDecimal("1622548800000"), epochSeconds), Arguments.of("Convert BigInteger representing epoch‑millis", new BigInteger("1622548800000"), epochSeconds), Arguments.of("Convert Integer interpreted as milliseconds", Integer.valueOf(1_622_548_800), 1_622_548.8));
    }

    private static Stream<Arguments> edgeCasesProvider() {
        return Stream.of(Arguments.of("Convert the epoch start (1970‑01‑01T00:00:00Z)", new Date(0L), 0.0), Arguments.of("Convert a date before the epoch (1969‑12‑31T00:00:00Z)", new Date(-86_400_000L), -86_400.0));
    }

    private static Stream<Arguments> invalidInputsProvider() {
        return Stream.of(Arguments.of("Passing null as the source object should result in NullPointerException", null, NullPointerException.class), Arguments.of("Passing an unsupported type (Boolean) should cause IllegalArgumentException", Boolean.TRUE, IllegalArgumentException.class), Arguments.of("LocalTime does not contain date information; conversion should fail", LocalTime.of(12, 0), IllegalArgumentException.class), Arguments.of("Arbitrary unsupported object should trigger IllegalArgumentException", new Object(), IllegalArgumentException.class));
    }

    @Test
    void toDouble_givenDate_returnsEpochSecondsAsDouble() {
        // GIVEN
        // 2021‑10‑01T00:00:00Z
        long epochMilli = 1_632_072_800_000L;
        Date date = new Date(epochMilli);
        // WHEN
        double result = LocalDateConversions.toDouble(date, converter);
        // THEN
        assertEquals(epochMilli / 1000.0, result, 0.000001, "Double conversion of Date should be epoch seconds");
    }

    @Test
    void toDouble_givenInstant_returnsEpochSecondsAsDouble() {
        // GIVEN
        // exact second, no nanos
        Instant instant = Instant.ofEpochSecond(123_456_789L, 0);
        // WHEN
        double result = LocalDateConversions.toDouble(instant, converter);
        // THEN
        assertEquals(123_456_789.0, result, 0.000001, "Double conversion of Instant should be epoch seconds");
    }

    @Test
    void toDouble_givenMapWithTimeKey_returnsEpochSecondsAsDouble() {
        // GIVEN
        // The map follows the convention used by the Converter for date/time types:
        // a key named "time" whose value is the epoch milliseconds.
        // 5 seconds after epoch
        long epochMilli = 5_000L;
        Map<String, Object> map = Map.of("time", epochMilli);
        // WHEN
        double result = LocalDateConversions.toDouble(map, converter);
        // THEN
        assertEquals(epochMilli / 1000.0, result, 0.000001, "Double conversion of a Map with a 'time' key should be epoch seconds");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("supportedTypesProvider")
    void toDouble_supportedTypes(String description, Object from, double expected) {
        double result = LocalDateConversions.toDouble(from, converter);
        assertEquals(expected, result, 0.000001, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("additionalZonesProvider")
    void toDouble_additionalZones(String description, Object from, double expected) {
        double result = LocalDateConversions.toDouble(from, converter);
        assertEquals(expected, result, 0.000001, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("numericTypesProvider")
    void toDouble_numericTypes(String description, Object from, double expected) {
        double result = LocalDateConversions.toDouble(from, converter);
        assertEquals(expected, result, 0.000001, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("edgeCasesProvider")
    void toDouble_edgeCases(String description, Object from, double expected) {
        double result = LocalDateConversions.toDouble(from, converter);
        assertEquals(expected, result, 0.000001, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidInputsProvider")
    void toDouble_invalidInputs(String description, Object from, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> LocalDateConversions.toDouble(from, converter), description);
    }

    @Test
    void toDouble_nullSourceAndNullConverter_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> LocalDateConversions.toDouble(null, null), "Both source object and converter are null – should throw NPE");
    }

    @Test
    void toDouble_validSourceNullConverter_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> LocalDateConversions.toDouble(new Date(1622548800000L), null), "Valid source but null converter – should throw NPE");
    }
}
