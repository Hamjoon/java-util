package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import static com.cedarsoftware.util.convert.MapConversions.INSTANT;

class InstantConversions_toDate_Test {

    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise a default Converter instance – this is what the original test suite used.
        converter = new Converter(new ConverterOptions());
        // Force a deterministic default time‑zone for the whole test run.
        // The library interprets some types (LocalDate, LocalTime, LocalDateTime) in the
        // system default zone, therefore we set it to UTC to make expected values stable.
        TimeZone original = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        // store original if you ever need to restore – not required for this isolated test.
    }

    /**
     * Supplies all successful conversion scenarios.
     */
    private static Stream<Arguments> successCases() {
        // Helper objects used in multiple cases
        Calendar utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcCal.setTimeInMillis(1622548800000L);
        return Stream.of(// ---- Primitive / boxed numbers -------------------------------------------------
        Arguments.of(1622548800000L, null, 1622548800000L, "Long value representing epoch milliseconds should be converted directly to a Date"), Arguments.of(1622548800, null, 1622548800L, "Integer value (treated as milliseconds) should be converted to a Date"), // ---- java.util.Date -----------------------------------------------------------
        Arguments.of(new Date(1622548800000L), null, 1622548800000L, "java.util.Date instance should be converted to a new Date preserving the original epoch"), // ---- java.sql.Timestamp -------------------------------------------------------
        Arguments.of(Timestamp.valueOf("2021-06-01 12:00:00"), null, 1622548800000L, "java.sql.Timestamp should be converted to a Date with the same epoch milliseconds"), // ---- java.time.Instant ---------------------------------------------------------
        Arguments.of(Instant.ofEpochMilli(1622548800000L), null, 1622548800000L, "java.time.Instant should be converted to a Date representing the same instant"), // ---- LocalDateTime ------------------------------------------------------------
        Arguments.of(LocalDateTime.of(2021, 6, 1, 12, 0, 0), null, LocalDateTime.of(2021, 6, 1, 12, 0, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), "LocalDateTime interpreted in the system default zone should be converted to the corresponding epoch milliseconds"), // ---- LocalDate ---------------------------------------------------------------
        Arguments.of(LocalDate.of(2021, 6, 1), null, LocalDate.of(2021, 6, 1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(), "LocalDate (midnight at system default zone) should be converted to the epoch milliseconds for 2021‑06‑01T00:00:00"), // ---- LocalTime ---------------------------------------------------------------
        Arguments.of(LocalTime.of(12, 0, 0), null, LocalDate.of(1970, 1, 1).atTime(LocalTime.of(12, 0, 0)).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), "LocalTime is interpreted as time‑of‑day on the epoch date (1970‑01‑01) and converted to milliseconds"), // ---- ZonedDateTime (UTC) ------------------------------------------------------
        Arguments.of(ZonedDateTime.of(2021, 6, 1, 12, 0, 0, 0, ZoneId.of("UTC")), null, ZonedDateTime.of(2021, 6, 1, 12, 0, 0, 0, ZoneId.of("UTC")).toInstant().toEpochMilli(), "ZonedDateTime in UTC should be converted to the exact epoch milliseconds"), // ---- OffsetDateTime (UTC) -----------------------------------------------------
        Arguments.of(OffsetDateTime.of(2021, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC), null, OffsetDateTime.of(2021, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC).toInstant().toEpochMilli(), "OffsetDateTime with zero offset should be converted to the same epoch milliseconds as UTC"), // ---- Calendar ---------------------------------------------------------------
        Arguments.of(utcCal, null, 1622548800000L, "Calendar instance set to a specific epoch millisecond should be converted to a Date with that timestamp"), // ---- BigDecimal ---------------------------------------------------------------
        Arguments.of(new BigDecimal("1622548800000"), null, 1622548800000L, "BigDecimal representing milliseconds should be converted to a Date preserving the value"), // ---- BigInteger ---------------------------------------------------------------
        Arguments.of(new BigInteger("1622548800000"), null, 1622548800000L, "BigInteger representing milliseconds should be converted to a Date preserving the value"), // ---- AtomicLong ---------------------------------------------------------------
        Arguments.of(new AtomicLong(1622548800000L), null, 1622548800000L, "AtomicLong holding epoch milliseconds should be converted to a Date with the same timestamp"), // ---- Map containing INSTANT key ------------------------------------------------
        Arguments.of(Map.of(INSTANT, Instant.ofEpochMilli(1622548800000L)), null, 1622548800000L, "Map containing the INSTANT key with an Instant value should be converted to the corresponding Date"), // ---- Numeric String ------------------------------------------------------------
        Arguments.of("1622548800000", null, 1622548800000L, "String containing a numeric epoch millisecond value should be parsed and converted to a Date"), // ---- Custom Converter (fallback for unsupported type) -------------------------
        Arguments.of(new Object() {

            public long toLong() {
                return 1622548800000L;
            }
        }, new Converter() {

            @Override
            public long convert(Object src) {
                // Reflective fallback used by the library; we simply delegate to the object's method.
                try {
                    return (long) src.getClass().getMethod("toLong").invoke(src);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }, 1622548800000L, "When the source type is not directly supported, the provided Converter should be invoked to obtain the epoch milliseconds"), // ---- Zero epoch ---------------------------------------------------------------
        Arguments.of(0L, null, 0L, "Zero epoch milliseconds (Long) should produce the Unix epoch start date"), // ---- Negative epoch -----------------------------------------------------------
        Arguments.of(-1L, null, -1L, "Negative epoch milliseconds should be handled correctly, yielding a Date before the Unix epoch"), // ---- Long.MAX_VALUE -----------------------------------------------------------
        Arguments.of(Long.MAX_VALUE, null, Long.MAX_VALUE, "Maximum long value should be converted without overflow, resulting in a far‑future Date"), // ---- Long.MIN_VALUE -----------------------------------------------------------
        Arguments.of(Long.MIN_VALUE, null, Long.MIN_VALUE, "Minimum long value should be converted, producing a far‑past Date (may overflow internal Date handling)"), // ---- java.sql.Date (subclass of java.util.Date) -------------------------------
        Arguments.of(new java.sql.Date(1622548800000L), null, 1622548800000L, "java.sql.Date (subclass of java.util.Date) should be treated like a regular Date"), // ---- ZonedDateTime with non‑UTC zone -----------------------------------------
        Arguments.of(ZonedDateTime.of(2021, 6, 1, 12, 0, 0, 0, ZoneId.of("America/New_York")), null, ZonedDateTime.of(2021, 6, 1, 12, 0, 0, 0, ZoneId.of("America/New_York")).toInstant().toEpochMilli(), "ZonedDateTime in a non‑UTC zone should be converted using its offset (EDT = UTC‑4)"), // ---- OffsetDateTime with -05:00 offset ---------------------------------------
        Arguments.of(OffsetDateTime.of(2021, 6, 1, 12, 0, 0, 0, ZoneOffset.ofHours(-5)), null, OffsetDateTime.of(2021, 6, 1, 12, 0, 0, 0, ZoneOffset.ofHours(-5)).toInstant().toEpochMilli(), "OffsetDateTime with -05:00 offset should be converted to UTC epoch milliseconds"), // ---- String with surrounding whitespace ---------------------------------------
        Arguments.of("   1622548800000   ", null, 1622548800000L, "String with surrounding whitespace should be trimmed and parsed correctly"), // ---- Decimal String (fraction ignored) ---------------------------------------
        Arguments.of("1622548800.123", null, 1622548800L, "String representing a decimal number should be parsed as long (fraction ignored) and converted"), // ---- Object whose toString returns numeric string -----------------------------
        Arguments.of(new Object() {

            @Override
            public String toString() {
                return "1622548800000";
            }
        }, null, 1622548800000L, "Object whose toString yields a numeric string should be parsed via the fallback string handling"), // ---- Negative AtomicLong -------------------------------------------------------
        Arguments.of(new AtomicLong(-1622548800000L), null, -1622548800000L, "Negative value held in AtomicLong should be converted correctly"), // ---- Map without INSTANT key + custom converter -------------------------------
        Arguments.of(Map.of("otherKey", "value"), new Converter() {

            @Override
            public long convert(Object src) {
                return 123456789L;
            }
        }, 123456789L, "Map without INSTANT key triggers the custom Converter, which returns a fixed epoch value"), // ---- Object with public toLong() method (reflection) -------------------------
        Arguments.of(new Object() {

            public long toLong() {
                return 0L;
            }
        }, null, 0L, "Object with a public toLong() method should be recognized via reflection and used"), // ---- Object with public toInt() method (reflection) --------------------------
        Arguments.of(new Object() {

            public int toInt() {
                return 1622548800;
            }
        }, null, 1622548800L, "Object with a public toInt() method should be used as an integer millisecond source"));
    }

    /**
     * Supplies all scenarios that should result in an exception.
     */
    private static Stream<Arguments> exceptionCases() {
        return Stream.of(// Null source object – the library's toLong throws NPE
        Arguments.of(null, null, NullPointerException.class, "Passing a null source object should cause a NullPointerException because toLong cannot process null"), // Custom converter throws IllegalArgumentException
        Arguments.of(new Object(), new Converter() {

            @Override
            public long convert(Object src) {
                throw new IllegalArgumentException("Unsupported type");
            }
        }, IllegalArgumentException.class, "When a custom Converter is supplied but it throws an IllegalArgumentException for an unsupported type, the exception should propagate"), // Non‑numeric string
        Arguments.of("notANumber", null, NumberFormatException.class, "String that cannot be parsed as a number should cause NumberFormatException during parsing"), // Object whose toString returns non‑numeric string
        Arguments.of(new Object() {

            @Override
            public String toString() {
                return "notANumber";
            }
        }, null, NumberFormatException.class, "Object whose toString returns a non‑numeric string should trigger parsing failure resulting in NumberFormatException"), // Reflection method throws RuntimeException (toLong)
        Arguments.of(new Object() {

            public long toLong() {
                throw new RuntimeException("boom");
            }
        }, null, RuntimeException.class, "Reflection‑based toLong method throws an exception; the exception should propagate"), // Reflection method throws RuntimeException (toInt)
        Arguments.of(new Object() {

            public int toInt() {
                throw new RuntimeException("boom");
            }
        }, null, RuntimeException.class, "Reflection‑based toInt method throws an exception; the exception should propagate"), // Unsupported numeric method (toDouble) – library expects long/int, so ClassCastException
        Arguments.of(new Object() {

            public double toDouble() {
                return 123.45;
            }
        }, null, ClassCastException.class, "Unsupported numeric method (toDouble) is not handled; conversion should fail with ClassCastException or similar"), // Object with no recognizable conversion methods
        Arguments.of(new Object() {

            public Object getValue() {
                return new Object();
            }
        }, null, IllegalArgumentException.class, "Object with no recognizable conversion methods should cause IllegalArgumentException indicating unsupported source type"));
    }

    @Test
    void toDate_fromDate_returnsSameInstant() {
        Date now = new Date();
        Date result = InstantConversions.toDate(now, converter);
        assertEquals(now.getTime(), result.getTime(), "Converting a Date should preserve the millisecond value");
    }

    @Test
    void toDate_fromLong_returnsCorrespondingDate() {
        // 2021‑05‑01T00:00:00Z
        long epochMillis = 1_620_505_600_000L;
        Date result = InstantConversions.toDate(epochMillis, converter);
        assertEquals(epochMillis, result.getTime(), "Converting a Long (epoch ms) should produce a Date with the same epoch");
    }

    @Test
    void toDate_fromString_returnsCorrespondingDate() {
        // 2021‑06‑01T00:00:00Z
        String epochString = "1622505600000";
        long expected = Long.parseLong(epochString);
        Date result = InstantConversions.toDate(epochString, converter);
        assertEquals(expected, result.getTime(), "Converting a numeric String should produce a Date with the parsed epoch value");
    }

    @Test
    void toDate_fromInstant_returnsCorrespondingDate() {
        Instant instant = Instant.parse("2022-01-15T12:34:56.789Z");
        Date result = InstantConversions.toDate(instant, converter);
        assertEquals(instant.toEpochMilli(), result.getTime(), "Converting an Instant should yield a Date with the same epoch milliseconds");
    }

    @Test
    void toDate_fromLocalDateTime_returnsCorrespondingDate() {
        // Use UTC to have a deterministic epoch value
        LocalDateTime ldt = LocalDateTime.of(2020, Month.DECEMBER, 31, 23, 59, 59, 123_000_000);
        ZonedDateTime zdt = ldt.atZone(ZoneOffset.UTC);
        long expected = zdt.toInstant().toEpochMilli();
        Date result = InstantConversions.toDate(ldt, converter);
        assertEquals(expected, result.getTime(), "Converting a LocalDateTime (interpreted as UTC) should match the expected epoch ms");
    }

    @Test
    void toDate_fromCalendar_returnsCorrespondingDate() {
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        // 2020‑09‑13T12:26:40Z
        cal.setTimeInMillis(1_600_000_000_000L);
        Date result = InstantConversions.toDate(cal, converter);
        assertEquals(cal.getTimeInMillis(), result.getTime(), "Converting a Calendar should produce a Date with the same millisecond value");
    }

    @Test
    void toDate_fromNull_returnsEpochZeroDate() {
        Date result = InstantConversions.toDate(null, converter);
        assertEquals(0L, result.getTime(), "Converting null should result in a Date representing the epoch (0 ms)");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("successCases")
    void toDate_successful(Object from, Converter customConverter, long expectedEpochMillis, String description) {
        // Use the custom converter if supplied, otherwise fall back to the default one.
        Converter used = (customConverter != null) ? customConverter : converter;
        Date result = InstantConversions.toDate(from, used);
        assertNotNull(result, "Resulting Date should never be null");
        assertEquals(expectedEpochMillis, result.getTime(), () -> "Failed conversion: " + description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("exceptionCases")
    void toDate_exceptions(Object from, Converter customConverter, Class<? extends Throwable> expectedException, String description) {
        Converter used = (customConverter != null) ? customConverter : converter;
        assertThrows(expectedException, () -> InstantConversions.toDate(from, used), () -> "Expected exception not thrown: " + description);
    }
}
