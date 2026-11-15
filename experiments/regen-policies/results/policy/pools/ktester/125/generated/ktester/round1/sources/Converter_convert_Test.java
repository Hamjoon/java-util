package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convert_Test {

    /**
     * The instance under test.
     */
    private Converter converter;

    class SimplePojo {

        final String value;

        SimplePojo(String v) {
            this.value = v;
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof SimplePojo) && ((SimplePojo) o).value.equals(this.value);
        }
    }

    class MyClass {

        private final String value;

        MyClass(String v) {
            this.value = v;
        }

        String getValue() {
            return value;
        }
    }

    class MyClass {

        private final String value;

        MyClass(String v) {
            this.value = v;
        }

        String getValue() {
            return value;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Each test receives a fresh Converter with default options.
        converter = new Converter(defaultOptions());
        converter = new Converter(defaultOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // The Converter does not hold external resources, but we clear the reference
        // to help the garbage collector and to make the intent explicit.
        converter = null;
        converter = null;
    }

    /**
     * Simple {@link ConverterOptions} implementation used for all tests.
     */
    private static ConverterOptions defaultOptions() {
        return new ConverterOptions() {

            @Override
            public <T> T getCustomOption(String name) {
                return null;
            }
        };
    }

    /**
     * Inserts the internal {@code UNSUPPORTED} marker into the user‑conversion map
     * for the supplied source/target pair.  This allows us to test the fallback
     * to the factory conversion when a user conversion is explicitly marked as
     * unsupported.
     */
    private void registerUnsupportedUserConversion(Class<?> source, Class<?> target) throws Exception {
        // Access the private static final UNSUPPORTED field
        Field unsupportedField = Converter.class.getDeclaredField("UNSUPPORTED");
        unsupportedField.setAccessible(true);
        Object unsupported = unsupportedField.get(null);
        // Access the private USER_DB field (instance field)
        Field userDbField = Converter.class.getDeclaredField("USER_DB");
        userDbField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<ConversionPair, Convert<?>> userDb = (Map<ConversionPair, Convert<?>>) userDbField.get(converter);
        // Build the key and put the UNSUPPORTED marker
        Method pairMethod = Converter.class.getDeclaredMethod("pair", Class.class, Class.class);
        pairMethod.setAccessible(true);
        Object key = pairMethod.invoke(null, source, target);
        userDb.put((ConversionPair) key, (Convert<?>) unsupported);
    }

    private static Stream<Arguments> primitiveStringConversions() {
        return Stream.of(Arguments.of("456", int.class, 456), Arguments.of("123", Integer.class, 123));
    }

    private static Stream<Arguments> collectionConversionData() {
        return Stream.of(// List -> Set
        Arguments.of(List.of(1, 2, 3), Set.class, Set.of(1, 2, 3)), // Primitive int[] -> List<Integer>
        Arguments.of(new int[] { 1, 2, 3 }, List.class, List.of(1, 2, 3)), // HashSet<String> -> SortedSet<String> (TreeSet)
        Arguments.of(new HashSet<>(Arrays.asList("b", "a")), SortedSet.class, new TreeSet<>(Arrays.asList("a", "b"))));
    }

    @Test
    void convert_null_to_object_returnsNull() {
        String result = converter.convert(null, String.class);
        assertNull(result, "Converting null to a reference type should return null");
    }

    @Test
    void convert_null_to_primitiveInt_returnsZero() {
        int result = converter.convert(null, int.class);
        assertEquals(0, result, "Converting null to int should return 0");
    }

    @Test
    void convert_null_to_primitiveBoolean_returnsFalse() {
        boolean result = converter.convert(null, boolean.class);
        assertFalse(result, "Converting null to boolean should return false");
    }

    @Test
    void convert_stringToInteger_successful() {
        Integer result = converter.convert("12345", Integer.class);
        assertEquals(12345, result);
    }

    @Test
    void convert_stringToBigDecimal_successful() {
        BigDecimal result = converter.convert("12.34", BigDecimal.class);
        assertEquals(new BigDecimal("12.34"), result);
    }

    @Test
    void convert_stringToLocalDate_successful() {
        LocalDate expected = LocalDate.of(2023, 3, 14);
        LocalDate result = converter.convert("2023-03-14", LocalDate.class);
        assertEquals(expected, result);
    }

    @Test
    void convert_byteArrayToByteBuffer_successful() {
        byte[] bytes = new byte[] { 1, 2, 3 };
        ByteBuffer buffer = converter.convert(bytes, ByteBuffer.class);
        assertArrayEquals(bytes, buffer.array());
    }

    @Test
    void convert_charArrayToCharBuffer_successful() {
        char[] chars = new char[] { 'a', 'b', 'c' };
        CharBuffer buffer = converter.convert(chars, CharBuffer.class);
        assertEquals(CharBuffer.wrap(chars), buffer);
    }

    @Test
    void convert_mapWithValueKeyToDouble_successful() {
        Map<String, Object> map = Map.of(Converter.VALUE, "75.5");
        Double result = converter.convert(map, double.class);
        assertEquals(75.5, result);
    }

    @Test
    void convert_unsupportedConversion_throwsException() {
        // Attempt to convert a UUID to an unsupported target type (e.g., Thread)
        UUID uuid = UUID.randomUUID();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> converter.convert(uuid, Thread.class));
        assertTrue(ex.getMessage().contains("Unsupported conversion"));
    }

    @Test
    void convert_toEnumSet_requiresEnumClass_throwsException() {
        // The Converter expects the enum class to be supplied when converting to EnumSet.
        // Passing EnumSet.class directly should trigger the specific IllegalArgumentException.
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> converter.convert(new String[] { "A", "B" }, EnumSet.class));
        assertTrue(ex.getMessage().contains("To convert to EnumSet, specify the Enum class"));
    }

    @Test
    void convert_customAddedConversion_isUsed() {
        // Register a custom conversion from String to a simple POJO.
        class SimplePojo {

            final String value;

            SimplePojo(String v) {
                this.value = v;
            }

            @Override
            public boolean equals(Object o) {
                return (o instanceof SimplePojo) && ((SimplePojo) o).value.equals(this.value);
            }
        }
        converter.addConversion(String.class, SimplePojo.class, (from, conv, target) -> new SimplePojo((String) from));
        SimplePojo result = converter.convert("hello", SimplePojo.class);
        assertEquals(new SimplePojo("hello"), result);
    }

    @Test
    void convert_nullTargetType_throwsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> converter.convert("anything", null));
        assertEquals("toType cannot be null", ex.getMessage());
    }

    @Test
    void convert_nullSourceToPrimitiveInt_returnsZero() {
        int result = converter.convert(null, int.class);
        assertEquals(0, result);
    }

    @Test
    void convert_nullSourceToPrimitiveBoolean_returnsFalse() {
        boolean result = converter.convert(null, boolean.class);
        assertFalse(result);
    }

    @Test
    void convert_stringToInteger_factoryConversion() {
        Integer result = converter.convert("123", Integer.class);
        assertEquals(123, result);
    }

    @Test
    void convert_stringToInteger_userConversionOverridesFactory() {
        // User conversion returns the length of the string ("custom".length() == 6)
        converter.addConversion(String.class, Integer.class, (from, conv, target) -> ((String) from).length());
        Integer result = converter.convert("custom", Integer.class);
        assertEquals(6, result);
    }

    @Test
    void convert_stringToInteger_userConversionUnsupportedFallsBackToFactory() throws Exception {
        // Register a user conversion that is explicitly marked as UNSUPPORTED
        registerUnsupportedUserConversion(String.class, Integer.class);
        // Factory conversion should still work and return the parsed integer
        Integer result = converter.convert("123", Integer.class);
        assertEquals(123, result);
    }

    @Test
    void convert_toEnumSetWithoutEnumClass_throwsSpecificException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> converter.convert(new String[] { "A", "B" }, EnumSet.class));
        assertTrue(ex.getMessage().contains("To convert to EnumSet, specify the Enum class"));
    }

    @Test
    void convert_timestampToLong_inheritedConversion() {
        Timestamp ts = Timestamp.from(Instant.parse("2023-01-01T00:00:00Z"));
        Long result = converter.convert(ts, Long.class);
        assertEquals(1672531200000L, result);
    }

    @Test
    void convert_unsupportedSourceToString_throwsIllegalArgumentException() {
        Object src = new Object();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> converter.convert(src, String.class));
        assertTrue(ex.getMessage().contains("Unsupported conversion"));
        assertTrue(ex.getMessage().contains("source type [java.lang.Object]"));
        assertTrue(ex.getMessage().contains("target type 'String'"));
    }

    @ParameterizedTest(name = "String \"{0}\" to primitive {1} => {2}")
    @MethodSource("primitiveStringConversions")
    void convert_stringToPrimitiveNumber(String source, Class<?> target, Number expected) {
        Object result = converter.convert(source, target);
        assertEquals(expected, result);
    }

    @Test
    void convert_nullSourceToWrapperInteger_throwsUnsupportedException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> converter.convert(null, Integer.class));
        assertTrue(ex.getMessage().contains("Unsupported conversion"));
        assertTrue(ex.getMessage().contains("source type [void]"));
        assertTrue(ex.getMessage().contains("target type 'Integer'"));
    }

    @ParameterizedTest(name = "Collection conversion: {0} -> {1}")
    @MethodSource("collectionConversionData")
    void convert_collectionConversions(Object source, Class<?> target, Object expected) {
        Object result = converter.convert(source, target);
        assertEquals(expected, result);
    }

    @Test
    void convert_stringTrueToBoolean_factoryConversion() {
        Boolean result = converter.convert("true", Boolean.class);
        assertTrue(result);
    }

    @Test
    void convert_sqlDateToLong_inheritedConversion() {
        java.sql.Date sqlDate = new java.sql.Date(1672531200000L);
        Long result = converter.convert(sqlDate, Long.class);
        assertEquals(1672531200000L, result);
    }

    @Test
    void convert_customClassWithUserConversion() {
        // Simple custom class used only for the test
        class MyClass {

            private final String value;

            MyClass(String v) {
                this.value = v;
            }

            String getValue() {
                return value;
            }
        }
        // Register user conversion: MyClass -> String
        converter.addConversion(MyClass.class, String.class, (from, conv, target) -> "converted:" + ((MyClass) from).getValue());
        MyClass src = new MyClass("test");
        String result = converter.convert(src, String.class);
        assertEquals("converted:test", result);
    }

    @Test
    void convert_hashMapToList_unsupportedConversion() {
        Map<String, String> map = new HashMap<>();
        map.put("a", "b");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> converter.convert(map, List.class));
        assertTrue(ex.getMessage().contains("Unsupported conversion"));
    }

    @Test
    void convert_invalidLocalDateString_throwsDateTimeParseException() {
        assertThrows(DateTimeParseException.class, () -> converter.convert("2021-13-01", LocalDate.class));
    }

    @Test
    void convert_invalidUuidString_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> converter.convert("not-a-uuid", UUID.class));
    }

    @Test
    void convert_nullSourceToEnumSet_throwsSpecificException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> converter.convert(null, EnumSet.class));
        assertTrue(ex.getMessage().contains("To convert to EnumSet, specify the Enum class"));
    }
}
