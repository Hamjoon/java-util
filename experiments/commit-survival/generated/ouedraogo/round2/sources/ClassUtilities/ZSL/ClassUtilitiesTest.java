package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import static org.junit.Assert.*;

public class ClassUtilitiesTest {

    private ClassLoader contextClassLoader;

    @Before
    public void setUp() {
        // Preserve the original context class loader
        contextClassLoader = Thread.currentThread().getContextClassLoader();
    }

    @After
    public void tearDown() {
        // Restore the original context class loader
        Thread.currentThread().setContextClassLoader(contextClassLoader);
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /* ---------------------------------------------------------------------- */
    /*  Tests for addPermanentClassAlias / removePermanentClassAlias          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testAddAndRemovePermanentClassAlias() {
        // Ensure alias does not exist initially
        assertNull(ClassUtilities.forName("MyString", this.getClass().getClassLoader()));

        // Add alias for java.lang.String
        ClassUtilities.addPermanentClassAlias(String.class, "MyString");
        Class<?> clazz = ClassUtilities.forName("MyString", this.getClass().getClassLoader());
        assertSame(String.class, clazz);

        // Remove alias and verify it is no longer resolvable
        ClassUtilities.removePermanentClassAlias("MyString");
        assertNull(ClassUtilities.forName("MyString", this.getClass().getClassLoader()));
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for computeInheritanceDistance                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testComputeInheritanceDistance_SameClass() {
        assertEquals(0, ClassUtilities.computeInheritanceDistance(String.class, String.class));
    }

    @Test
    public void testComputeInheritanceDistance_Superclass() {
        // Integer -> Number -> Object
        assertEquals(1, ClassUtilities.computeInheritanceDistance(Integer.class, Number.class));
        assertEquals(2, ClassUtilities.computeInheritanceDistance(Integer.class, Object.class));
        // No path upwards
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(Number.class, Integer.class));
    }

    @Test
    public void testComputeInheritanceDistance_InterfaceImplementation() {
        // ArrayList implements List
        assertEquals(1, ClassUtilities.computeInheritanceDistance(ArrayList.class, List.class));
        // HashMap implements Map
        assertEquals(1, ClassUtilities.computeInheritanceDistance(HashMap.class, Map.class));
    }

    @Test
    public void testComputeInheritanceDistance_UnrelatedClasses() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(String.class, Integer.class));
    }

    @Test
    public void testComputeInheritanceDistance_PrimitiveAndWrapper() {
        // Primitive to wrapper (treated as same)
        assertEquals(0, ClassUtilities.computeInheritanceDistance(int.class, Integer.class));
        assertEquals(0, ClassUtilities.computeInheritanceDistance(Integer.class, int.class));
        // Different primitives
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(int.class, long.class));
    }

    @Test
    public void testComputeInheritanceDistance_NullArguments() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, String.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(String.class, null));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, null));
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for isPrimitive                                                  */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testIsPrimitive_PrimitiveAndWrapper() {
        assertTrue(ClassUtilities.isPrimitive(int.class));
        assertTrue(ClassUtilities.isPrimitive(Integer.class));
        assertTrue(ClassUtilities.isPrimitive(boolean.class));
        assertTrue(ClassUtilities.isPrimitive(Boolean.class));
    }

    @Test
    public void testIsPrimitive_NonPrimitive() {
        assertFalse(ClassUtilities.isPrimitive(String.class));
        assertFalse(ClassUtilities.isPrimitive(Object.class));
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for forName (covers internalClassForName & loadClass)            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testForName_NullOrEmpty() {
        assertNull(ClassUtilities.forName(null, this.getClass().getClassLoader()));
        assertNull(ClassUtilities.forName("", this.getClass().getClassLoader()));
    }

    @Test
    public void testForName_StandardClass() {
        Class<?> c = ClassUtilities.forName("java.lang.String", this.getClass().getClassLoader());
        assertSame(String.class, c);
    }

    @Test
    public void testForName_NonExistentClass() {
        assertNull(ClassUtilities.forName("non.existent.FooBar", this.getClass().getClassLoader()));
    }

    @Test
    public void testForName_ArrayClass() {
        // Object array
        Class<?> arrClass = ClassUtilities.forName("[Ljava.lang.String;", this.getClass().getClassLoader());
        assertTrue(arrClass.isArray());
        assertSame(String.class, arrClass.getComponentType());

        // Primitive int array
        Class<?> intArr = ClassUtilities.forName("[I", this.getClass().getClassLoader());
        assertTrue(intArr.isArray());
        assertSame(int.class, intArr.getComponentType());
    }

    @Test
    public void testForName_SecurityRestrictedClass() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Security exception");
        ClassUtilities.forName("java.lang.ClassLoader", this.getClass().getClassLoader());
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for isClassFinal                                                */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testIsClassFinal() {
        assertTrue(ClassUtilities.isClassFinal(String.class)); // final
        assertFalse(ClassUtilities.isClassFinal(ArrayList.class)); // not final
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for areAllConstructorsPrivate                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testAreAllConstructorsPrivate_AllPrivate() {
        assertTrue(ClassUtilities.areAllConstructorsPrivate(PrivateConstructorOnly.class));
    }

    @Test
    public void testAreAllConstructorsPrivate_NotAllPrivate() {
        assertFalse(ClassUtilities.areAllConstructorsPrivate(PublicConstructor.class));
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for toPrimitiveWrapperClass                                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToPrimitiveWrapperClass_Primitive() {
        assertSame(Integer.class, ClassUtilities.toPrimitiveWrapperClass(int.class));
        assertSame(Boolean.class, ClassUtilities.toPrimitiveWrapperClass(boolean.class));
    }

    @Test
    public void testToPrimitiveWrapperClass_NonPrimitiveReturnsItself() {
        assertSame(String.class, ClassUtilities.toPrimitiveWrapperClass(String.class));
    }

    @Test
    public void testToPrimitiveWrapperClass_InvalidArgument() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("not a primitive class");
        ClassUtilities.toPrimitiveWrapperClass(String.class);
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for doesOneWrapTheOther                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testDoesOneWrapTheOther_PositiveCases() {
        assertTrue(ClassUtilities.doesOneWrapTheOther(int.class, Integer.class));
        assertTrue(ClassUtilities.doesOneWrapTheOther(Integer.class, int.class));
        assertTrue(ClassUtilities.doesOneWrapTheOther(boolean.class, Boolean.class));
    }

    @Test
    public void testDoesOneWrapTheOther_NegativeCase() {
        assertFalse(ClassUtilities.doesOneWrapTheOther(int.class, Long.class));
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for getClassLoader                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetClassLoader_Default() {
        ClassLoader cl = ClassUtilities.getClassLoader();
        assertNotNull(cl);
        // The loader should be able to load this test class
        try {
            Class<?> loaded = cl.loadClass(this.getClass().getName());
            assertSame(this.getClass(), loaded);
        } catch (ClassNotFoundException e) {
            fail("Default ClassLoader could not load the test class");
        }
    }

    @Test
    public void testGetClassLoader_WithAnchor() {
        ClassLoader cl = ClassUtilities.getClassLoader(ClassUtilitiesTest.class);
        assertNotNull(cl);
        // Verify that the loader can load a known JDK class
        try {
            Class<?> mapClass = cl.loadClass("java.util.Map");
            assertSame(Map.class, mapClass);
        } catch (ClassNotFoundException e) {
            fail("Anchor ClassLoader could not load java.util.Map");
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  Helper classes for constructor visibility tests                        */
    /* ---------------------------------------------------------------------- */

    private static class PrivateConstructorOnly {
        private PrivateConstructorOnly() {
            // private constructor
        }
    }

    public static class PublicConstructor {
        public PublicConstructor() {
            // public constructor
        }
    }
}
