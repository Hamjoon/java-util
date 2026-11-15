package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite that exercises every public API of {@link ReflectionUtils}.
 * <p>
 * The suite follows the steps requested:
 * <ol>
 *   <li>List all public methods (see the block comment at the top of the class).</li>
 *   <li>Provide a basic functional test for each method.</li>
 *   <li>Identify edge‑cases / exception scenarios.</li>
 *   <li>Write additional tests that hit those edge cases.</li>
 *   <li>Combine everything into a single test file.</li>
 * </ol>
 */
public class ReflectionUtilsTest {

    /* -----------------------------------------------------------------------
     *  Public methods of ReflectionUtils (as extracted from the source)
     * -----------------------------------------------------------------------
     *
     * 1.  <T extends Annotation> T getClassAnnotation(Class<?> classToCheck,
     *                                                   Class<T> annoClass)
     *
     * 2.  <T extends Annotation> T getMethodAnnotation(Method method,
     *                                                    Class<T> annoClass)
     *
     * 3.  Method getMethod(Class<?> c, String methodName, Class<?>... types)
     *
     * 4.  List<Field> getDeclaredFields(Class<?> c)
     *
     * 5.  Collection<Field> getDeepDeclaredFields(Class<?> c)
     *
     * 6.  void getDeclaredFields(Class<?> c, Collection<Field> fields)
     *
     * 7.  Map<String, Field> getDeepDeclaredFieldMap(Class<?> c)
     *
     * 8.  Object call(Object bean, Method method, Object... args)
     *
     * 9.  Object call(Object bean, String methodName, Object... args)
     *
     * 10. Method getMethod(Object bean, String methodName, int argCount)
     *
     * 11. Constructor<?> getConstructor(Class<?> clazz, Class<?>... parameterTypes)
     *
     * 12. Method getNonOverloadedMethod(Class<?> clazz, String methodName)
     *
     * 13. String getClassName(Object o)
     *
     * 14. String getClassNameFromByteCode(byte[] byteCode) throws Exception
     *
     * ----------------------------------------------------------------------- */

    /* -----------------------------------------------------------------------
     *  Helper annotation and test classes used by many test cases
     * ----------------------------------------------------------------------- */
    @Retention(RetentionPolicy.RUNTIME)
    private @interface TestAnno { }

    @TestAnno
    private static class BaseAnnotatedClass {
        @TestAnno
        public void annotatedMethod() { }

        public void nonAnnotatedMethod() { }

        public int publicField = 1;
        private String privateField = "secret";
        static int staticField = 42;
        transient int transientField = 7;
    }

    private static class SubClass extends BaseAnnotatedClass {
        @Override
        public void annotatedMethod() { } // overrides but still should inherit annotation

        public void subMethod(String s) { }

        private void privateOnly() { }
    }

    private static class Overloaded {
        public void foo() { }
        public void foo(int i) { }
        public void foo(String a, String b) { }
    }

    private static class NoArgClass {
        public void simple() { }
    }

    private static class ExceptionThrower {
        public void explode() {
            throw new IllegalStateException("boom");
        }
    }

    private static class PrivateConstructor {
        private PrivateConstructor() { }
        public PrivateConstructor(String s) { }
    }

    /* -----------------------------------------------------------------------
     *  Basic functional tests
     * ----------------------------------------------------------------------- */

    @Test
    public void testGetClassAnnotation_FoundOnSuperclass() {
        TestAnno anno = ReflectionUtils.getClassAnnotation(SubClass.class, TestAnno.class);
        assertNotNull("Annotation should be discovered on superclass", anno);
    }

    @Test
    public void testGetClassAnnotation_NotFoundReturnsNull() {
        TestAnno anno = ReflectionUtils.getClassAnnotation(String.class, TestAnno.class);
        assertNull("No annotation expected on java.lang.String", anno);
    }

    @Test
    public void testGetMethodAnnotation_FoundOnSuperclassMethod() throws Exception {
        Method subMethod = SubClass.class.getDeclaredMethod("annotatedMethod");
        TestAnno anno = ReflectionUtils.getMethodAnnotation(subMethod, TestAnno.class);
        assertNotNull("Method annotation should be found on overridden method", anno);
    }

    @Test
    public void testGetMethodAnnotation_NotFoundReturnsNull() throws Exception {
        Method m = BaseAnnotatedClass.class.getDeclaredMethod("nonAnnotatedMethod");
        TestAnno anno = ReflectionUtils.getMethodAnnotation(m, TestAnno.class);
        assertNull("Method has no annotation", anno);
    }

    @Test
    public void testGetMethod_SimpleLookupAndCache() throws Exception {
        Method m1 = ReflectionUtils.getMethod(BaseAnnotatedClass.class, "annotatedMethod");
        assertNotNull(m1);
        // second call should return the *same* Method instance because of caching
        Method m2 = ReflectionUtils.getMethod(BaseAnnotatedClass.class, "annotatedMethod");
        assertSame("Method instance should be cached", m1, m2);
    }

    @Test
    public void testGetMethod_NotFoundReturnsNull() {
        Method m = ReflectionUtils.getMethod(BaseAnnotatedClass.class, "nonExistent", String.class);
        assertNull("Lookup of a non‑existent method must return null", m);
    }

