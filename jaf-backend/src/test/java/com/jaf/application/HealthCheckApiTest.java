package com.jaf.application;

import com.jaf.application.config.GerenciadorTokenJwt;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Sem broker acessível: reproduz a AWS sem RabbitMQ, que derrubava o target group do ALB.
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:health;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop", "app.seed.enabled=false",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.rabbitmq.host=127.0.0.1", "spring.rabbitmq.port=1"
})
@AutoConfigureMockMvc
class HealthCheckApiTest {
    @MockitoBean GerenciadorTokenJwt tokenJwt;
    @Autowired MockMvc mvc;

    @Test void readinessDoLoadBalancerIgnoraRabbitIndisponivel() throws Exception {
        mvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
    }

    @Test void healthCompletoContinuaIndicandoBrokerIndisponivel() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isServiceUnavailable());
    }
}
