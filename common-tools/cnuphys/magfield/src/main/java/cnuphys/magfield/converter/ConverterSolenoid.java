package cnuphys.magfield.converter;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;

import cnuphys.magfield.FloatVect;

public class ConverterSolenoid {


	private static int PHI = 0;
	private static int RHO = 1;
	private static int Z = 2;

	private static final double TINY = 1.0e-12;

	static boolean firstLine = true;
	static int lineCount;

	private static String gemcName[] = { "\"azimuthal\"", "\"transverse\"", "\"longitudinal\"" };
	private static String ordinal[] = { "first", "second", "third" };
	private static String units[] = { "\"deg\"", "\"cm\"", "\"cm\"" };

	private static String _homeDir = System.getProperty("user.home");

	private static ArrayList<File> dataFiles(String dataDir) {

		ArrayList<File> files = new ArrayList<File>();

		File dir = new File(dataDir);
		if (dir.isDirectory()) {

			FileFilter filter = new FileFilter() {

				@Override
				public boolean accept(File file) {
					String name = file.getName();
					return name.contains("polarpatch");
				}

			};

			File[] fileArray = dir.listFiles(filter);

			if ((fileArray != null) && (fileArray.length > 0)) {
				System.out.println("has " + fileArray.length + " filtered files");

				for (File f : fileArray) {
					files.add(f);
				}
			}
		}

		return files;
	}


	// get the dir with all the tables
	private static String getDataDir() {
		return _homeDir + "/newMapsSolenoid";
	}

	private static GridData[] preProcessor(ArrayList<ZFile> zfiles) throws IOException {

		if (!zfiles.isEmpty()) {
			System.out.println("Found " + zfiles.size() + " files.");

			GridData gdata[] = new GridData[3];
			for (int i = 0; i < 3; i++) {
				gdata[i] = (new ConverterSolenoid()).new GridData(i);
			}

			gdata[PHI].min = 0;
			gdata[PHI].max = 360;
			gdata[Z].n = zfiles.size();

			int zIndex = 0;
			for (ZFile zfile : zfiles) {
				
				File file = zfile.file;

	//			System.out.println(" PRE-PROCESSING FILE [" + file.getName() + "] zindex =  " + zIndex + "  ");
				firstLine = true;
				lineCount = 0;
				try {

					AsciiReader ar = new AsciiReader(file, zIndex) {

						@Override
						protected void processLine(String line) {
							String tokens[] = AsciiReadSupport.tokens(line);

							if (firstLine) {
								int nRho = Integer.parseInt(tokens[0]);
								int nPhi = Integer.parseInt(tokens[1]);

								if (gdata[PHI].n == 0) {
									gdata[PHI].n = nPhi;
									gdata[RHO].n = nRho;
								} else {
									if ((gdata[PHI].n != nPhi) || (gdata[RHO].n != nRho)) {
										System.err.println("Grid Inconsistency");
										System.exit(1);
									}
								}

								firstLine = false;
							} // firstLine

							if ((tokens != null) && (tokens.length == 7)) {

								double newX = Double.parseDouble(tokens[0]) / 10;
								double newY = Double.parseDouble(tokens[1]) / 10;
								double newZ = Float.parseFloat(tokens[2]) / 10;
								double newRho = Math.hypot(newX, newY);


								gdata[RHO].min = Math.min(gdata[RHO].min, newRho);
								gdata[RHO].max = Math.max(gdata[RHO].max, newRho);
								gdata[Z].min = Math.min(gdata[Z].min, newZ);
								gdata[Z].max = Math.max(gdata[Z].max, newZ);

								lineCount++;
							}

						} // processLine

						@Override
						public void done() {
//							System.out.println(" processed " + lineCount + " lines");
						}

					};
				} catch (FileNotFoundException e) {
					e.printStackTrace();
					System.exit(1);
				}

				zIndex++;
			}

			return gdata;
		}

		return null;
	}

	private static boolean zeroAngle(double angDeg) {
		if ((Math.abs(angDeg) < TINY) || (Math.abs(angDeg - 360.) < TINY)) {
			return true;
		}

		return false;
	}

