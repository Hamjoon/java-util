package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableSetNullSafe_last_Test {

    private NavigableSet<String> set;

    private NavigableSet<String> stringSet;

    private NavigableSet<Integer> intSet;

    // used for the incomparable‑elements test
    private NavigableSet<Object> rawSet;

    @BeforeEach
    void setupBeforeEach() {
        // default (natural‑ordering) constructors
        stringSet = new ConcurrentNavigableSetNullSafe<>();
        intSet = new ConcurrentNavigableSetNullSafe<>();
        rawSet = new ConcurrentNavigableSetNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        stringSet.clear();
        intSet.clear();
        rawSet.clear();
        stringSet = null;
        intSet = null;
        rawSet = null;
    }

    @Test
    void lastReturnsHighestElementWithNaturalOrder() {
        // Arrange
        stringSet.add("apple");
        stringSet.add("banana");
        // null is masked internally and should be the highest.
        stringSet.add(null);
        stringSet.add("cherry");
        // Act
        String result = stringSet.last();
        // Assert
        assertNull(result, "With natural ordering the masked null element should be the last element.");
    }

    @Test
    void lastReturnsHighestElementWhenNullIsAbsent() {
        // Arrange
        stringSet.add("apple");
        stringSet.add("banana");
        stringSet.add("cherry");
        // Act
        String result = stringSet.last();
        // Assert
        assertEquals("cherry", result, "The lexicographically greatest element should be returned.");
    }

    @Test
    void lastReturnsHighestElementWithCustomComparator() {
        // Arrange – comparator based on string length; null is treated as length 0 and therefore lowest.
        Comparator<String> lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        NavigableSet<String> lengthSet = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
        // length 4
        lengthSet.add("kiwi");
        // length 5
        lengthSet.add("apple");
        // length 6
        lengthSet.add("banana");
        // length 0 (lowest)
        lengthSet.add(null);
        // Act
        String result = lengthSet.last();
        // Assert
        assertEquals("banana", result, "The element with the greatest length should be returned.");
    }

    @Test
    void lastThrowsWhenSetIsEmpty() {
        // Act & Assert
        assertThrows(NoSuchElementException.class, () -> stringSet.last(), "Calling last() on an empty set must throw NoSuchElementException.");
    }

    @Test
    void lastReflectsRemovalOfNullElement() {
        // Arrange
        stringSet.add("apple");
        stringSet.add("banana");
        stringSet.add(null);
        stringSet.add("cherry");
        // Remove the null element (masked sentinel)
        assertTrue(stringSet.remove(null), "Null element should be removable.");
        // Act
        String result = stringSet.last();
        // Assert – now the highest natural‑order element is "cherry".
        assertEquals("cherry", result, "After removing null, the last element should be the highest non‑null value.");
    }

    @Test
    void lastReturnsGreatestIntegerInNaturalOrder() {
        // Non‑empty set with natural ordering: 1, 2, 3 → expect 3
        intSet.add(1);
        intSet.add(2);
        intSet.add(3);
        assertEquals(3, intSet.last(), "The greatest integer should be returned.");
    }

    @Test
    void lastReturnsNullWhenOnlyNullIsPresent() {
        // Set containing only a null element → expect null
        intSet.add(null);
        assertNull(intSet.last(), "A set that only contains null should return null.");
    }

    @Test
    void lastPropagatesNoSuchElementExceptionWhenEmpty() {
        // Empty set → expect NoSuchElementException
        assertThrows(NoSuchElementException.class, () -> intSet.last(), "Calling last() on an empty set must propagate NoSuchElementException.");
    }

    @Test
    void lastRespectsReverseAlphabeticalComparator() {
        // Comparator: reverse alphabetical order → last() should be the smallest alphabetically ("a")
        Comparator<String> reverse = Comparator.reverseOrder();
        NavigableSet<String> reverseSet = new ConcurrentNavigableSetNullSafe<>(reverse);
        reverseSet.add("a");
        reverseSet.add("b");
        reverseSet.add("c");
        assertEquals("a", reverseSet.last(), "With reverse order comparator, the logical last element should be \"a\".");
    }

    @Test
    void lastReturnsGreatestWithMixedNullAndNumbers() {
        // Mixed null and non‑null (natural ordering): null, 5, 10 → expect 10
        intSet.add(null);
        intSet.add(5);
        intSet.add(10);
        assertEquals(10, intSet.last(), "The greatest non‑null integer should be returned when null is present.");
    }

    @Test
    void lastReflectsRemovalOfGreatestElement() {
        // Add 1,2,3 → last() = 3, remove 3 → last() = 2
        intSet.add(1);
        intSet.add(2);
        intSet.add(3);
        assertEquals(3, intSet.last(), "Initial last() should be 3.");
        assertTrue(intSet.remove(3), "Removal of the greatest element should succeed.");
        assertEquals(2, intSet.last(), "After removal, last() should be 2.");
    }

    @Test
    void lastIgnoresDuplicateElements() {
        // Add 1,2,2,3 → duplicate 2 ignored → last() = 3
        intSet.add(1);
        intSet.add(2);
        // duplicate
        intSet.add(2);
        intSet.add(3);
        assertEquals(3, intSet.last(), "Duplicates must be ignored; the greatest element should still be 3.");
    }

    @Test
    void lastUsesAbsoluteValueComparator() {
        // Comparator that orders by absolute value → expect -5 as greatest (|‑5|=5 > 4)
        Comparator<Integer> absComparator = (a, b) -> Integer.compare(Math.abs(a), Math.abs(b));
        NavigableSet<Integer> absSet = new ConcurrentNavigableSetNullSafe<>(absComparator);
        absSet.add(-5);
        absSet.add(3);
        absSet.add(4);
        assertEquals(-5, absSet.last(), "With absolute‑value comparator, -5 should be considered the greatest.");
    }

    @Test
    void lastReturnsSingleElement() {
        // Single element set → expect that element
        intSet.add(42);
        assertEquals(42, intSet.last(), "A set with a single element must return that element.");
    }

    @Test
    void lastTreatsMaskedNullAsGreatestViaComparator() {
        // Comparator that treats any non‑String (i.e., the sentinel) as greater than normal strings
        Comparator<String> sentinelGreatest = (o1, o2) -> {
            boolean o1Sentinel = !(o1 instanceof String);
            boolean o2Sentinel = !(o2 instanceof String);
            if (o1Sentinel && o2Sentinel)
                return 0;
            // sentinel > any real string
            if (o1Sentinel)
                return 1;
            if (o2Sentinel)
                return -1;
            return o1.compareTo(o2);
        };
        NavigableSet<String> set = new ConcurrentNavigableSetNullSafe<>(sentinelGreatest);
        // becomes sentinel internally
        set.add(null);
        set.add("a");
        assertNull(set.last(), "When the comparator treats the masked‑null sentinel as greatest, last() must return null.");
    }

    @Test
    void lastPropagatesClassCastExceptionForIncomparableElements() {
        // Raw type set with natural ordering, add Integer and String → expect ClassCastException on last()
        @SuppressWarnings("unchecked")
        ConcurrentNavigableSetNullSafe rawSet = new ConcurrentNavigableSetNullSafe();
        rawSet.add(1);
        rawSet.add("a");
        assertThrows(ClassCastException.class, rawSet::last, "Mixing incomparable types should cause ClassCastException.");
    }

    @Test
    void lastPropagatesNullPointerExceptionFromComparator() {
        // Comparator that throws NPE when it sees the sentinel (non‑String)
        Comparator<String> npeComparator = (o1, o2) -> {
            if (!(o1 instanceof String) || !(o2 instanceof String)) {
                throw new NullPointerException("Comparator received sentinel");
            }
            return o1.compareTo(o2);
        };
        NavigableSet<String> set = new ConcurrentNavigableSetNullSafe<>(npeComparator);
        // sentinel
        set.add(null);
        set.add("x");
        assertThrows(NullPointerException.class, set::last, "Comparator throwing NPE must propagate through last().");
    }

    @Test
    void lastPropagatesIllegalArgumentExceptionFromComparator() {
        // Comparator that throws IllegalArgumentException when first argument equals 42
        Comparator<Integer> illegalArgComparator = (a, b) -> {
            if (a != null && a == 42) {
                throw new IllegalArgumentException("42 is not allowed");
            }
            return Integer.compare(a, b);
        };
        NavigableSet<Integer> set = new ConcurrentNavigableSetNullSafe<>(illegalArgComparator);
        set.add(42);
        set.add(1);
        assertThrows(IllegalArgumentException.class, set::last, "Comparator throwing IllegalArgumentException must propagate through last().");
    }
}
