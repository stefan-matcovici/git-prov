package ro.uaic.info.gitprov.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

/**
 * The Application config.
 */
@Configuration
@PropertySource("classpath:application.properties")
public class ApplicationConfig {
    /**
     * The Environment
     */
    @Autowired
    Environment env;

    /**
     * Github token string.
     *
     * @return the string
     */
    @Bean
    public String githubToken() {
        return env.getProperty("github.token");
    }
}
