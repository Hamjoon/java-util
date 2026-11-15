package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_get_Test {

    private AbstractConcurrentNullSafeMap<String, String> map;

    @BeforeEach
    void setupBeforeEach() {
        // Use a plain ConcurrentHashMap as the backing store.
        ConcurrentMap<Object, Object> backing = new ConcurrentHashMap<>();
        map = new AbstractConcurrentNullSafeMap<String, String>(backing) {
        };
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to guarantee isolation between tests.
        map.clear();
    }

    @Test
    void getReturnsValueForExistingNonNullKey() {
        // given
        map.put("foo", "bar");
        // when
        String result = map.get("foo");
        // then
        assertEquals("bar", result, "get() should return the value associated with a non‑null key");
    }

    @Test
    void getReturnsNullWhenKeyIsAbsent() {
        // given – map is empty
        // when
        String result = map.get("missing");
        // then
        assertNull(result, "get() should return null for a key that is not present");
    }

    @Test
    void getHandlesNullKey() {
        // given
        map.put(null, "valueForNullKey");
        // when
        String result = map.get(null);
        // then
        assertEquals("valueForNullKey", result, "get() must correctly retrieve the value stored under a null key");
    }

    @Test
    void getHandlesNullValue() {
        // given
        map.put("keyWithNullValue", null);
        // when
        String result = map.get("keyWithNullValue");
        // then
        assertNull(result, "get() must return null when the stored value for a key is null");
    }

    @Test
    void getHandlesBothNullKeyAndNullValue() {
        // given
        map.put(null, null);
        // when
        String result = map.get(null);
        // then
        assertNull(result, "get() must return null when both the key and the associated value are null");
    }

    @Test
    void getThrowsClassCastExceptionForInvalidKeyType() {
        // The map is declared with String keys. Supplying an Integer should trigger
        // the cast (K)key inside get() and result in a ClassCastException.
        assertThrows(ClassCastException.class, () -> map.get(123), "Calling get() with a key of the wrong type must throw ClassCastException");
    }

    @Test
    void getThrowsNullPointerExceptionWhenInternalMapIsNull_withNullKey() {
        // Create a map instance whose internalMap reference is deliberately null.
        AbstractConcurrentNullSafeMap<String, String> mapWithNullBacking = new AbstractConcurrentNullSafeMap<String, String>(null) {
        };
        // Expect a NullPointerException because internalMap.get(...) dereferences a null.
        assertThrows(NullPointerException.class, () -> mapWithNullBacking.get(null), "If internalMap is null, get(null) must throw NullPointerException");
    }

    @Test
    void getThrowsNullPointerExceptionWhenInternalMapIsNull_withNonNullKey() {
        // Create a map instance whose internalMap reference is deliberately null.
        AbstractConcurrentNullSafeMap<String, String> mapWithNullBacking = new AbstractConcurrentNullSafeMap<String, String>(null) {
        };
        // Expect a NullPointerException because internalMap.get(...) dereferences a null.
        assertThrows(NullPointerException.class, () -> mapWithNullBacking.get("anyKey"), "If internalMap is null, get(non‑null key) must throw NullPointerException");
    }
}
