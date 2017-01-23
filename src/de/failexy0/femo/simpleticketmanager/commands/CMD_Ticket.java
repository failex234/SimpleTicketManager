package de.failexy0.femo.simpleticketmanager.commands;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.failexy0.femo.simpleticketmanager.enums.Language;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import de.failexy0.femo.simpleticketmanager.Main;
import de.failexy0.femo.simpleticketmanager.enums.Messages;
import de.failexy0.femo.simpleticketmanager.sender.ServerMessageSender;
import mkremins.fanciful.FancyMessage;

public class CMD_Ticket implements CommandExecutor {

	ServerMessageSender sms = new ServerMessageSender();

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		Player p = (Player) sender;
		if (cmd.getName().equalsIgnoreCase("ticket")) {
			// If no args are given
			if (args.length == 0) {
				if (!p.hasPermission("ticket.list")) {
					p.sendMessage(Messages.PREFIX.getMSG() + Messages.STM_NOPERM.getMSG());
				} else if (!p.hasPermission("ticket.review")) {
					p.sendMessage(Messages.PREFIX.getMSG() + Messages.STM_LISTPERM.getMSG());
				} else {
					p.sendMessage(Messages.PREFIX.getMSG() + Messages.STM_FULLPERM.getMSG());
				}
				// if the first arg is "compose"
			} else if (args[0].equalsIgnoreCase("compose")) {
				if (args.length == 1) {
					p.sendMessage(Messages.PREFIX.getMSG() + Messages.STM_MSGREQUIRED.getMSG());
				} else {
					Statement statement;
					try {
						// If a ticket with the senders name is found in the db,
						// the sender is not able to compose another ticket.
						statement = Main.main.c.createStatement();
						ResultSet res = statement
								.executeQuery("select * from ticketsystem where UUID = '" + p.getUniqueId() + "';");
						res.next();
						// This string just gets used to trigger an exception,
						// if the query is empty
						String test = res.getString("Name");
						p.sendMessage(Messages.PREFIX.getMSG() + Messages.STM_ALREADYCOMPOSED.getMSG());
					} catch (SQLException e) {
						// If sender is not found in db, an exception gets
						// caught and the ticket gets saved in the db.
						String myString = "";
						// loop args from first one to last one to make a String
						// out of all
						for (int i = 1; i < args.length; i++) {
							String arg = args[i] + " ";
							myString = myString + arg;
						}

						Collection<? extends Player> onlinePlayers1 = Bukkit.getServer().getOnlinePlayers();
						Player Cpl;

						// Send a ticket report to all online players that have
						// the permission "ticket.review"
						Bukkit.broadcast(ChatColor.RED + "----------" + ChatColor.YELLOW + "Ticket System"
								+ ChatColor.RED + "----------", "ticket.review");
						Bukkit.broadcast(
								ChatColor.BOLD + Messages.STM_TICKETBY.getMSG() + ChatColor.YELLOW + p.getName(),
								"ticket.review");
						Bukkit.broadcast(
								ChatColor.BOLD + "" + ChatColor.RED + "UUID: " + ChatColor.YELLOW + p.getUniqueId(),
								"ticket.review");
						Bukkit.broadcast(Messages.STM_TICKETMSG.getMSG() + ChatColor.YELLOW + myString,
								"ticket.review");
						Bukkit.broadcast(
								ChatColor.RED + "Server: " + ChatColor.YELLOW + Main.main.getServer().getServerName(),
								"ticket.review");
						Bukkit.broadcast(ChatColor.RED + "----------" + ChatColor.YELLOW + "Ticket System"
								+ ChatColor.RED + "----------", "ticket.review");
						for (int i = 0; i < onlinePlayers1.size(); i++) {
							Cpl = (Player) onlinePlayers1.toArray()[i];
							if (Cpl.hasPermission("ticket.review")) {
								new FancyMessage(Messages.STM_CLICKTOREVIEW.getMSG()).color(ChatColor.GREEN)
										.tooltip(Messages.STM_CLICKTOREVIEWTOOLTIP.getMSG())
										.command("/ticket review " + p.getUniqueId()).send(Cpl);
							}
						}
						// One measure to prevent SQL Injection even though you
						// can't really do something.
						myString = myString.replace("'", "{SEMICOLON}");
						sms.BungeeMessageSender(sender, p.getName(), p.getUniqueId().toString(), myString,
								Main.main.getServer().getServerName());
						// Get Date and Time for later use with db
						DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
						Calendar cal = Calendar.getInstance();
						String datum = dateFormat.format(cal.getTime());
						try {
							// Save to MySQL Table Code here, Save Username,
							// Message, time and server
							statement = Main.main.c.createStatement();
							statement.executeUpdate(
									"INSERT INTO ticketsystem (`Name`, `UUID`, `Nachricht`, `srv`, `datum`) VALUES ('"
											+ p.getName() + "', '" + p.getUniqueId() + "', '" + myString + "', '"
											+ Main.main.getServer().getServerName() + "', '" + datum + "');");
							try {
								statement = Main.main.c.createStatement();
								ResultSet res = statement.executeQuery(
										"select * from ticketsystem_player where UUID = '" + p.getUniqueId() + "';");
								res.next();
								String test_pl = res.getString("Name");
								int anzahl = res.getInt("Anzahl_Tickets");
								anzahl += 1;
								statement.executeUpdate("UPDATE `ticketsystem_player` SET Name = '" + p.getName()
										+ "', UUID = '" + p.getUniqueId() + "', Letztes_Ticket = '" + datum
										+ "', Anzahl_Tickets = '" + anzahl + "' WHERE UUID = '" + p.getUniqueId()
										+ "';");
							} catch (SQLException eSQL) {
								statement.executeUpdate(
										"INSERT INTO ticketsystem_player (`Name`, `UUID`, `Letztes_Ticket`, `Anzahl_Tickets`) VALUES ('"
												+ p.getName() + "','" + p.getUniqueId() + "','" + datum + "', 1);");
							}
							p.sendMessage(Messages.PREFIX.getMSG() + Messages.STM_TICKETCOMPOSED.getMSG());
						} catch (SQLException f) {
							f.printStackTrace();
						}
					}
					// Done with saving to database
				}
				// Check if first arg is "review"
			} else if (args[0].equalsIgnoreCase("review")) {
				if (p.hasPermission("ticket.review") && args.length == 1) {
					p.sendMessage(Messages.PREFIX.getMSG() + Messages.STM_PLAYERIDMISSING.getMSG());
				} else if (p.hasPermission("ticket.review")) {
					// Execute if sender has the permission node "ticket.review"
					// retrieve from MySQL Table Code here, retrieve Username,
					// Message, time and server
					Statement statement;
					try {
						statement = Main.main.c.createStatement();
						ResultSet res = statement
								.executeQuery("select * from ticketsystem where Name LIKE '%" + args[1] + "%';");
						res.next();
						Main.main.Name = res.getString("Name");
						String msg = res.getString("Nachricht").replace("{SEMICOLON}", "'");
						String UUIDD = res.getString("UUID");
						p.sendMessage(ChatColor.RED + "----------" + ChatColor.YELLOW + "Ticket System" + ChatColor.RED
								+ "----------");
						p.sendMessage(Messages.STM_TICKETBY.getMSG() + ChatColor.YELLOW + res.getString("Name"));
						p.sendMessage(ChatColor.RED + "UUID: " + ChatColor.YELLOW + UUIDD);
						p.sendMessage(Messages.STM_TICKETMSG.getMSG() + ChatColor.YELLOW + msg);
						p.sendMessage(Messages.STM_TICKETDATE.getMSG() + ChatColor.YELLOW + res.getString("datum"));
						p.sendMessage(ChatColor.RED + "Server: " + ChatColor.YELLOW + res.getString("srv"));
						p.sendMessage(ChatColor.RED + "----------" + ChatColor.YELLOW + "Ticket System" + ChatColor.RED
								+ "----------");
						statement
								.executeUpdate("DELETE FROM ticketsystem WHERE UUID = '" + res.getString("UUID") + "'");
						p.sendMessage(Messages.PREFIX.getMSG() + Messages.STM_TICKETDELETEDSUCCESS.getMSG());
						Bukkit.broadcast(
								Messages.PREFIX.getMSG() + Messages.STM_TICKETREVIEWEDSTAFF.getMSG()
										.replace("{reviewer}", p.getName().replace("{user}", Main.main.Name)),
								"ticket.review");
						UUID ui = UUID.fromString(UUIDD);
						String pl = Bukkit.getPlayer(ui).getName();
						Player pll = Bukkit.getServer().getPlayer(pl);
						pll.sendMessage(Messages.PREFIX.getMSG()
								+ Messages.STM_TICKETREVIEWEDUSER.getMSG().replace("{reviewer}", p.getName()));
						// statement.executeUpdate("UPDATE `ticketsystem` SET
						// `reviewed` = null WHERE Name = '"+ args[1] +"'");
						// Sends a message to all online players that have the
						// permission "ticket.review"
					} catch (NullPointerException np) {
					} catch (SQLException e) {
						try {
							statement = Main.main.c.createStatement();
							ResultSet res2 = statement
									.executeQuery("SELECT * FROM ticketsystem WHERE UUID = '" + args[1] + "';");
							res2.next();
							String Name1 = res2.getString("Name");
							String msg = res2.getString("Nachricht").replace("{SEMICOLON}", "'");
							p.sendMessage(ChatColor.RED + "----------" + ChatColor.YELLOW + "Ticket System"
									+ ChatColor.RED + "----------");
							p.sendMessage(Messages.STM_TICKETBY.getMSG() + ChatColor.YELLOW + res2.getString("Name"));
							p.sendMessage(ChatColor.RED + "UUID: " + ChatColor.YELLOW + res2.getString("UUID"));
							p.sendMessage(Messages.STM_TICKETMSG.getMSG() + ChatColor.YELLOW + msg);
							p.sendMessage(
									Messages.STM_TICKETDATE.getMSG() + ChatColor.YELLOW + res2.getString("datum"));
							p.sendMessage(ChatColor.RED + "Server: " + ChatColor.YELLOW + res2.getString("srv"));
							p.sendMessage(ChatColor.RED + "----------" + ChatColor.YELLOW + "Ticket System"
									+ ChatColor.RED + "----------");
							UUID ui = UUID.fromString(res2.getString("UUID"));
							statement.executeUpdate("DELETE FROM ticketsystem WHERE UUID = '" + args[1] + "'");
							p.sendMessage(Messages.PREFIX.getMSG() + Messages.STM_TICKETDELETEDSUCCESS.getMSG());
							String pl = Bukkit.getPlayer(ui).getName();
							Player pll = Bukkit.getServer().getPlayer(pl);
							pll.sendMessage(Messages.PREFIX.getMSG()
									+ Messages.STM_TICKETREVIEWEDUSER.getMSG().replace("{reviewer}", p.getName()));
							// Sends a message to all online players that have
							// the permission "ticket.review"
							Bukkit.broadcast(
									Messages.PREFIX.getMSG() + Messages.STM_TICKETREVIEWEDSTAFF.getMSG()
											.replace("{reviewer}", p.getName().replace("{user}", Name1)),
									"ticket.review");
						} catch (SQLException ef) {
							// Sender has to look into the console because of
							// the stacktrace
							p.sendMessage(Messages.PREFIX.getMSG() + Messages.STM_CANTRECIEVEDATA.getMSG());
							ef.printStackTrace();
						} catch (NullPointerException npef) {
						}
					}
				} else if (!p.hasPermission("ticket.review")) {
					p.sendMessage(Messages.PREFIX.getMSG() + Messages.STM_NOPERM.getMSG());
				}
			} else if (args[0].equalsIgnoreCase("status")) {
				Statement statement;
				try {
					statement = Main.main.c.createStatement();
					ResultSet res = statement
							.executeQuery("select * from ticketsystem where UUID = '" + p.getUniqueId() + "';");
					res.next();
					// This string just gets used to trigger an exception, if
					// the query is empty
					String test = res.getString("Name");
					p.sendMessage(Messages.PREFIX.getMSG() + Messages.STM_TICKETNOTREVIWEDSTATUS.getMSG());
				} catch (SQLException e) {
					p.sendMessage(Messages.PREFIX.getMSG() + Messages.STM_TICKETREVIWEDSTATUS.getMSG());
				}

			} else if (args[0].equalsIgnoreCase("list")) {
				if (!p.hasPermission("ticket.list")) {
					p.sendMessage(Messages.PREFIX.getMSG() + Messages.STM_NOPERM.getMSG());
				} else {
					PreparedStatement sql;
					try {
						/*
						 * --------------------------------------- This code is
						 * by McMedia! http://dev.bukkit.org/mcmedia/ <-
						 * --------------------------------------- Sadly he
						 * wasn't online since May 2015 :(
						 * --------------------------------------- I was too
						 * lazy to come up with my own solution :D Just blame me
						 */
						sql = Main.main.c.prepareStatement("SELECT * FROM `ticketsystem`");
						ResultSet result = sql.executeQuery();
						StringBuilder sb = new StringBuilder();
						for (; result.next(); sb.append(
								(new StringBuilder(String.valueOf(result.getString("Name")))).append(", ").toString()))
							;
						String NewTickets = sb.toString();
						Pattern pattern = Pattern.compile(", $");
						Matcher matcher = pattern.matcher(NewTickets);
						NewTickets = matcher.replaceAll("");
						if (NewTickets.equals("")) {
							sender.sendMessage(Messages.PREFIX.getMSG() + Messages.STM_NONEWTICKETS.getMSG());
						} else {
							sender.sendMessage((new StringBuilder())
									.append(Messages.PREFIX.getMSG() + Messages.STM_NEWTICKETSFROM.getMSG())
									.toString());
							sender.sendMessage((new StringBuilder()).append(ChatColor.RED).append(NewTickets)
									.append(".").toString());
						}
					} catch (SQLException e) {
						e.printStackTrace();
					}

				}
			} else if (args[0].equalsIgnoreCase("stats")) {
				if (!p.hasPermission("ticket.stats")) {
					p.sendMessage(Messages.PREFIX.getMSG() + Messages.STM_NOPERM.getMSG());
				} else {
					if (args.length == 1) {
						Statement statement;
						try {
							statement = Main.main.c.createStatement();
							ResultSet res = statement.executeQuery(
									"select * from ticketsystem_player where UUID = '" + p.getUniqueId() + "';");
							res.next();
							Main.main.Name = res.getString("Name");
							p.sendMessage(Messages.STM_STATSFROM.getMSG() + res.getString("Name")
									+ Messages.STM_YOU.getMSG());
							p.sendMessage(Messages.STM_NUMBERCOMPOSEDTICKETS.getMSG() + ChatColor.GOLD + "'"
									+ res.getInt("Anzahl_Tickets") + "'");
							p.sendMessage(Messages.STM_LASTTICKETCOMPOSEDDATE.getMSG() + ChatColor.GOLD + "'"
									+ res.getString("Letztes_Ticket") + "'");
							p.sendMessage(ChatColor.YELLOW + "UUID: " + res.getString("UUID"));
						} catch (SQLException e) {
							p.sendMessage(Messages.PREFIX.getMSG() + Messages.STM_NOTFOUNDINDBYOU.getMSG());
						}

					} else {
						Statement statement;
						try {
							statement = Main.main.c.createStatement();
							ResultSet res = statement.executeQuery(
									"select * from ticketsystem_player where Name LIKE '%" + args[1] + "%';");
							res.next();
							Main.main.Name = res.getString("Name");
							p.sendMessage(Messages.STM_STATSFROM.getMSG() + args[1] + ":");
							p.sendMessage(Messages.STM_NUMBERCOMPOSEDTICKETS.getMSG() + ChatColor.GOLD + "'"
									+ res.getInt("Anzahl_Tickets") + "'");
							p.sendMessage(Messages.STM_LASTTICKETCOMPOSEDDATE.getMSG() + ChatColor.GOLD + "'"
									+ res.getString("Letztes_Ticket") + "'");
							p.sendMessage(ChatColor.YELLOW + "UUID: " + res.getString("UUID"));
						} catch (SQLException e) {
							p.sendMessage(Messages.PREFIX.getMSG() + Messages.STM_NOTFOUNFINDBPLAYER.getMSG());
						}
					}

				}
			} else if (args[0].equalsIgnoreCase("reload")) {
				Main.main.reloadConfig();
				Main.main.updateLanguage();
				p.sendMessage(Messages.PREFIX.getMSG() + Messages.STM_RELOADED.getMSG());
			} else if (args[0].equalsIgnoreCase("language") || args[0].equals("lang")) {
				if (args.length == 1) {
					p.sendMessage(Messages.PREFIX.getMSG() + Messages.STM_GIVELANG.getMSG());
				} else {
					if (args[1].equals("de")) {
						if (Main.main.lang.equals("de")) {
							p.sendMessage(Messages.PREFIX.getMSG() + Messages.STM_LANGALREADYSET.getMSG().replace("{lang}", "de"));
						} else {
							Main.main.lang = Language.DE;
							Main.main.getConfig().set("language", "de");
							Main.main.saveConfig();
							Main.main.reloadConfig();
							p.sendMessage(Messages.PREFIX.getMSG()
									+ Messages.STM_CHANGELANG.getMSG().replace("{lang}", "de"));
						}
					} else if (args[1].equals("en")) {
						if (Main.main.lang.equals("en")) {
							p.sendMessage(Messages.PREFIX.getMSG() + Messages.STM_LANGALREADYSET.getMSG().replace("{lang}", "en"));
						} else {
							Main.main.lang = Language.EN;
							Main.main.getConfig().set("language", "en");
							Main.main.saveConfig();
							Main.main.reloadConfig();
							p.sendMessage(Messages.PREFIX.getMSG()
									+ Messages.STM_CHANGELANG.getMSG().replace("{lang}", "en"));
						}
					} else {
						p.sendMessage(Messages.PREFIX.getMSG() + Messages.STM_LANGNOTFOUND.getMSG().replace("{lang}", args[1]));
					}
				}
			}
		}
		return true;
	}

}
