package com.chainguard.maven.plugin;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * Unit tests for {@link VerifyDependenciesMojo}.
 *
 * <p>Full integration tests (requiring a real Maven reactor and chainctl binary) should
 * live in src/it/ and be driven by the maven-invoker-plugin. The tests here cover
 * unit-level behaviour that can be exercised without spawning a subprocess.</p>
 */
public class VerifyDependenciesMojoTest {

    @Test
    public void mojoCanBeInstantiated() {
        VerifyDependenciesMojo mojo = new VerifyDependenciesMojo();
        assertNotNull(mojo);
    }

    // TODO: add tests that mock ProcessBuilder / inject a fake chainctl binary to
    //       verify:
    //  - skip=true suppresses execution
    //  - non-zero exit code throws MojoFailureException
    //  - IOException on missing binary throws MojoExecutionException
}
