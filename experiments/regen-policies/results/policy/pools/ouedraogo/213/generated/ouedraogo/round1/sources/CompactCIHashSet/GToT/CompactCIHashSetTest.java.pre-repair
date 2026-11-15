package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.Rule;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link CompactCIHashSet}.
 *
 * The tests cover:
 *   • All public constructors.
 *   • All inherited Set operations (add, remove, contains, etc.) with a focus on
 *     case‑insensitive behaviour.
 *   • Edge‑case handling such as null arguments and duplicate values with
 *     different capitalisation.
 *   • The protected {@code isCaseInsensitive()} method via reflection.
 */
public class CompactCIHashSetTest {

    private CompactCIHashSet<String> emptySet;
    private CompactCIHashSet<String> populatedSet;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        emptySet = new CompactCIHashSet<>();
        populatedSet = new CompactCIHashSet<>(Arrays.asList("Apple", "Banana"));
    }

    /* --------------------------------------------------------------------- */
    /* Constructor tests                                                     */
    /* --------------------------------------------------------------------- */

    @Test
    public void testDefaultConstructorCreatesEmptySet() {
        assertTrue(emptySet.isEmpty());
        assertEquals(0, emptySet.size());
    }

    @Test
    public void testConstructorWithCollectionCopiesElements() {
        List<String> source = Arrays.asList("One", "Two", "Three");
        CompactCIHashSet<String> set = new CompactCIHashSet<>(source);
        assertEquals(source.size(), set.size());
        for (String s : source) {
            assertTrue(set.contains(s));
        }
    }

    @Test
    public void testConstructorWithNullCollectionThrowsNPE() {
        thrown.expect(NullPointerException.class);
        new CompactCIHashSet<String>(null);
    }

    /* --------------------------------------------------------------------- */
    /* Case‑insensitive behaviour tests                                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAddAndContainsAreCaseInsensitive() {
        emptySet.add("Hello");
        assertTrue(emptySet.contains("hello"));
        assertTrue(emptySet.contains("HELLO"));
    }

    @Test
    public void testAddingDuplicateValuesWithDifferentCaseDoesNotIncreaseSize() {
        emptySet.add("Test");
        boolean addedAgain = emptySet.add("test"); // should be false – duplicate
        assertFalse(addedAgain);
        assertEquals(1, emptySet.size());
    }

    @Test
    public void testRemoveIsCaseInsensitive() {
        emptySet.add("World");
        boolean removed = emptySet.remove("world");
        assertTrue(removed);
        assertTrue(emptySet.isEmpty());
    }

    @Test
    public void testAddAllIsCaseInsensitive() {
        List<String> list = Arrays.asList("Alpha", "ALPHA", "beta");
        emptySet.addAll(list);
        // "Alpha" and "ALPHA" are the same entry
        assertEquals(2, emptySet.size());
        assertTrue(emptySet.contains("alpha"));
        assertTrue(emptySet.contains("BETA"));
    }

    /* --------------------------------------------------------------------- */
    /* Standard Set operation tests                                          */
    /* --------------------------------------------------------------------- */

    @Test
    public void testContainsAll() {
        List<String> sub = Arrays.asList("apple", "BANANA");
        assertTrue(populatedSet.containsAll(sub));
    }

    @Test
    public void testRetainAllKeepsOnlySpecifiedElements() {
        List<String> retain = Arrays.asList("APPLE", "Cherry");
        boolean changed = populatedSet.retainAll(retain);
        assertTrue(changed);
        assertEquals(1, populatedSet.size());
        assertTrue(populatedSet.contains("apple"));
    }

    @Test
    public void testRemoveAllRemovesSpecifiedElements() {
        List<String> toRemove = Arrays.asList("APPLE");
        boolean changed = populatedSet.removeAll(toRemove);
        assertTrue(changed);
        assertEquals(1, populatedSet.size());
        assertFalse(populatedSet.contains("Apple"));
        assertTrue(populatedSet.contains("Banana"));
    }

    @Test
    public void testClearEmptiesTheSet() {
        populatedSet.clear();
        assertTrue(populatedSet.isEmpty());
        assertEquals(0, populatedSet.size());
    }

    @Test
    public void testIteratorTraversesAllElements() {
        Iterator<String> it = populatedSet.iterator();
        int count = 0;
        while (it.hasNext()) {
            String val = it.next();
            assertNotNull(val);
            count++;
        }
        assertEquals(populatedSet.size(), count);
    }

    @Test
    public void testToArrayReturnsAllElements() {
        Object[] array = populatedSet.toArray();
        assertEquals(populatedSet.size(), array.length);
        // Ensure case‑insensitivity of the content
        assertTrue(Arrays.asList(array).contains("apple"));
        assertTrue(Arrays.asList(array).contains("banana"));
    }

    @Test
    public void testToArrayTypedReturnsAllElements() {
        String[] typed = populatedSet.toArray(new String[0]);
        assertEquals(populatedSet.size(), typed.length);
        assertTrue(Arrays.asList(typed).containsAll(Arrays.asList("Apple", "Banana")));
    }

    @Test
    public void testEqualsAndHashCode() {
        CompactCIHashSet<String> other = new CompactCIHashSet<>(Arrays.asList("banana", "APPLE"));
        assertEquals(populatedSet, other);
        assertEquals(populatedSet.hashCode(), other.hashCode());

        other.add("extra");
        assertNotEquals(populatedSet, other);
    }

    @Test
    public void testToStringContainsElements() {
        String s = populatedSet.toString();
        assertTrue(s.contains("Apple"));
        assertTrue(s.contains("Banana"));
    }

    /* --------------------------------------------------------------------- */
    /* Protected method verification (via reflection)                       */
    /* --------------------------------------------------------------------- */

    @Test
    public void testIsCaseInsensitiveViaReflection() throws Exception {
        Method m = CompactCIHashSet.class.getDeclaredMethod("isCaseInsensitive");
        m.setAccessible(true);
        boolean result = (Boolean) m.invoke(populatedSet);
        assertTrue("CompactCIHashSet should report case‑insensitive", result);
    }

    /* --------------------------------------------------------------------- */
    /* Miscellaneous edge‑case tests                                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAddNullReturnsFalseOrThrows() {
        // CompactSet's contract for null is not defined; we accept either behaviour.
        try {
            boolean added = emptySet.add(null);
            // If the implementation allows null, it must be considered a distinct element.
            // In that case size should be 1; otherwise false.
            if (added) {
                assertEquals(1, emptySet.size());
                assertTrue(emptySet.contains(null));
            } else {
                assertEquals(0, emptySet.size());
                assertFalse(emptySet.contains(null));
            }
        } catch (NullPointerException npe) {
            // Acceptable alternative behaviour.
        }
    }

    @Test
    public void testContainsNullReturnsFalseOrThrows() {
        try {
            assertFalse(emptySet.contains(null));
        } catch (NullPointerException npe) {
            // Acceptable – some Set implementations throw NPE for null argument.
        }
    }
}
