package com.wire.bots.scrabbler;

import com.wire.bots.sdk.Logger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.stream.Collectors;

public class WordList {
  private Set<String> wordList;

  WordList() {
      try (InputStream input = getClass().getResourceAsStream("sowpods.txt")) {
        wordList = new BufferedReader(new InputStreamReader(input)).lines().collect(Collectors.toSet());
        Logger.info("wordlist loaded: " + wordList.size());
      } catch (Exception e) {
        e.printStackTrace();
        Logger.error(e.getMessage());
      }
  }

  public boolean hasWord(String word) {
    return wordList.contains(word);
  }
}
