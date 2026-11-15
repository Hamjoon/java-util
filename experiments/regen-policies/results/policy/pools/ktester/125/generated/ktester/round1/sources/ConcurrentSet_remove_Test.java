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
class ConcurrentSet_remove_Test {

    /**
     * The set used in each test.
     */
    private ConcurrentSet<Integer> intSet;

    /**
     * The set used for {@code null} handling tests (type {@code String} because {@code null}
     *  can be stored in any generic type).
     */
    private ConcurrentSet<String> stringSet;

    @BeforeEach
    void setupBeforeEach() {
        // Fresh instances for every test to guarantee isolation.
        intSet = new ConcurrentSet<>();
        stringSet = new ConcurrentSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and make sure no stray references survive between tests.
        intSet = null;
        stringSet = null;
    }

    private static Stream<String> provideExistingElements() {
        // Currently only the "apple" case is defined in the JSON, but the stream can be expanded.
        return Stream.of("apple");
    }

    @Test
    void removeExistingElement_returnsTrueAndRemovesIt() {
        // given
        assertTrue(intSet.add(42), "Pre‑condition: element must be added successfully");
        // when
        boolean removed = intSet.remove(42);
        // then
        assertTrue(removed, "remove() should return true for an element that was present");
        assertFalse(intSet.contains(42), "Set must no longer contain the removed element");
        assertTrue(intSet.isEmpty(), "Set should be empty after removing the sole element");
    }

    @Test
    void removeNonExistingElement_returnsFalseAndLeavesSetUnchanged() {
        // given
        intSet.addAll(Arrays.asList(1, 2, 3));
        // when
        boolean removed = intSet.remove(99);
        // then
        assertFalse(removed, "remove() should return false when the element is not present");
        assertEquals(3, intSet.size(), "Set size must remain unchanged");
        assertTrue(intSet.containsAll(Arrays.asList(1, 2, 3)), "All originally added elements must still be present");
    }

    @Test
    void removeNullElement_whenNullIsPresent_returnsTrue() {
        // given
        assertTrue(stringSet.add(null), "Pre‑condition: null must be added successfully");
        assertTrue(stringSet.contains(null), "Set must contain null before removal");
        // when
        boolean removed = stringSet.remove(null);
        // then
        assertTrue(removed, "remove(null) should return true when null is present");
        assertFalse(stringSet.contains(null), "Set must no longer contain null after removal");
        assertTrue(stringSet.isEmpty(), "Set should be empty after removing the only element");
    }

    @Test
    void removeNullElement_whenNullIsAbsent_returnsFalse() {
        // given
        stringSet.addAll(Arrays.asList("a", "b"));
        // when
        boolean removed = stringSet.remove(null);
        // then
        assertFalse(removed, "remove(null) should return false when null is not present");
        assertEquals(2, stringSet.size(), "Set size must remain unchanged");
        assertTrue(stringSet.containsAll(Arrays.asList("a", "b")), "Existing non‑null elements must remain untouched");
    }

    @Test
    void removeAfterClear_alwaysReturnsFalse() {
        // given
        intSet.addAll(Arrays.asList(10, 20, 30));
        intSet.clear();
        assertTrue(intSet.isEmpty(), "Set must be empty after clear()");
        // when
        boolean removed = intSet.remove(10);
        // then
        assertFalse(removed, "remove() should return false on an empty set");
    }

    @Test
    void removeSameElementTwice_firstCallTrueSecondCallFalse() {
        // given
        assertTrue(intSet.add(7), "Pre‑condition: element must be added successfully");
        // when
        boolean firstRemoval = intSet.remove(7);
        boolean secondRemoval = intSet.remove(7);
        // then
        assertTrue(firstRemoval, "First removal should succeed and return true");
        assertFalse(secondRemoval, "Second removal of the same element should return false");
        assertTrue(intSet.isEmpty(), "Set should be empty after the element has been removed");
    }

    @Test
    void removeDoesNotAffectOtherElements() {
        // given
        intSet.addAll(Arrays.asList(1, 2, 3, 4));
        // when
        boolean removed = intSet.remove(3);
        // then
        assertTrue(removed, "remove(3) should return true");
        assertFalse(intSet.contains(3), "Set must no longer contain the removed element");
        Set<Integer> expected = new HashSet<>(Arrays.asList(1, 2, 4));
        assertEquals(expected, new HashSet<>(intSet), "All other elements must remain unchanged");
    }

    /**
     * Parameterized test for removing an element that is known to exist.
     * The JSON test case uses the string "apple" and expects {@code true}.
     */
    @ParameterizedTest(name = "remove existing element ''{0}'' should return true")
    @MethodSource("provideExistingElements")
    void removeExistingElement_parameterized(String element) {
        // given
        ConcurrentSet<String> set = new ConcurrentSet<>();
        assertTrue(set.add(element), "Pre‑condition: element must be added successfully");
        // when
        boolean removed = set.remove(element);
        // then
        assertTrue(removed, "remove() should return true for an existing element");
        assertFalse(set.contains(element), "Set must no longer contain the removed element");
    }

    /**
     * The JSON test case expects a {@link NullPointerException} when {@code null}
     * is passed to {@code remove(Object)}. According to the library documentation,
     * {@code ConcurrentSet} supports {@code null} via a sentinel, but the behaviour
     * may differ depending on the internal implementation version.
     *
     * This test asserts that a {@code NullPointerException} is thrown, satisfying the
     * contract described in the supplied JSON.
     */
    @Test
    void removeNullElement_throwsNullPointerException() {
        // given
        ConcurrentSet<String> set = new ConcurrentSet<>();
        // No need to add null; we are testing the direct call.
        // when / then
        assertThrows(NullPointerException.class, () -> set.remove(null), "remove(null) is expected to throw NullPointerException according to the JSON spec");
    }
}
