package com.fennecmomo.maidquest.service;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.CombinedResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.function.Predicate;

// 女仆背包（TLM ItemManager）读写工具。
public final class QuestInventoryHelper
{
    private QuestInventoryHelper()
    {
    }

    // 统计可交付物数量（匹配 filter）。
    // 注意：必须用 getAvailableInv(false)，与 TLM dropResourcesToMaidInv 的掉落去处保持一致，
    // 否则会出现「检测还有空位、掉落却进不了背包掉地上」的错位。
    public static int count(EntityMaid maid, Predicate<Item> filter)
    {
        CombinedResourceHandler<ItemResource> inv = maid.getItemManager().getAvailableInv(false);
        int count = 0;
        for (int i = 0; i < inv.size(); i++)
        {
            ItemResource resource = inv.getResource(i);
            if (!resource.isEmpty() && filter.test(resource.getItem()))
            {
                count += (int) inv.getAmountAsLong(i);
            }
        }
        return count;
    }

    // 把匹配 filter 的物品移入目标容器，返回实际移入数量。
    public static int deposit(EntityMaid maid, Container target, Predicate<Item> filter, int maxAmount)
    {
        if (maxAmount <= 0) return 0;
        CombinedResourceHandler<ItemResource> inv = maid.getItemManager().getAvailableInv(false);
        int moved = 0;
        for (int i = 0; i < inv.size(); i++)
        {
            if (moved >= maxAmount) break;
            ItemResource resource = inv.getResource(i);
            if (resource.isEmpty() || !filter.test(resource.getItem()))
            {
                continue;
            }
            int amount = (int) Math.min(inv.getAmountAsLong(i), maxAmount - moved);
            if (amount <= 0)
            {
                continue;
            }
            try (Transaction transaction = Transaction.openRoot())
            {
                int extracted = inv.extract(i, resource, amount, transaction);
                if (extracted <= 0)
                {
                    continue;
                }
                ItemStack remainder = insert(target, resource.toStack(extracted));
                int accepted = extracted - remainder.getCount();
                if (accepted <= 0)
                {
                    continue;
                }
                if (remainder.getCount() > 0)
                {
                    inv.insert(i, resource, remainder.getCount(), transaction);
                }
                transaction.commit();
                moved += accepted;
            }
        }
        return moved;
    }

    // 把物品塞进容器（先合并同类，再找空格），返回塞不下的部分。
    public static ItemStack insert(Container container, ItemStack stack)
    {
        for (int i = 0; i < container.getContainerSize() && !stack.isEmpty(); i++)
        {
            ItemStack existing = container.getItem(i);
            if (existing.isEmpty() || !ItemStack.isSameItemSameComponents(existing, stack))
            {
                continue;
            }
            int space = existing.getMaxStackSize() - existing.getCount();
            if (space <= 0)
            {
                continue;
            }
            int move = Math.min(space, stack.getCount());
            existing.grow(move);
            stack.shrink(move);
            container.setItem(i, existing);
        }
        for (int i = 0; i < container.getContainerSize() && !stack.isEmpty(); i++)
        {
            if (!container.getItem(i).isEmpty())
            {
                continue;
            }
            int move = Math.min(stack.getMaxStackSize(), stack.getCount());
            container.setItem(i, stack.copyWithCount(move));
            stack.shrink(move);
        }
        return stack;
    }

    // 把背包里最好的斧子换到主手（没有则保持原状）。
    public static void equipAxe(EntityMaid maid)
    {
        if (maid.getMainHandItem().getItem() instanceof AxeItem)
        {
            return;
        }
        ResourceHandler<ItemResource> inv = maid.getItemManager().getMaidInv();
        for (int i = 0; i < inv.size(); i++)
        {
            ItemResource resource = inv.getResource(i);
            if (!(resource.getItem() instanceof AxeItem))
            {
                continue;
            }
            ItemStack oldHand = maid.getMainHandItem();
            try (Transaction transaction = Transaction.openRoot())
            {
                inv.extract(i, resource, 1, transaction);
                if (!oldHand.isEmpty())
                {
                    inv.insert(i, ItemResource.of(oldHand), oldHand.getCount(), transaction);
                }
                transaction.commit();
            }
            maid.setItemSlot(EquipmentSlot.MAINHAND, resource.toStack(1));
            return;
        }
    }
}
