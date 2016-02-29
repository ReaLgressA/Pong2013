package engine.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

public class TCP_Server extends Thread
{

    private static ServerSocket serverSocket = null;
    private static Socket clientSocket = null;
    private static Vector<TCP_Message> Inbox = new Vector<TCP_Message>();

    public boolean stop = false;

    private PrintWriter out = null;
    private BufferedReader in = null;

    public TCP_Server(int port)
    {
        try
        {
            serverSocket = new ServerSocket(port);
        }
        catch (IOException e)
        {

            System.err.println(e.getMessage());
        }
    }

    public void run()
    {
        Inbox.clear();
        try
        {
            System.out.println("[S]Waiting for connection...");
            clientSocket = serverSocket.accept();//wait for client 
            System.out.println("[S]Connection successful");
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String Input;
            while (!clientSocket.isClosed() && !stop)
            {
                Input = in.readLine();
                if (Input == null)
                {
                    Shutdown();
                    Inbox.add(new TCP_Message("NET_ER", "end of stream reached"));
                    return;
                }
                String msg[] = Input.split("\\|", 2);
                if (msg[1] != null)
                {
                    Inbox.add(new TCP_Message(msg[0], msg[1]));
                }
            }
            return;
        }
        catch (IOException e)
        {
            System.err.println(e.getMessage());
            Inbox.add(new TCP_Message("SYS_EX", e.getMessage()));
        }
    }

    public void Shutdown()
    {
        stop = true;
        try
        {
            if (out != null)
            {
                out.close();
            }
            if (in != null)
            {
                in.close();
            }
            if (serverSocket != null)
            {
                if (!serverSocket.isClosed())
                {
                    serverSocket.close();
                }
                serverSocket = null;
            }
            if (clientSocket != null)
            {
                if (!clientSocket.isClosed())
                {
                    clientSocket.close();
                }
                clientSocket = null;
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        System.out.println("Server shutdown");
    }

    public void SendMessage(String cmd, String data)
    {
        out.println(cmd + "|" + data);
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
