package bdi.postcar.environment;


public interface ILocation
{
	double getX();
	double getY();
	double getDistance(ILocation other);
	boolean isNear(ILocation other);
}
