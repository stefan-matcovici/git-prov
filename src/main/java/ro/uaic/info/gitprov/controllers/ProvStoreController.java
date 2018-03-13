package ro.uaic.info.gitprov.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kevinsawicki.http.HttpRequest;
import org.eclipse.egit.github.core.Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.uaic.info.gitprov.models.ProvStore.ProvStoreRequestStorage;
import ro.uaic.info.gitprov.models.ProvStore.ProvStoreResponseStorage;
import ro.uaic.info.gitprov.services.GithubService;
import ro.uaic.info.gitprov.services.ProvenanceService;

import java.io.IOException;

@RestController
@RequestMapping(value = "/store")
public class ProvStoreController {

    private static final String PROVSTORE_STORE_DOCUMENT = "https://provenance.ecs.soton.ac.uk/store/api/v0/documents/";
    @Autowired
    private GithubService githubService;
    @Autowired
    private ProvenanceService provenanceService;
    @Autowired
    private String provStoreApiKey;
    @Autowired
    private ObjectMapper objectMapper;

    @RequestMapping(value = "/owner/{owner}/{name}", method = RequestMethod.POST)
    @ResponseBody
    HttpEntity<?> getRepositoryByUserAndName(@PathVariable String owner, @PathVariable String name, @RequestHeader("Content-Type") String contentType) throws IOException {
        Repository repository = githubService.getRepositoryByOwnerAndName(owner, name);
        String result = provenanceService.repositoryToDocument(repository, ControllerLinkBuilder.linkTo(ProvController.class).slash("owner").slash(owner).slash(name).toString() + "#", contentType);

        ProvStoreRequestStorage requestStorage = new ProvStoreRequestStorage(owner + "-" + name, true, result);

        HttpRequest request = HttpRequest.post(PROVSTORE_STORE_DOCUMENT)
                .authorization("ApiKey stefan-matcovici:" + provStoreApiKey)
                .accept("application/json")
                .contentType("application/json")
                .send(objectMapper.writeValueAsBytes(requestStorage));

        int responseCode = request.code();
        String responseBody = request.body();
        if (HttpStatus.valueOf(responseCode) != HttpStatus.CREATED) {
            return new ResponseEntity<>(responseBody, HttpStatus.OK);
        }

        ProvStoreResponseStorage responseStorage = objectMapper.readValue(responseBody, ProvStoreResponseStorage.class);

        return new ResponseEntity<>(responseStorage, HttpStatus.CREATED);


    }


}
