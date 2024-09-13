package top.speedcubing.mcproxy.packet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.ReferenceCountUtil;
import java.nio.BufferOverflowException;

public class PacketUtils {

    public static void checkReadable(ByteBuf buf) {
        if (buf.readableBytes() != 0) {
            throw new BufferOverflowException();
        }
        ReferenceCountUtil.release(buf);
    }
}
