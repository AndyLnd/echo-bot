package com.wire.bots.scrabbler;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class Cheaters {
  static private long cooldownMinutes = 10;
  private Map<String, LocalDateTime> cheaters = new HashMap<String, LocalDateTime>();

  public void add(String userId) {
    cheaters.put(userId, LocalDateTime.now());
  }

  public boolean isCheater(String userId) {
    if (cheaters.containsKey(userId)) {
      LocalDateTime time = cheaters.get(userId);
      if (time.plusMinutes(cooldownMinutes).isAfter(LocalDateTime.now())) {
        cheaters.remove(userId);
        return false;
      }
      return true;
    }
    return false;
  }
}
