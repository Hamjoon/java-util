package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

public class ReflectionUtilsTest {

    /* --------------------------------------------------------------
     * Helper annotation and test classes used by many test cases
     * -------------------------------------------------------------- */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    private @interface TestAnno { String value() default ""; }

    @TestAnno("base")
    private static class BaseClass {
        private String baseField = "base";
        private static String staticBaseField = "staticBase";

        @TestAnno("baseMethod")
        private void privateBaseMethod() {}
        protected void protectedBaseMethod() {}
        public void publicBaseMethod() {}
    }

    private interface TestInterface {
        @TestAnno("interfaceMethod")
        default void ifaceMethod() {}
    }

    @TestAnno("derived")
    private static class DerivedClass extends BaseClass implements TestInterface {
        private String derivedField = "derived";
        private static String staticDerivedField = "staticDerived";

        // overloads for getMethod(Object, name, argCount) tests
        public void overloaded() {}
        protected void overloaded(String s) {}
        private void overloaded(String s, int i) {}
        public void bridgeMethod() {} // not a real bridge, just to have another method
    }

    private static class NoDefaultConstructor {
        private final int x;
        private NoDefaultConstructor(int x) { this.x = x; }
    }

    private static class SimpleClass {
        public int publicField;
        private String privateField;
        static int staticField;
        public SimpleClass() {}
        private SimpleClass(String s) {}
        public String hello() { return "hi"; }
        private String secret() { return "shhh"; }
    }

    /* --------------------------------------------------------------
     * Custom cache implementations used to verify that the
     * set*Cache methods really replace the internal caches.
     * -------------------------------------------------------------- */
    private static class SimpleMap<K,V> extends LinkedHashMap<K,V> {
        // no extra behaviour – just a concrete thread‑safe enough map for the test
    }

    private SimpleMap<Object,Method> methodCache;
    private SimpleMap<Object,Collection<Field>> fieldsCache;
    private SimpleMap<Object,Field> fieldCache;
    private SimpleMap<Object,Annotation> classAnnoCache;
    private SimpleMap<Object,Annotation> methodAnnoCache;
    private SimpleMap<Object,Constructor<?>> ctorCache;

    @Before
    public void setUpCaches() {
        methodCache = new SimpleMap<>();
        fieldsCache = new SimpleMap<>();
        fieldCache = new SimpleMap<>();
        classAnnoCache = new SimpleMap<>();
        methodAnnoCache = new SimpleMap<>();
        ctorCache = new SimpleMap<>();

        ReflectionUtils.setMethodCache(methodCache);
        ReflectionUtils.setClassFieldsCache(fieldsCache);
        ReflectionUtils.setFieldCache(fieldCache);
        ReflectionUtils.setClassAnnotationCache(classAnnoCache);
        ReflectionUtils.setMethodAnnotationCache(methodAnnoCache);
        ReflectionUtils.setConstructorCache(ctorCache);
    }

    @After
    public void clearCaches() {
        // Restore the default caches to avoid side effects for other tests
        ReflectionUtils.setMethodCache(new LRUCache<>(1500));
        ReflectionUtils.setClassFieldsCache(new LRUCache<>(1500));
        ReflectionUtils.setFieldCache(new LRUCache<>(1500 * 10));
        ReflectionUtils.setClassAnnotationCache(new LRUCache<>(1500));
        ReflectionUtils.setMethodAnnotationCache(new LRUCache<>(1500));
        ReflectionUtils.setConstructorCache(new LRUCache<>(1500));
    }

    /* --------------------------------------------------------------
     * 1. Test getClassAnnotation – inheritance, interface and cache
     * -------------------------------------------------------------- */
    @Test
    public void testGetClassAnnotation_InheritedAndInterface() {
        // Annotation is on BaseClass, not on DerivedClass
        TestAnno anno = ReflectionUtils.getClassAnnotation(DerivedClass.class, TestAnno.class);
        assertNotNull("Annotation should be found on superclass", anno);
        assertEquals("base", anno.value());

        // Annotation on interface method – not class annotation – should be null
        TestAnno ifaceAnno = ReflectionUtils.getClassAnnotation(TestInterface.class, TestAnno.class);
        assertNull(ifaceAnno);

        // Verify that a cache entry was created
        assertTrue("Class annotation cache must contain the key",
                classAnnoCache.containsKey(new ReflectionUtils.ClassAnnotationCacheKey(DerivedClass.class, TestAnno.class)));
    }

