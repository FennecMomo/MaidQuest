package com.fennecmomo.maidquest.client;

import com.fennecmomo.maidquest.menu.TaskBoardMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

// 任务板交付仓库界面。
//
// 自绘面板（原版灰底 + 槽位凹陷 + 标题/背包标签），右上角「返回」回委托板界面。
public class TaskBoardStorageScreen extends AbstractContainerScreen<TaskBoardMenu>
{
    private static final Identifier CHEST_TEXTURE =
            Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final int PANEL_COLOR = 0xFFC6C6C6;
    private static final int BORDER_COLOR = 0xFF555555;
    private static final int LABEL_COLOR = 0xFF404040;
    // 原版箱子贴图里 18×18 槽位凹陷精灵的左上角
    private static final int SLOT_SPRITE_U = 7;
    private static final int SLOT_SPRITE_V = 17;
    private static final int SLOT_SPRITE_SIZE = 18;
    private static final int BACK_BUTTON_WIDTH = 60;
    private static final int BACK_BUTTON_HEIGHT = 14;

    public TaskBoardStorageScreen(TaskBoardMenu menu, Inventory inventory, Component title)
    {
        super(menu, inventory, title, menu.getImageWidth(), menu.getImageHeight());
    }

    @Override
    protected void init()
    {
        super.init();
        this.addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> backToBoard())
                .bounds(this.leftPos + this.imageWidth - BACK_BUTTON_WIDTH - 6, this.topPos + 3,
                        BACK_BUTTON_WIDTH, BACK_BUTTON_HEIGHT)
                .build());
    }

    private void backToBoard()
    {
        if (this.minecraft != null)
        {
            this.minecraft.setScreen(new QuestBoardScreen(this.menu.getBoardPos()));
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick)
    {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, this.width, this.height, 0xBB000000);
        graphics.fill(this.leftPos, this.topPos,
                this.leftPos + this.imageWidth, this.topPos + this.imageHeight, PANEL_COLOR);
        graphics.outline(this.leftPos, this.topPos, this.imageWidth, this.imageHeight, BORDER_COLOR);
        for (Slot slot : this.menu.slots)
        {
            graphics.blit(RenderPipelines.GUI_TEXTURED, CHEST_TEXTURE,
                    this.leftPos + slot.x - 1, this.topPos + slot.y - 1,
                    SLOT_SPRITE_U, SLOT_SPRITE_V, SLOT_SPRITE_SIZE, SLOT_SPRITE_SIZE, 256, 256);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY)
    {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, LABEL_COLOR, false);
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY,
                LABEL_COLOR, false);
    }
}
