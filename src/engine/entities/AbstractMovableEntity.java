package engine.entities;

public abstract class AbstractMovableEntity extends AbstractEntity implements MovableEntity
{

    protected double dx, dy;

    public AbstractMovableEntity(double x, double y, double width, double height)
    {
        super(x, y, width, height);
        this.dx = 0;
        this.dy = 0;
    }

    @Override
    public void update(int delta)
    {
        x += delta * dx;
        y += delta * dy;
    }

    public void setDX(double dx)
    {
        this.dx = dx;
    }

    public void setDY(double dy)
    {
        this.dy = dy;
    }

    public double getDX()
    {
        return dx;
    }

    public double getDY()
    {
        return dy;
    }

}
