import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ClassUtilities_findLowestCommonSupertypesExcluding_Test {

    class LocalImplementsA implements MarkerA {
    }

    class LocalImplementsB implements MarkerB {
    }

    /* ----------------------------------------------------------------------
    5) Multiple lowest common supertypes – interfaces that are not sub‑interfaces
    ---------------------------------------------------------------------- */
    interface MarkerA {
    }

    interface MarkerB {
    }

    class ImplementsA implements MarkerA {
    }

    class ImplementsB implements MarkerB {
    }

    static class MultiMarker implements MarkerA, MarkerB {
    }

//     class LocalImplementsA implements MarkerA {
    }

//     class LocalImplementsB implements MarkerB {
    }

    /* ----------------------------------------------------------------------
    1) Null handling – both arguments null → empty set
    ----------------------------------------------------------------------
     */
//     @Test
//     @DisplayName("findLowestCommonSupertypesExcluding – both arguments null")
//     void testNullBoth() {
//         Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(null, null, Collections.emptySet());
//         assertNotNull("Result must never be null", result);
//         assertTrue("Both null arguments should yield an empty set", result.isEmpty());
//     }

//     @Test
//     @DisplayName("findLowestCommonSupertypesExcluding – one argument null")
//     void testNullOneSide() {
//         Set<Class<?>> resultA = ClassUtilities.findLowestCommonSupertypesExcluding(null, String.class, Collections.emptySet());
//         Set<Class<?>> resultB = ClassUtilities.findLowestCommonSupertypesExcluding(Integer.class, null, Collections.emptySet());
//         assertTrue("Null on first side should yield empty set", resultA.isEmpty());
//         assertTrue("Null on second side should yield empty set", resultB.isEmpty());
//     }

    /* ----------------------------------------------------------------------
    2) Same class handling – with and without skip list
    ----------------------------------------------------------------------
     */
//     @Test
//     @DisplayName("findLowestCommonSupertypesExcluding – same class, not skipped")
//     void testSameClassNotSkipped() {
//         Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(String.class, String.class, Collections.emptySet());
//         assertEquals("Same class not in skip list should return a singleton set", 1, result.size());
//         assertTrue("Result should contain the class itself", result.contains(String.class));
//     }

//     @Test
//     @DisplayName("findLowestCommonSupertypesExcluding – same class, skipped")
//     void testSameClassSkipped() {
//         Set<Class<?>> skip = new HashSet<>();
//         skip.add(Number.class);
//         Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(Number.class, Number.class, skip);
//         assertTrue("When the class is in the skip list the result must be empty", result.isEmpty());
//     }

    /* ----------------------------------------------------------------------
    3) Simple inheritance – ArrayList vs LinkedList
    ----------------------------------------------------------------------
     */
//     @Test
//     @DisplayName("findLowestCommonSupertypesExcluding – ArrayList vs LinkedList")
//     void testArrayListLinkedList() {
//         Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(ArrayList.class, LinkedList.class, Collections.emptySet());
//         // The deepest common supertype is AbstractList
//         assertEquals("Expected a single deepest common supertype", 1, result.size());
//         assertTrue("Expected AbstractList to be the lowest common supertype", result.contains(AbstractList.class));
//     }

//     @Test
//     @DisplayName("findLowestCommonSupertypesExcluding – skip deepest common supertype")
//     void testSkipDeepestCommonSupertype() {
//         Set<Class<?>> skip = new HashSet<>();
//         // skip the deepest common type
//         skip.add(AbstractList.class);
//         Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(ArrayList.class, LinkedList.class, skip);
//         // After skipping AbstractList the remaining common supertypes are List, AbstractCollection, Object
//         assertFalse("Result should not be empty after skipping the deepest type", result.isEmpty());
//         assertFalse("Skipped type must not appear in the result", result.contains(AbstractList.class));
//         assertTrue("Result should contain the next deepest common supertype(s)", result.contains(List.class) || result.contains(AbstractCollection.class));
//     }

    /* ----------------------------------------------------------------------
    4) No common supertype other than Object – skipping Object yields empty set
    ----------------------------------------------------------------------
     */
