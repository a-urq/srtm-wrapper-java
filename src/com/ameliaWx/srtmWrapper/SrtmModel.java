package com.ameliaWx.srtmWrapper;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
//import java.awt.Graphics2D;
//import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Scanner;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;

import ucar.ma2.Array;
import ucar.nc2.Variable;

//import javax.imageio.ImageIO;
//import javax.net.ssl.HostnameVerifier;
//import javax.net.ssl.HttpsURLConnection;
//import javax.net.ssl.SSLContext;
//import javax.net.ssl.SSLEngine;
//import javax.net.ssl.SSLSession;
//import javax.net.ssl.TrustManager;
//import javax.net.ssl.X509ExtendedTrustManager;

// finalize this into the getElevation(double latitude, double longitude) functionality
// have option between nearest neighbor and bilinear, make nearest neighbor default
// cache 3 previously loaded files in addition to the currently active file to prevent excessive file loading
// probably rename srtmData-12bit to just srtmData
// unload srtmRawData off to external drive. DO NOT DELETE IN CASE I NEED IT AGAIN
public class SrtmModel {
	/**
	 * make dynamically assigned lapse rates based on temp and HRRR elev data
	 */

	private String tempFilePath;
	private short[][][] cachedData;
	private String[] cachedFileNames;
	private int mostRecentlyUsedCacheSlot;
	private int mostRecentlyLoadedCacheSlot;

