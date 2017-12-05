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
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.bind.annotation.*;
import ro.uaic.info.gitprov.services.GithubService;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping(value = "/repositories")
public class ProvController {
    @Autowired
    private GithubService githubService;

    /**
     * The constant logger.
     */
    final static Logger logger = Logger.getLogger(GithubService.class);


    @RequestMapping(value = "",method = RequestMethod.GET)
    @ResponseBody
    HttpEntity<List<String>> getRepositoryByQueryString(HttpServletRequest request) throws IOException{
        Map<String, String[]> requestParameters = request.getParameterMap();
        List<String> result = new ArrayList<>();
        for (SearchRepository repository:githubService.getRepositoryByQueryParameters(requestParameters)){
            ControllerLinkBuilder builder = ControllerLinkBuilder.linkTo(ProvController.class).slash(repository.getOwner()).slash(repository.getName());
            result.add(builder.toString());
        }

        return new ResponseEntity<>(result, HttpStatus.MULTIPLE_CHOICES);
    }

    @RequestMapping(value = "/{user}/{name}",method = RequestMethod.GET)
    @ResponseBody
    HttpEntity<?> getRepositoryByUserAndName(@PathVariable String user, @PathVariable String name) throws IOException {
        Repository repository = githubService.getRepositoryByUserAndName(user, name);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(value = "/{user}",method = RequestMethod.GET)
    @ResponseBody
    HttpEntity<List<String>> getAllRepositoriesByUser(@PathVariable String user) throws IOException {
        List<String> result = new ArrayList<>();
        for (Repository repository:githubService.getAllRepositoriesByUser(user)){
            ControllerLinkBuilder builder = ControllerLinkBuilder.linkTo(ProvController.class).slash(repository.getOwner().getLogin()).slash(repository.getName());
            result.add(builder.toString());
        }

        return new ResponseEntity<>(result, HttpStatus.MULTIPLE_CHOICES);
    }
}
