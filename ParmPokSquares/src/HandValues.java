import java.util.HashMap;

public class HandValues {
    private final int SIZE = 5;

    private HashMap<OurPokerHand, Integer>[] handVals = new HashMap[3];
    
    public HandValues() {
        for (int i = 0; i < handVals.length; i++) {
            handVals[i] = new HashMap<>();
        }        
    }
    
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
    
}
