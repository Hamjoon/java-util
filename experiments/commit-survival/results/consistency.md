# Consistency checks

```json
{
  "nonincreasing": true,
  "index_zero_equals_denominators": true,
  "raw_records": 882,
  "expected_raw_records": 882,
  "raw_count_matches": true,
  "missing_records": [],
  "build_fail_indices": [
    29,
    225,
    226,
    227,
    256,
    257
  ],
  "measured_raw_records": 882,
  "derived_raw_records": 0,
  "expected_measured_raw_records": 882,
  "expected_derived_raw_records": 0,
  "skipped_indices": [
    3,
    23,
    93,
    95,
    97,
    98,
    99,
    100,
    108,
    110,
    114,
    115,
    116,
    119,
    128,
    138,
    139,
    142,
    146,
    154,
    159,
    172,
    173,
    179,
    182,
    183,
    187,
    190,
    196,
    197,
    205,
    206,
    208,
    209,
    210,
    211,
    212,
    214,
    230,
    232,
    238,
    246,
    248,
    251,
    259,
    270,
    278,
    282,
    284,
    286,
    287,
    294,
    295,
    296,
    297,
    298,
    299,
    300
  ]
}
```

Coverage: identical executable line sets across all 3 groups at 233 indices. Required indices: 239. Missing measurements: 18 (enumerated in coverage-consistency.json).

## Three-group checks

| Check | Dev (scope) | KTester | Ouédraogo |
|---|---:|---:|---:|
| Fixed denominator | 346 | 171 | 375 |
| Final survivors | 181 | 135 | 241 |
| Survival records | 294 | 294 | 294 |
| Coverage records | 237 | 237 | 237 |
| Matching executable universes | 233 | 233 | 233 |
