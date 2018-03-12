// For conditions of distribution and use, see copyright notice in Main.java

// C. elegans worm: a morphognosis organism.

package openworm.morphognosis.wormworx;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.io.*;
import java.security.SecureRandom;
import java.util.ArrayList;

import hex.genmodel.tools.WormWorxPredict;
import morphognosis.Metamorph;
import morphognosis.Morphognostic;
import morphognosis.Orientation;
import morphognosis.SectorDisplay;
import morphognosis.Utility;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.core.Attribute;
import weka.core.Debug;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.core.converters.ArffSaver;

public class Worm
{
   // Agar.
   public Agar agar;

   // Sensors.
   // 0-21: neighboring worm segment relative coordinates.
   // 22-25: relative salt distances.
   public static final int NUM_SENSORS = 26;

   // Responses.
   public static final int MOVE_NW       = 0;
   public static final int MOVE_NORTH    = 1;
   public static final int MOVE_NE       = 2;
   public static final int MOVE_WEST     = 3;
   public static final int STAY          = 4;
   public static final int MOVE_EAST     = 5;
   public static final int MOVE_SW       = 6;
   public static final int MOVE_SOUTH    = 7;
   public static final int MOVE_SE       = 8;
   public static final int NUM_RESPONSES = 9;

   // Event time.
   public int eventTime;

   // Driver.
   public enum DRIVER_TYPE
   {
      METAMORPH_DB(0),
      METAMORPH_WEKA_NN(1),
      METAMORPH_H2O_NN(2),
      WORMSIM(3);

      private int value;

      DRIVER_TYPE(int value)
      {
         this.value = value;
      }

      public int getValue()
      {
         return(value);
      }
   }
   int driver;

   // SMB muscle amplfier overrides.
   public static double DORSAL_SMB_MUSCLE_AMPLIFIER_OVERRIDE  = -1.0;
   public static double VENTRAL_SMB_MUSCLE_AMPLIFIER_OVERRIDE = -1.0;

   // H2O morphognostic classification.
   public WormWorxPredict H2Opredict;
   public                 String[] H2OresponseLabels;

   // Found food?
   public boolean foundFood;

   // Simulator synchronization.
   public Object wormsimLock;

   // Neural network dataset save file name.
   public static final String NN_DATASET_SAVE_FILE_NAME = "metamorphs.csv";

   // Worm segment.
   public class Segment
   {
      // Number.
      public int number;

      // Number of superpositions.
      // Must be > 0 and <= NUM_RESPONSES.
      public static final int NUM_SUPERPOSITIONS = 1;

      // Position.
      public int[] x;
      public int[] y;
      public int   nx, ny;
      public int   rx, ry;

      // Sensors.
      public float[] sensors;

      // Responses.
      public int[] responses;

      // Current morphognostic.
      public Morphognostic morphognostic;

      // Navigation.
      public boolean[][] landmarkMap;
      public int         maxEventAge;
      public class Event
      {
         public int[] values;
         public int   x;
         public int   y;
         public int   time;
         public Event(int[] values, int x, int y, int time)
         {
            int n = values.length;

            this.values = new int[n];
            for (int i = 0; i < n; i++)
            {
               this.values[i] = values[i];
            }
            this.x    = x;
            this.y    = y;
            this.time = time;
         }


         public Event clone()
         {
            int[] v = new int[values.length];
            for (int i = 0; i < v.length; i++)
            {
               v[i] = values[i];
            }
            return(new Event(v, x, y, time));
         }
      }
      public ArrayList<Event> events;

      // Constructors.
      public Segment(int number)
      {
         init(number);
         int [] numEventTypes = new int[NUM_SENSORS];
         for (int i = 0; i < 22; i++)
         {
            numEventTypes[i] = 1;
         }
         for (int i = 22; i < NUM_SENSORS; i++)
         {
            numEventTypes[i] = 3;
         }
         morphognostic = new Morphognostic(Orientation.WEST, numEventTypes);
         Morphognostic.Neighborhood n = morphognostic.neighborhoods.get(morphognostic.NUM_NEIGHBORHOODS - 1);
         maxEventAge = n.epoch + n.duration - 1;
      }


      public Segment(int number,
                     int NUM_NEIGHBORHOODS,
                     int NEIGHBORHOOD_INITIAL_DIMENSION,
                     int NEIGHBORHOOD_DIMENSION_STRIDE,
                     int NEIGHBORHOOD_DIMENSION_MULTIPLIER,
                     int EPOCH_INTERVAL_STRIDE,
                     int EPOCH_INTERVAL_MULTIPLIER)
      {
         init(number);
         int [] numEventTypes = new int[NUM_SENSORS];
         for (int i = 0; i < 22; i++)
         {
            numEventTypes[i] = 1;
         }
         for (int i = 22; i < NUM_SENSORS; i++)
         {
            numEventTypes[i] = 3;
         }
         morphognostic = new Morphognostic(Orientation.WEST, numEventTypes,
                                           NUM_NEIGHBORHOODS,
                                           NEIGHBORHOOD_INITIAL_DIMENSION,
                                           NEIGHBORHOOD_DIMENSION_STRIDE,
                                           NEIGHBORHOOD_DIMENSION_MULTIPLIER,
                                           EPOCH_INTERVAL_STRIDE,
                                           EPOCH_INTERVAL_MULTIPLIER);
         Morphognostic.Neighborhood n = morphognostic.neighborhoods.get(morphognostic.NUM_NEIGHBORHOODS - 1);
         maxEventAge = n.epoch + n.duration - 1;
      }


