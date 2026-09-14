#!/usr/bin/env bash
# Source this file before building:  source scripts/dev-env.sh

export JAVA_HOME="${HOME}/.local/opt/jdk-21"
export PATH="${JAVA_HOME}/bin:${HOME}/.local/opt/maven/bin:${PATH}"

# Optional: export OPENROUTER_API_KEY=sk-or-... before starting the API for Samosa AI.
# Never commit the key. Without it, assistant falls back to local menu tools.

echo "JAVA_HOME=${JAVA_HOME}"
java -version
mvn -version
if [[ -n "${OPENROUTER_API_KEY:-}" ]]; then
  echo "OPENROUTER_API_KEY is set (Samosa AI LLM enabled)"
else
  echo "OPENROUTER_API_KEY unset (Samosa AI uses local tools only)"
fi
