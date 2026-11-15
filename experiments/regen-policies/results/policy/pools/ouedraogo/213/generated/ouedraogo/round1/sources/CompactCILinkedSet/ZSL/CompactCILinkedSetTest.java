/****************************************************************************************
 *  CompactCILinkedSetTest.java
 *
 *  JUnit‑4 test suite that provides comprehensive coverage of the public and protected
 *  behavior of {@link CompactCILinkedSet}.  The tests exercise:
 *
 *   • Construction (empty and collection‑based)
 *   • Case‑insensitive add / contains / remove semantics
 *   • Linked (insertion‑order) iteration
 *   • Bulk operations: addAll, containsAll, retainAll, removeAll
 *   • clear, toArray, equals, hashCode, toString
 *   • The protected {@code isCaseInsensitive()} flag (accessed via reflection)
 *
 *  The test class lives in the same package as the class under test so that protected
 *  members can be accessed without resorting to subclassing or mocking.
 *
 *  Author: OpenAI ChatGPT
 *****************************************************************************************/

package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.*;

import static org.junit.Assert.*;

public class CompactCILinkedSetTest {

    private CompactCILinkedSet<String> emptySet;
    private CompactCILinkedSet<String> populatedSet;

    @Before
    public void setUp() {
        emptySet = new CompactCILinkedSet<>();
        populatedSet = new CompactCILinkedSet<>();
        populatedSet.add("Alpha");
        populatedSet.add("Bravo");
        populatedSet.add("Charlie");
    }

    /** Helper to invoke a protected method via reflection. */
    private Object invokeProtected(Object target, String methodName, Class<?>... paramTypes) throws Exception {
        Method m = target.getClass().getSuperclass().getDeclaredMethod(methodName, paramTypes);
        m.setAccessible(true);
        return m.invoke(target);
    }

    /* -------------------------------------------------------------------------
     *  Construction & basic properties
     * ---------------------------------------------------------------------- */

    @Test
    public void testEmptyConstructor() {
        assertTrue("New set should be empty", emptySet.isEmpty());
        assertEquals("Size of a new set must be 0", 0, emptySet.size());
    }

    @Test
    public void testCollectionConstructorHandlesDuplicatesCaseInsensitively() {
        List<String> list = Arrays.asList("One", "two", "ONE", "Two", "three");
        CompactCILinkedSet<String> set = new CompactCILinkedSet<>(list);
        // Expected distinct elements: "One", "two", "three"
        assertEquals("Duplicate values with different case should be collapsed", 3, set.size());
        assertTrue(set.contains("ONE"));
        assertTrue(set.contains("Two"));
        assertTrue(set.contains("THREE"));
    }

    @Test
    public void testIsCaseInsensitiveFlag() throws Exception {
        // The flag is protected; use reflection to verify it returns true.
        Boolean flag = (Boolean) invokeProtected(emptySet, "isCaseInsensitive");
        assertTrue("CompactCILinkedSet must report case‑insensitive behavior", flag);
    }

    /* -------------------------------------------------------------------------
     *  Add / Contains / Remove (case‑insensitive)
     * ---------------------------------------------------------------------- */

    @Test
    public void testAddAndContainsCaseInsensitive() {
        assertTrue(populatedSet.add("Delta"));
        assertTrue(populatedSet.contains("delta"));
        assertTrue(populatedSet.contains("DELTA"));
        // Adding a duplicate with a different case must return false and not increase size
        assertFalse(populatedSet.add("DELTA"));
        assertEquals(4, populatedSet.size());
    }

    @Test
    public void testRemoveCaseInsensitive() {
        assertTrue(populatedSet.remove("bravo")); // lower‑case removal
        assertFalse(populatedSet.contains("BRAVO"));
        assertEquals(2, populatedSet.size());

        // Removing a non‑existent element should return false
        assertFalse(populatedSet.remove("nonexistent"));
    }

    /* -------------------------------------------------------------------------
     *  Bulk operations
     * ---------------------------------------------------------------------- */

    @Test
    public void testAddAllPreservesInsertionOrderAndCaseInsensitivity() {
        List<String> more = Arrays.asList("Echo", "Foxtrot", "ECHO", "golf");
        assertTrue(populatedSet.addAll(more));
        // Expected order: Alpha, Bravo, Charlie, Echo, Foxtrot, golf
        List<String> expected = Arrays.asList("Alpha", "Bravo", "Charlie", "Echo", "Foxtrot", "golf");
        List<String> actual = new ArrayList<>();
        for (String s : populatedSet) {
            actual.add(s);
        }
        assertEquals("Iteration order must reflect first occurrence of each distinct element",
                expected, actual);
        assertEquals(6, populatedSet.size());
    }

    @Test
    public void testContainsAllCaseInsensitive() {
        Collection<String> query = Arrays.asList("alpha", "BRAVO", "charlie");
        assertTrue(populatedSet.containsAll(query));
        assertFalse(populatedSet.containsAll(Arrays.asList("alpha", "delta")));
    }

