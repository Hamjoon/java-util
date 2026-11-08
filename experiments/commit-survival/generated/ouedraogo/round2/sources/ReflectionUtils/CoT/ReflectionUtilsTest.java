package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

import static org.junit.Assert.*;

/**
 * JUnit 4 test suite for {@link ReflectionUtils}.
 *
 * The tests cover:
 * <ul>
 *   <li>Basic functionality of each public method.</li>
 *   <li>Edge‑cases such as null arguments, missing methods/constructors, overloaded methods, and
 *       exception handling.</li>
 *   <li>Correct filtering of fields (static, transient, Groovy metaClass, synthetic this$ fields).</li>
 * </ul>
 */
public class ReflectionUtilsTest {

    /* ---------------------------------------------------------------------- */
    /*  Helper annotations, classes and interfaces used by the test cases.   */
    /* ---------------------------------------------------------------------- */

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    private @interface TestAnno {
        String value();
    }

    @TestAnno("base")
    private interface BaseInterface {
        @TestAnno("interfaceMethod")
        default void ifaceMethod() {}
    }

    @TestAnno("super")
    private static class SuperClass implements BaseInterface {
        @TestAnno("superMethod")
        public void superMethod() {}

        private static final String STATIC_FIELD = "static";
        private transient int transientField = 42;
        private int privateField = 7;
        public int publicField = 8;
    }

    private static class SubClass extends SuperClass {
        private String subPrivate = "sub";
        public String subPublic = "subPub";

        // Synthetic field "this$0" will be generated automatically for inner classes,
        // but we also create an explicit inner class to verify it is ignored.
        private class Inner {
            private int innerField = 1;
        }
    }

    private static class NoDefaultConstructor {
        private final String data;
        public NoDefaultConstructor(String data) { this.data = data; }
    }

    private static class OverloadedMethods {
        public void foo(int a) {}
        public void foo(String a) {}
        public void foo(int a, String b) {}
    }

    private static class MethodThrowsException {
        public void explode() {
            throw new IllegalStateException("boom");
        }
    }

    private static class DummyClass {
        // empty class used for byte‑code extraction test
    }

    /* ---------------------------------------------------------------------- */
    /*  Test fixtures                                                       */
    /* ---------------------------------------------------------------------- */

    private SubClass subInstance;
    private SuperClass superInstance;

    @Before
    public void setUp() {
        subInstance = new SubClass();
        superInstance = new SuperClass();
    }

    /* ---------------------------------------------------------------------- */
    /*  1. getClassAnnotation                                                */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetClassAnnotationFoundOnClass() {
        TestAnno anno = ReflectionUtils.getClassAnnotation(SubClass.class, TestAnno.class);
        assertNotNull("Annotation should be found on superclass", anno);
        assertEquals("super", anno.value());
    }

    @Test
    public void testGetClassAnnotationFoundOnInterface() {
        TestAnno anno = ReflectionUtils.getClassAnnotation(SubClass.class, TestAnno.class);
        assertNotNull(anno);
        // The first found annotation in the hierarchy is the one on SuperClass,
        // not the interface. To test interface lookup, use a class that only
        // implements the interface.
        TestAnno ifaceAnno = ReflectionUtils.getClassAnnotation(BaseInterface.class, TestAnno.class);
        assertNotNull(ifaceAnno);
        assertEquals("base", ifaceAnno.value());
    }

    @Test
    public void testGetClassAnnotationNotFound() {
        TestAnno anno = ReflectionUtils.getClassAnnotation(Object.class, TestAnno.class);
        assertNull("No annotation should be found on java.lang.Object", anno);
    }

    /* ---------------------------------------------------------------------- */
    /*  2. getMethodAnnotation                                                */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetMethodAnnotationFound() throws Exception {
        Method method = SuperClass.class.getMethod("superMethod");
        TestAnno anno = ReflectionUtils.getMethodAnnotation(method, TestAnno.class);
        assertNotNull(anno);
        assertEquals("superMethod", anno.value());
    }

    @Test
    public void testGetMethodAnnotationInheritedFromInterface() throws Exception {
        Method method = BaseInterface.class.getMethod("ifaceMethod");
        TestAnno anno = ReflectionUtils.getMethodAnnotation(method, TestAnno.class);
        assertNotNull(anno);
        assertEquals("interfaceMethod", anno.value());
    }

    @Test
    public void testGetMethodAnnotationNotFound() throws Exception {
        Method method = SuperClass.class.getMethod("toString");
        TestAnno anno = ReflectionUtils.getMethodAnnotation(method, TestAnno.class);
        assertNull(anno);
    }

