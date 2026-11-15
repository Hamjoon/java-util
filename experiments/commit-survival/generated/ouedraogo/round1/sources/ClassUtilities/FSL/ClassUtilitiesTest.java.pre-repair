package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit4 test suite for {@link ClassUtilities}.
 */
public class ClassUtilitiesTest {

    /** Reset the static alias map before each test to avoid cross‑test pollution. */
    @Before
    public void setUp() {
        // Ensure any custom aliases added by a previous test are removed.
        // The test suite only adds the alias "myDate".  Remove it if present.
        ClassUtilities.removePermanentClassAlias("myDate");
    }

    /*--------------------------------------------------------------------
     *  Tests for computeInheritanceDistance
     *--------------------------------------------------------------------*/

    @Test
    public void testComputeInheritanceDistance_SameClass() {
        assertEquals(0, ClassUtilities.computeInheritanceDistance(String.class, String.class));
    }

    @Test
    public void testComputeInheritanceDistance_Superclass() {
        // String -> Object distance = 1
        assertEquals(1, ClassUtilities.computeInheritanceDistance(String.class, Object.class));
    }

    @Test
    public void testComputeInheritanceDistance_InterfaceImplementation() {
        // ArrayList implements List -> distance 1
        assertEquals(1, ClassUtilities.computeInheritanceDistance(java.util.ArrayList.class, java.util.List.class));
    }

    @Test
    public void testComputeInheritanceDistance_MultiStep() {
        // Integer -> Number -> Object  distance = 2
        assertEquals(2, ClassUtilities.computeInheritanceDistance(Integer.class, Object.class));
    }

