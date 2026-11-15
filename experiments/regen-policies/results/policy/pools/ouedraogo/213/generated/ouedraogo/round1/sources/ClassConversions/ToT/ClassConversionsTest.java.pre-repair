package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link ClassConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Typical usage – converting a regular {@code Class} to its name.</li>
 *   <li>Primitive, array and inner‑class names.</li>
 *   <li>Behaviour when {@code null} is supplied for the {@code from} argument.</li>
 *   <li>Behaviour when {@code from} is not a {@code Class} (expect {@code ClassCastException}).</li>
 *   <li>Verification that the {@code Converter} argument is truly ignored (including a {@code null} value).</li>
 *   <li>Ensuring the utility class cannot be instantiated (private constructor).</li>
 * </ul>
 */
@RunWith(JUnit4.class)
public class ClassConversionsTest {

    /**
     * Helper to create a {@link Converter} instance. The library permits a {@code null}
     * {@link ConverterOptions} argument, so we simply use that to avoid depending on the
     * concrete {@code ConverterOptions} implementation.
     */
    private static Converter createConverter() {
        return new Converter(null);
    }

    @Test
    public void testToStringWithRegularClass() {
        Converter converter = createConverter();
        String result = ClassConversions.toString(String.class, converter);
        assertEquals("java.lang.String", result);
    }

    @Test
    public void testToStringWithPrimitiveClass() {
        Converter converter = createConverter();
        String result = ClassConversions.toString(int.class, converter);
        // Primitive class names are the keyword itself.
        assertEquals("int", result);
    }

    @Test
    public void testToStringWithArrayClass() {
        Converter converter = createConverter();
        String result = ClassConversions.toString(String[].class, converter);
        // The JVM encodes array types in the name: "[L<component>;"
        assertEquals("[Ljava.lang.String;", result);
    }

    @Test
    public void testToStringWithInnerClass() {
        Converter converter = createConverter();
        class LocalInner {}
        String result = ClassConversions.toString(LocalInner.class, converter);
        // The name contains the enclosing class name followed by $LocalInner
        assertTrue(result.contains("$LocalInner"));
    }

    @Test
    public void testToStringIgnoresConverterArgument() {
        // Pass a real Converter – it should be ignored.
        Converter realConverter = createConverter();
        String realResult = ClassConversions.toString(Boolean.class, realConverter);
        assertEquals("java.lang.Boolean", realResult);

        // Pass a null Converter – method must still work because it never touches the argument.
        String nullResult = ClassConversions.toString(Boolean.class, null);
        assertEquals("java.lang.Boolean", nullResult);
    }

    @Test(expected = NullPointerException.class)
    public void testToStringWithNullFromArgument() {
        // The cast to Class<?> succeeds (null), but calling getName() on a null reference throws NPE.
        Converter converter = createConverter();
        ClassConversions.toString(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToStringWithNonClassFromArgument() {
        // Supplying an object that is not a Class should cause a ClassCastException
        // because the method performs an unchecked cast.
        Converter converter = createConverter();
        ClassConversions.toString(new Object(), converter);
    }

    @Test
    public void testUtilityClassHasPrivateConstructor() throws Exception {
        Constructor<ClassConversions> ctor = ClassConversions.class.getDeclaredConstructor();
        assertTrue("Constructor should be private", (ctor.getModifiers() & java.lang.reflect.Modifier.PRIVATE) != 0);
        ctor.setAccessible(true);
        try {
            ctor.newInstance();
            fail("Instantiating the utility class should throw an exception");
        } catch (InvocationTargetException e) {
            // The constructor is empty; reflection will wrap the IllegalAccessException or
            // any other exception. The expected behaviour is that instantiation is not allowed.
            // Since the constructor is private, we get IllegalAccessException wrapped in
            // InvocationTargetException.
            assertTrue(e.getCause() instanceof IllegalAccessException);
        }
    }
}
