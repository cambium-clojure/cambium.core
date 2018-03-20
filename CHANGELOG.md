# cambium.core - TODO and Change Log

## TODO

- [TODO] Automatically put `ex-data` of `ExceptionInfo` into context
  - Configuration var: `cambium.core/*ex-data-key*`
  - Default value is `nil` (any falsy value is a no-op)
  - Value `true` merges to current context, any other truthy (e.g. `:ex-data`, `[:foo :bar]`) considered context key
  - Resolved at compile-time (in the macro)
- [TODO] Key codec support
  - Candidate use-case: EDN
- [TODO] Configurable level for `deflevel` name prefixes
  - E.g. `couchbase.success.*=debug`, `couchbase.error.*=info` for logger `METRICS`
  - Impl: Auxiliary logger name, e.g. `METRICS.couchbase.success` for 'enabled?' check
  - Impl: Regular logger name, e.g. `METRICS` for logging
  - Impl: Prefix->level map must be available before `deflevel` is eval'ed (compile time)


## [WIP] 0.9.2 / 2018-March-??

- [TODO] Put line number automatically efficiently
  - https://medium.com/@hlship/macros-meta-and-logging-575d5047924c
- [Todo] Use cambium.codec-simple 0.9.2 for testing


## 0.9.1 / 2017-September-11

- Inherited code from cambium.core `0.9.0`
  - Old repo: https://github.com/kumarshantanu/cambium
- Bump clojure/tools.logging dependency version to `0.4.0`
