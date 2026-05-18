package com.centroweg.senai.system_deployment_project_api.config;

import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuração temporária para desenvolvimento e testes até o módulo Identity entregar JWT.
 * Usuários de exemplo usam o UUID como username (ver {@code CurrentUserProvider}).
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    public static final UUID DEV_ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID DEV_ALMOXARIFE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    public static final UUID DEV_COLABORADOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    @Profile({"local", "test", "default"})
    UserDetailsService devUserDetailsService(PasswordEncoder passwordEncoder) {
        return new InMemoryUserDetailsManager(
                User.withUsername(DEV_ADMIN_ID.toString())
                        .password(passwordEncoder.encode("admin"))
                        .roles("ADMIN")
                        .build(),
                User.withUsername(DEV_ALMOXARIFE_ID.toString())
                        .password(passwordEncoder.encode("almoxarife"))
                        .roles("ALMOXARIFE")
                        .build(),
                User.withUsername(DEV_COLABORADOR_ID.toString())
                        .password(passwordEncoder.encode("colaborador"))
                        .roles("COLABORADOR")
                        .build());
    }
}
