package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CompactSet_isEmpty_Test {

    private CompactSet<String> set;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh CompactSet instance before each test.
        set = new CompactSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly necessary but keeps the pattern consistent.
        set = null;
    }

    private static Stream<Arguments> actionSequences() {
        return Stream.of(// description, actions, expected emptiness
        Arguments.of("Add then clear", List.of("add:a", "clear"), true), Arguments.of("Add null then remove null", List.of("add:null", "remove:null"), true), Arguments.of("Add two, remove one", List.of("add:x", "add:y", "remove:x"), false), Arguments.of("Add duplicate, remove once", List.of("add:z", "add:z", "remove:z"), true), Arguments.of("Clear empty set", List.of("clear"), true));
    }

    @Test
    void isEmpty_OnNewSet_ReturnsTrue() {
        // Given a newly created CompactSet
        // When checking emptiness
        boolean empty = set.isEmpty();
        // Then it should be empty
        assertTrue(empty, "A newly constructed CompactSet must be empty");
    }

    @Test
    void isEmpty_AfterAdd_ReturnsFalse() {
        // Given a CompactSet with one element added
        set.add("element");
        // When checking emptiness
        boolean empty = set.isEmpty();
        // Then it should not be empty
        assertFalse(empty, "CompactSet should not be empty after adding an element");
    }

    @Test
    void isEmpty_AfterAddNull_ReturnsFalse() {
        // Given a CompactSet with a null element added (null is allowed)
        set.add(null);
        // When checking emptiness
        boolean empty = set.isEmpty();
        // Then it should not be empty
        assertFalse(empty, "CompactSet should not be empty after adding a null element");
    }

    @Test
    void isEmpty_AfterClear_ReturnsTrue() {
        // Given a CompactSet with several elements
        set.add("a");
        set.add("b");
        set.add("c");
        // When clearing the set
        set.clear();
        // Then it should be empty again
        assertTrue(set.isEmpty(), "CompactSet should be empty after clear()");
    }

    @Test
    void isEmpty_AfterRemoveLastElement_ReturnsTrue() {
        // Given a CompactSet with a single element
        set.add("only");
        // When that element is removed
        set.remove("only");
        // Then the set should be empty
        assertTrue(set.isEmpty(), "CompactSet should be empty after removing the last element");
    }

    @Test
    void isEmpty_AfterAddAllAndRemoveAll_ReturnsTrue() {
        // Given a collection of elements
        Set<String> source = new HashSet<>(Set.of("x", "y", "z"));
        // When constructing a CompactSet from that collection
        CompactSet<String> fromCollection = new CompactSet<>(source);
        // And removing all elements
        fromCollection.removeAll(source);
        // Then it should be empty
        assertTrue(fromCollection.isEmpty(), "CompactSet created from a collection should be empty after removing all its elements");
    }

    @Test
    void isEmpty_AfterCrossingCompactSize_StaysConsistent() {
        // Given a CompactSet that will cross the compactSize threshold (default 80)
        for (int i = 0; i < 85; i++) {
            set.add("item-" + i);
        }
        // When checking emptiness
        boolean empty = set.isEmpty();
        // Then it must be false (the set holds many items)
        assertFalse(empty, "CompactSet should not be empty after crossing compactSize threshold");
    }

    @Test
    void isEmpty_AddDuplicate_ElementRemainsNonEmpty() {
        // Add an element, then add the same element again.
        set.add("dup");
        // duplicate, should have no effect on emptiness
        set.add("dup");
        assertFalse(set.isEmpty(), "Adding a duplicate must not make the set empty");
    }

    @Test
    void isEmpty_AddNullThenRemoveNull_ReturnsTrue() {
        // Add a null element and then remove it.
        set.add(null);
        assertFalse(set.isEmpty(), "After adding null, set must be non‑empty");
        set.remove(null);
        assertTrue(set.isEmpty(), "Removing the only null element should revert to empty");
    }

    @Test
    void isEmpty_RemoveNonExistingElement_LeavesNonEmpty() {
        // Add a distinct element, attempt to remove a different one.
        set.add("present");
        boolean removed = set.remove("absent");
        assertFalse(removed, "remove() should return false for absent element");
        assertFalse(set.isEmpty(), "Set should stay non‑empty after failed removal");
    }

    @Test
    void isEmpty_RemoveFromAlreadyEmpty_ReturnsTrue() {
        // Remove an element from an empty set.
        boolean removed = set.remove("nothing");
        assertFalse(removed, "remove() on empty set should return false");
        assertTrue(set.isEmpty(), "Emptiness must remain true after removing from empty set");
    }

    @Test
    void isEmpty_ClearOnEmpty_IsIdempotent() {
        // Clear an already empty set.
        // first clear (no‑op)
        set.clear();
        assertTrue(set.isEmpty(), "Set should stay empty after clear()");
        // second clear, still no‑op
        set.clear();
        assertTrue(set.isEmpty(), "Second clear() must also leave set empty");
    }

    @Test
    void isEmpty_IteratorRemove_EmptiesSet() {
        // Add a single element, obtain iterator, remove via iterator.
        set.add("single");
        Iterator<String> it = set.iterator();
        assertTrue(it.hasNext(), "Iterator should have the element");
        it.next();
        // iterator‑based removal
        it.remove();
        assertTrue(set.isEmpty(), "Set should be empty after iterator.remove()");
    }

    @Test
    void isEmpty_RemoveOneOfTwoElements_StaysNonEmpty() {
        // Add two distinct elements, remove one.
        set.add("first");
        set.add("second");
        set.remove("first");
        assertFalse(set.isEmpty(), "Set should still contain one element after removing one of two");
    }

    @Test
    void isEmpty_RemoveRemainingElement_BecomesEmpty() {
        // Continue from previous test: remove the last remaining element.
        set.add("alpha");
        set.add("beta");
        set.remove("alpha");
        set.remove("beta");
        assertTrue(set.isEmpty(), "Set should be empty after removing all elements");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("actionSequences")
    void isEmpty_ActionSequenceTests(String description, List<String> actions, boolean expectedEmpty) {
        // Re‑initialize for each parameterized run
        CompactSet<String> pSet = new CompactSet<>();
        for (String act : actions) {
            if (act.startsWith("add:")) {
                String val = act.substring(4);
                pSet.add("null".equals(val) ? null : val);
            } else if (act.startsWith("remove:")) {
                String val = act.substring(7);
                pSet.remove("null".equals(val) ? null : val);
            } else if ("clear".equals(act)) {
                pSet.clear();
            }
        }
        assertEquals(expectedEmpty, pSet.isEmpty(), () -> "After actions " + actions + " expected isEmpty() to be " + expectedEmpty);
    }
}
