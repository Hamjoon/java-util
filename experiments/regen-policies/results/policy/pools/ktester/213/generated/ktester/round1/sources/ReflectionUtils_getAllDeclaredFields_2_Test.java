package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.security.Permission;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getAllDeclaredFields_2_Test {

    /* -------------------------------------------------------------
     *  Test fixtures – simple class hierarchy and an enum to verify
     *  the filtering logic inside ReflectionUtils.getAllDeclaredFields.
     *  ------------------------------------------------------------- */
    static class SuperClass {

        private int superField;

        // should be filtered out
        public static int staticSuper;
    }

    static class SubClass extends SuperClass {

        protected String subField;

        // should be filtered out
        private static String staticSub;
    }

    class EmptyClass {
    }

    /* -------------------------------------------------------------
     *  JUnit lifecycle hooks – no external resources are used, but
     *  we clear the internal caches of ReflectionUtils to guarantee
     *  test isolation.
     * ------------------------------------------------------------- */
    @BeforeAll
    static void setupBeforeAll() {
        // Reset any internal caches that might have been populated
        ReflectionUtils.setClassFieldsCache(new ConcurrentHashMap<>());
        ReflectionUtils.setClassAnnotationCache(new ConcurrentHashMap<>());
        ReflectionUtils.setConstructorCache(new ConcurrentHashMap<>());
        ReflectionUtils.setMethodCache(new ConcurrentHashMap<>());
        ReflectionUtils.setMethodAnnotationCache(new ConcurrentHashMap<>());
        ReflectionUtils.setSortedConstructorsCache(new ConcurrentHashMap<>());
        ReflectionUtils.setFieldCache(new ConcurrentHashMap<>());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Clear caches again for good measure
        ReflectionUtils.setClassFieldsCache(new ConcurrentHashMap<>());
    }

    /**
     * Supplies the data for {@link #testDefaultFieldFilter(String, String, String)}.
     */
    private static Stream<Arguments> defaultFilterProvider() {
        return Stream.of(// SimpleClass – one static, one instance field
        Arguments.of("com.cedarsoftware.util.ReflectionUtilsTestClasses.SimpleClass", "[\"instanceField\"]", "SimpleClass has one static field and one instance field. The static field should be filtered out."), // EnumWithInternal – synthetic enum fields only
        Arguments.of("com.cedarsoftware.util.ReflectionUtilsTestClasses.EnumWithInternal", "[]", "EnumWithInternal declares the synthetic fields \"internal\" and \"ENUM$VALUES\" which the predicate excludes."), // CustomEnum – user‑defined instance field plus filtered enum fields
        Arguments.of("com.cedarsoftware.util.ReflectionUtilsTestClasses.CustomEnum", "[\"customField\"]", "CustomEnum has a regular instance field \"customField\" that must survive the filter."), // GroovyMetaClassHolder – metaClass of type groovy.lang.MetaClass must be filtered
        Arguments.of("com.cedarsoftware.util.ReflectionUtilsTestClasses.GroovyMetaClassHolder", "[]", "GroovyMetaClassHolder contains a field named \"metaClass\" of type groovy.lang.MetaClass, which is filtered."), // EnumSubclassWithHashOrdinal – inherits Enum, declares hash/ordinal (filtered) and extra
        Arguments.of("com.cedarsoftware.util.ReflectionUtilsTestClasses.EnumSubclassWithHashOrdinal", "[\"extra\"]", "EnumSubclassWithHashOrdinal extends Enum and declares its own fields \"hash\", \"ordinal\" (filtered) and a normal field \"extra\"."), // EmptyClass – no fields at all
        Arguments.of("com.cedarsoftware.util.ReflectionUtilsTestClasses.EmptyClass", "[]", "EmptyClass declares no fields; the method should return an empty list."), // Null class argument – should raise NPE
        Arguments.of(null, "NullPointerException", "Passing a null Class reference should result in a NullPointerException."), // Security manager restriction – expect SecurityException
        Arguments.of("com.cedarsoftware.util.ReflectionUtilsTestClasses.SimpleClass", "SecurityException", "Custom SecurityManager denies reflective access, causing a SecurityException."));
    }

    /**
     * Parses the JSON‑style expected string into a list of field names.
     */
    private static List<String> parseExpectedNames(String jsonArray) {
        // Remove surrounding brackets and possible whitespace
        String trimmed = jsonArray.trim();
        if (trimmed.equals("[]")) {
            return Collections.emptyList();
        }
        // Example: ["instanceField","other"]
        // strip [ ]
        trimmed = trimmed.substring(1, trimmed.length() - 1);
        String[] parts = trimmed.split(",");
        List<String> result = new ArrayList<>();
        for (String part : parts) {
            // Remove surrounding quotes and whitespace
            result.add(part.replaceAll("^\\s*\"|\"\\s*$", ""));
        }
        return result;
    }

    @Test
    void testFieldsFromSuperAndSubClassAreReturnedAndStaticFieldsAreFiltered() {
        List<Field> fields = ReflectionUtils.getAllDeclaredFields(SubClass.class);
        // Expected instance fields
        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("superField")), "Should contain inherited instance field 'superField'");
        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("subField")), "Should contain declared instance field 'subField'");
        // Static fields must be filtered out
        assertFalse(fields.stream().anyMatch(f -> f.getName().equals("staticSuper")), "Static field 'staticSuper' must be filtered out");
        assertFalse(fields.stream().anyMatch(f -> f.getName().equals("staticSub")), "Static field 'staticSub' must be filtered out");
    }

    @Test
    void testEnumFieldsAreFilteredAccordingToDEFAULT_FIELD_FILTER() {
        List<Field> enumFields = ReflectionUtils.getAllDeclaredFields(SampleEnum.class);
        // The user‑declared field should be present
        assertTrue(enumFields.stream().anyMatch(f -> f.getName().equals("value")), "Enum should expose its own instance field 'value'");
        // Synthetic/enum‑specific fields must be filtered out
        assertFalse(enumFields.stream().anyMatch(f -> f.getName().equals("ENUM$VALUES")), "Synthetic field 'ENUM$VALUES' must be filtered out");
        assertFalse(enumFields.stream().anyMatch(f -> f.getName().equals("hash")), "Enum field 'hash' must be filtered out");
        assertFalse(enumFields.stream().anyMatch(f -> f.getName().equals("ordinal")), "Enum field 'ordinal' must be filtered out");
    }

    @Test
    void testGetAllDeclaredFieldsReturnsEmptyListForClassWithNoInstanceFields() {
        class EmptyClass {
        }
        List<Field> fields = ReflectionUtils.getAllDeclaredFields(EmptyClass.class);
        assertNotNull(fields, "Result should never be null");
        assertTrue(fields.isEmpty(), "Class without instance fields should yield an empty list");
    }

    /* -------------------------------------------------------------
     *  1. Default field‑filter group – covers many different class shapes.
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("defaultFilterProvider")
    void testDefaultFieldFilter(String className, String expected, String description) throws Exception {
        // Load the class under test
        Class<?> clazz = className == null ? null : Class.forName(className);
        // Expected result can be a list of field names or an exception name
        if ("NullPointerException".equals(expected) || "SecurityException".equals(expected)) {
            Class<? extends Throwable> exceptionClass = Class.forName("java.lang." + expected).asSubclass(Throwable.class);
            assertThrows(exceptionClass, () -> ReflectionUtils.getAllDeclaredFields(clazz));
        } else {
            List<String> expectedNames = parseExpectedNames(expected);
            List<Field> actualFields = ReflectionUtils.getAllDeclaredFields(clazz);
            List<String> actualNames = new ArrayList<>();
            for (Field f : actualFields) {
                actualNames.add(f.getName());
            }
            // Order is not guaranteed – compare as unordered collections
            assertEquals(new HashSet<>(expectedNames), new HashSet<>(actualNames), () -> "Fields for " + className + " do not match expectation: " + description);
        }
    }

    /* -------------------------------------------------------------
     *  2. Inheritance handling – super‑class fields must be included.
     * ------------------------------------------------------------- */
    @Test
    void testInheritanceHandling() {
        List<Field> fields = ReflectionUtils.getAllDeclaredFields(com.cedarsoftware.util.ReflectionUtilsTestClasses.SubClassWithSuper.class);
        List<String> names = new ArrayList<>();
        for (Field f : fields) {
            names.add(f.getName());
        }
        assertTrue(names.containsAll(Arrays.asList("superInstanceField", "subInstanceField")), "Both superclass and subclass instance fields must be present");
        // static fields are filtered out – ensure they are not present
        assertFalse(names.contains("superStaticField"));
        assertFalse(names.contains("subStaticField"));
    }

    /* -------------------------------------------------------------
     *  3. Non‑enum class with fields named 'hash' and 'ordinal' – should be kept.
     * ------------------------------------------------------------- */
    @Test
    void testNonEnumHashOrdinalFieldsAreKept() {
        List<Field> fields = ReflectionUtils.getAllDeclaredFields(com.cedarsoftware.util.ReflectionUtilsTestClasses.ClassWithHashOrdinalNames.class);
        List<String> names = new ArrayList<>();
        for (Field f : fields) {
            names.add(f.getName());
        }
        assertTrue(names.containsAll(Arrays.asList("hash", "ordinal")), "Non‑enum class fields named 'hash' and 'ordinal' must not be filtered");
    }

    /* -------------------------------------------------------------
     *  4. Inner class synthetic field – default filter does NOT exclude it.
     * ------------------------------------------------------------- */
    @Test
    void testInnerClassSyntheticFieldIsReturned() {
        List<Field> fields = ReflectionUtils.getAllDeclaredFields(com.cedarsoftware.util.ReflectionUtilsTestClasses.OuterClass.InnerClass.class);
        List<String> names = new ArrayList<>();
        for (Field f : fields) {
            names.add(f.getName());
        }
        assertTrue(names.contains("this$0"), "Synthetic field 'this$0' should be present");
        assertTrue(names.contains("innerField"), "User‑declared field 'innerField' should be present");
    }

    /* -------------------------------------------------------------
     *  5. Field named metaClass with wrong type – must NOT be filtered.
     * ------------------------------------------------------------- */
    @Test
    void testMetaClassFieldWithWrongTypeIsKept() {
        List<Field> fields = ReflectionUtils.getAllDeclaredFields(com.cedarsoftware.util.ReflectionUtilsTestClasses.MetaClassWrongType.class);
        List<String> names = new ArrayList<>();
        for (Field f : fields) {
            names.add(f.getName());
        }
        assertTrue(names.contains("metaClass"), "Field named 'metaClass' with type other than groovy.lang.MetaClass must be kept");
    }

    /* -------------------------------------------------------------
     *  6. Class with only static final fields – all must be filtered out.
     * ------------------------------------------------------------- */
    @Test
    void testStaticFinalFieldFiltering() {
        List<Field> fields = ReflectionUtils.getAllDeclaredFields(com.cedarsoftware.util.ReflectionUtilsTestClasses.ClassWithStaticFinal.class);
        assertTrue(fields.isEmpty(), "ClassWithStaticFinal declares only static final fields, result must be empty");
    }

    /* -------------------------------------------------------------
     *  7. Non‑enum class with a field named 'internal' – must be kept.
     * ------------------------------------------------------------- */
    @Test
    void testNonEnumInternalFieldIsKept() {
        List<Field> fields = ReflectionUtils.getAllDeclaredFields(com.cedarsoftware.util.ReflectionUtilsTestClasses.ClassWithInternalField.class);
        List<String> names = new ArrayList<>();
        for (Field f : fields) {
            names.add(f.getName());
        }
        assertTrue(names.contains("internal"), "Non‑enum field named 'internal' should not be filtered");
    }

    /* -------------------------------------------------------------
     *  8. Class with only static fields of various visibilities – all filtered.
     * ------------------------------------------------------------- */
    @Test
    void testClassWithOnlyStaticFieldsReturnsEmpty() {
        List<Field> fields = ReflectionUtils.getAllDeclaredFields(com.cedarsoftware.util.ReflectionUtilsTestClasses.ClassOnlyStatic.class);
        assertTrue(fields.isEmpty(), "ClassOnlyStatic declares only static fields; the returned list must be empty");
    }

    /* -------------------------------------------------------------
     *  9. SecurityManager restriction – ensure the SecurityException propagates.
     * ------------------------------------------------------------- */
    @Test
    void testSecurityManagerRestrictionPropagatesException() throws Exception {
        // Preserve the original security manager to restore later
        SecurityManager original = System.getSecurityManager();
        // Install a restrictive SecurityManager that throws on any reflective permission
        System.setSecurityManager(new SecurityManager() {

            @Override
            public void checkPermission(Permission perm) {
                // Disallow reflective access
                if (perm instanceof java.lang.reflect.ReflectPermission) {
                    throw new SecurityException("Reflective access denied by test SecurityManager");
                }
            }

            @Override
            public void checkPermission(Permission perm, Object context) {
                checkPermission(perm);
            }
        });
        try {
            Class<?> clazz = Class.forName("com.cedarsoftware.util.ReflectionUtilsTestClasses.SimpleClass");
            assertThrows(SecurityException.class, () -> ReflectionUtils.getAllDeclaredFields(clazz), "A SecurityException should be thrown when reflective access is denied");
        } finally {
            // Restore the original security manager regardless of test outcome
            System.setSecurityManager(original);
        }
    }
}
