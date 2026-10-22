
class BarDemo {
    //made this to test the cpu and memory bars separately
    //without changing the main program
    public static void main(String[] args) throws InterruptedException {
        showBars();
    }

    static void showBars() throws InterruptedException {
        System.out.println("CPU LOAD");

        int cores = cpuInfo.getCount();

        if (cores == 0) {
            System.out.println("No CPU data found");
        } else {
            //arrays to store the first readings for each core
            long[] firstBusy = new long[cores];
            long[] firstIdle = new long[cores];

            for (int i = 0; i < cores; i++) {
                firstBusy[i] = cpuInfo.getBusy(i);
                firstIdle[i] = cpuInfo.getIdle(i);
            }

            //why wait? cpu times are counted since boot
            //we need two readings to see how much they changed
            Thread.sleep(1000);

            double totalPercent = 0;
            int validCores = 0;

            //get the second readings and work out the load
            for (int i = 0; i < cores; i++) {
                long busy = cpuInfo.getBusy(i);
                long idle = cpuInfo.getIdle(i);

                //using the calculation from cpuInfo
                double load = cpuInfo.calculateLoad(
                    firstBusy[i], firstIdle[i], busy, idle
                );

                //skip this core if the reading isn't valid
                if (load < 0 || load > 1 || Double.isNaN(load)) {
                    System.out.println("cpu" + i + " N/A");
                    continue;
                }

                //calculate the percentage and print its bar
                double percent = load * 100;

                System.out.println("cpu" + i + " [" +
                    BarGraph.makeBar(percent) + "] " +
                    Math.round(percent) + "%");

                totalPercent += percent;
                validCores++;
            }

            //average of all the cores with valid readings
            if (validCores > 0) {
                double average = totalPercent / validCores;

                System.out.println("Average [" +
                    BarGraph.makeBar(average) + "] " +
                    Math.round(average) + "%");
            } else {
                System.out.println("Average N/A");
            }
        }

        System.out.println();
        System.out.println("MEMORY");

        long total = memInfo.getTotal();
        long available = memInfo.getAvailable();

        //check that the memory values make sense
        if (total <= 0 || available < 0 || available > total) {
            System.out.println("Memory information unavailable");
        } else {
            //work out the used and available percentages
            long used = total - available;
            double usedPercent = (double) used / total * 100;
            double availablePercent = (double) available / total * 100;

            //the # characters show used memory, dots show available
            System.out.println("RAM [" +
                BarGraph.makeBar(usedPercent) + "] " +
                Math.round(usedPercent) + "% used, " +
                Math.round(availablePercent) + "% available");
        }
    }
}
