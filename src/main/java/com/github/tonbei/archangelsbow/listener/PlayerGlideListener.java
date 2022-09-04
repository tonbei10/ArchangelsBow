package com.github.tonbei.archangelsbow.listener;

import com.github.tonbei.archangelsbow.ArchangelsBow;
import com.github.tonbei.archangelsbow.util.ABUtil;
import com.github.tonbei.archangelsbow.util.PacketUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

public class PlayerGlideListener implements Listener {

    public static final String AB_GLIDE_META_KEY = "ArchangelsBow:Glide";

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent e) {
        Player player = e.getPlayer();
        boolean cancelGlideFlag = false;

        if (player.getMetadata(AB_GLIDE_META_KEY).stream().anyMatch(MetadataValue::asBoolean)) {
            if (player.isOnGround() || player.isFlying() || player.isInWaterOrBubbleColumn() || player.isInLava()) {
                cancelGlideFlag = true;
            }
        } else if (player.getFallDistance() > 1.5f) {
            if (!player.hasMetadata(AB_GLIDE_META_KEY)) {
                ItemStack chestplate = player.getInventory().getChestplate();
                if (chestplate == null || chestplate.getType() != Material.ELYTRA) {
                    PacketUtil.sendEquipmentPacket(player, ABUtil.getPacketElytra(), player.getWorld().getPlayers());
                    player.setMetadata(AB_GLIDE_META_KEY, new FixedMetadataValue(ArchangelsBow.getInstance(), false));
                }
            }
        } else {
            if (player.hasMetadata(AB_GLIDE_META_KEY)) {
                cancelGlideFlag = true;
            }
        }

        if (cancelGlideFlag) {
            player.removeMetadata(AB_GLIDE_META_KEY, ArchangelsBow.getInstance());
            PacketUtil.sendEquipmentPacket(player, player.getInventory().getArmorContents()[2], player.getWorld().getPlayers());
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerGlide(EntityToggleGlideEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        Player player = (Player) e.getEntity();

        if (player.hasMetadata(AB_GLIDE_META_KEY)) {
            if (player.getMetadata(AB_GLIDE_META_KEY).stream().noneMatch(MetadataValue::asBoolean)) {
                player.setMetadata(AB_GLIDE_META_KEY, new FixedMetadataValue(ArchangelsBow.getInstance(), true));
                player.setGliding(true);
            }
            e.setCancelled(true);
        }
    }
}
