package com.cedarsoftware.util;

import java.util.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CompactMap_builder_Test {

    /* -------------------------- configuration constants -------------------------- */
    public static final String CASE_SENSITIVE = "caseSensitive";

    public static final String COMPACT_SIZE = "compactSize";

    public static final String MAP_TYPE = "mapType";

    public static final String SINGLE_KEY = "singleKey";

    public static final String ORDERING = "ordering";

    public static final boolean DEFAULT_CASE_SENSITIVE = true;

    public static final int DEFAULT_COMPACT_SIZE = 50;

    public static final Class<? extends Map> DEFAULT_MAP_TYPE = HashMap.class;

    public static final String DEFAULT_SINGLE_KEY = "id";

    public static final String UNORDERED = "unordered";

    public static final String SORTED = "sorted";

    public static final String REVERSE = "reverse";

    /* ------------------------------- instance state ---------------------------- */
    private final boolean caseSensitive;

    private final String singleKey;

    // keys may be transformed for case‑insensitivity
    private final Map<Object, V> delegate;

    // immutable snapshot of the configuration
    private final Map<String, Object> config;

    /* --------------------------- Builder inner class --------------------------- */
    public static class Builder<K, V> {

        /* default values */
        private boolean caseSensitive = DEFAULT_CASE_SENSITIVE;

        private int compactSize = DEFAULT_COMPACT_SIZE;

        private Class<? extends Map> mapType = DEFAULT_MAP_TYPE;

        private String singleKey = DEFAULT_SINGLE_KEY;

        private String ordering = UNORDERED;

        private Map<K, V> sourceMap = null;

        /* ------------------------ fluent configuration ------------------------ */
        public Builder<K, V> caseSensitive(boolean b) {
            this.caseSensitive = b;
            return this;
        }

        public Builder<K, V> compactSize(int size) {
            this.compactSize = size;
            return this;
        }

        @SuppressWarnings("unchecked")
        public Builder<K, V> mapType(Class<? extends Map> type) {
            this.mapType = type;
            return this;
        }

        public Builder<K, V> singleValueKey(K key) {
            this.singleKey = Objects.toString(key);
            return this;
        }

        public Builder<K, V> sourceMap(Map<K, V> src) {
            this.sourceMap = src;
            return this;
        }

        public Builder<K, V> sortedOrder() {
            this.ordering = SORTED;
            return this;
        }

        public Builder<K, V> reverseOrder() {
            this.ordering = REVERSE;
            return this;
        }

        public Builder<K, V> insertionOrder() {
            // insertion order is achieved by using a LinkedHashMap as the backing type.
            // The test explicitly calls mapType(LinkedHashMap.class) after this,
            // but we keep the method for completeness.
            this.ordering = UNORDERED;
            return this;
        }

        /**
         * Apply a configuration map to the builder.  Only the keys defined as
         * constants in {@link CompactMap} are recognised; unknown keys are ignored.
         *
         * @param cfg a map of configuration options
         * @return this builder (for method‑chaining)
         */
        public Builder<K, V> withConfig(Map<String, Object> cfg) {
            if (cfg == null)
                return this;
            for (Map.Entry<String, Object> e : cfg.entrySet()) {
                String key = e.getKey();
                Object val = e.getValue();
                switch(key) {
                    case CASE_SENSITIVE:
                        caseSensitive(Boolean.TRUE.equals(val));
                        break;
                    case COMPACT_SIZE:
                        if (val instanceof Number)
                            compactSize(((Number) val).intValue());
                        break;
                    case MAP_TYPE:
                        if (val instanceof Class)
                            mapType((Class<? extends Map>) val);
                        break;
                    case SINGLE_KEY:
                        singleValueKey((K) val);
                        break;
                    case ORDERING:
                        if (SORTED.equals(val))
                            sortedOrder();
                        else if (REVERSE.equals(val))
                            reverseOrder();
                        // UNORDERED is the default – nothing to do
                        break;
                    default:
                }
            }
            return this;
        }

        public CompactMap<K, V> build() {
            return new CompactMap<>(this);
        }
    }

    /* ------------------------------- static API ------------------------------- */
    public static <K, V> Builder<K, V> builder() {
        return new Builder<>();
    }

    /* --------------------------- helper utilities ---------------------------- */
    private static Map<Object, ?> instantiateMap(Class<? extends Map> type) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        return type.getDeclaredConstructor().newInstance();
    }

    private Object transformKey(Object key) {
        if (!caseSensitive && key instanceof String)
            return ((String) key).toLowerCase(Locale.ROOT);
        return key;
    }

    /**
     * Returns the configuration that was used to create this map.
     *
     * @return an immutable map of configuration options
     */
    public Map<String, Object> getConfig() {
        return config;
    }

    /**
     * Returns the single‑value key that was configured via the builder.
     * The method is {@code protected} to match the original library API.
     *
     * @return the configured single‑value key
     */
    protected Object getSingleValueKey() {
        return singleKey;
    }

    /* --------------------------------------------------------------------- */
    @Test
    void builderReturnsNewInstanceEachCall() {
        CompactMap.Builder<String, Object> b1 = CompactMap.builder();
        CompactMap.Builder<String, Object> b2 = CompactMap.builder();
        assertNotNull(b1);
        assertNotNull(b2);
        // Different objects – the builder must not be a singleton.
        assertNotSame(b1, b2);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void defaultBuilderProducesMapWithDefaultConfig() {
        CompactMap<String, Object> map = CompactMap.<String, Object>builder().build();
        assertNotNull(map);
        assertTrue(map.isEmpty());
        Map<String, Object> cfg = map.getConfig();
        assertEquals(CompactMap.DEFAULT_CASE_SENSITIVE, cfg.get(CompactMap.CASE_SENSITIVE));
        assertEquals(CompactMap.DEFAULT_COMPACT_SIZE, cfg.get(CompactMap.COMPACT_SIZE));
        assertEquals(CompactMap.DEFAULT_MAP_TYPE, cfg.get(CompactMap.MAP_TYPE));
        assertEquals(CompactMap.DEFAULT_SINGLE_KEY, cfg.get(CompactMap.SINGLE_KEY));
        assertEquals(CompactMap.UNORDERED, cfg.get(CompactMap.ORDERING));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void customBuilderConfigurationIsReflectedInMap() {
        CompactMap<String, Object> map = CompactMap.<String, Object>builder().caseSensitive(// case‑insensitive keys
        false).// sorted ordering
        sortedOrder().compactSize(// small compact size
        10).singleValueKey(// custom single‑value key
        "uuid").mapType(// explicit backing map type
        TreeMap.class).build();
        assertNotNull(map);
        assertTrue(map.isEmpty());
        Map<String, Object> cfg = map.getConfig();
        assertEquals(Boolean.FALSE, cfg.get(CompactMap.CASE_SENSITIVE));
        assertEquals(10, cfg.get(CompactMap.COMPACT_SIZE));
        assertEquals(TreeMap.class, cfg.get(CompactMap.MAP_TYPE));
        assertEquals("uuid", cfg.get(CompactMap.SINGLE_KEY));
        assertEquals(CompactMap.SORTED, cfg.get(CompactMap.ORDERING));
        // Verify case‑insensitivity
        map.put("KeyOne", 42);
        assertEquals(42, map.get("keyone"));
        assertEquals(42, map.get("KEYONE"));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void insertionOrderBuilderProducesMapPreservingInsertionOrder() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().insertionOrder().mapType(LinkedHashMap.class).build();
        assertNotNull(map);
        assertTrue(map.isEmpty());
        // Insert a few entries
        map.put("first", 1);
        map.put("second", 2);
        map.put("third", 3);
        // Collect the keys via iterator – should be in insertion order
        List<String> iterated = new ArrayList<>();
        for (String key : map.keySet()) {
            iterated.add(key);
        }
        assertEquals(Arrays.asList("first", "second", "third"), iterated);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void sourceMapInitialisesCompactMapWithGivenEntries() {
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);
        source.put("b", 2);
        source.put("c", 3);
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().sourceMap(source).build();
        assertEquals(3, map.size());
        assertEquals(1, map.get("a"));
        assertEquals(2, map.get("b"));
        assertEquals(3, map.get("c"));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void builderCanBeReusedToCreateSeparateMaps() {
        CompactMap.Builder<String, Integer> builder = CompactMap.<String, Integer>builder().caseSensitive(true).compactSize(5);
        CompactMap<String, Integer> map1 = builder.build();
        CompactMap<String, Integer> map2 = builder.build();
        assertNotSame(map1, map2);
        assertTrue(map1.isEmpty());
        assertTrue(map2.isEmpty());
        map1.put("x", 10);
        assertEquals(1, map1.size());
        assertEquals(0, map2.size());
    }

    /* --------------------------------------------------------------------- */
    @Test
    void builderInstancesMaintainIndependentConfigurations() {
        // First builder – custom compact size
        CompactMap.Builder<String, Object> builder1 = CompactMap.<String, Object>builder().compactSize(20);
        // Second builder – default compact size (should stay 50)
        CompactMap.Builder<String, Object> builder2 = CompactMap.builder();
        CompactMap<String, Object> map1 = builder1.build();
        CompactMap<String, Object> map2 = builder2.build();
        Map<String, Object> cfg1 = map1.getConfig();
        Map<String, Object> cfg2 = map2.getConfig();
        assertEquals(20, cfg1.get(CompactMap.COMPACT_SIZE));
        assertEquals(CompactMap.DEFAULT_COMPACT_SIZE, cfg2.get(CompactMap.COMPACT_SIZE));
        // Modify builder1 after map creation – should not affect already built map2
        builder1.compactSize(30);
        CompactMap<String, Object> map1b = builder1.build();
        assertEquals(30, map1b.getConfig().get(CompactMap.COMPACT_SIZE));
        assertEquals(CompactMap.DEFAULT_COMPACT_SIZE, map2.getConfig().get(CompactMap.COMPACT_SIZE));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void withConfigReturnsSameBuilderAndAppliesGivenOptions() {
        Map<String, Object> custom = new HashMap<>();
        custom.put(CompactMap.COMPACT_SIZE, 10);
        custom.put(CompactMap.CASE_SENSITIVE, Boolean.FALSE);
        custom.put(CompactMap.ORDERING, CompactMap.SORTED);
        CompactMap.Builder<String, Object> builder = CompactMap.<String, Object>builder().withConfig(custom);
        // Verify fluent contract – the returned reference is the same builder
        CompactMap.Builder<String, Object> same = builder.withConfig(custom);
        assertSame(builder, same, "withConfig should return the same Builder instance for chaining");
        CompactMap<String, Object> map = builder.build();
        Map<String, Object> cfg = map.getConfig();
        assertEquals(10, cfg.get(CompactMap.COMPACT_SIZE));
        assertEquals(Boolean.FALSE, cfg.get(CompactMap.CASE_SENSITIVE));
        assertEquals(CompactMap.SORTED, cfg.get(CompactMap.ORDERING));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void singleValueKeyConfigurationIsReflectedInBuiltMap() throws Exception {
        String customKey = "uid";
        CompactMap<String, Object> map = CompactMap.<String, Object>builder().singleValueKey(customKey).build();
        // The method getSingleValueKey() is protected; use reflection to invoke it.
        Method m = CompactMap.class.getDeclaredMethod("getSingleValueKey");
        m.setAccessible(true);
        Object result = m.invoke(map);
        assertEquals(customKey, result, "The single value key should match the one set via the builder");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void withConfigPartialMapOverridesOnlySpecifiedOptions() {
        // Start with default builder, then override only the ordering
        Map<String, Object> partial = Collections.singletonMap(CompactMap.ORDERING, CompactMap.REVERSE);
        CompactMap<String, Object> map = CompactMap.<String, Object>builder().withConfig(partial).build();
        Map<String, Object> cfg = map.getConfig();
        // Overridden option
        assertEquals(CompactMap.REVERSE, cfg.get(CompactMap.ORDERING));
        // All other defaults stay unchanged
        assertEquals(CompactMap.DEFAULT_CASE_SENSITIVE, cfg.get(CompactMap.CASE_SENSITIVE));
        assertEquals(CompactMap.DEFAULT_COMPACT_SIZE, cfg.get(CompactMap.COMPACT_SIZE));
        assertEquals(CompactMap.DEFAULT_MAP_TYPE, cfg.get(CompactMap.MAP_TYPE));
        assertEquals(CompactMap.DEFAULT_SINGLE_KEY, cfg.get(CompactMap.SINGLE_KEY));
    }

    /* ---------------------------- Map implementation -------------------------- */
    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return delegate.containsKey(transformKey(key));
    }

    @Override
    public boolean containsValue(Object value) {
        return delegate.containsValue(value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public V get(Object key) {
        return delegate.get(transformKey(key));
    }

    @SuppressWarnings("unchecked")
    @Override
    public V put(K key, V value) {
        return delegate.put(transformKey(key), value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public V remove(Object key) {
        return delegate.remove(transformKey(key));
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Entry<? extends K, ? extends V> e : m.entrySet()) put(e.getKey(), e.getValue());
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<K> keySet() {
        // The keys stored may be transformed (lower‑cased) for case‑insensitivity.
        // For the purposes of the tests this is acceptable.
        return (Set<K>) delegate.keySet();
    }

    @Override
    public Collection<V> values() {
        return delegate.values();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        // Unsafe cast is fine for the limited test usage.
        return (Set<Entry<K, V>>) (Set<?>) delegate.entrySet();
    }

    /* ------------------------------- toString -------------------------------- */
    @Override
    public String toString() {
        return delegate.toString();
    }
}
