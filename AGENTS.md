# AGENTS.md

Spring Boot 4 (parent 4.1.0) / Spring Framework 6 REST client on **Java 25** (enforced by the
maven-enforcer plugin). Single Maven module, package `guru.springframework.spring6restclient`. It is
an OAuth2 JWT client that consumes the `spring-6-rest-mvc` REST API (auth via `spring-6-auth-server`)
and exposes a Thymeleaf web UI.

## Build & test commands

- Full build: `./mvnw clean verify` — format checks, unit (`*Test`, surefire) + IT (`*IT`, failsafe)
  tests, Helm lint/template. `./mvnw verify` also runs the unit tests.
- Unit tests only: `./mvnw test`. Single test: `./mvnw test -Dtest=Spring6RestclientApplicationTest#methodName`.
- `./mvnw clean install` additionally builds the Docker image and packages the Helm chart into
  `target/helm/repo/`. Skip the Docker build with `-Dskip.docker.build=true` /
  `-Dskip.start.stop.springboot=true`.
- Start locally: `./mvnw spring-boot:run` (app on `:8085`, requires auth-server on `:9000` and
  rest-mvc on `:8081`).

After changing code, always verify: run the relevant Maven goal above and report its output
(evidence, not just "done").

## Sandbox build quirk (background)

This sandbox mounts the repo via filesystem passthrough, which blocks symlinks — Spotless's
`npm install` (prettier) would fail with `EPERM` unless npm skips bin links. The sandbox kit sets
`npm_config_bin_links=false` globally (`spec.yaml` → `environment.variables`), so no manual export
is needed here. On a normal host (Windows/CI) this does not apply either.

## Formatting is enforced (fails the `validate` phase)

- Java: Spring Java Format → fix with `./mvnw spring-javaformat:apply`.
- Everything else (pom.xml, `**/*.md`, json, `src/main/resources/application*.yaml`, `**/*.sh`):
  Spotless → fix with `./mvnw spotless:apply`. Spotless uses shfmt `3.13.1` for shell scripts.
- Spotless excludes `AGENTS.md`/`CLAUDE.md` from flexmark (markdown) formatting.

## External dependency gotcha

- The auth-server (`spring-6-auth-server`) is resolved from the auth-server project's GitHub Packages
  (`maven.pkg.github.com`) / Helm repo (`repo.repsy.io/user08694146/helm-dboeckli`). Without a PAT in
  `~/.m2/settings.xml` (server id `github`) the build cannot resolve the snapshot dependency.
- The Helm chart depends on **4 aliased subcharts**: `spring-6-auth-server` (Repsy), `spring-6-rest-mvc`
  + its `-mysql`/`-kafka` charts (Cloudsmith `oci://docker.cloudsmith.io/dboeckli/dboeckli-cloudsmith-repo`).
  `helm dependency build` pulls them during the build; all subcharts get a `fullnameOverride` so their
  service names are release-independent. The Cloudsmith charts are private — CI logs in via
  `helm registry login docker.cloudsmith.io` (`CLOUDSMITH_USERNAME` var, `CLOUDSMITH_API_KEY` secret),
  locally you need a manual `helm registry login` before building.
- Cloudsmith workaround: use the Docker-OCI URL `docker.cloudsmith.io`, not the native Helm endpoint
  `helm.oci.cloudsmith.io` (Early Access, currently returns 500). Tracked in spring-6-rest-mvc #210 —
  monitor stability before further Cloudsmith adoption.

## Test conventions

- Naming matters: `*Test` = unit (surefire), `*IT` = integration (failsafe). A `*Test` class will
  not run during `verify`'s failsafe phase and vice versa.
- `BeerClientImplWithTestContainerIT` uses Testcontainers: MySQL, Kafka, `spring-6-auth-server`,
  `spring-6-rest-mvc` and `spring-6-gateway` images (`domboeckli/*` tags track the respective app's
  Helm chart version, lowercase `-snapshot`). It needs Docker and the pullable images.

## Architecture

- `client/` contains the RestClient-based HTTP clients to `spring-6-rest-mvc`; `web/` the Thymeleaf
  UI (Bootstrap via webjars), `config/` the OAuth2/security setup, `dto/` the data model.
- The app is an OAuth2 JWT client (`spring-boot-starter-oauth2-client`) using authorization-code flow
  against `spring-6-auth-server`.
- Env / Helm values wire the upstream FQDNs (see `helm-charts/templates/deployment.yaml`).

## Helm / Deploy

- Chart in `helm-charts/`, packaged to `target/helm/repo/spring-6-restclient-chart-<version>.tgz`,
  release name = `spring-6-restclient`, namespace `spring-6-restclient`.
- CI (`.github/workflows/`): `maven-build.yml` builds + deploys snapshots and triggers
  `deploy-and-test-cluster.yml` (in-cluster); `release.yml` runs `mvn release:prepare release:perform`
  on main/master only (version must be `-SNAPSHOT`); SonarCloud analysis runs in the `analyze` job.
- Dependency updates are managed via `.github/renovate.json`; validate changes with
  `renovate-config-validator`.
