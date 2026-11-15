package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Constructor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getConstructor_Test {

    /**
     * Simple class with a public no‑arg constructor.
     */
    public static class NoArgClass {

        public NoArgClass() {
        }
    }

    /**
     * Simple class with a public constructor that takes a single {@code int}.
     */
    public static class IntArgClass {

        public IntArgClass(int size) {
        }
    }

    /**
     * Class with a public constructor that takes a {@code char[]} argument.
     */
    public static final class CharArrayCtor {

        public CharArrayCtor(char[] data) {
        }
    }

    /**
     * Class with a public constructor that takes a {@code byte[]} argument.
     */
    public static final class ByteArrayCtor {

        public ByteArrayCtor(byte[] data) {
        }
    }

    /**
     * Class used to provoke a {@link SecurityException}.
     */
    public static final class SecurityRestricted {

        // The constructor is public, but we will install a SecurityManager that denies
        // reflective access to it, causing Class.getConstructor to throw SecurityException.
        public SecurityRestricted() {
        }
    }

    /* ----------------------------------------------------------------------
     *  Utility holder for parameterized arguments.
     * ---------------------------------------------------------------------- */
    private static class ArgumentsHolder {

        final Class<?> clazz;

        final Class<?>[] paramTypes;

        // for documentation only
        final String expectedClassName;

        // for documentation only
        final String expectedParamDesc;

        ArgumentsHolder(Class<?> clazz, Class<?>[] paramTypes, String expectedClassName, String expectedParamDesc) {
            this.clazz = clazz;
            this.paramTypes = paramTypes;
            this.expectedClassName = expectedClassName;
            this.expectedParamDesc = expectedParamDesc;
        }

        @Override
        public String toString() {
            return clazz.getName() + "(" + (paramTypes.length == 0 ? "" : paramTypes[0].getName()) + ")";
        }
    }

    /**
     * Parameters for successful retrieval and caching tests.
     */
    static Stream<ArgumentsHolder> successfulRetrievalProvider() {
        return Stream.of(// String(char[])
        new ArgumentsHolder(String.class, new Class<?>[] { char[].class }, "java.lang.String", "char[]"), // StringBuilder() – no‑arg
        new ArgumentsHolder(StringBuilder.class, new Class<?>[0], "java.lang.StringBuilder", ""), // Integer(int) – primitive int
        new ArgumentsHolder(Integer.class, new Class<?>[] { int.class }, "java.lang.Integer", "int"));
    }

    /**
     * Parameters for distinct‑key caching tests (different signatures).
     */
    static Stream<ArgumentsHolder> distinctSignatureProvider() {
        return Stream.of(// String(byte[])
        new ArgumentsHolder(String.class, new Class<?>[] { byte[].class }, "java.lang.String", "byte[]"), // String(char[])
        new ArgumentsHolder(String.class, new Class<?>[] { char[].class }, "java.lang.String", "char[]"));
    }

    /**
     * Parameters for cases that must result in IllegalArgumentException.
     */
    static Stream<ArgumentsHolder> illegalArgumentProvider() {
        return Stream.of(// ArrayList(String) – no such ctor
        new ArgumentsHolder(ArrayList.class, new Class<?>[] { String.class }, null, null), // Primitive class argument (int.class) – no constructors
        new ArgumentsHolder(int.class, new Class<?>[0], null, null), // Array class argument (int[].class) – no public ctor
        new ArgumentsHolder(int[].class, new Class<?>[0], null, null));
    }

    @Test
    void getConstructor_returnsPublicNoArgConstructor() {
        // GIVEN
        Class<?> targetClass = NoArgClass.class;
        // no parameters
        Class<?>[] paramTypes = new Class<?>[0];
        // WHEN
        Constructor<?> ctor = ReflectionUtils.getConstructor(targetClass, paramTypes);
        // THEN
        assertNotNull(ctor, "Constructor should not be null");
        assertEquals(targetClass, ctor.getDeclaringClass(), "Constructor should belong to the requested class");
        assertEquals(0, ctor.getParameterCount(), "Constructor should have zero parameters");
    }

    @Test
    void getConstructor_returnsPublicIntArgConstructor() {
        // GIVEN
        Class<?> targetClass = IntArgClass.class;
        Class<?>[] paramTypes = new Class<?>[] { int.class };
        // WHEN
        Constructor<?> ctor = ReflectionUtils.getConstructor(targetClass, paramTypes);
        // THEN
        assertNotNull(ctor, "Constructor should not be null");
        assertEquals(targetClass, ctor.getDeclaringClass(), "Constructor should belong to the requested class");
        assertEquals(1, ctor.getParameterCount(), "Constructor should have exactly one parameter");
        assertEquals(int.class, ctor.getParameterTypes()[0], "Parameter type should be int");
    }

    @Test
    void getConstructor_throwsIllegalArgumentExceptionWhenConstructorMissing() {
        // GIVEN
        Class<?> targetClass = NoArgClass.class;
        // No such ctor
        Class<?>[] paramTypes = new Class<?>[] { String.class };
        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getConstructor(targetClass, paramTypes), "Expected IllegalArgumentException for missing constructor");
        assertTrue(ex.getMessage().contains("did not exist"), "Exception message should indicate missing constructor");
        assertNotNull(ex.getCause(), "Cause of IllegalArgumentException should be NoSuchMethodException");
        assertTrue(ex.getCause() instanceof NoSuchMethodException, "Cause should be NoSuchMethodException");
    }

    @ParameterizedTest(name = "[{index}] getConstructor returns expected ctor for {0}")
    @MethodSource("successfulRetrievalProvider")
    void getConstructor_successfulLookupAndCaching(ArgumentsHolder holder) {
        // First call – should locate the constructor via reflection.
        Constructor<?> first = ReflectionUtils.getConstructor(holder.clazz, holder.paramTypes);
        assertNotNull(first, "Constructor should be found on first call");
        assertEquals(holder.clazz, first.getDeclaringClass(), "Declaring class must match requested class");
        assertArrayEquals(holder.paramTypes, first.getParameterTypes(), "Parameter types must match the requested signature");
        // Second call – must return the *same* Constructor instance (cached).
        Constructor<?> second = ReflectionUtils.getConstructor(holder.clazz, holder.paramTypes);
        assertSame(first, second, "Subsequent call should return cached Constructor instance");
    }

    @Test
    void getConstructor_nullClazz_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ReflectionUtils.getConstructor(null, new Class<?>[0]), "Passing null for clazz should result in NPE");
    }

    @Test
    void getConstructor_nullParameterTypesArray_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ReflectionUtils.getConstructor(String.class, null), "Passing null for parameterTypes var‑args should result in NPE");
    }

    @Test
    void getConstructor_nullElementInsideParameterTypes_throwsNullPointerException() {
        Class<?>[] types = new Class<?>[] { null };
        assertThrows(NullPointerException.class, () -> ReflectionUtils.getConstructor(String.class, types), "Null element inside parameterTypes should cause NPE");
    }

    @ParameterizedTest(name = "[{index}] distinct signatures produce distinct cache entries")
    @MethodSource("distinctSignatureProvider")
    void getConstructor_distinctSignaturesSeparateCache(ArgumentsHolder holder) {
        Constructor<?> ctor = ReflectionUtils.getConstructor(holder.clazz, holder.paramTypes);
        assertNotNull(ctor, "Constructor must be found");
        // Verify that the key is indeed distinct by checking that a different signature
        // yields a different Constructor instance.
        Constructor<?> other = ReflectionUtils.getConstructor(String.class, new Class<?>[] { char[].class });
        if (!holder.paramTypes[0].equals(char[].class)) {
            assertNotSame(other, ctor, "Different parameter signatures must not share the same cached Constructor");
        }
    }

    @ParameterizedTest(name = "[{index}] illegal argument cases")
    @MethodSource("illegalArgumentProvider")
    void getConstructor_illegalArgumentExceptionWrapped(ArgumentsHolder holder) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getConstructor(holder.clazz, holder.paramTypes), "Expected IllegalArgumentException for class " + holder.clazz);
        assertNotNull(ex.getCause(), "Cause must be present");
        assertTrue(ex.getCause() instanceof NoSuchMethodException, "Cause should be NoSuchMethodException");
    }

    @Test
    @Disabled("Requires a SecurityManager that denies reflective access – not reliably testable in modern JVMs")
    void getConstructor_securityExceptionPropagates() {
        // In a JVM with a SecurityManager that throws SecurityException when
        // attempting to reflectively obtain the public constructor, the method
        // should let the exception bubble up unchanged.
        // This test is disabled because configuring such a SecurityManager is
        // environment‑specific and may be prohibited under the current test runner.
        assertThrows(SecurityException.class, () -> ReflectionUtils.getConstructor(SecurityRestricted.class, new Class<?>[0]), "SecurityException should propagate unchanged");
    }
}
