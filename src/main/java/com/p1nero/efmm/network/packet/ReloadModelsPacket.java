package com.p1nero.efmm.network.packet;

import com.p1nero.efmm.gameasstes.EFMMArmatures;
import com.p1nero.efmm.gameasstes.EFMMMeshes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import yesman.epicfight.main.EpicFightMod;

import javax.annotation.Nullable;

public record ReloadModelsPacket() implements BasePacket {
    @Override
    public void encode(FriendlyByteBuf buf) {}

    public static ReloadModelsPacket decode(FriendlyByteBuf buf) {
        return new ReloadModelsPacket();
    }

    @Override
    public void execute(@Nullable Player player) {
        if(EpicFightMod.isPhysicalClient()){
            EFMMArmatures.reloadArmatures();
            EFMMMeshes.reloadMeshes();
        }
    }

}