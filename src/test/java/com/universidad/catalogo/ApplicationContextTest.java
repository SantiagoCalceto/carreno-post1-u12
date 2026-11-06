package com.universidad.catalogo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de contexto: Spring construye TODOS los beans de la aplicacion con el
 * perfil dev (H2 en memoria), sin necesitar una base de datos externa. Es lo
 * mismo que hara el contenedor del smoke test cuando arranque sin variables.
 */
@SpringBootTest
@ActiveProfiles("dev")
class ApplicationContextTest {

    @Autowired Environment env;

    @Test
    void contextLoads_conPerfilDev_usaH2EnMemoria() {
        assertThat(env.getActiveProfiles()).containsExactly("dev");
        assertThat(env.getProperty("spring.datasource.url")).startsWith("jdbc:h2:mem:");
    }
}
