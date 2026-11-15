package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Collections;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CompactMap_isEmpty_Test {

    private CompactMap<String, Integer> map;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh instance before each test
        map = new CompactMap<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear the map to help GC (not strictly required)
        if (map != null) {
            map.clear();
        }
    }

    @Test
    @DisplayName("isEmpty() returns true for a newly created CompactMap")
    void testIsEmptyOnNewMap() {
        // when
        boolean empty = map.isEmpty();
        // then
        assertTrue(empty, "A newly created CompactMap should be empty");
    }

    @Test
    @DisplayName("Constructor with empty Map creates an empty CompactMap")
    void testConstructorWithEmptyMap() {
        // given
        Map<String, String> emptySource = new HashMap<>();
        // when
        CompactMap<String, String> constructed = new CompactMap<>(emptySource);
        // then
        assertTrue(constructed.isEmpty(), "CompactMap constructed with an empty Map should be empty");
    }

    @Test
    @DisplayName("Constructor with non‑empty Map creates a non‑empty CompactMap")
    void testConstructorWithNonEmptyMap() {
        // given
        Map<String, String> source = new HashMap<>();
        source.put("k1", "1");
        source.put("k2", "2");
        // when
        CompactMap<String, String> constructed = new CompactMap<>(source);
        // then
        assertFalse(constructed.isEmpty(), "CompactMap constructed with a non‑empty Map should not be empty");
        assertEquals(2, constructed.size(), "Size should match the source map");
    }

    @Test
    @DisplayName("isEmpty() remains true after putAll with an empty map")
    void testIsEmptyAfterPutAllEmpty() {
        // when
        map.putAll(Collections.emptyMap());
        // then
        assertTrue(map.isEmpty(), "Putting all from an empty map must keep CompactMap empty");
    }

    @Test
    @DisplayName("clear() on an already empty map leaves it empty")
    void testClearOnAlreadyEmpty() {
        // given
        assertTrue(map.isEmpty());
        // when
        map.clear();
        // then
        assertTrue(map.isEmpty(), "Calling clear on an empty map should keep it empty");
    }

    @Test
    @DisplayName("isEmpty() returns false after inserting a null value")
    void testIsEmptyAfterNullValue() {
        // given
        map.put("key", null);
        // when
        boolean empty = map.isEmpty();
        // then
        assertFalse(empty, "Map with a null value should not be empty");
    }
}
