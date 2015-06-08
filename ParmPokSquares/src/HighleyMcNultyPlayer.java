
import java.util.Random;
import java.util.*;

public class HighleyMcNultyPlayer implements PokerSquaresPlayer {

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

    @Override
    public void setPointSystem(PokerSquaresPointSystem system, long millis) {
        this.system = system;
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
        handVals.put(OurPokerHand.FLUSH4,   Math.max(system.getHandScore(PokerHand.FLUSH),
                                            Math.max(system.getHandScore(PokerHand.HIGH_CARD),
                                                     system.getHandScore(PokerHand.ONE_PAIR))));        
        handVals.put(OurPokerHand.STRAIGHT4,Math.max(system.getHandScore(PokerHand.STRAIGHT),
                                            Math.max(system.getHandScore(PokerHand.HIGH_CARD),
                                                     system.getHandScore(PokerHand.ONE_PAIR))));
        handVals.put(OurPokerHand.INSIDE_STRAIGHT4, handVals.get(OurPokerHand.STRAIGHT4));
        handVals.put(OurPokerHand.THREE_OF_A_KIND4, Math.max(system.getHandScore(PokerHand.THREE_OF_A_KIND),
                                                    Math.max(system.getHandScore(PokerHand.FOUR_OF_A_KIND),
                                                             system.getHandScore(PokerHand.FULL_HOUSE))));
                
        
    }

