package ro.uaic.info.gitprov.controllers;

import org.apache.log4j.Logger;
import org.eclipse.egit.github.core.Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import ro.uaic.info.gitprov.services.GithubService;
import ro.uaic.info.gitprov.services.ProvenanceService;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static ro.uaic.info.gitprov.utils.ControllerUtils.getProvControllerProvenanceNamespace;

/**
 * The Prov controller.
 */
@Controller
@RequestMapping(value = "/repos")
public class ProvController {
    @Autowired
    private GithubService githubService;

    @Autowired
    private ProvenanceService provenanceService;

    /**
     * The constant logger.
     */
    final static Logger logger = Logger.getLogger(GithubService.class);


    /**
     * Gets repository by query string.
     *
     * @param request the request
     * @return the repository by query string
     * @throws IOException the io exception
     */
    @RequestMapping(value = "/search", method = RequestMethod.GET)
    @ResponseBody
    HttpEntity<List<String>> getRepositoryByQueryString(HttpServletRequest request) throws IOException {
        Map<String, String[]> requestParameters = request.getParameterMap();
        List<String> result = new ArrayList<>();

        if (requestParameters.size() == 0) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } else {
            githubService.getRepositoryByQueryParameters(requestParameters).forEach(repository -> result.add(getProvControllerProvenanceNamespace(repository.getOwner(), repository.getName())));
        }

        return new ResponseEntity<>(result, HttpStatus.MULTIPLE_CHOICES);
    }

    /**
     * Gets repository by user and name.
     *
     * @param owner the owner
     * @param name  the name
     * @return the repository by user and name
     * @throws IOException the io exception
     */
    @RequestMapping(value = "/owner/{owner}/{name}", method = RequestMethod.GET, produces = {"text/provenance-notation", "application/x-turtle", "application/xml", "application/rdf+xml", "application/pdf", "application/json", "application/msword", "image/svg+xml", "image/png", "image/jpeg", "application/trig"})
    @ResponseBody
    HttpEntity<?> getRepositoryByUserAndName(HttpServletRequest request, @PathVariable String owner, @PathVariable String name) throws IOException {
        Repository repository = githubService.getRepositoryByOwnerAndName(owner, name);
        String contentType = request.getHeader("Accept");
        String result = provenanceService.repositoryToDocument(repository, getProvControllerProvenanceNamespace(owner, name), contentType);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    /**
     * Gets all repositories by organization.
     *
     * @param organization the organization
     * @return all repositories owned by the organization
     * @throws IOException the io exception
     */
    @RequestMapping(value = "/organizations/{organization}", method = RequestMethod.GET)
    @ResponseBody
    HttpEntity<List<String>> getAllRepositoriesByOrganization(@PathVariable String organization) throws IOException {
        List<String> result = new ArrayList<>();

        githubService.getAllRepositoriesByOrganization(organization).forEach(repository -> result.add(getProvControllerProvenanceNamespace(organization, repository.getName())));

        return new ResponseEntity<>(result, HttpStatus.MULTIPLE_CHOICES);
    }

    /**
     * Gets all repositories by user.
     *
     * @param user the user
     * @return the all repositories by user
     * @throws IOException the io exception
     */
    @RequestMapping(value = "/users/{user}", method = RequestMethod.GET)
    @ResponseBody
    HttpEntity<List<String>> getAllRepositoriesByUser(@PathVariable String user) throws IOException {
        List<String> result = new ArrayList<>();

        githubService.getAllRepositoriesByUser(user).forEach(repository -> result.add(getProvControllerProvenanceNamespace(user, repository.getName())));

        return new ResponseEntity<>(result, HttpStatus.MULTIPLE_CHOICES);
    }


}
