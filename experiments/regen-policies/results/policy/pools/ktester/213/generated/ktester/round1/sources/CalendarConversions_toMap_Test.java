package com.cedarsoftware.util.convert;

import java.util.Calendar;
import java.util.Map;
import java.util.TimeZone;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.time.Instant;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CalendarConversions_toMap_Test {

    private Converter converter;

    /**
     * Some time‑zones used for deterministic calendar creation
     */
    private static final TimeZone TOKYO = TimeZone.getTimeZone("Asia/Tokyo");

    private static final TimeZone PARIS = TimeZone.getTimeZone("Europe/Paris");

    private static final TimeZone NEW_YORK = TimeZone.getTimeZone("America/New_York");

    @BeforeEach
    void setupBeforeEach() {
        // Use the default options – this mirrors the usage in the existing test suite
        converter = new Converter(new DefaultConverterOptions());
    }

    /**
     * Helper to build a Calendar with the given fields (month is 1‑based for callers).
     */
    private Calendar createCalendar(int year, int month, int day, int hour, int minute, int second, int millis, TimeZone tz) {
        Calendar cal = Calendar.getInstance(tz);
        cal.clear();
        // Calendar months are 0‑based
        cal.set(year, month - 1, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, millis);
        return cal;
    }

    @Test
    void testToMapReturnsCorrectEntry() {
        Calendar cal = createCalendar(2023, 6, 15, 15, 30, 0, 500, TOKYO);
        // Expected value is whatever CalendarConversions.toString produces for this calendar
        String expectedString = CalendarConversions.toString(cal, converter);
        Map<String, Object> result = CalendarConversions.toMap(cal, converter);
        // The map should contain exactly one entry
        assertEquals(1, result.size(), "Map should contain exactly one entry");
        // Verify the key is the constant defined in MapConversions
        assertTrue(result.containsKey(MapConversions.CALENDAR), "Map should contain the key defined by MapConversions.CALENDAR");
        // Verify the value matches the string representation
        assertEquals(expectedString, result.get(MapConversions.CALENDAR), "Map value should be the string representation of the calendar");
    }

    @Test
    void testToMapWithNullSource() {
        // According to Converter contract, converting a null source to a String yields null
        String expectedString = CalendarConversions.toString(null, converter);
        Map<String, Object> result = CalendarConversions.toMap(null, converter);
        assertEquals(1, result.size(), "Map should contain exactly one entry even for null source");
        assertTrue(result.containsKey(MapConversions.CALENDAR), "Map should contain the key defined by MapConversions.CALENDAR");
        assertEquals(expectedString, result.get(MapConversions.CALENDAR), "Map value for null source should match CalendarConversions.toString(null, converter)");
    }

    @Test
    void testToMapBothArgumentsNull_throwsNPE() {
        assertThrows(NullPointerException.class, () -> CalendarConversions.toMap(null, null), "Both arguments null should cause NullPointerException");
    }

    @Test
    void testToMapNullSourceWithMockConverter() {
        // Mock converter that returns a fixed placeholder string for any input
        Converter mock = mock(Converter.class);
        when(mock.convert(isNull(), eq(String.class))).thenReturn("null-value");
        Map<String, Object> result = CalendarConversions.toMap(null, mock);
        assertEquals(1, result.size());
        assertEquals("null-value", result.get(MapConversions.CALENDAR));
    }

    @Test
    void testToMapCalendarWithIsoStringConverter() {
        Calendar cal = new GregorianCalendar(2023, Calendar.JANUARY, 1, 12, 30, 45);
        cal.set(Calendar.MILLISECOND, 0);
        Converter mock = mock(Converter.class);
        // Simulate ISO_LOCAL_DATE_TIME formatting
        when(mock.convert(eq(cal), eq(String.class))).thenReturn("2023-01-01T12:30:45");
        Map<String, Object> result = CalendarConversions.toMap(cal, mock);
        assertEquals("2023-01-01T12:30:45", result.get(MapConversions.CALENDAR));
    }

    @Test
    void testToMapIntegerWithToStringConverter() {
        Integer src = Integer.valueOf(42);
        Converter mock = mock(Converter.class);
        when(mock.convert(eq(src), eq(String.class))).thenReturn("42");
        Map<String, Object> result = CalendarConversions.toMap(src, mock);
        assertEquals("42", result.get(MapConversions.CALENDAR));
    }

    @Test
    void testToMapStringIdentityConverter() {
        String src = "some text";
        Converter mock = mock(Converter.class);
        when(mock.convert(eq(src), eq(String.class))).thenReturn(src);
        Map<String, Object> result = CalendarConversions.toMap(src, mock);
        assertEquals("some text", result.get(MapConversions.CALENDAR));
    }

    @Test
    void testToMapValidSourceConverterReturnsNull() {
        Date src = new Date(0L);
        Converter mock = mock(Converter.class);
        when(mock.convert(eq(src), eq(String.class))).thenReturn(null);
        Map<String, Object> result = CalendarConversions.toMap(src, mock);
        assertTrue(result.containsKey(MapConversions.CALENDAR));
        assertNull(result.get(MapConversions.CALENDAR));
    }

    @Test
    void testToMapNonNullSourceNullConverter_throwsNPE() {
        Instant src = Instant.now();
        assertThrows(NullPointerException.class, () -> CalendarConversions.toMap(src, null), "Null converter should cause NullPointerException");
    }

    @Test
    void testToMapConverterThrowsIllegalArgumentException() {
        Date src = new Date();
        Converter mock = mock(Converter.class);
        when(mock.convert(eq(src), eq(String.class))).thenThrow(new IllegalArgumentException("bad"));
        assertThrows(IllegalArgumentException.class, () -> CalendarConversions.toMap(src, mock));
    }

    @Test
    void testToMapConverterThrowsNullPointerException() {
        String src = "test";
        Converter mock = mock(Converter.class);
        when(mock.convert(eq(src), eq(String.class))).thenThrow(new NullPointerException("null"));
        assertThrows(NullPointerException.class, () -> CalendarConversions.toMap(src, mock));
    }

    @Test
    void testReturnedMapIsLinkedHashMapAndHasSingleEntry() {
        String src = "immutable";
        Converter mock = mock(Converter.class);
        when(mock.convert(eq(src), eq(String.class))).thenReturn("immutable");
        Map<String, Object> result = CalendarConversions.toMap(src, mock);
        assertTrue(result instanceof LinkedHashMap, "Result map should be a LinkedHashMap");
        assertEquals(1, result.size());
        assertTrue(result.containsKey(MapConversions.CALENDAR));
    }

    @Test
    void testMapIndependenceBetweenCalls() {
        String src = "first";
        Converter mock = mock(Converter.class);
        when(mock.convert(eq(src), eq(String.class))).thenReturn("first");
        Map<String, Object> first = CalendarConversions.toMap(src, mock);
        // mutate the first map
        first.put("extra", "value");
        Map<String, Object> second = CalendarConversions.toMap(src, mock);
        assertFalse(second.containsKey("extra"), "Second map should not contain modifications from the first");
        assertEquals(1, second.size());
    }

    @Test
    void testEntryOrderIsInsertionOrder() {
        String src = "order-test";
        Converter mock = mock(Converter.class);
        when(mock.convert(eq(src), eq(String.class))).thenReturn("order-test");
        Map<String, Object> result = CalendarConversions.toMap(src, mock);
        // Since there is only one entry, we just verify that iteration yields that entry first.
        var iterator = result.entrySet().iterator();
        assertTrue(iterator.hasNext());
        var entry = iterator.next();
        assertEquals(MapConversions.CALENDAR, entry.getKey());
        assertEquals("order-test", entry.getValue());
    }

    @Test
    void testConverterReturnsNonString_causesClassCastException() {
        Integer src = 12345;
        Converter mock = mock(Converter.class);
        // Return an Integer instead of a String
        when(mock.convert(eq(src), eq(String.class))).thenReturn((String) (Object) Integer.valueOf(999));
        assertThrows(ClassCastException.class, () -> CalendarConversions.toMap(src, mock), "Returning a non‑String from the converter should cause ClassCastException");
    }

    @Test
    void testUnsupportedSourceTypeThrowsIllegalArgumentException() {
        Object src = new Object();
        Converter mock = mock(Converter.class);
        // The mock pretends to convert any object to its toString representation
        when(mock.convert(eq(src), eq(String.class))).thenReturn(src.toString());
        // CalendarConversions.toString validates the source type and should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> CalendarConversions.toMap(src, mock));
    }

    @Test
    void testNullMapConversionsConstantCausesNPE() throws Exception {
        // Save original constant value
        Field field = MapConversions.class.getDeclaredField("CALENDAR");
        field.setAccessible(true);
        Object original = field.get(null);
        try {
            // Set the constant to null
            field.set(null, null);
            // Any call to toMap now triggers target.put(null, …)
            assertThrows(NullPointerException.class, () -> CalendarConversions.toMap("any value", converter));
        } finally {
            // Restore original constant to avoid side‑effects on other tests
            field.set(null, original);
        }
    }
}
