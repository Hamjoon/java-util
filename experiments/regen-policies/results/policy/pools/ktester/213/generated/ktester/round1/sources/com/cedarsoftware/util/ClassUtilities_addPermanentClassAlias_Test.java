package com.cedarsoftware.util;

import java.lang.reflect.Field;
import java.util.stream.Stream;
// import org.junit.Assert;
// import static org.junit.Assert.*;
// import org.junit.runner.RunWith;
// import org.junit.runners.Parameterized;
// import org.junit.runners.Parameterized.Parameters;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

// @ExtendWith(MockitoExtension.class)
class ClassUtilities_addPermanentClassAlias_Test {

    /**
     * Reflection handle to the private static {@code nameToClass} map inside {@link ClassUtilities}.
     */
    private static Field nameToClassField;

    /**
     * The map instance obtained via reflection (shared across all tests).
     */
//     private static Map<String, Class<?>> nameToClassMap;

    //     @RunWith(Parameterized.class)
    public static class BasicFunctionalityParameterized {

        private final Class<?> clazz;

        private final String alias;

        private final Class<?> expectedClass;

        private final String description;

        public BasicFunctionalityParameterized(Class<?> clazz, String alias, Class<?> expectedClass, String description) {
            this.clazz = clazz;
            this.alias = alias;
            this.expectedClass = expectedClass;
            this.description = description;
        }
        //         @Parameters(name = "{index}: {3}")
        //         public static Collection<Object[]> data() {
        //             return Arrays.asList(new Object[][]{
        //                     // normal addition
        //                     {String.class, "MyStringAlias", String.class, "Adds a normal alias for a non‑null class."},
        //                     // overwrite existing alias
        //                     {Long.class, "NumberAlias", Long.class, "Adds an alias that already exists – new class should replace old mapping."},
        //                     // same class added again (idempotent)
        //                     {String.class, "MyStringAlias", String.class, "Adds the same alias with the same class again – mapping stays unchanged."}
        //             });
        //         }
        //         @org.junit.Test
        //         public void testBasicFunctionality() {
        //             ClassUtilities.addPermanentClassAlias(clazz, alias);
        //             assertTrue("Alias should be present after addition – " + description, nameToClassMap.containsKey(alias));
        //             assertEquals("Mapped class should match expected – " + description, expectedClass, nameToClassMap.get(alias));
        //         }
    }

    //     @RunWith(Parameterized.class)
    public static class NullParameterParameterized {

        private final Class<?> clazz;

        private final String alias;

        private final String description;

        public NullParameterParameterized(Class<?> clazz, String alias, String description) {
            this.clazz = clazz;
            this.alias = alias;
            this.description = description;
        }
        //         @Parameters(name = "{index}: {2}")
        //         public static Collection<Object[]> data() {
        //             return Arrays.asList(new Object[][]{
        //                     {null, "NullClassAlias", "Null class argument should cause NullPointerException."},
        //                     {String.class, null, "Null alias argument should cause NullPointerException."},
        //                     {null, null, "Both arguments null – method should still throw NullPointerException."}
        //             });
        //         }
        //         @org.junit.Test
        //         public void testNullParameters() {
        //             try {
        //                 ClassUtilities.addPermanentClassAlias(clazz, alias);
        //                 fail(description);
        //             } catch (NullPointerException ignored) {
        //                 // expected
        //             }
        //             assertTrue("Map should remain empty when NPE is thrown – " + description, nameToClassMap.isEmpty());
        //         }
    }

    //     @RunWith(Parameterized.class)
    public static class ClassNameAliasParameterized {

        private final Class<?> clazz;

        private final String alias;

        private final String description;

