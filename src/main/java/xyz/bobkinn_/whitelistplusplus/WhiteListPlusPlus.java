package xyz.bobkinn_.whitelistplusplus;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nullable;

public class WhiteListPlusPlus extends JavaPlugin implements Listener{
    public final Logger logger = Logger.getLogger("WhiteListPlusPlus");
    public static WhiteListPlusPlus plugin;
    public static Connection connection;
    public String url;
    public boolean failedtoconnect = false;

    public String addedMsg, usageMsg, addedNotify, delMsg, enabledMsg, disabledMsg, listMsgStart, listMsgSep,reloadMsg, alreadyEnabled, alreadyDisabled, listMsgOnline, listMsgOffline, urlArgs;
    public static String tableName;
    public boolean useSSL, whitelistEnabled;

    @Override
    public void onDisable(){
        PluginDescriptionFile pdfFile = this.getDescription();
        this.logger.info(pdfFile.getName() + " Version " + pdfFile.getVersion() + " Has Been Disabled!");
        if (!failedtoconnect){
            try {
                if(connection == null && !connection.isClosed()){
                    connection.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onEnable(){
        PluginDescriptionFile pdfFile = this.getDescription();
        this.logger.info(pdfFile.getName() + " Version " + pdfFile.getVersion() + " Has Been Enabled!");
        getServer().getPluginManager().registerEvents(this, this);

        reloadMyConfig();

        String finalArgs = "";
        if (!Objects.equals(urlArgs, "")){
            finalArgs = "?"+urlArgs;
        }

        url = "jdbc:mysql://"+ getConfig().getString("mysql.host") +":" + getConfig().getString("mysql.port") + "/"+ getConfig().getString("mysql.database")+finalArgs;
//        this.logger.info("[DEBUG] URL: "+url);

        openConnection();
        if (failedtoconnect){return;}
        try{
            PreparedStatement sql = connection.prepareStatement("CREATE TABLE IF NOT EXISTS `" + tableName + "` (`UUID` varchar(100), `user` varchar(100)) ;");
            sql.execute();
            this.getCommand("whitelistpp").setTabCompleter(new TabCompleter());
        } catch(Exception e){
            e.printStackTrace();
        } finally{
            closeConnection();
        }
    }

    public List<String> getWhitelistUsers(CommandSender sender){
        List<String> list = new ArrayList<>(Collections.emptyList());
        try {
            this.openConnection();
            PreparedStatement sql = connection.prepareStatement("SELECT * FROM `"+tableName+"` WHERE  `user` IS NOT NULL;");
            ResultSet users = sql.executeQuery();
            while (users.next()) {
                String nickname = users.getString("user");
                list.add(nickname);

                sendConsoleInfo(nickname);
            }
            users.close();
            sql.close();
            sendConsoleInfo(list.toString());
            sender.sendMessage(list.toString()+list.size());
            return list;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    public synchronized void openConnection(){
        try{
            connection = DriverManager.getConnection(url, ""+ getConfig().getString("mysql.user") +"", ""+ getConfig().getString("mysql.password") +"");
        } catch (Exception e){
            getLogger().warning(ChatColor.RED+ "Failed to connect to database, shutting down..");
            failedtoconnect = true;
            e.printStackTrace();
            Bukkit.getServer().getPluginManager().disablePlugin(this);
        }
    }

    public synchronized static void closeConnection(){
        try{
            connection.close();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void sendConsoleInfo(String msg){
        plugin.getLogger().info(msg);
    }

    public List<Object> getList(){
        openConnection();
        List<Object> plist = new ArrayList<>(Collections.emptyList());
        try {
            PreparedStatement sql = connection.prepareStatement("SELECT * FROM `"+tableName+"` WHERE  `user` IS NOT NULL;");
            ResultSet users = sql.executeQuery();
            while (users.next()) {
                String nickname = users.getString("user");
                plist.add(nickname);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return plist;
    }

    public void reloadMyConfig() {
        File dataFolder = new File(getDataFolder().getAbsolutePath()+ File.separator);
        if (!dataFolder.exists()){
            getLogger().info("folder not exists");
            getDataFolder().mkdir();
        }
        File configFile = new File(getDataFolder(),"config.yml");
        if (!configFile.exists()){
            try {
                getLogger().info("file not exists");
                configFile.createNewFile();

                InputStream jarConfigStream = getResource("config.yml");
                OutputStream jarConfigStreamO = new FileOutputStream(configFile.getAbsoluteFile());
                IOUtils.copy(jarConfigStream,jarConfigStreamO);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
//        this.saveDefaultConfig();

        try {
            this.getConfig().load(configFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
        addedMsg = getConfig().getString("messages.addedMsg").replace("&","§");
        addedNotify = getConfig().getString("messages.addedNotify").replace("&","§");
        delMsg = getConfig().getString("messages.delMsg").replace("&","§");
        usageMsg = getConfig().getString("messages.usage").replace("&","§");
        enabledMsg = getConfig().getString("messages.enabledMsg").replace("&","§");
        disabledMsg =getConfig().getString("messages.disabledMsg").replace("&","§");
        listMsgStart =getConfig().getString("messages.listMsgStart").replace("&","§");
        listMsgSep = getConfig().getString("messages.listMsgSep").replace("&","§");
        reloadMsg = getConfig().getString("messages.reloadMsg").replace("&","§");
        alreadyEnabled = getConfig().getString("messages.alreadyEnabled").replace("&","§");
        alreadyDisabled = getConfig().getString("messages.alreadyDisabled").replace("&","§");
        listMsgOnline = getConfig().getString("messages.listMsgOnline").replace("&","§");
        listMsgOffline = getConfig().getString("messages.listMsgOffline").replace("&","§");
        tableName = getConfig().getString("mysql.table");
        urlArgs = getConfig().getString("mysql.urlArgs");

        whitelistEnabled = getConfig().getBoolean("enabled");
    }

    public boolean isWhitelisted(Player player){
        boolean useUUIDs = getConfig().getBoolean("useUUIDs");
        String pName = player.getName();
        openConnection();
        try{
            PreparedStatement sql = connection.prepareStatement("SELECT * FROM `"+tableName+"` WHERE `user` IS NOT NULL;");
            String uuid = player.getUniqueId().toString();
            ResultSet allPlayers = sql.executeQuery();
            if (allPlayers.next()){
                if (useUUIDs){
                    PreparedStatement sql2 = connection.prepareStatement("SELECT * FROM `"+tableName+"` WHERE `UUID` =?;");
                    sql2.setString(1,uuid);
                    ResultSet UUIDinSet = sql2.executeQuery();
                    if (!UUIDinSet.next()){
                        sql.close();
                        sql2.close();
                        UUIDinSet.close();
                        sql.close();
                        allPlayers.close();
//                        getLogger().info("1");
                        return true;
                    } else {
                        sql.close();
                        sql2.close();
                        UUIDinSet.close();
                        allPlayers.close();
//                        getLogger().info("2");
                        return false;
                    }

                } else {
                    PreparedStatement sql3 = connection.prepareStatement("SELECT * FROM `"+tableName+"` WHERE `user` =?;");
                    sql3.setString(1,pName);
                    ResultSet NameInSet = sql3.executeQuery();
                    if (NameInSet.next()){
                        sql3.close();
                        NameInSet.close();
                        sql.close();
                        allPlayers.close();
//                        getLogger().info("3");
                        return true;
                    } else {
                        sql3.close();
                        NameInSet.close();
                        sql.close();
                        allPlayers.close();
//                        getLogger().info("4");
                        return false;
                    }
                }
            } else {
                sql.close();
                allPlayers.close();
//                getLogger().info("5");
                return false;
            }

        }catch(Exception e){
            e.printStackTrace();
            return false;
        }finally{
            closeConnection();
        }
    }

    public void addWhitelistOnline(Player player, @Nullable CommandSender sender){
        openConnection();
        try{
            PreparedStatement sql = connection.prepareStatement("SELECT * FROM `" + tableName + "` WHERE `UUID`=?;");
            UUID uuid = player.getUniqueId();
            sql.setString(1, uuid.toString());
            ResultSet rs = sql.executeQuery();
            if(!rs.next()){
                PreparedStatement sql2 = connection.prepareStatement("DELETE FROM `"+tableName+"` WHERE  `UUID` IS NULL AND `user`=?");
                sql2.setString(1,player.getName());
                sql2.execute();
                sql2.close();
                PreparedStatement sql1 = connection.prepareStatement("INSERT INTO `" + tableName + "` (`UUID`, `user`) VALUES (?,?);");
                sql1.setString(1, uuid.toString());
                sql1.setString(2, player.getName());
                sql1.execute();
                sql1.close();
            }rs.close();
            sql.close();
            String pname = player.getName();
            String msg = this.getConfig().getString("messages.addedMsg");
            msg.replace("%player%",pname);
            if (sender !=null){
                sender.sendMessage(msg);
                player.sendMessage(addedNotify);
            }
        }catch(Exception e){
            e.printStackTrace();
        }finally{
            closeConnection();
        }
    }
    public void addWhitelistOffline(String player, CommandSender sender){
        openConnection();
        try{
            PreparedStatement sql = connection.prepareStatement("SELECT * FROM `" + tableName + "` WHERE `user`=?;");
            sql.setString(1, player);
            ResultSet rs = sql.executeQuery();
            if(!rs.next()){
                PreparedStatement sql1 = connection.prepareStatement("INSERT INTO `" + tableName + "` (`user`) VALUES (?);");
                sql1.setString(1, player);
                sql1.execute();
                sql1.close();
            }rs.close();
            sql.close();
            sender.sendMessage(addedMsg.replace("%player%",player));
        }catch(Exception e){
            e.printStackTrace();
        }finally{
            closeConnection();
        }
    }
    public void delWhitelistOnline(Player player, CommandSender sender){
        openConnection();
        try{
            PreparedStatement sql = connection.prepareStatement("DELETE FROM `" + tableName + "` WHERE `UUID`=?;");
            UUID uuid = player.getUniqueId();
            sql.setString(1, uuid.toString());
            sql.execute();
            sql.close();
            sender.sendMessage(delMsg.replace("%player%",player.getName()));
            player.kickPlayer(getConfig().getString("messages.kick").replace("&","§"));
        }catch(Exception e){
            e.printStackTrace();
        }finally{
            closeConnection();
        }
    }
    public void delWhitelistOffline(String player, CommandSender sender){
        openConnection();
        try{
            PreparedStatement sql = connection.prepareStatement("DELETE FROM `" + tableName + "` WHERE `user`=?;");
            sql.setString(1, player);
            sql.execute();
            sql.close();

            sender.sendMessage(delMsg.replace("%player%",player));
        }catch(Exception e){
            e.printStackTrace();
        }finally{
            closeConnection();
        }
    }
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, final String[] args){
        if(commandLabel.equalsIgnoreCase("wpp") || commandLabel.equalsIgnoreCase("whitelistpp")){
            String nopermMsg = getConfig().getString("messages.noperm").replace("&","§");
            if (!sender.hasPermission("whitelistpp.use")){
                sender.sendMessage(nopermMsg);
                return false;
            }
            if(args.length >= 1){
                if(args[0].equalsIgnoreCase("add")){
                    if(sender.hasPermission("whitelistpp.add") || sender.isOp() || sender.hasPermission("whitelistpp.*")){

                        if(args.length == 2){
                            if(sender.getServer().getPlayer(args[1]) != null){
                                addWhitelistOnline(sender.getServer().getPlayer(args[1]), sender);
                            }else{
                                addWhitelistOffline(args[1], sender);
                            }
                        }else{
                            sender.sendMessage(usageMsg.replace("%command%"," /wpp add [player]"));
                        }

                    }else{
                        sender.sendMessage(nopermMsg);
                    }
                }else if(args[0].equalsIgnoreCase("del")){
                    if(sender.hasPermission("whitelistpp.del") || sender.isOp() || sender.hasPermission("whitelistpp.*")){
                        if(args.length == 2){
                            if(sender.getServer().getPlayer(args[1]) != null){
                                delWhitelistOnline(sender.getServer().getPlayer(args[1]), sender);
                            }else{
                                delWhitelistOffline(args[1], sender);
                            }
                        }else{
                            sender.sendMessage(usageMsg.replace("%command%"," /wpp del [player]"));
                        }
                    }else{
                        sender.sendMessage(nopermMsg);
                    }
                }else if(args[0].equalsIgnoreCase("on")){
                    if(sender.hasPermission("whitelistpp.enable") || sender.isOp() || sender.hasPermission("whitelistpp.*")){
//                        if (this.getConfig().getBoolean("enabled")){
//                            sender.sendMessage(alreadyEnabled);
//                            return false;
//                        }
                        this.getConfig().set("enabled", true);
                        try {
                            this.getConfig().save(new File(getDataFolder(),"config.yml"));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        sender.sendMessage(enabledMsg);
                    }else{
                        sender.sendMessage(nopermMsg);
                    }
                }else if(args[0].equalsIgnoreCase("off")){
                    if(sender.hasPermission("whitelistpp.disable") || sender.isOp() || sender.hasPermission("whitelistpp.*")){
//                        if (this.getConfig().getBoolean("enabled")){
//                            sender.sendMessage(alreadyDisabled);
//                            return false;
//                        }
                        this.getConfig().set("enabled", false);
                        try {
                            this.getConfig().save(new File(getDataFolder(),"config.yml"));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        sender.sendMessage(disabledMsg);
                    }else{
                        sender.sendMessage(nopermMsg);
                    }
                } else if (args[0].equalsIgnoreCase("list")){
                    if (sender.hasPermission("whitelistpp.list") || sender.isOp() || sender.hasPermission("whitelistpp.*")){
                        List<Object> list = getList();
                        int lenght = list.size();
                        int online = 0;
                        StringBuffer returnStr = new StringBuffer("");
                        returnStr.append(listMsgStart);
                        for (Object name: list){
                            Player pl = Bukkit.getPlayerExact(name.toString());
                            if (pl == null) {
                                returnStr.append(listMsgOffline.replace("%player%", name.toString())).append(listMsgSep);
                            } else {
                                online +=1;
                                returnStr.append(listMsgOnline.replace("%player%", name.toString())).append(listMsgSep);
                            }

                        }

                        returnStr.delete(returnStr.length()-2,returnStr.length());
                        String returnStrS = returnStr.toString().replace("%online%", String.valueOf(online)).replace("%lenght%", String.valueOf(lenght));
                        sender.sendMessage(returnStrS);

                    } else{
                        sender.sendMessage(nopermMsg);
                    }
                } else if (args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("rl")){
                    if (sender.hasPermission("whitelistpp.reload") || sender.isOp() || sender.hasPermission("whitelistpp.*")){
                        reloadMyConfig();
                        closeConnection();
                        openConnection();
                        sender.sendMessage(reloadMsg);

                    } else{
                        sender.sendMessage(nopermMsg);
                    }
                }


                else{
                    sender.sendMessage(usageMsg.replace("%command%"," /wpp add/del/on/off/list/rl [player]"));
                }
            }else{
                sender.sendMessage(usageMsg.replace("%command%"," /wpp add/del/on/off/list/rl [player]"));
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
                event.setKickMessage(getConfig().getString("messages.kick").replace("&","§"));
                event.setResult(Result.KICK_WHITELIST);
            }
        } else {
            addWhitelistOnline(event.getPlayer(), null);
        }
    }
}

