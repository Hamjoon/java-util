package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class Converter_getSupportedConversions_Test {

    private Converter converter;

    /**
     * Simple source class for a custom conversion test.
     */
    private static class DummySource {
    }

    /**
     * Simple target class for a custom conversion test.
     */
    private static class DummyTarget {
    }

    /**
     * Minimal {@link Convert} implementation used for dummy registrations.
     */
    @FunctionalInterface
    private interface DummyConvert extends Convert<Object> {
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a Converter with minimal options – only charset is required for the constructor.
        ConverterOptions options = new ConverterOptions() {

            @Override
            public <T> T getCustomOption(String name) {
                return null;
            }

            @Override
            public java.nio.charset.Charset getCharset() {
                return java.nio.charset.StandardCharsets.UTF_8;
            }
        };
        converter = new Converter(options);
        // Ensure a clean state before each test.
        clearStaticDb();
        clearUserDb(converter);
    }

    @AfterEach
    void teardownAfterEach() {
        // The Converter does not hold external resources that need explicit cleanup.
        converter = null;
        // Clean up static state to avoid cross‑test contamination.
        clearStaticDb();
    }

    /**
     * Clears the static {@code CONVERSION_DB}.
     */
    private static void clearStaticDb() {
        try {
            Field staticDbField = Converter.class.getDeclaredField("CONVERSION_DB");
            staticDbField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<?, ?> staticDb = (Map<?, ?>) staticDbField.get(null);
            staticDb.clear();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Clears the instance {@code USER_DB} of the supplied converter.
     */
    private static void clearUserDb(Converter conv) {
        try {
            Field userDbField = Converter.class.getDeclaredField("USER_DB");
            userDbField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<?, ?> userDb = (Map<?, ?>) userDbField.get(conv);
            userDb.clear();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Inserts a conversion into the static DB via the public API.
     */
    private static void addStaticConversion(Class<?> src, Class<?> tgt, DummyConvert fn) {
        // Use a temporary Converter instance to call addConversion (adds to USER_DB),
        // then move the entry to the static DB via reflection.
        Converter temp = new Converter(new ConverterOptions() {

            @Override
            public <T> T getCustomOption(String name) {
                return null;
            }

            @Override
            public java.nio.charset.Charset getCharset() {
                return java.nio.charset.StandardCharsets.UTF_8;
            }
        });
        temp.addConversion(src, tgt, fn);
        // Move entry from USER_DB to CONVERSION_DB.
        try {
            Field staticDbField = Converter.class.getDeclaredField("CONVERSION_DB");
            Field userDbField = Converter.class.getDeclaredField("USER_DB");
            staticDbField.setAccessible(true);
            userDbField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<Object, Object> staticDb = (Map<Object, Object>) staticDbField.get(null);
            @SuppressWarnings("unchecked")
            Map<Object, Object> userDb = (Map<Object, Object>) userDbField.get(temp);
            staticDb.putAll(userDb);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Inserts a conversion into the user DB via the public API.
     */
    private static void addUserConversion(Converter conv, Class<?> src, Class<?> tgt, DummyConvert fn) {
        conv.addConversion(src, tgt, fn);
    }

    /**
     * Directly injects a malformed entry into the static DB (used for NPE tests).
     */
    private static void injectMalformedStaticEntry(Object key, Convert<?> fn) {
        try {
            Field staticDbField = Converter.class.getDeclaredField("CONVERSION_DB");
            staticDbField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<Object, Object> staticDb = (Map<Object, Object>) staticDbField.get(null);
            staticDb.put(key, fn);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Directly injects a malformed entry into the user DB (used for NPE tests).
     */
    private static void injectMalformedUserEntry(Converter conv, Object key, Convert<?> fn) {
        try {
            Field userDbField = Converter.class.getDeclaredField("USER_DB");
            userDbField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<Object, Object> userDb = (Map<Object, Object>) userDbField.get(conv);
            userDb.put(key, fn);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void getSupportedConversions_containsBuiltInConversion() {
        Map<String, Set<String>> supported = converter.getSupportedConversions();
        assertNotNull(supported, "Supported conversions map must not be null");
        // The built‑in conversion from String to Integer should be present.
        assertTrue(supported.containsKey("String"), "Supported conversions should contain a key for source type \"String\"");
        assertTrue(supported.get("String").contains("Integer"), "Supported conversions for \"String\" should include target type \"Integer\"");
    }

    @Test
    void getSupportedConversions_reflectsUserAddedConversion() {
        // Register a custom conversion.
        converter.addConversion(DummySource.class, DummyTarget.class, (from, conv) -> new DummyTarget());
        Map<String, Set<String>> supported = converter.getSupportedConversions();
        assertNotNull(supported, "Supported conversions map must not be null after adding a custom conversion");
        assertTrue(supported.containsKey("DummySource"), "Supported conversions should contain the custom source type \"DummySource\"");
        assertTrue(supported.get("DummySource").contains("DummyTarget"), "Supported conversions for \"DummySource\" should include the custom target type \"DummyTarget\"");
    }

    @Test
    void getSupportedConversions_EmptyDatabases() {
        // Both DBs are cleared in @BeforeEach.
        Map<String, Set<String>> supported = converter.getSupportedConversions();
        assertNotNull(supported);
        assertTrue(supported.isEmpty(), "Map should be empty when no conversions are registered");
    }

    @Test
    void getSupportedConversions_StaticDbOnly() {
        // dummy conversion
        DummyConvert fn = (from, c) -> 42;
        addStaticConversion(String.class, Integer.class, fn);
        Map<String, Set<String>> map = converter.getSupportedConversions();
        assertTrue(map.containsKey("Integer"));
        assertEquals(Set.of("String"), map.get("Integer"));
    }

    @Test
    void getSupportedConversions_UserDbOnly() {
        DummyConvert fn = (from, c) -> java.time.Instant.now();
        addUserConversion(converter, java.time.LocalDate.class, java.time.Instant.class, fn);
        Map<String, Set<String>> map = converter.getSupportedConversions();
        assertTrue(map.containsKey("Instant"));
        assertEquals(Set.of("LocalDate"), map.get("Instant"));
    }

    @Test
    void getSupportedConversions_CombinedDbDistinctTargets() {
        DummyConvert fn1 = (f, c) -> 1;
        DummyConvert fn2 = (f, c) -> 2.0;
        addStaticConversion(String.class, Integer.class, fn1);
        addUserConversion(converter, Boolean.class, Double.class, fn2);
        Map<String, Set<String>> map = converter.getSupportedConversions();
        assertEquals(Set.of("String"), map.get("Integer"));
        assertEquals(Set.of("Boolean"), map.get("Double"));
        assertEquals(2, map.size());
    }

    @Test
    void getSupportedConversions_CombinedDbOverlappingTarget() {
        DummyConvert fn1 = (f, c) -> 1;
        DummyConvert fn2 = (f, c) -> 2;
        addStaticConversion(String.class, Integer.class, fn1);
        addUserConversion(converter, Long.class, Integer.class, fn2);
        Map<String, Set<String>> map = converter.getSupportedConversions();
        assertTrue(map.containsKey("Integer"));
        Set<String> sources = map.get("Integer");
        assertEquals(Set.of("String", "Long"), sources);
    }

    @Test
    void getSupportedConversions_DuplicateConversionInBothDbs() {
        DummyConvert fn1 = (f, c) -> 1;
        DummyConvert fn2 = (f, c) -> 2;
        addStaticConversion(String.class, Integer.class, fn1);
        addUserConversion(converter, String.class, Integer.class, fn2);
        Map<String, Set<String>> map = converter.getSupportedConversions();
        assertTrue(map.containsKey("Integer"));
        assertEquals(Set.of("String"), map.get("Integer"));
    }

    @Test
    void getSupportedConversions_NullSourceOrTargetIgnored() {
        // Attempt to add invalid conversions – addConversion should reject them (no entry added).
        // The API silently ignores nulls; we just call and ensure map stays empty.
        assertDoesNotThrow(() -> converter.addConversion(null, Integer.class, (f, c) -> 0));
        assertDoesNotThrow(() -> converter.addConversion(String.class, null, (f, c) -> "x"));
        Map<String, Set<String>> map = converter.getSupportedConversions();
        assertTrue(map.isEmpty(), "Map should remain empty when null source/target are supplied");
    }

    @Test
    void getSupportedConversions_AlphabeticalOrderingOfTargetKeys() {
        DummyConvert fn1 = (f, c) -> 1;
        DummyConvert fn2 = (f, c) -> 2.0;
        DummyConvert fn3 = (f, c) -> java.math.BigDecimal.ONE;
        addStaticConversion(String.class, Integer.class, fn1);
        addStaticConversion(Boolean.class, Double.class, fn2);
        addStaticConversion(Long.class, java.math.BigDecimal.class, fn3);
        Map<String, Set<String>> map = converter.getSupportedConversions();
        // TreeMap guarantees natural ordering of keys.
        List<String> keys = new ArrayList<>(map.keySet());
        assertEquals(List.of("BigDecimal", "Double", "Integer"), keys);
        assertEquals(Set.of("Long"), map.get("BigDecimal"));
        assertEquals(Set.of("Boolean"), map.get("Double"));
        assertEquals(Set.of("String"), map.get("Integer"));
    }

    @Test
    void getSupportedConversions_AlphabeticalOrderingOfSourceSet() {
        DummyConvert fn1 = (f, c) -> 1;
        DummyConvert fn2 = (f, c) -> true;
        DummyConvert fn3 = (f, c) -> 'c';
        addStaticConversion(String.class, Integer.class, fn1);
        addStaticConversion(Boolean.class, Integer.class, fn2);
        addStaticConversion(Character.class, Integer.class, fn3);
        Map<String, Set<String>> map = converter.getSupportedConversions();
        assertTrue(map.containsKey("Integer"));
        // The Set implementation used by getSupportedConversions is a TreeSet (sorted).
        Set<String> sourceSet = map.get("Integer");
        List<String> ordered = new ArrayList<>(sourceSet);
        assertEquals(List.of("Boolean", "Character", "String"), ordered);
    }

    @Test
    void getSupportedConversions_DuplicateConversionWithinSameDb() {
        DummyConvert fn1 = (f, c) -> 1;
        DummyConvert fn2 = (f, c) -> 2;
        addStaticConversion(String.class, Integer.class, fn1);
        // duplicate
        addStaticConversion(String.class, Integer.class, fn2);
        Map<String, Set<String>> map = converter.getSupportedConversions();
        assertEquals(Set.of("String"), map.get("Integer"));
    }

    @Test
    void getSupportedConversions_IdempotentMapImmutability() {
        DummyConvert fn = (f, c) -> 2.0;
        addStaticConversion(Boolean.class, Double.class, fn);
        Map<String, Set<String>> first = converter.getSupportedConversions();
        assertEquals(Set.of("Boolean"), first.get("Double"));
        // Mutate the returned map.
        first.put("Fake", Set.of("X"));
        // Subsequent call should not see the mutation.
        Map<String, Set<String>> second = converter.getSupportedConversions();
        assertFalse(second.containsKey("Fake"));
        assertEquals(Set.of("Boolean"), second.get("Double"));
    }

    @Test
    void getSupportedConversions_NullConversionFunctionIgnored() {
        // Directly add a conversion with null function via reflection (simulating addConversion rejection).
        addStaticConversion(String.class, Integer.class, null);
        // The static DB now contains a null function entry; getSupportedConversions should ignore it.
        Map<String, Set<String>> map = converter.getSupportedConversions();
        // Implementation may still create the entry because key exists; we assert that no NPE is thrown.
        // The expected behaviour per spec is that such entry is not stored; we verify map is empty.
        assertTrue(map.isEmpty(), "Map should be empty when conversion function is null");
    }

    @Test
    void getSupportedConversions_PrimitiveSourceHandledCorrectly() {
        DummyConvert fn = (f, c) -> String.valueOf(f);
        addStaticConversion(int.class, String.class, fn);
        Map<String, Set<String>> map = converter.getSupportedConversions();
        assertTrue(map.containsKey("String"));
        assertEquals(Set.of("int"), map.get("String"));
    }

    @Test
    void getSupportedConversions_StaticDbMalformedEntryNullSourceClass() {
        // Build a ConversionPair with null source (via reflection).
        try {
            Class<?> pairClass = Class.forName("com.cedarsoftware.util.convert.Converter$ConversionPair");
            java.lang.reflect.Constructor<?> ctor = pairClass.getDeclaredConstructor(Class.class, Class.class);
            ctor.setAccessible(true);
            Object malformedKey = ctor.newInstance(null, Integer.class);
            injectMalformedStaticEntry(malformedKey, (Convert<?>) (f, c) -> 0);
        } catch (Exception e) {
            fail("Failed to inject malformed entry: " + e);
        }
        assertThrows(NullPointerException.class, () -> converter.getSupportedConversions(), "Null source class should cause NPE");
    }

    @Test
    void getSupportedConversions_StaticDbMalformedEntryNullTargetClass() {
        try {
            Class<?> pairClass = Class.forName("com.cedarsoftware.util.convert.Converter$ConversionPair");
            java.lang.reflect.Constructor<?> ctor = pairClass.getDeclaredConstructor(Class.class, Class.class);
            ctor.setAccessible(true);
            Object malformedKey = ctor.newInstance(String.class, null);
            injectMalformedStaticEntry(malformedKey, (Convert<?>) (f, c) -> 0);
        } catch (Exception e) {
            fail("Failed to inject malformed entry: " + e);
        }
        assertThrows(NullPointerException.class, () -> converter.getSupportedConversions(), "Null target class should cause NPE");
    }

    @Test
    void getSupportedConversions_UserDbMalformedEntryNullSourceClass() {
        try {
            Class<?> pairClass = Class.forName("com.cedarsoftware.util.convert.Converter$ConversionPair");
            java.lang.reflect.Constructor<?> ctor = pairClass.getDeclaredConstructor(Class.class, Class.class);
            ctor.setAccessible(true);
            Object malformedKey = ctor.newInstance(null, java.time.Instant.class);
            injectMalformedUserEntry(converter, malformedKey, (Convert<?>) (f, c) -> java.time.Instant.now());
        } catch (Exception e) {
            fail("Failed to inject malformed user entry: " + e);
        }
        assertThrows(NullPointerException.class, () -> converter.getSupportedConversions(), "Null source class in USER_DB should cause NPE");
    }

    @Test
    void getSupportedConversions_UserDbMalformedEntryNullTargetClass() {
        try {
            Class<?> pairClass = Class.forName("com.cedarsoftware.util.convert.Converter$ConversionPair");
            java.lang.reflect.Constructor<?> ctor = pairClass.getDeclaredConstructor(Class.class, Class.class);
            ctor.setAccessible(true);
            Object malformedKey = ctor.newInstance(java.time.LocalDate.class, null);
            injectMalformedUserEntry(converter, malformedKey, (Convert<?>) (f, c) -> java.time.Instant.now());
        } catch (Exception e) {
            fail("Failed to inject malformed user entry: " + e);
        }
        assertThrows(NullPointerException.class, () -> converter.getSupportedConversions(), "Null target class in USER_DB should cause NPE");
    }

    @Test
    void getSupportedConversions_StaticDbEntryWithNullConversionPairKey() {
        // Directly put a null key into the static DB.
        injectMalformedStaticEntry(null, (Convert<?>) (f, c) -> 0);
        assertThrows(NullPointerException.class, () -> converter.getSupportedConversions(), "Null key in static DB should cause NPE");
    }

    @Test
    void getSupportedConversions_StaticDbEntryWithNullConversionFunctionValue() {
        // Insert a valid key but null function.
        try {
            Class<?> pairClass = Class.forName("com.cedarsoftware.util.convert.Converter$ConversionPair");
            java.lang.reflect.Constructor<?> ctor = pairClass.getDeclaredConstructor(Class.class, Class.class);
            ctor.setAccessible(true);
            Object key = ctor.newInstance(String.class, Integer.class);
            injectMalformedStaticEntry(key, null);
        } catch (Exception e) {
            fail("Failed to inject entry with null function: " + e);
        }
        // The method accesses the function only to store it; a null function leads to NPE when used.
        assertThrows(NullPointerException.class, () -> converter.getSupportedConversions(), "Null conversion function should cause NPE");
    }
}