      void init(int number)
      {
         this.number = number;

         x = new int[NUM_SUPERPOSITIONS];
         y = new int[NUM_SUPERPOSITIONS];
         for (int i = 0; i < NUM_SUPERPOSITIONS; i++)
         {
            x[i] = segmentSimPositions[number].x;
            y[i] = segmentSimPositions[number].y;
         }
         rx      = segmentSimPositions[number].x;
         ry      = segmentSimPositions[number].y;
         sensors = new float[NUM_SENSORS];
         for (int i = 0; i < NUM_SENSORS; i++)
         {
            sensors[i] = 0.0f;
         }
         responses = new int[NUM_RESPONSES];
         for (int i = 0; i < NUM_RESPONSES; i++)
         {
            responses[i] = STAY;
         }
         landmarkMap = new boolean[Agar.GRID_SIZE.width][Agar.GRID_SIZE.height];
         for (int i = 0; i < Agar.GRID_SIZE.width; i++)
         {
            for (int j = 0; j < Agar.GRID_SIZE.height; j++)
            {
               landmarkMap[i][j] = false;
            }
         }
         events = new ArrayList<Event>();
      }


      public void reset()
      {
         for (int i = 0; i < NUM_SUPERPOSITIONS; i++)
         {
            x[i] = rx;
            y[i] = ry;
         }
         for (int i = 0; i < NUM_SENSORS; i++)
         {
            sensors[i] = 0.0f;
         }
         for (int i = 0; i < NUM_SUPERPOSITIONS; i++)
         {
            responses[i] = STAY;
         }
         for (int i = 0; i < Agar.GRID_SIZE.width; i++)
         {
            for (int j = 0; j < Agar.GRID_SIZE.height; j++)
            {
               landmarkMap[i][j] = false;
            }
         }
         events.clear();
         morphognostic.clear();
      }


