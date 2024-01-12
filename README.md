# BlockChanger
<div align="center">

  <a href="">[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2FTheGaming999%2FBlockChanger.svg?type=shield)](https://app.fossa.com/projects/git%2Bgithub.com%2FTheGaming999%2FBlockChanger?ref=badge_shield)</a>
  <a href=""> [![Codacy Badge](https://app.codacy.com/project/badge/Grade/a1ecc2905ea14fd39b29a5a3df2d124c)](https://www.codacy.com/gh/TheGaming999/BlockChanger/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=TheGaming999/BlockChanger&amp;utm_campaign=Badge_Grade)</a>

</div>
1.7 - 1.20.4 single class that allows you to change blocks at blazing fast speeds.  
  
Please refer to this thread for more information: (https://www.spigotmc.org/threads/methods-for-changing-massive-amount-of-blocks-up-to-14m-blocks-s.395868/)  

### Setup
Just import the class to your project.  
### Usage
```java
// player location as an example.
Player player = ...
BlockChanger.setBlock(player.getLocation(), Material.GOLD_BLOCK);
BlockChanger.setBlock(player.getLocation(), new ItemStack(Material.GOLD_BLOCK));

// Usage of async methods is the same as the regular methods.
// Type 1: Set block by NMS World method (nmsWorld#setTypeAndData) (80-90K blocks per second)
BlockChanger.setBlockAsynchronously(player.getLocation(), new ItemStack(Material.STONE), false);
// Type 2: Set block by NMS Chunk method (nmsChunk#setType) (2.19M blocks per second)
BlockChanger.setChunkBlockAsynchronously(player.getLocation(), new ItemStack(Material.DIRT), false);
// Type 3: Set block by NMS ChunkSection method (nmsChunkSection#setType) (7.9M blocks per second)
BlockChanger.setSectionBlockAsynchronously(player.getLocation(), new ItemStack(Material.GOLD_ORE), false);

// cornerA and cornerB are locations.
// This method returns a CompletableFuture<Void>, which gets completed once all blocks are set.
BlockChanger.setCuboidAsynchronously(cornerA, cornerB, new ItemStack(Material.DIAMOND_BLOCK), false)
.thenRun(() -> {
	// gets executed after cuboid has been filled entirely
	Bukkit.broadcastMessage("COMPLETED!");
});

// Fastest method (setSection).
BlockChanger.setSectionCuboidAsynchronously(corner1, corner2, new ItemStack(Material.GLASS), false)
.thenRunAsync(() -> {
	Bukkit.broadcastMessage("COMPLETED!");
});

BlockChanger. // see all the methods
```  
### ToDo  
- Method to update lighting?  



## License
[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2FTheGaming999%2FBlockChanger.svg?type=large)](https://app.fossa.com/projects/git%2Bgithub.com%2FTheGaming999%2FBlockChanger?ref=badge_large)
