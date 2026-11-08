package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link ClassUtilities}.
 * <p>
 * The tests cover:
 * <ul>
 *   <li>basic happy‑path functionality of each public method</li>
 *   <li>identified edge‑cases such as null arguments, primitive / wrapper handling, unknown classes, etc.</li>
 *   <li>exception scenarios (e.g., security‑related IllegalArgumentException from {@code forName})</li>
 *   <li>state‑cleanup between tests (alias map is cleared after each test)</li>
 * </ul>
 */
public class ClassUtilitiesTest {

    /* --------------------------------------------------------------------- */
    /*  Helper constants and cleanup                                           */
    /* --------------------------------------------------------------------- */

    private static final String TEST_ALIAS = "myTestAlias";

    @After
    public void cleanupAlias() {
        // Ensure the alias added during a test does not affect other tests
        ClassUtilities.removePermanentClassAlias(TEST_ALIAS);
    }

    /* --------------------------------------------------------------------- */
    /*  addPermanentClassAlias / removePermanentClassAlias                     */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAddAndRemovePermanentClassAlias() {
        // add alias for Integer.class
        ClassUtilities.addPermanentClassAlias(Integer.class, TEST_ALIAS);

        // forName should now resolve the alias
        Class<?> resolved = ClassUtilities.forName(TEST_ALIAS,
                Thread.currentThread().getContextClassLoader());
        assertEquals(Integer.class, resolved);

        // remove alias
        ClassUtilities.removePermanentClassAlias(TEST_ALIAS);

        // after removal, forName should return null (alias no longer known)
        Class<?> afterRemoval = ClassUtilities.forName(TEST_ALIAS,
                Thread.currentThread().getContextClassLoader());
        assertNull(afterRemoval);
    }

    /* --------------------------------------------------------------------- */
    /*  computeInheritanceDistance                                             */
    /* --------------------------------------------------------------------- */

    @Test
    public void testComputeInheritanceDistance_SameClass() {
        assertEquals(0, ClassUtilities.computeInheritanceDistance(String.class, String.class));
    }

