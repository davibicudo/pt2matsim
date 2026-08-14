package org.matsim.pt2matsim.hafas;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.pt2matsim.tools.VehicleTypeDefaults;

/**
 * A line-level Gattung must not silently become a non-rail vehicle.
 *
 * @author Swiss-QSM
 */
public class HafasVehicleTypeResolutionTest {

	@Test
	public void aGenericCategoryResolvesToItself() {
		Assertions.assertEquals(VehicleTypeDefaults.Type.S, HafasConverter.resolveVehicleType("S"));
		Assertions.assertEquals(VehicleTypeDefaults.Type.IC, HafasConverter.resolveVehicleType("IC"));
	}

	@Test
	public void aLineLevelCodeResolvesToItsGenericCategory() {
		// the 2013 Swiss export codes the Zurich S-Bahn services on the Weinbergtunnel corridor as
		// S14, S17 and S18; without this they are classified OTHER and dropped from the schedule
		Assertions.assertEquals(VehicleTypeDefaults.Type.S, HafasConverter.resolveVehicleType("S14"));
		Assertions.assertEquals(VehicleTypeDefaults.Type.S, HafasConverter.resolveVehicleType("S17"));
		Assertions.assertEquals(VehicleTypeDefaults.Type.S, HafasConverter.resolveVehicleType("S18"));
	}

	@Test
	public void theResolvedCategoryIsAddedToTheSchedule() {
		// the point of the change: OTHER is a non-rail category, so an unresolved S-Bahn service
		// would not reach the transit schedule as rail
		Assertions.assertTrue(HafasConverter.resolveVehicleType("S14").addToSchedule);
		Assertions.assertEquals(
				VehicleTypeDefaults.Type.S.transportMode,
				HafasConverter.resolveVehicleType("S14").transportMode);
		Assertions.assertNotEquals(
				VehicleTypeDefaults.Type.OTHER.transportMode,
				HafasConverter.resolveVehicleType("S14").transportMode);
	}

	@Test
	public void anUnknownCategoryStillFallsBackToOther() {
		Assertions.assertEquals(VehicleTypeDefaults.Type.OTHER, HafasConverter.resolveVehicleType("ZZZ"));
		Assertions.assertEquals(VehicleTypeDefaults.Type.OTHER, HafasConverter.resolveVehicleType("ZZZ9"));
	}

	@Test
	public void aNumericSuffixIsOnlyStrippedWhenTheGenericFormExists() {
		// stripping must not invent a category: "QQ7" has no generic form and stays OTHER
		Assertions.assertEquals(VehicleTypeDefaults.Type.OTHER, HafasConverter.resolveVehicleType("QQ7"));
	}
}
