import com.cedarsoftware.util.convert.Converter;
import com.cedarsoftware.util.convert.DefaultConverterOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.ReflectPermission;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClassUtilities_newInstance_Test {

    private Converter converter;

    class NoAccessibleCtor {

        private NoAccessibleCtor() {
        }
    }

    class PojoWithStringCtor {

        private final String value;

        public PojoWithStringCtor(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    class MultiCtor {

        public MultiCtor(String trigger) {
            throw new RuntimeException("boom");
        }

        public MultiCtor() {
            /* works */
        }
    }

    /* --------------------------------------------------------------
       Helper classes used in the tests
    -------------------------------------------------------------- */
    static class MyProcessBuilder extends ProcessBuilder {

        public MyProcessBuilder() {
            super();
        }
    }

    static class MyMethodHandle extends MethodHandle {

        protected MyMethodHandle() {
            super();
        }
    }

    static class ProcessImpl extends Process {

        @Override
        public void destroy() {
        }

        @Override
        public Process destroyForcibly() {
            return this;
        }

        @Override
        public boolean isAlive() {
            return false;
        }

        @Override
        public int exitValue() {
            return 0;
        }

        @Override
        public long pid() {
            return 0L;
        }

        @Override
        public int waitFor() throws InterruptedException {
            return 0;
        }

        @Override
        public InputStream getInputStream() {
            return null;
        }

        @Override
        public OutputStream getOutputStream() {
            return null;
        }

        @Override
        public InputStream getErrorStream() {
            return null;
        }
    }

    interface TestInterface {
    }

    abstract static class AbstractTestClass {
    }

    static class NoArgConstructor {

        public NoArgConstructor() {
        }
    }

    static class SingleArgConstructor {

        private final String value;

        public SingleArgConstructor(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    static class MultiArgConstructor {

        private final String str;

        private final int num;

        public MultiArgConstructor(String str, int num) {
            this.str = str;
            this.num = num;
        }

        public String getStr() {
            return str;
        }

        public int getNum() {
            return num;
        }
    }

    static class OverloadedConstructors {

        private final String value;

        private final int number;

        public OverloadedConstructors() {
            this("default", 0);
        }

        public OverloadedConstructors(String value) {
            this(value, 0);
        }

        public OverloadedConstructors(String value, int number) {
            this.value = value;
            this.number = number;
        }

        public String getValue() {
            return value;
        }

        public int getNumber() {
            return number;
        }
    }

    private static class PrivateConstructor {

        private final String secret;

        private PrivateConstructor(String secret) {
            this.secret = secret;
        }

        public String getSecret() {
            return secret;
        }
    }

    // Non‑static inner class – used to verify the special inner‑class handling
    class InnerClass {

        final String payload;

        InnerClass(String payload) {
            this.payload = payload;
        }
    }

    // Inner class whose reflective ctor throws – used to test the fallback path
    class BadInner {

        BadInner() {
            throw new RuntimeException("synthetic ctor failure");
        }

        public BadInner(String dummy) {
        }
    }

    static class SingleArgPojo {

        private final String value;

        public SingleArgPojo(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    static class TwoArgPojo {

        private final String first;

        private final Integer second;

        public TwoArgPojo(String first, Integer second) {
            this.first = first;
            this.second = second;
        }

        public String getFirst() {
            return first;
        }

        public Integer getSecond() {
            return second;
        }
    }

    static class IntegerHolder {

        private final Integer value;

        public IntegerHolder(Integer value) {
            this.value = value;
        }

        public Integer getValue() {
            return value;
        }
    }

    static class ThrowingError {

        public ThrowingError() {
            throw new AssertionError("boom");
        }
    }

    static class InaccessibleConstructorClass {

        InaccessibleConstructorClass() {
        }
    }

    static class VisibilityTest {

        public VisibilityTest() {
            throw new RuntimeException("public ctor fails");
        }

        protected VisibilityTest(int ignored) {
            /* succeeds */
        }

        private VisibilityTest(String ignored) {
            /* never used */
        }
    }

    static class PrivateNoCtor {

        private PrivateNoCtor() {
            throw new RuntimeException("always fails");
        }
    }

    static class Outer {

        static class StaticNested {

            public StaticNested() {
            }
        }

        class BadInnerClass {

            BadInnerClass() {
                throw new RuntimeException("synthetic ctor failure");
            }

            public BadInnerClass(String dummy) {
            }
        }
    }

    static class StaticNested {

        public StaticNested() {
        }
    }

    class BadInnerClass {

        BadInnerClass() {
            throw new RuntimeException("synthetic ctor failure");
        }

        public BadInnerClass(String dummy) {
        }
    }

    static class StaticNested {

        public StaticNested() {
        }
    }

    class BadInnerClass {

        BadInnerClass() {
            throw new RuntimeException("synthetic ctor failure");
        }

        public BadInnerClass(String dummy) {
        }
    }

    class NoAccessibleCtor {

        private NoAccessibleCtor() {
        }
    }

    class PojoWithStringCtor {

        private final String value;

        public PojoWithStringCtor(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    class MultiCtor {

        public MultiCtor(String trigger) {
            throw new RuntimeException("boom");
        }

        public MultiCtor() {
            /* works */
        }
    }

    // no‑arg ctor works
    @BeforeEach
    void setupBeforeEach() {
        // The Converter is a real instance – we do not mock it because
        // ClassUtilities.newInstance relies on its conversion capabilities.
        converter = new Converter(new DefaultConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset any security manager that a test may have installed.
        System.setSecurityManager(null);
    }

    /* --------------------------------------------------------------
    Tests for argument validation and security checks
    --------------------------------------------------------------
     */
    @Test
    @DisplayName("newInstance(null, …) throws IllegalArgumentException")
    void testNewInstance_NullClass() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ClassUtilities.newInstance(converter, null, Collections.emptyList()));
        assertEquals("Class cannot be null", ex.getMessage());
    }

    @Test
    @DisplayName("newInstance(interface, …) throws IllegalArgumentException")
    void testNewInstance_Interface() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ClassUtilities.newInstance(converter, TestInterface.class, Collections.emptyList()));
        assertTrue(ex.getMessage().contains("Cannot instantiate interface"));
    }

    @Test
    @DisplayName("newInstance(abstract class, …) throws IllegalArgumentException")
    void testNewInstance_AbstractClass() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ClassUtilities.newInstance(converter, AbstractTestClass.class, Collections.emptyList()));
        assertTrue(ex.getMessage().contains("Cannot instantiate abstract class"));
    }

    @Test
    @DisplayName("newInstance(security‑restricted class, …) throws IllegalArgumentException")
    void testNewInstance_SecurityCheck() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ClassUtilities.newInstance(converter, ProcessBuilder.class, Collections.emptyList()));
        assertTrue(ex.getMessage().contains("json-io does not allow instantiation"));
    }

    @Test
    @DisplayName("newInstance(java.lang.ProcessImpl, …) throws IllegalArgumentException")
    void testNewInstance_ProcessImplCheck() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ClassUtilities.newInstance(converter, java.lang.ProcessImpl.class, Collections.emptyList()));
        assertTrue(ex.getMessage().contains("json-io does not allow instantiation of: java.lang.ProcessImpl"));
    }

    /* --------------------------------------------------------------
    Constructor selection tests
    --------------------------------------------------------------
     */
    @Test
    @DisplayName("Instantiate class with no‑arg constructor")
    void testNewInstance_NoArgConstructor() {
        Object instance = ClassUtilities.newInstance(converter, ClassUtilities_newInstance_Test.NoArgConstructor.class, Collections.emptyList());
        assertInstanceOf(ClassUtilities_newInstance_Test.NoArgConstructor.class, instance);
    }

    @Test
    @DisplayName("Instantiate class with single‑arg constructor using conversion")
    void testNewInstance_SingleArgConstructor() {
        List<Object> args = Collections.singletonList(12345);
        Object instance = ClassUtilities.newInstance(converter, ClassUtilities_newInstance_Test.SingleArgConstructor.class, args);
        assertInstanceOf(ClassUtilities_newInstance_Test.SingleArgConstructor.class, instance);
        assertEquals("12345", ((SingleArgConstructor) instance).getValue());
    }

    @Test
    @DisplayName("Instantiate class with multi‑arg constructor")
    void testNewInstance_MultiArgConstructor() {
        List<Object> args = Arrays.asList("hello", 42);
        Object instance = ClassUtilities.newInstance(converter, ClassUtilities_newInstance_Test.MultiArgConstructor.class, args);
        assertInstanceOf(ClassUtilities_newInstance_Test.MultiArgConstructor.class, instance);
        MultiArgConstructor mac = (MultiArgConstructor) instance;
        assertEquals("hello", mac.getStr());
        assertEquals(42, mac.getNum());
    }

    @Test
    @DisplayName("Overloaded constructors – best match is chosen")
    void testNewInstance_OverloadedConstructors() {
        List<Object> args = Collections.singletonList("onlyOne");
        OverloadedConstructors oc = (OverloadedConstructors) ClassUtilities.newInstance(converter, ClassUtilities_newInstance_Test.OverloadedConstructors.class, args);
        assertEquals("onlyOne", oc.getValue());
        assertEquals(0, oc.getNumber());
        List<Object> twoArgs = Arrays.asList("two", 99);
        OverloadedConstructors oc2 = (OverloadedConstructors) ClassUtilities.newInstance(converter, ClassUtilities_newInstance_Test.OverloadedConstructors.class, twoArgs);
        assertEquals("two", oc2.getValue());
        assertEquals(99, oc2.getNumber());
    }

    @Test
    @DisplayName("Instantiate class with private constructor")
    void testNewInstance_PrivateConstructor() {
        List<Object> args = Collections.singletonList("secret");
        Object instance = ClassUtilities.newInstance(converter, ClassUtilities_newInstance_Test.PrivateConstructor.class, args);
        assertInstanceOf(ClassUtilities_newInstance_Test.PrivateConstructor.class, instance);
        assertEquals("secret", ((PrivateConstructor) instance).getSecret());
    }

    /* --------------------------------------------------------------
    Inner‑class handling test
    --------------------------------------------------------------
     */
    @Test
    @DisplayName("Instantiate a non‑static inner class")
    void testNewInstance_NonStaticInnerClass() {
        Class<?> innerClass = ClassUtilities_newInstance_Test.InnerClass.class;
        List<Object> args = Collections.singletonList("payload");
        Object instance = ClassUtilities.newInstance(converter, innerClass, args);
        assertInstanceOf(ClassUtilities_newInstance_Test.InnerClass.class, instance);
        assertEquals("payload", ((InnerClass) instance).payload);
    }

    /* --------------------------------------------------------------
    Fallback to unsafe instantiation – not directly verifiable,
    but we can assert that a class with no accessible constructor
    still results in a non‑null instance when unsafe is allowed.
    --------------------------------------------------------------
     */
    @Test
    @DisplayName("Unsafe fallback creates instance when no constructors succeed")
    void testNewInstance_UnsafeFallback() {
        class NoAccessibleCtor {

            private NoAccessibleCtor() {
            }
        }
        ClassUtilities.setUseUnsafe(true);
        Object instance = ClassUtilities.newInstance(converter, NoAccessibleCtor.class, Collections.emptyList());
        assertNotNull(instance);
        assertInstanceOf(NoAccessibleCtor.class, instance);
    }

    @Test
    @DisplayName("Security check blocks subclass of ProcessBuilder")
    void testSecurityCheck_Subclass() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ClassUtilities.newInstance(converter, MyProcessBuilder.class, Collections.emptyList()));
        assertTrue(ex.getMessage().contains("json-io does not allow instantiation of: java.lang.ProcessBuilder"));
    }

    @Test
    @DisplayName("Security check blocks subclass of MethodHandle")
    void testSecurityCheck_MethodHandleSubclass() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ClassUtilities.newInstance(converter, MyMethodHandle.class, Collections.emptyList()));
        assertTrue(ex.getMessage().contains("json-io does not allow instantiation of: java.lang.invoke.MethodHandle"));
    }

    @Test
    @DisplayName("Constructor visibility ordering – protected wins after public fails")
    void testNewInstance_VisibilityOrdering() {
        Object instance = ClassUtilities.newInstance(converter, ClassUtilities_newInstance_Test.VisibilityTest.class, Collections.emptyList());
        assertInstanceOf(ClassUtilities_newInstance_Test.VisibilityTest.class, instance);
    }

    @Test
    @DisplayName("Missing arguments are filled with null")
    void testNewInstance_MissingArgumentNullFill() {
        List<Object> args = Collections.singletonList("onlyOne");
        TwoArgPojo pojo = (TwoArgPojo) ClassUtilities.newInstance(converter, ClassUtilities_newInstance_Test.TwoArgPojo.class, args);
        assertEquals("onlyOne", pojo.getFirst());
        assertNull(pojo.getSecond());
    }

    @Test
    @DisplayName("Extra arguments are ignored")
    void testNewInstance_ExtraArgumentsIgnored() {
        List<Object> args = Arrays.asList("value", 123, true);
        SingleArgPojo pojo = (SingleArgPojo) ClassUtilities.newInstance(converter, ClassUtilities_newInstance_Test.SingleArgPojo.class, args);
        assertEquals("value", pojo.getValue());
    }

    @Test
    @DisplayName("Converter usage for type conversion (String → Integer)")
    void testNewInstance_ConverterTypeConversion() {
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.convert("42", Integer.class)).thenReturn(42);
        List<Object> args = Collections.singletonList("42");
        IntegerHolder holder = (IntegerHolder) ClassUtilities.newInstance(mockConverter, ClassUtilities_newInstance_Test.IntegerHolder.class, args);
        assertEquals(42, holder.getValue());
    }

    @Test
    @DisplayName("Failing converter falls back to null argument constructor")
    void testNewInstance_FailingConverterFallback() {
        Converter failingConverter = mock(Converter.class);
        when(failingConverter.convert(any(), any())).thenThrow(new RuntimeException("cannot convert"));
        class PojoWithStringCtor {

            private final String value;

            public PojoWithStringCtor(String value) {
                this.value = value;
            }

            public String getValue() {
                return value;
            }
        }
        List<Object> args = Collections.singletonList("value");
        PojoWithStringCtor pojo = (PojoWithStringCtor) ClassUtilities.newInstance(failingConverter, PojoWithStringCtor.class, args);
        assertNull(pojo.getValue());
    }

    @Test
    @DisplayName("First constructor throws, fallback to second succeeds")
    void testNewInstance_FallbackToNextConstructor() {
        class MultiCtor {

            public MultiCtor(String trigger) {
                throw new RuntimeException("boom");
            }

            public MultiCtor() {
                /* works */
            }
        }
        List<Object> args = Collections.singletonList("trigger");
        MultiCtor instance = (MultiCtor) ClassUtilities.newInstance(converter, MultiCtor.class, args);
        assertNotNull(instance);
    }

    @Test
    @DisplayName("Inner class where reflective ctor fails – fallback to regular ctor")
    void testNewInstance_InnerClassFallback() {
        Class<?> inner = ClassUtilities_newInstance_Test.BadInner.class;
        List<Object> args = Collections.singletonList("dummy");
        Object instance = ClassUtilities.newInstance(converter, inner, args);
        assertInstanceOf(ClassUtilities_newInstance_Test.BadInner.class, instance);
    }

    @Test
    @DisplayName("Instantiate a static nested class")
    void testNewInstance_StaticNestedClass() {
        Object instance = ClassUtilities.newInstance(converter, ClassUtilities_newInstance_Test.Outer.StaticNested.class, Collections.emptyList());
        assertInstanceOf(ClassUtilities_newInstance_Test.Outer.StaticNested.class, instance);
    }

    @Test
    @DisplayName("Unsafe fallback after private constructor throws")
    void testNewInstance_UnsafeAfterThrowingPrivateCtor() {
        ClassUtilities.setUseUnsafe(true);
        Object instance = ClassUtilities.newInstance(converter, ClassUtilities_newInstance_Test.PrivateNoCtor.class, Collections.emptyList());
        assertNotNull(instance);
        assertInstanceOf(ClassUtilities_newInstance_Test.PrivateNoCtor.class, instance);
    }

    /* --------------------------------------------------------------
    Primitive / array handling tests
    --------------------------------------------------------------
     */
    @Test
    @DisplayName("Primitive class handling – IllegalArgumentException")
    void testNewInstance_PrimitiveClass() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ClassUtilities.newInstance(converter, int.class, Collections.emptyList()));
        assertTrue(ex.getMessage().contains("Unable to instantiate: int"));
    }

    @Test
    @DisplayName("Array class handling – IllegalArgumentException")
    void testNewInstance_ArrayClass() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ClassUtilities.newInstance(converter, String[].class, Collections.emptyList()));
        assertTrue(ex.getMessage().contains("Unable to instantiate"));
    }

    /* --------------------------------------------------------------
    Error propagation tests
    --------------------------------------------------------------
     */
    @Test
    @DisplayName("Constructor throws unrecoverable Error – propagates")
    void testNewInstance_ConstructorThrowsError() {
        assertThrows(AssertionError.class, () -> ClassUtilities.newInstance(converter, ThrowingError.class, Collections.emptyList()));
    }

    @Test
    @DisplayName("SecurityException from trySetAccessible propagates")
    void testNewInstance_SecurityExceptionFromSetAccessible() {
        System.setSecurityManager(new SecurityManager() {

            @Override
            public void checkPermission(Permission perm) {
                if (perm instanceof ReflectPermission && "suppressAccessChecks".equals(perm.getName())) {
                    throw new SecurityException("Reflective access denied");
                }
            }
        });
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> ClassUtilities.newInstance(converter, InaccessibleConstructorClass.class, Collections.emptyList()));
        assertTrue((ex.getCause() instanceof SecurityException) || ex.getMessage().contains("Reflective access denied"));
    }
}
