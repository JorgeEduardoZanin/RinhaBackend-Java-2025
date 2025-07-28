package jorge.rinha.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.http.HttpProtocol;

import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    /**
     * Pool de conexões compartilhado por todos os WebClients.
     */
    @Bean
    public ConnectionProvider connectionProvider() {
        return ConnectionProvider.builder("rinha-pool")
            .maxConnections(200)                      // conexões simultâneas
            .pendingAcquireMaxCount(500)              // requisições em espera
            .pendingAcquireTimeout(Duration.ofSeconds(60))
            .maxIdleTime(Duration.ofSeconds(30))
            .maxLifeTime(Duration.ofMinutes(5))
            .build();
    }

    /**
     * HttpClient customizado para timeouts, compressão e HTTP/2.
     */
    @Bean
    public HttpClient reactorHttpClient(ConnectionProvider provider) {
        return HttpClient.create(provider)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5_000)   // timeout TCP
            .responseTimeout(Duration.ofSeconds(10))               // timeout de resposta
            .doOnConnected(conn ->
                conn.addHandlerLast(new ReadTimeoutHandler(10))
                    .addHandlerLast(new WriteTimeoutHandler(10)))
            .protocol(HttpProtocol.H2, HttpProtocol.HTTP11)        // prioriza HTTP/2
            .compress(true);                                       // gzip, deflate etc.
    }

    /**
     * Builder que reaproveita o mesmo ReactorHttpClient.
     */
    @Bean
    public WebClient.Builder webClientBuilder(HttpClient reactorHttpClient) {
        // reduz uso de memória para buffering de corpos grandes (opcional)
        ExchangeStrategies strategies = ExchangeStrategies.builder()
            .codecs(config -> config.defaultCodecs().maxInMemorySize(2 * 1024 * 1024)) // 2 MiB
            .build();

        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(reactorHttpClient))
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.ACCEPT,      MediaType.APPLICATION_JSON_VALUE)
            .exchangeStrategies(strategies);
    }
}
