package com.developer.nefarious.zjoule.plugin.core.functions;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Display;

import com.developer.nefarious.zjoule.plugin.agent.ChatStreamListener;
import com.google.gson.Gson;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;

/**
 * Batches streaming tokens and applies them to the chat browser about every 50 ms.
 */
public final class BrowserStreamBridge implements ChatStreamListener {

    /** Delay between flushes of queued tokens. */
    public static final int FLUSH_MS = 50;

    private static final Gson GSON = new Gson();

    private static final String FAILED = "The chat request failed.";

    private final ScriptBrowser browser;

    private final UiScheduler scheduler;

    private final StringBuilder buffer = new StringBuilder();

    private final Object lock = new Object();

    private final AtomicBoolean flushScheduled = new AtomicBoolean(false);

    private final AtomicInteger epoch = new AtomicInteger(0);

    /**
     * Creates a bridge for the workbench browser.
     *
     * @param browser the chat browser.
     * @param display the UI display.
     */
    public BrowserStreamBridge(final Browser browser, final Display display) {
        this(new BrowserScripts(browser), new DisplayScheduler(display));
    }

    /**
     * Creates a bridge with test stand-ins.
     *
     * @param browser the script target.
     * @param scheduler the UI scheduler.
     */
    public BrowserStreamBridge(final ScriptBrowser browser, final UiScheduler scheduler) {
        this.browser = browser;
        this.scheduler = scheduler;
    }

    /**
     * Replays stored history. Tool-only messages are hidden.
     *
     * @param browser the chat browser.
     * @param history the checkpoint messages.
     */
    public static void render(final Browser browser, final List<ChatMessage> history) {
        render(new BrowserScripts(browser), history);
    }

    /**
     * Replays stored history through a script target.
     *
     * @param browser the script target.
     * @param history the checkpoint messages.
     */
    public static void render(final ScriptBrowser browser, final List<ChatMessage> history) {
        if (history == null) {
            return;
        }
        for (ChatMessage message : history) {
            renderOne(browser, message);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStart() {
        async("startBotMessage()");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onToken(final String chunk) {
        if (chunk == null) {
            return;
        }
        synchronized (lock) {
            buffer.append(chunk);
            scheduleFlush();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onComplete(final String fullText) {
        String tail = retire();
        scheduler.asyncExec(() -> finish(tail, fullText));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onApprovalRequired(final String toolSummaryJson) {
        async("showApproval(" + GSON.toJson(toolSummaryJson) + ")");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCancelled() {
        retire();
        async("markCancelled()");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onError(final Throwable error) {
        retire();
        async("showError(" + GSON.toJson(message(error)) + ")");
    }

    /**
     * Schedules one flush when none is already waiting.
     */
    private void scheduleFlush() {
        if (flushScheduled.compareAndSet(false, true)) {
            int scheduledEpoch = epoch.get();
            scheduler.asyncExec(() -> scheduler.timerExec(FLUSH_MS, () -> flush(scheduledEpoch)));
        }
    }

    /**
     * Sends the queued tokens when this flush is still the current epoch.
     *
     * @param scheduledEpoch the epoch captured when the timer was scheduled.
     */
    private void flush(final int scheduledEpoch) {
        String chunk = take(scheduledEpoch);
        if (chunk == null || chunk.isEmpty()) {
            return;
        }
        execute("appendBotChunk(" + GSON.toJson(chunk) + ")");
    }

    /**
     * Drains the buffer when the epoch still matches.
     *
     * @param scheduledEpoch the epoch captured when the timer was scheduled.
     * @return the text, or {@code null} when a newer turn retired the buffer.
     */
    private String take(final int scheduledEpoch) {
        synchronized (lock) {
            if (scheduledEpoch != epoch.get()) {
                return null;
            }
            return drain();
        }
    }

    /**
     * Drops any queued flush and returns text that has not been sent yet.
     *
     * @return the unsent text.
     */
    private String retire() {
        synchronized (lock) {
            epoch.incrementAndGet();
            return drain();
        }
    }

    /**
     * Empties the buffer. Caller holds {@link #lock}.
     *
     * @return the buffered text.
     */
    private String drain() {
        String chunk = buffer.toString();
        buffer.setLength(0);
        flushScheduled.set(false);
        return chunk;
    }

    /**
     * Appends a leftover tail and renders the final markdown.
     *
     * @param tail text that was not flushed.
     * @param fullText the assistant text.
     */
    private void finish(final String tail, final String fullText) {
        if (browser.isDisposed()) {
            return;
        }
        if (tail != null && !tail.isEmpty()) {
            browser.execute("appendBotChunk(" + GSON.toJson(tail) + ")");
        }
        browser.execute("finishBotMessage(" + GSON.toJson(fullText == null ? "" : fullText) + ")");
    }

    /**
     * Runs a script on the UI thread.
     *
     * @param script the script.
     */
    private void async(final String script) {
        scheduler.asyncExec(() -> execute(script));
    }

    /**
     * Runs a script when the browser is still open.
     *
     * @param script the script.
     */
    private void execute(final String script) {
        if (!browser.isDisposed()) {
            browser.execute(script);
        }
    }

    /**
     * Renders one stored message.
     *
     * @param browser the script target.
     * @param message the message.
     */
    private static void renderOne(final ScriptBrowser browser, final ChatMessage message) {
        if (browser.isDisposed()) {
            return;
        }
        if (message instanceof UserMessage user) {
            browser.execute("addUserMessage(" + GSON.toJson(user.singleText()) + ")");
            return;
        }
        if (message instanceof AiMessage ai && ai.text() != null && !ai.text().isBlank()) {
            browser.execute("addBotMessage(" + GSON.toJson(ai.text()) + ")");
        }
    }

    /**
     * Returns a message safe to show in the view.
     *
     * @param error the failure.
     * @return the message text.
     */
    private static String message(final Throwable error) {
        if (error == null || error.getMessage() == null || error.getMessage().isBlank()) {
            return FAILED;
        }
        return error.getMessage();
    }

    /**
     * Adapts an SWT browser.
     */
    private static final class BrowserScripts implements ScriptBrowser {

        private final Browser browser;

        private BrowserScripts(final Browser browser) {
            this.browser = browser;
        }

        @Override
        public boolean isDisposed() {
            return browser.isDisposed();
        }

        @Override
        public void execute(final String script) {
            browser.execute(script);
        }
    }

    /**
     * Adapts an SWT display.
     */
    private static final class DisplayScheduler implements UiScheduler {

        private final Display display;

        private DisplayScheduler(final Display display) {
            this.display = display;
        }

        @Override
        public void asyncExec(final Runnable runnable) {
            if (!display.isDisposed()) {
                display.asyncExec(runnable);
            }
        }

        @Override
        public void timerExec(final int milliseconds, final Runnable runnable) {
            if (!display.isDisposed()) {
                display.timerExec(milliseconds, runnable);
            }
        }
    }
}
