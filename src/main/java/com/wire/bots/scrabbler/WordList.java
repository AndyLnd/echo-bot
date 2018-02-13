package com.wire.bots.scrabbler;

import com.wire.bots.sdk.Logger;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WordList {
  private Set<String> wordList;

  WordList() {
    try {
      URI u = getClass().getResource("sowpods.txt").toURI();
      Logger.info(new File(u).getPath());
      List<String> lines = Files.readAllLines(Paths.get(new File(u).getPath()));
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
