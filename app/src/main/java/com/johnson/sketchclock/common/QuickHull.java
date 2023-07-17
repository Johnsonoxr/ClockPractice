package com.johnson.sketchclock.common;
/*
 * Copyright (C) 2014 Jared Rummler <jaredrummler@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;

import androidx.annotation.NonNull;

/**
 * Find points in convex hull using quick hull method
 */
public class QuickHull {

    /**
     * Returns the quick hull of a {@link Drawable}, which is scaled down by the size
     */
    public static ArrayList<Point> quickHull(@NonNull Drawable drawable, int size) {
        return quickHull(Bitmap.createScaledBitmap(drawableToBitmap(drawable), size, size, false));
    }

    /**
     * Returns the quick hull of a {@link Drawable}
     */
    public static ArrayList<Point> quickHull(@NonNull Drawable drawable) {
        final ArrayList<Point> hull = new ArrayList<Point>();
        final Bitmap bitmap = drawableToBitmap(drawable);
        if (bitmap != null) {
            hull.addAll(quickHull(bitmap));
        }
        return hull;
    }

    /**
     * Returns the quick hull of a bitmap
     */
    public static ArrayList<Point> quickHull(@NonNull Bitmap bitmap) {
        final ArrayList<Point> points = new ArrayList<Point>();
        final int width = bitmap.getWidth();
        final int height = bitmap.getHeight();
        final int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        for (int x = 0; x < width; x++) {
            int firstY = -1, lastY = -1;
            for (int y = 0; y < height; y++) {
                final boolean transparent = (pixels[y * width + x] == Color.TRANSPARENT);
                if (!transparent) {
                    if (firstY == -1) {
                        firstY = y;
                    }
                    lastY = y;
                }
            }
            if (firstY != -1) {
                points.add(new Point(x, firstY));
                points.add(new Point(x, lastY));
            }
        }
        return quickHull(points);
    }

    /**
     * Returns a list of points in convex hull using quick hull method
     */
    @SuppressWarnings("unchecked")
    public static ArrayList<Point> quickHull(ArrayList<Point> points) {
        final ArrayList<Point> convexHull = new ArrayList<Point>();
        if (points.size() < 3) {
            return (ArrayList<Point>) points.clone();
        }

        int minPoint = -1, maxPoint = -1;
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        for (int i = 0; i < points.size(); i++) {
            if (points.get(i).x < minX) {
                minX = points.get(i).x;
                minPoint = i;
            }
            if (points.get(i).x > maxX) {
                maxX = points.get(i).x;
                maxPoint = i;
            }
        }
        final Point a = points.get(minPoint);
        final Point b = points.get(maxPoint);
        convexHull.add(a);
        convexHull.add(b);
        points.remove(a);
        points.remove(b);

        ArrayList<Point> leftSet = new ArrayList<Point>();
        ArrayList<Point> rightSet = new ArrayList<Point>();

        for (int i = 0; i < points.size(); i++) {
            Point p = points.get(i);
            if (pointLocation(a, b, p) == -1)
                leftSet.add(p);
            else
                rightSet.add(p);
        }
        hullSet(a, b, rightSet, convexHull);
        hullSet(b, a, leftSet, convexHull);

        return convexHull;
    }

    private static int distance(Point a, Point b, Point c) {
        final int ABx = b.x - a.x;
        final int ABy = b.y - a.y;
        int num = ABx * (a.y - c.y) - ABy * (a.x - c.x);
        if (num < 0)
            num = -num;
        return num;
    }

    private static void hullSet(Point a, Point b, ArrayList<Point> set, ArrayList<Point> hull) {
        final int insertPosition = hull.indexOf(b);
        if (set.size() == 0)
            return;
        if (set.size() == 1) {
            final Point p = set.get(0);
            set.remove(p);
            hull.add(insertPosition, p);
            return;
        }
        int dist = Integer.MIN_VALUE;
        int furthestPoint = -1;
        for (int i = 0; i < set.size(); i++) {
            Point p = set.get(i);
            int distance = distance(a, b, p);
            if (distance > dist) {
                dist = distance;
                furthestPoint = i;
            }
        }
        final Point p = set.get(furthestPoint);
        set.remove(furthestPoint);
        hull.add(insertPosition, p);

        // Determine who's to the left of AP
        final ArrayList<Point> leftSetAP = new ArrayList<Point>();
        for (int i = 0; i < set.size(); i++) {
            final Point m = set.get(i);
            if (pointLocation(a, p, m) == 1) {
                leftSetAP.add(m);
            }
        }

        // Determine who's to the left of PB
        final ArrayList<Point> leftSetPB = new ArrayList<Point>();
        for (int i = 0; i < set.size(); i++) {
            final Point m = set.get(i);
            if (pointLocation(p, b, m) == 1) {
                leftSetPB.add(m);
            }
        }
        hullSet(a, p, leftSetAP, hull);
        hullSet(p, b, leftSetPB, hull);
    }

    private static int pointLocation(Point a, Point b, Point p) {
        int cp1 = (b.x - a.x) * (p.y - a.y) - (b.y - a.y) * (p.x - a.x);
        return (cp1 > 0) ? 1 : -1;
    }

    private static Bitmap drawableToBitmap(@NonNull Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }
        final int height = drawable.getIntrinsicHeight();
        final int width = drawable.getIntrinsicWidth();
        if (width <= 0 || height <= 0) {
            return null;
        }
        final Bitmap bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    private QuickHull() {
        throw new IllegalStateException("no instances");
    }

}