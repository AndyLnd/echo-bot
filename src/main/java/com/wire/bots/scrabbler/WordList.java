package com.wire.bots.scrabbler;

import com.wire.bots.sdk.Logger;
import java.io.File;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WordList {
  private Set<String> wordList;

  WordList() {
    try {
      File file = new File(getClass().getResource("sowpods.txt").getFile());
      Logger.info(file.toPath().toString());
      List<String> lines = Files.readAllLines(file.toPath());
      wordList = new HashSet<String>(lines);
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
