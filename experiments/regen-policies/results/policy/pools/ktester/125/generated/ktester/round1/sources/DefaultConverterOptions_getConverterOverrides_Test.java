package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.convert.Converter.ConversionPair;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;

@ExtendWith(MockitoExtension.class)
class DefaultConverterOptions_getConverterOverrides_Test {

    private DefaultConverterOptions options;

    private static class ThrowingMap extends AbstractMap<ConversionPair, Convert<?>> {

        private final Map<ConversionPair, Convert<?>> delegate = new HashMap<>();

        @Override
        public Set<Entry<ConversionPair, Convert<?>>> entrySet() {
            return delegate.entrySet();
        }

        @Override
        public Convert<?> put(ConversionPair key, Convert<?> value) {
            throw new RuntimeException("Deliberate put failure");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh instance for each test to guarantee isolation
        options = new DefaultConverterOptions();
    }

    @Test
    void getConverterOverrides_ShouldReturnNonNullEmptyMapInitially() {
        // GIVEN
        // options instance created in @BeforeEach
        // WHEN
        Map<ConversionPair, Convert<?>> overrides = options.getConverterOverrides();
        // THEN
        assertNotNull(overrides, "The overrides map should never be null");
        assertTrue(overrides.isEmpty(), "The overrides map should be empty immediately after construction");
    }

    @Test
    void getConverterOverrides_ShouldReturnSameMutableMapInstance(@Mock Convert<Object> mockConvert) {
        // GIVEN
        Map<ConversionPair, Convert<?>> firstCall = options.getConverterOverrides();
        ConversionPair pair = mock(ConversionPair.class);
        // WHEN
        firstCall.put(pair, mockConvert);
        Map<ConversionPair, Convert<?>> secondCall = options.getConverterOverrides();
        // THEN
        assertSame(firstCall, secondCall, "Subsequent calls must return the same map instance");
        assertEquals(1, secondCall.size(), "The map should contain the entry that was added via the first reference");
        assertTrue(secondCall.containsKey(pair), "The map should contain the previously inserted key");
        assertEquals(mockConvert, secondCall.get(pair), "The value associated with the key should be the mock Convert instance");
    }

    @Test
    void getConverterOverrides_ShouldContainSingleCustomEntry(@Mock Convert<String> mockConvert) {
        // GIVEN
        Map<ConversionPair, Convert<?>> map = options.getConverterOverrides();
        ConversionPair pair = mock(ConversionPair.class);
        when(pair.toString()).thenReturn("String->Integer");
        // WHEN
        map.put(pair, mockConvert);
        // THEN
        assertEquals(1, map.size(), "Map should contain exactly one entry");
        assertSame(mockConvert, map.get(pair), "Stored value must be the mock Convert instance");
    }

    @Test
    void getConverterOverrides_ShouldContainTwoEntriesAfterAddingSecond(@Mock Convert<String> mockConvert1, @Mock Convert<Integer> mockConvert2) {
        // GIVEN
        Map<ConversionPair, Convert<?>> map = options.getConverterOverrides();
        ConversionPair pair1 = mock(ConversionPair.class);
        ConversionPair pair2 = mock(ConversionPair.class);
        when(pair1.toString()).thenReturn("String->Integer");
        when(pair2.toString()).thenReturn("Integer->String");
        // WHEN
        map.put(pair1, mockConvert1);
        map.put(pair2, mockConvert2);
        // THEN
        assertEquals(2, map.size(), "Map should contain two entries");
        assertTrue(map.containsKey(pair1) && map.containsKey(pair2), "Both keys must be present");
        assertEquals(mockConvert1, map.get(pair1));
        assertEquals(mockConvert2, map.get(pair2));
    }

    @Test
    void getConverterOverrides_ReferenceEquality_ModificationsReflectInternally(@Mock Convert<Object> mockConvert) {
        // GIVEN
        Map<ConversionPair, Convert<?>> first = options.getConverterOverrides();
        ConversionPair pair = mock(ConversionPair.class);
        // WHEN
        first.put(pair, mockConvert);
        Map<ConversionPair, Convert<?>> second = options.getConverterOverrides();
        // THEN
        assertSame(first, second, "Both calls must return the exact same map instance");
        assertEquals(1, second.size(), "Modification via first reference must be visible via second");
    }

    @Test
    void getConverterOverrides_WhenInternalMapIsNull_ReflectionSetsFieldToNull_ShouldReturnNull() throws Exception {
        // GIVEN – use reflection to corrupt the internal state
        Field f = DefaultConverterOptions.class.getDeclaredField("converterOverrides");
        f.setAccessible(true);
        f.set(options, null);
        // WHEN
        Map<ConversionPair, Convert<?>> result = options.getConverterOverrides();
        // THEN
        assertNull(result, "If the internal field is null, the getter should return null");
    }

