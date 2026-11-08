package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ClassUtilities_addPermanentClassAlias_Test{

    private static final String TEST_ALIAS = "TestAlias";

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for this test class
    }

    @BeforeEach
    void setupBeforeEach() {
        // Ensure a clean state before each test – remove the alias if it somehow remains
        ClassUtilities.removePermanentClassAlias(TEST_ALIAS);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clean up after each test to avoid side‑effects on other tests
        ClassUtilities.removePermanentClassAlias(TEST_ALIAS);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to clean up
    }

    /**
     * Verify that a class can be retrieved via its alias after it has been added.
     */
    @Test
    @DisplayName("addPermanentClassAlias – basic add and lookup")
    void testAddAndLookupAlias() throws Exception {
        // Add an alias for java.lang.String
        ClassUtilities.addPermanentClassAlias(String.class, TEST_ALIAS);

        // Lookup the alias using the public forName API
        Class<?> result = ClassUtilities.forName(TEST_ALIAS, ClassUtilities.getClassLoader());

        assertNotNull(result, "Result should not be null");
        assertEquals(String.class, result, "Alias should resolve to the original class");
    }

    /**
     * Adding an alias with the same name should replace the previous mapping.
     */
    @Test
    @DisplayName("addPermanentClassAlias – replace existing alias")
    void testReplaceExistingAlias() throws Exception {
        // First mapping
        ClassUtilities.addPermanentClassAlias(String.class, TEST_ALIAS);
        Class<?> first = ClassUtilities.forName(TEST_ALIAS, ClassUtilities.getClassLoader());
        assertEquals(String.class, first, "First mapping should resolve to String");

        // Replace with a different class
        ClassUtilities.addPermanentClassAlias(Integer.class, TEST_ALIAS);
        Class<?> second = ClassUtilities.forName(TEST_ALIAS, ClassUtilities.getClassLoader());
        assertEquals(Integer.class, second, "Second mapping should resolve to Integer");
    }

    /**
     * After removing an alias, a lookup should fail with ClassNotFoundException.
     */
    @Test
    @DisplayName("removePermanentClassAlias – alias no longer resolvable")
    void testRemoveAlias() throws Exception {
        ClassUtilities.addPermanentClassAlias(Double.class, TEST_ALIAS);
        // Ensure it is present
        assertEquals(Double.class,
                ClassUtilities.forName(TEST_ALIAS, ClassUtilities.getClassLoader()),
                "Alias should resolve before removal");

        // Remove the alias
        ClassUtilities.removePermanentClassAlias(TEST_ALIAS);

        // The forName method should now attempt to load a class with that name and fail
        assertThrows(ClassNotFoundException.class,
                () -> ClassUtilities.forName(TEST_ALIAS, ClassUtilities.getClassLoader()),
                "Lookup after removal should throw ClassNotFoundException");
    }

    /**
     * Adding a null alias should result in a NullPointerException because
     * ConcurrentHashMap does not permit null keys.
     */
    @Test
    @DisplayName("addPermanentClassAlias – null alias throws NPE")
    void testNullAliasThrows() {
        assertThrows(NullPointerException.class,
                () -> ClassUtilities.addPermanentClassAlias(String.class, null),
                "Null alias should cause NullPointerException");
    }

    /**
     * Adding a null class should also result in a NullPointerException because
     * ConcurrentHashMap does not permit null values.
     */
    @Test
    @DisplayName("addPermanentClassAlias – null class throws NPE")
    void testNullClassThrows() {
        assertThrows(NullPointerException.class,
                () -> ClassUtilities.addPermanentClassAlias(null, TEST_ALIAS),
                "Null class should cause NullPointerException");
    }
}