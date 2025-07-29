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

  
    @Bean
    public ConnectionProvider connectionProvider() {
        return ConnectionProvider.builder("rinha-pool")
            .maxConnections(200)                     
            .pendingAcquireMaxCount(500)             
            .pendingAcquireTimeout(Duration.ofSeconds(60))
            .maxIdleTime(Duration.ofSeconds(30))
            .maxLifeTime(Duration.ofMinutes(5))
            .build();
    }

 
    @Bean
    public HttpClient reactorHttpClient(ConnectionProvider provider) {
        return HttpClient.create(provider)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5_000)   
            .responseTimeout(Duration.ofSeconds(10))              
            .doOnConnected(conn ->
                conn.addHandlerLast(new ReadTimeoutHandler(10))
                    .addHandlerLast(new WriteTimeoutHandler(10)))
            .protocol(HttpProtocol.H2, HttpProtocol.HTTP11)        
            .compress(true);                                  
    }


    @Bean
    public WebClient.Builder webClientBuilder(HttpClient reactorHttpClient) {

        ExchangeStrategies strategies = ExchangeStrategies.builder()
            .codecs(config -> config.defaultCodecs().maxInMemorySize(2 * 1024 * 1024)) 
            .build();

        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(reactorHttpClient))
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.ACCEPT,      MediaType.APPLICATION_JSON_VALUE)
            .exchangeStrategies(strategies);
    }
}
