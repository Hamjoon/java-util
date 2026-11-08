package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

@ExtendWith(MockitoExtension.class)
class CompactMap_isCaseInsensitive_Test {

    /**
     * A map that uses the default implementation of {@code isCaseInsensitive()}.
     */
    private CompactMap<String, Object> defaultMap;

    /**
     * A map that overrides {@code isCaseInsensitive()} to return {@code true}.
     */
    private CompactMap<String, Object> overriddenMap;

    @BeforeEach
    void setupBeforeEach() {
        /* ----------------------------------------------------------------------
         * The default constructor of CompactMap checks that compactSize() >= 2.
         * We therefore provide a minimal concrete subclass that supplies a
         * compact size of 3 and a simple backing map.  This allows us to
         * instantiate the class without triggering IllegalArgumentException.
         * ---------------------------------------------------------------------- */
        defaultMap = new CompactMap<String, Object>() {

            @Override
            protected int compactSize() {
                // any value >= 2 is acceptable
                return 3;
            }

            @Override
            protected Map<String, Object> getNewMap() {
                // simple backing map
                return new HashMap<>();
            }
        };
        overriddenMap = new CompactMap<String, Object>() {

            @Override
            protected int compactSize() {
                return 3;
            }

            @Override
            protected Map<String, Object> getNewMap() {
                return new HashMap<>();
            }

            @Override
            protected boolean isCaseInsensitive() {
                // explicit override for testing
                return true;
            }
        };
    }

    /**
     * The default implementation must return {@code false}.
     */
    @Test
    void testDefaultIsCaseInsensitiveReturnsFalse() {
        assertFalse(defaultMap.isCaseInsensitive(), "CompactMap default should be case‑sensitive, therefore isCaseInsensitive() must be false");
    }

    /**
     * A subclass that overrides the method should return the overridden value.
     */
    @Test
    void testOverriddenIsCaseInsensitiveReturnsTrue() {
        assertTrue(overriddenMap.isCaseInsensitive(), "Overridden isCaseInsensitive() should return true as defined in the subclass");
    }

    /**
     * The constructor validates that a {@link java.util.SortedMap} using a
     * case‑insensitive comparator is only allowed when {@code isCaseInsensitive()}
     * returns {@code true}.  When the configuration is inconsistent an
     * {@link IllegalStateException} must be thrown.
     */
    @Test
    void testConstructorInconsistentCaseSensitivityThrows() {
        assertThrows(IllegalStateException.class, () -> {
            new CompactMap<String, Object>() {

                @Override
                protected int compactSize() {
                    return 3;
                }

                @Override
                protected Map<String, Object> getNewMap() {
                    // TreeMap with a case‑insensitive comparator
                    return new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
                }
            };
        }, "Constructor should detect inconsistent case‑sensitivity configuration and throw IllegalStateException");
    }

    /**
     * Verify that attempting to change the static final field {@code DEFAULT_CASE_SENSITIVE}
     * via reflection does **not** affect the result of {@code isCaseInsensitive()}.
     * The method returns the negated compile‑time constant, therefore it must still
     * return {@code false} after the reflective change.
     */
    @Test
    void testReflectionChangingDefaultCaseSensitiveHasNoEffect() throws Exception {
        // Preserve original value so we can restore it after the test.
        Field field = CompactMap.class.getDeclaredField("DEFAULT_CASE_SENSITIVE");
        field.setAccessible(true);
        boolean original = (boolean) field.get(null);
        try {
            // Remove the final modifier (required on pre‑Java‑12 runtimes).
            Field modifiers = Field.class.getDeclaredField("modifiers");
            modifiers.setAccessible(true);
            modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);
            // Attempt to set the constant to false.
            field.set(null, false);
            // Create a fresh instance (same subclass used in @BeforeEach).
            CompactMap<String, Object> map = new CompactMap<String, Object>() {

                @Override
                protected int compactSize() {
                    return 3;
                }

                @Override
                protected Map<String, Object> getNewMap() {
                    return new HashMap<>();
                }
            };
            // Even though we changed the field, the method should still return false.
            assertFalse(map.isCaseInsensitive(), "isCaseInsensitive() must remain false because the value is inlined at compile time");
        } finally {
            // Restore original constant to avoid side‑effects for other tests.
            field.set(null, original);
        }
    }

    /**
     * Attempt to invoke the protected {@code isCaseInsensitive()} method via reflection
     * on a {@code null} target. The reflective call must fail with an
     * {@link InvocationTargetException} whose cause is a {@link NullPointerException}.
     */
    @Test
    void testReflectionInvokeIsCaseInsensitiveOnNullTarget() throws Exception {
        Method method = CompactMap.class.getDeclaredMethod("isCaseInsensitive");
        method.setAccessible(true);
        InvocationTargetException thrown = assertThrows(InvocationTargetException.class, () -> {
            // Passing null as the target instance.
            method.invoke(null);
        }, "Invoking isCaseInsensitive() on a null target should throw InvocationTargetException");
        assertTrue(thrown.getCause() instanceof NullPointerException, "The cause of the InvocationTargetException should be NullPointerException");
    }
}
