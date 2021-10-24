package com.nucleon.scouter;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("wintertodtscouter")

public interface WintertodtScouterConfig extends Config
{
	String SCOUTER_UP_REALTIME_LINK = "get uplink";
	String SCOUTER_DOWN_REALTIME_LINK= "get downlink";
	@ConfigItem
			(
				keyName = SCOUTER_UP_REALTIME_LINK,
				position = 0, name = "Realtime Uplink",
				description = "Web endpoint to upload boss data to"
			)
	default String wintertodtGetDownlinkConfig()
	{
		return "/* firebase https w/ authorization header */";
	}

	@ConfigItem(keyName = SCOUTER_DOWN_REALTIME_LINK,
				position = 1,
				name = "Realtime Downlink",
				description = "Web endpoint to get boss data from"
	)
	default String wintertodtGetUplinkConfig()
	{
		return "/* firebase https w/ authorization header */";
	}
}
