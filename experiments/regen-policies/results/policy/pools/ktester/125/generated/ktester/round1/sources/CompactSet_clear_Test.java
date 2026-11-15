package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.*;
import java.util.stream.IntStream;

@ExtendWith(MockitoExtension.class)
class CompactSet_clear_Test {

    private CompactSet<String> set;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh CompactSet for each test case
        set = new CompactSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear the set in case a test fails before invoking clear()
        if (set != null && !set.isEmpty()) {
            set.clear();
        }
    }

    @Test
    void clearRemovesAllElements() {
        // given
        set.add("alpha");
        set.add("beta");
        set.add("gamma");
        assertFalse(set.isEmpty(), "Set should contain elements before clear");
        assertEquals(3, set.size(), "Set size should be 3 before clear");
        // when
        set.clear();
        // then
        assertTrue(set.isEmpty(), "Set should be empty after clear");
        assertEquals(0, set.size(), "Set size should be 0 after clear");
        assertFalse(set.contains("alpha"));
        assertFalse(set.contains("beta"));
        assertFalse(set.contains("gamma"));
    }

    @Test
    void clearOnAlreadyEmptySetIsIdempotent() {
        // given
        assertTrue(set.isEmpty(), "Set starts empty");
        assertEquals(0, set.size(), "Initial size should be 0");
        // when
        set.clear();
        // then
        assertTrue(set.isEmpty(), "Set should remain empty after clear");
        assertEquals(0, set.size(), "Size should remain 0 after clear");
    }

    @Test
    void clearAfterIteratorRemovalLeavesSetEmpty() {
        // given
        set.add("one");
        set.add("two");
        set.add("three");
        assertEquals(3, set.size());
        // remove all elements via iterator
        var iterator = set.iterator();
        while (iterator.hasNext()) {
            iterator.next();
            iterator.remove();
        }
        assertTrue(set.isEmpty(), "Set should be empty after iterator removals");
        // when
        // should be a no‑op but must not throw
        set.clear();
        // then
        assertTrue(set.isEmpty(), "Set should still be empty after clear");
        assertEquals(0, set.size());
    }

    @Test
    void clearDoesNotAffectSubsequentAdds() {
        // given
        set.add("first");
        set.clear();
        assertTrue(set.isEmpty());
        // when
        boolean added = set.add("second");
        // then
        assertTrue(added, "Adding after clear should succeed");
        assertEquals(1, set.size());
        assertTrue(set.contains("second"));
        assertFalse(set.contains("first"));
    }

    @Test
    void clearTwiceIsIdempotent() {
        // given
        set.add("x");
        set.add("y");
        // first clear, set becomes empty
        set.clear();
        // when
        // second clear
        set.clear();
        // then
        assertTrue(set.isEmpty(), "Set should remain empty after second clear");
        assertEquals(0, set.size(), "Size should stay 0 after second clear");
    }

    @Test
    void clearRemovesNullElementIfSupported() {
        // given
        // CompactSet permits null values (behaviour mirrors java.util.HashSet)
        set.add(null);
        set.add("notNull");
        assertEquals(2, set.size());
        assertTrue(set.contains(null));
        // when
        set.clear();
        // then
        assertTrue(set.isEmpty(), "Set should be empty after clear");
        assertEquals(0, set.size());
        assertFalse(set.contains(null), "Null element should be removed");
        assertFalse(set.contains("notNull"));
    }

    @Test
    void clearAfterAddAndRemoveOperations() {
        // given
        set.add("A");
        set.add("B");
        set.add("C");
        // now set contains A and C
        set.remove("B");
        assertEquals(2, set.size());
        assertTrue(set.contains("A"));
        assertTrue(set.contains("C"));
        assertFalse(set.contains("B"));
        // when
        set.clear();
        // then
        assertTrue(set.isEmpty(), "Set should be empty after clear");
        assertEquals(0, set.size());
        assertFalse(set.contains("A"));
        assertFalse(set.contains("C"));
    }

    @Test
    void clearAfterAddAllAndRemoveAll() {
        // given
        List<String> elements = List.of("one", "two", "three", "four");
        set.addAll(elements);
        assertEquals(4, set.size());
        // remove all via removeAll
        set.removeAll(elements);
        assertTrue(set.isEmpty(), "Set should be empty after removeAll");
        // when
        set.clear();
        // then
        assertTrue(set.isEmpty(), "Set should stay empty after clear");
        assertEquals(0, set.size());
        elements.forEach(e -> assertFalse(set.contains(e)));
    }

    @Test
    void clearAfterRetainAllOperation() {
        // given
        set.addAll(List.of("A", "B", "C", "D"));
        assertEquals(4, set.size());
        // retain only A and C
        set.retainAll(Set.of("A", "C"));
        assertEquals(2, set.size());
        assertTrue(set.contains("A"));
        assertTrue(set.contains("C"));
        assertFalse(set.contains("B"));
        assertFalse(set.contains("D"));
        // when
        set.clear();
        // then
        assertTrue(set.isEmpty(), "Set should be empty after clear");
        assertEquals(0, set.size());
        assertFalse(set.contains("A"));
        assertFalse(set.contains("C"));
    }

    @Test
    void clearWorksWithLargeDataset() {
        // given
        int largeSize = 10_000;
        IntStream.range(0, largeSize).forEach(i -> set.add("elem-" + i));
        assertEquals(largeSize, set.size());
        // when
        set.clear();
        // then
        assertTrue(set.isEmpty(), "Set should be empty after clearing large dataset");
        assertEquals(0, set.size());
        // Spot‑check a few elements to ensure they are gone
        assertFalse(set.contains("elem-0"));
        assertFalse(set.contains("elem-5000"));
        assertFalse(set.contains("elem-9999"));
    }

    @Test
    void iteratorIsEmptyAfterClear() {
        // given
        set.addAll(List.of("a", "b", "c"));
        assertFalse(set.isEmpty());
        // when
        set.clear();
        // then
        Iterator<String> it = set.iterator();
        assertFalse(it.hasNext(), "Iterator should have no elements after clear");
        assertThrows(NoSuchElementException.class, it::next, "Calling next() on empty iterator should throw");
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 2, 3, 4, 5 })
    void repeatedClearCallsLeaveSetEmpty(int repetitions) {
        // given
        set.addAll(List.of("x", "y", "z"));
        assertEquals(3, set.size());
        // when
        for (int i = 0; i < repetitions; i++) {
            set.clear();
        }
        // then
        assertTrue(set.isEmpty(), "Set should be empty after " + repetitions + " clear() calls");
        assertEquals(0, set.size());
    }
}
