package main;

import static org.lwjgl.opengl.GL11.*;

import java.awt.Font;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.*;

import org.newdawn.slick.SlickException;
import org.newdawn.slick.*;
import org.newdawn.slick.font.effects.ColorEffect;

import engine.*;
import engine.entities.*;
import engine.network.*;

enum GameState
{

    MainMenu, Single, Multi, Net, Connecting;
}

public class MainPong
{

    private static final int WIDTH = 800, HEIGHT = 600, FPS = 60;
    private static final double GAME_BASE_SPEED = 0.25; // base game speed
    private static final int SPLASH_COOLDOWN = 2000;

    private static GL_Display glDisplay;
    private static GameState gState = GameState.MainMenu;
    private static int fps = FPS;//fps -> current, FPS -> Maximal
    private static UnicodeFont fontFPS, fontScore, fontSplash, fontConnect;

    private TCP_Server Server;
    private TCP_Client Client;

    private Timer splashTimer = new Timer();
    private Vector<MenuButton> MenuButtons = new Vector<MenuButton>();
    private Bat Bat_1, Bat_2;
    private Ball ball;
    private String splashString, IP_LAN, connectionState, connectionIP;
    private Color splashColor;

    private int Score_1, Score_2;
    private int speed_mod = 0;// Additional speed 

    private boolean LMB_Down = true;//Mouse left button trigger
    private boolean isHost, isAI_1, isAI_2;//is AI enabled(net game) 1 = red side, 2 = blue side
    private boolean showSplash = false;

