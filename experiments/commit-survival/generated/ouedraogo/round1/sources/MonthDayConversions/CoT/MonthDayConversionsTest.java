package com.cedarsoftware.util.convert;
import com.cedarsoftware.util.CompactLinkedMap;


import java.time.MonthDay;
import java.util.Map;

import org.junit.Test;
import static org.junit.Assert.*;

public class MonthDayConversionsTest {

    /** Basic happy‑path test */
    @Test
    public void basicToMap_convertsMonthDayToMap() {
        MonthDay md = MonthDay.of(5, 12);               // 5 December
        Converter converter = new Converter(null);     // converter is unused
        Map result = MonthDayConversions.toMap(md, converter);

        assertTrue("Result should be a CompactLinkedMap", result instanceof CompactLinkedMap);
        assertEquals("Map should contain exactly two entries", 2, result.size());
        assertEquals("Day value incorrect", 5, result.get("day"));
        assertEquals("Month value incorrect", 12, result.get("month"));
    }

    /** Null source object – expect NullPointerException */
    @Test(expected = NullPointerException.class)
    public void toMap_nullFrom_throwsNPE() {
        Converter converter = new Converter(null);
        MonthDayConversions.toMap(null, converter);
    }

    /** Source object of wrong type – expect ClassCastException */
    @Test(expected = ClassCastException.class)
    public void toMap_wrongType_throwsClassCastException() {
        Converter converter = new Converter(null);
        MonthDayConversions.toMap("not a MonthDay", converter);
    }

    /** Converter argument can be null – method should still succeed */
    @Test
    public void toMap_nullConverter_worksNormally() {
        MonthDay md = MonthDay.of(1, 1);
        Map result = MonthDayConversions.toMap(md, null);
        assertEquals(1, result.get("day"));
        assertEquals(1, result.get("month"));
    }

    /** Verify handling of a valid leap‑day MonthDay */
    @Test
    public void toMap_leapDay_isHandledCorrectly() {
        MonthDay md = MonthDay.of(29, 2);   // Feb 29
        Converter converter = new Converter(null);
        Map result = MonthDayConversions.toMap(md, converter);
        assertEquals(29, result.get("day"));
        assertEquals(2, result.get("month"));
    }
}
