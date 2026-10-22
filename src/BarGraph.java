class BarGraph {
    //makes a bar based on a percentage
    //used for both cpu load and memory usage
    static String makeBar(double percent) {

        //check if the percentage is valid (between 0 and 100)
        if (percent < 0 || percent > 100) {
            return "N/A";
        }

        //why divide by 5? each character represents 5%
        //20 characters * 5 = 100%
        //math.round rounds to the nearest whole number
        int filled = (int) Math.round(percent / 5);
        String bar = "";

        //loop through all 20 positions in the bar
        for (int i = 0; i < 20; i++) {

            //if the position is less than filled, add a #
            //add a dot for the unused percentage
            if (i < filled) {
                bar += "#";
            } else {
                bar += ".";
            }
        }

        //return the finished bar so it can be printed elsewhere
        return bar;
    }
}