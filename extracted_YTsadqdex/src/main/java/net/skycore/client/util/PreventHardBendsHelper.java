package net.skycore.client.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.util.Mth;

public class PreventHardBendsHelper {
   public int numIterations;
   public List<PreventHardBendsHelper.Point> points = new ArrayList<>();
   public List<PreventHardBendsHelper.Stick> sticks = new ArrayList<>();
   private final float maxBend;
   public CapeVector3 gravityDirection = new CapeVector3(0.0F, -1.0F, 0.0F);
   public float gravity = CapeSimulationManager.config.gravity;
   public boolean sneaking;

   public PreventHardBendsHelper() {
      this.numIterations = 30;
      this.maxBend = 20.0F;
      this.sneaking = false;
   }

   public List<PreventHardBendsHelper.Point> getPoints() {
      return this.points;
   }

   private void preventHardBends() {
      for (int value = 1; value < this.points.size() - 2; value++) {
         double angle = this.getAngle(this.points.get(value).position, this.points.get(value - 1).position, this.points.get(value + 1).position);
         if (angle < -20.0F) {
            CapeVector3 replacement = this.getReplacement(this.points.get(value).position, this.points.get(value - 1).position, -20.0F * 2.0F);
            this.points.get(value + 1).position = replacement;
         }

         if (angle > 20.0) {
            CapeVector3 replacement2 = this.getReplacement(this.points.get(value).position, this.points.get(value - 1).position, 20.0F * 2.0F);
            this.points.get(value + 1).position = replacement2;
         }
      }
   }

   public void setSneaking(boolean sneaking2) {
      this.sneaking = sneaking2;
   }

   public void setGravityDirection(CapeVector3 gravityDirection2) {
      this.gravityDirection = gravityDirection2;
   }

   private void applyMotion() {
      for (int value = 0; value < this.numIterations; value++) {
         for (int index = this.sticks.size() - 1; index >= 0; index--) {
            PreventHardBendsHelper.Stick stick = this.sticks.get(index);
            CapeVector3 normalizeHelper3 = stick.pointA.position.clone().add(stick.pointB.position).div(2.0F);
            CapeVector3 normalizeHelper32 = stick.pointA.position.clone().subtract(stick.pointB.position).normalize();
            if (!stick.pointA.locked) {
               stick.pointA.position = normalizeHelper3.clone().add(normalizeHelper32.clone().mul(stick.length / 2.0F));
            }

            if (!stick.pointB.locked) {
               stick.pointB.position = normalizeHelper3.clone().subtract(normalizeHelper32.clone().mul(stick.length / 2.0F));
            }
         }
      }
   }

   private void limitLength() {
      int value = 0;

      while (true) {
         if (value >= this.sticks.size()) {
            return;
         }

         PreventHardBendsHelper.Stick stick = this.sticks.get(value);
         CapeVector3 normalizeHelper3 = stick.pointA.position.clone();
         CapeVector3 normalizeHelper32 = normalizeHelper3.subtract(stick.pointB.position).normalize();
         if (!stick.pointB.locked) {
            PreventHardBendsHelper.Point pointB = stick.pointB;
            CapeVector3 normalizeHelper33 = stick.pointA.position.clone();
            CapeVector3 normalizeHelper34 = normalizeHelper32.mul(stick.length);
            pointB.position = normalizeHelper33.subtract(normalizeHelper34);
         }

         value++;
      }
   }

   public boolean isSneaking() {
      return this.sneaking;
   }

   private double getAngle(CapeVector3 normalizeHelper3, CapeVector3 normalizeHelper32, CapeVector3 normalizeHelper33) {
      float x = normalizeHelper32.x - normalizeHelper3.x;
      float y = normalizeHelper32.y - normalizeHelper3.y;
      float x2 = normalizeHelper32.x - normalizeHelper33.x;
      float y2 = normalizeHelper32.y - normalizeHelper33.y;
      float computedValue = x * x2 + y * y2;
      float computedValue2 = x * y2 - y * x2;
      double angleRadians = Mth.atan2(computedValue2, computedValue);
      return angleRadians * 180.0 / Math.PI;
   }

   private void applyGravity() {
      CapeVector3 normalizeHelper3 = this.gravityDirection.clone();
      float computedValue = this.gravity * 0.05F;
      CapeVector3 normalizeHelper32 = normalizeHelper3.mul(computedValue);
      CapeVector3 normalizeHelper33 = new CapeVector3(0.0F, 0.0F, 0.0F);
      Iterator iterator = this.points.iterator();

      while (true) {
         if (!iterator.hasNext()) {
            return;
         }

         PreventHardBendsHelper.Point element = (PreventHardBendsHelper.Point)iterator.next();
         if (!element.locked) {
            normalizeHelper33.copy(element.position);
            element.position.add(normalizeHelper32);
            element.prevPosition.copy(normalizeHelper33);
         }
      }
   }

