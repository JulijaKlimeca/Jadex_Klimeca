package bdi.postcar.environment.impl;

import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IInternalAccess;
import jadex.bridge.service.types.cms.CMSStatusEvent;
import jadex.bridge.service.types.cms.CMSStatusEvent.CMSTerminatedEvent;
import jadex.bridge.service.types.cms.SComponentManagementService;
import jadex.commons.future.IntermediateEmptyResultListener;
import bdi.postcar.environment.ILocationObject;

import java.lang.reflect.Array;
import java.util.*;

public class Environment {
    private static Environment instance;
    private Map<IComponentIdentifier, Car> cars;
    private List<Package> packages;
    private List<Poststation> poststations;
    private List<Charger> chargers;

    private Environment() {
        this.cars = new LinkedHashMap<IComponentIdentifier, Car>();
        this.packages = new ArrayList<Package>();
        this.poststations = new ArrayList<Poststation>();
        this.chargers = new ArrayList<Charger>();

       // pakotnes atrašanas vietas
        addPackage(new Package(new Location(0.6, 0.2)));
        addPackage(new Package(new Location(0.2, 0.5)));
        addPackage(new Package(new Location(0.4, 0.3)));
        addPackage(new Package(new Location(0.9, 0.9)));
        addPackage(new Package(new Location(0.3, 0.7)));
        addPackage(new Package(new Location(0.5, 0.6)));
        addPackage(new Package(new Location(0.7, 0.6)));
        // pasta stacijas atrašanas vietas
        addPoststation(new Poststation(new Location(0.3, 0.2), 20));
        addPoststation(new Poststation(new Location(0.5, 0.7), 20));
        // uzlades stacijas atrašanas vietas
        addCharger(new Charger(new Location(0.8, 0.7)));
        addCharger(new Charger(new Location(0.2, 0.4)));
    }

    public static synchronized Environment getInstance() {
        if (instance == null) {
            instance = new Environment();
        }
        return instance;
    }

    public Car createCar(IInternalAccess agent) {
        IComponentIdentifier cid = agent.getId();
        Car ret;
        boolean create;
        synchronized (this) {
            ret = cars.get(cid);
            create = ret == null;
            if (create) {
                ret = new Car(cid, new Location(0.1, 0.9), null, 0.1, 0.8);

                cars.put(cid, ret);
            }
        }

        if (create) {
            SComponentManagementService.listenToComponent(cid, agent)
                    .addResultListener(new IntermediateEmptyResultListener<CMSStatusEvent>() {
                        @Override
                        public void intermediateResultAvailable(CMSStatusEvent cse) {
                            if (cse instanceof CMSTerminatedEvent) {
                                synchronized (Environment.this) {
                                    cars.remove(cid);
                                }
                            }
                        }
                    });
        } else {
            throw new IllegalStateException("Car for agent " + cid + " already exists");
        }

        return ret.clone();
    }

    public synchronized void addPackage(Package myPackage) {
        packages.add(myPackage.clone());
    }

    public synchronized boolean removePackage(Package myPackage) {
        boolean ret = packages.remove(myPackage);
        return ret;
    }

    public synchronized void addPoststation(Poststation poststation) {
        poststations.add(poststation.clone());
    }

    public synchronized void addCharger(Charger station) {
        chargers.add(station.clone());
    }

    public synchronized Package[] getPackages() {
        return cloneList(packages, Package.class);
    }

    public synchronized Poststation[] getPoststations() {
        return cloneList(poststations, Poststation.class);
    }

    public synchronized Charger[] getChargers() {
        return cloneList(chargers, Charger.class);
    }

    public synchronized Car[] getCars() {
        return cloneList(cars.values(), Car.class);
    }

    public synchronized void updateCar(Car car) {
        cars.put(car.getAgentIdentifier(), car.clone());
    }

    public synchronized void pickupPackage(Car car, Package myPackage) {
        Car myCar = cars.get(car.getAgentIdentifier());

        if (myCar.getCarriedPackage() != null) {
            throw new RuntimeException("PostCar is carrying package: " + myPackage);
        }

        Package aPackage = null;
        for (Package p : packages) {
            if (p.equals(myPackage)) {
                aPackage = p;
            }
        }

        if (aPackage == null) {
            throw new RuntimeException("No such Package: " + myPackage);
        }

        if (myCar.getLocation().isNear(myPackage.getLocation())) {
            aPackage.setLocation(null);
            myCar.setCarriedPackage(aPackage);
            packages.remove(myPackage);
        } else {
            throw new RuntimeException("PostCar not in pickup range: " + myCar + ", " + aPackage);
        }
    }

    public synchronized void dropPackageInPoststation(Car car, Package aPackage, Poststation poststation) {
        Car car1 = cars.get(car.getAgentIdentifier());

        if (car1.getCarriedPackage() == null || !car1.getCarriedPackage().equals(aPackage)) {
            throw new RuntimeException("PostCar does not carry the aPackage: " + car + ", " + aPackage);
        }

        Poststation myPoststation = null;
        for (Poststation ps : poststations) {
            if (ps.equals(poststation)) {
                myPoststation = ps;
            }
        }

        if (myPoststation == null) {
            throw new RuntimeException("No such maPackage bin: " + poststation);
        }

        if (car1.getLocation().isNear(poststation.getLocation())) {
            myPoststation.addPackage(aPackage.clone());
            car1.setCarriedPackage(null);
        } else {
            throw new RuntimeException("PostCar not in drop raange: " + car1 + ", " + myPoststation);
        }
    }
    public synchronized Poststation getPoststation(String name) {
        Poststation ret = null;
        for (Poststation wb : poststations) {
            if (wb.getId().equals(name)) {
                ret = wb;
                break;
            }
        }
        return ret;
    }

    public static <T extends ILocationObject> T[] cloneList(Collection<T> list, Class<T> type) {
        List<ILocationObject> ret = new ArrayList<>();
        for (ILocationObject o : list) {
            ret.add(((LocationObject) o).clone());
        }
        @SuppressWarnings("unchecked")
        T[] aret = ret.toArray((T[]) Array.newInstance(type, list.size()));
        return aret;
    }
}
