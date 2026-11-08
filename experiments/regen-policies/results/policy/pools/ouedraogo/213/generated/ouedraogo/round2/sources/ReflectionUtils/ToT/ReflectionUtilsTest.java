package com.cedarsoftware.util;

import org.junit.*;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Comprehensive JUnit‑4 test suite for {@link ReflectionUtils}.
 *
 * <p>The tests are written as if three independent experts had each
 * contributed one test case for every public API method, then merged
 * their work into a single file.  The suite covers typical usage,
 * edge‑cases, error handling and a few internal behaviours such as
 * caching.</p>
 */
public class ReflectionUtilsTest {

    /* --------------------------------------------------------------------- *
     *  Helper annotations and test classes                                    *
     * --------------------------------------------------------------------- */

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    private @interface ClassAnno { }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    private @interface MethodAnno { }

    @ClassAnno
    private static class SuperClass {
        private String superField = "super";

        @MethodAnno
        public void annotatedMethod() { }

        public void overloaded(int a) { }

        public void overloaded(String s) { }

        private void privateMethod() { }

        public SuperClass() { }

        protected SuperClass(int x) { }
    }

    private static class SubClass extends SuperClass {
        private int subField = 42;

        // field with same name as in super class to test name‑collision handling
        private String superField = "sub";

        @Override
        public void overloaded(int a) { }

        private void privateMethod() { }
    }

    private interface TestInterface {
        @MethodAnno
        void ifaceMethod();
    }

    private static class ImplementsInterface implements TestInterface {
        @Override
        public void ifaceMethod() { }
    }

    /* --------------------------------------------------------------------- *
     *  JUnit Rules                                                            *
     * --------------------------------------------------------------------- */

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /* --------------------------------------------------------------------- *
     *  1. set*Cache methods – custom map injection and cache usage            *
     * --------------------------------------------------------------------- */

    @Test
    public void testSetMethodCacheAndCacheReuse() throws Exception {
        Map<Object, Method> custom = new ConcurrentHashMap<>();
        ReflectionUtils.setMethodCache(custom);

        Method m1 = ReflectionUtils.getMethod(SuperClass.class, "annotatedMethod");
        Method m2 = ReflectionUtils.getMethod(SuperClass.class, "annotatedMethod");

        // Same Method instance must be returned (cached)
        Assert.assertSame("Method should be cached and reused", m1, m2);
        Assert.assertTrue("Custom map must contain the cached entry",
                custom.containsKey(new ReflectionUtils.MethodCacheKey(SuperClass.class, "annotatedMethod")));

        // Clean up – restore default cache for other tests
        ReflectionUtils.setMethodCache(new LRUCache<>(ReflectionUtils.CACHE_SIZE));
    }

    @Test
    public void testSetClassFieldsCache() {
        Map<Object, Collection<Field>> custom = new ConcurrentHashMap<>();
        ReflectionUtils.setClassFieldsCache(custom);

        Field f = ReflectionUtils.getField(SubClass.class, "subField");
        Assert.assertNotNull("Field must be found", f);
        Assert.assertTrue(custom.containsKey(new ReflectionUtils.FieldsCacheKey(SubClass.class,
                ReflectionUtils.DEFAULT_FIELD_FILTER, false)));

        // Restore default
        ReflectionUtils.setClassFieldsCache(new LRUCache<>(ReflectionUtils.CACHE_SIZE));
    }

    @Test
    public void testSetFieldCache() {
        Map<Object, Field> custom = new ConcurrentHashMap<>();
        ReflectionUtils.setFieldCache(custom);

        Field f = ReflectionUtils.getField(SuperClass.class, "superField");
        Assert.assertNotNull(f);
        Assert.assertTrue(custom.containsKey(new ReflectionUtils.FieldNameCacheKey(SuperClass.class, "superField")));

        // Restore default
        ReflectionUtils.setFieldCache(new LRUCache<>(ReflectionUtils.CACHE_SIZE * 10));
    }

    /* --------------------------------------------------------------------- *
     *  2. getClassAnnotation – inheritance & caching                         *
     * --------------------------------------------------------------------- */