    MainPong()
    {
        glDisplay = new GL_Display();
        glDisplay.start(WIDTH, HEIGHT, "Pong by ReaLgressA (1.3) Final. [3 January 2013]", false);
        glClearColor((float) 0.1, (float) 0.1, (float) 0.1, 0);// Set cleaning color to dark gray
        SetFonts();//Setup fonts
        //Initialize main menu buttons
        MenuButtons.add(new MenuButton(100, 200, "Singleplayer", Color.white, Color.gray));
        MenuButtons.add(new MenuButton(100, 250, "Multiplayer", Color.white, Color.gray));
        MenuButtons.add(new MenuButton(100, 300, "Direct IP", Color.white, Color.gray));
        MenuButtons.add(new MenuButton(100, 350, "Exit", Color.white, Color.gray));
        MenuButtons.add(new MenuButton(100, 200, "Host", Color.white, Color.gray));
        MenuButtons.add(new MenuButton(100, 250, "Join", Color.white, Color.gray));
        MenuButtons.add(new MenuButton(100, 350, "Back", Color.white, Color.gray));
        MenuButtons.get(4).isActive = false;
        MenuButtons.get(4).isVisible = false;
        MenuButtons.get(5).isActive = false;
        MenuButtons.get(5).isVisible = false;
        MenuButtons.get(6).isActive = false;
        MenuButtons.get(6).isVisible = false;
        while (!Display.isCloseRequested())
        {
            processKeyboard();//check keyboard events
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);//Clear screen
            int delta = glDisplay.getDelta();
            render();
            update(delta);
            Display.update();
            Display.sync(FPS);
        }
        Display.destroy();
        System.exit(0);
    }

    void render()
    {
        switch (gState)
        {
            case MainMenu:
                glBegin(GL_QUADS);
                glColor3d(1.0, 0, 0);
                glVertex2i(0, 190);
                glVertex2i(800, 190);
                glColor3d(0, 0, 1.0);
                glVertex2i(800, 400);
                glVertex2i(0, 400);
                glEnd();
                for (Entity object : MenuButtons)
                {
                    object.draw();
                }
                break;
            case Single:
            case Multi:
            case Net:
                glColor3d(1, 1, 1);
                glBegin(GL_QUADS);
                for (int i = 5; i < HEIGHT; i += 60)
                {
                    glVertex2i(WIDTH / 2 - 3, i);
                    glVertex2i(WIDTH / 2 + 3, i);
                    glVertex2i(WIDTH / 2 + 3, i + 50);
                    glVertex2i(WIDTH / 2 - 3, i + 50);
                }
                glEnd();
                glBegin(GL_LINES);
                glVertex2i(0, 1);
                glVertex2i(WIDTH, 1);
                glVertex2i(WIDTH, HEIGHT);
                glVertex2i(0, HEIGHT);
                glEnd();

                Bat_1.draw();
                Bat_2.draw();
                ball.draw();

                glEnable(GL_BLEND);//enable blend for text drawing
                if (isAI_1 && gState == GameState.Net)//Draw "AI" if it activated
                {
                    fontSplash.drawString(50, 50, "AI");
                }
                if (isAI_2 && gState == GameState.Net)//Same for second player
                {
                    fontSplash.drawString(WIDTH - 50 - fontSplash.getWidth("AI"), 50, "AI");
                }
                fontScore.drawString(WIDTH / 2 - 80, 40, "" + Score_1);//Score. Middle & top
                fontScore.drawString(WIDTH / 2 + 80 - fontScore.getWidth("" + Score_2), 40, "" + Score_2);
                if (showSplash)//Draw popup text if it activated
                {
                    fontSplash.drawString(WIDTH / 2 - fontSplash.getWidth(splashString) / 2, HEIGHT / 2 - fontSplash.getHeight(splashString), splashString, splashColor);
                }
                glDisable(GL_BLEND);
                break;
            case Connecting:
                glEnable(GL_BLEND);
                if (connectionState.equals("Connecting to host..."))
                {
                    fontConnect.drawString(WIDTH / 2 - fontConnect.getWidth("Input IP: " + connectionIP) / 2, HEIGHT / 2 - fontConnect.getHeight("Input IP: " + connectionIP) / 2, "Input IP: " + connectionIP, Color.white);
                }
                else
                {
                    fontConnect.drawString(WIDTH / 2 - fontConnect.getWidth(connectionState) / 2, HEIGHT / 2 - fontConnect.getHeight(connectionState) / 2, connectionState, Color.white);
                }
                if (showSplash)
                {
                    fontSplash.drawString(WIDTH / 2 - fontSplash.getWidth(splashString) / 2, HEIGHT / 3 - fontSplash.getHeight(splashString), splashString, splashColor);
                }
                glDisable(GL_BLEND);
                break;
        }
        //Print fps after render
        int tFPS = glDisplay.AQupdateFPS();
        if (tFPS > 0)
        {
            fps = tFPS;
        }
        glEnable(GL_BLEND);//Enable blending for correct rendering of textures(yea text is texture)
        fontFPS.drawString(0, 0, "FPS: " + fps + "/" + FPS + "    press F11 for toggle fullscreen ", Color.green);
        glDisable(GL_BLEND);
    }

    void update(int delta)
    {
        if (delta > 250)// if drawing is too slow ignore update(). (usually happens then toggle full screen or moving window)
        {
            return;
        }
        switch (gState)
        {
            case MainMenu:
                for (MenuButton object : MenuButtons)
                {
                    object.update(delta);
                    if (!Mouse.isButtonDown(0))//For nice button clicking
                    {
                        LMB_Down = false;
                    }
                    else if (object.isActive && object.onTop && !LMB_Down)
                    {
                        LMB_Down = true;
                        if (object.text() == "Singleplayer")
                        {
                            StartSingle();
                        }
                        else if (object.text() == "Multiplayer")
                        {
                            StartMulti();
                        }
                        else if (object.text() == "Exit")
                        {
                            System.exit(0);
                        }
                        else if (object.text() == "Host")
                        {
                            try
                            {
                                IP_LAN = InetAddress.getLocalHost().getHostAddress();
                            }
                            catch (UnknownHostException e)
                            {
                                e.printStackTrace();
                            }
                            connectionState = "Waiting for opponent. Your IP: " + IP_LAN;
                            Server = new TCP_Server(1255);
                            Server.start();
                            gState = GameState.Connecting;
                        }
                        else if (object.text() == "Join")
                        {
                            connectionState = "Connecting to host...";
                            connectionIP = "";
                            gState = GameState.Connecting;
                        }
                        else if (object.text() == "Direct IP")
                        {
                            MenuButtons.get(0).isActive = false;
                            MenuButtons.get(0).isVisible = false;
                            MenuButtons.get(1).isActive = false;
                            MenuButtons.get(1).isVisible = false;
                            MenuButtons.get(2).isActive = false;
                            MenuButtons.get(2).isVisible = false;
                            MenuButtons.get(3).isActive = false;
                            MenuButtons.get(3).isVisible = false;
                            MenuButtons.get(4).isActive = true;
                            MenuButtons.get(4).isVisible = true;
                            MenuButtons.get(5).isActive = true;
                            MenuButtons.get(5).isVisible = true;
                            MenuButtons.get(6).isActive = true;
                            MenuButtons.get(6).isVisible = true;
                        }
                        else if (object.text() == "Back")
                        {
                            MenuButtons.get(0).isActive = true;
                            MenuButtons.get(0).isVisible = true;
                            MenuButtons.get(1).isActive = true;
                            MenuButtons.get(1).isVisible = true;
                            MenuButtons.get(2).isActive = true;
                            MenuButtons.get(2).isVisible = true;
                            MenuButtons.get(3).isActive = true;
                            MenuButtons.get(3).isVisible = true;
                            MenuButtons.get(4).isActive = false;
                            MenuButtons.get(4).isVisible = false;
                            MenuButtons.get(5).isActive = false;
                            MenuButtons.get(5).isVisible = false;
                            MenuButtons.get(6).isActive = false;
                            MenuButtons.get(6).isVisible = false;
                        }
                    }
                }
                break;
            case Single:
                Bat_2.AI(ball.getX(), ball.getY(), ball.getDX(), ball.getDY(), (double) -0.035 + speed_mod / 5000.0, true);
            case Multi:
                Bat_1.update(delta);
                Bat_2.update(delta);
                ball.update(delta);
                if (ball.getDX() < 0)//Ball moving to Bat_1
                {
                    if (Bat_1.intersects(ball))
                    {
                        if (speed_mod < 60)
                        {
                            speed_mod += 3;
                        }
                        if (ball.getDX() > 0)
                        {
                            ball.setDX(-GAME_BASE_SPEED - (double) speed_mod / 100.0);
                        }
                        else
                        {
                            ball.setDX(GAME_BASE_SPEED + (double) speed_mod / 100.0);
                        }
                    }
                    if (ball.getX() < 1)
                    {
                        ScoreGoal(true);
                    }
                }
                else//To Bat_2
                {
                    if (Bat_2.intersects(ball))
                    {
                        if (speed_mod < 60)
                        {
                            speed_mod += 3;
                        }
                        if (ball.getDX() > 0)
                        {
                            ball.setDX(-GAME_BASE_SPEED - (double) speed_mod / 100.0);
                        }
                        else
                        {
                            ball.setDX(GAME_BASE_SPEED + (double) speed_mod / 100.0);
                        }
                    }
                    if (ball.getX() > WIDTH - 1)
                    {
                        ScoreGoal(false);
                    }
                }
                checkBoost(speed_mod);//Check current boost value. Show message if speed increases enough;
                break;
            case Net:
                if (isAI_2 && !isHost)
                {
                    Bat_2.AI(ball.getX(), ball.getY(), ball.getDX(), ball.getDY(), (double) -0.035 + speed_mod / 5000.0, true);
                }
                if (isAI_1 && isHost)
                {
                    Bat_1.AI(ball.getX(), ball.getY(), ball.getDX(), ball.getDY(), (double) -0.035 + speed_mod / 5000.0, false);
                }
                Bat_1.update(delta);
                if (isHost && Bat_1.isDirChanged)
                {
                    Bat_1.isDirChanged = false;
                    Server.SendMessage("Move", Bat_1.getY() + "|" + Bat_1.getDY());
                }
                Bat_2.update(delta);
                if (!isHost && Bat_2.isDirChanged)
                {
                    Bat_2.isDirChanged = false;
                    Client.SendMessage("Move", Bat_2.getY() + "|" + Bat_2.getDY());
                }
                ball.update(delta);
                if (isHost && ball.isDirChanged)//Ball direction changed
                {
                    ball.isDirChanged = false;
                    Server.SendMessage("Ball", ball.getX() + "|" + ball.getY() + "|" + ball.getDX() + "|" + ball.getDY());
                }
                if (ball.getDX() < 0)//Ball moving to Bat_1
                {
                    if (Bat_1.intersects(ball))
                    {
                        if (speed_mod < 60)
                        {
                            speed_mod += 3;
                        }
                        if (ball.getDX() > 0)
                        {
                            ball.setDX(-GAME_BASE_SPEED - (double) speed_mod / 100.0);
                        }
                        else
                        {
                            ball.setDX(GAME_BASE_SPEED + (double) speed_mod / 100.0);
                        }
                    }
                    if (ball.getX() < 1 && isHost)
                    {
                        ScoreGoalNet(false);
                    }
                }
                else//To Bat_2
                {
                    if (Bat_2.intersects(ball))
                    {
                        if (speed_mod < 60)
                        {
                            speed_mod += 3;
                        }
                        if (ball.getDX() > 0)
                        {
                            ball.setDX(-GAME_BASE_SPEED - (double) speed_mod / 100.0);
                        }
                        else
                        {
                            ball.setDX(GAME_BASE_SPEED + (double) speed_mod / 100.0);
                        }
                    }
                    if (ball.getX() > WIDTH - 1 && isHost)
                    {
                        ScoreGoalNet(true);
                    }
                }
                if (isHost)
                {
                    TCP_Message msg = Server.GetMessage();
                    while (msg != null)
                    {
                        System.out.println("Server. Message received. cmd: " + msg.cmd + " data: " + msg.data);
                        switch (msg.cmd)
                        {
                            case "SYS_EX":
                            case "NET_ER":
                                splashString = "error: " + msg.data;
                                splashColor = Color.red;
                                showSplash = true;
                                ball.setDX(0);
                                ball.setDY(0);
                                break;
                            case "Move":
                                String tmp[] = msg.data.split("\\|");
                                Bat_2.setY(Double.parseDouble(tmp[0]));
                                Bat_2.setDY(Double.parseDouble(tmp[1]));
                                break;
                            case "AI":
                                isAI_2 = !isAI_2;
                                break;
                        }
                        msg = Server.GetMessage();
                    }
                }
                else
                {
                    TCP_Message msg = Client.GetMessage();
                    while (msg != null)
                    {
                        System.out.println("Client. Message received. cmd: " + msg.cmd + " data: " + msg.data);
                        switch (msg.cmd)
                        {
                            case "SYS_EX":
                            case "NET_ER":
                                splashString = "error: " + msg.data;
                                splashColor = Color.red;
                                showSplash = true;
                                ball.setDX(0);
                                ball.setDY(0);
                                break;
                            case "Move":
                                String tmp[] = msg.data.split("\\|");
                                Bat_1.setY(Double.parseDouble(tmp[0]));
                                Bat_1.setDY(Double.parseDouble(tmp[1]));
                                break;
                            case "AI":
                                isAI_1 = !isAI_1;
                                break;
                            case "Ball":
                                tmp = msg.data.split("\\|");
                                ball.setX(Double.parseDouble(tmp[0]));
                                ball.setY(Double.parseDouble(tmp[1]));
                                ball.setDX(Double.parseDouble(tmp[2]));
                                ball.setDY(Double.parseDouble(tmp[3]));
                                break;
                            case "Score":
                                speed_mod = 0;
                                String t[] = msg.data.split("\\|");
                                Score_1 = Integer.parseInt(t[0]);
                                Score_2 = Integer.parseInt(t[1]);
                                if (Score_1 == 9)//red wins
                                {
                                    splashColor = Color.red;
                                    splashString = "You lose!";
                                }
                                else if (Score_2 == 9)//blue wins
                                {
                                    splashColor = Color.green;
                                    splashString = "You win!";
                                }
                                break;
                        }
                        msg = Client.GetMessage();
                    }
                }
                checkBoost(speed_mod);
                break;
            case Connecting:
                if (Server != null)
                {
                    TCP_Message msg = Server.GetMessage();
                    while (msg != null)
                    {
                        System.out.println("Server. Message received. cmd: " + msg.cmd + " data: " + msg.data);
                        switch (msg.cmd)
                        {
                            case "SYS_EX":
                            case "NET_ER":
                                connectionState = "error: " + msg.data;
                                break;
                            case "Join":
                                Server.SendMessage("Join", "0");
                                StartNet(true);
                                break;
                        }
                        msg = Server.GetMessage();
                    }
                }
                else if (Client != null)
                {
                    TCP_Message msg = Client.GetMessage();
                    while (msg != null)
                    {
                        System.out.println("Client. Message received. cmd: " + msg.cmd + " data: " + msg.data);
                        switch (msg.cmd)
                        {
                            case "SYS_EX":
                            case "NET_ER":
                                Client.Disconnect();
                                Client = null;
                                gState = GameState.MainMenu;
                                return;
                            case "Join":
                                StartNet(false);
                                break;
                        }
                        msg = Client.GetMessage();
                    }
                }
                break;
        }
    }

    void processKeyboard()
    {
        while (Keyboard.next())
        {
            switch (gState)
            {
                case MainMenu:
                    switch (Keyboard.getEventKey())
                    {
                        case Keyboard.KEY_F11:
                            glDisplay.setDisplayMode(WIDTH, HEIGHT, !Display.isFullscreen());
                            break;
                    }
                    break;
                case Single:
                    switch (Keyboard.getEventKey())
                    {
                        case Keyboard.KEY_ESCAPE:
                            gState = GameState.MainMenu;
                            break;
                        case Keyboard.KEY_F11:
                            glDisplay.setDisplayMode(WIDTH, HEIGHT, !Display.isFullscreen());
                            break;
                        case Keyboard.KEY_W:
                            if (Keyboard.getEventKeyState() && Bat_1.getDY() == 0)
                            {
                                Bat_1.setDY(-GAME_BASE_SPEED - speed_mod / 300);
                            }
                            else if (Keyboard.isKeyDown(Keyboard.KEY_S))
                            {
                                Bat_1.setDY(GAME_BASE_SPEED + speed_mod / 300);
                            }
                            else
                            {
                                Bat_1.setDY(0);
                            }
                            break;
                        case Keyboard.KEY_S:
                            if (Keyboard.getEventKeyState() && Bat_1.getDY() == 0)
                            {
                                Bat_1.setDY(GAME_BASE_SPEED + speed_mod / 300);
                            }
                            else if (Keyboard.isKeyDown(Keyboard.KEY_W))
                            {
                                Bat_1.setDY(-GAME_BASE_SPEED - speed_mod / 300);
                            }
                            else
                            {
                                Bat_1.setDY(0);
                            }
                            break;
                    }
                    break;
                case Multi:
                    switch (Keyboard.getEventKey())
                    {
                        case Keyboard.KEY_ESCAPE:
                            gState = GameState.MainMenu;
                            break;
                        case Keyboard.KEY_F11:
                            glDisplay.setDisplayMode(WIDTH, HEIGHT, !Display.isFullscreen());
                            break;
                        case Keyboard.KEY_W:
                            if (Keyboard.getEventKeyState() && Bat_1.getDY() == 0)
                            {
                                Bat_1.setDY(-GAME_BASE_SPEED - speed_mod / 300);
                            }
                            else if (Keyboard.isKeyDown(Keyboard.KEY_S))
                            {
                                Bat_1.setDY(GAME_BASE_SPEED + speed_mod / 300);
                            }
                            else
                            {
                                Bat_1.setDY(0);
                            }
                            break;
                        case Keyboard.KEY_S:
                            if (Keyboard.getEventKeyState() && Bat_1.getDY() == 0)
                            {
                                Bat_1.setDY(GAME_BASE_SPEED + speed_mod / 300);
                            }
                            else if (Keyboard.isKeyDown(Keyboard.KEY_W))
                            {
                                Bat_1.setDY(-GAME_BASE_SPEED - speed_mod / 300);
                            }
                            else
                            {
                                Bat_1.setDY(0);
                            }
                            break;
                        case Keyboard.KEY_UP:
                            if (Keyboard.getEventKeyState() && Bat_2.getDY() == 0)
                            {
                                Bat_2.setDY(-GAME_BASE_SPEED - speed_mod / 300);
                            }
                            else if (Keyboard.isKeyDown(Keyboard.KEY_DOWN))
                            {
                                Bat_2.setDY(GAME_BASE_SPEED + speed_mod / 300);
                            }
                            else
                            {
                                Bat_2.setDY(0);
                            }
                            break;
                        case Keyboard.KEY_DOWN:
                            if (Keyboard.getEventKeyState() && Bat_2.getDY() == 0)
                            {
                                Bat_2.setDY(GAME_BASE_SPEED + speed_mod / 300);
                            }
                            else if (Keyboard.isKeyDown(Keyboard.KEY_UP))
                            {
                                Bat_2.setDY(-GAME_BASE_SPEED - speed_mod / 300);
                            }
                            else
                            {
                                Bat_2.setDY(0);
                            }
                            break;
                    }
                    break;
                case Net:
                    switch (Keyboard.getEventKey())
                    {
                        case Keyboard.KEY_RETURN:
                            if (isHost && Keyboard.getEventKeyState() && ball.getDX() == 0 && ball.getDY() == 0)
                            {
                                Random random = new Random();
                                if (random.nextInt(2) == 0)
                                {
                                    ball.setDX(-GAME_BASE_SPEED);
                                }
                                else
                                {
                                    ball.setDX(GAME_BASE_SPEED);
                                }
                                if (random.nextInt(2) == 0)
                                {
                                    ball.setDY(-GAME_BASE_SPEED);
                                }
                                else
                                {
                                    ball.setDY(GAME_BASE_SPEED);
                                }
                                showSplash = false;
                            }
                            break;
                        case Keyboard.KEY_SPACE:
                            if (Keyboard.getEventKeyState())
                            {
                                if (isHost)
                                {
                                    Server.SendMessage("AI", "0");
                                    isAI_1 = !isAI_1;
                                }
                                else
                                {
                                    Client.SendMessage("AI", "0");
                                    isAI_2 = !isAI_2;
                                }
                            }
                            break;
                        case Keyboard.KEY_ESCAPE:
                            showSplash = false;
                            if (isHost)
                            {
                                Server.Shutdown();
                                Server = null;
                            }
                            else
                            {
                                Client.Disconnect();
                                Client = null;
                            }
                            gState = GameState.MainMenu;
                            break;
                        case Keyboard.KEY_F11:
                            glDisplay.setDisplayMode(WIDTH, HEIGHT, !Display.isFullscreen());
                            break;
                        case Keyboard.KEY_W:
                            if (isHost && !isAI_1)
                            {
                                if (Keyboard.getEventKeyState() && Bat_1.getDY() == 0)
                                {
                                    Bat_1.setDY(-GAME_BASE_SPEED - speed_mod / 300);
                                }
                                else if (Keyboard.isKeyDown(Keyboard.KEY_S))
                                {
                                    Bat_1.setDY(GAME_BASE_SPEED + speed_mod / 300);
                                }
                                else
                                {
                                    Bat_1.setDY(0);
                                }
                            }
                            else if (!isAI_2)
                            {
                                if (Keyboard.getEventKeyState() && Bat_2.getDY() == 0)
                                {
                                    Bat_2.setDY(-GAME_BASE_SPEED - speed_mod / 300);
                                }
                                else if (Keyboard.isKeyDown(Keyboard.KEY_S))
                                {
                                    Bat_2.setDY(GAME_BASE_SPEED + speed_mod / 300);
                                }
                                else
                                {
                                    Bat_2.setDY(0);
                                }
                            }
                            break;
                        case Keyboard.KEY_S:
                            if (isHost && !isAI_1)
                            {
                                if (Keyboard.getEventKeyState() && Bat_1.getDY() == 0)
                                {
                                    Bat_1.setDY(GAME_BASE_SPEED + speed_mod / 300);
                                }
                                else if (Keyboard.isKeyDown(Keyboard.KEY_W))
                                {
                                    Bat_1.setDY(-GAME_BASE_SPEED - speed_mod / 300);
                                }
                                else
                                {
                                    Bat_1.setDY(0);
                                }
                            }
                            else if (!isAI_2)
                            {
                                if (Keyboard.getEventKeyState() && Bat_2.getDY() == 0)
                                {
                                    Bat_2.setDY(GAME_BASE_SPEED + speed_mod / 300);
                                }
                                else if (Keyboard.isKeyDown(Keyboard.KEY_W))
                                {
                                    Bat_2.setDY(-GAME_BASE_SPEED - speed_mod / 300);
                                }
                                else
                                {
                                    Bat_2.setDY(0);
                                }
                            }
                            break;
                    }
                    break;
                case Connecting:
                    if (Keyboard.getEventKey() == Keyboard.KEY_ESCAPE)
                    {
                        if (Server != null)
                        {
                            Server.Shutdown();
                            Server = null;
                        }
                        else if (Client != null)
                        {
                            Client.Disconnect();
                            Client = null;
                        }
                        else
                        {
                            gState = GameState.MainMenu;
                        }
                    }
                    else if (connectionState.equals("Connecting to host..."))
                    {
                        char c = Keyboard.getEventCharacter();
                        if (connectionIP.length() < 15 && (c == '.' || c == '0' || c == '1' || c == '2' || c == '3' || c == '4' || c == '5' || c == '6' || c == '7' || c == '8' || c == '9'))
                        {
                            connectionIP += "" + c;
                        }
                        else if (Keyboard.isKeyDown(Keyboard.KEY_BACK) && !connectionIP.isEmpty())
                        {
                            connectionIP = connectionIP.substring(0, connectionIP.length() - 1);
                        }
                        else if (Keyboard.isKeyDown(Keyboard.KEY_RETURN))
                        {
                            //Detect valid ip
                            Pattern ip = Pattern.compile(
                                    "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
                                    + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
                                    + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
                                    + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");
                            Matcher matcher = ip.matcher(connectionIP);
                            if (matcher.matches())//IP is correct
                            {
                                Client = new TCP_Client(connectionIP, 1255);
                                String temp = Client.Connect();
                                if (temp != null)
                                {
                                    splashString = temp;
                                    splashColor = Color.red;
                                    showSplash = true;
                                    Client = null;
                                }
                                else
                                {
                                    Client.start();
                                    Client.SendMessage("Join", "0");
                                }
                            }
                            else
                            {
                                splashString = "wrong IP format";
                                splashColor = Color.red;
                                showSplash = true;
                            }
                            try
                            {
                                splashTimer.cancel();
                                splashTimer.purge();
                            }
                            catch (java.lang.IllegalStateException ex)
                            {
                                System.out.println(ex.getMessage());
                            }
                            finally
                            {
                                splashTimer = new Timer();
                            }
                            splashTimer.schedule(new TimerTask()
                            {
                                public void run()
                                {
                                    showSplash = false;
                                }
                            }, SPLASH_COOLDOWN);
                        }
                    }
                    break;
            }
        }
    }

    void SetFonts()
    {
        try
        {
            Font awtFont = new Font("Arial", Font.PLAIN, 12);//Fps counter font
            fontFPS = new UnicodeFont(awtFont);
            fontFPS.getEffects().add(new ColorEffect(java.awt.Color.white));
            fontFPS.addAsciiGlyphs();
            fontFPS.loadGlyphs();
            awtFont = new Font("Arial", Font.BOLD, 64);//Score counter font
            fontScore = new UnicodeFont(awtFont);
            fontScore.getEffects().add(new ColorEffect(java.awt.Color.white));
            fontScore.addAsciiGlyphs();
            fontScore.loadGlyphs();
            awtFont = new Font("Helvitica", Font.ITALIC, 48);//Popup text font
            fontSplash = new UnicodeFont(awtFont);
            fontSplash.getEffects().add(new ColorEffect(java.awt.Color.white));
            fontSplash.addAsciiGlyphs();
            fontSplash.loadGlyphs();
            awtFont = new Font("Arial", Font.BOLD, 24);//Connection state text font
            fontConnect = new UnicodeFont(awtFont);
            fontConnect.getEffects().add(new ColorEffect(java.awt.Color.white));
            fontConnect.addAsciiGlyphs();
            fontConnect.loadGlyphs();
        }
        catch (SlickException e)
        {
            e.printStackTrace();
        }
    }

    void StartSingle()
    {
        //Init Bats for game modes
        speed_mod = 0;
        Bat_1 = new Bat(0, HEIGHT / 2 - 40, 15, 80, Color.red);
        Bat_2 = new Bat(WIDTH - 15, HEIGHT / 2 - 40, 15, 80, Color.blue);
        //next initialize ball direction
        ball = new Ball(WIDTH / 2, HEIGHT / 2, 8);
        Random random = new Random();
        if (random.nextInt(2) == 0)
        {
            ball.setDX(-GAME_BASE_SPEED);
        }
        else
        {
            ball.setDX(GAME_BASE_SPEED);
        }
        if (random.nextInt(2) == 0)
        {
            ball.setDY(-GAME_BASE_SPEED);
        }
        else
        {
            ball.setDY(GAME_BASE_SPEED);
        }
        Score_1 = 0;
        Score_2 = 0;
        gState = GameState.Single;//Change game state
        splashString = "I challenge you! :D";
        splashColor = Color.orange;
        showSplash = true;
        try
        {
            splashTimer.cancel();
            splashTimer.purge();
        }
        catch (java.lang.IllegalStateException ex)
        {
            System.out.println(ex.getMessage());
        }
        finally
        {
            splashTimer = new Timer();
        }
        splashTimer.schedule(new TimerTask()
        {
            public void run()
            {
                showSplash = false;
            }
        }, SPLASH_COOLDOWN);
    }

    void StartMulti()
    {
        speed_mod = 0;
        Bat_1 = new Bat(0, HEIGHT / 2 - 40, 15, 80, Color.red);
        Bat_2 = new Bat(WIDTH - 15, HEIGHT / 2 - 40, 15, 80, Color.blue);
        ball = new Ball(WIDTH / 2, HEIGHT / 2, 8);
        Random random = new Random();
        if (random.nextInt(2) == 0)
        {
            ball.setDX(-GAME_BASE_SPEED);
        }
        else
        {
            ball.setDX(GAME_BASE_SPEED);
        }
        if (random.nextInt(2) == 0)
        {
            ball.setDY(-GAME_BASE_SPEED);
        }
        else
        {
            ball.setDY(GAME_BASE_SPEED);
        }
        Score_1 = 0;
        Score_2 = 0;
        gState = GameState.Multi;
    }

    void StartNet(boolean host)
    {
        isHost = host;
        isAI_1 = false;
        isAI_2 = false;
        Bat_1 = new Bat(0, HEIGHT / 2 - 40, 15, 80, Color.red);
        Bat_2 = new Bat(WIDTH - 15, HEIGHT / 2 - 40, 15, 80, Color.blue);
        ball = new Ball(WIDTH / 2, HEIGHT / 2, 8);
        speed_mod = 0;
        Score_1 = 0;
        Score_2 = 0;
        gState = GameState.Net;
        if (isHost)
        {
            splashColor = Color.magenta;
            showSplash = true;
            splashString = "Press \"Enter\" to launch the ball";
        }
    }

    void ScoreGoalNet(boolean player)//true - red side  false - blue
    {
        speed_mod = 0;
        ball.setDX(0);
        ball.setDY(0);
        ball.setX(WIDTH / 2);
        ball.setY(HEIGHT / 2);
        if (player)
        {
            Score_1++;
        }
        else
        {
            Score_2++;
        }
        showSplash = true;
        if (Score_1 == 9)//red wins
        {
            splashColor = Color.green;
            splashString = "You win!";
        }
        else if (Score_2 == 9)//blue wins
        {
            splashColor = Color.red;
            splashString = "You lose!";
        }
        else//continue game
        {
            splashColor = Color.magenta;
            splashString = "Press \"Enter\" to launch the ball";
        }
        Server.SendMessage("Score", Score_1 + "|" + Score_2);
    }

    void ScoreGoal(boolean player)// true - Bat_1 false - Bat_2
    {
        speed_mod = 0;
        Random random = new Random();
        if (random.nextInt(2) == 0)
        {
            ball.setDY(-GAME_BASE_SPEED);
        }
        else
        {
            ball.setDY(GAME_BASE_SPEED);
        }
        ball.setX(WIDTH / 2);
        ball.setY(HEIGHT / 2);
        if (player)
        {
            if (Score_2 < 8)
            {
                Score_2++;
            }
            else
            {
                splashColor = Color.red;
                splashString = "You lose!";
                Score_1 = 0;
                Score_2 = 0;   //Temporal. its win!
                showSplash = true;
                try
                {
                    splashTimer.cancel();
                    splashTimer.purge();
                }
                catch (java.lang.IllegalStateException ex)
                {
                    System.out.println(ex.getMessage());
                }
                finally
                {
                    splashTimer = new Timer();
                }
                splashTimer.schedule(new TimerTask()
                {
                    public void run()
                    {
                        gState = GameState.MainMenu;
                    }
                }, SPLASH_COOLDOWN);
                ball.setDX(0);
                ball.setDY(0);
                return;
            }
            ball.setDX(GAME_BASE_SPEED);
            //say something to player :D (Singleplayer only)
            if (gState == GameState.Single && random.nextInt() % 4 == 0)// 25%
            {
                switch (random.nextInt() % 5)
                {
                    case 0:
                        splashString = ":3";
                        splashColor = Color.yellow;
                        break;
                    case 1:
                        splashString = "<3";
                        splashColor = Color.pink;
                        break;
                    case 2:
                        splashString = "^_^";
                        splashColor = Color.cyan;
                        break;
                    case 3:
                        splashString = "LOL :D";
                        splashColor = Color.green;
                        break;
                    case 4:
                        splashString = "ROFL xD";
                        splashColor = Color.red;
                        break;
                }
                showSplash = true;
                try
                {
                    splashTimer.cancel();
                    splashTimer.purge();
                }
                catch (java.lang.IllegalStateException ex)
                {
                    System.out.println(ex.getMessage());
                }
                finally
                {
                    splashTimer = new Timer();
                }
                splashTimer.schedule(new TimerTask()
                {
                    public void run()
                    {
                        showSplash = false;
                    }
                }, SPLASH_COOLDOWN);
            }

        }
        else
        {
            if (Score_1 < 8)
            {
                Score_1++;
            }
            else
            {
                splashColor = Color.green;
                splashString = "You win!";
                Score_1 = 0;
                Score_2 = 0;   //its win!
                showSplash = true;
                try
                {
                    splashTimer.cancel();
                    splashTimer.purge();
                }
                catch (java.lang.IllegalStateException ex)
                {
                    System.out.println(ex.getMessage());
                }
                finally
                {
                    splashTimer = new Timer();
                }
                splashTimer.schedule(new TimerTask()
                {
                    public void run()
                    {
                        gState = GameState.MainMenu;
                    }
                }, 5000);
                ball.setDX(0);
                ball.setDY(0);
                return;
            }
            ball.setDX(-GAME_BASE_SPEED);
        }
    }

    void checkBoost(int boost)
    {
        if (boost == 12)// 1.5 speed
        {
            splashString = "Hurry up 1.5x speed";
            splashColor = Color.green;
            showSplash = true;
        }
        else if (boost == 27)//2x speed
        {
            splashString = "Keep steady! 2.0x speed";
            splashColor = Color.cyan;
            showSplash = true;
        }
        else if (boost == 39)
        {
            splashString = "Amazing! 2.5x speed";
            splashColor = Color.red;
            showSplash = true;
        }
        else if (boost == 54)
        {
            splashString = "Supersonic! 3.0x speed";
            splashColor = Color.blue;
            showSplash = true;
        }
        else
        {
            return;
        }
        try
        {
            splashTimer.cancel();
            splashTimer.purge();
        }
        catch (java.lang.IllegalStateException ex)
        {
            System.out.println(ex.getMessage());
        }
        finally
        {
            splashTimer = new Timer();
        }
        splashTimer.schedule(new TimerTask()
        {
            public void run()
            {
                showSplash = false;
            }
        }, SPLASH_COOLDOWN);
    }

    public static void main(String[] argv)
    {
        new MainPong();
    }
}

