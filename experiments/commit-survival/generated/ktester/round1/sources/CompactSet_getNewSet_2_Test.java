package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class CompactSet_getNewSet_2_Test {

    /**
     * Holds the static mock for {@link ReflectionUtils}.
     */
    private MockedStatic<ReflectionUtils> reflectionUtilsMock;

    /**
     * Helper subclass that makes the protected methods public for the test.
     */
    private static final class TestCompactSet<E> extends CompactSet<E> {

        /**
         * Expose {@code getNewSet()}
         */
        @Override
        public Set<E> getNewSet() {
            return super.getNewSet();
        }

        /**
         * Expose {@code getNewSet(int)}
         */
        public Set<E> getNewSetWithSize(int size) {
            return super.getNewSet(size);
        }
    }

    /**
     * Variant that returns a custom {@code DummySet} from {@code getNewSet()}.
     */
    private static final class DummySetCompactSet<E> extends TestCompactSet<E> {

        @Override
        public Set<E> getNewSet() {
            return new DummySet<>();
        }
    }

    /**
     * Variant that returns a custom {@code BadSet} from {@code getNewSet()}.
     */
    private static final class BadSetCompactSet<E> extends TestCompactSet<E> {

        @Override
        public Set<E> getNewSet() {
            return new BadSet<>();
        }
    }

    /**
     * Simple {@code Set} implementation that has a no‑arg and an {@code int} constructor.
     */
    private static final class DummySet<E> extends HashSet<E> {

        public DummySet() {
            super();
        }

        public DummySet(int capacity) {
            super();
            /* ignore capacity – just a placeholder */
        }
    }

    /**
     * {@code Set} whose {@code int} constructor always throws an exception.
     */
    private static final class BadSet<E> extends HashSet<E> {

        public BadSet() {
            super();
        }

        public BadSet(int capacity) {
            throw new RuntimeException("forced ctor failure");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh static mock for each test so that stubbing does not leak.
        reflectionUtilsMock = Mockito.mockStatic(ReflectionUtils.class);
    }

    @AfterEach
    void teardownAfterEach() {
        // Close the static mock to restore the original behavior.
        reflectionUtilsMock.close();
    }

    @Test
    void testGetNewSet_WithSizeConstructor_ReturnsSetCreatedViaConstructor() throws Exception {
        // Arrange ------------------------------------------------------------
        TestCompactSet<Integer> set = new TestCompactSet<>();
        // The default implementation of getNewSet() returns a HashSet.
        // We want the static call to return the HashSet(int) constructor.
        Constructor<HashSet> hashSetCtor = HashSet.class.getConstructor(int.class);
        reflectionUtilsMock.when(() -> ReflectionUtils.getConstructor(eq(HashSet.class), eq(Integer.TYPE))).thenReturn(hashSetCtor);
        // Act ----------------------------------------------------------------
        Set<Integer> result = set.getNewSetWithSize(10);
        // Assert ---------------------------------------------------------------
        assertNotNull(result, "Resulting set must not be null");
        assertTrue(result instanceof HashSet, "Result should be a HashSet when the size constructor is available");
        assertTrue(result.isEmpty(), "Newly created set must be empty");
    }

    @Test
    void testGetNewSet_WhenConstructorThrows_FallsBackToDefaultSet() {
        // Arrange ------------------------------------------------------------
        TestCompactSet<String> set = new TestCompactSet<>();
        // Simulate a failure while trying to obtain the constructor.
        reflectionUtilsMock.when(() -> ReflectionUtils.getConstructor(any(Class.class), eq(Integer.TYPE))).thenThrow(new RuntimeException("simulated failure"));
        // Act ----------------------------------------------------------------
        Set<String> result = set.getNewSetWithSize(5);
        // Assert ---------------------------------------------------------------
        assertNotNull(result, "Resulting set must not be null even when constructor lookup fails");
        // The fallback is the no‑arg getNewSet(), which for the default implementation is a HashSet.
        assertTrue(result instanceof HashSet, "Fallback set should be a HashSet");
        assertTrue(result.isEmpty(), "Fallback set must be empty");
    }

    @Test
    void testGetNewSet_WithSizeZero_ReturnsEmptySetViaConstructor() throws Exception {
        // Arrange
        TestCompactSet<String> set = new TestCompactSet<>();
        Constructor<HashSet> hashSetCtor = HashSet.class.getConstructor(int.class);
        reflectionUtilsMock.when(() -> ReflectionUtils.getConstructor(eq(HashSet.class), eq(Integer.TYPE))).thenReturn(hashSetCtor);
        // Act
        Set<String> result = set.getNewSetWithSize(0);
        // Assert
        assertNotNull(result);
        assertTrue(result instanceof HashSet);
        assertTrue(result.isEmpty(), "Set created with size 0 must be empty");
    }

    @Test
    void testGetNewSet_WithNegativeSize_FallsBackToDefaultSet() throws Exception {
        // Arrange
        TestCompactSet<String> set = new TestCompactSet<>();
        // Use the real HashSet(int) constructor – it throws IllegalArgumentException for negative capacity.
        Constructor<HashSet> hashSetCtor = HashSet.class.getConstructor(int.class);
        reflectionUtilsMock.when(() -> ReflectionUtils.getConstructor(eq(HashSet.class), eq(Integer.TYPE))).thenReturn(hashSetCtor);
        // Act
        Set<String> result = set.getNewSetWithSize(-5);
        // Assert
        assertNotNull(result);
        // The fallback is the no‑arg getNewSet(), which returns a HashSet.
        assertTrue(result instanceof HashSet, "Fallback set should be a HashSet");
        assertTrue(result.isEmpty(), "Fallback set must be empty");
    }

    @Test
    void testGetNewSet_WithMaxInt_NoOOM_DoesNotThrow() throws Exception {
        // Arrange
        DummySetCompactSet<Integer> set = new DummySetCompactSet<>();
        // Force the reflection utils to return the DummySet(int) constructor.
        Constructor<DummySet> dummyCtor = DummySet.class.getConstructor(int.class);
        reflectionUtilsMock.when(() -> ReflectionUtils.getConstructor(eq(DummySet.class), eq(Integer.TYPE))).thenReturn(dummyCtor);
        // Act
        Set<Integer> result = set.getNewSetWithSize(Integer.MAX_VALUE);
        // Assert
        assertNotNull(result);
        assertTrue(result instanceof DummySet, "Result should be a DummySet (size constructor mocked)");
        assertTrue(result.isEmpty(), "Newly created DummySet must be empty");
    }

    @Test
    void testGetNewSet_WhenConstructorLookupFails_FallsBackToDefaultSet() {
        // Arrange
        TestCompactSet<String> set = new TestCompactSet<>();
        // Simulate a failure while trying to obtain the constructor.
        reflectionUtilsMock.when(() -> ReflectionUtils.getConstructor(any(Class.class), eq(Integer.TYPE))).thenThrow(new RuntimeException("simulated lookup failure"));
        // Act
        Set<String> result = set.getNewSetWithSize(5);
        // Assert
        assertNotNull(result);
        assertTrue(result instanceof HashSet, "Fallback set should be a HashSet");
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetNewSet_WhenConstructorInvocationFails_FallsBackToDefaultSet() throws Exception {
        // Arrange
        BadSetCompactSet<String> set = new BadSetCompactSet<>();
        // Return the BadSet(int) constructor which throws when invoked.
        Constructor<BadSet> badCtor = BadSet.class.getConstructor(int.class);
        reflectionUtilsMock.when(() -> ReflectionUtils.getConstructor(eq(BadSet.class), eq(Integer.TYPE))).thenReturn(badCtor);
        // Act
        Set<String> result = set.getNewSetWithSize(20);
        // Assert
        assertNotNull(result);
        // The fallback is the no‑arg BadSet returned by getNewSet().
        assertTrue(result instanceof BadSet, "Fallback set should be the original BadSet instance");
        assertTrue(result.isEmpty());
    }
}
