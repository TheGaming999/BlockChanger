# BlockChanger
1.7 - 1.18 utility class that allows you to change blocks at blazing fast speeds  

### Setup
Just import the class to your project.  
### Usage
```java
Player player = ...
BlockChanger.setBlock(player.getLocation(), Material.GOLD_BLOCK);
BlockChanger.setBlock(player.getLocation(), new ItemStack(Material.GOLD_BLOCK));
// Use async methods
BlockChanger.setCuboidAsynchronously(// corner no.1, // corner no.2, itemStack, false).thenRun(() -> {
	// gets executed after cuboid has been filled entirely
	Bukkit.broadcastMessage("COMPLETED!");
});
BlockChanger. // see all the methods
```  
### ToDo  
- Method to update lighting?  