/**
 * Below is my custom classes for this epic pong game ! Definitely they're just
 * useless wrapper.
 *
 * @author ReaLgressA
 *
 * Menu buttons, bat and ball classes;
 */
class MenuButton extends AbstractEntity
{

    public boolean isVisible = true, isActive = true, onTop = false;
    ;
	private String text;
    private UnicodeFont font;//font -> simple font, font_up - mouse is on the top
    private Color color, color_top;

    public MenuButton(double x, double y, String text, Color color, Color top_color)
    {
        super(x, y, 0, 0);
        this.text = text;
        this.color = color;
        this.color_top = top_color;
        Font awtFont = new Font("Arial", Font.BOLD, 32);// Main menu font
        font = new UnicodeFont(awtFont);
        font.getEffects().add(new ColorEffect(java.awt.Color.white));
        font.addAsciiGlyphs();
        try
        {
            font.loadGlyphs();
        }
        catch (SlickException e)
        {
            e.printStackTrace();
        }
        setWidth(font.getWidth(text));
        setHeight(font.getLineHeight());
    }

    @Override
    public void draw()
    {
        if (!isVisible)
        {
            return;
        }
        glEnable(GL_BLEND);
        if (!onTop)
        {
            font.drawString((float) x, (float) y, text, color);
        }
        else
        {
            font.drawString((float) x, (float) y, text, color_top);
        }
        glDisable(GL_BLEND);
    }

