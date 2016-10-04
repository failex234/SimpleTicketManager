package de.failexy0.femo.simpleticketmanager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.huskehhh.mysql.mysql.*;
import com.huskehhh.mysql.sqlite.SQLite;

import mkremins.fanciful.FancyMessage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
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

/*
 * SimpleTicketManager - created by failexy0 for FeMoCraft
 * 
 * Remember to change Connection Settings before using it with a real server.
 * 
 * @author failexy0
 * 
 * Maybe i will implement a config file that you can use to change these settings quick and on-the-fly.
 */
public class Main extends JavaPlugin implements PluginMessageListener {
	String Name;
	String srvn = this.getConfig().getString("server");
	String port = this.getConfig().getString("port");
	String db = this.getConfig().getString("database");
	String user = this.getConfig().getString("user");
	String pw = this.getConfig().getString("password");
	//SQLite sqll = new SQLite("simpleticketmanager");
	MySQL sqll = new MySQL(srvn, port, db, user, pw);
	Connection c;

	@Override
	public void onEnable() {
		this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
		this.getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", this);
		File config = new File(this.getDataFolder(), "config.yml");
		if (!config.exists()) {
			this.getConfig().options().copyDefaults(true);
			this.saveConfig();
		}
		try {
			// Creation of the table "ticketsystem" with the values Name,
			// Nachricht, srv, datum and reviewed (Legacy var)
			c = sqll.openConnection();
			for (int i = 0; i < 9; i++) {
				System.out.println(
						"[SimpleTicketManager] Erfolgreich eine Verbindung mit dem Server hergestellt! (STUFE 1 / 2)");
			}
			Statement statement = c.createStatement();
			statement.executeUpdate(
					"CREATE TABLE IF NOT EXISTS `ticketsystem` ( Name text, UUID text, Nachricht text, srv text, datum text);");
			statement.executeUpdate(
					"CREATE TABLE IF NOT EXISTS `ticketsystem_player` ( Name text, UUID text, Letztes_Ticket text, Anzahl_Tickets int);");
			System.out
					.println("[SimpleTicketManager] Erfolgreich die Tabellen zur Datenbank hinzugefügt (STUFE 2 / 2)");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			for (int i = 0; i < 9; i++) {
				System.out.println("[SimpleTicketManager] KANN NICHT MIT DATENBANK verbinden!");
			}
			this.getServer().getPluginManager().disablePlugin(this);
			e.printStackTrace();
		}
	}

