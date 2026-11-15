package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import java.lang.reflect.Field;
import java.util.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SealableSet_contains_Test {

    private SealableSet<Integer> set;

    private volatile boolean sealed;

    private Supplier<Boolean> sealedSupplier;

    class Wrapper {

        final int value;

        Wrapper(int v) {
            this.value = v;
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof Wrapper) && ((Wrapper) o).value == this.value;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(value);
        }
    }

    class Wrapper {

        final int value;

        Wrapper(int v) {
            this.value = v;
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof Integer && ((Integer) o) == value) || (o instanceof Wrapper && ((Wrapper) o).value == value);
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(value);
        }
    }

    class Wrapper {

        final int value;

        Wrapper(int v) {
            this.value = v;
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof Integer && ((Integer) o) == value) || (o instanceof Wrapper && ((Wrapper) o).value == value);
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(value);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        sealed = false;
        sealedSupplier = () -> sealed;
        set = new SealableSet<>(sealedSupplier);
        // Populate the set with a few distinct values, including null
        set.add(10);
        set.add(20);
        set.add(null);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference to help GC; not strictly required
        set = null;
    }

    /**
     * Helper that injects a custom {@link Set} into a {@link SealableSet}
     * via reflection.  The {@code SealableSet} constructor creates its own
     * internal set, which we replace with the supplied one.
     */
    private static <T> SealableSet<T> createWithDelegate(Set<T> delegate, Supplier<Boolean> sealedSupplier) {
        SealableSet<T> wrapper = new SealableSet<>(sealedSupplier);
        try {
            Field f = SealableSet.class.getDeclaredField("set");
            f.setAccessible(true);
            f.set(wrapper, delegate);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        return wrapper;
    }

    @Test
    void contains_ShouldReturnTrue_ForExistingElements() {
        assertTrue(set.contains(10), "Set should contain 10");
        assertTrue(set.contains(20), "Set should contain 20");
        assertTrue(set.contains(null), "Set should contain null");
    }

    @Test
    void contains_ShouldReturnFalse_ForMissingElements() {
        assertFalse(set.contains(30), "Set should not contain 30");
        assertFalse(set.contains("string"), "Set should not contain a different type");
    }

    @Test
    void contains_ShouldWorkWhenSealed() {
        // switch to sealed (read‑only) mode
        sealed = true;
        // contains must still delegate to the underlying set and therefore succeed
        assertTrue(set.contains(10));
        assertTrue(set.contains(null));
        assertFalse(set.contains(99));
    }

    @Test
    void contains_ShouldRespectCustomEqualsImplementation() {
        // Use a simple wrapper class that defines equality based on its internal value
        class Wrapper {

            final int value;

            Wrapper(int v) {
                this.value = v;
            }

            @Override
            public boolean equals(Object o) {
                return (o instanceof Wrapper) && ((Wrapper) o).value == this.value;
            }

            @Override
            public int hashCode() {
                return Integer.hashCode(value);
            }
        }
        SealableSet<Wrapper> wrapperSet = new SealableSet<>(sealedSupplier);
        wrapperSet.add(new Wrapper(5));
        wrapperSet.add(new Wrapper(10));
        assertTrue(wrapperSet.contains(new Wrapper(5)), "Set should contain Wrapper with value 5");
        assertFalse(wrapperSet.contains(new Wrapper(7)), "Set should not contain Wrapper with value 7");
    }

    @Test
    void contains_ShouldReturnFalse_OnEmptySet() {
        SealableSet<String> emptySet = new SealableSet<>(sealedSupplier);
        assertFalse(emptySet.contains("anything"));
    }

    @ParameterizedTest(name = "[{index}] contains({0}) → {1}")
    @CsvSource({ "'apple', true,  // element present", "'banana', false, // element absent", "'null', false   // null not added" })
    void contains_BasicFunctionality(String element, boolean expected) {
        // Build a backing HashSet with only "apple"
        Set<String> backing = new HashSet<>(Collections.singletonList("apple"));
        SealableSet<String> stringSet = createWithDelegate(backing, sealedSupplier);
        Object query = "null".equals(element) ? null : element;
        assertEquals(expected, stringSet.contains(query), () -> "contains(" + query + ") should be " + expected);
    }

    @Test
    void contains_NullElementPresent_ShouldReturnTrue() {
        Set<String> backing = new HashSet<>();
        backing.add(null);
        SealableSet<String> stringSet = createWithDelegate(backing, sealedSupplier);
        assertTrue(stringSet.contains(null), "Set contains null, contains(null) must be true");
    }

    @Test
    void contains_EmptySet_ShouldReturnFalse() {
        SealableSet<String> empty = new SealableSet<>(sealedSupplier);
        assertFalse(empty.contains("anyValue"));
    }

    @Test
    void contains_TypeMismatch_ShouldReturnFalse() {
        Set<Integer> backing = new HashSet<>(Collections.singleton(5));
        SealableSet<Integer> intSet = createWithDelegate(backing, sealedSupplier);
        // Pass a Long; the underlying Set will simply not find it.
        assertFalse(intSet.contains(5L), "Long 5L should not be considered equal to Integer 5");
    }

    @Test
    void contains_AfterRemoval_ShouldReturnFalse() {
        Set<String> backing = new HashSet<>();
        backing.add("toBeRemoved");
        SealableSet<String> stringSet = createWithDelegate(backing, sealedSupplier);
        // remove via the wrapper (delegates to the set)
        assertTrue(stringSet.remove("toBeRemoved"));
        assertFalse(stringSet.contains("toBeRemoved"));
    }

    @Test
    void contains_CustomEqualsElement_ShouldReturnTrue() {
        // Wrapper that is equal to an Integer with the same numeric value
        class Wrapper {

            final int value;

            Wrapper(int v) {
                this.value = v;
            }

            @Override
            public boolean equals(Object o) {
                return (o instanceof Integer && ((Integer) o) == value) || (o instanceof Wrapper && ((Wrapper) o).value == value);
            }

            @Override
            public int hashCode() {
                return Integer.hashCode(value);
            }
        }
        Set<Object> backing = new HashSet<>();
        backing.add(new Wrapper(10));
        SealableSet<Object> setWithWrapper = createWithDelegate(backing, sealedSupplier);
        assertTrue(setWithWrapper.contains(10), "Wrapper equals Integer 10, so contains(10) must be true");
    }

    @Test
    void contains_UnderlyingSetThrowsRuntimeException_ShouldPropagate() {
        @SuppressWarnings("unchecked")
        Set<String> mockSet = mock(Set.class);
        when(mockSet.contains("trigger")).thenThrow(new RuntimeException("boom"));
        SealableSet<String> wrapper = createWithDelegate(mockSet, sealedSupplier);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> wrapper.contains("trigger"));
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void contains_UnderlyingSetDisallowsNull_ShouldThrowNullPointerException() {
        // TreeSet without a comparator does not allow null
        Set<String> treeSet = new TreeSet<>();
        SealableSet<String> wrapper = createWithDelegate(treeSet, sealedSupplier);
        assertThrows(NullPointerException.class, () -> wrapper.contains(null));
    }

    @Test
    void contains_UnderlyingTreeSetWithIncompatibleType_ShouldThrowClassCastException() {
        Set<Integer> treeSet = new TreeSet<>();
        treeSet.add(1);
        SealableSet<Integer> wrapper = createWithDelegate(treeSet, sealedSupplier);
        assertThrows(ClassCastException.class, () -> wrapper.contains("stringValue"));
    }

    @Test
    void contains_UnderlyingSetThrowsUnsupportedOperationException_ShouldPropagate() {
        @SuppressWarnings("unchecked")
        Set<String> mockSet = mock(Set.class);
        when(mockSet.contains(any())).thenThrow(new UnsupportedOperationException("not supported"));
        SealableSet<String> wrapper = createWithDelegate(mockSet, sealedSupplier);
        assertThrows(UnsupportedOperationException.class, () -> wrapper.contains("anyValue"));
    }

    @Test
    void contains_InternalSetIsNull_ShouldThrowNullPointerException() throws Exception {
        SealableSet<String> wrapper = new SealableSet<>(sealedSupplier);
        // Force the private field to null via reflection
        Field f = SealableSet.class.getDeclaredField("set");
        f.setAccessible(true);
        f.set(wrapper, null);
        assertThrows(NullPointerException.class, () -> wrapper.contains("irrelevant"));
    }
}
