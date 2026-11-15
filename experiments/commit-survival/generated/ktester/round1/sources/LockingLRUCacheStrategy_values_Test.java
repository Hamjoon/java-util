package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@ExtendWith(MockitoExtension.class)
class LockingLRUCacheStrategy_values_Test {

    private static final int CAPACITY = 5;

    private LockingLRUCacheStrategy<String, Integer> cache;

    class ExplodingCache extends LockingLRUCacheStrategy<String, Integer> {

        ExplodingCache(int capacity) {
            super(capacity);
        }

        @Override
        public Collection<Integer> values() {
            lock.lock();
            try {
                Map<String, Integer> map = new LinkedHashMap<String, Integer>() {

                    @Override
                    public Integer put(String k, Integer v) {
                        if (!isEmpty()) {
                            throw new RuntimeException("Injected failure");
                        }
                        return super.put(k, v);
                    }
                };
                for (Node<String, Integer> node = head.next; node != tail; node = node.next) {
                    // will throw on second iteration
                    map.put(node.key, node.value);
                }
                return map.values();
            } finally {
                lock.unlock();
            }
        }
    }

    class CorruptHeadCache extends LockingLRUCacheStrategy<String, Integer> {

        CorruptHeadCache(int capacity) {
            super(capacity);
        }

        void corruptHead() throws Exception {
            Field headField = LockingLRUCacheStrategy.class.getDeclaredField("head");
            headField.setAccessible(true);
            headField.set(this, null);
        }
    }

    class CorruptTailCache extends LockingLRUCacheStrategy<String, Integer> {

        CorruptTailCache(int capacity) {
            super(capacity);
        }

        void corruptTail() throws Exception {
            Field tailField = LockingLRUCacheStrategy.class.getDeclaredField("tail");
            tailField.setAccessible(true);
            tailField.set(this, null);
        }
    }

    class BrokenLinkCache extends LockingLRUCacheStrategy<String, Integer> {

        BrokenLinkCache(int capacity) {
            super(capacity);
        }

        void breakLink() throws Exception {
            Field headField = LockingLRUCacheStrategy.class.getDeclaredField("head");
            headField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Node<String, Integer> headNode = (Node<String, Integer>) headField.get(this);
            // head.next points to first real node; set its next to null
            Node<String, Integer> first = headNode.next;
            Field nextField = Node.class.getDeclaredField("next");
            nextField.setAccessible(true);
            nextField.set(first, null);
        }
    }

    class PutFailCache extends LockingLRUCacheStrategy<String, Integer> {

        PutFailCache(int capacity) {
            super(capacity);
        }

        @Override
        public Collection<Integer> values() {
            lock.lock();
            try {
                Map<String, Integer> map = new LinkedHashMap<String, Integer>() {

                    private boolean first = true;

                    @Override
                    public Integer put(String k, Integer v) {
                        if (!first) {
                            throw new RuntimeException("Map put failure");
                        }
                        first = false;
                        return super.put(k, v);
                    }
                };
                for (Node<String, Integer> node = head.next; node != tail; node = node.next) {
                    // throws on second iteration
                    map.put(node.key, node.value);
                }
                return map.values();
            } finally {
                lock.unlock();
            }
        }
    }

    class ExplodingCache extends LockingLRUCacheStrategy<String, Integer> {

        ExplodingCache(int capacity) {
            super(capacity);
        }

        @Override
        public Collection<Integer> values() {
            lock.lock();
            try {
                Map<String, Integer> map = new LinkedHashMap<String, Integer>() {

                    @Override
                    public Integer put(String k, Integer v) {
                        if (!isEmpty()) {
                            throw new RuntimeException("Injected failure");
                        }
                        return super.put(k, v);
                    }
                };
                for (Node<String, Integer> node = head.next; node != tail; node = node.next) {
                    // will throw on second iteration
                    map.put(node.key, node.value);
                }
                return map.values();
            } finally {
                lock.unlock();
            }
        }
    }

    class CorruptHeadCache extends LockingLRUCacheStrategy<String, Integer> {