    public void init() {

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
		while (!card.equals(simDeck[cardIndex]))
			cardIndex++;
		simDeck[cardIndex] = simDeck[numPlays];
		simDeck[numPlays] = card;
		
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
					if (averageScore > maxAverageScore)
						bestPlays.clear();
					bestPlays.add(play);
					maxAverageScore = averageScore;
				}
			}
			int bestPlay = bestPlays.get(random.nextInt(bestPlays.size())); // choose a best play (breaking ties randomly)
			// update our list of plays, recording the chosen play in its sequential position; all onward from numPlays are empty positions
			int bestPlayIndex = numPlays;
			while (plays[bestPlayIndex] != bestPlay)
				bestPlayIndex++;
			plays[bestPlayIndex] = plays[numPlays];
			plays[numPlays] = bestPlay;
		}
		int[] playPos = {plays[numPlays] / SIZE, plays[numPlays] % SIZE}; // decode it into row and column
		makePlay(card, playPos[0], playPos[1]); // make the chosen play (not undoing this time)
		return playPos; // return the chosen play
	}

	/**
	 * From the chosen play, perform simulated Card draws and greedy placement (depthLimit) iterations forward 
	 * and return the resulting grid score.
	 * @param depthLimit - how many simulated greedy plays to perform
	 * @return resulting grid score after greedy MC simulation to given depthLimit
	 */
	private int simGreedyPlay(int depthLimit) {
		if (depthLimit == 0) { // with zero depth limit, return current score
			return system.getScore(grid);
		}
		else { // up to the non-zero depth limit or to game end, iteratively make the given number of greedy plays o
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
					score = system.getScore(grid);
					if (score >= maxScore) {
						if (score > maxScore)
							bestPlays.clear();
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
		while (!card.equals(simDeck[cardIndex]))
			cardIndex++;
		simDeck[cardIndex] = simDeck[numPlays];
		simDeck[numPlays] = card;
		
		// update plays to reflect chosen play in sequence
		grid[row][col] = card;
		int play = row * SIZE + col;
		int j = 0;
		while (plays[j] != play)
			j++;
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

    public String getName() {
        return "HighleyMcNulty";
    }

    /**
     * OurPokerHand - An enumeration of Poker hand classes along with associated
     * identification numbers and Strings. Provides utility methods for
     * classifying complete or partial Poker hands.
     *
     * @author tneller
     *
     */
    private enum OurPokerHand {

        HIGH_CARD5(0, "high card"), ONE_PAIR5(1, "one pair"), TWO_PAIR5(2, "two pair"), THREE_OF_A_KIND5(3, "three of a kind"),
        STRAIGHT5(4, "straight"), FLUSH5(5, "flush"), FULL_HOUSE5(6, "full house"),
        FOUR_OF_A_KIND5(7, "four of a kind"), STRAIGHT_FLUSH5(8, "straight flush"), ROYAL_FLUSH5(9, "royal flush"),
        HIGH_CARD4(10, "high card 4"), ONE_PAIR4(11, "one pair 4"), TWO_PAIR4(12, "two pair 4"), THREE_OF_A_KIND4(13, "three of a kind 4"),
        STRAIGHT4(14, "straight (diff suits) 4"), FLUSH4(15, "flush 4"),
        FOUR_OF_A_KIND4(16, "four of a kind 4"), STRAIGHT_FLUSH4(17, "straight flush 4"), ROYAL_FLUSH4(18, "royal flush 4"),
        INSIDE_STRAIGHT4(19, "inside straight 4"), INSIDE_STRAIGHT_FLUSH4(20, "inside straight flush 4"),
        HIGH_CARD3(21, "high card 3"), ONE_PAIR3(22, "one pair 3"), THREE_OF_A_KIND3(23, "three of a kind 3"),
        STRAIGHT3(24, "straight (diff suits) 3"), FLUSH3(25, "flush 3"),
        STRAIGHT_FLUSH3(26, "straight flush 3"), ROYAL_FLUSH3(27, "royal flush 3"),
        INSIDE_STRAIGHT3(28, "inside straight 3"), INSIDE_STRAIGHT_FLUSH3(29, "inside straight flush 3"),
        HIGH_CARD2(30, "high card 2"), ONE_PAIR2(31, "one pair 2"),
        STRAIGHT2(32, "straight (diff suits) 2"), FLUSH2(33, "flush 2"),
        STRAIGHT_FLUSH2(34, "straight flush 2"), ROYAL_FLUSH2(35, "royal flush 2"),
        INSIDE_STRAIGHT2(36, "inside straight 2"), INSIDE_STRAIGHT_FLUSH2(37, "inside straight flush 2"),
        ONE_CARD(38, "one card"), ZERO_CARDS(39, "zero cards");

        public static final int NUM_HANDS = OurPokerHand.values().length;
        public int id;
        public String name;

        OurPokerHand(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public static OurPokerHand getPokerHand(Card[] hand) {
            int count = 0;
            for (int i = 0; i < hand.length; i++) {
                if (hand[i] != null) {
                    count++;
                }
            }
            if (count == 0) {
                return OurPokerHand.ZERO_CARDS;
            } else if (count == 1) {
                return OurPokerHand.ONE_CARD;
            } else if (count == 2) {
                return getPokerHand2(hand);
            } else if (count == 3) {
                return getPokerHand3(hand);
            } else if (count == 4) {
                return getPokerHand4(hand);
            } else {
                return getPokerHand5(hand);
            }
        }

        private static OurPokerHand getPokerHand2(Card[] hand) {

            // Compute counts
            int[] rankCounts = new int[Card.NUM_RANKS];
            int[] suitCounts = new int[Card.NUM_SUITS];
            for (Card card : hand) {
                if (card != null) {
                    rankCounts[card.getRank()]++;
                    suitCounts[card.getSuit()]++;
                }
            }

            // Compute count of rank counts
            int maxOfAKind = 0;
            int[] rankCountCounts = new int[hand.length + 1];
            for (int count : rankCounts) {
                rankCountCounts[count]++;
                if (count > maxOfAKind) {
                    maxOfAKind = count;
                }
            }

            // Flush check
            boolean hasFlush = false;
            for (int i = 0; i < Card.NUM_SUITS; i++) {
                if (suitCounts[i] != 0) {
                    if (suitCounts[i] == 2) {
                        hasFlush = true;
                    }
                    break;
                }
            }

            // Straight check
            boolean hasStraight = false;
            boolean hasRoyal = false;
            int rank = 0;
            while (rank <= Card.NUM_RANKS - 2 && rankCounts[rank] == 0) {
                rank++;
            }
            hasStraight = (rank <= Card.NUM_RANKS - 4 && rankCounts[rank] == 1 && rankCounts[rank + 1] == 1);
            if (rankCounts[0] == 1 && rankCounts[12] == 1) {
                hasStraight = true;
            }

            // Royal check
            int royalCount = 0;
            for (int i = 9; i <= 12; i++) {
                if (rankCounts[i] == 1) {
                    royalCount++;
                }
            }
            if (rankCounts[0] == 1) {
                royalCount++;
            }
            if (royalCount == 2) {
                hasRoyal = true;
            }

            // Inside straight check
            boolean hasInsideStraight = false;
            if (maxOfAKind == 1) {
                // Find max/min kind
                int maxKind = 12;
                while (rankCounts[maxKind] == 0) {
                    maxKind--;
                }

                int minKind = 0;
                while (rankCounts[minKind] == 0) {
                    minKind++;
                }

                if (maxKind - minKind <= 4) {
                    hasInsideStraight = false;
                }
            }

            if (hasFlush) {
                if (hasRoyal) {
                    return OurPokerHand.ROYAL_FLUSH2; // Royal Flush
                }
                if (hasStraight) {
                    return OurPokerHand.STRAIGHT_FLUSH2; // Straight Flush
                }
                if (hasInsideStraight) {
                    return OurPokerHand.INSIDE_STRAIGHT_FLUSH2;
                }
            }
            if (hasFlush) {
                return OurPokerHand.FLUSH2; // Flush
            }
            if (hasStraight) {
                return OurPokerHand.STRAIGHT2; // Straight
            }
            if (hasInsideStraight) {
                return OurPokerHand.INSIDE_STRAIGHT2;
            }
            if (rankCountCounts[2] == 1) {
                return OurPokerHand.ONE_PAIR2; // One Pair
            }
            return OurPokerHand.HIGH_CARD2; // Otherwise, High Card.  This applies to empty Card arrays as well.            
        }

        private static OurPokerHand getPokerHand3(Card[] hand) {

            // Compute counts
            int[] rankCounts = new int[Card.NUM_RANKS];
            int[] suitCounts = new int[Card.NUM_SUITS];
            for (Card card : hand) {
                if (card != null) {
                    rankCounts[card.getRank()]++;
                    suitCounts[card.getSuit()]++;
                }
            }

            // Compute count of rank counts
            int maxOfAKind = 0;
            int[] rankCountCounts = new int[hand.length + 1];
            for (int count : rankCounts) {
                rankCountCounts[count]++;
                if (count > maxOfAKind) {
                    maxOfAKind = count;
                }
            }

            // Flush check
            boolean hasFlush = false;
            for (int i = 0; i < Card.NUM_SUITS; i++) {
                if (suitCounts[i] != 0) {
                    if (suitCounts[i] == 3) {
                        hasFlush = true;
                    }
                    break;
                }
            }

            // Straight check
            boolean hasStraight = false;
            boolean hasRoyal = false;
            int rank = 0;
            while (rank <= Card.NUM_RANKS - 3 && rankCounts[rank] == 0) {
                rank++;
            }
            hasStraight = (rank <= Card.NUM_RANKS - 4 && rankCounts[rank] == 1 && rankCounts[rank + 1] == 1 && rankCounts[rank + 2] == 1);
            if (rankCounts[0] == 1 && rankCounts[12] == 1 && rankCounts[11] == 1) {
                hasStraight = true;
            }

            // Royal check
            int royalCount = 0;
            for (int i = 9; i <= 12; i++) {
                if (rankCounts[i] == 1) {
                    royalCount++;
                }
            }
            if (rankCounts[0] == 1) {
                royalCount++;
            }
            if (royalCount == 3) {
                hasRoyal = true;
            }

            // Inside straight check
            boolean hasInsideStraight = false;
            if (maxOfAKind == 1) {
                // Find max/min kind
                int maxKind = 12;
                while (rankCounts[maxKind] == 0) {
                    maxKind--;
                }

                int minKind = 0;
                while (rankCounts[minKind] == 0) {
                    minKind++;
                }

                if (maxKind - minKind <= 4) {
                    hasInsideStraight = false;
                }
            }

            if (hasFlush) {
                if (hasRoyal) {
                    return OurPokerHand.ROYAL_FLUSH3; // Royal Flush
                }
                if (hasStraight) {
                    return OurPokerHand.STRAIGHT_FLUSH3; // Straight Flush
                }
                if (hasInsideStraight) {
                    return OurPokerHand.INSIDE_STRAIGHT_FLUSH3;
                }
            }
            if (hasFlush) {
                return OurPokerHand.FLUSH3; // Flush
            }
            if (hasStraight) {
                return OurPokerHand.STRAIGHT3; // Straight
            }
            if (hasInsideStraight) {
                return OurPokerHand.INSIDE_STRAIGHT3;
            }
            if (maxOfAKind == 3) {
                return OurPokerHand.THREE_OF_A_KIND3; // Three of a Kind
            }
            if (rankCountCounts[2] == 1) {
                return OurPokerHand.ONE_PAIR3; // One Pair
            }
            return OurPokerHand.HIGH_CARD3; // Otherwise, High Card.  This applies to empty Card arrays as well.            
        }

        private static OurPokerHand getPokerHand4(Card[] hand) {

            // Compute counts
            int[] rankCounts = new int[Card.NUM_RANKS];
            int[] suitCounts = new int[Card.NUM_SUITS];
            for (Card card : hand) {
                if (card != null) {
                    rankCounts[card.getRank()]++;
                    suitCounts[card.getSuit()]++;
                }
            }

            // Compute count of rank counts
            int maxOfAKind = 0;
            int[] rankCountCounts = new int[hand.length + 1];
            for (int count : rankCounts) {
                rankCountCounts[count]++;
                if (count > maxOfAKind) {
                    maxOfAKind = count;
                }
            }

            // Flush check
            boolean hasFlush = false;
            for (int i = 0; i < Card.NUM_SUITS; i++) {
                if (suitCounts[i] != 0) {
                    if (suitCounts[i] == 4) {
                        hasFlush = true;
                    }
                    break;
                }
            }

            // Straight check
            boolean hasStraight = false;
            boolean hasRoyal = false;
            int rank = 0;
            while (rank <= Card.NUM_RANKS - 4 && rankCounts[rank] == 0) {
                rank++;
            }
            hasStraight = (rank <= Card.NUM_RANKS - 4 && rankCounts[rank] == 1 && rankCounts[rank + 1] == 1 && rankCounts[rank + 2] == 1 && rankCounts[rank + 3] == 1);
            if (rankCounts[0] == 1 && rankCounts[12] == 1 && rankCounts[11] == 1 && rankCounts[10] == 1) {
                hasStraight = true;
            }

            // Royal check
            int royalCount = 0;
            for (int i = 9; i <= 12; i++) {
                if (rankCounts[i] == 1) {
                    royalCount++;
                }
            }
            if (rankCounts[0] == 1) {
                royalCount++;
            }
            if (royalCount == 4) {
                hasRoyal = true;
            }

            // Inside straight check
            boolean hasInsideStraight = false;
            if (maxOfAKind == 1) {
                // Find max/min kind
                int maxKind = 12;
                while (rankCounts[maxKind] == 0) {
                    maxKind--;
                }

                int minKind = 0;
                while (rankCounts[minKind] == 0) {
                    minKind++;
                }

                if (maxKind - minKind <= 4) {
                    hasInsideStraight = false;
                }
            }

            if (hasFlush) {
                if (hasRoyal) {
                    return OurPokerHand.ROYAL_FLUSH4; // Royal Flush
                }
                if (hasStraight) {
                    return OurPokerHand.STRAIGHT_FLUSH4; // Straight Flush
                }
                if (hasInsideStraight) {
                    return OurPokerHand.INSIDE_STRAIGHT_FLUSH4;
                }
            }
            if (maxOfAKind == 4) {
                return OurPokerHand.FOUR_OF_A_KIND4; // Four of a Kind
            }
            if (hasFlush) {
                return OurPokerHand.FLUSH4; // Flush
            }
            if (hasStraight) {
                return OurPokerHand.STRAIGHT4; // Straight
            }
            if (hasInsideStraight) {
                return OurPokerHand.INSIDE_STRAIGHT4;
            }
            if (maxOfAKind == 3) {
                return OurPokerHand.THREE_OF_A_KIND4; // Three of a Kind
            }
            if (rankCountCounts[2] == 2) {
                return OurPokerHand.TWO_PAIR4; // Two Pair
            }
            if (rankCountCounts[2] == 1) {
                return OurPokerHand.ONE_PAIR4; // One Pair
            }
            return OurPokerHand.HIGH_CARD4; // Otherwise, High Card.  This applies to empty Card arrays as well.            
        }

        /**
         * Given a Card array (possibly with null values) classifies the current
         * Poker hand and returns the classification.
         *
         * @param hand - a Poker hand represented as an array of Card objects
         * which may contain null values
         * @return classification of the given Poker hand
         */
        private static OurPokerHand getPokerHand5(Card[] hand) {
            // Compute counts
            int[] rankCounts = new int[Card.NUM_RANKS];
            int[] suitCounts = new int[Card.NUM_SUITS];
            for (Card card : hand) {
                if (card != null) {
                    rankCounts[card.getRank()]++;
                    suitCounts[card.getSuit()]++;
                }
            }

            // Compute count of rank counts
            int maxOfAKind = 0;
            int[] rankCountCounts = new int[hand.length + 1];
            for (int count : rankCounts) {
                rankCountCounts[count]++;
                if (count > maxOfAKind) {
                    maxOfAKind = count;
                }
            }

            // Flush check
            boolean hasFlush = false;
            for (int i = 0; i < Card.NUM_SUITS; i++) {
                if (suitCounts[i] != 0) {
                    if (suitCounts[i] == hand.length) {
                        hasFlush = true;
                    }
                    break;
                }
            }

            // Straight check
            boolean hasStraight = false;
            boolean hasRoyal = false;
            int rank = 0;
            while (rank <= Card.NUM_RANKS - 5 && rankCounts[rank] == 0) {
                rank++;
            }
            hasStraight = (rank <= Card.NUM_RANKS - 5 && rankCounts[rank] == 1 && rankCounts[rank + 1] == 1 && rankCounts[rank + 2] == 1 && rankCounts[rank + 3] == 1 && rankCounts[rank + 4] == 1);
            if (rankCounts[0] == 1 && rankCounts[12] == 1 && rankCounts[11] == 1 && rankCounts[10] == 1 && rankCounts[9] == 1) {
                hasStraight = hasRoyal = true;
            }

            // Return score
            if (hasFlush) {
                if (hasRoyal) {
                    return OurPokerHand.ROYAL_FLUSH5; // Royal Flush
                }
                if (hasStraight) {
                    return OurPokerHand.STRAIGHT_FLUSH5; // Straight Flush
                }
            }
            if (maxOfAKind == 4) {
                return OurPokerHand.FOUR_OF_A_KIND5; // Four of a Kind
            }
            if (rankCountCounts[3] == 1 && rankCountCounts[2] == 1) {
                return OurPokerHand.FULL_HOUSE5; // Full House
            }
            if (hasFlush) {
                return OurPokerHand.FLUSH5; // Flush
            }
            if (hasStraight) {
                return OurPokerHand.STRAIGHT5; // Straight
            }
            if (maxOfAKind == 3) {
                return OurPokerHand.THREE_OF_A_KIND5; // Three of a Kind
            }
            if (rankCountCounts[2] == 2) {
                return OurPokerHand.TWO_PAIR5; // Two Pair
            }
            if (rankCountCounts[2] == 1) {
                return OurPokerHand.ONE_PAIR5; // One Pair
            }
            return OurPokerHand.HIGH_CARD5; // Otherwise, High Card.  This applies to empty Card arrays as well.
        }

        /**
         * Given a Card array (possibly with null values) classifies the current
         * Poker hand and returns the classification identification number.
         *
         * @param hand - a Poker hand represented as an array of Card objects
         * which may contain null values
         * @return classification identification number of the given Poker hand
         */
        public static final int getPokerHandId(Card[] hand) {
            return getPokerHand(hand).id;
        }

        /* (non-Javadoc)
         * @see java.lang.Enum#toString()
         */
        public String toString() {
            return name;
        }
    }

}
