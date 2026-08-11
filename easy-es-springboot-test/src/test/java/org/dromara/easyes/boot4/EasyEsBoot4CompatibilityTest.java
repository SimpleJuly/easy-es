package org.dromara.easyes.boot4;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import org.dromara.easyes.common.property.EasyEsProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class EasyEsBoot4CompatibilityTest {

    @Test
    void shouldLoadEasyEsAutoConfigurationWithEsClient9OnSpringBoot4() {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(TestApplication.class)
                .properties(
                        "easy-es.address=127.0.0.1:9200",
                        "easy-es.banner=false"
                )
                .run()) {
            assertNotNull(context.getBean(EasyEsProperties.class));
            ElasticsearchClient client = context.getBean(ElasticsearchClient.class);
            assertInstanceOf(Rest5ClientTransport.class, client._transport());
        }
    }

    @SpringBootApplication
    static class TestApplication {
    }
}
