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

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;

public class MessageHandler extends MessageHandlerBase {
    private final String dataDir;
    private boolean isGameRunning;
    private int secretNumber;
    private int guessCount;

    MessageHandler(String dataDir) {
        this.dataDir = dataDir;
    }

    /**
     * @param newBot Initialization object for new Bot instance
     *               -  id          : The unique user ID for the bot.
     *               -  client      : The client ID for the bot.
     *               -  origin      : The profile of the user who requested the bot, as it is returned from GET /bot/users.
     *               -  conversation: The conversation as seen by the bot and as returned from GET /bot/conversation.
     *               -  token       : The bearer token that the bot must use on inbound requests.
     *               -  locale      : The preferred locale for the bot to use, in form of an IETF language tag.
     * @return If TRUE is returned new bot instance is created for this conversation
     * If FALSE is returned this service declines to create new bot instance for this conversation
     */
    @Override
    public boolean onNewBot(NewBot newBot) {
        Logger.info(String.format("onNewBot: bot: %s, username: %s",
                newBot.id,
                newBot.origin.handle));

        for (Member member : newBot.conversation.members) {
            if (member.service != null) {
                Logger.warning("Rejecting NewBot. Provider: %s service: %s",
                        member.service.provider,
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
            switch(text) {
                case "lets play":
                case "start game":
                    if(!this.isGameRunning){
                        this.startGame();
                        client.sendText("Alright! I chose a number from 1 to 100. Try to guess it!");
                    } else {
                        client.sendText("Let's finish this one first, okay?");
                    }
                    break;
                default:
                    try {
                        int guess = Integer.parseInt(msg.getText(), 10);
                        this.guessCount++;
                        if (guess > this.secretNumber) {
                            client.sendText("Too high!");
                        } else if (guess < this.secretNumber) {
                            client.sendText("Too low!");
                        } else {
                            client.sendText("Correct! It took you only " + this.guessCount + " guesses.");
                            this.endGame();
                        }
                    } catch (NumberFormatException e) {
                        client.sendText("Please enter a number.");
                    }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startGame() {
        this.isGameRunning = true;
        this.guessCount = 0;
        this.secretNumber = (int) Math.ceil(Math.random() * 100);
    }

    private void endGame() {
        this.isGameRunning = false;
    }

    @Override
    public void onNewConversation(WireClient client) {
        try {
            Logger.info("onNewConversation: bot: %s, conv: %s",
                    client.getId(),
                    client.getConversationId());

            String label = "Hi! Want to play a Game? Just say \"Let's play!\"";
            client.sendText(label);
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error(e.getMessage());
        }
    }

    @Override
    public void onMemberJoin(WireClient client, ArrayList<String> userIds) {
        try {
            Collection<User> users = client.getUsers(userIds);
            for (User user : users) {
                Logger.info("onMemberJoin: bot: %s, user: %s/%s @%s",
                        client.getId(),
                        user.id,
                        user.name,
                        user.handle
                );

                // say Hi to new participant
                client.sendText("Hi there " + user.name);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error(e.getMessage());
        }
    }

    @Override
    public void onMemberLeave(WireClient client, ArrayList<String> userIds) {
        Logger.info("onMemberLeave: users: %s, bot: %s",
                userIds,
                client.getId());
    }

    @Override
    public void onBotRemoved(String botId) {
        Logger.info("Bot: %s got removed from the conversation :(", botId);
    }
}