   public float getGravity() {
      return this.gravity;
   }

   public void setGravity(float gravity2) {
      this.gravity = gravity2;
   }

   public boolean init(int index) {
      if (this.points.size() != index) {
         this.points.clear();
         this.sticks.clear();

         for (int value = 0; value < index; value++) {
            PreventHardBendsHelper.Point point = new PreventHardBendsHelper.Point();
            point.position.y = -value;
            point.position.x = -value;
            point.locked = ((value == 0 ? 1 : 0) != 0);
            this.points.add(point);
            if (value > 0) {
               this.sticks.add(new PreventHardBendsHelper.Stick(this.points.get(value - 1), point, 1.0F));
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private void preventSelfClipping() {
      int value = 0;

      byte flag;
      do {
         flag = 0;

         for (int value2 = 0; value2 < this.points.size(); value2++) {
            for (int index = value2 + 1; index < this.points.size(); index++) {
               PreventHardBendsHelper.Point point = this.points.get(value2);
               PreventHardBendsHelper.Point point2 = this.points.get(index);
               CapeVector3 normalizeHelper3 = point.position.clone().subtract(point2.position);
               if (normalizeHelper3.sqrMagnitude() < 0.99) {
                  flag = 1;
                  value++;
                  normalizeHelper3.normalize();
                  CapeVector3 normalizeHelper32 = point.position.clone().add(point2.position).div(2.0F);
                  if (!point.locked) {
                     point.position = normalizeHelper32.clone().add(normalizeHelper3.clone().mul(0.5F));
                  }

                  if (!point2.locked) {
                     point2.position = normalizeHelper32.clone().subtract(normalizeHelper3.clone().mul(0.5F));
                  }
               }
            }
         }
      } while (flag != 0 && value < 32);
   }

   public boolean empty() {
      return this.sticks.isEmpty();
   }

   private CapeVector3 getReplacement(CapeVector3 normalizeHelper3, CapeVector3 normalizeHelper32, double value) {
      CapeVector3 normalizeHelper33 = normalizeHelper3.clone();
      CapeVector3 normalizeHelper34 = normalizeHelper33.subtract(normalizeHelper32);
      CapeVector3 normalizeHelper35 = normalizeHelper34.rotateDegrees((float)value);
      normalizeHelper35.add(normalizeHelper3);
      return normalizeHelper34;
   }

   private void preventClipping() {
      PreventHardBendsHelper.Point point = this.points.get(0);

      for (int value = 1; value < this.points.size(); value++) {
         PreventHardBendsHelper.Point point2 = this.points.get(value);
         if (point2.position.x - point.position.x > 0.0F) {
            point2.position.x = point.position.x;
         }

         float computedValue = (float)value / this.points.size() * ((float)value / this.points.size()) * 5.0F;
         float z = point.position.z - point2.position.z;
         if (z > computedValue) {
            point2.position.z = point.position.z - computedValue;
         }

         if (z < -computedValue) {
            point2.position.z = point.position.z + computedValue;
         }
      }
   }

   public void applyMovement(CapeVector3 normalizeHelper3) {
      CapeVector3 normalizeHelper32 = this.points.get(0).prevPosition;
      PreventHardBendsHelper.Point point = this.points.get(0);
      normalizeHelper32.copy(point.position);
      this.points.get(0).position.add(normalizeHelper3);
   }

   public void simulate() {
      this.applyGravity();
      this.preventClipping();
      this.preventSelfClipping();
      this.applyMotion();
      this.preventSelfClipping();
      this.preventHardBends();
      this.limitLength();
   }

   public static class Point {
      public CapeVector3 prevPosition;
      public boolean locked;
      public CapeVector3 position = new CapeVector3(0.0F, 0.0F, 0.0F);

      public float getLerpY(float value) {
         return Mth.lerp(value, this.prevPosition.y, this.position.y);
      }

      public CapeVector3 getLerpedPos(float value) {
         float lerpX = this.getLerpX(value);
         float lerpY = this.getLerpY(value);
         float lerpZ = this.getLerpZ(value);
         return new CapeVector3(lerpX, lerpY, lerpZ);
      }

      public float getLerpX(float value) {
         return Mth.lerp(value, this.prevPosition.x, this.position.x);
      }

      public float getLerpZ(float value) {
         return Mth.lerp(value, this.prevPosition.z, this.position.z);
      }

      public Point() {
         this.prevPosition = new CapeVector3(0.0F, 0.0F, 0.0F);
      }
   }

   public static class Stick {
      public PreventHardBendsHelper.Point pointA;
      public PreventHardBendsHelper.Point pointB;
      public float length;

      public Stick(PreventHardBendsHelper.Point pointA2, PreventHardBendsHelper.Point pointB2, float length2) {
         this.pointA = pointA2;
         this.pointB = pointB2;
         this.length = length2;
      }
   }
}
