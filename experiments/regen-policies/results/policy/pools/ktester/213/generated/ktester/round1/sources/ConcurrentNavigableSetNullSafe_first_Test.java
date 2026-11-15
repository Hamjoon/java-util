package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.NavigableSet;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentSkipListSet;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableSetNullSafe_first_Test {

    private NavigableSet<String> set;

    @BeforeEach
    void setupBeforeEach() {
        // Use the default constructor for most tests; individual tests can replace it.
        set = new ConcurrentNavigableSetNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the set to avoid cross‑test contamination (good practice even though a new set is created each time).
        set.clear();
    }

    @Test
    void firstOnEmptySetThrowsException() {
        assertTrue(set.isEmpty(), "Set should be empty before invoking first()");
        assertThrows(NoSuchElementException.class, () -> set.first(), "Calling first() on an empty ConcurrentNavigableSetNullSafe must throw NoSuchElementException");
    }

    @Test
    void firstReturnsNullWhenNullIsPresent() {
        assertTrue(set.add("apple"));
        assertTrue(set.add(null));
        assertTrue(set.add("banana"));
        // According to the null‑safe contract, null is ordered before any non‑null element.
        assertNull(set.first(), "first() should return null when null is present in the set");
    }

    @Test
    void firstReturnsLowestElementWhenNullIsAbsent() {
        assertTrue(set.add("banana"));
        assertTrue(set.add("apple"));
        assertTrue(set.add("cherry"));
        // Natural ordering: "apple" < "banana" < "cherry"
        assertEquals("apple", set.first(), "first() should return the smallest element according to natural ordering");
    }

    @Test
    void firstRespectsCustomComparator() {
        Comparator<String> lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        // Use the constructor that accepts a Comparator (exists in the real class).
        set = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
        // length 4
        assertTrue(set.add("kiwi"));
        // length 5
        assertTrue(set.add("apple"));
        // length 0 (treated as smallest)
        assertTrue(set.add(null));
        // length 6
        assertTrue(set.add("banana"));
        // With the length comparator, null (length 0) is the smallest.
        assertNull(set.first(), "first() should return null when the comparator treats null as the smallest element");
    }

    @Test
    void firstAfterRemovingCurrentFirstElement() {
        assertTrue(set.add(null));
        assertTrue(set.add("apple"));
        assertTrue(set.add("banana"));
        // Initial first is null
        assertNull(set.first());
        // Remove null; the next smallest element should become the first.
        assertTrue(set.remove(null));
        assertEquals("apple", set.first(), "After removing null, first() should return the next smallest element");
    }

    @Test
    void firstWithAllElementsRemovedThrowsException() {
        assertTrue(set.add("apple"));
        assertTrue(set.add("banana"));
        // Remove both elements
        assertTrue(set.remove("apple"));
        assertTrue(set.remove("banana"));
        assertTrue(set.isEmpty());
        assertThrows(NoSuchElementException.class, () -> set.first(), "first() must throw NoSuchElementException after all elements have been removed");
    }

    @Test
    void firstWhenInternalSetIsNullThrowsNPE() throws Exception {
        // Create a normal instance then force its internalSet field to null via reflection.
        ConcurrentNavigableSetNullSafe<String> unsafeSet = new ConcurrentNavigableSetNullSafe<>();
        Field internalSetField = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        internalSetField.setAccessible(true);
        internalSetField.set(unsafeSet, null);
        // The call should now dereference a null field and throw NPE.
        assertThrows(NullPointerException.class, unsafeSet::first, "first() should throw NullPointerException when internalSet is null");
    }

    @Test
    void firstWithIncomparableElementsThrowsClassCastException() throws Exception {
        // Use the raw constructor to get a set that uses natural ordering.
        ConcurrentNavigableSetNullSafe<Object> rawSet = new ConcurrentNavigableSetNullSafe<>();
        // Insert heterogeneous, non‑comparable elements directly into the internal set.
        Field internalSetField = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        internalSetField.setAccessible(true);
        @SuppressWarnings("unchecked")
        NavigableSet<Object> internal = (NavigableSet<Object>) internalSetField.get(rawSet);
        internal.add("string");
        // Integer – not comparable with String under natural ordering.
        internal.add(42);
        // The first() call triggers the internal comparison and should throw ClassCastException.
        assertThrows(ClassCastException.class, rawSet::first, "first() should propagate ClassCastException when elements are not mutually comparable");
    }

    @Test
    void firstWithTypeMismatchBetweenGenericAndStoredElementThrowsClassCastException() throws Exception {
        // Create a set typed as String but manipulate its internal raw set to store an Integer.
        ConcurrentNavigableSetNullSafe<String> typedSet = new ConcurrentNavigableSetNullSafe<>();
        // Bypass generic safety via reflection.
        Field internalSetField = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        internalSetField.setAccessible(true);
        @SuppressWarnings("unchecked")
        NavigableSet<Object> internal = (NavigableSet<Object>) internalSetField.get(typedSet);
        // Integer stored where String is expected.
        internal.add(123);
        // The first() call will retrieve the Integer and then unmaskNull will attempt to cast it to String.
        assertThrows(ClassCastException.class, typedSet::first, "first() should throw ClassCastException when the stored element type does not match the generic type");
    }

    @Test
    void firstReturnsSingletonElement() {
        assertTrue(set.add("singleton"));
        assertEquals("singleton", set.first(), "A set containing exactly one non‑null element should return that element as first()");
    }
}
