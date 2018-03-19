package ro.uaic.info.gitprov.controllers;

import com.github.kevinsawicki.http.HttpRequest;
import org.eclipse.egit.github.core.Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.uaic.info.gitprov.services.GithubService;
import ro.uaic.info.gitprov.services.ProvOVizService;
import ro.uaic.info.gitprov.services.ProvenanceService;

import java.io.IOException;

@RestController
@RequestMapping(value = "/viz/{owner}/{name}")
public class ProvOVizController {


    @Autowired
    private GithubService githubService;

    @Autowired
    private ProvenanceService provenanceService;

    @Autowired
    private ProvOVizService provOVizService;

    @RequestMapping(value = "", method = RequestMethod.GET)
    @ResponseBody
    HttpEntity<?> getVizualization(@PathVariable String owner, @PathVariable String name) throws IOException {
        String provOVizServiceUrl = "http://provoviz.org/service";

        Repository repository = githubService.getRepositoryByOwnerAndName(owner, name);
        String result = provenanceService.repositoryToDocument(repository, ControllerLinkBuilder.linkTo(ProvController.class).slash("owner").slash(owner).slash(name).toString() + "#", "application/x-turtle");

        HttpRequest httpRequest = HttpRequest.post(provOVizServiceUrl)
                .form(provOVizService.getFormParameters(result));


        return new ResponseEntity<>(httpRequest.body(), HttpStatus.valueOf(httpRequest.code()));

    }

}
