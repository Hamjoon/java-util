package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Assert;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

/**
 * Unit tests for {@link StringBufferConversions}.
 *
 * The class under test is package‑private, final and has a private constructor.
 * It only provides a static {@code toString(Object, Converter)} method that simply
 * delegates to {@code Object.toString()}.
 *
 * The tests verify:
 * <ul>
 *   <li>Correct string conversion for a variety of input objects.</li>
 *   <li>The method throws {@link NullPointerException} when the source object is {@code null}.</li>
 *   <li>The {@code Converter} argument is truly ignored (passing {@code null} works).</li>
 *   <li>The class cannot be instantiated via normal means and its constructor is private.</li>
 * </ul>
 */
public class StringBufferConversionsTest {

    /** Simple POJO with a custom {@code toString()} implementation. */
    private static final class CustomObject {
        private final String name;
        private final int id;

        CustomObject(String name, int id) {
            this.name = name;
            this.id = id;
        }

        @Override
        public String toString() {
            return "CustomObject{name='" + name + "', id=" + id + '}';
        }
    }

    @Test
    public void toString_WithString_ReturnsSameString() {
        String source = "Hello, World!";
        // Converter is not used; passing null is sufficient.
        String result = StringBufferConversions.toString(source, null);
        Assert.assertEquals(source, result);
    }

    @Test
    public void toString_WithInteger_ReturnsIntegerString() {
        Integer source = 12345;
        String result = StringBufferConversions.toString(source, null);
        Assert.assertEquals("12345", result);
    }

    @Test
    public void toString_WithCustomObject_UsesCustomToString() {
        CustomObject source = new CustomObject("Test", 99);
        String expected = "CustomObject{name='Test', id=99}";
        String result = StringBufferConversions.toString(source, null);
        Assert.assertEquals(expected, result);
    }

    @Test(expected = NullPointerException.class)
    public void toString_WithNullSource_ThrowsNullPointerException() {
        // The implementation calls from.toString() directly, so NPE is expected.
        StringBufferConversions.toString(null, null);
    }

    @Test
    public void privateConstructor_IsNotAccessible() throws Exception {
        Constructor<StringBufferConversions> ctor = StringBufferConversions.class.getDeclaredConstructor();
        // Verify that the constructor is indeed private.
        Assert.assertTrue("Constructor should be private", Modifier.isPrivate(ctor.getModifiers()));

        // Attempting to instantiate without overriding accessibility should fail.
        try {
            ctor.newInstance();
            Assert.fail("Expected IllegalAccessException when invoking private constructor without setAccessible(true)");
        } catch (IllegalAccessException expected) {
            // Expected path.
        }

        // If we forcibly make it accessible, we *can* instantiate it, but the instance
        // should be of the expected type and no side‑effects occur.
        ctor.setAccessible(true);
        StringBufferConversions instance = ctor.newInstance();
        Assert.assertNotNull("Instance created via reflection should not be null", instance);
    }
}
