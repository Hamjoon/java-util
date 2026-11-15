package com.cedarsoftware.util;

import static org.mockito.Mockito.*;
import java.util.*;
import java.util.stream.*;
import org.mockito.Mockito;
// import org.junit.*;
// import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

// @ExtendWith(MockitoExtension.class)
// @RunWith(MockitoJUnitRunner.class)
class CompactSet_addAll_Test {

    /**
     * The set under test – a real instance (not a mock) so that the
     * actual {@code addAll} implementation is exercised.
     */
    private CompactSet<String> realSet;

    /**
     * A spy that wraps the real set – this allows us to verify how many
     * times {@code add(E)} is invoked without altering the behaviour.
     */
    private CompactSet<String> spySet;

    /**
     * Thread‑local holder used to pass pre‑population data from the
     * argument provider to the test method without exposing it in the
     * method signature.
     */
    private static class ThreadLocalPrepopulatedData {

        private static final ThreadLocal<List<String>> data = new ThreadLocal<>();

        static void store(List<String> initial) {
            data.set(initial);
        }

        static List<String> retrieveAndClear() {
            List<String> initial = data.get();
            data.remove();
            return initial;
        }
    }

//     @BeforeEach
    void setupBeforeEach() {
        realSet = new CompactSet<>();
        // Wrap the real instance with a spy; the spy will delegate to the real methods.
        spySet = Mockito.spy(realSet);
    }

//     @AfterEach
    void teardownAfterEach() {
        realSet = null;
        spySet = null;
    }

    /**
     * Provides a handful of scenarios for the {@code addAll} tests.
     */
//     private static Stream<Arguments> provideAddAllScenarios() {
//         List<Object[]> list = new ArrayList<>();
//         // empty collection on a set that already contains elements
//         list.add(new Object[] { Collections.emptyList(), false, "Empty collection on a set that already contains elements" });
//         // all elements are new – should return true
//         list.add(new Object[] { Arrays.asList("A", "B", "C"), true, "All elements are new – should return true" });
//         // mixed existing and new elements – should return true
//         list.add(new Object[] { prepopulateSet(Arrays.asList("X", "Y"), Arrays.asList("X", "Z", "Y", "W")), true, "Mixed existing and new elements – should return true" });
//         // collection contains a null (new) and a new element – should return true
//         list.add(new Object[] { Arrays.asList(null, "A"), true, "Collection contains a null (new) and a new element – should return true" });
//         return list;
//     }

    /**
     * Helper that creates a fresh {@link CompactSet} pre‑populated with {@code initialElements},
     * then returns the collection to be added (the {@code toAdd} argument).
     * The returned collection is used directly in the parameterized test, while the
     * pre‑populated set is stored in a thread‑local variable accessed by the test method.
     */
    private static Collection<String> prepopulateSet(List<String> initialElements, List<String> toAdd) {
        ThreadLocalPrepopulatedData.store(initialElements);
        return toAdd;
    }

    /**
     * Simple sanity test that verifies {@code addAll} works for a
     *  normal collection of new elements.
     */
//     @Test
//     public void addAllAddsAllElementsAndReturnsTrue() {
//         // GIVEN
//         Collection<String> toAdd = Arrays.asList("alpha", "beta", "gamma");
//         // WHEN
//         boolean changed = spySet.addAll(toAdd);
//         // THEN
//         Assert.assertTrue("addAll should report modification when new elements are added", changed);
//         Assert.assertEquals("Set size must match number of distinct added elements", 3, spySet.size());
//         Assert.assertTrue("All supplied elements must be present after addAll", spySet.containsAll(toAdd));
//         // Verify that add(E) was invoked exactly once for each element.
//         for (String s : toAdd) {
//             verify(spySet, times(1)).add(s);
//         }
//     }

    /**
     * Parameterised‑style test that iterates over the scenarios
     *  defined in {@link #provideAddAllScenarios()}.
     */
//     @Test
//     public void addAllParameterized() {
//         for (Object[] scenario : provideAddAllScenarios()) {
//             @SuppressWarnings("unchecked")
//             Collection<String> collection = (Collection<String>) scenario[0];
//             boolean expectedResult = (Boolean) scenario[1];
//             String description = (String) scenario[2];
//             // Retrieve any pre‑population data that may have been stored.
//             List<String> prepopulate = ThreadLocalPrepopulatedData.retrieveAndClear();
//             if (prepopulate != null) {
//                 prepopulate.forEach(spySet::add);
//             }
//             // WHEN
//             boolean changed = spySet.addAll(collection);
//             // THEN
//             Assert.assertEquals("Unexpected return value for scenario: " + description, expectedResult, changed);
//         }
//     }

    /**
     * Verifies that adding a collection containing {@code null}
     *  works correctly.
     */
//     @Test
//     public void addAllWithNullElement() {
//         Collection<String> coll = Arrays.asList(null, "A");
//         boolean changed = spySet.addAll(coll);
//         Assert.assertTrue(changed);
//         Assert.assertTrue(spySet.contains(null));
//         Assert.assertTrue(spySet.contains("A"));
//     }

    /**
     * Verifies that adding an empty collection returns {@code false}
     *  and does not modify the set.
     */
//     @Test
//     public void addAllWithEmptyCollectionReturnsFalseAndLeavesSetUnchanged() {
//         spySet.addAll(Arrays.asList("a", "b"));
//         Collection<String> empty = Collections.emptyList();
//         boolean changed = spySet.addAll(empty);
//         Assert.assertFalse(changed);
//         Assert.assertEquals(2, spySet.size());
//         Assert.assertTrue(spySet.containsAll(Arrays.asList("a", "b")));
//         // No interaction with add(E) because the collection is empty.
//         verify(spySet, never()).add(any());
//     }

    /**
     * Verifies that passing {@code null} as the collection throws
     *  {@link NullPointerException}.
     */
//     @Test(expected = NullPointerException.class)
//     public void addAllNullCollectionThrowsNullPointerException() {
//         spySet.addAll(null);
//     }
}