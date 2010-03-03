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
		String username="", password, input;
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
				System.out.println("You are now logged in.");
				loggedIn=true;
			}
			else
			{
				System.out.println("Incorrect username or password. Please try again.");
			}
		}
		if (loggedIn)
		{
			ArrayList<String> characters=new ArrayList<String>();
			result=query.executeQuery("SELECT * FROM playerCharacter WHERE username='"+username+"';");
			System.out.println("0: Create a new character");
			int i=0;
			while (result.next())
			{
				i++;
				characters.add(result.getString(1));
				System.out.println(i+": Play as "+characters.get(i-1));
			}
			System.out.print("Enter your selection: ");
			input=prompt.nextLine();
			boolean invalidSelection=true;
			int choice=-1;
			while (invalidSelection)
			{
				try
				{
					choice=Integer.parseInt(input);
				}
				catch (NumberFormatException e)
				{
					System.out.println("Invalid selection. Please try again.");
				}
				if (!(choice<0 || choice>i))
				{
					invalidSelection=false;
					break;
				}
				System.out.print("Enter your selection: ");
				input=prompt.nextLine();
			}
			String character="", command;
			int charX, charY;
			if (choice!=0)
			{
				character=characters.get(choice-1);
				System.out.println("You are now playing as "+characters.get(choice-1));
			}
			else
			{
				System.out.print("Enter your character's name: ");
				input=prompt.nextLine();
				query.executeUpdate("INSERT INTO playerCharacter VALUES ('"+input+"',1,1,10,'"+username+"',0,0);");
				character=input;
				
			}
			input="look";
			while(true)
			{
				result=query.executeQuery("SELECT * FROM playerCharacter WHERE name='"+character+"';");
				result.next();
				charX=result.getInt(6);
				charY=result.getInt(7);
				command=input;
				if (command.startsWith("look"))
				{
					result=query.executeQuery("SELECT * FROM area WHERE x="+charX+" AND y="+charY+";");
					result.next();
					System.out.println(result.getString(4));
				}
				else if (command.startsWith("who"))
				{
					result=query.executeQuery("SELECT * FROM NPC WHERE areaX="+charX+" AND areaY="+charY+";");
					System.out.println("The following people are located here:");
					while (result.next())
					{
						System.out.println(result.getString(4));
					}
				}
				else if (command.startsWith("inventory"))
				{
					result=query.executeQuery("SELECT * FROM inventory WHERE playerName='"+character+"';");
					System.out.println("You have the following items in your inventory:");
					while (result.next())
					{
						System.out.print(result.getString(4)+" "+result.getString(2));
						if (result.getInt(3)==1)
						{
							System.out.print(" (equipped)");
						}
					}
					System.out.println();
				}
				else if (command.startsWith("status"))
				{
					result=query.executeQuery("SELECT * FROM playerCharacter WHERE name='"+character+"';");
					System.out.println("Your current status:");
					while (result.next())
					{
						System.out.println("ATK: "+result.getString(2));
						System.out.println("DEF: "+result.getString(3));
						System.out.println("Health: "+result.getString(4));
					}
				}
				else if (command.startsWith("exit") || command.startsWith("quit") || command.startsWith("logout"))
				{
					break;
				}
				else if (command.startsWith("talk to"))
				{
					String person=command.replace("talk to ","");
					result=query.executeQuery("SELECT * FROM NPC WHERE name='"+person+"';");
					if (!result.next())
					{
						System.out.println("That person is not here.");
					}
					else System.out.println(result.getString(5));
				}
				else if (command.startsWith("attack"))
				{
					String person=command.replace("attack ","");
					result=query.executeQuery("SELECT * FROM NPC JOIN enemy ON NPC.ID=enemy.ID WHERE name='"+person+"';");
					if (!result.next())
					{
						System.out.println("That is not an enemy.");
					}
					else
					{
						System.out.println("You attack "+person+"!");
						double playerHealth, playerATK, playerDEF, enemyHealth, enemyATK, enemyDEF;
						enemyHealth=result.getInt(7);
						enemyATK=result.getInt(8);
						enemyDEF=result.getInt(9);
						result=query.executeQuery("SELECT * FROM playerCharacter WHERE name='"+character+"';");
						result.next();
						playerHealth=result.getInt(4);
						playerATK=result.getInt(2);
						playerDEF=result.getInt(3);
						while (enemyHealth*playerHealth!=0)
						{
							double percent=Math.random();
							if (percent<.15)
							{
								System.out.println("You missed.");
							}
							else if (percent<.95)
							{
								System.out.println("You hit for "+(int)Math.max((percent+0.2)*playerATK-enemyDEF,0)+" damage.");
								enemyHealth-=(int)Math.max((percent+0.2)*playerATK-enemyDEF,0);
								if (enemyHealth<=0) 
								{
									System.out.println("You vanquished your foe!");
									break;
								}
							}
							else
							{
								System.out.println("You made a critical hit for "+(int)Math.max(2*playerATK-enemyDEF,0)+" damage.");
								enemyHealth-=(int)Math.max(2*playerATK-enemyDEF,0);
								if (enemyHealth<=0)
								{
									System.out.println("You vanquished your foe!");
									break;
								}
							}
							percent=Math.random();
							if (percent<.15)
							{
								System.out.println("Your opponent missed.");
							}
							else if (percent<.95)
							{
								System.out.println("Your opponent hit for "+(int)Math.max((percent+0.2)*enemyATK-playerDEF,0)+" damage.");
								playerHealth-=(int)Math.max((percent+0.2)*enemyATK-playerDEF,0);
								if (playerHealth<=0) 
								{
									System.out.println("You were defeated.");
									break;
								}
							}
							else
							{
								System.out.println("Your opponent made a critical hit for "+(int)Math.max(2*enemyATK-playerDEF,0)+" damage.");
								playerHealth-=(int)Math.max(2*enemyATK-playerDEF,0);
								if (playerHealth<=0) 
								{
									System.out.println("You were defeated.");
									break;
								}
							}
						}
					}
					
				}
				else if (command.startsWith("equip"))
				{
					String item=command.replace("equip ","");
					result=query.executeQuery("SELECT * FROM inventory WHERE playerName='"+character+"' AND itemName='"+item+"';");
					if (!result.next())
					{
						System.out.println("You do not have that item in your inventory.");
					}
					else if (result.getInt(3)==1)
					{
						System.out.println("You already have that item equipped.");
					}
					else
					{
						result=query.executeQuery("SELECT * FROM item WHERE name='"+item+"';");
						result.next();
						int attack, defense;
						attack=result.getInt(3);
						defense=result.getInt(4);
						result=query.executeQuery("SELECT * FROM playerCharacter WHERE name='"+character+"';");
						result.next();
						attack+=result.getInt(2);
						defense+=result.getInt(3);
						query.executeUpdate("UPDATE playerCharacter SET attack="+attack+", defense="+defense+" WHERE name='"+character+"';");
						query.executeUpdate("UPDATE inventory SET isEquipped=1 WHERE playerName='"+character+"' AND itemName='"+item+"';");
						System.out.println("Item equipped.");
					}
				}
				else if (command.startsWith("unequip"))
				{
					String item=command.replace("unequip ","");
					result=query.executeQuery("SELECT * FROM inventory WHERE playerName='"+character+"' AND itemName='"+item+"';");
					if (!result.next())
					{
						System.out.println("You do not have that item in your inventory.");
					}
					else if (result.getInt(3)==0)
					{
						System.out.println("You do not have that item equipped.");
					}
					else
					{
						result=query.executeQuery("SELECT * FROM item WHERE name='"+item+"';");
						result.next();
						int attack, defense;
						attack=result.getInt(3);
						defense=result.getInt(4);
						result=query.executeQuery("SELECT * FROM playerCharacter WHERE name='"+character+"';");
						result.next();
						attack=result.getInt(2)-attack;
						defense=result.getInt(3)-defense;
						query.executeUpdate("UPDATE playerCharacter SET attack="+attack+", defense="+defense+" WHERE name='"+character+"';");
						query.executeUpdate("UPDATE inventory SET isEquipped=0 WHERE playerName='"+character+"' AND itemName='"+item+"';");
						System.out.println("Item unequipped.");
					}
				}
				else
				{
					System.out.println("I don't know what that means.");
				}
				input=prompt.nextLine();
			}
		}
		connection.close();
		System.out.println("Goodbye.");
	}
}
