import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ClassUtilities_findLowestCommonSupertypesExcluding_Test {

    /* ---------- Test helper types ---------- */
    interface I1 {
    }

    interface I2 extends I1 {
    }

    interface I3 extends I1 {
    }

    interface TestInterface {
    }

    interface SubInterface extends TestInterface {
    }

    class A implements Cloneable, Serializable {
    }

    class B implements Cloneable, Serializable {
    }

    class LocalA implements Cloneable, Serializable {
    }

    class LocalB implements Cloneable, Serializable {
    }

    /* ---------- Test helper types ---------- */
    interface TestInterface {
    }

    interface SubInterface extends TestInterface {
    }

    interface I1 {
    }

    interface I2 extends I1 {
    }

    interface I3 extends I1 {
    }

    class LocalA implements Cloneable, Serializable {
    }

    class LocalB implements Cloneable, Serializable {
    }

    class LocalA implements Cloneable, Serializable {
    }

    class LocalB implements Cloneable, Serializable {
    }

    private static Stream<Arguments> nullExcludedSetProvider() {
        return Stream.of(Arguments.of(null, String.class, null), Arguments.of(String.class, Integer.class, null), Arguments.of(String.class, String.class, null));
    }

    @Test
    @DisplayName("Both arguments null → empty set")
    void testBothNull() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(null, null, Collections.emptySet());
        assertTrue(result.isEmpty(), "Expected empty set when both classes are null");
    }

    @Test
    @DisplayName("One argument null → empty set")
    void testOneNull() {
        Set<Class<?>> result1 = ClassUtilities.findLowestCommonSupertypesExcluding(String.class, null, Collections.emptySet());
        Set<Class<?>> result2 = ClassUtilities.findLowestCommonSupertypesExcluding(null, Integer.class, Collections.emptySet());
        assertTrue(result1.isEmpty(), "Expected empty set when second class is null");
        assertTrue(result2.isEmpty(), "Expected empty set when first class is null");
    }

    @Test
    @DisplayName("Same class not excluded → singleton set")
    void testSameClassNotExcluded() {
        Set<Class<?>> excluded = Collections.emptySet();
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(List.class, List.class, excluded);
        assertEquals(1, result.size());
        assertTrue(result.contains(List.class));
    }

    @Test
    @DisplayName("Same class excluded → empty set")
    void testSameClassExcluded() {
        Set<Class<?>> excluded = new HashSet<>(Collections.singletonList(List.class));
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(List.class, List.class, excluded);
        assertTrue(result.isEmpty(), "Result should be empty because the class is excluded");
    }

    @Test
    @DisplayName("No common supertype other than Object → empty set")
    void testNoCommonSupertypes() {
        Set<Class<?>> excluded = Collections.emptySet();
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(Collection.class, Map.class, excluded);
        assertTrue(result.isEmpty(), "Collection and Map share only Object, which is filtered out by the algorithm");
    }

    @Test
    @DisplayName("Single lowest common supertype (ArrayList vs LinkedList) → AbstractList")
    void testSingleLowestCommonSupertype() {
        Set<Class<?>> excluded = Collections.emptySet();
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(ArrayList.class, LinkedList.class, excluded);
        assertEquals(1, result.size());
        assertTrue(result.contains(AbstractList.class), "Expected AbstractList as the lowest common supertype");
    }

    @Test
    @DisplayName("Multiple lowest common supertypes (Integer vs Double) → Number")
    void testMultipleCommonSupertypesSingleLowest() {
        Set<Class<?>> excluded = Collections.emptySet();
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(Integer.class, Double.class, excluded);
        assertEquals(1, result.size());
        assertTrue(result.contains(Number.class), "Expected Number as the lowest common supertype");
    }

    @Test
    @DisplayName("Excluding the most specific common type yields remaining lowest supertypes")
    void testExcludingMostSpecificLeavesOthers() {
        Set<Class<?>> excluded = new HashSet<>(Collections.singletonList(Number.class));
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(Integer.class, Double.class, excluded);
        assertEquals(2, result.size());
        assertTrue(result.contains(Comparable.class), "Expected Comparable to be present after exclusion");
        assertTrue(result.contains(Serializable.class), "Expected Serializable to be present after exclusion");
    }

    @Test
    @DisplayName("Interface hierarchy – SubInterface vs TestInterface")
    void testInterfaceHierarchy() {
        Set<Class<?>> excluded = Collections.emptySet();
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(SubInterface.class, TestInterface.class, excluded);
        assertEquals(1, result.size());
        assertTrue(result.contains(TestInterface.class));
    }

    @Test
    @DisplayName("Complex hierarchy with multiple lowest supertypes")
    void testComplexMultipleLowest() {
        class LocalA implements Cloneable, Serializable {
        }
        class LocalB implements Cloneable, Serializable {
        }
        Set<Class<?>> excluded = Collections.emptySet();
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(LocalA.class, LocalB.class, excluded);
        assertEquals(2, result.size());
        assertTrue(result.contains(Cloneable.class));
        assertTrue(result.contains(Serializable.class));
    }

    @ParameterizedTest(name = "{index}: classA={0}, classB={1}, excluded={2}")
    @MethodSource("nullExcludedSetProvider")
    @DisplayName("Null excluded set → NullPointerException")
    void testNullExcludedSetThrowsNPE(Class<?> classA, Class<?> classB, Set<Class<?>> excluded) {
        assertThrows(NullPointerException.class, () -> ClassUtilities.findLowestCommonSupertypesExcluding(classA, classB, excluded), "Expected NullPointerException when excluded set is null");
    }

    @Test
    @DisplayName("Both arguments null with non‑null excluded set → empty set (sanity)")
    void testBothNullWithNonNullExcluded() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(null, null, new HashSet<>());
        assertTrue(result.isEmpty(), "Expected empty set when both classes are null even with non‑null excluded");
    }

    @Test
    @DisplayName("Both classes are interfaces with multiple common super‑interfaces")
    void testInterfaceMultipleCommon() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(I2.class, I3.class, Collections.emptySet());
        assertEquals(1, result.size());
        assertTrue(result.contains(I1.class));
    }

    @Test
    @DisplayName("Excluding Object should not affect result when other common supertypes exist")
    void testExcludeObjectOnly() {
        Set<Class<?>> excluded = new HashSet<>(Collections.singletonList(Object.class));
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(Integer.class, Double.class, excluded);
        assertEquals(1, result.size());
        assertTrue(result.contains(Number.class));
    }
}
