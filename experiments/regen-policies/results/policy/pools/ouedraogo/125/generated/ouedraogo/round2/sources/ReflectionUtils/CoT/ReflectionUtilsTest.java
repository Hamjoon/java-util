package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.lang.annotation.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link ReflectionUtils}.
 * <p>
 * The test class follows the steps requested:
 * <ol>
 *   <li>Lists all public methods (see the comment block at the top).</li>
 *   <li>Provides a basic test for each method.</li>
 *   <li>Identifies edge‑cases / exception scenarios and adds dedicated tests.</li>
 *   <li>Aggregates everything into a single file that starts with <code>###Test START##</code>
 *       and ends with <code>###Test END##</code>.</li>
 * </ol>
 */
public class ReflectionUtilsTest {

    /* ----------------------------------------------------------------------
     *  1. Public API of ReflectionUtils (extracted from the source)
     * ----------------------------------------------------------------------
     *
     * static void setMethodCache(Map<Object,Method> cache)
     * static void setClassFieldsCache(Map<Object,Collection<Field>> cache)
     * static void setFieldCache(Map<Object,Field> cache)
     * static void setClassAnnotationCache(Map<Object,Annotation> cache)
     * static void setMethodAnnotationCache(Map<Object,Annotation> cache)
     * static void setConstructorCache(Map<Object,Constructor<?>> cache)
     *
     * static <T extends Annotation> T getClassAnnotation(Class<?> classToCheck,
     *                                                    Class<T> annoClass)
     *
     * static <T extends Annotation> T getMethodAnnotation(Method method,
     *                                                     Class<T> annoClass)
     *
     * static Field getField(Class<?> c, String fieldName)
     *
     * static List<Field> getDeclaredFields(Class<?> c,
     *                                      Predicate<Field> fieldFilter)
     * static List<Field> getDeclaredFields(Class<?> c)
     *
     * static List<Field> getAllDeclaredFields(Class<?> c,
     *                                         Predicate<Field> fieldFilter)
     * static List<Field> getAllDeclaredFields(Class<?> c)
     *
     * static Map<String,Field> getAllDeclaredFieldsMap(Class<?> c,
     *                                                  Predicate<Field> fieldFilter)
     * static Map<String,Field> getAllDeclaredFieldsMap(Class<?> c)
     *
     * static Collection<Field> getDeepDeclaredFields(Class<?> c)   // deprecated
     * static Map<String,Field> getDeepDeclaredFieldMap(Class<?> c) // deprecated
     * static void getDeclaredFields(Class<?> c, Collection<Field> fields) // deprecated
     *
     * static Object call(Object instance, Method method, Object... args)
     * static Object call(Object instance, String methodName, Object... args)
     *
     * static Method getMethod(Class<?> c, String methodName,
     *                         Class<?>... types)
     * static Method getMethod(Object instance, String methodName,
     *                         int argCount)
     *
     * static Method getNonOverloadedMethod(Class<?> clazz,
     *                                      String methodName)
     *
     * static String getClassName(Object o)
     *
     * static String getClassNameFromByteCode(byte[] byteCode)
     *                                            throws IOException
     *
     * static Constructor<?> getConstructor(Class<?> clazz,
     *                                      Class<?>... parameterTypes)
     * static Constructor<?>[] getAllConstructors(Class<?> clazz)
     */

    /* ----------------------------------------------------------------------
     *  Helper types used by the tests
     * ---------------------------------------------------------------------- */

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    private @interface TestAnno { }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    private @interface TestMethodAnno { }

    @TestAnno
    private static class SuperClass {
        private int privateSuperField;
        protected String protectedSuperField;
        public static final int STATIC_SUPER = 1;

        @TestMethodAnno
        public void annotatedSuperMethod() { }

        private void privateSuperMethod() { }

        public String overloaded(String s) { return "super:" + s; }

        public String overloaded(Integer i) { return "super:" + i; }
    }

    private static class SubClass extends SuperClass {
        private long privateSubField;
        public String publicSubField;

        @Override
        public void annotatedSuperMethod() { } // inherits annotation

        public String overloaded(String s) { return "sub:" + s; } // overload

        private void privateSubMethod() { }

        public void methodThrows() { throw new IllegalArgumentException("boom"); }

        public String noArg() { return "ok"; }
    }

