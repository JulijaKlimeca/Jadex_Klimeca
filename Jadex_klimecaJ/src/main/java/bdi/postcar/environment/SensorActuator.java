package bdi.postcar.environment;

import bdi.postcar.environment.impl.Package;
import bdi.postcar.environment.impl.*;
import jadex.bridge.IComponentStep;
import jadex.bridge.IInternalAccess;
import jadex.bridge.component.IExecutionFeature;
import jadex.bridge.component.impl.ExecutionComponentFeature;
import jadex.bridge.service.component.IRequiredServicesFeature;
import jadex.bridge.service.search.ServiceQuery;
import jadex.bridge.service.types.clock.IClockService;
import jadex.bridge.service.types.clock.ITimedObject;
import jadex.commons.ErrorException;
import jadex.commons.SUtil;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.future.IResultListener;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

//SensorActuator klase kas parvalda agenta parliecibas
public class SensorActuator {
    //klases mainigie
    public IInternalAccess agent;
    private Car self;
    private Location target;
    private Set<ICar> cars;
    private Set<IPackage> myPackages;
    private Set<ICharger> chargers;
    private Set<IPoststation> poststations;
    private Future<Void> recharging;

    public SensorActuator() {
        this.agent = ExecutionComponentFeature.LOCAL.get();
        if (agent == null) {
            throw new IllegalStateException("Failed to find any active agent");
        }

        self = Environment.getInstance().createCar(agent);
        this.cars = new LinkedHashSet<>();
        this.myPackages = new LinkedHashSet<>();
        this.chargers = new LinkedHashSet<>();
        this.poststations = new LinkedHashSet<>();
    }

    //Agenta parstavniciba par sevi
    public ICar getSelf() {
        if (!agent.getFeature(IExecutionFeature.class).isComponentThread()) {
            throw new IllegalStateException("Error: Agent must be called in a thread");
        }

        return self;
    }

    public void managePackagesIn(Set<IPackage> packages) {
        packages.addAll(this.myPackages);
        this.myPackages = packages;
    }

    public void managePoststationsIn(Set<IPoststation> poststations) {
        poststations.addAll(this.poststations);
        this.poststations = poststations;
    }

    public void manageChargersIn(Set<ICharger> chargers) {
        chargers.addAll(this.chargers);
        this.chargers = chargers;
    }

    public void moveTo(ILocation location) {
        moveTo(location.getX(), location.getY());
    }

    //agents atbrauc iz noradito vietu pec x y koordinatiem
    public void moveTo(double x, double y) {
        if (!agent.getFeature(IExecutionFeature.class).isComponentThread()) {
            throw new IllegalStateException("Error: Agent must be called in a thread");
        }
        if (target != null) {
            throw new IllegalStateException("Cant go to the multy targets at once. Target is: " + target);
        }
        //parbauda agenta uzlades limeni
        if (self.getChargestate() <= 0) {
            if (recharging == null) {
                recharging = new Future<Void>();
            }
            agent.getLogger().warning(" Agent was called with empty battery");
            recharging.get();
        }

        this.target = new Location(x, y);

        final Future<Void> reached = new Future<>();
        //agents ir netalu no noteiktas vietas
        if (self.getLocation().isNear(target)) {
            reached.setResultIfUndone(null);
        } else {

            final IClockService clock = agent.getFeature(IRequiredServicesFeature.class).getLocalService(new ServiceQuery<>(IClockService.class));
            clock.createTickTimer(new ITimedObject() {
                long lasttime = clock.getTime();
                ITimedObject timer = this;

                @Override
                public void timeEventOccurred(long currenttime) {
                    if (!reached.isDone())
                    {

                        agent.getFeature(IExecutionFeature.class).scheduleStep(new IComponentStep<Void>() {
                            @Override
                            public IFuture<Void> execute(IInternalAccess ia) {

                                double delta = (currenttime - lasttime) / 1000.0;

                                double chargestate = self.getChargestate() - delta / 100;
                                if (chargestate < 0) {
                                    self.setChargestate(0);
                                    return new Future<>(new IllegalStateException("Run of battery!"));
                                }
                                self.setChargestate(chargestate);
                                //apekina distanciju lidz objektam
                                double total_dist = self.getLocation().getDistance(target);
                                double move_dist = Math.min(total_dist, 0.1 * delta);
                                //updejto x un y koordinates
                                double dx = (target.getX() - self.getLocation().getX()) * move_dist / total_dist;
                                double dy = (target.getY() - self.getLocation().getY()) * move_dist / total_dist;
                                //updejto agenta koordinates
                                self.setLocation(new Location(self.getLocation().getX() + dx, self.getLocation().getY() + dy));
                                //agenta atrasanas vieta vide
                                Environment.getInstance().updateCar(self);

                                update();

                                if (self.getLocation().isNear(target)) {
                                    reached.setResultIfUndone(null);
                                } else {
                                    lasttime = currenttime;
                                    clock.createTickTimer(timer);
                                }
                                return IFuture.DONE;
                            }
                        }).addResultListener(new IResultListener<Void>() {
                            @Override
                            public void exceptionOccurred(Exception exception) {
                                reached.setExceptionIfUndone(exception);
                            }

                            @Override
                            public void resultAvailable(Void result) {
                            }
                        });
                    }
                }
            });
        }

        try {
            reached.get();
        } catch (Throwable t) {
            reached.setExceptionIfUndone(t instanceof Exception ? (Exception) t : new ErrorException((Error) t));
            SUtil.throwUnchecked(t);
        } finally {
            target = null;
            //Updejto/izdzes objekta koordinated, kad agents jau atbrauca uz noteikto vietu
        }
    }

