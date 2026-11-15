package com.cedarsoftware.util.convert;
import com.cedarsoftware.util.CompactLinkedMap;


import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.time.YearMonth;
import java.util.Map;

import static org.junit.Assert.*;

public class YearMonthConversionsTest {

    // -----------------------------------------------------------------
    //  Fixtures
    // -----------------------------------------------------------------
    private YearMonth sampleYearMonth;

    @Before
    public void setUp() {
        // a deterministic sample value
        sampleYearMonth = YearMonth.of(2023, 5); // May 2023
    }

    // -----------------------------------------------------------------
    //  Happy‑path test – normal YearMonth conversion
    // -----------------------------------------------------------------
    @Test
    public void testToMapReturnsCorrectContents() {
        // Act
        Map<String, Object> result = YearMonthConversions.toMap(sampleYearMonth, null);

        // Assert type – implementation returns a CompactLinkedMap
        assertTrue("Result should be a CompactLinkedMap",
                result instanceof CompactLinkedMap);

        // Assert expected keys
        assertTrue("Result must contain key \"year\"", result.containsKey("year"));
        assertTrue("Result must contain key \"month\"", result.containsKey("month"));

        // Assert expected values
        assertEquals("Year value is wrong",
                Integer.valueOf(2023), result.get("year"));
        assertEquals("Month value is wrong",
                Integer.valueOf(5), result.get("month"));
    }

    // -----------------------------------------------------------------
    //  Null‑source test – expecting NullPointerException
    // -----------------------------------------------------------------
    @Rule
    public ExpectedException npeRule = ExpectedException.none();

    @Test
    public void testToMapWithNullSourceThrowsNPE() {
        npeRule.expect(NullPointerException.class);
        // The cast inside toMap will trigger NPE before any other code runs
        YearMonthConversions.toMap(null, null);
    }

    // -----------------------------------------------------------------
    //  Wrong‑type test – expecting ClassCastException
    // -----------------------------------------------------------------
    @Test
    public void testToMapWithInvalidSourceThrowsClassCastException() {
        Object notAYearMonth = "2023-05";
        try {
            YearMonthConversions.toMap(notAYearMonth, null);
            fail("Expected ClassCastException because the source is not a YearMonth");
        } catch (ClassCastException expected) {
            // success – the exception message is not important for this test
        }
    }
}
