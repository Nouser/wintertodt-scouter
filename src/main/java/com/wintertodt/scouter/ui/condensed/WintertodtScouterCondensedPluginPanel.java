/*
 * Copyright (c) 2021, Andrew McAdams, Cyborger1, Psikoi <https://github.com/Psikoi> (Basis)
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
package com.wintertodt.scouter.ui.condensed;

import com.wintertodt.scouter.WintertodtBossData;
import com.wintertodt.scouter.WintertodtScouterPlugin;
import com.wintertodt.scouter.ui.WintertodtScouterPluginPanelBase;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
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
import net.runelite.client.task.Schedule;
import net.runelite.client.ui.*;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.WorldUtil;
import net.runelite.http.api.worlds.World;


import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Function;

import static java.util.Comparator.*;

public class WintertodtScouterCondensedPluginPanel extends WintertodtScouterPluginPanelBase
{
	private static final Color ODD_ROW = new Color(44, 44, 44);

	private static final int WORLD_COLUMN_WIDTH = 55;
	private static final int TIME_COLUMN_WIDTH = 55;

	private final JPanel listContainer = new JPanel();

	private WintertodtScouterPanelHeader worldHeader;
	private WintertodtScouterPanelHeader healthHeader;
	private WintertodtScouterPanelHeader timerHeader;

	private WintertodtScouterOrder orderIndex = WintertodtScouterOrder.HEALTH;
	private boolean ascendingOrder = true;

	private final ArrayList<WintertodtScouterTableRow> rows = new ArrayList<>();

	public WintertodtScouterCondensedPluginPanel(WintertodtScouterPlugin plugin)
	{
		super(plugin);

		setBorder(null);
		setLayout(new DynamicGridLayout(0, 1));
		JPanel title = title();
		JPanel tip = tip();
		JPanel headerContainer = buildHeader();
		JPanel p =new JPanel();
		listContainer.setLayout(new GridLayout(0, 1));

		add(title);
		add(headerContainer);
		add(listContainer);
		add(tip);
	}

	/**
	 * Builds the entire table header.
	 */

	private JPanel title()
	{
		JLabel title = new JLabel("Wintertodt Scouter");
		title.setFont(FontManager.getRunescapeBoldFont());
		JPanel panel = new JPanel(new BorderLayout());
		panel.setLayout(new GridBagLayout());
		panel.add(title);

		return panel;
	}

	private JPanel tip()
	{
		JPanel layout = new JPanel(new BorderLayout());
		JLabel tip = new JLabel("You can double click to hop worlds!");

		tip.setFont(FontManager.getRunescapeSmallFont());
		layout.add(tip);
		layout.setBorder(BorderFactory.createTitledBorder(BorderFactory.createRaisedBevelBorder(), "Tip"));

		return layout;
	}

	private JPanel buildHeader()
	{
		JPanel header = new JPanel(new BorderLayout());
		JPanel status = new JPanel(new BorderLayout());

		healthHeader = new WintertodtScouterPanelHeader("Health", orderIndex == WintertodtScouterOrder.HEALTH, ascendingOrder);
		healthHeader.setPreferredSize(new Dimension(WORLD_COLUMN_WIDTH, 50));
		healthHeader.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent mouseEvent)
			{
				if (SwingUtilities.isRightMouseButton(mouseEvent))
				{
					return;
				}
				ascendingOrder = orderIndex != WintertodtScouterOrder.HEALTH || !ascendingOrder;
				orderBy(WintertodtScouterOrder.HEALTH);
			}
		});

		worldHeader = new WintertodtScouterPanelHeader("World", orderIndex == WintertodtScouterOrder.WORLD, ascendingOrder);
		worldHeader.setPreferredSize(new Dimension(TIME_COLUMN_WIDTH, 50));
		worldHeader.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent mouseEvent)
			{
				if (SwingUtilities.isRightMouseButton(mouseEvent))
				{
					return;
				}
				ascendingOrder = orderIndex != WintertodtScouterOrder.WORLD || !ascendingOrder;
				orderBy(WintertodtScouterOrder.WORLD);
			}
		});

		timerHeader = new WintertodtScouterPanelHeader("Reset", orderIndex == WintertodtScouterOrder.TIMER, ascendingOrder);
		timerHeader.setPreferredSize(new Dimension(TIME_COLUMN_WIDTH, 50));
		timerHeader.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent mouseEvent)
			{
				if (SwingUtilities.isRightMouseButton(mouseEvent))
				{
					return;
				}
				ascendingOrder = orderIndex != WintertodtScouterOrder.TIMER || !ascendingOrder;
				orderBy(WintertodtScouterOrder.TIMER);
			}
		});

		status.add(worldHeader, BorderLayout.WEST);
		status.add(healthHeader, BorderLayout.CENTER);
		status.add(timerHeader, BorderLayout.EAST);

		header.add(status, BorderLayout.CENTER);

		return header;
	}

	private void orderBy(WintertodtScouterOrder order)
	{
		worldHeader.highlight(false, ascendingOrder);
		healthHeader.highlight(false, ascendingOrder);
		timerHeader.highlight(false, ascendingOrder);

		switch (order)
		{
			case HEALTH:
				healthHeader.highlight(true, ascendingOrder);
				break;
			case WORLD:
				worldHeader.highlight(true, ascendingOrder);
				break;
			case TIMER:
				timerHeader.highlight(true, ascendingOrder);
				break;
		}

		orderIndex = order;
		updateList();
	}

	@Override
	public void updateList()
	{
		rows.sort((r1, r2) ->
		{
			switch (orderIndex)
			{
				case HEALTH:
					return getCompareValue(r1, r2, WintertodtScouterTableRow::getHealth);
				case WORLD:
					return getCompareValue(r1, r2, WintertodtScouterTableRow::getBossworld);
				case TIMER:
					return getCompareValue(r1, r2, WintertodtScouterTableRow::getTimer);
				default:
					return 0;
			}
		});

		listContainer.removeAll();

		int currentWorld = plugin.getCurrentWorld();

		int i = 0;

		for (WintertodtScouterTableRow row : rows)
		{
			row.updateStatus(row.getWorld().getId() == currentWorld);
			setColorOnRow(row, i++ % 2 == 0);
			listContainer.add(row);

		}

		listContainer.revalidate();
		listContainer.repaint();
	}

	@SuppressWarnings("rawtypes")
	private int getCompareValue(WintertodtScouterTableRow row1, WintertodtScouterTableRow row2, Function<WintertodtScouterTableRow, Comparable> compareByFn)
	{
		Comparator<WintertodtScouterTableRow> c = ascendingOrder ?
			comparing(compareByFn, naturalOrder()) : comparing(compareByFn, reverseOrder());
		// Always default to ordering by Max time for the second sort pass
		return c.thenComparing(WintertodtScouterTableRow::getHealth, naturalOrder()).compare(row1, row2);
	}

	@Override
	public void populate(ArrayList<WintertodtBossData> globalBossData)
	{
		rows.clear();

		for (int i = 0; i < globalBossData.size(); i++)
		{
			WintertodtBossData boss = globalBossData.get(i);
			rows.add(buildRow(globalBossData, i % 2 == 0, i));
		}

		updateList();
	}

	private WintertodtScouterTableRow buildRow(ArrayList<WintertodtBossData> bossData, boolean stripe, int index)
	{
		World world = plugin.getWorldService().getWorlds().findWorld(bossData.get(index).getWorld());
		boolean current = plugin.getCurrentWorld() == bossData.get(index).getWorld();
		WintertodtScouterTableRow row = new WintertodtScouterTableRow(world,
			bossData.get(index).getWorld(), current,
			bossData.get(index).getHealth(),
			bossData.get(index).getTimer(), plugin::hopTo);
		setColorOnRow(row, stripe);
		return row;
	}

	private void setColorOnRow(WintertodtScouterTableRow row, boolean stripe)
	{
		Color c = stripe ? ODD_ROW : ColorScheme.DARK_GRAY_COLOR;

		row.setBackground(c);
	}

}
