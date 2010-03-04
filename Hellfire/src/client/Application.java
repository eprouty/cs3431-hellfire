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
			result=query.executeQuery("SELECT password FROM account WHERE username='"+username+"';");
			
			//result.next() gets the next row in the result set. if it's false on the first call, 
			//no rows were returned 
			if (result.next()==false)
			{
				System.out.println("Incorrect username or password. Please try again.");
				continue;
			}
			
			//the result is in the form USERNAME PASSWORD ADMIN, so we compare what the user put in 
			//for the password to what is in the database. if they match, 
			if (result.getString("password").equals(password))
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
			//once the player is logged in, give them a menu to choose which of their characters they
			//want to play, or allow them to make a new character if they want
			ArrayList<String> characters=new ArrayList<String>();
			result=query.executeQuery("SELECT name FROM playerCharacter WHERE username='"+username+"';");
			System.out.println("0: Create a new character");
			int i=0;
			while (result.next())
			{
				i++;
				characters.add(result.getString("name"));
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
				while(true)
				{
					System.out.print("Enter your character's name: ");
					input=prompt.nextLine();
					result=query.executeQuery("SELECT * FROM playerCharacter WHERE name='"+input+"';");
					if (!result.next())
					{
						query.executeUpdate("INSERT INTO playerCharacter VALUES ('"+input+"',1,1,10,'"+username+"',0,0);");
						break;
					}
					else
					{
						System.out.println("That name is already in use.");
					}
				}
				character=input;
				
			}
			
			//now that they've chosen the character, start the main loop
			input="look";
			while(true)
			{
				result=query.executeQuery("SELECT x,y FROM playerCharacter WHERE name='"+character+"';");
				result.next();
				charX=result.getInt("x");
				charY=result.getInt("y");
				command=input;
				if (command.startsWith("look"))
				{
					result=query.executeQuery("SELECT description FROM area WHERE x="+charX+" AND y="+charY+";");
					result.next();
					System.out.println(result.getString("description"));
				}
				else if (command.startsWith("who"))
				{
					result=query.executeQuery("SELECT name FROM NPC WHERE areaX="+charX+" AND areaY="+charY+";");
					System.out.println("The following people are located here:");
					while (result.next())
					{
						System.out.println(result.getString("name"));
					}
				}
				else if (command.startsWith("inventory"))
				{
					result=query.executeQuery("SELECT itemName,isEquipped,quantity FROM inventory WHERE playerName='"+character+"';");
					System.out.println("You have the following items in your inventory:");
					while (result.next())
					{
						System.out.print(result.getString("quantity")+" "+result.getString("itemName"));
						if (result.getInt("isEquipped")==1)
						{
							System.out.print(" (equipped)");
						}
					}
					System.out.println();
				}
				else if (command.startsWith("inspect"))
				{
					String item = command.replace("inspect ", "");
					result = query.executeQuery("SELECT description, ATKModifier, DEFModifier FROM inventory JOIN item ON inventory.itemName = item.name WHERE itemName = '"+item+"'");
					if (!result.next()){
						System.out.println("You cannot inspect this item.");
					} else {
						System.out.println(result.getString("description"));
						System.out.println("Attack: " + result.getString("ATKModifier"));
						System.out.println("Defense: " + result.getString("DEFModifier"));
					}
				}
				else if (command.startsWith("take"))
				{
					String item = command.replace("take ", "");
					result = query.executeQuery("SELECT x, y FROM playerCharacter WHERE name = '"+character+"'");
					result.next();
					int x = result.getInt("x");
					int y = result.getInt("y");
					int areaQuantity = 1;
					result = query.executeQuery("SELECT itemName, quantity FROM areaItems WHERE areaX = "+x+" AND areaY="+y+" AND itemName = '"+item+"'");
					if (!result.next()){
						System.out.println("That item is not available to pick up.");
					} else {
						areaQuantity = result.getInt("quantity");
						result = query.executeQuery("SELECT itemName FROM inventory WHERE playerName = '"+character+"' AND itemName = '"+item+"'");
						if (!result.next()){
							query.executeUpdate("INSERT INTO inventory VALUES ('"+character+"', '"+item+"', 0, 1)");
						} else {
							result = query.executeQuery("SELECT quantity FROM inventory WHERE playerName = '"+character+"' AND itemName = '"+item+"'");
							result.next();
							int quantity = result.getInt("quantity");
							query.executeUpdate("UPDATE inventory SET quantity = "+(quantity + 1)+" WHERE playerName = '"+character+"' AND itemName = '"+item+"'");
						}
						areaQuantity--;
						if (areaQuantity < 1){
							query.executeUpdate("DELETE FROM areaItems WHERE areaX = "+x+" AND areaY="+y+" AND itemName = '"+item+"'");
						} else {
							query.executeUpdate("UPDATE areaItems SET quantity = "+(areaQuantity - 1)+" WHERE areaX = "+x+" AND areaY="+y+" AND itemName = '"+item+"'");
						}
						System.out.println("You picked up an "+item+"!");
					}
				}
				else if (command.startsWith("status"))
				{
					result=query.executeQuery("SELECT attack,defense,health FROM playerCharacter WHERE name='"+character+"';");
					System.out.println("Your current status:");
					while (result.next())
					{
						System.out.println("ATK: "+result.getString("attack"));
						System.out.println("DEF: "+result.getString("defense"));
						System.out.println("Health: "+result.getString("health"));
					}
				}
				else if (command.startsWith("exit") || command.startsWith("quit") || command.startsWith("logout"))
				{
					break;
				}
				else if (command.startsWith("talk to"))
				{
					String person=command.replace("talk to ","");
					result=query.executeQuery("SELECT dialog FROM NPC WHERE name='"+person+"';");
					if (!result.next())
					{
						System.out.println("That person is not here.");
					}
					else System.out.println(result.getString("dialog"));
				}
				else if (command.startsWith("attack"))
				{
					String person=command.replace("attack ","");
					result=query.executeQuery("SELECT NPC.name,enemy.health,enemy.attack,enemy.defense FROM NPC JOIN enemy ON NPC.ID=enemy.ID WHERE name='"+person+"';");
					if (!result.next())
					{
						System.out.println("That is not an enemy.");
					}
					else
					{
						System.out.println("You attack "+person+"!");
						double playerHealth, playerATK, playerDEF, enemyHealth, enemyATK, enemyDEF;
						enemyHealth=result.getInt("enemy.health");
						enemyATK=result.getInt("enemy.attack");
						enemyDEF=result.getInt("enemy.defense");
						result=query.executeQuery("SELECT health,attack,defense FROM playerCharacter WHERE name='"+character+"';");
						result.next();
						playerHealth=result.getInt("health");
						playerATK=result.getInt("attack");
						playerDEF=result.getInt("defense");
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
					result=query.executeQuery("SELECT itemName,isEquipped FROM inventory WHERE playerName='"+character+"' AND itemName='"+item+"';");
					if (!result.next())
					{
						System.out.println("You do not have that item in your inventory.");
					}
					else if (result.getInt("isEquipped")==1)
					{
						System.out.println("You already have that item equipped.");
					}
					else
					{
						result=query.executeQuery("SELECT ATKModifier,DEFModifier FROM item WHERE name='"+item+"';");
						result.next();
						int attack, defense;
						attack=result.getInt("ATKModifier");
						defense=result.getInt("DEFModifier");
						result=query.executeQuery("SELECT attack,defense FROM playerCharacter WHERE name='"+character+"';");
						result.next();
						attack+=result.getInt("attack");
						defense+=result.getInt("defense");
						query.executeUpdate("UPDATE playerCharacter SET attack="+attack+", defense="+defense+" WHERE name='"+character+"';");
						query.executeUpdate("UPDATE inventory SET isEquipped=1 WHERE playerName='"+character+"' AND itemName='"+item+"';");
						System.out.println("Item equipped.");
					}
				}
				else if (command.startsWith("unequip"))
				{
					String item=command.replace("unequip ","");
					result=query.executeQuery("SELECT itemName,isEquipped FROM inventory WHERE playerName='"+character+"' AND itemName='"+item+"';");
					if (!result.next())
					{
						System.out.println("You do not have that item in your inventory.");
					}
					else if (result.getInt("isEquipped")==0)
					{
						System.out.println("You do not have that item equipped.");
					}
					else
					{
						result=query.executeQuery("SELECT ATKModifier,DEFModifier FROM item WHERE name='"+item+"';");
						result.next();
						int attack, defense;
						attack=result.getInt("ATKModifier");
						defense=result.getInt("DEFModifier");
						result=query.executeQuery("SELECT attack,defense FROM playerCharacter WHERE name='"+character+"';");
						result.next();
						attack=result.getInt("attack")-attack;
						defense=result.getInt("defense")-defense;
						query.executeUpdate("UPDATE playerCharacter SET attack="+attack+", defense="+defense+" WHERE name='"+character+"';");
						query.executeUpdate("UPDATE inventory SET isEquipped=0 WHERE playerName='"+character+"' AND itemName='"+item+"';");
						System.out.println("Item unequipped.");
					}
				}
				else if (command.startsWith("admin"))
				{
					result=query.executeQuery("SELECT admin FROM account WHERE username='"+username+"';");
					result.next();
					if (result.getInt("admin")==1)
					{
						String uName, pass, charName;
						int admin;
						boolean loop=true;
						while (loop)
						{
							System.out.println("Admin Menu");
							System.out.println("--------------------");
							System.out.println("1. Create Account");
							System.out.println("2. Modify Account");
							System.out.println("3. Delete Account");
							System.out.println("4. List All Accounts");
							System.out.println("5. Create Character");
							System.out.println("6. Modify Character");
							System.out.println("7. Delete Character");
							System.out.println("8. List All Characters");
							System.out.println("9. Exit Admin Menu");
							System.out.print("> ");
							input=prompt.nextLine();
							choice=Integer.parseInt(input);
							switch (choice)
							{
							case 1:
								while (true)
								{
									System.out.print("Enter the username: ");
									uName=prompt.nextLine();
									result=query.executeQuery("SELECT username FROM account WHERE username='"+uName+"';");
									if (!result.next()) break;
									else System.out.println("That username is already in use.");
								}
								System.out.print("Enter the password: ");
								pass=prompt.nextLine();
								System.out.print("Is the user an admin (1=yes, 0=no)? ");
								admin=Integer.parseInt(prompt.nextLine());
								query.executeUpdate("INSERT INTO account VALUES ('"+uName+"','"+pass+"',"+admin+");");
								System.out.println("Account created.");
								break;
							case 2:
								while (true)
								{
									System.out.print("Enter the username to modify: ");
									uName=prompt.nextLine();
									result=query.executeQuery("SELECT username FROM account WHERE username='"+uName+"';");
									if (result.next()) break;
									else System.out.println("There is no user by that name.");
								}
								System.out.print("Enter the password (or leave blank to not change): ");
								pass=prompt.nextLine();
								System.out.print("Is the user an admin (1=yes, 0=no)? ");
								admin=Integer.parseInt(prompt.nextLine());
								if (!pass.equals(""))
								{
									query.executeUpdate("UPDATE account SET password='"+pass+"',admin="+admin+" WHERE username='"+uName+"';");
								}
								else
								{
									query.executeUpdate("UPDATE account SET admin="+admin+" WHERE username='"+uName+"';");
								}
								System.out.println("Account modified.");
								break;
							case 3:
								while (true)
								{
									System.out.print("Enter the username to delete: ");
									uName=prompt.nextLine();
									result=query.executeQuery("SELECT username FROM account WHERE username='"+uName+"';");
									if (result.next()) break;
									else System.out.println("There is no user by that name.");
								}
								query.executeUpdate("DELETE FROM playerCharacter WHERE username='"+uName+"';");
								query.executeUpdate("DELETE FROM account WHERE username='"+uName+"';");
								System.out.println("Account deleted.");
								break;
							case 4:
								result=query.executeQuery("SELECT * FROM account;");
								System.out.println("username, password, admin");
								while (result.next())
								{
									System.out.println(result.getString("username")+", "+result.getString("password")+", "+result.getString("admin"));
								}
								break;
							case 5:
								break;
							case 6:
								break;
							case 7:
								break;
							case 8:
								break;
							case 9:
								loop=false;
								break;
							}
						}
					}
					else
					{
						System.out.println("You are not an admin.");
					}
				}
				else
				{
					System.out.println("I don't know what that means.");
				}
				System.out.print("> ");
				input=prompt.nextLine();
			}
		}
		connection.close();
		System.out.println("Goodbye.");
	}
}