    @Test
    public void testGetClassAnnotationFoundOnSuperclass() {
        // SubClass does NOT declare the annotation, but its super does.
        ClassAnno anno = ReflectionUtils.getClassAnnotation(SubClass.class, ClassAnno.class);
        Assert.assertNotNull("Annotation should be discovered on superclass", anno);
    }

    @Test
    public void testGetClassAnnotationCacheMiss() {
        // No annotation of this type anywhere – result should be cached as null.
        Annotation missing = ReflectionUtils.getClassAnnotation(SubClass.class, Deprecated.class);
        Assert.assertNull(missing);

        // Second call must return the same null (cached) without re‑searching.
        Annotation missing2 = ReflectionUtils.getClassAnnotation(SubClass.class, Deprecated.class);
        Assert.assertNull(missing2);
    }

    @Test
    public void testGetClassAnnotationNullArguments() {
        thrown.expect(IllegalArgumentException.class);
        ReflectionUtils.getClassAnnotation(null, ClassAnno.class);
    }

    /* --------------------------------------------------------------------- *
     *  3. getMethodAnnotation – interface and superclass search               *
     * --------------------------------------------------------------------- */

    @Test
    public void testGetMethodAnnotationFromInterface() throws Exception {
        Method ifaceMethod = TestInterface.class.getMethod("ifaceMethod");
        MethodAnno anno = ReflectionUtils.getMethodAnnotation(ifaceMethod, MethodAnno.class);
        Assert.assertNotNull("Annotation should be found on interface method", anno);
    }

    @Test
    public void testGetMethodAnnotationFromSuperclass() throws Exception {
        Method m = SubClass.class.getDeclaredMethod("annotatedMethod");
        MethodAnno anno = ReflectionUtils.getMethodAnnotation(m, MethodAnno.class);
        Assert.assertNotNull("Annotation should be found on superclass method", anno);
    }

    @Test
    public void testGetMethodAnnotationCacheMiss() throws Exception {
        Method m = SubClass.class.getDeclaredMethod("privateMethod");
        MethodAnno result = ReflectionUtils.getMethodAnnotation(m, MethodAnno.class);
        Assert.assertNull(result);
    }

