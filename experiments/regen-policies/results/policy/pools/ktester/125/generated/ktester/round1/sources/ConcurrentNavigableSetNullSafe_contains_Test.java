package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableSetNullSafe_contains_Test {

    /**
     * Instance used in most tests – created fresh before each test method.
     */
    private ConcurrentNavigableSetNullSafe<String> set;

    /* ---------------------------------------------------------------------- */
    private static final class ArgumentsHolder {

        final ConcurrentNavigableSetNullSafe<String> set;

        final Object argument;

        // Boolean or Class<? extends Throwable>
        final Object expected;

        final String description;

        ArgumentsHolder(ConcurrentNavigableSetNullSafe<String> set, Object argument, Object expected, String description) {
            this.set = set;
            this.argument = argument;
            this.expected = expected;
            this.description = description;
        }

        // Overload for exception expectation without description
        ArgumentsHolder(ConcurrentNavigableSetNullSafe<String> set, Object argument, Class<? extends Throwable> exceptionClass, String description) {
            this(set, argument, (Object) exceptionClass, description);
        }

        // JUnit 5 method source needs to return a Stream of Arguments
        public static Stream<org.junit.jupiter.params.provider.Arguments> toArgumentsStream(Stream<ArgumentsHolder> source) {
            return source.map(holder -> org.junit.jupiter.params.provider.Arguments.of(holder.set, holder.argument, holder.expected, holder.description));
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use the default (natural‑ordering) constructor for the majority of tests.
        set = new ConcurrentNavigableSetNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the set to help the GC and to guarantee isolation.
        set.clear();
    }

    /**
     * Provides a stream of arguments for the basic {@code contains(Object)} scenarios.
     * Each entry contains: the set to use, the argument to pass, the expected outcome,
     * and a brief description (used only for readability).
     */
    private static Stream<ArgumentsHolder> basicContainsProvider() {
        // 1. Empty set, contains(null) → false
        ConcurrentNavigableSetNullSafe<String> emptySet = new ConcurrentNavigableSetNullSafe<>();
        // 2. Set containing null sentinel, contains(null) → true
        ConcurrentNavigableSetNullSafe<String> nullSet = new ConcurrentNavigableSetNullSafe<>();
        nullSet.add(null);
        // 3. Set with "apple", contains("apple") → true (different String instance)
        ConcurrentNavigableSetNullSafe<String> appleSet = new ConcurrentNavigableSetNullSafe<>();
        appleSet.add("apple");
        // 4. Set without "banana", contains("banana") → false
        ConcurrentNavigableSetNullSafe<String> bananaMissingSet = new ConcurrentNavigableSetNullSafe<>();
        bananaMissingSet.add("apple");
        // 5. Set declared for String, argument is UUID → ClassCastException
        ConcurrentNavigableSetNullSafe<String> stringSet = new ConcurrentNavigableSetNullSafe<>();
        stringSet.add("something");
        return Stream.of(new ArgumentsHolder(emptySet, null, false, "Empty set, null argument should be false"), new ArgumentsHolder(nullSet, null, true, "Set already contains null sentinel"), new ArgumentsHolder(appleSet, new String("apple"), true, "Equal but distinct String instance"), new ArgumentsHolder(bananaMissingSet, "banana", false, "Element not present"), new ArgumentsHolder(stringSet, UUID.randomUUID(), ClassCastException.class, "Wrong type argument should propagate ClassCastException"));
    }

    // Bridge method required because @MethodSource expects a Stream<Arguments>
    private static Stream<org.junit.jupiter.params.provider.Arguments> basicContainsProvider() {
        return ArgumentsHolder.toArgumentsStream(basicContainsProviderRaw());
    }

    // Raw provider returning our custom holder objects.
    private static Stream<ArgumentsHolder> basicContainsProviderRaw() {
        // 1. Empty set, contains(null) → false
        ConcurrentNavigableSetNullSafe<String> emptySet = new ConcurrentNavigableSetNullSafe<>();
        // 2. Set containing null sentinel, contains(null) → true
        ConcurrentNavigableSetNullSafe<String> nullSet = new ConcurrentNavigableSetNullSafe<>();
        nullSet.add(null);
        // 3. Set with "apple", contains(new String("apple")) → true
        ConcurrentNavigableSetNullSafe<String> appleSet = new ConcurrentNavigableSetNullSafe<>();
        appleSet.add("apple");
        // 4. Set without "banana", contains("banana") → false
        ConcurrentNavigableSetNullSafe<String> bananaMissingSet = new ConcurrentNavigableSetNullSafe<>();
        bananaMissingSet.add("apple");
        // 5. Set declared for String, argument is UUID → ClassCastException
        ConcurrentNavigableSetNullSafe<String> stringSet = new ConcurrentNavigableSetNullSafe<>();
        stringSet.add("something");
        return Stream.of(new ArgumentsHolder(emptySet, null, false, "Empty set, null argument should be false"), new ArgumentsHolder(nullSet, null, true, "Set already contains null sentinel"), new ArgumentsHolder(appleSet, new String("apple"), true, "Equal but distinct String instance"), new ArgumentsHolder(bananaMissingSet, "banana", false, "Element not present"), new ArgumentsHolder(stringSet, UUID.randomUUID(), ClassCastException.class, "Wrong type argument should propagate ClassCastException"));
    }

    @Test
    void containsShouldReturnTrueForAddedElementsIncludingNull() {
        // Arrange
        assertTrue(set.add("alpha"));
        assertTrue(set.add("beta"));
        assertTrue(set.add(null));
        // Act & Assert
        assertTrue(set.contains("alpha"));
        assertTrue(set.contains("beta"));
        assertTrue(set.contains(null));
        // Negative case
        assertFalse(set.contains("gamma"));
    }

    @Test
    void containsWorksWithCustomComparator() {
        // Arrange – comparator orders strings by length, treating null as length 0.
        Comparator<String> lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        ConcurrentNavigableSetNullSafe<String> lengthSet = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
        assertTrue(lengthSet.add("a"));
        assertTrue(lengthSet.add("abcd"));
        assertTrue(lengthSet.add(null));
        // Act & Assert
        assertTrue(lengthSet.contains("a"));
        assertTrue(lengthSet.contains("abcd"));
        assertTrue(lengthSet.contains(null));
        assertFalse(lengthSet.contains("ab"));
    }

    @Test
    void containsReflectsElementsAddedViaCollectionConstructor() {
        // Arrange
        Collection<String> source = Arrays.asList("one", "two", null, "three");
        ConcurrentNavigableSetNullSafe<String> collSet = new ConcurrentNavigableSetNullSafe<>(source);
        // Act & Assert
        assertEquals(4, collSet.size());
        assertTrue(collSet.contains("one"));
        assertTrue(collSet.contains("two"));
        assertTrue(collSet.contains("three"));
        assertTrue(collSet.contains(null));
        assertFalse(collSet.contains("four"));
    }

    @Test
    void containsHandlesNullWhenSetIsEmpty() {
        // The set is freshly created and empty.
        assertFalse(set.contains(null));
        assertFalse(set.contains("anything"));
    }

    @Test
    void containsWorksOnSubSetThatIncludesNullBounds() {
        // Arrange – populate the main set.
        set.add("apple");
        set.add("banana");
        set.add("cherry");
        // sentinel for null
        set.add(null);
        // Subset from "banana" (inclusive) to null (inclusive)
        NavigableSet<String> sub = set.subSet("banana", true, null, true);
        // Act & Assert – contains should be true for all elements that belong to the subset.
        assertTrue(sub.contains("banana"));
        assertTrue(sub.contains("cherry"));
        assertTrue(sub.contains(null));
        // Elements outside the subset must not be reported as contained.
        assertFalse(sub.contains("apple"));
    }

    @Test
    void containsReturnsFalseForDifferentTypeObjects() {
        // Arrange
        set.add("string");
        set.add(null);
        // Act & Assert – passing an object of an unrelated type should simply return false,
        // not throw a ClassCastException.
        assertFalse(set.contains(123));
        assertFalse(set.contains(new Object()));
    }

    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("basicContainsProvider")
    void containsBasicScenarios(ConcurrentNavigableSetNullSafe<String> testSet, Object argument, Object expected, String description) {
        if (expected instanceof Class) {
            // Expect an exception
            assertThrows((Class<? extends Throwable>) expected, () -> testSet.contains(argument));
        } else {
            // Normal boolean result
            assertEquals(expected, testSet.contains(argument));
        }
    }

    /**
     * Tests where the underlying {@code internalSet} throws an exception.
     */
    @Test
    void containsPropagatesExceptionFromInternalSet() throws Exception {
        // Create a normal set and then replace its internalSet with a mock that throws.
        ConcurrentNavigableSetNullSafe<String> victim = new ConcurrentNavigableSetNullSafe<>();
        @SuppressWarnings("unchecked")
        NavigableSet<Object> mockInternal = mock(NavigableSet.class);
        when(mockInternal.contains(any())).thenThrow(new IllegalStateException("forced"));
        // Inject the mock via reflection (the field is private final).
        Field f = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        f.setAccessible(true);
        f.set(victim, mockInternal);
        // The exception should propagate unchanged.
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> victim.contains("anything"));
        assertEquals("forced", ex.getMessage());
    }

    /**
     * Comparator that throws {@link IllegalArgumentException} when the element equals "bad".
     */
    @Test
    void containsThrowsIllegalArgumentExceptionFromComparator() {
        Comparator<String> badThrowingComparator = (s1, s2) -> {
            if ("bad".equals(s1) || "bad".equals(s2)) {
                throw new IllegalArgumentException("bad element");
            }
            return s1.compareTo(s2);
        };
        ConcurrentNavigableSetNullSafe<String> setWithBadComparator = new ConcurrentNavigableSetNullSafe<>(badThrowingComparator);
        setWithBadComparator.add("good");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> setWithBadComparator.contains("bad"));
        assertEquals("bad element", ex.getMessage());
    }

    /**
     * Comparator that always throws {@link ClassCastException} – used to verify that
     * such an exception propagates from {@code contains}.
     */
    @Test
    void containsPropagatesClassCastExceptionFromComparator() {
        Comparator<String> alwaysThrowing = (o1, o2) -> {
            throw new ClassCastException("forced");
        };
        ConcurrentNavigableSetNullSafe<String> set = new ConcurrentNavigableSetNullSafe<>(alwaysThrowing);
        // Adding an element works because the comparator is only consulted on ordering;
        // however, the first add will trigger the comparator, so we add after catching.
        assertThrows(ClassCastException.class, () -> set.add("any"));
        // Even if the set were somehow populated, contains would still hit the comparator.
        assertThrows(ClassCastException.class, () -> set.contains("apple"));
    }

    /**
     * Set declared for {@code Number} with a comparator that treats numbers with the same
     * {@code doubleValue()} as equal. Verifies that {@code contains} works across
     * different numeric types.
     */
    @Test
    void containsWorksAcrossNumericTypesUsingCustomComparator() {
        Comparator<Number> numericComparator = (n1, n2) -> Double.compare(n1.doubleValue(), n2.doubleValue());
        ConcurrentNavigableSetNullSafe<Number> numberSet = new ConcurrentNavigableSetNullSafe<>(numericComparator);
        // Integer 5
        assertTrue(numberSet.add(5));
        // Long 5L should be considered equal by the comparator.
        assertTrue(numberSet.contains(5L));
        // A different numeric value must be reported as absent.
        assertFalse(numberSet.contains(6));
    }

    /**
     * Verify that when the set uses natural ordering (no comparator) and the sentinel
     * object is *not* present, {@code contains(null)} correctly returns {@code false}
     * rather than throwing a {@link ClassCastException}.
     */
    @Test
    void containsNullWhenSentinelNotPresentReturnsFalse() {
        // Natural ordering set without adding null.
        ConcurrentNavigableSetNullSafe<String> naturalSet = new ConcurrentNavigableSetNullSafe<>();
        naturalSet.add("a");
        naturalSet.add("b");
        // The sentinel is absent, so contains(null) should be false.
        assertFalse(naturalSet.contains(null));
    }
}
