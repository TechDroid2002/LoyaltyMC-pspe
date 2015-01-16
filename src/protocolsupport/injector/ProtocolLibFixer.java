package protocolsupport.injector;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.List;

import net.minecraft.server.v1_8_R1.Packet;

import org.bukkit.Bukkit;
import org.spigotmc.SneakyThrow;

import protocolsupport.protocol.PublicPacketDecoder;
import protocolsupport.protocol.PublicPacketEncoder;
import protocolsupport.utils.Utils;

import com.comphenix.protocol.reflect.accessors.MethodAccessor;

public class ProtocolLibFixer {

	private static boolean init = false;

	private static final MethodHandle vanillaDecoder = getVanillaDecoderSetterMethodHandle().asType(MethodType.methodType(void.class, ChannelHandler.class, ChannelHandler.class));
	private static final MethodHandle vanillaEncoder = getVanillaEncoderSetterMethodHandle().asType(MethodType.methodType(void.class, ChannelHandler.class, ChannelHandler.class));

	public static void init() {
		try {
			Class<?> channelInjectorClass = Class.forName("com.comphenix.protocol.injector.netty.ChannelInjector");
			Utils.setAccessible(channelInjectorClass.getDeclaredField("DECODE_BUFFER")).set(null, new PipelineDecodeMethodAccessor());
			Utils.setAccessible(channelInjectorClass.getDeclaredField("ENCODE_BUFFER")).set(null, new PipelineEncodeMethodAccessor());
			init = true;
		} catch (Throwable t) {
		}
	}

	private static MethodHandle getVanillaDecoderSetterMethodHandle() {
		try {
			Class<?> channelInjectorClass = Class.forName("com.comphenix.protocol.injector.netty.ChannelInjector");
			return MethodHandles.lookup().unreflectSetter(Utils.setAccessible(channelInjectorClass.getDeclaredField("vanillaDecoder")));
		} catch (Throwable t) {
		}
		return null;		
	}

	private static MethodHandle getVanillaEncoderSetterMethodHandle() {
		try {
			Class<?> channelInjectorClass = Class.forName("com.comphenix.protocol.injector.netty.ChannelInjector");
			return MethodHandles.lookup().unreflectSetter(Utils.setAccessible(channelInjectorClass.getDeclaredField("vanillaEncoder")));
		} catch (Throwable t) {
		}
		return null;		
	}

	public static void fixProtocolLib(ChannelPipeline pipeline, ChannelHandler decoder, ChannelHandler encoder) {
		if (!init) {
			return;
		}
		try {
			ChannelHandler protocolLibDecoder = pipeline.get("protocol_lib_decoder");
			if (protocolLibDecoder != null) {
				vanillaDecoder.invokeExact(protocolLibDecoder, decoder);
				vanillaEncoder.invokeExact(protocolLibDecoder, encoder);
			}
		} catch (Throwable t) {
			System.err.println("Failed to fix protocollib decoder, shutting down");
			t.printStackTrace();
			Bukkit.shutdown();
		}
	}

	private static class PipelineEncodeMethodAccessor implements MethodAccessor {

		@Override
		public Method getMethod() {
			throw new RuntimeException("PipelineEncodeMethodAccessor is dynamic and can't provide method instance");
		}

		@Override
		public Object invoke(Object obj, Object... args) {
			try {
				PublicPacketEncoder publicEncoder = (PublicPacketEncoder) obj;
				publicEncoder.publicEncode((ChannelHandlerContext) args[0], (Packet) args[1], (ByteBuf) args[2]);
			} catch (Throwable t) {
				SneakyThrow.sneaky(t);
			}
			return null;
		}

	}

	private static class PipelineDecodeMethodAccessor implements MethodAccessor {

		@Override
		public Method getMethod() {
			throw new RuntimeException("PipelineDecodeMethodAccessor is dynamic and can't provide method instance");
		}

		@SuppressWarnings("unchecked")
		@Override
		public Object invoke(Object obj, Object... args) {
			try {
				PublicPacketDecoder publicDecoder = (PublicPacketDecoder) obj;
				publicDecoder.publicDecode((ChannelHandlerContext) args[0], (ByteBuf) args[1], (List<Object>) args[2]);
			} catch (Throwable t) {
				SneakyThrow.sneaky(t);
			}
			return null;
		}

	}

}
