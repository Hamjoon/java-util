/*****************************************************************************************
 * JUnit 4 test suite for com.cedarsoftware.util.convert.VoidConversions
 *
 * This test class exercises every public static method of VoidConversions and verifies
 * that the returned values are exactly as defined in the source implementation,
 * regardless of the input parameters.
 *
 * The tests are placed in the same package as VoidConversions so that package‑private
 * static methods are directly accessible.
 *
 * Author: Automated Generation (ChatGPT)
 * Date:   2026‑11‑14
 *****************************************************************************************/

package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Comprehensive unit tests for the {@code VoidConversions} utility class.
 *
 * The class under test contains three static conversion methods that deliberately
 * ignore their inputs and always return a constant value:
 * <ul>
 *   <li>{@code toNull}    – always returns {@code null}</li>
 *   <li>{@code toBoolean} – always returns {@code Boolean.FALSE}</li>
 *   <li>{@code toCharacter} – always returns {@code Character.MIN_VALUE}</li>
 * </ul>
 *
 * The tests verify those contracts with a variety of input arguments, including
 * {@code null}, primitive wrappers, strings and custom objects.  A real {@link Converter}
 * instance is constructed (via {@link ConverterOptions}) to satisfy the method signatures,
 * even though the implementation does not actually use it.
 */
public class VoidConversionsTest
{
    /** A reusable Converter instance – the implementation of VoidConversions does not
     *  depend on its behaviour, but the API requires a non‑null argument for most calls. */
    private Converter converter;

    @Before
    public void setUp()
    {
        // ConverterOptions is part of the public API; it has a public no‑arg constructor.
        // If the library later changes the construction pattern, this test will still compile
        // because it only relies on the existence of the constructor.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    /*======================================================================
     *  Tests for toNull(...)
     *======================================================================*/

    @Test
    public void toNull_ShouldReturnNull_ForNonNullInput()
    {
        Object result = VoidConversions.toNull("anyString", converter);
        assertNull("toNull must return null for a non‑null input", result);
    }

    @Test
    public void toNull_ShouldReturnNull_ForNullInput()
    {
        Object result = VoidConversions.toNull(null, converter);
        assertNull("toNull must return null for a null input", result);
    }

    @Test
    public void toNull_ShouldReturnNull_RegardlessOfConverter()
    {
        // Pass a different Converter instance to prove that the method ignores it.
        Converter anotherConverter = new Converter(new ConverterOptions());
        Object result = VoidConversions.toNull(12345, anotherConverter);
        assertNull("toNull must always return null, irrespective of the Converter instance", result);
    }

    /*======================================================================
     *  Tests for toBoolean(...)
     *======================================================================*/

    @Test
    public void toBoolean_ShouldAlwaysReturnFalse_ForVariousInputs()
    {
        Object[] testValues = {
                null,
                Boolean.TRUE,
                Boolean.FALSE,
                "true",
                "false",
                "any random string",
                0,
                1,
                -42,
                new Object()
        };

        for (Object input : testValues)
        {
            Boolean result = VoidConversions.toBoolean(input, converter);
            assertEquals("toBoolean must always return Boolean.FALSE", Boolean.FALSE, result);
            // Ensure the same singleton instance is returned (Boolean.FALSE is cached)
            assertSame("toBoolean should return the cached Boolean.FALSE instance", Boolean.FALSE, result);
        }
    }

    @Test
    public void toBoolean_IgnoresConverterInstance()
    {
        Converter other = new Converter(new ConverterOptions());
        Boolean result = VoidConversions.toBoolean("ignored", other);
        assertEquals(Boolean.FALSE, result);
    }

    /*======================================================================
     *  Tests for toCharacter(...)
     *======================================================================*/

    @Test
    public void toCharacter_ShouldAlwaysReturnMinValue_ForVariousInputs()
    {
        Object[] testValues = {
                null,
                'A',
                'z',
                '\n',
                123,
                "some string",
                new Object()
        };

        for (Object input : testValues)
        {
            Character result = VoidConversions.toCharacter(input, converter);
            // Character.MIN_VALUE is the null character '\u0000'
            assertEquals("toCharacter must always return Character.MIN_VALUE",
                         Character.valueOf(Character.MIN_VALUE), result);
            assertEquals("Returned character must be the null character",
                         '\u0000', result.charValue());
        }
    }

    @Test
    public void toCharacter_IgnoresConverterInstance()
    {
        Converter dummy = new Converter(new ConverterOptions());
        Character result = VoidConversions.toCharacter("anything", dummy);
        assertEquals(Character.valueOf('\u0000'), result);
    }
}
