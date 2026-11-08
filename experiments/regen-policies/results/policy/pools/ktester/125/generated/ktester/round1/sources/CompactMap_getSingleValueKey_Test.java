package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CompactMap_getSingleValueKey_Test {

    /**
     * Simple user‑defined key type used in one of the exception scenarios.
     */
    private static class CustomKey {
        // No fields – only its type matters for the test
    }

    /**
     * Supplies all test cases defined in the original JSON matrix.
     *
     * Each {@link Arguments} consists of:
     * <ul>
     *   <li>A {@link Supplier} that creates a fresh {@link CompactMap} instance
     *       with the required generic type and optional pre‑population.</li>
     *   <li>The {@link ExpectedResult} for that instance.</li>
     *   <li>A textual description (used only for readability of the test report).</li>
     * </ul>
     */
//     private static Stream<Arguments> getSingleValueKeyCases() {
//         return Stream.of(// -----------------------------------------------------------------
//         // Group: getSingleValueKey
//         // -----------------------------------------------------------------
//         Arguments.of((Supplier<CompactMap<?, ?>>) CompactMap::new, ExpectedResult.ID, "CompactMap<Object, Object> – default settings should return \"id\""), Arguments.of((Supplier<CompactMap<?, ?>>) () -> new CompactMap<String, String>(), ExpectedResult.ID, "CompactMap<String, String> – generic type is String, should return \"id\""), Arguments.of((Supplier<CompactMap<?, ?>>) () -> new CompactMap<Integer, Integer>(), ExpectedResult.CLASS_CAST_EXCEPTION, "CompactMap<Integer, Integer> – incompatible generic type, expect ClassCastException"), Arguments.of((Supplier<CompactMap<?, ?>>) () -> {
//             CompactMap<String, Integer> m = new CompactMap<>();
//             m.put("a", 1);
//             m.put("b", 2);
//             return m;
//         }, ExpectedResult.ID, "CompactMap<String, Integer> with entries – still returns \"id\""), // -----------------------------------------------------------------
//         // Group: getSingleValueKey_additional
//         // -----------------------------------------------------------------
//         Arguments.of((Supplier<CompactMap<?, ?>>) () -> new CompactMap(), ExpectedResult.ID, "Raw CompactMap (no generics) – returns \"id\" as Object"), Arguments.of((Supplier<CompactMap<?, ?>>) () -> new CompactMap<CharSequence, String>(), ExpectedResult.ID, "CompactMap<CharSequence, String> – CharSequence is compatible, returns \"id\""), Arguments.of((Supplier<CompactMap<?, ?>>) () -> new CompactMap<Number, Double>(), ExpectedResult.CLASS_CAST_EXCEPTION, "CompactMap<Number, Double> – incompatible, expect ClassCastException"), Arguments.of((Supplier<CompactMap<?, ?>>) () -> new CompactMap<CustomKey, String>(), ExpectedResult.CLASS_CAST_EXCEPTION, "CompactMap<CustomKey, String> – custom key type, expect ClassCastException"), Arguments.of((Supplier<CompactMap<?, ?>>) () -> {
//             CompactMap<String, Integer> m = new CompactMap<>();
//             // call twice and compare references in the test method
//             return m;
//         }, ExpectedResult.SAME_INSTANCE, "Two successive calls on same instance must return the same String object"), Arguments.of((Supplier<CompactMap<?, ?>>) () -> {
//             CompactMap<String, Integer> m = new CompactMap<>();
//             m.put("x", 10);
//             // clear after population
//             m.clear();
//             return m;
//         }, ExpectedResult.ID, "CompactMap<String, Integer> populated then cleared – still returns \"id\""), // -----------------------------------------------------------------
//         // Group: getSingleValueKey_exception_additional
//         // -----------------------------------------------------------------
//         Arguments.of((Supplier<CompactMap<?, ?>>) () -> new CompactMap<List, String>(), ExpectedResult.CLASS_CAST_EXCEPTION, "CompactMap<List, String> – List is not compatible, expect ClassCastException"), Arguments.of((Supplier<CompactMap<?, ?>>) () -> new CompactMap<Map, Integer>(), ExpectedResult.CLASS_CAST_EXCEPTION, "CompactMap<Map, Integer> – Map is not compatible, expect ClassCastException"), Arguments.of((Supplier<CompactMap<?, ?>>) () -> new CompactMap<Boolean, String>(), ExpectedResult.CLASS_CAST_EXCEPTION, "CompactMap<Boolean, String> – Boolean is not compatible, expect ClassCastException"), Arguments.of((Supplier<CompactMap<?, ?>>) () -> new CompactMap<Collection, Double>(), ExpectedResult.CLASS_CAST_EXCEPTION, "CompactMap<Collection, Double> – Collection is not compatible, expect ClassCastException"));
//     }

    @Test
    void defaultSingleValueKey_isId() {
        // CompactMap with the default configuration (String keys)
        CompactMap<String, Object> map = new CompactMap<>();
        // The protected method is visible because this test resides in the same package.
        String key = map.getSingleValueKey();
        assertEquals("id", key, "The default single‑value key should be the literal \"id\".");
    }

    @Test
    void overriddenSingleValueKey_isReturnedWhenSubclassed() {
        // Create an anonymous subclass that overrides the protected method.
        CompactMap<String, Object> map = new CompactMap<String, Object>() {

            @Override
            protected String getSingleValueKey() {
                return "customKey";
            }
        };
        String key = map.getSingleValueKey();
        assertEquals("customKey", key, "A subclass that overrides getSingleValueKey() should return the overridden value.");
    }

    @Test
    void getSingleValueKey_withIncompatibleKeyType_throwsClassCastException() {
        // The map is declared with Integer keys, but the implementation returns a String.
        CompactMap<Integer, Object> map = new CompactMap<Integer, Object>() {
        };
        // The cast performed inside getSingleValueKey() will fail at runtime.
        assertThrows(ClassCastException.class, map::getSingleValueKey, "Calling getSingleValueKey() when the generic key type is not String should throw ClassCastException.");
    }

    /**
     * Executes each scenario supplied by {@link #getSingleValueKeyCases()}.
     *
     * @param mapSupplier   creates a fresh map instance for the test
     * @param expected      the expected outcome for this scenario
     * @param description   human‑readable description (shown by JUnit)
     */
//     @ParameterizedTest(name = "[{index}] {2}")
//     @MethodSource("getSingleValueKeyCases")
//     void getSingleValueKey_variousScenarios(Supplier<CompactMap<?, ?>> mapSupplier, ExpectedResult expected, String description) {
//         CompactMap<?, ?> map = mapSupplier.get();
//         switch(expected) {
//             case ID ->
//                 {
//                     // The method should return the literal "id"
//                     Object result = map.getSingleValueKey();
//                     assertEquals("id", result, "Expected the default single‑value key \"id\" but got: " + result);
//                 }
//             case SAME_INSTANCE ->
//                 {
//                     // Verify that two invocations return the *same* object reference
//                     Object first = map.getSingleValueKey();
//                     Object second = map.getSingleValueKey();
//                     assertSame(first, second, "Two calls to getSingleValueKey() should return the same object instance");
//                     assertEquals("id", first, "Even when checking reference equality, the value must be the literal \"id\"");
//                 }
//             case CLASS_CAST_EXCEPTION ->
//                 {
//                     // The cast inside the method should fail for the supplied generic type
//                     assertThrows(ClassCastException.class, map::getSingleValueKey, "Expected a ClassCastException for map with key type incompatible with String");
//                 }
//             default ->
//                 fail("Unhandled ExpectedResult: " + expected);
//         }
//     }
}