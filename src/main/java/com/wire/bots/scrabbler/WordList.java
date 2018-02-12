package com.wire.bots.scrabbler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Set;

public class WordList {
  public Set<String> wordList = new HashSet<String>();

  WordList(String pathToList) {
    BufferedReader br = null;
    try {
      String filePath = new File("").getAbsolutePath();
      br = new BufferedReader(new FileReader(filePath + "/" + pathToList));
      String line = null;
      while ((line = br.readLine()) != null) {
        this.wordList.add(line.trim());
      }
    } catch (Exception e) {
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
