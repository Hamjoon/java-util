package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SealableNavigableSet_last_Test {

    private SealableNavigableSet<Integer> set;

    private volatile boolean sealedState = false;

    private Supplier<Boolean> sealedSupplier = () -> sealedState;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh set for each test and populate it with a known ordering
        set = new SealableNavigableSet<>(sealedSupplier);
        set.add(10);
        set.add(20);
        set.add(30);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the sealed flag to its default value to avoid cross‑test interference
        sealedState = false;
    }

    /**
     * Provides a stream of arguments: a supplier that creates a configured
     * {@link SealableNavigableSet}, the expected result, and a description.
     */
    private static Stream<Arguments> normalLastProvider() {
        return Stream.of(// empty underlying set – TreeSet throws NoSuchElementException,
        // but we simulate a set that returns null via a mock.
        Arguments.of((Supplier<SealableNavigableSet<Integer>>) () -> {
            @SuppressWarnings("unchecked")
            NavigableSet<Integer> mockNav = mock(NavigableSet.class);
            when(mockNav.last()).thenReturn(null);
            SealableNavigableSet<Integer> s = new SealableNavigableSet<>(() -> false);
            injectNavSet(s, mockNav);
            return s;
        }, null, "Empty underlying set returns null"), // regular set {1,3,5,7} – expect 7
        Arguments.of((Supplier<SealableNavigableSet<Integer>>) () -> {
            SealableNavigableSet<Integer> s = new SealableNavigableSet<>(() -> false);
            s.addAll(Arrays.asList(1, 3, 5, 7));
            return s;
        }, 7, "Standard set returns greatest element"), // single element {42}
        Arguments.of((Supplier<SealableNavigableSet<Integer>>) () -> {
            SealableNavigableSet<Integer> s = new SealableNavigableSet<>(() -> false);
            s.add(42);
            return s;
        }, 42, "Single‑element set returns that element"), // reverse comparator with {1,2,3} – expect 1
        Arguments.of((Supplier<SealableNavigableSet<Integer>>) () -> {
            SealableNavigableSet<Integer> s = new SealableNavigableSet<>(Comparator.reverseOrder(), () -> false);
            s.addAll(Arrays.asList(1, 2, 3));
            return s;
        }, 1, "Reverse order comparator returns logical last"), // ConcurrentSkipListSet underlying implementation
        Arguments.of((Supplier<SealableNavigableSet<Integer>>) () -> {
            // Build a SealableNavigableSet that delegates to a ConcurrentSkipListSet
            SealableNavigableSet<Integer> s = new SealableNavigableSet<>(() -> false);
            // Replace the internal navSet via reflection
            ConcurrentSkipListSet<Integer> concurrentSet = new ConcurrentSkipListSet<>();
            concurrentSet.addAll(Arrays.asList(5, 10, 15));
            injectNavSet(s, concurrentSet);
            return s;
        }, 15, "ConcurrentSkipListSet returns highest element"));
    }

    /* ---------------------------------------------------------------------- */
    private static void injectNavSet(SealableNavigableSet<?> target, NavigableSet<?> navSet) {
        try {
            Field f = SealableNavigableSet.class.getDeclaredField("navSet");
            f.setAccessible(true);
            f.set(target, navSet);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to inject mock navSet", e);
        }
    }

    @Test
    void testLastReturnsHighestElement() {
        // The natural ordering of Integer makes 30 the greatest element
        assertEquals(30, set.last());
    }

    @Test
    void testLastWhenSealedDoesNotAffectResult() {
        // Switching to a sealed state should not change read‑only operations
        sealedState = true;
        assertEquals(30, set.last());
    }

    @Test
    void testLastOnEmptySetThrowsNoSuchElementException() {
        // An empty SealableNavigableSet should forward the exception from the underlying set
        SealableNavigableSet<Integer> emptySet = new SealableNavigableSet<>(sealedSupplier);
        assertThrows(NoSuchElementException.class, emptySet::last);
    }

    @Test
    void testLastWithCustomComparator() {
        // Using a reverse order comparator, the "greatest" element according to the comparator
        // is the smallest natural value.
        SealableNavigableSet<Integer> reverseSet = new SealableNavigableSet<>(Comparator.reverseOrder(), sealedSupplier);
        reverseSet.add(5);
        reverseSet.add(15);
        reverseSet.add(25);
        // In reverse order the elements are [25, 15, 5]; last() should return 5
        assertEquals(5, reverseSet.last());
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("normalLastProvider")
    void testLastNormalScenarios(Supplier<SealableNavigableSet<Integer>> setSupplier, Object expected, String description) {
        SealableNavigableSet<Integer> s = setSupplier.get();
        if (expected == null) {
            assertNull(s.last(), description);
        } else {
            assertEquals(expected, s.last(), description);
        }
    }

    @Test
    void testLastPropagatesNoSuchElementExceptionFromUnderlyingSet() {
        // Mock a NavigableSet that throws NoSuchElementException on last()
        @SuppressWarnings("unchecked")
        NavigableSet<Integer> mockNav = mock(NavigableSet.class);
        when(mockNav.last()).thenThrow(new NoSuchElementException("empty"));
        SealableNavigableSet<Integer> s = new SealableNavigableSet<>(() -> false);
        injectNavSet(s, mockNav);
        assertThrows(NoSuchElementException.class, s::last);
    }

    @Test
    void testLastPropagatesIllegalStateExceptionFromMock() {
        // Mock a NavigableSet that throws IllegalStateException on last()
        @SuppressWarnings("unchecked")
        NavigableSet<Integer> mockNav = mock(NavigableSet.class);
        when(mockNav.last()).thenThrow(new IllegalStateException("illegal state"));
        SealableNavigableSet<Integer> s = new SealableNavigableSet<>(() -> false);
        injectNavSet(s, mockNav);
        assertThrows(IllegalStateException.class, s::last);
    }

    @Test
    void testLastDoesNotModifySetSize() {
        SealableNavigableSet<Integer> s = new SealableNavigableSet<>(() -> false);
        s.addAll(Arrays.asList(5, 10, 15));
        int before = s.size();
        // invoke the method under test
        s.last();
        int after = s.size();
        assertEquals(before, after, "Calling last() must not change the set size");
    }

    @Test
    void testLastWhenInternalNavSetIsNullThrowsNullPointerException() throws Exception {
        SealableNavigableSet<Integer> s = new SealableNavigableSet<>(() -> false);
        // Force the private field to null via reflection
        Field f = SealableNavigableSet.class.getDeclaredField("navSet");
        f.setAccessible(true);
        f.set(s, null);
        assertThrows(NullPointerException.class, s::last);
    }

    @Test
    void testLastThrowsClassCastExceptionWhenElementTypeMismatches() throws Exception {
        // Underlying set holds Strings, but we treat the wrapper as Integer
        NavigableSet<String> stringSet = new TreeSet<>();
        stringSet.addAll(Arrays.asList("a", "b", "c"));
        SealableNavigableSet<Integer> s = new SealableNavigableSet<>(() -> false);
        // Inject the mismatched set
        @SuppressWarnings("unchecked")
        NavigableSet<Integer> raw = (NavigableSet<Integer>) (NavigableSet<?>) stringSet;
        injectNavSet(s, raw);
        // The cast inside last() should fail
        assertThrows(ClassCastException.class, s::last);
    }

    @Test
    void testLastPropagatesConcurrentModificationException() {
        // Mock a NavigableSet that throws CME during last()
        @SuppressWarnings("unchecked")
        NavigableSet<Integer> mockNav = mock(NavigableSet.class);
        when(mockNav.last()).thenThrow(new ConcurrentModificationException("concurrent"));
        SealableNavigableSet<Integer> s = new SealableNavigableSet<>(() -> false);
        injectNavSet(s, mockNav);
        assertThrows(ConcurrentModificationException.class, s::last);
    }
}
