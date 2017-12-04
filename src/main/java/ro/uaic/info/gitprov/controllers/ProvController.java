package ro.uaic.info.gitprov.controllers;

import org.apache.log4j.Logger;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.SearchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import ro.uaic.info.gitprov.services.GithubService;

import java.io.IOException;

@Controller
public class ProvController {
    @Autowired
    GithubService githubService;

    /**
     * The constant logger.
     */
    final static Logger logger = Logger.getLogger(GithubService.class);


    @RequestMapping(value = "/",method = RequestMethod.GET)
    @ResponseBody
    String getRepositoryByQueryString(@RequestParam(value="name", required=true) String repositoryName) throws IOException{
        for (SearchRepository repository:githubService.getRepositoryByQueryString(repositoryName)){
            logger.info(repository);
        }
        return "test";
    }

    @RequestMapping(value = "/{user}/{name}",method = RequestMethod.GET)
    @ResponseBody
    ResponseEntity<?> getRepositoryByUserAndName(@PathVariable String user, @PathVariable String name) throws IOException {
        Repository repository = githubService.getRepositoryByUserAndName(user, name);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
