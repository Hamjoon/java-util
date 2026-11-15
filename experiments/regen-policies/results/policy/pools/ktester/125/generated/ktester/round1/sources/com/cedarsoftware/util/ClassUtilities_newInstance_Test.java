import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import com.cedarsoftware.util.convert.Converter;
import com.cedarsoftware.util.convert.DefaultConverterOptions;

@ExtendWith(MockitoExtension.class)
class ClassUtilities_newInstance_Test {

    private Converter converter;

    class Inner {

        private final String data;

        Inner(String data) {
            this.data = data;
        }

        String getData() {
            return data;
        }
    }

    class NeedsInteger {

        private final Integer number;

        NeedsInteger(Integer number) {
            this.number = number;
        }

        Integer getNumber() {
            return number;
        }
    }

    class NoMatching {

        NoMatching(String s, Integer i) {
        }
    }

    /* -----------------------------------------------------------
       Helper classes used by the various test cases
    ----------------------------------------------------------- */
//     class NeedsInteger {

        private final Integer number;

        NeedsInteger(Integer number) {
            this.number = number;
        }

        Integer getNumber() {
            return number;
        }
    }

//     class NoMatching {

        NoMatching(String s, Integer i) {
        }
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

    // Non‑static inner class – used to verify the inner‑class handling logic
    class InnerClass {

        private final String payload;

        InnerClass(String payload) {
            this.payload = payload;
        }

        String getPayload() {
            return payload;
        }
    }

    // Private constructor – newInstance should still be able to instantiate it
    static class PrivateConstructor {

        private final String secret;

        private PrivateConstructor() {
            this.secret = "hidden";
        }

        String getSecret() {
            return secret;
        }
    }

    // -----------------------------------------------------------------
    static class Outer {

        public Outer() {
        }

        class Inner {

            private final String data;

            Inner(String data) {
                this.data = data;
            }

            String getData() {
                return data;
            }
        }
    }

//     class Inner {

        private final String data;

        Inner(String data) {
            this.data = data;
        }

        String getData() {
            return data;
        }
    }

//     class Inner {

        private final String data;

        Inner(String data) {
            this.data = data;
        }

        String getData() {
            return data;
        }
    }

//     class Inner {

        private final String data;

        Inner(String data) {
            this.data = data;
        }

        String getData() {
            return data;
        }
    }

    // A class whose inner‑class handling will deliberately fail (constructor throws)
    static class FailingOuter {

        private FailingOuter() {
            throw new RuntimeException("fail outer");
        }

        class Inner {

            public Inner() {
            }
        }
    }

//     class Inner {

        public Inner() {
        }
    }

//     class Inner {

        public Inner() {
        }
    }

    static class MultiCtor {

        String value;

        MultiCtor() {
            this.value = "default";
        }

        MultiCtor(String v) {
            this.value = v;
        }
    }

    static class NullCtor {

        Integer num;

        NullCtor() {
            this.num = null;
        }

        NullCtor(Integer n) {
            this.num = n;
        }
    }

    static class Uninstantiable {

        private Uninstantiable() {
            throw new RuntimeException("cannot instantiate");
        }
    }

    static class NoArgClass {

        public NoArgClass() {
        }
    }

    static class OuterStatic {

        static class InnerStatic {

            public InnerStatic() {
            }
        }
    }

    static class InnerStatic {

        public InnerStatic() {
        }
    }

//     static class InnerStatic {

        public InnerStatic() {
        }
    }

    static class VisibilityTest {

        Object createdVia;

        public VisibilityTest(String s) {
            this.createdVia = "public:" + s;
        }

        protected VisibilityTest(Integer i) {
            this.createdVia = "protected:" + i;
        }

        private VisibilityTest() {
            this.createdVia = "private";
        }
    }

    static class PrimitiveCtor {

        int value;

        PrimitiveCtor(int v) {
            this.value = v;
        }
    }

    static class PrimitiveIntCtor {

        int value;

        PrimitiveIntCtor(int v) {
            this.value = v;
        }
    }

    static class ThrowingCtor {

        public ThrowingCtor() {
            throw new RuntimeException("boom");
        }
    }

    static class UnsafeInstantiable {

        private UnsafeInstantiable() {
            throw new RuntimeException("should not be called");
        }

        int dummy = 42;
    }

    static class CollectionCtor {

        List<?> list;

        CollectionCtor(List<?> list) {
            this.list = list;
        }
    }

//     class NeedsInteger {

        private final Integer number;

        NeedsInteger(Integer number) {
            this.number = number;
        }

        Integer getNumber() {
            return number;
        }
    }

