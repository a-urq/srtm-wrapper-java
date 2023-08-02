package com.ameliaWx.srtmWrapper;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Scanner;

import org.apache.commons.io.FileUtils;

public class ColorScale {
	// write this to read in *.pal files, unmodified. use sam emmerson's colortables
	// as tests
	
	private static final int NUM_MASKS = 12; // number of precip types supported
	private Color[][] colors = new Color[NUM_MASKS][];
	private Color noData;
	private Color rangeFolded;

	private double ndValue = -1024;
	private double rfValue = -2048;

	private double vmax;
	private double vmin;
	private String units;
	
	private double interval;

	private double scale = 1;
	
	public ColorScale(File f, double resolution, double interval, String units) {
		this.units = units;
		this.interval = interval;
		
		Scanner sc = null;
		try {
			sc = new Scanner(f);
		} catch (FileNotFoundException e) {
			vmax = 0.0;
			vmin = 0.0;

			e.printStackTrace();
		}

		ArrayList<String[]> tokensList = new ArrayList<>();

		while (sc.hasNextLine()) {
			String line = sc.nextLine();

			if (line.length() == 0)
				continue;
			if (';' == line.charAt(0))
				continue;

			if (line.startsWith("Color:")) {
				StringBuilder tokensStr = new StringBuilder();

				Scanner lineSc = new Scanner(line);

				while (lineSc.hasNext()) {
					tokensStr.append(lineSc.next() + " ");
				}
				lineSc.close();

				String[] tokens = tokensStr.toString().split(" ");

//				System.out.println(Arrays.toString(tokens));

				tokensList.add(tokens);
			}
			
			if(line.startsWith("Scale:")) {
				StringBuilder tokensStr = new StringBuilder();

				Scanner lineSc = new Scanner(line);

				while (lineSc.hasNext()) {
					tokensStr.append(lineSc.next() + " ");
				}
				lineSc.close();

				String[] tokens = tokensStr.toString().split(" ");
				
				scale = Double.valueOf(tokens[1]);
//				System.out.println("scale: " + scale);
			}
			
			if(line.startsWith("ND:")) {
				StringBuilder tokensStr = new StringBuilder();

				Scanner lineSc = new Scanner(line);

				while (lineSc.hasNext()) {
					tokensStr.append(lineSc.next() + " ");
				}
				lineSc.close();

				String[] tokens = tokensStr.toString().split(" ");
				
				noData = new Color(Integer.valueOf(tokens[1]), Integer.valueOf(tokens[2]), Integer.valueOf(tokens[3]));
			}
			
			if(line.startsWith("RF:")) {
				StringBuilder tokensStr = new StringBuilder();

				Scanner lineSc = new Scanner(line);

				while (lineSc.hasNext()) {
					tokensStr.append(lineSc.next() + " ");
				}
				lineSc.close();

				String[] tokens = tokensStr.toString().split(" ");
				
				rangeFolded = new Color(Integer.valueOf(tokens[1]), Integer.valueOf(tokens[2]), Integer.valueOf(tokens[3]));
			}
		}

		int[] colorKeyAmt = new int[NUM_MASKS];
		for (int i = 0; i < tokensList.size(); i++) {
			int mask = 0;
			int rgbValuesListed = (tokensList.get(i).length - 2) / 3;

			colorKeyAmt[mask] += rgbValuesListed;
		}

		double[][] colorKeys = new double[NUM_MASKS][];
		Color[][] colorValues = new Color[NUM_MASKS][];

		for (int i = 0; i < NUM_MASKS; i++) {
			colorKeys[i] = new double[colorKeyAmt[i]];
			colorValues[i] = new Color[colorKeyAmt[i]];
		}

		int[] colorKeysProcessed = new int[NUM_MASKS];
		for (int mask = 0; mask < NUM_MASKS; mask++) {
			for (int i = 0; i < tokensList.size(); i++) {
				String[] tokens = tokensList.get(i);
				int rgbValuesListed = (tokens.length - 2) / 3;

				int selectedMask = 0;

				if (mask == selectedMask) {
					for (int j = 0; j < rgbValuesListed; j++) {
						colorKeys[mask][colorKeysProcessed[mask]] = Double.valueOf(tokens[1]);
						colorValues[mask][colorKeysProcessed[mask]] = new Color(Integer.valueOf(tokens[2 + 3 * j]),
								Integer.valueOf(tokens[3 + 3 * j]), Integer.valueOf(tokens[4 + 3 * j]));
						
						colorKeysProcessed[mask]++;
					}
				}
			}
			
			shiftKeys(colorKeys[mask]);
		}

		vmin = colorKeys[0][0];
		vmax = colorKeys[0][colorKeys[0].length - 1];

		for (int i = 0; i < NUM_MASKS; i++) {
			colors[i] = new Color[(int) Math.round((vmax - vmin) / resolution) + 1];

			for (double v = vmin; v <= vmax; v += resolution) {
				int index = (int) Math.round((v - vmin) / resolution);
				colors[i][index] = colorLerp(v, colorKeys[i], colorValues[i]);
			}
		}

		sc.close();
	}

	// value should NEVER be outside keys. as such, handling for that case is not
	// implemented
	private Color colorLerp(double value, double[] keys, Color[] values) {
		if(values.length == 0) return Color.BLACK;
		
		for (int i = 0; i < keys.length - 1; i++) {
			if (value > keys[i + 1])
				continue;
			if (keys[i] == keys[i + 1])
				return values[i];

			double w1 = (keys[i + 1] - value) / (keys[i + 1] - keys[i]);
			double w2 = (value - keys[i]) / (keys[i + 1] - keys[i]);

			int r1 = values[i].getRed();
			int g1 = values[i].getGreen();
			int b1 = values[i].getBlue();
			int r2 = values[i + 1].getRed();
			int g2 = values[i + 1].getGreen();
			int b2 = values[i + 1].getBlue();

			Color c = new Color((int) (r1 * w1 + r2 * w2), (int) (g1 * w1 + g2 * w2), (int) (b1 * w1 + b2 * w2));
			
			return c;
		}

		return values[values.length - 1];
	}
	