      // Sensor/response cycle.
      public void cycle(float[] sensors)
      {
         // Determine best superposition given sensor data.
         int[] values = new int[NUM_SENSORS];
         for (int i = 0; i < NUM_SENSORS; i++)
         {
            this.sensors[i] = sensors[i];
            values[i]       = (int)sensors[i];
         }
         nx = x[0];
         ny = y[0];
         int w = Agar.GRID_SIZE.width;
         int h = Agar.GRID_SIZE.height;
         int a = maxEventAge + 1;
         if (driver == DRIVER_TYPE.WORMSIM.getValue())
         {
            int response = wormsimResponse(this);
            for (int i = 0; i < NUM_RESPONSES; i++)
            {
               responses[i] = response;
            }
         }
         else
         {
            for (int i = 0; i < NUM_RESPONSES; i++)
            {
               responses[i] = STAY;
            }
            float[] responseValues = new float[NUM_RESPONSES];
            float dist = -1.0f;
            for (int i = 0; i < NUM_SUPERPOSITIONS; i++)
            {
               ArrayList<Event> evts = new ArrayList<Event>();
               for (Event e : events)
               {
                  evts.add(e.clone());
               }
               evts.add(new Event(values, x[i], y[i], eventTime));
               if ((eventTime - evts.get(0).time) > maxEventAge)
               {
                  evts.remove(0);
               }
               int morphEvents[][][][] = new int[w][h][NUM_SENSORS][a];
               for (int x2 = 0; x2 < w; x2++)
               {
                  for (int y2 = 0; y2 < h; y2++)
                  {
                     for (int n = 0; n < NUM_SENSORS; n++)
                     {
                        for (int t = 0; t < a; t++)
                        {
                           morphEvents[x2][y2][n][t] = -1;
                        }
                     }
                  }
               }
               for (Event e : evts)
               {
                  for (int n = 0; n < NUM_SENSORS; n++)
                  {
                     morphEvents[e.x][e.y][n][eventTime - e.time] = e.values[n];
                  }
               }
               Morphognostic m = morphognostic.clone();
               m.update(morphEvents, x[i], y[i]);

               // Evaluate against metamorphs.
               if (driver == DRIVER_TYPE.METAMORPH_DB.getValue())
               {
                  float[] responseDistances = metamorphDBresponse(m);
                  float d = -1.0f;
                  for (int j = 0; j < NUM_RESPONSES; j++)
                  {
                     if (responseDistances[j] != -1.0f)
                     {
                        if ((d == -1.0f) || (responseDistances[j] < d))
                        {
                           d = responseDistances[j];
                        }
                     }
                  }
                  if (d != -1.0f)
                  {
                     if ((dist == -1.0f) || (d < dist))
                     {
                        nx   = x[i];
                        ny   = y[i];
                        dist = d;
                        for (int j = 0; j < NUM_RESPONSES; j++)
                        {
                           responseValues[j] = responseDistances[j];
                        }
                     }
                  }
               }
               else if ((driver == DRIVER_TYPE.METAMORPH_WEKA_NN.getValue()) ||
                        (driver == DRIVER_TYPE.METAMORPH_H2O_NN.getValue()))
               {
                  float[] responseProbabilities;
                  if (driver == DRIVER_TYPE.METAMORPH_WEKA_NN.getValue())
                  {
                     responseProbabilities = metamorphWekaNNresponse(m);
                  }
                  else
                  {
                     responseProbabilities = metamorphH2ONNresponse(m);
                  }
                  float d = -1.0f;
                  for (int j = 0; j < NUM_RESPONSES; j++)
                  {
                     if ((d == -1.0f) || (responseProbabilities[j] > d))
                     {
                        d = responseProbabilities[j];
                     }
                  }
                  if (d != -1.0f)
                  {
                     if ((dist == -1.0f) || (d > dist))
                     {
                        nx   = x[i];
                        ny   = y[i];
                        dist = d;
                        for (int j = 0; j < NUM_RESPONSES; j++)
                        {
                           responseValues[j] = responseProbabilities[j];
                        }
                     }
                  }
               }
            }
            if (dist != -1.0f)
            {
               if (driver == DRIVER_TYPE.METAMORPH_DB.getValue())
               {
                  for (int i = 0; i < NUM_SUPERPOSITIONS; i++)
                  {
                     float d = -1.0f;
                     int   k = random.nextInt(NUM_RESPONSES);
                     int   q = -1;
                     for (int j = 0; j < NUM_RESPONSES; j++)
                     {
                        if (responseValues[k] != -1.0f)
                        {
                           if ((d == -1.0f) || (responseValues[k] < d))
                           {
                              if (q != -1)
                              {
                                 responseValues[q] = d;
                              }
                              q = k;
                              d = responseValues[k];
                              responseValues[k] = -1.0f;
                              responses[i]      = k;
                           }
                        }
                        k = (k + 1) % NUM_RESPONSES;
                     }
                  }
               }
               else
               {
                  for (int i = 0; i < NUM_SUPERPOSITIONS; i++)
                  {
                     float d = -1.0f;
                     int   k = random.nextInt(NUM_RESPONSES);
                     int   q = -1;
                     for (int j = 0; j < NUM_RESPONSES; j++)
                     {
                        if (responseValues[k] != -1.0f)
                        {
                           if ((d == -1.0f) || (responseValues[k] > d))
                           {
                              if (q != -1)
                              {
                                 responseValues[q] = d;
                              }
                              q = k;
                              d = responseValues[k];
                              responseValues[k] = -1.0f;
                              responses[i]      = k;
                           }
                        }
                        k = (k + 1) % NUM_RESPONSES;
                     }
                  }
               }
            }
         }

         // Update landmarks.
         landmarkMap[nx][ny] = true;

         // Update morphognostic.
         events.add(new Event(values, nx, ny, eventTime));
         if ((eventTime - events.get(0).time) > maxEventAge)
         {
            events.remove(0);
         }
         int morphEvents[][][][] = new int[w][h][NUM_SENSORS][a];
         for (int x2 = 0; x2 < w; x2++)
         {
            for (int y2 = 0; y2 < h; y2++)
            {
               for (int n = 0; n < NUM_SENSORS; n++)
               {
                  for (int t = 0; t < a; t++)
                  {
                     morphEvents[x2][y2][n][t] = -1;
                  }
               }
            }
         }
         for (Event e : events)
         {
            for (int n = 0; n < NUM_SENSORS; n++)
            {
               morphEvents[e.x][e.y][n][eventTime - e.time] = e.values[n];
            }
         }
         morphognostic.update(morphEvents, nx, ny);

         // Update metamorphs.
         Metamorph metamorph = new Metamorph(morphognostic.clone(), responses[0], getResponseName(responses[0]));
         boolean   found     = false;
         boolean   dup       = false;
         for (Metamorph m : metamorphs)
         {
            if (m.morphognostic.compare(metamorph.morphognostic) == 0.0f)
            {
               if (m.response == metamorph.response)
               {
                  found = true;
                  break;
               }
               else
               {
                  dup = true;
               }
               break;
            }
         }
         if (!found)
         {
            if (dup)
            {
               System.out.println("Warning: metamorph with identical existing morphognostic and different response added");
            }
            metamorphs.add(metamorph);
         }
      }


      // Collapse superpositions.
      public void collapsePosition()
      {
         x[0] = nx;
         y[0] = ny;
      }


      public void save(FileOutputStream output) throws IOException
      {
         PrintWriter writer = new PrintWriter(new OutputStreamWriter(output));

         for (int i = 0; i < NUM_SUPERPOSITIONS; i++)
         {
            Utility.saveInt(writer, x[i]);
            Utility.saveInt(writer, y[i]);
         }
         Utility.saveInt(writer, rx);
         Utility.saveInt(writer, ry);
         Utility.saveInt(writer, maxEventAge);
         morphognostic.save(output);
         writer.flush();
      }


