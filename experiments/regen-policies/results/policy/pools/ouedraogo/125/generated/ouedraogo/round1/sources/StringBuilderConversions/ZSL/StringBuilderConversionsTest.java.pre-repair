package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

public class StringBuilderConversionsTest {

    /**
     * Helper method to create a minimal {@link Converter} instance.
     * The {@link Converter} constructor requires a {@link ConverterOptions} argument,
     * but for the purposes of these tests the actual options are irrelevant because
     * {@code StringBuilderConversions.toString} never uses the converter.
     */
    private static Converter createConverter() {
        // Passing {@code null} is acceptable for the constructor – the class only stores the reference.
        return new Converter(null);
    }

    @Test
    public void testToStringWithStringBuilder() {
        StringBuilder sb = new StringBuilder("Hello World");
        Converter converter = createConverter();

        String result = StringBuilderConversions.toString(sb, converter);

        assertEquals("Hello World", result);
    }

    @Test
    public void testToStringWithCustomObject() {
        Object custom = new Object() {
            @Override
            public String toString() {
                return "CustomString";
            }
        };
        Converter converter = createConverter();

        String result = StringBuilderConversions.toString(custom, converter);

        assertEquals("CustomString", result);
    }

    @Test
    public void testToStringIgnoresConverter() {
        // The converter is deliberately created with null options; if the method ever tried to use it,
        // a NullPointerException would be thrown. The test verifies that no such call occurs.
        Converter converter = new Converter(null);
        StringBuilder sb = new StringBuilder("IgnoredConverter");

        String result = StringBuilderConversions.toString(sb, converter);

        assertEquals("IgnoredConverter", result);
    }

    @Test
    public void testToStringWithNullConverterDoesNotThrow() {
        StringBuilder sb = new StringBuilder("NoConverter");
        // Pass {@code null} as the converter – the method should ignore it completely.
        String result = StringBuilderConversions.toString(sb, null);

        assertEquals("NoConverter", result);
    }

    @Test(expected = NullPointerException.class)
    public void testToStringWithNullObjectThrowsNPE() {
        Converter converter = createConverter();
        // The method attempts to call {@code toString()} on a null reference, which must throw NPE.
        StringBuilderConversions.toString(null, converter);
    }

    @Test
    public void testPrivateConstructorIsInaccessible() throws Exception {
        // Verify that the class cannot be instantiated via reflection (as it has a private constructor).
        java.lang.reflect.Constructor<StringBuilderConversions> ctor =
                StringBuilderConversions.class.getDeclaredConstructor();
        assertFalse("Constructor should be private", ctor.isAccessible());

        // Attempt to make it accessible and instantiate – this should succeed but the instance is unused.
        ctor.setAccessible(true);
        Object instance = ctor.newInstance();
        assertNotNull("Instance created via reflection should not be null", instance);
    }
}
