package net.skycore.client.module.misc;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Robot;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.Minecraft;
import net.skycore.client.SkycoreClient;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.util.SliderSetting;

public final class Top4ikModule extends ClientModule {
   private volatile Top4ikModule.MirrorPanel panel;
   private final SliderSetting quality;
   private JFrame window;
   private final SliderSetting fps = this.add(new SliderSetting("FPS", 15.0, 1.0, 30.0, 1.0));
   public static final Top4ikModule INSTANCE = new Top4ikModule();
   private ScheduledExecutorService captureWorker;
   private volatile int captureFps;
   private volatile int captureHeight;
   private final AtomicLong generation;

   @Override
   protected void onDisable() {
      this.generation.incrementAndGet();
      if (this.captureWorker != null) {
         this.captureWorker.shutdownNow();
         this.captureWorker = null;
      }

      Runnable runnable = () -> {
         this.panel = null;
         if (this.window != null) {
            this.window.dispose();
            this.window = null;
         }
      };
      SwingUtilities.invokeLater(runnable);
   }

   private void captureFrame(long timestamp, Robot robot, Rectangle rectangle, AtomicBoolean atomicBoolean, long[] longValues) {
      if (this.generation.get() == timestamp && this.panel != null) {
         long nowNanos = System.nanoTime();
         if (nowNanos >= longValues[0] && atomicBoolean.compareAndSet(false, true)) {
            longValues[0] = nowNanos + 1000000000L / this.captureFps;

            try {
               BufferedImage image = robot.createScreenCapture(rectangle);
               int captureHeight2 = this.captureHeight;
               int width = Math.max(1, (int)Math.round((double)image.getWidth() * captureHeight2 / image.getHeight()));
               BufferedImage image2 = new BufferedImage(width, captureHeight2, 1);
               Graphics2D graphics2D = image2.createGraphics();

               try {
                  graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                  graphics2D.drawImage(image, 0, 0, width, captureHeight2, null);
               } finally {
                  graphics2D.dispose();
                  image.flush();
               }

               SwingUtilities.invokeLater(() -> {
                  boolean condition = false /* VF: Semaphore variable */;

                  try {
                     condition = true;
                     if (this.generation.get() == timestamp) {
                        if (this.panel != null) {
                           this.panel.image = image2;
                           this.panel.repaint();
                           condition = false;
                        } else {
                           condition = false;
                        }
                     } else {
                        condition = false;
                     }
                  } finally {
                     if (condition) {
                        atomicBoolean.set(false);
                     }
                  }

                  atomicBoolean.set(false);
               });
            } catch (RuntimeException failure) {
               atomicBoolean.set(false);
               this.fail(timestamp, failure);
            }
         }
      }
   }

   private void fail(long timestamp, Exception failure) {
      SkycoreClient.LOGGER.warn("top4ik mirror failed", failure);
      Minecraft.getInstance().execute(() -> {
         if (this.generation.get() == timestamp) {
            if (this.isEnabled()) {
               this.setEnabled(false);
            }
         }
      });
   }

   @Override
   protected void onEnable() {
      long timestamp = this.generation.incrementAndGet();
      this.onTick();
      if (GraphicsEnvironment.isHeadless()) {
         IllegalStateException illegalStateException = new IllegalStateException("Desktop graphics are unavailable");
         this.fail(timestamp, illegalStateException);
      } else {
         SwingUtilities.invokeLater(() -> {
            if (this.generation.get() == timestamp) {
               try {
                  this.window = new JFrame("специально");
                  this.panel = new Top4ikModule.MirrorPanel();
                  this.window.setContentPane(this.panel);
                  this.window.setDefaultCloseOperation(0);
                  this.window.setAutoRequestFocus(false);
                  this.window.addWindowListener(new WindowAdapter() {
                     @Override
                     public void windowClosing(WindowEvent windowEvent) {
                        Minecraft instance = Minecraft.getInstance();
                        instance.execute(() -> {
                           byte flag = 1;
                           flag = 1;
                           flag = -1;
                           if (Top4ikModule.this.generation.get() == timestamp) {
                              Top4ikModule.this.setEnabled(false);
                           }
                        });
                     }
                  });
                  this.window.pack();
                  this.window.setLocationByPlatform(true);
                  this.window.setVisible(true);
               } catch (RuntimeException failure) {
                  this.fail(timestamp, failure);
               }
            }
         });
         byte flag = -1;
         this.captureWorker = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "SkyCore-screen-mirror");
            thread.setDaemon(true);
            return thread;
         });
         ScheduledExecutorService captureWorker2 = this.captureWorker;
         captureWorker2.execute(() -> {

            try {
               GraphicsEnvironment localGraphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
               GraphicsDevice defaultScreenDevice = localGraphicsEnvironment.getDefaultScreenDevice();
               Robot robot = new Robot(defaultScreenDevice);
               Rectangle bounds = defaultScreenDevice.getDefaultConfiguration().getBounds();
               AtomicBoolean atomicBoolean = new AtomicBoolean();
               long[] longValues = new long[]{0L};
               Runnable runnable2 = () -> {
                  this.captureFrame(timestamp, robot, bounds, atomicBoolean, longValues);
               };
               captureWorker2.scheduleWithFixedDelay(runnable2, 0L, 5L, TimeUnit.MILLISECONDS);
            } catch (AWTException | RuntimeException failure2) {
               if (this.generation.get() == timestamp) {
                  this.fail(timestamp, failure2);
               }
            }
         });
      }
   }

   private Top4ikModule() {
      super("top4ik", "Primary-screen mirror for Discord: select the window специально", ModuleCategory.MISC);
      this.quality = this.add(new SliderSetting("Качество (p)", 720.0, 1.0, 1080.0, 1.0));
      this.generation = new AtomicLong();
      this.captureFps = 15;
      this.captureHeight = 720;
      ClientLifecycleEvents.CLIENT_STOPPING.register(minecraft -> {
         if (this.isEnabled()) {
            this.setEnabled(false);
         }
      });
   }

   @Override
   protected void onTick() {
      Double value = this.fps.get();
      int value2 = value.intValue();
      int minimum = Math.min(30, value2);
      this.captureFps = Math.max(1, minimum);
      value = this.quality.get();
      int value3 = value.intValue();
      minimum = Math.min(1080, value3);
      this.captureHeight = Math.max(1, minimum);
   }

   static final class MirrorPanel extends JPanel {
      private BufferedImage image;

      @Override
      protected void paintComponent(Graphics graphics) {
         super.paintComponent(graphics);
         if (this.image != null) {
            Graphics2D graphics2D = (Graphics2D)graphics.create();

            try {
               graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
               double width = Math.min((double)this.getWidth() / this.image.getWidth(), (double)this.getHeight() / this.image.getHeight());
               int width2 = Math.max(1, (int)Math.round(this.image.getWidth() * width));
               int height = Math.max(1, (int)Math.round(this.image.getHeight() * width));
               graphics2D.drawImage(this.image, (this.getWidth() - width2) / 2, (this.getHeight() - height) / 2, width2, height, null);
            } finally {
               graphics2D.dispose();
            }
         }
      }

      private MirrorPanel() {
         this.setPreferredSize(new Dimension(854, 480));
         this.setBackground(Color.BLACK);
      }
   }
}
