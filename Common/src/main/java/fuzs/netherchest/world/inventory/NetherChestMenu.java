package fuzs.netherchest.world.inventory;

import com.google.common.collect.Sets;
import fuzs.netherchest.init.ModRegistry;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.Registry;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class NetherChestMenu extends ChestMenu {
    private final Set<Slot> quickcraftSlots = Sets.newHashSet();
    private int quickcraftType = -1;
    private int quickcraftStatus;

    public NetherChestMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(54) {

            @Override
            public int getMaxStackSize() {
                return super.getMaxStackSize() * 8;
            }
        });
    }

    public NetherChestMenu(int containerId, Inventory inventory, Container container) {
        super(ModRegistry.NETHER_CHEST_MENU_TYPE.get(), containerId, inventory, container, 6);
        for (int l = 0; l < 6; ++l) {
            for (int m = 0; m < 9; ++m) {
                Slot slot = new Slot(container, m + l * 9, 8 + m * 18, 18 + l * 18) {

                    @Override
                    public int getMaxStackSize(ItemStack stack) {
                        if (stack.getMaxStackSize() > 1 || !stack.isDamageableItem() || !stack.isDamaged()) {
                            return stack.getMaxStackSize() * 8;
                        }
                        return super.getMaxStackSize(stack);
                    }
                };
                slot.index = this.slots.set(l * 9 + m, slot).index;
            }
        }
    }

    public void setActualSynchronizer(ContainerSynchronizer synchronizer) {
        super.setSynchronizer(synchronizer);
        super.sendAllDataToRemote();
    }

    public void sendAllDataToRemote() {

    }

    public void broadcastFullState() {
        super.broadcastFullState();
        super.sendAllDataToRemote();
    }

    public static void getQuickCraftSlotCount(Set<Slot> dragSlots, int dragMode, ItemStack stack, int slotStackSize, Slot slot) {
        switch (dragMode) {
            case 0:
                stack.setCount(Mth.floor((float) stack.getCount() / (float) dragSlots.size()));
                break;
            case 1:
                stack.setCount(1);
                break;
            case 2:
                stack.setCount(slot.getMaxStackSize(stack));
        }

        stack.grow(slotStackSize);
    }

    public static boolean canItemQuickReplace(@Nullable Slot slot, ItemStack stack, boolean stackSizeMatters) {
        boolean bl = slot == null || !slot.hasItem();
        if (!bl && ItemStack.isSameItemSameTags(stack, slot.getItem())) {
            return slot.getItem().getCount() + (stackSizeMatters ? 0 : stack.getCount()) <= slot.getMaxStackSize(stack);
        } else {
            return bl;
        }
    }

    public static int getRedstoneSignalFromBlockEntity(@Nullable BlockEntity blockEntity) {
        return blockEntity instanceof Container ? getRedstoneSignalFromContainer((Container) blockEntity) : 0;
    }

    public static int getRedstoneSignalFromContainer(@Nullable Container container) {
        if (container == null) {
            return 0;
        } else {
            int i = 0;
            float f = 0.0F;

            for (int j = 0; j < container.getContainerSize(); ++j) {
                ItemStack itemStack = container.getItem(j);
                if (!itemStack.isEmpty()) {
                    f += (float) itemStack.getCount() / (float) Math.min(container.getMaxStackSize(), itemStack.getMaxStackSize());
                    ++i;
                }
            }

            f /= (float) container.getContainerSize();
            return Mth.floor(f * 14.0F) + (i > 0 ? 1 : 0);
        }
    }

    @Override
    public void clicked(int mouseX, int mouseY, ClickType clickType, Player player) {
        try {
            this.doClick(mouseX, mouseY, clickType, player);
        } catch (Exception var8) {
            CrashReport crashReport = CrashReport.forThrowable(var8, "Container click");
            CrashReportCategory crashReportCategory = crashReport.addCategory("Click info");
            crashReportCategory.setDetail("Menu Type", () -> {
                Objects.requireNonNull(this.getType(), "menu type is null");
                return Registry.MENU.getKey(this.getType()).toString();
            });
            crashReportCategory.setDetail("Menu Class", () -> this.getClass().getCanonicalName());
            crashReportCategory.setDetail("Slot Count", this.slots.size());
            crashReportCategory.setDetail("Slot", mouseX);
            crashReportCategory.setDetail("Button", mouseY);
            crashReportCategory.setDetail("Type", clickType);
            throw new ReportedException(crashReport);
        }
    }

    private void doClick(int mouseX, int mouseY, ClickType clickType, Player player) {
        Inventory inventory = player.getInventory();
        if (clickType == ClickType.QUICK_CRAFT) {
            int i = this.quickcraftStatus;
            this.quickcraftStatus = getQuickcraftHeader(mouseY);
            if ((i != 1 || this.quickcraftStatus != 2) && i != this.quickcraftStatus) {
                this.resetQuickCraft();
            } else if (this.getCarried().isEmpty()) {
                this.resetQuickCraft();
            } else if (this.quickcraftStatus == 0) {
                this.quickcraftType = getQuickcraftType(mouseY);
                if (isValidQuickcraftType(this.quickcraftType, player)) {
                    this.quickcraftStatus = 1;
                    this.quickcraftSlots.clear();
                } else {
                    this.resetQuickCraft();
                }
            } else if (this.quickcraftStatus == 1) {
                Slot slot = this.slots.get(mouseX);
                ItemStack itemStack = this.getCarried();
                if (canItemQuickReplace(slot, itemStack, true) && slot.mayPlace(itemStack) && (this.quickcraftType == 2 || itemStack.getCount() > this.quickcraftSlots.size()) && this.canDragTo(slot)) {
                    this.quickcraftSlots.add(slot);
                }
            } else if (this.quickcraftStatus == 2) {
                if (!this.quickcraftSlots.isEmpty()) {
                    if (this.quickcraftSlots.size() == 1) {
                        int j = this.quickcraftSlots.iterator().next().index;
                        this.resetQuickCraft();
                        this.doClick(j, this.quickcraftType, ClickType.PICKUP, player);
                        return;
                    }

                    ItemStack itemStack2 = this.getCarried().copy();
                    int k = this.getCarried().getCount();

                    for (Slot slot2 : this.quickcraftSlots) {
                        ItemStack itemStack3 = this.getCarried();
                        if (slot2 != null && canItemQuickReplace(slot2, itemStack3, true) && slot2.mayPlace(itemStack3) && (this.quickcraftType == 2 || itemStack3.getCount() >= this.quickcraftSlots.size()) && this.canDragTo(slot2)) {
                            ItemStack itemStack4 = itemStack2.copy();
                            int l = slot2.hasItem() ? slot2.getItem().getCount() : 0;
                            getQuickCraftSlotCount(this.quickcraftSlots, this.quickcraftType, itemStack4, l, slot2);
                            int m = slot2.getMaxStackSize(itemStack4);
                            if (itemStack4.getCount() > m) {
                                itemStack4.setCount(m);
                            }

                            k -= itemStack4.getCount() - l;
                            slot2.set(itemStack4);
                        }
                    }

                    itemStack2.setCount(k);
                    this.setCarried(itemStack2);
                }

                this.resetQuickCraft();
            } else {
                this.resetQuickCraft();
            }
        } else if (this.quickcraftStatus != 0) {
            this.resetQuickCraft();
        } else if ((clickType == ClickType.PICKUP || clickType == ClickType.QUICK_MOVE) && (mouseY == 0 || mouseY == 1)) {
            ClickAction clickAction = mouseY == 0 ? ClickAction.PRIMARY : ClickAction.SECONDARY;
            if (mouseX == -999) {
                if (!this.getCarried().isEmpty()) {
                    if (clickAction == ClickAction.PRIMARY) {
                        player.drop(this.getCarried(), true);
                        this.setCarried(ItemStack.EMPTY);
                    } else {
                        player.drop(this.getCarried().split(1), true);
                    }
                }
            } else if (clickType == ClickType.QUICK_MOVE) {
                if (mouseX < 0) {
                    return;
                }

                Slot slot = this.slots.get(mouseX);
                if (!slot.mayPickup(player)) {
                    return;
                }

                ItemStack itemStack = this.quickMoveStack(player, mouseX);

                while (!itemStack.isEmpty() && ItemStack.isSame(slot.getItem(), itemStack)) {
                    itemStack = this.quickMoveStack(player, mouseX);
                }
            } else {
                if (mouseX < 0) {
                    return;
                }

                Slot slot = this.slots.get(mouseX);
                ItemStack itemStack = slot.getItem();
                ItemStack itemStack5 = this.getCarried();
                player.updateTutorialInventoryAction(itemStack5, slot.getItem(), clickAction);
                if (!itemStack5.overrideStackedOnOther(slot, clickAction, player) && !itemStack.overrideOtherStackedOnMe(itemStack5, slot, clickAction, player, this.createCarriedSlotAccess())) {
                    if (itemStack.isEmpty()) {
                        if (!itemStack5.isEmpty()) {
                            int n = clickAction == ClickAction.PRIMARY ? itemStack5.getCount() : 1;
                            this.setCarried(slot.safeInsert(itemStack5, n));
                        }
                    } else if (slot.mayPickup(player)) {
                        if (itemStack5.isEmpty()) {
                            int n = clickAction == ClickAction.PRIMARY ? itemStack.getCount() : (itemStack.getCount() + 1) / 2;
                            Optional<ItemStack> optional = slot.tryRemove(n, Integer.MAX_VALUE, player);
                            optional.ifPresent(itemStack22 -> {
                                this.setCarried(itemStack22);
                                slot.onTake(player, itemStack22);
                            });
                        } else if (slot.mayPlace(itemStack5)) {
                            if (ItemStack.isSameItemSameTags(itemStack, itemStack5)) {
                                int n = clickAction == ClickAction.PRIMARY ? itemStack5.getCount() : 1;
                                this.setCarried(slot.safeInsert(itemStack5, n));
                            } else if (itemStack5.getCount() <= slot.getMaxStackSize(itemStack5)) {
                                this.setCarried(itemStack);
                                slot.set(itemStack5);
                            }
                        } else if (ItemStack.isSameItemSameTags(itemStack, itemStack5)) {
                            Optional<ItemStack> optional2 = slot.tryRemove(itemStack.getCount(), slot.getMaxStackSize(itemStack5) - itemStack5.getCount(), player);
                            optional2.ifPresent(itemStack2x -> {
                                itemStack5.grow(itemStack2x.getCount());
                                slot.onTake(player, itemStack2x);
                            });
                        }
                    }
                }

                slot.setChanged();
            }
        } else if (clickType == ClickType.SWAP) {
            Slot slot3 = this.slots.get(mouseX);
            ItemStack itemStack2 = inventory.getItem(mouseY);
            ItemStack itemStack = slot3.getItem();
            if (!itemStack2.isEmpty() || !itemStack.isEmpty()) {
                if (itemStack2.isEmpty()) {
                    if (slot3.mayPickup(player)) {
                        inventory.setItem(mouseY, itemStack);
//                        slot3.onSwapCraft(itemStack.getCount());
                        slot3.set(ItemStack.EMPTY);
                        slot3.onTake(player, itemStack);
                    }
                } else if (itemStack.isEmpty()) {
                    if (slot3.mayPlace(itemStack2)) {
                        int o = slot3.getMaxStackSize(itemStack2);
                        if (itemStack2.getCount() > o) {
                            slot3.set(itemStack2.split(o));
                        } else {
                            inventory.setItem(mouseY, ItemStack.EMPTY);
                            slot3.set(itemStack2);
                        }
                    }
                } else if (slot3.mayPickup(player) && slot3.mayPlace(itemStack2)) {
                    int o = slot3.getMaxStackSize(itemStack2);
                    if (itemStack2.getCount() > o) {
                        slot3.set(itemStack2.split(o));
                        slot3.onTake(player, itemStack);
                        if (!inventory.add(itemStack)) {
                            player.drop(itemStack, true);
                        }
                    } else {
                        inventory.setItem(mouseY, itemStack);
                        slot3.set(itemStack2);
                        slot3.onTake(player, itemStack);
                    }
                }
            }
        } else if (clickType == ClickType.CLONE && player.getAbilities().instabuild && this.getCarried().isEmpty() && mouseX >= 0) {
            Slot slot3 = this.slots.get(mouseX);
            if (slot3.hasItem()) {
                ItemStack itemStack2 = slot3.getItem().copy();
                itemStack2.setCount(slot3.getMaxStackSize(itemStack2));
                this.setCarried(itemStack2);
            }
        } else if (clickType == ClickType.THROW && this.getCarried().isEmpty() && mouseX >= 0) {
            Slot slot3 = this.slots.get(mouseX);
            int j = mouseY == 0 ? 1 : slot3.getItem().getCount();
            ItemStack itemStack = slot3.safeTake(j, Integer.MAX_VALUE, player);
            player.drop(itemStack, true);
        } else if (clickType == ClickType.PICKUP_ALL && mouseX >= 0) {
            Slot slot3 = this.slots.get(mouseX);
            ItemStack itemStack2 = this.getCarried();
            if (!itemStack2.isEmpty() && (!slot3.hasItem() || !slot3.mayPickup(player))) {
                int k = mouseY == 0 ? 0 : this.slots.size() - 1;
                int o = mouseY == 0 ? 1 : -1;

                for (int n = 0; n < 2; ++n) {
                    for (int p = k; p >= 0 && p < this.slots.size() && itemStack2.getCount() < this.slots.get(p).getMaxStackSize(itemStack2); p += o) {
                        Slot slot4 = this.slots.get(p);
                        if (slot4.hasItem() && canItemQuickReplace(slot4, itemStack2, true) && slot4.mayPickup(player) && this.canTakeItemForPickAll(itemStack2, slot4)) {
                            ItemStack itemStack6 = slot4.getItem();
                            if (n != 0 || itemStack6.getCount() != slot4.getMaxStackSize(itemStack6)) {
                                ItemStack itemStack7 = slot4.safeTake(itemStack6.getCount(), slot4.getMaxStackSize(itemStack2) - itemStack2.getCount(), player);
                                itemStack2.grow(itemStack7.getCount());
                            }
                        }
                    }
                }
            }
        }

    }

    @Override
    protected void resetQuickCraft() {
        this.quickcraftStatus = 0;
        this.quickcraftSlots.clear();
    }

    private SlotAccess createCarriedSlotAccess() {
        return new SlotAccess() {

            @Override
            public ItemStack get() {
                return NetherChestMenu.this.getCarried();
            }

            @Override
            public boolean set(ItemStack carried) {
                NetherChestMenu.this.setCarried(carried);
                return true;
            }
        };
    }

    @Override
    protected boolean moveItemStackTo(ItemStack stack, int startIndex, int endIndex, boolean reverseDirection) {
        boolean bl = false;
        int i = startIndex;
        if (reverseDirection) {
            i = endIndex - 1;
        }

        if (stack.getMaxStackSize() > 1 || !stack.isDamageableItem() || !stack.isDamaged()) {
            while (!stack.isEmpty()) {
                if (reverseDirection) {
                    if (i < startIndex) {
                        break;
                    }
                } else if (i >= endIndex) {
                    break;
                }

                Slot slot = this.slots.get(i);
                ItemStack itemStack = slot.getItem();
                if (!itemStack.isEmpty() && ItemStack.isSameItemSameTags(stack, itemStack)) {
                    int j = itemStack.getCount() + stack.getCount();
                    if (j <= slot.getMaxStackSize(stack)) {
                        stack.setCount(0);
                        itemStack.setCount(j);
                        slot.setChanged();
                        bl = true;
                    } else if (itemStack.getCount() < slot.getMaxStackSize(stack)) {
                        stack.shrink(slot.getMaxStackSize(stack) - itemStack.getCount());
                        itemStack.setCount(slot.getMaxStackSize(stack));
                        slot.setChanged();
                        bl = true;
                    }
                }

                if (reverseDirection) {
                    --i;
                } else {
                    ++i;
                }
            }
        }

        if (!stack.isEmpty()) {
            if (reverseDirection) {
                i = endIndex - 1;
            } else {
                i = startIndex;
            }

            while (true) {
                if (reverseDirection) {
                    if (i < startIndex) {
                        break;
                    }
                } else if (i >= endIndex) {
                    break;
                }

                Slot slot = this.slots.get(i);
                ItemStack itemStack = slot.getItem();
                if (itemStack.isEmpty() && slot.mayPlace(stack)) {
                    if (stack.getCount() > slot.getMaxStackSize(stack)) {
                        slot.set(stack.split(slot.getMaxStackSize(stack)));
                    } else {
                        slot.set(stack.split(stack.getCount()));
                    }

                    slot.setChanged();
                    bl = true;
                    break;
                }

                if (reverseDirection) {
                    --i;
                } else {
                    ++i;
                }
            }
        }

        return bl;
    }
}