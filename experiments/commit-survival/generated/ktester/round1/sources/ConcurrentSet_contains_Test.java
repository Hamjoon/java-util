package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConcurrentSet_contains_Test {

    private ConcurrentSet<String> set;

    /**
     * A minimal {@link Set} implementation that throws {@link IllegalStateException}
     * from every method. Used to verify that {@link ConcurrentSet#contains(Object)}
     * propagates unexpected runtime exceptions from its delegate.
     */
    private static class ExplodingSet<E> implements Set<E> {

        private final RuntimeException exception = new IllegalStateException("exploding set");

        private void explode() {
            throw exception;
        }

        @Override
        public int size() {
            explode();
            return 0;
        }

        @Override
        public boolean isEmpty() {
            explode();
            return false;
        }

        @Override
        public boolean contains(Object o) {
            explode();
            return false;
        }

        @Override
        public Iterator<E> iterator() {
            explode();
            return null;
        }

        @Override
        public Object[] toArray() {
            explode();
            return null;
        }

        @Override
        public <T> T[] toArray(T[] a) {
            explode();
            return null;
        }

        @Override
        public boolean add(E e) {
            explode();
            return false;
        }

        @Override
        public boolean remove(Object o) {
            explode();
            return false;
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            explode();
            return false;
        }

        @Override
        public boolean addAll(Collection<? extends E> c) {
            explode();
            return false;
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            explode();
            return false;
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            explode();
            return false;
        }

        @Override
        public void clear() {
            explode();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh instance for every test
        set = new ConcurrentSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear the set to help GC and avoid accidental cross‑test contamination
        if (set != null) {
            set.clear();
        }
    }

    @Test
    void containsShouldReturnFalseWhenSetIsEmpty() {
        assertFalse(set.contains("any"), "Empty set must not contain any element");
    }

    @Test
    void containsShouldDetectExistingElement() {
        set.add("alpha");
        assertTrue(set.contains("alpha"), "Set should contain the element that was added");
    }

    @Test
    void containsShouldReturnFalseForMissingElement() {
        set.addAll(Arrays.asList("a", "b", "c"));
        assertFalse(set.contains("z"), "Set should not contain an element that was never added");
    }

    @Test
    void containsShouldHandleNullWhenNullIsPresent() {
        set.add(null);
        assertTrue(set.contains(null), "Set must report true for a stored null element");
    }

    @Test
    void containsShouldHandleNullWhenNullIsAbsent() {
        set.addAll(Arrays.asList("x", "y"));
        assertFalse(set.contains(null), "Set must report false for null when it has not been added");
    }

    @Test
    void containsShouldWorkWithDifferentObjectTypes() {
        ConcurrentSet<Object> objSet = new ConcurrentSet<>();
        objSet.add(123);
        objSet.add("string");
        objSet.add(new HashSet<>(Collections.singletonList("inner")));
        assertTrue(objSet.contains(123), "Should contain the Integer element");
        assertTrue(objSet.contains("string"), "Should contain the String element");
        assertTrue(objSet.contains(new HashSet<>(Collections.singletonList("inner"))), "Should contain an equal Set element");
        assertFalse(objSet.contains(456), "Should not contain an Integer that was not added");
    }

    @Test
    void containsShouldBeConsistentAfterConcurrentModifications() throws InterruptedException {
        // Populate the set with a known range
        for (int i = 0; i < 1000; i++) {
            set.add(String.valueOf(i));
        }
        // Start threads that add and remove elements concurrently
        Thread adder = new Thread(() -> {
            for (int i = 1000; i < 2000; i++) {
                set.add(String.valueOf(i));
            }
        });
        Thread remover = new Thread(() -> {
            for (int i = 0; i < 500; i++) {
                set.remove(String.valueOf(i));
            }
        });
        adder.start();
        remover.start();
        adder.join();
        remover.join();
        // Verify that elements that should remain are reported correctly
        for (int i = 500; i < 2000; i++) {
            assertTrue(set.contains(String.valueOf(i)), "After concurrent modifications, set should contain element " + i);
        }
        // Elements that were removed must not be reported as present
        for (int i = 0; i < 500; i++) {
            assertFalse(set.contains(String.valueOf(i)), "After concurrent modifications, set should not contain element " + i);
        }
    }

    @Test
    void containsShouldThrowClassCastExceptionWhenWrongTypeProvided() {
        set.add("string");
        // The generic type of the set is String; passing an Integer forces a cast failure.
        assertThrows(ClassCastException.class, () -> set.contains(123), "Calling contains with a value of the wrong generic type must propagate ClassCastException");
    }

    @Test
    void containsShouldReturnTrueWhenDuplicateElementsAreAdded() {
        // Adding the same logical element several times – Set semantics should keep only one.
        set.add("dup");
        // duplicate, should be ignored
        set.add("dup");
        set.addAll(Arrays.asList("dup", "dup"));
        assertTrue(set.contains("dup"), "Set must report true for an element that was added, even if added repeatedly");
        assertEquals(1, set.size(), "Duplicate additions must not increase the size of the set");
    }

    @Test
    void containsShouldReturnFalseWhenElementIsAbsentInSingleElementSet() {
        set.add("single");
        assertFalse(set.contains("other"), "A set containing only \"single\" must not report true for a different element");
    }

    @Test
    void containsShouldReturnTrueForMixedNullAndNonNullElements() {
        set.add(null);
        set.add("nonNull");
        assertAll(() -> assertTrue(set.contains(null), "Null sentinel must be found when present"), () -> assertTrue(set.contains("nonNull"), "Non‑null element must be found when present"));
    }

    @Test
    void containsShouldPropagateExceptionFromUnderlyingSet() throws Exception {
        // Create a mock Set whose contains method always throws IllegalStateException
        @SuppressWarnings("unchecked")
        Set<Object> explodingSet = mock(Set.class);
        when(explodingSet.contains(any())).thenThrow(new IllegalStateException("boom"));
        // Inject the mock into the ConcurrentSet instance via reflection
        Field field = ConcurrentSet.class.getDeclaredField("set");
        field.setAccessible(true);
        field.set(set, explodingSet);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> set.contains("trigger"), "The IllegalStateException from the underlying set must be propagated unchanged");
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void containsShouldThrowNullPointerExceptionWhenInternalSetIsNull() throws Exception {
        // Corrupt the internal state by setting the private field `set` to null
        Field field = ConcurrentSet.class.getDeclaredField("set");
        field.setAccessible(true);
        field.set(set, null);
        assertThrows(NullPointerException.class, () -> set.contains("anyValue"), "If the internal backing set is null, contains must throw NullPointerException");
    }

    @Test
    void containsShouldReturnFalseWhenNullIsAbsentAndNoClassCastOccurs() {
        // No null sentinel stored; casting null to T does NOT throw ClassCastException.
        // The method should simply delegate to the underlying set which returns false.
        assertFalse(set.contains(null), "Calling contains(null) on an empty set should return false without throwing");
    }

    @Test
    void containsShouldPropagateExceptionFromExplodingSet() throws Exception {
        // Replace the internal set with an instance that always throws IllegalStateException
        ExplodingSet<Object> exploding = new ExplodingSet<>();
        Field field = ConcurrentSet.class.getDeclaredField("set");
        field.setAccessible(true);
        field.set(set, exploding);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> set.contains("any"), "Exception from an ExplodingSet must be propagated");
        assertEquals("exploding set", ex.getMessage());
    }
}
