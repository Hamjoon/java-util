package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ThreadedLRUCacheStrategy_containsKey_Test {

    private ThreadedLRUCacheStrategy<String, Integer> cacheStrategy;

    /**
     * Simple key class with correct equals / hashCode implementation.
     */
    private static final class CustomKey {

        private final String id;

        CustomKey(String id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof CustomKey))
                return false;
            CustomKey that = (CustomKey) o;
            return id.equals(that.id);
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }
    }

    /**
     * Key whose hashCode() deliberately throws an exception.
     */
    private static final class BadHashKey {

        @Override
        public int hashCode() {
            throw new IllegalStateException("hashCode failure");
        }

        @Override
        public boolean equals(Object o) {
            return false;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // capacity = 5, cleanup delay = 50 ms (arbitrary valid values)
        cacheStrategy = new ThreadedLRUCacheStrategy<>(5, 50);
        // Populate the cache with a few entries for the "containsKey" tests
        cacheStrategy.put("one", 1);
        cacheStrategy.put("two", 2);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure each test starts with a clean cache
        cacheStrategy.clear();
    }

    @AfterAll
    static void teardownAfterAll() {
        // Shut down the shared scheduler used by all ThreadedLRUCacheStrategy instances
        ThreadedLRUCacheStrategy.shutdown();
    }

    private static Stream<Arguments> providePresentKeys() {
        return Stream.of(Arguments.of("alpha", true), // Integer key
        Arguments.of(123, true));
    }

    private static Stream<Arguments> provideAbsentKeys() {
        return Stream.of(Arguments.of("gamma", false), // Long key not present
        Arguments.of(999L, false));
    }

    /**
     * Replaces the private {@code cache} field of {@link ThreadedLRUCacheStrategy}
     * with a supplied mock using reflection.
     */
    private static void setPrivateCacheField(ThreadedLRUCacheStrategy<?, ?> target, ConcurrentMap<?, ?> mockCache) throws Exception {
        Field field = ThreadedLRUCacheStrategy.class.getDeclaredField("cache");
        field.setAccessible(true);
        field.set(target, mockCache);
    }

    @Test
    void containsKey_returnsTrueWhenKeyIsPresent() {
        // given – the key "one" was added in @BeforeEach
        // when
        boolean result = cacheStrategy.containsKey("one");
        // then
        assertTrue(result, "containsKey should return true for a key that exists in the cache");
    }

    @Test
    void containsKey_returnsFalseWhenKeyIsAbsent() {
        // given – a key that was never added
        // when
        boolean result = cacheStrategy.containsKey("missing");
        // then
        assertFalse(result, "containsKey should return false for a key that does not exist in the cache");
    }

    @Test
    void containsKey_handlesNullKey() {
        // given – put a null key into the cache (LRU cache supports null keys)
        cacheStrategy.put(null, 999);
        // when
        boolean containsNull = cacheStrategy.containsKey(null);
        // then
        assertTrue(containsNull, "containsKey should correctly report presence of a null key");
        // also verify that a non‑null key that is not present returns false
        assertFalse(cacheStrategy.containsKey("nonexistent"));
    }

    @ParameterizedTest(name = "{index} => key={0}, expected={1}")
    @MethodSource("providePresentKeys")
    void containsKey_keyPresent(Object key, boolean expected) {
        // arrange – put the key into the cache
        cacheStrategy.put(key, 42);
        // act
        boolean result = cacheStrategy.containsKey(key);
        // assert
        assertEquals(expected, result, () -> "containsKey should return " + expected + " for key: " + key);
    }

    @ParameterizedTest(name = "{index} => key={0}, expected={1}")
    @MethodSource("provideAbsentKeys")
    void containsKey_keyAbsent(Object key, boolean expected) {
        // act – no prior insertion
        boolean result = cacheStrategy.containsKey(key);
        // assert
        assertEquals(expected, result, () -> "containsKey should return " + expected + " for absent key: " + key);
    }

    /* -------------------------------------------------------------
     *  After removal
     * ------------------------------------------------------------- */
    @Test
    void containsKey_afterRemoval_returnsFalse() {
        cacheStrategy.put("delta", 4);
        cacheStrategy.remove("delta");
        assertFalse(cacheStrategy.containsKey("delta"), "containsKey should return false after the key has been removed");
    }

    /* -------------------------------------------------------------
     *  After clear
     * ------------------------------------------------------------- */
    @Test
    void containsKey_afterClear_returnsFalse() {
        cacheStrategy.put("epsilon", 5);
        cacheStrategy.clear();
        assertFalse(cacheStrategy.containsKey("epsilon"), "containsKey must return false after the cache has been cleared");
    }

    /* -------------------------------------------------------------
     *  Null key – expect NullPointerException (underlying map does not allow null)
     * ------------------------------------------------------------- */
    @Test
    void containsKey_nullKey_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> cacheStrategy.containsKey(null), "containsKey should throw NullPointerException when a null key is supplied");
    }

    /* -------------------------------------------------------------
     *  Custom object key with proper equals / hashCode
     * ------------------------------------------------------------- */
    @Test
    void containsKey_customObjectKey_returnsTrue() {
        CustomKey key = new CustomKey("ck1");
        cacheStrategy.put(key, 99);
        assertTrue(cacheStrategy.containsKey(new CustomKey("ck1")), "containsKey should recognise a custom key with proper equals/hashCode");
    }

    /* -------------------------------------------------------------
     *  Key whose hashCode() throws IllegalStateException
     * ------------------------------------------------------------- */
    @Test
    void containsKey_keyHashCodeThrows_throwsIllegalStateException() {
        BadHashKey badKey = new BadHashKey();
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> cacheStrategy.containsKey(badKey), "containsKey should propagate IllegalStateException thrown from key.hashCode()");
        assertEquals("hashCode failure", ex.getMessage());
    }

    /* -------------------------------------------------------------
     *  Key whose equals() throws ClassCastException – simulated via mock
     * ------------------------------------------------------------- */
    @Test
    void containsKey_keyEqualsThrows_throwsClassCastException(@Mock ConcurrentMap<Object, ThreadedLRUCacheStrategy.Node<Object, Integer>> mockCache) throws Exception {
        // Arrange: replace the private 'cache' field with a mock that throws on containsKey
        setPrivateCacheField(cacheStrategy, mockCache);
        when(mockCache.containsKey("trigger")).thenThrow(new ClassCastException("Bad equals"));
        // Act & Assert
        ClassCastException ex = assertThrows(ClassCastException.class, () -> cacheStrategy.containsKey("trigger"), "containsKey should propagate ClassCastException thrown from equals()");
        assertEquals("Bad equals", ex.getMessage());
    }
}
