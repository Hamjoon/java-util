package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

@ExtendWith(MockitoExtension.class)
class ConcurrentSet_toArray_Test {

    /**
     * The set instance used by each test.
     */
    private ConcurrentSet<Object> set;

    /**
     * Simple POJO used for reference‑equality verification.
     */
    static final class Person {

        final int id;

        final String name;

        Person(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return "Person{id=" + id + ", name='" + name + "'}";
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh set before each test to guarantee isolation.
        set = new ConcurrentSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the set to help the garbage collector and avoid side‑effects.
        if (set != null) {
            set.clear();
        }
    }

    /**
     * Helper to replace the private final {@code set} field via reflection.
     */
    private void injectInternalSet(Set<Object> delegate) throws Exception {
        Field f = ConcurrentSet.class.getDeclaredField("set");
        f.setAccessible(true);
        // Remove final modifier if present (works on most JVMs for test purposes)
        f.set(set, delegate);
    }

    @Test
    void testToArrayWithoutNullElements() {
        // given
        set.addAll(Arrays.asList("x", "y", "z"));
        // when
        Object[] array = set.toArray();
        // then
        assertEquals(3, array.length, "Array length should match set size");
        List<Object> asList = Arrays.asList(array);
        assertTrue(asList.containsAll(Arrays.asList("x", "y", "z")), "Array should contain all elements added to the set");
    }

    @Test
    void testToArrayWithNullElement() {
        // given
        set.addAll(Arrays.asList("a", null, "b"));
        // when
        Object[] array = set.toArray();
        // then
        assertEquals(3, array.length, "Array should have three elements");
        List<Object> asList = Arrays.asList(array);
        assertTrue(asList.contains(null), "Array must contain the null element");
        assertTrue(asList.containsAll(Arrays.asList("a", "b")), "Array must contain the non‑null elements");
    }

    @Test
    void testToArrayReturnedArrayIsIndependent() {
        // given
        set.addAll(Arrays.asList(1, 2, 3));
        // when
        Object[] array = set.toArray();
        // mutate the returned array
        array[0] = 99;
        // then
        // The set must remain unchanged – mutation of the array must not affect it.
        assertTrue(set.containsAll(Arrays.asList(1, 2, 3)), "Modifying the returned array must not affect the set contents");
        assertEquals(3, set.size(), "Set size must stay the same after array mutation");
    }

    @Test
    void testTypedToArrayWithoutNull() {
        // given
        set.addAll(Arrays.asList("alpha", "beta"));
        // when
        String[] result = set.toArray(new String[0]);
        // then
        assertEquals(2, result.length, "Typed array length must match set size");
        List<String> asList = Arrays.asList(result);
        assertTrue(asList.containsAll(Arrays.asList("alpha", "beta")), "Typed array must contain all added elements");
    }

    @Test
    void testTypedToArrayWithNull() {
        // given
        set.addAll(Arrays.asList("first", null, "last"));
        // when
        String[] result = set.toArray(new String[0]);
        // then
        assertEquals(3, result.length, "Typed array length must match set size");
        List<String> asList = Arrays.asList(result);
        assertTrue(asList.contains(null), "Typed array must contain the null element");
        assertTrue(asList.containsAll(Arrays.asList("first", "last")), "Typed array must contain the non‑null elements");
    }

    @Test
    void testNullSentinelReplacedWithNull() {
        // given – add a null (stored as NullSentinel) and two normal strings
        set.addAll(Arrays.asList(null, "a", "b"));
        // when
        Object[] array = set.toArray();
        // then – expect a real null in place of the sentinel
        assertArrayEquals(new Object[] { null, "a", "b" }, array, "NullSentinel should be replaced with a genuine null");
    }

    @Test
    void testToArrayWhenOnlyNormalElements() {
        // given
        set.addAll(Arrays.asList("x", "y"));
        // when
        Object[] array = set.toArray();
        // then
        assertArrayEquals(new Object[] { "x", "y" }, array, "Array should contain only the normal elements unchanged");
    }

    @Test
    void testToArrayWhenSetIsEmpty() {
        // when
        Object[] array = set.toArray();
        // then
        assertEquals(0, array.length, "Empty set must yield an empty array");
    }

    @Test
    void testToArrayWhenUnderlyingSetReturnsDifferentComponentType() {
        // Simulate an underlying set whose toArray returns a String[] (different component type)
        Set<Object> mockSet = mock(Set.class);
        when(mockSet.toArray()).thenReturn(new String[] { "one", "two" });
        // also make size() and iterator() behave consistently
        when(mockSet.size()).thenReturn(2);
        when(mockSet.iterator()).thenReturn(Arrays.asList("one", "two").iterator());
        assertDoesNotThrow(() -> injectInternalSet(mockSet));
        // when
        Object[] result = set.toArray();
        // then – result must be an Object[] containing the same values
        assertTrue(result instanceof Object[], "Result must be an Object[]");
        assertArrayEquals(new Object[] { "one", "two" }, result, "Values must be preserved even if underlying set returned a String[]");
    }

