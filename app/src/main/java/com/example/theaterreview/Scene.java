package com.example.theaterreview;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;

public class Scene {
    public static class Line {
        public String character;
        public String line;

        Line(String character, String line) {
            this.character = character;
            this.line = line;
        }
    }

    public ArrayList<Line> lines;
    public HashSet<String> characters;

    Scene(InputStreamReader reader) throws IOException {
        lines = new ArrayList<>();
        characters = new HashSet<>();

        BufferedReader buffer = new BufferedReader(reader);

        Line current_line = new Line("","");

        for (String line = buffer.readLine(); line != null; line = buffer.readLine()) {
            if(line.startsWith("#"))
                continue;

            if(line.charAt(0) == '=' && line.charAt(line.length()-1) == '=') {
                if(!current_line.character.equals("") && !current_line.line.equals("")) {
                    lines.add(current_line);
                    current_line = new Line("","");
                }

                current_line.character = line.substring(1, line.length()-1).toUpperCase(Locale.ROOT);
                characters.add(current_line.character);
            } else {
                current_line.line += "\n" + line;
            }
        }

        if(!current_line.character.equals("") && !current_line.line.equals("")) {
            lines.add(current_line);
        }

        buffer.close();
    }
}
