package com.cedarsoftware.util;

import org.mockito.Mock;
import java.lang.reflect.Field;
import java.util.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
// import org.junit.*;
// import org.junit.rules.ExpectedException;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
// import static org.junit.Assert.*;

// @ExtendWith(MockitoExtension.class)
class CompactSet_add_Test {

    /**
     * The set under test – created fresh for each test method.
     */
    private Set<String> set;

    /**
     * Allows us to inject Mockito mocks without the JUnit‑5 extension.
     */
//     @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

//     @BeforeEach
    void setupBeforeEach() {
        // Use the public no‑arg constructor which creates a default CompactMap internally.
        set = new CompactSet<>();
//         assertTrue("Set should start empty", set.isEmpty());
    }

//     @AfterEach
    void teardownAfterEach() {
        // Ensure the set is cleared after each test to avoid cross‑test contamination.
        set.clear();
//         assertTrue("Set should be empty after clear", set.isEmpty());
    }

//     @Test
//     @DisplayName("Adding a new element returns true and increments size")
//     void testAddNewElement() {
//         String element = "alpha";
//         boolean added = set.add(element);
//         assertTrue("First addition of a new element must return true", added);
//         assertEquals("Size should be 1 after adding one element", 1, set.size());
//         assertTrue("Set must contain the newly added element", set.contains(element));
//     }

//     @Test
//     @DisplayName("Adding a duplicate element returns false and does not change size")
//     void testAddDuplicateElement() {
//         String element = "beta";
//         assertTrue("Initial add must succeed", set.add(element));
//         boolean addedAgain = set.add(element);
//         assertFalse("Adding a duplicate must return false", addedAgain);
//         assertEquals("Size must remain unchanged after duplicate add", 1, set.size());
//     }

//     @Test
//     @DisplayName("Adding null works like any other element")
//     void testAddNullElement() {
//         boolean firstAdd = set.add(null);
//         boolean secondAdd = set.add(null);
//         assertTrue("First addition of null should return true", firstAdd);
//         assertFalse("Second addition of null should return false", secondAdd);
//         assertEquals("Size should be 1 after adding null once", 1, set.size());
//         assertTrue("Set must contain null after it has been added", set.contains(null));
//     }

//     @Test
//     @DisplayName("Adding several distinct elements updates size correctly")
//     void testAddMultipleDistinctElements() {
//         String[] elements = { "one", "two", "three", "four" };
//         for (String e : elements) {
//             assertTrue("Adding a new distinct element must return true", set.add(e));
//         }
//         assertEquals("Size must equal number of distinct elements added", elements.length, set.size());
//         for (String e : elements) {
//             assertTrue("Set must contain each added element", set.contains(e));
//         }
//     }

//     @Test
//     @DisplayName("Adding after removal works as expected")
//     void testAddAfterRemoval() {
//         String elem = "gamma";
//         assertTrue("Initial add must succeed", set.add(elem));
//         assertTrue("Removal must succeed", set.remove(elem));
//         assertTrue("Set should be empty after removal", set.isEmpty());
//         boolean addedAgain = set.add(elem);
//         assertTrue("Re‑adding after removal must return true", addedAgain);
//         assertEquals("Size should be 1 after re‑adding", 1, set.size());
//         assertTrue("Set must contain the element after re‑adding", set.contains(elem));
//     }

//     @Test
//     @DisplayName("Adding after clear treats previously‑present element as new")
//     void testAddAfterClear() {
//         String element = "beta";
//         assertTrue("First add should succeed", set.add(element));
//         set.clear();
//         assertTrue("Set should be empty after clear", set.isEmpty());
//         assertTrue("Adding after clear must be treated as new and return true", set.add(element));
//     }

//     @Test
//     @DisplayName("Case‑insensitive set treats different‑cased strings as duplicates")
//     void testCaseInsensitiveBehavior() {
//         CompactSet<String> ciSet = CompactSet.<String>builder().caseSensitive(false).build();
//         assertTrue("First addition of \"Alpha\" must succeed", ciSet.add("Alpha"));
//         assertFalse("Adding \"alpha\" should be seen as duplicate in case‑insensitive mode", ciSet.add("alpha"));
//     }

//     @Test
//     @DisplayName("Sorted ordering does not allow null keys (NPE)")
//     void testSortedOrderingNullHandling() {
//         CompactSet<String> sortedSet = CompactSet.<String>builder().sortedOrder().build();
//         try {
//             sortedSet.add(null);
//             fail("Adding null to a sorted CompactSet must throw NullPointerException");
//         } catch (NullPointerException ignored) {
//             // expected
//         }
//     }

//     @Test
//     @DisplayName("retainAll reduces the set and allows re‑adding removed element")
//     void testRetainAllReducesSetAndReAdd() {
//         CompactSet<String> cs = new CompactSet<>();
//         assertTrue(cs.add("x"));
//         assertTrue(cs.add("y"));
//         cs.retainAll(Collections.singleton("x"));
//         assertEquals(1, cs.size());
//         assertTrue(cs.contains("x"));
//         assertFalse(cs.contains("y"));
//         assertTrue("Re‑adding an element removed by retainAll should succeed", cs.add("y"));
//         assertEquals(2, cs.size());
//         assertTrue(cs.containsAll(Arrays.asList("x", "y")));
//     }

//     @Test
//     @DisplayName("Duplicate element with distinct instance (equals) is rejected")
//     void testDuplicateWithEqualButDistinctInstance() {
//         assertTrue(set.add("beta"));
//         assertFalse("Adding a distinct String instance that equals existing element must return false", set.add(new String("beta")));
//     }

//     @Test
//     @DisplayName("Sorted ordering type mismatch causes ClassCastException")
//     void testSortedOrderingTypeMismatch() {
//         CompactSet<Object> sortedSet = CompactSet.<Object>builder().sortedOrder().build();
//         assertTrue(sortedSet.add("alpha"));
//         try {
//             sortedSet.add(1);
//             fail("Adding an Integer to a TreeMap that already contains Strings must throw ClassCastException");
//         } catch (ClassCastException ignored) {
//             // expected
//         }
//     }

//     @Test
//     @DisplayName("Underlying unmodifiable map propagates UnsupportedOperationException")
//     void testUnderlyingMapUnmodifiable() throws Exception {
//         CompactSet<String> mutableSet = new CompactSet<>();
//         @SuppressWarnings("unchecked")
//         CompactMap<String, Object> mockedMap = mock(CompactMap.class);
//         when(mockedMap.put(any(), any())).thenThrow(new UnsupportedOperationException("unmodifiable"));
//         Field mapField = CompactSet.class.getDeclaredField("map");
//         mapField.setAccessible(true);
//         mapField.set(mutableSet, mockedMap);
//         try {
//             mutableSet.add("gamma");
//             fail("add() should propagate UnsupportedOperationException from the backing map");
//         } catch (UnsupportedOperationException ex) {
//             assertEquals("unmodifiable", ex.getMessage());
//         }
//     }

    /* --------------------------------------------------------------
     *  Parameterized test for basic true/false expectations (optional)
     * -------------------------------------------------------------- */
//     @ParameterizedTest(name = "[{index}] add({0}) => {1}")
//     @CsvSource({ // duplicate
//     // duplicate
//     // duplicate
//     "'alpha', true", "'alpha', false", "'beta', true", "'beta', false" })
//     void testParameterizedAdd(String element, boolean expected) {
//         // First call for a given element should be true, second false.
//         // To keep the test deterministic we reset the set for each parameter set.
//         Set<String> localSet = new CompactSet<>();
//         boolean first = localSet.add(element);
//         assertTrue(first, "First addition must be true");
//         boolean second = localSet.add(element);
//         assertEquals(expected, second, "Second addition expectation does not match");
//     }

    /* --------------------------------------------------------------
     *  Simple deterministic test for duplicate addition behaviour.
     * -------------------------------------------------------------- */
//     @Test
//     public void testSimpleDuplicateAdd() {
//         Set<String> localSet = new CompactSet<>();
//         assertTrue(localSet.add("alpha"));
//         assertFalse(localSet.add("alpha"));
//         assertTrue(localSet.add("beta"));
//         assertFalse(localSet.add("beta"));
//     }
}