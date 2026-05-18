package com.centroweg.senai.system_deployment_project_api.inventory;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.centroweg.senai.system_deployment_project_api.config.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
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
class StockEntryControllerTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    private String partId;

    @BeforeEach
    void createPart() throws Exception {
        MvcResult result = mockMvc.perform(post("/parts")
                        .with(httpBasic(SecurityConfig.DEV_ADMIN_ID.toString(), "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "code": "ENT-001",
                                  "name": "Rolamento",
                                  "unit": "un",
                                  "qtyMinimum": 5
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        partId = body.substring(body.indexOf("\"id\":\"") + 6, body.indexOf("\"", body.indexOf("\"id\":\"") + 6));
    }

    @Test
    void shouldRegisterStockEntryAndIncrementStock() throws Exception {
        mockMvc.perform(post("/parts/" + partId + "/stock-entries")
                        .with(httpBasic(SecurityConfig.DEV_ALMOXARIFE_ID.toString(), "almoxarife"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "quantity": 50,
                                  "note": "Compra fornecedor X"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantity").value(50));

        mockMvc.perform(get("/parts/" + partId + "/stock-entries")
                        .with(httpBasic(SecurityConfig.DEV_ALMOXARIFE_ID.toString(), "almoxarife")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(get("/parts/" + partId)
                        .with(httpBasic(SecurityConfig.DEV_COLABORADOR_ID.toString(), "colaborador")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.qtyInStock").value(50));
    }
}