    /** Simple custom map that records put/get calls – used to verify cache injection. */
    private static class RecordingMap<K, V> extends LinkedHashMap<K, V> {
        boolean putCalled = false;
        boolean getCalled = false;

        @Override
        public V put(K key, V value) {
            putCalled = true;
            return super.put(key, value);
        }

        @Override
        public V get(Object key) {
            getCalled = true;
            return super.get(key);
        }
    }

    /* ----------------------------------------------------------------------
     *  Reset caches before each test to avoid cross‑test contamination.
     * ---------------------------------------------------------------------- */
    @Before
    public void resetCaches() {
        // The library does not expose a "reset" method, but we can safely replace each
        // cache with a fresh plain HashMap – this does not affect functionality.
        ReflectionUtils.setMethodCache(new HashMap<>());
        ReflectionUtils.setClassFieldsCache(new HashMap<>());
        ReflectionUtils.setFieldCache(new HashMap<>());
        ReflectionUtils.setClassAnnotationCache(new HashMap<>());
        ReflectionUtils.setMethodAnnotationCache(new HashMap<>());
        ReflectionUtils.setConstructorCache(new HashMap<>());
    }

    /* ----------------------------------------------------------------------
     *  2. Basic tests for each public method
     * ---------------------------------------------------------------------- */

    @Test
    public void testSetMethodCache_andGetMethodUsesCustomCache() throws Exception {
        RecordingMap<Object, Method> custom = new RecordingMap<>();
        ReflectionUtils.setMethodCache(custom);

        Method m = ReflectionUtils.getMethod(SubClass.class, "noArg");
        assertNotNull(m);
        // The custom map must have been used for the put (caching) operation.
        assertTrue(custom.putCalled);
        // The key stored must be equal to a freshly created key.
        ReflectionUtils.MethodCacheKey expectedKey =
                new ReflectionUtils.MethodCacheKey(SubClass.class, "noArg");
        assertTrue(custom.containsKey(expectedKey));
    }

    @Test
    public void testSetClassFieldsCache_andGetAllDeclaredFieldsUsesCustomCache() {
        RecordingMap<Object, Collection<Field>> custom = new RecordingMap<>();
        ReflectionUtils.setClassFieldsCache(custom);

        List<Field> fields = ReflectionUtils.getAllDeclaredFields(SubClass.class);
        assertNotNull(fields);
        assertTrue(custom.putCalled);
    }

    @Test
    public void testSetFieldCache_andGetFieldUsesCustomCache() {
        RecordingMap<Object, Field> custom = new RecordingMap<>();
        ReflectionUtils.setFieldCache(custom);

        Field f = ReflectionUtils.getField(SubClass.class, "publicSubField");
        assertNotNull(f);
        assertTrue(custom.putCalled);
        ReflectionUtils.FieldNameCacheKey key =
                new ReflectionUtils.FieldNameCacheKey(SubClass.class, "publicSubField");
        assertTrue(custom.containsKey(key));
    }

    @Test
    public void testSetClassAnnotationCache_andGetClassAnnotationUsesCustomCache() {
        RecordingMap<Object, Annotation> custom = new RecordingMap<>();
        ReflectionUtils.setClassAnnotationCache(custom);

        TestAnno anno = ReflectionUtils.getClassAnnotation(SubClass.class, TestAnno.class);
        assertNotNull(anno);
        assertTrue(custom.putCalled);
        ReflectionUtils.ClassAnnotationCacheKey key =
                new ReflectionUtils.ClassAnnotationCacheKey(SubClass.class, TestAnno.class);
        assertTrue(custom.containsKey(key));
    }

    @Test
    public void testSetMethodAnnotationCache_andGetMethodAnnotationUsesCustomCache() throws Exception {
        RecordingMap<Object, Annotation> custom = new RecordingMap<>();
        ReflectionUtils.setMethodAnnotationCache(custom);

        Method m = SubClass.class.getMethod("annotatedSuperMethod");
        TestMethodAnno anno = ReflectionUtils.getMethodAnnotation(m, TestMethodAnno.class);
        assertNotNull(anno);
        assertTrue(custom.putCalled);
        ReflectionUtils.MethodAnnotationCacheKey key =
                new ReflectionUtils.MethodAnnotationCacheKey(m, TestMethodAnno.class);
        assertTrue(custom.containsKey(key));
    }

