package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ArrayUtilities_toArray_Test {

    class Animal {
    }

    class Dog extends Animal {
    }

    static Stream<Arguments> normalBehaviourProvider() {
        return Stream.of(Arguments.of(String.class, List.of("alpha", "beta", "gamma"), new String[] { "alpha", "beta", "gamma" }), Arguments.of(Integer.class, List.of(1, 2, 3, 4), new Integer[] { 1, 2, 3, 4 }), Arguments.of(String.class, List.of("first", null, "third"), new String[] { "first", null, "third" }));
    }

    static Stream<Arguments> emptyCollectionProvider() {
        return Stream.of(Arguments.of(String.class), Arguments.of(Object.class));
    }

    static Stream<Arguments> singleElementProvider() {
        return Stream.of(Arguments.of(Long.class, 1234567890123L), Arguments.of(String.class, "only"));
    }

    @Test
    void toArray_WithEmptyCollection_ReturnsEmptyArrayOfRequestedType() {
        // GIVEN
        Collection<String> empty = new ArrayList<>();
        // WHEN
        String[] result = ArrayUtilities.toArray(String.class, empty);
        // THEN
        assertNotNull(result, "Result must not be null");
        assertEquals(0, result.length, "Result array should be empty");
        assertEquals(String.class, result.getClass().getComponentType(), "Component type must match the supplied class");
    }

    @Test
    void toArray_WithStringCollection_ReturnsArrayWithSameElements() {
        // GIVEN
        List<String> source = List.of("alpha", "beta", "gamma");
        // WHEN
        String[] result = ArrayUtilities.toArray(String.class, source);
        // THEN
        assertArrayEquals(new String[] { "alpha", "beta", "gamma" }, result, "Array content must match the collection order");
        assertEquals(String.class, result.getClass().getComponentType(), "Component type must be String");
    }

    @Test
    void toArray_WithIntegerCollection_ReturnsArrayOfInteger() {
        // GIVEN
        Collection<Integer> source = List.of(1, 2, 3, 4);
        // WHEN
        Integer[] result = ArrayUtilities.toArray(Integer.class, source);
        // THEN
        assertArrayEquals(new Integer[] { 1, 2, 3, 4 }, result);
        assertEquals(Integer.class, result.getClass().getComponentType(), "Component type must be Integer");
    }

    @Test
    void toArray_WithNullElements_HandlesNullsCorrectly() {
        // GIVEN
        List<String> source = new ArrayList<>();
        source.add("first");
        source.add(null);
        source.add("last");
        // WHEN
        String[] result = ArrayUtilities.toArray(String.class, source);
        // THEN
        assertEquals(3, result.length);
        assertEquals("first", result[0]);
        assertNull(result[1], "Null element must be preserved");
        assertEquals("last", result[2]);
    }

    @Test
    void toArray_WithSubclassElements_ReturnsArrayOfSuperclassType() {
        // GIVEN
        class Animal {
        }
        class Dog extends Animal {
        }
        List<Dog> dogs = List.of(new Dog(), new Dog());
        // WHEN
        Animal[] result = ArrayUtilities.toArray(Animal.class, dogs);
        // THEN
        assertEquals(2, result.length);
        assertTrue(result[0] instanceof Dog);
        assertTrue(result[1] instanceof Dog);
        assertEquals(Animal.class, result.getClass().getComponentType(), "Component type must be the class supplied (Animal)");
    }

    @Test
    void toArray_DoesNotExposeInternalCollectionChanges() {
        // GIVEN
        List<String> mutable = new ArrayList<>();
        mutable.add("a");
        mutable.add("b");
        // WHEN
        String[] snapshot = ArrayUtilities.toArray(String.class, mutable);
        // mutate the original collection after conversion
        mutable.add("c");
        // THEN
        assertEquals(2, snapshot.length, "Snapshot size must stay unchanged");
        assertArrayEquals(new String[] { "a", "b" }, snapshot, "Snapshot content must reflect the state at conversion time");
    }

    // ---------- Normal behaviour ----------
    @ParameterizedTest(name = "{index} => class={0}, collection={1}")
    @MethodSource("normalBehaviourProvider")
    void toArray_NormalBehaviour(Class<?> targetClass, Collection<?> collection, Object[] expected) {
        Object[] result = ArrayUtilities.toArray(targetClass, collection);
        assertArrayEquals(expected, result);
        assertEquals(targetClass, result.getClass().getComponentType(), "Component type must match the supplied class");
    }

    // ---------- Empty collection ----------
    @ParameterizedTest(name = "{index} => class={0}")
    @MethodSource("emptyCollectionProvider")
    void toArray_EmptyCollection(Class<?> targetClass) {
        Collection<?> empty = Collections.emptyList();
        Object[] result = ArrayUtilities.toArray(targetClass, empty);
        assertNotNull(result);
        assertEquals(0, result.length);
        assertEquals(targetClass, result.getClass().getComponentType(), "Component type must match the supplied class");
    }

    // ---------- Single element collection ----------
    @ParameterizedTest(name = "{index} => class={0}, element={1}")
    @MethodSource("singleElementProvider")
    void toArray_SingleElementCollection(Class<?> targetClass, Object element) {
        Collection<?> single = List.of(element);
        Object[] result = ArrayUtilities.toArray(targetClass, single);
        assertEquals(1, result.length);
        assertEquals(element, result[0]);
        assertEquals(targetClass, result.getClass().getComponentType(), "Component type must match the supplied class");
    }

    // ---------- Mixed compatible types ----------
    @Test
    void toArray_MixedNumberSubclasses() {
        Collection<Number> numbers = List.of(1, 2.5, 3L);
        Number[] result = ArrayUtilities.toArray(Number.class, numbers);
        assertArrayEquals(new Number[] { 1, 2.5, 3L }, result);
        assertEquals(Number.class, result.getClass().getComponentType());
    }

    @Test
    void toArray_MixedCharSequenceImplementations() {
        Collection<CharSequence> seqs = List.of("abc", new StringBuilder("def"));
        CharSequence[] result = ArrayUtilities.toArray(CharSequence.class, seqs);
        assertArrayEquals(new CharSequence[] { "abc", "def" }, result);
        assertEquals(CharSequence.class, result.getClass().getComponentType());
    }

    // ---------- Incompatible element type ----------
    @Test
    void toArray_IncompatibleElementType_ThrowsArrayStoreException() {
        List<Object> mixed = new ArrayList<>();
        mixed.add("ok");
        // Integer not assignable to String[]
        mixed.add(42);
        assertThrows(ArrayStoreException.class, () -> ArrayUtilities.toArray(String.class, mixed), "ArrayStoreException expected when collection contains incompatible element");
    }

    // ---------- Null arguments ----------
    @Test
    void toArray_NullClassArgument_ThrowsNullPointerException() {
        Collection<String> coll = List.of("a");
        assertThrows(NullPointerException.class, () -> ArrayUtilities.toArray(null, coll), "NullPointerException expected when classToCastTo is null");
    }

    @Test
    void toArray_NullCollectionArgument_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ArrayUtilities.toArray(String.class, null), "NullPointerException expected when collection argument is null");
    }

    // ---------- Preserve null elements with wrapper type ----------
    @Test
    void toArray_PreserveNullsWithIntegerWrapper() {
        List<Integer> source = Arrays.asList(null, 5, null);
        Integer[] result = ArrayUtilities.toArray(Integer.class, source);
        assertArrayEquals(new Integer[] { null, 5, null }, result);
        assertEquals(Integer.class, result.getClass().getComponentType());
    }

    // ---------- Primitive component type ----------
    @Test
    void toArray_PrimitiveIntClass_ThrowsClassCastException() {
        Collection<Integer> source = List.of(1, 2, 3);
        assertThrows(ClassCastException.class, () -> ArrayUtilities.toArray(int.class, source), "ClassCastException expected when using primitive int.class as target type");
    }

    @Test
    void toArray_PrimitiveBooleanClass_ThrowsClassCastException() {
        Collection<Boolean> source = Collections.emptyList();
        assertThrows(ClassCastException.class, () -> ArrayUtilities.toArray(boolean.class, source), "ClassCastException expected when using primitive boolean.class as target type");
    }

    // ---------- Invalid component type (void) ----------
    @Test
    void toArray_VoidClass_ThrowsIllegalArgumentException() {
        Collection<Object> empty = Collections.emptyList();
        assertThrows(IllegalArgumentException.class, () -> ArrayUtilities.toArray(void.class, empty), "IllegalArgumentException expected when using void.class as component type");
    }
}
