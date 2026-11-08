import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import com.cedarsoftware.util.CompactMap;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Method;

@ExtendWith(MockitoExtension.class)
class CompactMap_isCaseInsensitive_Test {

    /**
     * Concrete subclass exposing the protected {@code isCaseInsensitive()} method as public.
     */
    static class ExposedCompactMap<K, V> extends CompactMap<K, V> {

        @Override
        protected int compactSize() {
            return 3;
        }

        @Override
        protected Map<K, V> getNewMap() {
            return new HashMap<>();
        }

        /**
         * Public wrapper for the protected method.
         */
        public boolean isCaseInsensitivePublic() {
            return super.isCaseInsensitive();
        }
    }

    /**
     * Helper that invokes the protected {@code isCaseInsensitive()} method via reflection.
     */
    private static boolean invokeIsCaseInsensitive(CompactMap<?, ?> map) {
        try {
            Method m = CompactMap.class.getDeclaredMethod("isCaseInsensitive");
            m.setAccessible(true);
            return (boolean) m.invoke(map);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Verify the default implementation of {@code isCaseInsensitive()} returns {@code false}
     * because {@code DEFAULT_CASE_SENSITIVE} is {@code true}.
     */
    @Test
    void defaultIsCaseInsensitiveIsFalse() {
        CompactMap<String, Object> map = new CompactMap<>() {

            @Override
            protected int compactSize() {
                // any value >= 2
                return 3;
            }

            @Override
            protected Map<String, Object> getNewMap() {
                return new HashMap<>();
            }
        };
        assertFalse(invokeIsCaseInsensitive(map), "CompactMap should be case‑sensitive by default, therefore isCaseInsensitive() must be false");
    }

    /**
     * A legacy subclass (i.e. not a generated class) that uses a case‑insensitive
     * {@link SortedMap} but does **not** override {@code isCaseInsensitive()}.
     * According to the constructor logic this should raise an {@link IllegalStateException}
     * because the configuration is inconsistent.
     */
    @Test
    void legacySubclassWithInconsistentCaseSensitivityThrows() {
        assertThrows(IllegalStateException.class, () -> {
            new CompactMap<String, String>() {

                @Override
                protected int compactSize() {
                    return 3;
                }

                @Override
                protected Map<String, String> getNewMap() {
                    // TreeMap with case‑insensitive comparator
                    return new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
                }
            };
        });
    }

    /**
     * The same legacy subclass as above, but this time we explicitly override
     * {@code isCaseInsensitive()} to return {@code true}.  The constructor should
     * accept the configuration without throwing.
     */
    @Test
    void legacySubclassWithConsistentCaseSensitivityDoesNotThrow() {
        CompactMap<String, String> map = assertDoesNotThrow(() -> new CompactMap<String, String>() {

            @Override
            protected int compactSize() {
                return 3;
            }

            @Override
            protected Map<String, String> getNewMap() {
                return new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            }

            @Override
            protected boolean isCaseInsensitive() {
                // Override to match the case‑insensitive comparator
                return true;
            }
        });
        assertTrue(invokeIsCaseInsensitive(map), "After overriding to be case‑insensitive the method must return true");
    }

    /**
     * Repeatedly invoke {@code isCaseInsensitive()} on the same instance to ensure
     * the result is deterministic and unchanged across calls.
     */
    @Test
    void repeatedCallsAlwaysReturnFalse() {
        CompactMap<String, Object> map = new CompactMap<>() {

            @Override
            protected int compactSize() {
                return 3;
            }

            @Override
            protected Map<String, Object> getNewMap() {
                return new HashMap<>();
            }
        };
        // First call
        assertFalse(invokeIsCaseInsensitive(map), "First call should return false (default case‑sensitive)");
        // Second call
        assertFalse(invokeIsCaseInsensitive(map), "Second call should still return false");
    }

    /**
     * Mutate the protected {@code val} field (which holds the internal storage) via reflection.
     * The method {@code isCaseInsensitive()} must still return {@code false},
     * proving it does not depend on the internal map state.
     */
    @Test
    void afterInternalStateMutationIsCaseInsensitiveRemainsFalse() throws Exception {
        CompactMap<String, Object> map = new CompactMap<>() {

            @Override
            protected int compactSize() {
                return 3;
            }

            @Override
            protected Map<String, Object> getNewMap() {
                return new HashMap<>();
            }
        };
        // Use reflection to set the protected field 'val' to a non‑empty HashMap
        Field valField = CompactMap.class.getDeclaredField("val");
        valField.setAccessible(true);
        Map<String, Object> nonEmpty = new HashMap<>();
        nonEmpty.put("key", "value");
        valField.set(map, nonEmpty);
        // Verify the method still reports case‑sensitive
        assertFalse(invokeIsCaseInsensitive(map), "isCaseInsensitive() must remain false even after internal state mutation");
    }

    /**
     * Subclass that makes {@code isCaseInsensitive()} public, allowing external callers
     * to query the setting directly.  The default implementation should still return false.
     */
    @Test
    void viaSubclassExposureIsCaseInsensitiveIsFalse() {
        ExposedCompactMap<String, Object> map = new ExposedCompactMap<>();
        assertFalse(map.isCaseInsensitivePublic(), "Public exposure of isCaseInsensitive() should return false by default");
    }
}
