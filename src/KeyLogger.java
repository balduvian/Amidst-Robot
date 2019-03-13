import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.Buffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.NativeInputEvent;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;

import javax.imageio.ImageIO;

/**
 * Logs keystrokes in a log file.
 * @author Samir Joglekar
 */
public class KeyLogger implements NativeKeyListener {

	enum Biome {
		//OCEAN(0x000070, 0x9090a0, 0x000030, 0x282898),
		//PLAINS(0x8db360),
		//DESERT(0xfa9418, 0xd25f12, 0xffbc40),
		//EXTREME_HILLS(0x606060, 0x72789a, 0x507050, 0x888888),
		//FOREST(0x056621, 0x22551c),
		//TAIGA(0x0b6659, 0x163933, 0x31554a, 0x243f36),
		SWAMP(0x07f9b2),
		//RIVER(0x0000ff, 0xa0a0ff),
		//ICE_PLAINS(0xffffff),
		//ICE_MOUNTAINS(0xa0a0a0),
		MUSHROOM_ISLAND(0xff00ff),//0xa000ff
		//BEACH(0xfade55, 0xfaf0c0),
		JUNGLE(0x537b09),//0x2c4205, 0x628b17
		//STONE_BEACH(0xa2a284),
		//BIRCH_FOREST(0x307444, 0x1f5f32),
		ROOFED_FOREST(0x40511a),
		MEGA_TAIGA(0x596651),//, 0x454f3e
		//SAVANNA(0xbdb25f, 0xa79d64),
		MESA(0xd94515),//0xb09765, 0xb09765
		//SUNFLOWER_PLAINS(0xb5db88),
		FLOWER_FOREST(0x2d8e49),
		ICE_PLAINS_SPIKES(0xb4dcdc);

		private int color;

		private Biome(int c) {
			color = c;
		}

		public int getColor() {
			return color;
		}
	}

	private static final Biome[] BIOMES = {
		Biome.SWAMP,
		Biome.FLOWER_FOREST,
		Biome.ICE_PLAINS_SPIKES,
		Biome.MUSHROOM_ISLAND,
		Biome.MESA,
		Biome.MEGA_TAIGA,
		Biome.ROOFED_FOREST,
		Biome.JUNGLE,
	};

	public static final int DEFAULT_POPUP_WAIT = 200;
	public static final int DEFAULT_SCREENSHOT_WAIT = 550;

	int x0;
	int y0;
	int x1;
	int y1;

	int checked;

	int popupWait;
	int screenShotWait;

	Robot robot;

	Continuous continuous;

	String bestSeed;
	int bestCount;
	boolean[] bestBiomes;

	public KeyLogger() throws IOException {
		checkForPreferences();
		try {
			robot = new Robot();
		} catch (AWTException ex) {}
	}

	public void nativeKeyPressed(NativeKeyEvent nativeKeyEvent) {}

	public void nativeKeyReleased(NativeKeyEvent nativeKeyEvent) {}

	public void nativeKeyTyped(NativeKeyEvent nativeKeyEvent) {
		Point point = null;
		switch(nativeKeyEvent.getKeyChar()) {
			case('a'):
				point = MouseInfo.getPointerInfo().getLocation();

				x0 = point.x;
				y0 = point.y;

				switchBounds();
				printBounds();
				break;
			case('d'):
				point = MouseInfo.getPointerInfo().getLocation();

				x1 = point.x;
				y1 = point.y;

				switchBounds();
				printBounds();
				break;
			case('g'):
				continuous.cycle();
				break;
			case('s'):
				if(continuous == null || !continuous.running) {
					continuous = new Continuous();
					continuous.start();
				}
				break;
			case('p'):
				continuous.setRunning(false);
				try {
					continuous.join();
				}catch (Exception ex) {}
				System.out.println("BEST seed out of " + checked + ": " + bestSeed + " total: " + bestCount + " found:" + biomeList(bestBiomes));
		}
	}

	private void checkForPreferences() {
		File f = new File(".settings");
		if(f.exists()) {
			try {
				FileInputStream inp = new FileInputStream(f);
				//TODO reading
			} catch(Exception ex) {}
			x0 = -1;
			x1 = -1;
			y0 = -1;
			y1 = -1;
			popupWait = DEFAULT_POPUP_WAIT;
			screenShotWait = DEFAULT_SCREENSHOT_WAIT;
			bestSeed = "none";
			bestCount = 0;
			bestBiomes = new boolean[BIOMES.length];
			checked = 0;
		} else {
			try {
				FileOutputStream out = new FileOutputStream(f);
				out.write("x0: -1\n".getBytes());
				out.write("x1: -1\n".getBytes());
				out.write("y0: -1\n".getBytes());
				out.write("y1: -1\n".getBytes());
				out.write("best seed: none\n".getBytes());
				out.write("best biome count: 0\n".getBytes());
				out.write("biome list: none\n".getBytes());
				out.write(("popup wait: " + DEFAULT_POPUP_WAIT + "\n").getBytes());
				out.write(("screenshot wait: " + DEFAULT_SCREENSHOT_WAIT + "\n").getBytes());
				out.write("checked: 0".getBytes());
				out.close();
			} catch (Exception ex) {}
			x0 = -1;
			x1 = -1;
			y0 = -1;
			y1 = -1;
			popupWait = DEFAULT_POPUP_WAIT;
			screenShotWait = DEFAULT_SCREENSHOT_WAIT;
			bestSeed = "none";
			bestCount = 0;
			bestBiomes = new boolean[BIOMES.length];
			checked = 0;
		}
	}

