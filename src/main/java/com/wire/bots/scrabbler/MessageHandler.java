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

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Random;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class MessageHandler extends MessageHandlerBase {
    private boolean isGameRunning;
    private Map<String, Integer> scores = new HashMap<String, Integer>();
    private Set<String> guessedWords = new HashSet<String>();
    private Timer timer = new Timer("GameTimer");
    private WordList wordList = new WordList("sowpods.txt");
    private Set<Character> chars = new HashSet<Character>();
    private String wordRegex;
    private int charCount = 8;

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
        Logger.info("wordList.size():" + this.wordList.wordList.size());
        try {
            String text = msg.getText().toLowerCase().replaceAll("[^ a-z]", "");
            if (this.isGameRunning) {
                this.handleInput(msg);
            } else if (text.equals("lets play") || text.equals("start game")) {
                this.startGame(client);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startGame(WireClient client) {
        this.isGameRunning = true;
        this.scores.clear();
        this.guessedWords.clear();
        this.chars.clear();
        String abc = "abcdefghijklmnopqrstuvwxyz";
        Random r = new Random();
        while (this.chars.size() < this.charCount) {
            this.chars.add(abc.charAt(r.nextInt(abc.length())));
        }
        this.wordRegex = "(?i)^[" + String.valueOf(this.chars) + "]+$";
        // this.sendText(client, "Your letters:");
        this.sendText(client, StringUtils.join(this.chars, " ").toUpperCase());
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                endGame(client);
            }
        }, TimeUnit.SECONDS.toMillis(10));
    }

    private void handleInput(TextMessage msg) {
        String word = msg.getText().trim().toLowerCase();
        String user = msg.getUserId();
        Logger.info("handleInput - word: %s user: %s", word, user);
        if (this.isValidWord(word)) {
            Logger.info("isValid");
            Integer oldScore = this.scores.get(user);
            if (oldScore == null) {
                oldScore = 0;
            }
            this.scores.put(user, oldScore + 1);
            this.guessedWords.add(word);
        }
    }

    private void endGame(WireClient client) {
        this.isGameRunning = false;
        this.sendText(client, "Time's up!");
        this.sendText(client, "Here are the scores:");
        try {
            Collection<User> users = client.getUsers(new ArrayList<String>(scores.keySet()));
            List<User> userList = new ArrayList<User>(users);
            Collections.sort(userList, new Sorter());
            for (User user : userList) {
                this.sendText(client, user.name + ": " + this.scores.get(user.id));
            }
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error(e.getMessage());
        }
    }

    private boolean isValidWord(String word) {
        return !this.guessedWords.contains(word) && word.matches(this.wordRegex) && this.wordList.hasWord(word);
    }

    @Override
    public void onNewConversation(WireClient client) {
        Logger.info("onNewConversation: bot: %s, conv: %s", client.getId(), client.getConversationId());
        Logger.info("wordList.size():" + this.wordList.wordList.size());
        this.sendText(client, "Hi! Want to play a Game? Just say \"Let's play!\"");
    }

    @Override
    public void onMemberJoin(WireClient client, ArrayList<String> userIds) {
        try {
            Collection<User> users = client.getUsers(userIds);
            for (User user : users) {
                Logger.info("onMemberJoin: bot: %s, user: %s/%s @%s", client.getId(), user.id, user.name, user.handle);

                // say Hi to new participant
                client.sendText("Hi there " + user.name);
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

    public class Sorter implements Comparator<User> {
        public int compare(User user1, User user2) {
            return scores.get(user1.id) - scores.get(user2.id);
        }
    }
}
