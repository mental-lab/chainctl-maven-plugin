# chainctl-maven-plugin

A Maven plugin that verifies project dependencies against [Chainguard Libraries](https://www.chainguard.dev/) using `chainctl` before fat JAR assembly.

## What it does

During the `verify` phase the plugin shells out to `chainctl` and checks whether the project's resolved dependencies satisfy Chainguard's supply-chain policies. If `chainctl` exits with a non-zero status the build is failed immediately, preventing a non-compliant fat JAR from being produced.

## Prerequisites

* Java 11+
* Maven 3.6+
* `chainctl` installed and authenticated ([installation guide](https://edu.chainguard.dev/chainguard/chainctl/))

## Usage

Add the plugin to your `pom.xml`:

```xml
<build>
  <plugins>
    <plugin>
      <groupId>com.chainguard</groupId>
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

| Parameter | Property | Default | Description |
|-----------|----------|---------|-------------|
| `chainctlPath` | `chainctl.path` | `chainctl` | Path to the `chainctl` binary. Override when `chainctl` is not on `PATH`. |
| `skip` | `chainctl.skip` | `false` | Set to `true` to bypass the verification step. |

#### Example: custom binary path

```xml
<configuration>
  <chainctlPath>/usr/local/bin/chainctl</chainctlPath>
</configuration>
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

## Building from source

```bash
mvn install
```

## Status

This plugin is a scaffold / work in progress. The `verify-dependencies` goal currently stubs out the actual `chainctl libs verify` invocation — real integration is tracked in follow-up issues.
