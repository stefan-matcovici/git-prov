package ro.uaic.info.gitprov.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * The Application config.
 */
@Configuration
@PropertySource("classpath:application.properties")
public class ApplicationConfig {

    final static Logger logger = Logger.getLogger(ApplicationConfig.class);

    private GitHubClient gitHubClient;

    @Autowired
    public ApplicationConfig(Environment environment) {
        gitHubClient = new GitHubClient();
        gitHubClient.setOAuth2Token(System.getenv().get("github-token"));
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurerAdapter() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**").allowedOrigins("*");
            }
        };
    }

    @Bean
    public RepositoryService repositoryService() {
        return new RepositoryService(gitHubClient);
    }

    @Bean
    public ContentsService contentsService() {
        return new ContentsService(gitHubClient);
    }

    @Bean
    public DataService dataService() {
        return new DataService(gitHubClient);
    }

    @Bean
    public CommitService commitService() {
        return new CommitService(gitHubClient);
    }

    @Bean
    public UserService userService() {
        return new UserService(gitHubClient);
    }

    @Bean
    String provStoreApiKey() {
        return System.getenv().get("provstore-api-key");
    }

    @Bean
    String provStoreConsumerKey() {
        return System.getenv().get("provstore-consumer-key");
    }

    @Bean
    String provStoreConsumerSecret() {
        return System.getenv().get("provstore-consumer-secret");
    }

    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
