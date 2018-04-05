// For conditions of distribution and use, see copyright notice in Main.java

// Worm dashboard.

package openworm.morphognosis.wormworx;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class WormDashboard extends JFrame
{
   private static final long serialVersionUID = 0L;

   // Components.
   DriverPanel driver;

   // Targets.
   Worm    worm;
   Display display;

   // Constructor.
   public WormDashboard(Worm worm, Display display)
   {
      this.worm    = worm;
      this.display = display;

      setTitle("Worm");
      addWindowListener(new WindowAdapter()
                        {
                           public void windowClosing(WindowEvent e) { close(); }
                        }
                        );
      JPanel basePanel = (JPanel)getContentPane();
      basePanel.setLayout(new BorderLayout());
      driver = new DriverPanel();
      basePanel.add(driver, BorderLayout.CENTER);
      pack();
      setBounds();
      setVisible(false);
      update();
   }


   void setBounds()
   {
      Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
      int       w   = getSize().width;
      int       h   = getSize().height;
      int       x   = (dim.width - w) / 2;
      int       y   = (dim.height - h) / 2;

      setBounds(x, y, 400, 125);
   }


   // Update dashboard.
   void update()
   {
      setDriverChoice(worm.driver);
   }


   // Open the dashboard.
   void open()
   {
      setVisible(true);
   }


   // Close the dashboard.
   void close()
   {
      setVisible(false);
   }


   // Get driver choice.
   int getDriverChoice()
   {
      return(driver.driverChoice.getSelectedIndex());
   }


   // Set driver choice.
   void setDriverChoice(int driverChoice)
   {
      driver.driverChoice.select(driverChoice);
   }


   // Driver panel.
   class DriverPanel extends JPanel implements ItemListener, ActionListener
   {
      private static final long serialVersionUID = 0L;

      // Components.
      Choice   driverChoice;
      JButton  moveUpButton;
      JButton  moveDownButton;
      JButton  moveLeftButton;
      JButton  moveRightButton;
      JButton  raiseSurfaceButton;
      JButton  lowerSurfaceButton;
      Checkbox trainNNcheck;
      JButton  saveNNdataButton;

      // Constructor.
      public DriverPanel()
      {
         setLayout(new BorderLayout());
         //setBorder(BorderFactory.createTitledBorder(
         //BorderFactory.createLineBorder(Color.black), "Driver"));
         JPanel driverPanel = new JPanel();
         driverPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
         add(driverPanel, BorderLayout.NORTH);
         driverPanel.add(new JLabel("Driver:"));
         driverChoice = new Choice();
         driverPanel.add(driverChoice);
         driverChoice.add("metamorphDB");
         driverChoice.add("metamorphWekaNN");
         driverChoice.add("metamorphH2ONN");
         driverChoice.add("wormsim");
         driverChoice.addItemListener(this);
         JPanel trainNNpanel = new JPanel();
         trainNNpanel.setLayout(new FlowLayout(FlowLayout.LEFT));
         add(trainNNpanel, BorderLayout.SOUTH);
         trainNNpanel.add(new JLabel("Train Weka NN:"));
         trainNNcheck = new Checkbox();
         trainNNcheck.setState(false);
         trainNNcheck.addItemListener(this);
         trainNNpanel.add(trainNNcheck);
         saveNNdataButton = new JButton("Save NN dataset");
         saveNNdataButton.addActionListener(this);
         trainNNpanel.add(saveNNdataButton);
      }


      // Choice listener.
      public void itemStateChanged(ItemEvent evt)
      {
         Object source = evt.getSource();

         if (source instanceof Choice && ((Choice)source == driverChoice))
         {
            worm.setDriver(driverChoice.getSelectedIndex());
            return;
         }
         if (source instanceof Checkbox && ((Checkbox)source == trainNNcheck))
         {
            if (trainNNcheck.getState())
            {
               try
               {
                  worm.createHeadMetamorphWekaNN();
               }
               catch (Exception e)
               {
                  display.controls.messageText.setText("Cannot train head metamorph Weka NN: " + e.getMessage());
               }
               try
               {
                  worm.createBodyMetamorphWekaNN();
               }
               catch (Exception e)
               {
                  display.controls.messageText.setText("Cannot train body metamorph Weka NN: " + e.getMessage());
               }
               trainNNcheck.setState(false);
            }
            return;
         }
      }


      // Step button listener.
      public void actionPerformed(ActionEvent evt)
      {
         if (evt.getSource() == (Object)saveNNdataButton)
         {
            try
            {
               worm.saveHeadMetamorphNNtrainingData();
            }
            catch (Exception e)
            {
               display.controls.messageText.setText("Cannot save head metamorph NN dataset: " + e.getMessage());
               return;
            }
            try
            {
               worm.saveBodyMetamorphNNtrainingData();
            }
            catch (Exception e)
            {
               display.controls.messageText.setText("Cannot save body metamorph NN dataset: " + e.getMessage());
               return;
            }
            display.controls.messageText.setText("Metamorph NN datasets saved in files " +
                                                 Worm.HEAD_NN_DATASET_SAVE_FILE_NAME + " and " + Worm.BODY_NN_DATASET_SAVE_FILE_NAME);
            return;
         }
      }
   }
}
