package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.util.stream.Stream;

class StringConversions_toClass_Test {

    private Converter converter;

    // a Converter that uses the system class loader
    private Converter defaultConverter;

    // Simple inner class used for the custom‑loader test case.
    static class InnerTestClass {
    }

    private static class DelegatingClassLoader extends ClassLoader {

        DelegatingClassLoader(ClassLoader parent) {
            super(parent);
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            // Delegate to parent – this loader can still load any class visible to the parent.
            return super.loadClass(name);
        }
    }

    private static class SecurityExceptionClassLoader extends ClassLoader {

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            throw new SecurityException("Access denied for class: " + name);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Build a real Converter that uses the default (system) class loader.
        // The actual implementation of Converter is not needed – we only need a non‑null instance.
        // default options → default class loader
        ConverterOptions options = new ConverterOptions();
        defaultConverter = new Converter(options);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up; the Converter does not hold external resources that need explicit release
        converter = null;
    }

    /**
     * Creates a mock {@link Converter} whose {@link ConverterOptions#getClassLoader()} returns {@code loader}.
     */
    private static Converter mockConverterWithLoader(ClassLoader loader) {
        ConverterOptions options = mock(ConverterOptions.class);
        when(options.getClassLoader()).thenReturn(loader);
        Converter converter = mock(Converter.class);
        when(converter.getOptions()).thenReturn(options);
        return converter;
    }

    /**
     * Mock Converter whose {@code getOptions()} returns {@code null}.
     */
    private static Converter mockConverterWithNullOptions() {
        Converter converter = mock(Converter.class);
        when(converter.getOptions()).thenReturn(null);
        return converter;
    }

    /**
     * Mock Converter whose options return a {@code null} class loader.
     */
    private static Converter mockConverterWithNullClassLoader() {
        ConverterOptions options = mock(ConverterOptions.class);
        when(options.getClassLoader()).thenReturn(null);
        Converter converter = mock(Converter.class);
        when(converter.getOptions()).thenReturn(options);
        return converter;
    }

    private static Stream<Arguments> successfulConversionProvider() {
        return Stream.of(// exact name, no whitespace
        Arguments.of("java.lang.String", String.class), // surrounding whitespace must be trimmed
        Arguments.of("  java.util.Date  ", java.util.Date.class), // inner class using '$' separator
        Arguments.of("java.util.Map$Entry", java.util.Map.Entry.class));
    }

    private static Stream<Arguments> invalidInputProvider() {
        return Stream.of(// null from value → NullPointerException when .trim() is invoked
        Arguments.of(null, NullPointerException.class, "null from value"), // non‑String from value → ClassCastException during cast
        Arguments.of(12345, ClassCastException.class, "non‑String from value"), // empty string after trim → IllegalArgumentException (class not found)
        Arguments.of("", IllegalArgumentException.class, "empty string"), // whitespace only → IllegalArgumentException (empty after trim)
        Arguments.of("   ", IllegalArgumentException.class, "whitespace only string"), // non‑existent class name → IllegalArgumentException
        Arguments.of("com.nonexistent.Foo", IllegalArgumentException.class, "non‑existent class"), // array syntax not accepted by Class.forName → IllegalArgumentException
        Arguments.of("java.lang.String[]", IllegalArgumentException.class, "array class name"), // primitive type name → IllegalArgumentException
        Arguments.of("int", IllegalArgumentException.class, "primitive type name"));
    }

    @Test
    void toClass_validClassName_returnsClass() {
        // given
        String className = "java.lang.String";
        // when
        Class<?> result = StringConversions.toClass(className, defaultConverter);
        // then
        assertNotNull(result);
        assertEquals(String.class, result);
    }

    @Test
    void toClass_validClassNameWithWhitespace_isTrimmedAndReturned() {
        // given
        String className = "  java.lang.Integer  ";
        // when
        Class<?> result = StringConversions.toClass(className, defaultConverter);
        // then
        assertNotNull(result);
        assertEquals(Integer.class, result);
    }

    @Test
    void toClass_unknownClass_throwsIllegalArgumentException() {
        // given
        String className = "com.this.does.not.Exist";
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toClass(className, defaultConverter));
        assertTrue(ex.getMessage().contains("Cannot convert String"));
        assertTrue(ex.getMessage().contains(className.trim()));
    }

    @ParameterizedTest(name = "[{index}] from=''{0}'' → {1}")
    @MethodSource("successfulConversionProvider")
    void toClass_successfulConversion(String from, Class<?> expected) {
        // The default converter uses the system class loader, which can load all standard JDK classes.
        Class<?> result = StringConversions.toClass(from, defaultConverter);
        assertNotNull(result);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] from=''{0}'' expects {1}")
    @MethodSource("invalidInputProvider")
    void toClass_invalidInputs(Object from, Class<? extends Throwable> expectedException, String description) {
        // Use the default converter – the failure originates from the input, not the class loader.
        Throwable ex = assertThrows(expectedException, () -> StringConversions.toClass(from, defaultConverter), "Expected exception for case: " + description);
        // For IllegalArgumentException we also verify that the message mentions the trimmed value.
        if (expectedException == IllegalArgumentException.class && from != null) {
            String trimmed = from.toString().trim();
            assertTrue(ex.getMessage().contains(trimmed), "Exception message should contain the trimmed class name");
        }
    }

    @Test
    void toClass_nullConverter_throwsNullPointerException() {
        String from = "java.lang.String";
        assertThrows(NullPointerException.class, () -> StringConversions.toClass(from, null), "Calling toClass with a null converter should NPE when accessing getOptions()");
    }

    @Test
    void toClass_converterWithNullOptions_throwsNullPointerException() {
        String from = "java.util.Date";
        Converter converter = mockConverterWithNullOptions();
        assertThrows(NullPointerException.class, () -> StringConversions.toClass(from, converter), "Converter returning null options should cause NPE when accessing getClassLoader()");
    }

    @Test
    void toClass_converterWithNullClassLoader_throwsIllegalArgumentException() {
        String from = "java.util.ArrayList";
        Converter converter = mockConverterWithNullClassLoader();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toClass(from, converter));
        // The message should contain the original (trimmed) class name.
        assertTrue(ex.getMessage().contains("ArrayList"), "Exception message must mention the class name that could not be found");
    }

    @Test
    void toClass_customClassLoader_loadsInnerTestClass() {
        // "com.cedarsoftware.util.convert.StringConversions_toClass_Test$InnerTestClass"
        String className = StringConversions_toClass_Test.InnerTestClass.class.getName();
        ClassLoader customLoader = new DelegatingClassLoader(this.getClass().getClassLoader());
        Converter converter = mockConverterWithLoader(customLoader);
        Class<?> result = StringConversions.toClass(className, converter);
        assertNotNull(result);
        assertEquals(StringConversions_toClass_Test.InnerTestClass.class, result);
    }

    @Test
    void toClass_classLoaderThrowsSecurityException_propagatesException() {
        String className = "java.lang.System";
        ClassLoader securityLoader = new SecurityExceptionClassLoader();
        Converter converter = mockConverterWithLoader(securityLoader);
        SecurityException ex = assertThrows(SecurityException.class, () -> StringConversions.toClass(className, converter));
        assertTrue(ex.getMessage().contains("System"), "SecurityException message should contain the attempted class name");
    }
}
