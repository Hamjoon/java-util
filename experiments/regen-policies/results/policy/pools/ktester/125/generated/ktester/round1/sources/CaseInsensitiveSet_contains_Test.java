package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveSet_contains_Test {

    private CaseInsensitiveSet<Object> set;

    @BeforeEach
    void setupBeforeEach() {
        // Fresh set for each test to guarantee isolation.
        set = new CaseInsensitiveSet<>();
        set.add("One");
        set.add("Two");
        set.add("Three");
        // non‑String element
        set.add(42);
        // verify null handling (should be stored as a key)
        set.add(null);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the set to avoid any side effects (not strictly required because a new set is created each test).
        set.clear();
    }

    /**
     * Provides a stream of arguments for basic {@code contains} checks.
     * Each element is an array: {input object, expected result, description}.
     */
    private static Stream<Arguments> basicContainsProvider() {
        return Stream.of(Arguments.of("Apple", true, "The set contains the element \"Apple\" added with the same case."), Arguments.of("apple", true, "Case‑insensitive lookup for \"apple\" after adding \"Apple\"."), Arguments.of("Banana", false, "Element \"Banana\" was never added."), Arguments.of("", true, "Empty string was added and should be found."), Arguments.of(" ", true, "Whitespace string should be found."));
    }

    @Test
    void containsStringDifferentCase() {
        assertTrue(set.contains("one"));
        assertTrue(set.contains("ONE"));
        assertTrue(set.contains("OnE"));
        assertTrue(set.contains("two"));
        assertTrue(set.contains("TWO"));
        assertTrue(set.contains("tWo"));
        assertTrue(set.contains("three"));
        assertTrue(set.contains("THREE"));
        assertTrue(set.contains("ThReE"));
    }

    @Test
    void containsNonStringElements() {
        assertTrue(set.contains(42));
        assertFalse(set.contains(24));
    }

    @Test
    void containsNullElement() {
        // The set was initialised with a null element.
        assertTrue(set.contains(null));
        // Removing the null element should make contains(null) return false.
        set.remove(null);
        assertFalse(set.contains(null));
    }

    @Test
    void containsAfterRemoval() {
        // Remove a string using a different case and verify it is no longer present.
        assertTrue(set.contains("Two"));
        set.remove("tWo");
        assertFalse(set.contains("Two"));
        assertFalse(set.contains("tWo"));
    }

    @Test
    void containsInEmptySet() {
        CaseInsensitiveSet<Object> empty = new CaseInsensitiveSet<>();
        assertFalse(empty.contains("anything"));
        assertFalse(empty.contains(null));
    }

    @Test
    void containsAfterAddingNewElements() {
        set.add("FoO");
        assertTrue(set.contains("foo"));
        assertTrue(set.contains("FOO"));
        assertTrue(set.contains("FoO"));
    }

    @Test
    void containsMixedTypesDoNotInterfere() {
        // Adding a String that looks like a number should not affect numeric lookup.
        set.add("42");
        // string lookup, case‑insensitive (irrelevant for numbers)
        assertTrue(set.contains("42"));
        // numeric element is distinct from the string "42"
        assertFalse(set.contains(42));
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("basicContainsProvider")
    void containsBasicScenarios(Object input, boolean expected, String description) {
        // Arrange: a fresh set containing the elements required for the test.
        CaseInsensitiveSet<Object> localSet = new CaseInsensitiveSet<>();
        localSet.add("Apple");
        localSet.add("");
        localSet.add(" ");
        // Act & Assert
        assertEquals(expected, localSet.contains(input));
    }

    @Test
    void containsNullShouldThrowNPE() {
        // According to the specification, a null key is not permitted by the
        // underlying case‑insensitive map. Use a set that does NOT contain a null.
        CaseInsensitiveSet<Object> noNullSet = new CaseInsensitiveSet<>();
        noNullSet.add("Apple");
        // The call must throw NullPointerException.
        assertThrows(NullPointerException.class, () -> noNullSet.contains(null));
    }

    @Test
    void containsAfterClearReturnsFalse() {
        CaseInsensitiveSet<Object> localSet = new CaseInsensitiveSet<>();
        localSet.add("Apple");
        // make the set empty
        localSet.clear();
        assertFalse(localSet.contains("Cherry"));
    }

    @Test
    void containsStringWhenOnlyIntegerPresentThrowsClassCastException() {
        CaseInsensitiveSet<Object> intOnlySet = new CaseInsensitiveSet<>();
        // non‑String element
        intOnlySet.add(10);
        // The case‑insensitive comparator expects Strings; passing a String should fail.
        assertThrows(ClassCastException.class, () -> intOnlySet.contains("10"));
    }

    @Test
    void containsStringBuilderThrowsClassCastException() {
        CaseInsensitiveSet<Object> mixedSet = new CaseInsensitiveSet<>();
        mixedSet.add("Apple");
        // StringBuilder is not comparable by the case‑insensitive comparator.
        assertThrows(ClassCastException.class, () -> mixedSet.contains(new StringBuilder("Apple")));
    }

    @Test
    void containsCustomObjectThrowsClassCastException() {
        // Populate the set with a generic Object instance.
        Object customObj = new Object();
        CaseInsensitiveSet<Object> objSet = new CaseInsensitiveSet<>();
        objSet.add(customObj);
        // Querying with a different Object should trigger ClassCastException.
        assertThrows(ClassCastException.class, () -> objSet.contains(new Object()));
    }

    @Test
    void containsIntegerWhenOnlyStringPresentReturnsFalse() {
        // The set contains a String; querying with an Integer should simply return false
        // (no exception because the map can store heterogeneous keys).
        CaseInsensitiveSet<Object> stringSet = new CaseInsensitiveSet<>();
        stringSet.add("Apple");
        assertFalse(stringSet.contains(42));
    }

    @Test
    void containsAfterAddingAndRemovingElement() {
        CaseInsensitiveSet<Object> localSet = new CaseInsensitiveSet<>();
        localSet.add("Pear");
        // case‑insensitive check
        assertTrue(localSet.contains("pear"));
        localSet.remove("PEAR");
        assertFalse(localSet.contains("pear"));
    }

    @Test
    void containsIntegerPresentReturnsTrue() {
        CaseInsensitiveSet<Object> localSet = new CaseInsensitiveSet<>();
        localSet.add(42);
        assertTrue(localSet.contains(42));
    }

    @Test
    void containsMissingIntegerReturnsFalse() {
        CaseInsensitiveSet<Object> localSet = new CaseInsensitiveSet<>();
        localSet.add(42);
        assertFalse(localSet.contains(43));
    }

    @Test
    void containsStringNumberDoesNotAffectIntegerLookup() {
        CaseInsensitiveSet<Object> localSet = new CaseInsensitiveSet<>();
        // string representation
        localSet.add("42");
        // integer representation
        localSet.add(42);
        assertTrue(localSet.contains("42"));
        assertTrue(localSet.contains(42));
        // Ensure they are treated as distinct keys.
        localSet.remove("42");
        assertTrue(localSet.contains(42));
        assertFalse(localSet.contains("42"));
    }
}
