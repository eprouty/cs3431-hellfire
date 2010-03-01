package server;

import java.io.*;
import java.net.*;
import java.util.*;

public class Server 
{
	public static void main(String args[])
	{
		String data = "Toobie ornaught toobie";
		try 
		{
			ServerSocket server = new ServerSocket(8088);
			Socket socket = server.accept();
			System.out.print("Server has connected!\n");
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			System.out.print("Sending string: '" + data + "'\n");
			out.print(data);
			out.close();
			socket.close();
			server.close();
		}
		catch(Exception e) 
		{
			System.out.print("Whoops! It didn't work!\n");
		}
	}
}
