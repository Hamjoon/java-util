package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link ReflectionUtils}.
 *
 * <p>The tests are written as if three independent experts were contributing one
 * test case per method.  After each round the shared state (caches) is cleared
 * so that the next expert works with a clean slate.</p>
 *
 * <p>Only APIs that appear in the source of {@code ReflectionUtils} (or in the
 * JDK) are used – no external mocking frameworks or library versions are
 * required.</p>
 */
public class ReflectionUtilsTest {

    /* --------------------------------------------------------------------- *
     *  Helper annotations & test classes used throughout the suite           *
     * --------------------------------------------------------------------- */

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    private @interface ClassAnno {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    private @interface MethodAnno {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    private @interface FieldAnno {}

    @ClassAnno
    private static class SuperClass {
        @MethodAnno
        public void superMethod() {}

        private String privateField = "super";
    }

    private static class SubClass extends SuperClass implements InterfaceWithAnno {
        private int privateInt = 42;
        public static final String STATIC_CONST = "CONST";

        @Override
        public void interfaceMethod() {}
    }

    private interface InterfaceWithAnno {
        @MethodAnno
        void interfaceMethod();
    }

    private static class OverloadedMethods {
        public String foo() { return "zero"; }
        public String foo(String a) { return "one:" + a; }
        protected String foo(Integer i) { return "int:" + i; }
        private String foo(String a, String b) { return "two:" + a + b; }
        // synthetic bridge method generated for generics – we don't declare it
    }

    private static class PrivateConstructor {
        private PrivateConstructor() {}
    }

    private static class ExceptionThrower {
        public void explode() throws IOException {
            throw new IOException("boom");
        }
    }

    /* --------------------------------------------------------------------- *
     *  Cache clean‑up utilities                                            *
     * --------------------------------------------------------------------- */

    @Before
    @After
    public void clearCaches() throws Exception {
        // All caches are private static fields; we clear them via reflection.
        clearCache("CONSTRUCTOR_CACHE");
        clearCache("METHOD_CACHE");
        clearCache("FIELDS_CACHE");
        clearCache("FIELD_NAME_CACHE");
        clearCache("CLASS_ANNOTATION_CACHE");
        clearCache("METHOD_ANNOTATION_CACHE");
    }

    private void clearCache(String fieldName) throws Exception {
        Field f = ReflectionUtils.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        Map<?,?> map = (Map<?,?>) f.get(null);
        map.clear();
    }

    /* --------------------------------------------------------------------- *
     *  1. set*Cache – custom cache injection                                 *
     * --------------------------------------------------------------------- */

    @Test
    public void testSetMethodCacheCustomImplementation() throws Exception {
        Map<Object, Method> custom = new ConcurrentHashMap<>();
        ReflectionUtils.setMethodCache(custom);

        // Verify that the internal static field now points to our map
        Field cacheField = ReflectionUtils.class.getDeclaredField("METHOD_CACHE");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Object, Method> actual = (Map<Object, Method>) cacheField.get(null);
        assertSame("Custom cache should be used", custom, actual);
    }

    @Test
    public void testSetAllCachesCustomImplementation() throws Exception {
        Map<Object, Constructor<?>> ctorCache = new HashMap<>();
        Map<Object, Field> fieldCache = new HashMap<>();
        Map<Object, Collection<Field>> fieldsCache = new HashMap<>();
        Map<Object, Annotation> classAnnoCache = new HashMap<>();
        Map<Object, Annotation> methodAnnoCache = new HashMap<>();

        ReflectionUtils.setConstructorCache(ctorCache);
        ReflectionUtils.setFieldCache(fieldCache);
        ReflectionUtils.setClassFieldsCache(fieldsCache);
        ReflectionUtils.setClassAnnotationCache(classAnnoCache);
        ReflectionUtils.setMethodAnnotationCache(methodAnnoCache);

        assertSame(ctorCache,
                getStaticMap("CONSTRUCTOR_CACHE"));
        assertSame(fieldCache,
                getStaticMap("FIELD_NAME_CACHE"));
        assertSame(fieldsCache,
                getStaticMap("FIELDS_CACHE"));
        assertSame(classAnnoCache,
                getStaticMap("CLASS_ANNOTATION_CACHE"));
        assertSame(methodAnnoCache,
                getStaticMap("METHOD_ANNOTATION_CACHE"));
    }

    @SuppressWarnings("unchecked")
    private <K,V> Map<K,V> getStaticMap(String name) throws Exception {
        Field f = ReflectionUtils.class.getDeclaredField(name);
        f.setAccessible(true);
        return (Map<K,V>) f.get(null);
    }

    /* --------------------------------------------------------------------- *
     *  2. getClassAnnotation – normal, cached and error cases               *
     * --------------------------------------------------------------------- */

    @Test
    public void testGetClassAnnotationFoundAndCached() {
        // SubClass inherits @ClassAnno from SuperClass
        ClassAnno anno1 = ReflectionUtils.getClassAnnotation(SubClass.class, ClassAnno.class);
        assertNotNull("Annotation should be found on superclass", anno1);

        // Second call must hit the cache – we verify by checking identity
        ClassAnno anno2 = ReflectionUtils.getClassAnnotation(SubClass.class, ClassAnno.class);
        assertSame("Second lookup should return cached instance", anno1, anno2);
    }

    @Test
    public void testGetClassAnnotationMissingReturnsNullAndCachesNull() {
        // No annotation of this type on String
        Deprecated missing = ReflectionUtils.getClassAnnotation(String.class, Deprecated.class);
        assertNull("Missing annotation should return null", missing);

        // Second call should also be null (cached miss)
        Deprecated missing2 = ReflectionUtils.getClassAnnotation(String.class, Deprecated.class);
        assertNull("Cached miss should still be null", missing2);
    }

    @Test
    public void testGetClassAnnotationNullArguments() {
        // null class => null result (method guards against it)
        assertNull(ReflectionUtils.getClassAnnotation(null, ClassAnno.class));

        // null annotation class => IllegalArgumentException via Convention.throwIfNull
        try {
            ReflectionUtils.getClassAnnotation(String.class, null);
            fail("Expected IllegalArgumentException for null annoClass");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    /* --------------------------------------------------------------------- *
     *  3. getMethodAnnotation – inheritance & caching                        *
     * --------------------------------------------------------------------- */

    @Test
    public void testGetMethodAnnotationInheritedFromInterface() throws Exception {
        Method m = SubClass.class.getMethod("interfaceMethod");
        MethodAnno anno = ReflectionUtils.getMethodAnnotation(m, MethodAnno.class);
        assertNotNull("Annotation should be inherited from interface", anno);
    }

    @Test
    public void testGetMethodAnnotationOnSuperMethod() throws Exception {
        Method m = SubClass.class.getMethod("superMethod");
        MethodAnno anno = ReflectionUtils.getMethodAnnotation(m, MethodAnno.class);
        assertNotNull("Annotation should be found on superclass method", anno);
    }

    @Test
    public void testGetMethodAnnotationCacheBehaviour() throws Exception {
        Method m = SubClass.class.getMethod("superMethod");
        MethodAnno first = ReflectionUtils.getMethodAnnotation(m, MethodAnno.class);
        MethodAnno second = ReflectionUtils.getMethodAnnotation(m, MethodAnno.class);
        assertSame("Second lookup should be cached", first, second);
    }

    @Test
    public void testGetMethodAnnotationNullArguments() {
        try {
            ReflectionUtils.getMethodAnnotation(null, MethodAnno.class);
            fail("Expected IllegalArgumentException for null method");
        } catch (IllegalArgumentException ignored) {}

        try {
            Method m = SubClass.class.getMethods()[0];
            ReflectionUtils.getMethodAnnotation(m, null);
            fail("Expected IllegalArgumentException for null annoClass");
        } catch (IllegalArgumentException ignored) {}
    }

    /* --------------------------------------------------------------------- *
     *  4. getField – private field lookup and caching                         *
     * --------------------------------------------------------------------- */

    @Test
    public void testGetFieldFindsPrivateFieldAndCachesResult() {
        Field f1 = ReflectionUtils.getField(SubClass.class, "privateInt");
        assertNotNull("Should locate private field", f1);
        assertTrue("Field should be accessible", f1.isAccessible());

        // Second call should return the same Field instance from cache
        Field f2 = ReflectionUtils.getField(SubClass.class, "privateInt");
        assertSame("Cached field instance expected", f1, f2);
    }

    @Test
    public void testGetFieldMissingReturnsNull() {
        Field missing = ReflectionUtils.getField(SubClass.class, "nonexistent");
        assertNull("Missing field should return null", missing);
    }

    @Test
    public void testGetFieldNullArguments() {
        try {
            ReflectionUtils.getField(null, "any");
            fail("Expected IllegalArgumentException for null class");
        } catch (IllegalArgumentException ignored) {}

        try {
            ReflectionUtils.getField(SubClass.class, null);
            fail("Expected IllegalArgumentException for null fieldName");
        } catch (IllegalArgumentException ignored) {}
    }

    /* --------------------------------------------------------------------- *
     *  5. getDeclaredFields – default filter and custom predicate            *
     * --------------------------------------------------------------------- */

    private static class FieldFilterTest {
        public int a;
        private static String ignoredStatic;
        protected String b;
        String c; // package‑private
        @FieldAnno
        private int annotated;
    }

    @Test
    public void testGetDeclaredFieldsDefaultFilterExcludesStatic() {
        List<Field> fields = ReflectionUtils.getDeclaredFields(FieldFilterTest.class);
        // static field must be filtered out
        assertFalse("Static field should be filtered", fields.stream()
                .anyMatch(f -> "ignoredStatic".equals(f.getName())));
        // All other instance fields should be present
        Set<String> names = new HashSet<>();
        fields.forEach(f -> names.add(f.getName()));
        assertTrue(names.containsAll(Arrays.asList("a", "b", "c", "annotated")));
    }

    @Test
    public void testGetDeclaredFieldsCustomPredicateIncludesAll() {
        Predicate<Field> includeAll = f -> true;
        List<Field> all = ReflectionUtils.getDeclaredFields(FieldFilterTest.class, includeAll);
        // Should contain every declared field, static included
        Set<String> names = new HashSet<>();
        all.forEach(f -> names.add(f.getName()));
        assertTrue(names.containsAll(Arrays.asList("a", "b", "c", "annotated", "ignoredStatic")));
    }

    /* --------------------------------------------------------------------- *
     *  6. getAllDeclaredFields / getAllDeclaredFieldsMap – hierarchy & coll. *
     * --------------------------------------------------------------------- */

    private static class Parent {
        private int value = 1;
    }

    private static class Child extends Parent {
        private int value = 2; // hides parent field
        private String extra = "x";
    }

    @Test
    public void testGetAllDeclaredFieldsInheritanceOrder() {
        List<Field> list = ReflectionUtils.getAllDeclaredFields(Child.class);
        // Child fields first, then parent fields
        assertEquals("Child field count", 2, list.size() - 1); // extra + child value
        assertEquals("First field should be Child.extra", "extra", list.get(0).getName());
        assertEquals("Second field should be Child.value", "value", list.get(1).getName());
        // Parent field appears last
        assertEquals("Last field should be Parent.value", "value", list.get(list.size() - 1).getName());
    }

    @Test
    public void testGetAllDeclaredFieldsMapCollisionHandling() {
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(Child.class);
        // Simple name "value" should map to child's field
        assertTrue(map.containsKey("value"));
        assertEquals("Child's value field should win simple name",
                Child.class, map.get("value").getDeclaringClass());

        // Parent's hidden field must be stored with qualified name
        String qualified = Parent.class.getName() + ".value";
        assertTrue("Qualified name expected for hidden parent field", map.containsKey(qualified));
        assertEquals(Parent.class, map.get(qualified).getDeclaringClass());
    }

    @Test
    public void testGetAllDeclaredFieldsMapDefaultFilterExcludesStatic() {
        class StaticHolder {
            static int staticField = 10;
            int instanceField = 5;
        }
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(StaticHolder.class);
        assertFalse("Static field must be filtered out", map.containsKey("staticField"));
        assertTrue("Instance field must be present", map.containsKey("instanceField"));
    }

    /* --------------------------------------------------------------------- *
     *  7. call(Method) – normal execution and exception wrapping            *
     * --------------------------------------------------------------------- */

    private static class SimpleTarget {
        public int add(int a, int b) { return a + b; }
    }

    @Test
    public void testCallMethodSuccess() throws Exception {
        SimpleTarget target = new SimpleTarget();
        Method m = SimpleTarget.class.getMethod("add", int.class, int.class);
        Object result = ReflectionUtils.call(target, m, 2, 3);
        assertEquals("Result of add(2,3) should be 5", 5, result);
    }

    @Test
    public void testCallMethodWrapsCheckedException() throws Exception {
        ExceptionThrower et = new ExceptionThrower();
        Method m = ExceptionThrower.class.getMethod("explode");
        try {
            ReflectionUtils.call(et, m);
            fail("Expected RuntimeException wrapping IOException");
        } catch (RuntimeException e) {
            assertTrue(e.getCause() instanceof IOException);
            assertEquals("boom", e.getCause().getMessage());
        }
    }

    @Test
    public void testCallMethodNullChecks() throws Exception {
        Method m = SimpleTarget.class.getMethod("add", int.class, int.class);
        try {
            ReflectionUtils.call(null, m, 1, 2);
            fail("Expected IllegalArgumentException for null instance");
        } catch (IllegalArgumentException ignored) {}

        try {
            ReflectionUtils.call(new SimpleTarget(), null);
            fail("Expected IllegalArgumentException for null method");
        } catch (IllegalArgumentException ignored) {}
    }

    /* --------------------------------------------------------------------- *
     *  8. call(String) – lookup by name & argument count                    *
     * --------------------------------------------------------------------- */

    private static class OverloadedTarget {
        public String greet() { return "hello"; }
        public String greet(String name) { return "hi " + name; }
        private String greet(int i) { return "num " + i; }
    }

    @Test
    public void testCallByNameNoArgs() {
        OverloadedTarget ot = new OverloadedTarget();
        Object result = ReflectionUtils.call(ot, "greet");
        assertEquals("hello", result);
    }

    @Test
    public void testCallByNameWithArgs() {
        OverloadedTarget ot = new OverloadedTarget();
        Object result = ReflectionUtils.call(ot, "greet", "Bob");
        assertEquals("hi Bob", result);
    }

    @Test
    public void testCallByNameMethodNotFound() {
        OverloadedTarget ot = new OverloadedTarget();
        try {
            ReflectionUtils.call(ot, "nonexistent");
            fail("Expected IllegalArgumentException for missing method");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("nonexistent"));
        }
    }

    @Test
    public void testCallByNameNullChecks() {
        try {
            ReflectionUtils.call(null, "greet");
            fail("Expected IllegalArgumentException for null instance");
        } catch (IllegalArgumentException ignored) {}

        try {
            ReflectionUtils.call(new OverloadedTarget(), null);
            fail("Expected IllegalArgumentException for null method name");
        } catch (IllegalArgumentException ignored) {}
    }

    /* --------------------------------------------------------------------- *
     *  9. getMethod(Class, name, types) – private & inheritance lookup      *
     * --------------------------------------------------------------------- */

    private static class MethodLookupTarget {
        private String secret() { return "shhh"; }
        protected String protectedMethod() { return "ok"; }
    }

    @Test
    public void testGetMethodFindsPrivateAndMakesAccessible() {
        Method m = ReflectionUtils.getMethod(MethodLookupTarget.class, "secret");
        assertNotNull("Private method should be found", m);
        assertTrue("Method should be made accessible", m.isAccessible());
        // second call hits cache – same instance
        Method m2 = ReflectionUtils.getMethod(MethodLookupTarget.class, "secret");
        assertSame(m, m2);
    }

    @Test
    public void testGetMethodNullArguments() {
        try {
            ReflectionUtils.getMethod(null, "any");
            fail("Expected IllegalArgumentException for null class");
        } catch (IllegalArgumentException ignored) {}

        try {
            ReflectionUtils.getMethod(MethodLookupTarget.class, null);
            fail("Expected IllegalArgumentException for null method name");
        } catch (IllegalArgumentException ignored) {}
    }

    /* --------------------------------------------------------------------- *
     * 10. getMethod(Object, name, argCount) – selection strategy            *
     * --------------------------------------------------------------------- */

    @Test
    public void testGetMethodByArgCountSelectsMostAccessible() {
        OverloadedMethods om = new OverloadedMethods();
        Method m = ReflectionUtils.getMethod(om, "foo", 1);
        // There are two public methods with 1 argument – String and Integer (protected)
        // The public one should be selected
        assertEquals("public String foo(String)", m.getReturnType(), String.class);
        assertEquals("String", m.getParameterTypes()[0].getSimpleName());
        assertTrue("Method should be public", Modifier.isPublic(m.getModifiers()));
    }

    @Test
    public void testGetMethodByArgCountSelectsNonSyntheticFirst() throws Exception {
        // Create a generic class to force a bridge method
        class Generic<T> {
            public T identity(T value) { return value; }
        }
        Generic<String> gen = new Generic<>();
        Method m = ReflectionUtils.getMethod(gen, "identity", 1);
        assertFalse("Bridge method must be avoided", m.isBridge());
        assertEquals("Return type should be Object (erasure)".trim(),
                Object.class, m.getReturnType());
    }

    @Test
    public void testGetMethodByArgCountMissingThrows() {
        OverloadedTarget ot = new OverloadedTarget();
        try {
            ReflectionUtils.getMethod(ot, "nonexistent", 0);
            fail("Expected IllegalArgumentException for missing method");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("nonexistent"));
        }
    }

    @Test
    public void testGetMethodByArgCountNegativeArgCount() {
        OverloadedTarget ot = new OverloadedTarget();
        try {
            ReflectionUtils.getMethod(ot, "greet", -1);
            fail("Expected IllegalArgumentException for negative arg count");
        } catch (IllegalArgumentException ignored) {}
    }

    /* --------------------------------------------------------------------- *
     * 11. getConstructor & getAllConstructors – private constructors        *
     * --------------------------------------------------------------------- */

    @Test
    public void testGetConstructorPrivate() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(PrivateConstructor.class);
        assertNotNull("Private constructor should be located", ctor);
        assertTrue("Constructor should be accessible", ctor.isAccessible());
    }

    @Test
    public void testGetConstructorCacheBehaviour() {
        Constructor<?> c1 = ReflectionUtils.getConstructor(PrivateConstructor.class);
        Constructor<?> c2 = ReflectionUtils.getConstructor(PrivateConstructor.class);
        assertSame("Second lookup must hit cache", c1, c2);
    }

    @Test
    public void testGetAllConstructorsCachesEachConstructor() {
        Constructor<?>[] all = ReflectionUtils.getAllConstructors(PrivateConstructor.class);
        assertEquals(1, all.length);
        Constructor<?> cached = ReflectionUtils.getConstructor(PrivateConstructor.class);
        assertSame("Array element should be the same cached instance", cached, all[0]);
    }

    @Test
    public void testGetConstructorNullArgument() {
        try {
            ReflectionUtils.getConstructor(null);
            fail("Expected IllegalArgumentException for null class");
        } catch (IllegalArgumentException ignored) {}
    }

    /* --------------------------------------------------------------------- *
     * 12. makeParamKey – private utility (tested via reflection)             *
     * --------------------------------------------------------------------- */

    @Test
    public void testMakeParamKeyProducesExpectedString() throws Exception {
        Method m = ReflectionUtils.class.getDeclaredMethod("makeParamKey", Class[].class);
        m.setAccessible(true);
        String result = (String) m.invoke(null, (Object) new Class[]{String.class, int.class, List.class});
        // Expected format ":String|int|List"
        assertEquals(":String|int|List", result);
    }

    @Test
    public void testMakeParamKeyEmptyArray() throws Exception {
        Method m = ReflectionUtils.class.getDeclaredMethod("makeParamKey", Class[].class);
        m.setAccessible(true);
        String result = (String) m.invoke(null, (Object) new Class[0]);
        assertEquals("", result);
    }

    /* --------------------------------------------------------------------- *
     * 13. getClassName – null handling                                         *
     * --------------------------------------------------------------------- */

    @Test
    public void testGetClassNameHandlesNull() {
        assertEquals("null", ReflectionUtils.getClassName(null));
        assertEquals(String.class.getName(),
                ReflectionUtils.getClassName("hello"));
    }

    /* --------------------------------------------------------------------- *
     * 14. getClassNameFromByteCode – compile‑in‑memory test class               *
     * --------------------------------------------------------------------- */

    @Test
    public void testGetClassNameFromByteCode() throws Exception {
        // Compile a tiny class in memory using the JavaCompiler API
        String source = "package testpkg; public class HelloWorld { }";
        byte[] bytecode = compileInMemory("testpkg.HelloWorld", source);
        String className = ReflectionUtils.getClassNameFromByteCode(bytecode);
        assertEquals("testpkg.HelloWorld", className);
    }

    /**
     * Compiles a single class given its fully qualified name and source code,
     * returning the resulting byte[] of the .class file.
     */
    private byte[] compileInMemory(String fqcn, String source) throws Exception {
        // JavaCompiler API (available in JDK)
        javax.tools.JavaCompiler compiler = javax.tools.ToolProvider.getSystemJavaCompiler();
        assertNotNull("JavaCompiler not available – test requires JDK", compiler);

        // In‑memory file manager
        SimpleJavaFileObject src = new SimpleJavaFileObject(
                java.net.URI.create("string:///" + fqcn.replace('.', '/') + ".java"),
                javax.tools.JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return source;
            }
        };

        java.util.List<javax.tools.JavaFileObject> sources = Collections.singletonList(src);
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        javax.tools.JavaFileManager fm = new javax.tools.ForwardingJavaFileManager<>(
                compiler.getStandardFileManager(null, null, null)) {
            @Override
            public javax.tools.JavaFileObject getJavaFileForOutput(Location location,
                                                                   String className,
                                                                   javax.tools.JavaFileObject.Kind kind,
                                                                   FileObject sibling) {
                return new SimpleJavaFileObject(
                        java.net.URI.create("mem:///" + className.replace('.', '/') + kind.extension),
                        kind) {
                    @Override
                    public java.io.OutputStream openOutputStream() {
                        return baos;
                    }
                };
            }
        };

        javax.tools.JavaCompiler.CompilationTask task = compiler.getTask(
                null, fm, null, null, null, sources);
        Boolean ok = task.call();
        assertTrue("Compilation failed", ok);
        return baos.toByteArray();
    }

