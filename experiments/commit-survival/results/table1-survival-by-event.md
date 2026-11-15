# Table 1. Survival by event

| Index | Date | Subject | Dev | KTester | Ouédraogo |
|---:|---|---|---:|---:|---:|
| 0 | 2024-10-17 | t = 2.18.0 | 1185 (100.0%) | 2688 (100.0%) | 3403 (100.0%) |
| 1 | 2024-11-29 | Converter - code cleanup and updated Javadoc | 1185 (100.0%) | 2688 (100.0%) | 3402 (100.0%) |
| 2 | 2024-12-02 | Converter now supports Collection, Array, and EnumSet conversions, including multiple dimension support, between collections of primitives, primitive wrappers, Date, Time, java.time.Temporal, the same set of base conversions that existed, are now supported in all possible way that I can think of. I'm sure I will hear from the Internet about other opportunities. | 1184 (99.9%) | 2688 (100.0%) | 3402 (100.0%) |
| 5 | 2024-12-02 | Improved performance of Convert by no longer requiring creating of a "pair" Map.Entry instance for lookup in the CONVERSION_DB and USER_DB. | 1184 (99.9%) | 2688 (100.0%) | 3352 (98.5%) |
| 6 | 2024-12-02 | Opened more tests to EnabledIf instead of Disabled. These can be run with -DperformRelease=true (whether you are doing an install or a deploy) | 1183 (99.8%) | 2688 (100.0%) | 3352 (98.5%) |
| 8 | 2024-12-03 | - Converter: performance improvements (no object creation when pairing keys) and improved scoring algorithm to find conversion pair that has the least distance in total for the source/target items. - UniqueIdGenerator - better approach to guarantee monotonically increase numbers and also supports a clock that may be been adjusted backward.  More supported added for cluster environments to identify unique node/pod (last component of it). Also fixed rate but possible non-monotonically increasing ID in rare race-condition with LinkedHashMap eviction. No longer maintains last 1000 or last 10,000 ids. | 1183 (99.8%) | 2679 (99.7%) | 3327 (97.8%) |
| 9 | 2024-12-03 | Opened more tests to EnabledIf instead of Disabled. These can be run with -DperformRelease=true (whether you are doing an install or a deploy) | 1183 (99.8%) | 2679 (99.7%) | 3324 (97.7%) |
| 11 | 2024-12-07 | - Adding more Javadoc - CaseInsensitiveMap, additional capabilities being added for the keySet() and entrySet() - toArray. Also may add the computeIfPresnet, etc. APIs to the CaseInsenstiveMap as well. | 1107 (93.4%) | 2679 (99.7%) | 3283 (96.5%) |
| 17 | 2024-12-14 | - Deprecated Sealable* - Converter now accurately says whether a Collection, Array, or EnumSet conversion is supported. | 1107 (93.4%) | 2678 (99.6%) | 3283 (96.5%) |
| 18 | 2024-12-15 | - recursive unmodifiability and synchronize on Collections when multidimensional copied - CollectionUtilities beefed up to handle all the wrapper types (wraps with most correct wrapper) - Massive Javadoc update | 842 (71.1%) | 2677 (99.6%) | 3264 (95.9%) |
| 30 | 2024-12-21 | Fighting backwards compatibility... | 842 (71.1%) | 2659 (98.9%) | 3238 (95.2%) |
| 31 | 2024-12-21 | - ReflectionUtils massively updated (improved, complete adjustible LRU caching) and new API to easily get methods (deep or shallow) - CompactMap now supports old construction patterns while offering new improved factory methods. - CompactMap factory method approach will be replaced with Builder pattern before this is released. | 841 (71.0%) | 2655 (98.8%) | 3238 (95.2%) |
| 37 | 2024-12-22 | - removed getNewMap(int size) as it was not always overwritten by users, leaving inconsistent behavior if they overwrote getNewMap() to return TreeMap(case insensitive, reverse order), but did not overwrite getNewMap(int size) to be the same way.  This improves correctness and avoids the customer running into hard-to-solve bugs. | 841 (71.0%) | 2652 (98.7%) | 3238 (95.2%) |
| 43 | 2024-12-24 | CompactMap automatic subclass generation for constants now complete.  CompactMap has one and only one member variable, 'val.'  And the class name will always be CompactMap$<options> | 841 (71.0%) | 2648 (98.5%) | 3237 (95.1%) |
| 61 | 2024-12-27 | DeepEquals breadcrumb trails | 841 (71.0%) | 2648 (98.5%) | 3233 (95.0%) |
| 63 | 2024-12-28 | - bread crumb output further refined. - Improved stack handling in deepEquals() | 841 (71.0%) | 2648 (98.5%) | 3141 (92.3%) |
| 68 | 2024-12-30 | - Formatting for deepEquals "diff" nearing completion.  Worrying about "fonts" and "colors" now (j/k) - ReflectionUtils is finished | 837 (70.6%) | 2648 (98.5%) | 3141 (92.3%) |
| 89 | 2025-01-07 | Deprecated CompactSet derivative classes. Use the builder pattern instead. | 835 (70.5%) | 2626 (97.7%) | 3110 (91.4%) |
| 90 | 2025-01-07 | Nearing Release. minor clean ups, Javadoc | 835 (70.5%) | 2626 (97.7%) | 3061 (90.0%) |
| 101 | 2025-01-10 | markdown updates | 834 (70.4%) | 2620 (97.5%) | 3051 (89.7%) |
| 106 | 2025-01-11 | Javadoc and userguide.md updates | 834 (70.4%) | 2619 (97.4%) | 3051 (89.7%) |
| 107 | 2025-01-11 | Useful SystemUtilities added. Javadoc and userguide.md added | 834 (70.4%) | 2619 (97.4%) | 3050 (89.6%) |
| 122 | 2025-01-15 | Removed references to CompactLinkedMapp | 834 (70.4%) | 2615 (97.3%) | 3031 (89.1%) |
| 123 | 2025-01-20 | > * [ClassUtilities](userguide.md#classutilities) adds >   * `Set<Class<?>> findLowestCommonSupertypes(Class<?> a, Class<?> b)` >     * which returns the lowest common anscestor(s) of two classes, excluding `Object.class.`  This is useful for finding the common ancestor of two classes that are not related by inheritance.  Generally, executes in O(n log n) - uses sort internally.  If more than one exists, you can filter the returned Set as you please, favoring classes, interfaces, etc. >   * `Class<?> findLowestCommonSupertype(Class<?> a, Class<?> b)` >     * which is a convenience method that calls the above method and then returns the first one in the Set or null. >   * `boolean haveCommonAncestor(Class<?> a, Class<?> b)` >     * which returns true if the two classes have a common ancestor (excluding `Object.class`). >   * `Set<Class<?>> getAllSupertypes(Class<?> clazz)` >     * which returns all superclasses and interfaces of a class, including itself.  This is useful for finding all the classes and interfaces that a class implements or extends. > * Moved `Sealable*` test cases to json-io project. > * Removed remaining usages of deprecated `CompactLinkedMap.` | 756 (63.8%) | 2079 (77.3%) | 2633 (77.4%) |
| 126 | 2025-01-24 | Timestamp outputs reliably in fully qualified ISO 8601 format (in UTC - "Z"), or it can read from epochMillis and optional nanos if passed that way to Converter. | 755 (63.7%) | 2079 (77.3%) | 2633 (77.4%) |
| 127 | 2025-01-25 | Date and sql Date conversions are no longer ambiguous on timezone.  The reader side (Map to Date, Map to sql date) always ensure timezone is included. If it is not specified (older JSON format) then it will use the system's timezone, which is what it did in the past. | 755 (63.7%) | 2077 (77.3%) | 2633 (77.4%) |
| 130 | 2025-01-26 | Cleaning up Calendar conversions and strengthening DateUtilities.parseDate | 755 (63.7%) | 2059 (76.6%) | 2633 (77.4%) |
| 131 | 2025-01-31 | Dates, java.util.Dates, and Timestamps now written out in String form by default. | 754 (63.6%) | 2058 (76.6%) | 2630 (77.3%) |
| 132 | 2025-02-01 | LocalDateTime, LocalDate, LocalTime, ZonedDateTime, OffsetDateTime all now use single String value for representation. | 754 (63.6%) | 2039 (75.9%) | 2630 (77.3%) |
| 133 | 2025-02-02 | - Added support for more number values to OffsetTime - Ensured no tests use Timestamp before year 0001 as it is not supported by java.sql.Timestamp - Ensured no tests use Calendar before year 0002, as it is not supported by Java Calendar. | 754 (63.6%) | 2028 (75.4%) | 2616 (76.9%) |
| 134 | 2025-02-03 | - All older Java date-time classes and newer temporal classes, output in "one field" when in Map form. - Many, many new tests added - Timezone handling improved - ZonedDateTime ISO_ZONE_DATE_TIME format used consistently and round trips. - GMT time supported but turns into Etc/GMT internally. | 754 (63.6%) | 2016 (75.0%) | 2571 (75.6%) |
| 135 | 2025-02-08 | - Added Currency and Pattern to Converter. - All Date-time types can be converted to Year, YearMonth, and MonthDay - java.sql.Date is treated as a true "date" with no surprises, like a birth date. | 754 (63.6%) | 2014 (74.9%) | 2550 (74.9%) |
| 143 | 2025-02-22 | TypeUtilities added | 754 (63.6%) | 2010 (74.8%) | 2534 (74.5%) |
| 153 | 2025-02-23 | Minor performance tweaks | 754 (63.6%) | 2008 (74.7%) | 2528 (74.3%) |
| 158 | 2025-02-24 | - Updated IOUtilities to use autoclose - Significant documentation updates on userguide | 753 (63.5%) | 2007 (74.7%) | 2528 (74.3%) |
| 160 | 2025-02-25 | Merge pull request #114 from kpartlow/kpartlow/cleaning-up-warnings | 753 (63.5%) | 2006 (74.6%) | 2528 (74.3%) |
| 168 | 2025-03-01 | performance and cleanup | 753 (63.5%) | 2006 (74.6%) | 2526 (74.2%) |
| 170 | 2025-03-06 | performance and cleanup | 753 (63.5%) | 2006 (74.6%) | 2522 (74.1%) |
| 178 | 2025-03-09 | deprecated shutdown api | 727 (61.4%) | 1914 (71.2%) | 2504 (73.6%) |
| 180 | 2025-03-23 | - Added back CompactMap/CompactSet specific subclasses to allow usage of common specialized CompactMaps/CompactSets without having to write specialized reader/writers. - ByteBuffer and CharBuffer converstions to/from Map added. - updated to use json-io 4.51.0 for testing - DEFAULT_FIELD_FILTER in ReflectionUtils made public. | 727 (61.4%) | 1910 (71.1%) | 2504 (73.6%) |
| 184 | 2025-03-26 | Merge pull request #116 from ozhelezniak-talend/ozhelezniak/fastwriter_int_write_fix | 727 (61.4%) | 1910 (71.1%) | 2503 (73.6%) |
| 193 | 2025-05-26 | converting groovy to java | 713 (60.2%) | 1893 (70.4%) | 2385 (70.1%) |
| 240 | 2025-06-14 | Merge pull request #162 from jdereg/codex/validate-capacity-in-lrucache-constructors | 713 (60.2%) | 1893 (70.4%) | 2384 (70.1%) |
| 241 | 2025-06-14 | Merge pull request #163 from jdereg/codex/modify-lockinglrucachestrategy.put-behavior | 713 (60.2%) | 1893 (70.4%) | 2383 (70.0%) |
| 244 | 2025-06-14 | Merge pull request #166 from jdereg/codex/update-ttlcache.put-and-add-tests | 713 (60.2%) | 1892 (70.4%) | 2380 (69.9%) |
| 247 | 2025-06-14 | Merge pull request #169 from jdereg/codex/update-hashcode-implementation-with-cachemap-entries | 711 (60.0%) | 1892 (70.4%) | 2380 (69.9%) |
| 249 | 2025-06-14 | Merge pull request #171 from jdereg/codex/review-trackingmap-for-defects-and-improvements | 711 (60.0%) | 1880 (69.9%) | 2281 (67.0%) |
| 255 | 2025-06-14 | Merge pull request #175 from jdereg/codex/review-byteutilities-for-improvements | 711 (60.0%) | 1879 (69.9%) | 2279 (67.0%) |
| 258 | 2025-06-14 | Merge pull request #177 from jdereg/tzkbmk-codex/review-and-improve-concurrentlist-code | 711 (60.0%) | 1875 (69.8%) | 2268 (66.6%) |
| 261 | 2025-06-15 | Merge pull request #180 from jdereg/4l9ef3-codex/review-and-improve-concurrentlist-code | 711 (60.0%) | 1875 (69.8%) | 2263 (66.5%) |
| 264 | 2025-06-15 | Merge pull request #183 from jdereg/codex/review-encryptionutilities-for-issues-and-improvements | 707 (59.7%) | 1871 (69.6%) | 2263 (66.5%) |
| 265 | 2025-06-15 | Merge pull request #184 from jdereg/codex/review-and-optimize-executor-class | 707 (59.7%) | 1869 (69.5%) | 2250 (66.1%) |
| 266 | 2025-06-15 | Merge pull request #185 from jdereg/codex/review-ioutilities-for-defects-and-improvements | 659 (55.6%) | 1869 (69.5%) | 2250 (66.1%) |
| 267 | 2025-06-15 | Merge pull request #186 from jdereg/codex/review-mathutilities-for-code-quality-improvements | 659 (55.6%) | 1864 (69.3%) | 2246 (66.0%) |
| 273 | 2025-06-15 | Merge pull request #191 from jdereg/codex/review-stringutilities-for-code-quality-improvements | 598 (50.5%) | 1862 (69.3%) | 2113 (62.1%) |
| 280 | 2025-06-15 | Merge pull request #198 from jdereg/codex/replace-system.out/system.err-with-java-logger | 598 (50.5%) | 1861 (69.2%) | 2113 (62.1%) |
| 283 | 2025-06-15 | Merge pull request #201 from jdereg/codex/standardize-java-logging-format | 598 (50.5%) | 1860 (69.2%) | 2113 (62.1%) |
| 300 | 2025-06-15 | Merge pull request #213 from jdereg/codex/write-junit-tests-for-collectionswrappers-apis | 598 (50.5%) | 1860 (69.2%) | 2113 (62.1%) |
