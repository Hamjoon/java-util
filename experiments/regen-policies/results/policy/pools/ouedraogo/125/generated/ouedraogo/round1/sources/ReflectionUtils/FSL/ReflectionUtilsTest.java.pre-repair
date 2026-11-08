package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.io.IOException;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link ReflectionUtils}.
 *
 * The tests cover:
 * <ul>
 *   <li>Annotation lookup on classes and methods (including caching)</li>
 *   <li>Field retrieval across inheritance hierarchies and name‑collision handling</li>
 *   <li>Declared‑field filtering (default filter and custom predicates)</li>
 *   <li>Method lookup (by name & types, by argument count) and selection strategy</li>
 *   <li>Constructor lookup and bulk constructor retrieval</li>
 *   <li>Utility helpers: call(), getNonOverloadedMethod(), getClassName(), getClassNameFromByteCode()</li>
 *   <li>Cache replacement via the {@code set*Cache} setters and verification of cache population</li>
 * </ul>
 *
 * No external mocking frameworks are used; all objects are created via real constructors.
 */
public class ReflectionUtilsTest {

    /* --------------------------------------------------------------------- */
    /*   Helper annotations and test classes                                 */
    /* --------------------------------------------------------------------- */

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    private @interface TestAnno {
        String value() default "";
    }

    @TestAnno("base")
    private static class BaseClass {
        private int baseValue = 1;
        private static int staticBase = 2;
        protected String protectedField = "base";

        @TestAnno("baseMethod")
        public void annotatedMethod() {}

        private void privateMethod() {}

        public void overloaded(String a, String b) {}

        protected void overloaded(Integer a, Integer b) {}

        private void overloaded(Object a, Object b) {}
    }

    @TestAnno("sub")
    private static class SubClass extends BaseClass {
        private int subValue = 10;
        private static int staticSub = 20;
        private String protectedField = "sub"; // hides parent field

        @Override
        public void annotatedMethod() {} // no annotation here

        public void uniqueMethod() {}

        public String throwsException() {
            throw new IllegalStateException("boom");
        }
    }

    private static class NoAnnotationClass {}

    private static class ConstructorDemo {
        public ConstructorDemo() {}
        private ConstructorDemo(String s) {}
    }

    /* --------------------------------------------------------------------- */
    /*   Setup – ensure we start with clean caches for each test            */
    /* --------------------------------------------------------------------- */

    @Before
    public void resetCaches() {
        // Replace the default caches with fresh LRUCache‑like maps (simple HashMap works for tests)
        ReflectionUtils.setMethodCache(new HashMap<>());
        ReflectionUtils.setClassFieldsCache(new HashMap<>());
        ReflectionUtils.setFieldCache(new HashMap<>());
        ReflectionUtils.setClassAnnotationCache(new HashMap<>());
        ReflectionUtils.setMethodAnnotationCache(new HashMap<>());
        ReflectionUtils.setConstructorCache(new HashMap<>());
    }

    /* --------------------------------------------------------------------- */
    /*   Class‑level annotation lookup                                       */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetClassAnnotationFoundAndCached() {
        TestAnno anno = ReflectionUtils.getClassAnnotation(SubClass.class, TestAnno.class);
        assertNotNull("Annotation should be found on SubClass", anno);
        assertEquals("sub", anno.value());

        // Second call must hit the cache – the returned instance should be the same
        TestAnno cached = ReflectionUtils.getClassAnnotation(SubClass.class, TestAnno.class);
        assertSame("Second lookup should return the cached instance", anno, cached);
    }