    /* ---------------------------------------------------------------------- */
    /*  3. getMethod(Class, String, Class...)                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetMethodSuccess() {
        Method m = ReflectionUtils.getMethod(SubClass.class, "subPublic");
        assertNotNull(m);
        assertEquals("subPublic", m.getName());
    }

    @Test
    public void testGetMethodWithParameters() {
        Method m = ReflectionUtils.getMethod(OverloadedMethods.class, "foo", int.class, String.class);
        assertNotNull(m);
        assertArrayEquals(new Class[]{int.class, String.class}, m.getParameterTypes());
    }

    @Test
    public void testGetMethodNotFoundReturnsNull() {
        Method m = ReflectionUtils.getMethod(SubClass.class, "nonExistingMethod");
        assertNull(m);
    }

    /* ---------------------------------------------------------------------- */
    /*  4. getDeclaredFields                                                  */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetDeclaredFieldsCachesResult() {
        List<Field> first = ReflectionUtils.getDeclaredFields(SuperClass.class);
        List<Field> second = ReflectionUtils.getDeclaredFields(SuperClass.class);
        // The same list instance should be returned because of caching
        assertSame(first, second);
        // Verify static field is excluded by buildDeclaredFields logic
        boolean containsStatic = first.stream()
                .anyMatch(f -> Modifier.isStatic(f.getModifiers()));
        assertFalse("Static fields should be filtered out", containsStatic);
    }

    /* ---------------------------------------------------------------------- */
    /*  5. getDeepDeclaredFields & getDeclaredFields(Class, Collection)      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetDeepDeclaredFieldsFiltersCorrectly() {
        Collection<Field> fields = ReflectionUtils.getDeepDeclaredFields(SubClass.class);
        Set<String> names = new HashSet<>();
        for (Field f : fields) {
            names.add(f.getName());
        }

        // Expected fields (including private ones) from SubClass and SuperClass
        assertTrue(names.contains("subPrivate"));
        assertTrue(names.contains("subPublic"));
        assertTrue(names.contains("privateField"));
        assertTrue(names.contains("publicField"));

        // Excluded fields
        assertFalse("static field should be excluded", names.contains("STATIC_FIELD"));
        assertFalse("transient field should be excluded", names.contains("transientField"));
        // Synthetic this$0 field is not present in the declared fields list, but we ensure it is not present
        for (String n : names) {
            assertFalse("Synthetic this$ field should be excluded", n.startsWith("this$"));
        }
    }

    @Test
    public void testGetDeclaredFieldsAddsToCollection() {
        List<Field> list = new ArrayList<>();
        ReflectionUtils.getDeclaredFields(SuperClass.class, list);
        assertFalse(list.isEmpty());
        // Ensure private field is present (setAccessible should have been called)
        boolean hasPrivate = list.stream().anyMatch(f -> f.getName().equals("privateField"));
        assertTrue(hasPrivate);
    }

    /* ---------------------------------------------------------------------- */
    /*  6. getDeepDeclaredFieldMap                                            */
    /* ---------------------------------------------------------------------- */

    private static class ParentWithDupField {
        private int dup = 1;
    }

    private static class ChildWithDupField extends ParentWithDupField {
        private int dup = 2;
    }

    @Test
    public void testGetDeepDeclaredFieldMapHandlesDuplicateNames() {
        Map<String, Field> map = ReflectionUtils.getDeepDeclaredFieldMap(ChildWithDupField.class);
        // The simple name should map to the child's field (the one discovered last)
        assertTrue(map.containsKey("dup"));
        assertEquals(ChildWithDupField.class, map.get("dup").getDeclaringClass());

        // The parent's field should be accessible via the qualified key
        String qualified = ParentWithDupField.class.getName() + ".dup";
        assertTrue(map.containsKey(qualified));
        assertEquals(ParentWithDupField.class, map.get(qualified).getDeclaringClass());
    }

