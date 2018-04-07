// For conditions of distribution and use, see copyright notice in Main.java

// Agar medium.

package openworm.morphognosis.wormworx;

import morphognosis.SectorDisplay;
import morphognosis.Utility;

import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

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
   public float[][]        saltCells;

   // Transforms.
   public static final float SCALE = 0.12f;
   public float              x_off, y_off;

   // Salty food.
   public static final int   RED_FOOD   = 0;
   public static final int   GREEN_FOOD = 1;
   public static final int   BLUE_FOOD  = 2;
   public static final int   NUM_SALTY  = 3;
   public double[]           saltyX;
   public double[]           saltyY;
   public int                currentSalty;
   public static final float SALT_CONSUMPTION_RANGE = 5.0f;

   // Constructors.
   public Agar(int foodColor)
   {
      int x, y;

      // Place food.
      float w = (float)SIZE.width;
      float h = (float)SIZE.height;

      x_off     = w * 0.775f;
      y_off     = h * 0.5f;
      saltyX    = new double[NUM_SALTY];
      saltyY    = new double[NUM_SALTY];
      saltyX[0] = -w * 5.0f;
      saltyY[0] = h * 1.5f;
      saltyX[1] = -w * 6.0f;
      saltyY[1] = 0.0f;
      saltyX[2] = -w * 5.0f;
      saltyY[2] = -h * 1.5f;
      for (int i = 0; i < NUM_SALTY; i++)
      {
         saltyX[i] = (saltyX[i] * SCALE) + x_off;
         saltyY[i] = (saltyY[i] * SCALE) + y_off;
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
      saltCells = new float[GRID_SIZE.width][GRID_SIZE.height];
      setSalty(foodColor);
   }


   // Set salt distances.
   public void setSaltDist()
   {
      int x, y;

      if (currentSalty == -1)
      {
         for (x = 0; x < GRID_SIZE.width; x++)
         {
            for (y = 0; y < GRID_SIZE.height; y++)
            {
               saltCells[x][y] = -1.0f;
            }
         }
      }
      else
      {
         int sx = (int)(saltyX[currentSalty] / CELL_WIDTH);
         int sy = (int)(saltyY[currentSalty] / CELL_HEIGHT);
         for (x = 0; x < GRID_SIZE.width; x++)
         {
            for (y = 0; y < GRID_SIZE.height; y++)
            {
               saltCells[x][y] = cellDist((float)sx, (float)sy, (float)x, (float)y);
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


   // Set salty food.
   public void setSalty(int saltyIndex)
   {
      currentSalty = saltyIndex;
      setSaltDist();
   }


   // Save cells.
   public void save(String filename) throws IOException
   {
      FileOutputStream output;

      try
      {
         output = new FileOutputStream(new File(filename));
      }
      catch (Exception e)
      {
         throw new IOException("Cannot open output file " + filename + ":" + e.getMessage());
      }
      save(output);
      output.close();
   }


   // Save cells.
   public void save(FileOutputStream output) throws IOException
   {
      int x, y;

      DataOutputStream writer = new DataOutputStream(output);

      for (x = 0; x < GRID_SIZE.width; x++)
      {
         for (y = 0; y < GRID_SIZE.height; y++)
         {
            Utility.saveInt(writer, wormCells[x][y]);
         }
      }
   }


   // Load cells from file.
   public void load(String filename) throws IOException
   {
      FileInputStream input;

      try {
         input = new FileInputStream(new File(filename));
      }
      catch (Exception e)
      {
         throw new IOException("Cannot open input file " + filename + ":" + e.getMessage());
      }
      load(input);
      input.close();
      setSaltDist();
   }


   // Load cells.
   public void load(FileInputStream input) throws IOException
   {
      int x, y;

      DataInputStream reader = new DataInputStream(input);

      wormCells = new int[GRID_SIZE.width][GRID_SIZE.height];
      clear();

      for (x = 0; x < GRID_SIZE.width; x++)
      {
         for (y = 0; y < GRID_SIZE.height; y++)
         {
            wormCells[x][y] = Utility.loadInt(reader);
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
}
