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

public class LevenshteinTopK {

	public static void main(String[] args) {
		// calculate("frankfurt", "frnkfurt", 100);
		// calculate("abc", "abc", 100);
		// calculate("abcd", "axyd", 10);
		calculate("101011001101", "1101010111010", 100);
	}

	private static final int BOUNDARY = -10;
	private static final int INSERTION_COST = 1;
	private static final int DELETION_COST = 1;
	private static final int SUBSTITUTION_COST = 1;

	/**
	 * Complexity: O(M*N*K*log(N*K)) - if arraylist + sorting will be used
	 *
	 * Complexity: O(M*N*K*log(K)) - if binary heap will be used
	 *
	 * Complexity: O(M*N*K) - if Quickselect algorithm will be used
	 */
	static void calculate(String s1, String s2, int topK) {

		Cell[][][] mem = new Cell[s1.length() + 1][s2.length() + 1][];

		int rows = s1.length() + 1;
		int cols = s2.length() + 1;

		mem[0][0] = new Cell[]{new Cell(0, BOUNDARY, BOUNDARY, BOUNDARY)};

		for (int row = 0; row < rows; row++) {
			mem[row][0] = new Cell[]{new Cell(row, -1, 0, 0)};
		}

		for (int col = 0; col < cols; col++) {
			mem[0][col] = new Cell[]{new Cell(col, 0, -1, 0)};
		}

		for (int row = 1; row < rows; row++) {
			char s1Char = s1.charAt(row - 1);

			for (int col = 1; col < cols; col++) {
				char s2Char = s2.charAt(col - 1);

				int subCost = s1Char == s2Char ? 0 : SUBSTITUTION_COST;

				// TODO: use either min-heap, or Quickselect algorithm
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

				// TODO: use either min-heap, or Quickselect algorithm
				candidates.sort(Comparator.comparing(c -> c.dist));
				if (candidates.size() > topK) {
					candidates = candidates.subList(0, topK);
				}
				mem[row][col] = candidates.toArray(new Cell[0]);
			}
		}

		System.out.println("Total amount of results: " + mem[s1.length()][s2.length()].length);
		System.out.println();

		for (Cell last : mem[s1.length()][s2.length()]) {
			traceBack(s1, s2, mem, last);
		}
	}

	private static void traceBack(String s1, String s2, Cell[][][] mem, Cell last) {

		int dist = last.dist;

		StringBuilder s1Aligned = new StringBuilder();
		StringBuilder s2Aligned = new StringBuilder();
		StringBuilder commonStr = new StringBuilder();

		// "r" is row
		// "c" is column

		int r = s1.length();
		int c = s2.length();

		Cell curr = last;
		while (r >= 1 || c >= 1) {

			if (curr.deltRow == -1 && curr.deltCol == 0) {
				char s1Char = s1.charAt(r - 1);
				s1Aligned.append(s1Char);
				s2Aligned.append("_");
				commonStr.append("_");
			} else if (curr.deltRow == 0 && curr.deltCol == -1) {
				char s2Char = s2.charAt(c - 1);
				s1Aligned.append("_");
				s2Aligned.append(s2Char);
				commonStr.append("_");
			} else if (curr.deltRow == -1 && curr.deltCol == -1) {
				char s1Char = s1.charAt(r - 1);
				char s2Char = s2.charAt(c - 1);
				if (s1Char == s2Char) {
					s1Aligned.append(s1Char);
					s2Aligned.append(s2Char);
					commonStr.append(s1Char);
				} else {
					s1Aligned.append(s1Char);
					s2Aligned.append(s2Char);
					commonStr.append("_");
				}
			}

			r = r + curr.deltRow;
			c = c + curr.deltCol;
			curr = mem[r][c][curr.prevTopK];
		}

		System.out.println("Edit distance:" + dist);
		System.out.println("s1 aligned: " + s1Aligned.reverse().toString());
		System.out.println("s2 aligned: " + s2Aligned.reverse().toString());
		System.out.println("common str: " + commonStr.reverse().toString());
		System.out.println();
	}

	static class Cell {
		public final int dist;
		public final int deltRow;
		public final int deltCol;
		public final int prevTopK;
		public Cell(int dist, int deltRow, int deltCol, int prevTopK) {
			this.dist = dist;
			this.deltRow = deltRow;
			this.deltCol = deltCol;
			this.prevTopK = prevTopK;
		}
	}
}
