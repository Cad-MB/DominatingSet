/**
 * Ce fichier implémente une solution pour le problème du Minimum Dominating Set.
 * Les ressources suivantes ont été consultées dans le cadre de ce projet pour
 * s'inspirer des méthodes et optimiser l'implémentation :
 *
 * - https://github.com/valeeraZ/Sorbonne_DAAR/blob/master/DominatingSet/src/algorithms/DefaultTeam.java
 * - https://github.com/axelzzz/AAGA-cds/blob/111059b72d1011ee2a469f5247b574c80d072bd1/TME3/src/algorithms/DefaultTeam.java#L530
 * - https://github.com/0xCAFEADD1C7/Minimum-Dominating-Set/blob/1226a13ee895cbd34d6187e64f9e29e47dc2f9e0/src/algorithms/DefaultTeam.java#L232
 * - https://github.com/CarloSegat/Daar/blob/main/tme_09_dominating%20set/src/algorithms/DefaultTeam.java
 */

 package algorithms;

import java.awt.Point;
import java.util.*;
import java.util.stream.Collectors;

public class DefaultTeam {

  // multiplier pour ajuster les seuils d'optimisation locale
  private static final double MULTIPLIER = 2.0;
  private float rangeLimit; // distance limite pour déterminer les voisins
  private Map<Point, List<Point>> influenceMap; // points influencés par un point donné
  private Map<Point, List<Point>> influencedByMap; // points influençant un point donné

  // méthode principale pour calculer un ensemble dominant
  public ArrayList<Point> calculDominatingSet(ArrayList<Point> nodes, int rangeThreshold) {
    this.rangeLimit = rangeThreshold;
    this.influenceMap = new HashMap<>();
    this.influencedByMap = new HashMap<>();

    // génération d'une solution initiale pour le problème
    List<Point> resultSet = generateInitialSolution(nodes);

    // boucle de raffinement sur plusieurs cycles
    for (int cycle = 0; cycle < 5; cycle++) {
      List<List<Point>> clusters = detectClusters(rangeThreshold, resultSet); // détecter les clusters
      for (List<Point> cluster : clusters) {
        ArrayList<Point> localCovers = getCovers(cluster); // récupérer les points couvrant un cluster
        ArrayList<Point> extendedCluster = extendCluster(cluster); // étendre la portée d'un cluster
        ArrayList<Point> existingCovers = filterExistingCovers(resultSet, cluster); // filtrer les points déjà couverts
        List<Point> refinedSolution = refineClusterSolution(extendedCluster, nodes, existingCovers); // affiner la solution

        // mise à jour de l'ensemble dominant si une meilleure solution est trouvée
        HashSet<Point> updatedSet = new HashSet<>(resultSet);
        updatedSet.removeAll(localCovers);
        updatedSet.addAll(refinedSolution);

        if (resultSet.size() > updatedSet.size()) {
          resultSet = new ArrayList<>(updatedSet);
        }
      }
    }

    // ajout des nœuds isolés à l'ensemble dominant
    resultSet.addAll(findIsolatedNodes(nodes));
    return new ArrayList<>(resultSet);
  }

  // méthode pour générer une solution initiale basée sur des heuristiques
  private List<Point> generateInitialSolution(ArrayList<Point> targets) {
    int minSize = targets.size();
    List<Point> optimalSolution = null;

    // effectuer plusieurs tentatives pour générer une meilleure solution
    for (int attempt = 0; attempt < 10; attempt++) {
      this.influenceMap.clear();
      this.influencedByMap.clear();

      List<List<Point>> adjacency = buildAdjacencyList(targets);
      ArrayList<Point> candidate = (ArrayList<Point>) buildRandomSolution(targets, adjacency, targets);

      int prevSize;
      do {
        prevSize = candidate.size();
        candidate = (ArrayList<Point>) applyLocalOptimization(candidate, targets, targets);
      } while (prevSize > candidate.size());

      if (candidate.size() < minSize) {
        minSize = candidate.size();
        optimalSolution = candidate;
      }
    }
    return optimalSolution;
  }

  // méthode pour affiner la solution en utilisant une approche basée sur les clusters
  private List<Point> refineClusterSolution(ArrayList<Point> cluster, ArrayList<Point> domain, ArrayList<Point> existingCovers) {
    int smallestSize = cluster.size();
    List<Point> bestSolution = null;

    // boucle pour améliorer les solutions existantes
    for (int iteration = 0; iteration < 20; iteration++) {
      this.influenceMap.clear();
      this.influencedByMap.clear();

      // initialisation des points influencés et influençant
      for (Point cover : existingCovers) {
        ArrayList<Point> neighbors = locateNeighbors(cover, domain, rangeLimit);
        neighbors.add(cover);
        this.influenceMap.put(cover, neighbors);

        for (Point neighbor : neighbors) {
          List<Point> influences = influencedByMap.getOrDefault(neighbor, new ArrayList<>());
          influences.add(cover);
          this.influencedByMap.put(neighbor, influences);
        }
      }

      List<List<Point>> adjacency = buildAdjacencyList(domain);
      ArrayList<Point> candidate = (ArrayList<Point>) buildRandomSolution(cluster, adjacency, domain);

      int excluded = countSharedCovers(existingCovers, candidate);

      int previousSize;
      do {
        previousSize = candidate.size() - excluded;
        excluded = 0;
        candidate = (ArrayList<Point>) applyLocalOptimization(candidate, cluster, domain);

        for (Point cover : existingCovers) {
          if (candidate.contains(cover)) {
            excluded++;
          }
        }
      } while (previousSize > candidate.size() - excluded);

      if (candidate.size() - excluded < smallestSize) {
        smallestSize = candidate.size() - excluded;
        bestSolution = candidate;
      }
    }
    return bestSolution;
  }

