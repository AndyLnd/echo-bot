//
// Wire
// Copyright (C) 2016 Wire Swiss GmbH
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see http://www.gnu.org/licenses/.
//

package com.wire.bots.scrabbler;

import com.wire.bots.sdk.Logger;
import com.wire.bots.sdk.MessageHandlerBase;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.models.*;
import com.wire.bots.sdk.server.model.Member;
import com.wire.bots.sdk.server.model.NewBot;
import com.wire.bots.sdk.server.model.User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;

public class MessageHandler extends MessageHandlerBase {
    private Map<String, Game> games = new HashMap<String, Game>();

    @Override
    public boolean onNewBot(NewBot newBot) {
        Logger.info(String.format("onNewBot: bot: %s, username: %s", newBot.id, newBot.origin.handle));

        for (Member member : newBot.conversation.members) {
            if (member.service != null) {
                Logger.warning("Rejecting NewBot. Provider: %s service: %s", member.service.provider,
                        member.service.id);
                return false; // we don't want to be in a conv if other bots are there.
            }
        }
        return true;
    }

    @Override
    public void onText(WireClient client, TextMessage msg) {
        try {
            String text = msg.getText().toLowerCase().replaceAll("[^ a-z]", "");
            String convId = client.getConversationId();
            if (games.containsKey(convId)) {
                if (msg.getText().equals("CHEATER!")) {
                    games.get(convId).blameCheater();
                } else {
                    games.get(convId).handleInput(msg);
                }
            } else if (text.equals("lets play") || text.equals("start game")) {
                Game newGame = new Game(client, () -> {
                    games.remove(convId);
                    Logger.info("Game %s ended.", convId);
                });
                games.put(convId, newGame);
            } else if (text.equals("help")) {
                sendInstructions(client);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error(e.getMessage());
        }
    }

    @Override
    public void onNewConversation(WireClient client) {
        Logger.info("onNewConversation: bot: %s, conv: %s", client.getId(), client.getConversationId());
        sendText(client, "Hi!");
        sendInstructions(client);
    }

    @Override
    public void onMemberJoin(WireClient client, ArrayList<String> userIds) {
        try {
            Collection<User> users = client.getUsers(userIds);
            for (User user : users) {
                Logger.info("onMemberJoin: bot: %s, user: %s/%s @%s", client.getId(), user.id, user.name, user.handle);

                // say Hi to new participant
                client.sendText("Hi there " + StringUtil.firstName(user.name));
                sendInstructions(client);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error(e.getMessage());
        }
    }

    private void sendText(WireClient client, String text) {
        try {
            client.sendText(text);
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error(e.getMessage());
        }
    }

    @Override
    public void onMemberLeave(WireClient client, ArrayList<String> userIds) {
        Logger.info("onMemberLeave: users: %s, bot: %s", userIds, client.getId());
    }

    @Override
    public void onBotRemoved(String botId) {
        Logger.info("Bot: %s got removed from the conversation :(", botId);
    }

    private void sendInstructions(WireClient client) {
        sendText(client,
                "I am the scrabble bot.\nIf you want to play, just say \"Let's play!\".\nI will give you 9 letters and you give me as many words as you can build from them in 30 seconds.");
    }
}
