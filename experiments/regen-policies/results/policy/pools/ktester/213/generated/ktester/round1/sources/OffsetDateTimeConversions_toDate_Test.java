package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class OffsetDateTimeConversions_toDate_Test {

    /**
     * Shared {@link Converter} used by all test cases.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The real Converter is sufficient for the scenarios that do not require a mock.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – not strictly necessary but keeps the pattern tidy.
        converter = null;
    }

    private static Stream<Arguments> numericWrapperProvider() {
        return Stream.of(// Long – already covered, but kept for completeness
        Arguments.of(Long.valueOf(123L), 123L), // Integer
        Arguments.of(Integer.valueOf(789), 789L), // Short
        Arguments.of(Short.valueOf((short) 12), 12L), // Byte
        Arguments.of(Byte.valueOf((byte) 5), 5L), // Double – fractional part truncated
        Arguments.of(Double.valueOf(1234.56), 1234L), // Float – fractional part truncated
        Arguments.of(Float.valueOf(987.65f), 987L), // BigInteger
        Arguments.of(new BigInteger("456"), 456L), // BigDecimal – fractional part truncated
        Arguments.of(new BigDecimal("321.99"), 321L));
    }

    private static Stream<Arguments> unsupportedSourceProvider() {
        return Stream.of(Arguments.of(Boolean.TRUE), Arguments.of(new Object()));
    }

    @Test
    void toDate_givenNull_returnsEpochZeroDate() {
        Date result = OffsetDateTimeConversions.toDate(null, converter);
        assertNotNull(result, "Resulting Date should never be null");
        assertEquals(0L, result.getTime(), "Null input should map to epoch 0");
    }

    @Test
    void toDate_givenDate_returnsSameInstant() {
        long expectedMillis = System.currentTimeMillis();
        Date source = new Date(expectedMillis);
        Date result = OffsetDateTimeConversions.toDate(source, converter);
        assertEquals(expectedMillis, result.getTime(), "When source is a java.util.Date the epoch milliseconds must be unchanged");
    }

    @Test
    void toDate_givenOffsetDateTime_convertsCorrectly() {
        OffsetDateTime odt = OffsetDateTime.of(2023, 5, 10, 12, 30, 15, 0, ZoneOffset.UTC);
        long expectedMillis = odt.toInstant().toEpochMilli();
        Date result = OffsetDateTimeConversions.toDate(odt, converter);
        assertEquals(expectedMillis, result.getTime(), "OffsetDateTime should be converted to the same epoch milliseconds as its Instant");
    }

    @Test
    void toDate_givenMapWithTimeKey_convertsCorrectly() {
        // arbitrary epoch value
        long expectedMillis = 1_640_995_200_000L;
        Map<String, Object> map = new HashMap<>();
        map.put("time", expectedMillis);
        Date result = OffsetDateTimeConversions.toDate(map, converter);
        assertEquals(expectedMillis, result.getTime(), "Map containing a \"time\" key should be converted to a Date with that epoch");
    }

    @Test
    void toDate_givenLong_convertsDirectly() {
        long expectedMillis = 1_600_000_000_000L;
        Long source = Long.valueOf(expectedMillis);
        Date result = OffsetDateTimeConversions.toDate(source, converter);
        assertEquals(expectedMillis, result.getTime(), "A Long source should be interpreted as epoch milliseconds directly");
    }

    // -----------------------------------------------------------------
    @Test
    void toDate_nullSource_nullConverter_throwsNPE() {
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toDate(null, null), "Both arguments null should result in NullPointerException");
    }

    @Test
    void toDate_nullSource_nonNullConverter_throwsNPE() {
        Converter mockConverter = new Converter(new ConverterOptions());
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toDate(null, mockConverter), "Null source with a non‑null converter should still throw NullPointerException");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] from={0} → expectedMillis={1}")
    @MethodSource("numericWrapperProvider")
    void toDate_numericWrappers_convertToDate(Object from, long expectedMillis) {
        Date result = OffsetDateTimeConversions.toDate(from, converter);
        assertEquals(expectedMillis, result.getTime(), () -> String.format("Numeric wrapper %s should be converted to epoch %d", from, expectedMillis));
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] from={0} should throw IllegalArgumentException")
    @MethodSource("unsupportedSourceProvider")
    void toDate_unsupportedSource_throwsIllegalArgumentException(Object from) {
        assertThrows(IllegalArgumentException.class, () -> OffsetDateTimeConversions.toDate(from, converter), () -> "Source type " + from.getClass().getSimpleName() + " must not be convertible");
    }

    // -----------------------------------------------------------------
    @Test
    void toDate_stringNotANumber_throwsNumberFormatException() {
        String badNumber = "not-a-number";
        assertThrows(NumberFormatException.class, () -> OffsetDateTimeConversions.toDate(badNumber, converter), "A non‑numeric string should cause NumberFormatException");
    }

    @Test
    void toDate_invalidIsoDateString_throwsDateTimeParseException() {
        // month 13 is invalid
        String invalidIso = "2021-13-01T00:00:00Z";
        assertThrows(DateTimeParseException.class, () -> OffsetDateTimeConversions.toDate(invalidIso, converter), "An invalid ISO‑8601 date string should trigger DateTimeParseException");
    }

    @Test
    void toDate_numericStringOutOfRange_throwsNumberFormatException() {
        // Long.MAX_VALUE + 1
        String outOfRange = "9223372036854775808";
        assertThrows(NumberFormatException.class, () -> OffsetDateTimeConversions.toDate(outOfRange, converter), "A numeric string exceeding Long.MAX_VALUE must cause NumberFormatException");
    }

    // -----------------------------------------------------------------
    @Test
    void toDate_offsetDateTimeWithoutConverter_throwsNPE() {
        OffsetDateTime now = OffsetDateTime.now();
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toDate(now, null), "OffsetDateTime conversion requires a non‑null Converter; null should raise NPE");
    }
}
