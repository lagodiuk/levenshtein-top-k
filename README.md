# levenshtein-top-k

In many applications of the sequence alignment techniques there is an assumption, that the difference between two compared sequences is a result of the minimal amount of the edit operations (thus the calculation of the minimal edit distance is used). However, in some cases there might be a need to analyze the certain amount of the possible sequence alignments (top-K alignments in the order of increase of the edit distance). Thus, the presented algorithm helps to tackle the latter use case.

Algorithm for the derivation of the top-K optimal string alignments, based on the Levenshtein distance.
Below is a short description of the algorithm, which complies with the terminology of the standard Levenshtein distance algorithm: https://en.wikipedia.org/wiki/Levenshtein_distance

![Description of the algorithm](img/description.png)