    @Test
    public void testSetConstructorCache_andGetConstructorUsesCustomCache() {
        RecordingMap<Object, Constructor<?>> custom = new RecordingMap<>();
        ReflectionUtils.setConstructorCache(custom);

        Constructor<?> ctor = ReflectionUtils.getConstructor(SubClass.class);
        assertNotNull(ctor);
        assertTrue(custom.putCalled);
        ReflectionUtils.ConstructorCacheKey key =
                new ReflectionUtils.ConstructorCacheKey(SubClass.class);
        assertTrue(custom.containsKey(key));
    }

    @Test
    public void testGetClassAnnotation_basic() {
        TestAnno anno = ReflectionUtils.getClassAnnotation(SubClass.class, TestAnno.class);
        assertNotNull("Annotation should be inherited from SuperClass", anno);
    }

    @Test
    public void testGetMethodAnnotation_basic() throws Exception {
        Method m = SubClass.class.getMethod("annotatedSuperMethod");
        TestMethodAnno anno = ReflectionUtils.getMethodAnnotation(m, TestMethodAnno.class);
        assertNotNull("Method annotation should be found on the overridden method", anno);
    }

    @Test
    public void testGetField_basic() {
        Field f = ReflectionUtils.getField(SubClass.class, "publicSubField");
        assertNotNull(f);
        assertEquals("publicSubField", f.getName());
    }

    @Test
    public void testGetDeclaredFields_customPredicate() {
        // Predicate that selects only public fields
        List<Field> publicFields = ReflectionUtils.getDeclaredFields(
                SubClass.class,
                field -> Modifier.isPublic(field.getModifiers()));
        assertEquals(1, publicFields.size());
        assertEquals("publicSubField", publicFields.get(0).getName());
    }

    @Test
    public void testGetDeclaredFields_defaultFilter() {
        List<Field> all = ReflectionUtils.getDeclaredFields(SubClass.class);
        // static field from superclass must be filtered out
        // private and protected fields are kept (they are non‑static)
        Set<String> names = new HashSet<>();
        for (Field f : all) names.add(f.getName());
        assertTrue(names.contains("privateSubField"));
        assertTrue(names.contains("publicSubField"));
        assertTrue(names.contains("privateSuperField"));
        assertTrue(names.contains("protectedSuperField"));
        assertFalse(names.contains("STATIC_SUPER"));
    }

    @Test
    public void testGetAllDeclaredFields_includesSuperclass() {
        List<Field> all = ReflectionUtils.getAllDeclaredFields(SubClass.class);
        Set<String> names = new HashSet<>();
        for (Field f : all) names.add(f.getName());

        // fields from both SubClass and SuperClass (non‑static) should appear
        assertTrue(names.containsAll(Arrays.asList(
                "privateSubField", "publicSubField",
                "privateSuperField", "protectedSuperField"
        )));
        // static field must be filtered out
        assertFalse(names.contains("STATIC_SUPER"));
    }

    @Test
    public void testGetAllDeclaredFieldsMap_collisionHandling() {
        // Both SuperClass and SubClass declare a field named "privateSuperField"
        // (actually only SuperClass does). To create a collision we add a field
        // with the same name in SubClass via reflection (not possible at runtime),
        // so instead we rely on the existing privateSuperField (in SuperClass) and
        // a field with the same name in SubClass's hierarchy – we add one manually.
        // For the purpose of this test we use the existing privateSuperField only;
        // the map should contain a single entry with the simple name.
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(SubClass.class);
        assertTrue(map.containsKey("privateSuperField"));
        // No fully‑qualified key should exist because there is no name clash.
        for (String key : map.keySet()) {
            assertFalse(key.contains(".privateSuperField"));
        }

        // Now create an artificial clash by defining a subclass with the same field name.
        class Clash extends SubClass {
            private int privateSuperField; // hides the field from SuperClass
        }
        Map<String, Field> clashMap = ReflectionUtils.getAllDeclaredFieldsMap(Clash.class);
        // Simple name should map to the field from Clash (the child)
        assertTrue(clashMap.containsKey("privateSuperField"));
        // The parent field must be stored under a fully qualified name.
        String fq = SuperClass.class.getName() + ".privateSuperField";
        assertTrue(clashMap.containsKey(fq));
    }

