package com.cedarsoftware.util;

import java.util.*;
import java.util.stream.Stream;

// import org.junit.After;
// import org.junit.Before;
// import org.junit.runner.RunWith;
// import org.junit.runners.Parameterized;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link CaseInsensitiveMap#remove(Object, Object)}.
 */
@ExtendWith(MockitoExtension.class)
class CaseInsensitiveMap_remove_2_Test {

    private CaseInsensitiveMap<String, String> stringMap;
    private CaseInsensitiveMap<Object, String> mixedMap;

    /* -------------------------------------------------
       Parameterized JUnit‑4 inner test classes
       ------------------------------------------------- */
//     @RunWith(Parameterized.class)
    public static class BasicRemovalParameterized {

        private final String key;
        private final String value;
        private final boolean expected;

        private final CaseInsensitiveMap_remove_2_Test outer = new CaseInsensitiveMap_remove_2_Test();

        public BasicRemovalParameterized(String key, String value, boolean expected) {
            this.key = key;
            this.value = value;
            this.expected = expected;
        }

//         @Parameterized.Parameters(name = "{index}: remove({0}, {1}) → {2}")
//         public static Collection<Object[]> data() {
//             return Arrays.asList(new Object[][]{
//                     {"KeyOne", "Value1", true},
//                     {"keyone", "Value1", true},
//                     {"KeyOne", "WrongValue", false},
//                     {"NonExisting", "Anything", false}
//             });
//         }

//         @Before
//         public void setUp() {
//             outer.setupBeforeEach();
//         }

//         @After
//         public void tearDown() {
//             outer.teardownAfterEach();
//         }

//         @org.junit.Test
//         public void test() {
//             boolean result = outer.stringMap.remove(key, value);
//             assertEquals(expected, result);
//         }
    }

//     @RunWith(Parameterized.class)
    public static class NullAndNonStringParameterized {

        private final Object key;
        private final Object value;
        // Boolean or Class<? extends Throwable>
        private final Object expected;

        private final CaseInsensitiveMap_remove_2_Test outer = new CaseInsensitiveMap_remove_2_Test();

        public NullAndNonStringParameterized(Object key, Object value, Object expected) {
            this.key = key;
            this.value = value;
            this.expected = expected;
        }

//         @Parameterized.Parameters(name = "{index}: remove({0}, {1}) → {2}")
//         public static Collection<Object[]> data() {
//             return Arrays.asList(new Object[][]{
//                     {null, null, true},
//                     {null, "SomeValue", false},
//                     {"KeyTwo", null, true},
//                     {"KeyThree", null, false},
//                     {42, "Answer", true},
//                     {99, "Anything", false}
//             });
//         }

//         @Before
//         public void setUp() {
//             outer.setupBeforeEach();
//         }

//         @After
//         public void tearDown() {
//             outer.teardownAfterEach();
//         }

//         @org.junit.Test
//         public void test() {
//             if (expected instanceof Class && Throwable.class.isAssignableFrom((Class<?>) expected)) {
//                 @SuppressWarnings("unchecked")
//                 Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected;
//                 try {
//                     outer.stringMap.remove(key, value);
//                     fail("Expected exception of type " + exClass.getName());
//                 } catch (Throwable t) {
//                     assertTrue("Expected exception of type " + exClass.getName(), exClass.isInstance(t));
//                 }
//             } else {
//                 boolean expectedBool = (Boolean) expected;
//                 boolean result = outer.stringMap.remove(key, value);
//                 assertEquals(expectedBool, result);
//             }
//         }
    }

    /* -------------------------------------------------
       JUnit‑5 lifecycle methods
       ------------------------------------------------- */
    @BeforeEach
    void setupBeforeEach() {
        // Map with String keys – case‑insensitive handling is exercised here
        stringMap = new CaseInsensitiveMap<>();
        stringMap.put("Alpha", "A");
        stringMap.put("Beta", "B");
        stringMap.put("Gamma", "C");
        // additional entries used by the parameterised tests
        stringMap.put("KeyOne", "Value1");
        stringMap.put("KeyTwo", null);
        stringMap.put("KeyThree", "NotNull");
        // Map with mixed (non‑String) keys – should behave like a normal map
        mixedMap = new CaseInsensitiveMap<>();
        mixedMap.put(1, "one");
        mixedMap.put(2, "two");
        mixedMap.put("Key", "value");
        mixedMap.put(42, "Answer");
    }

    @AfterEach
    void teardownAfterEach() {
        stringMap = null;
        mixedMap = null;
    }

    /* -------------------------------------------------
       JUnit‑5 method sources for parameterised tests
       ------------------------------------------------- */
    private static Stream<Arguments> basicRemovalProvider() {
        return Stream.of(
                Arguments.of("KeyOne", "Value1", true),
                Arguments.of("keyone", "Value1", true),
                Arguments.of("KeyOne", "WrongValue", false),
                Arguments.of("NonExisting", "Anything", false)
        );
    }

