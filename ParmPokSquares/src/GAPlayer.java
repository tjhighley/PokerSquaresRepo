
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class GAPlayer implements PokerSquaresPlayer {

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
    
    private int popSize;
    private boolean mutationOn = true;
    private boolean crossoverOn = true;
    
    private HandValues handVals = new HandValues();

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
    }
    
    public GAPlayer(int depthLimit, int popSize, boolean mutationOn, boolean crossoverOn) {
        this(depthLimit, popSize);
        this.mutationOn = mutationOn;
        this.crossoverOn = crossoverOn;
    }

    @Override
    public void setPointSystem(PokerSquaresPointSystem system, long millis) {
        long startTime = System.currentTimeMillis();
        long endTime = startTime + millis;
        this.system = system;

        // Five-card hands just have the scoring system values
        handVals.put(0, OurPokerHand.ROYAL_FLUSH5, system.getHandScore(PokerHand.ROYAL_FLUSH));
        handVals.put(0, OurPokerHand.STRAIGHT_FLUSH5, system.getHandScore(PokerHand.STRAIGHT_FLUSH));
        handVals.put(0, OurPokerHand.FOUR_OF_A_KIND5, system.getHandScore(PokerHand.FOUR_OF_A_KIND));
        handVals.put(0, OurPokerHand.FULL_HOUSE5, system.getHandScore(PokerHand.FULL_HOUSE));
        handVals.put(0, OurPokerHand.FLUSH5, system.getHandScore(PokerHand.FLUSH));
        handVals.put(0, OurPokerHand.STRAIGHT5, system.getHandScore(PokerHand.STRAIGHT));
        handVals.put(0, OurPokerHand.THREE_OF_A_KIND5, system.getHandScore(PokerHand.THREE_OF_A_KIND));
        handVals.put(0, OurPokerHand.TWO_PAIR5, system.getHandScore(PokerHand.TWO_PAIR));
        handVals.put(0, OurPokerHand.ONE_PAIR5, system.getHandScore(PokerHand.ONE_PAIR));
        handVals.put(0, OurPokerHand.HIGH_CARD5, system.getHandScore(PokerHand.HIGH_CARD));

        // Initialize values for partial hands as max potential score
        handVals.put(0, OurPokerHand.ROYAL_FLUSH4, Math.max(system.getHandScore(PokerHand.FLUSH),
                Math.max(system.getHandScore(PokerHand.STRAIGHT_FLUSH),
                        Math.max(system.getHandScore(PokerHand.ROYAL_FLUSH),
                                Math.max(system.getHandScore(PokerHand.HIGH_CARD),
                                        system.getHandScore(PokerHand.ONE_PAIR))))));
        handVals.put(0, OurPokerHand.STRAIGHT_FLUSH4, Math.max(system.getHandScore(PokerHand.FLUSH),
                Math.max(system.getHandScore(PokerHand.STRAIGHT_FLUSH),
                        Math.max(system.getHandScore(PokerHand.HIGH_CARD),
                                system.getHandScore(PokerHand.ONE_PAIR)))));
        handVals.put(0, OurPokerHand.INSIDE_STRAIGHT_FLUSH4, handVals.get(0, OurPokerHand.STRAIGHT_FLUSH4));
        handVals.put(0, OurPokerHand.FOUR_OF_A_KIND4, system.getHandScore(PokerHand.FOUR_OF_A_KIND));
        handVals.put(0, OurPokerHand.FLUSH4, Math.max(system.getHandScore(PokerHand.FLUSH),
                Math.max(system.getHandScore(PokerHand.HIGH_CARD),
                        system.getHandScore(PokerHand.ONE_PAIR))));
        handVals.put(0, OurPokerHand.STRAIGHT4, Math.max(system.getHandScore(PokerHand.STRAIGHT),
                Math.max(system.getHandScore(PokerHand.HIGH_CARD),
                        system.getHandScore(PokerHand.ONE_PAIR))));
        handVals.put(0, OurPokerHand.INSIDE_STRAIGHT4, handVals.get(0, OurPokerHand.STRAIGHT4));
        handVals.put(0, OurPokerHand.THREE_OF_A_KIND4, Math.max(system.getHandScore(PokerHand.THREE_OF_A_KIND),
                Math.max(system.getHandScore(PokerHand.FOUR_OF_A_KIND),
                        system.getHandScore(PokerHand.FULL_HOUSE))));
        handVals.put(0, OurPokerHand.TWO_PAIR4, Math.max(system.getHandScore(PokerHand.FULL_HOUSE),
                system.getHandScore(PokerHand.TWO_PAIR)));
        handVals.put(0, OurPokerHand.ONE_PAIR4, Math.max(system.getHandScore(PokerHand.ONE_PAIR),
                Math.max(system.getHandScore(PokerHand.TWO_PAIR),
                        system.getHandScore(PokerHand.THREE_OF_A_KIND))));
        handVals.put(0, OurPokerHand.HIGH_CARD4, Math.max(system.getHandScore(PokerHand.HIGH_CARD),
                system.getHandScore(PokerHand.ONE_PAIR)));

        // 3-card hands
        handVals.put(0, OurPokerHand.ROYAL_FLUSH3, Math.max(handVals.get(0, OurPokerHand.ROYAL_FLUSH4),
                Math.max(handVals.get(0, OurPokerHand.STRAIGHT4), Math.max(handVals.get(0, OurPokerHand.FLUSH4),
                                Math.max(handVals.get(0, OurPokerHand.HIGH_CARD4),
                                        Math.max(handVals.get(0, OurPokerHand.ONE_PAIR4),
                                                handVals.get(0, OurPokerHand.STRAIGHT_FLUSH4)))))));
        handVals.put(0, OurPokerHand.STRAIGHT_FLUSH3,
                Math.max(handVals.get(0, OurPokerHand.STRAIGHT4), Math.max(handVals.get(0, OurPokerHand.FLUSH4),
                                Math.max(handVals.get(0, OurPokerHand.HIGH_CARD4),
                                        (Math.max(handVals.get(0, OurPokerHand.ONE_PAIR4),
                                                handVals.get(0, OurPokerHand.STRAIGHT_FLUSH4)))))));
        handVals.put(0, OurPokerHand.INSIDE_STRAIGHT_FLUSH3, handVals.get(0, OurPokerHand.STRAIGHT_FLUSH3));
        handVals.put(0, OurPokerHand.FLUSH3, Math.max(handVals.get(0, OurPokerHand.FLUSH4),
                Math.max(handVals.get(0, OurPokerHand.HIGH_CARD4),
                        handVals.get(0, OurPokerHand.ONE_PAIR4))));
        handVals.put(0, OurPokerHand.STRAIGHT3, Math.max(handVals.get(0, OurPokerHand.STRAIGHT4),
                Math.max(handVals.get(0, OurPokerHand.HIGH_CARD4),
                        handVals.get(0, OurPokerHand.ONE_PAIR4))));
        handVals.put(0, OurPokerHand.INSIDE_STRAIGHT3, handVals.get(0, OurPokerHand.STRAIGHT3));
        handVals.put(0, OurPokerHand.THREE_OF_A_KIND3, Math.max(handVals.get(0, OurPokerHand.THREE_OF_A_KIND4),
                handVals.get(0, OurPokerHand.FOUR_OF_A_KIND4)));
        handVals.put(0, OurPokerHand.ONE_PAIR3, Math.max(handVals.get(0, OurPokerHand.THREE_OF_A_KIND4),
                Math.max(handVals.get(0, OurPokerHand.ONE_PAIR4),
                        handVals.get(0, OurPokerHand.TWO_PAIR4))));
        handVals.put(0, OurPokerHand.HIGH_CARD3, Math.max(handVals.get(0, OurPokerHand.HIGH_CARD4),
                handVals.get(0, OurPokerHand.ONE_PAIR4)));

        // 2-card hands
        handVals.put(0, OurPokerHand.ROYAL_FLUSH2, Math.max(handVals.get(0, OurPokerHand.ROYAL_FLUSH3),
                Math.max(handVals.get(0, OurPokerHand.STRAIGHT3), Math.max(handVals.get(0, OurPokerHand.FLUSH3),
                                Math.max(handVals.get(0, OurPokerHand.HIGH_CARD3),
                                        Math.max(handVals.get(0, OurPokerHand.ONE_PAIR3),
                                                handVals.get(0, OurPokerHand.STRAIGHT_FLUSH3)))))));
        handVals.put(0, OurPokerHand.STRAIGHT_FLUSH2,
                Math.max(handVals.get(0, OurPokerHand.STRAIGHT3), Math.max(handVals.get(0, OurPokerHand.FLUSH3),
                                Math.max(handVals.get(0, OurPokerHand.HIGH_CARD3),
                                        (Math.max(handVals.get(0, OurPokerHand.ONE_PAIR3),
                                                handVals.get(0, OurPokerHand.STRAIGHT_FLUSH3)))))));
        handVals.put(0, OurPokerHand.INSIDE_STRAIGHT_FLUSH2, handVals.get(0, OurPokerHand.STRAIGHT_FLUSH2));
        handVals.put(0, OurPokerHand.FLUSH2, Math.max(handVals.get(0, OurPokerHand.FLUSH3),
                Math.max(handVals.get(0, OurPokerHand.HIGH_CARD3),
                        handVals.get(0, OurPokerHand.ONE_PAIR3))));
        handVals.put(0, OurPokerHand.STRAIGHT2, Math.max(handVals.get(0, OurPokerHand.STRAIGHT3),
                Math.max(handVals.get(0, OurPokerHand.HIGH_CARD3),
                        handVals.get(0, OurPokerHand.ONE_PAIR3))));
        handVals.put(0, OurPokerHand.INSIDE_STRAIGHT2, handVals.get(0, OurPokerHand.STRAIGHT2));
        handVals.put(0, OurPokerHand.ONE_PAIR2, Math.max(handVals.get(0, OurPokerHand.THREE_OF_A_KIND3),
                handVals.get(0, OurPokerHand.ONE_PAIR3)));
        handVals.put(0, OurPokerHand.HIGH_CARD2, Math.max(handVals.get(0, OurPokerHand.HIGH_CARD3),
                handVals.get(0, OurPokerHand.ONE_PAIR3)));

        // Zero or one card hands
        handVals.put(0, OurPokerHand.ONE_CARD, Math.max(handVals.get(0, OurPokerHand.ROYAL_FLUSH2),
                Math.max(handVals.get(0, OurPokerHand.STRAIGHT2), Math.max(handVals.get(0, OurPokerHand.FLUSH2),
                                Math.max(handVals.get(0, OurPokerHand.HIGH_CARD2),
                                        Math.max(handVals.get(0, OurPokerHand.ONE_PAIR2),
                                                handVals.get(0, OurPokerHand.STRAIGHT_FLUSH2)))))));
        handVals.put(0, OurPokerHand.ZERO_CARDS, handVals.get(0, OurPokerHand.ONE_CARD));

        // Clone the handVals for the other 24 turns
        handVals.cloneAllTurns();

        adjustHandVals(endTime);
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
            ArrayList<Integer> bestPlays = new ArrayList<>(); // all plays yielding the maximum average score 
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
        if (numPlays > 24) {
            System.out.println("OOPS!  Start simGreedyPlay: numPlays: " + numPlays);
        }
        if (depthLimit == 0) { // with zero depth limit, return current score
            //return system.getScore(grid);
            return evalGrid(grid, numPlays - 1);
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
                    score = evalGrid(grid, numPlays - 1);
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

    /**
     * Use genetic algorithm to adjust the partial hand values.
     */
    private void adjustHandVals(long endTime) {
        int iter = 0;

        // Create initial population
        // 1. Unchanged (half)     
        // 2. Truly random (all others)
        GAPopulation population = new GAPopulation(popSize);
        for (int i = 0; i < popSize / 2; i++) {
            population.add(i, handVals.deepClone());
        }
        System.out.println(population.toString(0));
        
        for (int i = popSize / 2; i < popSize; i++) {
            population.add(i, getRandomHandValues(handVals));
            System.out.println("Initial population " + i +": ");
            System.out.println(population.toString(i));
        }
        long startLoopTime = System.currentTimeMillis();
        endTime = endTime - 3000; // Shorten time for testing
                
        // Generate new generations
        while (System.currentTimeMillis() < endTime) {
            iter++;
            if (iter % 1 == 0) {
                System.out.println(iter);
                System.out.println(population.toString(0));
            }

            // Evaluate each member of this generation
            for (int i = 0; i < popSize; i++) {
                population.updateValue(i, evalPartialHands(population.get(i)));
            }

            // Sort
            population.sort();

            // New generation
            GAPopulation newGeneration = new GAPopulation(popSize);

            // Keep the best 5% unchanged
            for (int i = 0; i < popSize / 20; i++) {
                newGeneration.add(i, population.get(i));
            }

            // popSize Children in next generation
            for (int i = popSize / 20; i < popSize; i++) {
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
                    newGeneration.add(i, population.get(i - popSize / 20));
                }
                
            }

            // Mutation
            // For each new child, choose 2 handvals and change at random (leaving best unchanged)
            if (mutationOn) {
                for (int i = popSize / 20; i < popSize; i++) {
                    for (int j = 0; j < 2; j++) {
                        int handIndex = random.nextInt(OurPokerHand.INSIDE_STRAIGHT_FLUSH2.ordinal() - OurPokerHand.HIGH_CARD4.ordinal())
                                + OurPokerHand.HIGH_CARD4.ordinal();
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

        for (int i = OurPokerHand.HIGH_CARD4.ordinal();
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

        for (int i = OurPokerHand.HIGH_CARD4.ordinal();
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

    private double evalPartialHands(HandValues values) {
        HandValues original = handVals;
        handVals = values;
        int total = 0;
        init();
        for (int i = 0; i < 10; i++) {
            int result = simGreedyPlay(25);
            total += result;
            //System.out.print("Play " + i + ": " + result + "; ");
            init();
        }
        //System.out.println();
        handVals = original;
        return total / 10.0;
    }

    /* (non-Javadoc)
     * @see PokerSquaresPlayer#getName()
     */
    @Override
    public String getName() {
        return "GAPlayer" + depthLimit + "_" + popSize;
    }

    private int evalGrid(Card[][] grid, int turn) {
        int[] handScores = new int[2 * SIZE];
        for (int row = 0; row < SIZE; row++) {
            Card[] hand = new Card[SIZE];
            for (int col = 0; col < SIZE; col++) {
                hand[col] = grid[row][col];
            }
            handScores[row] = handVals.get(turn, OurPokerHand.getPokerHand(hand));
        }
        for (int col = 0; col < SIZE; col++) {
            Card[] hand = new Card[SIZE];
            for (int row = 0; row < SIZE; row++) {
                hand[row] = grid[row][col];
            }
            handScores[SIZE + col] = handVals.get(turn, OurPokerHand.getPokerHand(hand));
        }

        int totalScore = 0;
        for (int handScore : handScores) {
            totalScore += handScore;
        }
        return totalScore;
    }

    /**
     * Get the score of the given Card hand (which may contain null values).
     *
     * @param hand Card hand
     * @return score of given Card hand.
     */
    public int getHandScore(Card[] hand, int turn) {
        return handVals.get(turn, OurPokerHand.getPokerHand(hand));
    }

    /**
     * Get an int array with the individual hand scores of rows 0 through 4
     * followed by columns 0 through 4.
     *
     * @param grid 2D Card array representing play grid
     * @return an int array with the individual hand scores of rows 0 through 4
     * followed by columns 0 through 4.
     */
    public int[] getHandScores(Card[][] grid, int turn) {
        int[] handScores = new int[2 * SIZE];
        for (int row = 0; row < SIZE; row++) {
            Card[] hand = new Card[SIZE];
            for (int col = 0; col < SIZE; col++) {
                hand[col] = grid[row][col];
            }
            handScores[row] = getHandScore(hand, turn);
        }
        for (int col = 0; col < SIZE; col++) {
            Card[] hand = new Card[SIZE];
            for (int row = 0; row < SIZE; row++) {
                hand[row] = grid[row][col];
            }
            handScores[SIZE + col] = getHandScore(hand, turn);
        }
        return handScores;
    }

    /**
     * Print the given game grid and score.
     *
     * @param grid given game grid
     */
    public void printGrid(Card[][] grid, int turn) {
        // get scores
        int[] handScores = getHandScores(grid, turn);
        int totalScore = 0;
        for (int handScore : handScores) {
            totalScore += handScore;
        }

        // print grid
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                System.out.printf(" %s ", grid[row][col] == null ? "--" : grid[row][col].toString());
            }
            System.out.printf("%3d\n", handScores[row]);
        }
        for (int col = 0; col < SIZE; col++) {
            System.out.printf("%3d ", handScores[SIZE + col]);
        }
        System.out.printf("%3d Total\n", totalScore);
    }

}
