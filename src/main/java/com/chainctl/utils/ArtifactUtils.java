package com.chainctl.utils;

import org.apache.maven.artifact.Artifact;

import java.io.File;

public final class ArtifactUtils {

    private ArtifactUtils() {}

    /**
     * Resolves the local repository file path for the given artifact.
     *
     * @param artifact the Maven artifact
     * @return the local file, or {@code null} if the artifact has no associated file
     */
    public static File getArtifactFile(Artifact artifact) {
        if (artifact.getFile() != null) {
            return artifact.getFile();
        }
        return null;
    }
}
