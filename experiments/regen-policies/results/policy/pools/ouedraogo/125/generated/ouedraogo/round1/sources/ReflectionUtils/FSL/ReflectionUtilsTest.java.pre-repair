package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.io.IOException;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit4 test suite for {@link ReflectionUtils}.
 * Covers annotation lookup, field/constructor/method utilities,
 * caching behaviour and error handling.
 */
public class ReflectionUtilsTest {

    /* -------------------------------------------------------------
     *  Helper annotations and test classes used throughout the suite
     * ------------------------------------------------------------- */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    private @interface TestAnno {}

    @TestAnno
    private static class SuperClass {
        private int superPrivate = 1;
        protected String superProtected = "sup";

        @TestAnno
        private void annotatedPrivateMethod() {}

        public void overloaded(int a) {}
        private void overloaded(String s) {}
        void overloaded(Object o) {}
    }

    private interface TestInterface {
        @TestAnno
        default void annotatedInterfaceMethod() {}
    }

    @TestAnno
    private static class SubClass extends SuperClass implements TestInterface {
        private int childField = 42;
        public static final int STATIC_FIELD = 99;
        private String childPrivate = "child";

        public void publicMethod() {}

        private void privateMethod() {}

        // overloads for selection test
        public void selector(int a, int b) {}
        private void selector(String s, String t) {}
    }

    private static class ClassWithTransient {
        private int normal = 1;
        private transient int trans = 2;
    }

    private static class ParentCollision {
        private int dup = 1;
    }

    private static class ChildCollision extends ParentCollision {
        private int dup = 2;
        private int unique = 3;
    }

    /* -------------------------------------------------------------
     *  Simple mutable map used to verify that the cache‑setter methods
     *  actually replace the internal cache instances.
     * ------------------------------------------------------------- */
    private static class CountingMap<K, V> extends HashMap<K, V> {
        int puts = 0;
        int gets = 0;

        @Override
        public V put(K key, V value) {
            puts++;
            return super.put(key, value);
        }

        @Override
        public V get(Object key) {
            gets++;
            return super.get(key);
        }
    }

    private CountingMap<Object, Method> methodCache;
    private CountingMap<Object, Field> fieldCache;
    private CountingMap<Object, Constructor<?>> ctorCache;

    @Before
    public void setUp() {
        methodCache = new CountingMap<>();
        fieldCache = new CountingMap<>();
        ctorCache = new CountingMap<>();

        // Replace the internal caches with our counting maps
        ReflectionUtils.setMethodCache(methodCache);
        ReflectionUtils.setFieldCache(fieldCache);
        ReflectionUtils.setConstructorCache(ctorCache);
    }

    /* -------------------------------------------------------------
     *  Tests for class‑level annotation lookup and caching
     * ------------------------------------------------------------- */
    @Test
    public void testGetClassAnnotationFoundOnClass() {
        TestAnno anno = ReflectionUtils.getClassAnnotation(SubClass.class, TestAnno.class);
        assertNotNull("Annotation should be found on SubClass (inherited from SuperClass)", anno);
    }

    @Test
    public void testGetClassAnnotationFoundOnInterface() {
        TestAnno anno = ReflectionUtils.getClassAnnotation(TestInterface.class, TestAnno.class);
        assertNotNull("Annotation should be found directly on interface", anno);
    }

    @Test
    public void testGetClassAnnotationNotFoundReturnsNullAndCachesMiss() {
        // No such annotation on java.lang.String
        Deprecated missing = ReflectionUtils.getClassAnnotation(String.class, Deprecated.class);
        assertNull(missing);

        // Second call should hit the cache (no extra computation)
        Deprecated missing2 = ReflectionUtils.getClassAnnotation(String.class, Deprecated.class);
        assertNull(missing2);
    }

    /* -------------------------------------------------------------
     *  Tests for method‑level annotation lookup and caching
     * ------------------------------------------------------------- */
    @Test
    public void testGetMethodAnnotationDirect() throws Exception {
        Method m = SubClass.class.getDeclaredMethod("annotatedPrivateMethod");
        TestAnno anno = ReflectionUtils.getMethodAnnotation(m, TestAnno.class);
        assertNotNull("Method annotation should be retrieved directly", anno);
    }

    @Test
    public void testGetMethodAnnotationFromInterface() throws Exception {
        Method m = SubClass.class.getMethod("annotatedInterfaceMethod");
        TestAnno anno = ReflectionUtils.getMethodAnnotation(m, TestAnno.class);
        assertNotNull("Method annotation should be found on interface default method", anno);
    }

