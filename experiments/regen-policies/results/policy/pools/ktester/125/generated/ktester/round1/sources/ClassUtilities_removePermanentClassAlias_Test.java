package com.cedarsoftware.util;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Collection;
import java.util.Collections;
import java.util.Arrays;
// import org.junit.After;
// import org.junit.AfterClass;
// import org.junit.Before;
// import org.junit.BeforeClass;
// import org.junit.Test;
// import org.junit.runner.RunWith;
// import org.junit.runners.Parameterized;
// import static org.junit.Assert.*;

// @ExtendWith(MockitoExtension.class)
class ClassUtilities_removePermanentClassAlias_Test {

    /**
     * Holds a reference to the private static {@code nameToClass} map in {@link ClassUtilities}.
     */
    private static Map<String, Class<?>> nameToClassMap;

//     @RunWith(Parameterized.class)
    public static class RemoveAliasAmongMultipleTest {

//         @Parameterized.Parameter(0)
        public String aliasUnderTest;

        private static final String OTHER_ALIAS = "other.alias";

//         @BeforeClass
//         public static void init() throws Exception {
//             // reuse the same map reference as the outer test class
//             Field field = ClassUtilities.class.getDeclaredField("nameToClass");
//             field.setAccessible(true);
//             @SuppressWarnings("unchecked")
//             Map<String, Class<?>> map = (Map<String, Class<?>>) field.get(null);
//             nameToClassMap = map;
//         }

//         @Before
//         public void beforeEach() {
//             nameToClassMap.clear();
//             // set up the two distinct aliases
//             ClassUtilities.addPermanentClassAlias(Integer.class, OTHER_ALIAS);
//             ClassUtilities.addPermanentClassAlias(String.class, aliasUnderTest);
//         }

//         @After
//         public void afterEach() {
//             nameToClassMap.clear();
//         }

//         @Parameterized.Parameters(name = "remove alias \"{0}\" that exists among other entries")
//         public static Collection<Object[]> data() {
//             return Arrays.asList(new Object[][] { { "my.alias" }, { "" }, { "   " }, { "spécial-älias_!@#" } });
//         }

//         @Test
//         public void testRemoveAliasAmongMultiple() {
//             // pre‑conditions
//             assertTrue(nameToClassMap.containsKey(aliasUnderTest));
//             assertTrue(nameToClassMap.containsKey(OTHER_ALIAS));
//             // when
//             ClassUtilities.removePermanentClassAlias(aliasUnderTest);
//             // then
//             assertFalse(nameToClassMap.containsKey(aliasUnderTest));
//             assertTrue(nameToClassMap.containsKey(OTHER_ALIAS));
//             assertEquals(1, nameToClassMap.size());
//         }
    }

//     @RunWith(Parameterized.class)
    public static class RemoveEmptyOrWhitespaceAliasTest {

//         @Parameterized.Parameter(0)
        public String alias;

//         @BeforeClass
//         public static void init() throws Exception {
//             Field field = ClassUtilities.class.getDeclaredField("nameToClass");
//             field.setAccessible(true);
//             @SuppressWarnings("unchecked")
//             Map<String, Class<?>> map = (Map<String, Class<?>>) field.get(null);
//             nameToClassMap = map;
//         }

//         @Before
//         public void beforeEach() {
//             nameToClassMap.clear();
//             ClassUtilities.addPermanentClassAlias(Long.class, alias);
//             assertTrue(nameToClassMap.containsKey(alias));
//         }

//         @After
//         public void afterEach() {
//             nameToClassMap.clear();
//         }

//         @Parameterized.Parameters(name = "remove alias \"{0}\" (empty/whitespace) works")
//         public static Collection<Object[]> data() {
//             return Arrays.asList(new Object[][] { { "" }, { "   " } });
//         }

//         @Test
//         public void testRemoveEmptyOrWhitespaceAlias() {
//             // when
//             ClassUtilities.removePermanentClassAlias(alias);
//             // then
//             assertFalse(nameToClassMap.containsKey(alias));
//             assertTrue(nameToClassMap.isEmpty());
//         }
    }

//     @BeforeAll
    static void setupBeforeAll() throws Exception {
        // Obtain the private static field via reflection once for the whole test class.
        Field field = ClassUtilities.class.getDeclaredField("nameToClass");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Class<?>> map = (Map<String, Class<?>>) field.get(null);
        nameToClassMap = map;
    }

//     @BeforeEach
    void setupBeforeEach() {
        // Ensure a clean state before each test.
        nameToClassMap.clear();
    }

//     @AfterEach
    void teardownAfterEach() {
        // Clean up after each test to avoid cross‑test contamination.
        nameToClassMap.clear();
    }

//     @Test
//     @DisplayName("removePermanentClassAlias removes an existing alias")
//     void testRemoveExistingAlias() {
//         // given: an alias is registered
//         String alias = "myStringAlias";
//         ClassUtilities.addPermanentClassAlias(String.class, alias);
//         assertTrue("Pre‑condition: map should contain the alias after registration", nameToClassMap.containsKey(alias));
//         // when: the alias is removed
//         ClassUtilities.removePermanentClassAlias(alias);
//         // then: the map no longer contains the alias
//         assertFalse("Alias should have been removed from the internal map", nameToClassMap.containsKey(alias));
//     }

//     @Test
//     @DisplayName("removePermanentClassAlias on a non‑existent alias does nothing")
//     void testRemoveNonExistingAlias() {
//         // given: map is empty and a non‑existent alias
//         String missingAlias = "doesNotExist";
//         assertFalse("Pre‑condition: map should not contain the missing alias", nameToClassMap.containsKey(missingAlias));
//         // when: removal is attempted
//         try {
//             ClassUtilities.removePermanentClassAlias(missingAlias);
//         } catch (Exception e) {
//             fail("Removing a non‑existent alias should not throw, but got: " + e);
//         }
//         // then: map remains empty
//         assertTrue("Map should remain empty after attempting to remove a missing alias", nameToClassMap.isEmpty());
//     }

//     @Test
//     @DisplayName("removePermanentClassAlias with null argument does not throw")
//     void testRemoveNullAlias() {
//         // given: map is empty
//         assertTrue(nameToClassMap.isEmpty());
//         // when / then: calling with null is safe (ConcurrentHashMap permits null removal)
//         try {
//             ClassUtilities.removePermanentClassAlias(null);
//         } catch (Exception e) {
//             fail("Calling removePermanentClassAlias(null) should not throw, but got: " + e);
//         }
//         assertTrue("Map should still be empty after attempting to remove a null key", nameToClassMap.isEmpty());
//     }

//     @ParameterizedTest(name = "remove alias \"{0}\" that exists among other entries")
//     @ValueSource(strings = { "my.alias", "", "   ", "spécial-älias_!@#" })
//     void testRemoveAliasAmongMultiple(String alias) {
//         // given: two distinct aliases are present
//         String otherAlias = "other.alias";
//         ClassUtilities.addPermanentClassAlias(Integer.class, otherAlias);
//         ClassUtilities.addPermanentClassAlias(String.class, alias);
//         assertTrue(nameToClassMap.containsKey(alias), "Pre‑condition: map should contain the target alias");
//         assertTrue(nameToClassMap.containsKey(otherAlias), "Pre‑condition: map should contain the other alias");
//         // when: the target alias is removed
//         ClassUtilities.removePermanentClassAlias(alias);
//         // then: only the target alias disappears, the other stays untouched
//         assertFalse(nameToClassMap.containsKey(alias), "Target alias should have been removed");
//         assertTrue(nameToClassMap.containsKey(otherAlias), "Other alias must remain in the map");
//         assertEquals(1, nameToClassMap.size(), "Map size should be reduced by exactly one");
//     }

//     @Test
//     @DisplayName("removePermanentClassAlias is idempotent for duplicate removals")
//     void testDuplicateRemovalIdempotence() {
//         // given: a single alias is registered
//         String alias = "duplicate.alias";
//         ClassUtilities.addPermanentClassAlias(Double.class, alias);
//         assertTrue(nameToClassMap.containsKey(alias));
//         // when: remove it once
//         ClassUtilities.removePermanentClassAlias(alias);
//         assertFalse(nameToClassMap.containsKey(alias));
//         // and: attempt to remove it a second time
//         try {
//             ClassUtilities.removePermanentClassAlias(alias);
//         } catch (Exception e) {
//             fail("Second removal of the same alias should not throw, but got: " + e);
//         }
//         // then: map stays empty and no side‑effects occurred
//         assertTrue(nameToClassMap.isEmpty());
//     }

//     @ParameterizedTest(name = "remove alias \"{0}\" (empty/whitespace) works")
//     @ValueSource(strings = { "", "   " })
//     void testRemoveEmptyOrWhitespaceAlias(String alias) {
//         // given: the alias (empty or whitespace) is registered
//         ClassUtilities.addPermanentClassAlias(Long.class, alias);
//         assertTrue(nameToClassMap.containsKey(alias));
//         // when: removal is performed
//         ClassUtilities.removePermanentClassAlias(alias);
//         // then: the entry disappears without exception
//         assertFalse(nameToClassMap.containsKey(alias));
//         assertTrue(nameToClassMap.isEmpty());
//     }

//     @Test
//     @DisplayName("removePermanentClassAlias handles Unicode and punctuation characters")
//     void testRemoveSpecialCharacterAlias() {
//         String specialAlias = "spécial-älias_!@#";
//         ClassUtilities.addPermanentClassAlias(Boolean.class, specialAlias);
//         assertTrue(nameToClassMap.containsKey(specialAlias));
//         // when
//         ClassUtilities.removePermanentClassAlias(specialAlias);
//         // then
//         assertFalse(nameToClassMap.containsKey(specialAlias));
//         assertTrue(nameToClassMap.isEmpty());
//     }

//     @AfterClass
//     public static void teardownAfterAll() {
//         // Ensure the map is cleared after all tests – a safety net.
//         if (nameToClassMap != null) {
//             nameToClassMap.clear();
//         }
//     }
}