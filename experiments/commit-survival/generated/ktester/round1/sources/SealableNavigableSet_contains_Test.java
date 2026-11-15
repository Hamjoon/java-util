package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.concurrent.ConcurrentModificationException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SealableNavigableSet_contains_Test {

    /**
     * The mock backing set that the {@link SealableNavigableSet} delegates to.
     */
    @Mock
    private NavigableSet<Integer> mockBackingSet;

    /**
     * The {@link SealableNavigableSet} under test.
     */
    private SealableNavigableSet<Integer> sealableSet;

    /**
     * Simple mutable flag used by the {@link Supplier} to indicate sealed state.
     */
    private volatile boolean sealedState;

    /**
     * Supplier that reads {@link #sealedState}.
     */
    private Supplier<Boolean> sealedSupplier;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise Mockito annotations (needed when not using field injection via @MockitoSettings)
        MockitoAnnotations.openMocks(this);
        sealedState = false;
        sealedSupplier = () -> sealedState;
        // Construct the SealableNavigableSet with the mocked backing set.
        sealableSet = new SealableNavigableSet<>(mockBackingSet, sealedSupplier);
    }

    @AfterEach
    void teardownAfterEach() {
        // Verify no unexpected interactions on the mock used in the simple tests.
        if (mockBackingSet != null) {
            verifyNoMoreInteractions(mockBackingSet);
        }
    }

    private static Stream<Arguments> basicDelegationProvider() {
        return Stream.of(// empty HashSet, contains(null) -> false
        Arguments.of(new HashSet<String>(), null, false, "empty HashSet, null"), // TreeSet with "apple", contains("apple") -> true
        Arguments.of(new TreeSet<>(Collections.singleton("apple")), "apple", true, "TreeSet contains apple"), // TreeSet without "banana", contains("banana") -> false
        Arguments.of(new TreeSet<>(Collections.singleton("apple")), "banana", false, "TreeSet missing banana"), // TreeSet (no nulls) -> contains(null) throws NPE
        Arguments.of(new TreeSet<String>(), null, NullPointerException.class, "TreeSet null NPE"));
    }

    @Test
    void contains_DelegatesToBackingSet_ReturnsTrueWhenElementPresent() {
        // Arrange
        when(mockBackingSet.contains(10)).thenReturn(true);
        // Act
        boolean result = sealableSet.contains(10);
        // Assert
        assertTrue(result, "contains should return true when the backing set reports true");
        verify(mockBackingSet, times(1)).contains(10);
    }

    @Test
    void contains_DelegatesToBackingSet_ReturnsFalseWhenElementAbsent() {
        // Arrange
        when(mockBackingSet.contains(99)).thenReturn(false);
        // Act
        boolean result = sealableSet.contains(99);
        // Assert
        assertFalse(result, "contains should return false when the backing set reports false");
        verify(mockBackingSet, times(1)).contains(99);
    }

    @Test
    void contains_DelegatesToBackingSet_WithNullElement() {
        // Arrange
        when(mockBackingSet.contains(null)).thenReturn(true);
        // Act
        boolean result = sealableSet.contains(null);
        // Assert
        assertTrue(result, "contains should correctly handle null elements");
        verify(mockBackingSet, times(1)).contains(null);
    }

    @Test
    void contains_IgnoresSealedState_ReturnsResultRegardlessOfSeal() {
        // Arrange
        when(mockBackingSet.contains(20)).thenReturn(true);
        // Simulate a sealed collection
        sealedState = true;
        // Act
        boolean result = sealableSet.contains(20);
        // Assert
        assertTrue(result, "contains should not be affected by the sealed state");
        verify(mockBackingSet, times(1)).contains(20);
    }

    @Test
    void contains_MultipleCalls_DelegatesEachTime() {
        sealableSet = new SealableNavigableSet<>(mockBackingSet, sealedSupplier);
        when(mockBackingSet.contains(1)).thenReturn(false);
        when(mockBackingSet.contains(2)).thenReturn(true);
        assertFalse(sealableSet.contains(1));
        assertTrue(sealableSet.contains(2));
        verify(mockBackingSet, times(1)).contains(1);
        verify(mockBackingSet, times(1)).contains(2);
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("basicDelegationProvider")
    void contains_BasicDelegationScenarios(Set<String> backing, Object query, Object expected, String description) {
        // Build the SealableNavigableSet with a dummy supplier (sealed state irrelevant here)
        @SuppressWarnings("unchecked")
        SealableNavigableSet<String> set = new SealableNavigableSet<>((NavigableSet<String>) backing, sealedSupplier);
        if (expected instanceof Class) {
            // Expect an exception
            assertThrows((Class<? extends Throwable>) expected, () -> set.contains(query), description);
        } else {
            boolean result = set.contains(query);
            assertEquals(expected, result, description);
        }
        // No interactions to verify because we used real collections.
    }

    @Test
    void contains_NullPermittingSet_HashSetContainsNull_ReturnsTrue() {
        Set<String> hashSet = new HashSet<>();
        // HashSet permits null
        hashSet.add(null);
        @SuppressWarnings("unchecked")
        SealableNavigableSet<String> set = new SealableNavigableSet<>((NavigableSet<String>) hashSet, sealedSupplier);
        assertTrue(set.contains(null), "HashSet containing null should return true");
    }

    @Test
    void contains_TreeSetWithIncompatibleType_ThrowsClassCastException() {
        NavigableSet<Integer> treeSet = new TreeSet<>(Arrays.asList(1, 2, 3));
        @SuppressWarnings("unchecked")
        SealableNavigableSet<Integer> set = new SealableNavigableSet<>(treeSet, sealedSupplier);
        assertThrows(ClassCastException.class, () -> set.contains("string"), "TreeSet<Integer> should throw ClassCastException when searching for a String");
    }

    @Test
    void contains_CustomSetThrowsIllegalStateException_Propagates() {
        NavigableSet<String> customSet = new AbstractSet<>() {

            @Override
            public Iterator<String> iterator() {
                return Collections.emptyIterator();
            }

            @Override
            public int size() {
                return 0;
            }

            @Override
            public boolean contains(Object o) {
                throw new IllegalStateException("trigger");
            }
        };
        @SuppressWarnings("unchecked")
        SealableNavigableSet<String> set = new SealableNavigableSet<>(customSet, sealedSupplier);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> set.contains("trigger"));
        assertEquals("trigger", ex.getMessage());
    }

    @Test
    void contains_AfterClear_ReturnsFalse() {
        NavigableSet<String> backing = new TreeSet<>(Collections.singleton("apple"));
        @SuppressWarnings("unchecked")
        SealableNavigableSet<String> set = new SealableNavigableSet<>(backing, sealedSupplier);
        // Ensure element present before clear
        assertTrue(set.contains("apple"));
        // Clear via SealableNavigableSet (delegates to backing)
        set.clear();
        // Now should be absent
        assertFalse(set.contains("apple"));
    }

    @Test
    void contains_NullBackingSet_ThrowsNullPointerException() {
        // Directly instantiate with null backing set
        SealableNavigableSet<String> set = new SealableNavigableSet<>(null, sealedSupplier);
        assertThrows(NullPointerException.class, () -> set.contains("any"));
    }

    @Test
    void contains_CustomSetThrowsIllegalArgumentException_Propagates() {
        NavigableSet<String> customSet = new AbstractSet<>() {

            @Override
            public Iterator<String> iterator() {
                return Collections.emptyIterator();
            }

            @Override
            public int size() {
                return 0;
            }

            @Override
            public boolean contains(Object o) {
                throw new IllegalArgumentException("bad argument");
            }
        };
        @SuppressWarnings("unchecked")
        SealableNavigableSet<String> set = new SealableNavigableSet<>(customSet, sealedSupplier);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> set.contains("trigger"));
        assertEquals("bad argument", ex.getMessage());
    }

    @Test
    void contains_CustomSetThrowsConcurrentModificationException_Propagates() {
        NavigableSet<String> customSet = new AbstractSet<>() {

            @Override
            public Iterator<String> iterator() {
                return Collections.emptyIterator();
            }

            @Override
            public int size() {
                return 0;
            }

            @Override
            public boolean contains(Object o) {
                throw new ConcurrentModificationException("concurrent");
            }
        };
        @SuppressWarnings("unchecked")
        SealableNavigableSet<String> set = new SealableNavigableSet<>(customSet, sealedSupplier);
        ConcurrentModificationException ex = assertThrows(ConcurrentModificationException.class, () -> set.contains("any"));
        assertEquals("concurrent", ex.getMessage());
    }

    @Test
    void contains_TreeSetWithCaseInsensitiveComparator_ReturnsTrue() {
        NavigableSet<String> caseInsensitiveSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        caseInsensitiveSet.add("apple");
        @SuppressWarnings("unchecked")
        SealableNavigableSet<String> set = new SealableNavigableSet<>(caseInsensitiveSet, sealedSupplier);
        assertTrue(set.contains("APPLE"), "Case‑insensitive comparator should treat \"APPLE\" as present");
    }
}
