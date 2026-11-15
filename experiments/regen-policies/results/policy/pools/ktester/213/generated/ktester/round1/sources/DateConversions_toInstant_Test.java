package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import java.util.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.DateTimeException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DateConversions_toInstant_Test {

    /**
     * Converter instance required by the API – it is not used by {@code toInstant} itself,
     *  but many other conversion methods expect a non‑null instance.
     */
    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a fresh Converter with default options for each test.
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Release reference – helps GC in larger test suites.
        converter = null;
    }

    private static Stream<Arguments> provideSuccessfulConversions() {
        return Stream.of(// java.util.Date at epoch (converter is null)
        Arguments.of("java.util.Date epoch (0 ms) → Instant.EPOCH", new Date(0L), null, Instant.EPOCH), // java.util.Date with positive epoch
        Arguments.of("java.util.Date 2022‑12‑31T23:59:59Z", new Date(1_672_531_199_000L), null, Instant.parse("2022-12-31T23:59:59Z")), // java.sql.Date at epoch
        Arguments.of("java.sql.Date epoch (0 ms) → Instant.EPOCH", new java.sql.Date(0L), null, Instant.EPOCH), // java.sql.Date with positive epoch
        Arguments.of("java.sql.Date 2022‑12‑31T23:59:59Z", new java.sql.Date(1_672_531_199_000L), null, Instant.parse("2022-12-31T23:59:59Z")), // java.sql.Timestamp with nanosecond precision
        Arguments.of("java.sql.Timestamp with nanoseconds", Timestamp.valueOf("2022-12-31 23:59:59.123456789"), null, Instant.parse("2022-12-31T23:59:59.123456789Z")), // java.util.Date with a non‑null Converter (normal case)
        Arguments.of("java.util.Date with non‑null Converter", // 2022‑01‑01T00:00:00Z
        new Date(1_640_995_200_000L), new Converter(new ConverterOptions()), Instant.parse("2022-01-01T00:00:00Z")));
    }

    private static Stream<Arguments> provideExceptionConversions() {
        return Stream.of(// Null source object – NPE from casting to Date
        Arguments.of("null source object", null, null, NullPointerException.class), // Wrong type – ClassCastException from cast to Date
        Arguments.of("non‑date source (String)", "not a date", null, ClassCastException.class), // java.util.Date with epoch beyond Instant.MAX
        Arguments.of("java.util.Date with Long.MAX_VALUE epoch (out‑of‑range)", new Date(Long.MAX_VALUE), null, DateTimeException.class), // java.sql.Date with epoch beyond Instant.MAX (same behaviour)
        Arguments.of("java.sql.Date with Long.MAX_VALUE epoch (out‑of‑range)", new java.sql.Date(Long.MAX_VALUE), null, DateTimeException.class));
    }

    @Test
    void toInstant_withUtilDate_returnsSameInstant() {
        // GIVEN
        // 2022‑01‑01T00:00:00Z
        long epochMilli = 1_640_995_200_000L;
        Date utilDate = new Date(epochMilli);
        // WHEN
        Instant result = DateConversions.toInstant(utilDate, converter);
        // THEN
        assertNotNull(result, "Resulting Instant should not be null");
        assertEquals(Instant.ofEpochMilli(epochMilli), result, "Instant derived from java.util.Date must match its epoch milliseconds");
    }

    @Test
    void toInstant_withSqlDate_returnsInstantAtStartOfDay() {
        // GIVEN
        // java.sql.Date represents a date without time; its internal value is midnight UTC of that day.
        // 2022‑01‑01T00:00:00Z
        long epochMilli = 1_640_995_200_000L;
        java.sql.Date sqlDate = new java.sql.Date(epochMilli);
        // WHEN
        Instant result = DateConversions.toInstant(sqlDate, converter);
        // THEN
        assertNotNull(result, "Resulting Instant should not be null");
        // The implementation creates a new java.util.Date from the same epoch millis,
        // therefore the Instant should be identical to the original epoch value.
        assertEquals(Instant.ofEpochMilli(epochMilli), result, "Instant derived from java.sql.Date must equal the epoch millis of the underlying date");
    }

    @Test
    void toInstant_withTimestampBehavesLikeUtilDate() {
        // GIVEN
        // 2022‑01‑01T00:00:00Z
        long epochMilli = 1_640_995_200_000L;
        Timestamp timestamp = new Timestamp(epochMilli);
        // java.sql.Timestamp extends java.util.Date, so it follows the non‑sqlDate branch.
        // WHEN
        Instant result = DateConversions.toInstant(timestamp, converter);
        // THEN
        assertNotNull(result);
        assertEquals(Instant.ofEpochMilli(epochMilli), result, "Timestamp (subclass of java.util.Date) should be converted using date.toInstant()");
    }

    @Test
    void toInstant_nullFrom_throwsNullPointerException() {
        // GIVEN
        Object from = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> DateConversions.toInstant(from, converter), "Passing null as the source should result in NPE because of the cast to Date");
    }

    /* --------------------------------------------------------------
     *  Positive conversion test cases (return an Instant)
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideSuccessfulConversions")
    void toInstant_successful(String description, Object from, Converter conv, Instant expected) {
        // WHEN
        Instant result = DateConversions.toInstant(from, conv);
        // THEN
        assertNotNull(result, "Resulting Instant must not be null");
        assertEquals(expected, result, () -> "Conversion failed for case: " + description);
    }

    /* --------------------------------------------------------------
     *  Exception test cases (expecting an exception)
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideExceptionConversions")
    void toInstant_exceptions(String description, Object from, Converter conv, Class<? extends Throwable> expectedException) {
        // WHEN / THEN
        assertThrows(expectedException, () -> DateConversions.toInstant(from, conv), () -> "Expected " + expectedException.getSimpleName() + " for case: " + description);
    }
}
