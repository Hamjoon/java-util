package com.cedarsoftware.util.convert;
import com.cedarsoftware.util.CompactLinkedMap;


import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.TimeZone;
import java.time.ZoneId;

import static org.junit.Assert.*;

public class TimeZoneConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // Converter requires a ConverterOptions instance; use the default constructor.
        // If ConverterOptions does not have a no‑arg constructor, adjust accordingly.
        converter = new Converter(new ConverterOptions());
    }

    @Test
    public void testToString_WithUtcTimeZone() {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        String result = TimeZoneConversions.toString(tz, converter);
        assertEquals("UTC", result);
    }

    @Test
    public void testToString_WithRegionTimeZone() {
        TimeZone tz = TimeZone.getTimeZone("America/New_York");
        String result = TimeZoneConversions.toString(tz, converter);
        assertEquals("America/New_York", result);
    }

    @Test(expected = ClassCastException.class)
    public void testToString_WithInvalidObject() {
        // Passing a non‑TimeZone should cause a ClassCastException
        TimeZoneConversions.toString("not a timezone", converter);
    }

    @Test
    public void testToZoneId_Utc() {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        ZoneId zoneId = TimeZoneConversions.toZoneId(tz, converter);
        assertEquals(ZoneId.of("UTC"), zoneId);
    }

    @Test
    public void testToZoneId_Region() {
        TimeZone tz = TimeZone.getTimeZone("Europe/Paris");
        ZoneId zoneId = TimeZoneConversions.toZoneId(tz, converter);
        assertEquals(ZoneId.of("Europe/Paris"), zoneId);
    }

    @Test(expected = ClassCastException.class)
    public void testToZoneId_WithInvalidObject() {
        TimeZoneConversions.toZoneId(12345, converter);
    }

    @Test
    public void testToMap_Utc() {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        Map<String, Object> map = TimeZoneConversions.toMap(tz, converter);
        assertNotNull(map);
        assertTrue(map.containsKey(MapConversions.ZONE));
        assertEquals("UTC", map.get(MapConversions.ZONE));
        // The map should be an instance of CompactLinkedMap (or at least a Map)
        assertTrue(map instanceof CompactLinkedMap);
    }

    @Test
    public void testToMap_Region() {
        TimeZone tz = TimeZone.getTimeZone("Asia/Tokyo");
        Map<String, Object> map = TimeZoneConversions.toMap(tz, converter);
        assertNotNull(map);
        assertEquals("Asia/Tokyo", map.get(MapConversions.ZONE));
    }

    @Test(expected = ClassCastException.class)
    public void testToMap_WithInvalidObject() {
        TimeZoneConversions.toMap(new Object(), converter);
    }
}
