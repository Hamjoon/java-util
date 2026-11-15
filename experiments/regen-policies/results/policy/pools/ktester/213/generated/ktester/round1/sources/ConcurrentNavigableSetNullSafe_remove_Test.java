package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableSetNullSafe_remove_Test {

    /**
     * The set used in each test.
     */
    private NavigableSet<String> set;

    /**
     * Simple comparable POJO used for the custom‑object test.
     */
    private static final class Person implements Comparable<Person> {

        private final String name;

        Person(String name) {
            this.name = name;
        }

        @Override
        public int compareTo(Person o) {
            return this.name.compareTo(o.name);
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof Person p) && Objects.equals(name, p.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }

        @Override
        public String toString() {
            return "Person{" + name + '}';
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh instance for every test to guarantee isolation.
        set = new ConcurrentNavigableSetNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and avoid accidental reuse.
        set = null;
    }

    /**
     * Provides all cases that can be expressed as a simple boolean outcome.
     */
    private static Stream<RemoveCase> booleanRemoveCases() {
        return Stream.of(// remove existing element
        new RemoveCase("testElement", true, "Removing an element that exists in the set should return true and the element should no longer be present."), // remove non‑existing element
        new RemoveCase("absentElement", false, "Attempting to remove an element that is not present should return false and the set remains unchanged."), // remove null element when present
        new RemoveCase(null, true, "If the set contains a null (handled via maskNull), removing null should return true."), // remove null element when absent
        new RemoveCase(null, false, "If the set does not contain a null, removing null should return false."), // remove after clear
        new RemoveCase("anyElement", false, "After invoking clear() on the set, attempting to remove any element should return false because the set is empty."), // remove size decrement
        new RemoveCase("elem1", true, "Removing an existing element should return true and the set size should decrease by one."), // remove equal but distinct instance
        new RemoveCase(new String("duplicate"), true, "The set contains a String \"duplicate\" added earlier. Removing a different String instance with the same content should succeed."), // remove from set with multiple elements
        new RemoveCase("middle", true, "The set is pre‑populated with several distinct elements (e.g., \"first\", \"middle\", \"last\"). Removing \"middle\" should return true and leave the other elements untouched."), // remove null after it was removed
        new RemoveCase(null, false, "A null element (masked internally) is removed once successfully. A subsequent attempt to remove null again should return false because the null sentinel is no longer present."), // remove after removing all elements
        new RemoveCase("lastElement", false, "All elements have been removed from the set (e.g., via successive pollFirst() calls). Attempting to remove any element now should return false."));
    }

    @Test
    @DisplayName("remove(Object) returns true and actually removes an existing element")
    void testRemoveExistingElement() {
        // given
        set.add("apple");
        set.add("banana");
        set.add("cherry");
        set.add(null);
        // when
        boolean removed = set.remove("banana");
        // then
        assertTrue(removed, "remove should return true for an element that exists");
        assertEquals(3, set.size(), "size should decrease by one");
        assertFalse(set.contains("banana"), "set should no longer contain the removed element");
        // verify remaining order (null is the last element in natural order)
        var it = set.iterator();
        assertEquals("apple", it.next());
        assertEquals("cherry", it.next());
        assertEquals(null, it.next());
        assertFalse(it.hasNext());
    }

    @Test
    @DisplayName("remove(Object) correctly removes a null element")
    void testRemoveNullElement() {
        // given
        set.add(null);
        set.add("apple");
        set.add("banana");
        // when
        boolean removed = set.remove(null);
        // then
        assertTrue(removed, "remove should return true when null is present");
        assertEquals(2, set.size(), "size should reflect the removal of null");
        assertFalse(set.contains(null), "set should no longer contain null");
        // verify remaining order
        var it = set.iterator();
        assertEquals("apple", it.next());
        assertEquals("banana", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    @DisplayName("remove(Object) returns false when the element is not present")
    void testRemoveNonExistingElement() {
        // given
        set.add("apple");
        set.add("banana");
        // when
        boolean removed = set.remove("orange");
        // then
        assertFalse(removed, "remove should return false for a non‑existent element");
        assertEquals(2, set.size(), "size must remain unchanged");
        assertTrue(set.contains("apple"));
        assertTrue(set.contains("banana"));
    }

    @ParameterizedTest(name = "{index}: {0.description}")
    @MethodSource("booleanRemoveCases")
    void testRemoveBooleanOutcomes(RemoveCase tc) {
        // -------------------------------------------------------------
        // Arrange – build the appropriate initial state for each case
        // -------------------------------------------------------------
        switch(tc.description()) {
            case "Removing an element that exists in the set should return true and the element should no longer be present." ->
                {
                    set.add("testElement");
                }
            case "Attempting to remove an element that is not present should return false and the set remains unchanged." ->
                {
                    set.add("absentElement");
                    set.add("someOther");
                }
            case "If the set contains a null (handled via maskNull), removing null should return true." ->
                {
                    set.add(null);
                    set.add("x");
                }
            case "If the set does not contain a null, removing null should return false." ->
                {
                    set.add("x");
                }
            case "After invoking clear() on the set, attempting to remove any element should return false because the set is empty." ->
                {
                    set.add("anyElement");
                    set.clear();
                }
            case "Removing an existing element should return true and the set size should decrease by one." ->
                {
                    set.add("elem1");
                    set.add("other");
                }
            case "The set contains a String \"duplicate\" added earlier. Removing a different String instance with the same content should succeed." ->
                {
                    set.add("duplicate");
                }
            case "The set is pre‑populated with several distinct elements (e.g., \"first\", \"middle\", \"last\"). Removing \"middle\" should return true and leave the other elements untouched." ->
                {
                    set.add("first");
                    set.add("middle");
                    set.add("last");
                }
            case "A null element (masked internally) is removed once successfully. A subsequent attempt to remove null again should return false because the null sentinel is no longer present." ->
                {
                    set.add(null);
                    // first removal to make the sentinel disappear
                    assertTrue(set.remove(null), "first removal of null must succeed");
                }
            case "All elements have been removed from the set (e.g., via successive pollFirst() calls). Attempting to remove any element now should return false." ->
                {
                    set.add("first");
                    set.add("second");
                    set.add("lastElement");
                    // empty the set using pollFirst()
                    while (!set.isEmpty()) {
                        set.pollFirst();
                    }
                    assertTrue(set.isEmpty(), "set must be empty before the removal attempt");
                }
            default ->
                {
                    // No special preparation needed.
                }
        }
        // -------------------------------------------------------------
        // Act
        // -------------------------------------------------------------
        boolean result = set.remove(tc.input());
        // -------------------------------------------------------------
        // Assert
        // -------------------------------------------------------------
        assertEquals(tc.expected(), result, tc.description());
        // Additional verification for the true‑cases that require state checks
        if (Boolean.TRUE.equals(tc.expected())) {
            // element should no longer be present
            assertFalse(set.contains(tc.input()), "Element should have been removed");
        }
    }

    @Test
    @DisplayName("remove with incompatible type should throw ClassCastException")
    void testRemoveIncompatibleType() {
        set.add("string");
        // The set is of String, passing an Integer forces a cast failure inside remove()
        assertThrows(ClassCastException.class, () -> set.remove(12345), "Removing an object of an incompatible type must raise ClassCastException");
    }

    @Test
    @DisplayName("remove from an unmodifiable underlying set should propagate UnsupportedOperationException")
    void testRemoveFromUnmodifiableSet() throws Exception {
        // Build a normal ConcurrentNavigableSetNullSafe, then replace its internalSet with an unmodifiable wrapper.
        ConcurrentNavigableSetNullSafe<String> unsafeSet = new ConcurrentNavigableSetNullSafe<>();
        // Populate with a value to ensure the set is not empty.
        unsafeSet.add("anyElement");
        // Use reflection to swap the private final field `internalSet`.
        Field field = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        field.setAccessible(true);
        NavigableSet<Object> original = (NavigableSet<Object>) field.get(unsafeSet);
        NavigableSet<Object> unmodifiable = Collections.unmodifiableNavigableSet(original);
        field.set(unsafeSet, unmodifiable);
        // Now any mutating operation should throw UnsupportedOperationException.
        assertThrows(UnsupportedOperationException.class, () -> unsafeSet.remove("anyElement"), "remove on an unmodifiable underlying set must throw UnsupportedOperationException");
    }

    @Test
    @DisplayName("remove custom comparable object")
    void testRemoveCustomComparableObject() {
        ConcurrentNavigableSetNullSafe<Person> personSet = new ConcurrentNavigableSetNullSafe<>();
        Person alice = new Person("Alice");
        Person bob = new Person("Bob");
        personSet.add(alice);
        personSet.add(bob);
        // Remove a *different* instance that is equal to `alice`.
        Person aliceCopy = new Person("Alice");
        assertTrue(personSet.remove(aliceCopy), "Removing an existing Person (by equality) should succeed");
        assertFalse(personSet.contains(alice), "Original instance should no longer be present");
        assertEquals(1, personSet.size(), "Only one element should remain");
        assertTrue(personSet.contains(bob), "Bob should still be present");
    }

    @Test
    @DisplayName("remove size decrement verification")
    void testRemoveSizeDecrement() {
        set.add("elem1");
        set.add("elem2");
        int sizeBefore = set.size();
        assertTrue(set.remove("elem1"), "Removal must return true for existing element");
        assertEquals(sizeBefore - 1, set.size(), "Size must decrease by one after successful removal");
    }
}
