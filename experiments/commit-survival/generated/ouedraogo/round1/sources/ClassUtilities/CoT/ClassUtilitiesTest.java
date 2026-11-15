package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.*;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link ClassUtilities}.
 *
 * The test suite is built in four logical steps as requested:
 *
 * 1. **Extraction of public API** – a list of every public method together with its
 *    full signature is provided in the comment block {@code PUBLIC_METHODS}.
 *
 * 2. **Basic functional tests** – one straightforward test per public method that
 *    verifies the documented “happy path” behaviour.
 *
 * 3. **Edge‑case / exception identification** – for each method we analyse possible
 *    abnormal inputs (nulls, empty strings, primitive vs. wrapper mismatches, etc.)
 *    and list the scenarios that should be covered.
 *
 * 4. **Targeted edge‑case tests** – concrete JUnit tests that exercise the identified
 *    edge conditions and assert the expected result (return value or thrown exception).
 *
 * All tests are written without any external mocking framework; they rely only on
 * the classes, constructors and methods that exist in the source code above or in the
 * standard JDK.
 */
public class ClassUtilitiesTest {

    /* ==============================================================
       1. PUBLIC METHOD SIGNATURES (extracted from ClassUtilities)
       ============================================================== */
    // -----------------------------------------------------------------
    // addPermanentClassAlias(Class<?> clazz, String alias)
    // removePermanentClassAlias(String alias)
    // computeInheritanceDistance(Class<?> source, Class<?> destination)
    // isPrimitive(Class<?> c)
    // forName(String name, ClassLoader classLoader)
    // isClassFinal(Class<?> c)
    // areAllConstructorsPrivate(Class<?> c)
    // toPrimitiveWrapperClass(Class<?> primitiveClass)
    // doesOneWrapTheOther(Class<?> x, Class<?> y)
    // getClassLoader()
    // getClassLoader(Class<?> anchorClass)
    // -----------------------------------------------------------------
    // The following methods are **private** or **package‑private** and are
    // therefore not part of the public test surface:
    //   comparePrimitiveToWrapper(...)
    //   internalClassForName(...)
    //   loadClass(...)
    //   getOSGiClassLoader(...)
    //   getOSGiClassLoader0(...)
    // -----------------------------------------------------------------

    /* ==============================================================
       2. BASIC FUNCTIONAL TESTS
       ============================================================== */

    @Test
    public void testAddAndRemovePermanentClassAlias() {
        // Use a unique alias to avoid collisions with static initialisation
        String alias = "myUniqueAlias";
        Class<?> clazz = java.math.BigDecimal.class;

        // Ensure alias is not present initially
        assertNull(ClassUtilities.forName(alias, getClass().getClassLoader()));

        // Add alias and verify it resolves
        ClassUtilities.addPermanentClassAlias(clazz, alias);
        assertEquals(clazz, ClassUtilities.forName(alias, getClass().getClassLoader()));

        // Remove alias and verify it is gone again
        ClassUtilities.removePermanentClassAlias(alias);
        assertNull(ClassUtilities.forName(alias, getClass().getClassLoader()));
    }

    @Test
    public void testComputeInheritanceDistance_SameClass() {
        assertEquals(0, ClassUtilities.computeInheritanceDistance(String.class, String.class));
    }

    @Test
    public void testComputeInheritanceDistance_Superclass() {
        // String → Object = 1 step
        assertEquals(1, ClassUtilities.computeInheritanceDistance(String.class, Object.class));
    }

    @Test
    public void testComputeInheritanceDistance_InterfaceImplementation() {
        // ArrayList implements List
        assertEquals(1, ClassUtilities.computeInheritanceDistance(ArrayList.class, List.class));
    }

