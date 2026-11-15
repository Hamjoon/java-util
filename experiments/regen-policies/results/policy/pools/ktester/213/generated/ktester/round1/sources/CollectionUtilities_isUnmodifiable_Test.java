package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.*;
import java.util.stream.Stream;

class CollectionUtilities_isUnmodifiable_Test {

    /**
     * Provides test data for cases that are expected to return {@code true}.
     *
     * <p>Each {@code Arguments} consists of:
     * <ul>
     *   <li>the {@code Class} to test</li>
     *   <li>a short description (used only for readability in the test output)</li>
     * </ul>
     */
    private static Stream<Arguments> trueCases() {
        return Stream.of(// Exact unmodifiable collection class
        Arguments.of(loadClass("java.util.Collections$UnmodifiableCollection"), "Exact unmodifiable collection class"), // Subclass – UnmodifiableRandomAccessList (the JDK list implementation)
        Arguments.of(loadClass("java.util.Collections$UnmodifiableRandomAccessList"), "Subclass of the unmodifiable collection class – list variant"), // Subclass – UnmodifiableSet
        Arguments.of(loadClass("java.util.Collections$UnmodifiableSet"), "Subclass of the unmodifiable collection class – set variant"));
    }

    /**
     * Provides test data for cases that are expected to return {@code false}.
     *
     * <p>Each {@code Arguments} consists of the {@code Class} to test and a
     * description string.
     */
    private static Stream<Arguments> falseCases() {
        return Stream.of(// Regular mutable collections
        Arguments.of(ArrayList.class, "Mutable List implementation"), Arguments.of(HashSet.class, "Mutable Set implementation"), // Super‑type of the unmodifiable wrapper
        Arguments.of(Collection.class, "Collection interface (super‑type)"), // Unrelated class
        Arguments.of(String.class, "Completely unrelated class"), // Superclass of the unmodifiable wrapper
        Arguments.of(AbstractCollection.class, "Superclass of the unmodifiable wrapper"), // Primitive type
        Arguments.of(int.class, "Primitive int type"), // Array type
        Arguments.of(String[].class, "Array type (String[])"), // Interface extending Collection
        Arguments.of(List.class, "Interface extending Collection"), // Unmodifiable map – not a Collection
        Arguments.of(loadClass("java.util.Collections$UnmodifiableMap"), "Unmodifiable Map implementation (not a Collection)"));
    }

    /**
     * Utility method that loads a class by its binary name.
     *
     * <p>The method wraps {@code Class.forName} and re‑throws any
     * {@code ClassNotFoundException} as an unchecked {@code IllegalArgumentException},
     * making the test data definition concise.</p>
     *
     * @param className binary name of the class (e.g. {@code "java.util.Collections$UnmodifiableSet"})
     * @return the {@code Class} object
     */
    private static Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            // If the JDK implementation changes (e.g., class name differs), fail fast.
            throw new IllegalArgumentException("Unable to load class: " + className, e);
        }
    }

    @Test
    void isUnmodifiable_WithUnmodifiableCollectionClass_ReturnsTrue() {
        // Given: an unmodifiable collection created via java.util.Collections
        List<Integer> modifiable = new ArrayList<>();
        List<Integer> unmodifiable = Collections.unmodifiableList(modifiable);
        Class<?> unmodifiableClass = unmodifiable.getClass();
        // When
        boolean result = CollectionUtilities.isUnmodifiable(unmodifiableClass);
        // Then
        assertTrue(result, "The class of an unmodifiable collection should be reported as unmodifiable");
    }

    @Test
    void isUnmodifiable_WithModifiableCollectionClass_ReturnsFalse() {
        // Given: a regular, modifiable collection class
        Class<?> modifiableClass = ArrayList.class;
        // When
        boolean result = CollectionUtilities.isUnmodifiable(modifiableClass);
        // Then
        assertFalse(result, "A normal collection class should not be reported as unmodifiable");
    }

    @Test
    void isUnmodifiable_NullArgument_ThrowsNullPointerException() {
        // Given: a null argument
        // When / Then
        NullPointerException ex = assertThrows(NullPointerException.class, () -> CollectionUtilities.isUnmodifiable(null));
        assertEquals("targetType (Class) cannot be null", ex.getMessage(), "The NPE message should match the one defined in the method");
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("trueCases")
    void isUnmodifiable_ShouldReturnTrue(Class<?> targetType, String description) {
        // When
        boolean result = CollectionUtilities.isUnmodifiable(targetType);
        // Then
        assertTrue(result, () -> "Expected true for: " + description + " (class: " + targetType + ")");
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("falseCases")
    void isUnmodifiable_ShouldReturnFalse(Class<?> targetType, String description) {
        // When
        boolean result = CollectionUtilities.isUnmodifiable(targetType);
        // Then
        assertFalse(result, () -> "Expected false for: " + description + " (class: " + targetType + ")");
    }
}
