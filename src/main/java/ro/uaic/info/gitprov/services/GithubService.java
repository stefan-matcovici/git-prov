package ro.uaic.info.gitprov.services;

import org.apache.log4j.Logger;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.SearchRepository;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
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
     * The repository service that is used to retrieve repositories
     */
    @Autowired
    private RepositoryService repositoryService;

    /**
     * Instantiates a new Github service.
     */
    public GithubService() {
    }

    /**
     * Gets a the repository identified by user that created it and its name
     *
     * @param owner          the owner
     * @param repositoryName the searched repository name
     * @return the repository identified by user and repositoryName
     * @throws IOException exception
     */
    public Repository getRepositoryByOwnerAndName(String owner, String repositoryName) throws IOException {
        return repositoryService.getRepository(owner, repositoryName);
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

    /**
     * Gets all repositories by organization.
     *
     * @param organization the organization
     * @return all repositories by organization
     * @throws IOException the io exception
     */
    public Collection<Repository> getAllRepositoriesByOrganization(String organization) throws IOException {
        return repositoryService.getOrgRepositories(organization);
    }

}
