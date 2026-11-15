package com.cedarsoftware.util;

import java.util.*;

// import org.junit.*;
// import static org.junit.Assert.*;
// @ExtendWith(MockitoExtension.class)
class CaseInsensitiveMap_keySet_Test {

    private CaseInsensitiveMap<String, String> stringMap;

    private CaseInsensitiveMap<Integer, String> intMap;

    //     @BeforeEach
    void setupBeforeEach() {
        // Map with String keys – case‑insensitive handling is exercised here
        stringMap = new CaseInsensitiveMap<>();
        stringMap.put("One", "1");
        stringMap.put("Two", "2");
        stringMap.put("Three", "3");
        // Map with non‑String keys – should behave like a normal map
        intMap = new CaseInsensitiveMap<>();
        intMap.put(1, "one");
        intMap.put(2, "two");
    }

    //     @AfterEach
    void teardownAfterEach() {
        // Reset static cache configuration to avoid side‑effects on other tests
        CaseInsensitiveMap.setMaxCacheLengthString(100);
        CaseInsensitiveMap.replaceCache(new LRUCache<>(5000, LRUCache.StrategyType.THREADED));
    }

    // -----------------------------------------------------------------------
    void setup() {
        // Map with String keys – case‑insensitive handling is exercised here
        stringMap = new CaseInsensitiveMap<>();
        stringMap.put("One", "1");
        stringMap.put("Two", "2");
        stringMap.put("Three", "3");
        // Map with non‑String keys – should behave like a normal map
        intMap = new CaseInsensitiveMap<>();
        intMap.put(1, "one");
        intMap.put(2, "two");
    }

    void teardown() {
        // Reset static cache configuration to avoid side‑effects on other code
        CaseInsensitiveMap.setMaxCacheLengthString(100);
        CaseInsensitiveMap.replaceCache(new LRUCache<>(5000, LRUCache.StrategyType.THREADED));
    }

    // -----------------------------------------------------------------------
//     public static void main(String[] args) {
//         CaseInsensitiveMapKeySetDemo demo = new CaseInsensitiveMapKeySetDemo();
//         demo.setup();
//         // 1. Verify that the iterator returns the original key case
//         Set<String> ks = demo.stringMap.keySet();
//         Iterator<String> it = ks.iterator();
//         List<String> iterated = new ArrayList<>();
//         while (it.hasNext()) {
//             iterated.add(it.next());
//         }
//         System.out.println("Iterated keys (original case): " + iterated);
//         // 2. Case‑insensitive contains()
//         System.out.println("Contains \"one\": " + ks.contains("one"));
//         System.out.println("Contains \"ONE\": " + ks.contains("ONE"));
//         System.out.println("Contains \"OnE\": " + ks.contains("OnE"));
//         System.out.println("Contains \"Four\": " + ks.contains("Four"));
//         // 3. Remove using a different case
//         boolean removed = ks.remove("tWo");
//         System.out.println("Removed \"tWo\": " + removed);
//         System.out.println("Map size after removal: " + demo.stringMap.size());
//         System.out.println("Contains \"Two\" after removal: " + ks.contains("Two"));
//         // 4. Size reflects map size
//         System.out.println("KeySet size: " + ks.size());
//         // 5. toArray preserves original case
//         String[] array = ks.toArray(new String[0]);
//         System.out.println("toArray result: " + Arrays.toString(array));
//         // 6. Working with non‑String keys
//         Set<Integer> intKs = demo.intMap.keySet();
//         System.out.println("Integer key set: " + intKs);
//         System.out.println("Remove key 1: " + intKs.remove(1));
//         System.out.println("Remaining integer keys: " + intKs);
//         demo.teardown();
//     }
}