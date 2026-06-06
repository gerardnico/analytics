# Should be first as it's used to set the path for maven, java and mybatis
# SDKMAN
# loading, as it's as the sdk command is a bash function
export SDKMAN_DIR=${SDKMAN_DIR:-"$HOME/.sdkman"}
SDKMAN_INIT_FILE="${SDKMAN_DIR}/bin/sdkman-init.sh"
if [[ ! -s "$SDKMAN_INIT_FILE" ]]; then
  echo "sdkman init file was not found and is mandatory (Path: $SDKMAN_INIT_FILE)"
  echo "You can"
  echo "  * install sdkman"
  echo "  * or overwrite the <.sdkman> dir by setting the SDKMAN_DIR env"
  return 1
fi
# We disable `set -o nounset` to avoid unbound variable errors
set +u
# shellcheck disable=SC1090
source "${SDKMAN_INIT_FILE}"

