package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DefaultConverterOptions_getCustomOption_Test {

    private DefaultConverterOptions options;

    /* --------------------------------------------------------------------- */
    private static final class MyCustom {

        private final int id;

        MyCustom(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof MyCustom))
                return false;
            MyCustom myCustom = (MyCustom) o;
            return id == myCustom.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public String toString() {
            return "MyCustom{id:" + id + '}';
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh instance for each test so that state does not leak between tests.
        options = new DefaultConverterOptions();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the reference – not strictly required but keeps the test tidy.
        options = null;
    }

    /**
     * Helper that injects a custom map into the {@code DefaultConverterOptions} instance
     * via reflection.
     */
    private void injectCustomOptions(Map<String, Object> map) throws ReflectiveOperationException {
        Field field = DefaultConverterOptions.class.getDeclaredField("customOptions");
        field.setAccessible(true);
        field.set(options, map);
    }

    private static Stream<Arguments> provideKeyAndExpected() {
        return Stream.of(Arguments.of("existingKey", "value1", String.class), Arguments.of("missingKey", null, Object.class), Arguments.of("nullValueKey", null, Object.class), Arguments.of("intKey", ClassCastException.class, String.class), Arguments.of("emptyKey", null, Object.class), Arguments.of(null, NullPointerException.class, Object.class), Arguments.of("numberKey", 42, Number.class), Arguments.of("listKey", Arrays.asList(1, 2), List.class), Arguments.of("customObjKey", new MyCustom(123), MyCustom.class));
    }

    @Test
    @DisplayName("getCustomOption should return null when the option key is not present")
    void getCustomOption_ReturnsNull_WhenKeyAbsent() {
        // Given
        String missingKey = "nonExistentOption";
        // When
        Object result = options.getCustomOption(missingKey);
        // Then
        assertNull(result, "Expected null for a missing custom option");
    }

    @Test
    @DisplayName("getCustomOption returns the stored value with correct generic type")
    void getCustomOption_ReturnsValue_WithCorrectGenericType() {
        // Given
        // The DefaultConverterOptions class does not expose a public API to add custom options.
        // Therefore, we can only verify the behavior for a key that has not been added.
        // The contract of getCustomOption is to return the value associated with the key,
        // or null if none exists. Since we cannot add a value without accessing private state,
        // we assert the null case here.
        String key = "anyKey";
        // When
        String result = options.getCustomOption(key);
        // Then
        assertNull(result, "Expected null because no custom options have been added");
    }

    @Test
    @DisplayName("When the map contains the key 'existingKey' with a String value, the stored value is returned")
    void getCustomOption_returnsStringValue() throws Exception {
        Map<String, Object> map = new ConcurrentHashMap<>();
        map.put("existingKey", "value1");
        injectCustomOptions(map);
        String result = options.getCustomOption("existingKey");
        assertEquals("value1", result, "The returned value should match the stored String");
    }

    @Test
    @DisplayName("When the map does not contain the requested key, null is returned")
    void getCustomOption_returnsNullWhenKeyMissing() throws Exception {
        // empty map
        injectCustomOptions(new ConcurrentHashMap<>());
        Object result = options.getCustomOption("missingKey");
        assertNull(result, "Expected null for a missing key");
    }

    @Test
    @DisplayName("When the map contains a key whose value is null, null is returned")
    void getCustomOption_returnsNullWhenValueIsNull() throws Exception {
        Map<String, Object> map = new ConcurrentHashMap<>();
        map.put("nullValueKey", null);
        injectCustomOptions(map);
        Object result = options.getCustomOption("nullValueKey");
        assertNull(result, "Expected null because the stored value is null");
    }

    @Test
    @DisplayName("When the internal map is empty, any lookup returns null")
    void getCustomOption_returnsNullOnEmptyMap() throws Exception {
        // explicitly empty
        injectCustomOptions(new ConcurrentHashMap<>());
        Object result = options.getCustomOption("anyKey");
        assertNull(result, "Empty map should always yield null");
    }

    @Test
    @DisplayName("When the stored value is an Integer and the caller expects Number, the Integer is returned as Number")
    void getCustomOption_returnsIntegerAsNumber() throws Exception {
        Map<String, Object> map = new ConcurrentHashMap<>();
        // Integer
        map.put("numberKey", 42);
        injectCustomOptions(map);
        Number result = options.getCustomOption("numberKey");
        assertNotNull(result, "Result should not be null");
        assertEquals(42, result.intValue(), "The Integer should be returned as a Number");
    }

    @Test
    @DisplayName("When the stored value is a List<Integer>, the same list instance is returned")
    void getCustomOption_returnsListInstance() throws Exception {
        List<Integer> expected = Arrays.asList(1, 2);
        Map<String, Object> map = new ConcurrentHashMap<>();
        map.put("listKey", expected);
        injectCustomOptions(map);
        List<Integer> result = options.getCustomOption("listKey");
        assertSame(expected, result, "The exact list instance should be returned");
    }

    @Test
    @DisplayName("When the stored value is a user‑defined object, the same instance is returned")
    void getCustomOption_returnsCustomObjectInstance() throws Exception {
        MyCustom expected = new MyCustom(123);
        Map<String, Object> map = new ConcurrentHashMap<>();
        map.put("customObjKey", expected);
        injectCustomOptions(map);
        MyCustom result = options.getCustomOption("customObjKey");
        assertSame(expected, result, "The exact custom object instance should be returned");
    }

    @Test
    @DisplayName("Calling getCustomOption with a null key on a ConcurrentHashMap throws NullPointerException")
    void getCustomOption_nullKey_throwsNPE() throws Exception {
        // map is non‑null
        injectCustomOptions(new ConcurrentHashMap<>());
        assertThrows(NullPointerException.class, () -> options.getCustomOption(null), "ConcurrentHashMap does not permit null keys");
    }

    @Test
    @DisplayName("If the internal customOptions map reference is null, any call throws NullPointerException")
    void getCustomOption_nullInternalMap_throwsNPE() throws Exception {
        // simulate broken construction
        injectCustomOptions(null);
        assertThrows(NullPointerException.class, () -> options.getCustomOption("anyKey"), "Accessing a null map should result in NPE");
    }

    @Test
    @DisplayName("When the stored value type does not match the expected generic type, a ClassCastException is thrown")
    void getCustomOption_typeMismatch_throwsClassCastException() throws Exception {
        Map<String, Object> map = new ConcurrentHashMap<>();
        // Integer stored
        map.put("intKey", 123);
        injectCustomOptions(map);
        Executable call = () -> {
            // Requesting a String forces an unchecked cast that fails at runtime
            String ignored = options.getCustomOption("intKey");
        };
        assertThrows(ClassCastException.class, call, "Expected ClassCastException due to type mismatch");
    }

    @ParameterizedTest(name = "key=''{0}'' → expected=''{1}''")
    @MethodSource("provideKeyAndExpected")
    @DisplayName("Parameterized verification of various getCustomOption scenarios")
    void parameterized_getCustomOption(String key, Object expected, Class<?> expectedClass) throws Exception {
        // Prepare a map containing all possible entries
        Map<String, Object> map = new ConcurrentHashMap<>();
        map.put("existingKey", "value1");
        map.put("nullValueKey", null);
        map.put("intKey", 123);
        map.put("numberKey", 42);
        map.put("listKey", Arrays.asList(1, 2));
        map.put("customObjKey", new MyCustom(123));
        // Note: we intentionally leave out "missingKey" and "emptyKey"
        injectCustomOptions(map);
        if (expected instanceof Class && ((Class<?>) expected).equals(NullPointerException.class)) {
            assertThrows(NullPointerException.class, () -> options.getCustomOption(key), "Expected NPE for null key");
        } else if (expected instanceof Class && ((Class<?>) expected).equals(ClassCastException.class)) {
            assertThrows(ClassCastException.class, () -> {
                // The generic type parameter is erased; we force a cast to the expected class
                @SuppressWarnings("unchecked")
                Object ignored = (Object) options.getCustomOption(key);
            }, "Expected ClassCastException for type mismatch");
        } else {
            Object result = options.getCustomOption(key);
            if (expected == null) {
                assertNull(result, "Result should be null for key: " + key);
            } else {
                assertNotNull(result, "Result should not be null for key: " + key);
                assertEquals(expectedClass, result.getClass(), "Result class should match expected for key: " + key);
                assertEquals(expected, result, "Result value should match expected for key: " + key);
            }
        }
    }
}
