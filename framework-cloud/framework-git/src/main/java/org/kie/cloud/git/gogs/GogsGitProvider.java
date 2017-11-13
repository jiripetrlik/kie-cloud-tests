package org.kie.cloud.git.gogs;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.gson.JsonObject;
import java.util.Base64;
import org.kie.cloud.git.AbstractGitProvider;
import org.kie.cloud.git.constants.GitConstants;

public class GogsGitProvider extends AbstractGitProvider {

    private static final String REST_API_SUFFIX = "/api/v1";

    private Client client;

    @Override public void createGitRepository(String repositoryName, String repositoryPath) {
        WebTarget webTarget = createWebTarget();
        webTarget = webTarget.path("admin/users/" + GitConstants.getGogsUser() + "/repos");

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("name", repositoryName);
        jsonObject.addProperty("private", false);
        String body = jsonObject.toString();

        Invocation.Builder builder = webTarget.request();
        builder = addBasicAuthenticationHeader(builder);
        builder = addJsonContentTypeHeader(builder);

        Response response = builder.post(Entity.entity(body, MediaType.APPLICATION_JSON));
        try {
            if (response.getStatus() != Response.Status.CREATED.getStatusCode()) {
                response.close();
                throw new RuntimeException("Unable to create new GIT repository: "
                        + repositoryName
                        + ", http response code: "
                        + String.valueOf(response.getStatus()));
            }
        } finally {
            response.close();
        }
    }

    @Override public void deleteGitRepository(String repositoryName) {
        WebTarget webTarget = createWebTarget();
        webTarget = webTarget.path("repos");
        webTarget = webTarget.path(GitConstants.getGogsUser());
        webTarget = webTarget.path(repositoryName);

        Invocation.Builder builder = webTarget.request();
        builder = addBasicAuthenticationHeader(builder);
        builder = addJsonContentTypeHeader(builder);

        Response response = builder.delete();
        try {
            if (response.getStatus() != Response.Status.NO_CONTENT.getStatusCode()) {
                throw new RuntimeException("Unable to delete GIT repository: "
                        + repositoryName
                        + ", http response code: "
                        + String.valueOf(response.getStatus()));
            }
        } finally {
            response.close();
        }
    }

    @Override public String getRepositoryUrl(String repositoryName) {
        return null;
    }

    @Override public void init() {
        client = ClientBuilder.newClient();
    }

    @Override public void destroy() {
        client.close();
    }

    private WebTarget createWebTarget() {
        return client.target(GitConstants.getGogsUrl() + REST_API_SUFFIX);
    }

    private Invocation.Builder addBasicAuthenticationHeader(Invocation.Builder builder) {
        Base64.Encoder encoder = Base64.getEncoder();

        String usernameAndPassword = GitConstants.getGogsUser() + ":" + GitConstants.getGogsPassword();
        String authorizationHeaderValue = "Basic " + encoder.encodeToString(usernameAndPassword.getBytes());
        builder.header("Authorization", authorizationHeaderValue);

        return builder;
    }

    private Invocation.Builder addJsonContentTypeHeader(Invocation.Builder builder) {
        builder.header("Content-Type", "application/json");
        return builder;
    }
}
