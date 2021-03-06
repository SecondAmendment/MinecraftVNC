package com.caetsamuel.gmail;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;

import org.inventivetalent.mapmanager.MapManagerPlugin;
import org.inventivetalent.mapmanager.controller.MapController;
import org.inventivetalent.mapmanager.controller.MultiMapController;
import org.inventivetalent.mapmanager.manager.MapManager;
import org.inventivetalent.mapmanager.wrapper.MapWrapper;

import java.awt.*;
import java.awt.image.BufferedImage;

public class VNCScreen implements Runnable {

    private MapManager mapManager = ((MapManagerPlugin) Bukkit.getPluginManager().getPlugin("MapManager")).getMapManager();

    private Thread thread;

    private boolean render = false;
    private boolean running = false;

    private int width = 1920;
    private int height = 1080;

    private int rows = (int)Math.ceil(height/128.0);
    private int columns = (int)Math.ceil(width/128.0);

    private ItemFrame[][] frames = new ItemFrame[rows][columns];
    private int[][] itemFrameIds = new int[columns][rows];

    private final double FRAME_CAP = 1.0/1.0;//1 FPS

    private final MapWrapper mapWrapper;

    private final MultiMapController mapController;

    private final Player viewer;

    private Robot robot;

    private World world;

    public VNCScreen(Location f1, Location f2, BlockFace bf) { //Need to add the source stream as an argument
        try {
            robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }

        world = f1.getWorld();

        addFrames(f1, f2, bf);
        //Set a starter image (to generate a map to put in the itemframe (you will be updating this image later using mapController.update(image)))
        BufferedImage image = scaleImage(robot.createScreenCapture(new Rectangle(1536, 864)));

        viewer = Bukkit.getPlayer("SecondAmendment");

        mapWrapper = mapManager.wrapMultiImage(image, rows, columns);
        mapController = (MultiMapController) mapWrapper.getController();

        mapController.addViewer(viewer);
        mapController.sendContent(viewer);

        setMaps(mapController);

        mapController.showInFrames(viewer, itemFrameIds);

        Bukkit.getScheduler().scheduleSyncRepeatingTask(VNCraft.getInstance(), new Runnable() {
            public void run() {
                mapController.update(scaleImage(robot.createScreenCapture(new Rectangle(1536, 864))));
            }
        }, 0L, 20L);

        //start();
    }

    public void start() {
        thread = new Thread(this);
        thread.run();
    }

    public void stop(){
        running = false;
    }

    public void run(){
        running = true;

        double initTime = 0;
        double lastTime = System.nanoTime() / 1000000000.0;
        double elapsedTime = 0;
        double unprocessedTime = 0;

        while(running){
            render = false;

            initTime = System.nanoTime() / 1000000000.0;
            elapsedTime = initTime - lastTime;
            lastTime = initTime;

            unprocessedTime += elapsedTime;

            while(unprocessedTime >= FRAME_CAP)
            {
                unprocessedTime -= FRAME_CAP;
                render = true;
            }

            if(render){
                //Render Screen Update
                mapController.update(scaleImage(robot.createScreenCapture(new Rectangle(1536, 864))));
            }
            else{
                try{
                    Thread.sleep(1);
                }
                catch(InterruptedException e){
                    e.printStackTrace();
                }
            }
        }
    }

    public Entity getEntityByLocation(Location loc){
        for(Entity e : loc.getWorld().getEntities()){
            if(e.getLocation().distanceSquared(loc) <= 1.0){
                return e;
            }
        }
        return null;
    }

    public void addFrames(Location f1, Location f2, BlockFace bf){

        int minX = Math.min(f1.getBlockX(), f2.getBlockX());
        int maxX = Math.max(f1.getBlockX(), f2.getBlockX());
        int minY = Math.min(f1.getBlockY(), f2.getBlockY());
        int maxY = Math.max(f1.getBlockY(), f2.getBlockY());
        int minZ = Math.min(f1.getBlockZ(), f2.getBlockZ());
        int maxZ = Math.max(f1.getBlockZ(), f2.getBlockZ());

        Location loc = new Location(world,0, 0, 0);

        int row = 0;
        int column = 0;

        for (int x = minX; x <= maxX; x++) {
            for (int y = maxY; y >= minY; y--) {
                for (int z = minZ; z <= maxZ; z++) {
                    loc.setX(x);
                    loc.setY(y);
                    loc.setZ(z);

                    ItemFrame frame;

                    world.getBlockAt(loc.getBlock().getRelative(bf).getLocation()).setType(Material.AIR);

                    frame = world.spawn(loc.getBlock().getRelative(bf).getLocation(), ItemFrame.class);

                    frames[row][column] = frame;
                    itemFrameIds[column][row] = frame.getEntityId();

                    if(row < rows - 1){
                        row++;
                    }
                    else if(column < columns - 1){
                        row = 0;
                        column++;
                    }
                }
            }
        }
    }

    public void setMaps(MapController mapController){
        try {
            for (int c = 0; c < columns; c++) {
                for (int r = 0; r < rows; r++) {
                    System.out.println("Rows: " + r + "Columns: " + c);
                    ItemStack map = new ItemStack(Material.MAP);
                    map.setDurability(mapController.getMapId(Bukkit.getOfflinePlayer(Bukkit.getServer().getPlayer("SecondAmendment").getUniqueId())));
                    frames[r][c].setItem(map);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private BufferedImage scaleImage(BufferedImage original) {
        if (original.getWidth() % 128 == 0 && original.getHeight() % 128 == 0) {
            return original;
        }
        int type = original.getType();
        System.out.println("TYPE IS: " + type);
        if (type == 0) {
            type = 5; }

        BufferedImage scaledImage = new BufferedImage(128 * this.columns, 128 * this.rows, type);
        Graphics scaledGraphics = scaledImage.getGraphics();

        Image instance = original.getScaledInstance(128 * this.columns, 128 * this.rows, Image.SCALE_FAST);
        scaledGraphics.drawImage(instance, 0, 0, null);

        instance.flush();
        scaledGraphics.dispose();
        return scaledImage;
    }
}
