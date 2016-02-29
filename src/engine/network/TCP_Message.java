package engine.network;

public class TCP_Message
{

    public String cmd, data;

    public TCP_Message(String cmd, String data)
    {
        this.cmd = cmd;
        this.data = data;
    }
}
