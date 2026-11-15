package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.io.*;
import java.lang.reflect.*;
import java.security.Permission;

class CompactCIHashMap_isCaseInsensitive_Test {

    private TestableCompactCIHashMap<String, Integer> emptyMap;

    private TestableCompactCIHashMap<String, Integer> mapWithData;

    /**
     * Simple concrete subclass that exposes the protected method for testing.
     */
    private static class TestableCompactCIHashMap<K, V> extends CompactCIHashMap<K, V> {

        /**
         * Exposes the protected method as public for the test suite.
         */
        public boolean isCaseInsensitivePublic() {
            return super.isCaseInsensitive();
        }
    }

    /**
     * Subclass that overrides the method to return {@code false}.
     */
    private static class OverrideFalseCompactCIHashMap<K, V> extends CompactCIHashMap<K, V> {

        @Override
        protected boolean isCaseInsensitive() {
            return false;
        }

        public boolean isCaseInsensitivePublic() {
            return super.isCaseInsensitive();
        }
    }

    /**
     * Subclass that overrides the method but still returns {@code true}.
     */
    private static class OverrideTrueCompactCIHashMap<K, V> extends CompactCIHashMap<K, V> {

        @Override
        protected boolean isCaseInsensitive() {
            return true;
        }

        public boolean isCaseInsensitivePublic() {
            return super.isCaseInsensitive();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Instance with no initial data.
        emptyMap = new TestableCompactCIHashMap<>();
        // Instance initialized with a pre‑populated map.
        Map<String, Integer> source = new HashMap<>();
        source.put("One", 1);
        source.put("Two", 2);
        mapWithData = new TestableCompactCIHashMap<>(source);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC; not strictly required but keeps the pattern consistent.
        emptyMap = null;
        mapWithData = null;
    }

    @Test
    void isCaseInsensitive_returnsTrue_forEmptyMap() {
        // GIVEN an empty CompactCIHashMap
        // WHEN the protected isCaseInsensitive() method is invoked via the public wrapper
        boolean result = emptyMap.isCaseInsensitivePublic();
        // THEN it must return true
        assertTrue(result, "CompactCIHashMap should report case‑insensitive behavior");
    }

    @Test
    void isCaseInsensitive_returnsTrue_forMapCreatedFromOther() {
        // GIVEN a CompactCIHashMap constructed from an existing map
        // WHEN the protected isCaseInsensitive() method is invoked via the public wrapper
        boolean result = mapWithData.isCaseInsensitivePublic();
        // THEN it must still return true
        assertTrue(result, "CompactCIHashMap should remain case‑insensitive when constructed from another map");
    }

    @Test
    void reflection_withoutSetAccessible_throwsIllegalAccessException() throws Exception {
        Method m = CompactCIHashMap.class.getDeclaredMethod("isCaseInsensitive");
        // Do NOT call setAccessible(true)
        CompactCIHashMap<?, ?> instance = new CompactCIHashMap<>();
        assertThrows(IllegalAccessException.class, () -> m.invoke(instance));
    }

    @Test
    void reflection_withSetAccessible_returnsTrue() throws Exception {
        Method m = CompactCIHashMap.class.getDeclaredMethod("isCaseInsensitive");
        m.setAccessible(true);
        CompactCIHashMap<?, ?> instance = new CompactCIHashMap<>();
        Object result = m.invoke(instance);
        assertTrue((Boolean) result, "Reflection with setAccessible(true) must return true");
    }

    @Test
    void reflection_nullTarget_throwsNullPointerException() throws Exception {
        Method m = CompactCIHashMap.class.getDeclaredMethod("isCaseInsensitive");
        m.setAccessible(true);
        assertThrows(NullPointerException.class, () -> m.invoke(null));
    }

