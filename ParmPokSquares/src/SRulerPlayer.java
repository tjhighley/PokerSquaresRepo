
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

/**
 * GreedyMCPlayer - a simple, greedy Monte Carlo implementation of the player
 * interface for PokerSquares. For each possible play, continues greedy play
 * with random possible card draws to a given depth limit (or game end). Having
 * sampled trajectories for all possible plays, the GreedyMCPlayer then selects
 * the play yielding the best average scoring potential in such Monte Carlo
 * simulation.
 *
 * Disclaimer: This example code is not intended as a model of efficiency.
 * (E.g., patterns from Knuth's Dancing Links algorithm (DLX) can provide faster
 * legal move list iteration/deletion/restoration.) Rather, this example code
 * illustrates how a player could be constructed. Note how time is simply
 * managed so as to not run out the play clock.
 *
 * Author: Todd W. Neller
 */
public class SRulerPlayer implements PokerSquaresPlayer {

    private final int SIZE = 5; // number of rows/columns in square grid
    private final int NUM_POS = SIZE * SIZE; // number of positions in square grid
    private final int NUM_CARDS = Card.NUM_CARDS; // number of cards in deck
    private Random random = new Random(); // pseudorandom number generator for Monte Carlo simulation 
    private int[] plays = new int[NUM_POS]; // positions of plays so far (index 0 through numPlays - 1) recorded as integers using row-major indices.
    // row-major indices: play (r, c) is recorded as a single integer r * SIZE + c (See http://en.wikipedia.org/wiki/Row-major_order)
    // From plays index [numPlays] onward, we maintain a list of yet unplayed positions.
    private int numPlays = 0; // number of Cards played into the grid so far
    private PokerSquaresPointSystem system; // point system
    private int depthLimit = 2; // default depth limit for Greedy Monte Carlo (MC) play
    private Card[][] grid = new Card[SIZE][SIZE]; // grid with Card objects or null (for empty positions)
    private Card[] simDeck = Card.getAllCards(); // a list of all Cards. As we learn the index of cards in the play deck,
    // we swap each dealt card to its correct index.  Thus, from index numPlays 
    // onward, we maintain a list of undealt cards for MC simulation.
    private int[][] legalPlayLists = new int[NUM_POS][NUM_POS]; // stores legal play lists indexed by numPlays (depth)
    // (This avoids constant allocation/deallocation of such lists during the greedy selections of MC simulations.)

    private HashMap<OurPokerHand, Integer> handVals = new HashMap<>();

    /**
     * Create a Greedy Monte Carlo player that simulates greedy play to depth 2.
     */
    public SRulerPlayer() {
    }

    /**
     * Create a Greedy Monte Carlo player that simulates greedy play to a given
     * depth limit.
     *
     * @param depthLimit depth limit for random greedy simulated play
     */
    public SRulerPlayer(int depthLimit) {
        this.depthLimit = depthLimit;
    }

