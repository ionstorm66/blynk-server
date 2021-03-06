package cc.blynk.utils;

import cc.blynk.server.core.protocol.enums.Command;
import cc.blynk.server.core.protocol.model.messages.MessageBase;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.CharsetUtil;

import static cc.blynk.server.core.protocol.enums.Response.*;

/**
 * Utility class that creates native netty buffers instead of java objects.
 * This is done in order to allocate less java objects and reduce GC pauses and load.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 10.03.16.
 */
public class ByteBufUtil {

    public static ByteBuf ok(ChannelHandlerContext ctx, int msgId) {
        return makeResponse(ctx.alloc(), msgId, OK);
    }

    public static ByteBuf ok(Channel channel, int msgId) {
        return makeResponse(channel.alloc(), msgId, OK);
    }

    public static ByteBuf makeResponse(ChannelHandlerContext ctx, int msgId, int response) {
        return makeResponse(ctx.alloc(), msgId, response);
    }

    public static ByteBuf makeResponse(Channel channel, int msgId, int response) {
        return makeResponse(channel.alloc(), msgId, response);
    }

    private static ByteBuf makeResponse(ByteBufAllocator allocator, int msgId, int response) {
        return allocator.ioBuffer(MessageBase.HEADER_LENGTH)
                .writeByte(Command.RESPONSE)
                .writeShort(msgId)
                .writeShort(response);
    }

    public static ByteBuf makeStringMessage(ChannelHandlerContext ctx, short cmd, int msgId, String data) {
        return makeBinaryMessage(ctx.alloc(), cmd, msgId, data.getBytes(CharsetUtil.UTF_8));
    }

    public static ByteBuf makeStringMessage(Channel channel, short cmd, int msgId, String data) {
        return makeBinaryMessage(channel.alloc(), cmd, msgId, data.getBytes(CharsetUtil.UTF_8));
    }

    public static ByteBuf makeBinaryMessage(ChannelHandlerContext ctx, short cmd, int msgId, byte[] byteData) {
        return makeBinaryMessage(ctx.alloc(), cmd, msgId, byteData);
    }

    public static ByteBuf makeBinaryMessage(Channel channel, short cmd, int msgId, byte[] byteData) {
        return makeBinaryMessage(channel.alloc(), cmd, msgId, byteData);
    }

    private static ByteBuf makeBinaryMessage(ByteBufAllocator allocator, short cmd, int msgId, byte[] byteData) {
        return allocator.ioBuffer(MessageBase.HEADER_LENGTH + byteData.length)
                .writeByte(cmd)
                .writeShort(msgId)
                .writeShort(byteData.length)
                .writeBytes(byteData);
    }

}
