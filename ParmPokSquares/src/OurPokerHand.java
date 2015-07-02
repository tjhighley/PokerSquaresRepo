
/**
 * OurPokerHand - An enumeration of Poker hand classes along with associated
 * identification numbers and Strings. Provides utility methods for classifying
 * complete or partial Poker hands.
 *
 * @author tneller
 *
 */
public enum OurPokerHand {

    // creates enumerated types for partial hands

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
    /*
     creates a poker hand
     */

    OurPokerHand(int id, String name) {
        this.id = id;
        this.name = name;
    }
    // returns the pokerhand that the player has

    public static OurPokerHand getPokerHand(Card[] hand) {
        int count = 0;
        // counts how many cards the player has in their hands
        for (int i = 0; i < hand.length; i++) {
            if (hand[i] != null) {
                count++;
            }
        }
        // returns ourPokerHand depending on how many cards are in it
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
    // returns the players poker hand when there are two cards in it

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

        // checks the player's hand for if they have the possibility of a flush
        boolean hasFlush = false;
        for (int i = 0; i < Card.NUM_SUITS; i++) {
            if (suitCounts[i] != 0) {
                if (suitCounts[i] == 2) {
                    hasFlush = true;
                }
                break;
            }
        }

        // checks the player's hand for if they have the possibility of a straight
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

        // checks the player's hand for if they have the possibility of the royalflush
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

        // checks the player's hand for the possibilty of an inside straight
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
                hasInsideStraight = true;
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
            return OurPokerHand.INSIDE_STRAIGHT2; // inside straight 2 cards in hand
        }
        if (rankCountCounts[2] == 1) {
            return OurPokerHand.ONE_PAIR2; // One Pair
        }
        return OurPokerHand.HIGH_CARD2; // Otherwise, High Card.  This applies to empty Card arrays as well.            
    }
// return the player's poker hand when they have 3 cards

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
                hasInsideStraight = true;
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
            return OurPokerHand.INSIDE_STRAIGHT3; // inside straight three cards in hand
        }
        if (maxOfAKind == 3) {
            return OurPokerHand.THREE_OF_A_KIND3; // Three of a Kind
        }
        if (rankCountCounts[2] == 1) {
            return OurPokerHand.ONE_PAIR3; // One Pair
        }
        return OurPokerHand.HIGH_CARD3; // Otherwise, High Card.  This applies to empty Card arrays as well.            
    }
// returns the player's pokerhand when there are four cards in hand

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
                hasInsideStraight = true;
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
            return OurPokerHand.INSIDE_STRAIGHT4; // inside straight 4 cards in hand
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
     * @param hand - a Poker hand represented as an array of Card objects which
     * may contain null values
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
     * @param hand - a Poker hand represented as an array of Card objects which
     * may contain null values
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