    @Test
    public void testGetClassAnnotationMissAndCacheNull() {
        // No annotation present – should return null and cache the miss
        TestAnno miss = ReflectionUtils.getClassAnnotation(NoAnnotationClass.class, TestAnno.class);
        assertNull(miss);

        // Second lookup – still null and should not throw
        TestAnno missAgain = ReflectionUtils.getClassAnnotation(NoAnnotationClass.class, TestAnno.class);
        assertNull(missAgain);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetClassAnnotationNullClass() {
        ReflectionUtils.getClassAnnotation(null, TestAnno.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetClassAnnotationNullAnnoClass() {
        ReflectionUtils.getClassAnnotation(SubClass.class, null);
    }

    /* --------------------------------------------------------------------- */
    /*   Method‑level annotation lookup                                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetMethodAnnotationFoundAndCached() throws NoSuchMethodException {
        Method m = SubClass.class.getMethod("annotatedMethod");
        TestAnno anno = ReflectionUtils.getMethodAnnotation(m, TestAnno.class);
        assertNotNull(anno);
        assertEquals("base", anno.value()); // inherited from BaseClass

        // Cached instance test
        TestAnno cached = ReflectionUtils.getMethodAnnotation(m, TestAnno.class);
        assertSame(anno, cached);
    }

    @Test
    public void testGetMethodAnnotationMiss() throws NoSuchMethodException {
        Method m = SubClass.class.getMethod("uniqueMethod");
        TestAnno missing = ReflectionUtils.getMethodAnnotation(m, TestAnno.class);
        assertNull(missing);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodAnnotationNullMethod() {
        ReflectionUtils.getMethodAnnotation(null, TestAnno.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodAnnotationNullAnnoClass() throws NoSuchMethodException {
        Method m = SubClass.class.getMethod("uniqueMethod");
        ReflectionUtils.getMethodAnnotation(m, null);
    }

    /* --------------------------------------------------------------------- */
    /*   Field retrieval (including inheritance & name collisions)          */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetFieldFoundInSuperclass() {
        Field f = ReflectionUtils.getField(SubClass.class, "baseValue");
        assertNotNull(f);
        assertEquals("baseValue", f.getName());
        assertEquals(BaseClass.class, f.getDeclaringClass());
    }

    @Test
    public void testGetFieldFoundInSubclass() {
        Field f = ReflectionUtils.getField(SubClass.class, "subValue");
        assertNotNull(f);
        assertEquals("subValue", f.getName());
        assertEquals(SubClass.class, f.getDeclaringClass());
    }

    @Test
    public void testGetFieldCacheReturnsSameInstance() {
        Field first = ReflectionUtils.getField(SubClass.class, "baseValue");
        Field second = ReflectionUtils.getField(SubClass.class, "baseValue");
        assertSame("Cached field instances must be identical", first, second);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetFieldNullClass() {
        ReflectionUtils.getField(null, "any");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetFieldNullName() {
        ReflectionUtils.getField(SubClass.class, null);
    }

    @Test
    public void testGetAllDeclaredFieldsMapCollisionHandling() {
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(SubClass.class);
        // Child's field should win the simple name
        assertTrue(map.containsKey("subValue"));
        assertEquals(SubClass.class, map.get("subValue").getDeclaringClass());

        // Parent's field with same simple name should be stored with qualified name
        assertTrue(map.containsKey(BaseClass.class.getName() + ".baseValue"));
        assertEquals(BaseClass.class, map.get(BaseClass.class.getName() + ".baseValue").getDeclaringClass());
    }

    /* --------------------------------------------------------------------- */
    /*   Declared‑field filtering (default & custom)                        */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetDeclaredFieldsDefaultFilterExcludesStatic() {
        List<Field> fields = ReflectionUtils.getDeclaredFields(SubClass.class);
        // static fields must be filtered out
        for (Field f : fields) {
            assertFalse("Static field should be filtered", Modifier.isStatic(f.getModifiers()));
        }
        // ensure a known instance field is present
        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("subValue")));
    }

    @Test
    public void testGetDeclaredFieldsCustomPredicate() {
        Predicate<Field> onlyPublic = f -> Modifier.isPublic(f.getModifiers());
        List<Field> publicFields = ReflectionUtils.getDeclaredFields(BaseClass.class, onlyPublic);
        // BaseClass has no public declared fields (only inherited Object fields)
        assertTrue(publicFields.isEmpty());
    }

    @Test
    public void testGetAllDeclaredFieldsHierarchyOrder() {
        List<Field> all = ReflectionUtils.getAllDeclaredFields(SubClass.class);
        // The first elements should belong to SubClass, then BaseClass
        assertEquals("subValue", all.get(0).getName());
        assertEquals("protectedField", all.get(1).getName()); // hides parent field
    }

    /* --------------------------------------------------------------------- */
    /*   Method lookup (by signature)                                        */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetMethodBySignaturePublic() {
        Method m = ReflectionUtils.getMethod(BaseClass.class, "annotatedMethod");
        assertNotNull(m);
        assertEquals("annotatedMethod", m.getName());
        assertTrue(Modifier.isPublic(m.getModifiers()));
    }

    @Test
    public void testGetMethodBySignaturePrivate() {
        Method m = ReflectionUtils.getMethod(BaseClass.class, "privateMethod");
        assertNotNull(m);
        assertEquals("privateMethod", m.getName());
        assertTrue(Modifier.isPrivate(m.getModifiers()));
        assertTrue(m.isAccessible() || m.canAccess(null)); // setAccessible should have been attempted
    }

    @Test
    public void testGetMethodCacheReuse() {
        Method first = ReflectionUtils.getMethod(BaseClass.class, "annotatedMethod");
        Method second = ReflectionUtils.getMethod(BaseClass.class, "annotatedMethod");
        assertSame(first, second);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodNullClass() {
        ReflectionUtils.getMethod(null, "any");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodNullName() {
        ReflectionUtils.getMethod(BaseClass.class, null);
    }

    /* --------------------------------------------------------------------- */
    /*   getMethod(instance, name, argCount) – selection strategy            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetMethodByArgCountSelectsMostAccessible() {
        Method m = ReflectionUtils.getMethod(new SubClass(), "overloaded", 2);
        assertNotNull(m);
        assertTrue("Public method should be selected", Modifier.isPublic(m.getModifiers()));
        assertEquals(SubClass.class, m.getDeclaringClass());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodByArgCountMissing() {
        ReflectionUtils.getMethod(new SubClass(), "nonexistent", 1);
    }

    /* --------------------------------------------------------------------- */
    /*   Constructor lookup and bulk retrieval                                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetConstructorPublic() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(ConstructorDemo.class);
        assertNotNull(ctor);
        assertTrue(Modifier.isPublic(ctor.getModifiers()));
    }

    @Test
    public void testGetConstructorPrivate() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(ConstructorDemo.class, String.class);
        assertNotNull(ctor);
        assertTrue(Modifier.isPrivate(ctor.getModifiers()));
        assertTrue(ctor.isAccessible() || ctor.canAccess(null));
    }

    @Test
    public void testGetAllConstructorsCacheConsistency() {
        Constructor<?>[] arr1 = ReflectionUtils.getAllConstructors(ConstructorDemo.class);
        Constructor<?>[] arr2 = ReflectionUtils.getAllConstructors(ConstructorDemo.class);
        assertEquals(arr1.length, arr2.length);
        for (int i = 0; i < arr1.length; i++) {
            assertSame("Constructor instances should be cached", arr1[i], arr2[i]);
        }
    }

    /* --------------------------------------------------------------------- */
    /*   getNonOverloadedMethod – success & overload error                     */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetNonOverloadedMethodSuccess() {
        Method m = ReflectionUtils.getNonOverloadedMethod(BaseClass.class, "annotatedMethod");
        assertNotNull(m);
        assertEquals("annotatedMethod", m.getName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethodAmbiguous() {
        // BaseClass has three overloads named "overloaded" (different signatures)
        ReflectionUtils.getNonOverloadedMethod(BaseClass.class, "overloaded");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethodNotFound() {
        ReflectionUtils.getNonOverloadedMethod(BaseClass.class, "missingMethod");
    }

    /* --------------------------------------------------------------------- */
    /*   call(Object, Method, …) – normal, illegal access, exception wrap   */
    /* --------------------------------------------------------------------- */

    @Test
    public void testCallWithPublicMethod() throws Exception {
        SubClass obj = new SubClass();
        Method m = ReflectionUtils.getMethod(SubClass.class, "uniqueMethod");
        Object result = ReflectionUtils.call(obj, m);
        assertNull("void method should return null", result);
    }

    @Test
    public void testCallWithPrivateMethod() throws Exception {
        SubClass obj = new SubClass();
        Method m = ReflectionUtils.getMethod(BaseClass.class, "privateMethod");
        Object result = ReflectionUtils.call(obj, m);
        assertNull(result);
    }

    @Test
    public void testCallWrapsInvocationTargetException() throws Exception {
        SubClass obj = new SubClass();
        Method m = ReflectionUtils.getMethod(SubClass.class, "throwsException");
        try {
            ReflectionUtils.call(obj, m);
            fail("Expected RuntimeException wrapping IllegalStateException");
        } catch (RuntimeException e) {
            assertTrue(e.getCause() instanceof IllegalStateException);
            assertEquals("boom", e.getCause().getMessage());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallNullMethod() {
        ReflectionUtils.call(new SubClass(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallNullInstance() throws Exception {
        Method m = SubClass.class.getDeclaredMethod("uniqueMethod");
        ReflectionUtils.call(null, m);
    }

    /* --------------------------------------------------------------------- */
    /*   getClassName utility                                                  */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetClassName() {
        assertEquals("null", ReflectionUtils.getClassName(null));
        assertEquals(SubClass.class.getName(), ReflectionUtils.getClassName(new SubClass()));
    }

    /* --------------------------------------------------------------------- */
    /*   getClassNameFromByteCode – read own class file                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetClassNameFromByteCode() throws IOException {
        String classResource = "/" + ReflectionUtils.class.getName().replace('.', '/') + ".class";
        byte[] bytes = ReflectionUtils.class.getResourceAsStream(classResource).readAllBytes();
        String name = ReflectionUtils.getClassNameFromByteCode(bytes);
        assertEquals(ReflectionUtils.class.getName(), name);
    }

    /* --------------------------------------------------------------------- */
    /*   Cache replacement verification (set*Cache)                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testSetMethodCacheAndVerifyPopulation() throws Exception {
        Map<Object, Method> custom = new HashMap<>();
        ReflectionUtils.setMethodCache(custom);

        // Trigger a cache entry
        Method m = ReflectionUtils.getMethod(BaseClass.class, "annotatedMethod");
        ReflectionUtils.MethodCacheKey key = new ReflectionUtils.MethodCacheKey(BaseClass.class, "annotatedMethod");
        assertTrue("Custom cache should contain the key", custom.containsKey(key));
        assertSame("Method stored in custom cache must be the same instance", m, custom.get(key));
    }

    @Test
    public void testSetFieldCacheAndVerifyPopulation() {
        Map<Object, Field> custom = new HashMap<>();
        ReflectionUtils.setFieldCache(custom);

        Field f = ReflectionUtils.getField(SubClass.class, "subValue");
        ReflectionUtils.FieldNameCacheKey key = new ReflectionUtils.FieldNameCacheKey(SubClass.class, "subValue");
        assertTrue(custom.containsKey(key));
        assertSame(f, custom.get(key));
    }

    @Test
    public void testSetClassFieldsCacheAndVerifyPopulation() {
        Map<Object, Collection<Field>> custom = new HashMap<>();
        ReflectionUtils.setClassFieldsCache(custom);

        // getAllDeclaredFields will use the FIELDS_CACHE
        List<Field> list = ReflectionUtils.getAllDeclaredFields(SubClass.class);
        ReflectionUtils.FieldsCacheKey key = new ReflectionUtils.FieldsCacheKey(SubClass.class, ReflectionUtils.DEFAULT_FIELD_FILTER, true);
        assertTrue(custom.containsKey(key));
        assertSame(list, custom.get(key));
    }

    @Test
    public void testSetClassAnnotationCacheAndVerifyPopulation() {
        Map<Object, Annotation> custom = new HashMap<>();
        ReflectionUtils.setClassAnnotationCache(custom);

        TestAnno anno = ReflectionUtils.getClassAnnotation(SubClass.class, TestAnno.class);
        ReflectionUtils.ClassAnnotationCacheKey key = new ReflectionUtils.ClassAnnotationCacheKey(SubClass.class, TestAnno.class);
        assertTrue(custom.containsKey(key));
        assertSame(anno, custom.get(key));
    }

    @Test
    public void testSetMethodAnnotationCacheAndVerifyPopulation() throws Exception {
        Map<Object, Annotation> custom = new HashMap<>();
        ReflectionUtils.setMethodAnnotationCache(custom);

        Method m = SubClass.class.getMethod("annotatedMethod");
        TestAnno anno = ReflectionUtils.getMethodAnnotation(m, TestAnno.class);
        ReflectionUtils.MethodAnnotationCacheKey key = new ReflectionUtils.MethodAnnotationCacheKey(m, TestAnno.class);
        assertTrue(custom.containsKey(key));
        assertSame(anno, custom.get(key));
    }

    @Test
    public void testSetConstructorCacheAndVerifyPopulation() {
        Map<Object, Constructor<?>> custom = new HashMap<>();
        ReflectionUtils.setConstructorCache(custom);

        Constructor<?> ctor = ReflectionUtils.getConstructor(ConstructorDemo.class, String.class);
        ReflectionUtils.ConstructorCacheKey key = new ReflectionUtils.ConstructorCacheKey(ConstructorDemo.class, String.class);
        assertTrue(custom.containsKey(key));
        assertSame(ctor, custom.get(key));
    }
}
