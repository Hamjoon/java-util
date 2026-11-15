package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.function.Consumer;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.InaccessibleObjectException;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicInteger;

@ExtendWith(MockitoExtension.class)
class Traverser_traverse_Test {

    /**
     * Simple class A – root of the graph
     */
    class Alpha {

        String name;

        Collection<Object> contacts;

        Beta beta;
    }

    /**
     * Simple class B – referenced from {@link Alpha}
     */
    class Beta {

        int age;

        Map<Object, Object> friends;

        Charlie charlie;
    }

    /**
     * Simple class C – creates a cycle back to {@link Alpha}
     */
    class Charlie {

        double salary;

        Collection<Object> timezones;

        Object[] dates;

        Alpha alpha;

        TimeZone zone = TimeZone.getDefault();

        Delta delta;
    }

    /**
     * Simple class D – used only to verify that a class can be skipped
     */
    class Delta {

        TimeZone timeZone = TimeZone.getDefault();
    }

    /**
     * Helper class with a private field that is not accessible via reflection
     */
    class InaccessibleFieldHolder {

        // The field is private and we will *not* call setAccessible(true) in the test,
        // so Traverser.walk should hit an InaccessibleObjectException (or IllegalAccessException)
        private final String secret = "top‑secret";
    }

    /**
     * Node used to build a deep cyclic linked list
     */
    static class CyclicNode {

        CyclicNode next;

        static CyclicNode createDeepCycle(int size) {
            if (size <= 0) {
                throw new IllegalArgumentException("size must be > 0");
            }
            CyclicNode first = new CyclicNode();
            CyclicNode prev = first;
            for (int i = 1; i < size; i++) {
                CyclicNode cur = new CyclicNode();
                prev.next = cur;
                prev = cur;
            }
            // close the cycle
            prev.next = first;
            return first;
        }
    }

    /**
     * Root Null Handling – parameterized to cover both sub‑cases **
     */
    @Nested
    class RootNullHandling {

        @Test
        void whenRootIsNullVisitorIsInvokedOnceWithNullNode() {
            // Visitor records how many times it is called and the node value
            AtomicInteger callCount = new AtomicInteger(0);
            Consumer<NodeVisit> visitor = visit -> {
                callCount.incrementAndGet();
                assertNull(visit.getNode(), "Node should be null when root is null");
            };
            Traverser.traverse(null, visitor, Collections.emptySet());
            assertEquals(1, callCount.get(), "Visitor must be invoked exactly once for a null root");
        }

        @Test
        void whenRootIsNullVisitorExceptionIsPropagated() {
            RuntimeException expected = new RuntimeException("visited");
            Consumer<NodeVisit> visitor = visit -> {
                throw expected;
            };
            RuntimeException actual = assertThrows(RuntimeException.class, () -> Traverser.traverse(null, visitor, Collections.emptySet()));
            assertSame(expected, actual, "Exception thrown by visitor should propagate unchanged");
        }
    }

