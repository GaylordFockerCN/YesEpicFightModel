package com.p1nero.efmm.client.gui.screen;

import com.p1nero.efmm.client.gui.widget.TexturedModelPreviewer;
import com.p1nero.efmm.efmodel.ClientModelManager;
import com.p1nero.efmm.gameasstes.EFMMArmatures;
import com.p1nero.efmm.gameasstes.EFMMMeshes;
import io.netty.util.internal.StringUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import yesman.epicfight.api.client.model.AnimatedMesh;
import yesman.epicfight.api.client.model.MeshProvider;
import yesman.epicfight.client.gui.datapack.screen.MessageScreen;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.gameasset.Armatures;

import java.util.Comparator;
import java.util.function.BiConsumer;

@OnlyIn(Dist.CLIENT)
public class SelectEFModelScreen extends Screen {
    private ModelList modelList;
    private TexturedModelPreviewer texturedModelPreviewer;
    private EditBox searchBox;
    private final BiConsumer<String, MeshProvider<AnimatedMesh>> selectCallback;
    private final BiConsumer<String, MeshProvider<AnimatedMesh>> cancelCallback;

    public SelectEFModelScreen(BiConsumer<String, MeshProvider<AnimatedMesh>> selectCallback, BiConsumer<String, MeshProvider<AnimatedMesh>> cancelCallback) {
        super(Component.translatable("gui.efmm.select.models"));
        this.selectCallback = selectCallback;
        this.cancelCallback = cancelCallback;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public void refreshModelList() {
        this.modelList.refreshModelList(this.searchBox.getValue());
    }

    @Override
    protected void init() {
        this.texturedModelPreviewer = new TexturedModelPreviewer(10, 20, 36, 60, null, null, null, null);
        this.modelList = new ModelList(Minecraft.getInstance(), this.width, this.height, 36, this.height - 16, 21);
        this.modelList.setRenderTopAndBottom(false);
        this.searchBox = new EditBox(Minecraft.getInstance().font, this.width / 2, 12, this.width / 2 - 12, 16, Component.literal("select_ef_model.keyword"));
        this.searchBox.setResponder(this.modelList::refreshModelList);

        this.modelList.refreshModelList(null);

        int split = this.width / 2 - 80;

        this.texturedModelPreviewer._setWidth(split - 10);
        this.texturedModelPreviewer._setHeight(this.height - 68);
        this.texturedModelPreviewer.resize(null);

        this.modelList.updateSize(this.width - split, this.height, 36, this.height - 32);
        this.modelList.setLeftPos(split);

        this.searchBox.setX(this.width / 2);
        this.searchBox.setY(12);
        this.searchBox.setWidth(this.width / 2 - 12);
        this.searchBox.setHeight(16);

        this.addRenderableWidget(this.searchBox);
        this.addRenderableWidget(this.texturedModelPreviewer);
        this.addRenderableWidget(this.modelList);

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_OK, (button) -> {
            if (this.modelList.getSelected() == null) {
                Minecraft.getInstance().setScreen(new MessageScreen<>("", "Please select a model from the list", this, (button$2) -> {
                    Minecraft.getInstance().setScreen(this);
                }, 180, 60));
            } else {
                try {
                    this.selectCallback.accept(this.modelList.getSelected().modelId, this.modelList.getSelected().mesh);
                    this.onClose();
                } catch (Exception e) {
                    Minecraft.getInstance().setScreen(null);
                }
            }

        }).pos(this.width / 2 - 162, this.height - 28).size(160, 21).build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, (button) -> {
            this.cancelCallback.accept(StringUtils.EMPTY, null);
            this.onClose();
        }).pos(this.width / 2 + 2, this.height - 28).size(160, 21).build());
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (this.texturedModelPreviewer.mouseDragged(mouseX, mouseY, button, dx, dy)) {
            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(null);
        this.texturedModelPreviewer.onDestroy();
    }

    @Override
    public void tick() {
        this.texturedModelPreviewer._tick();
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @OnlyIn(Dist.CLIENT)
    class ModelList extends ObjectSelectionList<ModelList.ModelEntry> {
        
        public ModelList(Minecraft minecraft, int width, int height, int y0, int y1, int itemHeight) {
            super(minecraft, width, height, y0, y1, itemHeight);
            this.setRenderBackground(false);//泥土丑甚
        }

        @Override
        public void setSelected(@Nullable ModelEntry selEntry) {
            super.setSelected(selEntry);
            if(selEntry != null){
                SelectEFModelScreen.this.texturedModelPreviewer.setMesh(selEntry.mesh);
                SelectEFModelScreen.this.texturedModelPreviewer.setArmature(EFMMArmatures.ARMATURES.getOrDefault(selEntry.modelId, Armatures.BIPED));
                SelectEFModelScreen.this.texturedModelPreviewer.addAnimationToPlay(Animations.BIPED_WALK);
                SelectEFModelScreen.this.texturedModelPreviewer.setTextureLocation(ClientModelManager.TEXTURE_CACHE.get(selEntry.modelId));
            }
        }

        @Override
        public int getRowWidth() {
            return this.width;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.x1 - 6;
        }

        public void refreshModelList(String keyward) {
            this.setScrollAmount(0.0D);
            this.children().clear();
            //刷新模型列表
            ClientModelManager.AUTHED_MODELS.stream().filter((modelId) -> StringUtil.isNullOrEmpty(keyward) || modelId.contains(keyward)).map((modelId) -> new ModelEntry(modelId, () -> ClientModelManager.getOrRequestMesh(modelId)))
                    .sorted(Comparator.comparing(entry$ -> entry$.modelId)).forEach(this::addEntry);
        }

        @OnlyIn(Dist.CLIENT)
        class ModelEntry extends ObjectSelectionList.Entry<ModelList.ModelEntry> {
            private final String modelId;
            private final MeshProvider<AnimatedMesh> mesh;

            public ModelEntry(String modelId, MeshProvider<AnimatedMesh> mesh) {
                this.modelId = modelId;
                this.mesh = mesh;
            }

            @Override
            public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTicks) {
                guiGraphics.drawString(Minecraft.getInstance().font, this.modelId, left + 5, top + 5, 16777215, false);
            }

            @Override
            public @NotNull Component getNarration() {
                return Component.translatable("narrator.select");
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (button == 0) {
                    if (ModelList.this.getSelected() == this) {
                        try {
                            SelectEFModelScreen.this.selectCallback.accept(this.modelId, this.mesh);
                            SelectEFModelScreen.this.onClose();
                        } catch (Exception e) {
                            Minecraft.getInstance().setScreen(null);
                        }

                        return true;
                    }

                    ModelList.this.setSelected(this);

                    return true;
                } else {
                    return false;
                }
            }
        }
    }
}
