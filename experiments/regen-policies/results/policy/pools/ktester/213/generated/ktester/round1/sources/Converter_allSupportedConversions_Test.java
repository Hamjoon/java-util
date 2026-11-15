package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.util.*;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;

@ExtendWith(MockitoExtension.class)
class Converter_allSupportedConversions_Test {

    /**
     * Hold original static maps so they can be restored after each test.
     */
    private static Map<?, ?> originalConversionDb;

    private static Map<?, ?> originalUserDb;

    /**
     * Reflection handles for the private static fields.
     */
    private static Field conversionDbField;

    private static Field userDbField;

    /**
     * Simple target type used for a custom conversion in the tests.
     */
    private static final class Dummy {

        final String value;

        Dummy(String v) {
            this.value = v;
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof Dummy) && Objects.equals(value, ((Dummy) o).value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }

    /**
     * Very small utility to instantiate a class without invoking its constructor.
     * Used only for creating a malformed ConversionPair.
     */
    private static final class UnsafeAllocator {

        private static final sun.misc.Unsafe UNSAFE;

        static {
            try {
                Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
                f.setAccessible(true);
                UNSAFE = (sun.misc.Unsafe) f.get(null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        static Object instantiate(Class<?> clazz) {
            return UNSAFE.allocateInstance(clazz);
        }
    }

    @BeforeAll
    static void initReflection() throws Exception {
        conversionDbField = Converter.class.getDeclaredField("CONVERSION_DB");
        conversionDbField.setAccessible(true);
        userDbField = Converter.class.getDeclaredField("USER_DB");
        userDbField.setAccessible(true);
        // keep a copy of the original maps (they are mutable, so we clone them)
        originalConversionDb = new HashMap<>((Map<?, ?>) conversionDbField.get(null));
        originalUserDb = new ConcurrentHashMap<>((Map<?, ?>) userDbField.get(null));
    }

    @BeforeEach
    void resetStaticState() throws Exception {
        // replace both DBs with fresh, empty instances before each test
        conversionDbField.set(null, new HashMap<>(860, 0.8F));
        userDbField.set(null, new ConcurrentHashMap<>());
    }

    @AfterEach
    void restoreStaticState() throws Exception {
        // restore the original content (helps when the library ships with many built‑in conversions)
        conversionDbField.set(null, new HashMap<>(originalConversionDb));
        userDbField.set(null, new ConcurrentHashMap<>(originalUserDb));
    }

    /**
     * Convenience overload that works with the static fields directly.
     */
    private static void injectIntoConversionDb(Map<?, ?> staticField, Class<?> source, Class<?> target, Convert<?> convert) throws Exception {
        // Obtain the inner class ConversionPair
        Class<?> pairClass = Class.forName("com.cedarsoftware.util.convert.Converter$ConversionPair");
        // The constructor is (Class, Class)
        Object pair = pairClass.getDeclaredConstructor(Class.class, Class.class).newInstance(source, target);
        ((Map<Object, Object>) targetMap).put(pair, convert);
    }

    @Test
    void allSupportedConversions_containsBuiltInAndCustomConversion_andIsSorted() {
        // --------------------------------------------------------------------
        // GIVEN – a custom conversion from String to Dummy
        // --------------------------------------------------------------------
        Converter.addConversion(String.class, Dummy.class, (from, conv) -> new Dummy((String) from));
        // --------------------------------------------------------------------
        // WHEN – the map of all supported conversions is obtained
        // --------------------------------------------------------------------
        Map<Class<?>, Set<Class<?>>> all = Converter.allSupportedConversions();
        // --------------------------------------------------------------------
        // THEN – the map is not null and contains at least one entry
        // --------------------------------------------------------------------
        assertNotNull(all, "The returned map must not be null");
        assertFalse(all.isEmpty(), "The map should contain the built‑in conversions");
        // --------------------------------------------------------------------
        // AND – the custom conversion we just registered is present
        // --------------------------------------------------------------------
        assertTrue(all.containsKey(String.class), "The source type String should be present in the map");
        Set<Class<?>> targetsForString = all.get(String.class);
        assertNotNull(targetsForString, "The target set for String must not be null");
        assertTrue(targetsForString.contains(Dummy.class), "Custom target Dummy should be listed for source String");
        // --------------------------------------------------------------------
        // AND – the keys are sorted alphabetically by class name
        // --------------------------------------------------------------------
        List<String> keyNames = all.keySet().stream().map(Class::getName).collect(Collectors.toList());
        List<String> sorted = new ArrayList<>(keyNames);
        Collections.sort(sorted);
        assertEquals(sorted, keyNames, "The keys of the map must be sorted by the fully‑qualified class name");
    }

    @Test
    void basic_emptyDatabases_returnsEmptyMap() {
        // GIVEN – both internal and user DBs are empty (handled by @BeforeEach)
        // WHEN
        Map<Class<?>, Set<Class<?>>> all = Converter.allSupportedConversions();
        // THEN
        assertNotNull(all, "Result must never be null");
        assertTrue(all.isEmpty(), "When no conversions are registered the map should be empty");
    }

    @Test
    void basic_singleUserConversion_isPresent() {
        // GIVEN – register a single user conversion
        Converter.addConversion(String.class, Integer.class, (from, conv) -> Integer.valueOf((String) from));
        // WHEN
        Map<Class<?>, Set<Class<?>>> all = Converter.allSupportedConversions();
        // THEN
        assertTrue(all.containsKey(String.class), "Source String should be present");
        Set<Class<?>> targets = all.get(String.class);
        assertNotNull(targets);
        assertEquals(Set.of(Integer.class), targets, "Only Integer should be listed as target");
    }

    @Test
    void basic_multipleTargetsForSameSource_areAggregatedWithoutDuplication() {
        // GIVEN – three different target types for the same source
        Converter.addConversion(String.class, Integer.class, (f, c) -> Integer.valueOf((String) f));
        Converter.addConversion(String.class, Double.class, (f, c) -> Double.valueOf((String) f));
        Converter.addConversion(String.class, BigDecimal.class, (f, c) -> new BigDecimal((String) f));
        // WHEN
        Map<Class<?>, Set<Class<?>>> all = Converter.allSupportedConversions();
        // THEN
        Set<Class<?>> expected = Set.of(Integer.class, Double.class, BigDecimal.class);
        assertEquals(expected, all.get(String.class), "All three target classes must be present for source String");
    }

    @Test
    void basic_twoSourceTypes_areSortedAlphabetically() {
        // GIVEN – conversions for two different source types
        Converter.addConversion(Integer.class, Long.class, (f, c) -> ((Integer) f).longValue());
        Converter.addConversion(String.class, Integer.class, (f, c) -> Integer.valueOf((String) f));
        // WHEN
        Map<Class<?>, Set<Class<?>>> all = Converter.allSupportedConversions();
        // THEN – verify ordering of keys (TreeMap with name‑based comparator)
        List<String> keyNames = all.keySet().stream().map(Class::getName).collect(Collectors.toList());
        List<String> expectedOrder = new ArrayList<>(keyNames);
        Collections.sort(expectedOrder);
        assertEquals(expectedOrder, keyNames, "Keys must be sorted alphabetically by fully‑qualified class name");
        // also verify both entries exist
        assertTrue(all.containsKey(Integer.class));
        assertTrue(all.containsKey(String.class));
    }

    @Test
    void basic_duplicateConversion_notDuplicatedInResultSet() throws Exception {
        // GIVEN – inject a conversion directly into the internal DB (simulating a built‑in conversion)
        injectIntoConversionDb((Map<?, ?>) conversionDbField.get(null), String.class, Integer.class, (from, conv) -> Integer.valueOf((String) from));
        // THEN add the same conversion via the public API (which overwrites but should not duplicate)
        Converter.addConversion(String.class, Integer.class, (from, conv) -> Integer.valueOf((String) from));
        // WHEN
        Map<Class<?>, Set<Class<?>>> all = Converter.allSupportedConversions();
        // THEN – only a single target class should be present
        Set<Class<?>> targets = all.get(String.class);
        assertNotNull(targets);
        assertEquals(1, targets.size(), "Duplicate registrations must not create duplicate set entries");
        assertTrue(targets.contains(Integer.class));
    }

    @Test
    void basic_returnedMapIsIndependentInstance() {
        // GIVEN – a conversion is registered
        Converter.addConversion(String.class, Integer.class, (from, conv) -> Integer.valueOf((String) from));
        // WHEN – obtain the map and mutate the returned instance
        Map<Class<?>, Set<Class<?>>> first = Converter.allSupportedConversions();
        // mutate
        first.put(Dummy.class, Set.of(Integer.class));
        // AND – call the method again
        Map<Class<?>, Set<Class<?>>> second = Converter.allSupportedConversions();
        // THEN – the internal state must not contain the artificial entry
        assertFalse(second.containsKey(Dummy.class), "Modifying the returned map must not affect subsequent calls");
    }

    @Test
    void internal_singleInjection_reflectedInResult() throws Exception {
        // GIVEN – inject directly into the private CONVERSION_DB
        injectIntoConversionDb((Map<?, ?>) conversionDbField.get(null), String.class, Integer.class, (from, conv) -> Integer.valueOf((String) from));
        // WHEN
        Map<Class<?>, Set<Class<?>>> all = Converter.allSupportedConversions();
        // THEN
        assertTrue(all.containsKey(String.class));
        assertTrue(all.get(String.class).contains(Integer.class));
    }

    @Test
    void internal_multipleDistinctInjections_areSortedAndPresent() throws Exception {
        // GIVEN – two distinct internal conversions
        injectIntoConversionDb((Map<?, ?>) conversionDbField.get(null), String.class, Integer.class, (f, c) -> Integer.valueOf((String) f));
        injectIntoConversionDb((Map<?, ?>) conversionDbField.get(null), LocalDate.class, LocalDateTime.class, (f, c) -> ((LocalDate) f).atStartOfDay());
        // WHEN
        Map<Class<?>, Set<Class<?>>> all = Converter.allSupportedConversions();
        // THEN – verify both keys exist and are ordered
        List<String> keyNames = all.keySet().stream().map(Class::getName).collect(Collectors.toList());
        List<String> expected = new ArrayList<>(keyNames);
        Collections.sort(expected);
        assertEquals(expected, keyNames, "Keys must be alphabetically ordered");
        assertEquals(Set.of(Integer.class), all.get(String.class));
        assertEquals(Set.of(LocalDateTime.class), all.get(LocalDate.class));
    }

    @Test
    void exception_nullConversionDb_throwsNPE() throws Exception {
        // GIVEN – force CONVERSION_DB to null
        conversionDbField.set(null, null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, Converter::allSupportedConversions, "Null CONVERSION_DB should cause NPE");
    }

    @Test
    void exception_nullUserDb_throwsNPE() throws Exception {
        // GIVEN – force USER_DB to null
        userDbField.set(null, null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, Converter::allSupportedConversions, "Null USER_DB should cause NPE");
    }

    @Test
    void exception_nullSourceInConversionPair_inConversionDb_throwsNPE() throws Exception {
        // GIVEN – a ConversionPair with a null source class inside CONVERSION_DB
        injectInvalidPairIntoDb((Map<?, ?>) conversionDbField.get(null), null, Integer.class, (f, c) -> 0);
        // WHEN / THEN
        assertThrows(NullPointerException.class, Converter::allSupportedConversions, "Null source class in CONVERSION_DB should cause NPE during TreeMap construction");
    }

    @Test
    void exception_nullSourceInConversionPair_inUserDb_throwsNPE() throws Exception {
        // GIVEN – a ConversionPair with a null source class inside USER_DB
        injectInvalidPairIntoDb((Map<?, ?>) userDbField.get(null), null, Integer.class, (f, c) -> 0);
        // WHEN / THEN
        assertThrows(NullPointerException.class, Converter::allSupportedConversions, "Null source class in USER_DB should cause NPE during TreeMap construction");
    }

    @Test
    void exception_concurrentModificationDuringIteration_throwsCME() throws Exception {
        // GIVEN – a map that will be modified concurrently while allSupportedConversions iterates it
        // We replace the internal CONVERSION_DB with a custom map that spawns a modifier thread.
        Map<Object, Object> concurrentMap = new HashMap<>();
        conversionDbField.set(null, concurrentMap);
        // Populate with an initial entry to ensure the iterator has something to walk over
        injectIntoConversionDb(concurrentMap, String.class, Integer.class, (f, c) -> Integer.valueOf((String) f));
        // Start a background thread that continuously adds entries
        AtomicBoolean stop = new AtomicBoolean(false);
        Thread modifier = new Thread(() -> {
            int i = 0;
            while (!stop.get()) {
                try {
                    injectIntoConversionDb(concurrentMap, Dummy.class, Dummy.class, (f, c) -> new Dummy("v" + i++));
                } catch (Exception ignored) {
                }
            }
        });
        modifier.start();
        // WHEN – invoke the method (it may or may not see the concurrent change)
        try {
            assertThrows(ConcurrentModificationException.class, Converter::allSupportedConversions, "Concurrent modification of CONVERSION_DB should result in CME");
        } finally {
            // clean up the modifier thread
            stop.set(true);
            modifier.join();
        }
    }

    /**
     * Injects a conversion entry directly into the given conversion map (internal or user).
     *
     * @param targetMap the static map field (either CONVERSION_DB or USER_DB)
     * @param source    source class
     * @param target    target class
     * @param convert   conversion lambda
     */
    @SuppressWarnings("unchecked")
    private static void injectIntoConversionDb(Object targetMap, Class<?> source, Class<?> target, Convert<?> convert) throws Exception {
        // Obtain the inner class ConversionPair
        Class<?> pairClass = Class.forName("com.cedarsoftware.util.convert.Converter$ConversionPair");
        // The constructor is (Class, Class)
        Object pair = pairClass.getDeclaredConstructor(Class.class, Class.class).newInstance(source, target);
        ((Map<Object, Object>) targetMap).put(pair, convert);
    }

    /**
     * Inserts an invalid ConversionPair (e.g., with a null source) into the specified map.
     */
    @SuppressWarnings("unchecked")
    private static void injectInvalidPairIntoDb(Object targetMap, Class<?> source, Class<?> target, Convert<?> convert) throws Exception {
        Class<?> pairClass = Class.forName("com.cedarsoftware.util.convert.Converter$ConversionPair");
        // Constructor may not allow null, but we bypass it via reflection
        Object pair = UnsafeAllocator.instantiate(pairClass);
        // Directly set the private fields 'source' and 'target'
        Field srcField = pairClass.getDeclaredField("source");
        srcField.setAccessible(true);
        srcField.set(pair, source);
        Field tgtField = pairClass.getDeclaredField("target");
        tgtField.setAccessible(true);
        tgtField.set(pair, target);
        ((Map<Object, Object>) targetMap).put(pair, convert);
    }
}
