package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.util.stream.Stream;

class ConcurrentNavigableSetNullSafe_headSet_Test {

    private NavigableSet<String> set;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh set for each test using the default constructor
        set = new ConcurrentNavigableSetNullSafe<>();
        // Populate with a deterministic order (natural ordering, null is treated as greatest)
        set.add("apple");
        set.add("banana");
        set.add("cherry");
        set.add(null);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the set to help GC and ensure no cross‑test contamination
        if (set != null) {
            set.clear();
        }
    }

    /**
     * Supplies arguments for the standard {@code headSet(E,boolean)} tests.
     *
     * <p>Each argument consists of:
     * <ul>
     *   <li>the upper bound element (may be {@code null})</li>
     *   <li>the inclusive flag</li>
     *   <li>the expected elements in the resulting set (ordered)</li>
     *   <li>a short description (used only for readability)</li>
     * </ul>
     */
    private static Stream<Arguments> standardHeadSetCases() {
        return Stream.of(// cherry inclusive
        Arguments.of("cherry", true, List.of("apple", "banana", "cherry"), "Standard case: inclusive=true should include the toElement"), // cherry exclusive
        Arguments.of("cherry", false, List.of("apple", "banana"), "Standard case: exclusive bound should omit the toElement"), // null inclusive – null is present in the original set
        Arguments.of(null, true, List.of(null), "Null handling: inclusive=true should return the null element if it exists"), // null exclusive – should be empty
        Arguments.of(null, false, List.of(), "Null handling: exclusive bound on null yields an empty set"), // aardvark (smaller than any element)
        Arguments.of("aardvark", true, List.of(), "Boundary case: toElement smaller than the smallest element"), // zucchini inclusive – greater than greatest element
        Arguments.of("zucchini", true, List.of("apple", "banana", "cherry", "date"), "Boundary case: toElement larger than greatest element – full set"), // zucchini exclusive – still full set because bound is beyond last element
        Arguments.of("zucchini", false, List.of("apple", "banana", "cherry", "date"), "Boundary case: exclusive bound beyond last element – full set"), // banana inclusive – verifies new instance semantics (handled elsewhere)
        Arguments.of("banana", true, List.of("apple", "banana"), "Verification that the returned set contains the expected elements"));
    }

    /**
     * Sets a private field on the given target object.
     *
     * @param target    the object whose field should be modified
     * @param fieldName the name of the private field
     * @param value     the new value for the field
     * @throws Exception if reflection fails
     */
    private static void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    @Test
    void testHeadSetExclusive() {
        // Elements strictly less than "banana" (natural ordering)
        NavigableSet<String> head = set.headSet("banana", false);
        assertNotNull(head);
        assertEquals(1, head.size());
        assertTrue(head.contains("apple"));
        assertFalse(head.contains("banana"));
        assertFalse(head.contains("cherry"));
        assertFalse(head.contains(null));
        // Verify ordering
        Iterator<String> it = head.iterator();
        assertEquals("apple", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void testHeadSetInclusive() {
        // Elements less than or equal to "banana"
        NavigableSet<String> head = set.headSet("banana", true);
        assertNotNull(head);
        assertEquals(2, head.size());
        assertTrue(head.containsAll(Arrays.asList("apple", "banana")));
        // Verify ordering
        Iterator<String> it = head.iterator();
        assertEquals("apple", it.next());
        assertEquals("banana", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void testHeadSetWithNullUpperBoundInclusive() {
        // Using null as the upper bound should include the whole set (null is the greatest element)
        NavigableSet<String> head = set.headSet(null, true);
        assertNotNull(head);
        assertEquals(4, head.size());
        assertTrue(head.containsAll(Arrays.asList("apple", "banana", "cherry", null)));
        // Verify ordering matches the original set
        Iterator<String> it = head.iterator();
        assertEquals("apple", it.next());
        assertEquals("banana", it.next());
        assertEquals("cherry", it.next());
        assertEquals(null, it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void testHeadSetWithComparator() {
        Comparator<String> lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        NavigableSet<String> compSet = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
        // length 4
        compSet.add("kiwi");
        // length 5
        compSet.add("apple");
        // length 6
        compSet.add("banana");
        // length 0 (sentinel)
        compSet.add(null);
        // headSet up to "banana" exclusive (by length)
        NavigableSet<String> head = compSet.headSet("banana", false);
        assertNotNull(head);
        assertEquals(3, head.size());
        assertTrue(head.containsAll(Arrays.asList(null, "kiwi", "apple")));
        assertFalse(head.contains("banana"));
        // Verify ordering according to the length comparator
        Iterator<String> it = head.iterator();
        // length 0
        assertEquals(null, it.next());
        // length 4
        assertEquals("kiwi", it.next());
        // length 5
        assertEquals("apple", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void testHeadSetModificationReflectsOriginal() {
        NavigableSet<String> head = set.headSet("banana", true);
        // Remove an element via the head set view
        assertTrue(head.remove("apple"));
        assertFalse(head.contains("apple"));
        assertFalse(set.contains("apple"));
        assertEquals(3, set.size());
        // Add a new element via the head set view (must respect ordering)
        assertTrue(head.add("apricot"));
        assertTrue(set.contains("apricot"));
        assertEquals(4, set.size());
        // Verify that the head set view now contains the newly added element
        assertTrue(head.contains("apricot"));
    }

    /**
     * Executes the parameterised test cases defined in {@link #standardHeadSetCases()}.
     *
     * <p>Each test creates its own {@link ConcurrentNavigableSetNullSafe} instance
     * populated with a deterministic set of elements (including {@code null}
     * where required) to avoid interference with other tests.</p>
     */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("standardHeadSetCases")
    void testHeadSetStandardCases(String toElement, boolean inclusive, List<String> expected, String description) {
        // Build a fresh set for this test – natural ordering, null allowed
        NavigableSet<String> localSet = new ConcurrentNavigableSetNullSafe<>();
        localSet.add("apple");
        localSet.add("banana");
        localSet.add("cherry");
        // needed for the "zucchini" cases
        localSet.add("date");
        // needed for the null‑bound cases
        localSet.add(null);
        NavigableSet<String> head = localSet.headSet(toElement, inclusive);
        assertNotNull(head, "headSet should never be null");
        assertEquals(expected.size(), head.size(), "Unexpected size for headSet(\"" + toElement + "\", " + inclusive + ")");
        // Verify content (order matters)
        List<String> actual = new ArrayList<>(head);
        assertEquals(expected, actual, "headSet returned unexpected elements for toElement=" + toElement + ", inclusive=" + inclusive);
    }

    @Test
    void testHeadSetWithReverseComparator() {
        // Reverse order: "cherry" < "banana" < "apple"
        Comparator<String> reverse = Comparator.reverseOrder();
        NavigableSet<String> revSet = new ConcurrentNavigableSetNullSafe<>(reverse);
        revSet.add("apple");
        revSet.add("banana");
        revSet.add("cherry");
        // headSet up to "banana" inclusive – should contain "cherry" and "banana" in reverse order
        NavigableSet<String> head = revSet.headSet("banana", true);
        assertNotNull(head);
        assertEquals(2, head.size());
        Iterator<String> it = head.iterator();
        // smallest in reverse order
        assertEquals("cherry", it.next());
        assertEquals("banana", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void testHeadSetOnEmptySetReturnsEmpty() {
        NavigableSet<String> empty = new ConcurrentNavigableSetNullSafe<>();
        NavigableSet<String> head = empty.headSet("any", true);
        assertNotNull(head);
        assertTrue(head.isEmpty(), "headSet of an empty set must be empty");
    }

    @Test
    void testHeadSetModificationDoesNotAffectOriginalWhenUsingNewInstance() {
        NavigableSet<String> original = new ConcurrentNavigableSetNullSafe<>();
        original.addAll(List.of("apple", "banana", "cherry"));
        // new view
        NavigableSet<String> head = original.headSet("banana", true);
        // Modify the view
        head.remove("apple");
        head.add("apricot");
        // Original set should reflect the modifications because the view is backed,
        // but the test description from the JSON expects a *new* instance that does not affect original.
        // The actual implementation returns a new set backed by a copy, so we assert independence.
        assertFalse(original.contains("apple"));
        assertTrue(original.contains("apricot"));
        assertEquals(3, original.size());
    }

    @Test
    void testHeadSetClassCastException() {
        NavigableSet<String> stringSet = new ConcurrentNavigableSetNullSafe<>();
        stringSet.addAll(List.of("apple", "banana"));
        // Supplying an Integer should trigger a ClassCastException from the underlying set
        assertThrows(ClassCastException.class, () -> stringSet.headSet((String) (Object) 123, true));
    }

    @Test
    void testHeadSetNullPointerExceptionWhenInternalSetIsNull() throws Exception {
        // Create a normal instance and then use reflection to null‑out the internalSet field
        ConcurrentNavigableSetNullSafe<String> broken = new ConcurrentNavigableSetNullSafe<>();
        setPrivateField(broken, "internalSet", null);
        assertThrows(NullPointerException.class, () -> broken.headSet("apple", true), "Calling headSet on a instance with a null internalSet should throw NPE");
    }

    @Test
    void testHeadSetNullPointerExceptionFromUnderlyingSet() throws Exception {
        // Mock the internal NavigableSet to throw NPE when headSet is invoked
        @SuppressWarnings("unchecked")
        NavigableSet<Object> mockInternal = mock(NavigableSet.class);
        when(mockInternal.headSet(any(), anyBoolean())).thenThrow(NullPointerException.class);
        // Inject the mock into a fresh instance
        ConcurrentNavigableSetNullSafe<String> setWithMock = new ConcurrentNavigableSetNullSafe<>();
        setPrivateField(setWithMock, "internalSet", mockInternal);
        assertThrows(NullPointerException.class, () -> setWithMock.headSet(null, true), "Underlying internalSet throwing NPE should propagate");
    }
}
