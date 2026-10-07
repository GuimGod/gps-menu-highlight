/*
 * Copyright (c) 2021, Runemoro, Skretzo and the Shortest Path contributors
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 *
 * WHAT THIS PLUGIN DOES
 * ---------------------
 * The GPS and Shortest Path hub plugins can publish the transports of the route they are
 * showing (their "Post transports" option). This plugin listens for that and, when the
 * Portal Nexus or the house jewellery box menu is open, colours the row of the destination
 * the route uses; in the nexus it also scrolls the list to it.
 *
 * PROVENANCE: the mark-and-scroll step follows FairyRingHighlighter.java of the GPS plugin
 * (https://github.com/PauloAguiar/runelite-gps-plugin, commit
 * 4345772359ed7f364034dc5325c96ab425b79251, BSD 2-Clause), which does the same for the
 * fairy ring log: find the row by its text, recolour it, set the scroll position and run
 * the scrollbar update script.
 *
 * It only changes the colour of a text and the scroll position of a list. It never clicks,
 * selects, hides, reorders or adds anything, does not change click areas or shortcut keys,
 * and does no network or file access.
 */
package com.gpsmenuhighlight;

import com.google.inject.Provides;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Menu;
import net.runelite.api.MenuEntry;
import net.runelite.api.ScriptID;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.PostClientTick;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "GPS Menu Highlight",
	description = "Highlights, in the Portal Nexus menu, the destination your GPS / Shortest Path route uses",
	tags = {"gps", "shortest path", "teleport", "nexus", "highlight", "poh"}
)
public class GpsMenuHighlightPlugin extends Plugin
{
	private static final String NAMESPACE_GPS = "gps";
	private static final String NAMESPACE_SHORTEST_PATH = "shortestpath";
	private static final String ACTION_TRANSPORTS = "transports";
	private static final String ACTION_CLEAR = "clear";

	/** The two text layers of the nexus list: the main destinations and the alternate ones. */
	private static final int[] NEXUS_TEXT_LAYERS = {
		InterfaceID.TelenexusTeleport.TEXT1,
		InterfaceID.TelenexusTeleport.EXTRAS
	};

	@Inject
	private Client client;

	@Inject
	private GpsMenuHighlightConfig config;

	/** The six groups of the jewellery box menu. */
	private static final int[] BOX_TEXT_LAYERS = {
		InterfaceID.PohJewelleryBox.DUELING,
		InterfaceID.PohJewelleryBox.GAMING,
		InterfaceID.PohJewelleryBox.COMBAT,
		InterfaceID.PohJewelleryBox.SKILLS,
		InterfaceID.PohJewelleryBox.WEALTH,
		InterfaceID.PohJewelleryBox.GLORY
	};

	private final RouteSteps route = new RouteSteps();
	/** The Master scroll book interface (learned from the client log; its rows are named after their scrolls). */
	private static final int SCROLL_BOOK_GROUP = 597;
	private static final int SCROLL_BOOK_CHILDREN = 120;
	private boolean bookOpen;
	private Widget bookMarked;
	private int bookOriginalColour;
	private boolean nexusOpen;
	private boolean boxOpen;
	private boolean scrolledThisOpen;
	private boolean loggedThisOpen;
	/** The row currently recoloured, with the text it had before and the text we gave it. */
	private Widget markedRow;
	private String markedOriginal;
	private String markedText;

	@Inject
	private ClientThread clientThread;

	@Provides
	GpsMenuHighlightConfig provideConfig(ConfigManager manager)
	{
		return manager.getConfig(GpsMenuHighlightConfig.class);
	}

	@Override
	protected void startUp()
	{
		route.clear();
		nexusOpen = false;
	}

	@Override
	protected void shutDown()
	{
		route.clear();
		nexusOpen = false;
		boxOpen = false;
		clientThread.invokeLater(this::unmark);
	}

