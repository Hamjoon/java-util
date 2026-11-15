package com.cedarsoftware.util.convert;

import java.time.Period;
import java.util.HashMap;
import java.util.Map;
import java.time.format.DateTimeParseException;
// import org.junit.*;
// import static org.junit.Assert.*;

// @ExtendWith(MockitoExtension.class)
class MapConversions_toPeriod_Test {

    private static Converter converter;

//     @BeforeAll
    static void setupBeforeAll() {
        // The Converter class requires a ConverterOptions instance.
        // Using the default (no‑arg) constructor of ConverterOptions supplies the
        // standard configuration needed for the tests.
//         converter = new Converter(new ConverterOptions());
    }

//     @Test
//     void toPeriod_fromMapWithPeriodKey() {
//         Map<String, Object> map = new HashMap<>();
//         // ISO‑8601 period string
//         map.put("period", "P1Y2M3D");
//         Period result = MapConversions.toPeriod(map, converter);
//         assertEquals(Period.of(1, 2, 3), result);
//     }

//     @Test
//     void toPeriod_fromMapWithValueKey() {
//         Map<String, Object> map = new HashMap<>();
//         // 5 days
//         map.put("value", "P5D");
//         Period result = MapConversions.toPeriod(map, converter);
//         assertEquals(Period.ofDays(5), result);
//     }

//     @Test
//     void toPeriod_fromMapWithVKey() {
//         Map<String, Object> map = new HashMap<>();
//         // 4 weeks (ISO‑8601)
//         map.put("_v", "P4W");
//         Period result = MapConversions.toPeriod(map, converter);
//         assertEquals(Period.ofWeeks(4), result);
//     }

//     @Test
//     void toPeriod_whenSourceIsAlreadyPeriod() {
//         Period source = Period.ofMonths(6);
//         Period result = MapConversions.toPeriod(source, converter);
//         // The method should return the original Period instance (or an equal one)
//         assertEquals(source, result);
//     }

//     @Test
//     void toPeriod_whenSourceIsNull() {
//         assertNull(MapConversions.toPeriod(null, converter));
//     }

//     @Test
//     void toPeriod_fromMapWithPeriodKeyHoldingPeriod() {
//         Map<String, Object> map = Map.of(MapConversions.PERIOD, Period.of(2, 0, 0));
//         Period result = MapConversions.toPeriod(map, converter);
//         assertEquals(Period.of(2, 0, 0), result);
//     }

//     @Test
//     void toPeriod_fromMapWithValueKeyHoldingString() {
//         Map<String, Object> map = Map.of(MapConversions.VALUE, "P3Y4M5D");
//         Period result = MapConversions.toPeriod(map, converter);
//         assertEquals(Period.of(3, 4, 5), result);
//     }

//     @Test
//     void toPeriod_fromMapWithVKeyHoldingString() {
//         Map<String, Object> map = Map.of(MapConversions.V, "P6D");
//         Period result = MapConversions.toPeriod(map, converter);
//         assertEquals(Period.ofDays(6), result);
//     }

//     @Test
//     void toPeriod_fromMapWithPeriodKeyHoldingString() {
//         Map<String, Object> map = Map.of(MapConversions.PERIOD, "P2Y3M4D");
//         Period result = MapConversions.toPeriod(map, converter);
//         assertEquals(Period.of(2, 3, 4), result);
//     }

//     @Test
//     void toPeriod_fromMapWithBothPeriodAndValueKeys_usesPeriodPrecedence() {
//         Map<String, Object> map = Map.of(MapConversions.PERIOD, Period.of(1, 0, 0), MapConversions.VALUE, "P5D");
//         Period result = MapConversions.toPeriod(map, converter);
//         assertEquals(Period.of(1, 0, 0), result);
//     }

//     @Test
//     void toPeriod_fromMapWithPeriodKeyAndNullValue() {
//         Map<String, Object> map = Map.of(MapConversions.PERIOD, null);
//         assertNull(MapConversions.toPeriod(map, converter));
//     }

//     @Test
//     void toPeriod_fromMapWithValueKeyAndNullValue() {
//         Map<String, Object> map = Map.of(MapConversions.VALUE, null);
//         assertNull(MapConversions.toPeriod(map, converter));
//     }

//     @Test
//     void toPeriod_fromMapWithUnsupportedKey() {
//         Map<String, Object> map = Map.of("unknown", "P1Y");
//         try {
//             MapConversions.toPeriod(map, converter);
//             fail("Expected IllegalArgumentException");
//         } catch (IllegalArgumentException ignored) {
//         }
//     }

//     @Test
//     void toPeriod_fromEmptyMap() {
//         Map<String, Object> map = Map.of();
//         try {
//             MapConversions.toPeriod(map, converter);
//             fail("Expected IllegalArgumentException");
//         } catch (IllegalArgumentException ignored) {
//         }
//     }

//     @Test
//     void toPeriod_fromMapWithValueKeyHoldingPeriodObject() {
//         Map<String, Object> map = Map.of(MapConversions.VALUE, Period.ofMonths(5));
//         Period result = MapConversions.toPeriod(map, converter);
//         assertEquals(Period.ofMonths(5), result);
//     }

//     @Test
//     void toPeriod_fromMapWithValueKeyHoldingUnsupportedInteger() {
//         Map<String, Object> map = Map.of(MapConversions.VALUE, 10);
//         try {
//             MapConversions.toPeriod(map, converter);
//             fail("Expected IllegalArgumentException");
//         } catch (IllegalArgumentException ignored) {
//         }
//     }

//     @Test
//     void toPeriod_fromMapWithPeriodKeyHoldingInvalidString() {
//         Map<String, Object> map = Map.of(MapConversions.PERIOD, "INVALID");
//         try {
//             MapConversions.toPeriod(map, converter);
//             fail("Expected DateTimeParseException");
//         } catch (DateTimeParseException ignored) {
//         }
//     }

//     @Test
//     void toPeriod_fromMapWithValueKeyHoldingEmptyString() {
//         Map<String, Object> map = Map.of(MapConversions.VALUE, "");
//         try {
//             MapConversions.toPeriod(map, converter);
//             fail("Expected DateTimeParseException");
//         } catch (DateTimeParseException ignored) {
//         }
//     }

//     @Test
//     void toPeriod_fromMapWithPeriodKeyHoldingBoolean() {
//         Map<String, Object> map = Map.of(MapConversions.PERIOD, Boolean.TRUE);
//         try {
//             MapConversions.toPeriod(map, converter);
//             fail("Expected IllegalArgumentException");
//         } catch (IllegalArgumentException ignored) {
//         }
//     }

//     @Test
//     void toPeriod_fromString() {
//         Period result = MapConversions.toPeriod("P1Y", converter);
//         assertEquals(Period.ofYears(1), result);
//     }

//     @Test
//     void toPeriod_fromStringBuilder() {
//         Period result = MapConversions.toPeriod(new StringBuilder("P2Y"), converter);
//         assertEquals(Period.ofYears(2), result);
//     }

//     @Test
//     void toPeriod_fromInvalidString() {
//         try {
//             MapConversions.toPeriod("INVALID", converter);
//             fail("Expected DateTimeParseException");
//         } catch (DateTimeParseException ignored) {
//         }
//     }

//     @Test
//     void toPeriod_fromEmptyString() {
//         try {
//             MapConversions.toPeriod("", converter);
//             fail("Expected DateTimeParseException");
//         } catch (DateTimeParseException ignored) {
//         }
//     }

//     @Test
//     void toPeriod_fromWhitespaceString() {
//         try {
//             MapConversions.toPeriod("   ", converter);
//             fail("Expected DateTimeParseException");
//         } catch (DateTimeParseException ignored) {
//         }
//     }

//     @Test
//     void toPeriod_fromUnsupportedInteger() {
//         try {
//             MapConversions.toPeriod(5, converter);
//             fail("Expected IllegalArgumentException");
//         } catch (IllegalArgumentException ignored) {
//         }
//     }

//     @Test
//     void toPeriod_nullConverterWithPeriodSource() {
//         Period source = Period.of(1, 2, 3);
//         try {
//             MapConversions.toPeriod(source, null);
//             fail("Expected NullPointerException");
//         } catch (NullPointerException ignored) {
//         }
//     }

//     @Test
//     void toPeriod_nullConverterWithCharSequenceSource() {
//         try {
//             MapConversions.toPeriod("P1Y", null);
//             fail("Expected NullPointerException");
//         } catch (NullPointerException ignored) {
//         }
//     }

//     @Test
//     void toPeriod_nullConverterWithMapSource() {
//         Map<String, Object> map = Map.of(MapConversions.PERIOD, "P2Y");
//         try {
//             MapConversions.toPeriod(map, null);
//             fail("Expected NullPointerException");
//         } catch (NullPointerException ignored) {
//         }
//     }

//     @Test
//     public void toPeriod_fromVKey() {
//         Map<String, Object> map = new HashMap<>();
//         // 4 weeks (ISO‑8601)
//         map.put("_v", "P4W");
//         Period result = MapConversions.toPeriod(map, converter);
//         assertEquals(Period.ofWeeks(4), result);
//     }
}