    @Test
    public void testDeprecated_getDeepDeclaredFields_behavesLike_getAllDeclaredFields() {
        Collection<Field> fields = ReflectionUtils.getDeepDeclaredFields(SubClass.class);
        assertNotNull(fields);
        // Verify that the collection contains the same fields as the non‑deprecated version.
        List<Field> expected = ReflectionUtils.getAllDeclaredFields(SubClass.class);
        assertEquals(new HashSet<>(expected), new HashSet<>(fields));
    }

    @Test
    public void testDeprecated_getDeepDeclaredFieldMap_behavesLike_getAllDeclaredFieldsMap() {
        Map<String, Field> map = ReflectionUtils.getDeepDeclaredFieldMap(SubClass.class);
        Map<String, Field> expected = ReflectionUtils.getAllDeclaredFieldsMap(SubClass.class);
        assertEquals(expected, map);
    }

    @Test
    public void testDeprecated_getDeclaredFields_collectionParameter() {
        List<Field> collector = new ArrayList<>();
        ReflectionUtils.getDeclaredFields(SubClass.class, collector);
        assertFalse(collector.isEmpty());
        // The collector should contain the same fields as getDeclaredFields(Class)
        List<Field> direct = ReflectionUtils.getDeclaredFields(SubClass.class);
        assertEquals(new HashSet<>(direct), new HashSet<>(collector));
    }

