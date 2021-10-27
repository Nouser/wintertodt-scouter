/*
 * Copyright (c) 2021, Cyborger1, Psikoi <https://github.com/Psikoi> (Basis)
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
package com.wintertodt.scouter.ui.condensed;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;
import net.runelite.client.ui.FontManager;
import net.runelite.http.api.worlds.World;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Consumer;

public class WintertodtScouterTableRow extends JPanel
{
	private static final int WORLD_COLUMN_WIDTH = 55;
	private static final int TIME_COLUMN_WIDTH = 55;

	private static final Color CURRENT_WORLD = new Color(66, 227, 17);

	private JLabel worldField;
	private JLabel healthField;
	private JLabel timerField;
	private JProgressBar healthBar;

	@Getter
	private final World world;

	@Getter
	private int bossworld;

	@Getter
	private int health;

	@Getter
	private int timer;

	private Color lastBackground;

	// bubble up events
	private final MouseAdapter labelMouseListener = new MouseAdapter()
	{
		@Override
		public void mouseClicked(MouseEvent mouseEvent)
		{
			dispatchEvent(mouseEvent);
		}

		@Override
		public void mousePressed(MouseEvent mouseEvent)
		{
			dispatchEvent(mouseEvent);
		}

		@Override
		public void mouseReleased(MouseEvent mouseEvent)
		{
			dispatchEvent(mouseEvent);
		}

		@Override
		public void mouseEntered(MouseEvent mouseEvent)
		{
			dispatchEvent(mouseEvent);
		}

		@Override
		public void mouseExited(MouseEvent mouseEvent)
		{
			dispatchEvent(mouseEvent);
		}
	};

	WintertodtScouterTableRow(World world, int bossworld, boolean current, int health, int timer, Consumer<World> onSelect)
	{
		this.world = world;
		this.bossworld = bossworld;
		this.health = health;
		this.timer = timer;

		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(2, 0, 2, 0));

		addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent mouseEvent)
			{
				if (mouseEvent.getClickCount() == 2)
				{
					if (onSelect != null)
					{
						onSelect.accept(world);
					}
				}
			}

			@Override
			public void mousePressed(MouseEvent mouseEvent)
			{
				if (mouseEvent.getClickCount() == 2)
				{
					setBackground(getBackground().brighter());
				}
			}

			@Override
			public void mouseReleased(MouseEvent mouseEvent)
			{
				if (mouseEvent.getClickCount() == 2)
				{
					setBackground(getBackground().darker());
				}
			}

			@Override
			public void mouseEntered(MouseEvent mouseEvent)
			{
				WintertodtScouterTableRow.this.lastBackground = getBackground();
				setBackground(getBackground().brighter());
			}

			@Override
			public void mouseExited(MouseEvent mouseEvent)
			{
				setBackground(lastBackground);
			}
		});

		JPanel status = new JPanel(new BorderLayout());
		JPanel rightSide = new JPanel(new BorderLayout());
		status.setOpaque(false);
		rightSide.setOpaque(false);

		JPanel worldField = buildWorldField();
		worldField.setPreferredSize(new Dimension(WORLD_COLUMN_WIDTH, 20));
		worldField.setOpaque(false);

		JPanel healthField = buildHealthField();
		healthField.setPreferredSize(new Dimension(TIME_COLUMN_WIDTH, 20));
		healthField.setOpaque(false);

		JPanel timerField = buildTimerField();
		timerField.setPreferredSize(new Dimension(TIME_COLUMN_WIDTH, 20));
		timerField.setOpaque(false);

		updateStatus(current);

		status.add(worldField, BorderLayout.WEST);
		status.add(healthField, BorderLayout.CENTER);
		status.add(timerField, BorderLayout.EAST);

		add(status, BorderLayout.CENTER);

	}

	void updateStatus(boolean current)
	{
		healthBar.setStringPainted(true);
		healthBar.setValue(getHealth());
		healthBar.setString(getHealth() + "%");
		healthBar.setForeground(Color.green);
		timerField.setText(""+ getTimer());

		if (getHealth() <= 60)
		{
			healthBar.setForeground(Color.orange);
		}

		if (getHealth() <= 50)
		{
			healthBar.setForeground(Color.red);
		}

		recolour(current);
	}

	void updateInfo(int health, int world, int timer, boolean current)
	{
		this.timer = timer;
		this.health = health;
		this.bossworld = world;
		updateStatus(current);
	}

	private static StringBool timeString(Instant time)
	{
		long s = Duration.between(Instant.now(), time).getSeconds();
		boolean negative = false;
		if (s < 0)
		{
			s *= -1;
			negative = true;
		}

		String str;
		long mins = s / 60;
		long secs = s % 60;

		if (negative)
		{
			if (mins > 9)
			{
				str = String.format("-%dm", mins);
			}
			else
			{
				str = String.format("-%1d:%02d", mins, secs);
			}
		}
		else
		{
			if (mins > 99)
			{
				str = String.format("%dm", mins);
			}
			else
			{
				str = String.format("%1d:%02d", mins, secs);
			}
		}

		return new StringBool(str, negative);
	}

	public void recolour(boolean current)
	{
		if (current)
		{
			worldField.setForeground(CURRENT_WORLD);
			return;
		}
	}

	private JPanel buildWorldField()
	{
		JPanel column = new JPanel(new BorderLayout());
		column.setBorder(new EmptyBorder(0, 5, 0, 5));

		worldField = new JLabel(world.getId() + "");
		column.add(worldField, BorderLayout.CENTER);

		return column;
	}

	private JPanel buildHealthField()
	{
		JPanel column = new JPanel(new BorderLayout());
		column.setBorder(new EmptyBorder(0, 5, 0, 5));

		healthBar = new JProgressBar();
		healthBar.setFont(FontManager.getRunescapeSmallFont());

		column.add(healthBar, BorderLayout.CENTER);

		return column;
	}


	private JPanel buildTimerField()
	{
		JPanel column = new JPanel(new BorderLayout());
		column.setBorder(new EmptyBorder(0, 5, 0, 0));

		timerField = new JLabel();
		timerField.setFont(FontManager.getRunescapeSmallFont());
		timerField.setHorizontalAlignment(SwingConstants.CENTER);
		column.add(timerField, BorderLayout.CENTER);

		return column;
	}


	@Value
	@AllArgsConstructor
	private static class StringBool
	{
		String string;
		boolean boolValue;
	}
}
