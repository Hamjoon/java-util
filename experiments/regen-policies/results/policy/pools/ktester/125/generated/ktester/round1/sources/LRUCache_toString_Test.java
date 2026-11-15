package com.cedarsoftware.util;

import com.cedarsoftware.util.cache.ThreadedLRUCacheStrategy;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

class LRUCache_toString_Test {

    private LRUCache<Integer, String> cache;

    /**
     * Shut down the shared thread pool used by the threaded strategy (if any).
     */
    @BeforeAll
    static void setupBeforeAll() {
        // The threaded strategy creates a static executor that must be closed after all tests.
        ThreadedLRUCacheStrategy.shutdown();
    }

    /**
     * Create a fresh cache before each test.  The capacity is deliberately larger than the
     * number of entries we will insert so that eviction does not interfere with the
     * {@code toString()} verification.
     */
    @BeforeEach
    void setupBeforeEach() {
        // Use the locking strategy as the default; it has no background threads.
        cache = new LRUCache<>(10, LRUCache.StrategyType.LOCKING);
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear the cache to avoid any cross‑test contamination.
        cache.clear();
    }

    @AfterAll
    static void teardownAfterAll() {
        // Ensure the threaded executor is shut down even if a test switched to that strategy.
        ThreadedLRUCacheStrategy.shutdown();
    }

    static Collection<LRUCache.StrategyType> strategies() {
        return Arrays.asList(LRUCache.StrategyType.LOCKING, LRUCache.StrategyType.THREADED);
    }

    private void initCache(LRUCache.StrategyType type) {
        cache = new LRUCache<>(10, type);
    }

    /**
     * Uses reflection to replace the private {@code strategy} field with the supplied map.
     *
     * @param newStrategy the map that should be used as the underlying strategy
     */
    private void injectStrategy(Map<?, ?> newStrategy) throws Exception {
        Field f = LRUCache.class.getDeclaredField("strategy");
        f.setAccessible(true);
        f.set(cache, newStrategy);
    }

    private static Stream<Map.Entry<String, String>> customMapCases() {
        return Stream.of(new AbstractMap.SimpleEntry<>("{one=1, two=2, three=3}", "{one=1, two=2, three=3}"), new AbstractMap.SimpleEntry<>("{null=foo}", "{null=foo}"), new AbstractMap.SimpleEntry<>("{bar=null}", "{bar=null}"), new AbstractMap.SimpleEntry<>("{null=null}", "{null=null}"));
    }

    @Test
    void toStringEmptyCache() {
        assertTrue(cache.isEmpty(), "Cache should be empty before the test");
        String s = cache.toString();
        // An empty map's toString is "{}"
        assertEquals("{}", s, "Empty cache should render as {}");
    }

    @Test
    void toStringSingleEntry() {
        cache.put(1, "A");
        String s = cache.toString();
        assertTrue(s.contains("1=A"), "String representation must contain the entry 1=A");
        // The representation should start with "{" and end with "}"
        assertTrue(s.startsWith("{") && s.endsWith("}"));
    }

    @Test
    void toStringMultipleEntries() {
        cache.put(1, "A");
        cache.put(2, "B");
        cache.put(3, "C");
        String s = cache.toString();
        assertAll(() -> assertTrue(s.contains("1=A")), () -> assertTrue(s.contains("2=B")), () -> assertTrue(s.contains("3=C")));
    }

    @Test
    void toStringWithNullKeyAndValue() {
        cache.put(null, null);
        String s = cache.toString();
        assertTrue(s.contains("null=null"), "String must contain the null key/value pair");
    }

    @Test
    void toStringWithNullKey() {
        cache.put(null, "value");
        String s = cache.toString();
        assertTrue(s.contains("null=value"));
    }

