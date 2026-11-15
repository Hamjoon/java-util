package com.cedarsoftware.util;

import java.util.Map;
import java.util.HashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

// @ExtendWith(MockitoExtension.class)
class CompactMap_containsKey_Test {

    private CompactMap<String, Integer> map;

    /**
     * Creates a fresh CompactMap before each test.
     */
    @BeforeEach
    void setupBeforeEach() {
        map = new CompactMap<>();
    }

    /**
     * Clears the reference after each test.
     */
    @AfterEach
    void teardownAfterEach() {
        map = null;
    }

    /**
     * Creates a CompactMap whose {@code compactSize()} is overridden to {@code 2}
     * so that adding three entries forces the internal representation to switch
     * to a regular {@link Map}.
     */
    private CompactMap<String, Integer> createMapThatUsesDelegate() {
        return new CompactMap<String, Integer>() {

            @Override
            protected int compactSize() {
                // tiny size to trigger delegation quickly
                return 2;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                // default delegate
                return new HashMap<>();
            }
        };
    }

    @Test
    void containsKey_ShouldReturnFalse_WhenMapIsEmpty() {
        // Empty map must not contain any key, including null.
        assertFalse(map.containsKey("anyKey"), "Empty map must not contain any key");
        assertFalse(map.containsKey(null), "Empty map must not contain null key");
    }

    @Test
    void containsKey_ShouldFindExistingKey_InSingleEntryMap() {
        map.put("one", 1);
        assertTrue(map.containsKey("one"), "Key that was put must be found");
        assertFalse(map.containsKey("two"), "Different key must not be found");
        assertFalse(map.containsKey(null), "Null key must not be found when not present");
    }

    @Test
    void containsKey_ShouldHandleNullKey_InSingleEntryMap() {
        map.put(null, 42);
        assertTrue(map.containsKey(null), "Null key that was put must be found");
        assertFalse(map.containsKey("any"), "Non‑null key must not be found when only null key exists");
    }

    @Test
    void containsKey_ShouldFindKeys_WhenStoredInObjectArray() {
        // Default compactSize is 80, so 5 entries stay in the Object[] representation.
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);
        map.put("e", 5);
        assertTrue(map.containsKey("c"));
        assertTrue(map.containsKey("e"));
        assertFalse(map.containsKey("z"));
        assertFalse(map.containsKey(null));
    }

    @Test
    void containsKey_ShouldHandleNullKey_WhenStoredInObjectArray() {
        map.put("x", 10);
        map.put(null, 99);
        map.put("y", 20);
        assertTrue(map.containsKey(null));
        assertTrue(map.containsKey("x"));
        assertFalse(map.containsKey("z"));
    }

    @Test
    void containsKey_ShouldFindKeys_WhenUsingDelegateMap() {
        CompactMap<String, Integer> delegateMap = createMapThatUsesDelegate();
        // 3 entries -> internal Map (since compactSize() == 2)
        delegateMap.put("alpha", 1);
        delegateMap.put("beta", 2);
        delegateMap.put("gamma", 3);
        assertTrue(delegateMap.containsKey("beta"));
        assertTrue(delegateMap.containsKey("gamma"));
        assertFalse(delegateMap.containsKey("delta"));
        assertFalse(delegateMap.containsKey(null));
    }

    @Test
    void containsKey_ShouldHandleNullKey_WhenUsingDelegateMap() {
        CompactMap<String, Integer> delegateMap = createMapThatUsesDelegate();
        delegateMap.put(null, 777);
        delegateMap.put("k1", 1);
        // still > compactSize, uses delegate
        delegateMap.put("k2", 2);
        assertTrue(delegateMap.containsKey(null));
        assertTrue(delegateMap.containsKey("k1"));
        assertFalse(delegateMap.containsKey("missing"));
    }

    @Test
    void containsKey_EmptyMap_GroupCases() {
        // Group: EmptyMap
        assertFalse(map.containsKey("anyKey"));
        assertFalse(map.containsKey(null));
    }

    @Test
    void containsKey_SingleEntry_GroupCases() {
        // Group: SingleEntry
        map.put("one", 1);
        assertTrue(map.containsKey("one"));
        assertFalse(map.containsKey("missing"));
        assertFalse(map.containsKey(null));
    }

    @Test
    void containsKey_CompactArray_GroupCases() {
        // Group: CompactArray (2 entries, below threshold)
        map.put("alpha", 10);
        map.put("beta", 20);
        assertTrue(map.containsKey("alpha"));
        assertTrue(map.containsKey("beta"));
        assertFalse(map.containsKey("gamma"));
        assertFalse(map.containsKey(null));
    }

    @Test
    void containsKey_LargeMap_GroupCases() {
        // Group: LargeMap – force delegate by overriding compactSize to 2
        CompactMap<String, Integer> largeMap = createMapThatUsesDelegate();
        largeMap.put("key1", 1);
        largeMap.put("key15", 15);
        // null key allowed in delegate
        largeMap.put(null, 0);
        assertTrue(largeMap.containsKey("key15"));
        assertFalse(largeMap.containsKey("nonExisting"));
        assertTrue(largeMap.containsKey(null));
    }

    @Test
    void containsKey_NullKeyHandling_GroupCases() {
        // First case: null key in compact array state (size 2)
        map.put("a", 1);
        map.put(null, 2);
        assertTrue(map.containsKey(null));
        // Second case: null key after clearing and adding non‑null entry (single entry state)
        map.clear();
        map.put("b", 3);
        assertFalse(map.containsKey(null));
    }

    @Test
    void containsKey_SingleEntryNullKey_GroupCases() {
        // Group: SingleEntryNullKey
        map.put(null, 100);
        assertTrue(map.containsKey(null));
        assertFalse(map.containsKey("someKey"));
    }
}