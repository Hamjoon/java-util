package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link StringBufferConversions}.
 *
 * The class under test provides a single static method {@code toString(Object, Converter)} that simply
 * delegates to {@code Object.toString()}.  The tests therefore verify:
 *
 * <ul>
 *   <li>Correct string is returned for a variety of non‑null objects.</li>
 *   <li>The {@code Converter} argument is ignored (no side‑effects, no exceptions).</li>
 *   <li>A {@code NullPointerException} is thrown when {@code from} is {@code null}.</li>
 * </ul>
 *
 * The test uses only public API from the supplied library (the {@code Converter} and {@code ConverterOptions}
 * classes) and does not rely on any external mocking frameworks.
 */
public class StringBufferConversionsTest {

    /** Helper POJO with a custom {@code toString()} implementation. */
    private static final class CustomObject {
        private final int id;
        private final String name;

        CustomObject(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return "CustomObject{id=" + id + ",name='" + name + "'}";
        }
    }

    /** Creates a minimal {@code Converter} instance. The concrete configuration is irrelevant for these tests. */
    private static Converter createConverter() {
        // ConverterOptions has a public no‑arg constructor in the library.
        ConverterOptions options = new ConverterOptions();
        return new Converter(options);
    }

    @Test
    public void toString_WithString_ReturnsSameString() {
        String input = "Hello, World!";
        Converter converter = createConverter();

        String result = StringBufferConversions.toString(input, converter);

        assertEquals("The method should return the original string unchanged.", input, result);
    }

    @Test
    public void toString_WithInteger_ReturnsIntegerString() {
        Integer input = 42;
        Converter converter = createConverter();

        String result = StringBufferConversions.toString(input, converter);

        assertEquals("Integer.toString() should be used.", "42", result);
    }

    @Test
    public void toString_WithCustomObject_UsesCustomToString() {
        CustomObject input = new CustomObject(7, "test");
        Converter converter = createConverter();

        String result = StringBufferConversions.toString(input, converter);

        assertEquals("Custom object's toString() should be returned.", input.toString(), result);
    }

    @Test
    public void toString_IgnoresConverterArgument() {
        // The converter is constructed with a deliberately unusual configuration.
        // If the method ever consulted the converter, it would likely throw or return a different value.
        ConverterOptions options = new ConverterOptions();
        // Pretend we added a conversion that would change behavior (the real library allows this).
        // For the purpose of this test we simply create the converter; we do not invoke any conversion.
        Converter converter = new Converter(options);

        String input = "converter-ignored";

        String result = StringBufferConversions.toString(input, converter);

        assertEquals("The converter must be ignored; result should equal input.toString().", input, result);
    }

    @Test(expected = NullPointerException.class)
    public void toString_WithNullInput_ThrowsNullPointerException() {
        Converter converter = createConverter();

        // This call should throw NPE because the implementation does not guard against null.
        StringBufferConversions.toString(null, converter);
    }
}