        CorruptHeadCache(int capacity) {
            super(capacity);
        }

        void corruptHead() throws Exception {
            Field headField = LockingLRUCacheStrategy.class.getDeclaredField("head");
            headField.setAccessible(true);
            headField.set(this, null);
        }
    }

    class CorruptTailCache extends LockingLRUCacheStrategy<String, Integer> {

        CorruptTailCache(int capacity) {
            super(capacity);
        }

        void corruptTail() throws Exception {
            Field tailField = LockingLRUCacheStrategy.class.getDeclaredField("tail");
            tailField.setAccessible(true);
            tailField.set(this, null);
        }
    }

    class BrokenLinkCache extends LockingLRUCacheStrategy<String, Integer> {

        BrokenLinkCache(int capacity) {
            super(capacity);
        }

        void breakLink() throws Exception {
            Field headField = LockingLRUCacheStrategy.class.getDeclaredField("head");
            headField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Node<String, Integer> headNode = (Node<String, Integer>) headField.get(this);
            // head.next points to first real node; set its next to null
            Node<String, Integer> first = headNode.next;
            Field nextField = Node.class.getDeclaredField("next");
            nextField.setAccessible(true);
            nextField.set(first, null);
        }
    }

    class PutFailCache extends LockingLRUCacheStrategy<String, Integer> {

        PutFailCache(int capacity) {
            super(capacity);
        }

        @Override
        public Collection<Integer> values() {
            lock.lock();
            try {
                Map<String, Integer> map = new LinkedHashMap<String, Integer>() {

                    private boolean first = true;

                    @Override
                    public Integer put(String k, Integer v) {
                        if (!first) {
                            throw new RuntimeException("Map put failure");
                        }
                        first = false;
                        return super.put(k, v);
                    }
                };
                for (Node<String, Integer> node = head.next; node != tail; node = node.next) {
                    // throws on second iteration
                    map.put(node.key, node.value);
                }
                return map.values();
            } finally {
                lock.unlock();
            }
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh cache for each test
        cache = new LockingLRUCacheStrategy<>(CAPACITY);
        // Populate the cache with a deterministic set of entries.
        // The order of insertion will be the order returned by values()
        // because the internal linked list is traversed from head to tail.
        cache.put("one", 1);
        cache.put("two", 2);
        cache.put("three", 3);
        cache.put("four", 4);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the cache is cleared so that a subsequent test starts clean.
        cache.clear();
    }

    /**
     * Helper to obtain the private {@code lock} field from a cache instance.
     */
    private static Lock getLock(LockingLRUCacheStrategy<?, ?> cacheInstance) throws Exception {
        Field lockField = LockingLRUCacheStrategy.class.getDeclaredField("lock");
        lockField.setAccessible(true);
        return (Lock) lockField.get(cacheInstance);
    }

    @Test
    void values_returnsAllValuesInInsertionOrder() {
        // When
        Collection<Integer> values = cache.values();
        // Then
        assertNotNull(values, "values() should never return null");
        assertEquals(4, values.size(), "Unexpected number of values returned");
        // Convert to a list to verify ordering
        List<Integer> valueList = new ArrayList<>(values);
        List<Integer> expected = List.of(1, 2, 3, 4);
        assertEquals(expected, valueList, "Values are not in the expected insertion order");
    }

    @Test
    void values_reflectsCacheMutations() {
        // Remove an entry and add a new one
        cache.remove("two");
        cache.put("five", 5);
        Collection<Integer> values = cache.values();
        List<Integer> valueList = new ArrayList<>(values);
        // Expected order: one, three, four, five
        List<Integer> expected = List.of(1, 3, 4, 5);
        assertEquals(expected, valueList, "Values collection did not reflect recent mutations");
    }