	public static String getStackTrace(final Throwable throwable) {
		final StringWriter sw = new StringWriter();
		final PrintWriter pw = new PrintWriter(sw, true);
		throwable.printStackTrace(pw);
		return sw.getBuffer().toString();
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		Player p = (Player) sender;
		// If Player executes the command /ticket
		if (cmd.getName().equalsIgnoreCase("ticket")) {
			// If no args are given
			if (args.length == 0) {
				if (!p.hasPermission("ticket.list")) {
					p.sendMessage(ChatColor.RED + "Zu wenig Argumente, bitte schreib' /ticket <v|status>");
				} else if (!p.hasPermission("ticket.review")) {
					p.sendMessage(ChatColor.RED + "Zu wenig Argumente, bitte schreib' /ticket <v|status|list|>");
				} else {
					p.sendMessage(
							ChatColor.RED + "Zu wenig Argumente, bitte schreib' /ticket <v|status|review|list|stats>");
				}
				// if the first arg is "compose"
			} else if (args[0].equalsIgnoreCase("v")) {
				if (args.length == 1) {
					p.sendMessage(ChatColor.RED + "Bitte schreibe eine Nachricht dazu: /ticket v <nachricht>");
				} else {
					Statement statement;
					try {
						// If a ticket with the senders name is found in the db,
						// the sender is not able to compose another ticket.
						statement = c.createStatement();
						ResultSet res = statement
								.executeQuery("select * from ticketsystem where UUID = '" + p.getUniqueId() + "';");
						res.next();
						// This string just gets used to trigger an exception,
						// if the query is empty
						String test = res.getString("Name");
						p.sendMessage(ChatColor.RED
								+ "Entschludige aber du hast bereits ein Ticket verfasst, du kannst erst ein weiteres Ticket verfassen, wenn dein aktuelles reviewed wird!");
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
						Bukkit.broadcast(ChatColor.RED + "----------" + ChatColor.YELLOW + "Armageddon Ticket System"
								+ ChatColor.RED + "----------", "ticket.review");
						Bukkit.broadcast(ChatColor.BOLD + "" + ChatColor.RED + "Ticket von: " + ChatColor.YELLOW
								+ p.getDisplayName(), "ticket.review");
						Bukkit.broadcast(
								ChatColor.BOLD + "" + ChatColor.RED + "UUID: " + ChatColor.YELLOW + p.getUniqueId(),
								"ticket.review");
						Bukkit.broadcast(ChatColor.RED + "Nachricht: " + ChatColor.YELLOW + myString, "ticket.review");
						Bukkit.broadcast(
								ChatColor.RED + "Server: " + ChatColor.YELLOW + this.getServer().getServerName(),
								"ticket.review");
						Bukkit.broadcast(ChatColor.RED + "----------" + ChatColor.YELLOW + "Armageddon Ticket System"
								+ ChatColor.RED + "----------", "ticket.review");
						for (int i = 0; i < onlinePlayers1.size(); i++) {
							Cpl = (Player) onlinePlayers1.toArray()[i];
							if (Cpl.hasPermission("ticket.review")) {
								new FancyMessage("Klicke hier um das Ticket zu reviewen").color(ChatColor.GREEN)
										.tooltip("Klick zum reviewen").command("/ticket review " + p.getUniqueId())
										.send(Cpl);
							}
						}
						myString = myString.replace("'", "{SEMICOLON}");
						BungeeMessageSender(sender, p.getDisplayName(), p.getUniqueId().toString(), myString,
								this.getServer().getServerName());
						// Get Date and Time for later use with db
						DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
						Calendar cal = Calendar.getInstance();
						String datum = dateFormat.format(cal.getTime());
						try {
							// Save to MySQL Table Code here, Save Username,
							// Message, time and server
							statement = c.createStatement();
							statement.executeUpdate(
									"INSERT INTO ticketsystem (`Name`, `UUID`, `Nachricht`, `srv`, `datum`) VALUES ('"
											+ p.getDisplayName() + "', '" + p.getUniqueId() + "', '" + myString + "', '"
											+ this.getServer().getServerName() + "', '" + datum + "');");
							try {
								statement = c.createStatement();
								ResultSet res = statement.executeQuery(
										"select * from ticketsystem_player where UUID = '" + p.getUniqueId() + "';");
								res.next();
								String test_pl = res.getString("Name");
								int anzahl = res.getInt("Anzahl_Tickets");
								anzahl += 1;
								statement.executeUpdate("UPDATE `ticketsystem_player` SET Name = '" + p.getDisplayName()
										+ "', UUID = '" + p.getUniqueId() + "', Letztes_Ticket = '" + datum
										+ "', Anzahl_Tickets = '" + anzahl + "' WHERE UUID = '" + p.getUniqueId()
										+ "';");
							} catch (SQLException eSQL) {
								statement.executeUpdate(
										"INSERT INTO ticketsystem_player (`Name`, `UUID`, `Letztes_Ticket`, `Anzahl_Tickets`) VALUES ('"
												+ p.getDisplayName() + "','" + p.getUniqueId() + "','" + datum
												+ "', 1);");
							}
							p.sendMessage(ChatColor.RED + "Vielen Dank! Deine Nachricht wurde in die" + ChatColor.YELLOW
									+ " Datenbank" + ChatColor.RED
									+ " und wird von uns so schnell wie möglich reviewed. " + ChatColor.YELLOW
									+ "Alle Team-Mitglieder" + ChatColor.RED
									+ " die Online sind, kriegen dein Ticket direkt.");
						} catch (SQLException f) {
							f.printStackTrace();
						}
					}
					// Done with saving to database
				}
				// Check if first arg is "review"
			} else if (args[0].equalsIgnoreCase("review")) {
				if (p.hasPermission("ticket.review") && args.length == 1) {
					p.sendMessage(
							ChatColor.RED + "Du musst einen Spielernamen angeben: /ticket review <spielername|UUID>");
				} else if (p.hasPermission("ticket.review")) {
					// Execute if sender has the permission node "ticket.review"
					// retrieve from MySQL Table Code here, retrieve Username,
					// Message, time and server
					Statement statement;
					try {
						statement = c.createStatement();
						ResultSet res = statement
								.executeQuery("select * from ticketsystem where Name LIKE '%" + args[1] + "%';");
						res.next();
						Name = res.getString("Name");
						String msg = res.getString("Nachricht").replace("{SEMICOLON}", "'");
						String UUIDD = res.getString("UUID");
						p.sendMessage(ChatColor.RED + "----------" + ChatColor.YELLOW + "Armageddon Ticket System"
								+ ChatColor.RED + "----------");
						p.sendMessage(ChatColor.RED + "Ticket von: " + ChatColor.YELLOW + res.getString("Name"));
						p.sendMessage(ChatColor.RED + "UUID: " + ChatColor.YELLOW + UUIDD);
						p.sendMessage(ChatColor.RED + "Nachricht: " + ChatColor.YELLOW + msg);
						p.sendMessage(ChatColor.RED + "Datum / Uhrzeit: " + ChatColor.YELLOW + res.getString("datum"));
						p.sendMessage(ChatColor.RED + "Server: " + ChatColor.YELLOW + res.getString("srv"));
						p.sendMessage(ChatColor.RED + "----------" + ChatColor.YELLOW + "Armageddon Ticket System"
								+ ChatColor.RED + "----------");
						statement
								.executeUpdate("DELETE FROM ticketsystem WHERE UUID = '" + res.getString("UUID") + "'");
						p.sendMessage(ChatColor.GREEN + "Ticket wurde aus der Datenbank gelöscht");
						Bukkit.broadcast(
								ChatColor.GREEN + p.getDisplayName() + " has just reviewed the ticket of " + Name,
								"ticket.review");
						UUID ui = UUID.fromString(UUIDD);
						String pl = Bukkit.getPlayer(ui).getName();
						Player pll = Bukkit.getServer().getPlayer(pl);
						pll.sendMessage(
								ChatColor.GREEN + "Dein Ticket wurde gerade reviewed von " + p.getDisplayName() + "!");
						// statement.executeUpdate("UPDATE `ticketsystem` SET
						// `reviewed` = null WHERE Name = '"+ args[1] +"'");
						// Sends a message to all online players that have the
						// permission "ticket.review"
					} catch (NullPointerException np) {
					} catch (SQLException e) {
						try {
							statement = c.createStatement();
							ResultSet res2 = statement
									.executeQuery("SELECT * FROM ticketsystem WHERE UUID = '" + args[1] + "';");
							res2.next();
							String Name1 = res2.getString("Name");
							String msg = res2.getString("Nachricht").replace("{SEMICOLON}", "'");
							p.sendMessage(ChatColor.RED + "----------" + ChatColor.YELLOW + "Armageddon Ticket System"
									+ ChatColor.RED + "----------");
							p.sendMessage(ChatColor.RED + "Ticket von: " + ChatColor.YELLOW + res2.getString("Name"));
							p.sendMessage(ChatColor.RED + "UUID: " + ChatColor.YELLOW + res2.getString("UUID"));
							p.sendMessage(
									ChatColor.RED + "Nachricht: " + ChatColor.YELLOW + msg);
							p.sendMessage(ChatColor.RED + "Datum: " + ChatColor.YELLOW + res2.getString("datum"));
							p.sendMessage(ChatColor.RED + "Server: " + ChatColor.YELLOW + res2.getString("srv"));
							p.sendMessage(ChatColor.RED + "----------" + ChatColor.YELLOW + "Armageddon Ticket System"
									+ ChatColor.RED + "----------");
							UUID ui = UUID.fromString(res2.getString("UUID"));
							statement.executeUpdate("DELETE FROM ticketsystem WHERE UUID = '" + args[1] + "'");
							p.sendMessage(ChatColor.GREEN + "Ticket wurde aus der Datenbank gelöscht");
							String pl = Bukkit.getPlayer(ui).getName();
							Player pll = Bukkit.getServer().getPlayer(pl);
							pll.sendMessage(ChatColor.GREEN + "Dein Ticket wurde gerade reviewed von "
									+ p.getDisplayName() + "!");
							// Sends a message to all online players that have
							// the permission "ticket.review"
							Bukkit.broadcast(ChatColor.GREEN + p.getDisplayName() + " hat gerade das Ticket von "
									+ Name1 + " reviewed.", "ticket.review");
						} catch (SQLException ef) {
							// Sender has to look into the console because of
							// the stacktrace
							p.sendMessage(ChatColor.RED
									+ "Kann Daten nicht empfangen, warscheinlich gibt es gar kein Ticket von dieser Person, guck bitte mal in die Konsole!");
							ef.printStackTrace();
						} catch (NullPointerException npef) {
						}
					}
				} else if (!p.hasPermission("ticket.review")) {
					p.sendMessage(ChatColor.RED + "Entschuldigung aber du scheinst keine Rechte dafür zu haben!");
				}
			} else if (args[0].equalsIgnoreCase("status")) {
				Statement statement;
				try {
					statement = c.createStatement();
					ResultSet res = statement
							.executeQuery("select * from ticketsystem where UUID = '" + p.getUniqueId() + "';");
					res.next();
					// This string just gets used to trigger an exception, if
					// the query is empty
					String test = res.getString("Name");
					p.sendMessage(ChatColor.RED
							+ "Dein Ticket wurde noch nicht reviewed, frag doch eine Team-Mitglied, ob er sich um dich kümmert.");
				} catch (SQLException e) {
					p.sendMessage(ChatColor.GREEN + "Dein Ticket wurde reviewed!");
				}

			} else if (args[0].equalsIgnoreCase("list")) {
				if (!p.hasPermission("ticket.list")) {
					p.sendMessage(ChatColor.RED + "Entschuldigung aber du scheinst keine Rechte dafür zu haben!");
				} else {
					PreparedStatement sql;
					try {
						/*
						 * --------------------------------------- This code is
						 * by McMedia! http://dev.bukkit.org/mcmedia/ <-
						 * Platzhalter ---------------------------------------
						 * Sadly he wasn't online since May 2015 :(
						 * --------------------------------------- I was too
						 * lazy to come up with my own solution :D Just blame me
						 */
						sql = c.prepareStatement("SELECT * FROM `ticketsystem`");
						ResultSet result = sql.executeQuery();
						StringBuilder sb = new StringBuilder();
						for (; result.next(); sb.append(
								(new StringBuilder(String.valueOf(result.getString("Name")))).append(", ").toString()))
							;
						String BannedPlayers = sb.toString();
						Pattern pattern = Pattern.compile(", $");
						Matcher matcher = pattern.matcher(BannedPlayers);
						BannedPlayers = matcher.replaceAll("");
						if (BannedPlayers.equals("")) {
							sender.sendMessage(ChatColor.GREEN + "Keine offenen Tickets");
						} else {
							sender.sendMessage((new StringBuilder()).append(ChatColor.GOLD)
									.append("Es gibt offene Tickets von:").toString());
							sender.sendMessage((new StringBuilder()).append(ChatColor.RED).append(BannedPlayers)
									.append(".").toString());
						}
					} catch (SQLException e) {
						e.printStackTrace();
					}

				}
			} else if (args[0].equalsIgnoreCase("stats")) {
				if (!p.hasPermission("ticket.stats")) {
					p.sendMessage(ChatColor.RED + "Entschuldigung aber du scheinst keine Rechte dafür zu haben!");
				} else {
					if (args.length == 1) {
						Statement statement;
						try {
							statement = c.createStatement();
							ResultSet res = statement.executeQuery(
									"select * from ticketsystem_player where UUID = '" + p.getUniqueId() + "';");
							res.next();
							Name = res.getString("Name");
							p.sendMessage(ChatColor.YELLOW + "Statistik von " + res.getString("Name") + " (Du):");
							p.sendMessage(ChatColor.RED + "Anzahl an verfassten Tickets: " + ChatColor.GOLD + "'"
									+ res.getInt("Anzahl_Tickets") + "'");
							p.sendMessage(ChatColor.RED + "Letztes Ticket verfasst: " + ChatColor.GOLD + "'"
									+ res.getString("Letztes_Ticket") + "'");
							p.sendMessage(ChatColor.YELLOW + "UUID: " + res.getString("UUID"));
						} catch (SQLException e) {
							p.sendMessage(ChatColor.RED + "Du wurdest nicht in der Datenbank gefunden.");
						}

					} else {
						Statement statement;
						try {
							statement = c.createStatement();
							ResultSet res = statement.executeQuery(
									"select * from ticketsystem_player where Name LIKE '%" + args[1] + "%';");
							res.next();
							Name = res.getString("Name");
							p.sendMessage(ChatColor.YELLOW + "Statistik von " + args[1] + ":");
							p.sendMessage(ChatColor.RED + "Anzahl an verfassten Tickets: " + ChatColor.GOLD + "'"
									+ res.getInt("Anzahl_Tickets") + "'");
							p.sendMessage(ChatColor.RED + "Letztes Ticket verfasst: " + ChatColor.GOLD + "'"
									+ res.getString("Letztes_Ticket") + "'");
							p.sendMessage(ChatColor.YELLOW + "UUID: " + res.getString("UUID"));
						} catch (SQLException e) {
							p.sendMessage(ChatColor.RED + "Dieser Spieler wurde nicht in der Datenbank gefunden.");
						}
					}

				}
			}
		}

		return true;
	}

