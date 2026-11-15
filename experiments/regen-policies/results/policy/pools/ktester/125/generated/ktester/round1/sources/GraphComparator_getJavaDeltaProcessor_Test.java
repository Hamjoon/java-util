package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@ExtendWith(MockitoExtension.class)
class GraphComparator_getJavaDeltaProcessor_Test {

    @Test
    void getJavaDeltaProcessor_ShouldReturnNonNullInstance() {
        // GIVEN – nothing to arrange, the method is static and parameter‑less
        // WHEN
        GraphComparator.DeltaProcessor processor = GraphComparator.getJavaDeltaProcessor();
        // THEN
        assertNotNull(processor, "DeltaProcessor should not be null");
    }

    @Test
    void getJavaDeltaProcessor_ShouldReturnJavaDeltaProcessorImplementation() {
        // GIVEN
        GraphComparator.DeltaProcessor processor = GraphComparator.getJavaDeltaProcessor();
        // WHEN – we inspect the concrete class name (the inner class is package‑private)
        String concreteClassName = processor.getClass().getSimpleName();
        // THEN – the class name should be “JavaDeltaProcessor”.  Using the name avoids
        // compile‑time coupling to the inner class visibility.
        assertEquals("JavaDeltaProcessor", concreteClassName, "Expected the concrete implementation to be JavaDeltaProcessor");
    }

    @Test
    void getJavaDeltaProcessor_ShouldReturnNewInstanceOnEachCall() {
        // GIVEN
        GraphComparator.DeltaProcessor first = GraphComparator.getJavaDeltaProcessor();
        GraphComparator.DeltaProcessor second = GraphComparator.getJavaDeltaProcessor();
        // THEN – each invocation should produce a distinct object (no caching)
        assertNotSame(first, second, "Each call should return a new DeltaProcessor instance");
    }

    @Test
    void getJavaDeltaProcessor_ShouldImplementDeltaProcessorInterface() {
        // GIVEN
        GraphComparator.DeltaProcessor processor = GraphComparator.getJavaDeltaProcessor();
        // THEN – the returned object must be assignable to the DeltaProcessor interface
        assertTrue(processor instanceof GraphComparator.DeltaProcessor, "Returned object must implement GraphComparator.DeltaProcessor");
    }

    @Test
    void getJavaDeltaProcessor_ShouldBeExactNestedClass() {
        // GIVEN
        GraphComparator.DeltaProcessor processor = GraphComparator.getJavaDeltaProcessor();
        // WHEN – obtain the fully‑qualified class name
        String fqcn = processor.getClass().getName();
        // THEN – it must equal the expected nested class name
        assertEquals("com.cedarsoftware.util.GraphComparator$JavaDeltaProcessor", fqcn, "Processor should be the exact nested class GraphComparator$JavaDeltaProcessor");
    }

    @Test
    void getJavaDeltaProcessor_ShouldNeverThrowException() {
        // This test simply ensures that invoking the method does not result in any
        // unchecked exception, even when the JVM classloader behaves oddly.
        assertDoesNotThrow(() -> GraphComparator.getJavaDeltaProcessor(), "Calling getJavaDeltaProcessor() must not throw any exception");
    }

    @Test
    void getJavaDeltaProcessor_TwoInstancesShouldHaveDifferentHashCodes() {
        // GIVEN
        GraphComparator.DeltaProcessor first = GraphComparator.getJavaDeltaProcessor();
        GraphComparator.DeltaProcessor second = GraphComparator.getJavaDeltaProcessor();
        // THEN – distinct objects normally have distinct hash codes
        assertNotEquals(first.hashCode(), second.hashCode(), "Two distinct instances should produce different hashCode values");
    }

    @Test
    void getJavaDeltaProcessor_ToStringShouldContainClassNameAndNotBeNull() {
        // GIVEN
        GraphComparator.DeltaProcessor processor = GraphComparator.getJavaDeltaProcessor();
        // WHEN
        String str = processor.toString();
        // THEN
        assertNotNull(str, "toString() must not return null");
        assertTrue(str.contains("JavaDeltaProcessor"), "toString() should contain the class name JavaDeltaProcessor");
    }

    /**
     * Parameterized sanity check – repeat the non‑null assertion several times **
     */
    @ParameterizedTest(name = "Invocation {index} should return non‑null")
    @ValueSource(ints = { 1, 2, 3, 4, 5 })
    void getJavaDeltaProcessor_MultipleInvocationsReturnNonNull(int dummy) {
        // WHEN
        GraphComparator.DeltaProcessor processor = GraphComparator.getJavaDeltaProcessor();
        // THEN
        assertNotNull(processor, "Each invocation must return a non‑null instance");
    }
}
