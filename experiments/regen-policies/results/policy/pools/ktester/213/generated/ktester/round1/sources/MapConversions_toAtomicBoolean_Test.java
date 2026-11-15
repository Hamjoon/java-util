package com.cedarsoftware.util.convert;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Stream;
// import org.junit.*;
// import org.junit.runner.RunWith;
// import org.junit.runners.Parameterized;
// import org.junit.runners.Parameterized.Parameters;

class MapConversions_toAtomicBoolean_Test {

    private Converter converter;

//     @RunWith(Parameterized.class)
    public static class SuccessfulConversionTests {

        private static Stream<Arguments> successfulCases() {
            return Stream.of(// already an AtomicBoolean – should be returned unchanged (or equivalent)
            Arguments.of(new AtomicBoolean(true), true, "AtomicBoolean true should be returned as‑is"), Arguments.of(new AtomicBoolean(false), false, "AtomicBoolean false should be returned as‑is"), // Boolean values
            Arguments.of(Boolean.TRUE, true, "Boolean.TRUE → true"), Arguments.of(Boolean.FALSE, false, "Boolean.FALSE → false"), // String values (case‑insensitive, trimmed)
            Arguments.of("true", true, "String \"true\" → true"), Arguments.of("false", false, "String \"false\" → false"), Arguments.of("TRUE", true, "Upper‑case \"TRUE\" → true"), Arguments.of("  false  ", false, "String with whitespace → false"), Arguments.of("1", true, "Numeric string \"1\" → true"), Arguments.of("0", false, "Numeric string \"0\" → false"), // Numeric primitives / wrappers (non‑zero = true, zero = false)
            Arguments.of(Integer.valueOf(1), true, "Integer 1 → true"), Arguments.of(Integer.valueOf(0), false, "Integer 0 → false"), Arguments.of(Long.valueOf(123L), true, "Long 123 → true"), Arguments.of(Long.valueOf(0L), false, "Long 0 → false"), Arguments.of(Short.valueOf((short) 0), false, "Short 0 → false"), Arguments.of(Byte.valueOf((byte) 5), true, "Byte 5 → true"), // Atomic numeric types
            Arguments.of(new AtomicInteger(0), false, "AtomicInteger 0 → false"), Arguments.of(new AtomicLong(1L), true, "AtomicLong 1 → true"), // Maps with "value" or "_v" keys
            Arguments.of(Map.of("value", Boolean.TRUE), true, "Map {value=TRUE} → true"), Arguments.of(Map.of("value", Boolean.FALSE), false, "Map {value=FALSE} → false"), Arguments.of(Map.of("_v", "true"), true, "Map {_v=\"true\"} → true"), Arguments.of(Map.of("value", null), null, "Map {value=null} → null"), Arguments.of(Map.of("value", null, "_v", Boolean.TRUE), null, "Map {value=null, _v=TRUE} prefers \"value\" → null"), Arguments.of(Map.of("_v", null), null, "Map {_v=null} → null"), Arguments.of(Map.of("value", "true"), true, "Map {value=\"true\"} → true"), Arguments.of(Map.of("_v", 0), false, "Map {_v=0} → false"), Arguments.of(Map.of("value", new AtomicBoolean(false)), false, "Map {value=AtomicBoolean(false)} → false"), Arguments.of(Map.of("value", new AtomicInteger(2)), true, "Map {value=AtomicInteger(2)} → true"), // CharSequence implementations
            Arguments.of(new StringBuilder("false"), false, "StringBuilder \"false\" → false"), // Null source
            Arguments.of(null, null, "null source → null"));
        }

        private final Object source;

        private final Boolean expected;

        private final String description;

        private Converter converter;

        public SuccessfulConversionTests(Object source, Boolean expected, String description) {
            this.source = source;
            this.expected = expected;
            this.description = description;
        }

//         @Before
//         public void initConverter() {
//             converter = new Converter(new ConverterOptions() {
//             });
//         }

//         @Parameters(name = "[{index}] {2}")
//         public static Collection<Object[]> data() {
//             List<Object[]> list = new ArrayList<>();
//             successfulCases().forEach(arg -> {
//                 Object[] arr = new Object[3];
//                 arr[0] = arg.get()[0];
//                 arr[1] = arg.get()[1];
//                 arr[2] = arg.get()[2];
//                 list.add(arr);
//             });
//             return list;
//         }

//         @Test
//         public void testConversion() {
//             AtomicBoolean result = MapConversions.toAtomicBoolean(source, converter);
//             if (expected == null) {
//                 Assert.assertNull(description, result);
//             } else {
//                 Assert.assertNotNull(description, result);
//                 Assert.assertEquals(description, expected, result.get());
//             }
//         }
    }