    @Test
    void testSnapshotImmutabilityAfterClear() {
        // given
        set.addAll(Arrays.asList("alpha", null));
        Object[] snapshot = set.toArray();
        // when – modify the set after the snapshot was taken
        set.clear();
        // then – snapshot must stay unchanged
        assertEquals(2, snapshot.length, "Snapshot length must stay the same");
        List<Object> asList = Arrays.asList(snapshot);
        assertTrue(asList.containsAll(Arrays.asList("alpha", null)), "Snapshot must retain original elements even after the set is cleared");
    }

    @Test
    void testMultipleNullEntries() {
        // given – two null values (each stored as NullSentinel)
        set.add(null);
        set.add(null);
        // when
        Object[] array = set.toArray();
        // then
        assertEquals(2, array.length, "Array length should be 2");
        assertNull(array[0], "First entry must be null");
        assertNull(array[1], "Second entry must be null");
    }

    @Test
    void testSingleNullElement() {
        // given
        set.add(null);
        // when
        Object[] array = set.toArray();
        // then
        assertArrayEquals(new Object[] { null }, array, "Array must contain exactly one null element");
    }

    @Test
    void testLargeSetSize() {
        // given – populate with 1,000 distinct integers
        for (int i = 0; i < 1000; i++) {
            set.add(i);
        }
        // when
        Object[] array = set.toArray();
        // then
        assertEquals(1000, array.length, "Array length must be 1000");
        // Verify that every integer is present (order is not guaranteed)
        Set<Integer> expected = new HashSet<>();
        for (int i = 0; i < 1000; i++) expected.add(i);
        assertTrue(expected.containsAll(Arrays.asList(array)), "Array must contain all integers 0‑999");
    }

    @Test
    void testReturnedArrayIsObjectArray() {
        // given
        set.addAll(Arrays.asList(1, "two", null));
        // when
        Object[] array = set.toArray();
        // then
        assertTrue(array instanceof Object[], "Returned array must be an Object[]");
        assertEquals(Object[].class, array.getClass(), "Runtime class of the result should be exactly Object[].class");
    }

    @Test
    void testCustomObjectPreservation() {
        // given – two distinct Person instances
        Person p1 = new Person(1, "Alice");
        Person p2 = new Person(2, "Bob");
        set.add(p1);
        set.add(p2);
        // when
        Object[] array = set.toArray();
        // then – the exact same object references must appear
        assertEquals(2, array.length, "Array length must be 2");
        assertTrue(Arrays.asList(array).contains(p1), "Array must contain the first Person instance");
        assertTrue(Arrays.asList(array).contains(p2), "Array must contain the second Person instance");
        // Reference equality checks
        for (Object o : array) {
            if (o == p1)
                assertSame(p1, o);
            if (o == p2)
                assertSame(p2, o);
        }
    }

    @Test
    void testUnderlyingSetReturnsNull() throws Exception {
        // given – a mock Set whose toArray returns null
        Set<Object> mockSet = mock(Set.class);
        when(mockSet.toArray()).thenReturn(null);
        // size/iterator are not used before the NPE, but we provide them to avoid surprises
        when(mockSet.size()).thenReturn(0);
        when(mockSet.iterator()).thenReturn(Collections.emptyIterator());
        injectInternalSet(mockSet);
        // when / then – NullPointerException should be thrown when we iterate over the null array
        assertThrows(NullPointerException.class, () -> set.toArray(), "When underlying set returns null, ConcurrentSet.toArray() must throw NPE");
    }

    @Test
    void testUnderlyingSetThrowsUnsupportedOperationException() throws Exception {
        // given – mock that throws UOE from toArray()
        Set<Object> mockSet = mock(Set.class);
        when(mockSet.toArray()).thenThrow(new UnsupportedOperationException("mock"));
        injectInternalSet(mockSet);
        // when / then
        assertThrows(UnsupportedOperationException.class, () -> set.toArray(), "Exception from underlying set should propagate unchanged");
    }

    @Test
    void testUnderlyingSetThrowsConcurrentModificationException() throws Exception {
        // given – mock that throws CME from toArray()
        Set<Object> mockSet = mock(Set.class);
        when(mockSet.toArray()).thenThrow(new ConcurrentModificationException("mock"));
        injectInternalSet(mockSet);
        // when / then
        assertThrows(ConcurrentModificationException.class, () -> set.toArray(), "ConcurrentModificationException must propagate");
    }
}
