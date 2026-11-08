package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

import static org.junit.Assert.*;

public class ReflectionUtilsTest {

    /* -------------------------------------------------------------
     *  Simple annotations used for the tests
     * ------------------------------------------------------------- */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface ClassAnno {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface MethodAnno {}

    /* -------------------------------------------------------------
     *  Test class hierarchy for annotation lookup tests
     * ------------------------------------------------------------- */
    @ClassAnno
    public static class AnnotatedSuper {
        @MethodAnno
        public void annotatedMethod() {}
    }

    public static class ChildOfAnnotatedSuper extends AnnotatedSuper {}

    public interface AnnotatedInterface {}

    @ClassAnno
    public interface AnnotatedSuperInterface {}

    public static class ImplementsAnnotatedInterface implements AnnotatedSuperInterface {}

    /* -------------------------------------------------------------
     *  Helper classes for field / method / constructor tests
     * ------------------------------------------------------------- */
    public static class FieldHolder {
        private String hidden;
        public int publicInt;
        static long staticLong;                 // should be ignored by default filter
        private transient String transientField; // for deprecated deep‑field tests
    }

    public static class OverloadedMethods {
        public String foo() { return "zero"; }
        public String foo(String a) { return "one:" + a; }
        protected String foo(Integer i) { return "int:" + i; }
        private String foo(Object o) { return "obj:" + o; }
    }

    public static class ConstructorHolder {
        private ConstructorHolder() {}
        public ConstructorHolder(String s) {}
    }

    public static class ExceptionThrower {
        public void explode() throws Exception {
            throw new IllegalStateException("boom");
        }
    }

    /* -------------------------------------------------------------
     *  Reset static caches after each test to avoid cross‑test pollution.
     * ------------------------------------------------------------- */
    @After
    public void resetCaches() {
        ReflectionUtils.setMethodCache(new HashMap<>());
        ReflectionUtils.setClassFieldsCache(new HashMap<>());
        ReflectionUtils.setFieldCache(new HashMap<>());
        ReflectionUtils.setClassAnnotationCache(new HashMap<>());
        ReflectionUtils.setMethodAnnotationCache(new HashMap<>());
        ReflectionUtils.setConstructorCache(new HashMap<>());
    }

    /* -------------------------------------------------------------
     *  1.  getClassAnnotation – inheritance & caching
     * ------------------------------------------------------------- */
    @Test
    public void testGetClassAnnotation_Inherited() {
        ClassAnno anno = ReflectionUtils.getClassAnnotation(ChildOfAnnotatedSuper.class, ClassAnno.class);
        assertNotNull("Annotation should be inherited from superclass", anno);

        // second call must hit the cache – we verify by using a custom map that records puts
        Map<Object, Annotation> customCache = new HashMap<>();
        ReflectionUtils.setClassAnnotationCache(customCache);
        ReflectionUtils.getClassAnnotation(ChildOfAnnotatedSuper.class, ClassAnno.class);
        assertEquals("Cache should contain exactly one entry after lookup", 1, customCache.size());
    }

    @Test
    public void testGetClassAnnotation_Interface() {
        ClassAnno anno = ReflectionUtils.getClassAnnotation(ImplementsAnnotatedInterface.class, ClassAnno.class);
        assertNotNull("Annotation should be found on implemented interface", anno);
    }

    @Test
    public void testGetClassAnnotation_NullClassReturnsNull() {
        assertNull(ReflectionUtils.getClassAnnotation(null, ClassAnno.class));
    }

    /* -------------------------------------------------------------
     *  2.  getMethodAnnotation – inheritance & caching
     * ------------------------------------------------------------- */
    @Test
    public void testGetMethodAnnotation_Inherited() throws NoSuchMethodException {
        Method method = ChildOfAnnotatedSuper.class.getMethod("annotatedMethod");
        MethodAnno anno = ReflectionUtils.getMethodAnnotation(method, MethodAnno.class);
        assertNotNull("Method annotation should be inherited from superclass", anno);
    }

    @Test
    public void testGetMethodAnnotation_CacheWorks() throws NoSuchMethodException {
        Map<Object, Annotation> customCache = new HashMap<>();
        ReflectionUtils.setMethodAnnotationCache(customCache);

        Method m = AnnotatedSuper.class.getMethod("annotatedMethod");
        ReflectionUtils.getMethodAnnotation(m, MethodAnno.class);
        assertEquals("Cache should contain exactly one entry", 1, customCache.size());
    }

    /* -------------------------------------------------------------
     *  3.  getField – retrieval and caching
     * ------------------------------------------------------------- */
    @Test
    public void testGetField_FindsPrivateField() {
        Field f = ReflectionUtils.getField(FieldHolder.class, "hidden");
        assertNotNull("Should locate private field 'hidden'", f);
        assertFalse("Field should be accessible after lookup", !f.isAccessible());
    }

    @Test
    public void testGetField_CacheBehaviour() {
        Map<Object, Field> customCache = new HashMap<>();
        ReflectionUtils.setFieldCache(customCache);
        ReflectionUtils.getField(FieldHolder.class, "publicInt");
        assertEquals(1, customCache.size());
        // second call should not increase size
        ReflectionUtils.getField(FieldHolder.class, "publicInt");
        assertEquals(1, customCache.size());
    }

    @Test
    public void testGetField_NonExistingReturnsNull() {
        assertNull(ReflectionUtils.getField(FieldHolder.class, "doesNotExist"));
    }

    /* -------------------------------------------------------------
     *  4.  getDeclaredFields (custom filter) and default filter
     * ------------------------------------------------------------- */
    @Test
    public void testGetDeclaredFields_CustomFilter() {
        Predicate<Field> onlyString = f -> f.getType() == String.class;
        List<Field> strings = ReflectionUtils.getDeclaredFields(FieldHolder.class, onlyString);
        assertEquals(1, strings.size());
        assertEquals("hidden", strings.get(0).getName());
    }

    @Test
    public void testGetDeclaredFields_DefaultFilterExcludesStatic() {
        List<Field> defaults = ReflectionUtils.getDeclaredFields(FieldHolder.class);
        // staticLong should be filtered out
        assertFalse(defaults.stream().anyMatch(f -> f.getName().equals("staticLong")));
        // transientField is NOT filtered by default filter (only deprecated deep methods filter it)
        assertTrue(defaults.stream().anyMatch(f -> f.getName().equals("transientField")));
    }

    /* -------------------------------------------------------------
     *  5.  getAllDeclaredFields (hierarchy) and map collisions
     * ------------------------------------------------------------- */
    public static class ParentCollision {
        private int value;
    }

    public static class ChildCollision extends ParentCollision {
        private String value; // same simple name, different type
    }

    @Test
    public void testGetAllDeclaredFields_HierarchyOrder() {
        List<Field> all = ReflectionUtils.getAllDeclaredFields(ChildCollision.class);
        // child fields first, then parent fields
        assertEquals("value", all.get(0).getName()); // child field
        assertEquals("value", all.get(1).getName()); // parent field
        assertEquals(ChildCollision.class, all.get(0).getDeclaringClass());
        assertEquals(ParentCollision.class, all.get(1).getDeclaringClass());
    }

    @Test
    public void testGetAllDeclaredFieldsMap_CollisionHandling() {
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(ChildCollision.class);
        // simple name should map to child field, parent field gets qualified name
        assertTrue(map.containsKey("value"));
        assertTrue(map.containsKey(ParentCollision.class.getName() + ".value"));
        assertEquals(String.class, map.get("value").getType());
        assertEquals(int.class, map.get(ParentCollision.class.getName() + ".value").getType());
    }

    /* -------------------------------------------------------------
     *  6.  getMethod (exact signature) – private method accessibility
     * ------------------------------------------------------------- */
    public static class PrivateMethodHolder {
        private String secret() { return "shhh"; }
    }

    @Test
    public void testGetMethod_PrivateAccess() {
        Method m = ReflectionUtils.getMethod(PrivateMethodHolder.class, "secret");
        assertNotNull(m);
        assertTrue("Method should be made accessible", m.isAccessible());
        try {
            assertEquals("shhh", m.invoke(new PrivateMethodHolder()));
        } catch (Exception e) {
            fail("Invocation failed: " + e);
        }
    }

    /* -------------------------------------------------------------
     *  7.  getMethod(Object, name, argCount) – selection priority
     * ------------------------------------------------------------- */
    @Test
    public void testGetMethod_ObjectArgCount_SelectsMostAccessible() {
        OverloadedMethods instance = new OverloadedMethods();
        Method m = ReflectionUtils.getMethod(instance, "foo", 1);
        assertNotNull(m);
        // public method with String parameter is more accessible than protected Integer or private Object
        assertEquals(String.class, m.getParameterTypes()[0]);
        assertEquals("public", Modifier.toString(m.getModifiers()).split(" ")[0]); // ensure public
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethod_ObjectArgCount_NoMatchThrows() {
        OverloadedMethods o = new OverloadedMethods();
        ReflectionUtils.getMethod(o, "nonexistent", 2);
    }

    /* -------------------------------------------------------------
     *  8.  call(Object, Method) – normal and exception paths
     * ------------------------------------------------------------- */
    @Test
    public void testCall_WithMethod_ReturnsResult() throws Exception {
        Method m = OverloadedMethods.class.getMethod("foo", String.class);
        Object result = ReflectionUtils.call(new OverloadedMethods(), m, "test");
        assertEquals("one:test", result);
    }

    @Test
    public void testCall_MethodThrowsWrappedRuntimeException() throws Exception {
        Method m = ExceptionThrower.class.getMethod("explode");
        try {
            ReflectionUtils.call(new ExceptionThrower(), m);
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            assertTrue(e.getCause() instanceof IllegalStateException);
            assertEquals("boom", e.getCause().getMessage());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCall_NullMethodThrows() {
        ReflectionUtils.call(new Object(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCall_NullInstanceThrows() throws Exception {
        Method m = OverloadedMethods.class.getMethod("foo");
        ReflectionUtils.call(null, m);
    }

    /* -------------------------------------------------------------
     *  9.  call(Object, String, ...) – method lookup & exception handling
     * ------------------------------------------------------------- */
    @Test
    public void testCall_StringMethodName_Succeeds() {
        OverloadedMethods o = new OverloadedMethods();
        Object r = ReflectionUtils.call(o, "foo");
        assertEquals("zero", r);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCall_StringMethodName_NotFound() {
        ReflectionUtils.call(new OverloadedMethods(), "doesNotExist");
    }

    /* -------------------------------------------------------------
     * 10. getConstructor & getAllConstructors – caching
     * ------------------------------------------------------------- */
    @Test
    public void testGetConstructor_PrivateAndPublic() {
        Constructor<?> privateCtor = ReflectionUtils.getConstructor(PrivateMethodHolder.class);
        assertNotNull(privateCtor);
        assertTrue(privateCtor.isAccessible() || Modifier.isPublic(privateCtor.getModifiers()));

        Constructor<?> publicCtor = ReflectionUtils.getConstructor(ConstructorHolder.class, String.class);
        assertNotNull(publicCtor);
        assertEquals(1, publicCtor.getParameterTypes().length);
    }

    @Test
    public void testGetAllConstructors_CachePopulation() {
        Map<Object, Constructor<?>> customCache = new HashMap<>();
        ReflectionUtils.setConstructorCache(customCache);
        Constructor<?>[] ctors = ReflectionUtils.getAllConstructors(ConstructorHolder.class);
        // Two constructors declared
        assertEquals(2, ctors.length);
        // Cache should now contain both constructors
        assertEquals(2, customCache.size());
    }

    /* -------------------------------------------------------------
     * 11. getNonOverloadedMethod – happy path and error cases
     * ------------------------------------------------------------- */
    public static class SimpleNoArgs {
        public String greet() { return "hi"; }
    }

    @Test
    public void testGetNonOverloadedMethod_FindsMethod() {
        Method m = ReflectionUtils.getNonOverloadedMethod(SimpleNoArgs.class, "greet");
        assertNotNull(m);
        assertEquals(0, m.getParameterCount());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethod_OverloadedThrows() {
        ReflectionUtils.getNonOverloadedMethod(OverloadedMethods.class, "foo");
    }

    /* -------------------------------------------------------------
     * 12. getClassName – null handling
     * ------------------------------------------------------------- */
    @Test
    public void testGetClassName_NullAndNonNull() {
        assertEquals("null", ReflectionUtils.getClassName(null));
        assertEquals(ReflectionUtilsTest.class.getName(),
                ReflectionUtils.getClassName(this));
    }

    /* -------------------------------------------------------------
     * 13. getClassNameFromByteCode – read own class file bytes
     * ------------------------------------------------------------- */
    @Test
    public void testGetClassNameFromByteCode_RealClass() throws IOException {
        // Load the .class bytes of this test class via its class loader
        String resource = '/' + ReflectionUtilsTest.class.getName().replace('.', '/') + ".class";
        InputStream is = ReflectionUtilsTest.class.getResourceAsStream(resource);
        assertNotNull("Unable to locate class file for test class", is);
        byte[] bytes = is.readAllBytes();
        is.close();

        String name = ReflectionUtils.getClassNameFromByteCode(bytes);
        assertEquals(ReflectionUtilsTest.class.getName(), name);
    }

    /* -------------------------------------------------------------
     * 14. Deprecated deep‑field helpers – ensure they delegate correctly
     * ------------------------------------------------------------- */
    @Test
    public void testGetDeepDeclaredFields_UsesLegacyFilter() {
        Collection<Field> deep = ReflectionUtils.getDeepDeclaredFields(FieldHolder.class);
        // Should exclude staticLong (default filter) and also exclude transient field
        assertFalse(deep.stream().anyMatch(f -> f.getName().equals("staticLong")));
        assertFalse(deep.stream().anyMatch(f -> f.getName().equals("transientField")));
    }

    @Test
    public void testGetDeepDeclaredFieldMap_HandlesCollisionAndLegacyFilter() {
        Map<String, Field> map = ReflectionUtils.getDeepDeclaredFieldMap(ChildCollision.class);
        // Same expectations as getAllDeclaredFieldsMap but transient fields filtered out (none here)
        assertTrue(map.containsKey("value"));
        assertTrue(map.containsKey(ParentCollision.class.getName() + ".value"));
    }

    /* -------------------------------------------------------------
     * 15. set*Cache methods – verify that the supplied map is used
     * ------------------------------------------------------------- */
    @Test
    public void testSetMethodCache_UsesSuppliedMap() {
        Map<Object, Method> custom = new HashMap<>();
        ReflectionUtils.setMethodCache(custom);
        Method m = ReflectionUtils.getMethod(OverloadedMethods.class, "foo");
        assertSame("Method should be stored in the custom map", m, custom.values().iterator().next());
    }

    @Test
    public void testSetClassFieldsCache_UsesSuppliedMap() {
        Map<Object, Collection<Field>> custom = new HashMap<>();
        ReflectionUtils.setClassFieldsCache(custom);
        List<Field> fields = ReflectionUtils.getDeclaredFields(FieldHolder.class);
        assertTrue(custom.containsKey(new ReflectionUtils.FieldsCacheKey(FieldHolder.class, ReflectionUtils.DEFAULT_FIELD_FILTER, false)));
        // The cached value should be the same list we received
        assertSame(fields, custom.values().iterator().next());
    }

    @Test
    public void testSetFieldCache_UsesSuppliedMap() {
        Map<Object, Field> custom = new HashMap<>();
        ReflectionUtils.setFieldCache(custom);
        Field f = ReflectionUtils.getField(FieldHolder.class, "publicInt");
        assertSame("Field should be cached in the supplied map", f, custom.values().iterator().next());
    }

    @Test
    public void testSetClassAnnotationCache_UsesSuppliedMap() {
        Map<Object, Annotation> custom = new HashMap<>();
        ReflectionUtils.setClassAnnotationCache(custom);
        Annotation a = ReflectionUtils.getClassAnnotation(AnnotatedSuper.class, ClassAnno.class);
        assertSame(a, custom.values().iterator().next());
    }

    @Test
    public void testSetMethodAnnotationCache_UsesSuppliedMap() throws NoSuchMethodException {
        Map<Object, Annotation> custom = new HashMap<>();
        ReflectionUtils.setMethodAnnotationCache(custom);
        Method m = AnnotatedSuper.class.getMethod("annotatedMethod");
        Annotation a = ReflectionUtils.getMethodAnnotation(m, MethodAnno.class);
        assertSame(a, custom.values().iterator().next());
    }

    @Test
    public void testSetConstructorCache_UsesSuppliedMap() {
        Map<Object, Constructor<?>> custom = new HashMap<>();
        ReflectionUtils.setConstructorCache(custom);
        Constructor<?> c = ReflectionUtils.getConstructor(ConstructorHolder.class, String.class);
        assertSame(c, custom.values().iterator().next());
    }
}
