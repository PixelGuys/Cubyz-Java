package cubyz.gui.game;

import cubyz.client.Cubyz;
import cubyz.gui.MenuGUI;
import cubyz.gui.components.Component;
import cubyz.gui.components.InventorySlot;
import cubyz.gui.game.inventory.GeneralInventory;
import cubyz.rendering.Graphics;
import cubyz.rendering.Texture;
import cubyz.rendering.Window;
import cubyz.rendering.text.Fonts;
import cubyz.rendering.text.TextLine;
import cubyz.world.items.Inventory;

import java.util.ArrayList;

import static cubyz.client.ClientSettings.GUI_SCALE;

/**
 * Basic overlay while in-game.<br>
 * Contains hotbar, hunger bars, and crosshair.
 */

public class GameOverlay extends MenuGUI {

	private static final Texture crosshair;
	private static final Texture selection;
	private static final Texture[] healthBar;
	private static final Texture[] hungerBar;
	
	long lastPlayerHurtMs; // stored here and not in Player for easier multiplayer integration
	float lastPlayerHealth;

	private final InventorySlot[] inv = new InventorySlot[8];

	private final ArrayList<String> chatMessages = new ArrayList<>();
	private final ArrayList<Long> chatTimes = new ArrayList<>();
	private static final int CHAT_TIMEOUT = 20000;
	private static final int CHAT_FADEOUT = 1000;

	static {
		crosshair = Texture.loadFromFile("assets/cubyz/textures/crosshair.png");
		selection = Texture.loadFromFile("assets/cubyz/guis/inventory/selected_slot.png");
		healthBar = new Texture[8];
		healthBar[0] = Texture.loadFromFile("assets/cubyz/textures/health_bar_beg_empty.png");
		healthBar[1] = Texture.loadFromFile("assets/cubyz/textures/health_bar_beg_full.png");
		healthBar[2] = Texture.loadFromFile("assets/cubyz/textures/health_bar_end_empty.png");
		healthBar[3] = Texture.loadFromFile("assets/cubyz/textures/health_bar_end_full.png");
		healthBar[4] = Texture.loadFromFile("assets/cubyz/textures/health_bar_mid_empty.png");
		healthBar[5] = Texture.loadFromFile("assets/cubyz/textures/health_bar_mid_half.png");
		healthBar[6] = Texture.loadFromFile("assets/cubyz/textures/health_bar_mid_full.png");
		healthBar[7] = Texture.loadFromFile("assets/cubyz/textures/health_bar_icon.png");
		hungerBar = new Texture[8];
		hungerBar[0] = Texture.loadFromFile("assets/cubyz/textures/hunger_bar_beg_empty.png");
		hungerBar[1] = Texture.loadFromFile("assets/cubyz/textures/hunger_bar_beg_full.png");
		hungerBar[2] = Texture.loadFromFile("assets/cubyz/textures/hunger_bar_end_empty.png");
		hungerBar[3] = Texture.loadFromFile("assets/cubyz/textures/hunger_bar_end_full.png");
		hungerBar[4] = Texture.loadFromFile("assets/cubyz/textures/hunger_bar_mid_empty.png");
		hungerBar[5] = Texture.loadFromFile("assets/cubyz/textures/hunger_bar_mid_half.png");
		hungerBar[6] = Texture.loadFromFile("assets/cubyz/textures/hunger_bar_mid_full.png");
		hungerBar[7] = Texture.loadFromFile("assets/cubyz/textures/hunger_bar_icon.png");
	}
	
	@Override
	public void init() {
		updateGUIScale();
	}

	public void addChatMessage(String msg) {
		synchronized(chatMessages) {
			chatMessages.add(msg);
			chatTimes.add(System.currentTimeMillis());
		}
	}

	@Override
	public void updateGUIScale() {
		Inventory inventory = Cubyz.player.getInventory_AND_DONT_FORGET_TO_SEND_CHANGES_TO_THE_SERVER();
		for(int i = 0; i < 8; i++) {
			inv[i] = new InventorySlot(inventory.getStack(i), (i - 4) * 20 * GUI_SCALE, 20 * GUI_SCALE, Component.ALIGN_BOTTOM);
		}
	}

