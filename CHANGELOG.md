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


## 1.1.1 / 2021-December-16

- Bump test dependencies to latest
- Bump SLF4j version to 1.7.32 - fixes #7
- Bump clojure/tools.logging to 1.2.2


## 1.1.0 / 2021-April-04

- Add utility fn `cambium.codec.util/dissoc-in`
- Add function `cambium.core/transform-context` to transform context attributes
  - Override with `alter-var-root`
  - Example use case: Redact sensitive attributes


## 1.0.0 / 2020-September-29

- Drop support for Clojure `1.5.x`
- Update tools.logging to version `1.1.0`
- Update SLF4j to version `1.7.30`


## 1.0.0-rc1 / 2019-July-09

- Add function `cambium.mdc/get-raw-mdc`
- Add `cambium.trace` namespace
  - Function middleware to extract and propagate trace info
  - Ring middleware for distributed tracing
    - Plus request and response logging


## 0.9.3 / 2019-May-07

- Upgrade dependencies
  - clojure.tools.logging to version `0.4.1`
  - SLF4j to version `1.7.26`
- Overload log API arities `[msg-or-throwable] [mdc-or-throwable msg] [mdc throwable msg]`


## 0.9.2 / 2018-March-22

- Put source namespace, line and column numbers into context automatically for every log event
  - Discovered at compile time (unlike Java/Logback), so quite inexpensive
  - Disable with system property `cambium.caller.meta.in.context` set to `false`
    - See also: `cambium.core/caller-meta-in-context?`
  - See also: https://medium.com/@hlship/macros-meta-and-logging-575d5047924c


## 0.9.1 / 2017-September-11

- Inherited code from cambium.core `0.9.0`
  - Old repo: https://github.com/kumarshantanu/cambium
- Bump clojure/tools.logging dependency version to `0.4.0`
