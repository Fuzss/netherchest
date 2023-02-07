package fuzs.netherchest.networking;

import fuzs.netherchest.NetherChest;
import net.minecraft.core.NonNullList;
import net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.item.ItemStack;

public class UnlimitedContainerSynchronizer implements ContainerSynchronizer {
    private final ServerPlayer player;

    public UnlimitedContainerSynchronizer(ServerPlayer player) {
        this.player = player;
    }

    @Override
    public void sendInitialData(AbstractContainerMenu container, NonNullList<ItemStack> items, ItemStack carriedItem, int[] is) {
        NetherChest.NETWORK.sendTo(new ClientboundContainerSetContentMessage(container.containerId, container.incrementStateId(), items, carriedItem), this.player);

        for (int i = 0; i < is.length; ++i) {
            this.broadcastDataValue(container, i, is[i]);
        }
    }

    @Override
    public void sendSlotChange(AbstractContainerMenu container, int slot, ItemStack itemStack) {
        NetherChest.NETWORK.sendTo(new ClientboundContainerSetSlotMessage(container.containerId, container.incrementStateId(), slot, itemStack), this.player);
    }

    @Override
    public void sendCarriedChange(AbstractContainerMenu containerMenu, ItemStack stack) {
        NetherChest.NETWORK.sendTo(new ClientboundContainerSetSlotMessage(-1, containerMenu.incrementStateId(), -1, stack), this.player);
    }

    @Override
    public void sendDataChange(AbstractContainerMenu container, int id, int value) {
        this.broadcastDataValue(container, id, value);
    }

    private void broadcastDataValue(AbstractContainerMenu container, int id, int value) {
        this.player.connection.send(new ClientboundContainerSetDataPacket(container.containerId, id, value));
    }
}
