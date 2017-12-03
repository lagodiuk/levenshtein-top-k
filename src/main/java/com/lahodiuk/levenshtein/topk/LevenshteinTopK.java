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
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * The algorithm, which returns top-K different string alignments with the
 * shortest edit distances (based on the Levenshtein distance definition).
 *
 * For example, given the input strings s1 = "ABCD" and s2 = "AXYD".
 * Let the character '_' be the "gap" character for marking the gaps in the
 * aligned strings (which correspond to the edit operations).
 * Then:
 *
 * 1) The alignment with the shortest edit distance (2) will be:
 * s1 aligned: ABCD
 * s2 aligned: AXYD
 * common str: A__D
 *  In order to transform the string s1 to s2 it is needed to do the following
 *  edit operations with the string s1:
 *       Keep: 'A'
 * Substitute: 'B' -> 'X'
 * Substitute: 'C' -> 'Y'
 *       Keep: 'D'
 *
 * 2) The other possible alignment with the next shortest edit distance (3) will be:
 * s1 aligned: AB_CD
 * s2 aligned: AXY_D
 * common str: A___D
 *  In order to transform the string s1 to s2 it is needed to do the following
 *  edit operations with the string s1:
 *       Keep: 'A'
 * Substitute: 'B' -> 'X'
 *     Insert: 'Y'
 *     Delete: 'C'
 *       Keep: 'D'
 *
 * And so forth.
 *
 * One of a supplementary steps of the algorithm is a selection of
 * the K smallest elements of an array.
 * The different strategies can be used for this purpose:
 * - Quickselect-based partition of an array (based on the Hoare's selection algorithm)
 * - Sorting-based partition of an array
 *
 * In case of the Quickselect-based partition, the expected runtime complexity is: O(M*N*K + K*log(K)).
 * In case of the Sorting-based partition, the expected runtime complexity is: O(M*N*K*log(K) + K*log(K)).
 * Where:
 * - M is the length of the input string s1
 * - N is the length of the input string s2
 * - K amount of alignments with the shortest edit distances (top-K)
 */
public class LevenshteinTopK {

    public static final int INSERTION_COST = 1;
    public static final int DELETION_COST = 1;
    public static final int SUBSTITUTION_COST = 1;
    public static final char DEFAULT_GAP_CHAR = '_';

    /**
     * If true, then the Quickselect-based partition is used,
     * otherwise the Sorting-based partition is used.
     */
    public static boolean QUICKSELECT_BASED_PARTITION = false;

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
     * The expected runtime complexity is: O(M*N*K + K*log(K)),
     * where:
     * - M is the length of the input string s1
     * - N is the length of the input string s2
     * - K amount of alignments with the shortest edit distances (top-K)
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

        // Supplementary array
        Cell[] candidates = new Cell[3 * topK];
        int candidatesCount = 0;

        // Calculation of the memoization table
        for (int row = 1; row < rows; row++) {
            char s1Char = s1.charAt(row - 1);

            for (int col = 1; col < cols; col++) {
                char s2Char = s2.charAt(col - 1);

                int subCost = (s1Char == s2Char) ? 0 : SUBSTITUTION_COST;

                candidatesCount = 0;
                // Iterate over the predecessors (for the case of insertion)
                for (int prevTopK = 0; prevTopK < mem[row - 1][col].length; prevTopK++) {
                    int newDist = mem[row - 1][col][prevTopK].dist + INSERTION_COST;
                    candidates[candidatesCount] = new Cell(newDist, -1, 0, prevTopK);
                    candidatesCount++;
                }
                // Iterate over the predecessors (for the case of deletion)
                for (int prevTopK = 0; prevTopK < mem[row][col - 1].length; prevTopK++) {
                    int newDist = mem[row][col - 1][prevTopK].dist + DELETION_COST;
                    candidates[candidatesCount] = new Cell(newDist, 0, -1, prevTopK);
                    candidatesCount++;
                }
                // Iterate over the predecessors (for the case of substitution)
                for (int prevTopK = 0; prevTopK < mem[row - 1][col - 1].length; prevTopK++) {
                    int newDist = mem[row - 1][col - 1][prevTopK].dist + subCost;
                    candidates[candidatesCount] = new Cell(newDist, -1, -1, prevTopK);
                    candidatesCount++;
                }

                if (candidatesCount > topK) {
                    // Select from all candidates K cells with the smallest edit distances
                    selection(candidates, candidatesCount, topK);
                    mem[row][col] = new Cell[topK];
                    System.arraycopy(candidates, 0, mem[row][col], 0, topK);
                } else {
                    // Amount of candidates is smaller than K
                    mem[row][col] = new Cell[candidatesCount];
                    System.arraycopy(candidates, 0, mem[row][col], 0, candidatesCount);
                }
            }
        }

        Arrays.sort(mem[s1.length()][s2.length()], Comparator.comparing(c -> c.dist));
        return mem;
    }

    /**
     * Reconstruction of the strings alignments from the memoization table.
     * The runtime complexity is O(M + N),
     * where:
     * - M is the length of the input string s1
     * - N is the length of the input string s2
     */
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
     * Moves the top-k smallest items of the array to the left hand side.
     * Uses either the Sorting-based or the Quickselect-based partition.
     * See: https://en.wikipedia.org/wiki/Selection_algorithm
     */
    private static void selection(Cell[] arr, int length, int k) {
        if (QUICKSELECT_BASED_PARTITION) {
            // According to the recommendation of Robert Sedgewick
            // it worth to shuffle the array in order to guarantee
            // the expected linear runtime complexity.
            shuffle(arr, length);
            quickselect(arr, 0, length - 1, k - 1);
        } else {
            // Sorting-based partition
            Arrays.sort(arr, 0, length, Comparator.comparing(c -> c.dist));
        }
    }

    /**
     * Hoare's selection algorithm.
     * Moves the top-k smallest items of the array to the left hand side.
     * See: https://en.wikipedia.org/wiki/Quickselect
     */
    private static Cell quickselect(Cell[] arr, int left, int right, int k) {
        int p = partition(arr, left, right);
        if (k == p) {
            return arr[p];
        } else if (k < p) {
            return quickselect(arr, left, p - 1, k);
        } else {
            return quickselect(arr, p + 1, right, k);
        }
    }

    /**
     * 3-way partition, used by the Hoare's selection algorithm.
     */
    private static int partition(Cell[] arr, int left, int right) {
        int pivot = arr[left].dist;
        int less = left;
        int greater = right;
        int i = left + 1;
        while (i <= greater) {
            if (arr[i].dist < pivot) {
                swap(arr, i, less);
                i++;
                less++;
            } else if (arr[i].dist > pivot) {
                swap(arr, i, greater);
                greater--;
            } else {
                i++;
            }
        }
        return (less + greater) / 2;
    }

    /**
     * Used by the Hoare's selection algorithm.
     */
    private static void swap(Cell[] arr, int i, int j) {
        Cell tmp = arr[i];
        arr[i] = arr[j];
        arr[j] = tmp;
    }

    /**
     * Used by the Hoare's selection algorithm.
     */
    static void shuffle(Cell[] arr, int length) {
        for (int i = 0; i < length; i++) {
            int x = RANDOM.nextInt(i + 1);
            swap(arr, i, x);
        }
    }

    private static final Random RANDOM = new Random(1);

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