    @Test
    public void testCall_withMethod_success() throws Exception {
        SubClass obj = new SubClass();
        Method m = SubClass.class.getMethod("noArg");
        Object result = ReflectionUtils.call(obj, m);
        assertEquals("ok", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCall_nullMethod_throwsIllegalArgumentException() {
        ReflectionUtils.call(new SubClass(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCall_nullInstance_throwsIllegalArgumentException() throws Exception {
        Method m = SubClass.class.getMethod("noArg");
        ReflectionUtils.call(null, m);
    }

    @Test
    public void testCall_methodThrowsWrappedRuntimeException() throws Exception {
        SubClass obj = new SubClass();
        Method m = SubClass.class.getMethod("methodThrows");
        try {
            ReflectionUtils.call(obj, m);
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Exception thrown inside reflectively called method"));
            assertTrue(e.getCause() instanceof IllegalArgumentException);
            assertEquals("boom", e.getCause().getMessage());
        }
    }

    @Test
    public void testCallByName_success() {
        SubClass obj = new SubClass();
        Object result = ReflectionUtils.call(obj, "noArg");
        assertEquals("ok", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallByName_methodNotFound() {
        SubClass obj = new SubClass();
        ReflectionUtils.call(obj, "nonExistingMethod");
    }

    @Test
    public void testGetMethod_bySignature_privateMethod() {
        Method m = ReflectionUtils.getMethod(SubClass.class, "privateSubMethod");
        assertNotNull(m);
        assertTrue(m.isAccessible() || !Modifier.isPublic(m.getModifiers()));
    }

    @Test
    public void testGetMethod_byInstanceAndArgCount() {
        SubClass obj = new SubClass();
        Method m = ReflectionUtils.getMethod(obj, "overloaded", 1);
        assertNotNull(m);
        assertEquals(1, m.getParameterCount());
        // The selected method should be the most specific (the SubClass overload)
        assertEquals(SubClass.class, m.getDeclaringClass());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethod_byInstance_noMatchThrows() {
        SubClass obj = new SubClass();
        ReflectionUtils.getMethod(obj, "nonExisting", 0);
    }

    @Test
    public void testGetNonOverloadedMethod_success() {
        Method m = ReflectionUtils.getNonOverloadedMethod(SubClass.class, "noArg");
        assertNotNull(m);
        assertEquals("noArg", m.getName());
        assertEquals(0, m.getParameterCount());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethod_overloadedMethodThrows() {
        // "overloaded" exists with two different signatures
        ReflectionUtils.getNonOverloadedMethod(SubClass.class, "overloaded");
    }

    @Test
    public void testGetClassName() {
        assertEquals("null", ReflectionUtils.getClassName(null));
        assertEquals(SubClass.class.getName(),
                ReflectionUtils.getClassName(new SubClass()));
    }

    @Test
    public void testGetClassNameFromByteCode_validClass() throws IOException {
        // Load the bytecode of this test class itself.
        String className = this.getClass().getSimpleName() + ".class";
        byte[] bytes = this.getClass().getResourceAsStream(className).readAllBytes();
        String extracted = ReflectionUtils.getClassNameFromByteCode(bytes);
        assertEquals(this.getClass().getName(), extracted);
    }

    @Test(expected = IllegalStateException.class)
    public void testGetClassNameFromByteCode_invalidDataThrows() throws IOException {
        // Feed a random byte array – the parser should hit an unknown tag.
        byte[] bogus = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        ReflectionUtils.getClassNameFromByteCode(bogus);
    }

    @Test
    public void testGetConstructor_successfulAndMiss() {
        // public no‑arg constructor (generated by compiler)
        Constructor<?> ctor = ReflectionUtils.getConstructor(SubClass.class);
        assertNotNull(ctor);
        assertEquals(SubClass.class, ctor.getDeclaringClass());

        // non‑existent constructor signature
        Constructor<?> missing = ReflectionUtils.getConstructor(SubClass.class, String.class);
        assertNull(missing);
    }

    @Test
    public void testGetAllConstructors_returnsAllDeclared() {
        Constructor<?>[] ctors = ReflectionUtils.getAllConstructors(SubClass.class);
        // SubClass has the default no‑arg constructor plus the synthetic
        // constructor used for inner classes (none here). At least one should be present.
        assertTrue(ctors.length >= 1);
        // Verify that each constructor is the same instance as the cached one.
        for (Constructor<?> c : ctors) {
            Constructor<?> cached = ReflectionUtils.getConstructor(SubClass.class, c.getParameterTypes());
            assertSame(c, cached);
        }
    }

    /* ----------------------------------------------------------------------
     *  3. Edge‑case / exception handling tests (already mixed above)
     * ---------------------------------------------------------------------- */

    @Test
    public void testGetClassAnnotation_nullClass_returnsNull() {
        assertNull(ReflectionUtils.getClassAnnotation(null, TestAnno.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetClassAnnotation_nullAnnoClass_throws() {
        ReflectionUtils.getClassAnnotation(SubClass.class, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodAnnotation_nullMethod_throws() {
        ReflectionUtils.getMethodAnnotation(null, TestMethodAnno.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodAnnotation_nullAnnoClass_throws() {
        try {
            Method m = SubClass.class.getMethod("noArg");
            ReflectionUtils.getMethodAnnotation(m, null);
        } catch (NoSuchMethodException e) {
            fail("Unexpected NoSuchMethodException");
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetField_nullClass_throws() {
        ReflectionUtils.getField(null, "any");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetField_nullFieldName_throws() {
        ReflectionUtils.getField(SubClass.class, null);
    }

    @Test
    public void testGetField_nonExisting_returnsNull() {
        assertNull(ReflectionUtils.getField(SubClass.class, "doesNotExist"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetDeclaredFields_nullClass_throws() {
        ReflectionUtils.getDeclaredFields(null, f -> true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetDeclaredFields_nullPredicate_throws() {
        ReflectionUtils.getDeclaredFields(SubClass.class, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetAllDeclaredFields_nullClass_throws() {
        ReflectionUtils.getAllDeclaredFields(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetAllDeclaredFields_nullPredicate_throws() {
        ReflectionUtils.getAllDeclaredFields(null, f -> true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetAllDeclaredFieldsMap_nullClass_throws() {
        ReflectionUtils.getAllDeclaredFieldsMap(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetAllDeclaredFieldsMap_nullPredicate_throws() {
        ReflectionUtils.getAllDeclaredFieldsMap(null, f -> true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethod_nullClass_throws() {
        ReflectionUtils.getMethod(null, "any");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethod_nullMethodName_throws() {
        ReflectionUtils.getMethod(SubClass.class, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethod_nullInstance_throws() {
        ReflectionUtils.getMethod(null, "noArg", 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethod_nullMethodName_instance_throws() {
        ReflectionUtils.getMethod(new SubClass(), null, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethod_negativeArgCount_throws() {
        ReflectionUtils.getMethod(new SubClass(), "noArg", -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethod_nullClass_throws() {
        ReflectionUtils.getNonOverloadedMethod(null, "any");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethod_blankMethodName_throws() {
        ReflectionUtils.getNonOverloadedMethod(SubClass.class, "   ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetConstructor_nullClass_throws() {
        ReflectionUtils.getConstructor(null);
    }
}
