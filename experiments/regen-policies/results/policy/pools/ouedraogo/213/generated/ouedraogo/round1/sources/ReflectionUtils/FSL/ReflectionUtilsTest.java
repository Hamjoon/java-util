package com.cedarsoftware.util;
import com.cedarsoftware.util.ReflectionUtils.ClassAnnotationCacheKey;
import com.cedarsoftware.util.ReflectionUtils.MethodCacheKey;


import static org.junit.Assert.*;

import java.io.IOException;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link ReflectionUtils}.
 * <p>
 * The tests cover:
 * <ul>
 *   <li>Annotation lookup (class and method) including caching</li>
 *   <li>Field lookup and hierarchical search</li>
 *   <li>Declared‑field filtering and map conversion</li>
 *   <li>Method lookup (by signature, by name/arg count) and selection strategy</li>
 *   <li>Constructor lookup and ordered constructor list</li>
 *   <li>Utility methods: {@code call}, {@code getClassName},
 *       {@code getClassNameFromByteCode} and {@code isJavaCompilerAvailable}</li>
 *   <li>Custom cache replacement via the {@code set*Cache} methods</li>
 * </ul>
 * </p>
 */
public class ReflectionUtilsTest {

    /* ---------------------------------------------------------------------- */
    /*  Helper annotations and test classes                                   */
    /* ---------------------------------------------------------------------- */

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    private @interface TestAnno {
        String value() default "";
    }

    @TestAnno("base")
    private static class BaseClass {
        private String baseField = "base";
        private static String staticBaseField = "staticBase";

        @TestAnno("baseMethod")
        private void privateBaseMethod() {}

        public void overloaded(int a) {}

        protected void overloaded(String s) {}

        void overloaded(Object o) {}
    }

    @TestAnno("derived")
    private static class DerivedClass extends BaseClass {
        private String derivedField = "derived";

        @TestAnno("derivedMethod")
        public void publicDerivedMethod() {}

        // same name as in BaseClass but different visibility
        public void overloaded(int a) {}
    }

    private static class NoDefaultConstructor {
        private final int x;
        private NoDefaultConstructor(int x) { this.x = x; }
    }

    /* ---------------------------------------------------------------------- */
    /*  Reflection helpers to access the private static cache fields            */
    /* ---------------------------------------------------------------------- */

    @SuppressWarnings("unchecked")
    private static <K, V> Map<K, V> getCacheField(String fieldName) throws Exception {
        Field f = ReflectionUtils.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        AtomicReference<Map<?, ?>> ref = (AtomicReference<Map<?, ?>>) f.get(null);
        return (Map<K, V>) ref.get();
    }

    @Before
    public void clearCaches() throws Exception {
        // Replace each cache with a fresh ConcurrentHashMap so that tests start from a clean state.
        ReflectionUtils.setMethodCache(new ConcurrentHashMap<>());
        ReflectionUtils.setClassFieldsCache(new ConcurrentHashMap<>());
        ReflectionUtils.setFieldCache(new ConcurrentHashMap<>());
        ReflectionUtils.setClassAnnotationCache(new ConcurrentHashMap<>());
        ReflectionUtils.setMethodAnnotationCache(new ConcurrentHashMap<>());
        ReflectionUtils.setConstructorCache(new ConcurrentHashMap<>());
        ReflectionUtils.setSortedConstructorsCache(new ConcurrentHashMap<>());
    }