      public void load(FileInputStream input) throws IOException
      {
         DataInputStream reader = new DataInputStream(input);

         for (int i = 0; i < NUM_SUPERPOSITIONS; i++)
         {
            x[i] = Utility.loadInt(reader);
            y[i] = Utility.loadInt(reader);
         }
         rx            = Utility.loadInt(reader);
         ry            = Utility.loadInt(reader);
         maxEventAge   = Utility.loadInt(reader);
         morphognostic = Morphognostic.load(input);
      }
   }
   public static final int NUM_SEGMENTS = 12;
   public Segment[] segments;
   public static final int NSEG = 48;
   public static final int NBAR = (NSEG + 1);
   public double[]         wormBody;
   public Point2D.         Double[] wormVerts;
   public Point[] segmentSimPositions;

   // Metamorphs.
   public ArrayList<Metamorph> metamorphs;
   public FastVector           metamorphWekaNNattributeNames;
   public Instances            metamorphWekaInstances;
   MultilayerPerceptron        metamorphWekaNN;
   public static final boolean saveMetamorphWekaInstances = false;
   public static final boolean saveMetamorphWekaNN        = false;
   public static final boolean evaluateMetamorphWekaNN    = true;

   // Random numbers.
   public int          randomSeed;
   public SecureRandom random;

   // Constructors.
   public Worm(Agar agar, int randomSeed)
   {
      init(agar, randomSeed);
      segments = new Segment[NUM_SEGMENTS];
      for (int i = 0; i < NUM_SEGMENTS; i++)
      {
         segments[i] = new Segment(i);
      }
      placeWormOnAgar();
      metamorphs = new ArrayList<Metamorph>();
      Morphognostic morphognostic = segments[0].morphognostic;
      initMetamorphWekaNN(morphognostic);
   }


   public Worm(Agar agar, int randomSeed,
               int NUM_NEIGHBORHOODS,
               int NEIGHBORHOOD_INITIAL_DIMENSION,
               int NEIGHBORHOOD_DIMENSION_STRIDE,
               int NEIGHBORHOOD_DIMENSION_MULTIPLIER,
               int EPOCH_INTERVAL_STRIDE,
               int EPOCH_INTERVAL_MULTIPLIER)
   {
      init(agar, randomSeed);
      segments = new Segment[NUM_SEGMENTS];
      for (int i = 0; i < NUM_SEGMENTS; i++)
      {
         segments[i] = new Segment(i,
                                   NUM_NEIGHBORHOODS,
                                   NEIGHBORHOOD_INITIAL_DIMENSION,
                                   NEIGHBORHOOD_DIMENSION_STRIDE,
                                   NEIGHBORHOOD_DIMENSION_MULTIPLIER,
                                   EPOCH_INTERVAL_STRIDE,
                                   EPOCH_INTERVAL_MULTIPLIER);
      }
      placeWormOnAgar();
      metamorphs = new ArrayList<Metamorph>();
      Morphognostic morphognostic = segments[0].morphognostic;
      initMetamorphWekaNN(morphognostic);
   }


   // Initialize.
   boolean init(Agar agar, int randomSeed)
   {
      this.agar       = agar;
      this.randomSeed = randomSeed;
      random          = new SecureRandom();
      random.setSeed(randomSeed);
      eventTime = 0;
      driver    = DRIVER_TYPE.WORMSIM.getValue();
      Wormsim.init();
      wormBody            = new double[NBAR * 3];
      wormVerts           = new Point2D.Double[NBAR];
      segmentSimPositions = new Point[NUM_SEGMENTS];
      getSegmentSimPositions();
      H2Opredict        = new WormWorxPredict();
      foundFood         = false;
      wormsimLock       = new Object();
      H2OresponseLabels = H2Opredict.initPredict("wormworx_model");
      if (H2OresponseLabels == null)
      {
         System.err.println("Cannot initialize H2O neural network");
         return(false);
      }
      else
      {
         return(true);
      }
   }


   // Terminate.
   public void terminate()
   {
      synchronized (wormsimLock)
      {
         Wormsim.terminate();
      }
   }


   // Reset.
   public void reset()
   {
      random.setSeed(randomSeed);
      eventTime = 0;
      synchronized (wormsimLock)
      {
         Wormsim.terminate();
         Wormsim.init();
      }
      for (Segment segment : segments)
      {
         segment.reset();
      }
      placeWormOnAgar();
      foundFood = false;
   }


   // Place worm on agar.
   public void placeWormOnAgar()
   {
      for (int x = 0; x < Agar.GRID_SIZE.width; x++)
      {
         for (int y = 0; y < Agar.GRID_SIZE.height; y++)
         {
            agar.cells[x][y][Agar.WORM_CELL_INDEX] = SectorDisplay.EMPTY_CELL_VALUE;
         }
      }
      for (int i = 0; i < NUM_SEGMENTS; i++)
      {
         Segment segment = segments[i];
         agar.cells[segment.x[0]][segment.y[0]][Agar.WORM_CELL_INDEX] = Agar.WORM_SEGMENT_VALUE;
      }
   }