	private void shiftKeys(double[] keys) {
		for(int i = 0; i < keys.length - 2; i++) {
			if(keys[i] == keys[i + 1]) {
				keys[i + 1] = keys[i + 2];
				i++;
			}
		}
	}

	public Color getColor(double value) {
		return getColor(value, 0);
	}

	public Color getColor(double value, int mask) {
		if (vmax == vmin)
			return Color.BLACK;

		if (value == ndValue)
			return noData;
		if (value == rfValue)
			return rangeFolded;

		double resolution = (vmax - vmin) / colors[mask].length;
		int index = (int) Math.round((value * scale - vmin) / resolution);

		if (index < 0)
			return colors[mask][0];
		if (index >= colors[0].length)
			return colors[mask][colors.length - 1];

		return colors[mask][index];
	}

	public double getNdValue() {
		return ndValue;
	}

	public void setNdValue(double ndValue) {
		this.ndValue = ndValue;
	}

	public double getRfValue() {
		return rfValue;
	}

	public void setRfValue(double rfValue) {
		this.rfValue = rfValue;
	}
	
	public BufferedImage drawColorLegend(int width, int height, int padding, boolean vertical) {
		BufferedImage legend = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		Graphics2D g = legend.createGraphics();

		g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, width, height);
		g.setColor(Color.WHITE);
		g.drawLine(0, 0, 0, height);
		
		if(vertical) {
			for(int i = 0; i < height - 2 * padding; i++) {
				double val = vmin + (vmax - vmin) / (height - 2 * padding) * i;
				
				g.setColor(getColor(val / scale));
				g.fillRect(padding, height - padding - i, width - 2 * padding, 1);
			}
			
			int numMarks = (int) Math.round((vmax - vmin) / interval);
			g.setColor(Color.WHITE);
			for(int i = 0; i <= numMarks; i++) {
				int y = (int) ((height - padding) - (height - 2.0 * padding) / numMarks * i);
				double val = vmin + (vmax - vmin) / numMarks * i;
				
				if("C".equals(units)) val = convertKtoC(val);
				if("F".equals(units)) val = convertKtoF(val);
				
				g.fillRect(padding, y, width - 2 * padding, 1);
				drawCenteredString(g, String.format("%6.1f " + units, val), new Rectangle(width - padding + 28, y, 0, 0), g.getFont());
			}
		} else {
			for(int i = 0; i < width - 2 * padding; i++) {
				double val = vmin + (vmax - vmin) / (width - 2 * padding) * i;
				
				g.setColor(getColor(val / scale));
				g.fillRect(i + padding, padding, 1, height - 2 * padding);
			}
			
			int numMarks = (int) (Math.round(vmax - vmin) / interval);
			g.setColor(Color.WHITE);
			for(int i = 0; i <= numMarks; i++) {
				int y = (int) ((width - padding) - (width - 2.0 * padding) / numMarks * i);
				double val = vmin + (vmax - vmin) / numMarks * i;
				
				if("C".equals(units)) val = convertKtoC(val);
				if("F".equals(units)) val = convertKtoF(val);
				
				g.fillRect(padding, y, height - 2 * padding, 1);
				
				String u = (i == 0) ? " " + units : "";
				
				drawCenteredString(g, String.format("%6.1f" + u, val), new Rectangle(y, padding - 25, 0, 0), g.getFont());
			}
		}
		
		g.dispose();
		return legend;
	}
	
	/**
     * Draw a String centered in the middle of a Rectangle.
     *
     * @param g    The Graphics instance.
     * @param text The String to draw.
     * @param rect The Rectangle to center the text in.
     */
    public static void drawCenteredString(Graphics2D g, String text, Rectangle rect, Font font) {
        // Get the FontMetrics
        FontMetrics metrics = g.getFontMetrics(font);
        // Determine the X coordinate for the text
        int x = rect.x + (rect.width - metrics.stringWidth(text)) / 2;
        // Determine the Y coordinate for the text (note we add the ascent, as in java
        // 2d 0 is top of the screen)
        int y = rect.y + (rect.height + metrics.getHeight()) / 3;
        // Set the font
        g.setFont(font);
        // Draw the String
        g.drawString(text, x, y);
    }
    
    private static double convertKtoC(double k) {
    	return k - 273.15;
    }
    
    private static double convertCtoF(double c) {
    	return 1.8 * c + 32.0;
    }
    
    private static double convertKtoF(double k) {
    	return convertCtoF(convertKtoC(k));
    }

	public static File loadResourceAsFile(String urlStr) {
//		System.out.println(urlStr);
		URL url = ColorScale.class.getResource(urlStr);
//		System.out.println(url);
//		System.out.println(is);
		URL tilesObj = url;

		// System.out.println("Temp-file created.");

//		RadarView.tempFilesToDelete.add(RadarView.dataFolder + "temp/" + urlStr + "");
		File file = new File("temp/" + urlStr + "");

		if (tilesObj == null) {
			System.out.println("Loading failed to start.");
			return null;
		}

		// System.out.println("Loading successfully started.");

		try {
			FileUtils.copyURLToFile(tilesObj, file);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return null;
		}

		return file;
	}
}
