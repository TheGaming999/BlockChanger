package me.blockchanger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * @version 1.5
 * @author TheGaming999
 * @apiNote 1.7 - 1.18 easy to use class to take advantage of different methods that allow you to change blocks at rocket speeds
 * <p>Made with the help of <a href="https://github.com/CryptoMorin/XSeries/blob/master/src/main/java/com/cryptomorin/xseries/ReflectionUtils.java">ReflectionUtils</a> by <a href="https://github.com/CryptoMorin">CryptoMorin</a></p>
 * <p>Uses the methods found <a href="https://www.spigotmc.org/threads/395868/">here</a> by <a href="https://www.spigotmc.org/members/220001/">NascentNova</a></p>
 * <p>Async methods were made using <a href="https://www.spigotmc.org/threads/409003/">How to handle heavy splittable tasks</a> by <a href="https://www.spigotmc.org/members/43809/">7smile7</a></p>
 */
public class BlockChanger {

	private final static Map<Material, Object> NMS_BLOCK_MATERIALS = new HashMap<>();
	private final static Map<World, Object> NMS_WORLDS = new HashMap<>();
	private final static Map<String, Object> NMS_WORLD_NAMES = new HashMap<>();
	private final static MethodHandle WORLD_GET_HANDLE;
	/**<p>Invoked parameters -> <i>CraftItemStack.asNMSCopy({@literal<org.bukkit.inventory.ItemStack>})</i>*/
	private final static MethodHandle NMS_ITEM_STACK_COPY;
	/**<p>Invoked parameters -> <i>Block.asBlock({@literal<net.minecraft.world.item.Item>})</i>*/
	private final static MethodHandle NMS_BLOCK_FROM_ITEM;
	/**<p>Invoked parameters -> <i>{@literal<net.minecraft.world.item.ItemStack>}.getItem()</i>*/
	private final static MethodHandle NMS_ITEM_STACK_TO_ITEM;
	/**
	 * <p>Changes block data / durability</p>
	 * <p>Invoked parameters -> <i>{@literal<net.minecraft.world.block.Block>}.fromLegacyData({@literal<int>});</i></p>
	 */
	private final static MethodHandle BLOCK_DATA_FROM_LEGACY_DATA;
	/**<p>Invoked parameters -> <i>{@literal<net.minecraft.world.level.block.Block>}.getBlockData()</i>*/
	private final static MethodHandle ITEM_TO_BLOCK_DATA;
	private final static MethodHandle SET_TYPE_AND_DATA;
	private final static MethodHandle WORLD_GET_CHUNK;
	private final static MethodHandle CHUNK_GET_SECTIONS;
	private final static MethodHandle CHUNK_SECTION_SET_TYPE;
	/**<p>Behavior -> <i>{@literal<Chunk>}.getLevelHeightAccessor()</i>*/
	private final static MethodHandle GET_LEVEL_HEIGHT_ACCESSOR;
	/**<p>Behavior -> <i>{@literal<Chunk>}.getSectionIndex()</i> or <i>{@literal<LevelHeightAccessor>}.getSectionIndex()</i>*/
	private final static MethodHandle GET_SECTION_INDEX;
	/**<p>Behavior -> <i>Chunk.getSections[{@literal<index>}] = {@literal<ChunkSection>}</i></p>*/
	private final static MethodHandle SET_SECTION_ELEMENT;
	private final static MethodHandle CHUNK_SECTION;
	private final static MethodHandle CHUNK_SET_TYPE;
	private final static MethodHandle BLOCK_NOTIFY;
	// 1.13+ private final static MethodHandle CRAFT_BLOCK_GET_NMS;
	private final static MethodHandle CRAFT_BLOCK_GET_NMS_BLOCK;
	private final static MethodHandle NMS_BLOCK_GET_BLOCK_DATA;
	private final static MethodHandle WORLD_REMOVE_TILE_ENTITY;
	private final static MethodHandle WORLD_CAPTURED_TILE_ENTITIES;
	private final static MethodHandle IS_TILE_ENTITY;
	private final static BlockUpdater BLOCK_UPDATER;
	private final static BlockPositionConstructor BLOCK_POSITION_CONSTRUCTOR;
	private final static BlockDataRetriever BLOCK_DATA_GETTER;
	private final static TileEntityManager TILE_ENTITY_MANAGER;
	private final static String AVAILABLE_BLOCKS;
	private final static UncheckedSetters UNCHECKED_SETTERS;
	private final static WorkloadRunnable WORKLOAD_RUNNABLE;
	private final static JavaPlugin PLUGIN;
	private final static Object AIR_BLOCK_DATA;

	// 1.13+ -> { [PUBLIC] [IBlockData] getNMS() }
	// 1.7+ -> { [PRIVATE] [Block] getNMSBlock() -> [PUBLIC] [IBlockData] getBlockData() } 

	static {

		Class<?> worldServer = ReflectionUtils.getNMSClass("server.level", "WorldServer");
		Class<?> world = ReflectionUtils.getNMSClass("world.level", "World");
		Class<?> craftWorld = ReflectionUtils.getCraftClass("CraftWorld");
		Class<?> craftBlock = ReflectionUtils.getCraftClass("block.CraftBlock");
		Class<?> blockPosition = ReflectionUtils.supports(8) ? ReflectionUtils.getNMSClass("core", "BlockPosition") : null;
		Class<?> mutableBlockPosition = ReflectionUtils.supports(8) ? ReflectionUtils.getNMSClass("core", "BlockPosition$MutableBlockPosition") : null;
		Class<?> blockData = ReflectionUtils.supports(8) ? ReflectionUtils.getNMSClass("world.level.block.state", "IBlockData") : null;
		Class<?> craftItemStack = ReflectionUtils.getCraftClass("inventory.CraftItemStack");
		Class<?> worldItemStack = ReflectionUtils.getNMSClass("world.item", "ItemStack");
		Class<?> item = ReflectionUtils.getNMSClass("world.item", "Item");
		Class<?> block = ReflectionUtils.getNMSClass("world.level.block", "Block");
		Class<?> chunk = ReflectionUtils.getNMSClass("world.level.chunk", "Chunk");
		Class<?> chunkSection = ReflectionUtils.getNMSClass("world.level.chunk", "ChunkSection");
		Class<?> levelHeightAccessor = ReflectionUtils.supports(17) ? ReflectionUtils.getNMSClass("world.level.LevelHeightAccessor") : null;
		Class<?> blockDataReference = ReflectionUtils.supports(13) ? craftBlock : block;
		
		Method getNMSBlockMethod = null;

		if(ReflectionUtils.VER <= 12) {
			try {
				getNMSBlockMethod = craftBlock.getDeclaredMethod("getNMSBlock");
				getNMSBlockMethod.setAccessible(true);
			} catch (NoSuchMethodException | SecurityException e2) {
				e2.printStackTrace();
			}
		}

		MethodHandles.Lookup lookup = MethodHandles.lookup();

		Object airBlockData = null;
		try {
			airBlockData = lookup.findStatic(block, ReflectionUtils.supports(18) ? "a" : "getByCombinedId", MethodType.methodType(blockData, int.class))
					.invoke((int)0);
		} catch (Throwable e1) {
			e1.printStackTrace();
		}
		AIR_BLOCK_DATA = airBlockData;

		MethodHandle worldGetHandle = null;
		MethodHandle blockPositionXYZ = null;
		MethodHandle nmsItemStackCopy = null;
		MethodHandle blockFromItem = null;
		MethodHandle nmsItemStackToItem = null;
		MethodHandle itemToBlockData = null;
		MethodHandle setTypeAndData = null;
		MethodHandle worldGetChunk = null;
		MethodHandle chunkSetTypeM = null;
		MethodHandle blockNotify = null;
		MethodHandle chunkGetSections = null;
		MethodHandle chunkSectionSetType = null;
		MethodHandle getLevelHeightAccessor = null;
		MethodHandle getSectionIndex = null;
		MethodHandle setSectionElement = null;
		MethodHandle chunkSectionConstructor = null;
		MethodHandle blockDataFromLegacyData = null;
		MethodHandle mutableBlockPositionSet = null;
		MethodHandle mutableBlockPositionXYZ = null;
		// MethodHandle craftBlockGetNMS = null;
		MethodHandle craftBlockGetNMSBlock = null;
		MethodHandle nmsBlockGetBlockData = null;
		MethodHandle worldRemoveTileEntity = null;
		MethodHandle worldCapturedTileEntities = null;
		MethodHandle capturedTileEntitiesContainsKey = null;

		String asBlock = ReflectionUtils.supports(18) || ReflectionUtils.VER < 8 ? "a" : "asBlock";
		String getBlockData = ReflectionUtils.supports(18) ? "n" : "getBlockData";
		String getItem = ReflectionUtils.supports(18) ? "c" : "getItem";
		String setType = ReflectionUtils.supports(18) ? "a" : "setTypeAndData";
		String getChunkAt = ReflectionUtils.supports(18) ? "d" : "getChunkAt";
		String chunkSetType = ReflectionUtils.supports(18) ? "a" : ReflectionUtils.VER < 8 ? "setTypeId" : ReflectionUtils.VER <= 12 ? "a" : "setType";
		String notify = ReflectionUtils.supports(18) ? "a" : "notify";
		String getSections = ReflectionUtils.supports(18) ? "d" : "getSections";
		String sectionSetType = ReflectionUtils.supports(18) ? "a" : ReflectionUtils.VER < 8 ? "setTypeId" : "setType";
		String setXYZ = ReflectionUtils.supports(13) ? "d" : "c";
		String getBlockData2 = ReflectionUtils.supports(13) ? "getNMS" : "getBlockData";
		String removeTileEntity = ReflectionUtils.supports(18) ? "m" : ReflectionUtils.supports(14) ? "removeTileEntity" :
			ReflectionUtils.supports(13) ? "n" : ReflectionUtils.supports(9) ? "s" : ReflectionUtils.supports(8) ? "t" : "p";
		
		MethodType notifyMethodType = ReflectionUtils.VER >= 14 ? MethodType.methodType(void.class, blockPosition, blockData, blockData, int.class) :
			ReflectionUtils.VER < 8 ? MethodType.methodType(void.class, int.class, int.class, int.class) : ReflectionUtils.VER == 8 ?
					MethodType.methodType(void.class, blockPosition) : MethodType.methodType(void.class, blockPosition, blockData, blockData, int.class) ;

		MethodType chunkSetTypeMethodType = ReflectionUtils.VER <= 12 ? ReflectionUtils.VER >= 8 ? MethodType.methodType(blockData, blockPosition, blockData) :
			MethodType.methodType(boolean.class, int.class, int.class, int.class, block, int.class) :
				MethodType.methodType(blockData, blockPosition, blockData, boolean.class);

		MethodType chunkSectionSetTypeMethodType = ReflectionUtils.VER >= 14 ? MethodType.methodType(blockData, int.class, int.class, int.class, blockData) :
			ReflectionUtils.VER < 8 ? MethodType.methodType(void.class, int.class, int.class, int.class, block) :
				MethodType.methodType(void.class, int.class, int.class, int.class, blockData);

		MethodType chunkSectionConstructorMT = ReflectionUtils.supports(18) ? null :
			ReflectionUtils.supports(14) ? MethodType.methodType(void.class, int.class) :
				MethodType.methodType(void.class, int.class, boolean.class);
		
		MethodType removeTileEntityMethodType = ReflectionUtils.supports(8) ? MethodType.methodType(void.class, blockPosition) 
				: MethodType.methodType(void.class, int.class, int.class, int.class);

		MethodType fromLegacyDataMethodType = ReflectionUtils.VER <= 12 ? MethodType.methodType(blockData, int.class) : null;

		BlockPositionConstructor blockPositionConstructor = null;

		try {
			worldGetHandle = lookup.findVirtual(craftWorld, "getHandle", MethodType.methodType(worldServer));
			worldGetChunk = lookup.findVirtual(worldServer, getChunkAt, MethodType.methodType(chunk, int.class, int.class));
			nmsItemStackCopy = lookup.findStatic(craftItemStack, "asNMSCopy", MethodType.methodType(worldItemStack, ItemStack.class));
			blockFromItem = lookup.findStatic(block, asBlock, MethodType.methodType(block, item));	
			if(ReflectionUtils.supports(8)) {
				blockPositionXYZ = lookup.findConstructor(blockPosition, MethodType.methodType(void.class, int.class, int.class, int.class));
				mutableBlockPositionXYZ = lookup.findConstructor(mutableBlockPosition, MethodType.methodType(void.class, int.class, int.class, int.class));
				itemToBlockData = lookup.findVirtual(block, getBlockData, MethodType.methodType(blockData));
				setTypeAndData = lookup.findVirtual(worldServer, setType, MethodType.methodType(boolean.class, blockPosition, blockData, int.class));
				mutableBlockPositionSet = lookup.findVirtual(mutableBlockPosition, setXYZ, MethodType.methodType(mutableBlockPosition, int.class, int.class, int.class));
				blockPositionConstructor = new BlockPositionNormal(blockPositionXYZ, mutableBlockPositionXYZ, mutableBlockPositionSet);
			} else {
				blockPositionXYZ = lookup.findConstructor(Location.class, MethodType.methodType(void.class, World.class, double.class, double.class, double.class));
				mutableBlockPositionXYZ = lookup.findConstructor(Location.class, MethodType.methodType(void.class, World.class, double.class, double.class, double.class));
				blockPositionConstructor = new BlockPositionAncient(blockPositionXYZ, mutableBlockPositionXYZ);
			}
			nmsItemStackToItem = lookup.findVirtual(worldItemStack, getItem, MethodType.methodType(item));
			blockDataFromLegacyData = ReflectionUtils.VER <= 12 ? lookup.findVirtual(block, "fromLegacyData", fromLegacyDataMethodType) : null;
			chunkSetTypeM = lookup.findVirtual(chunk, chunkSetType, chunkSetTypeMethodType);
			blockNotify = lookup.findVirtual(worldServer, notify, notifyMethodType);
			chunkGetSections = lookup.findVirtual(chunk, getSections, MethodType.methodType(ReflectionUtils.toArrayClass(chunkSection)));
			chunkSectionSetType = lookup.findVirtual(chunkSection, sectionSetType, chunkSectionSetTypeMethodType);
			setSectionElement = MethodHandles.arrayElementSetter(ReflectionUtils.toArrayClass(chunkSection));
			chunkSectionConstructor = !ReflectionUtils.supports(18) ? lookup.findConstructor(chunkSection, chunkSectionConstructorMT) : null;
			if (ReflectionUtils.supports(18)) {
				getLevelHeightAccessor = lookup.findVirtual(chunk, "z", MethodType.methodType(levelHeightAccessor));
				getSectionIndex = lookup.findVirtual(levelHeightAccessor, "e", MethodType.methodType(int.class, int.class));
			} else if (ReflectionUtils.supports(17)) {
				getSectionIndex = lookup.findVirtual(chunk, "getSectionIndex", MethodType.methodType(int.class, int.class));
			}
			craftBlockGetNMSBlock = ReflectionUtils.VER <= 12 ? lookup.unreflect(getNMSBlockMethod) : null;
			nmsBlockGetBlockData = lookup.findVirtual(blockDataReference, getBlockData2, MethodType.methodType(blockData));
			// craftBlockGetNMS = ReflectionUtils.supports(13) ? lookup.findVirtual(craftBlock, "getNMS", MethodType.methodType(blockData)) : null;
			worldRemoveTileEntity = lookup.findVirtual(world, removeTileEntity, removeTileEntityMethodType);
			worldCapturedTileEntities = ReflectionUtils.supports(8) ? lookup.findGetter(world, "capturedTileEntities", Map.class) : null;
			capturedTileEntitiesContainsKey = ReflectionUtils.supports(8) ? lookup.findVirtual(Map.class, "containsKey", MethodType.methodType(boolean.class, Object.class)) : null;
		} catch (NoSuchMethodException | IllegalAccessException | NoSuchFieldException e) {
			e.printStackTrace();
		}

		WORLD_GET_HANDLE = worldGetHandle;
		WORLD_GET_CHUNK = worldGetChunk;
		NMS_ITEM_STACK_COPY = nmsItemStackCopy;
		NMS_BLOCK_FROM_ITEM = blockFromItem;
		NMS_ITEM_STACK_TO_ITEM = nmsItemStackToItem;
		ITEM_TO_BLOCK_DATA = itemToBlockData;
		SET_TYPE_AND_DATA = setTypeAndData;
		CHUNK_SET_TYPE = chunkSetTypeM;
		BLOCK_NOTIFY = blockNotify;
		CHUNK_GET_SECTIONS = chunkGetSections;
		CHUNK_SECTION_SET_TYPE = chunkSectionSetType;
		GET_LEVEL_HEIGHT_ACCESSOR = getLevelHeightAccessor;
		GET_SECTION_INDEX = getSectionIndex;
		SET_SECTION_ELEMENT = setSectionElement;
		CHUNK_SECTION = chunkSectionConstructor;
		BLOCK_POSITION_CONSTRUCTOR = blockPositionConstructor;
		BLOCK_DATA_FROM_LEGACY_DATA = blockDataFromLegacyData;
		// CRAFT_BLOCK_GET_NMS = craftBlockGetNMS;
		CRAFT_BLOCK_GET_NMS_BLOCK = craftBlockGetNMSBlock;
		NMS_BLOCK_GET_BLOCK_DATA = nmsBlockGetBlockData;
		WORLD_REMOVE_TILE_ENTITY = worldRemoveTileEntity;
		WORLD_CAPTURED_TILE_ENTITIES = worldCapturedTileEntities;
		IS_TILE_ENTITY = capturedTileEntitiesContainsKey;

		BLOCK_DATA_GETTER = ReflectionUtils.supports(13) ? new BlockDataGetter() : ReflectionUtils.supports(8) ? 
				new BlockDataGetterLegacy() : new BlockDataGetterAncient();

		BLOCK_UPDATER = ReflectionUtils.supports(18) ? new BlockUpdaterLatest(BLOCK_NOTIFY, CHUNK_SET_TYPE, GET_SECTION_INDEX, GET_LEVEL_HEIGHT_ACCESSOR) :
			ReflectionUtils.supports(17) ? new BlockUpdater17(BLOCK_NOTIFY, CHUNK_SET_TYPE, GET_SECTION_INDEX, CHUNK_SECTION, SET_SECTION_ELEMENT) :
				ReflectionUtils.supports(13) ? new BlockUpdater13(BLOCK_NOTIFY, CHUNK_SET_TYPE, CHUNK_SECTION, SET_SECTION_ELEMENT) :
					ReflectionUtils.supports(9) ? new BlockUpdater9(BLOCK_NOTIFY, CHUNK_SET_TYPE, CHUNK_SECTION, SET_SECTION_ELEMENT) :
						ReflectionUtils.supports(8) ? new BlockUpdaterLegacy(BLOCK_NOTIFY, CHUNK_SET_TYPE, CHUNK_SECTION, SET_SECTION_ELEMENT) :
							new BlockUpdaterAncient(BLOCK_NOTIFY, CHUNK_SET_TYPE, CHUNK_SECTION, SET_SECTION_ELEMENT);

		TILE_ENTITY_MANAGER = ReflectionUtils.supports(8) ? new TileEntityManagerSupported() : new TileEntityManagerDummy();
		
		Arrays.stream(Material.values())
		.filter(Material::isBlock)
		.forEach(BlockChanger::addNMSBlockData);

		NMS_BLOCK_MATERIALS.put(Material.AIR, AIR_BLOCK_DATA);

		AVAILABLE_BLOCKS = String.join(", ", NMS_BLOCK_MATERIALS.keySet().stream()
				.map(Material::name)
				.map(String::toLowerCase)
				.collect(Collectors.toList()));

		Bukkit.getWorlds().forEach(BlockChanger::addNMSWorld);

		UNCHECKED_SETTERS = new UncheckedSetters();

		WORKLOAD_RUNNABLE = new WorkloadRunnable();

		PLUGIN = JavaPlugin.getProvidingPlugin(BlockChanger.class);

		Bukkit.getScheduler().runTaskTimer(PLUGIN, WORKLOAD_RUNNABLE, 1, 1);

	}

