package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * JUnit 4 test cases for {@link ClassConversions}.
 *
 * The class under test contains only one static method:
 * <pre>
 * static String toString(Object from, Converter converter)
 * </pre>
 * The method simply casts {@code from} to {@code Class<?>} and returns the
 * fully‑qualified class name.  The {@code Converter} argument is currently unused,
 * but it is part of the public API and therefore must be supplied.
 *
 * The test suite covers:
 * <ul>
 *   <li>Normal operation with a regular class.</li>
 *   <li>Operation with special class types (array, primitive wrapper, inner class).</li>
 *   <li>Behavior when {@code converter} is {@code null} (the method does not use it).</li>
 *   <li>Edge‑case handling:
 *       <ul>
 *         <li>Passing {@code null} as the {@code from} argument – expect {@link NullPointerException}.</li>
 *         <li>Passing an object that is not a {@code Class} – expect {@link ClassCastException}.</li>
 *       </ul>
 *   </li>
 * </ul>
 */
public class ClassConversionsTest {

    // Helper method to create a minimal Converter instance.
    // The real ConverterOptions class is not part of the supplied source,
    // but the constructor accepts a ConverterOptions argument that may be null.
    private static Converter createConverter() {
        return new Converter(null);
    }

    /*** Basic functional test ***/
    @Test
    public void testToString_WithRegularClass_ReturnsClassName() {
        Converter converter = createConverter();
        String result = ClassConversions.toString(String.class, converter);
        assertEquals("java.lang.String", result);
    }

    /*** Test with an array class ***/
    @Test
    public void testToString_WithArrayClass_ReturnsArrayClassName() {
        Converter converter = createConverter();
        String result = ClassConversions.toString(int[].class, converter);
        // The name of an array class follows the JVM internal format.
        assertEquals("[I", result);
    }

    /*** Test with a primitive wrapper class ***/
    @Test
    public void testToString_WithWrapperClass_ReturnsClassName() {
        Converter converter = createConverter();
        String result = ClassConversions.toString(Integer.class, converter);
        assertEquals("java.lang.Integer", result);
    }

    /*** Test with an inner (static nested) class ***/
    @Test
    public void testToString_WithInnerClass_ReturnsQualifiedName() {
        Converter converter = createConverter();
        String result = ClassConversions.toString(InnerClass.class, converter);
        assertEquals("com.cedarsoftware.util.convert.ClassConversionsTest$InnerClass", result);
    }

    /*** Test that a null Converter does not affect the outcome ***/
    @Test
    public void testToString_WithNullConverter_ReturnsClassName() {
        String result = ClassConversions.toString(Double.class, null);
        assertEquals("java.lang.Double", result);
    }

    /*** Edge case: from argument is null – expect NullPointerException ***/
    @Test(expected = NullPointerException.class)
    public void testToString_NullFrom_ThrowsNullPointerException() {
        Converter converter = createConverter();
        ClassConversions.toString(null, converter);
    }

    /*** Edge case: from argument is not a Class – expect ClassCastException ***/
    @Test(expected = ClassCastException.class)
    public void testToString_NonClassFrom_ThrowsClassCastException() {
        Converter converter = createConverter();
        ClassConversions.toString("I am not a Class object", converter);
    }

    /*** Inner static class used for the inner‑class test ***/
    static class InnerClass {
        // No content needed – the existence of the class is sufficient.
    }
}
