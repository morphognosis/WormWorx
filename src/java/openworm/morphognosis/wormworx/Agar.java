// For conditions of distribution and use, see copyright notice in Main.java

// Agar medium.

package openworm.morphognosis.wormworx;

import morphognosis.SectorDisplay;
import morphognosis.Utility;

import java.awt.*;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class Agar
{
   // Dimensions.
   public static Dimension SIZE = new Dimension(1000, 500);

   // Cell grid dimensions.
   public static final Dimension GRID_SIZE = new Dimension(100, 50);

   // Cell size.
   public static float CELL_WIDTH  = (float)SIZE.width / (float)GRID_SIZE.width;
   public static float CELL_HEIGHT = (float)SIZE.height / (float)GRID_SIZE.height;

   // Resize dimensions.
   public static void resize(int width, int height)
   {
      SIZE        = new Dimension(width, height);
      CELL_WIDTH  = (float)SIZE.width / (float)GRID_SIZE.width;
      CELL_HEIGHT = (float)SIZE.height / (float)GRID_SIZE.height;
   }


   // Cells.
   // See SectorDisplay.EMPTY_CELL_VALUE.
   public static final int WORM_SEGMENT_VALUE = 1;
   public int[][]          wormCells;
   public float[][]        foodCells;

   // Transforms.
   public static final float SCALE = 0.12f;
   public float              x_off, y_off;

   // Salty food.
   public static final int   RED_FOOD   = 0;
   public static final int   GREEN_FOOD = 1;
   public static final int   BLUE_FOOD  = 2;
   public static final int   NUM_FOOD   = 3;
   public double[]           foodX;
   public double[]           foodY;
   public int                currentFood;
   public static final float FOOD_CONSUMPTION_RANGE = 5.0f;

   // Constructors.
   public Agar(int foodColor)
   {
      int x, y;

      // Place food.
      float w = (float)SIZE.width;
      float h = (float)SIZE.height;

      x_off    = w * 0.775f;
      y_off    = h * 0.5f;
      foodX    = new double[NUM_FOOD];
      foodY    = new double[NUM_FOOD];
      foodX[0] = -w * 5.0f;
      foodY[0] = h * 1.5f;
      foodX[1] = -w * 6.0f;
      foodY[1] = 0.0f;
      foodX[2] = -w * 5.0f;
      foodY[2] = -h * 1.5f;
      for (int i = 0; i < NUM_FOOD; i++)
      {
         foodX[i] = (foodX[i] * SCALE) + x_off;
         foodY[i] = (foodY[i] * SCALE) + y_off;
      }

      // Create cells.
      wormCells = new int[GRID_SIZE.width][GRID_SIZE.height];
      for (x = 0; x < GRID_SIZE.width; x++)
      {
         for (y = 0; y < GRID_SIZE.height; y++)
         {
            wormCells[x][y] = SectorDisplay.EMPTY_CELL_VALUE;
         }
      }
      foodCells = new float[GRID_SIZE.width][GRID_SIZE.height];
      setFood(foodColor);
   }


   // Set food distances.
   public void setFoodDist()
   {
      int x, y;

      if (currentFood == -1)
      {
         for (x = 0; x < GRID_SIZE.width; x++)
         {
            for (y = 0; y < GRID_SIZE.height; y++)
            {
               foodCells[x][y] = -1.0f;
            }
         }
      }
      else
      {
         int sx = (int)(foodX[currentFood] / CELL_WIDTH);
         int sy = (int)(foodY[currentFood] / CELL_HEIGHT);
         for (x = 0; x < GRID_SIZE.width; x++)
         {
            for (y = 0; y < GRID_SIZE.height; y++)
            {
               foodCells[x][y] = cellDist((float)sx, (float)sy, (float)x, (float)y);
            }
         }
      }
   }


   // Cell distance.
   public float cellDist(float fromX, float fromY, float toX, float toY)
   {
      double dx  = Math.abs(toX - fromX);
      double dy  = Math.abs(toY - fromY);
      double dx2 = dx * dx;
      double dy2 = dy * dy;

      return((float)Math.sqrt(dx2 + dy2));
   }


   // Set food.
   public void setFood(int foodColor)
   {
      currentFood = foodColor;
      setFoodDist();
   }


   // Save cells.
   public void save(String filename) throws IOException
   {
      DataOutputStream output;

      try
      {
         output = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(new File(filename))));
      }
      catch (Exception e)
      {
         throw new IOException("Cannot open output file " + filename + ":" + e.getMessage());
      }
      save(output);
      output.close();
   }


   // Save cells.
   public void save(DataOutputStream output) throws IOException
   {
      for (int x = 0; x < GRID_SIZE.width; x++)
      {
         for (int y = 0; y < GRID_SIZE.height; y++)
         {
            Utility.saveInt(output, wormCells[x][y]);
         }
      }
   }


   // Load cells from file.
   public void load(String filename) throws IOException
   {
      DataInputStream input;

      try {
         input = new DataInputStream(new BufferedInputStream(new FileInputStream(new File(filename))));
      }
      catch (Exception e)
      {
         throw new IOException("Cannot open input file " + filename + ":" + e.getMessage());
      }
      load(input);
      input.close();
      setFoodDist();
   }


   // Load cells.
   public void load(DataInputStream input) throws IOException
   {
      wormCells = new int[GRID_SIZE.width][GRID_SIZE.height];
      clear();

      for (int x = 0; x < GRID_SIZE.width; x++)
      {
         for (int y = 0; y < GRID_SIZE.height; y++)
         {
            wormCells[x][y] = Utility.loadInt(input);
         }
      }
   }


   // Clear cells.
   public void clear()
   {
      int x, y;

      for (x = 0; x < GRID_SIZE.width; x++)
      {
         for (y = 0; y < GRID_SIZE.height; y++)
         {
            wormCells[x][y] = SectorDisplay.EMPTY_CELL_VALUE;
         }
      }
   }


   // Convert food color name to food color.
   public static int foodColorNameTofoodColor(String name)
   {
      if (name.equals("red"))
      {
         return(RED_FOOD);
      }
      else if (name.equals("green"))
      {
         return(GREEN_FOOD);
      }
      else if (name.equals("blue"))
      {
         return(BLUE_FOOD);
      }
      return(-1);
   }
}