    @Test
    public void testGetDeclaredFields_ExcludesStaticAndTransient() {
        List<Field> fields = ReflectionUtils.getDeclaredFields(BaseAnnotatedClass.class);
        Set<String> names = new HashSet<>();
        for (Field f : fields) names.add(f.getName());

        assertTrue(names.contains("publicField"));
        assertTrue(names.contains("privateField"));
        assertFalse("static field must be filtered out", names.contains("staticField"));
        assertFalse("transient field must be filtered out", names.contains("transientField"));
    }

    @Test
    public void testGetDeepDeclaredFields_IncludesInheritedNonStatic() {
        Collection<Field> fields = ReflectionUtils.getDeepDeclaredFields(SubClass.class);
        Set<String> names = new HashSet<>();
        for (Field f : fields) names.add(f.getName());

        // from SubClass
        assertTrue(names.contains("subMethod"));
        // from BaseAnnotatedClass (publicField, privateField)
        assertTrue(names.contains("publicField"));
        assertTrue(names.contains("privateField"));
        // static and transient should never appear
        assertFalse(names.contains("staticField"));
        assertFalse(names.contains("transientField"));
    }

    @Test
    public void testGetDeclaredFields_CollectionParameter() {
        List<Field> collector = new ArrayList<>();
        ReflectionUtils.getDeclaredFields(BaseAnnotatedClass.class, collector);
        Set<String> names = new HashSet<>();
        for (Field f : collector) names.add(f.getName());

        assertTrue(names.contains("publicField"));
        assertTrue(names.contains("privateField"));
        assertFalse(names.contains("staticField"));
    }

    @Test
    public void testGetDeepDeclaredFieldMap_UniqueAndDuplicateNames() {
        // Create a subclass that redeclares a field name to trigger duplicate handling
        class DuplicateField extends BaseAnnotatedClass {
            private int publicField; // hides the superclass field
        }

        Map<String, Field> map = ReflectionUtils.getDeepDeclaredFieldMap(DuplicateField.class);
        // The simple name key should refer to the subclass field (the later one wins)
        assertTrue(map.containsKey("publicField"));
        assertEquals("private int DuplicateField.publicField",
                map.get("publicField").toString());

        // The fully‑qualified duplicate key must also be present
        String qualified = DuplicateField.class.getName() + ".publicField";
        assertTrue(map.containsKey(qualified));
        assertEquals("private int DuplicateField.publicField",
                map.get(qualified).toString());
    }

    @Test
    public void testCall_WithMethodObject() throws Exception {
        BaseAnnotatedClass bean = new BaseAnnotatedClass();
        Method m = ReflectionUtils.getMethod(BaseAnnotatedClass.class, "annotatedMethod");
        Object result = ReflectionUtils.call(bean, m);
        assertNull("annotatedMethod returns void → null", result);
    }

