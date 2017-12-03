# levenshtein-top-k

In many applications of the sequence alignment techniques there is an assumption, that the difference between two compared sequences is a result of the minimal amount of the edit operations (thus the calculation of the minimal edit distance is used). However, in some cases there might be a need to analyze the certain amount of the possible sequence alignments (top-K alignments in the order of increase of the edit distance). Thus, the presented algorithm helps to tackle the latter use case.

![Description of the algorithm](img/illustration.png)

The presented algorithm, finds the top-K different string alignments with the shortest edit distances among other possible alignments (based on the Levenshtein distance definition).
Below is a short description of the algorithm, which complies with the terminology of the standard Levenshtein distance algorithm: https://en.wikipedia.org/wiki/Levenshtein_distance

![Description of the algorithm](img/description.png)

Let's denote `M` as the length of the input string `s1`, and `N` is the length of the input string `s2`.
Then, the runtime complexity of the algorithm is `O(N*M*K + K*log(K))` or `O(N*M*K*log(K))` (depending on the implementation of the supplementary procedure for selection of the `K` smallest elements of an array).
