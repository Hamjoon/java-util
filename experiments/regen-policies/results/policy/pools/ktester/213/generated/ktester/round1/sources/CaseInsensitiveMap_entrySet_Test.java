package com.cedarsoftware.util;

import java.util.*;
import java.lang.reflect.Array;
import java.util.stream.Stream;

// import org.junit.*;
// import org.junit.runner.RunWith;
// import org.junit.runners.Parameterized;
class CaseInsensitiveMap_entrySet_Test {

    private CaseInsensitiveMap<String, String> map;

    //     @BeforeAll
    static void setupBeforeAll() {
        // Ensure the global cache is in a known state for the whole suite.
        CaseInsensitiveMap.setMaxCacheLengthString(100);
        CaseInsensitiveMap.replaceCache(new LRUCache<>(5000, LRUCache.StrategyType.THREADED));
        // Ensure a deterministic cache state for the whole suite.
        CaseInsensitiveMap.setMaxCacheLengthString(100);
        CaseInsensitiveMap.replaceCache(new LRUCache<>(5000, LRUCache.StrategyType.THREADED));
    }

    //     @BeforeEach
    void setupBeforeEach() {
        map = new CaseInsensitiveMap<>();
        map.put("One", "Alpha");
        map.put("Two", "Beta");
        map.put("Three", "Gamma");
    }

    //     @AfterEach
    void teardownAfterEach() {
        // Reset the cache after each test to avoid cross‑test interference.
        CaseInsensitiveMap.setMaxCacheLengthString(100);
        CaseInsensitiveMap.replaceCache(new LRUCache<>(5000, LRUCache.StrategyType.THREADED));
        // Reset global cache after each test.
        CaseInsensitiveMap.setMaxCacheLengthString(100);
        CaseInsensitiveMap.replaceCache(new LRUCache<>(5000, LRUCache.StrategyType.THREADED));
    }
}