    @Override
    public void update(int delta)
    {
        if (!isActive)
        {
            return;
        }
        if (x < Mouse.getX() && x + width > Mouse.getX() && y < Display.getHeight() - Mouse.getY() && y + height > Display.getHeight() - Mouse.getY())
        {
            onTop = true;
        }
        else
        {
            onTop = false;
        }
    }

    public String text()
    {
        return text;
    }

    public void setText(String text)
    {
        this.text = text;
        setWidth(font.getWidth(text));
        setHeight(font.getLineHeight());
    }

}

class Bat extends AbstractMovableEntity
{

    private Color color;
    public boolean isDirChanged = false;

    public Bat(double x, double y, double width, double height, Color color)
    {
        super(x, y, width, height);
        this.color = color;
    }

    @Override
    public void draw()
    {
        glColor3f(color.r, color.g, color.b);
        glBegin(GL_QUADS);
        glVertex2d(x, y);
        glVertex2d(x + width, y);
        glVertex2d(x + width, y + height);
        glVertex2d(x, y + height);
        glEnd();
    }

    @Override
    public void setDX(double dx)
    {
        if (this.dx != dx)
        {
            isDirChanged = true;
        }
        // TODO Auto-generated method stub
        super.setDX(dx);
    }

    @Override
    public void setDY(double dy)
    {
        if (this.dy != dy)
        {
            isDirChanged = true;
        }
        // TODO Auto-generated method stub
        super.setDY(dy);
    }

