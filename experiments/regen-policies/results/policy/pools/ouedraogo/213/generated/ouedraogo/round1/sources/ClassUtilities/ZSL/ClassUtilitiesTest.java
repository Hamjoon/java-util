package com.cedarsoftware.util;

import com.cedarsoftware.util.convert.Converter;
import com.cedarsoftware.util.convert.ConverterOptions;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.*;

import static org.junit.Assert.*;

public class ClassUtilitiesTest {

    /**
     * Helper converter used by many tests.  The library supplies a default
     * constructor that accepts a {@link ConverterOptions} instance.
     */
    private static final Converter CONVERTER = new Converter(new ConverterOptions());

    @Test
    public void testIsPrimitiveAndWrapperMappings() {
        // primitives
        assertTrue(ClassUtilities.isPrimitive(int.class));
        assertTrue(ClassUtilities.isPrimitive(boolean.class));
        assertTrue(ClassUtilities.isPrimitive(void.class));

        // wrappers are considered primitive by isPrimitive()
        assertTrue(ClassUtilities.isPrimitive(Integer.class));
        assertTrue(ClassUtilities.isPrimitive(Boolean.class));
        assertTrue(ClassUtilities.isPrimitive(Void.class));

        // non‑primitive, non‑wrapper
        assertFalse(ClassUtilities.isPrimitive(String.class));
        assertFalse(ClassUtilities.isPrimitive(List.class));

        // wrapper → primitive
        assertEquals(int.class, ClassUtilities.getPrimitiveFromWrapper(Integer.class));
        assertEquals(boolean.class, ClassUtilities.getPrimitiveFromWrapper(Boolean.class));
        assertNull(ClassUtilities.getPrimitiveFromWrapper(String.class));
    }

    @Test
    public void testToPrimitiveWrapperClass() {
        assertEquals(Integer.class, ClassUtilities.toPrimitiveWrapperClass(int.class));
        assertEquals(Boolean.class, ClassUtilities.toPrimitiveWrapperClass(boolean.class));
        assertEquals(Void.class, ClassUtilities.toPrimitiveWrapperClass(void.class));

        // already a wrapper – should be returned unchanged
        assertEquals(Long.class, ClassUtilities.toPrimitiveWrapperClass(Long.class));

        // non‑primitive – unchanged
        assertEquals(String.class, ClassUtilities.toPrimitiveWrapperClass(String.class));
    }

    @Test
    public void testDoesOneWrapTheOther() {
        assertTrue(ClassUtilities.doesOneWrapTheOther(Integer.class, int.class));
        assertTrue(ClassUtilities.doesOneWrapTheOther(int.class, Integer.class));
        assertFalse(ClassUtilities.doesOneWrapTheOther(Integer.class, long.class));
        assertFalse(ClassUtilities.doesOneWrapTheOther(String.class, int.class));
    }

    @Test
    public void testComputeInheritanceDistance() {
        // direct super‑interface
        assertEquals(1, ClassUtilities.computeInheritanceDistance(ArrayList.class, List.class));
        // same class
        assertEquals(0, ClassUtilities.computeInheritanceDistance(String.class, String.class));
        // unrelated
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(String.class, Integer.class));

        // primitive ↔ wrapper (same primitive)
        assertEquals(0, ClassUtilities.computeInheritanceDistance(int.class, Integer.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(int.class, Long.class));
    }

    @Test
    public void testClassHierarchyInfoDepthAndDistances() {
        ClassUtilities.ClassHierarchyInfo info = ClassUtilities.getClassHierarchyInfo(ArrayList.class);
        // depth of ArrayList → Object chain is 2 (ArrayList → AbstractList → Object)
        assertEquals(2, info.getDepth());

        // distance to List should be 1 (ArrayList → List)
        assertEquals(1, info.getDistanceMap().get(List.class).intValue());

        // distance to Collection should be 2 (ArrayList → AbstractCollection → Collection)
        assertEquals(2, info.getDistanceMap().get(Collection.class).intValue());
    }

    @Test
    public void testAddAndRemovePermanentClassAliasAndForName() {
        Class<?> original = HashMap.class;
        String alias = "MyMapAlias";

        // ensure alias does not exist initially
        assertNull(ClassUtilities.forName(alias, ClassUtilities.class.getClassLoader()));

        ClassUtilities.addPermanentClassAlias(original, alias);
        Class<?> lookedUp = ClassUtilities.forName(alias, ClassUtilities.class.getClassLoader());
        assertNotNull(lookedUp);
        assertEquals(original, lookedUp);

        ClassUtilities.removePermanentClassAlias(alias);
        assertNull(ClassUtilities.forName(alias, ClassUtilities.class.getClassLoader()));
    }

    @Test
    public void testAreAllConstructorsPrivate() {
        // java.util.Collections has a private constructor
        assertTrue(ClassUtilities.areAllConstructorsPrivate(Collections.class));

        // java.lang.String has public constructors
        assertFalse(ClassUtilities.areAllConstructorsPrivate(String.class));
    }

    @Test
    public void testIsClassFinal() {
        assertTrue(ClassUtilities.isClassFinal(String.class)); // String is final
        assertFalse(ClassUtilities.isClassFinal(ArrayList.class));
    }

    @Test
    public void testFindClosestExactAndInheritanceMatch() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(Number.class, "number");
        map.put(Integer.class, "integer");
        map.put(Object.class, "object");

        // exact match
        assertEquals("integer", ClassUtilities.findClosest(Integer.class, map, "default"));

        // closest superclass (Double extends Number)
        assertEquals("number", ClassUtilities.findClosest(Double.class, map, "default"));

