/*******************************************************************************
 * Copyright 2017 Yurii Lahodiuk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.lahodiuk.levenshtein.topk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The algorithm, which returns top-K different string alignments with the
 * shortest edit distances (based on the Levenshtein distance definition). <br>
 *
 * <br>
 * For example, given the input strings "ABCD" and "AXYD". <br>
 * Let the character '_' be the "gap" character for marking the gaps in the
 * aligned strings (which correspond to the edit operations). <br>
 *
 * Then: <br>
 *
 * <br>
 * 1) <br>
 * The alignment with the shortest edit distance (2) will be: <br>
 *
 * s1 aligned: ABCD <br>
 * s2 aligned: AXYD <br>
 * common str: A__D <br>
 * <br>
 * In order to transform the string s1 to s2 it is needed to do the following
 * edit operations with the string s1: <br>
 *       Keep: 'A'        <br>
 * Substitute: 'B' -> 'X' <br>
 * Substitute: 'C' -> 'Y' <br>
 *       Keep: 'D'        <br>
 *
 * <br>
 * 2) <br>
 * The other possible alignment with the next shortest edit distance (3) will be: <br>
 *
 * s1 aligned: AB_CD <br>
 * s2 aligned: AXY_D <br>
 * common str: A___D <br>
 * <br>
 * In order to transform the string s1 to s2 it is needed to do the following
 * edit operations with the string s1: <br>
 *       Keep: 'A'        <br>
 * Substitute: 'B' -> 'X' <br>
 *     Insert: 'Y'        <br>
 *     Delete: 'C'        <br>
 *       Keep: 'D'        <br>
 *
 * <br>
 * And so forth.
 */
public class LevenshteinTopK {

    public static final int INSERTION_COST = 1;
    public static final int DELETION_COST = 1;
    public static final int SUBSTITUTION_COST = 1;
    public static final char DEFAULT_GAP_CHAR = '_';

    public static List<Alignment> getAlignments(
            String s1,
            String s2,
            int topK) {

        return getAlignments(s1, s2, topK, DEFAULT_GAP_CHAR);
    }

    /**
     * Returns top-K different string alignments with the shortest edit
     * distances (based on the Levenshtein distance definition).
     */
    public static List<Alignment> getAlignments(
            String s1,
            String s2,
            int topK,
            char gap) {

        Cell[][][] mem = calculateMemoizationTable(s1, s2, topK);

        List<Alignment> result = new ArrayList<>();
        for (Cell last : mem[s1.length()][s2.length()]) {
            Alignment alignment = traceBack(s1, s2, mem, last, gap);
            result.add(alignment);
        }
        return result;
    }

    /**
     * Complexity: O(M*N*K*log(N*K)) - if arraylist + sorting will be used
     *
     * Complexity: O(M*N*K*log(K)) - if binary heap will be used
     *
     * Complexity: O(M*N*K) - if Quickselect algorithm will be used
     */
    private static Cell[][][] calculateMemoizationTable(
            String s1,
            String s2,
            int topK) {

        int rows = s1.length() + 1;
        int cols = s2.length() + 1;

        // Initialization of the memoization table
        Cell[][][] mem = new Cell[rows][cols][];
        mem[0][0] = new Cell[] { new Cell(0) };
        for (int row = 0; row < rows; row++) {
            mem[row][0] = new Cell[] { new Cell(row, -1, 0, 0) };
        }
        for (int col = 0; col < cols; col++) {
            mem[0][col] = new Cell[] { new Cell(col, 0, -1, 0) };
        }

        // Calculation of the memoization table
        for (int row = 1; row < rows; row++) {
            char s1Char = s1.charAt(row - 1);

            for (int col = 1; col < cols; col++) {
                char s2Char = s2.charAt(col - 1);

                int subCost = (s1Char == s2Char) ? 0 : SUBSTITUTION_COST;

                // TODO: use either a min-heap, or the Quickselect algorithm
                List<Cell> candidates = new ArrayList<>();

                for (int prevTopK = 0; prevTopK < mem[row - 1][col].length; prevTopK++) {
                    candidates.add(new Cell(mem[row - 1][col][prevTopK].dist + INSERTION_COST, -1, 0, prevTopK));
                }

                for (int prevTopK = 0; prevTopK < mem[row][col - 1].length; prevTopK++) {
                    candidates.add(new Cell(mem[row][col - 1][prevTopK].dist + DELETION_COST, 0, -1, prevTopK));
                }

                for (int prevTopK = 0; prevTopK < mem[row - 1][col - 1].length; prevTopK++) {
                    candidates.add(new Cell(mem[row - 1][col - 1][prevTopK].dist + subCost, -1, -1, prevTopK));
                }

                // TODO: use either a min-heap, or the Quickselect algorithm
                candidates.sort(Comparator.comparing(c -> c.dist));
                if (candidates.size() > topK) {
                    candidates = candidates.subList(0, topK);
                }
                mem[row][col] = candidates.toArray(new Cell[0]);
            }
        }

        return mem;
    }