  // compte le nombre de points communs entre deux ensembles de couverture
  private int countSharedCovers(ArrayList<Point> existing, ArrayList<Point> candidates) {
    int count = 0;
    for (Point cover : existing) {
      if (candidates.contains(cover)) {
        count++; // incrémente si le point est partagé par les deux ensembles
      }
    }
    return count;
  }

  // filtre les couvertures existantes qui ne sont pas incluses dans un cluster donné
  private ArrayList<Point> filterExistingCovers(List<Point> currentSet, List<Point> cluster) {
    List<Point> existing = new ArrayList<>();
    ArrayList<Point> clusterCovers = getCovers(cluster); // récupère les points couvrant le cluster

    for (Point cover : currentSet) {
      if (!clusterCovers.contains(cover)) {
        existing.add(cover); // ajoute les points non présents dans les couvertures du cluster
      }
    }
    return new ArrayList<>(existing);
  }

  // étend un cluster en incluant les points qui couvrent et sont couverts par le cluster
  private ArrayList<Point> extendCluster(List<Point> cluster) {
    ArrayList<Point> clusterCovers = getCovers(cluster); // récupère les couvertures actuelles
    ArrayList<Point> allNodes = getCoveredNodes(clusterCovers); // récupère tous les points couverts

    Set<Point> extendedSet = new HashSet<>();
    extendedSet.addAll(cluster);
    extendedSet.addAll(clusterCovers);
    extendedSet.addAll(allNodes);

    return new ArrayList<>(extendedSet);
  }

  // détecte les clusters dans les couvertures basées sur un seuil de distance
  private List<List<Point>> detectClusters(int rangeThreshold, List<Point> covers) {
    List<Point> multipleCoversNodes = findNodesWithMultipleCovers(covers); // trouve les nœuds avec plusieurs couvertures

    List<List<Point>> preliminaryClusters = new ArrayList<>();

    for (Point primary : multipleCoversNodes) {
      List<Point> checked = new ArrayList<>();
      List<Point> cluster = new ArrayList<>();
      cluster.add(primary); // commence un cluster avec le point principal
      checked.add(primary);

      for (Point secondary : multipleCoversNodes) {
        if (primary.equals(secondary) || checked.contains(secondary)) {
          continue; // saute si déjà vérifié
        }
        if (primary.distance(secondary) <= 2 * rangeThreshold) {
          cluster.add(secondary); // ajoute les points voisins dans le cluster
          checked.add(secondary);
        }
      }
      preliminaryClusters.add(cluster);
    }
    // trie les clusters par taille décroissante
    preliminaryClusters.sort((cluster1, cluster2) -> Integer.compare(cluster2.size(), cluster1.size()));

    List<List<Point>> validClusters = new ArrayList<>();
    Set<Point> used = new HashSet<>();

    // valide les clusters en évitant les chevauchements
    for (List<Point> cluster : preliminaryClusters) {
      if (cluster.stream().noneMatch(used::contains)) {
        validClusters.add(cluster);
        used.addAll(cluster);
      }
    }

    return validClusters;
  }

  // trouve les nœuds ayant plusieurs couvertures
  private List<Point> findNodesWithMultipleCovers(List<Point> covers) {
    List<Point> result = new ArrayList<>();
    for (Point node : influencedByMap.keySet()) {
      if (!covers.contains(node) && influencedByMap.get(node).size() >= 2) {
        result.add(node); // ajoute les nœuds couverts par au moins deux points
      }
    }
    return result;
  }

  // construit une liste d'adjacence pour les nœuds
  private List<List<Point>> buildAdjacencyList(ArrayList<Point> nodes) {
    List<List<Point>> adjacency = new ArrayList<>();
    for (Point node : nodes) {
      for (Point neighbor : locateNeighbors(node, nodes, rangeLimit)) {
        adjacency.add(Arrays.asList(node, neighbor)); // ajoute une arête entre chaque nœud et son voisin
      }
    }
    return adjacency;
  }

