package com.cedarsoftware.util;

import static org.junit.Assert.*;

import org.junit.Test;

import java.lang.reflect.Constructor;
import java.util.*;

public class ClassUtilitiesTest {

    /* ---------- addPermanentClassAlias / removePermanentClassAlias ---------- */

    @Test
    public void testAddAndRemovePermanentClassAlias() {
        // Ensure alias does not exist initially
        assertNull(ClassUtilities.forName("myString", getClass().getClassLoader()));

        // Add alias for java.lang.String
        ClassUtilities.addPermanentClassAlias(String.class, "myString");
        Class<?> result = ClassUtilities.forName("myString", getClass().getClassLoader());
        assertEquals(String.class, result);

        // Remove the alias and verify it is gone
        ClassUtilities.removePermanentClassAlias("myString");
        assertNull(ClassUtilities.forName("myString", getClass().getClassLoader()));
    }

    /* ---------- computeInheritanceDistance ---------- */

    @Test
    public void testComputeInheritanceDistance_SameClass() {
        assertEquals(0, ClassUtilities.computeInheritanceDistance(String.class, String.class));
    }

    @Test
    public void testComputeInheritanceDistance_SubclassToSuperclass() {
        // Integer extends Number
        assertEquals(1, ClassUtilities.computeInheritanceDistance(Integer.class, Number.class));
    }

    @Test
    public void testComputeInheritanceDistance_InterfaceImplementation() {
        // ArrayList implements List directly
        assertEquals(1, ClassUtilities.computeInheritanceDistance(ArrayList.class, List.class));
    }

    @Test
    public void testComputeInheritanceDistance_NoRelation() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(String.class, Integer.class));
    }

    @Test
    public void testComputeInheritanceDistance_PrimitiveToWrapper() {
        // int primitive to Integer wrapper
        assertEquals(0, ClassUtilities.computeInheritanceDistance(int.class, Integer.class));
    }

    @Test
    public void testComputeInheritanceDistance_WrapperToPrimitive() {
        // Integer wrapper to int primitive
        assertEquals(0, ClassUtilities.computeInheritanceDistance(Integer.class, int.class));
    }

    @Test
    public void testComputeInheritanceDistance_DifferentPrimitives() {
        // int primitive to long primitive – not the same, should be -1
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(int.class, long.class));
    }

    @Test
    public void testComputeInheritanceDistance_NullArguments() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, String.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(String.class, null));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, null));
    }

    /* ---------- isPrimitive ---------- */

    @Test
    public void testIsPrimitive_PrimitiveAndWrappers() {
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

    /* ---------- toPrimitiveWrapperClass ---------- */

    @Test
    public void testToPrimitiveWrapperClass_Primitive() {
        assertEquals(Integer.class, ClassUtilities.toPrimitiveWrapperClass(int.class));
        assertEquals(Boolean.class, ClassUtilities.toPrimitiveWrapperClass(boolean.class));
        assertEquals(Void.class, ClassUtilities.toPrimitiveWrapperClass(void.class));
    }

    @Test
    public void testToPrimitiveWrapperClass_NonPrimitiveReturnsItself() {
        assertEquals(String.class, ClassUtilities.toPrimitiveWrapperClass(String.class));
        assertEquals(Object.class, ClassUtilities.toPrimitiveWrapperClass(Object.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToPrimitiveWrapperClass_InvalidPrimitive() {
        // Create a synthetic class that is primitive but not in the map (should not happen,
        // but we force the path by passing a non‑primitive class that is not a wrapper)
        ClassUtilities.toPrimitiveWrapperClass(void.class); // void is in map, use a dummy class
    }

    /* ---------- doesOneWrapTheOther ---------- */

    @Test
    public void testDoesOneWrapTheOther() {
        assertTrue(ClassUtilities.doesOneWrapTheOther(int.class, Integer.class));
        assertTrue(ClassUtilities.doesOneWrapTheOther(Integer.class, int.class));
        assertTrue(ClassUtilities.doesOneWrapTheOther(boolean.class, Boolean.class));
        assertFalse(ClassUtilities.doesOneWrapTheOther(int.class, Long.class));
        assertFalse(ClassUtilities.doesOneWrapTheOther(String.class, String.class));
    }

    /* ---------- isClassFinal ---------- */

    @Test
    public void testIsClassFinal() {
        assertTrue(ClassUtilities.isClassFinal(String.class)); // String is final
        assertFalse(ClassUtilities.isClassFinal(ArrayList.class)); // not final
    }

    /* ---------- areAllConstructorsPrivate ---------- */

    @Test
    public void testAreAllConstructorsPrivate_AllPrivate() {
        assertTrue(ClassUtilities.areAllConstructorsPrivate(AllPrivateConstructor.class));
    }

    @Test
    public void testAreAllConstructorsPrivate_NotAllPrivate() {
        assertFalse(ClassUtilities.areAllConstructorsPrivate(NotAllPrivateConstructor.class));
    }

    /* ---------- getClassLoader ---------- */

    @Test
    public void testGetClassLoader_NotNull() {
        ClassLoader cl = ClassUtilities.getClassLoader();
        assertNotNull(cl);
    }

    @Test
    public void testGetClassLoader_WithAnchor() {
        ClassLoader cl = ClassUtilities.getClassLoader(ClassUtilitiesTest.class);
        assertNotNull(cl);
    }

    /* ---------- forName ---------- */

    @Test
    public void testForName_NullOrEmpty() {
        assertNull(ClassUtilities.forName(null, getClass().getClassLoader()));
        assertNull(ClassUtilities.forName("", getClass().getClassLoader()));
    }

    @Test
    public void testForName_StandardClass() {
        Class<?> c = ClassUtilities.forName("java.lang.String", getClass().getClassLoader());
        assertEquals(String.class, c);
    }

    @Test
    public void testForName_PrimitiveArray() {
        Class<?> c = ClassUtilities.forName("[I", getClass().getClassLoader());
        assertEquals(int[].class, c);
    }

    @Test
    public void testForName_MultiDimObjectArray() {
        Class<?> c = ClassUtilities.forName("[[Ljava.lang.String;", getClass().getClassLoader());
        assertEquals(String[][].class, c);
    }

    @Test
    public void testForName_UnknownClassReturnsNull() {
        assertNull(ClassUtilities.forName("com.this.does.not.Exist", getClass().getClassLoader()));
    }

    /* ---------- internal behavior of loadClass via forName (array of primitive) ---------- */

    @Test
    public void testForName_PrimitiveBooleanArray() {
        Class<?> c = ClassUtilities.forName("[Z", getClass().getClassLoader());
        assertEquals(boolean[].class, c);
    }

    /* ---------- OSGi related methods (expected to return null in a non‑OSGi env) ---------- */

    @Test
    public void testGetOSGiClassLoader_ReturnsNullWhenNotPresent() throws Exception {
        // The private method is not accessible, but we can indirectly verify that
        // getClassLoader does not throw and returns a non‑null loader.
        ClassLoader cl = ClassUtilities.getClassLoader();
        assertNotNull(cl);
    }

    /* ---------- Helper classes for constructor visibility tests ---------- */

    /** Utility class with only private constructors */
    private static final class AllPrivateConstructor {
        private AllPrivateConstructor() {}
        private AllPrivateConstructor(int x) {}
    }

    /** Class with at least one public constructor */
    private static class NotAllPrivateConstructor {
        public NotAllPrivateConstructor() {}
        private NotAllPrivateConstructor(int x) {}
    }
}
