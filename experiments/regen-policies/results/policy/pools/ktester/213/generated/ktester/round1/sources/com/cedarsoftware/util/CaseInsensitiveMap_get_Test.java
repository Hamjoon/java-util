package com.cedarsoftware.util;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// import org.junit.*;
// import static org.junit.Assert.*;
class CaseInsensitiveMap_get_Test {

    /**
     * Map instance used by the majority of tests.
     */
    private CaseInsensitiveMap<String, String> stringMap;

    /**
     * Map instance that mixes String and non‑String keys.
     */
    private CaseInsensitiveMap<Object, String> mixedMap;

    /**
     * Simple custom key with proper equals/hashCode based on a string value.
     */
    private static final class CustomKey {

        private final String value;

        CustomKey(String value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof CustomKey))
                return false;
            CustomKey that = (CustomKey) o;
            return Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }

    /**
     * Key that throws a RuntimeException from {@code hashCode()}.
     */
    private static final class BadHashCodeKey {

        @Override
        public int hashCode() {
            throw new RuntimeException("bad hashCode");
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj;
        }
    }

    /**
     * Key that throws a RuntimeException from {@code equals(Object)}.
     */
    private static final class BadEqualsKey {

        @Override
        public int hashCode() {
            return 1;
        }

        @Override
        public boolean equals(Object obj) {
            throw new RuntimeException("bad equals");
        }
    }

    //     @BeforeAll
    static void setupBeforeAll() {
        // Ensure the global cache is in a known state for all tests.
        CaseInsensitiveMap.setMaxCacheLengthString(100);
        CaseInsensitiveMap.replaceCache(new LRUCache<>(5000, LRUCache.StrategyType.THREADED));
    }

    //     @BeforeEach
    void setupBeforeEach() {
        // Simple map with only String keys – used for the case‑insensitivity tests.
        stringMap = new CaseInsensitiveMap<>();
        stringMap.put("One", "First");
        stringMap.put("Two", "Second");
        stringMap.put("Three", "Third");
        // Map that contains both String and non‑String keys.
        mixedMap = new CaseInsensitiveMap<>();
        mixedMap.put("Alpha", "A");
        mixedMap.put(42, "Answer");
        mixedMap.put(null, "NullValue");
    }

    //     @AfterEach
    void teardownAfterEach() {
        // Reset the cache after each test to avoid cross‑test pollution.
        CaseInsensitiveMap.setMaxCacheLengthString(100);
        CaseInsensitiveMap.replaceCache(new LRUCache<>(5000, LRUCache.StrategyType.THREADED));
    }
}
