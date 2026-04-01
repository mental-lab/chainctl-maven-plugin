package com.chainctl;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class VerifyDependenciesMojoTest {

    @Test
    public void mojoClassIsInstantiable() {
        VerifyDependenciesMojo mojo = new VerifyDependenciesMojo();
        assertNotNull(mojo);
    }
}
