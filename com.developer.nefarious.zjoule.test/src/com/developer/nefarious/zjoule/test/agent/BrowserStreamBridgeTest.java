package com.developer.nefarious.zjoule.test.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.developer.nefarious.zjoule.plugin.agent.ChatState;
import com.developer.nefarious.zjoule.plugin.core.functions.BrowserStreamBridge;
import com.developer.nefarious.zjoule.plugin.core.functions.ScriptBrowser;
import com.developer.nefarious.zjoule.plugin.core.functions.UiScheduler;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;

public class BrowserStreamBridgeTest {

    @Test
    public void shouldBatchTokensIntoOneFlush() {
        ScriptRecorder browser = new ScriptRecorder();
        TimerRecorder scheduler = new TimerRecorder();
        BrowserStreamBridge bridge = new BrowserStreamBridge(browser, scheduler);

        bridge.onToken("a");
        bridge.onToken("b");
        bridge.onToken("c");

        assertEquals(1, scheduler.delays.size());
        assertEquals(BrowserStreamBridge.FLUSH_MS, scheduler.delays.get(0));
        assertTrue(browser.scripts.isEmpty());

        scheduler.timers.get(0).run();

        assertEquals(1, browser.scripts.size());
        assertEquals("appendBotChunk(\"abc\")", browser.scripts.get(0));

        bridge.onToken("d");
        assertEquals(2, scheduler.delays.size());
    }

    @Test
    public void shouldIgnoreAFlushThatWasQueuedBeforeCompletion() {
        ScriptRecorder browser = new ScriptRecorder();
        TimerRecorder scheduler = new TimerRecorder();
        BrowserStreamBridge bridge = new BrowserStreamBridge(browser, scheduler);

        bridge.onToken("tail");
        bridge.onComplete("full");
        scheduler.timers.get(0).run();

        assertEquals(2, browser.scripts.size());
        assertEquals("appendBotChunk(\"tail\")", browser.scripts.get(0));
        assertEquals("finishBotMessage(\"full\")", browser.scripts.get(1));
    }

    @Test
    public void shouldSkipScriptsWhenTheBrowserIsDisposed() {
        ScriptRecorder browser = new ScriptRecorder();
        browser.disposed = true;
        TimerRecorder scheduler = new TimerRecorder();
        BrowserStreamBridge bridge = new BrowserStreamBridge(browser, scheduler);

        bridge.onStart();
        bridge.onToken("a");
        scheduler.timers.get(0).run();

        assertTrue(browser.scripts.isEmpty());
    }

    @Test
    public void shouldHideToolOnlyMessagesWhenReplaying() {
        ScriptRecorder browser = new ScriptRecorder();
        ToolExecutionRequest request = ToolExecutionRequest.builder().id("1").name("lookup").arguments("{}").build();

        BrowserStreamBridge.render(browser, List.of(UserMessage.from("hi"), AiMessage.from(request),
                ToolExecutionResultMessage.from(request, "done"), AiMessage.from("answer")));

        assertEquals(2, browser.scripts.size());
        assertEquals("addUserMessage(\"hi\")", browser.scripts.get(0));
        assertEquals("addBotMessage(\"answer\")", browser.scripts.get(1));
        assertTrue(browser.scripts.stream().noneMatch(script -> script.contains(ChatState.APPROVAL)));
    }

    private static final class ScriptRecorder implements ScriptBrowser {

        private final List<String> scripts = new ArrayList<>();

        private boolean disposed;

        @Override
        public boolean isDisposed() {
            return disposed;
        }

        @Override
        public void execute(final String script) {
            scripts.add(script);
        }
    }

    private static final class TimerRecorder implements UiScheduler {

        private final List<Integer> delays = new ArrayList<>();

        private final List<Runnable> timers = new ArrayList<>();

        @Override
        public void asyncExec(final Runnable runnable) {
            runnable.run();
        }

        @Override
        public void timerExec(final int milliseconds, final Runnable runnable) {
            delays.add(milliseconds);
            timers.add(runnable);
        }
    }
}
