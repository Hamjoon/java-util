package com.cedarsoftware.util;

import java.util.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
// import org.junit.*;
import org.junit.jupiter.api.Test;
// import static org.junit.Assert.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

class CompactSet_removeAll_Test {

    private CompactSet<String> set;

    private CompactSet<Integer> intSet;

    private CompactSet<String> caseSet;

    //     @BeforeEach
    void setupBeforeEach() {
        // Integer set used for the “Basic removal scenarios”
        intSet = new CompactSet<>();
        intSet.addAll(Arrays.asList(1, 2));
        // String set used for case‑sensitivity tests – default case‑sensitive
        caseSet = new CompactSet<>();
        caseSet.add("a");
        // Main set used by most of the tests
        set = new CompactSet<>();
        set.addAll(Arrays.asList("alpha", "beta", "gamma", "delta"));
    }

    //     @AfterEach
    void teardownAfterEach() {
        set = null;
        intSet = null;
        caseSet = null;
    }

    /* --------------------------------------------------------------------- */
    private static Stream<Arguments> basicRemovalProvider() {
        return Stream.of(// collection contains elements present in the set (remove 2, keep 1)
        Arguments.of(Arrays.asList(2, 3), true, Collections.singletonList(1)), // collection contains no elements from the set (nothing removed)
        Arguments.of(Arrays.asList(3, 4), false, Arrays.asList(1, 2)), // empty collection (nothing removed)
        Arguments.of(Collections.emptyList(), false, Arrays.asList(1, 2)), // collection contains all elements of the set (set becomes empty)
        Arguments.of(Arrays.asList(1, 2), true, Collections.emptyList()));
    }

    @Test
    void removeAll_existingElements_returnsTrueAndRemovesThem() {
        // given
        Collection<String> toRemove = Arrays.asList("beta", "delta");
        // when
        boolean changed = set.removeAll(toRemove);
        // then
        assertTrue(changed, "removeAll should report a change when elements are removed");
        assertEquals(2, set.size(), "Set should contain only the remaining elements");
        assertFalse(set.contains("beta"));
        assertFalse(set.contains("delta"));
        assertTrue(set.contains("alpha"));
        assertTrue(set.contains("gamma"));
    }

    @Test
    void removeAll_noMatchingElements_returnsFalseAndSetUnchanged() {
        // given
        Collection<String> toRemove = Arrays.asList("epsilon", "zeta");
        // when
        boolean changed = set.removeAll(toRemove);
        // then
        assertFalse(changed, "removeAll should report no change when nothing matches");
        assertEquals(4, set.size(), "Set size must stay unchanged");
        assertTrue(set.containsAll(Arrays.asList("alpha", "beta", "gamma", "delta")));
    }

    @Test
    void removeAll_collectionIsSuperset_clearsSet() {
        // given
        Collection<String> toRemove = Arrays.asList("alpha", "beta", "gamma", "delta", "extra");
        // when
        boolean changed = set.removeAll(toRemove);
        // then
        assertTrue(changed, "removeAll should report a change when the set becomes empty");
        assertTrue(set.isEmpty(), "Set must be empty after removing all its elements");
    }

    @Test
    void removeAll_collectionContainsDuplicates_behaviourIsIdempotent() {
        // given
        Collection<String> toRemove = Arrays.asList("beta", "beta", "beta");
        // when
        boolean changed = set.removeAll(toRemove);
        // then
        assertTrue(changed, "removeAll should report a change even if the collection has duplicates");
        assertEquals(3, set.size(), "Only one element should be removed");
        assertFalse(set.contains("beta"));
    }

    @Test
    void removeAll_nullCollection_throwsNullPointerException() {
        // given
        Collection<?> nullCollection = null;
        // when / then
        assertThrows(NullPointerException.class, () -> set.removeAll(nullCollection));
    }

