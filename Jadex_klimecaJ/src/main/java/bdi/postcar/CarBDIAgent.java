package bdi.postcar;

import bdi.postcar.environment.*;
import jadex.bdiv3.annotation.*;
import jadex.bdiv3.features.IBDIAgentFeature;
import jadex.bdiv3.runtime.IGoal;
import jadex.bdiv3.runtime.IPlan;
import jadex.bridge.service.annotation.OnStart;
import jadex.micro.annotation.Agent;

import java.util.LinkedHashSet;
import java.util.Set;


@Agent(type = "bdi")
public class CarBDIAgent {
    //sensors, kas palidz agentam uztvert apkartejo vidi
    private SensorActuator actsense = new SensorActuator();

    //agenta, parliecibas par sevi, savu atrasanas vietu un uzledetaja limeni
    @Belief
    private ICar self = actsense.getSelf();

    //Agenta sensora parvaldito uzlades staciju saraksts
    @Belief
    private Set<ICharger> stations = new LinkedHashSet<>();

    //Agenta sensora parvaldito pasta saraksts
    @Belief
    private Set<IPoststation> poststations = new LinkedHashSet<>();

    //Agenta sensora parvaldito pakotnes saraksts
    @Belief
    private Set<IPackage> packages = new LinkedHashSet<>();

    @OnStart
    private void exampleBehavior(IBDIAgentFeature bdi) {
        //ar so agenta sensori updejto agenta patliecibas, par pasta nodalam, pakotnem un uzlades stacijam
        actsense.manageChargersIn(stations);
        actsense.managePackagesIn(packages);
        actsense.managePoststationsIn(poststations);


        //Merka izveidosana
        bdi.dispatchTopLevelGoal(new PerformPatrol());
        bdi.dispatchTopLevelGoal(new MaintainBatteryLoaded());
    }

    //Merki:

    @Goal(recur = true, orsuccess = false, recurdelay = 3000)
    class PerformPatrol {
    }
    // Merkis lai uzladetos, kas limenis ir mazaks par 20%
    @Goal(recur = true, recurdelay = 3000,
            deliberation = @Deliberation(inhibits = {PerformPatrol.class, AchievePickupPackage.class}))
    class MaintainBatteryLoaded {
        @GoalMaintainCondition

        boolean isBatteryLoaded() {
            return self.getChargestate() >= 0.2;
        }
        @GoalTargetCondition
        boolean isBatteryFullyLoaded() {
            return self.getChargestate() >= 0.99;
        }
    }

    //Uzlades stacijas atrasanas vietas meklesana
    @Goal(excludemode = ExcludeMode.Never)
    class QueryCharger {

        ICharger station;

        @GoalTargetCondition
        boolean isStationKnown() {
            station = stations.isEmpty() ? null : stations.iterator().next();
            return station != null;
        }
    }

    //Pasta atrasanas vietas meklesana
    @Goal(excludemode = ExcludeMode.Never)
    class QueryPoststation {
        IPoststation poststation;

        @GoalTargetCondition
        boolean isPoststationKnown() {
            poststation = poststations.isEmpty() ? null : poststations.iterator().next();
            return poststation != null;
        }
    }

    //Merkis lai sanemt konkretu pakotni
    @Goal(recur = true, recurdelay = 3000,
            deliberation = @Deliberation(inhibits = PerformPatrol.class, cardinalityone = true))
    class AchievePickupPackage {

        IPackage myPackage;
        @GoalCreationCondition(factadded = "packages")
        public AchievePickupPackage(IPackage myPackage) {
            System.out.println("Clear agent's achieved goal: " + myPackage);
            this.myPackage = myPackage;
        }
        @GoalTargetCondition
        boolean isClean() {

            return !packages.contains(myPackage)

                    && !myPackage.equals(self.getCarriedPackage());
        }
        @GoalContextCondition
        boolean isPossible() {
            return self.getCarriedPackage() == null || self.getCarriedPackage().equals(myPackage);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "(" + myPackage + ")";
        }
    }
    // plans pasta un uzlades staciju atrasanai
    @Plan(trigger = @Trigger(goals = PerformPatrol.class))
    private void performPatrolPlan() {
        System.out.println("Start Move Plan");
        actsense.moveTo(0.9, 0.9);
        actsense.moveTo(0.9, 0.7);
        actsense.moveTo(0.1, 0.7);
        actsense.moveTo(0.1, 0.5);
        actsense.moveTo(0.9, 0.5);
        actsense.moveTo(0.9, 0.3);
        actsense.moveTo(0.1, 0.3);
    }

    //Plans lai atbrauktu uz ulades staciju un attiecigi uzladeties
    @Plan(trigger = @Trigger(goals = MaintainBatteryLoaded.class))
    private void loadBattery(IPlan plan) {
        System.out.println("GO and LOAD Battery ");

        // apaksmerkis lai atrastu uzlades staciju
        QueryCharger querygoal = new QueryCharger();
        plan.dispatchSubgoal(querygoal).get();
        ICharger charger = querygoal.station;
        // Atbraukt uz uzlades staciju
        actsense.moveTo(charger.getLocation());
        // Uzladeties
        actsense.recharge(charger, 1.0);
    }

    //Plans lai nejausi parvietoties vide
    @Plan(trigger = @Trigger(goals = {QueryCharger.class, QueryPoststation.class}))
    private void moveAround(IPlan plan) {
        System.out.println("Move around " + plan.getReason());
        actsense.moveTo(Math.random(), Math.random());
    }

    //plans lai paziņot par iegutiem merkiem
    @Plan(trigger = @Trigger(goalfinisheds = AchievePickupPackage.class))
    private void finishedCleanup(IGoal goal, AchievePickupPackage cleanup) {
        String state = goal.getProcessingState().toString();
        System.out.println("Finished goal and goal status : " + state + " for " + cleanup.myPackage);
    }

    //Plans lai panemt pakotni un atnest uz pastu
    @Plan(trigger = @Trigger(goals = AchievePickupPackage.class))
    private void cleanupPackage(IPlan plan, AchievePickupPackage cleanup) {
        System.out.println("Clean up package ");

        // Atbraukt un panemt paku
        if (!cleanup.myPackage.equals(self.getCarriedPackage())) {
            actsense.moveTo(cleanup.myPackage.getLocation());
            actsense.pickUpPackage(cleanup.myPackage);
        }
        // Nosutit apaksmerki, lai atrastu pastu
        QueryPoststation querygoal = new QueryPoststation();
        plan.dispatchSubgoal(querygoal).get();
        IPoststation poststation = querygoal.poststation;

        // Atbraukt pie pasta stacijas
        actsense.moveTo(poststation.getLocation());

        // Piegadat paku uz staciju un atstat to pastas nodala
        actsense.dropPackageInPoststation(cleanup.myPackage, poststation);
    }
}
