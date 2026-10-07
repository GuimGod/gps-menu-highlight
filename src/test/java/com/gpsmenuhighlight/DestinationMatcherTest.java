package com.gpsmenuhighlight;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DestinationMatcherTest
{
	@Test
	public void portalPlaceOnlyAcceptsHousePortalLabels()
	{
		assertEquals("ardougne", DestinationMatcher.portalPlace("Ardougne Portal"));
		assertEquals("apeatolldungeon", DestinationMatcher.portalPlace("Ape Atoll Dungeon Portal"));
		assertEquals("kourendcastle", DestinationMatcher.portalPlace("Kourend Portal"));
		assertEquals("forgottencemetery", DestinationMatcher.portalPlace("Cemetery Portal"));
		assertEquals("respawn", DestinationMatcher.portalPlace("Respawn Portal (Lumbridge)"));
		// Shortest Path adds the nexus key in front once it has read the menu
		assertEquals("ardougne", DestinationMatcher.portalPlace("C: Ardougne Portal"));
		assertEquals("weiss", DestinationMatcher.portalPlace("F2: Weiss Portal"));
		assertEquals("annakarl", DestinationMatcher.portalPlace("G : Annakarl Portal"));
		// spells, items and jewellery are not nexus destinations
		assertEquals("", DestinationMatcher.portalPlace("Varrock Teleport"));
		assertEquals("", DestinationMatcher.portalPlace("Varrock tablet"));
		assertEquals("", DestinationMatcher.portalPlace("Max cape: Crafting Guild"));
		assertEquals("", DestinationMatcher.portalPlace("Skills necklace: Fishing Guild"));
		assertEquals("", DestinationMatcher.portalPlace("2: Castle Wars"));
		assertEquals("", DestinationMatcher.portalPlace(null));
		assertEquals("", DestinationMatcher.portalPlace("Portal"));
	}

	@Test
	public void rowNamesHandleEveryShortcutPrefixFormat()
	{
		assertEquals(Arrays.asList("ardougne"), DestinationMatcher.rowNames("<col=ffffff>3</col> :  Ardougne"));
		assertEquals(Arrays.asList("ardougne"), DestinationMatcher.rowNames("<col=ffffff>3:</col> Ardougne"));
		assertEquals(Arrays.asList("ardougne"), DestinationMatcher.rowNames("<col=ffffff>F1</col> :  Ardougne"));
		assertEquals(Arrays.asList("ardougne"), DestinationMatcher.rowNames("<col=ffffff>Ctrl+A</col> : Ardougne"));
		assertEquals(Arrays.asList("ardougne"), DestinationMatcher.rowNames("<col=ffffff>A B</col> : Ardougne"));
		assertEquals(Arrays.asList("ardougne"), DestinationMatcher.rowNames("Ardougne"));
		assertEquals(Arrays.asList("ardougne"), DestinationMatcher.rowNames("<str><col=808080>3 : Ardougne</col></str>"));
		assertEquals(Arrays.asList("alkharid"), DestinationMatcher.rowNames("Al Kharid"));
		assertTrue(DestinationMatcher.rowNames(null).isEmpty());
		assertTrue(DestinationMatcher.rowNames("").isEmpty());
		assertTrue(DestinationMatcher.rowNames("<col=ffffff>3</col> : ").isEmpty());
	}

	@Test
	public void rowsWithAnAlternateNameAnswerToBoth()
	{
		assertEquals(Arrays.asList("canifiskharyrll", "canifis", "kharyrll"),
			DestinationMatcher.rowNames("<col=ffffff>5</col> : Canifis (Kharyrll)"));
		assertTrue(DestinationMatcher.matchesNexusRow("Kharyrll Portal", "5 : Canifis (Kharyrll)"));
		assertTrue(DestinationMatcher.matchesNexusRow("Senntisten Portal", "6 : Exam Centre (Senntisten)"));
		assertTrue(DestinationMatcher.matchesNexusRow("Carrallangar Portal", "7 : Carrallanger"));
	}

	@Test
	public void matchesNexusRowsAgainstPortalLabelsOnly()
	{
		assertTrue(DestinationMatcher.matchesNexusRow("Ardougne Portal", "<col=ffffff>3</col> :  Ardougne"));
		assertTrue(DestinationMatcher.matchesNexusRow("Annakarl Portal", "Annakarl"));
		assertTrue(DestinationMatcher.matchesNexusRow("Kourend Portal", "8 : Kourend Castle"));
		assertFalse(DestinationMatcher.matchesNexusRow("Varrock Teleport", "<col=ffffff>1</col> :  Varrock"));
		assertFalse(DestinationMatcher.matchesNexusRow("Varrock tablet", "<col=ffffff>1</col> :  Varrock"));
		assertFalse(DestinationMatcher.matchesNexusRow("Skills necklace: Fishing Guild", "9 : Fishing Guild"));
		assertFalse(DestinationMatcher.matchesNexusRow("Ardougne Portal", ""));
		assertFalse(DestinationMatcher.matchesNexusRow("Ardougne Portal", null));
		assertFalse(DestinationMatcher.matchesNexusRow(null, "Ardougne"));
		assertFalse(DestinationMatcher.matchesNexusRow("Ardougne Portal", "East Ardougne"));
	}

	@Test
	public void findNexusRowUsesTheEarliestRouteStepThatHasARow()
	{
		List<String> rows = Arrays.asList("1 :  Varrock", "2 :  Lumbridge", "3 :  Ardougne", "4 :  Camelot");
		assertEquals(2, DestinationMatcher.findNexusRow(Arrays.asList("Max cape: Tele to POH", "Ardougne Portal"), rows));
		assertEquals(3, DestinationMatcher.findNexusRow(Arrays.asList("Camelot Portal", "Ardougne Portal"), rows));
		assertEquals(-1, DestinationMatcher.findNexusRow(Arrays.asList("Varrock Teleport"), rows));
		assertEquals(-1, DestinationMatcher.findNexusRow(Arrays.asList("Fairy ring AKQ"), rows));
		assertEquals(-1, DestinationMatcher.findNexusRow(Collections.<String>emptyList(), rows));
		assertEquals(-1, DestinationMatcher.findNexusRow(Arrays.asList("Ardougne Portal"), Collections.<String>emptyList()));
	}

	@Test
	public void jewelleryBoxLabelsMatchTheirRowsOnly()
	{
		assertEquals("castlewars", DestinationMatcher.boxPlace("2: Castle Wars"));
		assertEquals("warriorsguild", DestinationMatcher.boxPlace("A: Warriors' Guild"));
		assertEquals("", DestinationMatcher.boxPlace("Ardougne Portal"));
		assertEquals("", DestinationMatcher.boxPlace("Max cape: Crafting Guild"));
		assertEquals("", DestinationMatcher.boxPlace("Edgeville"));
		assertEquals("", DestinationMatcher.boxPlace(null));

		assertTrue(DestinationMatcher.matchesBoxRow("2: Castle Wars", "<col=ccccff>2:</col> Castle Wars"));
		assertTrue(DestinationMatcher.matchesBoxRow("G: Crafting Guild", "<col=ccccff>G</col> : Crafting Guild"));
		assertTrue(DestinationMatcher.matchesBoxRow("O: Edgeville", "Edgeville"));
		assertFalse(DestinationMatcher.matchesBoxRow("Ardougne Portal", "3 : Ardougne"));
		assertFalse(DestinationMatcher.matchesBoxRow("Max cape: Crafting Guild", "G : Crafting Guild"));
		assertFalse(DestinationMatcher.matchesNexusRow("E: Fishing Guild", "H : Fishing Guild"));

		List<String> rows = Arrays.asList("1: Emir's Arena", "2: Castle Wars", "3: Ferox Enclave");
		assertEquals(1, DestinationMatcher.findBoxRow(Arrays.asList("Max cape: Tele to POH", "2: Castle Wars"), rows));
		assertEquals(-1, DestinationMatcher.findBoxRow(Arrays.asList("Annakarl Portal"), rows));
		assertEquals(-1, DestinationMatcher.findBoxRow(Collections.<String>emptyList(), rows));
	}

	/** The rows and the route a real client logged on 2026-10-05. */
	@Test
	public void realNexusRowsFromTheClientLog()
	{
		List<String> rows = Arrays.asList(
			"<col=ffffff>1</col> :  Civitas illa Fortis",
			"<col=ffffff>8</col> :  Kourend Castle",
			"<col=ffffff>B</col> :  Graveyard of Shadows (Carrallanger)",
			"<col=ffffff>C</col> :  Ardougne",
			"<col=ffffff>D</col> :  Exam Centre (Senntisten)",
			"<col=ffffff>G</col> :  Demonic Ruins (Annakarl)",
			"<col=ffffff>Q</col> :  West Ardougne",
			"<col=ffffff>T</col> :  Cemetery",
			"<col=ffffff>Z</col> :  Boat",
			"Respawn",
			"Crazy Archaeologist (Dareeyak)");
		List<String> route = Arrays.asList("Max cape: Tele to POH", "Annakarl Portal", "Annakarl Portal");
		assertEquals(5, DestinationMatcher.findNexusRow(route, rows));
		assertEquals(3, DestinationMatcher.findNexusRow(Arrays.asList("Ardougne Portal"), rows));
		assertEquals(1, DestinationMatcher.findNexusRow(Arrays.asList("Kourend Portal"), rows));
		assertEquals(10, DestinationMatcher.findNexusRow(Arrays.asList("Dareeyak Portal"), rows));
		assertEquals(2, DestinationMatcher.findNexusRow(Arrays.asList("Carrallangar Portal"), rows));
	}

	@Test
	public void itemLabelsSplitIntoItemAndPlace()
	{
		assertEquals("amuletofglory", DestinationMatcher.itemOf("Amulet of glory: Edgeville"));
		assertEquals("edgeville", DestinationMatcher.itemPlace("Amulet of glory: Edgeville"));
		assertEquals("xericstalisman", DestinationMatcher.itemOf("Xeric's talisman: 4. Xeric's Heart"));
		assertEquals("xericsheart", DestinationMatcher.itemPlace("Xeric's talisman: 4. Xeric's Heart"));
		assertEquals("maxcape", DestinationMatcher.itemOf("Max cape: Fishing Teleports: Fishing Guild"));
		assertEquals("fishingguild", DestinationMatcher.itemPlace("Max cape: Fishing Teleports: Fishing Guild"));
		// not item labels
		assertEquals("", DestinationMatcher.itemOf("2: Castle Wars"));
		assertEquals("", DestinationMatcher.itemPlace("2: Castle Wars"));
		assertEquals("", DestinationMatcher.itemOf("Ardougne Portal"));
		assertEquals("", DestinationMatcher.itemOf(null));
	}

	@Test
	public void itemMenuOptionsMatchOnlyForTheSameItem()
	{
		String glory = "<col=ff9040>Amulet of glory(2)</col>";
		assertTrue(DestinationMatcher.matchesItemOption("Amulet of glory: Edgeville", glory, "Edgeville"));
		assertFalse(DestinationMatcher.matchesItemOption("Amulet of glory: Edgeville", glory, "Karamja"));
		assertFalse(DestinationMatcher.matchesItemOption("Amulet of glory: Edgeville", glory, "Rub"));
		assertFalse(DestinationMatcher.matchesItemOption("Amulet of glory: Edgeville", glory, ""));
		assertFalse(DestinationMatcher.matchesItemOption("Amulet of glory: Edgeville", glory, null));
		// another item with the same destination name must not light up
		assertFalse(DestinationMatcher.matchesItemOption("Amulet of glory: Edgeville",
			"<col=ff9040>Combat bracelet(4)</col>", "Edgeville"));
		assertFalse(DestinationMatcher.matchesItemOption("Ardougne Portal", glory, "Edgeville"));
		// shortened option text
		assertTrue(DestinationMatcher.matchesItemOption("Xeric's talisman: 4. Xeric's Heart",
			"<col=ff9040>Xeric's talisman</col>", "Xeric's Heart"));
		assertTrue(DestinationMatcher.matchesItemOption("Xeric's talisman: 4. Xeric's Heart",
			"<col=ff9040>Xeric's talisman</col>", "Heart"));
		assertTrue(DestinationMatcher.isItemOfStep("Amulet of glory: Edgeville", glory));
		assertFalse(DestinationMatcher.isItemOfStep("Amulet of glory: Edgeville", "<col=ff9040>Spade</col>"));
		assertFalse(DestinationMatcher.isItemOfStep("2: Castle Wars", glory));
	}

	@Test
	public void recolourReplacesTheGamesOwnColourTags()
	{
		assertEquals("<col=00ff00>3 :  Ardougne</col>",
			DestinationMatcher.recolour("<col=ffffff>3</col> :  Ardougne", "00ff00"));
		assertEquals("<col=00ff00>Ardougne</col>", DestinationMatcher.recolour("Ardougne", "00ff00"));
		assertEquals("<col=00ff00><shad=000000>3 : Ardougne</shad></col>",
			DestinationMatcher.recolour("<shad=000000><col=FFFFFF>3</col> : Ardougne</shad>", "00ff00"));
		assertEquals("", DestinationMatcher.recolour("", "00ff00"));
		// recolouring twice gives the same text, and the place name still matches afterwards
		String once = DestinationMatcher.recolour("<col=ffffff>3</col> :  Ardougne", "00ff00");
		assertEquals(once, DestinationMatcher.recolour(once, "00ff00"));
		assertTrue(DestinationMatcher.matchesNexusRow("Ardougne Portal", once));
	}

	@Test
	public void routeStepsParseDefensivelyAndExpire()
	{
		Map<String, Object> data = new HashMap<>();
		data.put("displayInfo", Arrays.asList("Max cape: Tele to POH", null, "", 5, "Ardougne Portal"));
		assertEquals(Arrays.asList("Max cape: Tele to POH", "Ardougne Portal"), RouteSteps.parse(data));
		assertTrue(RouteSteps.parse(null).isEmpty());
		assertTrue(RouteSteps.parse(new HashMap<>()).isEmpty());
		data.put("displayInfo", "not a list");
		assertTrue(RouteSteps.parse(data).isEmpty());

		RouteSteps steps = new RouteSteps();
		assertTrue(steps.current(1_000, 60_000).isEmpty());
		data.put("displayInfo", Arrays.asList("Ardougne Portal"));
		steps.accept(data, 1_000);
		assertEquals(1, steps.current(30_000, 60_000).size());
		assertTrue(steps.current(70_000, 60_000).isEmpty());
		assertEquals(1, steps.current(999_999, 0).size());
		steps.clear();
		assertTrue(steps.current(1_000, 0).isEmpty());
	}

	@Test
	public void diaryCapeLabelNamesTheNpcAndTheMenuNamesTheRegion()
	{
		String label = "Achievement diary cape: B. Hatius Cosaintus";
		String target = "<col=ff9040>Achievement diary cape</col>";
		assertTrue(DestinationMatcher.matchesItemOption(label, target, "Lumbridge & Draynor"));
		assertFalse(DestinationMatcher.matchesItemOption(label, target, "Varrock"));
		assertTrue(DestinationMatcher.matchesItemOption("Achievement diary cape: Kaleb Paramaya", target, "Karamja (Shilo)"));
		assertFalse(DestinationMatcher.matchesItemOption("Achievement diary cape: Kaleb Paramaya", target, "Karamja"));
		assertTrue(DestinationMatcher.matchesItemOption("Achievement diary cape: Twiggy O'Korn", target, "Twiggy O'Korn"));
	}

	@Test
	public void maxCapeTeleToPohIsTheHomeOptionOnTheFirstLevelOnly()
	{
		String label = "Max cape: Tele to POH";
		String target = "<col=ff9040>Max cape</col>";
		assertTrue(DestinationMatcher.matchesTopLevelItemOption(label, target, "Home"));
		assertFalse(DestinationMatcher.matchesTopLevelItemOption(label, target, "Crafting Guild"));
		assertFalse(DestinationMatcher.matchesItemOption(label, target, "Home"));
		assertFalse(DestinationMatcher.matchesTopLevelItemOption("Max cape: Fishing Guild", target, "Home"));
	}

	@Test
	public void aPluralInTheMenuStillMatches()
	{
		String label = "Max cape: Other Teleports: Black chinchompa";
		String target = "<col=ff9040>Max cape</col>";
		assertTrue(DestinationMatcher.matchesItemOption(label, target, "Black chinchompas"));
		assertFalse(DestinationMatcher.matchesItemOption(label, target, "Carnivorous chinchompas"));
	}

	@Test
	public void scrollBookLightsOpenAndTheRowNotTheDefaultTeleport()
	{
		String label = "Master scroll book: Feldip hills teleport";
		String target = "<col=ff9040>Master scroll book</col>";
		assertFalse(DestinationMatcher.matchesItemOption(label, target, "Teleport"));
		assertTrue(DestinationMatcher.matchesTopLevelItemOption(label, target, "Open"));
		assertFalse(DestinationMatcher.matchesTopLevelItemOption(label, target, "Teleport"));
		assertTrue(DestinationMatcher.matchesScrollBookRow(label, "<col=FF981F>Feldip Hills teleport scroll"));
		assertFalse(DestinationMatcher.matchesScrollBookRow(label, "<col=FF981F>Spider Cave teleport scroll"));
		assertFalse(DestinationMatcher.matchesScrollBookRow("Max cape: Crafting Guild", "<col=FF981F>Feldip Hills teleport scroll"));
		assertTrue(DestinationMatcher.matchesItemOption("Xeric's talisman: 4. Xeric's Heart", "<col=ff9040>Xeric's talisman</col>", "Xeric's Heart"));
	}

	@Test
	public void theTeleportsSubMenuOfACarriedCapeIsAFlatList()
	{
		assertTrue(DestinationMatcher.isFlatTeleportList("Teleports"));
		assertTrue(DestinationMatcher.isFlatTeleportList("<col=00ff00>Teleports</col>"));
		assertFalse(DestinationMatcher.isFlatTeleportList("POH Portals"));
		assertFalse(DestinationMatcher.isFlatTeleportList("Guild Teleports"));
		assertFalse(DestinationMatcher.isFlatTeleportList(null));
	}

	@Test
	public void jewelleryBoxRowsNamedDifferentlyFromTheRoute()
	{
		assertTrue(DestinationMatcher.matchesBoxRow("C: Edgeville Monastery", "<col=ccccff>C:</col> Monastery"));
		assertTrue(DestinationMatcher.matchesBoxRow("8: Chasm of Tears", "<col=ccccff>8:</col> Tears of Guthix"));
		assertFalse(DestinationMatcher.matchesBoxRow("C: Edgeville Monastery", "<col=ccccff>O:</col> Edgeville"));
	}
}
