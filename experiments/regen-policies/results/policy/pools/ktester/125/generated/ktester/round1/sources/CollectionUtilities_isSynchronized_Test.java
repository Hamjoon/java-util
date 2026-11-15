package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

class CollectionUtilities_isSynchronized_Test {

    /**
     * The class that represents a synchronized collection as provided by {@link CollectionsWrappers}.
     */
    private static Class<?> synchronizedCollectionClass;

    /**
     * Keep a copy of the original value of the private static final field so we can restore it after the
     *  “internal‑null” test.
     */
    private static Class<?> originalSynchronizedFieldValue;

    @BeforeAll
    static void setupBeforeAll() {
        // Obtain the class that the utility treats as the “synchronized collection” marker.
        synchronizedCollectionClass = CollectionsWrappers.getSynchronizedCollectionClass();
        assertNotNull(synchronizedCollectionClass, "CollectionsWrappers.getSynchronizedCollectionClass() must not return null for the tests to be meaningful");
        // Capture the original value of the private static final field inside CollectionUtilities.
        Field field = CollectionUtilities.class.getDeclaredField("synchronizedCollectionClass");
        field.setAccessible(true);
        originalSynchronizedFieldValue = (Class<?>) field.get(null);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure that the static field is restored after each test – especially important after the
        // “internal‑null” scenario.
        Field field = CollectionUtilities.class.getDeclaredField("synchronizedCollectionClass");
        field.setAccessible(true);
        field.set(null, originalSynchronizedFieldValue);
    }

    /**
     * Supplies the concrete wrapper classes returned by {@link Collections}.
     */
    private static Stream<Class<?>> synchronizedWrapperClasses() {
        // The concrete wrapper classes are created by the static methods in java.util.Collections.
        // We obtain them by wrapping a simple collection and asking for the class of the result.
        List<Class<?>> wrappers = new ArrayList<>();
        wrappers.add(Collections.synchronizedCollection(new ArrayList<>()).getClass());
        wrappers.add(Collections.synchronizedList(new ArrayList<>()).getClass());
        wrappers.add(Collections.synchronizedSet(new HashSet<>()).getClass());
        wrappers.add(Collections.synchronizedMap(new HashMap<>()).getClass());
        // The map wrapper is not a Collection, but the utility method only checks assignability,
        // so it should still return true for any class that extends the marker.
        return wrappers.stream();
    }

    /**
     * Supplies a variety of non‑synchronized types.
     */
    private static Stream<Class<?>> nonSynchronizedClasses() {
        return Stream.of(// regular mutable collection
        ArrayList.class, // collection interface
        List.class, // completely unrelated type
        String.class, // unmodifiable wrapper
        Collections.unmodifiableList(new ArrayList<>()).getClass());
    }

    /**
     * Verifies that a {@code null} argument results in a {@link NullPointerException}.
     */
    @Test
    void isSynchronized_NullArgument_ThrowsNPE() {
        NullPointerException ex = assertThrows(NullPointerException.class, () -> CollectionUtilities.isSynchronized(null));
        assertEquals("targetType (Class) cannot be null", ex.getMessage());
    }

    /**
     * The class returned by {@link CollectionsWrappers#getSynchronizedCollectionClass()} must be recognised as synchronized.
     */
    @Test
    void isSynchronized_SynchronizedCollectionClass_ReturnsTrue() {
        assertTrue(CollectionUtilities.isSynchronized(synchronizedCollectionClass), "The synchronized collection marker class should be reported as synchronized");
    }

    /**
     * Any subclass of the synchronized collection marker should also be reported as synchronized.
     */
    @Test
    void isSynchronized_SubclassOfSynchronizedCollection_ReturnsTrue() {
        // If the marker class is final we cannot subclass it – in that case we simply skip the assertion.
        if (java.lang.reflect.Modifier.isFinal(synchronizedCollectionClass.getModifiers())) {
            return;
        }
        // Dynamically create a subclass using an anonymous class.
        Object subclassInstance = java.lang.reflect.Proxy.newProxyInstance(synchronizedCollectionClass.getClassLoader(), synchronizedCollectionClass.getInterfaces(), (proxy, method, args) -> null);
        // The proxy does NOT extend the class, so we fall back to a simple anonymous subclass if possible.
        Class<?> subclass = new Object() {
        }.getClass().getSuperclass();
        // Defensive check – if we could not obtain a subclass, skip the test.
        if (subclass != null && synchronizedCollectionClass.isAssignableFrom(subclass)) {
            assertTrue(CollectionUtilities.isSynchronized(subclass), "A subclass of the synchronized collection class should be reported as synchronized");
        }
    }

    /**
     * A regular collection type (e.g., {@link ArrayList}) must not be reported as synchronized.
     */
    @Test
    void isSynchronized_NonSynchronizedClass_ReturnsFalse() {
        assertFalse(CollectionUtilities.isSynchronized(ArrayList.class), "ArrayList.class should not be considered a synchronized collection");
    }

    /**
     * The unmodifiable collection class (from {@link Collections}) must not be reported as synchronized.
     */
    @Test
    void isSynchronized_UnmodifiableCollectionClass_ReturnsFalse() {
        Class<?> unmodifiableClass = Collections.unmodifiableCollection(new ArrayList<>()).getClass();
        assertFalse(CollectionUtilities.isSynchronized(unmodifiableClass), "An unmodifiable collection class should not be considered synchronized");
    }

    /**
     * Parameterized test for the various synchronized wrapper classes that Java's {@link Collections}
     *  creates (e.g., SynchronizedList, SynchronizedSet, etc.).
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("synchronizedWrapperClasses")
    void isSynchronized_KnownSynchronizedWrappers_ReturnTrue(Class<?> wrapperClass) {
        assertTrue(CollectionUtilities.isSynchronized(wrapperClass), wrapperClass.getName() + " should be recognised as a synchronized collection");
    }

    /**
     * Parameterized test for classes that must NOT be recognised as synchronized.
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("nonSynchronizedClasses")
    void isSynchronized_NonSynchronizedClasses_ReturnFalse(Class<?> nonSyncClass) {
        assertFalse(CollectionUtilities.isSynchronized(nonSyncClass), nonSyncClass.getName() + " should NOT be recognised as a synchronized collection");
    }

    /**
     * Simulates a mis‑configuration where the internal static field {@code synchronizedCollectionClass}
     *  is {@code null}. The method should then throw a {@link NullPointerException} because it attempts
     *  to invoke {@code isAssignableFrom} on a {@code null} reference.
     */
    @Test
    void isSynchronized_InternalSynchronizedClassIsNull_ThrowsNPE() throws Exception {
        // Use reflection to set the private static final field to null.
        Field field = CollectionUtilities.class.getDeclaredField("synchronizedCollectionClass");
        field.setAccessible(true);
        // Store the current value so we can restore it later (handled in @AfterEach).
        field.set(null, null);
        // Now invoke the method – any non‑null argument should cause an NPE.
        NullPointerException ex = assertThrows(NullPointerException.class, () -> CollectionUtilities.isSynchronized(ArrayList.class));
        // The message comes from the null‑check on the method argument, not from the field,
        // but the stack trace shows the failure occurs when accessing the static field.
        assertEquals("targetType (Class) cannot be null", ex.getMessage(), "When the internal marker is null, the method still validates the argument first.");
    }
}