	@Subscribe
	public void onPluginMessage(PluginMessage message)
	{
		if (!NAMESPACE_GPS.equals(message.getNamespace()) && !NAMESPACE_SHORTEST_PATH.equals(message.getNamespace()))
		{
			return;
		}
		if (ACTION_TRANSPORTS.equals(message.getName()))
		{
			route.accept(message.getData(), System.currentTimeMillis());
			if (config.diagnosticLogging())
			{
				log.debug("GPS Menu Highlight route steps: {}", RouteSteps.parse(message.getData()));
			}
		}
		else if (ACTION_CLEAR.equals(message.getName()))
		{
			route.clear();
		}
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() == InterfaceID.TELENEXUS_TELEPORT)
		{
			nexusOpen = true;
			scrolledThisOpen = false;
			loggedThisOpen = false;
		}
		else if (event.getGroupId() == InterfaceID.POH_JEWELLERY_BOX)
		{
			boxOpen = true;
			loggedThisOpen = false;
		}
		else if (event.getGroupId() == SCROLL_BOOK_GROUP)
		{
			bookOpen = true;
		}
	}

	@Subscribe
	public void onWidgetClosed(WidgetClosed event)
	{
		if (event.getGroupId() == InterfaceID.TELENEXUS_TELEPORT)
		{
			nexusOpen = false;
			forgetMark();
		}
		else if (event.getGroupId() == InterfaceID.POH_JEWELLERY_BOX)
		{
			boxOpen = false;
			forgetMark();
		}
		else if (event.getGroupId() == SCROLL_BOOK_GROUP)
		{
			bookOpen = false;
			bookMarked = null;
		}
	}

	/**
	 * Right-click menu of a teleport item: colours the destination option, and the option
	 * that opens the sub-menu holding it. Only the text colour of existing options changes;
	 * nothing is added, removed or reordered.
	 */
	@Subscribe
	public void onMenuOpened(MenuOpened event)
	{
		if (!config.highlightItemMenus())
		{
			return;
		}
		List<String> steps = route.current(System.currentTimeMillis(), config.routeMaxAge() * 60_000L);
		if (steps.isEmpty())
		{
			return;
		}
		String hex = hex(config.highlightColour());
		boolean logged = false;
		for (MenuEntry entry : event.getMenuEntries())
		{
			String target = entry.getTarget();
			if (target == null || target.isEmpty() || !usedByAnyStep(steps, target))
			{
				continue;
			}
			Menu sub = entry.getSubMenu();
			MenuEntry[] subEntries = sub == null ? null : sub.getMenuEntries();
			if (config.diagnosticLogging() && !logged)
			{
				logged = true;
				log.debug("GPS Menu Highlight item menu for {}; route steps in use: {}", target, steps);
			}
			if (config.diagnosticLogging())
			{
				List<String> subOptions = new ArrayList<>();
				if (subEntries != null)
				{
					for (MenuEntry subEntry : subEntries)
					{
						subOptions.add(subEntry.getOption());
					}
				}
				log.debug("GPS Menu Highlight item option '{}' sub-options {}", entry.getOption(), subOptions);
			}
			if (matchesAnyStep(steps, target, entry.getOption()) || matchesAnyStepTopLevel(steps, target, entry.getOption()))
			{
				entry.setOption(DestinationMatcher.recolour(entry.getOption(), hex));
			}
			if (subEntries == null)
			{
				continue;
			}
			boolean inside = false;
			// an unworn cape lists every destination in one "Teleports" sub-menu, "Home" included
			boolean flat = DestinationMatcher.isFlatTeleportList(entry.getOption());
			for (MenuEntry subEntry : subEntries)
			{
				if (matchesAnyStep(steps, target, subEntry.getOption())
					|| (flat && matchesAnyStepTopLevel(steps, target, subEntry.getOption())))
				{
					subEntry.setOption(DestinationMatcher.recolour(subEntry.getOption(), hex));
					inside = true;
				}
			}
			if (inside)
			{
				entry.setOption(DestinationMatcher.recolour(entry.getOption(), hex));
			}
		}
	}

	private static boolean usedByAnyStep(List<String> steps, String target)
	{
		for (String step : steps)
		{
			if (DestinationMatcher.isItemOfStep(step, target))
			{
				return true;
			}
		}
		return false;
	}

	private static boolean matchesAnyStepTopLevel(List<String> steps, String target, String option)
	{
		for (String step : steps)
		{
			if (DestinationMatcher.matchesTopLevelItemOption(step, target, option))
			{
				return true;
			}
		}
		return false;
	}

	private static boolean matchesAnyStep(List<String> steps, String target, String option)
	{
		for (String step : steps)
		{
			if (DestinationMatcher.matchesItemOption(step, target, option))
			{
				return true;
			}
		}
		return false;
	}

	// After each client tick while a menu is open, because the game rebuilds the rows.
	@Subscribe
	public void onPostClientTick(PostClientTick event)
	{
		if (bookOpen)
		{
			highlightScrollBook();
		}
		if (nexusOpen)
		{
			highlight(config.highlightNexus(), NEXUS_TEXT_LAYERS, true, "nexus");
		}
		else if (boxOpen)
		{
			highlight(config.highlightJewelleryBox(), BOX_TEXT_LAYERS, false, "jewellery box");
		}
	}

	/** Colours the name under the scroll the route uses, in the open Master scroll book. */
	private void highlightScrollBook()
	{
		if (client.getWidget(SCROLL_BOOK_GROUP, 0) == null)
		{
			bookOpen = false;
			bookMarked = null;
			return;
		}
		Widget wanted = null;
		if (config.highlightItemMenus())
		{
			List<String> steps = route.current(System.currentTimeMillis(), config.routeMaxAge() * 60_000L);
			for (int child = 0; child < SCROLL_BOOK_CHILDREN && wanted == null; child++)
			{
				Widget row = client.getWidget(SCROLL_BOOK_GROUP, child);
				if (row == null || row.getName() == null || row.getName().isEmpty())
				{
					continue;
				}
				for (String step : steps)
				{
					if (DestinationMatcher.matchesScrollBookRow(step, row.getName()))
					{
						wanted = scrollBookLabel(row);
						break;
					}
				}
			}
		}
		if (bookMarked != null && bookMarked != wanted)
		{
			bookMarked.setTextColor(bookOriginalColour);
			bookMarked = null;
		}
		if (wanted != null)
		{
			int colour = config.highlightColour().getRGB() & 0xFFFFFF;
			if (bookMarked == null)
			{
				bookOriginalColour = wanted.getTextColor();
				bookMarked = wanted;
			}
			if (wanted.getTextColor() != colour)
			{
				wanted.setTextColor(colour);
			}
		}
	}

	/** The destination name inside a scroll book row: its text that is not the scroll count. */
	private static Widget scrollBookLabel(Widget row)
	{
		Widget[][] kids = {row.getStaticChildren(), row.getNestedChildren(), row.getDynamicChildren()};
		for (Widget[] list : kids)
		{
			if (list == null)
			{
				continue;
			}
			for (Widget kid : list)
			{
				String text = kid == null ? null : kid.getText();
				if (text != null && !text.isEmpty() && !text.matches("[0-9,]+"))
				{
					return kid;
				}
			}
		}
		return null;
	}

	private void highlight(boolean enabled, int[] layerIds, boolean nexus, String menuName)
	{
		if (!enabled)
		{
			unmark();
			return;
		}
		List<String> steps = route.current(System.currentTimeMillis(), config.routeMaxAge() * 60_000L);

		List<Widget> rows = new ArrayList<>();
		List<String> texts = new ArrayList<>();
		for (int layerId : layerIds)
		{
			Widget layer = client.getWidget(layerId);
			if (layer == null || layer.isHidden())
			{
				continue;
			}
			collect(layer.getDynamicChildren(), rows, texts);
			collect(layer.getStaticChildren(), rows, texts);
			collect(layer.getNestedChildren(), rows, texts);
		}

		if (config.diagnosticLogging() && !loggedThisOpen && !rows.isEmpty())
		{
			loggedThisOpen = true;
			log.debug("GPS Menu Highlight {} rows: {}", menuName, texts);
			log.debug("GPS Menu Highlight route steps in use: {}", steps);
		}

		// Our own recoloured row must be compared by the text it had, so put the original back
		// in the list that is searched.
		if (markedRow != null)
		{
			int at = rows.indexOf(markedRow);
			if (at >= 0 && markedText != null && markedText.equals(texts.get(at)))
			{
				texts.set(at, markedOriginal);
			}
		}

		int index = -1;
		if (!steps.isEmpty() && !rows.isEmpty())
		{
			index = nexus ? DestinationMatcher.findNexusRow(steps, texts) : DestinationMatcher.findBoxRow(steps, texts);
		}
		if (index < 0)
		{
			unmark();
			return;
		}

		Widget row = rows.get(index);
		String hex = hex(config.highlightColour());
		if (row != markedRow || !row.getText().equals(markedText))
		{
			if (row != markedRow)
			{
				unmark();
				markedOriginal = texts.get(index);
			}
			// The row text carries the game's own colour tags, which win over setTextColor.
			markedText = DestinationMatcher.recolour(markedOriginal, hex);
			markedRow = row;
			row.setText(markedText);
		}

		if (nexus && config.scrollToRow() && !scrolledThisOpen && scrollTo(row))
		{
			scrolledThisOpen = true;
		}
	}

	private static void collect(Widget[] children, List<Widget> rows, List<String> texts)
	{
		if (children == null)
		{
			return;
		}
		for (Widget child : children)
		{
			if (child == null || child.isHidden())
			{
				continue;
			}
			String text = child.getText();
			if (text != null && !text.isEmpty())
			{
				rows.add(child);
				texts.add(text);
			}
		}
	}

	/** Gives the recoloured row its own text back, unless the game has rewritten it meanwhile. */
	private void unmark()
	{
		if (markedRow != null && markedText != null && markedText.equals(markedRow.getText()))
		{
			markedRow.setText(markedOriginal);
		}
		forgetMark();
	}

	private void forgetMark()
	{
		markedRow = null;
		markedOriginal = null;
		markedText = null;
	}

	/** Same steps as GPS's fairy ring log helper. Done once per opening so the player can scroll away. */
	private boolean scrollTo(Widget row)
	{
		if (row.getParentId() != InterfaceID.TelenexusTeleport.TEXT1)
		{
			// a row of the alternate list, which does not scroll
			return true;
		}
		Widget list = client.getWidget(InterfaceID.TelenexusTeleport.SCROLLING1);
		if (list == null || list.getHeight() <= 0)
		{
			// not laid out yet: try again next tick
			return false;
		}
		int max = list.getScrollHeight() - list.getHeight();
		if (max <= 0)
		{
			return true;
		}
		int scrollY = Math.max(0, Math.min(row.getRelativeY(), max));
		list.setScrollY(scrollY);
		list.revalidateScroll();
		client.runScript(ScriptID.UPDATE_SCROLLBAR, InterfaceID.TelenexusTeleport.SCROLLBAR1,
			InterfaceID.TelenexusTeleport.SCROLLING1, scrollY);
		return true;
	}

	private static String hex(Color colour)
	{
		return String.format("%06x", colour.getRGB() & 0xFFFFFF);
	}
}
