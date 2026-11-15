package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.io.*;
import java.lang.invoke.*;
import java.lang.reflect.*;

@ExtendWith(MockitoExtension.class)
class CompactLinkedSet_isCaseInsensitive_Test {

    private CompactLinkedSet<String> caseInsensitiveSet;

    /**
     * Helper subclass that exposes the protected isCaseInsensitive() method.
     */
    private static class ExposedSet<E> extends CompactLinkedSet<E> {

        boolean exposeIsCaseInsensitive() {
            return super.isCaseInsensitive();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh instance before each test
        caseInsensitiveSet = new CompactLinkedSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference to help GC (not strictly necessary)
        caseInsensitiveSet = null;
    }

    @Test
    @DisplayName("isCaseInsensitive() should always return true")
    void testIsCaseInsensitiveReturnsTrue() {
        // Given: a newly created CompactLinkedSet (setup in @BeforeEach)
        // When: invoking the protected method via a public wrapper
        // Since isCaseInsensitive() is protected, we create an anonymous subclass
        // that exposes it for testing purposes.
        CompactLinkedSet<String> testSet = new CompactLinkedSet<String>() {

            boolean exposeIsCaseInsensitive() {
                return super.isCaseInsensitive();
            }
        };
        // Then: the method must return true
        assertTrue(testSet.exposeIsCaseInsensitive(), "CompactLinkedSet.isCaseInsensitive() should return true");
    }

    @Test
    @DisplayName("CompactLinkedSet should behave like a Set (basic add/contains) while being case‑insensitive")
    void testSetBehaviorIsCaseInsensitive() {
        // Given
        caseInsensitiveSet.add("Hello");
        caseInsensitiveSet.add("World");
        // When
        // different case
        boolean containsLower = caseInsensitiveSet.contains("hello");
        // different case
        boolean containsUpper = caseInsensitiveSet.contains("WORLD");
        // Then
        // The underlying implementation is case‑insensitive, so both look‑ups must succeed.
        assertTrue(containsLower, "Set should contain 'hello' irrespective of case");
        assertTrue(containsUpper, "Set should contain 'WORLD' irrespective of case");
        // Also verify size remains 2 (no duplicate entries added due to case differences)
        assertEquals(2, caseInsensitiveSet.size(), "Set size should reflect unique case‑insensitive elements");
    }

    @Test
    @DisplayName("Direct invocation: freshly created instance returns true")
    void testDirectInvocationReturnsTrue() {
        ExposedSet<String> set = new ExposedSet<>();
        assertTrue(set.exposeIsCaseInsensitive(), "CompactLinkedSet.isCaseInsensitive() should return true for a new instance");
    }

    @Test
    @DisplayName("Polymorphic invocation via reflection on CompactSet reference")
    void testReflectionViaSuperclassReference() throws Exception {
        CompactLinkedSet<String> set = new CompactLinkedSet<>();
        // Upcast to CompactSet (the superclass) – method is still defined there
        CompactSet<String> superRef = set;
        Method m = CompactSet.class.getDeclaredMethod("isCaseInsensitive");
        m.setAccessible(true);
        boolean result = (boolean) m.invoke(superRef);
        assertTrue(result, "Reflection call through superclass reference must return true");
    }

    @Test
    @DisplayName("Non‑String generic type: Integer set")
    void testNonStringGenericType() {
        ExposedSet<Integer> intSet = new ExposedSet<>();
        assertTrue(intSet.exposeIsCaseInsensitive(), "isCaseInsensitive() must return true regardless of generic type");
    }

