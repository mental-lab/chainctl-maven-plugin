package com.chainctl;

import com.chainctl.utils.ArtifactUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyNode;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Verifies project dependencies against Chainguard Libraries using chainctl.
 * Fails the build if any dependency cannot be verified and failOnUnverified is true.
 */
@Mojo(name = "verify-dependencies",
      defaultPhase = LifecyclePhase.VERIFY,
      requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class VerifyDependenciesMojo extends AbstractMojo {

    /**
     * Path to the chainctl executable. Defaults to 'chainctl' (assumes it is on PATH).
     */
    @Parameter(property = "chainctl.path", defaultValue = "chainctl")
    private String chainctlPath;

    /**
     * Whether to fail the build when unverified dependencies are found.
     * Can be overridden by the CHAINCTL_FAIL_ON_UNVERIFIED environment variable.
     */
    @Parameter(property = "chainctl.failOnUnverified", defaultValue = "true")
    private boolean failOnUnverified;

    /**
     * Comma-separated list of dependency scopes to skip during verification.
     * Can be overridden by the CHAINCTL_IGNORED_SCOPES environment variable.
     */
    @Parameter(property = "chainctl.ignoredScopes", defaultValue = "test,provided")
    private String ignoredScopes;

    /**
     * Skip the chainctl verification step entirely.
     */
    @Parameter(property = "chainctl.skip", defaultValue = "false")
    private boolean skip;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Component
    private DependencyGraphBuilder dependencyGraphBuilder;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("chainctl dependency verification skipped.");
            return;
        }

        // Apply environment variable overrides
        String envFailOnUnverified = System.getenv(Config.ENV_FAIL_ON_UNVERIFIED);
        if (envFailOnUnverified != null && !envFailOnUnverified.isEmpty()) {
            failOnUnverified = Boolean.parseBoolean(envFailOnUnverified);
        }

        String envIgnoredScopes = System.getenv(Config.ENV_IGNORED_SCOPES);
        if (envIgnoredScopes != null && !envIgnoredScopes.isEmpty()) {
            ignoredScopes = envIgnoredScopes;
        }

        Set<String> scopesToSkip = new HashSet<String>();
        for (String scope : ignoredScopes.split(",")) {
            String trimmed = scope.trim();
            if (!trimmed.isEmpty()) {
                scopesToSkip.add(trimmed);
            }
        }

        // Collect artifacts from dependency tree
        Set<Artifact> artifacts;
        try {
            DependencyNode rootNode = dependencyGraphBuilder.buildDependencyGraph(project, null);
            artifacts = collectArtifacts(rootNode, scopesToSkip);
        } catch (DependencyGraphBuilderException e) {
            throw new MojoExecutionException("Failed to build dependency graph: " + e.getMessage(), e);
        }

        int total = artifacts.size();
        int verified = 0;
        int failed = 0;

        for (Artifact artifact : artifacts) {
            File jarFile = ArtifactUtils.getArtifactFile(artifact);
            if (jarFile == null || !jarFile.exists()) {
                getLog().warn("Skipping artifact with no resolved file: " + artifact);
                continue;
            }

            boolean ok = verifyJar(jarFile);
            if (ok) {
                verified++;
            } else {
                failed++;
                getLog().warn("Verification failed for: " + jarFile.getAbsolutePath());
            }
        }

        getLog().info(verified + "/" + total + " dependencies verified from Chainguard Libraries");

        if (failed > 0) {
            String message = failed + " dependency(ies) could not be verified by Chainguard Libraries.";
            if (failOnUnverified) {
                throw new MojoFailureException(message);
            } else {
                getLog().warn(message);
            }
        }
    }

    private Set<Artifact> collectArtifacts(DependencyNode node, Set<String> scopesToSkip) {
        Set<Artifact> result = new HashSet<Artifact>();
        collectArtifactsRecursive(node, scopesToSkip, result, true);
        return result;
    }

    private void collectArtifactsRecursive(DependencyNode node, Set<String> scopesToSkip,
                                            Set<Artifact> result, boolean isRoot) {
        if (!isRoot) {
            Artifact artifact = node.getArtifact();
            String scope = artifact.getScope();
            if (scope == null || !scopesToSkip.contains(scope)) {
                result.add(artifact);
            }
        }
        if (node.getChildren() != null) {
            for (DependencyNode child : node.getChildren()) {
                collectArtifactsRecursive(child, scopesToSkip, result, false);
            }
        }
    }

    /**
     * Runs 'chainctl libraries verify &lt;path&gt;' and returns true if exit code is 0.
     */
    private boolean verifyJar(File jarFile) throws MojoExecutionException {
        ProcessBuilder pb = new ProcessBuilder(chainctlPath, "libraries", "verify",
                jarFile.getAbsolutePath());
        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    getLog().debug("[chainctl] " + line);
                }
            }
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (IOException e) {
            throw new MojoExecutionException(
                    "Failed to execute chainctl at '" + chainctlPath + "': " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MojoExecutionException("chainctl execution was interrupted", e);
        }
    }
}
