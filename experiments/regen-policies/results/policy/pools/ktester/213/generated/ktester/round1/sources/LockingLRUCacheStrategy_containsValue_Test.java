package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

class LockingLRUCacheStrategy_containsValue_Test {

    private LockingLRUCacheStrategy<Integer, String> cache;

    class BadEquals {

        @Override
        public boolean equals(Object o) {
            throw new IllegalArgumentException("Bad equals");
        }
    }

    private static class ArgumentsHolder {

        final Object value;

        final boolean expected;

        final String description;

        ArgumentsHolder(Object value, boolean expected, String description) {
            this.value = value;
            this.expected = expected;
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    class BadEquals {

        @Override
        public boolean equals(Object o) {
            throw new IllegalArgumentException("Bad equals");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Default cache used by most tests – capacity 5 and a few entries.
        cache = new LockingLRUCacheStrategy<>(5);
        cache.put(1, "firstValue");
        cache.put(2, "middleValue");
        cache.put(3, "lastValue");
        // explicit null value
        cache.put(4, null);
    }

    @AfterEach
    void teardownAfterEach() {
        // Cleanup the cache reference
        cache = null;
    }

    /**
     * Retrieves a private field and makes it accessible.
     */
    private static Field getField(Class<?> clazz, String name) throws NoSuchFieldException {
        Field f = clazz.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }

    /**
     * Sets the value of a private field on the given target object.
     */
    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field f = getField(target.getClass(), fieldName);
        f.set(target, value);
    }

    /**
     * Returns the inner Node class of {@link LockingLRUCacheStrategy}.
     */
    private static Class<?> nodeClass() throws ClassNotFoundException {
        return Class.forName("com.cedarsoftware.util.cache.LockingLRUCacheStrategy$Node");
    }

    /**
     * Sets the 'next' reference of a Node instance.
     */
    private static void setNodeNext(Object node, Object next) throws Exception {
        Field f = getField(nodeClass(), "next");
        f.set(node, next);
    }

    /**
     * Sets the 'value' field of a Node instance.
     */
    private static void setNodeValue(Object node, Object value) throws Exception {
        Field f = getField(nodeClass(), "value");
        f.set(node, value);
    }

    /**
     * Retrieves the private 'head' field from the cache instance.
     */
    private Object getHead() throws Exception {
        return getField(LockingLRUCacheStrategy.class, "head").get(cache);
    }

    /**
     * Retrieves the private 'tail' field from the cache instance.
     */
    private Object getTail() throws Exception {
        return getField(LockingLRUCacheStrategy.class, "tail").get(cache);
    }

    static Stream<ArgumentsHolder> matchAtFirstNode() {
        return Stream.of(new ArgumentsHolder("firstValue", true, "Cache is populated with entries ['firstValue', 'middleValue', 'lastValue'] in that order. " + "The searched value is the first entry's value, so the method should return true after checking the first node."));
    }

    static Stream<ArgumentsHolder> matchAtLastNode() {
        return Stream.of(new ArgumentsHolder("lastValue", true, "Cache contains the same three entries as above. The searched value is the value of the node just before the tail sentinel."));
    }

    static Stream<ArgumentsHolder> valueNotPresent() {
        return Stream.of(new ArgumentsHolder("absentValue", false, "Cache holds ['firstValue', 'middleValue', 'lastValue']; the searched value does not exist."));
    }

    static Stream<ArgumentsHolder> nullValuePresent() {
        return Stream.of(new ArgumentsHolder(null, true, "Cache is populated with a null value entry (key 4 -> null)."));
    }

    static Stream<ArgumentsHolder> nullValueAbsent() {
        return Stream.of(new ArgumentsHolder(null, false, "Cache contains only non‑null values (null entry removed in setup)."));
    }

    static Stream<ArgumentsHolder> emptyCache() {
        return Stream.of(new ArgumentsHolder("anyValue", false, "Cache has been cleared (size 0)."));
    }

    static Stream<ArgumentsHolder> valuePresentMultipleTimes() {
        return Stream.of(new ArgumentsHolder("dupValue", true, "Cache contains several entries with the same value 'dupValue'."));
    }

    static Stream<ArgumentsHolder> tailSentinelNotExamined() {
        return Stream.of(new ArgumentsHolder("sentinelValue", false, "Even if tail sentinel's internal value is set, the loop never checks it."));
    }

    static Stream<ArgumentsHolder> singleElementCache() {
        return Stream.of(new ArgumentsHolder("onlyValue", true, "Cache has exactly one user entry; containsValue should find it."), new ArgumentsHolder("missing", false, "Same single‑element cache, but searched value is not present."));
    }

    static Stream<ArgumentsHolder> valueDifferentType() {
        return Stream.of(new ArgumentsHolder(12345, false, "Cache stores String values only. Searching with an Integer should not match."));
    }

    @Test
    void containsValue_existingValue_returnsTrue() {
        // WHEN: checking for a value that is present
        boolean result = cache.containsValue("two");
        // THEN: the method should return true
        assertTrue(result, "Cache should contain the value \"two\"");
    }

    @Test
    void containsValue_nonExistingValue_returnsFalse() {
        // WHEN: checking for a value that is not present
        boolean result = cache.containsValue("four");
        // THEN: the method should return false
        assertFalse(result, "Cache should not contain the value \"four\"");
    }

    @Test
    void containsValue_nullValueWhenStored_returnsTrue() {
        // WHEN: checking for a null value that has been stored
        boolean result = cache.containsValue(null);
        // THEN: the method should return true
        assertTrue(result, "Cache should contain a stored null value");
    }

    @Test
    void containsValue_nullValueWhenNotStored_returnsFalse() {
        // GIVEN: a fresh cache without a null value
        cache = new LockingLRUCacheStrategy<>(3);
        cache.put(10, "ten");
        cache.put(20, "twenty");
        // WHEN: checking for null
        boolean result = cache.containsValue(null);
        // THEN: the method should return false
        assertFalse(result, "Cache should not contain null when no null value was stored");
    }

    @Test
    void containsValue_afterRemoval_returnsFalse() {
        // GIVEN: a value that will be removed
        // removes the entry with value "two"
        cache.remove(2);
        // WHEN: checking for the removed value
        boolean result = cache.containsValue("two");
        // THEN: the method should return false
        assertFalse(result, "Cache should not contain a value after its entry has been removed");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("matchAtFirstNode")
    void containsValue_matchAtFirstNode(ArgumentsHolder args) {
        boolean result = cache.containsValue(args.value);
        assertEquals(args.expected, result);
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("matchAtLastNode")
    void containsValue_matchAtLastNode(ArgumentsHolder args) {
        boolean result = cache.containsValue(args.value);
        assertEquals(args.expected, result);
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("valueNotPresent")
    void containsValue_valueNotPresent(ArgumentsHolder args) {
        boolean result = cache.containsValue(args.value);
        assertEquals(args.expected, result);
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("nullValuePresent")
    void containsValue_nullValuePresent(ArgumentsHolder args) {
        boolean result = cache.containsValue(args.value);
        assertEquals(args.expected, result);
    }

    @Test
    void containsValue_nullValueAbsent() {
        // Remove the null entry added in @BeforeEach
        cache.remove(4);
        boolean result = cache.containsValue(null);
        assertFalse(result, "Cache should not contain null when no null value was stored");
    }

    @Test
    void containsValue_emptyCache() {
        cache.clear();
        boolean result = cache.containsValue("anyValue");
        assertFalse(result, "Empty cache must return false for any value");
    }

    @Test
    void containsValue_concurrentModificationSafety() throws Exception {
        // Prepare a latch so the background thread holds the lock while we call containsValue.
        CountDownLatch lockAcquired = new CountDownLatch(1);
        CountDownLatch releaseLock = new CountDownLatch(1);
        Thread writer = new Thread(() -> {
            // Acquire the same lock used inside containsValue.
            try {
                Field lockField = getField(LockingLRUCacheStrategy.class, "lock");
                java.util.concurrent.locks.Lock lock = (java.util.concurrent.locks.Lock) lockField.get(cache);
                lock.lock();
                // signal that lock is held
                lockAcquired.countDown();
                // wait until main thread finishes containsValue
                releaseLock.await();
                // Add a new entry while holding the lock.
                cache.put(99, "midValue");
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                // Ensure lock is released.
                try {
                    Field lockField = getField(LockingLRUCacheStrategy.class, "lock");
                    java.util.concurrent.locks.Lock lock = (java.util.concurrent.locks.Lock) lockField.get(cache);
                    lock.unlock();
                } catch (Exception ignored) {
                }
            }
        });
        writer.start();
        // Wait until writer thread has the lock.
        lockAcquired.await();
        // Now call containsValue – it will block until the writer releases the lock.
        boolean result = cache.containsValue("midValue");
        assertTrue(result, "containsValue should see the newly added entry after lock release");
        // Allow writer to finish.
        releaseLock.countDown();
        writer.join();
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("valuePresentMultipleTimes")
    void containsValue_valuePresentMultipleTimes(ArgumentsHolder args) {
        // Insert duplicate values.
        cache.put(5, "dupValue");
        cache.put(6, "dupValue");
        boolean result = cache.containsValue(args.value);
        assertEquals(args.expected, result);
    }

    @Test
    void containsValue_tailSentinelNotExamined() throws Exception {
        // Force the tail sentinel's value to a specific string.
        Object tail = getTail();
        setNodeValue(tail, "sentinelValue");
        boolean result = cache.containsValue("sentinelValue");
        assertFalse(result, "Tail sentinel should not be examined, method must return false");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("singleElementCache")
    void containsValue_singleElementCache(ArgumentsHolder args) throws Exception {
        // Build a cache that has exactly one user node.
        cache.clear();
        // now size 1
        cache.put(42, "onlyValue");
        boolean result = cache.containsValue(args.value);
        assertEquals(args.expected, result);
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("valueDifferentType")
    void containsValue_valueOfDifferentType(ArgumentsHolder args) {
        boolean result = cache.containsValue(args.value);
        assertEquals(args.expected, result);
    }

    @Test
    void containsValue_headSentinelNull_expectNPE() throws Exception {
        // Set the internal head reference to null via reflection.
        setField(cache, "head", null);
        assertThrows(NullPointerException.class, () -> cache.containsValue("anyValue"), "When head is null, containsValue should throw NullPointerException");
    }

    @Test
    void containsValue_tailSentinelNull_expectNPE() throws Exception {
        // Set the internal tail reference to null via reflection.
        setField(cache, "tail", null);
        assertThrows(NullPointerException.class, () -> cache.containsValue("anyValue"), "When tail is null, containsValue should throw NullPointerException");
    }

    @Test
    void containsValue_brokenLinkBeforeTail_expectNPE() throws Exception {
        // Break the link: set the first user node's next to null.
        Object head = getHead();
        Field nextField = getField(nodeClass(), "next");
        // head.next
        Object firstUser = nextField.get(head);
        // Break the chain.
        setNodeNext(firstUser, null);
        assertThrows(NullPointerException.class, () -> cache.containsValue("anyValue"), "A broken link before tail should cause NullPointerException");
    }

    @Test
    void containsValue_valueEqualsThrowsException_expectRuntimeException() {
        // Custom object whose equals throws.
        class BadEquals {

            @Override
            public boolean equals(Object o) {
                throw new IllegalArgumentException("Bad equals");
            }
        }
        BadEquals bad = new BadEquals();
        cache.put(7, bad);
        // Searching for any value will trigger Objects.equals(bad, value) which calls bad.equals(...)
        assertThrows(RuntimeException.class, () -> cache.containsValue("trigger"), "Exception from equals should propagate as RuntimeException");
    }
}
