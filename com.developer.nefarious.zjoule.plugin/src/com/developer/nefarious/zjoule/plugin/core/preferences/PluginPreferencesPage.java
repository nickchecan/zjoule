package com.developer.nefarious.zjoule.plugin.core.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.developer.nefarious.zjoule.plugin.auth.SessionManager;
import com.developer.nefarious.zjoule.plugin.core.Activator;
import com.developer.nefarious.zjoule.plugin.memory.MemoryDeployment;
import com.developer.nefarious.zjoule.plugin.memory.MemoryOllamaEndpoint;
import com.developer.nefarious.zjoule.plugin.memory.MemoryOllamaModel;
import com.developer.nefarious.zjoule.plugin.memory.MemoryResourceGroup;
import com.developer.nefarious.zjoule.plugin.models.Deployment;
import com.developer.nefarious.zjoule.plugin.models.OllamaModel;

/**
 * Represents the preference page for configuring plugin settings.
 * <p>
 * The {@code PluginPreferencesPage} allows users to configure settings
 * related to SAP AI Core and Ollama AI models. It dynamically determines
 * which settings to display based on the active session type.
 * </p>
 */
public class PluginPreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    /** Default spacer size in pixels. */
    private static final int SPACER_SIZE = 10;

    /** Default spacer width in pixels. */
    private static final int SPACER_WIDTH = 10;

    /** Default spacer height in pixels. */
    private static final int SPACER_HEIGHT = 10;

    /**
     * Constructs a {@code PluginPreferencesPage} instance.
     * <p>
     * This sets up the preference page layout using the Eclipse preference store.
     * </p>
     */
    public PluginPreferencesPage() {
        super(GRID);
    }

    /**
     * Initializes the preference page with the Eclipse workbench.
     *
     * @param workbench the {@link IWorkbench} instance associated with the preference page.
     */
    @Override
    public void init(final IWorkbench workbench) {
        setPreferenceStore(Activator.getDefault().getPreferenceStore());
    }

    /**
     * Creates the field editors for the preference page.
     * <p>
     * This method dynamically adds configuration settings based on the session type.
     * It displays chat instructions and either SAP AI Core or Ollama configuration settings.
     * </p>
     */
    @Override
    protected void createFieldEditors() {
        displayInstructionsInput();
        createSpacer();

        if (SessionManager.isSapSession()) {
            displaySapSettings();
        } else if (SessionManager.isOllamaSession()) {
            displayOllamaSettings();
        }
    }

    /**
     * Displays the input field for chat instructions.
     */
    private void displayInstructionsInput() {
        Group group = new Group(getFieldEditorParent(), SWT.NONE);
        group.setText("Chat Settings");
        group.setLayout(new GridLayout(1, false));
        group.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        Composite groupContent = new Composite(group, SWT.NONE);
        groupContent.setLayout(new GridLayout(1, false));
        groupContent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        addField(new InputField(Instruction.KEY, "Instructions:", groupContent));
        addField(new IntegerFieldEditor(HistoryWindow.KEY, "History window:", groupContent));
    }

    /**
     * Creates a spacer to add vertical spacing in the layout.
     */
    private void createSpacer() {
        Composite spacer = new Composite(getFieldEditorParent(), SWT.NONE);
        GridData gridData = new GridData(SPACER_WIDTH, SPACER_HEIGHT);
        gridData.heightHint = SPACER_SIZE;
        spacer.setLayoutData(gridData);
    }

    /**
     * Creates a container group with a given title for organizing settings.
     *
     * @param title the title of the container group.
     * @return a {@link Composite} container for adding UI components.
     */
    private Composite createContentContainer(final String title) {
        Group group = new Group(getFieldEditorParent(), SWT.NONE);
        group.setText(title);
        group.setLayout(new GridLayout(1, false));
        group.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        Composite groupContent = new Composite(group, SWT.NONE);
        groupContent.setLayout(new GridLayout(1, false));
        groupContent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        return groupContent;
    }

    /**
     * Displays SAP AI Core configuration settings.
     * <p>
     * This method retrieves SAP AI Core parameters from memory and presents
     * them as read-only fields.
     * </p>
     */
    private void displaySapSettings() {
        Composite contentContainer = createContentContainer("SAP AI Core Configuration Parameters");

        class OutputFieldFactory {
            StringFieldEditor create(final String key, final String value) {
                return new OutputField(key, value, contentContainer);
            }
        }

        OutputFieldFactory outputFieldFactory = new OutputFieldFactory();

        String resourceGroup = MemoryResourceGroup.getInstance().load();
        addField(outputFieldFactory.create("Resource Group:", resourceGroup));

        Deployment deployment = MemoryDeployment.getInstance().load();

        String configurationName = deployment.getConfigurationName();
        addField(outputFieldFactory.create("Configuration Name:", configurationName));

        String model = deployment.getModelName();
        addField(outputFieldFactory.create("Model:", model));

        String version = deployment.getModelVersion();
        addField(outputFieldFactory.create("Version:", version));

        String deploymentUrl = deployment.getDeploymentUrl();
        addField(outputFieldFactory.create("Deployment Url:", deploymentUrl));
    }

    /**
     * Displays Ollama AI model configuration settings.
     * <p>
     * This method retrieves Ollama parameters from memory and presents
     * them as read-only fields.
     * </p>
     */
    private void displayOllamaSettings() {
        Composite contentContainer = createContentContainer("Ollama Configuration Parameters");

        class OutputFieldFactory {
            StringFieldEditor create(final String key, final String value) {
                return new OutputField(key, value, contentContainer);
            }
        }

        OutputFieldFactory outputFieldFactory = new OutputFieldFactory();

        String endpoint = MemoryOllamaEndpoint.getInstance().load();
        addField(outputFieldFactory.create("Endpoint:", endpoint));

        OllamaModel ollamaModel = MemoryOllamaModel.getInstance().load();

        String name = ollamaModel.getName();
        addField(outputFieldFactory.create("Name:", name));

        String model = ollamaModel.getModel();
        addField(outputFieldFactory.create("Model:", model));

        String format = ollamaModel.getFormat();
        addField(outputFieldFactory.create("Format:", format));

        String family = ollamaModel.getFamily();
        addField(outputFieldFactory.create("Family:", family));

        String parameterSize = ollamaModel.getParameterSize();
        addField(outputFieldFactory.create("Parameter Size:", parameterSize));

        String quantizationLevel = ollamaModel.getQuantizationLevel();
        addField(outputFieldFactory.create("Quantization Level:", quantizationLevel));
    }
}