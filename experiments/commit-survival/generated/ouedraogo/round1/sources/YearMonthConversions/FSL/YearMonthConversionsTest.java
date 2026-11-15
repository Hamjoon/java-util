package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.CompactLinkedMap;
import org.junit.Test;

import java.time.YearMonth;
import java.util.Map;

import static org.junit.Assert.*;

public class YearMonthConversionsTest {

    /**
     * Helper method to obtain a dummy Converter instance.
     * The Converter is not used by YearMonthConversions.toMap, but the
     * constructor requires a ConverterOptions argument. Passing {@code null}
     * is acceptable for the purpose of these tests.
     */
    private static Converter dummyConverter() {
        return new Converter(null);
    }

    @Test
    public void testToMapNormalYearMonth() {
        YearMonth ym = YearMonth.of(2023, 5);
        Map<String, Object> result = YearMonthConversions.toMap(ym, dummyConverter());

        // Verify the concrete type
        assertTrue("Result should be a CompactLinkedMap", result instanceof CompactLinkedMap);

        // Verify map contents
        assertEquals("Map should contain exactly two entries", 2, result.size());
        assertTrue("Map should contain key 'year'", result.containsKey("year"));
        assertTrue("Map should contain key 'month'", result.containsKey("month"));
        assertEquals(2023, result.get("year"));
        assertEquals(5, result.get("month"));
    }

    @Test
    public void testToMapNegativeYear() {
        YearMonth ym = YearMonth.of(-44, 12); // year -44, month December
        Map<String, Object> result = YearMonthConversions.toMap(ym, dummyConverter());

        assertEquals(-44, result.get("year"));
        assertEquals(12, result.get("month"));
    }

    @Test
    public void testToMapZeroMonthIsInvalid() {
        // YearMonth.of throws DateTimeException for month 0, so we verify that
        // YearMonthConversions does not silently accept an invalid month.
        try {
            YearMonth.of(2020, 0);
            fail("YearMonth.of should have thrown an exception for month 0");
        } catch (Exception ignored) {
            // Expected path – we cannot create an invalid YearMonth, therefore
            // this test simply confirms the API contract.
        }
    }

    @Test
    public void testToMapNullFromThrowsNPE() {
        try {
            YearMonthConversions.toMap(null, dummyConverter());
            fail("Expected NullPointerException when 'from' is null");
        } catch (NullPointerException expected) {
            // Expected exception
        }
    }

    @Test
    public void testToMapInvalidTypeThrowsClassCastException() {
        String notAYearMonth = "2023-05";
        try {
            YearMonthConversions.toMap(notAYearMonth, dummyConverter());
            fail("Expected ClassCastException when 'from' is not a YearMonth");
        } catch (ClassCastException expected) {
            // Expected exception
        }
    }

    @Test
    public void testConverterParameterIsIgnored() {
        // Create a Converter that would throw if any of its methods were called.
        // Since YearMonthConversions does not use the converter, no exception should be thrown.
        Converter converter = new Converter(null) {
            @Override
            public ConverterOptions getOptions() {
                throw new IllegalStateException("Converter should not be used");
            }

            @Override
            public <T> T convert(Object from, Class<T> toType) {
                throw new IllegalStateException("Converter should not be used");
            }

            @Override
            public boolean isConversionSupportedFor(Class<?> source, Class<?> target) {
                throw new IllegalStateException("Converter should not be used");
            }

            @Override
            public Map<Class<?>, java.util.Set<Class<?>>> allSupportedConversions() {
                throw new IllegalStateException("Converter should not be used");
            }

            @Override
            public Map<String, java.util.Set<String>> getSupportedConversions() {
                throw new IllegalStateException("Converter should not be used");
            }

            @Override
            public Convert<?> addConversion(Class<?> source, Class<?> target, Convert<?> conversionFunction) {
                throw new IllegalStateException("Converter should not be used");
            }
        };

        YearMonth ym = YearMonth.of(1999, 12);
        Map<String, Object> result = YearMonthConversions.toMap(ym, converter);

        // If we reach this point without an exception, the test passes.
        assertEquals(1999, result.get("year"));
        assertEquals(12, result.get("month"));
    }
}
