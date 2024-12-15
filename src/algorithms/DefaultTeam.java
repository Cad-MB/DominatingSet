package algorithms;

import java.awt.Point;
import java.util.*;

import java.util.stream.Collectors;

public class DefaultTeam {

  private static final double DISTANCE_THRESH_2K = 2;
  private float edgeThreshold;
  private Map<Point, List<Point>> coverToCovertee;
  private Map<Point, List<Point>> coverteeToCovers;

  public ArrayList<Point> calculDominatingSet(ArrayList<Point> points, int edgeThreshold) {
    this.edgeThreshold = edgeThreshold;
    this.coverToCovertee = new HashMap<>();
    this.coverteeToCovers = new HashMap<>();

    List<Point> result = findApproximation(points);

    for(int repeat = 0; repeat < 5; repeat++){
      List<List<Point>> clusters = getClustersOfCoverteesWithMnayCovers(edgeThreshold, result);
      for (List<Point> cluster : clusters) {
        ArrayList<Point> covers = getCovers(cluster);

        ArrayList<Point> augmentedCluster = getAugmentedCluster(cluster);
        ArrayList<Point> alreadyCovers = getAlreadyCovers(result, cluster);
        List<Point> newLocalSolution = findApproximationSubGraph(augmentedCluster, points, alreadyCovers);
        HashSet<Point> ok = new HashSet<>(result);
        ok.removeAll(covers);
        ok.addAll(newLocalSolution);
        if(result.size() > ok.size()){
          result = new ArrayList<>(ok);
        }
      }
    }
    result.addAll(getDegree0Nodes(points));
    return (ArrayList<Point>) result;
  }

  private List<Point> findApproximation(ArrayList<Point> targetPoints) {
    int minSize = targetPoints.size();
    List<Point> result = null;

    for (int tries = 0; tries < 10; tries++) {
      this.coverToCovertee = new HashMap<>();
      this.coverteeToCovers = new HashMap<>();

      List<List<Point>> searchSpaceAdj = getGraphAsAdjecencyList(targetPoints);
      ArrayList<Point> randomSolution = (ArrayList<Point>) getRandomSolution(targetPoints, searchSpaceAdj, targetPoints);
      int oldSize;
      do {
        oldSize = randomSolution.size();
        randomSolution = (ArrayList<Point>) doK2LocalSearch(randomSolution, targetPoints, targetPoints);
      } while (oldSize > randomSolution.size());

      if (randomSolution.size() < minSize) {
        minSize = randomSolution.size();
        result = randomSolution;
      }
    }
    return result;
  }


  private List<Point> findApproximationSubGraph(ArrayList<Point> targetPoints, ArrayList<Point> searchSpace, ArrayList<Point> alreadySolutions) {
    int minSize = targetPoints.size();
    List<Point> result = null;

    for (int tries = 0; tries < 20; tries++) {
      this.coverToCovertee = new HashMap<>();
      this.coverteeToCovers = new HashMap<>();

      for (Point p : alreadySolutions) {
        ArrayList<Point> neighbours = getNeighbours(p, searchSpace, edgeThreshold);
        neighbours.add(p);
        this.coverToCovertee.put(p, neighbours);

        neighbours.forEach(n -> {
          List<Point> old = coverteeToCovers.getOrDefault(n, new ArrayList<>());
          old.add(p);
          this.coverteeToCovers.put(n, old);
        });
      }
      List<List<Point>> searchSpaceAdj = getGraphAsAdjecencyList(searchSpace);
      ArrayList<Point> randomSolution = (ArrayList<Point>) getRandomSolution(targetPoints, searchSpaceAdj, searchSpace);

      int dontCount = countCoversAlreadyPresent(alreadySolutions, randomSolution);

      int oldSize = 0;

      do {
        oldSize = randomSolution.size() - dontCount;
        dontCount = 0;
        randomSolution = (ArrayList<Point>) doK2LocalSearch(randomSolution, targetPoints, searchSpace);
        for (Point p : alreadySolutions) {
          if (randomSolution.contains(p)) {
            dontCount++;
          }
        }
      } while (oldSize > randomSolution.size() - dontCount);

      if (randomSolution.size() - dontCount < minSize) {
        minSize = randomSolution.size() - dontCount;
        result = randomSolution;
      }
    }
    return result;
  }

  private int countCoversAlreadyPresent(ArrayList<Point> alreadySolutions, ArrayList<Point> randomSolution) {
    int dontCount = 0;
    for (Point p : alreadySolutions) {
      if (randomSolution.contains(p)) {
        dontCount++;
      }
    }
    return dontCount;
  }