   // Get segment positions from simulation.
   public void getSegmentSimPositions()
   {
      Wormsim.getBody(wormBody);
      float s = (float)Agar.SIZE.width * Agar.SCALE / 0.001f;
      for (int i = 0; i < NBAR; i++)
      {
         double x = ((wormBody[i * 3]) * s) + agar.x_off;
         double y = ((wormBody[i * 3 + 1]) * s) + agar.y_off;
         wormVerts[i] = new Point2D.Double(x, y);
      }
      double w = (double)Agar.SIZE.width / (double)Agar.GRID_SIZE.width;
      double h = (double)Agar.SIZE.height / (double)Agar.GRID_SIZE.height;
      for (int i = 0; i < NUM_SEGMENTS; i++)
      {
         double x = 0.0;
         double y = 0.0;
         for (int j = 0, k = i * 4; j < 4; j++, k++)
         {
            x += wormVerts[k].x;
            y += wormVerts[k].y;
         }
         x /= 4.0;
         y /= 4.0;
         segmentSimPositions[i] = new Point((int)(x / w), (int)(y / h));
      }
   }


   // Set driver.
   public void setDriver(int driver)
   {
      this.driver = driver;
      if (driver == DRIVER_TYPE.WORMSIM.getValue())
      {
         reset();
         return;
      }
   }


   // Save worm to file.
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


   // Save worm.
   public void save(FileOutputStream output) throws IOException
   {
      for (Segment segment : segments)
      {
         segment.save(output);
      }
      PrintWriter writer = new PrintWriter(new OutputStreamWriter(output));
      Utility.saveInt(writer, eventTime);
      Utility.saveInt(writer, metamorphs.size());
      for (Metamorph m : metamorphs)
      {
         m.save(output);
      }
      Utility.saveInt(writer, driver);
      writer.flush();
   }


   // Load worm from file.
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


   // Load worm.
   public void load(FileInputStream input) throws IOException
   {
      for (Segment segment : segments)
      {
         segment.load(input);
      }
      DataInputStream reader = new DataInputStream(input);
      eventTime = Utility.loadInt(reader);
      metamorphs.clear();
      int n = Utility.loadInt(reader);
      for (int i = 0; i < n; i++)
      {
         metamorphs.add(Metamorph.load(input));
      }
      Morphognostic morphognostic = segments[0].morphognostic;
      initMetamorphWekaNN(morphognostic);
      driver = Utility.loadInt(reader);
      setDriver(driver);
   }


   // Step.
   // Return true if food found.
   boolean step()
   {
      int width, height;

      // Check if food found.
      float sx = (float)agar.saltyX[agar.currentSalty] / Agar.CELL_WIDTH;
      float sy = (float)agar.saltyY[agar.currentSalty] / Agar.CELL_HEIGHT;

      sy = Agar.GRID_SIZE.height - sy;
      float dist = (float)Math.sqrt(Math.pow((sx - (float)segments[0].x[0]), 2) +
                                    Math.pow((sy - segments[0].y[0]), 2));
      if (dist <= Agar.SALT_CONSUMPTION_RANGE)
      {
         foundFood = true;
      }
      if (foundFood)
      {
         return(true);
      }

      // Step simulation?
      if (driver == DRIVER_TYPE.WORMSIM.getValue())
      {
         double dorsal  = 1.0;
         double ventral = 1.0;
         switch (agar.currentSalty)
         {
         case Agar.RED_FOOD:
            ventral = 1.1;
            break;

         case Agar.GREEN_FOOD:
            break;

         case Agar.BLUE_FOOD:
            dorsal = 1.5;
            break;
         }
         if (DORSAL_SMB_MUSCLE_AMPLIFIER_OVERRIDE != -1.0)
         {
            dorsal = DORSAL_SMB_MUSCLE_AMPLIFIER_OVERRIDE;
         }
         if (VENTRAL_SMB_MUSCLE_AMPLIFIER_OVERRIDE != -1.0)
         {
            ventral = VENTRAL_SMB_MUSCLE_AMPLIFIER_OVERRIDE;
         }
         Wormsim.overrideSMBmuscleAmplifiers(dorsal, ventral);
         synchronized (wormsimLock)
         {
            Wormsim.step(0.0);
         }
         getSegmentSimPositions();
      }

      // Cycle segments.
      float[] sensors = new float[NUM_SENSORS];
      width           = Agar.GRID_SIZE.width;
      height          = Agar.GRID_SIZE.height;
      for (int i = 0; i < NUM_SEGMENTS; i++)
      {
         Segment segment = segments[i];

         // Initialize sensors.
         for (int j = 0; j < 11; j++)
         {
            int n = i - (j + 1);
            if (n < 0) { n += NUM_SEGMENTS; }
            sensors[j * 2]       = segments[n].x[0] - segment.x[0];
            sensors[(j * 2) + 1] = segments[n].y[0] - segment.y[0];
         }
         for (int j = 0; j < 4; j++)
         {
            int x = segment.x[0];
            int y = segment.y[0];
            switch (j)
            {
            case 0:
               y           = ((y + 1) % height);
               sensors[22] = (float)agar.cells[x][y][Agar.SALT_CELL_INDEX];
               break;

            case 1:
               x--;
               if (x < 0) { x += width; }
               sensors[23] = (float)agar.cells[x][y][Agar.SALT_CELL_INDEX];
               break;

            case 2:
               x           = ((x + 1) % width);
               sensors[24] = (float)agar.cells[x][y][Agar.SALT_CELL_INDEX];
               break;

            case 3:
               y--;
               if (y < 0) { y += height; }
               sensors[25] = (float)agar.cells[x][y][Agar.SALT_CELL_INDEX];
               break;
            }
         }
         float saltMin = -1.0f;
         for (int j = 22; j < 26; j++)
         {
            if ((saltMin < 0.0f) || (sensors[j] < saltMin))
            {
               saltMin = sensors[j];
            }
         }
         for (int j = 22; j < 26; j++)
         {
            sensors[j] -= saltMin;
            if (sensors[j] > 2.0f) { sensors[j] = 2.0f; }
         }

         // Cycle segment.
         segment.cycle(sensors);
      }
      eventTime++;

      // Execute responses.
      for (int i = 0; i < NUM_SEGMENTS; i++)
      {
         Segment segment = segments[i];
         segment.collapsePosition();

         int ux, uy, dx, dy, lx, ly, rx, ry;
         ux = dx = lx = rx = 0;
         uy = dy = ly = ry = 0;
         for (int j = 0; j < 4; j++)
         {
            int x = segment.x[0];
            int y = segment.y[0];
            switch (j)
            {
            case 0:
               y  = ((y + 1) % height);
               ux = x;
               uy = y;
               break;

            case 1:
               x--;
               if (x < 0) { x += width; }
               lx = x;
               ly = y;
               break;

            case 2:
               x  = ((x + 1) % width);
               rx = x;
               ry = y;
               break;

            case 3:
               y--;
               if (y < 0) { y += height; }
               dx = x;
               dy = y;
               break;
            }
         }
         for (int j = 0; j < Segment.NUM_SUPERPOSITIONS; j++)
         {
            segment.x[j] = segment.x[0];
            segment.y[j] = segment.y[0];
            switch (segment.responses[j])
            {
            case MOVE_NW:
               segment.x[j] = lx;
               segment.y[j] = uy;
               break;

            case MOVE_NORTH:
               segment.x[j] = ux;
               segment.y[j] = uy;
               break;

            case MOVE_NE:
               segment.x[j] = rx;
               segment.y[j] = uy;
               break;

            case MOVE_WEST:
               segment.x[j] = lx;
               segment.y[j] = ly;
               break;

            case STAY:
               break;

            case MOVE_EAST:
               segment.x[j] = rx;
               segment.y[j] = ry;
               break;

            case MOVE_SW:
               segment.x[j] = lx;
               segment.y[j] = dy;
               break;

            case MOVE_SOUTH:
               segment.x[j] = dx;
               segment.y[j] = dy;
               break;

            case MOVE_SE:
               segment.x[j] = rx;
               segment.y[j] = dy;
               break;
            }
         }
      }
      placeWormOnAgar();
      return(false);
   }