    @Test
    void removeAll_collectionWithNullElement_doesNotAffectSet() {
        // given
        Collection<String> toRemove = Collections.singletonList(null);
        // when
        boolean changed = set.removeAll(toRemove);
        // then
        assertFalse(changed, "removeAll should report no change when collection contains only null");
        assertEquals(4, set.size(), "Set size must stay unchanged");
    }

    @Test
    void removeAll_onCaseSensitiveSet_respectsCase() {
        // given – a case‑sensitive set already contains "alpha"
        Collection<String> toRemove = Collections.singletonList("ALPHA");
        // when
        boolean changed = set.removeAll(toRemove);
        // then
        assertFalse(changed, "Case‑sensitive set must not remove element with different case");
        assertTrue(set.contains("alpha"));
    }

    @ParameterizedTest(name = "{index}: removeAll({0}) → changed={1}, contents={2}")
    @MethodSource("basicRemovalProvider")
    void removeAll_basicScenarios(Collection<?> toRemove, boolean expectedChanged, List<Integer> expectedContents) {
        // when
        boolean changed = intSet.removeAll(toRemove);
        // then
        assertEquals(expectedChanged, changed, "removeAll should report " + expectedChanged + " for collection " + toRemove);
        assertEquals(expectedContents.size(), intSet.size(), "Set size after removal should match expected");
        assertTrue(intSet.containsAll(expectedContents), "Set contents after removal should be " + expectedContents);
    }

    @Test
    void removeAll_caseInsensitiveSet_removesIgnoringCase() {
        // Build a case‑insensitive set containing "a"
        CompactSet<String> ciSet = CompactSet.<String>builder().caseSensitive(false).build();
        ciSet.add("a");
        // collection with upper‑case "A"
        Collection<String> toRemove = Collections.singletonList("A");
        boolean changed = ciSet.removeAll(toRemove);
        assertTrue(changed, "Case‑insensitive set should remove element regardless of case");
        assertTrue(ciSet.isEmpty(), "Set must be empty after removal");
    }

    @Test
    void removeAll_caseSensitiveSet_doesNotRemoveDifferentCase() {
        // caseSet is case‑sensitive by default and already contains "a"
        Collection<String> toRemove = Collections.singletonList("A");
        boolean changed = caseSet.removeAll(toRemove);
        assertFalse(changed, "Case‑sensitive set must not remove element with different case");
        assertTrue(caseSet.contains("a"));
    }

    @Test
    void removeAll_selfReference_throwsConcurrentModificationException() {
        // Calling removeAll with the same set should raise ConcurrentModificationException
        assertThrows(ConcurrentModificationException.class, () -> intSet.removeAll(intSet));
    }

    @Test
    void intSet_removeAll_collectionContainsPresentAndAbsentElements() {
        // collection = [2, 3] – 2 is present, 3 is not
        Collection<Integer> toRemove = Arrays.asList(2, 3);
        boolean changed = intSet.removeAll(toRemove);
        assertTrue(changed);
        assertEquals(1, intSet.size());
        assertTrue(intSet.containsAll(Collections.singletonList(1)));
    }

    @Test
    void intSet_removeAll_collectionContainsOnlyAbsentElements() {
        // collection = [3, 4] – none are present
        Collection<Integer> toRemove = Arrays.asList(3, 4);
        boolean changed = intSet.removeAll(toRemove);
        assertFalse(changed);
        assertEquals(2, intSet.size());
        assertTrue(intSet.containsAll(Arrays.asList(1, 2)));
    }

    @Test
    void intSet_removeAll_emptyCollection() {
        Collection<Integer> toRemove = Collections.emptyList();
        boolean changed = intSet.removeAll(toRemove);
        assertFalse(changed);
        assertEquals(2, intSet.size());
        assertTrue(intSet.containsAll(Arrays.asList(1, 2)));
    }

    @Test
    void intSet_removeAll_collectionContainsAllElements() {
        Collection<Integer> toRemove = Arrays.asList(1, 2);
        boolean changed = intSet.removeAll(toRemove);
        assertTrue(changed);
        assertTrue(intSet.isEmpty());
    }
}
