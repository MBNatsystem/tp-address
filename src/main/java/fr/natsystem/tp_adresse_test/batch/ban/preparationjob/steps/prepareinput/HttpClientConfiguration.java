package fr.natsystem.tp_adresse_test.batch.ban.preparationjob.steps.prepareinput;

import java.net.http.HttpClient;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HttpClientConfiguration {
    
    @Bean
    HttpClient httpClient(){
        return HttpClient.newHttpClient();
    }
}