	private boolean[][] filePresent = new boolean[72][24];
//	
//	public static void main(String[] args) throws IOException {
//		ColorScale aruElev = new ColorScale(ColorScale.loadResourceAsFile("aruElev.pal"), 0.1, 1320, " ft");
//		ColorScale aruTmp = new ColorScale(ColorScale.loadResourceAsFile("aruTmp.pal"), 0.1, 10, " K");
//		
//		NetcdfFile ncfileElev = NetcdfFile.open("/home/a-urq/eclipse-workspace/RadarViewTakeThree/hrrr-elev.grib2");
//		NetcdfFile ncfileTmp = NetcdfFile.open("/home/a-urq/eclipse-workspace/RadarViewTakeThree/hrrr.t12z.wrfsubhf00.grib2");
//		
//		Variable hrrrElev = ncfileElev.findVariable("Geopotential_height_surface");
//		Variable hrrrTmp = ncfileTmp.findVariable("Temperature_height_above_ground");
//		
//		double[][] coloradoElevHrrrRaw = extractNetcdfToData(hrrrElev, 0);
//		double[][] coloradoTmpHrrrRaw = extractNetcdfToData(hrrrTmp, 0);
//		
//		double[][] coloradoHrrrLapseRateI = new double[coloradoTmpHrrrRaw.length][coloradoTmpHrrrRaw[0].length];
//		double[][] coloradoHrrrLapseRateJ = new double[coloradoTmpHrrrRaw.length][coloradoTmpHrrrRaw[0].length];
//		
//		for(int i = 0; i < coloradoTmpHrrrRaw.length; i++) {
//			for(int j = 0; j < coloradoTmpHrrrRaw[i].length; j++) {
//				if(i != coloradoTmpHrrrRaw.length - 1) {
//					coloradoHrrrLapseRateI[i][j] = (coloradoTmpHrrrRaw[i + 1][j] - coloradoTmpHrrrRaw[i][j])/(coloradoElevHrrrRaw[i + 1][j] - coloradoElevHrrrRaw[i][j]);
//					System.out.println(coloradoHrrrLapseRateI[i][j]);
//				}
//				
//				if(j != coloradoTmpHrrrRaw[i].length - 1) {
//					coloradoHrrrLapseRateJ[i][j] = (coloradoTmpHrrrRaw[i][j + 1] - coloradoTmpHrrrRaw[i][j])/(coloradoElevHrrrRaw[i][j + 1] - coloradoElevHrrrRaw[i][j]);
//				}
//			}
//		}
//		
//		SrtmModel srtm = new SrtmModel("/home/a-urq/Documents/RadarView/data/");
//
//		// for colorado
//		double[][] coloradoElevSrtm = new double[9 * 400 + 1][6 * 400 + 1];
//		double[][] coloradoElevHrrr = new double[9 * 400 + 1][6 * 400 + 1];
//		double[][] coloradoTmpHrrr = new double[9 * 400 + 1][6 * 400 + 1];
//		double[][] coloradoLapseRateHrrr = new double[9 * 400 + 1][6 * 400 + 1];
//		
//		// for texas
////		double[][] coloradoElevSrtm = new double[15 * 400 + 1][12 * 400 + 1];
////		double[][] coloradoElevHrrr = new double[15 * 400 + 1][12 * 400 + 1];
////		double[][] coloradoTmpHrrr = new double[15 * 400 + 1][12 * 400 + 1];
////		double[][] coloradoLapseRateHrrr = new double[15 * 400 + 1][12 * 400 + 1];
//		
//		for(int i = 0; i < coloradoElevSrtm.length; i++) {
//			for(int j = 0; j < coloradoElevSrtm[0].length; j++) {
//				// for colorado
//				double latitude = 42.0 - (j / 400.0);
//				double longitude = -110.0 + (i / 400.0);
//				
//				// for texas
////				double latitude = 37.0 - (j / 400.0);
////				double longitude = -107.0 + (i / 400.0);
//				
//				PointD hrrrIJ = LambertConformalProjection.hrrrProj.projectLatLonToIJ(longitude, latitude);
//				hrrrIJ.add(new PointD(0, -1));
//
//				coloradoElevSrtm[i][j] = srtm.getElevation(latitude, longitude);
//				coloradoElevHrrr[i][j] = bilinearInterp(coloradoElevHrrrRaw, hrrrIJ.getX(), hrrrIJ.getY());
//				coloradoTmpHrrr[i][j] = bilinearInterp(coloradoTmpHrrrRaw, hrrrIJ.getX(), hrrrIJ.getY());
//				
//				double weightI2 = hrrrIJ.getY() % 1.0;
//				double weightI1 = 1 - (weightI2);
//				double weightJ2 = hrrrIJ.getX() % 1.0;
//				double weightJ1 = 1 - (weightJ2);
//				
//				double lapseRateI1 = coloradoHrrrLapseRateI[(int) Math.floor(hrrrIJ.getX())][(int) Math.floor(hrrrIJ.getY())];
//				double lapseRateI2 = coloradoHrrrLapseRateI[(int) Math.floor(hrrrIJ.getX())][(int) Math.floor(hrrrIJ.getY() + 1)];
//				double lapseRateJ1 = coloradoHrrrLapseRateI[(int) Math.floor(hrrrIJ.getX())][(int) Math.floor(hrrrIJ.getY())];
//				double lapseRateJ2 = coloradoHrrrLapseRateI[(int) Math.floor(hrrrIJ.getX() + 1)][(int) Math.floor(hrrrIJ.getY())];
//				
//				double lapseRate = (weightI1 * lapseRateI1 + weightI2 * lapseRateI2 + weightJ1 * lapseRateJ1 + weightJ2 * lapseRateJ2)/2.0;
//				
//				coloradoLapseRateHrrr[i][j] = lapseRate;
//			}
//		}
//		
//		BufferedImage coloradoElevHrrrImg = new BufferedImage(coloradoElevHrrr.length, coloradoElevHrrr[0].length, BufferedImage.TYPE_3BYTE_BGR);
//		Graphics2D g = coloradoElevHrrrImg.createGraphics();
//		
//		for(int i = 0; i < coloradoElevSrtm.length; i++) {
//			for(int j = 0; j < coloradoElevSrtm[0].length; j++) {
//				g.setColor(aruElev.getColor(coloradoElevHrrr[i][j]));
//				g.fillRect(i, j, 1, 1);
//			}
//		}
//		
//		BufferedImage coloradoBasemap = ImageIO.read(new File("/home/a-urq/eclipse-workspace/RadarViewTakeThree/northAmericaBasemap-colorado-400.png"));
//		g.drawImage(coloradoBasemap, 0, 0, null);
//		
//		ImageIO.write(coloradoElevHrrrImg, "PNG", new File("colorado-elev-test-hrrr.png"));
//		
//		coloradoElevHrrrImg = new BufferedImage(coloradoElevHrrr.length, coloradoElevHrrr[0].length, BufferedImage.TYPE_3BYTE_BGR);
//		g = coloradoElevHrrrImg.createGraphics();
//		
//		for(int i = 0; i < coloradoElevSrtm.length; i++) {
//			for(int j = 0; j < coloradoElevSrtm[0].length; j++) {
//				g.setColor(aruTmp.getColor(coloradoTmpHrrr[i][j]));
//				g.fillRect(i, j, 1, 1);
//			}
//		}
//		
//		g.drawImage(coloradoBasemap, 0, 0, null);
//		
//		ImageIO.write(coloradoElevHrrrImg, "PNG", new File("colorado-tmp-test-hrrr.png"));
//		
//		coloradoElevHrrrImg = new BufferedImage(coloradoElevHrrr.length, coloradoElevHrrr[0].length, BufferedImage.TYPE_3BYTE_BGR);
//		g = coloradoElevHrrrImg.createGraphics();
//		
//		for(int i = 0; i < coloradoElevSrtm.length; i++) {
//			for(int j = 0; j < coloradoElevSrtm[0].length; j++) {
////				System.out.println(coloradoLapseRateHrrr[i][j]);
//				
//				g.setColor(aruTmp.getColor(coloradoTmpHrrr[i][j] - 0.0065 * (coloradoElevSrtm[i][j] - coloradoElevHrrr[i][j])));
//				g.fillRect(i, j, 1, 1);
//			}
//		}
//		
//		g.drawImage(coloradoBasemap, 0, 0, null);
//		
//		ImageIO.write(coloradoElevHrrrImg, "PNG", new File("colorado-tmp-test-srtm-adjusted.png"));
//	}

