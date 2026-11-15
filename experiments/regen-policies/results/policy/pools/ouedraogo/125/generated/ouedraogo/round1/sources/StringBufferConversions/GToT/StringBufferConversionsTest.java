package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link StringBufferConversions}.
 *
 * The class under test contains only one public static method:
 *
 * <pre>
 * static String toString(Object from, Converter converter)
 * </pre>
 *
 * The method simply delegates to {@code from.toString()} and does **not**
 * use the {@code Converter} argument.  The tests therefore verify:
 *
 * <ul>
 *   <li>Normal operation with typical objects.</li>
 *   <li>That a supplied {@code Converter} (or {@code null}) does not affect the result.</li>
 *   <li>Behaviour when {@code from} is {@code null} – a {@link NullPointerException}
 *       is expected because the implementation calls {@code from.toString()}.</li>
 *   <li>Edge cases where {@code toString()} returns {@code null} or throws an exception.</li>
 * </ul>
 *
 * All tests are placed in the same package as the class under test so that the
 * package‑private class can be accessed.
 */
public class StringBufferConversionsTest {

    /* ---------------------------------------------------------------------- */
    /* Helper objects                                                         */
    /* ---------------------------------------------------------------------- */

    /** Simple object whose {@code toString()} returns a fixed value. */
    private static final class FixedString {
        private final String value;
        FixedString(String value) { this.value = value; }
        @Override public String toString() { return value; }
    }

    /** Object whose {@code toString()} deliberately returns {@code null}. */
    private static final class NullString {
        @Override public String toString() { return null; }
    }

    /** Object whose {@code toString()} throws a runtime exception. */
    private static final class ExplodingString {
        @Override public String toString() { throw new IllegalStateException("boom"); }
    }

    /* ---------------------------------------------------------------------- */
    /* Basic functionality tests                                              */
    /* ---------------------------------------------------------------------- */

    /** Verify that the method returns the exact result of {@code from.toString()}. */
    @Test
    public void testToStringBasic() {
        Integer source = 42;
        Converter converter = new Converter(new ConverterOptions());
        String result = StringBufferConversions.toString(source, converter);
        assertEquals("42", result);
    }

    /** The supplied {@code Converter} must not influence the result. */
    @Test
    public void testToStringIgnoresConverter() {
        FixedString source = new FixedString("ignored");
        // Pass a real converter – it should be ignored.
        Converter converter = new Converter(new ConverterOptions());
        assertEquals("ignored", StringBufferConversions.toString(source, converter));

        // Also verify that a {@code null} converter is tolerated.
        assertEquals("ignored", StringBufferConversions.toString(source, null));
    }

    /** Custom objects with overridden {@code toString()} should be handled correctly. */
    @Test
    public void testToStringWithCustomObject() {
        FixedString source = new FixedString("customValue");
        assertEquals("customValue",
                StringBufferConversions.toString(source, new Converter(new ConverterOptions())));
    }

    /* ---------------------------------------------------------------------- */
    /* Edge‑case and exception tests                                          */
    /* ---------------------------------------------------------------------- */

    /** Passing {@code null} as the source must throw {@link NullPointerException}. */
    @Test(expected = NullPointerException.class)
    public void testToStringWithNullSource() {
        StringBufferConversions.toString(null, new Converter(new ConverterOptions()));
    }

    /** If {@code from.toString()} returns {@code null}, the method should return {@code null}. */
    @Test
    public void testToStringReturnsNullWhenSourceToStringIsNull() {
        NullString source = new NullString();
        String result = StringBufferConversions.toString(source, new Converter(new ConverterOptions()));
        assertNull(result);
    }

    /** Exceptions thrown by {@code from.toString()} must propagate unchanged. */
    @Test(expected = IllegalStateException.class)
    public void testToStringPropagatesExceptionFromSource() {
        ExplodingString source = new ExplodingString();
        StringBufferConversions.toString(source, new Converter(new ConverterOptions()));
    }

    /* ---------------------------------------------------------------------- */
    /* Reflection test to ensure the private constructor cannot be invoked   */
    /* ---------------------------------------------------------------------- */

    /**
     * The class has a private no‑arg constructor to prevent instantiation.
     * This test uses reflection to confirm that it is indeed private and
     * cannot be called without setting accessibility.
     */
    @Test
    public void testPrivateConstructorIsInaccessible() throws Exception {
        java.lang.reflect.Constructor<StringBufferConversions> ctor =
                StringBufferConversions.class.getDeclaredConstructor();
        assertTrue("Constructor should be private", java.lang.reflect.Modifier.isPrivate(ctor.getModifiers()));
        // Attempting to invoke it without overriding accessibility should fail.
        try {
            ctor.newInstance();
            fail("Expected IllegalAccessException when invoking private constructor");
        } catch (IllegalAccessException expected) {
            // Expected path
        }
    }
}
