# bb-azure

A lightweight Azure CLI wrapper for [Babashka](https://babashka.org/).

**bb-azure** eliminates boilerplate when shelling out to `az` commands. Parse JSON output, handle errors, and manage Azure resources with idiomatic Clojure.

## Installation

Add to your `bb.edn`:

```clojure
{:deps {io.github.yourusername/bb-azure
        {:git/url "https://github.com/yourusername/bb-azure"
         :git/sha "..."}}}
```

## Namespaces

| Namespace | Purpose |
|-----------|---------|
| `bb-azure.core` | Low-level `az` CLI wrapper |
| `bb-azure.webapp` | App Service operations |
| `bb-azure.acr` | Container Registry operations |
| `bb-azure.postgres` | PostgreSQL Flexible Server |
| `bb-azure.rg` | Resource group operations |
| `bb-azure.deploy` | Bicep deployments |
| `bb-azure.errors` | Error parsing & formatting |
| `bb-azure.output` | CLI output helpers |
| `bb-azure.utils` | Utilities (IP, secrets, EDN) |

## Quick Start

```clojure
;; Low-level: direct az commands
(require '[bb-azure.core :as az])
(az/az "account" "show")           ; => {:id "..." :name "..."}
(az/az! "group" "list")            ; throws on failure
(az/az-run! "webapp" "restart" "-g" "my-rg" "-n" "my-app")

;; High-level: resource operations
(require '[bb-azure.webapp :as webapp])
(webapp/restart! "my-rg" "my-app")
(webapp/logs! "my-rg" "my-app")    ; streams logs interactively

(require '[bb-azure.acr :as acr])
(acr/repos-show! "my-rg")          ; prints table of repos/tags
```

## Example: Minimal bb.edn

With bb-azure, your infrastructure tasks become one-liners:

```clojure
{:deps {bb-azure/bb-azure {:local/root "../bb-azure"}}
 :tasks
 {:requires ([bb-azure.webapp :as webapp]
             [bb-azure.rg :as rg]
             [clojure.edn :as edn])
  
  :init (do (def cfg (edn/read-string (slurp "config.edn")))
            (defn rg [] (str "rg-myapp-" (first *command-line-args*)))
            (defn app [] (str "app-myapp-" (first *command-line-args*))))
  
  restart {:doc "Restart: bb restart <env>" :task (webapp/restart! (rg) (app))}
  stop    {:doc "Stop: bb stop <env>"       :task (webapp/stop! (rg) (app))}
  start   {:doc "Start: bb start <env>"     :task (webapp/start! (rg) (app))}
  logs    {:doc "Logs: bb logs <env>"       :task (webapp/logs! (rg) (app))}
  ssh     {:doc "SSH: bb ssh <env>"         :task (webapp/ssh! (rg) (app))}
  status  {:doc "Status: bb status <env>"   :task (rg/show! (rg))}}}
```

## Core API

| Function | Returns | On Failure | Use |
|----------|---------|------------|-----|
| `az` | Parsed JSON | `nil` | Queries (missing = ok) |
| `az!` | Parsed JSON | Throws | Queries (must succeed) |
| `az-run!` | `nil` | Throws | Side effects |
| `az-interactive!` | - | - | SSH, logs, etc. |

### Control Modes

```clojure
(binding [az/*explain-mode* true]  ; print commands before executing
  (az/az "account" "show"))

(binding [az/*dry-run* true]       ; print but don't execute
  (az/az-run! "webapp" "restart" ...))
```

## Resource Namespaces

### bb-azure.webapp

```clojure
(webapp/restart! rg app)
(webapp/stop! rg app)
(webapp/start! rg app)
(webapp/logs! rg app)              ; interactive
(webapp/ssh! rg app)               ; interactive
(webapp/config rg app)             ; => [{:name "KEY" :value "val"} ...]
(webapp/config-show! rg app)       ; prints table
(webapp/config-set! rg app ["KEY=val" ...])
```

### bb-azure.acr

```clojure
(acr/find-acr rg)                  ; => "myacr" or nil
(acr/login! acr-name)              ; interactive
(acr/repos acr-name)               ; => ["repo1" "repo2"]
(acr/tags acr-name repo)           ; => ["latest" "v1.0"]
(acr/repos-show! rg)               ; prints table
```

### bb-azure.deploy

```clojure
(deploy/deploy! {:location "westeurope"
                 :template "main.bicep"
                 :name "deploy-123"
                 :params {:env "dev" :sku "B1"}})
;; => {:ok true} or {:error {...}}

(deploy/whatif! {...})             ; => {:status "..." :changes [...]}
(deploy/deploy-with-output! {...} on-success-fn)  ; formatted output
```

### bb-azure.rg

```clojure
(rg/exists? rg)                    ; => true/false
(rg/resources rg)                  ; => [{:name "..." :type "..."} ...]
(rg/show! rg)                      ; prints table
(rg/delete! rg "dev")              ; prompts for confirmation
```

## Error Handling

```clojure
(require '[bb-azure.errors :as errors])

(errors/parse-azure-error stderr)
;; => {:error {:code "..." :message "..." :details [...]}}

(errors/format-error error)
;; => "InvalidTemplate: Deployment failed
;;       └─ ResourceError: SKU not available"

(errors/save-error! error)         ; => ".last-error.edn"
```

## CLI Output

```clojure
(require '[bb-azure.output :as out])

(out/info "Deploying...")          ; cyan
(out/success "Done!")              ; green ✓
(out/warn "Careful")               ; yellow ⚠
(out/error "Failed")               ; red ✗
(out/die! "Fatal error")           ; red ✗ + exit 1
(out/header "Section")             ; bold + separator line
(out/confirm "Delete?")            ; y/N prompt
(out/confirm-typed "Delete?" "prod") ; type to confirm
```

## Prerequisites

- [Babashka](https://babashka.org/)
- [Azure CLI](https://docs.microsoft.com/en-us/cli/azure/) (`az login`)

## License

MIT
