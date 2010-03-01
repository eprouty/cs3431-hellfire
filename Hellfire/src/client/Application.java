package client;
import java.sql.*;
import java.util.*;

public class Application
{
	public static void main(String[] args) throws Exception 
	{  
		//connect to the database
		Class.forName ("com.mysql.jdbc.Driver").newInstance();
		Connection connection = DriverManager.getConnection("jdbc:mysql://mysql.wpi.edu/hellfire","mgheorghe","nu9W8Q");
		
		//create new scanner for reading user input
		Scanner prompt=new Scanner(System.in);
		
		//more initialization
		String username, password;
		Statement query;
		query=connection.createStatement();
		ResultSet result=null;
		boolean loggedIn=false;
		
		System.out.println("Welcome to Hellfire. New account registration is currently disabled.");
		
		//keep looping until either:
		//	user successfully logs in
		//	user types in exit or quit
		while (!loggedIn)
		{
			//prompt user for login details
			System.out.print("Username ('exit' to end the program): ");
			username=prompt.nextLine();
			if (username.equalsIgnoreCase("exit") || username.equalsIgnoreCase("quit"))
			{
				loggedIn=false;
				break;
			}
			System.out.print("Password: ");
			password=prompt.nextLine();
			System.out.println("Logging in...");
			
			//execute the following SQL query (it just returns all rows where the username equals
			//what the user typed in), store the results in 'result'
			result=query.executeQuery("SELECT * FROM account WHERE username='"+username+"';");
			
			//result.next() gets the next row in the result set. if it's false on the first call, 
			//no rows were returned 
			if (result.next()==false)
			{
				System.out.println("Incorrect username or password. Please try again.");
				continue;
			}
			
			//the result is in the form USERNAME PASSWORD ADMIN, so we compare what the user put in 
			//for the password to what is in the database. if they match, 
			if (result.getString(2).equals(password))
			{
				System.out.println("You are now logged in. Unfortunately, there is nothing to do at the moment.");
				loggedIn=true;
			}
			else
			{
				System.out.println("Incorrect username or password. Please try again.");
			}
		}
		if (loggedIn)
		{
			//result=query.executeQuery("SELECT * FROM account WHERE username='"+username+"';");
		}
		connection.close();
		System.out.println("Goodbye.");
	}
}