//     @Test
//     @DisplayName("findLowestCommonSupertypesExcluding – Runnable vs Error, skip Object")
//     void testNoCommonSupertypeSkippingObject() {
//         Set<Class<?>> skip = new HashSet<>();
//         skip.add(Object.class);
//         Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(Runnable.class, Error.class, skip);
//         assertTrue("When the only common supertype (Object) is skipped the result must be empty", result.isEmpty());
//     }

//     @Test
//     @DisplayName("findLowestCommonSupertypesExcluding – two unrelated interfaces")
//     void testMultipleLowestCommonSupertypes() {
//         // Same class case returns the class itself, not the interfaces.
//         // To create a true multiple‑lowest scenario we compare two different classes
//         // that share both interfaces but no deeper common class.
//         class LocalImplementsA implements MarkerA {
//         }
//         class LocalImplementsB implements MarkerB {
//         }
//         Set<Class<?>> res = ClassUtilities.findLowestCommonSupertypesExcluding(LocalImplementsA.class, LocalImplementsB.class, Collections.emptySet());
//         // The only common supertype is Object, which is the deepest.
//         // By skipping Object we can verify that the method correctly returns an empty set.
//         Set<Class<?>> skipObject = new HashSet<>();
//         skipObject.add(Object.class);
//         Set<Class<?>> empty = ClassUtilities.findLowestCommonSupertypesExcluding(LocalImplementsA.class, LocalImplementsB.class, skipObject);
//         assertTrue("Skipping Object should leave no common supertypes", empty.isEmpty());
//     }

    /* ----------------------------------------------------------------------
    6) Verify that the method never returns null
    ----------------------------------------------------------------------
     */
//     @Test
//     @DisplayName("findLowestCommonSupertypesExcluding – never returns null")
//     void testNeverReturnsNull() {
//         Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(String.class, Integer.class, Collections.emptySet());
//         assertNotNull("Method contract requires a non‑null Set", result);
//     }

    /* ----------------------------------------------------------------------
    6) Skip list removes all common supertypes
    ----------------------------------------------------------------------
     */
//     @Test
//     @DisplayName("findLowestCommonSupertypesExcluding – skip all common supertypes")
//     void testSkipAllCommonSupertypes() {
//         Set<Class<?>> skip = new HashSet<>(Arrays.asList(Object.class, Serializable.class, Comparable.class));
//         Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(Integer.class, String.class, skip);
//         assertTrue("All common supertypes are skipped – result must be empty", result.isEmpty());
//     }

    /* ----------------------------------------------------------------------
    7) Skip list removes the deepest common supertype only (ArrayList vs LinkedList)
    ----------------------------------------------------------------------
     */
//     @Test
//     @DisplayName("findLowestCommonSupertypesExcluding – skip only AbstractList")
//     void testSkipOnlyDeepest() {
//         Set<Class<?>> skip = new HashSet<>(Collections.singletonList(AbstractList.class));
//         Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(ArrayList.class, LinkedList.class, skip);
//         // After removing AbstractList, List becomes the deepest common supertype.
//         assertEquals("Result should contain exactly one type after skipping AbstractList", 1, result.size());
//         assertTrue("Result should contain List", result.contains(List.class));
//     }

    /* ----------------------------------------------------------------------
    8) Skip list contains only Object – should not affect deeper result
    ----------------------------------------------------------------------
     */
//     @Test
//     @DisplayName("findLowestCommonSupertypesExcluding – skip only Object")
//     void testSkipOnlyObject() {
//         Set<Class<?>> skip = new HashSet<>(Collections.singletonList(Object.class));
//         Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(ArrayList.class, LinkedList.class, skip);
//         // AbstractList is still the deepest common supertype.
//         assertEquals("Skipping Object must not change the deepest common supertype", 1, result.size());
//         assertTrue("Result should still contain AbstractList", result.contains(AbstractList.class));
//     }

    /* ----------------------------------------------------------------------
    9) Subclass relationship – ArrayList vs AbstractList
    ----------------------------------------------------------------------
     */
