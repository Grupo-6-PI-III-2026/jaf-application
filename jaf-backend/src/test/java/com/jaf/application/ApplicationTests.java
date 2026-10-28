package com.jaf.application;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:context;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa", "spring.datasource.password=",
		"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop",
		"app.seed.enabled=false", "spring.rabbitmq.listener.simple.auto-startup=false"
})
class ApplicationTests {

	@org.springframework.test.context.bean.override.mockito.MockitoBean
	com.jaf.application.config.GerenciadorTokenJwt tokenJwt;

	@Test
	void contextLoads() {
	}

}