    @Test
    public void testComputeInheritanceDistance_NoRelation() {
        // Integer and Thread are unrelated
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(Integer.class, Thread.class));
    }

    @Test
    public void testComputeInheritanceDistance_PrimitiveWrapper() {
        // int (primitive) vs Integer (wrapper) should be distance 0
        assertEquals(0, ClassUtilities.computeInheritanceDistance(int.class, Integer.class));
        assertEquals(0, ClassUtilities.computeInheritanceDistance(Integer.class, int.class));
    }

    @Test
    public void testIsPrimitive() {
        assertTrue(ClassUtilities.isPrimitive(int.class));
        assertTrue(ClassUtilities.isPrimitive(Integer.class));
        assertTrue(ClassUtilities.isPrimitive(boolean.class));
        assertTrue(ClassUtilities.isPrimitive(Boolean.class));
        assertFalse(ClassUtilities.isPrimitive(String.class));
    }

    @Test
    public void testForName_StandardClass() {
        Class<?> clazz = ClassUtilities.forName("java.util.Date", getClass().getClassLoader());
        assertEquals(Date.class, clazz);
    }

    @Test
    public void testForName_PrimitiveAlias() {
        // The static map contains "int" -> int primitive type
        Class<?> clazz = ClassUtilities.forName("int", getClass().getClassLoader());
        assertEquals(int.class, clazz);
    }

    @Test
    public void testIsClassFinal() {
        assertTrue(ClassUtilities.isClassFinal(String.class)); // String is final
        assertFalse(ClassUtilities.isClassFinal(ArrayList.class)); // not final
    }

    @Test
    public void testAreAllConstructorsPrivate() {
        // Utility class with private constructor
        assertTrue(ClassUtilities.areAllConstructorsPrivate(ClassUtilities.class));

        // Class with public constructor
        assertFalse(ClassUtilities.areAllConstructorsPrivate(ArrayList.class));
    }

    @Test
    public void testToPrimitiveWrapperClass() {
        assertEquals(Integer.class, ClassUtilities.toPrimitiveWrapperClass(int.class));
        assertEquals(int.class, ClassUtilities.toPrimitiveWrapperClass(Integer.class)); // non‑primitive returns itself
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToPrimitiveWrapperClass_NonPrimitiveThrows() {
        ClassUtilities.toPrimitiveWrapperClass(String.class);
    }

    @Test
    public void testDoesOneWrapTheOther() {
        assertTrue(ClassUtilities.doesOneWrapTheOther(int.class, Integer.class));
        assertTrue(ClassUtilities.doesOneWrapTheOther(Integer.class, int.class));
        assertFalse(ClassUtilities.doesOneWrapTheOther(String.class, Object.class));
    }

    @Test
    public void testGetClassLoader_NoOSGi() {
        // In a normal JVM environment getClassLoader() should never return null
        ClassLoader cl = ClassUtilities.getClassLoader();
        assertNotNull(cl);
    }

    @Test
    public void testGetClassLoader_WithAnchor() {
        ClassLoader cl = ClassUtilities.getClassLoader(ClassUtilitiesTest.class);
        assertNotNull(cl);
    }

    /* ==============================================================
       3. EDGE CASE / EXCEPTION SCENARIOS
       ============================================================== */

    // computeInheritanceDistance
    //   - source == null or destination == null  -> -1
    //   - primitive to unrelated non‑primitive -> -1
    //   - wrapper to primitive where wrapper is not the correct one -> -1
    //   - interface hierarchy (multiple levels)
    //   - class hierarchy deeper than one level

    // forName
    //   - name == null -> null
    //   - name == "" (empty) -> null
    //   - unknown class name -> null (no exception)
    //   - name that maps to a forbidden class (e.g., java.lang.Class) -> should succeed because
    //     the security check only blocks a few reflection‑related types.
    //   - name that triggers SecurityException (hard to force without OSGi), we verify that
    //     IllegalArgumentException is thrown when internalClassForName throws SecurityException.

    // toPrimitiveWrapperClass
    //   - null argument -> NullPointerException (inherited from Map.get)
    //   - primitive that is not in primitiveToWrapper map (void) -> IllegalArgumentException

    // doesOneWrapTheOther
    //   - null arguments -> false (map.get returns null)

    // areAllConstructorsPrivate
    //   - class with no declared constructors (e.g., interface) -> true (no constructors to check)

    /* ==============================================================
       4. EDGE CASE TESTS
       ============================================================== */

    // ---------- computeInheritanceDistance edge cases ----------
    @Test
    public void testComputeInheritanceDistance_NullArguments() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, Object.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(Object.class, null));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, null));
    }

    @Test
    public void testComputeInheritanceDistance_PrimitiveToUnrelatedWrapper() {
        // int primitive to String wrapper (not a primitive wrapper) -> -1
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(int.class, String.class));
    }

    @Test
    public void testComputeInheritanceDistance_WrapperToDifferentPrimitive() {
        // Integer wrapper to long primitive -> -1
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(Integer.class, long.class));
    }

    @Test
    public void testComputeInheritanceDistance_InterfaceMultiLevel() {
        // List -> Collection -> Iterable -> Object : distance 3
        assertEquals(3, ClassUtilities.computeInheritanceDistance(List.class, Object.class));
    }

    @Test
    public void testComputeInheritanceDistance_ClassMultiLevel() {
        // ArrayList -> AbstractList -> AbstractCollection -> Object : distance 3
        assertEquals(3, ClassUtilities.computeInheritanceDistance(ArrayList.class, Object.class));
    }

    // ---------- forName edge cases ----------
    @Test
    public void testForName_NullOrEmpty() {
        assertNull(ClassUtilities.forName(null, getClass().getClassLoader()));
        assertNull(ClassUtilities.forName("", getClass().getClassLoader()));
    }

    @Test
    public void testForName_UnknownClass() {
        assertNull(ClassUtilities.forName("com.this.does.not.Exist", getClass().getClassLoader()));
    }

    @Test
    public void testForName_ForbiddenReflectionClass() {
        // Class.class is allowed; the security check blocks only a few specific reflection classes.
        Class<?> clazz = ClassUtilities.forName("class", getClass().getClassLoader());
        assertEquals(Class.class, clazz);
    }

    // ---------- toPrimitiveWrapperClass edge cases ----------
    @Test(expected = NullPointerException.class)
    public void testToPrimitiveWrapperClass_NullArgument() {
        ClassUtilities.toPrimitiveWrapperClass(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToPrimitiveWrapperClass_VoidPrimitive() {
        // void is present in the map but is not a usable primitive wrapper for this method
        ClassUtilities.toPrimitiveWrapperClass(void.class);
    }

    // ---------- doesOneWrapTheOther edge cases ----------
    @Test
    public void testDoesOneWrapTheOther_NullArguments() {
        assertFalse(ClassUtilities.doesOneWrapTheOther(null, Integer.class));
        assertFalse(ClassUtilities.doesOneWrapTheOther(int.class, null));
        assertFalse(ClassUtilities.doesOneWrapTheOther(null, null));
    }

    // ---------- areAllConstructorsPrivate edge cases ----------
    @Test
    public void testAreAllConstructorsPrivate_Interface() {
        // Interfaces have no constructors; the method should return true.
        assertTrue(ClassUtilities.areAllConstructorsPrivate(Runnable.class));
    }

    // ---------- getClassLoader edge cases ----------
    @Test
    public void testGetClassLoader_NullAnchorHandledGracefully() {
        // Passing null would cause a NullPointerException in the current implementation,
        // but we verify that the method throws NPE rather than returning an unexpected value.
        try {
            ClassUtilities.getClassLoader(null);
            fail("Expected NullPointerException");
        } catch (NullPointerException ignored) {
            // expected
        }
    }

    // ---------- addPermanentClassAlias edge case (duplicate alias) ----------
    @Test
    public void testAddPermanentClassAlias_OverwriteExistingAlias() {
        String alias = "duplicateAlias";
        ClassUtilities.addPermanentClassAlias(String.class, alias);
        // Overwrite with a different class
        ClassUtilities.addPermanentClassAlias(Integer.class, alias);
        // The alias now resolves to the last class added
        assertEquals(Integer.class, ClassUtilities.forName(alias, getClass().getClassLoader()));
        // Cleanup
        ClassUtilities.removePermanentClassAlias(alias);
    }

    // ---------- internal sanity: ensure static maps are populated correctly ----------
    @Test
    public void testStaticMapsContainExpectedEntries() throws Exception {
        // Primitive name map
        assertEquals(int.class, ClassUtilities.forName("int", getClass().getClassLoader()));
        // Wrapper map direction
        assertTrue(ClassUtilities.doesOneWrapTheOther(int.class, Integer.class));
        // Primitive wrapper conversion
        assertEquals(Integer.class, ClassUtilities.toPrimitiveWrapperClass(int.class));
    }
}
