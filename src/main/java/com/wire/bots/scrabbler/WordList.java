package com.wire.bots.scrabbler;

import com.wire.bots.sdk.Logger;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Set;

public class WordList {
  public Set<String> wordList = new HashSet<String>();

  WordList(String pathToList) {
    BufferedReader br = null;
    try {
      String filePath = WordList.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
      filePath = filePath.substring(0, filePath.lastIndexOf("/"));
      br = new BufferedReader(new FileReader(filePath + "/" + pathToList));
      String line = br.readLine();
      while (line != null) {
        this.wordList.add(line.trim());
        line = br.readLine();
      }
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
