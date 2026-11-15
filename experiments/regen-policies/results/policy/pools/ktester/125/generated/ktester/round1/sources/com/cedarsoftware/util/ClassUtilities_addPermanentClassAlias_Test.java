package com.cedarsoftware.util;

import java.lang.reflect.Field;
import java.util.*;
// import org.junit.*;
// import static org.junit.Assert.*;

// @ExtendWith(MockitoExtension.class)
class ClassUtilities_addPermanentClassAlias_Test {

    /**
     * Holds aliases that were added during a test so they can be removed afterwards.
     */
    private final List<String> addedAliases = new ArrayList<>();

    /**
     * Reflective access to the private static map used by ClassUtilities.
     */
    private static Map<String, Class<?>> nameToClassMap;

//     @BeforeAll
    static void initReflection() throws Exception {
        Field mapField = ClassUtilities.class.getDeclaredField("nameToClass");
        mapField.setAccessible(true);
        //noinspection unchecked
        nameToClassMap = (Map<String, Class<?>>) mapField.get(null);
    }

//     @BeforeEach
    void setupBeforeEach() {
        // Ensure a clean state before each test. The ClassUtilities map is shared,
        // so we remove any leftover aliases from previous tests (should be none,
        // but defensive programming is cheap).
        addedAliases.forEach(ClassUtilities::removePermanentClassAlias);
        addedAliases.clear();
        // Also clear the map directly in case an alias was added without being tracked.
        nameToClassMap.keySet().removeIf(key -> !addedAliases.contains(key));
    }

//     @AfterEach
    void teardownAfterEach() {
        // Clean up any aliases that were added during the test.
        addedAliases.forEach(ClassUtilities::removePermanentClassAlias);
        addedAliases.clear();
    }

//     @Test
//     @DisplayName("addPermanentClassAlias registers an alias that can be resolved via forName")
//     void testAddAliasIsResolvable() {
//         // GIVEN
//         String alias = "MyStringAlias";
//         Class<?> target = String.class;
//         // WHEN
//         ClassUtilities.addPermanentClassAlias(target, alias);
//         addedAliases.add(alias);
//         // THEN
//         Class<?> resolved = ClassUtilities.forName(alias, null);
//         assertNotNull("Resolved class should not be null", resolved);
//         assertEquals("Alias should resolve to the original class", target, resolved);
//     }

//     @Test
//     @DisplayName("addPermanentClassAlias overrides an existing alias")
//     void testAliasOverride() {
//         // GIVEN
//         String alias = "NumberAlias";
//         Class<?> first = Integer.class;
//         Class<?> second = Double.class;
//         // first registration
//         ClassUtilities.addPermanentClassAlias(first, alias);
//         addedAliases.add(alias);
//         Class<?> resolvedFirst = ClassUtilities.forName(alias, null);
//         assertEquals("First registration should resolve to Integer", first, resolvedFirst);
//         // second registration (override)
//         ClassUtilities.addPermanentClassAlias(second, alias);
//         Class<?> resolvedSecond = ClassUtilities.forName(alias, null);
//         assertEquals("Second registration should override and resolve to Double", second, resolvedSecond);
//     }

//     @Test
//     @DisplayName("addPermanentClassAlias with null alias throws NullPointerException")
//     void testNullAliasThrows() {
//         // GIVEN
//         Class<?> clazz = String.class;
//         // WHEN / THEN
//         ClassUtilities.addPermanentClassAlias(clazz, null);
//     }

//     @Test
//     @DisplayName("addPermanentClassAlias with null class throws NullPointerException")
//     void testNullClassThrows() {
//         // GIVEN
//         String alias = "NullClassAlias";
//         // WHEN / THEN
//         ClassUtilities.addPermanentClassAlias(null, alias);
//     }

//     @Test
//     @DisplayName("removePermanentClassAlias actually removes the alias")
//     void testRemoveAlias() {
//         // GIVEN
//         String alias = "TempAlias";
//         Class<?> target = ArrayList.class;
//         ClassUtilities.addPermanentClassAlias(target, alias);
//         addedAliases.add(alias);
//         // Ensure it is present
//         assertEquals(target, ClassUtilities.forName(alias, null));
//         // WHEN
//         ClassUtilities.removePermanentClassAlias(alias);
//         addedAliases.remove(alias);
//         // THEN
//         assertNull("After removal, forName should return null for the alias", ClassUtilities.forName(alias, null));
//     }

//     @Test
//     @DisplayName("addPermanentClassAlias overwrites an existing alias with a new class")
//     void testOverwriteExistingAlias() {
//         String alias = "DuplicateAlias";
//         Class<?> first = String.class;
//         Class<?> second = Double.class;
//         // first registration
//         ClassUtilities.addPermanentClassAlias(first, alias);
//         addedAliases.add(alias);
//         assertEquals(first, ClassUtilities.forName(alias, null));
//         // overwrite
//         ClassUtilities.addPermanentClassAlias(second, alias);
//         assertEquals(second, ClassUtilities.forName(alias, null));
//         assertEquals(second, nameToClassMap.get(alias));
//     }

//     @Test
//     @DisplayName("addPermanentClassAlias with empty and whitespace aliases are stored")
//     void testEmptyAndWhitespaceAliases() {
//         // empty string
//         ClassUtilities.addPermanentClassAlias(ArrayList.class, "");
//         addedAliases.add("");
//         assertEquals(ArrayList.class, ClassUtilities.forName("", null));
//         // whitespace only
//         String ws = "   ";
//         ClassUtilities.addPermanentClassAlias(HashMap.class, ws);
//         addedAliases.add(ws);
//         assertEquals(HashMap.class, ClassUtilities.forName(ws, null));
//     }

//     @Test
//     @DisplayName("addPermanentClassAlias accepts special‑character alias")
//     void testSpecialCharacterAlias() {
//         String alias = "My$Alias#1";
//         ClassUtilities.addPermanentClassAlias(Boolean.class, alias);
//         addedAliases.add(alias);
//         assertEquals(Boolean.class, ClassUtilities.forName(alias, null));
//     }

//     @Test
//     @DisplayName("addPermanentClassAlias accepts primitive and array classes")
//     void testPrimitiveAndArrayAliases() {
//         ClassUtilities.addPermanentClassAlias(int.class, "primitiveInt");
//         addedAliases.add("primitiveInt");
//         assertEquals(int.class, ClassUtilities.forName("primitiveInt", null));
//         ClassUtilities.addPermanentClassAlias(String[].class, "StringArray");
//         addedAliases.add("StringArray");
//         assertEquals(String[].class, ClassUtilities.forName("StringArray", null));
//     }

//     @Test
//     @DisplayName("addPermanentClassAlias accepts a very long alias key")
//     void testVeryLongAlias() {
//         String longAlias = "A_very_long_alias_name_exceeding_normal_lengths_to_test_map_handling_of_long_keys_1234567890";
//         ClassUtilities.addPermanentClassAlias(LinkedList.class, longAlias);
//         addedAliases.add(longAlias);
//         assertEquals(LinkedList.class, ClassUtilities.forName(longAlias, null));
//     }

//     @Before
//     public void setUp() {
//         // Ensure a clean state before each test.
//         for (String alias : addedAliases) {
//             ClassUtilities.removePermanentClassAlias(alias);
//         }
//         addedAliases.clear();
//         // Remove any stray entries that were not tracked.
//         nameToClassMap.keySet().removeIf(key -> !addedAliases.contains(key));
//     }

//     @After
//     public void tearDown() {
//         // Clean up any aliases that were added during the test.
//         for (String alias : addedAliases) {
//             ClassUtilities.removePermanentClassAlias(alias);
//         }
//         addedAliases.clear();
//     }
}