    @Test
    void values_isThreadSafeUnderConcurrentAccess() throws InterruptedException {
        // This test exercises the lock used inside values().
        // We start a thread that holds the lock for a short period while the main thread
        // calls values(). The implementation must release the lock correctly.
        Thread holder = new Thread(() -> {
            // Acquire the internal lock directly via a public method that also locks.
            // The simplest way is to call put() which obtains the write lock.
            cache.put("six", 6);
            // Sleep while holding the lock to simulate contention.
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
        });
        holder.start();
        // Give the holder a moment to acquire the lock.
        Thread.sleep(10);
        // Main thread calls values(); it should wait for the lock and then return correct data.
        Collection<Integer> values = cache.values();
        // Ensure the background thread has finished.
        holder.join();
        List<Integer> valueList = new ArrayList<>(values);
        // The cache now contains: one, two, three, four, six (capacity is 5, so "five" was never added)
        List<Integer> expected = List.of(1, 2, 3, 4, 6);
        assertEquals(expected, valueList, "values() did not return the correct collection under contention");
    }

    @Test
    void values_emptyCache_returnsEmptyCollectionAndReleasesLock() throws Exception {
        // Arrange: clear the cache so it is empty
        cache.clear();
        // Act
        Collection<Integer> values = cache.values();
        // Assert collection is empty
        assertNotNull(values);
        assertTrue(values.isEmpty(), "Expected empty collection for empty cache");
        // Verify lock is not held after method returns
        assertFalse(getLock(cache).tryLock(), "Lock should be released after values()");
    }

    @Test
    void values_singleEntryCache_returnsSingleValueAndReleasesLock() throws Exception {
        // Arrange: new cache with one entry
        cache.clear();
        cache.put("key1", 42);
        // Act
        Collection<Integer> values = cache.values();
        // Assert
        assertEquals(1, values.size());
        assertEquals(List.of(42), new ArrayList<>(values));
        assertFalse(getLock(cache).tryLock(), "Lock must be released after values()");
    }

    @Test
    void values_multipleEntries_preservesInsertionOrderAndReleasesLock() throws Exception {
        // Arrange: specific order
        cache.clear();
        cache.put("A", 10);
        cache.put("B", 20);
        cache.put("C", 30);
        // Act
        Collection<Integer> values = cache.values();
        // Assert order
        assertEquals(List.of(10, 20, 30), new ArrayList<>(values));
        assertFalse(getLock(cache).tryLock(), "Lock must be released after values()");
    }

    @Test
    void values_lockReleasedWhenExceptionDuringIteration() throws Exception {
        // Subclass that injects RuntimeException after first put into the temporary map
        class ExplodingCache extends LockingLRUCacheStrategy<String, Integer> {

            ExplodingCache(int capacity) {
                super(capacity);
            }

            @Override
            public Collection<Integer> values() {
                lock.lock();
                try {
                    Map<String, Integer> map = new LinkedHashMap<String, Integer>() {

                        @Override
                        public Integer put(String k, Integer v) {
                            if (!isEmpty()) {
                                throw new RuntimeException("Injected failure");
                            }
                            return super.put(k, v);
                        }
                    };
                    for (Node<String, Integer> node = head.next; node != tail; node = node.next) {
                        // will throw on second iteration
                        map.put(node.key, node.value);
                    }
                    return map.values();
                } finally {
                    lock.unlock();
                }
            }
        }
        ExplodingCache exploding = new ExplodingCache(5);
        exploding.put("x", 1);
        exploding.put("y", 2);
        exploding.put("z", 3);
        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, exploding::values);
        assertEquals("Injected failure", ex.getMessage());
        // Ensure lock is released despite exception
        assertFalse(getLock(exploding).tryLock(), "Lock must be released after exception");
    }

    @Test
    void values_includesNullValuesAndReleasesLock() throws Exception {
        cache.clear();
        cache.put("nullKey", null);
        cache.put("valKey", 99);
        Collection<Integer> values = cache.values();
        assertEquals(List.of(null, 99), new ArrayList<>(values));
        assertFalse(getLock(cache).tryLock(), "Lock must be released after values()");
    }

