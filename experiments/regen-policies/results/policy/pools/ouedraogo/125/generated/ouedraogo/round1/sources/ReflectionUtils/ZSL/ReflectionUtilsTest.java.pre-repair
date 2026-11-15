package com.cedarsoftware.util;

import org.junit.*;
import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

import static org.junit.Assert.*;

public class ReflectionUtilsTest {

    /* ----------------------------------------------------------------------
     * Helper annotations, interfaces and classes used throughout the tests
     * ---------------------------------------------------------------------- */

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    private @interface TestAnno {}

    @TestAnno
    private static class BaseAnnotated {}

    private static class SubAnnotated extends BaseAnnotated {}

    private interface AnnotatedInterface {
        @TestAnno
        void annotatedMethod();
    }

    private static class ImplementsInterface implements AnnotatedInterface {
        @Override public void annotatedMethod() {}
        public void annotatedMethod(String s) {} // overload, not annotated
    }

    private static class SuperClass {
        private String superField = "super";
    }

    private static class SubClass extends SuperClass {
        private int subField = 42;
    }

    private static class CollisionParent {
        public String dup = "parent";
    }

    private static class CollisionChild extends CollisionParent {
        public String dup = "child";
    }

    private static class PrivateCtor {
        private PrivateCtor() {}
    }

    private static class MultipleMethods {
        public void foo() {}
        protected void foo(Object o) {}
        private void foo(String s) {}
    }

    private static class ThrowsException {
        public void explode() {
            throw new IllegalStateException("boom");
        }
    }

    private static enum SampleEnum {
        ONE, TWO;
        private static final String internal = "ignore";
    }

    /* ----------------------------------------------------------------------
     * Test fixtures
     * ---------------------------------------------------------------------- */

    @Before
    public void setUp() {
        // Replace all caches with simple HashMaps to make cache behaviour observable
        ReflectionUtils.setMethodCache(new HashMap<>());
        ReflectionUtils.setClassFieldsCache(new HashMap<>());
        ReflectionUtils.setFieldCache(new HashMap<>());
        ReflectionUtils.setClassAnnotationCache(new HashMap<>());
        ReflectionUtils.setMethodAnnotationCache(new HashMap<>());
        ReflectionUtils.setConstructorCache(new HashMap<>());
    }

    /* ----------------------------------------------------------------------
     * getClassAnnotation()
     * ---------------------------------------------------------------------- */

    @Test
    public void testGetClassAnnotationFoundInSuperclass() {
        TestAnno anno = ReflectionUtils.getClassAnnotation(SubAnnotated.class, TestAnno.class);
        assertNotNull("Annotation should be found on superclass", anno);
    }

    @Test
    public void testGetClassAnnotationMissing() {
        TestAnno anno = ReflectionUtils.getClassAnnotation(String.class, TestAnno.class);
        assertNull("Annotation should be missing", anno);
    }

    @Test
    public void testGetClassAnnotationCachesMisses() {
        Map<Object, Annotation> cache = new HashMap<>();
        ReflectionUtils.setClassAnnotationCache(cache);
        // First call – cache miss stored as null
        ReflectionUtils.getClassAnnotation(String.class, TestAnno.class);
        assertTrue("Cache should contain a key after miss", cache.containsKey(
                new ReflectionUtils.ClassAnnotationCacheKey(String.class, TestAnno.class)));
        // Second call – should retrieve the same null without recomputing
        assertNull(ReflectionUtils.getClassAnnotation(String.class, TestAnno.class));
    }

    /* ----------------------------------------------------------------------
     * getMethodAnnotation()
     * ---------------------------------------------------------------------- */

    @Test
    public void testGetMethodAnnotationFoundOnInterface() throws Exception {
        Method m = ImplementsInterface.class.getMethod("annotatedMethod");
        TestAnno anno = ReflectionUtils.getMethodAnnotation(m, TestAnno.class);
        assertNotNull("Annotation should be found on interface method", anno);
    }

    @Test
    public void testGetMethodAnnotationNotFound() throws Exception {
        Method m = ImplementsInterface.class.getMethod("annotatedMethod", String.class);
        TestAnno anno = ReflectionUtils.getMethodAnnotation(m, TestAnno.class);
        assertNull("Annotation should be missing", anno);
    }

    /* ----------------------------------------------------------------------
     * getField()
     * ---------------------------------------------------------------------- */

    @Test
    public void testGetFieldFindsPrivateSuperclassField() {
        Field f = ReflectionUtils.getField(SubClass.class, "superField");
        assertNotNull("Should locate private field in superclass", f);
        assertEquals("superField", f.getName());
        assertFalse("Field should be made accessible", f.isAccessible() && !Modifier.isPublic(f.getModifiers()));
    }