  private ArrayList<Point> getAlreadyCovers(List<Point> result, List<Point> cluster) {
    List<Point> alreadyCovers = new ArrayList<>();
    ArrayList<Point> clusterCovers = getCovers(cluster);
    for (Point cover : result) {
      if (!clusterCovers.contains(cover)) {
        alreadyCovers.add(cover);
      }
    }
    assert alreadyCovers.size() + clusterCovers.size() == result.size();
    return (ArrayList<Point>) alreadyCovers;
  }

  private ArrayList<Point> getAugmentedCluster(List<Point> cluster) {
    ArrayList<Point> covers = getCovers(cluster);
    ArrayList<Point> allCovertees = getCovertees(covers);
    Set<Point> augmentedCluster = new HashSet<>();
    augmentedCluster.addAll(cluster);
    augmentedCluster.addAll(covers);
    augmentedCluster.addAll(allCovertees);
    return new ArrayList<>(augmentedCluster);
  }

  private List<List<Point>> getClustersOfCoverteesWithMnayCovers(int edgeThreshold, List<Point> covers) {
    List<Point> coverteesWithManyCovers = getCoverteesWithManyCovers(covers);

    List<List<Point>> allPossibleClusters = new ArrayList<>();

    for (Point p1 : coverteesWithManyCovers) {
      List<Point> done = new ArrayList<>();
      List<Point> currentCluster = new ArrayList<>();
      currentCluster.add(p1);
      done.add(p1);
      for (Point p2 : coverteesWithManyCovers) {
        if (p1 == p2 || done.contains(p2)) {
          continue;
        }
        if (p1.distance(p2) <= 2 * edgeThreshold) {
          currentCluster.add(p2);
          done.add(p2);
        }
      }
      allPossibleClusters.add(currentCluster);
    }

    allPossibleClusters = allPossibleClusters.stream()
            .sorted(Comparator.comparingInt(List::size))
            .collect(Collectors.toList());
    Collections.reverse(allPossibleClusters);

    List<List<Point>> chosenClusters = new ArrayList<>();

    List<Point> done = new ArrayList<>();

    for (List<Point> cluster_ : allPossibleClusters) {
      for (Point p : cluster_) {
        if (!done.contains(p) && done.stream().allMatch(donePoint -> !cluster_.contains(donePoint))) {
          chosenClusters.add(cluster_);
          done.addAll(cluster_);
          break;
        }
      }
    }

    done = new ArrayList<>();
    for (List<Point> cluster : chosenClusters) {
      for (Point p : cluster) {
        if (done.contains(p)) {
          throw new RuntimeException("Duplicates!");
        }
        done.add(p);
      }
    }

    return chosenClusters;
  }

  private List<Point> getCoverteesWithManyCovers(List<Point> covers) {
    List<Point> coverteesWithManyCovers = new ArrayList<>();
    for (Point coverteeKey : coverteeToCovers.keySet()) {
      if (covers.contains(coverteeKey)) {
        // add only non-covers
        continue;
      }
      if (this.coverteeToCovers.get(coverteeKey).size() >= 2) {
        // if the covertee has more than 2 covers it's considered
        coverteesWithManyCovers.add(coverteeKey);
      }
    }
    return coverteesWithManyCovers;
  }

  private List<List<Point>> getGraphAsAdjecencyList(ArrayList<Point> points) {
    List<List<Point>> adj = new ArrayList<>();
    for (int i = 0; i < points.size(); i++) {
      for (Point neig : getNeighbours(points.get(i), points, this.edgeThreshold)) {
        adj.add(new ArrayList<>(Arrays.asList(points.get(i), neig)));
      }
    }
    return adj;
  }

  private List<Point> getRandomSolution(List<Point> targetPoints, List<List<Point>> searchSpaceAdj, List<Point> searchSpace) {
    List<Point> result = new ArrayList<>();
    List<Point> covered = new ArrayList<>();
    Collections.shuffle(searchSpaceAdj);
    for (List<Point> edge : searchSpaceAdj) {
      for (Point n : edge) {
        if (!covered.contains(n) && targetPoints.contains(n)) {
          result.add(n);
          covered.add(n);

          ArrayList<Point> coverteeOfThis = getNeighbours(n, searchSpace, this.edgeThreshold);
          coverteeOfThis.add(n);

          coverteeOfThis.forEach(p -> {
            covered.add(p);
            List<Point> old = coverteeToCovers.getOrDefault(p, new ArrayList<>());
            old.add(n);
            coverteeToCovers.put(p, old);
          });

          coverToCovertee.put(n, coverteeOfThis);
        }
      }
    }
    return result;
  }

