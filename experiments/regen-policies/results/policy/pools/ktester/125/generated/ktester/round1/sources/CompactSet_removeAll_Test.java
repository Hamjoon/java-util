package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.concurrent.ConcurrentModificationException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CompactSet_removeAll_Test {

    /**
     * Mocked map used for delegation verification.
     */
    @Mock
    private CompactMap<String, Object> mockMap;

    /**
     * Mocked key‑set returned by the mocked map.
     */
    @Mock
    private Set<String> mockKeySet;

    /**
     * A real CompactSet instance used for functional tests.
     */
    private CompactSet<String> realSet;

    /**
     * Collection whose iterator throws {@link IllegalStateException} from {@code hasNext()}.
     */
    private static class ExceptionOnHasNextCollection<E> extends AbstractCollection<E> {

        @Override
        public Iterator<E> iterator() {
            return new Iterator<E>() {

                @Override
                public boolean hasNext() {
                    throw new IllegalStateException("hasNext failure");
                }

                @Override
                public E next() {
                    return null;
                }
            };
        }

        @Override
        public int size() {
            // size is irrelevant for the test
            return 1;
        }
    }

    /**
     * Collection whose iterator throws {@link ConcurrentModificationException} from {@code next()}.
     */
    private static class ExceptionOnNextCollection<E> extends AbstractCollection<E> {

        @Override
        public Iterator<E> iterator() {
            return new Iterator<E>() {

                private boolean first = true;

                @Override
                public boolean hasNext() {
                    return first;
                }

                @Override
                public E next() {
                    first = false;
                    throw new ConcurrentModificationException("next failure");
                }
            };
        }

        @Override
        public int size() {
            return 1;
        }
    }

    /**
     * Collection whose iterator throws a generic {@link RuntimeException} from {@code next()}.
     */
    private static class RuntimeExceptionOnIteratorCollection<E> extends AbstractCollection<E> {

        @Override
        public Iterator<E> iterator() {
            return new Iterator<E>() {

                private boolean first = true;

                @Override
                public boolean hasNext() {
                    return first;
                }

                @Override
                public E next() {
                    first = false;
                    throw new RuntimeException("iterator failure");
                }
            };
        }

        @Override
        public int size() {
            return 1;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise the real set for functional tests.
        realSet = new CompactSet<>();
        // Configure the mock map to return the mock key‑set.
        when(mockMap.keySet()).thenReturn(mockKeySet);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks to avoid cross‑test interference.
        clearInvocations(mockMap, mockKeySet);
    }

    /**
     * Provides arguments for {@link #removeAllBasicContract(Set, Collection, boolean, Set)}.
     *
     * <p>Each argument consists of:
     * <ul>
     *   <li>initial set contents</li>
     *   <li>collection passed to {@code removeAll}</li>
     *   <li>expected boolean result</li>
     *   <li>expected remaining elements after removal</li>
     * </ul>
     */
    private static Stream<Arguments> basicContractProvider() {
        return Stream.of(// [] (null collection) is exercised in a dedicated test above.
        Arguments.of(Set.of("a", "b", "c"), Collections.emptyList(), false, Set.of("a", "b", "c")), Arguments.of(Set.of("a", "b", "c"), List.of("a", "b"), true, Set.of("c")), Arguments.of(Set.of("a", "b", "c"), List.of("b", "x"), true, Set.of("a", "c")), Arguments.of(Set.of("a", "b", "c"), List.of("x", "y"), false, Set.of("a", "b", "c")), Arguments.of(Set.of("a", "b", "c"), List.of("a", "a", "b"), true, Set.of("c")));
    }

    @Test
    void removeAllRemovesExistingElementsAndReturnsTrue() {
        // given
        realSet.add("alpha");
        realSet.add("beta");
        realSet.add("gamma");
        // "delta" is not present
        Collection<String> toRemove = Arrays.asList("beta", "delta");
        // when
        boolean changed = realSet.removeAll(toRemove);
        // then
        assertTrue(changed, "removeAll should report a change when at least one element is removed");
        assertEquals(2, realSet.size());
        assertTrue(realSet.contains("alpha"));
        assertTrue(realSet.contains("gamma"));
        assertFalse(realSet.contains("beta"));
    }

    @Test
    void removeAllWithNoMatchingElementsReturnsFalse() {
        // given
        realSet.add("one");
        realSet.add("two");
        Collection<String> toRemove = Collections.singleton("three");
        // when
        boolean changed = realSet.removeAll(toRemove);
        // then
        assertFalse(changed, "removeAll should return false when the set is not modified");
        assertEquals(2, realSet.size());
        assertTrue(realSet.contains("one"));
        assertTrue(realSet.contains("two"));
    }

    @Test
    void removeAllWithEmptyCollectionDoesNotModifySet() {
        // given
        realSet.add("x");
        realSet.add("y");
        // when
        boolean changed = realSet.removeAll(Collections.emptyList());
        // then
        assertFalse(changed);
        assertEquals(2, realSet.size());
    }

    @Test
    void removeAllNullCollectionThrowsNPE() {
        // given
        realSet.add("a");
        // when / then
        assertThrows(NullPointerException.class, () -> realSet.removeAll(null));
    }

    @Test
    void removeAllDelegatesToMapKeySetAndReturnsItsResult() {
        // given
        Collection<String> collection = Arrays.asList("x", "y");
        when(mockKeySet.removeAll(collection)).thenReturn(true);
        CompactSet<String> setWithMock = new CompactSet<>(mockMap);
        // when
        boolean result = setWithMock.removeAll(collection);
        // then
        assertTrue(result, "Result should be whatever the mock keySet returns");
        verify(mockMap, times(1)).keySet();
        verify(mockKeySet, times(1)).removeAll(collection);
    }

    @Test
    void removeAllDelegatesAndReturnsFalseWhenNoChange() {
        // given
        Collection<String> collection = Collections.singleton("z");
        when(mockKeySet.removeAll(collection)).thenReturn(false);
        CompactSet<String> setWithMock = new CompactSet<>(mockMap);
        // when
        boolean result = setWithMock.removeAll(collection);
        // then
        assertFalse(result);
        verify(mockMap).keySet();
        verify(mockKeySet).removeAll(collection);
    }

    @Test
    void removeAllRemovesNullElementWhenPresent() {
        // given
        realSet.add(null);
        realSet.add("a");
        realSet.add("b");
        Collection<String> toRemove = Collections.singletonList(null);
        // when
        boolean changed = realSet.removeAll(toRemove);
        // then
        assertTrue(changed, "Removing a present null element must return true");
        assertEquals(2, realSet.size());
        assertTrue(realSet.contains("a"));
        assertTrue(realSet.contains("b"));
        assertFalse(realSet.contains(null));
    }

    @Test
    void removeAllNullElementNotPresentReturnsFalse() {
        // given
        realSet.add("a");
        realSet.add("b");
        realSet.add("c");
        Collection<String> toRemove = Collections.singletonList(null);
        // when
        boolean changed = realSet.removeAll(toRemove);
        // then
        assertFalse(changed, "Removing a null element that is not present must return false");
        assertEquals(3, realSet.size());
        assertTrue(realSet.containsAll(Arrays.asList("a", "b", "c")));
    }

    @Test
    void removeAllSelfRemovalNonEmptySet() {
        // given
        realSet.add("a");
        realSet.add("b");
        realSet.add("c");
        // when
        boolean changed = realSet.removeAll(realSet);
        // then
        assertTrue(changed, "Calling removeAll with the set itself should clear it and return true");
        assertTrue(realSet.isEmpty());
    }

    @Test
    void removeAllSelfRemovalEmptySet() {
        // given – realSet is already empty
        // when
        boolean changed = realSet.removeAll(realSet);
        // then
        assertFalse(changed, "Calling removeAll with the set itself on an empty set must return false");
        assertTrue(realSet.isEmpty());
    }

    @Test
    void removeAllEmptyCollectionOnEmptySetReturnsFalse() {
        // given – empty set
        // when
        boolean changed = realSet.removeAll(Collections.emptyList());
        // then
        assertFalse(changed);
        assertTrue(realSet.isEmpty());
    }

    @Test
    void removeAllEmptySetWithNonEmptyCollectionReturnsFalse() {
        // given – empty set
        Collection<String> toRemove = Collections.singleton("a");
        // when
        boolean changed = realSet.removeAll(toRemove);
        // then
        assertFalse(changed);
        assertTrue(realSet.isEmpty());
    }

    @ParameterizedTest(name = "{index}: removeAll({1}) on set {0} → changed={2}, remaining={3}")
    @MethodSource("basicContractProvider")
    void removeAllBasicContract(Set<String> initialSet, Collection<String> toRemove, boolean expectedChanged, Set<String> expectedRemaining) {
        // initialise the real set with the supplied elements
        realSet.clear();
        realSet.addAll(initialSet);
        // execute
        boolean changed = realSet.removeAll(toRemove);
        // verify
        assertEquals(expectedChanged, changed, () -> "Expected change flag to be " + expectedChanged);
        assertEquals(expectedRemaining, new HashSet<>(realSet), () -> "Remaining elements do not match expected");
    }

    @Test
    void removeAllPropagatesExceptionFromIteratorHasNext() {
        // given
        Collection<String> badCollection = new ExceptionOnHasNextCollection<>();
        // when / then
        assertThrows(IllegalStateException.class, () -> realSet.removeAll(badCollection));
    }

    @Test
    void removeAllPropagatesExceptionFromIteratorNext() {
        // given
        Collection<String> badCollection = new ExceptionOnNextCollection<>();
        // when / then
        assertThrows(ConcurrentModificationException.class, () -> realSet.removeAll(badCollection));
    }

    @Test
    void removeAllPropagatesRuntimeExceptionFromIterator() {
        // given
        Collection<String> badCollection = new RuntimeExceptionOnIteratorCollection<>();
        // when / then
        assertThrows(RuntimeException.class, () -> realSet.removeAll(badCollection));
    }
}
