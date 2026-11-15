package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

import java.time.ZoneId;
import java.util.Map;
import java.util.TimeZone;

/**
 * Tests for {@link ZoneIdConversions}.
 * The test class resides in the same package so it can access the package‑private
 * static methods of {@code ZoneIdConversions}.
 */
public class ZoneIdConversionsTest {

    /* -------------------------------------------------------------
     *  Basic functionality tests
     * ----------------------------------------------------------- */

    @Test
    public void testToMap_basic() {
        ZoneId zoneId = ZoneId.of("America/New_York");
        Map<String, Object> result = ZoneIdConversions.toMap(zoneId, null);
        assertNotNull("Result map must not be null", result);
        assertTrue("Result must contain key \"zone\"", result.containsKey("zone"));
        assertEquals("Zone string must match ZoneId.toString()", zoneId.toString(),
                     result.get("zone"));
    }

    @Test
    public void testToTimeZone_basic() {
        ZoneId zoneId = ZoneId.of("Europe/Paris");
        TimeZone tz = ZoneIdConversions.toTimeZone(zoneId, null);
        assertNotNull("TimeZone must not be null", tz);
        assertEquals("TimeZone ID must match ZoneId ID", zoneId.getId(), tz.getID());
    }

    /* -------------------------------------------------------------
     *  Tests for unusual but valid ZoneIds
     * ----------------------------------------------------------- */

    @Test
    public void testToMap_unusualZoneIds() {
        String[] ids = {"UTC", "GMT+05:30", "GMT-02:00", "Etc/GMT+12"};
        for (String id : ids) {
            ZoneId zid = ZoneId.of(id);
            Map<String, Object> map = ZoneIdConversions.toMap(zid, null);
            assertEquals("Map value must equal ZoneId string for id: " + id,
                         zid.toString(), map.get("zone"));
        }
    }

    @Test
    public void testToTimeZone_unusualZoneIds() {
        String[] ids = {"UTC", "GMT+05:30", "GMT-02:00", "Etc/GMT+12"};
        for (String id : ids) {
            ZoneId zid = ZoneId.of(id);
            TimeZone tz = ZoneIdConversions.toTimeZone(zid, null);
            assertEquals("TimeZone ID must equal ZoneId ID for id: " + id,
                         zid.getId(), tz.getID());
        }
    }

    /* -------------------------------------------------------------
     *  Null handling tests
     * ----------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testToMap_nullFrom() {
        ZoneIdConversions.toMap(null, null);
    }

    @Test(expected = NullPointerException.class)
    public void testToTimeZone_nullFrom() {
        ZoneIdConversions.toTimeZone(null, null);
    }

    /* -------------------------------------------------------------
     *  Wrong‑type handling tests
     * ----------------------------------------------------------- */

    @Test(expected = ClassCastException.class)
    public void testToMap_wrongType() {
        // Passing a String instead of a ZoneId triggers a ClassCastException
        ZoneIdConversions.toMap("not-a-zone-id", null);
    }

    @Test(expected = ClassCastException.class)
    public void testToTimeZone_wrongType() {
        // Passing an Integer instead of a ZoneId triggers a ClassCastException
        ZoneIdConversions.toTimeZone(Integer.valueOf(42), null);
    }

    /* -------------------------------------------------------------
     *  Converter argument is irrelevant – ensure null works
     * ----------------------------------------------------------- */

    @Test
    public void testToMap_nullConverter() {
        ZoneId zoneId = ZoneId.of("Asia/Tokyo");
        Map<String, Object> map = ZoneIdConversions.toMap(zoneId, null);
        assertEquals(zoneId.toString(), map.get("zone"));
    }

    @Test
    public void testToTimeZone_nullConverter() {
        ZoneId zoneId = ZoneId.of("Asia/Tokyo");
        TimeZone tz = ZoneIdConversions.toTimeZone(zoneId, null);
        assertEquals(zoneId.getId(), tz.getID());
    }
}
