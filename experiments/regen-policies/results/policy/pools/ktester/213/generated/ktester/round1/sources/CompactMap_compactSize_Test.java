package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.Permission;
import java.util.Collections;
import java.util.HashMap;

@ExtendWith(MockitoExtension.class)
class CompactMap_compactSize_Test {

    /* --------------------------------------------------------------------- */
    private static class ExposedCompactMap<K, V> extends CompactMap<K, V> {

        @Override
        protected Map<K, V> getNewMap() {
            // Simple backing map – sufficient for construction.
            return new HashMap<>();
        }

        /**
         * Public wrapper used by the tests to call the protected method.
         */
        public int publicCompactSize() {
            return compactSize();
        }
    }

    /**
     * Verify that the default implementation of {@code compactSize()} returns
     * the constant {@link CompactMap#DEFAULT_COMPACT_SIZE}.
     */
    @Test
    void defaultCompactSize_isConstant() {
        CompactMap<String, String> map = new CompactMap<>();
        // The method is protected – we can call it via reflection or by using a
        // subclass that exposes it. Here we use an anonymous subclass that simply
        // forwards the call.
        int size = ((CompactMap<String, String>) map).compactSize();
        assertEquals(CompactMap.DEFAULT_COMPACT_SIZE, size, "compactSize() should return the default constant");
    }

    /**
     * Verify that a subclass can override {@code compactSize()} and that the
     * overridden value is returned.
     */
    @Test
    void overriddenCompactSize_isReturned() {
        CompactMap<String, Integer> map = new CompactMap<String, Integer>() {

            @Override
            protected int compactSize() {
                // a value different from the default
                return 7;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                // The concrete map type is irrelevant for this test; we just
                // need a valid map to satisfy the constructor.
                return new java.util.HashMap<>();
            }
        };
        int size = ((CompactMap<String, Integer>) map).compactSize();
        assertEquals(7, size, "compactSize() should return the overridden value");
    }

