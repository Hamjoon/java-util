package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mockito;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;

@ExtendWith(MockitoExtension.class)
class ConcurrentSet_toString_Test {

    class FooBar {

        @Override
        public String toString() {
            return "foo, bar";
        }
    }

    class Wrapper {

        private final Set<?> referenceSet;

        Wrapper(Set<?> referenceSet) {
            this.referenceSet = referenceSet;
        }

        @Override
        public boolean equals(Object o) {
            return o == referenceSet;
        }

        @Override
        public int hashCode() {
            // arbitrary constant
            return 42;
        }

        @Override
        public String toString() {
            return "Wrapper";
        }
    }

    class NullIteratorSet extends ConcurrentSet<Object> {

        @Override
        public Iterator<Object> iterator() {
            return null;
        }
    }

    class BadIteratorSet extends ConcurrentSet<Object> {

        @Override
        public Iterator<Object> iterator() {
            return badIt;
        }
    }

    class BadNextSet extends ConcurrentSet<Object> {

        @Override
        public Iterator<Object> iterator() {
            return badIt;
        }
    }

    class BadElement {

        @Override
        public String toString() {
            throw new RuntimeException("element failure");
        }
    }

    class FooBar {

        @Override
        public String toString() {
            return "foo, bar";
        }
    }

    class Wrapper {

        private final Set<?> referenceSet;

        Wrapper(Set<?> referenceSet) {
            this.referenceSet = referenceSet;
        }

        @Override
        public boolean equals(Object o) {
            return o == referenceSet;
        }

        @Override
        public int hashCode() {
            // arbitrary constant
            return 42;
        }

        @Override
        public String toString() {
            return "Wrapper";
        }
    }

    class NullIteratorSet extends ConcurrentSet<Object> {

        @Override
        public Iterator<Object> iterator() {
            return null;
        }
    }

    class BadIteratorSet extends ConcurrentSet<Object> {

        @Override
        public Iterator<Object> iterator() {
            return badIt;
        }
    }

    class BadNextSet extends ConcurrentSet<Object> {

        @Override
        public Iterator<Object> iterator() {
            return badIt;
        }
    }

    class BadElement {

        @Override
        public String toString() {
            throw new RuntimeException("element failure");
        }
    }

    @Test
    void testToStringEmptySet() {
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        assertEquals("{}", set.toString(), "Empty set should be represented as {}");
    }

    @Test
    void testToStringSingleElement() {
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        set.add(42);
        assertEquals("{42}", set.toString(), "Set with a single element should be formatted correctly");
    }