    //uzlades agentu noteikta uzlades stacija
    public synchronized void recharge(ICharger charger, double level) {
        if (!agent.getFeature(IExecutionFeature.class).isComponentThread()) {
            throw new IllegalStateException("Error: Agent must be called in a thread");
        }

        final Future<Void> reached = new Future<>();

        final IClockService clock = agent.getFeature(IRequiredServicesFeature.class).getLocalService(new ServiceQuery<>(IClockService.class));
        clock.createTickTimer(new ITimedObject() {
            long lasttime = clock.getTime();
            ITimedObject timer = this;

            @Override
            public void timeEventOccurred(long currenttime) {
                if (!reached.isDone())
                {
                    agent.getFeature(IExecutionFeature.class).scheduleStep(new IComponentStep<Void>() {
                        @Override
                        public IFuture<Void> execute(IInternalAccess ia) {
                            //exception, ja agent ir loti talu no uzlades stacijas
                            if (!self.getLocation().isNear(charger.getLocation())) {
                                throw new IllegalStateException("Cannot recharge!");
                            }

                            double delta = (currenttime - lasttime) / 1000.0;

                            double inc = delta / 10;
                            if (self.getChargestate() > 0.7)
                            {
                                inc = inc * 10 / 3.0 * (1 - self.getChargestate());
                            }
                            //palielina agenta uzlades limeni
                            self.setChargestate(self.getChargestate() + inc);

                            Future<Void> rec = recharging;
                            recharging = null;
                            if (rec != null) {
                                rec.setResult(null);
                            }
                            //updejto agenta stavokli vide
                            Environment.getInstance().updateCar(self);
                            //ja agenta limenis sasniedzis vajadzigo pakapi, tad process ir pabeigts
                            if (self.getChargestate() >= level) {
                                reached.setResultIfUndone(null);
                            } else {
                                lasttime = currenttime;
                                clock.createTickTimer(timer);
                            }
                            return null;
                        }
                    }).addResultListener(new IResultListener<Void>() {
                        @Override
                        public void exceptionOccurred(Exception exception) {
                            reached.setExceptionIfUndone(exception);
                        }

                        @Override
                        public void resultAvailable(Void result) {
                        }
                    });
                }
            }
        });

        try {
            reached.get();
        } catch (Throwable t) {
            reached.setExceptionIfUndone(t instanceof Exception ? (Exception) t : new ErrorException((Error) t));
            SUtil.throwUnchecked(t);
        }
    }

    //nakosa merka atrasanas vita

    //panemt paku, updejto pakotni vide, uzstada agentam parvadato pakotni,
    //izdzes pakotni no agenta saraksta un atjaunina pakotned atrasanas vietu = 0
    public void pickUpPackage(IPackage myPackage) {
        Environment.getInstance().pickupPackage(self, (Package) myPackage);
        self.setCarriedPackage((Package) myPackage);
        myPackages.remove(myPackage);
        ((Package) myPackage).setLocation(null);
    }

    //Atstat pakotni uz posta stacijas, atjaunina pakotnes un pasta staciju vide,
    // izdzes nesto pakotne no agenta, pievieno pakotni pasta stacijai
    public void dropPackageInPoststation(IPackage aPackage, IPoststation poststation) {

        Environment.getInstance().dropPackageInPoststation(self, (Package) aPackage, (Poststation) poststation);

        self.setCarriedPackage(null);
        ((Poststation) poststation).addPackage(aPackage);
    }

    //atjaunina agenta uzskatu uz vidi
    void update() {
        updateObjects(cars, Environment.getInstance().getCars());
        updateObjects(myPackages, Environment.getInstance().getPackages());
        updateObjects(chargers, Environment.getInstance().getChargers());
        updateObjects(poststations, Environment.getInstance().getPoststations());
    }

    //atjaunina atrasanas vietu objektiem
    <T extends ILocationObject> void updateObjects(Set<T> oldset, T[] newset) {
        Map<T, T> newmap = new LinkedHashMap<>();
        for (T o : newset) {
            if (o.equals(self)) {
                self.update((Car) o);
            }
            else {
                newmap.put(o, o);
            }
        }

        for (LocationObject oldObject : oldset.toArray(new LocationObject[oldset.size()])) {
            LocationObject newobj = (LocationObject) newmap.remove(oldObject);
            if (oldObject.getLocation().getDistance(self.getLocation()) <= self.getVisionRange()
                    && (newobj == null || newobj.getLocation().getDistance(self.getLocation()) > self.getVisionRange())) {
                oldset.remove(oldObject);
            }
            if (newobj != null && newobj.getLocation().getDistance(self.getLocation()) <= self.getVisionRange()) {
                oldObject.update(newobj);
            }
        }
        for (T newobj : newmap.values()) {
            if (newobj.getLocation().getDistance(self.getLocation()) <= self.getVisionRange()) {
                oldset.add(newobj);
            }
        }
    }
}