	private static double bilinearInterp(double[][] data, double i, double j) {
		double data00 = data[(int) i][(int) j];
		double data10 = data[(int) i + 1][(int) j];
		double data01 = data[(int) i][(int) j + 1];
		double data11 = data[(int) i + 1][(int) j + 1];

		double weightI = i % 1;
		double weightJ = j % 1;

		double interpData = data00 * (1 - weightI) * (1 - weightJ) + data10 * (weightI) * (1 - weightJ)
				+ data01 * (1 - weightI) * (weightJ) + data11 * (weightI) * (weightJ);

		return interpData;
	}

	private static double[][] extractNetcdfToData(Variable var, int subindex) {
		double[][] data = new double[1799][1059];

		Array arr = null;
		try {
			arr = var.read();
		} catch (IOException e1) {

		}

		for (int i = 0; i < 1799; i++) {
			for (int j = 0; j < 1059; j++) {
				data[i][1058 - j] = arr.getDouble(1799 * 1059 * subindex + 1799 * j + i);
			}
		}

		return data;
	}

	public SrtmModel(String tempFilePath) {
		this(tempFilePath, 9);
	}

	public SrtmModel(String tempFilePath, int cacheSize) {
		this.tempFilePath = tempFilePath;

		cachedData = new short[cacheSize][2000][2000];
		cachedFileNames = new String[cacheSize];
		mostRecentlyUsedCacheSlot = -1;
		mostRecentlyLoadedCacheSlot = -1;

		for (int i = 0; i < filePresent.length; i++) {
			for (int j = 0; j < filePresent[0].length; j++) {
				File dataFile = loadResourceAsFile("srtmData/srtm_" + String.format("%02d", i + 1) + "_"
						+ String.format("%02d", j + 1) + ".elev.gz");

				filePresent[i][j] = (dataFile != null);

				if (dataFile != null)
					dataFile.delete();
			}
		}
	}

