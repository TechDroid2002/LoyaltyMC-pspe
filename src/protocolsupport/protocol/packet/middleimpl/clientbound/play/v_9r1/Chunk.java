package protocolsupport.protocol.packet.middleimpl.clientbound.play.v_9r1;

import java.util.Map;

import protocolsupport.protocol.ConnectionImpl;
import protocolsupport.protocol.packet.ClientBoundPacket;
import protocolsupport.protocol.packet.middle.clientbound.play.MiddleChunk;
import protocolsupport.protocol.packet.middleimpl.ClientBoundPacketData;
import protocolsupport.protocol.packet.middleimpl.clientbound.play.v_8_9r1_9r2_10_11_12r1_12r2_13.BlockTileUpdate;
import protocolsupport.protocol.serializer.ArraySerializer;
import protocolsupport.protocol.serializer.PositionSerializer;
import protocolsupport.protocol.serializer.VarNumberSerializer;
import protocolsupport.protocol.typeremapper.block.LegacyBlockData;
import protocolsupport.protocol.typeremapper.chunknew.ChunkWriterVariesWithLight;
import protocolsupport.protocol.typeremapper.utils.RemappingTable.ArrayBasedIdRemappingTable;
import protocolsupport.protocol.utils.chunk.ChunkConstants;
import protocolsupport.protocol.utils.types.Position;
import protocolsupport.protocol.utils.types.TileEntity;
import protocolsupport.utils.Utils;
import protocolsupport.utils.recyclable.RecyclableArrayList;
import protocolsupport.utils.recyclable.RecyclableCollection;

public class Chunk extends MiddleChunk {

	protected final ArrayBasedIdRemappingTable blockDataRemappingTable = LegacyBlockData.REGISTRY.getTable(version);

	public Chunk(ConnectionImpl connection) {
		super(connection);
	}

	@Override
	public RecyclableCollection<ClientBoundPacketData> toData() {
		RecyclableArrayList<ClientBoundPacketData> packets = RecyclableArrayList.create();

		ClientBoundPacketData chunksingle = ClientBoundPacketData.create(ClientBoundPacket.PLAY_CHUNK_SINGLE_ID);
		PositionSerializer.writeIntChunkCoord(chunksingle, coord);
		chunksingle.writeBoolean(full);
		VarNumberSerializer.writeVarInt(chunksingle, blockMask);
		boolean hasSkyLight = cache.getAttributesCache().hasSkyLightInCurrentDimension();
		ArraySerializer.writeVarIntByteArray(chunksingle, to -> {
			for (int sectionNumber = 0; sectionNumber < ChunkConstants.SECTION_COUNT_BLOCKS; sectionNumber++) {
				if (Utils.isBitSet(blockMask, sectionNumber)) {
					ChunkWriterVariesWithLight.writeSectionDataPreFlattening(
						to,
						13, blockDataRemappingTable,
						cachedChunk, hasSkyLight, sectionNumber
					);
				}
			}
			if (full) {
				for (int i = 0; i < biomeData.length; i++) {
					to.writeInt(biomeData[i]);
				}
			}
		});
		packets.add(chunksingle);

		for (Map<Position, TileEntity> sectionTiles : cachedChunk.getTiles()) {
			for (TileEntity tile : sectionTiles.values()) {
				packets.add(BlockTileUpdate.create(version, tile));
			}
		}

		return packets;
	}

}
