package top.speedcubing.minecraftproxy;

import io.netty.buffer.ByteBuf;

public class Utils {
    public static int readVarInt(ByteBuf input) {
        int out = 0;
        int bytes = 0;

        byte in;
        do {
            in = input.readByte();
            out |= (in & 127) << bytes++ * 7;
            if (bytes > 5) {
                throw new RuntimeException("VarInt too big");
            }
        } while ((in & 128) == 128);

        return out;
    }

    public static void writeVarInt(ByteBuf out, int i) {
        do {
            int part = i & 127;
            i >>>= 7;
            if (i != 0) {
                part |= 128;
            }

            out.writeByte(part);
        } while (i != 0);

    }

    public static String readString(ByteBuf buf) {
        int len = readVarInt(buf);
        byte[] b = new byte[len];
        buf.readBytes(b);
        return new String(b);
    }

    public static void writeString(ByteBuf buf, String s) {
        byte[] b = s.getBytes();
        writeVarInt(buf, b.length);
        buf.writeBytes(b);
    }

    public static void writeVarShort(ByteBuf buf, int i) {
        int low = i & 32767;
        int high = (i & 8355840) >> 15;
        if (high != 0) {
            low |= 32768;
        }

        buf.writeShort(low);
        if (high != 0) {
            buf.writeByte(high);
        }

    }
}
