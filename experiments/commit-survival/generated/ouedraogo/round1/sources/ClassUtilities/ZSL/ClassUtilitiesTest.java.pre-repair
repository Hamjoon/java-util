package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.*;

import static org.junit.Assert.*;

public class ClassUtilitiesTest {

    /* --------------------------------------------------------------------- */
    /*  Helpers                                                               */
    /* --------------------------------------------------------------------- */

    /** Simple class with only private constructors – used for areAllConstructorsPrivate() */
    private static final class PrivateCtor {
        private PrivateCtor() {}
        private PrivateCtor(int x) {}
    }

    /** Simple class with a public constructor – used for areAllConstructorsPrivate() */
    private static final class PublicCtor {
        public PublicCtor() {}
    }

    /** Interface hierarchy for computeInheritanceDistance() tests */
    private interface A {}
    private interface B extends A {}
    private static class SuperClass implements B {}
    private static class SubClass extends SuperClass {}

    /* --------------------------------------------------------------------- */
    /*  Lifecycle                                                             */
    /* --------------------------------------------------------------------- */

    @Before
    public void setUp() {
        // Ensure the alias map is clean for each test
        ClassUtilities.removePermanentClassAlias("TmpAlias");
    }

    @After
    public void tearDown() {
        // Clean any alias we may have added
        ClassUtilities.removePermanentClassAlias("TmpAlias");
    }

    /* --------------------------------------------------------------------- */
    /*  computeInheritanceDistance                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testComputeInheritanceDistance_SameClass() {
        assertEquals(0, ClassUtilities.computeInheritanceDistance(String.class, String.class));
    }

    @Test
    public void testComputeInheritanceDistance_Superclass() {
        // SubClass -> SuperClass is one step
        assertEquals(1, ClassUtilities.computeInheritanceDistance(SubClass.class, SuperClass.class));
    }

    @Test
    public void testComputeInheritanceDistance_Interface() {
        // SubClass -> B (implemented by SuperClass) is one step
        assertEquals(1, ClassUtilities.computeInheritanceDistance(SubClass.class, B.class));
        // SubClass -> A (parent of B) is two steps
        assertEquals(2, ClassUtilities.computeInheritanceDistance(SubClass.class, A.class));
    }

    @Test
    public void testComputeInheritanceDistance_NoRelation() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(String.class, Integer.class));
    }

    @Test
    public void testComputeInheritanceDistance_PrimitiveAndWrapper() {
        // int primitive to Integer wrapper => distance 0 (treated as same)
        assertEquals(0, ClassUtilities.computeInheritanceDistance(int.class, Integer.class));
        // Integer wrapper to int primitive => distance 0
        assertEquals(0, ClassUtilities.computeInheritanceDistance(Integer.class, int.class));
    }

    @Test
    public void testComputeInheritanceDistance_WrongPrimitiveCombination() {
        // int primitive to Long wrapper => -1 (different primitive)
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(int.class, Long.class));
        // Long wrapper to int primitive => -1
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(Long.class, int.class));
    }

    @Test
    public void testComputeInheritanceDistance_NullArguments() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, String.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(String.class, null));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, null));
    }

    /* --------------------------------------------------------------------- */
    /*  isPrimitive                                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testIsPrimitive_PrimitiveAndWrapper() {
        assertTrue(ClassUtilities.isPrimitive(int.class));
        assertTrue(ClassUtilities.isPrimitive(Integer.class));
        assertTrue(ClassUtilities.isPrimitive(void.class));
        assertTrue(ClassUtilities.isPrimitive(Void.class));
    }

    @Test
    public void testIsPrimitive_NonPrimitive() {
        assertFalse(ClassUtilities.isPrimitive(String.class));
        assertFalse(ClassUtilities.isPrimitive(Object.class));
    }

    /* --------------------------------------------------------------------- */
    /*  addPermanentClassAlias / removePermanentClassAlias                     */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAliasManagement() {
        Class<?> target = Date.class;
        String alias = "TmpAlias";

        // initially not present
        assertNull(ClassUtilities.forName(alias, ClassUtilities.class.getClassLoader()));

        // add alias and verify forName resolves it
        ClassUtilities.addPermanentClassAlias(target, alias);
        assertEquals(target, ClassUtilities.forName(alias, ClassUtilities.class.getClassLoader()));

        // remove alias and verify it is no longer resolvable
        ClassUtilities.removePermanentClassAlias(alias);
        assertNull(ClassUtilities.forName(alias, ClassUtilities.class.getClassLoader()));
    }

