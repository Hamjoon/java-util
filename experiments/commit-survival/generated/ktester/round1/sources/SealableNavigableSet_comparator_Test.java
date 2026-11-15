package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SealableNavigableSet_comparator_Test {

    /**
     * The set under test
     */
    private SealableNavigableSet<Integer> set;

    /**
     * Supplier that always reports the set as un‑sealed (mutable) – the comparator tests do not need sealing
     */
    private final Supplier<Boolean> sealedSupplier = () -> false;

    /**
     * A custom comparator used for the “with comparator” test case
     */
    private static final Comparator<Integer> REVERSE_ORDER = (a, b) -> b - a;

    @BeforeEach
    void setupBeforeEach() {
        // The individual test methods will decide which constructor to use,
        // so we initialise the field to null here.
        set = null;
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear the reference – helps the GC in larger suites
        set = null;
    }

    private static Stream<Class<?>> naturalOrderSetProvider() {
        return Stream.of(TreeSet.class, ConcurrentSkipListSet.class);
    }

    @Test
    void comparatorReturnsProvidedComparator() {
        // GIVEN a SealableNavigableSet created with a custom comparator
        set = new SealableNavigableSet<>(REVERSE_ORDER, sealedSupplier);
        // WHEN we request the comparator
        Comparator<? super Integer> result = set.comparator();
        // THEN the returned comparator is the same instance we supplied
        assertSame(REVERSE_ORDER, result, "The comparator() method should return the comparator supplied to the constructor");
    }

    @Test
    void comparatorReturnsNullWhenNoComparatorSupplied() {
        // GIVEN a SealableNavigableSet created with the default constructor (no comparator)
        set = new SealableNavigableSet<>(sealedSupplier);
        // WHEN we request the comparator
        Comparator<? super Integer> result = set.comparator();
        // THEN the result should be null because the underlying ConcurrentNavigableSetNullSafe
        // uses natural ordering when no comparator is provided
        assertNull(result, "The comparator() method should return null when the set was constructed without an explicit comparator");
    }

    @Test
    @DisplayName("Comparator returns null for natural ordering")
    void comparatorReturnsNullForNaturalOrdering() {
        // TreeSet with natural ordering (no explicit comparator)
        TreeSet<Integer> underlying = new TreeSet<>();
        underlying.addAll(Arrays.asList(3, 1, 2));
        set = new SealableNavigableSet<>(underlying, sealedSupplier);
        assertNull(set.comparator(), "Natural‑order sets should return null from comparator()");
    }

    @Test
    @DisplayName("Comparator returns custom comparator (String reverse order)")
    void comparatorReturnsCustomComparator_StringReverse() {
        Comparator<String> reverse = Comparator.reverseOrder();
        TreeSet<String> underlying = new TreeSet<>(reverse);
        underlying.addAll(Arrays.asList("a", "b", "c"));
        SealableNavigableSet<String> stringSet = new SealableNavigableSet<>(underlying, sealedSupplier);
        assertSame(reverse, stringSet.comparator(), "The returned comparator must be the same instance supplied to the underlying set");
    }

    @Test
    @DisplayName("Comparator delegation to mocked NavigableSet")
    void comparatorDelegatesToMockedNavigableSet() {
        // Create a mock NavigableSet<Double>
        @SuppressWarnings("unchecked")
        NavigableSet<Double> mockSet = mock(NavigableSet.class);
        Comparator<Double> mockComparator = mock(Comparator.class);
        when(mockSet.comparator()).thenReturn(mockComparator);
        // Construct the SealableNavigableSet with the mock
        SealableNavigableSet<Double> doubleSet = new SealableNavigableSet<>(mockSet, sealedSupplier);
        // Verify delegation
        assertSame(mockComparator, doubleSet.comparator(), "SealableNavigableSet should delegate comparator() to the underlying NavigableSet");
        verify(mockSet, times(1)).comparator();
    }

    @Test
    @DisplayName("Comparator call does not mutate the set")
    void comparatorCallDoesNotMutateSet() {
        TreeSet<Integer> underlying = new TreeSet<>(Arrays.asList(1, 2, 3));
        set = new SealableNavigableSet<>(underlying, sealedSupplier);
        int sizeBefore = set.size();
        // method under test
        set.comparator();
        int sizeAfter = set.size();
        assertEquals(3, sizeBefore, "Pre‑condition: size should be 3");
        assertEquals(sizeBefore, sizeAfter, "Calling comparator() must not change the size of the set");
    }

    @Test
    @DisplayName("Comparator identity across multiple invocations")
    void comparatorIdentityAcrossMultipleInvocations() {
        Comparator<String> reverse = Comparator.reverseOrder();
        TreeSet<String> underlying = new TreeSet<>(reverse);
        underlying.addAll(Arrays.asList("x", "y", "z"));
        SealableNavigableSet<String> stringSet = new SealableNavigableSet<>(underlying, sealedSupplier);
        Comparator<? super String> first = stringSet.comparator();
        Comparator<? super String> second = stringSet.comparator();
        assertSame(first, second, "Multiple calls to comparator() should return the identical instance");
    }

    @Test
    @DisplayName("Comparator with ConcurrentSkipListSet custom comparator")
    void comparatorWithConcurrentSkipListSetCustomComparator() {
        Comparator<Integer> mod5Comparator = Comparator.comparingInt(i -> i % 5);
        ConcurrentSkipListSet<Integer> underlying = new ConcurrentSkipListSet<>(mod5Comparator);
        underlying.addAll(Arrays.asList(7, 12, 3));
        SealableNavigableSet<Integer> intSet = new SealableNavigableSet<>(underlying, sealedSupplier);
        assertSame(mod5Comparator, intSet.comparator(), "The comparator returned must be the exact instance supplied to the ConcurrentSkipListSet");
    }

    @Test
    @DisplayName("Comparator null for empty natural‑order set")
    void comparatorNullForEmptyNaturalOrderSet() {
        // empty, natural order
        TreeSet<Long> underlying = new TreeSet<>();
        SealableNavigableSet<Long> longSet = new SealableNavigableSet<>(underlying, sealedSupplier);
        assertNull(longSet.comparator(), "Even an empty natural‑order set should return null from comparator()");
    }

    @Test
    @DisplayName("Comparator unchanged after adding elements")
    void comparatorUnchangedAfterAddingElements() {
        Comparator<Double> reversed = Comparator.naturalOrder().reversed();
        TreeSet<Double> underlying = new TreeSet<>(reversed);
        underlying.addAll(Arrays.asList(1.1, 2.2));
        SealableNavigableSet<Double> doubleSet = new SealableNavigableSet<>(underlying, sealedSupplier);
        Comparator<? super Double> before = doubleSet.comparator();
        // mutate through the SealableNavigableSet API
        doubleSet.add(3.3);
        Comparator<? super Double> after = doubleSet.comparator();
        assertSame(before, after, "Adding elements must not cause comparator() to return a new instance");
    }

    @Test
    @DisplayName("Comparator delegation through unmodifiable wrapper")
    void comparatorDelegationThroughUnmodifiableWrapper() {
        Comparator<String> lengthComparator = Comparator.comparingInt(String::length);
        TreeSet<String> baseSet = new TreeSet<>(lengthComparator);
        baseSet.addAll(Arrays.asList("aa", "bbb", "c"));
        NavigableSet<String> unmodifiable = Collections.unmodifiableNavigableSet(baseSet);
        SealableNavigableSet<String> wrapped = new SealableNavigableSet<>(unmodifiable, sealedSupplier);
        assertSame(lengthComparator, wrapped.comparator(), "SealableNavigableSet should correctly delegate through an unmodifiableNavigableSet wrapper");
    }

    // Example of a parameterised test that could cover several natural‑order cases.
    @ParameterizedTest(name = "[{index}] natural order set of type {0}")
    @MethodSource("naturalOrderSetProvider")
    void comparatorReturnsNullForVariousNaturalOrderSets(Class<?> setClass) {
        NavigableSet<?> underlying;
        if (setClass == TreeSet.class) {
            underlying = new TreeSet<>();
        } else if (setClass == ConcurrentSkipListSet.class) {
            underlying = new ConcurrentSkipListSet<>();
        } else {
            fail("Unsupported set class: " + setClass);
            return;
        }
        //noinspection unchecked
        SealableNavigableSet<?> sealable = new SealableNavigableSet<>(underlying, sealedSupplier);
        assertNull(sealable.comparator(), "Natural‑order " + setClass.getSimpleName() + " should return null from comparator()");
    }
}
