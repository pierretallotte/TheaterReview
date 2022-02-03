package com.example.theaterreview;

/*
    This is a partial reimplementation of the Python difflib.SequenceMatcher class:
    https://github.com/python/cpython/blob/3.10/Lib/difflib.py

    Only array of String are supported.
 */

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/*
    SequenceMatcher class compares pairs of sequence of String (called A and B in the comments)
 */
public class SequenceMatcher {
    /*
        An opcode explains how to turn a part of the sequence A to B

         For instance:
        A = ["This", "is", "true"]
        B = ["Wrong"]

        The corresponding opcode is:
            - Tag: REPLACE
            - aBegin: 0
            - aEnd: 2
            - bBegin: 0
            - bEnd: 0
     */
    public static class Opcode {
        /*
            The Tag says what operation to do to turn A to B
         */
        public enum Tag {
            REPLACE,
            DELETE,
            INSERT,
            EQUAL
        }

        // Operation to perform
        public Tag tag;

        // Index in A of the beginning of the sequence
        public int aBegin;

        // Index in A of the end of the sequence
        public int aEnd;

        // Index in B of the beginning of the sequence
        public int bBegin;

        // Index in B of the end of the sequence
        public int bEnd;

        // Construct a new Opcode
        Opcode(Tag tag, int aBegin, int aEnd, int bBegin, int bEnd) {
            this.tag = tag;
            this.aBegin = aBegin;
            this.aEnd = aEnd;
            this.bBegin = bBegin;
            this.bEnd = bEnd;
        }
    }

    /*
        A Block represents a pair of sequence in A and B

        For instance, let's consider A and B as:
            - A: ["This", "is", "the", "first", "sequence"]
            - B: ["This", "is", "another", "sequence"]

        A Block can represent the following pair of sequence:
            - A: ["the", "first"]
            - B: ["another"]
        with the following values:
            - aBegin = 2
            - aEnd = 3
            - bBegin = 2
            - bEnd = 2
     */
    public static class Block {
        // Index in A of the beginning of the sequence
        public int aBegin;

        // Index in A of the end of the sequence
        public int aEnd;

        // Index in B of the beginning of the sequence
        public int bBegin;

        // Index in B of the end of the sequence
        public int bEnd;

        // Construct a new Block
        Block(int aBegin, int aEnd, int bBegin, int bEnd) {
            this.aBegin = aBegin;
            this.aEnd = aEnd;
            this.bBegin = bBegin;
            this.bEnd = bEnd;
        }
    }

    /*
        A Match represents two identical pair of sequence in A and B

        For instance, let's consider A and B as:
            - A: ["This", "is", "the", "first", "sequence"]
            - B: ["This", "is", "another", "sequence"]

        We have two matches that can be represented with the following values:
            - Match 1:
                - aBegin: 0
                - bBegin: 0
                - size: 2
            - Match 2:
                - aBegin: 4
                - bBegin: 3
                - size: 1
     */
    public static class Match implements Comparable<Match> {
        // Index in A of the beginning of the sequence
        public int aBegin;

        // Index in B of the beginning of the sequence
        public int bBegin;

        // Size of the match
        public int size;

        // Construct a new Match
        Match(int aBegin, int bBegin, int size) {
            this.aBegin = aBegin;
            this.bBegin = bBegin;
            this.size = size;
        }

        /*
            Compare two Match instances
            The comparison is done in the following order: aBegin, bBegin, size
            Returns a negative number if the Match is lower, 0 if both are equals and a positive
            number if the Match is higher
         */
        public int compareTo(Match other) {
            int diff = Integer.compare(this.aBegin, other.aBegin);
            if(diff == 0) {
                diff = Integer.compare(this.bBegin, other.bBegin);

                if(diff == 0) {
                    diff = Integer.compare(this.size, other.size);
                }
            }

            return diff;
        }
    }

    // Sequence of String A
    private ArrayList<String> a;

    // Sequence of String B
    private ArrayList<String> b;

