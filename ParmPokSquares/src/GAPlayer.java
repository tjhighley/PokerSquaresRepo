public class GAPlayer extends OurPlayer implements PokerSquaresPlayer {
   
    private int popSize;
    private int numElites;
    private int numMutations = 2;
    private boolean crossoverOn = true;
    
    /**
     * Create a Greedy Monte Carlo player that simulates greedy play to depth 2.
     */
    public GAPlayer() {
    }

    /**
     * Create a Greedy Monte Carlo player that simulates greedy play to a given
     * depth limit.
     *
     * @param depthLimit depth limit for random greedy simulated play
     */
    public GAPlayer(int depthLimit, int popSize) {        
        this.depthLimit = depthLimit;
        this.popSize = popSize;
        this.numElites = popSize / 20;
    }
    
    public GAPlayer(int depthLimit, int popSize, int numMutations, boolean crossoverOn) {
        this(depthLimit, popSize);
        this.numMutations = numMutations;
        this.crossoverOn = crossoverOn;
    }

    /**
     * Use genetic algorithm to adjust the partial hand values.
     */
    protected void adjustHandVals(long endTime) {
        int iter = 0;

        // Create initial population
        // 1. Unchanged (half)     
        // 2. Truly random (all others)
        GAPopulation population = new GAPopulation(popSize);
        for (int i = 0; i < popSize; i++) {
            population.add(i, handVals.deepClone());
        }
        System.out.println(population.toString(0));
        
        for (int i = popSize / 2; i < popSize; i++) {
            population.add(i, getRandomHandValues(handVals));
//            System.out.println("Initial population " + i +": ");
//            System.out.println(population.toString(i));
        }
        long startLoopTime = System.currentTimeMillis();
        endTime = endTime - 3000; // Shorten time for testing
                
        // Generate new generations
        while (System.currentTimeMillis() < endTime) {
            iter++;
            if (iter % 10 == 0) {
                System.out.println(iter + "\t%time elapsed: " + 100 * (double) (System.currentTimeMillis() - startLoopTime) / (endTime - startLoopTime));
                System.out.println(population.toString(0));
            }

            // Evaluate each member of this generation
            for (int i = 0; i < popSize; i++) {
                population.updateValue(i, evalPartialHands(population.get(i), 50));
            }

            // Sort
            population.sort();

            // New generation
            GAPopulation newGeneration = new GAPopulation(popSize);

            // Keep the best unchanged
            for (int i = 0; i < numElites; i++) {
                newGeneration.add(i, population.get(i));
            }

            // popSize Children in next generation
            for (int i = numElites; i < popSize; i++) {
                //Choose two parents (lower-ranked are only in the pool for the early children)
                int parent1 = random.nextInt(popSize - i + 1);
                int parent2 = random.nextInt(popSize - i + 1);

                if (crossoverOn) {
                    // Combine the parents 50/50 into a child                
                    HandValues newChild
                        = get5050Child(population.get(parent1), population.get(parent2));

                    newGeneration.add(i, newChild);
                } else {
                    // 
                    newGeneration.add(i, population.get(i - numElites));
                }
                
            }

            // Mutation
            // For each new child (except elites), choose handvals and change at random
            if (numMutations > 0) {
                for (int i = numElites; i < popSize; i++) {
                    for (int j = 0; j < numMutations; j++) {
                        int handIndex = random.nextInt(OurPokerHand.INSIDE_STRAIGHT_FLUSH2.ordinal() - OurPokerHand.HIGH_CARD5.ordinal())
                                + OurPokerHand.HIGH_CARD5.ordinal();
                        int playIndex = random.nextInt(25);
                        HandValues current = newGeneration.get(i);
                        int change = random.nextInt(5) - 2;

                        int newVal = current.get(playIndex, OurPokerHand.values()[handIndex]) + change;
                        newVal = Math.min(127, newVal);
                        newVal = Math.max(-128, newVal);
                        current.put(playIndex, OurPokerHand.values()[handIndex], newVal);
                    }
                }
            }

            population = newGeneration;
        }
        handVals = population.get(0);
    }

    private HandValues get5050Child(HandValues parent1, HandValues parent2) {
        HandValues result = (HandValues) parent1.deepClone();

        for (int i = OurPokerHand.HIGH_CARD5.ordinal();
                i <= OurPokerHand.ZERO_CARDS.ordinal(); i++) {
            if (random.nextBoolean()) {
                for (int j = 0; j < 3; j++) {
                    result.put(j*10 + 1, OurPokerHand.values()[i], parent2.get(j * 10 + 1, OurPokerHand.values()[i]));
                }
            }
        }
        return result;
    }

    private HandValues getRandomHandValues(HandValues initial) {
        HandValues result = (HandValues) initial.deepClone();

        for (int i = OurPokerHand.HIGH_CARD5.ordinal();
                i <= OurPokerHand.ZERO_CARDS.ordinal(); i++) {
            int change = random.nextInt(21) - 10;
            for (int j = 0; j < 3; j++) {
                int newVal = initial.get(j*10 + 1, OurPokerHand.values()[i]) + change;
                newVal = Math.min(127, newVal);
                newVal = Math.max(-128, newVal);
                result.put(j*10 + 1, OurPokerHand.values()[i], newVal);
            }
        }        
        return result;
    }

    private double evalPartialHands(HandValues values, int iter) {
        HandValues original = handVals;
        handVals = values;
        int total = 0;
        init();
        for (int i = 0; i < iter; i++) {
            int result = simGreedyPlay(25);
            total += result;
            //System.out.print("Play " + i + ": " + result + "; ");
            init();
        }
        //System.out.println();
        handVals = original;
        return total / (double) iter;
    }

    /* (non-Javadoc)
     * @see PokerSquaresPlayer#getName()
     */
    @Override
    public String getName() {
        return "GAPlayer" + depthLimit + "_" + popSize + "_" + numMutations;
    }
}
