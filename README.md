# BlockChanger
1.7 - 1.18 utility class that allows you to change blocks at blazing fast speeds  

### Setup
Just import the class to your project.  
### Usage
```java
Player player = ...
BlockChanger.setBlock(player.getLocation(), Material.GOLD_BLOCK);
BlockChanger.setBlock(player.getLocation(), new ItemStack(Material.GOLD_BLOCK));
BlockChanger. // see all the methods
```  
### Tips
To make things more efficient you can do the following:  
#### Cache Objects  
All methods that use ItemStack as a parameter need to convert bukkit ItemStack to nms BlockData, this process can take a little bit of time. So to avoid this, you can cache the ItemStack and use the methods from UncheckedSetters class using .getUncheckedSetters(). This way, you can reuse the converted objects again and again. Here is a simple demonstration:  
```java
// cache the world
Object nmsWorld = BlockChanger.getWorld("world");

// cache the item stack
Object nmsBlockData = BlockChanger.getBlockData(new ItemStack(Material.DIAMOND_BLOCK));

// cache the block position if possible
Object blockPosition = BlockChanger.newBlockPosition(nmsWorld, 0, 0, 0);

// reuse the same method with the cached objects
BlockChanger.getUncheckedSetters().setBlock(nmsWorld, blockPosition, nmsBlockData, 2);
```  
With that being said, the performance difference is barely noticable due to the fact that the methods we are using to convert the objects are cached using MethodHandles. 
#### Use The Proper Method  
If you are going to place a lot of blocks all at once, use setBlocks(...). Otherwise, use setBlock. The reason for this is the same as the reason above.
