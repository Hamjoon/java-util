import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.StampedLock;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.Phaser;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.concurrent.atomic.AtomicStampedReference;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;

// @ExtendWith(MockitoExtension.class)
class ClassUtilities_haveCommonAncestor_Test {

    /* -----------------------------------------------------------------
    Helper types used in some of the test cases.
    -----------------------------------------------------------------
     */
    private interface Unrelated {
    }

    interface LocalUnrelated {
    }

    /* -----------------------------------------------------------------
     * Helper types used in some of the test cases.
     * -----------------------------------------------------------------
     */
//     interface Unrelated {
    }

//     interface LocalUnrelated {
    }

    class LocalUnrelatedClass {
    }

    interface TestInterface {
    }

    interface SubInterface extends TestInterface {
    }

    /* -----------------------------------------------------------------
     * Additional helper class used in negativeCases()
     * -----------------------------------------------------------------
     */
    class UnrelatedClass {
    }

    /* -----------------------------------------------------------------
    Argument providers for the parameterized tests.
    -----------------------------------------------------------------
     */
//     private static Stream<Arguments> positiveCases() {
//         return Stream.of(Arguments.of(String.class, String.class), Arguments.of(ArrayList.class, List.class), Arguments.of(String.class, Integer.class), Arguments.of(Runnable.class, String.class), Arguments.of(int[].class, Object.class), Arguments.of(int[].class, Cloneable.class), Arguments.of(int[].class, java.io.Serializable.class), Arguments.of(String[].class, Object[].class), Arguments.of(List.class, Collection.class), Arguments.of(Map.class, HashMap.class), Arguments.of(Set.class, SortedSet.class), Arguments.of(HashMap.class, AbstractMap.class), Arguments.of(ConcurrentHashMap.class, Map.class), Arguments.of(EnumSet.class, AbstractSet.class), Arguments.of(SubInterface.class, TestInterface.class));
//     }

    private static Stream<Arguments> negativeCases() {
        return Stream.of(Arguments.of(int.class, double.class), Arguments.of(int.class, Integer.class), Arguments.of(Runnable.class, Comparator.class), Arguments.of(int[].class, int.class), Arguments.of(Integer.class, int.class), Arguments.of(Unrelated.class, UnrelatedClass.class));
    }

    private static Stream<Arguments> nullArgumentCases() {
        return Stream.of(Arguments.of(null, String.class), Arguments.of(String.class, null), Arguments.of(null, null));
    }

    /* -----------------------------------------------------------------
    haveCommonAncestor() – basic positive cases
    -----------------------------------------------------------------
     */
//     @Test
//     @DisplayName("Classes that share a common ancestor should return true")
//     void testHaveCommonAncestor_Positive() {
//         // List implementations
//         assertTrue(ClassUtilities.haveCommonAncestor(LinkedList.class, ArrayList.class), "LinkedList and ArrayList both implement java.util.List");
//         // Numeric wrapper classes
//         assertTrue(ClassUtilities.haveCommonAncestor(Integer.class, Double.class), "Integer and Double both extend java.lang.Number");
//         // Interface hierarchy
//         assertTrue(ClassUtilities.haveCommonAncestor(SubInterface.class, TestInterface.class), "SubInterface extends TestInterface");
//     }

    /* -----------------------------------------------------------------
    haveCommonAncestor() – basic negative cases
    -----------------------------------------------------------------
     */
//     @Test
//     @DisplayName("Classes that share only Object should return false")
//     void testHaveCommonAncestor_Negative() {
//         // Runnable vs. Error – only Object is common
//         assertFalse(ClassUtilities.haveCommonAncestor(Runnable.class, Error.class), "Runnable and Error share only java.lang.Object");
//     }

    /* -----------------------------------------------------------------
    haveCommonAncestor() – null handling
    -----------------------------------------------------------------
     */
//     @Test
//     @DisplayName("Null arguments should be handled gracefully")
//     void testHaveCommonAncestor_NullArguments() {
//         assertFalse(ClassUtilities.haveCommonAncestor(null, String.class), "Null first argument must return false");
//         assertFalse(ClassUtilities.haveCommonAncestor(String.class, null), "Null second argument must return false");
//         assertFalse(ClassUtilities.haveCommonAncestor(null, null), "Both arguments null must return false");
//     }

    /* -----------------------------------------------------------------
    haveCommonAncestor() – interface vs. class where interface is not
    implemented by the class (should be false)
    -----------------------------------------------------------------
     */
//     @Test
//     @DisplayName("Unrelated interface and class should return false")
//     void testHaveCommonAncestor_UnrelatedInterface() {
//         assertFalse(ClassUtilities.haveCommonAncestor(LocalUnrelated.class, LocalUnrelatedClass.class), "Unrelated interface and class have only Object in common");
//     }

    /* -----------------------------------------------------------------
    haveCommonAncestor() – primitive wrapper vs. primitive (should be true
    because wrappers are treated as interchangeable with primitives)
    -----------------------------------------------------------------
     */
//     @Test
//     @DisplayName("Primitive wrapper and primitive type share a common ancestor")
//     void testHaveCommonAncestor_PrimitiveAndWrapper() {
//         assertTrue(ClassUtilities.haveCommonAncestor(Integer.class, int.class), "Wrapper class Integer and primitive int are considered related");
//         assertTrue(ClassUtilities.haveCommonAncestor(int.class, Integer.class), "Primitive int and wrapper class Integer are considered related");
//     }

    /* -----------------------------------------------------------------
    haveCommonAncestor() – collection hierarchy exclusion test (mirrors
    existing findLowestCommonSupertypesExcluding usage)
    -----------------------------------------------------------------
     */
//     @Test
//     @DisplayName("Collections that are excluded should result in empty common set")
//     void testFindLowestCommonSupertypesExcluding_EmptyResult() {
//         Set<Class<?>> skip = new HashSet<>();
//         Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(Collection.class, Map.class, skip);
//         assertTrue(result.isEmpty(), "Collection and Map have no common supertype other than Object, which is excluded");
//     }
}