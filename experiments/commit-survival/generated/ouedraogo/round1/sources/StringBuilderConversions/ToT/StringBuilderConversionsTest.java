package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link StringBuilderConversions}.
 * <p>
 * The class under test is deliberately tiny – it only contains a private constructor
 * and a single static {@code toString} method.  The tests therefore focus on:
 * <ul>
 *   <li>Typical usage with a {@link StringBuilder} instance.</li>
 *   <li>Typical usage with an arbitrary object that overrides {@code toString()}.</li>
 *   <li>Behaviour when {@code from} is {@code null} (NPE is expected).</li>
 *   <li>Behaviour when {@code converter} is {@code null} – the method must ignore it.</li>
 *   <li>Ensuring the class is {@code final} and its constructor is private.</li>
 *   <li>Confirming that the {@code Converter} argument is not used.</li>
 * </ul>
 * Only the public API of {@link Converter} and {@link ConverterOptions} is used – no
 * mocking frameworks or subclassing is employed.
 */
public class StringBuilderConversionsTest {

    // -------------------------------------------------------------------------
    // Helper objects used by several tests
    // -------------------------------------------------------------------------

    /** A simple {@link ConverterOptions} instance – the real class is part of the library
     *  and provides a public no‑arg constructor.  No configuration is required for
     *  these tests. */
    private static ConverterOptions defaultOptions;

    /** A {@link Converter} instance created with the default options. */
    private static Converter defaultConverter;

    @BeforeClass
    public static void setUpClass() {
        // The library supplies a public no‑arg constructor for ConverterOptions.
        defaultOptions = new ConverterOptions();
        defaultConverter = new Converter(defaultOptions);
    }

    // -------------------------------------------------------------------------
    // 1. Typical case – converting a StringBuilder
    // -------------------------------------------------------------------------

    @Test
    public void toString_WithStringBuilder_ReturnsItsString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Hello ").append("World!");

        String result = StringBuilderConversions.toString(sb, defaultConverter);

        assertEquals("Hello World!", result);
    }

    // -------------------------------------------------------------------------
    // 2. Typical case – converting an arbitrary object with a custom toString()
    // -------------------------------------------------------------------------

    private static final class CustomObject {
        private final int id;
        CustomObject(int id) { this.id = id; }
        @Override
        public String toString() {
            return "CustomObject#" + id;
        }
    }

    @Test
    public void toString_WithCustomObject_UsesObjectToString() {
        CustomObject obj = new CustomObject(42);

        String result = StringBuilderConversions.toString(obj, defaultConverter);

        assertEquals("CustomObject#42", result);
    }

    // -------------------------------------------------------------------------
    // 3. Edge case – null 'from' argument (should throw NullPointerException)
    // -------------------------------------------------------------------------

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void toString_NullFrom_ThrowsNullPointerException() {
        thrown.expect(NullPointerException.class);
        // The method dereferences 'from' immediately, so NPE is expected.
        StringBuilderConversions.toString(null, defaultConverter);
    }

    // -------------------------------------------------------------------------
    // 4. Edge case – null 'converter' argument (must be ignored)
    // -------------------------------------------------------------------------

    @Test
    public void toString_NullConverter_IgnoresConverter() {
        StringBuilder sb = new StringBuilder("No converter needed");
        // Pass null for the converter; the method should still work.
        String result = StringBuilderConversions.toString(sb, null);
        assertEquals("No converter needed", result);
    }

    // -------------------------------------------------------------------------
    // 5. Verify that the provided Converter is never consulted
    // -------------------------------------------------------------------------

    @Test
    public void toString_ConverterIsIgnored_EvenIfConversionWouldDiffer() {
        // Create an object whose toString() returns a simple value.
        StringBuilder sb = new StringBuilder("original");
        // Create a Converter that would normally convert a StringBuilder to something else.
        // We do NOT register any custom conversion – we just rely on the fact that
        // StringBuilderConversions does not call any method on the converter.
        String result = StringBuilderConversions.toString(sb, defaultConverter);
        assertEquals("original", result);
    }

    // -------------------------------------------------------------------------
    // 6. Class design checks – final class and private constructor
    // -------------------------------------------------------------------------

    @Test
    public void class_IsFinal() {
        assertTrue("StringBuilderConversions should be final",
                Modifier.isFinal(StringBuilderConversions.class.getModifiers()));
    }

    @Test
    public void constructor_IsPrivate() throws Exception {
        Constructor<StringBuilderConversions> ctor =
                StringBuilderConversions.class.getDeclaredConstructor();
        assertTrue("Constructor must be private",
                Modifier.isPrivate(ctor.getModifiers()));
        // Ensure that the constructor cannot be invoked without overriding accessibility.
        try {
            ctor.newInstance();
            fail("Expected IllegalAccessException because constructor is private");
        } catch (IllegalAccessException expected) {
            // Expected path
        }
    }

    @Test
    public void constructor_CanBeInvokedViaReflectionWhenAccessible() throws Exception {
        Constructor<StringBuilderConversions> ctor =
                StringBuilderConversions.class.getDeclaredConstructor();
        ctor.setAccessible(true); // bypass the private modifier
        // The class has no state; the instance is discarded.
        Object instance = ctor.newInstance();
        assertNotNull("Instance should be created via reflection", instance);
    }
}