//     @Test
//     @DisplayName("findLowestCommonSupertypesExcluding – subclass relationship")
//     void testSubclassRelationship() {
//         Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(ArrayList.class, AbstractList.class, Collections.emptySet());
//         assertEquals("Result should contain exactly one type", 1, result.size());
//         assertTrue("AbstractList is the deepest common supertype", result.contains(AbstractList.class));
//     }

    /* ----------------------------------------------------------------------
    10) Multiple interfaces common supertypes – HashSet vs TreeSet
    ----------------------------------------------------------------------
     */
//     @Test
//     @DisplayName("findLowestCommonSupertypesExcluding – HashSet vs TreeSet")
//     void testMultipleInterfaceSupertypes() {
//         Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(HashSet.class, TreeSet.class, Collections.emptySet());
//         Set<Class<?>> expected = new HashSet<>(Arrays.asList(Set.class, Cloneable.class, Serializable.class));
//         assertEquals("HashSet and TreeSet should share Set, Cloneable and Serializable as lowest supertypes", expected, result);
//     }

    /* ----------------------------------------------------------------------
    11) Primitive and wrapper interaction – int vs Integer
    ----------------------------------------------------------------------
     */
//     @Test
//     @DisplayName("findLowestCommonSupertypesExcluding – primitive vs wrapper")
//     void testPrimitiveVsWrapper() {
//         Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(int.class, Integer.class, Collections.emptySet());
//         assertTrue("Primitive int and Integer share no common supertype, result must be empty", result.isEmpty());
//     }

    /* ----------------------------------------------------------------------
    12) Array types common supertypes – int[] vs String[]
    ----------------------------------------------------------------------
     */
//     @Test
//     @DisplayName("findLowestCommonSupertypesExcluding – array types")
//     void testArrayTypesCommonSupertypes() {
//         Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(int[].class, String[].class, Collections.emptySet());
//         Set<Class<?>> expected = new HashSet<>(Arrays.asList(Cloneable.class, Serializable.class));
//         assertEquals("All array types implement Cloneable and Serializable", expected, result);
//     }

    /* ----------------------------------------------------------------------
    13) Skip list with non‑common type – RandomAccess should be ignored
    ----------------------------------------------------------------------
     */
//     @Test
//     @DisplayName("findLowestCommonSupertypesExcluding – skip non‑common type")
//     void testSkipNonCommonType() {
//         Set<Class<?>> skip = new HashSet<>(Collections.singletonList(RandomAccess.class));
//         Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(ArrayList.class, LinkedList.class, skip);
//         // RandomAccess is only on ArrayList; skipping it must not affect the result.
//         assertEquals("Result size must stay 1", 1, result.size());
//         assertTrue("Result should still contain AbstractList", result.contains(AbstractList.class));
//     }

    /* ----------------------------------------------------------------------
    14) Skip list containing a null element – null should be ignored
    ----------------------------------------------------------------------
     */
//     @Test
//     @DisplayName("findLowestCommonSupertypesExcluding – skip list contains null")
//     void testSkipListContainsNull() {
//         Set<Class<?>> skip = new HashSet<>();
//         // deliberately add a null entry
//         skip.add(null);
//         Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(Integer.class, String.class, skip);
//         Set<Class<?>> expected = new HashSet<>(Arrays.asList(Serializable.class, Comparable.class));
//         assertEquals("Null entry in skip list must be ignored, behavior same as empty skip list", expected, result);
//     }

    /* ----------------------------------------------------------------------
    15) Null skipList handling – should throw NullPointerException
    ----------------------------------------------------------------------
     */
//     @Test
//     @DisplayName("findLowestCommonSupertypesExcluding – null skipList throws NPE")
//     void testNullSkipListThrowsNPE() {
//         try {
//             ClassUtilities.findLowestCommonSupertypesExcluding(ArrayList.class, LinkedList.class, null);
//             fail("Providing a null skipList must result in NullPointerException");
//         } catch (NullPointerException expected) {
//             // expected
//         }
//     }
}