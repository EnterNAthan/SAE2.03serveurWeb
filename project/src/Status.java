public class Status {
    public static String buildStatusPage() {

        //recuperer la memoire libre
        String freeMemory = Utils.getFreeMemory();
        //System.out.println(freeMemory);
        //recuperer le disque libre
        String freeDisk = Utils.getFreeDisk();
        //recuperer le nombre de processus
        String processCount = Utils.getProcessCount().replace(" ", "&nbsp;");


        return """
                <html>
                    <head>
                        <title>Status</title>
                    </head>
                    <body>
                        <h1>Status</h1>
                        <p>Free memory: <strong>%s Mb</strong></p>
                        <p>Free disk: <strong>%s Mb</strong></p>
                        <p>Process count: <strong>%s</strong></p>
                    </body>
                </html>
                """.formatted(freeMemory, freeDisk, processCount);
    }
}