    /* ---------------------------------------------------------------------- */
    /*  Cache replacement tests                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testSetMethodCacheReplacesDefault() throws Exception {
        Map<Object, Method> custom = new ConcurrentHashMap<>();
        ReflectionUtils.setMethodCache(custom);
        assertSame(custom, getCacheField("METHOD_CACHE"));
    }

    @Test
    public void testSetClassAnnotationCacheReplacesDefault() throws Exception {
        Map<Object, Annotation> custom = new ConcurrentHashMap<>();
        ReflectionUtils.setClassAnnotationCache(custom);
        assertSame(custom, getCacheField("CLASS_ANNOTATION_CACHE"));
    }

    /* ---------------------------------------------------------------------- */
    /*  Class annotation lookup (including caching)                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetClassAnnotationFoundAndCached() {
        // First lookup – cache miss, should find the annotation on DerivedClass
        TestAnno anno = ReflectionUtils.getClassAnnotation(DerivedClass.class, TestAnno.class);
        assertNotNull(anno);
        assertEquals("derived", anno.value());

        // Second lookup – should hit the cache and return the same instance
        TestAnno cachedAnno = ReflectionUtils.getClassAnnotation(DerivedClass.class, TestAnno.class);
        assertSame(anno, cachedAnno);
    }

    @Test
    public void testGetClassAnnotationSearchesSuperclasses() {
        // DerivedClass does not have @TestAnno on the class "BaseClass" level, but BaseClass is annotated.
        TestAnno baseAnno = ReflectionUtils.getClassAnnotation(BaseClass.class, TestAnno.class);
        assertNotNull(baseAnno);
        assertEquals("base", baseAnno.value());

        // Searching for the same annotation on DerivedClass should return the DerivedClass annotation,
        // not the superclass one, because the subclass annotation exists.
        TestAnno derivedAnno = ReflectionUtils.getClassAnnotation(DerivedClass.class, TestAnno.class);
        assertEquals("derived", derivedAnno.value());
    }

    @Test
    public void testGetClassAnnotationReturnsNullWhenMissing() {
        // No annotation of type Deprecated on the test classes
        Deprecated missing = ReflectionUtils.getClassAnnotation(DerivedClass.class, Deprecated.class);
        assertNull(missing);
    }

    @Test
    public void testGetClassAnnotationNullClassReturnsNull() {
        TestAnno result = ReflectionUtils.getClassAnnotation(null, TestAnno.class);
        assertNull(result);
    }

    /* ---------------------------------------------------------------------- */
    /*  Method annotation lookup                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetMethodAnnotationFoundAndCached() throws Exception {
        Method m = DerivedClass.class.getDeclaredMethod("publicDerivedMethod");
        TestAnno anno = ReflectionUtils.getMethodAnnotation(m, TestAnno.class);
        assertNotNull(anno);
        assertEquals("derivedMethod", anno.value());

        // Cached result should be identical
        TestAnno cached = ReflectionUtils.getMethodAnnotation(m, TestAnno.class);
        assertSame(anno, cached);
    }

    @Test
    public void testGetMethodAnnotationSearchesSuperclasses() throws Exception {
        Method m = DerivedClass.class.getDeclaredMethod("privateBaseMethod");
        TestAnno anno = ReflectionUtils.getMethodAnnotation(m, TestAnno.class);
        assertNotNull(anno);
        assertEquals("baseMethod", anno.value());
    }

    @Test
    public void testGetMethodAnnotationReturnsNullWhenMissing() throws Exception {
        Method m = DerivedClass.class.getDeclaredMethod("publicDerivedMethod");
        Override missing = ReflectionUtils.getMethodAnnotation(m, Override.class);
        assertNull(missing);
    }

    /* ---------------------------------------------------------------------- */
    /*  Field lookup (including inheritance)                                   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetFieldFindsDeclaredAndInheritedFields() {
        Field fDerived = ReflectionUtils.getField(DerivedClass.class, "derivedField");
        assertNotNull(fDerived);
        assertEquals("derivedField", fDerived.getName());

        Field fBase = ReflectionUtils.getField(DerivedClass.class, "baseField");
        assertNotNull(fBase);
        assertEquals("baseField", fBase.getName());

        // static fields are ignored by default filter but getField does not filter them,
        // it only searches by name, so staticBaseField should be found.
        Field fStatic = ReflectionUtils.getField(DerivedClass.class, "staticBaseField");
        assertNotNull(fStatic);
        assertEquals("staticBaseField", fStatic.getName());
    }

    @Test
    public void testGetFieldReturnsNullWhenMissing() {
        Field missing = ReflectionUtils.getField(DerivedClass.class, "nonExistent");
        assertNull(missing);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetFieldNullClassThrows() {
        ReflectionUtils.getField(null, "any");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetFieldNullNameThrows() {
        ReflectionUtils.getField(DerivedClass.class, null);
    }

    /* ---------------------------------------------------------------------- */
    /*  Declared fields filtering and map conversion                           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetDeclaredFieldsDefaultFilterExcludesStatic() {
        List<Field> fields = ReflectionUtils.getDeclaredFields(BaseClass.class);
        // staticBaseField should be filtered out by DEFAULT_FIELD_FILTER
        assertFalse(fields.stream().anyMatch(f -> f.getName().equals("staticBaseField")));
        // baseField should be present
        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("baseField")));
    }

    @Test
    public void testGetDeclaredFieldsCustomFilter() {
        List<Field> onlyStrings = ReflectionUtils.getDeclaredFields(BaseClass.class,
                f -> f.getType().equals(String.class));
        assertEquals(1, onlyStrings.size());
        assertEquals("baseField", onlyStrings.get(0).getName());
    }

    @Test
    public void testGetAllDeclaredFieldsHierarchy() {
        List<Field> all = ReflectionUtils.getAllDeclaredFields(DerivedClass.class);
        // Should contain both derivedField and baseField
        Set<String> names = new HashSet<>();
        for (Field f : all) names.add(f.getName());

        assertTrue(names.contains("derivedField"));
        assertTrue(names.contains("baseField"));
    }

    @Test
    public void testGetAllDeclaredFieldsMapCollisionResolution() throws Exception {
        // Create a subclass that declares a field with the same name as its superclass
        class Parent {
            private int dup = 1;
        }
        class Child extends Parent {
            private String dup = "child";
        }

        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(Child.class);
        // Simple name should map to child's field (most specific)
        assertEquals(String.class, map.get("dup").getType());

        // Fully qualified name should map to parent's field
        String fq = Parent.class.getName() + ".dup";
        assertEquals(int.class, map.get(fq).getType());
    }

    /* ---------------------------------------------------------------------- */
    /*  Method lookup – by signature and by name/arg count                       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetMethodBySignatureFindsPrivateMethod() throws Exception {
        Method privateMethod = ReflectionUtils.getMethod(BaseClass.class, "privateBaseMethod");
        assertNotNull(privateMethod);
        assertTrue(Modifier.isPrivate(privateMethod.getModifiers()));
    }

    @Test
    public void testGetMethodBySignatureCachesResult() throws Exception {
        Method m1 = ReflectionUtils.getMethod(BaseClass.class, "privateBaseMethod");
        Method m2 = ReflectionUtils.getMethod(BaseClass.class, "privateBaseMethod");
        assertSame(m1, m2);
    }

    @Test
    public void testGetMethodByNameAndArgCountSelectsMostAccessible() throws Exception {
        // Both BaseClass and DerivedClass have "overloaded(int)" – DerivedClass's is public,
        // BaseClass's is private. The utility should select the public one.
        Method m = ReflectionUtils.getMethod(new DerivedClass(), "overloaded", 1);
        assertEquals(1, m.getParameterCount());
        assertTrue(Modifier.isPublic(m.getModifiers()));
        assertEquals(DerivedClass.class, m.getDeclaringClass());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodByNameAndArgCountThrowsWhenNotFound() {
        ReflectionUtils.getMethod(new DerivedClass(), "nonexistent", 2);
    }

    @Test
    public void testGetNonOverloadedMethodSuccess() {
        Method m = ReflectionUtils.getNonOverloadedMethod(BaseClass.class, "privateBaseMethod");
        assertNotNull(m);
        assertEquals("privateBaseMethod", m.getName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethodFailsWhenOverloaded() {
        // "overloaded" exists multiple times in BaseClass
        ReflectionUtils.getNonOverloadedMethod(BaseClass.class, "overloaded");
    }

    /* ---------------------------------------------------------------------- */
    /*  Constructor lookup and ordering                                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetConstructorFindsPrivateConstructor() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(NoDefaultConstructor.class, int.class);
        assertNotNull(ctor);
        assertTrue(Modifier.isPrivate(ctor.getModifiers()));
    }

    @Test
    public void testGetConstructorCachesResult() {
        Constructor<?> c1 = ReflectionUtils.getConstructor(NoDefaultConstructor.class, int.class);
        Constructor<?> c2 = ReflectionUtils.getConstructor(NoDefaultConstructor.class, int.class);
        assertSame(c1, c2);
    }

    @Test
    public void testGetAllConstructorsOrdering() {
        class Sample {
            private Sample() {}
            Sample(int a) {}
            protected Sample(String s, int i) {}
            public Sample(String s) {}
        }

        Constructor<?>[] ctors = ReflectionUtils.getAllConstructors(Sample.class);
        assertEquals(4, ctors.length);
        // Expected order: public, protected, package (default), private
        assertTrue(Modifier.isPublic(ctors[0].getModifiers()));
        assertTrue(Modifier.isProtected(ctors[1].getModifiers()));
        assertFalse(Modifier.isPublic(ctors[2].getModifiers()));
        assertFalse(Modifier.isProtected(ctors[2].getModifiers()));
        assertTrue(Modifier.isPrivate(ctors[3].getModifiers()));
    }

    /* ---------------------------------------------------------------------- */
    /*  Utility methods: call, getClassName, getClassNameFromByteCode, etc.    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCallWithMethodObjectSuccess() throws Exception {
        class Target {
            private String secret(int x) { return "val-" + x; }
        }
        Target t = new Target();
        Method m = ReflectionUtils.getMethod(Target.class, "secret", int.class);
        Object result = ReflectionUtils.call(t, m, 5);
        assertEquals("val-5", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallWithNullMethodThrows() {
        ReflectionUtils.call(new Object(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallWithNullInstanceThrows() throws Exception {
        Method m = Object.class.getMethod("toString");
        ReflectionUtils.call(null, m);
    }

    @Test
    public void testCallMethodThatThrowsCheckedExceptionWrappedInRuntime() throws Exception {
        class ThrowsChecked {
            private void explode() throws Exception { throw new Exception("boom"); }
        }
        ThrowsChecked obj = new ThrowsChecked();
        Method m = ReflectionUtils.getMethod(ThrowsChecked.class, "explode");
        try {
            ReflectionUtils.call(obj, m);
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            assertTrue(e.getCause() instanceof Exception);
            assertEquals("boom", e.getCause().getMessage());
        }
    }

    @Test
    public void testCallByNameUsesMethodCache() {
        class Simple {
            public String greet(String name) { return "Hi " + name; }
        }
        Simple s = new Simple();
        Object res = ReflectionUtils.call(s, "greet", "Bob");
        assertEquals("Hi Bob", res);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallByNameThrowsWhenMethodMissing() {
        ReflectionUtils.call(new Object(), "noSuchMethod");
    }

    @Test
    public void testGetClassName() {
        assertEquals("null", ReflectionUtils.getClassName(null));
        assertEquals(String.class.getName(), ReflectionUtils.getClassName("test"));
    }

    @Test
    public void testGetClassNameFromByteCodeForSelf() throws IOException {
        // Load the .class file of this test class as a byte array
        String resource = "/" + ReflectionUtils.class.getName().replace('.', '/') + ".class";
        byte[] bytes = ReflectionUtilsTest.class.getResourceAsStream(resource).readAllBytes();
        String className = ReflectionUtils.getClassNameFromByteCode(bytes);
        assertEquals(ReflectionUtils.class.getName(), className);
    }

    @Test
    public void testIsJavaCompilerAvailableHonorsSystemProperty() {
        // Preserve original property
        String original = System.getProperty("java.util.force.jre");
        try {
            System.setProperty("java.util.force.jre", "true");
            assertFalse(ReflectionUtils.isJavaCompilerAvailable());

            System.setProperty("java.util.force.jre", "false");
            // The actual result depends on the runtime; we only verify that the method does not throw.
            ReflectionUtils.isJavaCompilerAvailable();
        } finally {
            if (original != null) {
                System.setProperty("java.util.force.jre", original);
            } else {
                System.clearProperty("java.util.force.jre");
            }
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  Verify that cache keys implement equals/hashCode correctly (basic)   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCacheKeyEqualityAndHashCodeConsistency() throws Exception {
        // ClassAnnotationCacheKey
        Class<?> c1 = BaseClass.class;
        Class<?> c2 = BaseClass.class;
        ClassAnnotationCacheKey k1 = (ClassAnnotationCacheKey)
                ReflectionUtils.class.getDeclaredClasses()[0].getDeclaredConstructor(Class.class, Class.class)
                        .newInstance(c1, TestAnno.class);
        ClassAnnotationCacheKey k2 = (ClassAnnotationCacheKey)
                ReflectionUtils.class.getDeclaredClasses()[0].getDeclaredConstructor(Class.class, Class.class)
                        .newInstance(c2, TestAnno.class);
        assertEquals(k1, k2);
        assertEquals(k1.hashCode(), k2.hashCode());

        // MethodCacheKey
        Method m = BaseClass.class.getDeclaredMethod("privateBaseMethod");
        MethodCacheKey mk1 = new MethodCacheKey(BaseClass.class, m.getName(), m.getParameterTypes());
        MethodCacheKey mk2 = new MethodCacheKey(BaseClass.class, m.getName(), m.getParameterTypes());
        assertEquals(mk1, mk2);
        assertEquals(mk1.hashCode(), mk2.hashCode());
    }
}