	@Override
	public void render() {
		Graphics.setColor(0xFFFFFF);
		if (Cubyz.gameUI.getMenuGUI() == null || !Cubyz.gameUI.getMenuGUI().ungrabsMouse()) {
			Graphics.drawImage(crosshair, Window.getWidth()/2 - 8 * GUI_SCALE, Window.getHeight()/2 - 8 * GUI_SCALE, 16 * GUI_SCALE, 16 * GUI_SCALE);
		}
		if (!(Cubyz.gameUI.getMenuGUI() instanceof GeneralInventory)) {
			Graphics.drawImage(
				selection,
				Window.getWidth()/2 - 79 * GUI_SCALE + Cubyz.inventorySelection*20 * GUI_SCALE,
				Window.getHeight() - 19 * GUI_SCALE, 18 * GUI_SCALE, 18 * GUI_SCALE
			);
			for(int i = 0; i < 8; i++) {
				inv[i].reference = Cubyz.player.getInventory_AND_DONT_FORGET_TO_SEND_CHANGES_TO_THE_SERVER().getStack(i); // without it, if moved in inventory, stack won't refresh
				inv[i].render();
			}
		}
		// Draw the health bar:
		float maxHealth = Cubyz.player.maxHealth;
		float health = Cubyz.player.health;
		if (lastPlayerHealth != health) {
			if (lastPlayerHealth > health) {
				lastPlayerHurtMs = System.currentTimeMillis();
			}
			lastPlayerHealth = health;
		}
		if (System.currentTimeMillis() < lastPlayerHurtMs+510) {
			Graphics.setColor(0xFF3232, (int) (255-(System.currentTimeMillis()-lastPlayerHurtMs))/2);
			Graphics.fillRect(0, 0, Window.getWidth(), Window.getHeight());
		}
		Graphics.setColor(0xFFFFFF);
		TextLine text = new TextLine(Fonts.PIXEL_FONT, Math.round(health*10)/10.0f + "/" + Math.round(maxHealth) + " HP", 8 * GUI_SCALE, false);
		float width = text.getWidth();
		Graphics.drawImage(healthBar[7], (int)(Window.getWidth() - (maxHealth*6 + 12) * GUI_SCALE), 4 * GUI_SCALE, 12 * GUI_SCALE, 12 * GUI_SCALE);
		for(int i = 0; i < maxHealth; i += 2) {
			boolean half = i + 1 == health;
			boolean empty = i >= health;
			
			int idx;
			if (i == 0) { // beggining
				idx = empty ? 0 : 1;
			} else if (i == maxHealth-2) { // end
				idx = i + 1 >= health ? 2 : 3;
			} else {
				idx = empty ? 4 : (half ? 5 : 6); // if empty => 4, half => 5, full => 6
			}
			Graphics.drawImage(healthBar[idx], (int)(i*6 * GUI_SCALE + Window.getWidth() - (maxHealth*6 + 4) * GUI_SCALE), 4 * GUI_SCALE, 12 * GUI_SCALE, 12 * GUI_SCALE);
		}
		text.render((maxHealth / 2) * 6 * GUI_SCALE + Window.getWidth() - (maxHealth*6 + 4) * GUI_SCALE - width / 2, 7 * GUI_SCALE);
		// Draw the hunger bar:
		float maxHunger = Cubyz.player.maxHunger;
		float hunger = Cubyz.player.hunger;
		text = new TextLine(Fonts.PIXEL_FONT, Math.round(hunger*10)/10.0f + "/" + Math.round(maxHunger) + " HP", 8 * GUI_SCALE, false);
		width = text.getWidth();
		Graphics.drawImage(hungerBar[7], (int)(Window.getWidth() - (maxHunger*6 + 12) * GUI_SCALE), 20 * GUI_SCALE, 12 * GUI_SCALE, 12 * GUI_SCALE);
		for(int i = 0; i < maxHunger; i += 2) {
			boolean half = i + 1 == hunger;
			boolean empty = i >= hunger;
			
			int idx;
			if (i == 0) { // beggining
				idx = empty ? 0 : 1;
			} else if (i == maxHunger-2) { // end
				idx = i + 1 >= hunger ? 2 : 3;
			} else {
				idx = empty ? 4 : (half ? 5 : 6); // if empty => 4, half => 5, full => 6
			}
			Graphics.drawImage(hungerBar[idx], (int)(i*6 * GUI_SCALE + Window.getWidth() - (maxHunger*6 + 4) * GUI_SCALE), 20 * GUI_SCALE, 12 * GUI_SCALE, 12 * GUI_SCALE);
		}
		text.render((maxHunger / 2) * 6 * GUI_SCALE + Window.getWidth() - (maxHealth*6 + 4) * GUI_SCALE - width / 2, 23 * GUI_SCALE);

		// Draw the chat:
		if(!(Cubyz.gameUI.getMenuGUI() instanceof ChatGUI)) {
			synchronized(chatMessages) {
				ArrayList<TextLine> textLines = new ArrayList<>();
				ArrayList<Float> alphas = new ArrayList<>();
				Graphics.setColor(255, 255, 255);
				float maxWidth = 0;
				for(int i = chatMessages.size() - 1; i >= 0; i--) {
					String msg = chatMessages.get(i);
					long time = System.currentTimeMillis() - chatTimes.get(i);
					float alpha = 1;
					if(time > CHAT_TIMEOUT + CHAT_FADEOUT) {
						chatMessages.remove(i);
						chatTimes.remove(i);
						continue;
					} else if(time > CHAT_TIMEOUT) {
						alpha = 1.0f - (time - CHAT_TIMEOUT)/(float)CHAT_FADEOUT;
					}

					TextLine line = new TextLine(Fonts.PIXEL_FONT, msg, 16*GUI_SCALE, false);
					maxWidth = Math.max(maxWidth, line.getTextWidth());
					textLines.add(line);
					alphas.add(alpha);
				}
				float oldAlpha = Graphics.getGlobalAlphaMultiplier();
				int y = Window.getHeight() - 20*GUI_SCALE;
				for(int i = 0; i < textLines.size(); i++) {
					float alpha = alphas.get(i);
					Graphics.setGlobalAlphaMultiplier(oldAlpha*alpha*0.5f);
					Graphics.setColor(0);
					Graphics.fillRect(0, y, maxWidth + 10, 20*GUI_SCALE);
					Graphics.setGlobalAlphaMultiplier(oldAlpha*alpha);
					textLines.get(i).render(GUI_SCALE, y);
					y -= 20*GUI_SCALE;
				}
				Graphics.setGlobalAlphaMultiplier(oldAlpha);
			}
		}
	}

	@Override
	public boolean doesPauseGame() {
		return false;
	}

}
