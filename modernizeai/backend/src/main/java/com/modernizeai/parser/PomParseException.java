package com.modernizeai.parser;

/** A POM could not be parsed. The message is safe to show to users. */
public class PomParseException extends Exception {
    private final Integer line;

    public PomParseException(String message, Integer line) {
        super(message);
        this.line = line;
    }

    public Integer line() {
        return line;
    }
}