    @Test
    public void testCall_WithMethodObject_NullMethodThrows() {
        try {
            ReflectionUtils.call(new Object(), null);
            fail("Expected IllegalArgumentException for null method");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("null Method passed"));
        }
    }

    @Test
    public void testCall_WithMethodObject_NullBeanThrows() throws Exception {
        Method m = ReflectionUtils.getMethod(BaseAnnotatedClass.class, "annotatedMethod");
        try {
            ReflectionUtils.call(null, m);
            fail("Expected IllegalArgumentException for null bean");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Cannot call"));
        }
    }

    @Test
    public void testCall_WithMethodObject_MethodThrowsWrappedRuntimeException() throws Exception {
        ExceptionThrower bean = new ExceptionThrower();
        Method m = ReflectionUtils.getMethod(ExceptionThrower.class, "explode");
        try {
            ReflectionUtils.call(bean, m);
            fail("Expected RuntimeException wrapping the IllegalStateException");
        } catch (RuntimeException e) {
            assertTrue(e.getCause() instanceof IllegalStateException);
            assertEquals("boom", e.getCause().getMessage());
        }
    }

    @Test
    public void testCall_WithMethodName() throws Exception {
        BaseAnnotatedClass bean = new BaseAnnotatedClass();
        Object result = ReflectionUtils.call(bean, "annotatedMethod");
        assertNull(result);
    }

    @Test
    public void testGetMethod_ObjectSignature_Found() {
        BaseAnnotatedClass bean = new BaseAnnotatedClass();
        Method m = ReflectionUtils.getMethod(bean, "annotatedMethod", 0);
        assertNotNull(m);
        assertEquals("annotatedMethod", m.getName());
    }

    @Test
    public void testGetMethod_ObjectSignature_BeanNullThrows() {
        try {
            ReflectionUtils.getMethod(null, "any", 0);
            fail("Expected IllegalArgumentException for null bean");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("null instance"));
        }
    }

    @Test
    public void testGetMethod_ObjectSignature_MethodNameNullThrows() {
        try {
            ReflectionUtils.getMethod(new Object(), null, 0);
            fail("Expected IllegalArgumentException for null method name");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("null method name"));
        }
    }

    @Test
    public void testGetMethod_ObjectSignature_MethodNotFoundThrows() {
        try {
            ReflectionUtils.getMethod(new BaseAnnotatedClass(), "doesNotExist", 0);
            fail("Expected IllegalArgumentException for unknown method");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Method: doesNotExist() is not found"));
        }
    }

    @Test
    public void testGetConstructor_Existing() {
        Constructor<?> c = ReflectionUtils.getConstructor(String.class, byte[].class);
        assertNotNull(c);
        assertEquals(String.class, c.getDeclaringClass());
    }

    @Test
    public void testGetConstructor_NoSuchConstructorThrows() {
        try {
            ReflectionUtils.getConstructor(String.class, int.class);
            fail("Expected IllegalArgumentException for missing constructor");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Attempted to get Constructor that did not exist"));
        }
    }

    @Test
    public void testGetNonOverloadedMethod_Found() {
        Method m = ReflectionUtils.getNonOverloadedMethod(NoArgClass.class, "simple");
        assertNotNull(m);
        assertEquals("simple", m.getName());
    }

    @Test
    public void testGetNonOverloadedMethod_ClassNullThrows() {
        try {
            ReflectionUtils.getNonOverloadedMethod(null, "any");
            fail("Expected IllegalArgumentException for null class");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("null class"));
        }
    }

    @Test
    public void testGetNonOverloadedMethod_MethodNameNullThrows() {
        try {
            ReflectionUtils.getNonOverloadedMethod(Object.class, null);
            fail("Expected IllegalArgumentException for null method name");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("null method name"));
        }
    }

    @Test
    public void testGetNonOverloadedMethod_OverloadedMethodThrows() {
        try {
            ReflectionUtils.getNonOverloadedMethod(Overloaded.class, "foo");
            fail("Expected IllegalArgumentException because method is overloaded");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("overloaded methods"));
        }
    }

    @Test
    public void testGetClassName_NullAndNonNull() {
        assertEquals("null", ReflectionUtils.getClassName(null));
        assertEquals(String.class.getName(), ReflectionUtils.getClassName("hello"));
    }

    @Test
    public void testGetClassNameFromByteCode_ValidClass() throws Exception {
        // Load the byte‑code of a known class (the test class itself) via the class loader.
        String className = this.getClass().getName();
        String resourcePath = '/' + className.replace('.', '/') + ".class";
        byte[] bytes = this.getClass().getResourceAsStream(resourcePath).readAllBytes();

        String extracted = ReflectionUtils.getClassNameFromByteCode(bytes);
        assertEquals(className, extracted);
    }

    @Test
    public void testGetClassNameFromByteCode_MalformedThrows() {
        // A minimal but invalid class file (wrong magic number)
        byte[] broken = new byte[] {0,0,0,0, 0,0, 0,0};
        try {
            ReflectionUtils.getClassNameFromByteCode(broken);
            fail("Expected IllegalStateException or other exception for malformed bytecode");
        } catch (Exception e) {
            // The method may throw IllegalStateException wrapped in Exception
            assertTrue(e instanceof IllegalStateException ||
                       e.getCause() instanceof IllegalStateException);
        }
    }

    /* -----------------------------------------------------------------------
     *  Tests that reach into package‑private helpers via reflection
     * ----------------------------------------------------------------------- */

    @Test
    public void testGetClassLoaderName_ForBootstrapClass() throws Exception {
        Method m = ReflectionUtils.class.getDeclaredMethod("getClassLoaderName", Class.class);
        m.setAccessible(true);
        String result = (String) m.invoke(null, String.class);
        assertEquals("bootstrap", result);
    }

    @Test
    public void testMakeParamKey_IndirectlyViaGetConstructor() {
        // Two constructors with different signatures should generate different keys.
        Constructor<?> c1 = ReflectionUtils.getConstructor(String.class, byte[].class);
        Constructor<?> c2 = ReflectionUtils.getConstructor(String.class, String.class);
        assertNotNull(c1);
        assertNotNull(c2);
        // They are different constructors, proving that distinct param keys are used.
        assertNotEquals(c1, c2);
    }

    /* -----------------------------------------------------------------------
     *  Utility to generate a tiny synthetic class file for malformed‑bytecode test
     * ----------------------------------------------------------------------- */
    private static byte[] createInvalidClassFile() throws Exception {
        // Very small, deliberately incorrect class file (just the magic number + zeros)
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(0xCAFEBABE); // correct magic number (still accepted)
        dos.writeShort(0); // minor version
        dos.writeShort(0); // major version
        dos.writeShort(1); // constant_pool_count (minimum, but no entries)
        dos.writeShort(0); // access_flags
        dos.writeShort(0); // this_class
        dos.writeShort(0); // super_class
        dos.flush();
        return baos.toByteArray();
    }

    @Test
    public void testGetClassNameFromByteCode_ArtificialInvalidFileThrows() throws Exception {
        byte[] broken = createInvalidClassFile();
        try {
            ReflectionUtils.getClassNameFromByteCode(broken);
            fail("Expected IllegalStateException for malformed constant pool");
        } catch (IllegalStateException e) {
            // Expected path
        }
    }
}