    /* -----------------------------------------------------------------
     *  Helper class to mimic the JUnit‑5 Arguments container
     * ----------------------------------------------------------------- */
    private static final class Arguments {

        private final Object[] values;

        private Arguments(Object... values) {
            this.values = values;
        }

        static Arguments of(Object... values) {
            return new Arguments(values);
        }

        Object[] get() {
            return values;
        }
    }

//     @BeforeEach
    void setupBeforeEach() {
        // The Converter constructor requires a ConverterOptions instance.
        // If ConverterOptions is abstract we create an anonymous subclass; otherwise we use the concrete constructor.
        converter = new Converter(new ConverterOptions() {
        });
    }

    private static Stream<Arguments> successfulCases() {
        return // already an AtomicBoolean – should be returned unchanged (or equivalent)
        Stream.// already an AtomicBoolean – should be returned unchanged (or equivalent)
        of(// Boolean values
        Arguments.of(new AtomicBoolean(true), true, "AtomicBoolean true should be returned as‑is"), // Boolean values
        Arguments.of(new AtomicBoolean(false), false, "AtomicBoolean false should be returned as‑is"), // String values (case‑insensitive, trimmed)
        Arguments.of(Boolean.TRUE, true, "Boolean.TRUE → true"), // String values (case‑insensitive, trimmed)
        Arguments.of(Boolean.FALSE, false, "Boolean.FALSE → false"), // Numeric primitives / wrappers (non‑zero = true, zero = false)
        Arguments.of("true", true, "String \"true\" → true"), // Numeric primitives / wrappers (non‑zero = true, zero = false)
        Arguments.of("false", false, "String \"false\" → false"), // Numeric primitives / wrappers (non‑zero = true, zero = false)
        Arguments.of("TRUE", true, "Upper‑case \"TRUE\" → true"), // Numeric primitives / wrappers (non‑zero = true, zero = false)
        Arguments.of("  false  ", false, "String with whitespace → false"), // Numeric primitives / wrappers (non‑zero = true, zero = false)
        Arguments.of("1", true, "Numeric string \"1\" → true"), // Numeric primitives / wrappers (non‑zero = true, zero = false)
        Arguments.of("0", false, "Numeric string \"0\" → false"), // Atomic numeric types
        Arguments.of(Integer.valueOf(1), true, "Integer 1 → true"), // Atomic numeric types
        Arguments.of(Integer.valueOf(0), false, "Integer 0 → false"), // Atomic numeric types
        Arguments.of(Long.valueOf(123L), true, "Long 123 → true"), // Atomic numeric types
        Arguments.of(Long.valueOf(0L), false, "Long 0 → false"), // Atomic numeric types
        Arguments.of(Short.valueOf((short) 0), false, "Short 0 → false"), // Atomic numeric types
        Arguments.of(Byte.valueOf((byte) 5), true, "Byte 5 → true"), // Maps with "value" or "_v" keys
        Arguments.of(new AtomicInteger(0), false, "AtomicInteger 0 → false"), // Maps with "value" or "_v" keys
        Arguments.of(new AtomicLong(1L), true, "AtomicLong 1 → true"), // CharSequence implementations
        Arguments.of(Map.of("value", Boolean.TRUE), true, "Map {value=TRUE} → true"), // CharSequence implementations
        Arguments.of(Map.of("value", Boolean.FALSE), false, "Map {value=FALSE} → false"), // CharSequence implementations
        Arguments.of(Map.of("_v", "true"), true, "Map {_v=\"true\"} → true"), // CharSequence implementations
        Arguments.of(Map.of("value", null), null, "Map {value=null} → null"), // CharSequence implementations
        Arguments.of(Map.of("value", null, "_v", Boolean.TRUE), null, "Map {value=null, _v=TRUE} prefers \"value\" → null"), // CharSequence implementations
        Arguments.of(Map.of("_v", null), null, "Map {_v=null} → null"), // CharSequence implementations
        Arguments.of(Map.of("value", "true"), true, "Map {value=\"true\"} → true"), // CharSequence implementations
        Arguments.of(Map.of("_v", 0), false, "Map {_v=0} → false"), // CharSequence implementations
        Arguments.of(Map.of("value", new AtomicBoolean(false)), false, "Map {value=AtomicBoolean(false)} → false"), // CharSequence implementations
        Arguments.of(Map.of("value", new AtomicInteger(2)), true, "Map {value=AtomicInteger(2)} → true"), // Null source
        Arguments.of(new StringBuilder("false"), false, "StringBuilder \"false\" → false"), Arguments.of(null, null, "null source → null"));
    }

