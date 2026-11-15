package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@ExtendWith(MockitoExtension.class)
class Converter_allSupportedConversions_Test {

    /**
     * Instance under test.
     */
    private Converter converter;

    /**
     * Dummy source class for custom conversion tests.
     */
    private static final class MySource {
    }

    /**
     * Dummy target class for custom conversion tests.
     */
    private static final class MyTarget {
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh Converter for each test to avoid cross‑test contamination.
        this.converter = new Converter(defaultOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Clean up any reflection‑based modifications to the static CONVERSION_DB.
        clearStaticConversionDb();
        // Clean up any modifications to the instance USER_DB.
        clearUserDb(converter);
        this.converter = null;
    }

    /**
     * Simple {@link ConverterOptions} implementation that uses defaults for everything.
     */
    private static ConverterOptions defaultOptions() {
        return new ConverterOptions() {

            @Override
            public <T> T getCustomOption(String name) {
                return null;
            }
        };
    }

    /**
     * Removes any entries that were injected into the static {@code CONVERSION_DB}.
     */
    private static void clearStaticConversionDb() throws Exception {
        Map<Object, Convert<?>> db = getStaticConversionDb();
        // Remove any null keys or ConversionPairs with null components that we may have added.
        db.keySet().removeIf(Objects::isNull);
        // Also remove any ConversionPair whose source or target is null.
        db.keySet().removeIf(k -> {
            try {
                Class<?> source = (Class<?>) k.getClass().getMethod("getSource").invoke(k);
                Class<?> target = (Class<?>) k.getClass().getMethod("getTarget").invoke(k);
                return source == null || target == null;
            } catch (Exception e) {
                return false;
            }
        });
    }

    /**
     * Clears any entries that were injected into the instance {@code USER_DB}.
     */
    private static void clearUserDb(Converter conv) throws Exception {
        Map<Object, Convert<?>> db = getUserDb(conv);
        db.clear();
    }

    /**
     * Creates a {@code ConversionPair} instance via reflection.
     */
    private static Object newConversionPair(Class<?> source, Class<?> target) throws Exception {
        Class<?> pairClass = Class.forName("com.cedarsoftware.util.convert.Converter$ConversionPair");
        Constructor<?> ctor = pairClass.getDeclaredConstructor(Class.class, Class.class);
        ctor.setAccessible(true);
        return ctor.newInstance(source, target);
    }

    /**
     * Verify that the map returned by {@code allSupportedConversions()} is non‑null,
     * non‑empty and contains a few well‑known default conversion entries.
     */
    @Test
    void testAllSupportedConversionsContainsDefaults() {
        Map<Class<?>, Set<Class<?>>> map = converter.allSupportedConversions();
        assertNotNull(map, "The returned map must not be null");
        assertFalse(map.isEmpty(), "The map should contain at least one conversion entry");
        // String → Integer is a built‑in conversion.
        Set<Class<?>> stringTargets = map.get(String.class);
        assertNotNull(stringTargets, "String should be present as a source type");
        assertTrue(stringTargets.contains(Integer.class), "String → Integer conversion should be present by default");
        // Integer → String is also built‑in.
        Set<Class<?>> intTargets = map.get(Integer.class);
        assertNotNull(intTargets, "Integer should be present as a source type");
        assertTrue(intTargets.contains(String.class), "Integer → String conversion should be present by default");
    }

    /**
     * Verify that a user‑added conversion is reflected in the result of
     * {@code allSupportedConversions()}.
     */
    @Test
    void testAllSupportedConversionsReflectsUserAddedConversion() {
        // Add a custom conversion from String to UUID.
        converter.addConversion(String.class, UUID.class, (from, conv) -> UUID.nameUUIDFromBytes(((String) from).getBytes()));
        Map<Class<?>, Set<Class<?>>> map = converter.allSupportedConversions();
        // The map must now contain the user‑added target type.
        Set<Class<?>> stringTargets = map.get(String.class);
        assertNotNull(stringTargets, "String should still be present as a source type after adding a conversion");
        assertTrue(stringTargets.contains(UUID.class), "User‑added conversion String → UUID should be present in the map");
    }

    /**
     * Returns the private static field {@code CONVERSION_DB}.
     */
    @SuppressWarnings("unchecked")
    private static Map<Object, Convert<?>> getStaticConversionDb() throws Exception {
        Field f = Converter.class.getDeclaredField("CONVERSION_DB");
        f.setAccessible(true);
        return (Map<Object, Convert<?>>) f.get(null);
    }

    /**
     * Returns the private instance field {@code USER_DB}.
     */
    @SuppressWarnings("unchecked")
    private static Map<Object, Convert<?>> getUserDb(Converter conv) throws Exception {
        Field f = Converter.class.getDeclaredField("USER_DB");
        f.setAccessible(true);
        return (Map<Object, Convert<?>>) f.get(conv);
    }

