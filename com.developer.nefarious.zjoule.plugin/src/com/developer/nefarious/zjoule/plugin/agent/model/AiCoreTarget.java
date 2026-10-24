package com.developer.nefarious.zjoule.plugin.agent.model;

/**
 * One SAP AI Core deployment the OpenAI client should call.
 *
 * @param apiBaseUrl service URL already suffixed with {@code /v2}
 * @param deploymentId deployment id used in the inference path
 * @param resourceGroup AI Core resource group header
 * @param modelName model name sent as the OpenAI model field
 * @param apiVersion query parameter value
 */
public record AiCoreTarget(String apiBaseUrl, String deploymentId, String resourceGroup, String modelName,
        String apiVersion) {

    /** Query value sent as {@code api-version}. */
    public static final String API_VERSION = "2024-10-21";

    /** Path segment between the service URL and the deployment id. */
    private static final String DEPLOYMENTS = "/inference/deployments/";

    /**
     * Returns the base URL LangChain4j appends {@code /chat/completions} to.
     *
     * @return the deployment base URL, without a trailing slash.
     */
    public String deploymentBaseUrl() {
        String base = apiBaseUrl == null ? "" : apiBaseUrl;
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + DEPLOYMENTS + deploymentId;
    }
}
