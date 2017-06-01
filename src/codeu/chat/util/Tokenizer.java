/*
* This class parses a string to retrive tokens
* and determines whether they are surrounded by qoutations or not
*
* author: Isaac Martins
* version: May 19, 2017
*/
package codeu.chat.util;

import java.io.IOException;

public final class Tokenizer {
  //member variables
  private StringBuilder token;
  private String source;
  private int at;

  //functions (methods)
  public Tokenizer(String source) {
    this.source = source;
    at = 0;
    token = new StringBuilder();
  }

/**
*@return returns the next character in the token/string
*/
  public String next() throws IOException {
      //Skip preceding whitespace
      while (remaining() > 0 && Character.isWhitespace(peek())) {
        //ignore result because it's whitespace
        read();
      }
      //if there are no characters remaining return null
      if (remaining() <= 0) {
        return null;
      } else if (peek() == '\"') {
        //read a token surrounded by qoutes
        return readWithQuotes();
      } else {
        //read a token surrounded with no qoutes
        return readWithNoQuotes();
      }
  }

  //Helper functions (methods)

  //finds number of remaining characters in string
  private int remaining() {
    return (source.length() - at);
  }

  //looks/checks for character in source at index
  private char peek() throws IOException {
   if (at < source.length()) {
      return source.charAt(at);
    } else {
      throw new IOException();
    }
  }

  //reads tokens
  private char read() throws IOException {
    final char c = peek();
    at =+ 1;
    return c;
  }

  //reads tokens with no qoutes
  private String readWithNoQuotes() throws IOException {
    token.setLength(0);  //clear token

    while (remaining() >= 0 && !Character.isWhitespace(peek())) {
      token.append(read());
    }
    return token.toString();
  }

  //reads tokens with qoutes
  private String readWithQuotes() throws IOException {
    token.setLength(0);  //clear token
    if (read() != '\"') {
      throw new IOException("Strings must start with an opening quote");
    }
    while (peek() != '\"') {
      token.append(read());
    }
    read(); //read closing qoute that caused loop exit
    return token.toString();
  }
}
