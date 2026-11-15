package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringConversions_toClass_Test {

    @Mock
    private Converter mockConverter;

    @Mock
    private ConverterOptions mockOptions;

    /**
     * Initialise the mock hierarchy before each test.
     */
    @BeforeEach
    void setupBeforeEach() {
        // The Converter#getOptions() call must return a ConverterOptions
        // whose getClassLoader() returns a usable ClassLoader.
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getClassLoader()).thenReturn(ClassLoader.getSystemClassLoader());
    }

    /* -------------------------------------------------------------
     *  Successful conversion test cases (parameterised)
     * ------------------------------------------------------------- */
    private static Stream<Arguments> successfulConversionProvider() {
        return Stream.of(// leading / trailing whitespace
        Arguments.of("   java.lang.String   ", String.class), // exact name
        Arguments.of("java.util.ArrayList", java.util.ArrayList.class), // tab / newline characters
        Arguments.of("\tjava.lang.Integer\n", Integer.class), // array class internal name
        Arguments.of("[Ljava.lang.String;", String[].class), // inner class using '$'
        Arguments.of("java.util.Map$Entry", java.util.Map.Entry.class), // null class loader – still resolves
        Arguments.of("java.lang.Long", Long.class));
    }

    /* -------------------------------------------------------------
     *  Failure conversion – class not found (parameterised)
     * ------------------------------------------------------------- */
    private static Stream<Arguments> classNotFoundProvider() {
        return Stream.of(// completely unknown class
        Arguments.of("com.example.DoesNotExist"), // empty string after trim
        Arguments.of(""), // whitespace‑only string
        Arguments.of("   \t\n  "), // Unicode EM SPACE is not trimmed
        Arguments.of("\u2003java.lang.String\u2003"), // primitive type name (int)
        Arguments.of("int"));
    }

    @Test
    void toClass_validClassName_returnsClass() {
        // given
        String className = "java.lang.String";
        // when
        Class<?> result = StringConversions.toClass(className, mockConverter);
        // then
        assertNotNull(result);
        assertEquals(String.class, result);
    }

    @Test
    void toClass_validClassNameWithWhitespace_isTrimmedAndReturned() {
        // given
        String classNameWithSpaces = "   java.lang.Integer   ";
        // when
        Class<?> result = StringConversions.toClass(classNameWithSpaces, mockConverter);
        // then
        assertNotNull(result);
        assertEquals(Integer.class, result);
    }

    @Test
    void toClass_unknownClass_throwsIllegalArgumentException() {
        // given
        String unknownClass = "com.example.DoesNotExist";
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toClass(unknownClass, mockConverter));
        assertTrue(ex.getMessage().contains("Cannot convert String"));
        assertTrue(ex.getMessage().contains(unknownClass));
    }

    @ParameterizedTest(name = "[{index}] ''{0}'' → {1}")
    @MethodSource("successfulConversionProvider")
    void toClass_successfulConversions(String input, Class<?> expected) {
        // For the "null class loader" case we need to override the default mock behaviour.
        if ("java.lang.Long".equals(input.trim())) {
            // Simulate a null ClassLoader in the options.
            when(mockOptions.getClassLoader()).thenReturn(null);
        }
        Class<?> result = StringConversions.toClass(input, mockConverter);
        assertNotNull(result);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] ''{0}'' → IllegalArgumentException")
    @MethodSource("classNotFoundProvider")
    void toClass_classNotFound_throwsIllegalArgumentException(String input) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toClass(input, mockConverter));
        // The message always starts with "Cannot convert String '<trimmed>' to class"
        assertTrue(ex.getMessage().contains("Cannot convert String"));
        // Ensure the original (trimmed) representation appears in the message.
        String trimmed = input.trim();
        assertTrue(ex.getMessage().contains("'" + trimmed + "'"));
    }

    /* -------------------------------------------------------------
     *  Null / invalid source arguments
     * ------------------------------------------------------------- */
    @Test
    void toClass_nullSource_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> StringConversions.toClass(null, mockConverter));
    }

    @Test
    void toClass_nonStringSource_throwsClassCastException() {
        assertThrows(ClassCastException.class, () -> StringConversions.toClass(12345, mockConverter));
    }

    /* -------------------------------------------------------------
     *  Null converter / null options handling
     * ------------------------------------------------------------- */
    @Test
    void toClass_nullConverter_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> StringConversions.toClass("java.lang.String", null));
    }

    @Test
    void toClass_nullOptionsInConverter_throwsNullPointerException() {
        // Configure the mock to return null for getOptions()
        when(mockConverter.getOptions()).thenReturn(null);
        assertThrows(NullPointerException.class, () -> StringConversions.toClass("java.lang.String", mockConverter));
    }
}
