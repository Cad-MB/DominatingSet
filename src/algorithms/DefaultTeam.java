package algorithms;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;

public class DefaultTeam {

  public ArrayList<Point> calculDominatingSet(ArrayList<Point> points, int edgeThreshold) {
    HashSet<Point> uncovered = new HashSet<>(points);
    ArrayList<Point> result = new ArrayList<>();

    while (!uncovered.isEmpty()) {
      Point bestPoint = null;
      int maxCoverage = -1;

      for (Point candidate : uncovered) {
        int coverage = countNeighbors(candidate, uncovered, edgeThreshold);
        if (coverage > maxCoverage) {
          maxCoverage = coverage;
          bestPoint = candidate;
        }
      }

      if (bestPoint != null) {
        result.add(bestPoint);
        Point finalBestPoint = bestPoint;
        uncovered.removeIf(p -> p.distance(finalBestPoint) <= edgeThreshold);
      }
    }

    return result;
  }

  private int countNeighbors(Point p, HashSet<Point> points, int edgeThreshold) {
    int count = 0;
    for (Point q : points) {
      if (p.distance(q) <= edgeThreshold) {
        count++;
      }
    }
    return count;
  }
}
