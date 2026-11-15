package com.cedarsoftware.util;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// @ExtendWith(MockitoExtension.class)
class CaseInsensitiveMap_getWrappedMap_Test {

    /**
     * No global state is required for these tests, but we keep the lifecycle methods
     *  for consistency with the project’s test conventions.
     */
//     @BeforeAll
    static void setupBeforeAll() {
        // Ensure the default cache length is restored in case other tests changed it.
        CaseInsensitiveMap.setMaxCacheLengthString(100);
    }

//     @AfterEach
    void teardownAfterEach() {
        // Reset the cache after each test to avoid cross‑test interference.
        CaseInsensitiveMap.replaceCache(new LRUCache<>(5000, LRUCache.StrategyType.THREADED));
    }

//     @AfterAll
    static void teardownAfterAll() {
        // Clean‑up any static modifications that could affect other test suites.
        CaseInsensitiveMap.setMaxCacheLengthString(100);
        CaseInsensitiveMap.replaceCache(new LRUCache<>(5000, LRUCache.StrategyType.THREADED));
    }

    /**
     * Supplying {@code null} as the source map must throw NPE.
     */
    private static void testConstructorWithNullMapThrowsException() {
        try {
            new CaseInsensitiveMap<>(null);
            throw new AssertionError("Expected NullPointerException");
        } catch (NullPointerException ignored) {
            // expected
        }
    }

    /**
     * After {@code put}, the wrapped map must contain the entry.
     */
    private static void testWrappedMapReflectsPut() {
        CaseInsensitiveMap<String, String> ciMap = new CaseInsensitiveMap<>();
        ciMap.put("a", "1");
        Map<String, String> wrapped = ciMap.getWrappedMap();
        assert "1".equals(wrapped.get("a"));
        assert "1".equals(ciMap.get("A"));
    }

    /**
     * After {@code remove}, the wrapped map must no longer contain the key.
     */
    private static void testWrappedMapReflectsRemove() {
        CaseInsensitiveMap<String, String> ciMap = new CaseInsensitiveMap<>();
        ciMap.put("a", "1");
        assert ciMap.getWrappedMap().containsKey("a");
        ciMap.remove("a");
        assert !ciMap.getWrappedMap().containsKey("a");
    }

    /**
     * Direct modifications of the wrapped map must be visible through the CI map.
     */
    private static void testDirectModificationThroughWrappedMap() {
        CaseInsensitiveMap<String, String> ciMap = new CaseInsensitiveMap<>();
        Map<String, String> wrapped = ciMap.getWrappedMap();
        wrapped.put("b", "2");
        assert "2".equals(ciMap.get("b"));
        assert "2".equals(ciMap.get("B"));
    }

    /* --------------------------------------------------------------------- */
//     public static void main(String[] args) {
//         // Enable assertions if the JVM was started without the -ea flag.
//         // This is a safety net – the demo works even if assertions are disabled.
//         boolean assertionsEnabled = false;
//         // Intentional side‑effect
//         assert assertionsEnabled = true;
//         if (!assertionsEnabled) {
//             System.out.println("WARNING: Assertions are disabled. " + "Run the program with the -ea JVM option for full verification.");
//         }
//         try {
//             // Reset any static state that might have been altered by previous runs.
//             CaseInsensitiveMap.setMaxCacheLengthString(100);
//             CaseInsensitiveMap.replaceCache(new LRUCache<>(5000, LRUCache.StrategyType.THREADED));
//             // Execute the test‑like methods.
//             testWrappedMapModificationReflectsInCaseInsensitiveMap();
//             testConstructorWithNullMapThrowsException();
//             testWrappedMapReflectsPut();
//             testWrappedMapReflectsRemove();
//             testDirectModificationThroughWrappedMap();
//             System.out.println("All demo checks passed.");
//         } catch (AssertionError e) {
//             System.err.println("Demo check failed: " + e.getMessage());
//             e.printStackTrace();
//             System.exit(1);
//         } catch (Throwable t) {
//             System.err.println("Unexpected error during demo execution:");
//             t.printStackTrace();
//             System.exit(1);
//         } finally {
//             // Clean‑up static modifications so they do not affect other code.
//             CaseInsensitiveMap.setMaxCacheLengthString(100);
//             CaseInsensitiveMap.replaceCache(new LRUCache<>(5000, LRUCache.StrategyType.THREADED));
//         }
//     }

    /**
     * Modifying the map returned by {@code getWrappedMap()} must be reflected in the
     * {@code CaseInsensitiveMap} view and vice‑versa. This test adds an entry directly
     * to the wrapped map and then retrieves it through the case‑insensitive API,
     * confirming that the two maps share the same underlying data structure.
     */
//     @Test
//     void testWrappedMapModificationReflectsInCaseInsensitiveMap() {
//         CaseInsensitiveMap<String, String> ciMap = new CaseInsensitiveMap<>();
//         ciMap.put("Alpha", "A");
//         ciMap.put("Beta", "B");
//         Map<String, String> wrapped = ciMap.getWrappedMap();
//         wrapped.put("Gamma", "C");
//         assert "C".equals(ciMap.get("gamma"));
//         assert "C".equals(ciMap.get("GAMMA"));
//         assert ciMap.size() == 3;
//         ciMap.remove("beta");
//         assert !wrapped.containsKey("Beta");
//         assert wrapped.size() == 2;
//     }

    /**
     * Supplying {@code null} as the source map must result in a {@link NullPointerException}.
     */
//     @Test
//     @DisplayName("ConstructorWithNullMap – throws NPE")
//     void constructorWithNullMapThrowsException() {
//         assertThrows(NullPointerException.class, () -> new CaseInsensitiveMap<>(null));
//     }

    /**
     * After calling {@code put} on the {@code CaseInsensitiveMap}, the underlying
     * wrapped map must contain the entry.
     */
//     @Test
//     @DisplayName("WrappedMapReflectsPut – entry appears in wrapped map")
//     void wrappedMapReflectsPut() {
//         CaseInsensitiveMap<String, String> ciMap = new CaseInsensitiveMap<>();
//         ciMap.put("a", "1");
//         Map<String, String> wrapped = ciMap.getWrappedMap();
//         assertEquals("1", wrapped.get("a"));
//         assertEquals("1", ciMap.get("A"));
//     }

    /**
     * After removing a key via the {@code CaseInsensitiveMap}, the wrapped map
     * must no longer contain that key.
     */
//     @Test
//     @DisplayName("WrappedMapReflectsRemove – entry disappears from wrapped map")
//     void wrappedMapReflectsRemove() {
//         CaseInsensitiveMap<String, String> ciMap = new CaseInsensitiveMap<>();
//         ciMap.put("a", "1");
//         assertTrue(ciMap.getWrappedMap().containsKey("a"));
//         ciMap.remove("a");
//         assertFalse(ciMap.getWrappedMap().containsKey("a"));
//     }

    /**
     * Direct modifications of the wrapped map must be visible through the
     * {@code CaseInsensitiveMap} view.
     */
//     @Test
//     @DisplayName("DirectModificationThroughWrappedMap – view sees external changes")
//     void directModificationThroughWrappedMap() {
//         CaseInsensitiveMap<String, String> ciMap = new CaseInsensitiveMap<>();
//         Map<String, String> wrapped = ciMap.getWrappedMap();
//         wrapped.put("b", "2");
//         assertEquals("2", ciMap.get("b"));
//         assertEquals("2", ciMap.get("B"));
//     }
}