package com.cedarsoftware.util;

import java.lang.reflect.Field;
import java.util.Map;
// import org.junit.*;
// import static org.junit.Assert.*;
// import org.junit.After;
// import org.junit.Before;
// import org.junit.Test;
// import static org.junit.Assert.*;

class ClassUtilities_removePermanentClassAlias_Test {

    /**
     * Alias used for the tests – it does not correspond to a real class name.
     */
    private static final String TEST_ALIAS = "MyTestAlias";

    //     @BeforeEach
    void setupBeforeEach() {
        // Ensure a clean state before each test by removing the test alias if it somehow remains.
        // This is safe – removing a non‑existent alias is a no‑op.
        ClassUtilities.removePermanentClassAlias(TEST_ALIAS);
    }

    //     @AfterEach
    void teardownAfterEach() {
        // Clean up any alias that might have been left behind by a failing test.
        ClassUtilities.removePermanentClassAlias(TEST_ALIAS);
    }

    /**
     * Helper that uses reflection to obtain the private static {@code nameToClass} map
     * from {@link ClassUtilities}. This allows us to assert internal state without
     * exposing the field publicly.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Class<?>> getInternalMap() throws Exception {
        Field field = ClassUtilities.class.getDeclaredField("nameToClass");
        field.setAccessible(true);
        return (Map<String, Class<?>>) field.get(null);
    }

    /**
     * Verify that an alias added with {@code addPermanentClassAlias} can be resolved,
     * and that after removal the alias is no longer resolvable.
     */
//     @Test
//     public void testAddAndRemoveAlias() {
//         // Arrange: register an alias for java.lang.String
//         ClassUtilities.addPermanentClassAlias(String.class, TEST_ALIAS);
//         // Act & Assert: the alias should resolve to String.class
//         Class<?> beforeRemoval = ClassUtilities.forName(TEST_ALIAS, null);
//         assertNotNull("Alias should resolve before removal", beforeRemoval);
//         assertEquals("Alias should resolve to the class it was registered for", String.class, beforeRemoval);
//         // Remove the alias
//         ClassUtilities.removePermanentClassAlias(TEST_ALIAS);
//         // After removal the alias should no longer resolve.
//         // The public API returns null when an alias cannot be found.
//         Class<?> afterRemoval = ClassUtilities.forName(TEST_ALIAS, null);
//         assertNull("Alias should not resolve after removal", afterRemoval);
//     }

    /**
     * Removing an alias that has never been added must not throw and must leave existing
     * mappings untouched.
     */
//     @Test
//     public void testRemoveNonExistentAlias() {
//         // Arrange: add a different alias that we will keep
//         final String otherAlias = "OtherAlias";
//         ClassUtilities.addPermanentClassAlias(Integer.class, otherAlias);
//         // Act: attempt to remove a completely unrelated alias
//         try {
//             ClassUtilities.removePermanentClassAlias("NonExistentAlias");
//         } catch (Exception e) {
//             fail("Removing a non‑existent alias should not throw, but got: " + e);
//         }
//         // Assert: the other alias is still resolvable
//         Class<?> stillThere = ClassUtilities.forName(otherAlias, null);
//         assertNotNull("Other alias should still resolve after attempting to remove a non‑existent alias", stillThere);
//         assertEquals("Other alias should still map to its original class", Integer.class, stillThere);
//         // Clean up the auxiliary alias
//         ClassUtilities.removePermanentClassAlias(otherAlias);
//     }

    /**
     * Directly inspect the internal map to ensure that a previously added alias is
     * really removed from {@code ClassUtilities.nameToClass}.
     */
//     @Test
//     public void testRemoveExistingAlias() throws Exception {
//         // Arrange: add an alias for java.lang.String
//         ClassUtilities.addPermanentClassAlias(String.class, TEST_ALIAS);
//         // Verify that the internal map contains the entry (reflection)
//         Map<String, Class<?>> internalMap = getInternalMap();
//         assertTrue("Internal map should contain the alias before removal", internalMap.containsKey(TEST_ALIAS));
//         // Act: remove the alias
//         ClassUtilities.removePermanentClassAlias(TEST_ALIAS);
//         // Assert: the internal map no longer contains the key
//         assertFalse("Internal map should no longer contain the alias after removal", internalMap.containsKey(TEST_ALIAS));
//     }

    /**
     * Passing {@code null} as the alias should result in a {@link NullPointerException}
     * because {@link java.util.concurrent.ConcurrentHashMap} does not permit null keys.
     */
//     @Test(expected = NullPointerException.class)
//     public void testRemoveNullAlias() {
//         ClassUtilities.removePermanentClassAlias(null);
//     }
}