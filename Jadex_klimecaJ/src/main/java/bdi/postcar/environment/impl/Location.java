package bdi.postcar.environment.impl;

import bdi.postcar.environment.ILocation;

public class Location implements ILocation, Cloneable {
    public static final double DEFAULT_TOLERANCE = 0.001;
    private double x;
    private double y;

    public Location(double x, double y) {
        if (Double.isNaN(x))
            throw new IllegalArgumentException("x is not a number: " + x);
        if (Double.isNaN(y))
            throw new IllegalArgumentException("y is not a number: " + y);
        setX(x);
        setY(y);
    }

    public double getX() {
        return this.x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return this.y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public String toString() {
        return "Locationn(" + "x=" + getX() + ", y=" + getY() + ")";
    }

    public double getDistance(ILocation other) {
        assert other != null;
        return Math.sqrt((other.getY() - this.y) * (other.getY() - this.y) + (other.getX() - this.x) * (other.getX() - this.x));
    }

    public boolean isNear(ILocation other) {
        return isNear(other, DEFAULT_TOLERANCE);
    }

    public boolean isNear(ILocation other, double tolerance) {
        return getDistance(other) <= tolerance;
    }

    public boolean equals(Object o) {
        boolean ret = false;
        if (o instanceof Location) {
            Location loc = (Location) o;
            if (loc.x == x && loc.y == y)
                ret = true;
        }
        return ret;
    }

    public int hashCode() {
        return (int) (x * 21 + y);
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            assert false;
            throw new RuntimeException("Clone not supported");
        }
    }
}
