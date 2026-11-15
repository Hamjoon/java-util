package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_put_Test {

    private SimpleNullSafeMap<String, String> map;

    /**
     * Simple concrete implementation used for the majority of tests.
     */
    private static class SimpleNullSafeMap<K, V> extends AbstractConcurrentNullSafeMap<K, V> {

        SimpleNullSafeMap() {
            super(new ConcurrentHashMap<>());
        }
    }

    /**
     * Subclass that deliberately returns {@code null} from maskNullKey for a specific trigger key.
     */
    private static class NullKeyMaskingMap<K, V> extends AbstractConcurrentNullSafeMap<K, V> {

        NullKeyMaskingMap() {
            super(new ConcurrentHashMap<>());
        }

        @Override
        protected Object maskNullKey(K key) {
            // Simulate a buggy implementation that returns null for a particular key.
            return "nullKeyTrigger".equals(key) ? null : super.maskNullKey(key);
        }
    }

    /**
     * Subclass that deliberately returns {@code null} from maskNullValue for a specific trigger value.
     */
    private static class NullValueMaskingMap<K, V> extends AbstractConcurrentNullSafeMap<K, V> {

        NullValueMaskingMap() {
            super(new ConcurrentHashMap<>());
        }

        @Override
        protected Object maskNullValue(V value) {
            // Simulate a buggy implementation that returns null for a particular value.
            return "nullValueTrigger".equals(value) ? null : super.maskNullValue(value);
        }
    }

    /**
     * ConcurrentMap wrapper that throws {@link UnsupportedOperationException} on any put operation.
     */
    private static class UnsupportedPutMap implements ConcurrentMap<Object, Object> {

        private final ConcurrentMap<Object, Object> delegate = new ConcurrentHashMap<>();

        @Override
        public Object put(Object k, Object v) {
            throw new UnsupportedOperationException("put not supported");
        }

        @Override
        public Object get(Object k) {
            return delegate.get(k);
        }

        @Override
        public boolean containsKey(Object k) {
            return delegate.containsKey(k);
        }

        @Override
        public boolean containsValue(Object v) {
            return delegate.containsValue(v);
        }

        @Override
        public Set<Entry<Object, Object>> entrySet() {
            return delegate.entrySet();
        }

        @Override
        public Set<Object> keySet() {
            return delegate.keySet();
        }

        @Override
        public Collection<Object> values() {
            return delegate.values();
        }

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        @Override
        public void clear() {
            delegate.clear();
        }

        @Override
        public Object remove(Object k) {
            return delegate.remove(k);
        }

        @Override
        public boolean remove(Object k, Object v) {
            return delegate.remove(k, v);
        }

        @Override
        public Object putIfAbsent(Object k, Object v) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean replace(Object k, Object oldV, Object newV) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object replace(Object k, Object v) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean replace(Object k, Object oldV, Object newV) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean replace(Object k, Object v, Object newV) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean equals(Object o) {
            return delegate.equals(o);
        }

        @Override
        public int hashCode() {
            return delegate.hashCode();
        }

        @Override
        public void putAll(Map<? extends Object, ? extends Object> m) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object computeIfAbsent(Object k, Function<? super Object, ? extends Object> mappingFunction) {
            return delegate.computeIfAbsent(k, mappingFunction);
        }

        @Override
        public Object computeIfPresent(Object k, BiFunction<? super Object, ? super Object, ? extends Object> remappingFunction) {
            return delegate.computeIfPresent(k, remappingFunction);
        }

        @Override
        public Object compute(Object k, BiFunction<? super Object, ? super Object, ? extends Object> remappingFunction) {
            return delegate.compute(k, remappingFunction);
        }

        @Override
        public Object merge(Object k, Object v, BiFunction<? super Object, ? super Object, ? extends Object> remappingFunction) {
            return delegate.merge(k, v, remappingFunction);
        }

        @Override
        public Object getOrDefault(Object k, Object defaultValue) {
            return delegate.getOrDefault(k, defaultValue);
        }
    }

    /**
     * ConcurrentMap that only accepts {@link String} keys; otherwise throws {@link ClassCastException}.
     */
    private static class StringKeyOnlyMap implements ConcurrentMap<Object, Object> {

        private final ConcurrentMap<Object, Object> delegate = new ConcurrentHashMap<>();

        @Override
        public Object put(Object k, Object v) {
            if (!(k instanceof String)) {
                throw new ClassCastException("Only String keys are allowed");
            }
            return delegate.put(k, v);
        }

        // The rest delegate directly – they are not exercised in the tests.
        @Override
        public Object get(Object k) {
            return delegate.get(k);
        }

        @Override
        public boolean containsKey(Object k) {
            return delegate.containsKey(k);
        }

        @Override
        public boolean containsValue(Object v) {
            return delegate.containsValue(v);
        }

        @Override
        public Set<Entry<Object, Object>> entrySet() {
            return delegate.entrySet();
        }

        @Override
        public Set<Object> keySet() {
            return delegate.keySet();
        }

        @Override
        public Collection<Object> values() {
            return delegate.values();
        }

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        @Override
        public void clear() {
            delegate.clear();
        }

        @Override
        public Object remove(Object k) {
            return delegate.remove(k);
        }

        @Override
        public boolean remove(Object k, Object v) {
            return delegate.remove(k, v);
        }

        @Override
        public Object putIfAbsent(Object k, Object v) {
            return delegate.putIfAbsent(k, v);
        }

        @Override
        public boolean replace(Object k, Object oldV, Object newV) {
            return delegate.replace(k, oldV, newV);
        }

        @Override
        public Object replace(Object k, Object v) {
            return delegate.replace(k, v);
        }

        @Override
        public boolean replace(Object k, Object oldV, Object newV) {
            return delegate.replace(k, oldV, newV);
        }

        @Override
        public boolean replace(Object k, Object v, Object newV) {
            return delegate.replace(k, v, newV);
        }

        @Override
        public boolean equals(Object o) {
            return delegate.equals(o);
        }

        @Override
        public int hashCode() {
            return delegate.hashCode();
        }

        @Override
        public void putAll(Map<? extends Object, ? extends Object> m) {
            delegate.putAll(m);
        }

        @Override
        public Object computeIfAbsent(Object k, Function<? super Object, ? extends Object> mappingFunction) {
            return delegate.computeIfAbsent(k, mappingFunction);
        }

        @Override
        public Object computeIfPresent(Object k, BiFunction<? super Object, ? super Object, ? extends Object> remappingFunction) {
            return delegate.computeIfPresent(k, remappingFunction);
        }

        @Override
        public Object compute(Object k, BiFunction<? super Object, ? super Object, ? extends Object> remappingFunction) {
            return delegate.compute(k, remappingFunction);
        }

        @Override
        public Object merge(Object k, Object v, BiFunction<? super Object, ? super Object, ? extends Object> remappingFunction) {
            return delegate.merge(k, v, remappingFunction);
        }

        @Override
        public Object getOrDefault(Object k, Object defaultValue) {
            return delegate.getOrDefault(k, defaultValue);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh map for each test to guarantee isolation.
        map = new SimpleNullSafeMap<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to help GC and avoid accidental cross‑test contamination.
        map.clear();
    }

    /* --------------------------------------------------------------
     *  Parameterized tests for the basic "put" scenarios described in JSON.
     * -------------------------------------------------------------- */
    private static Stream<Arguments> basicPutProvider() {
        return Stream.of(// Insert non‑null key/value
        Arguments.of("alpha", "beta", null, "Insert a new entry with non‑null key and non‑null value."), // Insert non‑null key, null value
        Arguments.of("gamma", null, null, "Insert a new entry where the value is null."), // Insert null key, non‑null value
        Arguments.of(null, "delta", null, "Insert a new entry where the key is null."), // Insert null key, null value
        Arguments.of(null, null, null, "Insert a new entry where both key and value are null."));
    }

    @Test
    void putNonNullKeyAndValue_returnsPreviousValueAndStoresNewEntry() {
        // given
        String key = "alpha";
        String firstValue = "first";
        String secondValue = "second";
        // when – first insertion
        String returned1 = map.put(key, firstValue);
        // then
        assertNull(returned1, "First put should return null because there was no previous mapping");
        assertEquals(firstValue, map.get(key), "Map should contain the first value after first put");
        // when – second insertion (overwrite)
        String returned2 = map.put(key, secondValue);
        // then
        assertEquals(firstValue, returned2, "Second put should return the previous value");
        assertEquals(secondValue, map.get(key), "Map should contain the second value after overwrite");
    }

    @Test
    void putNullKeyAndNonNullValue_isHandledCorrectly() {
        // given
        String value = "valueForNullKey";
        // when
        String previous = map.put(null, value);
        // then
        assertNull(previous, "Putting a null key for the first time should return null");
        assertEquals(value, map.get(null), "Map should return the value associated with a null key");
    }

    @Test
    void putNonNullKeyAndNullValue_isHandledCorrectly() {
        // given
        String key = "keyForNullValue";
        // when
        String previous = map.put(key, null);
        // then
        assertNull(previous, "Putting a null value for the first time should return null");
        assertNull(map.get(key), "Map should return null for a key that was associated with a null value");
    }

    @Test
    void putNullKeyAndNullValue_isHandledCorrectly() {
        // when
        String previous = map.put(null, null);
        // then
        assertNull(previous, "First put with both key and value null should return null");
        assertNull(map.get(null), "Map should return null for a null key after putting null value");
    }

    @Test
    void putReturnsPreviousMappedValueWhenBothKeyAndValueAreNull() {
        // given: first insertion of null key/value
        map.put(null, null);
        // when: second insertion overwrites the previous null mapping
        String previous = map.put(null, null);
        // then
        assertNull(previous, "Even when overwriting a null mapping, the returned previous value should be null");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("basicPutProvider")
    void putBasicScenarios(String key, String value, String expectedReturn, String description) {
        // when
        String returned = map.put(key, value);
        // then
        assertEquals(expectedReturn, returned, "Return value mismatch");
        assertEquals(value, map.get(key), "Stored value mismatch for key: " + key);
    }

    /* --------------------------------------------------------------
     *  Overwrite scenarios – previous value exists (null‑masked or real)
     * -------------------------------------------------------------- */
    @Test
    void overwriteNonNullPreviousValue() {
        // Setup initial mapping
        map.put("alpha", "beta");
        // Overwrite
        String previous = map.put("alpha", "newBeta");
        assertEquals("beta", previous, "Should return the previous non‑null value");
        assertEquals("newBeta", map.get("alpha"), "Map should contain the new value after overwrite");
    }

    @Test
    void overwritePreviousNullValueMasked() {
        // Initial mapping with null value (masked internally)
        map.put("nullKey", null);
        // Overwrite with non‑null
        String previous = map.put("nullKey", "first");
        assertNull(previous, "Previous value was null (masked), so unmasked return should be null");
        assertEquals("first", map.get("nullKey"), "New value should replace the sentinel");
    }

    @Test
    void overwriteNonNullWithNullValue() {
        // Initial mapping
        map.put("k2", "v2");
        // Overwrite with null (which will be masked)
        String previous = map.put("k2", null);
        assertEquals("v2", previous, "Should return the previous non‑null value");
        assertNull(map.get("k2"), "After overwriting with null, get should return null");
    }

    @Test
    void overwriteNullKeyWithNonNullPreviousValue() {
        // Initial mapping for null key
        map.put(null, "oldDelta");
        // Overwrite null key with new value
        String previous = map.put(null, "newDelta");
        assertEquals("oldDelta", previous, "Previous value for null key should be returned");
        assertEquals("newDelta", map.get(null), "New value should be stored for null key");
    }

    @Test
    void overwriteNullKeyAndValueBothPreviouslyNull() {
        // Initial mapping of null key/value
        map.put(null, null);
        // Overwrite with same nulls
        String previous = map.put(null, null);
        assertNull(previous, "Previous value was null, so return should be null");
        assertNull(map.get(null), "Map should still return null for null key");
    }

    @Test
    void putIdempotentSameKeyAndValue() {
        map.put("idempotent", "same");
        String previous = map.put("idempotent", "same");
        assertEquals("same", previous, "Idempotent put should return the existing value");
        assertEquals("same", map.get("idempotent"), "Value must remain unchanged");
    }

    /* --------------------------------------------------------------
     *  Sentinel collision checks – ensure that the sentinel objects
     *  used for null masking never clash with legitimate data.
     * -------------------------------------------------------------- */
    @Test
    void sentinelCollisionValueNull() {
        // Use a value that could theoretically equal the sentinel (unlikely, but we verify behaviour)
        map.put("sentinelKey", null);
        assertNull(map.get("sentinelKey"), "Value retrieved should be null (masked sentinel)");
    }

    @Test
    void sentinelCollisionKeyNull() {
        map.put(null, "sentinelValue");
        assertEquals("sentinelValue", map.get(null), "Value for null key should be stored correctly");
    }

    /* --------------------------------------------------------------
     *  Exception propagation tests
     * -------------------------------------------------------------- */
    @Test
    void putThrowsNullPointerExceptionWhenMaskNullKeyReturnsNull() {
        AbstractConcurrentNullSafeMap<String, String> faultyMap = new NullKeyMaskingMap<>();
        // The key "nullKeyTrigger" will cause maskNullKey to return null → NPE from underlying map
        assertThrows(NullPointerException.class, () -> faultyMap.put("nullKeyTrigger", "anyValue"), "Expected NullPointerException when masked key is null");
    }

    @Test
    void putThrowsNullPointerExceptionWhenMaskNullValueReturnsNull() {
        AbstractConcurrentNullSafeMap<String, String> faultyMap = new NullValueMaskingMap<>();
        // The value "nullValueTrigger" will cause maskNullValue to return null → NPE from underlying map
        assertThrows(NullPointerException.class, () -> faultyMap.put("anyKey", "nullValueTrigger"), "Expected NullPointerException when masked value is null");
    }

    @Test
    void putPropagatesUnsupportedOperationExceptionFromInternalMap() {
        // Internal map that throws on put
        AbstractConcurrentNullSafeMap<String, String> mapWithUnsupported = new AbstractConcurrentNullSafeMap<>(new UnsupportedPutMap()) {
        };
        assertThrows(UnsupportedOperationException.class, () -> mapWithUnsupported.put("k", "v"), "UnsupportedOperationException should be propagated");
    }

    @Test
    void putPropagatesClassCastExceptionFromInternalMap() {
        AbstractConcurrentNullSafeMap<Object, String> mapWithStringKeyOnly = new AbstractConcurrentNullSafeMap<>(new StringKeyOnlyMap()) {
        };
        assertThrows(ClassCastException.class, () -> mapWithStringKeyOnly.put(123, "v"), "ClassCastException should be propagated when key type is invalid");
    }

    /* --------------------------------------------------------------
     *  Additional sanity checks (size consistency)
     * -------------------------------------------------------------- */
    @Test
    void sizeReflectsNumberOfDistinctKeysAfterVariousPuts() {
        map.put("a", "1");
        map.put(null, "nullKey");
        map.put("b", null);
        // overwrite same key
        map.put("a", "2");
        assertEquals(3, map.size(), "Map size should count distinct logical keys (including null)");
        assertEquals("2", map.get("a"));
        assertEquals("nullKey", map.get(null));
        assertNull(map.get("b"));
    }
}
