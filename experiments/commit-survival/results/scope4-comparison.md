# Week 1 scope classes inside the all-classes run

The 2026-11 week 1 java-util experiment generated tests for four scope classes only (`CompactMap`, `DeepEquals`, `ClassUtilities`, `ReflectionUtils`). This run generated tests for all classes. The tables below restrict this run's results to those four classes so the two runs can be compared on the same lines. One-time generation (no regeneration) only.

## 1. Changed-line coverage, split by class set

Computed from `results/coverage/<index>-<group>-lines.csv.gz` (per-commit line records) intersected with the post-image lines in `results/changes/<index>.json`; both are in the archive, not in this branch. The "All classes" column equals `results/policy/summary.csv` of the policy run (none row), and the 6,565 changed lines of the four classes equal the week 1 total.

| Group | All classes (11,572 lines) | Week 1 scope classes (6,565 lines) | Other classes (5,007 lines) |
|---|---:|---:|---:|
| Dev | 6,715 | 3,824 | 2,891 |
| KTester | 3,993 | 1,331 | 2,662 |
| Ouédraogo | 4,785 | 2,033 | 2,752 |

Per class (covered / changed executable lines):

| Group | ClassUtilities (1,051) | CompactMap (1,912) | DeepEquals (2,574) | ReflectionUtils (1,028) |
|---|---:|---:|---:|---:|
| Dev | 483 | 697 | 1,885 | 759 |
| KTester | 316 | 494 | 178 | 343 |
| Ouédraogo | 385 | 567 | 791 | 290 |

## 2. LLM test methods of the four classes at t

From `results/frozen-denominators.json` minus `results/control-excluded.json`, broken methods from `results/first-failures.csv`. A method belongs to the class it was generated for (KTester: the class prefix of the test class name; Ouédraogo: the class under test of the test file).

| Group | Class | At t | Broken by the last commit | Commits that broke the most |
|---|---|---:|---:|---|
| KTester | CompactMap | 105 | 26 | #30 (18), #43 (4), #37 (3) |
| KTester | DeepEquals | 0 | 0 | |
| KTester | ClassUtilities | 9 | 0 | |
| KTester | ReflectionUtils | 16 | 3 | #31 (3) |
| Ouédraogo | CompactMap | 39 | 26 | #30 (25), #43 (1) |
| Ouédraogo | DeepEquals | 95 | 95 | #63 (92), #61 (3) |
| Ouédraogo | ClassUtilities | 101 | 3 | #168 (2), #61 (1) |
| Ouédraogo | ReflectionUtils | 0 | 0 | |

## 3. Why KTester has no DeepEquals tests at t

All six KTester test files generated for `DeepEquals` (120 methods) failed to compile at t (screening records in `generated/ktester/round1/validation/`). The javac errors, with some files having more than one:

- a nested helper class or a method defined twice in the same test class: 4 files;
- `@ExtendWith(MockitoExtension.class)` used without importing `ExtendWith` or `MockitoExtension`: 3 files;
- an import of a package that does not exist (`com.cedarsoftware.util.test`): 1 file.

Across all KTester files, 111 of 1,118 in `generated/ktester/round1/java-util/test_classes/` declare `@ExtendWith(MockitoExtension.class)` without importing `org.junit.jupiter.api.extension.ExtendWith`.