    @Test
    public void testRetainAllKeepsOnlySpecifiedElements() {
        Collection<String> keep = Arrays.asList("bravo", "CHARLIE", "delta");
        assertTrue(populatedSet.retainAll(keep));
        // After retainAll, only Bravo and Charlie should remain (delta wasn't present)
        assertEquals(2, populatedSet.size());
        assertTrue(populatedSet.contains("BRAVO"));
        assertTrue(populatedSet.contains("charlie"));
        assertFalse(populatedSet.contains("Alpha"));
    }

    @Test
    public void testRemoveAllEliminatesSpecifiedElements() {
        Collection<String> remove = Arrays.asList("ALPHA", "bravo");
        assertTrue(populatedSet.removeAll(remove));
        assertEquals(1, populatedSet.size());
        assertFalse(populatedSet.contains("alpha"));
        assertFalse(populatedSet.contains("BRAVO"));
        assertTrue(populatedSet.contains("Charlie"));
    }

    /* -------------------------------------------------------------------------
     *  Clear / Iterator / toArray
     * ---------------------------------------------------------------------- */

    @Test
    public void testClearEmptiesSet() {
        populatedSet.clear();
        assertTrue(populatedSet.isEmpty());
        assertEquals(0, populatedSet.size());
    }

    @Test
    public void testIterationOrderIsLinked() {
        CompactCILinkedSet<String> linked = new CompactCILinkedSet<>();
        linked.add("first");
        linked.add("Second");
        linked.add("THIRD");
        linked.add("second"); // duplicate with different case – should be ignored

        Iterator<String> it = linked.iterator();
        List<String> order = new ArrayList<>();
        while (it.hasNext()) {
            order.add(it.next());
        }
        List<String> expected = Arrays.asList("first", "Second", "THIRD");
        assertEquals("Linked insertion order must be preserved", expected, order);
    }

    @Test
    public void testToArrayMatchesIterationOrder() {
        Object[] array = populatedSet.toArray();
        String[] expected = {"Alpha", "Bravo", "Charlie"};
        assertArrayEquals("toArray must return elements in insertion order", expected, array);
    }

    @Test
    public void testToArrayTyped() {
        String[] a = new String[0];
        String[] result = populatedSet.toArray(a);
        String[] expected = {"Alpha", "Bravo", "Charlie"};
        assertArrayEquals(expected, result);
    }

    /* -------------------------------------------------------------------------
     *  equals, hashCode, toString
     * ---------------------------------------------------------------------- */

    @Test
    public void testEqualsAndHashCode() {
        CompactCILinkedSet<String> setA = new CompactCILinkedSet<>();
        setA.addAll(Arrays.asList("x", "Y", "z"));

        CompactCILinkedSet<String> setB = new CompactCILinkedSet<>();
        setB.addAll(Arrays.asList("X", "y", "Z"));

        assertEquals("Sets with same logical elements must be equal", setA, setB);
        assertEquals("Equal sets must have identical hashCodes", setA.hashCode(), setB.hashCode());

        // Different content
        CompactCILinkedSet<String> setC = new CompactCILinkedSet<>();
        setC.addAll(Arrays.asList("x", "y"));
        assertNotEquals("Sets with different elements must not be equal", setA, setC);
    }

    @Test
    public void testToStringContainsElements() {
        String s = populatedSet.toString();
        assertTrue("toString should contain Alpha", s.contains("Alpha"));
        assertTrue("toString should contain Bravo", s.contains("Bravo"));
        assertTrue("toString should contain Charlie", s.contains("Charlie"));
        // Ensure brackets are present (standard Set toString format)
        assertTrue("toString should start with '['", s.startsWith("["));
        assertTrue("toString should end with ']'", s.endsWith("]"));
    }

    /* -------------------------------------------------------------------------
     *  Additional sanity checks for internal configuration (non‑public API)
     * ---------------------------------------------------------------------- */

    @Test
    public void testGetNewSetReturnsNullAsSpecified() throws Exception {
        // getNewSet is protected; verify that the subclass returns null per its contract.
        Object result = invokeProtected(populatedSet, "getNewSet");
        assertNull("CompactCILinkedSet.getNewSet() must return null", result);
    }

    @Test
    public void testWithConfigReturnsSelfWhenNoChanges() {
        // The base class provides withConfig; we simply verify that it returns a set
        // that is still case‑insensitive and contains the original elements.
        Map<String, Object> cfg = Collections.emptyMap();
        CompactCILinkedSet<String> after = (CompactCILinkedSet<String>) populatedSet.withConfig(cfg);
        assertEquals(populatedSet, after);
        assertTrue(after.contains("alpha"));
        assertTrue(after.contains("BRAVO"));
    }
}
