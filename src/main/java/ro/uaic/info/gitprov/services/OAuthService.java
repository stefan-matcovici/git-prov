package ro.uaic.info.gitprov.services;

import com.github.seratch.signedrequest4j.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class OAuthService {

    @Autowired
    private String provStoreConsumerKey;

    @Autowired
    private String provStoreConsumerSecret;

    public static Map<String, String> decodeQueryString(String query) {
        try {
            Map<String, String> params = new LinkedHashMap<>();
            for (String param : query.split("&")) {
                String[] keyValue = param.split("=", 2);
                String key = URLDecoder.decode(keyValue[0], "UTF-8");
                String value = keyValue.length > 1 ? URLDecoder.decode(keyValue[1], "UTF-8") : "";
                if (!key.isEmpty()) {
                    params.put(key, value);
                }
            }
            return params;
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e); // Cannot happen with UTF-8 encoding.
        }
    }

    public String getRequestTokenAuthorizationHeader() throws UnsupportedEncodingException {
        String nonce = String.valueOf(((int) (Math.random() * 100000000)));
        long timestamp = System.currentTimeMillis() / 1000;

        String callbackUrl = "http://localhost:8080/store/oauth-response";

        return "OAuth " + "oauth_callback=\"" +
                URLEncoder.encode(callbackUrl, "utf-8") + "\"," +
                "oauth_consumer_key=\"" + provStoreConsumerKey + "\"," +
                "oauth_nonce=\"" + nonce + "\"," +
                "oauth_signature_method=\"PLAINTEXT\"," +
                "oauth_timestamp=\"" + timestamp + "\"," +
                "oauth_version=\"1.0\"," +
                "oauth_signature=\"" + URLEncoder.encode(provStoreConsumerSecret + "&", "utf-8") + "\"";
    }

    public String getAccessTokenAuthorizationHeader(String oauthToken, String oauthVerifier, String oauthTokenSecret) throws UnsupportedEncodingException {
        String nonce = String.valueOf(((int) (Math.random() * 100000000)));
        long timestamp = System.currentTimeMillis() / 1000;

        return "OAuth " +
                "oauth_consumer_key=\"" + provStoreConsumerKey + "\"," +
                "oauth_nonce=\"" + nonce + "\"," +
                "oauth_signature_method=\"PLAINTEXT\"," +
                "oauth_timestamp=\"" + timestamp + "\"," +
                "oauth_token=\"" + oauthToken + "\"," +
                "oauth_verifier=\"" + oauthVerifier + "\"," +
                "oauth_version=\"1.0\"," +
                "oauth_signature=\"" + URLEncoder.encode(provStoreConsumerSecret + "&" + oauthTokenSecret, "utf-8") + "\"";
    }

    public String getRequestAuthorizationHeader(String oauthToken, String oauthTokenSecret, String updateDocumentsUrl) {
        OAuthConsumer consumer = new OAuthConsumer(provStoreConsumerKey, provStoreConsumerSecret);
        OAuthAccessToken authAccessToken = new OAuthAccessToken(oauthToken, oauthTokenSecret);
        SignedRequest signedRequest = SignedRequestFactory.create(consumer, authAccessToken, SignatureMethod.HMAC_SHA1);

        String nonce = String.valueOf(((int) (Math.random() * 100000000)));
        long timestamp = System.currentTimeMillis() / 1000;

        return "OAuth " +
                "oauth_consumer_key=\"" + provStoreConsumerKey + "\"," +
                "oauth_token=\"" + oauthToken + "\"," +
                "oauth_signature_method=\"HMAC-SHA1\"," +
                "oauth_timestamp=\"" + timestamp + "\"," +
                "oauth_nonce=\"" + nonce + "\"," +
                "oauth_version=\"1.0\"," +
                "oauth_signature=\"" + signedRequest.getSignature(updateDocumentsUrl, HttpMethod.POST, nonce, timestamp) + "\"";
    }
}
