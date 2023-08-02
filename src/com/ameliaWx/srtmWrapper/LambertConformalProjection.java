package com.ameliaWx.srtmWrapper;

public class LambertConformalProjection {
	// ALL ANGLES IN DEGREES; -180 = 180W, 180 = 180E
	public double refLongitude;
	public double refLatitude;
	public static final double R = 6371.14035848;
	public double standardParallel1;
	public double standardParallel2;
	public double dx;
	public double dy;
	public double offsetX;
	public double offsetY;
	
	public static LambertConformalProjection hrrrProj = new LambertConformalProjection(-97.5, 21.138, 38.5, 38.5, 3, 3, 899.178564262, -123.970196814);

	public LambertConformalProjection(double refLongitude, double refLatitude, double standardParallel1,
			double standardParallel2, double dx, double dy, double offsetX, double offsetY) {
		this.refLongitude = refLongitude;
		this.refLatitude = refLatitude;
		this.standardParallel1 = standardParallel1;
		this.standardParallel2 = standardParallel2;
		this.dx = dx;
		this.dy = dy;
		this.offsetX = offsetX;
		this.offsetY = offsetY;
	}

	public PointD projectLatLonToIJ(double longitude, double latitude) {

		double n;
		if (standardParallel1 == standardParallel2) {
			n = sin(standardParallel1);
		} else {
			n = (Math.log(cos(standardParallel1) * sec(standardParallel2)))
					/ (Math.log(tan(0.25 * 180 + 0.5 * standardParallel2) * cot(0.25 * 180 + 0.5 * standardParallel1)));
		}
		double F = (cos(standardParallel1) * Math.pow(tan(0.25 * 180 + 0.5 * standardParallel1), n)) / n;
		double rho = R * F * Math.pow(cot(0.25 * 180 + 0.5 * latitude), n);
		double rho0 = R * F * Math.pow(cot(0.25 * 180 + 0.5 * refLatitude), n);

		//System.out.println(n);

		double x = rho * sin(n * (longitude - refLongitude));
		double y = rho0 - rho * cos(n * (longitude - refLongitude));

		//System.out.println(new PointD(x, y));
		return new PointD(x / dx + offsetX, 1059 - (y / dy + offsetY));
	}

	public PointD projectLatLonToIJ(PointD p) {
		return projectLatLonToIJ(p.getY(), p.getX());
	}

	private double sin(double theta) {
		return (Math.sin(Math.toRadians(theta)));
	}

	private double cos(double theta) {
		return (Math.cos(Math.toRadians(theta)));
	}

	private double tan(double theta) {
		return (Math.tan(Math.toRadians(theta)));
	}

	private double sec(double theta) {
		return 1 / cos(theta);
	}

	@SuppressWarnings("unused")
	private double csc(double theta) {
		return 1 / sin(theta);
	}

	private double cot(double theta) {
		return 1 / tan(theta);
	}
}