    @Test
    public void testGetFieldCachesResult() {
        Map<Object, Field> cache = new HashMap<>();
        ReflectionUtils.setFieldCache(cache);
        Field f1 = ReflectionUtils.getField(SubClass.class, "subField");
        Field f2 = ReflectionUtils.getField(SubClass.class, "subField");
        assertSame("Second retrieval should hit cache", f1, f2);
        assertTrue(cache.containsKey(new ReflectionUtils.FieldNameCacheKey(SubClass.class, "subField")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetFieldNullClass() {
        ReflectionUtils.getField(null, "any");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetFieldNullName() {
        ReflectionUtils.getField(SubClass.class, null);
    }

    /* ----------------------------------------------------------------------
     * getDeclaredFields() with default filter
     * ---------------------------------------------------------------------- */

    @Test
    public void testGetDeclaredFieldsDefaultFilterExcludesStaticAndEnumInternals() {
        List<Field> fields = ReflectionUtils.getDeclaredFields(SampleEnum.class);
        // Should exclude "internal" static field and synthetic "$VALUES"
        for (Field f : fields) {
            assertFalse("Should not contain static field", Modifier.isStatic(f.getModifiers()));
            assertFalse("Should not contain internal enum field", f.getName().equals("internal"));
        }
    }

    @Test
    public void testGetDeclaredFieldsCacheWorksWithPredicate() {
        Predicate<Field> nameStartsWithS = f -> f.getName().startsWith("s");
        List<Field> first = ReflectionUtils.getDeclaredFields(SubClass.class, nameStartsWithS);
        List<Field> second = ReflectionUtils.getDeclaredFields(SubClass.class, nameStartsWithS);
        assertSame("Cached list should be returned on second call", first, second);
    }

    /* ----------------------------------------------------------------------
     * getAllDeclaredFieldsMap() – name collision handling
     * ---------------------------------------------------------------------- */

    @Test
    public void testGetAllDeclaredFieldsMapHandlesCollision() {
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(CollisionChild.class);
        // Child field should be under simple name
        assertTrue(map.containsKey("dup"));
        assertEquals(CollisionChild.class, map.get("dup").getDeclaringClass());

        // Parent field should be fully qualified
        String qualified = CollisionParent.class.getName() + ".dup";
        assertTrue(map.containsKey(qualified));
        assertEquals(CollisionParent.class, map.get(qualified).getDeclaringClass());
    }

    /* ----------------------------------------------------------------------
     * call(Object, Method) – exception handling
     * ---------------------------------------------------------------------- */

    @Test
    public void testCallMethodThrowsRuntimeExceptionWithCause() throws Exception {
        ThrowsException target = new ThrowsException();
        Method m = ThrowsException.class.getMethod("explode");
        try {
            ReflectionUtils.call(target, m);
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            assertTrue(e.getCause() instanceof IllegalStateException);
            assertEquals("boom", e.getCause().getMessage());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallObjectMethodNullMethod() {
        ReflectionUtils.call(new Object(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallObjectMethodNullInstance() throws Exception {
        Method m = Object.class.getMethod("toString");
        ReflectionUtils.call(null, m);
    }

    /* ----------------------------------------------------------------------
     * call(Object, String) – method lookup by name/arg count
     * ---------------------------------------------------------------------- */

    @Test
    public void testCallByNameInvokesCorrectMethod() {
        MultipleMethods target = new MultipleMethods();
        Object result = ReflectionUtils.call(target, "foo");
        // The no‑arg public method returns void, so result should be null
        assertNull(result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallByNameMethodNotFound() {
        ReflectionUtils.call(new Object(), "nonExistingMethod");
    }

    /* ----------------------------------------------------------------------
     * getMethod(Class, name, types) – private method accessibility
     * ---------------------------------------------------------------------- */

    private static class PrivateMethodClass {
        private String secret() { return "secret"; }
    }

    @Test
    public void testGetMethodFindsPrivateMethodAndMakesAccessible() throws Exception {
        Method m = ReflectionUtils.getMethod(PrivateMethodClass.class, "secret");
        assertNotNull(m);
        assertTrue("Method should be accessible", m.isAccessible() || Modifier.isPublic(m.getModifiers()));
        PrivateMethodClass instance = new PrivateMethodClass();
        Object val = m.invoke(instance);
        assertEquals("secret", val);
    }

    @Test
    public void testGetMethodReturnsNullWhenNotFound() {
        Method m = ReflectionUtils.getMethod(String.class, "nonExisting", int.class);
        assertNull(m);
    }

    /* ----------------------------------------------------------------------
     * getMethod(Object, name, argCount) – selection priority
     * ---------------------------------------------------------------------- */

    @Test
    public void testGetMethodByArgCountSelectsMostAccessible() {
        MultipleMethods target = new MultipleMethods();
        Method m = ReflectionUtils.getMethod(target, "foo", 1);
        // Expect the protected method (more accessible than private) to be chosen
        assertEquals(1, m.getParameterCount());
        assertFalse(m.isSynthetic());
        assertTrue(Modifier.isProtected(m.getModifiers()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodByArgCountNotFound() {
        ReflectionUtils.getMethod(new Object(), "missingMethod", 2);
    }

    /* ----------------------------------------------------------------------
     * getConstructor() – private constructor handling
     * ---------------------------------------------------------------------- */

    @Test
    public void testGetConstructorFindsPrivateConstructor() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(PrivateCtor.class);
        assertNotNull(ctor);
        assertTrue(ctor.isAccessible() || Modifier.isPublic(ctor.getModifiers()));
    }

    @Test
    public void testGetAllConstructorsCachesInstances() {
        Constructor<?>[] ctors = ReflectionUtils.getAllConstructors(PrivateCtor.class);
        assertEquals(1, ctors.length);
        Constructor<?> cached = ReflectionUtils.getConstructor(PrivateCtor.class);
        assertSame("Should retrieve same cached constructor", cached, ctors[0]);
    }

    /* ----------------------------------------------------------------------
     * getClassNameFromByteCode()
     * ---------------------------------------------------------------------- */

    @Test
    public void testGetClassNameFromByteCode() throws Exception {
        // Load the bytecode of this test class itself
        String className = ReflectionUtilsTest.class.getName();
        String resource = "/" + className.replace('.', '/') + ".class";
        InputStream is = ReflectionUtilsTest.class.getResourceAsStream(resource);
        assertNotNull("Class file should be loadable", is);
        byte[] bytes = readAllBytes(is);
        String resolved = ReflectionUtils.getClassNameFromByteCode(bytes);
        assertEquals(className, resolved);
    }

    private static byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = is.read(buf)) != -1) {
            baos.write(buf, 0, n);
        }
        return baos.toByteArray();
    }

    /* ----------------------------------------------------------------------
     * getClassName()
     * ---------------------------------------------------------------------- */

    @Test
    public void testGetClassNameHandlesNull() {
        assertEquals("null", ReflectionUtils.getClassName(null));
    }

    @Test
    public void testGetClassNameReturnsCorrectName() {
        String name = ReflectionUtils.getClassName(this);
        assertEquals(ReflectionUtilsTest.class.getName(), name);
    }

    /* ----------------------------------------------------------------------
     * getNonOverloadedMethod()
     * ---------------------------------------------------------------------- */

    private static class SingleMethod {
        public String greet() { return "hi"; }
    }

    private static class OverloadedMethod {
        public String greet() { return "hi"; }
        public String greet(String name) { return "hi " + name; }
    }

    @Test
    public void testGetNonOverloadedMethodSuccess() throws Exception {
        Method m = ReflectionUtils.getNonOverloadedMethod(SingleMethod.class, "greet");
        assertNotNull(m);
        assertEquals(0, m.getParameterCount());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethodFailsOnOverload() throws Exception {
        ReflectionUtils.getNonOverloadedMethod(OverloadedMethod.class, "greet");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethodFailsOnMissing() throws Exception {
        ReflectionUtils.getNonOverloadedMethod(SingleMethod.class, "missing");
    }

    /* ----------------------------------------------------------------------
     * Cache replacement sanity checks
     * ---------------------------------------------------------------------- */

    @Test
    public void testCustomMethodCacheIsUsed() {
        Map<Object, Method> custom = new LinkedHashMap<>();
        ReflectionUtils.setMethodCache(custom);
        Method m = ReflectionUtils.getMethod(String.class, "length");
        assertTrue(custom.containsKey(new ReflectionUtils.MethodCacheKey(String.class, "length")));
        assertSame(m, custom.get(new ReflectionUtils.MethodCacheKey(String.class, "length")));
    }

    @Test
    public void testCustomConstructorCacheIsUsed() {
        Map<Object, Constructor<?>> custom = new LinkedHashMap<>();
        ReflectionUtils.setConstructorCache(custom);
        Constructor<?> ctor = ReflectionUtils.getConstructor(ArrayList.class);
        assertTrue(custom.containsKey(new ReflectionUtils.ConstructorCacheKey(ArrayList.class)));
        assertSame(ctor, custom.get(new ReflectionUtils.ConstructorCacheKey(ArrayList.class)));
    }
}
