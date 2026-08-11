package org.dromara.easyes.boot3;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.dromara.easyes.common.property.EasyEsProperties;
import org.dromara.easyes.common.utils.EsClientUtils;
import org.dromara.easyes.spring.annotation.EsMapperScan;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class EasyEsBoot3CompatibilityTest {

    @Test
    void shouldLoadEasyEsAutoConfigurationOnSpringBoot3() {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(TestApplication.class)
                .properties(
                        "easy-es.address=127.0.0.1:9200",
                        "easy-es.banner=false"
                )
                .run()) {
            assertThat(context.getBean(EasyEsProperties.class)).isNotNull();
            ElasticsearchClient client = context.getBean(ElasticsearchClient.class);
            assertThat(client._transport()).isInstanceOf(RestClientTransport.class);
            assertThat(EsClientUtils.class.getProtectionDomain().getCodeSource().getLocation().toString())
                    .contains("easy-es-common-3.2.0.jar");
        }
    }

    @Test
    void shouldPerformCrudAgainstLocalElasticsearchWithEsClient8() {
        String password = System.getenv("EASY_ES_TEST_PASSWORD");
        assumeTrue(password != null && !password.isBlank(),
                "EASY_ES_TEST_PASSWORD is required for the local integration test");
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(TestApplication.class)
                .properties(
                        "easy-es.address=127.0.0.1:9200",
                        "easy-es.username=elastic",
                        "easy-es.password=" + password,
                        "easy-es.banner=false",
                        "easy-es.global-config.process-index-mode=manual"
                )
                .run()) {
            Boot3DocumentMapper mapper = context.getBean(Boot3DocumentMapper.class);
            if (mapper.existsIndex(Boot3Document.INDEX_NAME)) {
                mapper.deleteIndex(Boot3Document.INDEX_NAME);
            }
            try {
                assertThat(mapper.createIndex()).isTrue();
                Boot3Document document = new Boot3Document()
                        .setId("boot3-1")
                        .setTitle("boot3-es8-integration");
                assertThat(mapper.insert(document)).isEqualTo(1);
                Boot3Document stored = mapper.selectById(document.getId());
                assertThat(stored).isNotNull();
                assertThat(stored.getTitle()).isEqualTo(document.getTitle());
                assertThat(mapper.deleteById(document.getId())).isEqualTo(1);
            } finally {
                if (mapper.existsIndex(Boot3Document.INDEX_NAME)) {
                    mapper.deleteIndex(Boot3Document.INDEX_NAME);
                }
            }
        }
    }

    @SpringBootApplication
    @EsMapperScan("org.dromara.easyes.boot3")
    static class TestApplication {
    }
}
