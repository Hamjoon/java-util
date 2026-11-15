package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link ReflectionUtils}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>annotation lookup on class hierarchies</li>
 *   <li>method annotation lookup on overridden methods</li>
 *   <li>method retrieval with and without argument types (caching behaviour)</li>
 *   <li>field discovery (declared, deep, and field‑map handling)</li>
 *   <li>reflective invocation helpers (including error handling)</li>
 *   <li>constructor lookup and error handling</li>
 *   <li>utility helpers (class name extraction, byte‑code parsing, class‑loader name, param‑key creation)</li>
 *   <li>edge‑cases such as null arguments, overloaded methods, static / transient fields, enum fields, etc.</li>
 * </ul>
 *
 * <p>All helper types used in the tests are defined as static nested classes/annotations inside this test class,
 * ensuring no external dependencies or mocking frameworks are required.
 */
public class ReflectionUtilsTest {

    /* -------------------------------------------------------------
     *  Helper types used throughout the test suite
     * ------------------------------------------------------------- */

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    private @interface SampleClassAnnotation {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    private @interface SampleMethodAnnotation {
        String value();
    }

    private interface InterfaceA {
        @SampleMethodAnnotation("interfaceA")
        void annotatedMethod();
    }

    private interface InterfaceB extends InterfaceA {
    }

    @SampleClassAnnotation("super")
    private static class SuperClass implements InterfaceB {
        @SampleMethodAnnotation("super")
        public void annotatedMethod() {
        }

        public void regularMethod() {
        }

        private static final String STATIC_FIELD = "static";
        private transient int transientField;
        private int regularField;
    }

    private static class SubClass extends SuperClass {
        private String subField;

        @Override
        public void annotatedMethod() {
        }

        public void overloaded(int i) {
        }

        public void overloaded(String s) {
        }
    }

    private static class NoDefaultCtor {
        private NoDefaultCtor(String s) {
        }
    }

    private enum SampleEnum {
        ONE, TWO;

        // compiler generated fields that should be filtered out by buildDeclaredFields()
        private static final String $VALUES = null;
    }

    /* -------------------------------------------------------------
     *  Test fixtures
     * ------------------------------------------------------------- */

    private SubClass subInstance;
    private SuperClass superInstance;

    @Before
    public void setUp() {
        subInstance = new SubClass();
        superInstance = new SuperClass();
        // clear caches that might have been polluted by previous tests (use reflection on the private maps)
        clearCacheMaps();
    }

    /** Clears the internal caches so tests are independent of ordering. */
    private void clearCacheMaps() {
        try {
            Field f = ReflectionUtils.class.getDeclaredField("METHOD_MAP");
            f.setAccessible(true);
            ((ConcurrentMap<?, ?>) f.get(null)).clear();

            f = ReflectionUtils.class.getDeclaredField("METHOD_MAP2");
            f.setAccessible(true);
            ((ConcurrentMap<?, ?>) f.get(null)).clear();

            f = ReflectionUtils.class.getDeclaredField("METHOD_MAP3");
            f.setAccessible(true);
            ((ConcurrentMap<?, ?>) f.get(null)).clear();

            f = ReflectionUtils.class.getDeclaredField("FIELD_MAP");
            f.setAccessible(true);
            ((ConcurrentMap<?, ?>) f.get(null)).clear();

            f = ReflectionUtils.class.getDeclaredField("FIELD_META_CACHE");
            f.setAccessible(true);
            ((ConcurrentMap<?, ?>) f.get(null)).clear();

            f = ReflectionUtils.class.getDeclaredField("CONSTRUCTORS");
            f.setAccessible(true);
            ((ConcurrentMap<?, ?>) f.get(null)).clear();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /* -------------------------------------------------------------
     *  Tests for getClassAnnotation()
     * ------------------------------------------------------------- */

    @Test
    public void testGetClassAnnotation_FindsAnnotationOnSuperclass() {
        SampleClassAnnotation ann = ReflectionUtils.getClassAnnotation(SubClass.class, SampleClassAnnotation.class);
        assertNotNull("Annotation should be found on superclass", ann);
        assertEquals("super", ann.value());
    }

    @Test
    public void testGetClassAnnotation_FindsAnnotationOnInterface() {
        SampleMethodAnnotation ann = ReflectionUtils.getMethodAnnotation(
                getMethodFromClass(SubClass.class, "annotatedMethod"),
                SampleMethodAnnotation.class);
        assertNotNull("Method annotation should be found on interface", ann);
        assertEquals("interfaceA", ann.value());
    }

    @Test
    public void testGetClassAnnotation_ReturnsNullWhenMissing() {
        SampleClassAnnotation ann = ReflectionUtils.getClassAnnotation(String.class, SampleClassAnnotation.class);
        assertNull("No annotation should be found on unrelated class", ann);
    }

    /* -------------------------------------------------------------
     *  Tests for getMethod()
     * ------------------------------------------------------------- */

    @Test
    public void testGetMethod_CachesResult() throws Exception {
        Method m1 = ReflectionUtils.getMethod(SuperClass.class, "regularMethod");
        Method m2 = ReflectionUtils.getMethod(SuperClass.class, "regularMethod");
        assertSame("Second call should return cached Method instance", m1, m2);
    }

    @Test
    public void testGetMethod_ReturnsNullOnMissingMethod() {
        Method m = ReflectionUtils.getMethod(SuperClass.class, "nonExistingMethod");
        assertNull("Should return null when method does not exist", m);
    }

    @Test
    public void testGetMethod_WithParameterTypes() {
        Method m = ReflectionUtils.getMethod(SubClass.class, "overloaded", int.class);
        assertNotNull("Should locate overloaded(int) method", m);
        assertEquals("overloaded", m.getName());
        assertArrayEquals(new Class[]{int.class}, m.getParameterTypes());
    }

    /* -------------------------------------------------------------
     *  Tests for getDeclaredFields() and getDeepDeclaredFields()
     * ------------------------------------------------------------- */

    @Test
    public void testGetDeclaredFields_ReturnsAllDeclaredFieldsIncludingPrivate() {
        List<Field> fields = ReflectionUtils.getDeclaredFields(SubClass.class);
        Set<String> names = new HashSet<>();
        for (Field f : fields) names.add(f.getName());

        assertTrue("Should contain private field 'subField'", names.contains("subField"));
        assertTrue("Should contain inherited but declared field 'regularField' (declared in SuperClass)",
                names.contains("regularField"));
        // static field should be filtered out
        assertFalse("Static field should not be present", names.contains("STATIC_FIELD"));
    }

    @Test
    public void testGetDeepDeclaredFields_IncludesInheritedAndFiltersStaticTransient() {
        Collection<Field> deep = ReflectionUtils.getDeepDeclaredFields(SubClass.class);
        Set<String> names = new HashSet<>();
        for (Field f : deep) names.add(f.getName());

        // fields from SubClass
        assertTrue(names.contains("subField"));
        // fields from SuperClass
        assertTrue(names.contains("regularField"));
        // static and transient must be omitted
        assertFalse(names.contains("STATIC_FIELD"));
        assertFalse(names.contains("transientField"));
    }

    @Test
    public void testGetDeepDeclaredFieldMap_HandlesDuplicateNames() {
        // Create a subclass that redeclares a field name from its superclass
        class DuplicateFieldSuper {
            private String duplicate;
        }
        class DuplicateFieldSub extends DuplicateFieldSuper {
            private String duplicate; // hides super field
        }

        Map<String, Field> map = ReflectionUtils.getDeepDeclaredFieldMap(DuplicateFieldSub.class);
        // The map should contain both fields, one under the simple name, the other under qualified name
        assertTrue(map.containsKey("duplicate"));
        assertTrue(map.containsKey(DuplicateFieldSub.class.getName() + ".duplicate"));
        assertEquals(DuplicateFieldSub.class, map.get(DuplicateFieldSub.class.getName() + ".duplicate").getDeclaringClass());
        assertEquals(DuplicateFieldSuper.class, map.get("duplicate").getDeclaringClass());
    }

    /* -------------------------------------------------------------
     *  Tests for call(Object, Method, ...) and call(Object, String, ...)
     * ------------------------------------------------------------- */

    @Test
    public void testCall_WithMethodObject_InvokesSuccessfully() throws Exception {
        Method m = ReflectionUtils.getMethod(SubClass.class, "regularMethod");
        Object result = ReflectionUtils.call(subInstance, m);
        assertNull("void method should return null", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCall_WithNullMethod_ThrowsIAE() {
        ReflectionUtils.call(subInstance, (Method) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCall_WithNullBean_ThrowsIAE() throws Exception {
        Method m = ReflectionUtils.getMethod(SubClass.class, "regularMethod");
        ReflectionUtils.call(null, m);
    }

    @Test
    public void testCall_WithMethodName_InvokesSuccessfully() {
        Object result = ReflectionUtils.call(subInstance, "regularMethod");
        assertNull("void method should return null", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCall_WithMethodName_NullBean_ThrowsIAE() {
        ReflectionUtils.call(null, "regularMethod");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCall_WithMethodName_NullMethodName_ThrowsIAE() {
        ReflectionUtils.call(subInstance, (String) null);
    }

    @Test(expected = RuntimeException.class)
    public void testCall_WithMethodName_MethodThrowsException_Propagates() {
        class ThrowsException {
            public void explode() {
                throw new IllegalStateException("boom");
            }
        }
        ThrowsException obj = new ThrowsException();
        ReflectionUtils.call(obj, "explode");
    }

    /* -------------------------------------------------------------
     *  Tests for getMethod(Object, String, int)
     * ------------------------------------------------------------- */

    @Test
    public void testGetMethod_ObjectAndArgCount_FindsMethod() {
        Method m = ReflectionUtils.getMethod(subInstance, "regularMethod", 0);
        assertNotNull(m);
        assertEquals("regularMethod", m.getName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethod_ObjectIsNull_ThrowsIAE() {
        ReflectionUtils.getMethod(null, "any", 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethod_MethodNameIsNull_ThrowsIAE() {
        ReflectionUtils.getMethod(subInstance, null, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethod_MethodNotFound_ThrowsIAE() {
        ReflectionUtils.getMethod(subInstance, "nonExisting", 0);
    }

    /* -------------------------------------------------------------
     *  Tests for getConstructor()
     * ------------------------------------------------------------- */

    @Test
    public void testGetConstructor_ValidConstructor_ReturnsIt() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(String.class, byte[].class, int.class, int.class);
        assertNotNull(ctor);
        assertEquals(String.class, ctor.getDeclaringClass());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetConstructor_NoSuchConstructor_ThrowsIAE() {
        ReflectionUtils.getConstructor(NoDefaultCtor.class);
    }

    /* -------------------------------------------------------------
     *  Tests for makeParamKey (private)
     * ------------------------------------------------------------- */

    @Test
    public void testMakeParamKey_EmptyAndNull() throws Exception {
        Method makeKey = ReflectionUtils.class.getDeclaredMethod("makeParamKey", Class[].class);
        makeKey.setAccessible(true);
        assertEquals("", makeKey.invoke(null, (Object) null));
        assertEquals("", makeKey.invoke(null, (Object) new Class[0]));
    }

    @Test
    public void testMakeParamKey_WithParameters() throws Exception {
        Method makeKey = ReflectionUtils.class.getDeclaredMethod("makeParamKey", Class[].class);
        makeKey.setAccessible(true);
        String key = (String) makeKey.invoke(null, (Object) new Class[]{String.class, int.class});
        assertEquals(":java.lang.String|int", key);
    }

    /* -------------------------------------------------------------
     *  Tests for getNonOverloadedMethod()
     * ------------------------------------------------------------- */

    @Test
    public void testGetNonOverloadedMethod_ReturnsMethod() {
        Method m = ReflectionUtils.getNonOverloadedMethod(SuperClass.class, "regularMethod");
        assertNotNull(m);
        assertEquals("regularMethod", m.getName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethod_Overloaded_ThrowsIAE() {
        // SubClass has two overloaded() methods
        ReflectionUtils.getNonOverloadedMethod(SubClass.class, "overloaded");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethod_MethodMissing_ThrowsIAE() {
        ReflectionUtils.getNonOverloadedMethod(SuperClass.class, "doesNotExist");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethod_NullClass_ThrowsIAE() {
        ReflectionUtils.getNonOverloadedMethod(null, "any");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethod_NullMethodName_ThrowsIAE() {
        ReflectionUtils.getNonOverloadedMethod(SuperClass.class, null);
    }

    /* -------------------------------------------------------------
     *  Tests for getClassName()
     * ------------------------------------------------------------- */

    @Test
    public void testGetClassName_NullObject_ReturnsStringNull() {
        assertEquals("null", ReflectionUtils.getClassName(null));
    }

    @Test
    public void testGetClassName_NonNullObject_ReturnsClassName() {
        assertEquals(SubClass.class.getName(), ReflectionUtils.getClassName(subInstance));
    }

    /* -------------------------------------------------------------
     *  Tests for getClassNameFromByteCode()
     * ------------------------------------------------------------- */

    @Test
    public void testGetClassNameFromByteCode_ParsesClassFileCorrectly() throws Exception {
        // Load the .class file for a known class (this test class itself)
        InputStream is = ReflectionUtilsTest.class.getResourceAsStream("ReflectionUtilsTest.class");
        assertNotNull("Unable to locate class file for test class", is);
        byte[] bytes = is.readAllBytes();

        String className = ReflectionUtils.getClassNameFromByteCode(bytes);
        assertEquals(ReflectionUtilsTest.class.getName(), className);
    }

    @Test(expected = IllegalStateException.class)
    public void testGetClassNameFromByteCode_UnsupportedTag_Throws() throws Exception {
        // Corrupt bytecode – change the first tag to an unsupported value (e.g., 99)
        byte[] corrupted = new byte[]{0,0,0,0,0,0,0,0,0,0,99};
        ReflectionUtils.getClassNameFromByteCode(corrupted);
    }

    /* -------------------------------------------------------------
     *  Tests for getClassLoaderName (private)
     * ------------------------------------------------------------- */

    @Test
    public void testGetClassLoaderName_BootstrapClassLoader() throws Exception {
        Method m = ReflectionUtils.class.getDeclaredMethod("getClassLoaderName", Class.class);
        m.setAccessible(true);
        String name = (String) m.invoke(null, Object.class); // Object is loaded by bootstrap
        assertEquals("bootstrap", name);
    }

    @Test
    public void testGetClassLoaderName_NonBootstrap() throws Exception {
        Method m = ReflectionUtils.class.getDeclaredMethod("getClassLoaderName", Class.class);
        m.setAccessible(true);
        String name = (String) m.invoke(null, ReflectionUtilsTest.class);
        assertTrue("Class loader name should contain the class loader's toString()", name.contains("ReflectionUtilsTest"));
    }

    /* -------------------------------------------------------------
     *  Tests for buildDeclaredFields (private)
     * ------------------------------------------------------------- */

    @Test
    public void testBuildDeclaredFields_FiltersStaticAndEnumSyntheticFields() throws Exception {
        Method m = ReflectionUtils.class.getDeclaredMethod("buildDeclaredFields", Class.class);
        m.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Field> fields = (List<Field>) m.invoke(null, SampleEnum.class);
        // Enum constants are not fields returned by getDeclaredFields() (they are synthetic), only the enum's own fields
        for (Field f : fields) {
            assertFalse("Static fields should be filtered", Modifier.isStatic(f.getModifiers()));
            assertFalse("Synthetic enum fields should be filtered", f.getName().equals("$VALUES"));
        }
    }

    @Test
    public void testBuildDeclaredFields_ThrowsWhenClassIsNull() throws Exception {
        Method m = ReflectionUtils.class.getDeclaredMethod("buildDeclaredFields", Class.class);
        m.setAccessible(true);
        try {
            m.invoke(null, (Object) null);
            fail("Expected IllegalArgumentException from Convention.throwIfNull");
        } catch (InvocationTargetException ite) {
            assertTrue(ite.getTargetException() instanceof IllegalArgumentException);
        }
    }

    /* -------------------------------------------------------------
     *  Helper utilities for the tests
     * ------------------------------------------------------------- */

    /** Shortcut to fetch a public method via reflection (used in annotation tests). */
    private static Method getMethodFromClass(Class<?> clazz, String name, Class<?>... paramTypes) {
        try {
            return clazz.getMethod(name, paramTypes);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