    /**
     * The {@link CompactMap} constructor checks that {@code compactSize()} is
     * at least 2. An overridden value lower than that must cause an
     * {@link IllegalArgumentException}.
     */
    @Test
    void compactSizeBelowTwo_causesException() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            new CompactMap<String, String>() {

                @Override
                protected int compactSize() {
                    // illegal – less than the required minimum
                    return 1;
                }

                @Override
                protected Map<String, String> getNewMap() {
                    return new java.util.HashMap<>();
                }
            };
        });
        assertTrue(thrown.getMessage().contains("compactSize() must be >= 2"));
    }

    /**
     * Ensure that a valid custom compact size (>=2) does not throw and the map
     * can be used normally.
     */
    @Test
    void validCustomCompactSize_allowsNormalOperation() {
        CompactMap<String, String> map = new CompactMap<String, String>() {

            @Override
            protected int compactSize() {
                return 3;
            }

            @Override
            protected Map<String, String> getNewMap() {
                return new java.util.LinkedHashMap<>();
            }
        };
        // Normal map operations should work
        assertTrue(map.isEmpty());
        assertNull(map.put("a", "alpha"));
        assertEquals(1, map.size());
        assertEquals("alpha", map.get("a"));
        assertEquals("alpha", map.remove("a"));
        assertTrue(map.isEmpty());
    }

    /* --------------------------------------------------------------------- */
    @Test
    void overriddenCompactSize_positiveValue() {
        CompactMap<String, Integer> map = new CompactMap<String, Integer>() {

            @Override
            protected int compactSize() {
                return 10;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new HashMap<>();
            }

            public int publicCompactSize() {
                return compactSize();
            }
        };
        assertEquals(10, map.publicCompactSize(), "Overridden compactSize() should return 10");
    }

    @Test
    void overriddenCompactSize_negativeValue() {
        CompactMap<String, Integer> map = new CompactMap<String, Integer>() {

            @Override
            protected int compactSize() {
                return -1;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new HashMap<>();
            }

            public int publicCompactSize() {
                return compactSize();
            }
        };
        assertEquals(-1, map.publicCompactSize(), "Overridden compactSize() should return the negative value -1");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void reflectionInvoke_compactSize_returnsDefault() throws Exception {
        CompactMap<String, String> map = new CompactMap<>();
        Method m = CompactMap.class.getDeclaredMethod("compactSize");
        assertTrue(Modifier.isProtected(m.getModifiers()));
        m.setAccessible(true);
        int result = (int) m.invoke(map);
        assertEquals(CompactMap.DEFAULT_COMPACT_SIZE, result);
    }

    @Test
    void reflectionInvoke_compactSize_viaSuperclassMethodObject() throws Exception {
        CompactMap<String, String> map = new CompactMap<>();
        Method m = CompactMap.class.getDeclaredMethod("compactSize");
        m.setAccessible(true);
        int result = (int) m.invoke(map);
        assertEquals(CompactMap.DEFAULT_COMPACT_SIZE, result);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void withConfig_doesNotChangeCompactSize() {
        ExposedCompactMap<String, String> map = new ExposedCompactMap<>();
        Map<String, Object> cfg = Collections.singletonMap(CompactMap.COMPACT_SIZE, 100);
        CompactMap<String, String> configured = map.withConfig(cfg);
        // Cast back to our test subclass to call the public wrapper
        int size = ((ExposedCompactMap<String, String>) configured).publicCompactSize();
        assertEquals(CompactMap.DEFAULT_COMPACT_SIZE, size, "compactSize() should ignore configuration and stay at default");
    }

    @Test
    void withConfig_nullConfigKeepsDefault() {
        ExposedCompactMap<String, String> map = new ExposedCompactMap<>();
        CompactMap<String, String> configured = map.withConfig(null);
        int size = ((ExposedCompactMap<String, String>) configured).publicCompactSize();
        assertEquals(CompactMap.DEFAULT_COMPACT_SIZE, size, "compactSize() should remain default when withConfig(null) is called");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void anonymousSubclass_withoutOverride_returnsDefault() {
        CompactMap<String, String> map = new CompactMap<String, String>() {

            @Override
            protected Map<String, String> getNewMap() {
                return new HashMap<>();
            }

            public int publicCompactSize() {
                return compactSize();
            }
        };
        assertEquals(CompactMap.DEFAULT_COMPACT_SIZE, map.publicCompactSize(), "Anonymous subclass without override should return default compact size");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void reflectionInvoke_nullTarget_throwsNPE() throws Exception {
        Method m = CompactMap.class.getDeclaredMethod("compactSize");
        m.setAccessible(true);
        assertThrows(NullPointerException.class, () -> m.invoke(null));
    }

    @Test
    void reflectionInvoke_wrongTargetType_throwsIllegalArgumentException() throws Exception {
        Method m = CompactMap.class.getDeclaredMethod("compactSize");
        m.setAccessible(true);
        assertThrows(IllegalArgumentException.class, () -> m.invoke("I am not a CompactMap"));
    }

    @Test
    void reflectionInvoke_extraArguments_throwsIllegalArgumentException() throws Exception {
        Method m = CompactMap.class.getDeclaredMethod("compactSize");
        m.setAccessible(true);
        CompactMap<String, String> map = new CompactMap<>();
        // Passing two arguments where none are expected
        assertThrows(IllegalArgumentException.class, () -> m.invoke(map, 1, 2));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void overriddenCompactSize_throwsRuntimeException() {
        CompactMap<Integer, Integer> map = new CompactMap<Integer, Integer>() {

            @Override
            protected int compactSize() {
                throw new IllegalStateException("forced failure");
            }

            @Override
            protected Map<Integer, Integer> getNewMap() {
                return new HashMap<>();
            }

            public int publicCompactSize() {
                return compactSize();
            }
        };
        IllegalStateException ex = assertThrows(IllegalStateException.class, map::publicCompactSize);
        assertEquals("forced failure", ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    @Test
    // safeguard against an endless test
    @Timeout(5)
    void overriddenCompactSize_recursiveCall_causesStackOverflowError() {
        CompactMap<Integer, Integer> map = new CompactMap<Integer, Integer>() {

            @Override
            protected int compactSize() {
                // Intentional infinite recursion
                return compactSize();
            }

            @Override
            protected Map<Integer, Integer> getNewMap() {
                return new HashMap<>();
            }

            public int publicCompactSize() {
                return compactSize();
            }
        };
        assertThrows(StackOverflowError.class, map::publicCompactSize);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void reflectionSetAccessible_deniedBySecurityManager_throwsSecurityException() throws Exception {
        // Preserve the original security manager
        SecurityManager original = System.getSecurityManager();
        // Install a restrictive security manager that forbids suppressAccessChecks
        System.setSecurityManager(new SecurityManager() {

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything except reflective access checks
                if ("suppressAccessChecks".equals(perm.getName())) {
                    throw new SecurityException("Reflective access not allowed");
                }
            }
        });
        try {
            Method m = CompactMap.class.getDeclaredMethod("compactSize");
            // This call should now fail with SecurityException
            assertThrows(SecurityException.class, () -> m.setAccessible(true));
        } finally {
            // Restore the original security manager to avoid side‑effects on other tests
            System.setSecurityManager(original);
        }
    }
}