    private static Stream<Arguments> conversionExceptionCases() {
        return Stream.of(Arguments.of(Map.of("other", Boolean.TRUE), "Map without \"value\" or \"_v\" keys → ConversionException"), Arguments.of(new Date(), "Unsupported type java.util.Date → ConversionException"), Arguments.of("yes", "Unsupported textual value \"yes\" → ConversionException"), Arguments.of("", "Empty string → ConversionException"), Arguments.of("   ", "Whitespace‑only string → ConversionException"), Arguments.of(Double.NaN, "Double.NaN → ConversionException"), Arguments.of(Double.POSITIVE_INFINITY, "Double.POSITIVE_INFINITY → ConversionException"), Arguments.of(Float.NaN, "Float.NaN → ConversionException"), Arguments.of(new boolean[] { true }, "boolean array → ConversionException"), Arguments.of(java.time.DayOfWeek.MONDAY, "Enum DayOfWeek.MONDAY → ConversionException"), Arguments.of(new Object(), "Generic Object → ConversionException"), Arguments.of(Map.of("value", new Date()), "Map {value=Date} → ConversionException"), Arguments.of(Map.of("_v", List.of(Boolean.TRUE)), "Map {_v=List} → ConversionException"));
    }

    private static Stream<Arguments> nullConverterCases() {
        return Stream.of(Arguments.of("true", "Null Converter with valid source → NullPointerException"));
    }

//     @Test
//     void toAtomicBoolean_fromMapWithValueKey_booleanTrue() {
//         Map<String, Object> src = new HashMap<>();
//         src.put(MapConversions.VALUE, Boolean.TRUE);
//         AtomicBoolean result = MapConversions.toAtomicBoolean(src, converter);
//         Assert.assertNotNull("Result should not be null", result);
//         Assert.assertTrue("AtomicBoolean should contain true", result.get());
//     }

//     @Test
//     void toAtomicBoolean_fromMapWithVKey_stringFalse() {
//         Map<String, Object> src = new HashMap<>();
//         src.put(MapConversions.V, "false");
//         AtomicBoolean result = MapConversions.toAtomicBoolean(src, converter);
//         Assert.assertNotNull("Result should not be null", result);
//         Assert.assertFalse("AtomicBoolean should contain false", result.get());
//     }

//     @Test
//     void toAtomicBoolean_fromDirectBoolean_true() {
//         AtomicBoolean result = MapConversions.toAtomicBoolean(Boolean.TRUE, converter);
//         Assert.assertNotNull("Result should not be null", result);
//         Assert.assertTrue("AtomicBoolean should contain true", result.get());
//     }

//     @Test
//     void toAtomicBoolean_fromDirectString_true() {
//         AtomicBoolean result = MapConversions.toAtomicBoolean("true", converter);
//         Assert.assertNotNull("Result should not be null", result);
//         Assert.assertTrue("AtomicBoolean should contain true", result.get());
//     }

//     @Test
//     void toAtomicBoolean_fromUnsupportedObject_returnsNull() {
//         Object unsupported = new Object();
//         AtomicBoolean result = MapConversions.toAtomicBoolean(unsupported, converter);
//         Assert.assertNull("Result should be null for unsupported source", result);
//     }

//     @Test
//     void toAtomicBoolean_fromNull_returnsNull() {
//         AtomicBoolean result = MapConversions.toAtomicBoolean(null, converter);
//         Assert.assertNull("Result should be null when source is null", result);
//     }

    /* -------------------------------------------------------------
     *  Successful conversion cases (including expected null result)
     * ------------------------------------------------------------- */
//     @ParameterizedTest(name = "[{index}] {2}")
//     @MethodSource("successfulCases")
//     void toAtomicBoolean_successful(Object from, Boolean expectedValue, String description) {
//         AtomicBoolean result = MapConversions.toAtomicBoolean(from, converter);
//         if (expectedValue == null) {
//             assertNull(result, description);
//         } else {
//             assertNotNull(result, description);
//             assertEquals(expectedValue, result.get(), description);
//         }
//     }

    /* -------------------------------------------------------------
     *  Cases that must raise NullPointerException (null Converter)
     * ------------------------------------------------------------- */
//     @ParameterizedTest(name = "[{index}] {2}")
//     @MethodSource("nullConverterCases")
//     void toAtomicBoolean_nullConverter(Object from, String description) {
//         assertThrows(NullPointerException.class, () -> MapConversions.toAtomicBoolean(from, null), description);
//     }

//     @Before
//     public void setUp() {
//         // The Converter constructor requires a ConverterOptions instance.
//         // If ConverterOptions is abstract we create an anonymous subclass; otherwise we use the concrete constructor.
//         converter = new Converter(new ConverterOptions() {
//         });
//     }

//     @Test(expected = NullPointerException.class)
//     public void toAtomicBoolean_nullConverter_throwsNPE() {
//         MapConversions.toAtomicBoolean("true", null);
//     }
}