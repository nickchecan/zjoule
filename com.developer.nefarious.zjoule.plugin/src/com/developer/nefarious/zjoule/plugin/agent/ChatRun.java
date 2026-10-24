package com.developer.nefarious.zjoule.plugin.agent;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.bsc.async.AsyncGenerator;
import org.eclipse.core.runtime.jobs.Job;

import dev.langchain4j.model.chat.response.StreamingHandle;

/**
 * One chat turn. Cancel stops the graph iterator and the HTTP handle.
 * {@code GraphResult.isCancelled()} is not used: it stayed false in the API probe.
 */
final class ChatRun implements RunHandle {

    private final String threadId;

    private final ChatStreamListener listener;

    private final AtomicReference<StreamingHandle> handle;

    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    private final AtomicBoolean done = new AtomicBoolean(false);

    private final AtomicReference<AsyncGenerator.Cancellable<?>> generator = new AtomicReference<>();

    private volatile Job job;

    /**
     * Creates the run.
     *
     * @param threadId the thread id.
     * @param listener the stream listener.
     * @param handle the slot filled when the model reports a streaming handle.
     */
    ChatRun(final String threadId, final ChatStreamListener listener,
            final AtomicReference<StreamingHandle> handle) {
        this.threadId = threadId;
        this.listener = listener;
        this.handle = handle;
    }

    /**
     * Attaches the Eclipse job that executes this run.
     *
     * @param job the job.
     */
    void attach(final Job job) {
        this.job = job;
    }

    /**
     * Attaches the graph stream so cancel can stop iteration.
     *
     * @param generator the cancellable stream.
     */
    void attachGenerator(final AsyncGenerator.Cancellable<?> generator) {
        this.generator.set(generator);
    }

    /**
     * Marks the job finished.
     */
    void markDone() {
        done.set(true);
    }

    /**
     * Returns the thread id.
     *
     * @return the thread id.
     */
    String threadId() {
        return threadId;
    }

    /**
     * Returns the listener.
     *
     * @return the listener.
     */
    ChatStreamListener listener() {
        return listener;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cancel() {
        if (!cancelled.compareAndSet(false, true)) {
            return;
        }
        stopStreams();
        Job current = job;
        if (current != null) {
            current.cancel();
        }
    }

    /**
     * Called from {@code Job.canceling()} so it does not call {@code Job.cancel()} again.
     */
    void cancelFromJob() {
        if (!cancelled.compareAndSet(false, true)) {
            return;
        }
        stopStreams();
    }

    /**
     * Waits until the job finishes.
     *
     * @throws InterruptedException when the wait is interrupted.
     */
    void join() throws InterruptedException {
        Job current = job;
        if (current != null) {
            current.join();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDone() {
        return done.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean wasCancelled() {
        return cancelled.get();
    }

    /**
     * Stops the graph iterator and the HTTP stream.
     */
    private void stopStreams() {
        AsyncGenerator.Cancellable<?> current = generator.get();
        if (current != null) {
            current.cancel(true);
        }
        StreamingHandle currentHandle = handle.getAndSet(null);
        if (currentHandle != null) {
            currentHandle.cancel();
        }
    }
}
