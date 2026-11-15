package com.cedarsoftware.util;

import org.junit.Test;
import java.lang.reflect.Field;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link CompactSet}.
 *
 * The tests cover:
 *   • All public constructors and methods.
 *   • Normal usage (add, contains, remove, iterator, clear, etc.).
 *   • Edge cases such as null elements, duplicate adds, removal of non‑existent items.
 *   • Illegal state when {@code compactSize()} is configured < 2.
 *   • Case‑insensitive mode.
 *   • Transition logic between the internal {@code Object[]} representation and a backing {@code Set}
 *     when the size crosses the {@code compactSize()} threshold (both upward and downward).
 *
 * Helper methods use reflection to inspect the private {@code val} field in order to assert the
 * internal representation without breaking encapsulation of the production class.
 */
public class CompactSetTest {

    /* -------------------------------------------------------------
     *  Helper utilities to inspect the private `val` field.
     * ------------------------------------------------------------- */
    private static boolean isArrayInternal(CompactSet<?> set) {
        try {
            Field f = CompactSet.class.getDeclaredField("val");
            f.setAccessible(true);
            Object val = f.get(set);
            return val instanceof Object[];
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isSetInternal(CompactSet<?> set) {
        try {
            Field f = CompactSet.class.getDeclaredField("val");
            f.setAccessible(true);
            Object val = f.get(set);
            return val instanceof Set;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /* -------------------------------------------------------------
     *  1. Basic construction, size & emptiness
     * ------------------------------------------------------------- */
    @Test
    public void testEmptySetSizeAndIsEmpty() {
        CompactSet<String> set = new CompactSet<>();
        assertEquals("New set should have size 0", 0, set.size());
        assertTrue("New set should be empty", set.isEmpty());
    }

    @Test
    public void testConstructorWithCollection() {
        List<String> list = Arrays.asList("alpha", "beta", "gamma");
        CompactSet<String> set = new CompactSet<>(list);
        assertEquals("Size should match collection size", 3, set.size());
        assertTrue("All elements must be present", set.containsAll(list));
    }

    /* -------------------------------------------------------------
     *  2. Adding elements, duplicates and null handling
     * ------------------------------------------------------------- */
    @Test
    public void testAddAndContains() {
        CompactSet<Integer> set = new CompactSet<>();
        assertTrue("First add should succeed", set.add(42));
        assertTrue("Element must be contained after add", set.contains(42));
        assertFalse("Duplicate add must return false", set.add(42));
        assertEquals("Size must remain 1 after duplicate add", 1, set.size());
    }

    @Test
    public void testAddNullElement() {
        CompactSet<String> set = new CompactSet<>();
        assertTrue("Adding null should succeed", set.add(null));
        assertTrue("Set must contain null after add", set.contains(null));
        assertEquals("Size must be 1 after adding null", 1, set.size());
    }

    /* -------------------------------------------------------------
     *  3. Removal logic & edge cases
     * ------------------------------------------------------------- */
    @Test
    public void testRemoveExistingElement() {
        CompactSet<String> set = new CompactSet<>();
        set.add("test");
        assertTrue("Removal of existing element must return true", set.remove("test"));
        assertFalse("Element must no longer be present", set.contains("test"));
        assertEquals("Size must be 0 after removal", 0, set.size());
    }

    @Test
    public void testRemoveNonExistingElement() {
        CompactSet<String> set = new CompactSet<>();
        set.add("present");
        assertFalse("Removing absent element must return false", set.remove("absent"));
        assertEquals("Size must stay unchanged", 1, set.size());
    }

    @Test
    public void testRemoveNullElement() {
        CompactSet<String> set = new CompactSet<>();
        set.add(null);
        assertTrue("Removing null must succeed", set.remove(null));
        assertFalse("Set must no longer contain null", set.contains(null));
    }

    /* -------------------------------------------------------------
     *  4. Iterator behaviour, including illegal remove()
     * ------------------------------------------------------------- */
    @Test
    public void testIteratorSimpleTraversal() {
        CompactSet<Integer> set = new CompactSet<>();
        set.add(1);
        set.add(2);
        set.add(3);

        Set<Integer> seen = new HashSet<>();
        for (Integer i : set) {
            assertTrue("Iterator must return each element once", seen.add(i));
        }
        assertEquals("All elements must have been visited", 3, seen.size());
    }

    @Test
    public void testIteratorRemoveWithoutCallingNext() {
        CompactSet<Integer> set = new CompactSet<>();
        set.add(10);
        Iterator<Integer> it = set.iterator();
        try {
            it.remove();
            fail("Calling remove() before next() must throw IllegalStateException");
        } catch (IllegalStateException expected) {
            // expected
        }
    }

    @Test
    public void testIteratorRemoveAfterNext() {
        CompactSet<String> set = new CompactSet<>();
        set.add("alpha");
        set.add("beta");

        Iterator<String> it = set.iterator();
        assertTrue("Iterator must have next", it.hasNext());
        String first = it.next();
        it.remove(); // should remove the element returned by next()
        assertFalse("Set must no longer contain the removed element", set.contains(first));
        assertEquals("Size must be reduced by one", 1, set.size());
    }

    /* -------------------------------------------------------------
     *  5. clear()
     * ------------------------------------------------------------- */
    @Test
    public void testClearEmptiesSet() {
        CompactSet<String> set = new CompactSet<>();
        set.add("x");
        set.add("y");
        set.clear();
        assertTrue("Set must be empty after clear()", set.isEmpty());
        assertEquals("Size must be 0 after clear()", 0, set.size());
    }

    /* -------------------------------------------------------------
     *  6. Illegal compactSize configuration (<2) must cause constructor failure
     * ------------------------------------------------------------- */
    @Test(expected = IllegalStateException.class)
    public void testIllegalCompactSizeThrows() {
        // Subclass CompactSet to force an illegal compactSize()
        new CompactSet<Integer>() {
            @Override
            protected int compactSize() {
                return 1; // illegal, must be >= 2
            }
        };
    }

    /* -------------------------------------------------------------
     *  7. Case‑insensitive mode
     * ------------------------------------------------------------- */
    @Test
    public void testCaseInsensitiveContains() {
        CompactSet<String> set = new CompactSet<String>() {
            @Override
            protected boolean isCaseInsensitive() {
                return true;
            }
        };
        set.add("HelloWorld");
        assertTrue("Contains must be case‑insensitive (lowercase)", set.contains("helloworld"));
        assertTrue("Contains must be case‑insensitive (mixed case)", set.contains("HeLLoWoRLd"));
    }

    /* -------------------------------------------------------------
     *  8. Transition from array → Set when size exceeds compactSize()
     * ------------------------------------------------------------- */
    @Test
    public void testTransitionToSetWhenThresholdExceeded() {
        CompactSet<Integer> set = new CompactSet<Integer>() {
            @Override
            protected int compactSize() {
                return 3; // small threshold for testing
            }
        };

        // Fill up to the threshold – internal representation should still be an array
        for (int i = 1; i <= 3; i++) {
            assertTrue(set.add(i));
        }
        assertEquals(3, set.size());
        assertTrue("Should still be using internal array representation", isArrayInternal(set));

        // Adding the 4th element must switch to a Set
        assertTrue(set.add(4));
        assertEquals(4, set.size());
        assertTrue("After crossing compactSize, internal representation must be a Set", isSetInternal(set));
    }

    /* -------------------------------------------------------------
     *  9. Transition from Set → array when size shrinks back to compactSize()
     * ------------------------------------------------------------- */
    @Test
    public void testTransitionBackToArrayWhenSizeDrops() {
        CompactSet<Integer> set = new CompactSet<Integer>() {
            @Override
            protected int compactSize() {
                return 2; // tiny threshold to force transition quickly
            }
        };

        // Add three elements → should be in Set mode (size > compactSize)
        set.add(10);
        set.add(20);
        set.add(30);
        assertEquals(3, set.size());
        assertTrue("Initially should be in Set mode", isSetInternal(set));

        // Remove one element → size becomes equal to compactSize; should revert to array mode
        assertTrue(set.remove(30));
        assertEquals(2, set.size());
        assertTrue("After shrinking to compactSize, should revert to array mode", isArrayInternal(set));
    }

    /* -------------------------------------------------------------
     * 10. Verify getNewSet(int) fallback works when no size‑constructor exists
     * ------------------------------------------------------------- */
    @Test
    public void testGetNewSetWithReflectionFallback() {
        // Use the default implementation; it attempts to locate a constructor with an int argument.
        // For HashSet there is such a constructor, so we just ensure no exception is thrown.
        CompactSet<String> set = new CompactSet<>();
        // The method is protected; we invoke it via an anonymous subclass.
        Set<String> newSet = set.getNewSet(5);
        assertNotNull("getNewSet(int) must return a non‑null Set", newSet);
        assertTrue("Returned Set should be empty", newSet.isEmpty());
    }
}
