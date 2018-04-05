/*
 * Copyright (c) 2018 Tom Portegys (portegys@gmail.com). All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list
 *    of conditions and the following disclaimer in the documentation and/or other materials
 *    provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY TOM PORTEGYS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

// Main.

package openworm.morphognosis.wormworx;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.SecureRandom;
import javax.swing.UIManager;

import morphognosis.Morphognostic;
import morphognosis.Utility;

public class Main
{
   // Default random seed.
   public static final int DEFAULT_RANDOM_SEED = 4517;

   // Usage.
   public static final String Usage =
      "Usage:\n" +
      "  New run:\n" +
      "    java openworm.morphognosis.wormworx.Main\n" +
      "      -steps <steps> | -display\n" +
      "     [-agarSize <width> <height> (default=" + Agar.SIZE.width + " " + Agar.SIZE.height + ")]\n" +
      "     [-foodColor <red | green | blue> (default=red)]\n" +
      "     [-driver <metamorphDB | metamorphWekaNN | metamorphH2ONN | wormsim> (worm driver: default=wormsim)]\n" +
      "     [-wormsimSMBmuscleAmplifierOverrides <dorsal> <ventral> (defaults=1.0)]\n" +
      "     [-numNeighborhoods <quantity> (default=" + Morphognostic.DEFAULT_NUM_NEIGHBORHOODS + ")]\n" +
      "     [-neighborhoodInitialDimension <quantity> (default=" + Morphognostic.DEFAULT_NEIGHBORHOOD_INITIAL_DIMENSION + ")]\n" +
      "     [-neighborhoodDimensionStride <quantity> (default=" + Morphognostic.DEFAULT_NEIGHBORHOOD_DIMENSION_STRIDE + ")]\n" +
      "     [-neighborhoodDimensionMultiplier <quantity> (default=" + Morphognostic.DEFAULT_NEIGHBORHOOD_DIMENSION_MULTIPLIER + ")]\n" +
      "     [-epochIntervalStride <quantity> (default=" + Morphognostic.DEFAULT_EPOCH_INTERVAL_STRIDE + ")]\n" +
      "     [-epochIntervalMultiplier <quantity> (default=" + Morphognostic.DEFAULT_EPOCH_INTERVAL_MULTIPLIER + ")]\n" +
      "     [-randomSeed <random number seed> (default=" + DEFAULT_RANDOM_SEED + ")]\n" +
      "     [-save <file name>]\n" +
      "     [-saveNNdatasets]\n" +
      "  Resume run:\n" +
      "    java openworm.morphognosis.wormworx.Main\n" +
      "      -steps <steps> | -display\n" +
      "      -load <file name>\n" +
      "     [-foodColor <red | green | blue> (default=red)]\n" +
      "     [-driver <metamorphDB | metamorphNN | wormsim> (default=wormsim)]\n" +
      "     [-wormsimSMBmuscleAmplifierOverrides <dorsal> <ventral> (defaults=1.0)]\n" +
      "     [-randomSeed <random number seed>]\n" +
      "     [-save <file name>]\n" +
      "     [-saveNNdatasets]\n" +
      "Exit codes:\n" +
      "  0=success (found food)\n" +
      "  1=fail";

   // Worm.
   public Worm worm;

   // Agar and food color.
   public Agar agar;
   public int  foodColor;

   // Display.
   public Display display;

   // Random numbers.
   int          randomSeed;
   SecureRandom random;

   // Constructor.
   public Main(int foodColor, int randomSeed)
   {
      this.foodColor  = foodColor;
      this.randomSeed = randomSeed;
      random          = new SecureRandom();
      random.setSeed(randomSeed);
   }


   // Initialize.
   public void init(int NUM_NEIGHBORHOODS,
                    int NEIGHBORHOOD_INITIAL_DIMENSION,
                    int NEIGHBORHOOD_DIMENSION_STRIDE,
                    int NEIGHBORHOOD_DIMENSION_MULTIPLIER,
                    int EPOCH_INTERVAL_STRIDE,
                    int EPOCH_INTERVAL_MULTIPLIER)
   {
      // Create agar.
      agar = new Agar(foodColor);

      // Create worm.
      worm = new Worm(agar, randomSeed,
                      NUM_NEIGHBORHOODS,
                      NEIGHBORHOOD_INITIAL_DIMENSION,
                      NEIGHBORHOOD_DIMENSION_STRIDE,
                      NEIGHBORHOOD_DIMENSION_MULTIPLIER,
                      EPOCH_INTERVAL_STRIDE,
                      EPOCH_INTERVAL_MULTIPLIER);
   }


   // Terminate.
   public void terminate()
   {
      worm.terminate();
   }


   // Reset.
   public void reset()
   {
      random.setSeed(randomSeed);
      if (worm != null)
      {
         worm.reset();
      }
      if (display != null)
      {
         display.close();
      }
   }


   // Clear.
   public void clear()
   {
      if (display != null)
      {
         display.close();
         display = null;
      }
      agar = null;
      worm = null;
   }


   // Save to file.
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


   // Save.
   public void save(FileOutputStream output) throws IOException
   {
      // Save agar.
      PrintWriter writer = new PrintWriter(output);

      Utility.saveInt(writer, Agar.SIZE.width);
      Utility.saveInt(writer, Agar.SIZE.height);
      agar.save(output);

      // Save worm.
      worm.save(output);
   }


   // Load from file.
   public void load(String filename) throws IOException
   {
      FileInputStream input;

      try
      {
         input = new FileInputStream(new File(filename));
      }
      catch (Exception e)
      {
         throw new IOException("Cannot open input file " + filename + ":" + e.getMessage());
      }
      load(input);
      input.close();
   }


   // Load.
   public void load(FileInputStream input) throws IOException
   {
      // Load agar.
      DataInputStream reader = new DataInputStream(input);
      int             width  = Utility.loadInt(reader);
      int             height = Utility.loadInt(reader);

      Agar.resize(width, height);
      agar = new Agar(foodColor);
      agar.load(input);

      // Load worm.
      worm = new Worm(agar, randomSeed);
      worm.load(input);
   }


   // Run.
   public boolean run(int steps)
   {
      boolean result = false;

      random.setSeed(randomSeed);
      if (steps >= 0)
      {
         for ( ; steps > 0; steps--)
         {
            result = worm.step();
         }
      }
      else
      {
         while (updateDisplay())
         {
            if (result = worm.step())
            {
               display.stepDelay = Display.MAX_STEP_DELAY;
            }
            else
            {
               display.controls.updateStepCounter();
            }
         }
      }
      return(result);
   }


   // Create display.
   public void createDisplay()
   {
      if (display == null)
      {
         display = new Display(agar, worm);
      }
   }


   // Destroy display.
   public void destroyDisplay()
   {
      if (display != null)
      {
         display.close();
         display = null;
      }
   }


   // Update display.
   // Return false for display quit.
   public boolean updateDisplay()
   {
      if (display != null)
      {
         display.update();
         if (display.quit)
         {
            display = null;
            return(false);
         }
         else
         {
            return(true);
         }
      }
      else
      {
         return(false);
      }
   }


   // Main.
   // Exit codes:
   // 0=success
   // 1=fail
   // 2=error
   public static void main(String[] args)
   {
      // Get options.
      int     steps             = -1;
      int     foodColor         = Agar.RED_FOOD;
      int     driver            = Worm.DRIVER_TYPE.WORMSIM.getValue();
      int     randomSeed        = DEFAULT_RANDOM_SEED;
      String  loadfile          = null;
      String  savefile          = null;
      boolean saveNNdatasets    = false;
      boolean display           = false;
      boolean gotParm           = false;
      int     NUM_NEIGHBORHOODS = Morphognostic.DEFAULT_NUM_NEIGHBORHOODS;
      int     NEIGHBORHOOD_INITIAL_DIMENSION    = Morphognostic.DEFAULT_NEIGHBORHOOD_INITIAL_DIMENSION;
      int     NEIGHBORHOOD_DIMENSION_STRIDE     = Morphognostic.DEFAULT_NEIGHBORHOOD_DIMENSION_STRIDE;
      int     NEIGHBORHOOD_DIMENSION_MULTIPLIER = Morphognostic.DEFAULT_NEIGHBORHOOD_DIMENSION_MULTIPLIER;
      int     EPOCH_INTERVAL_STRIDE             = Morphognostic.DEFAULT_EPOCH_INTERVAL_STRIDE;
      int     EPOCH_INTERVAL_MULTIPLIER         = Morphognostic.DEFAULT_EPOCH_INTERVAL_MULTIPLIER;

      for (int i = 0; i < args.length; i++)
      {
         if (args[i].equals("-steps"))
         {
            i++;
            if (i >= args.length)
            {
               System.err.println("Invalid steps option");
               System.err.println(Usage);
               System.exit(1);
            }
            try
            {
               steps = Integer.parseInt(args[i]);
            }
            catch (NumberFormatException e) {
               System.err.println("Invalid steps option");
               System.err.println(Usage);
               System.exit(1);
            }
            if (steps < 0)
            {
               System.err.println("Invalid steps option");
               System.err.println(Usage);
               System.exit(1);
            }
            continue;
         }
         if (args[i].equals("-display"))
         {
            display = true;
            continue;
         }
         if (args[i].equals("-agarSize"))
         {
            if (args.length > (i + 2))
            {
               try
               {
                  int width  = Integer.parseInt(args[i + 1]);
                  int height = Integer.parseInt(args[i + 2]);
                  if ((width <= 0) || (height <= 0))
                  {
                     System.err.println("Invalid agar size option");
                     System.err.println(Usage);
                     System.exit(1);
                  }
                  Agar.resize(width, height);
               }
               catch (NumberFormatException e) {
                  System.err.println("Invalid agar size option");
                  System.err.println(Usage);
                  System.exit(1);
               }
               i      += 2;
               gotParm = true;
            }
            else
            {
               System.err.println("Invalid agar size option");
               System.err.println(Usage);
               System.exit(1);
            }
            continue;
         }
         if (args[i].equals("-wormsimSMBmuscleAmplifierOverrides"))
         {
            if (args.length > (i + 2))
            {
               try
               {
                  Worm.DORSAL_SMB_MUSCLE_AMPLIFIER_OVERRIDE  = Double.parseDouble(args[i + 1]);
                  Worm.VENTRAL_SMB_MUSCLE_AMPLIFIER_OVERRIDE = Double.parseDouble(args[i + 2]);
               }
               catch (NumberFormatException e) {
                  System.err.println("Invalid SMB muscle amplifier override option");
                  System.err.println(Usage);
                  System.exit(1);
               }
               i      += 2;
               gotParm = true;
            }
            else
            {
               System.err.println("Invalid SMB muscle amplifier override option");
               System.err.println(Usage);
               System.exit(1);
            }
            continue;
         }
         if (args[i].equals("-foodColor"))
         {
            i++;
            if (i >= args.length)
            {
               System.err.println("Invalid foodColor option");
               System.err.println(Usage);
               System.exit(1);
            }
            if (args[i].equals("red"))
            {
               foodColor = Agar.RED_FOOD;
            }
            else if (args[i].equals("green"))
            {
               foodColor = Agar.GREEN_FOOD;
            }
            else if (args[i].equals("blue"))
            {
               foodColor = Agar.BLUE_FOOD;
            }
            else
            {
               System.err.println("Invalid foodColor option");
               System.err.println(Usage);
               System.exit(1);
            }
            continue;
         }
         if (args[i].equals("-driver"))
         {
            i++;
            if (i >= args.length)
            {
               System.err.println("Invalid driver option");
               System.err.println(Usage);
               System.exit(1);
            }
            if (args[i].equals("metamorphDB"))
            {
               driver = Worm.DRIVER_TYPE.METAMORPH_DB.getValue();
            }
            else if (args[i].equals("metamorphWekaNN"))
            {
               driver = Worm.DRIVER_TYPE.METAMORPH_WEKA_NN.getValue();
            }
            else if (args[i].equals("metamorphH2ONN"))
            {
               driver = Worm.DRIVER_TYPE.METAMORPH_H2O_NN.getValue();
            }
            else if (args[i].equals("wormsim"))
            {
               driver = Worm.DRIVER_TYPE.WORMSIM.getValue();
            }
            else
            {
               System.err.println("Invalid driver option");
               System.err.println(Usage);
               System.exit(1);
            }
            continue;
         }
         if (args[i].equals("-numNeighborhoods"))
         {
            i++;
            if (i >= args.length)
            {
               System.err.println("Invalid numNeighborhoods option");
               System.err.println(Usage);
               System.exit(1);
            }
            try
            {
               NUM_NEIGHBORHOODS = Integer.parseInt(args[i]);
            }
            catch (NumberFormatException e) {
               System.err.println("Invalid numNeighborhoods option");
               System.err.println(Usage);
               System.exit(1);
            }
            if (NUM_NEIGHBORHOODS < 0)
            {
               System.err.println("Invalid numNeighborhoods option");
               System.err.println(Usage);
               System.exit(1);
            }
            gotParm = true;
            continue;
         }
         if (args[i].equals("-neighborhoodInitialDimension"))
         {
            i++;
            if (i >= args.length)
            {
               System.err.println("Invalid neighborhoodInitialDimension option");
               System.err.println(Usage);
               System.exit(1);
            }
            try
            {
               NEIGHBORHOOD_INITIAL_DIMENSION = Integer.parseInt(args[i]);
            }
            catch (NumberFormatException e) {
               System.err.println("Invalid neighborhoodInitialDimension option");
               System.err.println(Usage);
               System.exit(1);
            }
            if ((NEIGHBORHOOD_INITIAL_DIMENSION < 3) ||
                ((NEIGHBORHOOD_INITIAL_DIMENSION % 2) == 0))
            {
               System.err.println("Invalid neighborhoodInitialDimension option");
               System.err.println(Usage);
               System.exit(1);
            }
            gotParm = true;
            continue;
         }
         if (args[i].equals("-neighborhoodDimensionStride"))
         {
            i++;
            if (i >= args.length)
            {
               System.err.println("Invalid neighborhoodDimensionStride option");
               System.err.println(Usage);
               System.exit(1);
            }
            try
            {
               NEIGHBORHOOD_DIMENSION_STRIDE = Integer.parseInt(args[i]);
            }
            catch (NumberFormatException e) {
               System.err.println("Invalid neighborhoodDimensionStride option");
               System.err.println(Usage);
               System.exit(1);
            }
            if (NEIGHBORHOOD_DIMENSION_STRIDE < 0)
            {
               System.err.println("Invalid neighborhoodDimensionStride option");
               System.err.println(Usage);
               System.exit(1);
            }
            gotParm = true;
            continue;
         }
         if (args[i].equals("-neighborhoodDimensionMultiplier"))
         {
            i++;
            if (i >= args.length)
            {
               System.err.println("Invalid neighborhoodDimensionMultiplier option");
               System.err.println(Usage);
               System.exit(1);
            }
            try
            {
               NEIGHBORHOOD_DIMENSION_MULTIPLIER = Integer.parseInt(args[i]);
            }
            catch (NumberFormatException e) {
               System.err.println("Invalid neighborhoodDimensionMultiplier option");
               System.err.println(Usage);
               System.exit(1);
            }
            if (NEIGHBORHOOD_DIMENSION_MULTIPLIER < 0)
            {
               System.err.println("Invalid neighborhoodDimensionMultiplier option");
               System.err.println(Usage);
               System.exit(1);
            }
            gotParm = true;
            continue;
         }
         if (args[i].equals("-epochIntervalStride"))
         {
            i++;
            if (i >= args.length)
            {
               System.err.println("Invalid epochIntervalStride option");
               System.err.println(Usage);
               System.exit(1);
            }
            try
            {
               EPOCH_INTERVAL_STRIDE = Integer.parseInt(args[i]);
            }
            catch (NumberFormatException e) {
               System.err.println("Invalid epochIntervalStride option");
               System.err.println(Usage);
               System.exit(1);
            }
            if (EPOCH_INTERVAL_STRIDE < 0)
            {
               System.err.println("Invalid epochIntervalStride option");
               System.err.println(Usage);
               System.exit(1);
            }
            gotParm = true;
            continue;
         }
         if (args[i].equals("-epochIntervalMultiplier"))
         {
            i++;
            if (i >= args.length)
            {
               System.err.println("Invalid epochIntervalMultiplier option");
               System.err.println(Usage);
               System.exit(1);
            }
            try
            {
               EPOCH_INTERVAL_MULTIPLIER = Integer.parseInt(args[i]);
            }
            catch (NumberFormatException e) {
               System.err.println("Invalid epochIntervalMultiplier option");
               System.err.println(Usage);
               System.exit(1);
            }
            if (EPOCH_INTERVAL_MULTIPLIER < 0)
            {
               System.err.println("Invalid epochIntervalMultiplier option");
               System.err.println(Usage);
               System.exit(1);
            }
            gotParm = true;
            continue;
         }
         if (args[i].equals("-randomSeed"))
         {
            i++;
            if (i >= args.length)
            {
               System.err.println("Invalid randomSeed option");
               System.err.println(Usage);
               System.exit(1);
            }
            try
            {
               randomSeed = Integer.parseInt(args[i]);
            }
            catch (NumberFormatException e) {
               System.err.println("Invalid randomSeed option");
               System.err.println(Usage);
               System.exit(1);
            }
            continue;
         }
         if (args[i].equals("-load"))
         {
            i++;
            if (i >= args.length)
            {
               System.err.println("Invalid load option");
               System.err.println(Usage);
               System.exit(1);
            }
            if (loadfile == null)
            {
               loadfile = args[i];
            }
            else
            {
               System.err.println("Duplicate load option");
               System.err.println(Usage);
               System.exit(1);
            }
            continue;
         }
         if (args[i].equals("-save"))
         {
            i++;
            if (i >= args.length)
            {
               System.err.println("Invalid save option");
               System.err.println(Usage);
               System.exit(1);
            }
            if (savefile == null)
            {
               savefile = args[i];
            }
            else
            {
               System.err.println("Duplicate save option");
               System.err.println(Usage);
               System.exit(1);
            }
            continue;
         }
         if (args[i].equals("-saveNNdatasets"))
         {
            saveNNdatasets = true;
            continue;
         }
         System.err.println(Usage);
         System.exit(1);
      }

      // Check options.
      if (((steps < 0) && !display) || ((steps >= 0) && display))
      {
         System.err.println(Usage);
         System.exit(1);
      }
      if ((loadfile != null) && gotParm)
      {
         System.err.println(Usage);
         System.exit(1);
      }

      // Set look and feel.
      try {
         UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      }
      catch (Exception e)
      {
         System.err.println("Warning: cannot set look and feel");
      }

      // Create world.
      Main main = new Main(foodColor, randomSeed);
      if (loadfile != null)
      {
         try
         {
            main.load(loadfile);
         }
         catch (Exception e)
         {
            System.err.println("Cannot load from file " + loadfile + ": " + e.getMessage());
            System.exit(1);
         }
      }
      else
      {
         try
         {
            main.init(NUM_NEIGHBORHOODS,
                      NEIGHBORHOOD_INITIAL_DIMENSION,
                      NEIGHBORHOOD_DIMENSION_STRIDE,
                      NEIGHBORHOOD_DIMENSION_MULTIPLIER,
                      EPOCH_INTERVAL_STRIDE,
                      EPOCH_INTERVAL_MULTIPLIER);
         }
         catch (Exception e)
         {
            System.err.println("Cannot initialize: " + e.getMessage());
            System.exit(1);
         }
      }

      // Create display?
      if (display)
      {
         main.createDisplay();
      }

      // Set worm driver.
      main.worm.driver = driver;
      if (driver == Worm.DRIVER_TYPE.METAMORPH_WEKA_NN.getValue())
      {
         try
         {
            System.out.println("Training head metamorph Weka NN...");
            main.worm.createHeadMetamorphWekaNN();
         }
         catch (Exception e)
         {
            System.err.println("Cannot train head metamorph Weka NN: " + e.getMessage());
         }
         try
         {
            System.out.println("Training body metamorph Weka NN...");
            main.worm.createBodyMetamorphWekaNN();
         }
         catch (Exception e)
         {
            System.err.println("Cannot train body metamorph Weka NN: " + e.getMessage());
         }
      }

      // Run.
      boolean result = main.run(steps);

      // Save?
      if (savefile != null)
      {
         try
         {
            main.save(savefile);
         }
         catch (Exception e)
         {
            System.err.println("Cannot save to file " + savefile + ": " + e.getMessage());
         }
      }

      // Save neural network datasets?
      if (saveNNdatasets)
      {
         try
         {
            main.worm.saveHeadMetamorphNNtrainingData();
         }
         catch (Exception e)
         {
            System.err.println("Cannot save head neural network dataset: " + e.getMessage());
         }
         try
         {
            main.worm.saveBodyMetamorphNNtrainingData();
         }
         catch (Exception e)
         {
            System.err.println("Cannot save body neural network dataset: " + e.getMessage());
         }
      }

      // Terminate worm.
      main.terminate();

      if (result)
      {
         System.exit(0);
      }
      else
      {
         System.exit(1);
      }
   }
}
