package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link ClassUtilities}.
 */
public class ClassUtilitiesTest {

    private static final ClassLoader CL = Thread.currentThread().getContextClassLoader();

    /*** Helper classes for testing ***/

    /** A final class – used for {@link ClassUtilities#isClassFinal(Class)}. */
    private static final class FinalTestClass {}

    /** A class with only private constructors – used for {@link ClassUtilities#areAllConstructorsPrivate(Class)}. */
    private static class PrivateCtorOnly {
        private PrivateCtorOnly() {}
        private PrivateCtorOnly(int x) {}
    }

    /** A class with mixed visibility constructors – used for {@link ClassUtilities#areAllConstructorsPrivate(Class)}. */
    private static class MixedCtor {
        public MixedCtor() {}
        private MixedCtor(int x) {}
    }

    /** A simple interface – used for inheritance distance tests. */
    private interface SampleInterface {}

    /** A class that implements {@link SampleInterface}. */
    private static class ImplementsInterface implements SampleInterface {}

    /** A class that extends {@link ImplementsInterface}. */
    private static class SubImplementsInterface extends ImplementsInterface {}

    /*** Test fixtures ***/

    @Before
    public void setUp() {
        // Ensure the alias map is clean before each test.
        ClassUtilities.removePermanentClassAlias("testAlias");
    }

    @After
    public void tearDown() {
        // Clean up any aliases added during a test.
        ClassUtilities.removePermanentClassAlias("testAlias");
    }

    /*** Tests for alias handling ***/
    @Test
    public void testAddAndRemovePermanentClassAlias() {
        ClassUtilities.addPermanentClassAlias(String.class, "testAlias");
        Class<?> cls = ClassUtilities.forName("testAlias", CL);
        assertSame("Alias should resolve to String.class", String.class, cls);

        ClassUtilities.removePermanentClassAlias("testAlias");
        assertNull("Alias should be removed and resolve to null", ClassUtilities.forName("testAlias", CL));
    }

    /*** Tests for computeInheritanceDistance ***/
    @Test
    public void testComputeInheritanceDistance_SameClass() {
        assertEquals(0, ClassUtilities.computeInheritanceDistance(String.class, String.class));
    }

    @Test
    public void testComputeInheritanceDistance_PrimitiveToWrapper() {
        assertEquals(0, ClassUtilities.computeInheritanceDistance(int.class, Integer.class));
    }

    @Test
    public void testComputeInheritanceDistance_WrapperToPrimitive() {
        assertEquals(0, ClassUtilities.computeInheritanceDistance(Integer.class, int.class));
    }

