package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveSet_isEmpty_Test {

    /**
     * Instance used for the majority of the tests – a fresh set for each test method.
     */
    private CaseInsensitiveSet<String> defaultSet;

    /**
     * Instance used to verify that non‑String elements do not affect the case‑insensitive logic.
     */
    private CaseInsensitiveSet<Object> mixedSet;

    @BeforeEach
    void setupBeforeEach() {
        // Create a new, empty default set before each test.
        defaultSet = new CaseInsensitiveSet<>();
        // Create a mixed‑type set (String + non‑String) to ensure isEmpty works uniformly.
        mixedSet = new CaseInsensitiveSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references – not strictly required but keeps the test class tidy.
        defaultSet = null;
        mixedSet = null;
    }

    /**
     * Supplies a stream of {@link Runnable} actions that manipulate {@code defaultSet}
     * and the expected result of {@code isEmpty()} after the actions.
     */
    private static Stream<Arguments> isEmptyScenarios() {
        return Stream.of(// scenario: new set -> true
        Arguments.of((Runnable) () -> {
        }, true, "Newly created set should be empty"), // scenario: add one element -> false
        Arguments.of((Runnable) () -> defaultSet.add("item"), false, "After adding one element, set is not empty"), // scenario: add then remove same element (different case) -> true
        Arguments.of((Runnable) () -> {
            defaultSet.add("Test");
            defaultSet.remove("TEST");
        }, true, "Add then remove same element (case‑insensitive) results in empty set"), // scenario: add null then remove null -> true
        Arguments.of((Runnable) () -> {
            defaultSet.add(null);
            defaultSet.remove(null);
        }, true, "Add and then remove null element restores emptiness"));
    }

    @Test
    void isEmptyOnNewSet_returnsTrue() {
        assertTrue(defaultSet.isEmpty(), "A newly created set should be empty");
    }

    @Test
    void isEmptyAfterAddingElement_returnsFalse() {
        defaultSet.add("Hello");
        assertFalse(defaultSet.isEmpty(), "Set should not be empty after adding an element");
    }

    @Test
    void isEmptyAfterRemovingElement_returnsTrue() {
        defaultSet.add("World");
        // case‑insensitive removal
        defaultSet.remove("WORLD");
        assertTrue(defaultSet.isEmpty(), "Set should be empty after removing the only element");
    }

    @Test
    void isEmptyWithMixedTypes_behavesConsistently() {
        // Add a non‑String element.
        mixedSet.add(42);
        assertFalse(mixedSet.isEmpty(), "Set with a non‑String element should not be empty");
        // Remove the non‑String element.
        mixedSet.remove(42);
        assertTrue(mixedSet.isEmpty(), "Set should be empty after removing the non‑String element");
    }

    @Test
    void isEmptyWhenBackedByCustomMap_behavesCorrectly() {
        // Construct a set with an explicit backing map (LinkedHashMap) to verify that the
        // constructor does not interfere with the isEmpty logic.
        Set<String> setWithMap = new CaseInsensitiveSet<>(Collections.emptySet(), new CaseInsensitiveMap<>(Collections.emptyMap(), new LinkedHashMap<>()));
        assertTrue(setWithMap.isEmpty(), "Set created with a custom backing map should start empty");
        setWithMap.add("Alpha");
        assertFalse(setWithMap.isEmpty(), "Set should not be empty after adding an element");
        setWithMap.remove("ALPHA");
        assertTrue(setWithMap.isEmpty(), "Set should be empty after removing the element (case‑insensitive)");
    }

    @Test
    void isEmptyWhenConstructedWithInitialCapacity_behavesCorrectly() {
        // Verify that the constructor taking an initial capacity does not affect emptiness.
        CaseInsensitiveSet<String> capacitySet = new CaseInsensitiveSet<>(10);
        assertTrue(capacitySet.isEmpty(), "Set with initial capacity should start empty");
        capacitySet.add("Beta");
        assertFalse(capacitySet.isEmpty(), "Set should not be empty after adding an element");
        capacitySet.remove("BETA");
        assertTrue(capacitySet.isEmpty(), "Set should be empty after removing the element (case‑insensitive)");
    }

    @Test
    void isEmptyAfterClear_returnsTrue() {
        // Populate the set, then clear it.
        defaultSet.add("one");
        defaultSet.add("two");
        defaultSet.clear();
        assertTrue(defaultSet.isEmpty(), "Calling clear() on a non‑empty set should make it empty");
    }

    @Test
    void isEmptyAfterAddingMultipleElements_returnsFalse() {
        // Add several distinct strings (case‑insensitively unique).
        defaultSet.add("Apple");
        defaultSet.add("Banana");
        defaultSet.add("Cherry");
        assertFalse(defaultSet.isEmpty(), "Set with multiple elements must report not empty");
    }

    @Test
    void isEmptyAfterRemoveAll_returnsTrue() {
        // Fill the set, then remove everything via removeAll(Collection).
        defaultSet.addAll(Arrays.asList("x", "y", "z"));
        // case‑insensitive match
        Collection<String> toRemove = Arrays.asList("X", "Y", "Z");
        defaultSet.removeAll(toRemove);
        assertTrue(defaultSet.isEmpty(), "removeAll should empty the set, making isEmpty() true");
    }

    @Test
    void isEmptyAfterAddingNullElement_returnsFalse() {
        // The implementation permits null as a key; after adding null the set is not empty.
        defaultSet.add(null);
        assertFalse(defaultSet.isEmpty(), "Adding a null element should make the set non‑empty");
    }

    @Test
    void isEmptyAfterAddingAndRemovingNullElement_returnsTrue() {
        defaultSet.add(null);
        defaultSet.remove(null);
        assertTrue(defaultSet.isEmpty(), "Removing the previously added null element should restore emptiness");
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("isEmptyScenarios")
    void parameterizedIsEmptyTests(Runnable actions, boolean expected, String description) {
        // The lambda manipulates the instance created in the test method.
        actions.run();
        assertEquals(expected, defaultSet.isEmpty(), description);
    }
}
