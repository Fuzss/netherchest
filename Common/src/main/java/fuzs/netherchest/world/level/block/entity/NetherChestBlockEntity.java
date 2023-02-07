package fuzs.netherchest.world.level.block.entity;

import fuzs.netherchest.NetherChest;
import fuzs.netherchest.config.ServerConfig;
import fuzs.netherchest.init.ModRegistry;
import fuzs.netherchest.world.inventory.NetherChestMenu;
import fuzs.netherchest.world.inventory.UnlimitedContainerUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;

public class NetherChestBlockEntity extends ChestBlockEntity {
    private static final MutableComponent CONTAINER_TITLE = Component.translatable("container.nether_chest");

    private final ContainerOpenersCounter openersCounter;

    public NetherChestBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(ModRegistry.NETHER_CHEST_BLOCK_ENTITY_TYPE.get(), blockPos, blockState);
        this.setItems(NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY));
        this.openersCounter = new ContainerOpenersCounter() {

            @Override
            protected void onOpen(Level level, BlockPos pos, BlockState state) {
                level.playSound(null, (double) pos.getX() + 0.5, (double) pos.getY() + 0.5, (double) pos.getZ() + 0.5, SoundEvents.ENDER_CHEST_OPEN, SoundSource.BLOCKS, 0.5F, level.random.nextFloat() * 0.1F + 0.9F);
            }

            @Override
            protected void onClose(Level level, BlockPos pos, BlockState state) {
                level.playSound(null, (double) pos.getX() + 0.5, (double) pos.getY() + 0.5, (double) pos.getZ() + 0.5, SoundEvents.ENDER_CHEST_CLOSE, SoundSource.BLOCKS, 0.5F, level.random.nextFloat() * 0.1F + 0.9F);
            }

            @Override
            protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int count, int openCount) {
                level.blockEvent(pos, state.getBlock(), 1, openCount);
            }

            @Override
            protected boolean isOwnContainer(Player player) {
                if (player.containerMenu instanceof NetherChestMenu netherChestMenu) {
                    return netherChestMenu.getContainer() == NetherChestBlockEntity.this;
                } else {
                    return false;
                }
            }
        };
    }

    @Override
    public int getContainerSize() {
        return 54;
    }

    @Override
    protected Component getDefaultName() {
        return CONTAINER_TITLE;
    }

    @Override
    public int getMaxStackSize() {
        return super.getMaxStackSize() * NetherChest.CONFIG.get(ServerConfig.class).stackSizeMultiplier;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.setItems(NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY));
        if (!this.tryLoadLootTable(tag)) {
            UnlimitedContainerUtils.loadAllItems(tag, this.getItems());
        }

    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.remove("Items");
        if (!this.trySaveLootTable(tag)) {
            UnlimitedContainerUtils.saveAllItems(tag, this.getItems(), true);
        }
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new NetherChestMenu(containerId, inventory, this);
    }

    @Override
    public void startOpen(Player player) {
        if (!this.remove && !player.isSpectator()) {
            this.openersCounter.incrementOpeners(player, this.getLevel(), this.getBlockPos(), this.getBlockState());
        }
    }

    @Override
    public void stopOpen(Player player) {
        if (!this.remove && !player.isSpectator()) {
            this.openersCounter.decrementOpeners(player, this.getLevel(), this.getBlockPos(), this.getBlockState());
        }
    }

    @Override
    public void recheckOpen() {
        if (!this.remove) {
            this.openersCounter.recheckOpeners(this.getLevel(), this.getBlockPos(), this.getBlockState());
        }
    }
}