   // Get metamorph DB response distances.
   float[] metamorphDBresponse(Morphognostic morphognostic)
   {
      float[] responseDistances = new float[NUM_RESPONSES];
      for (int i = 0; i < NUM_RESPONSES; i++)
      {
         responseDistances[i] = -1.0f;
      }
      for (Metamorph m : metamorphs)
      {
         float d = morphognostic.compare(m.morphognostic);
         if (responseDistances[m.response] == -1.0f)
         {
            responseDistances[m.response] = d;
         }
         else
         {
            if (d < responseDistances[m.response])
            {
               responseDistances[m.response] = d;
            }
            else if (d == responseDistances[m.response])
            {
               if (random.nextBoolean())
               {
                  responseDistances[m.response] = d;
               }
            }
         }
      }
      return(responseDistances);
   }


   // Get metamorph Weka neural network response probabilities.
   float[] metamorphWekaNNresponse(Morphognostic morphognostic)
   {
      return(classifyMorphognostic(morphognostic));
   }


   // Get metamorph H2O neural network response probabilities.
   float[] metamorphH2ONNresponse(Morphognostic morphognostic)
   {
      float[] responseProbabilities = new float[NUM_RESPONSES];
      for (int i = 0; i < responseProbabilities.length; i++)
      {
         responseProbabilities[i] = 0.0f;
      }
      try {
         float[] p = H2Opredict.predict(morphognostic2csv(morphognostic) + ",STAY");
         for (int i = 0; i < H2OresponseLabels.length; i++)
         {
            int j = getResponseValue(H2OresponseLabels[i]);
            responseProbabilities[j] = p[i];
         }
      }
      catch (Exception e) {
         System.err.println("H2O prediction failed: " + e.getMessage());
         return(null);
      }
      return(responseProbabilities);
   }