    @Test
    void values_nullHead_throwsNPEAndReleasesLock() throws Exception {
        // Subclass to corrupt the internal 'head' reference
        class CorruptHeadCache extends LockingLRUCacheStrategy<String, Integer> {

            CorruptHeadCache(int capacity) {
                super(capacity);
            }

            void corruptHead() throws Exception {
                Field headField = LockingLRUCacheStrategy.class.getDeclaredField("head");
                headField.setAccessible(true);
                headField.set(this, null);
            }
        }
        CorruptHeadCache corrupt = new CorruptHeadCache(5);
        corrupt.put("a", 1);
        corrupt.corruptHead();
        NullPointerException npe = assertThrows(NullPointerException.class, corrupt::values);
        assertNotNull(npe);
        assertFalse(getLock(corrupt).tryLock(), "Lock must be released after NPE");
    }

    @Test
    void values_nullTail_throwsNPEAndReleasesLock() throws Exception {
        // Subclass to corrupt the internal 'tail' reference
        class CorruptTailCache extends LockingLRUCacheStrategy<String, Integer> {

            CorruptTailCache(int capacity) {
                super(capacity);
            }

            void corruptTail() throws Exception {
                Field tailField = LockingLRUCacheStrategy.class.getDeclaredField("tail");
                tailField.setAccessible(true);
                tailField.set(this, null);
            }
        }
        CorruptTailCache corrupt = new CorruptTailCache(5);
        corrupt.put("a", 1);
        corrupt.corruptTail();
        NullPointerException npe = assertThrows(NullPointerException.class, corrupt::values);
        assertNotNull(npe);
        assertFalse(getLock(corrupt).tryLock(), "Lock must be released after NPE");
    }

    @Test
    void values_brokenLink_throwsNPEAndReleasesLock() throws Exception {
        // Subclass that breaks the linked list after the first node
        class BrokenLinkCache extends LockingLRUCacheStrategy<String, Integer> {

            BrokenLinkCache(int capacity) {
                super(capacity);
            }

            void breakLink() throws Exception {
                Field headField = LockingLRUCacheStrategy.class.getDeclaredField("head");
                headField.setAccessible(true);
                @SuppressWarnings("unchecked")
                Node<String, Integer> headNode = (Node<String, Integer>) headField.get(this);
                // head.next points to first real node; set its next to null
                Node<String, Integer> first = headNode.next;
                Field nextField = Node.class.getDeclaredField("next");
                nextField.setAccessible(true);
                nextField.set(first, null);
            }
        }
        BrokenLinkCache cache = new BrokenLinkCache(5);
        cache.put("a", 1);
        cache.put("b", 2);
        cache.breakLink();
        NullPointerException npe = assertThrows(NullPointerException.class, cache::values);
        assertNotNull(npe);
        assertFalse(getLock(cache).tryLock(), "Lock must be released after broken link NPE");
    }

    @Test
    void values_putThrowsRuntimeExceptionDuringCollectionBuild_lockReleased() throws Exception {
        // Subclass that replaces the temporary map with one that throws on second put
        class PutFailCache extends LockingLRUCacheStrategy<String, Integer> {

            PutFailCache(int capacity) {
                super(capacity);
            }

            @Override
            public Collection<Integer> values() {
                lock.lock();
                try {
                    Map<String, Integer> map = new LinkedHashMap<String, Integer>() {

                        private boolean first = true;

                        @Override
                        public Integer put(String k, Integer v) {
                            if (!first) {
                                throw new RuntimeException("Map put failure");
                            }
                            first = false;
                            return super.put(k, v);
                        }
                    };
                    for (Node<String, Integer> node = head.next; node != tail; node = node.next) {
                        // throws on second iteration
                        map.put(node.key, node.value);
                    }
                    return map.values();
                } finally {
                    lock.unlock();
                }
            }
        }
        PutFailCache cache = new PutFailCache(5);
        cache.put("x", 1);
        cache.put("y", 2);
        cache.put("z", 3);
        RuntimeException ex = assertThrows(RuntimeException.class, cache::values);
        assertEquals("Map put failure", ex.getMessage());
        assertFalse(getLock(cache).tryLock(), "Lock must be released after map.put exception");
    }
}
