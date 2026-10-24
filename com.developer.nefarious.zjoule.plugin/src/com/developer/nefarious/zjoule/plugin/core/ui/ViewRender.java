package com.developer.nefarious.zjoule.plugin.core.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.osgi.framework.Bundle;

import com.developer.nefarious.zjoule.plugin.core.Activator;

/**
 * Responsible for rendering an HTML-based view with embedded JavaScript and CSS resources.
 * <p>
 * The {@code ViewRender} class implements {@link IViewRender} and provides methods to build
 * a complete HTML page dynamically by including JavaScript and CSS resources from the plugin's bundle.
 */
public class ViewRender implements IViewRender {

    /** Path to the directory containing view resources such as JavaScript and CSS files. */
    private static final String VIEW_FILES_PATH = "resources/views/";

    /**
     * Factory method to create a new {@code ViewRender} instance.
     *
     * @return a new {@link IViewRender} instance.
     */
    public static IViewRender create() {
        return new ViewRender();
    }

    /**
     * Private constructor to enforce the use of the factory method.
     */
    private ViewRender() { }

    /**
     * {@inheritDoc}
     */
    @Override
    public String build() {
        String marked = getResourceContent("marked.min.js");
        String js = getResourceContent("scripts.js");
        String css = getResourceContent("styles.css");

        StringBuilder buffer = new StringBuilder();

        buffer.append("<!doctype html>");
        buffer.append("<html lang=\"en\">");
        buffer.append("<head>");
        buffer.append("<meta charset=\"utf-8\">");
        buffer.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
        buffer.append("<title>zJoule</title>");
        buffer.append("<style>" + css + "</style>");
        buffer.append("<script>" + marked + "</script>");
        buffer.append("<script>" + js + "</script>");
        buffer.append("</head>");
        buffer.append("<body>");
        buffer.append("<div class=\"center-instructions\" id=\"instructions\">");
        buffer.append("<h2>Let’s Get Started!</h2>");
        buffer.append("<p>Locate and click on the \"Authentication\" Button at the top of the screen. </p>");
        buffer.append("</div>");
        buffer.append("<div class=\"chat-container\">");
        buffer.append("<div class=\"placeholder\" id=\"placeholder\"> We’re ready to roll!<br />Start typing to begin! </div>");
        buffer.append("<div class=\"chat-box\" id=\"chatBox\">");
        buffer.append("</div>");
        buffer.append("<div class=\"chat-input\">");
        buffer.append("<div class=\"tag-box\">#Tag</div>");
        buffer.append("<input type=\"text\" id=\"userInput\" placeholder=\"Type your message here...\" />");
        buffer.append("<div class=\"button-row\">");
        buffer.append("<button id=\"send\" onclick=\"sendMessage()\">Send</button>");
        buffer.append("<button id=\"stop\" onclick=\"stopGeneration()\" disabled>Stop</button>");
        buffer.append("</div>");
        buffer.append("</div>");
        buffer.append("</div>");
        buffer.append("</body>");
        buffer.append("</html>");

        return buffer.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getResourceContent(final String filename) {
        Bundle bundle = Activator.getDefault().getBundle();
        final URL unresolvedfileURL = bundle.getEntry(VIEW_FILES_PATH + filename);
        try {
            URL resolvedFileURL = FileLocator.toFileURL(unresolvedfileURL);
            try (InputStream inputStream = resolvedFileURL.openStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
                return content.toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }
}
