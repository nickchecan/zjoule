package com.developer.nefarious.zjoule.plugin.agent;

import static org.bsc.langgraph4j.GraphDefinition.END;
import static org.bsc.langgraph4j.GraphDefinition.START;
import static org.bsc.langgraph4j.action.AsyncNodeActionWithConfig.node_async;

import java.util.Map;
import java.util.function.Consumer;

import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.checkpoint.BaseCheckpointSaver;
import org.bsc.langgraph4j.langchain4j.generators.StreamingChatGenerator;

import com.developer.nefarious.zjoule.plugin.agent.model.ModelProvider;

import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.PartialResponse;
import dev.langchain4j.model.chat.response.PartialResponseContext;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.chat.response.StreamingHandle;

/**
 * Builds the v1 chat graph: START, call_model, END.
 */
public final class ChatGraphFactory {

    /** Node that calls the chat model. */
    public static final String CALL_MODEL = "call_model";

    /** Reserved for a later tools node. */
    public static final String TOOLS = "tools";

    /** Reserved for a later compact node. */
    public static final String COMPACT = "compact";

    /** State key LangGraph4j reads to embed a streaming generator. */
    static final String GENERATOR = "_generator";

    private ChatGraphFactory() {
    }

    /**
     * Compiles one graph for the plug-in instance.
     * <p>
     * {@code releaseThread(false)} is required. The default releases the thread
     * after each turn and drops the saved history.
     *
     * @param models the model resolved on each call.
     * @param prompt the prompt assembler.
     * @param saver the checkpoint saver.
     * @param handleSink receives the HTTP streaming handle so cancel can abort it.
     * @return the compiled graph.
     * @throws GraphStateException when the graph shape is invalid.
     */
    public static CompiledGraph<ChatState> create(final ModelProvider models, final PromptAssembler prompt,
            final BaseCheckpointSaver saver, final Consumer<StreamingHandle> handleSink) throws GraphStateException {
        StateGraph<ChatState> graph = new StateGraph<>(ChatState.SCHEMA, ChatState.serializer());
        graph.addNode(CALL_MODEL, node_async((state, config) -> callModel(models, prompt, handleSink, state, config)));
        graph.addEdge(START, CALL_MODEL);
        graph.addEdge(CALL_MODEL, END);
        CompileConfig compiled = CompileConfig.builder().checkpointSaver(saver).releaseThread(false).build();
        return graph.compile(compiled);
    }

    /**
     * Calls the model and returns the streaming generator for the graph to embed.
     *
     * @param models the model provider.
     * @param prompt the prompt assembler.
     * @param handleSink the cancel handle sink.
     * @param state the current state.
     * @param config the run config.
     * @return the generator update.
     */
    private static Map<String, Object> callModel(final ModelProvider models, final PromptAssembler prompt,
            final Consumer<StreamingHandle> handleSink, final ChatState state, final RunnableConfig config) {
        StreamingChatGenerator<ChatState> generator = StreamingChatGenerator.<ChatState>builder()
                .mapResult(response -> messagesUpdate(response))
                .startingNode(CALL_MODEL)
                .startingState(state)
                .build();
        ChatRequest request = ChatRequest.builder().messages(prompt.build(state, config)).build();
        models.streaming().chat(request, capturing(generator.handler(), handleSink));
        return Map.of(GENERATOR, generator);
    }

    /**
     * Maps the model response into a messages-channel update.
     *
     * @param response the completed response.
     * @return the state update.
     */
    private static Map<String, Object> messagesUpdate(final ChatResponse response) {
        return Map.of(ChatState.MESSAGES, response.aiMessage());
    }

    /**
     * Wraps the generator handler so the first partial response stores the HTTP handle.
     * Text is forwarded once, through the single-argument callback.
     *
     * @param delegate the generator handler.
     * @param handleSink the sink. May be null in tests.
     * @return the wrapping handler.
     */
    private static StreamingChatResponseHandler capturing(final StreamingChatResponseHandler delegate,
            final Consumer<StreamingHandle> handleSink) {
        return new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(final String token) {
                delegate.onPartialResponse(token);
            }

            @Override
            public void onPartialResponse(final PartialResponse partial, final PartialResponseContext context) {
                remember(handleSink, context);
                delegate.onPartialResponse(partial.text());
            }

            @Override
            public void onCompleteResponse(final ChatResponse response) {
                delegate.onCompleteResponse(response);
            }

            @Override
            public void onError(final Throwable error) {
                delegate.onError(error);
            }
        };
    }

    /**
     * Stores the streaming handle when the model provided one.
     *
     * @param handleSink the sink.
     * @param context the partial-response context.
     */
    private static void remember(final Consumer<StreamingHandle> handleSink, final PartialResponseContext context) {
        if (handleSink != null && context != null && context.streamingHandle() != null) {
            handleSink.accept(context.streamingHandle());
        }
    }
}
