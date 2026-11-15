package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Assert;

import java.time.MonthDay;
import java.util.Map;

import static com.cedarsoftware.util.convert.MapConversions.MONTH_DAY;

/**
 * JUnit‑4 test suite for {@link MonthDayConversions}.
 *
 * The class under test is package‑private and has a single static method:
 * <pre>
 * static Map toMap(Object from, Converter converter)
 * </pre>
 *
 * The tests cover:
 * <ul>
 *   <li>Typical conversion of a valid {@code MonthDay} instance.</li>
 *   <li>Edge cases – {@code null} input and wrong input type.</li>
 *   <li>Verification that the returned {@code Map} contains the expected key/value.</li>
 * </ul>
 *
 * The {@code Converter} argument is not used by the implementation, therefore a
 * minimal instance (constructed with {@code null} options) is sufficient.
 */
public class MonthDayConversionsTest {

    /** Helper that creates a dummy {@link Converter}. */
    private static Converter dummyConverter() {
        // The Converter constructor accepts a ConverterOptions instance; the implementation
        // does not use the options for the toMap method, so passing {@code null} is safe.
        return new Converter(null);
    }

    /**
     * Basic happy‑path test: a normal {@code MonthDay} should be converted to a map
     * containing a single entry whose key is {@link MapConversions#MONTH_DAY}
     * and whose value is the ISO‑8601 string representation of the month‑day
     * (e.g. "--12-25").
     */
    @Test
    public void testToMap_WithValidMonthDay_ReturnsCorrectMap() {
        // Arrange
        MonthDay monthDay = MonthDay.of(12, 25);          // Christmas
        Converter converter = dummyConverter();

        // Act
        Map<String, Object> result = MonthDayConversions.toMap(monthDay, converter);

        // Assert
        Assert.assertNotNull("Result map must not be null", result);
        Assert.assertEquals("Map should contain exactly one entry", 1, result.size());
        Assert.assertTrue("Map must contain the MONTH_DAY key", result.containsKey(MONTH_DAY));
        Assert.assertEquals("Value must match MonthDay.toString()", monthDay.toString(),
                result.get(MONTH_DAY));
    }

    /**
     * Edge‑case test: passing {@code null} as the source object should result in a
     * {@link NullPointerException}. The implementation casts the argument to
     * {@code MonthDay} without a null‑check, so the NPE is expected.
     */
    @Test(expected = NullPointerException.class)
    public void testToMap_WithNullSource_ThrowsNullPointerException() {
        // Arrange
        Converter converter = dummyConverter();

        // Act
        MonthDayConversions.toMap(null, converter);

        // Assert – exception expected
    }

    /**
     * Edge‑case test: passing an object that is not a {@code MonthDay} should trigger
     * a {@link ClassCastException}. The method performs an unchecked cast, so the
     * exception propagates.
     */
    @Test(expected = ClassCastException.class)
    public void testToMap_WithInvalidType_ThrowsClassCastException() {
        // Arrange
        String notAMonthDay = "not-a-monthday";
        Converter converter = dummyConverter();

        // Act
        MonthDayConversions.toMap(notAMonthDay, converter);

        // Assert – exception expected
    }

    /**
     * Additional edge test: a leap‑day {@code MonthDay} (February 29) must be
     * handled correctly – the string representation includes the leading '--'.
     */
    @Test
    public void testToMap_WithLeapDay_ReturnsCorrectString() {
        // Arrange
        MonthDay leapDay = MonthDay.of(2, 29);   // valid only on leap years, but MonthDay permits it
        Converter converter = dummyConverter();

        // Act
        Map<String, Object> result = MonthDayConversions.toMap(leapDay, converter);

        // Assert
        Assert.assertEquals("Value must match MonthDay.toString() for leap day",
                leapDay.toString(), result.get(MONTH_DAY));
    }
}