	private String genSeedNumber() {
		StringBuilder builder = new StringBuilder();
		for(int i = 0; i < 18; ++i) {
			builder.append((char)(Math.random() * 10 + 48)) ;
		}
		return builder.toString();
	}

	private void slep(long time) {
		try {
			Thread.sleep(time);
		}catch (Exception ex) {}
	}

	private void newSeed(String seed) {
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_N);
		slep(popupWait);
		robot.keyRelease(KeyEvent.VK_N);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		int len = seed.length();
		for(int i = 0; i < len; ++i) {
			int code = (int)seed.charAt(i);
			robot.keyPress(code);
			robot.keyRelease(code);
		}
		robot.keyPress(KeyEvent.VK_ENTER);
		robot.keyRelease(KeyEvent.VK_ENTER);
	}

	private BufferedImage screenshot() {
		return robot.createScreenCapture(new Rectangle(x0, y0, x1 - x0, y1 - y0));
	}

	private class ScanContainer {
		public int count;
		public boolean[] found;

		public ScanContainer(int c, boolean[] f) {
			count = c;
			found = f;
		}
	}

	private ScanContainer scanScreenshot(BufferedImage b) {
		int width = b.getWidth();
		int height = b.getHeight();
		int[] array = new int[width * height];
		array = b.getRGB(0, 0, width, height, array, 0, width);

		int biomeCount = BIOMES.length;
		boolean[] found = new boolean[biomeCount];

		int cores = Runtime.getRuntime().availableProcessors();
		Looker[] lookers = new Looker[cores];

		for(int i = 0; i < cores; ++i) {
			lookers[i] = new Looker(array, found, i, cores);
			lookers[i].start();
		}

		for(int i = 0; i < cores; ++i) {
			try {
				lookers[i].join();
			} catch (Exception ex) {}
		}

		int count = 0;
		for(int i = 0; i < biomeCount; ++i) {
			if(found[i]) {
				++count;
			}
		}

		return new ScanContainer(count, found);
	}

	private String biomeList(boolean[] found) {
		StringBuilder builder = new StringBuilder();
		int l = BIOMES.length;
		boolean none = true;
		for(int i = 0; i < l; ++i) {
			if(found[i]) {
				builder.append(' ' + BIOMES[i].name());
				none = false;
			}
		}
		if(none) {
			builder.append(" none");
		}
		return builder.toString();
	}

	private class Continuous extends Thread {

		boolean running;

		public void run() {
			running = true;
			while(running) {
				cycle();
			}
		}

		public void cycle() {
			String seed = genSeedNumber();
			newSeed(seed);
			slep(screenShotWait);
			ScanContainer scan = scanScreenshot(screenshot());
			++checked;
			int total = scan.count;
			if(total > bestCount) {
				bestCount = total;
				bestSeed = seed;
				bestBiomes = scan.found;
				System.out.println("NEW BEST seed @" + checked + ": " + bestSeed + " total: " + bestCount + " found:" + biomeList(bestBiomes));
			} else if (total == bestCount){
				System.out.println("new seed @" + checked + ": " + seed + " total: " + total + " found:" + biomeList(scan.found));
			}
		}

		public void setRunning(boolean r) {
			running = r;
		}
	}

	private class Looker extends Thread {

		int[] array;
		boolean[] found;
		int numBiomes;
		int offset;
		int stride;
		int length;

		public Looker(int[] a, boolean[] f, int o, int s) {
			array = a;
			length = a.length;
			found = f;
			numBiomes = BIOMES.length;
			offset = o;
			stride = s;
		}

		public void run() {
			for(int i = offset; i < length; ) {
				int color = array[i];
				for(int j = 0; j < numBiomes; ++j) {
					int currentColor = BIOMES[j].getColor();
					if (color == (currentColor | 0xff000000)) {
						found[j] = true;
						break;
					}
				}
				i += stride;
			}
		}

	}

	private void printBounds() {
		System.out.println("x0: " + x0 + " y0: " + y0 + " x1: " + x1 + " y1: " + y1);
	}

	private boolean boundsReady() {
		return !(x0 == -1 || y0 == -1 || x1 == -1 || y1 == -1) ;
	}

	private void switchBounds() {
		if(boundsReady()) {
			if (x0 > x1) {
				int temp = x0;
				x0 = x1;
				x1 = temp;
			}
			if (y0 > y1) {
				int temp = y0;
				y0 = y1;
				y1 = temp;
			}
		}
	}

	/**
	 * The main method.
	 * @param arguments - Command-line arguments
	 * @throws IOException
	 * @throws NativeHookException
	 */
	public static void main(String arguments[]) throws IOException, NativeHookException {
		LogManager.getLogManager().reset();
		GlobalScreen.registerNativeHook();
		GlobalScreen.addNativeKeyListener(new KeyLogger());
	}
}