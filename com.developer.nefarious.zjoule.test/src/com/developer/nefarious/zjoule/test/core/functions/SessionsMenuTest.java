package com.developer.nefarious.zjoule.test.core.functions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.developer.nefarious.zjoule.plugin.agent.persistence.SessionInfo;
import com.developer.nefarious.zjoule.plugin.core.functions.SessionsMenu;

public class SessionsMenuTest {

    @Test
    public void shouldKeepOnlyTheNewestFifteen() {
        List<SessionInfo> sessions = new ArrayList<>();
        int total = SessionsMenu.RECENT_LIMIT + 1;
        for (int index = 0; index < total; index++) {
            sessions.add(new SessionInfo("id-" + index, "title-" + index, index, index, "sap", "gpt-4o"));
        }

        List<SessionInfo> recent = SessionsMenu.recent(sessions);

        assertEquals(SessionsMenu.RECENT_LIMIT, recent.size());
        assertEquals("id-0", recent.get(0).threadId());
        assertEquals("id-" + (SessionsMenu.RECENT_LIMIT - 1), recent.get(SessionsMenu.RECENT_LIMIT - 1).threadId());
    }

    @Test
    public void shouldReturnAnEmptyListWhenThereAreNoSessions() {
        assertTrue(SessionsMenu.recent(null).isEmpty());
        assertTrue(SessionsMenu.recent(List.of()).isEmpty());
    }
}
