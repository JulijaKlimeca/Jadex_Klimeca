package bdi.postcar.environment;

public interface IPoststation extends ILocationObject
{
	 String getId();
	 ILocation getLocation();
	 IPackage[] getPackages();
	 IPackage getPackage(int idx);
	 int getCapacity();
	 boolean isFull();
	 boolean contains(IPackage myPackage);
}
