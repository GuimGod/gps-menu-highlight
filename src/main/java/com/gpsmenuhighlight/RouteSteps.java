/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.gpsmenuhighlight;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The transports of the route a pathfinder plugin last published, in travel order. GPS and
 * Shortest Path post them as a PluginMessage named "transports" whose data holds parallel
 * lists; only the "displayInfo" texts are kept.
 *
 * Synchronized because the pathfinder may post from its own thread while the client thread reads.
 */
final class RouteSteps
{
	private static final String KEY_DISPLAY_INFO = "displayInfo";

	private List<String> labels = Collections.emptyList();
	private long receivedAtMillis;

	/** Replaces the stored route with the one in a "transports" message. */
	synchronized void accept(Map<String, Object> data, long nowMillis)
	{
		labels = parse(data);
		receivedAtMillis = nowMillis;
	}

	synchronized void clear()
	{
		labels = Collections.emptyList();
		receivedAtMillis = 0;
	}

	/** The labels, or an empty list once they are older than {@code maxAgeMillis} (0 = never expire). */
	synchronized List<String> current(long nowMillis, long maxAgeMillis)
	{
		if (maxAgeMillis > 0 && nowMillis - receivedAtMillis > maxAgeMillis)
		{
			return Collections.emptyList();
		}
		return labels;
	}

	static List<String> parse(Map<String, Object> data)
	{
		if (data == null)
		{
			return Collections.emptyList();
		}
		Object value = data.get(KEY_DISPLAY_INFO);
		if (!(value instanceof List<?>))
		{
			return Collections.emptyList();
		}
		List<String> out = new ArrayList<>();
		for (Object entry : (List<?>) value)
		{
			if (entry instanceof String && !((String) entry).trim().isEmpty())
			{
				out.add((String) entry);
			}
		}
		return Collections.unmodifiableList(out);
	}
}
