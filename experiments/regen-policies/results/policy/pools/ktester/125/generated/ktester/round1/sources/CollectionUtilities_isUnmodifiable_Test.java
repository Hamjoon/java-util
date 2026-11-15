package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CollectionUtilities_isUnmodifiable_Test {

    /**
     * Simple holder for a test case extracted from the JSON description.
     */
    private static final class TestCase {

        final String className;

        final boolean expected;

        final String description;

        TestCase(String className, boolean expected, String description) {
            this.className = className;
            this.expected = expected;
            this.description = description;
        }
    }

    /**
     * Parameter source that yields a stream of {@link TestCase} objects.
     * Each case contains the target {@link Class}, the expected boolean result,
     * and a short description (used only for readability in the test name).
     */
    private static Stream<TestCase> isUnmodifiableCases() {
        return Stream.of(// unrelated class
        new TestCase("java.lang.String", false, "Non-collection class; should return false."), // null target – handled separately (see dedicated test below)
        // unmodifiable List (RandomAccess)
        new TestCase("java.util.Collections$UnmodifiableRandomAccessList", true, "Class returned by Collections.unmodifiableList(new ArrayList<>()) implements the internal unmodifiable collection base class."), // unmodifiable Set
        new TestCase("java.util.Collections$UnmodifiableSet", true, "Class returned by Collections.unmodifiableSet(new HashSet<>()) is assignable from the internal unmodifiable collection class."), // unmodifiable SortedSet
        new TestCase("java.util.Collections$UnmodifiableSortedSet", true, "Class returned by Collections.unmodifiableSortedSet(new TreeSet<>()) should be recognised as unmodifiable."), // unmodifiable NavigableSet
        new TestCase("java.util.Collections$UnmodifiableNavigableSet", true, "Class returned by Collections.unmodifiableNavigableSet(new TreeSet<>()) should be recognised as unmodifiable."), // regular mutable List
        new TestCase("java.util.ArrayList", false, "A normal mutable List class is not assignable from the unmodifiable wrapper class."), // regular mutable Set
        new TestCase("java.util.HashSet", false, "A normal mutable Set class should return false."), // interface Collection
        new TestCase("java.util.Collection", false, "The Collection interface itself is not a concrete unmodifiable collection implementation."), // primitive array class
        new TestCase("[I", false, "A primitive array class (int[]) is unrelated to collection wrappers and should return false."), // exact internal unmodifiable base class
        new TestCase("java.util.Collections$UnmodifiableCollection", true, "Directly passing the internal unmodifiable collection base class must be recognised as unmodifiable."));
    }

    /**
     * Resolves a class name used in the JSON matrix to a {@link Class} object.
     * Handles normal fully‑qualified names as well as primitive array descriptors
     * such as {@code "[I"} (int[]), {@code "[[Ljava.lang.String;"} (String[][]), etc.
     *
     * @param name the class name from the JSON matrix
     * @return the corresponding {@link Class}
     * @throws ClassNotFoundException if the class cannot be loaded
     */
    private static Class<?> resolveClass(String name) throws ClassNotFoundException {
        // Primitive array descriptors are already in the format accepted by Class.forName
        if (name.startsWith("[") || name.equals("void")) {
            return Class.forName(name);
        }
        // Normal class name
        return Class.forName(name);
    }

    @Test
    void isUnmodifiable_returnsFalseForMutableCollectionClass() {
        // GIVEN a mutable collection class
        Class<?> mutableClass = ArrayList.class;
        // WHEN
        boolean result = CollectionUtilities.isUnmodifiable(mutableClass);
        // THEN
        assertFalse(result, "ArrayList.class should not be considered unmodifiable");
    }

    @Test
    void isUnmodifiable_returnsTrueForUnmodifiableCollectionClass() {
        // GIVEN an unmodifiable collection instance created via Collections.unmodifiableList(...)
        List<String> mutable = new ArrayList<>();
        mutable.add("a");
        List<String> unmodifiable = Collections.unmodifiableList(mutable);
        Class<?> unmodifiableClass = unmodifiable.getClass();
        // WHEN
        boolean result = CollectionUtilities.isUnmodifiable(unmodifiableClass);
        // THEN
        assertTrue(result, "The class of Collections.unmodifiableList(...) should be recognised as unmodifiable");
    }

    @Test
    void isUnmodifiable_throwsExceptionWhenNullArgument() {
        // GIVEN a null argument
        Class<?> target = null;
        // WHEN / THEN
        NullPointerException npe = assertThrows(NullPointerException.class, () -> CollectionUtilities.isUnmodifiable(target));
        assertEquals("targetType (Class) cannot be null", npe.getMessage(), "Exception message should match the one defined in the method");
    }

    /**
     * Parameterised test that verifies the boolean outcome for each supplied class.
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("isUnmodifiableCases")
    void isUnmodifiable_behaviourMatchesExpectation(TestCase tc) throws ClassNotFoundException {
        // GIVEN a Class instance derived from the string name in the test case
        Class<?> targetClass = resolveClass(tc.className);
        // WHEN
        boolean actual = CollectionUtilities.isUnmodifiable(targetClass);
        // THEN
        assertEquals(tc.expected, actual, () -> "Unexpected result for class '" + tc.className + "': " + tc.description);
    }

    /**
     * Dedicated test for the null argument case because it expects an exception,
     * which cannot be expressed with the boolean‑returning parameterised test.
     */
    @Test
    void isUnmodifiable_nullArgument_throwsNPE() {
        NullPointerException npe = assertThrows(NullPointerException.class, () -> CollectionUtilities.isUnmodifiable(null));
        assertEquals("targetType (Class) cannot be null", npe.getMessage(), "Exception message should match the one defined in the method");
    }
}
