package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DefaultConverterOptions_getCustomOption_Test {

    private static Field customOptionsField;

    private DefaultConverterOptions options;

    private Map<String, Object> customOptionsMap;

    /**
     * Simple POJO used in the “customObj” test case.
     */
    private static final class CustomObject {

        private final int id;

        CustomObject(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof CustomObject))
                return false;
            CustomObject that = (CustomObject) o;
            return id == that.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public String toString() {
            return "CustomObject{id=" + id + '}';
        }
    }

    /**
     * Container for a single test scenario.
     */
    private static final class Scenario {

        // for display in @ParameterizedTest name
        final String description;

        // argument passed to getCustomOption
        final String key;

        // value we expect (null if expecting exception)
        final Object expectedValue;

        // expected exception type (null if none)
        final Class<? extends Throwable> exception;

        // creates the map implementation
        final Supplier<Map<String, Object>> mapSupplier;

        // fills the map before the call
        final Consumer<Map<String, Object>> mapPopulator;

        Scenario(String description, String key, Object expectedValue, Class<? extends Throwable> exception, Supplier<Map<String, Object>> mapSupplier, Consumer<Map<String, Object>> mapPopulator) {
            this.description = description;
            this.key = key;
            this.expectedValue = expectedValue;
            this.exception = exception;
            this.mapSupplier = mapSupplier;
            this.mapPopulator = mapPopulator;
        }
    }

    @BeforeAll
    static void setupBeforeAll() throws NoSuchFieldException {
        // Obtain the private field reference once for all tests
        customOptionsField = DefaultConverterOptions.class.getDeclaredField("customOptions");
        customOptionsField.setAccessible(true);
    }

    @BeforeEach
    void setupBeforeEach() throws IllegalAccessException {
        // Create a fresh instance for each test
        options = new DefaultConverterOptions();
        // Initialise a fresh map (ConcurrentHashMap by default) and inject it
        customOptionsMap = new ConcurrentHashMap<>();
        customOptionsField.set(options, customOptionsMap);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC and ensure isolation
        options = null;
        customOptionsMap = null;
    }

    private static Stream<Arguments> basicScenarios() {
        return Stream.of(// 1. Existing Integer option
        Arguments.of(new Scenario("Retrieves existing Integer option (timeout → 30)", "timeout", 30, null, ConcurrentHashMap::new, m -> m.put("timeout", 30))), // 2. Existing Boolean option
        Arguments.of(new Scenario("Retrieves existing Boolean option (enabled → true)", "enabled", true, null, ConcurrentHashMap::new, m -> m.put("enabled", true))), // 3. Incompatible generic type – should throw ClassCastException
        Arguments.of(new Scenario("Attempts incompatible cast (timeout Integer → String)", "timeout", null, ClassCastException.class, ConcurrentHashMap::new, m -> m.put("timeout", 30))), // 4. Missing key – should return null
        Arguments.of(new Scenario("Missing key returns null", "missingKey", null, null, ConcurrentHashMap::new, m -> {
            /* leave empty */
        })), // 5. Null key with a HashMap (allows null) – should return null
        Arguments.of(new Scenario("Null key with HashMap returns null (no exception)", null, null, null, HashMap::new, m -> {
            /* no entry needed */
        })));
    }

    private static Stream<Arguments> additionalScenarios() {
        return Stream.of(// List<String>
        Arguments.of(new Scenario("Retrieves List<String> stored under key \"list\"", "list", Arrays.asList("alpha", "beta"), null, ConcurrentHashMap::new, m -> m.put("list", Arrays.asList("alpha", "beta")))), // CustomObject
        Arguments.of(new Scenario("Retrieves user‑defined CustomObject", "customObj", new CustomObject(5), null, ConcurrentHashMap::new, m -> m.put("customObj", new CustomObject(5)))), // Explicit null value
        Arguments.of(new Scenario("Key exists but maps to null", "nullValue", null, null, ConcurrentHashMap::new, m -> m.put("nullValue", null))), // Empty string key
        Arguments.of(new Scenario("Empty string key returns Integer 42", "", 42, null, ConcurrentHashMap::new, m -> m.put("", 42))), // Whitespace key
        Arguments.of(new Scenario("Whitespace key returns Boolean true", "   ", true, null, ConcurrentHashMap::new, m -> m.put("   ", true))), // Special characters in key
        Arguments.of(new Scenario("Special‑character key returns exact String", "special!@#", "specialKey", null, ConcurrentHashMap::new, m -> m.put("special!@#", "specialKey"))), // Subclass to super‑type cast (Integer → Number)
        Arguments.of(new Scenario("Integer stored as Number (key \"number\")", "number", 123, null, ConcurrentHashMap::new, m -> m.put("number", 123))), // ArrayList<Integer> as List<Integer>
        Arguments.of(new Scenario("ArrayList<Integer> retrieved as List<Integer>", "arrayList", new ArrayList<>(Arrays.asList(1, 2, 3)), null, ConcurrentHashMap::new, m -> m.put("arrayList", new ArrayList<>(Arrays.asList(1, 2, 3))))), // Unmodifiable map wrapper
        Arguments.of(new Scenario("Unmodifiable map still returns stored value", "unmodifiable", "fixed", null, () -> Collections.unmodifiableMap(new HashMap<>()), m -> m.put("unmodifiable", "fixed"))), // Null key with ConcurrentHashMap – should throw NPE
        Arguments.of(new Scenario("Null key on ConcurrentHashMap propagates NullPointerException", null, null, NullPointerException.class, ConcurrentHashMap::new, m -> {
            /* no entry needed */
        })));
    }

    private static Stream<Arguments> exceptionScenarios() {
        return Stream.of(// Empty string key on ConcurrentHashMap – NPE (as per description)
        Arguments.of(new Scenario("Empty string key on ConcurrentHashMap triggers NullPointerException", "", null, NullPointerException.class, ConcurrentHashMap::new, m -> {
            /* map stays empty */
        })), // Wrong cast on existing Integer value
        Arguments.of(new Scenario("ClassCastException when casting Integer to String", "someKey", null, ClassCastException.class, ConcurrentHashMap::new, m -> m.put("someKey", 99))));
    }

    @Test
    void getCustomOption_returnsStoredValueWithCorrectType() {
        // given
        String key = "myOption";
        Integer expectedValue = 42;
        customOptionsMap.put(key, expectedValue);
        // when
        Integer actual = options.getCustomOption(key);
        // then
        assertNotNull(actual, "Returned value should not be null");
        assertEquals(expectedValue, actual, "Returned value should match the stored value");
    }

    @Test
    void getCustomOption_returnsNullWhenOptionNotPresent() {
        // given
        String missingKey = "nonExistent";
        // when
        Object result = options.getCustomOption(missingKey);
        // then
        assertNull(result, "Result should be null when the option key is absent");
    }

    @Test
    void getCustomOption_isTypeSafeAtRuntime() {
        // given
        String key = "stringOption";
        String value = "testString";
        customOptionsMap.put(key, value);
        // when
        // Requesting a different type should cause a ClassCastException at runtime
        assertThrows(ClassCastException.class, () -> {
            @SuppressWarnings("unused")
            Integer wrongType = options.getCustomOption(key);
        });
    }

    @ParameterizedTest(name = "[{index}] {0.description}")
    @MethodSource("basicScenarios")
    void getCustomOption_basicScenarios(Scenario s) throws IllegalAccessException {
        // -----------------------------------------------------------------
        // Arrange – inject the required map implementation and populate it
        // -----------------------------------------------------------------
        Map<String, Object> map = s.mapSupplier.get();
        s.mapPopulator.accept(map);
        customOptionsField.set(options, map);
        // -----------------------------------------------------------------
        // Act & Assert
        // -----------------------------------------------------------------
        if (s.exception != null) {
            // For ClassCastException we need to perform an explicit cast in the lambda
            assertThrows(s.exception, () -> {
                // The unchecked cast is forced to the type that will cause the exception
                @SuppressWarnings("unchecked")
                String bogus = options.getCustomOption(s.key);
                // The above line is enough – if it returns without exception the test fails
            });
        } else {
            Object result = options.getCustomOption(s.key);
            assertEquals(s.expectedValue, result, () -> "Expected value for key '" + s.key + "' does not match");
        }
    }

    @ParameterizedTest(name = "[{index}] {0.description}")
    @MethodSource("additionalScenarios")
    void getCustomOption_additionalScenarios(Scenario s) throws IllegalAccessException {
        // -----------------------------------------------------------------
        // Arrange – create the map (may be unmodifiable) and inject it
        // -----------------------------------------------------------------
        Map<String, Object> map = s.mapSupplier.get();
        s.mapPopulator.accept(map);
        customOptionsField.set(options, map);
        // -----------------------------------------------------------------
        // Act & Assert
        // -----------------------------------------------------------------
        if (s.exception != null) {
            assertThrows(s.exception, () -> options.getCustomOption(s.key));
        } else {
            Object result = options.getCustomOption(s.key);
            assertEquals(s.expectedValue, result, () -> "Unexpected value for key '" + s.key + "'");
        }
    }

    @ParameterizedTest(name = "[{index}] {0.description}")
    @MethodSource("exceptionScenarios")
    void getCustomOption_exceptionScenarios(Scenario s) throws IllegalAccessException {
        // Arrange
        Map<String, Object> map = s.mapSupplier.get();
        s.mapPopulator.accept(map);
        customOptionsField.set(options, map);
        // Act & Assert
        assertThrows(s.exception, () -> {
            // Force a cast that will cause the expected exception
            @SuppressWarnings("unchecked")
            String bogus = options.getCustomOption(s.key);
        });
    }
}