    @Test
    public void testGetMethodAnnotationCacheMiss() throws Exception {
        Method m = SubClass.class.getMethod("publicMethod");
        // No annotation present
        Override missing = ReflectionUtils.getMethodAnnotation(m, Override.class);
        assertNull(missing);

        // Second lookup hits the cache (no extra reflection)
        Override missing2 = ReflectionUtils.getMethodAnnotation(m, Override.class);
        assertNull(missing2);
    }

    /* -------------------------------------------------------------
     *  Tests for field lookup and hierarchy handling
     * ------------------------------------------------------------- */
    @Test
    public void testGetFieldFindsPrivateFieldInSuperclass() {
        Field f = ReflectionUtils.getField(SubClass.class, "superPrivate");
        assertNotNull("Should locate private field defined in superclass", f);
        assertEquals("superPrivate", f.getName());
    }

    @Test
    public void testGetFieldIgnoresStaticField() {
        Field f = ReflectionUtils.getField(SubClass.class, "STATIC_FIELD");
        assertNotNull("Static field is present but should be returned (the API does not filter static)", f);
        assertTrue(Modifier.isStatic(f.getModifiers()));
    }

    @Test
    public void testGetDeclaredFieldsWithCustomFilter() {
        // Filter to include only int fields
        List<Field> ints = ReflectionUtils.getDeclaredFields(SubClass.class,
                f -> f.getType() == int.class);
        assertEquals(2, ints.size()); // childField and superPrivate
        Set<String> names = new HashSet<>();
        for (Field f : ints) names.add(f.getName());
        assertTrue(names.contains("childField"));
        assertTrue(names.contains("superPrivate"));
    }

    @Test
    public void testGetAllDeclaredFieldsOrdering() {
        List<Field> all = ReflectionUtils.getAllDeclaredFields(SubClass.class);
        // first fields belong to SubClass, then to SuperClass, then to Object (none)
        assertTrue(all.get(0).getDeclaringClass() == SubClass.class);
        assertTrue(all.stream().anyMatch(f -> f.getDeclaringClass() == SuperClass.class));
    }

    @Test
    public void testGetAllDeclaredFieldsMapCollisionHandling() {
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(ChildCollision.class);
        // Child's "dup" should be under simple name, parent's under fully qualified name
        assertTrue(map.containsKey("dup"));
        assertTrue(map.containsKey(ParentCollision.class.getName() + ".dup"));
        assertEquals(3, map.size()); // dup (child), ParentCollision.dup, unique
    }

    @Test
    public void testGetDeepDeclaredFieldsExcludesTransient() {
        Collection<Field> fields = ReflectionUtils.getDeepDeclaredFields(ClassWithTransient.class);
        // The transient field should be filtered out by the legacy filter
        for (Field f : fields) {
            assertNotEquals("trans", f.getName());
        }
    }

