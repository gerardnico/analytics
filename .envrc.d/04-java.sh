# Note sdkman vs nix (May be devbox?)
# We don't use nix because the jdk11 package distro is difficult to know and the version to pinpoint
# FYI:
# Nix: OpenJDK Runtime Environment (build 11.0.25+0-adhoc..source)
# Temurin: OpenJDK Runtime Environment Temurin-11.0.26+4 (build 11.0.26+4)

# Java Temurin
# We also download it with maven, may be we could download it once
export JDK_VERSION
JDK_VERSION=$(yq --exit-status '.project.properties."jdk.version"' pom.xml)
export JRELEASER_PROJECT_LANGUAGES_JAVA_VERSION=${JDK_VERSION}
export JDK_DISTRIBUTION
JDK_DISTRIBUTION=$(yq --exit-status '.project.properties."jdk.distribution"' pom.xml | cut -c 1-3)
SDKMAN_JDK_VERSION="${JDK_VERSION}-${JDK_DISTRIBUTION}"
if ! sdk home java "${SDKMAN_JDK_VERSION}" >/dev/null; then
  sdk install java "${SDKMAN_JDK_VERSION}"
fi
sdk use java "${SDKMAN_JDK_VERSION}"

