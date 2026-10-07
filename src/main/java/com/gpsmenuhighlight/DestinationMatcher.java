/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.gpsmenuhighlight;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Compares the text a pathfinder publishes for a house-portal route step (for example
 * "Ardougne Portal") with the text of a row in the Portal Nexus menu (for example
 * "<col=ffffff>3</col> : Ardougne"). Pure string handling.
 */
final class DestinationMatcher
{
	private static final Pattern TAG = Pattern.compile("<[^>]*>");
	/** A route step is a house portal / nexus destination when its label is "<place> Portal". */
	/**
	 * Shortest Path prefixes a nexus destination with its menu key once it has seen the menu,
	 * for example "C: Ardougne Portal" or "F2: Weiss Portal".
	 */
	private static final Pattern LABEL_KEY = Pattern.compile("^(?:[0-9A-Za-z]|F(?:[1-9]|10))\\s*:\\s+");
	private static final Pattern PORTAL_LABEL = Pattern.compile("^(.+?)\\s+Portal(\\s*\\(.*\\))?$", Pattern.CASE_INSENSITIVE);
	/** A jewellery box destination is published as "<key>: <place>", for example "2: Castle Wars". */
	private static final Pattern BOX_LABEL = Pattern.compile("^[0-9A-Za-z]:\\s+(.+)$");
	private static final Pattern PARENTHESISED = Pattern.compile("^(.*?)\\s*\\((.*?)\\)\\s*$");
	private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");
	/** A shortcut prefix ends at the first ':' and is short ("1", "A", "F1", "Ctrl+A"). */
	private static final int MAX_KEY_PREFIX = 14;

	/** Pathfinder name -> nexus name, where the two differ (both already reduced by {@link #key}). */
	private static final Map<String, String> ALIASES = new HashMap<>();

	static
	{
		ALIASES.put("kourend", "kourendcastle");
		// jewellery box: the pathfinder's name against the row's
		ALIASES.put("edgevillemonastery", "monastery");
		ALIASES.put("chasmoftears", "tearsofguthix");
		ALIASES.put("cemetery", "forgottencemetery");
		ALIASES.put("carrallangar", "carrallanger");
		ALIASES.put("carralanger", "carrallanger");
		ALIASES.put("carralangar", "carrallanger");
	}

	/**
	 * Item teleports whose route label names the arrival NPC while the menu names the region:
	 * the Achievement diary cape ("Achievement diary cape: B. Hatius Cosaintus" against the
	 * option "Lumbridge & Draynor"). Both sides already reduced by {@link #key}.
	 */
	private static final Map<String, String> PLACE_OPTIONS = new HashMap<>();

	static
	{
		PLACE_OPTIONS.put("twopints", "ardougne");
		PLACE_OPTIONS.put("jarr", "desert");
		PLACE_OPTIONS.put("sirrebral", "falador");
		PLACE_OPTIONS.put("thorodin", "fremennik");
		PLACE_OPTIONS.put("flaxkeeper", "kandarin");
		PLACE_OPTIONS.put("piratejackiethefruit", "karamja");
		PLACE_OPTIONS.put("kalebparamaya", "karamjashilo");
		PLACE_OPTIONS.put("jungleforester", "karamjajungle");
		PLACE_OPTIONS.put("tzhaarmej", "karamjamorulrek");
		PLACE_OPTIONS.put("elise", "kourendkebos");
		PLACE_OPTIONS.put("hatiuscosaintus", "lumbridgedraynor");
		PLACE_OPTIONS.put("lesabr", "morytania");
		PLACE_OPTIONS.put("lesabre", "morytania");
		PLACE_OPTIONS.put("toby", "varrock");
		PLACE_OPTIONS.put("lesserfanatic", "wilderness");
		PLACE_OPTIONS.put("eldergnomechild", "westernprovinces");
	}

	/** Options that name an action, not a destination. */
	private static final java.util.Set<String> GENERIC_OPTIONS = new java.util.HashSet<>(
		java.util.Arrays.asList("teleport", "open", "rub", "check", "wear", "wield", "remove"));
	private static final String SCROLL_BOOK = "masterscrollbook";

	private DestinationMatcher()
	{
	}

