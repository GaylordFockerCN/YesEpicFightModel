package com.p1nero.efmm.network;


import com.mojang.logging.LogUtils;
import com.p1nero.efmm.network.packet.RegisterModelPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;

public class ModelPacketSplitter {
    private static final ModelPacketSplitter INSTANCE = new ModelPacketSplitter();
    protected final UUID clientUUID = UUID.randomUUID();
    private final int SPLIT_BEGIN_FLAG = -2;
    private final int SPLIT_BODY_FLAG = -1;
    private final int SPLIT_END_FLAG = -3;

    private final HashMap<UUID, ArrayList<ByteBuf>> receivedBuffers = new HashMap<>();
    private final ExecutorService workThread = Executors.newFixedThreadPool(2, "EFMM-NET/N-ED");

    private static final Logger LOGGER = LogUtils.getLogger();

    protected ModelPacketSplitter(){

    }

    public static ModelPacketSplitter getInstance() {
        return INSTANCE;
    }

    public  <T>  void split(final RegisterModelPacket message, Function<FriendlyByteBuf, T> builder, int partSize, Consumer<T> consumer) {
        workThread.submit(() -> {
            var buffer = Unpooled.buffer();
            writePacket(message, buffer);
            buffer.capacity(buffer.readableBytes());
            int bufferSize = buffer.readableBytes();
            //符合天道禁锢就直接发
            if (bufferSize <= partSize) {
                consumer.accept(builder.apply(new FriendlyByteBuf(buffer)));
                return;
            }
            //包太大就切割
            for (int index = 0; index < bufferSize; index += partSize) {
                var partPrefix = Unpooled.buffer(4);
                if (index == 0) {
                    partPrefix.writeInt(SPLIT_BEGIN_FLAG);
                } else if ((index + partSize) >= bufferSize) {
                    partPrefix.writeInt(SPLIT_END_FLAG);
                } else {
                    partPrefix.writeInt(SPLIT_BODY_FLAG);
                }
                int resolvedPartSize = Math.min(bufferSize - index, partSize);
                var buffer1 = Unpooled.wrappedBuffer(partPrefix, buffer.readBytes(resolvedPartSize));
                var packet = builder.apply(new FriendlyByteBuf(buffer1));
                consumer.accept(packet);
            }
            buffer.release();
        });
    }

    public void merge(@Nullable UUID uuid, ByteBuf bufferIn, Consumer<RegisterModelPacket> consumer) {
        var buffer = bufferIn.asByteBuf();
        int packetState = buffer.getInt(0);
        //拼尸块，按uuid堆起来
        if (packetState < 0) {
            var playerReceivedBuffers = receivedBuffers.computeIfAbsent(uuid == null ? clientUUID : uuid, k -> new ArrayList<>());
            if (packetState == SPLIT_BEGIN_FLAG) {
                if (!playerReceivedBuffers.isEmpty()) {
                    LOGGER.warn("EFMM : split received out of order - inbound buffer not empty when receiving first");
                    playerReceivedBuffers.clear();
                }
            }
            buffer.skipBytes(4); // skip header
            playerReceivedBuffers.add(buffer.retainedDuplicate()); // we need to keep writer/reader index
            if (packetState == SPLIT_END_FLAG) {
                workThread.submit(() -> {
                    // ownership will transfer to full buffer, so don't call release again.
                    var full = Unpooled.wrappedBuffer(playerReceivedBuffers.toArray(new ByteBuf[0]));
                    playerReceivedBuffers.clear();
                    consumer.accept(readPacket(full));
                    full.release();
                });
            }
            return;
        }
        //全尸直接处理
        if (buffer.readableBytes() < 3000) { // 3k
            consumer.accept(readPacket(buffer));
            return;
        }
        var receivedBuf = buffer.retainedDuplicate(); // we need to keep writer/reader index
        workThread.submit(() -> {
            consumer.accept(readPacket(receivedBuf));
            receivedBuf.release();
        });
    }

    private void writePacket(RegisterModelPacket message, ByteBuf buffer) {
        message.encode(new FriendlyByteBuf(buffer));
    }

    private RegisterModelPacket readPacket(ByteBuf buffer) {
        return RegisterModelPacket.decode(new FriendlyByteBuf(buffer));
    }
}
