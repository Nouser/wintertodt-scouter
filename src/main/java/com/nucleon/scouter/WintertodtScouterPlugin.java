/*
 * Copyright (c) 2021, nucleon
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.nucleon.scouter;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.api.widgets.Widget;
import net.runelite.client.task.Schedule;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Slf4j
@PluginDescriptor(
		name = "Wintertodt-Scouter",
		description = "Crowdsources the health of the Wintertodt Boss in themed worlds",
		tags= {"firemaking", "wintertodt", "status", "health"}
)

public class WintertodtScouterPlugin extends Plugin
{
	public ArrayList<WintertodtBossData> localBossDataArrayList = new ArrayList<WintertodtBossData>();
	public ArrayList<WintertodtBossData> globalBossDataArrayList = new ArrayList<WintertodtBossData>();

	private static final int WINTERTODT_REGION = 6462;
	private final int SECONDS_BETWEEN_UPLINK = 5;
	private final int SECONDS_BETWEEN_DOWNLINK = 5;
	private final int SECONDS_BETWEEN_PANEL_REFRESH = 5;
	private final int SECONDS_BETWEEN_POLL_HEALTH = 1;
	public static final int WINTERTODT_HEALTH_PACKED_ID = 25952277;

	static final String CONFIG_GROUP_KEY = "scouter";

	@Inject
	Client client;

	@Inject
	private WintertodtScouterConfig config;

	@Override
	protected void startUp() throws Exception
	{
		log.info("Wintertodt-Scouter started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("Wintertodt-Scouter stopped!");
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			//client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Wintertodt-Scouter says " + "version 1.0", null);
		}
	}

	private boolean isInWintertodtRegion()
	{
		if (client.getLocalPlayer() != null)
		{
			return client.getLocalPlayer().getWorldLocation().getRegionID() == WINTERTODT_REGION;
		}

		return false;
	}

	@Schedule(
			period = SECONDS_BETWEEN_POLL_HEALTH,
			unit = ChronoUnit.SECONDS,
			asynchronous = true
	)

	// Encapsulate this later

	public void captureBossHealth() {
		// The Wintertodt Energy Bar packed ID
		Widget wintertodtEnergyWidget = client.getWidget(WINTERTODT_HEALTH_PACKED_ID);

		// Check if the player is in the Wintertodt boss fight region, also check if the widget is loaded
		if (isInWintertodtRegion() && wintertodtEnergyWidget != null) {

			// Pull just the numbers from the Widget's text property ("Wintertodt Energy: 100%")
			Pattern regex = Pattern.compile("\\d+");
			Matcher bossEnergy = regex.matcher(wintertodtEnergyWidget.getText().toString());

			// Isolate the numbers
			if (bossEnergy.find()) {

				// add it to the arraylist for further network processing
				int energy = Integer.parseInt(bossEnergy.group(0));
				int world = client.getWorld();

				WintertodtBossData current = new WintertodtBossData(energy, world, Instant.now().getEpochSecond(), false);

				//check if the energy data is the same as the last upload; if so, skip this data.
				if (localBossDataArrayList.size() > 1) {

					WintertodtBossData previous = localBossDataArrayList.get(0);

					//if (previous.isUploaded()) {
						if (previous.getWorld() == current.getWorld()) {
							if (previous.getHealth() == current.getHealth()) {
								System.out.println("- Skipped Data, it's the same.");
								return;
							}
						}
					//}
				}
				localBossDataArrayList.add(current);
				localBossDataArrayList.sort(new WintertodtBossDataComparator());
				Collections.reverse(localBossDataArrayList);
			}
		}
		if (localBossDataArrayList.size() > 0)
			System.out.println(localBossDataArrayList.get(0).convertToDate().toString() + ":" + localBossDataArrayList.get(0).getHealth());
	}

	public static class WintertodtBossDataComparator implements Comparator<WintertodtBossData> {
		@Override
		public int compare(WintertodtBossData o1, WintertodtBossData o2) {
			return o1.convertToDate().compareTo(o2.convertToDate());
		}
	}

	@Provides
	WintertodtScouterConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(WintertodtScouterConfig.class);
	}
}
