package fuzs.netherchest.networking;

import fuzs.puzzleslib.network.Message;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class ClientboundContainerSetSlotMessage implements Message<ClientboundContainerSetSlotMessage> {
    private int containerId;
    private int stateId;
    private int slot;
    private ItemStack itemStack;

    public ClientboundContainerSetSlotMessage() {

    }

    public ClientboundContainerSetSlotMessage(int i, int j, int k, ItemStack itemStack) {
        this.containerId = i;
        this.stateId = j;
        this.slot = k;
        this.itemStack = itemStack.copy();
    }

    @Override
    public void read(FriendlyByteBuf friendlyByteBuf) {
        this.containerId = friendlyByteBuf.readByte();
        this.stateId = friendlyByteBuf.readVarInt();
        this.slot = friendlyByteBuf.readShort();
        this.itemStack = ByteBufItemUtils.readItem(friendlyByteBuf);
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeByte(this.containerId);
        buffer.writeVarInt(this.stateId);
        buffer.writeShort(this.slot);
        ByteBufItemUtils.writeItem(buffer, this.itemStack);
    }

    @Override
    public MessageHandler<ClientboundContainerSetSlotMessage> makeHandler() {
        return new MessageHandler<ClientboundContainerSetSlotMessage>() {

            @Override
            public void handle(ClientboundContainerSetSlotMessage message, Player player, Object instance) {
                Minecraft minecraft = (Minecraft) instance;
                ItemStack itemStack = message.getItem();
                int i = message.getSlot();
                minecraft.getTutorial().onGetItem(itemStack);
                if (message.getContainerId() == -1) {
                    if (!(minecraft.screen instanceof CreativeModeInventoryScreen)) {
                        player.containerMenu.setCarried(itemStack);
                    }
                } else if (message.getContainerId() == -2) {
                    player.getInventory().setItem(i, itemStack);
                } else {
                    boolean bl = false;
                    if (minecraft.screen instanceof CreativeModeInventoryScreen) {
                        CreativeModeInventoryScreen creativeModeInventoryScreen = (CreativeModeInventoryScreen)minecraft.screen;
                        bl = creativeModeInventoryScreen.getSelectedTab() != CreativeModeTab.TAB_INVENTORY.getId();
                    }

                    if (message.getContainerId() == 0 && InventoryMenu.isHotbarSlot(i)) {
                        if (!itemStack.isEmpty()) {
                            ItemStack itemStack2 = player.inventoryMenu.getSlot(i).getItem();
                            if (itemStack2.isEmpty() || itemStack2.getCount() < itemStack.getCount()) {
                                itemStack.setPopTime(5);
                            }
                        }

                        player.inventoryMenu.setItem(i, message.getStateId(), itemStack);
                    } else if (message.getContainerId() == player.containerMenu.containerId && (message.getContainerId() != 0 || !bl)) {
                        player.containerMenu.setItem(i, message.getStateId(), itemStack);
                    }
                }
            }
        };
    }

    public int getContainerId() {
        return this.containerId;
    }

    public int getSlot() {
        return this.slot;
    }

    public ItemStack getItem() {
        return this.itemStack;
    }

    public int getStateId() {
        return this.stateId;
    }
}
