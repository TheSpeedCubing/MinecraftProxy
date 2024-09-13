package top.speedcubing.mcproxy.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.DecoderException;
import java.util.List;
import top.speedcubing.mcproxy.session.Session;

// com.velocitypowered.proxy.protocol.netty.MinecraftVarintFrameDecoder
public class MinecraftVarintFrameDecoder extends ByteToMessageDecoder {
    private static final DecoderException BAD_LENGTH_CACHED =
            new DecoderException("Bad packet length");
    private static final DecoderException VARINT_BIG_CACHED =
            new DecoderException("VarInt too big");

    boolean clientBound;
    Session session;
    public MinecraftVarintFrameDecoder(Session session,boolean clientBound) {
        this.session = session;
        this.clientBound = clientBound;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (!ctx.channel().isActive()) {
            in.clear();
            return;
        }


        final VarintByteDecoder reader = new VarintByteDecoder();

        int varintEnd = in.forEachByte(reader);
        if (varintEnd == -1) {
            // We tried to go beyond the end of the buffer. This is probably a good sign that the
            // buffer was too short to hold a proper varint.
            if (reader.getResult() == VarintByteDecoder.DecodeResult.RUN_OF_ZEROES) {
                // Special case where the entire top.speedcubing.mcproxy.packet is just a run of zeroes. We ignore them all.
                in.clear();
            }
            return;
        }

        if (reader.getResult() == VarintByteDecoder.DecodeResult.RUN_OF_ZEROES) {
            // this will return to the point where the next varint starts
            in.readerIndex(varintEnd);
        } else if (reader.getResult() == VarintByteDecoder.DecodeResult.SUCCESS) {
            int readVarint = reader.getReadVarint();

            int bytesRead = reader.getBytesRead();

            if (readVarint < 0) {
                in.clear();
                throw BAD_LENGTH_CACHED;
            } else if (readVarint == 0) {
                in.readerIndex(varintEnd + 1);
            } else {

                if (clientBound)
                    session.clientPacketLength = readVarint;
                else
                    session.serverPacketLength = readVarint;

                int minimumRead = bytesRead + readVarint;
                if (in.isReadable(minimumRead)) {
                    ByteBuf byteBuf = in.retainedSlice(varintEnd + 1, readVarint);
                    out.add(byteBuf);
                    in.skipBytes(minimumRead);
                }
            }
        } else if (reader.getResult() == VarintByteDecoder.DecodeResult.TOO_BIG) {
            in.clear();
            throw VARINT_BIG_CACHED;
        }
    }
}