    /* (non-Javadoc)
     * @see PokerSquaresPlayer#init()
     */
    @Override
    public void init() {
        // clear grid
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                grid[row][col] = null;
            }
        }
        // reset numPlays
        numPlays = 0;
        // (re)initialize list of play positions (row-major ordering)
        for (int i = 0; i < NUM_POS; i++) {
            plays[i] = i;
        }
    }

    /* (non-Javadoc)
     * @see PokerSquaresPlayer#getPlay(Card, long)
     */
    @Override
    public int[] getPlay(Card card, long millisRemaining) {
        /*
         * With this algorithm, the player chooses the legal play that has the highest expected score outcome.
         * This outcome is estimated as follows:
         *   For each move, many simulated greedy plays to the set depthLimit are performed and the (sometimes
         *     partially-filled) grid is scored.
         *   For each greedy play simulation, random undrawn cards are drawn in simulation and the greedy player
         *     picks a play position that maximizes the score (breaking ties randomly).
         *   After many such plays, the average score per simulated play is computed.  The play with the highest 
         *     average score is chosen (breaking ties randomly).   
         */

        // match simDeck to actual play event; in this way, all indices forward from the card contain a list of 
        //   undealt Cards in some permutation.
        int cardIndex = numPlays;
        while (!card.equals(simDeck[cardIndex])) {
            cardIndex++;
        }
        simDeck[cardIndex] = simDeck[numPlays];
        simDeck[numPlays] = card;

        if (numPlays == 0) { // trivial first play
            plays[0] = 0;
        }
        if (numPlays < 24) { // not the forced last play
            // compute average time per move evaluation
            int remainingPlays = NUM_POS - numPlays; // ignores triviality of last play to keep a conservative margin for game completion
            long millisPerPlay = millisRemaining / remainingPlays; // dividing time evenly with future getPlay() calls
            long millisPerMoveEval = millisPerPlay / remainingPlays; // dividing time evenly across moves now considered
            // copy the play positions (row-major indices) that are empty
            System.arraycopy(plays, numPlays, legalPlayLists[numPlays], 0, remainingPlays);
            double maxAverageScore = Double.NEGATIVE_INFINITY; // maximum average score found for moves so far
            ArrayList<Integer> bestPlays = new ArrayList<Integer>(); // all plays yielding the maximum average score 
            for (int i = 0; i < remainingPlays; i++) { // for each legal play position
                int play = legalPlayLists[numPlays][i];
                long startTime = System.currentTimeMillis();
                long endTime = startTime + millisPerMoveEval; // compute when MC simulations should end
                makePlay(card, play / SIZE, play % SIZE);  // play the card at the empty position
                int simCount = 0;
                int scoreTotal = 0;
                while (System.currentTimeMillis() < endTime) { // perform as many MC simulations as possible through the allotted time
                    // Perform a Monte Carlo simulation of greedy play to the depth limit or game end, whichever comes first.
                    scoreTotal += simGreedyPlay(depthLimit);  // accumulate MC simulation scores
                    simCount++; // increment count of MC simulations
                }
                undoPlay(); // undo the play under evaluation
                // update (if necessary) the maximum average score and the list of best plays
                double averageScore = (double) scoreTotal / simCount;
                if (averageScore >= maxAverageScore) {
                    if (averageScore > maxAverageScore) {
                        bestPlays.clear();
                    }
                    bestPlays.add(play);
                    maxAverageScore = averageScore;
                }
            }
            int bestPlay = bestPlays.get(random.nextInt(bestPlays.size())); // choose a best play (breaking ties randomly)
            // update our list of plays, recording the chosen play in its sequential position; all onward from numPlays are empty positions
            int bestPlayIndex = numPlays;
            while (plays[bestPlayIndex] != bestPlay) {
                bestPlayIndex++;
            }
            plays[bestPlayIndex] = plays[numPlays];
            plays[numPlays] = bestPlay;
        }
        int[] playPos = {plays[numPlays] / SIZE, plays[numPlays] % SIZE}; // decode it into row and column
        makePlay(card, playPos[0], playPos[1]); // make the chosen play (not undoing this time)
        return playPos; // return the chosen play
    }

    /**
     * From the chosen play, perform simulated Card draws and greedy placement
     * (depthLimit) iterations forward and return the resulting grid score.
     *
     * @param depthLimit - how many simulated greedy plays to perform
     * @return resulting grid score after greedy MC simulation to given
     * depthLimit
     */
    private int simGreedyPlay(int depthLimit) {
        if (numPlays > 24) System.out.println ("OOPS!  Start simGreedyPlay: numPlays: " + numPlays);
        if (depthLimit == 0) { // with zero depth limit, return current score
            //return system.getScore(grid);
            return evalGrid(grid);
        } else { // up to the non-zero depth limit or to game end, iteratively make the given number of greedy plays o
            int score = Integer.MIN_VALUE;
            int maxScore = Integer.MIN_VALUE;
            int depth = Math.min(depthLimit, NUM_POS - numPlays); // compute real depth limit, taking into account game end
            for (int d = 0; d < depth; d++) {
                // generate a random card draw
                int c = random.nextInt(NUM_CARDS - numPlays) + numPlays;
                Card card = simDeck[c];
                // iterate through legal plays and choose the best greedy play (see similar approach in getPlay)
                int remainingPlays = NUM_POS - numPlays;
                System.arraycopy(plays, numPlays, legalPlayLists[numPlays], 0, remainingPlays);
                maxScore = Integer.MIN_VALUE;
                ArrayList<Integer> bestPlays = new ArrayList<Integer>();
                for (int i = 0; i < remainingPlays; i++) {
                    int play = legalPlayLists[numPlays][i];
                    makePlay(card, play / SIZE, play % SIZE);
                    // score = system.getScore(grid);
                    score = evalGrid(grid);
                    if (score >= maxScore) {
                        if (score > maxScore) {
                            bestPlays.clear();
                        }
                        bestPlays.add(play);
                        maxScore = score;
                    }
                    undoPlay();
                }
                int bestPlay = bestPlays.get(random.nextInt(bestPlays.size()));
                makePlay(card, bestPlay / SIZE, bestPlay % SIZE);
            }
            // At this point, the last maxScore value is the end value of this Monte Carlo situation.
            // Undo MC plays.
            for (int d = 0; d < depth; d++) {
                undoPlay();
            }
            return maxScore;
        }
    }

    public void makePlay(Card card, int row, int col) {
        // match simDeck to event
        int cardIndex = numPlays;
        while (!card.equals(simDeck[cardIndex])) {
            cardIndex++;
        }
        simDeck[cardIndex] = simDeck[numPlays];
        simDeck[numPlays] = card;

        // update plays to reflect chosen play in sequence
        grid[row][col] = card;
        int play = row * SIZE + col;
        int j = 0;
        while (plays[j] != play) {
            j++;
        }
        plays[j] = plays[numPlays];
        plays[numPlays] = play;

        // increment the number of plays taken
        numPlays++;
    }

    public void undoPlay() { // undo the previous play
        numPlays--;
        int play = plays[numPlays];
        grid[play / SIZE][play % SIZE] = null;
    }

    @Override
    public void setPointSystem(PokerSquaresPointSystem system, long millis) {
        long startTime = System.currentTimeMillis();
        long endTime = startTime + millis;
        this.system = system;

        // Five-card hands just have the scoring system values
        handVals.put(OurPokerHand.ROYAL_FLUSH5, system.getHandScore(PokerHand.ROYAL_FLUSH));
        handVals.put(OurPokerHand.STRAIGHT_FLUSH5, system.getHandScore(PokerHand.STRAIGHT_FLUSH));
        handVals.put(OurPokerHand.FOUR_OF_A_KIND5, system.getHandScore(PokerHand.FOUR_OF_A_KIND));
        handVals.put(OurPokerHand.FULL_HOUSE5, system.getHandScore(PokerHand.FULL_HOUSE));
        handVals.put(OurPokerHand.FLUSH5, system.getHandScore(PokerHand.FLUSH));
        handVals.put(OurPokerHand.STRAIGHT5, system.getHandScore(PokerHand.STRAIGHT));
        handVals.put(OurPokerHand.THREE_OF_A_KIND5, system.getHandScore(PokerHand.THREE_OF_A_KIND));
        handVals.put(OurPokerHand.TWO_PAIR5, system.getHandScore(PokerHand.TWO_PAIR));
        handVals.put(OurPokerHand.ONE_PAIR5, system.getHandScore(PokerHand.ONE_PAIR));
        handVals.put(OurPokerHand.HIGH_CARD5, system.getHandScore(PokerHand.HIGH_CARD));

        // Initialize values for partial hands as max potential score
        handVals.put(OurPokerHand.ROYAL_FLUSH4, Math.max(system.getHandScore(PokerHand.FLUSH),
                Math.max(system.getHandScore(PokerHand.STRAIGHT_FLUSH),
                        Math.max(system.getHandScore(PokerHand.ROYAL_FLUSH),
                                Math.max(system.getHandScore(PokerHand.HIGH_CARD),
                                        system.getHandScore(PokerHand.ONE_PAIR))))));
        handVals.put(OurPokerHand.STRAIGHT_FLUSH4, Math.max(system.getHandScore(PokerHand.FLUSH),
                Math.max(system.getHandScore(PokerHand.STRAIGHT_FLUSH),
                        Math.max(system.getHandScore(PokerHand.HIGH_CARD),
                                system.getHandScore(PokerHand.ONE_PAIR)))));
        handVals.put(OurPokerHand.INSIDE_STRAIGHT_FLUSH4, handVals.get(OurPokerHand.STRAIGHT_FLUSH4));
        handVals.put(OurPokerHand.FOUR_OF_A_KIND4, system.getHandScore(PokerHand.FOUR_OF_A_KIND));
        handVals.put(OurPokerHand.FLUSH4, Math.max(system.getHandScore(PokerHand.FLUSH),
                Math.max(system.getHandScore(PokerHand.HIGH_CARD),
                        system.getHandScore(PokerHand.ONE_PAIR))));
        handVals.put(OurPokerHand.STRAIGHT4, Math.max(system.getHandScore(PokerHand.STRAIGHT),
                Math.max(system.getHandScore(PokerHand.HIGH_CARD),
                        system.getHandScore(PokerHand.ONE_PAIR))));
        handVals.put(OurPokerHand.INSIDE_STRAIGHT4, handVals.get(OurPokerHand.STRAIGHT4));
        handVals.put(OurPokerHand.THREE_OF_A_KIND4, Math.max(system.getHandScore(PokerHand.THREE_OF_A_KIND),
                Math.max(system.getHandScore(PokerHand.FOUR_OF_A_KIND),
                        system.getHandScore(PokerHand.FULL_HOUSE))));
        handVals.put(OurPokerHand.TWO_PAIR4, Math.max(system.getHandScore(PokerHand.FULL_HOUSE),
                system.getHandScore(PokerHand.TWO_PAIR)));
        handVals.put(OurPokerHand.ONE_PAIR4, Math.max(system.getHandScore(PokerHand.ONE_PAIR),
                Math.max(system.getHandScore(PokerHand.TWO_PAIR),
                        system.getHandScore(PokerHand.THREE_OF_A_KIND))));
        handVals.put(OurPokerHand.HIGH_CARD4, Math.max(system.getHandScore(PokerHand.HIGH_CARD),
                system.getHandScore(PokerHand.ONE_PAIR)));

        // 3-card hands
        handVals.put(OurPokerHand.ROYAL_FLUSH3, Math.max(handVals.get(OurPokerHand.ROYAL_FLUSH4),
                Math.max(handVals.get(OurPokerHand.STRAIGHT4), Math.max(handVals.get(OurPokerHand.FLUSH4),
                                Math.max(handVals.get(OurPokerHand.HIGH_CARD4),
                                        Math.max(handVals.get(OurPokerHand.ONE_PAIR4),
                                                handVals.get(OurPokerHand.STRAIGHT_FLUSH4)))))));
        handVals.put(OurPokerHand.STRAIGHT_FLUSH3,
                Math.max(handVals.get(OurPokerHand.STRAIGHT4), Math.max(handVals.get(OurPokerHand.FLUSH4),
                                Math.max(handVals.get(OurPokerHand.HIGH_CARD4),
                                        (Math.max(handVals.get(OurPokerHand.ONE_PAIR4),
                                                handVals.get(OurPokerHand.STRAIGHT_FLUSH4)))))));
        handVals.put(OurPokerHand.INSIDE_STRAIGHT_FLUSH3, handVals.get(OurPokerHand.STRAIGHT_FLUSH3));
        handVals.put(OurPokerHand.FLUSH3, Math.max(handVals.get(OurPokerHand.FLUSH4),
                Math.max(handVals.get(OurPokerHand.HIGH_CARD4),
                        handVals.get(OurPokerHand.ONE_PAIR4))));
        handVals.put(OurPokerHand.STRAIGHT3, Math.max(handVals.get(OurPokerHand.STRAIGHT4),
                Math.max(handVals.get(OurPokerHand.HIGH_CARD4),
                        handVals.get(OurPokerHand.ONE_PAIR4))));
        handVals.put(OurPokerHand.INSIDE_STRAIGHT3, handVals.get(OurPokerHand.STRAIGHT3));
        handVals.put(OurPokerHand.THREE_OF_A_KIND3, Math.max(handVals.get(OurPokerHand.THREE_OF_A_KIND4),
                handVals.get(OurPokerHand.FOUR_OF_A_KIND4)));
        handVals.put(OurPokerHand.ONE_PAIR3, Math.max(handVals.get(OurPokerHand.THREE_OF_A_KIND4),
                Math.max(handVals.get(OurPokerHand.ONE_PAIR4),
                        handVals.get(OurPokerHand.TWO_PAIR4))));
        handVals.put(OurPokerHand.HIGH_CARD3, Math.max(handVals.get(OurPokerHand.HIGH_CARD4),
                handVals.get(OurPokerHand.ONE_PAIR4)));

        // 2-card hands
        handVals.put(OurPokerHand.ROYAL_FLUSH2, Math.max(handVals.get(OurPokerHand.ROYAL_FLUSH3),
                Math.max(handVals.get(OurPokerHand.STRAIGHT3), Math.max(handVals.get(OurPokerHand.FLUSH3),
                                Math.max(handVals.get(OurPokerHand.HIGH_CARD3),
                                        Math.max(handVals.get(OurPokerHand.ONE_PAIR3),
                                                handVals.get(OurPokerHand.STRAIGHT_FLUSH3)))))));
        handVals.put(OurPokerHand.STRAIGHT_FLUSH2,
                Math.max(handVals.get(OurPokerHand.STRAIGHT3), Math.max(handVals.get(OurPokerHand.FLUSH3),
                                Math.max(handVals.get(OurPokerHand.HIGH_CARD3),
                                        (Math.max(handVals.get(OurPokerHand.ONE_PAIR3),
                                                handVals.get(OurPokerHand.STRAIGHT_FLUSH3)))))));
        handVals.put(OurPokerHand.INSIDE_STRAIGHT_FLUSH2, handVals.get(OurPokerHand.STRAIGHT_FLUSH2));
        handVals.put(OurPokerHand.FLUSH2, Math.max(handVals.get(OurPokerHand.FLUSH3),
                Math.max(handVals.get(OurPokerHand.HIGH_CARD3),
                        handVals.get(OurPokerHand.ONE_PAIR3))));
        handVals.put(OurPokerHand.STRAIGHT2, Math.max(handVals.get(OurPokerHand.STRAIGHT3),
                Math.max(handVals.get(OurPokerHand.HIGH_CARD3),
                        handVals.get(OurPokerHand.ONE_PAIR3))));
        handVals.put(OurPokerHand.INSIDE_STRAIGHT2, handVals.get(OurPokerHand.STRAIGHT2));
        handVals.put(OurPokerHand.ONE_PAIR2, Math.max(handVals.get(OurPokerHand.THREE_OF_A_KIND3),
                handVals.get(OurPokerHand.ONE_PAIR3)));
        handVals.put(OurPokerHand.HIGH_CARD2, Math.max(handVals.get(OurPokerHand.HIGH_CARD3),
                handVals.get(OurPokerHand.ONE_PAIR3)));

        // Zero or one card hands
        handVals.put(OurPokerHand.ONE_CARD, Math.max(handVals.get(OurPokerHand.ROYAL_FLUSH2),
                Math.max(handVals.get(OurPokerHand.STRAIGHT2), Math.max(handVals.get(OurPokerHand.FLUSH2),
                                Math.max(handVals.get(OurPokerHand.HIGH_CARD2),
                                        Math.max(handVals.get(OurPokerHand.ONE_PAIR2),
                                                handVals.get(OurPokerHand.STRAIGHT_FLUSH2)))))));
        handVals.put(OurPokerHand.ZERO_CARDS, handVals.get(OurPokerHand.ONE_CARD));

        adjustHandVals(endTime);
    }

    /**
     * Use stochastic ruler to adjust the partial hand values.
     */
    private void adjustHandVals(long endTime) {
        HashMap<OurPokerHand, Integer> current
                = (HashMap<OurPokerHand, Integer>) handVals.clone();

        // Save best seen
        HashMap<OurPokerHand, Integer> best
                = (HashMap<OurPokerHand, Integer>) handVals.clone();
        int bestValue, worstValue;
        bestValue = worstValue = srEvaluate(handVals);

        // Find least valuable hand and max value per partial hand
        int leastValHand = Integer.MAX_VALUE;
        HashMap<OurPokerHand, Integer> handValCap = 
                (HashMap<OurPokerHand, Integer>) handVals.clone();
        for (OurPokerHand thisHand : handVals.keySet()) {
            if (handVals.get(thisHand) < leastValHand)
                leastValHand = handVals.get(thisHand);            
        }

        long startLoopTime = System.currentTimeMillis();
        endTime = endTime - 1000; // Shorten time for testing
        int iter = 0;
        while (System.currentTimeMillis() < endTime) {
            iter++;
            //System.out.println(current);
            // Find a neighbor
            HashMap<OurPokerHand, Integer> neighbor
                    = (HashMap<OurPokerHand, Integer>) current.clone();

            for (OurPokerHand thisHand : neighbor.keySet()) {                
                if (thisHand.ordinal() >= 10
                        && random.nextInt(100) > ((double) (System.currentTimeMillis() - startLoopTime) / (endTime - startLoopTime) * 50)) {
                    int interval = (int) (((double) (System.currentTimeMillis() - startLoopTime) / (endTime - startLoopTime)) * 20)  + 2;
                    int newVal = neighbor.get(thisHand) + random.nextInt(interval) - (interval / 2);                    
                    newVal = Math.min(newVal, handValCap.get(thisHand));
                    newVal = Math.max(newVal, leastValHand);
                    neighbor.put(thisHand, newVal);
                }
            }

            // Evaluate the neighbor
            int neighborVal = srEvaluate(neighbor);

            // If neighbor is good, use the neighbor
            int theta = random.nextInt(bestValue - worstValue + 1) + worstValue;
            if (neighborVal > theta) {
                current = (HashMap<OurPokerHand, Integer>) neighbor;
                //System.out.println("New current: " + current);
            }

            // If neighbor is best, save best
            if (neighborVal > (bestValue + srEvaluate(best)) / 2) {
                best = (HashMap<OurPokerHand, Integer>) neighbor.clone();
                bestValue = neighborVal;
                System.out.println("******************************************************** iter: " + iter + "\t%time elapsed: " + 100*(double) (System.currentTimeMillis() - startLoopTime) / (endTime - startLoopTime));
                System.out.println("New best: " + best);
            }

            if (neighborVal < worstValue) {
                worstValue = neighborVal;
            }
        }
        handVals = (HashMap<OurPokerHand, Integer>) best;
        System.out.println("best");
        System.out.println(best);
    }

    private int srEvaluate(HashMap<OurPokerHand, Integer> values) {
        HashMap<OurPokerHand, Integer> original = handVals;
        handVals = values;        
        int total = 0;
        init();
        for (int i = 0; i < 20; i++) {            
            total += simGreedyPlay(25);
            init();
        }
        handVals = original;
        return total / 20;
    }

    /* (non-Javadoc)
     * @see PokerSquaresPlayer#getName()
     */
    @Override
    public String getName() {
        return "SRulerPlayer" + depthLimit;
    }

    private int evalGrid(Card[][] grid) {
        int[] handScores = new int[2 * SIZE];
        for (int row = 0; row < SIZE; row++) {
            Card[] hand = new Card[SIZE];
            for (int col = 0; col < SIZE; col++) {
                hand[col] = grid[row][col];
            }
            handScores[row] = handVals.get(OurPokerHand.getPokerHand(hand));
        }
        for (int col = 0; col < SIZE; col++) {
            Card[] hand = new Card[SIZE];
            for (int row = 0; row < SIZE; row++) {
                hand[row] = grid[row][col];
            }
            handScores[SIZE + col] = handVals.get(OurPokerHand.getPokerHand(hand));
        }

        int totalScore = 0;
        for (int handScore : handScores) {
            totalScore += handScore;
        }
        return totalScore;
    }

}
