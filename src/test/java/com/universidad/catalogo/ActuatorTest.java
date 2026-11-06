package com.universidad.catalogo;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Exposicion de Actuator diferenciada por perfil. */
class ActuatorTest {

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @ActiveProfiles("dev")
    class PerfilDev {
        @Autowired MockMvc mvc;

        @Test
        void health_respondeUpConDetalles() throws Exception {
            mvc.perform(get("/actuator/health"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.status").value("UP"))
               .andExpect(jsonPath("$.components.db.status").value("UP"));
        }

        @Test
        void beans_estaExpuestoEnDev() throws Exception {
            mvc.perform(get("/actuator/beans")).andExpect(status().isOk());
        }
    }

    // Perfil prod con H2 en vez de PostgreSQL: lo que se prueba aqui es la exposicion
    // de endpoints, no la base de datos (PostgreSQL real se prueba en el pipeline).
    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @ActiveProfiles("prod")
    @TestPropertySource(properties = {
            "spring.datasource.url=jdbc:h2:mem:prodtest;DB_CLOSE_DELAY=-1",
            "spring.datasource.username=sa",
            "spring.jpa.hibernate.ddl-auto=create-drop"})
    class PerfilProd {
        @Autowired MockMvc mvc;

        @Test
        void health_respondeUpSinDetalles() throws Exception {
            mvc.perform(get("/actuator/health"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.status").value("UP"))
               .andExpect(jsonPath("$.components").doesNotExist());
        }

        @Test
        void info_estaExpuesto() throws Exception {
            mvc.perform(get("/actuator/info"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.app.nombre").value("Catalogo de Productos"));
        }

        @Test
        void beans_noEstaExpuestoEnProd_responde404() throws Exception {
            mvc.perform(get("/actuator/beans")).andExpect(status().isNotFound());
        }

        @Test
        void env_noEstaExpuestoEnProd_responde404() throws Exception {
            mvc.perform(get("/actuator/env")).andExpect(status().isNotFound());
        }
    }
}
