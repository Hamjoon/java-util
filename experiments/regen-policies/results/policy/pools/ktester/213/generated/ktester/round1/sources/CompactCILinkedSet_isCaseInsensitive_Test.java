package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;
import java.util.Collection;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

@ExtendWith(MockitoExtension.class)
class CompactCILinkedSet_isCaseInsensitive_Test {

    /**
     * Instance created before each test.
     */
    private CompactCILinkedSet<String> set;

    class ConcreteSubclass extends CompactCILinkedSet<String> {
        // No override
    }

    class ConcreteSubclass extends CompactCILinkedSet<String> {
        // No override
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use the default constructor for a fresh instance.
        set = new CompactCILinkedSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and guarantee no cross‑test contamination.
        set = null;
    }

    /**
     * Verify that the overridden method reports case‑insensitivity.
     */
    @Test
    void testIsCaseInsensitiveReturnsTrue() {
        // The method is protected but the test class resides in the same package,
        // so it can be accessed directly.
        assertTrue(set.isCaseInsensitive(), "CompactCILinkedSet should report case‑insensitive behavior");
    }

    /**
     * Ensure the collection‑based constructor copies all elements.
     */
    @Test
    void testCollectionConstructorCopiesElements() {
        Collection<String> source = Arrays.asList("One", "Two", "Three");
        CompactCILinkedSet<String> fromCollection = new CompactCILinkedSet<>(source);
        assertEquals(source.size(), fromCollection.size(), "The set created from a collection should contain the same number of elements");
        assertTrue(fromCollection.containsAll(source), "All elements from the source collection should be present in the new set");
        // The case‑insensitivity flag should still be true.
        assertTrue(fromCollection.isCaseInsensitive(), "The set created via the collection constructor must also be case‑insensitive");
    }

    /**
     * Invoke the protected method via reflection and verify it returns true.
     */
    @Test
    void testIsCaseInsensitiveViaReflection() throws Exception {
        Method m = CompactCILinkedSet.class.getDeclaredMethod("isCaseInsensitive");
        m.setAccessible(true);
        boolean result = (boolean) m.invoke(set);
        assertTrue(result, "Reflection should return true for isCaseInsensitive()");
    }

    /**
     * Anonymous subclass overrides isCaseInsensitive() to return false – verify via reflection.
     */
    @Test
    void testSubclassOverrideIsCaseInsensitiveFalse() throws Exception {
        CompactCILinkedSet<String> subclass = new CompactCILinkedSet<String>() {

            @Override
            protected boolean isCaseInsensitive() {
                return false;
            }
        };
        Method m = CompactCILinkedSet.class.getDeclaredMethod("isCaseInsensitive");
        m.setAccessible(true);
        boolean result = (boolean) m.invoke(subclass);
        assertFalse(result, "Overridden subclass should return false via reflection");
    }

    /**
     * Concrete subclass does not override – inherited implementation must still return true.
     */
    @Test
    void testConcreteSubclassInheritsTrue() throws Exception {
        class ConcreteSubclass extends CompactCILinkedSet<String> {
            // No override
        }
        CompactCILinkedSet<String> subclass = new ConcreteSubclass();
        Method m = CompactCILinkedSet.class.getDeclaredMethod("isCaseInsensitive");
        m.setAccessible(true);
        boolean result = (boolean) m.invoke(subclass);
        assertTrue(result, "Concrete subclass should inherit true from parent");
    }

    /**
     * Verify the method is declared with protected visibility.
     */
    @Test
    void testIsCaseInsensitiveVisibility() throws Exception {
        Method m = CompactCILinkedSet.class.getDeclaredMethod("isCaseInsensitive");
        int mods = m.getModifiers();
        assertTrue(Modifier.isProtected(mods), "isCaseInsensitive() should be declared protected");
    }

    /**
     * Attempt to invoke without setAccessible(true) – expect IllegalAccessException.
     */
    @Test
    void testReflectionIllegalAccessException() throws Exception {
        Method m = CompactCILinkedSet.class.getDeclaredMethod("isCaseInsensitive");
        // Do NOT call setAccessible(true)
        assertThrows(IllegalAccessException.class, () -> m.invoke(set), "Invoking protected method without setAccessible should throw IllegalAccessException");
    }

    /**
     * Invoke a subclass that throws RuntimeException – expect InvocationTargetException.
     */
    @Test
    void testReflectionInvocationTargetException() throws Exception {
        CompactCILinkedSet<String> badSubclass = new CompactCILinkedSet<String>() {

            @Override
            protected boolean isCaseInsensitive() {
                throw new RuntimeException("forced");
            }
        };
        Method m = CompactCILinkedSet.class.getDeclaredMethod("isCaseInsensitive");
        m.setAccessible(true);
        InvocationTargetException ite = assertThrows(InvocationTargetException.class, () -> m.invoke(badSubclass), "InvocationTargetException expected when underlying method throws");
        assertTrue(ite.getCause() instanceof RuntimeException, "Cause of InvocationTargetException should be RuntimeException");
        assertEquals("forced", ite.getCause().getMessage(), "RuntimeException message should be preserved");
    }

    /**
     * Pass null as target instance – expect NullPointerException.
     */
    @Test
    void testReflectionNullPointerException() throws Exception {
        Method m = CompactCILinkedSet.class.getDeclaredMethod("isCaseInsensitive");
        m.setAccessible(true);
        assertThrows(NullPointerException.class, () -> m.invoke(null), "Invoking instance method with null target should throw NullPointerException");
    }

    /**
     * Invoke on an object of unrelated type – expect IllegalArgumentException.
     */
    @Test
    void testReflectionIllegalArgumentException() throws Exception {
        Method m = CompactCILinkedSet.class.getDeclaredMethod("isCaseInsensitive");
        m.setAccessible(true);
        Object unrelated = new Object();
        assertThrows(IllegalArgumentException.class, () -> m.invoke(unrelated), "Invoking method on wrong type should throw IllegalArgumentException");
    }
}
