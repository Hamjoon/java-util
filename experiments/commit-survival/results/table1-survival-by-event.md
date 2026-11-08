# Table 1. Survival by event

| Index | Date | Subject | Dev (scope) | KTester | Ouédraogo |
|---:|---|---|---:|---:|---:|
| 0 | 2024-10-17 | t = 2.18.0 | 346 (100.0%) | 171 (100.0%) | 375 (100.0%) |
| 6 | 2024-12-02 | Opened more tests to EnabledIf instead of Disabled. These can be run with -DperformRelease=true (whether you are doing an install or a deploy) | 345 (99.7%) | 171 (100.0%) | 375 (100.0%) |
| 18 | 2024-12-15 | - recursive unmodifiability and synchronize on Collections when multidimensional copied - CollectionUtilities beefed up to handle all the wrapper types (wraps with most correct wrapper) - Massive Javadoc update | 229 (66.2%) | 171 (100.0%) | 375 (100.0%) |
| 30 | 2024-12-21 | Fighting backwards compatibility... | 229 (66.2%) | 150 (87.7%) | 373 (99.5%) |
| 31 | 2024-12-21 | - ReflectionUtils massively updated (improved, complete adjustible LRU caching) and new API to easily get methods (deep or shallow) - CompactMap now supports old construction patterns while offering new improved factory methods. - CompactMap factory method approach will be replaced with Builder pattern before this is released. | 228 (65.9%) | 147 (86.0%) | 372 (99.2%) |
| 43 | 2024-12-24 | CompactMap automatic subclass generation for constants now complete.  CompactMap has one and only one member variable, 'val.'  And the class name will always be CompactMap$<options> | 228 (65.9%) | 147 (86.0%) | 370 (98.7%) |
| 61 | 2024-12-27 | DeepEquals breadcrumb trails | 228 (65.9%) | 142 (83.0%) | 367 (97.9%) |
| 63 | 2024-12-28 | - bread crumb output further refined. - Improved stack handling in deepEquals() | 228 (65.9%) | 142 (83.0%) | 257 (68.5%) |
| 64 | 2024-12-28 | formatting deepEquals differences getting much better... | 228 (65.9%) | 141 (82.5%) | 257 (68.5%) |
| 68 | 2024-12-30 | - Formatting for deepEquals "diff" nearing completion.  Worrying about "fonts" and "colors" now (j/k) - ReflectionUtils is finished | 224 (64.7%) | 136 (79.5%) | 256 (68.3%) |
| 88 | 2025-01-06 | DeepEquals "diff" breadcrumb output complete. | 224 (64.7%) | 135 (78.9%) | 256 (68.3%) |
| 89 | 2025-01-07 | Deprecated CompactSet derivative classes. Use the builder pattern instead. | 223 (64.5%) | 135 (78.9%) | 255 (68.0%) |
| 158 | 2025-02-24 | - Updated IOUtilities to use autoclose - Significant documentation updates on userguide | 223 (64.5%) | 135 (78.9%) | 242 (64.5%) |
| 160 | 2025-02-25 | Merge pull request #114 from kpartlow/kpartlow/cleaning-up-warnings | 223 (64.5%) | 135 (78.9%) | 241 (64.3%) |
| 193 | 2025-05-26 | converting groovy to java | 212 (61.3%) | 135 (78.9%) | 241 (64.3%) |
| 264 | 2025-06-15 | Merge pull request #183 from jdereg/codex/review-encryptionutilities-for-issues-and-improvements | 211 (61.0%) | 135 (78.9%) | 241 (64.3%) |
| 266 | 2025-06-15 | Merge pull request #185 from jdereg/codex/review-ioutilities-for-defects-and-improvements | 181 (52.3%) | 135 (78.9%) | 241 (64.3%) |
| 300 | 2025-06-15 | Merge pull request #213 from jdereg/codex/write-junit-tests-for-collectionswrappers-apis | 181 (52.3%) | 135 (78.9%) | 241 (64.3%) |