    @Test
    void testTraverseNullVisitorThrows() {
        Alpha root = new Alpha();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Traverser.traverse(root, null, Collections.emptySet()));
        assertEquals("visitor cannot be null", ex.getMessage());
    }

    @Test
    void testTraverseVisitsAllNodes() {
        // Build a small cyclic graph (same shape as the original TraverserTest)
        Alpha alpha = new Alpha();
        Beta beta = new Beta();
        Charlie charlie = new Charlie();
        alpha.name = "alpha";
        alpha.beta = beta;
        alpha.contacts = new ArrayList<>();
        alpha.contacts.add(beta);
        alpha.contacts.add(charlie);
        alpha.contacts.add("Harry");
        beta.age = 45;
        beta.charlie = charlie;
        beta.friends = new LinkedHashMap<>();
        beta.friends.put("Tom", "Tom Jones");
        beta.friends.put(alpha, "Alpha beta");
        beta.friends.put("beta", beta);
        charlie.salary = 150_000.01;
        charlie.alpha = alpha;
        charlie.timezones = new LinkedList<>();
        charlie.timezones.add(TimeZone.getTimeZone("EST"));
        charlie.timezones.add(TimeZone.getTimeZone("GMT"));
        charlie.dates = new Date[] { new Date() };
        // Counters for each type we expect to see
        final Map<Class<?>, Integer> visitCount = new HashMap<>();
        Consumer<NodeVisit> visitor = visit -> {
            Object node = visit.getNode();
            visitCount.merge(node.getClass(), 1, Integer::sum);
        };
        Traverser.traverse(alpha, visitor, Collections.emptySet());
        // Verify that each distinct class in the graph was visited exactly once
        assertEquals(1, visitCount.getOrDefault(Alpha.class, 0));
        assertEquals(1, visitCount.getOrDefault(Beta.class, 0));
        assertEquals(1, visitCount.getOrDefault(Charlie.class, 0));
        // TimeZone objects are visited – at least the one in Charlie.zone
        assertTrue(visitCount.getOrDefault(TimeZone.class, 0) >= 1);
        // Primitive wrapper / collection types are also visited but we do not assert them here
    }

    @Test
    void testTraverseSkipsSpecifiedClasses() {
        // Build the same graph as the previous test
        Alpha alpha = new Alpha();
        Beta beta = new Beta();
        Charlie charlie = new Charlie();
        alpha.name = "alpha";
        alpha.beta = beta;
        alpha.contacts = new ArrayList<>();
        alpha.contacts.add(beta);
        alpha.contacts.add(charlie);
        alpha.contacts.add("Harry");
        beta.age = 45;
        beta.charlie = charlie;
        beta.friends = new LinkedHashMap<>();
        beta.friends.put("Tom", "Tom Jones");
        beta.friends.put(alpha, "Alpha beta");
        beta.friends.put("beta", beta);
        charlie.salary = 150_000.01;
        charlie.alpha = alpha;
        charlie.timezones = new LinkedList<>();
        charlie.timezones.add(TimeZone.getTimeZone("EST"));
        charlie.timezones.add(TimeZone.getTimeZone("GMT"));
        charlie.dates = new Date[] { new Date() };
        // Skip TimeZone – we expect no TimeZone nodes to be reported
        Set<Class<?>> skip = new HashSet<>();
        skip.add(TimeZone.class);
        final Map<Class<?>, Integer> visitCount = new HashMap<>();
        Consumer<NodeVisit> visitor = visit -> {
            Object node = visit.getNode();
            visitCount.merge(node.getClass(), 1, Integer::sum);
        };
        Traverser.traverse(alpha, visitor, skip);
        // Verify that the core domain objects are still visited
        assertEquals(1, visitCount.getOrDefault(Alpha.class, 0));
        assertEquals(1, visitCount.getOrDefault(Beta.class, 0));
        assertEquals(1, visitCount.getOrDefault(Charlie.class, 0));
        // TimeZone should be completely omitted
        assertEquals(0, visitCount.getOrDefault(TimeZone.class, 0));
    }

    /**
     * ClassesToSkip Null Handling **
     */
    @Test
    void whenClassesToSkipIsNullTraversalFails() {
        // The contract of Traverser (as exercised by the original library) does not accept a null set.
        // We assert that *some* NPE/IAE is thrown – the exact type is implementation dependent.
        Consumer<NodeVisit> visitor = visit -> {
            /* no‑op */
        };
        Object root = new Object();
        Exception ex = assertThrows(Exception.class, () -> Traverser.traverse(root, visitor, null));
        // Accept either NullPointerException or IllegalArgumentException
        assertTrue(ex instanceof NullPointerException || ex instanceof IllegalArgumentException, "Expected NPE or IAE when classesToSkip is null, but got: " + ex);
    }

    /**
     * ClassesToSkip Contains Null **
     */
    @Test
    void whenClassesToSkipContainsNullTraversalFailsDuringIteration() {
        Consumer<NodeVisit> visitor = visit -> {
            /* no‑op */
        };
        Object root = new Object();
        Set<Class<?>> skip = new HashSet<>(Arrays.asList(String.class, null));
        // The traversal should attempt to use the null entry and consequently throw NPE.
        assertThrows(NullPointerException.class, () -> Traverser.traverse(root, visitor, skip), "Iterating over a Set that contains null should cause a NullPointerException");
    }

    /**
     * Inaccessible Field Exception **
     */
    @Test
    void whenEncounteringInaccessibleFieldExceptionIsPropagated() {
        // The holder has a private field; Traverser.walk will try to make it accessible.
        // In a JPMS environment without opening the module, this results in InaccessibleObjectException.
        // We accept either InaccessibleObjectException or IllegalAccessException.
        Consumer<NodeVisit> visitor = visit -> {
            /* no‑op */
        };
        InaccessibleFieldHolder holder = new InaccessibleFieldHolder();
        Exception ex = assertThrows(Exception.class, () -> Traverser.traverse(holder, visitor, Collections.emptySet()));
        assertTrue(ex instanceof InaccessibleObjectException || ex instanceof IllegalAccessException, "Expected InaccessibleObjectException or IllegalAccessException, but got: " + ex);
    }

    /**
     * Cyclic Graph StackOverflow **
     */
    @Test
    @Disabled("Running this test may cause the JVM to crash due to StackOverflowError. " + "Enable only when the implementation is known to be recursive without cycle detection.")
    void whenDeepCyclicGraphIsTraversedStackOverflowOccurs() {
        // Build a very deep cyclic list (20 000 nodes).  The Traverser implementation in the library
        // uses recursion; without proper cycle detection it will eventually overflow the stack.
        CyclicNode root = CyclicNode.createDeepCycle(20_000);
        Consumer<NodeVisit> visitor = visit -> {
            /* no‑op */
        };
        assertThrows(StackOverflowError.class, () -> Traverser.traverse(root, visitor, Collections.emptySet()), "Expected StackOverflowError for a deep cyclic graph when traversal is recursive");
    }
}
