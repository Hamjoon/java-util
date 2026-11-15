package com.cedarsoftware.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class CompactMap_size_Test {

    private CompactMap<String, Integer> map;

    /**
     * Executed before each test – creates a fresh CompactMap instance.
     */
    @BeforeEach
    void setupBeforeEach() {
        map = new CompactMap<>();
    }

    /**
     * Executed after each test – clears the map to help GC (optional).
     */
    @AfterEach
    void teardownAfterEach() {
        map.clear();
        map = null;
    }

    /* --------------------------------------------------------------------- */
    private void setInternalVal(Object newVal) throws Exception {
        Field field = CompactMap.class.getDeclaredField("val");
        field.setAccessible(true);
        field.set(map, newVal);
    }

    /* --------------------------------------------------------------------- */
    private static Stream<Arguments> arrayBranchProvider() {
        return Stream.of(Arguments.of(new Object[0], 0, "zero‑length array"), Arguments.of(new Object[] { "k1", 1 }, 1, "single pair array"), Arguments.of(new Object[] { "k1", 1, "k2", 2 }, 2, "two‑pair array"), Arguments.of(new Object[] { "k1", 1, "extra" }, 1, "odd‑length array"));
    }

    /**
     * Returns {@code true} if the supplied method has a {@code void} return type.
     *
     * @param method the method to examine
     * @return {@code true} if the method returns {@code void}
     */
    public static boolean returnsVoid(Method method) {
        return method != null && method.getReturnType() == void.class;
    }

    /**
     * Find a method with the given name and parameter types on the supplied class.
     * The search includes public methods (including inherited) and declared methods.
     *
     * @param clazz          the class to search
     * @param name           the method name
     * @param parameterTypes the parameter types (may be empty)
     * @return the {@link Method} if found, otherwise {@code null}
     */
    public static Method findMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
        return findMethod(clazz, name, new Class<?>[0]);
    }

    /**
     * Convenience wrapper around {@link #findMethod(Class, String, Class[])}.
     *
     * @param clazz          the class to search
     * @param name           the method name
     * @param parameterTypes the parameter types (may be empty)
     * @return {@code true} if a matching method exists
     */
    public static boolean isMethodPresent(Class<?> clazz, String name, Class<?>... parameterTypes) {
        return findMethod(clazz, name, parameterTypes) != null;
    }

    public static boolean isPublic(Method method) {
        return (method.getModifiers() & java.lang.reflect.Modifier.PUBLIC) != 0;
    }

    public static boolean isStatic(Method method) {
        return (method.getModifiers() & java.lang.reflect.Modifier.STATIC) != 0;
    }

    public static boolean isAbstract(Method method) {
        return (method.getModifiers() & java.lang.reflect.Modifier.ABSTRACT) != 0;
    }

    public static boolean isPublicStatic(Method method) {
        return isPublic(method) && isStatic(method);
    }

    public static boolean isPublicStaticVoidNoArgs(Method method) {
        Method m = findMethod(clazz, methodName);
        return isPublicStaticVoidNoArgs(m);
    }

    // required for the current test suite but are harmless to provide.
    public static boolean isPublicStaticVoidNoArgs(Class<?> clazz, String methodName) {
        Method m = findMethod(clazz, methodName);
        return isPublicStaticVoidNoArgs(m);
    }

    public static Method findMethod(Class<?> clazz, String name) {
        return findMethod(clazz, name, new Class<?>[0]);
    }

    /**
     * Empty map must report size 0.
     */
    @Test
    void size_EmptyMap_ReturnsZero() {
        assertEquals(0, map.size(), "A newly created CompactMap should be empty");
    }

    /**
     * One entry should be reported as size 1 regardless of internal representation.
     */
    @Test
    void size_SingleEntry_ReturnsOne() {
        map.put("one", 1);
        assertEquals(1, map.size(), "Size should be 1 after adding a single entry");
    }

    /**
     * Two entries trigger the internal Object[] representation; size must be 2.
     */
    @Test
    void size_TwoEntries_ReturnsTwo() {
        map.put("one", 1);
        map.put("two", 2);
        assertEquals(2, map.size(), "Size should be 2 after adding two entries");
    }

    /**
     * Adding a few entries (still below the default compactSize of 80) must return the exact count.
     */
    @Test
    void size_UnderCompactSize_ReturnsCorrectCount() {
        int count = 15;
        for (int i = 0; i < count; i++) {
            map.put("k" + i, i);
        }
        assertEquals(count, map.size(), "Size should match the number of inserted entries when below compactSize");
    }

    /**
     * Adding more entries than compactSize forces the map to delegate to a regular Map; size must still be correct.
     */
    @Test
    void size_OverCompactSize_ReturnsCorrectCount() {
        int compactSize = 80;
        int count = compactSize + 10;
        for (int i = 0; i < count; i++) {
            map.put("k" + i, i);
        }
        assertEquals(count, map.size(), "Size should match the number of inserted entries when exceeding compactSize");
    }

    /**
     * Constructor that copies another map must preserve the size of the source map.
     */
    @Test
    void size_ConstructedFromOtherMap_PreservesSize() {
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);
        source.put("b", 2);
        source.put("c", 3);
        CompactMap<String, Integer> copy = new CompactMap<>(source);
        assertEquals(source.size(), copy.size(), "CompactMap created from another map must have the same size");
    }

    /**
     * Using reflection to set {@code val} to a plain Object (neither array nor Map).
     */
    @Test
    void size_ValIsPlainObject_ReturnsOne() throws Exception {
        setInternalVal(new Object());
        assertEquals(1, map.size(), "When internal val is a plain Object, size() should fall through and return 1");
    }

    /**
     * Using reflection to set {@code val} to an Object[] representing two key/value pairs.
     */
    @Test
    void size_ValIsObjectArray_TwoPairs_ReturnsTwo() throws Exception {
        Object[] arr = new Object[] { "k1", 1, "k2", 2 };
        setInternalVal(arr);
        assertEquals(2, map.size(), "Object[] length 4 (>>1 == 2) should result in size 2");
    }

    /**
     * Using reflection to set {@code val} to an Object[] of length 2 (single pair).
     */
    @Test
    void size_ValIsObjectArray_OnePair_ReturnsOne() throws Exception {
        Object[] arr = new Object[] { "k1", 1 };
        setInternalVal(arr);
        assertEquals(1, map.size(), "Object[] length 2 (>>1 == 1) should result in size 1");
    }

    /**
     * Using reflection to set {@code val} to a HashMap containing three entries.
     */
    @Test
    void size_ValIsHashMap_ThreeEntries_ReturnsThree() throws Exception {
        Map<String, Integer> backing = new HashMap<>();
        backing.put("a", 1);
        backing.put("b", 2);
        backing.put("c", 3);
        setInternalVal(backing);
        assertEquals(3, map.size(), "Delegated Map size should be returned unchanged");
    }

    /**
     * Clear after having added entries – size should return to 0.
     */
    @Test
    void size_ClearAfterAdds_ReturnsZero() {
        map.put("x", 10);
        map.put("y", 20);
        map.clear();
        assertEquals(0, map.size(), "clear() must reset internal state to EMPTY_MAP resulting in size 0");
    }

    /**
     * Using reflection to set {@code val} to a zero‑length Object[] – size must be 0.
     */
    @Test
    void size_ZeroLengthObjectArray_ReturnsZero() throws Exception {
        setInternalVal(new Object[0]);
        assertEquals(0, map.size(), "Zero‑length array should yield size 0 (0 >> 1 == 0)");
    }

    /**
     * Using reflection to set {@code val} to an odd‑length Object[] – verifies bit‑shift logic.
     */
    @Test
    void size_OddLengthObjectArray_ReturnsOne() throws Exception {
        Object[] arr = new Object[] { "k1", 1, "extra" };
        setInternalVal(arr);
        assertEquals(1, map.size(), "Odd length 3 >> 1 == 1; size() should reflect this even for malformed state");
    }

    /**
     * Using reflection to set {@code val} to a TreeMap (non‑HashMap) containing four entries.
     */
    @Test
    void size_NonHashMapBacking_TreeMap_ReturnsFour() throws Exception {
        Map<String, Integer> tree = new TreeMap<>();
        tree.put("a", 1);
        tree.put("b", 2);
        tree.put("c", 3);
        tree.put("d", 4);
        setInternalVal(tree);
        assertEquals(4, map.size(), "Any Map implementation should be delegated to; TreeMap size must be returned");
    }

    @Test
    void size_SequentialPuts_TransitionThroughAllRepresentations() {
        map.put("k1", 1);
        assertEquals(1, map.size(), "After first put size should be 1");
        map.put("k2", 2);
        assertEquals(2, map.size(), "After second put size should be 2 (Object[] branch)");
        map.put("k3", 3);
        assertEquals(3, map.size(), "After third put size should be 3 (Map branch)");
    }

    @Test
    void size_Removals_ShrinkBackThroughRepresentations() {
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        assertEquals(3, map.size(), "Pre‑condition: map size 3");
        map.remove("a");
        assertEquals(2, map.size(), "After removing one entry size should be 2 (Object[] branch)");
        map.remove("b");
        assertEquals(1, map.size(), "After removing second entry size should be 1 (single entry)");
        map.remove("c");
        assertEquals(0, map.size(), "After removing final entry size should be 0 (EMPTY_MAP)");
    }

    @Test
    void size_AfterPlus_CombinesTwoMaps() {
        CompactMap<String, Integer> left = new CompactMap<>();
        left.put("a", 1);
        left.put("b", 2);
        CompactMap<String, Integer> right = new CompactMap<>();
        right.put("c", 3);
        right.put("d", 4);
        right.put("e", 5);
        Map<String, Integer> result = left.plus(right);
        assertEquals(5, result.size(), "Result of plus() should contain all entries from both maps (2 + 3 = 5)");
    }

    @Test
    void size_AfterMinus_RemovesSpecifiedKeys() {
        CompactMap<String, Integer> base = new CompactMap<>();
        base.put("x", 10);
        base.put("y", 20);
        base.put("z", 30);
        Map<String, Integer> afterFirstMinus = base.minus("y");
        assertEquals(2, afterFirstMinus.size(), "Minus one existing key should reduce size to 2");
        Map<String, Integer> afterSecondMinus = ((CompactMap<String, Integer>) afterFirstMinus).minus("x");
        afterSecondMinus = ((CompactMap<String, Integer>) afterSecondMinus).minus("z");
        assertEquals(0, afterSecondMinus.size(), "Removing all remaining keys should result in size 0");
    }

    @ParameterizedTest(name = "Array branch [{2}] → expected size {1}")
    @MethodSource("arrayBranchProvider")
    void size_ArrayBranch_VariousLengths(Object[] arr, int expectedSize, String description) throws Exception {
        setInternalVal(arr);
        assertEquals(expectedSize, map.size(), "Array branch with " + description + " should return " + expectedSize);
    }
}
