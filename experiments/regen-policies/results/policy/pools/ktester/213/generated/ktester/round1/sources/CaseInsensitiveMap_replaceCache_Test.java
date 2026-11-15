package com.cedarsoftware.util;

import java.lang.reflect.Field;
// import org.junit.After;
// import org.junit.Before;
// import org.junit.Test;
// import static org.junit.Assert.*;

// @ExtendWith(MockitoExtension.class)
class CaseInsensitiveMap_replaceCache_Test {

    /**
     * The default cache used by {@link CaseInsensitiveMap.CaseInsensitiveString}.
     * Restored after each test to avoid side‑effects.
     */
    private static final LRUCache<String, CaseInsensitiveMap.CaseInsensitiveString> DEFAULT_CACHE = new LRUCache<>(5000, LRUCache.StrategyType.THREADED);

//     @BeforeEach
    void setupBeforeEach() {
        // Ensure each test starts with the original cache and default max length.
        CaseInsensitiveMap.setMaxCacheLengthString(100);
        CaseInsensitiveMap.replaceCache(DEFAULT_CACHE);
    }

//     @AfterEach
    void teardownAfterEach() {
        // Reset to the original state for the next test.
        CaseInsensitiveMap.setMaxCacheLengthString(100);
        CaseInsensitiveMap.replaceCache(DEFAULT_CACHE);
    }

    /**
     * Verifies that {@code replaceCache} swaps the internal {@code COMMON_STRINGS}
     * cache with the supplied {@link LRUCache}.  After the swap, a call to
     * {@link CaseInsensitiveMap.CaseInsensitiveString#of(String)} should store the
     * new key in the supplied cache.
     */
//     @Test
//     void testReplaceCacheSwapsCache() throws Exception {
//         // Arrange – create a fresh cache with a small capacity so we can inspect it.
//         LRUCache<String, CaseInsensitiveMap.CaseInsensitiveString> newCache = new LRUCache<>(10, LRUCache.StrategyType.THREADED);
//         // Act – replace the static cache with our new instance.
//         CaseInsensitiveMap.replaceCache(newCache);
//         // Verify via reflection that the static field now points to the new cache.
//         Field cacheField = CaseInsensitiveMap.CaseInsensitiveString.class.getDeclaredField("COMMON_STRINGS");
//         cacheField.setAccessible(true);
//         Object fieldValue = cacheField.get(null);
//         assertSame("COMMON_STRINGS should reference the cache passed to replaceCache", newCache, fieldValue);
//         // Use a key that is not present in the original pre‑populated cache.
//         String key = "UniqueTestKey123";
//         // The call should trigger computeIfAbsent on the cache we supplied.
//         CaseInsensitiveMap.CaseInsensitiveString result = CaseInsensitiveMap.CaseInsensitiveString.of(key);
//         // Assert – the cache now contains the key.
//         assertNotNull("CaseInsensitiveString.of should never return null for a non‑null input", result);
//         assertTrue("After replaceCache, the new LRUCache must contain the key inserted via of()", newCache.containsKey(key));
//     }

    /**
     * Ensures that after restoring the default cache, the original behaviour is
     * preserved – i.e. a key that is part of the default pre‑populated set is
     * retrieved from the default cache and not from a previously injected cache.
     */
//     @Test
//     void testDefaultCacheRestoredAfterEach() {
//         // The default cache is pre‑populated with the value "true".
//         String commonKey = "true";
//         // Act – obtain the CaseInsensitiveString using the default cache.
//         CaseInsensitiveMap.CaseInsensitiveString cis = CaseInsensitiveMap.CaseInsensitiveString.of(commonKey);
//         // Assert – the returned instance is the same as the one stored in the default cache.
//         // Since we cannot access the private static field directly, we verify that the
//         // instance is cached by calling of() a second time and checking reference equality.
//         CaseInsensitiveMap.CaseInsensitiveString cisSecond = CaseInsensitiveMap.CaseInsensitiveString.of(commonKey);
//         assertSame("With the default cache restored, repeated calls for the same key should return the same instance", cis, cisSecond);
//     }

    /**
     * Passing {@code null} to {@code replaceCache} must result in a
     * {@link NullPointerException}. The method contract explicitly forbids a null cache.
     */
//     @Test
//     void testReplaceCache_nullArgument() {
//         // Act – this should throw NPE.
//         CaseInsensitiveMap.replaceCache(null);
//     }
}