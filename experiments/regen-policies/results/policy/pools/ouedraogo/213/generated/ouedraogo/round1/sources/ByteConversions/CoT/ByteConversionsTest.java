package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link ByteConversions}.
 *
 * The class under test is package‑private, therefore the test class must be placed
 * in the same package to have access to its members.
 */
public class ByteConversionsTest {

    // ------------------------------------------------------------------------
    //  List of public (package‑private) methods extracted from ByteConversions
    // ------------------------------------------------------------------------
    // static Character toCharacter(Object from, Converter converter)

    // ------------------------------------------------------------------------
    //  Basic functionality tests
    // ------------------------------------------------------------------------

    @Test
    public void testToCharacter_WithPositiveByte() {
        Byte b = 65;                 // ASCII 'A'
        Character result = ByteConversions.toCharacter(b, null);
        assertEquals(Character.valueOf('A'), result);
    }

    @Test
    public void testToCharacter_WithZeroByte() {
        Byte b = 0;
        Character result = ByteConversions.toCharacter(b, null);
        assertEquals(Character.valueOf('\0'), result);
    }

    @Test
    public void testToCharacter_WithMaximumByte() {
        Byte b = Byte.MAX_VALUE;    // 127
        Character result = ByteConversions.toCharacter(b, null);
        // 127 as a char is the same numeric value
        assertEquals(Character.valueOf((char)127), result);
    }

    @Test
    public void testToCharacter_WithMinimumByte() {
        Byte b = Byte.MIN_VALUE;    // -128
        Character result = ByteConversions.toCharacter(b, null);
        // Casting -128 to char yields 0xFF80 = 65408
        assertEquals(Character.valueOf((char)-128), result);
    }

    @Test
    public void testToCharacter_WithNegativeByte() {
        Byte b = -1;                // 0xFF
        Character result = ByteConversions.toCharacter(b, null);
        // (char)-1 == 0xFFFF == 65535
        assertEquals(Character.valueOf((char)-1), result);
    }

    @Test
    public void testToCharacter_WithNullConverterDoesNotThrow() {
        Byte b = 42;
        // The converter argument is not used inside the method,
        // passing null must not cause any exception.
        Character result = ByteConversions.toCharacter(b, null);
        assertEquals(Character.valueOf((char)42), result);
    }

    // ------------------------------------------------------------------------
    //  Edge‑case & exception handling tests
    // ------------------------------------------------------------------------

    @Test(expected = NullPointerException.class)
    public void testToCharacter_WithNullFromThrowsNPE() {
        // The method casts the 'from' argument to Byte and then dereferences it.
        // Supplying null should therefore result in a NullPointerException.
        ByteConversions.toCharacter(null, null);
    }

    @Test(expected = ClassCastException.class)
    public void testToCharacter_WithNonByteObjectThrowsClassCastException() {
        // Passing an object that is not a Byte forces the explicit cast to fail.
        Integer notAByte = Integer.valueOf(10);
        ByteConversions.toCharacter(notAByte, null);
    }

    // ------------------------------------------------------------------------
    //  Additional sanity check – ensure the private constructor cannot be invoked
    // ------------------------------------------------------------------------

    @Test
    public void testPrivateConstructorIsNotInstantiable() throws Exception {
        // Using reflection to ensure the class cannot be instantiated.
        java.lang.reflect.Constructor<ByteConversions> ctor =
                ByteConversions.class.getDeclaredConstructor();
        assertTrue("Constructor should be private", java.lang.reflect.Modifier.isPrivate(ctor.getModifiers()));
        ctor.setAccessible(true);
        try {
            ctor.newInstance();
            fail("Instantiating ByteConversions via reflection should throw an exception");
        } catch (java.lang.reflect.InvocationTargetException e) {
            // The private constructor does nothing, but the class is final and has no state.
            // The test simply confirms that it is indeed inaccessible in normal use.
            assertTrue(e.getCause() instanceof IllegalAccessException ||
                       e.getCause() instanceof InstantiationException);
        }
    }
}
