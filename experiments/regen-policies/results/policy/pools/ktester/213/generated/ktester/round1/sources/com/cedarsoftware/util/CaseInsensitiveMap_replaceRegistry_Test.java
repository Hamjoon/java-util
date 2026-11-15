package com.cedarsoftware.util;

import java.lang.reflect.Method;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

// @ExtendWith(MockitoExtension.class)
class CaseInsensitiveMap_replaceRegistry_Test {

//     @BeforeEach
    void setupBeforeEach() {
        // Ensure a clean registry before each test.
//         CaseInsensitiveMap.replaceRegistry(Collections.emptyList());
    }

//     @AfterEach
    void teardownAfterEach() {
        // Reset to an empty (default) registry so later tests start fresh.
//         CaseInsensitiveMap.replaceRegistry(Collections.emptyList());
    }

    /**
     * Returns {@code true} if the supplied method has a {@code void}
     * return type.
     *
     * @param method the method to inspect; never {@code null}
     * @return {@code true} if the method returns {@code void}
     */
    public static boolean returnsVoid(Method method) {
        return method.getReturnType() == void.class;
    }

    /**
     * Attempts to locate a method with the given name and parameter types
     * on the supplied class.  If the method is not found an empty
     * {@code Optional} is returned.
     *
     * @param clazz          the class to search; never {@code null}
     * @param methodName     the name of the method; never {@code null}
     * @param parameterTypes the parameter types of the method; may be empty
     * @return an {@code Optional} containing the {@code Method} if found
     */
//     public static Optional<Method> findMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
//         try {
//             Method method = clazz.getDeclaredMethod(methodName, parameterTypes);
//             return Optional.of(method);
//         } catch (NoSuchMethodException ex) {
//             return Optional.empty();
//         }
//     }

    /**
     * Returns {@code true} if a method with the given signature is present
     * on the supplied class.
     *
     * @param clazz          the class to inspect; never {@code null}
     * @param methodName     the name of the method; never {@code null}
     * @param parameterTypes the parameter types of the method; may be empty
     * @return {@code true} if such a method exists
     */
    public static boolean isMethodPresent(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        return findMethod(clazz, methodName, parameterTypes).isPresent();
    }

    public static boolean isPublic(Method method) {
        return Modifier.isPublic(method.getModifiers());
    }

    public static boolean isStatic(Method method) {
        return Modifier.isStatic(method.getModifiers());
    }

    public static boolean isAbstract(Method method) {
        return Modifier.isAbstract(method.getModifiers());
    }

    public static boolean isPrivate(Method method) {
        return Modifier.isPrivate(method.getModifiers());
    }

    public static boolean isPackagePrivate(Method method) {
        int mods = method.getModifiers();
        return !(Modifier.isPublic(mods) || Modifier.isProtected(mods) || Modifier.isPrivate(mods));
    }

    public static boolean isProtected(Method method) {
        return Modifier.isProtected(method.getModifiers());
    }

    public static boolean isPublicOrProtected(Method method) {
        int mods = method.getModifiers();
        return Modifier.isPublic(mods) || Modifier.isProtected(mods);
    }

    public static boolean isPublicOrPackagePrivate(Method method) {
        int mods = method.getModifiers();
        return Modifier.isPublic(mods) || isPackagePrivate(method);
    }

    public static boolean isPublicOrProtectedOrPackagePrivate(Method method) {
        int mods = method.getModifiers();
        return Modifier.isPublic(mods) || Modifier.isProtected(mods) || isPackagePrivate(method);
    }

    public static boolean isPublicOrProtectedOrPrivate(Method method) {
        int mods = method.getModifiers();
        return Modifier.isPublic(mods) || Modifier.isProtected(mods) || Modifier.isPrivate(mods);
    }

    public static boolean isAbstractOrInterface(Class<?> clazz) {
        return Modifier.isAbstract(clazz.getModifiers()) || clazz.isInterface();
    }

    public static boolean isInterface(Class<?> clazz) {
        return clazz.isInterface();
    }

    public static boolean isEnum(Class<?> clazz) {
        return clazz.isEnum();
    }

