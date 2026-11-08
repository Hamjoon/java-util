package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link ReflectionUtils}.
 * <p>
 * The tests exercise every public method, verify caching behaviour,
 * and also cover edge‑cases such as null arguments and exception handling.
 * </p>
 */
public class ReflectionUtilsTest {

    /* --------------------------------------------------------------------- */
    /*  Test Annotations and Helper Classes                                  */
    /* --------------------------------------------------------------------- */

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface ClassAnno {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface MethodAnno {}

    @ClassAnno
    interface AnnotatedInterface {
        @MethodAnno
        void interfaceMethod();
    }

    static class SuperClass {
        private String privateSuperField = "super";
        protected int protectedSuperField = 42;
        public static int staticSuperField = 99;
        public void superMethod() {}
    }

    static class SubClass extends SuperClass implements AnnotatedInterface {
        private String privateSubField = "sub";

        @Override
        public void interfaceMethod() {}

        // overloaded methods for getMethod(Object, String, int) tests
        public void overloaded() {}
        protected void overloaded(String s) {}
        private void overloaded(String s, int i) {}
    }

    static class NoDefaultConstructor {
        private NoDefaultConstructor() {}
        public NoDefaultConstructor(String s) {}
    }

    /* --------------------------------------------------------------------- */
    /*  Test Infrastructure                                                  */
    /* --------------------------------------------------------------------- */

    // custom caches that we can inspect after each test
    private Map<Object, Method> methodCache;
    private Map<Object, Collection<Field>> fieldsCache;
    private Map<Object, Field> fieldNameCache;
    private Map<Object, Annotation> classAnnoCache;
    private Map<Object, Annotation> methodAnnoCache;
    private Map<Object, Constructor<?>> ctorCache;

    @Before
    public void setUp() {
        methodCache      = new HashMap<>();
        fieldsCache      = new HashMap<>();
        fieldNameCache   = new HashMap<>();
        classAnnoCache   = new HashMap<>();
        methodAnnoCache  = new HashMap<>();
        ctorCache        = new HashMap<>();

        ReflectionUtils.setMethodCache(methodCache);
        ReflectionUtils.setClassFieldsCache(fieldsCache);
        ReflectionUtils.setFieldCache(fieldNameCache);
        ReflectionUtils.setClassAnnotationCache(classAnnoCache);
        ReflectionUtils.setMethodAnnotationCache(methodAnnoCache);
        ReflectionUtils.setConstructorCache(ctorCache);
    }

    @After
    public void tearDown() {
        // Reset to default caches to avoid side effects for other tests
        ReflectionUtils.setMethodCache(new LRUCache<>(1500));
        ReflectionUtils.setClassFieldsCache(new LRUCache<>(1500));
        ReflectionUtils.setFieldCache(new LRUCache<>(1500 * 10));
        ReflectionUtils.setClassAnnotationCache(new LRUCache<>(1500));
        ReflectionUtils.setMethodAnnotationCache(new LRUCache<>(1500));
        ReflectionUtils.setConstructorCache(new LRUCache<>(1500));
    }

    /* --------------------------------------------------------------------- */
    /*  Tests for Class‑Level Annotation Lookup                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetClassAnnotation_FindsOnSuperclass() {
        // SubClass does not have the annotation, its superclass does
        ClassAnno anno = ReflectionUtils.getClassAnnotation(SubClass.class, ClassAnno.class);
        assertNotNull("Annotation should be inherited from SuperClass (via interface)", anno);
        // ensure it was cached (null or not, the key must exist)
        assertTrue(classAnnoCache.containsKey(new ReflectionUtils.ClassAnnotationCacheKey(SubClass.class, ClassAnno.class)));
    }

    @Test
    public void testGetClassAnnotation_FindsOnInterface() {
        // Interface is annotated; SubClass implements it
        ClassAnno anno = ReflectionUtils.getClassAnnotation(SubClass.class, ClassAnno.class);
        assertNotNull("Annotation should be found on the implemented interface", anno);
    }

    @Test
    public void testGetClassAnnotation_NullClassReturnsNull() {
        assertNull(ReflectionUtils.getClassAnnotation(null, ClassAnno.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetClassAnnotation_NullAnnotationClassThrows() {
        ReflectionUtils.getClassAnnotation(SubClass.class, null);
    }

    /* --------------------------------------------------------------------- */
    /*  Tests for Method‑Level Annotation Lookup                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetMethodAnnotation_FindsOnInterfaceMethod() throws Exception {
        Method m = SubClass.class.getMethod("interfaceMethod");
        MethodAnno anno = ReflectionUtils.getMethodAnnotation(m, MethodAnno.class);
        assertNotNull("Method annotation should be discovered on the interface method", anno);
        assertTrue(methodAnnoCache.containsKey(new ReflectionUtils.MethodAnnotationCacheKey(m, MethodAnno.class)));
    }

    @Test
    public void testGetMethodAnnotation_NoAnnotationReturnsNull() throws Exception {
        Method m = SubClass.class.getMethod("superMethod");
        assertNull(ReflectionUtils.getMethodAnnotation(m, MethodAnno.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodAnnotation_NullMethodThrows() {
        ReflectionUtils.getMethodAnnotation(null, MethodAnno.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodAnnotation_NullAnnoClassThrows() throws NoSuchMethodException {
        Method m = SubClass.class.getMethod("superMethod");
        ReflectionUtils.getMethodAnnotation(m, null);
    }

    /* --------------------------------------------------------------------- */
    /*  Tests for Field Retrieval & Caching                                    */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetField_FindsDeclaredAndSuperclassFields() {
        Field f1 = ReflectionUtils.getField(SubClass.class, "privateSubField");
        assertNotNull(f1);
        assertEquals("privateSubField", f1.getName());

        Field f2 = ReflectionUtils.getField(SubClass.class, "privateSuperField");
        assertNotNull(f2);
        assertEquals("privateSuperField", f2.getName());

        // verify caching (the same Field instance is returned on a second call)
        Field f1Again = ReflectionUtils.getField(SubClass.class, "privateSubField");
        assertSame("Field should be cached", f1, f1Again);
        assertTrue(fieldNameCache.containsKey(new ReflectionUtils.FieldNameCacheKey(SubClass.class, "privateSubField")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetField_NullClassThrows() {
        ReflectionUtils.getField(null, "any");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetField_NullFieldNameThrows() {
        ReflectionUtils.getField(SubClass.class, null);
    }

    /* --------------------------------------------------------------------- */
    /*  Tests for Declared Fields (custom and default filters)                */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetDeclaredFields_CustomFilter() {
        // Only include fields whose name contains "Super"
        Predicate<Field> filter = f -> f.getName().contains("Super");
        List<Field> fields = ReflectionUtils.getDeclaredFields(SubClass.class, filter);
        assertEquals(1, fields.size());
        assertEquals("privateSuperField", fields.get(0).getName());

        // ensure caching works (same list instance)
        List<Field> fieldsAgain = ReflectionUtils.getDeclaredFields(SubClass.class, filter);
        assertSame(fields, fieldsAgain);
    }

    @Test
    public void testGetDeclaredFields_DefaultFilterExcludesStaticAndSynthetic() {
        List<Field> fields = ReflectionUtils.getDeclaredFields(SubClass.class);
        // staticSuperField should be excluded
        for (Field f : fields) {
            assertFalse(Modifier.isStatic(f.getModifiers()));
        }
    }

    @Test
    public void testGetAllDeclaredFields_IncludesSuperclassFields() {
        List<Field> all = ReflectionUtils.getAllDeclaredFields(SubClass.class);
        // should contain both subclass and superclass fields
        Set<String> names = new HashSet<>();
        for (Field f : all) names.add(f.getName());

        assertTrue(names.contains("privateSubField"));
        assertTrue(names.contains("privateSuperField"));
    }

    @Test
    public void testGetAllDeclaredFields_CustomFilter() {
        Predicate<Field> filter = f -> f.getType() == String.class;
        List<Field> allStringFields = ReflectionUtils.getAllDeclaredFields(SubClass.class, filter);
        assertEquals(2, allStringFields.size()); // privateSubField and privateSuperField
    }

    /* --------------------------------------------------------------------- */
    /*  Tests for Field‑Map Generation (collision handling)                    */
    /* --------------------------------------------------------------------- */

    static class ParentCollision {
        private int value;
    }

    static class ChildCollision extends ParentCollision {
        private int value; // hides parent field
    }

    @Test
    public void testGetAllDeclaredFieldsMap_CollisionResolution() {
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(ChildCollision.class);
        // simple name should map to child's field
        assertTrue(map.containsKey("value"));
        assertEquals(ChildCollision.class, map.get("value").getDeclaringClass());

        // fully qualified name should map to parent's field
        String parentKey = ParentCollision.class.getName() + ".value";
        assertTrue(map.containsKey(parentKey));
        assertEquals(ParentCollision.class, map.get(parentKey).getDeclaringClass());
    }

    /* --------------------------------------------------------------------- */
    /*  Tests for the Reflection “call” helpers                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testCall_WithMethodObject_InvokesSuccessfully() throws Exception {
        SubClass obj = new SubClass();
        Method m = SubClass.class.getMethod("superMethod");
        assertNull(ReflectionUtils.call(obj, m)); // void method returns null
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCall_NullMethodThrows() {
        ReflectionUtils.call(new SubClass(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCall_NullInstanceThrows() throws NoSuchMethodException {
        Method m = SubClass.class.getMethod("superMethod");
        ReflectionUtils.call(null, m);
    }

    @Test
    public void testCall_WithMethodName_InvokesSuccessfully() {
        SubClass obj = new SubClass();
        Object result = ReflectionUtils.call(obj, "superMethod");
        assertNull(result);
    }

    @Test
    public void testCall_WithMethodName_ThrowsWrappedException() {
        SubClass obj = new SubClass() {
            public void badMethod() {
                throw new RuntimeException("boom");
            }
        };
        try {
            ReflectionUtils.call(obj, "badMethod");
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            assertEquals("boom", e.getCause().getMessage());
        }
    }

    /* --------------------------------------------------------------------- */
    /*  Tests for Method Lookup (by signature & by arg count)                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetMethod_BySignature_FindsPrivateMethod() {
        Method m = ReflectionUtils.getMethod(SubClass.class, "overloaded", String.class, int.class);
        assertNotNull(m);
        assertTrue(Modifier.isPrivate(m.getModifiers()));
        // cached?
        ReflectionUtils.MethodCacheKey key = new ReflectionUtils.MethodCacheKey(SubClass.class, "overloaded", String.class, int.class);
        assertSame(m, methodCache.get(key));
    }

    @Test
    public void testGetMethod_ByArgCount_SelectsMostAccessible() {
        // there are three overloads named "overloaded"
        Method m = ReflectionUtils.getMethod(new SubClass(), "overloaded", 2);
        assertNotNull(m);
        // most accessible among the candidates with 2 args is the protected one
        assertTrue(Modifier.isProtected(m.getModifiers()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethod_ByArgCount_NoMatchThrows() {
        ReflectionUtils.getMethod(new SubClass(), "nonexistent", 1);
    }

    @Test
    public void testGetNonOverloadedMethod_Succeeds() {
        Method m = ReflectionUtils.getNonOverloadedMethod(SubClass.class, "superMethod");
        assertNotNull(m);
        assertEquals("superMethod", m.getName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethod_OverloadedThrows() {
        // "overloaded" is overloaded, should trigger exception
        ReflectionUtils.getNonOverloadedMethod(SubClass.class, "overloaded");
    }

    /* --------------------------------------------------------------------- */
    /*  Tests for Constructor Retrieval & Caching                              */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetConstructor_PrivateConstructor() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(NoDefaultConstructor.class, String.class);
        assertNotNull(ctor);
        assertTrue(Modifier.isPublic(ctor.getModifiers()) || ctor.isAccessible());
        // cached?
        ReflectionUtils.ConstructorCacheKey key = new ReflectionUtils.ConstructorCacheKey(NoDefaultConstructor.class, String.class);
        assertSame(ctor, ctorCache.get(key));
    }

    @Test
    public void testGetAllConstructors_CachesAll() {
        Constructor<?>[] ctors = ReflectionUtils.getAllConstructors(NoDefaultConstructor.class);
        assertEquals(2, ctors.length);
        // each constructor should now be present in the cache
        for (Constructor<?> c : ctors) {
            ReflectionUtils.ConstructorCacheKey key = new ReflectionUtils.ConstructorCacheKey(NoDefaultConstructor.class, c.getParameterTypes());
            assertSame(c, ctorCache.get(key));
        }
    }

    /* --------------------------------------------------------------------- */
    /*  Tests for Utility Methods                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetClassName_NullAndNonNull() {
        assertEquals("null", ReflectionUtils.getClassName(null));
        SubClass obj = new SubClass();
        assertEquals(SubClass.class.getName(), ReflectionUtils.getClassName(obj));
    }

    @Test
    public void testGetClassNameFromByteCode_ValidClass() throws IOException {
        // Load the bytecode of the test class itself via the classloader
        String className = SubClass.class.getName();
        String resourcePath = className.replace('.', '/') + ".class";
        InputStream is = SubClass.class.getClassLoader().getResourceAsStream(resourcePath);
        assertNotNull("Unable to locate .class file for SubClass", is);
        byte[] bytes = is.readAllBytes();
        is.close();

        String extracted = ReflectionUtils.getClassNameFromByteCode(bytes);
        assertEquals(className, extracted);
    }

    @Test(expected = IllegalStateException.class)
    public void testGetClassNameFromByteCode_BadDataThrows() throws IOException {
        // Feed garbage data – should trigger IllegalStateException
        ReflectionUtils.getClassNameFromByteCode(new byte[]{0, 1, 2, 3, 4});
    }

    /* --------------------------------------------------------------------- */
    /*  Tests for Cache‑Setter Methods (ensure they replace the internal map) */
    /* --------------------------------------------------------------------- */

    @Test
    public void testSetMethodCache_ReplacesCache() {
        Map<Object, Method> newCache = new HashMap<>();
        ReflectionUtils.setMethodCache(newCache);
        Method m = ReflectionUtils.getMethod(SubClass.class, "superMethod");
        assertTrue(newCache.containsKey(new ReflectionUtils.MethodCacheKey(SubClass.class, "superMethod")));
        assertSame(m, newCache.get(new ReflectionUtils.MethodCacheKey(SubClass.class, "superMethod")));
    }

    @Test
    public void testSetFieldCache_ReplacesCache() {
        Map<Object, Field> newCache = new HashMap<>();
        ReflectionUtils.setFieldCache(newCache);
        Field f = ReflectionUtils.getField(SubClass.class, "privateSubField");
        assertTrue(newCache.containsKey(new ReflectionUtils.FieldNameCacheKey(SubClass.class, "privateSubField")));
        assertSame(f, newCache.get(new ReflectionUtils.FieldNameCacheKey(SubClass.class, "privateSubField")));
    }

    @Test
    public void testSetClassFieldsCache_ReplacesCache() {
        Map<Object, Collection<Field>> newCache = new HashMap<>();
        ReflectionUtils.setClassFieldsCache(newCache);
        List<Field> fields = ReflectionUtils.getDeclaredFields(SubClass.class);
        // The cache key is a private class – we use reflection to create it
        Class<?> keyClass = Class.forName("com.cedarsoftware.util.ReflectionUtils$FieldsCacheKey");
        java.lang.reflect.Constructor<?> ctor = keyClass.getDeclaredConstructor(Class.class, Predicate.class, boolean.class);
        ctor.setAccessible(true);
        Object key = ctor.newInstance(SubClass.class, ReflectionUtils.DEFAULT_FIELD_FILTER, false);
        assertTrue(newCache.containsKey(key));
        assertSame(fields, newCache.get(key));
    }

    @Test
    public void testSetClassAnnotationCache_ReplacesCache() {
        Map<Object, Annotation> newCache = new HashMap<>();
        ReflectionUtils.setClassAnnotationCache(newCache);
        ReflectionUtils.getClassAnnotation(SubClass.class, ClassAnno.class);
        Object key = new ReflectionUtils.ClassAnnotationCacheKey(SubClass.class, ClassAnno.class);
        assertTrue(newCache.containsKey(key));
    }

    @Test
    public void testSetMethodAnnotationCache_ReplacesCache() throws NoSuchMethodException {
        Map<Object, Annotation> newCache = new HashMap<>();
        ReflectionUtils.setMethodAnnotationCache(newCache);
        Method m = SubClass.class.getMethod("interfaceMethod");
        ReflectionUtils.getMethodAnnotation(m, MethodAnno.class);
        Object key = new ReflectionUtils.MethodAnnotationCacheKey(m, MethodAnno.class);
        assertTrue(newCache.containsKey(key));
    }

    @Test
    public void testSetConstructorCache_ReplacesCache() {
        Map<Object, Constructor<?>> newCache = new HashMap<>();
        ReflectionUtils.setConstructorCache(newCache);
        ReflectionUtils.getConstructor(NoDefaultConstructor.class, String.class);
        Object key = new ReflectionUtils.ConstructorCacheKey(NoDefaultConstructor.class, String.class);
        assertTrue(newCache.containsKey(key));
    }
}
