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

package com.wintertodt.scouter;

import com.google.inject.Provides;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.swing.*;

import com.wintertodt.scouter.ui.WintertodtScouterPluginPanelBase;
import com.wintertodt.scouter.ui.condensed.WintertodtScouterCondensedPluginPanel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.WorldService;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.api.widgets.Widget;
import net.runelite.client.task.Schedule;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.WorldUtil;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldResult;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Slf4j
@PluginDescriptor(
		name = "Wintertodt Scouter",
		description = "Crowdsources the health of the Wintertodt Boss in themed worlds",
		tags= {"firemaking", "wintertodt", "status", "health"}
)

public class WintertodtScouterPlugin extends Plugin
{
	public ArrayList<WintertodtBossData> localBossDataArrayList = new ArrayList<>();

	@Getter
	String apiVersion = "2";

	@Getter
	@Setter
	public ArrayList<WintertodtBossData> globalBossDataArrayList = new ArrayList<>();

	private static final int WINTERTODT_REGION = 6462;
	private final int SECONDS_BETWEEN_UPLINK = 5;
	private final int SECONDS_BETWEEN_DOWNLINK = 5;
	private final int SECONDS_BETWEEN_PANEL_REFRESH = 5;
	private boolean canRefresh;
	private final int SECONDS_BETWEEN_POLL_HEALTH = 1;
	public static final int WINTERTODT_HEALTH_PACKED_ID = 25952277;
	public static final int WINTERTODT_GAME_TIMER_ID = 25952259;

	static final String CONFIG_GROUP_KEY = "scouter";

	@Inject
	private ChatMessageManager chatMessageManager;

	@Getter
	String wintertodtGetUplink;

	@Getter
	String wintertodtGetDownlink;

	@Getter
	@Setter
	private boolean postError = false;

	@Getter
	@Setter
	private boolean getError = false;

	@Inject
	Client client;

	@Inject
	private WintertodtScouterNetwork manager;

	@Inject
	@Getter
	private WintertodtScouterConfig config;

	@Inject
	private WintertodtScouterOverlayPanel overlayPanel;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ClientThread clientThread;

	@Inject
	@Getter
	private WorldService worldService;

	private WintertodtScouterPluginPanelBase wintertodtScouterPanel;
	private NavigationButton navButton = null;
	private final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/wintertodt-scouter-icon.png");

	@Override
	protected void startUp() throws Exception
	{
		log.info("Wintertodt-Scouter started!");
		canRefresh = true;
		// Set up config variables
		wintertodtGetUplink = config.wintertodtGetUplinkConfig();
		wintertodtGetDownlink = config.wintertodtGetDownlinkConfig();

		// Add the overlay to the OverlayManager
		overlayManager.add(overlayPanel);

		// Set up the sidebar panel
		loadPluginPanel();
	}

