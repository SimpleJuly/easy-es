package org.dromara.easyes.common.utils;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import co.elastic.clients.transport.rest5_client.low_level.Rest5ClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpRequestInterceptor;
import org.apache.hc.core5.http.HttpResponseInterceptor;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.dromara.easyes.common.enums.SchemaEnum;
import org.dromara.easyes.common.property.EasyEsProperties;

import javax.net.ssl.SSLContext;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static org.dromara.easyes.common.constants.BaseEsConstants.*;

/**
 * @author lyy
 */
public class EsClientUtils {

    public static final String DEFAULT_DS = "DEFAULT_DS";

    private final static Map<String, ElasticsearchClient> restHighLevelClientMap = new ConcurrentHashMap<>();

    public EsClientUtils() {
    }

    public static ElasticsearchClient getElasticsearchClient(String restHighLevelClientId) {
        if (DEFAULT_DS.equals(restHighLevelClientId)) {
            return restHighLevelClientMap.values()
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> ExceptionUtils.eee("Could not found ElasticsearchClient,restHighLevelClientId:%s", restHighLevelClientId));
        }
        ElasticsearchClient client = restHighLevelClientMap.get(restHighLevelClientId);
        if (client == null) {
            LogUtils.formatError("restHighLevelClientId: %s can not find any data source, please check your config", restHighLevelClientId);
            throw ExceptionUtils.eee("Cloud not found ElasticsearchClient,restHighLevelClientId:%s", restHighLevelClientId);
        }
        return client;
    }

    public static void registerClient(String restHighLevelClientId, Supplier<ElasticsearchClient> restHighLevelClient) {
        EsClientUtils.restHighLevelClientMap.putIfAbsent(restHighLevelClientId, restHighLevelClient.get());
    }

    public static ElasticsearchClient buildClient(EasyEsProperties easyEsConfigProperties, ObjectMapper objectMapper,
                                                  EasyEsHeadersCustomizer headersCustomizer) {
        // 处理地址
        String address = easyEsConfigProperties.getAddress();
        if (StringUtils.isEmpty(address)) {
            throw ExceptionUtils.eee("please config the es address");
        }
        if (!address.contains(COLON)) {
            throw ExceptionUtils.eee("the address must contains port and separate by ':'");
        }
        String schema = StringUtils.isEmpty(easyEsConfigProperties.getSchema())
                ? DEFAULT_SCHEMA : easyEsConfigProperties.getSchema();
        List<HttpHost> hostList = new ArrayList<>();
        Arrays.stream(easyEsConfigProperties.getAddress().split(COMMA))
                .forEach(item -> hostList.add(new HttpHost(schema, item.split(COLON)[0],
                        Integer.parseInt(item.split(COLON)[1]))));

        // 转换成 HttpHost 数组
        HttpHost[] httpHost = hostList.toArray(new HttpHost[]{});

        // 构建连接对象
        Rest5ClientBuilder builder = Rest5Client.builder(httpHost);

        boolean https = SchemaEnum.https.name().equals(schema);
        SSLContext sslContext = https ? buildTrustAllSslContext() : null;
        Optional.ofNullable(sslContext).ifPresent(builder::setSSLContext);

        // 连接池配置: 最大连接数、最大连接路由在HttpClient5中由连接管理器负责
        builder.setConnectionManagerCallback(connectionManagerBuilder -> {
            Optional.ofNullable(easyEsConfigProperties.getMaxConnTotal())
                    .ifPresent(connectionManagerBuilder::setMaxConnTotal);
            Optional.ofNullable(easyEsConfigProperties.getMaxConnPerRoute())
                    .ifPresent(connectionManagerBuilder::setMaxConnPerRoute);
            // https场景下信任所有证书并跳过主机名校验
            if (sslContext != null) {
                connectionManagerBuilder.setTlsStrategy(ClientTlsStrategyBuilder.create()
                        .setSslContext(sslContext)
                        .setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                        .build());
            }
        });

        builder.setHttpClientConfigCallback(httpClientBuilder -> {
            // 设置心跳时间
            Optional.ofNullable(easyEsConfigProperties.getKeepAliveMillis())
                    .ifPresent(p -> httpClientBuilder.setKeepAliveStrategy(
                            (response, context) -> TimeValue.ofMilliseconds(p)));

            // 设置账号密码
            String username = easyEsConfigProperties.getUsername();
            String password = easyEsConfigProperties.getPassword();
            if (StringUtils.isNotEmpty(username) && StringUtils.isNotEmpty(password)) {
                BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(new AuthScope(null, null, -1, null, null),
                        new UsernamePasswordCredentials(username, password.toCharArray()));
                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
            }

            // 兼容性配置和请求头自定义配置
            buildCompatible(httpClientBuilder, builder, easyEsConfigProperties, headersCustomizer);
        });

        // 设置超时时间之类的
        builder.setRequestConfigCallback(requestConfigBuilder -> {
            Optional.ofNullable(easyEsConfigProperties.getConnectTimeout())
                    .ifPresent(p -> requestConfigBuilder.setConnectTimeout(Timeout.ofMilliseconds(p)));
            Optional.ofNullable(easyEsConfigProperties.getSocketTimeout())
                    .ifPresent(p -> requestConfigBuilder.setResponseTimeout(Timeout.ofMilliseconds(p)));
            Optional.ofNullable(easyEsConfigProperties.getConnectionRequestTimeout())
                    .ifPresent(p -> requestConfigBuilder.setConnectionRequestTimeout(Timeout.ofMilliseconds(p)));
        });

        // 如果是驼峰转下划线, 则增加序列化器
        if (easyEsConfigProperties.getGlobalConfig().getDbConfig().isMapUnderscoreToCamelCase()) {
            objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        }
        return new ElasticsearchClient(new Rest5ClientTransport(builder.build(),
                new JacksonJsonpMapper(objectMapper)));
    }

    /**
     * 构建信任所有证书的SSLContext
     *
     * @return SSLContext
     */
    private static SSLContext buildTrustAllSslContext() {
        try {
            return SSLContextBuilder.create()
                    .loadTrustMaterial(null, (chain, authType) -> true)
                    .build();
        } catch (Exception e) {
            LogUtils.error("elasticsearchClient build SSLContext exception: %s", e.getMessage());
            throw ExceptionUtils.eee(e);
        }
    }

    /**
     * 兼容性配置
     *
     * @param httpClientBuilder httpClientBuilder
     * @param clientBuilder     Rest5Client建造者,默认请求头由其承载
     */
    private static void buildCompatible(org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder httpClientBuilder,
                                        Rest5ClientBuilder clientBuilder,
                                        EasyEsProperties properties, EasyEsHeadersCustomizer headersCustomizer) {
        if (properties.isCompatible()) {
            httpClientBuilder.addRequestInterceptorFirst((HttpRequestInterceptor) (request, entity, context) -> {
                // 这里主动编辑请求头，避免兼容性问题报错
                // 在es8/es9中，默认的请求头 Accept 字段为：
                // Accept: application/vnd.elasticsearch+json; compatible-with=8
                // 访问低版本es集群有可能出现
                // [es/create] failed:
                // [media_type_header_exception] Invalid media-type value
                // on headers [Accept, Content-Type]
                // 通过拦截器编辑请求头以避免此错误！
                request.setHeader("Accept", "application/json");
            });
            clientBuilder.setDefaultHeaders(new Header[]{
                    new BasicHeader("Accept", "application/json"),
                    new BasicHeader("Content-Type", "application/json"),
                    new BasicHeader("Connection", "Keep-Alive"),
                    new BasicHeader("Charset", "UTF-8")
            });
            // 这部分为了避免旧版本406报错
            httpClientBuilder.addResponseInterceptorLast((HttpResponseInterceptor)
                    (response, entity, context) ->
                            response.addHeader("X-Elastic-Product", "Elasticsearch"));
        }
        // 如果自定义了请求头，则添加
        Optional.ofNullable(headersCustomizer).ifPresent(consumer -> httpClientBuilder
                .addRequestInterceptorFirst((HttpRequestInterceptor) (request, entity, context) ->
                        consumer.customizer().forEach(request::addHeader)));
    }

    public ElasticsearchClient getClient(String clientId) {
        return getElasticsearchClient(clientId);
    }
}