    @Test
    void reflection_wrongArgumentCount_throwsIllegalArgumentException() throws Exception {
        Method m = CompactCIHashMap.class.getDeclaredMethod("isCaseInsensitive");
        m.setAccessible(true);
        CompactCIHashMap<?, ?> instance = new CompactCIHashMap<>();
        assertThrows(IllegalArgumentException.class, () -> m.invoke(instance, "unexpected"));
    }

    @Test
    void reflection_securityManagerDenies_throwsSecurityException() throws Exception {
        // Preserve the original SecurityManager
        SecurityManager original = System.getSecurityManager();
        // Install a restrictive SecurityManager that denies ReflectPermission "suppressAccessChecks"
        System.setSecurityManager(new SecurityManager() {

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything except ReflectPermission to suppress access checks
                if (perm instanceof ReflectPermission && "suppressAccessChecks".equals(perm.getName())) {
                    throw new SecurityException("Reflective access denied by test SecurityManager");
                }
            }
        });
        try {
            Method m = CompactCIHashMap.class.getDeclaredMethod("isCaseInsensitive");
            // This call should now throw SecurityException because setAccessible(true) is denied
            assertThrows(SecurityException.class, () -> m.setAccessible(true));
        } finally {
            // Restore original SecurityManager to avoid side‑effects on other tests
            System.setSecurityManager(original);
        }
    }

    @Test
    void subclass_withoutOverride_returnsTrue() {
        TestableCompactCIHashMap<String, Integer> sub = new TestableCompactCIHashMap<>();
        assertTrue(sub.isCaseInsensitivePublic(), "Subclass that does not override should inherit true");
    }

    @Test
    void subclass_overrideTrue_returnsTrue() {
        OverrideTrueCompactCIHashMap<String, Integer> sub = new OverrideTrueCompactCIHashMap<>();
        assertTrue(sub.isCaseInsensitivePublic(), "Subclass that overrides to true must still return true");
    }

    @Test
    void subclass_overrideFalse_returnsFalse() {
        OverrideFalseCompactCIHashMap<String, Integer> sub = new OverrideFalseCompactCIHashMap<>();
        assertFalse(sub.isCaseInsensitivePublic(), "Subclass that overrides to false must return false");
    }

    @Test
    void isCaseInsensitive_stillTrue_afterMutations() {
        TestableCompactCIHashMap<String, Integer> map = new TestableCompactCIHashMap<>();
        map.put("Alpha", 10);
        map.put("Beta", 20);
        map.put("Gamma", 30);
        assertTrue(map.isCaseInsensitivePublic(), "Method must stay true after adding entries");
        map.clear();
        assertTrue(map.isCaseInsensitivePublic(), "Method must stay true after clearing the map");
    }

    @Test
    void isCaseInsensitive_afterSerialization_returnsTrue() throws IOException, ClassNotFoundException {
        TestableCompactCIHashMap<String, Integer> original = new TestableCompactCIHashMap<>();
        original.put("Key", 123);
        // Serialize
        byte[] bytes;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(original);
            out.flush();
            bytes = bos.toByteArray();
        }
        // Deserialize
        TestableCompactCIHashMap<String, Integer> deserialized;
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            ObjectInputStream in = new ObjectInputStream(bis)) {
            @SuppressWarnings("unchecked")
            TestableCompactCIHashMap<String, Integer> tmp = (TestableCompactCIHashMap<String, Integer>) in.readObject();
            deserialized = tmp;
        }
        assertTrue(deserialized.isCaseInsensitivePublic(), "Deserialized instance must still report case‑insensitive behavior");
    }

    @ParameterizedTest(name = "reflection setAccessible={0} should return true")
    @ValueSource(booleans = { true })
    void reflection_parameterized_setAccessibleTrue_returnsTrue(boolean setAccessible) throws Exception {
        Method m = CompactCIHashMap.class.getDeclaredMethod("isCaseInsensitive");
        m.setAccessible(setAccessible);
        CompactCIHashMap<?, ?> instance = new CompactCIHashMap<>();
        Object result = m.invoke(instance);
        assertTrue((Boolean) result);
    }
}
