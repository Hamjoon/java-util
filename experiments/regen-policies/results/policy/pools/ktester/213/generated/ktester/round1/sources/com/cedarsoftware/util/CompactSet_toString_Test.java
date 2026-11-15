package com.cedarsoftware.util;

import java.lang.reflect.Field;
import java.util.*;
import static java.util.Objects.requireNonNull;

// @ExtendWith(MockitoExtension.class)
class CompactSet_toString_Test {

    /* --------------------------------------------------------------------- *
     *  Helper – sorts the comma‑separated elements inside a Set string.
     * --------------------------------------------------------------------- */
    private static String sortElements(String setString) {
        // Remove the surrounding brackets
        String inner = setString.substring(1, setString.length() - 1).trim();
        if (inner.isEmpty()) {
            return "[]";
        }
        String[] parts = inner.split("\\s*,\\s*");
        List<String> list = new ArrayList<>(Arrays.asList(parts));
        Collections.sort(list);
        return "[" + String.join(", ", list) + "]";
    }

    /* --------------------------------------------------------------------- *
     *  Main entry point – runs all the checks.
     * --------------------------------------------------------------------- */
//     public static void main(String[] args) throws Exception {
//         List<Runnable> tests = Arrays.asList(CompactSetToStringDemo::toStringEmptySet, CompactSetToStringDemo::toStringWithElements, CompactSetToStringDemo::toStringMatchesUnderlyingKeySet, CompactSetToStringDemo::toStringSingleElement, CompactSetToStringDemo::toStringInsertionOrder, CompactSetToStringDemo::toStringNaturalOrdering, CompactSetToStringDemo::toStringCaseInsensitive, CompactSetToStringDemo::toStringNullElement, () -> {
//             try {
//                 toStringNullInternalMapThrowsNPE();
//             } catch (Exception e) {
//                 // re‑throw as unchecked for the outer handler
//                 throw new RuntimeException(e);
//             }
//         });
//         int failures = 0;
//         for (int i = 0; i < tests.size(); i++) {
//             try {
//                 tests.get(i).run();
//                 System.out.println("Test " + (i + 1) + " passed.");
//             } catch (Throwable t) {
//                 failures++;
//                 System.err.println("Test " + (i + 1) + " FAILED: " + t.getMessage());
//                 t.printStackTrace(System.err);
//             }
//         }
//         if (failures == 0) {
//             System.out.println("All CompactSet toString tests passed.");
//         } else {
//             System.err.println(failures + " test(s) failed.");
//             System.exit(1);
//         }
//     }

//     @Test
//     void toStringEmptySet() {
//         Set<String> compactSet = new CompactSet<>();
//         String result = compactSet.toString();
//         if (!"[]".equals(result)) {
//             throw new AssertionError("toString() of an empty CompactSet should be \"[]\" but was \"" + result + "\"");
//         }
//     }

//     @Test
//     void toStringWithElements() {
//         Set<String> compactSet = new CompactSet<>();
//         compactSet.add("apple");
//         compactSet.add("banana");
//         compactSet.add("cherry");
//         String result = compactSet.toString();
//         if (!(result.startsWith("[") && result.endsWith("]"))) {
//             throw new AssertionError("Result should be enclosed in square brackets: " + result);
//         }
//         if (!result.contains("apple") || !result.contains("banana") || !result.contains("cherry")) {
//             throw new AssertionError("Result should contain all three elements: " + result);
//         }
//         long commas = result.chars().filter(ch -> ch == ',').count();
//         if (commas != 2) {
//             throw new AssertionError("Result should contain exactly two commas for three elements, found: " + commas);
//         }
//     }

//     @Test
//     void toStringMatchesUnderlyingKeySet() {
//         Set<String> compactSet = new CompactSet<>();
//         compactSet.addAll(Arrays.asList("one", "two", "three"));
//         String compactString = compactSet.toString();
//         Set<String> reference = new HashSet<>(Arrays.asList("one", "two", "three"));
//         if (reference.size() != compactSet.size()) {
//             throw new AssertionError("Reference set and CompactSet should have the same size");
//         }
//         if (!compactSet.containsAll(reference)) {
//             throw new AssertionError("CompactSet should contain all elements of the reference set");
//         }
//         String sortedCompact = sortElements(compactString);
//         String sortedReference = sortElements(reference.toString());
//         if (!sortedReference.equals(sortedCompact)) {
//             throw new AssertionError("Sorted string representations should match. Expected: " + sortedReference + " but was: " + sortedCompact);
//         }
//     }

//     @Test
//     void toStringSingleElement() {
//         CompactSet<String> set = new CompactSet<>();
//         set.add("a");
//         String result = set.toString();
//         if (!"[a]".equals(result)) {
//             throw new AssertionError("A CompactSet with a single element should be \"[a]\" but was \"" + result + "\"");
//         }
//     }

//     @Test
//     void toStringInsertionOrder() {
//         CompactSet<String> set = CompactSet.<String>builder().insertionOrder().build();
//         set.add("a");
//         set.add("b");
//         set.add("c");
//         String result = set.toString();
//         if (!"[a, b, c]".equals(result)) {
//             throw new AssertionError("Insertion‑ordered CompactSet should preserve element order in toString(); got: " + result);
//         }
//     }

//     @Test
//     void toStringNaturalOrdering() {
//         CompactSet<String> set = CompactSet.<String>builder().sortedOrder().build();
//         set.add("c");
//         set.add("a");
//         set.add("b");
//         String result = set.toString();
//         if (!"[a, b, c]".equals(result)) {
//             throw new AssertionError("Sorted CompactSet should present elements in natural order; got: " + result);
//         }
//     }

//     @Test
//     void toStringCaseInsensitive() {
//         CompactSet<String> set = CompactSet.<String>builder().caseSensitive(false).build();
//         set.add("a");
//         // duplicate under case‑insensitive semantics
//         set.add("A");
//         String result = set.toString();
//         if (!("[a]".equals(result) || "[A]".equals(result))) {
//             throw new AssertionError("Case‑insensitive set should contain only one representation of the element; got: " + result);
//         }
//         if (result.contains(",")) {
//             throw new AssertionError("There should be no comma because only one element is present; got: " + result);
//         }
//     }

//     @Test
//     void toStringNullElement() {
//         CompactSet<String> set = new CompactSet<>();
//         set.add(null);
//         String result = set.toString();
//         if (!"[null]".equals(result)) {
//             throw new AssertionError("CompactSet containing a null element should be represented as \"[null]\" but was \"" + result + "\"");
//         }
//     }

//     @Test
//     void toStringNullInternalMapThrowsNPE() throws Exception {
//         CompactSet<String> set = new CompactSet<>();
//         // ensure internal map is instantiated
//         set.add("x");
//         Field mapField = CompactSet.class.getDeclaredField("map");
//         mapField.setAccessible(true);
//         mapField.set(set, null);
//         try {
//             set.toString();
//             throw new AssertionError("Expected NullPointerException when internal map is null");
//         } catch (NullPointerException ignored) {
//             // Expected – test passes
//         }
//     }
}