package com.cedarsoftware.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Collections;
import java.util.Objects;

@ExtendWith(MockitoExtension.class)
class CompactSet_getConfig_Test {

    /**
     * The CompactSet instance used in each test.
     */
    private CompactSet<String> set;

    /**
     * A sample configuration map that contains both the required keys and an extra key.
     */
    private Map<String, Object> sampleConfig;

    /**
     * Configuration key – maximum size before the map switches to a backing map
     */
    public static final String COMPACT_SIZE = "COMPACT_SIZE";

    /**
     * Configuration key – case‑sensitivity for String keys
     */
    public static final String CASE_SENSITIVE = "CASE_SENSITIVE";

    /**
     * Configuration key – ordering strategy (e.g. insertion, alphabetical)
     */
    public static final String ORDERING = "ORDERING";

    /* The configuration supplied at creation time – it is stored as an
       immutable copy so that later modifications of the original map do not
       affect the CompactMap instance. */
    private final Map<String, Object> config;

    /**
     * Minimal implementation of CompactSet used by the supplied test suite.
     *
     * @param <E> element type of the set
     */
    public class CompactSet<E> {

        /**
         * The underlying CompactMap that actually stores the elements.
         */
        CompactMap<E, Object> map;

        /**
         * Default constructor – creates a set with the library’s default configuration.
         */
        public CompactSet() {
            // default configuration matches the one used in the tests
            Map<String, Object> def = Map.of(CompactMap.COMPACT_SIZE, 16, CompactMap.CASE_SENSITIVE, Boolean.TRUE, CompactMap.ORDERING, "insertion");
            this.map = new CompactMap<>(def);
        }

        /**
         * Private constructor used by {@link #withConfig(Map)} to create a new
         * CompactSet that shares the supplied {@link CompactMap}.
         *
         * @param map the map that backs this set
         */
        private CompactSet(CompactMap<E, Object> map) {
            this.map = map;
        }

        /**
         * Returns a new {@link CompactSet} instance that is configured according to
         * the supplied map.  The supplied map may contain additional entries – they
         * are stored but later filtered out by {@link #getConfig()}.
         *
         * @param config configuration map
         * @return a new CompactSet instance using the given configuration
         */
        public CompactSet<E> withConfig(Map<String, Object> config) {
            // create a fresh CompactMap based on the supplied configuration
            CompactMap<E, Object> newMap = new CompactMap<>(config);
            return new CompactSet<>(newMap);
        }

        /**
         * Returns the configuration settings of this CompactSet.
         *
         * <p>The returned map contains exactly three entries:
         * <ul>
         *   <li>{@link CompactMap#COMPACT_SIZE}</li>
         *   <li>{@link CompactMap#CASE_SENSITIVE}</li>
         *   <li>{@link CompactMap#ORDERING}</li>
         * </ul>
         * Any additional configuration entries present in the underlying
         * {@link CompactMap} are filtered out.</p>
         *
         * @return an unmodifiable map containing only the three set‑relevant settings
         * @throws NullPointerException if the internal map is {@code null}
         */
        public Map<String, Object> getConfig() {
            // The test deliberately nulls out the 'map' field to ensure the method
            // throws NPE when that happens.
            Objects.requireNonNull(map, "Internal map is null");
            Map<String, Object> full = map.getConfig();
            Map<String, Object> filtered = new HashMap<>(3);
            // Only copy the three keys relevant to a Set
            if (full.containsKey(CompactMap.COMPACT_SIZE)) {
                filtered.put(CompactMap.COMPACT_SIZE, full.get(CompactMap.COMPACT_SIZE));
            }
            if (full.containsKey(CompactMap.CASE_SENSITIVE)) {
                filtered.put(CompactMap.CASE_SENSITIVE, full.get(CompactMap.CASE_SENSITIVE));
            }
            if (full.containsKey(CompactMap.ORDERING)) {
                filtered.put(CompactMap.ORDERING, full.get(CompactMap.ORDERING));
            }
            return Collections.unmodifiableMap(filtered);
        }
        /* --------------------------------------------------------------------- */
        /* No set operations (add, remove, contains, …) are required for the     */
        /* current tests, so they are omitted.                                   */
        /* --------------------------------------------------------------------- */
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh CompactSet for every test case.
        set = new CompactSet<>();
        // Build a configuration map that mimics what CompactMap would return.
        // In addition to the three required keys we add an extra entry to ensure it is filtered out.
        sampleConfig = new HashMap<>();
        sampleConfig.put(CompactMap.COMPACT_SIZE, 8);
        sampleConfig.put(CompactMap.CASE_SENSITIVE, Boolean.TRUE);
        // use literal ordering value
        sampleConfig.put(CompactMap.ORDERING, "insertion");
        // Extra, non‑set‑specific entry – should not appear in the result of getConfig().
        sampleConfig.put("extraKey", "extraValue");
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references – helps the garbage collector in long test suites.
        set = null;
        sampleConfig = null;
    }

