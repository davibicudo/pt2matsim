/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2026 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 * *********************************************************************** *
 */

package org.matsim.pt2matsim.hafas.lib;

/**
 * Detects whether a HAFAS/HRDF record carries a five- or six-character Fahrtnummer.
 *
 * <p>HRDF 5.40 specifies a six-character trip number, and the field layouts documented in
 * {@link FPLANReader} and {@link DurchbiReader} follow it. Earlier generations of the Swiss
 * export — and files still in circulation for historic timetable years — use five characters,
 * which shifts every field to the right of the trip number one column to the left.</p>
 *
 * <p>Reading a five-character file with the six-character offsets does not fail: {@code substring}
 * succeeds and returns a value that is wrong by one column. The operator code is then read with a
 * leading character missing, the lookup misses, and the line identifier is built from a null
 * operator. Because the parse is silent, it is detected here per record rather than per file, so a
 * single reader serves both generations and a mixed file does not need a mode flag.</p>
 *
 * @author Swiss-QSM
 */
public final class HafasTripNumberWidth {

	/** Column that separates the trip number from the operator in the six-character layout. */
	private static final int SEPARATOR_IN_SIX_CHARACTER_LAYOUT = 8;

	private HafasTripNumberWidth() {
	}

	/**
	 * Whether an {@code *Z} record uses the six-character trip number of HRDF 5.40.
	 *
	 * <p>In the six-character layout columns 4–9 hold the trip number, so the character at
	 * 0-based index 8 is still part of it; in the five-character layout that index is the blank
	 * separating the trip number from the operator.</p>
	 */
	public static boolean sixCharacterFplan(String line) {
		if (line.length() <= SEPARATOR_IN_SIX_CHARACTER_LAYOUT) {
			return true;
		}
		return line.charAt(SEPARATOR_IN_SIX_CHARACTER_LAYOUT) != ' ';
	}

	/**
	 * Whether a {@code DURCHBI} record uses the six-character trip number of HRDF 5.40.
	 *
	 * <p>The record opens with the trip number, so in the six-character layout index 6 is the
	 * blank that follows it, and in the five-character layout it is already the operator.</p>
	 */
	public static boolean sixCharacterDurchbi(String line) {
		if (line.length() <= 6) {
			return true;
		}
		return line.charAt(6) == ' ';
	}

	/**
	 * Column offset to apply to every field right of the trip number: 0 for the six-character
	 * layout and -1 for the five-character one.
	 *
	 * <p>Valid where a record carries a single trip number, as {@code *Z} does. It is not valid
	 * for {@code DURCHBI}, which carries two — see {@link #durchbiField}.</p>
	 */
	public static int offset(boolean sixCharacter) {
		return sixCharacter ? 0 : -1;
	}

	/*
	 A DURCHBI record names two trips, so the shift is cumulative rather than uniform: the fields
	 between the two trip numbers move by one column and those after the second by two. Both
	 layouts are therefore tabulated rather than derived from a single offset.

	 1  first trip number     2  first operator     3  stop of the through-binding
	 4  second trip number    5  second operator    6  Verkehrstagenummer
	 7  last stop of the second trip
	 */
	private static final int[][] DURCHBI_SIX = {
			{0, 6}, {7, 13}, {14, 21}, {22, 28}, {29, 35}, {36, 42}, {43, 50}};
	private static final int[][] DURCHBI_FIVE = {
			{0, 5}, {6, 12}, {13, 20}, {21, 26}, {27, 33}, {34, 40}, {41, 48}};

	/** Number of fields a DURCHBI record carries in either layout. */
	public static final int DURCHBI_FIELDS = DURCHBI_SIX.length;

	/**
	 * One field of a DURCHBI record, 1-indexed as in the layout comment above, or an empty string
	 * where the record is too short to carry it.
	 */
	public static String durchbiField(String line, int field) {
		int[][] bounds = sixCharacterDurchbi(line) ? DURCHBI_SIX : DURCHBI_FIVE;
		int[] span = bounds[field - 1];
		if (line.length() < span[1]) {
			return line.length() <= span[0] ? "" : line.substring(span[0]).trim();
		}
		return line.substring(span[0], span[1]).trim();
	}

	/** Columns a DURCHBI record must have for its six mandatory fields to be readable. */
	public static int minimumDurchbiLength(String line) {
		int[][] bounds = sixCharacterDurchbi(line) ? DURCHBI_SIX : DURCHBI_FIVE;
		return bounds[5][1];
	}
}