	public double getElevation(double latitude, double longitude) {
		// special handling for a data void in lake superior
		if (latitude >= 47 && latitude <= 48 && longitude >= -87 && longitude <= -86) {
			return 160.0;
		}

		int fileI = (int) Math.floor(0.2 * (longitude + 180)) + 1;
		int fileJ = (int) Math.ceil(-0.2 * (latitude - 60));

		longitude = (longitude + 360.0) % 360.0;

		String fileIdentifier = String.format("%02d", fileI) + "_" + String.format("%02d", fileJ);

		if (mostRecentlyUsedCacheSlot == -1) {
			if (filePresent[fileI - 1][fileJ - 1]) {
				mostRecentlyUsedCacheSlot = 0;
				mostRecentlyLoadedCacheSlot = 0;

				cachedData[mostRecentlyUsedCacheSlot] = loadElevData(fileI, fileJ);
				cachedFileNames[mostRecentlyUsedCacheSlot] = fileIdentifier;

				int dataI = (int) Math.round((longitude % 5) * 400);
				int dataJ = 1999 - (int) Math.round((latitude % 5) * 400);

				double ret = cachedData[mostRecentlyUsedCacheSlot]
						[dataI]
								[dataJ];
				if (ret == -1024.0)
					ret = 0.0;
				return ret;
			} else {
				return 0.0;
			}
		} else {
			// if current file identifier matched most recently used cached identifier, use
			// that cache slot
			if (fileIdentifier.equals(cachedFileNames[mostRecentlyUsedCacheSlot])) {
				int dataI = (int) Math.round((longitude % 5) * 400);
				int dataJ = 1999 - (int) Math.round((latitude % 5) * 400);

				double ret = cachedData[mostRecentlyUsedCacheSlot][dataI][dataJ];
				if (ret == -1024.0)
					ret = 0.0;
				return ret;
			} else {
				// check all cached file identifiers to see if active file identifier matches.
				// if not, proceed to load new elev file
				for (int i = 0; i < cachedFileNames.length; i++) {
					if (i != mostRecentlyUsedCacheSlot) {
						if (fileIdentifier.equals(cachedFileNames[i])) {
							mostRecentlyUsedCacheSlot = i;

							int dataI = (int) Math.round((longitude % 5) * 400);
							int dataJ = 1999 - (int) Math.round((latitude % 5) * 400);

							double ret = cachedData[i][dataI][dataJ];
							if (ret == -1024.0)
								ret = 0.0;
							return ret;
						}
					}
				}

				// if data requested is not contained within the cache, load into LEAST recently
				// used cache slot
				mostRecentlyLoadedCacheSlot++;
				if (mostRecentlyLoadedCacheSlot == cachedData.length)
					mostRecentlyLoadedCacheSlot = 0;

				mostRecentlyUsedCacheSlot = mostRecentlyLoadedCacheSlot;

				cachedData[mostRecentlyUsedCacheSlot] = loadElevData(fileI, fileJ);
				cachedFileNames[mostRecentlyUsedCacheSlot] = fileIdentifier;

				int dataI = (int) Math.round((longitude % 5) * 400);
				int dataJ = 1999 - (int) Math.round((latitude % 5) * 400);

				double ret = cachedData[mostRecentlyUsedCacheSlot][dataI][dataJ];
				if (ret == -1024.0)
					ret = 0.0;
				return ret;
			}
		}
	}

	public short[][] loadElevData(int fileI, int fileJ) {
//		System.out.println("most recently used cache slot: " + mostRecentlyUsedCacheSlot);
//		System.out.println("most recently loaded cache slot: " + mostRecentlyLoadedCacheSlot);
		
		String dataResourcePath = "srtmData/srtm_" + String.format("%02d", fileI) + "_" + String.format("%02d", fileJ)
				+ ".elev.gz";

		boolean resourceExists = checkIfResourceExists(dataResourcePath);

		if (resourceExists) {
			File dataFile = loadResourceAsFile(dataResourcePath);
			String dataFilePath = dataFile.getAbsolutePath();
			String dataFileUnzipPath = dataFilePath.substring(0, dataFilePath.length() - 3);

			decompressGzipFile(dataFilePath, dataFileUnzipPath);
			dataFile.delete();

			try {
				File dataFileUnzipped = new File(dataFileUnzipPath);

				short[][] elevData = loadElev(dataFileUnzipped);
				dataFileUnzipped.delete();

				return elevData;
			} catch (IOException e) {
				return new short[2000][2000];
			}
		} else {
			return new short[2000][2000];
		}
	}

