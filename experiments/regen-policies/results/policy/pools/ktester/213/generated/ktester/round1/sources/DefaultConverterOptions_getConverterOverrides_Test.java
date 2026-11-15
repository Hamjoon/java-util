package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import java.util.Map;
import java.lang.reflect.Field;
import java.util.AbstractMap;
import java.util.concurrent.ConcurrentHashMap;

@ExtendWith(MockitoExtension.class)
class DefaultConverterOptions_getConverterOverrides_Test {

    /**
     * Returns a fresh {@link DefaultConverterOptions} instance whose
     *  {@code converterOverrides} field has been replaced by {@code map}
     *  via reflection.
     */
    private static DefaultConverterOptions withMap(Map<ConversionPair, Convert<?>> map) throws Exception {
        DefaultConverterOptions options = new DefaultConverterOptions();
        Field f = DefaultConverterOptions.class.getDeclaredField("converterOverrides");
        f.setAccessible(true);
        f.set(options, map);
        return options;
    }

    /**
     * Creates a dummy {@link ConversionPair} mock.
     */
    private static ConversionPair dummyKey() {
        return mock(ConversionPair.class);
    }

    @Test
    void getConverterOverrides_ShouldReturnNonNullEmptyMapInitially() {
        // GIVEN
        DefaultConverterOptions options = new DefaultConverterOptions();
        // WHEN
        Map<ConversionPair, Convert<?>> overrides = options.getConverterOverrides();
        // THEN
        assertNotNull(overrides, "The returned map must never be null.");
        assertTrue(overrides.isEmpty(), "The map should be empty immediately after construction.");
    }

    @Test
    void getConverterOverrides_ShouldReturnLiveMapAndSameInstanceOnRepeatedCalls() {
        // GIVEN
        DefaultConverterOptions options = new DefaultConverterOptions();
        Map<ConversionPair, Convert<?>> firstCall = options.getConverterOverrides();
        // Use Mockito to create dummy key/value objects.
        ConversionPair dummyKey = mock(ConversionPair.class);
        @SuppressWarnings("unchecked")
        Convert<Object> dummyValue = mock(Convert.class);
        // WHEN
        // mutate the map returned by the first call
        firstCall.put(dummyKey, dummyValue);
        Map<ConversionPair, Convert<?>> secondCall = options.getConverterOverrides();
        // THEN
        // The two references must be the same object (the map is stored internally).
        assertSame(firstCall, secondCall, "Subsequent calls must return the same map instance.");
        // The mutation performed through the first reference must be visible via the second.
        assertEquals(1, secondCall.size(), "Map should contain exactly one entry after insertion.");
        assertTrue(secondCall.containsKey(dummyKey), "Map must contain the inserted key.");
        assertEquals(dummyValue, secondCall.get(dummyKey), "Map must return the inserted value for the key.");
    }

    /**
     * Creates a dummy {@link Convert} mock.
     */
    @SuppressWarnings("unchecked")
    private static Convert<Object> dummyValue() {
        return mock(Convert.class);
    }

    @Test
    void emptyConverterOverridesMap_ShouldReturnSameEmptyConcurrentHashMap() throws Exception {
        // GIVEN – a fresh instance (the constructor already creates an empty ConcurrentHashMap)
        DefaultConverterOptions options = new DefaultConverterOptions();
        // WHEN
        Map<ConversionPair, Convert<?>> map = options.getConverterOverrides();
        // THEN
        assertNotNull(map, "Map must not be null.");
        assertTrue(map.isEmpty(), "Map should be empty.");
        assertTrue(map instanceof ConcurrentHashMap, "Map implementation must be ConcurrentHashMap.");
    }

    @Test
    void nonEmptyConverterOverridesMap_ShouldPreservePrePopulatedEntry() throws Exception {
        // GIVEN – a map with one entry injected via reflection
        ConversionPair key = dummyKey();
        Convert<Object> value = dummyValue();
        Map<ConversionPair, Convert<?>> prePopulated = new ConcurrentHashMap<>();
        prePopulated.put(key, value);
        DefaultConverterOptions options = withMap(prePopulated);
        // WHEN
        Map<ConversionPair, Convert<?>> map = options.getConverterOverrides();
        // THEN
        assertSame(prePopulated, map, "Returned map must be the exact instance injected.");
        assertEquals(1, map.size(), "Map should contain the pre‑populated entry.");
        assertEquals(value, map.get(key), "Stored value must match the pre‑populated one.");
    }

