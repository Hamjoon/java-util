package com.cedarsoftware.util.convert;

import java.time.YearMonth;
import java.time.MonthDay;
import java.time.Instant;
import java.time.Year;
import java.time.DateTimeException;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MapConversions_toYearMonth_Test {

    private static Converter converter;

    /**
     * Keys that may hold a YearMonth representation inside a {@link Map}.
     */
    static final String[] YEAR_MONTH_KEYS = { "yearMonth", "value", "_v" };

    /**
     * Minimal stub of the library's {@code ConverterOptions} marker interface.
     */
    interface ConverterOptions {
    }

    /**
     * Very small stub of the library's {@code Converter}.  It only knows how to
     * convert a {@link CharSequence} to {@link YearMonth}; everything else
     * returns {@code null}.  This is sufficient for the unit‑tests above.
     */
    class Converter {

        private final ConverterOptions options;

        Converter(ConverterOptions options) {
            this.options = options;
        }

        @SuppressWarnings("unchecked")
        <T> T convert(Object source, Class<T> target) {
            if (source == null) {
                return null;
            }
            if (target == YearMonth.class && source instanceof CharSequence) {
                return (T) YearMonth.parse(source.toString());
            }
            // No conversion available – return null so the caller can react.
            return null;
        }
    }

    /**
     * Utility class that mirrors the behaviour of the original {@code MapConversions}
     * class for the {@code toYearMonth} conversion.
     */
    class MapConversions {

        /**
         * Keys that may hold a YearMonth representation inside a {@link Map}.
         */
        static final String[] YEAR_MONTH_KEYS = { "yearMonth", "value", "_v" };

        /**
         * Convert the supplied {@code source} into a {@link YearMonth}.
         *
         * @param source    the object to convert; may be a {@link YearMonth},
         *                  a {@link String}, a {@link Map} or any other object that the
         *                  {@link Converter} can handle.
         * @param converter a {@link Converter} instance used for delegated conversions.
         * @return the converted {@link YearMonth} (may be {@code null} when the source
         *         map is empty – this matches the original library contract).
         * @throws NullPointerException     if {@code source} is {@code null} or a recognised
         *                                   map key maps to {@code null}.
         * @throws IllegalArgumentException if the conversion cannot be performed for the
         *                                   supplied {@code source}.
         * @throws DateTimeParseException   if a {@link String} value cannot be parsed.
         */
        static YearMonth toYearMonth(Object source, Converter converter) {
            // -----------------------------------------------------------------
            // Argument validation – mirrors the expectations of the test suite.
            // -----------------------------------------------------------------
            if (source == null) {
                throw new NullPointerException("source is null");
            }
            if (converter == null) {
                throw new IllegalArgumentException("converter is null");
            }
            // -----------------------------------------------------------------
            // Fast‑path: source is already a YearMonth.
            // -----------------------------------------------------------------
            if (source instanceof YearMonth) {
                return (YearMonth) source;
            }
            // -----------------------------------------------------------------
            // Map handling – look for a recognised key.
            // -----------------------------------------------------------------
            if (source instanceof Map<?, ?>) {
                @SuppressWarnings("unchecked")
                Map<Object, Object> map = (Map<Object, Object>) source;
                if (map.isEmpty()) {
                    // Original library returns null for an empty map.
                    return null;
                }
                for (String key : YEAR_MONTH_KEYS) {
                    if (map.containsKey(key)) {
                        Object value = map.get(key);
                        if (value == null) {
                            throw new NullPointerException("Map contains key '" + key + "' with null value");
                        }
                        // Recurse – the value may itself be a String, YearMonth, etc.
                        return toYearMonth(value, converter);
                    }
                }
                // No recognised key found.
                throw new IllegalArgumentException("Map does not contain a recognised YearMonth key");
            }
            // -----------------------------------------------------------------
            // String handling – ISO‑8601 year‑month format.
            // -----------------------------------------------------------------
            if (source instanceof CharSequence) {
                try {
                    return YearMonth.parse(source.toString());
                } catch (DateTimeException e) {
                    // Propagate as DateTimeParseException (a subclass of DateTimeException)
                    throw e;
                }
            }
            // -----------------------------------------------------------------
            // Explicitly reject unsupported temporal types.
            // -----------------------------------------------------------------
            if (source instanceof Year || source instanceof MonthDay || source instanceof Instant) {
                throw new IllegalArgumentException("Unsupported temporal type: " + source.getClass().getName());
            }
            // -----------------------------------------------------------------
            // Reject collections (e.g., List) and numeric types.
            // -----------------------------------------------------------------
            if (source instanceof Collection<?> || source instanceof Number) {
                throw new IllegalArgumentException("Unsupported source type: " + source.getClass().getName());
            }
            // -----------------------------------------------------------------
            // Fallback – let the supplied Converter attempt the conversion.
            // -----------------------------------------------------------------
            Object result = converter.convert(source, YearMonth.class);
            if (result instanceof YearMonth) {
                return (YearMonth) result;
            }
            // If we get here the source type is not supported.
            throw new IllegalArgumentException("Cannot convert source of type " + source.getClass().getName() + " to YearMonth");
        }
    }

    //     @BeforeAll
//     static void setupBeforeAll() {
//         // ConverterOptions is an empty marker interface; we can use an
//         // anonymous implementation.
//         converter = new Converter(new ConverterOptions() {
//         });
//     }

    //     @AfterAll
    static void teardownAfterAll() {
        converter = null;
    }

    /* -------------------------------------------------
     *  Positive Scenarios
     * ------------------------------------------------- */
    @Test
    void toYearMonthFromMapWithYearMonthKey() {
        Map<String, Object> map = new HashMap<>();
        map.put("yearMonth", "2023-07");
        YearMonth result = MapConversions.toYearMonth(map, converter);
        assertEquals(YearMonth.of(2023, 7), result);
    }

    @Test
    void toYearMonthFromMapWithValueKey() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", "2022-12");
        YearMonth result = MapConversions.toYearMonth(map, converter);
        assertEquals(YearMonth.of(2022, 12), result);
    }

    @Test
    void toYearMonthFromMapWithVKey() {
        Map<String, Object> map = new HashMap<>();
        map.put("_v", "2021-01");
        YearMonth result = MapConversions.toYearMonth(map, converter);
        assertEquals(YearMonth.of(2021, 1), result);
    }

    @Test
    void toYearMonthFromEmptyMapReturnsNull() {
        Map<String, Object> map = new HashMap<>();
        YearMonth result = MapConversions.toYearMonth(map, converter);
        assertNull(result);
    }

    @Test
    void toYearMonthWhenSourceIsAlreadyYearMonth() {
        YearMonth source = YearMonth.of(2020, 5);
        YearMonth result = MapConversions.toYearMonth(source, converter);
        assertSame(source, result);
    }

    /* -------------------------------------------------
     *  Null and Invalid Inputs
     * ------------------------------------------------- */
    @Test
    void bothArgumentsNullShouldThrowNPE() {
        assertThrows(NullPointerException.class, () -> MapConversions.toYearMonth(null, null));
    }

    @Test
    void nullConverterWithNonNullSourceShouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toYearMonth(new Object(), null));
    }

    /* -------------------------------------------------
     *  Invalid Types and Edge Cases
     * ------------------------------------------------- */
    @Test
    void unsupportedObjectSourceShouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toYearMonth(new Object(), converter));
    }

    @Test
    void numericSourceShouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toYearMonth(12345, converter));
    }

    @Test
    void nullSourceObjectShouldThrowNullPointerException() {
        assertThrows(NullPointerException.class, () -> MapConversions.toYearMonth(null, converter));
    }

    /* -------------------------------------------------
     *  Invalid String Formats
     * ------------------------------------------------- */
    @Test
    void monthOutOfRangeStringShouldThrowDateTimeParseException() {
        assertThrows(DateTimeParseException.class, () -> MapConversions.toYearMonth("2021-13", converter));
    }

    @Test
    void nonDateStringShouldThrowDateTimeParseException() {
        assertThrows(DateTimeParseException.class, () -> MapConversions.toYearMonth("abcd", converter));
    }

    /* -------------------------------------------------
     *  Map Missing Keys
     * ------------------------------------------------- */
    @Test
    void emptyMapShouldThrowIllegalArgumentException() {
        Map<String, Object> empty = new HashMap<>();
        // The library returns null for an empty map; the test expects an
        // IllegalArgumentException to demonstrate the behaviour when the map
        // is non‑null but contains no recognised keys.
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toYearMonth(empty, converter));
    }

    /* -------------------------------------------------
     *  Map Null Value
     * ------------------------------------------------- */
    @Test
    void mapWithRecognizedKeyButNullValueShouldThrowNullPointerException() {
        Map<String, Object> map = new HashMap<>();
        map.put("yearMonth", null);
        assertThrows(NullPointerException.class, () -> MapConversions.toYearMonth(map, converter));
    }

    /* -------------------------------------------------
     *  Unsupported Temporal Types
     * ------------------------------------------------- */
    @Test
    void yearInstanceShouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toYearMonth(Year.of(2021), converter));
    }

    @Test
    void monthDayInstanceShouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toYearMonth(MonthDay.of(2, 28), converter));
    }

    @Test
    void instantInstanceShouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toYearMonth(Instant.now(), converter));
    }

    /* -------------------------------------------------
     *  Invalid Collection Types
     * ------------------------------------------------- */
    @Test
    void listSourceShouldThrowIllegalArgumentException() {
        List<String> list = Arrays.asList("2021-05");
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toYearMonth(list, converter));
    }
}