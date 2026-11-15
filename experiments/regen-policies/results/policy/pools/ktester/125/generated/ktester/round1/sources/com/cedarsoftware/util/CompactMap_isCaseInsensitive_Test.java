package com.cedarsoftware.util;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.HashMap;
import java.util.Map;

// @ExtendWith(MockitoExtension.class)
class CompactMap_isCaseInsensitive_Test {

    /**
     * Returns {@code true} if the supplied method has a {@code void}
     * return type.
     *
     * @param method the method to inspect; never {@code null}
     * @return {@code true} if the method returns {@code void}
     */
    public static boolean returnsVoid(Method method) {
        Objects.requireNonNull(method, "method");
        return method.getReturnType() == void.class;
    }

    /**
     * Finds the first method in the given class (including its super‑classes)
     * that matches the supplied predicate.
     *
     * @param clazz     the class to search; never {@code null}
     * @param predicate a predicate that tests methods; never {@code null}
     * @return the first matching {@link Method} or {@code null} if none match
     */
    public static Method findMethod(Class<?> clazz, Predicate<Method> predicate) {
        Objects.requireNonNull(clazz, "clazz");
        Objects.requireNonNull(predicate, "predicate");
        for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (predicate.test(m)) {
                    return m;
                }
            }
        }
        return null;
    }

    /**
     * Checks whether a method with the given name and parameter types is
     * present on the supplied class (or any of its super‑classes).
     *
     * @param clazz          the class to inspect; never {@code null}
     * @param methodName     the name of the method; never {@code null}
     * @param parameterTypes the parameter types of the method; may be empty
     * @return {@code true} if such a method exists
     */
    public static boolean isMethodPresent(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        Objects.requireNonNull(clazz, "clazz");
        Objects.requireNonNull(methodName, "methodName");
        try {
            clazz.getMethod(methodName, parameterTypes);
            return true;
        } catch (NoSuchMethodException ex) {
            try {
                clazz.getDeclaredMethod(methodName, parameterTypes);
                return true;
            } catch (NoSuchMethodException ignored) {
                return false;
            }
        }
    }

    /**
     * Entry point used by the execution harness.
     */
//     public static void main(String[] args) {
//         defaultIsCaseInsensitiveReturnsFalse();
//         overriddenIsCaseInsensitiveReturnsTrue();
//         System.out.println("All CompactMap case‑insensitivity checks passed.");
//     }

    /**
     * Verify the default implementation of {@code isCaseInsensitive()}
     * returns {@code false}.  The {@code CompactMap} constructor requires
     * a {@code compactSize()} of at least 2 and a backing map, so we provide
     * minimal overrides for those abstract‑like methods.
     */
//     @Test
//     void defaultIsCaseInsensitiveReturnsFalse() {
//         CompactMap<String, Object> map = new CompactMap<String, Object>() {
// 
//             @Override
//             protected int compactSize() {
//                 // satisfy constructor check
//                 return 3;
//             }
// 
//             @Override
//             protected Map<String, Object> getNewMap() {
//                 // simple backing map
//                 return new HashMap<>();
//             }
//         };
//         if (map.isCaseInsensitive()) {
//             throw new AssertionError("Default CompactMap should be case‑sensitive, therefore isCaseInsensitive() must be false");
//         }
//     }

    /**
     * Verify that a subclass can override {@code isCaseInsensitive()} and that
     * the overridden value is observed.  This proves the method is not final
     * and can be customized by generated subclasses.
     */
//     @Test
//     void overriddenIsCaseInsensitiveReturnsTrue() {
//         CompactMap<String, Object> map = new CompactMap<String, Object>() {
// 
//             @Override
//             protected int compactSize() {
//                 return 4;
//             }
// 
//             @Override
//             protected Map<String, Object> getNewMap() {
//                 return new HashMap<>();
//             }
// 
//             @Override
//             protected boolean isCaseInsensitive() {
//                 // custom behaviour for this test
//                 return true;
//             }
//         };
//         if (!map.isCaseInsensitive()) {
//             throw new AssertionError("When overridden to return true, isCaseInsensitive() should reflect the overridden value");
//         }
//     }
}