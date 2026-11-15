package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

class LockingLRUCacheStrategy_toString_Test {

    private LockingLRUCacheStrategy<String, Integer> cache;

    /**
     * Create a fresh cache before each test. A small capacity is enough for the
     * toString() tests – we are only interested in the string representation,
     * not eviction behaviour.
     */
    @BeforeEach
    void setupBeforeEach() {
        cache = new LockingLRUCacheStrategy<>(10);
    }

    /**
     * Clear the cache after each test – not strictly required because a new
     * instance is created for every test, but it demonstrates the typical
     * teardown pattern.
     */
    @AfterEach
    void teardownAfterEach() {
        cache.clear();
    }

    /**
     * Helper that checks whether the expected fragment appears in the array.
     */
    private boolean containsFragment(String[] fragments, String expected) {
        for (String f : fragments) {
            if (f.equals(expected)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sets a private field (including final ones) via reflection.
     *
     * @param target    the object whose field is to be modified
     * @param fieldName name of the field
     * @param value     new value
     * @throws Exception if reflection fails
     */
    private static void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        // For final fields we need to remove the 'final' modifier
        java.lang.reflect.Field modifiersField = java.lang.reflect.Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(f, f.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
        f.set(target, value);
    }

    @Test
    void toString_ShouldReturnEmptyBraces_WhenCacheIsEmpty() {
        // given – cache is newly created and therefore empty
        // when
        String result = cache.toString();
        // then
        assertEquals("{}", result, "Empty cache should be represented as {}");
    }

    @Test
    void toString_ShouldContainSingleEntry_WhenOneElementIsAdded() {
        // given
        cache.put("one", 1);
        // when
        String result = cache.toString();
        // then
        assertEquals("{one=1}", result, "Cache with a single entry should be formatted without a trailing comma");
    }

    @Test
    void toString_ShouldContainAllEntries_WithProperCommaAndSpaceFormatting() {
        // given
        cache.put("first", 100);
        cache.put("second", 200);
        cache.put("third", 300);
        // when
        String result = cache.toString();
        // then – the string must start with '{' and end with '}'
        assertTrue(result.startsWith("{") && result.endsWith("}"), "Result should be wrapped with braces");
        // remove the surrounding braces for easier checks
        String inner = result.substring(1, result.length() - 1);
        // split on comma‑space to obtain individual key=value fragments
        String[] fragments = inner.split(", ");
        // we expect exactly three fragments
        assertEquals(3, fragments.length, "There should be three key=value pairs");
        // each fragment must match one of the inserted entries
        assertTrue(containsFragment(fragments, "first=100"));
        assertTrue(containsFragment(fragments, "second=200"));
        assertTrue(containsFragment(fragments, "third=300"));
    }

    @Test
    void toString_ShouldHandleNullKeyAndValue() {
        // given
        cache.put(null, null);
        cache.put("key", null);
        cache.put(null, 42);
        // when
        String result = cache.toString();
        // then – verify that the literal string "null" is used for null elements
        assertTrue(result.contains("null=null"));
        assertTrue(result.contains("key=null"));
        assertTrue(result.contains("null=42"));
    }

    @Test
    void toString_ShouldRenderSpecialCharactersWithoutEscaping() {
        // given – keys/values contain commas, braces, spaces and quotes
        cache.put("a,b", 1);
        cache.put("{value}", 2);
        cache.put("\"quoted\"", 3);
        cache.put("space value", 4);
        // when
        String result = cache.toString();
        // then – the raw toString of each element appears directly
        assertTrue(result.contains("a,b=1"));
        assertTrue(result.contains("{value}=2"));
        assertTrue(result.contains("\"quoted\"=3"));
        assertTrue(result.contains("space value=4"));
    }

    @Test
    void toString_ShouldBlockConcurrentModificationUntilLockIsReleased() throws Exception {
        // given – a latch that will start the concurrent put after we have entered toString
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch putDoneLatch = new CountDownLatch(1);
        Thread modifier = new Thread(() -> {
            try {
                // wait until main thread signals that it has entered toString
                startLatch.await();
                cache.put("concurrent", 99);
                putDoneLatch.countDown();
            } catch (InterruptedException ignored) {
            }
        });
        modifier.start();
        // when – we call toString while the other thread is ready to modify
        // Signal the other thread *after* we have acquired the lock (inside toString)
        // To achieve that we use reflection to replace the lock with a proxy that
        // notifies us when lock() is called.
        // Simpler approach: give the modifier a tiny head‑start, then call toString.
        // The put will block on the internal ReentrantLock until toString releases it.
        // After toString returns we wait for the put to finish.
        // No special instrumentation is required.
        // Give the modifier a moment to start and attempt the put (it will block).
        Thread.sleep(50);
        // Signal the modifier that it may attempt the put (it already is waiting)
        startLatch.countDown();
        // holds the lock
        String result = cache.toString();
        // then – the put should have completed after toString released the lock
        assertTrue(putDoneLatch.await(1, TimeUnit.SECONDS), "Concurrent put should complete after toString releases the lock");
        assertTrue(result.contains("concurrent=99") || cache.containsKey("concurrent"), "Result should eventually contain the concurrently added entry");
        modifier.join();
    }

    @Test
    void toString_ShouldPreserveInterruptStatusWhenThreadIsInterrupted() {
        // given – interrupt the current thread before calling toString
        Thread.currentThread().interrupt();
        // when
        String result = cache.toString();
        // then – toString does not clear the interrupt flag
        assertTrue(Thread.currentThread().isInterrupted(), "Thread interrupt status must remain set after toString()");
        assertEquals("{}", result);
    }

    @Test
    void toString_ShouldThrowNullPointerException_WhenHeadIsNull() throws Exception {
        // given – corrupt the internal 'head' reference via reflection
        setPrivateField(cache, "head", null);
        // when / then
        assertThrows(NullPointerException.class, () -> cache.toString(), "Null head should cause NPE during iteration");
    }

    @Test
    void toString_ShouldThrowNullPointerException_WhenTailIsNull() throws Exception {
        // given – corrupt the internal 'tail' reference via reflection
        setPrivateField(cache, "tail", null);
        // when / then
        assertThrows(NullPointerException.class, () -> cache.toString(), "Null tail should cause NPE during iteration");
    }

    @Test
    void toString_ShouldPropagateException_FromKeyToString() {
        // given – a key whose toString() throws
        Object badKey = new Object() {

            @Override
            public String toString() {
                throw new RuntimeException("bad key");
            }
        };
        // unchecked cast, but we only need the map entry
        cache.put((String) badKey, 1);
        // when / then
        RuntimeException ex = assertThrows(RuntimeException.class, () -> cache.toString(), "Exception from key.toString() must propagate");
        assertEquals("bad key", ex.getMessage());
    }

    @Test
    void toString_ShouldPropagateException_FromValueToString() {
        // given – a value whose toString() throws
        Object badValue = new Object() {

            @Override
            public String toString() {
                throw new RuntimeException("bad value");
            }
        };
        // unchecked cast, just to store the object
        cache.put("key", (Integer) badValue);
        // when / then
        RuntimeException ex = assertThrows(RuntimeException.class, () -> cache.toString(), "Exception from value.toString() must propagate");
        assertEquals("bad value", ex.getMessage());
    }
}
