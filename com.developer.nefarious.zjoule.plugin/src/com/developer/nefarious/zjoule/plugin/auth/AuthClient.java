package com.developer.nefarious.zjoule.plugin.auth;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpResponse;

import com.developer.nefarious.zjoule.plugin.memory.IMemoryObject;
import com.developer.nefarious.zjoule.plugin.models.AccessToken;
import com.developer.nefarious.zjoule.plugin.models.ServiceKey;

/**
 * The {@code AuthClient} class provides functionality to authenticate with a service
 * and retrieve access tokens. It interacts with memory storage for access tokens
 * and service keys and uses an HTTP client to make requests to the authentication server.
 */
public class AuthClient implements IAuthClient {

    /** Memory storage for saving and loading access tokens. */
    private IMemoryObject<AccessToken> memoryAccessToken;

    /** Memory storage for saving and loading service keys. */
    private IMemoryObject<ServiceKey> memoryServiceKey;

    /** Helper class for constructing requests and processing responses. */
    private IAuthClientHelper authClientHelper;

    /**
     * Constructs an {@code AuthClient} instance with the given dependencies.
     *
     * @param memoryAccessToken the memory storage for access tokens
     * @param memoryServiceKey the memory storage for service keys
     * @param authClientHelper the helper for constructing and handling authentication requests
     */
    public AuthClient(
            final IMemoryObject<AccessToken> memoryAccessToken,
            final IMemoryObject<ServiceKey> memoryServiceKey,
            final IAuthClientHelper authClientHelper) {
        this.memoryAccessToken = memoryAccessToken;
        this.memoryServiceKey = memoryServiceKey;
        this.authClientHelper = authClientHelper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAccessToken() throws IOException, InterruptedException {
        AccessToken lastTokenResponse = memoryAccessToken.load();
        if (lastTokenResponse.isValid()) {
            return lastTokenResponse.getAccessToken();
        } else {
            return getNewAccessToken();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getNewAccessToken() throws IOException, InterruptedException {
        ServiceKey serviceKey = memoryServiceKey.load();
        return getNewAccessToken(serviceKey);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getNewAccessToken(final ServiceKey serviceKey) throws IOException, InterruptedException {
        AccessToken newAccessToken = requestAccessToken(serviceKey);
        memoryAccessToken.save(newAccessToken);
        memoryServiceKey.save(serviceKey);
        return newAccessToken.getAccessToken();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AccessToken requestAccessToken(final ServiceKey serviceKey) throws IOException, InterruptedException {
        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(tokenRequest(serviceKey), HttpResponse.BodyHandlers.ofString());
        return authClientHelper.convertResponseToObject(response.body());
    }

    /**
     * Builds the client-credentials request.
     *
     * @param serviceKey the service key.
     * @return the HTTP request.
     */
    private HttpRequest tokenRequest(final ServiceKey serviceKey) {
        URI endpoint = authClientHelper.convertEndpointStringToURI(serviceKey.getTokenURL());
        BodyPublisher requestBody = authClientHelper.createRequestBody(serviceKey.getClientId(),
                serviceKey.getClientSecret());
        return HttpRequest.newBuilder()
                .uri(endpoint)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(requestBody)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getServiceUrl() {
        return memoryServiceKey.load().getServiceURL();
    }

}
