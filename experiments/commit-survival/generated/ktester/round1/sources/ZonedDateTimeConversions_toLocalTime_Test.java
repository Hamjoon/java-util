package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import com.cedarsoftware.util.CompactLinkedMap;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeException;
import java.util.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.stream.Stream;

class ZonedDateTimeConversions_toLocalTime_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // ConverterOptions has a public no‑arg constructor in the library.
        // Using the default options gives us the built‑in conversion rules.
        converter = new Converter(new ConverterOptions());
    }

    static Stream<Arguments> toLocalTimeTestProvider() {
        ???;
    }

    @Test
    void toLocalTime_fromZonedDateTime_returnsCorrectLocalTime() {
        // GIVEN
        ZonedDateTime zdt = ZonedDateTime.of(2023, 5, 10, 15, 30, 45, 0, ZoneId.of("UTC"));
        // WHEN
        LocalTime result = ZonedDateTimeConversions.toLocalTime(zdt, converter);
        // THEN
        assertNotNull(result);
        assertEquals(LocalTime.of(15, 30, 45), result);
    }

    @Test
    void toLocalTime_fromOffsetDateTime_returnsCorrectLocalTime() {
        // GIVEN
        OffsetDateTime odt = OffsetDateTime.of(LocalDateTime.of(2023, 5, 10, 9, 15, 20), ZoneOffset.ofHours(-4));
        // WHEN
        LocalTime result = ZonedDateTimeConversions.toLocalTime(odt, converter);
        // THEN
        assertNotNull(result);
        assertEquals(LocalTime.of(9, 15, 20), result);
    }

    @Test
    void toLocalTime_fromMapWithTimeAndZone_returnsCorrectLocalTime() {
        // GIVEN
        Map<String, Object> map = new CompactLinkedMap<>();
        map.put(MapConversions.TIME, "10:20:30");
        map.put(MapConversions.ZONE, "Europe/Paris");
        // WHEN
        LocalTime result = ZonedDateTimeConversions.toLocalTime(map, converter);
        // THEN
        assertNotNull(result);
        assertEquals(LocalTime.of(10, 20, 30), result);
    }

    @Test
    void toLocalTime_nullInput_returnsNull() {
        // GIVEN
        Object input = null;
        // WHEN
        LocalTime result = ZonedDateTimeConversions.toLocalTime(input, converter);
        // THEN
        assertNull(result);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("toLocalTimeTestProvider")
    void toLocalTime_variousInputs(String description, Object input, Object expected) {
        if (expected instanceof Class) {
            Class<?> exClass = (Class<?>) expected;
            assertThrows(exClass, () -> ZonedDateTimeConversions.toLocalTime(input, converter), description);
        } else {
            LocalTime result = ZonedDateTimeConversions.toLocalTime(input, converter);
            if (expected == null) {
                assertNull(result, description);
            } else {
                assertNotNull(result, description);
                assertEquals(expected, result, description);
            }
        }
    }
}
