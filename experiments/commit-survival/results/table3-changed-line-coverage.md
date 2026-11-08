# Table 3. Changed-line coverage

Coverage uses methods alive entering each index. Changed-line coordinates are the post-image added/modified lines. Build failures and missing coverage remain unavailable.

## All files

| Index | Subject | Executable lines | Dev (scope) covered | KTester covered | Ouédraogo covered | Dev (scope) lost | KTester lost | Ouédraogo lost |
|---:|---|---:|---:|---:|---:|---:|---:|---:|
Unavailable: batch execution measures scope files only.
## Scope files

| Index | Subject | Executable lines | Dev (scope) covered | KTester covered | Ouédraogo covered | Dev (scope) lost | KTester lost | Ouédraogo lost |
|---:|---|---:|---:|---:|---:|---:|---:|---:|
| 15 | Significant update to allow wrapped collection types to be supported.  Beginning removal of Sealed* as that will no longer be needed now that we support Unmodifiable* wrappers. | 53 | 17 (32.1%) | 0 (0.0%) | 8 (15.1%) | 0 | 0 | 0 |
| 19 | - CompactMap has new factory constructor methods so that no subclassing of CompactMap is necessary to support alternative behaviors (compactSize, cpaacity, case-sensitivity, Map type to use, source Map, usage of copying iterators, single-key setting) - Starting to break-out put/get so that it will honor ordering-based Maps. | 278 | 181 (65.1%) | 163 (58.6%) | 203 (73.0%) | 0 | 0 | 0 |
| 20 | - CompactMap has new factory constructor methods so that no subclassing of CompactMap is necessary to support alternative behaviors (compactSize, cpaacity, case-sensitivity, Map type to use, source Map, usage of copying iterators, single-key setting) - Starting to break-out put/get so that it will honor ordering-based Maps. | 51 | 22 (43.1%) | 23 (45.1%) | 25 (49.0%) | 0 | 0 | 0 |
| 21 | CompactMap now handling ordering - natural order, sorted, reverse sorted, and unordered | 46 | 6 (13.0%) | 6 (13.0%) | 6 (13.0%) | 0 | 0 | 0 |
| 22 | CompactMap - case-sensitive flag fully honored when no comparator set.  If comparator set, then it is ignored. CompactMap - documentation for class and newMap fully blown out. | 21 | 2 (9.5%) | 2 (9.5%) | 2 (9.5%) | 0 | 0 | 0 |
| 24 | almost there - struggling with sorting... | 89 | 38 (42.7%) | 34 (38.2%) | 38 (42.7%) | 0 | 0 | 0 |
| 25 | - I have a working version of CompactMap with all tests passing (all java-util tests) | 34 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 26 | saving off correct copy before final optimizations | 30 | 2 (6.7%) | 3 (10.0%) | 3 (10.0%) | 0 | 0 | 0 |
| 27 | - updated markdown | 49 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 28 | Fixed some edge cases with respect to case insensitivity | 85 | 8 (9.4%) | 8 (9.4%) | 8 (9.4%) | 0 | 0 | 0 |
| 30 | Fighting backwards compatibility... | 81 | 4 (4.9%) | 4 (4.9%) | 4 (4.9%) | 0 | 21 | 2 |
| 31 | - ReflectionUtils massively updated (improved, complete adjustible LRU caching) and new API to easily get methods (deep or shallow) - CompactMap now supports old construction patterns while offering new improved factory methods. - CompactMap factory method approach will be replaced with Builder pattern before this is released. | 328 | 246 (75.0%) | 124 (37.8%) | 187 (57.0%) | 1 | 3 | 1 |
| 32 | Added more tests to ensure old-style CompactMap construction works in all cases.  Fixed a couple of bugs it uncovered. | 56 | 49 (87.5%) | 19 (33.9%) | 23 (41.1%) | 0 | 0 | 0 |
| 33 | minor code fixes | 3 | 3 (100.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 34 | warning fixes | 5 | 4 (80.0%) | 4 (80.0%) | 5 (100.0%) | 0 | 0 | 0 |
| 35 | CompactMap now keeps items in insertion order, and only sorts the keys when changing to an iterator view, greatly improving performance. | 27 | 26 (96.3%) | 9 (33.3%) | 14 (51.9%) | 0 | 0 | 0 |
| 36 | - remove() sped up dramatically - no sorting during remove. | 31 | 30 (96.8%) | 7 (22.6%) | 6 (19.4%) | 0 | 0 | 0 |
| 37 | - removed getNewMap(int size) as it was not always overwritten by users, leaving inconsistent behavior if they overwrote getNewMap() to return TreeMap(case insensitive, reverse order), but did not overwrite getNewMap(int size) to be the same way.  This improves correctness and avoids the customer running into hard-to-solve bugs. | 3 | 3 (100.0%) | 0 (0.0%) | 3 (100.0%) | 0 | 0 | 0 |
| 38 | minor code changes for consistency | 27 | 20 (74.1%) | 7 (25.9%) | 7 (25.9%) | 0 | 0 | 0 |
| 39 | prep for conversion to builder pattern | 4 | 2 (50.0%) | 2 (50.0%) | 2 (50.0%) | 0 | 0 | 0 |
| 40 | CompactMap builder pattern added. More tests added. Adding more tests until code coverage is complete. | 27 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 41 | - migrating toward a solution that creates classes with static final constants so that users do not have to create subclasses. | 20 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 42 | dynamically generating subclass for CompactMap based on options set. | 159 | 7 (4.4%) | 4 (2.5%) | 4 (2.5%) | 0 | 0 | 0 |
| 43 | CompactMap automatic subclass generation for constants now complete.  CompactMap has one and only one member variable, 'val.'  And the class name will always be CompactMap$<options> | 40 | 3 (7.5%) | 6 (15.0%) | 6 (15.0%) | 0 | 0 | 2 |
| 44 | code cleanup | 2 | 2 (100.0%) | 2 (100.0%) | 2 (100.0%) | 0 | 0 | 0 |
| 45 | ThreadLocal removed. Temporary state.  New to remove getNewMap() calls making determinations about maptype. | 28 | 19 (67.9%) | 12 (42.9%) | 12 (42.9%) | 0 | 0 | 0 |
| 46 | - all tests passing - narrowing the gap between the legacy creation and new builder creation | 56 | 5 (8.9%) | 3 (5.4%) | 3 (5.4%) | 0 | 0 | 0 |
| 47 | Finally taking shape.  More documentation coming, should be near final version. | 88 | 54 (61.4%) | 12 (13.6%) | 15 (17.0%) | 0 | 0 | 0 |
| 48 | minor code cleanup | 2 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 49 | adding more generality to differing Map types to be supported. | 101 | 46 (45.5%) | 39 (38.6%) | 44 (43.6%) | 0 | 0 | 0 |
| 50 | - Legacy mode and new "template" mode are completely integrated. | 9 | 8 (88.9%) | 4 (44.4%) | 4 (44.4%) | 0 | 0 | 0 |
| 51 | added better error handling when user specifies incorrect class instead of Map.  Also, can handle Map that does not take capacity. | 62 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 52 | More readable template | 41 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 53 | - removed capacity. Should never have been added.  what was i thinking? | 35 | 1 (2.9%) | 2 (5.7%) | 2 (5.7%) | 0 | 0 | 0 |
| 54 | Beefing up Javadoc | 1 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 55 | - Further blow-out of Javadoc - CsaeInsensitiveMap's wrapped Map maintained. | 19 | 4 (21.1%) | 2 (10.5%) | 4 (21.1%) | 0 | 0 | 0 |
| 59 | - finished javadocs | 6 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 60 | - reduce API surface area - Use better approach to get classLoader | 1 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 61 | DeepEquals breadcrumb trails | 481 | 401 (83.4%) | 264 (54.9%) | 361 (75.1%) | 0 | 5 | 3 |
| 62 | deep equals breadcrumb gen in progress... | 236 | 206 (87.3%) | 145 (61.4%) | 197 (83.5%) | 0 | 0 | 0 |
| 63 | - bread crumb output further refined. - Improved stack handling in deepEquals() | 93 | 68 (73.1%) | 65 (69.9%) | 0 (0.0%) | 0 | 0 | 110 |
| 64 | formatting deepEquals differences getting much better... | 279 | 187 (67.0%) | 124 (44.4%) | 0 (0.0%) | 0 | 1 | 0 |
| 65 | - working out formatting delta issues... | 230 | 160 (69.6%) | 105 (45.7%) | 0 (0.0%) | 0 | 0 | 0 |
| 66 | - Adding the concept of a container that contains an ItemToCompare | 6 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 67 | improving DeepEquals breadcrumb output. | 283 | 243 (85.9%) | 227 (80.2%) | 0 (0.0%) | 0 | 0 | 0 |
| 68 | - Formatting for deepEquals "diff" nearing completion.  Worrying about "fonts" and "colors" now (j/k) - ReflectionUtils is finished | 379 | 293 (77.3%) | 102 (26.9%) | 139 (36.7%) | 4 | 5 | 1 |
| 69 | -perfecting deepEquals() format.  Just a few small items left. | 13 | 11 (84.6%) | 9 (69.2%) | 0 (0.0%) | 0 | 0 | 0 |
| 70 | - formatting for deepEquals, further improved. | 3 | 3 (100.0%) | 2 (66.7%) | 0 (0.0%) | 0 | 0 | 0 |
| 71 | - improved comparison between simple types - use compareTo() == 0 if they are Comparable, before using .equals() | 5 | 5 (100.0%) | 5 (100.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 72 | - formatting improvements. | 34 | 32 (94.1%) | 10 (29.4%) | 0 (0.0%) | 0 | 0 | 0 |
| 73 | - great headway made on deepEquals() difference output | 262 | 175 (66.8%) | 138 (52.7%) | 0 (0.0%) | 0 | 0 | 0 |
| 74 | - formatting just about complete for deepEquals output differences. | 49 | 37 (75.5%) | 24 (49.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 75 | - formatting nearly complete | 81 | 42 (51.9%) | 30 (37.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 76 | - perfecting deepEquals diff output | 21 | 20 (95.2%) | 15 (71.4%) | 0 (0.0%) | 0 | 0 | 0 |
| 77 | nearing "art" form... on diff formatting. | 19 | 17 (89.5%) | 11 (57.9%) | 0 (0.0%) | 0 | 0 | 0 |
| 78 | - perfecting a little further | 1 | 1 (100.0%) | 1 (100.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 79 | - I think we have it. "diff" is now formatted consistently | 66 | 59 (89.4%) | 49 (74.2%) | 0 (0.0%) | 0 | 0 | 0 |
| 80 | - minor tweak to deepEquals output | 5 | 4 (80.0%) | 1 (20.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 81 | - almost final final | 138 | 116 (84.1%) | 99 (71.7%) | 0 (0.0%) | 0 | 0 | 0 |
| 82 | - working through support of key of map being map itself - no stackoverflow | 74 | 49 (66.2%) | 32 (43.2%) | 0 (0.0%) | 0 | 0 | 0 |
| 83 | - working, but with stackoverflow on Maps with Map instance as key | 23 | 12 (52.2%) | 11 (47.8%) | 0 (0.0%) | 0 | 0 | 0 |
| 84 | - solved stackoverflow issue | 33 | 17 (51.5%) | 10 (30.3%) | 0 (0.0%) | 0 | 0 | 0 |
| 85 | - almost there, why .value? | 117 | 10 (8.5%) | 7 (6.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 86 | - difference type description now precise | 47 | 32 (68.1%) | 28 (59.6%) | 0 (0.0%) | 0 | 0 | 0 |
| 87 | RC1 | 39 | 39 (100.0%) | 31 (79.5%) | 0 (0.0%) | 0 | 0 | 0 |
| 88 | DeepEquals "diff" breadcrumb output complete. | 37 | 26 (70.3%) | 17 (45.9%) | 0 (0.0%) | 0 | 1 | 0 |
| 89 | Deprecated CompactSet derivative classes. Use the builder pattern instead. | 4 | 3 (75.0%) | 2 (50.0%) | 1 (25.0%) | 1 | 0 | 1 |
| 90 | Nearing Release. minor clean ups, Javadoc | 5 | 3 (60.0%) | 1 (20.0%) | 4 (80.0%) | 0 | 0 | 0 |
| 91 | "newInstance" brought over to ClassUtilities from json-io. | 224 | 43 (19.2%) | 41 (18.3%) | 40 (17.9%) | 0 | 0 | 0 |
| 92 | - ReflectionUtils - increased flexibility in filtering fields | 50 | 35 (70.0%) | 20 (40.0%) | 39 (78.0%) | 0 | 0 | 0 |
| 94 | - CompactMap now properly caches the classes it generates. It also locks at a per classname level, increasing parallel execution. - More Javadoc updates. | 35 | 29 (82.9%) | 4 (11.4%) | 4 (11.4%) | 0 | 0 | 0 |
| 96 | More tests and documentation added | 2 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 112 | - More effort into matching constructors to arguments in newInstance(). - More effort when trying to create an Exception from a Map. | 92 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 113 | - Added ReflectionUtils method to get all constructors, reusing existing construtor cache - ClassUtilities does not attempt to instantiate abstract classes - ClassUtilities added MethodHandle (JDK 8) to the no-no list.  VarHandle is Java 9+ | 21 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 117 | - use computeIfAbsent when caching constructors | 42 | 0 (0.0%) | 5 (11.9%) | 5 (11.9%) | 0 | 0 | 0 |
| 118 | - ReflectionUtils uses computeIfAbsent() style caching for all of it's cached items, ensuring that one instance only, is ever handed out from a cache. No race conditions, less "heapy" - Moved Sealable* back to json-io | 78 | 62 (79.5%) | 37 (47.4%) | 58 (74.4%) | 0 | 0 | 0 |
| 123 | > * [ClassUtilities](userguide.md#classutilities) adds >   * `Set<Class<?>> findLowestCommonSupertypes(Class<?> a, Class<?> b)` >     * which returns the lowest common anscestor(s) of two classes, excluding `Object.class.`  This is useful for finding the common ancestor of two classes that are not related by inheritance.  Generally, executes in O(n log n) - uses sort internally.  If more than one exists, you can filter the returned Set as you please, favoring classes, interfaces, etc. >   * `Class<?> findLowestCommonSupertype(Class<?> a, Class<?> b)` >     * which is a convenience method that calls the above method and then returns the first one in the Set or null. >   * `boolean haveCommonAncestor(Class<?> a, Class<?> b)` >     * which returns true if the two classes have a common ancestor (excluding `Object.class`). >   * `Set<Class<?>> getAllSupertypes(Class<?> clazz)` >     * which returns all superclasses and interfaces of a class, including itself.  This is useful for finding all the classes and interfaces that a class implements or extends. > * Moved `Sealable*` test cases to json-io project. > * Removed remaining usages of deprecated `CompactLinkedMap.` | 49 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 134 | - All older Java date-time classes and newer temporal classes, output in "one field" when in Map form. - Many, many new tests added - Timezone handling improved - ZonedDateTime ISO_ZONE_DATE_TIME format used consistently and round trips. - GMT time supported but turns into Etc/GMT internally. | 4 | 4 (100.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 143 | TypeUtilities added | 3 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 151 | Caching added to more reflective options | 61 | 37 (60.7%) | 6 (9.8%) | 39 (63.9%) | 0 | 0 | 0 |
| 152 | Caching improved on Converter, ClassUtilities, and TypeUtilities. | 1 | 1 (100.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 153 | Minor performance tweaks | 2 | 2 (100.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 157 | Merge pull request #113 from kpartlow/kpartlow/cleanup-overrides-deprecated-java | 6 | 4 (66.7%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 158 | - Updated IOUtilities to use autoclose - Significant documentation updates on userguide | 2 | 2 (100.0%) | 2 (100.0%) | 2 (100.0%) | 0 | 0 | 13 |
| 160 | Merge pull request #114 from kpartlow/kpartlow/cleaning-up-warnings | 2 | 2 (100.0%) | 2 (100.0%) | 2 (100.0%) | 0 | 0 | 1 |
| 161 | Incorporated Ken's latest changes | 2 | 2 (100.0%) | 2 (100.0%) | 2 (100.0%) | 0 | 0 | 0 |
| 163 | - Performance improvements for object creation by improved constructor caching (cached in proper sorted order) - Last successfully used constructor is cached for performance improvement - newInstance() is now faster in my testing (almost 2x) - More parameter types supplied for argument matching when searching for constructor values to fill in. | 236 | 171 (72.5%) | 82 (34.7%) | 82 (34.7%) | 0 | 0 | 0 |
| 164 | code clean up on computeInheritanceDistance | 38 | 33 (86.8%) | 30 (78.9%) | 36 (94.7%) | 0 | 0 | 0 |
| 165 | performance improvements | 96 | 66 (68.8%) | 45 (46.9%) | 69 (71.9%) | 0 | 0 | 0 |
| 166 | performance and cleanup | 40 | 11 (27.5%) | 5 (12.5%) | 6 (15.0%) | 0 | 0 | 0 |
| 167 | performance and cleanup | 36 | 28 (77.8%) | 28 (77.8%) | 34 (94.4%) | 0 | 0 | 0 |
| 168 | performance and cleanup | 45 | 42 (93.3%) | 31 (68.9%) | 34 (75.6%) | 0 | 0 | 0 |
| 170 | performance and cleanup | 34 | 26 (76.5%) | 12 (35.3%) | 22 (64.7%) | 0 | 0 | 0 |
| 171 | performance and cleanup | 2 | 2 (100.0%) | 2 (100.0%) | 2 (100.0%) | 0 | 0 | 0 |
| 176 | - getConfig() and withConfig() support for CompactMap and CompactSet - Tests added for getConfig() withConfig() - If a non-JDK Map is uses as the "mapType" it no longer has to have both a constructor that takes an int (initialSize) and a default constructor | 69 | 6 (8.7%) | 1 (1.4%) | 1 (1.4%) | 0 | 0 | 0 |
| 177 | CompactMap and CompactSet now have getConfig() and withConfig() as another alternative for creation and getting config information - updated to 3.2.0 | 3 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 180 | - Added back CompactMap/CompactSet specific subclasses to allow usage of common specialized CompactMaps/CompactSets without having to write specialized reader/writers. - ByteBuffer and CharBuffer converstions to/from Map added. - updated to use json-io 4.51.0 for testing - DEFAULT_FIELD_FILTER in ReflectionUtils made public. | 11 | 2 (18.2%) | 2 (18.2%) | 2 (18.2%) | 0 | 0 | 0 |
| 191 | LRUCache - added getCapacity() so you can tell how it was established. ReflectionUtils - swaping out caches is now atomic and guarantees the swapped in cache is a ConcurrentMap, LRUCache, and if not, wraps it in a synchronized map. - CaseInsensitiveMap - swapping out the cache is atomic, the CaseInsenstiveString comparison is slightly faster when the inbound String is a String (not a CseInsensitiveString) - compares hashes first, and finally the code was reduced by putting common check for CaseInsensitiveString() into a separate method. | 40 | 28 (70.0%) | 23 (57.5%) | 24 (60.0%) | 0 | 0 | 0 |
| 193 | converting groovy to java | 13 | 5 (38.5%) | 0 (0.0%) | 0 (0.0%) | 11 | 0 | 0 |
| 195 | supporting all non-build() paths of CompactMap and CompactSet | 1 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 200 | Merge pull request #124 from jdereg/codex/fix-compactset-test-failures-and-improve-reflectionutils-wor | 3 | 1 (33.3%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 201 | Merge pull request #125 from jdereg/codex/fix-assertion-failures-in-testlegacyreverse | 5 | 5 (100.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 203 | Merge pull request #127 from jdereg/codex/add-binary-search-support-in-compactmap | 75 | 37 (49.3%) | 34 (45.3%) | 37 (49.3%) | 0 | 0 | 0 |
| 204 | Merge pull request #128 from jdereg/codex/fix-test-failures-after-binary-search-changes | 4 | 4 (100.0%) | 4 (100.0%) | 4 (100.0%) | 0 | 0 | 0 |
| 215 | Merge pull request #138 from jdereg/codex/update-resource-loading-and-documentation | 11 | 8 (72.7%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 216 | Merge pull request #139 from jdereg/codex/update-decomposeunorderedcollection-method | 2 | 2 (100.0%) | 2 (100.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 220 | Merge pull request #143 from jdereg/codex/change-stack-to-arraydeque-in-deepequals | 9 | 9 (100.0%) | 2 (22.2%) | 0 (0.0%) | 0 | 0 | 0 |
| 223 | Merge pull request #145 from jdereg/codex/strengthen-validation-in-validateandfinalizeoptions | 11 | 1 (9.1%) | 1 (9.1%) | 1 (9.1%) | 0 | 0 | 0 |
| 224 | Merge pull request #146 from jdereg/codex/update-putall-in-compactmap-for-efficiency | 14 | 12 (85.7%) | 0 (0.0%) | 9 (64.3%) | 0 | 0 | 0 |
| 228 | Merge pull request #150 from jdereg/yytw45-codex/ensure-source-compatibility-with-jdk-1.8 | 2 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 263 | Merge pull request #182 from jdereg/codex/review-classutilities-for-improvements | 10 | 5 (50.0%) | 2 (20.0%) | 2 (20.0%) | 0 | 0 | 0 |
| 271 | Merge pull request #189 from jdereg/codex/review-reflectionutils-for-defects-and-improvements | 15 | 12 (80.0%) | 10 (66.7%) | 10 (66.7%) | 0 | 0 | 0 |
| 280 | Merge pull request #198 from jdereg/codex/replace-system.out/system.err-with-java-logger | 2 | 1 (50.0%) | 1 (50.0%) | 1 (50.0%) | 0 | 0 | 0 |
| 283 | Merge pull request #201 from jdereg/codex/standardize-java-logging-format | 1 | 1 (100.0%) | 1 (100.0%) | 1 (100.0%) | 0 | 0 | 0 |
| Total | Common measured commits | 6565 | 3824 (58.2%) | 2503 (38.1%) | 1910 (29.1%) | 17 | 36 | 134 |

## Other files

| Index | Subject | Executable lines | Dev (scope) covered | KTester covered | Ouédraogo covered | Dev (scope) lost | KTester lost | Ouédraogo lost |
|---:|---|---:|---:|---:|---:|---:|---:|---:|
Unavailable: batch execution measures scope files only.
Production-change commits: 231. Commits with zero executable changed lines: 111. Commits without an executable-line measurement: 6.
