package bdi.postcar.environment.impl;

import bdi.postcar.environment.IPackage;

public class Package extends LocationObject implements IPackage {
    private static int packageCounter;

    private static synchronized int getNumber() {
        return ++packageCounter;
    }

    public Package() {
    }
    public Package(Location location) {
        super("Package " + getNumber(), location);
    }
    public String toString() {
        return "Package(" + "id=" + getId() + ", locaption=" + getLocation() + ")";
    }

    @Override
    public Package clone() {
        return (Package) super.clone();
    }
}

