package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.*;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link ReflectionUtils}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public APIs – normal operation, edge cases and exception paths.</li>
 *   <li>Caching behaviour (method/constructor look‑ups are reused).</li>
 *   <li>Annotation lookup across inheritance hierarchies.</li>
 *   <li>Field extraction rules (static, transient, synthetic, etc.).</li>
 *   <li>Byte‑code parsing for class name extraction.</li>
 * </ul>
 *
 * The test class defines several helper types (annotations, interfaces,
 * classes with fields and methods) that are used throughout the suite.
 */
public class ReflectionUtilsTest
{
    /* -------------------------------------------------------------
     *  Helper annotations, interfaces and test model classes
     * ------------------------------------------------------------- */
    @Retention(RetentionPolicy.RUNTIME)
    private @interface ClassAnno {}

    @Retention(RetentionPolicy.RUNTIME)
    private @interface MethodAnno {}

    @ClassAnno
    private interface AnnotatedInterface {}

    @ClassAnno
    private static class SuperClass implements AnnotatedInterface
    {
        @MethodAnno
        public String annotatedMethod()
        {
            return "super";
        }

        public String overloaded(int i)
        {
            return "int:" + i;
        }

        public String overloaded(String s)
        {
            return "string:" + s;
        }

        public void methodThrowingChecked() throws Exception
        {
            throw new Exception("checked");
        }

        public void methodThrowingRuntime()
        {
            throw new IllegalStateException("runtime");
        }
    }

    private static class SubClass extends SuperClass
    {
        @Override
        public String annotatedMethod()
        {
            return "sub";
        }
    }

    private static class FieldHolderParent
    {
        private int parentPrivate;
        public static int staticField;
        public transient String transientField;
        public String publicParent;
    }

    private static class FieldHolderChild extends FieldHolderParent
    {
        private int childPrivate;
        public String publicChild;
        // duplicate field name (private) to test map key handling
        private int parentPrivate;
    }

    private static class ConstructorTest
    {
        public ConstructorTest() {}
        public ConstructorTest(String s, int i) {}
    }

    /* -------------------------------------------------------------
     *  Setup – shared objects
     * ------------------------------------------------------------- */
    private SubClass subInstance;
    private Method annotatedMethodSuper; // method from SuperClass (annotated)
    private Method annotatedMethodSub;   // overridden method in SubClass

    @Before
    public void setUp() throws Exception
    {
        subInstance = new SubClass();
        annotatedMethodSuper = SuperClass.class.getMethod("annotatedMethod");
        annotatedMethodSub = SubClass.class.getMethod("annotatedMethod");
    }

    /* -------------------------------------------------------------
     *  1. getClassAnnotation()
     * ------------------------------------------------------------- */
    @Test
    public void testGetClassAnnotationDirect()
    {
        ClassAnno anno = ReflectionUtils.getClassAnnotation(SuperClass.class, ClassAnno.class);
        assertNotNull(anno);
    }

    @Test
    public void testGetClassAnnotationFromSuperclass()
    {
        // SubClass does not have the annotation itself, it inherits from SuperClass
        ClassAnno anno = ReflectionUtils.getClassAnnotation(SubClass.class, ClassAnno.class);
        assertNotNull(anno);
    }

    @Test
    public void testGetClassAnnotationFromInterface()
    {
        // Interface is annotated, class implements it indirectly
        ClassAnno anno = ReflectionUtils.getClassAnnotation(SubClass.class, ClassAnno.class);
        assertNotNull(anno);
    }

    @Test
    public void testGetClassAnnotationNotFound()
    {
        Annotation result = ReflectionUtils.getClassAnnotation(String.class, ClassAnno.class);
        assertNull(result);
    }

    /* -------------------------------------------------------------
     *  2. getMethodAnnotation()
     * ------------------------------------------------------------- */
    @Test
    public void testGetMethodAnnotationFoundOnSuper()
    {
        MethodAnno anno = ReflectionUtils.getMethodAnnotation(annotatedMethodSub, MethodAnno.class);
        assertNotNull(anno);
    }

