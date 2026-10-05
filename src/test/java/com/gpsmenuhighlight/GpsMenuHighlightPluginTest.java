package com.gpsmenuhighlight;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class GpsMenuHighlightPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(GpsMenuHighlightPlugin.class);
		RuneLite.main(args);
	}
}
