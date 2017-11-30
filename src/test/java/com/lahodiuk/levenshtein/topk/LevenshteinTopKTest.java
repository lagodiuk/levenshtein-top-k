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
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.generator.InRange;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;

@RunWith(JUnitQuickcheck.class)
public class LevenshteinTopKTest {

    private final char gapChar = LevenshteinTopK.DEFAULT_GAP_CHAR;
    private final LevenshteinTopKCalculator alg = LevenshteinTopK::getAlignments;

    @Property
    public void alignedStringsHaveTheSameLenth(
            String s1,
            String s2,
            @InRange(min = "1", max = "20") int topK) {

        List<Alignment> results = this.alg.getAlignments(s1, s2, topK, this.gapChar);
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
            String s1,
            String s2,
            @InRange(min = "1", max = "20") int topK) {

        List<Alignment> results = this.alg.getAlignments(s1, s2, topK, this.gapChar);
        for (int i = 1; i < results.size(); i++) {

            Alignment curr = results.get(i);
            Alignment prev = results.get(i - 1);

            assertTrue(curr.editDist >= prev.editDist);
        }
    }
}