    @Override
    public void update(int delta)
    {
        y += delta * dy;
        if (y > Display.getHeight() - height - 1)
        {
            y = Display.getHeight() - height - 1;
        }
        else if (y < 1)
        {
            y = 1;
        }
    }

    public void AI(double ball_x, double ball_y, double ball_dx, double ball_dy, double speed_mod, boolean RightSide)
    {
        if (RightSide)
        {
            if (ball_dx > 0)//ONLY for RIGHT side AI !
            {//move closer to ball position
                if (y + height / 2 < ball_y)
                {
                    setDY(0.25 + speed_mod);
                }
                else if (y + height / 2 > ball_y)
                {
                    setDY(-0.25 - speed_mod);
                }
                else
                {
                    setDY(0);
                }
            }
            else
            {//move to center
                if (y > Display.getHeight() / 2 - height / 2 - 2 && y < Display.getHeight() / 2 - height / 2 + 2)
                {
                    setDY(0);
                }
                else if (y < Display.getHeight() / 2 - height / 2)
                {
                    setDY(0.25 + speed_mod);
                }
                else if (y > Display.getHeight() / 2 - height / 2)
                {
                    setDY(-0.25 - speed_mod);
                }
            }
        }
        else
        {
            if (ball_dx < 0)//ONLY for LEFT side AI !
            {//move closer to ball position
                if (y + height / 2 < ball_y)
                {
                    setDY(0.25 + speed_mod);
                }
                else if (y + height / 2 > ball_y)
                {
                    setDY(-0.25 - speed_mod);
                }
                else
                {
                    setDY(0);
                }
            }
            else
            {//move to center
                if (y > Display.getHeight() / 2 - height / 2 - 2 && y < Display.getHeight() / 2 - height / 2 + 2)
                {
                    setDY(0);
                }
                else if (y < Display.getHeight() / 2 - height / 2)
                {
                    setDY(0.25 + speed_mod);
                }
                else if (y > Display.getHeight() / 2 - height / 2)
                {
                    setDY(-0.25 - speed_mod);
                }
            }
        }
    }
}

