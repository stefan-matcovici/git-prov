package ro.uaic.info.gitprov.controllers;

import org.eclipse.egit.github.core.Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.uaic.info.gitprov.services.GithubService;
import ro.uaic.info.gitprov.services.ProvenanceService;
import ro.uaic.info.gitprov.services.StoreService;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static ro.uaic.info.gitprov.utils.ControllerUtils.getProvControllerProvenanceNamespace;
import static ro.uaic.info.gitprov.utils.ControllerUtils.getStoreControllerProvenanceNamespace;

@RestController
@RequestMapping(value = "/store")
public class StoreController {

    @Autowired
    GithubService githubService;

    @Autowired
    StoreService storeService;

    @Autowired
    ProvenanceService provenanceService;


    @RequestMapping(value = "/owner/{owner}/{name}", method = RequestMethod.GET, produces = {"text/csv", "application/json", "application/rdf+xml", "application/x-turtle", "application/n-triples", "application/ld+json"})
    @ResponseBody
    HttpEntity<?> getRepositoryByUserAndName(HttpServletRequest request, @PathVariable String owner, @PathVariable String name) throws IOException {
        Repository repository = githubService.getRepositoryByOwnerAndName(owner, name);
        String contentType = request.getHeader("Accept");
        String result = storeService.getDocument(owner + "/" + name, contentType);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @RequestMapping(value = "/owner/{owner}/{name}", method = RequestMethod.POST)
    @ResponseBody
    HttpEntity<?> storeRepositoryByUserAndName(HttpServletRequest request, @PathVariable String owner, @PathVariable String name) throws IOException {
        Repository repository = githubService.getRepositoryByOwnerAndName(owner, name);
        storeService.storeDocument(owner + "/" + name, provenanceService.repositoryToDocument(repository, getProvControllerProvenanceNamespace(owner, name), "application/x-turtle"));
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(value = "/repos", method = RequestMethod.GET)
    @ResponseBody
    HttpEntity<?> getStoredRepos() {
        List<String> result = new ArrayList<>();
        storeService.getStoredRepositories().forEach(parsedString -> result.add(getStoreControllerProvenanceNamespace(parsedString[0], parsedString[1])));
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}
