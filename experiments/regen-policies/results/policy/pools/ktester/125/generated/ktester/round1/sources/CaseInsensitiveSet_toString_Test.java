package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveSet_toString_Test {

    class Custom {

        @Override
        public String toString() {
            return "custom";
        }
    }

    class Custom {

        @Override
        public String toString() {
            return "custom";
        }
    }

    /**
     * Verify that {@code toString()} returns the string representation of the
     * underlying map's key set. The default constructor uses a {@link CaseInsensitiveMap}
     * with a {@link java.util.LinkedHashMap} backing, preserving insertion order.
     */
    @Test
    void toString_DefaultConstructor_InsertionOrderPreserved() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        set.add("Apple");
        set.add("banana");
        set.add("CHERRY");
        // The expected order is the order of insertion, and the original case is preserved.
        String expected = "[Apple, banana, CHERRY]";
        assertEquals(expected, set.toString(), "toString() should reflect the underlying keySet order and original case");
    }

    /**
     * Adding the same string with different case must not create a new entry,
     * and the original case should stay unchanged in the string representation.
     */
    @Test
    void toString_DuplicateCaseInsensitiveElements_OriginalCaseKept() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        set.add("Hello");
        // duplicate, should be ignored
        set.add("HELLO");
        assertEquals("[Hello]", set.toString(), "Duplicate case‑insensitive entry must not appear in toString()");
    }

    /**
     * Verify that a heterogeneous set (String and non‑String elements) displays
     * all elements correctly. Non‑String elements are handled by the backing map
     * without case‑insensitive logic.
     */
    @Test
    void toString_MixedTypes_AllElementsDisplayed() {
        CaseInsensitiveSet<Object> set = new CaseInsensitiveSet<>();
        set.add("One");
        set.add(42);
        set.add("TWO");
        set.add(3.14);
        // Order of insertion is preserved.
        String expected = "[One, 42, TWO, 3.14]";
        assertEquals(expected, set.toString(), "toString() must include both String and non‑String elements");
    }

    /**
     * Use the constructor that accepts an initial capacity and verify that the
     * string representation is still correct. The capacity does not affect behaviour,
     * but we test that the constructor does not break {@code toString()}.
     */
    @Test
    void toString_WithInitialCapacity_ProducesCorrectOutput() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(10);
        set.addAll(Arrays.asList("alpha", "Beta", "GAMMA"));
        // Original case must be kept, order is insertion order.
        String expected = "[alpha, Beta, GAMMA]";
        assertEquals(expected, set.toString());
    }

    /**
     * Verify that the constructor which receives a backing map works as expected.
     * Here we supply a LinkedHashMap to preserve order and then check the output.
     */
    @Test
    void toString_WithExplicitBackingMap_OrderAndCasePreserved() {
        Map<String, Object> backing = new CaseInsensitiveMap<>(new LinkedHashMap<>());
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(Collections.emptySet(), backing);
        set.addAll(Arrays.asList("x", "Y", "z"));
        // LinkedHashMap preserves insertion order, so we expect that order.
        String expected = "[x, Y, z]";
        assertEquals(expected, set.toString());
    }

    /**
     * Verify that an empty set returns the standard empty collection string.
     */
    @Test
    void toString_EmptySet_ReturnsEmptyBrackets() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        assertEquals("[]", set.toString(), "Empty set should be represented as []");
    }

    /**
     * Verify that a set built from a {@link SortedSet} (TreeMap backing) produces a
     * sorted string representation, because the underlying map is a TreeMap.
     */
    @Test
    void toString_FromSortedSet_IsSorted() {
        SortedSet<String> source = new TreeSet<>(Arrays.asList("delta", "Alpha", "charlie"));
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(source);
        // TreeMap orders keys according to natural ordering (case‑insensitive due to the map)
        // The natural order of the lower‑cased strings is: "Alpha", "charlie", "delta"
        // Original case of the first occurrence is kept.
        String expected = "[Alpha, charlie, delta]";
        assertEquals(expected, set.toString(), "Set constructed from SortedSet should be sorted in the string representation");
    }

    /**
     * Verify that a set containing three distinct strings is rendered in the
     * order of insertion (the default backing map preserves insertion order).
     */
    @Test
    void toString_NonEmptySet_ThreeDistinctStrings() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        set.add("a");
        set.add("b");
        set.add("c");
        assertEquals("[a, b, c]", set.toString(), "The string representation should list elements in insertion order");
    }

    /**
     * Adding the same string with a different case must not create a new entry.
     * The first inserted case is kept in the output.
     */
    @Test
    void toString_NonEmptySet_CaseInsensitiveDuplicateKeepsFirstCase() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        set.add("A");
        // duplicate, ignored
        set.add("a");
        assertEquals("[A]", set.toString(), "Duplicate case‑insensitive entry should keep the first inserted case");
    }

    /**
     * {@code null} is a legal element for this set. Its {@code toString()}
     * representation should be the literal string {@code "null"}.
     */
    @Test
    void toString_NonEmptySet_NullElement() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        set.add(null);
        assertEquals("[null]", set.toString(), "Null element must be rendered as \"null\" inside the brackets");
    }

    /**
     * The set works with non‑String objects; their own {@code toString()}
     * implementations are used.
     */
    @Test
    void toString_NonEmptySet_IntegerElements() {
        CaseInsensitiveSet<Integer> set = new CaseInsensitiveSet<>();
        set.add(1);
        set.add(2);
        set.add(3);
        assertEquals("[1, 2, 3]", set.toString(), "Integer elements should be rendered using their standard toString()");
    }

    /**
     * Custom objects are displayed via their {@code toString()} method.
     */
    @Test
    void toString_NonEmptySet_CustomObject() {
        class Custom {

            @Override
            public String toString() {
                return "custom";
            }
        }
        CaseInsensitiveSet<Object> set = new CaseInsensitiveSet<>();
        set.add(new Custom());
        assertEquals("[custom]", set.toString(), "Custom object's toString() should be used in the set's representation");
    }

    /**
     * Simulate a corrupted instance where the internal {@code map} field is
     * {@code null}. The implementation directly accesses {@code map.keySet()},
     * therefore a {@link NullPointerException} is expected.
     */
    @Test
    void toString_NullInternalMap_ThrowsNullPointerException() throws Exception {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        // Force the private field 'map' to null using reflection
        Field mapField = CaseInsensitiveSet.class.getDeclaredField("map");
        mapField.setAccessible(true);
        mapField.set(set, null);
        assertThrows(NullPointerException.class, set::toString, "Calling toString() when internal map is null should throw NPE");
    }

    /**
     * Verify that a fail‑fast iterator throws {@link ConcurrentModificationException}
     * when the backing map is modified while {@code toString()} is iterating.
     *
     * <p>We use a {@link TreeMap} as the backing map because its iterators are fail‑fast.
     * The set is filled with many elements to make the iteration take a measurable amount
     * of time, allowing a second thread to modify the set during the iteration.</p>
     */
    @Test
    void toString_ConcurrentModificationDuringToString_ThrowsCME() throws Exception {
        // Build a set with a TreeMap backing (insertion order is not important here)
        Map<String, Object> backing = new TreeMap<>();
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(Collections.emptySet(), backing);
        // Populate with many entries to lengthen the iteration
        for (int i = 0; i < 10_000; i++) {
            set.add("elem" + i);
        }
        // Latch to make sure the modifier thread runs while toString() is iterating
        CountDownLatch startIterating = new CountDownLatch(1);
        CountDownLatch modificationDone = new CountDownLatch(1);
        Thread modifier = new Thread(() -> {
            try {
                // Wait a tiny moment so that the main thread has started iterating
                startIterating.await(5, TimeUnit.SECONDS);
                // Perform a structural change
                set.add("newElement");
                modificationDone.countDown();
            } catch (InterruptedException ignored) {
            }
        });
        modifier.start();
        // Wrap the call to toString() so we can signal when iteration begins.
        // The TreeMap's iterator is obtained inside the toString() implementation;
        // we cannot hook directly, but we can rely on the time taken for a large set.
        // The modifier thread will fire after the latch is released.
        String result;
        try {
            // Signal the modifier thread that we are about to start iterating
            startIterating.countDown();
            // The following call *should* throw CME because the set is modified concurrently.
            result = set.toString();
            // If no exception is thrown, fail the test.
            fail("Expected ConcurrentModificationException was not thrown. Result was: " + result);
        } catch (ConcurrentModificationException expected) {
            // Expected path – test passes.
        } finally {
            // Ensure the modifier thread finishes before the test ends.
            modificationDone.await(5, TimeUnit.SECONDS);
            modifier.join();
        }
    }
}
