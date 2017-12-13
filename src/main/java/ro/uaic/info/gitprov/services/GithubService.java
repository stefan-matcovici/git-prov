package ro.uaic.info.gitprov.services;

import org.apache.log4j.Logger;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.SearchRepository;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Service that gets a repository or a list of repositories based on different ways of querying the Github API
 *
 * @author Matcovici Stefan
 * @since 2017 -11-29
 */
@Service
public class GithubService {

    /**
     * The constant logger.
     */
    final static Logger logger = Logger.getLogger(GithubService.class);

    /**
     * The github client that authenticates the application on Github
     */
    private GitHubClient client;

    /**
     * The repository service that it's used to retrieve repositories
     */
    private RepositoryService repositoryService;

    @Autowired
    private String githubToken;

    /**
     * Instantiates a new Github service. Sets up the github client with token from properties file and the repository
     * service that will be used to fetch data about repositories
     */
    public GithubService() {
        client = new GitHubClient();
    }

    @PostConstruct
    public void init() {
        client.setOAuth2Token(githubToken);
        repositoryService = new RepositoryService(client);
    }

    /**
     * Gets a the repository identified by user that created it and its name
     *
     * @param user           the user
     * @param repositoryName the searched repository name
     * @return the repository identified by user and repositoryName
     * @throws IOException exception
     */
    public Repository getRepositoryByUserAndName(String user, String repositoryName) throws IOException {
        return repositoryService.getRepository(user, repositoryName);
    }

    /**
     * Gets a list of repositories which match the query params
     *
     * @param queryParameters the parameters by which to filter
     * @return the repository by query string
     * @throws IOException exception
     */
    public Collection<SearchRepository> getRepositoryByQueryParameters(Map<String, String[]> queryParameters) throws IOException {
        Map<String, String> transformedMap = new HashMap<>();
        for (Map.Entry<String, String[]> entry : queryParameters.entrySet()) {
            for (String value : entry.getValue()) {
                transformedMap.put(entry.getKey(), value);
            }
        }

        return repositoryService.searchRepositories(transformedMap);
    }


    /**
     * Gets all repositories for a specified user.
     *
     * @param user the user
     * @return the all repositories by user
     * @throws IOException exception
     */
    public Collection<Repository> getAllRepositoriesByUser(String user) throws IOException {
        return repositoryService.getRepositories(user);
    }

    public Collection<Repository> getAllRepositoriesByOrganization(String organization) throws IOException {
        return repositoryService.getOrgRepositories(organization);
    }

}
