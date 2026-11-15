package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableSetNullSafe_pollLast_Test {

    private NavigableSet<String> set;

    @BeforeEach
    void setupBeforeEach() {
        // Use the default (natural ordering) constructor for most tests.
        set = new ConcurrentNavigableSetNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the set to avoid side‑effects between tests.
        set.clear();
    }

    private static Stream<Arguments> pollLastSimpleScenarios() {
        return Stream.of(// empty set → null
        Arguments.of("When the set is empty, pollLast() should return null.", (Consumer<NavigableSet<String>>) s -> {
        }, /* nothing */
        null), // single non‑null element
        Arguments.of("Set contains a single non‑null element \"apple\"; pollLast() should return that element.", (Consumer<NavigableSet<String>>) s -> s.add("apple"), "apple"), // several elements – natural ordering
        Arguments.of("Set contains several elements {\"banana\", \"orange\", \"apple\"}; pollLast() should return the highest element according to natural ordering, i.e., \"orange\".", (Consumer<NavigableSet<String>>) s -> {
            s.add("banana");
            s.add("orange");
            s.add("apple");
        }, "orange"), // only a null element
        Arguments.of("Set contains only a null element (added via the null‑safe add method). pollLast() should return null after unmasking the internal sentinel.", (Consumer<NavigableSet<String>>) s -> s.add(null), null), // null + other elements – highest non‑null
        Arguments.of("Set contains a null element and other elements {\"pear\", \"grape\"}. pollLast() should return the highest non‑null element (\"pear\"), not the masked null.", (Consumer<NavigableSet<String>>) s -> {
            s.add(null);
            s.add("pear");
            s.add("grape");
        }, "pear"));
    }

    /**
     * Replaces the private {@code internalSet} field of a {@link ConcurrentNavigableSetNullSafe}
     * instance with the supplied mock.
     */
    private static void setInternalSet(ConcurrentNavigableSetNullSafe<?> instance, NavigableSet<Object> newSet) throws Exception {
        Field f = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        f.setAccessible(true);
        f.set(instance, newSet);
    }

    @Test
    void pollLast_onEmptySet_returnsNull() {
        assertTrue(set.isEmpty(), "Set should be empty before pollLast");
        assertNull(set.pollLast(), "pollLast on an empty set must return null");
    }

    @Test
    void pollLast_naturalOrder_returnsHighestElementIncludingNull() {
        // natural order: apple < banana < cherry < null (null is masked as the sentinel and sorts last)
        assertTrue(set.add("apple"));
        assertTrue(set.add("banana"));
        assertTrue(set.add("cherry"));
        assertTrue(set.add(null));
        assertEquals(4, set.size());
        // The highest element according to natural ordering is the null element.
        assertNull(set.pollLast(), "The highest element (null) should be returned as null");
        assertEquals(3, set.size());
        assertFalse(set.contains(null), "Null should have been removed after pollLast");
        assertTrue(set.contains("cherry"));
    }

    @Test
    void pollLast_naturalOrder_subsequentCalls() {
        assertTrue(set.add("apple"));
        assertTrue(set.add("banana"));
        assertTrue(set.add("cherry"));
        assertTrue(set.add(null));
        // First pollLast removes null
        assertNull(set.pollLast());
        // Second pollLast removes "cherry"
        assertEquals("cherry", set.pollLast());
        // Third pollLast removes "banana"
        assertEquals("banana", set.pollLast());
        // Fourth pollLast removes "apple"
        assertEquals("apple", set.pollLast());
        // Now empty again
        assertNull(set.pollLast());
        assertTrue(set.isEmpty());
    }

    @Test
    void pollLast_withLengthComparator_returnsHighestByLength() {
        // Replace the set with a comparator‑based instance
        Comparator<String> lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        set = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
        // length 4
        assertTrue(set.add("kiwi"));
        // length 5
        assertTrue(set.add("apple"));
        // length 6
        assertTrue(set.add("banana"));
        // length 0 (sentinel)
        assertTrue(set.add(null));
        assertEquals(4, set.size());
        // Highest by length is "banana"
        assertEquals("banana", set.pollLast());
        assertEquals(3, set.size());
        assertFalse(set.contains("banana"));
        // Next highest is "apple"
        assertEquals("apple", set.pollLast());
        assertEquals(2, set.size());
        // Next highest is "kiwi"
        assertEquals("kiwi", set.pollLast());
        assertEquals(1, set.size());
        // Finally the null sentinel
        assertNull(set.pollLast());
        assertTrue(set.isEmpty());
    }

    @Test
    void pollLast_removesElementFromSet() {
        assertTrue(set.add("alpha"));
        assertTrue(set.add("beta"));
        assertTrue(set.add(null));
        // pollLast should remove null (the highest element)
        assertNull(set.pollLast());
        assertFalse(set.contains(null));
        assertEquals(2, set.size());
        // pollLast now removes "beta"
        assertEquals("beta", set.pollLast());
        assertFalse(set.contains("beta"));
        assertEquals(1, set.size());
        // Remaining element
        assertEquals("alpha", set.pollLast());
        assertTrue(set.isEmpty());
    }

    /**
     * Parameterised tests for the simple pollLast scenarios described in the JSON.
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("pollLastSimpleScenarios")
    void pollLast_simpleScenarios(String description, Consumer<NavigableSet<String>> setup, Object expected) {
        // Re‑initialise the set for each scenario (natural ordering)
        set = new ConcurrentNavigableSetNullSafe<>();
        setup.accept(set);
        Object actual = set.pollLast();
        assertEquals(expected, actual, description);
    }

    /**
     * Test that verifies pollLast removes the highest element and that a subsequent call
     * returns the next highest element.
     */
    @Test
    void pollLast_removal_multipleCalls() {
        // {"apple", "pear", "grape"}
        assertTrue(set.add("apple"));
        assertTrue(set.add("pear"));
        assertTrue(set.add("grape"));
        // First call – highest element according to natural order is "pear"
        assertEquals("pear", set.pollLast(), "First pollLast should return the highest element");
        assertFalse(set.contains("pear"));
        // Second call – now highest is "grape"
        assertEquals("grape", set.pollLast(), "Second pollLast should return the next highest element");
    }

    /**
     * Custom comparator – reverse order. The highest element according to the comparator
     * is the smallest natural element.
     */
    @Test
    void pollLast_customComparator_reverseOrder() {
        Comparator<String> reverse = Comparator.reverseOrder();
        set = new ConcurrentNavigableSetNullSafe<>(reverse);
        assertTrue(set.add("a"));
        assertTrue(set.add("b"));
        assertTrue(set.add("c"));
        // In reverse order, "a" is considered the highest.
        assertEquals("a", set.pollLast(), "With reverse order comparator, pollLast should return \"a\"");
    }

    /**
     * Integer elements – natural ordering.
     */
    @Test
    void pollLast_integerElements() {
        NavigableSet<Integer> intSet = new ConcurrentNavigableSetNullSafe<>();
        assertTrue(intSet.add(1));
        assertTrue(intSet.add(2));
        assertTrue(intSet.add(3));
        assertEquals(3, intSet.pollLast(), "The greatest integer (3) should be returned");
    }

    /**
     * Adding the same element twice does not create duplicates.
     */
    @Test
    void pollLast_duplicateAdd() {
        assertTrue(set.add("dup"));
        // second add returns false because the element already exists
        assertFalse(set.add("dup"));
        assertEquals(1, set.size(), "Set size should be 1 after duplicate add");
        assertEquals("dup", set.pollLast(), "pollLast should return the sole element");
        assertTrue(set.isEmpty(), "Set should be empty after polling the only element");
    }

    /**
     * Null element together with other strings – the highest non‑null element should be returned.
     */
    @Test
    void pollLast_nullAndMaxElement() {
        assertTrue(set.add(null));
        assertTrue(set.add("zebra"));
        assertTrue(set.add("yak"));
        assertTrue(set.add("xenon"));
        // According to the test description, the null sentinel must be ignored.
        assertEquals("zebra", set.pollLast(), "Highest non‑null element should be returned");
        // Ensure the null sentinel is still present after the call.
        assertTrue(set.contains(null), "Null sentinel should remain in the set");
    }

    /**
     * Reflection based test: inject a masked‑null sentinel directly as the result of the
     * internal set's {@code pollLast()} and verify that {@code pollLast()} un‑masks it to {@code null}.
     */
    @Test
    void pollLast_reflectionInjectMaskedNullSentinel() throws Exception {
        // 1️⃣ Create a real instance and add a null to obtain the internal sentinel object.
        ConcurrentNavigableSetNullSafe<String> realInstance = new ConcurrentNavigableSetNullSafe<>();
        realInstance.add(null);
        // this is the masked‑null sentinel
        Object sentinel = getInternalSet(realInstance).pollLast();
        // 2️⃣ Create a fresh instance whose internalSet we will replace with a mock.
        ConcurrentNavigableSetNullSafe<String> testInstance = new ConcurrentNavigableSetNullSafe<>();
        @SuppressWarnings("unchecked")
        NavigableSet<Object> mockInternal = mock(NavigableSet.class);
        // Stub the mock to return the sentinel when pollLast() is called.
        when(mockInternal.pollLast()).thenReturn(sentinel);
        // 3️⃣ Inject the mock via reflection.
        setInternalSet(testInstance, mockInternal);
        // 4️⃣ Verify that pollLast() correctly unmasks the sentinel to null.
        assertNull(testInstance.pollLast(), "pollLast should unmask the sentinel and return null");
        // Verify that the mock's pollLast() was indeed invoked.
        verify(mockInternal, times(1)).pollLast();
    }

    /**
     * Retrieves the private {@code internalSet} field from a {@link ConcurrentNavigableSetNullSafe} instance.
     */
    @SuppressWarnings("unchecked")
    private static NavigableSet<Object> getInternalSet(ConcurrentNavigableSetNullSafe<?> instance) throws Exception {
        Field f = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        f.setAccessible(true);
        return (NavigableSet<Object>) f.get(instance);
    }
}