    /* --------------------------------------------------------------------- *
     * 15. getNonOverloadedMethod – correct handling of overloads             *
     * --------------------------------------------------------------------- */

    private static class NoOverload {
        public String single() { return "ok"; }
    }

    private static class Overloaded {
        public String dup() { return "a"; }
        public String dup(int i) { return "b"; }
    }

    @Test
    public void testGetNonOverloadedMethodSuccess() {
        Method m = ReflectionUtils.getNonOverloadedMethod(NoOverload.class, "single");
        assertNotNull(m);
        assertEquals("single", m.getName());
    }

    @Test
    public void testGetNonOverloadedMethodThrowsOnOverload() {
        try {
            ReflectionUtils.getNonOverloadedMethod(Overloaded.class, "dup");
            fail("Expected IllegalArgumentException due to overloads");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("overloaded"));
        }
    }

    @Test
    public void testGetNonOverloadedMethodNullChecks() {
        try {
            ReflectionUtils.getNonOverloadedMethod(null, "any");
            fail("Expected IllegalArgumentException for null class");
        } catch (IllegalArgumentException ignored) {}

        try {
            ReflectionUtils.getNonOverloadedMethod(NoOverload.class, null);
            fail("Expected IllegalArgumentException for null method name");
        } catch (IllegalArgumentException ignored) {}
    }

    /* --------------------------------------------------------------------- *
     * 16. Deprecated methods – ensure they still function via delegation   *
     * --------------------------------------------------------------------- */

    @Test
    public void testDeprecatedGetDeepDeclaredFieldsBehavesLikeAllDeclaredFields() {
        List<Field> deep = new ArrayList<>(ReflectionUtils.getDeepDeclaredFields(Child.class));
        List<Field> all = ReflectionUtils.getAllDeclaredFields(Child.class);
        // Both should contain the same fields (order may differ, we compare as sets)
        assertEquals(new HashSet<>(all), new HashSet<>(deep));
    }

    @Test
    public void testDeprecatedGetDeepDeclaredFieldMapBehavesLikeAllDeclaredFieldsMap() {
        Map<String, Field> deepMap = ReflectionUtils.getDeepDeclaredFieldMap(Child.class);
        Map<String, Field> allMap = ReflectionUtils.getAllDeclaredFieldsMap(Child.class);
        assertEquals(allMap, deepMap);
    }

    @Test
    public void testDeprecatedGetDeclaredFieldsAddsToCollection() {
        List<Field> list = new ArrayList<>();
        ReflectionUtils.getDeclaredFields(Child.class, list);
        // The list should now contain the same fields as getAllDeclaredFields
        assertEquals(new HashSet<>(ReflectionUtils.getAllDeclaredFields(Child.class)),
                new HashSet<>(list));
    }
}
