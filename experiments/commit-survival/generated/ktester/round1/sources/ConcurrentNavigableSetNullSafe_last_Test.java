package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentSkipListSet;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableSetNullSafe_last_Test {

    /**
     * Set used in the tests – created fresh for each test method.
     */
    private NavigableSet<String> set;

    @BeforeEach
    void setupBeforeEach() {
        // Use the default (natural‑ordering) constructor for most tests.
        set = new ConcurrentNavigableSetNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the set is cleared so that a stray element cannot affect another test.
        set.clear();
    }

    @Test
    void lastReturnsHighestElementWithNaturalOrdering() {
        // Elements are ordered according to their natural String ordering.
        set.add("apple");
        set.add("banana");
        set.add("cherry");
        // null is treated as the greatest element
        set.add(null);
        assertEquals(null, set.last(), "With natural ordering null should be the last element");
    }

    @Test
    void lastReturnsHighestElementWithCustomComparator() {
        // Comparator that orders strings by length; null is considered length 0.
        Comparator<String> lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        set = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
        // length 4
        set.add("kiwi");
        // length 5
        set.add("apple");
        // length 6
        set.add("banana");
        // length 0 (smallest)
        set.add(null);
        assertEquals("banana", set.last(), "Longest string should be returned as last");
    }

    @Test
    void lastReflectsChangesAfterRemovals() {
        set.add("alpha");
        set.add("beta");
        set.add("gamma");
        set.add(null);
        // Initial last is null.
        assertEquals(null, set.last());
        // Remove the null element – now the greatest element is "gamma".
        assertTrue(set.remove(null));
        assertEquals("gamma", set.last());
        // Remove "gamma" – now "beta" becomes the greatest.
        assertTrue(set.remove("gamma"));
        assertEquals("beta", set.last());
    }

    @Test
    void lastOnEmptySetThrowsNoSuchElementException() {
        assertTrue(set.isEmpty(), "Set should be empty at the start of the test");
        assertThrows(NoSuchElementException.class, () -> set.last(), "Calling last() on an empty NavigableSet must throw NoSuchElementException");
    }

    @Test
    void lastWorksWhenSetCreatedFromCollection() {
        Collection<String> coll = Arrays.asList("delta", "epsilon", null, "zeta");
        set = new ConcurrentNavigableSetNullSafe<>(coll);
        // With natural ordering null is the greatest element.
        assertEquals(null, set.last());
    }

    /**
     * Typical non‑null elements using {@link UUID} as the element type.
     * The greatest UUID according to its natural ordering should be returned.
     */
    @Test
    void lastReturnsGreatestUuidWithNaturalOrdering() {
        NavigableSet<UUID> uuidSet = new ConcurrentNavigableSetNullSafe<>();
        UUID uuid1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID uuid2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID uuid3 = UUID.fromString("00000000-0000-0000-0000-000000000003");
        uuidSet.add(uuid1);
        uuidSet.add(uuid2);
        uuidSet.add(uuid3);
        assertEquals(uuid3, uuidSet.last(), "The highest UUID (according to natural ordering) should be returned");
    }

    /**
     * Set contains only a {@code null} element.
     * The internal sentinel is unmasked and {@code null} is returned.
     */
    @Test
    void lastReturnsNullWhenOnlyNullElementIsPresent() {
        set.add(null);
        assertNull(set.last(), "When the set contains only null, last() must return null");
    }

    /**
     * Set contains a single non‑null element.
     */
    @Test
    void lastReturnsTheOnlyElementWhenSetHasOneItem() {
        String sole = "singleton-value";
        set.add(sole);
        assertEquals(sole, set.last(), "With a single element the same element must be returned");
    }

    /**
     * The underlying {@code internalSet} contains an element of an incompatible type.
     * {@code unmaskNull} casts the masked element to {@code E}, causing a {@link ClassCastException}.
     */
    @Test
    void lastPropagatesClassCastExceptionWhenInternalElementHasWrongType() throws Exception {
        // Create a correctly typed wrapper.
        ConcurrentNavigableSetNullSafe<String> unsafeSet = new ConcurrentNavigableSetNullSafe<>();
        // Bypass the public API and inject an Integer directly into the internal set.
        Field internalField = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        internalField.setAccessible(true);
        @SuppressWarnings("unchecked")
        NavigableSet<Object> internal = (NavigableSet<Object>) internalField.get(unsafeSet);
        // Integer is not a String
        internal.add(42);
        // The cast inside unmaskNull should now fail.
        assertThrows(ClassCastException.class, unsafeSet::last, "A ClassCastException should be thrown when the stored element type does not match the generic type");
    }

    /**
     * The wrapper is instantiated with a {@code null} {@code internalSet} reference
     * (simulated via reflection). Calling {@code last()} should result in a {@link NullPointerException}.
     */
    @Test
    void lastThrowsNullPointerExceptionWhenInternalSetIsNull() throws Exception {
        // Normal construction first.
        ConcurrentNavigableSetNullSafe<String> brokenSet = new ConcurrentNavigableSetNullSafe<>();
        // Corrupt the internal state using reflection.
        Field internalField = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        internalField.setAccessible(true);
        internalField.set(brokenSet, null);
        // The call should now NPE.
        assertThrows(NullPointerException.class, brokenSet::last, "If internalSet is null, last() must throw NullPointerException");
    }
}
