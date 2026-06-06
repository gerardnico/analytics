###################
# Mavens
###################
SDKMAN_MAVEN_VERSION=3.9.9
if ! sdk home maven "${SDKMAN_MAVEN_VERSION}" >/dev/null; then
  sdk install maven "${SDKMAN_MAVEN_VERSION}"
fi
sdk use maven "${SDKMAN_MAVEN_VERSION}"