	/**
	 * Simply calls <b>static {}</b> so methods get cached, and ensures that the first setBlock method call is as executed as fast as possible.
	 * <p>This already happens when calling a method for the first time.</p>
	 * <p>Added for debugging purposes.</p>
	 */
	public static void cache() {}

	private static void addNMSBlockData(Material material) {
		ItemStack itemStack = new ItemStack(material);
		Object nmsData = getNMSBlockData(itemStack);
		if(nmsData != null) NMS_BLOCK_MATERIALS.put(material, nmsData);
	}

	private static void addNMSWorld(World world) {
		if(world == null) return; 
		Object nmsWorld = getNMSWorld(world);
		if(nmsWorld != null) {
			NMS_WORLDS.put(world, nmsWorld);
			NMS_WORLD_NAMES.put(world.getName(), nmsWorld);
		}
	}

	/**
	 * Changes block type using native NMS world block type and data setter {@code nmsWorld.setTypeAndData(...)},
	 * which surpasses bukkit's {@linkplain org.bukkit.block.Block#setType(Material) Block.setType(Material)} speed.
	 * @param world world where the block is located
	 * @param x x location point
	 * @param y y location point
	 * @param z z location point
	 * @param material block material to apply on the created block
	 * @throws IllegalArgumentException if material is not perceived as a block material
	 */
	public static void setBlock(World world, int x, int y, int z, Material material) {
		setBlock(world, x, y, z, material, true);
	}

	/**
	 * Changes block type using native NMS world block type and data setter {@code nmsWorld.setTypeAndData(...)},
	 * which surpasses bukkit's {@linkplain org.bukkit.block.Block#setType(Material) Block.setType(Material)} speed.
	 * @param world world where the block is located
	 * @param x x location point
	 * @param y y location point
	 * @param z z location point
	 * @param itemStack ItemStack to apply on the created block
	 * @throws IllegalArgumentException if material is not perceived as a block material
	 */
	public static void setBlock(World world, int x, int y, int z, ItemStack itemStack) {
		setBlock(world, x, y, z, itemStack, true);
	}

	/**
	 * Changes block type using native NMS world block type and data setter {@code nmsWorld.setTypeAndData(...)},
	 * which surpasses bukkit's {@linkplain org.bukkit.block.Block#setType(Material) Block.setType(Material)} speed.
	 * @param world world where the block is located
	 * @param x x location point
	 * @param y y location point
	 * @param z z location point
	 * @param material block material to apply on the created block
	 * @param physics whether physics such as gravity should be applied or not
	 * @throws IllegalArgumentException if material is not perceived as a block material
	 */
	public static void setBlock(World world, int x, int y, int z, Material material, boolean physics) {
		if (!material.isBlock()) 
			throw new IllegalArgumentException("The specified material is not a placeable block!");
		Object nmsWorld = getWorld(world);
		Object blockPosition = newBlockPosition(world, x, y, z);
		Object blockData = getBlockData(material);
		removeIfTileEntity(nmsWorld, blockPosition);
		setTypeAndData(nmsWorld, blockPosition, blockData, physics ? 3 : 2);
	}

	/**
	 * Changes block type using native NMS world block type and data setter {@code nmsWorld.setTypeAndData(...)},
	 * which surpasses bukkit's {@linkplain org.bukkit.block.Block#setType(Material) Block.setType(Material)} speed.
	 * @param world world where the block is located
	 * @param x x location point
	 * @param y y location point
	 * @param z z location point
	 * @param itemStack ItemStack to apply on the created block
	 * @param physics whether physics such as gravity should be applied or not
	 */
	public static void setBlock(World world, int x, int y, int z, ItemStack itemStack, boolean physics) {
		Object nmsWorld = getWorld(world);
		Object blockPosition = newBlockPosition(world, x, y, z);
		Object blockData = getBlockData(itemStack);
		removeIfTileEntity(nmsWorld, blockPosition);
		setTypeAndData(nmsWorld, blockPosition, blockData, physics ? 3 : 2);
	}

	/**
	 * Changes block type using native NMS world block type and data setter {@code nmsWorld.setTypeAndData(...)},
	 * which surpasses bukkit's {@linkplain org.bukkit.block.Block#setType(Material) Block.setType(Material)} speed.
	 * @param world world where the block is located
	 * @param location location to put the block at
	 * @param material block material to apply on the created block
	 * @throws IllegalArgumentException if material is not perceived as a block material
	 * @throws NullPointerException if the specified material has no block data assigned to it
	 */
	public static void setBlock(World world, Location location, Material material) {
		setBlock(world, location, material, true);
	}

	/**
	 * Changes block type using native NMS world block type and data setter {@code nmsWorld.setTypeAndData(...)},
	 * which surpasses bukkit's {@linkplain org.bukkit.block.Block#setType(Material) Block.setType(Material)} speed.
	 * @param world world where the block is located
	 * @param location location to put the block at
	 * @param itemStack ItemStack to apply on the created block
	 */
	public static void setBlock(World world, Location location, ItemStack itemStack) {
		setBlock(world, location, itemStack, true);
	}

	/**
	 * Changes block type using native NMS world block type and data setter {@code nmsWorld.setTypeAndData(...)},
	 * which surpasses bukkit's {@linkplain org.bukkit.block.Block#setType(Material) Block.setType(Material)} speed.
	 * @param world world where the block is located
	 * @param location location to put the block at
	 * @param material block material to apply on the created block
	 * @param physics whether physics such as gravity should be applied or not
	 * @throws IllegalArgumentException if material is not perceived as a block material
	 * @throws NullPointerException if the specified material has no block data assigned to it
	 */
	public static void setBlock(World world, Location location, Material material, boolean physics) {
		if (!material.isBlock()) 
			throw new IllegalArgumentException("The specified material is not a placeable block!");
		Object nmsWorld = getWorld(world);
		Object blockPosition = newBlockPosition(world, location.getBlockX(), location.getBlockY(), location.getBlockZ());
		Object blockData = getBlockData(material);
		if (blockData == null)
			throw new NullPointerException("Unable to retrieve block data for the corresponding material.");
		removeIfTileEntity(nmsWorld, blockPosition);
		setTypeAndData(nmsWorld, blockPosition, blockData, physics ? 3 : 2);
	}

