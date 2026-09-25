package com.fennecmomo.maidquest.client;

import com.fennecmomo.fenneclib.template.Interface.IListItem;
import com.fennecmomo.fenneclib.template.List.ButtonListItem;
import com.fennecmomo.fenneclib.template.List.ContentList;
import com.fennecmomo.fenneclib.template.List.InputListItem;
import com.fennecmomo.maidquest.api.IQuestType;
import com.fennecmomo.maidquest.api.QuestTypeRegistry;
import com.fennecmomo.maidquest.block.entity.TaskBoardBlockEntity;
import com.fennecmomo.maidquest.network.QuestCancelPayload;
import com.fennecmomo.maidquest.network.QuestOpenStoragePayload;
import com.fennecmomo.maidquest.network.QuestPublishPayload;
import com.fennecmomo.maidquest.quest.Quest;
import com.fennecmomo.maidquest.quest.QuestStatus;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

// 任务板主界面——右键任务板打开。
//
// 左侧：委托列表（发布者可撤销自己的委托）
// 右侧：委托类型按钮 → 数量输入 → 发布
// 底部：交付仓库 / 关闭
public class QuestBoardScreen extends Screen
{
    private static final int ROW_H = 12;
    private static final int MARGIN_L = 20;
    private static final int MARGIN_T = 15;
    private static final int MARGIN_R = 20;
    private static final int BOTTOM_RESERVE = 40;
    private static final int RIGHT_COL_W = 96;
    private static final int TYPE_BTN_W = 72;
    private static final int TYPE_BTN_H = 18;
    private static final int MAX_QUANTITY_DIGITS = 4;
    private static final int COLOR_TITLE = 0xFFAA00;
    private static final int COLOR_OPEN = 0x55FF55;
    private static final int COLOR_CLAIMED = 0xFFAA00;
    private static final int COLOR_DONE = 0x888888;

    private final BlockPos boardPos;
    private TaskBoardBlockEntity cachedBoard;

    private ContentList questList;
    private ContentList typeButtonList;
    private ContentList detailList;
    private InputListItem quantityEntry;
    private IQuestType selectedType;
    // 委托列表签名：内容不变就不重建条目（避免每帧新增 widget）
    private String questSignature = "";

    public QuestBoardScreen(BlockPos boardPos)
    {
        super(Component.translatable("screen.maidquest.task_board"));
        this.boardPos = boardPos;
    }

