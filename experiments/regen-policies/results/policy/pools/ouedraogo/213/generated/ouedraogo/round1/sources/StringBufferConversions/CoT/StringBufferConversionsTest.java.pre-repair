package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * JUnit 4 test cases for {@link StringBufferConversions}.
 *
 * The class under test is package‑private and its only method is
 * {@code static String toString(Object from, Converter converter)}.
 * The method simply returns {@code from.toString()} and does not use the
 * {@code Converter} argument.
 *
 * The tests cover:
 *   • Normal usage with various object types
 *   • Behaviour when the {@code Converter} argument is {@code null}
 *   • Edge cases such as {@code null} source objects and objects whose
 *     {@code toString()} implementation returns {@code null}
 *   • Verification that the method does not depend on the {@code Converter}
 *     instance.
 */
public class StringBufferConversionsTest {

    /**
     * Helper method to create a {@link Converter} instance.
     * The real {@code Converter} class requires a {@code ConverterOptions}
     * argument; passing {@code null} is acceptable for the purpose of these
     * tests because the {@code toString} method never touches the converter.
     */
    private static Converter createConverter() {
        return new Converter(null);
    }

    // -----------------------------------------------------------------
    //  Basic functional tests
    // -----------------------------------------------------------------

    @Test
    public void testToString_WithPlainObject() {
        Object obj = new Object() {
            @Override
            public String toString() {
                return "plain-object";
            }
        };
        String result = StringBufferConversions.toString(obj, createConverter());
        assertEquals("plain-object", result);
    }

    @Test
    public void testToString_WithString() {
        String source = "Hello, World!";
        String result = StringBufferConversions.toString(source, createConverter());
        assertEquals(source, result);
    }

    @Test
    public void testToString_WithStringBuilder() {
        StringBuilder sb = new StringBuilder("builder");
        String result = StringBufferConversions.toString(sb, createConverter());
        assertEquals(sb.toString(), result);
    }

    @Test
    public void testToString_WithCustomObject() {
        class Custom {
            @Override
            public String toString() {
                return "custom-value";
            }
        }
        Custom custom = new Custom();
        String result = StringBufferConversions.toString(custom, createConverter());
        assertEquals("custom-value", result);
    }

    // -----------------------------------------------------------------
    //  Edge case / exception handling tests
    // -----------------------------------------------------------------

    @Test(expected = NullPointerException.class)
    public void testToString_NullFrom_ThrowsNPE() {
        // The method directly invokes from.toString(), so a null source must
        // raise a NullPointerException.
        StringBufferConversions.toString(null, createConverter());
    }

    @Test
    public void testToString_NullConverter_DoesNotAffectResult() {
        // The converter argument is ignored; passing null must not change the
        // outcome.
        String source = "no-converter";
        String result = StringBufferConversions.toString(source, null);
        assertEquals(source, result);
    }

    @Test
    public void testToString_ObjectToStringReturnsNull() {
        // Although unusual, a class may deliberately return null from toString().
        class NullToString {
            @Override
            public String toString() {
                return null;
            }
        }
        NullToString obj = new NullToString();
        String result = StringBufferConversions.toString(obj, createConverter());
        assertNull("Expected null when object's toString() returns null", result);
    }

    // -----------------------------------------------------------------
    //  Additional sanity checks
    // -----------------------------------------------------------------

    @Test
    public void testToString_DifferentConverterInstancesYieldSameResult() {
        String source = "same-result";
        Converter c1 = new Converter(null);
        Converter c2 = new Converter(null);
        String r1 = StringBufferConversions.toString(source, c1);
        String r2 = StringBufferConversions.toString(source, c2);
        assertEquals(r1, r2);
    }

    // -----------------------------------------------------------------
    //  Reflection test to ensure the private constructor is not usable
    // -----------------------------------------------------------------

    @Test(expected = IllegalAccessException.class)
    public void testPrivateConstructor_IsNotAccessible() throws Exception {
        // Attempt to instantiate the utility class via reflection.
        // The constructor is private; invoking it should throw IllegalAccessException.
        java.lang.reflect.Constructor<StringBufferConversions> ctor =
                StringBufferConversions.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        ctor.newInstance();
    }
}
