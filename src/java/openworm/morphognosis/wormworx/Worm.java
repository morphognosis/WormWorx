// For conditions of distribution and use, see copyright notice in Main.java

// C. elegans worm: a morphognosis organism.

package openworm.morphognosis.wormworx;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.io.*;
import java.security.SecureRandom;
import java.util.ArrayList;
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

   // Driver.
   public enum DRIVER_TYPE
   {
      METAMORPH_DB(0),
      METAMORPH_NN(1),
      WORMSIM(2);

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

   // Found food?
   boolean foundFood;

   // Simulator synchronization.
   Object wormsimLock;

   // Worm segment.
   public class Segment
   {
      // Number.
      public int number;

      // Location.
      public int x, y;
      public int x2, y2;

      // Sensors.
      public float[] sensors;

      // Response.
      public int response;

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
      }
      public ArrayList<Event> events;
      public int              eventTime;

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

         this.x  = x2 = segmentSimPositions[number].x;
         this.y  = y2 = segmentSimPositions[number].y;
         sensors = new float[NUM_SENSORS];
         for (int i = 0; i < NUM_SENSORS; i++)
         {
            sensors[i] = 0.0f;
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
         events    = new ArrayList<Event>();
         eventTime = 0;
      }


      public void reset()
      {
         x = x2;
         y = y2;
         for (int i = 0; i < NUM_SENSORS; i++)
         {
            sensors[i] = 0.0f;
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
      public int cycle(float[] sensors)
      {
         // Update morphognostic.
         int[] values = new int[NUM_SENSORS];
         for (int i = 0; i < NUM_SENSORS; i++)
         {
            this.sensors[i] = sensors[i];
            values[i]       = (int)sensors[i];
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
            response = metamorphDBresponse(morphognostic);
         }
         else if (driver == DRIVER_TYPE.METAMORPH_NN.getValue())
         {
            response = metamorphNNresponse(morphognostic);
         }
         else if (driver == DRIVER_TYPE.WORMSIM.getValue())
         {
            response = wormsimResponse(this);
         }
         else
         {
            response = STAY;
         }

         // Update metamorphs.
         Metamorph metamorph = new Metamorph(morphognostic.clone(), response);
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

         eventTime++;
         return(response);
      }


      public void save(FileOutputStream output) throws IOException
      {
         PrintWriter writer = new PrintWriter(new OutputStreamWriter(output));

         Utility.saveInt(writer, x);
         Utility.saveInt(writer, y);
         Utility.saveInt(writer, x2);
         Utility.saveInt(writer, y2);
         Utility.saveInt(writer, maxEventAge);
         morphognostic.save(output);
         writer.flush();
      }


      public void load(FileInputStream input) throws IOException
      {
         DataInputStream reader = new DataInputStream(input);

         x             = Utility.loadInt(reader);
         y             = Utility.loadInt(reader);
         x2            = Utility.loadInt(reader);
         y2            = Utility.loadInt(reader);
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
   public FastVector           metamorphNNattributeNames;
   public Instances            metamorphInstances;
   MultilayerPerceptron        metamorphNN;
   public static final boolean saveMetamorphInstances = false;
   public static final boolean saveMetamorphNN        = false;
   public static final boolean evaluateMetamorphNN    = true;

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
      initMetamorphNN(morphognostic);
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
      initMetamorphNN(morphognostic);
   }


   // Initialize.
   void init(Agar agar, int randomSeed)
   {
      this.agar       = agar;
      this.randomSeed = randomSeed;
      random          = new SecureRandom();
      random.setSeed(randomSeed);
      driver = DRIVER_TYPE.WORMSIM.getValue();
      Wormsim.init();
      wormBody            = new double[NBAR * 3];
      wormVerts           = new Point2D.Double[NBAR];
      segmentSimPositions = new Point[NUM_SEGMENTS];
      getSegmentSimPositions();
      foundFood   = false;
      wormsimLock = new Object();
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
         agar.cells[segment.x][segment.y][Agar.WORM_CELL_INDEX] = Agar.WORM_SEGMENT_VALUE;
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
      metamorphs.clear();
      int n = Utility.loadInt(reader);
      for (int i = 0; i < n; i++)
      {
         metamorphs.add(Metamorph.load(input));
      }
      Morphognostic morphognostic = segments[0].morphognostic;
      initMetamorphNN(morphognostic);
      driver = Utility.loadInt(reader);
      setDriver(driver);
   }


   // Step.
   // Return true if food found.
   boolean step()
   {
      int width, height;

      int[] responses;

      // Check if food found.
      float sx = (float)agar.saltyX[agar.currentSalty] / Agar.CELL_WIDTH;
      float sy = (float)agar.saltyY[agar.currentSalty] / Agar.CELL_HEIGHT;
      sy = Agar.GRID_SIZE.height - sy;
      float dist = (float)Math.sqrt(Math.pow((sx - (float)segments[0].x), 2) +
                                    Math.pow((sy - segments[0].y), 2));
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
         switch (agar.currentSalty)
         {
         case Agar.RED_FOOD:
            Wormsim.overrideSMBmuscleAmplifiers(1.0, 1.1);
            break;

         case Agar.GREEN_FOOD:
            Wormsim.overrideSMBmuscleAmplifiers(1.0, 1.0);
            break;

         case Agar.BLUE_FOOD:
            Wormsim.overrideSMBmuscleAmplifiers(1.5, 1.0);
            break;
         }
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
      responses       = new int[NUM_SEGMENTS];
      for (int i = 0; i < NUM_SEGMENTS; i++)
      {
         Segment segment = segments[i];

         // Update landmarks.
         segment.landmarkMap[segment.x][segment.y] = true;

         // Initialize sensors.
         for (int j = 0; j < 11; j++)
         {
            int n = i - (j + 1);
            if (n < 0) { n += NUM_SEGMENTS; }
            sensors[j * 2]       = segments[n].x - segment.x;
            sensors[(j * 2) + 1] = segments[n].y - segment.y;
         }
         for (int j = 0; j < 4; j++)
         {
            int x = segment.x;
            int y = segment.y;
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
         }

         // Get response.
         responses[i] = segment.cycle(sensors);
      }

      // Execute responses.
      for (int i = 0; i < NUM_SEGMENTS; i++)
      {
         Segment segment = segments[i];

         int ux, uy, dx, dy, lx, ly, rx, ry;
         ux = dx = lx = rx = 0;
         uy = dy = ly = ry = 0;
         for (int j = 0; j < 4; j++)
         {
            int x = segment.x;
            int y = segment.y;
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
         switch (responses[i])
         {
         case MOVE_NW:
            segment.x = lx;
            segment.y = uy;
            break;

         case MOVE_NORTH:
            segment.x = ux;
            segment.y = uy;
            break;

         case MOVE_NE:
            segment.x = rx;
            segment.y = uy;
            break;

         case MOVE_WEST:
            segment.x = lx;
            segment.y = ly;
            break;

         case STAY:
            break;

         case MOVE_EAST:
            segment.x = rx;
            segment.y = ry;
            break;

         case MOVE_SW:
            segment.x = lx;
            segment.y = dy;
            break;

         case MOVE_SOUTH:
            segment.x = dx;
            segment.y = dy;
            break;

         case MOVE_SE:
            segment.x = rx;
            segment.y = dy;
            break;
         }
      }
      placeWormOnAgar();
      return(false);
   }


   // Get metamorph DB response.
   int metamorphDBresponse(Morphognostic morphognostic)
   {
      int       response  = STAY;
      Metamorph metamorph = null;
      float     d         = 0.0f;
      float     d2;

      for (Metamorph m : metamorphs)
      {
         d2 = morphognostic.compare(m.morphognostic);
         if ((metamorph == null) || (d2 < d))
         {
            d         = d2;
            metamorph = m;
         }
         else
         {
            if (d2 == d)
            {
               if (random.nextBoolean())
               {
                  d         = d2;
                  metamorph = m;
               }
            }
         }
      }
      if (metamorph != null)
      {
         response = metamorph.response;
      }
      return(response);
   }


   // Get metamorph neural network response.
   int metamorphNNresponse(Morphognostic morphognostic)
   {
      return(classifyMorphognostic(morphognostic));
   }


   // Wormsim response.
   int wormsimResponse(Segment segment)
   {
      int sx     = segmentSimPositions[segment.number].x;
      int sy     = segmentSimPositions[segment.number].y;
      int width  = Agar.GRID_SIZE.width;
      int height = Agar.GRID_SIZE.height;

      for (int i = 0; i < 9; i++)
      {
         int x = segment.x;
         int y = segment.y;
         switch (i)
         {
         case 0:
            x--;
            if (x < 0) { x += width; }
            y = ((y + 1) % height);
            if ((x == sx) && (y == sy))
            {
               return(MOVE_NW);
            }
            break;

         case 1:
            y = ((y + 1) % height);
            if ((x == sx) && (y == sy))
            {
               return(MOVE_NORTH);
            }
            break;

         case 2:
            x = ((x + 1) % width);
            y = ((y + 1) % height);
            if ((x == sx) && (y == sy))
            {
               return(MOVE_NE);
            }
            break;

         case 3:
            x--;
            if (x < 0) { x += width; }
            if ((x == sx) && (y == sy))
            {
               return(MOVE_WEST);
            }
            break;

         case 4:
            if ((x == sx) && (y == sy))
            {
               return(STAY);
            }
            break;

         case 5:
            x = ((x + 1) % width);
            if ((x == sx) && (y == sy))
            {
               return(MOVE_EAST);
            }
            break;

         case 6:
            x--;
            if (x < 0) { x += width; }
            y--;
            if (y < 0) { y += height; }
            if ((x == sx) && (y == sy))
            {
               return(MOVE_SW);
            }
            break;

         case 7:
            y--;
            if (y < 0) { y += height; }
            if ((x == sx) && (y == sy))
            {
               return(MOVE_SOUTH);
            }
            break;

         case 8:
            x = ((x + 1) % width);
            y--;
            if (y < 0) { y += height; }
            if ((x == sx) && (y == sy))
            {
               return(MOVE_SE);
            }
            break;
         }
      }
      return(STAY);
   }


   // Initialize metamorph neural network.
   public void initMetamorphNN(Morphognostic morphognostic)
   {
      metamorphNNattributeNames = new FastVector();
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
                     metamorphNNattributeNames.addElement(new Attribute(i + "-" + x + "-" + y + "-" + d + "-" + j));
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
      metamorphNNattributeNames.addElement(new Attribute("type", responseVals));
      metamorphInstances = new Instances("metamorphs", metamorphNNattributeNames, 0);
      metamorphNN        = new MultilayerPerceptron();
   }


   // Create and train metamorph neural network.
   public void createMetamorphNN() throws Exception
   {
      // Create instances.
      metamorphInstances = new Instances("metamorphs", metamorphNNattributeNames, 0);
      for (Metamorph m : metamorphs)
      {
         metamorphInstances.add(createInstance(metamorphInstances, m));
      }
      metamorphInstances.setClassIndex(metamorphInstances.numAttributes() - 1);

      // Create and train the neural network.
      MultilayerPerceptron mlp = new MultilayerPerceptron();
      metamorphNN = mlp;
      mlp.setLearningRate(0.1);
      mlp.setMomentum(0.2);
      mlp.setTrainingTime(2000);
      mlp.setHiddenLayers("20");
      mlp.setOptions(Utils.splitOptions("-L 0.1 -M 0.2 -N 2000 -V 0 -S 0 -E 20 -H 20"));
      mlp.buildClassifier(metamorphInstances);

      // Save training instances?
      if (saveMetamorphInstances)
      {
         ArffSaver saver = new ArffSaver();
         saver.setInstances(metamorphInstances);
         saver.setFile(new File("metamorphInstances.arff"));
         saver.writeBatch();
      }

      // Save networks?
      if (saveMetamorphNN)
      {
         Debug.saveToFile("metamorphNN.dat", mlp);
      }

      // Evaluate the network.
      if (evaluateMetamorphNN)
      {
         Evaluation eval = new Evaluation(metamorphInstances);
         eval.evaluateModel(mlp, metamorphInstances);
         System.out.println("Error rate=" + eval.errorRate());
         System.out.println(eval.toSummaryString());
      }
   }


   // Create metamorph NN instance.
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


   // Use metamorph NN to classify morphognostic as a response.
   public int classifyMorphognostic(Morphognostic morphognostic)
   {
      int       response  = STAY;
      Metamorph metamorph = new Metamorph(morphognostic, response);

      try
      {
         // Classify.
         Instance instance        = createInstance(metamorphInstances, metamorph);
         int      predictionIndex = (int)metamorphNN.classifyInstance(instance);

         // Get the predicted class label from the predictionIndex.
         String predictedClassLabel = metamorphInstances.classAttribute().value(predictionIndex);
         response = Integer.parseInt(predictedClassLabel);

         // Get the prediction probability distribution.
         //double[] predictionDistribution = metamorphNN.distributionForInstance(instance);

         // Get morphognostic distance from prediction probability.
         //float dist = (1.0f - (float)predictionDistribution[predictionIndex]);
      }
      catch (Exception e)
      {
         System.err.println("Error classifying morphognostic:");
         e.printStackTrace();
      }
      return(response);
   }
}
