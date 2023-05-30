package bdi.postcar.environment.impl;

import bdi.postcar.environment.ICharger;

public class Charger extends LocationObject implements ICharger {
    private static int instanceCounter = 0;

    private static synchronized int getNumber() {
        return ++instanceCounter;
    }

    public Charger(Location location) {
        super("ICharger " + getNumber(), location);
    }

    public String toString() {
        return "Charger(" + "id=" + getId() + ", location=" + getLocation() + ")";
    }

    @Override
    public Charger clone() {
        return (Charger) super.clone();
    }
}
