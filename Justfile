#
# And awayyyy we go!
#

set dotenv-load
set positional-arguments
set quiet

# List all recipes (_ == hidden recipe)
_default:
    just --list

# Cat the Justfile
cat:
    just --dump

# Upgrade dependencies
deps:
    clojure -X:antq
