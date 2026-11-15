package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import java.time.*;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DoubleConversions_toTimestamp_Test {

    /**
     * The Converter instance used for all tests.
     */
    private static Converter converter;

    /**
     * A real Converter instance used for the majority of cases where no custom behaviour is required.
     */
    private static Converter defaultConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance – the default constructor creates an empty options set.
        converter = new Converter(new ConverterOptions());
        defaultConverter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – helps the JVM GC in larger test suites.
        converter = null;
    }

    /**
     * Valid conversions from supported types (including null source).
     */
    static Stream<Arguments> validConversions() {
        // Helper to build a Calendar in UTC with the required epoch‑millis.
        Calendar utcCal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        // 2023‑01‑01T00:00:00Z
        utcCal.setTimeInMillis(1672531200000L);
        return Stream.of(// Instant → Timestamp
        Arguments.of(Instant.ofEpochMilli(1672531200000L), null, Timestamp.valueOf("2023-01-01 00:00:00.0"), null), // java.util.Date → Timestamp
        Arguments.of(new Date(1672531200000L), null, Timestamp.valueOf("2023-01-01 00:00:00.0"), null), // LocalDateTime (system default zone) → Timestamp
        Arguments.of(LocalDateTime.of(2023, 1, 1, 0, 0, 0), null, Timestamp.valueOf("2023-01-01 00:00:00.0"), null), // ZonedDateTime (UTC) → Timestamp
        Arguments.of(ZonedDateTime.of(2023, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), null, Timestamp.valueOf("2023-01-01 00:00:00.0"), null), // OffsetDateTime (+02:00) → Timestamp (adjusted to UTC)
        Arguments.of(OffsetDateTime.of(2023, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(2)), null, Timestamp.valueOf("2022-12-31 22:00:00.0"), null), // Calendar (UTC) → Timestamp
        Arguments.of(utcCal, null, Timestamp.valueOf("2023-01-01 00:00:00.0"), null), // Long epoch‑millis → Timestamp
        Arguments.of(Long.valueOf(1672531200000L), null, Timestamp.valueOf("2023-01-01 00:00:00.0"), null), // String epoch‑millis → Timestamp
        Arguments.of("1672531200000", null, Timestamp.valueOf("2023-01-01 00:00:00.0"), null), // null source with null converter → null result
        Arguments.of(null, null, null, null), // Unsupported source type (Object) → IllegalArgumentException
        Arguments.of(new Object(), null, null, IllegalArgumentException.class), // null source with a custom converter that returns an Instant
        Arguments.of(null, mockConverterReturningInstant(), Timestamp.valueOf("2023-01-01 00:00:00.0"), null));
    }

    /**
     * Additional supported type conversions (including time‑zone handling).
     */
    static Stream<Arguments> additionalSupportedConversions() {
        return Stream.of(// Already a java.sql.Timestamp – should be returned unchanged (value‑wise)
        Arguments.of(Timestamp.valueOf("2023-06-15 12:34:56.789"), null, Timestamp.valueOf("2023-06-15 12:34:56.789"), null), // ZonedDateTime in America/New_York (UTC‑4 in June) → UTC Timestamp
        Arguments.of(ZonedDateTime.of(2023, 6, 15, 12, 34, 56, 0, ZoneId.of("America/New_York")), null, Timestamp.valueOf("2023-06-15 16:34:56.0"), null), // OffsetDateTime with -05:00 offset → UTC Timestamp
        Arguments.of(OffsetDateTime.of(2023, 6, 15, 12, 34, 56, 0, ZoneOffset.ofHours(-5)), null, Timestamp.valueOf("2023-06-15 17:34:56.0"), null), // LocalDate (no time) → IllegalArgumentException
        Arguments.of(LocalDate.of(2023, 6, 15), null, null, IllegalArgumentException.class), // LocalTime (no date) → IllegalArgumentException
        Arguments.of(LocalTime.of(12, 34, 56), null, null, IllegalArgumentException.class), // Integer epoch‑millis (unsupported) → IllegalArgumentException
        Arguments.of(Integer.valueOf(1672531200), null, null, IllegalArgumentException.class), // Double epoch‑millis (unsupported) → IllegalArgumentException
        Arguments.of(Double.valueOf(1.6725312e12), null, null, IllegalArgumentException.class), // ISO‑8601 text (unsupported) → IllegalArgumentException
        Arguments.of("2023-01-01T00:00:00Z", null, null, IllegalArgumentException.class), // null source with custom converter that returns null → null result
        Arguments.of(null, mockConverterReturningNull(), null, null), // null source with custom converter that throws IllegalArgumentException
        Arguments.of(null, mockConverterThrowing(IllegalArgumentException.class), null, IllegalArgumentException.class));
    }

    /**
     * Boundary epoch values (zero and negative).
     */
    static Stream<Arguments> boundaryEpochValues() {
        return Stream.of(Arguments.of(Long.valueOf(0L), null, Timestamp.valueOf("1970-01-01 00:00:00.0"), null), Arguments.of(Long.valueOf(-86_400_000L), null, Timestamp.valueOf("1969-12-31 00:00:00.0"), null));
    }

    /**
     * Unsupported temporal types that must raise IllegalArgumentException.
     */
    static Stream<Arguments> unsupportedTemporalTypes() {
        return Stream.of(// OffsetTime (no date)
        Arguments.of(OffsetTime.of(12, 34, 56, 0, ZoneOffset.ofHours(2)), null, null, IllegalArgumentException.class), // Duration (no instant)
        Arguments.of(Duration.ofHours(5), null, null, IllegalArgumentException.class));
    }

    /**
     * Invalid string inputs that cannot be parsed as epoch‑millis.
     */
    static Stream<Arguments> invalidStringInputs() {
        return Stream.of(// Non‑numeric string
        Arguments.of("notANumber", null, null, IllegalArgumentException.class), // Number larger than Long.MAX_VALUE
        Arguments.of("9223372036854775808", null, null, IllegalArgumentException.class));
    }

    /**
     * Custom converter error‑propagation scenarios.
     */
    static Stream<Arguments> customConverterErrorPropagation() {
        return Stream.of(// Valid Instant source but converter throws IllegalArgumentException
        Arguments.of(Instant.ofEpochMilli(1672531200000L), mockConverterThrowing(IllegalArgumentException.class), null, IllegalArgumentException.class), // Valid Instant source but converter throws NullPointerException
        Arguments.of(Instant.ofEpochMilli(1672531200000L), mockConverterThrowing(NullPointerException.class), null, NullPointerException.class), // Valid Instant source but converter returns unsupported type (String)
        Arguments.of(Instant.ofEpochMilli(1672531200000L), mockConverterReturningString(), null, IllegalArgumentException.class));
    }

    private static Converter mockConverterReturningInstant() {
        Converter mock = mock(Converter.class);
        when(mock.convert(any(), eq(Instant.class))).thenReturn(Instant.ofEpochMilli(1672531200000L));
        return mock;
    }

    private static Converter mockConverterReturningNull() {
        Converter mock = mock(Converter.class);
        when(mock.convert(any(), eq(Instant.class))).thenReturn(null);
        return mock;
    }

    private static Converter mockConverterThrowing(Class<? extends RuntimeException> exClass) {
        Converter mock = mock(Converter.class);
        RuntimeException ex;
        try {
            ex = exClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Unable to instantiate exception " + exClass, e);
        }
        when(mock.convert(any(), eq(Instant.class))).thenThrow(ex);
        return mock;
    }

    private static Converter mockConverterReturningString() {
        Converter mock = mock(Converter.class);
        when(mock.convert(any(), eq(Instant.class))).thenReturn("not an instant");
        return mock;
    }

    /**
     * Centralised execution logic shared by all parameterised tests.
     *
     * @param from               the source object supplied to {@code toTimestamp}
     * @param conv               the {@link Converter} instance (may be {@code null})
     * @param expectedTimestamp  the expected {@link Timestamp} result (or {@code null})
     * @param expectedException  the expected exception type (or {@code null} if no exception)
     */
    private void executeTestCase(Object from, Converter conv, Timestamp expectedTimestamp, Class<? extends Throwable> expectedException) {
        // Use the real default converter when the test case specifies a null converter.
        Converter effectiveConverter = (conv == null) ? defaultConverter : conv;
        if (expectedException != null) {
            // Exception expected – verify that it propagates unchanged.
            assertThrows(expectedException, () -> DoubleConversions.toTimestamp(from, effectiveConverter), () -> "Expected exception " + expectedException.getSimpleName() + " for input: " + from);
        } else {
            // Normal conversion – result may be null or a concrete Timestamp.
            Timestamp result = DoubleConversions.toTimestamp(from, effectiveConverter);
            if (expectedTimestamp == null) {
                assertNull(result, "Result should be null for input: " + from);
            } else {
                assertNotNull(result, "Result should not be null for input: " + from);
                assertEquals(expectedTimestamp.getTime(), result.getTime(), "Timestamp epoch milliseconds differ for input: " + from);
                // Preserve nanosecond precision when the source is a java.sql.Timestamp.
                if (from instanceof Timestamp) {
                    assertEquals(((Timestamp) from).getNanos(), result.getNanos(), "Nanosecond component should be preserved for Timestamp source");
                }
            }
        }
    }

    @Test
    void toTimestamp_fromInstant_returnsEquivalentTimestamp() {
        // GIVEN
        Instant now = Instant.now();
        // WHEN
        Timestamp ts = DoubleConversions.toTimestamp(now, converter);
        // THEN
        assertNotNull(ts, "Timestamp should not be null");
        assertEquals(now.toEpochMilli(), ts.getTime(), "Timestamp milliseconds must match the source Instant");
    }

    @Test
    void toTimestamp_fromDate_returnsEquivalentTimestamp() {
        // GIVEN
        // java.util.Date
        Date date = new Date();
        long expectedMillis = date.getTime();
        // WHEN
        Timestamp ts = DoubleConversions.toTimestamp(date, converter);
        // THEN
        assertNotNull(ts);
        assertEquals(expectedMillis, ts.getTime(), "Timestamp milliseconds must match the source Date");
    }

    @Test
    void toTimestamp_fromSqlTimestamp_returnsSameValue() {
        // GIVEN
        Timestamp source = new Timestamp(System.currentTimeMillis());
        // WHEN
        Timestamp result = DoubleConversions.toTimestamp(source, converter);
        // THEN
        assertNotNull(result);
        assertEquals(source.getTime(), result.getTime(), "Resulting Timestamp should have the same epoch millis as the source Timestamp");
        // The nanosecond part is also preserved by Timestamp.from(Instant)
        assertEquals(source.getNanos(), result.getNanos(), "Nanosecond component should be preserved");
    }

    @Test
    void toTimestamp_fromLocalDateTime_usesSystemDefaultZone() {
        // GIVEN
        LocalDateTime ldt = LocalDateTime.now();
        ZoneId zone = ZoneId.systemDefault();
        Instant expected = ldt.atZone(zone).toInstant();
        // WHEN
        Timestamp ts = DoubleConversions.toTimestamp(ldt, converter);
        // THEN
        assertNotNull(ts);
        assertEquals(expected.toEpochMilli(), ts.getTime(), "Timestamp should represent the same instant as the LocalDateTime in the system default zone");
    }

    @Test
    void toTimestamp_fromZonedDateTime_preservesInstant() {
        // GIVEN
        ZonedDateTime zdt = ZonedDateTime.now(ZoneOffset.UTC);
        Instant expected = zdt.toInstant();
        // WHEN
        Timestamp ts = DoubleConversions.toTimestamp(zdt, converter);
        // THEN
        assertNotNull(ts);
        assertEquals(expected.toEpochMilli(), ts.getTime(), "Timestamp should match the instant represented by the ZonedDateTime");
    }

    @Test
    void toTimestamp_fromNull_throwsNullPointerException() {
        // GIVEN
        Object nullObj = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> DoubleConversions.toTimestamp(nullObj, converter), "Converting a null source should result in NPE because Timestamp.from(null) is invoked");
    }

    @ParameterizedTest(name = "[{index}] {0} → {2}")
    @MethodSource("validConversions")
    void testValidConversions(Object from, Converter conv, Timestamp expected, Class<? extends Throwable> expectedException) {
        executeTestCase(from, conv, expected, expectedException);
    }

    @ParameterizedTest(name = "[{index}] {0} → {2}")
    @MethodSource("additionalSupportedConversions")
    void testAdditionalSupportedConversions(Object from, Converter conv, Timestamp expected, Class<? extends Throwable> expectedException) {
        executeTestCase(from, conv, expected, expectedException);
    }

    @ParameterizedTest(name = "[{index}] {0} → {2}")
    @MethodSource("boundaryEpochValues")
    void testBoundaryEpochValues(Object from, Converter conv, Timestamp expected, Class<? extends Throwable> expectedException) {
        executeTestCase(from, conv, expected, expectedException);
    }

    @ParameterizedTest(name = "[{index}] {0} → {2}")
    @MethodSource("unsupportedTemporalTypes")
    void testUnsupportedTemporalTypes(Object from, Converter conv, Timestamp expected, Class<? extends Throwable> expectedException) {
        executeTestCase(from, conv, expected, expectedException);
    }

    @ParameterizedTest(name = "[{index}] {0} → {2}")
    @MethodSource("invalidStringInputs")
    void testInvalidStringInputs(Object from, Converter conv, Timestamp expected, Class<? extends Throwable> expectedException) {
        executeTestCase(from, conv, expected, expectedException);
    }

    @ParameterizedTest(name = "[{index}] {0} → {2}")
    @MethodSource("customConverterErrorPropagation")
    void testCustomConverterErrorPropagation(Object from, Converter conv, Timestamp expected, Class<? extends Throwable> expectedException) {
        executeTestCase(from, conv, expected, expectedException);
    }
}