	/**
	 * This method gets all the following parameters and sends them via the
	 * Plugin Channel "BungeeCord" and with the suchannel "ticketsystem" via
	 * BungeeCord to all other servers in the network. The servers will then
	 * broadcast a very similar looking ticket alert to the admins.
	 * 
	 * @param sender
	 *            any player. Sends the msgbytes via the PluginMessageChannel
	 * @param name
	 *            The name of the player who composed the ticket
	 * @param uuid
	 *            UUID of the player
	 * @param msg
	 *            The message that the player composed into a ticket
	 * @param srv
	 *            Name of the server the ticket got composed on
	 */
	public void BungeeMessageSender(CommandSender sender, String name, String uuid, String msg, String srv) {
		ByteArrayDataOutput out = ByteStreams.newDataOutput();
		Player player = (Player) sender;
		out.writeUTF("Forward"); // So BungeeCord knows to forward it
		out.writeUTF("ALL");
		out.writeUTF("ticketmanager"); // The channel name to check if this your
										// data

		ByteArrayOutputStream msgbytes = new ByteArrayOutputStream();
		DataOutputStream msgout = new DataOutputStream(msgbytes);

		try {
			msgout.writeUTF(name);
			msgout.writeUTF(uuid);
			msgout.writeUTF(msg);
			msgout.writeUTF(srv);
			msgout.writeShort(123);
		} catch (IOException e) {
			e.printStackTrace();
		}

		out.writeShort(msgbytes.toByteArray().length);
		out.write(msgbytes.toByteArray());
		player.sendPluginMessage(this, "BungeeCord", out.toByteArray());
	}

