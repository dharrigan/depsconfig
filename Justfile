#
# And awayyyy we go!
#

set dotenv-load := true
set positional-arguments := true

# List all recipes (_ == hidden recipe)
_default:
    @just --list

# Cat the Justfile
@cat:
    just --dump

# Upgrade dependencies
@deps:
    clojure -X:antq