    public static boolean isRecord(Class<?> clazz) {
        // Records are available from Java 16 onwards.
        try {
            return (boolean) Class.class.getMethod("isRecord").invoke(clazz);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    public static boolean isAnnotation(Class<?> clazz) {
        return clazz.isAnnotation();
    }

    public static boolean isSynthetic(Class<?> clazz) {
        return clazz.isSynthetic();
    }

    public static boolean isBridge(Method method) {
        return method.isBridge();
    }

    public static boolean isDefaultMethod(Method method) {
        return method.isDefault();
    }

    public static boolean isAnnotationMethod(Method method) {
        return method.getAnnotations().length > 0;
    }

    public static boolean isConstructorPresent(Class<?> clazz, Class<?>... parameterTypes) {
        try {
            Constructor<?> ctor = clazz.getDeclaredConstructor(parameterTypes);
            return ctor != null;
        } catch (NoSuchMethodException ex) {
            return false;
        }
    }

    public static boolean isAnnotationPresent(Class<?> clazz, Class<? extends Annotation> annotation) {
        return clazz.isAnnotationPresent(annotation);
    }

    /**
     * -----------------------------------------------------------------
     *  Positive test – a well‑formed registry is accepted without exception.
     *  ----------------------------------------------------------------- **
     */
//     @Test
//     void replaceRegistryAcceptsValidList() {
//         List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> registry = new ArrayList<>();
//         // HashMap entry
//         registry.add(new AbstractMap.SimpleEntry<>(HashMap.class, (Function<Integer, Map<?, ?>>) size -> new HashMap<>(size)));
//         // TreeMap entry
//         registry.add(new AbstractMap.SimpleEntry<>(TreeMap.class, (Function<Integer, Map<?, ?>>) size -> new TreeMap<>()));
//         assertDoesNotThrow(() -> CaseInsensitiveMap.replaceRegistry(registry));
//     }

    /**
     * -----------------------------------------------------------------
     *  Null argument checks – the method must reject null inputs.
     *  ----------------------------------------------------------------- **
     */
//     @Test
//     void replaceRegistryThrowsWhenListIsNull() {
//         NullPointerException ex = assertThrows(NullPointerException.class, () -> CaseInsensitiveMap.replaceRegistry(null));
//         assertTrue(ex.getMessage().contains("New registry list cannot be null"));
//     }

//     @Test
//     void replaceRegistryThrowsWhenEntryIsNull() {
//         List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> registry = new ArrayList<>();
//         // explicit null entry
//         registry.add(null);
//         NullPointerException ex = assertThrows(NullPointerException.class, () -> CaseInsensitiveMap.replaceRegistry(registry));
//         assertTrue(ex.getMessage().contains("Registry entries cannot be null"));
//     }

//     @Test
//     void replaceRegistryThrowsWhenEntryKeyIsNull() {
//         List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> registry = new ArrayList<>();
//         registry.add(new AbstractMap.SimpleEntry<>(null, (Function<Integer, Map<?, ?>>) size -> new HashMap<>()));
//         NullPointerException ex = assertThrows(NullPointerException.class, () -> CaseInsensitiveMap.replaceRegistry(registry));
//         assertTrue(ex.getMessage().contains("Registry entry key (Class) cannot be null"));
//     }

//     @Test
//     void replaceRegistryThrowsWhenEntryValueIsNull() {
//         List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> registry = new ArrayList<>();
//         registry.add(new AbstractMap.SimpleEntry<>(LinkedHashMap.class, null));
//         NullPointerException ex = assertThrows(NullPointerException.class, () -> CaseInsensitiveMap.replaceRegistry(registry));
//         assertTrue(ex.getMessage().contains("Registry entry value (Function) cannot be null"));
//     }

    /**
     * -----------------------------------------------------------------
     *  Duplicate class detection – the method must reject a list that
     *  contains the same map class more than once.
     *  ----------------------------------------------------------------- **
     */
//     @Test
//     void replaceRegistryThrowsWhenDuplicateClass() {
//         List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> registry = new ArrayList<>();
//         // First entry for HashMap
//         registry.add(new AbstractMap.SimpleEntry<>(HashMap.class, (Function<Integer, Map<?, ?>>) size -> new HashMap<>(size)));
//         // Second entry with the same Class – should trigger IllegalArgumentException
//         registry.add(new AbstractMap.SimpleEntry<>(HashMap.class, (Function<Integer, Map<?, ?>>) size -> new HashMap<>(size)));
//         IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> CaseInsensitiveMap.replaceRegistry(registry));
//         assertTrue(ex.getMessage().contains("Duplicate map type"));
//     }

    /**
     * -----------------------------------------------------------------
     *  validateMappings – non‑Map key (entry key does not implement Map)
     *  ----------------------------------------------------------------- **
     */
//     @Test
//     void replaceRegistryThrowsWhenKeyIsNotMapClass() {
//         List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> registry = new ArrayList<>();
//         registry.add(new AbstractMap.SimpleEntry<>(String.class, (Function<Integer, Map<?, ?>>) size -> new HashMap<>()));
//         IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> CaseInsensitiveMap.replaceRegistry(registry));
//         assertTrue(ex.getMessage().contains("does not implement Map"));
//     }

    /**
     * -----------------------------------------------------------------
     *  validateMappings – function returns null
     *  ----------------------------------------------------------------- **
     */
//     @Test
//     void replaceRegistryThrowsWhenFunctionReturnsNull() {
//         List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> registry = new ArrayList<>();
//         registry.add(new AbstractMap.SimpleEntry<>(HashMap.class, (Function<Integer, Map<?, ?>>) size -> null));
//         IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> CaseInsensitiveMap.replaceRegistry(registry));
//         assertTrue(ex.getMessage().contains("function returned null"));
//     }

    /**
     * -----------------------------------------------------------------
     *  validateMappings – mismatched map type (function returns wrong concrete class)
     *  ----------------------------------------------------------------- **
     */
//     @Test
//     void replaceRegistryThrowsWhenReturnedMapTypeMismatchesKey() {
//         List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> registry = new ArrayList<>();
//         registry.add(new AbstractMap.SimpleEntry<>(HashMap.class, (Function<Integer, Map<?, ?>>) size -> new Hashtable<>()));
//         IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> CaseInsensitiveMap.replaceRegistry(registry));
//         assertTrue(ex.getMessage().contains("does not match the declared class"));
//     }

    /**
     * -----------------------------------------------------------------
     *  validateMappings – order violation (concurrent map before its non‑concurrent counterpart)
     *  ----------------------------------------------------------------- **
     */
//     @Test
//     void replaceRegistryThrowsWhenOrderIsInvalid() {
//         List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> registry = new ArrayList<>();
//         // Incorrect order: ConcurrentHashMap first, then HashMap
//         registry.add(new AbstractMap.SimpleEntry<>(ConcurrentHashMap.class, (Function<Integer, Map<?, ?>>) size -> new ConcurrentHashMap<>()));
//         registry.add(new AbstractMap.SimpleEntry<>(HashMap.class, (Function<Integer, Map<?, ?>>) size -> new HashMap<>()));
//         IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> CaseInsensitiveMap.replaceRegistry(registry));
//         assertTrue(ex.getMessage().contains("Invalid registry order"));
//     }
}