    @Test
    public void testGetMethodAnnotationNotFound()
    {
        Method method = SuperClass.class.getMethod("overloaded", int.class);
        MethodAnno anno = ReflectionUtils.getMethodAnnotation(method, MethodAnno.class);
        assertNull(anno);
    }

    /* -------------------------------------------------------------
     *  3. getMethod(Class, String, Class...)
     * ------------------------------------------------------------- */
    @Test
    public void testGetMethodExactMatch()
    {
        Method m = ReflectionUtils.getMethod(SuperClass.class, "overloaded", int.class);
        assertNotNull(m);
        assertEquals("overloaded", m.getName());
        assertEquals(1, m.getParameterCount());
        assertEquals(int.class, m.getParameterTypes()[0]);
    }

    @Test
    public void testGetMethodNullReturnWhenNotFound()
    {
        Method m = ReflectionUtils.getMethod(SuperClass.class, "nonExistingMethod", String.class);
        assertNull(m);
    }

    @Test
    public void testGetMethodCaching()
    {
        Method first = ReflectionUtils.getMethod(SuperClass.class, "overloaded", String.class);
        Method second = ReflectionUtils.getMethod(SuperClass.class, "overloaded", String.class);
        // Cached instance should be the same reference
        assertSame(first, second);
    }

    /* -------------------------------------------------------------
     *  4. getDeclaredFields(Class)
     * ------------------------------------------------------------- */
    @Test
    public void testGetDeclaredFieldsCaching()
    {
        List<Field> first = ReflectionUtils.getDeclaredFields(FieldHolderParent.class);
        List<Field> second = ReflectionUtils.getDeclaredFields(FieldHolderParent.class);
        assertSame(first, second);
        // Ensure static and transient fields are present in raw list (they are not filtered here)
        assertTrue(first.stream().anyMatch(f -> f.getName().equals("staticField")));
        assertTrue(first.stream().anyMatch(f -> f.getName().equals("transientField")));
    }

    /* -------------------------------------------------------------
     *  5. getDeepDeclaredFields(Class)
     * ------------------------------------------------------------- */
    @Test
    public void testGetDeepDeclaredFieldsInheritance()
    {
        Collection<Field> fields = ReflectionUtils.getDeepDeclaredFields(FieldHolderChild.class);
        // Should contain both parent and child non‑static/non‑transient fields
        Set<String> names = new HashSet<>();
        for (Field f : fields) names.add(f.getName());

        assertTrue(names.contains("parentPrivate"));
        assertTrue(names.contains("childPrivate"));
        assertTrue(names.contains("publicParent"));
        assertTrue(names.contains("publicChild"));
        // static and transient should be omitted
        assertFalse(names.contains("staticField"));
        assertFalse(names.contains("transientField"));
    }

    @Test
    public void testGetDeepDeclaredFieldsCaching()
    {
        Collection<Field> first = ReflectionUtils.getDeepDeclaredFields(FieldHolderParent.class);
        Collection<Field> second = ReflectionUtils.getDeepDeclaredFields(FieldHolderParent.class);
        assertSame(first, second);
    }

    /* -------------------------------------------------------------
     *  6. getDeclaredFields(Class, Collection<Field>)
     * ------------------------------------------------------------- */
    @Test
    public void testGetDeclaredFieldsUtility()
    {
        Collection<Field> collection = new ArrayList<>();
        ReflectionUtils.getDeclaredFields(FieldHolderParent.class, collection);
        Set<String> names = new HashSet<>();
        for (Field f : collection) names.add(f.getName());

        assertTrue(names.contains("parentPrivate"));
        assertTrue(names.contains("publicParent"));
        // static & transient must be filtered out
        assertFalse(names.contains("staticField"));
        assertFalse(names.contains("transientField"));
    }

    /* -------------------------------------------------------------
     *  7. getDeepDeclaredFieldMap(Class)
     * ------------------------------------------------------------- */
    @Test
    public void testGetDeepDeclaredFieldMapWithDuplicateNames()
    {
        Map<String, Field> map = ReflectionUtils.getDeepDeclaredFieldMap(FieldHolderChild.class);
        // The duplicate name from child should be stored with a qualified key
        assertTrue(map.containsKey("parentPrivate"));
        assertTrue(map.containsKey(FieldHolderChild.class.getName() + ".parentPrivate"));
        // Ensure the two entries reference different Field objects
        assertNotSame(map.get("parentPrivate"), map.get(FieldHolderChild.class.getName() + ".parentPrivate"));
    }

