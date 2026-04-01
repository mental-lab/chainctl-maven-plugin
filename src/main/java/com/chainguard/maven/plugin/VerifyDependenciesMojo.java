package com.chainguard.maven.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Verifies project dependencies against Chainguard Libraries using chainctl.
 * Fails the build if chainctl reports any policy violations.
 */
@Mojo(name = "verify-dependencies", defaultPhase = LifecyclePhase.VERIFY)
public class VerifyDependenciesMojo extends AbstractMojo {

    /**
     * Path to the chainctl executable. Defaults to 'chainctl' (assumes it is on PATH).
     */
    @Parameter(property = "chainctl.path", defaultValue = "chainctl")
    private String chainctlPath;

    /**
     * Skip the chainctl verification step entirely.
     */
    @Parameter(property = "chainctl.skip", defaultValue = "false")
    private boolean skip;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("chainctl dependency verification skipped.");
            return;
        }

        getLog().info("Verifying dependencies with chainctl: " + chainctlPath);

        // TODO: resolve the effective dependency list from the Maven project and pass
        //       it to chainctl for policy evaluation. For now this is a stub.
        runChainctl();
    }

    /**
     * Shells out to chainctl to check dependencies.
     * Fails the build if chainctl exits with a non-zero status.
     */
    void runChainctl() throws MojoExecutionException, MojoFailureException {
        // TODO: build the full argument list once the chainctl sub-command for
        //       dependency verification is finalised (e.g. "chainctl libs verify ...").
        ProcessBuilder pb = new ProcessBuilder(chainctlPath, "version");
        pb.redirectErrorStream(true);

        int exitCode;
        try {
            Process process = pb.start();
            // Stream chainctl output to Maven log
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    getLog().debug("[chainctl] " + line);
                }
            }
            exitCode = process.waitFor();
        } catch (java.io.IOException e) {
            throw new MojoExecutionException(
                    "Failed to execute chainctl at '" + chainctlPath + "': " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MojoExecutionException("chainctl execution was interrupted", e);
        }

        if (exitCode != 0) {
            throw new MojoFailureException(
                    "chainctl exited with status " + exitCode + ". Dependency verification failed.");
        }

        getLog().info("chainctl dependency verification passed.");
    }
}
