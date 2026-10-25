package com.developer.nefarious.zjoule.plugin.core;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.developer.nefarious.zjoule.plugin.agent.ChatRuntime;
import com.developer.nefarious.zjoule.plugin.agent.ChatSessionService;
import com.developer.nefarious.zjoule.plugin.core.preferences.CompactionLimits;
import com.developer.nefarious.zjoule.plugin.core.preferences.Instruction;
import com.developer.nefarious.zjoule.plugin.login.memory.TemporaryMemoryAccessToken;
import com.developer.nefarious.zjoule.plugin.login.memory.TemporaryMemoryDeployment;
import com.developer.nefarious.zjoule.plugin.login.memory.TemporaryMemoryOllamaEndpoint;
import com.developer.nefarious.zjoule.plugin.login.memory.TemporaryMemoryOllamaModel;
import com.developer.nefarious.zjoule.plugin.login.memory.TemporaryMemoryResourceGroup;
import com.developer.nefarious.zjoule.plugin.login.memory.TemporaryMemoryServiceKey;
import com.developer.nefarious.zjoule.plugin.memory.EclipseMemory;
import com.developer.nefarious.zjoule.plugin.memory.LegacySecretCleaner;
import com.developer.nefarious.zjoule.plugin.memory.MemoryDeployment;
import com.developer.nefarious.zjoule.plugin.memory.MemoryOllamaEndpoint;
import com.developer.nefarious.zjoule.plugin.memory.MemoryOllamaModel;
import com.developer.nefarious.zjoule.plugin.memory.MemoryResourceGroup;
import com.developer.nefarious.zjoule.plugin.memory.MemoryServiceKey;
import com.developer.nefarious.zjoule.plugin.memory.SecureStore;
import com.developer.nefarious.zjoule.plugin.memory.utils.ObjectSerializer;

/**
 * The activator class controls the plug-in lifecycle for the zJoule plugin.
 * <p>
 * This class extends {@link AbstractUIPlugin} and serves as the entry point
 * for the plug-in. It manages the initialization of various memory components
 * and provides a shared instance for use across the plugin.
 */
public class Activator extends AbstractUIPlugin {

    /** The plug-in ID for the zJoule plugin. */
    public static final String PLUGIN_ID = "com.developer.nefarious.zjoule"; //$NON-NLS-1$

    /** The shared instance of the {@code Activator}. */
    private static Activator plugin;

    /** Lazily opened chat runtime. History lives in the checkpointer. */
    private ChatRuntime chatRuntime;

    /**
     * Returns the shared instance of the {@code Activator}.
     *
     * @return the shared {@code Activator} instance.
     */
    public static Activator getDefault() {
        return plugin;
    }

    /**
     * The default constructor for the {@code Activator}.
     */
    public Activator() {
    }

    /**
     * Initializes memory resources used throughout the plugin.
     * <p>
     * This method sets up resources for chat operations, login management,
     * and temporary memory storage by leveraging {@link ObjectSerializer} and
     * {@link EclipseMemory}.
     */
    private void initialize() {
        ObjectSerializer objectSerializer = new ObjectSerializer();
        EclipseMemory eclipseMemory = new EclipseMemory();
        LegacySecretCleaner.erase(eclipseMemory);
        SecureStore secureStore = new SecureStore();

        MemoryServiceKey.initialize(secureStore);
        MemoryResourceGroup.initialize(eclipseMemory);
        MemoryDeployment.initialize(objectSerializer, eclipseMemory);
        MemoryOllamaEndpoint.initialize(eclipseMemory);
        MemoryOllamaModel.initialize(objectSerializer, eclipseMemory);

        TemporaryMemoryAccessToken.initialize();
        TemporaryMemoryServiceKey.initialize(secureStore);
        TemporaryMemoryResourceGroup.initialize(eclipseMemory);
        TemporaryMemoryDeployment.initialize(objectSerializer, eclipseMemory);
        TemporaryMemoryOllamaEndpoint.initialize(eclipseMemory);
        TemporaryMemoryOllamaModel.initialize(objectSerializer, eclipseMemory);
    }

    /**
     * Returns the chat session service, opening it on first use.
     *
     * @return the session service.
     */
    public synchronized ChatSessionService chatSessions() {
        if (chatRuntime == null) {
            chatRuntime = ChatRuntime.openDefault();
        }
        return chatRuntime;
    }

    /**
     * Starts the plugin by initializing the shared instance and memory resources.
     *
     * @param context the OSGi bundle context.
     * @throws Exception if the plugin fails to start.
     */
    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
        initialize();
    }

    /**
     * Stops the plugin by clearing the shared instance.
     *
     * @param context the OSGi bundle context.
     * @throws Exception if the plugin fails to stop.
     */
    @Override
    public void stop(final BundleContext context) throws Exception {
        if (chatRuntime != null) {
            chatRuntime.shutdown();
            chatRuntime = null;
        }
        plugin = null;
        super.stop(context);
    }
    
    /**
     * Initializes the default preferences for the plugin.
     * <p>
     * This method sets up default preference values to guide plugin behavior.
     *
     * @param store the {@link IPreferenceStore} instance used for storing preferences.
     */
    @Override
    protected void initializeDefaultPreferences(final IPreferenceStore store) {
        store.setDefault(Instruction.KEY, "Be a friendly SAP ABAP expert, providing concise answers aligned with best practices and clean-code principles.");
        store.setDefault(CompactionLimits.THRESHOLD_KEY, CompactionLimits.DEFAULT_THRESHOLD);
        store.setDefault(CompactionLimits.KEEP_KEY, CompactionLimits.DEFAULT_KEEP);
    }
}