    @Test
    void nonEmptyConverterOverridesMap_ModificationsAfterRetrievalAreVisible() throws Exception {
        // GIVEN – a map with a single entry injected via reflection
        ConversionPair key1 = dummyKey();
        Convert<Object> value1 = dummyValue();
        Map<ConversionPair, Convert<?>> base = new ConcurrentHashMap<>();
        base.put(key1, value1);
        DefaultConverterOptions options = withMap(base);
        // WHEN – retrieve and add a second entry
        Map<ConversionPair, Convert<?>> map = options.getConverterOverrides();
        ConversionPair key2 = dummyKey();
        Convert<Object> value2 = dummyValue();
        map.put(key2, value2);
        // THEN
        assertEquals(2, map.size(), "Map should contain two entries after the second insertion.");
        assertEquals(value2, map.get(key2), "Second entry must be present.");
    }

    @Test
    void nullConverterOverridesField_ShouldReturnNullWithoutException() throws Exception {
        // GIVEN – set the private final field to null via reflection
        DefaultConverterOptions options = new DefaultConverterOptions();
        Field f = DefaultConverterOptions.class.getDeclaredField("converterOverrides");
        f.setAccessible(true);
        f.set(options, null);
        // WHEN
        Map<ConversionPair, Convert<?>> result = options.getConverterOverrides();
        // THEN
        assertNull(result, "When the internal field is null, the getter should return null.");
    }

    @Test
    void referenceEqualityAcrossCalls_ShouldReturnIdenticalMapInstance() {
        // GIVEN
        DefaultConverterOptions options = new DefaultConverterOptions();
        // WHEN
        Map<ConversionPair, Convert<?>> first = options.getConverterOverrides();
        Map<ConversionPair, Convert<?>> second = options.getConverterOverrides();
        // THEN
        assertSame(first, second, "Two calls must return the exact same Map instance.");
    }

    @Test
    void mapTypeIsConcurrentHashMap_ShouldBeThreadSafeImplementation() {
        // GIVEN
        DefaultConverterOptions options = new DefaultConverterOptions();
        // WHEN
        Map<ConversionPair, Convert<?>> map = options.getConverterOverrides();
        // THEN
        assertTrue(map instanceof ConcurrentHashMap, "Returned map must be an instance of ConcurrentHashMap.");
    }

    @Test
    void concurrentModificationVisibility_ShouldBeSeenByMainThread() throws Exception {
        // GIVEN
        DefaultConverterOptions options = new DefaultConverterOptions();
        Map<ConversionPair, Convert<?>> map = options.getConverterOverrides();
        // Background thread adds an entry
        Thread t = new Thread(() -> {
            ConversionPair bgKey = dummyKey();
            Convert<Object> bgValue = dummyValue();
            map.put(bgKey, bgValue);
        });
        t.start();
        // wait for background work
        t.join();
        // THEN – main thread sees the entry
        assertEquals(1, map.size(), "Entry added by background thread must be visible.");
    }