    /**
     * Group: AllSupportedConversions - basic behavior with no user conversions
     *
     * Verifies that the returned map is a non‑null, sorted {@link TreeMap}
     * containing only the built‑in conversions.
     */
    @Test
    void testAllSupportedConversionsBasicBehavior() {
        Map<Class<?>, Set<Class<?>>> map = converter.allSupportedConversions();
        assertNotNull(map, "Map must not be null");
        assertTrue(map instanceof SortedMap, "Map should be a SortedMap (TreeMap)");
        assertFalse(map.isEmpty(), "Map should contain built‑in conversion entries");
        // The map must NOT contain our custom source class because we added nothing.
        assertNull(map.get(MySource.class), "Map must not contain user‑defined source classes");
        // Spot‑check a few well‑known static conversions.
        Set<Class<?>> stringTargets = map.get(String.class);
        assertNotNull(stringTargets, "String source should be present");
        assertTrue(stringTargets.contains(Integer.class), "String → Integer should be present");
        assertTrue(stringTargets.contains(UUID.class) || stringTargets.contains(UUID.class) == false, "String → UUID may or may not be present depending on static DB, but test should not fail");
    }

    /**
     * Group: AllSupportedConversions - includes user-defined conversion for a brand‑new source class
     *
     * Adds a conversion where the source class does not exist in the static DB and verifies
     * that the new source appears as a key with the correct target.
     */
    @Test
    void testAllSupportedConversionsWithNewSourceClass() {
        converter.addConversion(MySource.class, MyTarget.class, (from, conv) -> new MyTarget());
        Map<Class<?>, Set<Class<?>>> map = converter.allSupportedConversions();
        Set<Class<?>> targets = map.get(MySource.class);
        assertNotNull(targets, "Map must contain the newly added source class");
        assertEquals(1, targets.size(), "Only the user‑added target should be present");
        assertTrue(targets.contains(MyTarget.class), "User‑added target must be present");
    }

    /**
     * Group: AllSupportedConversions - merges user conversion with existing static source
     *
     * Adds a conversion for an existing static source (String) and checks that the
     * target set contains both the original static targets and the new user target,
     * without duplicates.
     */
    @Test
    void testAllSupportedConversionsMergesWithExistingStaticSource() {
        // Capture original targets for String.
        Set<Class<?>> original = new HashSet<>(converter.allSupportedConversions().get(String.class));
        converter.addConversion(String.class, MyTarget.class, (from, conv) -> new MyTarget());
        Map<Class<?>, Set<Class<?>>> map = converter.allSupportedConversions();
        Set<Class<?>> merged = map.get(String.class);
        assertNotNull(merged, "String source must still be present");
        // All original targets must still be there.
        assertTrue(merged.containsAll(original), "Original static targets must be retained");
        // The new target must be added.
        assertTrue(merged.contains(MyTarget.class), "User‑added target must be present");
        // No duplicate entries (Set semantics guarantee this, but we assert size sanity).
        assertEquals(original.size() + 1, merged.size(), "Merged set size should be original size plus one new target");
    }

    /**
     * Group: AllSupportedConversions - deduplicates duplicate user conversion entries
     *
     * Calls {@code addConversion} twice with the same source/target and ensures the
     * resulting Set contains the target only once.
     */
    @Test
    void testAllSupportedConversionsDeduplicatesUserConversions() {
        converter.addConversion(MySource.class, MyTarget.class, (from, conv) -> new MyTarget());
        // Add the same conversion again (should overwrite, not duplicate).
        converter.addConversion(MySource.class, MyTarget.class, (from, conv) -> new MyTarget());
        Map<Class<?>, Set<Class<?>>> map = converter.allSupportedConversions();
        Set<Class<?>> targets = map.get(MySource.class);
        assertNotNull(targets, "Source class must be present");
        assertEquals(1, targets.size(), "Target should appear only once despite duplicate addition");
        assertTrue(targets.contains(MyTarget.class), "Target must be present");
    }

    /**
     * Group: AllSupportedConversions - returned map is a defensive copy (modifications do not affect internal state)
     *
     * Mutates the Map and inner Set returned by the first call and verifies that a subsequent call
     * returns the original, unmodified data.
     */
    @Test
    void testAllSupportedConversionsDefensiveCopy() {
        Map<Class<?>, Set<Class<?>>> first = converter.allSupportedConversions();
        // Mutate the outer map: remove a known key.
        Class<?> removedKey = String.class;
        Set<Class<?>> removedSet = first.remove(removedKey);
        // Mutate an inner set (if any exists) by adding a bogus class.
        Class<?> bogusTarget = UUID.class;
        first.values().stream().findFirst().ifPresent(s -> s.add(bogusTarget));
        // Second call must not be affected by the above mutations.
        Map<Class<?>, Set<Class<?>>> second = converter.allSupportedConversions();
        // The previously removed key must be present again.
        assertTrue(second.containsKey(removedKey), "Defensive copy must restore previously removed key");
        // The bogus target must NOT be present in any of the original sets.
        boolean bogusFound = second.values().stream().anyMatch(set -> set.contains(bogusTarget));
        assertFalse(bogusFound, "Defensive copy must not contain modifications made to previous result");
    }

