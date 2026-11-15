package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveMap_replaceCache_Test {

    /**
     * The mock cache that will replace the internal static cache.
     */
    @Mock
    private LRUCache<String, Object> mockCache;

    /**
     * Holds the original cache so we can restore it after each test.
     */
    private static LRUCache<String, CaseInsensitiveMap.CaseInsensitiveString> originalCache;

    /**
     * Reflection handle for the private static field.
     */
    private static Field cacheField;

    @BeforeAll
    static void captureOriginalCache() throws Exception {
        cacheField = CaseInsensitiveMap.class.getDeclaredField("ciStringCache");
        cacheField.setAccessible(true);
        // store the original cache to restore later
        @SuppressWarnings("unchecked")
        LRUCache<String, CaseInsensitiveMap.CaseInsensitiveString> current = (LRUCache<String, CaseInsensitiveMap.CaseInsensitiveString>) cacheField.get(null);
        originalCache = current;
    }

    /**
     * Replace the internal {@code ciStringCache} with the mock before each test.
     * This allows us to verify that the map delegates to the supplied cache.
     */
    @BeforeEach
    void setupBeforeEach() {
        // The method under test – replace the static cache with our mock.
        CaseInsensitiveMap.replaceCache(mockCache);
    }

    /**
     * Restore the original cache after each test so that other tests are not affected.
     */
    @AfterEach
    void teardownAfterEach() {
        // The library’s default cache size is 1000 entries.
        CaseInsensitiveMap.replaceCache(new LRUCache<>(1000));
        // ensure other test classes are not polluted
        CaseInsensitiveMap.replaceCache(originalCache);
    }

    /**
     * Supplies test data for the “basic functionality” group.
     */
    private static Stream<BasicCase> basicCases() {
        return Stream.of(new BasicCase(new LRUCache<>(5), "new LRUCache<>(5)"), new BasicCase(new LRUCache<>(10), "existingCache (different instance)"), new BasicCase(null, "null cache"));
    }

    /**
     * Supplies test data for the “edge capacity handling” group.
     */
    private static Stream<EdgeCase> edgeCases() {
        return Stream.of(new EdgeCase(new LRUCache<>(0), "new LRUCache<>(0) – zero capacity"), new EdgeCase(new LRUCache<>(-1), "new LRUCache<>(-1) – negative capacity"));
    }

    @Test
    void replaceCacheShouldDelegateCacheOperationsToProvidedCache() {
        // GIVEN – a fresh case‑insensitive map
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        // WHEN – we put a new entry (this should cause the map to put a
        //        CaseInsensitiveString into the cache)
        map.put("KeyOne", "ValueOne");
        // THEN – the mock cache must have received a put call for the key.
        verify(mockCache, atLeastOnce()).put(eq("KeyOne"), any());
        // WHEN – we retrieve the value using a different case
        String retrieved = map.get("keyone");
        // THEN – the mock cache must have been queried.
        verify(mockCache, atLeastOnce()).get(eq("keyone"));
        assertEquals("ValueOne", retrieved);
    }

    @Test
    void replaceCacheShouldAllowSubsequentOperationsToContinueWorking() {
        // GIVEN – a map after the cache has been replaced
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put("Alpha", "A");
        map.put("Beta", "B");
        // WHEN – we read values using various casings
        String a = map.get("ALPHA");
        String b = map.get("beta");
        // THEN – the map still returns the correct values even though the
        //        underlying cache is a mock (the mock does not throw).
        assertEquals("A", a);
        assertEquals("B", b);
        // Verify that the mock cache was interacted with for each get.
        verify(mockCache, atLeastOnce()).get(eq("ALPHA"));
        verify(mockCache, atLeastOnce()).get(eq("beta"));
    }

    /**
     * Verifies that {@code replaceCache} stores the exact instance supplied,
     * even when the argument is {@code null}.
     */
    @ParameterizedTest(name = "[basic] {index}: {0.description}")
    @MethodSource("basicCases")
    void replaceCache_assignsExactInstance(BasicCase tc) throws Exception {
        // Act – replace the static cache with the test case value
        CaseInsensitiveMap.replaceCache(tc.cache);
        // Assert – the internal field must reference the same object (or null)
        Object actual = cacheField.get(null);
        assertSame(tc.cache, actual, () -> "ciStringCache should be the same reference as the supplied cache");
    }

    /**
     * Verifies that {@code replaceCache} accepts caches with unusual capacities
     * (zero or negative) and stores them unchanged.
     */
    @ParameterizedTest(name = "[edge] {index}: {0.description}")
    @MethodSource("edgeCases")
    void replaceCache_acceptsEdgeCapacities(EdgeCase tc) throws Exception {
        // Act
        CaseInsensitiveMap.replaceCache(tc.cache);
        // Assert – the field must hold the exact instance we passed
        Object actual = cacheField.get(null);
        assertSame(tc.cache, actual, () -> "ciStringCache should store the cache even if its capacity is unusual");
    }

    /**
     * Additional sanity test that after replacing the cache with a functional
     * instance, normal map operations still work (delegating to the new cache).
     */
    @Test
    void mapOperationsWorkAfterCacheReplacement() {
        // Arrange – a fresh cache that we can inspect
        LRUCache<String, CaseInsensitiveMap.CaseInsensitiveString> newCache = new LRUCache<>(2);
        CaseInsensitiveMap.replaceCache(newCache);
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        // Act – put and get using different casings
        map.put("Hello", "World");
        String v1 = map.get("hello");
        String v2 = map.get("HELLO");
        // Assert – values are correct
        assertEquals("World", v1);
        assertEquals("World", v2);
        // The cache should now contain the case‑insensitive wrapper for "Hello"
        assertNotNull(newCache.get("Hello"), "The new cache should have cached the CaseInsensitiveString for the key");
    }
}
