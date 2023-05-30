package bdi.postcar.environment.impl;

import bdi.postcar.environment.IPackage;
import bdi.postcar.environment.IPoststation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Poststation extends LocationObject implements IPoststation {
    private static int instanceCounter = 0;

    private static synchronized int getNumber() {
        return ++instanceCounter;
    }

    private List<IPackage> packages;
    private int capacity;

    public Poststation() {
        this.packages = new ArrayList<IPackage>();
    }

    public Poststation(Location location, int capacity) {
        super("Poststation " + getNumber(), location);
        this.packages = new ArrayList<IPackage>();
        setCapacity(capacity);
    }

    public Package[] getPackages() {
        return (Package[]) packages.toArray(new Package[packages.size()]);
    }

    public void setPackages(Package[] packages) {
        this.packages.clear();
        Collections.addAll(this.packages, packages);
        getPropertyChangeHandler().firePropertyChange("packages", null, packages);
    }

    public Package getPackage(int idx) {
        return (Package) this.packages.get(idx);
    }

    public void setPackage(int idx, Package myPackage) {
        this.packages.set(idx, myPackage);
        getPropertyChangeHandler().firePropertyChange("packages", null, packages);
    }

    public void addPackage(IPackage myPackage) {
        this.packages.add(myPackage);
        getPropertyChangeHandler().firePropertyChange("packages", null, packages);
    }

    public boolean removePackage(Package myPackage) {
        boolean ret = this.packages.remove(myPackage);
        if (ret)
            getPropertyChangeHandler().firePropertyChange("packages", null, packages);
        return ret;
    }

    public int getCapacity() {
        return this.capacity;
    }

    public void setCapacity(int capacity) {
        int oldc = this.capacity;
        this.capacity = capacity;
        getPropertyChangeHandler().firePropertyChange("capacity", oldc, capacity);
    }

    public String toString() {
        return "Poststation(" + "id=" + getId() + ", location=" + getLocation() + ")";
    }

    public boolean isFull() {
        return packages.size() >= capacity;
    }

    public void empty() {
        packages.clear();
    }

    public void fill() {
        while (!isFull())
            packages.add(new Package(new Location(-1, -1)));
    }

    public boolean contains(IPackage myPackage) {
        return packages.contains(myPackage);
    }

    public Poststation clone() {
        Poststation clone = (Poststation) super.clone();
        clone.packages = new ArrayList<IPackage>();
        for (int i = 0; i < packages.size(); i++)
            clone.packages.add((IPackage) ((Package) packages.get(i)).clone());
        return clone;
    }
}