class Ball extends AbstractMovableEntity
{

    private int slices = 128;
    private double radius = width;
    private float incr = (float) (2 * Math.PI / slices);
    public boolean isDirChanged = false;

    public Ball(double x, double y, double radius)
    {
        super(x, y, radius, radius);
        this.radius = radius;
    }

    @Override
    public void draw()
    {
        glLoadIdentity();
        glBegin(GL_TRIANGLE_FAN);

        glColor3d(1, 1, 1);
        glVertex2d(this.x, this.y);
        glColor3d(0.7, 0.7, 0.7);

        for (int i = 0; i < slices; i++)
        {
            float angle = incr * i;

            float X = (float) Math.cos(angle) * (float) radius;
            float Y = (float) Math.sin(angle) * (float) radius;

            glVertex2d(this.x + X, this.y + Y);
        }
        glVertex2d(this.x + (double) radius, this.y);
        glEnd();
    }

    @Override
    public void setDX(double dx)
    {
        if (this.dx != dx)
        {
            isDirChanged = true;
        }
        // TODO Auto-generated method stub
        super.setDX(dx);
    }

    @Override
    public void setDY(double dy)
    {
        if (this.dy != dy)
        {
            isDirChanged = true;
        }
        // TODO Auto-generated method stub
        super.setDY(dy);
    }

    @Override
    public void update(int delta)
    {
        super.update(delta);

        if (y < 1 + radius + dy)
        {
            y = 1 + radius + dy;
            dy = -dy;
            isDirChanged = true;
        }
        else if (y > Display.getHeight() - radius - dy)
        {
            y = Display.getHeight() - radius - dy;
            dy = -dy;
            isDirChanged = true;
        }
    }
}
