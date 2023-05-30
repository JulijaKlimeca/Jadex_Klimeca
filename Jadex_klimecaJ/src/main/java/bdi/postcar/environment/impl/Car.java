package bdi.postcar.environment.impl;

import bdi.postcar.environment.ICar;
import jadex.bridge.IComponentIdentifier;
import jadex.commons.SUtil;

public class Car extends LocationObject implements ICar {
    private IComponentIdentifier cid;
    private double chargestate;
    private Package carriedPackage;
    private double visionrange;

    public Car(IComponentIdentifier cid, Location location, Package carriedPackage, double vision, double chargestate) {
        super(cid.getName(), location);
        this.cid = cid;
        setCarriedPackage(carriedPackage);
        setVisionRange(vision);
        setChargestate(chargestate);
    }

    public IComponentIdentifier getAgentIdentifier() {
        return cid;
    }

    public double getChargestate() {
        return this.chargestate;
    }

    public void setChargestate(double chargestate) {
        double oldcs = this.chargestate;
        this.chargestate = chargestate;
        pcs.firePropertyChange("chargestate", oldcs, chargestate);
    }

    public Package getCarriedPackage() {
        return this.carriedPackage;
    }

    public void setCarriedPackage(Package carriedPackage) {
        Package oldcw = this.carriedPackage;
        this.carriedPackage = carriedPackage;
        pcs.firePropertyChange("carriedPackage", oldcw, carriedPackage);
    }

    public double getVisionRange() {
        return this.visionrange;
    }

    public void setVisionRange(double visionrange) {
        double oldvr = this.visionrange;
        this.visionrange = visionrange;
        pcs.firePropertyChange("visionRange", oldvr, visionrange);
    }

    public void update(Car cl) {
        assert this.getId().equals(cl.getId());
        super.update(cl);

        setChargestate(cl.getChargestate());
        setVisionRange(cl.getVisionRange());

        if (SUtil.equals(getCarriedPackage(), cl.getCarriedPackage()))
            setCarriedPackage(cl.getCarriedPackage());
    }

    public String toString() {
        return "Car(" + "id=" + getId() + ", location=" + getLocation() + ")";
    }

    public Car clone() {
        Car clone = (Car) super.clone();
        if (getCarriedPackage() != null)
            clone.setCarriedPackage((Package) getCarriedPackage().clone());
        return clone;
    }
}
