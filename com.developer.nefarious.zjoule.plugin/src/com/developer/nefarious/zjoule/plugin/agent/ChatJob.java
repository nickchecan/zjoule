package com.developer.nefarious.zjoule.plugin.agent;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

/**
 * Runs one chat turn where the Progress view can cancel it.
 */
final class ChatJob extends Job {

    /** Name shown in the Progress view. */
    static final String NAME = "zJoule chat";

    private final ChatRuntime runtime;

    private final ChatRun run;

    private final String userText;

    private final String editorContext;

    private final boolean resume;

    private final boolean approved;

    /**
     * Creates a user-visible job.
     *
     * @param runtime the runtime.
     * @param run the run handle.
     * @param userText the user message.
     * @param editorContext the editor text.
     * @param resume whether this is a resume.
     * @param approved the resume decision.
     */
    ChatJob(final ChatRuntime runtime, final ChatRun run, final String userText, final String editorContext,
            final boolean resume, final boolean approved) {
        super(NAME);
        this.runtime = runtime;
        this.run = run;
        this.userText = userText;
        this.editorContext = editorContext;
        this.resume = resume;
        this.approved = approved;
        setSystem(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected IStatus run(final IProgressMonitor monitor) {
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(modelClassLoader());
        try {
            ChatTurn.execute(runtime, run, userText, editorContext, resume, approved, monitor);
        } finally {
            Thread.currentThread().setContextClassLoader(previous);
            run.markDone();
        }
        return Status.OK_STATUS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void canceling() {
        run.cancelFromJob();
    }

    /**
     * Uses the libs bundle class loader so ServiceLoader lookups see the embedded jars.
     *
     * @return the model class loader, or the current one when it is unavailable.
     */
    private static ClassLoader modelClassLoader() {
        ClassLoader loader = OpenAiStreamingChatModel.class.getClassLoader();
        if (loader == null) {
            return Thread.currentThread().getContextClassLoader();
        }
        return loader;
    }
}
