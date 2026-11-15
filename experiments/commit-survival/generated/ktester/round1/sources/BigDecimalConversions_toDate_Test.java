package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BigDecimalConversions_toDate_Test {

    /**
     * Shared Converter instance used by all tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // ConverterOptions has a public no‑arg constructor that creates an empty options set.
        // Using a real Converter (instead of a mock) ensures the internal conversion logic
        // exercised by BigDecimalConversions works as intended.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – not strictly necessary but keeps the pattern tidy.
        converter = null;
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> successCases() {
        ZoneId originalDefault = ZoneId.systemDefault();
        ???;
    }

    @Test
    void toDate_givenInstant_returnsEquivalentDate() {
        // GIVEN
        Instant now = Instant.now();
        // WHEN
        Date result = BigDecimalConversions.toDate(now, converter);
        // THEN
        assertEquals(Date.from(now), result, "Converting an Instant should produce a Date with the same instant value");
    }

    @Test
    void toDate_givenDate_returnsSameInstance() {
        // GIVEN
        Date original = new Date();
        // WHEN
        Date result = BigDecimalConversions.toDate(original, converter);
        // THEN
        assertEquals(original, result, "Converting a java.util.Date should return an equal Date (same epoch millis)");
    }

    @Test
    void toDate_givenNull_returnsNull() {
        // GIVEN
        Object source = null;
        // WHEN
        Date result = BigDecimalConversions.toDate(source, converter);
        // THEN
        assertNull(result, "Converting a null source should return null");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("successCases")
    void toDate_successfulConversions(Object from, Instant expectedInstant, String description) {
        Date result = BigDecimalConversions.toDate(from, converter);
        assertNotNull(result, "Resulting Date must not be null");
        assertEquals(Date.from(expectedInstant), result, () -> "Conversion of " + description + " did not yield the expected instant");
        if (from instanceof Date && !(from instanceof java.sql.Date)) {
            assertNotSame(from, result, "A new Date instance should be created, not the original reference");
        }
    }

    @Test
    void toDate_givenNullSource_returnsNull() {
        assertNull(BigDecimalConversions.toDate(null, converter), "Converting a null source should return null");
    }

    @Test
    void toDate_givenNullConverter_throwsNPE() {
        Date src = new Date(1_600_000_000_000L);
        assertThrows(NullPointerException.class, () -> BigDecimalConversions.toDate(src, null), "Passing a null converter must result in NullPointerException");
    }

    @Test
    void toDate_givenLocalTime_throwsIllegalArgumentException() {
        LocalTime lt = LocalTime.of(12, 34, 56, 789_000_000);
        assertThrows(IllegalArgumentException.class, () -> BigDecimalConversions.toDate(lt, converter), "LocalTime has no date component and should cause IllegalArgumentException");
    }

    @Test
    void toDate_givenBoolean_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> BigDecimalConversions.toDate(Boolean.TRUE, converter));
    }

    @Test
    void toDate_givenDouble_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> BigDecimalConversions.toDate(Double.valueOf(1_600_000_000_000.0), converter));
    }

    @Test
    void toDate_givenUUID_throwsIllegalArgumentException() {
        UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        assertThrows(IllegalArgumentException.class, () -> BigDecimalConversions.toDate(uuid, converter));
    }

    @Test
    void toDate_givenMalformedString_throwsDateTimeParseException() {
        String bad = "2020-13-40T99:99:99Z";
        assertThrows(DateTimeParseException.class, () -> BigDecimalConversions.toDate(bad, converter));
    }

    @Test
    void toDate_givenEmptyString_throwsDateTimeParseException() {
        assertThrows(DateTimeParseException.class, () -> BigDecimalConversions.toDate("", converter));
    }
}
