package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import java.time.*;
import java.time.format.DateTimeFormatter;

// Mockito is not used but kept from the template
@ExtendWith(MockitoExtension.class)
class OffsetDateTimeConversions_toMap_Test {

    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The library’s Converter needs a ConverterOptions instance; the default works for our tests.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @Test
    void toMap_withOffsetDateTime_returnsMapContainingStringRepresentation() {
        // given
        OffsetDateTime now = OffsetDateTime.now();
        // when
        Map<String, Object> result = OffsetDateTimeConversions.toMap(now, converter);
        // then
        assertNotNull(result, "Resulting map must not be null");
        assertEquals(1, result.size(), "Map should contain exactly one entry");
        assertTrue(result.containsKey(MapConversions.OFFSET_DATE_TIME), "Map must contain the key defined in MapConversions.OFFSET_DATE_TIME");
        String expectedString = converter.convert(now, String.class);
        assertEquals(expectedString, result.get(MapConversions.OFFSET_DATE_TIME), "The value stored in the map must be the String conversion of the OffsetDateTime");
    }

    @Test
    void toMap_withNullSource_returnsMapWithNullValue() {
        // given
        Object source = null;
        // when
        Map<String, Object> result = OffsetDateTimeConversions.toMap(source, converter);
        // then
        assertNotNull(result, "Resulting map must not be null even when source is null");
        assertEquals(1, result.size(), "Map should contain exactly one entry");
        assertTrue(result.containsKey(MapConversions.OFFSET_DATE_TIME), "Map must contain the key defined in MapConversions.OFFSET_DATE_TIME");
        assertNull(result.get(MapConversions.OFFSET_DATE_TIME), "The value for a null source must be null");
    }

    @Test
    void toMap_withZonedDateTime_instance_convertsUsingOffset() {
        // given
        ZonedDateTime zdt = ZonedDateTime.of(2021, 12, 31, 23, 59, 59, 0, // +01:00 at that date
        ZoneId.of("Europe/Paris"));
        // when
        Map<String, Object> result = OffsetDateTimeConversions.toMap(zdt, converter);
        // then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.containsKey(MapConversions.OFFSET_DATE_TIME));
        // Expected ISO‑8601 string with the correct offset
        String expected = "2021-12-31T23:59:59+01:00";
        assertEquals(expected, result.get(MapConversions.OFFSET_DATE_TIME));
    }

    @Test
    void toMap_withLocalDateTime_instance_returnsPlainLocalDateTimeString() {
        // given
        LocalDateTime ldt = LocalDateTime.of(2020, 2, 29, 0, 0, 0);
        // when
        Map<String, Object> result = OffsetDateTimeConversions.toMap(ldt, converter);
        // then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.containsKey(MapConversions.OFFSET_DATE_TIME));
        // LocalDateTime has no offset, so the toString helper should return the plain ISO string
        String expected = "2020-02-29T00:00:00";
        assertEquals(expected, result.get(MapConversions.OFFSET_DATE_TIME));
    }

    @Test
    void toMap_withStringSource_returnsSameString() {
        // given
        String source = "2022-01-01T10:15:30+05:00";
        // when
        Map<String, Object> result = OffsetDateTimeConversions.toMap(source, converter);
        // then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.containsKey(MapConversions.OFFSET_DATE_TIME));
        assertEquals(source, result.get(MapConversions.OFFSET_DATE_TIME));
    }

    @Test
    void toMap_withCustomConverter_changesFormatAccordingToConverter() {
        // given: an OffsetDateTime that will be formatted by a custom Converter
        OffsetDateTime odt = OffsetDateTime.of(1999, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC);
        // custom converter that formats OffsetDateTime using a specific pattern
        Converter customConverter = new Converter() {

            @Override
            public <T> T convert(Object src, Class<T> target) {
                if (target == String.class && src instanceof OffsetDateTime) {
                    String formatted = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm O").format((OffsetDateTime) src);
                    @SuppressWarnings("unchecked")
                    T result = (T) formatted;
                    return result;
                }
                return super.convert(src, target);
            }
        };
        // when
        Map<String, Object> result = OffsetDateTimeConversions.toMap(odt, customConverter);
        // then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.containsKey(MapConversions.OFFSET_DATE_TIME));
        String expected = "31/12/1999 23:59 Z";
        assertEquals(expected, result.get(MapConversions.OFFSET_DATE_TIME));
    }

    @Test
    void toMap_withNullConverter_throwsNullPointerException() {
        // given
        OffsetDateTime now = OffsetDateTime.now();
        // when / then
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toMap(now, null), "Passing a null Converter should result in NullPointerException");
    }

    @Test
    void toMap_withUnsupportedSourceType_throwsIllegalArgumentException() {
        // given
        Integer unsupported = 123;
        // when / then
        assertThrows(IllegalArgumentException.class, () -> OffsetDateTimeConversions.toMap(unsupported, converter), "An unsupported source type should cause IllegalArgumentException");
    }

    @Test
    void toMap_whenConverterThrowsRuntimeException_propagatesException() {
        // given
        OffsetDateTime now = OffsetDateTime.now();
        // Converter that deliberately throws a RuntimeException during conversion
        Converter explodingConverter = new Converter() {

            @Override
            public <T> T convert(Object src, Class<T> target) {
                throw new RuntimeException("boom");
            }
        };
        // when / then
        RuntimeException ex = assertThrows(RuntimeException.class, () -> OffsetDateTimeConversions.toMap(now, explodingConverter), "The RuntimeException thrown by the Converter must be propagated");
        assertEquals("boom", ex.getMessage());
    }
}
