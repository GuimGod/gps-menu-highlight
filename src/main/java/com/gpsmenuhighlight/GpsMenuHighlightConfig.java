/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.gpsmenuhighlight;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(GpsMenuHighlightConfig.GROUP)
public interface GpsMenuHighlightConfig extends Config
{
	String GROUP = "gpsmenuhighlight";

	@ConfigItem(
		keyName = "highlightNexus",
		name = "Portal Nexus",
		description = "In the Portal Nexus menu, colour the destination your current route uses. Needs 'Post transports' enabled in the GPS (or Shortest Path) plugin settings.",
		position = 0
	)
	default boolean highlightNexus()
	{
		return true;
	}

	@ConfigItem(
		keyName = "highlightJewelleryBox",
		name = "Jewellery box",
		description = "In the house jewellery box menu, colour the destination your current route uses.",
		position = 0
	)
	default boolean highlightJewelleryBox()
	{
		return true;
	}

	@ConfigItem(
		keyName = "highlightItemMenus",
		name = "Item right-click menus",
		description = "In the right-click menu of a teleport item (glory, max cape and so on), colour the destination your current route uses.",
		position = 0
	)
	default boolean highlightItemMenus()
	{
		return true;
	}

	@ConfigItem(
		keyName = "highlightColour",
		name = "Highlight colour",
		description = "Colour of the highlighted destination.",
		position = 1
	)
	default Color highlightColour()
	{
		return Color.GREEN;
	}

	@ConfigItem(
		keyName = "scrollToRow",
		name = "Scroll to it",
		description = "Scroll the list to the highlighted destination when the menu opens.",
		position = 2
	)
	default boolean scrollToRow()
	{
		return true;
	}

	@Range(min = 0, max = 120)
	@Units(Units.MINUTES)
	@ConfigItem(
		keyName = "routeMaxAge",
		name = "Forget route after",
		description = "Stop highlighting when the last route received is older than this (0 = never). The pathfinder does not announce a cleared route.",
		position = 3
	)
	default int routeMaxAge()
	{
		return 10;
	}

	@ConfigItem(
		keyName = "diagnosticLogging",
		name = "Diagnostic logging",
		description = "Write the route steps and the menu rows to the client log (debug level) when a menu opens. For troubleshooting names that do not match.",
		position = 10
	)
	default boolean diagnosticLogging()
	{
		return false;
	}
}
