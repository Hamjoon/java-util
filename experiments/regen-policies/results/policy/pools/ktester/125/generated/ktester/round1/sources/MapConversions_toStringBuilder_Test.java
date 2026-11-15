package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import com.cedarsoftware.util.CompactMap;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.*;
import java.math.*;
import java.net.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MapConversions_toStringBuilder_Test {

    /**
     * Shared Converter instance used by all tests.
     */
    private static Converter converter;

    /**
     * Shared real converter for the majority of tests.
     */
    private static Converter realConverter;

    class ThrowingGetterBean {

        public String getBad() {
            throw new RuntimeException("getter failure");
        }
    }

    class BadToString {

        @Override
        public String toString() {
            throw new RuntimeException("bad toString");
        }
    }

    class ThrowingGetterBean {

        public String getBad() {
            throw new RuntimeException("getter failure");
        }
    }

    class BadToString {

        @Override
        public String toString() {
            throw new RuntimeException("bad toString");
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; use the default options.
        converter = new Converter(new ConverterOptions());
        realConverter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the shared Converter reference.
        converter = null;
        realConverter = null;
    }

    /**
     * Verify that a plain {@link String} is converted to a {@link StringBuilder}
     * containing the same characters.
     */
    @Test
    void toStringBuilder_WithString_ReturnsBuilderWithSameContent() {
        // given
        String source = "sample text";
        // when
        StringBuilder result = MapConversions.toStringBuilder(source, converter);
        // then
        assertNotNull(result, "Resulting StringBuilder should not be null");
        assertEquals(source, result.toString(), "StringBuilder should contain the original string");
    }

    /**
     * Verify that a {@link Map} containing a {@code "value"} entry is converted
     * to a {@link StringBuilder} whose content is the mapped value.
     */
    @Test
    void toStringBuilder_WithMapHavingValueKey_ReturnsBuilderWithMappedValue() {
        // given
        Map<String, Object> map = new CompactMap<>();
        map.put(MapConversions.VALUE, "mapped value");
        // when
        StringBuilder result = MapConversions.toStringBuilder(map, converter);
        // then
        assertNotNull(result, "Resulting StringBuilder should not be null");
        assertEquals("mapped value", result.toString(), "StringBuilder should contain the value from the map");
    }

    /**
     * Verify that a {@link Map} containing the special {@code "_v"} key
     * (used by the conversion utilities) is handled correctly.
     */
    @Test
    void toStringBuilder_WithMapHavingVKey_ReturnsBuilderWithVValue() {
        // given
        Map<String, Object> map = new CompactMap<>();
        map.put(MapConversions.V, "v‑key value");
        // when
        StringBuilder result = MapConversions.toStringBuilder(map, converter);
        // then
        assertNotNull(result, "Resulting StringBuilder should not be null");
        assertEquals("v‑key value", result.toString(), "StringBuilder should contain the value associated with the _v key");
    }

    @Test
    void toStringBuilder_WithNull_ReturnsLiteralNullString() {
        StringBuilder result = MapConversions.toStringBuilder(null, realConverter);
        assertNotNull(result);
        assertEquals("null", result.toString());
    }

    @Test
    void toStringBuilder_WithInteger_ReturnsBuilderWithNumber() {
        Integer source = 42;
        StringBuilder result = MapConversions.toStringBuilder(source, realConverter);
        assertEquals("42", result.toString());
    }

    @Test
    void toStringBuilder_WithBoolean_ReturnsBuilderWithBoolean() {
        Boolean source = Boolean.TRUE;
        StringBuilder result = MapConversions.toStringBuilder(source, realConverter);
        assertEquals("true", result.toString());
    }

    @Test
    void toStringBuilder_WithMap_ValueKey() {
        Map<String, Object> map = new CompactMap<>();
        map.put(MapConversions.VALUE, "mapped value");
        StringBuilder result = MapConversions.toStringBuilder(map, realConverter);
        assertEquals("mapped value", result.toString());
    }

    @Test
    void toStringBuilder_WithMap_VKey() {
        Map<String, Object> map = new CompactMap<>();
        map.put(MapConversions.V, "v‑key value");
        StringBuilder result = MapConversions.toStringBuilder(map, realConverter);
        assertEquals("v‑key value", result.toString());
    }

    @Test
    void toStringBuilder_WithList() {
        List<Integer> list = Arrays.asList(1, 2, 3);
        StringBuilder result = MapConversions.toStringBuilder(list, realConverter);
        assertEquals("[1, 2, 3]", result.toString());
    }

    @Test
    void toStringBuilder_WithEmptyMap() {
        Map<String, Object> empty = new CompactMap<>();
        StringBuilder result = MapConversions.toStringBuilder(empty, realConverter);
        assertEquals("{}", result.toString());
    }

    @Test
    void toStringBuilder_WithEmptyList() {
        List<Object> empty = Collections.emptyList();
        StringBuilder result = MapConversions.toStringBuilder(empty, realConverter);
        assertEquals("[]", result.toString());
    }

    @Test
    void toStringBuilder_WithLocalDate_ProducesMap() {
        LocalDate date = LocalDate.of(2023, 5, 17);
        StringBuilder sb = MapConversions.toStringBuilder(date, realConverter);
        // Expected map representation contains year, month and day keys
        assertTrue(sb.toString().contains("year=2023"));
        assertTrue(sb.toString().contains("month=5"));
        assertTrue(sb.toString().contains("day=17"));
    }

    @Test
    void toStringBuilder_WithLocalDateTime_ProducesMap() {
        LocalDateTime ldt = LocalDateTime.of(2023, 5, 17, 14, 30, 45, 123_456_789);
        StringBuilder sb = MapConversions.toStringBuilder(ldt, realConverter);
        assertTrue(sb.toString().contains("date=2023-05-17"));
        assertTrue(sb.toString().contains("time=14:30:45"));
        assertTrue(sb.toString().contains("nanos=123456789"));
    }

    @Test
    void toStringBuilder_WithInstant_ProducesEpochMillis() {
        Instant instant = Instant.ofEpochMilli(1_600_000_000_000L);
        StringBuilder sb = MapConversions.toStringBuilder(instant, realConverter);
        assertEquals("{epochMillis=1600000000000}", sb.toString());
    }

    @Test
    void toStringBuilder_WithLocalTime_ProducesMap() {
        LocalTime lt = LocalTime.of(14, 30, 15, 123_456_789);
        StringBuilder sb = MapConversions.toStringBuilder(lt, realConverter);
        assertTrue(sb.toString().contains("time=14:30:15"));
        assertTrue(sb.toString().contains("nanos=123456789"));
    }

    @Test
    void toStringBuilder_WithOffsetTime_ProducesMap() {
        OffsetTime ot = OffsetTime.of(9, 15, 0, 0, ZoneOffset.ofHoursMinutes(2, 30));
        StringBuilder sb = MapConversions.toStringBuilder(ot, realConverter);
        assertTrue(sb.toString().contains("time=09:15"));
        assertTrue(sb.toString().contains("offset=+02:30"));
        assertTrue(sb.toString().contains("offsetHour=2"));
        assertTrue(sb.toString().contains("offsetMinute=30"));
    }

    @Test
    void toStringBuilder_WithOffsetDateTime_ProducesMap() {
        OffsetDateTime odt = OffsetDateTime.of(2023, 5, 17, 14, 30, 45, 0, ZoneOffset.ofHours(-5));
        StringBuilder sb = MapConversions.toStringBuilder(odt, realConverter);
        assertTrue(sb.toString().contains("date=2023-05-17"));
        assertTrue(sb.toString().contains("time=14:30:45"));
        assertTrue(sb.toString().contains("offset=-05:00"));
        assertTrue(sb.toString().contains("offsetHour=-5"));
        assertTrue(sb.toString().contains("offsetMinute=0"));
    }

    @Test
    void toStringBuilder_WithZonedDateTime_ProducesMap() {
        ZonedDateTime zdt = ZonedDateTime.of(2023, 12, 31, 23, 59, 59, 0, ZoneId.of("America/New_York"));
        StringBuilder sb = MapConversions.toStringBuilder(zdt, realConverter);
        assertTrue(sb.toString().contains("date=2023-12-31"));
        assertTrue(sb.toString().contains("time=23:59:59"));
        assertTrue(sb.toString().contains("zone=America/New_York"));
    }

    @Test
    void toStringBuilder_WithYear_ProducesMap() {
        Year y = Year.of(1999);
        StringBuilder sb = MapConversions.toStringBuilder(y, realConverter);
        assertEquals("{year=1999}", sb.toString());
    }

    @Test
    void toStringBuilder_WithYearMonth_ProducesMap() {
        YearMonth ym = YearMonth.of(2022, 11);
        StringBuilder sb = MapConversions.toStringBuilder(ym, realConverter);
        assertTrue(sb.toString().contains("year=2022"));
        assertTrue(sb.toString().contains("month=11"));
    }

    @Test
    void toStringBuilder_WithMonthDay_ProducesMap() {
        MonthDay md = MonthDay.of(2, 29);
        StringBuilder sb = MapConversions.toStringBuilder(md, realConverter);
        assertTrue(sb.toString().contains("month=2"));
        assertTrue(sb.toString().contains("day=29"));
    }

    @Test
    void toStringBuilder_WithPeriod_ProducesMap() {
        Period p = Period.of(3, 2, 15);
        StringBuilder sb = MapConversions.toStringBuilder(p, realConverter);
        assertTrue(sb.toString().contains("years=3"));
        assertTrue(sb.toString().contains("months=2"));
        assertTrue(sb.toString().contains("days=15"));
    }

    @Test
    void toStringBuilder_WithDuration_ProducesMap() {
        Duration d = Duration.ofHours(5).plusMinutes(30).plusNanos(123_456_789);
        StringBuilder sb = MapConversions.toStringBuilder(d, realConverter);
        assertTrue(sb.toString().contains("hours=5"));
        assertTrue(sb.toString().contains("minutes=30"));
        assertTrue(sb.toString().contains("seconds=0"));
        assertTrue(sb.toString().contains("nanos=123456789"));
    }

    @Test
    void toStringBuilder_WithZoneId_ProducesString() {
        ZoneId zid = ZoneId.of("Europe/Paris");
        StringBuilder sb = MapConversions.toStringBuilder(zid, realConverter);
        assertEquals("Europe/Paris", sb.toString());
    }

    @Test
    void toStringBuilder_WithZoneOffset_ProducesMap() {
        ZoneOffset zo = ZoneOffset.ofHoursMinutes(5, 45);
        StringBuilder sb = MapConversions.toStringBuilder(zo, realConverter);
        assertTrue(sb.toString().contains("offset=+05:45"));
        assertTrue(sb.toString().contains("offsetHour=5"));
        assertTrue(sb.toString().contains("offsetMinute=45"));
    }

    @Test
    void toStringBuilder_WithTimeZone_ProducesString() {
        TimeZone tz = TimeZone.getTimeZone("GMT+02:00");
        StringBuilder sb = MapConversions.toStringBuilder(tz, realConverter);
        assertEquals("GMT+02:00", sb.toString());
    }

    @Test
    void toStringBuilder_WithCalendar_ProducesMap() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        // fixed instant
        cal.setTimeInMillis(1_600_000_000_000L);
        StringBuilder sb = MapConversions.toStringBuilder(cal, realConverter);
        // 1600000000000 ms -> 2020‑09‑13
        assertTrue(sb.toString().contains("date=2020-09-13"));
        assertTrue(sb.toString().contains("time=12:26:40"));
        assertTrue(sb.toString().contains("zone=UTC"));
    }

    @Test
    void toStringBuilder_WithTimestamp_ProducesMap() {
        Timestamp ts = new Timestamp(1_600_000_000_000L);
        StringBuilder sb = MapConversions.toStringBuilder(ts, realConverter);
        assertEquals("{epochMillis=1600000000000}", sb.toString());
    }

    @Test
    void toStringBuilder_WithDate_ProducesMap() {
        Date date = new Date(1_600_000_000_000L);
        StringBuilder sb = MapConversions.toStringBuilder(date, realConverter);
        assertEquals("{epochMillis=1600000000000}", sb.toString());
    }

    @Test
    void toStringBuilder_WithAtomicBoolean() {
        AtomicBoolean ab = new AtomicBoolean(true);
        StringBuilder sb = MapConversions.toStringBuilder(ab, realConverter);
        assertEquals("true", sb.toString());
    }

    @Test
    void toStringBuilder_WithAtomicInteger() {
        AtomicInteger ai = new AtomicInteger(123);
        StringBuilder sb = MapConversions.toStringBuilder(ai, realConverter);
        assertEquals("123", sb.toString());
    }

    @Test
    void toStringBuilder_WithAtomicLong() {
        AtomicLong al = new AtomicLong(9_876_543_210L);
        StringBuilder sb = MapConversions.toStringBuilder(al, realConverter);
        assertEquals("9876543210", sb.toString());
    }

    @Test
    void toStringBuilder_WithBigInteger() {
        BigInteger bi = new BigInteger("12345678901234567890");
        StringBuilder sb = MapConversions.toStringBuilder(bi, realConverter);
        assertEquals("12345678901234567890", sb.toString());
    }

    @Test
    void toStringBuilder_WithBigDecimal() {
        BigDecimal bd = new BigDecimal("123.45");
        StringBuilder sb = MapConversions.toStringBuilder(bd, realConverter);
        assertEquals("123.45", sb.toString());
    }

    @Test
    void toStringBuilder_WithUUID() {
        UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        StringBuilder sb = MapConversions.toStringBuilder(uuid, realConverter);
        assertEquals("123e4567-e89b-12d3-a456-426614174000", sb.toString());
    }

    @Test
    void toStringBuilder_WithURI() {
        URI uri = URI.create("https://example.com/path?query=1");
        StringBuilder sb = MapConversions.toStringBuilder(uri, realConverter);
        assertEquals("https://example.com/path?query=1", sb.toString());
    }

    @Test
    void toStringBuilder_WithURL() throws MalformedURLException {
        URL url = new URL("https://example.com");
        StringBuilder sb = MapConversions.toStringBuilder(url, realConverter);
        assertEquals("https://example.com", sb.toString());
    }

    @Test
    void toStringBuilder_WithCustomConverter_TransformsValue() {
        // Mock a Converter that returns a prefixed String for a BigDecimal
        Converter mock = mock(Converter.class);
        when(mock.convert(any(), eq(String.class))).thenAnswer(invocation -> {
            Object src = invocation.getArgument(0);
            return "bigDecimal:" + src.toString();
        });
        BigDecimal bd = new BigDecimal("123.45");
        StringBuilder sb = MapConversions.toStringBuilder(bd, mock);
        assertEquals("bigDecimal:123.45", sb.toString());
    }

    @Test
    void toStringBuilder_WithCustomConverter_ReturnsNull_TreatedAsLiteralNull() {
        Converter mock = mock(Converter.class);
        when(mock.convert(any(), eq(String.class))).thenReturn(null);
        StringBuilder sb = MapConversions.toStringBuilder("anyString", mock);
        assertEquals("null", sb.toString());
    }

    @Test
    void toStringBuilder_WithThrowingConverter_PropagatesException() {
        Converter mock = mock(Converter.class);
        when(mock.convert(any(), eq(String.class))).thenThrow(new RuntimeException("boom"));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> MapConversions.toStringBuilder("any", mock));
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void toStringBuilder_NullConverter_ThrowsNPE() {
        assertThrows(NullPointerException.class, () -> MapConversions.toStringBuilder("any", null));
    }

    @Test
    void toStringBuilder_UnsupportedSource_ThrowsIllegalArgumentException() {
        ByteArrayInputStream bis = new ByteArrayInputStream(new byte[] { 1, 2, 3 });
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toStringBuilder(bis, realConverter));
    }

    @Test
    void toStringBuilder_ConverterReturnsIncompatibleObject_ThrowsClassCastException() {
        Converter mock = mock(Converter.class);
        when(mock.convert(any(), eq(String.class))).thenReturn(new Thread());
        assertThrows(ClassCastException.class, () -> MapConversions.toStringBuilder("anyString", mock));
    }

    @Test
    void toStringBuilder_BeanGetterThrows_PropagatesRuntimeException() {
        // Bean with a getter that throws
        class ThrowingGetterBean {

            public String getBad() {
                throw new RuntimeException("getter failure");
            }
        }
        ThrowingGetterBean bean = new ThrowingGetterBean();
        assertThrows(RuntimeException.class, () -> MapConversions.toStringBuilder(bean, realConverter));
    }

    @Test
    void toStringBuilder_MapWithNullKey_ThrowsNullPointerException() {
        Map<String, Object> map = new HashMap<>();
        map.put(null, "value");
        assertThrows(NullPointerException.class, () -> MapConversions.toStringBuilder(map, realConverter));
    }

    @Test
    void toStringBuilder_MapValueToStringThrows_PropagatesRuntimeException() {
        class BadToString {

            @Override
            public String toString() {
                throw new RuntimeException("bad toString");
            }
        }
        Map<String, Object> map = Collections.singletonMap("bad", new BadToString());
        assertThrows(RuntimeException.class, () -> MapConversions.toStringBuilder(map, realConverter));
    }

    @Test
    @Disabled("Demonstrates expected StackOverflowError for circular maps – not run by default")
    void toStringBuilder_CircularReferenceMap_ThrowsStackOverflowError() {
        Map<String, Object> map = new HashMap<>();
        // direct circular reference
        map.put("self", map);
        assertThrows(StackOverflowError.class, () -> MapConversions.toStringBuilder(map, realConverter));
    }

    /* --------------------------------------------------------------------- */
    @SuppressWarnings("unused")
    private static Map<String, Object> createCircularMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("self", map);
        return map;
    }
}
