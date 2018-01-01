package ro.uaic.info.gitprov.config;

import org.apache.log4j.Logger;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.CommitService;
import org.eclipse.egit.github.core.service.ContentsService;
import org.eclipse.egit.github.core.service.DataService;
import org.eclipse.egit.github.core.service.RepositoryService;
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

    final static Logger logger = Logger.getLogger(ApplicationConfig.class);

    private GitHubClient gitHubClient;

    @Autowired
    public ApplicationConfig(Environment environment) {
        gitHubClient = new GitHubClient();
        gitHubClient.setOAuth2Token(environment.getProperty("github.token"));
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
}