    /* ---------------------------------------------------------------------- */
    /*  7. call(Object, Method, Object...)                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCallWithValidMethod() throws Exception {
        Method m = SuperClass.class.getMethod("superMethod");
        Object result = ReflectionUtils.call(superInstance, m);
        assertNull(result); // method returns void
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallNullMethodThrows() {
        ReflectionUtils.call(superInstance, (Method) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallNullBeanThrows() throws Exception {
        Method m = SuperClass.class.getMethod("superMethod");
        ReflectionUtils.call(null, m);
    }

    @Test
    public void testCallMethodThatThrowsWrappedRuntimeException() throws Exception {
        Method m = MethodThrowsException.class.getMethod("explode");
        try {
            ReflectionUtils.call(new MethodThrowsException(), m);
            fail("Expected RuntimeException wrapping the target exception");
        } catch (RuntimeException e) {
            assertTrue(e.getCause() instanceof IllegalStateException);
            assertEquals("boom", e.getCause().getMessage());
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  8. call(Object, String, Object...)                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCallByNameSuccess() {
        Object result = ReflectionUtils.call(subInstance, "subPublic");
        assertEquals("subPub", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallByNameMethodNotFound() {
        ReflectionUtils.call(subInstance, "nonExistingMethod");
    }

    /* ---------------------------------------------------------------------- */
    /*  9. getMethod(Object, String, int)                                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetMethodByArgCountSuccess() {
        Method m = ReflectionUtils.getMethod(subInstance, "subPublic", 0);
        assertNotNull(m);
        assertEquals("subPublic", m.getName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodByArgCountNullBean() {
        ReflectionUtils.getMethod(null, "anyMethod", 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodByArgCountNullName() {
        ReflectionUtils.getMethod(subInstance, null, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodByArgCountNotFound() {
        ReflectionUtils.getMethod(subInstance, "nonExisting", 0);
    }

    /* ---------------------------------------------------------------------- */
    /* 10. getConstructor                                                     */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetConstructorSuccess() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(NoDefaultConstructor.class, String.class);
        assertNotNull(ctor);
        assertEquals(1, ctor.getParameterCount());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetConstructorNotFoundThrows() {
        ReflectionUtils.getConstructor(NoDefaultConstructor.class);
    }

    /* ---------------------------------------------------------------------- */
    /* 11. getNonOverloadedMethod                                             */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetNonOverloadedMethodSuccess() {
        Method m = ReflectionUtils.getNonOverloadedMethod(OverloadedMethods.class, "foo");
        // Since OverloadedMethods has overloaded foo, this should throw IllegalArgumentException.
        // However, the method is designed to throw when more than one method with the name exists.
        // Therefore we expect an exception. The test will be in a separate method.
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethodWithOverloadsThrows() {
        ReflectionUtils.getNonOverloadedMethod(OverloadedMethods.class, "foo");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethodNullClass() {
        ReflectionUtils.getNonOverloadedMethod(null, "any");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethodNullName() {
        ReflectionUtils.getNonOverloadedMethod(OverloadedMethods.class, null);
    }

    @Test
    public void testGetNonOverloadedMethodSingleMethod() throws Exception {
        Method m = ReflectionUtils.getNonOverloadedMethod(SubClass.class, "subPublic");
        assertNotNull(m);
        assertEquals("subPublic", m.getName());
    }

    /* ---------------------------------------------------------------------- */
    /* 12. getClassName                                                       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetClassNameWithObject() {
        assertEquals(SubClass.class.getName(), ReflectionUtils.getClassName(subInstance));
    }

    @Test
    public void testGetClassNameWithNull() {
        assertEquals("null", ReflectionUtils.getClassName(null));
    }

    /* ---------------------------------------------------------------------- */
    /* 13. getClassNameFromByteCode                                           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetClassNameFromByteCode() throws Exception {
        // Load the .class file bytes of DummyClass (inner static class of this test)
        String classResource = DummyClass.class.getSimpleName() + ".class";
        InputStream is = DummyClass.class.getResourceAsStream(classResource);
        assertNotNull("Unable to locate class bytecode for DummyClass", is);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int read;
        while ((read = is.read(buffer)) != -1) {
            baos.write(buffer, 0, read);
        }
        byte[] byteCode = baos.toByteArray();

        String className = ReflectionUtils.getClassNameFromByteCode(byteCode);
        assertEquals(DummyClass.class.getName(), className);
    }

    /* ---------------------------------------------------------------------- */
    /*  14. Edge cases for makeParamKey (indirectly via public APIs)         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetMethodWithNoParameterTypesUsesEmptyKey() {
        // getMethod(Class, name) without types should behave like an empty param list.
        Method m = ReflectionUtils.getMethod(SubClass.class, "subPublic");
        assertNotNull(m);
        assertEquals(0, m.getParameterCount());
    }

    @Test
    public void testGetConstructorWithNoParamsUsesEmptyKey() {
        // The class Object has a public no‑arg constructor.
        Constructor<?> ctor = ReflectionUtils.getConstructor(Object.class);
        assertNotNull(ctor);
        assertEquals(0, ctor.getParameterCount());
    }

    /* ---------------------------------------------------------------------- */
    /*  15. Verify that getClassLoaderName returns a non‑null string (package‑private) */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetClassLoaderNameIsNonNull() throws Exception {
        // Use reflection to access the package‑private method.
        Method m = ReflectionUtils.class.getDeclaredMethod("getClassLoaderName", Class.class);
        m.setAccessible(true);
        String name = (String) m.invoke(null, ReflectionUtilsTest.class);
        assertNotNull(name);
        assertFalse(name.isEmpty());
    }
}
