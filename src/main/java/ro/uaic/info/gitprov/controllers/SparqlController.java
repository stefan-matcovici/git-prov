package ro.uaic.info.gitprov.controllers;

import org.eclipse.egit.github.core.Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.uaic.info.gitprov.services.GithubService;
import ro.uaic.info.gitprov.services.ProvenanceService;
import ro.uaic.info.gitprov.services.SparqlService;

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

    @RequestMapping(value = "/owner/{owner}/{name}", method = RequestMethod.POST, produces = {"text/csv", "application/ld+json", "application/n-quads", "application/n-triples", "application/json", "application/sparql-results+thrift", "application/trig", "text/tab-separated-values", "application/x-turtle", "application/rdf+xml", "text/plain"})
    @ResponseBody
    HttpEntity<?> executeQuery(HttpServletRequest request, @PathVariable String owner, @PathVariable String name, @RequestBody String query) throws IOException {
        Repository repository = githubService.getRepositoryByOwnerAndName(owner, name);
        String result = provenanceService.repositoryToDocument(repository, ControllerLinkBuilder.linkTo(ProvController.class).slash("owner").slash(owner).slash(name).toString() + "#", "application/x-turtle");

        String contentType = request.getHeader("Accept");
        return new ResponseEntity<>(sparqlService.getQueryResult(result, query, contentType), HttpStatus.OK);

    }
}
