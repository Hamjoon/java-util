package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link VoidConversions}. The class under test is package‑private,
 * therefore the test resides in the same package.
 *
 * The methods in {@code VoidConversions} are deliberately simple – they ignore the
 * input value and always return a constant (null, {@code Boolean.FALSE}, or
 * {@code Character.MIN_VALUE}). The tests verify that:
 *
 * <ul>
 *   <li>The returned value is exactly the expected constant.</li>
 *   <li>The methods never throw an exception, regardless of the type or value
 *       of the {@code from} argument (including {@code null}).</li>
 *   <li>The {@code Converter} argument is not used to affect the outcome.</li>
 * </ul>
 *
 * Because {@link Converter} requires a {@link ConverterOptions} instance, and the
 * library does not expose a public no‑arg constructor for {@code ConverterOptions},
 * we instantiate {@code Converter} with a {@code null} options reference – this is
 * valid for the purpose of these tests because the {@code VoidConversions} methods
 * never invoke any behaviour on the {@code Converter} object.
 */
public class VoidConversionsTest {

    /** Helper method to create a {@link Converter} instance for the tests. */
    private static Converter createConverter() {
        // Passing null is acceptable – the methods under test never use the Converter.
        return new Converter(null);
    }

    /** A collection of diverse objects used as the {@code from} argument. */
    private static final Object[] SAMPLE_INPUTS = new Object[] {
            null,
            Boolean.TRUE,
            Boolean.FALSE,
            "some string",
            123,
            45.6,
            new Object(),
            new int[]{1, 2, 3},
            new Thread()
    };

    @Test
    public void testToNullAlwaysReturnsNull() {
        Converter converter = createConverter();

        // Verify that null is returned for each sample input.
        for (Object input : SAMPLE_INPUTS) {
            Object result = VoidConversions.toNull(input, converter);
            assertNull("toNull should return null for input: " + input, result);
        }
    }

    @Test
    public void testToBooleanAlwaysReturnsBooleanFalse() {
        Converter converter = createConverter();

        for (Object input : SAMPLE_INPUTS) {
            Boolean result = VoidConversions.toBoolean(input, converter);
            // Boolean.FALSE is a singleton; we can use assertSame.
            assertSame("toBoolean should always return Boolean.FALSE for input: " + input,
                       Boolean.FALSE, result);
        }
    }

    @Test
    public void testToCharacterAlwaysReturnsMinValue() {
        Converter converter = createConverter();

        for (Object input : SAMPLE_INPUTS) {
            Character result = VoidConversions.toCharacter(input, converter);
            // Verify the primitive char value is the minimum value (U+0000).
            assertEquals("toCharacter should return Character.MIN_VALUE for input: " + input,
                         Character.MIN_VALUE.charValue(), result.charValue());
        }
    }

    @Test
    public void testMethodsNeverThrowException() {
        Converter converter = createConverter();

        // The methods are trivial, but we still confirm they do not throw.
        for (Object input : SAMPLE_INPUTS) {
            try {
                VoidConversions.toNull(input, converter);
                VoidConversions.toBoolean(input, converter);
                VoidConversions.toCharacter(input, converter);
            } catch (Exception e) {
                fail("No exception should be thrown for input: " + input + " – got: " + e);
            }
        }
    }

    @Test
    public void testMultipleCallsReturnConsistentResults() {
        Converter converter = createConverter();

        // Repeated calls with the same argument must yield the same constant.
        for (Object input : SAMPLE_INPUTS) {
            Boolean firstBool = VoidConversions.toBoolean(input, converter);
            Boolean secondBool = VoidConversions.toBoolean(input, converter);
            assertSame(firstBool, secondBool);

            Character firstChar = VoidConversions.toCharacter(input, converter);
            Character secondChar = VoidConversions.toCharacter(input, converter);
            assertEquals(firstChar.charValue(), secondChar.charValue());
        }
    }
}
