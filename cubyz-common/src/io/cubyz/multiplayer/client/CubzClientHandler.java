package io.cubyz.multiplayer.client;

import java.nio.charset.Charset;

import io.cubyz.multiplayer.Packet;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class CubzClientHandler extends ChannelInboundHandlerAdapter {

	private CubzClient cl;
	private ChannelHandlerContext ctx;

	private boolean hasPinged;
	
	public CubzClientHandler(CubzClient cl, boolean doPing) {
		this.cl = cl;
	}
	
	public void ping() {
		ByteBuf buf = ctx.alloc().buffer(1);
		buf.writeByte(Packet.PACKET_PINGDATA);
		ctx.writeAndFlush(buf);
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) {
		this.ctx = ctx;
		ByteBuf buf = ctx.alloc().buffer(1);
		buf.writeByte(Packet.PACKET_GETVERSION);
		ctx.write(buf);
		ctx.flush();
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		ByteBuf buf = (ByteBuf) msg;
		byte responseType = buf.readByte();
		
		if (responseType == Packet.PACKET_GETVERSION) {
			int length = buf.readUnsignedByte();
			String raw = buf.readCharSequence(length, Charset.forName("UTF-8")).toString();
			cl.getLocalServer().brand = raw.split(";")[0];
			cl.getLocalServer().version = raw.split(";")[1];
			System.out.println("[CubzClientHandler] Raw version + brand: " + raw);
		}
		
		if (responseType == Packet.PACKET_PINGDATA) {
			
		}
		
		if (responseType == Packet.PACKET_PINGPONG) {
			ByteBuf b = ctx.alloc().buffer(5);
			b.writeByte(Packet.PACKET_PINGPONG);
			b.writeInt(buf.readInt());
		}
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) {
		ctx.flush();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		// Close the connection when an exception is raised.
		cause.printStackTrace();
		ctx.close();
	}

}