//     class NoMatching {

        NoMatching(String s, Integer i) {
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // The real Converter is needed for argument conversion (e.g., String → Integer)
        converter = new Converter(new DefaultConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset unsafe flag after each test that may have changed it
//         ClassUtilities.setUseUnsafe(false);
        converter = null;
    }

//     @Test
//     @DisplayName("newInstance throws IllegalArgumentException when class argument is null")
//     void testNullClass() {
//         IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ClassUtilities.newInstance(converter, null, Collections.emptyList()));
//         assertEquals("Class cannot be null", ex.getMessage());
//     }

//     @Test
//     @DisplayName("newInstance throws when asked to instantiate an interface")
//     void testInterfaceClass() {
//         IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ClassUtilities.newInstance(converter, Runnable.class, Collections.emptyList()));
//         assertTrue(ex.getMessage().contains("Cannot instantiate interface"));
//     }

//     @Test
//     @DisplayName("newInstance throws when asked to instantiate an abstract class")
//     void testAbstractClass() {
//         IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ClassUtilities.newInstance(converter, AbstractList.class, Collections.emptyList()));
//         assertTrue(ex.getMessage().contains("Cannot instantiate abstract class"));
//     }

//     @Test
//     @DisplayName("newInstance rejects prohibited security‑sensitive classes")
//     void testSecurityChecks() {
//         Set<Class<?>> prohibited = Set.of(ProcessBuilder.class, Process.class, ClassLoader.class, Constructor.class, Method.class, Field.class, MethodHandle.class);
//         for (Class<?> prohibitedClass : prohibited) {
//             IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ClassUtilities.newInstance(converter, prohibitedClass, Collections.emptyList()), "Expected rejection for " + prohibitedClass.getName());
//             assertTrue(ex.getMessage().contains("does not allow instantiation of"));
//         }
//         // Special case for ProcessImpl (may not exist on all JDKs)
//         try {
//             Class<?> processImplClass = Class.forName("java.lang.ProcessImpl");
//             IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ClassUtilities.newInstance(converter, processImplClass, Collections.emptyList()));
//             assertTrue(ex.getMessage().contains("java.lang.ProcessImpl"));
//         } catch (ClassNotFoundException ignored) {
//             // If ProcessImpl does not exist, the test is simply skipped.
//         }
//     }

//     @Test
//     @DisplayName("newInstance creates an object with a no‑arg constructor")
//     void testNoArgConstructor() {
//         Object o = ClassUtilities.newInstance(converter, ClassUtilities_newInstance_Test.NoArgConstructor.class, Collections.emptyList());
//         assertInstanceOf(ClassUtilities_newInstance_Test.NoArgConstructor.class, o);
//     }

//     @Test
//     @DisplayName("newInstance uses a single‑argument constructor and passes the supplied value")
//     void testSingleArgConstructor() {
//         List<Object> args = List.of("hello");
//         Object o = ClassUtilities.newInstance(converter, ClassUtilities_newInstance_Test.SingleArgConstructor.class, args);
//         assertInstanceOf(ClassUtilities_newInstance_Test.SingleArgConstructor.class, o);
//         assertEquals("hello", ((SingleArgConstructor) o).getValue());
//     }

//     @Test
//     @DisplayName("newInstance matches arguments to a multi‑argument constructor")
//     void testMultiArgConstructor() {
//         List<Object> args = List.of("text", 7);
//         Object o = ClassUtilities.newInstance(converter, ClassUtilities_newInstance_Test.MultiArgConstructor.class, args);
//         assertInstanceOf(ClassUtilities_newInstance_Test.MultiArgConstructor.class, o);
//         MultiArgConstructor mac = (MultiArgConstructor) o;
//         assertEquals("text", mac.getStr());
//         assertEquals(7, mac.getNum());
//     }

//     @Test
//     @DisplayName("newInstance selects the most appropriate overloaded constructor")
//     void testOverloadedConstructors() {
//         // Provide only a single String argument – should pick (String) constructor
//         List<Object> args = List.of("provided");
//         Object o = ClassUtilities.newInstance(converter, ClassUtilities_newInstance_Test.OverloadedConstructors.class, args);
//         assertInstanceOf(ClassUtilities_newInstance_Test.OverloadedConstructors.class, o);
//         OverloadedConstructors oc = (OverloadedConstructors) o;
//         assertEquals("provided", oc.getValue());
//         assertEquals(0, oc.getNumber());
//         // Provide both arguments – should pick the (String, int) constructor
//         args = List.of("both", 42);
//         o = ClassUtilities.newInstance(converter, ClassUtilities_newInstance_Test.OverloadedConstructors.class, args);
//         oc = (OverloadedConstructors) o;
//         assertEquals("both", oc.getValue());
//         assertEquals(42, oc.getNumber());
//     }

//     @Test
//     @DisplayName("newInstance uses Converter to convert argument types (String → Integer)")
//     void testArgumentConversion() {
//         // Constructor expects Integer, we pass a String that can be converted
//         class NeedsInteger {
// 
//             private final Integer number;
// 
//             NeedsInteger(Integer number) {
//                 this.number = number;
//             }
// 
//             Integer getNumber() {
//                 return number;
//             }
//         }
//         List<Object> args = List.of("123");
//         Object o = ClassUtilities.newInstance(converter, NeedsInteger.class, args);
//         assertInstanceOf(NeedsInteger.class, o);
//         assertEquals(123, ((NeedsInteger) o).getNumber());
//     }

//     @Test
//     @DisplayName("newInstance correctly creates an instance of a non‑static inner class")
//     void testInnerClassInstantiation() {
//         // The outer instance is the test class itself (this)
//         Class<?> innerClass = ClassUtilities_newInstance_Test.InnerClass.class;
//         List<Object> args = List.of("payload");
//         Object o = ClassUtilities.newInstance(converter, innerClass, args);
//         assertInstanceOf(ClassUtilities_newInstance_Test.InnerClass.class, o);
//         assertEquals("payload", ((InnerClass) o).getPayload());
//     }

//     @Test
//     @DisplayName("newInstance can instantiate a class with a private no‑arg constructor")
//     void testPrivateConstructor() {
//         Object o = ClassUtilities.newInstance(converter, ClassUtilities_newInstance_Test.PrivateConstructor.class, Collections.emptyList());
//         assertInstanceOf(ClassUtilities_newInstance_Test.PrivateConstructor.class, o);
//         assertEquals("hidden", ((PrivateConstructor) o).getSecret());
//     }

//     @Test
//     @DisplayName("newInstance throws IllegalArgumentException when no suitable constructor is found")
//     void testNoMatchingConstructor() {
//         class NoMatching {
// 
//             NoMatching(String s, Integer i) {
//             }
//         }
//         List<Object> args = List.of(1, "not-a-number");
//         IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ClassUtilities.newInstance(converter, NoMatching.class, args));
//         assertTrue(ex.getMessage().contains("Unable to instantiate"));
//     }

//     @Test
//     @DisplayName("newInstance falls back to regular constructor selection when inner‑class handling fails")
//     void testInnerClassFallbackWhenEnclosingFails() {
//         // The inner class is non‑static, but its enclosing class throws in its constructor.
//         // The catch block should ignore the failure and newInstance must try the inner class's own constructors.
//         Class<?> inner = ClassUtilities_newInstance_Test.FailingOuter.Inner.class;
//         Object o = ClassUtilities.newInstance(converter, inner, Collections.emptyList());
//         assertInstanceOf(ClassUtilities_newInstance_Test.FailingOuter.Inner.class, o);
//     }

//     @Test
//     @DisplayName("null argumentValues are treated as an empty collection")
//     void testNullArgumentValuesTreatedAsEmpty() {
//         Object o = ClassUtilities.newInstance(converter, ClassUtilities_newInstance_Test.NoArgClass.class, null);
//         assertInstanceOf(ClassUtilities_newInstance_Test.NoArgClass.class, o);
//     }

//     @Test
//     @DisplayName("static inner class is instantiated without needing an enclosing instance")
//     void testStaticInnerClassInstantiation() {
//         Class<?> inner = ClassUtilities_newInstance_Test.OuterStatic.InnerStatic.class;
//         Object o = ClassUtilities.newInstance(converter, inner, Collections.emptyList());
//         assertInstanceOf(ClassUtilities_newInstance_Test.OuterStatic.InnerStatic.class, o);
//     }

//     @Test
//     @DisplayName("newInstance prefers a constructor that matches a provided non‑null argument")
//     void testConstructorSelectionNonNullSuccess() throws Exception {
//         List<Object> args = List.of("provided");
//         Object o = ClassUtilities.newInstance(converter, ClassUtilities_newInstance_Test.MultiCtor.class, args);
//         assertInstanceOf(ClassUtilities_newInstance_Test.MultiCtor.class, o);
//         var f = ClassUtilities_newInstance_Test.MultiCtor.class.getDeclaredField("value");
//         f.setAccessible(true);
//         assertEquals("provided", f.get(o));
//     }

//     @Test
//     @DisplayName("newInstance falls back to a no‑arg constructor when argument list is empty")
//     void testConstructorSelectionFallbackToNull() throws Exception {
//         Object o = ClassUtilities.newInstance(converter, ClassUtilities_newInstance_Test.NullCtor.class, Collections.emptyList());
//         assertInstanceOf(ClassUtilities_newInstance_Test.NullCtor.class, o);
//         var f = ClassUtilities_newInstance_Test.NullCtor.class.getDeclaredField("num");
//         f.setAccessible(true);
//         assertNull(f.get(o));
//     }

//     @Test
//     @DisplayName("newInstance throws when all constructors fail and unsafe allocation is disabled")
//     void testAllConstructorsFail() {
//         IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ClassUtilities.newInstance(converter, Uninstantiable.class, Collections.emptyList()));
//         assertTrue(ex.getMessage().contains("Unable to instantiate"));
//     }

//     @Test
//     @DisplayName("public constructor is chosen over protected/private when arguments match")
//     void testVisibilityOrderingPublicPreferred() {
//         List<Object> args = List.of("publicArg");
//         VisibilityTest vt = (VisibilityTest) ClassUtilities.newInstance(converter, ClassUtilities_newInstance_Test.VisibilityTest.class, args);
//         assertEquals("public:publicArg", vt.createdVia);
//     }

//     @Test
//     @DisplayName("protected constructor is chosen when only a matching protected argument is supplied")
//     void testVisibilityOrderingProtectedPreferred() {
//         List<Object> args = List.of(123);
//         VisibilityTest vt = (VisibilityTest) ClassUtilities.newInstance(converter, ClassUtilities_newInstance_Test.VisibilityTest.class, args);
//         assertEquals("protected:123", vt.createdVia);
//     }

//     @Test
//     @DisplayName("primitive int parameter is satisfied by an Integer argument (auto‑unboxing)")
//     void testPrimitiveParameterHandling() throws Exception {
//         List<Object> args = List.of(5);
//         Object o = ClassUtilities.newInstance(converter, ClassUtilities_newInstance_Test.PrimitiveCtor.class, args);
//         assertInstanceOf(ClassUtilities_newInstance_Test.PrimitiveCtor.class, o);
//         var f = ClassUtilities_newInstance_Test.PrimitiveCtor.class.getDeclaredField("value");
//         f.setAccessible(true);
//         assertEquals(5, f.getInt(o));
//     }

//     @Test
//     @DisplayName("primitive int constructor fails when argument cannot be converted")
//     void testPrimitiveIntTypeMismatch() {
//         List<Object> args = List.of("notANumber");
//         IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ClassUtilities.newInstance(converter, PrimitiveIntCtor.class, args));
//         assertTrue(ex.getMessage().contains("Unable to instantiate"));
//     }

//     @Test
//     @DisplayName("exception from constructor is wrapped in IllegalArgumentException")
//     void testConstructorThrowsException() {
//         IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ClassUtilities.newInstance(converter, ThrowingCtor.class, Collections.emptyList()));
//         assertTrue(ex.getMessage().contains("Unable to instantiate"));
//     }

//     @Test
//     @DisplayName("when all constructors fail, enabling unsafe allocation creates an instance")
//     void testUnsafeInstantiationFallback() {
//         // Enable the unsafe path
//         ClassUtilities.setUseUnsafe(true);
//         Object o = ClassUtilities.newInstance(converter, ClassUtilities_newInstance_Test.UnsafeInstantiable.class, Collections.emptyList());
//         assertInstanceOf(ClassUtilities_newInstance_Test.UnsafeInstantiable.class, o);
//         // Verify that the dummy field is present (means the object was actually allocated)
//         assertEquals(42, ((UnsafeInstantiable) o).dummy);
//     }

//     @Test
//     @DisplayName("unsafe allocation is not used when it is disabled")
//     void testUnsafeDisabledFallbackAfterConstructorException() {
//         // Do NOT enable unsafe – default is disabled
//         IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ClassUtilities.newInstance(converter, UnsafeInstantiable.class, Collections.emptyList()));
//         assertTrue(ex.getMessage().contains("Unable to instantiate"));
//     }

//     @Test
//     @DisplayName("extra arguments beyond the first matching one are ignored")
//     void testExtraArgumentValuesIgnored() {
//         List<Object> args = List.of("first", "second", "third");
//         Object o = ClassUtilities.newInstance(converter, ClassUtilities_newInstance_Test.SingleArgConstructor.class, args);
//         assertInstanceOf(ClassUtilities_newInstance_Test.SingleArgConstructor.class, o);
//         assertEquals("first", ((SingleArgConstructor) o).getValue());
//     }

//     @Test
//     @DisplayName("constructor expecting a List receives a non‑null empty List even if supplied collection is empty")
//     void testCollectionParameterNonNullEmpty() throws Exception {
//         List<Object> args = List.of(Collections.emptyList());
//         Object o = ClassUtilities.newInstance(converter, ClassUtilities_newInstance_Test.CollectionCtor.class, args);
//         assertInstanceOf(ClassUtilities_newInstance_Test.CollectionCtor.class, o);
//         var f = ClassUtilities_newInstance_Test.CollectionCtor.class.getDeclaredField("list");
//         f.setAccessible(true);
//         Object list = f.get(o);
//         assertNotNull(list);
//         assertTrue(list instanceof List);
//         assertTrue(((List<?>) list).isEmpty());
//     }
}