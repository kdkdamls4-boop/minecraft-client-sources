package net.skycore.client.util;

public final class PerlinNoise {
   private static final int[] P = new int[512];
   private static final int[] PERMUTATION = new int[]{
      151,
      160,
      137,
      91,
      90,
      15,
      131,
      13,
      201,
      95,
      96,
      53,
      194,
      233,
      7,
      225,
      140,
      36,
      103,
      30,
      69,
      142,
      8,
      99,
      37,
      240,
      21,
      10,
      23,
      190,
      6,
      148,
      247,
      120,
      234,
      75,
      0,
      26,
      197,
      62,
      94,
      252,
      219,
      203,
      117,
      35,
      11,
      32,
      57,
      177,
      33,
      88,
      237,
      149,
      56,
      87,
      174,
      20,
      125,
      136,
      171,
      168,
      68,
      175,
      74,
      165,
      71,
      134,
      139,
      48,
      27,
      166,
      77,
      146,
      158,
      231,
      83,
      111,
      229,
      122,
      60,
      211,
      133,
      230,
      220,
      105,
      92,
      41,
      55,
      46,
      245,
      40,
      244,
      102,
      143,
      54,
      65,
      25,
      63,
      161,
      1,
      216,
      80,
      73,
      209,
      76,
      132,
      187,
      208,
      89,
      18,
      169,
      200,
      196,
      135,
      130,
      116,
      188,
      159,
      86,
      164,
      100,
      109,
      198,
      173,
      186,
      3,
      64,
      52,
      217,
      226,
      250,
      124,
      123,
      5,
      202,
      38,
      147,
      118,
      126,
      255,
      82,
      85,
      212,
      207,
      206,
      59,
      227,
      47,
      16,
      58,
      17,
      182,
      189,
      28,
      42,
      223,
      183,
      170,
      213,
      119,
      248,
      152,
      2,
      44,
      145,
      31,
      179,
      228,
      167,
      215,
      221,
      201,
      157,
      49,
      192,
      239,
      97,
      30,
      181,
      141,
      180,
      193,
      39,
      12,
      84,
      115,
      153,
      163,
      85,
      214,
      129,
      24,
      254,
      43,
      150,
      138,
      19,
      106,
      78,
      13,
      67,
      112,
      61,
      66,
      114,
      249,
      14,
      236,
      239,
      79,
      178,
      241,
      34,
      45,
      218,
      63,
      93,
      110,
      243,
      101,
      154,
      128,
      155,
      144,
      145,
      152,
      246,
      127,
      156,
      22,
      251,
      108,
      176,
      121,
      235,
      162,
      195,
      242,
      107,
      172,
      113,
      253,
      224,
      199,
      4,
      116,
      164,
      222,
      185,
      184,
      179
   };

   private static double grad(int value, double value2, double value3) {
      int value4 = value & 7;
      double value5 = value4 < 4 ? value2 : value3;
      double value6 = value4 < 4 ? value3 : value2;
      return ((value4 & 1) == 0 ? value5 : -value5) + ((value4 & 2) == 0 ? value6 : -value6);
   }

   private static double lerp(double value, double value2, double value3) {
      return value2 + value * (value3 - value2);
   }

   static {
      for (int value = 0; value < 256; value++) {
         P[256 + value] = P[value] = PERMUTATION[value % PERMUTATION.length];
      }
   }

   public static float perlin2D(double value, double value2) {
      int computedValue = (int)Math.floor(value) & 0xFF;
      int computedValue2 = (int)Math.floor(value2) & 0xFF;
      int intermediate = -1;
      value -= Math.floor(value);
      intermediate = (byte)1;
      value2 -= Math.floor(value2);
      int value3 = 1;
      double value4 = fade(value);
      double value5 = fade(value2);
      int computedValue3 = P[computedValue] + computedValue2;
      int computedValue4 = P[computedValue + 1] + computedValue2;
      value3 = P[computedValue3];
      double value6 = grad(value3, value, value2);
      intermediate = P[computedValue4];
      double intermediate2 = value - 1.0;
      double value7 = grad(intermediate, intermediate2, value2);
      double value8 = lerp(value4, value6, value7);
      double computedValue5 = grad(P[computedValue3 + 1], value, value2 - 1.0);
      int computedValue6 = P[computedValue4 + 1];
      double computedValue7 = value - 1.0;
      double computedValue8 = value2 - 1.0;
      intermediate2 = grad(computedValue6, computedValue7, computedValue8);
      return (float)lerp(value5, value8, lerp(value4, computedValue5, intermediate2));
   }

   public static float perlin1D(double value) {
      return perlin2D(value, 0.5);
   }

   private static double fade(double value) {
      return value * value * value * (value * (value * 6.0 - 15.0) + 10.0);
   }

   private PerlinNoise() {
   }
}
