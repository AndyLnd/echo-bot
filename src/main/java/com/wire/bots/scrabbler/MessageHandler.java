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
    private WordList wordList = new WordList();
    private Set<Character> letters = new HashSet<Character>();
    private String wordRegex;
    private int consonantCount = 6;
    private int vowelCount = 3;
    private String consonants = "bcdfghjklmnpqrstvwxyz";
    private String vowels = "aeiou";
    private Random rand = new Random();

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
        try {
            String text = msg.getText().toLowerCase().replaceAll("[^ a-z]", "");
            if (isGameRunning) {
                if (handleInput(msg)) {
                    client.sendReaction(msg.getMessageId(), unicode(0x2764));
                }
            } else if (text.equals("lets play") || text.equals("start game")) {
                startGame(client);
            } else if (text.equals("help")) {
                sendInstructions(client);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startGame(WireClient client) {
        isGameRunning = true;
        scores.clear();
        guessedWords.clear();
        letters.clear();
        while (letters.size() < consonantCount) {
            letters.add(consonants.charAt(rand.nextInt(consonants.length())));
        }
        while (letters.size() < consonantCount + vowelCount) {
            letters.add(vowels.charAt(rand.nextInt(vowels.length())));
        }

        wordRegex = "(?i)^[" + String.valueOf(letters) + "]+$";
        String letterString = StringUtils.join(letters, " ").toUpperCase();
        sendText(client, "Your letters:\n" + letterString);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                sendText(client, "20 seconds left!\n" + letterString);
            }
        }, TimeUnit.SECONDS.toMillis(10));
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                sendText(client, "10 seconds left!\n" + letterString);
            }
        }, TimeUnit.SECONDS.toMillis(20));
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                endGame(client);
            }
        }, TimeUnit.SECONDS.toMillis(30));
    }

    private boolean handleInput(TextMessage msg) {
        String word = msg.getText().trim().toLowerCase();
        String user = msg.getUserId();
        Integer oldScore = scores.getOrDefault(user, 0);
        if (isValidWord(word)) {
            scores.put(user, oldScore + word.length());
            guessedWords.add(word);
            return true;
        } else {
            scores.put(user, oldScore);
            return false;
        }
    }

    private void endGame(WireClient client) {
        isGameRunning = false;

        try {
            Collection<User> users = client.getUsers(new ArrayList<String>(scores.keySet()));
            List<User> userList = new ArrayList<User>(users);
            Collections.sort(userList, new Sorter());
            Integer highScore = Collections.max(scores.values());
            String scoreList = "";
            for (User user : userList) {
                Integer score = scores.get(user.id);
                scoreList += user.name + ": " + (score == 0 ? unicode(0x1F4A9) : score)
                        + (score == highScore && highScore != 0 ? " " + unicode(0x1F389) : "") + "\n";
            }
            sendText(client, "Time's up!\nHere are the scores:\n" + scoreList);
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error(e.getMessage());
        }
    }

    private String unicode(int code) {
        return new String(Character.toChars(code));
    }

    private boolean isValidWord(String word) {
        return !guessedWords.contains(word) && word.matches(wordRegex) && wordList.hasWord(word);
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
                client.sendText("Hi there " + user.name);
                sendInstructions(client);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error(e.getMessage());
        }
    }

    private void sendInstructions(WireClient client) {
        sendText(client,
                "I am the scrabble bot.\nIf you want to play, just say \"Let's play!\".\nI will give you 9 letters and you give me as many words as you can build from them in 30 seconds.");
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
            return scores.get(user2.id) - scores.get(user1.id);
        }
    }
}
