package com.developer.nefarious.zjoule.test.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bsc.langgraph4j.RunnableConfig;
import org.junit.jupiter.api.Test;

import com.developer.nefarious.zjoule.plugin.agent.ChatState;
import com.developer.nefarious.zjoule.plugin.agent.PromptAssembler;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;

public class PromptAssemblerTest {

    private static final int WINDOW_TWO = 2;

    private static final int WINDOW_THREE = 3;

    @Test
    public void shouldSkipAToolResultThatWouldOpenTheWindow() {
        PromptAssembler assembler = assembler(WINDOW_TWO);
        List<ChatMessage> prompt = assembler.build(state(history(), 0, "older turns"), config(null));

        assertEquals(WINDOW_TWO, prompt.size());
        assertInstanceOf(SystemMessage.class, prompt.get(0));
        assertInstanceOf(UserMessage.class, prompt.get(1));
        assertEquals("next", ((UserMessage) prompt.get(1)).singleText());
    }

    @Test
    public void shouldKeepAnAiToolRequestAlreadyInsideTheWindow() {
        PromptAssembler assembler = assembler(WINDOW_THREE);
        List<ChatMessage> prompt = assembler.build(state(history(), 0, ""), config(null));

        assertEquals(WINDOW_THREE + 1, prompt.size());
        assertInstanceOf(AiMessage.class, prompt.get(1));
        assertTrue(((AiMessage) prompt.get(1)).hasToolExecutionRequests());
        assertInstanceOf(ToolExecutionResultMessage.class, prompt.get(2));
        assertEquals("next", ((UserMessage) prompt.get(WINDOW_THREE)).singleText());
    }

    @Test
    public void shouldIncludeEditorContextOnlyWhenMetadataIsPresent() {
        PromptAssembler assembler = assembler(WINDOW_THREE);
        ChatState chat = state(history(), 0, "summary text");

        String withEditor = ((SystemMessage) assembler.build(chat, config("")).get(0)).text();
        String withoutEditor = ((SystemMessage) assembler.build(chat, config(null)).get(0)).text();

        assertTrue(withEditor.contains("instructions"));
        assertTrue(withEditor.contains(" Consider the following code as context: "));
        assertTrue(withEditor.contains("summary text"));
        assertFalse(withoutEditor.contains(" Consider the following code as context: "));
        assertTrue(withoutEditor.contains("summary text"));
    }

    private static PromptAssembler assembler(final int window) {
        return new PromptAssembler(() -> "instructions", () -> window);
    }

    private static List<ChatMessage> history() {
        ToolExecutionRequest request = ToolExecutionRequest.builder().id("1").name("lookup").arguments("{}").build();
        return List.of(UserMessage.from("first"), AiMessage.from(request), ToolExecutionResultMessage.from(request,
                "done"), UserMessage.from("next"));
    }

    private static ChatState state(final List<ChatMessage> messages, final int summarized, final String summary) {
        Map<String, Object> data = new HashMap<>();
        data.put(ChatState.MESSAGES, messages);
        data.put(ChatState.SUMMARY, summary);
        data.put(ChatState.SUMMARIZED_COUNT, summarized);
        return new ChatState(data);
    }

    private static RunnableConfig config(final String editor) {
        RunnableConfig.Builder builder = RunnableConfig.builder().threadId("t");
        if (editor != null) {
            builder.putMetadata(PromptAssembler.META_EDITOR_CONTEXT, editor);
        }
        return builder.build();
    }
}
