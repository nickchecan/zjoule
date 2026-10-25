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

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;

public class PromptAssemblerTest {

    private static final int HISTORY_SIZE = 4;

    @Test
    public void shouldSendTheSummaryAndEveryRemainingMessage() {
        PromptAssembler assembler = new PromptAssembler(() -> "instructions");
        List<ChatMessage> prompt = assembler.build(state(history(), "older turns"), config(null));

        assertEquals(HISTORY_SIZE + 1, prompt.size());
        assertInstanceOf(SystemMessage.class, prompt.get(0));
        assertTrue(((SystemMessage) prompt.get(0)).text().contains("older turns"));
        assertEquals("first", ((UserMessage) prompt.get(1)).singleText());
        assertEquals("next", ((UserMessage) prompt.get(HISTORY_SIZE)).singleText());
    }

    @Test
    public void shouldIncludeEditorContextOnlyWhenMetadataIsPresent() {
        PromptAssembler assembler = new PromptAssembler(() -> "instructions");
        ChatState chat = state(history(), "summary text");

        String withEditor = ((SystemMessage) assembler.build(chat, config("")).get(0)).text();
        String withoutEditor = ((SystemMessage) assembler.build(chat, config(null)).get(0)).text();

        assertTrue(withEditor.contains("instructions"));
        assertTrue(withEditor.contains(" Consider the following code as context: "));
        assertTrue(withEditor.contains("summary text"));
        assertFalse(withoutEditor.contains(" Consider the following code as context: "));
        assertTrue(withoutEditor.contains("summary text"));
    }

    private static List<ChatMessage> history() {
        return List.of(UserMessage.from("first"), AiMessage.from("reply"), UserMessage.from("again"),
                UserMessage.from("next"));
    }

    private static ChatState state(final List<ChatMessage> messages, final String summary) {
        Map<String, Object> data = new HashMap<>();
        data.put(ChatState.MESSAGES, messages);
        data.put(ChatState.SUMMARY, summary);
        data.put(ChatState.SUMMARIZED_COUNT, 0);
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
