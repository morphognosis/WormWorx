// For conditions of distribution and use, see copyright notice in Main.java

// Worm segment dashboard.

package openworm.morphognosis.wormworx;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import morphognosis.MorphognosticDisplay;

public class WormSegmentDashboard extends JFrame
{
   private static final long serialVersionUID = 0L;

   // Components.
   SensorsResponsePanel sensorsResponse;
   MorphognosticDisplay morphognostic;

   // Targets.
   Worm.Segment wormSegment;
   Display      display;

   // Constructor.
   public WormSegmentDashboard(Worm.Segment wormSegment, Display display)
   {
      this.wormSegment = wormSegment;
      this.display     = display;

      setTitle("Worm segment " + wormSegment.number);
      addWindowListener(new WindowAdapter()
                        {
                           public void windowClosing(WindowEvent e) { close(); }
                        }
                        );
      JPanel basePanel = (JPanel)getContentPane();
      basePanel.setLayout(new BorderLayout());
      sensorsResponse = new SensorsResponsePanel();
      basePanel.add(sensorsResponse, BorderLayout.NORTH);
      morphognostic = new MorphognosticDisplay(0, wormSegment.morphognostic);
      basePanel.add(morphognostic, BorderLayout.SOUTH);
      pack();
      setCenterLocation();
      setVisible(false);
      update();
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


   // Update dashboard.
   void update()
   {
      String neighborsString = "";
      String saltString      = "";

      for (int i = 0; i < 11; i++)
      {
         neighborsString += wormSegment.sensors[(i * 2)] + "/" + wormSegment.sensors[(i * 2) + 1];
         if (i < 10)
         {
            neighborsString += ",";
         }
      }
      setNeighbors(neighborsString);
      saltString = wormSegment.sensors[22] + "," + wormSegment.sensors[23] + "," +
                   wormSegment.sensors[24] + "," + wormSegment.sensors[25];
      setSalt(saltString);
      switch (wormSegment.responses[0])
      {
      case Worm.MOVE_NW:
         setResponse("Move NW");
         break;

      case Worm.MOVE_NORTH:
         setResponse("Move North");
         break;

      case Worm.MOVE_NE:
         setResponse("Move NE");
         break;

      case Worm.MOVE_WEST:
         setResponse("Move West");
         break;

      case Worm.STAY:
         setResponse("Stay");
         break;

      case Worm.MOVE_EAST:
         setResponse("Move East");
         break;

      case Worm.MOVE_SW:
         setResponse("Move SW");
         break;

      case Worm.MOVE_SOUTH:
         setResponse("Move South");
         break;

      case Worm.MOVE_SE:
         setResponse("Move SE");
         break;
      }
   }


   // Open the dashboard.
   void open()
   {
      setVisible(true);
   }


   // Close the dashboard.
   void close()
   {
      morphognostic.close();
      setVisible(false);
   }


   // Set neighbors display.
   void setNeighbors(String neighborsString)
   {
      sensorsResponse.neighborsText.setText(neighborsString);
   }


   // Set salt display.
   void setSalt(String saltString)
   {
      sensorsResponse.saltText.setText(saltString);
   }


   // Set response display.
   void setResponse(String responseString)
   {
      sensorsResponse.responseText.setText(responseString);
   }


   // Sensors/Response panel.
   class SensorsResponsePanel extends JPanel
   {
      private static final long serialVersionUID = 0L;

      // Components.
      JTextField neighborsText;
      JTextField saltText;
      JTextField responseText;

      // Constructor.
      public SensorsResponsePanel()
      {
         setLayout(new BorderLayout());
         setBorder(BorderFactory.createTitledBorder(
                      BorderFactory.createLineBorder(Color.black),
                      "Sensors/Response"));
         JPanel neighborsPanel = new JPanel();
         neighborsPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
         add(neighborsPanel, BorderLayout.NORTH);
         neighborsPanel.add(new JLabel("Neighbors:"));
         neighborsText = new JTextField(50);
         neighborsText.setEditable(false);
         neighborsPanel.add(neighborsText);
         JPanel saltPanel = new JPanel();
         saltPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
         add(saltPanel, BorderLayout.CENTER);
         saltPanel.add(new JLabel("Salt:"));
         saltText = new JTextField(20);
         saltText.setEditable(false);
         saltPanel.add(saltText);
         JPanel responsePanel = new JPanel();
         responsePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
         add(responsePanel, BorderLayout.SOUTH);
         responsePanel.add(new JLabel("Response:"));
         responseText = new JTextField(10);
         responseText.setEditable(false);
         responsePanel.add(responseText);
      }
   }
}
