package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.*;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CompactCILinkedMap_isCaseInsensitive_Test {

    private TestableCompactCILinkedMap<String, String> map;

    /**
     * Exposes the protected {@code isCaseInsensitive()} method.
     */
    private static class TestableCompactCILinkedMap<K, V> extends CompactCILinkedMap<K, V> {

        public boolean callIsCaseInsensitive() {
            return super.isCaseInsensitive();
        }

        /**
         * Helper to expose {@code getNewMap()} for the “other protected methods” group.
         */
        public Map<K, V> callGetNewMap() {
            return super.getNewMap();
        }

        /**
         * Helper to expose {@code useCopyIterator()} for the “other protected methods” group.
         */
        public boolean callUseCopyIterator() {
            return super.useCopyIterator();
        }
    }

    /**
     * Subclass that overrides {@code isCaseInsensitive()} to return {@code false}
     * but still provides a public method that delegates to {@code super.isCaseInsensitive()}.
     */
    private static class OverridingCompactCILinkedMap<K, V> extends CompactCILinkedMap<K, V> {

        @Override
        protected boolean isCaseInsensitive() {
            return false;
        }

        public boolean callSuperIsCaseInsensitive() {
            return super.isCaseInsensitive();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh instance before each test.
        map = new TestableCompactCILinkedMap<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference to help GC; not strictly required.
        map = null;
    }

    private static Stream<Map.Entry<String, Map<?, ?>>> genericMapProvider() {
        return Stream.of(Map.entry("String,Integer", new CompactCILinkedMap<String, Integer>()), Map.entry("Integer,String", new CompactCILinkedMap<Integer, String>()), Map.entry("Object,Object", new CompactCILinkedMap<Object, Object>()));
    }

    @Test
    void isCaseInsensitive_returnsTrue() {
        // GIVEN – map is instantiated in @BeforeEach
        // WHEN
        boolean result = map.callIsCaseInsensitive();
        // THEN
        assertTrue(result, "CompactCILinkedMap should report case‑insensitivity as true");
    }

    @Test
    @DisplayName("Default instance returns true")
    void isCaseInsensitive_returnsTrue_defaultInstance() {
        assertTrue(map.callIsCaseInsensitive(), "CompactCILinkedMap should report case‑insensitivity as true");
    }

    @Test
    @DisplayName("Subclass instance (no override) still returns true")
    void isCaseInsensitive_returnsTrue_subclassNoOverride() {
        TestableCompactCILinkedMap<String, String> sub = new TestableCompactCILinkedMap<>();
        assertTrue(sub.callIsCaseInsensitive(), "Subclass without override should still return true");
    }

    @Test
    @DisplayName("Populated map still returns true")
    void isCaseInsensitive_returnsTrue_afterPopulation() {
        map.put("KeyOne", "value1");
        map.put("KEYTWO", "value2");
        assertTrue(map.callIsCaseInsensitive(), "Method must keep returning true after entries are added");
    }

    @Test
    @DisplayName("Cleared map still returns true")
    void isCaseInsensitive_returnsTrue_afterClear() {
        map.put("a", "1");
        map.clear();
        assertTrue(map.callIsCaseInsensitive(), "Method must keep returning true after the map is cleared");
    }

    @ParameterizedTest(name = "{index} – {0}")
    @MethodSource("genericMapProvider")
    @DisplayName("isCaseInsensitive() returns true for various generic types")
    void isCaseInsensitive_returnsTrue_variousGenerics(String description, Map<?, ?> instance) {
        // Use reflection to call the protected method because the generic instance
        // is not of the testable subclass.
        try {
            Method m = CompactCILinkedMap.class.getDeclaredMethod("isCaseInsensitive");
            m.setAccessible(true);
            boolean result = (boolean) m.invoke(instance);
            assertTrue(result, "Expected true for generic type: " + description);
        } catch (Exception e) {
            fail("Reflection failed for generic type " + description, e);
        }
    }

    @Test
    @DisplayName("Overridden method returns false, super call returns true")
    void isCaseInsensitive_overriddenButSuperReturnsTrue() {
        OverridingCompactCILinkedMap<String, String> overriding = new OverridingCompactCILinkedMap<>();
        // Direct call (should be false)
        assertFalse(overriding.isCaseInsensitive(), "Overridden method should return false");
        // Call via helper that invokes super – must be true
        assertTrue(overriding.callSuperIsCaseInsensitive(), "Super implementation must still return true");
    }

    @Test
    @DisplayName("Reflection on overridden subclass still sees true from CompactCILinkedMap")
    void isCaseInsensitive_reflectionOnOverriddenSubclass() throws Exception {
        OverridingCompactCILinkedMap<String, String> overriding = new OverridingCompactCILinkedMap<>();
        Method m = CompactCILinkedMap.class.getDeclaredMethod("isCaseInsensitive");
        m.setAccessible(true);
        boolean result = (boolean) m.invoke(overriding);
        assertTrue(result, "Reflective call on declared method should return true even if subclass overrides");
    }

    @Test
    @DisplayName("Serialized then deserialized populated map retains true")
    void isCaseInsensitive_afterSerialization_populated() throws Exception {
        map.put("One", "1");
        map.put("Two", "2");
        CompactCILinkedMap<String, String> deserialized = serializeAndDeserialize(map);
        assertTrue(deserialized.isCaseInsensitive(), "Deserialized populated map must still report true");
    }

    @Test
    @DisplayName("Serialized then deserialized empty map retains true")
    void isCaseInsensitive_afterSerialization_empty() throws Exception {
        CompactCILinkedMap<String, String> deserialized = serializeAndDeserialize(map);
        assertTrue(deserialized.isCaseInsensitive(), "Deserialized empty map must still report true");
    }

    /**
     * Helper that serialises an object to a byte‑array and deserialises it back.
     */
    @SuppressWarnings("unchecked")
    private static <K, V> CompactCILinkedMap<K, V> serializeAndDeserialize(CompactCILinkedMap<K, V> original) throws IOException, ClassNotFoundException {
        byte[] bytes;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(original);
            out.flush();
            bytes = bos.toByteArray();
        }
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            ObjectInputStream in = new ObjectInputStream(bis)) {
            return (CompactCILinkedMap<K, V>) in.readObject();
        }
    }

    @Test
    @DisplayName("Calling getNewMap() does not affect isCaseInsensitive()")
    void isCaseInsensitive_afterGetNewMap() {
        Map<String, String> newMap = map.callGetNewMap();
        assertNotNull(newMap, "getNewMap() should return a non‑null map");
        assertTrue(map.callIsCaseInsensitive(), "isCaseInsensitive() must still be true after getNewMap()");
    }

    @Test
    @DisplayName("Calling useCopyIterator() does not affect isCaseInsensitive()")
    void isCaseInsensitive_afterUseCopyIterator() {
        boolean copyIter = map.callUseCopyIterator();
        // The actual value is not relevant for the test; we only care that the call succeeds.
        assertTrue(map.callIsCaseInsensitive(), "isCaseInsensitive() must still be true after useCopyIterator()");
    }

    @Test
    @DisplayName("Direct reflective invocation returns true")
    void isCaseInsensitive_reflectionDirect() throws Exception {
        Method m = CompactCILinkedMap.class.getDeclaredMethod("isCaseInsensitive");
        m.setAccessible(true);
        boolean result = (boolean) m.invoke(map);
        assertTrue(result, "Reflective call should return true");
    }

    @Test
    @DisplayName("Reflective invocation on subclass overriding method still returns true")
    void isCaseInsensitive_reflectionOnSubclassOverride() throws Exception {
        OverridingCompactCILinkedMap<String, String> overriding = new OverridingCompactCILinkedMap<>();
        Method m = CompactCILinkedMap.class.getDeclaredMethod("isCaseInsensitive");
        m.setAccessible(true);
        boolean result = (boolean) m.invoke(overriding);
        assertTrue(result, "Reflective call on declared method should return true even if subclass overrides");
    }
}
