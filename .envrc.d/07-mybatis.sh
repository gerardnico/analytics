###################
# MYBatis (ie migrate cli)
###################
SDKMAN_MYBATIS_VERSION=3.3.11
if ! sdk home mybatis "${SDKMAN_MYBATIS_VERSION}" >/dev/null; then
  sdk install mybatis "${SDKMAN_MYBATIS_VERSION}"
fi
sdk use mybatis "${SDKMAN_MYBATIS_VERSION}"
# copy the driver
MYBATIS_DRIVER_DIR="$PROJECT_DIR/db/drivers"
POSTGRES_VERSION=42.7.3
if [ ! -f "$MYBATIS_DRIVER_DIR/postgresql-$POSTGRES_VERSION.jar" ]; then
  mvn dependency:copy \
    "-Dartifact=org.postgresql:postgresql:$POSTGRES_VERSION" \
    "-DoutputDirectory=$MYBATIS_DRIVER_DIR"
else
  echo "MBatis Postgres Driver found"
fi