    /* --------------------------------------------------------------
     * 2. Test getMethodAnnotation – method on superclass and interface
     * -------------------------------------------------------------- */
    @Test
    public void testGetMethodAnnotation_SuperclassAndInterface() throws Exception {
        Method baseMethod = BaseClass.class.getDeclaredMethod("privateBaseMethod");
        TestAnno anno = ReflectionUtils.getMethodAnnotation(baseMethod, TestAnno.class);
        assertNotNull(anno);
        assertEquals("baseMethod", anno.value());

        // Interface default method
        Method ifaceMethod = TestInterface.class.getMethod("ifaceMethod");
        TestAnno ifaceAnno = ReflectionUtils.getMethodAnnotation(ifaceMethod, TestAnno.class);
        assertNotNull(ifaceAnno);
        assertEquals("interfaceMethod", ifaceAnno.value());

        // Cache verification
        ReflectionUtils.MethodAnnotationCacheKey key =
                new ReflectionUtils.MethodAnnotationCacheKey(baseMethod, TestAnno.class);
        assertTrue(methodAnnoCache.containsKey(key));
    }

    /* --------------------------------------------------------------
     * 3. Test getField – private field, static field ignored, cache
     * -------------------------------------------------------------- */
    @Test
    public void testGetField_PrivateAndStatic() {
        Field privateField = ReflectionUtils.getField(DerivedClass.class, "derivedField");
        assertNotNull(privateField);
        assertEquals("derivedField", privateField.getName());
        assertFalse("Field should not be static", Modifier.isStatic(privateField.getModifiers()));

        // static field should not be returned
        Field staticField = ReflectionUtils.getField(DerivedClass.class, "staticDerivedField");
        assertNull(staticField);

        // Cache entry
        ReflectionUtils.FieldNameCacheKey key = new ReflectionUtils.FieldNameCacheKey(DerivedClass.class, "derivedField");
        assertTrue(fieldCache.containsKey(key));
    }

    /* --------------------------------------------------------------
     * 4. Test getDeclaredFields default filter – static excluded,
     *    enum internal excluded (using a simple enum)
     * -------------------------------------------------------------- */
    private enum SampleEnum {
        ONE, TWO;
        private int hidden;
        public static final String ENUM$VALUES = "shouldBeFiltered";
        private static final String internal = "shouldBeFiltered";
    }

    @Test
    public void testGetDeclaredFields_DefaultFilter() {
        List<Field> fields = ReflectionUtils.getDeclaredFields(SampleEnum.class);
        // Should contain only the enum constant fields (ONE, TWO) and the private instance field 'hidden'
        Set<String> names = new HashSet<>();
        for (Field f : fields) names.add(f.getName());

        assertTrue(names.contains("hidden"));
        assertFalse(names.contains("ENUM$VALUES"));
        assertFalse(names.contains("internal"));
    }

    /* --------------------------------------------------------------
     * 5. Test getAllDeclaredFields – hierarchy order and filter
     * -------------------------------------------------------------- */
    @Test
    public void testGetAllDeclaredFields_Hierarchy() {
        List<Field> all = ReflectionUtils.getAllDeclaredFields(DerivedClass.class);
        // Expect fields from DerivedClass first, then BaseClass
        assertEquals("derivedField", all.get(0).getName());
        assertEquals("baseField", all.get(1).getName());

        // Verify that the list is unmodifiable
        try {
            all.add(null);
            fail("List should be unmodifiable");
        } catch (UnsupportedOperationException expected) { }
    }

