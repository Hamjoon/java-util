package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.mockito.Mockito.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Method;
import java.util.function.BiFunction;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_remove_Test {

    private ConcurrentMap<Object, Object> internalMapSpy;

    private TestMap<String, String> map;

    /**
     * Concrete implementation used only for testing
     */
    private static final class TestMap<K, V> extends AbstractConcurrentNullSafeMap<K, V> {

        TestMap(ConcurrentMap<Object, Object> internalMap) {
            super(internalMap);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use a real ConcurrentHashMap as the backing store and spy on it
        ConcurrentHashMap<Object, Object> realMap = new ConcurrentHashMap<>();
        internalMapSpy = Mockito.spy(realMap);
        map = new TestMap<>(internalMapSpy);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to avoid cross‑test contamination
        map.clear();
    }

    @Test
    void removeExistingNonNullKey_returnsAssociatedValue_andRemovesEntry() {
        // given
        map.put("alpha", "A");
        map.put("beta", "B");
        // when
        String removed = map.remove("alpha");
        // then
        assertEquals("A", removed, "remove should return the previous value");
        assertNull(map.get("alpha"), "key should no longer be present after removal");
        assertEquals("B", map.get("beta"), "other entries must stay untouched");
        // verify that the internal map received the masked key (for non‑null key it's the same)
        verify(internalMapSpy, times(1)).remove(argThat(k -> "alpha".equals(k)));
    }

    @Test
    void removeExistingNullKey_returnsAssociatedValue_andRemovesEntry() {
        // given
        map.put(null, "nullKeyValue");
        map.put("regular", "value");
        // when
        String removed = map.remove(null);
        // then
        assertEquals("nullKeyValue", removed, "remove should correctly handle a null key");
        assertNull(map.get(null), "null key must be removed");
        assertEquals("value", map.get("regular"), "other entries must stay untouched");
        // The internal map receives a sentinel object for the null key.
        // We cannot know the exact sentinel class, but we can assert that the argument is not null.
        verify(internalMapSpy, times(1)).remove(argThat(arg -> arg != null));
    }

    @Test
    void removeMissingKey_returnsNull_andLeavesMapUnchanged() {
        // given
        map.put("present", "yes");
        // when
        String removed = map.remove("absent");
        // then
        assertNull(removed, "remove of a non‑existent key must return null");
        assertEquals("yes", map.get("present"), "existing entry must remain untouched");
        // internal map's remove should still be called once
        verify(internalMapSpy, times(1)).remove(argThat(key -> key.equals("absent")));
    }

    @Test
    void removeExistingKeyWithNullValue_returnsNull_andRemovesEntry() {
        // given
        // null value is stored in masked form
        map.put("beta", null);
        map.put("gamma", "G");
        // when
        String removed = map.remove("beta");
        // then
        assertNull(removed, "remove should return null when the associated value is null");
        assertNull(map.get("beta"), "null‑value key must be removed");
        assertEquals("G", map.get("gamma"), "other entries must stay untouched");
        verify(internalMapSpy, times(1)).remove(argThat(k -> "beta".equals(k)));
    }

    @Test
    void removeNullKey_returnsAssociatedValue_andRemovesEntry() throws Exception {
        // given
        // null key is masked internally
        map.put(null, "Z");
        map.put("regular", "R");
        // when
        String removed = map.remove(null);
        // then
        assertEquals("Z", removed, "remove should correctly handle a null key");
        assertNull(map.get(null), "null key must be removed");
        assertEquals("R", map.get("regular"), "other entries must stay untouched");
        // The internal map receives a sentinel object for the null key.
        // We cannot know the exact sentinel class, but we can assert that the argument is not null.
        verify(internalMapSpy, times(1)).remove(argThat(arg -> arg != null));
    }

    @Test
    void removeFromEmptyMap_anyKey_returnsNull() {
        // map is empty by default
        String removed = map.remove("anyKey");
        assertNull(removed, "remove on empty map should return null for any key");
        verify(internalMapSpy, times(1)).remove(argThat(k -> "anyKey".equals(k)));
    }

    @Test
    void removeFromEmptyMap_nullKey_returnsNull() {
        // map is empty by default
        String removed = map.remove(null);
        assertNull(removed, "remove on empty map with null key should return null");
        verify(internalMapSpy, times(1)).remove(argThat(arg -> arg != null));
    }

    @Test
    void removeExistingEntry_reducesSize_andKeyIsAbsent() {
        // given
        map.put("delta", "D");
        map.put("epsilon", "E");
        int sizeBefore = map.size();
        // when
        String removed = map.remove("delta");
        // then
        assertEquals("D", removed);
        assertEquals(sizeBefore - 1, map.size(), "size should be reduced by one after removal");
        assertFalse(map.containsKey("delta"), "removed key must no longer be present");
        assertTrue(map.containsKey("epsilon"), "other keys must remain");
    }

    @Test
    void removeWithKeyOfWrongType_Integer_throwsClassCastException() {
        assertThrows(ClassCastException.class, () -> map.remove(12345), "Removing with an Integer key should throw ClassCastException");
    }

    @Test
    void removeWithKeyOfWrongType_Object_throwsClassCastException() {
        assertThrows(ClassCastException.class, () -> map.remove(new Object()), "Removing with an Object key should throw ClassCastException");
    }

    @Test
    void removeWhenStoredValueHasWrongType_throwsClassCastException() throws Exception {
        // Prepare internal map with a correct masked key but an Integer value
        // Use reflection to call the protected maskNullKey method
        Method maskKeyMethod = AbstractConcurrentNullSafeMap.class.getDeclaredMethod("maskNullKey", Object.class);
        maskKeyMethod.setAccessible(true);
        Object maskedKey = maskKeyMethod.invoke(map, "mismatchKey");
        // Directly put an Integer value (wrong type) into the internal map
        internalMapSpy.put(maskedKey, 42);
        // The map reports that the key exists (containsKey uses maskNullKey as well)
        assertTrue(map.containsKey("mismatchKey"));
        // Attempt to remove – should trigger ClassCastException when unmasking the value
        assertThrows(ClassCastException.class, () -> map.remove("mismatchKey"), "Removing a key whose stored value type is incompatible should throw ClassCastException");
    }

    @Test
    void removeNullKeyWhenAbsent_returnsNull() {
        // map contains only a regular entry
        map.put("regular", "R");
        String removed = map.remove(null);
        assertNull(removed, "Removing a null key that is not present should return null");
        assertFalse(map.containsKey(null), "Map must not contain a null key after removal attempt");
        verify(internalMapSpy, times(1)).remove(argThat(arg -> arg != null));
    }
}
