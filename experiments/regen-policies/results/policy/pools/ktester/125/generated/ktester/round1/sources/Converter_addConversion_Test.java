package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.mockito.Mockito.*;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class Converter_addConversion_Test {

    private Converter converter;

    /**
     * Simple {@link ConverterOptions} implementation – all default methods are sufficient for the tests.
     */
    private static final ConverterOptions DEFAULT_OPTIONS = new ConverterOptions() {
    };

    @BeforeEach
    void setupBeforeEach() {
        converter = new Converter(DEFAULT_OPTIONS);
    }

    private static Stream<Arguments> nullConversionFunctionProvider() {
        return Stream.of(Arguments.of(Integer.class, String.class), Arguments.of(int[].class, String.class), Arguments.of(null, String.class), Arguments.of(String.class, null));
    }

    /**
     * Verify that a conversion added via {@code addConversion} is later invoked by {@code convert}.
     */
    @Test
    void addConversion_isUsedByConvert() {
        // GIVEN
        @SuppressWarnings("unchecked")
        Convert<Integer> mockConvert = mock(Convert.class);
        when(mockConvert.convert("123", converter, Integer.class)).thenReturn(123);
        // WHEN
        converter.addConversion(String.class, Integer.class, mockConvert);
        Integer result = converter.convert("123", Integer.class);
        // THEN
        assertEquals(123, result);
        verify(mockConvert, times(1)).convert("123", converter, Integer.class);
    }

    /**
     * Verify that adding a conversion for an already‑registered source/target pair returns the
     * previously‑registered {@link Convert} instance.
     */
    @Test
    void addConversion_returnsPreviousConversion() {
        // GIVEN
        @SuppressWarnings("unchecked")
        Convert<Integer> first = mock(Convert.class);
        @SuppressWarnings("unchecked")
        Convert<Integer> second = mock(Convert.class);
        when(first.convert(any(), any(), any())).thenReturn(1);
        when(second.convert(any(), any(), any())).thenReturn(2);
        // Register the first conversion.
        converter.addConversion(String.class, Integer.class, first);
        // WHEN – register a second conversion for the same pair.
        Convert<?> returned = converter.addConversion(String.class, Integer.class, second);
        // THEN – the method should return the first conversion.
        assertSame(first, returned);
        // And the second conversion should now be used.
        Integer result = converter.convert("any", Integer.class);
        assertEquals(2, result);
        verify(second, times(1)).convert("any", converter, Integer.class);
        verify(first, never()).convert(any(), any(), any());
    }

    /**
     * Verify that primitive source/target classes are automatically converted to their wrapper
     * equivalents before being stored in the internal map.
     */
    @Test
    void addConversion_handlesPrimitiveTypes() {
        // GIVEN – use primitive classes as arguments.
        @SuppressWarnings("unchecked")
        Convert<Long> mockConvert = mock(Convert.class);
        when(mockConvert.convert(42, converter, Long.class)).thenReturn(42L);
        // WHEN – add conversion using primitive types.
        converter.addConversion(int.class, long.class, mockConvert);
        // THEN – converting an {@link Integer} to {@link Long} should hit the mock.
        Long result = converter.convert(42, Long.class);
        assertEquals(42L, result);
        verify(mockConvert, times(1)).convert(42, converter, Long.class);
    }

    @Test
    @DisplayName("Add conversion int → String (primitive source normalized to Integer)")
    void addConversion_primitiveSource_normalizedAndUsed() {
        // GIVEN – a simple Convert that turns an Integer into its String representation
        Convert<String> intToString = new Convert<String>() {

            @Override
            public String convert(Object from, Converter converter, Class<String> toClass) {
                return ((Integer) from).toString();
            }
        };
        // WHEN – register conversion using primitive source type
        Convert<?> previous = converter.addConversion(int.class, String.class, intToString);
        // THEN – no previous conversion, and the conversion works via convert()
        assertNull(previous, "First registration should return null");
        String result = converter.convert(42, String.class);
        assertEquals("42", result);
    }

    @Test
    @DisplayName("Replace existing conversion for Integer → String and obtain previous instance")
    void addConversion_replaceExisting_returnsPrevious() {
        // FIRST registration
        Convert<String> first = new Convert<String>() {

            @Override
            public String convert(Object from, Converter converter, Class<String> toClass) {
                return ((Integer) from).toString();
            }
        };
        converter.addConversion(Integer.class, String.class, first);
        // SECOND registration (different implementation)
        Convert<String> second = new Convert<String>() {

            @Override
            public String convert(Object from, Converter converter, Class<String> toClass) {
                return "num:" + from;
            }
        };
        Convert<?> returned = converter.addConversion(Integer.class, String.class, second);
        // EXPECTED – the first conversion is returned
        assertSame(first, returned, "addConversion should return the previously registered Convert");
        // AND – the new conversion is now used by convert()
        String out = converter.convert(7, String.class);
        assertEquals("num:7", out);
    }

    @Test
    @DisplayName("Primitive char source is normalized to Character wrapper")
    void addConversion_primitiveChar_normalized() {
        Convert<Character> charConvert = new Convert<Character>() {

            @Override
            public Character convert(Object from, Converter converter, Class<Character> toClass) {
                return (Character) from;
            }
        };
        converter.addConversion(char.class, Character.class, charConvert);
        Character result = converter.convert('x', Character.class);
        assertEquals('x', result);
    }

    @Test
    @DisplayName("Null source class causes NullPointerException")
    void addConversion_nullSource_throwsNPE() {
        Convert<String> dummy = new Convert<String>() {

            @Override
            public String convert(Object from, Converter converter, Class<String> toClass) {
                return "dummy";
            }
        };
        assertThrows(NullPointerException.class, () -> converter.addConversion(null, String.class, dummy), "Passing null as source should throw NPE");
    }

    @Test
    @DisplayName("Null target class causes NullPointerException")
    void addConversion_nullTarget_throwsNPE() {
        Convert<String> dummy = new Convert<String>() {

            @Override
            public String convert(Object from, Converter converter, Class<String> toClass) {
                return "dummy";
            }
        };
        assertThrows(NullPointerException.class, () -> converter.addConversion(String.class, null, dummy), "Passing null as target should throw NPE");
    }

    @Test
    @DisplayName("Null conversionFunction leads to NullPointerException (ConcurrentHashMap disallows null values)")
    void addConversion_nullConversionFunction_throwsNPE() {
        assertThrows(NullPointerException.class, () -> converter.addConversion(Integer.class, String.class, null), "Null conversionFunction must propagate NPE");
    }

    @Test
    @DisplayName("Add conversion Integer → int (target primitive normalized to Integer)")
    void addConversion_targetPrimitive_normalized() {
        Convert<Integer> identity = new Convert<Integer>() {

            @Override
            public Integer convert(Object from, Converter converter, Class<Integer> toClass) {
                return (Integer) from;
            }
        };
        Convert<?> previous = converter.addConversion(Integer.class, int.class, identity);
        assertNull(previous, "First registration should return null");
        Integer result = converter.convert(123, Integer.class);
        assertEquals(123, result);
    }

    @Test
    @DisplayName("Self‑conversion String → String (uppercase) and then replace with lowercase")
    void addConversion_selfConversion_replace() {
        // First conversion – uppercase
        Convert<String> upper = new Convert<String>() {

            @Override
            public String convert(Object from, Converter converter, Class<String> toClass) {
                return ((String) from).toUpperCase();
            }
        };
        converter.addConversion(String.class, String.class, upper);
        // Replace with lowercase conversion
        Convert<String> lower = new Convert<String>() {

            @Override
            public String convert(Object from, Converter converter, Class<String> toClass) {
                return ((String) from).toLowerCase();
            }
        };
        Convert<?> returned = converter.addConversion(String.class, String.class, lower);
        assertSame(upper, returned, "Previous self‑conversion should be returned");
        // Verify the new conversion is used
        String out = converter.convert("TeSt", String.class);
        assertEquals("test", out);
    }

    @Test
    @DisplayName("Conversion from List to Set")
    void addConversion_listToSet() {
        Convert<Set<?>> listToSet = new Convert<Set<?>>() {

            @Override
            public Set<?> convert(Object from, Converter converter, Class<Set<?>> toClass) {
                return new HashSet<>((List<?>) from);
            }
        };
        converter.addConversion(List.class, Set.class, listToSet);
        List<Integer> src = Arrays.asList(1, 2, 3, 2);
        Set<?> result = converter.convert(src, Set.class);
        assertEquals(new HashSet<>(src), result);
    }

    @Test
    @DisplayName("Conversion from int[] to String")
    void addConversion_intArrayToString() {
        Convert<String> arrayToString = new Convert<String>() {

            @Override
            public String convert(Object from, Converter converter, Class<String> toClass) {
                return Arrays.toString((int[]) from);
            }
        };
        converter.addConversion(int[].class, String.class, arrayToString);
        int[] arr = { 4, 5, 6 };
        String result = converter.convert(arr, String.class);
        assertEquals("[4, 5, 6]", result);
    }

    @Test
    @DisplayName("Conversion Double → double (target primitive normalized)")
    void addConversion_doubleToPrimitive() {
        Convert<Double> identity = new Convert<Double>() {

            @Override
            public Double convert(Object from, Converter converter, Class<Double> toClass) {
                return (Double) from;
            }
        };
        Convert<?> previous = converter.addConversion(Double.class, double.class, identity);
        assertNull(previous, "First registration should return null");
        Double result = converter.convert(3.14, Double.class);
        assertEquals(3.14, result);
    }

    @Test
    @DisplayName("Both source and target null cause NullPointerException")
    void addConversion_bothNull_throwsNPE() {
        Convert<Object> dummy = new Convert<Object>() {

            @Override
            public Object convert(Object from, Converter converter, Class<Object> toClass) {
                return from;
            }
        };
        assertThrows(NullPointerException.class, () -> converter.addConversion(null, null, dummy), "Both source and target null should throw NPE");
    }

    @ParameterizedTest(name = "[{index}] source={0}, target={1}")
    @MethodSource("nullConversionFunctionProvider")
    @DisplayName("addConversion with null conversionFunction should throw NullPointerException")
    void addConversion_nullConversionFunction_throws(Class<?> source, Class<?> target) {
        assertThrows(NullPointerException.class, () -> converter.addConversion(source, target, null), "Null conversionFunction must result in NPE");
    }
}
