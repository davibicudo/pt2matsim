package org.matsim.pt2matsim.hafas.lib;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Records quoted verbatim from Swiss HRDF exports of five different timetable years.
 *
 * @author Swiss-QSM
 */
public class HafasTripNumberWidthTest {

	// *Z records. 2013, 2016 and 2019 carry five characters, 2024 and 2025 six.
	private static final String Z_2013 = "*Z 00001 101___    01 084 010                             % 00001 101___    01 (001)";
	private static final String Z_2016 = "*Z 20000 000101   010                                     % 20000 000101   010 (001)";
	private static final String Z_2019 = "*Z 004270 06____   001                                     %";
	private static final String Z_2025 = "*Z 000003 000011   101                                     % -- 42540358552 --";

	// DURCHBI records, same split.
	private static final String DURCHBI_2016 = "00001 000037 8500063 00002 000037 000000 8500063    % Oberwil BL";
	private static final String DURCHBI_2024 = "000001 000181 8530625 000002 000181 000000 8530625   ";

	@Test
	public void fiveCharacterFplanRecordsAreDetected() {
		Assertions.assertFalse(HafasTripNumberWidth.sixCharacterFplan(Z_2013));
		Assertions.assertFalse(HafasTripNumberWidth.sixCharacterFplan(Z_2016));
	}

	@Test
	public void sixCharacterFplanRecordsAreDetected() {
		// the 2019 record above is already staged to six characters; 2025 ships that way
		Assertions.assertTrue(HafasTripNumberWidth.sixCharacterFplan(Z_2019));
		Assertions.assertTrue(HafasTripNumberWidth.sixCharacterFplan(Z_2025));
	}

	@Test
	public void theOperatorIsReadWholeOnBothLayouts() {
		Assertions.assertEquals("101___", operatorOf(Z_2013));
		Assertions.assertEquals("000101", operatorOf(Z_2016));
		Assertions.assertEquals("000011", operatorOf(Z_2025));
	}

	@Test
	public void theSixCharacterOffsetsWouldTruncateAFiveCharacterOperator() {
		// this is the silent failure the detection exists to prevent: substring succeeds and the
		// operator lookup misses, leaving the line identifier built on a null operator
		Assertions.assertEquals("01___", Z_2013.substring(10, 16).trim());
		Assertions.assertEquals("00101", Z_2016.substring(10, 16).trim());
	}

	@Test
	public void theTripNumberIsReadWholeOnBothLayouts() {
		Assertions.assertEquals("00001", tripNumberOf(Z_2013));
		Assertions.assertEquals("20000", tripNumberOf(Z_2016));
		Assertions.assertEquals("000003", tripNumberOf(Z_2025));
	}

	@Test
	public void theCycleFieldsFollowTheTripNumberWidth() {
		int shift = HafasTripNumberWidth.offset(HafasTripNumberWidth.sixCharacterFplan(Z_2013));
		Assertions.assertEquals("084", Z_2013.substring(23 + shift, 26 + shift));
		Assertions.assertEquals("010", Z_2013.substring(27 + shift, 30 + shift));
	}

	@Test
	public void durchbiRecordsAreDetectedByTheirOwnLayout() {
		Assertions.assertFalse(HafasTripNumberWidth.sixCharacterDurchbi(DURCHBI_2016));
		Assertions.assertTrue(HafasTripNumberWidth.sixCharacterDurchbi(DURCHBI_2024));
	}

	@Test
	public void everyDurchbiFieldLandsWholeOnBothLayouts() {
		Assertions.assertEquals("00001", HafasTripNumberWidth.durchbiField(DURCHBI_2016, 1));
		Assertions.assertEquals("000037", HafasTripNumberWidth.durchbiField(DURCHBI_2016, 2));
		Assertions.assertEquals("8500063", HafasTripNumberWidth.durchbiField(DURCHBI_2016, 3));
		Assertions.assertEquals("00002", HafasTripNumberWidth.durchbiField(DURCHBI_2016, 4));
		Assertions.assertEquals("000037", HafasTripNumberWidth.durchbiField(DURCHBI_2016, 5));
		Assertions.assertEquals("000000", HafasTripNumberWidth.durchbiField(DURCHBI_2016, 6));
		Assertions.assertEquals("8500063", HafasTripNumberWidth.durchbiField(DURCHBI_2016, 7));

		Assertions.assertEquals("000001", HafasTripNumberWidth.durchbiField(DURCHBI_2024, 1));
		Assertions.assertEquals("000181", HafasTripNumberWidth.durchbiField(DURCHBI_2024, 2));
		Assertions.assertEquals("8530625", HafasTripNumberWidth.durchbiField(DURCHBI_2024, 3));
		Assertions.assertEquals("000002", HafasTripNumberWidth.durchbiField(DURCHBI_2024, 4));
		Assertions.assertEquals("000181", HafasTripNumberWidth.durchbiField(DURCHBI_2024, 5));
		Assertions.assertEquals("000000", HafasTripNumberWidth.durchbiField(DURCHBI_2024, 6));
	}

	@Test
	public void theShiftAcrossADurchbiRecordIsCumulativeNotUniform() {
		// the record names two trips, so a single -1 offset is right for the fields between them
		// and wrong for those after the second: the second operator would lose a character
		Assertions.assertEquals("00037", DURCHBI_2016.substring(28, 34).trim());
		Assertions.assertEquals("000037", HafasTripNumberWidth.durchbiField(DURCHBI_2016, 5));
	}

	@Test
	public void aShortRecordIsTreatedAsTheCurrentLayoutRatherThanThrowing() {
		Assertions.assertTrue(HafasTripNumberWidth.sixCharacterFplan("*Z 0001"));
		Assertions.assertTrue(HafasTripNumberWidth.sixCharacterDurchbi("00001"));
	}

	private static String operatorOf(String line) {
		int shift = HafasTripNumberWidth.offset(HafasTripNumberWidth.sixCharacterFplan(line));
		return line.substring(10 + shift, 16 + shift).trim();
	}

	private static String tripNumberOf(String line) {
		int shift = HafasTripNumberWidth.offset(HafasTripNumberWidth.sixCharacterFplan(line));
		return line.substring(3, 9 + shift).trim();
	}
}