    /* --------------------------------------------------------------------- */
    /*  forName (including array handling and security exception)            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testForName_SimpleClass() {
        Class<?> c = ClassUtilities.forName("java.lang.String", ClassUtilities.class.getClassLoader());
        assertEquals(String.class, c);
    }

    @Test
    public void testForName_PrimitiveAlias() {
        // "int" is mapped in the static initializer
        Class<?> c = ClassUtilities.forName("int", ClassUtilities.class.getClassLoader());
        assertEquals(int.class, c);
    }

    @Test
    public void testForName_ArrayNotation() {
        // int[]
        Class<?> c = ClassUtilities.forName("[I", ClassUtilities.class.getClassLoader());
        assertTrue(c.isArray());
        assertEquals(int.class, c.getComponentType());

        // String[]
        Class<?> cs = ClassUtilities.forName("[Ljava.lang.String;", ClassUtilities.class.getClassLoader());
        assertTrue(cs.isArray());
        assertEquals(String.class, cs.getComponentType());

        // multi‑dimensional array [[Ljava.lang.Integer;
        Class<?> cm = ClassUtilities.forName("[[Ljava.lang.Integer;", ClassUtilities.class.getClassLoader());
        assertTrue(cm.isArray());
        assertTrue(cm.getComponentType().isArray());
        assertEquals(Integer.class, cm.getComponentType().getComponentType());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testForName_SecuritySensitiveClass() {
        // ClassLoader is on the forbidden list; forName should wrap the SecurityException
        ClassUtilities.forName("java.lang.ClassLoader", ClassUtilities.class.getClassLoader());
    }

    @Test
    public void testForName_NonExistingClassReturnsNull() {
        assertNull(ClassUtilities.forName("com.cedarsoftware.util.DoesNotExist", ClassUtilities.class.getClassLoader()));
    }

    /* --------------------------------------------------------------------- */
    /*  isClassFinal                                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testIsClassFinal() {
        assertTrue(ClassUtilities.isClassFinal(String.class)); // String is final
        assertFalse(ClassUtilities.isClassFinal(Object.class)); // Object is not final
    }

    /* --------------------------------------------------------------------- */
    /*  areAllConstructorsPrivate                                                */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAllConstructorsPrivate_True() {
        assertTrue(ClassUtilities.areAllConstructorsPrivate(PrivateCtor.class));
    }

    @Test
    public void testAllConstructorsPrivate_False() {
        assertFalse(ClassUtilities.areAllConstructorsPrivate(PublicCtor.class));
    }

    /* --------------------------------------------------------------------- */
    /*  toPrimitiveWrapperClass                                                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToPrimitiveWrapperClass_Primitive() {
        assertEquals(Integer.class, ClassUtilities.toPrimitiveWrapperClass(int.class));
        assertEquals(Void.class, ClassUtilities.toPrimitiveWrapperClass(void.class));
    }

    @Test
    public void testToPrimitiveWrapperClass_NonPrimitiveReturnsItself() {
        assertEquals(String.class, ClassUtilities.toPrimitiveWrapperClass(String.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToPrimitiveWrapperClass_InvalidPrimitive() {
        // Pass a class that is not a primitive (e.g., an array) – should throw
        ClassUtilities.toPrimitiveWrapperClass(int[].class);
    }

    /* --------------------------------------------------------------------- */
    /*  doesOneWrapTheOther                                                     */
    /* --------------------------------------------------------------------- */

    @Test
    public void testDoesOneWrapTheOther() {
        assertTrue(ClassUtilities.doesOneWrapTheOther(int.class, Integer.class));
        assertTrue(ClassUtilities.doesOneWrapTheOther(Integer.class, int.class));
        assertFalse(ClassUtilities.doesOneWrapTheOther(int.class, Long.class));
        assertFalse(ClassUtilities.doesOneWrapTheOther(String.class, String.class));
    }

    /* --------------------------------------------------------------------- */
    /*  getClassLoader                                                          */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetClassLoader_Fallbacks() {
        // In a normal JVM (no OSGi), getClassLoader should never return null.
        ClassLoader cl = ClassUtilities.getClassLoader();
        assertNotNull(cl);

        // Explicit anchor class version should also return a non‑null loader.
        ClassLoader cl2 = ClassUtilities.getClassLoader(ClassUtilitiesTest.class);
        assertNotNull(cl2);
        // Both calls should return the same loader (the thread context loader in this test environment)
        assertEquals(cl, cl2);
    }

    /* --------------------------------------------------------------------- */
    /*  internal sanity check for private static cache (optional)             */
    /* --------------------------------------------------------------------- */

    @Test
    public void testComputeInheritanceDistance_CacheDoesNotAffectResult() {
        // First call populates the internal nameToClass cache for "java.lang.String"
        Class<?> first = ClassUtilities.forName("java.lang.String", ClassUtilities.class.getClassLoader());
        assertEquals(String.class, first);

        // Second call should retrieve from cache and still be correct
        Class<?> second = ClassUtilities.forName("java.lang.String", ClassUtilities.class.getClassLoader());
        assertSame(first, second);
    }
}
