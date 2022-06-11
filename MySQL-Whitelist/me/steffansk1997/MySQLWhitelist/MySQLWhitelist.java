package me.steffansk1997.MySQLWhitelist;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

public class MySQLWhitelist extends JavaPlugin implements Listener{
	public final Logger logger = Logger.getLogger("Minecraft");
	public static MySQLWhitelist plugin;
	public static Connection connection;
	
	@Override
	public void onDisable(){
		PluginDescriptionFile pdfFile = this.getDescription();
		this.logger.info(pdfFile.getName() + " Version " + pdfFile.getVersion() + " Has Been Disabled!");
		try {
			if(connection == null && !connection.isClosed()){
				connection.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void onEnable(){
		PluginDescriptionFile pdfFile = this.getDescription();
		this.logger.info(pdfFile.getName() + " Version " + pdfFile.getVersion() + " Has Been Enabled!");
		getServer().getPluginManager().registerEvents(this, this);
		this.saveDefaultConfig();
		
		openConnection();
		try{
			PreparedStatement sql = connection.prepareStatement("CREATE TABLE IF NOT EXISTS `" + getConfig().getString("table") + "` (`UUID` varchar(100), `user` varchar(100)) ;");
			sql.execute();
		} catch(Exception e){
			e.printStackTrace();
		} finally{
			closeConnection();
		}
	}
	
	public synchronized void openConnection(){
		try{
			connection = DriverManager.getConnection("jdbc:mysql://"+ getConfig().getString("host") +":" + getConfig().getString("port") + "/"+ getConfig().getString("database") +"", ""+ getConfig().getString("user") +"", ""+ getConfig().getString("password") +"");
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	
	public synchronized static void closeConnection(){
		try{
			connection.close();
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	
	public boolean isWhitelisted(Player player){
		openConnection();
		try{
			PreparedStatement sql = connection.prepareStatement("SELECT * FROM `" + getConfig().getString("table") + "` WHERE `UUID`=?;");
			UUID uuid = player.getUniqueId();
			sql.setString(1, uuid.toString());
			ResultSet rs = sql.executeQuery();
			if(rs.next()){
				PreparedStatement sql4 = connection.prepareStatement("UPDATE `" + getConfig().getString("table") + "` SET `user`=? WHERE `UUID`=?;");
				sql4.setString(1, player.getName());
				sql4.setString(2, uuid.toString());
				sql4.executeUpdate();
				sql4.close();
				sql.close();
				rs.close();
				return true;
			}else{
				PreparedStatement sql2 = connection.prepareStatement("SELECT * FROM `" + getConfig().getString("table") + "` WHERE `user`=? && `UUID` IS NULL;");
				sql2.setString(1, player.getName());
				ResultSet rs2 = sql2.executeQuery();
				if(rs2.next()){
					PreparedStatement sql3 = connection.prepareStatement("UPDATE `" + getConfig().getString("table") + "` SET `UUID`=? WHERE `user`=?;");
					sql3.setString(1, uuid.toString());
					sql3.setString(2, player.getName());
					sql3.executeUpdate();
					sql2.close();
					rs2.close();
					sql3.close();
					return true;
				}else{
					return false;
				}
			}
		}catch(Exception e){
			e.printStackTrace();
			return false;
		}finally{
			closeConnection();
		}
	}
	
	public void addWhitelistOnline(Player player, CommandSender sender){
		openConnection();
		try{
		PreparedStatement sql = connection.prepareStatement("SELECT * FROM `" + getConfig().getString("table") + "` WHERE `UUID`=?;");
		UUID uuid = player.getUniqueId();
		sql.setString(1, uuid.toString());
		ResultSet rs = sql.executeQuery();
		if(!rs.next()){
			PreparedStatement sql1 = connection.prepareStatement("INSERT INTO `" + getConfig().getString("table") + "` (`UUID`, `user`) VALUES (?,?);");
			sql1.setString(1, uuid.toString());
			sql1.setString(2, player.getName());
			sql1.execute();
			sql1.close();
		}rs.close();
		sql.close();
		sender.sendMessage(ChatColor.GREEN + player.getName() + " is now whitelisted!");
		player.sendMessage(ChatColor.GREEN + "You have been whitelisted!");
		}catch(Exception e){
			e.printStackTrace();;
		}finally{
			closeConnection();
		}
	}
	public void addWhitelistOffline(String player, CommandSender sender){
		openConnection();
		try{
		PreparedStatement sql = connection.prepareStatement("SELECT * FROM `" + getConfig().getString("table") + "` WHERE `user`=?;");
		sql.setString(1, player);
		ResultSet rs = sql.executeQuery();
		if(!rs.next()){
			PreparedStatement sql1 = connection.prepareStatement("INSERT INTO `" + getConfig().getString("table") + "` (`user`) VALUES (?);");
			sql1.setString(1, player);
			sql1.execute();
			sql1.close();
		}rs.close();
		sql.close();
		sender.sendMessage(ChatColor.GREEN + player + " is now whitelisted!");
		}catch(Exception e){
			e.printStackTrace();;
		}finally{
			closeConnection();
		}
	}
	public void delWhitelistOnline(Player player, CommandSender sender){
		openConnection();
		try{
		PreparedStatement sql = connection.prepareStatement("DELETE FROM `" + getConfig().getString("table") + "` WHERE `UUID`=?;");
		UUID uuid = player.getUniqueId();
		sql.setString(1, uuid.toString());
		sql.execute();
		sql.close();
		sender.sendMessage(ChatColor.RED + player.getName() + " is no longer whitelisted!");
		player.kickPlayer("You have been removed from our whitelist");
		}catch(Exception e){
			e.printStackTrace();;
		}finally{
			closeConnection();
		}
	}
	public void delWhitelistOffline(String player, CommandSender sender){
		openConnection();
		try{
		PreparedStatement sql = connection.prepareStatement("DELETE FROM `" + getConfig().getString("table") + "` WHERE `user`=?;");
		sql.setString(1, player);
		sql.execute();
		sql.close();
		sender.sendMessage(ChatColor.RED + player + " is no longer whitelisted!");
		}catch(Exception e){
			e.printStackTrace();;
		}finally{
			closeConnection();
		}
	}
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, final String[] args){
		if(commandLabel.equalsIgnoreCase("mywl") || commandLabel.equalsIgnoreCase("mywhitelist")){
			if(args.length >= 1){
				if(args[0].equalsIgnoreCase("add")){
					if(sender.hasPermission("MySQLWhitelist.del") || sender.isOp() || sender.hasPermission("MySQLWhitelist.*")){
						if(args.length == 2){
							if(sender.getServer().getPlayer(args[1]) != null){
								addWhitelistOnline(sender.getServer().getPlayer(args[1]), sender);
							}else{
								addWhitelistOffline(args[1], sender);
							}
						}else{
							sender.sendMessage(ChatColor.RED + "Usage:" + ChatColor.WHITE + " /mywl add <player>");
						}
					}else{
						sender.sendMessage(ChatColor.RED + "I'm sorry but you dont have the right permissions to do this");
					}
				}else if(args[0].equalsIgnoreCase("del")){
					if(sender.hasPermission("MySQLWhitelist.del") || sender.isOp() || sender.hasPermission("MySQLWhitelist.*")){
						if(args.length == 2){
							if(sender.getServer().getPlayer(args[1]) != null){
								delWhitelistOnline(sender.getServer().getPlayer(args[1]), sender);
							}else{
								delWhitelistOffline(args[1], sender);
							}
						}else{
							sender.sendMessage(ChatColor.RED + "Usage:" + ChatColor.WHITE + " /mywl del <player>");
						}
					}else{
						sender.sendMessage(ChatColor.RED + "I'm sorry but you dont have the right permissions to do this");
					}
				}else if(args[0].equalsIgnoreCase("on")){
					if(sender.hasPermission("MySQLWhitelist.enable") || sender.isOp() || sender.hasPermission("MySQLWhitelist.*")){
						this.getConfig().set("enabled", true);
						sender.sendMessage(ChatColor.GREEN + "WhiteList enabled");
					}else{
						sender.sendMessage(ChatColor.RED + "I'm sorry but you dont have the right permissions to do this");
					}
				}else if(args[0].equalsIgnoreCase("off")){
					if(sender.hasPermission("MySQLWhitelist.disable") || sender.isOp() || sender.hasPermission("MySQLWhitelist.*")){
					this.getConfig().set("enabled", false);
					sender.sendMessage(ChatColor.GREEN + "WhiteList disabled");
					}else{
						sender.sendMessage(ChatColor.RED + "I'm sorry but you dont have the right permissions to do this");
					}
				}else{
					sender.sendMessage(ChatColor.RED + "Usage:" + ChatColor.WHITE + " /mywl add/del/on/off [player]");
				}
			}else{
				sender.sendMessage(ChatColor.RED + "Usage:" + ChatColor.WHITE + " /mywl add/del/on/off [player]");
			}
			return true;
		}
		return false;
	}
	@EventHandler
	public void onPlayerLogin(PlayerLoginEvent event){
		Player player = event.getPlayer();
		if(!isWhitelisted(player)){
			if(this.getConfig().getBoolean("enabled")){
				event.setKickMessage("You're not on our whitelist");
				event.setResult(Result.KICK_WHITELIST);
			}
		}
	}
}
