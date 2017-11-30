package com.lahodiuk.levenshtein.topk;

import java.util.List;

import com.lahodiuk.levenshtein.topk.LevenshteinTopK.Alignment;

public interface LevenshteinTopKCalculator {

    List<Alignment> getAlignments(
            String s1,
            String s2,
            int topK,
            char gap);
}
