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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.runner.RunWith;

import com.lahodiuk.levenshtein.topk.LevenshteinTopK.Alignment;
import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;

/**
 * The following properties are tested:
 *
 * 1) The edit distance between two strings always exists. Thus, the results of
 * the LevenshteinTopK algorithm are never empty.
 *
 * 2) The results of the LevenshteinTopK algorithm contain alignments of the
 * strings. The alignments represent the the original input strings with
 * additional "gap" characters inside (these gap characters reflect the edit
 * operations on the strings). The aligned strings must always have the same
 * length.
 *
 * 3) The results of the LevenshteinTopK algorithm represent the top-K different
 * alignments with the smallest edit distances (in the order of increase of the
 * edit distance). Thus, the edit distances of results must always be in
 * non-decreasing order.
 *
 * 4) The calculated values of the edit distances must comply to the amount of
 * edit operations, derived from the aligned strings.
 *
 * 5) Removal of the gap characters from the aligned strings results in the
 * original input strings.
 *
 * 6) The non-gap characters in the longest common substring are exactly the
 * same as the characters in the corresponding positions inside the both of
 * aligned strings.
 *
 * 7) Results must be stable. Given the strings s1 and s2, and the top-K and and
 * top-N results of the LevenshteinTopK algorithm on these strings (the results
 * are the lists of alignments with the smallest edit distances). Let M = min(K,
 * N), then the list of top-M results of the LevenshteinTopK algorithm will be
 * present at the head (prefix) of both other lists: top-K and top-N.
 *
 * 8) The edit distance of the first results of the LevenshteinTopK algorithm
 * must be equal to the edit distance, calculated by the Wagner–Fischer
 * algorithm.
 *
 * 9) Given the input strings s1 and s2, it follows that the length of the
 * corresponding aligned strings (an the longest common aligned substring) must
 * be less or equal than (length(s1) + length(s2)).
 */
@RunWith(JUnitQuickcheck.class)
public class LevenshteinTopKTest {

    private final LevenshteinTopKCalculator alg = LevenshteinTopK::getAlignments;

    /**
     * 1) The edit distance between two strings always exists. Thus, the results
     * of the LevenshteinTopK algorithm are never empty.
     */
    @Property
    public void resultAlwaysNonEmpty(
            @From(InputDataGenerator.class) InputData input) {

        List<Alignment> results = this.alg.getAlignments(
                input.s1, input.s2, input.topK, input.gapChar);

        assertTrue(results.size() > 0);
    }

    /**
     * 2) The results of the LevenshteinTopK algorithm contain alignments of the
     * strings. The alignments represent the the original input strings with
     * additional "gap" characters inside (these gap characters reflect the edit
     * operations on the strings). The aligned strings must always have the same
     * length.
     */
    @Property
    public void alignedStringsHaveTheSameLenth(
            @From(InputDataGenerator.class) InputData input) {

        List<Alignment> results = this.alg.getAlignments(
                input.s1, input.s2, input.topK, input.gapChar);

        for (Alignment alignment : results) {

            int alignedS1Length = alignment.alignedStr1.length();
            int alignedS2Length = alignment.alignedStr2.length();
            int commonLength = alignment.commonStr.length();

            assertEquals(alignedS1Length, alignedS2Length);
            assertEquals(alignedS1Length, commonLength);
        }
    }

    /**
     * 3) The results of the LevenshteinTopK algorithm represent the top-K
     * different alignments with the smallest edit distances (in the order of
     * increase of the edit distance). Thus, the edit distances of results must
     * always be in non-decreasing order.
     */
    @Property
    public void editDistancesAreIncreasing(
            @From(InputDataGenerator.class) InputData input) {

        List<Alignment> results = this.alg.getAlignments(
                input.s1, input.s2, input.topK, input.gapChar);

        for (int i = 1; i < results.size(); i++) {

            Alignment curr = results.get(i);
            Alignment prev = results.get(i - 1);

            assertTrue(curr.editDist >= prev.editDist);
        }
    }

    /**
     * 4) The calculated values of the edit distances must comply to the amount
     * of edit operations, derived from the aligned strings.
     */
    @Property
    public void editDistancesMustBeCorrect(
            @From(InputDataGenerator.class) InputData input) {

        List<Alignment> results = this.alg.getAlignments(
                input.s1, input.s2, input.topK, input.gapChar);

        for (Alignment alignment : results) {

            int expectedEditDistance = this.calculateExpectedEditDistance(alignment);

            assertEquals(expectedEditDistance, alignment.editDist);
        }
    }