        public ClassNameAliasParameterized(Class<?> clazz, String alias, String description) {
            this.clazz = clazz;
            this.alias = alias;
            this.description = description;
        }
        //         @Parameters(name = "{index}: {2}")
        //         public static Collection<Object[]> data() {
        //             return Arrays.asList(new Object[][]{
        //                     {java.time.LocalDate.class, "java.time.LocalDate", "Using the fully qualified class name as the alias."},
        //                     {java.time.LocalDate.class, "LocalDate", "Using the simple class name as the alias."}
        //             });
        //         }
        //         @org.junit.Test
        //         public void testClassNameAlias() {
        //             ClassUtilities.addPermanentClassAlias(clazz, alias);
        //             assertTrue("Alias should be present – " + description, nameToClassMap.containsKey(alias));
        //             assertEquals("Mapping should point to the correct class – " + description, clazz, nameToClassMap.get(alias));
        //         }
    }

    @BeforeAll
    static void setupBeforeAll() throws Exception {
        // Obtain the private static field and make it accessible once for the whole suite.
        nameToClassField = ClassUtilities.class.getDeclaredField("nameToClass");
        nameToClassField.setAccessible(true);
        @SuppressWarnings("unchecked")
//         Map<String, Class<?>> map = (Map<String, Class<?>>) nameToClassField.get(null);
        nameToClassMap = map;
    }

    @BeforeEach
    void setupBeforeEach() {
        // Ensure a clean state before each test.
        nameToClassMap.clear();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clean up after each test to avoid cross‑test interference.
        nameToClassMap.clear();
    }

    private static Stream<Arguments> basicFunctionalityProvider() {
        return Stream.of(Arguments.of(String.class, "MyStringAlias", String.class, "Adds a normal alias for a non‑null class."), Arguments.of(Long.class, "NumberAlias", Long.class, "Adds an alias that already exists – new class should replace old mapping."), Arguments.of(String.class, "MyStringAlias", String.class, "Adds the same alias with the same class again – mapping stays unchanged."));
    }

    private static Stream<Arguments> nullParameterProvider() {
        return Stream.of(Arguments.of(null, "NullClassAlias", "Null class argument should cause NullPointerException."), Arguments.of(String.class, null, "Null alias argument should cause NullPointerException."), Arguments.of(null, null, "Both arguments null – method should still throw NullPointerException."));
    }

    private static Stream<Arguments> classNameAliasProvider() {
        return Stream.of(Arguments.of(java.time.LocalDate.class, "java.time.LocalDate", "Using the fully qualified class name as the alias."), Arguments.of(java.time.LocalDate.class, "LocalDate", "Using the simple class name as the alias."));
    }

    @Test
    @DisplayName("addPermanentClassAlias stores the alias → class mapping")
    void testAddPermanentClassAliasAddsMapping() {
        Class<?> clazz = String.class;
        String alias = "MyStringAlias";
        ClassUtilities.addPermanentClassAlias(clazz, alias);
        assert nameToClassMap.containsKey(alias) : "The alias should be present in the internal map after adding.";
        assert clazz.equals(nameToClassMap.get(alias)) : "The class associated with the alias should be the one that was added.";
    }

    @Test
    @DisplayName("addPermanentClassAlias overwrites an existing alias")
    void testAddPermanentClassAliasOverridesExisting() {
        String alias = "NumberAlias";
        ClassUtilities.addPermanentClassAlias(Integer.class, alias);
        assert Integer.class.equals(nameToClassMap.get(alias));
        ClassUtilities.addPermanentClassAlias(Long.class, alias);
        assert Long.class.equals(nameToClassMap.get(alias)) : "Adding the same alias again should replace the previous class mapping.";
    }

    @Test
    @DisplayName("addPermanentClassAlias throws NPE when class argument is null")
    void testAddPermanentClassAliasNullClass() {
        String alias = "nullClassAlias";
        try {
            ClassUtilities.addPermanentClassAlias(null, alias);
            assert false : "Expected NullPointerException for null class argument.";
        } catch (NullPointerException ignored) {
            // expected
        }
    }