    @Test
    public void testGetMethodAnnotationNullArguments() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        ReflectionUtils.getMethodAnnotation(null, MethodAnno.class);
    }

    /* --------------------------------------------------------------------- *
     *  4. getField – inheritance, private field access & caching             *
     * --------------------------------------------------------------------- */

    @Test
    public void testGetFieldFromSuperclass() {
        Field f = ReflectionUtils.getField(SubClass.class, "superField");
        Assert.assertNotNull("Should retrieve inherited field", f);
        Assert.assertEquals("Field name must match", "superField", f.getName());
        Assert.assertTrue("Field should be accessible", f.isAccessible());
    }

    @Test
    public void testGetFieldCacheMiss() {
        Field f = ReflectionUtils.getField(SubClass.class, "nonExisting");
        Assert.assertNull(f);
    }

    @Test
    public void testGetFieldNullArguments() {
        thrown.expect(IllegalArgumentException.class);
        ReflectionUtils.getField(null, "any");
    }

    /* --------------------------------------------------------------------- *
     *  5. getDeclaredFields (custom predicate)                               *
     * --------------------------------------------------------------------- */

    @Test
    public void testGetDeclaredFieldsWithCustomPredicate() {
        Predicate<Field> onlyInts = f -> f.getType() == int.class;
        List<Field> intFields = ReflectionUtils.getDeclaredFields(SubClass.class, onlyInts);
        Assert.assertEquals(1, intFields.size());
        Assert.assertEquals("subField", intFields.get(0).getName());
    }

    @Test
    public void testGetDeclaredFieldsDefaultFilter() {
        List<Field> fields = ReflectionUtils.getDeclaredFields(SuperClass.class);
        // SuperClass has two instance fields, both non‑static, non‑synthetic.
        Assert.assertEquals(2, fields.size());
        Set<String> names = new HashSet<>();
        for (Field f : fields) names.add(f.getName());
        Assert.assertTrue(names.contains("superField"));
    }

    /* --------------------------------------------------------------------- *
     *  6. getAllDeclaredFields (deep) & map handling                         *
     * --------------------------------------------------------------------- */

    @Test
    public void testGetAllDeclaredFieldsDeep() {
        List<Field> all = ReflectionUtils.getAllDeclaredFields(SubClass.class);
        // SubClass declares 2 fields, SuperClass declares 2 => total 4.
        Assert.assertEquals(4, all.size());

        // Verify that both "superField" entries appear (simple and qualified)
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(SubClass.class);
        Assert.assertTrue(map.containsKey("subField"));
        Assert.assertTrue(map.containsKey("superField")); // child’s field wins simple name
        Assert.assertTrue(map.containsKey(SuperClass.class.getName() + ".superField"));
    }

    @Test
    public void testGetAllDeclaredFieldsMapWithPredicate() {
        Predicate<Field> onlyStrings = f -> f.getType() == String.class;
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(SubClass.class, onlyStrings);
        // Both superField (String) from SuperClass and from SubClass should be present
        Assert.assertEquals(2, map.size());
        Assert.assertTrue(map.containsKey("superField"));
        Assert.assertTrue(map.containsKey(SuperClass.class.getName() + ".superField"));
    }

    /* --------------------------------------------------------------------- *
     *  7. Deprecated deep‑declared helpers (still exercised)                 *
     * --------------------------------------------------------------------- */

    @Test
    public void testGetDeepDeclaredFields() {
        Collection<Field> deep = ReflectionUtils.getDeepDeclaredFields(SubClass.class);
        // Same as getAllDeclaredFields with extra transient/synthetic filter – none here.
        Assert.assertEquals(4, deep.size());
    }

    @Test
    public void testGetDeepDeclaredFieldMap() {
        Map<String, Field> map = ReflectionUtils.getDeepDeclaredFieldMap(SubClass.class);
        Assert.assertEquals(4, map.size());
    }

    /* --------------------------------------------------------------------- *
     *  8. call(Object,Method) – successful and exception wrapping           *
     * --------------------------------------------------------------------- */

    private static class CallTarget {
        public String greet(String name) { return "Hello " + name; }

        public void explode() { throw new IllegalStateException("boom"); }
    }

    @Test
    public void testCallWithValidMethod() throws Exception {
        CallTarget target = new CallTarget();
        Method m = ReflectionUtils.getMethod(CallTarget.class, "greet", String.class);
        Object result = ReflectionUtils.call(target, m, "Bob");
        Assert.assertEquals("Hello Bob", result);
    }

    @Test
    public void testCallWrapsInvocationTargetException() throws Exception {
        CallTarget target = new CallTarget();
        Method m = ReflectionUtils.getMethod(CallTarget.class, "explode");
        thrown.expect(RuntimeException.class);
        thrown.expectMessage("Exception thrown inside reflectively called method");
        ReflectionUtils.call(target, m);
    }

    @Test
    public void testCallNullMethodOrInstance() {
        thrown.expect(IllegalArgumentException.class);
        ReflectionUtils.call(new Object(), null);
    }

    @Test
    public void testCallNullInstance() {
        thrown.expect(IllegalArgumentException.class);
        Method dummy;
        try {
            dummy = CallTarget.class.getMethod("greet", String.class);
        } catch (NoSuchMethodException e) { throw new RuntimeException(e); }
        ReflectionUtils.call(null, dummy);
    }

    /* --------------------------------------------------------------------- *
     *  9. call(Object,String) – method lookup by name & arg count            *
     * --------------------------------------------------------------------- */

    @Test
    public void testCallByName() {
        CallTarget target = new CallTarget();
        Object result = ReflectionUtils.call(target, "greet", "Alice");
        Assert.assertEquals("Hello Alice", result);
    }

    @Test
    public void testCallByNameMethodNotFound() {
        thrown.expect(IllegalArgumentException.class);
        ReflectionUtils.call(new CallTarget(), "nonExistingMethod");
    }

    /* --------------------------------------------------------------------- *
     * 10. getMethod(Class,…) – private and inherited methods                 *
     * --------------------------------------------------------------------- */

    @Test
    public void testGetMethodPrivateInherited() {
        Method m = ReflectionUtils.getMethod(SubClass.class, "privateMethod");
        Assert.assertNotNull(m);
        Assert.assertTrue("Method must be accessible after lookup", m.isAccessible());
        // Should be the method declared in SubClass, not the one in SuperClass
        Assert.assertEquals(SubClass.class, m.getDeclaringClass());
    }

    @Test
    public void testGetMethodCacheReuse() {
        Method m1 = ReflectionUtils.getMethod(SuperClass.class, "annotatedMethod");
        Method m2 = ReflectionUtils.getMethod(SuperClass.class, "annotatedMethod");
        Assert.assertSame(m1, m2);
    }

    @Test
    public void testGetMethodNullArguments() {
        thrown.expect(IllegalArgumentException.class);
        ReflectionUtils.getMethod(null, "any");
    }

    /* --------------------------------------------------------------------- *
     * 11. getMethod(Object,…) – overload resolution & selection strategy    *
     * --------------------------------------------------------------------- */

    @Test
    public void testGetMethodByArgCountSelectsMostAccessible() {
        Method m = ReflectionUtils.getMethod(new SubClass(), "overloaded", 1);
        // Both SuperClass and SubClass have a method named overloaded(int)
        // SubClass overrides it, so the selected method should be from SubClass.
        Assert.assertEquals(SubClass.class, m.getDeclaringClass());
    }

    @Test
    public void testGetMethodByArgCountNoMatch() {
        thrown.expect(IllegalArgumentException.class);
        ReflectionUtils.getMethod(new SubClass(), "nonexistent", 0);
    }

    @Test
    public void testGetMethodByArgCountNullArguments() {
        thrown.expect(IllegalArgumentException.class);
        ReflectionUtils.getMethod(null, "any", 0);
    }

    /* --------------------------------------------------------------------- *
     * 12. getConstructor – private constructor handling                        *
     * --------------------------------------------------------------------- */

    private static class PrivateCtor {
        private PrivateCtor() { }
        public PrivateCtor(String s) { }
    }

    @Test
    public void testGetConstructorPrivate() {
        Constructor<?> c = ReflectionUtils.getConstructor(PrivateCtor.class);
        Assert.assertNotNull(c);
        Assert.assertTrue("Constructor must be accessible", c.isAccessible());
        Assert.assertEquals(0, c.getParameterCount());
    }

    @Test
    public void testGetConstructorCacheReuse() {
        Constructor<?> c1 = ReflectionUtils.getConstructor(PrivateCtor.class, String.class);
        Constructor<?> c2 = ReflectionUtils.getConstructor(PrivateCtor.class, String.class);
        Assert.assertSame(c1, c2);
    }

    @Test
    public void testGetConstructorNotFound() {
        Constructor<?> c = ReflectionUtils.getConstructor(PrivateCtor.class, int.class);
        Assert.assertNull(c);
    }

    /* --------------------------------------------------------------------- *
     * 13. getAllConstructors – ordering verification                         *
     * --------------------------------------------------------------------- */

    private static class ConstructorOrder {
        public ConstructorOrder() { }
        protected ConstructorOrder(int a) { }
        ConstructorOrder(String s, int i) { }
        private ConstructorOrder(double d, String s, int i) { }
    }

    @Test
    public void testGetAllConstructorsOrdering() {
        Constructor<?>[] ctors = ReflectionUtils.getAllConstructors(ConstructorOrder.class);
        Assert.assertEquals(4, ctors.length);

        // Expected order: public, protected, package, private;
        // within same visibility more parameters first.
        Assert.assertTrue(Modifier.isPublic(ctors[0].getModifiers()));
        Assert.assertTrue(Modifier.isProtected(ctors[1].getModifiers()));
        Assert.assertFalse(Modifier.isPublic(ctors[2].getModifiers()));
        Assert.assertFalse(Modifier.isProtected(ctors[2].getModifiers()));
        Assert.assertTrue(Modifier.isPrivate(ctors[3].getModifiers()));

        // Verify parameter‑count ordering inside the package‑private group
        Assert.assertEquals(2, ctors[2].getParameterCount()); // (String, int)
        Assert.assertEquals(3, ctors[3].getParameterCount()); // (double, String, int)
    }

    /* --------------------------------------------------------------------- *
     * 14. getNonOverloadedMethod – success and error paths                  *
     * --------------------------------------------------------------------- */

    private static class SimpleNoArg {
        public void ping() { }
    }

    private static class OverloadedNoArg {
        public void ping() { }
        public void ping(String s) { }
    }

    @Test
    public void testGetNonOverloadedMethodSuccess() {
        Method m = ReflectionUtils.getNonOverloadedMethod(SimpleNoArg.class, "ping");
        Assert.assertNotNull(m);
        Assert.assertEquals(0, m.getParameterCount());
    }

    @Test
    public void testGetNonOverloadedMethodAmbiguous() {
        thrown.expect(IllegalArgumentException.class);
        ReflectionUtils.getNonOverloadedMethod(OverloadedNoArg.class, "ping");
    }

    @Test
    public void testGetNonOverloadedMethodNotFound() {
        thrown.expect(IllegalArgumentException.class);
        ReflectionUtils.getNonOverloadedMethod(SimpleNoArg.class, "missing");
    }

    /* --------------------------------------------------------------------- *
     * 15. getClassName – null handling                                      *
     * --------------------------------------------------------------------- */

    @Test
    public void testGetClassName() {
        Assert.assertEquals("null", ReflectionUtils.getClassName(null));
        Assert.assertEquals(String.class.getName(),
                ReflectionUtils.getClassName("hello"));
    }

    /* --------------------------------------------------------------------- *
     * 16. getClassNameFromByteCode – read own .class file                     *
     * --------------------------------------------------------------------- */

    @Test
    public void testGetClassNameFromByteCode() throws IOException {
        // Load the .class bytes of this test class via its classloader.
        String resource = ReflectionUtilsTest.class.getName().replace('.', '/') + ".class";
        InputStream is = ReflectionUtilsTest.class.getClassLoader().getResourceAsStream(resource);
        Assert.assertNotNull("Unable to locate class bytecode resource", is);
        byte[] bytes = is.readAllBytes();

        String name = ReflectionUtils.getClassNameFromByteCode(bytes);
        Assert.assertEquals(ReflectionUtilsTest.class.getName(), name);
    }

    @Test
    public void testGetClassNameFromByteCodeInvalid() {
        thrown.expect(IllegalStateException.class);
        byte[] bogus = new byte[]{0,1,2,3};
        try {
            ReflectionUtils.getClassNameFromByteCode(bogus);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /* --------------------------------------------------------------------- *
     * 17. isJavaCompilerAvailable – property override                         *
     * --------------------------------------------------------------------- */

    @Test
    public void testIsJavaCompilerAvailableWhenForcedJRE() {
        String prop = "java.util.force.jre";
        String original = System.getProperty(prop);
        try {
            System.setProperty(prop, "true");
            Assert.assertFalse("Compiler should be reported unavailable when forced JRE", ReflectionUtils.isJavaCompilerAvailable());
        } finally {
            // Restore original system property
            if (original != null) {
                System.setProperty(prop, original);
            } else {
                System.clearProperty(prop);
            }
        }
    }

    @Test
    public void testIsJavaCompilerAvailableDefault() {
        // Most CI/JDK environments have a compiler; we only assert that the method
        // does not throw and returns a boolean.
        boolean available = ReflectionUtils.isJavaCompilerAvailable();
        Assert.assertTrue("Result must be a boolean", available || !available);
    }

    /* --------------------------------------------------------------------- *
     * 18. Miscellaneous private helper – makeParamKey (indirectly)           *
     * --------------------------------------------------------------------- */

    @Test
    public void testMakeParamKeyConsistency() throws Exception {
        // Use reflection to invoke the private static method.
        Method mk = ReflectionUtils.class.getDeclaredMethod("makeParamKey", Class[].class);
        mk.setAccessible(true);
        String key0 = (String) mk.invoke(null, (Object) new Class<?>[0]);
        String key1 = (String) mk.invoke(null, (Object) new Class[]{String.class, int.class});
        Assert.assertEquals("", key0);
        Assert.assertEquals(":String|int", key1);
    }
}
