package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Comprehensive JUnit‑4 test suite for {@link StringBufferConversions}.
 *
 * The class under test is intentionally simple – it only provides a static
 * {@code toString(Object, Converter)} method that returns {@code from.toString()}.
 * Nevertheless the tests cover:
 *
 * 1. Typical usage with various concrete types (String, StringBuffer, custom POJO).
 * 2. Edge cases such as {@code null} source objects (expect NPE).
 * 3. Verification that the {@code Converter} argument is truly ignored.
 * 4. Defensive test that the utility class cannot be instantiated (private ctor).
 */
public class StringBufferConversionsTest {

    private Converter converter;   // a real Converter instance – its behaviour is irrelevant

    @Before
    public void setUp() {
        // ConverterOptions is part of the library; we can use its default constructor.
        converter = new Converter(new ConverterOptions());
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /*** 1. Normal String argument **********************************************/
    @Test
    public void testToString_WithString_ReturnsSameString() {
        String source = "Hello, World!";
        String result = StringBufferConversions.toString(source, converter);
        assertEquals("The returned string should be equal to the source string.", source, result);
        // For a literal the returned instance may be the same; we only require equality.
    }

    /*** 2. StringBuffer argument ***********************************************/
    @Test
    public void testToString_WithStringBuffer_ReturnsBufferContent() {
        StringBuffer sb = new StringBuffer("Buffer content");
        String result = StringBufferConversions.toString(sb, converter);
        assertEquals("Result should be the buffer's content as a String.", "Buffer content", result);
    }

    /*** 3. Custom object with overridden toString *****************************/
    private static final class Person {
        private final String name;
        private final int age;

        Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        @Override
        public String toString() {
            return "Person{name='" + name + "', age=" + age + '}';
        }
    }

    @Test
    public void testToString_WithCustomObject_UsesOverriddenToString() {
        Person p = new Person("Alice", 30);
        String expected = "Person{name='Alice', age=30}";
        String result = StringBufferConversions.toString(p, converter);
        assertEquals("Should return the string produced by Person.toString().", expected, result);
    }

    /*** 4. Null source object – expect NullPointerException ******************/
    @Test
    public void testToString_NullSource_ThrowsNullPointerException() {
        thrown.expect(NullPointerException.class);
        StringBufferConversions.toString(null, converter);
    }

    /*** 5. Converter argument is ignored – passing null is safe ****************/
    @Test
    public void testToString_NullConverter_Ignored() {
        // The method does not dereference the converter, so a null value is acceptable.
        String source = "No converter needed";
        String result = StringBufferConversions.toString(source, null);
        assertEquals("Result should be identical to source even when converter is null.", source, result);
    }

    /*** 6. Private constructor cannot be instantiated *************************/
    @Test
    public void testPrivateConstructor_NotInstantiable() throws Exception {
        Constructor<StringBufferConversions> ctor = StringBufferConversions.class.getDeclaredConstructor();
        assertTrue("Constructor should be private.", java.lang.reflect.Modifier.isPrivate(ctor.getModifiers()));

        // Attempting to make it accessible and instantiate should throw an exception
        ctor.setAccessible(true);
        try {
            ctor.newInstance();
            fail("Instantiating the utility class should throw an IllegalAccessException or InvocationTargetException.");
        } catch (InvocationTargetException e) {
            // The private constructor throws an IllegalAccessException indirectly.
            // The actual cause is usually an IllegalAccessException wrapped in InvocationTargetException.
            assertTrue(e.getCause() instanceof IllegalAccessException);
        }
    }

    /*** 7. Verify that the method always returns a new String for mutable inputs */
    @Test
    public void testToString_MutableInput_ReturnsIndependentString() {
        StringBuilder mutable = new StringBuilder("mutable");
        String result = StringBufferConversions.toString(mutable, converter);
        assertEquals("Result should match mutable's content.", "mutable", result);

        // Change the original mutable after conversion and ensure the result does NOT change.
        mutable.append("-changed");
        assertEquals("Result must remain unchanged after source mutation.", "mutable", result);
    }
}
