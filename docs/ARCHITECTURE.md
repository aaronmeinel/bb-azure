# Architecture

This document explains the design decisions behind bb-azure.

## Design Philosophy

### 1. Thin Wrapper, Not an SDK

bb-azure wraps the Azure CLI rather than the Azure SDK because:

- **No dependencies**: Azure CLI is already installed on most developer machines
- **Consistent behavior**: Same commands work in scripts and interactive shells
- **Documentation reuse**: Azure CLI docs apply directly
- **Simple maintenance**: No API version tracking required

The tradeoff is startup latency (spawning `az` processes), which is acceptable for automation scripts.

### 2. Four Functions, Clear Semantics

The API consists of just four functions with distinct behaviors:

| Function | Output | `-o` flag | Failure Mode |
|----------|--------|-----------|--------------|
| `az` | parsed JSON | `json` | returns `nil` |
| `az!` | parsed JSON | `json` | throws ex-info |
| `az-run!` | `nil` | `none` | throws ex-info |
| `az-interactive!` | - | - | inherits stdio |

This design:
- Makes intent explicit (query vs side-effect vs interactive)
- Follows Clojure naming conventions (`!` for side effects)
- Allows pipeline composition with `az` (nil-punning)

### 3. Dynamic Configuration via Binding

Control modes use dynamic vars:

```clojure
(binding [*explain-mode* true]
  (az "account" "show"))
```

This approach:
- Works with existing code without modification
- Composes naturally (wrap any block of code)
- Avoids parameter threading through call stacks
- Is familiar to Clojure developers

Alternative considered: Passing options map. Rejected because it would require modifying every function signature and call site.

### 4. Errors as Data

Azure errors are parsed into Clojure maps, not wrapped in custom exception types:

```clojure
{:error
 {:code "InvalidTemplateDeployment"
  :message "Deployment failed"
  :details [{:code "ResourceDeploymentFailure" ...}]}}
```

Benefits:
- Standard Clojure data manipulation
- Easy serialization (EDN, JSON)
- No custom exception hierarchies
- REPL-friendly inspection

## Module Separation

### core.clj
The essential az wrapper. Zero dependencies beyond babashka.process and cheshire (for JSON). This module alone is useful.

### errors.clj
Error parsing and formatting. Depends only on cheshire and clojure.pprint. Useful for deployment scripts that need better error messages.

### output.clj
CLI output helpers. Pure Clojure with ANSI codes. Completely optional - use your own output strategy if preferred.

## Future Considerations

See [ROADMAP.md](../ROADMAP.md) for planned improvements.

### Resource Namespaces

We intentionally avoided resource-specific namespaces (e.g., `bb-azure.webapp`, `bb-azure.acr`) in v1 because:

1. They add maintenance burden (tracking az CLI changes)
2. Simple scripts don't need them
3. Users can easily build their own abstractions

This may change as patterns emerge from real-world usage.

### Retry Logic

Removed from v1 because:
1. Azure CLI already has built-in retry for transient errors
2. Different operations need different retry strategies
3. Adds complexity for rarely-used functionality

If needed, use a generic retry library like [safely](https://github.com/BrunoBonacci/safely).

## Naming

"bb-azure" follows the convention of babashka libraries:
- `bb-` prefix indicates Babashka-first design
- Works in JVM Clojure too (no bb-specific APIs used)