    @Test
    @DisplayName("Serialization round‑trip preserves case‑insensitive flag")
    void testSerializationPreservesFlag() throws IOException, ClassNotFoundException {
        // Populate set
        caseInsensitiveSet.add("Alpha");
        caseInsensitiveSet.add("Beta");
        // Serialize
        byte[] data;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(caseInsensitiveSet);
            data = bos.toByteArray();
        }
        // Deserialize
        CompactLinkedSet<String> deserialized;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(data))) {
            deserialized = (CompactLinkedSet<String>) in.readObject();
        }
        // Expose method via subclass
        ExposedSet<String> wrapper = new ExposedSet<>();
        // copy internal state (the deserialized instance) into wrapper using reflection
        // Since the flag is hard‑coded, we can simply invoke on deserialized instance
        Method m = CompactSet.class.getDeclaredMethod("isCaseInsensitive");
        m.setAccessible(true);
        boolean flag = (boolean) m.invoke(deserialized);
        assertTrue(flag, "Deserialized instance must still report case‑insensitive = true");
        // also ensure elements survived
        assertTrue(deserialized.contains("alpha"));
        assertEquals(2, deserialized.size());
    }

    @Test
    @DisplayName("Anonymous subclass without overriding retains true")
    void testAnonymousSubclass() {
        CompactLinkedSet<Object> anon = new CompactLinkedSet<>() {
        };
        // expose via reflection
        try {
            Method m = CompactSet.class.getDeclaredMethod("isCaseInsensitive");
            m.setAccessible(true);
            assertTrue((boolean) m.invoke(anon), "Anonymous subclass should inherit true result");
        } catch (Exception e) {
            fail("Reflection failed: " + e);
        }
    }

    @Test
    @DisplayName("MethodHandle invocation returns true")
    void testMethodHandleInvocation() throws Throwable {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodHandle mh = lookup.findVirtual(CompactLinkedSet.class, "isCaseInsensitive", MethodType.methodType(boolean.class));
        boolean result = (boolean) mh.invoke(caseInsensitiveSet);
        assertTrue(result, "MethodHandle should return true");
    }

    @Test
    @DisplayName("Reflection on null instance throws NullPointerException")
    void testReflectionOnNullInstance() throws Exception {
        Method m = CompactSet.class.getDeclaredMethod("isCaseInsensitive");
        m.setAccessible(true);
        assertThrows(NullPointerException.class, () -> m.invoke(null), "Invoking isCaseInsensitive on null should throw NPE");
    }

    @Test
    @DisplayName("Reflection without setAccessible throws IllegalAccessException")
    void testReflectionWithoutSetAccessible() throws Exception {
        Method m = CompactSet.class.getDeclaredMethod("isCaseInsensitive");
        // Do NOT call setAccessible(true)
        assertThrows(IllegalAccessException.class, () -> m.invoke(caseInsensitiveSet), "Reflective call without setAccessible must fail");
    }

    @Test
    @DisplayName("Reflection with extra argument throws IllegalArgumentException")
    void testReflectionWithExtraArgument() throws Exception {
        Method m = CompactSet.class.getDeclaredMethod("isCaseInsensitive");
        m.setAccessible(true);
        assertThrows(IllegalArgumentException.class, () -> m.invoke(caseInsensitiveSet, 42), "Providing an unexpected argument should raise IllegalArgumentException");
    }

    @Test
    @DisplayName("MethodHandle with wrong return type throws WrongMethodTypeException")
    void testMethodHandleWrongReturnType() throws Throwable {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        // Request a handle that returns String instead of boolean
        MethodType wrongType = MethodType.methodType(String.class);
        assertThrows(WrongMethodTypeException.class, () -> lookup.findVirtual(CompactLinkedSet.class, "isCaseInsensitive", wrongType), "Finding a handle with mismatched return type must fail");
    }

    @Test
    @DisplayName("MethodHandle invoked on null instance throws NullPointerException")
    void testMethodHandleOnNullInstance() throws Throwable {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodHandle mh = lookup.findVirtual(CompactLinkedSet.class, "isCaseInsensitive", MethodType.methodType(boolean.class));
        assertThrows(NullPointerException.class, () -> mh.invoke((Object) null), "Invoking MethodHandle with null receiver must throw NPE");
    }
}