    private static Alignment traceBack(
            String s1,
            String s2,
            Cell[][][] mem,
            Cell last,
            char gap) {

        int dist = last.dist;

        StringBuilder alignedS1 = new StringBuilder();
        StringBuilder alignedS2 = new StringBuilder();
        StringBuilder commonStr = new StringBuilder();

        // "r" is row
        // "c" is column

        int r = s1.length();
        int c = s2.length();

        Cell curr = last;
        while (r >= 1 || c >= 1) {

            if (curr.deltRow == -1 && curr.deltCol == 0) {
                // Insertion
                char s1Char = s1.charAt(r - 1);
                alignedS1.append(s1Char);
                alignedS2.append(gap);
                commonStr.append(gap);
            } else if (curr.deltRow == 0 && curr.deltCol == -1) {
                // Deletion
                char s2Char = s2.charAt(c - 1);
                alignedS1.append(gap);
                alignedS2.append(s2Char);
                commonStr.append(gap);
            } else if (curr.deltRow == -1 && curr.deltCol == -1) {
                // Substitution
                char s1Char = s1.charAt(r - 1);
                char s2Char = s2.charAt(c - 1);
                if (s1Char == s2Char) {
                    alignedS1.append(s1Char);
                    alignedS2.append(s2Char);
                    commonStr.append(s1Char);
                } else {
                    alignedS1.append(s1Char);
                    alignedS2.append(s2Char);
                    commonStr.append(gap);
                }
            }

            r = r + curr.deltRow;
            c = c + curr.deltCol;
            curr = mem[r][c][curr.prevTopK];
        }

        return new Alignment(
                dist,
                alignedS1.reverse().toString(),
                alignedS2.reverse().toString(),
                commonStr.reverse().toString(),
                gap);
    }

    /**
     * The cell of the memoization table, which handles the edit distance and
     * back references to the preceding cells of the memoization table.
     */
    private static class Cell {

        public final int dist;
        public final int prevTopK;
        // TODO: deltRow and deltCol can be stored inside one int field
        public final int deltRow;
        public final int deltCol;

        public Cell(int dist) {
            this(dist, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
        }

        public Cell(
                int dist,
                int deltRow,
                int deltCol,
                int prevTopK) {

            this.dist = dist;
            this.deltRow = deltRow;
            this.deltCol = deltCol;
            this.prevTopK = prevTopK;
        }
    }

    /**
     * The wrapper around the two mutually aligned strings, which contains the
     * amount of the edit operations, and the alignment with the corresponding
     * common substrings.
     */
    public static class Alignment {

        public final int editDist;
        public final String alignedStr1;
        public final String alignedStr2;
        public final String commonStr;
        public final char gapChar;

        public Alignment(
                int editDist,
                String alignedStr1,
                String alignedStr2,
                String commonStr,
                char gapChar) {

            this.editDist = editDist;
            this.alignedStr1 = alignedStr1;
            this.alignedStr2 = alignedStr2;
            this.commonStr = commonStr;
            this.gapChar = gapChar;
        }
    }
}
