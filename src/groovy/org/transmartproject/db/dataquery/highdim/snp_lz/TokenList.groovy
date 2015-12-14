package org.transmartproject.db.dataquery.highdim.snp_lz

import groovy.transform.CompileStatic

import java.io.IOException
import java.io.Reader
import java.util.InputMismatchException

/**
 * TokenList
 *
 * A wrapper around a StringBuilder, storing a sequence of characters,
 * and an array of end indices, that together encode a list of Strings:
 * element <var>i</var> of the list is stored in the StringBuilder
 * at positions <code>endIndexes[i-1]</code> till <code>endIndexes[i]</code>
 * (where <code>endIndexes[-1]</code> is 0 and <code>endIndexes</code> is 
 * undefined for indices &ge; <var>expectedSize</var>).
 * Empty values are supported.
 *
 * It comes with a {@link #parse(Reader)} function that parses a space-delimited
 * stream of values.
 */
@CompileStatic
class TokenList extends AbstractList<String> {

    private StringBuilder builder = new StringBuilder()
    private int size = 0
    private int expectedSize
    private int[] endIndexes

    /**
     * Initialises the TokenList. Sets the expected number of tokens.
     * The TokenList does not support more tokens. The parser function 
     * {@link #parse(Reader)} will throw an error if less tokens can be read.
     *
     * @param expectedSize the expected number of expected tokens.
     */
    public TokenList(int expectedSize) {
        this.expectedSize = expectedSize
        this.endIndexes = new int[expectedSize]
    }

    public int getExpectedSize() {
        expectedSize
    }

    /**
     * Reads a space-delimited stream of tokens from a Reader. If more or less than
     * <var>expectedSize</var> tokens are read, an {@link InputMismatchException}
     * is thrown.
     *
     * Empty tokens are supported: two consecutive spaces is interpreted as an empty token.
     * Empty input is interpreted as an empty list, not as a singleton list with an empty token.
     *
     * @param r the reader.
     */
    public void parse(Reader r) {
        int value
        char c
        while ((value = r.read()) > 0) {
            c = (char)value
            if (c == ' ') {
                commitToken()
            } else {
                addChar(c)
            }
        }
        commitTokenIfInputNotEmpty()
        if (this.size() < expectedSize) {
            throw new InputMismatchException("Expected " +
                    expectedSize + " tokens, but got only " +
                    String.valueOf(this.size()))
        }
    }

    /**
     * Append a character to the current token being read.
     *
     * @param c the character.
     */
    public void addChar(char c) {
        builder.append(c)
    }

    /**
     * Adds the currently read token to the token list.
     */
    public void commitToken() {
        if (size >= expectedSize) {
            throw new InputMismatchException(
                "Got more tokens than the " +
                        expectedSize + " expected")
        }
        endIndexes[size] = builder.length()
        size++
    }

    /**
     * Adds the currently read token to the list, if the input
     * was not empty (i.e., the list is not empty or the currently read
     * token is not empty).
     *
     * @return true iff a token was committed.
     */
    public boolean commitTokenIfInputNotEmpty() {
        if (builder.length() > 0) {
            commitToken()
            return true
        }
        false
    }

    @Override
    public String get(int i) {
        if (i < 0 || i >= size) {
            throw new IndexOutOfBoundsException()
        }
        int start = (i > 0) ? endIndexes[i-1] : 0
        int end = endIndexes[i]
        builder.substring(start, end)
    }

    @Override
    public int size() {
        size
    }

}
