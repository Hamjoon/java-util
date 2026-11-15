package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SealableNavigableSet_add_Test {

    private SealableNavigableSet<Integer> set;

    /**
     * mutable flag that the supplier reads – allows us to toggle sealed state during a test
     */
    private final AtomicBoolean sealedFlag = new AtomicBoolean(false);

    private final Supplier<Boolean> sealedSupplier = sealedFlag::get;

    /* --------------------------------------------------------------
     *  Helper class for parameterised arguments
     * -------------------------------------------------------------- */
    private static final class ArgumentsHolder {

        final String input;

        final Boolean expectedBoolean;

        final Class<? extends Throwable> expectedException;

        final String description;

        ArgumentsHolder(String input, boolean expected, String description) {
            this.input = input;
            this.expectedBoolean = expected;
            this.expectedException = null;
            this.description = description;
        }

        ArgumentsHolder(String input, Class<? extends Throwable> exception, String description) {
            this.input = input;
            this.expectedBoolean = null;
            this.expectedException = exception;
            this.description = description;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Start each test with an *unsealed* set containing three distinct integers.
        sealedFlag.set(false);
        set = new SealableNavigableSet<>(sealedSupplier);
        set.add(10);
        set.add(20);
        set.add(30);
    }

    @AfterEach
    void teardownAfterEach() {
        // help GC – not strictly required but keeps the pattern consistent
        set = null;
    }

    private static Stream<ArgumentsHolder> normalUnsealedProvider() {
        return Stream.of(new ArgumentsHolder("alpha", true, "Adding a brand‑new element should return true."), new ArgumentsHolder("beta", false, "Adding an existing element should return false."), new ArgumentsHolder(null, NullPointerException.class, "TreeSet does not allow null – should propagate NPE."));
    }

    @Test
    void addWhenUnsealedAddsElementAndReturnsTrue() {
        assertFalse(set.contains(40), "Pre‑condition: set should not contain 40");
        boolean added = set.add(40);
        assertTrue(added, "add should return true for a new element");
        assertTrue(set.contains(40), "Element 40 must be present after add");
        assertEquals(4, set.size(), "Size must increase by one");
    }

    @Test
    void addDuplicateWhenUnsealedReturnsFalseAndDoesNotChangeSize() {
        assertTrue(set.contains(20), "Pre‑condition: set should contain 20");
        int originalSize = set.size();
        boolean added = set.add(20);
        assertFalse(added, "add should return false when element already exists");
        assertEquals(originalSize, set.size(), "Size must stay the same after duplicate add");
    }

    @Test
    void addWhenSealedThrowsUnsupportedOperationException() {
        // seal the set
        sealedFlag.set(true);
        assertThrows(UnsupportedOperationException.class, () -> set.add(50), "add must throw when the set is sealed");
    }

    @Test
    void addNullWhenUnsealedIsAllowed() {
        // The underlying implementation permits null values.
        assertFalse(set.contains(null), "Pre‑condition: set should not contain null");
        boolean added = set.add(null);
        assertTrue(added, "add should return true for a new null element");
        assertTrue(set.contains(null), "Null must be present after add");
        assertEquals(4, set.size(), "Size must increase by one after adding null");
    }

    @Test
    void addNullWhenSealedThrowsUnsupportedOperationException() {
        sealedFlag.set(true);
        assertThrows(UnsupportedOperationException.class, () -> set.add(null), "Adding null while sealed must also throw");
    }

    @Test
    void sealedStateCanBeToggledDuringLifetime() {
        // initially unsealed – add works
        assertDoesNotThrow(() -> set.add(55));
        assertTrue(set.contains(55));
        // seal – further adds fail
        sealedFlag.set(true);
        assertThrows(UnsupportedOperationException.class, () -> set.add(66));
        // unseal again – adds succeed
        sealedFlag.set(false);
        assertDoesNotThrow(() -> set.add(66));
        assertTrue(set.contains(66));
    }

    /**
     * Replaces the private {@code navSet} field of a {@link SealableNavigableSet}
     * with a user‑provided {@link NavigableSet}.  This allows us to inject
     * specialised underlying collections (e.g., unmodifiable, custom comparator)
     * without altering the production code.
     */
    @SuppressWarnings("unchecked")
    private static <E> SealableNavigableSet<E> withUnderlyingSet(SealableNavigableSet<E> wrapper, NavigableSet<E> underlying) throws ReflectiveOperationException {
        Field navSetField = SealableNavigableSet.class.getDeclaredField("navSet");
        navSetField.setAccessible(true);
        navSetField.set(wrapper, underlying);
        return wrapper;
    }

    @ParameterizedTest(name = "[{index}] add({0}) → {1}")
    @MethodSource("normalUnsealedProvider")
    void addNormalUnsealed(ArgumentsHolder args) throws Exception {
        // Use a plain TreeSet (which throws NPE for null) as the underlying collection.
        NavigableSet<String> underlying = new TreeSet<>();
        // Pre‑populate with "beta" to test the duplicate case.
        underlying.add("beta");
        // Build a SealableNavigableSet<String> wrapping the underlying set.
        SealableNavigableSet<String> stringSet = withUnderlyingSet(new SealableNavigableSet<>(sealedSupplier), underlying);
        // Ensure the set is *unsealed*.
        sealedFlag.set(false);
        if (args.expectedException != null) {
            assertThrows(args.expectedException, () -> stringSet.add(args.input), args.description);
        } else {
            boolean result = stringSet.add(args.input);
            assertEquals(args.expectedBoolean, result, args.description);
            // Verify side‑effect for successful addition.
            if (result) {
                assertTrue(stringSet.contains(args.input));
            }
        }
    }

    @Test
    void addWhenSealedThrowsIllegalStateException() {
        // seal the set
        sealedFlag.set(true);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> set.add(50), "add must throw IllegalStateException when the set is sealed");
        assertNotNull(ex.getMessage());
    }

    @Test
    void addNullWhenSealedThrowsIllegalStateException() {
        sealedFlag.set(true);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> set.add(null), "Adding null while sealed must also throw IllegalStateException");
        assertNotNull(ex.getMessage());
    }

    @Test
    void addWithWrongTypePropagatesClassCastException() {
        // Underlying set is a TreeSet<Integer>
        NavigableSet<Integer> underlying = new TreeSet<>();
        SealableNavigableSet<Integer> intSet = null;
        try {
            intSet = withUnderlyingSet(new SealableNavigableSet<>(sealedSupplier), underlying);
        } catch (ReflectiveOperationException e) {
            fail("Reflection failed while injecting underlying set", e);
        }
        // The set is unsealed – attempt to add a String via raw cast.
        assertThrows(ClassCastException.class, () -> ((SealableNavigableSet) intSet).add("stringElement"), "Adding a String to a TreeSet<Integer> must throw ClassCastException");
    }

    @Test
    void addWhenComparatorRejectsElementThrowsIllegalArgumentException() {
        Comparator<String> rejector = (a, b) -> {
            if ("bad".equals(a) || "bad".equals(b)) {
                throw new IllegalArgumentException("value not allowed");
            }
            return a.compareTo(b);
        };
        NavigableSet<String> underlying = new TreeSet<>(rejector);
        SealableNavigableSet<String> stringSet = null;
        try {
            stringSet = withUnderlyingSet(new SealableNavigableSet<>(sealedSupplier), underlying);
        } catch (ReflectiveOperationException e) {
            fail("Reflection failed while injecting comparator‑based set", e);
        }
        sealedFlag.set(false);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> stringSet.add("bad"), "Comparator should reject \"bad\" and propagate IllegalArgumentException");
        assertEquals("value not allowed", ex.getMessage());
    }

    @Test
    void addNullWhenComparatorAllowsItReturnsTrue() {
        Comparator<String> nullFriendly = (a, b) -> {
            if (a == null && b == null)
                return 0;
            // null is smallest
            if (a == null)
                return -1;
            if (b == null)
                return 1;
            return a.compareTo(b);
        };
        NavigableSet<String> underlying = new TreeSet<>(nullFriendly);
        SealableNavigableSet<String> stringSet = null;
        try {
            stringSet = withUnderlyingSet(new SealableNavigableSet<>(sealedSupplier), underlying);
        } catch (ReflectiveOperationException e) {
            fail("Reflection failed while injecting null‑friendly comparator set", e);
        }
        sealedFlag.set(false);
        assertFalse(stringSet.contains(null), "Pre‑condition: set should not contain null");
        boolean added = stringSet.add(null);
        assertTrue(added, "Adding null should succeed when comparator permits it");
        assertTrue(stringSet.contains(null), "Null must be present after successful add");
        assertEquals(1, stringSet.size(), "Size should be 1 after adding the sole element");
    }

    @Test
    void addIncrementsSizeWhenElementIsNew() {
        int originalSize = set.size();
        // 40 is not present in the initial set
        boolean added = set.add(40);
        assertTrue(added, "add should return true for a new element");
        assertEquals(originalSize + 1, set.size(), "Size must increase by one after a successful add");
    }

    @Test
    void addToUnmodifiableUnderlyingSetThrowsUnsupportedOperationException() {
        NavigableSet<Integer> modifiable = new TreeSet<>();
        NavigableSet<Integer> unmodifiable = Collections.unmodifiableNavigableSet(modifiable);
        SealableNavigableSet<Integer> wrapped = null;
        try {
            wrapped = withUnderlyingSet(new SealableNavigableSet<>(sealedSupplier), unmodifiable);
        } catch (ReflectiveOperationException e) {
            fail("Reflection failed while injecting unmodifiable set", e);
        }
        // still unsealed – the underlying collection is immutable
        sealedFlag.set(false);
        assertThrows(UnsupportedOperationException.class, () -> wrapped.add(99), "Adding to an unmodifiable underlying set must propagate UnsupportedOperationException");
    }
}
