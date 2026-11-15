package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.TimeZone;

import static com.cedarsoftware.util.convert.MapConversions.ZONE;
import static org.junit.Assert.*;

public class TimeZoneConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // ConverterOptions is not needed for the methods under test; passing null is sufficient.
        converter = new Converter(null);
    }

    /* -------------------- basic happy‑path tests -------------------- */

    @Test
    public void testToString_happyPath() {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        String result = TimeZoneConversions.toString(tz, converter);
        assertEquals("UTC", result);
    }

    @Test
    public void testToZoneId_happyPath() {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        ZoneId result = TimeZoneConversions.toZoneId(tz, converter);
        assertEquals(ZoneId.of("UTC"), result);
    }

    @Test
    public void testToMap_happyPath() {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        Map<String, Object> map = TimeZoneConversions.toMap(tz, converter);
        assertNotNull(map);
        assertEquals(1, map.size());
        assertEquals("UTC", map.get(ZONE));
    }

    @Test
    public void testToZoneOffset_happyPath() {
        TimeZone tz = TimeZone.getTimeZone("Asia/Kolkata"); // UTC+05:30
        ZoneOffset expected = ZoneOffset.ofHoursMinutes(5, 30);
        ZoneOffset result = TimeZoneConversions.toZoneOffset(tz, converter);
        assertEquals(expected, result);
    }

    /* -------------------- edge‑case / exception tests -------------------- */

    // ----- null arguments -----
    @Test(expected = NullPointerException.class)
    public void testToString_null() {
        TimeZoneConversions.toString(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToZoneId_null() {
        TimeZoneConversions.toZoneId(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_null() {
        TimeZoneConversions.toMap(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToZoneOffset_null() {
        TimeZoneConversions.toZoneOffset(null, converter);
    }

    // ----- wrong object type -----
    @Test(expected = ClassCastException.class)
    public void testToString_wrongType() {
        TimeZoneConversions.toString("not a TimeZone", converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToZoneId_wrongType() {
        TimeZoneConversions.toZoneId(12345, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToMap_wrongType() {
        TimeZoneConversions.toMap(new Object(), converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToZoneOffset_wrongType() {
        TimeZoneConversions.toZoneOffset(Boolean.TRUE, converter);
    }

    // ----- negative and zero offsets -----
    @Test
    public void testToZoneOffset_negativeOffset() {
        TimeZone tz = TimeZone.getTimeZone("GMT-04:00"); // -4 hours
        ZoneOffset expected = ZoneOffset.ofHours(-4);
        ZoneOffset result = TimeZoneConversions.toZoneOffset(tz, converter);
        assertEquals(expected, result);
    }

    @Test
    public void testToZoneOffset_zeroOffset() {
        TimeZone tz = TimeZone.getTimeZone("GMT"); // UTC
        ZoneOffset result = TimeZoneConversions.toZoneOffset(tz, converter);
        assertEquals(ZoneOffset.UTC, result);
    }
}
