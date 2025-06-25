package me.plugin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class AutoMessagePlugin extends JavaPlugin {

    public void onEnable() {
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            Bukkit.broadcastMessage("§6§l[בדיקה] §fזוהי הודעה אוטומטית לכל השחקנים!");
            
            // שליחת ניסיון לכל שחקן אחרי 3 שניות
            Bukkit.getScheduler().runTaskLater(this, () -> {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendMessage("§7(§fניסיון§7): §aזהו ניסיון הודעה אישי אליך.");
                }
            }, 60L); // 60 טיקים = 3 שניות
        }, 0L, 1200L); // כל 1200 טיקים = 60 שניות
    }
}
