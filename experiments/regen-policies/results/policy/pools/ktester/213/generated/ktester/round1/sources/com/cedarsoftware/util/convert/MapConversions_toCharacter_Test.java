package com.cedarsoftware.util.convert;

// import org.junit.After;
// import org.junit.Before;
// import org.junit.Test;
// import static org.junit.Assert.*;

class MapConversions_toCharacter_Test {

    private Converter converter;

//     @AfterEach
    void teardownAfterEach() {
        // Release reference – helps GC when many tests are added later.
        converter = null;
    }

//     private static Stream<Arguments> basicTypeHandlingProvider() {
//         return // Direct Character instance
//         Stream.// Direct Character instance
//         of(// Single‑character String
//         Arguments.of(Character.valueOf('A'), 'A', null), // String longer than one character → IllegalArgumentException
//         Arguments.of("Z", 'Z', null), // Integer representing a Unicode code point (65 → 'A')
//         Arguments.of("TooLong", null, IllegalArgumentException.class), // Single‑element int array → code point conversion
//         Arguments.of(65, 'A', null), // Single‑element char array → contained character
//         Arguments.of(new int[] { 66 }, 'B', null), // Null input → null result
//         Arguments.of(new char[] { 'C' }, 'C', null), Arguments.of(null, null, null));
//     }

//     private static Stream<Arguments> mapHandlingProvider() {
//         return // {"value":"F"} → 'F'
//         Stream.// {"value":"F"} → 'F'
//         of(// {"_v":71} → 'G' (71 is Unicode for 'G')
//         Arguments.of(Collections.singletonMap("value", "F"), 'F', null), // Map without expected keys → IllegalArgumentException
//         Arguments.of(Collections.singletonMap("_v", 71), 'G', null), // Map with "value" key but string longer than one char → IllegalArgumentException
//         Arguments.of(Collections.singletonMap("other", "H"), null, IllegalArgumentException.class), // Map with "_v" key but value is an int array (unsupported) → IllegalArgumentException
//         Arguments.of(Collections.singletonMap("value", "TooLong"), null, IllegalArgumentException.class), Arguments.of(Collections.singletonMap("_v", new int[] { 67 }), null, IllegalArgumentException.class));
//     }

//     private static Stream<Arguments> unsupportedTypesProvider() {
//         return // Plain Object → IllegalArgumentException
//         Stream.// Plain Object → IllegalArgumentException
//         of(// Empty int array → IllegalArgumentException
//         Arguments.of(new Object(), IllegalArgumentException.class), // Empty char array → IllegalArgumentException
//         Arguments.of(new int[] {}, IllegalArgumentException.class), // Int array with more than one element → IllegalArgumentException
//         Arguments.of(new char[] {}, IllegalArgumentException.class), // Char array with more than one element → IllegalArgumentException
//         Arguments.of(new int[] { 65, 66 }, IllegalArgumentException.class), Arguments.of(new char[] { 'X', 'Y' }, IllegalArgumentException.class));
//     }

//     @Test
//     void toCharacter_fromMapWithValueKey() {
//         Map<String, Object> source = new HashMap<>();
//         source.put("value", "A");
//         Character result = MapConversions.toCharacter(source, converter);
//         assertEquals(Character.valueOf('A'), result);
//     }

//     @Test
//     void toCharacter_fromMapWithVKey() {
//         Map<String, Object> source = new HashMap<>();
//         source.put("_v", "B");
//         Character result = MapConversions.toCharacter(source, converter);
//         assertEquals(Character.valueOf('B'), result);
//     }

//     @Test
//     void toCharacter_fromCharacterInstance() {
//         Character src = 'C';
//         Character result = MapConversions.toCharacter(src, converter);
//         assertEquals(Character.valueOf('C'), result);
//     }

//     @Test
//     void toCharacter_fromSingleCharString() {
//         String src = "D";
//         Character result = MapConversions.toCharacter(src, converter);
//         assertEquals(Character.valueOf('D'), result);
//     }

//     @Test
//     void toCharacter_fromNumber() {
//         // ASCII code for 'E'
//         Integer src = 69;
//         Character result = MapConversions.toCharacter(src, converter);
//         assertEquals(Character.valueOf('E'), result);
//     }

//     @Test
//     void toCharacter_fromNullReturnsNull() {
//         Character result = MapConversions.toCharacter(null, converter);
//         assertNull(result);
//     }