    @Test
    public void testComputeInheritanceDistance_NullArguments() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, Object.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(Object.class, null));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, null));
    }

    @Test
    public void testComputeInheritanceDistance_Superclass() {
        // Integer extends Number
        assertEquals(1, ClassUtilities.computeInheritanceDistance(Integer.class, Number.class));
    }

    @Test
    public void testComputeInheritanceDistance_Interface() {
        // ArrayList implements List
        assertEquals(1, ClassUtilities.computeInheritanceDistance(ArrayList.class, List.class));
    }

    @Test
    public void testComputeInheritanceDistance_UnrelatedClasses() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(String.class, Integer.class));
    }

    @Test
    public void testComputeInheritanceDistance_PrimitiveToWrapper() {
        // int primitive to Integer wrapper should be distance 0
        assertEquals(0, ClassUtilities.computeInheritanceDistance(int.class, Integer.class));
    }

    @Test
    public void testComputeInheritanceDistance_PrimitiveToPrimitiveDifferent() {
        // int primitive to long primitive – no conversion allowed
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(int.class, long.class));
    }

    @Test
    public void testComputeInheritanceDistance_WrapperToPrimitive() {
        // Integer wrapper to int primitive – distance 0
        assertEquals(0, ClassUtilities.computeInheritanceDistance(Integer.class, int.class));
    }

    /* --------------------------------------------------------------------- */
    /*  isPrimitive                                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testIsPrimitive_WithPrimitiveAndWrapper() {
        assertTrue(ClassUtilities.isPrimitive(int.class));
        assertTrue(ClassUtilities.isPrimitive(Integer.class));
        assertTrue(ClassUtilities.isPrimitive(void.class));
        assertTrue(ClassUtilities.isPrimitive(Void.class));
    }

    @Test
    public void testIsPrimitive_WithNonPrimitive() {
        assertFalse(ClassUtilities.isPrimitive(String.class));
        assertFalse(ClassUtilities.isPrimitive(Object.class));
    }

    @Test(expected = NullPointerException.class)
    public void testIsPrimitive_NullArgument() {
        ClassUtilities.isPrimitive(null);
    }

    /* --------------------------------------------------------------------- */
    /*  forName                                                                */
    /* --------------------------------------------------------------------- */

    @Test
    public void testForName_NullOrEmpty() {
        assertNull(ClassUtilities.forName(null, getClass().getClassLoader()));
        assertNull(ClassUtilities.forName("", getClass().getClassLoader()));
    }

    @Test
    public void testForName_KnownBuiltin() {
        // "string" alias is predefined in the static block
        Class<?> c = ClassUtilities.forName("string", getClass().getClassLoader());
        assertEquals(String.class, c);
    }

    @Test
    public void testForName_AddedAlias() {
        ClassUtilities.addPermanentClassAlias(Date.class, TEST_ALIAS);
        Class<?> c = ClassUtilities.forName(TEST_ALIAS, getClass().getClassLoader());
        assertEquals(Date.class, c);
    }

    @Test
    public void testForName_UnknownClassReturnsNull() {
        Class<?> c = ClassUtilities.forName("com.this.does.not.Exist", getClass().getClassLoader());
        assertNull(c);
    }

    @Test
    public void testForName_SecurityExceptionConvertedToIllegalArgument() {
        // "java.lang.Class" triggers the security check inside internalClassForName
        try {
            ClassUtilities.forName("java.lang.Class", getClass().getClassLoader());
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Security exception"));
        }
    }

    /* --------------------------------------------------------------------- */
    /*  isClassFinal                                                          */
    /* --------------------------------------------------------------------- */

    @Test
    public void testIsClassFinal_WithFinalClass() {
        assertTrue(ClassUtilities.isClassFinal(String.class)); // String is final
    }

    @Test
    public void testIsClassFinal_WithNonFinalClass() {
        assertFalse(ClassUtilities.isClassFinal(Object.class));
    }

    /* --------------------------------------------------------------------- */
    /*  areAllConstructorsPrivate                                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAreAllConstructorsPrivate_WithAllPrivate() {
        // java.lang.Math has a single private constructor
        assertTrue(ClassUtilities.areAllConstructorsPrivate(Math.class));
    }

    @Test
    public void testAreAllConstructorsPrivate_WithPublicConstructor() {
        // java.lang.String has public constructors
        assertFalse(ClassUtilities.areAllConstructorsPrivate(String.class));
    }

    /* --------------------------------------------------------------------- */
    /*  toPrimitiveWrapperClass                                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToPrimitiveWrapperClass_WithPrimitive() {
        assertEquals(Integer.class, ClassUtilities.toPrimitiveWrapperClass(int.class));
        assertEquals(Void.class, ClassUtilities.toPrimitiveWrapperClass(void.class));
    }

    @Test
    public void testToPrimitiveWrapperClass_WithNonPrimitiveReturnsSame() {
        assertEquals(String.class, ClassUtilities.toPrimitiveWrapperClass(String.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToPrimitiveWrapperClass_WithNonPrimitiveAndNoMapping() {
        // Passing a primitive array class (e.g., int[]) which is not a primitive type
        ClassUtilities.toPrimitiveWrapperClass(int[].class);
    }

    /* --------------------------------------------------------------------- */
    /*  doesOneWrapTheOther                                                    */
    /* --------------------------------------------------------------------- */

    @Test
    public void testDoesOneWrapTheOther_PositiveCases() {
        assertTrue(ClassUtilities.doesOneWrapTheOther(int.class, Integer.class));
        assertTrue(ClassUtilities.doesOneWrapTheOther(Integer.class, int.class));
        assertTrue(ClassUtilities.doesOneWrapTheOther(void.class, Void.class));
    }

    @Test
    public void testDoesOneWrapTheOther_NegativeCases() {
        assertFalse(ClassUtilities.doesOneWrapTheOther(int.class, Long.class));
        assertFalse(ClassUtilities.doesOneWrapTheOther(String.class, String.class));
        assertFalse(ClassUtilities.doesOneWrapTheOther(Object.class, int.class));
    }

    /* --------------------------------------------------------------------- */
    /*  getClassLoader (both overloads)                                        */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetClassLoader_NoArgument() {
        ClassLoader cl = ClassUtilities.getClassLoader();
        assertNotNull(cl);
    }

    @Test
    public void testGetClassLoader_WithAnchorClass() {
        ClassLoader cl = ClassUtilities.getClassLoader(ClassUtilitiesTest.class);
        assertNotNull(cl);
    }

    /* --------------------------------------------------------------------- */
    /*  Additional sanity for internal private methods via reflection (optional) */
    /* --------------------------------------------------------------------- */
    // No direct tests for private methods as per the instructions – they are exercised indirectly
    // through the public API tests above.

}