	private static void mainOld(String[] args) throws IOException {
		for (int i = 100; i <= 28; i++) {
			for (int j = 100; j <= 9; j++) {
				try {
					if (new File(
							"/home/a-urq/eclipse-workspace/SRTMWrapper/src/com/zanderWx/srtmWrapper/srtmRawData/srtm_"
									+ String.format("%02d", i) + "_" + String.format("%02d", j) + "/srtm_"
									+ String.format("%02d", i) + "_" + String.format("%02d", j) + ".asc")
							.exists()) {

						short[][] elev = readEsriAsciiFile(new File(
								"/home/a-urq/eclipse-workspace/SRTMWrapper/src/com/zanderWx/srtmWrapper/srtmRawData/srtm_"
										+ String.format("%02d", i) + "_" + String.format("%02d", j) + "/srtm_"
										+ String.format("%02d", i) + "_" + String.format("%02d", j) + ".asc"));

						writeElevFile(elev,
								"/home/a-urq/eclipse-workspace/SRTMWrapper/src/com/zanderWx/srtmWrapper/srtmData-12bit/srtm_"
										+ String.format("%02d", i) + "_" + String.format("%02d", j) + ".elev");

						compressGzipFile(
								"/home/a-urq/eclipse-workspace/SRTMWrapper/src/com/zanderWx/srtmWrapper/srtmData-12bit/srtm_"
										+ String.format("%02d", i) + "_" + String.format("%02d", j) + ".elev",
								"/home/a-urq/eclipse-workspace/SRTMWrapper/src/com/zanderWx/srtmWrapper/srtmData-12bit/srtm_"
										+ String.format("%02d", i) + "_" + String.format("%02d", j) + ".elev.gz");

						new File(
								"/home/a-urq/eclipse-workspace/SRTMWrapper/src/com/zanderWx/srtmWrapper/srtmData-12bit/srtm_"
										+ String.format("%02d", i) + "_" + String.format("%02d", j) + ".elev")
								.delete();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

//		System.exit(0);

		ColorScale aruElev = new ColorScale(ColorScale.loadResourceAsFile("aruElev.pal"), 0.1, 1320, " ft");

//		BufferedImage elevScaleTestImg = new BufferedImage(2112, 100, BufferedImage.TYPE_3BYTE_BGR);
//		Graphics2D g = elevScaleTestImg.createGraphics();
//
//		for(int i = 0; i < 2112; i++) {
//			g.setColor(aruElev.getColor(10 * i / 3.28084));
//			g.fillRect(i, 0, 1, 100);
//		}
//		
//		ImageIO.write(elevScaleTestImg, "PNG", new File("elevScale.png"));

//		decompressGzipFile("/home/a-urq/eclipse-workspace/SRTMWrapper/src/com/zanderWx/srtmWrapper/srtmData-12bit/srtm_15_05.elev.gz", 
//				"/home/a-urq/eclipse-workspace/SRTMWrapper/src/com/zanderWx/srtmWrapper/srtmData-12bit/srtm_15_05.elev");
//		
//		File testFileGz = new File("/home/a-urq/eclipse-workspace/SRTMWrapper/src/com/zanderWx/srtmWrapper/srtmData-12bit/srtm_15_05.elev");
//		testFileGz.deleteOnExit();
//		
//		short[][] elevTestData = loadElev(testFileGz);
//
//		BufferedImage elevTestImg = new BufferedImage(2000, 2000, BufferedImage.TYPE_3BYTE_BGR);
//		Graphics2D g = elevTestImg.createGraphics();
//
//		for (int i = 0; i < elevTestData.length; i++) {
//			for (int j = 0; j < elevTestData[i].length; j++) {
//				g.setColor(aruElev.getColor(elevTestData[i][j] + 0.1));
//				g.fillRect(i, j, 1, 1);
//			}
//		}
//
//		ImageIO.write(elevTestImg, "PNG", new File("elevTest-read-15_05.png"));

		int fileI = 17;
		int fileJ = 6;

		decompressGzipFile(
				"/home/a-urq/eclipse-workspace/SRTMWrapper/src/com/zanderWx/srtmWrapper/srtmData-12bit/srtm_"
						+ String.format("%02d", fileI) + "_" + String.format("%02d", fileJ) + ".elev.gz",
				"/home/a-urq/eclipse-workspace/SRTMWrapper/src/com/zanderWx/srtmWrapper/srtmData-12bit/srtm_"
						+ String.format("%02d", fileI) + "_" + String.format("%02d", fileJ) + ".elev");

		File testFileGz = new File(
				"/home/a-urq/eclipse-workspace/SRTMWrapper/src/com/zanderWx/srtmWrapper/srtmData-12bit/srtm_"
						+ String.format("%02d", fileI) + "_" + String.format("%02d", fileJ) + ".elev");

		short[][] elevData = loadElev(testFileGz);
		testFileGz.delete();

		BufferedImage northAmericaMap = new BufferedImage(28000, 9000, BufferedImage.TYPE_3BYTE_BGR);
		Graphics2D g = northAmericaMap.createGraphics();

		for (int x = 1; x <= 28; x++) {
			System.out.println(x);
			for (int y = 1; y <= 9; y++) {
				fileI = x;
				fileJ = y;

				if (new File(
						"/home/a-urq/eclipse-workspace/SRTMWrapper/src/com/zanderWx/srtmWrapper/srtmData-12bit/srtm_"
								+ String.format("%02d", fileI) + "_" + String.format("%02d", fileJ) + ".elev.gz")
						.exists()) {
					decompressGzipFile(
							"/home/a-urq/eclipse-workspace/SRTMWrapper/src/com/zanderWx/srtmWrapper/srtmData-12bit/srtm_"
									+ String.format("%02d", fileI) + "_" + String.format("%02d", fileJ) + ".elev.gz",
							"/home/a-urq/eclipse-workspace/SRTMWrapper/src/com/zanderWx/srtmWrapper/srtmData-12bit/srtm_"
									+ String.format("%02d", fileI) + "_" + String.format("%02d", fileJ) + ".elev");

					testFileGz = new File(
							"/home/a-urq/eclipse-workspace/SRTMWrapper/src/com/zanderWx/srtmWrapper/srtmData-12bit/srtm_"
									+ String.format("%02d", fileI) + "_" + String.format("%02d", fileJ) + ".elev");

					elevData = loadElev(testFileGz);
					testFileGz.delete();
				} else {
					elevData = new short[2001][2000];
				}

				for (int i = 0; i < 1000; i++) {
					for (int j = 0; j < 1000; j++) {
						g.setColor((elevData.length == 2000) ? aruElev.getColor(elevData[2 * i][2 * j] - 18)
								: aruElev.getColor(-1024.0));
						g.fillRect(1000 * (x - 1) + i, 1000 * (y - 1) + j, 1, 1);
					}
				}
			}
		}

		g.dispose();

		ImageIO.write(northAmericaMap, "PNG", new File("northAmericaElevMap-modified-18.png"));
	}

	private static short[][] readEsriAsciiFile(File f) throws FileNotFoundException {
		short[][] elev = new short[6000][6000];

		Scanner sc = new Scanner(f);

		sc.nextLine();
		sc.nextLine();
		sc.nextLine();
		sc.nextLine();
		sc.nextLine();
		sc.nextLine();

		for (int i = 0; i < elev.length; i++) {
			for (int j = 0; j < elev[i].length; j++) {
				elev[j][i] = Short.valueOf(sc.nextShort());

				if (elev[j][i] == -9999)
					elev[j][i] = -1024;
			}
		}

		sc.close();

		return elev;
	}

	private static void writeElevFile(short[][] elev, String filename) throws IOException {
		FileOutputStream os = new FileOutputStream(new File(filename));

		for (int i = 0; i < elev.length; i += 3) {
			for (int j = 0; j < elev[i].length; j += 6) {
				short val0 = elev[i][j];
				short val1 = elev[i][j + 3];

				byte[] encodedBytes = encode12bitShortToBinary(val0, val1);

				os.write(encodedBytes);
			}
		}

		os.close();
	}

	private static byte[] encode12bitShortToBinary(short sh0, short sh1) {
		byte[] ret = new byte[3];

		byte val0byte0 = (byte) ((sh0 >> 8) & 0b11111111);
		byte val0byte1 = (byte) (sh0 & 0b11111111);
		byte val1byte0 = (byte) ((sh1 >> 8) & 0b11111111);
		byte val1byte1 = (byte) (sh1 & 0b11111111);

		byte wByte0 = val0byte0;
		byte wByte1A = (byte) (val0byte1 & 0b11110000);
		byte wByte1B = (byte) ((val1byte0 >> 4) & 0b00001111);
		byte wByte2 = (byte) (((val1byte0 << 4) & 0b11110000) + ((val1byte1 >> 4) & 0b00001111));

		byte wByte1 = (byte) (wByte1A + wByte1B);

		ret[0] = wByte0;
		ret[1] = wByte1;
		ret[2] = wByte2;

		return ret;
	}

	private static short[] decodeBinaryTo12bitShort(byte b0, byte b1, byte b2) {
		short[] ret = new short[2];

		byte byteSegment0 = b0;
		byte byteSegment1 = (byte) (b1 & 0b11110000);
		byte byteSegment2 = (byte) (b1 & 0b00001111);
		byte byteSegment3 = b2;

		short rVal0 = (short) (byteSegment0 << 8);
		rVal0 |= ((short) (byteSegment1) & 255);

		short rVal1 = (short) (byteSegment2 << 12);
		rVal1 += (short) (((short) (byteSegment3) & 255) << 4);

		ret[0] = rVal0;
		ret[1] = rVal1;

		return ret;
	}

	private static short[][] loadElev(File f) throws IOException {
		return loadElev(f, 4096);
	}

	private static short[][] loadElev(File f, int bufferSizeKibiEntries) throws IOException {
		short[][] elev = new short[2000][2000];

		FileInputStream is = new FileInputStream(f);

		int i = 0, j = 0;

		byte[] buffer = new byte[1024 * bufferSizeKibiEntries * 3];
		int status = 0;
		boolean broken = false;
		while (status != -1) {
			status = is.read(buffer);

			if (broken)
				break;

			for (int b = 0; b < buffer.length; b += 3) {
				byte b0 = buffer[b];
				byte b1 = buffer[b + 1];
				byte b2 = buffer[b + 2];

				short[] decodedData = decodeBinaryTo12bitShort(b0, b1, b2);

				elev[i][j] = decodedData[0];
				elev[i][j + 1] = decodedData[1];

				j += 2;
				if (j >= elev[0].length) {
					j = 0;
					i++;
				}

				if (i >= 2000) {
					broken = true;
					break;
				}
			}
		}

		is.close();

		return elev;
	}

//	private static String printBinary(short sh) {
//		String s = "";
//
//		for (int i = 15; i >= 0; i--) {
//			s += (sh >> i & (short) 1);
//
//			if (i == 8)
//				s += "-";
//		}
//
//		return s;
//	}
//
//	private static String printBinary(byte b) {
//		String s = "";
//
//		for (int i = 7; i >= 0; i--) {
//
//			s += (b >> i & (byte) 1);
//		}
//
//		return s;
//	}

	/*
	 * Example of reading Zip archive using ZipFile class
	 */

//	private static void readUsingZipFile(String fileName, String outputDir) throws IOException {
//		new File(outputDir).mkdirs();
//		final ZipFile file = new ZipFile(fileName);
//		// System.out.println("Iterating over zip file : " + fileName);
//
//		try {
//			final Enumeration<? extends ZipEntry> entries = file.entries();
//			while (entries.hasMoreElements()) {
//				final ZipEntry entry = entries.nextElement();
//				extractEntry(entry, file.getInputStream(entry), outputDir);
//			}
//			// System.out.printf("Zip file %s extracted successfully in %s",
//			// fileName, outputDir);
//		} finally {
//			file.close();
//		}
//	}

	/*
	 * Utility method to read data from InputStream
	 */

//	private static void extractEntry(final ZipEntry entry, InputStream is, String outputDir) throws IOException {
//		String exractedFile = outputDir + entry.getName();
//		FileOutputStream fos = null;
//
//		try {
//			fos = new FileOutputStream(exractedFile);
//			final byte[] buf = new byte[8192];
//			int length;
//
//			while ((length = is.read(buf, 0, buf.length)) >= 0) {
//				fos.write(buf, 0, length);
//			}
//
//		} catch (IOException ioex) {
//			fos.close();
//		}
//
//	}

	private static void decompressGzipFile(String gzipFile, String newFile) {
		try {
			FileInputStream fis = new FileInputStream(gzipFile);
			GZIPInputStream gis = new GZIPInputStream(fis);
			FileOutputStream fos = new FileOutputStream(newFile);
			byte[] buffer = new byte[1024];
			int len;
			while ((len = gis.read(buffer)) != -1) {
				fos.write(buffer, 0, len);
			}
			// close resources
			fos.close();
			gis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private static void compressGzipFile(String file, String gzipFile) {
		try {
			FileInputStream fis = new FileInputStream(file);
			FileOutputStream fos = new FileOutputStream(gzipFile);
			GZIPOutputStream gzipOS = new GZIPOutputStream(fos);
			byte[] buffer = new byte[1024];
			int len;
			while ((len = fis.read(buffer)) != -1) {
				gzipOS.write(buffer, 0, len);
			}
			// close resources
			gzipOS.close();
			fos.close();
			fis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private boolean checkIfResourceExists(String urlStr) {
		URL url = SrtmModel.class.getResource(urlStr);
		URL tilesObj = url;

		return (tilesObj != null);
	}

	private File loadResourceAsFile(String urlStr) {
//		System.out.println(urlStr);
		URL url = SrtmModel.class.getResource(urlStr);
//		InputStream is = SrtmModel.class.getResourceAsStream(urlStr);
//		System.out.println(url);
//		System.out.println(is);
		URL tilesObj = url;

		// System.out.println("Temp-file created.");

//		System.out.println(url);
//		System.out.println(tempFilePath + "" + urlStr + "");
		File file = new File(tempFilePath + "" + urlStr + "");

		if (tilesObj == null) {
//			System.err.println("SRTM file loading failed to start.");
			return null;
		}

//		System.out.println("SRTM file loading successfully started.");

		try {
			FileUtils.copyURLToFile(tilesObj, file);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return null;
		}

		return file;
	}

	private static void downloadFile(String url, String fileName) throws IOException {
		System.out.println("Downloading from: " + url);
		URL dataURL = new URL(url);

//		File dataDir = new File(dataFolder);
//		System.out.println("Creating Directory: " + dataFolder);
//		dataDir.mkdirs();
		InputStream is = dataURL.openStream();

//		System.out.println("Output File: " + dataFolder + fileName);
		OutputStream os = new FileOutputStream(fileName);
		byte[] buffer = new byte[16 * 1024];
		int transferredBytes = is.read(buffer);
		while (transferredBytes > -1) {
			os.write(buffer, 0, transferredBytes);
			// System.out.println("Transferred "+transferredBytes+" for "+fileName);
			transferredBytes = is.read(buffer);
		}
		is.close();
		os.close();
	}
}
