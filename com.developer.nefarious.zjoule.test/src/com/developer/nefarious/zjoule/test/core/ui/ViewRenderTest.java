package com.developer.nefarious.zjoule.test.core.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.ThreadLocalRandom;

import org.eclipse.core.runtime.FileLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.osgi.framework.Bundle;

import com.developer.nefarious.zjoule.plugin.core.Activator;
import com.developer.nefarious.zjoule.plugin.core.ui.IViewRender;
import com.developer.nefarious.zjoule.plugin.core.ui.ViewRender;

public class ViewRenderTest {

	private IViewRender cut;

	private String randomWord() {
		final String[] WORDS = { "apple", "banana", "grape" };
		int randomIndex = ThreadLocalRandom.current().nextInt(WORDS.length);
		return WORDS[randomIndex];
	}

	@BeforeEach
	public void setUp() {
		cut = spy(ViewRender.create());
	}

	@Test
	public void shouldGetResourceContent() throws IOException {
		// Arrange
		String expectedValue = randomWord() + "\n";
		InputStream mockInputStream = new ByteArrayInputStream(expectedValue.getBytes());

		String viewFilesPath = "resources/views/";
		String mockFileName = randomWord();

		Bundle mockBundle = mock(Bundle.class);
	    Activator mockActivator = mock(Activator.class);
		URL mockUnresolvedURL = mock(URL.class);
		URL mockResolvedURL = mock(URL.class);

		// Mock the static call for Platform.getBundle(...)
		try (MockedStatic<Activator> mockedStaticActivator = mockStatic(Activator.class)) {
			mockedStaticActivator.when(Activator::getDefault).thenReturn(mockActivator);

			when(mockActivator.getBundle()).thenReturn(mockBundle);
			when(mockBundle.getEntry(viewFilesPath + mockFileName)).thenReturn(mockUnresolvedURL);

			// Mock the static call for FileLocator.toFileURL(...)
			try (MockedStatic<FileLocator> mockedStaticLocator = mockStatic(FileLocator.class)) {
				mockedStaticLocator.when(() -> FileLocator.toFileURL(mockUnresolvedURL)).thenReturn(mockResolvedURL);

				when(mockResolvedURL.openStream()).thenReturn(mockInputStream);

				// Act
				String returnValue = cut.getResourceContent(mockFileName);

				// Assert
				verify(mockBundle).getEntry(viewFilesPath + mockFileName);
				verify(mockResolvedURL).openStream();
				assertEquals(returnValue, expectedValue);
			}
		}
	}

	@Test
	public void shouldGetViewContent() {
		// Arrange
		String mockMarkedContent = "marked-library";
		doReturn(mockMarkedContent).when(cut).getResourceContent("marked.min.js");
		String expectedMarkedReference = "<script>" + mockMarkedContent + "</script>";

		String mockScriptsFileContent = "js-scritps";
		doReturn(mockScriptsFileContent).when(cut).getResourceContent("scripts.js");
		String expectedScriptsReference = "<script>" + mockScriptsFileContent + "</script>";

		String mockStylesFileContent = "css-styles";
		doReturn(mockStylesFileContent).when(cut).getResourceContent("styles.css");
		String expectedStylesReference = "<style>" + mockStylesFileContent + "</style>";

		// Act
		String returnValue = cut.build();

		// Assert
		assertTrue(returnValue.contains("<title>zJoule</title>"));
		assertTrue(returnValue.contains(expectedMarkedReference));
		assertTrue(returnValue.contains(expectedScriptsReference));
		assertTrue(returnValue.contains(expectedStylesReference));
	}

}
