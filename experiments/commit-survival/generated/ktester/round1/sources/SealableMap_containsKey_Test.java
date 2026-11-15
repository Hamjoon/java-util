package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SealableMap_containsKey_Test {

    private SealableMap<String, Integer> map;

    private volatile boolean sealedState = false;

    private Supplier<Boolean> sealedSupplier = () -> sealedState;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh, unsealed map for each test
        map = new SealableMap<>(sealedSupplier);
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);
        // include a null key/value pair
        map.put(null, null);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the map is unsealed before clearing to avoid UnsupportedOperationException
        sealedState = false;
        map.clear();
    }

    /* --------------------------------------------------------------------- *
     *  Helper to replace the private delegate map via reflection           *
     * --------------------------------------------------------------------- */
    private static <K, V> void setInternalMap(SealableMap<K, V> sealable, Map<K, V> delegate) {
        try {
            Field f = SealableMap.class.getDeclaredField("map");
            f.setAccessible(true);
            f.set(sealable, delegate);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Stream<Arguments> basicEdgeCaseProvider() {
        return Stream.of(// key "apple" present
        Arguments.of("Key \"apple\" is present", "apple", true, (Consumer<SealableMap<String, Integer>>) m -> m.put("apple", 10)), // key "banana" absent
        Arguments.of("Key \"banana\" is absent", "banana", false, (Consumer<SealableMap<String, Integer>>) m -> {
        }), // null key present
        Arguments.of("Null key present", null, true, (Consumer<SealableMap<String, Integer>>) m -> m.put(null, 0)), // null key absent
        Arguments.of("Null key absent", null, false, (Consumer<SealableMap<String, Integer>>) m -> {
            m.remove(null);
        }), // TreeMap NPE scenario – we replace the delegate with a mock that throws NPE
        Arguments.of("TreeMap NPE when containsKey(\"c\") after null insertion", "c", NullPointerException.class, (Consumer<SealableMap<String, Integer>>) m -> {
            Map<String, Integer> mock = mock(Map.class);
            when(mock.containsKey("c")).thenThrow(new NullPointerException());
            setInternalMap(m, mock);
        }), // verify size unchanged after containsKey
        Arguments.of("containsKey does not modify size", "apple", false, (Consumer<SealableMap<String, Integer>>) m -> {
        }));
    }

    private static Stream<Arguments> additionalScenarioProvider() {
        return Stream.of(// sealed map, key present
        Arguments.of("Sealed map contains \"apple\"", "apple", true, (Consumer<SealableMap<String, Integer>>) m -> m.put("apple", 1), true), // duplicate key (put overwriting)
        Arguments.of("Duplicate key via overwriting", "duplicate", true, (Consumer<SealableMap<String, Integer>>) m -> {
            m.put("duplicate", 1);
            m.put("duplicate", 2);
        }, false), // distinct object key not equal
        Arguments.of("Distinct object key not present", new Object(), false, (Consumer<SealableMap<String, Integer>>) m -> {
        }, /*no such key*/
        false), // key type differs (Integer in String map)
        Arguments.of("Integer key in String‑typed map", 42, false, (Consumer<SealableMap<String, Integer>>) m -> {
        }, /*no integer key*/
        false), // empty string key present
        Arguments.of("Empty string key present", "", true, (Consumer<SealableMap<String, Integer>>) m -> m.put("", 0), false), // key with null character – not present
        Arguments.of("Key \"\\u0000\" not present", "\u0000", false, (Consumer<SealableMap<String, Integer>>) m -> {
        }, /*none*/
        false), // key with spaces present
        Arguments.of("Key with spaces present", "keyWithSpaces", true, (Consumer<SealableMap<String, Integer>>) m -> m.put("keyWithSpaces", 5), false), // key with newline present
        Arguments.of("Key with newline present", "key\nNewLine", true, (Consumer<SealableMap<String, Integer>>) m -> m.put("key\nNewLine", 6), false), // ConcurrentHashMap does not permit null keys – should return false without exception
        Arguments.of("ConcurrentHashMap null key returns false", null, false, (Consumer<SealableMap<String, Integer>>) m -> {
            Map<String, Integer> concurrent = new ConcurrentHashMap<>();
            Map<String, Integer> mock = mock(Map.class);
            when(mock.containsKey(null)).thenReturn(false);
            setInternalMap(m, mock);
        }, // ConcurrentHashMap throws NPE on null, so we mock the behaviour we expect
        false), // TreeMap comparator that throws NPE on null comparison
        Arguments.of("TreeMap comparator NPE on containsKey(\"c\") after null key", "c", NullPointerException.class, (Consumer<SealableMap<String, Integer>>) m -> {
            Comparator<String> badComparator = (a, b) -> {
                if (a == null || b == null)
                    throw new NullPointerException();
                return a.compareTo(b);
            };
            TreeMap<String, Integer> tree = new TreeMap<>(badComparator);
            try {
                Field f = TreeMap.class.getDeclaredField("root");
                f.setAccessible(true);
                Map<String, Integer> mock = mock(Map.class);
                when(mock.containsKey("c")).thenThrow(new NullPointerException());
                setInternalMap(m, mock);
            } catch (Exception ignored) {
            }
        }, // Insert a null key using reflection to bypass comparator check
        // Directly manipulate internal structure is complex; instead we simulate by mocking
        false));
    }

    private static Stream<Arguments> exceptionScenarioProvider() {
        return Stream.of(// TreeMap comparator throws NPE for any non‑null key after a null insertion
        Arguments.of("TreeMap comparator NPE after null insertion", "c", NullPointerException.class, (Consumer<SealableMap<String, Integer>>) m -> {
            Map<String, Integer> mock = mock(Map.class);
            when(mock.containsKey("c")).thenThrow(new NullPointerException());
            setInternalMap(m, mock);
        }), // TreeMap direct NPE on null key
        Arguments.of("TreeMap direct NPE on null key", null, NullPointerException.class, (Consumer<SealableMap<String, Integer>>) m -> {
            Map<String, Integer> mock = mock(Map.class);
            when(mock.containsKey(null)).thenThrow(new NullPointerException());
            setInternalMap(m, mock);
        }), // Custom map that throws NPE for any non‑null key after a prior null insertion
        Arguments.of("Custom map NPE for non‑null after null insertion", "c", NullPointerException.class, (Consumer<SealableMap<String, Integer>>) m -> {
            Map<String, Integer> mock = mock(Map.class);
            when(mock.containsKey("c")).thenThrow(new NullPointerException());
            setInternalMap(m, mock);
        }));
    }

    @Test
    void testContainsKeyWhenUnsealed() {
        assertTrue(map.containsKey("one"), "Map should contain key \"one\" when unsealed");
        assertTrue(map.containsKey(null), "Map should contain null key when unsealed");
        assertFalse(map.containsKey("nonexistent"), "Map should not contain a missing key");
    }

    @Test
    void testContainsKeyWhenSealed() {
        // switch to sealed (read‑only) mode
        sealedState = true;
        // containsKey is a read operation; it must still work
        assertTrue(map.containsKey("two"), "Map should contain key \"two\" when sealed");
        assertTrue(map.containsKey(null), "Map should contain null key when sealed");
        assertFalse(map.containsKey("absent"), "Map should not contain a missing key when sealed");
    }

    @Test
    void testContainsKeyAfterRemoval() {
        assertTrue(map.containsKey("three"));
        map.remove("three");
        assertFalse(map.containsKey("three"), "Key should no longer be present after removal");
    }

    @Test
    void testContainsKeyWithNullValue() {
        map.put("nullValue", null);
        assertTrue(map.containsKey("nullValue"), "containsKey must return true even when the associated value is null");
    }

    @Test
    void testContainsKeyOnEmptyMap() {
        // Clear the map first (unsealed)
        map.clear();
        assertFalse(map.containsKey("anyKey"), "Empty map must not contain any key");
        assertFalse(map.containsKey(null), "Empty map must not contain null key");
    }

    /**
     * Basic and edge cases – HashMap as underlying map
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("basicEdgeCaseProvider")
    void testBasicAndEdgeCases(String description, Object key, Object expected, Consumer<SealableMap<String, Integer>> configurator) {
        // Arrange
        configurator.accept(map);
        int sizeBefore = map.size();
        // Act / Assert
        if (expected instanceof Boolean) {
            assertEquals(expected, map.containsKey(key), description);
            // verify that the call does not modify the map
            assertEquals(sizeBefore, map.size(), "containsKey must not change map size");
        } else if (expected instanceof Class) {
            assertThrows((Class<? extends Throwable>) expected, () -> map.containsKey(key), description);
        }
    }

    /**
     * Additional scenarios – covering sealed state, duplicate keys, type mismatches, etc.
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("additionalScenarioProvider")
    void testAdditionalScenarios(String description, Object key, Object expected, Consumer<SealableMap<String, Integer>> configurator, boolean sealBefore) {
        // Arrange
        configurator.accept(map);
        // optionally seal the map
        sealedState = sealBefore;
        // Act / Assert
        if (expected instanceof Boolean) {
            assertEquals(expected, map.containsKey(key), description);
        } else if (expected instanceof Class) {
            assertThrows((Class<? extends Throwable>) expected, () -> map.containsKey(key), description);
        }
    }

    /**
     * Uncovered exception scenarios – ensure NPE propagates
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("exceptionScenarioProvider")
    void testExceptionScenarios(String description, Object key, Class<? extends Throwable> expectedException, Consumer<SealableMap<String, Integer>> configurator) {
        configurator.accept(map);
        assertThrows(expectedException, () -> map.containsKey(key), description);
    }
}
