package com.wire.bots.scrabbler;

import com.wire.bots.sdk.Logger;
import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WordList {
  public Set<String> wordList;

  WordList(String pathToList) {
    BufferedReader br = null;
    try {
      String filePath = WordList.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
      filePath = filePath.substring(0, filePath.lastIndexOf("/"));
      List<String> lines = Files.readAllLines(Paths.get(filePath, pathToList));
      this.wordList = new HashSet<String>(lines);
      Logger.info("wordlist loaded: " + this.wordList.size());
    } catch (Exception e) {
      e.printStackTrace();
      Logger.error(e.getMessage());
    } finally {
      if (br != null) {
        try {
          br.close();
        } catch (Exception e) {
        }
      }
    }
  }

  public boolean hasWord(String word) {
    return this.wordList.contains(word);
  }
}
