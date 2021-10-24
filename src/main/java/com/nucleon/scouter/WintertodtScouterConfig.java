package com.nucleon.scouter;

import lombok.Getter;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("wintertodtscouter")

public interface WintertodtScouterConfig extends Config
{
	String NETWORK_UPLINK = "get uplink";
	String NETWORK_DOWNLINK = "get downlink";

	@ConfigItem
			(
				keyName = NETWORK_UPLINK,
				position = 0, name = "Realtime Uplink",
				description = "Web endpoint to upload boss data to"
			)
	default String wintertodtGetDownlinkConfig()
	{
		return "/* firebase https w/ authorization header */";
	}

	@ConfigItem(keyName = NETWORK_DOWNLINK,
				position = 1,
				name = "Realtime Downlink",
				description = "Web endpoint to get boss data from"
	)
	default String wintertodtGetUplinkConfig()
	{

		return "/* firebase https w/ authorization header */";
	}
}
