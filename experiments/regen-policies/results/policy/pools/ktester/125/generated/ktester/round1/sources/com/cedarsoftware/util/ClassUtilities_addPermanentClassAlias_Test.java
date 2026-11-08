package com.cedarsoftware.util;

import java.lang.reflect.Method;

// @ExtendWith(MockitoExtension.class)
class ClassUtilities_addPermanentClassAlias_Test {

    private static final String ALIAS = "MyTestAlias";

    private static final String ALIAS_2 = "MySecondAlias";

    /**
     * Simple class used as a target for the alias.
     */
    private static class Dummy {
    }

    /**
     * Another class used to verify that an alias can be overwritten.
     */
    private static class OtherDummy {
    }

//     @BeforeEach
    void setupBeforeEach() {
        // Ensure a clean state before each test – remove any leftover aliases.
        ClassUtilities.removePermanentClassAlias(ALIAS);
        ClassUtilities.removePermanentClassAlias(ALIAS_2);
        // Also clear any entries that might have been added by other tests.
        getInternalMap().clear();
    }

//     @AfterEach
    void teardownAfterEach() {
        // Clean up any aliases that a test may have added.
        ClassUtilities.removePermanentClassAlias(ALIAS);
        ClassUtilities.removePermanentClassAlias(ALIAS_2);
        getInternalMap().clear();
    }

    /**
     * Returns {@code true} if the supplied {@link Method} has a {@code void}
     * return type.
     *
     * @param method the method to inspect; never {@code null}
     * @return {@code true} if the method returns {@code void}
     */
    public static boolean returnsVoid(Method method) {
        return method.getReturnType() == void.class;
    }

//     @Test
//     @DisplayName("addPermanentClassAlias stores mapping that forName can resolve")
//     void addPermanentClassAlias_isResolvableViaForName() {
//         // given
//         ClassUtilities.addPermanentClassAlias(Dummy.class, ALIAS);
//         // when
//         Class<?> result = ClassUtilities.forName(ALIAS, null);
//         // then
//         assertNotNull(result, "forName should return a class for a known alias");
//         assertEquals(Dummy.class, result, "Alias should resolve to the class that was registered");
//     }

//     @Test
//     @DisplayName("addPermanentClassAlias overwrites an existing alias")
//     void addPermanentClassAlias_overwritesExistingAlias() {
//         // given – first registration
//         ClassUtilities.addPermanentClassAlias(Dummy.class, ALIAS);
//         assertEquals(Dummy.class, ClassUtilities.forName(ALIAS, null));
//         // when – second registration with a different class but same alias
//         ClassUtilities.addPermanentClassAlias(OtherDummy.class, ALIAS);
//         // then – the alias now resolves to the new class
//         Class<?> result = ClassUtilities.forName(ALIAS, null);
//         assertEquals(OtherDummy.class, result, "Adding a second class with the same alias should replace the previous mapping");
//     }

//     @Test
//     @DisplayName("addPermanentClassAlias supports multiple independent aliases")
//     void addPermanentClassAlias_multipleAliases() {
//         // given
//         ClassUtilities.addPermanentClassAlias(Dummy.class, ALIAS);
//         ClassUtilities.addPermanentClassAlias(OtherDummy.class, ALIAS_2);
//         // when
//         Class<?> result1 = ClassUtilities.forName(ALIAS, null);
//         Class<?> result2 = ClassUtilities.forName(ALIAS_2, null);
//         // then
//         assertEquals(Dummy.class, result1);
//         assertEquals(OtherDummy.class, result2);
//     }

//     @Test
//     @DisplayName("addPermanentClassAlias throws NullPointerException for null alias")
//     void addPermanentClassAlias_nullAlias_throwsNPE() {
//         // given
//         Class<?> clazz = Dummy.class;
//         // when / then
//         assertThrows(NullPointerException.class, () -> ClassUtilities.addPermanentClassAlias(clazz, null), "ConcurrentHashMap does not allow null keys, so a null alias must cause NPE");
//     }

//     @Test
//     @DisplayName("addPermanentClassAlias throws NullPointerException for null class")
//     void addPermanentClassAlias_nullClass_throwsNPE() {
//         // given
//         String alias = "someAlias";
//         // when / then
//         assertThrows(NullPointerException.class, () -> ClassUtilities.addPermanentClassAlias(null, alias), "ConcurrentHashMap does not allow null values, so a null class must cause NPE");
//     }

    /**
     * Helper to obtain the private static map {@code nameToClass}.
     */
//     @SuppressWarnings("unchecked")
//     private static Map<String, Class<?>> getInternalMap() {
//         try {
//             Field f = ClassUtilities.class.getDeclaredField("nameToClass");
//             f.setAccessible(true);
//             return (Map<String, Class<?>>) f.get(null);
//         } catch (ReflectiveOperationException e) {
//             throw new AssertionError("Unable to access ClassUtilities.nameToClass via reflection", e);
//         }
//     }

    /**
     * Idempotent behavior – inserting the same alias/class twice.
     */
//     @Test
//     @DisplayName("addPermanentClassAlias idempotent when same alias/class inserted twice")
//     void addPermanentClassAlias_idempotentBehavior() {
//         String alias = "HashMapAlias";
//         Class<?> clazz = java.util.HashMap.class;
//         // First insertion
//         ClassUtilities.addPermanentClassAlias(clazz, alias);
//         Class<?> first = ClassUtilities.forName(alias, null);
//         assertEquals(clazz, first);
//         // Capture map state
//         Map<String, Class<?>> before = Map.copyOf(getInternalMap());
//         // Second insertion (same pair)
//         ClassUtilities.addPermanentClassAlias(clazz, alias);
//         Class<?> second = ClassUtilities.forName(alias, null);
//         assertEquals(clazz, second);
//         // Map should be unchanged
//         assertEquals(before, getInternalMap(), "Re‑adding the same alias/class pair must not modify the map");
//     }
}