	/**
	 * Whether a row of the Master scroll book is the destination of a route step. The row is
	 * named after its scroll ("Feldip Hills teleport scroll"), the step after the teleport
	 * ("Master scroll book: Feldip hills teleport").
	 */
	static boolean matchesScrollBookRow(String routeLabel, String rowName)
	{
		if (rowName == null || !SCROLL_BOOK.equals(itemOf(routeLabel)))
		{
			return false;
		}
		String place = itemPlace(routeLabel);
		String row = key(TAG.matcher(rowName).replaceAll(""));
		return !place.isEmpty() && (row.equals(place + "scroll") || row.equals(place + "sscroll")
			|| (place + "scroll").equals(row.replace("cavesteleport", "caveteleport")));
	}

	/** Lower case, letters and digits only. */
	static String key(String text)
	{
		if (text == null)
		{
			return "";
		}
		String k = NON_ALNUM.matcher(text.toLowerCase(Locale.ENGLISH)).replaceAll("");
		String alias = ALIASES.get(k);
		return alias != null ? alias : k;
	}

	/**
	 * The place a route step teleports to through a house portal or the nexus, or "" when the
	 * step is something else (a spell, an item, a jewellery box...), so those never light a row.
	 */
	static String portalPlace(String routeLabel)
	{
		if (routeLabel == null)
		{
			return "";
		}
		String label = LABEL_KEY.matcher(TAG.matcher(routeLabel).replaceAll("").trim()).replaceFirst("");
		Matcher m = PORTAL_LABEL.matcher(label);
		return m.matches() ? key(m.group(1)) : "";
	}

	/**
	 * The place a route step teleports to through the house jewellery box, or "" when the step
	 * is something else.
	 */
	static String boxPlace(String routeLabel)
	{
		if (routeLabel == null)
		{
			return "";
		}
		Matcher m = BOX_LABEL.matcher(TAG.matcher(routeLabel).replaceAll("").trim());
		return m.matches() ? key(m.group(1)) : "";
	}

	/**
	 * The names a nexus row answers to: the text after the shortcut prefix, and, for a row
	 * written "Canifis (Kharyrll)", each of the two names as well.
	 */
	static List<String> rowNames(String rowText)
	{
		List<String> names = new ArrayList<>();
		if (rowText == null)
		{
			return names;
		}
		String s = TAG.matcher(rowText).replaceAll("").replace((char) 160, ' ').trim();
		int colon = s.indexOf(':');
		if (colon >= 0 && colon <= MAX_KEY_PREFIX)
		{
			s = s.substring(colon + 1).trim();
		}
		add(names, key(s));
		Matcher m = PARENTHESISED.matcher(s);
		if (m.matches())
		{
			add(names, key(m.group(1)));
			add(names, key(m.group(2)));
		}
		return names;
	}

	private static void add(List<String> names, String name)
	{
		if (!name.isEmpty() && !names.contains(name))
		{
			names.add(name);
		}
	}

	static boolean matchesNexusRow(String routeLabel, String rowText)
	{
		String place = portalPlace(routeLabel);
		return !place.isEmpty() && rowNames(rowText).contains(place);
	}

	static boolean matchesBoxRow(String routeLabel, String rowText)
	{
		String place = boxPlace(routeLabel);
		return !place.isEmpty() && rowNames(rowText).contains(place);
	}

	/**
	 * @param routeLabels the route's steps in travel order
	 * @param rowTexts    the nexus rows
	 * @return the index of the row used by the earliest matching route step, or -1
	 */
	static int findNexusRow(List<String> routeLabels, List<String> rowTexts)
	{
		for (String label : routeLabels)
		{
			for (int i = 0; i < rowTexts.size(); i++)
			{
				if (matchesNexusRow(label, rowTexts.get(i)))
				{
					return i;
				}
			}
		}
		return -1;
	}

	/** As {@link #findNexusRow}, for the rows of the jewellery box menu. */
	static int findBoxRow(List<String> routeLabels, List<String> rowTexts)
	{
		for (String label : routeLabels)
		{
			for (int i = 0; i < rowTexts.size(); i++)
			{
				if (matchesBoxRow(label, rowTexts.get(i)))
				{
					return i;
				}
			}
		}
		return -1;
	}

