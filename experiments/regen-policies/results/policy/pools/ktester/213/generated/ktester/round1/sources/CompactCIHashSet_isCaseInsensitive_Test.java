package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Collections;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.io.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class CompactCIHashSet_isCaseInsensitive_Test {

    @Test
    void isCaseInsensitive_returnsTrue_forDefaultConstructor() {
        CompactCIHashSet<String> set = new CompactCIHashSet<>();
        assertTrue(set.isCaseInsensitive(), "CompactCIHashSet should report case‑insensitive behavior via isCaseInsensitive()");
    }

    @Test
    void isCaseInsensitive_returnsTrue_forCollectionConstructor() {
        CompactCIHashSet<String> set = new CompactCIHashSet<>(Collections.singleton("example"));
        assertTrue(set.isCaseInsensitive(), "CompactCIHashSet constructed with a collection should also report case‑insensitive behavior");
    }

    @Test
    void reflectionCall_returnsTrue() throws Exception {
        CompactCIHashSet<String> set = new CompactCIHashSet<>();
        Method m = CompactCIHashSet.class.getDeclaredMethod("isCaseInsensitive");
        // The method is protected but in the same package; no need for setAccessible(true)
        boolean result = (boolean) m.invoke(set);
        assertTrue(result, "Reflection call should return true");
    }

    @Test
    void reflectionViaSuperclassReference_returnsTrue() throws Exception {
        CompactSet<String> superRef = new CompactCIHashSet<>();
        Method m = CompactCIHashSet.class.getDeclaredMethod("isCaseInsensitive");
        boolean result = (boolean) m.invoke(superRef);
        assertTrue(result, "Invocation via superclass reference must still return true");
    }

    @Test
    void reflectionNullInstance_throwsNullPointerException() throws Exception {
        Method m = CompactCIHashSet.class.getDeclaredMethod("isCaseInsensitive");
        assertThrows(NullPointerException.class, () -> m.invoke(null), "Invoking via reflection on a null instance should throw NullPointerException");
    }

    @Test
    void reflectionWrongMethodName_throwsNoSuchMethodException() {
        assertThrows(NoSuchMethodException.class, () -> CompactCIHashSet.class.getDeclaredMethod("isCaseInsensitiv"), "Requesting a miss‑spelled method name must raise NoSuchMethodException");
    }

    @Test
    void reflectionIllegalAccess_withoutSetAccessible_throwsIllegalAccessException() throws Exception {
        // Force illegal access by using a different lookup class (java.lang.Object) via MethodHandles,
        // then fallback to reflection which will still throw IllegalAccessException because the
        // Method object is not made accessible and the caller is not in the same package hierarchy.
        Method m = CompactCIHashSet.class.getDeclaredMethod("isCaseInsensitive");
        // Explicitly ensure the method is not accessible
        m.setAccessible(false);
        // Attempt to invoke from a different class (Object) using reflection
        // The JVM checks access based on the caller's class; we simulate this by using
        // MethodHandles.lookup() from Object and then invoking via reflection.
        assertThrows(IllegalAccessException.class, () -> {
            // The following line runs in the context of this test class, which is the same package,
            // so we artificially trigger the exception by using MethodHandles to obtain a
            // MethodHandle that we then try to invoke reflectively.
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodHandle handle = lookup.unreflect(m);
            handle.invoke(new CompactCIHashSet<>());
        }, "Reflective invocation without setAccessible(true) should throw IllegalAccessException");
    }

    @Test
    void reflectionInvocationTargetException_wrappedException() throws Exception {
        CompactCIHashSet<Integer> anonymous = new CompactCIHashSet<Integer>() {

            @Override
            protected boolean isCaseInsensitive() {
                throw new RuntimeException("forced");
            }
        };
        Method m = CompactCIHashSet.class.getDeclaredMethod("isCaseInsensitive");
        m.setAccessible(true);
        InvocationTargetException ite = assertThrows(InvocationTargetException.class, () -> m.invoke(anonymous), "InvocationTargetException should wrap the RuntimeException thrown by the subclass");
        assertTrue(ite.getCause() instanceof RuntimeException);
        assertEquals("forced", ite.getCause().getMessage());
    }

    @Test
    void methodHandleCall_returnsTrue() throws Throwable {
        CompactCIHashSet<String> set = new CompactCIHashSet<>();
        // same package, allowed
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodHandle mh = lookup.findVirtual(CompactCIHashSet.class, "isCaseInsensitive", MethodType.methodType(boolean.class));
        boolean result = (boolean) mh.invoke(set);
        assertTrue(result, "MethodHandle invocation should return true");
    }

    @Test
    void methodHandleIllegalAccess_throwsIllegalAccessException() {
        // Attempt to obtain a MethodHandle from a lookup that does NOT have access rights.
        MethodHandles.Lookup illegalLookup = MethodHandles.privateLookupIn(Object.class, MethodHandles.lookup());
        assertThrows(IllegalAccessException.class, () -> illegalLookup.findVirtual(CompactCIHashSet.class, "isCaseInsensitive", MethodType.methodType(boolean.class)), "MethodHandle lookup from unrelated class should fail with IllegalAccessException");
    }

    @Test
    void afterModifications_isCaseInsensitiveRemainsTrue() {
        CompactCIHashSet<String> set = new CompactCIHashSet<>();
        set.add("Hello");
        set.add("world");
        set.add(null);
        // duplicate in different case
        set.add("HeLLo");
        set.remove("world");
        set.remove(null);
        assertTrue(set.isCaseInsensitive(), "isCaseInsensitive must stay true regardless of set mutations");
    }

    @Test
    void afterSerialization_isCaseInsensitiveRemainsTrue() throws IOException, ClassNotFoundException {
        CompactCIHashSet<String> original = new CompactCIHashSet<>();
        original.add("One");
        original.add("Two");
        // Serialize
        byte[] bytes;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(original);
            out.flush();
            bytes = bos.toByteArray();
        }
        // Deserialize
        CompactCIHashSet<String> deserialized;
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            ObjectInputStream in = new ObjectInputStream(bis)) {
            @SuppressWarnings("unchecked")
            CompactCIHashSet<String> tmp = (CompactCIHashSet<String>) in.readObject();
            deserialized = tmp;
        }
        assertTrue(deserialized.isCaseInsensitive(), "Deserialized instance must still report case‑insensitive behavior");
    }

    @Test
    void nonStringGenericType_isCaseInsensitiveTrue() {
        CompactCIHashSet<Integer> intSet = new CompactCIHashSet<>();
        intSet.add(1);
        intSet.add(2);
        assertTrue(intSet.isCaseInsensitive(), "The method should return true regardless of the generic type");
    }

    @Test
    @Disabled("SecurityManager is deprecated and often disabled; enable only on JVMs that support it")
    void securityManagerRestriction_throwsSecurityException() throws Exception {
        // Install a SecurityManager that denies reflective access to protected members.
        SecurityManager original = System.getSecurityManager();
        System.setSecurityManager(new SecurityManager() {

            @Override
            public void checkPermission(java.security.Permission perm) {
                // Allow everything except reflectively setting accessible
                if (perm instanceof ReflectPermission && "suppressAccessChecks".equals(perm.getName())) {
                    throw new SecurityException("Reflective access denied");
                }
            }
        });
        try {
            CompactCIHashSet<String> set = new CompactCIHashSet<>();
            Method m = CompactCIHashSet.class.getDeclaredMethod("isCaseInsensitive");
            // This should trigger a SecurityException when we try to make it accessible.
            assertThrows(SecurityException.class, () -> {
                m.setAccessible(true);
                m.invoke(set);
            });
        } finally {
            System.setSecurityManager(original);
        }
    }
}
