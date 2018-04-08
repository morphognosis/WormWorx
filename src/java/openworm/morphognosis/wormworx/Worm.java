// For conditions of distribution and use, see copyright notice in Main.java

// C. elegans worm: a morphognosis organism.

package openworm.morphognosis.wormworx;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.io.*;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hex.genmodel.tools.WormWorxBodyPredict;
import hex.genmodel.tools.WormWorxHeadPredict;
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

   // Directions.
   public static final int NORTHWEST      = 0;
   public static final int NORTH          = 1;
   public static final int NORTHEAST      = 2;
   public static final int WEST           = 3;
   public static final int CENTER         = 4;
   public static final int EAST           = 5;
   public static final int SOUTHWEST      = 6;
   public static final int SOUTH          = 7;
   public static final int SOUTHEAST      = 8;
   public static final int NUM_DIRECTIONS = 9;

   // Responses.
   public static final int NUM_RESPONSES = NUM_DIRECTIONS;
   public static final int STAY          = CENTER;

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
   public WormWorxHeadPredict H2OheadPredict;
   public WormWorxBodyPredict H2ObodyPredict;
   public String[] H2OresponseLabels;

   // Found food?
   public boolean foundFood;

   // Simulator synchronization.
   public Object wormsimLock;

   // Neural network dataset save file names.
   public static final String HEAD_NN_DATASET_SAVE_FILE_NAME = "headMetamorphs.csv";
   public static final String BODY_NN_DATASET_SAVE_FILE_NAME = "bodyMetamorphs.csv";

   // Worm segment.
   public class Segment
   {
      // Number.
      public int number;

      // Position.
      public int x, x2, rx;
      public int y, y2, ry;

      // Sensors.
      public int   NUM_SENSORS;
      public int[] sensors;

      // Response.
      public int response;

      // Morphognostic.
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

      // Metamorphs.
      public HashMap < Integer, List < Metamorph >> metamorphs;

      // Constructors.
      public Segment(int number, int numSensors, HashMap < Integer, List < Metamorph >> metamorphs)
      {
         init(number, numSensors, metamorphs);
         int [] numEventTypes = new int[NUM_SENSORS];
         for (int i = 0; i < NUM_SENSORS; i++)
         {
            numEventTypes[i] = 9;
         }
         morphognostic = new Morphognostic(Orientation.WEST, numEventTypes);
         Morphognostic.Neighborhood n = morphognostic.neighborhoods.get(morphognostic.NUM_NEIGHBORHOODS - 1);
         maxEventAge = n.epoch + n.duration - 1;
      }


      public Segment(int number, int numSensors, HashMap < Integer, List < Metamorph >> metamorphs,
                     int NUM_NEIGHBORHOODS,
                     int NEIGHBORHOOD_INITIAL_DIMENSION,
                     int NEIGHBORHOOD_DIMENSION_STRIDE,
                     int NEIGHBORHOOD_DIMENSION_MULTIPLIER,
                     int EPOCH_INTERVAL_STRIDE,
                     int EPOCH_INTERVAL_MULTIPLIER)
      {
         init(number, numSensors, metamorphs);
         int [] numEventTypes = new int[NUM_SENSORS];
         for (int i = 0; i < NUM_SENSORS; i++)
         {
            numEventTypes[i] = 9;
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


      void init(int number, int numSensors, HashMap < Integer, List < Metamorph >> metamorphs)
      {
         this.number     = number;
         NUM_SENSORS     = numSensors;
         this.metamorphs = metamorphs;

         x       = rx = segmentSimPositions[number].x;
         y       = ry = segmentSimPositions[number].y;
         sensors = new int[NUM_SENSORS];
         for (int i = 0; i < NUM_SENSORS; i++)
         {
            sensors[i] = CENTER;
         }
         response    = STAY;
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
         x = rx;
         y = ry;
         for (int i = 0; i < NUM_SENSORS; i++)
         {
            sensors[i] = CENTER;
         }
         response = STAY;
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
      public int cycle(int[] sensors)
      {
         // Update morphognostic.
         int[] values = new int[NUM_SENSORS];
         for (int i = 0; i < NUM_SENSORS; i++)
         {
            this.sensors[i] = sensors[i];
            values[i]       = sensors[i];
         }
         events.add(new Event(values, x, y, eventTime));
         if ((eventTime - events.get(0).time) > maxEventAge)
         {
            events.remove(0);
         }
         int w = Agar.GRID_SIZE.width;
         int h = Agar.GRID_SIZE.height;
         int a = maxEventAge + 1;
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
         morphognostic.update(morphEvents, x, y);

         // Respond.
         if (driver == DRIVER_TYPE.METAMORPH_DB.getValue())
         {
            response = metamorphDBresponse(morphognostic, number);
         }
         else if (driver == DRIVER_TYPE.METAMORPH_WEKA_NN.getValue())
         {
            response = metamorphWekaNNresponse(morphognostic, number);
         }
         else if (driver == DRIVER_TYPE.METAMORPH_H2O_NN.getValue())
         {
            response = metamorphH2ONNresponse(morphognostic, number);
         }
         else if (driver == DRIVER_TYPE.WORMSIM.getValue())
         {
            response = wormsimResponse(this);
         }
         else
         {
            response = STAY;
         }
         projectResponsePosition();

         // Update metamorphs.
         Metamorph       metamorph         = new Metamorph(morphognostic.clone(), response, getResponseName(response));
         int             morphognosticHash = hashMorphognostic(metamorph.morphognostic);
         List<Metamorph> metamorphValues   = metamorphs.get(morphognosticHash);
         if (metamorphValues != null)
         {
            boolean found = false;
            boolean dup   = false;
            for (Metamorph m : metamorphValues)
            {
               if (m.response == metamorph.response)
               {
                  found = true;
               }
               else
               {
                  dup = true;
               }
            }
            if (!found)
            {
               if (dup)
               {
                  System.out.println("Warning: metamorph with same morphognostic and different response added");
               }
               metamorphValues.add(metamorph);
            }
         }
         else
         {
            ArrayList<Metamorph> metamorphList = new ArrayList<Metamorph>();
            metamorphList.add(metamorph);
            metamorphs.put(morphognosticHash, metamorphList);
         }
         return(response);
      }


      // Determine next position from response.
      public void projectResponsePosition()
      {
         int width  = Agar.GRID_SIZE.width;
         int height = Agar.GRID_SIZE.height;
         int nx, ny, sx, sy, wx, wy, ex, ey;

         nx = x;
         ny = ((y + 1) % height);
         wx = x - 1;
         if (wx < 0) { wx += width; }
         wy = y;
         ex = ((x + 1) % width);
         ey = y;
         sx = x;
         sy = y - 1;
         if (sy < 0) { sy += height; }
         switch (response)
         {
         case NORTHWEST:
            x2 = wx;
            y2 = ny;
            break;

         case NORTH:
            x2 = nx;
            y2 = ny;
            break;

         case NORTHEAST:
            x2 = ex;
            y2 = ny;
            break;

         case WEST:
            x2 = wx;
            y2 = wy;
            break;

         case STAY:
            x2 = x;
            y2 = y;
            break;

         case EAST:
            x2 = ex;
            y2 = ey;
            break;

         case SOUTHWEST:
            x2 = wx;
            y2 = sy;
            break;

         case SOUTH:
            x2 = sx;
            y2 = sy;
            break;

         case SOUTHEAST:
            x2 = ex;
            y2 = sy;
            break;
         }
      }


      // Set projected position.
      public void setProjectedPosition()
      {
         x = x2;
         y = y2;
      }


      public void save(DataOutputStream output) throws IOException
      {
         Utility.saveInt(output, x);
         Utility.saveInt(output, y);
         Utility.saveInt(output, rx);
         Utility.saveInt(output, ry);
         Utility.saveInt(output, maxEventAge);
         morphognostic.save(output);
      }


      public void load(DataInputStream input) throws IOException
      {
         x             = Utility.loadInt(input);
         y             = Utility.loadInt(input);
         rx            = Utility.loadInt(input);
         ry            = Utility.loadInt(input);
         maxEventAge   = Utility.loadInt(input);
         morphognostic = Morphognostic.load(input);
      }
   }

   // Head segment.
   public class HeadSegment extends Segment
   {
      // Salt direction sensor.
      public static final int NUM_HEAD_SENSORS = 2;

      // Constructors.
      public HeadSegment(int number, HashMap < Integer, List < Metamorph >> headMetamorphs)
      {
         super(number, NUM_HEAD_SENSORS, headMetamorphs);
      }


      public HeadSegment(int number, HashMap < Integer, List < Metamorph >> headMetamorphs,
                         int NUM_NEIGHBORHOODS,
                         int NEIGHBORHOOD_INITIAL_DIMENSION,
                         int NEIGHBORHOOD_DIMENSION_STRIDE,
                         int NEIGHBORHOOD_DIMENSION_MULTIPLIER,
                         int EPOCH_INTERVAL_STRIDE,
                         int EPOCH_INTERVAL_MULTIPLIER)
      {
         super(number, NUM_HEAD_SENSORS, headMetamorphs,
               NUM_NEIGHBORHOODS,
               NEIGHBORHOOD_INITIAL_DIMENSION,
               NEIGHBORHOOD_DIMENSION_STRIDE,
               NEIGHBORHOOD_DIMENSION_MULTIPLIER,
               EPOCH_INTERVAL_STRIDE,
               EPOCH_INTERVAL_MULTIPLIER);
      }
   }

   // Body segment.
   public class BodySegment extends Segment
   {
      // Anterior segment current and next directions.
      public static final int NUM_BODY_SENSORS = 3;

      public BodySegment(int number, HashMap < Integer, List < Metamorph >> bodyMetamorphs)
      {
         super(number, NUM_BODY_SENSORS, bodyMetamorphs);
      }


      // Constructors.
      public BodySegment(int number, HashMap < Integer, List < Metamorph >> bodyMetamorphs,
                         int NUM_NEIGHBORHOODS,
                         int NEIGHBORHOOD_INITIAL_DIMENSION,
                         int NEIGHBORHOOD_DIMENSION_STRIDE,
                         int NEIGHBORHOOD_DIMENSION_MULTIPLIER,
                         int EPOCH_INTERVAL_STRIDE,
                         int EPOCH_INTERVAL_MULTIPLIER)
      {
         super(number, NUM_BODY_SENSORS, bodyMetamorphs,
               NUM_NEIGHBORHOODS,
               NEIGHBORHOOD_INITIAL_DIMENSION,
               NEIGHBORHOOD_DIMENSION_STRIDE,
               NEIGHBORHOOD_DIMENSION_MULTIPLIER,
               EPOCH_INTERVAL_STRIDE,
               EPOCH_INTERVAL_MULTIPLIER);
      }
   }

   public static final int NUM_SEGMENTS      = 12;
   public static final int NUM_BODY_SEGMENTS = 5;
   public HeadSegment      headSegment;
   public BodySegment[] bodySegments;
   public static final int NSEG = 48;
   public static final int NBAR = (NSEG + 1);
   public double[]         wormBody;
   public Point2D.         Double[] wormVerts;
   public Point[] segmentSimPositions;

   // Metamorphs.
   public                      HashMap < Integer, List < Metamorph >> headMetamorphs;
   public                      HashMap < Integer, List < Metamorph >> bodyMetamorphs;
   public FastVector           headMetamorphWekaNNattributeNames;
   public FastVector           bodyMetamorphWekaNNattributeNames;
   public Instances            headMetamorphWekaInstances;
   public Instances            bodyMetamorphWekaInstances;
   MultilayerPerceptron        headMetamorphWekaNN;
   MultilayerPerceptron        bodyMetamorphWekaNN;
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
      headMetamorphs = new HashMap < Integer, List < Metamorph >> ();
      headSegment    = new HeadSegment(0, headMetamorphs);
      bodyMetamorphs = new HashMap < Integer, List < Metamorph >> ();
      bodySegments   = new BodySegment[NUM_BODY_SEGMENTS];
      for (int i = 0; i < NUM_BODY_SEGMENTS; i++)
      {
         bodySegments[i] = new BodySegment(i + 1, bodyMetamorphs);
      }
      placeWormOnAgar();
      Morphognostic morphognostic = headSegment.morphognostic;
      initHeadMetamorphWekaNN(morphognostic);
      morphognostic = bodySegments[0].morphognostic;
      initBodyMetamorphWekaNN(morphognostic);
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
      headMetamorphs = new HashMap < Integer, List < Metamorph >> ();
      headSegment    = new HeadSegment(0, headMetamorphs,
                                       NUM_NEIGHBORHOODS,
                                       NEIGHBORHOOD_INITIAL_DIMENSION,
                                       NEIGHBORHOOD_DIMENSION_STRIDE,
                                       NEIGHBORHOOD_DIMENSION_MULTIPLIER,
                                       EPOCH_INTERVAL_STRIDE,
                                       EPOCH_INTERVAL_MULTIPLIER);
      bodyMetamorphs = new HashMap < Integer, List < Metamorph >> ();
      bodySegments   = new BodySegment[NUM_BODY_SEGMENTS];
      for (int i = 0; i < NUM_BODY_SEGMENTS; i++)
      {
         bodySegments[i] = new BodySegment(i + 1, bodyMetamorphs,
                                           NUM_NEIGHBORHOODS,
                                           NEIGHBORHOOD_INITIAL_DIMENSION,
                                           NEIGHBORHOOD_DIMENSION_STRIDE,
                                           NEIGHBORHOOD_DIMENSION_MULTIPLIER,
                                           EPOCH_INTERVAL_STRIDE,
                                           EPOCH_INTERVAL_MULTIPLIER);
      }
      placeWormOnAgar();
      Morphognostic morphognostic = headSegment.morphognostic;
      initHeadMetamorphWekaNN(morphognostic);
      morphognostic = bodySegments[0].morphognostic;
      initBodyMetamorphWekaNN(morphognostic);
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
      H2OheadPredict = new WormWorxHeadPredict();
      H2ObodyPredict = new WormWorxBodyPredict();
      foundFood      = false;
      wormsimLock    = new Object();
      boolean result = true;
      H2OresponseLabels = H2OheadPredict.initPredict("wormworx_head_model");
      if (H2OresponseLabels == null)
      {
         result = false;
      }
      if (H2ObodyPredict.initPredict("wormworx_body_model") == null)
      {
         result = false;
      }
      return(result);
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
      headSegment.reset();
      for (BodySegment segment : bodySegments)
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
            agar.wormCells[x][y] = SectorDisplay.EMPTY_CELL_VALUE;
         }
      }
      agar.wormCells[headSegment.x][headSegment.y] = Agar.WORM_SEGMENT_VALUE;
      for (int i = 0; i < NUM_BODY_SEGMENTS; i++)
      {
         BodySegment segment = bodySegments[i];
         agar.wormCells[segment.x][segment.y] = Agar.WORM_SEGMENT_VALUE;
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
      output.flush();
      output.close();
   }


   // Save worm.
   public void save(DataOutputStream output) throws IOException
   {
      headSegment.save(output);
      for (BodySegment segment : bodySegments)
      {
         segment.save(output);
      }
      Utility.saveInt(output, eventTime);
      Utility.saveInt(output, headMetamorphs.size());
      for (Map.Entry < Integer, List < Metamorph >> entry : headMetamorphs.entrySet())
      {
         int morphognosticHash = entry.getKey();
         Utility.saveInt(output, morphognosticHash);
         List<Metamorph> metamorphList = entry.getValue();
         Utility.saveInt(output, metamorphList.size());
         for (Metamorph m : metamorphList)
         {
            m.save(output);
         }
      }
      Utility.saveInt(output, bodyMetamorphs.size());
      for (Map.Entry < Integer, List < Metamorph >> entry : bodyMetamorphs.entrySet())
      {
         int morphognosticHash = entry.getKey();
         Utility.saveInt(output, morphognosticHash);
         List<Metamorph> metamorphList = entry.getValue();
         Utility.saveInt(output, metamorphList.size());
         for (Metamorph m : metamorphList)
         {
            m.save(output);
         }
      }
   }


   // Load worm from file.
   public void load(String filename) throws IOException
   {
      DataInputStream input;

      try
      {
         input = new DataInputStream(new BufferedInputStream(new FileInputStream(new File(filename))));
      }
      catch (Exception e)
      {
         throw new IOException("Cannot open input file " + filename + ":" + e.getMessage());
      }
      load(input);
      input.close();
   }


   // Load worm.
   public void load(DataInputStream input) throws IOException
   {
      headSegment.load(input);
      for (BodySegment segment : bodySegments)
      {
         segment.load(input);
      }
      eventTime = Utility.loadInt(input);
      headMetamorphs.clear();
      int n = Utility.loadInt(input);
      for (int i = 0; i < n; i++)
      {
         int morphognosticHash = Utility.loadInt(input);
         int n2 = Utility.loadInt(input);
         ArrayList<Metamorph> metamorphList = new ArrayList<Metamorph>();
         for (int j = 0; j < n2; j++)
         {
            metamorphList.add(Metamorph.load(input));
         }
         headMetamorphs.put(morphognosticHash, metamorphList);
      }
      bodyMetamorphs.clear();
      n = Utility.loadInt(input);
      for (int i = 0; i < n; i++)
      {
         int morphognosticHash = Utility.loadInt(input);
         int n2 = Utility.loadInt(input);
         ArrayList<Metamorph> metamorphList = new ArrayList<Metamorph>();
         for (int j = 0; j < n2; j++)
         {
            metamorphList.add(Metamorph.load(input));
         }
         bodyMetamorphs.put(morphognosticHash, metamorphList);
      }
      Morphognostic morphognostic = headSegment.morphognostic;
      initHeadMetamorphWekaNN(morphognostic);
      morphognostic = bodySegments[0].morphognostic;
      initBodyMetamorphWekaNN(morphognostic);
   }


   // Step.
   // Return true if food found.
   boolean step()
   {
      int width, height, x, y;

      // Check if food found.
      if (agar.saltCells[headSegment.x][headSegment.y] <= Agar.SALT_CONSUMPTION_RANGE)
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
            dorsal = 1.5;
            break;

         case Agar.GREEN_FOOD:
            dorsal = 1.08;
            break;

         case Agar.BLUE_FOOD:
            ventral = 1.06;
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
      width  = Agar.GRID_SIZE.width;
      height = Agar.GRID_SIZE.height;
      for (int i = 0; i <= NUM_BODY_SEGMENTS; i++)
      {
         Segment segment;
         if (i == 0)
         {
            segment = headSegment;
         }
         else
         {
            segment = bodySegments[i - 1];
         }

         int nx, ny, sx, sy, wx, wy, ex, ey;
         x  = segment.x;
         y  = segment.y;
         nx = x;
         ny = ((y + 1) % height);
         wx = x - 1;
         if (wx < 0) { wx += width; }
         wy = y;
         ex = ((x + 1) % width);
         ey = y;
         sx = x;
         sy = y - 1;
         if (sy < 0) { sy += height; }

         // Head segment?
         if (i == 0)
         {
            // Initialize sensors.
            int[] sensors = new int[headSegment.NUM_SENSORS];
            float dist = agar.saltCells[headSegment.x][headSegment.y];
            float d    = 0.0f;
            int   dir  = CENTER;
            for (int j = 0; j < NUM_DIRECTIONS; j++)
            {
               switch (j)
               {
               case NORTHWEST:
                  d = agar.saltCells[wx][ny];
                  break;

               case NORTH:
                  d = agar.saltCells[nx][ny];
                  break;

               case NORTHEAST:
                  d = agar.saltCells[ex][ny];
                  break;

               case WEST:
                  d = agar.saltCells[wx][wy];
                  break;

               case CENTER:
                  d = agar.saltCells[headSegment.x][headSegment.y];
                  break;

               case EAST:
                  d = agar.saltCells[ex][ey];
                  break;

               case SOUTHWEST:
                  d = agar.saltCells[wx][sy];
                  break;

               case SOUTH:
                  d = agar.saltCells[sx][sy];
                  break;

               case SOUTHEAST:
                  d = agar.saltCells[ex][sy];
                  break;
               }
               if (d < dist)
               {
                  dist = d;
                  dir  = j;
               }
            }
            sensors[0] = dir;
            sensors[1] = headSegment.response;

            // Cycle segment.
            headSegment.cycle(sensors);
         }
         else
         {
            // Cycle body segment.
            BodySegment bodySegment = bodySegments[i - 1];
            int[] sensors = new int[bodySegment.NUM_SENSORS];
            Segment priorSegment;
            if (i == 1)
            {
               priorSegment = headSegment;
            }
            else
            {
               priorSegment = bodySegments[i - 2];
            }
            x = priorSegment.x;
            y = priorSegment.y;
            int dir = -1;
            for (int j = 0; j < NUM_DIRECTIONS && dir == -1; j++)
            {
               switch (j)
               {
               case NORTHWEST:
                  if ((x == wx) && (y == ny)) { dir = NORTHWEST; }
                  break;

               case NORTH:
                  if ((x == nx) && (y == ny)) { dir = NORTH; }
                  break;

               case NORTHEAST:
                  if ((x == ex) && (y == ny)) { dir = NORTHEAST; }
                  break;

               case WEST:
                  if ((x == wx) && (y == wy)) { dir = WEST; }
                  break;

               case EAST:
                  if ((x == ex) && (y == ey)) { dir = EAST; }
                  break;

               case SOUTHWEST:
                  if ((x == wx) && (y == sy)) { dir = SOUTHWEST; }
                  break;

               case SOUTH:
                  if ((x == sx) && (y == sy)) { dir = SOUTH; }
                  break;

               case SOUTHEAST:
                  if ((x == ex) && (y == sy)) { dir = SOUTHEAST; }
                  break;
               }
            }
            if (dir == -1) { dir = CENTER; }
            sensors[0] = dir;
            sensors[1] = priorSegment.response;
            sensors[2] = bodySegment.response;
            bodySegment.cycle(sensors);

            // Mirror prior segment to avoid worm disintegration.
            if ((Math.abs(priorSegment.x2 - bodySegment.x2) > 1) ||
                (Math.abs(priorSegment.y2 - bodySegment.y2) > 1))
            {
               bodySegment.response = priorSegment.response;
               bodySegment.projectResponsePosition();
            }
         }
      }
      eventTime++;

      // Execute responses.
      for (int i = 0; i <= NUM_BODY_SEGMENTS; i++)
      {
         Segment segment;
         if (i == 0)
         {
            segment = headSegment;
         }
         else
         {
            segment = bodySegments[i - 1];
         }
         segment.setProjectedPosition();
      }
      placeWormOnAgar();
      return(false);
   }


   // Get metamorph DB response.
   int metamorphDBresponse(Morphognostic morphognostic, int segmentNumber)
   {
      HashMap < Integer, List < Metamorph >> metamorphs;
      if (segmentNumber == 0)
      {
         metamorphs = headMetamorphs;
      }
      else
      {
         metamorphs = bodyMetamorphs;
      }
      ArrayList<Integer> responses         = new ArrayList<Integer>();
      int                morphognosticHash = hashMorphognostic(morphognostic);
      List<Metamorph>    metamorphValues   = metamorphs.get(morphognosticHash);
      if (metamorphValues != null)
      {
         for (Metamorph m : metamorphValues)
         {
            responses.add(m.response);
         }
      }
      else
      {
         float dist = -1.0f;
         for (List<Metamorph> metamorphList : metamorphs.values())
         {
            for (Metamorph m : metamorphList)
            {
               float d = morphognostic.compare(m.morphognostic);
               if ((dist < 0.0f) || (d < dist))
               {
                  responses.clear();
                  responses.add(m.response);
                  dist = d;
               }
               else
               {
                  if (d == dist)
                  {
                     responses.add(m.response);
                  }
               }
            }
         }
      }
      int response = STAY;
      if (responses.size() > 0)
      {
         response = responses.get(random.nextInt(responses.size()));
      }
      return(response);
   }


   // Hash morphognostic.
   public int hashMorphognostic(Morphognostic morphognostic)
   {
      ArrayList<Float> densities = new ArrayList<Float>();
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
                     densities.add(s.typeDensities[d][j]);
                  }
               }
            }
         }
      }
      return(densities.hashCode());
   }


   // Get metamorph Weka neural network response.
   int metamorphWekaNNresponse(Morphognostic morphognostic, int segmentNumber)
   {
      if (segmentNumber == 0)
      {
         return(classifyHeadMorphognostic(morphognostic));
      }
      else
      {
         return(classifyBodyMorphognostic(morphognostic));
      }
   }


   // Get metamorph H2O neural network response.
   int metamorphH2ONNresponse(Morphognostic morphognostic, int segmentNumber)
   {
      int response = STAY;

      try {
         float[] p;
         if (segmentNumber == 0)
         {
            p = H2OheadPredict.predict(morphognostic2csv(morphognostic) + ",STAY");
         }
         else
         {
            p = H2ObodyPredict.predict(morphognostic2csv(morphognostic) + ",STAY");
         }
         float probability = -1.0f;
         for (int i = 0; i < H2OresponseLabels.length; i++)
         {
            if ((probability < 0.0f) || (p[i] > probability))
            {
               probability = p[i];
               response    = getResponseValue(H2OresponseLabels[i]);
            }
            else if (p[i] == probability)
            {
               if (random.nextBoolean())
               {
                  response = getResponseValue(H2OresponseLabels[i]);
               }
            }
         }
      }
      catch (Exception e) {
         System.err.println("H2O prediction failed: " + e.getMessage());
      }
      return(response);
   }


   // Wormsim response.
   int wormsimResponse(Segment segment)
   {
      int sx = segmentSimPositions[segment.number].x;
      int sy = segmentSimPositions[segment.number].y;

      int width  = Agar.GRID_SIZE.width;
      int height = Agar.GRID_SIZE.height;

      for (int i = 0; i < NUM_DIRECTIONS; i++)
      {
         int x = segment.x;
         int y = segment.y;
         switch (i)
         {
         case NORTHWEST:
            x--;
            if (x < 0) { x += width; }
            y = ((y + 1) % height);
            if ((x == sx) && (y == sy))
            {
               return(NORTHWEST);
            }
            break;

         case NORTH:
            y = ((y + 1) % height);
            if ((x == sx) && (y == sy))
            {
               return(NORTH);
            }
            break;

         case NORTHEAST:
            x = ((x + 1) % width);
            y = ((y + 1) % height);
            if ((x == sx) && (y == sy))
            {
               return(NORTHEAST);
            }
            break;

         case WEST:
            x--;
            if (x < 0) { x += width; }
            if ((x == sx) && (y == sy))
            {
               return(WEST);
            }
            break;

         case CENTER:
            if ((x == sx) && (y == sy))
            {
               return(STAY);
            }
            break;

         case EAST:
            x = ((x + 1) % width);
            if ((x == sx) && (y == sy))
            {
               return(EAST);
            }
            break;

         case SOUTHWEST:
            x--;
            if (x < 0) { x += width; }
            y--;
            if (y < 0) { y += height; }
            if ((x == sx) && (y == sy))
            {
               return(SOUTHWEST);
            }
            break;

         case SOUTH:
            y--;
            if (y < 0) { y += height; }
            if ((x == sx) && (y == sy))
            {
               return(SOUTH);
            }
            break;

         case SOUTHEAST:
            x = ((x + 1) % width);
            y--;
            if (y < 0) { y += height; }
            if ((x == sx) && (y == sy))
            {
               return(SOUTHEAST);
            }
            break;
         }
      }
      return(STAY);
   }


   // Initialize head metamorph Weka neural network.
   public void initHeadMetamorphWekaNN(Morphognostic morphognostic)
   {
      headMetamorphWekaNNattributeNames = new FastVector();
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
                     headMetamorphWekaNNattributeNames.addElement(new Attribute(i + "-" + x + "-" + y + "-" + d + "-" + j));
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
      headMetamorphWekaNNattributeNames.addElement(new Attribute("type", responseVals));
      headMetamorphWekaInstances = new Instances("head_metamorphs", headMetamorphWekaNNattributeNames, 0);
      headMetamorphWekaNN        = new MultilayerPerceptron();
   }


   // Initialize body metamorph Weka neural network.
   public void initBodyMetamorphWekaNN(Morphognostic morphognostic)
   {
      bodyMetamorphWekaNNattributeNames = new FastVector();
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
                     bodyMetamorphWekaNNattributeNames.addElement(new Attribute(i + "-" + x + "-" + y + "-" + d + "-" + j));
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
      bodyMetamorphWekaNNattributeNames.addElement(new Attribute("type", responseVals));
      bodyMetamorphWekaInstances = new Instances("body_metamorphs", bodyMetamorphWekaNNattributeNames, 0);
      bodyMetamorphWekaNN        = new MultilayerPerceptron();
   }


   // Create and train head metamorph neural network.
   public void createHeadMetamorphWekaNN() throws Exception
   {
      // Create instances.
      headMetamorphWekaInstances = new Instances("head_metamorphs", headMetamorphWekaNNattributeNames, 0);
      for (List<Metamorph> metamorphList : headMetamorphs.values())
      {
         for (Metamorph m : metamorphList)
         {
            headMetamorphWekaInstances.add(createInstance(headMetamorphWekaInstances, m));
         }
      }
      headMetamorphWekaInstances.setClassIndex(headMetamorphWekaInstances.numAttributes() - 1);

      // Create and train the neural network.
      MultilayerPerceptron mlp = new MultilayerPerceptron();
      headMetamorphWekaNN = mlp;
      mlp.setLearningRate(0.1);
      mlp.setMomentum(0.2);
      mlp.setTrainingTime(2000);
      mlp.setHiddenLayers("20");
      mlp.setOptions(Utils.splitOptions("-L 0.1 -M 0.2 -N 2000 -V 0 -S 0 -E 20 -H 20"));
      mlp.buildClassifier(headMetamorphWekaInstances);

      // Save training instances?
      if (saveMetamorphWekaInstances)
      {
         ArffSaver saver = new ArffSaver();
         saver.setInstances(headMetamorphWekaInstances);
         saver.setFile(new File("headMetamorphWekaInstances.arff"));
         saver.writeBatch();
      }

      // Save networks?
      if (saveMetamorphWekaNN)
      {
         Debug.saveToFile("headMetamorphWekaNN.dat", mlp);
      }

      // Evaluate the network.
      if (evaluateMetamorphWekaNN)
      {
         Evaluation eval = new Evaluation(headMetamorphWekaInstances);
         eval.evaluateModel(mlp, headMetamorphWekaInstances);
         System.out.println("Error rate=" + eval.errorRate());
         System.out.println(eval.toSummaryString());
      }
   }


   // Create and train body metamorph neural network.
   public void createBodyMetamorphWekaNN() throws Exception
   {
      // Create instances.
      bodyMetamorphWekaInstances = new Instances("body_metamorphs", bodyMetamorphWekaNNattributeNames, 0);
      for (List<Metamorph> metamorphList : bodyMetamorphs.values())
      {
         for (Metamorph m : metamorphList)
         {
            bodyMetamorphWekaInstances.add(createInstance(bodyMetamorphWekaInstances, m));
         }
      }
      bodyMetamorphWekaInstances.setClassIndex(bodyMetamorphWekaInstances.numAttributes() - 1);

      // Create and train the neural network.
      MultilayerPerceptron mlp = new MultilayerPerceptron();
      bodyMetamorphWekaNN = mlp;
      mlp.setLearningRate(0.1);
      mlp.setMomentum(0.2);
      mlp.setTrainingTime(2000);
      mlp.setHiddenLayers("20");
      mlp.setOptions(Utils.splitOptions("-L 0.1 -M 0.2 -N 2000 -V 0 -S 0 -E 20 -H 20"));
      mlp.buildClassifier(bodyMetamorphWekaInstances);

      // Save training instances?
      if (saveMetamorphWekaInstances)
      {
         ArffSaver saver = new ArffSaver();
         saver.setInstances(bodyMetamorphWekaInstances);
         saver.setFile(new File("bodyMetamorphWekaInstances.arff"));
         saver.writeBatch();
      }

      // Save networks?
      if (saveMetamorphWekaNN)
      {
         Debug.saveToFile("bodyMetamorphWekaNN.dat", mlp);
      }

      // Evaluate the network.
      if (evaluateMetamorphWekaNN)
      {
         Evaluation eval = new Evaluation(bodyMetamorphWekaInstances);
         eval.evaluateModel(mlp, bodyMetamorphWekaInstances);
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


   // Save head metamorph neural network training dataset.
   public void saveHeadMetamorphNNtrainingData() throws Exception
   {
      FileOutputStream output;

      try
      {
         output = new FileOutputStream(new File(HEAD_NN_DATASET_SAVE_FILE_NAME));
      }
      catch (Exception e)
      {
         throw new IOException("Cannot open output file " + HEAD_NN_DATASET_SAVE_FILE_NAME + ":" + e.getMessage());
      }
      PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(output)));
      boolean     header = true;
      for (List<Metamorph> metamorphList : headMetamorphs.values())
      {
         for (Metamorph m : metamorphList)
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
      }
      writer.flush();
      output.close();
   }


   // Save body metamorph neural network training dataset.
   public void saveBodyMetamorphNNtrainingData() throws Exception
   {
      FileOutputStream output;

      try
      {
         output = new FileOutputStream(new File(BODY_NN_DATASET_SAVE_FILE_NAME));
      }
      catch (Exception e)
      {
         throw new IOException("Cannot open output file " + BODY_NN_DATASET_SAVE_FILE_NAME + ":" + e.getMessage());
      }
      PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(output)));
      boolean     header = true;
      for (List<Metamorph> metamorphList : bodyMetamorphs.values())
      {
         for (Metamorph m : metamorphList)
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
      }
      writer.flush();
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


   // Use head metamorph Weka NN to classify morphognostic as a response.
   public int classifyHeadMorphognostic(Morphognostic morphognostic)
   {
      Metamorph metamorph = new Metamorph(morphognostic, STAY, getResponseName(STAY));

      //float[] responseProbabilities = new float[NUM_RESPONSES];
      int response = STAY;

      try
      {
         // Classify.
         Instance instance        = createInstance(headMetamorphWekaInstances, metamorph);
         int      predictionIndex = (int)headMetamorphWekaNN.classifyInstance(instance);

         // Get the predicted class label from the predictionIndex.
         String predictedClassLabel = headMetamorphWekaInstances.classAttribute().value(predictionIndex);
         response = Integer.parseInt(predictedClassLabel);

         // Get the prediction probability distribution.
         //double[] d = headMetamorphWekaNN.distributionForInstance(instance);
         //for (int i = 0; i < NUM_RESPONSES; i++)
         //{
         //responseProbabilities[i] = (float)d[i];
         //}
      }
      catch (Exception e)
      {
         System.err.println("Error classifying head morphognostic: " + e.getMessage());
      }
      return(response);
   }


   // Use body metamorph Weka NN to classify morphognostic as a response.
   public int classifyBodyMorphognostic(Morphognostic morphognostic)
   {
      Metamorph metamorph = new Metamorph(morphognostic, STAY, getResponseName(STAY));

      //float[] responseProbabilities = new float[NUM_RESPONSES];
      int response = STAY;

      try
      {
         // Classify.
         Instance instance        = createInstance(bodyMetamorphWekaInstances, metamorph);
         int      predictionIndex = (int)bodyMetamorphWekaNN.classifyInstance(instance);

         // Get the predicted class label from the predictionIndex.
         String predictedClassLabel = bodyMetamorphWekaInstances.classAttribute().value(predictionIndex);
         response = Integer.parseInt(predictedClassLabel);

         // Get the prediction probability distribution.
         //double[] d = bodyMetamorphWekaNN.distributionForInstance(instance);
         //for (int i = 0; i < NUM_RESPONSES; i++)
         //{
         //responseProbabilities[i] = (float)d[i];
         //}
      }
      catch (Exception e)
      {
         System.err.println("Error classifying body morphognostic: " + e.getMessage());
      }
      return(response);
   }


   // Direction names.
   public static String getDirectionName(int dir)
   {
      switch (dir)
      {
      case NORTHWEST:
         return("NORTHWEST");

      case NORTH:
         return("NORTH");

      case NORTHEAST:
         return("NORTHEAST");

      case WEST:
         return("WEST");

      case CENTER:
         return("CENTER");

      case EAST:
         return("EAST");

      case SOUTHWEST:
         return("SOUTHWEST");

      case SOUTH:
         return("SOUTH");

      case SOUTHEAST:
         return("SOUTHEAST");
      }
      return("");
   }


   // Response names.
   public static String getResponseName(int response)
   {
      String name = getDirectionName(response);

      if (name.equals("CENTER"))
      {
         name = "STAY";
      }
      return(name);
   }


   // Response value from name.
   public static int getResponseValue(String name)
   {
      if (name.equals("NORTHWEST"))
      {
         return(NORTHWEST);
      }

      if (name.equals("NORTH"))
      {
         return(NORTH);
      }

      if (name.equals("NORTHEAST"))
      {
         return(NORTHEAST);
      }

      if (name.equals("WEST"))
      {
         return(WEST);
      }

      if (name.equals("STAY"))
      {
         return(STAY);
      }

      if (name.equals("EAST"))
      {
         return(EAST);
      }

      if (name.equals("SOUTHWEST"))
      {
         return(SOUTHWEST);
      }

      if (name.equals("SOUTH"))
      {
         return(SOUTH);
      }

      if (name.equals("SOUTHEAST"))
      {
         return(SOUTHEAST);
      }

      return(-1);
   }
}
