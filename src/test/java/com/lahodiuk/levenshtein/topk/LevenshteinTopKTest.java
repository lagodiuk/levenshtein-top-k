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

@RunWith(JUnitQuickcheck.class)
public class LevenshteinTopKTest {

    private final LevenshteinTopKCalculator alg = LevenshteinTopK::getAlignments;

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

    public int calculateExpectedEditDistance(Alignment alignment) {

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

    public static class InputData {
        public final String s1;
        public final String s2;
        public final char gapChar;
        public final int topK;

        public InputData(String s1, String s2, char gapChar, int topK) {
            this.s1 = s1;
            this.s2 = s2;
            this.gapChar = gapChar;
            this.topK = topK;
        }
    }

    public static class InputDataGenerator extends Generator<InputData> {

        public static final int MAX_STRING_LENGTH = 20;
        public static final int MAX_TOP_K = 100;

        public InputDataGenerator(Class<InputData> type) {
            super(type);
        }

        @Override
        public InputData generate(
                SourceOfRandomness rnd,
                GenerationStatus status) {

            char gapChar = rnd.nextChar((char) 0, (char) 255);
            String s1 = this.generateRandomString(rnd, status, gapChar);
            String s2 = this.generateRandomString(rnd, status, gapChar);
            int topK = rnd.nextInt(1, MAX_TOP_K);

            return new InputData(s1, s2, gapChar, topK);
        }

        public String generateRandomString(
                SourceOfRandomness rnd,
                GenerationStatus status,
                char gapChar) {

            StringBuilder sb = new StringBuilder();
            int length = rnd.nextInt(1, MAX_STRING_LENGTH);
            for (int i = 0; i < length; i++) {
                sb.append(rnd.nextChar('a', 'z'));
            }
            return sb.toString().replace(gapChar, '?');
        }
    }
}
