
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class GAPopulation {
    
    ArrayList<GAPopulationItem> population;
    
    public GAPopulation(int size) {
        population = new ArrayList<>();
    }
    
    public void add(int i, HandValues item) {
        population.add(i, new GAPopulationItem(item));        
    }
    
    public HandValues get (int i) {
        return population.get(i).getHandVals();
    }
    
    public void updateValue(int i, double newValue) {
        population.get(i).setValue(newValue);
    }
    
    public void sort() {
        Collections.sort(population);
        Collections.reverse(population);
    }
    
    public String toString(int index) {
        return population.get(index).toString();
    }
    
    private class GAPopulationItem implements Comparable<GAPopulationItem> {
        private HandValues handVals;
        private double value;
        
        public GAPopulationItem(HandValues handVals) {
            this.handVals = handVals;
            this.value = 0.0;
        }

        /**
         * @return the handVals
         */
        public HandValues getHandVals() {
            return handVals;
        }

        /**
         * @param handVals the handVals to set
         */
        public void setHandVals(HandValues handVals) {
            this.handVals = handVals;
        }

        /**
         * @return the value
         */
        public double getValue() {
            return value;
        }

        /**
         * @param value the value to set
         */
        public void setValue(double value) {
            this.value = value;
        }
        
        @Override
        public int compareTo (GAPopulationItem other) {
            if (this.value - other.value < 0.00000000001)
                return 0;
            return (int) (this.value - other.value);
        }
        
        public String toString() {
            String result = "";
            for (int i = 0; i < 25; i++) {
                for (int j = OurPokerHand.HIGH_CARD5.ordinal();
                        j < OurPokerHand.ZERO_CARDS.ordinal(); j++) {
                    result += handVals.get(i, OurPokerHand.values()[j]) + " ";
                }
                result += "\n";                        
            }
            return result;
        }        
    }
}
