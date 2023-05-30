package bdi.postcar;

import bdi.postcar.gui.EnvironmentGui;
import jadex.base.IPlatformConfiguration;
import jadex.base.PlatformConfigurationHandler;
import jadex.base.Starter;
import jadex.bridge.IExternalAccess;
import jadex.bridge.service.search.ServiceQuery;
import jadex.bridge.service.types.clock.IClock;
import jadex.bridge.service.types.clock.IClockService;
import jadex.bridge.service.types.threadpool.IThreadPoolService;
import jadex.commons.future.IFuture;

import javax.swing.*;
import java.util.logging.Level;

public class Main {
    protected static double CLOCK_SPEED = 3;
    public static void main(String[] args) {

        IPlatformConfiguration conf = PlatformConfigurationHandler.getMinimal();

        conf.setLoggingLevel(Level.WARNING);

        // ir nepieciesamas lai palaist BDI agentus
        conf.setValue("kernel_bdi", true);

        // cik daudz agentu ir pievienoti
        conf.addComponent("bdi/postcar/CarBDIAgent.class");

        // Asinhroni palaizam Jadex platformu
        IFuture<IExternalAccess> fut = Starter.createPlatform(conf);

        //funkcijas rezultatu iegusana
        fut.get();

        // Izveleta atruma pielietojums
        IClockService cs = fut.get().searchService(new ServiceQuery<>(IClockService.class)).get();
        cs.setClock(IClock.TYPE_CONTINUOUS, fut.get().searchService(new ServiceQuery<>(IThreadPoolService.class)).get());
        cs.setDilation(CLOCK_SPEED);

        //Agentu vides logu atversana
        SwingUtilities.invokeLater(() -> new EnvironmentGui().setVisible(true));
    }
}
