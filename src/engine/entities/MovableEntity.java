package engine.entities;

public interface MovableEntity extends Entity
{

    public void setDX(double dx);

    public void setDY(double dy);

    public double getDX();

    public double getDY();
}
