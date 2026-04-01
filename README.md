# chainctl-maven-plugin

A Maven plugin that verifies project dependencies against [Chainguard Libraries](https://www.chainguard.dev/) using `chainctl` before fat JAR assembly.

## What it does

During the `verify` phase the plugin walks the full dependency tree and shells out to `chainctl libraries verify <jar>` for each resolved JAR. A summary line is printed at the end:

```
X/Y dependencies verified from Chainguard Libraries
```

If any dependency fails verification and `failOnUnverified` is `true` (the default), the build is failed — preventing a non-compliant fat JAR from being produced. Set `failOnUnverified` to `false` to log a warning instead.

## Prerequisites

* Java 8+
* Maven 3.6+
* `chainctl` installed and authenticated ([installation guide](https://edu.chainguard.dev/chainguard/chainctl/))

## Usage

Add the plugin to your `pom.xml`:

```xml
<build>
  <plugins>
    <plugin>
      <groupId>com.chainctl</groupId>
      <artifactId>chainctl-maven-plugin</artifactId>
      <version>1.0.0-SNAPSHOT</version>
      <executions>
        <execution>
          <goals>
            <goal>verify-dependencies</goal>
          </goals>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

### Configuration

| Parameter | Property | Default | Env var override | Description |
|-----------|----------|---------|-----------------|-------------|
| `chainctlPath` | `chainctl.path` | `chainctl` | — | Path to the `chainctl` binary. Override when `chainctl` is not on `PATH`. |
| `failOnUnverified` | `chainctl.failOnUnverified` | `true` | `CHAINCTL_FAIL_ON_UNVERIFIED` | Fail the build when unverified dependencies are found. Set to `false` to warn instead. |
| `ignoredScopes` | `chainctl.ignoredScopes` | `test,provided` | `CHAINCTL_IGNORED_SCOPES` | Comma-separated list of dependency scopes to exclude from verification. |
| `skip` | `chainctl.skip` | `false` | — | Set to `true` to bypass the verification step entirely. |

#### Example: custom binary path

```xml
<configuration>
  <chainctlPath>/usr/local/bin/chainctl</chainctlPath>
</configuration>
```

#### Example: warn instead of fail

```xml
<configuration>
  <failOnUnverified>false</failOnUnverified>
</configuration>
```

Or via environment variable:

```bash
CHAINCTL_FAIL_ON_UNVERIFIED=false mvn verify
```

#### Example: change ignored scopes

```xml
<configuration>
  <ignoredScopes>test,provided,system</ignoredScopes>
</configuration>
```

Or via environment variable:

```bash
CHAINCTL_IGNORED_SCOPES=test,provided,system mvn verify
```

#### Example: skip verification for a specific profile

```xml
<profiles>
  <profile>
    <id>skip-chainctl</id>
    <properties>
      <chainctl.skip>true</chainctl.skip>
    </properties>
  </profile>
</profiles>
```

Or from the command line:

```bash
mvn verify -Dchainctl.skip=true
```

## Fat JAR caveat

This plugin verifies the individual dependency JARs present in the local Maven repository **before** assembly. It does not inspect the assembled fat JAR itself. Ensure the `verify-dependencies` goal runs before your assembly plugin (e.g. `maven-shade-plugin` or `maven-assembly-plugin`) to catch violations prior to packaging.

## Building from source

```bash
mvn install
```
