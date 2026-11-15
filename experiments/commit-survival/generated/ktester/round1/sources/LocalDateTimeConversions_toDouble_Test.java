package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalDateTimeConversions_toDouble_Test {

    /**
     * Shared {@link Converter} used by all test cases.
     */
    private static Converter converter;

    /**
     * Shared {@link Converter} used by most test cases.
     */
    private static Converter sharedConverter;

    /**
     * Functional interface used in parameterised tests to execute the assertion logic.
     */
    @FunctionalInterface
    private interface ExpectedResult {

        void assertResult(Object from, Converter conv);
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; the default constructor provides a
        // sensible set of options for the library's built‑in conversions.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – not strictly necessary but keeps the pattern tidy.
        converter = null;
    }

    /**
     * Creates a fresh dummy {@link Converter}.
     */
    private static Converter dummyConverter() {
        return new Converter(new ConverterOptions());
    }

    /**
     * Computes the double representation of an {@link Instant}.
     */
    private static double instantToDouble(Instant i) {
        return i.getEpochSecond() + i.getNano() / 1_000_000_000d;
    }

    /**
     * Provides arguments for the “branch coverage” group.
     */
    private static Stream<Arguments> branchCoverageProvider() {
        return Stream.of(// 1️⃣ null‑null → NPE
        Arguments.of(null, null, NullPointerException.class, "null‑null NPE"), // 2️⃣ Instant → forward unchanged
        Arguments.of(Instant.now(), dummyConverter(), (ExpectedResult) (from, conv) -> {
            Instant i = (Instant) from;
            double expected = instantToDouble(i);
            double actual = LocalDateTimeConversions.toDouble(i, conv);
            assertEquals(expected, actual, 1e-12);
        }, "Instant forward"), // 3️⃣ LocalDateTime → epoch conversion (system default zone)
        Arguments.of(LocalDateTime.of(2023, 5, 15, 12, 30, 45, 123_456_789), dummyConverter(), (ExpectedResult) (from, conv) -> {
            LocalDateTime ldt = (LocalDateTime) from;
            Instant i = ldt.atZone(ZoneId.systemDefault()).toInstant();
            double expected = instantToDouble(i);
            double actual = LocalDateTimeConversions.toDouble(ldt, conv);
            assertEquals(expected, actual, 1e-12);
        }, "LocalDateTime conversion"), // 4️⃣ Date epoch start → 0.0
        Arguments.of(new Date(0L), dummyConverter(), (ExpectedResult) (from, conv) -> {
            double actual = LocalDateTimeConversions.toDouble(from, conv);
            assertEquals(0.0d, actual, "Date epoch start should be 0.0");
        }, "Date epoch start"), // 5️⃣ Timestamp → seconds part only (no nanos in test data)
        Arguments.of(Timestamp.from(Instant.ofEpochMilli(1_600_000_000_000L)), dummyConverter(), (ExpectedResult) (from, conv) -> {
            Timestamp ts = (Timestamp) from;
            Instant i = ts.toInstant();
            double expected = instantToDouble(i);
            double actual = LocalDateTimeConversions.toDouble(ts, conv);
            assertEquals(expected, actual, 1e-12);
        }, "Timestamp conversion"), // 6️⃣ ISO‑8601 string → parsed as LocalDateTime
        Arguments.of("2023-05-15T12:30:45.123456789", dummyConverter(), (ExpectedResult) (from, conv) -> {
            String s = (String) from;
            LocalDateTime ldt = LocalDateTime.parse(s);
            Instant i = ldt.atZone(ZoneId.systemDefault()).toInstant();
            double expected = instantToDouble(i);
            double actual = LocalDateTimeConversions.toDouble(s, conv);
            assertEquals(expected, actual, 1e-12);
        }, "String ISO_LOCAL_DATE_TIME"), // 7️⃣ Long epoch seconds → double of same value
        Arguments.of(12_345L, dummyConverter(), (ExpectedResult) (from, conv) -> {
            long secs = (Long) from;
            double expected = secs;
            double actual = LocalDateTimeConversions.toDouble(secs, conv);
            assertEquals(expected, actual, 1e-12);
        }, "Long epoch seconds"), // 8️⃣ BigDecimal → double value
        Arguments.of(new BigDecimal("12345.678"), dummyConverter(), (ExpectedResult) (from, conv) -> {
            BigDecimal bd = (BigDecimal) from;
            double expected = bd.doubleValue();
            double actual = LocalDateTimeConversions.toDouble(bd, conv);
            assertEquals(expected, actual, 1e-12);
        }, "BigDecimal conversion"), // 9️⃣ AtomicLong → double value
        Arguments.of(new AtomicLong(9_876_543_210L), dummyConverter(), (ExpectedResult) (from, conv) -> {
            AtomicLong al = (AtomicLong) from;
            double expected = al.get();
            double actual = LocalDateTimeConversions.toDouble(al, conv);
            assertEquals(expected, actual, 1e-12);
        }, "AtomicLong conversion"), // 🔟 Unsupported object → ClassCastException (or IllegalArgumentException)
        Arguments.of(new Object(), dummyConverter(), ClassCastException.class, "Unsupported object type"));
    }

    /**
     * Provides arguments for the “additional type & edge cases” group.
     */
    private static Stream<Arguments> additionalTypesProvider() {
        return Stream.of(// LocalDate → start of day in system default zone
        Arguments.of(LocalDate.of(2023, 5, 15), dummyConverter(), (ExpectedResult) (from, conv) -> {
            LocalDate ld = (LocalDate) from;
            Instant i = ld.atStartOfDay(ZoneId.systemDefault()).toInstant();
            double expected = instantToDouble(i);
            double actual = LocalDateTimeConversions.toDouble(ld, conv);
            assertEquals(expected, actual, 1e-12);
        }, "LocalDate conversion"), // LocalTime → epoch date + time in system default zone
        Arguments.of(LocalTime.of(12, 30, 45, 123_456_789), dummyConverter(), (ExpectedResult) (from, conv) -> {
            LocalTime lt = (LocalTime) from;
            LocalDate epoch = LocalDate.ofEpochDay(0);
            Instant i = lt.atDate(epoch).atZone(ZoneId.systemDefault()).toInstant();
            double expected = instantToDouble(i);
            double actual = LocalDateTimeConversions.toDouble(lt, conv);
            assertEquals(expected, actual, 1e-12);
        }, "LocalTime conversion"), // ZonedDateTime with negative offset
        Arguments.of(ZonedDateTime.of(2023, 5, 15, 12, 30, 45, 123_456_789, ZoneOffset.ofHours(-5)), dummyConverter(), (ExpectedResult) (from, conv) -> {
            ZonedDateTime zdt = (ZonedDateTime) from;
            Instant i = zdt.toInstant();
            double expected = instantToDouble(i);
            double actual = LocalDateTimeConversions.toDouble(zdt, conv);
            assertEquals(expected, actual, 1e-12);
        }, "ZonedDateTime negative offset"), // OffsetDateTime +02:00
        Arguments.of(OffsetDateTime.of(2023, 5, 15, 12, 30, 45, 0, ZoneOffset.ofHours(2)), dummyConverter(), (ExpectedResult) (from, conv) -> {
            OffsetDateTime odt = (OffsetDateTime) from;
            Instant i = odt.toInstant();
            double expected = instantToDouble(i);
            double actual = LocalDateTimeConversions.toDouble(odt, conv);
            assertEquals(expected, actual, 1e-12);
        }, "OffsetDateTime +02:00"), // Calendar (UTC) with specific millis
        Arguments.of(createUtcCalendar(1_600_000_000_123L), dummyConverter(), (ExpectedResult) (from, conv) -> {
            Calendar cal = (Calendar) from;
            Instant i = cal.toInstant();
            double expected = instantToDouble(i);
            double actual = LocalDateTimeConversions.toDouble(cal, conv);
            assertEquals(expected, actual, 1e-12);
        }, "Calendar UTC"), // java.sql.Date epoch start → 0.0
        Arguments.of(new java.sql.Date(0L), dummyConverter(), (ExpectedResult) (from, conv) -> {
            double actual = LocalDateTimeConversions.toDouble(from, conv);
            assertEquals(0.0d, actual, "java.sql.Date epoch start should be 0.0");
        }, "java.sql.Date epoch start"), // BigInteger → epoch seconds (possible precision loss)
        Arguments.of(new BigInteger("1234567890123456789"), dummyConverter(), (ExpectedResult) (from, conv) -> {
            BigInteger bi = (BigInteger) from;
            Instant i = Instant.ofEpochSecond(bi.longValue());
            double expected = instantToDouble(i);
            double actual = LocalDateTimeConversions.toDouble(bi, conv);
            assertEquals(expected, actual, 1e-6);
        }, "BigInteger epoch seconds"), // Integer → double value
        Arguments.of(Integer.valueOf(12_345), dummyConverter(), (ExpectedResult) (from, conv) -> {
            int val = (Integer) from;
            double expected = val;
            double actual = LocalDateTimeConversions.toDouble(val, conv);
            assertEquals(expected, actual, 1e-12);
        }, "Integer conversion"), // Double → unchanged
        Arguments.of(Double.valueOf(12_345.678), dummyConverter(), (ExpectedResult) (from, conv) -> {
            double expected = (Double) from;
            double actual = LocalDateTimeConversions.toDouble(from, conv);
            assertEquals(expected, actual, 1e-12);
        }, "Double conversion"), // Float → widened to double
        Arguments.of(Float.valueOf(12_345.678f), dummyConverter(), (ExpectedResult) (from, conv) -> {
            float f = (Float) from;
            double expected = f;
            double actual = LocalDateTimeConversions.toDouble(f, conv);
            assertEquals(expected, actual, 1e-6);
        }, "Float conversion"), // Empty string → parsing exception
        Arguments.of("", dummyConverter(), DateTimeParseException.class, "Empty string parsing failure"), // Invalid string → parsing exception
        Arguments.of("not-a-date", dummyConverter(), DateTimeParseException.class, "Invalid string parsing failure"), // Unsupported object with null converter → ClassCastException (or IllegalArgumentException)
        Arguments.of(new Object(), null, ClassCastException.class, "Unsupported object with null converter"));
    }

    /**
     * Provides arguments for the “exception scenarios” group.
     */
    private static Stream<Arguments> exceptionScenariosProvider() {
        return Stream.of(// Timestamp with extreme epoch millis → overflow
        Arguments.of(Timestamp.from(Instant.ofEpochMilli(Long.MAX_VALUE)), dummyConverter(), ArithmeticException.class, "Timestamp overflow"), // Date with extreme negative millis → overflow
        Arguments.of(new Date(Long.MIN_VALUE), dummyConverter(), ArithmeticException.class, "Date underflow"), // BigDecimal beyond Double.MAX_VALUE → overflow
        Arguments.of(new BigDecimal("1e400"), dummyConverter(), ArithmeticException.class, "BigDecimal overflow"), // Very large BigInteger → overflow
        Arguments.of(new BigInteger("1" + "0".repeat(500)), dummyConverter(), ArithmeticException.class, "BigInteger overflow"), // Extreme LocalDateTime with null converter → NPE (converter used for zone handling)
        Arguments.of(LocalDateTime.of(9999, 12, 31, 23, 59, 59, 999_999_999), null, NullPointerException.class, "Null converter with extreme LocalDateTime"), // OffsetDateTime with offset +18:00 (out of range) → DateTimeException
        Arguments.of(OffsetDateTime.of(9999, 12, 31, 23, 59, 59, 0, ZoneOffset.ofHoursMinutes(18, 0)), dummyConverter(), DateTimeException.class, "OffsetDateTime offset out of range"), // Invalid ZonedDateTime (Feb 30) – construction itself throws, so we wrap it
        Arguments.of(() -> ZonedDateTime.of(LocalDateTime.of(2023, 2, 30, 12, 0), ZoneId.of("UTC")), dummyConverter(), DateTimeException.class, "ZonedDateTime invalid date"), // LocalTime with hour 25 → DateTimeException
        Arguments.of(() -> LocalTime.of(25, 0, 0), dummyConverter(), DateTimeException.class, "LocalTime invalid hour"), // Calendar with null TimeZone → NPE
        Arguments.of(() -> {
            Calendar cal = Calendar.getInstance();
            cal.setTimeZone(null);
            return cal;
        }, dummyConverter(), NullPointerException.class, "Calendar null timezone"), // AtomicLong with Long.MIN_VALUE → overflow/underflow
        Arguments.of(new AtomicLong(Long.MIN_VALUE), dummyConverter(), ArithmeticException.class, "AtomicLong underflow"));
    }

    /**
     * Helper to create a UTC Calendar set to the supplied epoch‑millis.
     */
    private static Calendar createUtcCalendar(long epochMillis) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(epochMillis);
        return cal;
    }

    /**
     * Verify conversion of a {@link LocalDateTime} to a {@code double}.
     * The expected value is the epoch seconds plus the nanosecond fraction.
     */
    @Test
    void toDouble_fromLocalDateTime() {
        LocalDateTime ldt = LocalDateTime.of(2023, Month.JANUARY, 2, 03, 04, 05, 123_456_789);
        double actual = LocalDateTimeConversions.toDouble(ldt, converter);
        Instant instant = ldt.toInstant(ZoneOffset.UTC);
        double expected = instant.getEpochSecond() + instant.getNano() / 1_000_000_000d;
        assertEquals(expected, actual, 1e-12, "Conversion from LocalDateTime should match the Instant representation");
    }

    /**
     * Verify conversion of a {@link java.util.Date} to a {@code double}.
     */
    @Test
    void toDouble_fromDate() {
        // Use a fixed point in time for reproducibility.
        // corresponds to 2022‑01‑01T00:00:00.123Z
        long millis = 1_640_995_200_123L;
        Date date = new Date(millis);
        double actual = LocalDateTimeConversions.toDouble(date, converter);
        Instant instant = date.toInstant();
        double expected = instant.getEpochSecond() + instant.getNano() / 1_000_000_000d;
        assertEquals(expected, actual, 1e-12, "Conversion from java.util.Date should match the Instant representation");
    }

    /**
     * Verify conversion of an {@link Instant} to a {@code double}.
     */
    @Test
    void toDouble_fromInstant() {
        Instant instant = Instant.parse("2024-03-15T12:30:45.987654321Z");
        double actual = LocalDateTimeConversions.toDouble(instant, converter);
        double expected = instant.getEpochSecond() + instant.getNano() / 1_000_000_000d;
        assertEquals(expected, actual, 1e-12, "Conversion from Instant should produce the correct epoch‑seconds double");
    }

    /**
     * Verify conversion of a {@link Long} (epoch milliseconds) to a {@code double}.
     */
    @Test
    void toDouble_fromLongEpochMillis() {
        // 2023‑11‑14T02:13:20Z
        long epochMillis = 1_700_000_000_000L;
        double actual = LocalDateTimeConversions.toDouble(epochMillis, converter);
        Instant instant = Instant.ofEpochMilli(epochMillis);
        double expected = instant.getEpochSecond() + instant.getNano() / 1_000_000_000d;
        assertEquals(expected, actual, 1e-12, "Conversion from epoch‑millis Long should match the Instant representation");
    }

    /**
     * Verify that a {@code null} source yields {@code 0.0}.
     * The library treats a null conversion to a primitive numeric type as zero.
     */
    @Test
    void toDouble_fromNull() {
        double actual = LocalDateTimeConversions.toDouble(null, converter);
        assertEquals(0.0d, actual, "Conversion from null should return 0.0 for primitive double target");
    }

    @ParameterizedTest(name = "[{index}] Branch coverage – {3}")
    @MethodSource("branchCoverageProvider")
    void testBranchCoverage(Object from, Converter conv, Object expected, String description) {
        if (expected instanceof Class) {
            // Expected an exception type
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected;
            assertThrows(exClass, () -> LocalDateTimeConversions.toDouble(from, conv), description);
        } else if (expected instanceof ExpectedResult) {
            ((ExpectedResult) expected).assertResult(from, conv);
        } else {
            fail("Unsupported expected type in branchCoverageProvider");
        }
    }

    @ParameterizedTest(name = "[{index}] Additional types – {3}")
    @MethodSource("additionalTypesProvider")
    void testAdditionalTypes(Object from, Converter conv, Object expected, String description) {
        if (expected instanceof Class) {
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected;
            assertThrows(exClass, () -> LocalDateTimeConversions.toDouble(from, conv), description);
        } else if (expected instanceof ExpectedResult) {
            ((ExpectedResult) expected).assertResult(from, conv);
        } else {
            fail("Unsupported expected type in additionalTypesProvider");
        }
    }

    @ParameterizedTest(name = "[{index}] Exception scenarios – {3}")
    @MethodSource("exceptionScenariosProvider")
    void testExceptionScenarios(Object fromSupplierOrValue, Converter conv, Class<? extends Throwable> expectedException, String description) {
        Object actualFrom;
        // Some test cases supply a lambda that creates an invalid object (e.g., invalid ZonedDateTime)
        if (fromSupplierOrValue instanceof java.util.function.Supplier) {
            actualFrom = ((java.util.function.Supplier<?>) fromSupplierOrValue).get();
        } else {
            actualFrom = fromSupplierOrValue;
        }
        assertThrows(expectedException, () -> LocalDateTimeConversions.toDouble(actualFrom, conv), description);
    }

    @Test
    void toDouble_fromLocalDateTime_manual() {
        LocalDateTime ldt = LocalDateTime.of(2023, Month.JANUARY, 2, 3, 4, 5, 123_456_789);
        double actual = LocalDateTimeConversions.toDouble(ldt, sharedConverter);
        Instant instant = ldt.atZone(ZoneOffset.UTC).toInstant();
        double expected = instantToDouble(instant);
        assertEquals(expected, actual, 1e-12);
    }

    @Test
    void toDouble_fromDate_manual() {
        // 2022‑01‑01T00:00:00.123Z
        long millis = 1_640_995_200_123L;
        Date date = new Date(millis);
        double actual = LocalDateTimeConversions.toDouble(date, sharedConverter);
        double expected = instantToDouble(date.toInstant());
        assertEquals(expected, actual, 1e-12);
    }

    @Test
    void toDouble_fromInstant_manual() {
        Instant instant = Instant.parse("2024-03-15T12:30:45.987654321Z");
        double actual = LocalDateTimeConversions.toDouble(instant, sharedConverter);
        double expected = instantToDouble(instant);
        assertEquals(expected, actual, 1e-12);
    }

    @Test
    void toDouble_fromLongEpochMillis_manual() {
        // 2023‑11‑14T02:13:20Z
        long epochMillis = 1_700_000_000_000L;
        double actual = LocalDateTimeConversions.toDouble(epochMillis, sharedConverter);
        double expected = instantToDouble(Instant.ofEpochMilli(epochMillis));
        assertEquals(expected, actual, 1e-12);
    }

    @Test
    void toDouble_fromNull_manual() {
        double actual = LocalDateTimeConversions.toDouble(null, sharedConverter);
        assertEquals(0.0d, actual);
    }
}
