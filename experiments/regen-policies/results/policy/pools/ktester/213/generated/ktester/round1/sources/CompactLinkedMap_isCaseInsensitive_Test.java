package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@ExtendWith(MockitoExtension.class)
class CompactLinkedMap_isCaseInsensitive_Test {

    private CompactLinkedMap<String, Integer> map;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh instance before each test
        map = new CompactLinkedMap<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clean up instance references
        map = null;
    }

    @Test
    void isCaseInsensitive_returnsFalse() {
        // GIVEN a newly created CompactLinkedMap (default behavior)
        // WHEN the protected method isCaseInsensitive() is invoked directly
        boolean result = map.isCaseInsensitive();
        // THEN it should return false, as per the default implementation
        assertFalse(result, "CompactLinkedMap.isCaseInsensitive() should return false by default");
    }

    /* -----------------------------------------------------------
     *  Group: isCaseInsensitive reflection access
     * ----------------------------------------------------------- */
    @Test
    void reflection_isCaseInsensitive_returnsFalse() throws Exception {
        // GIVEN a CompactLinkedMap instance and a Method object for the protected method
        Method method = CompactLinkedMap.class.getDeclaredMethod("isCaseInsensitive");
        // make it callable via reflection
        method.setAccessible(true);
        // WHEN the method is invoked reflectively
        boolean result = (boolean) method.invoke(map);
        // THEN the result must be false (default implementation)
        assertFalse(result, "Reflective call to isCaseInsensitive() should return false");
    }

    /* -----------------------------------------------------------
     *  Group: isCaseInsensitive overridden behavior
     * ----------------------------------------------------------- */
    @Test
    void overridden_isCaseInsensitive_returnsTrue() {
        // GIVEN an anonymous subclass that overrides the method to return true
        CompactLinkedMap<String, Integer> overriddenMap = new CompactLinkedMap<String, Integer>() {

            @Override
            protected boolean isCaseInsensitive() {
                return true;
            }
        };
        // WHEN the method is called
        boolean result = overriddenMap.isCaseInsensitive();
        // THEN it should return true
        assertTrue(result, "Overridden isCaseInsensitive() should return true");
    }

    /* -----------------------------------------------------------
     *  Group: isCaseInsensitive reflection IllegalAccessException
     * ----------------------------------------------------------- */
    @Test
    @Disabled("Cannot reliably trigger IllegalAccessException from the same package. " + "The test is kept for documentation purposes.")
    void reflection_illegalAccessException_whenNotAccessible() throws Exception {
        // GIVEN a Method object without calling setAccessible(true)
        Method method = CompactLinkedMap.class.getDeclaredMethod("isCaseInsensitive");
        // Intentionally NOT calling method.setAccessible(true);
        // WHEN invoking the method from a different package without accessibility,
        // THEN an IllegalAccessException is expected.
        assertThrows(IllegalAccessException.class, () -> method.invoke(map));
    }

    /* -----------------------------------------------------------
     *  Group: isCaseInsensitive reflection InvocationTargetException
     * ----------------------------------------------------------- */
    @Test
    void reflection_invocationTargetException_whenOverrideThrows() throws Exception {
        // GIVEN a subclass that throws a RuntimeException from the overridden method
        CompactLinkedMap<String, Integer> throwingMap = new CompactLinkedMap<String, Integer>() {

            @Override
            protected boolean isCaseInsensitive() {
                throw new RuntimeException("forced");
            }
        };
        Method method = CompactLinkedMap.class.getDeclaredMethod("isCaseInsensitive");
        method.setAccessible(true);
        // WHEN invoking via reflection, THEN an InvocationTargetException should be thrown
        InvocationTargetException ite = assertThrows(InvocationTargetException.class, () -> method.invoke(throwingMap), "InvocationTargetException expected when overridden method throws");
        // AND the cause should be the original RuntimeException
        assertTrue(ite.getCause() instanceof RuntimeException);
        assertEquals("forced", ite.getCause().getMessage());
    }

    /* -----------------------------------------------------------
     *  Group: isCaseInsensitive reflection NullPointerException
     * ----------------------------------------------------------- */
    @Test
    void reflection_nullPointerException_whenTargetIsNull() throws Exception {
        // GIVEN a Method object for isCaseInsensitive()
        Method method = CompactLinkedMap.class.getDeclaredMethod("isCaseInsensitive");
        method.setAccessible(true);
        // WHEN invoking with a null target, THEN NullPointerException is expected
        assertThrows(NullPointerException.class, () -> method.invoke(null), "Method.invoke(null) should throw NullPointerException");
    }

    /* -----------------------------------------------------------
     *  Group: isCaseInsensitive reflection IllegalArgumentException
     * ----------------------------------------------------------- */
    @Test
    void reflection_illegalArgumentException_whenExtraArgumentsProvided() throws Exception {
        // GIVEN a Method object for the no‑arg protected method
        Method method = CompactLinkedMap.class.getDeclaredMethod("isCaseInsensitive");
        method.setAccessible(true);
        // WHEN invoking with an argument array (method takes none), THEN IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> method.invoke(map, true), "Providing arguments to a no‑arg method should throw IllegalArgumentException");
    }

    /* -----------------------------------------------------------
     *  Group: isCaseInsensitive overridden throws exception
     * ----------------------------------------------------------- */
    @Test
    void overridden_isCaseInsensitive_throwsIllegalStateException() {
        // GIVEN a subclass that throws IllegalStateException from the overridden method
        CompactLinkedMap<String, Integer> errorMap = new CompactLinkedMap<String, Integer>() {

            @Override
            protected boolean isCaseInsensitive() {
                throw new IllegalStateException("override error");
            }
        };
        // WHEN the method is called directly, THEN the IllegalStateException propagates
        IllegalStateException ise = assertThrows(IllegalStateException.class, errorMap::isCaseInsensitive, "Expected IllegalStateException from overridden method");
        assertEquals("override error", ise.getMessage());
    }
}
