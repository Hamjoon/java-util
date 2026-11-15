package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.TimeZone;

import static com.cedarsoftware.util.convert.MapConversions.ZONE_OFFSET;
import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link ZoneOffsetConversions}.
 *
 * The suite is written from the perspective of three independent experts,
 * each contributing a single test case for each public static method.
 * After the collaborative discussion the final set of tests covers:
 *
 * <ul>
 *   <li>Typical usage (positive, negative, and UTC offsets)</li>
 *   <li>Edge cases (null arguments, unusual ISO‑8601 representations)</li>
 *   <li>Error scenarios (expected {@link NullPointerException})</li>
 * </ul>
 *
 * The {@code Converter} argument is not used by the implementation, therefore
 * {@code null} is passed where a {@code Converter} instance would otherwise be
 * required.
 */
public class ZoneOffsetConversionsTest {

    // ----------------------------------------------------------------------
    //  Shared fixtures
    // ----------------------------------------------------------------------
    private Converter dummyConverter; // not used by the methods under test

    @Before
    public void setUp() {
        // The Converter constructor requires a ConverterOptions instance.
        // The options are irrelevant for the static methods, so we can safely pass null.
        dummyConverter = null;
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    // ----------------------------------------------------------------------
    //  Tests for ZoneOffsetConversions.toMap(...)
    // ----------------------------------------------------------------------

    /**
     * Expert A – typical case: a positive offset (+02:00) is converted to a map.
     */
    @Test
    public void expertA_toMap_positiveOffset() {
        ZoneOffset offset = ZoneOffset.ofHoursMinutes(2, 0);
        Map<String, Object> result = ZoneOffsetConversions.toMap(offset, dummyConverter);

        assertNotNull("Result map must not be null", result);
        assertEquals("Map should contain exactly one entry", 1, result.size());
        assertTrue("Map must contain the key defined by ZONE_OFFSET", result.containsKey(ZONE_OFFSET));
        assertEquals("The stored value must be the ISO‑8601 identifier", "+02:00", result.get(ZONE_OFFSET));
    }

    /**
     * Expert B – edge case: UTC is represented by the constant {@code Z}.
     */
    @Test
    public void expertB_toMap_utcOffset() {
        ZoneOffset utc = ZoneOffset.UTC;
        Map<String, Object> result = ZoneOffsetConversions.toMap(utc, dummyConverter);

        assertEquals("UTC offset must be represented by 'Z'", "Z", result.get(ZONE_OFFSET));
    }

    /**
     * Expert C – error scenario: passing {@code null} must throw NPE.
     */
    @Test
    public void expertC_toMap_nullArgument() {
        thrown.expect(NullPointerException.class);
        ZoneOffsetConversions.toMap(null, dummyConverter);
    }

    // ----------------------------------------------------------------------
    //  Tests for ZoneOffsetConversions.toZoneId(...)
    // ----------------------------------------------------------------------

    /**
     * Expert A – typical case: a ZoneOffset is returned unchanged as a ZoneId.
     */
    @Test
    public void expertA_toZoneId_returnsSameInstance() {
        ZoneOffset offset = ZoneOffset.ofHours(-5);
        ZoneId result = ZoneOffsetConversions.toZoneId(offset, dummyConverter);
        assertSame("The returned ZoneId must be the same instance supplied", offset, result);
    }

    /**
     * Expert B – edge case: a ZoneId that is not a ZoneOffset (e.g., "America/New_York").
     */
    @Test
    public void expertB_toZoneId_nonOffsetZoneId() {
        ZoneId zoneId = ZoneId.of("America/New_York");
        ZoneId result = ZoneOffsetConversions.toZoneId(zoneId, dummyConverter);
        assertSame("When a non‑offset ZoneId is supplied it must be returned unchanged", zoneId, result);
    }

    /**
     * Expert C – error scenario: null argument.
     */
    @Test
    public void expertC_toZoneId_nullArgument() {
        thrown.expect(NullPointerException.class);
        ZoneOffsetConversions.toZoneId(null, dummyConverter);
    }

    // ----------------------------------------------------------------------
    //  Tests for ZoneOffsetConversions.toTimeZone(...)
    // ----------------------------------------------------------------------

    /**
     * Expert A – typical case: positive offset creates a GMT+HH:MM TimeZone.
     */
    @Test
    public void expertA_toTimeZone_positiveOffset() {
        ZoneOffset offset = ZoneOffset.ofHoursMinutesSeconds(3, 30, 0); // "+03:30"
        TimeZone tz = ZoneOffsetConversions.toTimeZone(offset, dummyConverter);
        assertEquals("GMT+03:30", tz.getID());
    }

    /**
     * Expert B – edge case: negative offset with seconds component.
     */
    @Test
    public void expertB_toTimeZone_negativeOffsetWithSeconds() {
        ZoneOffset offset = ZoneOffset.ofHoursMinutesSeconds(-4, -15, -30); // "-04:15:30"
        TimeZone tz = ZoneOffsetConversions.toTimeZone(offset, dummyConverter);
        // TimeZone drops the seconds part, keeping the hour/minute representation.
        assertEquals("GMT-04:15:30", tz.getID()); // Java's TimeZone keeps the full string
    }

    /**
     * Expert C – special case: UTC should map to plain "GMT".
     */
    @Test
    public void expertC_toTimeZone_utc() {
        ZoneOffset utc = ZoneOffset.UTC;
        TimeZone tz = ZoneOffsetConversions.toTimeZone(utc, dummyConverter);
        assertEquals("UTC must be represented as plain 'GMT'", "GMT", tz.getID());
    }

    /**
     * Expert A – error scenario: null argument.
     */
    @Test
    public void expertA_toTimeZone_nullArgument() {
        thrown.expect(NullPointerException.class);
        ZoneOffsetConversions.toTimeZone(null, dummyConverter);
    }
}
