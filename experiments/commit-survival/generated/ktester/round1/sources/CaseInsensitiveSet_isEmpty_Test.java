package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveSet_isEmpty_Test {

    private CaseInsensitiveSet<String> set;

    @BeforeEach
    void setupBeforeEach() {
        // Use the no‑arg constructor for most tests; individual tests may replace this.
        set = new CaseInsensitiveSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and avoid accidental reuse between tests.
        set = null;
    }

    @Test
    @DisplayName("isEmpty() returns true for a newly created set")
    void testIsEmptyOnNewSet() {
        // given – set is created in @BeforeEach
        // when
        boolean empty = set.isEmpty();
        // then
        assertTrue(empty, "A freshly constructed set should be empty");
    }

    @Test
    @DisplayName("isEmpty() returns false after adding an element")
    void testIsEmptyAfterAdd() {
        // given
        set.add("hello");
        // when
        boolean empty = set.isEmpty();
        // then
        assertFalse(empty, "Set should not be empty after adding an element");
    }

    @Test
    @DisplayName("isEmpty() returns true after clearing a populated set")
    void testIsEmptyAfterClear() {
        // given
        set.addAll(Arrays.asList("a", "b", "c"));
        assertFalse(set.isEmpty(), "Pre‑condition: set should be non‑empty before clear");
        // when
        set.clear();
        // then
        assertTrue(set.isEmpty(), "Set should be empty after clear()");
    }

    @Test
    @DisplayName("isEmpty() reflects the emptiness of the supplied collection")
    void testIsEmptyWithCollectionConstructor() {
        // given – an empty collection
        Collection<String> emptyColl = new HashSet<>();
        CaseInsensitiveSet<String> emptySet = new CaseInsensitiveSet<>(emptyColl);
        // when / then
        assertTrue(emptySet.isEmpty(), "Set constructed from an empty collection should be empty");
        // given – a non‑empty collection
        Collection<String> nonEmptyColl = Arrays.asList("x", "y");
        CaseInsensitiveSet<String> nonEmptySet = new CaseInsensitiveSet<>(nonEmptyColl);
        // when / then
        assertFalse(nonEmptySet.isEmpty(), "Set constructed from a non‑empty collection should not be empty");
    }

    @Test
    @DisplayName("isEmpty() returns true for sets created with initial capacity")
    void testIsEmptyWithInitialCapacityConstructors() {
        // given – constructor with only initial capacity
        CaseInsensitiveSet<String> capSet = new CaseInsensitiveSet<>(10);
        assertTrue(capSet.isEmpty(), "Set with initial capacity should start empty");
        // given – constructor with capacity and load factor
        CaseInsensitiveSet<String> capLoadSet = new CaseInsensitiveSet<>(10, 0.75f);
        assertTrue(capLoadSet.isEmpty(), "Set with capacity and load factor should start empty");
    }

    @Test
    @DisplayName("isEmpty() returns true after adding and then removing the same element")
    void testIsEmptyAfterAddThenRemove() {
        // given
        set.add("alpha");
        // when
        set.remove("alpha");
        // then
        assertTrue(set.isEmpty(), "Set should be empty after removing the only element that was added");
    }

    @Test
    @DisplayName("isEmpty() returns false after adding multiple distinct elements")
    void testIsEmptyAfterMultipleAdds() {
        // given
        set.add("one");
        set.add("two");
        set.add("three");
        // then
        assertFalse(set.isEmpty(), "Set should be non‑empty after adding several distinct elements");
    }

    @Test
    @DisplayName("isEmpty() returns false after add() followed by addAll()")
    void testIsEmptyAfterAddAndAddAll() {
        // given
        set.add("first");
        // when
        set.addAll(Arrays.asList("second", "third"));
        // then
        assertFalse(set.isEmpty(), "Set should be non‑empty after add() and addAll()");
    }

    @Test
    @DisplayName("isEmpty() returns true after retainAll() with an empty collection")
    void testIsEmptyAfterRetainAllEmpty() {
        // given – populate the set with several elements
        set.addAll(Arrays.asList("a", "b", "c"));
        assertFalse(set.isEmpty(), "Pre‑condition: set should be non‑empty before retainAll");
        // when – retain only elements present in an empty collection
        set.retainAll(Collections.emptyList());
        // then
        assertTrue(set.isEmpty(), "Set should be empty after retainAll() with an empty collection");
    }

    @Test
    @DisplayName("isEmpty() returns true when removing an element with different case")
    void testIsEmptyCaseInsensitiveRemoval() {
        // given – add a capitalised string
        set.add("Test");
        // when – remove using a different case
        boolean removed = set.remove("test");
        // then
        assertTrue(removed, "Removal should succeed regardless of case");
        assertTrue(set.isEmpty(), "Set should be empty after case‑insensitive removal");
    }

    @Test
    @DisplayName("isEmpty() throws NullPointerException when internal map is null")
    void testIsEmptyWhenMapIsNullThrows() throws Exception {
        // given – a set instance whose private 'map' field is forced to null via reflection
        CaseInsensitiveSet<String> faultySet = new CaseInsensitiveSet<>();
        Field mapField = CaseInsensitiveSet.class.getDeclaredField("map");
        mapField.setAccessible(true);
        mapField.set(faultySet, null);
        // when / then – invoking isEmpty() should propagate the NPE from map.isEmpty()
        assertThrows(NullPointerException.class, faultySet::isEmpty, "Calling isEmpty() when internal map is null should throw NullPointerException");
    }
}
