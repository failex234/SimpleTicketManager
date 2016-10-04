package de.failexy0.femo.simpleticketmanager.listener;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Collection;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;

import mkremins.fanciful.FancyMessage;

public class PluginMessageListener implements org.bukkit.plugin.messaging.PluginMessageListener {

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
				Bukkit.broadcast(ChatColor.RED + "----------" + ChatColor.YELLOW + "Ticket System"
						+ ChatColor.RED + "----------", "ticket.review");
				Bukkit.broadcast(ChatColor.RED + "Ticket from: " + ChatColor.YELLOW + name, "ticket.review");
				Bukkit.broadcast(ChatColor.RED + "UUID: " + ChatColor.YELLOW + uuid, "ticket.review");
				Bukkit.broadcast(ChatColor.RED + "Message: " + ChatColor.YELLOW + msg, "ticket.review");
				Bukkit.broadcast(ChatColor.RED + "Server: " + ChatColor.YELLOW + srv, "ticket.review");
				Bukkit.broadcast(ChatColor.RED + "----------" + ChatColor.YELLOW + "Ticket System"
						+ ChatColor.RED + "----------", "ticket.review");
				Collection<? extends Player> onlinePlayers1 = Bukkit.getServer().getOnlinePlayers();
				for (int i = 0; i < onlinePlayers1.size(); i++) {
					Player Cpl = (Player) onlinePlayers1.toArray()[i];
					if (Cpl.hasPermission("ticket.review")) {
						new FancyMessage("Click here to review this ticket").color(ChatColor.GREEN)
								.tooltip("Click to review").command("/ticket review " + uuid).send(Cpl);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}


	
}