    /*
        b2j represents the indices of the elements of B
        The key corresponds to a String in B.
        The value corresponds to the list of the indices in B.

        For instance, let's consider B as ["to", "be", "or", "not", "to", "be"]
        b2j will represents the sequence that way:
            - b2j["to"] = [0, 4]
            - b2j["be"] = [1, 5]
            - b2j["or"] = [2]
            - b2j["not"] = [3]
     */
    private HashMap<String, ArrayList<Integer>> b2j;

    // List of identical sequences in A and B
    private ArrayList<Match> matchingBlocks;

    // List of operations to turn A in B
    private ArrayList<Opcode> opcodes;

    // Construct a new SequenceMatcher to compare a to b
    SequenceMatcher(ArrayList<String> a, ArrayList<String> b) {
        this.setSequences(a, b);
    }

    // Set the sequences A and B
    public void setSequences(ArrayList<String> a, ArrayList<String> b) {
        this.setA(a);
        this.setB(b);
    }

    // Set the sequence A
    public void setA(ArrayList<String> a) {
        if(a.equals(this.a))
            return;
        this.a = a;
        this.matchingBlocks = new ArrayList<>();
        this.opcodes = new ArrayList<>();
    }

    // Set the sequence B
    public void setB(ArrayList<String> b) {
        if(b.equals(this.b))
            return;
        this.b = b;
        this.matchingBlocks = new ArrayList<>();
        this.opcodes = new ArrayList<>();
        this.resetB2J();
    }

    // Reset B2J. Should be called only by setB
    private void resetB2J() {
        ArrayList<String> b = this.b;
        this.b2j = new HashMap<>();

        for(int idx = 0; idx < b.size(); idx++) {
            ArrayList<Integer> indices = this.b2j.get(b.get(idx));
            if(indices == null) {
                indices = new ArrayList<>();
                indices.add(idx);
                b2j.put(b.get(idx), indices);
            } else {
                indices.add(idx);
            }
        }
    }

    /*
        Find the longest match between the given indexes of A and B and returns the corresponding
        Match object
     */
    public Match findLongestMatch(int aBegin, int aEnd, int bBegin, int bEnd) {
        int bestI = aBegin;
        int bestJ = bBegin;
        int bestSize = 0;

        HashMap<Integer, Integer> j2len = new HashMap<>();

        // Iterate through A to find the longest match in B
        for(int i = aBegin; i < aEnd; i++) {
            HashMap<Integer, Integer> newJ2Len = new HashMap<>();

            String key = a.get(i);
            if(key != null) {
                ArrayList<Integer> indices = this.b2j.get(key);
                if (indices != null) {
                    for (Integer j : indices) {
                        if (j < bBegin)
                            continue;
                        if (j >= bEnd)
                            break;

                        Integer k = j2len.get(j - 1);
                        if (k == null)
                            k = 0;
                        k++;
                        newJ2Len.put(j, k);

                        if (k > bestSize) {
                            bestI = i - k + 1;
                            bestJ = j - k + 1;
                            bestSize = k;
                        }
                    }
                }
            }
            j2len = newJ2Len;
        }

        // I don't know if the two following loops are relevant when junk elements are not taken
        // into account. I kept it from the Python implementation just in case
        while(bestI > aBegin && bestJ > bBegin && a.get(bestI-1).equals(b.get(bestJ-1))) {
            bestI--;
            bestJ--;
            bestSize++;
        }

        while(bestI + bestSize < aEnd && bestJ + bestSize < bEnd && a.get(bestI+bestSize).equals(b.get(bestJ+bestSize))) {
            bestSize++;
        }

        return new Match(bestI, bestJ, bestSize);
    }

