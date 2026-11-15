package com.cedarsoftware.util.convert;

import org.junit.Test;
import java.lang.reflect.Constructor;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@code VoidConversions}.
 *
 * <p>All package‑private static methods of {@code VoidConversions} are exercised.
 * The class lives in the same package as the test so that the package‑private
 * members are directly accessible.</p>
 *
 * <h2>Public (exposed) methods in {@code VoidConversions}</h2>
 * <ul>
 *   <li>{@code static Object toNull(Object from, Converter converter)}</li>
 *   <li>{@code static Boolean toBoolean(Object from, Converter converter)}</li>
 *   <li>{@code static Character toCharacter(Object from, Converter converter)}</li>
 * </ul>
 *
 * <h2>Test strategy</h2>
 * <ul>
 *   <li>Basic happy‑path tests – verify each method returns the constant
 *       value defined in the source.</li>
 *   <li>Edge‑case tests – pass {@code null} for both parameters and for the
 *       {@code from} argument while still expecting the constant return value.</li>
 *   <li>Constructor test – confirm the private constructor cannot be invoked
 *       without making it accessible (i.e. it throws {@code IllegalAccessException}).</li>
 * </ul>
 */
public class VoidConversionsTest {

    /* --------------------------------------------------------------------- *
     *  Basic functionality tests                                            *
     * --------------------------------------------------------------------- */

    @Test
    public void toNull_returnsNull_withNonNullParameters() {
        Object from = new Object();      // any non‑null object
        Converter converter = null;      // Converter is not used inside the method
        Object result = VoidConversions.toNull(from, converter);
        assertNull("toNull should always return null", result);
    }

    @Test
    public void toNull_returnsNull_withNullParameters() {
        Object result = VoidConversions.toNull(null, null);
        assertNull("toNull should return null even when both arguments are null", result);
    }

    @Test
    public void toBoolean_alwaysReturnsFalse_withNonNullParameters() {
        Boolean result = VoidConversions.toBoolean("any string", null);
        assertSame("toBoolean must always return Boolean.FALSE", Boolean.FALSE, result);
    }

    @Test
    public void toBoolean_alwaysReturnsFalse_withNullFrom() {
        Boolean result = VoidConversions.toBoolean(null, null);
        assertSame("toBoolean must return Boolean.FALSE even when 'from' is null", Boolean.FALSE, result);
    }

    @Test
    public void toCharacter_alwaysReturnsMinValue_withNonNullParameters() {
        Character result = VoidConversions.toCharacter(12345, null);
        assertEquals("toCharacter must return Character.MIN_VALUE",
                     Character.valueOf(Character.MIN_VALUE), result);
    }

    @Test
    public void toCharacter_alwaysReturnsMinValue_withNullFrom() {
        Character result = VoidConversions.toCharacter(null, null);
        assertEquals("toCharacter must return Character.MIN_VALUE even when 'from' is null",
                     Character.valueOf(Character.MIN_VALUE), result);
    }

    /* --------------------------------------------------------------------- *
     *  Edge / exception handling tests                                     *
     * --------------------------------------------------------------------- */

    @Test(expected = IllegalAccessException.class)
    public void privateConstructor_isNotAccessible_withoutSetAccessible()
            throws Exception {
        // Obtain the private no‑arg constructor
        Constructor<VoidConversions> ctor = VoidConversions.class.getDeclaredConstructor();
        // Do NOT make it accessible – invoking should throw IllegalAccessException
        ctor.newInstance();
    }

    // --------------------------------------------------------------------- //
    //  Additional sanity check – the constructor can be invoked via reflection
    //  when we explicitly make it accessible (this is **not** a failure case,
    //  it just proves the class can be instantiated reflectively if needed).
    // --------------------------------------------------------------------- //
    @Test
    public void privateConstructor_canBeInstantiated_whenAccessible() throws Exception {
        Constructor<VoidConversions> ctor = VoidConversions.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        Object instance = ctor.newInstance();
        assertNotNull("Instance created via reflection should not be null", instance);
    }
}
