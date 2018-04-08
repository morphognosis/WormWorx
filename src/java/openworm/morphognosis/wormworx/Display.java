// For conditions of distribution and use, see copyright notice in Main.java

// Display.

package openworm.morphognosis.wormworx;

import java.awt.*;
import java.awt.event.*;
import java.security.SecureRandom;
import javax.swing.*;
import javax.swing.event.*;

public class Display extends JFrame
{
   private static final long serialVersionUID = 0L;

   // Agar.
   Agar agar;

   // Worm.
   Worm worm;

   // Worm dashboards.
   WormDashboard wormDashboard;
   WormSegmentDashboard[] wormSegmentDashboards;

   // Worm display.
   WormDisplay wormDisplay;

   // Controls.
   WormControls controls;

   // Step frequency (ms).
   static final int MIN_STEP_DELAY = 0;
   static final int MAX_STEP_DELAY = 1000;
   int              stepDelay      = MAX_STEP_DELAY;

   // Quit.
   boolean quit;

   // Random numbers.
   SecureRandom random;
   int          randomSeed;

   // Constructor.
   public Display(Agar agar, Worm worm)
   {
      this.agar = agar;
      this.worm = worm;

      // Random numbers.
      randomSeed = worm.randomSeed;
      random     = new SecureRandom();
      random.setSeed(randomSeed);

      // Create worm dashboards.
      wormDashboard         = new WormDashboard(worm, this);
      wormSegmentDashboards = new WormSegmentDashboard[Worm.NUM_BODY_SEGMENTS + 1];
      for (int i = 0; i <= Worm.NUM_BODY_SEGMENTS; i++)
      {
         if (i == 0)
         {
            wormSegmentDashboards[i] = new WormSegmentDashboard(worm.headSegment, this);
         }
         else
         {
            wormSegmentDashboards[i] = new WormSegmentDashboard(worm.bodySegments[i - 1], this);
         }
      }

      // Set up display.
      setTitle("C. elegans locomotion and foraging");
      addWindowListener(new WindowAdapter()
                        {
                           public void windowClosing(WindowEvent e)
                           {
                              close();
                              quit = true;
                           }
                        }
                        );
      setBounds(0, 0, Agar.SIZE.width, (int)((float)Agar.SIZE.height * 1.25f));
      JPanel basePanel = (JPanel)getContentPane();
      basePanel.setLayout(new BorderLayout());

      // Create display.
      Dimension displaySize = new Dimension(Agar.SIZE.width, Agar.SIZE.height);
      wormDisplay = new WormDisplay(displaySize);
      basePanel.add(wormDisplay, BorderLayout.NORTH);

      // Create controls.
      controls = new WormControls();
      basePanel.add(controls, BorderLayout.SOUTH);

      // Make display visible.
      pack();
      setCenterLocation();
      setVisible(true);
   }


   void setCenterLocation()
   {
      Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
      int       w   = getSize().width;
      int       h   = getSize().height;
      int       x   = (dim.width - w) / 2;
      int       y   = (dim.height - h) / 2;

      setLocation(x, y);
   }


   // Close.
   void close()
   {
      wormDashboard.close();
      setVisible(false);
   }


   // Update display.
   private int timer = 0;
   public void update()
   {
      if (quit) { return; }

      // Update worm dashboards.
      wormDashboard.update();
      for (int i = 0; i <= Worm.NUM_BODY_SEGMENTS; i++)
      {
         wormSegmentDashboards[i].update();
      }

      // Update display.
      wormDisplay.update();

      // Timer loop: count down delay by 1ms.
      for (timer = stepDelay; timer > 0 && !quit; )
      {
         try
         {
            Thread.sleep(1);
         }
         catch (InterruptedException e) {
            break;
         }

         wormDisplay.update();

         if (stepDelay < MAX_STEP_DELAY)
         {
            timer--;
         }
      }
   }


   // Set step delay.
   void setStepDelay(int delay)
   {
      stepDelay = timer = delay;
   }


   // Step.
   void step()
   {
      setStepDelay(MAX_STEP_DELAY);
      controls.speedSlider.setValue(MAX_STEP_DELAY);
      timer = 0;
   }


   // Set message
   void setMessage(String message)
   {
      if (message == null)
      {
         controls.messageText.setText("");
      }
      else
      {
         controls.messageText.setText(message);
      }
   }