	// process all the files
	private static void processAllFiles(ArrayList<ZFile> zfiles, GridData gdata[]) throws IOException {
		if (!zfiles.isEmpty()) {

			File bfile = new File(getDataDir(), "solenoid.dat");
			DataOutputStream dos = new DataOutputStream(new FileOutputStream(bfile));

			int nPhi = gdata[PHI].n;
			int nRho = gdata[RHO].n;
			int nZ = gdata[Z].n;

			float phimin = (float) gdata[PHI].min;
			float phimax = (float) gdata[PHI].max;
			float rhomin = (float) gdata[RHO].min;
			float rhomax = (float) gdata[RHO].max;
			float zmin = (float) gdata[Z].min;
			float zmax = (float) gdata[Z].max;

			dos.writeInt(0xced);
			dos.writeInt(0);
			dos.writeInt(0);
			dos.writeInt(0);
			dos.writeInt(0);
			dos.writeInt(0);
			dos.writeFloat(phimin);
			dos.writeFloat(phimax);
			dos.writeInt(nPhi);
			dos.writeFloat(rhomin);
			dos.writeFloat(rhomax);
			dos.writeInt(nRho);
			dos.writeFloat(zmin);
			dos.writeFloat(zmax);
			dos.writeInt(nZ);

			long unixTime = System.currentTimeMillis();

			int high = (int) (unixTime >> 32);
			int low = (int) unixTime;

			// write reserved
			dos.writeInt(high); // first word of unix time
			dos.writeInt(low); // second word of unix time
			dos.writeInt(0);
			dos.writeInt(0);
			dos.writeInt(0);

			int size = 3 * 4 * nPhi * nRho * nZ;
			System.err.println("FILE SIZE = " + size + " bytes");

			int zIndex = 0;

			FloatVect[][] bvals = new FloatVect[nRho][nZ];

			for (ZFile zfile : zfiles) {
				
				File file = zfile.file;

				System.out.print(" PROCESSING FILE [" + file.getName() + "] zindex =  " + zIndex + "  ");

				try {

					AsciiReader ar = new AsciiReader(file, zIndex) {
						int rhoIndex = 0;

						@Override
						protected void processLine(String line) {
							String tokens[] = AsciiReadSupport.tokens(line);
							if ((tokens != null) && (tokens.length == 7)) {


								// note t to kG
								float Bx = Float.parseFloat(tokens[3]) * 10;
								float By = Float.parseFloat(tokens[4]) * 10;
								float Bz = Float.parseFloat(tokens[5]) * 10;
								float Brho = Bx;
								float Bphi = By;
								
								if (Math.abs(Bphi) > 0.1) {
									System.err.println("Non zero Bphi.");
									System.exit(1);
								}
								
								try {
									bvals[rhoIndex][iVal] = new FloatVect(Bphi, Brho, Bz);
								} catch (ArrayIndexOutOfBoundsException e) {
									e.printStackTrace();
									System.err.println(
											"RHOINDX: " + rhoIndex + "  ZINDX:  " + iVal);

									double x = Double.parseDouble(tokens[0]) / 10;
									double y = Double.parseDouble(tokens[1]);
									double z = Double.parseDouble(tokens[2]);
									double phi = Math.toDegrees(Math.atan2(y, x));
									double rho = Math.hypot(x, y);
									System.err.println("PHI: " + phi + "   rho: " + rho + "   z: " + z);

									System.exit(1);
								}

								rhoIndex++;
							}
						}

						@Override
						public void done() {
							System.out.println(" processed " + rhoIndex + " lines");
						}

					};
				} catch (FileNotFoundException e) {
					e.printStackTrace();
					System.exit(1);
				}

				zIndex++;

			} // end for loop

				for (int iRHO = 0; iRHO < nRho; iRHO++) {
					System.out.println("iRHO = " + iRHO);
					for (int iZ = 0; iZ < nZ; iZ++) {
						FloatVect fcv = bvals[iRHO][iZ];
						dos.writeFloat(fcv.x);
						dos.writeFloat(fcv.y);
						dos.writeFloat(fcv.z);
					}
				}

			dos.flush();
			dos.close();

			System.out.println("Binary file: " + bfile.getCanonicalPath());

		}

	}