    /*
        Find all the matching blocks between A and B and returns the corresponding list of Match
        objects
        The last Match is always:
            - aBegin = last index of A
            - bBegin = last index of B
            - size = 0
     */
    public ArrayList<Match> getMatchingBlocks() {
        if(!this.matchingBlocks.isEmpty()) {
            return this.matchingBlocks;
        }

        int aLength = this.a.size();
        int bLength = this.b.size();

        ArrayDeque<Block> queue = new ArrayDeque<>();
        queue.add(new Block(0, aLength, 0, bLength));

        // Find the longest match by comparing the full pair of sequences
        // Then find the longest match in the remaining parts of the sequences (before and after)
        while(!queue.isEmpty()) {
            Block currentBlock = queue.pop();
            Match longestMatch = this.findLongestMatch(currentBlock.aBegin, currentBlock.aEnd, currentBlock.bBegin, currentBlock.bEnd);

            if(longestMatch.size > 0) {
                matchingBlocks.add(longestMatch);

                if(currentBlock.aBegin < longestMatch.aBegin && currentBlock.bBegin < longestMatch.bBegin)
                    queue.push(new Block(currentBlock.aBegin, longestMatch.aBegin, currentBlock.bBegin, longestMatch.bBegin));

                if(longestMatch.aBegin + longestMatch.size < currentBlock.aEnd && longestMatch.bBegin + longestMatch.size < currentBlock.bEnd)
                    queue.push(new Block(longestMatch.aBegin + longestMatch.size, currentBlock.aEnd, longestMatch.bBegin + longestMatch.size, currentBlock.bEnd));
            }
        }

        Collections.sort(this.matchingBlocks);

        // Then merge the matching blocks if adjacent
        int aBegin = 0;
        int bBegin = 0;
        int size = 0;

        ArrayList<Match> nonAdjacent = new ArrayList<>();

        for(Match current_match : this.matchingBlocks) {
            if(aBegin + size == current_match.aBegin && bBegin + size == current_match.bBegin) {
                size += current_match.size;
            } else {
                if(size > 0) {
                    nonAdjacent.add(new Match(aBegin, bBegin, size));
                }
                aBegin = current_match.aBegin;
                bBegin = current_match.bBegin;
                size = current_match.size;
            }
        }

        if(size > 0) {
            nonAdjacent.add(new Match(aBegin, bBegin, size));
        }

        nonAdjacent.add(new Match(aLength, bLength, 0));
        this.matchingBlocks = nonAdjacent;

        return this.matchingBlocks;
    }

    /*
        Get the list of operations to turn A into B

        For instance, let's consider A and B as:
            - A: ["This", "is", "the", "first", "sequence"]
            - B: ["This", "is", "another", "sequence"]

        This function will return three opcodes:
            - Opcode 1:
                - Tag: EQUAL
                - aBegin: 0
                - aEnd: 1
                - bBegin: 0
                - bEnd: 1
            - Opcode 2:
                - Tag: REPLACE
                - aBegin: 2
                - aEnd: 3
                - bBegin: 2
                - bEnd: 2
            - Opcode 3:
                - Tag: EQUAL
                - aBegin: 4
                - aEnd: 4
                - bBegin: 3
                - bBegin: 3
     */
    public ArrayList<Opcode> getOpcodes() {
        if(!this.opcodes.isEmpty()) {
            return this.opcodes;
        }

        int aIndex = 0;
        int bIndex = 0;

        for(Match current_match : this.getMatchingBlocks()) {
            Opcode.Tag current_tag = null;

            if(aIndex < current_match.aBegin && bIndex < current_match.bBegin) {
                current_tag = Opcode.Tag.REPLACE;
            } else if(aIndex < current_match.aBegin) {
                current_tag = Opcode.Tag.DELETE;
            } else if(bIndex < current_match.bBegin) {
                current_tag = Opcode.Tag.INSERT;
            }

            if(current_tag != null) {
                this.opcodes.add(new Opcode(current_tag, aIndex, current_match.aBegin, bIndex, current_match.bBegin));
            }

            aIndex = current_match.aBegin + current_match.size;
            bIndex = current_match.bBegin + current_match.size;

            if(current_match.size > 0) {
                this.opcodes.add(new Opcode(Opcode.Tag.EQUAL, current_match.aBegin, aIndex, current_match.bBegin, bIndex));
            }
        }

        return this.opcodes;
    }
}