import java.util.HashMap;

public class HandValues {
    private final int SIZE = 5;

    private HashMap<OurPokerHand, Integer>[] handVals = new HashMap[3];
    
    public HandValues() {
        for (int i = 0; i < handVals.length; i++) {
            handVals[i] = new HashMap<>();
        }        
    }
    
    /**
     * Store the value of a particular hand on a particular turn.
     * Uses the same value for turns 0-9, 10-19, and 20-24.
     * 
     * @param index The turn at which the hand is to be evaluated
     * @param hand 
     * @param value The new value for the given hand 
     */
    public void put (int index, OurPokerHand hand, Integer value) {
        handVals[index / 10].put(hand, value);
    }
    
    public Integer get(int index, OurPokerHand hand) {
        return handVals[index / 10].get(hand);
    }
    
    // Copy the turn 0 hand values into all the other turns    
    public void cloneAllTurns() {
        for (int i = 1; i < handVals.length; i++) {
            handVals[i] = (HashMap<OurPokerHand, Integer>) handVals[0].clone();
        }
    }
    
    // Deep copy
    public HandValues deepClone() {
        HandValues result = new HandValues();
        
        for (int i = 0; i < result.handVals.length; i++) {
            result.handVals[i] = (HashMap<OurPokerHand, Integer>) this.handVals[i].clone();
        }
        
        return result;
    }
    
    public int size() {
        return handVals.length;
    }
    
    public String toString() {
        String result = "";
        result += " HC  1P  2P  3K  ST  FL  FH  4K  SF  RF HC4 1P4 2P4 3K4 ST4 FL4 4K4 SF4 RF4 IS4 IF4 HC3 1P3 3K3 ST3 FL3 SF3 RF3 IS3 IF3 HC2 1P2 ST2 FL2 SF2 RF2 IS2 IF2  1C  0C\n";
        for (int i = 0; i < 3; i++) {
            for (OurPokerHand theHand : OurPokerHand.values()) {
                result += String.format("%3d", handVals[i].get(theHand)) + " ";
            }
            result += "\n";
        }        
        return result;
    }
}