   // Wormsim response.
   int wormsimResponse(Segment segment)
   {
      int sx       = segmentSimPositions[segment.number].x;
      int sy       = segmentSimPositions[segment.number].y;
      int width    = Agar.GRID_SIZE.width;
      int height   = Agar.GRID_SIZE.height;
      int response = -1;

      for (int i = 0; i < 9 && response == -1; i++)
      {
         int x = segment.x[0];
         int y = segment.y[0];
         switch (i)
         {
         case 0:
            x--;
            if (x < 0) { x += width; }
            y = ((y + 1) % height);
            if ((x == sx) && (y == sy))
            {
               response = MOVE_NW;
            }
            break;

         case 1:
            y = ((y + 1) % height);
            if ((x == sx) && (y == sy))
            {
               response = MOVE_NORTH;
            }
            break;

         case 2:
            x = ((x + 1) % width);
            y = ((y + 1) % height);
            if ((x == sx) && (y == sy))
            {
               response = MOVE_NE;
            }
            break;

         case 3:
            x--;
            if (x < 0) { x += width; }
            if ((x == sx) && (y == sy))
            {
               response = MOVE_WEST;
            }
            break;

         case 4:
            if ((x == sx) && (y == sy))
            {
               response = STAY;
            }
            break;

         case 5:
            x = ((x + 1) % width);
            if ((x == sx) && (y == sy))
            {
               response = MOVE_EAST;
            }
            break;

         case 6:
            x--;
            if (x < 0) { x += width; }
            y--;
            if (y < 0) { y += height; }
            if ((x == sx) && (y == sy))
            {
               response = MOVE_SW;
            }
            break;

         case 7:
            y--;
            if (y < 0) { y += height; }
            if ((x == sx) && (y == sy))
            {
               response = MOVE_SOUTH;
            }
            break;

         case 8:
            x = ((x + 1) % width);
            y--;
            if (y < 0) { y += height; }
            if ((x == sx) && (y == sy))
            {
               response = MOVE_SE;
            }
            break;
         }
      }
      if (response == -1) { response = STAY; }
      return(response);
   }


   // Initialize metamorph Weka neural network.
   public void initMetamorphWekaNN(Morphognostic morphognostic)
   {
      metamorphWekaNNattributeNames = new FastVector();
      for (int i = 0; i < morphognostic.NUM_NEIGHBORHOODS; i++)
      {
         int n = morphognostic.neighborhoods.get(i).sectors.length;
         for (int x = 0; x < n; x++)
         {
            for (int y = 0; y < n; y++)
            {
               for (int d = 0; d < morphognostic.eventDimensions; d++)
               {
                  for (int j = 0; j < morphognostic.numEventTypes[d]; j++)
                  {
                     metamorphWekaNNattributeNames.addElement(new Attribute(i + "-" + x + "-" + y + "-" + d + "-" + j));
                  }
               }
            }
         }
      }
      FastVector responseVals = new FastVector();
      for (int i = 0; i < NUM_RESPONSES; i++)
      {
         responseVals.addElement(i + "");
      }
      metamorphWekaNNattributeNames.addElement(new Attribute("type", responseVals));
      metamorphWekaInstances = new Instances("metamorphs", metamorphWekaNNattributeNames, 0);
      metamorphWekaNN        = new MultilayerPerceptron();
   }


   // Create and train metamorph neural network.
   public void createMetamorphWekaNN() throws Exception
   {
      // Create instances.
      metamorphWekaInstances = new Instances("metamorphs", metamorphWekaNNattributeNames, 0);
      for (Metamorph m : metamorphs)
      {
         metamorphWekaInstances.add(createInstance(metamorphWekaInstances, m));
      }
      metamorphWekaInstances.setClassIndex(metamorphWekaInstances.numAttributes() - 1);

      // Create and train the neural network.
      MultilayerPerceptron mlp = new MultilayerPerceptron();
      metamorphWekaNN = mlp;
      mlp.setLearningRate(0.1);
      mlp.setMomentum(0.2);
      mlp.setTrainingTime(2000);
      mlp.setHiddenLayers("20");
      mlp.setOptions(Utils.splitOptions("-L 0.1 -M 0.2 -N 2000 -V 0 -S 0 -E 20 -H 20"));
      mlp.buildClassifier(metamorphWekaInstances);

      // Save training instances?
      if (saveMetamorphWekaInstances)
      {
         ArffSaver saver = new ArffSaver();
         saver.setInstances(metamorphWekaInstances);
         saver.setFile(new File("metamorphWekaInstances.arff"));
         saver.writeBatch();
      }

      // Save networks?
      if (saveMetamorphWekaNN)
      {
         Debug.saveToFile("metamorphWekaNN.dat", mlp);
      }

      // Evaluate the network.
      if (evaluateMetamorphWekaNN)
      {
         Evaluation eval = new Evaluation(metamorphWekaInstances);
         eval.evaluateModel(mlp, metamorphWekaInstances);
         System.out.println("Error rate=" + eval.errorRate());
         System.out.println(eval.toSummaryString());
      }
   }


   // Create metamorph Weka NN instance.
   Instance createInstance(Instances instances, Metamorph m)
   {
      double[]  attrValues = new double[instances.numAttributes()];
      int a = 0;
      for (int i = 0; i < m.morphognostic.NUM_NEIGHBORHOODS; i++)
      {
         int n = m.morphognostic.neighborhoods.get(i).sectors.length;
         for (int x = 0; x < n; x++)
         {
            for (int y = 0; y < n; y++)
            {
               Morphognostic.Neighborhood.Sector s = m.morphognostic.neighborhoods.get(i).sectors[x][y];
               for (int d = 0; d < m.morphognostic.eventDimensions; d++)
               {
                  for (int j = 0; j < s.typeDensities[d].length; j++)
                  {
                     attrValues[a] = s.typeDensities[d][j];
                     a++;
                  }
               }
            }
         }
      }
      attrValues[a] = instances.attribute(a).indexOfValue(m.response + "");
      a++;
      return(new Instance(1.0, attrValues));
   }


