package com.developer.nefarious.zjoule.plugin.agent;

import java.util.List;
import java.util.Map;

import org.bsc.async.AsyncGenerator;
import org.bsc.langgraph4j.GraphInput;
import org.bsc.langgraph4j.GraphResult;
import org.bsc.langgraph4j.NodeOutput;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.streaming.StreamingOutput;
import org.eclipse.core.runtime.IProgressMonitor;

import dev.langchain4j.data.message.UserMessage;

/**
 * Runs one graph stream and reports it to the listener.
 */
final class ChatTurn {

    private ChatTurn() {
    }

    /**
     * Executes the turn. Failures and cancellation are reported to the listener.
     *
     * @param runtime the runtime.
     * @param run the run handle.
     * @param userText the user message. Ignored when resuming.
     * @param editorContext the editor text, or {@code null}.
     * @param resume whether this is a resume.
     * @param approved the resume decision.
     * @param monitor the job monitor.
     */
    static void execute(final ChatRuntime runtime, final ChatRun run, final String userText, final String editorContext,
            final boolean resume, final boolean approved, final IProgressMonitor monitor) {
        try {
            run.listener().onStart();
            GraphResult result = stream(runtime, run, userText, editorContext, resume, approved, monitor);
            finish(runtime, run, result);
            runtime.touch(run.threadId());
        } catch (Exception ex) {
            fail(runtime, run, ex);
        }
    }

    /**
     * Iterates the graph stream until it ends or the user cancels it.
     *
     * @param runtime the runtime.
     * @param run the run handle.
     * @param userText the user message.
     * @param editorContext the editor text.
     * @param resume whether this is a resume.
     * @param approved the resume decision.
     * @param monitor the job monitor.
     * @return the graph result.
     * @throws Exception when the stream fails.
     */
    private static GraphResult stream(final ChatRuntime runtime, final ChatRun run, final String userText,
            final String editorContext, final boolean resume, final boolean approved, final IProgressMonitor monitor)
            throws Exception {
        AsyncGenerator.Cancellable<NodeOutput<ChatState>> generator = runtime.graph()
                .stream(input(userText, resume, approved), config(run.threadId(), editorContext));
        run.attachGenerator(generator);
        for (NodeOutput<ChatState> output : generator) {
            if (stopped(run, monitor)) {
                generator.cancel(true);
                break;
            }
            emit(run, output);
        }
        return GraphResult.from(generator);
    }

    /**
     * Reports a normal end, an approval pause, or a cancellation.
     *
     * @param runtime the runtime.
     * @param run the run handle.
     * @param result the graph result. Cancellation is taken from the run, not the result.
     */
    private static void finish(final ChatRuntime runtime, final ChatRun run, final GraphResult result) {
        if (run.wasCancelled()) {
            run.listener().onCancelled();
            return;
        }
        if (result != null && result.isInterruptionMetadata()) {
            run.listener().onApprovalRequired(result.asInterruptionMetadata().nodeId());
            return;
        }
        run.listener().onComplete(runtime.lastAiText(run.threadId()));
    }

    /**
     * Reports a failure unless the user already cancelled the turn.
     *
     * @param runtime the runtime.
     * @param run the run handle.
     * @param error the failure.
     */
    private static void fail(final ChatRuntime runtime, final ChatRun run, final Exception error) {
        if (run.wasCancelled()) {
            run.listener().onCancelled();
        } else {
            run.listener().onError(error);
        }
        runtime.touch(run.threadId());
    }

    /**
     * Forwards a streaming chunk. End-of-stream markers are not tokens.
     *
     * @param run the run handle.
     * @param output the graph output.
     */
    private static void emit(final ChatRun run, final NodeOutput<ChatState> output) {
        if (output instanceof StreamingOutput<?> streaming && !streaming.isStreamingEnd()) {
            String chunk = streaming.chunk();
            if (chunk != null) {
                run.listener().onToken(chunk);
            }
        }
    }

    /**
     * Returns whether the job or the handle has been cancelled.
     *
     * @param run the run handle.
     * @param monitor the job monitor.
     * @return {@code true} when the loop should stop.
     */
    private static boolean stopped(final ChatRun run, final IProgressMonitor monitor) {
        return run.wasCancelled() || (monitor != null && monitor.isCanceled());
    }

    /**
     * Builds the graph input for a new message or a resume.
     *
     * @param userText the user message.
     * @param resume whether this is a resume.
     * @param approved the resume decision.
     * @return the graph input.
     */
    private static GraphInput input(final String userText, final boolean resume, final boolean approved) {
        if (resume) {
            String decision = approved ? ChatState.APPROVED : ChatState.REJECTED;
            return GraphInput.resume(Map.of(ChatState.APPROVAL, decision));
        }
        return GraphInput.args(Map.of(ChatState.MESSAGES, List.of(UserMessage.from(userText))));
    }

    /**
     * Builds the run config. Editor text stays in metadata and is not checkpointed.
     *
     * @param threadId the thread id.
     * @param editorContext the editor text, or {@code null} when there is no editor.
     * @return the config.
     */
    private static RunnableConfig config(final String threadId, final String editorContext) {
        RunnableConfig.Builder builder = RunnableConfig.builder().threadId(threadId);
        if (editorContext != null) {
            builder.putMetadata(PromptAssembler.META_EDITOR_CONTEXT, editorContext);
        }
        return builder.build();
    }
}