	/**
	 * Changes block type using native NMS world block type and data setter {@code nmsWorld.setTypeAndData(...)},
	 * which surpasses bukkit's {@linkplain org.bukkit.block.Block#setType(Material) Block.setType(Material)} speed.
	 * @param world world where the block is located
	 * @param location location to put the block at
	 * @param itemStack ItemStack to apply on the created block
	 * @param physics whether physics such as gravity should be applied or not
	 */
	public static void setBlock(World world, Location location, ItemStack itemStack, boolean physics) {
		Object nmsWorld = getWorld(world);
		Object blockPosition = newBlockPosition(world, location.getBlockX(), location.getBlockY(), location.getBlockZ());
		Object blockData = getBlockData(itemStack);
		removeIfTileEntity(nmsWorld, blockPosition);
		setTypeAndData(nmsWorld, blockPosition, blockData, physics ? 3 : 2);
	}

	/**
	 * Changes block type using native NMS world block type and data setter {@code nmsWorld.setTypeAndData(...)},
	 * which surpasses bukkit's {@linkplain org.bukkit.block.Block#setType(Material) Block.setType(Material)} speed.
	 * @param location location to put the block at
	 * @param material block material to apply on the created block
	 * @throws IllegalArgumentException if material is not perceived as a block material
	 * @throws NullPointerException if the specified material has no block data assigned to it
	 */
	public static void setBlock(Location location, Material material) {
		setBlock(location, material, true);
	}

	/**
	 * Changes block type using native NMS world block type and data setter {@code nmsWorld.setTypeAndData(...)},
	 * which surpasses bukkit's {@linkplain org.bukkit.block.Block#setType(Material) Block.setType(Material)} speed.
	 * @param location location to put the block at
	 * @param itemStack ItemStack to apply on the created block
	 */
	public static void setBlock(Location location, ItemStack itemStack) {
		setBlock(location, itemStack, true);
	}

