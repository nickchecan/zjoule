package com.developer.nefarious.zjoule.test.agent;

import static org.bsc.langgraph4j.GraphDefinition.END;
import static org.bsc.langgraph4j.GraphDefinition.START;
import static org.bsc.langgraph4j.action.AsyncNodeActionWithConfig.node_async;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphInput;
import org.bsc.langgraph4j.NodeOutput;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.serializer.StateSerializer;
import org.junit.jupiter.api.Test;

import com.developer.nefarious.zjoule.plugin.agent.ChatState;
import com.developer.nefarious.zjoule.plugin.agent.PromptAssembler;
import com.developer.nefarious.zjoule.plugin.agent.model.AiCoreTarget;
import com.developer.nefarious.zjoule.plugin.agent.persistence.StateLocationCheckpointSaver;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.http.client.jdk.JdkHttpClient;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

/**
 * Proves the libs bundle resolves and a checkpointer-backed graph runs in OSGi.
 */
public class LangGraphOsgiSmokeTest {

    private static final int TURNS = 3;

    private static final String THREAD = "thread-1";

    private static final String EDITOR = "CLASS zcl";

    private static final String PROMPT = "again";

    @Test
    public void shouldBuildClientsAndRunAGraphWithTheSaver() throws Exception {
        OpenAiStreamingChatModel openAi = OpenAiStreamingChatModel.builder()
                .baseUrl("http://127.0.0.1:9/v2/inference/deployments/dep")
                .modelName("gpt-4o")
                .customHeaders(() -> Map.of("Authorization", "Bearer token", "AI-Resource-Group", "rg"))
                .customQueryParams(Map.of("api-version", AiCoreTarget.API_VERSION))
                .httpClientBuilder(JdkHttpClient.builder())
                .build();
        OllamaStreamingChatModel ollama = OllamaStreamingChatModel.builder()
                .baseUrl("http://127.0.0.1:11434")
                .modelName("llama3")
                .httpClientBuilder(JdkHttpClient.builder())
                .build();
        assertNotNull(openAi);
        assertNotNull(ollama);

        Path directory = Files.createTempDirectory("zjoule-smoke");
        StateSerializer<ChatState> serializer = ChatState.serializer();
        StateLocationCheckpointSaver saver = new StateLocationCheckpointSaver(directory, serializer, 1);
        CompiledGraph<ChatState> graph = compile(serializer, saver);
        RunnableConfig config = RunnableConfig.builder().threadId(THREAD)
                .putMetadata(PromptAssembler.META_EDITOR_CONTEXT, EDITOR).build();

        for (int turn = 0; turn < TURNS; turn++) {
            drain(graph, config);
        }

        List<ChatMessage> messages = graph.lastStateOf(config).orElseThrow().state().messages();
        assertEquals(TURNS * 2, messages.size());
        int users = 0;
        for (ChatMessage message : messages) {
            if (message.type() == ChatMessageType.USER && PROMPT.equals(((UserMessage) message).singleText())) {
                users++;
            }
        }
        assertEquals(TURNS, users);
        String stored = Files.readString(directory.resolve(THREAD + ".json"));
        assertFalse(stored.contains(EDITOR));
        assertTrue(Files.exists(directory.resolve(THREAD + ".json")));
        assertFalse(Files.exists(directory.resolve(THREAD + ".json.tmp")));
    }

    private static CompiledGraph<ChatState> compile(final StateSerializer<ChatState> serializer,
            final StateLocationCheckpointSaver saver) throws Exception {
        StateGraph<ChatState> graph = new StateGraph<>(ChatState.SCHEMA, serializer);
        graph.addNode("call_model", node_async((state, config) -> Map.of(ChatState.MESSAGES,
                List.of(AiMessage.from("Hi")))));
        graph.addEdge(START, "call_model");
        graph.addEdge("call_model", END);
        CompileConfig compiled = CompileConfig.builder().checkpointSaver(saver).releaseThread(false).build();
        return graph.compile(compiled);
    }

    private static void drain(final CompiledGraph<ChatState> graph, final RunnableConfig config) throws Exception {
        Iterable<NodeOutput<ChatState>> stream = graph.stream(
                GraphInput.args(Map.of(ChatState.MESSAGES, List.of(UserMessage.from(PROMPT)))), config);
        int outputs = 0;
        for (NodeOutput<ChatState> output : stream) {
            if (output != null) {
                outputs++;
            }
        }
        assertTrue(outputs > 0);
    }
}