    private static Stream<Arguments> nullAndNonStringProvider() {
        return Stream.of(
                Arguments.of(null, null, true),
                Arguments.of(null, "SomeValue", false),
                Arguments.of("KeyTwo", null, true),
                Arguments.of("KeyThree", null, false),
                Arguments.of(42, "Answer", true),
                Arguments.of(99, "Anything", false)
        );
    }

    /* -------------------------------------------------
       Regular JUnit‑5 test cases
       ------------------------------------------------- */
//     @org.junit.jupiter.api.Test
//     void removeExistingEntryWithExactCase_ShouldReturnTrueAndRemove() {
//         boolean removed = stringMap.remove("Alpha", "A");
//         assertTrue("remove should return true when key/value match", removed);
//         assertNull(stringMap.get("Alpha"));
//         assertEquals(2, stringMap.size());
//     }

//     @org.junit.jupiter.api.Test
//     void removeExistingEntryWithDifferentKeyCase_ShouldReturnTrueAndRemove() {
//         boolean removed = stringMap.remove("aLpHa", "A");
//         assertTrue("remove should be case‑insensitive for String keys", removed);
//         assertNull(stringMap.get("Alpha"));
//         assertEquals(2, stringMap.size());
//     }

//     @org.junit.jupiter.api.Test
//     void removeExistingEntryWithMismatchedValue_ShouldReturnFalseAndLeaveMapUntouched() {
//         boolean removed = stringMap.remove("Beta", "WRONG");
//         assertFalse("remove should return false when the value does not match", removed);
//         assertEquals("B", stringMap.get("Beta"));
//         assertEquals(3, stringMap.size());
//     }

//     @org.junit.jupiter.api.Test
//     void removeNonExistingKey_ShouldReturnFalse() {
//         boolean removed = stringMap.remove("Delta", "D");
//         assertFalse("remove should return false for a key that does not exist", removed);
//         assertEquals(3, stringMap.size());
//     }

//     @org.junit.jupiter.api.Test
//     void removeWithNullKeyAndValue_ShouldBehaveLikeStandardMap() {
//         stringMap.put(null, null);
//         assertEquals(4, stringMap.size());
//         boolean removed = stringMap.remove(null, null);
//         assertTrue("remove should succeed when both key and value are null", removed);
//         assertNull(stringMap.get(null));
//         assertEquals(3, stringMap.size());
//     }

//     @org.junit.jupiter.api.Test
//     void removeNonStringKey_ShouldBeCaseSensitiveAndDelegateToBackingMap() {
//         boolean removed = mixedMap.remove(1, "one");
//         assertTrue("remove should succeed for non‑String keys", removed);
//         assertNull(mixedMap.get(1));
//         assertEquals(2, mixedMap.size());
// 
//         boolean notRemoved = mixedMap.remove(2, "wrong");
//         assertFalse("remove must return false when value does not match for non‑String key", notRemoved);
//         assertEquals("two", mixedMap.get(2));
//     }

    @ParameterizedTest(name = "{index}: remove({0}, {1}) → {2}")
    @MethodSource("basicRemovalProvider")
    void removeBasicFunctionality(String key, String value, boolean expectedResult) {
        boolean result = stringMap.remove(key, value);
        assertEquals(expectedResult, result,
                () -> String.format("remove(%s,%s) expected %s", key, value, expectedResult));
    }

    @ParameterizedTest(name = "{index}: remove({0}, {1}) → {2}")
    @MethodSource("nullAndNonStringProvider")
    void removeNullAndNonStringHandling(Object key, Object value, Object expected) {
        if (expected instanceof Class && Throwable.class.isAssignableFrom((Class<?>) expected)) {
            @SuppressWarnings("unchecked")
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected;
            assertThrows(exClass, () -> stringMap.remove(key, value));
        } else {
            boolean expectedBool = (Boolean) expected;
            boolean result = stringMap.remove(key, value);
            assertEquals(expectedBool, result,
                    () -> String.format("remove(%s,%s) expected %s", key, value, expectedBool));
        }
    }

    @org.junit.jupiter.api.Test
    void removeWithNullKeyOnHashtable_ShouldThrowNPE() {
        Hashtable<String, String> ht = new Hashtable<>();
        ht.put("Key", "Value");
        CaseInsensitiveMap<String, String> htMap = new CaseInsensitiveMap<>(ht);
        assertThrows(NullPointerException.class, () -> htMap.remove(null, "SomeValue"));
    }

    @org.junit.jupiter.api.Test
    void removeWithNullValueOnHashtable_ShouldThrowNPE() {
        Hashtable<String, String> ht = new Hashtable<>();
        ht.put("Key", "Value");
        CaseInsensitiveMap<String, String> htMap = new CaseInsensitiveMap<>(ht);
        assertThrows(NullPointerException.class, () -> htMap.remove("Key", null));
    }

    @org.junit.jupiter.api.Test
    void removeWithBothNullOnHashtable_ShouldThrowNPE() {
        Hashtable<String, String> ht = new Hashtable<>();
        ht.put("Key", "Value");
        CaseInsensitiveMap<String, String> htMap = new CaseInsensitiveMap<>(ht);
        assertThrows(NullPointerException.class, () -> htMap.remove(null, null));
    }
}