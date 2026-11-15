package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.lang.reflect.Field;
import java.util.concurrent.locks.Lock;

class LockingLRUCacheStrategy_toString_Test {

    /**
     * Instance under test – a small LRU cache that we can manipulate directly.
     */
    private LockingLRUCacheStrategy<String, Integer> cache;

    /**
     * Runs before each test method – creates a fresh cache instance.
     */
    @BeforeEach
    void setupBeforeEach() {
        // Capacity of 3 is enough for the scenarios we test.
        cache = new LockingLRUCacheStrategy<>(3);
    }

    /**
     * Runs after each test method – clears the cache reference.
     */
    @AfterEach
    void teardownAfterEach() {
        cache = null;
    }

    /**
     * Sets a private field on the given target object.
     *
     * @param target the object whose field is to be changed
     * @param fieldName name of the private field
     * @param value new value for the field
     * @throws Exception if reflection fails
     */
    private static void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    /**
     * Retrieves the value of a private field from the given target object.
     *
     * @param target the object whose field is to be read
     * @param fieldName name of the private field
     * @return the field's value
     * @throws Exception if reflection fails
     */
    private static Object getPrivateField(Object target, String fieldName) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        return f.get(target);
    }

    /**
     * Verify that an empty cache renders as an empty map literal.
     */
    @Test
    void toString_EmptyCache_ReturnsEmptyBraces() {
        String result = cache.toString();
        assertEquals("{}", result, "Empty cache should be represented as {}");
    }

    /**
     * Verify that a single entry is rendered correctly.
     */
    @Test
    void toString_SingleEntry_FormatsKeyValuePair() {
        cache.put("key1", 100);
        String result = cache.toString();
        assertEquals("{key1=100}", result, "Single entry should be formatted as {key=value}");
    }

    /**
     * Verify that multiple entries are rendered and contain all expected pairs without a trailing comma.
     */
    @Test
    void toString_MultipleEntries_ContainsAllPairsAndNoTrailingComma() {
        cache.put("first", 1);
        cache.put("second", 2);
        cache.put("third", 3);
        String result = cache.toString();
        // Basic structural checks
        assertTrue(result.startsWith("{") && result.endsWith("}"), "Result should start with '{' and end with '}'");
        // Ensure each expected pair appears exactly once
        assertTrue(result.contains("first=1"), "Result must contain first=1");
        assertTrue(result.contains("second=2"), "Result must contain second=2");
        assertTrue(result.contains("third=3"), "Result must contain third=3");
        // Verify that there is no trailing comma before the closing brace
        assertFalse(result.matches(".*?, \\}$"), "Result must not have a trailing comma before the closing brace");
    }

    /**
     * Verify that null keys and values are handled gracefully by toString.
     */
    @Test
    void toString_NullKeyAndValue_AreRepresentedAsNull() {
        cache.put(null, null);
        cache.put("notNull", null);
        cache.put(null, 99);
        String result = cache.toString();
        assertTrue(result.contains("null=null"), "Result must contain null=null");
        assertTrue(result.contains("notNull=null"), "Result must contain notNull=null");
        assertTrue(result.contains("null=99"), "Result must contain null=99");
    }

    /**
     * Verify that a null key is represented as the string "null".
     */
    @ParameterizedTest(name = "[{index}] key={0}, value={1}")
    @CsvSource({ "null, 10", "null, null", "'notNull', null" })
    void toString_NullKeyOrValue_RepresentedAsNull(String key, Integer value) {
        // The CSV source uses the literal string "null" to indicate a real null.
        String actualKey = "null".equals(key) ? null : key;
        Integer actualValue = "null".equals(String.valueOf(value)) ? null : value;
        cache.put(actualKey, actualValue);
        String result = cache.toString();
        // Build the expected fragment manually to avoid duplication of logic.
        String expectedFragment = (actualKey == null ? "null" : actualKey) + "=" + (actualValue == null ? "null" : actualValue);
        assertTrue(result.contains(expectedFragment), "Result must contain '" + expectedFragment + "'");
    }

    /**
     * Verify that after removing an entry the string reflects only the remaining entry.
     */
    @Test
    void toString_AfterRemoval_ReflectsRemainingEntry() {
        cache.put("key1", 1);
        cache.put("key2", 2);
        // remove first entry
        cache.remove("key1");
        String result = cache.toString();
        assertEquals("{key2=2}", result, "Only the remaining entry should be shown after removal");
    }

    /**
     * Verify that accessing a key moves it to the most‑recent position (head).
     */
    @Test
    void toString_LRUOrderAfterAccess_HeadIsMostRecent() {
        cache.put("key1", 1);
        cache.put("key2", 2);
        // Access key2 – it should become most‑recent.
        cache.get("key2");
        String result = cache.toString();
        assertEquals("{key2=2, key1=1}", result, "After accessing key2 it should appear before key1 in the string representation");
    }

    /**
     * Simulate a corrupted internal state where {@code head} is null.
     */
    @Test
    void toString_NullHead_ThrowsNullPointerException() throws Exception {
        setPrivateField(cache, "head", null);
        assertThrows(NullPointerException.class, cache::toString, "When head is null, toString() should throw NullPointerException");
    }

    /**
     * Simulate a corrupted internal state where {@code tail} is null.
     */
    @Test
    void toString_NullTail_ThrowsNullPointerException() throws Exception {
        setPrivateField(cache, "tail", null);
        assertThrows(NullPointerException.class, cache::toString, "When tail is null, toString() should throw NullPointerException");
    }

    /**
     * Simulate a broken link in the internal list (a node's next is null).
     */
    @Test
    void toString_BrokenLink_ThrowsNullPointerException() throws Exception {
        cache.put("a", 1);
        cache.put("b", 2);
        // Locate the node that follows the head (most‑recent entry)
        Object headNode = getPrivateField(cache, "head");
        Field nextField = headNode.getClass().getDeclaredField("next");
        nextField.setAccessible(true);
        // Break the chain by setting the next reference to null
        nextField.set(headNode, null);
        assertThrows(NullPointerException.class, cache::toString, "A broken next link should cause NullPointerException during iteration");
    }

    /**
     * Verify that a RuntimeException thrown from an entry's toString propagates out of cache.toString().
     */
    @Test
    void toString_ElementToStringException_PropagatesRuntimeException() {
        // Key whose toString throws a RuntimeException
        Object badKey = new Object() {

            @Override
            public String toString() {
                throw new RuntimeException("bad key");
            }
        };
        // unchecked cast – we only need the object in the map
        cache.put((String) badKey, 1);
        RuntimeException ex = assertThrows(RuntimeException.class, cache::toString, "Exception from entry toString() should propagate");
        assertEquals("bad key", ex.getMessage());
    }
}
