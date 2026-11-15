package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;
import org.mockito.Mockito;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class BigIntegerConversions_toSqlDate_Test {

    /**
     * Shared {@link Converter} instance used by all test methods.
     */
    private static Converter sharedConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // Use default options – the built‑in conversions are sufficient for the tests.
        sharedConverter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static converter reference.
        sharedConverter = null;
    }

    /* --------------------------------------------------------------
     *  Parameterized happy‑path conversions (no custom converter)
     * -------------------------------------------------------------- */
    private static Stream<Arguments> happyPathProvider() {
        return Stream.of(// java.sql.Timestamp
        Arguments.of(Timestamp.valueOf("2021-01-01 00:00:00.000"), null, 1609459200000L, "Timestamp at UTC midnight"), // java.time.Instant
        Arguments.of(Instant.ofEpochMilli(1609459200000L), null, 1609459200000L, "Instant with explicit epoch millis"), // java.time.ZonedDateTime (UTC)
        Arguments.of(ZonedDateTime.of(2021, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), null, 1609459200000L, "ZonedDateTime at UTC midnight"), // java.time.OffsetDateTime (UTC)
        Arguments.of(OffsetDateTime.of(2021, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), null, 1609459200000L, "OffsetDateTime at UTC midnight"), // java.util.Calendar (UTC)
        Arguments.of(createUtcCalendar(1609459200000L), null, 1609459200000L, "Calendar set to epoch millis in UTC"), // Long (epoch millis)
        Arguments.of(1609459200000L, null, 1609459200000L, "Long representing epoch millis"), // String containing epoch millis
        Arguments.of("1609459200000", null, 1609459200000L, "String numeric epoch millis"), // BigInteger within long range
        Arguments.of(new BigInteger("1609459200000"), null, 1609459200000L, "BigInteger that fits in a long"));
    }

    private static Calendar createUtcCalendar(long epochMilli) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(epochMilli);
        return cal;
    }

    @Test
    void toSqlDate_givenInstant_returnsDateWithSameEpochMillis() {
        // given
        Instant now = Instant.now();
        // when
        Date result = BigIntegerConversions.toSqlDate(now, sharedConverter);
        // then
        assertNotNull(result, "Resulting Date should not be null");
        assertEquals(now.toEpochMilli(), result.getTime(), "The Date's epoch milli should match the source Instant's epoch milli");
    }

    @Test
    void toSqlDate_givenUtilDate_returnsDateWithSameEpochMillis() {
        // given
        // current time
        Date source = new Date();
        // when
        Date result = BigIntegerConversions.toSqlDate(source, sharedConverter);
        // then
        assertNotNull(result, "Resulting Date should not be null");
        assertEquals(source.getTime(), result.getTime(), "The Date's epoch milli should match the source Date's epoch milli");
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("happyPathProvider")
    void toSqlDate_happyPath_convertsVariousSources(Object source, Converter converter, long expectedEpochMilli, String description) {
        // Use the shared real converter when none is supplied.
        Converter used = (converter == null) ? sharedConverter : converter;
        Date result = BigIntegerConversions.toSqlDate(source, used);
        assertNotNull(result, "Result must not be null for " + description);
        assertEquals(expectedEpochMilli, result.getTime(), "Incorrect epoch millis for " + description);
    }

    /* --------------------------------------------------------------
     *  Custom‑converter scenarios
     * -------------------------------------------------------------- */
    @Test
    void toSqlDate_customConverter_mapsCustomStringToInstant() {
        // Arrange: mock converter that knows how to turn "custom" into a fixed Instant.
        Converter mockConverter = mock(Converter.class);
        Instant fixed = Instant.ofEpochMilli(1609459200000L);
        when(mockConverter.convert(eq("custom"), eq(Instant.class))).thenReturn(fixed);
        // Act
        Date result = BigIntegerConversions.toSqlDate("custom", mockConverter);
        // Assert
        assertNotNull(result);
        assertEquals(fixed.toEpochMilli(), result.getTime(), "Date should reflect the Instant returned by the custom converter");
        verify(mockConverter).convert(eq("custom"), eq(Instant.class));
    }

    @Test
    void toSqlDate_customConverterThrowsRuntimeException_propagates() {
        // Arrange: mock converter that throws a RuntimeException.
        Converter mockConverter = mock(Converter.class);
        RuntimeException ex = new RuntimeException("conversion failure");
        when(mockConverter.convert(any(), eq(Instant.class))).thenThrow(ex);
        // Act & Assert
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> BigIntegerConversions.toSqlDate("any", mockConverter));
        assertSame(ex, thrown, "The original RuntimeException must be propagated unchanged");
    }

    @Test
    void toSqlDate_customConverterReturnsNull_resultsInNPE() {
        // Arrange: mock converter that returns null.
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.convert(any(), eq(Instant.class))).thenReturn(null);
        // Act & Assert
        NullPointerException npe = assertThrows(NullPointerException.class, () -> BigIntegerConversions.toSqlDate("nullResult", mockConverter));
        assertNotNull(npe, "A NullPointerException is expected when the converter returns null");
    }

    /* --------------------------------------------------------------
     *  Invalid‑input scenarios that must raise IllegalArgumentException
     * -------------------------------------------------------------- */
    @Test
    void toSqlDate_bigIntegerExceedingLongRange_throwsIllegalArgumentException() {
        // Long.MAX_VALUE + 1
        BigInteger tooBig = new BigInteger("9223372036854775808");
        IllegalArgumentException iae = assertThrows(IllegalArgumentException.class, () -> BigIntegerConversions.toSqlDate(tooBig, sharedConverter));
        assertTrue(iae.getMessage().contains("Long"), "Exception message should mention Long range");
    }

    @Test
    void toSqlDate_nullSource_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> BigIntegerConversions.toSqlDate(null, sharedConverter));
    }

    @Test
    void toSqlDate_nonNumericString_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> BigIntegerConversions.toSqlDate("notANumber", sharedConverter));
    }

    @Test
    void toSqlDate_localDateUnsupported_throwsIllegalArgumentException() {
        LocalDate ld = LocalDate.of(2021, 1, 1);
        assertThrows(IllegalArgumentException.class, () -> BigIntegerConversions.toSqlDate(ld, sharedConverter));
    }

    @Test
    void toSqlDate_localDateTimeUnsupported_throwsIllegalArgumentException() {
        LocalDateTime ldt = LocalDateTime.of(2021, 1, 1, 0, 0, 0);
        assertThrows(IllegalArgumentException.class, () -> BigIntegerConversions.toSqlDate(ldt, sharedConverter));
    }

    @Test
    void toSqlDate_localTimeUnsupported_throwsIllegalArgumentException() {
        LocalTime lt = LocalTime.of(12, 34, 56);
        assertThrows(IllegalArgumentException.class, () -> BigIntegerConversions.toSqlDate(lt, sharedConverter));
    }

    @Test
    void toSqlDate_booleanUnsupported_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> BigIntegerConversions.toSqlDate(Boolean.TRUE, sharedConverter));
    }
}
