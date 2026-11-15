package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import java.net.URL;
import java.security.AccessControlException;
import java.util.*;

class StringConversions_toClass_Test {

    private Converter converter;

    /**
     * Helper class used for the “custom class‑loader” test case.
     */
    public static class TestHelper {
    }

    @BeforeEach
    void setupBeforeEach() {
        // ConverterOptions has a public no‑arg constructor in the library.
        // Using the default options is sufficient for the tests.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @Test
    void toClass_validFullyQualifiedName_returnsClass() {
        // given
        String className = "java.lang.String";
        // when
        Class<?> result = StringConversions.toClass(className, converter);
        // then
        assertEquals(String.class, result);
    }

    @Test
    void toClass_nameWithWhitespace_isTrimmedAndReturnsClass() {
        // given
        String className = "   java.util.ArrayList   ";
        // when
        Class<?> result = StringConversions.toClass(className, converter);
        // then
        assertEquals(java.util.ArrayList.class, result);
    }

    @Test
    void toClass_unknownClass_throwsIllegalArgumentException() {
        // given
        String className = "com.nonexistent.FooBar";
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toClass(className, converter));
        assertTrue(ex.getMessage().contains("Cannot convert String"));
        assertTrue(ex.getMessage().contains(className.trim()));
    }

    @Test
    void toClass_trimNewlineAndTab_returnsClass() {
        // given
        String className = "\njava.math.BigDecimal\t";
        // when
        Class<?> result = StringConversions.toClass(className, converter);
        // then
        assertEquals(java.math.BigDecimal.class, result);
    }

    @Test
    void toClass_arrayDescriptor_returnsArrayClass() {
        // given
        String className = "[Ljava.lang.String;";
        // when
        Class<?> result = StringConversions.toClass(className, converter);
        // then
        assertEquals(String[].class, result);
    }

    @Test
    void toClass_customClassLoader_loadsTestHelper() {
        // given
        String className = "com.cedarsoftware.util.convert.StringConversions_toClass_Test$TestHelper";
        // a mock ClassLoader that can load the nested TestHelper class
        ClassLoader mockLoader = mock(ClassLoader.class);
        try {
            when(mockLoader.loadClass(className)).thenReturn(TestHelper.class);
        } catch (ClassNotFoundException e) {
            fail("Mock setup failed", e);
        }
        // mock ConverterOptions to return the mock loader
        ConverterOptions mockOptions = mock(ConverterOptions.class);
        when(mockOptions.getClassLoader()).thenReturn(mockLoader);
        // mock Converter to return the mock options
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        // mock ClassUtilities.forName to delegate to the mock loader
        try (MockedStatic<ClassUtilities> utilities = Mockito.mockStatic(ClassUtilities.class)) {
            utilities.when(() -> ClassUtilities.forName(className, mockLoader)).thenAnswer(inv -> {
                try {
                    return Class.forName(className, false, mockLoader);
                } catch (ClassNotFoundException e) {
                    return null;
                }
            });
            // when
            Class<?> result = StringConversions.toClass(className, mockConverter);
            // then
            assertEquals(TestHelper.class, result);
        }
    }

    @Test
    void toClass_nullFrom_throwsNullPointerException() {
        // given
        Object from = null;
        // when / then
        assertThrows(NullPointerException.class, () -> StringConversions.toClass(from, converter));
    }

    @Test
    void toClass_nonStringFrom_throwsClassCastException() {
        // given
        // Integer, not a String
        Object from = 123;
        // when / then
        assertThrows(ClassCastException.class, () -> StringConversions.toClass(from, converter));
    }