    @Test
    void getConverterOverrides_ShouldBeInstanceOfConcurrentHashMap() {
        // WHEN
        Map<ConversionPair, Convert<?>> map = options.getConverterOverrides();
        // THEN
        assertTrue(map instanceof ConcurrentHashMap, "The map implementation should be a ConcurrentHashMap for thread‑safety");
    }

    @Test
    void getConverterOverrides_NullKeyInsertion_ShouldThrowNPE() {
        // GIVEN
        Map<ConversionPair, Convert<?>> map = options.getConverterOverrides();
        Convert<Object> dummy = mock(Convert.class);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> map.put(null, dummy), "ConcurrentHashMap must reject null keys");
    }

    @Test
    void getConverterOverrides_NullValueInsertion_ShouldThrowNPE() {
        // GIVEN
        Map<ConversionPair, Convert<?>> map = options.getConverterOverrides();
        ConversionPair pair = mock(ConversionPair.class);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> map.put(pair, null), "ConcurrentHashMap must reject null values");
    }

    @Test
    void getConverterOverrides_ConcurrentUpdates_MultipleThreads_ShouldMaintainAllEntries() throws Exception {
        // GIVEN
        final int threadCount = 10;
        final int entriesPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        Map<ConversionPair, Convert<?>> map = options.getConverterOverrides();
        Callable<Void> task = () -> {
            for (int i = 0; i < entriesPerThread; i++) {
                // each mock is a distinct key (reference equality)
                ConversionPair pair = mock(ConversionPair.class);
                Convert<Object> convert = mock(Convert.class);
                map.put(pair, convert);
            }
            return null;
        };
        // WHEN
        List<Future<Void>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(task));
        }
        for (Future<Void> f : futures) {
            // re‑throw any exception
            f.get();
        }
        executor.shutdownNow();
        // THEN
        int expectedSize = threadCount * entriesPerThread;
        assertEquals(expectedSize, map.size(), "After concurrent inserts the map should contain exactly " + expectedSize + " entries");
    }

    @Test
    void getConverterOverrides_MapReferenceAfterReflectionSetup_ShouldReturnInjectedMap() throws Exception {
        // GIVEN – create a map with known content
        Map<ConversionPair, Convert<?>> injected = new ConcurrentHashMap<>();
        ConversionPair key = mock(ConversionPair.class);
        Convert<Object> value = mock(Convert.class);
        injected.put(key, value);
        // inject via reflection
        Field f = DefaultConverterOptions.class.getDeclaredField("converterOverrides");
        f.setAccessible(true);
        f.set(options, injected);
        // WHEN
        Map<ConversionPair, Convert<?>> result = options.getConverterOverrides();
        // THEN
        assertSame(injected, result, "Getter must return the exact map instance injected via reflection");
        assertEquals(1, result.size());
        assertTrue(result.containsKey(key));
        assertEquals(value, result.get(key));
    }

    @Test
    void getConverterOverrides_ImmutableMapModification_ShouldThrowUnsupportedOperationException() throws Exception {
        // GIVEN – mutable map wrapped with Collections.unmodifiableMap
        Map<ConversionPair, Convert<?>> mutable = new ConcurrentHashMap<>();
        Map<ConversionPair, Convert<?>> unmodifiable = Collections.unmodifiableMap(mutable);
        Field f = DefaultConverterOptions.class.getDeclaredField("converterOverrides");
        f.setAccessible(true);
        f.set(options, unmodifiable);
        Map<ConversionPair, Convert<?>> result = options.getConverterOverrides();
        ConversionPair pair = mock(ConversionPair.class);
        Convert<Object> convert = mock(Convert.class);
        // WHEN / THEN
        assertThrows(UnsupportedOperationException.class, () -> result.put(pair, convert), "Attempting to modify an unmodifiable map should throw UnsupportedOperationException");
    }

    @Test
    void getConverterOverrides_CustomMapExceptionPropagation_ShouldPropagateRuntimeException() throws Exception {
        // GIVEN – inject a map that throws on put
        ThrowingMap throwingMap = new ThrowingMap();
        Field f = DefaultConverterOptions.class.getDeclaredField("converterOverrides");
        f.setAccessible(true);
        f.set(options, throwingMap);
        Map<ConversionPair, Convert<?>> result = options.getConverterOverrides();
        ConversionPair pair = mock(ConversionPair.class);
        Convert<Object> convert = mock(Convert.class);
        // WHEN / THEN
        RuntimeException ex = assertThrows(RuntimeException.class, () -> result.put(pair, convert), "The custom map's RuntimeException should be propagated");
        assertEquals("Deliberate put failure", ex.getMessage());
    }
}
