package com.cedarsoftware.util;

import org.junit.*;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class ReflectionUtilsTest {

    // ----------------------------------------------------------------------
    // Helper annotations and classes used throughout the tests
    // ----------------------------------------------------------------------
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
    private @interface TestAnno {
        String value() default "";
    }

    @TestAnno("base")
    private static class BaseClass {
        @TestAnno("field")
        private String baseField;

        @TestAnno("method")
        public void annotatedMethod() {}

        private void privateMethod() {}

        public void overloadedMethod(String s) {}

        protected void overloadedMethod(Object o) {}

        private void overloadedMethod(Integer i) {}
    }

    private static class SubClass extends BaseClass {
        private String subField;
    }

    private static class FieldCollisionParent {
        private int dup;
        private int uniqueParent;
    }

    private static class FieldCollisionChild extends FieldCollisionParent {
        private int dup;          // hides parent field
        private int uniqueChild;
    }

    private static class ConstructorTest {
        public ConstructorTest() {}
        protected ConstructorTest(int a) {}
        ConstructorTest(String s) {}               // package‑private
        private ConstructorTest(double d) {}
    }

    private static class OverloadedMethods {
        private void foo(int i) {}
        public void foo(String s) {}
        protected void foo(Object o) {}
    }

    // ----------------------------------------------------------------------
    // Reset caches before each test to avoid cross‑test contamination
    // ----------------------------------------------------------------------
    @Before
    public void resetCaches() {
        ReflectionUtils.setMethodCache(new HashMap<>());
        ReflectionUtils.setClassFieldsCache(new HashMap<>());
        ReflectionUtils.setFieldCache(new HashMap<>());
        ReflectionUtils.setClassAnnotationCache(new HashMap<>());
        ReflectionUtils.setMethodAnnotationCache(new HashMap<>());
        ReflectionUtils.setConstructorCache(new HashMap<>());
        ReflectionUtils.setSortedConstructorsCache(new HashMap<>());
    }

    // ----------------------------------------------------------------------
    // Tests for cache‑setting methods
    // ----------------------------------------------------------------------
    @Test
    public void testSetMethodCacheReplacesCache() throws Exception {
        Map<Object, Method> custom = Collections.synchronizedMap(new HashMap<>());
        ReflectionUtils.setMethodCache(custom);

        // Grab the internal AtomicReference via reflection
        Field cacheField = ReflectionUtils.getField(ReflectionUtils.class, "METHOD_CACHE");
        @SuppressWarnings("unchecked")
        AtomicReference<Map<Object, Method>> ref = (AtomicReference<Map<Object, Method>>) cacheField.get(null);
        assertSame(custom, ref.get());
    }

    @Test
    public void testSetFieldCacheReplacesCache() throws Exception {
        Map<Object, Field> custom = new HashMap<>();
        ReflectionUtils.setFieldCache(custom);
        Field cacheField = ReflectionUtils.getField(ReflectionUtils.class, "FIELD_NAME_CACHE");
        @SuppressWarnings("unchecked")
        AtomicReference<Map<Object, Field>> ref = (AtomicReference<Map<Object, Field>>) cacheField.get(null);
        assertSame(custom, ref.get());
    }

    // ----------------------------------------------------------------------
    // Class annotation lookup
    // ----------------------------------------------------------------------
    @Test
    public void testGetClassAnnotationFindsAnnotationOnSuperclass() {
        TestAnno anno = ReflectionUtils.getClassAnnotation(SubClass.class, TestAnno.class);
        assertNotNull("Annotation should be found on superclass", anno);
        assertEquals("base", anno.value());
    }

    @Test
    public void testGetClassAnnotationReturnsNullWhenMissing() {
        TestAnno anno = ReflectionUtils.getClassAnnotation(String.class, TestAnno.class);
        assertNull(anno);
    }

    @Test
    public void testGetClassAnnotationNullClassReturnsNull() {
        TestAnno anno = ReflectionUtils.getClassAnnotation(null, TestAnno.class);
        assertNull(anno);
    }

    // ----------------------------------------------------------------------
    // Method annotation lookup
    // ----------------------------------------------------------------------
    @Test
    public void testGetMethodAnnotationFindsInheritedAnnotation() throws Exception {
        Method subMethod = SubClass.class.getMethod("annotatedMethod");
        TestAnno anno = ReflectionUtils.getMethodAnnotation(subMethod, TestAnno.class);
        assertNotNull(anno);
        assertEquals("method", anno.value());
    }

    @Test
    public void testGetMethodAnnotationReturnsNullWhenMissing() throws Exception {
        Method m = BaseClass.class.getMethod("privateMethod");
        TestAnno anno = ReflectionUtils.getMethodAnnotation(m, TestAnno.class);
        assertNull(anno);
    }

    // ----------------------------------------------------------------------
    // Field lookup
    // ----------------------------------------------------------------------
    @Test
    public void testGetFieldFindsSuperclassField() {
        Field f = ReflectionUtils.getField(SubClass.class, "baseField");
        assertNotNull(f);
        assertEquals("baseField", f.getName());
        assertEquals(BaseClass.class, f.getDeclaringClass());
    }

    @Test
    public void testGetFieldReturnsNullWhenNotFound() {
        Field f = ReflectionUtils.getField(SubClass.class, "nonExistent");
        assertNull(f);
    }

    // ----------------------------------------------------------------------
    // Declared fields with custom predicate
    // ----------------------------------------------------------------------
    @Test
    public void testGetDeclaredFieldsCustomFilter() {
        Predicate<Field> onlyStrings = f -> f.getType() == String.class;
        List<Field> fields = ReflectionUtils.getDeclaredFields(SubClass.class, onlyStrings);
        assertEquals(1, fields.size());
        assertEquals("subField", fields.get(0).getName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetDeclaredFieldsNullClass() {
        ReflectionUtils.getDeclaredFields(null, f -> true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetDeclaredFieldsNullPredicate() {
        ReflectionUtils.getDeclaredFields(SubClass.class, null);
    }

    // ----------------------------------------------------------------------
    // All declared fields (hierarchy) and map collision handling
    // ----------------------------------------------------------------------
    @Test
    public void testGetAllDeclaredFieldsIncludesSuperclass() {
        List<Field> all = ReflectionUtils.getAllDeclaredFields(FieldCollisionChild.class);
        Set<String> names = new HashSet<>();
        for (Field f : all) names.add(f.getName());
        // Both dup fields and the unique ones must be present
        assertTrue(names.contains("dup"));
        assertTrue(names.contains("uniqueParent"));
        assertTrue(names.contains("uniqueChild"));
    }

    @Test
    public void testGetAllDeclaredFieldsMapHandlesNameCollisions() {
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(FieldCollisionChild.class);
        // Simple name should resolve to child's field
        assertTrue(map.containsKey("dup"));
        assertEquals(FieldCollisionChild.class, map.get("dup").getDeclaringClass());

        // Parent's dup must be qualified
        String qualified = FieldCollisionParent.class.getName() + ".dup";
        assertTrue(map.containsKey(qualified));
        assertEquals(FieldCollisionParent.class, map.get(qualified).getDeclaringClass());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetAllDeclaredFieldsNullClassThrows() {
        ReflectionUtils.getAllDeclaredFields(null);
    }

    // ----------------------------------------------------------------------
    // Deprecated deep‑declared methods (still exercised)
    // ----------------------------------------------------------------------
    @Test
    public void testGetDeepDeclaredFieldsFiltersTransientAndSynthetic() {
        Collection<Field> fields = ReflectionUtils.getDeepDeclaredFields(FieldCollisionChild.class);
        // The method internally uses DEFAULT_FIELD_FILTER plus extra checks,
        // but for our simple class there should be exactly the three logical fields.
        assertEquals(3, fields.size());
    }

    @Test
    public void testGetDeepDeclaredFieldMapFiltersTransientAndSynthetic() {
        Map<String, Field> map = ReflectionUtils.getDeepDeclaredFieldMap(FieldCollisionChild.class);
        assertEquals(3, map.size());
        assertTrue(map.containsKey("dup"));
        assertTrue(map.containsKey("uniqueParent"));
        assertTrue(map.containsKey("uniqueChild"));
    }

    // ----------------------------------------------------------------------
    // Reflection call helpers
    // ----------------------------------------------------------------------
    @Test
    public void testCallMethodObjectMethod() throws Exception {
        BaseClass obj = new BaseClass();
        Method m = ReflectionUtils.getMethod(BaseClass.class, "privateMethod");
        Object result = ReflectionUtils.call(obj, m);
        assertNull(result); // void method returns null
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallWithNullMethodThrows() {
        ReflectionUtils.call(new BaseClass(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallWithNullInstanceThrows() throws Exception {
        Method m = BaseClass.class.getDeclaredMethod("privateMethod");
        ReflectionUtils.call(null, m);
    }

    @Test
    public void testCallByNameSelectsPublicMethod() {
        OverloadedMethods obj = new OverloadedMethods();
        Object result = ReflectionUtils.call(obj, "foo", "test");
        assertNull(result); // void method
        // Verify that the method selected is the public one
        Method selected = ReflectionUtils.getMethod(obj, "foo", 1);
        assertTrue(Modifier.isPublic(selected.getModifiers()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallByNameMethodNotFound() {
        ReflectionUtils.call(new BaseClass(), "nonExistingMethod");
    }

    // ----------------------------------------------------------------------
    // Method lookup (specific signatures)
    // ----------------------------------------------------------------------
    @Test
    public void testGetMethodFindsPrivateMethod() throws Exception {
        Method m = ReflectionUtils.getMethod(BaseClass.class, "privateMethod");
        assertNotNull(m);
        assertTrue(Modifier.isPrivate(m.getModifiers()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodNullClassThrows() {
        ReflectionUtils.getMethod(null, "any");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodNullNameThrows() {
        ReflectionUtils.getMethod(BaseClass.class, null);
    }

    @Test
    public void testGetMethodByArgCountSelectsMostAccessible() {
        OverloadedMethods obj = new OverloadedMethods();
        Method m = ReflectionUtils.getMethod(obj, "foo", 1);
        assertTrue("Selected method should be public", Modifier.isPublic(m.getModifiers()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodByArgCountNegativeArgCount() {
        ReflectionUtils.getMethod(new BaseClass(), "privateMethod", -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodByArgCountNotFound() {
        ReflectionUtils.getMethod(new BaseClass(), "nonExisting", 0);
    }

    // ----------------------------------------------------------------------
    // Constructor lookup and ordering
    // ----------------------------------------------------------------------
    @Test
    public void testGetConstructorFindsPrivateConstructor() {
        Constructor<?> c = ReflectionUtils.getConstructor(ConstructorTest.class, double.class);
        assertNotNull(c);
        assertTrue(Modifier.isPrivate(c.getModifiers()));
    }

    @Test
    public void testGetConstructorReturnsNullWhenMissing() {
        Constructor<?> c = ReflectionUtils.getConstructor(ConstructorTest.class, long.class);
        assertNull(c);
    }

    @Test
    public void testGetAllConstructorsOrdering() {
        Constructor<?>[] ctors = ReflectionUtils.getAllConstructors(ConstructorTest.class);
        assertEquals(4, ctors.length);
        // 1st should be public
        assertTrue(Modifier.isPublic(ctors[0].getModifiers()));
        // 2nd should be protected
        assertTrue(Modifier.isProtected(ctors[1].getModifiers()));
        // 3rd should be package‑private (no modifier)
        assertFalse(Modifier.isPublic(ctors[2].getModifiers()));
        assertFalse(Modifier.isProtected(ctors[2].getModifiers()));
        assertFalse(Modifier.isPrivate(ctors[2].getModifiers()));
        // 4th should be private
        assertTrue(Modifier.isPrivate(ctors[3].getModifiers()));
    }

    @Test
    public void testGetAllConstructorsNullClassReturnsEmptyArray() {
        Constructor<?>[] ctors = ReflectionUtils.getAllConstructors(null);
        assertNotNull(ctors);
        assertEquals(0, ctors.length);
    }

    // ----------------------------------------------------------------------
    // Utility methods
    // ----------------------------------------------------------------------
    @Test
    public void testGetClassNameHandlesNull() {
        assertEquals("null", ReflectionUtils.getClassName(null));
        assertEquals(String.class.getName(), ReflectionUtils.getClassName("test"));
    }

    // ----------------------------------------------------------------------
    // Byte‑code class name extraction (requires JavaCompiler)
    // ----------------------------------------------------------------------
    @Test
    public void testGetClassNameFromByteCode() throws Exception {
        Assume.assumeTrue("Java compiler not available", ReflectionUtils.isJavaCompilerAvailable());

        String className = "com.example.Generated";
        String source = "package com.example; public class Generated {}";

        // Compile source to a temporary .class file
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        File sourceFile = new File(tempDir, "Generated.java");
        try (Writer writer = new FileWriter(sourceFile)) {
            writer.write(source);
        }

        // Invoke the compiler
        javax.tools.JavaCompiler compiler = javax.tools.ToolProvider.getSystemJavaCompiler();
        int compileResult = compiler.run(null, null, null, sourceFile.getPath());
        assertEquals("Compilation failed", 0, compileResult);

        // Load the generated .class file bytes
        File classFile = new File(tempDir, "Generated.class");
        byte[] byteCode = java.nio.file.Files.readAllBytes(classFile.toPath());

        String extracted = ReflectionUtils.getClassNameFromByteCode(byteCode);
        assertEquals(className, extracted);
    }

    @Test
    public void testGetClassNameFromByteCodeInvalidDataThrows() {
        byte[] bogus = new byte[]{0, 1, 2, 3};
        try {
            ReflectionUtils.getClassNameFromByteCode(bogus);
            fail("Expected IOException");
        } catch (IOException e) {
            // expected
        }
    }

    // ----------------------------------------------------------------------
    // isJavaCompilerAvailable – forced JRE simulation
    // ----------------------------------------------------------------------
    @Test
    public void testIsJavaCompilerAvailableRespectsSystemProperty() {
        String original = System.getProperty("java.util.force.jre");
        try {
            System.setProperty("java.util.force.jre", "true");
            assertFalse(ReflectionUtils.isJavaCompilerAvailable());

            System.setProperty("java.util.force.jre", "false");
            // The real result depends on the environment, but it must be *some* boolean
            // (true on a JDK, false on a JRE).  We just ensure the call does not throw.
            ReflectionUtils.isJavaCompilerAvailable();
        } finally {
            if (original != null) {
                System.setProperty("java.util.force.jre", original);
            } else {
                System.clearProperty("java.util.force.jre");
            }
        }
    }

    // ----------------------------------------------------------------------
    // makeParamKey – internal helper (indirectly via cache keys)
    // ----------------------------------------------------------------------
    @Test
    public void testMakeParamKeyProducesExpectedString() throws Exception {
        // Access the private static method via reflection
        Method makeParamKey = ReflectionUtils.class.getDeclaredMethod("makeParamKey", Class[].class);
        makeParamKey.setAccessible(true);
        String empty = (String) makeParamKey.invoke(null, (Object) new Class<?>[0]);
        assertEquals("", empty);

        String one = (String) makeParamKey.invoke(null, (Object) new Class<?>[]{String.class});
        assertEquals(":String", one);

        String two = (String) makeParamKey.invoke(null, (Object) new Class<?>[]{int.class, double.class});
        assertEquals(":int|double", two);
    }

    // ----------------------------------------------------------------------
    // getClassLoaderName – indirect verification via cache key equality
    // ----------------------------------------------------------------------
    @Test
    public void testClassLoaderNameConsistentAcrossCacheKeys() throws Exception {
        // Create two cache keys for the same class and ensure they are equal
        Constructor<?>[] ctorArray = new Constructor<?>[0];
        // Access the private cache key classes
        Class<?> sortedKeyClass = Class.forName("com.cedarsoftware.util.ReflectionUtils$SortedConstructorsCacheKey");
        Constructor<?> sortedCtor = sortedKeyClass.getDeclaredConstructor(Class.class);
        sortedCtor.setAccessible(true);
        Object key1 = sortedCtor.newInstance(String.class);
        Object key2 = sortedCtor.newInstance(String.class);
        assertEquals(key1, key2);
        assertEquals(key1.hashCode(), key2.hashCode());
    }
}