    @Test
    @DisplayName("addPermanentClassAlias throws NPE when alias argument is null")
    void testAddPermanentClassAliasNullAlias() {
        Class<?> clazz = Integer.class;
        try {
            ClassUtilities.addPermanentClassAlias(clazz, null);
            assert false : "Expected NullPointerException for null alias argument.";
        } catch (NullPointerException ignored) {
            // expected
        }
    }

    @ParameterizedTest(name = "{index}: add alias ''{1}'' for class {0}")
    @MethodSource("basicFunctionalityProvider")
    @DisplayName("addPermanentClassAlias – basic functionality")
    void testAddPermanentClassAliasBasicFunctionality(Class<?> clazz, String alias, Class<?> expectedClass, String description) {
        ClassUtilities.addPermanentClassAlias(clazz, alias);
        assert nameToClassMap.containsKey(alias) : "Alias should be present after addition – " + description;
        assert expectedClass.equals(nameToClassMap.get(alias)) : "Mapped class should match expected – " + description;
    }

    @ParameterizedTest(name = "{index}: clazz={0}, alias={1}")
    @MethodSource("nullParameterProvider")
    @DisplayName("addPermanentClassAlias – null parameters should throw NPE")
    void testAddPermanentClassAliasNullParameters(Class<?> clazz, String alias, String description) {
        try {
            ClassUtilities.addPermanentClassAlias(clazz, alias);
            assert false : description;
        } catch (NullPointerException ignored) {
            // expected
        }
        assert nameToClassMap.isEmpty() : "Map should remain empty when NPE is thrown – " + description;
    }

    @Test
    @DisplayName("addPermanentClassAlias – empty alias string is accepted")
    void testAddPermanentClassAliasEmptyAlias() {
        ClassUtilities.addPermanentClassAlias(Boolean.class, "");
        assert nameToClassMap.containsKey("") : "Empty string should be a legal key and be present in the map.";
        assert Boolean.class.equals(nameToClassMap.get(""));
    }

    @Test
    @DisplayName("addPermanentClassAlias – whitespace alias is stored as‑is")
    void testAddPermanentClassAliasWhitespaceAlias() {
        String whitespace = "   ";
        ClassUtilities.addPermanentClassAlias(java.util.Date.class, whitespace);
        assert nameToClassMap.containsKey(whitespace) : "Whitespace alias should be accepted and stored.";
        assert java.util.Date.class.equals(nameToClassMap.get(whitespace));
    }

    @Test
    @DisplayName("addPermanentClassAlias – primitive class can be aliased")
    void testAddPermanentClassAliasPrimitiveClass() {
        ClassUtilities.addPermanentClassAlias(int.class, "primitiveInt");
        assert nameToClassMap.containsKey("primitiveInt") : "Alias for primitive class should be stored.";
        assert int.class.equals(nameToClassMap.get("primitiveInt"));
    }

    @ParameterizedTest(name = "{index}: alias=''{1}''")
    @MethodSource("classNameAliasProvider")
    @DisplayName("addPermanentClassAlias – alias equals class name")
    void testAddPermanentClassAliasClassNameAlias(Class<?> clazz, String alias, String description) {
        ClassUtilities.addPermanentClassAlias(clazz, alias);
        assert nameToClassMap.containsKey(alias) : "Alias should be present – " + description;
        assert clazz.equals(nameToClassMap.get(alias)) : "Mapping should point to the correct class – " + description;
    }

    @Test
    @DisplayName("addPermanentClassAlias – multiple distinct aliases coexist")
    void testAddPermanentClassAliasMultipleDistinctAliases() {
        ClassUtilities.addPermanentClassAlias(String.class, "AliasOne");
        assert String.class.equals(nameToClassMap.get("AliasOne"));
        ClassUtilities.addPermanentClassAlias(Integer.class, "AliasTwo");
        assert Integer.class.equals(nameToClassMap.get("AliasTwo"));
        assert nameToClassMap.size() == 2 : "After adding two distinct aliases the map size should be 2.";
        assert nameToClassMap.containsKey("AliasOne");
        assert nameToClassMap.containsKey("AliasTwo");
    }
}