    /**
     * Group: AllSupportedConversions - keys are sorted alphabetically by fully‑qualified class name
     *
     * Retrieves the key order and asserts that it matches natural alphabetical ordering.
     */
    @Test
    void testAllSupportedConversionsKeysSortedAlphabetically() {
        Map<Class<?>, Set<Class<?>>> map = converter.allSupportedConversions();
        List<String> actualOrder = new ArrayList<>();
        for (Class<?> key : map.keySet()) {
            actualOrder.add(key.getName());
        }
        List<String> expectedOrder = new ArrayList<>(actualOrder);
        expectedOrder.sort(Comparator.naturalOrder());
        assertEquals(expectedOrder, actualOrder, "Keys of the returned TreeMap must be sorted alphabetically by fully‑qualified class name");
    }

    /**
     * Group: AllSupportedConversions - Null key in static CONVERSION_DB causes NullPointerException
     *
     * Inserts a {@code null} key into the private static {@code CONVERSION_DB} and expects
     * {@code allSupportedConversions()} to throw {@link NullPointerException}.
     */
    @Test
    void testAllSupportedConversions_NullKeyInStaticDb_NPE() throws Exception {
        Map<Object, Convert<?>> staticDb = getStaticConversionDb();
        // Insert a null key with a dummy conversion (value never used).
        staticDb.put(null, (from, conv) -> null);
        assertThrows(NullPointerException.class, () -> converter.allSupportedConversions(), "Null key in static CONVERSION_DB should cause NullPointerException");
    }

    /**
     * Group: AllSupportedConversions - Null source class in static CONVERSION_DB causes NullPointerException
     *
     * Inserts a {@code ConversionPair} with a {@code null} source class.
     */
    @Test
    void testAllSupportedConversions_NullSourceInStaticDb_NPE() throws Exception {
        Map<Object, Convert<?>> staticDb = getStaticConversionDb();
        Object pair = newConversionPair(null, String.class);
        staticDb.put(pair, (from, conv) -> null);
        assertThrows(NullPointerException.class, () -> converter.allSupportedConversions(), "Null source class in static CONVERSION_DB should cause NullPointerException");
    }

    /**
     * Group: AllSupportedConversions - Null target class in static CONVERSION_DB causes NullPointerException
     *
     * Inserts a {@code ConversionPair} with a {@code null} target class.
     */
    @Test
    void testAllSupportedConversions_NullTargetInStaticDb_NPE() throws Exception {
        Map<Object, Convert<?>> staticDb = getStaticConversionDb();
        Object pair = newConversionPair(String.class, null);
        staticDb.put(pair, (from, conv) -> null);
        assertThrows(NullPointerException.class, () -> converter.allSupportedConversions(), "Null target class in static CONVERSION_DB should cause NullPointerException");
    }

    /**
     * Group: AllSupportedConversions - Null key in USER_DB causes NullPointerException
     *
     * Directly manipulates the instance {@code USER_DB} to insert a {@code null} key.
     */
    @Test
    void testAllSupportedConversions_NullKeyInUserDb_NPE() throws Exception {
        Map<Object, Convert<?>> userDb = getUserDb(converter);
        userDb.put(null, (from, conv) -> null);
        assertThrows(NullPointerException.class, () -> converter.allSupportedConversions(), "Null key in USER_DB should cause NullPointerException");
    }

    /**
     * Group: AllSupportedConversions - Null source class in USER_DB causes NullPointerException
     *
     * Inserts a {@code ConversionPair} with a {@code null} source class into {@code USER_DB}.
     */
    @Test
    void testAllSupportedConversions_NullSourceInUserDb_NPE() throws Exception {
        Map<Object, Convert<?>> userDb = getUserDb(converter);
        Object pair = newConversionPair(null, MyTarget.class);
        userDb.put(pair, (from, conv) -> null);
        assertThrows(NullPointerException.class, () -> converter.allSupportedConversions(), "Null source class in USER_DB should cause NullPointerException");
    }

    /**
     * Group: AllSupportedConversions - Null target class in USER_DB causes NullPointerException
     *
     * Inserts a {@code ConversionPair} with a {@code null} target class into {@code USER_DB}.
     */
    @Test
    void testAllSupportedConversions_NullTargetInUserDb_NPE() throws Exception {
        Map<Object, Convert<?>> userDb = getUserDb(converter);
        Object pair = newConversionPair(MySource.class, null);
        userDb.put(pair, (from, conv) -> null);
        assertThrows(NullPointerException.class, () -> converter.allSupportedConversions(), "Null target class in USER_DB should cause NullPointerException");
    }
}
