package org.gradle.internal.graph;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CachingDirectedGraphWalkerTest {

    private Map<String, List<String>> loadFromFile(String name) throws IOException {
        File file = new File(getClass().getClassLoader().getResource(name).getFile());

        Map<String, List<String>> map = new HashMap<>();  // normally we would want LinkedHashMap
        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] items = line.split(" ");
                if (items.length <= 1) // ignore empty lines and lines with only one item.
                    continue;
                String vertex = items[0];
                ArrayList<String> list = new ArrayList(items.length - 1);
                for (int i = 1; i < items.length; i++)
                    list.add(items[i]);

                map.put(vertex, list);
            }
        }

        return map;
    }

    @ParameterizedTest
    @ValueSource(strings = {"inputNoCycle.txt", "inputCycle.txt", "input10k_nodes_1_cycle.txt"})
    public void findCycles(String filename) throws IOException {
        Map<String, List<String>> map = loadFromFile(filename);

        CachingDirectedGraphWalker<String, Void> walker = new CachingDirectedGraphWalker((node, values, connectedNodes) -> {
            List<String> list = map.get(node);

            if (list != null)
                connectedNodes.addAll(list);
        });

        walker.add(map.keySet());

        List<Set<String>> cycles = walker.findCycles();

        System.out.println();
        System.out.println("cycles count=" + cycles.size());

        int[][] adjacencyList = new AdjacencyList(map).adjacencyListAsArray;

        List<List<Integer>> tarjanCycles = new TarjanRecursive().stronglyConnectedComponents(adjacencyList);

        CachingDirectedGraphWalker<Integer, Void> walker2 = new CachingDirectedGraphWalker((node, values, connectedNodes) -> {
            connectedNodes.addAll(Arrays.asList(adjacencyList[(int) node]));
        });

        for (int i = 0; i < adjacencyList.length; i++)
            walker2.add(i);

        List<Set<String>> cycles2 = walker.findCycles();

        assertEquals(cycles.size(), cycles2.size());
        assertEquals(cycles.size(), tarjanCycles.size());

        if (!cycles.isEmpty()) {
            Set<String> cycle = cycles.get(0);
            Set<String> cycle2 = cycles2.get(0);
            List<Integer> tarjanCycle = tarjanCycles.get(0);

            assertEquals(cycle.size(), cycle2.size());
            assertEquals(cycle.size(), tarjanCycle.size());
            assertEquals(cycle.size(), new HashSet<>(tarjanCycle).size());

            if (cycles.size() == 1) {
                for (String key : map.keySet()) {
                    CachingDirectedGraphWalker<String, Void> singleWalker = new CachingDirectedGraphWalker((node, values, connectedNodes) -> {
                        List<String> list = map.get(node);

                        if (list != null)
                            connectedNodes.addAll(list);
                    });

                    singleWalker.add(key);

                    List<Set<String>> cycles0 = singleWalker.findCycles();
                    if (cycles0.size() == 1) {
                        assertEquals(cycles0.get(0).size(), cycle.size(), "key=" + key); // expected: <2939> but was: <2943>
                        assertEquals(cycles0.get(0).size(), tarjanCycle.size(), "key=" + key); // expected: <2939> but was: <2943>
                    }
                }
            }

//            System.out.println("first cycle size=" + cycle.size());
//            String[] arr = cycle.toArray(new String[0]);
//            Arrays.sort(arr);
//            System.out.println(Arrays.toString(arr));
        }
    }


    @ParameterizedTest
    @ValueSource(strings = {"inputNoCycle.txt", "inputCycle.txt", "input10k_nodes_1_cycle.txt"})
    public void findValues(String filename) throws IOException {
        Map<String, List<String>> map = loadFromFile(filename);

        CachingDirectedGraphWalker<String, String> walker1 = new CachingDirectedGraphWalker((node, values, connectedNodes) -> {
            values.add(node);

            List<String> list = map.get(node);

            if (list != null)
                connectedNodes.addAll(list);
        });

        Set<String> values1 = walker1.add(map.keySet()).findValues();

        CachingDirectedGraphWalker<String, String> walker2 = new CachingDirectedGraphWalker((node, values, connectedNodes) -> {
            values.add(node);

            List<String> list = map.get(node);

            if (list != null)
                connectedNodes.addAll(list);
        });

        String[] keys = map.keySet().toArray(new String[0]);

        Arrays.sort(keys);

        Set<String> values2 = walker2.add(keys).findValues();

        assertEquals(values1.size(), values2.size());
    }
}
