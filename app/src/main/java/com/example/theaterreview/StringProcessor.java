package com.example.theaterreview;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Locale;

public class StringProcessor {
    public ArrayList<String> original;
    public ArrayList<String> processed;

    StringProcessor(String str) {
        this.original = new ArrayList<>();
        this.processed = new ArrayList<>();

        StringBuilder current_original = new StringBuilder();
        StringBuilder current_processed = new StringBuilder();

        for(int idx = 0; idx < str.length(); idx++) {
            char current_char = str.charAt(idx);
            char next_char = 0;

            if(idx + 1 < str.length())
                next_char = str.charAt(idx + 1);

            current_original.append(current_char);

            if(is_char(current_char)) {
                current_processed.append(current_char);
            }

            if((!is_char(current_char) && is_char(next_char) && (current_processed.length() > 0)) || next_char == 0) {
                original.add(current_original.toString());
                processed.add(removeAccent(current_processed.toString().toLowerCase(Locale.ROOT)));
                current_original = new StringBuilder();
                current_processed = new StringBuilder();
            }
        }
    }

    private boolean is_char(char c) {
        String punctuation = "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~";
        String whitespace = " \t\n\r" + (char)0x0b + (char)0x0c;

        return punctuation.indexOf(c) == -1 && whitespace.indexOf(c) == -1;
    }

    private String removeAccent(String str) {
        str = Normalizer.normalize(str, Normalizer.Form.NFD);
        str = str.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        return str;
    }
}
