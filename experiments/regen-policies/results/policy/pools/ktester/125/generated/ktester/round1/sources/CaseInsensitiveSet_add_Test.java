package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveSet_add_Test {

    private CaseInsensitiveSet<Object> set;

    /**
     * Simple class that implements {@link Comparable} but is not mutually comparable
     * with {@link String}. Adding it after a {@link String} is present should cause
     * {@link ClassCastException} inside {@link java.util.concurrent.ConcurrentSkipListMap}.
     */
    static class Foo implements Comparable<Foo> {

        private final int id;

        Foo(int id) {
            this.id = id;
        }

        @Override
        public int compareTo(Foo o) {
            return Integer.compare(this.id, o.id);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh instance before each test to guarantee isolation.
        set = new CaseInsensitiveSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help the garbage collector and ensure no leftover state.
        set = null;
    }

    @Test
    void addNewElement_returnsTrueAndIncrementsSize() {
        assertTrue(set.isEmpty(), "Set should start empty");
        boolean added = set.add("Hello");
        assertTrue(added, "Adding a brand‑new element must return true");
        assertEquals(1, set.size(), "Size must be 1 after successful addition");
        // Adding a different element should also succeed.
        added = set.add(42);
        assertTrue(added, "Adding a different non‑String element must return true");
        assertEquals(2, set.size(), "Size must be 2 after second successful addition");
    }

    @Test
    void addDuplicateStringDifferentCase_returnsFalseAndSizeUnchanged() {
        set.add("World");
        int sizeAfterFirstAdd = set.size();
        // Duplicate with different case – should be considered the same element.
        boolean added = set.add("wOrLd");
        assertFalse(added, "Adding a duplicate string with different case must return false");
        assertEquals(sizeAfterFirstAdd, set.size(), "Size must remain unchanged when duplicate (case‑insensitive) is added");
    }

    @Test
    void addDuplicateNonString_returnsFalseAndSizeUnchanged() {
        Integer value = 100;
        set.add(value);
        int sizeAfterFirstAdd = set.size();
        // Adding the same Integer again – duplicate.
        boolean added = set.add(value);
        assertFalse(added, "Adding a duplicate non‑String element must return false");
        assertEquals(sizeAfterFirstAdd, set.size(), "Size must remain unchanged when duplicate non‑String is added");
    }

    @Test
    void addNullElement_behavesLikeRegularSet() {
        // First null insertion should succeed.
        boolean added = set.add(null);
        assertTrue(added, "First insertion of null must return true");
        assertEquals(1, set.size(), "Size must be 1 after adding null");
        // Second null insertion is a duplicate.
        added = set.add(null);
        assertFalse(added, "Second insertion of null must return false");
        assertEquals(1, set.size(), "Size must stay 1 after duplicate null insertion");
    }

    @Test
    void addMultipleElementsAndVerifyReturnValues() {
        // Mix of strings with varying case and non‑strings.
        Object[] elements = { "Apple", "apple", "Banana", 123, 123 };
        boolean[] expectedResults = { true, false, true, true, false };
        for (int i = 0; i < elements.length; i++) {
            boolean result = set.add(elements[i]);
            assertEquals(expectedResults[i], result, "Unexpected result when adding element at index " + i + ": " + elements[i]);
        }
        // Expected distinct elements: "Apple", "Banana", 123
        assertEquals(3, set.size(), "Set should contain exactly three distinct elements");
        assertTrue(set.contains("APPLE"));
        assertTrue(set.contains("banana"));
        assertTrue(set.contains(123));
    }

    @Test
    void addDuplicateStringDifferentCase_returnsFalse() {
        set.add("Alpha");
        int sizeBefore = set.size();
        // Different case – should be considered duplicate.
        boolean added = set.add("ALPHA");
        assertFalse(added, "Case‑insensitive duplicate must return false");
        assertEquals(sizeBefore, set.size(), "Size must remain unchanged");
    }

    @Test
    void addNull_throwsNullPointerException() {
        // The underlying map (ConcurrentSkipListMap) rejects null keys.
        assertThrows(NullPointerException.class, () -> set.add(null), "Adding null must throw NullPointerException");
    }

    @Test
    void addDistinctNonStringElements() {
        assertTrue(set.add(100), "First insertion of Integer should succeed");
        assertEquals(1, set.size());
        assertFalse(set.add(100), "Second insertion of same Integer must be duplicate");
        assertEquals(1, set.size());
    }

    @Test
    void addIntegerAfterString_throwsClassCastException() {
        // establishes String as the key type in the map
        set.add("Alpha");
        assertThrows(ClassCastException.class, () -> set.add(1), "Adding an Integer after a String should throw ClassCastException");
    }

    @Test
    void reAddAfterRemoval_newCaseInsensitiveElement() {
        assertTrue(set.add("Delta"));
        assertTrue(set.remove("Delta"));
        assertTrue(set.isEmpty(), "Set should be empty after removal");
        // Adding with different case is now a brand‑new element.
        assertTrue(set.add("delta"));
        assertEquals(1, set.size());
    }

    @Test
    void addCompletelyDifferentString_returnsTrue() {
        set.add("Alpha");
        assertTrue(set.add("Beta"), "Adding a different string must succeed");
        assertEquals(2, set.size());
    }

    @Test
    void addNewStringObjectEqualToExisting_returnsFalse() {
        set.add("Alpha");
        // different instance, same value
        String another = new String("Alpha");
        assertFalse(set.add(another), "Duplicate value with different reference must return false");
        assertEquals(1, set.size());
    }

    @Test
    void addNonComparableObject_throwsClassCastException() {
        // does not implement Comparable
        Object obj = new Object();
        assertThrows(ClassCastException.class, () -> set.add(obj), "Adding a non‑Comparable object should throw ClassCastException");
    }

    @Test
    void addFooAfterString_throwsClassCastException() {
        // String establishes ordering
        set.add("Alpha");
        Foo foo = new Foo(1);
        assertThrows(ClassCastException.class, () -> set.add(foo), "Adding Foo after a String should throw ClassCastException");
    }

    @Test
    void addWhenInternalMapIsNull_throwsNullPointerException() throws Exception {
        // Use reflection to set the private 'map' field to null.
        Field mapField = CaseInsensitiveSet.class.getDeclaredField("map");
        mapField.setAccessible(true);
        mapField.set(set, null);
        assertThrows(NullPointerException.class, () -> set.add("Gamma"), "Invoking add() when internal map is null must throw NullPointerException");
    }

    @ParameterizedTest(name = "[{index}] add({0}) → expected: {1}")
    @CsvSource({ // element,               expectedResult (true=added, false=duplicate)
    "'Apple',                true", // duplicate case‑insensitive
    "'apple',                false", "'Banana',               true", "123,                    true", "123,                    false" })
    void addMultipleElementsParameterized(Object element, boolean expectedResult) {
        boolean result = set.add(element);
        assertEquals(expectedResult, result, () -> "Unexpected add() result for element: " + element);
    }

    @Test
    void verifySetContentsAfterMixedAdds() {
        Object[] elements = { "Apple", "apple", "Banana", 123, 123 };
        boolean[] expected = { true, false, true, true, false };
        for (int i = 0; i < elements.length; i++) {
            assertEquals(expected[i], set.add(elements[i]), "add() result mismatch at index " + i);
        }
        assertEquals(3, set.size(), "Set should contain exactly three distinct elements");
        assertTrue(set.contains("APPLE"));
        assertTrue(set.contains("banana"));
        assertTrue(set.contains(123));
    }
}
