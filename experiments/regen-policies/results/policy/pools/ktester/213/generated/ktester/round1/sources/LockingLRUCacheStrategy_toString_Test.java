package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;

class LockingLRUCacheStrategy_toString_Test {

    private static final int CAPACITY = 10;

    private LockingLRUCacheStrategy<String, String> cache;

    private static final int DEFAULT_CAPACITY = 10;

    /* --------------------------------------------------------------
     *  Custom object representation
     * -------------------------------------------------------------- */
    private static final class Person {

        private final String name;

        Person(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "Person[name=" + name + "]";
        }
    }

    /* --------------------------------------------------------------
     *  formatElement exception propagation – key throws
     * -------------------------------------------------------------- */
    private static final class BadToString {

        private final String message;

        BadToString(String message) {
            this.message = message;
        }

        @Override
        public String toString() {
            throw new RuntimeException(message);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh cache for each test so that state does not leak between tests
        cache = new LockingLRUCacheStrategy<>(CAPACITY);
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear the cache – not strictly required because a new instance is created for each test,
        // but it demonstrates good cleanup practice.
        cache.clear();
    }

    @Test
    void toString_EmptyCache_ReturnsEmptyBraces() {
        // given – cache is freshly created and therefore empty
        // when
        String result = cache.toString();
        // then
        assertEquals("{}", result, "toString on an empty cache should produce {}");
    }

    @Test
    void toString_SingleEntry_ReturnsSinglePair() {
        // given
        cache.put("key1", "value1");
        // when
        String result = cache.toString();
        // then
        assertEquals("{key1=value1}", result);
    }

    @Test
    void toString_MultipleEntries_ReturnsAllPairsInInsertionOrder() {
        // given
        cache.put("a", "1");
        cache.put("b", "2");
        cache.put("c", "3");
        // when
        String result = cache.toString();
        // then – insertion order is preserved by the internal linked list
        assertEquals("{a=1, b=2, c=3}", result);
    }

    @Test
    void toString_NullKeyAndValue_AreHandledGracefully() {
        // given
        cache.put(null, null);
        cache.put("x", null);
        cache.put(null, "y");
        // when
        String result = cache.toString();
        // then – the formatElement method (private) renders null as the string "null"
        // The expected order follows the insertion sequence.
        assertEquals("{null=null, x=null, null=y}", result);
    }

    @Test
    void toString_DoesNotMutateCacheState() {
        // given
        cache.put("k1", "v1");
        cache.put("k2", "v2");
        int sizeBefore = cache.size();
        // when
        cache.toString();
        // then – size and content remain unchanged
        assertEquals(sizeBefore, cache.size(), "Cache size should remain unchanged after toString()");
        assertTrue(cache.containsKey("k1"));
        assertTrue(cache.containsKey("k2"));
        assertEquals("v1", cache.get("k1"));
        assertEquals("v2", cache.get("k2"));
    }

    /* --------------------------------------------------------------
     *  LRU ordering after a get()
     * -------------------------------------------------------------- */
    @Test
    void toString_AfterGet_UpdatesLruOrder() {
        // Insert a→1, b→2, c→3
        cache.put("a", "1");
        cache.put("b", "2");
        cache.put("c", "3");
        // Access 'b' to make it most‑recent
        assertEquals("2", cache.get("b"));
        // Expected order: b (most recent), c, a (least recent)
        String result = cache.toString();
        assertEquals("{b=2, c=3, a=1}", result);
    }

    /* --------------------------------------------------------------
     *  Duplicate key update
     * -------------------------------------------------------------- */
    @Test
    void toString_DuplicateKey_UpdatesValueWithoutDuplication() {
        cache.put("a", "1");
        // same key, new value
        cache.put("a", "2");
        String result = cache.toString();
        assertEquals("{a=2}", result);
    }

    /* --------------------------------------------------------------
     *  Capacity eviction (LRU)
     * -------------------------------------------------------------- */
    @Test
    void toString_EvictsLeastRecentlyUsedWhenCapacityExceeded() {
        // Create a small cache (capacity 2)
        LockingLRUCacheStrategy<String, String> smallCache = new LockingLRUCacheStrategy<>(2);
        // becomes MRU
        smallCache.put("a", "1");
        // a becomes LRU
        smallCache.put("b", "2");
        // triggers eviction of 'a'
        smallCache.put("c", "3");
        // Expected order: c (most recent), b (older)
        String result = smallCache.toString();
        assertEquals("{c=3, b=2}", result);
    }

    @Test
    void toString_CustomObjects_UsesTheirToStringRepresentations() {
        LockingLRUCacheStrategy<Person, Person> objCache = new LockingLRUCacheStrategy<>(5);
        Person key = new Person("John");
        Person value = new Person("Doe");
        objCache.put(key, value);
        String result = objCache.toString();
        assertEquals("{Person[name=John]=Person[name=Doe]}", result);
    }

    /* --------------------------------------------------------------
     *  Corrupt internal state – null head
     * -------------------------------------------------------------- */
    @Test
    void toString_NullHead_ThrowsNullPointerException() throws Exception {
        // Use reflection to set the private 'head' field to null.
        Field headField = LockingLRUCacheStrategy.class.getDeclaredField("head");
        headField.setAccessible(true);
        headField.set(cache, null);
        assertThrows(NullPointerException.class, cache::toString, "When 'head' is null, toString() should throw NPE");
    }

    /* --------------------------------------------------------------
     *  Corrupt internal state – null tail
     * -------------------------------------------------------------- */
    @Test
    void toString_NullTail_ThrowsNullPointerException() throws Exception {
        // Use reflection to set the private 'tail' field to null.
        Field tailField = LockingLRUCacheStrategy.class.getDeclaredField("tail");
        tailField.setAccessible(true);
        tailField.set(cache, null);
        assertThrows(NullPointerException.class, cache::toString, "When 'tail' is null, toString() should throw NPE");
    }

    @Test
    void toString_KeyWithBadToString_PropagatesRuntimeException() {
        BadToString badKey = new BadToString("bad key");
        cache.put("good", "value");
        // Directly insert the bad key via the public API – it will be stored.
        // cast to bypass generic compile‑time check
        cache.put((String) (Object) badKey, "v");
        RuntimeException ex = assertThrows(RuntimeException.class, cache::toString, "Exception from key.toString() should propagate");
        assertEquals("bad key", ex.getMessage());
    }

    @Test
    void toString_ValueWithBadToString_PropagatesRuntimeException() {
        BadToString badValue = new BadToString("bad value");
        cache.put("good", "value");
        // Insert entry where the value's toString() throws.
        cache.put("k", (String) (Object) badValue);
        RuntimeException ex = assertThrows(RuntimeException.class, cache::toString, "Exception from value.toString() should propagate");
        assertEquals("bad value", ex.getMessage());
    }
}
