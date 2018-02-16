package com.wire.bots.scrabbler;

import com.wire.bots.sdk.Logger;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.models.TextMessage;
import com.wire.bots.sdk.server.model.User;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;

public class Game {
  static private String consonants = "bcdfghjklmnpqrstvwxyz";
  static private String vowels = "aeiou";
  static private int consonantCount = 6;
  static private int vowelCount = 3;
  private Map<String, Integer> scores = new HashMap<String, Integer>();
  private Set<String> guessedWords = new HashSet<String>();
  private Set<Character> letters = new HashSet<Character>();
  private String wordRegex;
  private WireClient client;
  private Timer timer;
  private Runnable closeGame;

  Game(WireClient client, Runnable closeGame) {
    this.client = client;
    this.closeGame = closeGame;
    this.timer = new Timer(client.getConversationId());
    Random rand = new Random();
    while (letters.size() < consonantCount) {
      letters.add(consonants.charAt(rand.nextInt(consonants.length())));
    }
    while (letters.size() < consonantCount + vowelCount) {
      letters.add(vowels.charAt(rand.nextInt(vowels.length())));
    }
    wordRegex = "(?i)^[" + String.valueOf(letters) + "]+$";
    String letterString = StringUtils.join(letters, " ").toUpperCase();
    sendText("Your letters:\n" + letterString);
    timer.schedule(new TimerTask() {
      @Override
      public void run() {
        sendText("20 seconds left!\n" + letterString);
      }
    }, TimeUnit.SECONDS.toMillis(10));
    timer.schedule(new TimerTask() {
      @Override
      public void run() {
        sendText("10 seconds left!\n" + letterString);
      }
    }, TimeUnit.SECONDS.toMillis(20));
    timer.schedule(new TimerTask() {
      @Override
      public void run() {
        endGame();
      }
    }, TimeUnit.SECONDS.toMillis(30));
  }

  public void handleInput(TextMessage msg) {
    String word = msg.getText().trim().toLowerCase();
    String user = msg.getUserId();
    Integer oldScore = scores.getOrDefault(user, 0);
    if (isValidWord(word)) {
      scores.put(user, oldScore + word.length());
      guessedWords.add(word);
      try {
        client.sendReaction(msg.getMessageId(), StringUtil.unicode(0x2764));
      } catch (Exception e) {
      }
    } else {
      scores.put(user, oldScore);
    }
  }

  private void sendText(String text) {
    try {
      client.sendText(text);
    } catch (Exception e) {
      e.printStackTrace();
      Logger.error(e.getMessage());
    }
  }

  private void endGame() {
    try {
      Collection<User> users = client.getUsers(new ArrayList<String>(scores.keySet()));
      List<User> userList = new ArrayList<User>(users);
      Collections.sort(userList, new Sorter());
      Integer highScore = Collections.max(scores.values());
      String scoreList = "";
      for (User user : userList) {
        Integer score = scores.get(user.id);
        scoreList += StringUtil.firstName(user.name) + ": " + (score == 0 ? StringUtil.unicode(0x1F4A9) : score)
            + (score == highScore && highScore != 0 ? " " + StringUtil.unicode(0x1F389) : "") + "\n";
      }
      sendText("Time's up!\nHere are the scores:\n" + scoreList);
    } catch (Exception e) {
      e.printStackTrace();
      Logger.error(e.getMessage());
    } finally {
      closeGame.run();
    }
  }

  private class Sorter implements Comparator<User> {
    public int compare(User user1, User user2) {
      return scores.get(user2.id) - scores.get(user1.id);
    }
  }


  private boolean isValidWord(String word) {
    return !guessedWords.contains(word) && word.matches(wordRegex) && WordList.hasWord(word);
  }
}