    @Override
    protected void init()
    {
        super.init();
        selectedType = null;

        int listY = MARGIN_T + 14;
        int availableHeight = this.height - listY - BOTTOM_RESERVE;
        int listWidth = this.width - MARGIN_L - MARGIN_R - 180;

        this.addRenderableWidget(new StringWidget(MARGIN_L, MARGIN_T, 120, 10,
                Component.translatable("screen.maidquest.task_board").withStyle(Style.EMPTY.withColor(COLOR_TITLE)),
                this.font));

        questList = new ContentList(this::addRenderableWidget, MARGIN_L, listY, listWidth, availableHeight, ROW_H);

        int rightEdge = this.width - MARGIN_R;
        int rightY = MARGIN_T + 10;
        List<IListItem> typeButtons = new ArrayList<>();
        for (IQuestType type : QuestTypeRegistry.getEnabled())
        {
            typeButtons.add(new ButtonListItem(0, 0, TYPE_BTN_W, TYPE_BTN_H,
                    type.displayName().copy().withStyle(Style.EMPTY.withColor(type.icon().isEmpty() ? 0xFFFFFF : 0xFFFFFF)),
                    button -> selectType(type), this::addRenderableWidget));
        }
        typeButtonList = new ContentList(this::addRenderableWidget,
                rightEdge - TYPE_BTN_W - 4, rightY, TYPE_BTN_W + 4, 150, TYPE_BTN_H + 4);
        typeButtonList.setEntries(typeButtons);

        int detailLeft = rightEdge - TYPE_BTN_W - 8 - RIGHT_COL_W;
        detailList = new ContentList(this::addRenderableWidget, detailLeft, rightY, RIGHT_COL_W, 150, 22);

        quantityEntry = new InputListItem(0, 0, RIGHT_COL_W - 4, 16, this.font,
                Component.translatable("screen.maidquest.quantity"), this::addRenderableWidget);
        quantityEntry.setFilter(text -> text.matches("\\d*"));
        quantityEntry.setMaxLength(MAX_QUANTITY_DIGITS);
        quantityEntry.getInput().setValue("1");
        quantityEntry.deactivate();

        int buttonY = this.height - 30;
        this.addRenderableWidget(Button.builder(Component.translatable("screen.maidquest.storage"),
                        button -> openStorage())
                .pos(rightEdge - 60, buttonY).size(60, 16).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> this.onClose())
                .pos(rightEdge - 130, buttonY).size(60, 16).build());
    }

    private void selectType(IQuestType type)
    {
        selectedType = type;
        detailList.setEntries(List.of(quantityEntry, new ButtonListItem(0, 0, RIGHT_COL_W, TYPE_BTN_H,
                Component.translatable("screen.maidquest.publish"),
                button -> publish(), this::addRenderableWidget)));
    }

    private void publish()
    {
        if (selectedType == null || this.minecraft == null || this.minecraft.getConnection() == null)
        {
            return;
        }
        int quantity;
        try
        {
            quantity = Math.max(1, Integer.parseInt(quantityEntry.getInput().getValue()));
        }
        catch (NumberFormatException e)
        {
            quantity = 1;
        }
        this.minecraft.getConnection().send(new QuestPublishPayload(boardPos, selectedType.id(), quantity));
    }

    private void openStorage()
    {
        if (this.minecraft != null && this.minecraft.getConnection() != null)
        {
            this.minecraft.getConnection().send(new QuestOpenStoragePayload(boardPos));
        }
    }

    private TaskBoardBlockEntity getBoard()
    {
        if (cachedBoard != null && !cachedBoard.isRemoved())
        {
            return cachedBoard;
        }
        Level level = this.minecraft == null ? null : this.minecraft.level;
        if (level != null && level.getBlockEntity(boardPos) instanceof TaskBoardBlockEntity board)
        {
            cachedBoard = board;
            return board;
        }
        return null;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick)
    {
        graphics.fill(0, 0, this.width, this.height, 0xBB000000);
        refreshQuestListIfChanged();
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick)
    {
    }

    private void refreshQuestListIfChanged()
    {
        TaskBoardBlockEntity board = getBoard();
        List<Quest> quests = board == null ? List.of() : board.getQuests();
        StringBuilder signature = new StringBuilder();
        for (Quest quest : quests)
        {
            signature.append(quest.id()).append('|')
                    .append(quest.status()).append('|')
                    .append(quest.claimedByName().orElse("")).append(';');
        }
        if (signature.toString().equals(questSignature))
        {
            return;
        }
        questSignature = signature.toString();

        String playerName = this.minecraft != null && this.minecraft.player != null
                ? this.minecraft.player.getName().getString() : "";
        int listWidth = this.width - MARGIN_L - MARGIN_R - 180;

        List<IListItem> entries = new ArrayList<>();
        for (Quest quest : quests)
        {
            if (quest.status() == QuestStatus.CANCELLED)
            {
                continue;
            }
            Component text = Component.literal(formatLine(quest)).withStyle(Style.EMPTY.withColor(colorOf(quest)));
            if (quest.postedBy().equals(playerName))
            {
                entries.add(new ButtonListItem(0, 0, listWidth, ROW_H, this.font, text,
                        "×", 14, 10, button -> cancelQuest(quest), this::addRenderableWidget));
            }
            else
            {
                entries.add(new ButtonListItem(0, 0, listWidth, ROW_H, this.font, text, this::addRenderableWidget));
            }
        }
        questList.setEntries(entries);
    }

    private void cancelQuest(Quest quest)
    {
        if (this.minecraft != null && this.minecraft.getConnection() != null)
        {
            this.minecraft.getConnection().send(new QuestCancelPayload(boardPos, quest.id()));
        }
    }

    private static int colorOf(Quest quest)
    {
        return switch (quest.status())
        {
            case OPEN -> COLOR_OPEN;
            case CLAIMED, IN_PROGRESS -> COLOR_CLAIMED;
            case COMPLETED -> COLOR_DONE;
            default -> COLOR_DONE;
        };
    }

    private static String formatLine(Quest quest)
    {
        IQuestType type = QuestTypeRegistry.get(quest.typeId());
        String typeName = type == null ? quest.typeId().toString() : type.displayName().getString();
        String claimed = quest.claimedByName().orElse("-");
        return "[" + quest.status().getDisplayName() + "] "
                + typeName
                + " x" + quest.quantity()
                + "  " + claimed
                + "  by " + quest.postedBy();
    }

    // ======== 事件转发 ========

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY)
    {
        if (questList != null && questList.mouseDragged(event, dragX, dragY))
        {
            return true;
        }
        if (typeButtonList != null && typeButtonList.mouseDragged(event, dragX, dragY))
        {
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)
    {
        if (questList != null && questList.mouseScrolled(mouseX, mouseY, scrollX, scrollY))
        {
            return true;
        }
        if (typeButtonList != null && typeButtonList.mouseScrolled(mouseX, mouseY, scrollX, scrollY))
        {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
