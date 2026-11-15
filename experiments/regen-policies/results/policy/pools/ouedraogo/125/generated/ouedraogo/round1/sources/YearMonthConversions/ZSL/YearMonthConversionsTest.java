package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Assert;

import java.time.YearMonth;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Unit tests for {@link YearMonthConversions}.
 *
 * The tests verify:
 * <ul>
 *   <li>Correct conversion of a {@link YearMonth} into a {@link Map} with expected keys/values.</li>
 *   <li>The insertion order of the map (\"year\" first, then \"month\").</li>
 *   <li>Behaviour when an unsupported source type is supplied (ClassCastException).</li>
 *   <li>Behaviour when {@code null} is supplied as the source object (NullPointerException).</li>
 * </ul>
 *
 * The {@code Converter} argument is not used by the implementation, therefore a {@code null}
 * reference is sufficient for the tests.
 */
public class YearMonthConversionsTest {

    /**
     * Helper method to create a {@link Converter} instance.
     * The implementation of {@code YearMonthConversions.toMap} does not invoke any
     * method on the converter, so we can safely pass {@code null}. The method is kept
     * for readability and future‑proofing.
     */
    private static Converter dummyConverter() {
        return null; // No interaction required.
    }

    @Test
    public void testToMapProducesCorrectEntriesAndInsertionOrder() {
        // Arrange
        YearMonth ym = YearMonth.of(2023, 7); // July 2023
        Converter converter = dummyConverter();

        // Act
        Map result = YearMonthConversions.toMap(ym, converter);

        // Assert size and content
        Assert.assertEquals("Map should contain exactly two entries", 2, result.size());
        Assert.assertTrue("Map should contain key 'year'", result.containsKey("year"));
        Assert.assertTrue("Map should contain key 'month'", result.containsKey("month"));
        Assert.assertEquals("Year value should match", 2023, result.get("year"));
        Assert.assertEquals("Month value should match", 7, result.get("month"));

        // Assert insertion order: first entry must be 'year', second entry must be 'month'
        Iterator<Entry> iterator = result.entrySet().iterator();
        Assert.assertTrue("Map should have a first entry", iterator.hasNext());
        Entry first = iterator.next();
        Assert.assertEquals("First entry key should be 'year'", "year", first.getKey());
        Assert.assertEquals("First entry value should be the year", 2023, first.getValue());

        Assert.assertTrue("Map should have a second entry", iterator.hasNext());
        Entry second = iterator.next();
        Assert.assertEquals("Second entry key should be 'month'", "month", second.getKey());
        Assert.assertEquals("Second entry value should be the month", 7, second.getValue());

        Assert.assertFalse("No more entries should exist", iterator.hasNext());
    }

    @Test(expected = ClassCastException.class)
    public void testToMapThrowsClassCastExceptionForInvalidSourceType() {
        // Arrange: an object that is NOT a YearMonth
        String notAYearMonth = "2023-07";
        Converter converter = dummyConverter();

        // Act: this should throw ClassCastException when the method attempts to cast
        YearMonthConversions.toMap(notAYearMonth, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToMapThrowsNullPointerExceptionWhenSourceIsNull() {
        // Arrange
        Converter converter = dummyConverter();

        // Act: passing null as the source triggers NPE when getYear() is invoked
        YearMonthConversions.toMap(null, converter);
    }
}
