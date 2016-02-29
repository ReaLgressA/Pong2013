package engine.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Vector;

public class TCP_Client extends Thread
{

    private static Vector<TCP_Message> Inbox = new Vector<TCP_Message>();
    private int PORT;
    private String IP;
    public boolean stop = false;

    private Socket socket = null;
    private PrintWriter out = null;
    private BufferedReader in = null;

    public TCP_Client(String ip, int port)
    {
        this.PORT = port;
        this.IP = ip;
    }

    public String Connect()
    {
        try
        {
            System.out.println("[C]Attemping to connect to " + IP + " : " + PORT);
            socket = new Socket(IP, PORT);
            System.out.println("[C]Connected");
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            return null;
        }
        catch (UnknownHostException e)
        {
            System.err.println(e.getMessage());
            return e.getMessage();
        }
        catch (IOException e)
        {
            System.err.println(e.getMessage());
            return e.getMessage();
        }
    }

    public void SendMessage(String cmd, String data)
    {
        out.println(cmd + "|" + data);
    }

    public void run()
    {
        Inbox.clear();
        System.out.println("[C]Run client thread");
        String message;
        try
        {
            while (!socket.isClosed() && !stop)
            {
                message = in.readLine();
                if (message == null)
                {
                    Disconnect();
                    Inbox.add(new TCP_Message("NET_ER", "end of stream reached"));
                    return;
                }
                String msg[] = message.split("\\|", 2);
                if (msg[1] != null)
                {
                    Inbox.add(new TCP_Message(msg[0], msg[1]));
                }
                else
                {
                    System.out.println("Package corrupted. Null data");
                }
            }
            Disconnect();
            return;
        }
        catch (IOException e)
        {
            e.printStackTrace();
            Inbox.add(new TCP_Message("SYS_EX", e.getMessage()));
        }
    }

    public void Disconnect()
    {
        try
        {
            out.close();
            in.close();
            if (!socket.isClosed())
            {
                socket.close();
            }
            System.out.println("[C]Disconnected succesfully");
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public boolean isConnected()
    {
        if (socket != null)
        {
            return socket.isConnected();
        }
        else
        {
            return false;
        }
    }

    public TCP_Message GetMessage()
    {
        if (Inbox.isEmpty())
        {
            return null;
        }
        else
        {
            TCP_Message msg = Inbox.get(0);
            Inbox.remove(0);
            return msg;
        }
    }
}
