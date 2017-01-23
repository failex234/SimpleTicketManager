package de.failexy0.femo.simpleticketmanager.sender;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import de.failexy0.femo.simpleticketmanager.Main;

public class ServerMessageSender {

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
		out.writeUTF("Forward");
		out.writeUTF("ALL");
		out.writeUTF("ticketmanager"); 

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
		player.sendPluginMessage(Main.main, "BungeeCord", out.toByteArray());
	}
}
