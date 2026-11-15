package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.*;

@ExtendWith(MockitoExtension.class)
class GraphComparator_getJavaDeltaProcessor_Test {

    @Test
    void getJavaDeltaProcessor_ShouldReturnNonNullInstance() {
        // GIVEN
        // (nothing to arrange)
        // WHEN
        GraphComparator.DeltaProcessor processor = GraphComparator.getJavaDeltaProcessor();
        // THEN
        assertNotNull(processor, "The factory must not return null");
    }

    @Test
    void getJavaDeltaProcessor_ShouldReturnObjectImplementingDeltaProcessor() {
        // GIVEN
        // (nothing to arrange)
        // WHEN
        Object processor = GraphComparator.getJavaDeltaProcessor();
        // THEN
        assertTrue(processor instanceof GraphComparator.DeltaProcessor, "Returned object must implement GraphComparator.DeltaProcessor");
    }

    @Test
    void getJavaDeltaProcessor_ShouldCreateNewInstanceOnEachCall() {
        // GIVEN
        // (nothing to arrange)
        // WHEN
        GraphComparator.DeltaProcessor first = GraphComparator.getJavaDeltaProcessor();
        GraphComparator.DeltaProcessor second = GraphComparator.getJavaDeltaProcessor();
        // THEN
        assertNotSame(first, second, "Each call to getJavaDeltaProcessor() should return a distinct instance");
    }

    @Test
    void getJavaDeltaProcessor_ShouldBeConcreteJavaDeltaProcessor() {
        // GIVEN
        // (nothing to arrange)
        // WHEN
        GraphComparator.DeltaProcessor processor = GraphComparator.getJavaDeltaProcessor();
        // THEN
        // The concrete class is package‑private, but its simple name is known.
        assertEquals("JavaDeltaProcessor", processor.getClass().getSimpleName(), "Factory should produce an instance of JavaDeltaProcessor");
    }

    @Test
    void getJavaDeltaProcessor_ShouldReturnExactInnerClassName() {
        // GIVEN
        // (nothing to arrange)
        // WHEN
        GraphComparator.DeltaProcessor processor = GraphComparator.getJavaDeltaProcessor();
        // THEN
        String expectedName = "com.cedarsoftware.util.GraphComparator$JavaDeltaProcessor";
        assertEquals(expectedName, processor.getClass().getName(), "Returned instance must be of the expected inner class");
    }

    @Test
    void getJavaDeltaProcessor_ShouldNeverThrowException() {
        // GIVEN
        // (nothing to arrange)
        // WHEN / THEN
        assertDoesNotThrow(() -> GraphComparator.getJavaDeltaProcessor(), "Calling getJavaDeltaProcessor() must not throw any exception");
    }

    @Test
    void getJavaDeltaProcessor_InnerClassShouldBeStatic() throws Exception {
        // GIVEN
        // Locate the inner class named JavaDeltaProcessor
        Class<?>[] declared = GraphComparator.class.getDeclaredClasses();
        Class<?> javaDeltaProcessorClass = null;
        for (Class<?> c : declared) {
            if ("JavaDeltaProcessor".equals(c.getSimpleName())) {
                javaDeltaProcessorClass = c;
                break;
            }
        }
        assertNotNull(javaDeltaProcessorClass, "Inner class JavaDeltaProcessor should exist inside GraphComparator");
        // THEN
        assertTrue(Modifier.isStatic(javaDeltaProcessorClass.getModifiers()), "JavaDeltaProcessor must be declared static to be instantiated without an outer instance");
    }

    @Test
    void getJavaDeltaProcessor_ShouldReturnOnlyJavaDeltaProcessorImplementation() {
        // GIVEN
        // (nothing to arrange)
        // WHEN
        GraphComparator.DeltaProcessor processor = GraphComparator.getJavaDeltaProcessor();
        // THEN
        // Ensure the concrete class is exactly the expected inner class
        Class<?> expectedClass = null;
        for (Class<?> c : GraphComparator.class.getDeclaredClasses()) {
            if ("JavaDeltaProcessor".equals(c.getSimpleName())) {
                expectedClass = c;
                break;
            }
        }
        assertNotNull(expectedClass, "Expected inner class not found");
        assertEquals(expectedClass, processor.getClass(), "Factory must return the exact JavaDeltaProcessor implementation, not another DeltaProcessor");
    }

    @Test
    void getJavaDeltaProcessor_ConcurrentCallsReturnDistinctInstances() throws Exception {
        // GIVEN
        final int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<GraphComparator.DeltaProcessor>> futures = new ArrayList<>();
        // WHEN
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(GraphComparator::getJavaDeltaProcessor));
        }
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "Executor did not terminate in time");
        // Collect results
        Set<GraphComparator.DeltaProcessor> distinct = new HashSet<>();
        for (Future<GraphComparator.DeltaProcessor> f : futures) {
            // get() rethrows any exception as ExecutionException
            distinct.add(f.get());
        }
        // THEN
        assertEquals(threadCount, distinct.size(), "Each concurrent call should yield a unique JavaDeltaProcessor instance");
    }
}
