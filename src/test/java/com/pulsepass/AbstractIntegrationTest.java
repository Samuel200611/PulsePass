package com.pulsepass;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base de todas las pruebas de integracion.
 *
 * NFR-004 / NFR-005: las pruebas levantan PostgreSQL real en un contenedor
 * Docker, sin depender de una instalacion manual y sin usar H2.
 *
 * @ServiceConnection conecta automaticamente el DataSource de Spring Boot
 * al contenedor (url, usuario y password), sin configurar propiedades a mano.
 *
 * El contenedor es estatico y se comparte entre las clases que heredan de
 * esta, de modo que solo se arranca una vez por ejecucion de la suite.
 */
@SpringBootTest
@Testcontainers
public abstract class AbstractIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:18-alpine")
                    .withDatabaseName("pulsepass_test")
                    .withUsername("pulsepass")
                    .withPassword("pulsepass");
}
