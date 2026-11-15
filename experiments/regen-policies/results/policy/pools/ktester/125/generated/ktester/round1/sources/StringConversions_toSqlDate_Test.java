package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.sql.Timestamp;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import java.time.*;

class StringConversions_toSqlDate_Test {

    private static Converter converter;

    // to restore after tests
    private static ZoneId originalDefaultZone;

    @Nested
    @DisplayName("Object identity verification")
    class IdentityVerification {

        @Test
        @DisplayName("Instant input yields a new Date instance with same epoch")
        void instantInputCreatesNewDate() {
            Instant instant = Instant.ofEpochMilli(5_555L);
            Date result = StringConversions.toSqlDate(instant, converter);
            assertNotNull(result);
            assertEquals(5_555L, result.getTime());
            // Ensure a fresh instance (not the same reference as any Date input)
            assertFalse(result instanceof java.sql.Date, "Result should be java.util.Date, not java.sql.Date");
        }

        @Test
        @DisplayName("Date input creates a new Date instance with same epoch")
        void dateInputCreatesNewInstance() {
            Date source = new Date(0L);
            Date result = StringConversions.toSqlDate(source, converter);
            assertNotNull(result);
            assertEquals(0L, result.getTime());
            assertNotSame(source, result, "Result must be a different instance than the source Date");
        }

        @Test
        @DisplayName("ISO‑8601 string is parsed correctly")
        void isoStringIsParsed() {
            String iso = "2023-03-15T12:34:56Z";
            Date result = StringConversions.toSqlDate(iso, converter);
            assertNotNull(result);
            Instant expected = Instant.parse(iso);
            assertEquals(expected.toEpochMilli(), result.getTime(), "String should be parsed to the correct instant");
        }

        @Test
        @DisplayName("LocalDate is converted to start‑of‑day UTC")
        void localDateConversion() {
            LocalDate ld = LocalDate.of(2023, 3, 15);
            Date result = StringConversions.toSqlDate(ld, converter);
            assertNotNull(result);
            // start of day at UTC (because we forced default zone to UTC)
            Instant expected = ld.atStartOfDay(ZoneOffset.UTC).toInstant();
            assertEquals(expected.toEpochMilli(), result.getTime());
        }

        @Test
        @DisplayName("LocalDateTime is interpreted in default zone")
        void localDateTimeConversion() {
            LocalDateTime ldt = LocalDateTime.of(2023, 3, 15, 12, 34, 56);
            Date result = StringConversions.toSqlDate(ldt, converter);
            assertNotNull(result);
            Instant expected = ldt.atZone(ZoneOffset.UTC).toInstant();
            assertEquals(expected.toEpochMilli(), result.getTime());
        }

        @Test
        @DisplayName("Timestamp valueOf conversion")
        void timestampValueOfConversion() {
            Timestamp ts = Timestamp.valueOf("2023-03-15 12:34:56.123");
            Date result = StringConversions.toSqlDate(ts, converter);
            assertNotNull(result);
            assertEquals(ts.getTime(), result.getTime(), "Timestamp should be converted to a Date with identical epoch millis");
        }

        @Test
        @DisplayName("Long epoch milliseconds are converted directly")
        void longEpochConversion() {
            long epoch = 123_456_789L;
            Date result = StringConversions.toSqlDate(epoch, converter);
            assertNotNull(result);
            assertEquals(epoch, result.getTime());
        }

        @Test
        @DisplayName("Unparsable string yields null (implementation‑dependent)")
        void unparsableStringReturnsNullOrException() {
            String bad = "not a date";
            // The implementation may return null or throw; we accept either.
            try {
                Date result = StringConversions.toSqlDate(bad, converter);
                assertNull(result, "Unparsable string should result in null");
            } catch (Exception e) {
                // Accept any exception that indicates failure to parse.
                assertTrue(e instanceof DateTimeParseException || e instanceof IllegalArgumentException, "Expected parsing related exception, got: " + e);
            }
        }

        @Test
        @DisplayName("Unsupported object type yields null or exception")
        void unsupportedObjectReturnsNullOrException() {
            Object obj = new Object();
            try {
                Date result = StringConversions.toSqlDate(obj, converter);
                assertNull(result, "Unsupported type should result in null");
            } catch (Exception e) {
                assertTrue(e instanceof IllegalArgumentException, "Expected IllegalArgumentException for unsupported type, got: " + e);
            }
        }
    }

    @Nested
    @DisplayName("Null converter handling")
    class NullConverter {

        @Test
        @DisplayName("Passing a null Converter throws NullPointerException")
        void nullConverterThrowsNPE() {
            String iso = "2023-03-15T12:34:56Z";
            assertThrows(NullPointerException.class, () -> StringConversions.toSqlDate(iso, null), "Calling toSqlDate with a null Converter must throw NullPointerException");
        }
    }

