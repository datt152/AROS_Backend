package vn.edu.aros.aroscore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import vn.edu.aros.aroscore.config.OmrProperties;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder(OmrProperties omrProperties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(omrProperties.getEngine().getConnectTimeoutMs());
        factory.setReadTimeout(omrProperties.getEngine().getReadTimeoutMs());
        return RestClient.builder().requestFactory(factory);
    }
}