  // construit une solution aléatoire pour couvrir les cibles
  private List<Point> buildRandomSolution(List<Point> targets, List<List<Point>> adjacency, List<Point> domain) {
    List<Point> solution = new ArrayList<>();
    Set<Point> coveredNodes = new HashSet<>();
    Collections.shuffle(adjacency); // mélange les arêtes pour garantir une solution aléatoire

    for (List<Point> edge : adjacency) {
      for (Point node : edge) {
        if (!coveredNodes.contains(node) && targets.contains(node)) {
          solution.add(node); // ajoute le point à la solution s'il n'est pas encore couvert
          coveredNodes.add(node);

          ArrayList<Point> coveredByNode = locateNeighbors(node, domain, rangeLimit);
          coveredByNode.add(node);

          for (Point covered : coveredByNode) {
            coveredNodes.add(covered);
            influencedByMap.computeIfAbsent(covered, k -> new ArrayList<>()).add(node);
          }

          influenceMap.put(node, coveredByNode);
        }
      }
    }
    return solution;
  }

  // applique des optimisations locales pour améliorer la couverture
  private List<Point> applyLocalOptimization(List<Point> covers, List<Point> targets, List<Point> domain) {
    for (int i = 0; i < covers.size(); i++) {
      for (int j = i; j < covers.size(); j++) {
        Point first = covers.get(i);
        Point second = covers.get(j);

        if (first.equals(second) || first.distance(second) > rangeLimit * MULTIPLIER) {
          continue; // ignore si les points sont trop éloignés ou identiques
        }

        List<Point> uniqueNodes = findExclusiveNodes(first, second, targets);

        for (Point candidate : domain) {
          if (candidate.distance(getCentroid(Arrays.asList(first, second))) > rangeLimit * MULTIPLIER) {
            continue; // saute les candidats hors du rayon
          }

          ArrayList<Point> candidateCovers = locateNeighbors(candidate, domain, rangeLimit);
          candidateCovers.add(candidate);

          if (candidateCovers.containsAll(uniqueNodes)) {
            substituteCovers(covers, first, second, candidate, candidateCovers);
            return covers;
          }
        }
      }
    }
    return covers;
  }

  // remplace deux couvertures existantes par une meilleure couverture
  private void substituteCovers(List<Point> covers, Point first, Point second, Point replacement, List<Point> replacementCovers) {
    updateMappingsOnRemoval(first);
    updateMappingsOnRemoval(second);

    covers.remove(first);
    covers.remove(second);

    influenceMap.put(replacement, replacementCovers);
    replacementCovers.forEach(node -> {
      influencedByMap.computeIfAbsent(node, k -> new ArrayList<>()).add(replacement);
    });

    covers.add(replacement);
  }

  // met à jour les mappages après avoir supprimé un point
  private void updateMappingsOnRemoval(Point point) {
    List<Point> influenced = influenceMap.remove(point);
    if (influenced != null) {
      for (Point node : influenced) {
        influencedByMap.get(node).remove(point); // retire les influences du point supprimé
      }
    }
  }

  // trouve les nœuds uniquement couverts par deux points spécifiques
  private List<Point> findExclusiveNodes(Point a, Point b, List<Point> targets) {
    Set<Point> sharedNodes = new HashSet<>();
    sharedNodes.addAll(influenceMap.getOrDefault(a, Collections.emptyList()));
    sharedNodes.addAll(influenceMap.getOrDefault(b, Collections.emptyList()));

    List<Point> exclusiveNodes = new ArrayList<>();
    for (Point node : sharedNodes) {
      if (influencedByMap.getOrDefault(node, Collections.emptyList()).stream().allMatch(cover -> cover.equals(a) || cover.equals(b))) {
        exclusiveNodes.add(node);
      }
    }
    return exclusiveNodes;
  }

  private Point getCentroid(List<Point> points) {
    int sumX = 0;
    int sumY = 0;
    for (Point point : points) {
      sumX += point.x;
      sumY += point.y;
    }
    return new Point(sumX / points.size(), sumY / points.size());
  }

  private ArrayList<Point> locateNeighbors(Point node, List<Point> domain, float threshold) {
    ArrayList<Point> neighbors = new ArrayList<>();
    for (Point candidate : domain) {
      if (!candidate.equals(node) && candidate.distance(node) < threshold) {
        neighbors.add((Point) candidate.clone());
      }
    }
    return neighbors;
  }

  private ArrayList<Point> findIsolatedNodes(ArrayList<Point> nodes) {
    return nodes.stream()
            .filter(node -> locateNeighbors(node, nodes, rangeLimit).isEmpty())
            .collect(Collectors.toCollection(ArrayList::new));
  }

  private ArrayList<Point> getCovers(List<Point> nodes) {
    Set<Point> covers = new HashSet<>();
    for (Point node : nodes) {
      covers.addAll(influencedByMap.getOrDefault(node, Collections.emptyList()));
    }
    return new ArrayList<>(covers);
  }

  private ArrayList<Point> getCoveredNodes(List<Point> covers) {
    Set<Point> covered = new HashSet<>();
    for (Point cover : covers) {
      covered.addAll(influenceMap.getOrDefault(cover, Collections.emptyList()));
    }
    return new ArrayList<>(covered);
  }
}