package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.util.Map;

/**
 * JUnit‑4 test suite for {@code OffsetTimeConversions}.
 *
 * The suite is organised as if three independent experts had contributed
 * one test per public method, then discussed edge‑cases and merged their
 * work into a single file.
 *
 * The class under test contains two static methods:
 *
 * <ul>
 *   <li>{@code static String toString(Object from, Converter converter)}</li>
 *   <li>{@code static Map<String,Object> toMap(Object from, Converter converter)}</li>
 * </ul>
 *
 * Both methods expect {@code from} to be an {@link OffsetTime} instance.
 * The {@code converter} argument is not used by the implementation, therefore
 * {@code null} is a perfectly valid value for the tests.
 *
 * Edge‑cases that are exercised:
 * <ul>
 *   <li>Typical values with positive, negative and zero offsets</li>
 *   <li>Maximum‑precision nanosecond values</li>
 *   <li>Null {@code from} argument → {@link NullPointerException}</li>
 *   <li>Wrong type for {@code from} → {@link ClassCastException}</li>
 * </ul>
 */
public class OffsetTimeConversionsTest
{
    // -----------------------------------------------------------------
    // Helper objects – shared by all test methods
    // -----------------------------------------------------------------
    private Converter dummyConverter;   // not used by the conversion methods

    @Before
    public void setUp()
    {
        // The Converter constructor requires a ConverterOptions instance.
        // The concrete type of ConverterOptions is not part of the public API
        // we are allowed to use, but we can safely pass null because the
        // conversion methods never touch the Converter argument.
        dummyConverter = new Converter(null);
    }

    // -----------------------------------------------------------------
    // 1. Tests for OffsetTimeConversions.toString(..)
    // -----------------------------------------------------------------

    /** Basic happy‑path test – positive offset */
    @Test
    public void testToString_PositiveOffset()
    {
        OffsetTime ot = OffsetTime.of(12, 34, 56, 0, ZoneOffset.ofHours(2));
        String result = OffsetTimeConversions.toString(ot, dummyConverter);
        assertEquals("12:34:56+02:00", result);
    }

    /** Happy‑path test – negative offset */
    @Test
    public void testToString_NegativeOffset()
    {
        OffsetTime ot = OffsetTime.of(23, 59, 59, 0, ZoneOffset.ofHours(-5));
        String result = OffsetTimeConversions.toString(ot, dummyConverter);
        assertEquals("23:59:59-05:00", result);
    }

    /** Zero offset (the “Z” designator) */
    @Test
    public void testToString_ZeroOffset()
    {
        OffsetTime ot = OffsetTime.of(08, 15, 30, 0, ZoneOffset.UTC);
        String result = OffsetTimeConversions.toString(ot, dummyConverter);
        assertEquals("08:15:30Z", result);
    }

    /** Full‑nanosecond precision is preserved */
    @Test
    public void testToString_MaxNanosecondPrecision()
    {
        OffsetTime ot = OffsetTime.of(01, 02, 03, 999_999_999, ZoneOffset.ofHoursMinutes(1, 30));
        String result = OffsetTimeConversions.toString(ot, dummyConverter);
        assertEquals("01:02:03.999999999+01:30", result);
    }

    /** Null source object – should throw NullPointerException */
    @Test(expected = NullPointerException.class)
    public void testToString_NullSource()
    {
        OffsetTimeConversions.toString(null, dummyConverter);
    }

    /** Wrong source type – should throw ClassCastException */
    @Test(expected = ClassCastException.class)
    public void testToString_WrongSourceType()
    {
        OffsetTimeConversions.toString("not an OffsetTime", dummyConverter);
    }

    // -----------------------------------------------------------------
    // 2. Tests for OffsetTimeConversions.toMap(..)
    // -----------------------------------------------------------------

    /** Basic happy‑path – map contains the correct key/value pair */
    @Test
    public void testToMap_Basic()
    {
        OffsetTime ot = OffsetTime.of(10, 20, 30, 0, ZoneOffset.ofHours(3));
        Map<String, Object> map = OffsetTimeConversions.toMap(ot, dummyConverter);

        assertNotNull("Returned map must not be null", map);
        assertEquals("Map should contain exactly one entry", 1, map.size());

        // The key used by the implementation is MapConversions.TIME.
        // The constant is defined elsewhere as the literal "time".
        // To keep the test independent of that class we use the literal.
        assertTrue("Map must contain the expected key", map.containsKey("time"));
        assertEquals("Value must be the OffsetTime's default string representation",
                     ot.toString(),
                     map.get("time"));
    }

    /** Zero offset – verifies that the “Z” designator is used */
    @Test
    public void testToMap_ZeroOffset()
    {
        OffsetTime ot = OffsetTime.of(00, 00, 00, 0, ZoneOffset.UTC);
        Map<String, Object> map = OffsetTimeConversions.toMap(ot, dummyConverter);
        assertEquals("00:00Z", map.get("time"));
    }

    /** Full‑nanosecond precision – map stores the full string */
    @Test
    public void testToMap_MaxNanosecondPrecision()
    {
        OffsetTime ot = OffsetTime.of(23, 59, 59, 123_456_789, ZoneOffset.ofHoursMinutes(-2, -15));
        Map<String, Object> map = OffsetTimeConversions.toMap(ot, dummyConverter);
        assertEquals("23:59:59.123456789-02:15", map.get("time"));
    }

    /** Null source object – should throw NullPointerException */
    @Test(expected = NullPointerException.class)
    public void testToMap_NullSource()
    {
        OffsetTimeConversions.toMap(null, dummyConverter);
    }

    /** Wrong source type – should throw ClassCastException */
    @Test(expected = ClassCastException.class)
    public void testToMap_WrongSourceType()
    {
        OffsetTimeConversions.toMap(42, dummyConverter);
    }

    // -----------------------------------------------------------------
    // 3. Miscellaneous sanity checks
    // -----------------------------------------------------------------

    /** The returned map is an instance of CompactLinkedMap (implementation detail) */
    @Test
    public void testToMap_ReturnsCompactLinkedMap()
    {
        OffsetTime ot = OffsetTime.now();
        Map<String, Object> map = OffsetTimeConversions.toMap(ot, dummyConverter);
        assertTrue("Implementation should return a CompactLinkedMap",
                   map instanceof CompactLinkedMap);
    }
}
