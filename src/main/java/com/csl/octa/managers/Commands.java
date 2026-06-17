package com.csl.octa.managers;

import com.csl.octa.listeners.GrapplingHookListener;
import com.csl.octa.utils.Warp;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.*;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Commands {

    private final JavaPlugin plugin;
    private final Warp warp;
    private final ItemManager itemManager;
    private final GrapplingHookListener grapplingHookListener;
    private final MapManager mapManager;
    private final MiniMessage mm = MiniMessage.miniMessage();

    private enum TpaType { TPA, TPAHERE }

    private record TpaRequest(UUID senderUUID, UUID targetUUID, TpaType type, long timestamp) {
        boolean isExpired() {
            return (System.currentTimeMillis() - timestamp) > (TPA_EXPIRY_SECONDS * 1000L);
        }
    }

    private static final int TPA_EXPIRY_SECONDS = 60;

    private final Map<UUID, Set<TpaRequest>> pendingRequests = new ConcurrentHashMap<>();
    private final Set<UUID> autoAcceptPlayers = ConcurrentHashMap.newKeySet();

    public Commands(JavaPlugin plugin, Warp warp, ItemManager itemManager, GrapplingHookListener grapplingHookListener, MapManager mapManager) {
        this.plugin = plugin;
        this.warp = warp;
        this.itemManager = itemManager;
        this.grapplingHookListener = grapplingHookListener;
        this.mapManager = mapManager;
    }

    public void register() {
        new CommandAPICommand("octa")
                .withSubcommand(new CommandAPICommand("help")
                        .executesPlayer((plr, args) -> {
                            plr.sendMessage(mm.deserialize(
                                    "<dark_gray>----------</dark_gray> <gold>Octa</gold> <dark_gray>---------</dark_gray>\n" +
                                            "            <white>We currently do not have any commands!"
                            ));
                        }))
                .withSubcommand(new CommandAPICommand("give")
                        .withPermission("octa.admin")
                        .withArguments(
                                new StringArgument("item")
                                        .replaceSuggestions(ArgumentSuggestions.strings(info ->
                                                itemManager.getAllItems().keySet().toArray(new String[0])
                                        )),
                                new EntitySelectorArgument.ManyPlayers("players")
                        )
                        .withOptionalArguments(
                                new IntegerArgument("amount", 1, 64)
                        )
                        .executesPlayer((plr, args) -> {
                            String itemId = (String) args.get("item");
                            @SuppressWarnings("unchecked")
                            Collection<Player> targets = (Collection<Player>) args.get("players");
                            int amount = (int) args.getOrDefault("amount", 1);

                            if (itemManager.getCustomItem(itemId) == null) {
                                plr.sendMessage(mm.deserialize(
                                        "<red>Unknown item: <white>" + itemId
                                ));
                                return;
                            }

                            for (Player target : targets) {
                                itemManager.give(target, itemId, amount);
                            }

                            plr.sendMessage(mm.deserialize(
                                    "<green>Gave <white>x" + amount + " " + itemId + "</white> to <white>" + targets.size() + "</white> player(s)."
                            ));
                        }))
                .withSubcommand(new CommandAPICommand("map")
                        .withPermission("octa.admin")
                        .withSubcommand(new CommandAPICommand("list")
                                .executesPlayer((plr, args) -> {
                                    plr.openInventory(mapManager.createListGui(plr));
                                }))
                        .withSubcommand(new CommandAPICommand("remove")
                                .withArguments(new StringArgument("id_or_player"))
                                .executesPlayer((plr, args) -> {
                                    String input = (String) args.get("id_or_player");

                                    MapManager.MapData data = mapManager.getMap(input);
                                    if (data != null) {
                                        mapManager.removeMap(input);
                                        plr.sendMessage(mm.deserialize("<green>Removed map <gold>#" + input + "<green>."));
                                        return;
                                    }

                                    Player target = Bukkit.getPlayer(input);
                                    UUID targetUUID = null;

                                    if (target != null) {
                                        targetUUID = target.getUniqueId();
                                    } else {
                                        try {
                                            targetUUID = Bukkit.getOfflinePlayer(input).getUniqueId();
                                        } catch (Exception ignored) {}
                                    }

                                    if (targetUUID != null) {
                                        List<MapManager.MapData> removed = mapManager.removeAllByPlayer(targetUUID);
                                        if (removed.isEmpty()) {
                                            plr.sendMessage(mm.deserialize("<red>No maps found for player <white>" + input + "<red>."));
                                        } else {
                                            plr.sendMessage(mm.deserialize("<green>Removed <white>" + removed.size() + "</white> map(s) by <gold>" + input + "<green>."));
                                        }
                                    } else {
                                        plr.sendMessage(mm.deserialize("<red>No map or player found with: <white>" + input));
                                    }
                                })))
                .withSubcommand(new CommandAPICommand("debuggrapple")
                        .executesPlayer((plr, args) -> {
                            grapplingHookListener.debugInfo(plr);
                        }))
                .register();

        spawn().register();
        tpa().register();
        tpahere().register();
        tpaccept().register();
        tpdeny().register();
        tpauto().register();
    }

    // ---- SPAWN ----

    private CommandAPICommand spawn() {
        return new CommandAPICommand("spawn")
                .executesPlayer((sender, args) -> {
                    Player plr = sender;
                    Location location = plugin.getServer().getWorld("world").getSpawnLocation();

                    new BukkitRunnable() {
                        int countdown = 3;

                        @Override
                        public void run() {
                            if (!plr.isOnline() || plr.isDead()) {
                                this.cancel();
                                return;
                            }

                            if (countdown > 0) {
                                plr.playSound(Sound.sound(
                                        Key.key("minecraft", "block.amethyst_block.break"),
                                        Sound.Source.BLOCK, 1, 1
                                ));

                                plr.showTitle(Title.title(
                                        mm.deserialize("<#ffd663>Warping in " + countdown),
                                        mm.deserialize("<white>Do not move"),
                                        Title.Times.times(
                                                Duration.ofMillis(0),
                                                Duration.ofSeconds(2),
                                                Duration.ofMillis(0)
                                        )
                                ));
                                countdown--;
                            } else {
                                plr.playSound(Sound.sound(
                                        Key.key("minecraft", "block.respawn_anchor.deplete"),
                                        Sound.Source.BLOCK, 1, 1
                                ));

                                plr.clearTitle();
                                Warp.warpPlayerSpawn(plr, plr.getLocation(), location);

                                this.cancel();
                            }
                        }
                    }.runTaskTimer(plugin, 0, 20);
                });
    }

    // ---- TPA ----

    private CommandAPICommand tpa() {
        return new CommandAPICommand("tpa")
                .withArguments(new EntitySelectorArgument.OnePlayer("player"))
                .executesPlayer((sender, args) -> {
                    Player target = (Player) args.get("player");
                    if (target == null) {
                        sender.sendMessage(mm.deserialize("<red>Player not found."));
                        return;
                    }

                    if (target.getUniqueId().equals(sender.getUniqueId())) {
                        sender.sendMessage(mm.deserialize("<red>You can't teleport to yourself."));
                        return;
                    }

                    if (hasPendingRequest(sender.getUniqueId(), target.getUniqueId(), TpaType.TPA)) {
                        sender.sendMessage(mm.deserialize(
                                "<white>You already have a pending request to <#FFB675>" + target.getName() + "<white>."
                        ));
                        return;
                    }

                    TpaRequest request = new TpaRequest(sender.getUniqueId(), target.getUniqueId(),
                            TpaType.TPA, System.currentTimeMillis());
                    addRequest(request);
                    scheduleExpiry(request);

                    sender.sendMessage(mm.deserialize(
                            "<white>Teleport request sent to <#FFB675>" + target.getName() + "<white>."
                    ));
                    sender.playSound(Sound.sound(
                            Key.key("minecraft", "ui.button.click"),
                            Sound.Source.MASTER, 0.5f, 1.2f
                    ));

                    if (autoAcceptPlayers.contains(target.getUniqueId())) {
                        target.sendMessage(mm.deserialize(
                                "<white>Auto-accepted TPA from <#FFB675>" + sender.getName() + "<white>."
                        ));
                        acceptRequest(target, sender, request);
                        return;
                    }

                    sendTpaMessage(target, sender, TpaType.TPA);
                    target.playSound(Sound.sound(
                            Key.key("minecraft", "block.note_block.bell"),
                            Sound.Source.MASTER, 1.0f, 1.0f
                    ));
                });
    }

    private CommandAPICommand tpahere() {
        return new CommandAPICommand("tpahere")
                .withArguments(new EntitySelectorArgument.OnePlayer("player"))
                .executesPlayer((sender, args) -> {
                    Player target = (Player) args.get("player");
                    if (target == null) {
                        sender.sendMessage(mm.deserialize("<red>Player not found."));
                        return;
                    }

                    if (target.getUniqueId().equals(sender.getUniqueId())) {
                        sender.sendMessage(mm.deserialize("<red>You can't teleport to yourself."));
                        return;
                    }

                    if (hasPendingRequest(sender.getUniqueId(), target.getUniqueId(), TpaType.TPAHERE)) {
                        sender.sendMessage(mm.deserialize(
                                "<white>You already have a pending request to <#FFB675>" + target.getName() + "<white>."
                        ));
                        return;
                    }

                    TpaRequest request = new TpaRequest(sender.getUniqueId(), target.getUniqueId(),
                            TpaType.TPAHERE, System.currentTimeMillis());
                    addRequest(request);
                    scheduleExpiry(request);

                    sender.sendMessage(mm.deserialize(
                            "<white>Teleport-here request sent to <#FFB675>" + target.getName() + "<white>."
                    ));
                    sender.playSound(Sound.sound(
                            Key.key("minecraft", "ui.button.click"),
                            Sound.Source.MASTER, 0.5f, 1.2f
                    ));

                    if (autoAcceptPlayers.contains(target.getUniqueId())) {
                        target.sendMessage(mm.deserialize(
                                "<white>Auto-accepted TPA-here from <#FFB675>" + sender.getName() + "<white>."
                        ));
                        acceptRequest(target, sender, request);
                        return;
                    }

                    sendTpaMessage(target, sender, TpaType.TPAHERE);
                    target.playSound(Sound.sound(
                            Key.key("minecraft", "block.note_block.bell"),
                            Sound.Source.MASTER, 1.0f, 1.0f
                    ));
                });
    }

    private CommandAPICommand tpaccept() {
        return new CommandAPICommand("tpaccept")
                .withOptionalArguments(new EntitySelectorArgument.OnePlayer("player"))
                .executesPlayer((sender, args) -> {
                    Player specifiedSender = (Player) args.get("player");

                    Set<TpaRequest> requests = pendingRequests.get(sender.getUniqueId());
                    if (requests == null || requests.isEmpty()) {
                        sender.sendMessage(mm.deserialize("<red>You have no pending teleport requests."));
                        return;
                    }

                    requests.removeIf(TpaRequest::isExpired);
                    if (requests.isEmpty()) {
                        sender.sendMessage(mm.deserialize("<red>You have no pending teleport requests."));
                        return;
                    }

                    TpaRequest request;

                    if (specifiedSender != null) {
                        request = requests.stream()
                                .filter(r -> r.senderUUID().equals(specifiedSender.getUniqueId()))
                                .findFirst()
                                .orElse(null);

                        if (request == null) {
                            sender.sendMessage(mm.deserialize(
                                    "<red>No pending request from <#FFB675>" + specifiedSender.getName() + "<red>."
                            ));
                            return;
                        }
                    } else {
                        request = requests.stream()
                                .max(Comparator.comparingLong(TpaRequest::timestamp))
                                .orElse(null);

                        if (request == null) {
                            sender.sendMessage(mm.deserialize("<red>You have no pending teleport requests."));
                            return;
                        }
                    }

                    Player requestSender = Bukkit.getPlayer(request.senderUUID());
                    if (requestSender == null || !requestSender.isOnline()) {
                        sender.sendMessage(mm.deserialize("<red>That player is no longer online."));
                        requests.remove(request);
                        return;
                    }

                    acceptRequest(sender, requestSender, request);
                });
    }

    private CommandAPICommand tpdeny() {
        return new CommandAPICommand("tpdeny")
                .withOptionalArguments(new EntitySelectorArgument.OnePlayer("player"))
                .executesPlayer((sender, args) -> {
                    Player specifiedSender = (Player) args.get("player");

                    Set<TpaRequest> requests = pendingRequests.get(sender.getUniqueId());
                    if (requests == null || requests.isEmpty()) {
                        sender.sendMessage(mm.deserialize("<red>You have no pending teleport requests."));
                        return;
                    }

                    requests.removeIf(TpaRequest::isExpired);
                    if (requests.isEmpty()) {
                        sender.sendMessage(mm.deserialize("<red>You have no pending teleport requests."));
                        return;
                    }

                    TpaRequest request;

                    if (specifiedSender != null) {
                        request = requests.stream()
                                .filter(r -> r.senderUUID().equals(specifiedSender.getUniqueId()))
                                .findFirst()
                                .orElse(null);

                        if (request == null) {
                            sender.sendMessage(mm.deserialize(
                                    "<red>No pending request from <#FFB675>" + specifiedSender.getName() + "<red>."
                            ));
                            return;
                        }
                    } else {
                        request = requests.stream()
                                .max(Comparator.comparingLong(TpaRequest::timestamp))
                                .orElse(null);

                        if (request == null) {
                            sender.sendMessage(mm.deserialize("<red>You have no pending teleport requests."));
                            return;
                        }
                    }

                    Player requestSender = Bukkit.getPlayer(request.senderUUID());
                    requests.remove(request);

                    String senderName = requestSender != null ? requestSender.getName() : "???";

                    sender.sendMessage(mm.deserialize(
                            "<white>Denied teleport request from <#FFB675>" + senderName + "<white>."
                    ));
                    sender.playSound(Sound.sound(
                            Key.key("minecraft", "block.note_block.bass"),
                            Sound.Source.MASTER, 1.0f, 0.5f
                    ));

                    if (requestSender != null && requestSender.isOnline()) {
                        requestSender.sendMessage(mm.deserialize(
                                "<#FFB675>" + sender.getName() + " <red>denied your teleport request."
                        ));
                        requestSender.playSound(Sound.sound(
                                Key.key("minecraft", "block.note_block.bass"),
                                Sound.Source.MASTER, 1.0f, 0.5f
                        ));
                    }
                });
    }

    private CommandAPICommand tpauto() {
        return new CommandAPICommand("tpauto")
                .withOptionalArguments(new MultiLiteralArgument("toggle",
                        "yes", "no", "true", "false", "allow", "deny"))
                .executesPlayer((sender, args) -> {
                    String toggle = (String) args.get("toggle");

                    boolean currentlyEnabled = autoAcceptPlayers.contains(sender.getUniqueId());

                    boolean enable;
                    if (toggle == null) {
                        enable = !currentlyEnabled;
                    } else {
                        enable = switch (toggle.toLowerCase()) {
                            case "yes", "true", "allow" -> true;
                            case "no", "false", "deny" -> false;
                            default -> !currentlyEnabled;
                        };
                    }

                    if (enable) {
                        autoAcceptPlayers.add(sender.getUniqueId());
                        sender.sendMessage(mm.deserialize(
                                "<white>TPA auto-accept <green>enabled<white>. All incoming requests will be auto-accepted."
                        ));
                    } else {
                        autoAcceptPlayers.remove(sender.getUniqueId());
                        sender.sendMessage(mm.deserialize(
                                "<white>TPA auto-accept <red>disabled<white>."
                        ));
                    }

                    sender.playSound(Sound.sound(
                            Key.key("minecraft", "ui.button.click"),
                            Sound.Source.MASTER, 0.5f, enable ? 1.5f : 0.8f
                    ));
                });
    }

    private void sendTpaMessage(Player target, Player sender, TpaType type) {
        String typeText = type == TpaType.TPA
                ? "Requested to teleport to you!"
                : "Requested you teleport to them!";

        target.sendMessage(mm.deserialize(
                "<#FFB675>" + sender.getName() + " <white>" + typeText + " " +
                        "<green><bold><hover:show_text:'<green>Click to accept'><click:run_command:'/tpaccept " + sender.getName() + "'>ACCEPT</click></hover></bold></green>" +
                        " <dark_gray>|</dark_gray> " +
                        "<red><bold><hover:show_text:'<red>Click to deny'><click:run_command:'/tpdeny " + sender.getName() + "'>DENY</click></hover></bold></red>"
        ));
    }

    private void acceptRequest(Player target, Player requestSender, TpaRequest request) {
        Set<TpaRequest> requests = pendingRequests.get(target.getUniqueId());
        if (requests != null) requests.remove(request);

        Player teleporter;
        Player destination;

        if (request.type() == TpaType.TPA) {
            teleporter = requestSender;
            destination = target;
        } else {
            teleporter = target;
            destination = requestSender;
        }

        target.sendMessage(mm.deserialize(
                "<white>Accepted teleport request from <#FFB675>" + requestSender.getName() + "<white>."
        ));
        target.playSound(Sound.sound(
                Key.key("minecraft", "block.note_block.chime"),
                Sound.Source.MASTER, 1.0f, 1.5f
        ));

        requestSender.sendMessage(mm.deserialize(
                "<#FFB675>" + target.getName() + " <green>accepted your teleport request!"
        ));
        requestSender.playSound(Sound.sound(
                Key.key("minecraft", "block.note_block.chime"),
                Sound.Source.MASTER, 1.0f, 1.5f
        ));

        startTpaCountdown(teleporter, destination);
    }

    private void startTpaCountdown(Player teleporter, Player destination) {
        new BukkitRunnable() {
            int countdown = 3;

            @Override
            public void run() {
                if (!teleporter.isOnline() || teleporter.isDead()) {
                    this.cancel();
                    if (destination.isOnline()) {
                        destination.sendMessage(mm.deserialize(
                                "<#FFB675>" + teleporter.getName() + " <red>is no longer available."
                        ));
                    }
                    return;
                }
                if (!destination.isOnline() || destination.isDead()) {
                    this.cancel();
                    teleporter.sendMessage(mm.deserialize(
                            "<#FFB675>" + destination.getName() + " <red>is no longer available."
                    ));
                    return;
                }

                if (countdown > 0) {
                    teleporter.playSound(Sound.sound(
                            Key.key("minecraft", "block.amethyst_block.break"),
                            Sound.Source.BLOCK, 1.0f, 1.0f
                    ));

                    teleporter.showTitle(Title.title(
                            mm.deserialize("<#ffd663>Warping in " + countdown),
                            mm.deserialize("<white>Do not move"),
                            Title.Times.times(
                                    Duration.ofMillis(0),
                                    Duration.ofSeconds(2),
                                    Duration.ofMillis(0)
                            )
                    ));

                    countdown--;
                } else {
                    teleporter.playSound(Sound.sound(
                            Key.key("minecraft", "block.respawn_anchor.deplete"),
                            Sound.Source.BLOCK, 1.0f, 1.0f
                    ));

                    teleporter.clearTitle();
                    Warp.warpPlayerTpa(teleporter, teleporter.getLocation(), destination.getLocation());

                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0, 20);
    }

    private void addRequest(TpaRequest request) {
        pendingRequests.computeIfAbsent(request.targetUUID(), k -> ConcurrentHashMap.newKeySet())
                .add(request);
    }

    private boolean hasPendingRequest(UUID senderUUID, UUID targetUUID, TpaType type) {
        Set<TpaRequest> requests = pendingRequests.get(targetUUID);
        if (requests == null) return false;
        requests.removeIf(TpaRequest::isExpired);
        return requests.stream().anyMatch(r ->
                r.senderUUID().equals(senderUUID) && r.type() == type);
    }

    private void scheduleExpiry(TpaRequest request) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Set<TpaRequest> requests = pendingRequests.get(request.targetUUID());
            if (requests != null && requests.remove(request)) {
                Player sender = Bukkit.getPlayer(request.senderUUID());
                Player target = Bukkit.getPlayer(request.targetUUID());

                String targetName = target != null ? target.getName() : "???";
                String senderName = sender != null ? sender.getName() : "???";

                if (sender != null && sender.isOnline()) {
                    sender.sendMessage(mm.deserialize(
                            "<white>Your teleport request to <#FFB675>" + targetName + " <gray>has expired."
                    ));
                }
                if (target != null && target.isOnline()) {
                    target.sendMessage(mm.deserialize(
                            "<white>Teleport request from <#FFB675>" + senderName + " <gray>has expired."
                    ));
                }
            }
        }, TPA_EXPIRY_SECONDS * 20L);
    }

    public void cleanup() {
        pendingRequests.clear();
        autoAcceptPlayers.clear();
    }
}