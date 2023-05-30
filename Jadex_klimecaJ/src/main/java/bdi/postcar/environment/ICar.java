package bdi.postcar.environment;

import jadex.bridge.IComponentIdentifier;

public interface ICar extends ILocationObject
{
	String getId();
	ILocation getLocation();
	double getChargestate();
	IPackage getCarriedPackage();
	double getVisionRange();
	IComponentIdentifier getAgentIdentifier();
}
