package studio.fantasyit.ether_craft.util;

public class MathUtil {
    public static int findLCM(int a, int b) {
        if (a == 0 || b == 0) return 0;
        return Math.abs(a * (b / findGCD(a, b)));
    }

    public static int findGCD(int a, int b) {
        while (b != 0) {
            int temp = b;
            b = a % b;
            a = temp;
        }
        return Math.abs(a);
    }
}
