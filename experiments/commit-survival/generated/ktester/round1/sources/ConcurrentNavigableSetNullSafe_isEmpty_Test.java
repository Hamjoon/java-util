package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mockito;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableSetNullSafe_isEmpty_Test {

    /**
     * Instance used by the majority of tests – re‑initialised before each test.
     */
    private NavigableSet<String> set;

    @BeforeEach
    void setupBeforeEach() {
        // Use the default (natural ordering) constructor for a fresh set.
        set = new ConcurrentNavigableSetNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear the set to aid GC and to guarantee isolation.
        if (set != null) {
            set.clear();
        }
    }

    @Test
    void isEmpty_onNewDefaultInstance_returnsTrue() {
        // GIVEN – a freshly created set (handled in @BeforeEach)
        // WHEN
        boolean empty = set.isEmpty();
        // THEN
        assertTrue(empty, "A newly constructed set should be empty");
    }

    @Test
    void isEmpty_afterAddingElements_returnsFalse() {
        // GIVEN
        assertTrue(set.isEmpty(), "Pre‑condition: set should start empty");
        // WHEN
        set.add("apple");
        set.add(null);
        // THEN
        assertFalse(set.isEmpty(), "Set should report non‑empty after adding elements");
        assertEquals(2, set.size(), "Size should reflect the added elements");
    }

    @Test
    void isEmpty_afterClear_returnsTrue() {
        // GIVEN
        set.add("apple");
        set.add("banana");
        assertFalse(set.isEmpty(), "Set should be non‑empty after additions");
        // WHEN
        set.clear();
        // THEN
        assertTrue(set.isEmpty(), "Set should be empty after clear()");
        assertEquals(0, set.size(), "Size must be zero after clear()");
    }

    @Test
    void isEmpty_withComparatorConstructor_behavesIdentically() {
        // GIVEN – a set built with a custom comparator (by string length)
        Comparator<String> lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        NavigableSet<String> lengthSet = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
        assertTrue(lengthSet.isEmpty(), "New comparator‑based set should be empty");
        // WHEN – add a few values
        lengthSet.add("kiwi");
        lengthSet.add(null);
        lengthSet.add("banana");
        // THEN
        assertFalse(lengthSet.isEmpty(), "Set should not be empty after adding elements");
        assertEquals(3, lengthSet.size());
    }

    @Test
    void isEmpty_withCollectionConstructor_reflectsInitialContent() {
        // GIVEN – a collection containing null and two strings
        Collection<String> initial = Arrays.asList("apple", null, "cherry");
        NavigableSet<String> collSet = new ConcurrentNavigableSetNullSafe<>(initial);
        // THEN – the set should not be empty
        assertFalse(collSet.isEmpty(), "Set constructed from a non‑empty collection must not be empty");
        assertEquals(3, collSet.size());
        // WHEN – remove all elements
        collSet.remove("apple");
        collSet.remove(null);
        collSet.remove("cherry");
        // THEN – it becomes empty again
        assertTrue(collSet.isEmpty(), "Set should become empty after removing all elements");
        assertEquals(0, collSet.size());
    }

    @Test
    void isEmpty_multipleThreads_concurrentAdditionsMaintainCorrectState() throws InterruptedException {
        // GIVEN – a set that will be accessed concurrently
        final NavigableSet<Integer> concurrentSet = new ConcurrentNavigableSetNullSafe<>();
        // WHEN – start two threads that each add 500 distinct integers
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 500; i++) {
                concurrentSet.add(i);
            }
        });
        Thread t2 = new Thread(() -> {
            for (int i = 500; i < 1000; i++) {
                concurrentSet.add(i);
            }
        });
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        // THEN – the set must report non‑empty and contain exactly 1000 elements
        assertFalse(concurrentSet.isEmpty(), "Set should be non‑empty after concurrent additions");
        assertEquals(1000, concurrentSet.size());
        assertTrue(concurrentSet.containsAll(Arrays.asList(0, 999)));
    }

    @Test
    void isEmpty_whenInternalSetIsNull_throwsNullPointerException() throws Exception {
        // GIVEN – a freshly constructed instance
        ConcurrentNavigableSetNullSafe<String> unsafe = new ConcurrentNavigableSetNullSafe<>();
        // Force the private final field 'internalSet' to null via reflection
        Field internalSetField = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        internalSetField.setAccessible(true);
        internalSetField.set(unsafe, null);
        // THEN – invoking isEmpty() must throw NPE
        assertThrows(NullPointerException.class, unsafe::isEmpty, "Calling isEmpty() when internalSet is null should throw NullPointerException");
    }

    @Test
    void isEmpty_whenDelegateThrowsIllegalStateException_propagatesException() throws Exception {
        // GIVEN – a mock NavigableSet whose isEmpty() throws IllegalStateException
        @SuppressWarnings("unchecked")
        NavigableSet<Object> mockDelegate = (NavigableSet<Object>) Mockito.mock(NavigableSet.class);
        Mockito.when(mockDelegate.isEmpty()).thenThrow(new IllegalStateException("delegate failure"));
        // Create a normal instance and replace its internalSet with the mock
        ConcurrentNavigableSetNullSafe<String> wrapper = new ConcurrentNavigableSetNullSafe<>();
        Field internalSetField = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        internalSetField.setAccessible(true);
        internalSetField.set(wrapper, mockDelegate);
        // THEN – the exception from the delegate must propagate unchanged
        IllegalStateException thrown = assertThrows(IllegalStateException.class, wrapper::isEmpty, "Exception from internal delegate should propagate");
        assertEquals("delegate failure", thrown.getMessage());
    }
}
