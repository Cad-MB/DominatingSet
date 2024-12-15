package algorithms;

import java.awt.Point;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class DefaultTeam {

  public ArrayList<Point> greedyWithLocalSearch(ArrayList<Point> points, int edgeThreshold) {
    ArrayList<Point> remaining = (ArrayList<Point>) points.clone();
    ArrayList<Point> solution = new ArrayList<>();
    ArrayList<Point> removedPoints = new ArrayList<>();
    ArrayList<Integer> bestIndices = new ArrayList<>(256);

    while (!remaining.isEmpty()) {
      int maxNeighbors = 0;
      bestIndices.clear();

      for (int i = 0; i < remaining.size(); i++) {
        Point candidate = remaining.get(i);
        int neighborCount = countNeighbors(candidate, remaining, edgeThreshold);
        if (neighborCount > maxNeighbors) {
          maxNeighbors = neighborCount;
          bestIndices.clear();
          bestIndices.add(i);
        } else if (neighborCount == maxNeighbors) {
          bestIndices.add(i);
        }
      }

      int bestIndex = bestIndices.get(ThreadLocalRandom.current().nextInt(bestIndices.size()));
      Point chosen = swapRemove(remaining, bestIndex);
      solution.add(chosen);

      for (int i = 0; i < remaining.size(); i++) {
        Point neighbor = remaining.get(i);
        if (isNeighbor(chosen, neighbor, edgeThreshold)) {
          removedPoints.add(neighbor);
          swapRemove(remaining, i--);
        }
      }
    }

    return localSearch(solution, removedPoints, edgeThreshold);
  }

  private ArrayList<Point> localSearch(ArrayList<Point> solution, ArrayList<Point> removedPoints, int edgeThreshold) {
    boolean improved = true;

    while (improved) {
      improved = false;

      for (int i = 0; i < solution.size(); i++) {
        Point toRemove = swapRemove(solution, i);

        if (isValidDominatingSet(solution, removedPoints, edgeThreshold)) {
          improved = true;
          break;
        }

        swapBack(solution, i, toRemove);
      }
    }

    return solution;
  }

  private boolean isValidDominatingSet(ArrayList<Point> solution, ArrayList<Point> points, int edgeThreshold) {
    HashSet<Point> covered = new HashSet<>(solution);

    for (Point p : solution) {
      for (Point q : points) {
        if (isNeighbor(p, q, edgeThreshold)) {
          covered.add(q);
        }
      }
    }

    return covered.containsAll(points);
  }

  private boolean isNeighbor(Point p, Point q, int threshold) {
    return p.distanceSq(q) <= threshold * threshold;
  }

  private int countNeighbors(Point p, ArrayList<Point> points, int threshold) {
    int count = 0;
    for (Point q : points) {
      if (p != q && isNeighbor(p, q, threshold)) {
        count++;
      }
    }
    return count;
  }

  private static <T> T swapRemove(List<T> list, int index) {
    int lastIndex = list.size() - 1;
    if (index == lastIndex) {
      return list.remove(index);
    }
    T removed = list.get(index);
    list.set(index, list.remove(lastIndex));
    return removed;
  }

  private static <T> void swapBack(List<T> list, int index, T value) {
    if (index >= list.size()) {
      list.add(value);
    } else {
      T swapped = list.get(index);
      list.set(index, value);
      list.add(swapped);
    }
  }

  public ArrayList<Point> calculDominatingSet(ArrayList<Point> points, int edgeThreshold) {
    Stream<ArrayList<Point>> solutions = IntStream.range(0, 4)
            .parallel()
            .mapToObj(i -> greedyWithLocalSearch(points, edgeThreshold));

    return solutions.min(Comparator.comparingInt(ArrayList::size)).get();
  }
}
