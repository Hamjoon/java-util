package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@ExtendWith(MockitoExtension.class)
class ConcurrentSet_clear_Test {

    @Test
    void testClearRemovesAllElements() {
        // given: a ConcurrentSet containing several elements
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList(1, 2, 3, 4, 5));
        assertEquals(5, set.size(), "Pre‑condition: set should contain 5 elements");
        assertFalse(set.isEmpty(), "Pre‑condition: set should not be empty");
        // when: clear() is invoked
        set.clear();
        // then: the set must be empty and size must be zero
        assertTrue(set.isEmpty(), "Set should be empty after clear()");
        assertEquals(0, set.size(), "Set size should be 0 after clear()");
        // also verify that contains returns false for previously present elements
        assertFalse(set.contains(1), "Set should not contain element 1 after clear()");
        assertFalse(set.contains(5), "Set should not contain element 5 after clear()");
    }

    @Test
    void testClearOnAlreadyEmptySet() {
        // given: an empty ConcurrentSet
        ConcurrentSet<String> set = new ConcurrentSet<>();
        assertEquals(0, set.size(), "Pre‑condition: set size is 0");
        assertTrue(set.isEmpty(), "Pre‑condition: set is empty");
        // when: clear() is called
        set.clear();
        // then: still empty and no exception
        assertTrue(set.isEmpty(), "Set should remain empty after clear()");
        assertEquals(0, set.size(), "Size should remain 0 after clear()");
    }

    @Test
    void testClearAfterAddAll() {
        // given: a set populated via addAll()
        ConcurrentSet<String> set = new ConcurrentSet<>();
        List<String> elements = Arrays.asList("a", "b", "c");
        set.addAll(elements);
        assertEquals(3, set.size());
        // when
        set.clear();
        // then
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    @Test
    void testClearAfterIteratorRemove() {
        // given: a set with a single element
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        set.add(42);
        assertEquals(1, set.size());
        // when: remove via iterator
        Iterator<Integer> it = set.iterator();
        assertTrue(it.hasNext());
        it.next();
        // now the set is empty
        it.remove();
        // then: set is empty before clear
        assertTrue(set.isEmpty());
        // when: clear() is called on already empty set after iterator removal
        set.clear();
        // then: still empty, no ConcurrentModificationException
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    @Test
    void testClearAfterRemoveOperations() {
        // given: a set with several elements
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList(1, 2, 3, 4, 5));
        // remove a subset
        set.remove(2);
        set.remove(4);
        assertEquals(3, set.size());
        // when
        set.clear();
        // then
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    @Test
    void testClearAfterRetainAllResultsInEmpty() {
        // given: a set with elements
        ConcurrentSet<String> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList("x", "y", "z"));
        // retain none of them
        set.retainAll(Collections.emptyList());
        assertTrue(set.isEmpty());
        // when
        set.clear();
        // then
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    @Test
    void testClearAfterRemoveAllEmptiesSet() {
        // given: a set with elements
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList(10, 20, 30));
        // remove all
        set.removeAll(Arrays.asList(10, 20, 30));
        assertTrue(set.isEmpty());
        // when
        set.clear();
        // then
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    @Test
    void testSubsequentAddWorksAfterClear() {
        // given: a populated set
        ConcurrentSet<String> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList("first", "second"));
        assertEquals(2, set.size());
        // when
        set.clear();
        set.add("new");
        // then
        assertEquals(1, set.size());
        assertTrue(set.contains("new"));
    }

    @Test
    void testMultipleReferencesSeeSameEmptyState() {
        // given: two references to the same ConcurrentSet instance
        ConcurrentSet<Integer> ref1 = new ConcurrentSet<>();
        // same underlying object
        ConcurrentSet<Integer> ref2 = ref1;
        ref1.addAll(Arrays.asList(1, 2, 3));
        assertEquals(3, ref1.size());
        assertEquals(3, ref2.size());
        // when: clear via second reference
        ref2.clear();
        // then: both see empty set
        assertTrue(ref1.isEmpty());
        assertTrue(ref2.isEmpty());
        assertEquals(0, ref1.size());
        assertEquals(0, ref2.size());
    }

    @Test
    void testClearHighVolumeElements() {
        // given: 10,000 distinct integers
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        int count = 10_000;
        for (int i = 0; i < count; i++) {
            set.add(i);
        }
        assertEquals(count, set.size());
        // when
        set.clear();
        // then
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    @Test
    void testClearWhileAnotherThreadAddsElements() throws InterruptedException {
        // given: a shared ConcurrentSet and a background adder thread
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        AtomicBoolean running = new AtomicBoolean(true);
        Thread adder = new Thread(() -> {
            int i = 0;
            while (running.get()) {
                set.add(i++);
                // tiny pause to avoid hogging CPU
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ignored) {
                }
            }
        });
        adder.start();
        // let the adder thread add a few elements
        Thread.sleep(50);
        // when: clear is called while other thread is adding
        set.clear();
        // stop the adder thread
        running.set(false);
        adder.join();
        // then: after clear the set must be empty (any later adds are ignored for this assertion)
        assertTrue(set.isEmpty(), "Set should be empty after clear even with concurrent adds");
        assertEquals(0, set.size());
    }

    @Test
    void testConcurrentClearWithIteratorRemoval() throws InterruptedException {
        // given: a set populated with several elements
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        for (int i = 0; i < 100; i++) {
            set.add(i);
        }
        assertEquals(100, set.size());
        // Thread 1: iterates and removes elements until empty
        Thread iteratorThread = new Thread(() -> {
            try {
                Iterator<Integer> it = set.iterator();
                while (it.hasNext()) {
                    it.next();
                    it.remove();
                }
            } catch (Exception e) {
                // Any exception should cause the test to fail
                fail("Iterator thread threw exception: " + e);
            }
        });
        // Thread 2: calls clear after a short delay
        Thread clearThread = new Thread(() -> {
            try {
                Thread.sleep(10);
                set.clear();
            } catch (Exception e) {
                fail("Clear thread threw exception: " + e);
            }
        });
        iteratorThread.start();
        clearThread.start();
        iteratorThread.join();
        clearThread.join();
        // then: set must be empty and no exception should have escaped
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    @Test
    void testClearWhenInternalSetIsNullThrowsNPE() throws Exception {
        // given: a normal ConcurrentSet instance
        ConcurrentSet<String> set = new ConcurrentSet<>();
        // use reflection to set the private 'set' field to null
        Field field = ConcurrentSet.class.getDeclaredField("set");
        field.setAccessible(true);
        field.set(set, null);
        // when / then: clear() should throw NullPointerException
        assertThrows(NullPointerException.class, set::clear, "clear() should throw NPE when internal set is null");
    }

    @Test
    void testClearPropagatesRuntimeExceptionFromUnderlyingSet() throws Exception {
        // given: a ConcurrentSet whose internal set throws IllegalStateException on clear()
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        // custom Set implementation that throws on clear()
        Set<Object> throwingSet = new AbstractSet<Object>() {

            @Override
            public Iterator<Object> iterator() {
                return Collections.emptyIterator();
            }

            @Override
            public int size() {
                return 0;
            }

            @Override
            public void clear() {
                throw new IllegalStateException("forced exception");
            }
        };
        // replace internal field via reflection
        Field field = ConcurrentSet.class.getDeclaredField("set");
        field.setAccessible(true);
        field.set(set, throwingSet);
        // when / then: the exception should propagate
        IllegalStateException ex = assertThrows(IllegalStateException.class, set::clear, "clear() should propagate IllegalStateException from underlying set");
        assertEquals("forced exception", ex.getMessage());
    }

    @Test
    void testClearAfterAddAllAndRemoveAllSequence() {
        // given: a set populated via addAll()
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        List<Integer> initial = Arrays.asList(1, 2, 3, 4, 5);
        set.addAll(initial);
        assertEquals(5, set.size());
        // when: removeAll the same collection
        set.removeAll(initial);
        assertTrue(set.isEmpty());
        // then: clear should keep it empty without error
        set.clear();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }
}
