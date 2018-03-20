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
import ro.uaic.info.gitprov.models.ProvStore.UploadDocumentRequest;
import ro.uaic.info.gitprov.services.GithubService;
import ro.uaic.info.gitprov.services.OAuthService;
import ro.uaic.info.gitprov.services.ProvenanceService;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Map;

import static ro.uaic.info.gitprov.services.OAuthService.decodeQueryString;

@RestController
@RequestMapping(value = "/store")
public class ProvStoreController {

    private static final String PROVSTORE_STORE_DOCUMENT = "https://provenance.ecs.soton.ac.uk/store/api/v0/documents/";
    private final String authorizeUrl = "https://provenance.ecs.soton.ac.uk/store/oauth/authorize/";
    private final String accessTokenUrl = "https://provenance.ecs.soton.ac.uk/store/oauth/access_token/";

    @Autowired
    private GithubService githubService;

    @Autowired
    private ProvenanceService provenanceService;

    @Autowired
    private String provStoreApiKey;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OAuthService oAuthService;

    @RequestMapping(value = "/owner/{owner}/{name}", method = RequestMethod.POST)
    @ResponseBody
    HttpEntity<?> getRepositoryByUserAndNameWithApiKey(@PathVariable String owner, @PathVariable String name, @RequestHeader("Content-Type") String contentType) throws IOException {
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

    @RequestMapping(value = "/login", method = RequestMethod.POST)
    @ResponseBody
    HttpEntity<?> login(HttpSession httpSession) throws IOException {
        String requestTokenUrl = "https://provenance.ecs.soton.ac.uk/store/oauth/request_token/";
        HttpRequest httpRequest = HttpRequest.get(requestTokenUrl)
                .header("Authorization", oAuthService.getRequestTokenAuthorizationHeader(ControllerLinkBuilder.linkTo(ProvStoreController.class).slash("oauth-response").toString()));


        Map<String, String> params = decodeQueryString(httpRequest.body());
        String redirectUri = "https://provenance.ecs.soton.ac.uk/store/oauth/authorize"
                + "?oauth_token=" + params.get("oauth_token");
        httpSession.setAttribute("oauth_token_secret", params.get("oauth_token_secret"));

        return new ResponseEntity<>(redirectUri, HttpStatus.TEMPORARY_REDIRECT);

    }

    @RequestMapping(value = "/oauth-response", method = RequestMethod.GET)
    @ResponseBody
    void oauthResponse(HttpSession session, HttpServletResponse response, @RequestParam("oauth_token") String oauthToken, @RequestParam("oauth_verifier") String oauthVerifier) throws IOException {
        String accessTokenUrl = "https://provenance.ecs.soton.ac.uk/store/oauth/access_token/";
        HttpRequest httpRequest = HttpRequest.get(accessTokenUrl)
                .header("Authorization", oAuthService.getAccessTokenAuthorizationHeader(oauthToken, oauthVerifier, String.valueOf(session.getAttribute("oauth_token_secret"))));

        Map<String, String> params = decodeQueryString(httpRequest.body());

        session.removeAttribute("oauth_token_secret");
        session.setAttribute("oauth_token_secret", params.get("oauth_token_secret"));
        session.setAttribute("oauth_token", params.get("oauth_token"));

        response.sendRedirect("/swagger-ui.html#!/prov45store45controller/updateUsingPOST");
    }

    @RequestMapping(value = "/{owner}/{name}", method = RequestMethod.POST)
    @ResponseBody
    HttpEntity<?> update(HttpSession session, @PathVariable String owner, @PathVariable String name, @RequestBody UploadDocumentRequest uploadDocumentRequest) throws IOException {
        String updateDocumentsUrl = "https://provenance.ecs.soton.ac.uk/store/api/v0/documents/";
        String oauthToken = String.valueOf(session.getAttribute("oauth_token"));
        String oauthTokenSecret = String.valueOf(session.getAttribute("oauth_token_secret"));

        Repository repository = githubService.getRepositoryByOwnerAndName(owner, name);
        String result = provenanceService.repositoryToDocument(repository, ControllerLinkBuilder.linkTo(ProvController.class).slash("owner").slash(owner).slash(name).toString() + "#", "application/json");

        ProvStoreRequestStorage requestStorage = new ProvStoreRequestStorage(uploadDocumentRequest.getName(), uploadDocumentRequest.isPublic(), result);

        HttpRequest request = HttpRequest.post(updateDocumentsUrl)
                .header("Authorization", oAuthService.getRequestAuthorizationHeader(oauthToken, oauthTokenSecret, updateDocumentsUrl))
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