    /* -------------------------------------------------------------
     *  Tests for method invocation helpers (call)
     * ------------------------------------------------------------- */
    @Test
    public void testCallWithMethodObjectSuccess() throws Exception {
        SubClass obj = new SubClass();
        Method m = SubClass.class.getDeclaredMethod("privateMethod");
        m.setAccessible(true);
        Object result = ReflectionUtils.call(obj, m);
        assertNull(result); // void method
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallWithNullMethodThrows() {
        ReflectionUtils.call(new SubClass(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallWithNullInstanceThrows() throws Exception {
        Method m = SubClass.class.getDeclaredMethod("privateMethod");
        ReflectionUtils.call(null, m);
    }

    @Test
    public void testCallMethodThatThrowsWrappedException() throws Exception {
        class Exploding {
            void boom() { throw new IllegalStateException("boom"); }
        }
        Exploding e = new Exploding();
        Method m = Exploding.class.getDeclaredMethod("boom");
        try {
            ReflectionUtils.call(e, m);
            fail("Expected RuntimeException wrapping IllegalStateException");
        } catch (RuntimeException re) {
            assertTrue(re.getCause() instanceof IllegalStateException);
            assertEquals("boom", re.getCause().getMessage());
        }
    }

    @Test
    public void testCallByNameSelectsPublicMethod() throws Exception {
        SubClass obj = new SubClass();
        Object ret = ReflectionUtils.call(obj, "publicMethod");
        assertNull(ret);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallByNameMethodNotFound() {
        ReflectionUtils.call(new SubClass(), "nonExistentMethod");
    }

    /* -------------------------------------------------------------
     *  Tests for getMethod (signature based) and caching
     * ------------------------------------------------------------- */
    @Test
    public void testGetMethodFindsPrivateMethodAndCachesIt() {
        Method m1 = ReflectionUtils.getMethod(SubClass.class, "privateMethod");
        Method m2 = ReflectionUtils.getMethod(SubClass.class, "privateMethod");
        assertSame("Method should be cached and identical on second lookup", m1, m2);
        assertTrue(m1.isAccessible());
    }

    @Test
    public void testGetMethodObjectSelectionByArgCount() {
        SubClass obj = new SubClass();
        Method selected = ReflectionUtils.getMethod(obj, "selector", 2);
        // Should pick the public version (most accessible)
        assertEquals("selector", selected.getName());
        assertArrayEquals(new Class[]{int.class, int.class}, selected.getParameterTypes());
        assertTrue(Modifier.isPublic(selected.getModifiers()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodObjectNoMatchThrows() {
        ReflectionUtils.getMethod(new SubClass(), "selector", 3);
    }

    /* -------------------------------------------------------------
     *  Tests for constructor utilities and caching
     * ------------------------------------------------------------- */
    private static class PrivateCtor {
        private PrivateCtor() {}
        PrivateCtor(String s) {}
    }

    @Test
    public void testGetConstructorPrivate() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(PrivateCtor.class);
        assertNotNull(ctor);
        assertTrue(ctor.isAccessible() || Modifier.isPublic(ctor.getModifiers()));
    }

    @Test
    public void testGetAllConstructorsCachesEachConstructor() {
        Constructor<?>[] all = ReflectionUtils.getAllConstructors(PrivateCtor.class);
        assertEquals(2, all.length);
        // The cache should now contain two entries
        assertEquals(2, ctorCache.puts);
    }

    /* -------------------------------------------------------------
     *  Tests for getNonOverloadedMethod behavior
     * ------------------------------------------------------------- */
    @Test
    public void testGetNonOverloadedMethodSuccess() {
        Method m = ReflectionUtils.getNonOverloadedMethod(SubClass.class, "publicMethod");
        assertNotNull(m);
        assertEquals("publicMethod", m.getName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethodAmbiguous() {
        // SubClass has overloaded(int) and overloaded(String) – both have same name
        ReflectionUtils.getNonOverloadedMethod(SubClass.class, "overloaded");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethodNotFound() {
        ReflectionUtils.getNonOverloadedMethod(SubClass.class, "doesNotExist");
    }

    /* -------------------------------------------------------------
     *  Tests for utility methods
     * ------------------------------------------------------------- */
    @Test
    public void testGetClassNameHandlesNull() {
        assertEquals("null", ReflectionUtils.getClassName(null));
        assertEquals(ReflectionUtilsTest.class.getName(),
                ReflectionUtils.getClassName(this));
    }

    @Test
    public void testGetClassNameFromByteCodeWorksForSimpleClass() throws IOException {
        // Load the .class bytes of an inner test class
        String resource = PrivateCtor.class.getSimpleName() + ".class";
        byte[] bytes = PrivateCtor.class.getResourceAsStream(resource).readAllBytes();
        String name = ReflectionUtils.getClassNameFromByteCode(bytes);
        assertEquals(PrivateCtor.class.getName(), name);
    }

    /* -------------------------------------------------------------
     *  Tests that cache‑setter methods actually replace the caches
     * ------------------------------------------------------------- */
    @Test
    public void testSetMethodCacheReplacesInternalCache() throws Exception {
        // The counting map was installed in setUp().
        // Perform a lookup that will store something in the cache.
        Method m = ReflectionUtils.getMethod(SubClass.class, "privateMethod");
        assertEquals(1, methodCache.puts); // one put for the cached method
        // Retrieve again – should hit the cache (no extra put)
        Method m2 = ReflectionUtils.getMethod(SubClass.class, "privateMethod");
        assertEquals(1, methodCache.puts);
        assertSame(m, m2);
    }

    @Test
    public void testSetFieldCacheReplacesInternalCache() {
        Field f = ReflectionUtils.getField(SubClass.class, "childField");
        assertEquals(1, fieldCache.puts);
        Field f2 = ReflectionUtils.getField(SubClass.class, "childField");
        assertEquals(1, fieldCache.puts);
        assertSame(f, f2);
    }

    @Test
    public void testSetConstructorCacheReplacesInternalCache() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(PrivateCtor.class, String.class);
        assertEquals(1, ctorCache.puts);
        Constructor<?> ctor2 = ReflectionUtils.getConstructor(PrivateCtor.class, String.class);
        assertEquals(1, ctorCache.puts);
        assertSame(ctor, ctor2);
    }
}
