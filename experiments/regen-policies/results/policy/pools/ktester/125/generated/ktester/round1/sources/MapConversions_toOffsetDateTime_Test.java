package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.time.*;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

class MapConversions_toOffsetDateTime_Test {

    private static Converter converter;

    /**
     * Data holder for a single test case.
     */
    private static class Case {

        final Map<String, Object> map;

        // null if no exception from converter
        final RuntimeException converterException;

        // null if expecting a result
        final Class<? extends Throwable> expectedException;

        // null if expecting an exception
        final OffsetDateTime expectedResult;

        final String description;

        Case(Map<String, Object> map, RuntimeException converterException, Class<? extends Throwable> expectedException, OffsetDateTime expectedResult, String description) {
            this.map = map;
            this.converterException = converterException;
            this.expectedException = expectedException;
            this.expectedResult = expectedResult;
            this.description = description;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Converter with default options – sufficient for the built‑in conversions used in the tests
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // No resources to release, but method kept for symmetry with the template
        converter = null;
    }

    /**
     * Build a map from a var‑args sequence of key/value pairs.
     */
    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }

    /**
     * Configure the mock {@code Converter} for a successful conversion
     *  from {@code source} to {@code targetClass}.
     */
    private static <T> void whenConvert(Object source, Class<T> targetClass, T result) {
        when(converter.convert(eq(source), eq(targetClass))).thenReturn(result);
    }

    /**
     * Configure the mock to throw {@code ex} when converting {@code source}
     *  to {@code targetClass}.
     */
    private static <T> void whenConvertThrows(Object source, Class<T> targetClass, RuntimeException ex) {
        when(converter.convert(eq(source), eq(targetClass))).thenThrow(ex);
    }

    /**
     * Supplies all cases that need a fresh mock per execution.
     */
    private static Stream<Case> caseProvider() {
        // 1. time+offset, date absent – happy path
        Map<String, Object> m1 = map("time", "2023-03-15T10:20:30", "offset", "+02:00");
        OffsetDateTime exp1 = OffsetDateTime.of(LocalDateTime.of(2023, 3, 15, 10, 20, 30), ZoneOffset.ofHours(2));
        Case c1 = new Case(m1, null, null, exp1, "Both TIME and OFFSET keys are present and DATE is missing.");
        // 2. date+time+offset – happy path
        Map<String, Object> m2 = map("date", "2023-03-15", "time", "10:20:30", "offset", "+02:00");
        OffsetDateTime exp2 = OffsetDateTime.of(LocalDate.of(2023, 3, 15), LocalTime.of(10, 20, 30), ZoneOffset.ofHours(2));
        Case c2 = new Case(m2, null, null, exp2, "All three keys DATE, TIME and OFFSET are present.");
        // 3. missing TIME – expect IllegalArgumentException from fromMap
        Map<String, Object> m3 = map("date", "2023-03-15", "offset", "+02:00");
        Case c3 = new Case(m3, null, IllegalArgumentException.class, null, "TIME is absent while DATE and OFFSET are present.");
        // 4. missing OFFSET – expect IllegalArgumentException
        Map<String, Object> m4 = map("date", "2023-03-15", "time", "10:20:30");
        Case c4 = new Case(m4, null, IllegalArgumentException.class, null, "OFFSET is missing.");
        // 5. empty map – expect IllegalArgumentException
        Map<String, Object> m5 = map();
        Case c5 = new Case(m5, null, IllegalArgumentException.class, null, "Empty map provides none of the required keys.");
        // 6. null map argument – NullPointerException from map.get
        Case c6 = new Case(null, null, NullPointerException.class, null, "The 'from' argument is null.");
        // 7. non‑map input – ClassCastException
        Case c7 = new Case(map("dummy", "value"), /* will be ignored */
        null, ClassCastException.class, null, "Input is not a Map.");
        // 8. offset supplied as minutes integer
        Map<String, Object> m8 = map("time", "2023-03-15T10:20:30", "offset", 120);
        OffsetDateTime exp8 = OffsetDateTime.of(LocalDateTime.of(2023, 3, 15, 10, 20, 30), ZoneOffset.ofHours(2));
        Case c8 = new Case(m8, null, null, exp8, "OFFSET is provided as an integer representing minutes.");
        // 9. time already a LocalDateTime instance
        LocalDateTime ldt = LocalDateTime.of(2023, 3, 15, 10, 20, 30);
        Map<String, Object> m9 = map("time", ldt, "offset", "+02:00");
        OffsetDateTime exp9 = OffsetDateTime.of(ldt, ZoneOffset.ofHours(2));
        Case c9 = new Case(m9, null, null, exp9, "TIME entry is already a LocalDateTime object.");
        // 10. null converter – NullPointerException when calling convert
        Map<String, Object> m10 = map("time", "2023-03-15T10:20:30", "offset", "+02:00");
        Case c10 = new Case(m10, null, NullPointerException.class, null, "Converter argument is null.");
        // 11. invalid time format for LocalDateTime conversion
        Map<String, Object> m11 = map("time", "not-a-datetime", "offset", "+02:00");
        RuntimeException ex11 = new IllegalArgumentException("bad time");
        Case c11 = new Case(m11, ex11, IllegalArgumentException.class, null, "TIME value cannot be parsed to LocalDateTime.");
        // 12. invalid time format for LocalTime conversion (date present)
        Map<String, Object> m12 = map("date", "2023-03-15", "time", "25:61:61", "offset", "+02:00");
        RuntimeException ex12 = new IllegalArgumentException("bad local time");
        Case c12 = new Case(m12, ex12, IllegalArgumentException.class, null, "Malformed time string when DATE is present.");
        // 13. invalid offset format
        Map<String, Object> m13 = map("time", "2023-03-15T10:20:30", "offset", "invalid-offset");
        RuntimeException ex13 = new IllegalArgumentException("bad offset");
        Case c13 = new Case(m13, ex13, IllegalArgumentException.class, null, "OFFSET value cannot be parsed to ZoneOffset.");
        // 14. invalid date format
        Map<String, Object> m14 = map("date", "2023-02-30", "time", "10:20:30", "offset", "+02:00");
        RuntimeException ex14 = new IllegalArgumentException("bad date");
        Case c14 = new Case(m14, ex14, IllegalArgumentException.class, null, "Impossible date value.");
        // 15. converter returns null for time (fast‑path)
        Map<String, Object> m15 = map("time", "2023-03-15T10:20:30", "offset", "+02:00");
        Case c15 = new Case(m15, null, NullPointerException.class, null, "Converter returns null for LocalDateTime.");
        // 16. converter returns null for offset (fast‑path)
        Map<String, Object> m16 = map("time", "2023-03-15T10:20:30", "offset", "+02:00");
        Case c16 = new Case(m16, null, NullPointerException.class, null, "Converter returns null for ZoneOffset.");
        // 17. converter returns null for date (full‑path)
        Map<String, Object> m17 = map("date", "2023-03-15", "time", "10:20:30", "offset", "+02:00");
        Case c17 = new Case(m17, null, NullPointerException.class, null, "Converter returns null for LocalDate.");
        // 18. converter returns null for time (full‑path)
        Map<String, Object> m18 = map("date", "2023-03-15", "time", "10:20:30", "offset", "+02:00");
        Case c18 = new Case(m18, null, NullPointerException.class, null, "Converter returns null for LocalTime.");
        return Stream.of(c1, c2, c3, c4, c5, c6, c7, c8, c9, c10, c11, c12, c13, c14, c15, c16, c17, c18);
    }

