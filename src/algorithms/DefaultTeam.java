package algorithms;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;

public class DefaultTeam {

  public ArrayList<Point> calculDominatingSet(ArrayList<Point> points, int edgeThreshold) {
    ArrayList<Point> result = new ArrayList<>();
    Set<Point> remaining = new HashSet<>(points);

    while (!remaining.isEmpty()) {
      Point bestPoint = selectBestPoint(remaining, edgeThreshold);
      if (bestPoint == null) break;

      result.add(bestPoint);
      removeNeighbors(bestPoint, remaining, edgeThreshold);
    }

    return localSearch(result, points, edgeThreshold);
  }

  private Point selectBestPoint(Set<Point> points, int edgeThreshold) {
    PriorityQueue<Point> priorityQueue = new PriorityQueue<>((a, b) -> {
      int scoreA = weightedNeighborScore(a, points, edgeThreshold);
      int scoreB = weightedNeighborScore(b, points, edgeThreshold);
      return Integer.compare(scoreB, scoreA);
    });
    priorityQueue.addAll(points);
    return priorityQueue.isEmpty() ? null : priorityQueue.poll();
  }

  private int weightedNeighborScore(Point p, Set<Point> points, int edgeThreshold) {
    int score = 0;
    for (Point q : points) {
      if (p.distance(q) <= edgeThreshold) {
        score += 1;
      }
    }
    return score;
  }

  private void removeNeighbors(Point p, Set<Point> points, int edgeThreshold) {
    points.removeIf(q -> p.distance(q) <= edgeThreshold);
  }

  private ArrayList<Point> localSearch(ArrayList<Point> initialSet, ArrayList<Point> points, int edgeThreshold) {
    ArrayList<Point> result = new ArrayList<>(initialSet);
    for (int i = 0; i < result.size(); i++) {
      Point removed = result.get(i);
      result.remove(i);

      if (!isDominatingSet(result, points, edgeThreshold)) {
        result.add(i, removed);
      }
    }
    return result;
  }

  private boolean isDominatingSet(ArrayList<Point> set, ArrayList<Point> points, int edgeThreshold) {
    Set<Point> covered = new HashSet<>(set);

    for (Point p : set) {
      for (Point q : points) {
        if (p.distance(q) <= edgeThreshold) {
          covered.add(q);
        }
      }
    }
    return covered.containsAll(points);
  }

  public ArrayList<Point> geometricOptimization(ArrayList<Point> points, int edgeThreshold) {
    ArrayList<ArrayList<Point>> cells = divideIntoCells(points, edgeThreshold);
    ArrayList<Point> result = new ArrayList<>();
    for (ArrayList<Point> cell : cells) {
      result.addAll(calculDominatingSet(cell, edgeThreshold));
    }
    return result;
  }

  private ArrayList<ArrayList<Point>> divideIntoCells(ArrayList<Point> points, int edgeThreshold) {
    ArrayList<ArrayList<Point>> cells = new ArrayList<>();
    cells.add(points);
    return cells;
  }
}