    /* -------------------------------------------------------------
     *  Basic type handling
     * ------------------------------------------------------------- */
//     @ParameterizedTest(name = "[{index}] basic conversion: from={0} => expected={1}")
//     @MethodSource("basicTypeHandlingProvider")
//     void toCharacter_basicTypes(Object from, Character expected, Class<? extends Throwable> expectedException) {
//         if (expectedException != null) {
//             assertThrows(expectedException, () -> MapConversions.toCharacter(from, converter), "Expected exception for input: " + from);
//         } else {
//             Character result = MapConversions.toCharacter(from, converter);
//             assertEquals(expected, result, "Conversion result does not match for input: " + from);
//         }
//     }

    /* -------------------------------------------------------------
     *  Map handling
     * ------------------------------------------------------------- */
//     @ParameterizedTest(name = "[{index}] map conversion: from={0} => expected={1}")
//     @MethodSource("mapHandlingProvider")
//     void toCharacter_mapHandling(Map<String, Object> from, Character expected, Class<? extends Throwable> expectedException) {
//         if (expectedException != null) {
//             assertThrows(expectedException, () -> MapConversions.toCharacter(from, converter), "Expected exception for map: " + from);
//         } else {
//             Character result = MapConversions.toCharacter(from, converter);
//             assertEquals(expected, result, "Map conversion result does not match for map: " + from);
//         }
//     }

    /* -------------------------------------------------------------
     *  Unsupported / ambiguous types
     * ------------------------------------------------------------- */
//     @ParameterizedTest(name = "[{index}] unsupported type: from={0}")
//     @MethodSource("unsupportedTypesProvider")
//     void toCharacter_unsupportedTypes(Object from, Class<? extends Throwable> expectedException) {
//         assertThrows(expectedException, () -> MapConversions.toCharacter(from, converter), "Expected exception for unsupported input: " + from);
//     }

    /* -------------------------------------------------------------
     *  Null Converter handling
     * ------------------------------------------------------------- */
//     @Test
//     void toCharacter_nullConverterThrowsNPE() {
//         Map<String, Object> map = Collections.singletonMap("_v", "D");
//         MapConversions.toCharacter(map, null);
//     }

//     @Before
//     public void setUp() {
//         // The Converter class requires a ConverterOptions instance.
//         // Using a default‑constructed ConverterOptions provides the standard configuration.
//         converter = new Converter(new ConverterOptions());
//     }

//     @After
//     public void tearDown() {
//         // Release reference – helps GC when many tests are added later.
//         converter = null;
//     }

//     @Test
//     public void toCharacter_fromIntArraySingleElement() {
//         int[] src = { 66 };
//         Character result = MapConversions.toCharacter(src, converter);
//         assertEquals(Character.valueOf('B'), result);
//     }

//     @Test
//     public void toCharacter_fromCharArraySingleElement() {
//         char[] src = { 'C' };
//         Character result = MapConversions.toCharacter(src, converter);
//         assertEquals(Character.valueOf('C'), result);
//     }

//     @Test(expected = IllegalArgumentException.class)
//     public void toCharacter_stringTooLong() {
//         MapConversions.toCharacter("TooLong", converter);
//     }

//     @Test(expected = IllegalArgumentException.class)
//     public void toCharacter_intArrayEmpty() {
//         MapConversions.toCharacter(new int[] {}, converter);
//     }

//     @Test(expected = IllegalArgumentException.class)
//     public void toCharacter_charArrayEmpty() {
//         MapConversions.toCharacter(new char[] {}, converter);
//     }

//     @Test(expected = IllegalArgumentException.class)
//     public void toCharacter_intArrayMultipleElements() {
//         MapConversions.toCharacter(new int[] { 65, 66 }, converter);
//     }

//     @Test(expected = IllegalArgumentException.class)
//     public void toCharacter_charArrayMultipleElements() {
//         MapConversions.toCharacter(new char[] { 'X', 'Y' }, converter);
//     }

//     @Test(expected = IllegalArgumentException.class)
//     public void toCharacter_unsupportedObject() {
//         MapConversions.toCharacter(new Object(), converter);
//     }

//     @Test(expected = IllegalArgumentException.class)
//     public void toCharacter_mapWithWrongKey() {
//         Map<String, Object> map = Collections.singletonMap("other", "H");
//         MapConversions.toCharacter(map, converter);
//     }

//     @Test(expected = IllegalArgumentException.class)
//     public void toCharacter_mapWithLongStringValue() {
//         Map<String, Object> map = Collections.singletonMap("value", "TooLong");
//         MapConversions.toCharacter(map, converter);
//     }

//     @Test(expected = IllegalArgumentException.class)
//     public void toCharacter_mapWithUnsupportedArray() {
//         Map<String, Object> map = Collections.singletonMap("_v", new int[] { 67 });
//         MapConversions.toCharacter(map, converter);
//     }
}