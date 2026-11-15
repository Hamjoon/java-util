package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Method;

@ExtendWith(MockitoExtension.class)
class CompactMap_getSingleValueKey_Test {

    private TestCompactMap map;

    /**
     * Simple subclass that makes {@code getSingleValueKey()} publicly accessible.
     */
    private static class TestCompactMap extends CompactMap<String, Integer> {

        /**
         * Exposes the protected method for testing.
         */
        public String exposeGetSingleValueKey() {
            return super.getSingleValueKey();
        }
    }

    /**
     * Subclass for {@code String} keys and {@code Object} values.
     */
    private static class StringCompactMap extends CompactMap<String, Object> {

        public String exposeGetSingleValueKey() {
            return super.getSingleValueKey();
        }
    }

    /**
     * Subclass for raw {@code Object} keys/values.
     */
    private static class ObjectCompactMap extends CompactMap<Object, Object> {

        public Object exposeGetSingleValueKey() {
            return super.getSingleValueKey();
        }
    }

    /**
     * Subclass for {@code Integer} keys – used to provoke a {@code ClassCastException}.
     */
    private static class IntegerCompactMap extends CompactMap<Integer, Object> {

        public Integer exposeGetSingleValueKey() {
            return super.getSingleValueKey();
        }
    }

    /**
     * Simple user‑defined type that does not extend {@code String}.
     */
    private static class CustomType {
    }

    /**
     * Subclass for {@code CustomType} keys – also used to provoke a {@code ClassCastException}.
     */
    private static class CustomTypeCompactMap extends CompactMap<CustomType, Object> {

        public CustomType exposeGetSingleValueKey() {
            return super.getSingleValueKey();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh instance before each test.
        map = new TestCompactMap();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference to help GC (not strictly necessary).
        map = null;
    }

    @Test
    @DisplayName("getSingleValueKey() should return the constant key defined in CompactMap")
    void testGetSingleValueKeyReturnsConstant() {
        // GIVEN – a freshly constructed CompactMap (via subclass)
        // WHEN
        String key = map.exposeGetSingleValueKey();
        // THEN
        assertEquals("key", key, "The single‑value key should be the literal string \"key\"");
    }

    @Test
    @DisplayName("String‑typed CompactMap returns \"key\" as a String")
    void testGetSingleValueKeyStringTyped() {
        StringCompactMap m = new StringCompactMap();
        String key = m.exposeGetSingleValueKey();
        assertEquals("key", key, "String‑typed map must return the literal \"key\"");
    }

    @Test
    @DisplayName("Object‑typed CompactMap returns a String instance")
    void testGetSingleValueKeyObjectTyped() {
        ObjectCompactMap m = new ObjectCompactMap();
        Object key = m.exposeGetSingleValueKey();
        assertTrue(key instanceof String, "Object‑typed map should return a String instance");
        assertEquals("key", key, "Returned value must equal \"key\"");
    }

    @Test
    @DisplayName("Integer‑typed CompactMap throws ClassCastException")
    void testGetSingleValueKeyIntegerTypedThrows() {
        IntegerCompactMap m = new IntegerCompactMap();
        assertThrows(ClassCastException.class, m::exposeGetSingleValueKey, "Casting the literal \"key\" to Integer must fail");
    }

    @Test
    @DisplayName("CustomType‑typed CompactMap throws ClassCastException")
    void testGetSingleValueKeyCustomTypeThrows() {
        CustomTypeCompactMap m = new CustomTypeCompactMap();
        assertThrows(ClassCastException.class, m::exposeGetSingleValueKey, "Casting the literal \"key\" to CustomType must fail");
    }

    @Test
    @DisplayName("Two successive calls return the identical object (reference equality)")
    void testGetSingleValueKeyIdentity() {
        String first = map.exposeGetSingleValueKey();
        String second = map.exposeGetSingleValueKey();
        assertSame(first, second, "Repeated calls must return the same object reference");
    }

    @Test
    @DisplayName("Reflection can invoke protected getSingleValueKey() and obtain \"key\"")
    void testGetSingleValueKeyViaReflection() throws Exception {
        // Raw (unchecked) instance – generic type is irrelevant for reflection.
        CompactMap rawMap = new CompactMap();
        Method method = CompactMap.class.getDeclaredMethod("getSingleValueKey");
        // bypass Java access checks
        method.setAccessible(true);
        Object result = method.invoke(rawMap);
        assertEquals("key", result, "Reflection should return the literal \"key\"");
    }

    @Test
    @DisplayName("Invoking protected getSingleValueKey() via reflection without setAccessible throws IllegalAccessException")
    void testGetSingleValueKeyReflectionIllegalAccess() throws Exception {
        CompactMap rawMap = new CompactMap();
        Method method = CompactMap.class.getDeclaredMethod("getSingleValueKey");
        // Do NOT call setAccessible(true); the JVM should enforce access control.
        assertThrows(IllegalAccessException.class, () -> method.invoke(rawMap), "Reflective invocation without accessibility override must fail");
    }
}