	//convert ot binary
	public static void convertToBinary(ArrayList<ZFile> zfiles, GridData gdata[]) {
		if (gdata != null) {
			try {
				processAllFiles(zfiles, gdata);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void convertToGemc(ArrayList<File> files, GridData gdata[]) {
		if (gdata != null) {
			File afile = new File(getDataDir(), "gemc_torus.txt");

			boolean assumedSymmetric = gdata[PHI].max < 100;
			System.out.println("In GEMC Converter assumed symmetric: " + assumedSymmetric);

			if (afile.exists()) {
				afile.delete();
			}

			try {
				PrintWriter writer = new PrintWriter(new FileOutputStream(afile));

				// write the header

				int indentLevel = 0;
				writeln(writer, indentLevel, false, "<mfield>");
				indentLevel++;

				writeln(writer, indentLevel, true, "description name=\"" + afile.getName()
						+ "\" factory=\"ASCII\" comment=\"clas12 superconducting torus\"");

				writeln(writer, indentLevel, true,
						"symmetry type=\"" + (assumedSymmetric ? "phi-segmented\"" : "none\"")
								+ " format=\"map\" integration=\"ClassicalRK4\" minStep=\"1*mm\"");

				writeln(writer, indentLevel, false, "<map>");
				indentLevel++;

				writeln(writer, indentLevel, false, "<coordinate>");
				indentLevel++;

				for (GridData gd : gdata) {
					writeln(writer, indentLevel, true, gd.forGEMC());
				}

				indentLevel--;
				writeln(writer, indentLevel, false, "</coordinate>");

				writeln(writer, indentLevel, true, "field unit=\"kilogauss\"");
				writeln(writer, indentLevel, true, "interpolation type=\"none\"");

				indentLevel--;
				writeln(writer, indentLevel, false, "</map>");

				indentLevel--;
				writeln(writer, indentLevel, false, "</mfield>");

				// OK now the data

				int nPhi = gdata[PHI].n;
				int nRho = gdata[RHO].n;
				int nZ = gdata[Z].n;

				int zIndex = 0;
				FloatVect[][][] bvals = new FloatVect[nPhi][nRho][nZ];

				for (File file : files) {

					System.out.print(" PROCESSING FILE [" + file.getName() + "] zindex =  " + zIndex + "  ");

					try {

						AsciiReader ar = new AsciiReader(file, zIndex) {
							int count = 0;

							@Override
							protected void processLine(String line) {
								String tokens[] = AsciiReadSupport.tokens(line);
								if ((tokens != null) && (tokens.length == 7)) {

									int rhoIndex = count % nRho;
									int phiIndex = count / nRho;

									// note T to kG
									float Bx = Float.parseFloat(tokens[3]) * 10;
									float By = Float.parseFloat(tokens[4]) * 10;
									float Bz = Float.parseFloat(tokens[5]) * 10;

									bvals[phiIndex][rhoIndex][iVal] = new FloatVect(Bx, By, Bz);

									count++;
								}
							}

							@Override
							public void done() {
								System.out.println(" processed " + count + " lines");
							}

						};
					} catch (FileNotFoundException e) {
						e.printStackTrace();
						System.exit(1);
					}

					zIndex++;

				} // end for loop

				// now write them back out
				for (int iPhi = 0; iPhi < nPhi; iPhi++) {
					double phi = gdata[PHI].min + iPhi * gdata[PHI].del();
					for (int iRho = 0; iRho < nRho; iRho++) {
						double rho = gdata[RHO].min + iRho * gdata[RHO].del();
						for (int iZ = 0; iZ < nZ; iZ++) {
							double z = gdata[Z].min + iZ * gdata[Z].del();
							FloatVect fv = bvals[iPhi][iRho][iZ];

							writeln(writer, 0, false, String.format("%-5.1f %-5.1f %-5.1f %s %s %s", phi, rho, z,
									bstr(fv.x), bstr(fv.y), bstr(fv.z)));

							// if (iZ == 200) {
							// writer.flush();
							// writer.close();
							// return;
							// }
						}

					}

				}

				writer.flush();
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static String bstr(float b) {
		if (Math.abs(b) < 1.0e-3) {
			return "0";
		} else {
			return String.format("% 11.4E", b);
		}
	}
	
	private static double getZ(File file) {
		ZFile zf = new ZFile(file);
		return zf.z;
	}


	private static String spaces = "    ";

	private static void writeln(PrintWriter writer, int indentLevel, boolean brackets, String line) {
		for (int i = 0; i < indentLevel; i++) {
			writer.print(spaces);
		}
		if (brackets) {
			writer.print("<");
			writer.print(line);
			writer.println("/>");
		} else {
			writer.println(line);
		}
	}

	private static ArrayList<ZFile> zOrderFiles(ArrayList<File> files) {
		
		ArrayList<ZFile> zfiles  = new ArrayList<>(files.size());
		for (File file : files) {
			ZFile zfile = new ZFile(file);
			zfiles.add(zfile);
		}
		
		Collections.sort(zfiles);
		
//		for (ZFile zf : zfiles) {
//			System.out.println("z = " + zf.z);
//		}
		
		return zfiles;
	}
	
	private static void findPhiDiff(ArrayList<ZFile> zfiles, double rho, double z) {
		for (ZFile zfile : zfiles) {
			if (Math.abs(zfile.z - z) < 0.01) {
				System.out.println("Found the file with z = " + zfile.z);
				
				
				AsciiReader ar;
				try {
					ar = new AsciiReader(zfile.file) {

						@Override
						protected void processLine(String line) {
							String tokens[] = AsciiReadSupport.tokens(line);
							if (tokens.length == 7) {
								double x = Double.parseDouble(tokens[0]);
								double y = Double.parseDouble(tokens[1]);
								double trho = Math.hypot(x, y);
								if (Math.abs(rho - trho) < 0.01) {
									double phi = Math.atan2(y, x);
									double cphi = Math.cos(phi);
									double sphi = Math.sin(phi);
									phi = Math.toDegrees(phi);
									double Bx = Double.parseDouble(tokens[3]);
									double By = Double.parseDouble(tokens[4]);
									double Bz = Double.parseDouble(tokens[5]);
									double bMag = Double.parseDouble(tokens[6]);
									double Brho = Bx*cphi + By*sphi;
									double Bphi = -Bx*sphi + By*cphi;
									
									if (Math.abs(Bphi) < 1.0e-10) Bphi = 0;
									
									String s = String.format("phi = %7.2f  (Bphi, Brho, Bz) = (%-8.4f, %-8.4f, %-8.4f)  Bmag = %-8.4f", phi, Bphi, Brho, Bz, bMag);
									System.out.println(s);
								}
							}
						}

						@Override
						public void done() {
						}
						
					};
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}

				return;
			}
		}
	}
	
	// MAIN PROGRAM
	public static void main(String arg[]) {
		String dataDir = getDataDir();

		ArrayList<File> files = dataFiles(dataDir);
		System.out.println("data dir = [" + dataDir + "]  file count: " + files.size());
		
		//get z ordering
		ArrayList<ZFile> zfiles = zOrderFiles(files);
		System.out.println("Z ordered Files");

		//USE THE ZFILE LIST!!!!!!
		//TODO implement from here using the zfile list
		
		// preprocess to get the grid data
		GridData gdata[] = null;
		try {
			gdata = preProcessor(zfiles);

			System.out.println("PHI: " + gdata[PHI]);
			System.out.println("RHO: " + gdata[RHO]);
			System.out.println("Z: " + gdata[Z]);

		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Preprocessed Files");

			


		convertToBinary(zfiles, gdata);
		// convertToGemc(files, gdata);

		System.out.println("done");
	}

	class GridData {
		public int index = -1;
		public int n = 0;
		public double min = Double.POSITIVE_INFINITY;
		public double max = Double.NEGATIVE_INFINITY;

		public GridData(int index) {
			this.index = index;
		}

		public String forGEMC() {
			return String.format("%-6s name=%-14s npoints=\"%d\" min=\"%d\" max=\"%d\" units=%s", ordinal[index],
					gemcName[index], n, (int) min, (int) max, units[index]);
		}

		@Override
		public String toString() {
			return String.format("N = %d   min = %12.5f  max = %12.5f", n, min, max);
		}

		public double del() {
			return (max - min) / (n - 1);
		}

	}

}
