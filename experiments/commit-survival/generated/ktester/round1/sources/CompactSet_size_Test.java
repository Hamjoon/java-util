package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class CompactSet_size_Test {

    /**
     * A CompactSet with a tiny {@code compactSize()} to make state‑change testing cheap.
     */
    private static class TinyCompactSet<E> extends CompactSet<E> {

        @Override
        protected int compactSize() {
            // transition after 3 elements
            return 3;
        }
        // No need to override getNewSet – the default HashSet is fine.
    }

    /**
     * Directly sets the private {@code val} field of a CompactSet instance.
     *
     * @param set   the CompactSet whose internal state is to be altered
     * @param value the new value for the {@code val} field (Object[], Set, etc.)
     */
    private static void setInternalVal(CompactSet<?> set, Object value) throws Exception {
        Field f = CompactSet.class.getDeclaredField("val");
        f.setAccessible(true);
        f.set(set, value);
    }

    /**
     * Retrieves the private static sentinel {@code EMPTY_SET} used by CompactSet.
     *
     * @return the sentinel object
     */
    private static Object getEmptySetSentinel() throws Exception {
        Field f = CompactSet.class.getDeclaredField("EMPTY_SET");
        f.setAccessible(true);
        return f.get(null);
    }

    @Test
    void sizeReturnsZeroWhenEmpty() {
        CompactSet<Integer> set = new CompactSet<>();
        assertEquals(0, set.size(), "A newly constructed CompactSet should be empty");
        assertTrue(set.isEmpty(), "isEmpty() should agree with size() == 0");
    }

    @Test
    void sizeReflectsNumberOfElementsWhileUsingObjectArray() {
        // With compactSize() = 3, adding 2 elements stays in the Object[] representation.
        CompactSet<String> set = new TinyCompactSet<>();
        assertTrue(set.add("a"));
        assertTrue(set.add("b"));
        assertEquals(2, set.size(), "size() should report the number of added elements");
        assertFalse(set.isEmpty(), "set should not be reported as empty after additions");
    }

    @Test
    void sizeWorksAfterTransitionToSetRepresentation() {
        // Adding 4 elements forces a transition to the Set representation (compactSize() = 3).
        CompactSet<Integer> set = new TinyCompactSet<>();
        assertTrue(set.add(1));
        assertTrue(set.add(2));
        assertTrue(set.add(3));
        // At this point we are still in the Object[] branch (size == 3).
        assertEquals(3, set.size());
        // Adding the fourth element triggers the switch to a Set.
        assertTrue(set.add(4));
        assertEquals(4, set.size(), "size() must reflect all elements after transition to Set");
        // Verify that further operations still behave correctly.
        assertTrue(set.contains(2), "contains() should work after transition");
        assertTrue(set.remove(3), "remove() should work after transition");
        assertEquals(3, set.size(), "size() should be updated after a removal");
    }

    @Test
    void sizeHandlesNullElement() {
        CompactSet<String> set = new TinyCompactSet<>();
        assertTrue(set.add(null), "null should be allowed as an element");
        assertEquals(1, set.size(), "size() should count the null element");
        assertTrue(set.contains(null), "contains(null) must be true after adding null");
    }

    @Test
    void sizeWorksWhenConstructedFromCollection() {
        Set<String> source = new HashSet<>();
        source.add("x");
        source.add("y");
        source.add("z");
        CompactSet<String> set = new CompactSet<>(source);
        assertEquals(source.size(), set.size(), "size() must match the size of the source collection");
        assertTrue(set.containsAll(source), "All elements from the source collection must be present");
    }

    @Test
    void sizeReturnsZeroAfterClear() {
        CompactSet<Integer> set = new TinyCompactSet<>();
        set.add(10);
        set.add(20);
        assertEquals(2, set.size(), "Pre‑condition: size should be 2 before clear");
        set.clear();
        assertEquals(0, set.size(), "size() must be 0 after clear()");
        assertTrue(set.isEmpty(), "isEmpty() must be true after clear()");
    }

    @Test
    void sizeWithObjectArrayRepresentationViaReflection() throws Exception {
        // Force the internal representation to an Object[] of length 3.
        CompactSet<String> set = new CompactSet<>();
        Object[] forcedArray = new Object[] { "a", "b", "c" };
        setInternalVal(set, forcedArray);
        assertEquals(3, set.size(), "When val is an Object[] the size() should return the array length");
    }

    @Test
    void sizeWithSetRepresentationViaReflection() throws Exception {
        // Force the internal representation to a Set containing four elements.
        CompactSet<String> set = new CompactSet<>();
        Set<String> forcedSet = new HashSet<>();
        forcedSet.add("one");
        forcedSet.add("two");
        forcedSet.add("three");
        forcedSet.add("four");
        setInternalVal(set, forcedSet);
        assertEquals(4, set.size(), "When val is a Set the size() should return the Set's size");
    }

    @Test
    void sizeAfterClearResetsToEmptySentinelViaReflection() throws Exception {
        // Start with a non‑empty representation, then invoke clear() and verify the sentinel.
        CompactSet<String> set = new CompactSet<>();
        set.add("x");
        set.add("y");
        assertEquals(2, set.size());
        set.clear();
        // Verify that the internal field has been reset to the EMPTY_SET sentinel.
        Field f = CompactSet.class.getDeclaredField("val");
        f.setAccessible(true);
        Object internalVal = f.get(set);
        Object emptySentinel = getEmptySetSentinel();
        assertSame(emptySentinel, internalVal, "After clear() the internal val should be reset to EMPTY_SET");
        assertEquals(0, set.size(), "size() must be 0 after clear()");
    }
}
