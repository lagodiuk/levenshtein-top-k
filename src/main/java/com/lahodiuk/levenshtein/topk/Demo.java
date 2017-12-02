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

import java.util.List;

import com.lahodiuk.levenshtein.topk.LevenshteinTopK.Alignment;

public class Demo {

    public static void main(String[] args) {
        // printAlignment("frankfurt", "frnkfurt", 100);
        // printAlignment("abc", "abc", 100);
        printAlignment("abcd", "axyd", 10);
        // printAlignment("101011001101", "1101010111010", 100);
    }

    static void printAlignment(String s1, String s2, int topK) {
        List<Alignment> result = LevenshteinTopK.getAlignments(s1, s2, topK);
        System.out.println("Total amount of results: " + result.size());
        System.out.println();
        for (Alignment alignment : result) {
            System.out.println("=======================================");
            System.out.println();
            System.out.println("Edit distance:" + alignment.editDist);
            System.out.println("s1 aligned: " + alignment.alignedStr1);
            System.out.println("s2 aligned: " + alignment.alignedStr2);
            System.out.println("common str: " + alignment.commonStr);
            printExplanation(alignment);
            System.out.println();
        }
    }

    static void printExplanation(Alignment alignment) {

        System.out.println();
        System.out.println("Transformation of the string s1 to s2:");

        for (int i = 0; i < alignment.alignedStr1.length(); i++) {

            char nextCharS1 = alignment.alignedStr1.charAt(i);
            char nextCharS2 = alignment.alignedStr2.charAt(i);

            if (nextCharS1 == alignment.gapChar) {
                // Insertion
                System.out.printf("%10s: '%c' %n", "Insert", nextCharS2);

            } else if (nextCharS2 == alignment.gapChar) {
                // Deletion
                System.out.printf("%10s: '%c' %n", "Delete", nextCharS1);

            } else if (alignment.alignedStr1.charAt(i) != nextCharS2) {
                // Substitution
                System.out.printf("%10s: '%c' -> '%c' %n", "Substitute", nextCharS1, nextCharS2);

            } else {
                // Common characters pair
                System.out.printf("%10s: '%c' %n", "Keep", nextCharS1);
            }
        }
    }
}
