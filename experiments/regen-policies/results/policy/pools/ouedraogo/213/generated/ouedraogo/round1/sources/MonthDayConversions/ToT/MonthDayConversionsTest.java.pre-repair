package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Assert;

import java.time.MonthDay;
import java.util.Map;

import static com.cedarsoftware.util.convert.MapConversions.MONTH_DAY;

/**
 * Comprehensive JUnit‑4 test suite for {@link MonthDayConversions}.
 *
 * The tests were devised by three “experts” who each contributed one
 * scenario before converging on a single, exhaustive test class.
 *
 * Scenarios covered:
 * 1. Normal conversion of a valid {@link MonthDay} instance.
 * 2. Passing {@code null} as the source object – expecting {@link NullPointerException}.
 * 3. Passing an object of an incompatible type – expecting {@link ClassCastException}.
 * 4. Supplying a {@code null} {@link Converter} – the method must still succeed because the
 *    converter is not used by the implementation.
 */
public class MonthDayConversionsTest {

    /**
     * Helper that creates a {@link Converter} instance.
     * The {@code Converter} constructor requires a {@link ConverterOptions} object.
     * The real library provides a public no‑arg constructor for {@code ConverterOptions},
     * so we can safely instantiate it directly.
     *
     * @return a usable {@link Converter} (may be {@code null} for the specific test that checks
     *         null handling).
     */
    private static Converter createConverter() {
        // The real ConverterOptions class has a public no‑arg constructor.
        // If the library ever changes, this method will be the single place to adjust.
        return new Converter(new ConverterOptions());
    }

    /**
     * Verify that a valid {@link MonthDay} is converted into a map that contains a single
     * entry with the correct key and the correctly formatted value.
     */
    @Test
    public void testToMap_ValidMonthDay_ReturnsCorrectMap() {
        // Arrange
        MonthDay monthDay = MonthDay.of(12, 25);               // Christmas
        Converter converter = createConverter();

        // Act
        Map<String, Object> result = MonthDayConversions.toMap(monthDay, converter);

        // Assert
        Assert.assertNotNull("Result map must not be null", result);
        Assert.assertEquals("Result map must contain exactly one entry", 1, result.size());
        Assert.assertTrue("Result map must contain the expected key", result.containsKey(MONTH_DAY));
        Assert.assertEquals("Value must be the MonthDay string in '--MM-dd' format",
                monthDay.toString(), result.get(MONTH_DAY));
    }

    /**
     * The implementation casts the {@code from} argument to {@link MonthDay} and then
     * invokes {@code toString()} on it. Supplying {@code null} therefore triggers a
     * {@link NullPointerException} at the point of method invocation.
     */
    @Test(expected = NullPointerException.class)
    public void testToMap_NullMonthDay_ThrowsNullPointerException() {
        // Arrange
        Converter converter = createConverter();

        // Act – should throw NPE
        MonthDayConversions.toMap(null, converter);
    }

    /**
     * Passing an object that is not a {@link MonthDay} should cause a {@link ClassCastException}
     * because the method performs an unchecked cast.
     */
    @Test(expected = ClassCastException.class)
    public void testToMap_IncompatibleSource_ThrowsClassCastException() {
        // Arrange
        Object notAMonthDay = "2021-01-01";   // a plain String
        Converter converter = createConverter();

        // Act – should throw CCE
        MonthDayConversions.toMap(notAMonthDay, converter);
    }

    /**
     * The {@code Converter} argument is not used by the implementation.
     * Supplying {@code null} must therefore not affect the successful conversion
     * of a valid {@link MonthDay}.
     */
    @Test
    public void testToMap_NullConverter_Succeeds() {
        // Arrange
        MonthDay monthDay = MonthDay.of(2, 29); // Leap day (valid for any year)
        Converter nullConverter = null;

        // Act
        Map<String, Object> result = MonthDayConversions.toMap(monthDay, nullConverter);

        // Assert – same expectations as the normal case
        Assert.assertNotNull("Result map must not be null", result);
        Assert.assertEquals(1, result.size());
        Assert.assertTrue(result.containsKey(MONTH_DAY));
        Assert.assertEquals(monthDay.toString(), result.get(MONTH_DAY));
    }

    /**
     * Additional sanity check: the returned map must be mutable (the contract does not
     * forbid modification). This test confirms that a new entry can be added without
     * throwing an exception.
     */
    @Test
    public void testToMap_ReturnedMapIsMutable() {
        // Arrange
        MonthDay monthDay = MonthDay.of(7, 4); // Independence Day
        Converter converter = createConverter();

        // Act
        Map<String, Object> result = MonthDayConversions.toMap(monthDay, converter);
        result.put("extraKey", "extraValue");

        // Assert
        Assert.assertEquals("Map should now contain two entries", 2, result.size());
        Assert.assertEquals("extraValue", result.get("extraKey"));
    }
}
