package com.developer.nefarious.zjoule.test.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import com.developer.nefarious.zjoule.plugin.auth.AuthClient;
import com.developer.nefarious.zjoule.plugin.auth.AuthClientHelper;
import com.developer.nefarious.zjoule.plugin.memory.IMemoryObject;
import com.developer.nefarious.zjoule.plugin.memory.MemoryServiceKey;
import com.developer.nefarious.zjoule.plugin.models.AccessToken;
import com.developer.nefarious.zjoule.plugin.models.ServiceKey;

public class AuthClientTest {

	private AuthClient cut;

	MockedStatic<HttpClient> mockedStaticHttpClient;

	MockedStatic<HttpRequest> mockedStaticHttpRequest;

	@Mock
	private HttpClient mockHttpClient;

	@Mock
	private AuthClientHelper mockAuthClientHelper;

	@Mock
	private IMemoryObject<AccessToken> mockMemoryAccessToken;

	@Mock
	private MemoryServiceKey mockMemoryServiceKey;

	@Mock
	private AccessToken mockAccessToken;

	@Mock
	private ServiceKey mockServiceKey;

	@Mock
	private Builder mockBuilder;

	@Mock
	private HttpRequest mockHttpRequest;

	@BeforeEach
	public void setUp() {
		MockitoAnnotations.openMocks(this);

		mockedStaticHttpClient = mockStatic(HttpClient.class);
		mockedStaticHttpClient.when(HttpClient::newHttpClient).thenReturn(mockHttpClient);

		mockedStaticHttpRequest = mockStatic(HttpRequest.class);
		mockedStaticHttpRequest.when(HttpRequest::newBuilder).thenReturn(mockBuilder);

		when(mockMemoryAccessToken.load()).thenReturn(mockAccessToken);
		when(mockMemoryServiceKey.load()).thenReturn(mockServiceKey);

		cut = spy(new AuthClient(mockMemoryAccessToken, mockMemoryServiceKey, mockAuthClientHelper));
	}

	@Test
	public void shouldGetNewAccessTokenWithoutServiceKey() throws IOException, InterruptedException {
		// Arrange
		when(mockMemoryServiceKey.load()).thenReturn(mockServiceKey);

		String expectedValue = "secret-token";
		doReturn(expectedValue).when(cut).getNewAccessToken(mockServiceKey);

		// Act
		String returnValue = cut.getNewAccessToken();

		// Assert
		assertEquals(expectedValue, returnValue);
	}

	@Test
	public void shouldGetNewAccessTokenWithServiceKey() throws IOException, InterruptedException {
		// Arrange
		String mockTokenURL = "some-url";
		when(mockServiceKey.getTokenURL()).thenReturn(mockTokenURL);

		URI mockEndpoint = mock(URI.class);
		when(mockAuthClientHelper.convertEndpointStringToURI(mockTokenURL)).thenReturn(mockEndpoint);

		String mockClientId = "client-id";
		when(mockServiceKey.getClientId()).thenReturn(mockClientId);
		String mockClientSecret = "client-secret";
		when(mockServiceKey.getClientSecret()).thenReturn(mockClientSecret);

		BodyPublisher mockRequestBody = mock(BodyPublisher.class);
		when(mockAuthClientHelper.createRequestBody(mockClientId, mockClientSecret)).thenReturn(mockRequestBody);

		when(mockBuilder.uri(mockEndpoint)).thenReturn(mockBuilder);
		when(mockBuilder.header("Content-Type", "application/x-www-form-urlencoded")).thenReturn(mockBuilder);
		when(mockBuilder.POST(mockRequestBody)).thenReturn(mockBuilder);
		when(mockBuilder.build()).thenReturn(mockHttpRequest);

		HttpResponse<String> mockResponse = mock(HttpResponse.class);
		when(mockHttpClient.send(mockHttpRequest, HttpResponse.BodyHandlers.ofString())).thenReturn(mockResponse);

		String mockResponseBody = "response-content";
		when(mockResponse.body()).thenReturn(mockResponseBody);
		when(mockAuthClientHelper.convertResponseToObject(mockResponseBody)).thenReturn(mockAccessToken);

		String expectedValue = "new-access-token";
		when(mockAccessToken.getAccessToken()).thenReturn(expectedValue);

		// Act
		String returnValue = cut.getNewAccessToken(mockServiceKey);

		// Assert
		verify(mockMemoryAccessToken).save(mockAccessToken);
		verify(mockMemoryServiceKey).save(mockServiceKey);
		assertEquals(expectedValue, returnValue);
	}

	@Test
	public void shouldReturnAccessTokenFromMemory() throws IOException, InterruptedException {
		// Arrange
		when(mockAccessToken.isValid()).thenReturn(true);
		String expectedValue = "super-secret-from-memory";
		when(mockAccessToken.getAccessToken()).thenReturn(expectedValue);

		// Act
		String returnValue = cut.getAccessToken();

		// Assert
		assertEquals(returnValue, expectedValue);
	}

	@Test
	public void shouldReturnNewAccessTokenIfMemoryIsInvalid() throws IOException, InterruptedException {
		// Arrange
		when(mockAccessToken.isValid()).thenReturn(false);
		String expectedValue = "super-secret-from-memory";
		doReturn(expectedValue).when(cut).getAccessToken();

		// Act
		String returnValue = cut.getAccessToken();

		// Assert
		assertEquals(returnValue, expectedValue);
	}

	@Test
	public void shouldReturnServiceURL() {
		// Arrange
		when(mockMemoryServiceKey.load()).thenReturn(mockServiceKey);

		String expectedValue = "some-url";
		when(mockServiceKey.getServiceURL()).thenReturn(expectedValue);

		// Act
		String returnValue = cut.getServiceUrl();

		// Assert
		assertEquals(expectedValue, returnValue);
	}

	@AfterEach
	public void tearDown() {
		if (mockedStaticHttpClient != null) {
			mockedStaticHttpClient.close();
		}
		if (mockedStaticHttpRequest != null) {
			mockedStaticHttpRequest.close();
		}
	}

}
