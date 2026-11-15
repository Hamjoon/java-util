package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class CollectionUtilities_isSynchronized_Test {

    /**
     * Holds the original value of the private static field so it can be restored after each test.
     */
    private static Class<?> originalSyncClass;

    class DummySyncBase {
    }

    class DummySyncBase {
    }

    class CustomSyncList extends DummySyncBase {
    }

    class DummySyncBase {
    }

    class CustomSyncList extends DummySyncBase {
    }

    class CustomSyncListSubclass extends CustomSyncList {
    }

    class DummySyncBase {
    }

    class DummySyncBase {
    }

    class CustomSyncList extends DummySyncBase {
    }

    class DummySyncBase {
    }

    class CustomSyncList extends DummySyncBase {
    }

    class CustomSyncListSubclass extends CustomSyncList {
    }

    @BeforeEach
    void setupBeforeEach() {
        // Capture the original value of the private static field before each test.
        Field f = CollectionUtilities.class.getDeclaredField("synchronizedCollectionClass");
        f.setAccessible(true);
        originalSyncClass = (Class<?>) f.get(null);
    }

    @AfterEach
    void teardownAfterEach() {
        // Restore the original value to avoid side‑effects between tests.
        setSynchronizedCollectionClass(originalSyncClass);
    }

    /**
     * Sets {@code CollectionUtilities.synchronizedCollectionClass} to the supplied class
     * (or {@code null}) using reflection.
     */
    private static void setSynchronizedCollectionClass(Class<?> clazz) throws Exception {
        Field f = CollectionUtilities.class.getDeclaredField("synchronizedCollectionClass");
        f.setAccessible(true);
        f.set(null, clazz);
    }

    @Test
    @DisplayName("isSynchronized returns true for a class that is a synchronized collection")
    void returnsTrueForSynchronizedCollectionClass() {
        // Arrange – create a synchronized collection and obtain its runtime class
        List<String> syncList = Collections.synchronizedList(new ArrayList<>());
        Class<?> syncClass = syncList.getClass();
        // Act
        boolean result = CollectionUtilities.isSynchronized(syncClass);
        // Assert
        assertTrue(result, "Expected the class of a synchronized collection to be recognised as synchronized");
    }

    @Test
    @DisplayName("isSynchronized returns false for a non‑synchronized collection class")
    void returnsFalseForNonSynchronizedCollectionClass() {
        // Arrange – a regular (non‑synchronized) collection class
        Class<?> regularClass = ArrayList.class;
        // Act
        boolean result = CollectionUtilities.isSynchronized(regularClass);
        // Assert
        assertFalse(result, "Expected a regular collection class not to be recognised as synchronized");
    }

    @Test
    @DisplayName("isSynchronized throws NPE when targetType is null")
    void throwsNPEWhenTargetTypeIsNull() {
        // Act & Assert
        NullPointerException ex = assertThrows(NullPointerException.class, () -> CollectionUtilities.isSynchronized(null));
        assertEquals("targetType (Class) cannot be null", ex.getMessage(), "Expected NPE message to match the contract");
    }

    @Test
    @DisplayName("isSynchronized returns true when the exact synchronized wrapper class is supplied")
    void exactSynchronizedWrapperReturnsTrue() throws Exception {
        // Use a dummy base class as the synchronized wrapper.
        class DummySyncBase {
        }
        setSynchronizedCollectionClass(DummySyncBase.class);
        boolean result = CollectionUtilities.isSynchronized(DummySyncBase.class);
        assertTrue(result, "The exact synchronized wrapper class should be recognised as synchronized");
    }

    @Test
    @DisplayName("isSynchronized returns true for a direct subclass of the synchronized wrapper")
    void subclassOfSynchronizedWrapperReturnsTrue() throws Exception {
        // Define a dummy base class and a subclass.
        class DummySyncBase {
        }
        class CustomSyncList extends DummySyncBase {
        }
        setSynchronizedCollectionClass(DummySyncBase.class);
        boolean result = CollectionUtilities.isSynchronized(CustomSyncList.class);
        assertTrue(result, "A subclass of the synchronized wrapper must be considered synchronized");
    }

    @Test
    @DisplayName("isSynchronized returns true for an indirect subclass of the synchronized wrapper")
    void indirectSubclassOfSynchronizedWrapperReturnsTrue() throws Exception {
        // Define a two‑level inheritance chain.
        class DummySyncBase {
        }
        class CustomSyncList extends DummySyncBase {
        }
        class CustomSyncListSubclass extends CustomSyncList {
        }
        setSynchronizedCollectionClass(DummySyncBase.class);
        boolean result = CollectionUtilities.isSynchronized(CustomSyncListSubclass.class);
        assertTrue(result, "An indirect subclass (sub‑subclass) should still be recognised as synchronized");
    }

    @ParameterizedTest(name = "{index} => targetType={0}")
    @EnumSource(TargetType.class)
    @DisplayName("isSynchronized returns false for interface types")
    void interfaceTypesReturnFalse(TargetType target) {
        // Interfaces should never be assignable from the synchronized wrapper class.
        boolean result = CollectionUtilities.isSynchronized(target.clazz);
        assertFalse(result, "Interface " + target.clazz.getName() + " must not be considered synchronized");
    }

    @Test
    @DisplayName("isSynchronized returns false for a superclass of the synchronized wrapper")
    void superclassOfSynchronizedWrapperReturnsFalse() throws Exception {
        // Use the real synchronized wrapper class (obtained from CollectionsWrappers) as the base.
        Class<?> syncBase = CollectionsWrappers.getSynchronizedCollectionClass();
        // AbstractCollection is a superclass of most collection implementations.
        boolean result = CollectionUtilities.isSynchronized(AbstractCollection.class);
        // The result must be false regardless of the actual syncBase.
        assertFalse(result, "Superclass of the synchronized wrapper must not be considered synchronized");
    }

    @Test
    @DisplayName("isSynchronized returns false for non‑collection concrete classes")
    void nonCollectionConcreteClassesReturnFalse() {
        assertAll(() -> assertFalse(CollectionUtilities.isSynchronized(Integer.class), "Integer is not a collection and must return false"), () -> assertFalse(CollectionUtilities.isSynchronized(String.class), "String is not a collection and must return false"));
    }

    @Test
    @DisplayName("isSynchronized throws NullPointerException when internal synchronizedCollectionClass is null")
    void nullInternalSynchronizedClassThrowsNPE() throws Exception {
        // Force the internal field to null.
        setSynchronizedCollectionClass(null);
        NullPointerException npe = assertThrows(NullPointerException.class, () -> CollectionUtilities.isSynchronized(ArrayList.class));
        // The message comes from the attempt to call isAssignableFrom on a null reference,
        // so we only assert that an NPE is thrown.
        assertNotNull(npe);
    }
}
