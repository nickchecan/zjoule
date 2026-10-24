package com.developer.nefarious.zjoule.plugin.auth;

import java.io.IOException;

import com.developer.nefarious.zjoule.plugin.models.AccessToken;
import com.developer.nefarious.zjoule.plugin.models.ServiceKey;

/**
 * Interface defining the contract for an authentication client.
 * An implementation of this interface provides methods to retrieve access tokens
 * and service URLs by interacting with a service's authentication mechanism.
 */
public interface IAuthClient {

	/**
     * Retrieves the current access token. If the token is valid, it returns the cached token;
     * otherwise, it fetches a new token.
     *
     * @return the access token as a {@link String}
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     */
	public String getAccessToken() throws IOException, InterruptedException;

	/**
     * Retrieves a new access token using the stored service key.
     *
     * @return the new access token as a {@link String}
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     */
	public String getNewAccessToken() throws IOException, InterruptedException;

	/**
     * Retrieves a new access token using the provided service key.
     *
     * @param serviceKey the {@link ServiceKey} used for authentication
     * @return the new access token as a {@link String}
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     */
	public String getNewAccessToken(final ServiceKey serviceKey) throws IOException, InterruptedException;

	/**
	 * Requests an access token and does not store it.
	 *
	 * @param serviceKey the {@link ServiceKey} used for authentication
	 * @return the token, including its expiry
	 * @throws IOException if an I/O error occurs
	 * @throws InterruptedException if the operation is interrupted
	 */
	AccessToken requestAccessToken(final ServiceKey serviceKey) throws IOException, InterruptedException;

	/**
     * Retrieves the service URL from the stored service key.
     *
     * @return the service URL as a {@link String}
     */
	public String getServiceUrl();

}
