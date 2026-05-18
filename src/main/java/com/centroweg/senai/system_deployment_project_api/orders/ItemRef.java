package com.centroweg.senai.system_deployment_project_api.orders;

import java.util.UUID;

public record ItemRef(UUID partId, int quantity) {}
