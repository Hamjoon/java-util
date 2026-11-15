package com.cedarsoftware.util;

import java.util.function.Supplier;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class SealableSet_addAll_Test {

    private SealableSet<Integer> set;

    private volatile boolean sealed = false;

    private Supplier<Boolean> sealedSupplier = () -> sealed;

    @BeforeEach
    void setupBeforeEach() {
        // By default we wrap a mutable HashSet so that normal operations succeed.
        set = new SealableSet<>(sealedSupplier);
        set.add("alpha");
        set.add("beta");
        // baseline contains a null element
        set.add(null);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the sealed flag to ensure isolation between tests
        sealed = false;
    }

    /**
     * Replaces the private {@code set} field of the current {@link SealableSet}
     * instance with the supplied delegate.  Used to simulate unmodifiable or
     * type‑restricted underlying sets.
     */
    private void replaceDelegateSet(Set<String> delegate) {
        try {
            Field f = SealableSet.class.getDeclaredField("set");
            f.setAccessible(true);
            f.set(set, delegate);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ---------- group: addAll - normal operation (unsealed) ----------
    static Stream<Arguments> normalOperationProvider() {
        return Stream.of(Arguments.of(Arrays.asList("gamma", "delta"), true, "Set should change and contain the new elements"), Arguments.of(Collections.singletonList("alpha"), false, "All elements already present – set unchanged"), Arguments.of(Collections.emptyList(), false, "Empty collection – no modification"));
    }

    // ---------- group: addAll - sealed behavior ----------
    static Stream<Arguments> sealedBehaviorProvider() {
        return Stream.of(Arguments.of(Arrays.asList("gamma", "delta")), Arguments.of(Collections.emptyList()));
    }

    // ---------- group: addAll - collection with null element ----------
    static Stream<Arguments> nullElementProvider() {
        return Stream.of(Arguments.of(Arrays.asList((String) null, "epsilon"), true, "Adding null (already present) + new element should change set"), Arguments.of(Collections.singletonList((String) null), false, "Adding only null when already present should not modify set"));
    }

    @Test
    void testAddAllAddsNewElementsAndReturnsTrue() {
        List<Integer> toAdd = Arrays.asList(30, 40);
        // The original framework used Integer; we keep it as a sanity check.
        SealableSet<Integer> intSet = new SealableSet<>(sealedSupplier);
        intSet.add(10);
        intSet.add(20);
        intSet.add(null);
        assertTrue(intSet.addAll(toAdd), "addAll should return true when the set changes");
        assertTrue(intSet.contains(30));
        assertTrue(intSet.contains(40));
        assertTrue(intSet.contains(10));
        assertTrue(intSet.contains(20));
        assertTrue(intSet.contains(null));
    }

    @Test
    void testAddAllReturnsFalseWhenNoNewElements() {
        List<Integer> duplicate = Arrays.asList(10, null, 20);
        SealableSet<Integer> intSet = new SealableSet<>(sealedSupplier);
        intSet.add(10);
        intSet.add(20);
        intSet.add(null);
        assertFalse(intSet.addAll(duplicate), "addAll should return false when nothing is added");
        assertEquals(3, intSet.size());
    }

    @Test
    void testAddAllWithMixedExistingAndNewElements() {
        List<Integer> mixed = Arrays.asList(20, 30, null, 40);
        assertTrue(set.addAll(mixed), "addAll should return true when at least one new element is added");
        assertTrue(set.contains(30));
        assertTrue(set.contains(40));
        // Existing elements must still be present
        assertTrue(set.contains(10));
        assertTrue(set.contains(20));
        assertTrue(set.contains(null));
        // Size should reflect only unique elements
        assertEquals(5, set.size());
    }

    @Test
    void testAddAllWhenSealedThrowsUnsupportedOperationException() {
        sealed = true;
        List<Integer> toAdd = Arrays.asList(50, 60);
        SealableSet<Integer> intSet = new SealableSet<>(sealedSupplier);
        intSet.add(10);
        intSet.add(20);
        intSet.add(null);
        // The original test expected UnsupportedOperationException; keep it for backward compatibility.
        assertThrows(UnsupportedOperationException.class, () -> intSet.addAll(toAdd));
        assertEquals(3, intSet.size());
        assertFalse(intSet.contains(50));
        assertFalse(intSet.contains(60));
    }

    @Test
    void testAddAllNullCollectionThrowsNullPointerException() {
        SealableSet<Integer> intSet = new SealableSet<>(sealedSupplier);
        intSet.add(1);
        assertThrows(NullPointerException.class, () -> intSet.addAll(null));
    }

    @ParameterizedTest(name = "[{index}] addAll normal: {2}")
    @MethodSource("normalOperationProvider")
    void testAddAllNormalOperation(List<String> col, boolean expectedResult, String description) {
        assertEquals(expectedResult, set.addAll(col), description);
        // Verify that all supplied elements are now present when expectedResult is true
        if (expectedResult) {
            col.forEach(e -> assertTrue(set.contains(e), "Set should contain added element: " + e));
        }
        // Baseline elements must always remain
        assertTrue(set.contains("alpha"));
        assertTrue(set.contains("beta"));
    }

    @ParameterizedTest(name = "[{index}] addAll when sealed should throw IllegalStateException")
    @MethodSource("sealedBehaviorProvider")
    void testAddAllWhenSealedThrows(List<String> col) {
        // put the set into sealed (immutable) mode
        sealed = true;
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> set.addAll(col), "Sealed set must reject addAll");
        // Ensure the underlying set has not been modified
        assertEquals(3, set.size());
    }

    // ---------- group: addAll - null collection ----------
    @Test
    void testAddAllNullCollectionThrows() {
        assertThrows(NullPointerException.class, () -> set.addAll(null), "Passing null collection must throw NPE");
    }

    @ParameterizedTest(name = "[{index}] addAll with null element: {2}")
    @MethodSource("nullElementProvider")
    void testAddAllWithNullElement(List<String> col, boolean expectedResult, String description) {
        assertEquals(expectedResult, set.addAll(col), description);
        if (expectedResult) {
            col.forEach(e -> assertTrue(set.contains(e), "Set should contain: " + e));
        }
        // baseline still present
        assertTrue(set.contains("alpha"));
        assertTrue(set.contains("beta"));
    }

    // ---------- group: addAll - mixed existing and new elements ----------
    @Test
    void testAddAllMixedExistingAndNew() {
        List<String> mixed = Arrays.asList("alpha", "zeta", "eta");
        assertTrue(set.addAll(mixed), "At least one new element (zeta, eta) should cause true");
        assertTrue(set.contains("zeta"));
        assertTrue(set.contains("eta"));
        // existing elements remain
        assertTrue(set.contains("alpha"));
        assertTrue(set.contains("beta"));
    }

    // ---------- group: addAll - collection with duplicate elements ----------
    @Test
    void testAddAllWithDuplicateElements() {
        List<String> dup = Arrays.asList("theta", "theta", "iota");
        assertTrue(set.addAll(dup), "Duplicates are ignored; new elements cause true");
        assertTrue(set.contains("theta"));
        assertTrue(set.contains("iota"));
        // size reflects unique entries only
        // alpha, beta, null, theta, iota
        assertEquals(5, set.size());
    }

    @Test
    void testAddAllWithOnlyDuplicateOfSingleNewElement() {
        List<String> dupSingle = Arrays.asList("kappa", "kappa");
        assertTrue(set.addAll(dupSingle), "Single new element added once, returns true");
        assertTrue(set.contains("kappa"));
        // alpha, beta, null, kappa
        assertEquals(4, set.size());
    }

    // ---------- group: addAll - unmodifiable collection ----------
    @Test
    void testAddAllWithUnmodifiableCollection() {
        List<String> modifiable = Arrays.asList("lambda", "mu");
        List<String> unmodifiable = Collections.unmodifiableList(modifiable);
        assertTrue(set.addAll(unmodifiable), "Should accept read‑only collections");
        assertTrue(set.contains("lambda"));
        assertTrue(set.contains("mu"));
    }

    // ---------- group: addAll - underlying set unmodifiable ----------
    @Test
    void testAddAllWhenUnderlyingSetIsUnmodifiable() {
        // Replace delegate with an unmodifiable set that initially contains baseline elements
        Set<String> delegate = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("alpha", "beta", null)));
        replaceDelegateSet(delegate);
        // Adding any new element should propagate UnsupportedOperationException from the delegate
        List<String> toAdd = Arrays.asList("a", "b");
        assertThrows(UnsupportedOperationException.class, () -> set.addAll(toAdd), "Delegating to an unmodifiable set must propagate UOE");
    }

    // ---------- group: addAll - incompatible element type ----------
    @Test
    @SuppressWarnings("unchecked")
    void testAddAllWithIncompatibleElementType() {
        // The SealableSet is typed as String, but we pass a collection containing an Integer.
        List mixed = Arrays.asList(123, "xyz");
        assertThrows(ClassCastException.class, () -> set.addAll(mixed), "Adding an element of the wrong type must cause CCE");
    }

    // ---------- group: addAll - null element not allowed by underlying set ----------
    @Test
    void testAddAllNullElementWhenUnderlyingSetDisallowsNull() {
        // TreeSet does not permit null (throws NPE on add)
        Set<String> treeSet = new TreeSet<>();
        // Populate with baseline non‑null elements only
        treeSet.add("alpha");
        treeSet.add("beta");
        replaceDelegateSet(treeSet);
        List<String> containsNull = Collections.singletonList((String) null);
        assertThrows(NullPointerException.class, () -> set.addAll(containsNull), "Underlying set that rejects null must propagate NPE");
    }
}
