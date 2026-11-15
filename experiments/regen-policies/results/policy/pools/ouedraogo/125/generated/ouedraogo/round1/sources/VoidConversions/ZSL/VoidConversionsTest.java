package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Assert;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

/**
 * Unit tests for {@link VoidConversions}.
 *
 * The {@code VoidConversions} class provides three static conversion methods
 * that deliberately ignore their input and always return a constant value:
 *
 * <ul>
 *   <li>{@code toNull}    – always returns {@code null}</li>
 *   <li>{@code toBoolean} – always returns {@link Boolean#FALSE}</li>
 *   <li>{@code toCharacter} – always returns {@link Character#MIN_VALUE}</li>
 * </ul>
 *
 * The tests verify that:
 * <ol>
 *   <li>Each method returns the expected constant for a wide variety of
 *       {@code from} arguments (including {@code null}).</li>
 *   <li>The supplied {@link Converter} instance is ignored – the result is
 *       identical regardless of the state of the converter.</li>
 *   <li>The class cannot be instantiated via a public constructor (its
 *       constructor is private).</li>
 * </ol>
 *
 * No external libraries, mocking frameworks or additional API calls are used;
 * only the classes and methods listed in the problem statement.
 */
public class VoidConversionsTest {

    /**
     * Helper method that creates a {@link Converter}. The {@code Converter}
     * constructor accepts a {@code ConverterOptions} argument, but the
     * implementation of {@code VoidConversions} does not use the converter
     * at all, so we can safely pass {@code null}.
     */
    private static Converter createConverter() {
        // Passing null is acceptable because the parameter type is a reference.
        return new Converter(null);
    }

    @Test
    public void testToNullAlwaysReturnsNull() {
        Converter converter = createConverter();

        // A collection of diverse input values, including {@code null}
        Object[] inputs = {
                null,
                "some string",
                123,
                45.67,
                new Object(),
                Boolean.TRUE,
                Character.valueOf('X')
        };

        for (Object input : inputs) {
            Object result = VoidConversions.toNull(input, converter);
            Assert.assertNull("toNull should return null for input: " + input, result);
        }
    }

    @Test
    public void testToBooleanAlwaysReturnsFalse() {
        Converter converter = createConverter();

        Object[] inputs = {
                null,
                "true",
                "false",
                Boolean.TRUE,
                Boolean.FALSE,
                0,
                1,
                new Object()
        };

        for (Object input : inputs) {
            Boolean result = VoidConversions.toBoolean(input, converter);
            // The method is documented to always return Boolean.FALSE
            Assert.assertSame("toBoolean should return Boolean.FALSE for input: " + input,
                    Boolean.FALSE, result);
        }
    }

    @Test
    public void testToCharacterAlwaysReturnsMinValue() {
        Converter converter = createConverter();

        Object[] inputs = {
                null,
                "A",
                'Z',
                100,
                new Object(),
                Boolean.FALSE
        };

        for (Object input : inputs) {
            Character result = VoidConversions.toCharacter(input, converter);
            Assert.assertEquals("toCharacter should return Character.MIN_VALUE for input: " + input,
                    Character.MIN_VALUE.charValue(), result.charValue());
        }
    }

    @Test
    public void testConverterArgumentIsIgnored() {
        // Two converters with different (null) options – they are still ignored.
        Converter converterA = new Converter(null);
        Converter converterB = new Converter(null); // identical in effect

        // Use a representative input value.
        Object sampleInput = "irrelevant";

        // All three conversion methods should produce the same result regardless of converter.
        Object nullResultA = VoidConversions.toNull(sampleInput, converterA);
        Object nullResultB = VoidConversions.toNull(sampleInput, converterB);
        Assert.assertNull(nullResultA);
        Assert.assertNull(nullResultB);

        Boolean boolResultA = VoidConversions.toBoolean(sampleInput, converterA);
        Boolean boolResultB = VoidConversions.toBoolean(sampleInput, converterB);
        Assert.assertSame(boolResultA, boolResultB);
        Assert.assertSame(Boolean.FALSE, boolResultA);

        Character charResultA = VoidConversions.toCharacter(sampleInput, converterA);
        Character charResultB = VoidConversions.toCharacter(sampleInput, converterB);
        Assert.assertEquals(charResultA, charResultB);
        Assert.assertEquals(Character.MIN_VALUE, charResultA);
    }

    @Test
    public void testPrivateConstructorIsNotAccessible() throws Exception {
        Constructor<VoidConversions> ctor = VoidConversions.class.getDeclaredConstructor();
        // The constructor must be private.
        Assert.assertTrue("Constructor should be private",
                Modifier.isPrivate(ctor.getModifiers()));

        // Attempting to instantiate without overriding accessibility should fail.
        try {
            ctor.newInstance();
            Assert.fail("Instantiating VoidConversions via reflection without setAccessible should throw IllegalAccessException");
        } catch (IllegalAccessException expected) {
            // Expected path.
        }

        // If we deliberately make it accessible, instantiation succeeds,
        // confirming that the only barrier is the access modifier.
        ctor.setAccessible(true);
        VoidConversions instance = ctor.newInstance();
        Assert.assertNotNull("Instance should be created when accessibility is overridden", instance);
    }
}