    /**
     * 5) Removal of the gap characters from the aligned strings results in the
     * original input strings.
     */
    @Property
    public void removalOfTheGapCharactersFromAlignedStringsEqualToInput(
            @From(InputDataGenerator.class) InputData input) {

        List<Alignment> results = this.alg.getAlignments(
                input.s1, input.s2, input.topK, input.gapChar);

        for (Alignment alignment : results) {

            String alignedStr1WithoutGaps =
                    this.deleteGapCharacter(alignment.alignedStr1, alignment.gapChar);

            String alignedStr2WithoutGaps =
                    this.deleteGapCharacter(alignment.alignedStr2, alignment.gapChar);

            assertEquals(input.s1, alignedStr1WithoutGaps);
            assertEquals(input.s2, alignedStr2WithoutGaps);
        }
    }

    /**
     * 6) The non-gap characters in the longest common substring are exactly the
     * same as the characters in the corresponding positions inside the both of
     * aligned strings.
     */
    @Property
    public void commonStrIsCorrect(
            @From(InputDataGenerator.class) InputData input) {

        List<Alignment> results = this.alg.getAlignments(
                input.s1, input.s2, input.topK, input.gapChar);

        for (Alignment alignment : results) {

            for (int i = 0; i < alignment.commonStr.length(); i++) {
                char commonStrChar = alignment.commonStr.charAt(i);

                if (commonStrChar != alignment.gapChar) {
                    char alignedStr1Char = alignment.alignedStr1.charAt(i);
                    char alignedStr2Char = alignment.alignedStr2.charAt(i);

                    assertEquals(commonStrChar, alignedStr1Char);
                    assertEquals(commonStrChar, alignedStr2Char);
                }
            }
        }
    }

    /**
     * 7) Results must be stable. Given the strings s1 and s2, and the top-K and
     * and top-N results of the LevenshteinTopK algorithm on these strings (the
     * results are the lists of alignments with the smallest edit distances).
     * Let M = min(K, N), then the list of top-M results of the LevenshteinTopK
     * algorithm will be present at the head (prefix) of both other lists: top-K
     * and top-N.
     */
    @Property(trials = 200)
    public void stabilityOfResults(
            @From(InputDataGenerator.class) InputData input) {

        int topM = Math.min(input.topK, input.topN);

        List<Alignment> resultsTopM = this.alg.getAlignments(
                input.s1, input.s2, topM, input.gapChar);

        List<Alignment> resultsTopK = this.alg.getAlignments(
                input.s1, input.s2, input.topK, input.gapChar);

        List<Alignment> resultsTopN = this.alg.getAlignments(
                input.s1, input.s2, input.topN, input.gapChar);

        for (int i = 0; i < resultsTopM.size(); i++) {
            Alignment alignmentTopM = resultsTopM.get(i);
            Alignment alignmentTopN = resultsTopN.get(i);

            this.assertEquality(alignmentTopM, alignmentTopN);
        }

        for (int i = 0; i < resultsTopM.size(); i++) {
            Alignment alignmentTopM = resultsTopM.get(i);
            Alignment alignmentTopK = resultsTopK.get(i);

            this.assertEquality(alignmentTopM, alignmentTopK);
        }
    }

    /**
     * 8) The edit distance of the first result of the LevenshteinTopK algorithm
     * must be equal to the edit distance, calculated by the Wagner–Fischer
     * algorithm.
     */
    @Property(trials = 200)
    public void firstResultHasShortestEditDistance(
            @From(InputDataGenerator.class) InputData input) {

        List<Alignment> results = this.alg.getAlignments(
                input.s1, input.s2, input.topK, input.gapChar);

        Alignment firstResult = results.get(0);

        int shortestEditDist = this.shortestEditDistance(
                input.s1.toCharArray(), input.s2.toCharArray());

        assertEquals(shortestEditDist, firstResult.editDist);
    }

    /**
     * 9) Given the input strings s1 and s2, it follows that the length of the
     * corresponding aligned strings (an the longest common aligned substring)
     * must be less or equal than (length(s1) + length(s2)).
     */
    @Property(trials = 200)
    public void maxLengthOfTheAlignedString(
            @From(InputDataGenerator.class) InputData input) {

        List<Alignment> results = this.alg.getAlignments(
                input.s1, input.s2, input.topK, input.gapChar);

        int maxLength = input.s1.length() + input.s2.length();

        for (Alignment alignment : results) {
            assertTrue(alignment.alignedStr1.length() <= maxLength);
            assertTrue(alignment.alignedStr2.length() <= maxLength);
            assertTrue(alignment.commonStr.length() <= maxLength);
        }
    }

    /**
     * Checking the equality of two different alignments.
     */
    public void assertEquality(Alignment a1, Alignment a2) {
        assertEquals(a1.editDist, a2.editDist);
        assertEquals(a1.alignedStr1, a2.alignedStr1);
        assertEquals(a1.alignedStr2, a2.alignedStr2);
        assertEquals(a1.commonStr, a2.commonStr);
        assertEquals(a1.gapChar, a2.gapChar);
    }

