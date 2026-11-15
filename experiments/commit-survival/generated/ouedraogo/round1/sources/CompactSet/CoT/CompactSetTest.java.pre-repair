package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import java.lang.reflect.Field;
import java.util.*;

import static org.junit.Assert.*;

public class CompactSetTest {

    /** Small‑threshold subclass used only in tests */
    private static class SmallCompactSet<E> extends CompactSet<E> {
        @Override
        protected int compactSize() { return 3; }   // tiny threshold for fast switching
    }

    /* -------------------------------------------------
       BASIC TESTS – one for each public method
       ------------------------------------------------- */

    @Test
    public void testEmptyConstructor() {
        CompactSet<Integer> set = new CompactSet<>();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    @Test
    public void testCollectionConstructor() {
        List<Integer> src = Arrays.asList(1, 2, 3);
        CompactSet<Integer> set = new CompactSet<>(src);
        assertEquals(3, set.size());
        assertTrue(set.containsAll(src));
    }

    @Test
    public void testAddAndSize() {
        CompactSet<String> set = new CompactSet<>();
        assertTrue(set.add("a"));
        assertEquals(1, set.size());
        assertFalse(set.add("a"));            // duplicate
        assertEquals(1, set.size());
        assertTrue(set.add("b"));
        assertEquals(2, set.size());
    }

    @Test
    public void testAddNull() {
        CompactSet<String> set = new CompactSet<>();
        assertTrue(set.add(null));
        assertTrue(set.contains(null));
        assertEquals(1, set.size());
    }

    @Test
    public void testContains() {
        CompactSet<Integer> set = new CompactSet<>();
        set.add(10);
        assertTrue(set.contains(10));
        assertFalse(set.contains(99));
    }

    @Test
    public void testIteratorBasic() {
        CompactSet<Integer> set = new CompactSet<>(Arrays.asList(1, 2, 3));
        Iterator<Integer> it = set.iterator();
        Set<Integer> seen = new HashSet<>();
        while (it.hasNext()) {
            seen.add(it.next());
        }
        assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), seen);
    }

    @Test(expected = IllegalStateException.class)
    public void testIteratorRemoveBeforeNext() {
        CompactSet<Integer> set = new CompactSet<>(Arrays.asList(1));
        Iterator<Integer> it = set.iterator();
        it.remove();          // should throw IllegalStateException
    }

    @Test
    public void testIteratorRemoveAfterNext() {
        CompactSet<Integer> set = new CompactSet<>(Arrays.asList(1, 2, 3));
        Iterator<Integer> it = set.iterator();
        Integer first = it.next();
        it.remove();          // remove the element returned by next()
        assertFalse(set.contains(first));
        assertEquals(2, set.size());
    }

    @Test
    public void testRemoveExisting() {
        CompactSet<String> set = new CompactSet<>();
        set.add("x");
        assertTrue(set.remove("x"));
        assertFalse(set.contains("x"));
        assertEquals(0, set.size());
    }

    @Test
    public void testRemoveNonExisting() {
        CompactSet<String> set = new CompactSet<>();
        set.add("y");
        assertFalse(set.remove("z"));
        assertEquals(1, set.size());
    }

    @Test
    public void testClear() {
        CompactSet<Integer> set = new CompactSet<>(Arrays.asList(1, 2, 3));
        set.clear();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    /* -------------------------------------------------
       EDGE‑CASE / EXCEPTION TESTS
       ------------------------------------------------- */

    @Test
    public void testSwitchToSetWhenThresholdExceeded() throws Exception {
        SmallCompactSet<Integer> set = new SmallCompactSet<>();
        // add exactly compactSize elements – still an Object[]
        set.add(1);
        set.add(2);
        set.add(3);
        Field valField = CompactSet.class.getDeclaredField("val");
        valField.setAccessible(true);
        assertTrue(valField.get(set) instanceof Object[]);

        // add one more – should switch to a Set
        set.add(4);
        Object internal = valField.get(set);
        assertTrue(internal instanceof Set);
        assertEquals(4, set.size());
    }

    @Test
    public void testSwitchBackToArrayWhenSizeDropsToCompactSize() throws Exception {
        SmallCompactSet<Integer> set = new SmallCompactSet<>();
        // bring the set above the threshold so it uses a Set internally
        set.addAll(Arrays.asList(1, 2, 3, 4));   // size = 4, internal = Set
        Field valField = CompactSet.class.getDeclaredField("val");
        valField.setAccessible(true);
        assertTrue(valField.get(set) instanceof Set);

        // remove one element → size becomes 3 (compactSize) → should convert back to Object[]
        set.remove(4);
        Object internal = valField.get(set);
        assertTrue(internal instanceof Object[]);
        assertEquals(3, set.size());
    }

    @Test
    public void testRemoveLastElementResetsToEmpty() throws Exception {
        CompactSet<String> set = new CompactSet<>();
        set.add("only");
        assertTrue(set.remove("only"));
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());

        Field valField = CompactSet.class.getDeclaredField("val");
        valField.setAccessible(true);
        // EMPTY_SET constant is the literal string defined in CompactSet
        assertEquals("_︿_ψ_☼", valField.get(set));
    }

    @Test(expected = IllegalStateException.class)
    public void testConstructorThrowsWhenCompactSizeTooSmall() {
        new CompactSet<Integer>() {
            @Override
            protected int compactSize() { return 1; }
        };
    }
}
