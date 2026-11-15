# Table 3. Changed-line coverage

Coverage uses methods alive entering each index. Changed-line coordinates are the post-image added/modified lines. Build failures and missing coverage remain unavailable.

## All files

| Index | Subject | Executable lines | Dev covered | KTester covered | Ouédraogo covered | Dev lost | KTester lost | Ouédraogo lost |
|---:|---|---:|---:|---:|---:|---:|---:|---:|
Unavailable: batch execution measures scope files only.
## Scope files

| Index | Subject | Executable lines | Dev covered | KTester covered | Ouédraogo covered | Dev lost | KTester lost | Ouédraogo lost |
|---:|---|---:|---:|---:|---:|---:|---:|---:|
| 1 | Converter - code cleanup and updated Javadoc | 40 | 32 (80.0%) | 27 (67.5%) | 32 (80.0%) | 0 | 0 | 1 |
| 2 | Converter now supports Collection, Array, and EnumSet conversions, including multiple dimension support, between collections of primitives, primitive wrappers, Date, Time, java.time.Temporal, the same set of base conversions that existed, are now supported in all possible way that I can think of. I'm sure I will hear from the Internet about other opportunities. | 95 | 34 (35.8%) | 24 (25.3%) | 30 (31.6%) | 1 | 0 | 0 |
| 4 | Concurrency related tests are now only run during deploy phase. | 24 | 11 (45.8%) | 10 (41.7%) | 9 (37.5%) | 0 | 0 | 0 |
| 5 | Improved performance of Convert by no longer requiring creating of a "pair" Map.Entry instance for lookup in the CONVERSION_DB and USER_DB. | 738 | 734 (99.5%) | 726 (98.4%) | 734 (99.5%) | 0 | 0 | 50 |
| 6 | Opened more tests to EnabledIf instead of Disabled. These can be run with -DperformRelease=true (whether you are doing an install or a deploy) | 718 | 716 (99.7%) | 712 (99.2%) | 716 (99.7%) | 1 | 0 | 0 |
| 8 | - Converter: performance improvements (no object creation when pairing keys) and improved scoring algorithm to find conversion pair that has the least distance in total for the source/target items. - UniqueIdGenerator - better approach to guarantee monotonically increase numbers and also supports a clock that may be been adjusted backward.  More supported added for cluster environments to identify unique node/pod (last component of it). Also fixed rate but possible non-monotonically increasing ID in rare race-condition with LinkedHashMap eviction. No longer maintains last 1000 or last 10,000 ids. | 129 | 110 (85.3%) | 77 (59.7%) | 108 (83.7%) | 0 | 9 | 25 |
| 9 | Opened more tests to EnabledIf instead of Disabled. These can be run with -DperformRelease=true (whether you are doing an install or a deploy) | 18 | 6 (33.3%) | 5 (27.8%) | 6 (33.3%) | 0 | 0 | 3 |
| 11 | - Adding more Javadoc - CaseInsensitiveMap, additional capabilities being added for the keySet() and entrySet() - toArray. Also may add the computeIfPresnet, etc. APIs to the CaseInsenstiveMap as well. | 140 | 59 (42.1%) | 62 (44.3%) | 44 (31.4%) | 76 | 0 | 41 |
| 12 | CaseInsensitiveMap - when constructing from other Maps, the determineBaseMap capability supports many more maps, still defaults to LinkedHashMap, and allows the entire registry of Map types to be loaded (as a public API).  In addition validation checks are performed to prevent IdentityHashMap as a source, as well as to ensure the orderings of the defined entries are from specific to more general. - Lots of new tests added, 100% class, method, line, and branch coverage now. | 118 | 53 (44.9%) | 53 (44.9%) | 52 (44.1%) | 0 | 0 | 0 |
| 13 | - 100% code coverage on CaseInsensitiveMap and Javadoc at 100% - Small cache used for creating/reusing CaseInsenstiveStrings. | 52 | 31 (59.6%) | 24 (46.2%) | 22 (42.3%) | 0 | 0 | 0 |
| 14 | - Renamed tests to follow modern naming convention. - Added tests that directly test MapConversion | 15 | 7 (46.7%) | 7 (46.7%) | 7 (46.7%) | 0 | 0 | 0 |
| 15 | Significant update to allow wrapped collection types to be supported.  Beginning removal of Sealed* as that will no longer be needed now that we support Unmodifiable* wrappers. | 69 | 20 (29.0%) | 4 (5.8%) | 5 (7.2%) | 0 | 0 | 0 |
| 16 | isConversionSupportedFor() and isDirectionConversionSupportedFor() updated to be more accurate, looking into array component types | 28 | 17 (60.7%) | 7 (25.0%) | 9 (32.1%) | 0 | 0 | 0 |
| 17 | - Deprecated Sealable* - Converter now accurately says whether a Collection, Array, or EnumSet conversion is supported. | 18 | 6 (33.3%) | 4 (22.2%) | 5 (27.8%) | 0 | 1 | 0 |
| 18 | - recursive unmodifiability and synchronize on Collections when multidimensional copied - CollectionUtilities beefed up to handle all the wrapper types (wraps with most correct wrapper) - Massive Javadoc update | 133 | 66 (49.6%) | 50 (37.6%) | 57 (42.9%) | 265 | 1 | 19 |
| 19 | - CompactMap has new factory constructor methods so that no subclassing of CompactMap is necessary to support alternative behaviors (compactSize, cpaacity, case-sensitivity, Map type to use, source Map, usage of copying iterators, single-key setting) - Starting to break-out put/get so that it will honor ordering-based Maps. | 316 | 209 (66.1%) | 183 (57.9%) | 234 (74.1%) | 0 | 0 | 0 |
| 20 | - CompactMap has new factory constructor methods so that no subclassing of CompactMap is necessary to support alternative behaviors (compactSize, cpaacity, case-sensitivity, Map type to use, source Map, usage of copying iterators, single-key setting) - Starting to break-out put/get so that it will honor ordering-based Maps. | 51 | 22 (43.1%) | 24 (47.1%) | 25 (49.0%) | 0 | 0 | 0 |
| 21 | CompactMap now handling ordering - natural order, sorted, reverse sorted, and unordered | 46 | 6 (13.0%) | 6 (13.0%) | 6 (13.0%) | 0 | 0 | 0 |
| 22 | CompactMap - case-sensitive flag fully honored when no comparator set.  If comparator set, then it is ignored. CompactMap - documentation for class and newMap fully blown out. | 21 | 2 (9.5%) | 2 (9.5%) | 2 (9.5%) | 0 | 0 | 0 |
| 24 | almost there - struggling with sorting... | 89 | 38 (42.7%) | 38 (42.7%) | 40 (44.9%) | 0 | 0 | 0 |
| 25 | - I have a working version of CompactMap with all tests passing (all java-util tests) | 34 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 26 | saving off correct copy before final optimizations | 30 | 2 (6.7%) | 3 (10.0%) | 3 (10.0%) | 0 | 0 | 0 |
| 27 | - updated markdown | 49 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 28 | Fixed some edge cases with respect to case insensitivity | 85 | 8 (9.4%) | 8 (9.4%) | 8 (9.4%) | 0 | 0 | 0 |
| 30 | Fighting backwards compatibility... | 85 | 4 (4.7%) | 2 (2.4%) | 4 (4.7%) | 0 | 18 | 26 |
| 31 | - ReflectionUtils massively updated (improved, complete adjustible LRU caching) and new API to easily get methods (deep or shallow) - CompactMap now supports old construction patterns while offering new improved factory methods. - CompactMap factory method approach will be replaced with Builder pattern before this is released. | 328 | 246 (75.0%) | 177 (54.0%) | 114 (34.8%) | 1 | 4 | 0 |
| 32 | Added more tests to ensure old-style CompactMap construction works in all cases.  Fixed a couple of bugs it uncovered. | 56 | 49 (87.5%) | 23 (41.1%) | 19 (33.9%) | 0 | 0 | 0 |
| 33 | minor code fixes | 3 | 3 (100.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 34 | warning fixes | 5 | 4 (80.0%) | 5 (100.0%) | 5 (100.0%) | 0 | 0 | 0 |
| 35 | CompactMap now keeps items in insertion order, and only sorts the keys when changing to an iterator view, greatly improving performance. | 27 | 26 (96.3%) | 14 (51.9%) | 15 (55.6%) | 0 | 0 | 0 |
| 36 | - remove() sped up dramatically - no sorting during remove. | 31 | 30 (96.8%) | 0 (0.0%) | 6 (19.4%) | 0 | 0 | 0 |
| 37 | - removed getNewMap(int size) as it was not always overwritten by users, leaving inconsistent behavior if they overwrote getNewMap() to return TreeMap(case insensitive, reverse order), but did not overwrite getNewMap(int size) to be the same way.  This improves correctness and avoids the customer running into hard-to-solve bugs. | 3 | 3 (100.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 3 | 0 |
| 38 | minor code changes for consistency | 27 | 20 (74.1%) | 6 (22.2%) | 8 (29.6%) | 0 | 0 | 0 |
| 39 | prep for conversion to builder pattern | 4 | 2 (50.0%) | 2 (50.0%) | 1 (25.0%) | 0 | 0 | 0 |
| 40 | CompactMap builder pattern added. More tests added. Adding more tests until code coverage is complete. | 27 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 41 | - migrating toward a solution that creates classes with static final constants so that users do not have to create subclasses. | 20 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 42 | dynamically generating subclass for CompactMap based on options set. | 159 | 7 (4.4%) | 4 (2.5%) | 4 (2.5%) | 0 | 0 | 0 |
| 43 | CompactMap automatic subclass generation for constants now complete.  CompactMap has one and only one member variable, 'val.'  And the class name will always be CompactMap$<options> | 40 | 3 (7.5%) | 6 (15.0%) | 6 (15.0%) | 0 | 4 | 1 |
| 44 | code cleanup | 2 | 2 (100.0%) | 2 (100.0%) | 2 (100.0%) | 0 | 0 | 0 |
| 45 | ThreadLocal removed. Temporary state.  New to remove getNewMap() calls making determinations about maptype. | 28 | 19 (67.9%) | 12 (42.9%) | 12 (42.9%) | 0 | 0 | 0 |
| 46 | - all tests passing - narrowing the gap between the legacy creation and new builder creation | 56 | 5 (8.9%) | 3 (5.4%) | 3 (5.4%) | 0 | 0 | 0 |
| 47 | Finally taking shape.  More documentation coming, should be near final version. | 88 | 54 (61.4%) | 12 (13.6%) | 12 (13.6%) | 0 | 0 | 0 |
| 48 | minor code cleanup | 2 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 49 | adding more generality to differing Map types to be supported. | 101 | 46 (45.5%) | 45 (44.6%) | 47 (46.5%) | 0 | 0 | 0 |
| 50 | - Legacy mode and new "template" mode are completely integrated. | 9 | 8 (88.9%) | 4 (44.4%) | 4 (44.4%) | 0 | 0 | 0 |
| 51 | added better error handling when user specifies incorrect class instead of Map.  Also, can handle Map that does not take capacity. | 62 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 52 | More readable template | 41 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 53 | - removed capacity. Should never have been added.  what was i thinking? | 35 | 1 (2.9%) | 2 (5.7%) | 1 (2.9%) | 0 | 0 | 0 |
| 54 | Beefing up Javadoc | 1 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 55 | - Further blow-out of Javadoc - CsaeInsensitiveMap's wrapped Map maintained. | 19 | 4 (21.1%) | 4 (21.1%) | 5 (26.3%) | 0 | 0 | 0 |
| 59 | - finished javadocs | 6 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 60 | - reduce API surface area - Use better approach to get classLoader | 1 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 61 | DeepEquals breadcrumb trails | 482 | 402 (83.4%) | 110 (22.8%) | 326 (67.6%) | 0 | 0 | 4 |
| 62 | deep equals breadcrumb gen in progress... | 236 | 206 (87.3%) | 23 (9.7%) | 199 (84.3%) | 0 | 0 | 0 |
| 63 | - bread crumb output further refined. - Improved stack handling in deepEquals() | 93 | 68 (73.1%) | 18 (19.4%) | 20 (21.5%) | 0 | 0 | 92 |
| 64 | formatting deepEquals differences getting much better... | 279 | 187 (67.0%) | 5 (1.8%) | 34 (12.2%) | 0 | 0 | 0 |
| 65 | - working out formatting delta issues... | 230 | 160 (69.6%) | 0 (0.0%) | 28 (12.2%) | 0 | 0 | 0 |
| 66 | - Adding the concept of a container that contains an ItemToCompare | 6 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 67 | improving DeepEquals breadcrumb output. | 283 | 243 (85.9%) | 28 (9.9%) | 68 (24.0%) | 0 | 0 | 0 |
| 68 | - Formatting for deepEquals "diff" nearing completion.  Worrying about "fonts" and "colors" now (j/k) - ReflectionUtils is finished | 380 | 294 (77.4%) | 98 (25.8%) | 46 (12.1%) | 4 | 0 | 0 |
| 69 | -perfecting deepEquals() format.  Just a few small items left. | 13 | 11 (84.6%) | 0 (0.0%) | 2 (15.4%) | 0 | 0 | 0 |
| 70 | - formatting for deepEquals, further improved. | 3 | 3 (100.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 71 | - improved comparison between simple types - use compareTo() == 0 if they are Comparable, before using .equals() | 5 | 5 (100.0%) | 3 (60.0%) | 3 (60.0%) | 0 | 0 | 0 |
| 72 | - formatting improvements. | 34 | 32 (94.1%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 73 | - great headway made on deepEquals() difference output | 262 | 175 (66.8%) | 5 (1.9%) | 43 (16.4%) | 0 | 0 | 0 |
| 74 | - formatting just about complete for deepEquals output differences. | 49 | 37 (75.5%) | 0 (0.0%) | 7 (14.3%) | 0 | 0 | 0 |
| 75 | - formatting nearly complete | 81 | 42 (51.9%) | 0 (0.0%) | 4 (4.9%) | 0 | 0 | 0 |
| 76 | - perfecting deepEquals diff output | 21 | 20 (95.2%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 77 | nearing "art" form... on diff formatting. | 19 | 17 (89.5%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 78 | - perfecting a little further | 1 | 1 (100.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 79 | - I think we have it. "diff" is now formatted consistently | 66 | 59 (89.4%) | 5 (7.6%) | 10 (15.2%) | 0 | 0 | 0 |
| 80 | - minor tweak to deepEquals output | 5 | 4 (80.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 81 | - almost final final | 138 | 116 (84.1%) | 30 (21.7%) | 68 (49.3%) | 0 | 0 | 0 |
| 82 | - working through support of key of map being map itself - no stackoverflow | 74 | 49 (66.2%) | 1 (1.4%) | 15 (20.3%) | 0 | 0 | 0 |
| 83 | - working, but with stackoverflow on Maps with Map instance as key | 23 | 12 (52.2%) | 1 (4.3%) | 5 (21.7%) | 0 | 0 | 0 |
| 84 | - solved stackoverflow issue | 33 | 17 (51.5%) | 4 (12.1%) | 5 (15.2%) | 0 | 0 | 0 |
| 85 | - almost there, why .value? | 117 | 10 (8.5%) | 0 (0.0%) | 3 (2.6%) | 0 | 0 | 0 |
| 86 | - difference type description now precise | 47 | 32 (68.1%) | 1 (2.1%) | 11 (23.4%) | 0 | 0 | 0 |
| 87 | RC1 | 39 | 39 (100.0%) | 16 (41.0%) | 16 (41.0%) | 0 | 0 | 0 |
| 88 | DeepEquals "diff" breadcrumb output complete. | 37 | 26 (70.3%) | 2 (5.4%) | 10 (27.0%) | 0 | 0 | 0 |
| 89 | Deprecated CompactSet derivative classes. Use the builder pattern instead. | 99 | 59 (59.6%) | 44 (44.4%) | 66 (66.7%) | 2 | 22 | 31 |
| 90 | Nearing Release. minor clean ups, Javadoc | 84 | 69 (82.1%) | 6 (7.1%) | 61 (72.6%) | 0 | 0 | 49 |
| 91 | "newInstance" brought over to ClassUtilities from json-io. | 224 | 43 (19.2%) | 40 (17.9%) | 40 (17.9%) | 0 | 0 | 0 |
| 92 | - ReflectionUtils - increased flexibility in filtering fields | 50 | 35 (70.0%) | 2 (4.0%) | 23 (46.0%) | 0 | 0 | 0 |
| 94 | - CompactMap now properly caches the classes it generates. It also locks at a per classname level, increasing parallel execution. - More Javadoc updates. | 35 | 29 (82.9%) | 29 (82.9%) | 29 (82.9%) | 0 | 0 | 0 |
| 96 | More tests and documentation added | 3 | 1 (33.3%) | 1 (33.3%) | 1 (33.3%) | 0 | 0 | 0 |
| 101 | markdown updates | 2 | 2 (100.0%) | 2 (100.0%) | 2 (100.0%) | 1 | 6 | 10 |
| 102 | - markdown updates - Better caching in Converter` | 8 | 8 (100.0%) | 8 (100.0%) | 8 (100.0%) | 0 | 0 | 0 |
| 103 | More Javadoc and userguide.md updates | 30 | 22 (73.3%) | 12 (40.0%) | 1 (3.3%) | 0 | 0 | 0 |
| 104 | userguide.md and Javadoc updates | 53 | 27 (50.9%) | 10 (18.9%) | 17 (32.1%) | 0 | 0 | 0 |
| 105 | Updated Javadoc and userguide.md | 10 | 3 (30.0%) | 3 (30.0%) | 5 (50.0%) | 0 | 0 | 0 |
| 107 | Useful SystemUtilities added. Javadoc and userguide.md added | 82 | 5 (6.1%) | 5 (6.1%) | 6 (7.3%) | 0 | 0 | 1 |
| 109 | Traverse now "offers" all graph node fields to the visitor | 49 | 43 (87.8%) | 0 (0.0%) | 43 (87.8%) | 0 | 0 | 0 |
| 111 | too aggressive on gzip guaging | 1 | 1 (100.0%) | 1 (100.0%) | 1 (100.0%) | 0 | 0 | 0 |
| 112 | - More effort into matching constructors to arguments in newInstance(). - More effort when trying to create an Exception from a Map. | 134 | 0 (0.0%) | 2 (1.5%) | 0 (0.0%) | 0 | 0 | 0 |
| 113 | - Added ReflectionUtils method to get all constructors, reusing existing construtor cache - ClassUtilities does not attempt to instantiate abstract classes - ClassUtilities added MethodHandle (JDK 8) to the no-no list.  VarHandle is Java 9+ | 21 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 117 | - use computeIfAbsent when caching constructors | 42 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 118 | - ReflectionUtils uses computeIfAbsent() style caching for all of it's cached items, ensuring that one instance only, is ever handed out from a cache. No race conditions, less "heapy" - Moved Sealable* back to json-io | 78 | 62 (79.5%) | 3 (3.8%) | 27 (34.6%) | 0 | 0 | 0 |
| 122 | Removed references to CompactLinkedMapp | 25 | 0 (0.0%) | 6 (24.0%) | 14 (56.0%) | 0 | 4 | 19 |
| 123 | > * [ClassUtilities](userguide.md#classutilities) adds >   * `Set<Class<?>> findLowestCommonSupertypes(Class<?> a, Class<?> b)` >     * which returns the lowest common anscestor(s) of two classes, excluding `Object.class.`  This is useful for finding the common ancestor of two classes that are not related by inheritance.  Generally, executes in O(n log n) - uses sort internally.  If more than one exists, you can filter the returned Set as you please, favoring classes, interfaces, etc. >   * `Class<?> findLowestCommonSupertype(Class<?> a, Class<?> b)` >     * which is a convenience method that calls the above method and then returns the first one in the Set or null. >   * `boolean haveCommonAncestor(Class<?> a, Class<?> b)` >     * which returns true if the two classes have a common ancestor (excluding `Object.class`). >   * `Set<Class<?>> getAllSupertypes(Class<?> clazz)` >     * which returns all superclasses and interfaces of a class, including itself.  This is useful for finding all the classes and interfaces that a class implements or extends. > * Moved `Sealable*` test cases to json-io project. > * Removed remaining usages of deprecated `CompactLinkedMap.` | 55 | 2 (3.6%) | 1 (1.8%) | 1 (1.8%) | 78 | 536 | 398 |
| 124 | - Improved Timestamp conversions - Added test that verifies all conversions work, not only directly, but via json-io (test not yet enabled, but works locally) | 52 | 7 (13.5%) | 10 (19.2%) | 7 (13.5%) | 0 | 0 | 0 |
| 126 | Timestamp outputs reliably in fully qualified ISO 8601 format (in UTC - "Z"), or it can read from epochMillis and optional nanos if passed that way to Converter. | 28 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 1 | 0 | 0 |
| 127 | Date and sql Date conversions are no longer ambiguous on timezone.  The reader side (Map to Date, Map to sql date) always ensure timezone is included. If it is not specified (older JSON format) then it will use the system's timezone, which is what it did in the past. | 43 | 0 (0.0%) | 9 (20.9%) | 0 (0.0%) | 0 | 2 | 0 |
| 129 | Date and sql Date conversions are no longer ambiguous on timezone.  The reader side (Map to Date, Map to sql date) always ensure timezone is included. If it is not specified (older JSON format) then it will use the system's timezone, which is what it did in the past. | 8 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 130 | Cleaning up Calendar conversions and strengthening DateUtilities.parseDate | 22 | 8 (36.4%) | 6 (27.3%) | 4 (18.2%) | 0 | 18 | 0 |
| 131 | Dates, java.util.Dates, and Timestamps now written out in String form by default. | 111 | 26 (23.4%) | 26 (23.4%) | 17 (15.3%) | 1 | 1 | 3 |
| 132 | LocalDateTime, LocalDate, LocalTime, ZonedDateTime, OffsetDateTime all now use single String value for representation. | 110 | 1 (0.9%) | 17 (15.5%) | 1 (0.9%) | 0 | 19 | 0 |
| 133 | - Added support for more number values to OffsetTime - Ensured no tests use Timestamp before year 0001 as it is not supported by java.sql.Timestamp - Ensured no tests use Calendar before year 0002, as it is not supported by Java Calendar. | 179 | 20 (11.2%) | 44 (24.6%) | 20 (11.2%) | 0 | 11 | 14 |
| 134 | - All older Java date-time classes and newer temporal classes, output in "one field" when in Map form. - Many, many new tests added - Timezone handling improved - ZonedDateTime ISO_ZONE_DATE_TIME format used consistently and round trips. - GMT time supported but turns into Etc/GMT internally. | 80 | 8 (10.0%) | 19 (23.8%) | 8 (10.0%) | 0 | 12 | 45 |
| 135 | - Added Currency and Pattern to Converter. - All Date-time types can be converted to Year, YearMonth, and MonthDay - java.sql.Date is treated as a true "date" with no surprises, like a birth date. | 326 | 58 (17.8%) | 106 (32.5%) | 63 (19.3%) | 0 | 2 | 21 |
| 143 | TypeUtilities added | 3 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 4 | 16 |
| 151 | Caching added to more reflective options | 61 | 37 (60.7%) | 3 (4.9%) | 37 (60.7%) | 0 | 0 | 0 |
| 152 | Caching improved on Converter, ClassUtilities, and TypeUtilities. | 69 | 62 (89.9%) | 64 (92.8%) | 7 (10.1%) | 0 | 0 | 0 |
| 153 | Minor performance tweaks | 32 | 29 (90.6%) | 28 (87.5%) | 27 (84.4%) | 0 | 2 | 6 |
| 155 | Merge pull request #112 from kpartlow/kpartlow/fix-threading-issue | 4 | 4 (100.0%) | 4 (100.0%) | 4 (100.0%) | 0 | 0 | 0 |
| 156 | Fixed threading issue (thread not being marked daemon.) | 4 | 4 (100.0%) | 4 (100.0%) | 4 (100.0%) | 0 | 0 | 0 |
| 157 | Merge pull request #113 from kpartlow/kpartlow/cleanup-overrides-deprecated-java | 6 | 4 (66.7%) | 4 (66.7%) | 4 (66.7%) | 0 | 0 | 0 |
| 158 | - Updated IOUtilities to use autoclose - Significant documentation updates on userguide | 60 | 47 (78.3%) | 20 (33.3%) | 5 (8.3%) | 1 | 1 | 0 |
| 160 | Merge pull request #114 from kpartlow/kpartlow/cleaning-up-warnings | 10 | 10 (100.0%) | 7 (70.0%) | 10 (100.0%) | 0 | 1 | 0 |
| 161 | Incorporated Ken's latest changes | 2 | 2 (100.0%) | 0 (0.0%) | 2 (100.0%) | 0 | 0 | 0 |
| 162 | Consolidated hex arrays | 3 | 3 (100.0%) | 2 (66.7%) | 3 (100.0%) | 0 | 0 | 0 |
| 163 | - Performance improvements for object creation by improved constructor caching (cached in proper sorted order) - Last successfully used constructor is cached for performance improvement - newInstance() is now faster in my testing (almost 2x) - More parameter types supplied for argument matching when searching for constructor values to fill in. | 236 | 171 (72.5%) | 82 (34.7%) | 82 (34.7%) | 0 | 0 | 0 |
| 164 | code clean up on computeInheritanceDistance | 38 | 33 (86.8%) | 30 (78.9%) | 36 (94.7%) | 0 | 0 | 0 |
| 165 | performance improvements | 103 | 73 (70.9%) | 63 (61.2%) | 69 (67.0%) | 0 | 0 | 0 |
| 166 | performance and cleanup | 41 | 12 (29.3%) | 6 (14.6%) | 6 (14.6%) | 0 | 0 | 0 |
| 167 | performance and cleanup | 36 | 28 (77.8%) | 28 (77.8%) | 33 (91.7%) | 0 | 0 | 0 |
| 168 | performance and cleanup | 47 | 44 (93.6%) | 33 (70.2%) | 36 (76.6%) | 0 | 0 | 2 |
| 170 | performance and cleanup | 157 | 113 (72.0%) | 95 (60.5%) | 65 (41.4%) | 0 | 0 | 4 |
| 171 | performance and cleanup | 2 | 2 (100.0%) | 2 (100.0%) | 2 (100.0%) | 0 | 0 | 0 |
| 176 | - getConfig() and withConfig() support for CompactMap and CompactSet - Tests added for getConfig() withConfig() - If a non-JDK Map is uses as the "mapType" it no longer has to have both a constructor that takes an int (initialSize) and a default constructor | 105 | 6 (5.7%) | 7 (6.7%) | 7 (6.7%) | 0 | 0 | 0 |
| 177 | CompactMap and CompactSet now have getConfig() and withConfig() as another alternative for creation and getting config information - updated to 3.2.0 | 3 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 180 | - Added back CompactMap/CompactSet specific subclasses to allow usage of common specialized CompactMaps/CompactSets without having to write specialized reader/writers. - ByteBuffer and CharBuffer converstions to/from Map added. - updated to use json-io 4.51.0 for testing - DEFAULT_FIELD_FILTER in ReflectionUtils made public. | 63 | 11 (17.5%) | 7 (11.1%) | 11 (17.5%) | 0 | 4 | 0 |
| 181 | Merge remote-tracking branch 'origin/master' | 3 | 1 (33.3%) | 0 (0.0%) | 2 (66.7%) | 0 | 0 | 0 |
| 184 | Merge pull request #116 from ozhelezniak-talend/ozhelezniak/fastwriter_int_write_fix | 3 | 2 (66.7%) | 0 (0.0%) | 3 (100.0%) | 0 | 0 | 1 |
| 185 | Sped up StringUtilities.hashCodeIgnoreCase by using ASCII trick when the current Locale represents a county that uses latin letters. | 128 | 59 (46.1%) | 54 (42.2%) | 45 (35.2%) | 0 | 0 | 0 |
| 188 | JDK 24 compatibility added | 4 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 189 | updated name of system API, added it to userguide.md | 1 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 191 | LRUCache - added getCapacity() so you can tell how it was established. ReflectionUtils - swaping out caches is now atomic and guarantees the swapped in cache is a ConcurrentMap, LRUCache, and if not, wraps it in a synchronized map. - CaseInsensitiveMap - swapping out the cache is atomic, the CaseInsenstiveString comparison is slightly faster when the inbound String is a String (not a CseInsensitiveString) - compares hashes first, and finally the code was reduced by putting common check for CaseInsensitiveString() into a separate method. | 72 | 44 (61.1%) | 40 (55.6%) | 35 (48.6%) | 0 | 0 | 0 |
| 192 | Merge pull request #118 from jdereg/codex/find-and-fix-a-bug | 7 | 6 (85.7%) | 0 (0.0%) | 6 (85.7%) | 0 | 0 | 0 |
| 193 | converting groovy to java | 13 | 5 (38.5%) | 5 (38.5%) | 5 (38.5%) | 14 | 17 | 118 |
| 194 | Merge pull request #119 from jdereg/codex/fix-compactset-to-support-subclass-overriding-in-jre | 41 | 0 (0.0%) | 0 (0.0%) | 9 (22.0%) | 0 | 0 | 0 |
| 195 | supporting all non-build() paths of CompactMap and CompactSet | 4 | 0 (0.0%) | 0 (0.0%) | 1 (25.0%) | 0 | 0 | 0 |
| 199 | Merge pull request #123 from jdereg/codex/update-fallback-stream-hashing-methods | 2 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 200 | Merge pull request #124 from jdereg/codex/fix-compactset-test-failures-and-improve-reflectionutils-wor | 3 | 1 (33.3%) | 0 (0.0%) | 1 (33.3%) | 0 | 0 | 0 |
| 201 | Merge pull request #125 from jdereg/codex/fix-assertion-failures-in-testlegacyreverse | 5 | 5 (100.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 203 | Merge pull request #127 from jdereg/codex/add-binary-search-support-in-compactmap | 75 | 37 (49.3%) | 20 (26.7%) | 37 (49.3%) | 0 | 0 | 0 |
| 204 | Merge pull request #128 from jdereg/codex/fix-test-failures-after-binary-search-changes | 4 | 4 (100.0%) | 4 (100.0%) | 4 (100.0%) | 0 | 0 | 0 |
| 215 | Merge pull request #138 from jdereg/codex/update-resource-loading-and-documentation | 11 | 8 (72.7%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 216 | Merge pull request #139 from jdereg/codex/update-decomposeunorderedcollection-method | 2 | 2 (100.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 217 | Merge pull request #140 from jdereg/codex/modify-replacecache-to-check-for-null-cache | 1 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 218 | Merge pull request #141 from jdereg/codex/update-caseinsensitivemap-constructor | 4 | 3 (75.0%) | 3 (75.0%) | 3 (75.0%) | 0 | 0 | 0 |
| 220 | Merge pull request #143 from jdereg/codex/change-stack-to-arraydeque-in-deepequals | 9 | 9 (100.0%) | 1 (11.1%) | 1 (11.1%) | 0 | 0 | 0 |
| 223 | Merge pull request #145 from jdereg/codex/strengthen-validation-in-validateandfinalizeoptions | 11 | 1 (9.1%) | 1 (9.1%) | 6 (54.5%) | 0 | 0 | 0 |
| 224 | Merge pull request #146 from jdereg/codex/update-putall-in-compactmap-for-efficiency | 14 | 12 (85.7%) | 2 (14.3%) | 2 (14.3%) | 0 | 0 | 0 |
| 228 | Merge pull request #150 from jdereg/yytw45-codex/ensure-source-compatibility-with-jdk-1.8 | 2 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 231 | Merge pull request #153 from jdereg/codex/update-wrap-method-signature-and-invocations | 3 | 3 (100.0%) | 3 (100.0%) | 3 (100.0%) | 0 | 0 | 0 |
| 233 | Merge pull request #155 from jdereg/codex/refactor-caseinsensitiveset-methods | 2 | 2 (100.0%) | 2 (100.0%) | 2 (100.0%) | 0 | 0 | 0 |
| 234 | The explicitly coded retainAll() and removeAll() are exactly, properly implemented routines that properly manage the Set change state.  The CompileClassResourceTest needed a missing import. | 21 | 21 (100.0%) | 18 (85.7%) | 21 (100.0%) | 0 | 0 | 0 |
| 236 | Merge pull request #158 from jdereg/codex/review-compactset-implementation | 1 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 237 | Merge pull request #159 from jdereg/codex/refactor-headset-and-tailset-methods | 2 | 2 (100.0%) | 0 (0.0%) | 2 (100.0%) | 0 | 0 | 0 |
| 240 | Merge pull request #162 from jdereg/codex/validate-capacity-in-lrucache-constructors | 8 | 3 (37.5%) | 3 (37.5%) | 4 (50.0%) | 0 | 0 | 1 |
| 241 | Merge pull request #163 from jdereg/codex/modify-lockinglrucachestrategy.put-behavior | 2 | 0 (0.0%) | 2 (100.0%) | 2 (100.0%) | 0 | 0 | 1 |
| 244 | Merge pull request #166 from jdereg/codex/update-ttlcache.put-and-add-tests | 8 | 8 (100.0%) | 2 (25.0%) | 8 (100.0%) | 0 | 1 | 3 |
| 245 | Merge pull request #167 from jdereg/codex/store-and-cancel-scheduledfuture-for-cache-task | 14 | 8 (57.1%) | 8 (57.1%) | 8 (57.1%) | 0 | 0 | 0 |
| 247 | Merge pull request #169 from jdereg/codex/update-hashcode-implementation-with-cachemap-entries | 7 | 7 (100.0%) | 0 (0.0%) | 7 (100.0%) | 2 | 0 | 0 |
| 249 | Merge pull request #171 from jdereg/codex/review-trackingmap-for-defects-and-improvements | 7 | 3 (42.9%) | 4 (57.1%) | 0 (0.0%) | 0 | 12 | 99 |
| 250 | Merge pull request #172 from jdereg/codex/review-concurrenthashmapnullsafe-for-improvements | 7 | 7 (100.0%) | 0 (0.0%) | 7 (100.0%) | 0 | 0 | 0 |
| 253 | Merge pull request #173 from jdereg/codex/review-concurrentnavigablemapnullsafe-for-issues | 38 | 14 (36.8%) | 4 (10.5%) | 28 (73.7%) | 0 | 0 | 0 |
| 254 | Merge pull request #174 from jdereg/codex/conduct-code-review-for-arrayutilities | 26 | 8 (30.8%) | 2 (7.7%) | 11 (42.3%) | 0 | 0 | 0 |
| 255 | Merge pull request #175 from jdereg/codex/review-byteutilities-for-improvements | 8 | 7 (87.5%) | 7 (87.5%) | 8 (100.0%) | 0 | 1 | 2 |
| 258 | Merge pull request #177 from jdereg/tzkbmk-codex/review-and-improve-concurrentlist-code | 31 | 21 (67.7%) | 19 (61.3%) | 24 (77.4%) | 0 | 4 | 11 |
| 260 | Merge pull request #179 from jdereg/codex/review-and-optimize-dateutilities | 8 | 6 (75.0%) | 6 (75.0%) | 6 (75.0%) | 0 | 0 | 0 |
| 261 | Merge pull request #180 from jdereg/4l9ef3-codex/review-and-improve-concurrentlist-code | 3 | 1 (33.3%) | 2 (66.7%) | 3 (100.0%) | 0 | 0 | 5 |
| 262 | Merge pull request #181 from jdereg/codex/review-converter-for-defects-and-improvements | 2 | 2 (100.0%) | 2 (100.0%) | 2 (100.0%) | 0 | 0 | 0 |
| 263 | Merge pull request #182 from jdereg/codex/review-classutilities-for-improvements | 10 | 5 (50.0%) | 2 (20.0%) | 2 (20.0%) | 0 | 0 | 0 |
| 264 | Merge pull request #183 from jdereg/codex/review-encryptionutilities-for-issues-and-improvements | 80 | 39 (48.8%) | 59 (73.8%) | 57 (71.2%) | 4 | 4 | 0 |
| 265 | Merge pull request #184 from jdereg/codex/review-and-optimize-executor-class | 54 | 24 (44.4%) | 30 (55.6%) | 53 (98.1%) | 0 | 2 | 13 |
| 266 | Merge pull request #185 from jdereg/codex/review-ioutilities-for-defects-and-improvements | 40 | 1 (2.5%) | 17 (42.5%) | 1 (2.5%) | 48 | 0 | 0 |
| 267 | Merge pull request #186 from jdereg/codex/review-mathutilities-for-code-quality-improvements | 45 | 30 (66.7%) | 9 (20.0%) | 38 (84.4%) | 0 | 5 | 4 |
| 269 | Merge pull request #187 from jdereg/vseo74-codex/review-encryptionutilities-for-issues-and-improvements | 20 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 271 | Merge pull request #189 from jdereg/codex/review-reflectionutils-for-defects-and-improvements | 15 | 12 (80.0%) | 10 (66.7%) | 12 (80.0%) | 0 | 0 | 0 |
| 273 | Merge pull request #191 from jdereg/codex/review-stringutilities-for-code-quality-improvements | 21 | 7 (33.3%) | 10 (47.6%) | 5 (23.8%) | 61 | 2 | 133 |
| 274 | Merge pull request #192 from jdereg/codex/review-systemutilities-for-issues-and-improvements | 40 | 1 (2.5%) | 1 (2.5%) | 1 (2.5%) | 0 | 0 | 0 |
| 275 | Merge pull request #193 from jdereg/codex/review-and-improve-traverser-class-code | 31 | 22 (71.0%) | 0 (0.0%) | 22 (71.0%) | 0 | 0 | 0 |
| 276 | Merge pull request #194 from jdereg/codex/review-uniqueidgenerator-code-for-issues-and-improvements | 6 | 3 (50.0%) | 2 (33.3%) | 3 (50.0%) | 0 | 0 | 0 |
| 277 | Merge pull request #195 from jdereg/codex/fix-failure-in-traversertest.testlazyfieldcollection | 3 | 2 (66.7%) | 0 (0.0%) | 2 (66.7%) | 0 | 0 | 0 |
| 279 | Merge pull request #197 from jdereg/codex/fix-failing-lazy-field-collection-test | 6 | 6 (100.0%) | 0 (0.0%) | 6 (100.0%) | 0 | 0 | 0 |
| 280 | Merge pull request #198 from jdereg/codex/replace-system.out/system.err-with-java-logger | 14 | 6 (42.9%) | 4 (28.6%) | 8 (57.1%) | 0 | 1 | 0 |
| 281 | Merge pull request #199 from jdereg/dg8z2w-codex/fix-failing-lazy-field-collection-test | 2 | 2 (100.0%) | 0 (0.0%) | 2 (100.0%) | 0 | 0 | 0 |
| 283 | Merge pull request #201 from jdereg/codex/standardize-java-logging-format | 7 | 7 (100.0%) | 5 (71.4%) | 7 (100.0%) | 0 | 1 | 0 |
| 293 | Merge pull request #207 from jdereg/codex/add-high-value-stringutilities-apis-with-tests | 54 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| Total | Common measured commits | 11572 | 6715 (58.0%) | 3993 (34.5%) | 4785 (41.3%) | 561 | 735 | 1272 |

## Other files

| Index | Subject | Executable lines | Dev covered | KTester covered | Ouédraogo covered | Dev lost | KTester lost | Ouédraogo lost |
|---:|---|---:|---:|---:|---:|---:|---:|---:|
Unavailable: batch execution measures scope files only.
Production-change commits: 231. Commits with zero executable changed lines: 34. Commits without an executable-line measurement: 6.
