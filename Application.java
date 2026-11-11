import nl.saxion.app.SaxionApp;

public class Application implements Runnable {

    public static void main(String[] args) {
        SaxionApp.start(new Application());
    }

    public void run() {int m = 18;
        int n = 17;
       SaxionApp.printLine(n / 10 + m % 20);
       SaxionApp.printLine(n % 2 + m % 20);
       SaxionApp.printLine((m + n) / 2);
       SaxionApp.printLine((m + n) / 2.0);


        // Define variables m and n

        // Print the different values - Remember to do this in your head / on paper first!

    }

}
