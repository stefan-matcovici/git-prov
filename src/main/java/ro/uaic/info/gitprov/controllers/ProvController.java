package ro.uaic.info.gitprov.controllers;

import org.apache.log4j.Logger;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.SearchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
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
            for (SearchRepository repository : githubService.getRepositoryByQueryParameters(requestParameters)) {
                ControllerLinkBuilder builder = ControllerLinkBuilder.linkTo(ProvController.class).slash("owner").slash(repository.getOwner()).slash(repository.getName());
                result.add(builder.toString());
            }
        }

        return new ResponseEntity<>(result, HttpStatus.MULTIPLE_CHOICES);
    }

    /**
     * Gets repository by user and name.
     *
     * @param owner the owner
     * @param name the name
     * @return the repository by user and name
     * @throws IOException the io exception
     */
    @RequestMapping(value = "/owner/{owner}/{name}", method = RequestMethod.GET)
    @ResponseBody
    HttpEntity<?> getRepositoryByUserAndName(@PathVariable String owner, @PathVariable String name) throws IOException {
        Repository repository = githubService.getRepositoryByOwnerAndName(owner, name);
        String result = provenanceService.repositoryToDocument(repository, ControllerLinkBuilder.linkTo(ProvController.class).slash("owner").slash(owner).slash(name).toString() + "#");
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
        for (Repository repository : githubService.getAllRepositoriesByOrganization(organization)) {
            ControllerLinkBuilder builder = ControllerLinkBuilder.linkTo(ProvController.class).slash("owner").slash(organization).slash(repository.getName());
            result.add(builder.toString());
        }

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
        for (Repository repository : githubService.getAllRepositoriesByUser(user)) {
            ControllerLinkBuilder builder = ControllerLinkBuilder.linkTo(ProvController.class).slash("owner").slash(repository.getOwner().getLogin()).slash(repository.getName());
            result.add(builder.toString());
        }

        return new ResponseEntity<>(result, HttpStatus.MULTIPLE_CHOICES);
    }
}
