import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ClassUtilities_computeInheritanceDistance_Test {

    class Super implements ILevel1 {
    }

    class Sub extends Super implements ILevel2 {
    }

    class Super implements ILevel1 {
    }

    class Sub extends Super implements ILevel2 {
    }

    // 6️⃣ Interface inheritance distance (including super‑interfaces)
    interface IRoot {
    }

    interface ILevel1 extends IRoot {
    }

    interface ILevel2 extends ILevel1 {
    }

    // Simple class hierarchy: A <- B <- D
    static class A {
    }

    static class B extends A {
    }

    static class D extends B {
    }

    // Interface I and class implementing it directly
    interface I {
    }

    static class ImplI implements I {
    }

    // Interface inheritance chain: RootInterface <- MidInterface <- LeafInterface
    interface RootInterface {
    }

    interface MidInterface extends RootInterface {
    }

    interface LeafInterface extends MidInterface {
    }

    // Sub‑interface extending a base interface
    interface BaseInterface {
    }

    interface SubInterface extends BaseInterface {
    }

    // Class that implements an interface via a superclass
    static class SuperWithI implements I {
    }

    static class SubWithI extends SuperWithI {
    }

    class Super implements ILevel1 {
    }

    class Sub extends Super implements ILevel2 {
    }

    /* -----------------------------------------------------------------
    8️⃣  Parameterized test covering a matrix of scenarios
    -----------------------------------------------------------------
     */
    private static Stream<Arguments> distanceProvider() {
        // source, destination, expected distance
        return Stream.of(Arguments.of(Integer.class, Number.class, 1), Arguments.of(Number.class, Integer.class, -1), Arguments.of(List.class, Collection.class, 1), Arguments.of(ArrayList.class, List.class, 1), Arguments.of(ArrayList.class, Iterable.class, 2), Arguments.of(int.class, Integer.class, 0), Arguments.of(Integer.class, int.class, 0), Arguments.of(int.class, int.class, 0), Arguments.of(Integer.class, Integer.class, 0), Arguments.of(int.class, long.class, -1), Arguments.of(String.class, Object.class, 1), Arguments.of(Object.class, String.class, -1), Arguments.of(ClassUtilities_computeInheritanceDistance_Test.D.class, ClassUtilities_computeInheritanceDistance_Test.A.class, 2), Arguments.of(ClassUtilities_computeInheritanceDistance_Test.ImplI.class, ClassUtilities_computeInheritanceDistance_Test.I.class, 1), Arguments.of(ClassUtilities_computeInheritanceDistance_Test.LeafInterface.class, ClassUtilities_computeInheritanceDistance_Test.RootInterface.class, 2), Arguments.of(ClassUtilities_computeInheritanceDistance_Test.SubWithI.class, ClassUtilities_computeInheritanceDistance_Test.I.class, 2));
    }

    /**
     * Returns {@code true} if the supplied method has a {@code void} return type.
     *
     * @param method
     * 		the method to inspect; must not be {@code null}
     * @return {@code true} if the method returns {@code void}
     */
    public static boolean returnsVoid(Method method) {
        Objects.requireNonNull(method, "method must not be null");
        return method.getReturnType() == void.class;
    }

    /**
     * Locate a method with the given name and parameter count on the supplied class
     * (including its super‑classes and interfaces).  Returns {@code null} if no such
     * method exists.
     *
     * @param clazz
     * 		the class to search; must not be {@code null}
     * @param name
     * 		the method name; must not be {@code null}
     * @param paramCount
     * 		the exact number of parameters the method must have
     * @return the matching {@link Method} or {@code null}
     */
    public static Method findMethod(Class<?> clazz, String name, int paramCount) {
        Objects.requireNonNull(clazz, "clazz must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(parameterTypes, "parameterTypes must not be null");
        try {
            return clazz.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException ignored) {
            // fall back to declared methods (including non‑public)
            Class<?> current = clazz;
            while (current != null) {
                try {
                    Method m = current.getDeclaredMethod(name, parameterTypes);
                    return m;
                } catch (NoSuchMethodException e) {
                    // continue searching super‑classes
                }
                current = current.getSuperclass();
            }
            return null;
        }
    }

    /**
     * Locate a method with the given name and exact parameter types on the supplied
     * class (including its super‑classes and interfaces).  Returns {@code null} if
     * no such method exists.
     *
     * @param clazz
     * 		the class to search; must not be {@code null}
     * @param name
     * 		the method name; must not be {@code null}
     * @param parameterTypes
     * 		the exact parameter types; may be empty but not {@code null}
     * @return the matching {@link Method} or {@code null}
     */
    public static Method findMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
        Objects.requireNonNull(clazz, "clazz must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(parameterTypes, "parameterTypes must not be null");
        try {
            return clazz.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException ignored) {
            // fall back to declared methods (including non‑public)
            Class<?> current = clazz;
            while (current != null) {
                try {
                    Method m = current.getDeclaredMethod(name, parameterTypes);
                    return m;
                } catch (NoSuchMethodException e) {
                    // continue searching super‑classes
                }
                current = current.getSuperclass();
            }
            return null;
        }
    }

    /**
     * Convenience wrapper around {@link #findMethod(Class, String, int)} that returns
     * {@code true} when a matching method is present.
     *
     * @param clazz
     * 		the class to search; must not be {@code null}
     * @param name
     * 		the method name; must not be {@code null}
     * @param paramCount
     * 		the exact number of parameters the method must have
     * @return {@code true} if a matching method exists
     */
    public static boolean isMethodPresent(Class<?> clazz, String name, int paramCount) {
        return findMethod(clazz, name, parameterTypes) != null;
    }

    /**
     * Convenience wrapper around {@link #findMethod(Class, String, Class...)} that
     * returns {@code true} when a matching method is present.
     *
     * @param clazz
     * 		the class to search; must not be {@code null}
     * @param name
     * 		the method name; must not be {@code null}
     * @param parameterTypes
     * 		the exact parameter types; may be empty but not {@code null}
     * @return {@code true} if a matching method exists
     */
    public static boolean isMethodPresent(Class<?> clazz, String name, Class<?>... parameterTypes) {
        return findMethod(clazz, name, parameterTypes) != null;
    }

    /**
     * Returns {@code true} if the supplied method is {@code public}.
     *
     * @param method
     * 		the method to inspect; must not be {@code null}
     * @return {@code true} if the method is {@code public}
     */
    public static boolean isPublic(Method method) {
        Objects.requireNonNull(method, "method must not be null");
        return (method.getModifiers() & Modifier.PUBLIC) != 0;
    }

    /**
     * Returns {@code true} if the supplied method is {@code static}.
     *
     * @param method
     * 		the method to inspect; must not be {@code null}
     * @return {@code true} if the method is {@code static}
     */
    public static boolean isStatic(Method method) {
        Objects.requireNonNull(method, "method must not be null");
        return Modifier.isStatic(method.getModifiers());
    }

    /**
     * Returns {@code true} if the supplied method is {@code abstract}.
     *
     * @param method
     * 		the method to inspect; must not be {@code null}
     * @return {@code true} if the method is {@code abstract}
     */
    public static boolean isAbstract(Method method) {
        Objects.requireNonNull(method, "method must not be null");
        return Modifier.isAbstract(method.getModifiers());
    }

    /**
     * Returns a string representation of the method suitable for debugging.
     *
     * @param method
     * 		the method; must not be {@code null}
     * @return a readable description of the method
     */
    public static String toString(Method method) {
        Objects.requireNonNull(method, "method must not be null");
        StringBuilder sb = new StringBuilder();
        sb.append(method.getDeclaringClass().getName()).append('#').append(method.getName()).append('(');
        Class<?>[] params = method.getParameterTypes();
        for (int i = 0; i < params.length; i++) {
            if (i > 0)
                sb.append(", ");
            sb.append(params[i].getSimpleName());
        }
        sb.append(')');
        return sb.toString();
    }

    // Simple assertEquals wrapper to avoid importing static methods repeatedly
    private static void assertEquals(int expected, int actual) {
        if (expected != actual) {
            throw new AssertionError("expected:<" + expected + "> but was:<" + actual + ">");
        }
    }

    private static void assertEquals(int expected, int actual, java.util.function.Supplier<String> messageSupplier) {
        if (expected != actual) {
            throw new AssertionError(messageSupplier.get() + " – expected:<" + expected + "> but was:<" + actual + ">");
        }
    }

    /* -----------------------------------------------------------------
    1️⃣  Null handling
    -----------------------------------------------------------------
     */
    @Test
    @DisplayName("computeInheritanceDistance returns -1 when either argument is null")
    void testNullArguments() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, String.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(Integer.class, null));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, null));
    }

    /* -----------------------------------------------------------------
    2️⃣  Same class – distance should be zero
    -----------------------------------------------------------------
     */
    @Test
    @DisplayName("computeInheritanceDistance returns 0 when source equals destination")
    void testSameClass() {
        assertEquals(0, ClassUtilities.computeInheritanceDistance(String.class, String.class));
        assertEquals(0, ClassUtilities.computeInheritanceDistance(int.class, int.class));
        assertEquals(0, ClassUtilities.computeInheritanceDistance(List.class, List.class));
    }

    /* -----------------------------------------------------------------
    3️⃣  Primitive ↔ Wrapper handling
    -----------------------------------------------------------------
     */
    @Test
    @DisplayName("primitive to wrapper distance is 1 (e.g., int → Integer)")
    void testPrimitiveToWrapper() {
        assertEquals(1, ClassUtilities.computeInheritanceDistance(int.class, Integer.class));
        assertEquals(1, ClassUtilities.computeInheritanceDistance(Integer.class, int.class));
    }

    @Test
    @DisplayName("different primitive types return -1")
    void testDifferentPrimitives() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(int.class, long.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(double.class, int.class));
    }

    @Test
    @DisplayName("primitive vs non‑primitive non‑wrapper returns -1")
    void testPrimitiveWithNonWrapper() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(int.class, String.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(String.class, int.class));
    }

    /* -----------------------------------------------------------------
    4️⃣  Class inheritance distance (single‑step)
    -----------------------------------------------------------------
     */
    @Test
    @DisplayName("direct superclass distance is 1")
    void testDirectSuperclass() {
        assertEquals(1, ClassUtilities.computeInheritanceDistance(ArrayList.class, AbstractList.class));
        assertEquals(1, ClassUtilities.computeInheritanceDistance(HashMap.class, AbstractMap.class));
    }

    @Test
    @DisplayName("direct interface implementation distance is 1")
    void testDirectInterface() {
        assertEquals(1, ClassUtilities.computeInheritanceDistance(ArrayList.class, List.class));
        assertEquals(1, ClassUtilities.computeInheritanceDistance(HashSet.class, Set.class));
    }

    /* -----------------------------------------------------------------
    5️⃣  Multi‑step inheritance distance (class hierarchy)
    -----------------------------------------------------------------
     */
    @Test
    @DisplayName("multiple inheritance steps are counted correctly")
    void testMultiStepInheritance() {
        // ArrayList → AbstractList → AbstractCollection → Object  (3 steps)
        assertEquals(3, ClassUtilities.computeInheritanceDistance(ArrayList.class, Object.class));
        // LinkedHashMap → HashMap → AbstractMap → Object (3 steps)
        assertEquals(3, ClassUtilities.computeInheritanceDistance(LinkedHashMap.class, Object.class));
    }

    @Test
    @DisplayName("interface super‑interface distance is counted")
    void testInterfaceSuperInterface() {
        assertEquals(1, ClassUtilities.computeInheritanceDistance(ILevel2.class, ILevel1.class));
        assertEquals(2, ClassUtilities.computeInheritanceDistance(ILevel2.class, IRoot.class));
    }

    @Test
    @DisplayName("class implementing an interface via super‑class counts correctly")
    void testClassImplementsInterfaceThroughSuperclass() {
        class Super implements ILevel1 {
        }
        class Sub extends Super implements ILevel2 {
        }
        assertEquals(1, ClassUtilities.computeInheritanceDistance(Sub.class, ILevel2.class));
        // Sub → ILevel1 (via Super) distance 2
        assertEquals(2, ClassUtilities.computeInheritanceDistance(Sub.class, ILevel1.class));
        // Sub → IRoot (via two interfaces) distance 3
        assertEquals(3, ClassUtilities.computeInheritanceDistance(Sub.class, IRoot.class));
    }

    /* -----------------------------------------------------------------
    7️⃣  No relationship returns -1
    -----------------------------------------------------------------
     */
    @Test
    @DisplayName("unrelated classes return -1")
    void testUnrelatedClasses() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(Runnable.class, String.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(Thread.class, Date.class));
    }

    @ParameterizedTest(name = "[{index}] distance({0}, {1}) = {2}")
    @MethodSource("distanceProvider")
    void testParameterizedDistances(Class<?> source, Class<?> destination, int expected) {
        assertEquals(expected, ClassUtilities.computeInheritanceDistance(source, destination), () -> String.format("Expected distance %d between %s and %s", expected, source, destination));
    }

    /* -----------------------------------------------------------------
    3️⃣  Primitive ↔ Wrapper handling
    -----------------------------------------------------------------
     */
    @Test
    @DisplayName("primitive ↔ matching wrapper distance is 0")
    void testPrimitiveWrapperMatching() {
        assertEquals(0, ClassUtilities.computeInheritanceDistance(int.class, Integer.class));
        assertEquals(0, ClassUtilities.computeInheritanceDistance(Integer.class, int.class));
        assertEquals(0, ClassUtilities.computeInheritanceDistance(long.class, Long.class));
        assertEquals(0, ClassUtilities.computeInheritanceDistance(Long.class, long.class));
    }

    @Test
    @DisplayName("custom superclass chain distance (D → A) is 2")
    void testCustomSuperclassDistance() {
        assertEquals(2, ClassUtilities.computeInheritanceDistance(D.class, A.class));
    }

    @Test
    @DisplayName("class implements interface directly (ImplI → I) distance is 1")
    void testInterfaceDistanceDirect() {
        assertEquals(1, ClassUtilities.computeInheritanceDistance(ImplI.class, I.class));
    }

    @Test
    @DisplayName("interface inheritance chain distance (LeafInterface → RootInterface) is 2")
    void testInterfaceInheritanceChain() {
        assertEquals(2, ClassUtilities.computeInheritanceDistance(LeafInterface.class, RootInterface.class));
    }

    @Test
    @DisplayName("sub‑interface extends base interface distance is 1")
    void testDirectInterfaceExtension() {
        assertEquals(1, ClassUtilities.computeInheritanceDistance(SubInterface.class, BaseInterface.class));
    }

    @Test
    @DisplayName("interface reachable via superclass (SubWithI → I) distance is 2")
    void testInterfaceViaSuperclass() {
        // SubWithI → SuperWithI (1) → I (2)
        assertEquals(2, ClassUtilities.computeInheritanceDistance(SubWithI.class, I.class));
    }

    /* -----------------------------------------------------------------
    8️⃣  Array type handling
    -----------------------------------------------------------------
     */
    @Test
    @DisplayName("array type's direct superclass is Object – distance 1")
    void testArraySuperclassDistance() {
        assertEquals(1, ClassUtilities.computeInheritanceDistance(int[].class, Object.class));
    }

    @Test
    @DisplayName("array types implement Serializable and Cloneable – distance 1")
    void testArrayInterfaceDistance() {
        assertEquals(1, ClassUtilities.computeInheritanceDistance(int[].class, Serializable.class));
        assertEquals(1, ClassUtilities.computeInheritanceDistance(int[].class, Cloneable.class));
    }

    @Test
    @DisplayName("interface to Object returns -1 (no superclass chain)")
    void testInterfaceToObject() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(List.class, Object.class));
    }

    @Test
    @DisplayName("wrapper to Object returns -1 (wrapper is not considered a superclass)")
    void testWrapperToObject() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(Integer.class, Object.class));
    }

    @Test
    @DisplayName("primitive to Object returns -1")
    void testPrimitiveToObject() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(int.class, Object.class));
    }

    @Test
    @DisplayName("primitive to array returns -1")
    void testPrimitiveToArray() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(int.class, int[].class));
    }

    @Test
    @DisplayName("wrapper not reachable to Collection returns -1")
    void testWrapperNotReachableToInterface() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(Integer.class, Collection.class));
    }
}