	@Override
	public void onPluginMessageReceived(String channel, Player player, byte[] message) {
		if (!channel.equals("BungeeCord")) {
			return;
		}
		ByteArrayDataInput in = ByteStreams.newDataInput(message);
		String subchannel = in.readUTF();
		if (subchannel.equals("ticketmanager")) {
			short len = in.readShort();
			byte[] msgbytes = new byte[len];
			in.readFully(msgbytes);
			DataInputStream msgin = new DataInputStream(new ByteArrayInputStream(msgbytes));
			String name;
			String uuid;
			String msg;
			String srv;
			short somenumber;
			try {
				name = msgin.readUTF();
				uuid = msgin.readUTF();
				msg = msgin.readUTF();
				srv = msgin.readUTF();
				somenumber = msgin.readShort();
				Bukkit.broadcast(ChatColor.RED + "----------" + ChatColor.YELLOW + "FeMoCraft Ticket System"
						+ ChatColor.RED + "----------", "ticket.review");
				Bukkit.broadcast(ChatColor.RED + "Ticket by: " + ChatColor.YELLOW + name, "ticket.review");
				Bukkit.broadcast(ChatColor.RED + "UUID: " + ChatColor.YELLOW + uuid, "ticket.review");
				Bukkit.broadcast(ChatColor.RED + "Message: " + ChatColor.YELLOW + msg, "ticket.review");
				Bukkit.broadcast(ChatColor.RED + "Server: " + ChatColor.YELLOW + srv, "ticket.review");
				Bukkit.broadcast(ChatColor.RED + "----------" + ChatColor.YELLOW + "FeMoCraft Ticket System"
						+ ChatColor.RED + "----------", "ticket.review");
				Collection<? extends Player> onlinePlayers1 = Bukkit.getServer().getOnlinePlayers();
				for (int i = 0; i < onlinePlayers1.size(); i++) {
					Player Cpl = (Player) onlinePlayers1.toArray()[i];
					if (Cpl.hasPermission("ticket.review")) {
						new FancyMessage("Click here to review/delete this ticket").color(ChatColor.GREEN)
								.tooltip("Click to review").command("/ticket review " + uuid).send(Cpl);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

}
