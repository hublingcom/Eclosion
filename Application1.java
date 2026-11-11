package part1;

import nl.saxion.app.SaxionApp;

import java.util.ArrayList;
import java.util.Arrays;

public class Application1 implements Runnable{
    public static void main(String[] args) {
        SaxionApp.start(new Application1(), 1000, 500);
    }

    // The run method can be used to test your implementation. You can also change the argument values for
    // additional test cases.
    public void run() {

        SaxionApp.printLine("convertCelsiusToKelvin( 0 ). Expect: 273. Actual: " + convertCelsiusToKelvin(0));
        SaxionApp.printLine("convertCelsiusToKelvin( 100 ). Expect: 373. Actual: " + convertCelsiusToKelvin(100));

        SaxionApp.printLine();
        SaxionApp.printLine("calculatePercentageChange( 100.0, 200.0 ). Expect: 100, Actual: " + calculatePercentageChange(100.0, 200.0));
        SaxionApp.printLine("calculatePercentageChange( 100.0, 50.0 ) Expect: -50, Actual: " + calculatePercentageChange(100.0, 50.0));

        SaxionApp.printLine();
        SaxionApp.printLine("areBothNumbersBiggerThanThreshold( 4, 6, 5 ). Expect: false. Actual: " + areBothNumbersBiggerThanThreshold(4, 6, 5));
        SaxionApp.printLine("areBothNumbersBiggerThanThreshold( 5, 6, 3 ). Expect: true. Actual: " + areBothNumbersBiggerThanThreshold(5, 6, 3));

        SaxionApp.printLine();
        ArrayList<String> listA = new ArrayList<>( Arrays.asList("A", "B", "C", "D"));
        ArrayList<String> listB = new ArrayList<>( Arrays.asList("_", "B", "_", "D"));
        SaxionApp.printLine("getMatchingValues( " + listA +  ", " +  listB + "). Expect: ['B','D']. Actual: " + getMatchingValues(listA, listB));

        SaxionApp.printLine();
        int [] numbersA = { 1,2,3,4 };
        int [] numbersB = { 1,4,3,2 };
        SaxionApp.printLine("isSorted( " + Arrays.toString(numbersA) + " ). Expect: true. Actual: " + isSorted(numbersA));
        SaxionApp.printLine("isSorted( " + Arrays.toString(numbersB) + " ). Expect: false. Actual: " + isSorted(numbersB));
    }

    /**
     * Convert the temperature in Celsius to the temperature in Kelvin.
     * The formula to convert the temperature from Celsius to Kelvin is to add 273 to the Celsius number.
     *
     * 0 degrees Kelvin = -273 degrees Celsius
     * 273 degrees Kelvin = 0 degrees Celsius
     */
    int convertCelsiusToKelvin(int tempInCelsius) {
        return 0;
    }

    /**
     * Calculates the percentage change between two amounts. The result is determined by dividing the
     * difference between the new amount (newAmount) and the old amount (oldAmount) by the old amount,
     * and then multiplying by 100.
     *
     * The returned value indicates the percentage increase or decrease of the new amount relative to the old amount.
     *
     * oldAmount  newAmount       % change
     * 100        150             +50%  (increase)
     * 100        50              -50%  (decrease)
     */
     double calculatePercentageChange(double oldAmount, double newAmount) {
        return 0;
    }

    /**
     * Return true if both numbers are bigger than the threshold, false if not.
     * 
     * For instance:
     *  4, 6 and threshold 5. Result: false
     *  5, 6 and threshold 3. Result: true
     */
    boolean areBothNumbersBiggerThanThreshold(int number1, int number2, int threshold ) {
       return false;
    }

    /**
     * Find the matching values between the 2 lists.
     *
     * For instance:
     * listA= [A,B,C,D] and listB=[_,_,A,B] -> Result= [A,B]. A and B occur in both lists.
     * listA= [A,B,C,D] and listB=[E,F,G,H] -> Result= []. Empty list. Since no matching values in lists occur.
     * listA= [A,A,B,B] and listB=[A,A,B,B] -> Result= [A,B]. A and B occur in both lists.
     * Similar elements should only occur once in the resulting list.
     */
    ArrayList<String> getMatchingValues(ArrayList<String> listA, ArrayList<String> listB) {
        return null;
    }

    /**
     * Determine if the passed list with numbers is sorted from low to high.
     *
     * For instance:
     * [1,2,3,4] => is sorted and should result true
     * [1,4,3,2] => is not sorted and should return false
     */
    boolean isSorted(int [] input ) {
       return false;
    }
}