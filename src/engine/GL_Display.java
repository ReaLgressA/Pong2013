package engine;

import static org.lwjgl.opengl.GL11.*;

import org.lwjgl.*;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

public class GL_Display
{
    public static int WIDTH, HEIGHT;
    public static String TITLE;
    static final boolean VSync = true;// is VSync Enabled 
    int fps;// frames per second
    long lastFrame, lastFPS;//time at last frame, last fps time

    public void start(int width, int height, String title, boolean fullscreen)
    {
        WIDTH = width;
        HEIGHT = height;
        TITLE = title;
        try
        {
            Display.setDisplayMode(new DisplayMode(WIDTH, HEIGHT));
            Display.setTitle(TITLE);
            Display.create();
        }
        catch (LWJGLException e)
        {
            e.printStackTrace();
            System.exit(0);
        }

        initGL(); // initialize OpenGL

        getDelta(); // call once before loop to initialize lastFrame
        lastFPS = getTime(); // call before loop to initialize fps timer
        if (fullscreen)
        {
            setDisplayMode(WIDTH, HEIGHT, fullscreen);
        }

    }

    /**
     * Set the display mode to be used
     *
     * @param width The width of the display required
     * @param height The height of the display required
     * @param fullscreen True if we want full screen mode
     */
    public void setDisplayMode(int width, int height, boolean fullscreen)
    {
        // return if requested DisplayMode is already set
        if ((Display.getDisplayMode().getWidth() == width)
                && (Display.getDisplayMode().getHeight() == height)
                && (Display.isFullscreen() == fullscreen))
        {
            return;
        }
        try
        {
            DisplayMode targetDisplayMode = null;
            if (fullscreen)
            {
                DisplayMode[] modes = Display.getAvailableDisplayModes();
                int freq = 0;

                for (int i = 0; i < modes.length; i++)
                {
                    DisplayMode current = modes[i];

                    if ((current.getWidth() == width) && (current.getHeight() == height))
                    {
                        if ((targetDisplayMode == null) || (current.getFrequency() >= freq))
                        {
                            if ((targetDisplayMode == null) || (current.getBitsPerPixel() > targetDisplayMode.getBitsPerPixel()))
                            {
                                targetDisplayMode = current;
                                freq = targetDisplayMode.getFrequency();
                            }
                        }
                        // if we've found a match for bpp and frequence against the 
                        // original display mode then it's probably best to go for this one
                        // since it's most likely compatible with the monitor
                        if ((current.getBitsPerPixel() == Display.getDesktopDisplayMode().getBitsPerPixel())
                                && (current.getFrequency() == Display.getDesktopDisplayMode().getFrequency()))
                        {
                            targetDisplayMode = current;
                            break;
                        }
                    }
                }
            }
            else
            {
                targetDisplayMode = new DisplayMode(width, height);
            }
            if (targetDisplayMode == null)
            {
                System.out.println("Failed to find value mode: " + width + "x" + height + " fs=" + fullscreen);
                return;
            }
            Display.setDisplayMode(targetDisplayMode);
            Display.setFullscreen(fullscreen);
        }
        catch (LWJGLException e)
        {
            System.out.println("Unable to setup mode " + width + "x" + height + " fullscreen=" + fullscreen + e);
        }
    }

    /**
     * Calculate how many milliseconds have passed since last frame.
     *
     * @return milliseconds passed since last frame
     */
    public int getDelta()
    {
        long time = getTime();
        int delta = (int) (time - lastFrame);
        lastFrame = time;
        return delta;
    }

    /**
     * Get the accurate system time
     *
     * @return The system time in milliseconds
     */
    public long getTime()
    {
        return (Sys.getTime() * 1000) / Sys.getTimerResolution();
    }

    public int AQupdateFPS()
    {
        if (getTime() - lastFPS > 1000)
        {
            int FPS = fps;
            fps = 0;
            lastFPS += 1000;
            fps++;
            return FPS;
        }
        ++fps;
        return 0;
    }

    public void initGL()
    {
        glShadeModel(GL_SMOOTH);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0, 800, 600, 0, 1, -1);
        glMatrixMode(GL_MODELVIEW);
        Display.setVSyncEnabled(VSync);//Setup VSync
    }
}