    @Test
    void nullKeyInsertion_ShouldThrowNullPointerException() {
        // GIVEN
        DefaultConverterOptions options = new DefaultConverterOptions();
        Map<ConversionPair, Convert<?>> map = options.getConverterOverrides();
        Convert<Object> dummy = dummyValue();
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> map.put(null, dummy), "Putting a null key into ConcurrentHashMap must throw NPE.");
    }

    @Test
    void nullValueInsertion_ShouldThrowNullPointerException() {
        // GIVEN
        DefaultConverterOptions options = new DefaultConverterOptions();
        Map<ConversionPair, Convert<?>> map = options.getConverterOverrides();
        ConversionPair dummyKey = dummyKey();
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> map.put(dummyKey, null), "Putting a null value into ConcurrentHashMap must throw NPE.");
    }

    @Test
    void clearMapAfterRetrieval_ShouldEmptyInternalState() throws Exception {
        // GIVEN – pre‑populate map with two entries via reflection
        Map<ConversionPair, Convert<?>> pre = new ConcurrentHashMap<>();
        pre.put(dummyKey(), dummyValue());
        pre.put(dummyKey(), dummyValue());
        DefaultConverterOptions options = withMap(pre);
        // WHEN
        Map<ConversionPair, Convert<?>> map = options.getConverterOverrides();
        map.clear();
        // THEN
        assertTrue(map.isEmpty(), "Map should be empty after clear().");
        assertTrue(options.getConverterOverrides().isEmpty(), "Internal map must also be empty after clear().");
    }

    @Test
    void nullKeyRemoval_ShouldThrowNullPointerException() {
        // GIVEN
        DefaultConverterOptions options = new DefaultConverterOptions();
        Map<ConversionPair, Convert<?>> map = options.getConverterOverrides();
        Convert<Object> dummy = dummyValue();
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> map.remove(null, dummy), "Removing with a null key must throw NPE.");
    }

    @Test
    void nullKeyReplace_ShouldThrowNullPointerException() {
        // GIVEN
        DefaultConverterOptions options = new DefaultConverterOptions();
        Map<ConversionPair, Convert<?>> map = options.getConverterOverrides();
        Convert<Object> dummy = dummyValue();
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> map.replace(null, dummy), "replace(null, value) must throw NPE.");
    }

    @Test
    void nullKeyReplaceIfAbsent_ShouldThrowNullPointerException() {
        // GIVEN
        DefaultConverterOptions options = new DefaultConverterOptions();
        Map<ConversionPair, Convert<?>> map = options.getConverterOverrides();
        Convert<Object> dummy = dummyValue();
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> map.replace(null, dummy), "replaceIfAbsent(null, value) must throw NPE.");
    }

    @Test
    void nullKeyPutIfAbsent_ShouldThrowNullPointerException() {
        // GIVEN
        DefaultConverterOptions options = new DefaultConverterOptions();
        Map<ConversionPair, Convert<?>> map = options.getConverterOverrides();
        Convert<Object> dummy = dummyValue();
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> map.putIfAbsent(null, dummy), "putIfAbsent(null, value) must throw NPE.");
    }

    @Test
    void nullKeyComputeIfAbsent_ShouldThrowNullPointerException() {
        // GIVEN
        DefaultConverterOptions options = new DefaultConverterOptions();
        Map<ConversionPair, Convert<?>> map = options.getConverterOverrides();
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> map.computeIfAbsent(null, k -> dummyValue()), "computeIfAbsent(null, ...) must throw NPE.");
    }

    @Test
    void nullKeyCompute_ShouldThrowNullPointerException() {
        // GIVEN
        DefaultConverterOptions options = new DefaultConverterOptions();
        Map<ConversionPair, Convert<?>> map = options.getConverterOverrides();
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> map.compute(null, (k, v) -> dummyValue()), "compute(null, ...) must throw NPE.");
    }

    @Test
    void nullKeyMerge_ShouldThrowNullPointerException() {
        // GIVEN
        DefaultConverterOptions options = new DefaultConverterOptions();
        Map<ConversionPair, Convert<?>> map = options.getConverterOverrides();
        Convert<Object> dummy = dummyValue();
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> map.merge(null, dummy, (oldV, newV) -> dummy), "merge(null, ...) must throw NPE.");
    }

    @Test
    void putAllWithNullKeyOrValue_ShouldThrowNullPointerException() {
        // GIVEN
        DefaultConverterOptions options = new DefaultConverterOptions();
        Map<ConversionPair, Convert<?>> map = options.getConverterOverrides();
        // Map containing a null key
        Map<ConversionPair, Convert<?>> badKeyMap = new ConcurrentHashMap<>();
        badKeyMap.put(null, dummyValue());
        // Map containing a null value
        Map<ConversionPair, Convert<?>> badValueMap = new ConcurrentHashMap<>();
        badValueMap.put(dummyKey(), null);
        // WHEN / THEN – null key
        assertThrows(NullPointerException.class, () -> map.putAll(badKeyMap), "putAll with a null key must throw NPE.");
        // WHEN / THEN – null value
        assertThrows(NullPointerException.class, () -> map.putAll(badValueMap), "putAll with a null value must throw NPE.");
    }

    @Test
    void entrySetAdd_ShouldThrowUnsupportedOperationException() {
        // GIVEN
        DefaultConverterOptions options = new DefaultConverterOptions();
        Map<ConversionPair, Convert<?>> map = options.getConverterOverrides();
        ConversionPair key = dummyKey();
        Convert<Object> value = dummyValue();
        // WHEN / THEN
        assertThrows(UnsupportedOperationException.class, () -> map.entrySet().add(new AbstractMap.SimpleEntry<>(key, value)), "Adding to entrySet of ConcurrentHashMap must throw UnsupportedOperationException.");
    }

    @Test
    void keySetAdd_ShouldThrowUnsupportedOperationException() {
        // GIVEN
        DefaultConverterOptions options = new DefaultConverterOptions();
        Map<ConversionPair, Convert<?>> map = options.getConverterOverrides();
        ConversionPair key = dummyKey();
        // WHEN / THEN
        assertThrows(UnsupportedOperationException.class, () -> map.keySet().add(key), "Adding to keySet of ConcurrentHashMap must throw UnsupportedOperationException.");
    }

    @Test
    void valuesAdd_ShouldThrowUnsupportedOperationException() {
        // GIVEN
        DefaultConverterOptions options = new DefaultConverterOptions();
        Map<ConversionPair, Convert<?>> map = options.getConverterOverrides();
        Convert<Object> value = dummyValue();
        // WHEN / THEN
        assertThrows(UnsupportedOperationException.class, () -> map.values().add(value), "Adding to values collection of ConcurrentHashMap must throw UnsupportedOperationException.");
    }
}
