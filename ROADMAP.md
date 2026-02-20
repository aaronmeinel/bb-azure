# Roadmap

Future improvements for bb-azure, organized by theme.

## Near-term

### Better Dry-run Mode
Currently `*dry-run*` just prints the command. Could be improved to:
- Return mock data for testing
- Generate shell script equivalent
- Track command history for replay

### Query Helpers
Common patterns from real usage:
```clojure
;; Instead of:
(az "webapp" "list" "-g" rg "--query" "[].name" "-o" "tsv")

;; Maybe:
(az-query ["webapp" "list" "-g" rg] "[].name")
```

### TSV Output Mode
Some queries benefit from TSV output (single values):
```clojure
(az-tsv "account" "show" "--query" "id")
;; => "12345-67890-..."
```

## Medium-term

### Resource Context
Thread common parameters (`-g`, `-n`, `--subscription`) through operations:
```clojure
(with-resource {:group "my-rg" :name "my-app"}
  (restart!)
  (show-config))
```

Open question: Is this actually simpler than explicit parameters?

### Stream Output
For long-running commands, stream output instead of waiting:
```clojure
(az-stream "deployment" "create" ...)
;; Prints lines as they arrive
```

### Test Fixtures
Mock az responses for testing:
```clojure
(with-az-mock {"account show" {:id "test" :name "Test"}}
  (az "account" "show"))
;; => {:id "test" :name "Test"}
```

## Long-term

### Resource Abstractions
Higher-level namespaces for common operations. Only if clear patterns emerge:

```clojure
;; bb-azure.webapp (hypothetical)
(webapp/restart rg name)
(webapp/logs rg name {:follow true})
(webapp/deploy rg name artifact)

;; bb-azure.acr
(acr/login registry)
(acr/push registry image)
(acr/list-repos registry)
```

Decision criteria:
- Must save significant boilerplate
- Must be commonly needed
- Must not hide important options

### Learning Mode
Generate documentation from usage:
```clojure
(with-learning
  (az "webapp" "show" "-g" "my-rg" "-n" "my-app"))
;; Prints: "webapp show - Get the details of a web app"
```

### JVM Compatibility
While designed for Babashka, ensure full JVM Clojure compatibility:
- Use only Babashka-compatible libraries
- Test on both platforms in CI
- Document any differences

## Non-goals

### Full Azure SDK Parity
The Azure SDK for Java has thousands of APIs. We won't replicate them. Use the SDK directly for complex scenarios.

### Azure AD Authentication
Azure CLI handles authentication. We delegate entirely to `az login`.

### Configuration Management
Use separate tools (params.edn, environment variables) for managing deployment configurations.

## Contributing

Ideas and PRs welcome! For new features, please open an issue first to discuss the design.
