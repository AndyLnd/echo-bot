package com.wire.bots.scrabbler;

import com.wire.bots.sdk.Logger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WordList {
  private Set<String> wordList;

  WordList(String pathToList) {

    try {
      String filePath = WordList.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
      filePath = filePath.substring(0, filePath.lastIndexOf("/"));
      List<String> lines = Files.readAllLines(Paths.get(filePath, pathToList));
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
