
import java.util.Random;
import java.awt.Color;
import nl.saxion.app.SaxionApp;


public class CozyCafe implements Runnable {
    int flowers = 0;
    int tables = 0;
    int wallDecorations = 0;
    int wallpaper = 0;
    Random random = new Random();

    int coins = 0;
    int customersServed = 0;
    int target = 50;

    String[] drinks = {
            "Coffee",
            "Tea",
            "Hot Chocolate",
            "Iced Coffee"
    };

    String[] customerNames = {
            "Lily",
            "Max",
            "Sophie",
            "Oliver"
    };

    String[] personalities = {
            "Friendly",
            "In a hurry",
            "Very chatty",
            "Sleepy"
    };


    public static void main(String[] args) {
        SaxionApp.start(new CozyCafe(), 800, 600);
    }

    @Override
    public void run() {

        drawCafe();


        SaxionApp.printLine("Welcome to Cozy Cafe!",new Color(90, 55, 40));
        SaxionApp.printLine("Your goal is to earn 50 coins.",new Color(90, 55, 40));


        while (coins < target && customersServed < 10) {

            // Choose a random customer
            int customer = random.nextInt(4);

            // Choose a random drink
            int order = random.nextInt(4);

            SaxionApp.printLine("--------------------------");

            SaxionApp.printLine("Customer: " + customerNames[customer]);
            SaxionApp.printLine("Personality: " + personalities[customer]);

            // Customer introduction
            if (customer == 0) {
                SaxionApp.printLine("Lily: Hi! I love your cafe!");
            } else if (customer == 1) {
                SaxionApp.printLine("Max: Quick! I am in a hurry!");
            } else if (customer == 2) {
                SaxionApp.printLine("Sophie: Hello! How is your day going?");
            } else {
                SaxionApp.printLine("Oliver: Yawn... I need some coffee.");
            }

            SaxionApp.printLine("They want: " + drinks[order]);

            SaxionApp.printLine("");
            SaxionApp.printLine("Choose a drink:");

            for (int i = 0; i < drinks.length; i++) {
                SaxionApp.printLine((i + 1) + ". " + drinks[i]);
            }

            int choice = SaxionApp.readInt();

            if (choice == order + 1) {

                SaxionApp.printLine("Correct order!");

                coins += 10;

                // Customer reaction
                if (customer == 0) {
                    SaxionApp.printLine("Lily: Thank you so much! ❤️");
                } else if (customer == 1) {
                    SaxionApp.printLine("Max: Perfect! Thanks!");
                } else if (customer == 2) {
                    SaxionApp.printLine("Sophie: This is amazing! Let's chat!");
                } else {
                    SaxionApp.printLine("Oliver: Finally... coffee! Zzz.");
                }

            } else {

                SaxionApp.printLine("Oops! Wrong drink!");
                SaxionApp.printLine("The customer is disappointed.");
            }

            customersServed++;

            SaxionApp.printLine("");
            SaxionApp.printLine("Coins: " + coins);
            SaxionApp.printLine("Customers served: " + customersServed);
        }
        decorationShop();

        SaxionApp.clear();
        drawCafe();

        SaxionApp.printLine("Your renovated cafe!");
        SaxionApp.printLine("Remaining coins: " + coins);
    }