	/**
	 * For an item teleport published as "<item>: <place>" (for example
	 * "Amulet of glory: Edgeville" or "Xeric's talisman: 4. Xeric's Heart"), the item part,
	 * reduced by {@link #key}; "" when the label has no item part.
	 */
	static String itemOf(String routeLabel)
	{
		if (routeLabel == null)
		{
			return "";
		}
		String s = TAG.matcher(routeLabel).replaceAll("").trim();
		int colon = s.indexOf(": ");
		// one character before the colon is a shortcut key ("2: Castle Wars"), not an item
		return colon > 1 ? key(s.substring(0, colon)) : "";
	}

	/** The place part of an item teleport label, without a leading "4. " style shortcut. */
	static String itemPlace(String routeLabel)
	{
		if (routeLabel == null || itemOf(routeLabel).isEmpty())
		{
			return "";
		}
		String s = TAG.matcher(routeLabel).replaceAll("").trim();
		s = s.substring(s.lastIndexOf(": ") + 2).trim();
		s = s.replaceFirst("^[0-9A-Za-z]\\.\\s+", "");
		return key(s);
	}

	/**
	 * Whether a right-click option is the destination of an item teleport step.
	 *
	 * @param menuTarget the item the menu belongs to, for example "<col=ff9040>Amulet of glory(2)</col>"
	 * @param option     the option text, for example "Edgeville"
	 */
	static boolean matchesItemOption(String routeLabel, String menuTarget, String option)
	{
		String item = itemOf(routeLabel);
		String place = itemPlace(routeLabel);
		if (item.isEmpty() || place.isEmpty() || menuTarget == null || option == null)
		{
			return false;
		}
		String target = key(TAG.matcher(menuTarget).replaceAll(""));
		if (!target.startsWith(item))
		{
			return false;
		}
		String opt = key(TAG.matcher(option).replaceAll(""));
		if (opt.isEmpty())
		{
			return false;
		}
		if (opt.equals(PLACE_OPTIONS.get(place)))
		{
			return true;
		}
		// "Black chinchompa" in the label is "Black chinchompas" in the menu
		if (opt.equals(place + "s") || place.equals(opt + "s"))
		{
			return true;
		}
		// "Xeric's Heart" in the label may be just "Heart" in the menu; but "Teleport" on a scroll
		// book is its default destination, not the "... teleport" the label ends with
		return opt.equals(place) || (opt.length() >= 4 && place.endsWith(opt) && !GENERIC_OPTIONS.contains(opt));
	}

	/**
	 * As {@link #matchesItemOption}, for options that only count on the item's first menu level:
	 * "Max cape: Tele to POH" is the option "Home" there, while "Home" inside "POH Portals"
	 * is the portal outside the house.
	 */
	static boolean matchesTopLevelItemOption(String routeLabel, String menuTarget, String option)
	{
		if (menuTarget == null || option == null || !isItemOfStep(routeLabel, menuTarget))
		{
			return false;
		}
		String opt = key(TAG.matcher(option).replaceAll(""));
		// a scroll book destination is picked inside the book
		if (SCROLL_BOOK.equals(itemOf(routeLabel)))
		{
			return opt.equals("open");
		}
		return "teletopoh".equals(itemPlace(routeLabel)) && (opt.equals("home") || opt.equals("teletopoh"));
	}

	/**
	 * Whether a sub-menu is the single list an item shows when it is carried rather than worn
	 * ("Teleports" on a Max cape in the inventory), where first-level options such as "Home" live.
	 */
	static boolean isFlatTeleportList(String option)
	{
		return option != null && "teleports".equals(key(TAG.matcher(option).replaceAll("")));
	}

	/** Whether a menu belongs to the item some route step uses. */
	static boolean isItemOfStep(String routeLabel, String menuTarget)
	{
		String item = itemOf(routeLabel);
		return !item.isEmpty() && menuTarget != null
			&& key(TAG.matcher(menuTarget).replaceAll("")).startsWith(item);
	}

	/** Puts {@code hex} (rrggbb) on a row's text, replacing the colour tags the game put there. */
	static String recolour(String text, String hex)
	{
		if (text == null || text.isEmpty())
		{
			return text;
		}
		String stripped = text.replaceAll("(?i)<col=[0-9a-f]+>", "").replaceAll("(?i)</col>", "");
		return "<col=" + hex + ">" + stripped + "</col>";
	}
}
