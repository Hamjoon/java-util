package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

@ExtendWith(MockitoExtension.class)
class DefaultConverterOptions_getConverterOverrides_Test {

    /**
     * Instance under test.
     */
    private DefaultConverterOptions options;

    /**
     * A mock {@link Convert} implementation used for override entries.
     */
    @Mock
    private Convert<Object> mockConvert;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh instance for each test to guarantee isolation.
        options = new DefaultConverterOptions();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references – helps the garbage collector and avoids accidental reuse.
        options = null;
    }

    /**
     * Instantiates {@link DefaultConverterOptions} with a map argument.
     *
     * <p>The library may expose a public constructor taking a {@code Map}. If such a constructor
     * is not public (or does not exist), reflection is used to locate a suitable constructor
     * (any constructor with a single {@code Map} parameter) and make it accessible.
     *
     * @param map the map to pass to the constructor; may be {@code null}
     * @return a new {@code DefaultConverterOptions} instance
     * @throws Exception if reflection fails
     */
    private static DefaultConverterOptions constructWithMap(Map<Map.Entry<Class<?>, Class<?>>, Convert<?>> map) throws Exception {
        // Try to find a public constructor first
        try {
            return new DefaultConverterOptions(map);
        } catch (NoSuchMethodError | IllegalArgumentException | NoSuchMethodException e) {
            // Fall back to reflection – locate any constructor with a single Map argument
            for (java.lang.reflect.Constructor<?> ctor : DefaultConverterOptions.class.getDeclaredConstructors()) {
                Class<?>[] params = ctor.getParameterTypes();
                if (params.length == 1 && Map.class.isAssignableFrom(params[0])) {
                    ctor.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    DefaultConverterOptions instance = (DefaultConverterOptions) ctor.newInstance(map);
                    return instance;
                }
            }
            // If we reach this point, no suitable constructor exists – rethrow for test failure clarity
            throw new IllegalStateException("DefaultConverterOptions does not have a constructor accepting a Map.", e);
        }
    }

    @Test
    void getConverterOverrides_ShouldReturnEmptyMapInitially() {
        // GIVEN
        // (options already created in @BeforeEach)
        // WHEN
        Map<Map.Entry<Class<?>, Class<?>>, Convert<?>> overrides = options.getConverterOverrides();
        // THEN
        assertNotNull(overrides, "The returned map must never be null.");
        assertTrue(overrides.isEmpty(), "A newly constructed DefaultConverterOptions should contain no overrides.");
    }

    @Test
    void getConverterOverrides_ShouldReturnSameInstanceOnRepeatedCalls() {
        // GIVEN
        Map<Map.Entry<Class<?>, Class<?>>, Convert<?>> firstCall = options.getConverterOverrides();
        // WHEN
        Map<Map.Entry<Class<?>, Class<?>>, Convert<?>> secondCall = options.getConverterOverrides();
        // THEN
        assertSame(firstCall, secondCall, "Subsequent calls must return the exact same map instance.");
    }

    @Test
    void getConverterOverrides_ShouldReflectAddedOverride() {
        // GIVEN
        Map<Map.Entry<Class<?>, Class<?>>, Convert<?>> overrides = options.getConverterOverrides();
        // Create a distinct key representing a conversion from String to Integer.
        Map.Entry<Class<?>, Class<?>> key = new AbstractMap.SimpleEntry<>(String.class, Integer.class);
        // Add the mock Convert implementation as an override.
        overrides.put(key, mockConvert);
        // WHEN
        Map<Map.Entry<Class<?>, Class<?>>, Convert<?>> retrieved = options.getConverterOverrides();
        // THEN
        assertEquals(1, retrieved.size(), "The map should contain exactly one entry after insertion.");
        assertTrue(retrieved.containsKey(key), "The map must contain the key that was added.");
        assertSame(mockConvert, retrieved.get(key), "The stored Convert instance must be the same as the one inserted.");
    }

    @Test
    void getConverterOverrides_ShouldBeConcurrentHashMap() {
        // GIVEN
        Map<Map.Entry<Class<?>, Class<?>>, Convert<?>> overrides = options.getConverterOverrides();
        // THEN
        assertTrue(overrides instanceof ConcurrentHashMap, "The internal map should be a ConcurrentHashMap to guarantee thread‑safety.");
    }

    @Test
    void constructor_WithPrePopulatedMap_ReturnsSameInstanceAndContainsEntry() throws Exception {
        // GIVEN – a pre‑populated map (HashMap works fine)
        Map<Map.Entry<Class<?>, Class<?>>, Convert<?>> preMap = new HashMap<>();
        Map.Entry<Class<?>, Class<?>> key = new AbstractMap.SimpleEntry<>(String.class, String.class);
        preMap.put(key, mockConvert);
        // WHEN – instantiate via the (presumed) constructor that accepts a map
        DefaultConverterOptions opt = constructWithMap(preMap);
        // THEN
        Map<Map.Entry<Class<?>, Class<?>>, Convert<?>> returned = opt.getConverterOverrides();
        assertSame(preMap, returned, "The returned map must be the exact same instance supplied to the constructor.");
        assertEquals(1, returned.size(), "The map should contain the pre‑populated entry.");
        assertSame(mockConvert, returned.get(key), "The stored Convert must be the mock supplied in the pre‑populated map.");
    }

    @Test
    void constructor_WithNullMap_ShouldCreateEmptyConcurrentHashMap() throws Exception {
        // GIVEN a null argument
        // WHEN
        DefaultConverterOptions opt = constructWithMap(null);
        // THEN
        Map<Map.Entry<Class<?>, Class<?>>, Convert<?>> map = opt.getConverterOverrides();
        assertNotNull(map, "Even when constructed with null, the map must never be null.");
        assertTrue(map.isEmpty(), "The map should be empty when constructed with a null argument.");
        assertTrue(map instanceof ConcurrentHashMap, "When null is supplied, the implementation should fall back to a ConcurrentHashMap.");
    }

    @Test
    void constructor_WithUnmodifiableMap_ShouldReturnSameUnmodifiableInstance() throws Exception {
        // GIVEN – an unmodifiable map wrapping a single entry
        Map<Map.Entry<Class<?>, Class<?>>, Convert<?>> mutable = new HashMap<>();
        Map.Entry<Class<?>, Class<?>> key = new AbstractMap.SimpleEntry<>(Integer.class, Long.class);
        mutable.put(key, mockConvert);
        Map<Map.Entry<Class<?>, Class<?>>, Convert<?>> unmodifiable = Collections.unmodifiableMap(mutable);
        // WHEN
        DefaultConverterOptions opt = constructWithMap(unmodifiable);
        // THEN
        Map<Map.Entry<Class<?>, Class<?>>, Convert<?>> returned = opt.getConverterOverrides();
        assertSame(unmodifiable, returned, "The returned map must be the exact unmodifiable map supplied to the constructor.");
        // Attempt a modification – should throw UnsupportedOperationException
        assertThrows(UnsupportedOperationException.class, () -> returned.put(new AbstractMap.SimpleEntry<>(Float.class, Double.class), mockConvert), "Modifying the unmodifiable map should raise UnsupportedOperationException.");
    }

    @Test
    void concurrentModifications_ShouldBeVisibleAcrossThreads() throws Exception {
        // GIVEN – a DefaultConverterOptions with its default (ConcurrentHashMap) map
        DefaultConverterOptions opt = new DefaultConverterOptions();
        // Latches to orchestrate ordering
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        AtomicReference<Map<Map.Entry<Class<?>, Class<?>>, Convert<?>>> threadBMapRef = new AtomicReference<>();
        // Thread A – inserts an entry
        Thread threadA = new Thread(() -> {
            try {
                // wait until both threads are ready
                startLatch.await();
                Map<Map.Entry<Class<?>, Class<?>>, Convert<?>> map = opt.getConverterOverrides();
                map.put(new AbstractMap.SimpleEntry<>(String.class, Integer.class), mockConvert);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        });
        // Thread B – reads the map after insertion
        Thread threadB = new Thread(() -> {
            try {
                startLatch.await();
                // Small pause to increase chance that A writes first
                Thread.sleep(10);
                threadBMapRef.set(opt.getConverterOverrides());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        });
        // Start both threads
        threadA.start();
        threadB.start();
        // Release them simultaneously
        startLatch.countDown();
        // Wait for completion
        assertTrue(doneLatch.await(2, TimeUnit.SECONDS), "Both threads should finish within timeout.");
        // THEN – Thread B should see the entry added by Thread A
        Map<Map.Entry<Class<?>, Class<?>>, Convert<?>> mapSeenByB = threadBMapRef.get();
        assertNotNull(mapSeenByB, "Thread B must have obtained a map reference.");
        assertEquals(1, mapSeenByB.size(), "The map must contain the entry added concurrently.");
        Map.Entry<Class<?>, Class<?>> expectedKey = new AbstractMap.SimpleEntry<>(String.class, Integer.class);
        assertTrue(mapSeenByB.containsKey(expectedKey), "The concurrent map should contain the expected key.");
        assertSame(mockConvert, mapSeenByB.get(expectedKey), "The value associated with the key must be the mock Convert.");
    }

    @Test
    void map_ShouldAcceptNullKeyAndNullValue() throws Exception {
        // GIVEN – a map that permits nulls (HashMap) with a null key and a null value entry
        Map<Map.Entry<Class<?>, Class<?>>, Convert<?>> mutable = new HashMap<>();
        // null key
        mutable.put(null, mockConvert);
        // null value (using a distinct key)
        Map.Entry<Class<?>, Class<?>> keyForNullValue = new AbstractMap.SimpleEntry<>(String.class, String.class);
        mutable.put(keyForNullValue, null);
        // WHEN – construct options with this map
        DefaultConverterOptions opt = constructWithMap(mutable);
        // THEN – both entries must be present unchanged
        Map<Map.Entry<Class<?>, Class<?>>, Convert<?>> returned = opt.getConverterOverrides();
        assertEquals(2, returned.size(), "Map should contain both the null‑key and null‑value entries.");
        assertTrue(returned.containsKey(null), "Map must contain the null key entry.");
        assertSame(mockConvert, returned.get(null), "The value for the null key must be the mock Convert.");
        assertTrue(returned.containsKey(keyForNullValue), "Map must contain the key whose value is null.");
        assertNull(returned.get(keyForNullValue), "The value for the key should be null.");
    }
}
