#!/bin/bash -x

set -o errexit
set -o errtrace
set -o nounset
set -o pipefail

for d in */; do
    echo "$d" && cd "$d" && (mvn wrapper:wrapper || echo "Failed") && cd ..
done
