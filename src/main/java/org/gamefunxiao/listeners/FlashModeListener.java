package org.gamefunxiao.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockReceiveGameEvent;
import org.bukkit.event.block.MoistureChangeEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.TrialSpawnerSpawnEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInputEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleUpdateEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.gamefunxiao.GameFunXiao;
import io.papermc.paper.event.entity.EntityLoadCrossbowEvent;
import io.papermc.paper.event.player.PlayerStopUsingItemEvent;
import io.papermc.paper.event.player.PlayerShieldDisableEvent;

public class FlashModeListener implements Listener {

    private final GameFunXiao plugin;

    public FlashModeListener(GameFunXiao plugin) {
        this.plugin = plugin;
    }

    private boolean isFlashPreGameInteractionLocked(Player player) {
        return player != null && plugin.getFlashModeManager().isFlashPreGameInteractionLocked(player,
                plugin.getRoomManager().getPlayerRoom(player.getUniqueId()));
    }

    private boolean isEndFlashStartupPhase(Player player) {
        return player != null && plugin.getFlashModeManager().isEndFlashStartupPhase(player,
                plugin.getRoomManager().getPlayerRoom(player.getUniqueId()));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        plugin.getFlashModeManager().prepareAnvilResult(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPrepareSmithing(PrepareSmithingEvent event) {
        plugin.getFlashModeManager().prepareStableSwordSmithingResult(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        plugin.getFlashModeManager().handleFlashCraftPrepare(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onCraftItem(CraftItemEvent event) {
        plugin.getFlashModeManager().handleFlashCraftTake(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            if (isFlashPreGameInteractionLocked(player)) {
                return;
            }
            org.bukkit.Bukkit.getScheduler().runTask(plugin,
                    () -> plugin.getFlashModeManager().normalizeUnstableCoreShieldBlockingDelay(player));
            var room = plugin.getRoomManager().getPlayerRoom(player.getUniqueId());
            if (plugin.getFlashModeManager().handleStableSwordSmithingTake(event, player, room)) {
                return;
            }
            if (plugin.getFlashModeManager().handleFlashRoomGuideBookClick(event, player, room)) {
                return;
            }
            if (plugin.getFlashModeManager().handleFlashBackpackInventoryClick(event, player, room)) {
                return;
            }
            if (plugin.getFlashModeManager().handleUnstableMaceParticleToggle(event, player, room)) {
                return;
            }
            if (plugin.getFlashModeManager().handleDragonBreathWeaponInfusion(event, player, room)) {
                return;
            }
            if (plugin.getFlashModeManager().handleRailgunInfusion(event, player, room)) {
                return;
            }
            if (plugin.getFlashModeManager().handleMaterialUpgradeInfusion(event, player, room)) {
                return;
            }
        }
        if (plugin.getFlashModeManager().handleFlashInventoryArmorUpgrade(event)) {
            return;
        }
        if (plugin.getFlashModeManager().handleFlashBundleClick(event)) {
            return;
        }
        if (plugin.getFlashModeManager().handleInvisibleItemFrameInfusion(event)) {
            return;
        }
        plugin.getFlashModeManager().handleFlashAnvilTakeResult(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            if (isFlashPreGameInteractionLocked(player)) {
                return;
            }
            var room = plugin.getRoomManager().getPlayerRoom(player.getUniqueId());
            if (plugin.getFlashModeManager().handleFlashRoomGuideBookDrag(event, player, room)) {
                return;
            }
            plugin.getFlashModeManager().handleFlashBackpackInventoryDrag(event, player, room);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHangingPlace(HangingPlaceEvent event) {
        if (event.getPlayer() != null && isFlashPreGameInteractionLocked(event.getPlayer())) {
            event.setCancelled(true);
            return;
        }
        plugin.getFlashModeManager().handleInvisibleItemFramePlace(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (isFlashPreGameInteractionLocked(player)) {
            event.setCancelled(true);
            plugin.getFlashModeManager().cancelDispenserLauncherCharge(player, false);
            return;
        }
        if (plugin.getFlashModeManager().handleFlashRoomGuideBookSwap(player,
                plugin.getRoomManager().getPlayerRoom(player.getUniqueId()))) {
            event.setCancelled(true);
            return;
        }
        plugin.getFlashModeManager().cancelDispenserLauncherCharge(event.getPlayer(), false);
        org.bukkit.Bukkit.getScheduler().runTask(plugin,
                () -> plugin.getFlashModeManager().normalizeUnstableCoreShieldBlockingDelay(event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        plugin.getFlashModeManager().cancelDispenserLauncherCharge(event.getPlayer(), false);
        org.bukkit.Bukkit.getScheduler().runTask(plugin,
                () -> plugin.getFlashModeManager().normalizeUnstableCoreShieldBlockingDelay(event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerItemBreak(PlayerItemBreakEvent event) {
        plugin.getFlashModeManager().handleMaceShieldItemBreak(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerStopUsingItem(PlayerStopUsingItemEvent event) {
        plugin.getFlashModeManager().handleDispenserLauncherStopUsing(event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getFlashModeManager().cancelDispenserLauncherCharge(event.getPlayer(), false);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onPlayerDeath(PlayerDeathEvent event) {
        plugin.getFlashModeManager().cancelDispenserLauncherCharge(event.getEntity(), false);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        boolean endFlashStartupPearl = isEndFlashStartupPhase(player)
                && event.getItem() != null
                && event.getItem().getType() == org.bukkit.Material.ENDER_PEARL;
        if (isFlashPreGameInteractionLocked(player) && !endFlashStartupPearl) {
            event.setCancelled(true);
            event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
            event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
            plugin.getFlashModeManager().cancelDispenserLauncherCharge(player, false);
            return;
        }
        if (endFlashStartupPearl && event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
            event.setUseItemInHand(org.bukkit.event.Event.Result.ALLOW);
        }
        if (plugin.getFlashModeManager().handleFlashRoomGuideBookUse(event, player,
                plugin.getRoomManager().getPlayerRoom(player.getUniqueId()))) {
            return;
        }
        plugin.getFlashModeManager().normalizeHeldRedstoneStabilizers(event.getPlayer());
        plugin.getFlashModeManager().normalizeUnstableCoreShieldBlockingDelay(event.getPlayer());
        if (plugin.getFlashModeManager().handleFlashCustomBlockInteract(event)) {
            return;
        }
        if (plugin.getFlashModeManager().handleRailgunUse(event)) {
            return;
        }
        if (event.getHand() == org.bukkit.inventory.EquipmentSlot.OFF_HAND
                && plugin.getFlashModeManager().shouldBlockRecentCrossbowLoadOffhandUse(event.getPlayer())) {
            event.setCancelled(true);
            event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
            event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
            return;
        }
        if (plugin.getFlashModeManager().handleShieldWindChargeDash(event)) {
            return;
        }
        if (plugin.getFlashModeManager().handleEnhancedWindChargeUse(event)) {
            return;
        }
        if (plugin.getFlashModeManager().handleCondensedEnderPearlTeleport(event)) {
            return;
        }
        if (plugin.getFlashModeManager().handlePseudoPoisonPotatoInteract(event)) {
            return;
        }
        if (plugin.getFlashModeManager().handleFlashCoarseDirtInteract(event)) {
            return;
        }
        if (plugin.getFlashModeManager().handleDispenserLauncherCharge(event)) {
            return;
        }
        if (plugin.getFlashModeManager().handleFlashCrossbowLoad(event)) {
            return;
        }
        if (plugin.getFlashModeManager().handleFlashTamedEntityUseConflict(event)) {
            return;
        }
        if (plugin.getFlashModeManager().handleDriedGhastSnowBoost(event)) {
            return;
        }
        plugin.getFlashModeManager().handleFlashNoteBlockInteract(event);
        if (plugin.getFlashModeManager().handleMagicBrushDraw(event)) {
            return;
        }
        plugin.getFlashModeManager().handleJukeboxAuraInteract(event);
        plugin.getFlashModeManager().handleSpyglassFocus(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onTrialSpawnerSpawn(TrialSpawnerSpawnEvent event) {
        plugin.getFlashModeManager().handleHeroTrialSpawnerSpawn(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockReceiveGameEvent(BlockReceiveGameEvent event) {
        plugin.getFlashModeManager().handleSilentBootSculk(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onMoistureChange(MoistureChangeEvent event) {
        plugin.getFlashModeManager().handleFlashWetFarmlandMoistureChange(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockFade(BlockFadeEvent event) {
        plugin.getFlashModeManager().handleFlashWetFarmlandFade(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (isFlashPreGameInteractionLocked(event.getPlayer())) {
            event.setCancelled(true);
            return;
        }
        plugin.getFlashModeManager().handleFlashTamedEntityInteract(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        if (isFlashPreGameInteractionLocked(event.getPlayer())) {
            event.setCancelled(true);
            return;
        }
        plugin.getFlashModeManager().handleFlashFoodConsume(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerInput(PlayerInputEvent event) {
        plugin.getFlashModeManager().handleHappyGhastMountedSpeed(event.getPlayer(), event.getInput());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (isFlashPreGameInteractionLocked(player)) {
            event.setCancelled(true);
            return;
        }
        if (plugin.getFlashModeManager().handleFlashRoomGuideBookDrop(event, player,
                plugin.getRoomManager().getPlayerRoom(player.getUniqueId()))) {
            return;
        }
        plugin.getFlashModeManager().handleSwordWaveDrop(event, player, plugin.getRoomManager().getPlayerRoom(player.getUniqueId()));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        var room = plugin.getRoomManager().getPlayerRoom(player.getUniqueId());
        if (!plugin.getFlashModeManager().isFlashCombatAvailable(player)) {
            return;
        }
        plugin.getFlashModeManager().handleHappyGhastMountedSpeed(player);
        plugin.getFlashModeManager().handleFlashTamedMountControl(event, player, room);
        plugin.getFlashModeManager().handleUnstableMaceDisplacementLock(event, player, room);
        plugin.getFlashModeManager().handleFlashHoeTrapMove(event, player, room);
        plugin.getFlashModeManager().handleFlashFishingWaterTrapMove(event, player, room);
        plugin.getFlashModeManager().handleSilentBootStep(event, player, room);
        plugin.getFlashModeManager().handleShieldWindChargeAirBounceLanding(player, room);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (event.getEntity() instanceof Player player && isFlashPreGameInteractionLocked(player)) {
            event.setCancelled(true);
            return;
        }
        if (plugin.getFlashModeManager().handleFlashGlobalMobBowShoot(event)) {
            return;
        }
        plugin.getFlashModeManager().handleFlashBowShoot(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onEntityLoadCrossbow(EntityLoadCrossbowEvent event) {
        if (event.getEntity() instanceof Player player && isFlashPreGameInteractionLocked(player)) {
            event.setCancelled(true);
            return;
        }
        plugin.getFlashModeManager().handleFlashCrossbowLoad(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerShieldDisable(PlayerShieldDisableEvent event) {
        plugin.getFlashModeManager().handleUnstableCoreShieldDisable(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity().getShooter() instanceof Player player
                && isFlashPreGameInteractionLocked(player)
                && !(event.getEntity() instanceof EnderPearl && isEndFlashStartupPhase(player))) {
            event.setCancelled(true);
            return;
        }
        plugin.getFlashModeManager().handleProjectileLaunch(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onProjectileHit(ProjectileHitEvent event) {
        plugin.getFlashModeManager().handleDispenserLauncherFireballHit(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onCondensedSlimeBlockBreak(BlockBreakEvent event) {
        if (isFlashPreGameInteractionLocked(event.getPlayer())) {
            event.setCancelled(true);
            return;
        }
        plugin.getFlashModeManager().handleCondensedSlimeBlockBreak(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onTmtBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (isFlashPreGameInteractionLocked(player)) {
            event.setCancelled(true);
            return;
        }
        var room = plugin.getRoomManager().getPlayerRoom(player.getUniqueId());
        if (plugin.getFlashModeManager().handleTmtBlockBreak(event, player, room)) {
            return;
        }
        if (plugin.getFlashModeManager().handleCoalPickaxeFireTrapBreak(event, player, room)) {
            return;
        }
        plugin.getFlashModeManager().handlePseudoPoisonPotatoCropBreak(event, player, room);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onCoalPickaxeBlockBreak(BlockBreakEvent event) {
        if (isFlashPreGameInteractionLocked(event.getPlayer())) {
            event.setCancelled(true);
            return;
        }
        plugin.getFlashModeManager().handleCoalPickaxeBreak(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockDamage(BlockDamageEvent event) {
        if (isFlashPreGameInteractionLocked(event.getPlayer())) {
            event.setCancelled(true);
            return;
        }
        plugin.getFlashModeManager().handleFlashCustomBlockDamage(event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        plugin.getFlashModeManager().handleFlashSoilBlockBreak(event);
        plugin.getFlashModeManager().handleFlashPickaxeAreaBreak(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (isFlashPreGameInteractionLocked(player)) {
            event.setCancelled(true);
            return;
        }
        var room = plugin.getRoomManager().getPlayerRoom(player.getUniqueId());
        if (plugin.getFlashModeManager().handleTmtBlockPlace(event, player, room)) {
            return;
        }
        if (plugin.getFlashModeManager().handleFlashCoarseDirtPlace(event, player, room)) {
            return;
        }
        if (plugin.getFlashModeManager().handleFlashWetFarmlandCropPlace(event, player, room)) {
            return;
        }
        plugin.getFlashModeManager().handleCondensedSlimeBlockPlace(event);
        plugin.getFlashModeManager().handleJukeboxAuraPlace(event);
        plugin.getFlashModeManager().handleFlashPlayerBlockPlace(event, player, room);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onEntityTarget(EntityTargetEvent event) {
        plugin.getGameManager().handleLuckyPillarsMobTarget(event);
        if (event.getEntity().getType() == org.bukkit.entity.EntityType.ENDERMAN
                && event.getTarget() instanceof org.bukkit.entity.Player player) {
            org.gamefunxiao.game.GameRoom room = plugin.getRoomManager().getPlayerRoom(player.getUniqueId());
            if (room != null
                    && (room.getGameMode() == org.gamefunxiao.game.GameMode.END_FLASH
                    || room.getGameMode() == org.gamefunxiao.game.GameMode.END_CHAPTER)
                    && !room.isGameActuallyStarted()) {
                event.setTarget(null);
                event.setCancelled(true);
                return;
            }
        }
        plugin.getFlashModeManager().handleFlashTamedTarget(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Player preGameAttacker = resolveAttackingPlayer(event.getDamager());
        if (isFlashPreGameInteractionLocked(preGameAttacker)
                || (event.getEntity() instanceof Player player && isFlashPreGameInteractionLocked(player))) {
            event.setCancelled(true);
            return;
        }
        if (plugin.getFlashModeManager().handleDispenserFireballExplosionDamage(event)) {
            return;
        }
        if (plugin.getFlashModeManager().handleDispenserLauncherFireballDamage(event)) {
            return;
        }
        plugin.getFlashModeManager().handleFlashGlobalSkeletonPayloadDamage(event);
        if (plugin.getFlashModeManager().handleTntMinecartHoeHit(event)) {
            return;
        }
        plugin.getFlashModeManager().handleFlashTamedDamage(event);
        plugin.getFlashModeManager().handleCondensedEnderPearlEndermanDamage(event);

        Player attacker = resolveAttackingPlayer(event.getDamager());
        if (attacker != null && event.getEntity() instanceof LivingEntity victim
                && plugin.getFlashModeManager().isFlashCombatAvailable(attacker)) {
            var room = plugin.getRoomManager().getPlayerRoom(attacker.getUniqueId());
            plugin.getFlashModeManager().prepareSpearKineticThreshold(attacker, room);
            plugin.getFlashModeManager().applyMeleeUpgradeBonus(event, attacker, victim, room);
            plugin.getFlashModeManager().rememberOwnerCombatTarget(attacker, victim);
            if (!event.isCancelled() && event.getDamage() > 0.0D && victim instanceof Player victimPlayer) {
                plugin.getFlashModeManager().rememberOwnerCombatTarget(victimPlayer, attacker);
            }
        }
    }

    private Player resolveAttackingPlayer(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && isFlashPreGameInteractionLocked(player)) {
            event.setCancelled(true);
            player.setFallDistance(0.0F);
            return;
        }
        plugin.getFlashModeManager().handleDispenserFireballExplosionDamage(event);
        plugin.getFlashModeManager().handleFlashEnderDragonDamage(event);
        plugin.getFlashModeManager().handleHappyGhastChestplateDamage(event);
        plugin.getFlashModeManager().handleTntArmorExplosionDamage(event);
        if (plugin.getFlashModeManager().handleCondensedSlimeBlockFallDamage(event)) {
            return;
        }
        if (event.getEntity() instanceof Player player) {
            plugin.getFlashModeManager().handleSilentBootFallDamage(event, player, plugin.getRoomManager().getPlayerRoom(player.getUniqueId()));
        }
        if (plugin.getFlashModeManager().handleTotemArmorFatalDamage(event)) {
            return;
        }
        plugin.getFlashModeManager().handleFlashTamedFallDamage(event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntitySpawn(EntitySpawnEvent event) {
        plugin.getFlashModeManager().handleTmtPrimedSpawn(event);
        plugin.getFlashModeManager().handleFlashLingeringCloudSpawn(event);
        plugin.getFlashModeManager().handleFlashEnderDragonSpawn(event);
        plugin.getFlashModeManager().handleFlashGlobalMobSpawn(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onEntityDeath(EntityDeathEvent event) {
        plugin.getFlashModeManager().handleFlashGlobalMobDeath(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onExplosionPrime(ExplosionPrimeEvent event) {
        plugin.getFlashModeManager().handleTmtExplosionPrime(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onEntityExplode(EntityExplodeEvent event) {
        plugin.getFlashModeManager().handleTmtEntityExplode(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockExplode(BlockExplodeEvent event) {
        plugin.getFlashModeManager().handleTmtBlockExplode(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onEntityResurrect(EntityResurrectEvent event) {
        plugin.getFlashModeManager().handleTotemArmorResurrect(event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        plugin.getFlashModeManager().handleOwnerCrossDimensionTeleport(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockDispense(BlockDispenseEvent event) {
        plugin.getFlashModeManager().handleDispenserSwordWave(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerFish(PlayerFishEvent event) {
        if (isFlashPreGameInteractionLocked(event.getPlayer())) {
            event.setCancelled(true);
            return;
        }
        plugin.getFlashModeManager().handleFishingRodLength(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        plugin.getFlashModeManager().handleBucketSwordChance(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerBucketFill(PlayerBucketFillEvent event) {
        plugin.getFlashModeManager().handleEnchantedBucketFill(event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVehicleUpdate(VehicleUpdateEvent event) {
        plugin.getFlashModeManager().handleHappyGhastHarnessSpeed(event);
    }
}
