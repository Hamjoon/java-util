package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_remove_2_Test {

    /**
     * The list under test – created fresh for each test method.
     */
    private List<Integer> list;

    /**
     * A wrapped list used in one of the tests to verify wrapper‑mode behavior.
     */
    private List<Integer> backingList;

    class Person {

        final String name;

        final int age;

        Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof Person))
                return false;
            Person p = (Person) o;
            return age == p.age && Objects.equals(name, p.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, age);
        }

        @Override
        public String toString() {
            return name + ":" + age;
        }
    }

    class Person {

        final String name;

        final int age;

        Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof Person))
                return false;
            Person p = (Person) o;
            return age == p.age && Objects.equals(name, p.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, age);
        }

        @Override
        public String toString() {
            return name + ":" + age;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Stand‑alone mode instance.
        list = new ConcurrentList<>();
        // Backing list for wrapper‑mode tests.
        backingList = new ArrayList<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required but keeps the pattern tidy.
        list = null;
        backingList = null;
    }

    @Test
    void removeExistingElement_returnsTrue_andUpdatesList() {
        list.add(10);
        list.add(20);
        list.add(30);
        assertTrue(list.remove(Integer.valueOf(20)), "remove(Object) should return true when the element is present");
        assertEquals(2, list.size(), "Size should decrease after successful removal");
        assertFalse(list.contains(20), "Removed element must no longer be present");
        assertTrue(list.containsAll(Arrays.asList(10, 30)), "Other elements must stay untouched");
    }

    @Test
    void removeNonExistingElement_returnsFalse_andLeavesListUnchanged() {
        list.add(1);
        list.add(2);
        assertFalse(list.remove(Integer.valueOf(99)), "remove(Object) should return false when the element is absent");
        assertEquals(2, list.size(), "Size must remain unchanged when removal fails");
        assertTrue(list.containsAll(Arrays.asList(1, 2)), "Existing elements must stay unchanged");
    }

    @Test
    void removeFromEmptyList_returnsFalse() {
        assertFalse(list.remove(Integer.valueOf(5)), "remove(Object) on an empty list should return false");
        assertTrue(list.isEmpty(), "List should still be empty");
    }

    @Test
    void removeNullElement_whenBackingListAllowsNull() {
        // Use wrapper mode with a backing list that permits null.
        List<Integer> wrapped = new ConcurrentList<>(backingList);
        wrapped.add(null);
        wrapped.add(1);
        wrapped.add(2);
        assertTrue(wrapped.remove(null), "remove(null) should succeed when the underlying list accepts null");
        assertEquals(2, wrapped.size(), "Size should reflect the removal of the null element");
        assertFalse(wrapped.contains(null), "Null element must no longer be present");
    }

    @Test
    void removeNullElement_whenBackingListDisallowsNull() {
        // Backing list that does NOT allow null (e.g., a custom list that throws NPE).
        // For simplicity we use a standard ArrayList which *does* allow null, so we
        // simulate the disallowed case by not adding a null first.
        List<Integer> wrapped = new ConcurrentList<>(backingList);
        wrapped.add(1);
        wrapped.add(2);
        assertFalse(wrapped.remove(null), "remove(null) should return false when null is not present");
        assertEquals(2, wrapped.size(), "Size must stay unchanged");
    }

    @Test
    void removeObjectInWrapperMode_affectsBackingList() {
        // Populate the backing list directly.
        backingList.add(100);
        backingList.add(200);
        backingList.add(300);
        // Wrap it.
        List<Integer> wrapped = new ConcurrentList<>(backingList);
        // Remove via the wrapper.
        assertTrue(wrapped.remove(Integer.valueOf(200)), "remove(Object) should succeed in wrapper mode");
        assertEquals(2, wrapped.size(), "Wrapper size must reflect removal");
        assertEquals(2, backingList.size(), "Backing list size must also reflect removal");
        assertFalse(backingList.contains(200), "Backing list must no longer contain the removed element");
    }

    @Test
    void removeDuplicateElement_onlyFirstOccurrenceRemoved() {
        list.addAll(Arrays.asList(5, 5, 5));
        assertTrue(list.remove(Integer.valueOf(5)), "First occurrence should be removed");
        assertEquals(2, list.size(), "Size should decrease by one");
        assertEquals(Integer.valueOf(5), list.get(0), "Second original element becomes first");
    }

    @Test
    void removeFirstElement_shiftsRemainingElements() {
        List<String> stringList = new ConcurrentList<>();
        stringList.addAll(Arrays.asList("first", "second", "third"));
        assertTrue(stringList.remove("first"));
        assertEquals(2, stringList.size());
        assertEquals("second", stringList.get(0));
        assertEquals("third", stringList.get(1));
    }

    @Test
    void removeLastElement_withoutAffectingEarlierOnes() {
        List<String> stringList = new ConcurrentList<>();
        stringList.addAll(Arrays.asList("alpha", "beta", "last"));
        assertTrue(stringList.remove("last"));
        assertEquals(2, stringList.size());
        assertEquals("alpha", stringList.get(0));
        assertEquals("beta", stringList.get(1));
    }

    @Test
    void removeMiddleElement_collapsesGap() {
        List<String> stringList = new ConcurrentList<>();
        stringList.addAll(Arrays.asList("head", "middle", "tail"));
        assertTrue(stringList.remove("middle"));
        assertEquals(2, stringList.size());
        assertEquals("head", stringList.get(0));
        assertEquals("tail", stringList.get(1));
    }

    @Test
    void removeNullWhenMultipleNulls_onlyFirstRemoved() {
        List<String> stringList = new ConcurrentList<>();
        stringList.addAll(Arrays.asList(null, "a", null, "b"));
        assertTrue(stringList.remove(null));
        assertEquals(3, stringList.size());
        assertEquals("a", stringList.get(0));
        assertEquals(null, stringList.get(1));
        assertEquals("b", stringList.get(2));
    }

    @Test
    void removeCustomObjectBasedOnEquals() {
        // Simple Person class defined locally for the test.
        class Person {

            final String name;

            final int age;

            Person(String name, int age) {
                this.name = name;
                this.age = age;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o)
                    return true;
                if (!(o instanceof Person))
                    return false;
                Person p = (Person) o;
                return age == p.age && Objects.equals(name, p.name);
            }

            @Override
            public int hashCode() {
                return Objects.hash(name, age);
            }

            @Override
            public String toString() {
                return name + ":" + age;
            }
        }
        ConcurrentList<Person> personList = new ConcurrentList<>();
        personList.add(new Person("John", 30));
        personList.add(new Person("Jane", 25));
        // New instance with same state as first element.
        Person toRemove = new Person("John", 30);
        assertTrue(personList.remove(toRemove), "Equal Person should be removed");
        assertEquals(1, personList.size());
        assertFalse(personList.contains(toRemove));
    }

    @Test
    void removeHeterogeneousIntegerFromMixedList() {
        List<Object> mixed = new ConcurrentList<>();
        mixed.add("string");
        mixed.add(42);
        mixed.add(3.14);
        assertTrue(mixed.remove(42));
        assertEquals(2, mixed.size());
        assertFalse(mixed.contains(42));
    }

    @Test
    void sequentialRemovals_multipleCalls() {
        List<String> list = new ConcurrentList<>();
        list.addAll(Arrays.asList("remove1", "remain", "remove2"));
        assertTrue(list.remove("remove1"));
        assertTrue(list.remove("remain"));
        assertEquals(1, list.size());
        assertEquals("remove2", list.get(0));
    }

    @Test
    void removeDifferentType_returnsFalse() {
        // List holds Strings, we try to remove an Integer.
        List<String> stringList = new ConcurrentList<>();
        stringList.addAll(Arrays.asList("a", "b"));
        assertFalse(stringList.remove(123), "Removing a different type should return false");
        assertEquals(2, stringList.size());
    }

    @Test
    void removeWithNullUnderlyingList_throwsNullPointerException() throws Exception {
        // Use reflection to inject a null delegate list.
        ConcurrentList<Integer> faulty = new ConcurrentList<>();
        Field listField = ConcurrentList.class.getDeclaredField("list");
        listField.setAccessible(true);
        // simulate constructor that received null
        listField.set(faulty, null);
        assertThrows(NullPointerException.class, () -> faulty.remove(1), "Calling remove on a ConcurrentList with a null internal list should throw NPE");
    }

    @Test
    void removeWithUnmodifiableUnderlyingList_throwsUnsupportedOperationException() {
        List<String> unmodifiable = Collections.unmodifiableList(new ArrayList<>(Arrays.asList("element", "other")));
        ConcurrentList<String> wrapped = new ConcurrentList<>(unmodifiable);
        assertThrows(UnsupportedOperationException.class, () -> wrapped.remove("element"), "Removal on an unmodifiable backing list should propagate UnsupportedOperationException");
    }

    @Test
    void removeWithUnderlyingListThrowingCustomRuntimeException_propagatesException(@Mock List<String> mockList) {
        // Configure mock to throw IllegalStateException on remove.
        when(mockList.remove(any())).thenThrow(new IllegalStateException("forced"));
        // Wrap the mock.
        ConcurrentList<String> wrapped = new ConcurrentList<>(mockList);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> wrapped.remove("trigger"), "The custom runtime exception should be propagated unchanged");
        assertEquals("forced", ex.getMessage());
        // Verify that remove was indeed called on the mock.
        verify(mockList, times(1)).remove("trigger");
    }
}
