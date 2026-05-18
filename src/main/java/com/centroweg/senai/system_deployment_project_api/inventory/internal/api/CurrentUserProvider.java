package com.centroweg.senai.system_deployment_project_api.inventory.internal.api;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Resolve o ID do usuário autenticado a partir do principal.
 * Em dev/local o username do {@link org.springframework.security.core.userdetails.User}
 * deve ser o UUID do usuário (substituído pelo JWT do módulo Identity quando disponível).
 */
@Component
public class CurrentUserProvider {

    public UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Usuário não autenticado");
        }
        return UUID.fromString(authentication.getName());
    }
}
