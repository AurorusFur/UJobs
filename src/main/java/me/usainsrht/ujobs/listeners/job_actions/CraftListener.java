package me.usainsrht.ujobs.listeners.job_actions;

import me.usainsrht.ujobs.managers.JobManager;
import me.usainsrht.ujobs.models.BuiltInActions;
import me.usainsrht.ujobs.models.Job;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class CraftListener implements Listener {

    JobManager jobManager;

    public CraftListener(JobManager jobManager) {
        this.jobManager = jobManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (jobManager.shouldIgnore(player)) return;

        if (e.getRecipe() == null) return;

        if (jobManager.getActionJobMap().containsKey(BuiltInActions.Material.CRAFT)) {

            ItemStack result = e.getRecipe().getResult();
            int amount = switch (e.getAction()) {
                // Take result onto cursor (left/right click)
                case PICKUP_ALL, PICKUP_HALF -> canCursorAccept(player, result) ? result.getAmount() : 0;
                // Drop from result slot — crafts even with a full inventory
                case DROP_ONE_SLOT, DROP_ALL_SLOT -> result.getAmount();
                // Shift-click into inventory — only as many full crafts as fit
                case MOVE_TO_OTHER_INVENTORY -> getShiftCraftAmount(e, player, result);
                default -> 0;
            };

            if (amount <= 0) return;

            for (Job job : jobManager.getJobsWithAction(BuiltInActions.Material.CRAFT)) {
                jobManager.processAction(player, BuiltInActions.Material.CRAFT, result.getType().name(), job, amount);
            }
        }
    }

    private static int getShiftCraftAmount(CraftItemEvent e, Player player, ItemStack result) {
        int perCraft = Math.max(1, result.getAmount());
        int maxCraftsBySpace = getRemainingSpace(player.getInventory(), result.asOne()) / perCraft;
        if (maxCraftsBySpace <= 0) return 0;

        int lowest = Integer.MAX_VALUE;
        for (ItemStack item : e.getInventory().getMatrix()) {
            if (item != null && !item.isEmpty()) {
                lowest = Math.min(lowest, item.getAmount());
            }
        }
        if (lowest == Integer.MAX_VALUE) return 0;

        return Math.min(lowest, maxCraftsBySpace) * perCraft;
    }

    /** True when the cursor can hold one full craft result (empty or similar with enough stack room). */
    private static boolean canCursorAccept(Player player, ItemStack result) {
        ItemStack cursor = player.getItemOnCursor();
        if (cursor == null || cursor.isEmpty()) return true;
        if (!cursor.isSimilar(result)) return false;
        return cursor.getAmount() + result.getAmount() <= cursor.getMaxStackSize();
    }

    /** Remaining space in main inventory + hotbar only (not armor/offhand). */
    public static int getRemainingSpace(PlayerInventory inventory, ItemStack item) {
        int space = 0;
        for (ItemStack itemStack : inventory.getStorageContents()) {
            if (itemStack == null || itemStack.isEmpty()) space += item.getMaxStackSize();
            else if (item.isSimilar(itemStack)) space += item.getMaxStackSize() - itemStack.getAmount();
        }
        return space;
    }
}