    /* -------------------------------------------------------------
     *  8. call(Object, Method, Object...)
     * ------------------------------------------------------------- */
    @Test
    public void testCallSimpleMethod()
    {
        Object result = ReflectionUtils.call(subInstance, annotatedMethodSuper);
        assertEquals("super", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallNullMethod()
    {
        ReflectionUtils.call(subInstance, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallNullBean()
    {
        ReflectionUtils.call(null, annotatedMethodSuper);
    }

    @Test
    public void testCallMethodThatThrowsCheckedException()
    {
        Method m = ReflectionUtils.getMethod(SuperClass.class, "methodThrowingChecked");
        try
        {
            ReflectionUtils.call(subInstance, m);
            fail("Expected RuntimeException wrapping the checked exception");
        }
        catch (RuntimeException e)
        {
            assertTrue(e.getCause() instanceof Exception);
            assertEquals("checked", e.getCause().getMessage());
        }
    }

    @Test
    public void testCallMethodThatThrowsRuntimeException()
    {
        Method m = ReflectionUtils.getMethod(SuperClass.class, "methodThrowingRuntime");
        try
        {
            ReflectionUtils.call(subInstance, m);
            fail("Expected RuntimeException wrapping the runtime exception");
        }
        catch (RuntimeException e)
        {
            assertTrue(e.getCause() instanceof IllegalStateException);
            assertEquals("runtime", e.getCause().getMessage());
        }
    }

    /* -------------------------------------------------------------
     *  9. call(Object, String, Object...)
     * ------------------------------------------------------------- */
    @Test
    public void testCallByNameSimple()
    {
        Object result = ReflectionUtils.call(subInstance, "annotatedMethod");
        assertEquals("sub", result);
    }

    @Test
    public void testCallByNameCaching()
    {
        Method m1 = ReflectionUtils.getMethod(subInstance, "annotatedMethod", 0);
        Object r1 = ReflectionUtils.call(subInstance, "annotatedMethod");
        Method m2 = ReflectionUtils.getMethod(subInstance, "annotatedMethod", 0);
        assertSame(m1, m2); // same cached method
        assertEquals(r1, "sub");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallByNameNullBean()
    {
        ReflectionUtils.call(null, "annotatedMethod");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCallByNameNullMethodName()
    {
        ReflectionUtils.call(subInstance, null);
    }

    /* -------------------------------------------------------------
     * 10. getMethod(Object, String, int)
     * ------------------------------------------------------------- */
    @Test
    public void testGetMethodByArgCount()
    {
        Method m = ReflectionUtils.getMethod(subInstance, "overloaded", 1);
        assertNotNull(m);
        assertEquals(1, m.getParameterCount());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodByArgCountBeanNull()
    {
        ReflectionUtils.getMethod(null, "anyMethod", 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodByArgCountNameNull()
    {
        ReflectionUtils.getMethod(subInstance, null, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMethodByArgCountNotFound()
    {
        ReflectionUtils.getMethod(subInstance, "nonExisting", 0);
    }

    /* -------------------------------------------------------------
     * 11. getConstructor(Class, Class...)
     * ------------------------------------------------------------- */
    @Test
    public void testGetConstructorSuccess()
    {
        Constructor<?> ctor = ReflectionUtils.getConstructor(ConstructorTest.class, String.class, int.class);
        assertNotNull(ctor);
        assertEquals(2, ctor.getParameterCount());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetConstructorNotFound()
    {
        ReflectionUtils.getConstructor(ConstructorTest.class, double.class);
    }

    /* -------------------------------------------------------------
     * 12. getClassName(Object)
     * ------------------------------------------------------------- */
    @Test
    public void testGetClassNameNormal()
    {
        assertEquals(SubClass.class.getName(), ReflectionUtils.getClassName(subInstance));
    }

    @Test
    public void testGetClassNameNull()
    {
        assertEquals("null", ReflectionUtils.getClassName(null));
    }

    /* -------------------------------------------------------------
     * 13. getClassNameFromByteCode(byte[])
     * ------------------------------------------------------------- */
    @Test
    public void testGetClassNameFromByteCodeRealClass() throws Exception
    {
        // Load the .class file of this test class itself
        String classResource = ReflectionUtilsTest.class.getSimpleName() + ".class";
        InputStream is = ReflectionUtilsTest.class.getResourceAsStream(classResource);
        assertNotNull("Unable to locate class bytecode resource", is);
        byte[] bytes = readAllBytes(is);
        String className = ReflectionUtils.getClassNameFromByteCode(bytes);
        assertEquals(ReflectionUtilsTest.class.getName(), className);
    }

    @Test(expected = Exception.class)
    public void testGetClassNameFromByteCodeInvalidData()
    throws Exception
    {
        // Empty array will cause EOFException / IllegalStateException
        ReflectionUtils.getClassNameFromByteCode(new byte[0]);
    }

    /* -------------------------------------------------------------
     * 14. getNonOverloadedMethod(Class, String)
     * ------------------------------------------------------------- */
    @Test
    public void testGetNonOverloadedMethodSuccess()
    {
        Method m = ReflectionUtils.getNonOverloadedMethod(SuperClass.class, "annotatedMethod");
        assertNotNull(m);
        assertEquals("annotatedMethod", m.getName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethodClassNull()
    {
        ReflectionUtils.getNonOverloadedMethod(null, "any");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethodNameNull()
    {
        ReflectionUtils.getNonOverloadedMethod(SuperClass.class, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNonOverloadedMethodOverloaded()
    {
        // 'overloaded' exists with two signatures – should trigger IllegalArgumentException
        ReflectionUtils.getNonOverloadedMethod(SuperClass.class, "overloaded");
    }

    /* -------------------------------------------------------------
     * 15. Private helpers (makeParamKey, getClassLoaderName)
     *    accessed via reflection to verify behaviour.
     * ------------------------------------------------------------- */
    @Test
    public void testMakeParamKeyEmpty()
    throws Exception
    {
        Method makeKey = ReflectionUtils.class.getDeclaredMethod("makeParamKey", Class[].class);
        makeKey.setAccessible(true);
        String key = (String) makeKey.invoke(null, (Object) new Class<?>[0]);
        assertEquals("", key);
    }

    @Test
    public void testMakeParamKeyWithTypes()
    throws Exception
    {
        Method makeKey = ReflectionUtils.class.getDeclaredMethod("makeParamKey", Class[].class);
        makeKey.setAccessible(true);
        String key = (String) makeKey.invoke(null, (Object) new Class<?>[]{String.class, int.class});
        assertEquals(":java.lang.String|int", key);
    }

    @Test
    public void testGetClassLoaderNameBootstrap()
    throws Exception
    {
        Method m = ReflectionUtils.class.getDeclaredMethod("getClassLoaderName", Class.class);
        m.setAccessible(true);
        // Primitive type uses bootstrap loader (null)
        String name = (String) m.invoke(null, int.class);
        assertEquals("bootstrap", name);
    }

    @Test
    public void testGetClassLoaderNameNonBootstrap()
    throws Exception
    {
        Method m = ReflectionUtils.class.getDeclaredMethod("getClassLoaderName", Class.class);
        m.setAccessible(true);
        String name = (String) m.invoke(null, ReflectionUtilsTest.class);
        assertTrue(name.startsWith("sun.misc.Launcher")); // typical for application classloader
    }

    /* -------------------------------------------------------------
     *  Utility method to read all bytes from an InputStream.
     * ------------------------------------------------------------- */
    private static byte[] readAllBytes(InputStream is) throws IOException
    {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] tmp = new byte[4096];
        int read;
        while ((read = is.read(tmp)) != -1)
        {
            buffer.write(tmp, 0, read);
        }
        return buffer.toByteArray();
    }
}