    /**
     * Remove the "gap" character from the string.
     */
    private String deleteGapCharacter(String s, char gapChar) {
        return s.replace("" + gapChar, "");
    }

    /**
     * Calculate the edit distance based on the aligned strings.
     */
    private int calculateExpectedEditDistance(Alignment alignment) {

        int expectedEditDistance = 0;
        for (int i = 0; i < alignment.commonStr.length(); i++) {

            char s1Chr = alignment.alignedStr1.charAt(i);
            char s2Chr = alignment.alignedStr2.charAt(i);

            if (s1Chr == alignment.gapChar && s2Chr != alignment.gapChar) {
                expectedEditDistance += LevenshteinTopK.INSERTION_COST;

            } else if (s1Chr != alignment.gapChar && s2Chr == alignment.gapChar) {
                expectedEditDistance += LevenshteinTopK.DELETION_COST;

            } else if (s1Chr != s2Chr) {
                expectedEditDistance += LevenshteinTopK.SUBSTITUTION_COST;
            }
        }
        return expectedEditDistance;
    }

    /**
     * Memory optimized version of the Wagner–Fischer algorithm.
     */
    private int shortestEditDistance(char[] s1, char[] s2) {

        // memoize only previous line of distance matrix
        int[] prev = new int[s2.length + 1];

        for (int j = 0; j < s2.length + 1; j++) {
            prev[j] = j;
        }

        for (int i = 1; i < s1.length + 1; i++) {

            // calculate current line of distance matrix
            int[] curr = new int[s2.length + 1];
            curr[0] = i;

            for (int j = 1; j < s2.length + 1; j++) {
                int d1 = prev[j] + LevenshteinTopK.DELETION_COST;
                int d2 = curr[j - 1] + LevenshteinTopK.INSERTION_COST;
                int d3 = prev[j - 1];
                if (s1[i - 1] != s2[j - 1]) {
                    d3 += LevenshteinTopK.SUBSTITUTION_COST;
                }
                curr[j] = Math.min(Math.min(d1, d2), d3);
            }

            // define current line of distance matrix as previous
            prev = curr;
        }
        return prev[s2.length];
    }

    /**
     * Input data for the LevenshteinTopK algorithm.
     */
    public static class InputData {
        public final String s1;
        public final String s2;
        public final char gapChar;
        public final int topK;
        public final int topN;

        public InputData(String s1, String s2, char gapChar, int topK, int topN) {
            this.s1 = s1;
            this.s2 = s2;
            this.gapChar = gapChar;
            this.topK = topK;
            this.topN = topN;
        }
    }

    /**
     * Generates the random instances of the input data for the LevenshteinTopK
     * algorithm.
     */
    public static class InputDataGenerator extends Generator<InputData> {

        public static final int MAX_STRING_LENGTH = 50;
        public static final int MAX_TOP_K = 100;

        public InputDataGenerator(Class<InputData> type) {
            super(type);
        }

        @Override
        public InputData generate(
                SourceOfRandomness rnd,
                GenerationStatus status) {

            char gapChar = rnd.nextChar((char) 0, (char) 254);
            String s1 = this.generateRandomSimpleString(rnd, status, gapChar);
            String s2 = this.generateRandomSimpleString(rnd, status, gapChar);
            int topK = rnd.nextInt(1, MAX_TOP_K);
            int topN = rnd.nextInt(1, MAX_TOP_K);

            return new InputData(s1, s2, gapChar, topK, topN);
        }

        /**
         * Usually, generated strings consists of the characters: 'a' to 'z'. <br>
         *
         * The gapChar characters in the generated string will be replaced to
         * the (char) (gapChar + 1). <br>
         *
         * Thus, in case if gapChar is 'z', then the generated string might
         * contain the character (char) ('z' + 1).
         */
        public String generateRandomSimpleString(
                SourceOfRandomness rnd,
                GenerationStatus status,
                char gapChar) {

            StringBuilder sb = new StringBuilder();
            int length = rnd.nextInt(1, MAX_STRING_LENGTH);
            for (int i = 0; i < length; i++) {
                sb.append(rnd.nextChar('a', 'z'));
            }
            return sb.toString().replace(gapChar, (char) (gapChar + 1));
        }
    }

    /**
     * Functional interface, which allows to abstract the signature of the
     * LevenshteinTopK algorithm.
     */
    @FunctionalInterface
    public interface LevenshteinTopKCalculator {

        List<Alignment> getAlignments(
                String s1,
                String s2,
                int topK,
                char gap);
    }
}
