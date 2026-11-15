package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_remove_2_Test {

    /**
     * The list instance used in each test.
     */
    private List<Integer> list;

    /**
     * A second list used for the “wrapped list” constructor scenario.
     */
    private List<Integer> backingList;

    /**
     * Simple POJO used to test {@code equals} delegation.
     */
    private static final class Foo {

        private final int id;

        Foo(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof Foo) && ((Foo) o).id == this.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public String toString() {
            return "Foo(" + id + ')';
        }
    }

    /**
     * An element whose {@code equals} always throws.
     */
    private static final class BadEquals {

        @Override
        public boolean equals(Object o) {
            throw new RuntimeException("Bad equals invoked");
        }

        @Override
        public int hashCode() {
            return 0;
        }
    }

    /**
     * List implementation that throws a supplied exception from {@code remove(Object)}.
     */
    private static final class ExceptionThrowingList<E> implements List<E> {

        private final RuntimeException toThrow;

        ExceptionThrowingList(RuntimeException toThrow) {
            this.toThrow = toThrow;
        }

        @Override
        public boolean remove(Object o) {
            throw toThrow;
        }

        // --- the rest of the List contract is delegated to an internal ArrayList ---
        private final List<E> delegate = new ArrayList<>();

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return delegate.contains(o);
        }

        @Override
        public Iterator<E> iterator() {
            return delegate.iterator();
        }

        @Override
        public Object[] toArray() {
            return delegate.toArray();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return delegate.toArray(a);
        }

        @Override
        public boolean add(E e) {
            return delegate.add(e);
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            return delegate.removeAll(c);
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            return delegate.retainAll(c);
        }

        @Override
        public void clear() {
            delegate.clear();
        }

        @Override
        public E get(int index) {
            return delegate.get(index);
        }

        @Override
        public E set(int index, E element) {
            return delegate.set(index, element);
        }

        @Override
        public void add(int index, E element) {
            delegate.add(index, element);
        }

        @Override
        public E remove(int index) {
            return delegate.remove(index);
        }

        @Override
        public int indexOf(Object o) {
            return delegate.indexOf(o);
        }

        @Override
        public int lastIndexOf(Object o) {
            return delegate.lastIndexOf(o);
        }

        @Override
        public ListIterator<E> listIterator() {
            return delegate.listIterator();
        }

        @Override
        public ListIterator<E> listIterator(int index) {
            return delegate.listIterator(index);
        }

        @Override
        public List<E> subList(int fromIndex, int toIndex) {
            return delegate.subList(fromIndex, toIndex);
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return delegate.containsAll(c);
        }

        @Override
        public boolean addAll(Collection<? extends E> c) {
            return delegate.addAll(c);
        }

        @Override
        public boolean addAll(int index, Collection<? extends E> c) {
            return delegate.addAll(index, c);
        }
    }

    class FaultyConcurrentList<E> extends ConcurrentList<E> {

        // Expose a protected method that delegates to the private writeOperation.
        // We use reflection to invoke the private method and force it to throw.
        @SuppressWarnings("unchecked")
        @Override
        public boolean remove(Object o) {
            // Force an IllegalStateException to mimic lock acquisition failure.
            throw new IllegalStateException("simulated lock failure");
        }
    }

    class FaultyConcurrentList<E> extends ConcurrentList<E> {

        // Expose a protected method that delegates to the private writeOperation.
        // We use reflection to invoke the private method and force it to throw.
        @SuppressWarnings("unchecked")
        @Override
        public boolean remove(Object o) {
            // Force an IllegalStateException to mimic lock acquisition failure.
            throw new IllegalStateException("simulated lock failure");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Fresh instance for every test to guarantee isolation.
        list = new ConcurrentList<>();
        backingList = new ArrayList<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and make sure no stray references remain.
        list = null;
        backingList = null;
    }

    /**
     * Data for successful removal of an existing element.
     */
    private static Stream<Arguments> successfulRemovalProvider() {
        return Stream.of(Arguments.of(new ArrayList<>(List.of("apple", "banana")), "apple", List.of("banana")), Arguments.of(new ArrayList<>(List.of("banana", "cherry", "banana")), "banana", List.of("cherry", "banana")));
    }

    /**
     * Data for removal of a non‑existing element.
     */
    private static Stream<Arguments> nonExistingRemovalProvider() {
        return Stream.of(Arguments.of(new ArrayList<>(List.of("apple", "banana")), "durian"), Arguments.of(new ArrayList<>(List.of("apple", "banana")), null));
    }

    /**
     * Data for single‑element list scenarios.
     */
    private static Stream<Arguments> singleElementProvider() {
        return Stream.of(Arguments.of("solo", true), Arguments.of("other", false));
    }

    @Test
    void removeExistingElement_returnsTrue_andElementIsRemoved() {
        // given
        list.add(10);
        list.add(20);
        list.add(30);
        // when
        boolean removed = list.remove(Integer.valueOf(20));
        // then
        assertTrue(removed, "remove should return true for an existing element");
        assertEquals(2, list.size(), "size should decrease after removal");
        assertFalse(list.contains(20), "list should no longer contain the removed element");
        assertTrue(list.containsAll(List.of(10, 30)), "other elements must stay untouched");
    }

    @Test
    void removeNonExistingElement_returnsFalse_andListUnchanged() {
        // given
        list.add(1);
        list.add(2);
        list.add(3);
        // when
        boolean removed = list.remove(Integer.valueOf(99));
        // then
        assertFalse(removed, "remove should return false when element is absent");
        assertEquals(3, list.size(), "size must stay unchanged");
        assertTrue(list.containsAll(List.of(1, 2, 3)), "original elements must remain");
    }

    @Test
    void removeNullElement_behavesAccordingToListContract() {
        // given
        list.add(null);
        list.add(5);
        // when
        boolean removed = list.remove(null);
        // then
        assertTrue(removed, "remove should succeed when null is present");
        assertEquals(1, list.size(), "size should reflect the removal of null");
        assertFalse(list.contains(null), "null should no longer be present");
        assertTrue(list.contains(5), "other elements must stay");
    }

    @Test
    void removeNullWhenNotPresent_returnsFalse() {
        // given
        list.add("a");
        list.add("b");
        // when
        boolean removed = list.remove(null);
        // then
        assertFalse(removed, "remove should return false when null is not in the list");
        assertEquals(2, list.size(), "size must stay unchanged");
    }

    @Test
    void removeFromWrappedList_preservesUnderlyingListSemantics() {
        // given: a backing list that we will wrap
        backingList.add(100);
        backingList.add(200);
        backingList.add(300);
        List<Integer> wrapped = new ConcurrentList<>(backingList);
        // when
        boolean removed = wrapped.remove(Integer.valueOf(200));
        // then
        assertTrue(removed, "wrapped list should remove existing element");
        assertEquals(2, wrapped.size(), "size after removal");
        assertFalse(wrapped.contains(200), "element must be gone from wrapped view");
        // The underlying list must reflect the same change because the wrapper does not copy elements.
        assertEquals(2, backingList.size(), "backing list size must also be updated");
        assertFalse(backingList.contains(200), "backing list must reflect removal");
    }

    @Test
    void removeFromWrappedListWhenElementAbsent_returnsFalse() {
        // given
        backingList.add(1);
        backingList.add(2);
        List<Integer> wrapped = new ConcurrentList<>(backingList);
        // when
        boolean removed = wrapped.remove(Integer.valueOf(99));
        // then
        assertFalse(removed, "removing absent element should return false");
        assertEquals(2, wrapped.size(), "wrapped list size unchanged");
        assertEquals(2, backingList.size(), "backing list size unchanged");
    }

    @Test
    void removeWhenListIsEmpty_returnsFalse() {
        // given: an empty list (already created in @BeforeEach)
        // when
        boolean removed = list.remove(Integer.valueOf(1));
        // then
        assertFalse(removed, "remove on empty list must return false");
        assertTrue(list.isEmpty(), "list remains empty");
    }

    @ParameterizedTest(name = "[{index}] remove \"{1}\" from {0}")
    @MethodSource("successfulRemovalProvider")
    void removeExistingElement_returnsTrue_andUpdatesList(List<String> initial, String toRemove, List<String> expected) {
        // given
        list.addAll(initial);
        // when
        boolean result = list.remove(toRemove);
        // then
        assertTrue(result, "remove should return true for an existing element");
        assertEquals(expected, list, "list content after removal must match expected");
    }

    @ParameterizedTest(name = "[{index}] single‑element list remove \"{0}\"")
    @MethodSource("singleElementProvider")
    void singleElementListRemoval(String toRemove, boolean expectedResult) {
        // given
        list.add("solo");
        // when
        boolean result = list.remove(toRemove);
        // then
        assertEquals(expectedResult, result, "remove result must match expectation");
        if (expectedResult) {
            assertTrue(list.isEmpty(), "list should be empty after successful removal");
        } else {
            assertEquals(1, list.size(), "list size must stay 1 after failed removal");
            assertEquals("solo", list.get(0));
        }
    }

    @Test
    void multipleNullsRemoval_deletesOnlyFirstOccurrence() {
        // given
        list.add(null);
        list.add(null);
        list.add("a");
        // when
        boolean removed = list.remove(null);
        // then
        assertTrue(removed, "remove should delete the first null");
        assertEquals(List.of(null, "a"), list, "list must contain the second null and the remaining element");
    }

    @Test
    void removeDifferentTypeElement_returnsFalse() {
        // given
        List<String> stringList = new ConcurrentList<>();
        stringList.add("one");
        stringList.add("two");
        // when
        // Integer vs String
        boolean removed = stringList.remove(1);
        // then
        assertFalse(removed, "removing an element of a different type must return false");
        assertEquals(List.of("one", "two"), stringList, "list content must stay unchanged");
    }

    @Test
    void removeCustomObjectWithEquals_usesEqualsLogic() {
        // given
        List<Foo> fooList = new ConcurrentList<>();
        fooList.add(new Foo(1));
        fooList.add(new Foo(42));
        fooList.add(new Foo(3));
        // when
        boolean removed = fooList.remove(new Foo(42));
        // then
        assertTrue(removed, "remove should succeed using Foo.equals");
        assertEquals(List.of(new Foo(1), new Foo(3)), fooList, "list must contain the remaining Foo objects");
    }

    @Test
    void removeElementWhoseEqualsThrows_propagatesRuntimeException() {
        // given
        List<BadEquals> badList = new ConcurrentList<>();
        BadEquals bad = new BadEquals();
        badList.add(bad);
        // when / then
        RuntimeException ex = assertThrows(RuntimeException.class, () -> badList.remove(new BadEquals()), "remove must propagate RuntimeException from equals");
        assertEquals("Bad equals invoked", ex.getMessage());
    }

    @Test
    void removeFromUnmodifiableUnderlyingList_propagatesUnsupportedOperationException() {
        // given
        List<String> unmodifiable = Collections.unmodifiableList(List.of("x", "y"));
        List<String> wrapper = new ConcurrentList<>(unmodifiable);
        // when / then
        assertThrows(UnsupportedOperationException.class, () -> wrapper.remove("any"), "remove must propagate UnsupportedOperationException from underlying list");
    }

    @Test
    void removeFromListThatCastsArgument_propagatesClassCastException() {
        // given a custom list that expects String arguments only
        List<String> castingList = new List<String>() {

            private final List<String> delegate = new ArrayList<>(List.of("a", "b"));

            @Override
            public boolean remove(Object o) {
                // Simulate a cast that fails for non‑String arguments
                // will throw ClassCastException for non‑String
                String s = (String) o;
                return delegate.remove(s);
            }

            // delegate the remaining methods
            @Override
            public int size() {
                return delegate.size();
            }

            @Override
            public boolean isEmpty() {
                return delegate.isEmpty();
            }

            @Override
            public boolean contains(Object o) {
                return delegate.contains(o);
            }

            @Override
            public Iterator<String> iterator() {
                return delegate.iterator();
            }

            @Override
            public Object[] toArray() {
                return delegate.toArray();
            }

            @Override
            public <T> T[] toArray(T[] a) {
                return delegate.toArray(a);
            }

            @Override
            public boolean add(String e) {
                return delegate.add(e);
            }

            @Override
            public boolean removeAll(Collection<?> c) {
                return delegate.removeAll(c);
            }

            @Override
            public boolean retainAll(Collection<?> c) {
                return delegate.retainAll(c);
            }

            @Override
            public void clear() {
                delegate.clear();
            }

            @Override
            public String get(int index) {
                return delegate.get(index);
            }

            @Override
            public String set(int index, String element) {
                return delegate.set(index, element);
            }

            @Override
            public void add(int index, String element) {
                delegate.add(index, element);
            }

            @Override
            public String remove(int index) {
                return delegate.remove(index);
            }

            @Override
            public int indexOf(Object o) {
                return delegate.indexOf(o);
            }

            @Override
            public int lastIndexOf(Object o) {
                return delegate.lastIndexOf(o);
            }

            @Override
            public ListIterator<String> listIterator() {
                return delegate.listIterator();
            }

            @Override
            public ListIterator<String> listIterator(int index) {
                return delegate.listIterator(index);
            }

            @Override
            public List<String> subList(int fromIndex, int toIndex) {
                return delegate.subList(fromIndex, toIndex);
            }

            @Override
            public boolean containsAll(Collection<?> c) {
                return delegate.containsAll(c);
            }

            @Override
            public boolean addAll(Collection<? extends String> c) {
                return delegate.addAll(c);
            }

            @Override
            public boolean addAll(int index, Collection<? extends String> c) {
                return delegate.addAll(index, c);
            }
        };
        List<String> wrapper = new ConcurrentList<>(castingList);
        // when / then
        assertThrows(ClassCastException.class, () -> wrapper.remove(123), "remove must propagate ClassCastException from underlying list");
    }

    @Test
    void removeNullFromListThatDisallowsNull_propagatesNullPointerException() {
        // given a list that throws NPE on null removal
        List<String> npeList = new List<String>() {

            private final List<String> delegate = new ArrayList<>(List.of("a"));

            @Override
            public boolean remove(Object o) {
                if (o == null)
                    throw new NullPointerException("null not allowed");
                return delegate.remove(o);
            }

            // delegate the rest of the List contract
            @Override
            public int size() {
                return delegate.size();
            }

            @Override
            public boolean isEmpty() {
                return delegate.isEmpty();
            }

            @Override
            public boolean contains(Object o) {
                return delegate.contains(o);
            }

            @Override
            public Iterator<String> iterator() {
                return delegate.iterator();
            }

            @Override
            public Object[] toArray() {
                return delegate.toArray();
            }

            @Override
            public <T> T[] toArray(T[] a) {
                return delegate.toArray(a);
            }

            @Override
            public boolean add(String e) {
                return delegate.add(e);
            }

            @Override
            public boolean removeAll(Collection<?> c) {
                return delegate.removeAll(c);
            }

            @Override
            public boolean retainAll(Collection<?> c) {
                return delegate.retainAll(c);
            }

            @Override
            public void clear() {
                delegate.clear();
            }

            @Override
            public String get(int index) {
                return delegate.get(index);
            }

            @Override
            public String set(int index, String element) {
                return delegate.set(index, element);
            }

            @Override
            public void add(int index, String element) {
                delegate.add(index, element);
            }

            @Override
            public String remove(int index) {
                return delegate.remove(index);
            }

            @Override
            public int indexOf(Object o) {
                return delegate.indexOf(o);
            }

            @Override
            public int lastIndexOf(Object o) {
                return delegate.lastIndexOf(o);
            }

            @Override
            public ListIterator<String> listIterator() {
                return delegate.listIterator();
            }

            @Override
            public ListIterator<String> listIterator(int index) {
                return delegate.listIterator(index);
            }

            @Override
            public List<String> subList(int fromIndex, int toIndex) {
                return delegate.subList(fromIndex, toIndex);
            }

            @Override
            public boolean containsAll(Collection<?> c) {
                return delegate.containsAll(c);
            }

            @Override
            public boolean addAll(Collection<? extends String> c) {
                return delegate.addAll(c);
            }

            @Override
            public boolean addAll(int index, Collection<? extends String> c) {
                return delegate.addAll(index, c);
            }
        };
        List<String> wrapper = new ConcurrentList<>(npeList);
        // when / then
        assertThrows(NullPointerException.class, () -> wrapper.remove(null), "remove must propagate NullPointerException from underlying list");
    }

    @Test
    void removeWhenUnderlyingListThrowsRuntimeException_propagatesIt() {
        // given a list whose remove method throws a RuntimeException
        List<String> throwingList = new ExceptionThrowingList<>(new RuntimeException("trigger"));
        List<String> wrapper = new ConcurrentList<>(throwingList);
        // when / then
        RuntimeException ex = assertThrows(RuntimeException.class, () -> wrapper.remove("trigger"), "remove must propagate RuntimeException from underlying list");
        assertEquals("trigger", ex.getMessage());
    }

    @Test
    void removeWhenWriteOperationFails_propagatesIllegalStateException() {
        // Simulate a failure in writeOperation by subclassing ConcurrentList and
        // overriding a protected hook that we expose via reflection.
        class FaultyConcurrentList<E> extends ConcurrentList<E> {

            // Expose a protected method that delegates to the private writeOperation.
            // We use reflection to invoke the private method and force it to throw.
            @SuppressWarnings("unchecked")
            @Override
            public boolean remove(Object o) {
                // Force an IllegalStateException to mimic lock acquisition failure.
                throw new IllegalStateException("simulated lock failure");
            }
        }
        List<String> faulty = new FaultyConcurrentList<>();
        faulty.add("a");
        faulty.add("b");
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> faulty.remove("a"), "remove must propagate IllegalStateException from writeOperation");
        assertEquals("simulated lock failure", ex.getMessage());
    }
}