   // Save metamorph neural network training dataset.
   public void saveMetamorphNNtrainingData() throws Exception
   {
      FileOutputStream output;

      try
      {
         output = new FileOutputStream(new File(NN_DATASET_SAVE_FILE_NAME));
      }
      catch (Exception e)
      {
         throw new IOException("Cannot open output file " + NN_DATASET_SAVE_FILE_NAME + ":" + e.getMessage());
      }
      PrintWriter writer = new PrintWriter(new OutputStreamWriter(output));
      boolean     header = true;
      for (Metamorph m : metamorphs)
      {
         String csv = morphognostic2csv(m.morphognostic);
         if (m.responseName.isEmpty())
         {
            csv += ("," + m.response);
         }
         else
         {
            csv += ("," + m.responseName);
         }
         if (header)
         {
            header = false;
            int    j    = csv.split(",").length - 1;
            String csv2 = "";
            for (int i = 0; i < j; i++)
            {
               csv2 += ("c" + i + ",");
            }
            csv2 += "response";
            writer.println(csv2);
         }
         writer.println(csv);
      }
      output.close();
   }


   // Flatten morphognostic to csv string.
   public String morphognostic2csv(Morphognostic morphognostic)
   {
      String  output    = "";
      boolean skipComma = true;

      for (int i = 0; i < morphognostic.NUM_NEIGHBORHOODS; i++)
      {
         int n = morphognostic.neighborhoods.get(i).sectors.length;
         for (int x = 0; x < n; x++)
         {
            for (int y = 0; y < n; y++)
            {
               Morphognostic.Neighborhood.Sector s = morphognostic.neighborhoods.get(i).sectors[x][y];
               for (int d = 0; d < morphognostic.eventDimensions; d++)
               {
                  for (int j = 0; j < s.typeDensities[d].length; j++)
                  {
                     if (skipComma)
                     {
                        skipComma = false;
                     }
                     else
                     {
                        output += ",";
                     }
                     output += (s.typeDensities[d][j] + "");
                  }
               }
            }
         }
      }
      return(output);
   }


   // Use metamorph Weka NN to classify morphognostic as a response.
   public float[] classifyMorphognostic(Morphognostic morphognostic)
   {
      Metamorph metamorph = new Metamorph(morphognostic, STAY, getResponseName(STAY));

      float[] responseProbabilities = new float[NUM_RESPONSES];
      try
      {
         // Classify.
         Instance instance = createInstance(metamorphWekaInstances, metamorph);
         //int      predictionIndex = (int)metamorphWekaNN.classifyInstance(instance);

         // Get the predicted class label from the predictionIndex.
         //String predictedClassLabel = metamorphWekaInstances.classAttribute().value(predictionIndex);
         //int response = Integer.parseInt(predictedClassLabel);

         // Get the prediction probability distribution.
         double[] d = metamorphWekaNN.distributionForInstance(instance);
         for (int i = 0; i < NUM_RESPONSES; i++)
         {
            responseProbabilities[i] = (float)d[i];
         }
      }
      catch (Exception e)
      {
         System.err.println("Error classifying morphognostic: " + e.getMessage());
      }
      return(responseProbabilities);
   }


   // Response names.
   public String getResponseName(int response)
   {
      switch (response)
      {
      case MOVE_NW:
         return("MOVE_NW");

      case MOVE_NORTH:
         return("MOVE_NORTH");

      case MOVE_NE:
         return("MOVE_NE");

      case MOVE_WEST:
         return("MOVE_WEST");

      case STAY:
         return("STAY");

      case MOVE_EAST:
         return("MOVE_EAST");

      case MOVE_SW:
         return("MOVE_SW");

      case MOVE_SOUTH:
         return("MOVE_SOUTH");

      case MOVE_SE:
         return("MOVE_SE");
      }
      return("");
   }


   // Response value from name.
   public int getResponseValue(String name)
   {
      if (name.equals("MOVE_NW"))
      {
         return(MOVE_NW);
      }

      if (name.equals("MOVE_NORTH"))
      {
         return(MOVE_NORTH);
      }

      if (name.equals("MOVE_NE"))
      {
         return(MOVE_NE);
      }

      if (name.equals("MOVE_WEST"))
      {
         return(MOVE_WEST);
      }

      if (name.equals("STAY"))
      {
         return(STAY);
      }

      if (name.equals("MOVE_EAST"))
      {
         return(MOVE_EAST);
      }

      if (name.equals("MOVE_SW"))
      {
         return(MOVE_SW);
      }

      if (name.equals("MOVE_SOUTH"))
      {
         return(MOVE_SOUTH);
      }

      if (name.equals("MOVE_SE"))
      {
         return(MOVE_SE);
      }

      return(-1);
   }
}