    @Test
    void testToStringMultipleElements() {
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList(1, 2, 3));
        String result = set.toString();
        // Basic format checks
        assertTrue(result.startsWith("{") && result.endsWith("}"), "Result should start with '{' and end with '}'");
        // Strip braces and split on ", "
        String inner = result.substring(1, result.length() - 1);
        Set<String> actualElements = Arrays.stream(inner.split(", ")).map(String::trim).collect(Collectors.toSet());
        Set<String> expectedElements = new HashSet<>(Arrays.asList("1", "2", "3"));
        assertEquals(expectedElements, actualElements, "All elements must appear exactly once, order is not guaranteed");
    }

    @Test
    void testToStringContainsNull() {
        ConcurrentSet<String> set = new ConcurrentSet<>();
        set.add(null);
        assertEquals("{null}", set.toString(), "A set containing only null should be represented as {null}");
    }

    @Test
    void testToStringSelfReference() {
        // Use raw type to allow adding the set itself
        @SuppressWarnings("unchecked")
        ConcurrentSet<Object> set = new ConcurrentSet<>();
        // Adding the set itself should be allowed (the class does not forbid it)
        assertTrue(set.add(set), "Adding the set to itself should succeed");
        String result = set.toString();
        assertEquals("{(this Set)}", result, "Self‑reference element must be rendered as \"(this Set)\"");
    }

    @Test
    void testToStringMixedElementsIncludingSelfAndNull() {
        @SuppressWarnings("unchecked")
        ConcurrentSet<Object> set = new ConcurrentSet<>();
        set.add(1);
        set.add(null);
        // self‑reference
        set.add(set);
        String result = set.toString();
        // Expected elements as strings
        Set<String> expected = new HashSet<>(Arrays.asList("1", "null", "(this Set)"));
        // Strip braces and split
        String inner = result.substring(1, result.length() - 1);
        Set<String> actual = Arrays.stream(inner.split(", ")).map(String::trim).collect(Collectors.toSet());
        assertEquals(expected, actual, "String representation must contain 1, null and the self‑reference marker");
    }

    @Test
    void testDuplicateElements() {
        // Adding the same element twice should not create duplicate output
        ConcurrentSet<String> set = new ConcurrentSet<>();
        set.add("apple");
        // duplicate, ignored by Set semantics
        set.add("apple");
        assertEquals("{apple}", set.toString(), "Duplicate element should appear only once in the string representation");
    }

    @Test
    void testCustomObjectElement() {
        // Object whose toString contains a comma and a space
        class FooBar {

            @Override
            public String toString() {
                return "foo, bar";
            }
        }
        ConcurrentSet<Object> set = new ConcurrentSet<>();
        set.add(new FooBar());
        // The set's toString does not escape commas, so we expect the raw string
        assertEquals("{foo, bar}", set.toString(), "Custom object's toString should be inserted verbatim");
    }

    @Test
    void testLargeSet() {
        // Add ten distinct integers (0‑9)
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        IntStream.range(0, 10).forEach(set::add);
        String result = set.toString();
        // Basic format checks
        assertTrue(result.startsWith("{") && result.endsWith("}"), "Result must start with '{' and end with '}'");
        // Split and count elements
        String inner = result.substring(1, result.length() - 1);
        List<String> tokens = Arrays.stream(inner.split(", ")).map(String::trim).collect(Collectors.toList());
        assertEquals(10, tokens.size(), "Large set should contain exactly ten element tokens");
        // Verify that each number appears once (order not guaranteed)
        Set<String> expected = IntStream.range(0, 10).mapToObj(Integer::toString).collect(Collectors.toSet());
        assertEquals(expected, new HashSet<>(tokens), "All numbers 0‑9 must be present");
    }

    @Test
    // safeguard against deadlock
    @Timeout(2)
    void testConcurrentModificationDuringToString() throws Exception {
        // Prepare a set with a few elements
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList(1, 2, 3));
        // Latch to make sure the background thread adds after iteration has started
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(1);
        Thread modifier = new Thread(() -> {
            try {
                // Wait until the main thread has entered toString()
                startLatch.await();
                // concurrent addition
                set.add(4);
            } catch (InterruptedException ignored) {
            } finally {
                doneLatch.countDown();
            }
        });
        modifier.start();
        // Wrap iterator to signal when iteration begins
        ConcurrentSet<Integer> spySet = Mockito.spy(set);
        Iterator<Integer> originalIt = set.iterator();
        Iterator<Integer> signallingIt = new Iterator<Integer>() {

            private final Iterator<Integer> delegate = originalIt;

            private boolean first = true;

            @Override
            public boolean hasNext() {
                if (first) {
                    first = false;
                    // signal that iteration started
                    startLatch.countDown();
                }
                return delegate.hasNext();
            }

            @Override
            public Integer next() {
                return delegate.next();
            }
        };
        doReturn(signallingIt).when(spySet).iterator();
        // Execute toString while the background thread may modify the set
        String result = spySet.toString();
        // Ensure the background thread finished
        doneLatch.await();
        // No exception should have been thrown; result must be well‑formed
        assertTrue(result.startsWith("{") && result.endsWith("}"), "Result must still be well‑formed despite concurrent modification");
        // At minimum the original elements should be present
        String inner = result.substring(1, result.length() - 1);
        Set<String> elements = Arrays.stream(inner.split(", ")).map(String::trim).collect(Collectors.toSet());
        Set<String> expectedOriginal = new HashSet<>(Arrays.asList("1", "2", "3"));
        assertTrue(elements.containsAll(expectedOriginal), "Result should contain at least the elements present before iteration started");
    }

    @Test
    void testSelfReferenceDuplicate() {
        @SuppressWarnings("unchecked")
        ConcurrentSet<Object> set = new ConcurrentSet<>();
        // Adding the same set instance twice (second add is ignored)
        assertTrue(set.add(set));
        assertFalse(set.add(set), "Second add of the same reference should be ignored");
        assertEquals("{(this Set)}", set.toString(), "Self‑reference placeholder must appear only once");
    }

    @Test
    void testEqualButNotSameElement() {
        // Wrapper whose equals returns true when compared to the set instance,
        // but is a distinct object (different reference)
        class Wrapper {

            private final Set<?> referenceSet;

            Wrapper(Set<?> referenceSet) {
                this.referenceSet = referenceSet;
            }

            @Override
            public boolean equals(Object o) {
                return o == referenceSet;
            }

            @Override
            public int hashCode() {
                // arbitrary constant
                return 42;
            }

            @Override
            public String toString() {
                return "Wrapper";
            }
        }
        ConcurrentSet<Object> set = new ConcurrentSet<>();
        Wrapper wrapper = new Wrapper(set);
        set.add(wrapper);
        // The set's toString checks identity (==) not equals(), so we expect "Wrapper"
        assertEquals("{Wrapper}", set.toString(), "Element equal to the set but not the same reference should be rendered via its toString()");
    }

    @Test
    void testNullIteratorThrowsNPE() {
        // Subclass that returns null from iterator()
        class NullIteratorSet extends ConcurrentSet<Object> {

            @Override
            public Iterator<Object> iterator() {
                return null;
            }
        }
        ConcurrentSet<Object> set = new NullIteratorSet();
        assertThrows(NullPointerException.class, set::toString, "toString() should throw NPE when iterator() returns null");
    }

    @Test
    void testIteratorHasNextThrowsException() {
        // Mock iterator whose hasNext() always throws RuntimeException
        @SuppressWarnings("unchecked")
        Iterator<Object> badIt = mock(Iterator.class);
        when(badIt.hasNext()).thenThrow(new RuntimeException("boom"));
        class BadIteratorSet extends ConcurrentSet<Object> {

            @Override
            public Iterator<Object> iterator() {
                return badIt;
            }
        }
        ConcurrentSet<Object> set = new BadIteratorSet();
        RuntimeException ex = assertThrows(RuntimeException.class, set::toString, "Exception from iterator.hasNext() must propagate");
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void testIteratorNextThrowsException() {
        // Mock iterator: first hasNext() true, then next() throws NoSuchElementException,
        // subsequent hasNext() false
        @SuppressWarnings("unchecked")
        Iterator<Object> badIt = mock(Iterator.class);
        when(badIt.hasNext()).thenReturn(true).thenReturn(false);
        when(badIt.next()).thenThrow(new NoSuchElementException("no element"));
        class BadNextSet extends ConcurrentSet<Object> {

            @Override
            public Iterator<Object> iterator() {
                return badIt;
            }
        }
        ConcurrentSet<Object> set = new BadNextSet();
        NoSuchElementException ex = assertThrows(NoSuchElementException.class, set::toString, "Exception from iterator.next() must propagate");
        assertEquals("no element", ex.getMessage());
    }

    @Test
    void testElementToStringThrowsException() {
        // Element whose toString throws RuntimeException
        class BadElement {

            @Override
            public String toString() {
                throw new RuntimeException("element failure");
            }
        }
        ConcurrentSet<Object> set = new ConcurrentSet<>();
        set.add(new BadElement());
        RuntimeException ex = assertThrows(RuntimeException.class, set::toString, "Exception from element.toString() must propagate");
        assertEquals("element failure", ex.getMessage());
    }
}
