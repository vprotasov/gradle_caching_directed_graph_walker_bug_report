package org.gradle.internal.graph;

import java.util.*;

public class AdjacencyList {
    //public List<List<Integer>> adjacencyList;
    public int[][] adjacencyListAsArray;

    private static final int[] EMPTY = {};
    private int idCounter = 0;
    final int initialCapacity = 1001;
    public final Map<String, Integer> itemToId = new HashMap<>(initialCapacity);
    public final List<String> idToItem = new ArrayList<>(initialCapacity);

    public AdjacencyList(Map<String, List<String>> map) {
        Map<Integer, List<Integer>> adjMap = new HashMap<>(map.size());

        for (String vertex : map.keySet()) {
            int vertexId = mapToId(vertex);

            adjMap.put(vertexId, buildAdjacencyRow(map.get(vertex)));
        }

        int V = idCounter;
        //adjacencyList = new ArrayList<>(V);
        adjacencyListAsArray = new int[V][];

        for (int i = 0; i < V; i++) {
            var adjacency = adjMap.get(i);

            //adjacencyList.add(adjacency == null || adjacency.size() == 0 ? Collections.EMPTY_LIST : adjacency);
            adjacencyListAsArray[i] = (adjacency == null || adjacency.size() == 0 ? EMPTY : adjacency.stream().mapToInt(Integer::valueOf).toArray());
        }
    }

    private List<Integer> buildAdjacencyRow(List<String> edges) {
        int edgesCount = edges.size();
        List<Integer> adjacency = new ArrayList<>(edgesCount);

        for (int i = 0; i < edgesCount; i++)
            adjacency.add(mapToId(edges.get(i)));

        return adjacency;
    }

    private int mapToId(String item) {
        Integer id = itemToId.get(item);

        if (id == null) {
            itemToId.put(item, idCounter);
            idToItem.add(item);
            idCounter++;

            return idCounter - 1;
        }

        return id;
    }

    public String idToItem(int i) {
        return idToItem.get(i);
    }
}