    /* --------------------------------------------------------------
     * 6. Test getAllDeclaredFieldsMap – name collision handling
     * -------------------------------------------------------------- */
    private static class ParentCollision {
        private String conflict = "parent";
    }
    private static class ChildCollision extends ParentCollision {
        private String conflict = "child";
    }

    @Test
    public void testGetAllDeclaredFieldsMap_Collision() {
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(ChildCollision.class);
        assertTrue(map.containsKey("conflict"));
        assertEquals("child", map.get("conflict").getDeclaringClass().getSimpleName());

        // Fully qualified name for parent field
        String fq = ParentCollision.class.getName() + ".conflict";
        assertTrue(map.containsKey(fq));
        assertEquals("parent", map.get(fq).getDeclaringClass().getSimpleName());
    }

    /* --------------------------------------------------------------
     * 7. Test getConstructor – private constructor and caching
     * -------------------------------------------------------------- */
    @Test
    public void testGetConstructor_PrivateAndCache() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(NoDefaultConstructor.class, int.class);
        assertNotNull(ctor);
        assertTrue("Constructor should be accessible after getConstructor", ctor.isAccessible());

        // Second call should retrieve the same instance from the cache
        Constructor<?> ctor2 = ReflectionUtils.getConstructor(NoDefaultConstructor.class, int.class);
        assertSame(ctor, ctor2);
        assertTrue(ctorCache.containsKey(new ReflectionUtils.ConstructorCacheKey(NoDefaultConstructor.class, int.class)));
    }

    /* --------------------------------------------------------------
     * 8. Test getAllConstructors – caching of each constructor
     * -------------------------------------------------------------- */
    @Test
    public void testGetAllConstructors_CacheConsistency() {
        Constructor<?>[] ctors = ReflectionUtils.getAllConstructors(SimpleClass.class);
        assertEquals(2, ctors.length); // public no‑arg and private String ctor
        // Ensure each constructor is cached
        for (Constructor<?> c : ctors) {
            ReflectionUtils.ConstructorCacheKey key =
                    new ReflectionUtils.ConstructorCacheKey(SimpleClass.class, c.getParameterTypes());
            assertTrue(ctorCache.containsKey(key));
        }
    }

    /* --------------------------------------------------------------
     * 9. Test getClassNameFromByteCode – read own class file bytes
     * -------------------------------------------------------------- */
    @Test
    public void testGetClassNameFromByteCode_Self() throws IOException {
        // Load the .class file of this test class via the class loader
        String classResource = "/" + ReflectionUtils.class.getName().replace('.', '/') + ".class";
        InputStream is = ReflectionUtils.class.getResourceAsStream(classResource);
        assertNotNull("Unable to locate class bytecode resource", is);
        byte[] bytes = is.readAllBytes();
        is.close();

        String name = ReflectionUtils.getClassNameFromByteCode(bytes);
        assertEquals(ReflectionUtils.class.getName(), name);
    }

    /* --------------------------------------------------------------
     * 10. Test getClassName – null handling
     * -------------------------------------------------------------- */
    @Test
    public void testGetClassName_NullAndObject() {
        assertEquals("null", ReflectionUtils.getClassName(null));
        assertEquals(SimpleClass.class.getName(), ReflectionUtils.getClassName(new SimpleClass()));
    }

    /* --------------------------------------------------------------
     * 11. Test getMethod – private method lookup and caching
     * -------------------------------------------------------------- */
    @Test
    public void testGetMethod_PrivateAndCache() throws Exception {
        Method m = ReflectionUtils.getMethod(SimpleClass.class, "secret");
        assertNotNull(m);
        assertTrue("Method should be made accessible", m.isAccessible());

        // Cached version
        Method m2 = ReflectionUtils.getMethod(SimpleClass.class, "secret");
        assertSame(m, m2);
        ReflectionUtils.MethodCacheKey key = new ReflectionUtils.MethodCacheKey(SimpleClass.class, "secret");
        assertTrue(methodCache.containsKey(key));
    }

    /* --------------------------------------------------------------
     * 12. Test getMethod(Object, name, argCount) – selection strategy
     * -------------------------------------------------------------- */
    @Test
    public void testGetMethodByArgCount_Selection() {
        DerivedClass obj = new DerivedClass();

        // argCount = 0 should pick publicBaseMethod (most accessible)
        Method m0 = ReflectionUtils.getMethod(obj, "publicBaseMethod", 0);
        assertEquals("publicBaseMethod", m0.getName());
        assertTrue(Modifier.isPublic(m0.getModifiers()));

        // argCount = 2 should pick the private overloaded method (only one with 2 params)
        Method m2 = ReflectionUtils.getMethod(obj, "overloaded", 2);
        assertEquals("overloaded", m2.getName());
        assertEquals(2, m2.getParameterCount());
        assertTrue(Modifier.isPrivate(m2.getModifiers()));
    }

    /* --------------------------------------------------------------
     * 13. Test getNonOverloadedMethod – success and overload error
     * -------------------------------------------------------------- */
    @Test
    public void testGetNonOverloadedMethod_Success() {
        Method m = ReflectionUtils.getNonOverloadedMethod(SimpleClass.class, "hello");
        assertNotNull(m);
        assertEquals("hello", m.getName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethod_OverloadFailure() {
        // 'overloaded' exists with several signatures in DerivedClass
        ReflectionUtils.getNonOverloadedMethod(DerivedClass.class, "overloaded");
    }

    /* --------------------------------------------------------------
     * 14. Test call(Object, Method) – normal return and exception wrapping
     * -------------------------------------------------------------- */
    @Test
    public void testCall_WithMethod_ReturnsValue() throws Exception {
        SimpleClass instance = new SimpleClass();
        Method hello = SimpleClass.class.getMethod("hello");
        Object result = ReflectionUtils.call(instance, hello);
        assertEquals("hi", result);
    }

    @Test
    public void testCall_WithMethod_ExceptionWrapping() throws Exception {
        // Create a method that throws a checked exception
        class Thrower {
            void explode() throws IOException { throw new IOException("boom"); }
        }
        Thrower t = new Thrower();
        Method explode = Thrower.class.getMethod("explode");
        try {
            ReflectionUtils.call(t, explode);
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            assertTrue(e.getCause() instanceof IOException);
            assertEquals("boom", e.getCause().getMessage());
        }
    }

    /* --------------------------------------------------------------
     * 15. Test call(Object, String) – method lookup by name & args
     * -------------------------------------------------------------- */
    @Test
    public void testCall_WithName_AndArgs() {
        SimpleClass obj = new SimpleClass();
        Object result = ReflectionUtils.call(obj, "hello");
        assertEquals("hi", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCall_WithName_MethodNotFound() {
        SimpleClass obj = new SimpleClass();
        ReflectionUtils.call(obj, "nonExistingMethod");
    }

    /* --------------------------------------------------------------
     * 16. Verify that custom caches are actually used by the utils.
     * -------------------------------------------------------------- */
    @Test
    public void testCustomCachesAreUsed() throws Exception {
        // Populate caches via a call
        Method m = ReflectionUtils.getMethod(SimpleClass.class, "hello");
        assertTrue(methodCache.containsKey(new ReflectionUtils.MethodCacheKey(SimpleClass.class, "hello")));

        Field f = ReflectionUtils.getField(SimpleClass.class, "publicField");
        assertTrue(fieldCache.containsKey(new ReflectionUtils.FieldNameCacheKey(SimpleClass.class, "publicField")));

        // Verify that the map instances we set are the ones referenced internally
        assertSame(methodCache, getInternalMap("METHOD_CACHE"));
        assertSame(fieldCache, getInternalMap("FIELD_NAME_CACHE"));
    }

    // Helper to peek at the private static fields via reflection (allowed in test code)
    private static Map<?,?> getInternalMap(String fieldName) throws Exception {
        Field f = ReflectionUtils.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        return (Map<?,?>) f.get(null);
    }
}
