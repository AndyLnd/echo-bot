package com.wire.bots.scrabbler;

public class StringUtil {

  static public String unicode(int code) {
    return new String(Character.toChars(code));
  }

  static public String firstName(String name) {
    return name.substring(0, name.indexOf(" "));
  }

  static public String shmify(String string) {
    return string.toLowerCase().replaceFirst("[bcdfghjklmnpqrstvwxz]*", "Shm");
  }
}
