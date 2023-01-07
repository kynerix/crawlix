package cloud.kynerix.crawlix.utils;

import java.util.StringTokenizer;

public class WatchFrequencyParser {

    // Returns the equivalent in seconds of an expression like "3d 4h" (=3 day and 4 hour)"
    public static int parse(String watchFreqExpr, int minValueSeconds) {
        int days = 0;
        int hours = 0;
        int minutes = 0;

        if (watchFreqExpr != null) {
            String str = watchFreqExpr
                    .toUpperCase()
                    .replace("D", " D ")
                    .replace("H", " H ")
                    .replace("M", " M ")
                    .replace(".", " ")
                    .replace("-", " ")
                    .trim();

            StringTokenizer stringTokenizer = new StringTokenizer(str, " ");
            int amount = 0;
            while (stringTokenizer.hasMoreTokens()) {
                String tok = stringTokenizer.nextToken();
                switch (tok) {
                    case "D":
                        days = amount;
                        amount = 0;
                        break;
                    case "H":
                        hours = amount;
                        amount = 0;
                        break;
                    case "M":
                        minutes = amount;
                        amount = 0;
                        break;
                    default:
                        try {
                            amount = Integer.parseInt(tok);
                        } catch (NumberFormatException e) {
                            // Ignore
                        }
                }
            }
        } else {
            days = 1;
        }

        // Return the equivalent in seconds
        return Math.max( days * 24 * 60 * 60 + hours * 60 * 60 + minutes * 60, minValueSeconds );
    }
}
