package bdi.postcar.environment.impl;

import jadex.commons.SimplePropertyChangeSupport;
import jadex.commons.beans.PropertyChangeListener;
import bdi.postcar.environment.ILocationObject;

public abstract class LocationObject implements ILocationObject, Cloneable {
    private String id;
    private Location location;
    SimplePropertyChangeSupport pcs;

    public LocationObject() {
        pcs = new SimplePropertyChangeSupport(this);
    }

    public LocationObject(String id, Location location) {
        this();
        setId(id);
        setLocation(location);
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        String oldid = this.id;
        this.id = id;
        pcs.firePropertyChange("id", oldid, id);
    }

    public Location getLocation() {
        return this.location;
    }

    public void setLocation(Location location) {
        Location oldloc = this.location;
        this.location = location;
        pcs.firePropertyChange("location", oldloc, location);
    }

    public boolean equals(Object o) {
        return o instanceof LocationObject && ((LocationObject) o).id.equals(id)
                && o.getClass().equals(this.getClass());
    }

    public int hashCode() {
        return 31 + id.hashCode();
    }

    public void update(LocationObject obj) {
        assert this.getId().equals(obj.getId());
        setLocation(obj.getLocation());
    }

    public LocationObject clone() {
        try {
            LocationObject clone = (LocationObject) super.clone();
            if (getLocation() != null)
                clone.setLocation((Location) getLocation().clone());
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Clone not supported");
        }
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }
    protected SimplePropertyChangeSupport getPropertyChangeHandler() {
        return pcs;
    }
}
