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
      String sensorsString = "";

      if (wormSegment.number == 0)
      {
         sensorsString = Worm.getDirectionName(wormSegment.sensors[0]);
      }
      else
      {
         sensorsString = Worm.getResponseName(wormSegment.sensors[0]);
      }
      for (int i = 1; i < wormSegment.sensors.length; i++)
      {
         sensorsString += ("," + Worm.getResponseName(wormSegment.sensors[i]));
      }
      setSensors(sensorsString);
      setResponse(Worm.getResponseName(wormSegment.response));
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


   // Set sensors display.
   void setSensors(String sensorsString)
   {
      sensorsResponse.sensorsText.setText(sensorsString);
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
      JTextField sensorsText;
      JTextField responseText;

      // Constructor.
      public SensorsResponsePanel()
      {
         setLayout(new BorderLayout());
         setBorder(BorderFactory.createTitledBorder(
                      BorderFactory.createLineBorder(Color.black),
                      "Sensors/Response"));
         JPanel sensorsPanel = new JPanel();
         sensorsPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
         add(sensorsPanel, BorderLayout.NORTH);
         sensorsPanel.add(new JLabel("Sensors:"));
         sensorsText = new JTextField(20);
         sensorsText.setEditable(false);
         sensorsPanel.add(sensorsText);
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
