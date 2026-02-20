# bb-azure

A lightweight Azure CLI wrapper for [Babashka](https://babashka.org/).

**bb-azure** eliminates the boilerplate of shelling out to `az` commands, parsing JSON output, and handling errors. Write declarative Azure automation scripts in Clojure that are easy to read, test, and maintain.

## Installation

Add to your `bb.edn`:

```clojure
{:deps {io.github.yourusername/bb-azure
        {:git/url "https://github.com/yourusername/bb-azure"
         :git/sha "..."}}}
```

Or copy the `src/bb_azure/` folder into your project.

## Quick Start

```clojure
(require '[bb-azure.core :as az])

;; Get current account info
(az/az "account" "show")
;; => {:id "...", :name "My Subscription", ...}

;; List resource groups (throws on failure)
(az/az! "group" "list")

;; Execute side-effect commands
(az/az-run! "webapp" "restart" "-g" "my-rg" "-n" "my-app")

;; Interactive commands (SSH, etc.)
(az/az-interactive! "webapp" "ssh" "-g" "my-rg" "-n" "my-app")
```

## Core API

| Function | Returns | On Failure | Use Case |
|----------|---------|------------|----------|
| `az` | Parsed JSON | `nil` | Queries where missing data is acceptable |
| `az!` | Parsed JSON | Throws | Queries that must succeed |
| `az-run!` | `nil` | Throws | Side effects (restart, delete, etc.) |
| `az-interactive!` | - | - | Interactive sessions (SSH, etc.) |

### Control Modes

```clojure
;; Print commands before executing (debugging)
(binding [az/*explain-mode* true]
  (az/az "account" "show"))
;; $ az account show -o json
;; => {:id "...", ...}

;; Print commands without executing (dry run)
(binding [az/*dry-run* true]
  (az/az-run! "webapp" "restart" "-g" "rg" "-n" "app"))
;; $ az webapp restart -g rg -n app -o none
;; => nil (nothing executed)
```

## Error Handling

Azure deployment errors can be deeply nested. The `bb-azure.errors` namespace helps parse and format them:

```clojure
(require '[bb-azure.errors :as errors])

;; Parse error JSON from stderr
(errors/parse-azure-error stderr)
;; => {:error {:code "InvalidTemplateDeployment" :message "..." :details [...]}}

;; Format as readable tree
(errors/format-error parsed-error)
;; => "InvalidTemplateDeployment: Deployment failed
;;       └─ ResourceDeploymentFailure: Resource operation failed
;;          └─ BadRequest: SKU not available"

;; Save to file for debugging
(errors/save-error! parsed-error)
;; => ".last-error.edn"
```

## CLI Output Helpers

Build colorful CLI applications with `bb-azure.output`:

```clojure
(require '[bb-azure.output :as out])

(out/header "Deploying Infrastructure")
(out/info "Creating resource group...")
(out/success "Resource group created")
(out/warn "SKU may not be available in all regions")
(out/error "Deployment failed")
(out/die! "Cannot continue")  ; Prints error and exits with code 1

;; User confirmation
(when (out/confirm "Delete resource group?")
  (az/az-run! "group" "delete" "-n" "my-rg" "-y"))
```

## Prerequisites

- [Babashka](https://babashka.org/) installed
- [Azure CLI](https://docs.microsoft.com/en-us/cli/azure/install-azure-cli) installed and logged in (`az login`)

## Project Structure

```
bb-azure/
├── bb.edn                    # Babashka config
├── src/bb_azure/
│   ├── core.clj              # Core az wrappers
│   ├── errors.clj            # Error parsing and formatting
│   └── output.clj            # CLI output helpers
├── docs/
│   └── ARCHITECTURE.md       # Design decisions
├── examples/
│   └── deploy.clj            # Example deployment script
└── ROADMAP.md                # Future plans
```

## Documentation

- [Architecture](docs/ARCHITECTURE.md) - Design decisions and rationale
- [Roadmap](ROADMAP.md) - Future improvements

## Related Projects

- [Babashka](https://babashka.org/) - Fast native Clojure scripting
- [Azure CLI](https://docs.microsoft.com/en-us/cli/azure/) - Official Azure command-line tool

## License

MIT
