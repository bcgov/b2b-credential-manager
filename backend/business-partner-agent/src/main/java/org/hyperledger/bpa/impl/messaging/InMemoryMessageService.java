/*
 * Copyright (c) 2020-2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hyperledger.bpa.impl.messaging;

import io.micronaut.context.annotation.Requires;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.scheduling.annotation.Scheduled;
import io.micronaut.websocket.WebSocketBroadcaster;
import io.micronaut.websocket.WebSocketSession;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.bpa.controller.api.WebSocketMessageBody;
import org.hyperledger.bpa.impl.util.Converter;
import org.hyperledger.bpa.repository.MessageQueueRepository;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Singleton
@Requires(missingProperty = "micronaut.session.http.redis.enabled")
public final class InMemoryMessageService implements MessageService {

    @Inject
    WebSocketBroadcaster broadcaster;

    @Inject
    @Getter
    MessageQueueRepository queue;

    @Inject
    @Getter
    Converter conv;

    private final Map<String, WebSocketSession> connected = new ConcurrentHashMap<>();

    public void subscribe(WebSocketSession session) {
        connected.put(session.getId(), session);
    }

    public void unsubscribe(WebSocketSession session) {
        connected.remove(session.getId());
    }

    public boolean hasConnectedSessions() {
        return CollectionUtils.isNotEmpty(connected);
    }

    public void send(WebSocketMessageBody message) {
        broadcaster.broadcastSync(message);
    }

    public Logger getLog() {
        return log;
    }

    @SuppressWarnings("unused")
    @Scheduled(fixedDelay = "1h", initialDelay = "2m")
    void cleanupStaleSessions() {
        log.debug("Cleaning up stale websocket sessions.");
        List<String> stale = new ArrayList<>();
        connected.forEach((k, v) -> {
            if (!v.isOpen()) {
                stale.add(k);
            }
        });
        log.debug("Found {} session(s), {} of them are stale.", connected.size(), stale.size());
        stale.forEach(connected::remove);
    }
}