    @Test
    public void testComputeInheritanceDistance_NoRelation() {
        // String and Integer are unrelated (except via Object, but we ask for Integer->String)
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(String.class, Integer.class));
    }

    @Test
    public void testComputeInheritanceDistance_PrimitiveAndWrapper() {
        // int (primitive) to Integer (wrapper) should be distance 0
        assertEquals(0, ClassUtilities.computeInheritanceDistance(int.class, Integer.class));
        // Integer (wrapper) to int (primitive) also 0
        assertEquals(0, ClassUtilities.computeInheritanceDistance(Integer.class, int.class));
    }

    @Test
    public void testComputeInheritanceDistance_PrimitiveMismatch() {
        // int to Long – not the same primitive, distance -1
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(int.class, Long.class));
        // Long wrapper to int primitive – also -1
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(Long.class, int.class));
    }

    @Test
    public void testComputeInheritanceDistance_NullArguments() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, Object.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(Object.class, null));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, null));
    }

    /*--------------------------------------------------------------------
     *  Tests for isPrimitive
     *--------------------------------------------------------------------*/

    @Test
    public void testIsPrimitive_PrimitiveTypes() {
        assertTrue(ClassUtilities.isPrimitive(int.class));
        assertTrue(ClassUtilities.isPrimitive(boolean.class));
        assertTrue(ClassUtilities.isPrimitive(void.class));
    }

    @Test
    public void testIsPrimitive_WrapperTypes() {
        assertTrue(ClassUtilities.isPrimitive(Integer.class));
        assertTrue(ClassUtilities.isPrimitive(Boolean.class));
        assertTrue(ClassUtilities.isPrimitive(Void.class));
    }

    @Test
    public void testIsPrimitive_NonPrimitive() {
        assertFalse(ClassUtilities.isPrimitive(String.class));
        assertFalse(ClassUtilities.isPrimitive(Object.class));
    }

    /*--------------------------------------------------------------------
     *  Tests for toPrimitiveWrapperClass
     *--------------------------------------------------------------------*/

    @Test
    public void testToPrimitiveWrapperClass_Primitive() {
        assertEquals(Integer.class, ClassUtilities.toPrimitiveWrapperClass(int.class));
        assertEquals(Boolean.class, ClassUtilities.toPrimitiveWrapperClass(boolean.class));
        assertEquals(Void.class, ClassUtilities.toPrimitiveWrapperClass(void.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToPrimitiveWrapperClass_NonPrimitiveThrows() {
        ClassUtilities.toPrimitiveWrapperClass(String.class);
    }

    /*--------------------------------------------------------------------
     *  Tests for doesOneWrapTheOther
     *--------------------------------------------------------------------*/

    @Test
    public void testDoesOneWrapTheOther_PositiveCases() {
        assertTrue(ClassUtilities.doesOneWrapTheOther(int.class, Integer.class));
        assertTrue(ClassUtilities.doesOneWrapTheOther(Integer.class, int.class));
        assertTrue(ClassUtilities.doesOneWrapTheOther(char.class, Character.class));
    }

    @Test
    public void testDoesOneWrapTheOther_NegativeCases() {
        assertFalse(ClassUtilities.doesOneWrapTheOther(int.class, Long.class));
        assertFalse(ClassUtilities.doesOneWrapTheOther(String.class, Integer.class));
        assertFalse(ClassUtilities.doesOneWrapTheOther(void.class, Void.class)); // void is not in wrapperMap
    }

    /*--------------------------------------------------------------------
     *  Tests for isClassFinal
     *--------------------------------------------------------------------*/

    @Test
    public void testIsClassFinal() {
        assertTrue(ClassUtilities.isClassFinal(String.class)); // String is final
        assertFalse(ClassUtilities.isClassFinal(Object.class));
        // Create a local final class for extra safety
        final class LocalFinal {}
        assertTrue(ClassUtilities.isClassFinal(LocalFinal.class));
    }

    /*--------------------------------------------------------------------
     *  Tests for areAllConstructorsPrivate
     *--------------------------------------------------------------------*/

    @Test
    public void testAreAllConstructorsPrivate_AllPrivate() {
        class AllPrivate {
            private AllPrivate() {}
            private AllPrivate(int x) {}
        }
        assertTrue(ClassUtilities.areAllConstructorsPrivate(AllPrivate.class));
    }

    @Test
    public void testAreAllConstructorsPrivate_NotAllPrivate() {
        class Mixed {
            private Mixed() {}
            public Mixed(String s) {}
        }
        assertFalse(ClassUtilities.areAllConstructorsPrivate(Mixed.class));
    }

    /*--------------------------------------------------------------------
     *  Tests for addPermanentClassAlias / removePermanentClassAlias / forName
     *--------------------------------------------------------------------*/

    @Test
    public void testAliasAddRemoveAndForName() {
        // Alias "myDate" to java.util.Date
        ClassUtilities.addPermanentClassAlias(Date.class, "myDate");

        // The alias should be resolved by forName without needing a class loader
        Class<?> loaded = ClassUtilities.forName("myDate", this.getClass().getClassLoader());
        assertEquals(Date.class, loaded);

        // Remove the alias and verify that forName now returns null
        ClassUtilities.removePermanentClassAlias("myDate");
        assertNull(ClassUtilities.forName("myDate", this.getClass().getClassLoader()));
    }

    @Test
    public void testForName_StandardClasses() {
        Class<?> stringClass = ClassUtilities.forName("string", this.getClass().getClassLoader());
        assertEquals(String.class, stringClass);

        Class<?> intClass = ClassUtilities.forName("int", this.getClass().getClassLoader());
        assertEquals(Integer.TYPE, intClass);
    }

    @Test
    public void testForName_InvalidNameReturnsNull() {
        assertNull(ClassUtilities.forName("non.existent.ClassName", this.getClass().getClassLoader()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testForName_SecurityExceptionIsWrapped() throws Exception {
        // Simulate a security exception by aliasing a forbidden class name.
        // The internal logic throws SecurityException for certain JDK classes (e.g., java.lang.Process).
        // We use the alias mechanism to force that path.
        ClassUtilities.addPermanentClassAlias(Process.class, "java.lang.Process");
        // The call will go through internalClassForName which will detect the forbidden class and throw.
        ClassUtilities.forName("java.lang.Process", this.getClass().getClassLoader());
    }

    /*--------------------------------------------------------------------
     *  Tests for getClassLoader (basic sanity)
     *--------------------------------------------------------------------*/

    @Test
    public void testGetClassLoader_NotNull() {
        ClassLoader cl = ClassUtilities.getClassLoader();
        assertNotNull(cl);
        // The returned loader should be able to load this test class.
        assertEquals(ClassUtilitiesTest.class, ClassUtilities.forName(ClassUtilitiesTest.class.getName(), cl));
    }

    /*--------------------------------------------------------------------
     *  Tests for internal behavior of loadClass (array handling)
     *--------------------------------------------------------------------*/

    @Test
    public void testLoadClass_ArrayNotation() throws Exception {
        // Use reflection to invoke the private loadClass method
        java.lang.reflect.Method m = ClassUtilities.class.getDeclaredMethod("loadClass", String.class, ClassLoader.class);
        m.setAccessible(true);
        ClassLoader cl = this.getClass().getClassLoader();

        // Primitive array "[I" should give int[]
        Class<?> intArray = (Class<?>) m.invoke(null, "[I", cl);
        assertTrue(intArray.isArray());
        assertEquals(int.class, intArray.getComponentType());

        // Object array "[[Ljava.lang.String;" should give String[][]
        Class<?> string2D = (Class<?>) m.invoke(null, "[[Ljava.lang.String;", cl);
        assertTrue(string2D.isArray());
        assertTrue(string2D.getComponentType().isArray());
        assertEquals(String.class, string2D.getComponentType().getComponentType());
    }

    /*--------------------------------------------------------------------
     *  Tests for getOSGiClassLoader private fallback (ensuring it returns null when OSGi not present)
     *--------------------------------------------------------------------*/

    @Test
    public void testGetOSGiClassLoaderReturnsNullWhenNoOSGi() throws Exception {
        java.lang.reflect.Method method = ClassUtilities.class.getDeclaredMethod("getOSGiClassLoader", Class.class);
        method.setAccessible(true);
        ClassLoader result = (ClassLoader) method.invoke(null, this.getClass());
        // In a normal test environment the OSGi framework is absent, thus result should be null.
        assertNull(result);
    }
}
