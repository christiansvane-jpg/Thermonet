package Server;

import Shared.ControlCommand;
import Shared.HeatPumpData;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable
{
  private final Socket socket;
  private final ThermonetServer server;
  private String clientId = "UNKNOWN";

  public ClientHandler(Socket socket, ThermonetServer server)
  {
    this.socket = socket;
    this.server = server;
  }

  @Override
  public void run()
  {
    try
    {
      BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

      String line;
      while ((line = in.readLine()) != null)
      {
        HeatPumpData data;
        try
        {
          data = HeatPumpData.parse(line);
        }
        catch (IllegalArgumentException e)
        {
          out.println("ERROR");
          continue;
        }

        clientId = data.getClientId();
        server.updateClientData(clientId, data);

        ControlCommand command = server.getCommandFor(clientId);
        out.println(command.name());
      }
    }
    catch (IOException e)
    {
      System.out.println("[Server] Client disconnected: " + clientId);
    }
    finally
    {
      server.removeClient(clientId);
      try { socket.close(); } catch (IOException ignored) {}
    }
  }
}
