package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link CompactLinkedSet}.
 *
 * The tests cover:
 * <ul>
 *   <li>Construction (empty and from a collection)</li>
 *   <li>Null‑argument handling for the collection constructor</li>
 *   <li>Case‑insensitive behaviour of {@code add}, {@code contains},
 *       {@code remove} and {@code addAll}</li>
 *   <li>Set semantics (no duplicates, size, emptiness)</li>
 *   <li>Preservation of insertion order (linked behaviour)</li>
 *   <li>Protected API contract: {@code isCaseInsensitive} returns {@code true}
 *       and {@code getNewSet} returns {@code null}</li>
 *   <li>Behaviour with {@code null} elements (allowed by {@link java.util.Set})</li>
 * </ul>
 */
public class CompactLinkedSetTest {

    private CompactLinkedSet<String> set;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        set = new CompactLinkedSet<>();
    }

    /*** 1. Construction ***/

    @Test
    public void testEmptyConstructorCreatesEmptySet() {
        assertTrue("Newly created set must be empty", set.isEmpty());
        assertEquals("Size must be zero", 0, set.size());
    }

    @Test
    public void testCollectionConstructorCopiesElements() {
        List<String> source = Arrays.asList("One", "Two", "Three");
        CompactLinkedSet<String> fromCollection = new CompactLinkedSet<>(source);

        assertEquals("All elements must be copied", source.size(), fromCollection.size());
        for (String s : source) {
            assertTrue("Set must contain element: " + s, fromCollection.contains(s));
        }
    }

    @Test
    public void testCollectionConstructorNullThrowsNPE() {
        thrown.expect(NullPointerException.class);
        new CompactLinkedSet<String>(null);
    }

    /*** 2. Case‑insensitive behaviour ***/

    @Test
    public void testAddIsCaseInsensitive() {
        assertTrue(set.add("Apple"));
        // Adding same word with different case must be rejected
        assertFalse("Duplicate (different case) must not be added", set.add("apple"));
        assertEquals("Size must stay 1", 1, set.size());
        assertTrue("contains must be case‑insensitive", set.contains("APPLE"));
    }

    @Test
    public void testAddAllIsCaseInsensitive() {
        Collection<String> coll = Arrays.asList("One", "TWO", "three", "ONE", "Two");
        assertTrue(set.addAll(coll));
        assertEquals("Duplicates (case‑insensitive) must be ignored", 3, set.size());
        assertTrue(set.contains("one"));
        assertTrue(set.contains("two"));
        assertTrue(set.contains("THREE"));
    }

    @Test
    public void testRemoveIsCaseInsensitive() {
        set.addAll(Arrays.asList("Red", "Green", "Blue"));
        assertTrue(set.remove("GREEN"));
        assertFalse(set.contains("green"));
        assertEquals(2, set.size());

        // Removing a non‑present element (different case) must return false
        assertFalse(set.remove("yellow"));
    }

    /*** 3. Set semantics ***/

    @Test
    public void testNoDuplicatesWithSameCase() {
        assertTrue(set.add("dup"));
        assertFalse(set.add("dup")); // exact duplicate
        assertEquals(1, set.size());
    }

    @Test
    public void testClearEmptiesSet() {
        set.addAll(Arrays.asList("a", "b", "c"));
        assertFalse(set.isEmpty());
        set.clear();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    /*** 4. Insertion order preservation ***/

    @Test
    public void testIteratorPreservesInsertionOrder() {
        List<String> input = Arrays.asList("first", "Second", "THIRD");
        set.addAll(input);

        Iterator<String> it = set.iterator();
        for (String expected : input) {
            assertTrue("Iterator must have next element", it.hasNext());
            assertEquals("Elements must appear in insertion order", expected, it.next());
        }
        assertFalse("Iterator must be exhausted", it.hasNext());
    }

    /*** 5. Null element handling ***/

    @Test
    public void testAddNullElement() {
        assertTrue("Set should allow null element", set.add(null));
        assertTrue("Contains must recognise null", set.contains(null));
        assertEquals(1, set.size());

        // Adding another null must be rejected (duplicate)
        assertFalse("Second null insertion must be ignored", set.add(null));
        assertEquals(1, set.size());

        // Removal of null works
        assertTrue(set.remove(null));
        assertFalse(set.contains(null));
        assertTrue(set.isEmpty());
    }

    /*** 6. Protected API contract ***/

    @Test
    public void testIsCaseInsensitiveProtectedMethodReturnsTrue() throws Exception {
        Method m = CompactLinkedSet.class.getDeclaredMethod("isCaseInsensitive");
        m.setAccessible(true);
        boolean result = (boolean) m.invoke(set);
        assertTrue("isCaseInsensitive must return true", result);
    }

    @Test
    public void testGetNewSetProtectedMethodReturnsNull() throws Exception {
        Method m = CompactLinkedSet.class.getDeclaredMethod("getNewSet");
        m.setAccessible(true);
        Object result = m.invoke(set);
        assertNull("getNewSet must return null as per implementation", result);
    }

    /*** 7. equals() and hashCode() contract ***/

    @Test
    public void testEqualsAndHashCodeWithCaseInsensitiveContent() {
        CompactLinkedSet<String> other = new CompactLinkedSet<>();
        set.addAll(Arrays.asList("Alpha", "beta"));
        other.addAll(Arrays.asList("ALPHA", "BETA"));

        assertEquals("Sets with same elements (case‑insensitive) must be equal", set, other);
        assertEquals("Hash codes must be equal for equal sets", set.hashCode(), other.hashCode());
    }

    /*** 8. toString representation includes elements ***/

    @Test
    public void testToStringContainsAllElements() {
        set.addAll(Arrays.asList("x", "y", "z"));
        String str = set.toString();

        assertTrue("toString must contain element 'x'", str.contains("x"));
        assertTrue("toString must contain element 'y'", str.contains("y"));
        assertTrue("toString must contain element 'z'", str.contains("z"));
    }
}