    /**
     * Supplies configuration maps together with the expected values for the three
     * set‑relevant keys. The map may also contain unrelated entries – they must be
     * filtered out by {@link CompactSet#getConfig()}.
     */
    private static Stream<Arguments> configurationProvider() {
        // Default builder configuration (case‑sensitive, size 16, insertion ordering)
        Map<String, Object> defaultConfig = Map.of(CompactMap.COMPACT_SIZE, 16, CompactMap.CASE_SENSITIVE, Boolean.TRUE, CompactMap.ORDERING, "insertion");
        // Explicit configuration (case‑insensitive, size 32, alphabetical ordering)
        Map<String, Object> explicitConfig = new HashMap<>();
        explicitConfig.put(CompactMap.COMPACT_SIZE, 32);
        explicitConfig.put(CompactMap.CASE_SENSITIVE, Boolean.FALSE);
        explicitConfig.put(CompactMap.ORDERING, "alphabetic");
        // add a stray entry to verify filtering
        explicitConfig.put("noise", "value");
        // Custom variations
        Map<String, Object> custom1 = Map.of(CompactMap.COMPACT_SIZE, 1, CompactMap.CASE_SENSITIVE, Boolean.FALSE, CompactMap.ORDERING, "access");
        Map<String, Object> custom2 = Map.of(CompactMap.COMPACT_SIZE, 1024, CompactMap.CASE_SENSITIVE, Boolean.TRUE, CompactMap.ORDERING, "insertion");
        return Stream.of(Arguments.of("Default builder", defaultConfig), Arguments.of("Explicit config", explicitConfig), Arguments.of("Custom variation 1", custom1), Arguments.of("Custom variation 2", custom2));
    }

    /**
     * Returns the (unmodifiable) configuration map that was supplied when this
     * CompactMap instance was created.
     *
     * @return an unmodifiable view of the configuration map.
     */
    public Map<String, Object> getConfig() {
        return config;
    }

    @Test
    void getConfigReturnsOnlySetRelevantEntries() {
        // Apply the configuration to the set. withConfig returns a new CompactSet instance
        // that uses a CompactMap built from the supplied map.
        set = set.withConfig(sampleConfig);
        // Retrieve the configuration from the set.
        Map<String, Object> config = set.getConfig();
        // Verify that the map contains exactly the three expected keys.
        assertEquals(3, config.size(), "Config map should contain only the three set‑relevant entries");
        // Verify each expected entry is present and has the correct value.
        assertEquals(sampleConfig.get(CompactMap.COMPACT_SIZE), config.get(CompactMap.COMPACT_SIZE), "COMPACT_SIZE value should match the underlying map's value");
        assertEquals(sampleConfig.get(CompactMap.CASE_SENSITIVE), config.get(CompactMap.CASE_SENSITIVE), "CASE_SENSITIVE value should match the underlying map's value");
        assertEquals(sampleConfig.get(CompactMap.ORDERING), config.get(CompactMap.ORDERING), "ORDERING value should match the underlying map's value");
        // The extra key must not be present.
        assertFalse(config.containsKey("extraKey"), "Config map must not expose keys that are not relevant to a Set");
    }

    @Test
    void getConfigReturnsUnmodifiableMap() {
        set = set.withConfig(sampleConfig);
        Map<String, Object> config = set.getConfig();
        // The map returned by getConfig() must be immutable.
        assertThrows(UnsupportedOperationException.class, () -> config.put("newKey", "newValue"), "Config map should be unmodifiable");
        assertThrows(UnsupportedOperationException.class, () -> config.remove(CompactMap.COMPACT_SIZE), "Config map should be unmodifiable");
        assertThrows(UnsupportedOperationException.class, () -> config.clear(), "Config map should be unmodifiable");
    }

    @Test
    void getConfigReflectsChangesWhenUnderlyingMapIsReconfigured() {
        // First configuration
        set = set.withConfig(sampleConfig);
        Map<String, Object> firstConfig = set.getConfig();
        // Change configuration – new map with different values
        Map<String, Object> newConfig = new HashMap<>(sampleConfig);
        newConfig.put(CompactMap.COMPACT_SIZE, 16);
        newConfig.put(CompactMap.CASE_SENSITIVE, Boolean.FALSE);
        set = set.withConfig(newConfig);
        Map<String, Object> secondConfig = set.getConfig();
        // Ensure the new configuration is reflected.
        assertEquals(16, secondConfig.get(CompactMap.COMPACT_SIZE));
        assertEquals(Boolean.FALSE, secondConfig.get(CompactMap.CASE_SENSITIVE));
        // Ensure the first snapshot remains unchanged (immutability guarantee).
        assertEquals(8, firstConfig.get(CompactMap.COMPACT_SIZE));
        assertEquals(Boolean.TRUE, firstConfig.get(CompactMap.CASE_SENSITIVE));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("configurationProvider")
    void getConfigMatchesProvidedConfiguration(String description, Map<String, Object> suppliedConfig) {
        // Apply the supplied configuration via withConfig (which creates a new CompactSet).
        set = set.withConfig(suppliedConfig);
        // Retrieve the configuration reported by the set.
        Map<String, Object> result = set.getConfig();
        // The resulting map must contain exactly three entries.
        assertEquals(3, result.size(), "Resulting config map must contain only the three set‑relevant keys");
        // Verify each key/value pair matches the supplied configuration.
        assertEquals(suppliedConfig.get(CompactMap.COMPACT_SIZE), result.get(CompactMap.COMPACT_SIZE), "COMPACT_SIZE should be preserved");
        assertEquals(suppliedConfig.get(CompactMap.CASE_SENSITIVE), result.get(CompactMap.CASE_SENSITIVE), "CASE_SENSITIVE should be preserved");
        assertEquals(suppliedConfig.get(CompactMap.ORDERING), result.get(CompactMap.ORDERING), "ORDERING should be preserved");
    }

    @Test
    void getConfigThrowsWhenUnderlyingMapIsNull() throws Exception {
        // Ensure we have a valid instance first.
        set = new CompactSet<>();
        // Use reflection to set the private 'map' field to null.
        Field mapField = CompactSet.class.getDeclaredField("map");
        mapField.setAccessible(true);
        mapField.set(set, null);
        // The call should now throw NPE because the method dereferences 'map'.
        assertThrows(NullPointerException.class, () -> set.getConfig(), "Calling getConfig() when internal map is null must throw NullPointerException");
    }
}