	/**
	 * Changes block type using native NMS world block type and data setter {@code nmsWorld.setTypeAndData(...)},
	 * which surpasses bukkit's {@linkplain org.bukkit.block.Block#setType(Material) Block.setType(Material)} speed.
	 * @param location location to put the block at
	 * @param material block material to apply on the created block
	 * @param physics whether physics such as gravity should be applied or not
	 * @throws IllegalArgumentException if material is not perceived as a block material
	 * @throws NullPointerException if the specified material has no block data assigned to it
	 */
	public static void setBlock(Location location, Material material, boolean physics) {
		if (!material.isBlock()) 
			throw new IllegalArgumentException("The specified material is not a placeable block!");
		Object nmsWorld = getWorld(location.getWorld());
		Object blockPosition = newBlockPosition(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
		Object blockData = getBlockData(material);
		if (blockData == null)
			throw new NullPointerException("Unable to retrieve block data for the corresponding material.");
		removeIfTileEntity(nmsWorld, blockPosition);
		setTypeAndData(nmsWorld, blockPosition, blockData, physics ? 3 : 2);
	}

	/**
	 * Changes block type using native NMS world block type and data setter {@code nmsWorld.setTypeAndData(...)},
	 * which surpasses bukkit's {@linkplain org.bukkit.block.Block#setType(Material) Block.setType(Material)} speed.
	 * @param location location to put the block at
	 * @param itemStack ItemStack to apply on the created block
	 * @param physics whether physics such as gravity should be applied or not
	 */
	public static void setBlock(Location location, ItemStack itemStack, boolean physics) {
		Object nmsWorld = getWorld(location.getWorld());
		Object blockPosition = newBlockPosition(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
		Object blockData = getBlockData(itemStack);
		removeIfTileEntity(nmsWorld, blockPosition);
		setTypeAndData(nmsWorld, blockPosition, blockData, physics ? 3 : 2);
	}

	/**
	 * Asynchronously changes block type using native NMS world block type and data setter {@code nmsWorld.setTypeAndData(...)},
	 * which surpasses bukkit's {@linkplain org.bukkit.block.Block#setType(Material) Block.setType(Material)} speed.
	 * <br>
	 * <br>Async within this context means:
	 * <ul>
	 * <li>There won't be any TPS loss no matter the amount of blocks being set</li>
	 * <li>It can be safely executed inside an asynchronous task</li>
	 * </ul>
	 * @param location location to put the block at
	 * @param itemStack ItemStack to apply on the created block
	 * @param physics whether physics such as gravity should be applied or not
	 */
	public static CompletableFuture<Void> setBlockAsynchronously(Location location, ItemStack itemStack, boolean physics) {
		Object nmsWorld = getWorld(location.getWorld());
		Object blockPosition = newMutableBlockPosition(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
		Object blockData = getBlockData(itemStack);
		CompletableFuture<Void> workloadFinishFuture = new CompletableFuture<>();
		BlockSetWorkload workload = new BlockSetWorkload(nmsWorld, blockPosition, blockData, location, physics);
		WORKLOAD_RUNNABLE.addWorkload(workload);
		WORKLOAD_RUNNABLE.whenComplete(() -> workloadFinishFuture.complete(null));
		return workloadFinishFuture;
	}

	/**
	 * Mass changes block types using native NMS world block type and data setter {@code nmsWorld.setTypeAndData(...)},
	 * which surpasses bukkit's {@linkplain org.bukkit.block.Block#setType(Material) Block.setType(Material)} speed.
	 * @param world world where the blocks are located at
	 * @param locations locations to put the block at
	 * @param material block material to apply on the created blocks
	 * @param physics whether physics such as gravity should be applied or not
	 * @throws IllegalArgumentException if material is not perceived as a block material
	 * @throws NullPointerException if the specified material has no block data assigned to it
	 */
	public static void setBlocks(World world, Collection<Location> locations, Material material, boolean physics) {
		if (!material.isBlock()) 
			throw new IllegalArgumentException("The specified material is not a placeable block!");
		Object nmsWorld = getWorld(world);
		Object blockData = getBlockData(material);
		if (blockData == null)
			throw new NullPointerException("Unable to retrieve block data for the corresponding material.");
		Object blockPosition = newMutableBlockPosition(world, 0, 0, 0);
		int applyPhysics = physics ? 3 : 2;
		locations.forEach(location -> {
			int x = location.getBlockX();
			int y = location.getBlockY();
			int z = location.getBlockZ();
			setBlockPosition(blockPosition, x, y, z);
			removeIfTileEntity(nmsWorld, blockPosition);
			setTypeAndData(nmsWorld, blockPosition, blockData, applyPhysics);
		});
	}

	/**
	 * Mass changes block types using native NMS world block type and data setter {@code nmsWorld.setTypeAndData(...)},
	 * which surpasses bukkit's {@linkplain org.bukkit.block.Block#setType(Material) Block.setType(Material)} speed.
	 * @param world world where the blocks are located at
	 * @param locations locations to put the block at
	 * @param itemStack ItemStack to apply on the created blocks
	 * @param physics whether physics such as gravity should be applied or not
	 * @throws IllegalArgumentException if material is not perceived as a block material
	 * @throws NullPointerException if the specified material has no block data assigned to it
	 */
	public static void setBlocks(World world, Collection<Location> locations, ItemStack itemStack, boolean physics) {
		Object nmsWorld = getWorld(world);
		Object blockData = getBlockData(itemStack);
		Object blockPosition = newMutableBlockPosition(world, 0, 0, 0);
		int applyPhysics = physics ? 3 : 2;
		locations.forEach(location -> {
			int x = location.getBlockX();
			int y = location.getBlockY();
			int z = location.getBlockZ();
			setBlockPosition(blockPosition, x, y, z);
			removeIfTileEntity(nmsWorld, blockPosition);
			setTypeAndData(nmsWorld, blockPosition, blockData, applyPhysics);
		});
	}

	/**
	 * Asynchronously changes block types using native NMS world block type and data setter {@code nmsWorld.setTypeAndData(...)},
	 * which surpasses bukkit's {@linkplain org.bukkit.block.Block#setType(Material) Block.setType(Material)} speed.
	 * <br>
	 * <br>Async within this context means:
	 * <ul>
	 * <li>There won't be any TPS loss no matter the amount of blocks being set</li>
	 * <li>It can be safely executed inside an asynchronous task</li>
	 * </ul>
	 * @param world world where the blocks are located at
	 * @param locations locations to put the block at
	 * @param itemStack ItemStack to apply on the created blocks
	 * @param physics whether physics such as gravity should be applied or not
	 * @throws IllegalArgumentException if material is not perceived as a block material
	 * @throws NullPointerException if the specified material has no block data assigned to it
	 */
	public static CompletableFuture<Void> setBlocksAsynchronously(World world, Collection<Location> locations, ItemStack itemStack, boolean physics) {
		Object nmsWorld = getWorld(world);
		Object blockData = getBlockData(itemStack);
		Object blockPosition = newMutableBlockPosition(world, 0, 0, 0);
		CompletableFuture<Void> workloadFinishFuture = new CompletableFuture<>();
		WorkloadRunnable workloadRunnable = new WorkloadRunnable();
		BukkitTask workloadTask = Bukkit.getScheduler().runTaskTimer(PLUGIN, workloadRunnable, 1, 1);
		locations.forEach(location -> {
			BlockSetWorkload workload = new BlockSetWorkload(nmsWorld, blockPosition, blockData, location, physics);
			workloadRunnable.addWorkload(workload);
		});
		workloadRunnable.whenComplete(() -> {
			workloadFinishFuture.complete(null);
			workloadTask.cancel();
		});
		return workloadFinishFuture;
	}

	/**
	 * Asynchronously fills a cuboid from a corner to another with blocks retrieved from the given ItemStack
	 * using native NMS world block type and data setter {@code nmsWorld.setTypeAndData(...)},
	 * which surpasses bukkit's {@linkplain org.bukkit.block.Block#setType(Material) Block.setType(Material)} speed.
	 * <br>
	 * <br>Async within this context means:
	 * <ul>
	 * <li>There won't be any TPS loss no matter the amount of blocks being set</li>
	 * <li>It can be safely executed inside an asynchronous task</li>
	 * </ul>
	 * @param world world where the blocks are located at
	 * @param loc1 first corner
	 * @param loc2 second corner
	 * @param itemStack ItemStack to apply on the created blocks
	 * @param physics whether physics such as gravity should be applied or not
	 * @throws IllegalArgumentException if material is not perceived as a block material
	 * @throws NullPointerException if the specified material has no block data assigned to it
	 */
	public static CompletableFuture<Void> setCuboidAsynchronously(Location loc1, Location loc2, ItemStack itemStack, boolean physics) {
		World world = loc1.getWorld();
		Object nmsWorld = getWorld(world);
		Object blockData = getBlockData(itemStack);
		int x1 = Math.min(loc1.getBlockX(), loc2.getBlockX());
		int y1 = Math.min(loc1.getBlockY(), loc2.getBlockY());
		int z1 = Math.min(loc1.getBlockZ(), loc2.getBlockZ());
		int x2 = Math.max(loc1.getBlockX(), loc2.getBlockX());
		int y2 = Math.max(loc1.getBlockY(), loc2.getBlockY());
		int z2 = Math.max(loc1.getBlockZ(), loc2.getBlockZ());
		int baseX = x1;
		int baseY = y1;
		int baseZ = z1;
		int sizeX = Math.abs(x2 - x1) + 1;
		int sizeY = Math.abs(y2 - y1) + 1;
		int sizeZ = Math.abs(z2 - z1) + 1;
		int x3 = 0, y3 = 0, z3 = 0;
		Location location = new Location(world, baseX + x3, baseY + y3, baseZ + z3);
		int cuboidSize = sizeX*sizeY*sizeZ;
		Object blockPosition = newMutableBlockPosition(location);
		CompletableFuture<Void> workloadFinishFuture = new CompletableFuture<>();
		WorkloadRunnable workloadRunnable = new WorkloadRunnable();
		BukkitTask workloadTask = Bukkit.getScheduler().runTaskTimer(PLUGIN, workloadRunnable, 1, 1);
		for (int i = 0; i < cuboidSize ; i++) {
			BlockSetWorkload workload = new BlockSetWorkload(nmsWorld, blockPosition, blockData, location.clone(), physics);
			if (++x3 >= sizeX) {
				x3 = 0;
				if (++y3 >= sizeY) {
					y3 = 0;
					++z3;
				}
			}
			location.setX(baseX + x3);
			location.setY(baseY + y3);
			location.setZ(baseZ + z3);
			workloadRunnable.addWorkload(workload);
		}
		workloadRunnable.whenComplete(() -> {
			workloadFinishFuture.complete(null);
			workloadTask.cancel();
		});
		return workloadFinishFuture;
	}

	/**
	 * <p>Changes block type using Chunk block setter, which in an NMS code, reads as follows {@code nmsChunk.setType(...)} which surpasses {@code nmsWorld.setTypeAndData(...)}
	 * speed due to absence of light updates, the method that {@link #setBlock(Location, Material)} uses. Then, 
	 * notifies the world of the updated blocks so they can be seen by the players.
	 * @param location location to put the block at
	 * @param material material to apply on the block
	 */
	public static void setChunkBlock(Location location, Material material) {
		setChunkBlock(location, material, false);
	}

	/**
	 * <p>Changes block type using Chunk block setter, which in an NMS code, reads as follows {@code nmsChunk.setType(...)} which surpasses {@code nmsWorld.setTypeAndData(...)}
	 * speed due to absence of light updates, the method that {@link #setBlock(Location, ItemStack)} uses. Then, 
	 * notifies the world of the updated blocks so they can be seen by the players.
	 * @param location location to put the block at
	 * @param itemStack ItemStack to apply on the block
	 */
	public static void setChunkBlock(Location location, ItemStack itemStack) {
		setChunkBlock(location, itemStack, false);
	}

	/**
	 * <p>Changes block type using Chunk block setter, which in an NMS code, reads as follows {@code nmsChunk.setType(...)} which surpasses {@code nmsWorld.setTypeAndData(...)}
	 * speed due to absence of light updates, the method that {@link #setBlock(Location, ItemStack)} uses. Then, 
	 * notifies the world of the updated blocks so they can be seen by the players.
	 * @param location location to put the block at
	 * @param material material to apply on the block
	 * @param physics whether physics should be applied or not
	 */
	public static void setChunkBlock(Location location, Material material, boolean physics) {
		if (!material.isBlock()) 
			throw new IllegalArgumentException("The specified material is not a placeable block!");
		Object nmsWorld = getWorld(location.getWorld());
		Object blockPosition = newBlockPosition(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
		Object blockData = getBlockData(material);
		if (blockData == null)
			throw new NullPointerException("Unable to retrieve block data for the corresponding material.");
		Object chunk = getChunkAt(nmsWorld, location);
		removeIfTileEntity(nmsWorld, blockPosition);
		setType(chunk, blockPosition, blockData, physics);
		updateBlock(nmsWorld, blockPosition, blockData, physics);
	}

	/**
	 * <p>Changes block type using Chunk block setter, which in an NMS code, reads as follows {@code nmsChunk.setType(...)} which surpasses {@code nmsWorld.setTypeAndData(...)}
	 * speed due to absence of light updates, the method that {@link #setBlock(Location, ItemStack)} uses. Then, 
	 * notifies the world of the updated blocks so they can be seen by the players.
	 * @param location location to put the block at
	 * @param itemStack itemStack to apply on the block
	 * @param physics whether physics should be applied or not
	 */
	public static void setChunkBlock(Location location, ItemStack itemStack, boolean physics) {
		Object nmsWorld = getWorld(location.getWorld());
		Object blockPosition = newBlockPosition(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
		Object blockData = getBlockData(itemStack);
		Object chunk = getChunkAt(nmsWorld, location);
		removeIfTileEntity(nmsWorld, blockPosition);
		setType(chunk, blockPosition, blockData, physics);
		updateBlock(nmsWorld, blockPosition, blockData, physics);
	}

	/**
	 * As stated in {@link #setChunkBlock(Location, ItemStack, boolean)}:
	 * <p>Changes block type using Chunk block setter, which in an NMS code, reads as follows {@code nmsChunk.setType(...)} which surpasses {@code nmsWorld.setTypeAndData(...)}
	 * speed due to absence of light updates, the method that {@link #setBlock(Location, ItemStack)} uses. Then, 
	 * notifies the world of the updated blocks so they can be seen by the players.
	 * 
	 * <p>In addition to that, it makes sure that there is no TPS loss due to the amount of blocks being changed.
	 * @param location location to put the block at
	 * @param itemStack itemStack to apply on the block
	 * @param physics whether physics should be applied or not
	 */
	public static CompletableFuture<Void> setChunkBlockAsynchronously(Location location, ItemStack itemStack, boolean physics) {
		Object nmsWorld = getWorld(location.getWorld());
		Object blockPosition = newMutableBlockPosition(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
		Object blockData = getBlockData(itemStack);
		CompletableFuture<Void> workloadFinishFuture = new CompletableFuture<>();
		ChunkSetWorkload workload = new ChunkSetWorkload(nmsWorld, blockPosition, blockData, location, physics);
		WORKLOAD_RUNNABLE.addWorkload(workload);
		WORKLOAD_RUNNABLE.whenComplete(() -> workloadFinishFuture.complete(null));
		return workloadFinishFuture;
	}

	/**
	 * Mass change blocks at the given locations using Chunk block setter which doesn't apply light updates but offers
	 * better performance in comparison to setBlocks(...)
	 * @param world world where the blocks are located at
	 * @param locations locations to put the block at
	 * @param itemStack ItemStack to apply on the created blocks
	 * @param physics whether physics such as gravity should be applied or not
	 * @throws IllegalArgumentException if material is not perceived as a block material
	 * @throws NullPointerException if the specified material has no block data assigned to it
	 */
	public static void setChunkBlocks(World world, Collection<Location> locations, ItemStack itemStack, boolean physics) {
		Object nmsWorld = getWorld(world);
		Object blockData = getBlockData(itemStack);
		Object blockPosition = newMutableBlockPosition(world, 0, 0, 0);
		locations.forEach(location -> {
			int x = location.getBlockX();
			int y = location.getBlockY();
			int z = location.getBlockZ();
			Object chunk = getChunkAt(nmsWorld, x, z);
			setBlockPosition(blockPosition, x, y, z);
			removeIfTileEntity(nmsWorld, blockPosition);
			setType(chunk, blockPosition, blockData, physics);
		});
	}
	/**
	 * A thread safe version of {@link #setChunkBlocks(World, Collection, ItemStack, boolean)}*
	 * <p>* Mass change blocks at the given locations using Chunk block setter which doesn't apply light updates but offers
	 * better performance in comparison to setBlocks(...). 
	 * <p>With an eye on the server TPS, this method won't degrade the server performance regardless of the 
	 * amount of blocks being changed in contrast to the regular one.
	 * @param world world where the blocks are located at
	 * @param locations locations to put the block at
	 * @param itemStack ItemStack to apply on the created blocks
	 * @param physics whether physics such as gravity should be applied or not
	 * @throws IllegalArgumentException if material is not perceived as a block material
	 * @throws NullPointerException if the specified material has no block data assigned to it
	 */
	public static CompletableFuture<Void> setChunkBlocksAsynchronously(World world, Collection<Location> locations, ItemStack itemStack, boolean physics) {
		Object nmsWorld = getWorld(world);
		Object blockData = getBlockData(itemStack);
		Object blockPosition = newMutableBlockPosition(world, 0, 0, 0);
		CompletableFuture<Void> workloadFinishFuture = new CompletableFuture<>();
		WorkloadRunnable workloadRunnable = new WorkloadRunnable();
		BukkitTask workloadTask = Bukkit.getScheduler().runTaskTimer(PLUGIN, workloadRunnable, 1, 1);
		locations.forEach(location -> {
			ChunkSetWorkload workload = new ChunkSetWorkload(nmsWorld, blockPosition, blockData, location, physics);
			workloadRunnable.addWorkload(workload);
		});
		workloadRunnable.whenComplete(() -> {
			workloadFinishFuture.complete(null);
			workloadTask.cancel();
		});
		return workloadFinishFuture;
	}

	/**
	 * Changes block type using {@code chunkSection.setType(...)} which is superior to {@code nmsChunk.setType(...)} and 
	 * {@code nmsWorld.setTypeAndData(...)} due to the absence of extensive checks, then notifies the world of the updated blocks so they can be seen by the players without the need to relogin
	 * @param location location to put the block at
	 * @param material material to apply on the block
	 */
	public static void setSectionBlock(Location location, Material material) {
		setSectionBlock(location, material, false);
	}

	/**
	 * Changes block type using {@code chunkSection.setType(...)} which is superior to {@code nmsChunk.setType(...)} and 
	 * {@code nmsWorld.setTypeAndData(...)} in terms of speed due to the absence of extensive checks, then notifies the world of the updated blocks so they can be seen by the players without the need to relogin
	 * @param location location to put the block at
	 * @param material material to apply on the block
	 * @param physics whether physics should be applied or not
	 */
	public static void setSectionBlock(Location location, Material material, boolean physics) {
		if (!material.isBlock()) 
			throw new IllegalArgumentException("The specified material is not a placeable block!");
		World world = location.getWorld();
		Object nmsWorld = getWorld(world);
		Object blockData = getBlockData(material);
		if (blockData == null)
			throw new NullPointerException("Unable to retrieve block data for the corresponding material.");
		int x = location.getBlockX();
		int y = location.getBlockY();
		int z = location.getBlockZ();
		Object nmsChunk = getChunkAt(nmsWorld, location);
		int j = x & 15;
		int k = y & 15;
		int l = z & 15;
		Object[] sections = getSections(nmsChunk);
		Object section = getSection(nmsChunk, sections, y);
		Object blockPosition = newBlockPosition(world, x, y, z);
		removeIfTileEntity(nmsWorld, blockPosition);
		setTypeChunkSection(section, j, k, l, blockData);
		updateBlock(nmsWorld, blockPosition, blockData, physics);
	}

	/**
	 * Changes block type using {@code chunkSection.setType(...)} which is superior to {@code nmsChunk.setType(...)} and 
	 * {@code nmsWorld.setTypeAndData(...)} due to the absence of extensive checks, then notifies the world of the updated blocks so they can be seen by the players without the need to relogin
	 * @param location location to put the block at
	 * @param itemStack ItemStack to apply on the block
	 */
	public static void setSectionBlock(Location location, ItemStack itemStack) {
		setSectionBlock(location, itemStack, false);
	}

	/**
	 * Changes block type using {@code chunkSection.setType(...)} which is superior to {@code nmsChunk.setType(...)} and 
	 * {@code nmsWorld.setTypeAndData(...)} in terms of speed due to the absence of extensive checks, then notifies the world of the updated blocks so they can be seen by the players without the need to relogin
	 * @param location location to put the block at
	 * @param itemStack ItemStack to apply on the block
	 * @param physics whether physics should be applied or not
	 */
	public static void setSectionBlock(Location location, ItemStack itemStack, boolean physics) {
		World world = location.getWorld();
		Object nmsWorld = getWorld(world);
		Object blockData = getBlockData(itemStack);
		if (blockData == null)
			throw new NullPointerException("Unable to retrieve block data for the corresponding material.");
		int x = location.getBlockX();
		int y = location.getBlockY();
		int z = location.getBlockZ();
		Object nmsChunk = getChunkAt(nmsWorld, location);
		int j = x & 15;
		int k = y & 15;
		int l = z & 15;
		Object[] sections = getSections(nmsChunk);
		Object section = getSection(nmsChunk, sections, y);
		Object blockPosition = newBlockPosition(world, x, y, z);
		removeIfTileEntity(nmsWorld, blockPosition);
		setTypeChunkSection(section, j, k, l, blockData);
		updateBlock(nmsWorld, blockPosition, blockData, physics);
	}

	/**
	 * Changes block type using {@code chunkSection.setType(...)} which is superior to {@code nmsChunk.setType(...)} and 
	 * {@code nmsWorld.setTypeAndData(...)} in terms of speed due to the absence of extensive checks, then notifies the world of the updated blocks so they can be seen by the players without the need to relogin
	 * @param location location to put the block at
	 * @param itemStack ItemStack to apply on the block
	 * @param physics whether physics should be applied or not
	 */
	public static CompletableFuture<Void> setSectionBlockAsynchronously(Location location, ItemStack itemStack, boolean physics) {
		World world = location.getWorld();
		Object nmsWorld = getWorld(world);
		Object blockData = getBlockData(itemStack);
		Object blockPosition = newMutableBlockPosition(world, 0, 0, 0);
		if (blockData == null)
			throw new NullPointerException("Unable to retrieve block data for the corresponding material.");
		CompletableFuture<Void> workloadFinishFuture = new CompletableFuture<Void>();
		SectionSetWorkload workload = new SectionSetWorkload(nmsWorld, blockPosition, blockData, location, physics);
		WORKLOAD_RUNNABLE.addWorkload(workload);
		WORKLOAD_RUNNABLE.whenComplete(() -> workloadFinishFuture.complete(null));
		return workloadFinishFuture;
	}

	/**
	 * Performs {@link #setSectionBlock(Location, Material)} but after storing the block data and the world as nms objects, so they can
	 * be used again and again instead of retrieving them every time a block gets set.
	 * @param locations locations to put the blocks at
	 * @param material material to apply on the blocks
	 * @param world world where locations are taken from
	 */
	public static void setSectionBlocks(World world, Collection<Location> locations, Material material) {
		if (!material.isBlock()) 
			throw new IllegalArgumentException("The specified material is not a placeable block!");
		Object nmsWorld = getWorld(world);
		Object blockData = getBlockData(material);
		if (blockData == null)
			throw new NullPointerException("Unable to retrieve block data for the corresponding material.");
		Object blockPosition = newMutableBlockPosition(world, 0, 0, 0);
		locations.forEach(location -> {
			int x = location.getBlockX();
			int y = location.getBlockY();
			int z = location.getBlockZ();
			Object nmsChunk = getChunkAt(nmsWorld, location);
			int j = x & 15;
			int k = y & 15;
			int l = z & 15;
			Object[] sections = getSections(nmsChunk);
			Object section = getSection(nmsChunk, sections, y);
			removeIfTileEntity(nmsWorld, blockPosition);
			setTypeChunkSection(section, j, k, l, blockData);
			setBlockPosition(blockPosition, x, y, z);
			updateBlock(nmsWorld, blockPosition, blockData, false);
		});
	}

	/**
	 * Performs {@link #setSectionBlock(Location, ItemStack)} but after storing the block data and the world as nms objects, so they can
	 * be used again and again instead of retrieving them every time a block gets set.
	 * @param locations locations to put the blocks at
	 * @param itemStack ItemStack to apply on the blocks
	 * @param world world where locations are taken from
	 */
	public static void setSectionBlocks(World world, Collection<Location> locations, ItemStack itemStack) {
		Object nmsWorld = getWorld(world);
		Object blockData = getBlockData(itemStack);
		Object blockPosition = newMutableBlockPosition(world, 0, 0, 0);
		locations.forEach(location -> {
			int x = location.getBlockX();
			int y = location.getBlockY();
			int z = location.getBlockZ();
			Object nmsChunk = getChunkAt(nmsWorld, location);
			int j = x & 15;
			int k = y & 15;
			int l = z & 15;
			Object[] sections = getSections(nmsChunk);
			Object section = getSection(nmsChunk, sections, y);
			removeIfTileEntity(nmsWorld, blockPosition);
			setTypeChunkSection(section, j, k, l, blockData);
			setBlockPosition(blockPosition, x, y, z);
			updateBlock(nmsWorld, blockPosition, blockData, false);
		});
	}

	/**
	 * Performs {@link #setSectionBlock(Location, ItemStack)} but after storing the block data and the world as nms objects, so they can
	 * be used again and again instead of retrieving them every time a block gets set.
	 * @param locations locations to put the blocks at
	 * @param itemStack ItemStack to apply on the blocks
	 * @param world world where locations are taken from
	 */
	public static CompletableFuture<Void> setSectionBlocksAsynchronously(World world, Collection<Location> locations, ItemStack itemStack) {
		Object nmsWorld = getWorld(world);
		Object blockData = getBlockData(itemStack);
		Object blockPosition = newMutableBlockPosition(world, 0, 0, 0);
		CompletableFuture<Void> workloadFinishFuture = new CompletableFuture<>();
		WorkloadRunnable workloadRunnable = new WorkloadRunnable();
		BukkitTask workloadTask = Bukkit.getScheduler().runTaskTimer(PLUGIN, workloadRunnable, 1, 1);
		locations.forEach(location -> {
			SectionSetWorkload workload = new SectionSetWorkload(nmsWorld, blockPosition, blockData, location, false);
			workloadRunnable.addWorkload(workload);
		});
		workloadRunnable.whenComplete(() -> {
			workloadFinishFuture.complete(null);
			workloadTask.cancel();
		});
		return workloadFinishFuture;
	}

	/**
	 * Has the same behavior as {@link #setSectionBlocks(World, Location, Material)} but creates a cuboid from a location
	 * to another location similar to the vanilla command <b>/fill</b>
	 * @param loc1 point 1
	 * @param loc2 point 2
	 * @param material material to apply on the blocks
	 */
	public static void setSectionCuboid(Location loc1, Location loc2, Material material) {
		setSectionCuboid(loc1, loc2, material, false);
	}

	/**
	 * Has the same behavior as {@link #setSectionBlocks(World, Location, Material)} but creates a cuboid from a location
	 * to another location similar to the vanilla command <b>/fill</b>
	 * @param loc1 point 1
	 * @param loc2 point 2
	 * @param material material to apply on the blocks
	 * @param physics whether to apply physics or not
	 */
	public static void setSectionCuboid(Location loc1, Location loc2, Material material, boolean physics) {
		if (!material.isBlock()) 
			throw new IllegalArgumentException("The specified material is not a placeable block!");
		World world = loc1.getWorld();
		Object nmsWorld = getWorld(world);
		Object blockData = getBlockData(material);
		if (blockData == null)
			throw new NullPointerException("Unable to retrieve block data for the corresponding material.");
		int x1 = Math.min(loc1.getBlockX(), loc2.getBlockX());
		int y1 = Math.min(loc1.getBlockY(), loc2.getBlockY());
		int z1 = Math.min(loc1.getBlockZ(), loc2.getBlockZ());
		int x2 = Math.max(loc1.getBlockX(), loc2.getBlockX());
		int y2 = Math.max(loc1.getBlockY(), loc2.getBlockY());
		int z2 = Math.max(loc1.getBlockZ(), loc2.getBlockZ());
		int baseX = x1;
		int baseY = y1;
		int baseZ = z1;
		int sizeX = Math.abs(x2 - x1) + 1;
		int sizeY = Math.abs(y2 - y1) + 1;
		int sizeZ = Math.abs(z2 - z1) + 1;
		int x3 = 0, y3 = 0, z3 = 0;
		Location location = new Location(loc1.getWorld(), baseX + x3, baseY + y3, baseZ + z3);
		int cuboidSize = sizeX*sizeY*sizeZ;
		Object blockPosition = newMutableBlockPosition(location);
		for (int i = 0; i < cuboidSize ; i++) {
			int x = location.getBlockX();
			int y = location.getBlockY();
			int z = location.getBlockZ();
			Object nmsChunk = getChunkAt(nmsWorld, location);
			int j = x & 15;
			int k = y & 15;
			int l = z & 15;
			Object[] sections = getSections(nmsChunk);
			Object section = getSection(nmsChunk, sections, y);
			removeIfTileEntity(nmsWorld, blockPosition);
			setTypeChunkSection(section, j, k, l, blockData);
			setBlockPosition(blockPosition, x, y, z);
			updateBlock(nmsWorld, blockPosition, blockData, physics);
			if (++x3 >= sizeX) {
				x3 = 0;
				if (++y3 >= sizeY) {
					y3 = 0;
					++z3;
				}
			}
			location.setX(baseX + x3);
			location.setY(baseY + y3);
			location.setZ(baseZ + z3);
		}
	}

	/**
	 * Has the same behavior as {@link #setSectionBlocks(World, Location, ItemStack)} but creates a cuboid from a location
	 * to another location similar to the vanilla command <b>/fill</b>
	 * @param loc1 point 1
	 * @param loc2 point 2
	 * @param itemStack ItemStack to apply on the blocks
	 */
	public static void setSectionCuboid(Location loc1, Location loc2, ItemStack itemStack) {
		setSectionCuboid(loc1, loc2, itemStack, false);
	}

	/**
	 * Has the same behavior as {@link #setSectionBlocks(World, Location, ItemStack)} but creates a cuboid from a location
	 * to another location similar to the vanilla command <b>/fill</b>
	 * @param loc1 point 1
	 * @param loc2 point 2
	 * @param itemStack ItemStack to apply on the blocks
	 */
	public static void setSectionCuboid(Location loc1, Location loc2, ItemStack itemStack, boolean physics) {
		World world = loc1.getWorld();
		Object nmsWorld = getWorld(world);
		Object blockData = getBlockData(itemStack);
		int x1 = Math.min(loc1.getBlockX(), loc2.getBlockX());
		int y1 = Math.min(loc1.getBlockY(), loc2.getBlockY());
		int z1 = Math.min(loc1.getBlockZ(), loc2.getBlockZ());
		int x2 = Math.max(loc1.getBlockX(), loc2.getBlockX());
		int y2 = Math.max(loc1.getBlockY(), loc2.getBlockY());
		int z2 = Math.max(loc1.getBlockZ(), loc2.getBlockZ());
		int baseX = x1;
		int baseY = y1;
		int baseZ = z1;
		int sizeX = Math.abs(x2 - x1) + 1;
		int sizeY = Math.abs(y2 - y1) + 1;
		int sizeZ = Math.abs(z2 - z1) + 1;
		int x3 = 0, y3 = 0, z3 = 0;
		Location location = new Location(loc1.getWorld(), baseX + x3, baseY + y3, baseZ + z3);
		int cuboidSize = sizeX*sizeY*sizeZ;
		Object blockPosition = newMutableBlockPosition(location);
		for (int i = 0; i < cuboidSize ; i++) {
			int x = location.getBlockX();
			int y = location.getBlockY();
			int z = location.getBlockZ();
			Object nmsChunk = getChunkAt(nmsWorld, location);
			int j = x & 15;
			int k = y & 15;
			int l = z & 15;
			Object[] sections = getSections(nmsChunk);
			Object section = getSection(nmsChunk, sections, y);
			removeIfTileEntity(nmsWorld, blockPosition);
			setTypeChunkSection(section, j, k, l, blockData);
			setBlockPosition(blockPosition, x, y, z);
			updateBlock(nmsWorld, blockPosition, blockData, physics);
			if (++x3 >= sizeX) {
				x3 = 0;
				if (++y3 >= sizeY) {
					y3 = 0;
					++z3;
				}
			}
			location.setX(baseX + x3);
			location.setY(baseY + y3);
			location.setZ(baseZ + z3);
		}
	}

	/**
	 * Has the same behavior as {@link #setSectionBlocks(World, Location, ItemStack)} but asynchronously creates a cuboid from a location
	 * to another location similar to the vanilla command <b>/fill</b>
	 * @param loc1 point 1
	 * @param loc2 point 2
	 * @param itemStack ItemStack to apply on the blocks
	 */
	public static CompletableFuture<Void> setSectionCuboidAsynchronously(Location loc1, Location loc2, ItemStack itemStack) {
		return setSectionCuboidAsynchronously(loc1, loc2, itemStack, false);
	}

	/**
	 * Has the same behavior as {@link #setSectionBlocks(World, Location, ItemStack)} but asynchronously creates a cuboid from a location
	 * to another location similar to the vanilla command <b>/fill</b>
	 * @param loc1 point 1
	 * @param loc2 point 2
	 * @param itemStack ItemStack to apply on the blocks
	 */
	public static CompletableFuture<Void> setSectionCuboidAsynchronously(Location loc1, Location loc2, ItemStack itemStack, boolean physics) {
		World world = loc1.getWorld();
		Object nmsWorld = getWorld(world);
		Object blockData = getBlockData(itemStack);
		int x1 = Math.min(loc1.getBlockX(), loc2.getBlockX());
		int y1 = Math.min(loc1.getBlockY(), loc2.getBlockY());
		int z1 = Math.min(loc1.getBlockZ(), loc2.getBlockZ());
		int x2 = Math.max(loc1.getBlockX(), loc2.getBlockX());
		int y2 = Math.max(loc1.getBlockY(), loc2.getBlockY());
		int z2 = Math.max(loc1.getBlockZ(), loc2.getBlockZ());
		int baseX = x1;
		int baseY = y1;
		int baseZ = z1;
		int sizeX = Math.abs(x2 - x1) + 1;
		int sizeY = Math.abs(y2 - y1) + 1;
		int sizeZ = Math.abs(z2 - z1) + 1;
		int x3 = 0, y3 = 0, z3 = 0;
		Location location = new Location(loc1.getWorld(), baseX + x3, baseY + y3, baseZ + z3);
		int cuboidSize = sizeX*sizeY*sizeZ;
		Object blockPosition = newMutableBlockPosition(location);
		CompletableFuture<Void> workloadFinishFuture = new CompletableFuture<>();
		WorkloadRunnable workloadRunnable = new WorkloadRunnable();
		BukkitTask workloadTask = Bukkit.getScheduler().runTaskTimer(PLUGIN, workloadRunnable, 1, 1);
		for (int i = 0; i < cuboidSize ; i++) {
			SectionSetWorkload workload = new SectionSetWorkload(nmsWorld, blockPosition, blockData, location.clone(), physics);
			if (++x3 >= sizeX) {
				x3 = 0;
				if (++y3 >= sizeY) {
					y3 = 0;
					++z3;
				}
			}
			location.setX(baseX + x3);
			location.setY(baseY + y3);
			location.setZ(baseZ + z3);
			workloadRunnable.addWorkload(workload);
		}
		workloadRunnable.whenComplete(() -> {
			workloadFinishFuture.complete(null);
			workloadTask.cancel();
		});
		return workloadFinishFuture;
	}

	private static Object getSection(Object nmsChunk, Object[] sections, int y) {
		return BLOCK_UPDATER.getSection(nmsChunk, sections, y);
	}

	private static Object[] getSections(Object nmsChunk) {
		try {
			return (Object[]) CHUNK_GET_SECTIONS.invoke(nmsChunk);
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return null;
	}

	private static void setTypeChunkSection(Object chunkSection, int x, int y, int z, Object blockData) {
		try {
			CHUNK_SECTION_SET_TYPE.invoke(chunkSection, x, y, z, blockData);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	private static void setTypeAndData(Object nmsWorld, Object blockPosition, Object blockData, int physics) {
		try {
			SET_TYPE_AND_DATA.invoke(nmsWorld, blockPosition, blockData, physics);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	private static void setType(Object chunk, Object blockPosition, Object blockData, boolean physics) {
		BLOCK_UPDATER.setType(chunk, blockPosition, blockData, physics);
	}

	private static Object getChunkAt(Object world, Location loc) {
		try {
			return WORLD_GET_CHUNK.invoke(world, loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return null;
	}

	private static Object getChunkAt(Object world, int x, int z) {
		try {
			return WORLD_GET_CHUNK.invoke(world, x >> 4, z >> 4);
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return null;
	}

	private static Object getNMSWorld(@Nonnull World world) {
		try {
			return WORLD_GET_HANDLE.invoke(world);
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return null;
	}

	private static @Nullable Object getNMSBlockData(@Nullable ItemStack itemStack) {
		try {
			if(itemStack == null) return null;
			Object nmsItemStack = NMS_ITEM_STACK_COPY.invoke(itemStack);
			if (nmsItemStack == null) return null;
			Object nmsItem = NMS_ITEM_STACK_TO_ITEM.invoke(nmsItemStack);
			Object block = NMS_BLOCK_FROM_ITEM.invoke(nmsItem);
			if(ReflectionUtils.VER < 8) return block;
			return ITEM_TO_BLOCK_DATA.invoke(block);
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private static boolean isTileEntity(Object nmsWorld, Object blockPosition) {
		return TILE_ENTITY_MANAGER.isTileEntity(nmsWorld, blockPosition);
	}
	
	private static boolean removeIfTileEntity(Object nmsWorld, Object blockPosition) {
		if(!isTileEntity(nmsWorld, blockPosition)) return false;
		TILE_ENTITY_MANAGER.destroyTileEntity(nmsWorld, blockPosition);
		return true;
	}

	/**
	 * Refreshes a block so it appears to the players
	 * @param world nms world {@link #getWorld(World)}
	 * @param blockPosition nms block position {@link #newBlockPosition(Object, Object, Object, Object)}
	 * @param blockData nms block data {@link #getBlockData(Material)}
	 * @param physics whether physics should be applied or not
	 */
	public static void updateBlock(Object world, Object blockPosition, Object blockData, boolean physics) {
		BLOCK_UPDATER.update(world, blockPosition, blockData, physics ? 3 : 2);
	}

	/**
	 * 
	 * @param world (Bukkit world) can be null for versions 1.8+
	 * @param x point
	 * @param y point
	 * @param z point
	 * @return constructs an unmodifiable block position
	 */
	public static Object newBlockPosition(@Nullable Object world, Object x, Object y, Object z) {
		try {
			return BLOCK_POSITION_CONSTRUCTOR.newBlockPosition(world, x, y, z);
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 
	 * @param world (Bukkit world) can be null for 1.8+
	 * @param x x pos
	 * @param y y pos
	 * @param z z pos
	 * @return constructs a mutable block position that can be modified using {@link #setBlockPosition(Object, Object, Object, Object)}
	 */
	public static Object newMutableBlockPosition(@Nullable Object world, Object x, Object y, Object z) {
		try {
			return BLOCK_POSITION_CONSTRUCTOR.newMutableBlockPosition(world, x, y, z);
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 
	 * @param location Location to get coordinates from
	 * @return constructs a mutable block position that can be modified using {@link #setBlockPosition(Object, Object, Object, Object)}
	 */
	public static Object newMutableBlockPosition(Location location) {
		try {
			return BLOCK_POSITION_CONSTRUCTOR.newMutableBlockPosition(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 
	 * @param mutableBlockPosition MutableBlockPosition to modify
	 * @param x new x pos
	 * @param y new y pos
	 * @param z new z pos
	 * @return modified MutableBlockPosition (no need to set the variable to the returned MutableBlockPosition)
	 */
	public static Object setBlockPosition(Object mutableBlockPosition, Object x, Object y, Object z) {
		try {
			return BLOCK_POSITION_CONSTRUCTOR.set(mutableBlockPosition, x, y, z);
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 
	 * @param itemStack bukkit ItemStack
	 * @return nms block data from bukkit item stack
	 * @throws IllegalArgumentException if material is not a block
	 */
	public static @Nonnull Object getBlockData(@Nonnull ItemStack itemStack) {
		Object blockData = BLOCK_DATA_GETTER.fromItemStack(itemStack);
		if(blockData == null) throw new IllegalArgumentException("Couldn't convert specified itemstack to block data");
		return blockData;
	}

	/**
	 * 
	 * @param material to get block data for
	 * @return stored nms block data for the specified material
	 */
	public static @Nullable Object getBlockData(@Nullable Material material) {
		return NMS_BLOCK_MATERIALS.get(material);
	}

	/**
	 * This method should get block data even if block is not actually placed i.e doesn't have location
	 * <p>Doesn't retrieve the tile entity as of now</p>
	 * @param block bukkit block to cast to nms block data
	 * @return nms block data from bukkit block
	 */
	public static @Nonnull Object getBlockData(Block block) {
		Object blockData = BLOCK_DATA_GETTER.fromBlock(block);
		return blockData != null ? blockData : AIR_BLOCK_DATA;
	}

	/**
	 * 
	 * @return nms air block data
	 */
	public static Object getAirBlockData() {
		return AIR_BLOCK_DATA;
	}

	/**
	 * 
	 * @param world to get nms world for
	 * @return stored nms world for the specified world
	 */
	public static Object getWorld(World world) {
		return NMS_WORLDS.get(world);
	}

	/**
	 * 
	 * @param worldName to get nms world for
	 * @return stored nms world for the specified world name
	 */
	public static Object getWorld(String worldName) {
		return NMS_WORLD_NAMES.get(worldName);
	}

	/**
	 * @return all available block materials for the current version separated by commas as follows: <p><i>dirt, stone, glass, etc...</i>
	 */
	public static String getAvailableBlockMaterials() {
		return AVAILABLE_BLOCKS;
	}

	/**
	 * physics: 3 = yes, 2 = no
	 * @return methods that accept nms objects
	 */
	public static UncheckedSetters getUncheckedSetters() {
		return UNCHECKED_SETTERS;
	}

	/**
	 * 
	 * @apiNote physics: 3 = yes, 2 = no
	 *
	 */
	public static class UncheckedSetters {

		/**
		 * 
		 * @param nmsWorld using {@link BlockChanger#getWorld(World) getWorld(World)} or {@link BlockChanger#getWorld(String) getWorld(String)}
		 * @param blockPosition using {@link BlockChanger#newMutableBlockPosition(Location) newMutableBlockPosition(Location)}
		 * @param nmsBlockData {@link BlockChanger#getBlockData(ItemStack) getBlockData(ItemStack)} or {@link BlockChanger#getBlockData(Material) getBlockData(Material)}
		 * @param physics 3 = applies physics, 2 = doesn't
		 * <p><i>blockPosition</i> can be further modified with new coordinates using {@link BlockChanger#setBlockPosition(Object, Object, Object, Object)}
		 */
		public void setBlock(Object nmsWorld, Object blockPosition, Object nmsBlockData, int physics) {
			setTypeAndData(nmsWorld, blockPosition, nmsBlockData, physics);
		}

		/**
		 * 
		 * @param nmsWorld using {@link BlockChanger#getWorld(World) getWorld(World)} or {@link BlockChanger#getWorld(String) getWorld(String)}
		 * @param blockPosition using {@link BlockChanger#newMutableBlockPosition(Location) newMutableBlockPosition(Location)}
		 * @param nmsBlockData {@link BlockChanger#getBlockData(ItemStack) getBlockData(ItemStack)} or {@link BlockChanger#getBlockData(Material) getBlockData(Material)}
		 * @param x x coordinate of the block
		 * @param z z coordinate of the block
		 * @param physics 3 = applies physics, 2 = doesn't
		 * <p><i>blockPosition</i> can be further modified with new coordinates using {@link BlockChanger#setBlockPosition(Object, Object, Object, Object)}
		 */
		public void setChunkBlock(Object nmsWorld, Object blockPosition, Object nmsBlockData, int x, int z, boolean physics) {
			Object chunk = getChunkAt(nmsWorld, x, z);
			setType(chunk, blockPosition, nmsBlockData, physics);
			updateBlock(nmsWorld, blockPosition, nmsBlockData, physics);
		}

		/**
		 * 
		 * @param nmsWorld using {@link BlockChanger#getWorld(World) getWorld(World)} or {@link BlockChanger#getWorld(String) getWorld(String)}
		 * @param blockPosition using {@link BlockChanger#newMutableBlockPosition(Location) newMutableBlockPosition(Location)}
		 * @param nmsBlockData {@link BlockChanger#getBlockData(ItemStack) getBlockData(ItemStack)} or {@link BlockChanger#getBlockData(Material) getBlockData(Material)}
		 * @param x x coordinate of the block
		 * @param y y coordinate of the block
		 * @param z z coordinate of the block
		 * @param physics 3 = applies physics, 2 = doesn't
		 * <p><i>blockPosition</i> can be further modified with new coordinates using {@link BlockChanger#setBlockPosition(Object, Object, Object, Object)}
		 */
		public void setSectionBlock(Object nmsWorld, Object blockPosition, Object nmsBlockData, int x, int y, int z, boolean physics) {
			Object nmsChunk = getChunkAt(nmsWorld, x, z);
			int j = x & 15;
			int k = y & 15;
			int l = z & 15;
			Object[] sections = getSections(nmsChunk);
			Object section = getSection(nmsChunk, sections, y);
			setTypeChunkSection(section, j, k, l, nmsBlockData);
			updateBlock(nmsWorld, blockPosition, nmsWorld, physics);
		}

	}

	private interface TileEntityManager {
		
		default Object getCapturedTileEntities(Object nmsWorld) {
			try {
				return WORLD_CAPTURED_TILE_ENTITIES.invoke(nmsWorld);
			} catch (Throwable e) {
				e.printStackTrace();
			}
			return null;
		}
		
		default boolean isTileEntity(Object nmsWorld, Object blockPosition) {
			try {
				return (boolean)IS_TILE_ENTITY.invoke(getCapturedTileEntities(nmsWorld), blockPosition);
			} catch (Throwable e) {
				e.printStackTrace();
			}
			return false;
		}
		
		default void destroyTileEntity(Object nmsWorld, Object blockPosition) {
			try {
				WORLD_REMOVE_TILE_ENTITY.invoke(nmsWorld, blockPosition);
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		
		/*
		Store bukkit block variable {
			Block block = ...
		}
		Get block data (title entity data still exists in old the bukkit block variable) {
		 	Object blockData = BlockChanger.getBlockData(block);
		}
		Set block using BlockChanger within the method {
			setType(...)
		}
		Check if block is a title entity {
			isTitleEntity
		}
		Get tile entity that was stored in the bukkit block variable {
			CraftBlockState craftBlockState = (CraftBlockState)block.getState();
			// getState() creates a new block state with the location of that block
			TileEntity nmsTileEntity = craftBlockState.getTileEntity();
		}
		Set tile entity using BlockChanger {
			<Use nms method that applies tile entity on the block>
		}
		*/
		
	}
	
	private static class TileEntityManagerSupported implements TileEntityManager {}
	
	private static class TileEntityManagerDummy implements TileEntityManager {
		
		@Override
		public Object getCapturedTileEntities(Object nmsWorld) {
			return null;
		}
		
		@Override
		public boolean isTileEntity(Object nmsWorld, Object blockPosition) {
			return false;
		}
		
		@Override
		public void destroyTileEntity(Object nmsWorld, Object blockPosition) {}
		
	}
	
	private interface BlockDataRetriever {

		default Object getNMSItem(ItemStack itemStack) throws Throwable {
			if(itemStack == null) throw new NullPointerException("ItemStack is null!");
			if(itemStack.getType() == Material.AIR) return null;
			Object nmsItemStack = NMS_ITEM_STACK_COPY.invoke(itemStack);
			if (nmsItemStack == null) throw new IllegalArgumentException("Failed to get NMS ItemStack!");
			return NMS_ITEM_STACK_TO_ITEM.invoke(nmsItemStack);
		}

		// 1.7-1.12 requires 2 methods to get block data
		default Object fromBlock(Block block) {
			try {
				Object nmsBlock = CRAFT_BLOCK_GET_NMS_BLOCK.invoke(block);
				return NMS_BLOCK_GET_BLOCK_DATA.invoke(nmsBlock);
			} catch (Throwable e) {
				e.printStackTrace();
			}
			return null;
		}

		Object fromItemStack(ItemStack itemStack);

	}

	// 1.13+ or 1.8+ without data support
	private static class BlockDataGetter implements BlockDataRetriever {

		@Override
		public Object fromItemStack(ItemStack itemStack) {
			try {
				Object block = NMS_BLOCK_FROM_ITEM.invoke(getNMSItem(itemStack));
				return ITEM_TO_BLOCK_DATA.invoke(block);
			} catch (Throwable e) {
				e.printStackTrace();
			}
			return null;
		}
		
		// 1.13+ one method to get block data (getNMS())
		@Override
		public Object fromBlock(Block block) {
			try {
				return NMS_BLOCK_GET_BLOCK_DATA.invoke(block);
			} catch (Throwable e) {
				e.printStackTrace();
			}
			return null;
		}

	}

	// 1.8-1.12
	private static class BlockDataGetterLegacy implements BlockDataRetriever {

		@Override
		public Object fromItemStack(ItemStack itemStack) {
			try {
				Object nmsItem = getNMSItem(itemStack);
				if(nmsItem == null) return AIR_BLOCK_DATA;
				Object block = NMS_BLOCK_FROM_ITEM.invoke(nmsItem);
				short data = itemStack.getDurability();
				return data > 0 ? BLOCK_DATA_FROM_LEGACY_DATA.invoke(block, data) : ITEM_TO_BLOCK_DATA.invoke(block);
			} catch (Throwable e) {
				e.printStackTrace();
			}
			return null;
		}

	}

	// 1.7
	private static class BlockDataGetterAncient implements BlockDataRetriever {

		@Override
		public Object fromItemStack(ItemStack itemStack) {
			try {
				Object nmsItem = getNMSItem(itemStack);
				if(nmsItem == null) return AIR_BLOCK_DATA;
				return NMS_BLOCK_FROM_ITEM.invoke(nmsItem);
			} catch (Throwable e) {
				e.printStackTrace();
			}
			return null;
		}

	}

	private static interface Workload {

		boolean compute();

	}

	private static class WorkloadRunnable implements Runnable {

		private static final double MAX_MILLIS_PER_TICK = 10.0;
		private static final int MAX_NANOS_PER_TICK = (int) (MAX_MILLIS_PER_TICK * 1E6);

		private final Deque<Workload> workloadDeque = new ArrayDeque<>();

		public void addWorkload(Workload workload) {
			this.workloadDeque.add(workload);
		}

		public void whenComplete(Runnable runnable) {
			WhenCompleteWorkload workload = new WhenCompleteWorkload(runnable);
			this.workloadDeque.add(workload);
		}

		@Override
		public void run() {
			long stopTime = System.nanoTime() + MAX_NANOS_PER_TICK;

			Workload nextLoad;

			while (System.nanoTime() <= stopTime && (nextLoad = this.workloadDeque.poll()) != null) {
				nextLoad.compute();
			}
		}

	}

	private static class BlockSetWorkload implements Workload {

		private Object nmsWorld;
		private Object blockPosition;
		private Object blockData;
		private Location location;
		private int physics;

		public BlockSetWorkload(Object nmsWorld, Object blockPosition, Object blockData, Location location, boolean physics) {
			this.nmsWorld = nmsWorld;
			this.blockPosition = blockPosition;
			this.blockData = blockData;
			this.location = location;
			this.physics = physics ? 3 : 2;
		}

		@Override
		public boolean compute() {
			BlockChanger.setBlockPosition(blockPosition, location.getBlockX(), location.getBlockY(), location.getBlockZ());
			BlockChanger.removeIfTileEntity(nmsWorld, blockPosition);
			BlockChanger.setTypeAndData(nmsWorld, blockPosition, blockData, physics);
			return true;
		}

	}
	
	private static class ChunkSetWorkload implements Workload {

		private Object nmsWorld;
		private Object blockPosition;
		private Object blockData;
		private Location location;
		private boolean physics;

		public ChunkSetWorkload(Object nmsWorld, Object blockPosition, Object blockData, Location location, boolean physics) {
			this.nmsWorld = nmsWorld;
			this.blockPosition = blockPosition;
			this.blockData = blockData;
			this.location = location;
			this.physics = physics;
		}

		@Override
		public boolean compute() {
			BlockChanger.setBlockPosition(blockPosition, location.getBlockX(), location.getBlockY(), location.getBlockZ());
			Object chunk = BlockChanger.getChunkAt(nmsWorld, location.getBlockX(), location.getBlockZ());
			BlockChanger.removeIfTileEntity(nmsWorld, blockPosition);
			BlockChanger.setType(chunk, blockPosition, blockData, physics);
			BlockChanger.updateBlock(nmsWorld, blockPosition, blockData, physics);
			return true;
		}

	}

	private static class SectionSetWorkload implements Workload {

		private Object nmsWorld;
		private Object blockPosition;
		private Object blockData;
		private Location location;
		private boolean physics;

		public SectionSetWorkload(Object nmsWorld, Object blockPosition, Object blockData, Location location, boolean physics) {
			this.nmsWorld = nmsWorld;
			this.blockPosition = blockPosition;
			this.blockData = blockData;
			this.location = location;
			this.physics = physics;
		}

		@Override
		public boolean compute() {
			BlockChanger.setBlockPosition(blockPosition, location.getBlockX(), location.getBlockY(), location.getBlockZ());
			int x = location.getBlockX();
			int y = location.getBlockY();
			int z = location.getBlockZ();
			Object nmsChunk = BlockChanger.getChunkAt(nmsWorld, x, z);
			int j = x & 15;
			int k = y & 15;
			int l = z & 15;
			Object[] sections = BlockChanger.getSections(nmsChunk);
			Object section = BlockChanger.getSection(nmsChunk, sections, y);
			BlockChanger.removeIfTileEntity(nmsWorld, blockPosition);
			BlockChanger.setTypeChunkSection(section, j, k, l, blockData);
			BlockChanger.updateBlock(nmsWorld, blockPosition, blockData, physics);
			return true;
		}

	}

	private static class WhenCompleteWorkload implements Workload {

		private Runnable runnable;

		public WhenCompleteWorkload(Runnable runnable) {
			this.runnable = runnable;
		}

		@Override
		public boolean compute() {
			runnable.run();
			return false;
		}

	}

}

interface BlockPositionConstructor {

	Object newBlockPosition(Object world, Object x, Object y, Object z);
	Object newMutableBlockPosition(Object world, Object x, Object y, Object z);
	Object set(Object mutableBlockPosition, Object x, Object y, Object z);

}


interface BlockUpdater {

	void setType(Object chunk, Object blockPosition, Object blockData, boolean physics);
	void update(Object world, Object blockPosition, Object blockData, int physics);
	Object getSection(Object nmsChunk, Object[] sections, int y);
	int getSectionIndex(Object nmsChunk, int y);

}

class BlockPositionNormal implements BlockPositionConstructor {

	private MethodHandle blockPositionConstructor;
	private MethodHandle mutableBlockPositionConstructor;
	private MethodHandle mutableBlockPositionSet;

	public BlockPositionNormal(MethodHandle blockPositionXYZ,
			MethodHandle mutableBlockPositionXYZ, MethodHandle mutableBlockPositionSet) {
		this.blockPositionConstructor = blockPositionXYZ;
		this.mutableBlockPositionConstructor = mutableBlockPositionXYZ;
		this.mutableBlockPositionSet = mutableBlockPositionSet;
	}

	@Override
	public Object newBlockPosition(Object world, Object x, Object y, Object z) {
		try {
			return blockPositionConstructor.invoke(x, y, z);
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Object newMutableBlockPosition(Object world, Object x, Object y, Object z) {
		try {
			return mutableBlockPositionConstructor.invoke(x, y, z);
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Object set(Object mutableBlockPosition, Object x, Object y, Object z) {
		try {
			return mutableBlockPositionSet.invoke(mutableBlockPosition, x, y, z);
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return null;
	}

}

class BlockPositionAncient implements BlockPositionConstructor {

	private MethodHandle blockPositionConstructor;
	private MethodHandle mutableBlockPositionConstructor;

	public BlockPositionAncient(MethodHandle blockPositionXYZ,
			MethodHandle mutableBlockPositionXYZ) {
		this.blockPositionConstructor = blockPositionXYZ;
		this.mutableBlockPositionConstructor = mutableBlockPositionXYZ;
	}

	@Override
	public Object newBlockPosition(Object world, Object x, Object y, Object z) {
		try {
			return blockPositionConstructor.invoke(world, x, y, z);
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Object newMutableBlockPosition(Object world, Object x, Object y, Object z) {
		try {
			return mutableBlockPositionConstructor.invoke(x, y, z);
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Object set(Object mutableBlockPosition, Object x, Object y, Object z) {
		try {
			Location loc = (Location)mutableBlockPosition;
			loc.setX((double)x);
			loc.setY((double)y);
			loc.setZ((double)z);
			return loc;	
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return null;
	}

}

class BlockUpdaterAncient implements BlockUpdater {

	private MethodHandle blockNotify;
	private MethodHandle chunkSetType;
	private MethodHandle chunkSection;
	private MethodHandle setSectionElement;

	public BlockUpdaterAncient(MethodHandle blockNotify, MethodHandle chunkSetType, 
			MethodHandle chunkSection, MethodHandle setSectionElement) {
		this.blockNotify = blockNotify;
		this.chunkSetType = chunkSetType;
		this.chunkSection = chunkSection;
		this.setSectionElement = setSectionElement;
	}

	@Override
	public void update(Object world, Object blockPosition, Object blockData, int physics) {
		try {
			Location loc = (Location)blockPosition;
			blockNotify.invoke(world, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	@Override
	public void setType(Object chunk, Object blockPosition, Object blockData, boolean physics) {
		try {
			chunkSetType.invoke(chunk, blockPosition, blockData);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	@Override
	public Object getSection(Object nmsChunk, Object[] sections, int y) {
		Object section = sections[getSectionIndex(null, y)];
		if(section == null) {
			try {
				section = chunkSection.invoke(y >> 4 << 4, true);
				setSectionElement.invoke(sections, y >> 4, section);
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		return section;
	}

	@Override
	public int getSectionIndex(Object nmsChunk, int y) {
		int i = y >> 4;
		return i <= 15 ? i : 15;
	}

}

class BlockUpdaterLegacy implements BlockUpdater {

	private MethodHandle blockNotify;
	private MethodHandle chunkSetType;
	private MethodHandle chunkSection;
	private MethodHandle setSectionElement;

	public BlockUpdaterLegacy(MethodHandle blockNotify, MethodHandle chunkSetType, 
			MethodHandle chunkSection, MethodHandle setSectionElement) {
		this.blockNotify = blockNotify;
		this.chunkSetType = chunkSetType;
		this.chunkSection = chunkSection;
		this.setSectionElement = setSectionElement;
	}

	@Override
	public void update(Object world, Object blockPosition, Object blockData, int physics) {
		try {
			blockNotify.invoke(world, blockPosition);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	@Override
	public void setType(Object chunk, Object blockPosition, Object blockData, boolean physics) {
		try {
			chunkSetType.invoke(chunk, blockPosition, blockData);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	@Override
	public Object getSection(Object nmsChunk, Object[] sections, int y) {
		Object section = sections[getSectionIndex(null, y)];
		if(section == null) {
			try {
				section = chunkSection.invoke(y >> 4 << 4, true);
				setSectionElement.invoke(sections, y >> 4, section);
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		return section;
	}

	@Override
	public int getSectionIndex(Object nmsChunk, int y) {
		int i = y >> 4;
		return i <= 15 ? i : 15;
	}

}

class BlockUpdater9 implements BlockUpdater {

	private MethodHandle blockNotify;
	private MethodHandle chunkSetType;
	private MethodHandle chunkSection;
	private MethodHandle setSectionElement;

	public BlockUpdater9(MethodHandle blockNotify, MethodHandle chunkSetType, MethodHandle chunkSection, MethodHandle setSectionElement) {
		this.blockNotify = blockNotify;
		this.chunkSetType= chunkSetType;
		this.chunkSection = chunkSection;
		this.setSectionElement = setSectionElement;
	}

	@Override
	public void update(Object world, Object blockPosition, Object blockData, int physics) {
		try {
			blockNotify.invoke(world, blockPosition, blockData, blockData, physics);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	@Override
	public void setType(Object chunk, Object blockPosition, Object blockData, boolean physics) {
		try {
			chunkSetType.invoke(chunk, blockPosition, blockData, physics);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	@Override
	public Object getSection(Object nmsChunk, Object[] sections, int y) {
		Object section = sections[getSectionIndex(null, y)];
		if(section == null) {
			try {
				section = chunkSection.invoke(y >> 4 << 4, true);
				setSectionElement.invoke(sections, y >> 4, section);
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		return section;
	}

	@Override
	public int getSectionIndex(Object nmsChunk, int y) {
		int i = y >> 4;
		return i <= 15 ? i : 15;
	}

}

class BlockUpdater13 implements BlockUpdater {

	private MethodHandle blockNotify;
	private MethodHandle chunkSetType;
	private MethodHandle chunkSection;
	private MethodHandle setSectionElement;

	public BlockUpdater13(MethodHandle blockNotify, MethodHandle chunkSetType, MethodHandle chunkSection, MethodHandle setSectionElement) {
		this.blockNotify = blockNotify;
		this.chunkSetType= chunkSetType;
		this.chunkSection = chunkSection;
		this.setSectionElement = setSectionElement;
	}

	@Override
	public void update(Object world, Object blockPosition, Object blockData, int physics) {
		try {
			blockNotify.invoke(world, blockPosition, blockData, blockData, physics);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	@Override
	public void setType(Object chunk, Object blockPosition, Object blockData, boolean physics) {
		try {
			chunkSetType.invoke(chunk, blockPosition, blockData, physics);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	@Override
	public Object getSection(Object nmsChunk, Object[] sections, int y) {
		Object section = sections[getSectionIndex(null, y)];
		if(section == null) {
			try {
				section = chunkSection.invoke(y >> 4 << 4);
				setSectionElement.invoke(sections, y >> 4, section);
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		return section;
	}

	@Override
	public int getSectionIndex(Object nmsChunk, int y) {
		int i = y >> 4;
		return i <= 15 ? i : 15;
	}

}

class BlockUpdater17 implements BlockUpdater {

	private MethodHandle blockNotify;
	private MethodHandle chunkSetType;
	private MethodHandle sectionIndexGetter;
	private MethodHandle chunkSection;
	private MethodHandle setSectionElement;

	public BlockUpdater17(MethodHandle blockNotify, MethodHandle chunkSetType, 
			MethodHandle sectionIndexGetter, MethodHandle chunkSection, MethodHandle setSectionElement) {
		this.blockNotify = blockNotify;
		this.chunkSetType= chunkSetType;
		this.sectionIndexGetter = sectionIndexGetter;
		this.chunkSection = chunkSection;
		this.setSectionElement = setSectionElement;
	}

	@Override
	public void update(Object world, Object blockPosition, Object blockData, int physics) {
		try {
			blockNotify.invoke(world, blockPosition, blockData, blockData, physics);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	@Override
	public void setType(Object chunk, Object blockPosition, Object blockData, boolean physics) {
		try {
			chunkSetType.invoke(chunk, blockPosition, blockData, physics);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	@Override
	public Object getSection(Object nmsChunk, Object[] sections, int y) {
		Object section = sections[getSectionIndex(nmsChunk, y)];
		if(section == null) {
			try {
				section = chunkSection.invoke(y >> 4 << 4);
				setSectionElement.invoke(sections, y >> 4, section);
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		return section;
	}

	@Override
	public int getSectionIndex(Object nmsChunk, int y) {
		int sectionIndex = -1;
		try {
			sectionIndex = (int)sectionIndexGetter.invoke(nmsChunk, y);
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return sectionIndex <= 15 ? sectionIndex : 15;
	}

}

class BlockUpdaterLatest implements BlockUpdater {

	private MethodHandle blockNotify;
	private MethodHandle chunkSetType;
	private MethodHandle sectionIndexGetter;
	private MethodHandle levelHeightAccessorGetter;

	public BlockUpdaterLatest(MethodHandle blockNotify, MethodHandle chunkSetType, 
			MethodHandle sectionIndexGetter, MethodHandle levelHeightAccessorGetter) {
		this.blockNotify = blockNotify;
		this.chunkSetType= chunkSetType;
		this.sectionIndexGetter = sectionIndexGetter;
		this.levelHeightAccessorGetter = levelHeightAccessorGetter;
	}

	@Override
	public void update(Object world, Object blockPosition, Object blockData, int physics) {
		try {
			blockNotify.invoke(world, blockPosition, blockData, blockData, physics);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	@Override
	public void setType(Object chunk, Object blockPosition, Object blockData, boolean physics) {
		try {
			chunkSetType.invoke(chunk, blockPosition, blockData, physics);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	@Override
	public Object getSection(Object nmsChunk, Object[] sections, int y) {
		return sections[getSectionIndex(nmsChunk, y)];
	}

	public Object getLevelHeightAccessor(Object nmsChunk) {
		try {
			return levelHeightAccessorGetter.invoke(nmsChunk);
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public int getSectionIndex(Object nmsChunk, int y) {
		Object levelHeightAccessor = getLevelHeightAccessor(nmsChunk);
		try {
			return (int)sectionIndexGetter.invoke(levelHeightAccessor, y);
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return -1;
	}

}

/**
 * <b>ReflectionUtils</b> - Reflection handler for NMS and CraftBukkit.<br>
 * Caches the packet related methods and is asynchronous.
 * <p>
 * This class does not handle null checks as most of the requests are from the
 * other utility classes that already handle null checks.
 * <p>
 * <a href="https://wiki.vg/Protocol">Clientbound Packets</a> are considered fake
 * updates to the client without changing the actual data. Since all the data is handled
 * by the server.
 * <p>
 * A useful resource used to compare mappings is <a href="https://minidigger.github.io/MiniMappingViewer/#/spigot">Mini's Mapping Viewer</a>
 *
 * @author Crypto Morin
 * @version 6.0.1
 */
final class ReflectionUtils {
	/**
	 * We use reflection mainly to avoid writing a new class for version barrier.
	 * The version barrier is for NMS that uses the Minecraft version as the main package name.
	 * <p>
	 * E.g. EntityPlayer in 1.15 is in the class {@code net.minecraft.server.v1_15_R1}
	 * but in 1.14 it's in {@code net.minecraft.server.v1_14_R1}
	 * In order to maintain cross-version compatibility we cannot import these classes.
	 * <p>
	 * Performance is not a concern for these specific statically initialized values.
	 */
	public static final String VERSION;

	static { // This needs to be right below VERSION because of initialization order.
		// This package loop is used to avoid implementation-dependant strings like Bukkit.getVersion() or Bukkit.getBukkitVersion()
		// which allows easier testing as well.
		String found = null;
		for (Package pack : Package.getPackages()) {
			String name = pack.getName();

			// .v because there are other packages.
			if (name.startsWith("org.bukkit.craftbukkit.v")) {
				found = pack.getName().split("\\.")[3];

				// Just a final guard to make sure it finds this important class.
				// As a protection for forge+bukkit implementation that tend to mix versions.
				// The real CraftPlayer should exist in the package.
				// Note: Doesn't seem to function properly. Will need to separate the version
				// handler for NMS and CraftBukkit for softwares like catmc.
				try {
					Class.forName("org.bukkit.craftbukkit." + found + ".entity.CraftPlayer");
					break;
				} catch (ClassNotFoundException e) {
					found = null;
				}
			}
		}
		if (found == null) throw new IllegalArgumentException("Failed to parse server version. Could not find any package starting with name: 'org.bukkit.craftbukkit.v'");
		VERSION = found;
	}

	/**
	 * The raw minor version number.
	 * E.g. {@code v1_17_R1} to {@code 17}
	 *
	 * @since 4.0.0
	 */
	public static final int VER = Integer.parseInt(VERSION.substring(1).split("_")[1]);
	/**
	 * Mojang remapped their NMS in 1.17 https://www.spigotmc.org/threads/spigot-bungeecord-1-17.510208/#post-4184317
	 */
	public static final String
	CRAFTBUKKIT = "org.bukkit.craftbukkit." + VERSION + '.',
	NMS = v(17, "net.minecraft.").orElse("net.minecraft.server." + VERSION + '.');
	/**
	 * A nullable public accessible field only available in {@code EntityPlayer}.
	 * This can be null if the player is offline.
	 */
	private static final MethodHandle PLAYER_CONNECTION;
	/**
	 * Responsible for getting the NMS handler {@code EntityPlayer} object for the player.
	 * {@code CraftPlayer} is simply a wrapper for {@code EntityPlayer}.
	 * Used mainly for handling packet related operations.
	 * <p>
	 * This is also where the famous player {@code ping} field comes from!
	 */
	private static final MethodHandle GET_HANDLE;
	/**
	 * Sends a packet to the player's client through a {@code NetworkManager} which
	 * is where {@code ProtocolLib} controls packets by injecting channels!
	 */
	private static final MethodHandle SEND_PACKET;

	static {
		Class<?> entityPlayer = getNMSClass("server.level", "EntityPlayer");
		Class<?> craftPlayer = getCraftClass("entity.CraftPlayer");
		Class<?> playerConnection = getNMSClass("server.network", "PlayerConnection");

		MethodHandles.Lookup lookup = MethodHandles.lookup();
		MethodHandle sendPacket = null, getHandle = null, connection = null;

		try {
			connection = lookup.findGetter(entityPlayer,
					v(17, "b").orElse("playerConnection"), playerConnection);
			getHandle = lookup.findVirtual(craftPlayer, "getHandle", MethodType.methodType(entityPlayer));
			sendPacket = lookup.findVirtual(playerConnection,
					v(18, "a").orElse("sendPacket"),
					MethodType.methodType(void.class, getNMSClass("network.protocol", "Packet")));
		} catch (NoSuchMethodException | NoSuchFieldException | IllegalAccessException ex) {
			ex.printStackTrace();
		}

		PLAYER_CONNECTION = connection;
		SEND_PACKET = sendPacket;
		GET_HANDLE = getHandle;
	}

	private ReflectionUtils() {}

	/**
	 * This method is purely for readability.
	 * No performance is gained.
	 *
	 * @since 5.0.0
	 */
	public static <T> VersionHandler<T> v(int version, T handle) {
		return new VersionHandler<>(version, handle);
	}

	public static <T> CallableVersionHandler<T> v(int version, Callable<T> handle) {
		return new CallableVersionHandler<>(version, handle);
	}

	/**
	 * Checks whether the server version is equal or greater than the given version.
	 *
	 * @param version the version to compare the server version with.
	 *
	 * @return true if the version is equal or newer, otherwise false.
	 * @since 4.0.0
	 */
	public static boolean supports(int version) {return VER >= version;}

	/**
	 * Get a NMS (net.minecraft.server) class which accepts a package for 1.17 compatibility.
	 *
	 * @param newPackage the 1.17 package name.
	 * @param name       the name of the class.
	 *
	 * @return the NMS class or null if not found.
	 * @since 4.0.0
	 */
	@Nullable
	public static Class<?> getNMSClass(@Nonnull String newPackage, @Nonnull String name) {
		if (supports(17)) name = newPackage + '.' + name;
		return getNMSClass(name);
	}

	/**
	 * Get a NMS (net.minecraft.server) class.
	 *
	 * @param name the name of the class.
	 *
	 * @return the NMS class or null if not found.
	 * @since 1.0.0
	 */
	@Nullable
	public static Class<?> getNMSClass(@Nonnull String name) {
		try {
			return Class.forName(NMS + name);
		} catch (ClassNotFoundException ex) {
			ex.printStackTrace();
			return null;
		}
	}

	/**
	 * Sends a packet to the player asynchronously if they're online.
	 * Packets are thread-safe.
	 *
	 * @param player  the player to send the packet to.
	 * @param packets the packets to send.
	 *
	 * @return the async thread handling the packet.
	 * @see #sendPacketSync(Player, Object...)
	 * @since 1.0.0
	 */
	@Nonnull
	public static CompletableFuture<Void> sendPacket(@Nonnull Player player, @Nonnull Object... packets) {
		return CompletableFuture.runAsync(() -> sendPacketSync(player, packets))
				.exceptionally(ex -> {
					ex.printStackTrace();
					return null;
				});
	}

	/**
	 * Sends a packet to the player synchronously if they're online.
	 *
	 * @param player  the player to send the packet to.
	 * @param packets the packets to send.
	 *
	 * @see #sendPacket(Player, Object...)
	 * @since 2.0.0
	 */
	public static void sendPacketSync(@Nonnull Player player, @Nonnull Object... packets) {
		try {
			Object handle = GET_HANDLE.invoke(player);
			Object connection = PLAYER_CONNECTION.invoke(handle);

			// Checking if the connection is not null is enough. There is no need to check if the player is online.
			if (connection != null) {
				for (Object packet : packets) SEND_PACKET.invoke(connection, packet);
			}
		} catch (Throwable throwable) {
			throwable.printStackTrace();
		}
	}

	@Nullable
	public static Object getHandle(@Nonnull Player player) {
		Objects.requireNonNull(player, "Cannot get handle of null player");
		try {
			return GET_HANDLE.invoke(player);
		} catch (Throwable throwable) {
			throwable.printStackTrace();
			return null;
		}
	}

	@Nullable
	public static Object getConnection(@Nonnull Player player) {
		Objects.requireNonNull(player, "Cannot get connection of null player");
		try {
			Object handle = GET_HANDLE.invoke(player);
			return PLAYER_CONNECTION.invoke(handle);
		} catch (Throwable throwable) {
			throwable.printStackTrace();
			return null;
		}
	}

	/**
	 * Get a CraftBukkit (org.bukkit.craftbukkit) class.
	 *
	 * @param name the name of the class to load.
	 *
	 * @return the CraftBukkit class or null if not found.
	 * @since 1.0.0
	 */
	@Nullable
	public static Class<?> getCraftClass(@Nonnull String name) {
		try {
			return Class.forName(CRAFTBUKKIT + name);
		} catch (ClassNotFoundException ex) {
			ex.printStackTrace();
			return null;
		}
	}

	public static Class<?> getArrayClass(String clazz, boolean nms) {
		clazz = "[L" + (nms ? NMS : CRAFTBUKKIT) + clazz + ';';
		try {
			return Class.forName(clazz);
		} catch (ClassNotFoundException ex) {
			ex.printStackTrace();
			return null;
		}
	}

	public static Class<?> toArrayClass(Class<?> clazz) {
		try {
			return Class.forName("[L" + clazz.getName() + ';');
		} catch (ClassNotFoundException ex) {
			ex.printStackTrace();
			return null;
		}
	}

	public static final class VersionHandler<T> {
		private int version;
		private T handle;

		private VersionHandler(int version, T handle) {
			if (supports(version)) {
				this.version = version;
				this.handle = handle;
			}
		}

		public VersionHandler<T> v(int version, T handle) {
			if (version == this.version) throw new IllegalArgumentException("Cannot have duplicate version handles for version: " + version);
			if (version > this.version && supports(version)) {
				this.version = version;
				this.handle = handle;
			}
			return this;
		}

		public T orElse(T handle) {
			return this.version == 0 ? handle : this.handle;
		}
	}

	public static final class CallableVersionHandler<T> {
		private int version;
		private Callable<T> handle;

		private CallableVersionHandler(int version, Callable<T> handle) {
			if (supports(version)) {
				this.version = version;
				this.handle = handle;
			}
		}

		public CallableVersionHandler<T> v(int version, Callable<T> handle) {
			if (version == this.version) throw new IllegalArgumentException("Cannot have duplicate version handles for version: " + version);
			if (version > this.version && supports(version)) {
				this.version = version;
				this.handle = handle;
			}
			return this;
		}

		public T orElse(Callable<T> handle) {
			try {
				return (this.version == 0 ? handle : this.handle).call();
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
	}
}