    @Test
    void toStringWithNullValue() {
        cache.put(42, null);
        String s = cache.toString();
        assertTrue(s.contains("42=null"));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void toStringWorksForAllStrategies(LRUCache.StrategyType type) {
        initCache(type);
        cache.put(10, "X");
        cache.put(20, "Y");
        String s = cache.toString();
        assertAll(() -> assertTrue(s.contains("10=X")), () -> assertTrue(s.contains("20=Y")));
    }

    @Test
    void toStringDelegatesToCustomMapImplementation() throws Exception {
        // A custom map that overrides toString() with a domain‑specific format.
        Map<String, String> customMap = new LinkedHashMap<>() {

            @Override
            public String toString() {
                return "CustomMap[alpha|beta]";
            }
        };
        // content irrelevant – toString() is fixed
        customMap.put("alpha", "beta");
        injectStrategy(customMap);
        String result = cache.toString();
        assertEquals("CustomMap[alpha|beta]", result, "LRUCache should return the custom map's toString() unchanged");
    }

    @Test
    void toStringPropagatesRuntimeExceptionFromStrategy() throws Exception {
        // Mock a map that throws when toString() is called.
        @SuppressWarnings("unchecked")
        Map<Object, Object> explodingMap = mock(Map.class);
        when(explodingMap.toString()).thenThrow(new RuntimeException("boom"));
        injectStrategy(explodingMap);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> cache.toString(), "Exception from underlying map should propagate");
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void toStringReflectsModificationsAfterInsertAndRemove() throws Exception {
        // Use a real LinkedHashMap to keep insertion order.
        Map<String, Integer> underlying = new LinkedHashMap<>();
        injectStrategy(underlying);
        // Insert first entry
        cache.put("first", 10);
        assertEquals("{first=10}", cache.toString());
        // Insert second entry
        cache.put("second", 20);
        assertEquals("{first=10, second=20}", cache.toString());
        // Remove the first entry
        cache.remove("first");
        assertEquals("{second=20}", cache.toString());
    }

    @Test
    void toStringHandlesMapReturningNullFromToString() throws Exception {
        // Mock a map whose toString() returns null.
        @SuppressWarnings("unchecked")
        Map<Object, Object> nullStringMap = mock(Map.class);
        when(nullStringMap.toString()).thenReturn(null);
        injectStrategy(nullStringMap);
        // The call itself returns null.
        String result = cache.toString();
        assertNull(result, "When the underlying map returns null, LRUCache.toString() should return null");
        // Using the returned value should cause NPE – emulate a typical caller.
        assertThrows(NullPointerException.class, () -> result.length(), "Calling a method on the null result should throw NPE");
    }

    @Test
    void toStringThrowsWhenUnderlyingStrategyIsNull() throws Exception {
        // Directly set the private field to null.
        injectStrategy(null);
        assertThrows(NullPointerException.class, () -> cache.toString(), "Calling toString() with a null strategy should throw NPE");
    }

    @Test
    void toStringWithThreadedStrategyRepresentation() throws Exception {
        // Initialise a cache with the threaded strategy.
        initCache(LRUCache.StrategyType.THREADED);
        // Insert a couple of entries – the real ThreadedLRUCacheStrategy implements its own toString().
        cache.put(1, "one");
        cache.put(2, "two");
        // We don't rely on the exact format (it may change), but we assert that the
        // result contains the class name and the two entries.
        String s = cache.toString();
        assertAll(() -> assertTrue(s.contains("ThreadedLRUCacheStrategy"), "Result should contain the strategy class name"), () -> assertTrue(s.contains("1=one")), () -> assertTrue(s.contains("2=two")));
    }

    @Test
    void toStringHandlesSpecialCharactersInKeysAndValues() throws Exception {
        Map<String, String> underlying = new LinkedHashMap<>();
        injectStrategy(underlying);
        underlying.put("key\nline", "val\ttab");
        underlying.put("space key", "space value");
        String s = cache.toString();
        // The default Map.toString() prints the literal characters (including newline/tab).
        assertTrue(s.contains("key\nline=val\ttab"));
        assertTrue(s.contains("space key=space value"));
        // Ensure the overall format is still a map representation.
        assertTrue(s.startsWith("{") && s.endsWith("}"));
    }

    @ParameterizedTest
    @MethodSource("customMapCases")
    void toStringMatchesExactMapString(Map.Entry<String, String> entry) throws Exception {
        // Build a LinkedHashMap that reproduces the expected string exactly.
        Map<String, String> map = new LinkedHashMap<>();
        // Parse the expected string (very simple parsing for test purposes).
        String expected = entry.getKey();
        // Remove braces and split by commas.
        String inner = expected.substring(1, expected.length() - 1).trim();
        if (!inner.isEmpty()) {
            for (String pair : inner.split(",")) {
                String[] kv = pair.trim().split("=", 2);
                String k = "null".equals(kv[0]) ? null : kv[0];
                String v = "null".equals(kv[1]) ? null : kv[1];
                map.put(k, v);
            }
        }
        injectStrategy(map);
        assertEquals(expected, cache.toString(), "LRUCache.toString() must return the exact underlying map representation");
    }
}