    @Test
    void shouldCreateOffsetDateTimeFromTimeAndOffsetWhenDateIsMissing() {
        // GIVEN a map that contains only "time" and "offset"
        Map<String, Object> map = new HashMap<>();
        // convertible to LocalDateTime
        map.put("time", "2023-07-20T15:30:45");
        // convertible to ZoneOffset
        map.put("offset", "+02:00");
        // WHEN converting
        OffsetDateTime result = MapConversions.toOffsetDateTime(map, converter);
        // THEN the result is built from LocalDateTime + ZoneOffset
        OffsetDateTime expected = OffsetDateTime.of(LocalDateTime.of(2023, 7, 20, 15, 30, 45), ZoneOffset.ofHours(2));
        assertEquals(expected, result);
    }

    @Test
    void shouldCreateOffsetDateTimeFromDateTimeAndOffsetWhenAllThreeKeysPresent() {
        // GIVEN a map that contains "date", "time" and "offset"
        Map<String, Object> map = new HashMap<>();
        // convertible to LocalDate
        map.put("date", "2023-07-20");
        // convertible to LocalTime
        map.put("time", "15:30:45");
        // convertible to ZoneOffset
        map.put("offset", "+02:00");
        // WHEN converting
        OffsetDateTime result = MapConversions.toOffsetDateTime(map, converter);
        // THEN the result is built from LocalDate + LocalTime + ZoneOffset
        OffsetDateTime expected = OffsetDateTime.of(LocalDate.of(2023, 7, 20), LocalTime.of(15, 30, 45), ZoneOffset.ofHours(2));
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "{index}: {0.description}")
    @MethodSource("caseProvider")
    void comprehensiveToOffsetDateTimeTests(Case tc) {
        // Reset mock for each iteration
        reset(converter);
        // -----------------------------------------------------------------
        // Prepare mock behaviour based on the case description
        // -----------------------------------------------------------------
        if (tc.map != null) {
            // Fast‑path: time + offset, date absent
            if (tc.map.containsKey("time") && tc.map.containsKey("offset") && !tc.map.containsKey("date")) {
                Object timeVal = tc.map.get("time");
                Object offsetVal = tc.map.get("offset");
                // Configure conversion for time -> LocalDateTime (or null / exception)
                if (tc.converterException != null && tc.converterException.getMessage().contains("bad time")) {
                    whenConvertThrows(timeVal, LocalDateTime.class, tc.converterException);
                } else if (tc.expectedException == NullPointerException.class && tc.description.contains("null for LocalDateTime")) {
                    whenConvert(timeVal, LocalDateTime.class, null);
                } else {
                    // normal conversion
                    LocalDateTime ldt = (timeVal instanceof LocalDateTime) ? (LocalDateTime) timeVal : LocalDateTime.of(2023, 3, 15, 10, 20, 30);
                    whenConvert(timeVal, LocalDateTime.class, ldt);
                }
                // Configure conversion for offset -> ZoneOffset
                if (tc.converterException != null && tc.converterException.getMessage().contains("bad offset")) {
                    whenConvertThrows(offsetVal, ZoneOffset.class, tc.converterException);
                } else if (tc.expectedException == NullPointerException.class && tc.description.contains("null for ZoneOffset")) {
                    whenConvert(offsetVal, ZoneOffset.class, null);
                } else if (offsetVal instanceof Integer) {
                    // integer minutes -> ZoneOffset.ofTotalSeconds(minutes*60)
                    int minutes = (Integer) offsetVal;
                    ZoneOffset zo = ZoneOffset.ofTotalSeconds(minutes * 60);
                    whenConvert(offsetVal, ZoneOffset.class, zo);
                } else {
                    ZoneOffset zo = ZoneOffset.ofHours(2);
                    whenConvert(offsetVal, ZoneOffset.class, zo);
                }
            }
            // Full‑path: date + time + offset
            if (tc.map.containsKey("date") && tc.map.containsKey("time") && tc.map.containsKey("offset")) {
                Object dateVal = tc.map.get("date");
                Object timeVal = tc.map.get("time");
                Object offsetVal = tc.map.get("offset");
                // date -> LocalDate
                if (tc.converterException != null && tc.converterException.getMessage().contains("bad date")) {
                    whenConvertThrows(dateVal, LocalDate.class, tc.converterException);
                } else if (tc.expectedException == NullPointerException.class && tc.description.contains("null for LocalDate")) {
                    whenConvert(dateVal, LocalDate.class, null);
                } else {
                    LocalDate ld = (dateVal instanceof LocalDate) ? (LocalDate) dateVal : LocalDate.of(2023, 3, 15);
                    whenConvert(dateVal, LocalDate.class, ld);
                }
                // time -> LocalTime
                if (tc.converterException != null && tc.converterException.getMessage().contains("bad local time")) {
                    whenConvertThrows(timeVal, LocalTime.class, tc.converterException);
                } else if (tc.expectedException == NullPointerException.class && tc.description.contains("null for LocalTime")) {
                    whenConvert(timeVal, LocalTime.class, null);
                } else {
                    LocalTime lt = (timeVal instanceof LocalTime) ? (LocalTime) timeVal : LocalTime.of(10, 20, 30);
                    whenConvert(timeVal, LocalTime.class, lt);
                }
                // offset -> ZoneOffset (same handling as above)
                if (tc.converterException != null && tc.converterException.getMessage().contains("bad offset")) {
                    whenConvertThrows(offsetVal, ZoneOffset.class, tc.converterException);
                } else if (tc.expectedException == NullPointerException.class && tc.description.contains("null for ZoneOffset")) {
                    whenConvert(offsetVal, ZoneOffset.class, null);
                } else {
                    ZoneOffset zo = ZoneOffset.ofHours(2);
                    whenConvert(offsetVal, ZoneOffset.class, zo);
                }
            }
        }
        // -----------------------------------------------------------------
        // Execute the method under test and verify expectations
        // -----------------------------------------------------------------
        if (tc.expectedException != null) {
            // Cases where we expect an exception from the method itself
            assertThrows(tc.expectedException, () -> {
                // special handling for the "null converter" case
                Converter usedConverter = tc.description.contains("null converter") ? null : converter;
                MapConversions.toOffsetDateTime(tc.map, usedConverter);
            }, tc.description);
        } else {
            // Expect a successful result
            OffsetDateTime result = MapConversions.toOffsetDateTime(tc.map, converter);
            assertEquals(tc.expectedResult, result, tc.description);
        }
    }
}
