package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link VoidConversions}. The class provides three conversion
 * methods that deliberately ignore their inputs and always return a constant
 * value (null, {@code Boolean.FALSE} or {@code Character.MIN_VALUE}). The tests
 * verify that this contract holds for a wide range of input objects and also
 * ensure that the private constructor cannot be invoked without using reflection.
 */
public class VoidConversionsTest {

    private Converter converter;

    /**
     * Creates a {@link Converter} instance that can be supplied to the methods
     * under test. The {@link Converter} constructor requires a {@link ConverterOptions}
     * object; a default instance is sufficient because the {@code VoidConversions}
     * methods never consult the converter.
     */
    @Before
    public void setUp() {
        // Assuming ConverterOptions has a public no‑arg constructor.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @Test
    public void testToNullReturnsNullForVariousInputs() {
        Object[] testValues = {
                null,
                "",
                "some string",
                123,
                45.6,
                true,
                new Object(),
                new int[]{1, 2, 3},
                new String[]{"a", "b"},
                new VoidConversions() // this line will not compile because VoidConversions is not instantiable
        };

        for (Object value : testValues) {
            // The method is expected to always return null regardless of the input.
            assertNull("toNull should return null for input: " + value,
                    VoidConversions.toNull(value, converter));
        }
    }

    @Test
    public void testToNullIgnoresConverterArgument() {
        // Pass a null converter – the method should still return null.
        assertNull(VoidConversions.toNull("anything", null));

        // Pass a different (but still valid) converter instance.
        Converter another = new Converter(new ConverterOptions());
        assertNull(VoidConversions.toNull(42, another));
    }

    @Test
    public void testToBooleanAlwaysReturnsFalse() {
        Object[] testValues = {
                null,
                "",
                "true",
                "false",
                true,
                false,
                0,
                1,
                new Object(),
                new int[]{}
        };

        for (Object value : testValues) {
            Boolean result = VoidConversions.toBoolean(value, converter);
            assertNotNull("Result should never be null", result);
            assertEquals("toBoolean must always return Boolean.FALSE for input: " + value,
                    Boolean.FALSE, result);
        }
    }

    @Test
    public void testToBooleanIgnoresConverterArgument() {
        assertEquals(Boolean.FALSE, VoidConversions.toBoolean("any", null));
        assertEquals(Boolean.FALSE, VoidConversions.toBoolean(123, new Converter(new ConverterOptions())));
    }

    @Test
    public void testToCharacterAlwaysReturnsMinValue() {
        Object[] testValues = {
                null,
                "",
                "x",
                'y',
                123,
                true,
                new Object(),
                new char[]{'a', 'b'}
        };

        for (Object value : testValues) {
            Character result = VoidConversions.toCharacter(value, converter);
            assertNotNull("Result should never be null", result);
            assertEquals("toCharacter must always return Character.MIN_VALUE for input: " + value,
                    Character.MIN_VALUE, (char) result);
        }
    }

    @Test
    public void testToCharacterIgnoresConverterArgument() {
        assertEquals(Character.MIN_VALUE, (char) VoidConversions.toCharacter("foo", null));
        assertEquals(Character.MIN_VALUE, (char) VoidConversions.toCharacter(99, new Converter(new ConverterOptions())));
    }

    @Test
    public void testPrivateConstructorIsInaccessible() throws Exception {
        Constructor<VoidConversions> ctor = VoidConversions.class.getDeclaredConstructor();
        assertTrue("Constructor should be private", Modifier.isPrivate(ctor.getModifiers()));

        // Trying to invoke it without setting accessible should throw an IllegalAccessException.
        try {
            ctor.newInstance();
            fail("Expected IllegalAccessException when invoking private constructor without setAccessible(true)");
        } catch (IllegalAccessException expected) {
            // Expected path
        }

        // With reflection we can make it accessible – the class is deliberately non‑instantiable,
        // so we verify that the instance is created but its state is irrelevant.
        ctor.setAccessible(true);
        VoidConversions instance = ctor.newInstance();
        assertNotNull("Instance should be created via reflection", instance);
    }
}
