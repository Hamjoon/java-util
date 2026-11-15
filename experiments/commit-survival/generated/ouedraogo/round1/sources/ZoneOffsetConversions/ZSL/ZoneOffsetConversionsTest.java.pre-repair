package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.CompactLinkedMap;
import org.junit.Test;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;

import static com.cedarsoftware.util.convert.MapConversions.HOURS;
import static com.cedarsoftware.util.convert.MapConversions.MINUTES;
import static com.cedarsoftware.util.convert.MapConversions.SECONDS;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link ZoneOffsetConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Conversion of {@link ZoneOffset} to a {@link Map} with correct hour/minute/second entries.</li>
 *   <li>Verification that the seconds entry is omitted when it is zero.</li>
 *   <li>Handling of positive, negative and zero offsets.</li>
 *   <li>Conversion of any {@link ZoneId} (including {@link ZoneOffset}) via {@code toZoneId}.</li>
 *   <li>Behaviour when {@code null} is supplied.</li>
 * </ul>
 *
 * The {@link Converter} argument is not used by the implementation, so a {@code null}
 * instance (or a real instance constructed with a {@code null} options object) is sufficient.
 */
public class ZoneOffsetConversionsTest {

    // -------------------------------------------------------------------------
    // Helper – creates a dummy Converter instance required by the API.
    // -------------------------------------------------------------------------
    private static Converter dummyConverter() {
        // The Converter constructor accepts a ConverterOptions argument.
        // Passing null is safe because the methods under test never invoke
        // any Converter functionality.
        return new Converter(null);
    }

    // -------------------------------------------------------------------------
    // Tests for ZoneOffsetConversions.toMap(...)
    // -------------------------------------------------------------------------

    @Test
    public void testToMap_WithZeroOffset_ShouldContainOnlyHoursAndMinutes() {
        ZoneOffset offset = ZoneOffset.UTC; // total seconds = 0
        Map<String, Object> result = ZoneOffsetConversions.toMap(offset, dummyConverter());

        // Verify map type
        assertTrue("Result should be a CompactLinkedMap", result instanceof CompactLinkedMap);

        // Expected values
        assertEquals("Hours should be 0", 0, result.get(HOURS));
        assertEquals("Minutes should be 0", 0, result.get(MINUTES));
        assertFalse("Seconds entry must be omitted when zero", result.containsKey(SECONDS));
    }

    @Test
    public void testToMap_WithPositiveWholeHourOffset() {
        ZoneOffset offset = ZoneOffset.ofHours(5); // +05:00
        Map<String, Object> result = ZoneOffsetConversions.toMap(offset, dummyConverter());

        assertEquals("Hours should be 5", 5, result.get(HOURS));
        assertEquals("Minutes should be 0", 0, result.get(MINUTES));
        assertFalse("Seconds entry must be omitted when zero", result.containsKey(SECONDS));
    }

    @Test
    public void testToMap_WithPositiveMixedOffset_ShouldIncludeSecondsWhenNonZero() {
        // +02:30:45  (2 hours, 30 minutes, 45 seconds)
        ZoneOffset offset = ZoneOffset.ofHoursMinutesSeconds(2, 30, 45);
        Map<String, Object> result = ZoneOffsetConversions.toMap(offset, dummyConverter());

        assertEquals("Hours should be 2", 2, result.get(HOURS));
        assertEquals("Minutes should be 30", 30, result.get(MINUTES));
        assertTrue("Seconds entry must be present when non‑zero", result.containsKey(SECONDS));
        assertEquals("Seconds should be 45", 45, result.get(SECONDS));
    }

    @Test
    public void testToMap_WithNegativeOffset_ShouldHandleSignCorrectly() {
        // -07:15:00  (negative 7 hours, 15 minutes)
        ZoneOffset offset = ZoneOffset.ofHoursMinutes(-7, -15);
        Map<String, Object> result = ZoneOffsetConversions.toMap(offset, dummyConverter());

        // totalSeconds = -(7*3600 + 15*60) = -26100
        // Division truncates toward zero for negative numbers in Java,
        // so hours = -7, minutes = -15, seconds = 0
        assertEquals("Hours should be -7", -7, result.get(HOURS));
        assertEquals("Minutes should be -15", -15, result.get(MINUTES));
        assertFalse("Seconds entry must be omitted when zero", result.containsKey(SECONDS));
    }

    @Test
    public void testToMap_WithNegativeOffsetHavingSeconds() {
        // -01:00:30  (negative 1 hour, 0 minutes, 30 seconds)
        ZoneOffset offset = ZoneOffset.ofHoursMinutesSeconds(-1, 0, -30);
        Map<String, Object> result = ZoneOffsetConversions.toMap(offset, dummyConverter());

        // totalSeconds = -(3600 + 30) = -3630
        // hours = -1, minutes = 0, seconds = -30 (modulo logic yields -30)
        assertEquals("Hours should be -1", -1, result.get(HOURS));
        assertEquals("Minutes should be 0", 0, result.get(MINUTES));
        assertTrue("Seconds entry must be present when non‑zero", result.containsKey(SECONDS));
        assertEquals("Seconds should be -30", -30, result.get(SECONDS));
    }

    @Test
    public void testToMap_WithMaximumPositiveOffset() {
        // Maximum ZoneOffset is +18:00
        ZoneOffset offset = ZoneOffset.ofHours(18);
        Map<String, Object> result = ZoneOffsetConversions.toMap(offset, dummyConverter());

        assertEquals("Hours should be 18", 18, result.get(HOURS));
        assertEquals("Minutes should be 0", 0, result.get(MINUTES));
        assertFalse("Seconds entry must be omitted when zero", result.containsKey(SECONDS));
    }

    @Test
    public void testToMap_WithMaximumNegativeOffset() {
        // Minimum ZoneOffset is -18:00
        ZoneOffset offset = ZoneOffset.ofHours(-18);
        Map<String, Object> result = ZoneOffsetConversions.toMap(offset, dummyConverter());

        assertEquals("Hours should be -18", -18, result.get(HOURS));
        assertEquals("Minutes should be 0", 0, result.get(MINUTES));
        assertFalse("Seconds entry must be omitted when zero", result.containsKey(SECONDS));
    }

    // -------------------------------------------------------------------------
    // Tests for ZoneOffsetConversions.toZoneId(...)
    // -------------------------------------------------------------------------

    @Test
    public void testToZoneId_WithZoneOffset_ReturnsSameInstance() {
        ZoneOffset offset = ZoneOffset.ofHoursMinutesSeconds(3, 45, 0);
        ZoneId result = ZoneOffsetConversions.toZoneId(offset, dummyConverter());

        assertSame("Result should be the exact same ZoneOffset instance", offset, result);
    }

    @Test
    public void testToZoneId_WithStandardZoneId_ReturnsSameInstance() {
        ZoneId zoneId = ZoneId.of("America/New_York");
        ZoneId result = ZoneOffsetConversions.toZoneId(zoneId, dummyConverter());

        assertSame("Result should be the exact same ZoneId instance", zoneId, result);
    }

    @Test
    public void testToZoneId_WithNull_ReturnsNull() {
        ZoneId result = ZoneOffsetConversions.toZoneId(null, dummyConverter());

        assertNull("Result should be null when input is null", result);
    }
}
