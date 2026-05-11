package com.centroweg.senai.system_deployment_project_api;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class ArchitectureTest {

    ApplicationModules modules = ApplicationModules.of(SystemDeploymentProjectApiApplication.class);

    @Test
    void verifyModularStructure() {
        modules.verify();
    }

    @Test
    void writeDocumentationAsPlantUml() {
        new Documenter(modules).writeModulesAsPlantUml();
    }
}