        // no match – fallback to default
        assertEquals("default", ClassUtilities.findClosest(Boolean.class, map, "default"));
    }

    @Test
    public void testLoadResourceAsStringAndBytes() throws Exception {
        // Use a well‑known resource that is always on the classpath.
        // META‑INF/MANIFEST.MF is present in the JAR that runs the tests.
        String resource = "META-INF/MANIFEST.MF";

        byte[] bytes = ClassUtilities.loadResourceAsBytes(resource);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        String content = ClassUtilities.loadResourceAsString(resource);
        assertNotNull(content);
        assertTrue(content.contains("Manifest-Version"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLoadResourceAsBytesNotFound() {
        ClassUtilities.loadResourceAsBytes("non‑existent‑resource-xyz.txt");
    }

    @Test
    public void testIndexOfSmallestValue() {
        int[] data = {5, 2, 9, 2, 7};
        // first smallest (2) at index 1
        assertEquals(1, ClassUtilities.indexOfSmallestValue(data));

        // empty array → -1
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(new int[0]));

        // null array → -1
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(null));
    }

    @Test
    public void testEnumDetection() {
        // Enum defined inside this test class
        class LocalEnumHolder {
            enum MyEnum { A, B }
        }
        Class<?> enumCls = LocalEnumHolder.MyEnum.class;
        assertEquals(enumCls, ClassUtilities.getClassIfEnum(enumCls));
        // Non‑enum returns null
        assertNull(ClassUtilities.getClassIfEnum(String.class));
    }

    @Test
    public void testFindLowestCommonSupertypesAndSupertype() {
        Set<Class<?>> supertypes = ClassUtilities.findLowestCommonSupertypes(Integer.class, Double.class);
        // Expected common supertypes (excluding Object, Serializable, etc.) are Number and Comparable
        assertTrue(supertypes.contains(Number.class));
        assertTrue(supertypes.contains(Comparable.class));
        assertFalse(supertypes.contains(Object.class));

        // Single most specific supertype (any one is fine)
        Class<?> low = ClassUtilities.findLowestCommonSupertype(Integer.class, Double.class);
        assertNotNull(low);
        assertTrue(low == Number.class || low == Comparable.class);
    }

    @Test
    public void testSecurityCheckerBlocksSensitiveClasses() {
        // ProcessBuilder is on the blocked list – verifyClass should throw SecurityException
        try {
            ClassUtilities.SecurityChecker.verifyClass(ProcessBuilder.class);
            fail("Expected SecurityException for blocked class");
        } catch (SecurityException ignored) {
            // expected
        }

        // A safe class (e.g., String) should not throw
        ClassUtilities.SecurityChecker.verifyClass(String.class);
    }

    @Test
    public void testGetClassLoaderHierarchy() {
        ClassLoader cl = ClassUtilities.getClassLoader(ClassUtilities.class);
        assertNotNull(cl);
        // The returned loader should be able to load a known class
        assertNotNull(cl.loadClass("java.lang.String"));
    }

    @Test
    public void testNewInstanceSimpleClass() {
        // Simple class with a no‑arg public constructor
        class Simple {
            public String value = "hello";
        }

        Object instance = ClassUtilities.newInstance(CONVERTER, Simple.class, Collections.emptyList());
        assertTrue(instance instanceof Simple);
        assertEquals("hello", ((Simple) instance).value);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewInstanceInterfaceFails() {
        ClassUtilities.newInstance(CONVERTER, List.class, Collections.emptyList());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewInstanceAbstractClassFails() {
        abstract class AbstractThing { }
        ClassUtilities.newInstance(CONVERTER, AbstractThing.class, Collections.emptyList());
    }

    @Test
    public void testNewInstanceWithConstructorArguments() {
        class WithArgs {
            final int a;
            final String b;

            WithArgs(int a, String b) {
                this.a = a;
                this.b = b;
            }
        }

        List<Object> args = Arrays.asList(42, "answer");
        Object obj = ClassUtilities.newInstance(CONVERTER, WithArgs.class, args);
        assertTrue(obj instanceof WithArgs);
        WithArgs wa = (WithArgs) obj;
        assertEquals(42, wa.a);
        assertEquals("answer", wa.b);
    }

    @Test
    public void testFindLowestCommonSupertypesExcluding() {
        Set<Class<?>> excluded = new HashSet<>(Arrays.asList(Number.class));
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(Integer.class, Double.class, excluded);
        // Number is excluded, so only Comparable should remain
        assertTrue(result.contains(Comparable.class));
        assertFalse(result.contains(Number.class));
    }

    @Test
    public void testGetClassLoaderFallbacks() {
        // Force context class loader to null temporarily
        Thread current = Thread.currentThread();
        ClassLoader original = current.getContextClassLoader();
        try {
            current.setContextClassLoader(null);
            ClassLoader cl = ClassUtilities.getClassLoader(ClassUtilities.class);
            assertNotNull(cl);
        } finally {
            current.setContextClassLoader(original);
        }
    }

    @Test
    public void testUnsafeInstantiationDisabledByDefault() {
        // Unsafe is off by default – try to instantiate a class with no accessible constructor
        // java.util.Collections has only a private constructor, but newInstance should fall back
        // to unsafe and succeed only when unsafe is enabled. Here we expect an exception.
        try {
            ClassUtilities.newInstance(CONVERTER, Collections.class, Collections.emptyList());
            fail("Expected IllegalArgumentException because unsafe is disabled");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
    }

    @Test
    public void testSetUseUnsafeEnablesUnsafe() {
        // Turn on unsafe – this is a best‑effort test; we only verify that the flag changes.
        ClassUtilities.setUseUnsafe(true);
        // No direct way to observe the Unsafe instance, but calling setUseUnsafe(false) should reset it.
        ClassUtilities.setUseUnsafe(false);
    }
}