   // Worm display.
   public class WormDisplay extends Canvas
   {
      private static final long serialVersionUID = 0L;

      // Buffered display.
      private Dimension canvasSize;
      private Graphics  graphics;
      private Image     image;
      private Graphics  imageGraphics;

      // Constructor.
      public WormDisplay(Dimension canvasSize)
      {
         // Configure canvas.
         this.canvasSize = canvasSize;
         setBounds(0, 0, canvasSize.width, canvasSize.height);
         addMouseListener(new CanvasMouseListener());
         addMouseMotionListener(new CanvasMouseMotionListener());
      }


      // Update display.
      synchronized void update()
      {
         int x, y, x2, y2, width, height;

         if (quit)
         {
            return;
         }

         if (graphics == null)
         {
            graphics      = getGraphics();
            image         = createImage(canvasSize.width, canvasSize.height);
            imageGraphics = image.getGraphics();
         }

         if (graphics == null)
         {
            return;
         }

         // Clear display.
         imageGraphics.setColor(Color.white);
         imageGraphics.fillRect(0, 0, canvasSize.width, canvasSize.height);

         width  = Agar.GRID_SIZE.width;
         height = Agar.GRID_SIZE.height;

         // Draw food.
         if (agar.currentFood != -1)
         {
            int r, g, b;
            r = g = b = 0;
            switch (agar.currentFood)
            {
            case 0:
               r = 255;
               break;

            case 1:
               g = 255;
               break;

            case 2:
               b = 255;
               break;
            }
            imageGraphics.setColor(new Color(r, g, b, 255));
            x2 = (int)(agar.foodX[agar.currentFood] / Agar.CELL_WIDTH);
            x2 = (int)((float)x2 * Agar.CELL_WIDTH);
            y2 = height - (int)(agar.foodY[agar.currentFood] / Agar.CELL_HEIGHT) - 1;
            y2 = (int)((float)y2 * Agar.CELL_HEIGHT);
            imageGraphics.fillRect(x2, y2, (int)Agar.CELL_WIDTH, (int)Agar.CELL_HEIGHT);
            for (x = 0; x < Agar.GRID_SIZE.width; x++)
            {
               for (y = 0; y < Agar.GRID_SIZE.height; y++)
               {
                  float d = agar.foodCells[x][y];
                  imageGraphics.setColor(new Color(r, g, b, (int)(255.0f * (1.0f / ((0.1f * d) + 1.0f)))));
                  x2 = (int)((float)x * Agar.CELL_WIDTH);
                  y2 = (int)((float)(height - y - 1) * Agar.CELL_HEIGHT);
                  imageGraphics.fillRect(x2, y2, (int)Agar.CELL_WIDTH, (int)Agar.CELL_HEIGHT);
               }
            }
         }

         // Draw worm.
         imageGraphics.setColor(Color.gray);
         for (int i = 0; i <= Worm.NUM_BODY_SEGMENTS; i++)
         {
            if (i == 0)
            {
               x = worm.headSegment.x;
               y = worm.headSegment.y;
            }
            else
            {
               x = worm.bodySegments[i - 1].x;
               y = worm.bodySegments[i - 1].y;
            }
            x2 = (int)(Agar.CELL_WIDTH * (float)x);
            y2 = (int)(Agar.CELL_HEIGHT * (float)(height - (y + 1)));
            imageGraphics.fillRect(x2, y2, (int)Agar.CELL_WIDTH, (int)Agar.CELL_HEIGHT);
         }

         // Draw grid.
         imageGraphics.setColor(Color.black);
         y2 = canvasSize.height;
         for (x = 1, x2 = (int)Agar.CELL_WIDTH; x < width;
              x++, x2 = (int)(Agar.CELL_WIDTH * (float)x))
         {
            imageGraphics.drawLine(x2, 0, x2, y2);
         }
         x2 = canvasSize.width;
         for (y = 1, y2 = (int)Agar.CELL_HEIGHT; y < height;
              y++, y2 = (int)(Agar.CELL_HEIGHT * (float)y))
         {
            imageGraphics.drawLine(0, y2, x2, y2);
         }

         // Refresh display.
         graphics.drawImage(image, 0, 0, this);
      }


