package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.net.URI;
import java.net.URL;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MapConversions_toString_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Use the real Converter with default options – this gives us the actual conversion logic.
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up per‑test.
        converter = null;
    }

    static Stream<Arguments> simpleMapProvider() {
        return Stream.of(Arguments.of(Map.of("name", "Alice", "age", 30), DefaultConverter.INSTANCE, "{name=Alice, age=30}"), Arguments.of(new CompactMap<>(Map.of("city", "Paris", "zip", "75001")), DefaultConverter.INSTANCE, "{city=Paris, zip=75001}"));
    }

    static Stream<Arguments> specialKeysMapProvider() {
        return Stream.of(Arguments.of(Map.of(MapConversions.VALUE, "special", MapConversions.CLASS, "java.lang.String"), DefaultConverter.INSTANCE, "{value=special, class=java.lang.String}"), Arguments.of(Map.of(MapConversions.V, "_v", MapConversions.ID, "12345"), DefaultConverter.INSTANCE, "{_v=_v, id=12345}"));
    }

    static Stream<Arguments> nonMapObjectProvider() {
        return Stream.of(Arguments.of("Just a plain string", DefaultConverter.INSTANCE, "Just a plain string"), Arguments.of(42, DefaultConverter.INSTANCE, "42"), Arguments.of(new Date(0L), DefaultConverter.INSTANCE, "1970-01-01T00:00:00Z"));
    }

    @Test
    void toString_usesVKey_whenPresent() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.V, "hello world");
        // WHEN
        String result = MapConversions.toString(map, converter);
        // THEN
        assertEquals("hello world", result, "MapConversions.toString should return the value associated with the \"_v\" key.");
    }

    @Test
    void toString_usesValueKey_whenVKeyMissing() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        // an Integer – should be converted to its String form
        map.put(MapConversions.VALUE, 12345);
        // WHEN
        String result = MapConversions.toString(map, converter);
        // THEN
        assertEquals("12345", result, "When the \"_v\" key is absent, MapConversions.toString should fall back to the \"value\" key " + "and convert the associated object to a String.");
    }

    @Test
    void toString_returnsNull_whenNoRecognisedKey() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put("unrelated", "data");
        // WHEN
        String result = MapConversions.toString(map, converter);
        // THEN
        assertNull(result, "If the map does not contain any of the recognised keys, the conversion should yield null.");
    }

    @Test
    void toString_handlesNullMapGracefully() {
        // GIVEN
        Map<String, Object> map = null;
        // WHEN
        String result = MapConversions.toString(map, converter);
        // THEN
        assertNull(result, "Passing a null map should result in a null return value rather than throwing an exception.");
    }

    // -----------------------------------------------------------------
    @Test
    void toString_nullInput_returnsLiteralNullString() {
        // GIVEN
        Object from = null;
        // the library’s default singleton
        Converter conv = DefaultConverter.INSTANCE;
        // WHEN
        String result = MapConversions.toString(from, conv);
        // THEN
        assertEquals("null", result, "When the source object is null the method should return the literal string \"null\".");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("simpleMapProvider")
    void toString_simpleMap(Object from, Converter conv, String expected) {
        assertEquals(expected, MapConversions.toString(from, conv));
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("specialKeysMapProvider")
    void toString_mapWithSpecialKeys(Object from, Converter conv, String expected) {
        assertEquals(expected, MapConversions.toString(from, conv));
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("nonMapObjectProvider")
    void toString_nonMapObject(Object from, Converter conv, String expected) {
        assertEquals(expected, MapConversions.toString(from, conv));
    }

    // -----------------------------------------------------------------
    @Test
    void toString_customConverter_appendsUsd() {
        // GIVEN
        Map<String, Object> map = Map.of("price", new java.math.BigDecimal("19.99"));
        Converter custom = new Converter() {

            @Override
            public <T> T convert(Object src, Class<T> target) {
                // Simple behaviour: append " USD" to any value’s toString()
                return (T) (src.toString() + " USD");
            }
        };
        // WHEN
        String result = MapConversions.toString(map, custom);
        // THEN
        assertEquals("{price=19.99 USD}", result);
    }

    @Test
    void toString_customConverter_throwsException_propagated() {
        // GIVEN
        Map<String, Object> map = Map.of("flag", true);
        Converter exploding = new Converter() {

            @Override
            public <T> T convert(Object src, Class<T> target) {
                throw new IllegalArgumentException("conversion not allowed");
            }
        };
        // WHEN / THEN
        RuntimeException ex = assertThrows(RuntimeException.class, () -> MapConversions.toString(map, exploding));
        // The library wraps the original exception; we verify the cause/message.
        assertTrue(ex.getCause() instanceof IllegalArgumentException);
        assertEquals("conversion not allowed", ex.getCause().getMessage());
    }

    // -----------------------------------------------------------------
    @Test
    void toString_complexNestedMap() {
        // GIVEN
        Map<String, Object> nested = Map.of("user", Map.of("id", 7, "name", "Bob"), "roles", List.of("admin", "user"));
        Converter conv = DefaultConverter.INSTANCE;
        // WHEN
        String result = MapConversions.toString(nested, conv);
        // THEN
        assertEquals("{user={id=7, name=Bob}, roles=[admin, user]}", result);
    }

    @Test
    void toString_mapWithInstant() {
        // GIVEN
        Map<String, Object> map = Map.of("timestamp", Instant.parse("2023-01-01T12:00:00Z"));
        Converter conv = DefaultConverter.INSTANCE;
        // WHEN
        String result = MapConversions.toString(map, conv);
        // THEN
        assertEquals("{timestamp=2023-01-01T12:00:00Z}", result);
    }

    // -----------------------------------------------------------------
    @Test
    void toString_emptyMap() {
        assertEquals("{}", MapConversions.toString(Map.of(), DefaultConverter.INSTANCE));
    }

    // -----------------------------------------------------------------
    @Test
    void toString_mapWithNullValue() {
        Map<String, Object> map = Map.of("key", null);
        assertEquals("{key=null}", MapConversions.toString(map, DefaultConverter.INSTANCE));
    }

    // -----------------------------------------------------------------
    @Test
    void toString_allTemporalTypes() {
        // GIVEN – a map containing one instance of each supported Java‑8+ temporal class
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("localDate", LocalDate.of(2022, 5, 1));
        map.put("localDateTime", LocalDateTime.of(2022, 5, 1, 13, 45, 30));
        map.put("localTime", LocalTime.of(13, 45, 30));
        map.put("offsetDateTime", OffsetDateTime.of(2022, 5, 1, 13, 45, 30, 0, ZoneOffset.ofHours(2)));
        map.put("offsetTime", OffsetTime.of(13, 45, 30, 0, ZoneOffset.ofHours(2)));
        map.put("zonedDateTime", ZonedDateTime.of(2022, 5, 1, 13, 45, 30, 0, ZoneId.of("Europe/Paris")));
        map.put("year", Year.of(2022));
        map.put("yearMonth", YearMonth.of(2022, 5));
        map.put("monthDay", MonthDay.of(5, 1));
        map.put("period", Period.of(1, 2, 3));
        map.put("duration", Duration.ofHours(4).plusMinutes(5).plusSeconds(6));
        map.put("zoneId", ZoneId.of("Europe/Paris"));
        map.put("zoneOffset", ZoneOffset.ofHours(2));
        String expected = "{localDate=2022-05-01, localDateTime=2022-05-01T13:45:30, localTime=13:45:30, " + "offsetDateTime=2022-05-01T13:45:30+02:00, offsetTime=13:45:30+02:00, " + "zonedDateTime=2022-05-01T13:45:30+02:00[Europe/Paris], year=2022, yearMonth=2022-05, " + "monthDay=--05-01, period=P1Y2M3D, duration=PT4H5M6S, zoneId=Europe/Paris, zoneOffset=+02:00}";
        assertEquals(expected, MapConversions.toString(map, DefaultConverter.INSTANCE));
    }

    // -----------------------------------------------------------------
    @Test
    void toString_localeUuidUriUrl() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("locale", Locale.forLanguageTag("en-US"));
        map.put("uuid", UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
        map.put("uri", URI.create("https://example.com/path?query=1"));
        map.put("url", new URL("https://example.com"));
        String expected = "{locale=en_US, uuid=123e4567-e89b-12d3-a456-426614174000, " + "uri=https://example.com/path?query=1, url=https://example.com}";
        assertEquals(expected, MapConversions.toString(map, DefaultConverter.INSTANCE));
    }

    // -----------------------------------------------------------------
    @Test
    void toString_atomicTypes() {
        Map<String, Object> map = Map.of("atomicBool", new AtomicBoolean(true), "atomicInt", new AtomicInteger(5), "atomicLong", new AtomicLong(10L));
        assertEquals("{atomicBool=true, atomicInt=5, atomicLong=10}", MapConversions.toString(map, DefaultConverter.INSTANCE));
    }

    // -----------------------------------------------------------------
    @Test
    void toString_numericPrimitiveWrappers() {
        Map<String, Object> map = Map.of("byte", Byte.valueOf((byte) 1), "short", Short.valueOf((short) 2), "float", Float.valueOf(3.14f));
        assertEquals("{byte=1, short=2, float=3.14}", MapConversions.toString(map, DefaultConverter.INSTANCE));
    }

    // -----------------------------------------------------------------
    @Test
    void toString_mapWithNonStringKeys() {
        Map<Object, String> map = new HashMap<>() {

            {
                put(1, "one");
                put(2L, "two");
                put(true, "bool");
            }
        };
        // The order of entries in a HashMap is undefined; we verify each pair is present.
        String result = MapConversions.toString(map, DefaultConverter.INSTANCE);
        assertTrue(result.contains("1=one"));
        assertTrue(result.contains("2=true"));
        assertTrue(result.contains("true=bool"));
        assertTrue(result.startsWith("{") && result.endsWith("}"));
    }

    // -----------------------------------------------------------------
    @Test
    void toString_nullConverter_throwsNPE() {
        assertThrows(NullPointerException.class, () -> MapConversions.toString("some text", null));
    }

    // -----------------------------------------------------------------
    @Test
    void toString_mapWithNullKey_throwsNPE() {
        Map<String, String> map = new HashMap<>() {

            {
                put(null, "value");
            }
        };
        assertThrows(NullPointerException.class, () -> MapConversions.toString(map, DefaultConverter.INSTANCE));
    }

    // -----------------------------------------------------------------
    @Test
    @Disabled("Intentionally disabled – it would cause a StackOverflowError in the test suite.")
    void toString_cyclicMapReference_overflows() {
        Map<String, Object> cyclic = new HashMap<>();
        cyclic.put("self", cyclic);
        assertThrows(StackOverflowError.class, () -> MapConversions.toString(cyclic, DefaultConverter.INSTANCE));
    }

    // -----------------------------------------------------------------
    @Test
    void toString_keyWithFaultyToString_propagatesException() {
        Object badKey = new Object() {

            @Override
            public String toString() {
                throw new RuntimeException("bad key");
            }
        };
        Map<Object, String> map = new HashMap<>();
        map.put(badKey, "val");
        RuntimeException ex = assertThrows(RuntimeException.class, () -> MapConversions.toString(map, DefaultConverter.INSTANCE));
        assertEquals("bad key", ex.getMessage());
    }

    // -----------------------------------------------------------------
    @Test
    void toString_valueWithFaultyToString_propagatesException() {
        Object badValue = new Object() {

            @Override
            public String toString() {
                throw new RuntimeException("bad value");
            }
        };
        Map<String, Object> map = new HashMap<>();
        map.put("key", badValue);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> MapConversions.toString(map, DefaultConverter.INSTANCE));
        assertEquals("bad value", ex.getMessage());
    }

    // -----------------------------------------------------------------
    @Test
    void toString_unsupportedValue_withoutConverter_throwsIAE() {
        Object nullToString = new Object() {

            @Override
            public String toString() {
                // deliberately broken
                return null;
            }
        };
        Map<String, Object> map = new HashMap<>();
        map.put("obj", nullToString);
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toString(map, DefaultConverter.INSTANCE));
    }
}