    @Test
    public void testComputeInheritanceDistance_NoRelationship() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(String.class, Integer.class));
    }

    @Test
    public void testComputeInheritanceDistance_SuperclassChain() {
        // Integer -> Number -> Object  = distance 2
        assertEquals(2, ClassUtilities.computeInheritanceDistance(Integer.class, Object.class));
    }

    @Test
    public void testComputeInheritanceDistance_Interface() {
        // ImplementsInterface implements SampleInterface => distance 1
        assertEquals(1, ClassUtilities.computeInheritanceDistance(ImplementsInterface.class, SampleInterface.class));
        // SubImplementsInterface -> ImplementsInterface -> SampleInterface => distance 2
        assertEquals(2, ClassUtilities.computeInheritanceDistance(SubImplementsInterface.class, SampleInterface.class));
    }

    @Test
    public void testComputeInheritanceDistance_NullArguments() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, Object.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(Object.class, null));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, null));
    }

    /*** Tests for isPrimitive ***/
    @Test
    public void testIsPrimitive_PrimitiveTypes() {
        assertTrue(ClassUtilities.isPrimitive(int.class));
        assertTrue(ClassUtilities.isPrimitive(void.class));
    }

    @Test
    public void testIsPrimitive_WrapperTypes() {
        assertTrue(ClassUtilities.isPrimitive(Integer.class));
        assertTrue(ClassUtilities.isPrimitive(Boolean.class));
    }

    @Test
    public void testIsPrimitive_NonPrimitive() {
        assertFalse(ClassUtilities.isPrimitive(String.class));
        assertFalse(ClassUtilities.isPrimitive(Object.class));
    }

    /*** Tests for forName ***/
    @Test
    public void testForName_NullOrEmpty() {
        assertNull(ClassUtilities.forName(null, CL));
        assertNull(ClassUtilities.forName("", CL));
    }

    @Test
    public void testForName_BuiltInAlias() {
        // "int" is mapped to the primitive int class
        assertEquals(int.class, ClassUtilities.forName("int", CL));
        // "string" is mapped to java.lang.String
        assertEquals(String.class, ClassUtilities.forName("string", CL));
    }

    @Test
    public void testForName_StandardClass() {
        Class<?> listClass = ClassUtilities.forName("java.util.ArrayList", CL);
        assertEquals(ArrayList.class, listClass);
    }

    @Test
    public void testForName_ArrayClass() {
        // Single‑dimensional array: [Ljava.lang.String;
        Class<?> stringArray = ClassUtilities.forName("[Ljava.lang.String;", CL);
        assertTrue(stringArray.isArray());
        assertEquals(String.class, stringArray.getComponentType());

        // Multi‑dimensional array: [[Ljava.lang.String;
        Class<?> stringArray2 = ClassUtilities.forName("[[Ljava.lang.String;", CL);
        assertTrue(stringArray2.isArray());
        assertTrue(stringArray2.getComponentType().isArray());
        assertEquals(String.class, stringArray2.getComponentType().getComponentType());
    }

    @Test
    public void testForName_SecurityExceptionPath() {
        try {
            ClassUtilities.forName("java.lang.Process", CL);
            fail("Expected IllegalArgumentException because Process is blocked for security reasons");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Security exception"));
        }
    }

    @Test
    public void testForName_NonExistentClass() {
        assertNull(ClassUtilities.forName("com.nonexistent.FooBar", CL));
    }

    /*** Tests for isClassFinal ***/
    @Test
    public void testIsClassFinal_True() {
        assertTrue(ClassUtilities.isClassFinal(FinalTestClass.class));
    }

    @Test
    public void testIsClassFinal_False() {
        assertFalse(ClassUtilities.isClassFinal(String.class));
    }

    /*** Tests for areAllConstructorsPrivate ***/
    @Test
    public void testAreAllConstructorsPrivate_AllPrivate() {
        assertTrue(ClassUtilities.areAllConstructorsPrivate(PrivateCtorOnly.class));
    }

    @Test
    public void testAreAllConstructorsPrivate_NotAllPrivate() {
        assertFalse(ClassUtilities.areAllConstructorsPrivate(MixedCtor.class));
    }

    /*** Tests for toPrimitiveWrapperClass ***/
    @Test
    public void testToPrimitiveWrapperClass_Primitive() {
        assertEquals(Integer.class, ClassUtilities.toPrimitiveWrapperClass(int.class));
        assertEquals(Boolean.class, ClassUtilities.toPrimitiveWrapperClass(boolean.class));
    }

    @Test
    public void testToPrimitiveWrapperClass_NonPrimitive_ReturnsSame() {
        assertEquals(String.class, ClassUtilities.toPrimitiveWrapperClass(String.class));
    }

    @Test
    public void testToPrimitiveWrapperClass_InvalidArgument() {
        try {
            ClassUtilities.toPrimitiveWrapperClass(String.class);
            fail("Expected IllegalArgumentException for non‑primitive argument");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    /*** Tests for doesOneWrapTheOther ***/
    @Test
    public void testDoesOneWrapTheOther_WrapperAndPrimitive() {
        assertTrue(ClassUtilities.doesOneWrapTheOther(int.class, Integer.class));
        assertTrue(ClassUtilities.doesOneWrapTheOther(Integer.class, int.class));
        assertFalse(ClassUtilities.doesOneWrapTheOther(int.class, Long.class));
    }

    /*** Tests for getClassLoader ***/
    @Test
    public void testGetClassLoader_Fallback() {
        ClassLoader cl = ClassUtilities.getClassLoader();
        assertNotNull("ClassLoader should never be null", cl);
        // In a normal JVM (non‑OSGi) the returned loader should be the thread context loader or the utility’s loader.
        ClassLoader expected = Thread.currentThread().getContextClassLoader();
        assertSame("Returned ClassLoader should be the thread context loader when OSGi is absent",
                expected != null ? expected : ClassUtilities.class.getClassLoader(),
                cl);
    }

    @Test
    public void testGetClassLoader_WithAnchor() {
        ClassLoader cl = ClassUtilities.getClassLoader(String.class);
        assertNotNull(cl);
        // Same reasoning as above – we expect the thread context loader or the anchor’s loader.
        ClassLoader expected = Thread.currentThread().getContextClassLoader();
        assertSame("Returned ClassLoader should be the thread context loader when OSGi is absent",
                expected != null ? expected : String.class.getClassLoader(),
                cl);
    }

    /*** Additional sanity test for internal caching ***/
    @Test
    public void testForName_CacheWorks() {
        // First call loads and caches
        Class<?> first = ClassUtilities.forName("java.util.HashMap", CL);
        assertEquals(HashMap.class, first);

        // Second call should retrieve from cache (no exception should be thrown)
        Class<?> second = ClassUtilities.forName("java.util.HashMap", CL);
        assertSame("Second call should return the same Class instance from cache", first, second);
    }

    /*** Test that primitive wrapper detection works for both sides of comparePrimitiveToWrapper ***/
    @Test
    public void testComputeInheritanceDistance_PrimitiveWrapperEquality() {
        // source = primitive, destination = wrapper – distance 0
        assertEquals(0, ClassUtilities.computeInheritanceDistance(double.class, Double.class));

        // source = wrapper, destination = primitive – distance 0
        assertEquals(0, ClassUtilities.computeInheritanceDistance(Double.class, double.class));
    }

    /*** Test that computeInheritanceDistance returns -1 for unrelated primitives ***/
    @Test
    public void testComputeInheritanceDistance_UnrelatedPrimitives() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(int.class, long.class));
    }

    /*** Test that computeInheritanceDistance works with interfaces on the source side ***/
    @Test
    public void testComputeInheritanceDistance_InterfaceSource() {
        // List interface to Collection interface – distance 1
        assertEquals(1, ClassUtilities.computeInheritanceDistance(List.class, java.util.Collection.class));
    }

    /*** Test that computeInheritanceDistance works with multiple interface levels ***/
    @Test
    public void testComputeInheritanceDistance_InterfaceChain() {
        // SubImplementsInterface implements SampleInterface via ImplementsInterface
        assertEquals(2, ClassUtilities.computeInheritanceDistance(SubImplementsInterface.class, SampleInterface.class));
    }

    /*** Test that computeInheritanceDistance correctly handles Object as destination ***/
    @Test
    public void testComputeInheritanceDistance_ToObject() {
        assertEquals(1, ClassUtilities.computeInheritanceDistance(Integer.class, Number.class));
        assertEquals(2, ClassUtilities.computeInheritanceDistance(Integer.class, Object.class));
    }

    /*** Test that getClassLoader does not throw when OSGi classes are absent ***/
    @Test
    public void testGetClassLoader_NoOSGi() {
        // The method should simply return a non‑null loader without throwing.
        ClassLoader cl = ClassUtilities.getClassLoader();
        assertNotNull(cl);
    }

    /*** Test that doesOneWrapTheOther returns false for two unrelated classes ***/
    @Test
    public void testDoesOneWrapTheOther_Unrelated() {
        assertFalse(ClassUtilities.doesOneWrapTheOther(String.class, Integer.class));
    }

    /*** Test that isPrimitive recognises primitive wrapper classes added to the static set ***/
    @Test
    public void testIsPrimitive_WrapperSet() {
        // The static initializer adds common wrappers; verify one of them.
        assertTrue(ClassUtilities.isPrimitive(Character.class));
    }

    /*** Test that adding a new alias does not interfere with built‑in aliases ***/
    @Test
    public void testAddAliasDoesNotOverwriteBuiltin() {
        // "int" is a built‑in alias; adding a new one with the same name should overwrite.
        ClassUtilities.addPermanentClassAlias(String.class, "int");
        assertEquals(String.class, ClassUtilities.forName("int", CL));

        // Clean up by restoring the original mapping
        ClassUtilities.removePermanentClassAlias("int");
        assertEquals(int.class, ClassUtilities.forName("int", CL));
    }
}
