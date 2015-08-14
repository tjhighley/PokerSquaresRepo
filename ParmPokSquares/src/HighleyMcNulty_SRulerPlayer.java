// HighleyMcNulty_SRulerPlayer
// Timothy Highley and Zachary McNulty
// Player for NSGC: Poker Squares


public class HighleyMcNulty_SRulerPlayer extends HighleyMcNulty_OurPlayer implements PokerSquaresPlayer {

    public HighleyMcNulty_SRulerPlayer() {
    }

    /**
     * Create a SRulerPlayer player that simulates greedy play to a given depth
     * limit.
     *
     * @param depthLimit depth limit for random sRuler simulated play
     */
    public HighleyMcNulty_SRulerPlayer(int depthLimit) {
        this();
        this.depthLimit = depthLimit;
    }

    /**
     * Use stochastic ruler to adjust the partial hand values before the initial
     * 5 minutes has ended
     */
    protected void adjustHandVals(long endTime) {
        // current holds the current value of handVals
        HandValues current = handVals.deepClone();

        // Save best value seen
        HandValues bestHandVals = handVals.deepClone();
        double bestValue, worstValue;
        bestValue = worstValue = srEvaluate(handVals, 100);
        System.out.println("Initial best/worst: " + bestValue);
        
        int currentM = 2; // # comparisons required before accepting a new set of handVals
        int bumpM = 100; // Iteration at which to next increase currentM

        // Find least valuable hand and max value per partial hand
        int leastValHand = Integer.MAX_VALUE;
        HandValues handValCap = handVals.deepClone();
        for (OurPokerHand thisHand : OurPokerHand.values()) {
            if (handVals.get(0, thisHand) < leastValHand) {
                leastValHand = handVals.get(0, thisHand);
            }
        }

        // finding "neighbors" to give new values for partial hands
        long startLoopTime = System.currentTimeMillis();
        endTime = endTime - 3000; // 3 seconds to make sure we end on time
        int iter = 0;
        while (System.currentTimeMillis() < endTime) {
            iter++;
            if (iter % 1000 == 0) 
                System.out.println(iter + "\t%time elapsed: " + 100 * (double) (System.currentTimeMillis() - startLoopTime) / (endTime - startLoopTime));
            // Find a neighbor
            HandValues neighbor = current.deepClone();

            for (int i = 0; i < neighbor.size(); i++) {
                for (OurPokerHand thisHand : OurPokerHand.values()) {
                    // At the beginning, change nearly everything.  Near the end, only change half.
                    if (random.nextInt(100) > ((double) (System.currentTimeMillis() - startLoopTime) / (endTime - startLoopTime) * 50)) {
                        // sets new values for partial hands to test the "neighbors"
                        //int interval = (int) (((double) (System.currentTimeMillis() - startLoopTime) / (endTime - startLoopTime)) * 20) + 2;
                        int interval = 4;
                        int newVal = neighbor.get(i*10+1, thisHand) + random.nextInt(interval) - (interval / 2);
                        // the new values for the neighbor can only ever be modified between -128-127
                        newVal = Math.min(newVal, 127);
                        newVal = Math.max(newVal, -128);
                        // assigns a value to the newly created neighbor
                        neighbor.put(i*10+1, thisHand, newVal);
                    }
                }
            }
            
            boolean acceptNeighbor = true;
            double neighborVal = 0.0, neighborValTotal = 0.0;
            int neighborValCount = 0;
            
            if (iter == bumpM) {
                currentM++;
                bumpM *= 5;
                System.out.println("New M: " + currentM);
            }
            for (int i = 0; i < currentM; i++) {
                // Evaluate the neighbor to test if it is "good"
                neighborVal = srEvaluate(neighbor, 10);
                neighborValTotal += neighborVal;
                neighborValCount++;
                int theta = random.nextInt((int) (bestValue - worstValue + 1)) + (int) worstValue;
                
                if (theta > neighborVal) {
                    acceptNeighbor = false;                
                    break;
                }
            }

            // If neighbor returns good values, use the neighbor
            if (acceptNeighbor) {
                System.out.print(".");
                current = (HandValues) neighbor;
            }

            // If neighbor returns the best value, save it
            if (acceptNeighbor
                    && ((double)neighborValTotal/neighborValCount) > srEvaluate(bestHandVals, 10) 
                    && (neighborVal = srEvaluate(neighbor, 500)) > srEvaluate(bestHandVals, 500)) {
                // saves the best hand values
                bestHandVals = neighbor.deepClone();
                bestValue = neighborVal;
                //System.out.println("******************************************************** iter: " + iter + "\t%time elapsed: " + 100 * (double) (System.currentTimeMillis() - startLoopTime) / (endTime - startLoopTime));
                //System.out.println("New best: " + bestValue + " \n" + bestHandVals);
            }
            

            if (neighborVal < worstValue) {
                worstValue = neighborVal;
            }
        }
        handVals = (HandValues) bestHandVals;

        System.out.println("best:\n");
        System.out.println(bestHandVals);
    }

    private double srEvaluate(HandValues values, int iter) {
        // saves the original hand values
        HandValues original = handVals;
        // takes in the new hand values
        handVals = values;
        int total = 0;
        init();
        // simulates a game of pokersquares using the neighbors value
        for (int i = 0; i < iter; i++) {
            int result = simGreedyPlay(25);
            total += result;
            init();
        }
        // resets the hand values toc the original
        handVals = original;
        return (double) total / iter;
    }

    /* (non-Javadoc)
     * @see PokerSquaresPlayer#getName()
     */
    @Override
    public String getName() {
        return "SRulerPlayer" + depthLimit;
    }
}
