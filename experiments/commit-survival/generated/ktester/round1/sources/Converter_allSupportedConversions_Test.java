package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class Converter_allSupportedConversions_Test {

    /**
     * The instance under test.
     */
    private Converter converter;

    /**
     * A very small stub of {@link ConverterOptions} that supplies only the data required by the {@link Converter}
     *  constructor. All other methods return harmless defaults.
     */
    private static class StubOptions implements ConverterOptions {

        @Override
        public java.util.Map<java.util.Map.Entry<Class<?>, Class<?>>, Convert<?>> getConverterOverrides() {
            return Collections.emptyMap();
        }

        @Override
        public java.nio.charset.Charset getCharset() {
            return StandardCharsets.UTF_8;
        }

        @Override
        public java.time.ZoneId getZoneId() {
            return ZoneId.systemDefault();
        }

        @Override
        public Character trueChar() {
            return null;
        }

        @Override
        public Character falseChar() {
            return null;
        }

        @Override
        public <T> T getCustomOption(String name) {
            return null;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh Converter for every test to avoid cross‑test contamination.
        converter = new Converter(new StubOptions());
        // a fresh Converter for each test – ensures USER_DB starts empty
        converter = new Converter(new StubOptions());
    }

    /**
     * Verify that the method returns a non‑empty map and that a known built‑in conversion is present.
     */
    @Test
    void shouldReturnBuiltInConversions() {
        Map<Class<?>, Set<Class<?>>> all = converter.allSupportedConversions();
        assertNotNull(all, "The returned map must not be null");
        assertFalse(all.isEmpty(), "There should be at least one built‑in conversion");
        // The library ships with a conversion from String to Integer (String → Integer).
        Set<Class<?>> targets = all.get(String.class);
        assertNotNull(targets, "String should be a source class in the map");
        assertTrue(targets.contains(Integer.class), "The built‑in conversion String → Integer should be present");
    }

    /**
     * Verify that a user‑added conversion appears in the result of {@code allSupportedConversions()}.
     */
    @Test
    void shouldIncludeUserAddedConversions() {
        // Add a simple identity conversion: Boolean → String
        converter.addConversion(Boolean.class, String.class, (Convert<String>) Object::toString);
        Map<Class<?>, Set<Class<?>>> all = converter.allSupportedConversions();
        assertNotNull(all);
        Set<Class<?>> targets = all.get(Boolean.class);
        assertNotNull(targets, "User‑added source class must be present");
        assertTrue(targets.contains(String.class), "User‑added conversion Boolean → String should be reflected in the map");
    }

    /* -------------------------------------------------------------
     *  Helper methods for reflection manipulation
     * ------------------------------------------------------------- */
    @SuppressWarnings("unchecked")
    private static Map<Map.Entry<Class<?>, Class<?>>, Convert<?>> getStaticConversionDb() throws Exception {
        Field f = Converter.class.getDeclaredField("CONVERSION_DB");
        f.setAccessible(true);
        return (Map<Map.Entry<Class<?>, Class<?>>, Convert<?>>) f.get(null);
    }

    @SuppressWarnings("unchecked")
    private static void setStaticConversionDb(Map<?, ?> newMap) throws Exception {
        Field f = Converter.class.getDeclaredField("CONVERSION_DB");
        f.setAccessible(true);
        f.set(null, newMap);
    }

    @SuppressWarnings("unchecked")
    private Map<Map.Entry<Class<?>, Class<?>>, Convert<?>> getUserDb() throws Exception {
        Field f = Converter.class.getDeclaredField("USER_DB");
        f.setAccessible(true);
        return (Map<Map.Entry<Class<?>, Class<?>>, Convert<?>>) f.get(converter);
    }

    @SuppressWarnings("unchecked")
    private void setUserDb(Map<?, ?> newMap) throws Exception {
        Field f = Converter.class.getDeclaredField("USER_DB");
        f.setAccessible(true);
        f.set(converter, newMap);
    }

    /* -------------------------------------------------------------
     *  1. Empty databases – both static and user maps cleared
     * ------------------------------------------------------------- */
    @Test
    void emptyDatabases_returnsEmptyTreeMap() throws Exception {
        // clear static DB
        setStaticConversionDb(new HashMap<>());
        // clear user DB
        setUserDb(new ConcurrentHashMap<>());
        Map<Class<?>, Set<Class<?>>> result = converter.allSupportedConversions();
        assertNotNull(result, "Result map must never be null");
        assertTrue(result.isEmpty(), "Both DBs are empty, so the returned map should be empty");
    }

    /* -------------------------------------------------------------
     *  2. Default static conversions – map non‑empty and contains String→Integer
     * ------------------------------------------------------------- */
    @Test
    void defaultStaticConversions_containsStringToInteger() {
        Map<Class<?>, Set<Class<?>>> all = converter.allSupportedConversions();
        assertNotNull(all);
        assertFalse(all.isEmpty(), "Static DB should provide at least one conversion");
        Set<Class<?>> targets = all.get(String.class);
        assertNotNull(targets, "String should be a source key");
        assertTrue(targets.contains(Integer.class), "Built‑in conversion String → Integer must be present");
    }

    /* -------------------------------------------------------------
     *  3. Verify TreeMap ordering (case‑insensitive by class name)
     * ------------------------------------------------------------- */
    @Test
    void treeMapOrdering_isCaseInsensitive() {
        Map<Class<?>, Set<Class<?>>> all = converter.allSupportedConversions();
        // extract the class names in iteration order
        List<String> names = new ArrayList<>();
        for (Class<?> key : all.keySet()) {
            names.add(key.getName());
        }
        // create a copy and sort it with the same comparator used by Converter
        List<String> sorted = new ArrayList<>(names);
        sorted.sort(String.CASE_INSENSITIVE_ORDER);
        assertEquals(sorted, names, "The TreeMap should be ordered case‑insensitively by class name");
    }

    /* -------------------------------------------------------------
     *  4. User‑added conversion appears in result
     * ------------------------------------------------------------- */
    @Test
    void userAddedConversion_isReflected() {
        converter.addConversion(Boolean.class, UUID.class, (Convert<UUID>) b -> UUID.nameUUIDFromBytes(b.toString().getBytes()));
        Map<Class<?>, Set<Class<?>>> all = converter.allSupportedConversions();
        Set<Class<?>> targets = all.get(Boolean.class);
        assertNotNull(targets);
        assertTrue(targets.contains(UUID.class), "User‑added Boolean → UUID must be present");
    }

    /* -------------------------------------------------------------
     *  5. Multiple targets for same source are accumulated
     * ------------------------------------------------------------- */
    @Test
    void multipleUserConversionsForSameSource_accumulateTargets() {
        converter.addConversion(LocalDate.class, LocalDateTime.class, (Convert<LocalDateTime>) d -> d.atStartOfDay());
        converter.addConversion(LocalDate.class, Instant.class, (Convert<Instant>) d -> d.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Map<Class<?>, Set<Class<?>>> all = converter.allSupportedConversions();
        Set<Class<?>> targets = all.get(LocalDate.class);
        assertNotNull(targets);
        assertTrue(targets.containsAll(Arrays.asList(LocalDateTime.class, Instant.class)), "Both LocalDate → LocalDateTime and LocalDate → Instant must be present");
    }

    /* -------------------------------------------------------------
     *  6. User override does not duplicate entry
     * ------------------------------------------------------------- */
    @Test
    void userOverrideDoesNotDuplicateExistingConversion() {
        // there is already a static String→Integer conversion
        converter.addConversion(String.class, Integer.class, (Convert<Integer>) Integer::valueOf);
        Map<Class<?>, Set<Class<?>>> all = converter.allSupportedConversions();
        Set<Class<?>> targets = all.get(String.class);
        assertNotNull(targets);
        // the set should still contain only one Integer entry
        long count = targets.stream().filter(c -> c.equals(Integer.class)).count();
        assertEquals(1, count, "Overriding an existing conversion must not create duplicate entries");
    }

    /* -------------------------------------------------------------
     *  7. Snapshot independence – subsequent adds do not affect previous map
     * ------------------------------------------------------------- */
    @Test
    void snapshotIndependence_afterSubsequentAdditions() {
        Map<Class<?>, Set<Class<?>>> snapshot = converter.allSupportedConversions();
        // add a new conversion after snapshot
        converter.addConversion(Byte.class, String.class, (Convert<String>) Object::toString);
        Map<Class<?>, Set<Class<?>>> later = converter.allSupportedConversions();
        // snapshot must stay unchanged
        assertFalse(later.equals(snapshot) && later.size() == snapshot.size(), "Later map should differ from the earlier snapshot");
        assertNull(snapshot.get(Byte.class), "Snapshot must not contain the newly added conversion");
    }

    /* -------------------------------------------------------------
     *  8. Null static DB leads to NPE
     * ------------------------------------------------------------- */
    @Test
    void nullStaticConversionDb_throwsNullPointerException() throws Exception {
        // set static field to null
        setStaticConversionDb(null);
        assertThrows(NullPointerException.class, () -> converter.allSupportedConversions(), "Iterating over a null static DB should throw NPE");
    }

    /* -------------------------------------------------------------
     *  9. Null user DB leads to NPE
     * ------------------------------------------------------------- */
    @Test
    void nullUserConversionDb_throwsNullPointerException() throws Exception {
        setUserDb(null);
        assertThrows(NullPointerException.class, () -> converter.allSupportedConversions(), "Iterating over a null user DB should throw NPE");
    }

    /* -------------------------------------------------------------
     * 10. Null entry key in static DB causes NPE
     * ------------------------------------------------------------- */
    @Test
    void nullEntryKeyInStaticDb_throwsNullPointerException() throws Exception {
        Map<Map.Entry<Class<?>, Class<?>>, Convert<?>> staticDb = getStaticConversionDb();
        // Insert a null key entry (the map permits null keys)
        staticDb.put(null, null);
        assertThrows(NullPointerException.class, () -> converter.allSupportedConversions(), "Processing a null map entry key should raise NPE");
    }

    /* -------------------------------------------------------------
     * 11. Null entry key in user DB causes NPE
     * ------------------------------------------------------------- */
    @Test
    void nullEntryKeyInUserDb_throwsNullPointerException() throws Exception {
        Map<Map.Entry<Class<?>, Class<?>>, Convert<?>> userDb = getUserDb();
        userDb.put(null, null);
        assertThrows(NullPointerException.class, () -> converter.allSupportedConversions(), "Processing a null map entry key in USER_DB should raise NPE");
    }

    /* -------------------------------------------------------------
     * 12. Null source class inside entry key causes NPE
     * ------------------------------------------------------------- */
    @Test
    void nullSourceClassInEntry_throwsNullPointerException() throws Exception {
        Map<Map.Entry<Class<?>, Class<?>>, Convert<?>> staticDb = getStaticConversionDb();
        // Create a Map.Entry where the source (key.getKey()) is null
        Map.Entry<Class<?>, Class<?>> brokenKey = new AbstractMap.SimpleEntry<>(null, Integer.class);
        staticDb.put(brokenKey, null);
        assertThrows(NullPointerException.class, () -> converter.allSupportedConversions(), "Null source class inside entry should cause NPE");
    }

    /* -------------------------------------------------------------
     * 13. Null target class inside entry key causes NPE
     * ------------------------------------------------------------- */
    @Test
    void nullTargetClassInEntry_throwsNullPointerException() throws Exception {
        Map<Map.Entry<Class<?>, Class<?>>, Convert<?>> userDb = getUserDb();
        // Entry with a null target class
        Map.Entry<Class<?>, Class<?>> brokenKey = new AbstractMap.SimpleEntry<>(String.class, null);
        userDb.put(brokenKey, null);
        assertThrows(NullPointerException.class, () -> converter.allSupportedConversions(), "Null target class inside entry should cause NPE");
    }

    /* -------------------------------------------------------------
     * 14. Invalid entry type in static DB causes ClassCastException
     * ------------------------------------------------------------- */
    @Test
    void invalidEntryTypeInStaticDb_throwsClassCastException() throws Exception {
        // Directly manipulate the raw map to insert a wrong‑typed key
        @SuppressWarnings("unchecked")
        Map rawMap = getStaticConversionDb();
        rawMap.put("not-an-entry", null);
        assertThrows(ClassCastException.class, () -> converter.allSupportedConversions(), "A non‑Map.Entry key should trigger ClassCastException");
    }

    /* -------------------------------------------------------------
     * 15. Invalid entry type in user DB causes ClassCastException
     * ------------------------------------------------------------- */
    @Test
    void invalidEntryTypeInUserDb_throwsClassCastException() throws Exception {
        @SuppressWarnings("unchecked")
        Map rawMap = getUserDb();
        // key not of type Class<?>
        rawMap.put(new AbstractMap.SimpleEntry<>("bad", "key"), null);
        assertThrows(ClassCastException.class, () -> converter.allSupportedConversions(), "Incorrectly typed entry key in USER_DB should trigger ClassCastException");
    }
}