  private List<Point> doK2LocalSearch(List<Point> cover, List<Point> targetPoints, List<Point> searchSpace) {
    for (int outer_i = 0; outer_i < cover.size(); outer_i++) {
      for (int inner_i = outer_i; inner_i < cover.size(); inner_i++) {
        Point p = cover.get(outer_i);
        Point p2 = cover.get(inner_i);
        if (p.distance(p2) > (edgeThreshold * DISTANCE_THRESH_2K)) {
          continue;
        }
        if (p == p2) {
          continue;
        }
        List<Point> uniqueCoverteesOfPairInTarget = getUniqueCoverteesOfPair(p, p2, targetPoints);
        for (Point out : searchSpace) {
          if (getCentroid(Arrays.asList(p, p2)).distance(out) > edgeThreshold * DISTANCE_THRESH_2K) {
            continue;
          }
          ArrayList<Point> coverteesOfOut = getNeighbours(out, searchSpace, this.edgeThreshold);
          coverteesOfOut.add(out);
          if (coverteesOfOut.containsAll(uniqueCoverteesOfPairInTarget)) {

            coverToCovertee.getOrDefault(p2, new ArrayList<>()).forEach(covertee -> {
              if (coverteeToCovers.get(covertee) != null) {
                coverteeToCovers.get(covertee).remove(p2);
              }
            });
            coverToCovertee.getOrDefault(p, new ArrayList<>()).forEach(covertee -> {
              if (coverteeToCovers.get(covertee) != null) {
                coverteeToCovers.get(covertee).remove(p);
              }
            });
            coverteeToCovers.get(p).remove(p);
            coverteeToCovers.get(p2).remove(p2);
            coverToCovertee.remove(p);
            coverToCovertee.remove(p2);

            coverToCovertee.put(out, coverteesOfOut);
            coverteeToCovers.getOrDefault(out, new ArrayList<>()).remove(p);
            coverteeToCovers.getOrDefault(out, new ArrayList<>()).remove(p2);
            coverteeToCovers.getOrDefault(out, new ArrayList<>()).add(out);
            coverteesOfOut.forEach(cov -> {
              List<Point> old = coverteeToCovers.getOrDefault(cov, new ArrayList<>());
              old.add(out);
              coverteeToCovers.put(cov, old);
            });

            cover.remove(p);
            cover.remove(p2);
            cover.add(out);
            return cover;
          }
        }
      }
    }
    return cover;
  }

  private List<Point> getUniqueCoverteesOfPair(Point a, Point b, List<Point> targetPoints) {

    List<Point> pair = new ArrayList<>(Arrays.asList(a, b));

    Set<Point> coverteesOfPair = new HashSet<>();
    pair.forEach(p -> coverteesOfPair.addAll(coverToCovertee.getOrDefault(p, new ArrayList<>())));

    List<Point> result = new ArrayList<>();
    for (Point covertee : coverteesOfPair) {
      if (coverteeToCovers.get(covertee).stream().allMatch(cover -> pair.contains(cover) && targetPoints.contains(covertee))) {
        // add a point if it's exclusively covered by this pair of nodes
        // and if we are interested in in
        result.add(covertee);
      }
    }
    return result;
  }

  private Point getCentroid(List<Point> points) {
    int sumX = 0;
    int sumY = 0;
    for (Point p : points) {
      sumX += p.x;
      sumY += p.y;
    }
    return new Point(sumX / points.size(), sumY / points.size());
  }

  private ArrayList<Point> getNeighbours(Point p, List<Point> vertices, float edgeThreshold) {
    ArrayList<Point> result = new ArrayList();
    Iterator var5 = vertices.iterator();

    while (var5.hasNext()) {
      Point point = (Point) var5.next();
      if (point.distance(p) < (double) edgeThreshold && !point.equals(p)) {
        result.add((Point) point.clone());
      }
    }

    return result;
  }

  private ArrayList<Point> getDegree0Nodes(ArrayList<Point> points) {
    return (ArrayList<Point>) points.stream().filter(p -> getNeighbours(p, points, this.edgeThreshold).size() == 0).collect(Collectors.toList());
  }

  private ArrayList<Point> getCovers(List<Point> covertees) {
    Set<Point> result = new HashSet<>();
    for (Point p : covertees) {
      result.addAll(coverteeToCovers.get(p));
    }
    return new ArrayList<>(result);
  }

  private ArrayList<Point> getCovertees(List<Point> covers) {
    Set<java.awt.Point> result = new HashSet<>();
    for (Point p : covers) {
      result.addAll(coverToCovertee.get(p));
    }
    return new ArrayList<>(result);
  }
}