      // Canvas mouse listener.
      class CanvasMouseListener extends MouseAdapter
      {
         // Mouse pressed.
         public void mousePressed(MouseEvent evt)
         {
            int x, y, cx, cy;
            int width  = Agar.GRID_SIZE.width;
            int height = Agar.GRID_SIZE.height;

            // Selecting worm segment?
            x = (int)((double)evt.getX() / Agar.CELL_WIDTH);
            y = height - (int)((double)evt.getY() / Agar.CELL_HEIGHT) - 1;
            if ((x >= 0) && (x < width) &&
                (y >= 0) && (y < height))
            {
               boolean segmentSelected = false;
               for (int i = 0; i <= Worm.NUM_BODY_SEGMENTS; i++)
               {
                  if (i == 0)
                  {
                     cx = worm.headSegment.x;
                     cy = worm.headSegment.y;
                  }
                  else
                  {
                     cx = worm.bodySegments[i - 1].x;
                     cy = worm.bodySegments[i - 1].y;
                  }
                  if ((cx == x) && (cy == y))
                  {
                     segmentSelected = true;
                     if (wormSegmentDashboards[i].isVisible())
                     {
                        wormSegmentDashboards[i].close();
                     }
                     else
                     {
                        wormSegmentDashboards[i].open();
                     }
                  }
               }
               if (!segmentSelected)
               {
                  for (int i = 0; i <= Worm.NUM_BODY_SEGMENTS; i++)
                  {
                     if (wormSegmentDashboards[i].isVisible())
                     {
                        wormSegmentDashboards[i].close();
                     }
                  }
                  wormDashboard.close();
               }
               else
               {
                  boolean segmentVisible = false;
                  for (int i = 0; i <= Worm.NUM_BODY_SEGMENTS; i++)
                  {
                     if (wormSegmentDashboards[i].isVisible())
                     {
                        segmentVisible = true;
                        break;
                     }
                  }
                  if (segmentVisible)
                  {
                     wormDashboard.open();
                  }
                  else
                  {
                     wormDashboard.close();
                  }
               }
            }

            // Refresh display.
            update();
         }
      }
   }

   // Canvas mouse motion listener.
   class CanvasMouseMotionListener extends MouseMotionAdapter
   {
      // Mouse dragged.
      public void mouseDragged(MouseEvent evt)
      {
      }
   }

   // Control panel.
   class WormControls extends JPanel implements ActionListener, ChangeListener
   {
      private static final long serialVersionUID = 0L;

      // Components.
      JButton    resetButton;
      JLabel     stepCounter;
      JSlider    speedSlider;
      JButton    stepButton;
      JTextField messageText;
      int        steps;

      // Constructor.
      WormControls()
      {
         setLayout(new BorderLayout());
         setBorder(BorderFactory.createRaisedBevelBorder());
         JPanel panel = new JPanel();
         resetButton = new JButton("Reset");
         resetButton.addActionListener(this);
         panel.add(resetButton);
         panel.add(new JLabel("Speed:   Fast", Label.RIGHT));
         speedSlider = new JSlider(JSlider.HORIZONTAL, MIN_STEP_DELAY,
                                   MAX_STEP_DELAY, MAX_STEP_DELAY);
         speedSlider.addChangeListener(this);
         panel.add(speedSlider);
         panel.add(new JLabel("Stop", Label.LEFT));
         stepButton = new JButton("Step");
         stepButton.addActionListener(this);
         panel.add(stepButton);
         stepCounter = new JLabel("Steps: 0");
         panel.add(stepCounter);
         add(panel, BorderLayout.NORTH);
         panel       = new JPanel();
         messageText = new JTextField("", 40);
         panel.add(messageText);
         add(panel, BorderLayout.SOUTH);
         steps = 0;
      }


      // Update step counter display
      void updateStepCounter()
      {
         steps++;
         stepCounter.setText("Steps: " + steps);
      }


      // Speed slider listener.
      public void stateChanged(ChangeEvent evt)
      {
         setStepDelay(speedSlider.getValue());
      }


      // Step button listener.
      public void actionPerformed(ActionEvent evt)
      {
         // Reset?
         if (evt.getSource() == (Object)resetButton)
         {
            setStepDelay(MAX_STEP_DELAY);
            speedSlider.setValue(MAX_STEP_DELAY);
            steps = 0;
            stepCounter.setText("Steps: 0");
            random = new SecureRandom();
            random.setSeed(randomSeed);
            worm.reset();
            wormDashboard.update();
            return;
         }

         // Step?
         if (evt.getSource() == (Object)stepButton)
         {
            step();

            return;
         }
      }
   }
}