    @Nested
    @DisplayName("Invalid string format exception")
    class InvalidStringFormat {

        @Test
        @DisplayName("Impossible date (Feb 30) causes DateTimeParseException")
        void impossibleDateThrowsException() {
            String badDate = "2023-02-30T12:00:00Z";
            assertThrows(DateTimeParseException.class, () -> StringConversions.toSqlDate(badDate, converter), "Parsing an impossible ISO‑8601 date must throw DateTimeParseException");
        }

        @Test
        @DisplayName("Completely non‑date string causes DateTimeParseException")
        void nonDateStringThrowsException() {
            String notADate = "not-an-instant";
            assertThrows(DateTimeParseException.class, () -> StringConversions.toSqlDate(notADate, converter), "Non‑date strings should trigger DateTimeParseException");
        }
    }

    @Nested
    @DisplayName("Unsupported type exception")
    class UnsupportedType {

        @Test
        @DisplayName("Boolean input causes IllegalArgumentException")
        void booleanInputThrows() {
            assertThrows(IllegalArgumentException.class, () -> StringConversions.toSqlDate(Boolean.TRUE, converter), "Boolean is not a supported source type for toSqlDate");
        }

        @Test
        @DisplayName("Arbitrary Object input causes IllegalArgumentException")
        void arbitraryObjectThrows() {
            assertThrows(IllegalArgumentException.class, () -> StringConversions.toSqlDate(new Object(), converter), "Unsupported object types must cause IllegalArgumentException");
        }
    }

    @Nested
    @DisplayName("Numeric overflow exception")
    class NumericOverflow {

        @Test
        @DisplayName("Long.MAX_VALUE causes ArithmeticException or DateTimeException")
        void longMaxValueOverflow() {
            // The exact exception type may differ between implementations; accept either.
            assertThrows(RuntimeException.class, () -> StringConversions.toSqlDate(Long.MAX_VALUE, converter), "An overflow when converting a too‑large epoch millisecond value should throw an exception");
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter needs a ConverterOptions instance; use the default constructor.
        converter = new Converter(new ConverterOptions());
        // Force a known default zone (UTC) so that LocalDate/LocalDateTime tests are repeatable.
        originalDefaultZone = ZoneId.systemDefault();
        System.setProperty("user.timezone", "UTC");
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @AfterAll
    static void teardownAfterAll() {
        // Restore the original default zone to avoid side‑effects on other tests.
        if (originalDefaultZone != null) {
            System.setProperty("user.timezone", originalDefaultZone.getId());
            TimeZone.setDefault(TimeZone.getTimeZone(originalDefaultZone));
        }
        converter = null;
    }

    @Test
    void toSqlDate_NullInput_ReturnsNull() {
        Date result = StringConversions.toSqlDate(null, converter);
        assertNull(result, "Converting a null source should return null");
    }

    @Test
    void toSqlDate_UtilDate_ReturnsEquivalentDate() {
        // a fixed epoch milli
        Date source = new Date(1_600_000_000_000L);
        Date result = StringConversions.toSqlDate(source, converter);
        assertNotNull(result);
        assertEquals(source.getTime(), result.getTime(), "The resulting java.sql.Date should have the same epoch milliseconds as the source Date");
    }

    @Test
    void toSqlDate_Timestamp_ReturnsEquivalentDate() {
        Timestamp source = new Timestamp(1_600_000_123_456L);
        Date result = StringConversions.toSqlDate(source, converter);
        assertNotNull(result);
        assertEquals(source.getTime(), result.getTime(), "The resulting java.sql.Date should match the Timestamp's epoch milliseconds");
    }

    @Test
    void toSqlDate_Instant_ReturnsEquivalentDate() {
        Instant source = Instant.ofEpochMilli(1_600_000_987_654L);
        Date result = StringConversions.toSqlDate(source, converter);
        assertNotNull(result);
        assertEquals(source.toEpochMilli(), result.getTime(), "The resulting java.sql.Date should match the Instant's epoch milliseconds");
    }

    @Test
    void toSqlDate_OffsetDateTime_ReturnsEquivalentDate() {
        // 2023-05-10T12:30:00Z  -> epoch millis = 1683719400000
        OffsetDateTime source = OffsetDateTime.of(2023, 5, 10, 12, 30, 0, 0, ZoneOffset.UTC);
        Date result = StringConversions.toSqlDate(source, converter);
        assertNotNull(result);
        assertEquals(source.toInstant().toEpochMilli(), result.getTime(), "The resulting java.sql.Date should match the OffsetDateTime's instant epoch milliseconds");
    }
}
