package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.lang.reflect.Field;

class ConcurrentNavigableSetNullSafe_headSet_Test {

    private NavigableSet<String> set;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh set for each test and populate it with a known ordering
        set = new ConcurrentNavigableSetNullSafe<>();
        // Natural ordering: null (sentinel) < "apple" < "banana" < "cherry"
        set.add(null);
        set.add("apple");
        set.add("banana");
        set.add("cherry");
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference for GC; the set itself does not hold external resources
        set.clear();
    }

    @Test
    void headSetExclusiveExcludesToElement() {
        // toElement = "banana", inclusive = false
        NavigableSet<String> head = set.headSet("banana", false);
        // Expected order: null, "apple"
        List<String> expected = Arrays.asList(null, "apple");
        assertEquals(expected.size(), head.size(), "Head set size mismatch");
        assertIterableEquals(expected, head, "Head set contents mismatch (exclusive)");
    }

    @Test
    void headSetInclusiveIncludesToElement() {
        // toElement = "banana", inclusive = true
        NavigableSet<String> head = set.headSet("banana", true);
        // Expected order: null, "apple", "banana"
        List<String> expected = Arrays.asList(null, "apple", "banana");
        assertEquals(expected.size(), head.size(), "Head set size mismatch");
        assertIterableEquals(expected, head, "Head set contents mismatch (inclusive)");
    }

    @Test
    void headSetWithNullToElementInclusive() {
        // toElement = null, inclusive = true → only the null sentinel should be present
        NavigableSet<String> head = set.headSet(null, true);
        assertEquals(1, head.size(), "Head set of null (inclusive) should contain exactly one element");
        assertTrue(head.contains(null), "Head set should contain null");
    }

    @Test
    void headSetWithNullToElementExclusive() {
        // toElement = null, inclusive = false → should be empty because nothing is less than null
        NavigableSet<String> head = set.headSet(null, false);
        assertTrue(head.isEmpty(), "Head set of null (exclusive) should be empty");
    }

    @Test
    void headSetIsIndependentFromOriginalSet() {
        NavigableSet<String> head = set.headSet("banana", true);
        // Modify the head set
        // "date" is greater than "banana" in natural order, but should be allowed in the head view
        head.add("date");
        // The original set must remain unchanged
        assertFalse(set.contains("date"), "Original set must not be affected by modifications to the head set");
        // The head set should contain the newly added element
        assertTrue(head.contains("date"), "Head set should contain the element added to it");
    }

    @Test
    void headSetWithCustomComparator() {
        // Comparator based on string length (null is treated as length 0)
        Comparator<String> lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        NavigableSet<String> customSet = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
        customSet.add(null);
        // length 4
        customSet.add("kiwi");
        // length 5
        customSet.add("apple");
        // length 6
        customSet.add("banana");
        // toElement = "apple", inclusive = true → should contain null, "kiwi", "apple"
        NavigableSet<String> head = customSet.headSet("apple", true);
        List<String> expected = Arrays.asList(null, "kiwi", "apple");
        assertEquals(expected.size(), head.size(), "Custom comparator head set size mismatch");
        assertIterableEquals(expected, head, "Custom comparator head set contents mismatch");
    }

    @Test
    void headSetWithCustomComparatorExclusive() {
        // Comparator based on string length (null is treated as length 0)
        Comparator<String> lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        NavigableSet<String> customSet = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
        customSet.add(null);
        // length 4
        customSet.add("kiwi");
        // length 5
        customSet.add("apple");
        // length 6
        customSet.add("banana");
        // toElement = "apple", inclusive = false → should contain null, "kiwi"
        NavigableSet<String> head = customSet.headSet("apple", false);
        List<String> expected = Arrays.asList(null, "kiwi");
        assertEquals(expected.size(), head.size(), "Custom comparator exclusive head set size mismatch");
        assertIterableEquals(expected, head, "Custom comparator exclusive head set contents mismatch");
    }

    /**
     * Parameterized test for exclusive and inclusive headSet with a non‑null boundary.
     * Underlying set: [a, b, c, d]
     */
    @ParameterizedTest(name = "headSet({0}, inclusive={1}) → expected {2}")
    @CsvSource({ // toElement, inclusive, expected (comma‑separated list)
    "c,false,a b", "c,true,a b c", "a,false,", "a,true,a", "z,false,a b c d", "z,true,a b c d" })
    void headSetNonNullElements(String toElement, boolean inclusive, String expectedCsv) {
        NavigableSet<String> localSet = new ConcurrentNavigableSetNullSafe<>();
        localSet.add("a");
        localSet.add("b");
        localSet.add("c");
        localSet.add("d");
        NavigableSet<String> head = localSet.headSet(toElement, inclusive);
        List<String> expected = expectedCsv.isEmpty() ? Collections.emptyList() : Arrays.asList(expectedCsv.split(" "));
        assertEquals(expected.size(), head.size(), () -> "Size mismatch for headSet(" + toElement + ", " + inclusive + ")");
        assertIterableEquals(expected, head, () -> "Content mismatch for headSet(" + toElement + ", " + inclusive + ")");
    }

    @Test
    void headSetOnEmptyUnderlyingSet() {
        NavigableSet<String> emptySet = new ConcurrentNavigableSetNullSafe<>();
        NavigableSet<String> head = emptySet.headSet("any", false);
        assertTrue(head.isEmpty(), "Head set of an empty backing set must be empty");
    }

    @Test
    void headSetWithNullBoundaryWhenNullNotPresent() {
        // Create a set that does NOT contain the null sentinel
        NavigableSet<String> noNullSet = new ConcurrentNavigableSetNullSafe<>();
        noNullSet.add("a");
        noNullSet.add("b");
        // Exclusive: should be empty
        NavigableSet<String> headExcl = noNullSet.headSet(null, false);
        assertTrue(headExcl.isEmpty(), "Exclusive headSet(null) on a set without null should be empty");
        // Inclusive: also empty because sentinel is not stored
        NavigableSet<String> headIncl = noNullSet.headSet(null, true);
        assertTrue(headIncl.isEmpty(), "Inclusive headSet(null) on a set without null should be empty");
    }

    @Test
    void headSetWithReverseComparator() {
        Comparator<String> reverse = Collections.reverseOrder();
        NavigableSet<String> revSet = new ConcurrentNavigableSetNullSafe<>(reverse);
        revSet.add("a");
        revSet.add("b");
        revSet.add("c");
        // natural order: a<b<c<d ; reverse order: d<c<b<a
        revSet.add("d");
        // Exclusive up to "c" (in reverse order) → elements greater than "c" in natural order, i.e., "d"
        NavigableSet<String> headExcl = revSet.headSet("c", false);
        List<String> expectedExcl = Collections.singletonList("d");
        assertIterableEquals(expectedExcl, headExcl, "Reverse comparator exclusive headSet should contain only 'd'");
        // Inclusive up to "c" → "d" and "c"
        NavigableSet<String> headIncl = revSet.headSet("c", true);
        List<String> expectedIncl = Arrays.asList("d", "c");
        assertIterableEquals(expectedIncl, headIncl, "Reverse comparator inclusive headSet should contain 'd' and 'c'");
    }

    @Test
    void headSetWithIncompatibleElementTypeThrowsClassCastException() {
        NavigableSet<String> localSet = new ConcurrentNavigableSetNullSafe<>();
        localSet.add("a");
        localSet.add("b");
        // Use raw type to bypass compile‑time generics
        @SuppressWarnings("unchecked")
        NavigableSet rawSet = (NavigableSet) localSet;
        assertThrows(ClassCastException.class, () -> rawSet.headSet(42, false), "Passing an Integer to a String set should throw ClassCastException");
    }

    @Test
    void headSetViewIsMutableAndReflectsBackToOriginal() {
        NavigableSet<String> localSet = new ConcurrentNavigableSetNullSafe<>();
        localSet.add("a");
        localSet.add("b");
        localSet.add("c");
        localSet.add("d");
        NavigableSet<String> head = localSet.headSet("c", true);
        // Adding an element greater than the boundary should fail with ClassCastException
        assertThrows(ClassCastException.class, () -> head.add("x"), "Adding element beyond the headSet boundary must throw ClassCastException");
        // Removing an existing element should succeed and be reflected in the original set
        assertTrue(head.remove("b"), "Element 'b' should be removable from the head view");
        assertFalse(localSet.contains("b"), "Removal from head view must affect the original set");
        assertIterableEquals(Arrays.asList("a", "c"), head, "After removal, head view should contain only 'a' and 'c'");
    }

    @Test
    void headSetOnUnmodifiableBackingSetThrowsUnsupportedOperationException() throws Exception {
        // Build a normal mutable set first
        NavigableSet<String> mutable = new ConcurrentSkipListSet<>();
        mutable.addAll(Arrays.asList("a", "b", "c", "d"));
        // Wrap it as unmodifiable
        NavigableSet<String> unmodifiable = Collections.unmodifiableNavigableSet(mutable);
        // Create a ConcurrentNavigableSetNullSafe instance and inject the unmodifiable set via reflection
        ConcurrentNavigableSetNullSafe<String> wrapper = new ConcurrentNavigableSetNullSafe<>();
        Field internalField = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        internalField.setAccessible(true);
        internalField.set(wrapper, unmodifiable);
        // The view returned should forward mutating calls to the unmodifiable set
        NavigableSet<String> head = wrapper.headSet("c", false);
        assertThrows(UnsupportedOperationException.class, () -> head.add("x"), "Adding to a headSet backed by an unmodifiable set must throw UnsupportedOperationException");
    }

    @Test
    void headSetWithBadComparatorThrowsClassCastException() {
        // Comparator that blindly casts to String – it cannot handle the sentinel object used for null
        @SuppressWarnings("unchecked")
        Comparator<String> badComparator = (Comparator) (Comparator<Object>) (a, b) -> ((String) a).compareTo((String) b);
        ConcurrentNavigableSetNullSafe<String> badSet = new ConcurrentNavigableSetNullSafe<>(badComparator);
        badSet.add("a");
        badSet.add("b");
        badSet.add("c");
        // Adding null will store the sentinel; the sentinel is not a String, so any comparison will fail
        badSet.add(null);
        assertThrows(ClassCastException.class, () -> badSet.headSet(null, false), "headSet with a comparator that cannot compare the sentinel should throw ClassCastException");
        assertThrows(ClassCastException.class, () -> badSet.headSet(null, true), "Inclusive headSet with a bad comparator should also throw ClassCastException");
    }
}
