package org.kabieror.elwasys.raspiclient.application;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class DevicePowerStatisticsAnalyzer {

    /**
     * Der Schwellwert für die Leistung, unter welcher das laufende Programm
     * beendet wird.
     */
    private static final float THREASHOLD = 2;

    /**
     * Die Wartezeit, die nach Unterschreiten des Schwellwerts vor Beendigung
     * des Programms gewartet wird.
     */
    private static final int WAIT_TIME = 30;

    public static void main(String[] args) {
        if (args.length != 1 || args[0].isEmpty()) {
            System.out.println("Call this program with the path to a fhem log file as parameter\n"
                    + "in which the power statistics can be found.");
            return;
        }

        final File logfile = new File(args[0]);
        if (!logfile.exists() || !logfile.canRead()) {
            System.out.println("The log file cannot be found or ist not readable.");
            return;
        }

        final BufferedReader reader = null;
        try {
            final List<String> lines = Files.readAllLines(logfile.toPath());

            LocalDateTime lowStart = null;

            String dateString = "";

            final DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;

            final DateTimeFormatter timeFormatter = DateTimeFormatter.ISO_LOCAL_TIME;

            final Pattern linePattern =
                    Pattern.compile("^(\\S+)\\s(\\S+)\\spower:\\s(\\d+(\\.?\\d+))$");
            for (final String line : lines) {
                final Matcher matcher = linePattern.matcher(line);
                if (matcher.matches()) {
                    final String strDate = matcher.group(1);
                    final String strPower = matcher.group(3);
                    final LocalDateTime now = LocalDateTime.parse(strDate,
                            DateTimeFormatter.ofPattern("uuuu-MM-dd_HH:mm:ss"));

                    final String nowDateString = dateFormatter.format(now);

                    final float power = Float.parseFloat(strPower);
                    if (power < THREASHOLD && lowStart == null) {
                        lowStart = now;
                    } else if (power > THREASHOLD && lowStart != null) {
                        final Duration lowDuration = Duration.between(lowStart, now);
                        String importantString = "";
                        if (!lowDuration.minus(Duration.ofSeconds(WAIT_TIME)).isNegative()) {
                            importantString = " !!END!!";
                        }
                        System.out.println(timeFormatter.format(lowStart) + " - Low "
                                + lowDuration.getSeconds() + "s" + importantString);
                        if (!nowDateString.equals(dateString)) {
                            dateString = nowDateString;
                            System.out.println("==\nDate: " + dateString);
                        }
                        System.out.println(timeFormatter.format(now) + " - High");
                        lowStart = null;
                    }
                }
            }
            if (lowStart != null) {
                System.out.println(timeFormatter.format(lowStart) + " - Low");
            }
        } catch (final IOException e) {
            e.printStackTrace();
            return;
        }
    }

}
