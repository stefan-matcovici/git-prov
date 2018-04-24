package ro.uaic.info.gitprov.controllers;

import org.eclipse.egit.github.core.Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.uaic.info.gitprov.services.GithubService;
import ro.uaic.info.gitprov.services.ProvenanceService;
import ro.uaic.info.gitprov.services.SparqlService;
import ro.uaic.info.gitprov.services.StoreService;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@RestController
@RequestMapping(value = "/sparql")
public class SparqlController {

    @Autowired
    private SparqlService sparqlService;

    @Autowired
    private GithubService githubService;

    @Autowired
    private ProvenanceService provenanceService;

    @Autowired
    private StoreService storeService;

    @RequestMapping(value = "/owner/{owner}/{name}", method = RequestMethod.POST, produces = {"text/plain", "application/xml", "text/csv", "application/json", "text/tab-separated-values", "application/sparql-results+xml", "text/rdf+n3", "application/x-turtle", "application/n-triples"})
    @ResponseBody
    HttpEntity<?> executeQuery(HttpServletRequest request, @PathVariable String owner, @PathVariable String name, @RequestBody String query) throws IOException {
        Repository repository = githubService.getRepositoryByOwnerAndName(owner, name);
//        String result = provenanceService.repositoryToDocument(repository, getProvControllerProvenanceNamespace(owner, name), "application/x-turtle");
        String result = storeService.getDocument(owner + "/" + name, "application/x-turtle");

        String contentType = request.getHeader("Accept");
        return new ResponseEntity<>(sparqlService.getQueryResult(result, query, contentType), HttpStatus.OK);

    }
}