	@Override
	protected void shutDown() throws Exception
	{
		// Remove the overlay from the OverlayManager
		overlayManager.remove(overlayPanel);

		// Remove sidebar panel button
		clientToolbar.removeNavigation(navButton);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!event.getGroup().equals(CONFIG_GROUP_KEY))
			return;
		switch (event.getKey())
		{
			case WintertodtScouterConfig.NETWORK_UPLINK:
				wintertodtGetUplink = config.wintertodtGetUplinkConfig();
				break;
			case WintertodtScouterConfig.NETWORK_DOWNLINK:
				wintertodtGetDownlink = config.wintertodtGetDownlinkConfig();
				manager.makeGetRequest();
				break;
			default:
				updatePanelList();
				break;
		}
	}

	@Subscribe
	public void onGameTick(GameTick gameTick)
	{
		handleHop();
		captureBossHealth();
	}

	private boolean isInWintertodtRegion()
	{
		if (client.getLocalPlayer() != null)
		{
			return client.getLocalPlayer().getWorldLocation().getRegionID() == WINTERTODT_REGION;
		}

		return false;
	}

	public void updatePanelList()
	{
		log.debug("Update panel list");
		SwingUtilities.invokeLater(() -> wintertodtScouterPanel.populate(globalBossDataArrayList.stream().filter(this::isAllowedWorld).collect(Collectors.toList())));
	}

	public void captureBossHealth() {
		// after hopping the widget contains the previous worlds data
		if (client.getGameState() != GameState.HOPPING) {
			// The Wintertodt Energy Bar packed ID
			Widget wintertodtEnergyWidget = client.getWidget(WINTERTODT_HEALTH_PACKED_ID);

			// Check if the player is in the Wintertodt boss fight region, also check if the widget is loaded
			if (isInWintertodtRegion() && wintertodtEnergyWidget != null) {

				// Pull just the numbers from the Widget's text property ("Wintertodt Energy: 100%")
				Pattern regex = Pattern.compile("\\d+");
				Matcher bossEnergy = regex.matcher(wintertodtEnergyWidget.getText().toString());

				// Isolate the numbers
				if (bossEnergy.find()) {

					// get ready to add it to the arraylist for further network processing
					int energy = Integer.parseInt(bossEnergy.group(0));
					int world = client.getWorld();
					long unixTime = Instant.now().getEpochSecond();

					if (energy == 0) {
						captureResetTimer();
						return;
					}

					WintertodtBossData current = new WintertodtBossData(energy, world, unixTime, false, -1);

					//check if the energy data is the same as the last upload; if so, skip this data.
					if (localBossDataArrayList.size() > 1) {
						WintertodtBossData previous = localBossDataArrayList.get(localBossDataArrayList.size() - 1);
						if (previous.getWorld() == current.getWorld()) {
							if (previous.getHealth() == current.getHealth()) {
								return;
							}
						}
					}
					localBossDataArrayList.add(current);
				}
				if (localBossDataArrayList.size() > 0)
					log.debug(    localBossDataArrayList.get(localBossDataArrayList.size() - 1).getTime() +
							": Health: " + localBossDataArrayList.get(localBossDataArrayList.size() - 1).getHealth());
				updatePanelList();
			}
		}
	}

	public void captureResetTimer() {
		if (client.getGameState() != GameState.HOPPING) {
			Widget wintertodtResetWidget = client.getWidget(WINTERTODT_GAME_TIMER_ID);

			// Check if the player is in the Wintertodt boss fight region, also check if the widget is loaded
			if (isInWintertodtRegion() && wintertodtResetWidget != null) {

				// Pull just the numbers from the Widget's text property ("Wintertodt Energy: 100%")
				Pattern regex = Pattern.compile("\\d:\\d+");
				Matcher bossTimer = regex.matcher(wintertodtResetWidget.getText());

				// Isolate the numbers
				if (bossTimer.find()) {

					// get ready to add it to the arraylist for further network processing
					String time = bossTimer.group(0);
					String minute = time.split(":")[0];
					String second = time.split(":")[1];
					int seconds;
					if (minute.equals("1")) {
						seconds = 60 + Integer.parseInt(second);
					} else {
						seconds = Integer.parseInt(second);
					}
					int timer = seconds;
					int world = client.getWorld();
					long unixTime = Instant.now().getEpochSecond();

					WintertodtBossData current = new WintertodtBossData(-1, world, unixTime, false, timer);

					//check if the energy data is the same as the last upload; if so, skip this data.
					if (localBossDataArrayList.size() > 1) {

						WintertodtBossData previous = localBossDataArrayList.get(localBossDataArrayList.size() - 1);


						if (previous.getWorld() == current.getWorld()) {
							if (previous.getTimer() == current.getTimer()) {
								log.debug("- Skipped Data, it's the same.");
								return;
							}
						}
					}

					localBossDataArrayList.add(current);
				}
				if (localBossDataArrayList.size() > 0)
					log.debug(localBossDataArrayList.get(localBossDataArrayList.size() - 1).getTime() + ": Timer: " + localBossDataArrayList.get(localBossDataArrayList.size() - 1).getTimer());
				updatePanelList();
			}
		}
	}

	// Takes the unix timestamp for each entry in the list, converts them to local date and orders them by oldest
	public static class WintertodtBossDataComparator implements Comparator<WintertodtBossData> {
		@Override
		public int compare(WintertodtBossData o1, WintertodtBossData o2) {
			return o1.convertToDate().compareTo(o2.convertToDate());
		}
	}

	private void loadPluginPanel()
	{
		if (navButton != null)
		{
			clientToolbar.removeNavigation(navButton);
		}

		Class<? extends WintertodtScouterPluginPanelBase> panelClass;

		panelClass = WintertodtScouterCondensedPluginPanel.class;

		try
		{
			wintertodtScouterPanel = panelClass.getDeclaredConstructor(this.getClass()).newInstance(this);
		}
		catch (Exception e)
		{
			log.error("Error loading panel class", e);
			return;
		}

		navButton = NavigationButton.builder().tooltip("Wintertodt Scouter").icon(icon).priority(7).panel(wintertodtScouterPanel).build();
		clientToolbar.addNavigation(navButton);
	}

	@Provides
	WintertodtScouterConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(WintertodtScouterConfig.class);
	}
	private boolean isAllowedWorld(WintertodtBossData bossData) {
		// Protect against non-existent world ids
		WorldResult worldResult = worldService.getWorlds();
		if (worldResult == null) {
			return false;
		}
		World world = worldResult.findWorld(bossData.getWorld());
		if (world == null) {
			return false;
		}


		// Disallow various landing sites (depending on config)
		return true;
	}


	public int getCurrentWorld()
	{
		return client.getWorld();
	}

	private static final int DISPLAY_SWITCHER_MAX_ATTEMPTS = 3;
	private net.runelite.api.World quickHopTargetWorld;
	private int displaySwitcherAttempts = 0;

	public void hopTo(World world)
	{
		hopTo(world.getId());
	}

	public void hopTo(int worldId)
	{
		clientThread.invoke(() -> hop(worldId));
	}

	private void hop(int worldId)
	{
		assert client.isClientThread();

		if (!config.isWorldHopperEnabled())
		{
			return;
		}

		WorldResult worldResult = worldService.getWorlds();
		if (worldResult == null)
		{
			return;
		}
		// Don't try to hop if the world doesn't exist
		World world = worldResult.findWorld(worldId);
		if (world == null)
		{
			return;
		}

		final net.runelite.api.World rsWorld = client.createWorld();
		rsWorld.setActivity(world.getActivity());
		rsWorld.setAddress(world.getAddress());
		rsWorld.setId(world.getId());
		rsWorld.setPlayerCount(world.getPlayers());
		rsWorld.setLocation(world.getLocation());
		rsWorld.setTypes(WorldUtil.toWorldTypes(world.getTypes()));

		String chatMessage = new ChatMessageBuilder()
				.append(ChatColorType.NORMAL)
				.append("Quick-hopping to World ")
				.append(ChatColorType.HIGHLIGHT)
				.append(Integer.toString(world.getId()))
				.append(ChatColorType.NORMAL)
				.append("..")
				.build();

		chatMessageManager
				.queue(QueuedMessage.builder()
						.type(ChatMessageType.CONSOLE)
						.runeLiteFormattedMessage(chatMessage)
						.build());

		if (client.getGameState() == GameState.LOGIN_SCREEN)
		{
			// on the login screen we can just change the world by ourselves
			client.changeWorld(rsWorld);
			return;
		}

		quickHopTargetWorld = rsWorld;
	}

	private void handleHop()
	{
		if (quickHopTargetWorld == null)
		{
			return;
		}

		if (client.getWidget(WidgetInfo.WORLD_SWITCHER_LIST) == null)
		{
			client.openWorldHopper();
			localBossDataArrayList.clear();
			if (++displaySwitcherAttempts >= DISPLAY_SWITCHER_MAX_ATTEMPTS)
			{
				String chatMessage = new ChatMessageBuilder()
						.append(ChatColorType.NORMAL)
						.append("Failed to quick-hop after ")
						.append(ChatColorType.HIGHLIGHT)
						.append(Integer.toString(displaySwitcherAttempts))
						.append(ChatColorType.NORMAL)
						.append(" attempts.")
						.build();

				chatMessageManager
						.queue(QueuedMessage.builder()
								.type(ChatMessageType.CONSOLE)
								.runeLiteFormattedMessage(chatMessage)
								.build());

				resetQuickHopper();
			}
		}
		else
		{
			client.hopToWorld(quickHopTargetWorld);
			resetQuickHopper();
		}
	}

	private void resetQuickHopper()
	{
		quickHopTargetWorld = null;
		displaySwitcherAttempts = 0;
		hitAPI();
	}
	public void hitAPI()
	{
		if (canRefresh)
		{

			if ((client.getGameState() == GameState.LOGGED_IN || client.getGameState() == GameState.HOPPING) && wintertodtScouterPanel.isOpen())
			{
				canRefresh = true;
				manager.makeGetRequest();
			}
		}
	}
	@Schedule(
			period = SECONDS_BETWEEN_UPLINK,
			unit = ChronoUnit.SECONDS,
			asynchronous = true
	)
	public void submitToAPI()
	{
		if ((client.getGameState() == GameState.LOGGED_IN || client.getGameState() != GameState.HOPPING) && isInWintertodtRegion()) {
			if (localBossDataArrayList.size() > 0) {
				WintertodtBossData last = localBossDataArrayList.get(localBossDataArrayList.size() - 1);
				last.setTime(Instant.now().getEpochSecond());
				if (!last.isUploaded()) {
					manager.submitToAPI(processLocalData(localBossDataArrayList));
				}
			}
		}

	}

	WintertodtBossData processLocalData(ArrayList<WintertodtBossData> localBossDataArrayList) {

		WintertodtBossData last = localBossDataArrayList.get(localBossDataArrayList.size() - 1);
		for (WintertodtBossData data : localBossDataArrayList) {
			if (!data.isUploaded()) {
				data.setUploaded(true);
			}
		}
		return last;
	}

	@Schedule(
			period = SECONDS_BETWEEN_DOWNLINK,
			unit = ChronoUnit.SECONDS,
			asynchronous = true
	)
	public void attemptGetRequest()
	{
		log.debug("Attempt get request");
		hitAPI();
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		if (event.getMessage().equals("Please finish what you're doing before using the World Switcher."))
		{
			resetQuickHopper();
		}
	}
}