        public void drawCafe() {

        // Background
        SaxionApp.setBackgroundColor(new Color(255, 235, 220));

        // Floor
        SaxionApp.setFill(new Color(220, 177, 137));
        SaxionApp.drawRectangle(0, 420, 800, 180);

        // Back wall
            if (wallpaper > 0) {
                SaxionApp.setFill(new Color(255, 190, 210));
            } else {
                SaxionApp.setFill(new Color(255, 224, 210));
            }

            SaxionApp.drawRectangle(0, 0, 800, 420);

        // Window
        SaxionApp.setFill(new Color(185, 225, 235));
        SaxionApp.drawRectangle(40, 70, 180, 150);

        // Window frames
        SaxionApp.setFill(new Color(255, 248, 230));
        SaxionApp.drawRectangle(125, 70, 10, 150);
        SaxionApp.drawRectangle(40, 140, 180, 10);

        // Cafe sign
        SaxionApp.setFill(new Color(165, 105, 80));
        SaxionApp.drawRectangle(280, 35, 250, 65);

        SaxionApp.setTextDrawingColor(Color.WHITE);
        SaxionApp.drawText("COZY CAFE", 315, 55, 28);

        // Counter
        SaxionApp.setFill(new Color(145, 90, 60));
        SaxionApp.drawRectangle(100, 340, 600, 100);

        // Counter top
        SaxionApp.setFill(new Color(190, 125, 85));
        SaxionApp.drawRectangle(80, 325, 640, 25);

        // Coffee machine
        SaxionApp.setFill(new Color(95, 95, 100));
        SaxionApp.drawRectangle(480, 235, 100, 90);

        SaxionApp.setFill(new Color(210, 210, 210));
        SaxionApp.drawRectangle(495, 250, 70, 35);

        // Coffee cup
        SaxionApp.setFill(new Color(255, 250, 235));
        SaxionApp.drawRectangle(520, 300, 35, 25);

        // Plant pot
        SaxionApp.setFill(new Color(190, 100, 100));
        SaxionApp.drawRectangle(650, 280, 45, 45);

        // Plant leaves
        SaxionApp.setFill(new Color(80, 145, 90));
        SaxionApp.drawOval(635, 245, 25, 45);
        SaxionApp.drawOval(655, 225, 25, 50);
        SaxionApp.drawOval(675, 245, 25, 45);

        // Welcome message
        SaxionApp.setTextDrawingColor(new Color(110, 65, 50));
        SaxionApp.drawText("Welcome! Fresh coffee every day.", 200, 490, 22);

        // Decorative flowers
        SaxionApp.setFill(new Color(255, 150, 180));
        SaxionApp.drawCircle(270, 170, 12);
        SaxionApp.drawCircle(300, 170, 12);
        SaxionApp.drawCircle(285, 150, 12);
        SaxionApp.drawCircle(285, 190, 12);

        SaxionApp.setFill(new Color(255, 220, 100));
        SaxionApp.drawCircle(285, 170, 9);


            // Purchased flower pots
            for (int i = 0; i < flowers; i++) {

                int x = 250 + (i * 65);

                SaxionApp.setFill(new Color(190, 100, 100));
                SaxionApp.drawRectangle(x, 390, 30, 30);

                SaxionApp.setFill(new Color(80, 145, 90));
                SaxionApp.drawOval(x - 5, 360, 20, 35);
                SaxionApp.drawOval(x + 15, 355, 20, 35);
            }

            // Purchased tables
            for (int i = 0; i < tables; i++) {

                int x = 120 + (i * 100);

                SaxionApp.setFill(new Color(145, 90, 60));
                SaxionApp.drawRectangle(x, 455, 70, 12);
                SaxionApp.drawRectangle(x + 8, 467, 8, 35);
                SaxionApp.drawRectangle(x + 54, 467, 8, 35);
            }

            // Wall decorations
            for (int i = 0; i < wallDecorations; i++) {

                int x = 300 + (i * 65);

                SaxionApp.setFill(new Color(190, 130, 100));
                SaxionApp.drawRectangle(x, 130, 45, 55);

                SaxionApp.setFill(new Color(255, 220, 180));
                SaxionApp.drawCircle(x + 22, 155, 12);
            }

        }

    public void decorationShop() {

        int choice = 0;

        while (choice != 5) {

            SaxionApp.printLine("======================");
            SaxionApp.printLine("   COZY CAFE SHOP");
            SaxionApp.printLine("======================");

            SaxionApp.printLine("Your coins: " + coins);

            SaxionApp.printLine("1. Flower pot - 20 coins");
            SaxionApp.printLine("2. Cafe table - 30 coins");
            SaxionApp.printLine("3. Wall decoration - 40 coins");
            SaxionApp.printLine("4. Pink wallpaper - 50 coins");
            SaxionApp.printLine("5. Leave shop");

            SaxionApp.printLine("Choose an item:");
            choice = SaxionApp.readInt();

            if (choice == 1) {

                if (coins >= 20) {
                    coins -= 20;
                    flowers++;
                    SaxionApp.printLine("You bought a flower pot!");
                } else {
                    SaxionApp.printLine("Not enough coins!");
                }

            } else if (choice == 2) {

                if (coins >= 30) {
                    coins -= 30;
                    tables++;
                    SaxionApp.printLine("You bought a cafe table!");
                } else {
                    SaxionApp.printLine("Not enough coins!");
                }

            } else if (choice == 3) {

                if (coins >= 40) {
                    coins -= 40;
                    wallDecorations++;
                    SaxionApp.printLine("You bought a wall decoration!");
                } else {
                    SaxionApp.printLine("Not enough coins!");
                }

            } else if (choice == 4) {

                if (coins >= 50) {
                    coins -= 50;
                    wallpaper++;
                    SaxionApp.printLine("You bought pink wallpaper!");
                } else {
                    SaxionApp.printLine("Not enough coins!");
                }

            } else if (choice == 5) {

                SaxionApp.printLine("Thanks for shopping!");

            } else {

                SaxionApp.printLine("Invalid choice!");
            }
        }
    }

}
