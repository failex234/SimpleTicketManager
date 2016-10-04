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
public class Main extends JavaPlugin {
	
	public static Main main;
	
	
	public String Name;
	public de.failexy0.femo.simpleticketmanager.listener.PluginMessageListener pml = new de.failexy0.femo.simpleticketmanager.listener.PluginMessageListener();
	String srvn = this.getConfig().getString("server");
	String port = this.getConfig().getString("port");
	String db = this.getConfig().getString("database");
	String user = this.getConfig().getString("user");
	String pw = this.getConfig().getString("password");
	//SQLite sqll = new SQLite("simpleticketmanager");
	MySQL sqll = new MySQL(srvn, port, db, user, pw);
	public Connection c;

	@Override
	public void onEnable() {
		main = this;
		this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
		this.getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", pml);
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
						"[SimpleTicketManager] Connected successfully to databse! (STAGE 1 / 2)");
			}
			Statement statement = c.createStatement();
			statement.executeUpdate(
					"CREATE TABLE IF NOT EXISTS `ticketsystem` ( Name text, UUID text, Nachricht text, srv text, datum text);");
			statement.executeUpdate(
					"CREATE TABLE IF NOT EXISTS `ticketsystem_player` ( Name text, UUID text, Letztes_Ticket text, Anzahl_Tickets int);");
			System.out
					.println("[SimpleTicketManager] Successfully added table to database / found table (STUFE 2 / 2)");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			for (int i = 0; i < 9; i++) {
				System.out.println("[SimpleTicketManager] CONNECTION WITH DB NOT POSSIBLE!");
			}
			this.getServer().getPluginManager().disablePlugin(this);
			e.printStackTrace();
		}
	}




}