    @ParameterizedTest
    @CsvSource({ "'', IllegalArgumentException", "'   \\t\\n  ', IllegalArgumentException" })
    void toClass_emptyOrWhitespaceOnlyString_throwsIllegalArgumentException(String input, String expectedException) {
        // given
        try (MockedStatic<ClassUtilities> utilities = Mockito.mockStatic(ClassUtilities.class)) {
            // ClassUtilities.forName will return null for an empty string
            utilities.when(() -> ClassUtilities.forName("", converter.getOptions().getClassLoader())).thenReturn(null);
            utilities.when(() -> ClassUtilities.forName(Mockito.anyString(), Mockito.any())).thenAnswer(inv -> {
                String name = inv.getArgument(0);
                ClassLoader cl = inv.getArgument(1);
                try {
                    return Class.forName(name, false, cl);
                } catch (ClassNotFoundException e) {
                    return null;
                }
            });
            // when / then
            assertThrows(IllegalArgumentException.class, () -> StringConversions.toClass(input, converter));
        }
    }

    @Test
    void toClass_nonExistentClass_throwsIllegalArgumentException() {
        // given
        String className = "com.example.DoesNotExist";
        try (MockedStatic<ClassUtilities> utilities = Mockito.mockStatic(ClassUtilities.class)) {
            utilities.when(() -> ClassUtilities.forName(className, converter.getOptions().getClassLoader())).thenReturn(// simulate not found
            null);
            // when / then
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toClass(className, converter));
            assertTrue(ex.getMessage().contains("Cannot convert String"));
            assertTrue(ex.getMessage().contains(className));
        }
    }

    @Test
    void toClass_primitiveName_throwsIllegalArgumentException() {
        // given
        String className = "int";
        try (MockedStatic<ClassUtilities> utilities = Mockito.mockStatic(ClassUtilities.class)) {
            utilities.when(() -> ClassUtilities.forName(className, converter.getOptions().getClassLoader())).thenReturn(// primitive names are not resolved
            null);
            // when / then
            assertThrows(IllegalArgumentException.class, () -> StringConversions.toClass(className, converter));
        }
    }

    @Test
    void toClass_nullConverter_throwsNullPointerException() {
        // given
        String className = "java.lang.String";
        Converter nullConverter = null;
        // when / then
        assertThrows(NullPointerException.class, () -> StringConversions.toClass(className, nullConverter));
    }

    @Test
    void toClass_converterWithNullOptions_throwsNullPointerException() {
        // given
        String className = "java.util.HashMap";
        Converter mockConverter = mock(Converter.class);
        // options are null
        when(mockConverter.getOptions()).thenReturn(null);
        // when / then
        assertThrows(NullPointerException.class, () -> StringConversions.toClass(className, mockConverter));
    }

    @Test
    void toClass_converterWithNullClassLoader_throwsNullPointerException() {
        // given
        String className = "java.util.HashSet";
        ConverterOptions mockOptions = mock(ConverterOptions.class);
        // class loader is null
        when(mockOptions.getClassLoader()).thenReturn(null);
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        try (MockedStatic<ClassUtilities> utilities = Mockito.mockStatic(ClassUtilities.class)) {
            // The real implementation would receive a null loader and likely throw NPE.
            utilities.when(() -> ClassUtilities.forName(className, null)).thenThrow(new NullPointerException("ClassLoader is null"));
            // when / then
            assertThrows(NullPointerException.class, () -> StringConversions.toClass(className, mockConverter));
        }
    }

    @Test
    void toClass_classLoaderThrowsSecurityException_propagatesException() {
        // given
        String className = "java.lang.System";
        ClassLoader mockLoader = mock(ClassLoader.class);
        try {
            when(mockLoader.loadClass(className)).thenThrow(new SecurityException("Forbidden"));
        } catch (ClassNotFoundException e) {
            fail("Mock setup failed", e);
        }
        ConverterOptions mockOptions = mock(ConverterOptions.class);
        when(mockOptions.getClassLoader()).thenReturn(mockLoader);
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        try (MockedStatic<ClassUtilities> utilities = Mockito.mockStatic(ClassUtilities.class)) {
            utilities.when(() -> ClassUtilities.forName(className, mockLoader)).thenAnswer(inv -> {
                // delegate to the mocked loader, which throws SecurityException
                return mockLoader.loadClass(className);
            });
            // when / then
            assertThrows(SecurityException.class, () -> StringConversions.toClass(className, mockConverter));
        }
    }
}
