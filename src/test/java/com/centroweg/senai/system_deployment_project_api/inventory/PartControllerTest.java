package com.centroweg.senai.system_deployment_project_api.inventory;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.centroweg.senai.system_deployment_project_api.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class PartControllerTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldCreateListAndUpdatePart() throws Exception {
        mockMvc.perform(post("/parts")
                        .with(httpBasic(
                                SecurityConfig.DEV_ADMIN_ID.toString(), "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "code": "PAR-001",
                                  "name": "Parafuso M8",
                                  "unit": "un",
                                  "qtyMinimum": 10
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("PAR-001"))
                .andExpect(jsonPath("$.qtyInStock").value(0));

        mockMvc.perform(get("/parts")
                        .with(httpBasic(
                                SecurityConfig.DEV_COLABORADOR_ID.toString(), "colaborador")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        MvcResult created = mockMvc.perform(get("/parts")
                        .with(httpBasic(
                                SecurityConfig.DEV_COLABORADOR_ID.toString(), "colaborador")))
                .andReturn();

        String body = created.getResponse().getContentAsString();
        String id = body.substring(body.indexOf("\"id\":\"") + 6, body.indexOf("\"", body.indexOf("\"id\":\"") + 6));

        mockMvc.perform(put("/parts/" + id)
                        .with(httpBasic(
                                SecurityConfig.DEV_ALMOXARIFE_ID.toString(), "almoxarife"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "name": "Parafuso M8 atualizado",
                                  "unit": "un",
                                  "qtyMinimum": 20,
                                  "active": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Parafuso M8 atualizado"));
    }

    @Test
    void shouldRejectDuplicateCode() throws Exception {
        String payload =
                """
                {
                  "code": "DUP-001",
                  "name": "Peça A",
                  "unit": "un",
                  "qtyMinimum": 0
                }
                """;

        mockMvc.perform(post("/parts")
                        .with(httpBasic(SecurityConfig.DEV_ADMIN_ID.toString(), "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/parts")
                        .with(httpBasic(SecurityConfig.DEV_ADMIN_ID.toString(), "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict());
    }
}
